package gov.nist.oscal.tools.api.model;

import gov.nist.oscal.tools.api.entity.AuthorizationGrant;
import gov.nist.oscal.tools.api.entity.AuthorizationRole;

import java.time.LocalDateTime;

public class AuthorizationGrantResponse {

    private Long id;
    private Long userId;
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private AuthorizationRole role;
    private String grantedByUsername;
    private LocalDateTime grantedAt;

    public AuthorizationGrantResponse() {}

    public AuthorizationGrantResponse(AuthorizationGrant grant) {
        this.id = grant.getId();
        this.userId = grant.getUser().getId();
        this.username = grant.getUser().getUsername();
        this.email = grant.getUser().getEmail();
        this.firstName = grant.getUser().getFirstName();
        this.lastName = grant.getUser().getLastName();
        this.role = grant.getRole();
        this.grantedByUsername = grant.getGrantedBy() != null ? grant.getGrantedBy().getUsername() : null;
        this.grantedAt = grant.getGrantedAt();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public AuthorizationRole getRole() { return role; }
    public void setRole(AuthorizationRole role) { this.role = role; }
    public String getGrantedByUsername() { return grantedByUsername; }
    public void setGrantedByUsername(String grantedByUsername) { this.grantedByUsername = grantedByUsername; }
    public LocalDateTime getGrantedAt() { return grantedAt; }
    public void setGrantedAt(LocalDateTime grantedAt) { this.grantedAt = grantedAt; }
}
