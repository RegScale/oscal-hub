package gov.nist.oscal.tools.api.model;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.Set;

/**
 * Request DTO for creating a new library item
 */
public class LibraryItemRequest {

    @NotBlank(message = "Title is required")
    private String title;

    private String description;

    @NotBlank(message = "OSCAL type is required")
    private String oscalType;

    @NotBlank(message = "File name is required")
    private String fileName;

    @NotBlank(message = "Format is required")
    private String format;

    @NotNull(message = "File content is required")
    private String fileContent;

    private Set<String> tags;

    // Constructors
    public LibraryItemRequest() {
    }

    public LibraryItemRequest(String title, String description, String oscalType, String fileName,
                             String format, String fileContent, Set<String> tags) {
        this.title = title;
        this.description = description;
        this.oscalType = oscalType;
        this.fileName = fileName;
        this.format = format;
        this.fileContent = fileContent;
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

    public String getOscalType() {
        return oscalType;
    }

    public void setOscalType(String oscalType) {
        this.oscalType = oscalType;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public String getFileContent() {
        return fileContent;
    }

    public void setFileContent(String fileContent) {
        this.fileContent = fileContent;
    }

    public Set<String> getTags() {
        return tags;
    }

    public void setTags(Set<String> tags) {
        this.tags = tags;
    }
}
