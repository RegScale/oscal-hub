package gov.nist.oscal.tools.api.config;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Audit Logging Configuration
 * <p>
 * Provides configuration for audit logging behavior, retention policies,
 * and output formatting.
 * </p>
 *
 * @see gov.nist.oscal.tools.api.service.AuditLogService
 */
@Configuration
@ConfigurationProperties(prefix = "audit.logging")
public class AuditLogConfig {

    private static final Logger logger = LoggerFactory.getLogger(AuditLogConfig.class);

    /**
     * Enable audit logging
     */
    private boolean enabled = true;

    /**
     * Log authentication events (login, logout, registration)
     */
    private boolean logAuthentication = true;

    /**
     * Log authorization events (access denied, permission changes)
     */
    private boolean logAuthorization = true;

    /**
     * Log data access events (file read, write, delete)
     */
    private boolean logDataAccess = true;

    /**
     * Log configuration changes
     */
    private boolean logConfiguration = true;

    /**
     * Log security events (account lockout, suspicious activity)
     */
    private boolean logSecurity = true;

    /**
     * Log system events (startup, shutdown, errors)
     */
    private boolean logSystem = true;

    /**
     * Also log events to application log (SLF4J)
     */
    private boolean logToApplicationLog = true;

    /**
     * Retention period for LOW risk events (days)
     */
    private int retentionLowRiskDays = 90;

    /**
     * Retention period for MEDIUM risk events (days)
     */
    private int retentionMediumRiskDays = 730; // 2 years

    /**
     * Retention period for HIGH risk events (days)
     */
    private int retentionHighRiskDays = 2555; // 7 years

    /**
     * Enable automatic cleanup of old events
     */
    private boolean autoCleanup = false;

    /**
     * Cleanup schedule (cron expression)
     * Default: daily at 2:00 AM
     */
    private String cleanupSchedule = "0 0 2 * * ?";

    /**
     * Maximum batch size for cleanup operations
     */
    private int cleanupBatchSize = 1000;

    @PostConstruct
    public void validateConfiguration() {
        logger.info("Initializing Audit Logging Configuration...");

        if (!enabled) {
            logger.warn("Audit logging is DISABLED. This is not recommended for production.");
            return;
        }

        logger.info("Audit logging enabled with the following settings:");
        logger.info("  - Authentication events: {}", logAuthentication);
        logger.info("  - Authorization events: {}", logAuthorization);
        logger.info("  - Data access events: {}", logDataAccess);
        logger.info("  - Configuration events: {}", logConfiguration);
        logger.info("  - Security events: {}", logSecurity);
        logger.info("  - System events: {}", logSystem);
        logger.info("  - Log to application log: {}", logToApplicationLog);
        logger.info("Retention policy:");
        logger.info("  - LOW risk: {} days", retentionLowRiskDays);
        logger.info("  - MEDIUM risk: {} days", retentionMediumRiskDays);
        logger.info("  - HIGH risk: {} days", retentionHighRiskDays);
        logger.info("  - Auto cleanup: {}", autoCleanup);

        if (autoCleanup) {
            logger.info("  - Cleanup schedule: {}", cleanupSchedule);
            logger.info("  - Cleanup batch size: {}", cleanupBatchSize);
        }
    }

    // Getters and Setters

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isLogAuthentication() {
        return logAuthentication;
    }

    public void setLogAuthentication(boolean logAuthentication) {
        this.logAuthentication = logAuthentication;
    }

    public boolean isLogAuthorization() {
        return logAuthorization;
    }

    public void setLogAuthorization(boolean logAuthorization) {
        this.logAuthorization = logAuthorization;
    }

    public boolean isLogDataAccess() {
        return logDataAccess;
    }

    public void setLogDataAccess(boolean logDataAccess) {
        this.logDataAccess = logDataAccess;
    }

    public boolean isLogConfiguration() {
        return logConfiguration;
    }

    public void setLogConfiguration(boolean logConfiguration) {
        this.logConfiguration = logConfiguration;
    }

    public boolean isLogSecurity() {
        return logSecurity;
    }

    public void setLogSecurity(boolean logSecurity) {
        this.logSecurity = logSecurity;
    }

    public boolean isLogSystem() {
        return logSystem;
    }

    public void setLogSystem(boolean logSystem) {
        this.logSystem = logSystem;
    }

    public boolean isLogToApplicationLog() {
        return logToApplicationLog;
    }

    public void setLogToApplicationLog(boolean logToApplicationLog) {
        this.logToApplicationLog = logToApplicationLog;
    }

    public int getRetentionLowRiskDays() {
        return retentionLowRiskDays;
    }

    public void setRetentionLowRiskDays(int retentionLowRiskDays) {
        this.retentionLowRiskDays = retentionLowRiskDays;
    }

    public int getRetentionMediumRiskDays() {
        return retentionMediumRiskDays;
    }

    public void setRetentionMediumRiskDays(int retentionMediumRiskDays) {
        this.retentionMediumRiskDays = retentionMediumRiskDays;
    }

    public int getRetentionHighRiskDays() {
        return retentionHighRiskDays;
    }

    public void setRetentionHighRiskDays(int retentionHighRiskDays) {
        this.retentionHighRiskDays = retentionHighRiskDays;
    }

    public boolean isAutoCleanup() {
        return autoCleanup;
    }

    public void setAutoCleanup(boolean autoCleanup) {
        this.autoCleanup = autoCleanup;
    }

    public String getCleanupSchedule() {
        return cleanupSchedule;
    }

    public void setCleanupSchedule(String cleanupSchedule) {
        this.cleanupSchedule = cleanupSchedule;
    }

    public int getCleanupBatchSize() {
        return cleanupBatchSize;
    }

    public void setCleanupBatchSize(int cleanupBatchSize) {
        this.cleanupBatchSize = cleanupBatchSize;
    }
}
