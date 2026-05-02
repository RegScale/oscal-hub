package gov.nist.oscal.tools.api.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import gov.nist.oscal.tools.api.email.EmailService;
import gov.nist.oscal.tools.api.entity.Organization;
import gov.nist.oscal.tools.api.entity.OrganizationMembership;
import gov.nist.oscal.tools.api.entity.User;
import gov.nist.oscal.tools.api.entity.UserAccessRequest;
import gov.nist.oscal.tools.api.model.RequestAccessRequest;
import gov.nist.oscal.tools.api.repository.OrganizationMembershipRepository;
import gov.nist.oscal.tools.api.repository.OrganizationRepository;
import gov.nist.oscal.tools.api.repository.UserAccessRequestRepository;
import gov.nist.oscal.tools.api.repository.UserRepository;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class AccessRequestEmailTriggerTest {

    @Autowired
    AuthService authService;

    @Autowired
    UserAccessRequestService accessRequestService;

    @Autowired
    OrganizationRepository orgRepo;

    @Autowired
    OrganizationMembershipRepository memRepo;

    @Autowired
    UserRepository userRepo;

    @Autowired
    UserAccessRequestRepository requestRepo;

    @Autowired
    PasswordEncoder passwordEncoder;

    @MockitoBean
    EmailService emailService;

    // =====================================================================
    // Test 1: submitting an access request triggers acknowledged + admins emails
    // =====================================================================

    @Test
    void requestAccessFiresAcknowledgedAndAdminEmails() {
        Organization org = makeOrg("Acme-" + System.nanoTime());
        User admin = makeAdminFor(org);

        RequestAccessRequest req = new RequestAccessRequest();
        req.setEmail("requester-" + System.nanoTime() + "@example.com");
        req.setFirstName("Pat");
        req.setLastName("Doe");
        req.setOrganizationId(org.getId());
        // username is optional in RequestAccessRequest; leave it null

        authService.requestAccess(req);

        verify(emailService, times(1)).sendAccessRequestAcknowledged(any(UserAccessRequest.class));
        verify(emailService, times(1)).sendAccessRequestPendingForAdmins(any(UserAccessRequest.class), anyList());
    }

    // =====================================================================
    // Test 2: approving a pending request triggers approved email
    // =====================================================================

    @Test
    void approveFiresApprovedEmail() {
        Organization org = makeOrg("Beta-" + System.nanoTime());
        User admin = makeAdminFor(org);

        // Create a pending access request with a unique username so a new user can be created
        String uniqueSuffix = String.valueOf(System.nanoTime());
        UserAccessRequest pendingRequest = new UserAccessRequest();
        pendingRequest.setOrganization(org);
        pendingRequest.setEmail("approve-requester-" + uniqueSuffix + "@example.com");
        pendingRequest.setFirstName("Alex");
        pendingRequest.setLastName("Smith");
        pendingRequest.setUsername("approve-user-" + uniqueSuffix);
        pendingRequest.setStatus(UserAccessRequest.RequestStatus.PENDING);
        pendingRequest.setRequestDate(LocalDateTime.now());
        pendingRequest = requestRepo.save(pendingRequest);

        accessRequestService.approveRequest(pendingRequest.getId(), admin.getId(), "Approved in test");

        verify(emailService, times(1)).sendAccessRequestApproved(
                any(UserAccessRequest.class),
                any(User.class));
    }

    // =====================================================================
    // Test 3: rejecting a pending request triggers rejected email with reason
    // =====================================================================

    @Test
    void rejectFiresRejectedEmail() {
        Organization org = makeOrg("Gamma-" + System.nanoTime());
        User admin = makeAdminFor(org);

        String uniqueSuffix = String.valueOf(System.nanoTime());
        UserAccessRequest pendingRequest = new UserAccessRequest();
        pendingRequest.setOrganization(org);
        pendingRequest.setEmail("reject-requester-" + uniqueSuffix + "@example.com");
        pendingRequest.setFirstName("Dana");
        pendingRequest.setLastName("Jones");
        pendingRequest.setUsername("reject-user-" + uniqueSuffix);
        pendingRequest.setStatus(UserAccessRequest.RequestStatus.PENDING);
        pendingRequest.setRequestDate(LocalDateTime.now());
        pendingRequest = requestRepo.save(pendingRequest);

        String reason = "Does not meet eligibility requirements";
        accessRequestService.rejectRequest(pendingRequest.getId(), admin.getId(), reason);

        verify(emailService, times(1)).sendAccessRequestRejected(
                any(UserAccessRequest.class),
                any(User.class),
                eq(reason));
    }

    // =====================================================================
    // Fixture helpers
    // =====================================================================

    private Organization makeOrg(String name) {
        Organization o = new Organization();
        o.setName(name);
        o.setActive(true);
        o.setCreatedAt(LocalDateTime.now());
        return orgRepo.save(o);
    }

    private User makeAdminFor(Organization org) {
        String suffix = String.valueOf(System.nanoTime());
        User u = new User();
        u.setUsername("admin-" + suffix);
        u.setEmail("admin-" + suffix + "@example.com");
        u.setPassword(passwordEncoder.encode("CorrectHorse123!"));
        u.setEnabled(true);
        u = userRepo.save(u);

        OrganizationMembership m = new OrganizationMembership();
        m.setUser(u);
        m.setOrganization(org);
        m.setRole(OrganizationMembership.OrganizationRole.ORG_ADMIN);
        m.setStatus(OrganizationMembership.MembershipStatus.ACTIVE);
        memRepo.save(m);
        return u;
    }
}
