package gov.nist.oscal.tools.api.crm;

/**
 * Events for marketing-CRM sync, published inside the signup transactions and
 * handled AFTER COMMIT by {@link CrmSyncListener} (same pattern as
 * {@code EmailEvents}). Carry IDs only — entities are reloaded in the
 * listener's own transaction.
 */
public final class CrmEvents {

    private CrmEvents() {}

    /** A new user account was created (any onboarding path). */
    public record ContactRegistered(Long userId, String source) {}

    /** A new organization was signed up self-serve by its owning user. */
    public record OrganizationSignedUp(Long organizationId, Long ownerUserId) {}
}
