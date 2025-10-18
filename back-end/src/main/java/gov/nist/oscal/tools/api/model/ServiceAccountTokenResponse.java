package gov.nist.oscal.tools.api.model;

public class ServiceAccountTokenResponse {

    private String token;
    private String tokenName;
    private String username;
    private String expiresAt;
    private Integer expirationDays;

    public ServiceAccountTokenResponse() {
    }

    public ServiceAccountTokenResponse(String token, String tokenName, String username, String expiresAt, Integer expirationDays) {
        this.token = token;
        this.tokenName = tokenName;
        this.username = username;
        this.expiresAt = expiresAt;
        this.expirationDays = expirationDays;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getTokenName() {
        return tokenName;
    }

    public void setTokenName(String tokenName) {
        this.tokenName = tokenName;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(String expiresAt) {
        this.expiresAt = expiresAt;
    }

    public Integer getExpirationDays() {
        return expirationDays;
    }

    public void setExpirationDays(Integer expirationDays) {
        this.expirationDays = expirationDays;
    }
}
