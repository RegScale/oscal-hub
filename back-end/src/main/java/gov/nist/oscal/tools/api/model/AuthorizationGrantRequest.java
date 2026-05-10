package gov.nist.oscal.tools.api.model;

import gov.nist.oscal.tools.api.entity.AuthorizationRole;
import jakarta.validation.constraints.NotNull;

public class AuthorizationGrantRequest {

    @NotNull
    private Long userId;

    @NotNull
    private AuthorizationRole role;

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public AuthorizationRole getRole() { return role; }
    public void setRole(AuthorizationRole role) { this.role = role; }
}
