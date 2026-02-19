package gov.nist.oscal.tools.api.model;

import gov.nist.oscal.tools.api.entity.Artifact.ArtifactVisibility;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Set;

/**
 * Request DTO for creating a new artifact
 */
public class ArtifactRequest {

    @NotBlank(message = "Title is required")
    private String title;

    private String description;

    @NotNull(message = "Visibility is required")
    private ArtifactVisibility visibility;

    private Long organizationId; // Required when visibility = ORGANIZATION

    @NotNull(message = "Content is required")
    private String content; // Markdown content

    private Set<String> tags;

    // Constructors
    public ArtifactRequest() {
    }

    public ArtifactRequest(String title, String description, ArtifactVisibility visibility,
                          Long organizationId, String content, Set<String> tags) {
        this.title = title;
        this.description = description;
        this.visibility = visibility;
        this.organizationId = organizationId;
        this.content = content;
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

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Set<String> getTags() {
        return tags;
    }

    public void setTags(Set<String> tags) {
        this.tags = tags;
    }
}
