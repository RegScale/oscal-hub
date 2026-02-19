package gov.nist.oscal.tools.api.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.ColumnDefault;
import java.time.LocalDateTime;

/**
 * Security Policy Entity (Singleton)
 * <p>
 * Stores global security settings for the platform.
 * This table contains a single row with ID=1 that is seeded on first migration.
 * </p>
 *
 * <h2>Settings</h2>
 * <ul>
 *   <li><b>MFA Policy</b>: Whether MFA is required for all users</li>
 *   <li><b>Password Policy</b>: Minimum/maximum length, rotation requirements</li>
 *   <li><b>Audit Log Retention</b>: Days to keep audit logs</li>
 * </ul>
 */
@Entity
@Table(name = "security_policy")
public class SecurityPolicy {

    public static final Long SINGLETON_ID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ========================================
    // MFA Policy
    // ========================================

    /**
     * When true, all users must configure MFA before accessing the system.
     * Users without MFA setup will be forced to complete setup on next login.
     */
    @Column(name = "mfa_required", nullable = false)
    private Boolean mfaRequired = false;

    // ========================================
    // Password Policy
    // ========================================

    /**
     * Minimum password length (8-128 characters).
     * OWASP recommends minimum 10 characters.
     */
    @Column(name = "password_min_length", nullable = false)
    private Integer passwordMinLength = 10;

    /**
     * Maximum password length (8-128 characters).
     * Set to 128 to prevent DoS via extremely long passwords.
     */
    @Column(name = "password_max_length", nullable = false)
    private Integer passwordMaxLength = 128;

    /**
     * Days before password expires (0 = never expires).
     * When set, users must change password after this many days.
     */
    @Column(name = "password_rotation_days", nullable = false)
    private Integer passwordRotationDays = 0;

    // ========================================
    // Audit Log Retention
    // ========================================

    /**
     * Days to retain audit logs before automatic deletion.
     * Logs older than this will be purged by scheduled cleanup job.
     */
    @Column(name = "audit_log_retention_days", nullable = false)
    private Integer auditLogRetentionDays = 90;

    // ========================================
    // Metadata
    // ========================================

    /**
     * Timestamp of last policy update.
     */
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Username of admin who last updated the policy.
     */
    @Column(name = "updated_by", length = 100)
    private String updatedBy;

    // ========================================
    // Constructors
    // ========================================

    public SecurityPolicy() {
        this.updatedAt = LocalDateTime.now();
    }

    // ========================================
    // Getters and Setters
    // ========================================

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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

    // ========================================
    // Helper Methods
    // ========================================

    /**
     * Check if password rotation is enabled.
     *
     * @return true if passwordRotationDays > 0
     */
    public boolean isPasswordRotationEnabled() {
        return passwordRotationDays != null && passwordRotationDays > 0;
    }

    /**
     * Update timestamp and author before saving.
     */
    @PreUpdate
    @PrePersist
    public void updateTimestamp() {
        this.updatedAt = LocalDateTime.now();
    }
}
