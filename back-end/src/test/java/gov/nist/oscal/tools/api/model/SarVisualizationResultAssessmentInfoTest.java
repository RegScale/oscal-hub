package gov.nist.oscal.tools.api.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SarVisualizationResultAssessmentInfoTest {

    @Test
    void testNoArgsConstructor() {
        SarVisualizationResult.AssessmentInfo info = new SarVisualizationResult.AssessmentInfo();

        assertNotNull(info);
        assertNull(info.getUuid());
        assertNull(info.getTitle());
        assertNull(info.getDescription());
        assertNull(info.getVersion());
        assertNull(info.getOscalVersion());
        assertNull(info.getPublished());
        assertNull(info.getLastModified());
        assertNull(info.getSspImportHref());
    }

    @Test
    void testSetUuid() {
        SarVisualizationResult.AssessmentInfo info = new SarVisualizationResult.AssessmentInfo();
        info.setUuid("550e8400-e29b-41d4-a716-446655440000");
        assertEquals("550e8400-e29b-41d4-a716-446655440000", info.getUuid());
    }

    @Test
    void testSetTitle() {
        SarVisualizationResult.AssessmentInfo info = new SarVisualizationResult.AssessmentInfo();
        info.setTitle("Security Assessment Report 2025");
        assertEquals("Security Assessment Report 2025", info.getTitle());
    }

    @Test
    void testSetDescription() {
        SarVisualizationResult.AssessmentInfo info = new SarVisualizationResult.AssessmentInfo();
        info.setDescription("Comprehensive security assessment for production system");
        assertEquals("Comprehensive security assessment for production system", info.getDescription());
    }

    @Test
    void testSetVersion() {
        SarVisualizationResult.AssessmentInfo info = new SarVisualizationResult.AssessmentInfo();
        info.setVersion("1.2.0");
        assertEquals("1.2.0", info.getVersion());
    }

    @Test
    void testSetOscalVersion() {
        SarVisualizationResult.AssessmentInfo info = new SarVisualizationResult.AssessmentInfo();
        info.setOscalVersion("1.0.4");
        assertEquals("1.0.4", info.getOscalVersion());
    }

    @Test
    void testSetPublished() {
        SarVisualizationResult.AssessmentInfo info = new SarVisualizationResult.AssessmentInfo();
        info.setPublished("2025-01-15T10:30:00Z");
        assertEquals("2025-01-15T10:30:00Z", info.getPublished());
    }

    @Test
    void testSetLastModified() {
        SarVisualizationResult.AssessmentInfo info = new SarVisualizationResult.AssessmentInfo();
        info.setLastModified("2025-01-20T14:45:00Z");
        assertEquals("2025-01-20T14:45:00Z", info.getLastModified());
    }

    @Test
    void testSetSspImportHref() {
        SarVisualizationResult.AssessmentInfo info = new SarVisualizationResult.AssessmentInfo();
        info.setSspImportHref("https://example.com/ssp.json");
        assertEquals("https://example.com/ssp.json", info.getSspImportHref());
    }

    @Test
    void testSetAllFields() {
        SarVisualizationResult.AssessmentInfo info = new SarVisualizationResult.AssessmentInfo();
        info.setUuid("test-uuid-123");
        info.setTitle("Annual Security Assessment");
        info.setDescription("Complete security assessment report");
        info.setVersion("2.0.0");
        info.setOscalVersion("1.0.4");
        info.setPublished("2025-03-01T09:00:00Z");
        info.setLastModified("2025-03-15T16:30:00Z");
        info.setSspImportHref("https://oscal.example.com/ssp/prod-2025.json");

        assertEquals("test-uuid-123", info.getUuid());
        assertEquals("Annual Security Assessment", info.getTitle());
        assertEquals("Complete security assessment report", info.getDescription());
        assertEquals("2.0.0", info.getVersion());
        assertEquals("1.0.4", info.getOscalVersion());
        assertEquals("2025-03-01T09:00:00Z", info.getPublished());
        assertEquals("2025-03-15T16:30:00Z", info.getLastModified());
        assertEquals("https://oscal.example.com/ssp/prod-2025.json", info.getSspImportHref());
    }

    @Test
    void testSetFieldsToNull() {
        SarVisualizationResult.AssessmentInfo info = new SarVisualizationResult.AssessmentInfo();
        info.setUuid("uuid");
        info.setTitle("title");
        info.setDescription("description");
        info.setVersion("1.0.0");
        info.setOscalVersion("1.0.0");
        info.setPublished("2025-01-01");
        info.setLastModified("2025-01-01");
        info.setSspImportHref("http://example.com");

        info.setUuid(null);
        info.setTitle(null);
        info.setDescription(null);
        info.setVersion(null);
        info.setOscalVersion(null);
        info.setPublished(null);
        info.setLastModified(null);
        info.setSspImportHref(null);

        assertNull(info.getUuid());
        assertNull(info.getTitle());
        assertNull(info.getDescription());
        assertNull(info.getVersion());
        assertNull(info.getOscalVersion());
        assertNull(info.getPublished());
        assertNull(info.getLastModified());
        assertNull(info.getSspImportHref());
    }

    @Test
    void testModifyAllFields() {
        SarVisualizationResult.AssessmentInfo info = new SarVisualizationResult.AssessmentInfo();

        info.setUuid("old-uuid");
        info.setTitle("Old Title");
        info.setVersion("1.0.0");

        info.setUuid("new-uuid");
        info.setTitle("New Title");
        info.setVersion("2.0.0");

        assertEquals("new-uuid", info.getUuid());
        assertEquals("New Title", info.getTitle());
        assertEquals("2.0.0", info.getVersion());
    }

    @Test
    void testWithEmptyStrings() {
        SarVisualizationResult.AssessmentInfo info = new SarVisualizationResult.AssessmentInfo();
        info.setUuid("");
        info.setTitle("");
        info.setDescription("");
        info.setVersion("");
        info.setOscalVersion("");
        info.setPublished("");
        info.setLastModified("");
        info.setSspImportHref("");

        assertEquals("", info.getUuid());
        assertEquals("", info.getTitle());
        assertEquals("", info.getDescription());
        assertEquals("", info.getVersion());
        assertEquals("", info.getOscalVersion());
        assertEquals("", info.getPublished());
        assertEquals("", info.getLastModified());
        assertEquals("", info.getSspImportHref());
    }

    @Test
    void testCompleteAssessmentScenario() {
        SarVisualizationResult.AssessmentInfo info = new SarVisualizationResult.AssessmentInfo();
        info.setUuid("sar-prod-2025-q1");
        info.setTitle("Production System Security Assessment - Q1 2025");
        info.setDescription("Quarterly security assessment results for production environment");
        info.setVersion("1.5.0");
        info.setOscalVersion("1.0.4");
        info.setPublished("2025-04-01T00:00:00Z");
        info.setLastModified("2025-04-15T12:00:00Z");
        info.setSspImportHref("#550e8400-e29b-41d4-a716-446655440000");

        assertNotNull(info);
        assertTrue(info.getTitle().contains("Q1 2025"));
        assertTrue(info.getDescription().contains("production"));
        assertTrue(info.getSspImportHref().startsWith("#"));
        assertEquals("1.5.0", info.getVersion());
        assertEquals("1.0.4", info.getOscalVersion());
    }

    @Test
    void testWithLongDescription() {
        SarVisualizationResult.AssessmentInfo info = new SarVisualizationResult.AssessmentInfo();
        StringBuilder longDesc = new StringBuilder();
        for (int i = 0; i < 50; i++) {
            longDesc.append("This is a detailed security assessment paragraph. ");
        }
        info.setDescription(longDesc.toString());
        assertTrue(info.getDescription().length() > 1000);
    }

    @Test
    void testWithUrlInSspImportHref() {
        SarVisualizationResult.AssessmentInfo info = new SarVisualizationResult.AssessmentInfo();
        info.setSspImportHref("https://oscal.nist.gov/ssp/example.json");
        assertTrue(info.getSspImportHref().startsWith("https://"));
        assertTrue(info.getSspImportHref().endsWith(".json"));
    }

    @Test
    void testWithRelativeHrefInSspImportHref() {
        SarVisualizationResult.AssessmentInfo info = new SarVisualizationResult.AssessmentInfo();
        info.setSspImportHref("../ssp/system-plan.xml");
        assertEquals("../ssp/system-plan.xml", info.getSspImportHref());
    }

    @Test
    void testWithSpecialCharactersInTitle() {
        SarVisualizationResult.AssessmentInfo info = new SarVisualizationResult.AssessmentInfo();
        String titleWithSpecialChars = "SAR v2.0 (Updated) [2025-Q1] - Final Assessment";
        info.setTitle(titleWithSpecialChars);
        assertEquals(titleWithSpecialChars, info.getTitle());
        assertTrue(info.getTitle().contains("(Updated)"));
    }

    @Test
    void testPartiallyFilledInfo() {
        SarVisualizationResult.AssessmentInfo info = new SarVisualizationResult.AssessmentInfo();
        info.setUuid("partial-uuid");
        info.setTitle("Partial Assessment");

        assertEquals("partial-uuid", info.getUuid());
        assertEquals("Partial Assessment", info.getTitle());
        assertNull(info.getDescription());
        assertNull(info.getVersion());
        assertNull(info.getOscalVersion());
    }
}
