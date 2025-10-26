package gov.nist.oscal.tools.api.model;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ProfileVisualizationResultTest {

    @Test
    void testNoArgsConstructor() {
        ProfileVisualizationResult result = new ProfileVisualizationResult();

        assertNotNull(result);
        assertNotNull(result.getTimestamp());
        assertNotNull(result.getImports());
        assertNotNull(result.getControlsByFamily());
        assertTrue(result.getImports().isEmpty());
        assertTrue(result.getControlsByFamily().isEmpty());
    }

    @Test
    void testTwoArgsConstructor() {
        ProfileVisualizationResult result = new ProfileVisualizationResult(true, "Success");

        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertEquals("Success", result.getMessage());
        assertNotNull(result.getTimestamp());
    }

    @Test
    void testSetSuccess() {
        ProfileVisualizationResult result = new ProfileVisualizationResult();
        result.setSuccess(true);
        assertTrue(result.isSuccess());

        result.setSuccess(false);
        assertFalse(result.isSuccess());
    }

    @Test
    void testSetMessage() {
        ProfileVisualizationResult result = new ProfileVisualizationResult();
        result.setMessage("Profile resolved successfully");
        assertEquals("Profile resolved successfully", result.getMessage());
    }

    @Test
    void testSetTimestamp() {
        ProfileVisualizationResult result = new ProfileVisualizationResult();
        String customTimestamp = "2024-01-15T10:30:00Z";
        result.setTimestamp(customTimestamp);
        assertEquals(customTimestamp, result.getTimestamp());
    }

    @Test
    void testSetProfileInfo() {
        ProfileVisualizationResult result = new ProfileVisualizationResult();
        ProfileVisualizationResult.ProfileInfo info = new ProfileVisualizationResult.ProfileInfo();
        info.setTitle("NIST SP 800-53 High Baseline");

        result.setProfileInfo(info);

        assertNotNull(result.getProfileInfo());
        assertEquals("NIST SP 800-53 High Baseline", result.getProfileInfo().getTitle());
    }

    @Test
    void testSetImports() {
        ProfileVisualizationResult result = new ProfileVisualizationResult();
        List<ProfileVisualizationResult.ImportInfo> imports = new ArrayList<>();

        ProfileVisualizationResult.ImportInfo importInfo = new ProfileVisualizationResult.ImportInfo();
        importInfo.setHref("catalog.json");
        imports.add(importInfo);

        result.setImports(imports);

        assertEquals(1, result.getImports().size());
        assertEquals("catalog.json", result.getImports().get(0).getHref());
    }

    @Test
    void testSetControlSummary() {
        ProfileVisualizationResult result = new ProfileVisualizationResult();
        ProfileVisualizationResult.ControlSummary summary = new ProfileVisualizationResult.ControlSummary();
        summary.setTotalIncludedControls(100);

        result.setControlSummary(summary);

        assertNotNull(result.getControlSummary());
        assertEquals(100, result.getControlSummary().getTotalIncludedControls());
    }

    @Test
    void testSetControlsByFamily() {
        ProfileVisualizationResult result = new ProfileVisualizationResult();
        Map<String, ProfileVisualizationResult.ControlFamilyInfo> families = new HashMap<>();

        ProfileVisualizationResult.ControlFamilyInfo acFamily = new ProfileVisualizationResult.ControlFamilyInfo();
        acFamily.setFamilyId("AC");
        acFamily.setFamilyName("Access Control");
        families.put("AC", acFamily);

        result.setControlsByFamily(families);

        assertEquals(1, result.getControlsByFamily().size());
        assertTrue(result.getControlsByFamily().containsKey("AC"));
    }

    @Test
    void testSetModificationSummary() {
        ProfileVisualizationResult result = new ProfileVisualizationResult();
        ProfileVisualizationResult.ModificationSummary modSummary = new ProfileVisualizationResult.ModificationSummary();
        modSummary.setTotalSetsParameters(25);

        result.setModificationSummary(modSummary);

        assertNotNull(result.getModificationSummary());
        assertEquals(25, result.getModificationSummary().getTotalSetsParameters());
    }

    @Test
    void testCompleteProfileScenario() {
        ProfileVisualizationResult result = new ProfileVisualizationResult(true, "Profile resolved");

        // Set profile info
        ProfileVisualizationResult.ProfileInfo info = new ProfileVisualizationResult.ProfileInfo();
        info.setTitle("FedRAMP High Baseline");
        info.setUuid("profile-001");
        result.setProfileInfo(info);

        // Set control summary
        ProfileVisualizationResult.ControlSummary summary = new ProfileVisualizationResult.ControlSummary();
        summary.setTotalIncludedControls(325);
        summary.setTotalExcludedControls(50);
        summary.setUniqueFamilies(18);
        result.setControlSummary(summary);

        // Set controls by family
        Map<String, ProfileVisualizationResult.ControlFamilyInfo> families = new HashMap<>();
        ProfileVisualizationResult.ControlFamilyInfo acFamily = new ProfileVisualizationResult.ControlFamilyInfo();
        acFamily.setFamilyId("AC");
        acFamily.setIncludedCount(25);
        families.put("AC", acFamily);
        result.setControlsByFamily(families);

        // Assertions
        assertTrue(result.isSuccess());
        assertEquals("Profile resolved", result.getMessage());
        assertEquals("FedRAMP High Baseline", result.getProfileInfo().getTitle());
        assertEquals(325, result.getControlSummary().getTotalIncludedControls());
        assertEquals(1, result.getControlsByFamily().size());
    }

    @Test
    void testWithMultipleImports() {
        ProfileVisualizationResult result = new ProfileVisualizationResult();
        List<ProfileVisualizationResult.ImportInfo> imports = new ArrayList<>();

        for (int i = 0; i < 5; i++) {
            ProfileVisualizationResult.ImportInfo importInfo = new ProfileVisualizationResult.ImportInfo();
            importInfo.setHref("catalog-" + i + ".json");
            imports.add(importInfo);
        }

        result.setImports(imports);

        assertEquals(5, result.getImports().size());
    }

    @Test
    void testWithMultipleControlFamilies() {
        ProfileVisualizationResult result = new ProfileVisualizationResult();
        Map<String, ProfileVisualizationResult.ControlFamilyInfo> families = new HashMap<>();

        String[] familyIds = {"AC", "AU", "CA", "CM", "IA", "SC", "SI"};
        for (String id : familyIds) {
            ProfileVisualizationResult.ControlFamilyInfo family = new ProfileVisualizationResult.ControlFamilyInfo();
            family.setFamilyId(id);
            families.put(id, family);
        }

        result.setControlsByFamily(families);

        assertEquals(7, result.getControlsByFamily().size());
    }

    @Test
    void testSetAllFieldsToNull() {
        ProfileVisualizationResult result = new ProfileVisualizationResult();

        result.setMessage(null);
        result.setTimestamp(null);
        result.setProfileInfo(null);
        result.setImports(null);
        result.setControlSummary(null);
        result.setControlsByFamily(null);
        result.setModificationSummary(null);

        assertNull(result.getMessage());
        assertNull(result.getTimestamp());
        assertNull(result.getProfileInfo());
        assertNull(result.getImports());
        assertNull(result.getControlSummary());
        assertNull(result.getControlsByFamily());
        assertNull(result.getModificationSummary());
    }

    @Test
    void testTimestampIsAutoGenerated() {
        ProfileVisualizationResult result1 = new ProfileVisualizationResult();

        // Small delay to ensure different timestamps
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        ProfileVisualizationResult result2 = new ProfileVisualizationResult();

        assertNotNull(result1.getTimestamp());
        assertNotNull(result2.getTimestamp());
        // Timestamps should be ISO-8601 format
        assertTrue(result1.getTimestamp().contains("T"));
        assertTrue(result2.getTimestamp().contains("Z"));
    }

    @Test
    void testWithEmptyCollections() {
        ProfileVisualizationResult result = new ProfileVisualizationResult();

        result.setImports(new ArrayList<>());
        result.setControlsByFamily(new HashMap<>());

        assertTrue(result.getImports().isEmpty());
        assertTrue(result.getControlsByFamily().isEmpty());
    }

    @Test
    void testFailureScenario() {
        ProfileVisualizationResult result = new ProfileVisualizationResult(false, "Profile resolution failed: Invalid format");

        assertFalse(result.isSuccess());
        assertEquals("Profile resolution failed: Invalid format", result.getMessage());
        assertNotNull(result.getTimestamp());
    }

    @Test
    void testModifyAllFields() {
        ProfileVisualizationResult result = new ProfileVisualizationResult(true, "First message");

        result.setSuccess(false);
        result.setMessage("Second message");

        assertFalse(result.isSuccess());
        assertEquals("Second message", result.getMessage());
    }

    @Test
    void testWithModifications() {
        ProfileVisualizationResult result = new ProfileVisualizationResult();

        ProfileVisualizationResult.ModificationSummary modSummary = new ProfileVisualizationResult.ModificationSummary();
        modSummary.setTotalSetsParameters(50);
        modSummary.setTotalAlters(25);

        List<String> modifiedIds = new ArrayList<>();
        modifiedIds.add("AC-1");
        modifiedIds.add("AC-2");
        modSummary.setModifiedControlIds(modifiedIds);

        result.setModificationSummary(modSummary);

        assertEquals(50, result.getModificationSummary().getTotalSetsParameters());
        assertEquals(25, result.getModificationSummary().getTotalAlters());
        assertEquals(2, result.getModificationSummary().getModifiedControlIds().size());
    }

    @Test
    void testWithCompleteControlSummary() {
        ProfileVisualizationResult result = new ProfileVisualizationResult();

        ProfileVisualizationResult.ControlSummary summary = new ProfileVisualizationResult.ControlSummary();
        summary.setTotalIncludedControls(325);
        summary.setTotalExcludedControls(50);
        summary.setTotalModifications(75);
        summary.setUniqueFamilies(18);

        result.setControlSummary(summary);

        assertEquals(325, result.getControlSummary().getTotalIncludedControls());
        assertEquals(50, result.getControlSummary().getTotalExcludedControls());
        assertEquals(75, result.getControlSummary().getTotalModifications());
        assertEquals(18, result.getControlSummary().getUniqueFamilies());
    }
}
