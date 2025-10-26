package gov.nist.oscal.tools.api.model;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SarVisualizationResultTest {

    @Test
    void testNoArgsConstructor() {
        SarVisualizationResult result = new SarVisualizationResult();

        assertNotNull(result);
        assertNotNull(result.getTimestamp());
        assertNotNull(result.getControlsByFamily());
        assertNotNull(result.getFindings());
        assertNotNull(result.getObservations());
        assertNotNull(result.getRisks());
        assertTrue(result.getControlsByFamily().isEmpty());
        assertTrue(result.getFindings().isEmpty());
        assertTrue(result.getObservations().isEmpty());
        assertTrue(result.getRisks().isEmpty());
    }

    @Test
    void testTwoArgsConstructor() {
        SarVisualizationResult result = new SarVisualizationResult(true, "Success");

        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertEquals("Success", result.getMessage());
        assertNotNull(result.getTimestamp());
    }

    @Test
    void testSetSuccess() {
        SarVisualizationResult result = new SarVisualizationResult();
        result.setSuccess(true);
        assertTrue(result.isSuccess());

        result.setSuccess(false);
        assertFalse(result.isSuccess());
    }

    @Test
    void testSetMessage() {
        SarVisualizationResult result = new SarVisualizationResult();
        result.setMessage("Assessment completed successfully");
        assertEquals("Assessment completed successfully", result.getMessage());
    }

    @Test
    void testSetTimestamp() {
        SarVisualizationResult result = new SarVisualizationResult();
        String customTimestamp = "2024-01-15T10:30:00Z";
        result.setTimestamp(customTimestamp);
        assertEquals(customTimestamp, result.getTimestamp());
    }

    @Test
    void testSetAssessmentInfo() {
        SarVisualizationResult result = new SarVisualizationResult();
        SarVisualizationResult.AssessmentInfo info = new SarVisualizationResult.AssessmentInfo();
        info.setTitle("Assessment Plan");

        result.setAssessmentInfo(info);

        assertNotNull(result.getAssessmentInfo());
        assertEquals("Assessment Plan", result.getAssessmentInfo().getTitle());
    }

    @Test
    void testSetAssessmentSummary() {
        SarVisualizationResult result = new SarVisualizationResult();
        SarVisualizationResult.AssessmentSummary summary = new SarVisualizationResult.AssessmentSummary();
        summary.setTotalControlsAssessed(50);

        result.setAssessmentSummary(summary);

        assertNotNull(result.getAssessmentSummary());
        assertEquals(50, result.getAssessmentSummary().getTotalControlsAssessed());
    }

    @Test
    void testSetControlsByFamily() {
        SarVisualizationResult result = new SarVisualizationResult();
        Map<String, SarVisualizationResult.ControlFamilyAssessment> controlsByFamily = new HashMap<>();

        SarVisualizationResult.ControlFamilyAssessment acFamily = new SarVisualizationResult.ControlFamilyAssessment();
        acFamily.setFamilyId("AC");
        acFamily.setFamilyName("Access Control");
        controlsByFamily.put("AC", acFamily);

        result.setControlsByFamily(controlsByFamily);

        assertEquals(1, result.getControlsByFamily().size());
        assertTrue(result.getControlsByFamily().containsKey("AC"));
    }

    @Test
    void testSetFindings() {
        SarVisualizationResult result = new SarVisualizationResult();
        List<SarVisualizationResult.Finding> findings = new ArrayList<>();

        SarVisualizationResult.Finding finding = new SarVisualizationResult.Finding();
        finding.setTitle("Security Finding");
        findings.add(finding);

        result.setFindings(findings);

        assertEquals(1, result.getFindings().size());
        assertEquals("Security Finding", result.getFindings().get(0).getTitle());
    }

    @Test
    void testSetObservations() {
        SarVisualizationResult result = new SarVisualizationResult();
        List<SarVisualizationResult.Observation> observations = new ArrayList<>();

        SarVisualizationResult.Observation observation = new SarVisualizationResult.Observation();
        observation.setTitle("Security Observation");
        observations.add(observation);

        result.setObservations(observations);

        assertEquals(1, result.getObservations().size());
        assertEquals("Security Observation", result.getObservations().get(0).getTitle());
    }

    @Test
    void testSetRisks() {
        SarVisualizationResult result = new SarVisualizationResult();
        List<SarVisualizationResult.Risk> risks = new ArrayList<>();

        SarVisualizationResult.Risk risk = new SarVisualizationResult.Risk();
        risk.setTitle("High Risk");
        risks.add(risk);

        result.setRisks(risks);

        assertEquals(1, result.getRisks().size());
        assertEquals("High Risk", result.getRisks().get(0).getTitle());
    }

    @Test
    void testCompleteAssessmentScenario() {
        SarVisualizationResult result = new SarVisualizationResult(true, "Assessment completed");

        // Set assessment info
        SarVisualizationResult.AssessmentInfo info = new SarVisualizationResult.AssessmentInfo();
        info.setTitle("Annual Security Assessment");
        info.setUuid("assess-001");
        result.setAssessmentInfo(info);

        // Set assessment summary
        SarVisualizationResult.AssessmentSummary summary = new SarVisualizationResult.AssessmentSummary();
        summary.setTotalControlsAssessed(100);
        summary.setTotalFindings(25);
        summary.setTotalObservations(50);
        summary.setTotalRisks(10);
        result.setAssessmentSummary(summary);

        // Set controls by family
        Map<String, SarVisualizationResult.ControlFamilyAssessment> families = new HashMap<>();
        SarVisualizationResult.ControlFamilyAssessment acFamily = new SarVisualizationResult.ControlFamilyAssessment();
        acFamily.setFamilyId("AC");
        acFamily.setTotalControlsAssessed(20);
        families.put("AC", acFamily);
        result.setControlsByFamily(families);

        // Assertions
        assertTrue(result.isSuccess());
        assertEquals("Assessment completed", result.getMessage());
        assertEquals("Annual Security Assessment", result.getAssessmentInfo().getTitle());
        assertEquals(100, result.getAssessmentSummary().getTotalControlsAssessed());
        assertEquals(1, result.getControlsByFamily().size());
    }

    @Test
    void testWithMultipleFindings() {
        SarVisualizationResult result = new SarVisualizationResult();
        List<SarVisualizationResult.Finding> findings = new ArrayList<>();

        for (int i = 0; i < 10; i++) {
            SarVisualizationResult.Finding finding = new SarVisualizationResult.Finding();
            finding.setTitle("Finding " + i);
            findings.add(finding);
        }

        result.setFindings(findings);

        assertEquals(10, result.getFindings().size());
    }

    @Test
    void testWithMultipleControlFamilies() {
        SarVisualizationResult result = new SarVisualizationResult();
        Map<String, SarVisualizationResult.ControlFamilyAssessment> families = new HashMap<>();

        String[] familyIds = {"AC", "AU", "CA", "CM", "IA", "SC", "SI"};
        for (String id : familyIds) {
            SarVisualizationResult.ControlFamilyAssessment family = new SarVisualizationResult.ControlFamilyAssessment();
            family.setFamilyId(id);
            families.put(id, family);
        }

        result.setControlsByFamily(families);

        assertEquals(7, result.getControlsByFamily().size());
    }

    @Test
    void testSetAllFieldsToNull() {
        SarVisualizationResult result = new SarVisualizationResult();

        result.setMessage(null);
        result.setTimestamp(null);
        result.setAssessmentInfo(null);
        result.setAssessmentSummary(null);
        result.setControlsByFamily(null);
        result.setFindings(null);
        result.setObservations(null);
        result.setRisks(null);

        assertNull(result.getMessage());
        assertNull(result.getTimestamp());
        assertNull(result.getAssessmentInfo());
        assertNull(result.getAssessmentSummary());
        assertNull(result.getControlsByFamily());
        assertNull(result.getFindings());
        assertNull(result.getObservations());
        assertNull(result.getRisks());
    }

    @Test
    void testTimestampIsAutoGenerated() {
        SarVisualizationResult result1 = new SarVisualizationResult();

        // Small delay to ensure different timestamps
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        SarVisualizationResult result2 = new SarVisualizationResult();

        assertNotNull(result1.getTimestamp());
        assertNotNull(result2.getTimestamp());
        // Timestamps should be ISO-8601 format
        assertTrue(result1.getTimestamp().contains("T"));
        assertTrue(result2.getTimestamp().contains("Z"));
    }

    @Test
    void testWithEmptyCollections() {
        SarVisualizationResult result = new SarVisualizationResult();

        result.setFindings(new ArrayList<>());
        result.setObservations(new ArrayList<>());
        result.setRisks(new ArrayList<>());
        result.setControlsByFamily(new HashMap<>());

        assertTrue(result.getFindings().isEmpty());
        assertTrue(result.getObservations().isEmpty());
        assertTrue(result.getRisks().isEmpty());
        assertTrue(result.getControlsByFamily().isEmpty());
    }

    @Test
    void testFailureScenario() {
        SarVisualizationResult result = new SarVisualizationResult(false, "Assessment failed: Invalid SAR format");

        assertFalse(result.isSuccess());
        assertEquals("Assessment failed: Invalid SAR format", result.getMessage());
        assertNotNull(result.getTimestamp());
    }

    @Test
    void testModifyAllFields() {
        SarVisualizationResult result = new SarVisualizationResult(true, "First message");

        result.setSuccess(false);
        result.setMessage("Second message");

        assertFalse(result.isSuccess());
        assertEquals("Second message", result.getMessage());
    }
}
