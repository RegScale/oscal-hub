package gov.nist.oscal.tools.api.model;

import gov.nist.oscal.tools.api.entity.User;

public class AuthResponse {

    private String token;
    private String username;
    private String email;
    private Long userId;
    private String globalRole;
    private String firstName;
    private String lastName;

    // MFA fields
    private Boolean mfaRequired;
    private Boolean mfaSetupRequired;
    private String mfaToken;

    public AuthResponse() {
    }

    public AuthResponse(String token, String username, String email, Long userId) {
        this.token = token;
        this.username = username;
        this.email = email;
        this.userId = userId;
    }

    public AuthResponse(String token, String username, String email, Long userId, String globalRole) {
        this.token = token;
        this.username = username;
        this.email = email;
        this.userId = userId;
        this.globalRole = globalRole;
    }

    /**
     * Constructor for MFA responses.
     */
    public AuthResponse(String token, User user) {
        this.token = token;
        this.username = user.getUsername();
        this.email = user.getEmail();
        this.userId = user.getId();
        this.globalRole = user.getGlobalRole() != null ? user.getGlobalRole().name() : null;
        this.firstName = user.getFirstName();
        this.lastName = user.getLastName();
    }

    /**
     * Create an MFA required response (user has MFA enabled, needs to verify).
     */
    public static AuthResponse mfaRequired(String mfaToken, User user) {
        AuthResponse response = new AuthResponse();
        response.setMfaRequired(true);
        response.setMfaToken(mfaToken);
        response.setUsername(user.getUsername());
        response.setEmail(user.getEmail());
        response.setUserId(user.getId());
        response.setFirstName(user.getFirstName());
        response.setLastName(user.getLastName());
        return response;
    }

    /**
     * Create an MFA setup required response (MFA is mandatory but user hasn't set it up).
     */
    public static AuthResponse mfaSetupRequired(String mfaToken, User user) {
        AuthResponse response = new AuthResponse();
        response.setMfaSetupRequired(true);
        response.setMfaToken(mfaToken);
        response.setUsername(user.getUsername());
        response.setEmail(user.getEmail());
        response.setUserId(user.getId());
        response.setFirstName(user.getFirstName());
        response.setLastName(user.getLastName());
        return response;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getGlobalRole() {
        return globalRole;
    }

    public void setGlobalRole(String globalRole) {
        this.globalRole = globalRole;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public Boolean getMfaRequired() {
        return mfaRequired;
    }

    public void setMfaRequired(Boolean mfaRequired) {
        this.mfaRequired = mfaRequired;
    }

    public Boolean getMfaSetupRequired() {
        return mfaSetupRequired;
    }

    public void setMfaSetupRequired(Boolean mfaSetupRequired) {
        this.mfaSetupRequired = mfaSetupRequired;
    }

    public String getMfaToken() {
        return mfaToken;
    }

    public void setMfaToken(String mfaToken) {
        this.mfaToken = mfaToken;
    }
}
