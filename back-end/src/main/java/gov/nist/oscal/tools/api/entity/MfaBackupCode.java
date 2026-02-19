package gov.nist.oscal.tools.api.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * MFA Backup Code Entity
 * <p>
 * Stores hashed backup codes for MFA recovery.
 * Users receive 10 one-time backup codes when they enable MFA.
 * Each code can only be used once.
 * </p>
 *
 * <h2>Security</h2>
 * <ul>
 *   <li>Codes are stored as SHA-256 hashes (never plaintext)</li>
 *   <li>Each code can only be used once</li>
 *   <li>Codes are deleted when user disables MFA or regenerates codes</li>
 * </ul>
 */
@Entity
@Table(name = "mfa_backup_codes",
       indexes = {
           @Index(name = "idx_backup_codes_user_id", columnList = "user_id"),
           @Index(name = "idx_backup_codes_hash", columnList = "code_hash")
       })
public class MfaBackupCode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * User who owns this backup code.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * SHA-256 hash of the backup code.
     * Never store the plaintext code.
     */
    @Column(name = "code_hash", nullable = false, length = 64)
    private String codeHash;

    /**
     * Whether this backup code has been used.
     */
    @Column(name = "used", nullable = false)
    private Boolean used = false;

    /**
     * Timestamp when the backup code was used.
     */
    @Column(name = "used_at")
    private LocalDateTime usedAt;

    /**
     * Timestamp when the backup code was created.
     */
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    // ========================================
    // Constructors
    // ========================================

    public MfaBackupCode() {
        this.createdAt = LocalDateTime.now();
    }

    public MfaBackupCode(User user, String codeHash) {
        this();
        this.user = user;
        this.codeHash = codeHash;
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

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getCodeHash() {
        return codeHash;
    }

    public void setCodeHash(String codeHash) {
        this.codeHash = codeHash;
    }

    public Boolean getUsed() {
        return used;
    }

    public void setUsed(Boolean used) {
        this.used = used;
    }

    public LocalDateTime getUsedAt() {
        return usedAt;
    }

    public void setUsedAt(LocalDateTime usedAt) {
        this.usedAt = usedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    // ========================================
    // Helper Methods
    // ========================================

    /**
     * Mark this backup code as used.
     */
    public void markAsUsed() {
        this.used = true;
        this.usedAt = LocalDateTime.now();
    }
}
