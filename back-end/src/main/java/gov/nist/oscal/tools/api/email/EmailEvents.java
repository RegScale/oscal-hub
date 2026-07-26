package gov.nist.oscal.tools.api.email;

/**
 * Events for emails that must be sent AFTER the surrounding transaction
 * commits, off the request thread (see {@link TransactionalEmailListener}).
 *
 * Events carry IDs (plus values that cannot be re-derived, like a raw reset
 * URL) — entities are reloaded in the listener's own transaction to avoid
 * lazy-loading detached objects on the async thread.
 */
public final class EmailEvents {

    private EmailEvents() {}

    /** Welcome email after successful registration. */
    public record WelcomeEmail(Long userId) {}

    /** Acknowledgment to the requester + notification to org admins. */
    public record AccessRequestSubmittedEmails(Long requestId) {}

    /** Self-serve password-reset link (the raw URL exists only here and in the email). */
    public record PasswordResetLinkEmail(Long userId, String resetUrl, int ttlMinutes) {}
}
