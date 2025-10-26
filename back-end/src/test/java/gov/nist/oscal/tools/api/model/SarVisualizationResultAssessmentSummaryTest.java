package gov.nist.oscal.tools.api.model;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SarVisualizationResultAssessmentSummaryTest {

    @Test
    void testNoArgsConstructor() {
        SarVisualizationResult.AssessmentSummary summary = new SarVisualizationResult.AssessmentSummary();

        assertNotNull(summary);
        assertEquals(0, summary.getTotalControlsAssessed());
        assertEquals(0, summary.getTotalFindings());
        assertEquals(0, summary.getTotalObservations());
        assertEquals(0, summary.getTotalRisks());
        assertEquals(0, summary.getUniqueFamiliesAssessed());
        assertNotNull(summary.getFindingsBySeverity());
        assertNotNull(summary.getObservationsByType());
        assertNotNull(summary.getScoreDistribution());
        assertNotNull(summary.getRisksBySeverity());
    }

    @Test
    void testSetTotalControlsAssessed() {
        SarVisualizationResult.AssessmentSummary summary = new SarVisualizationResult.AssessmentSummary();
        summary.setTotalControlsAssessed(50);
        assertEquals(50, summary.getTotalControlsAssessed());
    }

    @Test
    void testSetTotalFindings() {
        SarVisualizationResult.AssessmentSummary summary = new SarVisualizationResult.AssessmentSummary();
        summary.setTotalFindings(25);
        assertEquals(25, summary.getTotalFindings());
    }

    @Test
    void testSetTotalObservations() {
        SarVisualizationResult.AssessmentSummary summary = new SarVisualizationResult.AssessmentSummary();
        summary.setTotalObservations(100);
        assertEquals(100, summary.getTotalObservations());
    }

    @Test
    void testSetTotalRisks() {
        SarVisualizationResult.AssessmentSummary summary = new SarVisualizationResult.AssessmentSummary();
        summary.setTotalRisks(15);
        assertEquals(15, summary.getTotalRisks());
    }

    @Test
    void testSetUniqueFamiliesAssessed() {
        SarVisualizationResult.AssessmentSummary summary = new SarVisualizationResult.AssessmentSummary();
        summary.setUniqueFamiliesAssessed(12);
        assertEquals(12, summary.getUniqueFamiliesAssessed());
    }

    @Test
    void testSetFindingsBySeverity() {
        SarVisualizationResult.AssessmentSummary summary = new SarVisualizationResult.AssessmentSummary();
        Map<String, Integer> findingsBySeverity = new HashMap<>();
        findingsBySeverity.put("high", 5);
        findingsBySeverity.put("medium", 10);
        findingsBySeverity.put("low", 15);

        summary.setFindingsBySeverity(findingsBySeverity);

        assertEquals(3, summary.getFindingsBySeverity().size());
        assertEquals(5, summary.getFindingsBySeverity().get("high"));
        assertEquals(10, summary.getFindingsBySeverity().get("medium"));
        assertEquals(15, summary.getFindingsBySeverity().get("low"));
    }

    @Test
    void testSetObservationsByType() {
        SarVisualizationResult.AssessmentSummary summary = new SarVisualizationResult.AssessmentSummary();
        Map<String, Integer> observationsByType = new HashMap<>();
        observationsByType.put("interview", 20);
        observationsByType.put("examination", 30);
        observationsByType.put("test", 15);

        summary.setObservationsByType(observationsByType);

        assertEquals(3, summary.getObservationsByType().size());
        assertEquals(20, summary.getObservationsByType().get("interview"));
        assertEquals(30, summary.getObservationsByType().get("examination"));
        assertEquals(15, summary.getObservationsByType().get("test"));
    }

    @Test
    void testSetScoreDistribution() {
        SarVisualizationResult.AssessmentSummary summary = new SarVisualizationResult.AssessmentSummary();
        Map<String, Integer> scoreDistribution = new HashMap<>();
        scoreDistribution.put("0-25", 5);
        scoreDistribution.put("26-50", 10);
        scoreDistribution.put("51-75", 20);
        scoreDistribution.put("76-100", 15);

        summary.setScoreDistribution(scoreDistribution);

        assertEquals(4, summary.getScoreDistribution().size());
        assertEquals(5, summary.getScoreDistribution().get("0-25"));
        assertEquals(15, summary.getScoreDistribution().get("76-100"));
    }

    @Test
    void testSetRisksBySeverity() {
        SarVisualizationResult.AssessmentSummary summary = new SarVisualizationResult.AssessmentSummary();
        Map<String, Integer> risksBySeverity = new HashMap<>();
        risksBySeverity.put("critical", 2);
        risksBySeverity.put("high", 8);
        risksBySeverity.put("medium", 12);
        risksBySeverity.put("low", 18);

        summary.setRisksBySeverity(risksBySeverity);

        assertEquals(4, summary.getRisksBySeverity().size());
        assertEquals(2, summary.getRisksBySeverity().get("critical"));
        assertEquals(8, summary.getRisksBySeverity().get("high"));
        assertEquals(12, summary.getRisksBySeverity().get("medium"));
        assertEquals(18, summary.getRisksBySeverity().get("low"));
    }

    @Test
    void testSetAllFields() {
        SarVisualizationResult.AssessmentSummary summary = new SarVisualizationResult.AssessmentSummary();

        summary.setTotalControlsAssessed(100);
        summary.setTotalFindings(50);
        summary.setTotalObservations(200);
        summary.setTotalRisks(25);
        summary.setUniqueFamiliesAssessed(15);

        Map<String, Integer> findings = new HashMap<>();
        findings.put("high", 10);
        summary.setFindingsBySeverity(findings);

        Map<String, Integer> observations = new HashMap<>();
        observations.put("interview", 50);
        summary.setObservationsByType(observations);

        Map<String, Integer> scores = new HashMap<>();
        scores.put("76-100", 30);
        summary.setScoreDistribution(scores);

        Map<String, Integer> risks = new HashMap<>();
        risks.put("medium", 15);
        summary.setRisksBySeverity(risks);

        assertEquals(100, summary.getTotalControlsAssessed());
        assertEquals(50, summary.getTotalFindings());
        assertEquals(200, summary.getTotalObservations());
        assertEquals(25, summary.getTotalRisks());
        assertEquals(15, summary.getUniqueFamiliesAssessed());
        assertEquals(10, summary.getFindingsBySeverity().get("high"));
        assertEquals(50, summary.getObservationsByType().get("interview"));
        assertEquals(30, summary.getScoreDistribution().get("76-100"));
        assertEquals(15, summary.getRisksBySeverity().get("medium"));
    }

    @Test
    void testSetAllFieldsToNull() {
        SarVisualizationResult.AssessmentSummary summary = new SarVisualizationResult.AssessmentSummary();

        summary.setFindingsBySeverity(null);
        summary.setObservationsByType(null);
        summary.setScoreDistribution(null);
        summary.setRisksBySeverity(null);

        assertNull(summary.getFindingsBySeverity());
        assertNull(summary.getObservationsByType());
        assertNull(summary.getScoreDistribution());
        assertNull(summary.getRisksBySeverity());
    }

    @Test
    void testModifyAllFields() {
        SarVisualizationResult.AssessmentSummary summary = new SarVisualizationResult.AssessmentSummary();

        summary.setTotalControlsAssessed(10);
        summary.setTotalFindings(5);
        summary.setTotalObservations(20);
        summary.setTotalRisks(3);
        summary.setUniqueFamiliesAssessed(2);

        summary.setTotalControlsAssessed(100);
        summary.setTotalFindings(50);
        summary.setTotalObservations(200);
        summary.setTotalRisks(30);
        summary.setUniqueFamiliesAssessed(20);

        assertEquals(100, summary.getTotalControlsAssessed());
        assertEquals(50, summary.getTotalFindings());
        assertEquals(200, summary.getTotalObservations());
        assertEquals(30, summary.getTotalRisks());
        assertEquals(20, summary.getUniqueFamiliesAssessed());
    }

    @Test
    void testSetEmptyMaps() {
        SarVisualizationResult.AssessmentSummary summary = new SarVisualizationResult.AssessmentSummary();

        summary.setFindingsBySeverity(new HashMap<>());
        summary.setObservationsByType(new HashMap<>());
        summary.setScoreDistribution(new HashMap<>());
        summary.setRisksBySeverity(new HashMap<>());

        assertTrue(summary.getFindingsBySeverity().isEmpty());
        assertTrue(summary.getObservationsByType().isEmpty());
        assertTrue(summary.getScoreDistribution().isEmpty());
        assertTrue(summary.getRisksBySeverity().isEmpty());
    }

    @Test
    void testWithLargeNumbers() {
        SarVisualizationResult.AssessmentSummary summary = new SarVisualizationResult.AssessmentSummary();

        summary.setTotalControlsAssessed(10000);
        summary.setTotalFindings(5000);
        summary.setTotalObservations(25000);
        summary.setTotalRisks(2500);
        summary.setUniqueFamiliesAssessed(500);

        assertEquals(10000, summary.getTotalControlsAssessed());
        assertEquals(5000, summary.getTotalFindings());
        assertEquals(25000, summary.getTotalObservations());
        assertEquals(2500, summary.getTotalRisks());
        assertEquals(500, summary.getUniqueFamiliesAssessed());
    }

    @Test
    void testWithZeroValues() {
        SarVisualizationResult.AssessmentSummary summary = new SarVisualizationResult.AssessmentSummary();

        summary.setTotalControlsAssessed(0);
        summary.setTotalFindings(0);
        summary.setTotalObservations(0);
        summary.setTotalRisks(0);
        summary.setUniqueFamiliesAssessed(0);

        assertEquals(0, summary.getTotalControlsAssessed());
        assertEquals(0, summary.getTotalFindings());
        assertEquals(0, summary.getTotalObservations());
        assertEquals(0, summary.getTotalRisks());
        assertEquals(0, summary.getUniqueFamiliesAssessed());
    }

    @Test
    void testWithNegativeNumbers() {
        SarVisualizationResult.AssessmentSummary summary = new SarVisualizationResult.AssessmentSummary();

        summary.setTotalControlsAssessed(-1);
        summary.setTotalFindings(-5);
        summary.setTotalObservations(-10);
        summary.setTotalRisks(-2);
        summary.setUniqueFamiliesAssessed(-3);

        assertEquals(-1, summary.getTotalControlsAssessed());
        assertEquals(-5, summary.getTotalFindings());
        assertEquals(-10, summary.getTotalObservations());
        assertEquals(-2, summary.getTotalRisks());
        assertEquals(-3, summary.getUniqueFamiliesAssessed());
    }

    @Test
    void testCompleteAssessmentSummaryScenario() {
        SarVisualizationResult.AssessmentSummary summary = new SarVisualizationResult.AssessmentSummary();

        // Set counts
        summary.setTotalControlsAssessed(325);
        summary.setTotalFindings(45);
        summary.setTotalObservations(180);
        summary.setTotalRisks(12);
        summary.setUniqueFamiliesAssessed(18);

        // Set findings by severity
        Map<String, Integer> findings = new HashMap<>();
        findings.put("critical", 3);
        findings.put("high", 8);
        findings.put("medium", 15);
        findings.put("low", 19);
        summary.setFindingsBySeverity(findings);

        // Set observations by type
        Map<String, Integer> observations = new HashMap<>();
        observations.put("interview", 60);
        observations.put("examination", 70);
        observations.put("test", 50);
        summary.setObservationsByType(observations);

        // Set score distribution
        Map<String, Integer> scores = new HashMap<>();
        scores.put("0-25", 10);
        scores.put("26-50", 45);
        scores.put("51-75", 120);
        scores.put("76-100", 150);
        summary.setScoreDistribution(scores);

        // Set risks by severity
        Map<String, Integer> risks = new HashMap<>();
        risks.put("critical", 1);
        risks.put("high", 2);
        risks.put("medium", 4);
        risks.put("low", 5);
        summary.setRisksBySeverity(risks);

        // Verify all values
        assertEquals(325, summary.getTotalControlsAssessed());
        assertEquals(45, summary.getTotalFindings());
        assertEquals(180, summary.getTotalObservations());
        assertEquals(12, summary.getTotalRisks());
        assertEquals(18, summary.getUniqueFamiliesAssessed());

        assertEquals(4, summary.getFindingsBySeverity().size());
        assertEquals(3, summary.getFindingsBySeverity().get("critical"));
        assertEquals(19, summary.getFindingsBySeverity().get("low"));

        assertEquals(3, summary.getObservationsByType().size());
        assertEquals(60, summary.getObservationsByType().get("interview"));

        assertEquals(4, summary.getScoreDistribution().size());
        assertEquals(150, summary.getScoreDistribution().get("76-100"));

        assertEquals(4, summary.getRisksBySeverity().size());
        assertEquals(1, summary.getRisksBySeverity().get("critical"));
        assertEquals(5, summary.getRisksBySeverity().get("low"));
    }

    @Test
    void testMapWithMultipleEntries() {
        SarVisualizationResult.AssessmentSummary summary = new SarVisualizationResult.AssessmentSummary();

        Map<String, Integer> findings = new HashMap<>();
        for (int i = 1; i <= 10; i++) {
            findings.put("severity-" + i, i * 5);
        }
        summary.setFindingsBySeverity(findings);

        assertEquals(10, summary.getFindingsBySeverity().size());
        assertEquals(25, summary.getFindingsBySeverity().get("severity-5"));
        assertEquals(50, summary.getFindingsBySeverity().get("severity-10"));
    }

    @Test
    void testUpdateExistingMapValues() {
        SarVisualizationResult.AssessmentSummary summary = new SarVisualizationResult.AssessmentSummary();

        Map<String, Integer> findings = new HashMap<>();
        findings.put("high", 5);
        findings.put("low", 10);
        summary.setFindingsBySeverity(findings);

        assertEquals(5, summary.getFindingsBySeverity().get("high"));

        // Update the map
        Map<String, Integer> updatedFindings = new HashMap<>();
        updatedFindings.put("high", 15);
        updatedFindings.put("low", 20);
        updatedFindings.put("critical", 3);
        summary.setFindingsBySeverity(updatedFindings);

        assertEquals(3, summary.getFindingsBySeverity().size());
        assertEquals(15, summary.getFindingsBySeverity().get("high"));
        assertEquals(3, summary.getFindingsBySeverity().get("critical"));
    }
}
