package gov.nist.oscal.tools.api.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import gov.nist.oscal.tools.api.entity.AuditEvent;

import java.time.LocalDateTime;

/**
 * JSON-facing shape of an audit event for the admin dashboard.
 *
 * <p>Differs from the {@link AuditEvent} entity in two ways:
 * <ul>
 *   <li>IP address is masked (last octets removed) to limit re-identification
 *       if the dashboard is screenshotted, copied, or proxied.</li>
 *   <li>The {@code integrityHash} / {@code previousHash} chain fields are
 *       omitted; they're internal tamper-detection state, not display data.</li>
 * </ul>
 *
 * <p>The CSV and JSON-Lines exports do <em>not</em> use this DTO — they
 * stream the raw entity so SIEM tooling sees the full IP for correlation.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuditEventResponse {

    private Long id;
    private String eventType;
    private String category;
    private String username;
    private Long userId;
    private String ipAddress;
    private String userAgent;
    private String sessionId;
    private String resource;
    private String action;
    private String outcome;
    private String errorMessage;
    private String metadata;
    private String riskLevel;
    private LocalDateTime timestamp;
    private Long processingTimeMs;
    private Boolean reviewed;
    private String reviewNotes;
    private LocalDateTime reviewedAt;
    private String reviewedBy;
    private String requestUrl;
    private String httpMethod;

    public AuditEventResponse() {
    }

    public static AuditEventResponse from(AuditEvent entity) {
        if (entity == null) {
            return null;
        }
        AuditEventResponse dto = new AuditEventResponse();
        dto.id = entity.getId();
        dto.eventType = entity.getEventType() != null ? entity.getEventType().name() : null;
        dto.category = entity.getCategory();
        dto.username = entity.getUsername();
        dto.userId = entity.getUserId();
        dto.ipAddress = maskIp(entity.getIpAddress());
        dto.userAgent = entity.getUserAgent();
        dto.sessionId = entity.getSessionId();
        dto.resource = entity.getResource();
        dto.action = entity.getAction();
        dto.outcome = entity.getOutcome();
        dto.errorMessage = entity.getErrorMessage();
        dto.metadata = entity.getMetadata();
        dto.riskLevel = entity.getRiskLevel();
        dto.timestamp = entity.getTimestamp();
        dto.processingTimeMs = entity.getProcessingTimeMs();
        dto.reviewed = entity.getReviewed();
        dto.reviewNotes = entity.getReviewNotes();
        dto.reviewedAt = entity.getReviewedAt();
        dto.reviewedBy = entity.getReviewedBy();
        dto.requestUrl = entity.getRequestUrl();
        dto.httpMethod = entity.getHttpMethod();
        return dto;
    }

    /**
     * Mask the trailing host portion of an IP so the dashboard shows the
     * network/region but not the specific endpoint. IPv4: last two octets
     * become {@code x}. IPv6: last four groups become {@code x}. Loopback
     * and unparseable values pass through unchanged.
     */
    static String maskIp(String ip) {
        if (ip == null || ip.isBlank()) {
            return ip;
        }
        if ("127.0.0.1".equals(ip) || "::1".equals(ip) || "localhost".equalsIgnoreCase(ip)) {
            return ip;
        }
        if (ip.contains(".")) {
            String[] parts = ip.split("\\.");
            if (parts.length == 4) {
                return parts[0] + "." + parts[1] + ".x.x";
            }
            return ip;
        }
        if (ip.contains(":")) {
            String[] parts = ip.split(":", -1);
            if (parts.length >= 4) {
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < parts.length; i++) {
                    if (i > 0) {
                        sb.append(":");
                    }
                    sb.append(i < parts.length / 2 ? parts[i] : "x");
                }
                return sb.toString();
            }
        }
        return ip;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }

    public String getUserAgent() { return userAgent; }
    public void setUserAgent(String userAgent) { this.userAgent = userAgent; }

    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }

    public String getResource() { return resource; }
    public void setResource(String resource) { this.resource = resource; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public String getOutcome() { return outcome; }
    public void setOutcome(String outcome) { this.outcome = outcome; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public String getMetadata() { return metadata; }
    public void setMetadata(String metadata) { this.metadata = metadata; }

    public String getRiskLevel() { return riskLevel; }
    public void setRiskLevel(String riskLevel) { this.riskLevel = riskLevel; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

    public Long getProcessingTimeMs() { return processingTimeMs; }
    public void setProcessingTimeMs(Long processingTimeMs) { this.processingTimeMs = processingTimeMs; }

    public Boolean getReviewed() { return reviewed; }
    public void setReviewed(Boolean reviewed) { this.reviewed = reviewed; }

    public String getReviewNotes() { return reviewNotes; }
    public void setReviewNotes(String reviewNotes) { this.reviewNotes = reviewNotes; }

    public LocalDateTime getReviewedAt() { return reviewedAt; }
    public void setReviewedAt(LocalDateTime reviewedAt) { this.reviewedAt = reviewedAt; }

    public String getReviewedBy() { return reviewedBy; }
    public void setReviewedBy(String reviewedBy) { this.reviewedBy = reviewedBy; }

    public String getRequestUrl() { return requestUrl; }
    public void setRequestUrl(String requestUrl) { this.requestUrl = requestUrl; }

    public String getHttpMethod() { return httpMethod; }
    public void setHttpMethod(String httpMethod) { this.httpMethod = httpMethod; }
}
