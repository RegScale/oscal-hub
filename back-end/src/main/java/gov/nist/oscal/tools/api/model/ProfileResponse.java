package gov.nist.oscal.tools.api.model;

import gov.nist.oscal.tools.api.entity.Profile;

import java.time.LocalDateTime;

public class ProfileResponse {

    private Long id;
    private String oscalUuid;
    private String title;
    private String description;
    private String version;
    private String oscalVersion;
    private String storagePath;
    private String filename;
    private Long fileSize;
    private Integer importCount;
    private Integer controlCount;
    private Integer alterCount;
    private boolean draft;
    private String createdBy;
    private LocalDateTime createdAt;
    private String lastUpdatedBy;
    private LocalDateTime updatedAt;

    public ProfileResponse() {}

    public static ProfileResponse fromEntity(Profile entity) {
        ProfileResponse r = new ProfileResponse();
        r.setId(entity.getId());
        r.setOscalUuid(entity.getOscalUuid());
        r.setTitle(entity.getTitle());
        r.setDescription(entity.getDescription());
        r.setVersion(entity.getVersion());
        r.setOscalVersion(entity.getOscalVersion());
        r.setStoragePath(entity.getStoragePath());
        r.setFilename(entity.getFilename());
        r.setFileSize(entity.getFileSize());
        r.setImportCount(entity.getImportCount());
        r.setControlCount(entity.getControlCount());
        r.setAlterCount(entity.getAlterCount());
        r.setDraft(entity.isDraft());

        try {
            r.setCreatedBy(entity.getCreatedBy().getUsername());
        } catch (org.hibernate.LazyInitializationException e) {
            r.setCreatedBy(null);
        }
        r.setCreatedAt(entity.getCreatedAt());

        try {
            if (entity.getLastUpdatedBy() != null) {
                r.setLastUpdatedBy(entity.getLastUpdatedBy().getUsername());
            }
        } catch (org.hibernate.LazyInitializationException e) {
            r.setLastUpdatedBy(null);
        }
        r.setUpdatedAt(entity.getUpdatedAt());
        return r;
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
    public Integer getImportCount() { return importCount; }
    public void setImportCount(Integer importCount) { this.importCount = importCount; }
    public Integer getControlCount() { return controlCount; }
    public void setControlCount(Integer controlCount) { this.controlCount = controlCount; }
    public Integer getAlterCount() { return alterCount; }
    public void setAlterCount(Integer alterCount) { this.alterCount = alterCount; }
    public boolean isDraft() { return draft; }
    public void setDraft(boolean draft) { this.draft = draft; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public String getLastUpdatedBy() { return lastUpdatedBy; }
    public void setLastUpdatedBy(String lastUpdatedBy) { this.lastUpdatedBy = lastUpdatedBy; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
