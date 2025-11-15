package gov.nist.oscal.tools.api.model;

import jakarta.validation.constraints.NotNull;

public class AssignAdminRequest {

    @NotNull(message = "User ID is required")
    private Long userId;

    // Constructors
    public AssignAdminRequest() {
    }

    public AssignAdminRequest(Long userId) {
        this.userId = userId;
    }

    // Getters and Setters
    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }
}
