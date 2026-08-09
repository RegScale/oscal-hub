package gov.nist.oscal.tools.api.model;

import gov.nist.oscal.tools.api.entity.ServiceAccountToken;
import java.time.LocalDateTime;

/**
 * Listing view of a service account token. Deliberately has no {@code token}
 * field — the value is shown once at creation and is never retrievable.
 */
public class ServiceAccountTokenSummary {

    private Long id;
    private String tokenName;
    private String globalRole;
    private String orgRole;
    private Long organizationId;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
    private LocalDateTime lastUsedAt;
    private LocalDateTime revokedAt;
    private String status;

    public static ServiceAccountTokenSummary from(ServiceAccountToken t) {
        ServiceAccountTokenSummary s = new ServiceAccountTokenSummary();
        s.id = t.getId();
        s.tokenName = t.getTokenName();
        s.globalRole = t.getGlobalRole();
        s.orgRole = t.getOrgRole();
        s.organizationId = t.getOrganizationId();
        s.createdAt = t.getCreatedAt();
        s.expiresAt = t.getExpiresAt();
        s.lastUsedAt = t.getLastUsedAt();
        s.revokedAt = t.getRevokedAt();
        s.status = t.getStatus().name();
        return s;
    }

    public Long getId() { return id; }
    public String getTokenName() { return tokenName; }
    public String getGlobalRole() { return globalRole; }
    public String getOrgRole() { return orgRole; }
    public Long getOrganizationId() { return organizationId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public LocalDateTime getLastUsedAt() { return lastUsedAt; }
    public LocalDateTime getRevokedAt() { return revokedAt; }
    public String getStatus() { return status; }
}
