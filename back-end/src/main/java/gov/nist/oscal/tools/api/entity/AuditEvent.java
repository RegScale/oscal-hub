package gov.nist.oscal.tools.api.entity;

import gov.nist.oscal.tools.api.model.AuditEventType;
import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Audit Event Entity
 * <p>
 * Represents a security or operational event that has been logged for
 * audit trails, compliance reporting, and security monitoring.
 * </p>
 *
 * <h2>Event Data Model</h2>
 * <ul>
 *   <li><b>Event Type</b>: Categorized event (login, file access, etc.)</li>
 *   <li><b>User Identity</b>: Username or service account</li>
 *   <li><b>Context</b>: IP address, user agent, session ID</li>
 *   <li><b>Details</b>: Event-specific metadata (JSON format)</li>
 *   <li><b>Outcome</b>: Success, failure, error</li>
 *   <li><b>Timestamp</b>: When the event occurred (UTC)</li>
 * </ul>
 *
 * <h2>Retention Policy</h2>
 * <ul>
 *   <li>HIGH risk events: 7 years (compliance)</li>
 *   <li>MEDIUM risk events: 2 years (security monitoring)</li>
 *   <li>LOW risk events: 90 days (operational visibility)</li>
 * </ul>
 *
 * <h2>Database Indexing</h2>
 * <p>Recommended indexes for query performance:</p>
 * <ul>
 *   <li>idx_audit_event_type (eventType)</li>
 *   <li>idx_audit_username (username)</li>
 *   <li>idx_audit_timestamp (timestamp)</li>
 *   <li>idx_audit_outcome (outcome)</li>
 *   <li>idx_audit_ip_address (ipAddress)</li>
 * </ul>
 *
 * @see AuditEventType
 * @see gov.nist.oscal.tools.api.service.AuditLogService
 */
