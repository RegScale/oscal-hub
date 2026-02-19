package gov.nist.oscal.tools.api.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Request to complete MFA setup.
 */
public class MfaSetupCompleteRequest {

    @NotBlank(message = "TOTP code is required")
    @Pattern(regexp = "^[0-9]{6}$", message = "TOTP code must be 6 digits")
    private String totpCode;

    @NotBlank(message = "Setup token is required")
    private String setupToken;

    public MfaSetupCompleteRequest() {
    }

    public MfaSetupCompleteRequest(String totpCode, String setupToken) {
        this.totpCode = totpCode;
        this.setupToken = setupToken;
    }

    public String getTotpCode() {
        return totpCode;
    }

    public void setTotpCode(String totpCode) {
        this.totpCode = totpCode;
    }

    public String getSetupToken() {
        return setupToken;
    }

    public void setSetupToken(String setupToken) {
        this.setupToken = setupToken;
    }
}
