package gov.nist.oscal.tools.api.service.ai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Strips OVAL/SCAP boilerplate from XCCDF and SCAP datastream documents so
 * that what we send to Claude is the control-relevant content, not the
 * machine-checkable test logic. A typical 1.4 MB DISA STIG XCCDF file is
 * ~80% {@code <check>} elements pointing to OVAL and OVAL definitions; those
 * are noise for control mapping.
 *
 * <p>Kept: {@code <Benchmark>}, {@code <Group>}, {@code <Rule>},
 * {@code <title>}, {@code <description>}, {@code <ident>} (CCIs),
 * {@code <rationale>}, {@code <reference>}, {@code <warning>}.
 *
 * <p>Stripped: {@code <check>}, {@code <check-content>}, {@code <fix>},
 * {@code <fixtext>}, {@code <Profile>} (XCCDF tailoring, not OSCAL profiles),
 * {@code <Value>}, OVAL definitions/tests/objects/states/variables, CPE
 * dictionaries, {@code <TestResult>}.
 */
@Service
public class XccdfTrimmer {

    private static final Logger log = LoggerFactory.getLogger(XccdfTrimmer.class);

    private static final Set<String> SKIP_LOCAL_NAMES = Set.of(
            "check", "check-content", "check-content-ref",
            "check-import", "check-export",
            "fix", "fixtext",
            "Profile",
            "Value",
            "TestResult",
            "platform-specification",
            "definitions", "tests", "objects", "states", "variables",
            "generator", "schematron-version");

    public boolean looksLikeXccdf(String xml) {
        if (xml == null || xml.length() < 64) return false;
        return xml.contains("xccdf") || xml.contains("<Benchmark") || xml.contains(":Benchmark");
    }

    public String trim(String xml) {
        if (xml == null) return "";
        if (!looksLikeXccdf(xml)) return xml;

        try {
            XMLInputFactory in = XMLInputFactory.newInstance();
            in.setProperty(XMLInputFactory.IS_REPLACING_ENTITY_REFERENCES, true);
            in.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
            in.setProperty(XMLInputFactory.SUPPORT_DTD, false);

            XMLOutputFactory out = XMLOutputFactory.newInstance();

            StringWriter buf = new StringWriter(Math.min(xml.length(), 256 * 1024));
            XMLEventReader reader = in.createXMLEventReader(new StringReader(xml));
            XMLEventWriter writer = out.createXMLEventWriter(buf);

            int skipDepth = 0;
            while (reader.hasNext()) {
                XMLEvent e = reader.nextEvent();
                if (skipDepth > 0) {
                    if (e.isStartElement()) skipDepth++;
                    else if (e.isEndElement()) skipDepth--;
                    continue;
                }
                if (e.isStartElement()) {
                    StartElement se = e.asStartElement();
                    String local = se.getName().getLocalPart();
                    if (SKIP_LOCAL_NAMES.contains(local)) {
                        skipDepth = 1;
                        continue;
                    }
                }
                writer.add(e);
            }
            writer.flush();
            String trimmed = buf.toString();
            log.info("XccdfTrimmer reduced {} → {} chars ({}% retained)",
                    xml.length(), trimmed.length(),
                    xml.length() == 0 ? 0 : (trimmed.length() * 100 / xml.length()));
            return trimmed;
        } catch (Exception ex) {
            log.warn("XccdfTrimmer failed, falling back to verbatim XML: {}", ex.getMessage());
            return xml;
        }
    }

