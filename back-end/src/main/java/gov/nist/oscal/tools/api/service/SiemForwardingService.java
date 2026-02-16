package gov.nist.oscal.tools.api.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import gov.nist.oscal.tools.api.config.SiemConfig;
import gov.nist.oscal.tools.api.entity.AuditEvent;
import gov.nist.oscal.tools.api.repository.AuditEventRepository;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * SIEM Forwarding Service
 * <p>
 * Forwards audit events to external SIEM systems via webhook integration.
 * Supports multiple output formats (JSON, CEF, Syslog) and includes
 * batching, retry logic, and error handling.
 * </p>
 *
 * <h2>Features</h2>
 * <ul>
 *   <li>Batch processing for efficiency</li>
 *   <li>Exponential backoff retry logic</li>
 *   <li>Multiple output formats (JSON, CEF, Syslog)</li>
 *   <li>Category and risk level filtering</li>
 *   <li>Status tracking and metrics</li>
 * </ul>
 *
 * @see SiemConfig
 */
@Service
public class SiemForwardingService {

    private static final Logger logger = LoggerFactory.getLogger(SiemForwardingService.class);

    private final SiemConfig config;
    private final AuditEventRepository auditEventRepository;
    private final ObjectMapper objectMapper;

    private HttpClient httpClient;

    // Event queue for batching
    private final Queue<AuditEvent> eventQueue = new ConcurrentLinkedQueue<>();

    // Status tracking
    private final AtomicLong eventsForwarded = new AtomicLong(0);
    private final AtomicLong eventsFailed = new AtomicLong(0);
    private final AtomicLong batchesSent = new AtomicLong(0);
    private final AtomicLong batchesFailed = new AtomicLong(0);
    private final AtomicBoolean isHealthy = new AtomicBoolean(true);
    private volatile LocalDateTime lastSuccessfulSend;
    private volatile LocalDateTime lastFailedSend;
    private volatile String lastError;
    private volatile Long lastForwardedEventId;

    // CEF severity mapping
    private static final Map<String, Integer> CEF_SEVERITY = Map.of(
        "LOW", 3,
        "MEDIUM", 6,
        "HIGH", 9
    );

    @Autowired
    public SiemForwardingService(SiemConfig config,
                                  AuditEventRepository auditEventRepository,
                                  ObjectMapper objectMapper) {
        this.config = config;
        this.auditEventRepository = auditEventRepository;
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void initialize() {
        if (!config.isEnabled()) {
            logger.info("SIEM forwarding is disabled");
            return;
        }

        // Initialize HTTP client
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofMillis(config.getConnectionTimeoutMs()))
            .build();

        // Load last forwarded event ID from database
        initializeLastForwardedId();

        logger.info("SIEM forwarding service initialized");
    }

    private void initializeLastForwardedId() {
        try {
            // Get the most recent event to start from
            var page = auditEventRepository.findAllByOrderByTimestampDesc(PageRequest.of(0, 1));
            if (!page.isEmpty()) {
                lastForwardedEventId = page.getContent().get(0).getId();
                logger.info("SIEM forwarding starting from event ID: {}", lastForwardedEventId);
            }
        } catch (Exception e) {
            logger.warn("Could not initialize last forwarded ID: {}", e.getMessage());
        }
    }

    @PreDestroy
    public void shutdown() {
        // Flush any remaining events
        if (!eventQueue.isEmpty()) {
            logger.info("Flushing {} remaining events before shutdown", eventQueue.size());
            flushBatch();
        }
    }

    /**
     * Queue an event for forwarding to SIEM
     * Called by AuditLogService after saving an event
     */
    public void queueEvent(AuditEvent event) {
        if (!config.isEnabled()) {
            return;
        }

        // Apply filters
        if (!shouldForward(event)) {
            return;
        }

        eventQueue.offer(event);

        // If batch size reached, trigger immediate flush
        if (eventQueue.size() >= config.getBatchSize()) {
            flushBatch();
        }
    }

