package gov.nist.oscal.tools.api.model;

import gov.nist.oscal.tools.api.entity.AuthorizationDocument;
import gov.nist.oscal.tools.api.entity.DocumentType;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class AuthorizationDocumentResponse {

    private Long id;
    private Long authorizationId;
    private String originalFilename;
    private Long fileSize;
    private String contentType;
    private DocumentType documentType;
    private String description;
    private String tags;
    private String version;
    private LocalDate effectiveDate;
    private LocalDate expiresAt;
    private String uploadedByUsername;
    private LocalDateTime uploadedAt;

    public AuthorizationDocumentResponse() {}

    public AuthorizationDocumentResponse(AuthorizationDocument doc) {
        this.id = doc.getId();
        this.authorizationId = doc.getAuthorization().getId();
        this.originalFilename = doc.getOriginalFilename();
        this.fileSize = doc.getFileSize();
        this.contentType = doc.getContentType();
        this.documentType = doc.getDocumentType();
        this.description = doc.getDescription();
        this.tags = doc.getTags();
        this.version = doc.getVersion();
        this.effectiveDate = doc.getEffectiveDate();
        this.expiresAt = doc.getExpiresAt();
        this.uploadedByUsername = doc.getUploadedBy() != null ? doc.getUploadedBy().getUsername() : null;
        this.uploadedAt = doc.getUploadedAt();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getAuthorizationId() { return authorizationId; }
    public void setAuthorizationId(Long authorizationId) { this.authorizationId = authorizationId; }
    public String getOriginalFilename() { return originalFilename; }
    public void setOriginalFilename(String originalFilename) { this.originalFilename = originalFilename; }
    public Long getFileSize() { return fileSize; }
    public void setFileSize(Long fileSize) { this.fileSize = fileSize; }
    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }
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
    public String getUploadedByUsername() { return uploadedByUsername; }
    public void setUploadedByUsername(String uploadedByUsername) { this.uploadedByUsername = uploadedByUsername; }
    public LocalDateTime getUploadedAt() { return uploadedAt; }
    public void setUploadedAt(LocalDateTime uploadedAt) { this.uploadedAt = uploadedAt; }
}
