package gov.nist.oscal.tools.api.model;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.List;

public class CustomRuleRequest {

    @NotBlank(message = "Rule ID is required")
    private String ruleId;

    @NotBlank(message = "Rule name is required")
    private String name;

    private String description;

    @NotBlank(message = "Rule type is required")
    private String ruleType; // required-field, pattern-match, jsonpath, xpath, custom

    @NotBlank(message = "Severity is required")
    private String severity; // error, warning, info

    private String category;
    private String fieldPath;
    private String ruleExpression;
    private String constraintDetails;

    private List<String> applicableModelTypes;

    @NotNull(message = "Enabled status is required")
    private Boolean enabled;

    // Constructors
    public CustomRuleRequest() {
    }

    // Getters and Setters
    public String getRuleId() {
        return ruleId;
    }

    public void setRuleId(String ruleId) {
        this.ruleId = ruleId;
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

    public String getRuleType() {
        return ruleType;
    }

    public void setRuleType(String ruleType) {
        this.ruleType = ruleType;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
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

    public String getRuleExpression() {
        return ruleExpression;
    }

    public void setRuleExpression(String ruleExpression) {
        this.ruleExpression = ruleExpression;
    }

    public String getConstraintDetails() {
        return constraintDetails;
    }

    public void setConstraintDetails(String constraintDetails) {
        this.constraintDetails = constraintDetails;
    }

    public List<String> getApplicableModelTypes() {
        return applicableModelTypes;
    }

    public void setApplicableModelTypes(List<String> applicableModelTypes) {
        this.applicableModelTypes = applicableModelTypes;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }
}
