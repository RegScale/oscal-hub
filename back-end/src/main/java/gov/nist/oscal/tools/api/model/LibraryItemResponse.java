package gov.nist.oscal.tools.api.model;

import gov.nist.oscal.tools.api.entity.LibraryItem;
import gov.nist.oscal.tools.api.entity.LibraryTag;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Response DTO for library item
 */
public class LibraryItemResponse {

    private String itemId;
    private String title;
    private String description;
    private String oscalType;
    private String createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Set<String> tags;
    private LibraryVersionResponse currentVersion;
    private Long downloadCount;
    private Long viewCount;
    private Integer versionCount;

    // Constructors
    public LibraryItemResponse() {
    }

    public static LibraryItemResponse fromEntity(LibraryItem item) {
        LibraryItemResponse response = new LibraryItemResponse();
        response.setItemId(item.getItemId());
        response.setTitle(item.getTitle());
        response.setDescription(item.getDescription());
        response.setOscalType(item.getOscalType());
        response.setCreatedBy(item.getCreatedBy().getUsername());
        response.setCreatedAt(item.getCreatedAt());
        response.setUpdatedAt(item.getUpdatedAt());
        response.setTags(item.getTags().stream()
                .map(LibraryTag::getName)
                .collect(Collectors.toSet()));
        if (item.getCurrentVersion() != null) {
            response.setCurrentVersion(LibraryVersionResponse.fromEntity(item.getCurrentVersion()));
        }
        response.setDownloadCount(item.getDownloadCount());
        response.setViewCount(item.getViewCount());
        // Avoid LazyInitializationException - check if versions collection is initialized
        // before accessing size. If not initialized, use null (can be populated separately if needed)
        try {
            response.setVersionCount(item.getVersions() != null ? item.getVersions().size() : 0);
        } catch (org.hibernate.LazyInitializationException e) {
            response.setVersionCount(null); // Not available outside transaction
        }
        return response;
    }

    // Getters and Setters
    public String getItemId() {
        return itemId;
    }

    public void setItemId(String itemId) {
        this.itemId = itemId;
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

    public String getOscalType() {
        return oscalType;
    }

    public void setOscalType(String oscalType) {
        this.oscalType = oscalType;
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

    public Set<String> getTags() {
        return tags;
    }

    public void setTags(Set<String> tags) {
        this.tags = tags;
    }

    public LibraryVersionResponse getCurrentVersion() {
        return currentVersion;
    }

    public void setCurrentVersion(LibraryVersionResponse currentVersion) {
        this.currentVersion = currentVersion;
    }

    public Long getDownloadCount() {
        return downloadCount;
    }

    public void setDownloadCount(Long downloadCount) {
        this.downloadCount = downloadCount;
    }

    public Long getViewCount() {
        return viewCount;
    }

    public void setViewCount(Long viewCount) {
        this.viewCount = viewCount;
    }

    public Integer getVersionCount() {
        return versionCount;
    }

    public void setVersionCount(Integer versionCount) {
        this.versionCount = versionCount;
    }
}
