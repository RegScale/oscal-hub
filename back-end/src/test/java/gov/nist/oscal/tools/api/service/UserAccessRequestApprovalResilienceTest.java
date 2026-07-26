/*
 * SPDX-FileCopyrightText: none
 * SPDX-License-Identifier: CC0-1.0
 */

package gov.nist.oscal.tools.api.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import gov.nist.oscal.tools.api.email.EmailService;
import gov.nist.oscal.tools.api.entity.Organization;
import gov.nist.oscal.tools.api.entity.OrganizationMembership;
import gov.nist.oscal.tools.api.entity.OrganizationMembership.MembershipStatus;
import gov.nist.oscal.tools.api.entity.OrganizationMembership.OrganizationRole;
import gov.nist.oscal.tools.api.entity.User;
import gov.nist.oscal.tools.api.entity.UserAccessRequest;
import gov.nist.oscal.tools.api.entity.UserAccessRequest.RequestStatus;
import gov.nist.oscal.tools.api.repository.OrganizationMembershipRepository;
import gov.nist.oscal.tools.api.repository.OrganizationRepository;
import gov.nist.oscal.tools.api.repository.UserAccessRequestRepository;
import gov.nist.oscal.tools.api.repository.UserRepository;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

/**
 * Phase-1 resilience regressions for access-request approval:
 * duplicate (non-unique) emails must not crash the flow, and approving a
 * request for someone with an inactive membership must not silently leave
 * them locked out.
 */
@SpringBootTest
@Transactional
class UserAccessRequestApprovalResilienceTest {

    @Autowired UserAccessRequestService service;
    @Autowired UserAccessRequestRepository requestRepo;
    @Autowired UserRepository userRepo;
    @Autowired OrganizationRepository orgRepo;
    @Autowired OrganizationMembershipRepository memRepo;
    @Autowired PasswordEncoder passwordEncoder;
    @MockitoBean EmailService email;

    @Test
    void approveWithDuplicateEmailAndNoUsernameMatchGivesClearError() {
        Organization org = makeOrg();
        User reviewer = makeUser("reviewer");
        String shared = "shared-" + System.nanoTime() + "@example.com";
        User a = makeUser("appr-a");
        a.setEmail(shared);
        userRepo.save(a);
        User b = makeUser("appr-b");
        b.setEmail(shared);
        userRepo.save(b);

        UserAccessRequest req = makeRequest(org, shared, null);

        RuntimeException ex = assertThrows(RuntimeException.class,
            () -> service.approveRequest(req.getId(), reviewer.getId(), null));
        assertTrue(ex.getMessage().contains("more than one existing account"));
    }

    @Test
    void approveReactivatesDeactivatedMembership() {
        Organization org = makeOrg();
        User reviewer = makeUser("reviewer");
        User returning = makeUser("returning");
        OrganizationMembership membership =
            new OrganizationMembership(returning, org, OrganizationRole.USER);
        membership.setStatus(MembershipStatus.DEACTIVATED);
        memRepo.save(membership);

        UserAccessRequest req = makeRequest(org, returning.getEmail(), returning.getUsername());
        UserAccessRequest approved = service.approveRequest(req.getId(), reviewer.getId(), "welcome back");

        assertEquals(RequestStatus.APPROVED, approved.getStatus());
        OrganizationMembership reloaded =
            memRepo.findByUserIdAndOrganizationId(returning.getId(), org.getId()).orElseThrow();
        assertEquals(MembershipStatus.ACTIVE, reloaded.getStatus());
    }

    @Test
    void approveWithLockedMembershipThrowsAndLeavesRequestPending() {
        Organization org = makeOrg();
        User reviewer = makeUser("reviewer");
        User locked = makeUser("locked");
        OrganizationMembership membership =
            new OrganizationMembership(locked, org, OrganizationRole.USER);
        membership.setStatus(MembershipStatus.LOCKED);
        memRepo.save(membership);

        UserAccessRequest req = makeRequest(org, locked.getEmail(), locked.getUsername());

        RuntimeException ex = assertThrows(RuntimeException.class,
            () -> service.approveRequest(req.getId(), reviewer.getId(), null));
        assertTrue(ex.getMessage().contains("locked"));
        assertEquals(RequestStatus.PENDING,
            requestRepo.findById(req.getId()).orElseThrow().getStatus());
    }

    // --- helpers ---

    private Organization makeOrg() {
        Organization o = new Organization();
        o.setName("Org-" + System.nanoTime());
        o.setActive(true);
        o.setCreatedAt(LocalDateTime.now());
        return orgRepo.save(o);
    }

    private User makeUser(String prefix) {
        User u = new User();
        u.setUsername(prefix + "-" + System.nanoTime());
        u.setEmail(prefix + "-" + System.nanoTime() + "@example.com");
        u.setPassword(passwordEncoder.encode("CorrectH0rse!Batt"));
        u.setEnabled(true);
        return userRepo.save(u);
    }

    private UserAccessRequest makeRequest(Organization org, String emailAddr, String username) {
        UserAccessRequest r = new UserAccessRequest();
        r.setOrganization(org);
        r.setEmail(emailAddr);
        r.setUsername(username);
        r.setStatus(RequestStatus.PENDING);
        r.setRequestDate(LocalDateTime.now());
        return requestRepo.save(r);
    }
}
