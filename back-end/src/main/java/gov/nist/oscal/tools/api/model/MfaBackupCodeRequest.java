package gov.nist.oscal.tools.api.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Request to verify a backup code during login.
 */
public class MfaBackupCodeRequest {

    @NotBlank(message = "MFA token is required")
    private String mfaToken;

    @NotBlank(message = "Backup code is required")
    @Pattern(regexp = "^[0-9]{4}-?[0-9]{4}$", message = "Backup code must be 8 digits (format: XXXX-XXXX)")
    private String backupCode;

    public MfaBackupCodeRequest() {
    }

    public MfaBackupCodeRequest(String mfaToken, String backupCode) {
        this.mfaToken = mfaToken;
        this.backupCode = backupCode;
    }

    public String getMfaToken() {
        return mfaToken;
    }

    public void setMfaToken(String mfaToken) {
        this.mfaToken = mfaToken;
    }

    public String getBackupCode() {
        return backupCode;
    }

    public void setBackupCode(String backupCode) {
        this.backupCode = backupCode;
    }
}
