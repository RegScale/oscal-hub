package gov.nist.oscal.tools.api.model;

import gov.nist.oscal.tools.api.entity.LibraryVersion;

import java.time.LocalDateTime;

/**
 * Response DTO for library version
 */
public class LibraryVersionResponse {

    private String versionId;
    private Integer versionNumber;
    private String fileName;
    private String format;
    private Long fileSize;
    private String uploadedBy;
    private LocalDateTime uploadedAt;
    private String changeDescription;

    // Constructors
    public LibraryVersionResponse() {
    }

    public static LibraryVersionResponse fromEntity(LibraryVersion version) {
        LibraryVersionResponse response = new LibraryVersionResponse();
        response.setVersionId(version.getVersionId());
        response.setVersionNumber(version.getVersionNumber());
        response.setFileName(version.getFileName());
        response.setFormat(version.getFormat());
        response.setFileSize(version.getFileSize());
        response.setUploadedBy(version.getUploadedBy().getUsername());
        response.setUploadedAt(version.getUploadedAt());
        response.setChangeDescription(version.getChangeDescription());
        return response;
    }

    // Getters and Setters
    public String getVersionId() {
        return versionId;
    }

    public void setVersionId(String versionId) {
        this.versionId = versionId;
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

    public String getUploadedBy() {
        return uploadedBy;
    }

    public void setUploadedBy(String uploadedBy) {
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
}
