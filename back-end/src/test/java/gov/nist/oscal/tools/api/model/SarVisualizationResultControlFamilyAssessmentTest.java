package gov.nist.oscal.tools.api.model;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SarVisualizationResultControlFamilyAssessmentTest {

    @Test
    void testNoArgsConstructor() {
        SarVisualizationResult.ControlFamilyAssessment assessment =
            new SarVisualizationResult.ControlFamilyAssessment();

        assertNotNull(assessment);
        assertNull(assessment.getFamilyId());
        assertNull(assessment.getFamilyName());
        assertEquals(0, assessment.getTotalControlsAssessed());
        assertEquals(0, assessment.getTotalFindings());
        assertEquals(0, assessment.getTotalObservations());
        assertNotNull(assessment.getAssessedControls());
        assertTrue(assessment.getAssessedControls().isEmpty());
    }

    @Test
    void testSetFamilyId() {
        SarVisualizationResult.ControlFamilyAssessment assessment =
            new SarVisualizationResult.ControlFamilyAssessment();
        assessment.setFamilyId("AC");
        assertEquals("AC", assessment.getFamilyId());
    }

    @Test
    void testSetFamilyName() {
        SarVisualizationResult.ControlFamilyAssessment assessment =
            new SarVisualizationResult.ControlFamilyAssessment();
        assessment.setFamilyName("Access Control");
        assertEquals("Access Control", assessment.getFamilyName());
    }

    @Test
    void testSetTotalControlsAssessed() {
        SarVisualizationResult.ControlFamilyAssessment assessment =
            new SarVisualizationResult.ControlFamilyAssessment();
        assessment.setTotalControlsAssessed(25);
        assertEquals(25, assessment.getTotalControlsAssessed());
    }

    @Test
    void testSetTotalFindings() {
        SarVisualizationResult.ControlFamilyAssessment assessment =
            new SarVisualizationResult.ControlFamilyAssessment();
        assessment.setTotalFindings(12);
        assertEquals(12, assessment.getTotalFindings());
    }

    @Test
    void testSetTotalObservations() {
        SarVisualizationResult.ControlFamilyAssessment assessment =
            new SarVisualizationResult.ControlFamilyAssessment();
        assessment.setTotalObservations(45);
        assertEquals(45, assessment.getTotalObservations());
    }

    @Test
    void testSetAssessedControls() {
        SarVisualizationResult.ControlFamilyAssessment assessment =
            new SarVisualizationResult.ControlFamilyAssessment();

        List<SarVisualizationResult.ControlFamilyAssessment.AssessedControl> controls = new ArrayList<>();
        controls.add(new SarVisualizationResult.ControlFamilyAssessment.AssessedControl(
            "AC-1", 2, 5, "compliant"
        ));
        controls.add(new SarVisualizationResult.ControlFamilyAssessment.AssessedControl(
            "AC-2", 1, 3, "non-compliant"
        ));

        assessment.setAssessedControls(controls);

        assertEquals(2, assessment.getAssessedControls().size());
        assertEquals("AC-1", assessment.getAssessedControls().get(0).getControlId());
        assertEquals("AC-2", assessment.getAssessedControls().get(1).getControlId());
    }

    @Test
    void testSetAllFields() {
        SarVisualizationResult.ControlFamilyAssessment assessment =
            new SarVisualizationResult.ControlFamilyAssessment();

        List<SarVisualizationResult.ControlFamilyAssessment.AssessedControl> controls = new ArrayList<>();
        controls.add(new SarVisualizationResult.ControlFamilyAssessment.AssessedControl(
            "AC-1", 3, 8, "compliant"
        ));

        assessment.setFamilyId("AC");
        assessment.setFamilyName("Access Control");
        assessment.setTotalControlsAssessed(50);
        assessment.setTotalFindings(20);
        assessment.setTotalObservations(100);
        assessment.setAssessedControls(controls);

        assertEquals("AC", assessment.getFamilyId());
        assertEquals("Access Control", assessment.getFamilyName());
        assertEquals(50, assessment.getTotalControlsAssessed());
        assertEquals(20, assessment.getTotalFindings());
        assertEquals(100, assessment.getTotalObservations());
        assertEquals(1, assessment.getAssessedControls().size());
    }

    @Test
    void testSetAllFieldsToNull() {
        SarVisualizationResult.ControlFamilyAssessment assessment =
            new SarVisualizationResult.ControlFamilyAssessment();

        assessment.setFamilyId(null);
        assessment.setFamilyName(null);
        assessment.setAssessedControls(null);

        assertNull(assessment.getFamilyId());
        assertNull(assessment.getFamilyName());
        assertNull(assessment.getAssessedControls());
    }

    @Test
    void testModifyAllFields() {
        SarVisualizationResult.ControlFamilyAssessment assessment =
            new SarVisualizationResult.ControlFamilyAssessment();

        assessment.setFamilyId("AC");
        assessment.setFamilyName("Access Control");
        assessment.setTotalControlsAssessed(10);

        assessment.setFamilyId("AU");
        assessment.setFamilyName("Audit and Accountability");
        assessment.setTotalControlsAssessed(20);

        assertEquals("AU", assessment.getFamilyId());
        assertEquals("Audit and Accountability", assessment.getFamilyName());
        assertEquals(20, assessment.getTotalControlsAssessed());
    }

    @Test
    void testWithZeroCounts() {
        SarVisualizationResult.ControlFamilyAssessment assessment =
            new SarVisualizationResult.ControlFamilyAssessment();

        assessment.setTotalControlsAssessed(0);
        assessment.setTotalFindings(0);
        assessment.setTotalObservations(0);

        assertEquals(0, assessment.getTotalControlsAssessed());
        assertEquals(0, assessment.getTotalFindings());
        assertEquals(0, assessment.getTotalObservations());
    }

    @Test
    void testWithLargeCounts() {
        SarVisualizationResult.ControlFamilyAssessment assessment =
            new SarVisualizationResult.ControlFamilyAssessment();

        assessment.setTotalControlsAssessed(10000);
        assessment.setTotalFindings(5000);
        assessment.setTotalObservations(25000);

        assertEquals(10000, assessment.getTotalControlsAssessed());
        assertEquals(5000, assessment.getTotalFindings());
        assertEquals(25000, assessment.getTotalObservations());
    }

    @Test
    void testWithEmptyString() {
        SarVisualizationResult.ControlFamilyAssessment assessment =
            new SarVisualizationResult.ControlFamilyAssessment();

        assessment.setFamilyId("");
        assessment.setFamilyName("");

        assertEquals("", assessment.getFamilyId());
        assertEquals("", assessment.getFamilyName());
    }

    @Test
    void testWithEmptyAssessedControls() {
        SarVisualizationResult.ControlFamilyAssessment assessment =
            new SarVisualizationResult.ControlFamilyAssessment();
        assessment.setAssessedControls(new ArrayList<>());

        assertTrue(assessment.getAssessedControls().isEmpty());
        assertEquals(0, assessment.getAssessedControls().size());
    }

    @Test
    void testWithMultipleAssessedControls() {
        SarVisualizationResult.ControlFamilyAssessment assessment =
            new SarVisualizationResult.ControlFamilyAssessment();

        List<SarVisualizationResult.ControlFamilyAssessment.AssessedControl> controls = new ArrayList<>();
        for (int i = 1; i <= 25; i++) {
            controls.add(new SarVisualizationResult.ControlFamilyAssessment.AssessedControl(
                "AC-" + i, i, i * 2, "compliant"
            ));
        }

        assessment.setAssessedControls(controls);

        assertEquals(25, assessment.getAssessedControls().size());
        assertEquals("AC-1", assessment.getAssessedControls().get(0).getControlId());
        assertEquals("AC-25", assessment.getAssessedControls().get(24).getControlId());
    }

    @Test
    void testAccessControlFamily() {
        SarVisualizationResult.ControlFamilyAssessment assessment =
            new SarVisualizationResult.ControlFamilyAssessment();

        assessment.setFamilyId("AC");
        assessment.setFamilyName("Access Control");

        assertEquals("AC", assessment.getFamilyId());
        assertEquals("Access Control", assessment.getFamilyName());
    }

    @Test
    void testAuditFamily() {
        SarVisualizationResult.ControlFamilyAssessment assessment =
            new SarVisualizationResult.ControlFamilyAssessment();

        assessment.setFamilyId("AU");
        assessment.setFamilyName("Audit and Accountability");

        assertEquals("AU", assessment.getFamilyId());
        assertEquals("Audit and Accountability", assessment.getFamilyName());
    }

    @Test
    void testCompleteControlFamilyAssessmentScenario() {
        SarVisualizationResult.ControlFamilyAssessment assessment =
            new SarVisualizationResult.ControlFamilyAssessment();

        // Set family information
        assessment.setFamilyId("AC");
        assessment.setFamilyName("Access Control");
        assessment.setTotalControlsAssessed(22);
        assessment.setTotalFindings(15);
        assessment.setTotalObservations(67);

        // Add assessed controls
        List<SarVisualizationResult.ControlFamilyAssessment.AssessedControl> controls = new ArrayList<>();
        controls.add(new SarVisualizationResult.ControlFamilyAssessment.AssessedControl(
            "AC-1", 2, 8, "compliant"
        ));
        controls.add(new SarVisualizationResult.ControlFamilyAssessment.AssessedControl(
            "AC-2", 5, 12, "non-compliant"
        ));
        controls.add(new SarVisualizationResult.ControlFamilyAssessment.AssessedControl(
            "AC-3", 1, 7, "compliant"
        ));
        controls.add(new SarVisualizationResult.ControlFamilyAssessment.AssessedControl(
            "AC-6", 3, 10, "partially-compliant"
        ));

        assessment.setAssessedControls(controls);

        // Verify all fields
        assertEquals("AC", assessment.getFamilyId());
        assertEquals("Access Control", assessment.getFamilyName());
        assertEquals(22, assessment.getTotalControlsAssessed());
        assertEquals(15, assessment.getTotalFindings());
        assertEquals(67, assessment.getTotalObservations());
        assertEquals(4, assessment.getAssessedControls().size());

        // Verify assessed controls
        assertEquals("AC-2", assessment.getAssessedControls().get(1).getControlId());
        assertEquals(5, assessment.getAssessedControls().get(1).getFindingsCount());
        assertEquals(12, assessment.getAssessedControls().get(1).getObservationsCount());
        assertEquals("non-compliant", assessment.getAssessedControls().get(1).getAssessmentStatus());
    }

    @Test
    void testUpdateAssessedControls() {
        SarVisualizationResult.ControlFamilyAssessment assessment =
            new SarVisualizationResult.ControlFamilyAssessment();

        List<SarVisualizationResult.ControlFamilyAssessment.AssessedControl> initialControls = new ArrayList<>();
        initialControls.add(new SarVisualizationResult.ControlFamilyAssessment.AssessedControl(
            "AC-1", 1, 3, "compliant"
        ));
        assessment.setAssessedControls(initialControls);
        assertEquals(1, assessment.getAssessedControls().size());

        List<SarVisualizationResult.ControlFamilyAssessment.AssessedControl> updatedControls = new ArrayList<>();
        updatedControls.add(new SarVisualizationResult.ControlFamilyAssessment.AssessedControl(
            "AC-1", 1, 3, "compliant"
        ));
        updatedControls.add(new SarVisualizationResult.ControlFamilyAssessment.AssessedControl(
            "AC-2", 2, 4, "compliant"
        ));
        assessment.setAssessedControls(updatedControls);
        assertEquals(2, assessment.getAssessedControls().size());
    }

    @Test
    void testWithNegativeCounts() {
        SarVisualizationResult.ControlFamilyAssessment assessment =
            new SarVisualizationResult.ControlFamilyAssessment();

        assessment.setTotalControlsAssessed(-1);
        assessment.setTotalFindings(-5);
        assessment.setTotalObservations(-10);

        assertEquals(-1, assessment.getTotalControlsAssessed());
        assertEquals(-5, assessment.getTotalFindings());
        assertEquals(-10, assessment.getTotalObservations());
    }

    @Test
    void testWithSpecialCharacters() {
        SarVisualizationResult.ControlFamilyAssessment assessment =
            new SarVisualizationResult.ControlFamilyAssessment();

        assessment.setFamilyId("AC-<test>");
        assessment.setFamilyName("Access Control & \"Security\"");

        assertEquals("AC-<test>", assessment.getFamilyId());
        assertEquals("Access Control & \"Security\"", assessment.getFamilyName());
    }
}
