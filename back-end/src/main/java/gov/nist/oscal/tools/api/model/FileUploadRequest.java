package gov.nist.oscal.tools.api.model;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

public class FileUploadRequest {

    @NotBlank(message = "Content is required")
    private String content;

    @NotNull(message = "Format is required")
    private OscalFormat format;

    private OscalModelType modelType;

    private String fileName;

    // Constructors
    public FileUploadRequest() {}

    public FileUploadRequest(String content, OscalFormat format) {
        this.content = content;
        this.format = format;
    }

    public FileUploadRequest(String content, OscalFormat format, OscalModelType modelType, String fileName) {
        this.content = content;
        this.format = format;
        this.modelType = modelType;
        this.fileName = fileName;
    }

    // Getters and Setters
    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public OscalFormat getFormat() {
        return format;
    }

    public void setFormat(OscalFormat format) {
        this.format = format;
    }

    public OscalModelType getModelType() {
        return modelType;
    }

    public void setModelType(OscalModelType modelType) {
        this.modelType = modelType;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }
}
