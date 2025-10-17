package gov.nist.oscal.tools.api.service;

import gov.nist.oscal.tools.api.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ValidationRulesService {

    private static final Logger logger = LoggerFactory.getLogger(ValidationRulesService.class);
    private final Map<String, ValidationRule> rulesById = new HashMap<>();
    private final Map<String, ValidationRuleCategory> categoriesById = new HashMap<>();
    private final CustomRulesService customRulesService;

    @Autowired
    public ValidationRulesService(CustomRulesService customRulesService) {
        this.customRulesService = customRulesService;
    }

    @PostConstruct
    public void initialize() {
        logger.info("Initializing validation rules service with built-in OSCAL rules");
        loadBuiltInRules();
        logger.info("Loaded {} built-in validation rules in {} categories",
            rulesById.size(), categoriesById.size());
    }

    /**
     * Get all validation rules (built-in + custom)
     */
    public ValidationRulesResponse getAllRules() {
        ValidationRulesResponse response = new ValidationRulesResponse();

        // Get built-in rules
        List<ValidationRule> allRules = new ArrayList<>(rulesById.values());

        // Add custom rules
        List<CustomRuleResponse> customRules = customRulesService.getAllCustomRules();
        allRules.addAll(convertCustomRulesToValidationRules(customRules));

        response.setRules(allRules);
        response.setCategories(new ArrayList<>(categoriesById.values()));
        response.calculateStats();
        return response;
    }

    /**
     * Get validation rules for a specific OSCAL model type
     */
    public ValidationRulesResponse getRulesForModelType(OscalModelType modelType) {
        // Get built-in rules for this model type
        List<ValidationRule> filteredRules = rulesById.values().stream()
            .filter(rule -> rule.isApplicableTo(modelType))
            .collect(Collectors.toList());

        // Add custom rules for this model type
        List<CustomRuleResponse> customRules = customRulesService.getCustomRulesForModelType(modelType.getValue());
        filteredRules.addAll(convertCustomRulesToValidationRules(customRules));

        ValidationRulesResponse response = new ValidationRulesResponse();
        response.setRules(filteredRules);

        // Group filtered rules by category
        Map<String, ValidationRuleCategory> filteredCategories = new HashMap<>();
        for (ValidationRule rule : filteredRules) {
            String categoryId = rule.getCategory();
            if (categoryId != null && categoriesById.containsKey(categoryId)) {
                ValidationRuleCategory category = filteredCategories.computeIfAbsent(
                    categoryId,
                    k -> {
                        ValidationRuleCategory orig = categoriesById.get(k);
                        return new ValidationRuleCategory(orig.getId(), orig.getName(), orig.getDescription());
                    }
                );
                category.addRule(rule);
            }
        }

        response.setCategories(new ArrayList<>(filteredCategories.values()));
        response.calculateStats();
        return response;
    }

    /**
     * Get all rule categories
     */
    public List<ValidationRuleCategory> getCategories() {
        return new ArrayList<>(categoriesById.values());
    }

    /**
     * Load built-in OSCAL validation rules
     */
    private void loadBuiltInRules() {
        // Initialize categories
        initializeCategories();

        // Load rules for each category
        loadMetadataRules();
        loadSecurityControlRules();
        loadIdentifierRules();
        loadReferenceRules();
        loadStructuralRules();
        loadProfileRules();
        loadComponentDefinitionRules();
        loadSSPRules();
        loadAssessmentRules();
    }

    private void initializeCategories() {
        addCategory("metadata", "Metadata", "Rules validating document metadata and common properties");
        addCategory("security-controls", "Security Controls", "Rules for control definitions and implementations");
        addCategory("identifiers", "Identifiers", "Rules for IDs, UUIDs, and identifier references");
        addCategory("references", "References", "Rules for links, citations, and external references");
        addCategory("structural", "Document Structure", "Rules for overall document structure and organization");
        addCategory("profile", "Profile-Specific", "Rules specific to OSCAL Profile documents");
        addCategory("component", "Component-Specific", "Rules specific to Component Definition documents");
        addCategory("ssp", "SSP-Specific", "Rules specific to System Security Plan documents");
        addCategory("assessment", "Assessment", "Rules for Assessment Plans and Results");
    }

    private void addCategory(String id, String name, String description) {
        categoriesById.put(id, new ValidationRuleCategory(id, name, description));
    }

    private void loadMetadataRules() {
        // Common metadata rules across all OSCAL types
        List<OscalModelType> allModels = Arrays.asList(OscalModelType.values());

        addRule("metadata-title-required", "Document Title Required",
            "Every OSCAL document must have a title in its metadata section",
            ValidationRuleType.REQUIRED_FIELD, ValidationRuleSeverity.ERROR,
            "metadata", "/metadata/title", allModels);

        addRule("metadata-last-modified-required", "Last Modified Date Required",
            "The last-modified timestamp must be present in document metadata",
            ValidationRuleType.REQUIRED_FIELD, ValidationRuleSeverity.ERROR,
            "metadata", "/metadata/last-modified", allModels);

        addRule("metadata-last-modified-format", "Last Modified Date Format",
            "Last modified date must be in ISO 8601 format (YYYY-MM-DDTHH:MM:SSZ)",
            ValidationRuleType.PATTERN_MATCH, ValidationRuleSeverity.ERROR,
            "metadata", "/metadata/last-modified", allModels);

        addRule("metadata-version-required", "Document Version Required",
            "Every OSCAL document must have a version in its metadata",
            ValidationRuleType.REQUIRED_FIELD, ValidationRuleSeverity.ERROR,
            "metadata", "/metadata/version", allModels);

        addRule("metadata-oscal-version-required", "OSCAL Version Required",
            "The OSCAL version must be specified in document metadata",
            ValidationRuleType.REQUIRED_FIELD, ValidationRuleSeverity.ERROR,
            "metadata", "/metadata/oscal-version", allModels);

        addRule("metadata-oscal-version-format", "OSCAL Version Format",
            "OSCAL version must follow semantic versioning (e.g., 1.0.4)",
            ValidationRuleType.PATTERN_MATCH, ValidationRuleSeverity.ERROR,
            "metadata", "/metadata/oscal-version", allModels);
    }

    private void loadSecurityControlRules() {
        addRule("control-id-required", "Control ID Required",
            "Every security control must have a unique identifier",
            ValidationRuleType.REQUIRED_FIELD, ValidationRuleSeverity.ERROR,
            "security-controls", "/catalog/control/id",
            Arrays.asList(OscalModelType.CATALOG, OscalModelType.PROFILE));

        addRule("control-title-required", "Control Title Required",
            "Every security control must have a descriptive title",
            ValidationRuleType.REQUIRED_FIELD, ValidationRuleSeverity.ERROR,
            "security-controls", "/catalog/control/title",
            Arrays.asList(OscalModelType.CATALOG, OscalModelType.PROFILE));

        addRule("control-id-unique", "Control IDs Must Be Unique",
            "Control identifiers must be unique within a catalog or profile",
            ValidationRuleType.CROSS_FIELD, ValidationRuleSeverity.ERROR,
            "security-controls", "/catalog/control/id",
            Arrays.asList(OscalModelType.CATALOG, OscalModelType.PROFILE));

        addRule("control-parameter-id-required", "Parameter ID Required",
            "Control parameters must have unique identifiers",
            ValidationRuleType.REQUIRED_FIELD, ValidationRuleSeverity.ERROR,
            "security-controls", "/catalog/control/param/id",
            Arrays.asList(OscalModelType.CATALOG, OscalModelType.PROFILE));
    }

    private void loadIdentifierRules() {
        List<OscalModelType> allModels = Arrays.asList(OscalModelType.values());

        addRule("uuid-required", "Document UUID Required",
            "Every OSCAL document must have a unique UUID identifier",
            ValidationRuleType.REQUIRED_FIELD, ValidationRuleSeverity.ERROR,
            "identifiers", "/*/uuid", allModels);

        addRule("uuid-format", "UUID Format Validation",
            "UUIDs must conform to RFC 4122 format (e.g., 123e4567-e89b-12d3-a456-426614174000)",
            ValidationRuleType.PATTERN_MATCH, ValidationRuleSeverity.ERROR,
            "identifiers", "/*/uuid", allModels);

        addRule("id-format", "ID Format Validation",
            "IDs must be valid NCNames (no spaces, must start with letter or underscore)",
            ValidationRuleType.PATTERN_MATCH, ValidationRuleSeverity.ERROR,
            "identifiers", "/*/@id", allModels);
    }

    private void loadReferenceRules() {
        List<OscalModelType> allModels = Arrays.asList(OscalModelType.values());

        addRule("link-href-required", "Link HREF Required",
            "Every link element must have an href attribute",
            ValidationRuleType.REQUIRED_FIELD, ValidationRuleSeverity.ERROR,
            "references", "/*/link/@href", allModels);

        addRule("resource-uuid-required", "Resource UUID Required",
            "Back-matter resources must have unique UUIDs",
            ValidationRuleType.REQUIRED_FIELD, ValidationRuleSeverity.ERROR,
            "references", "/*/back-matter/resource/uuid", allModels);

        addRule("resource-uuid-unique", "Resource UUIDs Must Be Unique",
            "Resource UUIDs must be unique within the document's back-matter",
            ValidationRuleType.CROSS_FIELD, ValidationRuleSeverity.ERROR,
            "references", "/*/back-matter/resource/uuid", allModels);
    }

    private void loadStructuralRules() {
        List<OscalModelType> allModels = Arrays.asList(OscalModelType.values());

        addRule("valid-json-structure", "Valid JSON Structure",
            "Document must be valid JSON when in JSON format",
            ValidationRuleType.DATA_TYPE, ValidationRuleSeverity.ERROR,
            "structural", "/", allModels);

        addRule("valid-xml-structure", "Valid XML Structure",
            "Document must be well-formed XML when in XML format",
            ValidationRuleType.DATA_TYPE, ValidationRuleSeverity.ERROR,
            "structural", "/", allModels);

        addRule("valid-yaml-structure", "Valid YAML Structure",
            "Document must be valid YAML when in YAML format",
            ValidationRuleType.DATA_TYPE, ValidationRuleSeverity.ERROR,
            "structural", "/", allModels);

        addRule("schema-compliance", "Schema Compliance",
            "Document must validate against the appropriate OSCAL schema",
            ValidationRuleType.DATA_TYPE, ValidationRuleSeverity.ERROR,
            "structural", "/", allModels);
    }

    private void loadProfileRules() {
        List<OscalModelType> profileOnly = Collections.singletonList(OscalModelType.PROFILE);

        addRule("profile-import-required", "Profile Import Required",
            "Profiles must import at least one catalog or profile",
            ValidationRuleType.CARDINALITY, ValidationRuleSeverity.ERROR,
            "profile", "/profile/import", profileOnly);

        addRule("profile-import-href-required", "Import HREF Required",
            "Every profile import must specify an href to the source catalog/profile",
            ValidationRuleType.REQUIRED_FIELD, ValidationRuleSeverity.ERROR,
            "profile", "/profile/import/@href", profileOnly);

        addRule("profile-merge-method", "Merge Method Validation",
            "Profile merge method must be 'use-first', 'merge', or 'keep'",
            ValidationRuleType.ALLOWED_VALUES, ValidationRuleSeverity.ERROR,
            "profile", "/profile/merge/method", profileOnly);
    }

    private void loadComponentDefinitionRules() {
        List<OscalModelType> componentOnly = Collections.singletonList(OscalModelType.COMPONENT_DEFINITION);

        addRule("component-uuid-required", "Component UUID Required",
            "Every component must have a unique UUID",
            ValidationRuleType.REQUIRED_FIELD, ValidationRuleSeverity.ERROR,
            "component", "/component-definition/component/uuid", componentOnly);

        addRule("component-type-required", "Component Type Required",
            "Every component must specify its type",
            ValidationRuleType.REQUIRED_FIELD, ValidationRuleSeverity.ERROR,
            "component", "/component-definition/component/type", componentOnly);

        addRule("component-title-required", "Component Title Required",
            "Every component must have a descriptive title",
            ValidationRuleType.REQUIRED_FIELD, ValidationRuleSeverity.ERROR,
            "component", "/component-definition/component/title", componentOnly);

        addRule("component-description-required", "Component Description Required",
            "Every component must have a description",
            ValidationRuleType.REQUIRED_FIELD, ValidationRuleSeverity.ERROR,
            "component", "/component-definition/component/description", componentOnly);
    }

    private void loadSSPRules() {
        List<OscalModelType> sspOnly = Collections.singletonList(OscalModelType.SYSTEM_SECURITY_PLAN);

        addRule("ssp-system-id-required", "System ID Required",
            "System Security Plans must identify the system being documented",
            ValidationRuleType.REQUIRED_FIELD, ValidationRuleSeverity.ERROR,
            "ssp", "/system-security-plan/system-id", sspOnly);

        addRule("ssp-security-sensitivity-required", "Security Sensitivity Level Required",
            "SSP must specify the system's security sensitivity level",
            ValidationRuleType.REQUIRED_FIELD, ValidationRuleSeverity.ERROR,
            "ssp", "/system-security-plan/system-characteristics/security-sensitivity-level", sspOnly);

        addRule("ssp-system-info-required", "System Information Required",
            "SSP must include system-characteristics section",
            ValidationRuleType.REQUIRED_FIELD, ValidationRuleSeverity.ERROR,
            "ssp", "/system-security-plan/system-characteristics", sspOnly);

        addRule("ssp-control-implementation-required", "Control Implementation Required",
            "SSP must document implementation of security controls",
            ValidationRuleType.REQUIRED_FIELD, ValidationRuleSeverity.ERROR,
            "ssp", "/system-security-plan/control-implementation", sspOnly);

        addRule("ssp-authorization-boundary-required", "Authorization Boundary Required",
            "SSP must define the system authorization boundary",
            ValidationRuleType.REQUIRED_FIELD, ValidationRuleSeverity.WARNING,
            "ssp", "/system-security-plan/system-characteristics/authorization-boundary", sspOnly);
    }

    private void loadAssessmentRules() {
        List<OscalModelType> assessmentModels = Arrays.asList(
            OscalModelType.ASSESSMENT_PLAN,
            OscalModelType.ASSESSMENT_RESULTS
        );

        addRule("assessment-objectives-required", "Assessment Objectives Required",
            "Assessment documents must define objectives",
            ValidationRuleType.REQUIRED_FIELD, ValidationRuleSeverity.ERROR,
            "assessment", "/*/objectives", assessmentModels);

        addRule("assessment-subject-required", "Assessment Subject Required",
            "Assessments must identify what is being assessed",
            ValidationRuleType.REQUIRED_FIELD, ValidationRuleSeverity.ERROR,
            "assessment", "/*/assessment-subject", assessmentModels);

        // Assessment Results specific
        addRule("assessment-results-required", "Assessment Results Required",
            "Assessment Results must contain actual result data",
            ValidationRuleType.REQUIRED_FIELD, ValidationRuleSeverity.ERROR,
            "assessment", "/assessment-results/result",
            Collections.singletonList(OscalModelType.ASSESSMENT_RESULTS));
    }

    private void addRule(String id, String name, String description,
                        ValidationRuleType ruleType, ValidationRuleSeverity severity,
                        String categoryId, String fieldPath, List<OscalModelType> applicableModels) {
        ValidationRule rule = new ValidationRule(id, name, description, ruleType, severity, true, categoryId);
        rule.setFieldPath(fieldPath);
        rule.setApplicableModelTypes(applicableModels);

        rulesById.put(id, rule);

        if (categoriesById.containsKey(categoryId)) {
            categoriesById.get(categoryId).addRule(rule);
        }
    }

    /**
     * Convert custom rule responses to validation rules
     */
    private List<ValidationRule> convertCustomRulesToValidationRules(List<CustomRuleResponse> customRules) {
        List<ValidationRule> validationRules = new ArrayList<>();

        for (CustomRuleResponse customRule : customRules) {
            ValidationRule rule = new ValidationRule();
            rule.setId(customRule.getRuleId());
            rule.setName(customRule.getName());
            rule.setDescription(customRule.getDescription());
            rule.setBuiltIn(false); // Custom rules are not built-in
            rule.setCategory(customRule.getCategory());
            rule.setFieldPath(customRule.getFieldPath());
            rule.setConstraintDetails(customRule.getConstraintDetails());

            // Convert string ruleType to enum
            try {
                rule.setRuleType(ValidationRuleType.fromString(customRule.getRuleType()));
            } catch (IllegalArgumentException e) {
                logger.warn("Invalid rule type '{}' for custom rule {}, defaulting to CUSTOM",
                    customRule.getRuleType(), customRule.getRuleId());
                rule.setRuleType(ValidationRuleType.CUSTOM);
            }

            // Convert string severity to enum
            try {
                rule.setSeverity(ValidationRuleSeverity.fromString(customRule.getSeverity()));
            } catch (IllegalArgumentException e) {
                logger.warn("Invalid severity '{}' for custom rule {}, defaulting to WARNING",
                    customRule.getSeverity(), customRule.getRuleId());
                rule.setSeverity(ValidationRuleSeverity.WARNING);
            }

            // Convert string model types to enum list
            List<OscalModelType> modelTypes = new ArrayList<>();
            if (customRule.getApplicableModelTypes() != null) {
                for (String modelTypeStr : customRule.getApplicableModelTypes()) {
                    try {
                        modelTypes.add(OscalModelType.fromString(modelTypeStr));
                    } catch (IllegalArgumentException e) {
                        logger.warn("Invalid model type '{}' for custom rule {}, skipping",
                            modelTypeStr, customRule.getRuleId());
                    }
                }
            }
            rule.setApplicableModelTypes(modelTypes);

            validationRules.add(rule);
        }

        return validationRules;
    }
}
