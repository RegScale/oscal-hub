package gov.nist.oscal.tools.api.model;

import jakarta.validation.constraints.NotNull;

public class UpdateMemberRoleRequest {

    @NotNull(message = "Role is required")
    private String role; // "USER" or "ORG_ADMIN"

    // Constructors
    public UpdateMemberRoleRequest() {
    }

    public UpdateMemberRoleRequest(String role) {
        this.role = role;
    }

    // Getters and Setters
    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }
}
