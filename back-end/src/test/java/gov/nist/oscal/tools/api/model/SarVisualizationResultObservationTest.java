package gov.nist.oscal.tools.api.model;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SarVisualizationResultObservationTest {

    @Test
    void testNoArgsConstructor() {
        SarVisualizationResult.Observation observation = new SarVisualizationResult.Observation();

        assertNotNull(observation);
        assertNull(observation.getUuid());
        assertNull(observation.getTitle());
        assertNull(observation.getDescription());
        assertNotNull(observation.getRelatedControls());
        assertTrue(observation.getRelatedControls().isEmpty());
        assertNull(observation.getObservationType());
        assertNull(observation.getOverallScore());
        assertNull(observation.getQualityScore());
        assertNull(observation.getCompletenessScore());
    }

    @Test
    void testSetUuid() {
        SarVisualizationResult.Observation observation = new SarVisualizationResult.Observation();
        observation.setUuid("550e8400-e29b-41d4-a716-446655440000");
        assertEquals("550e8400-e29b-41d4-a716-446655440000", observation.getUuid());
    }

    @Test
    void testSetTitle() {
        SarVisualizationResult.Observation observation = new SarVisualizationResult.Observation();
        observation.setTitle("Security Control Implementation Review");
        assertEquals("Security Control Implementation Review", observation.getTitle());
    }

    @Test
    void testSetDescription() {
        SarVisualizationResult.Observation observation = new SarVisualizationResult.Observation();
        observation.setDescription("Observed implementation of access control mechanisms");
        assertEquals("Observed implementation of access control mechanisms", observation.getDescription());
    }

    @Test
    void testSetRelatedControls() {
        SarVisualizationResult.Observation observation = new SarVisualizationResult.Observation();
        List<String> controls = Arrays.asList("AC-1", "AC-2", "AC-3");
        observation.setRelatedControls(controls);

        assertEquals(3, observation.getRelatedControls().size());
        assertTrue(observation.getRelatedControls().contains("AC-1"));
        assertTrue(observation.getRelatedControls().contains("AC-2"));
        assertTrue(observation.getRelatedControls().contains("AC-3"));
    }

    @Test
    void testSetObservationType() {
        SarVisualizationResult.Observation observation = new SarVisualizationResult.Observation();
        observation.setObservationType("interview");
        assertEquals("interview", observation.getObservationType());
    }

    @Test
    void testSetOverallScore() {
        SarVisualizationResult.Observation observation = new SarVisualizationResult.Observation();
        observation.setOverallScore(85.5);
        assertEquals(85.5, observation.getOverallScore());
    }

    @Test
    void testSetQualityScore() {
        SarVisualizationResult.Observation observation = new SarVisualizationResult.Observation();
        observation.setQualityScore(92.3);
        assertEquals(92.3, observation.getQualityScore());
    }

    @Test
    void testSetCompletenessScore() {
        SarVisualizationResult.Observation observation = new SarVisualizationResult.Observation();
        observation.setCompletenessScore(78.9);
        assertEquals(78.9, observation.getCompletenessScore());
    }

    @Test
    void testSetAllFields() {
        SarVisualizationResult.Observation observation = new SarVisualizationResult.Observation();

        observation.setUuid("obs-001");
        observation.setTitle("Test Observation");
        observation.setDescription("Test description");
        observation.setRelatedControls(Arrays.asList("AC-1", "AC-2"));
        observation.setObservationType("examination");
        observation.setOverallScore(90.0);
        observation.setQualityScore(95.0);
        observation.setCompletenessScore(85.0);

        assertEquals("obs-001", observation.getUuid());
        assertEquals("Test Observation", observation.getTitle());
        assertEquals("Test description", observation.getDescription());
        assertEquals(2, observation.getRelatedControls().size());
        assertEquals("examination", observation.getObservationType());
        assertEquals(90.0, observation.getOverallScore());
        assertEquals(95.0, observation.getQualityScore());
        assertEquals(85.0, observation.getCompletenessScore());
    }

    @Test
    void testSetAllFieldsToNull() {
        SarVisualizationResult.Observation observation = new SarVisualizationResult.Observation();

        observation.setUuid(null);
        observation.setTitle(null);
        observation.setDescription(null);
        observation.setRelatedControls(null);
        observation.setObservationType(null);
        observation.setOverallScore(null);
        observation.setQualityScore(null);
        observation.setCompletenessScore(null);

        assertNull(observation.getUuid());
        assertNull(observation.getTitle());
        assertNull(observation.getDescription());
        assertNull(observation.getRelatedControls());
        assertNull(observation.getObservationType());
        assertNull(observation.getOverallScore());
        assertNull(observation.getQualityScore());
        assertNull(observation.getCompletenessScore());
    }

    @Test
    void testModifyAllFields() {
        SarVisualizationResult.Observation observation = new SarVisualizationResult.Observation();

        observation.setUuid("first-uuid");
        observation.setTitle("First Title");
        observation.setOverallScore(50.0);

        observation.setUuid("second-uuid");
        observation.setTitle("Second Title");
        observation.setOverallScore(100.0);

        assertEquals("second-uuid", observation.getUuid());
        assertEquals("Second Title", observation.getTitle());
        assertEquals(100.0, observation.getOverallScore());
    }

    @Test
    void testWithEmptyStrings() {
        SarVisualizationResult.Observation observation = new SarVisualizationResult.Observation();

        observation.setUuid("");
        observation.setTitle("");
        observation.setDescription("");
        observation.setObservationType("");

        assertEquals("", observation.getUuid());
        assertEquals("", observation.getTitle());
        assertEquals("", observation.getDescription());
        assertEquals("", observation.getObservationType());
    }

    @Test
    void testWithEmptyRelatedControls() {
        SarVisualizationResult.Observation observation = new SarVisualizationResult.Observation();
        observation.setRelatedControls(new ArrayList<>());

        assertTrue(observation.getRelatedControls().isEmpty());
        assertEquals(0, observation.getRelatedControls().size());
    }

    @Test
    void testWithZeroScores() {
        SarVisualizationResult.Observation observation = new SarVisualizationResult.Observation();

        observation.setOverallScore(0.0);
        observation.setQualityScore(0.0);
        observation.setCompletenessScore(0.0);

        assertEquals(0.0, observation.getOverallScore());
        assertEquals(0.0, observation.getQualityScore());
        assertEquals(0.0, observation.getCompletenessScore());
    }

    @Test
    void testWithNegativeScores() {
        SarVisualizationResult.Observation observation = new SarVisualizationResult.Observation();

        observation.setOverallScore(-10.0);
        observation.setQualityScore(-5.5);
        observation.setCompletenessScore(-15.75);

        assertEquals(-10.0, observation.getOverallScore());
        assertEquals(-5.5, observation.getQualityScore());
        assertEquals(-15.75, observation.getCompletenessScore());
    }

    @Test
    void testWithMaxScores() {
        SarVisualizationResult.Observation observation = new SarVisualizationResult.Observation();

        observation.setOverallScore(100.0);
        observation.setQualityScore(100.0);
        observation.setCompletenessScore(100.0);

        assertEquals(100.0, observation.getOverallScore());
        assertEquals(100.0, observation.getQualityScore());
        assertEquals(100.0, observation.getCompletenessScore());
    }

    @Test
    void testWithDecimalScores() {
        SarVisualizationResult.Observation observation = new SarVisualizationResult.Observation();

        observation.setOverallScore(87.65);
        observation.setQualityScore(93.42);
        observation.setCompletenessScore(81.98);

        assertEquals(87.65, observation.getOverallScore());
        assertEquals(93.42, observation.getQualityScore());
        assertEquals(81.98, observation.getCompletenessScore());
    }

    @Test
    void testWithLongDescription() {
        SarVisualizationResult.Observation observation = new SarVisualizationResult.Observation();
        String longDesc = "A".repeat(5000);

        observation.setDescription(longDesc);

        assertEquals(longDesc, observation.getDescription());
        assertEquals(5000, observation.getDescription().length());
    }

    @Test
    void testWithMultipleRelatedControls() {
        SarVisualizationResult.Observation observation = new SarVisualizationResult.Observation();
        List<String> controls = new ArrayList<>();

        for (int i = 1; i <= 50; i++) {
            controls.add("AC-" + i);
        }

        observation.setRelatedControls(controls);

        assertEquals(50, observation.getRelatedControls().size());
        assertTrue(observation.getRelatedControls().contains("AC-1"));
        assertTrue(observation.getRelatedControls().contains("AC-50"));
    }

    @Test
    void testInterviewObservationType() {
        SarVisualizationResult.Observation observation = new SarVisualizationResult.Observation();
        observation.setObservationType("interview");
        assertEquals("interview", observation.getObservationType());
    }

    @Test
    void testExaminationObservationType() {
        SarVisualizationResult.Observation observation = new SarVisualizationResult.Observation();
        observation.setObservationType("examination");
        assertEquals("examination", observation.getObservationType());
    }

    @Test
    void testTestObservationType() {
        SarVisualizationResult.Observation observation = new SarVisualizationResult.Observation();
        observation.setObservationType("test");
        assertEquals("test", observation.getObservationType());
    }

    @Test
    void testCompleteObservationScenario() {
        SarVisualizationResult.Observation observation = new SarVisualizationResult.Observation();

        observation.setUuid("550e8400-e29b-41d4-a716-446655440000");
        observation.setTitle("Access Control Implementation Assessment");
        observation.setDescription(
            "Conducted thorough examination of access control mechanisms. " +
            "Verified that user authentication requires multi-factor authentication. " +
            "Confirmed that role-based access control is properly implemented."
        );
        observation.setRelatedControls(Arrays.asList(
            "AC-1", "AC-2", "AC-3", "AC-6", "AC-7", "IA-2"
        ));
        observation.setObservationType("examination");
        observation.setOverallScore(88.5);
        observation.setQualityScore(92.0);
        observation.setCompletenessScore(85.0);

        assertEquals("550e8400-e29b-41d4-a716-446655440000", observation.getUuid());
        assertEquals("Access Control Implementation Assessment", observation.getTitle());
        assertTrue(observation.getDescription().contains("multi-factor authentication"));
        assertEquals(6, observation.getRelatedControls().size());
        assertTrue(observation.getRelatedControls().contains("IA-2"));
        assertEquals("examination", observation.getObservationType());
        assertTrue(observation.getOverallScore() > 85.0);
        assertTrue(observation.getQualityScore() > 90.0);
        assertTrue(observation.getCompletenessScore() >= 85.0);
    }

    @Test
    void testUpdateRelatedControls() {
        SarVisualizationResult.Observation observation = new SarVisualizationResult.Observation();

        List<String> initialControls = Arrays.asList("AC-1", "AC-2");
        observation.setRelatedControls(initialControls);
        assertEquals(2, observation.getRelatedControls().size());

        List<String> updatedControls = Arrays.asList("AC-1", "AC-2", "AC-3", "AC-4");
        observation.setRelatedControls(updatedControls);
        assertEquals(4, observation.getRelatedControls().size());
        assertTrue(observation.getRelatedControls().contains("AC-4"));
    }

    @Test
    void testWithSpecialCharacters() {
        SarVisualizationResult.Observation observation = new SarVisualizationResult.Observation();

        observation.setTitle("Test <Observation> & \"Assessment\"");
        observation.setDescription("Description with special chars: @#$%^&*()");

        assertEquals("Test <Observation> & \"Assessment\"", observation.getTitle());
        assertEquals("Description with special chars: @#$%^&*()", observation.getDescription());
    }
}
