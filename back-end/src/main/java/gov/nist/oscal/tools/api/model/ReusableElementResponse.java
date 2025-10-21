package gov.nist.oscal.tools.api.model;

import gov.nist.oscal.tools.api.entity.ReusableElement;

import java.time.LocalDateTime;

/**
 * Response DTO for reusable element
 */
public class ReusableElementResponse {

    private Long id;
    private String type;
    private String name;
    private String jsonContent;
    private String description;
    private String createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private boolean isShared;
    private Integer useCount;

    // Constructors
    public ReusableElementResponse() {
    }

    /**
     * Convert entity to response DTO
     */
    public static ReusableElementResponse fromEntity(ReusableElement entity) {
        ReusableElementResponse response = new ReusableElementResponse();
        response.setId(entity.getId());
        response.setType(entity.getType().name());
        response.setName(entity.getName());
        response.setJsonContent(entity.getJsonContent());
        response.setDescription(entity.getDescription());
        response.setCreatedBy(entity.getCreatedBy().getUsername());
        response.setCreatedAt(entity.getCreatedAt());
        response.setUpdatedAt(entity.getUpdatedAt());
        response.setShared(entity.isShared());
        response.setUseCount(entity.getUseCount());
        return response;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getJsonContent() {
        return jsonContent;
    }

    public void setJsonContent(String jsonContent) {
        this.jsonContent = jsonContent;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public boolean isShared() {
        return isShared;
    }

    public void setShared(boolean shared) {
        isShared = shared;
    }

    public Integer getUseCount() {
        return useCount;
    }

    public void setUseCount(Integer useCount) {
        this.useCount = useCount;
    }
}
