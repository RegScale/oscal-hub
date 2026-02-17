package gov.nist.oscal.tools.api.model;

import gov.nist.oscal.tools.api.entity.SecurityPolicy;

import java.time.LocalDateTime;

/**
 * Response DTO for Security Policy.
 */
public class SecurityPolicyResponse {

    // MFA Policy
    private Boolean mfaRequired;

    // Password Policy
    private Integer passwordMinLength;
    private Integer passwordMaxLength;
    private Integer passwordRotationDays;

    // Audit Log Retention
    private Integer auditLogRetentionDays;

    // Metadata
    private LocalDateTime updatedAt;
    private String updatedBy;

    public SecurityPolicyResponse() {
    }

    public static SecurityPolicyResponse fromEntity(SecurityPolicy policy) {
        SecurityPolicyResponse response = new SecurityPolicyResponse();
        response.setMfaRequired(policy.getMfaRequired());
        response.setPasswordMinLength(policy.getPasswordMinLength());
        response.setPasswordMaxLength(policy.getPasswordMaxLength());
        response.setPasswordRotationDays(policy.getPasswordRotationDays());
        response.setAuditLogRetentionDays(policy.getAuditLogRetentionDays());
        response.setUpdatedAt(policy.getUpdatedAt());
        response.setUpdatedBy(policy.getUpdatedBy());
        return response;
    }

    // Getters and Setters

    public Boolean getMfaRequired() {
        return mfaRequired;
    }

    public void setMfaRequired(Boolean mfaRequired) {
        this.mfaRequired = mfaRequired;
    }

    public Integer getPasswordMinLength() {
        return passwordMinLength;
    }

    public void setPasswordMinLength(Integer passwordMinLength) {
        this.passwordMinLength = passwordMinLength;
    }

    public Integer getPasswordMaxLength() {
        return passwordMaxLength;
    }

    public void setPasswordMaxLength(Integer passwordMaxLength) {
        this.passwordMaxLength = passwordMaxLength;
    }

    public Integer getPasswordRotationDays() {
        return passwordRotationDays;
    }

    public void setPasswordRotationDays(Integer passwordRotationDays) {
        this.passwordRotationDays = passwordRotationDays;
    }

    public Integer getAuditLogRetentionDays() {
        return auditLogRetentionDays;
    }

    public void setAuditLogRetentionDays(Integer auditLogRetentionDays) {
        this.auditLogRetentionDays = auditLogRetentionDays;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }
}
