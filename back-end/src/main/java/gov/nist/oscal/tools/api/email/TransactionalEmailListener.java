package gov.nist.oscal.tools.api.email;

import gov.nist.oscal.tools.api.entity.OrganizationMembership;
import gov.nist.oscal.tools.api.entity.OrganizationMembership.MembershipStatus;
import gov.nist.oscal.tools.api.entity.OrganizationMembership.OrganizationRole;
import gov.nist.oscal.tools.api.entity.User;
import gov.nist.oscal.tools.api.entity.UserAccessRequest;
import gov.nist.oscal.tools.api.repository.OrganizationMembershipRepository;
import gov.nist.oscal.tools.api.repository.UserAccessRequestRepository;
import gov.nist.oscal.tools.api.repository.UserRepository;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Sends onboarding emails AFTER the publishing transaction commits, on an
 * async executor, with one retry.
 *
 * <p>Why: these sends used to run synchronously inside {@code @Transactional}
 * service methods, so SendGrid latency extended DB transactions and a SendGrid
 * outage added its full timeout to every registration. After-commit also fixes
 * a correctness race for reset links: the email can no longer arrive before
 * the token row is committed.</p>
 *
 * <p>Failures are logged loudly but never propagate — the user-facing
 * operation has already committed. Admin-facing sends that need immediate
 * delivery feedback (invitations with their {@code emailSent} flag) stay
 * synchronous by design.</p>
 */
@Component
public class TransactionalEmailListener {

    private static final Logger logger = LoggerFactory.getLogger(TransactionalEmailListener.class);

    /** One retry after a short pause — enough for transient SendGrid blips. */
    private static final int MAX_ATTEMPTS = 2;
    private static final long RETRY_DELAY_MS = 2000;

    @Autowired private EmailService emailService;
    @Autowired private UserRepository userRepository;
    @Autowired private UserAccessRequestRepository accessRequestRepository;
    @Autowired private OrganizationMembershipRepository membershipRepository;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
    public void onWelcomeEmail(EmailEvents.WelcomeEmail event) {
        User user = userRepository.findById(event.userId()).orElse(null);
        if (user == null) {
            return;
        }
        sendWithRetry("welcome to " + user.getEmail(), () -> emailService.sendWelcome(user));
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
    public void onAccessRequestSubmitted(EmailEvents.AccessRequestSubmittedEmails event) {
        UserAccessRequest request = accessRequestRepository
                .findByIdWithRelations(event.requestId()).orElse(null);
        if (request == null) {
            return;
        }
        sendWithRetry("access-request ack to " + request.getEmail(),
                () -> emailService.sendAccessRequestAcknowledged(request));

        List<User> admins = membershipRepository
                .findByOrganizationIdAndRoleAndStatus(
                        request.getOrganization().getId(),
                        OrganizationRole.ORG_ADMIN,
                        MembershipStatus.ACTIVE)
                .stream()
                .map(OrganizationMembership::getUser)
                .collect(Collectors.toList());
        sendWithRetry("access-request admin notifications for request " + request.getId(),
                () -> emailService.sendAccessRequestPendingForAdmins(request, admins));
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
    public void onPasswordResetLink(EmailEvents.PasswordResetLinkEmail event) {
        User user = userRepository.findById(event.userId()).orElse(null);
        if (user == null) {
            return;
        }
        sendWithRetry("password-reset link to " + user.getEmail(),
                () -> emailService.sendPasswordResetLink(user, event.resetUrl(), event.ttlMinutes()));
    }

    private void sendWithRetry(String description, Runnable send) {
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                send.run();
                return;
            } catch (Exception e) {
                if (attempt < MAX_ATTEMPTS) {
                    logger.warn("Email send failed ({}), retrying: {}", description, e.getMessage());
                    try {
                        Thread.sleep(RETRY_DELAY_MS);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                } else {
                    logger.error("Email send FAILED after {} attempts ({}): {}",
                            MAX_ATTEMPTS, description, e.getMessage());
                }
            }
        }
    }
}
