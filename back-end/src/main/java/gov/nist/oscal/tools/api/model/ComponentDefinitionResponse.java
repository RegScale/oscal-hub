package gov.nist.oscal.tools.api.model;

import gov.nist.oscal.tools.api.entity.ComponentDefinition;

import java.time.LocalDateTime;

/**
 * Response DTO for component definition
 */
public class ComponentDefinitionResponse {

    private Long id;
    private String oscalUuid;
    private String title;
    private String description;
    private String version;
    private String oscalVersion;
    private String storagePath;
    private String filename;
    private Long fileSize;
    private Integer componentCount;
    private Integer capabilityCount;
    private Integer controlCount;
    private String createdBy;
    private LocalDateTime createdAt;
    private String lastUpdatedBy;
    private LocalDateTime updatedAt;

    // Constructors
    public ComponentDefinitionResponse() {
    }

    /**
     * Convert entity to response DTO
     */
    public static ComponentDefinitionResponse fromEntity(ComponentDefinition entity) {
        ComponentDefinitionResponse response = new ComponentDefinitionResponse();
        response.setId(entity.getId());
        response.setOscalUuid(entity.getOscalUuid());
        response.setTitle(entity.getTitle());
        response.setDescription(entity.getDescription());
        response.setVersion(entity.getVersion());
        response.setOscalVersion(entity.getOscalVersion());
        response.setStoragePath(entity.getStoragePath());
        response.setFilename(entity.getFilename());
        response.setFileSize(entity.getFileSize());
        response.setComponentCount(entity.getComponentCount());
        response.setCapabilityCount(entity.getCapabilityCount());
        response.setControlCount(entity.getControlCount());
        response.setCreatedBy(entity.getCreatedBy().getUsername());
        response.setCreatedAt(entity.getCreatedAt());
        if (entity.getLastUpdatedBy() != null) {
            response.setLastUpdatedBy(entity.getLastUpdatedBy().getUsername());
        }
        response.setUpdatedAt(entity.getUpdatedAt());
        return response;
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

    public String getStoragePath() {
        return storagePath;
    }

    public void setStoragePath(String storagePath) {
        this.storagePath = storagePath;
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

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getLastUpdatedBy() {
        return lastUpdatedBy;
    }

    public void setLastUpdatedBy(String lastUpdatedBy) {
        this.lastUpdatedBy = lastUpdatedBy;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
