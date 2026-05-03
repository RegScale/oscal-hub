package gov.nist.oscal.tools.api.service.ai;

import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.ByteArrayInputStream;
import java.net.URI;

@Service
public class SourceIngestor {

    private static final Logger log = LoggerFactory.getLogger(SourceIngestor.class);
    private static final long MAX_PDF_BYTES = 32L * 1024 * 1024;
    private static final long MAX_TEXT_CHARS = 1_500_000;

    private final RestTemplate restTemplate = new RestTemplate();

    public IngestedSource ingestText(String text) {
        if (text == null) text = "";
        if (text.length() > MAX_TEXT_CHARS) {
            throw new IllegalArgumentException("Text input exceeds " + MAX_TEXT_CHARS + " chars");
        }
        return new IngestedSource(IngestedSource.Kind.TEXT, null, text, null, text.length());
    }

    public IngestedSource ingestPdf(String filename, byte[] bytes) {
        if (bytes == null) throw new IllegalArgumentException("Empty PDF");
        if (bytes.length > MAX_PDF_BYTES) {
            throw new IllegalArgumentException("PDF size " + bytes.length + " exceeds " + MAX_PDF_BYTES);
        }
        return new IngestedSource(IngestedSource.Kind.PDF, filename, null, bytes, bytes.length);
    }

    public IngestedSource ingestDocx(byte[] bytes) {
        try (XWPFDocument doc = new XWPFDocument(new ByteArrayInputStream(bytes));
             XWPFWordExtractor extractor = new XWPFWordExtractor(doc)) {
            String text = extractor.getText();
            return ingestText(text);
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to read .docx: " + e.getMessage(), e);
        }
    }

    public IngestedSource ingestUrl(String url) {
        try {
            String body = restTemplate.getForObject(URI.create(url), String.class);
            // Best-effort HTML->text strip. For richer extraction, a follow-up plan can swap in jsoup.
            String text = body == null ? "" : body.replaceAll("<[^>]+>", " ").replaceAll("\\s+", " ").trim();
            return ingestText(text);
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to fetch URL " + url + ": " + e.getMessage(), e);
        }
    }
}
