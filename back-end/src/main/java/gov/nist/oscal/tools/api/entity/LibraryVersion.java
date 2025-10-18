package gov.nist.oscal.tools.api.entity;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * Represents a specific version of a library item's file
 * Tracks complete version history including metadata changes
 */
@Entity
@Table(name = "library_versions")
public class LibraryVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String versionId; // UUID for external reference

    @ManyToOne
    @JoinColumn(name = "library_item_id", nullable = false)
    private LibraryItem libraryItem;

    @Column(nullable = false)
    private Integer versionNumber; // Incremental version number

    @Column(nullable = false, length = 255)
    private String fileName;

    @Column(nullable = false, length = 20)
    private String format; // JSON, XML, YAML

    @Column(nullable = false)
    private Long fileSize;

    @Column(nullable = false, length = 500)
    private String filePath; // Path in Azure Blob Storage

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

    @Column(length = 50)
    private String oscalTypeSnapshot;

    // Constructors
    public LibraryVersion() {
        this.uploadedAt = LocalDateTime.now();
    }

    public LibraryVersion(String versionId, LibraryItem libraryItem, Integer versionNumber,
                          String fileName, String format, Long fileSize, String filePath,
                          User uploadedBy, String changeDescription) {
        this();
        this.versionId = versionId;
        this.libraryItem = libraryItem;
        this.versionNumber = versionNumber;
        this.fileName = fileName;
        this.format = format;
        this.fileSize = fileSize;
        this.filePath = filePath;
        this.uploadedBy = uploadedBy;
        this.changeDescription = changeDescription;

        // Capture metadata snapshot
        if (libraryItem != null) {
            this.titleSnapshot = libraryItem.getTitle();
            this.descriptionSnapshot = libraryItem.getDescription();
            this.oscalTypeSnapshot = libraryItem.getOscalType();
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

    public LibraryItem getLibraryItem() {
        return libraryItem;
    }

    public void setLibraryItem(LibraryItem libraryItem) {
        this.libraryItem = libraryItem;
    }

    public Integer getVersionNumber() {
        return versionNumber;
    }

    public void setVersionNumber(Integer versionNumber) {
        this.versionNumber = versionNumber;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
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

    public String getOscalTypeSnapshot() {
        return oscalTypeSnapshot;
    }

    public void setOscalTypeSnapshot(String oscalTypeSnapshot) {
        this.oscalTypeSnapshot = oscalTypeSnapshot;
    }
}
