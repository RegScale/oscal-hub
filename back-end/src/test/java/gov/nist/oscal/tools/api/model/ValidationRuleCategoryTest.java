package gov.nist.oscal.tools.api.model;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ValidationRuleCategoryTest {

    @Test
    void testNoArgsConstructor() {
        ValidationRuleCategory category = new ValidationRuleCategory();

        assertNotNull(category);
        assertNull(category.getId());
        assertNull(category.getName());
        assertNull(category.getDescription());
        assertNotNull(category.getRules());
        assertTrue(category.getRules().isEmpty());
        assertEquals(0, category.getRuleCount());
    }

    @Test
    void testAllArgsConstructor() {
        ValidationRuleCategory category = new ValidationRuleCategory(
            "schema",
            "Schema Validation",
            "Rules for validating against OSCAL schemas"
        );

        assertEquals("schema", category.getId());
        assertEquals("Schema Validation", category.getName());
        assertEquals("Rules for validating against OSCAL schemas", category.getDescription());
        assertNotNull(category.getRules());
    }

    @Test
    void testSetId() {
        ValidationRuleCategory category = new ValidationRuleCategory();
        category.setId("content");
        assertEquals("content", category.getId());
    }

    @Test
    void testSetName() {
        ValidationRuleCategory category = new ValidationRuleCategory();
        category.setName("Content Validation");
        assertEquals("Content Validation", category.getName());
    }

    @Test
    void testSetDescription() {
        ValidationRuleCategory category = new ValidationRuleCategory();
        category.setDescription("Validates content structure and semantics");
        assertEquals("Validates content structure and semantics", category.getDescription());
    }

    @Test
    void testSetRules() {
        ValidationRuleCategory category = new ValidationRuleCategory();
        List<ValidationRule> rules = new ArrayList<>();
        rules.add(new ValidationRule());
        rules.add(new ValidationRule());
        rules.add(new ValidationRule());

        category.setRules(rules);

        assertEquals(rules, category.getRules());
        assertEquals(3, category.getRuleCount());
    }

    @Test
    void testSetRuleCount() {
        ValidationRuleCategory category = new ValidationRuleCategory();
        category.setRuleCount(5);
        assertEquals(5, category.getRuleCount());
    }

    @Test
    void testAddRule() {
        ValidationRuleCategory category = new ValidationRuleCategory();
        ValidationRule rule1 = new ValidationRule();
        ValidationRule rule2 = new ValidationRule();

        category.addRule(rule1);
        assertEquals(1, category.getRules().size());
        assertEquals(1, category.getRuleCount());

        category.addRule(rule2);
        assertEquals(2, category.getRules().size());
        assertEquals(2, category.getRuleCount());
    }

    @Test
    void testAddMultipleRules() {
        ValidationRuleCategory category = new ValidationRuleCategory();

        for (int i = 0; i < 10; i++) {
            category.addRule(new ValidationRule());
        }

        assertEquals(10, category.getRules().size());
        assertEquals(10, category.getRuleCount());
    }

    @Test
    void testSetAllFields() {
        ValidationRuleCategory category = new ValidationRuleCategory();
        category.setId("constraint");
        category.setName("Constraint Validation");
        category.setDescription("Validates OSCAL constraints");

        List<ValidationRule> rules = Arrays.asList(
            new ValidationRule(),
            new ValidationRule()
        );
        category.setRules(rules);

        assertEquals("constraint", category.getId());
        assertEquals("Constraint Validation", category.getName());
        assertEquals("Validates OSCAL constraints", category.getDescription());
        assertEquals(2, category.getRules().size());
        assertEquals(2, category.getRuleCount());
    }

    @Test
    void testSetFieldsToNull() {
        ValidationRuleCategory category = new ValidationRuleCategory("id", "name", "desc");
        category.setId(null);
        category.setName(null);
        category.setDescription(null);

        assertNull(category.getId());
        assertNull(category.getName());
        assertNull(category.getDescription());
    }

    @Test
    void testModifyAllFields() {
        ValidationRuleCategory category = new ValidationRuleCategory();

        category.setId("first");
        category.setName("First Name");
        category.setDescription("First description");

        category.setId("second");
        category.setName("Second Name");
        category.setDescription("Second description");

        assertEquals("second", category.getId());
        assertEquals("Second Name", category.getName());
        assertEquals("Second description", category.getDescription());
    }

    @Test
    void testWithEmptyStrings() {
        ValidationRuleCategory category = new ValidationRuleCategory("", "", "");

        assertEquals("", category.getId());
        assertEquals("", category.getName());
        assertEquals("", category.getDescription());
    }

    @Test
    void testWithEmptyRuleList() {
        ValidationRuleCategory category = new ValidationRuleCategory();
        category.setRules(new ArrayList<>());

        assertEquals(0, category.getRules().size());
        assertEquals(0, category.getRuleCount());
    }

    @Test
    void testRuleCountSyncsWithRuleListSize() {
        ValidationRuleCategory category = new ValidationRuleCategory();

        List<ValidationRule> rules = new ArrayList<>();
        rules.add(new ValidationRule());
        rules.add(new ValidationRule());
        rules.add(new ValidationRule());

        category.setRules(rules);
        assertEquals(category.getRules().size(), category.getRuleCount());

        category.addRule(new ValidationRule());
        assertEquals(category.getRules().size(), category.getRuleCount());
    }

    @Test
    void testConstructorAndSettersCombined() {
        ValidationRuleCategory category = new ValidationRuleCategory(
            "schema",
            "Schema Validation",
            "Schema rules"
        );

        assertEquals("schema", category.getId());
        assertEquals("Schema Validation", category.getName());

        category.setId("content");
        category.setName("Content Validation");

        assertEquals("content", category.getId());
        assertEquals("Content Validation", category.getName());
    }

    @Test
    void testWithLongDescription() {
        ValidationRuleCategory category = new ValidationRuleCategory();
        String longDescription = "This is a very long description that describes " +
                                "the validation rules in great detail. ".repeat(10);
        category.setDescription(longDescription);
        assertEquals(longDescription, category.getDescription());
    }

    @Test
    void testWithSpecialCharacters() {
        ValidationRuleCategory category = new ValidationRuleCategory();
        category.setId("schema-v1.2.3");
        category.setName("Schema Validation (v1.2.3)");
        category.setDescription("Validates XML/JSON/YAML schemas & constraints");

        assertTrue(category.getId().contains("-"));
        assertTrue(category.getName().contains("("));
        assertTrue(category.getDescription().contains("&"));
    }

    @Test
    void testCompleteValidationScenario() {
        ValidationRuleCategory category = new ValidationRuleCategory(
            "schema-validation",
            "Schema Validation Rules",
            "Rules for validating OSCAL documents against schemas"
        );

        ValidationRule rule1 = new ValidationRule();
        ValidationRule rule2 = new ValidationRule();
        ValidationRule rule3 = new ValidationRule();

        category.addRule(rule1);
        category.addRule(rule2);
        category.addRule(rule3);

        assertNotNull(category);
        assertEquals("schema-validation", category.getId());
        assertEquals(3, category.getRuleCount());
        assertEquals(3, category.getRules().size());
    }

    @Test
    void testMultipleCategoriesIndependence() {
        ValidationRuleCategory category1 = new ValidationRuleCategory("cat1", "Category 1", "First");
        ValidationRuleCategory category2 = new ValidationRuleCategory("cat2", "Category 2", "Second");

        category1.addRule(new ValidationRule());
        category2.addRule(new ValidationRule());
        category2.addRule(new ValidationRule());

        assertNotEquals(category1.getId(), category2.getId());
        assertNotEquals(category1.getRuleCount(), category2.getRuleCount());
        assertEquals(1, category1.getRuleCount());
        assertEquals(2, category2.getRuleCount());
    }

    @Test
    void testRuleCountManualOverride() {
        ValidationRuleCategory category = new ValidationRuleCategory();

        // Add 3 rules
        category.addRule(new ValidationRule());
        category.addRule(new ValidationRule());
        category.addRule(new ValidationRule());

        assertEquals(3, category.getRuleCount());

        // Manually override the count (edge case)
        category.setRuleCount(10);
        assertEquals(10, category.getRuleCount());

        // Note: actual rules list still has 3 items
        assertEquals(3, category.getRules().size());
    }

    @Test
    void testWithNullRulesList() {
        ValidationRuleCategory category = new ValidationRuleCategory();

        // Initially has empty list
        assertNotNull(category.getRules());

        // This would be an unusual case, but we can test it
        List<ValidationRule> rules = new ArrayList<>();
        rules.add(new ValidationRule());
        category.setRules(rules);

        assertEquals(1, category.getRuleCount());
    }
}
