package gov.nist.oscal.tools.api.model;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ValidationRuleTest {

    @Test
    void testNoArgsConstructor() {
        ValidationRule rule = new ValidationRule();

        assertNotNull(rule);
        assertNull(rule.getId());
        assertNull(rule.getName());
        assertNull(rule.getDescription());
        assertNull(rule.getRuleType());
        assertNull(rule.getSeverity());
        assertNotNull(rule.getApplicableModelTypes());
        assertTrue(rule.getApplicableModelTypes().isEmpty());
        assertFalse(rule.isBuiltIn());
        assertNull(rule.getCategory());
        assertNull(rule.getFieldPath());
        assertNull(rule.getConstraintDetails());
    }

    @Test
    void testAllArgsConstructor() {
        ValidationRule rule = new ValidationRule(
            "rule-001",
            "Required UUID",
            "UUID is required for all OSCAL documents",
            ValidationRuleType.REQUIRED_FIELD,
            ValidationRuleSeverity.ERROR,
            true,
            "schema-validation"
        );

        assertEquals("rule-001", rule.getId());
        assertEquals("Required UUID", rule.getName());
        assertEquals("UUID is required for all OSCAL documents", rule.getDescription());
        assertEquals(ValidationRuleType.REQUIRED_FIELD, rule.getRuleType());
        assertEquals(ValidationRuleSeverity.ERROR, rule.getSeverity());
        assertTrue(rule.isBuiltIn());
        assertEquals("schema-validation", rule.getCategory());
    }

    @Test
    void testSetId() {
        ValidationRule rule = new ValidationRule();
        rule.setId("custom-rule-123");
        assertEquals("custom-rule-123", rule.getId());
    }

    @Test
    void testSetName() {
        ValidationRule rule = new ValidationRule();
        rule.setName("Custom Validation Rule");
        assertEquals("Custom Validation Rule", rule.getName());
    }

    @Test
    void testSetDescription() {
        ValidationRule rule = new ValidationRule();
        rule.setDescription("This rule validates custom constraints");
        assertEquals("This rule validates custom constraints", rule.getDescription());
    }

    @Test
    void testSetRuleType() {
        ValidationRule rule = new ValidationRule();
        rule.setRuleType(ValidationRuleType.PATTERN_MATCH);
        assertEquals(ValidationRuleType.PATTERN_MATCH, rule.getRuleType());
    }

    @Test
    void testSetSeverity() {
        ValidationRule rule = new ValidationRule();
        rule.setSeverity(ValidationRuleSeverity.WARNING);
        assertEquals(ValidationRuleSeverity.WARNING, rule.getSeverity());
    }

    @Test
    void testSetApplicableModelTypes() {
        ValidationRule rule = new ValidationRule();
        List<OscalModelType> types = Arrays.asList(OscalModelType.CATALOG, OscalModelType.PROFILE);
        rule.setApplicableModelTypes(types);

        assertEquals(2, rule.getApplicableModelTypes().size());
        assertTrue(rule.getApplicableModelTypes().contains(OscalModelType.CATALOG));
        assertTrue(rule.getApplicableModelTypes().contains(OscalModelType.PROFILE));
    }

    @Test
    void testSetBuiltIn() {
        ValidationRule rule = new ValidationRule();
        assertFalse(rule.isBuiltIn());

        rule.setBuiltIn(true);
        assertTrue(rule.isBuiltIn());

        rule.setBuiltIn(false);
        assertFalse(rule.isBuiltIn());
    }

    @Test
    void testSetCategory() {
        ValidationRule rule = new ValidationRule();
        rule.setCategory("custom-rules");
        assertEquals("custom-rules", rule.getCategory());
    }

    @Test
    void testSetFieldPath() {
        ValidationRule rule = new ValidationRule();
        rule.setFieldPath("metadata.title");
        assertEquals("metadata.title", rule.getFieldPath());
    }

    @Test
    void testSetConstraintDetails() {
        ValidationRule rule = new ValidationRule();
        rule.setConstraintDetails("Title must not exceed 100 characters");
        assertEquals("Title must not exceed 100 characters", rule.getConstraintDetails());
    }

    @Test
    void testAddApplicableModelType() {
        ValidationRule rule = new ValidationRule();

        rule.addApplicableModelType(OscalModelType.CATALOG);
        assertEquals(1, rule.getApplicableModelTypes().size());

        rule.addApplicableModelType(OscalModelType.PROFILE);
        assertEquals(2, rule.getApplicableModelTypes().size());

        rule.addApplicableModelType(OscalModelType.SYSTEM_SECURITY_PLAN);
        assertEquals(3, rule.getApplicableModelTypes().size());
    }

    @Test
    void testAddApplicableModelTypePreventsDuplicates() {
        ValidationRule rule = new ValidationRule();

        rule.addApplicableModelType(OscalModelType.CATALOG);
        assertEquals(1, rule.getApplicableModelTypes().size());

        // Try to add the same type again
        rule.addApplicableModelType(OscalModelType.CATALOG);
        assertEquals(1, rule.getApplicableModelTypes().size());

        // Verify the type is still there
        assertTrue(rule.getApplicableModelTypes().contains(OscalModelType.CATALOG));
    }

    @Test
    void testIsApplicableToWhenListIsEmpty() {
        ValidationRule rule = new ValidationRule();

        // When applicable types list is empty, rule applies to all types
        assertTrue(rule.isApplicableTo(OscalModelType.CATALOG));
        assertTrue(rule.isApplicableTo(OscalModelType.PROFILE));
        assertTrue(rule.isApplicableTo(OscalModelType.SYSTEM_SECURITY_PLAN));
    }

    @Test
    void testIsApplicableToWhenListHasTypes() {
        ValidationRule rule = new ValidationRule();
        rule.addApplicableModelType(OscalModelType.CATALOG);
        rule.addApplicableModelType(OscalModelType.PROFILE);

        assertTrue(rule.isApplicableTo(OscalModelType.CATALOG));
        assertTrue(rule.isApplicableTo(OscalModelType.PROFILE));
        assertFalse(rule.isApplicableTo(OscalModelType.SYSTEM_SECURITY_PLAN));
    }

    @Test
    void testCompleteValidationRuleScenario() {
        ValidationRule rule = new ValidationRule(
            "uuid-required",
            "UUID Required",
            "All OSCAL documents must have a UUID",
            ValidationRuleType.REQUIRED_FIELD,
            ValidationRuleSeverity.ERROR,
            true,
            "required-fields"
        );

        rule.addApplicableModelType(OscalModelType.CATALOG);
        rule.addApplicableModelType(OscalModelType.PROFILE);
        rule.setFieldPath("metadata.uuid");
        rule.setConstraintDetails("UUID must be a valid UUID v4 format");

        assertEquals("uuid-required", rule.getId());
        assertEquals("UUID Required", rule.getName());
        assertEquals(ValidationRuleType.REQUIRED_FIELD, rule.getRuleType());
        assertEquals(ValidationRuleSeverity.ERROR, rule.getSeverity());
        assertTrue(rule.isBuiltIn());
        assertEquals(2, rule.getApplicableModelTypes().size());
        assertTrue(rule.isApplicableTo(OscalModelType.CATALOG));
        assertFalse(rule.isApplicableTo(OscalModelType.SYSTEM_SECURITY_PLAN));
    }

    @Test
    void testSetAllFieldsToNull() {
        ValidationRule rule = new ValidationRule(
            "id",
            "name",
            "desc",
            ValidationRuleType.REQUIRED_FIELD,
            ValidationRuleSeverity.ERROR,
            true,
            "category"
        );

        rule.setId(null);
        rule.setName(null);
        rule.setDescription(null);
        rule.setRuleType(null);
        rule.setSeverity(null);
        rule.setCategory(null);
        rule.setFieldPath(null);
        rule.setConstraintDetails(null);
        rule.setApplicableModelTypes(null);

        assertNull(rule.getId());
        assertNull(rule.getName());
        assertNull(rule.getDescription());
        assertNull(rule.getRuleType());
        assertNull(rule.getSeverity());
        assertNull(rule.getCategory());
        assertNull(rule.getFieldPath());
        assertNull(rule.getConstraintDetails());
        assertNull(rule.getApplicableModelTypes());
    }

    @Test
    void testWithDifferentRuleTypes() {
        ValidationRule requiredFieldRule = new ValidationRule();
        requiredFieldRule.setRuleType(ValidationRuleType.REQUIRED_FIELD);
        assertEquals(ValidationRuleType.REQUIRED_FIELD, requiredFieldRule.getRuleType());

        ValidationRule patternMatchRule = new ValidationRule();
        patternMatchRule.setRuleType(ValidationRuleType.PATTERN_MATCH);
        assertEquals(ValidationRuleType.PATTERN_MATCH, patternMatchRule.getRuleType());

        ValidationRule customRule = new ValidationRule();
        customRule.setRuleType(ValidationRuleType.CUSTOM);
        assertEquals(ValidationRuleType.CUSTOM, customRule.getRuleType());
    }

    @Test
    void testWithDifferentSeverities() {
        ValidationRule errorRule = new ValidationRule();
        errorRule.setSeverity(ValidationRuleSeverity.ERROR);
        assertEquals(ValidationRuleSeverity.ERROR, errorRule.getSeverity());

        ValidationRule warningRule = new ValidationRule();
        warningRule.setSeverity(ValidationRuleSeverity.WARNING);
        assertEquals(ValidationRuleSeverity.WARNING, warningRule.getSeverity());

        ValidationRule infoRule = new ValidationRule();
        infoRule.setSeverity(ValidationRuleSeverity.INFO);
        assertEquals(ValidationRuleSeverity.INFO, infoRule.getSeverity());
    }

    @Test
    void testWithEmptyApplicableModelTypes() {
        ValidationRule rule = new ValidationRule();
        rule.setApplicableModelTypes(new ArrayList<>());

        assertTrue(rule.getApplicableModelTypes().isEmpty());
        // Empty list means applies to all types
        assertTrue(rule.isApplicableTo(OscalModelType.CATALOG));
    }

    @Test
    void testWithMultipleApplicableModelTypes() {
        ValidationRule rule = new ValidationRule();

        rule.addApplicableModelType(OscalModelType.CATALOG);
        rule.addApplicableModelType(OscalModelType.PROFILE);
        rule.addApplicableModelType(OscalModelType.COMPONENT_DEFINITION);
        rule.addApplicableModelType(OscalModelType.SYSTEM_SECURITY_PLAN);

        assertEquals(4, rule.getApplicableModelTypes().size());
        assertTrue(rule.isApplicableTo(OscalModelType.CATALOG));
        assertTrue(rule.isApplicableTo(OscalModelType.SYSTEM_SECURITY_PLAN));
    }

    @Test
    void testBuiltInVsCustomRule() {
        ValidationRule builtInRule = new ValidationRule();
        builtInRule.setBuiltIn(true);
        builtInRule.setCategory("schema");

        ValidationRule customRule = new ValidationRule();
        customRule.setBuiltIn(false);
        customRule.setCategory("custom");

        assertTrue(builtInRule.isBuiltIn());
        assertFalse(customRule.isBuiltIn());
    }

    @Test
    void testModifyAllFields() {
        ValidationRule rule = new ValidationRule();

        rule.setId("first-id");
        rule.setName("First Name");
        rule.setRuleType(ValidationRuleType.REQUIRED_FIELD);

        rule.setId("second-id");
        rule.setName("Second Name");
        rule.setRuleType(ValidationRuleType.PATTERN_MATCH);

        assertEquals("second-id", rule.getId());
        assertEquals("Second Name", rule.getName());
        assertEquals(ValidationRuleType.PATTERN_MATCH, rule.getRuleType());
    }

    @Test
    void testWithLongFieldPath() {
        ValidationRule rule = new ValidationRule();
        String longPath = "metadata.parties[0].addresses[1].city";
        rule.setFieldPath(longPath);
        assertEquals(longPath, rule.getFieldPath());
    }

    @Test
    void testWithComplexConstraintDetails() {
        ValidationRule rule = new ValidationRule();
        String details = "Title must be between 1 and 200 characters, " +
                        "cannot contain special characters except: - , . ( )";
        rule.setConstraintDetails(details);
        assertEquals(details, rule.getConstraintDetails());
    }
}
