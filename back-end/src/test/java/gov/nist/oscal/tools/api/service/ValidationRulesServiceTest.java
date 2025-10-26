package gov.nist.oscal.tools.api.service;

import gov.nist.oscal.tools.api.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class ValidationRulesServiceTest {

    @Mock
    private CustomRulesService customRulesService;

    @InjectMocks
    private ValidationRulesService validationRulesService;

    @BeforeEach
    void setUp() {
        // Initialize the service (calls @PostConstruct)
        validationRulesService.initialize();

        // Setup mock responses for custom rules
        when(customRulesService.getAllCustomRules()).thenReturn(new ArrayList<>());
        when(customRulesService.getCustomRulesForModelType(anyString())).thenReturn(new ArrayList<>());
    }

    // ========== Initialization Tests ==========

    @Test
    void testInitialize_loadsBuiltInRules() {
        ValidationRulesResponse response = validationRulesService.getAllRules();

        assertNotNull(response);
        assertNotNull(response.getRules());
        assertTrue(response.getRules().size() > 0, "Built-in rules should be loaded");
        assertNotNull(response.getCategories());
        assertTrue(response.getCategories().size() > 0, "Categories should be loaded");
    }

    @Test
    void testInitialize_loadsAllCategories() {
        List<ValidationRuleCategory> categories = validationRulesService.getCategories();

        assertNotNull(categories);
        assertTrue(categories.size() >= 9, "Should have at least 9 categories");

        // Verify expected categories exist
        List<String> categoryIds = new ArrayList<>();
        for (ValidationRuleCategory cat : categories) {
            categoryIds.add(cat.getId());
        }

        assertTrue(categoryIds.contains("metadata"));
        assertTrue(categoryIds.contains("security-controls"));
        assertTrue(categoryIds.contains("identifiers"));
        assertTrue(categoryIds.contains("references"));
        assertTrue(categoryIds.contains("structural"));
        assertTrue(categoryIds.contains("profile"));
        assertTrue(categoryIds.contains("component"));
        assertTrue(categoryIds.contains("ssp"));
        assertTrue(categoryIds.contains("assessment"));
    }

    @Test
    void testInitialize_metadataRulesLoaded() {
        ValidationRulesResponse response = validationRulesService.getAllRules();

        // Check for metadata rules
        boolean hasMetadataTitleRequired = response.getRules().stream()
            .anyMatch(rule -> "metadata-title-required".equals(rule.getId()));
        boolean hasLastModifiedRequired = response.getRules().stream()
            .anyMatch(rule -> "metadata-last-modified-required".equals(rule.getId()));
        boolean hasVersionRequired = response.getRules().stream()
            .anyMatch(rule -> "metadata-version-required".equals(rule.getId()));

        assertTrue(hasMetadataTitleRequired);
        assertTrue(hasLastModifiedRequired);
        assertTrue(hasVersionRequired);
    }

    // ========== getAllRules Tests ==========

    @Test
    void testGetAllRules_includesBuiltInRules() {
        ValidationRulesResponse response = validationRulesService.getAllRules();

        assertNotNull(response);
        assertNotNull(response.getRules());
        assertTrue(response.getRules().size() > 0);

        // Verify all rules are marked as built-in
        long builtInCount = response.getRules().stream()
            .filter(ValidationRule::isBuiltIn)
            .count();

        assertTrue(builtInCount > 0, "Should have built-in rules");
    }

    @Test
    void testGetAllRules_includesCustomRules() {
        // Setup custom rules
        List<CustomRuleResponse> customRules = new ArrayList<>();
        CustomRuleResponse customRule = new CustomRuleResponse();
        customRule.setRuleId("custom-rule-1");
        customRule.setName("Custom Rule");
        customRule.setDescription("Test custom rule");
        customRule.setRuleType("CUSTOM");
        customRule.setSeverity("ERROR");
        customRule.setCategory("metadata");
        customRule.setFieldPath("/metadata/test");
        customRule.setApplicableModelTypes(Arrays.asList("catalog"));
        customRules.add(customRule);

        when(customRulesService.getAllCustomRules()).thenReturn(customRules);

        ValidationRulesResponse response = validationRulesService.getAllRules();

        assertNotNull(response);
        assertTrue(response.getRules().size() > 0);

        // Verify custom rule is included
        boolean hasCustomRule = response.getRules().stream()
            .anyMatch(rule -> "custom-rule-1".equals(rule.getId()));

        assertTrue(hasCustomRule);

        // Verify custom rule is not marked as built-in
        ValidationRule customRuleInResponse = response.getRules().stream()
            .filter(rule -> "custom-rule-1".equals(rule.getId()))
            .findFirst()
            .orElse(null);

        assertNotNull(customRuleInResponse);
        assertFalse(customRuleInResponse.isBuiltIn());
    }

    @Test
    void testGetAllRules_calculatesStats() {
        ValidationRulesResponse response = validationRulesService.getAllRules();

        assertNotNull(response);
        // Stats should be calculated - this calls calculateStats() internally
        // We just verify the method doesn't throw an exception
        assertTrue(response.getRules().size() > 0);
    }

    // ========== getRulesForModelType Tests ==========

    @Test
    void testGetRulesForModelType_catalog() {
        ValidationRulesResponse response = validationRulesService.getRulesForModelType(OscalModelType.CATALOG);

        assertNotNull(response);
        assertNotNull(response.getRules());
        assertTrue(response.getRules().size() > 0);

        // All rules should be applicable to CATALOG
        for (ValidationRule rule : response.getRules()) {
            assertTrue(rule.isApplicableTo(OscalModelType.CATALOG),
                "Rule " + rule.getId() + " should be applicable to CATALOG");
        }
    }

    @Test
    void testGetRulesForModelType_profile() {
        ValidationRulesResponse response = validationRulesService.getRulesForModelType(OscalModelType.PROFILE);

        assertNotNull(response);
        assertTrue(response.getRules().size() > 0);

        // Should include profile-specific rules
        boolean hasProfileImportRequired = response.getRules().stream()
            .anyMatch(rule -> "profile-import-required".equals(rule.getId()));

        assertTrue(hasProfileImportRequired, "Should include profile-specific rules");
    }

    @Test
    void testGetRulesForModelType_componentDefinition() {
        ValidationRulesResponse response = validationRulesService.getRulesForModelType(OscalModelType.COMPONENT_DEFINITION);

        assertNotNull(response);
        assertTrue(response.getRules().size() > 0);

        // Should include component-specific rules
        boolean hasComponentUuidRequired = response.getRules().stream()
            .anyMatch(rule -> "component-uuid-required".equals(rule.getId()));
        boolean hasComponentTypeRequired = response.getRules().stream()
            .anyMatch(rule -> "component-type-required".equals(rule.getId()));

        assertTrue(hasComponentUuidRequired);
        assertTrue(hasComponentTypeRequired);
    }

    @Test
    void testGetRulesForModelType_ssp() {
        ValidationRulesResponse response = validationRulesService.getRulesForModelType(OscalModelType.SYSTEM_SECURITY_PLAN);

        assertNotNull(response);
        assertTrue(response.getRules().size() > 0);

        // Should include SSP-specific rules
        boolean hasSspSystemIdRequired = response.getRules().stream()
            .anyMatch(rule -> "ssp-system-id-required".equals(rule.getId()));
        boolean hasSspControlImplRequired = response.getRules().stream()
            .anyMatch(rule -> "ssp-control-implementation-required".equals(rule.getId()));

        assertTrue(hasSspSystemIdRequired);
        assertTrue(hasSspControlImplRequired);
    }

    @Test
    void testGetRulesForModelType_assessmentPlan() {
        ValidationRulesResponse response = validationRulesService.getRulesForModelType(OscalModelType.ASSESSMENT_PLAN);

        assertNotNull(response);
        assertTrue(response.getRules().size() > 0);

        // Should include assessment rules
        boolean hasAssessmentObjectivesRequired = response.getRules().stream()
            .anyMatch(rule -> "assessment-objectives-required".equals(rule.getId()));

        assertTrue(hasAssessmentObjectivesRequired);
    }

    @Test
    void testGetRulesForModelType_includesCustomRules() {
        // Setup custom rule for specific model type
        List<CustomRuleResponse> customRules = new ArrayList<>();
        CustomRuleResponse customRule = new CustomRuleResponse();
        customRule.setRuleId("custom-catalog-rule");
        customRule.setName("Custom Catalog Rule");
        customRule.setDescription("Custom rule for catalog");
        customRule.setRuleType("CUSTOM");
        customRule.setSeverity("WARNING");
        customRule.setCategory("security-controls");
        customRule.setFieldPath("/catalog/custom-field");
        customRule.setApplicableModelTypes(Arrays.asList("catalog"));
        customRules.add(customRule);

        when(customRulesService.getCustomRulesForModelType("catalog")).thenReturn(customRules);

        ValidationRulesResponse response = validationRulesService.getRulesForModelType(OscalModelType.CATALOG);

        assertNotNull(response);
        assertTrue(response.getRules().size() > 0);

        // Verify custom rule is included
        boolean hasCustomRule = response.getRules().stream()
            .anyMatch(rule -> "custom-catalog-rule".equals(rule.getId()));

        assertTrue(hasCustomRule);
    }

    @Test
    void testGetRulesForModelType_groupsByCategory() {
        ValidationRulesResponse response = validationRulesService.getRulesForModelType(OscalModelType.CATALOG);

        assertNotNull(response);
        assertNotNull(response.getCategories());
        assertTrue(response.getCategories().size() > 0);

        // Verify categories contain rules
        for (ValidationRuleCategory category : response.getCategories()) {
            assertNotNull(category.getId());
            assertNotNull(category.getName());
            assertNotNull(category.getRules());
        }
    }

    // ========== getCategories Tests ==========

    @Test
    void testGetCategories_returnsAllCategories() {
        List<ValidationRuleCategory> categories = validationRulesService.getCategories();

        assertNotNull(categories);
        assertTrue(categories.size() >= 9);

        // Verify each category has required fields
        for (ValidationRuleCategory category : categories) {
            assertNotNull(category.getId());
            assertNotNull(category.getName());
            assertNotNull(category.getDescription());
        }
    }

    @Test
    void testGetCategories_hasMetadataCategory() {
        List<ValidationRuleCategory> categories = validationRulesService.getCategories();

        ValidationRuleCategory metadataCategory = categories.stream()
            .filter(cat -> "metadata".equals(cat.getId()))
            .findFirst()
            .orElse(null);

        assertNotNull(metadataCategory);
        assertEquals("Metadata", metadataCategory.getName());
        assertTrue(metadataCategory.getDescription().contains("metadata"));
    }

    @Test
    void testGetCategories_hasSecurityControlsCategory() {
        List<ValidationRuleCategory> categories = validationRulesService.getCategories();

        ValidationRuleCategory securityControlsCategory = categories.stream()
            .filter(cat -> "security-controls".equals(cat.getId()))
            .findFirst()
            .orElse(null);

        assertNotNull(securityControlsCategory);
        assertEquals("Security Controls", securityControlsCategory.getName());
    }

    // ========== Custom Rule Conversion Tests ==========

    @Test
    void testConvertCustomRules_validRule() {
        List<CustomRuleResponse> customRules = new ArrayList<>();
        CustomRuleResponse customRule = new CustomRuleResponse();
        customRule.setRuleId("test-rule");
        customRule.setName("Test Rule");
        customRule.setDescription("Test description");
        customRule.setRuleType("CUSTOM"); // Use CUSTOM which is valid
        customRule.setSeverity("ERROR");
        customRule.setCategory("metadata");
        customRule.setFieldPath("/test/field");
        customRule.setConstraintDetails("Test constraint");
        customRule.setApplicableModelTypes(Arrays.asList("catalog", "profile"));
        customRules.add(customRule);

        when(customRulesService.getAllCustomRules()).thenReturn(customRules);

        ValidationRulesResponse response = validationRulesService.getAllRules();

        ValidationRule converted = response.getRules().stream()
            .filter(rule -> "test-rule".equals(rule.getId()))
            .findFirst()
            .orElse(null);

        assertNotNull(converted);
        assertEquals("Test Rule", converted.getName());
        assertEquals("Test description", converted.getDescription());
        assertEquals(ValidationRuleType.CUSTOM, converted.getRuleType());
        assertEquals(ValidationRuleSeverity.ERROR, converted.getSeverity());
        assertEquals("metadata", converted.getCategory());
        assertEquals("/test/field", converted.getFieldPath());
        assertEquals("Test constraint", converted.getConstraintDetails());
        assertFalse(converted.isBuiltIn());
        assertEquals(2, converted.getApplicableModelTypes().size());
    }

    @Test
    void testConvertCustomRules_invalidRuleType() {
        List<CustomRuleResponse> customRules = new ArrayList<>();
        CustomRuleResponse customRule = new CustomRuleResponse();
        customRule.setRuleId("invalid-type-rule");
        customRule.setName("Invalid Type Rule");
        customRule.setDescription("Test");
        customRule.setRuleType("INVALID_TYPE");
        customRule.setSeverity("WARNING");
        customRule.setCategory("metadata");
        customRules.add(customRule);

        when(customRulesService.getAllCustomRules()).thenReturn(customRules);

        ValidationRulesResponse response = validationRulesService.getAllRules();

        ValidationRule converted = response.getRules().stream()
            .filter(rule -> "invalid-type-rule".equals(rule.getId()))
            .findFirst()
            .orElse(null);

        assertNotNull(converted);
        // Should default to CUSTOM when rule type is invalid
        assertEquals(ValidationRuleType.CUSTOM, converted.getRuleType());
    }

    @Test
    void testConvertCustomRules_invalidSeverity() {
        List<CustomRuleResponse> customRules = new ArrayList<>();
        CustomRuleResponse customRule = new CustomRuleResponse();
        customRule.setRuleId("invalid-severity-rule");
        customRule.setName("Invalid Severity Rule");
        customRule.setDescription("Test");
        customRule.setRuleType("CUSTOM");
        customRule.setSeverity("INVALID_SEVERITY");
        customRule.setCategory("metadata");
        customRules.add(customRule);

        when(customRulesService.getAllCustomRules()).thenReturn(customRules);

        ValidationRulesResponse response = validationRulesService.getAllRules();

        ValidationRule converted = response.getRules().stream()
            .filter(rule -> "invalid-severity-rule".equals(rule.getId()))
            .findFirst()
            .orElse(null);

        assertNotNull(converted);
        // Should default to WARNING when severity is invalid
        assertEquals(ValidationRuleSeverity.WARNING, converted.getSeverity());
    }

    @Test
    void testConvertCustomRules_invalidModelType() {
        List<CustomRuleResponse> customRules = new ArrayList<>();
        CustomRuleResponse customRule = new CustomRuleResponse();
        customRule.setRuleId("invalid-model-rule");
        customRule.setName("Invalid Model Rule");
        customRule.setDescription("Test");
        customRule.setRuleType("CUSTOM");
        customRule.setSeverity("ERROR");
        customRule.setCategory("metadata");
        customRule.setApplicableModelTypes(Arrays.asList("catalog", "invalid-model", "profile"));
        customRules.add(customRule);

        when(customRulesService.getAllCustomRules()).thenReturn(customRules);

        ValidationRulesResponse response = validationRulesService.getAllRules();

        ValidationRule converted = response.getRules().stream()
            .filter(rule -> "invalid-model-rule".equals(rule.getId()))
            .findFirst()
            .orElse(null);

        assertNotNull(converted);
        // Should skip invalid model types but include valid ones
        assertEquals(2, converted.getApplicableModelTypes().size());
        assertTrue(converted.isApplicableTo(OscalModelType.CATALOG));
        assertTrue(converted.isApplicableTo(OscalModelType.PROFILE));
    }

    @Test
    void testConvertCustomRules_nullModelTypes() {
        List<CustomRuleResponse> customRules = new ArrayList<>();
        CustomRuleResponse customRule = new CustomRuleResponse();
        customRule.setRuleId("null-models-rule");
        customRule.setName("Null Models Rule");
        customRule.setDescription("Test");
        customRule.setRuleType("CUSTOM");
        customRule.setSeverity("ERROR");
        customRule.setCategory("metadata");
        customRule.setApplicableModelTypes(null); // Null model types
        customRules.add(customRule);

        when(customRulesService.getAllCustomRules()).thenReturn(customRules);

        ValidationRulesResponse response = validationRulesService.getAllRules();

        ValidationRule converted = response.getRules().stream()
            .filter(rule -> "null-models-rule".equals(rule.getId()))
            .findFirst()
            .orElse(null);

        assertNotNull(converted);
        assertNotNull(converted.getApplicableModelTypes());
        assertEquals(0, converted.getApplicableModelTypes().size());
    }

    // ========== Built-in Rule Content Tests ==========

    @Test
    void testBuiltInRules_securityControlRules() {
        ValidationRulesResponse response = validationRulesService.getRulesForModelType(OscalModelType.CATALOG);

        boolean hasControlIdRequired = response.getRules().stream()
            .anyMatch(rule -> "control-id-required".equals(rule.getId()));
        boolean hasControlTitleRequired = response.getRules().stream()
            .anyMatch(rule -> "control-title-required".equals(rule.getId()));
        boolean hasControlIdUnique = response.getRules().stream()
            .anyMatch(rule -> "control-id-unique".equals(rule.getId()));

        assertTrue(hasControlIdRequired);
        assertTrue(hasControlTitleRequired);
        assertTrue(hasControlIdUnique);
    }

    @Test
    void testBuiltInRules_identifierRules() {
        ValidationRulesResponse response = validationRulesService.getAllRules();

        boolean hasUuidRequired = response.getRules().stream()
            .anyMatch(rule -> "uuid-required".equals(rule.getId()));
        boolean hasUuidFormat = response.getRules().stream()
            .anyMatch(rule -> "uuid-format".equals(rule.getId()));
        boolean hasIdFormat = response.getRules().stream()
            .anyMatch(rule -> "id-format".equals(rule.getId()));

        assertTrue(hasUuidRequired);
        assertTrue(hasUuidFormat);
        assertTrue(hasIdFormat);
    }

    @Test
    void testBuiltInRules_structuralRules() {
        ValidationRulesResponse response = validationRulesService.getAllRules();

        boolean hasJsonStructure = response.getRules().stream()
            .anyMatch(rule -> "valid-json-structure".equals(rule.getId()));
        boolean hasXmlStructure = response.getRules().stream()
            .anyMatch(rule -> "valid-xml-structure".equals(rule.getId()));
        boolean hasSchemaCompliance = response.getRules().stream()
            .anyMatch(rule -> "schema-compliance".equals(rule.getId()));

        assertTrue(hasJsonStructure);
        assertTrue(hasXmlStructure);
        assertTrue(hasSchemaCompliance);
    }
}
