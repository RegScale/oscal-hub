package gov.nist.oscal.tools.api.model;

import gov.nist.oscal.tools.api.entity.AuthorizationRole;

public class ShareWithOrgRequest {

    /** May be null to clear the share-with-org default. */
    private AuthorizationRole role;

    public AuthorizationRole getRole() { return role; }
    public void setRole(AuthorizationRole role) { this.role = role; }
}
