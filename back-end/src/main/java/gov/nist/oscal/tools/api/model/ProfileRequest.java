package gov.nist.oscal.tools.api.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class ProfileRequest {

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

    private String oscalUuid;

    private Integer importCount;

    private Integer controlCount;

    private Integer alterCount;

    private Boolean draft;

    public ProfileRequest() {}

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }
    public String getOscalVersion() { return oscalVersion; }
    public void setOscalVersion(String oscalVersion) { this.oscalVersion = oscalVersion; }
    public String getFilename() { return filename; }
    public void setFilename(String filename) { this.filename = filename; }
    public String getJsonContent() { return jsonContent; }
    public void setJsonContent(String jsonContent) { this.jsonContent = jsonContent; }
    public String getOscalUuid() { return oscalUuid; }
    public void setOscalUuid(String oscalUuid) { this.oscalUuid = oscalUuid; }
    public Integer getImportCount() { return importCount; }
    public void setImportCount(Integer importCount) { this.importCount = importCount; }
    public Integer getControlCount() { return controlCount; }
    public void setControlCount(Integer controlCount) { this.controlCount = controlCount; }
    public Integer getAlterCount() { return alterCount; }
    public void setAlterCount(Integer alterCount) { this.alterCount = alterCount; }
    public Boolean getDraft() { return draft; }
    public void setDraft(Boolean draft) { this.draft = draft; }
}
