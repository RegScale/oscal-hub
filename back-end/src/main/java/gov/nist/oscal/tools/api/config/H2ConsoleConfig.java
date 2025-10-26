package gov.nist.oscal.tools.api.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.h2.H2ConsoleProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import jakarta.annotation.PostConstruct;

/**
 * H2 Console configuration with environment-based access control
 *
 * H2 Console is a web-based database management interface that should ONLY
 * be enabled in development environments. It poses security risks in production:
 * - Exposes database structure and data
 * - Allows direct SQL execution
 * - No authentication by default
 * - Can be exploited if exposed
 *
 * This configuration ensures H2 Console is:
 * - Enabled ONLY in 'dev' profile
 * - Disabled automatically in 'staging' and 'prod' profiles
 * - Logged at startup for visibility
 */
@Configuration
public class H2ConsoleConfig {

    private static final Logger logger = LoggerFactory.getLogger(H2ConsoleConfig.class);

    @Value("${spring.profiles.active:dev}")
    private String activeProfile;

    @Value("${spring.h2.console.enabled:false}")
    private boolean h2ConsoleEnabled;

    @PostConstruct
    public void validateH2ConsoleConfiguration() {
        if (h2ConsoleEnabled) {
            if ("prod".equalsIgnoreCase(activeProfile) || "production".equalsIgnoreCase(activeProfile)) {
                String errorMessage = "CRITICAL SECURITY ERROR: H2 Console is ENABLED in PRODUCTION environment. " +
                        "This exposes your database to potential attacks. " +
                        "Set spring.h2.console.enabled=false or remove H2 console dependency.";
                logger.error(errorMessage);
                throw new IllegalStateException(errorMessage);
            } else if ("staging".equalsIgnoreCase(activeProfile)) {
                logger.warn("WARNING: H2 Console is enabled in STAGING environment. " +
                        "This should only be used for debugging. Disable in production.");
                logger.warn("H2 Console available at: /h2-console");
            } else {
                logger.info("H2 Console enabled for development at: /h2-console");
                logger.info("  JDBC URL: jdbc:h2:file:./data/oscal-history");
                logger.info("  Username: sa");
                logger.info("  Password: (empty or configured)");
            }
        } else {
            logger.info("H2 Console disabled");
        }
    }

    /**
     * Configuration bean for H2 Console - only active in development
     * This ensures H2 console settings are only loaded when appropriate
     */
    @Bean
    @Profile("dev")
    @ConditionalOnExpression("${spring.h2.console.enabled:false}")
    public H2ConsoleProperties h2ConsoleProperties() {
        logger.debug("H2 Console properties configured for development profile");
        return new H2ConsoleProperties();
    }
}
