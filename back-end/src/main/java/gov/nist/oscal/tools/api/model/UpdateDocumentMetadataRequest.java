package gov.nist.oscal.tools.api.model;

import gov.nist.oscal.tools.api.entity.DocumentType;

import java.time.LocalDate;

public class UpdateDocumentMetadataRequest {

    private DocumentType documentType;
    private String description;
    private String tags;
    private String version;
    private LocalDate effectiveDate;
    private LocalDate expiresAt;

    public DocumentType getDocumentType() { return documentType; }
    public void setDocumentType(DocumentType documentType) { this.documentType = documentType; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getTags() { return tags; }
    public void setTags(String tags) { this.tags = tags; }
    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }
    public LocalDate getEffectiveDate() { return effectiveDate; }
    public void setEffectiveDate(LocalDate effectiveDate) { this.effectiveDate = effectiveDate; }
    public LocalDate getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDate expiresAt) { this.expiresAt = expiresAt; }
}
