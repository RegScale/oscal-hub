package gov.nist.oscal.tools.api.model;

/**
 * Result of certificate validation
 */
public class CertificateValidationResult {

    private boolean valid;
    private String notes;

    // Constructors
    public CertificateValidationResult() {
    }

    public CertificateValidationResult(boolean valid, String notes) {
        this.valid = valid;
        this.notes = notes;
    }

    // Builder pattern
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private boolean valid;
        private String notes;

        public Builder valid(boolean valid) {
            this.valid = valid;
            return this;
        }

        public Builder notes(String notes) {
            this.notes = notes;
            return this;
        }

        public CertificateValidationResult build() {
            return new CertificateValidationResult(valid, notes);
        }
    }

    // Getters and Setters
    public boolean isValid() {
        return valid;
    }

    public void setValid(boolean valid) {
        this.valid = valid;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
