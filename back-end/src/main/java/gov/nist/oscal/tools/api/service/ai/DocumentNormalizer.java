package gov.nist.oscal.tools.api.service.ai;

import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.HttpHeaders;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.metadata.TikaCoreProperties;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.sax.ToHTMLContentHandler;
import org.apache.tika.sax.ToTextContentHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.xml.sax.SAXException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Service
public class DocumentNormalizer {

    private static final Logger log = LoggerFactory.getLogger(DocumentNormalizer.class);
    private static final int MAX_CHARS = 1_500_000;

    private final XccdfTrimmer xccdfTrimmer;

    public DocumentNormalizer(XccdfTrimmer xccdfTrimmer) {
        this.xccdfTrimmer = xccdfTrimmer;
    }

    /**
     * Filename extensions whose content is already structured text Claude
     * reads natively. We bypass Tika for these so the model sees XCCDF tags,
     * JSON keys, YAML indentation, CSV columns intact instead of a Tika dump
     * that strips the structure.
     *
     * <p>Covers the STIG / SCAP variants we care about:
     * <ul>
     *   <li>{@code .xml}, {@code .xccdf} — XCCDF checklists, SCAP datastreams,
     *       OVAL definitions, SRGs in XML form</li>
     *   <li>{@code .json} — DISA's newer JSON STIG releases</li>
     *   <li>{@code .yaml}, {@code .yml} — Ansible playbook STIG content</li>
     *   <li>{@code .csv} — STIG Viewer exports, POA&amp;M trackers</li>
     *   <li>{@code .txt}, {@code .md} — paste-as-text fallbacks</li>
     * </ul>
     */
    private static final Map<String, String> STRUCTURED_TEXT_MIME = Map.of(
            ".xml", "application/xml",
            ".xccdf", "application/xml",
            ".json", "application/json",
            ".yaml", "application/yaml",
            ".yml", "application/yaml",
            ".csv", "text/csv",
            ".txt", "text/plain",
            ".md", "text/markdown");

    public NormalizedDoc normalize(String plainText) {
        String safe = plainText == null ? "" : plainText;
        if (safe.length() > MAX_CHARS) {
            throw new IllegalArgumentException("Input exceeds " + MAX_CHARS + " chars");
        }
        return new NormalizedDoc(safe, safe, "text/plain", null, safe.length());
    }

    public NormalizedDoc normalize(byte[] bytes, String filename) {
        // Pass structured text formats through verbatim — Tika would strip
        // their structure (XML element names, JSON keys, YAML indentation,
        // CSV columns) which Claude reads natively.
        String lower = filename == null ? "" : filename.toLowerCase();
        for (Map.Entry<String, String> e : STRUCTURED_TEXT_MIME.entrySet()) {
            if (lower.endsWith(e.getKey())) {
                String text = new String(bytes, StandardCharsets.UTF_8);
                // For XCCDF/SCAP, drop XML structure entirely and emit a
                // compact rule digest (RULE / Title / CCIs / Description).
                // STIG XCCDF files are routinely 1-3 MB raw but compress
                // ~95-98% as digest, well within the input budget.
                if ((".xml".equals(e.getKey()) || ".xccdf".equals(e.getKey()))
                        && xccdfTrimmer.looksLikeXccdf(text)) {
                    text = xccdfTrimmer.digest(text);
                }
                if (text.length() > MAX_CHARS) {
                    throw new IllegalArgumentException(
                            "Structured text input exceeds " + MAX_CHARS + " chars");
                }
                log.info("Passed-through structured filename={} mime={} chars={}",
                        filename, e.getValue(), text.length());
                return new NormalizedDoc(text, text, e.getValue(), filename, text.length());
            }
        }

        try {
            AutoDetectParser parser = new AutoDetectParser();
            Metadata md = new Metadata();
            if (filename != null) md.set(TikaCoreProperties.RESOURCE_NAME_KEY, filename);

            ToHTMLContentHandler html = new ToHTMLContentHandler();
            parser.parse(new ByteArrayInputStream(bytes), html, md, new ParseContext());
            String xhtml = html.toString();

            ToTextContentHandler text = new ToTextContentHandler();
            parser.parse(new ByteArrayInputStream(bytes), text, new Metadata(), new ParseContext());
            String plain = text.toString();

            if (plain.length() > MAX_CHARS) {
                throw new IllegalArgumentException("Extracted text exceeds " + MAX_CHARS + " chars");
            }

            String mime = md.get(HttpHeaders.CONTENT_TYPE);
            log.info("Normalized document filename={} mime={} chars={}", filename, mime, plain.length());
            return new NormalizedDoc(plain, xhtml, mime, filename, plain.length());
        } catch (TikaException | IOException | SAXException e) {
            throw new IllegalArgumentException("Failed to normalize document: " + e.getMessage(), e);
        }
    }
}