    /**
     * Compact rule digest — drops XML structure entirely and emits a flat
     * text representation of every Rule. Typical reduction is 5-10× beyond
     * what {@link #trim(String)} achieves, so multi-MB STIGs comfortably fit
     * in the input budget. Format per rule:
     *
     * <pre>
     * RULE V-230222 (severity=medium) [CCI-000196, CCI-000205]
     *   Title: Set passwords to 15 characters
     *   Description: Long passwords slow brute force.
     * </pre>
     */
    public String digest(String xml) {
        if (xml == null) return "";
        if (!looksLikeXccdf(xml)) return xml;

        try {
            XMLInputFactory in = XMLInputFactory.newInstance();
            in.setProperty(XMLInputFactory.IS_REPLACING_ENTITY_REFERENCES, true);
            in.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
            in.setProperty(XMLInputFactory.SUPPORT_DTD, false);
            XMLEventReader reader = in.createXMLEventReader(new StringReader(xml));

            StringBuilder out = new StringBuilder(Math.min(xml.length() / 4, 256 * 1024));
            String benchmarkTitle = null;
            String benchmarkVersion = null;

            // Per-rule scratch state. Reset on every <Rule> start.
            String ruleId = null;
            String ruleSeverity = null;
            List<String> ccis = new ArrayList<>();
            StringBuilder title = new StringBuilder();
            StringBuilder description = new StringBuilder();

            // Cursor: which child element of <Rule> is currently being read.
            // Determines which buffer accumulates Characters events.
            String cursor = null;
            int skipDepth = 0;
            boolean inRule = false;
            boolean inBenchmarkTitle = false;
            boolean inBenchmarkVersion = false;

            while (reader.hasNext()) {
                XMLEvent e = reader.nextEvent();
                if (skipDepth > 0) {
                    if (e.isStartElement()) skipDepth++;
                    else if (e.isEndElement()) skipDepth--;
                    continue;
                }
                if (e.isStartElement()) {
                    StartElement se = e.asStartElement();
                    String local = se.getName().getLocalPart();
                    if (SKIP_LOCAL_NAMES.contains(local)) {
                        skipDepth = 1;
                        continue;
                    }
                    if (!inRule && "Benchmark".equals(local)) {
                        // pull title/version from sub-elements as we go
                        continue;
                    }
                    if (!inRule && "Rule".equals(local)) {
                        inRule = true;
                        ruleId = attr(se, "id");
                        ruleSeverity = attr(se, "severity");
                        ccis.clear();
                        title.setLength(0);
                        description.setLength(0);
                        cursor = null;
                        continue;
                    }
                    if (inRule) {
                        if ("title".equals(local)) cursor = "title";
                        else if ("description".equals(local)) cursor = "description";
                        else if ("ident".equals(local)) cursor = "ident";
                        else cursor = null;
                        continue;
                    }
                    if (!inRule && "title".equals(local)) inBenchmarkTitle = true;
                    if (!inRule && "version".equals(local)) inBenchmarkVersion = true;
                } else if (e.isEndElement()) {
                    String local = e.asEndElement().getName().getLocalPart();
                    if (inRule && "Rule".equals(local)) {
                        appendRule(out, ruleId, ruleSeverity, ccis,
                                title.toString().trim(), description.toString().trim());
                        inRule = false;
                        cursor = null;
                        continue;
                    }
                    if (inRule) {
                        cursor = null;
                        continue;
                    }
                    if (inBenchmarkTitle && "title".equals(local)) inBenchmarkTitle = false;
                    if (inBenchmarkVersion && "version".equals(local)) inBenchmarkVersion = false;
                } else if (e.isCharacters() && !e.asCharacters().isWhiteSpace()) {
                    String text = e.asCharacters().getData();
                    if (inRule && cursor != null) {
                        if ("title".equals(cursor)) title.append(text);
                        else if ("description".equals(cursor)) description.append(text);
                        else if ("ident".equals(cursor)) ccis.add(text.trim());
                    } else if (!inRule && inBenchmarkTitle && benchmarkTitle == null) {
                        benchmarkTitle = text.trim();
                    } else if (!inRule && inBenchmarkVersion && benchmarkVersion == null) {
                        benchmarkVersion = text.trim();
                    }
                }
            }

            StringBuilder header = new StringBuilder();
            if (benchmarkTitle != null) header.append("Document: ").append(benchmarkTitle).append('\n');
            if (benchmarkVersion != null) header.append("Version: ").append(benchmarkVersion).append('\n');
            if (header.length() > 0) header.append('\n');
            String digest = header.toString() + out.toString();

            log.info("XccdfTrimmer.digest reduced {} → {} chars ({}% retained)",
                    xml.length(), digest.length(),
                    xml.length() == 0 ? 0 : (digest.length() * 100 / xml.length()));
            return digest;
        } catch (Exception ex) {
            log.warn("XccdfTrimmer.digest failed, falling back to trim(): {}", ex.getMessage());
            return trim(xml);
        }
    }

    private static String attr(StartElement se, String name) {
        Iterator<Attribute> it = se.getAttributes();
        while (it.hasNext()) {
            Attribute a = it.next();
            if (name.equals(a.getName().getLocalPart())) return a.getValue();
        }
        return null;
    }

    private static void appendRule(StringBuilder out, String id, String severity,
                                   List<String> ccis, String title, String description) {
        if (id == null && title.isEmpty()) return;
        out.append("RULE ").append(id == null ? "(unknown)" : id);
        if (severity != null) out.append(" (severity=").append(severity).append(')');
        if (!ccis.isEmpty()) out.append(" [").append(String.join(", ", ccis)).append(']');
        out.append('\n');
        if (!title.isEmpty()) out.append("  Title: ").append(squash(title)).append('\n');
        if (!description.isEmpty()) out.append("  Description: ").append(squash(description)).append('\n');
        out.append('\n');
    }

    private static String squash(String s) {
        return s.replaceAll("\\s+", " ").trim();
    }
}
