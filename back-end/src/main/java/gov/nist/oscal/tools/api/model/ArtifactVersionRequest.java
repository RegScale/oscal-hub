package gov.nist.oscal.tools.api.model;

import jakarta.validation.constraints.NotNull;

/**
 * Request DTO for adding a new artifact version
 */
public class ArtifactVersionRequest {

    @NotNull(message = "Content is required")
    private String content; // Markdown content

    private String changeDescription;

    // Constructors
    public ArtifactVersionRequest() {
    }

    public ArtifactVersionRequest(String content, String changeDescription) {
        this.content = content;
        this.changeDescription = changeDescription;
    }

    // Getters and Setters
    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getChangeDescription() {
        return changeDescription;
    }

    public void setChangeDescription(String changeDescription) {
        this.changeDescription = changeDescription;
    }
}
