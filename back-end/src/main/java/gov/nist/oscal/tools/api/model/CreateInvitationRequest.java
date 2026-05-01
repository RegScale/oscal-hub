package gov.nist.oscal.tools.api.model;

import gov.nist.oscal.tools.api.entity.Invitation;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class CreateInvitationRequest {
    @NotBlank @Email private String email;
    @NotNull private Long organizationId;
    private Invitation.Role role = Invitation.Role.USER;

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public Long getOrganizationId() { return organizationId; }
    public void setOrganizationId(Long organizationId) { this.organizationId = organizationId; }
    public Invitation.Role getRole() { return role; }
    public void setRole(Invitation.Role role) { this.role = role; }
}
