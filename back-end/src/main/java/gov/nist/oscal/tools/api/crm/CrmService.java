package gov.nist.oscal.tools.api.crm;

import gov.nist.oscal.tools.api.entity.Organization;
import gov.nist.oscal.tools.api.entity.User;

/**
 * Marketing-CRM integration boundary. Implementations register new OSCAL Hub
 * users and organizations in the marketing database (HubSpot in production;
 * a no-op when unconfigured).
 *
 * Calls are made only AFTER the signup transaction commits, on an async
 * executor (see {@link CrmSyncListener}) — CRM latency or outages must never
 * affect onboarding.
 */
public interface CrmService {

    /**
     * Register (or update) a contact for a newly created user account.
     *
     * @param user   the newly created user
     * @param source which flow created the account (self_serve_registration,
     *               invitation, access_request_approval) — recorded so marketing
     *               can segment by acquisition path
     */
    void syncContact(User user, String source);

    /**
     * Register a newly signed-up organization as a company, associated with
     * the owning contact.
     */
    void syncOrganization(Organization organization, User owner);
}
