package gov.nist.oscal.tools.api.model;

import gov.nist.oscal.tools.api.entity.Invitation;
import java.time.LocalDateTime;

public class InvitationResponse {
    private Long id;
    private String email;
    private String organizationName;
    private String inviterName;
    private String role;
    private String status;
    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;

    public static InvitationResponse from(Invitation inv) {
        InvitationResponse r = new InvitationResponse();
        r.id = inv.getId();
        r.email = inv.getEmail();
        r.organizationName = inv.getOrganization() == null ? null : inv.getOrganization().getName();
        r.inviterName = inv.getInvitedBy() == null ? null : inv.getInvitedBy().getUsername();
        r.role = inv.getRole().name();
        r.status = inv.getStatus().name();
        r.expiresAt = inv.getExpiresAt();
        r.createdAt = inv.getCreatedAt();
        return r;
    }

    public Long getId() { return id; }
    public String getEmail() { return email; }
    public String getOrganizationName() { return organizationName; }
    public String getInviterName() { return inviterName; }
    public String getRole() { return role; }
    public String getStatus() { return status; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