    /**
     * Check if an event should be forwarded based on configured filters
     */
    private boolean shouldForward(AuditEvent event) {
        // Check category filter
        if (!config.shouldForwardCategory(event.getCategory())) {
            return false;
        }

        // Check risk level filter
        if (!config.shouldForwardRiskLevel(event.getRiskLevel())) {
            return false;
        }

        // Check failed events only filter
        if (config.isFailedEventsOnly() && !event.isFailure()) {
            return false;
        }

        return true;
    }

    /**
     * Scheduled task to flush batched events
     * Runs based on configured interval
     */
    @Scheduled(fixedRateString = "${siem.batch-interval-seconds:60}000")
    public void scheduledFlush() {
        if (!config.isEnabled() || eventQueue.isEmpty()) {
            return;
        }
        flushBatch();
    }

    /**
     * Flush queued events to SIEM
     */
    public synchronized void flushBatch() {
        if (eventQueue.isEmpty()) {
            return;
        }

        List<AuditEvent> batch = new ArrayList<>();
        while (!eventQueue.isEmpty() && batch.size() < config.getBatchSize()) {
            AuditEvent event = eventQueue.poll();
            if (event != null) {
                batch.add(event);
            }
        }

        if (batch.isEmpty()) {
            return;
        }

        // Format the batch
        String payload = formatBatch(batch);
        if (payload == null) {
            logger.error("Failed to format batch for SIEM");
            eventsFailed.addAndGet(batch.size());
            return;
        }

        // Send with retry
        boolean success = sendWithRetry(payload);

        if (success) {
            eventsForwarded.addAndGet(batch.size());
            batchesSent.incrementAndGet();
            lastSuccessfulSend = LocalDateTime.now();
            isHealthy.set(true);

            // Update last forwarded ID
            if (!batch.isEmpty()) {
                lastForwardedEventId = batch.get(batch.size() - 1).getId();
            }

            logger.debug("Successfully forwarded {} events to SIEM", batch.size());
        } else {
            eventsFailed.addAndGet(batch.size());
            batchesFailed.incrementAndGet();
            lastFailedSend = LocalDateTime.now();
            isHealthy.set(false);

            logger.error("Failed to forward {} events to SIEM after {} retries",
                batch.size(), config.getMaxRetries());
        }
    }

    /**
     * Format a batch of events according to configured format
     */
    private String formatBatch(List<AuditEvent> events) {
        String format = config.getFormat().toLowerCase();

        switch (format) {
            case "cef":
                return formatAsCef(events);
            case "syslog":
                return formatAsSyslog(events);
            case "json":
            default:
                return formatAsJson(events);
        }
    }

    /**
     * Format events as JSON (default)
     */
    private String formatAsJson(List<AuditEvent> events) {
        try {
            ArrayNode array = objectMapper.createArrayNode();

            for (AuditEvent event : events) {
                ObjectNode node = objectMapper.createObjectNode();
                node.put("id", event.getId());
                node.put("timestamp", formatTimestamp(event.getTimestamp()));
                node.put("eventType", event.getEventType().name());
                node.put("category", event.getCategory());
                node.put("description", event.getEventType().getDescription());
                node.put("username", event.getUsername());
                node.put("userId", event.getUserId());
                node.put("ipAddress", event.getIpAddress());
                node.put("userAgent", event.getUserAgent());
                node.put("sessionId", event.getSessionId());
                node.put("resource", event.getResource());
                node.put("action", event.getAction());
                node.put("outcome", event.getOutcome());
                node.put("errorMessage", event.getErrorMessage());
                node.put("riskLevel", event.getRiskLevel());
                node.put("httpMethod", event.getHttpMethod());
                node.put("requestUrl", event.getRequestUrl());
                node.put("processingTimeMs", event.getProcessingTimeMs());

                // Parse and include metadata as nested object
                if (event.getMetadata() != null && !event.getMetadata().isEmpty()) {
                    try {
                        node.set("metadata", objectMapper.readTree(event.getMetadata()));
                    } catch (JsonProcessingException e) {
                        node.put("metadata", event.getMetadata());
                    }
                }

                array.add(node);
            }

            return objectMapper.writeValueAsString(array);
        } catch (JsonProcessingException e) {
            logger.error("Failed to format events as JSON", e);
            return null;
        }
    }

