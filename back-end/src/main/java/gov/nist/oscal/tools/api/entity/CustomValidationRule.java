package gov.nist.oscal.tools.api.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "custom_validation_rules")
public class CustomValidationRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String ruleId; // Unique identifier like "custom-rule-001"

    @Column(nullable = false, length = 255)
    private String name;

    @Column(length = 1000)
    private String description;

    @Column(nullable = false, length = 50)
    private String ruleType; // required-field, pattern-match, jsonpath, xpath, custom

    @Column(nullable = false, length = 20)
    private String severity; // error, warning, info

    @Column(length = 500)
    private String category;

    @Column(length = 500)
    private String fieldPath;

    @Column(length = 2000)
    private String ruleExpression; // JSONPath, XPath, or custom expression

    @Column(length = 1000)
    private String constraintDetails;

    @Column(name = "applicable_model_types", length = 500)
    private String applicableModelTypes; // Comma-separated list: catalog,profile,ssp

    @Column(nullable = false)
    private Boolean enabled = true;

    @Column(nullable = false)
    private LocalDateTime createdDate;

    @Column(nullable = false)
    private LocalDateTime updatedDate;

    @Column(length = 100)
    private String createdBy;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user; // User who owns this custom rule

    // Constructors
    public CustomValidationRule() {
        this.createdDate = LocalDateTime.now();
        this.updatedDate = LocalDateTime.now();
    }

    public CustomValidationRule(String ruleId, String name, String description,
                                String ruleType, String severity) {
        this();
        this.ruleId = ruleId;
        this.name = name;
        this.description = description;
        this.ruleType = ruleType;
        this.severity = severity;
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

    public String getApplicableModelTypes() {
        return applicableModelTypes;
    }

    public void setApplicableModelTypes(String applicableModelTypes) {
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

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedDate = LocalDateTime.now();
    }
}
