package gov.nist.oscal.tools.api.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Request DTO for adding a new version to a library item
 */
public class LibraryVersionRequest {

    @NotBlank(message = "File name is required")
    private String fileName;

    @NotBlank(message = "Format is required")
    private String format;

    @NotNull(message = "File content is required")
    private String fileContent;

    private String changeDescription;

    // Constructors
    public LibraryVersionRequest() {
    }

    public LibraryVersionRequest(String fileName, String format, String fileContent, String changeDescription) {
        this.fileName = fileName;
        this.format = format;
        this.fileContent = fileContent;
        this.changeDescription = changeDescription;
    }

    // Getters and Setters
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

    public String getChangeDescription() {
        return changeDescription;
    }

    public void setChangeDescription(String changeDescription) {
        this.changeDescription = changeDescription;
    }
}
