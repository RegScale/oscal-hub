package gov.nist.oscal.tools.api.config;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * SIEM (Security Information and Event Management) Forwarding Configuration
 * <p>
 * Provides configuration for forwarding audit events to external SIEM systems
 * via webhook, syslog, or other integration methods.
 * </p>
 *
 * <h2>Supported Formats</h2>
 * <ul>
 *   <li><b>JSON</b>: Standard JSON format (default)</li>
 *   <li><b>CEF</b>: Common Event Format (ArcSight compatible)</li>
 *   <li><b>SYSLOG</b>: RFC 5424 syslog format</li>
 * </ul>
 *
 * @see gov.nist.oscal.tools.api.service.SiemForwardingService
 */
@Configuration
@ConfigurationProperties(prefix = "siem")
public class SiemConfig {

    private static final Logger logger = LoggerFactory.getLogger(SiemConfig.class);

    /**
     * Enable SIEM forwarding
     */
    private boolean enabled = false;

    /**
     * Webhook URL for sending events
     */
    private String webhookUrl;

    /**
     * Authorization header value (e.g., "Bearer token123")
     */
    private String authorizationHeader;

    /**
     * Custom header name for authentication (default: Authorization)
     */
    private String authHeaderName = "Authorization";

    /**
     * Output format: json, cef, syslog
     */
    private String format = "json";

    /**
     * Number of events to batch before sending
     */
    private int batchSize = 100;

    /**
     * Interval in seconds between batch sends
     */
    private int batchIntervalSeconds = 60;

    /**
     * Categories to include (comma-separated)
     * Default: All categories
     */
    private String includeCategories = "";

    /**
     * Minimum risk level to forward (LOW, MEDIUM, HIGH)
     * Events at or above this level will be forwarded
     */
    private String minRiskLevel = "LOW";

    /**
     * Maximum retry attempts for failed sends
     */
    private int maxRetries = 3;

    /**
     * Initial retry delay in milliseconds
     */
    private long retryDelayMs = 1000;

    /**
     * Retry backoff multiplier
     */
    private double retryBackoffMultiplier = 2.0;

    /**
     * Connection timeout in milliseconds
     */
    private int connectionTimeoutMs = 5000;

    /**
     * Read timeout in milliseconds
     */
    private int readTimeoutMs = 30000;

    /**
     * Include only failed events (outcome = FAILURE or ERROR)
     */
    private boolean failedEventsOnly = false;

    /**
     * CEF vendor name (for CEF format)
     */
    private String cefVendor = "NIST";

    /**
     * CEF product name (for CEF format)
     */
    private String cefProduct = "OSCAL-Tools";

    /**
     * CEF product version (for CEF format)
     */
    private String cefVersion = "1.0";

    /**
     * Syslog facility (for syslog format)
     * Valid values: LOCAL0-LOCAL7, AUTH, AUTHPRIV, etc.
     */
    private String syslogFacility = "LOCAL0";

    /**
     * Syslog hostname (for syslog format)
     */
    private String syslogHostname;

    /**
     * Syslog app name (for syslog format)
     */
    private String syslogAppName = "oscal-api";

    // Parsed values
    private transient Set<String> includeCategoriesSet = new HashSet<>();

    @PostConstruct
    public void validateConfiguration() {
        logger.info("Initializing SIEM Forwarding Configuration...");

        if (!enabled) {
            logger.info("SIEM forwarding is DISABLED");
            return;
        }

        // Parse categories
        if (includeCategories != null && !includeCategories.trim().isEmpty()) {
            includeCategoriesSet = Arrays.stream(includeCategories.split(","))
                .map(String::trim)
                .collect(Collectors.toSet());
        }

        // Validate format
        if (!isValidFormat(format)) {
            logger.error("Invalid SIEM format '{}'. Using 'json' as default.", format);
            format = "json";
        }

        // Validate risk level
        if (!isValidRiskLevel(minRiskLevel)) {
            logger.error("Invalid minimum risk level '{}'. Using 'LOW' as default.", minRiskLevel);
            minRiskLevel = "LOW";
        }

        // Log configuration
        logger.info("SIEM forwarding enabled with the following settings:");
        logger.info("  - Webhook URL: {}", maskUrl(webhookUrl));
        logger.info("  - Format: {}", format);
        logger.info("  - Batch size: {}", batchSize);
        logger.info("  - Batch interval: {} seconds", batchIntervalSeconds);
        logger.info("  - Min risk level: {}", minRiskLevel);
        logger.info("  - Include categories: {}",
            includeCategoriesSet.isEmpty() ? "ALL" : includeCategoriesSet);
        logger.info("  - Failed events only: {}", failedEventsOnly);
        logger.info("  - Max retries: {}", maxRetries);

        if (webhookUrl == null || webhookUrl.trim().isEmpty()) {
            logger.warn("SIEM forwarding enabled but no webhook URL configured!");
        }
    }

