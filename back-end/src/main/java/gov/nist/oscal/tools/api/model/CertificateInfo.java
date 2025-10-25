package gov.nist.oscal.tools.api.model;

import java.util.Date;

/**
 * Parsed information from X.509 certificate
 */
public class CertificateInfo {

    private String commonName;
    private String email;
    private String edipi;
    private String subjectDN;
    private String issuerDN;
    private String serialNumber;
    private Date notBefore;
    private Date notAfter;

    // Constructors
    public CertificateInfo() {
    }

    // Builder pattern
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String commonName;
        private String email;
        private String edipi;
        private String subjectDN;
        private String issuerDN;
        private String serialNumber;
        private Date notBefore;
        private Date notAfter;

        public Builder commonName(String commonName) {
            this.commonName = commonName;
            return this;
        }

        public Builder email(String email) {
            this.email = email;
            return this;
        }

        public Builder edipi(String edipi) {
            this.edipi = edipi;
            return this;
        }

        public Builder subjectDN(String subjectDN) {
            this.subjectDN = subjectDN;
            return this;
        }

        public Builder issuerDN(String issuerDN) {
            this.issuerDN = issuerDN;
            return this;
        }

        public Builder serialNumber(String serialNumber) {
            this.serialNumber = serialNumber;
            return this;
        }

        public Builder notBefore(Date notBefore) {
            this.notBefore = notBefore;
            return this;
        }

        public Builder notAfter(Date notAfter) {
            this.notAfter = notAfter;
            return this;
        }

        public CertificateInfo build() {
            CertificateInfo info = new CertificateInfo();
            info.commonName = this.commonName;
            info.email = this.email;
            info.edipi = this.edipi;
            info.subjectDN = this.subjectDN;
            info.issuerDN = this.issuerDN;
            info.serialNumber = this.serialNumber;
            info.notBefore = this.notBefore;
            info.notAfter = this.notAfter;
            return info;
        }
    }

    // Getters and Setters
    public String getCommonName() {
        return commonName;
    }

    public void setCommonName(String commonName) {
        this.commonName = commonName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getEdipi() {
        return edipi;
    }

    public void setEdipi(String edipi) {
        this.edipi = edipi;
    }

    public String getSubjectDN() {
        return subjectDN;
    }

    public void setSubjectDN(String subjectDN) {
        this.subjectDN = subjectDN;
    }

    public String getIssuerDN() {
        return issuerDN;
    }

    public void setIssuerDN(String issuerDN) {
        this.issuerDN = issuerDN;
    }

    public String getSerialNumber() {
        return serialNumber;
    }

    public void setSerialNumber(String serialNumber) {
        this.serialNumber = serialNumber;
    }

    public Date getNotBefore() {
        return notBefore;
    }

    public void setNotBefore(Date notBefore) {
        this.notBefore = notBefore;
    }

    public Date getNotAfter() {
        return notAfter;
    }

    public void setNotAfter(Date notAfter) {
        this.notAfter = notAfter;
    }
}
