package gov.nist.oscal.tools.api.model;

import gov.nist.oscal.tools.api.entity.ConditionOfApproval;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Request DTO for creating or updating a condition of approval
 */
public class ConditionOfApprovalRequest {

    @NotNull(message = "Authorization ID is required")
    private Long authorizationId;

    @NotBlank(message = "Condition text is required")
    private String condition;

    @NotNull(message = "Condition type is required")
    private ConditionOfApproval.ConditionType conditionType;

    private String dueDate; // ISO date string, optional for RECOMMENDED, required for MANDATORY

    // Constructors
    public ConditionOfApprovalRequest() {
    }

    public ConditionOfApprovalRequest(Long authorizationId, String condition,
                                     ConditionOfApproval.ConditionType conditionType, String dueDate) {
        this.authorizationId = authorizationId;
        this.condition = condition;
        this.conditionType = conditionType;
        this.dueDate = dueDate;
    }

    // Getters and Setters
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

    public String getDueDate() {
        return dueDate;
    }

    public void setDueDate(String dueDate) {
        this.dueDate = dueDate;
    }
}
