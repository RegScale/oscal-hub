package gov.nist.oscal.tools.api.model;

import java.time.LocalDateTime;

/**
 * Detailed information about an authorization's digital signature
 */
public class SignatureDetailsResponse {

    private boolean signed;
    private String signerName;
    private String signerEmail;
    private String signerEdipi;
    private LocalDateTime signatureTimestamp;
    private String certificateIssuer;
    private String certificateSerial;
    private LocalDateTime certificateNotBefore;
    private LocalDateTime certificateNotAfter;
    private Boolean certificateVerified;
    private LocalDateTime verificationDate;
    private String verificationNotes;
    private String message;

    // Constructors
    public SignatureDetailsResponse() {
    }

    public SignatureDetailsResponse(String message) {
        this.signed = false;
        this.message = message;
    }

    // Builder pattern
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private boolean signed;
        private String signerName;
        private String signerEmail;
        private String signerEdipi;
        private LocalDateTime signatureTimestamp;
        private String certificateIssuer;
        private String certificateSerial;
        private LocalDateTime certificateNotBefore;
        private LocalDateTime certificateNotAfter;
        private Boolean certificateVerified;
        private LocalDateTime verificationDate;
        private String verificationNotes;
        private String message;

        public Builder signed(boolean signed) {
            this.signed = signed;
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

        public Builder certificateIssuer(String certificateIssuer) {
            this.certificateIssuer = certificateIssuer;
            return this;
        }

        public Builder certificateSerial(String certificateSerial) {
            this.certificateSerial = certificateSerial;
            return this;
        }

        public Builder certificateNotBefore(LocalDateTime certificateNotBefore) {
            this.certificateNotBefore = certificateNotBefore;
            return this;
        }

        public Builder certificateNotAfter(LocalDateTime certificateNotAfter) {
            this.certificateNotAfter = certificateNotAfter;
            return this;
        }

        public Builder certificateVerified(Boolean certificateVerified) {
            this.certificateVerified = certificateVerified;
            return this;
        }

        public Builder verificationDate(LocalDateTime verificationDate) {
            this.verificationDate = verificationDate;
            return this;
        }

        public Builder verificationNotes(String verificationNotes) {
            this.verificationNotes = verificationNotes;
            return this;
        }

        public Builder message(String message) {
            this.message = message;
            return this;
        }

        public SignatureDetailsResponse build() {
            SignatureDetailsResponse response = new SignatureDetailsResponse();
            response.signed = this.signed;
            response.signerName = this.signerName;
            response.signerEmail = this.signerEmail;
            response.signerEdipi = this.signerEdipi;
            response.signatureTimestamp = this.signatureTimestamp;
            response.certificateIssuer = this.certificateIssuer;
            response.certificateSerial = this.certificateSerial;
            response.certificateNotBefore = this.certificateNotBefore;
            response.certificateNotAfter = this.certificateNotAfter;
            response.certificateVerified = this.certificateVerified;
            response.verificationDate = this.verificationDate;
            response.verificationNotes = this.verificationNotes;
            response.message = this.message;
            return response;
        }
    }

    // Getters and Setters
    public boolean isSigned() {
        return signed;
    }

    public void setSigned(boolean signed) {
        this.signed = signed;
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

    public String getCertificateIssuer() {
        return certificateIssuer;
    }

    public void setCertificateIssuer(String certificateIssuer) {
        this.certificateIssuer = certificateIssuer;
    }

    public String getCertificateSerial() {
        return certificateSerial;
    }

    public void setCertificateSerial(String certificateSerial) {
        this.certificateSerial = certificateSerial;
    }

    public LocalDateTime getCertificateNotBefore() {
        return certificateNotBefore;
    }

    public void setCertificateNotBefore(LocalDateTime certificateNotBefore) {
        this.certificateNotBefore = certificateNotBefore;
    }

    public LocalDateTime getCertificateNotAfter() {
        return certificateNotAfter;
    }

    public void setCertificateNotAfter(LocalDateTime certificateNotAfter) {
        this.certificateNotAfter = certificateNotAfter;
    }

    public Boolean getCertificateVerified() {
        return certificateVerified;
    }

    public void setCertificateVerified(Boolean certificateVerified) {
        this.certificateVerified = certificateVerified;
    }

    public LocalDateTime getVerificationDate() {
        return verificationDate;
    }

    public void setVerificationDate(LocalDateTime verificationDate) {
        this.verificationDate = verificationDate;
    }

    public String getVerificationNotes() {
        return verificationNotes;
    }

    public void setVerificationNotes(String verificationNotes) {
        this.verificationNotes = verificationNotes;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