    /**
     * Format events as CEF (Common Event Format)
     * Format: CEF:Version|Device Vendor|Device Product|Device Version|Signature ID|Name|Severity|Extension
     */
    private String formatAsCef(List<AuditEvent> events) {
        StringBuilder sb = new StringBuilder();

        for (AuditEvent event : events) {
            // CEF header
            sb.append("CEF:0|");
            sb.append(escapeCef(config.getCefVendor())).append("|");
            sb.append(escapeCef(config.getCefProduct())).append("|");
            sb.append(escapeCef(config.getCefVersion())).append("|");
            sb.append(event.getEventType().name()).append("|");
            sb.append(escapeCef(event.getEventType().getDescription())).append("|");
            sb.append(CEF_SEVERITY.getOrDefault(event.getRiskLevel(), 3)).append("|");

            // CEF extension
            sb.append("rt=").append(formatTimestamp(event.getTimestamp()));

            if (event.getUsername() != null) {
                sb.append(" suser=").append(escapeCefValue(event.getUsername()));
            }
            if (event.getIpAddress() != null) {
                sb.append(" src=").append(event.getIpAddress());
            }
            if (event.getRequestUrl() != null) {
                sb.append(" request=").append(escapeCefValue(event.getRequestUrl()));
            }
            if (event.getHttpMethod() != null) {
                sb.append(" requestMethod=").append(event.getHttpMethod());
            }
            if (event.getOutcome() != null) {
                sb.append(" outcome=").append(event.getOutcome());
            }
            if (event.getResource() != null) {
                sb.append(" cs1=").append(escapeCefValue(event.getResource()));
                sb.append(" cs1Label=Resource");
            }
            if (event.getErrorMessage() != null) {
                sb.append(" msg=").append(escapeCefValue(event.getErrorMessage()));
            }

            sb.append("\n");
        }

        return sb.toString();
    }

    /**
     * Format events as RFC 5424 Syslog
     */
    private String formatAsSyslog(List<AuditEvent> events) {
        StringBuilder sb = new StringBuilder();

        int facility = getSyslogFacility();
        String hostname = config.getSyslogHostname() != null ?
            config.getSyslogHostname() : "oscal-api";
        String appName = config.getSyslogAppName();

        for (AuditEvent event : events) {
            int severity = getSyslogSeverity(event);
            int priority = (facility * 8) + severity;

            // RFC 5424 format
            sb.append("<").append(priority).append(">");
            sb.append("1 "); // Version
            sb.append(formatTimestampSyslog(event.getTimestamp())).append(" ");
            sb.append(hostname).append(" ");
            sb.append(appName).append(" ");
            sb.append(event.getId() != null ? event.getId() : "-").append(" ");
            sb.append(event.getEventType().name()).append(" ");

            // Structured data
            sb.append("[oscal@1 ");
            sb.append("eventType=\"").append(event.getEventType().name()).append("\" ");
            sb.append("category=\"").append(event.getCategory()).append("\" ");
            sb.append("outcome=\"").append(event.getOutcome()).append("\" ");
            sb.append("riskLevel=\"").append(event.getRiskLevel()).append("\"");
            if (event.getUsername() != null) {
                sb.append(" user=\"").append(escapeSyslogValue(event.getUsername())).append("\"");
            }
            if (event.getIpAddress() != null) {
                sb.append(" ip=\"").append(event.getIpAddress()).append("\"");
            }
            sb.append("] ");

            // Message
            sb.append(event.getEventType().getDescription());
            if (event.getResource() != null) {
                sb.append(" - ").append(event.getResource());
            }

            sb.append("\n");
        }

        return sb.toString();
    }

