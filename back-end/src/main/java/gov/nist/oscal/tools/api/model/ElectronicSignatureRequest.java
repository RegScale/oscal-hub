package gov.nist.oscal.tools.api.model;

/**
 * Request to sign an authorization with electronic signature (drawn signature image)
 */
public class ElectronicSignatureRequest {

    private Long authorizationId;
    private String signerName; // Full name typed by user
    private String signerTitle; // Optional title/position
    private String signatureImageData; // Base64-encoded PNG image

    // Constructors
    public ElectronicSignatureRequest() {
    }

    public ElectronicSignatureRequest(Long authorizationId, String signerName, String signerTitle, String signatureImageData) {
        this.authorizationId = authorizationId;
        this.signerName = signerName;
        this.signerTitle = signerTitle;
        this.signatureImageData = signatureImageData;
    }

    // Getters and Setters
    public Long getAuthorizationId() {
        return authorizationId;
    }

    public void setAuthorizationId(Long authorizationId) {
        this.authorizationId = authorizationId;
    }

    public String getSignerName() {
        return signerName;
    }

    public void setSignerName(String signerName) {
        this.signerName = signerName;
    }

    public String getSignerTitle() {
        return signerTitle;
    }

    public void setSignerTitle(String signerTitle) {
        this.signerTitle = signerTitle;
    }

    public String getSignatureImageData() {
        return signatureImageData;
    }

    public void setSignatureImageData(String signatureImageData) {
        this.signatureImageData = signatureImageData;
    }
}
