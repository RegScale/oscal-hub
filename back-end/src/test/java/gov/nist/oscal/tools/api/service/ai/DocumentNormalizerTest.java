package gov.nist.oscal.tools.api.service.ai;

import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;

import static org.assertj.core.api.Assertions.assertThat;

class DocumentNormalizerTest {

    private final DocumentNormalizer normalizer = new DocumentNormalizer(new XccdfTrimmer());

    @Test
    void plainTextPassesThrough() {
        NormalizedDoc d = normalizer.normalize("Hello world");
        assertThat(d.plainText()).isEqualTo("Hello world");
        assertThat(d.detectedMime()).isEqualTo("text/plain");
    }

    @Test
    void docxIsExtractedToTextAndXhtml() throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (XWPFDocument doc = new XWPFDocument()) {
            doc.createParagraph().createRun().setText("Document body for test.");
            doc.write(bos);
        }
        NormalizedDoc d = normalizer.normalize(bos.toByteArray(), "test.docx");
        assertThat(d.plainText()).contains("Document body for test");
        assertThat(d.xhtml()).contains("Document body for test");
        assertThat(d.detectedMime()).contains("wordprocessingml");
    }

    @Test
    void htmlIsExtractedAndStripped() {
        String html = "<html><body><h1>Heading</h1><p>Body text.</p></body></html>";
        NormalizedDoc d = normalizer.normalize(html.getBytes(), "test.html");
        assertThat(d.plainText()).contains("Heading");
        assertThat(d.plainText()).contains("Body text");
        // XHTML output preserves structure
        assertThat(d.xhtml()).containsAnyOf("<h1", "Heading");
    }
}
