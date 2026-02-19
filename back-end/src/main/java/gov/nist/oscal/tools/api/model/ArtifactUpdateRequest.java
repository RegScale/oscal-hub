package gov.nist.oscal.tools.api.model;

import gov.nist.oscal.tools.api.entity.Artifact.ArtifactVisibility;
import java.util.Set;

/**
 * Request DTO for updating an artifact's metadata
 */
public class ArtifactUpdateRequest {

    private String title;
    private String description;
    private ArtifactVisibility visibility;
    private Long organizationId;
    private Set<String> tags;

    // Constructors
    public ArtifactUpdateRequest() {
    }

    public ArtifactUpdateRequest(String title, String description, ArtifactVisibility visibility,
                                Long organizationId, Set<String> tags) {
        this.title = title;
        this.description = description;
        this.visibility = visibility;
        this.organizationId = organizationId;
        this.tags = tags;
    }

    // Getters and Setters
    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public ArtifactVisibility getVisibility() {
        return visibility;
    }

    public void setVisibility(ArtifactVisibility visibility) {
        this.visibility = visibility;
    }

    public Long getOrganizationId() {
        return organizationId;
    }

    public void setOrganizationId(Long organizationId) {
        this.organizationId = organizationId;
    }

    public Set<String> getTags() {
        return tags;
    }

    public void setTags(Set<String> tags) {
        this.tags = tags;
    }
}
