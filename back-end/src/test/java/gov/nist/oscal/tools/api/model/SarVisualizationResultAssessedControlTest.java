package gov.nist.oscal.tools.api.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SarVisualizationResultAssessedControlTest {

    @Test
    void testNoArgsConstructor() {
        SarVisualizationResult.ControlFamilyAssessment.AssessedControl control =
            new SarVisualizationResult.ControlFamilyAssessment.AssessedControl();

        assertNotNull(control);
        assertNull(control.getControlId());
        assertEquals(0, control.getFindingsCount());
        assertEquals(0, control.getObservationsCount());
        assertNull(control.getAssessmentStatus());
    }

    @Test
    void testAllArgsConstructor() {
        SarVisualizationResult.ControlFamilyAssessment.AssessedControl control =
            new SarVisualizationResult.ControlFamilyAssessment.AssessedControl(
                "AC-1",
                3,
                5,
                "Satisfied"
            );

        assertEquals("AC-1", control.getControlId());
        assertEquals(3, control.getFindingsCount());
        assertEquals(5, control.getObservationsCount());
        assertEquals("Satisfied", control.getAssessmentStatus());
    }

    @Test
    void testSetControlId() {
        SarVisualizationResult.ControlFamilyAssessment.AssessedControl control =
            new SarVisualizationResult.ControlFamilyAssessment.AssessedControl();
        control.setControlId("AC-2");
        assertEquals("AC-2", control.getControlId());
    }

    @Test
    void testSetFindingsCount() {
        SarVisualizationResult.ControlFamilyAssessment.AssessedControl control =
            new SarVisualizationResult.ControlFamilyAssessment.AssessedControl();
        control.setFindingsCount(7);
        assertEquals(7, control.getFindingsCount());
    }

    @Test
    void testSetObservationsCount() {
        SarVisualizationResult.ControlFamilyAssessment.AssessedControl control =
            new SarVisualizationResult.ControlFamilyAssessment.AssessedControl();
        control.setObservationsCount(12);
        assertEquals(12, control.getObservationsCount());
    }

    @Test
    void testSetAssessmentStatus() {
        SarVisualizationResult.ControlFamilyAssessment.AssessedControl control =
            new SarVisualizationResult.ControlFamilyAssessment.AssessedControl();
        control.setAssessmentStatus("Not Satisfied");
        assertEquals("Not Satisfied", control.getAssessmentStatus());
    }

    @Test
    void testSetAllFields() {
        SarVisualizationResult.ControlFamilyAssessment.AssessedControl control =
            new SarVisualizationResult.ControlFamilyAssessment.AssessedControl();
        control.setControlId("SC-7");
        control.setFindingsCount(2);
        control.setObservationsCount(8);
        control.setAssessmentStatus("Partially Satisfied");

        assertEquals("SC-7", control.getControlId());
        assertEquals(2, control.getFindingsCount());
        assertEquals(8, control.getObservationsCount());
        assertEquals("Partially Satisfied", control.getAssessmentStatus());
    }

    @Test
    void testSetFieldsToNull() {
        SarVisualizationResult.ControlFamilyAssessment.AssessedControl control =
            new SarVisualizationResult.ControlFamilyAssessment.AssessedControl(
                "AC-1",
                5,
                10,
                "Satisfied"
            );

        control.setControlId(null);
        control.setAssessmentStatus(null);

        assertNull(control.getControlId());
        assertNull(control.getAssessmentStatus());
        assertEquals(5, control.getFindingsCount());
        assertEquals(10, control.getObservationsCount());
    }

    @Test
    void testModifyAllFields() {
        SarVisualizationResult.ControlFamilyAssessment.AssessedControl control =
            new SarVisualizationResult.ControlFamilyAssessment.AssessedControl();

        control.setControlId("AC-1");
        control.setFindingsCount(3);
        control.setObservationsCount(5);
        control.setAssessmentStatus("Satisfied");

        control.setControlId("AC-2");
        control.setFindingsCount(7);
        control.setObservationsCount(9);
        control.setAssessmentStatus("Not Satisfied");

        assertEquals("AC-2", control.getControlId());
        assertEquals(7, control.getFindingsCount());
        assertEquals(9, control.getObservationsCount());
        assertEquals("Not Satisfied", control.getAssessmentStatus());
    }

    @Test
    void testWithZeroCounts() {
        SarVisualizationResult.ControlFamilyAssessment.AssessedControl control =
            new SarVisualizationResult.ControlFamilyAssessment.AssessedControl(
                "AC-1",
                0,
                0,
                "Satisfied"
            );

        assertEquals("AC-1", control.getControlId());
        assertEquals(0, control.getFindingsCount());
        assertEquals(0, control.getObservationsCount());
        assertEquals("Satisfied", control.getAssessmentStatus());
    }

    @Test
    void testWithHighCounts() {
        SarVisualizationResult.ControlFamilyAssessment.AssessedControl control =
            new SarVisualizationResult.ControlFamilyAssessment.AssessedControl();
        control.setControlId("SC-7");
        control.setFindingsCount(99);
        control.setObservationsCount(150);
        control.setAssessmentStatus("Not Satisfied");

        assertEquals(99, control.getFindingsCount());
        assertEquals(150, control.getObservationsCount());
    }

    @Test
    void testCompleteAssessmentScenario() {
        SarVisualizationResult.ControlFamilyAssessment.AssessedControl control =
            new SarVisualizationResult.ControlFamilyAssessment.AssessedControl(
                "AU-2",
                5,
                12,
                "Partially Satisfied"
            );

        assertNotNull(control);
        assertTrue(control.getControlId().startsWith("AU"));
        assertTrue(control.getFindingsCount() > 0);
        assertTrue(control.getObservationsCount() > control.getFindingsCount());
        assertTrue(control.getAssessmentStatus().contains("Satisfied"));
    }

    @Test
    void testWithEmptyStrings() {
        SarVisualizationResult.ControlFamilyAssessment.AssessedControl control =
            new SarVisualizationResult.ControlFamilyAssessment.AssessedControl(
                "",
                1,
                2,
                ""
            );

        assertEquals("", control.getControlId());
        assertEquals("", control.getAssessmentStatus());
        assertEquals(1, control.getFindingsCount());
    }

    @Test
    void testDifferentAssessmentStatuses() {
        String[] statuses = {"Satisfied", "Not Satisfied", "Partially Satisfied", "Not Applicable", "Unknown"};

        for (String status : statuses) {
            SarVisualizationResult.ControlFamilyAssessment.AssessedControl control =
                new SarVisualizationResult.ControlFamilyAssessment.AssessedControl();
            control.setAssessmentStatus(status);
            assertEquals(status, control.getAssessmentStatus());
        }
    }

    @Test
    void testConstructorAndSettersCombined() {
        SarVisualizationResult.ControlFamilyAssessment.AssessedControl control =
            new SarVisualizationResult.ControlFamilyAssessment.AssessedControl(
                "AC-1",
                2,
                4,
                "Satisfied"
            );

        assertEquals("AC-1", control.getControlId());
        assertEquals(2, control.getFindingsCount());

        control.setControlId("AC-2");
        control.setFindingsCount(5);

        assertEquals("AC-2", control.getControlId());
        assertEquals(5, control.getFindingsCount());
    }

    @Test
    void testNegativeCounts() {
        SarVisualizationResult.ControlFamilyAssessment.AssessedControl control =
            new SarVisualizationResult.ControlFamilyAssessment.AssessedControl();
        control.setFindingsCount(-1);
        control.setObservationsCount(-5);

        assertEquals(-1, control.getFindingsCount());
        assertEquals(-5, control.getObservationsCount());
    }

    @Test
    void testWithEnhancementControls() {
        SarVisualizationResult.ControlFamilyAssessment.AssessedControl control =
            new SarVisualizationResult.ControlFamilyAssessment.AssessedControl(
                "AC-2(1)",
                1,
                3,
                "Satisfied"
            );

        assertTrue(control.getControlId().contains("("));
        assertTrue(control.getControlId().contains(")"));
        assertEquals("AC-2(1)", control.getControlId());
    }
}
