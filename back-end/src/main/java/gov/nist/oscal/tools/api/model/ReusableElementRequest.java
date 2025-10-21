package gov.nist.oscal.tools.api.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Request DTO for creating or updating a reusable element
 */
public class ReusableElementRequest {

    @NotBlank(message = "Element type is required")
    private String type; // ROLE, PARTY, LINK, BACK_MATTER, RESPONSIBLE_PARTY

    @NotBlank(message = "Name is required")
    private String name;

    @NotNull(message = "JSON content is required")
    private String jsonContent;

    private String description;

    private Boolean isShared = false;

    // Constructors
    public ReusableElementRequest() {
    }

    public ReusableElementRequest(String type, String name, String jsonContent) {
        this.type = type;
        this.name = name;
        this.jsonContent = jsonContent;
    }

    public ReusableElementRequest(String type, String name, String jsonContent, String description) {
        this.type = type;
        this.name = name;
        this.jsonContent = jsonContent;
        this.description = description;
    }

    // Getters and Setters
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getJsonContent() {
        return jsonContent;
    }

    public void setJsonContent(String jsonContent) {
        this.jsonContent = jsonContent;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Boolean getIsShared() {
        return isShared;
    }

    public void setIsShared(Boolean isShared) {
        this.isShared = isShared;
    }
}
