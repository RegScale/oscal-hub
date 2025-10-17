package gov.nist.oscal.tools.api.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class ValidationResult {
    private boolean valid;
    private List<ValidationError> errors = new ArrayList<>();
    private List<ValidationError> warnings = new ArrayList<>();
    private OscalModelType modelType;
    private OscalFormat format;
    private String timestamp;

    // Constructors
    public ValidationResult() {
        this.timestamp = Instant.now().toString();
    }

    public ValidationResult(boolean valid) {
        this();
        this.valid = valid;
    }

    // Getters and Setters
    public boolean isValid() {
        return valid;
    }

    public void setValid(boolean valid) {
        this.valid = valid;
    }

    public List<ValidationError> getErrors() {
        return errors;
    }

    public void setErrors(List<ValidationError> errors) {
        this.errors = errors;
    }

    public List<ValidationError> getWarnings() {
        return warnings;
    }

    public void setWarnings(List<ValidationError> warnings) {
        this.warnings = warnings;
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

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    // Helper methods
    public void addError(ValidationError error) {
        this.errors.add(error);
    }

    public void addWarning(ValidationError warning) {
        this.warnings.add(warning);
    }
}
