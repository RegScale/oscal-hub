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

@Service
public class DocumentNormalizer {

    private static final Logger log = LoggerFactory.getLogger(DocumentNormalizer.class);
    private static final int MAX_CHARS = 1_500_000;

    public NormalizedDoc normalize(String plainText) {
        String safe = plainText == null ? "" : plainText;
        if (safe.length() > MAX_CHARS) {
            throw new IllegalArgumentException("Input exceeds " + MAX_CHARS + " chars");
        }
        return new NormalizedDoc(safe, safe, "text/plain", null, safe.length());
    }

    public NormalizedDoc normalize(byte[] bytes, String filename) {
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
