package gov.nist.oscal.tools.api.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * Represents a Markdown artifact template with variable support.
 * Supports three-tier visibility: PRIVATE, ORGANIZATION, PUBLIC.
 */
@Entity
@Table(name = "artifacts")
public class Artifact {

    public enum ArtifactVisibility {
        PRIVATE,      // Only owner can see
        ORGANIZATION, // All org members can see
        PUBLIC        // All authenticated users can see
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String artifactId; // UUID for external reference

    @Column(nullable = false, length = 255)
    private String title;

    @Column(length = 2000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ArtifactVisibility visibility = ArtifactVisibility.PRIVATE;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id")
    private Organization organization; // Required when visibility = ORGANIZATION

    @ManyToOne
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToOne
    @JoinColumn(name = "current_version_id")
    private ArtifactVersion currentVersion;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "artifact_tag_mapping",
        joinColumns = @JoinColumn(name = "artifact_id"),
        inverseJoinColumns = @JoinColumn(name = "tag_id")
    )
    private Set<ArtifactTag> tags = new HashSet<>();

    @OneToMany(mappedBy = "artifact", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private Set<ArtifactVersion> versions = new HashSet<>();

    @Column(columnDefinition = "TEXT")
    private String extractedVariables; // JSON array of variable names extracted from content

    @Column(nullable = false)
    private Long downloadCount = 0L;

    @Column(nullable = false)
    private Long viewCount = 0L;

    // Constructors
    public Artifact() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public Artifact(String artifactId, String title, String description,
                    ArtifactVisibility visibility, User createdBy) {
        this();
        this.artifactId = artifactId;
        this.title = title;
        this.description = description;
        this.visibility = visibility;
        this.createdBy = createdBy;
    }

    public Artifact(String artifactId, String title, String description,
                    ArtifactVisibility visibility, Organization organization, User createdBy) {
        this(artifactId, title, description, visibility, createdBy);
        this.organization = organization;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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
        this.updatedAt = LocalDateTime.now();
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
        this.updatedAt = LocalDateTime.now();
    }

    public ArtifactVisibility getVisibility() {
        return visibility;
    }

    public void setVisibility(ArtifactVisibility visibility) {
        this.visibility = visibility;
        this.updatedAt = LocalDateTime.now();
    }

    public Organization getOrganization() {
        return organization;
    }

    public void setOrganization(Organization organization) {
        this.organization = organization;
        this.updatedAt = LocalDateTime.now();
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

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public ArtifactVersion getCurrentVersion() {
        return currentVersion;
    }

    public void setCurrentVersion(ArtifactVersion currentVersion) {
        this.currentVersion = currentVersion;
        this.updatedAt = LocalDateTime.now();
    }

    public Set<ArtifactTag> getTags() {
        return tags;
    }

    public void setTags(Set<ArtifactTag> tags) {
        this.tags = tags;
    }

    public void addTag(ArtifactTag tag) {
        this.tags.add(tag);
        this.updatedAt = LocalDateTime.now();
    }

    public void removeTag(ArtifactTag tag) {
        this.tags.remove(tag);
        this.updatedAt = LocalDateTime.now();
    }

    public Set<ArtifactVersion> getVersions() {
        return versions;
    }

    public void setVersions(Set<ArtifactVersion> versions) {
        this.versions = versions;
    }

    public String getExtractedVariables() {
        return extractedVariables;
    }

    public void setExtractedVariables(String extractedVariables) {
        this.extractedVariables = extractedVariables;
    }

    public Long getDownloadCount() {
        return downloadCount;
    }

    public void setDownloadCount(Long downloadCount) {
        this.downloadCount = downloadCount;
    }

    public void incrementDownloadCount() {
        this.downloadCount++;
    }

    public Long getViewCount() {
        return viewCount;
    }

    public void setViewCount(Long viewCount) {
        this.viewCount = viewCount;
    }

    public void incrementViewCount() {
        this.viewCount++;
    }
}
