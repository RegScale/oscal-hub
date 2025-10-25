package gov.nist.oscal.tools.api.model;

/**
 * Request to sign an authorization with CAC/PIV certificate
 * The certificate is extracted from the TLS connection, not the request body
 */
public class SignRequest {

    private Long authorizationId;

    // Constructors
    public SignRequest() {
    }

    public SignRequest(Long authorizationId) {
        this.authorizationId = authorizationId;
    }

    // Getters and Setters
    public Long getAuthorizationId() {
        return authorizationId;
    }

    public void setAuthorizationId(Long authorizationId) {
        this.authorizationId = authorizationId;
    }
}