    /**
     * Send payload to SIEM with retry logic
     */
    private boolean sendWithRetry(String payload) {
        String url = config.getWebhookUrl();
        if (url == null || url.trim().isEmpty()) {
            logger.error("SIEM webhook URL not configured");
            lastError = "Webhook URL not configured";
            return false;
        }

        long delay = config.getRetryDelayMs();

        for (int attempt = 1; attempt <= config.getMaxRetries(); attempt++) {
            try {
                HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofMillis(config.getReadTimeoutMs()))
                    .header("Content-Type", getContentType())
                    .POST(HttpRequest.BodyPublishers.ofString(payload));

                // Add authorization header if configured
                if (config.getAuthorizationHeader() != null &&
                    !config.getAuthorizationHeader().trim().isEmpty()) {
                    requestBuilder.header(
                        config.getAuthHeaderName(),
                        config.getAuthorizationHeader()
                    );
                }

                HttpRequest request = requestBuilder.build();
                HttpResponse<String> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() >= 200 && response.statusCode() < 300) {
                    lastError = null;
                    return true;
                }

                lastError = String.format("HTTP %d: %s",
                    response.statusCode(), response.body());
                logger.warn("SIEM webhook returned {} on attempt {}: {}",
                    response.statusCode(), attempt, response.body());

            } catch (IOException | InterruptedException e) {
                lastError = e.getMessage();
                logger.warn("SIEM webhook failed on attempt {}: {}", attempt, e.getMessage());

                if (e instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                    return false;
                }
            }