@Entity
@Table(name = "audit_events", indexes = {
    @Index(name = "idx_audit_event_type", columnList = "event_type"),
    @Index(name = "idx_audit_username", columnList = "username"),
    @Index(name = "idx_audit_timestamp", columnList = "timestamp"),
    @Index(name = "idx_audit_outcome", columnList = "outcome"),
    @Index(name = "idx_audit_ip_address", columnList = "ip_address")
})
public class AuditEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Type of audit event (login, file access, etc.)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 50)
    private AuditEventType eventType;

    /**
     * Category of the event (Authentication, Authorization, Data Access, etc.)
     * Denormalized from AuditEventType for faster querying
     */
    @Column(name = "category", nullable = false, length = 50)
    private String category;

    /**
     * Username of the user who triggered the event
     * Null for unauthenticated requests or system events
     */
    @Column(name = "username", length = 100)
    private String username;

    /**
     * User ID (if authenticated)
     * Null for unauthenticated requests or system events
     */
    @Column(name = "user_id")
    private Long userId;

    /**
     * IP address of the client that triggered the event
     * Handles X-Forwarded-For for proxied requests
     */
    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    /**
     * User agent string (browser/client identification)
     */
    @Column(name = "user_agent", length = 500)
    private String userAgent;

    /**
     * Session ID or JWT token ID (for correlation)
     */
    @Column(name = "session_id", length = 100)
    private String sessionId;

    /**
     * Resource that was accessed (file ID, endpoint URL, etc.)
     */
    @Column(name = "resource", length = 500)
    private String resource;

    /**
     * Action performed on the resource (READ, WRITE, DELETE, etc.)
     */
    @Column(name = "action", length = 50)
    private String action;

    /**
     * Outcome of the event (SUCCESS, FAILURE, ERROR)
     */
    @Column(name = "outcome", nullable = false, length = 20)
    private String outcome;

    /**
     * Error message (if outcome is FAILURE or ERROR)
     */
    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    /**
     * Additional event-specific metadata in JSON format
     * Examples:
     * - {"fileName": "catalog.json", "fileSize": 12345}
     * - {"oldEmail": "old@example.com", "newEmail": "new@example.com"}
     * - {"failedAttempts": 3, "remainingAttempts": 2}
     */
    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata;

    /**
     * Risk level of the event (LOW, MEDIUM, HIGH)
     * Denormalized from AuditEventType for faster querying
     */
    @Column(name = "risk_level", length = 10)
    private String riskLevel;

    /**
     * Timestamp when the event occurred (UTC)
     */
    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;

    /**
     * Processing time in milliseconds (for performance monitoring)
     */
    @Column(name = "processing_time_ms")
    private Long processingTimeMs;

    /**
     * Whether this event has been reviewed by security team
     */
    @Column(name = "reviewed")
    private Boolean reviewed = false;

    /**
     * Notes from security review
     */
    @Column(name = "review_notes", length = 1000)
    private String reviewNotes;

    /**
     * Timestamp when the event was reviewed
     */
    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    /**
     * Username of the person who reviewed the event
     */
    @Column(name = "reviewed_by", length = 100)
    private String reviewedBy;

    // ========================================
    // Constructors
    // ========================================

    public AuditEvent() {
        this.timestamp = LocalDateTime.now();
        this.reviewed = false;
    }

    public AuditEvent(AuditEventType eventType, String username, String outcome) {
        this();
        this.eventType = eventType;
        this.category = eventType.getCategory();
        this.riskLevel = eventType.getRiskLevel();
        this.username = username;
        this.outcome = outcome;
    }

    // ========================================
    // Getters and Setters
    // ========================================

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public AuditEventType getEventType() {
        return eventType;
    }

    public void setEventType(AuditEventType eventType) {
        this.eventType = eventType;
        if (eventType != null) {
            this.category = eventType.getCategory();
            this.riskLevel = eventType.getRiskLevel();
        }
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getResource() {
        return resource;
    }

    public void setResource(String resource) {
        this.resource = resource;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getOutcome() {
        return outcome;
    }

    public void setOutcome(String outcome) {
        this.outcome = outcome;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getMetadata() {
        return metadata;
    }

    public void setMetadata(String metadata) {
        this.metadata = metadata;
    }

    public String getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(String riskLevel) {
        this.riskLevel = riskLevel;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public Long getProcessingTimeMs() {
        return processingTimeMs;
    }

    public void setProcessingTimeMs(Long processingTimeMs) {
        this.processingTimeMs = processingTimeMs;
    }

    public Boolean getReviewed() {
        return reviewed;
    }

    public void setReviewed(Boolean reviewed) {
        this.reviewed = reviewed;
    }

    public String getReviewNotes() {
        return reviewNotes;
    }

    public void setReviewNotes(String reviewNotes) {
        this.reviewNotes = reviewNotes;
    }

    public LocalDateTime getReviewedAt() {
        return reviewedAt;
    }

    public void setReviewedAt(LocalDateTime reviewedAt) {
        this.reviewedAt = reviewedAt;
    }

    public String getReviewedBy() {
        return reviewedBy;
    }

    public void setReviewedBy(String reviewedBy) {
        this.reviewedBy = reviewedBy;
    }

    // ========================================
    // Utility Methods
    // ========================================

    /**
     * Check if this is a high-risk event
     *
     * @return true if risk level is HIGH
     */
    public boolean isHighRisk() {
        return "HIGH".equals(riskLevel);
    }

    /**
     * Check if this is a failed event
     *
     * @return true if outcome is FAILURE or ERROR
     */
    public boolean isFailure() {
        return "FAILURE".equals(outcome) || "ERROR".equals(outcome);
    }

    /**
     * Check if this event requires review
     *
     * @return true if high risk and not yet reviewed
     */
    public boolean requiresReview() {
        return isHighRisk() && !reviewed;
    }

    /**
     * Get a summary string for this event
     *
     * @return Summary string
     */
    public String getSummary() {
        return String.format(
            "[%s] %s by %s from %s - %s",
            timestamp,
            eventType.getDescription(),
            username != null ? username : "anonymous",
            ipAddress != null ? ipAddress : "unknown",
            outcome
        );
    }

    @Override
    public String toString() {
        return String.format(
            "AuditEvent{id=%d, type=%s, username='%s', outcome='%s', timestamp=%s}",
            id, eventType, username, outcome, timestamp
        );
    }
}
