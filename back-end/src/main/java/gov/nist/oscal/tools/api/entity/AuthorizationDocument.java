package gov.nist.oscal.tools.api.entity;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "authorization_documents",
       indexes = {
           @Index(name = "idx_authorization_documents_auth",
                  columnList = "authorization_id, uploadedAt DESC"),
           @Index(name = "idx_authorization_documents_type",
                  columnList = "authorization_id, documentType")
       })
public class AuthorizationDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "authorization_id", nullable = false)
    private Authorization authorization;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "uploaded_by", nullable = false)
    private User uploadedBy;

    @Column(name = "uploaded_at", nullable = false)
    private LocalDateTime uploadedAt = LocalDateTime.now();

    @Column(name = "original_filename", nullable = false, length = 512)
    private String originalFilename;

    @Column(name = "file_size", nullable = false)
    private Long fileSize;

    @Column(name = "content_type", nullable = false, length = 128)
    private String contentType;

    @Column(name = "storage_path", nullable = false, length = 1024)
    private String storagePath;

    @Enumerated(EnumType.STRING)
    @Column(name = "document_type", nullable = false, length = 64)
    private DocumentType documentType;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 512)
    private String tags;

    @Column(length = 64)
    private String version;

    @Column(name = "effective_date")
    private LocalDate effectiveDate;

    @Column(name = "expires_at")
    private LocalDate expiresAt;

    public AuthorizationDocument() {
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Authorization getAuthorization() { return authorization; }
    public void setAuthorization(Authorization authorization) { this.authorization = authorization; }
    public User getUploadedBy() { return uploadedBy; }
    public void setUploadedBy(User uploadedBy) { this.uploadedBy = uploadedBy; }
    public LocalDateTime getUploadedAt() { return uploadedAt; }
    public void setUploadedAt(LocalDateTime uploadedAt) { this.uploadedAt = uploadedAt; }
    public String getOriginalFilename() { return originalFilename; }
    public void setOriginalFilename(String originalFilename) { this.originalFilename = originalFilename; }
    public Long getFileSize() { return fileSize; }
    public void setFileSize(Long fileSize) { this.fileSize = fileSize; }
    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }
    public String getStoragePath() { return storagePath; }
    public void setStoragePath(String storagePath) { this.storagePath = storagePath; }
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
