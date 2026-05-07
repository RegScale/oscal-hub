package gov.nist.oscal.tools.api.email;

import gov.nist.oscal.tools.api.entity.Invitation;
import gov.nist.oscal.tools.api.entity.Organization;
import gov.nist.oscal.tools.api.entity.User;
import gov.nist.oscal.tools.api.entity.UserAccessRequest;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NoOpEmailService implements EmailService {

    private static final Logger logger = LoggerFactory.getLogger(NoOpEmailService.class);

    private void log(String template, String to) {
        logger.info("[email-noop] would have sent template={} to={}", template, to);
    }

    @Override public void sendWelcome(User user) { log("welcome", user.getEmail()); }
    @Override public void sendAccessRequestAcknowledged(UserAccessRequest r) { log("access-request-acknowledged", r.getEmail()); }
    @Override public void sendAccessRequestPendingForAdmins(UserAccessRequest r, List<User> admins) {
        admins.forEach(a -> log("access-request-pending-admin", a.getEmail()));
    }
    @Override public void sendAccessRequestApproved(UserAccessRequest r, User approver) { log("access-request-approved", r.getEmail()); }
    @Override public void sendAccessRequestRejected(UserAccessRequest r, User rejector, String reason) { log("access-request-rejected", r.getEmail()); }
    @Override public void sendInvitation(Invitation inv, User inviter, Organization org) { log("invitation", inv.getEmail()); }
    @Override public void sendPasswordReset(User user, String tempPassword, User adminWhoReset) { log("password-reset", user.getEmail()); }
}
