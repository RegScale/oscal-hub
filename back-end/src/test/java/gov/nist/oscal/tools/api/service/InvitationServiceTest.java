package gov.nist.oscal.tools.api.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import gov.nist.oscal.tools.api.email.EmailService;
import gov.nist.oscal.tools.api.entity.Invitation;
import gov.nist.oscal.tools.api.entity.Organization;
import gov.nist.oscal.tools.api.entity.OrganizationMembership;
import gov.nist.oscal.tools.api.entity.User;
import gov.nist.oscal.tools.api.exception.InvitationExpiredException;
import gov.nist.oscal.tools.api.exception.UserAlreadyMemberException;
import gov.nist.oscal.tools.api.repository.InvitationRepository;
import gov.nist.oscal.tools.api.repository.OrganizationMembershipRepository;
import gov.nist.oscal.tools.api.repository.OrganizationRepository;
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
@org.springframework.test.context.event.RecordApplicationEvents
class InvitationServiceTest {

    @Autowired org.springframework.test.context.event.ApplicationEvents applicationEvents;
    @Autowired InvitationService service;
    @Autowired InvitationRepository invRepo;
    @Autowired UserRepository userRepo;
    @Autowired OrganizationRepository orgRepo;
    @Autowired OrganizationMembershipRepository memRepo;
    @Autowired PasswordEncoder passwordEncoder;
    @MockitoBean EmailService email;

    @Test
    void createInvitationPersistsAndSendsEmail() {
        Organization org = makeOrg();
        User admin = makeUser("admin");
        Invitation inv = service.createInvitation(org.getId(), "teammate-" + System.nanoTime() + "@example.com",
            Invitation.Role.USER, admin);

        assertNotNull(inv.getId());
        assertEquals(Invitation.Status.PENDING, inv.getStatus());
        assertNotNull(inv.getToken());
        verify(email, times(1))
            .sendInvitation(any(), eq(admin), eq(org));
    }

    @Test
    void reInviteRevokesPriorPending() {
        Organization org = makeOrg();
        User admin = makeUser("admin");
        String emailAddr = "x-" + System.nanoTime() + "@example.com";
        Invitation first = service.createInvitation(org.getId(), emailAddr, Invitation.Role.USER, admin);
        Invitation second = service.createInvitation(org.getId(), emailAddr, Invitation.Role.USER, admin);

        Invitation reloaded = invRepo.findById(first.getId()).orElseThrow();
        assertEquals(Invitation.Status.REVOKED, reloaded.getStatus());
        assertEquals(Invitation.Status.PENDING, second.getStatus());
    }

    @Test
    void invitingExistingActiveMemberThrows() {
        Organization org = makeOrg();
        User admin = makeUser("admin");
        User existing = makeUser("already");
        // existing email matches what we'll invite
        existing.setEmail("already-" + System.nanoTime() + "@example.com");
        userRepo.save(existing);
        memRepo.save(new OrganizationMembership(existing, org, OrganizationMembership.OrganizationRole.USER));

        assertThrows(UserAlreadyMemberException.class,
            () -> service.createInvitation(org.getId(), existing.getEmail(),
                Invitation.Role.USER, admin));
    }

    @Test
    void acceptInvitationCreatesUserAndMembership() {
        Organization org = makeOrg();
        User admin = makeUser("admin");
        Invitation inv = service.createInvitation(org.getId(), "new-" + System.nanoTime() + "@example.com",
            Invitation.Role.USER, admin);

        User accepted = service.acceptInvitation(inv.getToken(), "newuser-" + System.nanoTime(), "CorrectH0rse!Batt", null);

        assertEquals(Invitation.Status.ACCEPTED, invRepo.findById(inv.getId()).orElseThrow().getStatus());
        assertNotNull(memRepo.findByUserIdAndOrganizationId(accepted.getId(), org.getId()));
    }

