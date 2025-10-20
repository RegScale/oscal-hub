package gov.nist.oscal.tools.api.model;

import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for creating or updating an authorization template
 */
public class AuthorizationTemplateRequest {

    @NotBlank(message = "Template name is required")
    private String name;

    @NotBlank(message = "Template content is required")
    private String content;

    // Constructors
    public AuthorizationTemplateRequest() {
    }

    public AuthorizationTemplateRequest(String name, String content) {
        this.name = name;
        this.content = content;
    }

    // Getters and Setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
