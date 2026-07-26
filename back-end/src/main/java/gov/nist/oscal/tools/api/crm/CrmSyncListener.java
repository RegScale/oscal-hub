package gov.nist.oscal.tools.api.crm;

import gov.nist.oscal.tools.api.entity.Organization;
import gov.nist.oscal.tools.api.entity.User;
import gov.nist.oscal.tools.api.repository.OrganizationRepository;
import gov.nist.oscal.tools.api.repository.UserRepository;
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
 * Pushes new users and organizations to the marketing CRM AFTER the signup
 * transaction commits, on an async executor, with one retry — mirrors
 * {@code TransactionalEmailListener}. CRM latency or outages must never slow
 * or fail onboarding; failures are logged loudly and dropped.
 */
@Component
public class CrmSyncListener {

    private static final Logger logger = LoggerFactory.getLogger(CrmSyncListener.class);

    private static final int MAX_ATTEMPTS = 2;
    private static final long RETRY_DELAY_MS = 2000;

    @Autowired private CrmService crmService;
    @Autowired private UserRepository userRepository;
    @Autowired private OrganizationRepository organizationRepository;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
    public void onContactRegistered(CrmEvents.ContactRegistered event) {
        User user = userRepository.findById(event.userId()).orElse(null);
        if (user == null) {
            return;
        }
        syncWithRetry("contact " + user.getEmail(),
                () -> crmService.syncContact(user, event.source()));
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
    public void onOrganizationSignedUp(CrmEvents.OrganizationSignedUp event) {
        Organization organization = organizationRepository.findById(event.organizationId()).orElse(null);
        User owner = userRepository.findById(event.ownerUserId()).orElse(null);
        if (organization == null || owner == null) {
            return;
        }
        syncWithRetry("organization '" + organization.getName() + "'",
                () -> crmService.syncOrganization(organization, owner));
    }

    private void syncWithRetry(String description, Runnable sync) {
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                sync.run();
                return;
            } catch (Exception e) {
                if (attempt < MAX_ATTEMPTS) {
                    logger.warn("CRM sync failed ({}), retrying: {}", description, e.getMessage());
                    try {
                        Thread.sleep(RETRY_DELAY_MS);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                } else {
                    logger.error("CRM sync FAILED after {} attempts ({}): {}",
                            MAX_ATTEMPTS, description, e.getMessage());
                }
            }
        }
    }
}
