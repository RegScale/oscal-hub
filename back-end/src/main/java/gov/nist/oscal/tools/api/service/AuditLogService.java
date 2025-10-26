package gov.nist.oscal.tools.api.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import gov.nist.oscal.tools.api.config.AuditLogConfig;
import gov.nist.oscal.tools.api.entity.AuditEvent;
import gov.nist.oscal.tools.api.model.AuditEventType;
import gov.nist.oscal.tools.api.repository.AuditEventRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Audit Logging Service
 * <p>
 * Central service for logging security and operational events to the audit trail.
 * Provides methods for logging various event types with contextual information.
 * </p>
 *
 * @see AuditEvent
 * @see AuditEventType
 */
@Service
public class AuditLogService {

    private static final Logger logger = LoggerFactory.getLogger(AuditLogService.class);

    private final AuditEventRepository auditEventRepository;
    private final AuditLogConfig config;
    private final ObjectMapper objectMapper;

    @Autowired
    public AuditLogService(AuditEventRepository auditEventRepository,
                          AuditLogConfig config,
                          ObjectMapper objectMapper) {
        this.auditEventRepository = auditEventRepository;
        this.config = config;
        this.objectMapper = objectMapper;
    }

    /**
     * Log an audit event asynchronously
     */
    @Async
    @Transactional
    public void logEvent(AuditEventType eventType, String username, String outcome) {
        if (!config.isEnabled()) return;

        AuditEvent event = createBaseEvent(eventType, username, outcome);
        saveEvent(event);
    }

    /**
     * Log an audit event with additional details
     */
    @Async
    @Transactional
    public void logEvent(AuditEventType eventType, String username, String outcome,
                        String resource, String action) {
        if (!config.isEnabled()) return;

        AuditEvent event = createBaseEvent(eventType, username, outcome);
        event.setResource(resource);
        event.setAction(action);
        saveEvent(event);
    }

    /**
     * Log an audit event with metadata
     */
    @Async
    @Transactional
    public void logEvent(AuditEventType eventType, String username, Long userId,
                        String outcome, String resource, String action,
                        Map<String, Object> metadata) {
        if (!config.isEnabled()) return;

        AuditEvent event = createBaseEvent(eventType, username, outcome);
        event.setUserId(userId);
        event.setResource(resource);
        event.setAction(action);

        if (metadata != null && !metadata.isEmpty()) {
            try {
                event.setMetadata(objectMapper.writeValueAsString(metadata));
            } catch (JsonProcessingException e) {
                logger.warn("Failed to serialize metadata for audit event", e);
            }
        }

        saveEvent(event);
    }

    /**
     * Log a failed event with error message
     */
    @Async
    @Transactional
    public void logFailure(AuditEventType eventType, String username, String errorMessage) {
        if (!config.isEnabled()) return;

        AuditEvent event = createBaseEvent(eventType, username, "FAILURE");
        event.setErrorMessage(errorMessage);
        saveEvent(event);
    }

    /**
     * Log a successful authentication event
     */
    public void logAuthSuccess(String username, Long userId) {
        logEvent(AuditEventType.AUTH_LOGIN_SUCCESS, username, userId, "SUCCESS", null, "LOGIN", null);
    }

    /**
     * Log a failed authentication event
     */
    public void logAuthFailure(String username, String errorMessage) {
        logFailure(AuditEventType.AUTH_LOGIN_FAILURE, username, errorMessage);
    }

    /**
     * Log account lockout
     */
    public void logAccountLockout(String username, Long userId, int failedAttempts) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("failedAttempts", failedAttempts);
        logEvent(AuditEventType.SECURITY_ACCOUNT_LOCKED, username, userId, "SUCCESS",
                username, "LOCK", metadata);
    }

    /**
     * Log file upload
     */
    public void logFileUpload(String username, Long userId, String fileName, long fileSize) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("fileName", fileName);
        metadata.put("fileSize", fileSize);
        logEvent(AuditEventType.DATA_FILE_UPLOAD, username, userId, "SUCCESS",
                fileName, "UPLOAD", metadata);
    }

    /**
     * Log file access
     */
    public void logFileAccess(String username, Long userId, String fileId) {
        logEvent(AuditEventType.DATA_FILE_ACCESS, username, userId, "SUCCESS",
                fileId, "READ", null);
    }

    /**
     * Log file deletion
     */
    public void logFileDelete(String username, Long userId, String fileId) {
        logEvent(AuditEventType.DATA_FILE_DELETE, username, userId, "SUCCESS",
                fileId, "DELETE", null);
    }

    /**
     * Create base audit event with context from current HTTP request
     */
    private AuditEvent createBaseEvent(AuditEventType eventType, String username, String outcome) {
        AuditEvent event = new AuditEvent(eventType, username, outcome);

        // Get HTTP request context if available
        ServletRequestAttributes attributes =
            (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            event.setIpAddress(getClientIpAddress(request));
            event.setUserAgent(request.getHeader("User-Agent"));
            event.setSessionId(request.getSession(false) != null ?
                request.getSession().getId() : null);
        }

        return event;
    }

    /**
     * Save event to database and optionally log to application log
     */
    private void saveEvent(AuditEvent event) {
        try {
            auditEventRepository.save(event);

            if (config.isLogToApplicationLog()) {
                String logMessage = String.format("[AUDIT] %s", event.getSummary());
                if (event.isHighRisk() || event.isFailure()) {
                    logger.warn(logMessage);
                } else {
                    logger.info(logMessage);
                }
            }
        } catch (Exception e) {
            logger.error("Failed to save audit event: {}", event.getEventType(), e);
        }
    }

    /**
     * Extract client IP address from request
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }

        return request.getRemoteAddr();
    }
}
