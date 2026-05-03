package gov.nist.oscal.tools.api.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Unified storage for OSCAL documents that don't yet have dedicated builders
 * (System Security Plan, Assessment Plan, Assessment Results, POA&amp;M).
 * Catalog, Profile, and Component Definition keep their own entities.
 */
@Entity
@Table(name = "oscal_documents")
public class OscalDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "oscal_uuid", nullable = false, unique = true)
    private String oscalUuid;

    @Enumerated(EnumType.STRING)
    @Column(name = "model_type", nullable = false, length = 40)
    private OscalModelType modelType;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 50)
    private String version;

    @Column(name = "oscal_version", length = 20)
    private String oscalVersion;

    @Column(name = "storage_path", nullable = false)
    private String storagePath;

    @Column(name = "filename", nullable = false)
    private String filename;

    @Column(name = "file_size")
    private Long fileSize;

    /** Free-form, model-specific count summary serialized as JSON (e.g. {"controls":42}). */
    @Column(name = "stats_json", columnDefinition = "TEXT")
    private String statsJson;

    @Column(name = "is_draft", nullable = false)
    private boolean draft = false;

    @ManyToOne
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @ManyToOne
    @JoinColumn(name = "last_updated_by")
    private User lastUpdatedBy;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public OscalDocument() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public OscalDocument(String oscalUuid, OscalModelType modelType, String title, String storagePath, User createdBy) {
        this();
        this.oscalUuid = oscalUuid;
        this.modelType = modelType;
        this.title = title;
        this.storagePath = storagePath;
        this.createdBy = createdBy;
        this.lastUpdatedBy = createdBy;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getOscalUuid() { return oscalUuid; }
    public void setOscalUuid(String oscalUuid) { this.oscalUuid = oscalUuid; }
    public OscalModelType getModelType() { return modelType; }
    public void setModelType(OscalModelType modelType) { this.modelType = modelType; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }
    public String getOscalVersion() { return oscalVersion; }
    public void setOscalVersion(String oscalVersion) { this.oscalVersion = oscalVersion; }
    public String getStoragePath() { return storagePath; }
    public void setStoragePath(String storagePath) { this.storagePath = storagePath; }
    public String getFilename() { return filename; }
    public void setFilename(String filename) { this.filename = filename; }
    public Long getFileSize() { return fileSize; }
    public void setFileSize(Long fileSize) { this.fileSize = fileSize; }
    public String getStatsJson() { return statsJson; }
    public void setStatsJson(String statsJson) { this.statsJson = statsJson; }
    public boolean isDraft() { return draft; }
    public void setDraft(boolean draft) { this.draft = draft; }
    public User getCreatedBy() { return createdBy; }
    public void setCreatedBy(User createdBy) { this.createdBy = createdBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public User getLastUpdatedBy() { return lastUpdatedBy; }
    public void setLastUpdatedBy(User lastUpdatedBy) { this.lastUpdatedBy = lastUpdatedBy; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
