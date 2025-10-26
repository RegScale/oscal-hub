package gov.nist.oscal.tools.api.model;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ProfileVisualizationResultControlFamilyInfoTest {

    @Test
    void testNoArgsConstructor() {
        ProfileVisualizationResult.ControlFamilyInfo familyInfo =
            new ProfileVisualizationResult.ControlFamilyInfo();

        assertNotNull(familyInfo);
        assertNull(familyInfo.getFamilyId());
        assertNull(familyInfo.getFamilyName());
        assertEquals(0, familyInfo.getIncludedCount());
        assertEquals(0, familyInfo.getExcludedCount());
        assertNotNull(familyInfo.getIncludedControls());
        assertNotNull(familyInfo.getExcludedControls());
        assertTrue(familyInfo.getIncludedControls().isEmpty());
        assertTrue(familyInfo.getExcludedControls().isEmpty());
    }

    @Test
    void testSetFamilyId() {
        ProfileVisualizationResult.ControlFamilyInfo familyInfo =
            new ProfileVisualizationResult.ControlFamilyInfo();
        familyInfo.setFamilyId("AC");
        assertEquals("AC", familyInfo.getFamilyId());
    }

    @Test
    void testSetFamilyName() {
        ProfileVisualizationResult.ControlFamilyInfo familyInfo =
            new ProfileVisualizationResult.ControlFamilyInfo();
        familyInfo.setFamilyName("Access Control");
        assertEquals("Access Control", familyInfo.getFamilyName());
    }

    @Test
    void testSetIncludedCount() {
        ProfileVisualizationResult.ControlFamilyInfo familyInfo =
            new ProfileVisualizationResult.ControlFamilyInfo();
        familyInfo.setIncludedCount(25);
        assertEquals(25, familyInfo.getIncludedCount());
    }

    @Test
    void testSetExcludedCount() {
        ProfileVisualizationResult.ControlFamilyInfo familyInfo =
            new ProfileVisualizationResult.ControlFamilyInfo();
        familyInfo.setExcludedCount(5);
        assertEquals(5, familyInfo.getExcludedCount());
    }

    @Test
    void testSetIncludedControls() {
        ProfileVisualizationResult.ControlFamilyInfo familyInfo =
            new ProfileVisualizationResult.ControlFamilyInfo();
        List<String> controls = Arrays.asList("AC-1", "AC-2", "AC-3");
        familyInfo.setIncludedControls(controls);
        assertEquals(controls, familyInfo.getIncludedControls());
        assertEquals(3, familyInfo.getIncludedControls().size());
    }

    @Test
    void testSetExcludedControls() {
        ProfileVisualizationResult.ControlFamilyInfo familyInfo =
            new ProfileVisualizationResult.ControlFamilyInfo();
        List<String> controls = Arrays.asList("AC-10", "AC-15");
        familyInfo.setExcludedControls(controls);
        assertEquals(controls, familyInfo.getExcludedControls());
        assertEquals(2, familyInfo.getExcludedControls().size());
    }

    @Test
    void testSetAllFields() {
        ProfileVisualizationResult.ControlFamilyInfo familyInfo =
            new ProfileVisualizationResult.ControlFamilyInfo();

        familyInfo.setFamilyId("SC");
        familyInfo.setFamilyName("System and Communications Protection");
        familyInfo.setIncludedCount(10);
        familyInfo.setExcludedCount(2);
        familyInfo.setIncludedControls(Arrays.asList("SC-1", "SC-2", "SC-7"));
        familyInfo.setExcludedControls(Arrays.asList("SC-20"));

        assertEquals("SC", familyInfo.getFamilyId());
        assertEquals("System and Communications Protection", familyInfo.getFamilyName());
        assertEquals(10, familyInfo.getIncludedCount());
        assertEquals(2, familyInfo.getExcludedCount());
        assertEquals(3, familyInfo.getIncludedControls().size());
        assertEquals(1, familyInfo.getExcludedControls().size());
    }

    @Test
    void testSetFieldsToNull() {
        ProfileVisualizationResult.ControlFamilyInfo familyInfo =
            new ProfileVisualizationResult.ControlFamilyInfo();

        familyInfo.setFamilyId("AU");
        familyInfo.setFamilyName("Audit and Accountability");
        familyInfo.setIncludedControls(Arrays.asList("AU-1"));

        familyInfo.setFamilyId(null);
        familyInfo.setFamilyName(null);
        familyInfo.setIncludedControls(null);
        familyInfo.setExcludedControls(null);

        assertNull(familyInfo.getFamilyId());
        assertNull(familyInfo.getFamilyName());
        assertNull(familyInfo.getIncludedControls());
        assertNull(familyInfo.getExcludedControls());
    }

    @Test
    void testModifyAllFields() {
        ProfileVisualizationResult.ControlFamilyInfo familyInfo =
            new ProfileVisualizationResult.ControlFamilyInfo();

        familyInfo.setFamilyId("AC");
        familyInfo.setFamilyName("Access Control");
        familyInfo.setIncludedCount(5);
        familyInfo.setExcludedCount(1);

        familyInfo.setFamilyId("AU");
        familyInfo.setFamilyName("Audit and Accountability");
        familyInfo.setIncludedCount(10);
        familyInfo.setExcludedCount(2);

        assertEquals("AU", familyInfo.getFamilyId());
        assertEquals("Audit and Accountability", familyInfo.getFamilyName());
        assertEquals(10, familyInfo.getIncludedCount());
        assertEquals(2, familyInfo.getExcludedCount());
    }

    @Test
    void testWithEmptyStrings() {
        ProfileVisualizationResult.ControlFamilyInfo familyInfo =
            new ProfileVisualizationResult.ControlFamilyInfo();
        familyInfo.setFamilyId("");
        familyInfo.setFamilyName("");

        assertEquals("", familyInfo.getFamilyId());
        assertEquals("", familyInfo.getFamilyName());
    }

    @Test
    void testWithZeroCounts() {
        ProfileVisualizationResult.ControlFamilyInfo familyInfo =
            new ProfileVisualizationResult.ControlFamilyInfo();
        familyInfo.setIncludedCount(0);
        familyInfo.setExcludedCount(0);

        assertEquals(0, familyInfo.getIncludedCount());
        assertEquals(0, familyInfo.getExcludedCount());
    }

    @Test
    void testWithHighCounts() {
        ProfileVisualizationResult.ControlFamilyInfo familyInfo =
            new ProfileVisualizationResult.ControlFamilyInfo();
        familyInfo.setIncludedCount(150);
        familyInfo.setExcludedCount(50);

        assertEquals(150, familyInfo.getIncludedCount());
        assertEquals(50, familyInfo.getExcludedCount());
    }

    @Test
    void testWithEmptyLists() {
        ProfileVisualizationResult.ControlFamilyInfo familyInfo =
            new ProfileVisualizationResult.ControlFamilyInfo();
        familyInfo.setIncludedControls(new ArrayList<>());
        familyInfo.setExcludedControls(new ArrayList<>());

        assertNotNull(familyInfo.getIncludedControls());
        assertNotNull(familyInfo.getExcludedControls());
        assertTrue(familyInfo.getIncludedControls().isEmpty());
        assertTrue(familyInfo.getExcludedControls().isEmpty());
    }

    @Test
    void testWithSingleItemLists() {
        ProfileVisualizationResult.ControlFamilyInfo familyInfo =
            new ProfileVisualizationResult.ControlFamilyInfo();
        familyInfo.setIncludedControls(Arrays.asList("AC-1"));
        familyInfo.setExcludedControls(Arrays.asList("AC-25"));

        assertEquals(1, familyInfo.getIncludedControls().size());
        assertEquals(1, familyInfo.getExcludedControls().size());
        assertEquals("AC-1", familyInfo.getIncludedControls().get(0));
        assertEquals("AC-25", familyInfo.getExcludedControls().get(0));
    }

    @Test
    void testWithLargeLists() {
        ProfileVisualizationResult.ControlFamilyInfo familyInfo =
            new ProfileVisualizationResult.ControlFamilyInfo();

        List<String> included = new ArrayList<>();
        for (int i = 1; i <= 50; i++) {
            included.add("AC-" + i);
        }
        familyInfo.setIncludedControls(included);

        assertEquals(50, familyInfo.getIncludedControls().size());
        assertTrue(familyInfo.getIncludedControls().contains("AC-1"));
        assertTrue(familyInfo.getIncludedControls().contains("AC-50"));
    }

    @Test
    void testAccessControlFamily() {
        ProfileVisualizationResult.ControlFamilyInfo familyInfo =
            new ProfileVisualizationResult.ControlFamilyInfo();

        familyInfo.setFamilyId("AC");
        familyInfo.setFamilyName("Access Control");
        familyInfo.setIncludedCount(25);
        familyInfo.setExcludedCount(0);
        familyInfo.setIncludedControls(Arrays.asList("AC-1", "AC-2", "AC-3", "AC-4", "AC-5"));
        familyInfo.setExcludedControls(new ArrayList<>());

        assertTrue(familyInfo.getFamilyId().equals("AC"));
        assertTrue(familyInfo.getFamilyName().contains("Access"));
        assertTrue(familyInfo.getIncludedCount() > 0);
        assertEquals(0, familyInfo.getExcludedCount());
        assertFalse(familyInfo.getIncludedControls().isEmpty());
        assertTrue(familyInfo.getExcludedControls().isEmpty());
    }

    @Test
    void testAuditAndAccountabilityFamily() {
        ProfileVisualizationResult.ControlFamilyInfo familyInfo =
            new ProfileVisualizationResult.ControlFamilyInfo();

        familyInfo.setFamilyId("AU");
        familyInfo.setFamilyName("Audit and Accountability");
        familyInfo.setIncludedCount(16);
        familyInfo.setExcludedCount(2);
        familyInfo.setIncludedControls(Arrays.asList("AU-1", "AU-2", "AU-3"));
        familyInfo.setExcludedControls(Arrays.asList("AU-15", "AU-16"));

        assertEquals("AU", familyInfo.getFamilyId());
        assertTrue(familyInfo.getFamilyName().contains("Audit"));
        assertEquals(16, familyInfo.getIncludedCount());
        assertEquals(2, familyInfo.getExcludedCount());
    }

    @Test
    void testNegativeCounts() {
        ProfileVisualizationResult.ControlFamilyInfo familyInfo =
            new ProfileVisualizationResult.ControlFamilyInfo();
        familyInfo.setIncludedCount(-1);
        familyInfo.setExcludedCount(-5);

        assertEquals(-1, familyInfo.getIncludedCount());
        assertEquals(-5, familyInfo.getExcludedCount());
    }

    @Test
    void testWithEnhancementControls() {
        ProfileVisualizationResult.ControlFamilyInfo familyInfo =
            new ProfileVisualizationResult.ControlFamilyInfo();

        List<String> controls = Arrays.asList("AC-2", "AC-2(1)", "AC-2(2)", "AC-2(3)");
        familyInfo.setIncludedControls(controls);

        assertEquals(4, familyInfo.getIncludedControls().size());
        assertTrue(familyInfo.getIncludedControls().contains("AC-2"));
        assertTrue(familyInfo.getIncludedControls().contains("AC-2(1)"));
    }

    @Test
    void testCountsMatchListSizes() {
        ProfileVisualizationResult.ControlFamilyInfo familyInfo =
            new ProfileVisualizationResult.ControlFamilyInfo();

        List<String> included = Arrays.asList("AC-1", "AC-2", "AC-3", "AC-4", "AC-5");
        List<String> excluded = Arrays.asList("AC-20", "AC-21");

        familyInfo.setIncludedControls(included);
        familyInfo.setExcludedControls(excluded);
        familyInfo.setIncludedCount(included.size());
        familyInfo.setExcludedCount(excluded.size());

        assertEquals(familyInfo.getIncludedControls().size(), familyInfo.getIncludedCount());
        assertEquals(familyInfo.getExcludedControls().size(), familyInfo.getExcludedCount());
    }

    @Test
    void testAllNISTControlFamilies() {
        String[] familyIds = {"AC", "AT", "AU", "CA", "CM", "CP", "IA", "IR", "MA", "MP", "PE", "PL", "PS", "PT", "RA", "SA", "SC", "SI", "SR"};

        for (String familyId : familyIds) {
            ProfileVisualizationResult.ControlFamilyInfo familyInfo =
                new ProfileVisualizationResult.ControlFamilyInfo();
            familyInfo.setFamilyId(familyId);
            assertEquals(familyId, familyInfo.getFamilyId());
        }
    }
}
