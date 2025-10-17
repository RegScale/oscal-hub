package gov.nist.oscal.tools.api.model;

import java.util.ArrayList;
import java.util.List;

public class ValidationRule {
    private String id;
    private String name;
    private String description;
    private ValidationRuleType ruleType;
    private ValidationRuleSeverity severity;
    private List<OscalModelType> applicableModelTypes = new ArrayList<>();
    private boolean isBuiltIn;
    private String category;
    private String fieldPath;
    private String constraintDetails;

    // Constructors
    public ValidationRule() {
    }

    public ValidationRule(String id, String name, String description, ValidationRuleType ruleType,
                          ValidationRuleSeverity severity, boolean isBuiltIn, String category) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.ruleType = ruleType;
        this.severity = severity;
        this.isBuiltIn = isBuiltIn;
        this.category = category;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public ValidationRuleType getRuleType() {
        return ruleType;
    }

    public void setRuleType(ValidationRuleType ruleType) {
        this.ruleType = ruleType;
    }

    public ValidationRuleSeverity getSeverity() {
        return severity;
    }

    public void setSeverity(ValidationRuleSeverity severity) {
        this.severity = severity;
    }

    public List<OscalModelType> getApplicableModelTypes() {
        return applicableModelTypes;
    }

    public void setApplicableModelTypes(List<OscalModelType> applicableModelTypes) {
        this.applicableModelTypes = applicableModelTypes;
    }

    public boolean isBuiltIn() {
        return isBuiltIn;
    }

    public void setBuiltIn(boolean builtIn) {
        isBuiltIn = builtIn;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getFieldPath() {
        return fieldPath;
    }

    public void setFieldPath(String fieldPath) {
        this.fieldPath = fieldPath;
    }

    public String getConstraintDetails() {
        return constraintDetails;
    }

    public void setConstraintDetails(String constraintDetails) {
        this.constraintDetails = constraintDetails;
    }

    // Helper methods
    public void addApplicableModelType(OscalModelType modelType) {
        if (!this.applicableModelTypes.contains(modelType)) {
            this.applicableModelTypes.add(modelType);
        }
    }

    public boolean isApplicableTo(OscalModelType modelType) {
        return this.applicableModelTypes.isEmpty() || this.applicableModelTypes.contains(modelType);
    }
}
