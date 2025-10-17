package gov.nist.oscal.tools.api.model;

public class ConversionResult {
    private boolean success;
    private String content;
    private String error;
    private OscalFormat fromFormat;
    private OscalFormat toFormat;

    // Constructor for success
    public ConversionResult(boolean success, String content, OscalFormat fromFormat, OscalFormat toFormat) {
        this.success = success;
        this.content = content;
        this.fromFormat = fromFormat;
        this.toFormat = toFormat;
    }

    // Constructor for error
    public ConversionResult(boolean success, String error, OscalFormat fromFormat, OscalFormat toFormat, boolean isError) {
        this.success = success;
        this.error = error;
        this.fromFormat = fromFormat;
        this.toFormat = toFormat;
    }

    // Getters and Setters
    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
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
}
