package gov.nist.oscal.tools.api.model.library;

import gov.nist.oscal.tools.api.entity.Visibility;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class VisibilityChangeRequest {

    @NotNull
    private Visibility visibility;

    private Long organizationId;       // required when visibility=ORGANIZATION

    @Size(max = 500)
    private String reason;             // required when SUPER_ADMIN force-unpublishes

    public Visibility getVisibility() { return visibility; }
    public void setVisibility(Visibility visibility) { this.visibility = visibility; }
    public Long getOrganizationId() { return organizationId; }
    public void setOrganizationId(Long organizationId) { this.organizationId = organizationId; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
