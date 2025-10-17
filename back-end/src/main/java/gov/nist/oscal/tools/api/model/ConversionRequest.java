package gov.nist.oscal.tools.api.model;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

public class ConversionRequest {
    @NotBlank(message = "Content is required")
    private String content;

    @NotNull(message = "From format is required")
    private OscalFormat fromFormat;

    @NotNull(message = "To format is required")
    private OscalFormat toFormat;

    @NotNull(message = "Model type is required")
    private OscalModelType modelType;

    private String fileName;

    // Getters and Setters
    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public OscalFormat getFromFormat() {
        return fromFormat;
    }

    public void setFromFormat(OscalFormat fromFormat) {
        this.fromFormat = fromFormat;
    }

    public OscalFormat getToFormat() {
        return toFormat;
    }

    public void setToFormat(OscalFormat toFormat) {
        this.toFormat = toFormat;
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
