package gov.nist.oscal.tools.api.model;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ValidationRulesResponseTest {

    @Test
    void testNoArgsConstructor() {
        ValidationRulesResponse response = new ValidationRulesResponse();

        assertNotNull(response);
        assertEquals(0, response.getTotalRules());
        assertEquals(0, response.getBuiltInRules());
        assertEquals(0, response.getCustomRules());
        assertNotNull(response.getRulesByModelType());
        assertNotNull(response.getRulesByCategory());
        assertNotNull(response.getCategories());
        assertNotNull(response.getRules());
    }

    @Test
    void testSetTotalRules() {
        ValidationRulesResponse response = new ValidationRulesResponse();
        response.setTotalRules(100);
        assertEquals(100, response.getTotalRules());
    }

    @Test
    void testSetBuiltInRules() {
        ValidationRulesResponse response = new ValidationRulesResponse();
        response.setBuiltInRules(75);
        assertEquals(75, response.getBuiltInRules());
    }

    @Test
    void testSetCustomRules() {
        ValidationRulesResponse response = new ValidationRulesResponse();
        response.setCustomRules(25);
        assertEquals(25, response.getCustomRules());
    }

    @Test
    void testSetRulesByModelType() {
        ValidationRulesResponse response = new ValidationRulesResponse();
        Map<String, Integer> rulesByModelType = new HashMap<>();
        rulesByModelType.put("catalog", 50);
        rulesByModelType.put("profile", 30);
        rulesByModelType.put("ssp", 20);

        response.setRulesByModelType(rulesByModelType);

        assertEquals(3, response.getRulesByModelType().size());
        assertEquals(50, response.getRulesByModelType().get("catalog"));
    }

    @Test
    void testSetRulesByCategory() {
        ValidationRulesResponse response = new ValidationRulesResponse();
        Map<String, Integer> rulesByCategory = new HashMap<>();
        rulesByCategory.put("schema", 40);
        rulesByCategory.put("constraint", 60);

        response.setRulesByCategory(rulesByCategory);

        assertEquals(2, response.getRulesByCategory().size());
        assertEquals(40, response.getRulesByCategory().get("schema"));
    }

    @Test
    void testSetCategories() {
        ValidationRulesResponse response = new ValidationRulesResponse();
        List<ValidationRuleCategory> categories = new ArrayList<>();

        ValidationRuleCategory category1 = new ValidationRuleCategory("schema", "Schema Validation", "Schema rules");
        ValidationRuleCategory category2 = new ValidationRuleCategory("constraint", "Constraint Validation", "Constraint rules");
        categories.add(category1);
        categories.add(category2);

        response.setCategories(categories);

        assertEquals(2, response.getCategories().size());
        assertEquals("schema", response.getCategories().get(0).getId());
    }

    @Test
    void testSetRules() {
        ValidationRulesResponse response = new ValidationRulesResponse();
        List<ValidationRule> rules = new ArrayList<>();

        ValidationRule rule1 = new ValidationRule();
        ValidationRule rule2 = new ValidationRule();
        rules.add(rule1);
        rules.add(rule2);

        response.setRules(rules);

        assertEquals(2, response.getRules().size());
    }

    @Test
    void testAddRule() {
        ValidationRulesResponse response = new ValidationRulesResponse();

        ValidationRule rule = new ValidationRule();
        response.addRule(rule);

        assertEquals(1, response.getRules().size());
    }

    @Test
    void testAddMultipleRules() {
        ValidationRulesResponse response = new ValidationRulesResponse();

        for (int i = 0; i < 10; i++) {
            ValidationRule rule = new ValidationRule();
            response.addRule(rule);
        }

        assertEquals(10, response.getRules().size());
    }

    @Test
    void testAddCategory() {
        ValidationRulesResponse response = new ValidationRulesResponse();

        ValidationRuleCategory category = new ValidationRuleCategory("schema", "Schema", "Desc");
        response.addCategory(category);

        assertEquals(1, response.getCategories().size());
    }

    @Test
    void testAddMultipleCategories() {
        ValidationRulesResponse response = new ValidationRulesResponse();

        ValidationRuleCategory category1 = new ValidationRuleCategory("schema", "Schema", "Desc");
        ValidationRuleCategory category2 = new ValidationRuleCategory("constraint", "Constraint", "Desc");

        response.addCategory(category1);
        response.addCategory(category2);

        assertEquals(2, response.getCategories().size());
    }

    @Test
    void testCalculateStatsWithNoRules() {
        ValidationRulesResponse response = new ValidationRulesResponse();
        response.calculateStats();

        assertEquals(0, response.getTotalRules());
        assertEquals(0, response.getBuiltInRules());
        assertEquals(0, response.getCustomRules());
        assertTrue(response.getRulesByModelType().isEmpty());
        assertTrue(response.getRulesByCategory().isEmpty());
    }

    @Test
    void testCalculateStatsWithBuiltInRules() {
        ValidationRulesResponse response = new ValidationRulesResponse();

        ValidationRule builtInRule = new ValidationRule();
        builtInRule.setBuiltIn(true);
        builtInRule.setApplicableModelTypes(Arrays.asList(OscalModelType.CATALOG));
        builtInRule.setCategory("schema");

        response.addRule(builtInRule);
        response.calculateStats();

        assertEquals(1, response.getTotalRules());
        assertEquals(1, response.getBuiltInRules());
        assertEquals(0, response.getCustomRules());
    }

    @Test
    void testCalculateStatsWithCustomRules() {
        ValidationRulesResponse response = new ValidationRulesResponse();

        ValidationRule customRule = new ValidationRule();
        customRule.setBuiltIn(false);
        customRule.setApplicableModelTypes(Arrays.asList(OscalModelType.SYSTEM_SECURITY_PLAN));
        customRule.setCategory("custom");

        response.addRule(customRule);
        response.calculateStats();

        assertEquals(1, response.getTotalRules());
        assertEquals(0, response.getBuiltInRules());
        assertEquals(1, response.getCustomRules());
    }

    @Test
    void testCalculateStatsWithMixedRules() {
        ValidationRulesResponse response = new ValidationRulesResponse();

        ValidationRule builtInRule = new ValidationRule();
        builtInRule.setBuiltIn(true);
        builtInRule.setApplicableModelTypes(Arrays.asList(OscalModelType.CATALOG));
        builtInRule.setCategory("schema");

        ValidationRule customRule = new ValidationRule();
        customRule.setBuiltIn(false);
        customRule.setApplicableModelTypes(Arrays.asList(OscalModelType.SYSTEM_SECURITY_PLAN));
        customRule.setCategory("custom");

        response.addRule(builtInRule);
        response.addRule(customRule);
        response.calculateStats();

        assertEquals(2, response.getTotalRules());
        assertEquals(1, response.getBuiltInRules());
        assertEquals(1, response.getCustomRules());
    }

    @Test
    void testCalculateStatsRulesByModelType() {
        ValidationRulesResponse response = new ValidationRulesResponse();

        ValidationRule catalogRule = new ValidationRule();
        catalogRule.setApplicableModelTypes(Arrays.asList(OscalModelType.CATALOG));
        catalogRule.setCategory("schema");

        ValidationRule profileRule = new ValidationRule();
        profileRule.setApplicableModelTypes(Arrays.asList(OscalModelType.PROFILE));
        profileRule.setCategory("constraint");

        response.addRule(catalogRule);
        response.addRule(profileRule);
        response.calculateStats();

        assertEquals(1, response.getRulesByModelType().get("catalog"));
        assertEquals(1, response.getRulesByModelType().get("profile"));
    }

    @Test
    void testCalculateStatsRulesByCategory() {
        ValidationRulesResponse response = new ValidationRulesResponse();

        ValidationRule schemaRule1 = new ValidationRule();
        schemaRule1.setApplicableModelTypes(Arrays.asList(OscalModelType.CATALOG));
        schemaRule1.setCategory("schema");

        ValidationRule schemaRule2 = new ValidationRule();
        schemaRule2.setApplicableModelTypes(Arrays.asList(OscalModelType.PROFILE));
        schemaRule2.setCategory("schema");

        ValidationRule constraintRule = new ValidationRule();
        constraintRule.setApplicableModelTypes(Arrays.asList(OscalModelType.SYSTEM_SECURITY_PLAN));
        constraintRule.setCategory("constraint");

        response.addRule(schemaRule1);
        response.addRule(schemaRule2);
        response.addRule(constraintRule);
        response.calculateStats();

        assertEquals(2, response.getRulesByCategory().get("schema"));
        assertEquals(1, response.getRulesByCategory().get("constraint"));
    }

    @Test
    void testCalculateStatsWithMultipleModelTypes() {
        ValidationRulesResponse response = new ValidationRulesResponse();

        ValidationRule multiModelRule = new ValidationRule();
        multiModelRule.setApplicableModelTypes(Arrays.asList(OscalModelType.CATALOG, OscalModelType.PROFILE, OscalModelType.SYSTEM_SECURITY_PLAN));
        multiModelRule.setCategory("universal");

        response.addRule(multiModelRule);
        response.calculateStats();

        assertEquals(1, response.getRulesByModelType().get("catalog"));
        assertEquals(1, response.getRulesByModelType().get("profile"));
        assertEquals(1, response.getRulesByModelType().get("system-security-plan"));
    }

    @Test
    void testCalculateStatsIgnoresNullCategory() {
        ValidationRulesResponse response = new ValidationRulesResponse();

        ValidationRule ruleWithNullCategory = new ValidationRule();
        ruleWithNullCategory.setApplicableModelTypes(Arrays.asList(OscalModelType.CATALOG));
        ruleWithNullCategory.setCategory(null);

        response.addRule(ruleWithNullCategory);
        response.calculateStats();

        assertEquals(1, response.getTotalRules());
        assertTrue(response.getRulesByCategory().isEmpty());
    }

    @Test
    void testCalculateStatsIgnoresEmptyCategory() {
        ValidationRulesResponse response = new ValidationRulesResponse();

        ValidationRule ruleWithEmptyCategory = new ValidationRule();
        ruleWithEmptyCategory.setApplicableModelTypes(Arrays.asList(OscalModelType.CATALOG));
        ruleWithEmptyCategory.setCategory("");

        response.addRule(ruleWithEmptyCategory);
        response.calculateStats();

        assertEquals(1, response.getTotalRules());
        assertTrue(response.getRulesByCategory().isEmpty());
    }

    @Test
    void testCalculateStatsClearsExistingMaps() {
        ValidationRulesResponse response = new ValidationRulesResponse();

        // Set some initial values
        Map<String, Integer> initialByModelType = new HashMap<>();
        initialByModelType.put("old-type", 99);
        response.setRulesByModelType(initialByModelType);

        Map<String, Integer> initialByCategory = new HashMap<>();
        initialByCategory.put("old-category", 99);
        response.setRulesByCategory(initialByCategory);

        // Add a new rule
        ValidationRule newRule = new ValidationRule();
        newRule.setApplicableModelTypes(Arrays.asList(OscalModelType.CATALOG));
        newRule.setCategory("schema");
        response.addRule(newRule);

        // Calculate stats should clear old values
        response.calculateStats();

        assertFalse(response.getRulesByModelType().containsKey("old-type"));
        assertFalse(response.getRulesByCategory().containsKey("old-category"));
        assertEquals(1, response.getRulesByModelType().get("catalog"));
        assertEquals(1, response.getRulesByCategory().get("schema"));
    }

    @Test
    void testCompleteValidationRulesScenario() {
        ValidationRulesResponse response = new ValidationRulesResponse();

        // Add built-in rules
        ValidationRule builtIn1 = new ValidationRule();
        builtIn1.setBuiltIn(true);
        builtIn1.setApplicableModelTypes(Arrays.asList(OscalModelType.CATALOG));
        builtIn1.setCategory("schema");

        ValidationRule builtIn2 = new ValidationRule();
        builtIn2.setBuiltIn(true);
        builtIn2.setApplicableModelTypes(Arrays.asList(OscalModelType.PROFILE));
        builtIn2.setCategory("schema");

        // Add custom rules
        ValidationRule custom1 = new ValidationRule();
        custom1.setBuiltIn(false);
        custom1.setApplicableModelTypes(Arrays.asList(OscalModelType.SYSTEM_SECURITY_PLAN));
        custom1.setCategory("custom");

        response.addRule(builtIn1);
        response.addRule(builtIn2);
        response.addRule(custom1);

        // Add categories
        response.addCategory(new ValidationRuleCategory("schema", "Schema Validation", "Schema rules"));
        response.addCategory(new ValidationRuleCategory("custom", "Custom Validation", "Custom rules"));

        // Calculate stats
        response.calculateStats();

        // Assertions
        assertEquals(3, response.getTotalRules());
        assertEquals(2, response.getBuiltInRules());
        assertEquals(1, response.getCustomRules());
        assertEquals(2, response.getCategories().size());
        assertEquals(2, response.getRulesByCategory().get("schema"));
        assertEquals(1, response.getRulesByCategory().get("custom"));
    }

    @Test
    void testSetAllFieldsToNull() {
        ValidationRulesResponse response = new ValidationRulesResponse();

        response.setRulesByModelType(null);
        response.setRulesByCategory(null);
        response.setCategories(null);
        response.setRules(null);

        assertNull(response.getRulesByModelType());
        assertNull(response.getRulesByCategory());
        assertNull(response.getCategories());
        assertNull(response.getRules());
    }

    @Test
    void testModifyAllFields() {
        ValidationRulesResponse response = new ValidationRulesResponse();

        response.setTotalRules(10);
        response.setBuiltInRules(5);
        response.setCustomRules(5);

        response.setTotalRules(20);
        response.setBuiltInRules(15);
        response.setCustomRules(5);

        assertEquals(20, response.getTotalRules());
        assertEquals(15, response.getBuiltInRules());
        assertEquals(5, response.getCustomRules());
    }
}
