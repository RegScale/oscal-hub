package gov.nist.oscal.tools.api.service.ai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import java.io.StringReader;
import java.io.StringWriter;
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
}
