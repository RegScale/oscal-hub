package gov.nist.oscal.tools.api.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class InvitationServiceTest {

    @Autowired InvitationService service;
    @Autowired InvitationRepository invRepo;
    @Autowired UserRepository userRepo;
    @Autowired OrganizationRepository orgRepo;
    @Autowired OrganizationMembershipRepository memRepo;
    @Autowired PasswordEncoder passwordEncoder;
    @MockBean EmailService email;

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

        User accepted = service.acceptInvitation(inv.getToken(), "newuser-" + System.nanoTime(), "CorrectH0rse!Batt");

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
            () -> service.acceptInvitation(inv.getToken(), "u-" + System.nanoTime(), "CorrectH0rse!Batt"));
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
