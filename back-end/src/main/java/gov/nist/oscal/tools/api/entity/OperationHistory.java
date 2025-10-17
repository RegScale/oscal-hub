package gov.nist.oscal.tools.api.entity;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "operation_history")
public class OperationHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String operationType; // VALIDATE, CONVERT, RESOLVE, BATCH_VALIDATE, BATCH_CONVERT

    @Column(nullable = false, length = 255)
    private String fileName;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @Column(nullable = false)
    private Boolean success;

    @Column(length = 1000)
    private String details; // Summary of operation result

    @Column(name = "duration_ms")
    private Long durationMs;

    @Column(length = 50)
    private String modelType; // catalog, profile, etc.

    @Column(length = 20)
    private String format; // JSON, XML, YAML

    @Column(name = "file_count")
    private Integer fileCount; // For batch operations

    @Column(name = "batch_operation_id", length = 100)
    private String batchOperationId; // Links multiple files in a batch

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user; // User who performed the operation

    // Constructors
    public OperationHistory() {
    }

    public OperationHistory(String operationType, String fileName, Boolean success, String details) {
        this.operationType = operationType;
        this.fileName = fileName;
        this.timestamp = LocalDateTime.now();
        this.success = success;
        this.details = details;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getOperationType() {
        return operationType;
    }

    public void setOperationType(String operationType) {
        this.operationType = operationType;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public Boolean getSuccess() {
        return success;
    }

    public void setSuccess(Boolean success) {
        this.success = success;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public Long getDurationMs() {
        return durationMs;
    }

    public void setDurationMs(Long durationMs) {
        this.durationMs = durationMs;
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

    public Integer getFileCount() {
        return fileCount;
    }

    public void setFileCount(Integer fileCount) {
        this.fileCount = fileCount;
    }

    public String getBatchOperationId() {
        return batchOperationId;
    }

    public void setBatchOperationId(String batchOperationId) {
        this.batchOperationId = batchOperationId;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }
}
