package gov.nist.oscal.tools.api.model;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import gov.nist.oscal.tools.api.entity.ArtifactVersion;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Response DTO for artifact version
 */
public class ArtifactVersionResponse {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    private String versionId;
    private Integer versionNumber;
    private Long contentSize;
    private String uploadedBy;
    private LocalDateTime uploadedAt;
    private String changeDescription;
    private List<String> extractedVariables;

    // Constructors
    public ArtifactVersionResponse() {
    }

    public static ArtifactVersionResponse fromEntity(ArtifactVersion version) {
        ArtifactVersionResponse response = new ArtifactVersionResponse();
        response.setVersionId(version.getVersionId());
        response.setVersionNumber(version.getVersionNumber());
        response.setContentSize(version.getContentSize());

        // Handle lazy-loaded uploadedBy
        try {
            response.setUploadedBy(version.getUploadedBy().getUsername());
        } catch (org.hibernate.LazyInitializationException e) {
            response.setUploadedBy(null);
        }

        response.setUploadedAt(version.getUploadedAt());
        response.setChangeDescription(version.getChangeDescription());
        response.setExtractedVariables(parseVariables(version.getExtractedVariablesSnapshot()));
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

    public Long getContentSize() {
        return contentSize;
    }

    public void setContentSize(Long contentSize) {
        this.contentSize = contentSize;
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

    public List<String> getExtractedVariables() {
        return extractedVariables;
    }

    public void setExtractedVariables(List<String> extractedVariables) {
        this.extractedVariables = extractedVariables;
    }
}
