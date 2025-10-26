package gov.nist.oscal.tools.api.model;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CustomRuleRequestTest {

    @Test
    void testNoArgsConstructor() {
        CustomRuleRequest request = new CustomRuleRequest();

        assertNotNull(request);
        assertNull(request.getRuleId());
        assertNull(request.getName());
        assertNull(request.getDescription());
        assertNull(request.getRuleType());
        assertNull(request.getSeverity());
        assertNull(request.getCategory());
        assertNull(request.getFieldPath());
        assertNull(request.getRuleExpression());
        assertNull(request.getConstraintDetails());
        assertNull(request.getApplicableModelTypes());
        assertNull(request.getEnabled());
    }

    @Test
    void testSetRuleId() {
        CustomRuleRequest request = new CustomRuleRequest();
        request.setRuleId("rule-001");
        assertEquals("rule-001", request.getRuleId());
    }

    @Test
    void testSetName() {
        CustomRuleRequest request = new CustomRuleRequest();
        request.setName("Required Field Validation");
        assertEquals("Required Field Validation", request.getName());
    }

    @Test
    void testSetDescription() {
        CustomRuleRequest request = new CustomRuleRequest();
        request.setDescription("Validates required fields");
        assertEquals("Validates required fields", request.getDescription());
    }

    @Test
    void testSetRuleType() {
        CustomRuleRequest request = new CustomRuleRequest();
        request.setRuleType("required-field");
        assertEquals("required-field", request.getRuleType());
    }

    @Test
    void testSetSeverity() {
        CustomRuleRequest request = new CustomRuleRequest();
        request.setSeverity("error");
        assertEquals("error", request.getSeverity());
    }

    @Test
    void testSetCategory() {
        CustomRuleRequest request = new CustomRuleRequest();
        request.setCategory("structural");
        assertEquals("structural", request.getCategory());
    }

    @Test
    void testSetFieldPath() {
        CustomRuleRequest request = new CustomRuleRequest();
        request.setFieldPath("$.metadata.title");
        assertEquals("$.metadata.title", request.getFieldPath());
    }

    @Test
    void testSetRuleExpression() {
        CustomRuleRequest request = new CustomRuleRequest();
        request.setRuleExpression("^[A-Z][a-z]+$");
        assertEquals("^[A-Z][a-z]+$", request.getRuleExpression());
    }

    @Test
    void testSetConstraintDetails() {
        CustomRuleRequest request = new CustomRuleRequest();
        request.setConstraintDetails("Must match pattern");
        assertEquals("Must match pattern", request.getConstraintDetails());
    }

    @Test
    void testSetApplicableModelTypes() {
        CustomRuleRequest request = new CustomRuleRequest();
        List<String> types = Arrays.asList("catalog", "profile");
        request.setApplicableModelTypes(types);
        assertEquals(types, request.getApplicableModelTypes());
        assertEquals(2, request.getApplicableModelTypes().size());
    }

    @Test
    void testSetEnabled() {
        CustomRuleRequest request = new CustomRuleRequest();
        request.setEnabled(true);
        assertTrue(request.getEnabled());

        request.setEnabled(false);
        assertFalse(request.getEnabled());
    }

    @Test
    void testSetAllFields() {
        CustomRuleRequest request = new CustomRuleRequest();
        List<String> types = Arrays.asList("catalog", "profile", "ssp");

        request.setRuleId("rule-001");
        request.setName("Test Rule");
        request.setDescription("Test Description");
        request.setRuleType("pattern-match");
        request.setSeverity("warning");
        request.setCategory("semantic");
        request.setFieldPath("$.metadata.version");
        request.setRuleExpression("\\d+\\.\\d+\\.\\d+");
        request.setConstraintDetails("Version must be in x.y.z format");
        request.setApplicableModelTypes(types);
        request.setEnabled(true);

        assertEquals("rule-001", request.getRuleId());
        assertEquals("Test Rule", request.getName());
        assertEquals("Test Description", request.getDescription());
        assertEquals("pattern-match", request.getRuleType());
        assertEquals("warning", request.getSeverity());
        assertEquals("semantic", request.getCategory());
        assertEquals("$.metadata.version", request.getFieldPath());
        assertEquals("\\d+\\.\\d+\\.\\d+", request.getRuleExpression());
        assertEquals("Version must be in x.y.z format", request.getConstraintDetails());
        assertEquals(types, request.getApplicableModelTypes());
        assertTrue(request.getEnabled());
    }

    @Test
    void testSetAllFieldsToNull() {
        CustomRuleRequest request = new CustomRuleRequest();

        request.setRuleId(null);
        request.setName(null);
        request.setDescription(null);
        request.setRuleType(null);
        request.setSeverity(null);
        request.setCategory(null);
        request.setFieldPath(null);
        request.setRuleExpression(null);
        request.setConstraintDetails(null);
        request.setApplicableModelTypes(null);
        request.setEnabled(null);

        assertNull(request.getRuleId());
        assertNull(request.getName());
        assertNull(request.getDescription());
        assertNull(request.getRuleType());
        assertNull(request.getSeverity());
        assertNull(request.getCategory());
        assertNull(request.getFieldPath());
        assertNull(request.getRuleExpression());
        assertNull(request.getConstraintDetails());
        assertNull(request.getApplicableModelTypes());
        assertNull(request.getEnabled());
    }

    @Test
    void testWithEmptyStrings() {
        CustomRuleRequest request = new CustomRuleRequest();

        request.setRuleId("");
        request.setName("");
        request.setDescription("");
        request.setRuleType("");
        request.setSeverity("");
        request.setCategory("");
        request.setFieldPath("");
        request.setRuleExpression("");
        request.setConstraintDetails("");

        assertEquals("", request.getRuleId());
        assertEquals("", request.getName());
        assertEquals("", request.getDescription());
        assertEquals("", request.getRuleType());
        assertEquals("", request.getSeverity());
        assertEquals("", request.getCategory());
        assertEquals("", request.getFieldPath());
        assertEquals("", request.getRuleExpression());
        assertEquals("", request.getConstraintDetails());
    }

    @Test
    void testWithEmptyApplicableModelTypes() {
        CustomRuleRequest request = new CustomRuleRequest();
        request.setApplicableModelTypes(Arrays.asList());

        assertTrue(request.getApplicableModelTypes().isEmpty());
        assertEquals(0, request.getApplicableModelTypes().size());
    }

    @Test
    void testDifferentRuleTypes() {
        CustomRuleRequest request = new CustomRuleRequest();

        request.setRuleType("required-field");
        assertEquals("required-field", request.getRuleType());

        request.setRuleType("pattern-match");
        assertEquals("pattern-match", request.getRuleType());

        request.setRuleType("jsonpath");
        assertEquals("jsonpath", request.getRuleType());

        request.setRuleType("xpath");
        assertEquals("xpath", request.getRuleType());

        request.setRuleType("custom");
        assertEquals("custom", request.getRuleType());
    }

    @Test
    void testDifferentSeverities() {
        CustomRuleRequest request = new CustomRuleRequest();

        request.setSeverity("error");
        assertEquals("error", request.getSeverity());

        request.setSeverity("warning");
        assertEquals("warning", request.getSeverity());

        request.setSeverity("info");
        assertEquals("info", request.getSeverity());
    }

    @Test
    void testWithMultipleApplicableModelTypes() {
        CustomRuleRequest request = new CustomRuleRequest();
        List<String> types = Arrays.asList(
            "catalog",
            "profile",
            "component-definition",
            "system-security-plan",
            "assessment-plan",
            "assessment-results",
            "plan-of-action-and-milestones"
        );
        request.setApplicableModelTypes(types);

        assertEquals(7, request.getApplicableModelTypes().size());
        assertTrue(request.getApplicableModelTypes().contains("catalog"));
        assertTrue(request.getApplicableModelTypes().contains("plan-of-action-and-milestones"));
    }

    @Test
    void testWithSpecialCharacters() {
        CustomRuleRequest request = new CustomRuleRequest();

        request.setName("Test <Rule> & \"Validation\"");
        request.setDescription("Description with special chars: @#$%^&*()");
        request.setRuleExpression("[A-Za-z0-9!@#$%^&*()]");

        assertEquals("Test <Rule> & \"Validation\"", request.getName());
        assertEquals("Description with special chars: @#$%^&*()", request.getDescription());
        assertEquals("[A-Za-z0-9!@#$%^&*()]", request.getRuleExpression());
    }

    @Test
    void testCompleteCustomRuleScenario() {
        CustomRuleRequest request = new CustomRuleRequest();

        request.setRuleId("custom-rule-001");
        request.setName("UUID Format Validation");
        request.setDescription("Ensures all UUIDs are in standard format");
        request.setRuleType("pattern-match");
        request.setSeverity("error");
        request.setCategory("data-quality");
        request.setFieldPath("$.metadata.document-id");
        request.setRuleExpression("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");
        request.setConstraintDetails("UUID must be in format: xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx");
        request.setApplicableModelTypes(Arrays.asList("catalog", "profile", "system-security-plan"));
        request.setEnabled(true);

        assertEquals("custom-rule-001", request.getRuleId());
        assertEquals("UUID Format Validation", request.getName());
        assertEquals("Ensures all UUIDs are in standard format", request.getDescription());
        assertEquals("pattern-match", request.getRuleType());
        assertEquals("error", request.getSeverity());
        assertEquals("data-quality", request.getCategory());
        assertEquals("$.metadata.document-id", request.getFieldPath());
        assertTrue(request.getRuleExpression().contains("^[0-9a-fA-F]"));
        assertEquals("UUID must be in format: xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx", request.getConstraintDetails());
        assertEquals(3, request.getApplicableModelTypes().size());
        assertTrue(request.getEnabled());
    }
}
