/*
 * SPDX-FileCopyrightText: none
 * SPDX-License-Identifier: CC0-1.0
 */

package gov.nist.oscal.tools.api.repository;

import gov.nist.oscal.tools.api.entity.Invitation;
import gov.nist.oscal.tools.api.entity.Organization;
import gov.nist.oscal.tools.api.entity.User;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.hibernate.Hibernate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Regression tests for the org-admin invitations 500.
 *
 * InvitationResponse.from() reads invitation.organization.name and
 * invitation.invitedBy.username in the controller, AFTER the service
 * transaction has closed (OSIV is off). Both associations are LAZY, so the
 * repository queries must fetch-join them — otherwise production throws
 * LazyInitializationException (observed as 500s on
 * GET /api/org-admin/invitations, 2026-07-06/07).
 *
 * These tests clear the persistence context and assert the associations are
 * INITIALIZED, which fails when the fetch joins are removed even though the
 * test itself runs inside a transaction.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class InvitationRepositoryFetchTest {

    @PersistenceContext
    EntityManager em;

    @Autowired InvitationRepository invRepo;
    @Autowired OrganizationRepository orgRepo;
    @Autowired UserRepository userRepo;

    private Invitation seedInvitation() {
        Organization org = new Organization();
        org.setName("Fetch-Org-" + System.nanoTime());
        org.setActive(true);
        org.setCreatedAt(LocalDateTime.now());
        org = orgRepo.save(org);

        User inviter = new User();
        inviter.setUsername("inviter-" + System.nanoTime());
        inviter.setEmail(inviter.getUsername() + "@example.com");
        inviter.setPassword("x");
        inviter.setEnabled(true);
        inviter = userRepo.save(inviter);

        Invitation inv = new Invitation();
        inv.setEmail("invitee-" + System.nanoTime() + "@example.com");
        inv.setOrganization(org);
        inv.setInvitedBy(inviter);
        inv.setRole(Invitation.Role.USER);
        inv = invRepo.save(inv);

        // Detach everything so subsequent queries return fresh entities whose
        // lazy associations are uninitialized proxies unless fetch-joined.
        em.flush();
        em.clear();
        return inv;
    }

    @Test
    void findByOrganizationIdAndStatus_fetchJoinsOrganizationAndInviter() {
        Invitation seeded = seedInvitation();

        List<Invitation> result = invRepo.findByOrganizationIdAndStatus(
                seeded.getOrganization().getId(), Invitation.Status.PENDING);

        assertThat(result).hasSize(1);
        Invitation loaded = result.get(0);
        assertThat(Hibernate.isInitialized(loaded.getOrganization()))
                .as("organization must be fetch-joined (read outside the transaction)")
                .isTrue();
        assertThat(Hibernate.isInitialized(loaded.getInvitedBy()))
                .as("invitedBy must be fetch-joined (read outside the transaction)")
                .isTrue();
        assertThat(loaded.getOrganization().getName()).startsWith("Fetch-Org-");
    }

    @Test
    void findByToken_fetchJoinsOrganizationAndInviter() {
        Invitation seeded = seedInvitation();

        Optional<Invitation> result = invRepo.findByToken(seeded.getToken());

        assertThat(result).isPresent();
        assertThat(Hibernate.isInitialized(result.get().getOrganization())).isTrue();
        assertThat(Hibernate.isInitialized(result.get().getInvitedBy())).isTrue();
    }
}
