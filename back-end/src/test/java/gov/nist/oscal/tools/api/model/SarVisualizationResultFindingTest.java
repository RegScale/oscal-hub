package gov.nist.oscal.tools.api.model;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SarVisualizationResultFindingTest {

    @Test
    void testDefaultConstructor() {
        SarVisualizationResult.Finding finding = new SarVisualizationResult.Finding();

        assertNotNull(finding);
        assertNull(finding.getUuid());
        assertNull(finding.getTitle());
        assertNull(finding.getDescription());
        assertNotNull(finding.getRelatedControls());
        assertNotNull(finding.getRelatedObservations());
        assertEquals(0, finding.getRelatedControls().size());
        assertEquals(0, finding.getRelatedObservations().size());
        assertNull(finding.getScore());
        assertNull(finding.getQualityScore());
        assertNull(finding.getCompletenessScore());
    }

    @Test
    void testSetUuid() {
        SarVisualizationResult.Finding finding = new SarVisualizationResult.Finding();
        finding.setUuid("f1a2b3c4-d5e6-7890-abcd-ef1234567890");
        assertEquals("f1a2b3c4-d5e6-7890-abcd-ef1234567890", finding.getUuid());
    }

    @Test
    void testSetTitle() {
        SarVisualizationResult.Finding finding = new SarVisualizationResult.Finding();
        finding.setTitle("Missing encryption controls");
        assertEquals("Missing encryption controls", finding.getTitle());
    }

    @Test
    void testSetDescription() {
        SarVisualizationResult.Finding finding = new SarVisualizationResult.Finding();
        finding.setDescription("System lacks adequate encryption for data at rest");
        assertTrue(finding.getDescription().contains("encryption"));
    }

    @Test
    void testSetRelatedControls() {
        SarVisualizationResult.Finding finding = new SarVisualizationResult.Finding();
        List<String> controls = Arrays.asList("sc-28", "sc-28.1", "mp-5");
        finding.setRelatedControls(controls);

        assertEquals(3, finding.getRelatedControls().size());
        assertTrue(finding.getRelatedControls().contains("sc-28"));
    }

    @Test
    void testSetRelatedObservations() {
        SarVisualizationResult.Finding finding = new SarVisualizationResult.Finding();
        List<String> observations = Arrays.asList("obs-001", "obs-002", "obs-003");
        finding.setRelatedObservations(observations);

        assertEquals(3, finding.getRelatedObservations().size());
        assertTrue(finding.getRelatedObservations().contains("obs-001"));
    }

    @Test
    void testSetScore() {
        SarVisualizationResult.Finding finding = new SarVisualizationResult.Finding();
        finding.setScore(7.5);
        assertEquals(7.5, finding.getScore());
    }

    @Test
    void testSetQualityScore() {
        SarVisualizationResult.Finding finding = new SarVisualizationResult.Finding();
        finding.setQualityScore(8.2);
        assertEquals(8.2, finding.getQualityScore());
    }

    @Test
    void testSetCompletenessScore() {
        SarVisualizationResult.Finding finding = new SarVisualizationResult.Finding();
        finding.setCompletenessScore(6.8);
        assertEquals(6.8, finding.getCompletenessScore());
    }

    @Test
    void testWithEmptyLists() {
        SarVisualizationResult.Finding finding = new SarVisualizationResult.Finding();
        finding.setRelatedControls(new ArrayList<>());
        finding.setRelatedObservations(new ArrayList<>());

        assertEquals(0, finding.getRelatedControls().size());
        assertEquals(0, finding.getRelatedObservations().size());
    }

    @Test
    void testSetAllFields() {
        SarVisualizationResult.Finding finding = new SarVisualizationResult.Finding();
        finding.setUuid("abc-123-def-456");
        finding.setTitle("Critical security finding");
        finding.setDescription("Multiple authentication controls are not properly implemented");
        finding.setRelatedControls(Arrays.asList("ac-2", "ac-3", "ia-2"));
        finding.setRelatedObservations(Arrays.asList("obs-100", "obs-101"));
        finding.setScore(9.5);
        finding.setQualityScore(8.5);
        finding.setCompletenessScore(7.5);

        assertEquals("abc-123-def-456", finding.getUuid());
        assertEquals("Critical security finding", finding.getTitle());
        assertTrue(finding.getDescription().contains("authentication"));
        assertEquals(3, finding.getRelatedControls().size());
        assertEquals(2, finding.getRelatedObservations().size());
        assertEquals(9.5, finding.getScore());
        assertEquals(8.5, finding.getQualityScore());
        assertEquals(7.5, finding.getCompletenessScore());
    }

    @Test
    void testSetFieldsToNull() {
        SarVisualizationResult.Finding finding = new SarVisualizationResult.Finding();
        finding.setUuid("test");
        finding.setTitle("test");
        finding.setScore(5.0);

        finding.setUuid(null);
        finding.setTitle(null);
        finding.setDescription(null);
        finding.setRelatedControls(null);
        finding.setRelatedObservations(null);
        finding.setScore(null);
        finding.setQualityScore(null);
        finding.setCompletenessScore(null);

        assertNull(finding.getUuid());
        assertNull(finding.getTitle());
        assertNull(finding.getDescription());
        assertNull(finding.getRelatedControls());
        assertNull(finding.getRelatedObservations());
        assertNull(finding.getScore());
        assertNull(finding.getQualityScore());
        assertNull(finding.getCompletenessScore());
    }

    @Test
    void testModifyAllFields() {
        SarVisualizationResult.Finding finding = new SarVisualizationResult.Finding();

        finding.setUuid("old-uuid");
        finding.setTitle("Old title");
        finding.setRelatedControls(Arrays.asList("ac-1"));
        finding.setScore(5.0);

        finding.setUuid("new-uuid");
        finding.setTitle("New title");
        finding.setRelatedControls(Arrays.asList("ac-2", "ac-3"));
        finding.setScore(8.0);

        assertEquals("new-uuid", finding.getUuid());
        assertEquals("New title", finding.getTitle());
        assertEquals(2, finding.getRelatedControls().size());
        assertEquals(8.0, finding.getScore());
    }

    @Test
    void testWithLongDescription() {
        SarVisualizationResult.Finding finding = new SarVisualizationResult.Finding();
        StringBuilder longDesc = new StringBuilder();
        for (int i = 0; i < 50; i++) {
            longDesc.append("This finding describes a critical security vulnerability. ");
        }
        finding.setDescription(longDesc.toString());
        assertTrue(finding.getDescription().length() > 1000);
    }

    @Test
    void testWithManyRelatedControls() {
        SarVisualizationResult.Finding finding = new SarVisualizationResult.Finding();
        List<String> controls = new ArrayList<>();
        for (int i = 1; i <= 20; i++) {
            controls.add("control-" + i);
        }
        finding.setRelatedControls(controls);
        assertEquals(20, finding.getRelatedControls().size());
    }

    @Test
    void testWithManyRelatedObservations() {
        SarVisualizationResult.Finding finding = new SarVisualizationResult.Finding();
        List<String> observations = new ArrayList<>();
        for (int i = 1; i <= 15; i++) {
            observations.add("observation-" + i);
        }
        finding.setRelatedObservations(observations);
        assertEquals(15, finding.getRelatedObservations().size());
    }

    @Test
    void testScoreBoundaries() {
        SarVisualizationResult.Finding finding = new SarVisualizationResult.Finding();

        finding.setScore(0.0);
        assertEquals(0.0, finding.getScore());

        finding.setScore(10.0);
        assertEquals(10.0, finding.getScore());

        finding.setScore(5.5);
        assertEquals(5.5, finding.getScore());
    }

    @Test
    void testCompleteFindingScenario() {
        SarVisualizationResult.Finding finding = new SarVisualizationResult.Finding();
        finding.setUuid("finding-2025-001");
        finding.setTitle("Inadequate Access Controls");
        finding.setDescription("System does not implement proper role-based access controls for administrative functions");
        finding.setRelatedControls(Arrays.asList("ac-2", "ac-3", "ac-5", "ac-6"));
        finding.setRelatedObservations(Arrays.asList("obs-2025-010", "obs-2025-011", "obs-2025-012"));
        finding.setScore(8.5);
        finding.setQualityScore(7.8);
        finding.setCompletenessScore(8.2);

        assertTrue(finding.getUuid().contains("2025"));
        assertTrue(finding.getTitle().contains("Access Controls"));
        assertEquals(4, finding.getRelatedControls().size());
        assertEquals(3, finding.getRelatedObservations().size());
        assertTrue(finding.getScore() > 8.0);
    }

    @Test
    void testWithSpecialCharactersInTitle() {
        SarVisualizationResult.Finding finding = new SarVisualizationResult.Finding();
        finding.setTitle("Finding #2025-Q1: Missing Controls (AC-2, AC-3) - Critical");
        assertTrue(finding.getTitle().contains("#"));
        assertTrue(finding.getTitle().contains(":"));
        assertTrue(finding.getTitle().contains("-"));
    }

    @Test
    void testOnlyRequiredFields() {
        SarVisualizationResult.Finding finding = new SarVisualizationResult.Finding();
        finding.setUuid("minimal-finding");
        finding.setTitle("Minimal finding");

        assertEquals("minimal-finding", finding.getUuid());
        assertEquals("Minimal finding", finding.getTitle());
        assertNull(finding.getDescription());
        assertNull(finding.getScore());
    }

    @Test
    void testNegativeScores() {
        SarVisualizationResult.Finding finding = new SarVisualizationResult.Finding();
        finding.setScore(-1.0);
        finding.setQualityScore(-0.5);

        assertEquals(-1.0, finding.getScore());
        assertEquals(-0.5, finding.getQualityScore());
    }

    @Test
    void testVeryHighScores() {
        SarVisualizationResult.Finding finding = new SarVisualizationResult.Finding();
        finding.setScore(100.0);
        finding.setQualityScore(99.9);
        finding.setCompletenessScore(98.5);

        assertEquals(100.0, finding.getScore());
        assertEquals(99.9, finding.getQualityScore());
        assertEquals(98.5, finding.getCompletenessScore());
    }
}
