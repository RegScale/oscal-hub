package gov.nist.oscal.tools.api.model;

import java.time.LocalDateTime;

public class SavedFile {
    private String id;
    private String fileName;
    private OscalModelType modelType;
    private OscalFormat format;
    private long fileSize;
    private LocalDateTime uploadedAt;
    private String filePath;
    private String username;

    // Constructors
    public SavedFile() {}

    public SavedFile(String id, String fileName, OscalModelType modelType, OscalFormat format, long fileSize, LocalDateTime uploadedAt, String filePath) {
        this.id = id;
        this.fileName = fileName;
        this.modelType = modelType;
        this.format = format;
        this.fileSize = fileSize;
        this.uploadedAt = uploadedAt;
        this.filePath = filePath;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public OscalModelType getModelType() {
        return modelType;
    }

    public void setModelType(OscalModelType modelType) {
        this.modelType = modelType;
    }

    public OscalFormat getFormat() {
        return format;
    }

    public void setFormat(OscalFormat format) {
        this.format = format;
    }

    public long getFileSize() {
        return fileSize;
    }

    public void setFileSize(long fileSize) {
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

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
}
