package gov.nist.oscal.tools.api.model;

/**
 * Response for MFA setup initiation.
 * Contains the QR code and secret for authenticator app setup.
 */
public class MfaSetupResponse {

    private String qrCodeDataUri;
    private String secret;
    private String formattedSecret;
    private String setupToken;

    public MfaSetupResponse() {
    }

    public MfaSetupResponse(String qrCodeDataUri, String secret, String formattedSecret, String setupToken) {
        this.qrCodeDataUri = qrCodeDataUri;
        this.secret = secret;
        this.formattedSecret = formattedSecret;
        this.setupToken = setupToken;
    }

    public String getQrCodeDataUri() {
        return qrCodeDataUri;
    }

    public void setQrCodeDataUri(String qrCodeDataUri) {
        this.qrCodeDataUri = qrCodeDataUri;
    }

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public String getFormattedSecret() {
        return formattedSecret;
    }

    public void setFormattedSecret(String formattedSecret) {
        this.formattedSecret = formattedSecret;
    }

    public String getSetupToken() {
        return setupToken;
    }

    public void setSetupToken(String setupToken) {
        this.setupToken = setupToken;
    }
}
