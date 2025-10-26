package gov.nist.oscal.tools.api.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import jakarta.annotation.PostConstruct;

/**
 * Environment configuration validator
 * Validates critical configuration on application startup
 * Logs environment information for debugging
 */
@Configuration
public class EnvironmentConfig {

    private static final Logger logger = LoggerFactory.getLogger(EnvironmentConfig.class);

    private final Environment environment;

    @Value("${spring.profiles.active:dev}")
    private String activeProfile;

    @Value("${spring.application.name:oscal-cli-api}")
    private String applicationName;

    @Value("${server.port:8080}")
    private String serverPort;

    @Value("${spring.datasource.url:}")
    private String databaseUrl;

    @Value("${spring.h2.console.enabled:false}")
    private boolean h2ConsoleEnabled;

    @Value("${springdoc.swagger-ui.enabled:true}")
    private boolean swaggerEnabled;

    @Value("${security.headers.enabled:false}")
    private boolean securityHeadersEnabled;

    @Value("${security.require-https:false}")
    private boolean httpsRequired;

    @Value("${rate.limit.enabled:false}")
    private boolean rateLimitEnabled;

    @Value("${audit.logging.enabled:false}")
    private boolean auditLoggingEnabled;

    public EnvironmentConfig(Environment environment) {
        this.environment = environment;
    }

    @PostConstruct
    public void logEnvironmentInfo() {
        logger.info("================================================================================");
        logger.info("OSCAL CLI API - Environment Configuration");
        logger.info("================================================================================");
        logger.info("Application Name: {}", applicationName);
        logger.info("Active Profile: {}", activeProfile);
        logger.info("Server Port: {}", serverPort);
        logger.info("Database URL: {}", maskSensitiveInfo(databaseUrl));
        logger.info("--------------------------------------------------------------------------------");
        logger.info("Security Features:");
        logger.info("  H2 Console Enabled: {}", h2ConsoleEnabled);
        logger.info("  Swagger UI Enabled: {}", swaggerEnabled);
        logger.info("  Security Headers: {}", securityHeadersEnabled);
        logger.info("  HTTPS Required: {}", httpsRequired);
        logger.info("  Rate Limiting: {}", rateLimitEnabled);
        logger.info("  Audit Logging: {}", auditLoggingEnabled);
        logger.info("================================================================================");

        // Validate production environment
        validateProductionConfiguration();
    }

    /**
     * Validates that production environment has appropriate security settings
     */
    private void validateProductionConfiguration() {
        if ("prod".equalsIgnoreCase(activeProfile) || "production".equalsIgnoreCase(activeProfile)) {
            logger.info("Validating PRODUCTION environment configuration...");

            // Critical production checks
            if (h2ConsoleEnabled) {
                logger.error("SECURITY WARNING: H2 Console is ENABLED in PRODUCTION. This should be disabled.");
            }

            if (swaggerEnabled) {
                logger.warn("SECURITY WARNING: Swagger UI is ENABLED in PRODUCTION. " +
                        "Consider disabling or requiring authentication.");
            }

            if (!securityHeadersEnabled) {
                logger.warn("SECURITY WARNING: Security headers are DISABLED in PRODUCTION. " +
                        "Enable security headers for better protection.");
            }

            if (!httpsRequired) {
                logger.warn("SECURITY WARNING: HTTPS is not REQUIRED in PRODUCTION. " +
                        "Enable HTTPS to encrypt data in transit.");
            }

            if (!rateLimitEnabled) {
                logger.warn("SECURITY WARNING: Rate limiting is DISABLED in PRODUCTION. " +
                        "Enable rate limiting to prevent abuse.");
            }

            if (!auditLoggingEnabled) {
                logger.warn("SECURITY WARNING: Audit logging is DISABLED in PRODUCTION. " +
                        "Enable audit logging for security monitoring.");
            }

            // Validate database is not H2
            if (databaseUrl.contains("h2")) {
                logger.error("CRITICAL: H2 database detected in PRODUCTION. " +
                        "Use PostgreSQL or another production-grade database.");
            }

            logger.info("Production configuration validation complete.");
        } else if ("staging".equalsIgnoreCase(activeProfile)) {
            logger.info("Running in STAGING environment");
            if (h2ConsoleEnabled) {
                logger.warn("H2 Console is enabled in STAGING environment");
            }
        } else {
            logger.info("Running in DEVELOPMENT environment");
        }
    }

    /**
     * Masks sensitive information in database URLs for logging
     */
    private String maskSensitiveInfo(String url) {
        if (url == null || url.isEmpty()) {
            return "[not configured]";
        }
        // Mask password if present in URL
        return url.replaceAll("password=[^&;]+", "password=***");
    }

    public String getActiveProfile() {
        return activeProfile;
    }

    public boolean isProductionEnvironment() {
        return "prod".equalsIgnoreCase(activeProfile) || "production".equalsIgnoreCase(activeProfile);
    }

    public boolean isDevelopmentEnvironment() {
        return "dev".equalsIgnoreCase(activeProfile) || "development".equalsIgnoreCase(activeProfile);
    }

    public boolean isStagingEnvironment() {
        return "staging".equalsIgnoreCase(activeProfile);
    }
}
