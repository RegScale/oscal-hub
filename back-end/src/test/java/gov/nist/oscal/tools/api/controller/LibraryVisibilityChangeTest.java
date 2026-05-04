/*
 * SPDX-FileCopyrightText: none
 * SPDX-License-Identifier: CC0-1.0
 */

package gov.nist.oscal.tools.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for {@code PATCH /api/library/{itemId}/visibility}.
 * <p>
 * Covers the role-gate matrix: creator can publish/unpublish their own items;
 * non-creator regular users get 404 (existence is hidden, never 403); platform
 * SUPER_ADMINs can force-unpublish another user's item; ORGANIZATION visibility
 * requires {@code organizationId} in the body.
 * <p>
 * Pattern matches {@link LibraryVisibilityControllerTest}: @SpringBootTest +
 * @AutoConfigureMockMvc + @Transactional rollback, with @WithMockUser to drive
 * Principal.getName() into the controller. EmailService is mocked to avoid
 * SMTP startup probes during context init.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class LibraryVisibilityChangeTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired LibraryItemRepository libraryItemRepo;
    @Autowired UserRepository userRepo;
    @Autowired OrganizationRepository orgRepo;
    @Autowired OrganizationMembershipRepository memRepo;
    @Autowired PasswordEncoder passwordEncoder;

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

    private User makeUser(String username, Organization org) {
        return makeUser(username, org, User.GlobalRole.USER);
    }

    private User makeUser(String username, Organization org, User.GlobalRole globalRole) {
        User u = new User();
        u.setUsername(username);
        u.setEmail(username + "@example.com");
        u.setPassword(passwordEncoder.encode("CorrectH0rse!Batt"));
        u.setEnabled(true);
        u.setGlobalRole(globalRole);
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
        if (v == Visibility.PUBLIC) {
            // Mirror the production state: a PUBLIC seed has been published before.
            LocalDateTime now = LocalDateTime.now();
            item.setPublishedAt(now);
            item.setLastPublishedAt(now);
        }
        return libraryItemRepo.save(item);
    }

    private String json(Object body) throws Exception {
        return objectMapper.writeValueAsString(body);
    }

    // -------------------------------------------------------------------------
    // Tests
    // -------------------------------------------------------------------------

    /** 1. Creator can publish their own PRIVATE item — publishedAt + lastPublishedAt stamped. */
    @Test
    @WithMockUser(username = "alice-publisher")
    void creator_canPublish_privateToPublic_stampsPublishTimestamps() throws Exception {
        Organization orgA = makeOrg("orgA");
        User alice = makeUser("alice-publisher", orgA);

        LibraryItem item = makeItem(alice, orgA, Visibility.PRIVATE);

        Map<String, Object> body = new HashMap<>();
        body.put("visibility", "PUBLIC");

        mockMvc.perform(patch("/api/library/{itemId}/visibility", item.getItemId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(body)))
               .andExpect(status().isOk());

        LibraryItem updated = libraryItemRepo.findByItemId(item.getItemId()).orElseThrow();
        assertNotNull(updated.getPublishedAt(), "publishedAt should be stamped");
        assertNotNull(updated.getLastPublishedAt(), "lastPublishedAt should be stamped");
    }

    /** 2. Non-creator regular user gets 404 (existence is hidden, never 403). */
    @Test
    @WithMockUser(username = "bob-stranger")
    void nonCreator_regularUser_returns404() throws Exception {
        Organization orgA = makeOrg("orgA");
        User alice = makeUser("alice-owner-" + System.nanoTime(), orgA);
        makeUser("bob-stranger", orgA);

        LibraryItem item = makeItem(alice, orgA, Visibility.PUBLIC);

        Map<String, Object> body = new HashMap<>();
        body.put("visibility", "PRIVATE");

        mockMvc.perform(patch("/api/library/{itemId}/visibility", item.getItemId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(body)))
               .andExpect(status().isNotFound());
    }

    /** 3. SUPER_ADMIN can force-unpublish another user's item. */
    @Test
    @WithMockUser(username = "admin-takedown")
    void superAdmin_canForceUnpublish_othersItem() throws Exception {
        Organization orgA = makeOrg("orgA");
        User alice = makeUser("alice-author-" + System.nanoTime(), orgA);
        // Admin doesn't need an org membership to act; pass null org.
        makeUser("admin-takedown", null, User.GlobalRole.SUPER_ADMIN);

        LibraryItem item = makeItem(alice, orgA, Visibility.PUBLIC);

        Map<String, Object> body = new HashMap<>();
        body.put("visibility", "PRIVATE");
        body.put("reason", "violates policy");

        mockMvc.perform(patch("/api/library/{itemId}/visibility", item.getItemId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(body)))
               .andExpect(status().isOk());
    }

    /** 4. ORGANIZATION visibility without organizationId returns 400. */
    @Test
    @WithMockUser(username = "alice-orgshare")
    void orgVisibility_withoutOrgId_returns400() throws Exception {
        Organization orgA = makeOrg("orgA");
        User alice = makeUser("alice-orgshare", orgA);

        LibraryItem item = makeItem(alice, orgA, Visibility.PRIVATE);

        Map<String, Object> body = new HashMap<>();
        body.put("visibility", "ORGANIZATION");
        // intentionally no organizationId

        mockMvc.perform(patch("/api/library/{itemId}/visibility", item.getItemId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(body)))
               .andExpect(status().isBadRequest());
    }
}
