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

    @Bean
    public EmailService emailService(
        @Value("${email.enabled:true}") boolean enabled,
        @Value("${email.sendgrid.api-key:}") String apiKey,
        @Value("${email.sendgrid.from-email}") String fromEmail,
        @Value("${email.sendgrid.from-name}") String fromName,
        @Value("${app.base-url}") String baseUrl,
        TemplateRenderer renderer,
        EmailAuditLogger audit
    ) {
        if (!enabled || apiKey == null || apiKey.isBlank()) {
            return new NoOpEmailService();
        }
        return new SendGridEmailService(apiKey, fromEmail, fromName, baseUrl, renderer, audit);
    }
}
