package gov.nist.oscal.tools.api.model;

import java.time.LocalDateTime;

/**
 * Result of signing operation
 */
public class SignatureResult {

    private boolean success;
    private String signerName;
    private String signerEmail;
    private String signerEdipi;
    private LocalDateTime signatureTimestamp;
    private String message;

    // Constructors
    public SignatureResult() {
    }

    public SignatureResult(boolean success, String message) {
        this.success = success;
        this.message = message;
    }

    // Builder pattern for convenience
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private boolean success;
        private String signerName;
        private String signerEmail;
        private String signerEdipi;
        private LocalDateTime signatureTimestamp;
        private String message;

        public Builder success(boolean success) {
            this.success = success;
            return this;
        }

        public Builder signerName(String signerName) {
            this.signerName = signerName;
            return this;
        }

        public Builder signerEmail(String signerEmail) {
            this.signerEmail = signerEmail;
            return this;
        }

        public Builder signerEdipi(String signerEdipi) {
            this.signerEdipi = signerEdipi;
            return this;
        }

        public Builder signatureTimestamp(LocalDateTime signatureTimestamp) {
            this.signatureTimestamp = signatureTimestamp;
            return this;
        }

        public Builder message(String message) {
            this.message = message;
            return this;
        }

        public SignatureResult build() {
            SignatureResult result = new SignatureResult();
            result.success = this.success;
            result.signerName = this.signerName;
            result.signerEmail = this.signerEmail;
            result.signerEdipi = this.signerEdipi;
            result.signatureTimestamp = this.signatureTimestamp;
            result.message = this.message;
            return result;
        }
    }

    // Getters and Setters
    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getSignerName() {
        return signerName;
    }

    public void setSignerName(String signerName) {
        this.signerName = signerName;
    }

    public String getSignerEmail() {
        return signerEmail;
    }

    public void setSignerEmail(String signerEmail) {
        this.signerEmail = signerEmail;
    }

    public String getSignerEdipi() {
        return signerEdipi;
    }

    public void setSignerEdipi(String signerEdipi) {
        this.signerEdipi = signerEdipi;
    }

    public LocalDateTime getSignatureTimestamp() {
        return signatureTimestamp;
    }

    public void setSignatureTimestamp(LocalDateTime signatureTimestamp) {
        this.signatureTimestamp = signatureTimestamp;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
