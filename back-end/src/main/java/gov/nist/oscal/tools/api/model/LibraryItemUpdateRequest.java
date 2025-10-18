package gov.nist.oscal.tools.api.model;

import java.util.Set;

/**
 * Request DTO for updating library item metadata
 */
public class LibraryItemUpdateRequest {

    private String title;
    private String description;
    private Set<String> tags;

    // Constructors
    public LibraryItemUpdateRequest() {
    }

    public LibraryItemUpdateRequest(String title, String description, Set<String> tags) {
        this.title = title;
        this.description = description;
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

    public Set<String> getTags() {
        return tags;
    }

    public void setTags(Set<String> tags) {
        this.tags = tags;
    }
}
