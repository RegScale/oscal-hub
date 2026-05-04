package gov.nist.oscal.tools.api.model.library;

import gov.nist.oscal.tools.api.entity.Visibility;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Set;

/**
 * Request body for the per-builder {@code POST /save-to-library} endpoints.
 * The builder row id comes from the URL; this DTO carries the user-supplied
 * library metadata.
 */
public class SaveToLibraryRequest {

    @NotBlank
    @Size(max = 255)
    private String title;

    @Size(max = 2000)
    private String description;

    private Set<String> tags;

    private Visibility visibility = Visibility.PRIVATE;

    private Long organizationId;

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Set<String> getTags() { return tags; }
    public void setTags(Set<String> tags) { this.tags = tags; }

    public Visibility getVisibility() { return visibility; }
    public void setVisibility(Visibility visibility) { this.visibility = visibility; }

    public Long getOrganizationId() { return organizationId; }
    public void setOrganizationId(Long organizationId) { this.organizationId = organizationId; }
}
