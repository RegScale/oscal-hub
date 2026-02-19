package gov.nist.oscal.tools.api.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Represents a specific version of an artifact's Markdown content.
 * Tracks complete version history including metadata changes and extracted variables.
 */
@Entity
@Table(name = "artifact_versions")
public class ArtifactVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String versionId; // UUID for external reference

    @ManyToOne
    @JoinColumn(name = "artifact_id", nullable = false)
    private Artifact artifact;

    @Column(nullable = false)
    private Integer versionNumber; // Incremental version number

    @Column(nullable = false)
    private Long contentSize; // Size in bytes

    @Column(nullable = false, length = 500)
    private String filePath; // Path in storage (Azure Blob or local)

    @ManyToOne
    @JoinColumn(name = "uploaded_by", nullable = false)
    private User uploadedBy;

    @Column(name = "uploaded_at", nullable = false)
    private LocalDateTime uploadedAt;

    @Column(length = 1000)
    private String changeDescription; // Description of what changed in this version

    // Snapshot of metadata at time of version creation
    @Column(length = 255)
    private String titleSnapshot;

    @Column(length = 2000)
    private String descriptionSnapshot;

    @Column(length = 20)
    private String visibilitySnapshot;

    @Column(columnDefinition = "TEXT")
    private String extractedVariablesSnapshot; // JSON array of variables at time of version

    // Constructors
    public ArtifactVersion() {
        this.uploadedAt = LocalDateTime.now();
    }

    public ArtifactVersion(String versionId, Artifact artifact, Integer versionNumber,
                           Long contentSize, String filePath, User uploadedBy,
                           String changeDescription) {
        this();
        this.versionId = versionId;
        this.artifact = artifact;
        this.versionNumber = versionNumber;
        this.contentSize = contentSize;
        this.filePath = filePath;
        this.uploadedBy = uploadedBy;
        this.changeDescription = changeDescription;

        // Capture metadata snapshot
        if (artifact != null) {
            this.titleSnapshot = artifact.getTitle();
            this.descriptionSnapshot = artifact.getDescription();
            this.visibilitySnapshot = artifact.getVisibility() != null
                ? artifact.getVisibility().name() : null;
            this.extractedVariablesSnapshot = artifact.getExtractedVariables();
        }
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getVersionId() {
        return versionId;
    }

    public void setVersionId(String versionId) {
        this.versionId = versionId;
    }

    public Artifact getArtifact() {
        return artifact;
    }

    public void setArtifact(Artifact artifact) {
        this.artifact = artifact;
    }

    public Integer getVersionNumber() {
        return versionNumber;
    }

    public void setVersionNumber(Integer versionNumber) {
        this.versionNumber = versionNumber;
    }

    public Long getContentSize() {
        return contentSize;
    }

    public void setContentSize(Long contentSize) {
        this.contentSize = contentSize;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public User getUploadedBy() {
        return uploadedBy;
    }

    public void setUploadedBy(User uploadedBy) {
        this.uploadedBy = uploadedBy;
    }

    public LocalDateTime getUploadedAt() {
        return uploadedAt;
    }

    public void setUploadedAt(LocalDateTime uploadedAt) {
        this.uploadedAt = uploadedAt;
    }

    public String getChangeDescription() {
        return changeDescription;
    }

    public void setChangeDescription(String changeDescription) {
        this.changeDescription = changeDescription;
    }

    public String getTitleSnapshot() {
        return titleSnapshot;
    }

    public void setTitleSnapshot(String titleSnapshot) {
        this.titleSnapshot = titleSnapshot;
    }

    public String getDescriptionSnapshot() {
        return descriptionSnapshot;
    }

    public void setDescriptionSnapshot(String descriptionSnapshot) {
        this.descriptionSnapshot = descriptionSnapshot;
    }

    public String getVisibilitySnapshot() {
        return visibilitySnapshot;
    }

    public void setVisibilitySnapshot(String visibilitySnapshot) {
        this.visibilitySnapshot = visibilitySnapshot;
    }

    public String getExtractedVariablesSnapshot() {
        return extractedVariablesSnapshot;
    }

    public void setExtractedVariablesSnapshot(String extractedVariablesSnapshot) {
        this.extractedVariablesSnapshot = extractedVariablesSnapshot;
    }
}
