package gov.nist.oscal.tools.api.model;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SarVisualizationResultRiskTest {

    @Test
    void testNoArgsConstructor() {
        SarVisualizationResult.Risk risk = new SarVisualizationResult.Risk();

        assertNotNull(risk);
        assertNull(risk.getUuid());
        assertNull(risk.getTitle());
        assertNull(risk.getDescription());
        assertNull(risk.getStatus());
        assertNotNull(risk.getRelatedControls());
        assertTrue(risk.getRelatedControls().isEmpty());
    }

    @Test
    void testSetUuid() {
        SarVisualizationResult.Risk risk = new SarVisualizationResult.Risk();
        risk.setUuid("risk-001");
        assertEquals("risk-001", risk.getUuid());
    }

    @Test
    void testSetTitle() {
        SarVisualizationResult.Risk risk = new SarVisualizationResult.Risk();
        risk.setTitle("Insufficient Access Controls");
        assertEquals("Insufficient Access Controls", risk.getTitle());
    }

    @Test
    void testSetDescription() {
        SarVisualizationResult.Risk risk = new SarVisualizationResult.Risk();
        risk.setDescription("Access controls are not properly configured");
        assertEquals("Access controls are not properly configured", risk.getDescription());
    }

    @Test
    void testSetStatus() {
        SarVisualizationResult.Risk risk = new SarVisualizationResult.Risk();
        risk.setStatus("open");
        assertEquals("open", risk.getStatus());
    }

    @Test
    void testSetRelatedControls() {
        SarVisualizationResult.Risk risk = new SarVisualizationResult.Risk();
        List<String> controls = Arrays.asList("AC-1", "AC-2", "AC-3");
        risk.setRelatedControls(controls);

        assertEquals(controls, risk.getRelatedControls());
        assertEquals(3, risk.getRelatedControls().size());
    }

    @Test
    void testSetAllFields() {
        SarVisualizationResult.Risk risk = new SarVisualizationResult.Risk();

        risk.setUuid("risk-123");
        risk.setTitle("Critical Security Risk");
        risk.setDescription("This is a detailed description of the security risk");
        risk.setStatus("mitigated");
        risk.setRelatedControls(Arrays.asList("SC-7", "SC-8", "SC-13"));

        assertEquals("risk-123", risk.getUuid());
        assertEquals("Critical Security Risk", risk.getTitle());
        assertEquals("This is a detailed description of the security risk", risk.getDescription());
        assertEquals("mitigated", risk.getStatus());
        assertEquals(3, risk.getRelatedControls().size());
    }

    @Test
    void testSetFieldsToNull() {
        SarVisualizationResult.Risk risk = new SarVisualizationResult.Risk();

        risk.setUuid("uuid");
        risk.setTitle("title");
        risk.setDescription("description");
        risk.setStatus("status");

        risk.setUuid(null);
        risk.setTitle(null);
        risk.setDescription(null);
        risk.setStatus(null);
        risk.setRelatedControls(null);

        assertNull(risk.getUuid());
        assertNull(risk.getTitle());
        assertNull(risk.getDescription());
        assertNull(risk.getStatus());
        assertNull(risk.getRelatedControls());
    }

    @Test
    void testModifyAllFields() {
        SarVisualizationResult.Risk risk = new SarVisualizationResult.Risk();

        risk.setUuid("first-uuid");
        risk.setTitle("First Title");
        risk.setStatus("open");

        risk.setUuid("second-uuid");
        risk.setTitle("Second Title");
        risk.setStatus("closed");

        assertEquals("second-uuid", risk.getUuid());
        assertEquals("Second Title", risk.getTitle());
        assertEquals("closed", risk.getStatus());
    }

    @Test
    void testWithEmptyStrings() {
        SarVisualizationResult.Risk risk = new SarVisualizationResult.Risk();

        risk.setUuid("");
        risk.setTitle("");
        risk.setDescription("");
        risk.setStatus("");

        assertEquals("", risk.getUuid());
        assertEquals("", risk.getTitle());
        assertEquals("", risk.getDescription());
        assertEquals("", risk.getStatus());
    }

    @Test
    void testWithEmptyRelatedControls() {
        SarVisualizationResult.Risk risk = new SarVisualizationResult.Risk();
        risk.setRelatedControls(new ArrayList<>());

        assertNotNull(risk.getRelatedControls());
        assertTrue(risk.getRelatedControls().isEmpty());
    }

    @Test
    void testWithSingleRelatedControl() {
        SarVisualizationResult.Risk risk = new SarVisualizationResult.Risk();
        risk.setRelatedControls(Arrays.asList("AC-1"));

        assertEquals(1, risk.getRelatedControls().size());
        assertEquals("AC-1", risk.getRelatedControls().get(0));
    }

    @Test
    void testWithMultipleRelatedControls() {
        SarVisualizationResult.Risk risk = new SarVisualizationResult.Risk();
        List<String> controls = Arrays.asList(
            "AC-1", "AC-2", "AC-3", "AU-1", "AU-2",
            "CA-1", "CP-1", "IA-1", "SC-1", "SI-1"
        );
        risk.setRelatedControls(controls);

        assertEquals(10, risk.getRelatedControls().size());
        assertTrue(risk.getRelatedControls().contains("AC-1"));
        assertTrue(risk.getRelatedControls().contains("SI-1"));
    }

    @Test
    void testDifferentRiskStatuses() {
        String[] statuses = {"open", "closed", "mitigated", "accepted", "transferred", "investigating"};

        for (String status : statuses) {
            SarVisualizationResult.Risk risk = new SarVisualizationResult.Risk();
            risk.setStatus(status);
            assertEquals(status, risk.getStatus());
        }
    }

    @Test
    void testWithLongDescription() {
        SarVisualizationResult.Risk risk = new SarVisualizationResult.Risk();
        String longDescription = "This is a very detailed description of the security risk " +
                                "that includes multiple paragraphs and extensive analysis. ".repeat(5);
        risk.setDescription(longDescription);
        assertEquals(longDescription, risk.getDescription());
    }

    @Test
    void testCompleteRiskScenario() {
        SarVisualizationResult.Risk risk = new SarVisualizationResult.Risk();

        risk.setUuid("550e8400-e29b-41d4-a716-446655440000");
        risk.setTitle("Inadequate Encryption Standards");
        risk.setDescription("The system uses outdated encryption algorithms that are vulnerable to attacks");
        risk.setStatus("open");
        risk.setRelatedControls(Arrays.asList("SC-8", "SC-12", "SC-13"));

        assertNotNull(risk);
        assertTrue(risk.getTitle().contains("Encryption"));
        assertTrue(risk.getDescription().contains("encryption"));
        assertEquals("open", risk.getStatus());
        assertTrue(risk.getRelatedControls().contains("SC-8"));
    }

    @Test
    void testMultipleRisksIndependence() {
        SarVisualizationResult.Risk risk1 = new SarVisualizationResult.Risk();
        SarVisualizationResult.Risk risk2 = new SarVisualizationResult.Risk();

        risk1.setUuid("risk-1");
        risk1.setTitle("Risk 1");
        risk1.setStatus("open");

        risk2.setUuid("risk-2");
        risk2.setTitle("Risk 2");
        risk2.setStatus("closed");

        assertNotEquals(risk1.getUuid(), risk2.getUuid());
        assertNotEquals(risk1.getTitle(), risk2.getTitle());
        assertNotEquals(risk1.getStatus(), risk2.getStatus());
    }

    @Test
    void testWithControlEnhancements() {
        SarVisualizationResult.Risk risk = new SarVisualizationResult.Risk();
        List<String> controls = Arrays.asList("AC-2", "AC-2(1)", "AC-2(2)", "AC-2(3)");
        risk.setRelatedControls(controls);

        assertEquals(4, risk.getRelatedControls().size());
        assertTrue(risk.getRelatedControls().contains("AC-2"));
        assertTrue(risk.getRelatedControls().contains("AC-2(1)"));
    }

    @Test
    void testWithSpecialCharactersInTitle() {
        SarVisualizationResult.Risk risk = new SarVisualizationResult.Risk();
        risk.setTitle("Risk: SQL Injection (High Severity) - Database");

        assertTrue(risk.getTitle().contains(":"));
        assertTrue(risk.getTitle().contains("("));
        assertTrue(risk.getTitle().contains(")"));
        assertTrue(risk.getTitle().contains("-"));
    }

    @Test
    void testWithValidUuidFormat() {
        SarVisualizationResult.Risk risk = new SarVisualizationResult.Risk();
        String uuid = "f47ac10b-58cc-4372-a567-0e02b2c3d479";
        risk.setUuid(uuid);

        assertEquals(36, risk.getUuid().length());
        assertTrue(risk.getUuid().contains("-"));
    }

    @Test
    void testRiskLifecycle() {
        SarVisualizationResult.Risk risk = new SarVisualizationResult.Risk();

        // Risk is created and identified
        risk.setUuid("risk-lifecycle-001");
        risk.setTitle("Configuration Vulnerability");
        risk.setDescription("Server misconfiguration detected");
        risk.setStatus("open");
        risk.setRelatedControls(Arrays.asList("CM-1", "CM-2"));

        assertEquals("open", risk.getStatus());

        // Risk is being investigated
        risk.setStatus("investigating");
        assertEquals("investigating", risk.getStatus());

        // Risk is mitigated
        risk.setStatus("mitigated");
        assertEquals("mitigated", risk.getStatus());

        // Risk is closed
        risk.setStatus("closed");
        assertEquals("closed", risk.getStatus());
    }

    @Test
    void testWithDifferentControlFamilies() {
        SarVisualizationResult.Risk risk = new SarVisualizationResult.Risk();
        List<String> controls = Arrays.asList(
            "AC-1",  // Access Control
            "AU-2",  // Audit and Accountability
            "CA-1",  // Security Assessment
            "IA-2",  // Identification and Authentication
            "SC-7"   // System and Communications Protection
        );
        risk.setRelatedControls(controls);

        assertEquals(5, risk.getRelatedControls().size());
        assertTrue(risk.getRelatedControls().stream().anyMatch(c -> c.startsWith("AC")));
        assertTrue(risk.getRelatedControls().stream().anyMatch(c -> c.startsWith("AU")));
        assertTrue(risk.getRelatedControls().stream().anyMatch(c -> c.startsWith("SC")));
    }

    @Test
    void testCriticalRiskScenario() {
        SarVisualizationResult.Risk risk = new SarVisualizationResult.Risk();

        risk.setUuid("critical-001");
        risk.setTitle("Critical: Unpatched Vulnerabilities");
        risk.setDescription("Multiple critical security patches are missing from production systems");
        risk.setStatus("open");
        risk.setRelatedControls(Arrays.asList("SI-2", "RA-5", "CM-3"));

        assertNotNull(risk);
        assertTrue(risk.getTitle().startsWith("Critical"));
        assertEquals("open", risk.getStatus());
        assertTrue(risk.getRelatedControls().size() > 0);
    }

    @Test
    void testFieldAssignmentOrder() {
        SarVisualizationResult.Risk risk = new SarVisualizationResult.Risk();

        // Set in different order
        risk.setStatus("open");
        risk.setRelatedControls(Arrays.asList("AC-1"));
        risk.setTitle("Test Risk");
        risk.setUuid("uuid-123");
        risk.setDescription("Test description");

        // Order shouldn't matter, all should be set correctly
        assertEquals("uuid-123", risk.getUuid());
        assertEquals("Test Risk", risk.getTitle());
        assertEquals("Test description", risk.getDescription());
        assertEquals("open", risk.getStatus());
        assertEquals(1, risk.getRelatedControls().size());
    }
}
