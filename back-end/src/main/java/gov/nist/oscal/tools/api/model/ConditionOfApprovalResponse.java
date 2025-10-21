package gov.nist.oscal.tools.api.model;

import gov.nist.oscal.tools.api.entity.ConditionOfApproval;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Response DTO for condition of approval
 */
public class ConditionOfApprovalResponse {

    private Long id;
    private Long authorizationId;
    private String condition;
    private ConditionOfApproval.ConditionType conditionType;
    private LocalDate dueDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Constructors
    public ConditionOfApprovalResponse() {
    }

    public ConditionOfApprovalResponse(ConditionOfApproval condition) {
        this.id = condition.getId();
        this.authorizationId = condition.getAuthorization().getId();
        this.condition = condition.getCondition();
        this.conditionType = condition.getConditionType();
        this.dueDate = condition.getDueDate();
        this.createdAt = condition.getCreatedAt();
        this.updatedAt = condition.getUpdatedAt();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getAuthorizationId() {
        return authorizationId;
    }

    public void setAuthorizationId(Long authorizationId) {
        this.authorizationId = authorizationId;
    }

    public String getCondition() {
        return condition;
    }

    public void setCondition(String condition) {
        this.condition = condition;
    }

    public ConditionOfApproval.ConditionType getConditionType() {
        return conditionType;
    }

    public void setConditionType(ConditionOfApproval.ConditionType conditionType) {
        this.conditionType = conditionType;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
