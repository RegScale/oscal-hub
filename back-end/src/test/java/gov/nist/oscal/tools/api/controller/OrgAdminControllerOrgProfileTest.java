/*
 * SPDX-FileCopyrightText: none
 * SPDX-License-Identifier: CC0-1.0
 */

package gov.nist.oscal.tools.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import gov.nist.oscal.tools.api.email.EmailService;
import gov.nist.oscal.tools.api.entity.Organization;
import gov.nist.oscal.tools.api.entity.OrganizationMembership;
import gov.nist.oscal.tools.api.entity.OrganizationMembership.OrganizationRole;
import gov.nist.oscal.tools.api.entity.User;
import gov.nist.oscal.tools.api.repository.OrganizationMembershipRepository;
import gov.nist.oscal.tools.api.repository.OrganizationRepository;
import gov.nist.oscal.tools.api.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for the ORG_ADMIN-scoped organization profile endpoints:
 * {@code GET /api/org-admin/organizations/{id}} and the matching {@code PATCH}.
 *
 * <p>Covers the three rule branches: an ORG_ADMIN of the target org succeeds,
 * an ORG_ADMIN of a different org gets 403 (no cross-org write), and a regular
 * USER member of the org also gets 403. SUPER_ADMIN with no membership at all
 * is allowed (platform-level bypass).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class OrgAdminControllerOrgProfileTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
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
        o.setDescription("Initial description for " + label);
        o.setActive(true);
        o.setCreatedAt(LocalDateTime.now());
        return orgRepo.save(o);
    }

    private User makeUser(String username, Organization org, OrganizationRole role,
                          User.GlobalRole globalRole) {
        User u = new User();
        u.setUsername(username);
        u.setEmail(username + "@example.com");
        u.setPassword(passwordEncoder.encode("CorrectH0rse!Batt"));
        u.setEnabled(true);
        u.setGlobalRole(globalRole);
        u = userRepo.save(u);
        if (org != null) {
            memRepo.save(new OrganizationMembership(u, org, role));
        }
        return u;
    }

    private String json(Object body) throws Exception {
        return objectMapper.writeValueAsString(body);
    }

    // -------------------------------------------------------------------------
    // GET /api/org-admin/organizations/{id}
    // -------------------------------------------------------------------------

    @Test
    @WithMockUser(username = "alice-get-admin", roles = {"ORG_ADMIN"})
    void getOrgProfile_orgAdminOfThatOrg_returnsProfile() throws Exception {
        Organization orgA = makeOrg("Alpha");
        makeUser("alice-get-admin", orgA, OrganizationRole.ORG_ADMIN, User.GlobalRole.USER);

        mockMvc.perform(get("/api/org-admin/organizations/{id}", orgA.getId()))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.id").value(orgA.getId()))
               .andExpect(jsonPath("$.name").value(orgA.getName()))
               .andExpect(jsonPath("$.description").value(orgA.getDescription()));
    }

    @Test
    @WithMockUser(username = "bob-get-other", roles = {"ORG_ADMIN"})
    void getOrgProfile_orgAdminOfDifferentOrg_returns403() throws Exception {
        Organization orgA = makeOrg("Alpha");
        Organization orgB = makeOrg("Bravo");
        // Bob is ORG_ADMIN of orgB but tries to read orgA.
        makeUser("bob-get-other", orgB, OrganizationRole.ORG_ADMIN, User.GlobalRole.USER);

        mockMvc.perform(get("/api/org-admin/organizations/{id}", orgA.getId()))
               .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "charlie-regular", roles = {"ORG_ADMIN"})
    void getOrgProfile_regularUserOfOrg_returns403() throws Exception {
        Organization orgA = makeOrg("Alpha");
        // Charlie has @WithMockUser ORG_ADMIN (Spring Security gate), but his
        // membership role is USER — the per-org membership check rejects him.
        makeUser("charlie-regular", orgA, OrganizationRole.USER, User.GlobalRole.USER);

        mockMvc.perform(get("/api/org-admin/organizations/{id}", orgA.getId()))
               .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "platform-admin", roles = {"SUPER_ADMIN"})
    void getOrgProfile_superAdminWithNoMembership_succeeds() throws Exception {
        Organization orgA = makeOrg("Alpha");
        // Platform admin has SUPER_ADMIN global role but no membership in orgA.
        makeUser("platform-admin", null, null, User.GlobalRole.SUPER_ADMIN);

        mockMvc.perform(get("/api/org-admin/organizations/{id}", orgA.getId()))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.id").value(orgA.getId()));
    }

    // -------------------------------------------------------------------------
    // PATCH /api/org-admin/organizations/{id}
    // -------------------------------------------------------------------------

    @Test
    @WithMockUser(username = "alice-patch-admin", roles = {"ORG_ADMIN"})
    void patchOrgProfile_orgAdminOfThatOrg_updatesNameAndDescription() throws Exception {
        Organization orgA = makeOrg("Alpha");
        makeUser("alice-patch-admin", orgA, OrganizationRole.ORG_ADMIN, User.GlobalRole.USER);

        Map<String, Object> body = new HashMap<>();
        body.put("name", "Renamed Org-" + System.nanoTime());
        body.put("description", "Updated description");

        mockMvc.perform(patch("/api/org-admin/organizations/{id}", orgA.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(body)))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.name").value(body.get("name")))
               .andExpect(jsonPath("$.description").value("Updated description"));

        Organization reloaded = orgRepo.findById(orgA.getId()).orElseThrow();
        assertEquals(body.get("name"), reloaded.getName());
        assertEquals("Updated description", reloaded.getDescription());
    }

    @Test
    @WithMockUser(username = "alice-patch-name-only", roles = {"ORG_ADMIN"})
    void patchOrgProfile_omittedFieldIsLeftAlone() throws Exception {
        Organization orgA = makeOrg("Alpha");
        makeUser("alice-patch-name-only", orgA, OrganizationRole.ORG_ADMIN, User.GlobalRole.USER);
        String originalDescription = orgA.getDescription();

        Map<String, Object> body = new HashMap<>();
        body.put("name", "Just-name-changed-" + System.nanoTime());
        // No description key — backend must leave the existing value alone.

        mockMvc.perform(patch("/api/org-admin/organizations/{id}", orgA.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(body)))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.description").value(originalDescription));
    }

    @Test
    @WithMockUser(username = "bob-patch-other", roles = {"ORG_ADMIN"})
    void patchOrgProfile_orgAdminOfDifferentOrg_returns403() throws Exception {
        Organization orgA = makeOrg("Alpha");
        Organization orgB = makeOrg("Bravo");
        String originalName = orgA.getName();
        makeUser("bob-patch-other", orgB, OrganizationRole.ORG_ADMIN, User.GlobalRole.USER);

        Map<String, Object> body = new HashMap<>();
        body.put("name", "Hijacked-name");

        mockMvc.perform(patch("/api/org-admin/organizations/{id}", orgA.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(body)))
               .andExpect(status().isForbidden());

        // Name unchanged.
        Organization reloaded = orgRepo.findById(orgA.getId()).orElseThrow();
        assertEquals(originalName, reloaded.getName());
    }

    @Test
    @WithMockUser(username = "duplicate-name-attempt", roles = {"ORG_ADMIN"})
    void patchOrgProfile_duplicateName_returns400() throws Exception {
        Organization orgA = makeOrg("Alpha");
        Organization orgB = makeOrg("Bravo");
        makeUser("duplicate-name-attempt", orgA, OrganizationRole.ORG_ADMIN, User.GlobalRole.USER);

        Map<String, Object> body = new HashMap<>();
        body.put("name", orgB.getName()); // collide with orgB

        mockMvc.perform(patch("/api/org-admin/organizations/{id}", orgA.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(body)))
               .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "platform-rename-admin", roles = {"SUPER_ADMIN"})
    void patchOrgProfile_superAdminCanRenameAnyOrg() throws Exception {
        Organization orgA = makeOrg("Alpha");
        makeUser("platform-rename-admin", null, null, User.GlobalRole.SUPER_ADMIN);

        Map<String, Object> body = new HashMap<>();
        body.put("name", "Renamed-by-admin-" + System.nanoTime());

        mockMvc.perform(patch("/api/org-admin/organizations/{id}", orgA.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(body)))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.name").value(body.get("name")));
    }

    // -------------------------------------------------------------------------
    // POST /api/org-admin/organizations/{id}/logo
    // (Authorisation + validation paths — we deliberately don't exercise the
    // happy filesystem-write path here, as the service does virus scans and
    // disk writes that are covered in OrganizationServiceTest.)
    // -------------------------------------------------------------------------

    @Test
    @WithMockUser(username = "bob-logo-other", roles = {"ORG_ADMIN"})
    void uploadLogo_orgAdminOfDifferentOrg_returns403() throws Exception {
        Organization orgA = makeOrg("Alpha");
        Organization orgB = makeOrg("Bravo");
        makeUser("bob-logo-other", orgB, OrganizationRole.ORG_ADMIN, User.GlobalRole.USER);

        MockMultipartFile file = new MockMultipartFile(
                "file", "logo.png", "image/png", new byte[]{1, 2, 3});

        mockMvc.perform(multipart("/api/org-admin/organizations/{id}/logo", orgA.getId())
                .file(file))
               .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "charlie-logo-regular", roles = {"ORG_ADMIN"})
    void uploadLogo_regularUserOfOrg_returns403() throws Exception {
        Organization orgA = makeOrg("Alpha");
        makeUser("charlie-logo-regular", orgA, OrganizationRole.USER, User.GlobalRole.USER);

        MockMultipartFile file = new MockMultipartFile(
                "file", "logo.png", "image/png", new byte[]{1, 2, 3});

        mockMvc.perform(multipart("/api/org-admin/organizations/{id}/logo", orgA.getId())
                .file(file))
               .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "alice-logo-empty", roles = {"ORG_ADMIN"})
    void uploadLogo_emptyFile_returns400() throws Exception {
        Organization orgA = makeOrg("Alpha");
        makeUser("alice-logo-empty", orgA, OrganizationRole.ORG_ADMIN, User.GlobalRole.USER);

        MockMultipartFile file = new MockMultipartFile(
                "file", "logo.png", "image/png", new byte[0]);

        mockMvc.perform(multipart("/api/org-admin/organizations/{id}/logo", orgA.getId())
                .file(file))
               .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "alice-logo-badtype", roles = {"ORG_ADMIN"})
    void uploadLogo_invalidContentType_returns400() throws Exception {
        Organization orgA = makeOrg("Alpha");
        makeUser("alice-logo-badtype", orgA, OrganizationRole.ORG_ADMIN, User.GlobalRole.USER);

        // application/pdf is not in the PNG/JPG/SVG allow-list.
        MockMultipartFile file = new MockMultipartFile(
                "file", "logo.pdf", "application/pdf", new byte[]{1, 2, 3, 4});

        mockMvc.perform(multipart("/api/org-admin/organizations/{id}/logo", orgA.getId())
                .file(file))
               .andExpect(status().isBadRequest());
    }

    // -------------------------------------------------------------------------
    // DELETE /api/org-admin/organizations/{id}/logo
    // -------------------------------------------------------------------------

    @Test
    @WithMockUser(username = "alice-delete-logo", roles = {"ORG_ADMIN"})
    void deleteLogo_orgAdminOfThatOrg_clearsLogoUrl() throws Exception {
        Organization orgA = makeOrg("Alpha");
        // Seed a logo URL value on the entity directly so we can verify the
        // endpoint nulls it without needing a real filesystem upload.
        orgA.setLogoUrl("/api/files/org-logos/seed.png");
        orgRepo.save(orgA);
        makeUser("alice-delete-logo", orgA, OrganizationRole.ORG_ADMIN, User.GlobalRole.USER);

        mockMvc.perform(delete("/api/org-admin/organizations/{id}/logo", orgA.getId()))
               .andExpect(status().isNoContent());

        Organization reloaded = orgRepo.findById(orgA.getId()).orElseThrow();
        assertNull(reloaded.getLogoUrl(), "DELETE should null the logoUrl");
    }

    @Test
    @WithMockUser(username = "alice-delete-no-logo", roles = {"ORG_ADMIN"})
    void deleteLogo_noExistingLogo_stillReturns204() throws Exception {
        // Service treats "no logo to delete" as a no-op success.
        Organization orgA = makeOrg("Alpha");
        assertNull(orgA.getLogoUrl());
        makeUser("alice-delete-no-logo", orgA, OrganizationRole.ORG_ADMIN, User.GlobalRole.USER);

        mockMvc.perform(delete("/api/org-admin/organizations/{id}/logo", orgA.getId()))
               .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(username = "bob-delete-other", roles = {"ORG_ADMIN"})
    void deleteLogo_orgAdminOfDifferentOrg_returns403_andLeavesLogoIntact() throws Exception {
        Organization orgA = makeOrg("Alpha");
        Organization orgB = makeOrg("Bravo");
        orgA.setLogoUrl("/api/files/org-logos/seed.png");
        orgRepo.save(orgA);
        makeUser("bob-delete-other", orgB, OrganizationRole.ORG_ADMIN, User.GlobalRole.USER);

        mockMvc.perform(delete("/api/org-admin/organizations/{id}/logo", orgA.getId()))
               .andExpect(status().isForbidden());

        Organization reloaded = orgRepo.findById(orgA.getId()).orElseThrow();
        assertNotNull(reloaded.getLogoUrl(), "logoUrl must be unchanged after a forbidden delete");
    }
}
