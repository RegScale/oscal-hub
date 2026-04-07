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

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.context.annotation.Lazy;

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
    private final SiemForwardingService siemForwardingService;

    /**
     * Self-reference for calling @Async methods through the Spring proxy.
     * Without this, internal method calls bypass the proxy and @Async is ignored.
     */
    @Autowired
    @Lazy
    private AuditLogService self;

    /**
     * Thread-safe storage of the last audit event hash for chain linking.
     * This creates a blockchain-like chain of audit events.
     */
    private final AtomicReference<String> lastEventHash = new AtomicReference<>("");

    @Autowired
    public AuditLogService(AuditEventRepository auditEventRepository,
                          AuditLogConfig config,
                          ObjectMapper objectMapper,
                          @Lazy SiemForwardingService siemForwardingService) {
        this.auditEventRepository = auditEventRepository;
        this.config = config;
        this.objectMapper = objectMapper;
        this.siemForwardingService = siemForwardingService;
        // Initialize with the hash of the most recent event if exists
        initializeLastHash();
    }

    /**
     * Initialize the last event hash from the database on startup
     */
    private void initializeLastHash() {
        try {
            AuditEvent lastEvent = auditEventRepository.findTopByOrderByIdDesc();
            if (lastEvent != null && lastEvent.getIntegrityHash() != null) {
                lastEventHash.set(lastEvent.getIntegrityHash());
                logger.info("Initialized audit chain with last hash: {}...",
                    lastEvent.getIntegrityHash().substring(0, 8));
            }
        } catch (Exception e) {
            logger.warn("Could not initialize last audit hash: {}", e.getMessage());
        }
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
     * Log a successful authentication event.
     * Uses self-reference to call through Spring proxy so @Async works.
     */
    public void logAuthSuccess(String username, Long userId) {
        self.logEvent(AuditEventType.AUTH_LOGIN_SUCCESS, username, userId, "SUCCESS", null, "LOGIN", null);
    }

    /**
     * Log a failed authentication event.
     * Uses self-reference to call through Spring proxy so @Async works.
     */
    public void logAuthFailure(String username, String errorMessage) {
        self.logFailure(AuditEventType.AUTH_LOGIN_FAILURE, username, errorMessage);
    }

    /**
     * Log account lockout.
     * Uses self-reference to call through Spring proxy so @Async works.
     */
    public void logAccountLockout(String username, Long userId, int failedAttempts) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("failedAttempts", failedAttempts);
        self.logEvent(AuditEventType.SECURITY_ACCOUNT_LOCKED, username, userId, "SUCCESS",
                username, "LOCK", metadata);
    }

    /**
     * Log file upload.
     * Uses self-reference to call through Spring proxy so @Async works.
     */
    public void logFileUpload(String username, Long userId, String fileName, long fileSize) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("fileName", fileName);
        metadata.put("fileSize", fileSize);
        self.logEvent(AuditEventType.DATA_FILE_UPLOAD, username, userId, "SUCCESS",
                fileName, "UPLOAD", metadata);
    }

    /**
     * Log file access.
     * Uses self-reference to call through Spring proxy so @Async works.
     */
    public void logFileAccess(String username, Long userId, String fileId) {
        self.logEvent(AuditEventType.DATA_FILE_ACCESS, username, userId, "SUCCESS",
                fileId, "READ", null);
    }

    /**
     * Log file deletion.
     * Uses self-reference to call through Spring proxy so @Async works.
     */
    public void logFileDelete(String username, Long userId, String fileId) {
        self.logEvent(AuditEventType.DATA_FILE_DELETE, username, userId, "SUCCESS",
                fileId, "DELETE", null);
    }

    /**
     * Log a security event (MFA operations, security policy changes, etc.)
     *
     * @param eventType The type of security event
     * @param username The username performing the action
     * @param details Description of what happened
     * @param request The HTTP request (for IP, user agent, etc.)
     */
    @Async
    @Transactional
    public void logSecurityEvent(AuditEventType eventType, String username, String details,
                                  HttpServletRequest request) {
        if (!config.isEnabled()) return;

        AuditEvent event = new AuditEvent(eventType, username, "SUCCESS");
        event.setAction(details);

        if (request != null) {
            event.setIpAddress(getClientIpAddress(request));
            event.setUserAgent(request.getHeader("User-Agent"));
            event.setSessionId(request.getSession(false) != null ?
                request.getSession().getId() : null);
            event.setHttpMethod(request.getMethod());
            event.setRequestUrl(request.getRequestURI());
        }

        saveEvent(event);
    }

    /**
     * Log a security event failure
     *
     * @param eventType The type of security event
     * @param username The username performing the action
     * @param details Description of what happened
     * @param request The HTTP request (for IP, user agent, etc.)
     */
    @Async
    @Transactional
    public void logSecurityEventFailure(AuditEventType eventType, String username, String details,
                                         HttpServletRequest request) {
        if (!config.isEnabled()) return;

        AuditEvent event = new AuditEvent(eventType, username, "FAILURE");
        event.setAction(details);
        event.setErrorMessage(details);

        if (request != null) {
            event.setIpAddress(getClientIpAddress(request));
            event.setUserAgent(request.getHeader("User-Agent"));
            event.setSessionId(request.getSession(false) != null ?
                request.getSession().getId() : null);
            event.setHttpMethod(request.getMethod());
            event.setRequestUrl(request.getRequestURI());
        }

        saveEvent(event);
    }

    /**
     * Log a configuration change event (security policy updates, etc.)
     *
     * @param username The username making the change
     * @param details Description of the configuration change
     * @param request The HTTP request (for IP, user agent, etc.)
     */
    @Async
    @Transactional
    public void logConfigChange(String username, String details, HttpServletRequest request) {
        if (!config.isEnabled()) return;

        AuditEvent event = new AuditEvent(AuditEventType.CONFIG_SECURITY_POLICY_CHANGE, username, "SUCCESS");
        event.setAction(details);
        event.setResource("SecurityPolicy");

        if (request != null) {
            event.setIpAddress(getClientIpAddress(request));
            event.setUserAgent(request.getHeader("User-Agent"));
            event.setSessionId(request.getSession(false) != null ?
                request.getSession().getId() : null);
            event.setHttpMethod(request.getMethod());
            event.setRequestUrl(request.getRequestURI());
        }

        saveEvent(event);
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

            // Capture request URL and HTTP method
            event.setHttpMethod(request.getMethod());
            String requestUrl = request.getRequestURI();
            String queryString = request.getQueryString();
            if (queryString != null && !queryString.isEmpty()) {
                requestUrl = requestUrl + "?" + queryString;
            }
            event.setRequestUrl(requestUrl);
        }

        return event;
    }

    /**
     * Save event to database and optionally log to application log.
     * Computes integrity hash before saving to ensure immutability.
     * Also forwards the event to SIEM if configured.
     */
    private void saveEvent(AuditEvent event) {
        try {
            // Set the previous hash for chain linking
            event.setPreviousHash(lastEventHash.get());

            // Compute the integrity hash
            String hash = computeHash(event.getHashContent());
            event.setIntegrityHash(hash);

            // Save the event
            AuditEvent savedEvent = auditEventRepository.save(event);

            // Update the last hash for the next event
            lastEventHash.set(hash);

            if (config.isLogToApplicationLog()) {
                String logMessage = String.format("[AUDIT] %s", event.getSummary());
                if (event.isHighRisk() || event.isFailure()) {
                    logger.warn(logMessage);
                } else {
                    logger.info(logMessage);
                }
            }

            // Forward to SIEM if configured
            if (siemForwardingService != null) {
                siemForwardingService.queueEvent(savedEvent);
            }
        } catch (Exception e) {
            logger.error("Failed to save audit event: {}", event.getEventType(), e);
        }
    }

    /**
     * Compute SHA-256 hash of the given content.
     *
     * @param content The content to hash
     * @return Hex-encoded SHA-256 hash
     */
    private String computeHash(String content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(content.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            logger.error("SHA-256 algorithm not available", e);
            return null;
        }
    }

    /**
     * Verify the integrity of an audit event chain.
     *
     * @param eventId The ID of the event to verify
     * @return true if the event and its chain are valid
     */
    public boolean verifyEventIntegrity(Long eventId) {
        AuditEvent event = auditEventRepository.findById(eventId).orElse(null);
        if (event == null) {
            return false;
        }
        String computedHash = computeHash(event.getHashContent());
        return event.verifyIntegrity(computedHash);
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
