package gov.nist.oscal.tools.api.model;

import java.time.LocalDateTime;

/**
 * Response from signature re-verification operation
 */
public class SignatureVerificationResponse {

    private boolean valid;
    private LocalDateTime verificationDate;
    private String notes;

    // Constructors
    public SignatureVerificationResponse() {
    }

    public SignatureVerificationResponse(boolean valid, LocalDateTime verificationDate, String notes) {
        this.valid = valid;
        this.verificationDate = verificationDate;
        this.notes = notes;
    }

    // Builder pattern
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private boolean valid;
        private LocalDateTime verificationDate;
        private String notes;

        public Builder valid(boolean valid) {
            this.valid = valid;
            return this;
        }

        public Builder verificationDate(LocalDateTime verificationDate) {
            this.verificationDate = verificationDate;
            return this;
        }

        public Builder notes(String notes) {
            this.notes = notes;
            return this;
        }

        public SignatureVerificationResponse build() {
            return new SignatureVerificationResponse(valid, verificationDate, notes);
        }
    }

    // Getters and Setters
    public boolean isValid() {
        return valid;
    }

    public void setValid(boolean valid) {
        this.valid = valid;
    }

    public LocalDateTime getVerificationDate() {
        return verificationDate;
    }

    public void setVerificationDate(LocalDateTime verificationDate) {
        this.verificationDate = verificationDate;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
