package gov.nist.oscal.tools.api.model;

import jakarta.validation.constraints.NotNull;

public class AddMemberRequest {

    @NotNull(message = "User ID is required")
    private Long userId;

    @NotNull(message = "Role is required")
    private String role; // "USER" or "ORG_ADMIN"

    // Constructors
    public AddMemberRequest() {
    }

    public AddMemberRequest(Long userId, String role) {
        this.userId = userId;
        this.role = role;
    }

    // Getters and Setters
    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }
}
