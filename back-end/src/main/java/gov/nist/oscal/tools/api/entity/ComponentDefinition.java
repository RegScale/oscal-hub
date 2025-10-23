package gov.nist.oscal.tools.api.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Represents an OSCAL Component Definition
 * Stores metadata about component definitions, actual JSON stored in Azure Blob Storage
 */
@Entity
@Table(name = "component_definitions")
public class ComponentDefinition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "oscal_uuid", nullable = false, unique = true)
    private String oscalUuid; // OSCAL document UUID

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 50)
    private String version;

    @Column(name = "oscal_version", length = 20)
    private String oscalVersion;

    @Column(name = "azure_blob_path", nullable = false)
    private String azureBlobPath; // Path in Azure: build/{username}/{filename}

    @Column(name = "filename", nullable = false)
    private String filename; // Original filename

    @Column(name = "file_size")
    private Long fileSize; // Size in bytes

    @Column(name = "component_count")
    private Integer componentCount; // Number of components in definition

    @Column(name = "capability_count")
    private Integer capabilityCount; // Number of capabilities in definition

    @Column(name = "control_count")
    private Integer controlCount; // Total number of implemented controls

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

    // Constructors
    public ComponentDefinition() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public ComponentDefinition(String oscalUuid, String title, String azureBlobPath, User createdBy) {
        this();
        this.oscalUuid = oscalUuid;
        this.title = title;
        this.azureBlobPath = azureBlobPath;
        this.createdBy = createdBy;
        this.lastUpdatedBy = createdBy;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getOscalUuid() {
        return oscalUuid;
    }

    public void setOscalUuid(String oscalUuid) {
        this.oscalUuid = oscalUuid;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getOscalVersion() {
        return oscalVersion;
    }

    public void setOscalVersion(String oscalVersion) {
        this.oscalVersion = oscalVersion;
    }

    public String getAzureBlobPath() {
        return azureBlobPath;
    }

    public void setAzureBlobPath(String azureBlobPath) {
        this.azureBlobPath = azureBlobPath;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public Integer getComponentCount() {
        return componentCount;
    }

    public void setComponentCount(Integer componentCount) {
        this.componentCount = componentCount;
    }

    public Integer getCapabilityCount() {
        return capabilityCount;
    }

    public void setCapabilityCount(Integer capabilityCount) {
        this.capabilityCount = capabilityCount;
    }

    public Integer getControlCount() {
        return controlCount;
    }

    public void setControlCount(Integer controlCount) {
        this.controlCount = controlCount;
    }

    public User getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(User createdBy) {
        this.createdBy = createdBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public User getLastUpdatedBy() {
        return lastUpdatedBy;
    }

    public void setLastUpdatedBy(User lastUpdatedBy) {
        this.lastUpdatedBy = lastUpdatedBy;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
