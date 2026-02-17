package gov.nist.oscal.tools.api.model;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Request DTO for updating Security Policy.
 */
public class SecurityPolicyUpdateRequest {

    @NotNull(message = "MFA required flag is required")
    private Boolean mfaRequired;

    @NotNull(message = "Minimum password length is required")
    @Min(value = 8, message = "Minimum password length must be at least 8")
    @Max(value = 128, message = "Minimum password length cannot exceed 128")
    private Integer passwordMinLength;

    @NotNull(message = "Maximum password length is required")
    @Min(value = 8, message = "Maximum password length must be at least 8")
    @Max(value = 128, message = "Maximum password length cannot exceed 128")
    private Integer passwordMaxLength;

    @NotNull(message = "Password rotation days is required")
    @Min(value = 0, message = "Password rotation days cannot be negative")
    @Max(value = 365, message = "Password rotation days cannot exceed 365")
    private Integer passwordRotationDays;

    @NotNull(message = "Audit log retention days is required")
    @Min(value = 1, message = "Audit log retention must be at least 1 day")
    @Max(value = 3650, message = "Audit log retention cannot exceed 10 years (3650 days)")
    private Integer auditLogRetentionDays;

    public SecurityPolicyUpdateRequest() {
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
}
