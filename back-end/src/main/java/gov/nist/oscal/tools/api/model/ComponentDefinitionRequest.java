package gov.nist.oscal.tools.api.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Request DTO for creating or updating a component definition
 */
public class ComponentDefinitionRequest {

    @NotBlank(message = "Title is required")
    private String title;

    private String description;

    private String version;

    @NotBlank(message = "OSCAL version is required")
    private String oscalVersion;

    @NotBlank(message = "Filename is required")
    private String filename;

    @NotNull(message = "JSON content is required")
    private String jsonContent;

    private String oscalUuid; // Optional for create, extracted from JSON if not provided

    private Integer componentCount;

    private Integer capabilityCount;

    private Integer controlCount;

    // Constructors
    public ComponentDefinitionRequest() {
    }

    public ComponentDefinitionRequest(String title, String description, String version, String oscalVersion,
                                     String filename, String jsonContent) {
        this.title = title;
        this.description = description;
        this.version = version;
        this.oscalVersion = oscalVersion;
        this.filename = filename;
        this.jsonContent = jsonContent;
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

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getOscalVersion() {
        return oscalVersion;
    }

    public void setOscalVersion(String oscalVersion) {
        this.oscalVersion = oscalVersion;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String getJsonContent() {
        return jsonContent;
    }

    public void setJsonContent(String jsonContent) {
        this.jsonContent = jsonContent;
    }

    public String getOscalUuid() {
        return oscalUuid;
    }

    public void setOscalUuid(String oscalUuid) {
        this.oscalUuid = oscalUuid;
    }

    public Integer getComponentCount() {
        return componentCount;
    }

    public void setComponentCount(Integer componentCount) {
        this.componentCount = componentCount;
    }

    public Integer getCapabilityCount() {
        return capabilityCount;
    }

    public void setCapabilityCount(Integer capabilityCount) {
        this.capabilityCount = capabilityCount;
    }

    public Integer getControlCount() {
        return controlCount;
    }

    public void setControlCount(Integer controlCount) {
        this.controlCount = controlCount;
    }
}
