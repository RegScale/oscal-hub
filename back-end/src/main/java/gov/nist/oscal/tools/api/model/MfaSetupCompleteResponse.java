package gov.nist.oscal.tools.api.model;

import java.util.List;

/**
 * Response for successful MFA setup completion.
 * Contains the full JWT token and backup codes.
 */
public class MfaSetupCompleteResponse {

    private String token;
    private List<String> backupCodes;
    private String message;

    public MfaSetupCompleteResponse() {
    }

    public MfaSetupCompleteResponse(String token, List<String> backupCodes) {
        this.token = token;
        this.backupCodes = backupCodes;
        this.message = "MFA setup completed successfully. Please save your backup codes.";
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public List<String> getBackupCodes() {
        return backupCodes;
    }

    public void setBackupCodes(List<String> backupCodes) {
        this.backupCodes = backupCodes;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
