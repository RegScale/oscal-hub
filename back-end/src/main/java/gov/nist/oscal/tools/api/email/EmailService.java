package gov.nist.oscal.tools.api.email;

import gov.nist.oscal.tools.api.entity.Invitation;
import gov.nist.oscal.tools.api.entity.Organization;
import gov.nist.oscal.tools.api.entity.User;
import gov.nist.oscal.tools.api.entity.UserAccessRequest;
import java.util.List;

public interface EmailService {
    void sendWelcome(User user);
    void sendAccessRequestAcknowledged(UserAccessRequest request);
    void sendAccessRequestPendingForAdmins(UserAccessRequest request, List<User> admins);
    void sendAccessRequestApproved(UserAccessRequest request, User approver);
    void sendAccessRequestRejected(UserAccessRequest request, User rejector, String reason);
    void sendInvitation(Invitation invitation, User inviter, Organization org);
    void sendPasswordReset(User user, String tempPassword, User adminWhoReset);
}
