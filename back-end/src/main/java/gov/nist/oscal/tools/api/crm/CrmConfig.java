package gov.nist.oscal.tools.api.crm;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CrmConfig {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(CrmConfig.class);

    @Bean
    public CrmService crmService(
            @Value("${hubspot.enabled:true}") boolean enabled,
            @Value("${hubspot.service-key:}") String serviceKey,
            @Value("${hubspot.base-url:https://api.hubapi.com}") String baseUrl
    ) {
        if (!enabled || serviceKey == null || serviceKey.isBlank()) {
            // Unlike email, a missing CRM sync loses marketing leads, not user
            // functionality — an INFO note suffices at any profile.
            logger.info("HubSpot CRM sync disabled ({}) — new users/orgs will not be "
                    + "registered in the marketing database. Set HUBSPOT_SERVICE_KEY to enable.",
                    !enabled ? "hubspot.enabled=false" : "no service key");
            return new NoOpCrmService();
        }
        logger.info("HubSpot CRM sync enabled ({})", baseUrl);
        return new HubSpotCrmService(baseUrl, serviceKey);
    }
}