    @Test
    void acceptExpiredInvitationThrows() {
        Organization org = makeOrg();
        User admin = makeUser("admin");
        Invitation inv = service.createInvitation(org.getId(), "late-" + System.nanoTime() + "@example.com",
            Invitation.Role.USER, admin);
        inv.setExpiresAt(LocalDateTime.now().minusDays(1));
        invRepo.save(inv);

        assertThrows(InvitationExpiredException.class,
            () -> service.acceptInvitation(inv.getToken(), "u-" + System.nanoTime(), "CorrectH0rse!Batt", null));
    }

    // ------------------------------------------------------------------
    // Resilience regressions (Phase 1): duplicate emails, idempotency,
    // username collisions, inactive memberships
    // ------------------------------------------------------------------

    @Test
    void acceptWithDuplicateEmailAccountsGivesClearErrorInsteadOfCrashing() {
        // Emails are not unique; an Optional-based lookup used to throw
        // IncorrectResultSizeDataAccessException here (500 in prod).
        Organization org = makeOrg();
        User admin = makeUser("admin");
        String shared = "shared-" + System.nanoTime() + "@example.com";
        User a = makeUser("dup-a");
        a.setEmail(shared);
        userRepo.save(a);
        User b = makeUser("dup-b");
        b.setEmail(shared);
        userRepo.save(b);

        Invitation inv = service.createInvitation(org.getId(), shared, Invitation.Role.USER, admin);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
            () -> service.acceptInvitation(inv.getToken(), null, null, null));
        assertTrue(ex.getMessage().contains("sign in"));
    }

    @Test
    void createInvitationToleratesDuplicateEmailAccounts() {
        Organization org = makeOrg();
        User admin = makeUser("admin");
        String shared = "shared-c-" + System.nanoTime() + "@example.com";
        User a = makeUser("dupc-a");
        a.setEmail(shared);
        userRepo.save(a);
        User b = makeUser("dupc-b");
        b.setEmail(shared);
        userRepo.save(b);
        // one of the duplicate accounts is already an active member
        memRepo.save(new OrganizationMembership(b, org, OrganizationMembership.OrganizationRole.USER));

        assertThrows(UserAlreadyMemberException.class,
            () -> service.createInvitation(org.getId(), shared, Invitation.Role.USER, admin));
    }

    @Test
    void acceptIsIdempotentForSameUsernameRetry() {
        Organization org = makeOrg();
        User admin = makeUser("admin");
        Invitation inv = service.createInvitation(org.getId(),
            "retry-" + System.nanoTime() + "@example.com", Invitation.Role.USER, admin);
        String username = "retry-user-" + System.nanoTime();

        User first = service.acceptInvitation(inv.getToken(), username, "CorrectH0rse!Batt", null);
        // Double-click / client retry: must return the accepted user, not 410
        User second = service.acceptInvitation(inv.getToken(), username, "CorrectH0rse!Batt", null);

        assertEquals(first.getId(), second.getId());
    }

    @Test
    void acceptWithTakenUsernameGivesClearError() {
        Organization org = makeOrg();
        User admin = makeUser("admin");
        User existing = makeUser("taken");
        Invitation inv = service.createInvitation(org.getId(),
            "fresh-" + System.nanoTime() + "@example.com", Invitation.Role.USER, admin);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
            () -> service.acceptInvitation(inv.getToken(), existing.getUsername(), "CorrectH0rse!Batt", null));
        assertTrue(ex.getMessage().contains("already taken"));
    }

    @Test
    void acceptReactivatesDeactivatedMembership() {
        Organization org = makeOrg();
        User admin = makeUser("admin");
        User member = makeUser("returning");
        OrganizationMembership membership =
            new OrganizationMembership(member, org, OrganizationMembership.OrganizationRole.USER);
        membership.setStatus(OrganizationMembership.MembershipStatus.DEACTIVATED);
        memRepo.save(membership);

        Invitation inv = service.createInvitation(org.getId(), member.getEmail(),
            Invitation.Role.USER, admin);
        User accepted = service.acceptInvitation(inv.getToken(), null, null, member);

        assertEquals(member.getId(), accepted.getId());
        OrganizationMembership reloaded =
            memRepo.findByUserIdAndOrganizationId(member.getId(), org.getId()).orElseThrow();
        assertEquals(OrganizationMembership.MembershipStatus.ACTIVE, reloaded.getStatus());
        assertEquals(Invitation.Status.ACCEPTED, invRepo.findById(inv.getId()).orElseThrow().getStatus());
    }

    @Test
    void acceptWithLockedMembershipThrowsAndDoesNotConsumeInvitation() {
        Organization org = makeOrg();
        User admin = makeUser("admin");
        User member = makeUser("locked");
        OrganizationMembership membership =
            new OrganizationMembership(member, org, OrganizationMembership.OrganizationRole.USER);
        membership.setStatus(OrganizationMembership.MembershipStatus.LOCKED);
        memRepo.save(membership);

        Invitation inv = service.createInvitation(org.getId(), member.getEmail(),
            Invitation.Role.USER, admin);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
            () -> service.acceptInvitation(inv.getToken(), null, null, member));
        assertTrue(ex.getMessage().contains("locked"));
        assertEquals(Invitation.Status.PENDING, invRepo.findById(inv.getId()).orElseThrow().getStatus());
    }

    @Test
    void acceptCreatingNewUserPublishesCrmContactEvent() {
        Organization org = makeOrg();
        User admin = makeUser("admin");
        Invitation inv = service.createInvitation(org.getId(),
            "crm-inv-" + System.nanoTime() + "@example.com", Invitation.Role.USER, admin);

        User accepted = service.acceptInvitation(inv.getToken(),
            "crm-user-" + System.nanoTime(), "CorrectH0rse!Batt", null);

        var events = applicationEvents
            .stream(gov.nist.oscal.tools.api.crm.CrmEvents.ContactRegistered.class)
            .filter(e -> e.userId().equals(accepted.getId()))
            .toList();
        assertEquals(1, events.size());
        assertEquals("invitation", events.get(0).source());
    }

    @Test
    void signedInAcceptDoesNotPublishCrmContactEvent() {
        // Existing accounts are already in the marketing DB (or opted out) —
        // only NEW account creation registers a contact.
        Organization org = makeOrg();
        User admin = makeUser("admin");
        User member = makeUser("existing");
        Invitation inv = service.createInvitation(org.getId(), member.getEmail(),
            Invitation.Role.USER, admin);

        service.acceptInvitation(inv.getToken(), null, null, member);

        assertEquals(0, applicationEvents
            .stream(gov.nist.oscal.tools.api.crm.CrmEvents.ContactRegistered.class)
            .filter(e -> e.userId().equals(member.getId()))
            .count());
    }

    // ------------------------------------------------------------------
    // Existing-account accepts require sign-in (Phase 3)
    // ------------------------------------------------------------------

    @Test
    void anonymousAcceptForExistingAccountEmailIsRefused() {
        // Possession of the emailed link must never yield a session for an
        // existing account (account-takeover vector).
        Organization org = makeOrg();
        User admin = makeUser("admin");
        User existing = makeUser("victim");
        Invitation inv = service.createInvitation(org.getId(), existing.getEmail(),
            Invitation.Role.USER, admin);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
            () -> service.acceptInvitation(inv.getToken(), null, null, null));
        assertTrue(ex.getMessage().contains("sign in"));
        // The invitation must remain usable after the refused attempt
        assertEquals(Invitation.Status.PENDING, invRepo.findById(inv.getId()).orElseThrow().getStatus());
    }

    @Test
    void signedInAcceptBindsTheAuthenticatedAccount() {
        Organization org = makeOrg();
        User admin = makeUser("admin");
        User member = makeUser("joiner");
        // Invitation addressed to a different email than the signed-in account —
        // the membership still binds to the authenticated user.
        Invitation inv = service.createInvitation(org.getId(),
            "work-" + System.nanoTime() + "@example.com", Invitation.Role.USER, admin);

        User accepted = service.acceptInvitation(inv.getToken(), null, null, member);

        assertEquals(member.getId(), accepted.getId());
        assertTrue(memRepo.findByUserIdAndOrganizationId(member.getId(), org.getId()).isPresent());
        assertEquals(Invitation.Status.ACCEPTED, invRepo.findById(inv.getId()).orElseThrow().getStatus());
    }

    // ------------------------------------------------------------------
    // Resend + email-send status (Phase 2)
    // ------------------------------------------------------------------

    @Test
    void createInvitationRecordsEmailSent() {
        Organization org = makeOrg();
        User admin = makeUser("admin");
        Invitation inv = service.createInvitation(org.getId(),
            "sent-" + System.nanoTime() + "@example.com", Invitation.Role.USER, admin);
        assertEquals(Boolean.TRUE, inv.getEmailSent());
    }

    @Test
    void createInvitationSurvivesEmailFailureAndRecordsIt() {
        Organization org = makeOrg();
        User admin = makeUser("admin");
        doThrow(new RuntimeException("smtp down"))
            .when(email).sendInvitation(any(), any(), any());

        Invitation inv = service.createInvitation(org.getId(),
            "fail-" + System.nanoTime() + "@example.com", Invitation.Role.USER, admin);

        assertEquals(Invitation.Status.PENDING, inv.getStatus());
        assertEquals(Boolean.FALSE, inv.getEmailSent());
        assertNotNull(inv.getToken(), "admin must still be able to copy the accept link");
    }

    @Test
    void resendRegeneratesTokenAndExtendsExpiry() {
        Organization org = makeOrg();
        User admin = makeUser("admin");
        Invitation inv = service.createInvitation(org.getId(),
            "resend-" + System.nanoTime() + "@example.com", Invitation.Role.USER, admin);
        String oldToken = inv.getToken();
        inv.setExpiresAt(LocalDateTime.now().plusDays(1));
        invRepo.save(inv);

        Invitation resent = service.resendInvitation(inv.getId(), admin);

        assertNotNull(resent.getToken());
        assertEquals(false, oldToken.equals(resent.getToken()));
        assertEquals(Invitation.Status.PENDING, resent.getStatus());
        assertEquals(true, resent.getExpiresAt().isAfter(LocalDateTime.now().plusDays(6)));
        assertEquals(Boolean.TRUE, resent.getEmailSent());
    }

    @Test
    void resendRevivesExpiredInvitation() {
        Organization org = makeOrg();
        User admin = makeUser("admin");
        Invitation inv = service.createInvitation(org.getId(),
            "revive-" + System.nanoTime() + "@example.com", Invitation.Role.USER, admin);
        inv.setStatus(Invitation.Status.EXPIRED);
        inv.setExpiresAt(LocalDateTime.now().minusDays(1));
        invRepo.save(inv);

        Invitation resent = service.resendInvitation(inv.getId(), admin);

        assertEquals(Invitation.Status.PENDING, resent.getStatus());
        assertEquals(true, resent.getExpiresAt().isAfter(LocalDateTime.now()));
    }

    @Test
    void resendRejectsAcceptedAndRevokedInvitations() {
        Organization org = makeOrg();
        User admin = makeUser("admin");

        Invitation accepted = service.createInvitation(org.getId(),
            "acc-" + System.nanoTime() + "@example.com", Invitation.Role.USER, admin);
        accepted.setStatus(Invitation.Status.ACCEPTED);
        invRepo.save(accepted);
        assertThrows(IllegalArgumentException.class,
            () -> service.resendInvitation(accepted.getId(), admin));

        Invitation revoked = service.createInvitation(org.getId(),
            "rev-" + System.nanoTime() + "@example.com", Invitation.Role.USER, admin);
        revoked.setStatus(Invitation.Status.REVOKED);
        invRepo.save(revoked);
        assertThrows(IllegalArgumentException.class,
            () -> service.resendInvitation(revoked.getId(), admin));
    }

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
}