    private boolean isValidFormat(String fmt) {
        return "json".equalsIgnoreCase(fmt) ||
               "cef".equalsIgnoreCase(fmt) ||
               "syslog".equalsIgnoreCase(fmt);
    }

    private boolean isValidRiskLevel(String level) {
        return "LOW".equalsIgnoreCase(level) ||
               "MEDIUM".equalsIgnoreCase(level) ||
               "HIGH".equalsIgnoreCase(level);
    }

    private String maskUrl(String url) {
        if (url == null || url.length() < 20) {
            return "***";
        }
        return url.substring(0, 20) + "***";
    }

    /**
     * Check if an event should be forwarded based on category filter
     */
    public boolean shouldForwardCategory(String category) {
        if (includeCategoriesSet.isEmpty()) {
            return true; // Forward all if no filter specified
        }
        return includeCategoriesSet.contains(category);
    }

    /**
     * Check if an event should be forwarded based on risk level
     */
    public boolean shouldForwardRiskLevel(String riskLevel) {
        int minLevel = riskLevelToInt(minRiskLevel);
        int eventLevel = riskLevelToInt(riskLevel);
        return eventLevel >= minLevel;
    }

    private int riskLevelToInt(String level) {
        if ("HIGH".equalsIgnoreCase(level)) return 3;
        if ("MEDIUM".equalsIgnoreCase(level)) return 2;
        return 1; // LOW or default
    }

    // Getters and Setters

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getWebhookUrl() {
        return webhookUrl;
    }

    public void setWebhookUrl(String webhookUrl) {
        this.webhookUrl = webhookUrl;
    }

    public String getAuthorizationHeader() {
        return authorizationHeader;
    }

    public void setAuthorizationHeader(String authorizationHeader) {
        this.authorizationHeader = authorizationHeader;
    }

    public String getAuthHeaderName() {
        return authHeaderName;
    }

    public void setAuthHeaderName(String authHeaderName) {
        this.authHeaderName = authHeaderName;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public int getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }

    public int getBatchIntervalSeconds() {
        return batchIntervalSeconds;
    }

    public void setBatchIntervalSeconds(int batchIntervalSeconds) {
        this.batchIntervalSeconds = batchIntervalSeconds;
    }

    public String getIncludeCategories() {
        return includeCategories;
    }

    public void setIncludeCategories(String includeCategories) {
        this.includeCategories = includeCategories;
    }

    public String getMinRiskLevel() {
        return minRiskLevel;
    }

    public void setMinRiskLevel(String minRiskLevel) {
        this.minRiskLevel = minRiskLevel;
    }

    public int getMaxRetries() {
        return maxRetries;
    }

    public void setMaxRetries(int maxRetries) {
        this.maxRetries = maxRetries;
    }

    public long getRetryDelayMs() {
        return retryDelayMs;
    }

    public void setRetryDelayMs(long retryDelayMs) {
        this.retryDelayMs = retryDelayMs;
    }

    public double getRetryBackoffMultiplier() {
        return retryBackoffMultiplier;
    }

    public void setRetryBackoffMultiplier(double retryBackoffMultiplier) {
        this.retryBackoffMultiplier = retryBackoffMultiplier;
    }

    public int getConnectionTimeoutMs() {
        return connectionTimeoutMs;
    }

    public void setConnectionTimeoutMs(int connectionTimeoutMs) {
        this.connectionTimeoutMs = connectionTimeoutMs;
    }

    public int getReadTimeoutMs() {
        return readTimeoutMs;
    }

    public void setReadTimeoutMs(int readTimeoutMs) {
        this.readTimeoutMs = readTimeoutMs;
    }

    public boolean isFailedEventsOnly() {
        return failedEventsOnly;
    }

    public void setFailedEventsOnly(boolean failedEventsOnly) {
        this.failedEventsOnly = failedEventsOnly;
    }

    public String getCefVendor() {
        return cefVendor;
    }

    public void setCefVendor(String cefVendor) {
        this.cefVendor = cefVendor;
    }

    public String getCefProduct() {
        return cefProduct;
    }

    public void setCefProduct(String cefProduct) {
        this.cefProduct = cefProduct;
    }

    public String getCefVersion() {
        return cefVersion;
    }

    public void setCefVersion(String cefVersion) {
        this.cefVersion = cefVersion;
    }

    public String getSyslogFacility() {
        return syslogFacility;
    }

    public void setSyslogFacility(String syslogFacility) {
        this.syslogFacility = syslogFacility;
    }

    public String getSyslogHostname() {
        return syslogHostname;
    }

    public void setSyslogHostname(String syslogHostname) {
        this.syslogHostname = syslogHostname;
    }

    public String getSyslogAppName() {
        return syslogAppName;
    }

    public void setSyslogAppName(String syslogAppName) {
        this.syslogAppName = syslogAppName;
    }

    public Set<String> getIncludeCategoriesSet() {
        return includeCategoriesSet;
    }
}
