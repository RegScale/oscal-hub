package gov.nist.oscal.tools.api.crm;

import gov.nist.oscal.tools.api.entity.Organization;
import gov.nist.oscal.tools.api.entity.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Active when no HubSpot token is configured — logs instead of syncing. */
public class NoOpCrmService implements CrmService {

    private static final Logger logger = LoggerFactory.getLogger(NoOpCrmService.class);

    @Override
    public void syncContact(User user, String source) {
        logger.info("[crm-noop] would have synced contact {} (source={})", user.getEmail(), source);
    }

    @Override
    public void syncOrganization(Organization organization, User owner) {
        logger.info("[crm-noop] would have synced organization '{}'", organization.getName());
    }
}
