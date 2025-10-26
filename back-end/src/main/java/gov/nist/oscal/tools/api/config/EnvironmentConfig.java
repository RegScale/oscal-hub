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
     * Counts warnings and errors for summary
     */
    private void validateProductionConfiguration() {
        if ("prod".equalsIgnoreCase(activeProfile) || "production".equalsIgnoreCase(activeProfile)) {
            logger.info("================================================================================");
            logger.info("PRODUCTION ENVIRONMENT - Security Configuration Validation");
            logger.info("================================================================================");

            int criticalIssues = 0;
            int warnings = 0;

            // Critical production checks
            if (h2ConsoleEnabled) {
                logger.error("❌ CRITICAL: H2 Console is ENABLED in PRODUCTION");
                logger.error("   Risk: Exposes database structure and allows direct SQL execution");
                logger.error("   Fix: Set spring.h2.console.enabled=false");
                criticalIssues++;
            } else {
                logger.info("✓ H2 Console disabled");
            }

            if (swaggerEnabled) {
                logger.warn("⚠ WARNING: Swagger UI is ENABLED in PRODUCTION");
                logger.warn("   Risk: Exposes API structure and endpoints");
                logger.warn("   Fix: Set springdoc.swagger-ui.enabled=false or require authentication");
                warnings++;
            } else {
                logger.info("✓ Swagger UI disabled");
            }

            if (!securityHeadersEnabled) {
                logger.warn("⚠ WARNING: Security headers are DISABLED");
                logger.warn("   Risk: Vulnerable to XSS, clickjacking, MIME sniffing");
                logger.warn("   Fix: Set security.headers.enabled=true");
                warnings++;
            } else {
                logger.info("✓ Security headers enabled");
            }

            if (!httpsRequired) {
                logger.warn("⚠ WARNING: HTTPS is not REQUIRED");
                logger.warn("   Risk: Data transmitted in cleartext, vulnerable to interception");
                logger.warn("   Fix: Set security.require-https=true and configure SSL");
                warnings++;
            } else {
                logger.info("✓ HTTPS required");
            }

            if (!rateLimitEnabled) {
                logger.warn("⚠ WARNING: Rate limiting is DISABLED");
                logger.warn("   Risk: Vulnerable to brute force and DoS attacks");
                logger.warn("   Fix: Set rate.limit.enabled=true");
                warnings++;
            } else {
                logger.info("✓ Rate limiting enabled");
            }

            if (!auditLoggingEnabled) {
                logger.warn("⚠ WARNING: Audit logging is DISABLED");
                logger.warn("   Risk: Cannot detect or investigate security incidents");
                logger.warn("   Fix: Set audit.logging.enabled=true");
                warnings++;
            } else {
                logger.info("✓ Audit logging enabled");
            }

            // Validate database is not H2
            if (databaseUrl.contains("h2")) {
                logger.error("❌ CRITICAL: H2 database detected in PRODUCTION");
                logger.error("   Risk: H2 is not production-grade, lacks enterprise features");
                logger.error("   Fix: Use PostgreSQL, MySQL, or another production database");
                criticalIssues++;
            } else {
                logger.info("✓ Production-grade database configured");
            }

            logger.info("================================================================================");
            logger.info("Security Validation Summary:");
            logger.info("  Critical Issues: {}", criticalIssues);
            logger.info("  Warnings: {}", warnings);

            if (criticalIssues > 0) {
                logger.error("❌ PRODUCTION DEPLOYMENT BLOCKED: {} critical security issues found", criticalIssues);
                logger.error("Fix critical issues before deploying to production");
                throw new IllegalStateException(
                        String.format("Production deployment blocked: %d critical security issues", criticalIssues));
            } else if (warnings > 0) {
                logger.warn("⚠ Production deployment allowed with {} warnings", warnings);
                logger.warn("Recommended: Address all warnings before production use");
            } else {
                logger.info("✓ Production security configuration validated successfully");
            }
            logger.info("================================================================================");

        } else if ("staging".equalsIgnoreCase(activeProfile)) {
            logger.info("================================================================================");
            logger.info("STAGING ENVIRONMENT");
            logger.info("================================================================================");
            if (h2ConsoleEnabled) {
                logger.warn("⚠ H2 Console enabled in STAGING (use for debugging only)");
            }
            if (swaggerEnabled) {
                logger.info("Swagger UI enabled for API testing");
            }
            logger.info("================================================================================");
        } else {
            logger.info("================================================================================");
            logger.info("DEVELOPMENT ENVIRONMENT");
            logger.info("================================================================================");
            logger.info("Running in development mode - security restrictions relaxed");
            if (h2ConsoleEnabled) {
                logger.info("H2 Console available at: /h2-console");
            }
            if (swaggerEnabled) {
                logger.info("Swagger UI available at: /swagger-ui.html");
            }
            logger.info("================================================================================");
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
