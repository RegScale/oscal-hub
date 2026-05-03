package gov.nist.oscal.tools.api.service.ai;

import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;

import static org.assertj.core.api.Assertions.assertThat;

class SourceIngestorTest {

    private final SourceIngestor ingestor = new SourceIngestor();

    @Test
    void plainTextPassesThrough() {
        IngestedSource s = ingestor.ingestText("Hello world");
        assertThat(s.text()).isEqualTo("Hello world");
        assertThat(s.kind()).isEqualTo(IngestedSource.Kind.TEXT);
    }

    @Test
    void docxIsExtractedToText() throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (XWPFDocument doc = new XWPFDocument()) {
            doc.createParagraph().createRun().setText("Document body for test.");
            doc.write(bos);
        }
        IngestedSource s = ingestor.ingestDocx(bos.toByteArray());
        assertThat(s.text()).contains("Document body for test");
        assertThat(s.kind()).isEqualTo(IngestedSource.Kind.TEXT);
    }

    @Test
    void pdfIsKeptAsBytes() {
        IngestedSource s = ingestor.ingestPdf("name.pdf", new byte[]{1, 2, 3});
        assertThat(s.kind()).isEqualTo(IngestedSource.Kind.PDF);
        assertThat(s.pdfBytes()).hasSize(3);
    }

    @Test
    void rejectsOversizedPdf() {
        byte[] big = new byte[60 * 1024 * 1024]; // 60 MB
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> ingestor.ingestPdf("big.pdf", big))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("exceeds");
    }
}
