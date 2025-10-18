package gov.nist.oscal.tools.api.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class SspVisualizationRequest {

    @NotBlank(message = "Content is required")
    private String content;

    @NotNull(message = "Format is required")
    private OscalFormat format;

    private String fileName;

    // Constructors
    public SspVisualizationRequest() {}

    public SspVisualizationRequest(String content, OscalFormat format) {
        this.content = content;
        this.format = format;
    }

    public SspVisualizationRequest(String content, OscalFormat format, String fileName) {
        this.content = content;
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
