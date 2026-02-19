package gov.nist.oscal.tools.api.model;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import gov.nist.oscal.tools.api.entity.Artifact;
import gov.nist.oscal.tools.api.entity.ArtifactTag;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Response DTO for artifact
 */
public class ArtifactResponse {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    private String artifactId;
    private String title;
    private String description;
    private String visibility;
    private Long organizationId;
    private String organizationName;
    private String createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Set<String> tags;
    private ArtifactVersionResponse currentVersion;
    private List<String> extractedVariables;
    private Long downloadCount;
    private Long viewCount;
    private Integer versionCount;

    // Rating and comment fields
    private Double averageRating;
    private Long totalRatings;
    private Long commentCount;

    // Constructors
    public ArtifactResponse() {
    }

    public static ArtifactResponse fromEntity(Artifact artifact) {
        ArtifactResponse response = new ArtifactResponse();
        response.setArtifactId(artifact.getArtifactId());
        response.setTitle(artifact.getTitle());
        response.setDescription(artifact.getDescription());
        response.setVisibility(artifact.getVisibility().name());

        // Handle lazy-loaded organization
        try {
            if (artifact.getOrganization() != null) {
                response.setOrganizationId(artifact.getOrganization().getId());
                response.setOrganizationName(artifact.getOrganization().getName());
            }
        } catch (org.hibernate.LazyInitializationException e) {
            // Organization not loaded, leave as null
        }

        // Handle lazy-loaded createdBy
        try {
            response.setCreatedBy(artifact.getCreatedBy().getUsername());
        } catch (org.hibernate.LazyInitializationException e) {
            response.setCreatedBy(null);
        }

        response.setCreatedAt(artifact.getCreatedAt());
        response.setUpdatedAt(artifact.getUpdatedAt());

        // Handle lazy-loaded tags
        try {
            response.setTags(artifact.getTags().stream()
                    .map(ArtifactTag::getName)
                    .collect(Collectors.toSet()));
        } catch (org.hibernate.LazyInitializationException e) {
            response.setTags(new java.util.HashSet<>());
        }

        if (artifact.getCurrentVersion() != null) {
            response.setCurrentVersion(ArtifactVersionResponse.fromEntity(artifact.getCurrentVersion()));
        }

        // Parse extracted variables from JSON
        response.setExtractedVariables(parseVariables(artifact.getExtractedVariables()));

        response.setDownloadCount(artifact.getDownloadCount());
        response.setViewCount(artifact.getViewCount());

        try {
            response.setVersionCount(artifact.getVersions() != null ? artifact.getVersions().size() : 0);
        } catch (org.hibernate.LazyInitializationException e) {
            response.setVersionCount(null);
        }
        return response;
    }

    private static List<String> parseVariables(String variablesJson) {
        if (variablesJson == null || variablesJson.isEmpty()) {
            return new ArrayList<>();
        }
        try {
            return objectMapper.readValue(variablesJson, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    // Getters and Setters
    public String getArtifactId() {
        return artifactId;
    }

    public void setArtifactId(String artifactId) {
        this.artifactId = artifactId;
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

    public String getVisibility() {
        return visibility;
    }

    public void setVisibility(String visibility) {
        this.visibility = visibility;
    }

    public Long getOrganizationId() {
        return organizationId;
    }

    public void setOrganizationId(Long organizationId) {
        this.organizationId = organizationId;
    }

    public String getOrganizationName() {
        return organizationName;
    }

    public void setOrganizationName(String organizationName) {
        this.organizationName = organizationName;
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

    public ArtifactVersionResponse getCurrentVersion() {
        return currentVersion;
    }

    public void setCurrentVersion(ArtifactVersionResponse currentVersion) {
        this.currentVersion = currentVersion;
    }

    public List<String> getExtractedVariables() {
        return extractedVariables;
    }

    public void setExtractedVariables(List<String> extractedVariables) {
        this.extractedVariables = extractedVariables;
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

    public Double getAverageRating() {
        return averageRating;
    }

    public void setAverageRating(Double averageRating) {
        this.averageRating = averageRating;
    }

    public Long getTotalRatings() {
        return totalRatings;
    }

    public void setTotalRatings(Long totalRatings) {
        this.totalRatings = totalRatings;
    }

    public Long getCommentCount() {
        return commentCount;
    }

    public void setCommentCount(Long commentCount) {
        this.commentCount = commentCount;
    }
}
