package gov.nist.oscal.tools.api.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "catalogs")
public class Catalog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "oscal_uuid", nullable = false, unique = true)
    private String oscalUuid;

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

    @Column(name = "group_count")
    private Integer groupCount;

    @Column(name = "control_count")
    private Integer controlCount;

    @Column(name = "param_count")
    private Integer paramCount;

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

    public Catalog() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public Catalog(String oscalUuid, String title, String storagePath, User createdBy) {
        this();
        this.oscalUuid = oscalUuid;
        this.title = title;
        this.storagePath = storagePath;
        this.createdBy = createdBy;
        this.lastUpdatedBy = createdBy;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getOscalUuid() { return oscalUuid; }
    public void setOscalUuid(String oscalUuid) { this.oscalUuid = oscalUuid; }
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
    public Integer getGroupCount() { return groupCount; }
    public void setGroupCount(Integer groupCount) { this.groupCount = groupCount; }
    public Integer getControlCount() { return controlCount; }
    public void setControlCount(Integer controlCount) { this.controlCount = controlCount; }
    public Integer getParamCount() { return paramCount; }
    public void setParamCount(Integer paramCount) { this.paramCount = paramCount; }
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
