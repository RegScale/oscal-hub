package gov.nist.oscal.tools.api.service.ai;

import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;

import static org.assertj.core.api.Assertions.assertThat;

class SourceIngestorTest {

    private final SourceIngestor ingestor = new SourceIngestor(new DocumentNormalizer(new XccdfTrimmer()));

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

    @Test
    void xccdfIsDigestedToCompactTextWithRuleStructure() {
        // XCCDF gets reduced to a compact rule digest (no XML) before being
        // sent to the model — so multi-MB STIGs fit in the input budget.
        String xccdf =
                "<xccdf:Benchmark xmlns:xccdf=\"http://checklists.nist.gov/xccdf/1.2\">\n" +
                "  <xccdf:Rule id=\"V-12345\" severity=\"high\">\n" +
                "    <xccdf:title>Disable telnet</xccdf:title>\n" +
                "  </xccdf:Rule>\n" +
                "</xccdf:Benchmark>";
        IngestedSource s = ingestor.ingestAny(xccdf.getBytes(java.nio.charset.StandardCharsets.UTF_8), "rhel9.xccdf");
        assertThat(s.kind()).isEqualTo(IngestedSource.Kind.TEXT);
        assertThat(s.text()).contains("RULE V-12345");
        assertThat(s.text()).contains("severity=high");
        assertThat(s.text()).contains("Disable telnet");
        // No XML structure should remain.
        assertThat(s.text()).doesNotContain("xccdf:Rule");
        assertThat(s.text()).doesNotContain("<");
    }

    @Test
    void jsonPassesThroughWithKeysIntact() {
        String json = "{\"benchmark\":{\"id\":\"RHEL_9\",\"rules\":[{\"id\":\"V-1\",\"title\":\"x\"}]}}";
        IngestedSource s = ingestor.ingestAny(json.getBytes(java.nio.charset.StandardCharsets.UTF_8), "stig.json");
        assertThat(s.kind()).isEqualTo(IngestedSource.Kind.TEXT);
        assertThat(s.text()).isEqualTo(json);
    }

    @Test
    void yamlPassesThroughWithIndentation() {
        String yaml = "- name: Set permissions\n  file:\n    path: /etc/foo\n    mode: '0644'\n";
        IngestedSource s = ingestor.ingestAny(yaml.getBytes(java.nio.charset.StandardCharsets.UTF_8), "playbook.yaml");
        assertThat(s.kind()).isEqualTo(IngestedSource.Kind.TEXT);
        assertThat(s.text()).contains("- name:");
        assertThat(s.text()).contains("    mode: '0644'");
    }

    @Test
    void csvPassesThroughWithCommas() {
        String csv = "V-ID,Severity,Title\nV-1,high,Disable telnet\nV-2,medium,Enforce password length";
        IngestedSource s = ingestor.ingestAny(csv.getBytes(java.nio.charset.StandardCharsets.UTF_8), "stig-export.csv");
        assertThat(s.kind()).isEqualTo(IngestedSource.Kind.TEXT);
        assertThat(s.text()).contains("V-ID,Severity,Title");
        assertThat(s.text()).contains("V-2,medium");
    }

    @Test
    void unknownExtensionFallsThroughToTika() throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (XWPFDocument doc = new XWPFDocument()) {
            doc.createParagraph().createRun().setText("Body via Tika");
            doc.write(bos);
        }
        IngestedSource s = ingestor.ingestAny(bos.toByteArray(), "unknown.docx");
        assertThat(s.kind()).isEqualTo(IngestedSource.Kind.TEXT);
        assertThat(s.text()).contains("Body via Tika");
    }
}
