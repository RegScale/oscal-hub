package gov.nist.oscal.tools.api.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class ValidationRequest {

    @NotBlank(message = "Content is required")
    private String content;

    @NotNull(message = "Model type is required")
    private OscalModelType modelType;

    @NotNull(message = "Format is required")
    private OscalFormat format;

    private String fileName;

    // Constructors
    public ValidationRequest() {}

    public ValidationRequest(String content, OscalModelType modelType, OscalFormat format) {
        this.content = content;
        this.modelType = modelType;
        this.format = format;
    }

    public ValidationRequest(String content, OscalModelType modelType, OscalFormat format, String fileName) {
        this.content = content;
        this.modelType = modelType;
        this.format = format;
        this.fileName = fileName;
    }

    // Getters and Setters
    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public OscalModelType getModelType() {
        return modelType;
    }

    public void setModelType(OscalModelType modelType) {
        this.modelType = modelType;
    }

    public OscalFormat getFormat() {
        return format;
    }

    public void setFormat(OscalFormat format) {
        this.format = format;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }
}
