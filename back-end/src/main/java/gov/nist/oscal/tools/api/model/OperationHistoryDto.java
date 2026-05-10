package gov.nist.oscal.tools.api.model;

import gov.nist.oscal.tools.api.entity.OperationHistory;
import gov.nist.oscal.tools.api.entity.User;

import java.time.LocalDateTime;

/**
 * Slim response shape for operation history. Excludes the JPA-managed User
 * relation (which holds password hash, MFA secret, and other sensitive fields)
 * and exposes only userId/username for display.
 */
public class OperationHistoryDto {

    private Long id;
    private String operationType;
    private String fileName;
    private LocalDateTime timestamp;
    private Boolean success;
    private String details;
    private Long durationMs;
    private String modelType;
    private String format;
    private Integer fileCount;
    private String batchOperationId;
    private Long userId;
    private String username;

    public OperationHistoryDto() {
    }

    public static OperationHistoryDto from(OperationHistory entity) {
        if (entity == null) {
            return null;
        }
        OperationHistoryDto dto = new OperationHistoryDto();
        dto.id = entity.getId();
        dto.operationType = entity.getOperationType();
        dto.fileName = entity.getFileName();
        dto.timestamp = entity.getTimestamp();
        dto.success = entity.getSuccess();
        dto.details = entity.getDetails();
        dto.durationMs = entity.getDurationMs();
        dto.modelType = entity.getModelType();
        dto.format = entity.getFormat();
        dto.fileCount = entity.getFileCount();
        dto.batchOperationId = entity.getBatchOperationId();
        User user = entity.getUser();
        if (user != null) {
            dto.userId = user.getId();
            dto.username = user.getUsername();
        }
        return dto;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getOperationType() { return operationType; }
    public void setOperationType(String operationType) { this.operationType = operationType; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

    public Boolean getSuccess() { return success; }
    public void setSuccess(Boolean success) { this.success = success; }

    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }

    public Long getDurationMs() { return durationMs; }
    public void setDurationMs(Long durationMs) { this.durationMs = durationMs; }

    public String getModelType() { return modelType; }
    public void setModelType(String modelType) { this.modelType = modelType; }

    public String getFormat() { return format; }
    public void setFormat(String format) { this.format = format; }

    public Integer getFileCount() { return fileCount; }
    public void setFileCount(Integer fileCount) { this.fileCount = fileCount; }

    public String getBatchOperationId() { return batchOperationId; }
    public void setBatchOperationId(String batchOperationId) { this.batchOperationId = batchOperationId; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
}
