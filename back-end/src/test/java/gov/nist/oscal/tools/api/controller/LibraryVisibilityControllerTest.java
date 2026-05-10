/*
 * SPDX-FileCopyrightText: none
 * SPDX-License-Identifier: CC0-1.0
 */

package gov.nist.oscal.tools.api.controller;

import gov.nist.oscal.tools.api.email.EmailService;
import gov.nist.oscal.tools.api.entity.LibraryItem;
import gov.nist.oscal.tools.api.entity.Organization;
import gov.nist.oscal.tools.api.entity.OrganizationMembership;
import gov.nist.oscal.tools.api.entity.OrganizationMembership.OrganizationRole;
import gov.nist.oscal.tools.api.entity.User;
import gov.nist.oscal.tools.api.entity.Visibility;
import gov.nist.oscal.tools.api.repository.LibraryItemRepository;
import gov.nist.oscal.tools.api.repository.OrganizationMembershipRepository;
import gov.nist.oscal.tools.api.repository.OrganizationRepository;
import gov.nist.oscal.tools.api.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end visibility matrix test for the single-item GET endpoint.
 * <p>
 * Pattern matches existing controller integration tests (e.g.
 * {@code InvitationControllerTest}): @SpringBootTest + @AutoConfigureMockMvc +
 * @Transactional rollback. We do not mint real JWTs — Spring Security's
 * @WithMockUser stand-in is sufficient because the controller resolves the
 * caller via {@code Principal.getName()}, which @WithMockUser populates.
 * <p>
 * Each test seeds:
 *   - two organizations
 *   - one creator user (a member of org-A) — owns the seeded library item
 *   - one viewer user (a member of org-A or org-B depending on the case)
 * then uses @WithMockUser to make a GET request as the viewer and asserts the
 * expected HTTP status.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class LibraryVisibilityControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired LibraryItemRepository libraryItemRepo;
    @Autowired UserRepository userRepo;
    @Autowired OrganizationRepository orgRepo;
    @Autowired OrganizationMembershipRepository memRepo;
    @Autowired PasswordEncoder passwordEncoder;

    // Email service is wired into auth flows that we never exercise here — mock
    // to avoid SMTP startup probes during context init.
    @MockitoBean EmailService emailService;

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private Organization makeOrg(String label) {
        Organization o = new Organization();
        o.setName(label + "-" + System.nanoTime());
        o.setActive(true);
        o.setCreatedAt(LocalDateTime.now());
        return orgRepo.save(o);
    }

    /** Build a user (with a known username for @WithMockUser) and an ACTIVE membership in org. */
    private User makeUser(String username, Organization org) {
        User u = new User();
        u.setUsername(username);
        u.setEmail(username + "@example.com");
        u.setPassword(passwordEncoder.encode("CorrectH0rse!Batt"));
        u.setEnabled(true);
        u = userRepo.save(u);
        if (org != null) {
            memRepo.save(new OrganizationMembership(u, org, OrganizationRole.USER));
        }
        return u;
    }

    private LibraryItem makeItem(User creator, Organization org, Visibility v) {
        LibraryItem item = new LibraryItem(
            UUID.randomUUID().toString(),
            "Test Item",
            "Test description",
            "catalog",
            creator);
        item.setOrganization(org);
        item.setVisibility(v);
        return libraryItemRepo.save(item);
    }

    // -------------------------------------------------------------------------
    // Visibility matrix — GET /api/library/{itemId}
    // -------------------------------------------------------------------------

    @Test
    @WithMockUser(username = "viewer-public-sameorg")
    void publicItem_otherCreator_sameOrg_returns200() throws Exception {
        Organization orgA = makeOrg("orgA");
        User creator = makeUser("creator-pub-same-" + System.nanoTime(), orgA);
        makeUser("viewer-public-sameorg", orgA);

        LibraryItem item = makeItem(creator, orgA, Visibility.PUBLIC);

        mockMvc.perform(get("/api/library/{itemId}", item.getItemId()))
               .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "viewer-public-diffrog")
    void publicItem_otherCreator_differentOrg_returns200() throws Exception {
        Organization orgA = makeOrg("orgA");
        Organization orgB = makeOrg("orgB");
        User creator = makeUser("creator-pub-diff-" + System.nanoTime(), orgA);
        makeUser("viewer-public-diffrog", orgB);

        LibraryItem item = makeItem(creator, orgA, Visibility.PUBLIC);

        mockMvc.perform(get("/api/library/{itemId}", item.getItemId()))
               .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "viewer-org-sameorg")
    void organizationItem_otherCreator_sameOrg_returns200() throws Exception {
        Organization orgA = makeOrg("orgA");
        User creator = makeUser("creator-org-same-" + System.nanoTime(), orgA);
        makeUser("viewer-org-sameorg", orgA);

        LibraryItem item = makeItem(creator, orgA, Visibility.ORGANIZATION);

        mockMvc.perform(get("/api/library/{itemId}", item.getItemId()))
               .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "viewer-org-difforg")
    void organizationItem_otherCreator_differentOrg_returns404() throws Exception {
        Organization orgA = makeOrg("orgA");
        Organization orgB = makeOrg("orgB");
        User creator = makeUser("creator-org-diff-" + System.nanoTime(), orgA);
        makeUser("viewer-org-difforg", orgB);

        LibraryItem item = makeItem(creator, orgA, Visibility.ORGANIZATION);

        mockMvc.perform(get("/api/library/{itemId}", item.getItemId()))
               .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "creator-org-self")
    void organizationItem_selfCreator_sameOrg_returns200() throws Exception {
        Organization orgA = makeOrg("orgA");
        User creator = makeUser("creator-org-self", orgA);

        LibraryItem item = makeItem(creator, orgA, Visibility.ORGANIZATION);

        mockMvc.perform(get("/api/library/{itemId}", item.getItemId()))
               .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "viewer-priv-sameorg")
    void privateItem_otherCreator_sameOrg_returns404() throws Exception {
        // Same-org membership does NOT grant access to PRIVATE items —
        // they are restricted to the creator only.
        Organization orgA = makeOrg("orgA");
        User creator = makeUser("creator-priv-same-" + System.nanoTime(), orgA);
        makeUser("viewer-priv-sameorg", orgA);

        LibraryItem item = makeItem(creator, orgA, Visibility.PRIVATE);

        mockMvc.perform(get("/api/library/{itemId}", item.getItemId()))
               .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "creator-priv-self")
    void privateItem_selfCreator_returns200() throws Exception {
        Organization orgA = makeOrg("orgA");
        User creator = makeUser("creator-priv-self", orgA);

        LibraryItem item = makeItem(creator, orgA, Visibility.PRIVATE);

        mockMvc.perform(get("/api/library/{itemId}", item.getItemId()))
               .andExpect(status().isOk());
    }
}
