package gov.nist.oscal.tools.api.config;

import gov.nist.oscal.tools.api.email.EmailAuditLogger;
import gov.nist.oscal.tools.api.email.EmailService;
import gov.nist.oscal.tools.api.email.NoOpEmailService;
import gov.nist.oscal.tools.api.email.SendGridEmailService;
import gov.nist.oscal.tools.api.email.TemplateRenderer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EmailConfig {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(EmailConfig.class);

    @Bean
    public EmailService emailService(
        @Value("${email.enabled:true}") boolean enabled,
        @Value("${email.sendgrid.api-key:}") String apiKey,
        @Value("${email.sendgrid.from-email}") String fromEmail,
        @Value("${email.sendgrid.from-name}") String fromName,
        @Value("${app.base-url}") String baseUrl,
        @Value("${app.support.email}") String supportEmail,
        @Value("${spring.profiles.active:dev}") String activeProfile,
        TemplateRenderer renderer,
        EmailAuditLogger audit
    ) {
        if (!enabled || apiKey == null || apiKey.isBlank()) {
            String reason = !enabled ? "email.enabled=false" : "email.sendgrid.api-key is not set";
            if ("dev".equalsIgnoreCase(activeProfile) || "test".equalsIgnoreCase(activeProfile)) {
                logger.info("Email delivery disabled ({}) — using NoOpEmailService", reason);
            } else {
                // Outside dev this silently blackholes invitations, password resets,
                // and temp-password emails — make it impossible to miss in the logs.
                logger.error("==============================================================================");
                logger.error("EMAIL DELIVERY IS DISABLED ({}) on profile '{}'.", reason, activeProfile);
                logger.error("Invitations, password resets, and approval notifications WILL NOT BE SENT.");
                logger.error("Set EMAIL_SENDGRID_API_KEY (and email.enabled=true) to enable delivery.");
                logger.error("==============================================================================");
            }
            return new NoOpEmailService();
        }
        return new SendGridEmailService(apiKey, fromEmail, fromName, baseUrl, supportEmail, renderer, audit);
    }
}
