package gov.nist.oscal.tools.api.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * A long-lived API credential issued from the Profile page.
 * <p>
 * Unlike {@link PasswordResetToken}, no hash of the token is stored. The JWT
 * signature already proves authenticity; this row exists so the token can be
 * listed and revoked, keyed by the {@code jti} claim embedded in the JWT.
 * </p>
 */
@Entity
@Table(name = "service_account_tokens")
public class ServiceAccountToken {

    public enum Status { ACTIVE, EXPIRED, REVOKED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "token_name", nullable = false)
    private String tokenName;

    /** UUID matching the JWT's {@code jti} claim. */
    @Column(name = "jti", nullable = false, unique = true, length = 36)
    private String jti;

    /** Permissions snapshotted at issuance, retained for display and audit. */
    @Column(name = "global_role", length = 50)
    private String globalRole;

    @Column(name = "org_role", length = 50)
    private String orgRole;

    @Column(name = "organization_id")
    private Long organizationId;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;

    @Column(name = "revoked_by")
    private String revokedBy;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    /** Written at most once per hour to keep the auth path cheap. */
    @Column(name = "last_used_at")
    private LocalDateTime lastUsedAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }

    public Status getStatus() {
        if (revokedAt != null) return Status.REVOKED;
        if (expiresAt != null && expiresAt.isBefore(LocalDateTime.now())) return Status.EXPIRED;
        return Status.ACTIVE;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public String getTokenName() { return tokenName; }
    public void setTokenName(String tokenName) { this.tokenName = tokenName; }

    public String getJti() { return jti; }
    public void setJti(String jti) { this.jti = jti; }

    public String getGlobalRole() { return globalRole; }
    public void setGlobalRole(String globalRole) { this.globalRole = globalRole; }

    public String getOrgRole() { return orgRole; }
    public void setOrgRole(String orgRole) { this.orgRole = orgRole; }

    public Long getOrganizationId() { return organizationId; }
    public void setOrganizationId(Long organizationId) { this.organizationId = organizationId; }

    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }

    public LocalDateTime getRevokedAt() { return revokedAt; }
    public void setRevokedAt(LocalDateTime revokedAt) { this.revokedAt = revokedAt; }

    public String getRevokedBy() { return revokedBy; }
    public void setRevokedBy(String revokedBy) { this.revokedBy = revokedBy; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getLastUsedAt() { return lastUsedAt; }
    public void setLastUsedAt(LocalDateTime lastUsedAt) { this.lastUsedAt = lastUsedAt; }
}
