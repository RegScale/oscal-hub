package gov.nist.oscal.tools.api.model;

import gov.nist.oscal.tools.api.entity.CustomValidationRule;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class CustomRuleResponse {

    private Long id;
    private String ruleId;
    private String name;
    private String description;
    private String ruleType;
    private String severity;
    private String category;
    private String fieldPath;
    private String ruleExpression;
    private String constraintDetails;
    private List<String> applicableModelTypes;
    private Boolean enabled;
    private LocalDateTime createdDate;
    private LocalDateTime updatedDate;
    private String createdBy;

    // Constructors
    public CustomRuleResponse() {
    }

    public static CustomRuleResponse fromEntity(CustomValidationRule entity) {
        CustomRuleResponse response = new CustomRuleResponse();
        response.setId(entity.getId());
        response.setRuleId(entity.getRuleId());
        response.setName(entity.getName());
        response.setDescription(entity.getDescription());
        response.setRuleType(entity.getRuleType());
        response.setSeverity(entity.getSeverity());
        response.setCategory(entity.getCategory());
        response.setFieldPath(entity.getFieldPath());
        response.setRuleExpression(entity.getRuleExpression());
        response.setConstraintDetails(entity.getConstraintDetails());

        // Parse comma-separated model types
        if (entity.getApplicableModelTypes() != null && !entity.getApplicableModelTypes().isEmpty()) {
            response.setApplicableModelTypes(
                Arrays.asList(entity.getApplicableModelTypes().split(","))
            );
        } else {
            response.setApplicableModelTypes(Collections.emptyList());
        }

        response.setEnabled(entity.getEnabled());
        response.setCreatedDate(entity.getCreatedDate());
        response.setUpdatedDate(entity.getUpdatedDate());
        response.setCreatedBy(entity.getCreatedBy());

        return response;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(LocalDateTime createdDate) {
        this.createdDate = createdDate;
    }

    public LocalDateTime getUpdatedDate() {
        return updatedDate;
    }

    public void setUpdatedDate(LocalDateTime updatedDate) {
        this.updatedDate = updatedDate;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }
}
