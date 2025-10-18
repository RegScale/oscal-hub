package gov.nist.oscal.tools.api.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "saved_files")
public class SavedFileEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String fileId; // UUID

    @Column(nullable = false, length = 255)
    private String fileName;

    @Column(nullable = false, length = 50)
    private String modelType; // catalog, profile, etc.

    @Column(nullable = false, length = 20)
    private String format; // JSON, XML, YAML

    @Column(nullable = false)
    private Long fileSize;

    @Column(nullable = false)
    private LocalDateTime uploadedAt;

    @Column(nullable = false, length = 500)
    private String filePath; // Path to stored file

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user; // User who uploaded the file

    // Constructors
    public SavedFileEntity() {
        this.uploadedAt = LocalDateTime.now();
    }

    public SavedFileEntity(String fileId, String fileName, String modelType, String format,
                           Long fileSize, String filePath, User user) {
        this();
        this.fileId = fileId;
        this.fileName = fileName;
        this.modelType = modelType;
        this.format = format;
        this.fileSize = fileSize;
        this.filePath = filePath;
        this.user = user;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFileId() {
        return fileId;
    }

    public void setFileId(String fileId) {
        this.fileId = fileId;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getModelType() {
        return modelType;
    }

    public void setModelType(String modelType) {
        this.modelType = modelType;
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

    public LocalDateTime getUploadedAt() {
        return uploadedAt;
    }

    public void setUploadedAt(LocalDateTime uploadedAt) {
        this.uploadedAt = uploadedAt;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }
}
