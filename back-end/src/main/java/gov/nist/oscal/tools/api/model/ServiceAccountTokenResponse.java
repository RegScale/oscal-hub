package gov.nist.oscal.tools.api.model;

public class ServiceAccountTokenResponse {

    private Long id;
    private String token;
    private String tokenName;
    private String username;
    private String expiresAt;
    private Integer expirationDays;
    /** The permission snapshot baked into the token, shown so the issuer sees what they handed out. */
    private String globalRole;
    private String orgRole;

    public ServiceAccountTokenResponse() {
    }

    public ServiceAccountTokenResponse(Long id, String token, String tokenName, String username,
                                       String expiresAt, Integer expirationDays,
                                       String globalRole, String orgRole) {
        this.id = id;
        this.token = token;
        this.tokenName = tokenName;
        this.username = username;
        this.expiresAt = expiresAt;
        this.expirationDays = expirationDays;
        this.globalRole = globalRole;
        this.orgRole = orgRole;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getGlobalRole() {
        return globalRole;
    }

    public void setGlobalRole(String globalRole) {
        this.globalRole = globalRole;
    }

    public String getOrgRole() {
        return orgRole;
    }

    public void setOrgRole(String orgRole) {
        this.orgRole = orgRole;
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
