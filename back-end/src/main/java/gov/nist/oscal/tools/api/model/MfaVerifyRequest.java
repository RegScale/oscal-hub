package gov.nist.oscal.tools.api.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Request to verify MFA during login.
 */
public class MfaVerifyRequest {

    @NotBlank(message = "MFA token is required")
    private String mfaToken;

    @NotBlank(message = "TOTP code is required")
    @Pattern(regexp = "^[0-9]{6}$", message = "TOTP code must be 6 digits")
    private String totpCode;

    public MfaVerifyRequest() {
    }

    public MfaVerifyRequest(String mfaToken, String totpCode) {
        this.mfaToken = mfaToken;
        this.totpCode = totpCode;
    }

    public String getMfaToken() {
        return mfaToken;
    }

    public void setMfaToken(String mfaToken) {
        this.mfaToken = mfaToken;
    }

    public String getTotpCode() {
        return totpCode;
    }

    public void setTotpCode(String totpCode) {
        this.totpCode = totpCode;
    }
}
