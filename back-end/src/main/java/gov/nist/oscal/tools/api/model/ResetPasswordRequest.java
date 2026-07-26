package gov.nist.oscal.tools.api.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ResetPasswordRequest {

    @NotBlank(message = "Reset token is required")
    @Size(max = 128, message = "Token must not exceed 128 characters")
    private String token;

    // Length/complexity are enforced by PasswordValidationService against the
    // admin-editable policy — no duplicate bounds here.
    @NotBlank(message = "New password is required")
    @Size(max = 128, message = "Password must not exceed 128 characters")
    private String newPassword;

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public String getNewPassword() { return newPassword; }
    public void setNewPassword(String newPassword) { this.newPassword = newPassword; }
}
