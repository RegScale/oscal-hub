package gov.nist.oscal.tools.api.model;

public class ValidationError {
    private Integer line;
    private Integer column;
    private String message;
    private String severity;
    private String path;

    // Constructors
    public ValidationError() {}

    public ValidationError(String message, String severity) {
        this.message = message;
        this.severity = severity;
    }

    public ValidationError(Integer line, Integer column, String message, String severity, String path) {
        this.line = line;
        this.column = column;
        this.message = message;
        this.severity = severity;
        this.path = path;
    }

    // Getters and Setters
    public Integer getLine() {
        return line;
    }

    public void setLine(Integer line) {
        this.line = line;
    }

    public Integer getColumn() {
        return column;
    }

    public void setColumn(Integer column) {
        this.column = column;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }
}
