package gov.nist.oscal.tools.api.crm;

import com.fasterxml.jackson.databind.JsonNode;
import gov.nist.oscal.tools.api.entity.Organization;
import gov.nist.oscal.tools.api.entity.User;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

/**
 * HubSpot implementation of {@link CrmService} using a Service Key (the
 * supported credential for single-account, system-to-system API access —
 * private apps are legacy) and the CRM v3/v4 REST APIs. Service Keys
 * authenticate with the standard {@code Authorization: Bearer} header.
 *
 * <ul>
 *   <li>Contacts are UPSERTED by email (batch-upsert with idProperty=email),
 *       so retries and duplicate signups with a shared email are idempotent.</li>
 *   <li>Companies are looked up by name first and only created when absent
 *       (org names are unique in OSCAL Hub, enforced case-insensitively).</li>
 *   <li>The owning contact is associated to the company with the default
 *       contact→company association.</li>
 * </ul>
 *
 * Only standard HubSpot properties are used (email, firstname, lastname,
 * company, plus lifecyclestage=lead), so no custom-property setup is required
 * in the HubSpot account.
 */
public class HubSpotCrmService implements CrmService {

    private static final Logger logger = LoggerFactory.getLogger(HubSpotCrmService.class);

    private final RestClient restClient;

    public HubSpotCrmService(String baseUrl, String serviceKey) {
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("Authorization", "Bearer " + serviceKey)
                .build();
    }

    // Visible for tests
    HubSpotCrmService(RestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    public void syncContact(User user, String source) {
        if (user.getEmail() == null || user.getEmail().isBlank()) {
            logger.warn("Skipping HubSpot contact sync for user {} — no email", user.getUsername());
            return;
        }
        upsertContact(user, source);
        logger.info("HubSpot: synced contact {} (source={})", user.getEmail(), source);
    }

    @Override
    public void syncOrganization(Organization organization, User owner) {
        String companyId = findCompanyIdByName(organization.getName());
        if (companyId == null) {
            companyId = createCompany(organization);
        }

        String contactId = upsertContact(owner, "self_serve_registration");
        if (companyId != null && contactId != null) {
            associateContactWithCompany(contactId, companyId);
        }
        logger.info("HubSpot: synced organization '{}' (companyId={})", organization.getName(), companyId);
    }

    /** @return the HubSpot contact id, or null if the response had none */
    private String upsertContact(User user, String source) {
        String firstName = user.getFirstName() != null && !user.getFirstName().isBlank()
                ? user.getFirstName() : user.getUsername();
        Map<String, Object> properties = new java.util.LinkedHashMap<>();
        properties.put("email", user.getEmail());
        properties.put("firstname", firstName);
        if (user.getLastName() != null && !user.getLastName().isBlank()) {
            properties.put("lastname", user.getLastName());
        }
        properties.put("lifecyclestage", "lead");
        // Standard properties only — HubSpot rejects the whole request on unknown
        // property names, and requiring custom-property setup would make the
        // integration fragile. The acquisition source is logged app-side.

        JsonNode response = restClient.post()
                .uri("/crm/v3/objects/contacts/batch/upsert")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("inputs", List.of(Map.of(
                        "idProperty", "email",
                        "id", user.getEmail(),
                        "properties", properties))))
                .retrieve()
                .body(JsonNode.class);

        logger.debug("HubSpot contact upsert for {} (source={})", user.getEmail(), source);
        if (response != null && response.path("results").isArray()
                && response.path("results").size() > 0) {
            return response.path("results").get(0).path("id").asText(null);
        }
        return null;
    }

    private String findCompanyIdByName(String name) {
        JsonNode response = restClient.post()
                .uri("/crm/v3/objects/companies/search")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("filterGroups", List.of(Map.of(
                        "filters", List.of(Map.of(
                                "propertyName", "name",
                                "operator", "EQ",
                                "value", name))))))
                .retrieve()
                .body(JsonNode.class);

        if (response != null && response.path("results").isArray()
                && response.path("results").size() > 0) {
            return response.path("results").get(0).path("id").asText(null);
        }
        return null;
    }

    private String createCompany(Organization organization) {
        Map<String, Object> properties = new java.util.LinkedHashMap<>();
        properties.put("name", organization.getName());
        if (organization.getDescription() != null && !organization.getDescription().isBlank()) {
            properties.put("description", organization.getDescription());
        }

        JsonNode response = restClient.post()
                .uri("/crm/v3/objects/companies")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("properties", properties))
                .retrieve()
                .body(JsonNode.class);

        return response != null ? response.path("id").asText(null) : null;
    }

    private void associateContactWithCompany(String contactId, String companyId) {
        restClient.put()
                .uri("/crm/v4/objects/contact/{contactId}/associations/default/company/{companyId}",
                        contactId, companyId)
                .retrieve()
                .toBodilessEntity();
    }
}
