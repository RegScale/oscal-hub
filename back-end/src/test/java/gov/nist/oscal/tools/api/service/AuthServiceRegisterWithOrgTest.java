package gov.nist.oscal.tools.api.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import gov.nist.oscal.tools.api.email.EmailService;
import gov.nist.oscal.tools.api.entity.Organization;
import gov.nist.oscal.tools.api.entity.OrganizationMembership;
import gov.nist.oscal.tools.api.entity.User;
import gov.nist.oscal.tools.api.exception.OrganizationNameInUseException;
import gov.nist.oscal.tools.api.model.AuthResponse;
import gov.nist.oscal.tools.api.model.RegisterRequest;
import gov.nist.oscal.tools.api.repository.OrganizationMembershipRepository;
import gov.nist.oscal.tools.api.repository.OrganizationRepository;
import gov.nist.oscal.tools.api.repository.UserRepository;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
@org.springframework.test.context.event.RecordApplicationEvents
class AuthServiceRegisterWithOrgTest {

    @Autowired AuthService authService;
    @Autowired UserRepository userRepo;
    @Autowired OrganizationRepository orgRepo;
    @Autowired OrganizationMembershipRepository membershipRepo;
    @Autowired org.springframework.test.context.event.ApplicationEvents applicationEvents;
    @MockitoBean EmailService emailService;

    @Test
    void registerWithoutOrgNameKeepsOldBehavior() {
        RegisterRequest r = new RegisterRequest();
        r.setUsername("noorg-user-" + System.nanoTime());
        r.setEmail("noorg-" + System.nanoTime() + "@example.com");
        r.setPassword("CorrectH0rse!Batt");

        AuthResponse resp = authService.register(r);

        assertNotNull(resp.getToken());
        long count = membershipRepo.findAll().stream()
            .filter(m -> m.getUser().getId().equals(resp.getUserId()))
            .count();
        assertEquals(0, count);
    }

    @Test
    void registerWithOrgNameCreatesOrgAndOrgAdminMembership() {
        long suffix = System.nanoTime();
        RegisterRequest r = new RegisterRequest();
        r.setUsername("withorg-" + suffix);
        r.setEmail("withorg-" + suffix + "@example.com");
        r.setPassword("CorrectH0rse!Batt");
        r.setOrganizationName("Acme " + suffix);

        AuthResponse resp = authService.register(r);

        Organization org = orgRepo.findByName("Acme " + suffix).orElseThrow();
        List<OrganizationMembership> memberships = membershipRepo.findAll().stream()
            .filter(m -> m.getUser().getId().equals(resp.getUserId()))
            .collect(Collectors.toList());
        assertEquals(1, memberships.size());
        assertEquals(org.getId(), memberships.get(0).getOrganization().getId());
        assertTrue(memberships.get(0).getRole().name().equals("ORG_ADMIN"));
        assertTrue(memberships.get(0).getStatus().name().equals("ACTIVE"));
    }

    @Test
    void registerWithDuplicateOrgNameThrowsTypedException() {
        long suffix = System.nanoTime();
        Organization existing = new Organization();
        existing.setName("Taken " + suffix);
        existing.setActive(true);
        existing.setCreatedAt(java.time.LocalDateTime.now());
        orgRepo.save(existing);

        RegisterRequest r = new RegisterRequest();
        r.setUsername("collision-" + suffix);
        r.setEmail("col-" + suffix + "@example.com");
        r.setPassword("CorrectH0rse!Batt");
        r.setOrganizationName("Taken " + suffix);

        assertThrows(OrganizationNameInUseException.class, () -> authService.register(r));
    }

    @Test
    void registerWithCaseDifferentOrgNameStillCollides() {
        long suffix = System.nanoTime();
        Organization existing = new Organization();
        existing.setName("MixedCase " + suffix);
        existing.setActive(true);
        existing.setCreatedAt(java.time.LocalDateTime.now());
        orgRepo.save(existing);

        RegisterRequest r = new RegisterRequest();
        r.setUsername("user-" + suffix);
        r.setEmail("u-" + suffix + "@example.com");
        r.setPassword("CorrectH0rse!Batt");
        r.setOrganizationName("mixedcase " + suffix);  // different case

        assertThrows(OrganizationNameInUseException.class, () -> authService.register(r));
    }

    @Test
    void registerPublishesWelcomeEmailEvent() {
        // The welcome email is sent AFTER COMMIT by TransactionalEmailListener;
        // in a rolled-back test transaction the listener never fires, so assert
        // the published event instead of a mock invocation.
        long suffix = System.nanoTime();
        RegisterRequest r = new RegisterRequest();
        r.setUsername("welcomed-" + suffix);
        r.setEmail("hi-" + suffix + "@example.com");
        r.setPassword("CorrectH0rse!Batt");
        r.setOrganizationName("Welcomed " + suffix);

        AuthResponse resp = authService.register(r);

        List<gov.nist.oscal.tools.api.email.EmailEvents.WelcomeEmail> events =
            applicationEvents.stream(gov.nist.oscal.tools.api.email.EmailEvents.WelcomeEmail.class)
                .collect(Collectors.toList());
        assertEquals(1, events.size());
        assertEquals(resp.getUserId(), events.get(0).userId());
    }
}
