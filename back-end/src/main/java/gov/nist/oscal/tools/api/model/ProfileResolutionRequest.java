package gov.nist.oscal.tools.api.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class ProfileResolutionRequest {
    @NotBlank(message = "Profile content is required")
    private String profileContent;

    @NotNull(message = "Format is required")
    private OscalFormat format;

    // Getters and Setters
    public String getProfileContent() {
        return profileContent;
    }

    public void setProfileContent(String profileContent) {
        this.profileContent = profileContent;
    }

    public OscalFormat getFormat() {
        return format;
    }

    public void setFormat(OscalFormat format) {
        this.format = format;
    }
}