            // Wait before retry (exponential backoff)
            if (attempt < config.getMaxRetries()) {
                try {
                    Thread.sleep(delay);
                    delay = (long) (delay * config.getRetryBackoffMultiplier());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return false;
                }
            }
        }

        return false;
    }

    /**
     * Get content type based on format
     */
    private String getContentType() {
        String format = config.getFormat().toLowerCase();
        switch (format) {
            case "cef":
                return "text/plain; charset=utf-8";
            case "syslog":
                return "text/plain; charset=utf-8";
            case "json":
            default:
                return "application/json; charset=utf-8";
        }
    }

    // ========================================
    // Formatting Utilities
    // ========================================

    private String formatTimestamp(LocalDateTime dt) {
        if (dt == null) return null;
        return dt.atZone(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT);
    }

    private String formatTimestampSyslog(LocalDateTime dt) {
        if (dt == null) return "-";
        return dt.atZone(ZoneOffset.UTC)
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"));
    }

    private String escapeCef(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\")
                   .replace("|", "\\|");
    }

    private String escapeCefValue(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\")
                   .replace("=", "\\=")
                   .replace("\n", "\\n")
                   .replace("\r", "\\r");
    }

    private String escapeSyslogValue(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\")
                   .replace("\"", "\\\"")
                   .replace("]", "\\]");
    }

    private int getSyslogFacility() {
        String facility = config.getSyslogFacility().toUpperCase();
        switch (facility) {
            case "LOCAL0": return 16;
            case "LOCAL1": return 17;
            case "LOCAL2": return 18;
            case "LOCAL3": return 19;
            case "LOCAL4": return 20;
            case "LOCAL5": return 21;
            case "LOCAL6": return 22;
            case "LOCAL7": return 23;
            case "AUTH": return 4;
            case "AUTHPRIV": return 10;
            default: return 16; // LOCAL0
        }
    }

    private int getSyslogSeverity(AuditEvent event) {
        // Map outcome and risk level to syslog severity
        if ("ERROR".equals(event.getOutcome())) return 3; // Error
        if ("FAILURE".equals(event.getOutcome())) return 4; // Warning
        if ("HIGH".equals(event.getRiskLevel())) return 4; // Warning
        if ("MEDIUM".equals(event.getRiskLevel())) return 5; // Notice
        return 6; // Informational
    }

    // ========================================
    // Status and Metrics
    // ========================================

    /**
     * Get the current status of the SIEM forwarding service
     */
    public SiemStatus getStatus() {
        return new SiemStatus(
            config.isEnabled(),
            isHealthy.get(),
            eventsForwarded.get(),
            eventsFailed.get(),
            batchesSent.get(),
            batchesFailed.get(),
            eventQueue.size(),
            lastSuccessfulSend,
            lastFailedSend,
            lastError,
            config.getWebhookUrl() != null ? "***configured***" : null,
            config.getFormat()
        );
    }

    /**
     * Test the SIEM connection by sending a test event
     */
    public TestResult testConnection() {
        if (!config.isEnabled()) {
            return new TestResult(false, "SIEM forwarding is disabled");
        }

        if (config.getWebhookUrl() == null || config.getWebhookUrl().trim().isEmpty()) {
            return new TestResult(false, "Webhook URL not configured");
        }

        // Create a test event
        String testPayload;
        if ("json".equalsIgnoreCase(config.getFormat())) {
            testPayload = "[{\"test\": true, \"message\": \"OSCAL SIEM connection test\", \"timestamp\": \""
                + Instant.now().toString() + "\"}]";
        } else if ("cef".equalsIgnoreCase(config.getFormat())) {
            testPayload = "CEF:0|" + config.getCefVendor() + "|" + config.getCefProduct()
                + "|" + config.getCefVersion() + "|TEST|Connection Test|1|msg=SIEM connection test";
        } else {
            testPayload = "<134>1 " + Instant.now().toString()
                + " oscal-api test - TEST - Connection test message";
        }

        boolean success = sendWithRetry(testPayload);

        if (success) {
            return new TestResult(true, "Connection successful");
        } else {
            return new TestResult(false, "Connection failed: " + (lastError != null ? lastError : "Unknown error"));
        }
    }

    /**
     * Manually trigger a flush of queued events
     */
    public int manualFlush() {
        int queuedCount = eventQueue.size();
        if (queuedCount > 0) {
            flushBatch();
        }
        return queuedCount;
    }

    // ========================================
    // Status DTOs
    // ========================================

    public static class SiemStatus {
        private final boolean enabled;
        private final boolean healthy;
        private final long eventsForwarded;
        private final long eventsFailed;
        private final long batchesSent;
        private final long batchesFailed;
        private final int queuedEvents;
        private final LocalDateTime lastSuccessfulSend;
        private final LocalDateTime lastFailedSend;
        private final String lastError;
        private final String webhookUrl;
        private final String format;

        public SiemStatus(boolean enabled, boolean healthy, long eventsForwarded,
                         long eventsFailed, long batchesSent, long batchesFailed,
                         int queuedEvents, LocalDateTime lastSuccessfulSend,
                         LocalDateTime lastFailedSend, String lastError,
                         String webhookUrl, String format) {
            this.enabled = enabled;
            this.healthy = healthy;
            this.eventsForwarded = eventsForwarded;
            this.eventsFailed = eventsFailed;
            this.batchesSent = batchesSent;
            this.batchesFailed = batchesFailed;
            this.queuedEvents = queuedEvents;
            this.lastSuccessfulSend = lastSuccessfulSend;
            this.lastFailedSend = lastFailedSend;
            this.lastError = lastError;
            this.webhookUrl = webhookUrl;
            this.format = format;
        }

        // Getters
        public boolean isEnabled() { return enabled; }
        public boolean isHealthy() { return healthy; }
        public long getEventsForwarded() { return eventsForwarded; }
        public long getEventsFailed() { return eventsFailed; }
        public long getBatchesSent() { return batchesSent; }
        public long getBatchesFailed() { return batchesFailed; }
        public int getQueuedEvents() { return queuedEvents; }
        public LocalDateTime getLastSuccessfulSend() { return lastSuccessfulSend; }
        public LocalDateTime getLastFailedSend() { return lastFailedSend; }
        public String getLastError() { return lastError; }
        public String getWebhookUrl() { return webhookUrl; }
        public String getFormat() { return format; }
    }

    public static class TestResult {
        private final boolean success;
        private final String message;

        public TestResult(boolean success, String message) {
            this.success = success;
            this.message = message;
        }

        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
    }
}
