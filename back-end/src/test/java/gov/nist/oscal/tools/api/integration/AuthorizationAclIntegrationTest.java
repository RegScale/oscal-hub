package gov.nist.oscal.tools.api.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import gov.nist.oscal.tools.api.entity.Authorization;
import gov.nist.oscal.tools.api.entity.AuthorizationGrant;
import gov.nist.oscal.tools.api.entity.AuthorizationRole;
import gov.nist.oscal.tools.api.entity.AuthorizationTemplate;
import gov.nist.oscal.tools.api.entity.Organization;
import gov.nist.oscal.tools.api.entity.OrganizationMembership;
import gov.nist.oscal.tools.api.entity.OrganizationMembership.MembershipStatus;
import gov.nist.oscal.tools.api.entity.OrganizationMembership.OrganizationRole;
import gov.nist.oscal.tools.api.entity.User;
import gov.nist.oscal.tools.api.entity.User.GlobalRole;
import gov.nist.oscal.tools.api.repository.AuthorizationGrantRepository;
import gov.nist.oscal.tools.api.repository.AuthorizationRepository;
import gov.nist.oscal.tools.api.repository.AuthorizationTemplateRepository;
import gov.nist.oscal.tools.api.repository.OrganizationMembershipRepository;
import gov.nist.oscal.tools.api.repository.OrganizationRepository;
import gov.nist.oscal.tools.api.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * End-to-end RBAC integration tests for the Authorization ACL system.
 *
 * These tests run through the full Spring Security + JPA stack with a real H2
 * database and MockMvc. They verify the actual HTTP behaviour produced by the
 * combination of AuthorizationAccessGuard, AuthorizationService, and
 * AuthorizationController — including places where the current implementation
 * differs from a strict "private-by-default" spec (noted in test display names).
 *
 * Role resolution summary (AuthorizationAccessGuard):
 *   SUPER_ADMIN global role  → effective OWNER (bypasses org check)
 *   ORG_ADMIN org role       → effective OWNER (still requires org membership)
 *   creator (authorizedBy)   → effective OWNER
 *   explicit AuthorizationGrant → that grant's role
 *   shareWithOrgDefaultRole  → fallback for org members with no explicit grant
 *   no role at all           → null (access denied)
 *
 * Implementation notes that affect expected HTTP status codes:
 *   - GET /{id} wraps the service call in try-catch(Exception) → 404 on any error.
 *     getAuthorizationForUser() is org-scoped but NOT access-guard-filtered, so
 *     same-org users without a grant still receive 200 (not 404).
 *   - PUT /{id} wraps everything in try-catch(Exception) → 404 even for 403 cases.
 *   - DELETE /{id} only returns 403 when the message contains "Only the creator",
 *     but InsufficientAuthorizationRoleException says "Insufficient role…", so
 *     insufficient-role deletes return 404 (not 403).
 *   - listGrants / addGrant / share-with-org are NOT wrapped in generic try-catch,
 *     so InsufficientAuthorizationRoleException (@ResponseStatus 403) propagates
 *     correctly.
 *   - SUPER_ADMIN users without an org membership trigger NoActiveOrganizationException
 *     inside the org-context resolver; org-scoped endpoints return 404 (GET/PUT/DELETE)
 *     or 403 (grant endpoints) for such users.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("Authorization RBAC end-to-end")
class AuthorizationAclIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private EntityManager entityManager;
    @Autowired private UserRepository userRepository;
    @Autowired private OrganizationRepository organizationRepository;
    @Autowired private OrganizationMembershipRepository membershipRepository;
    @Autowired private AuthorizationTemplateRepository templateRepository;
    @Autowired private AuthorizationRepository authorizationRepository;
    @Autowired private AuthorizationGrantRepository grantRepository;

    // Shared fixtures — re-created fresh for each test by @Transactional rollback + @BeforeEach.
    private Organization orgA;
    private Organization orgB;
    private User alice;    // creator/OWNER of authA, member of orgA
    private User bob;      // ordinary USER in orgA, no grants by default
    private User carol;    // ordinary USER in orgB (cross-org)
    private User dave;     // SUPER_ADMIN — no org membership
    private User eve;      // ORG_ADMIN of orgA
    private AuthorizationTemplate templateA;
    private Authorization authA;

    @BeforeEach
    void setUp() {
        // @Transactional rolls back after each test, so no manual deletes needed.
        orgA = newOrg("Org A");
        orgB = newOrg("Org B");

        alice = newUser("alice", GlobalRole.USER);
        bob   = newUser("bob",   GlobalRole.USER);
        carol = newUser("carol", GlobalRole.USER);
        dave  = newUser("dave",  GlobalRole.SUPER_ADMIN);
        eve   = newUser("eve",   GlobalRole.USER);

        joinOrg(alice, orgA, OrganizationRole.USER);
        joinOrg(bob,   orgA, OrganizationRole.USER);
        joinOrg(carol, orgB, OrganizationRole.USER);
        // dave (SUPER_ADMIN) intentionally has no org membership
        joinOrg(eve,   orgA, OrganizationRole.ORG_ADMIN);

        templateA = newTemplate("TA", alice, orgA);
        authA     = newAuthorization("Authorization A", alice, templateA, orgA);

        // Flush all pending writes to the H2 DB and clear the JPA first-level cache.
        // This ensures that subsequent entity loads (e.g. user.getOrganizationMemberships()
        // in isInSameOrg()) re-fetch from the DB and see the memberships saved above,
        // rather than returning stale cached User objects whose lazy collections are empty.
        entityManager.flush();
        entityManager.clear();
    }

    // ========================================================================
    // GET /api/authorizations/{id}
    // ========================================================================

    @Nested
    @DisplayName("GET /api/authorizations/{id}")
    class ReadOne {

        @Test
        @WithMockUser("alice")
        @DisplayName("creator gets 200 — auto-OWNER via authorizedBy")
        void creator_ok() throws Exception {
            mockMvc.perform(get("/api/authorizations/" + authA.getId()))
                    .andExpect(status().isOk());
        }

        @Test
        @WithMockUser("bob")
        @DisplayName("same-org user without grant gets 200 — getAuthorizationForUser is org-scoped only, not access-guard-filtered")
        void sameOrgNoGrant_ok_notFiltered() throws Exception {
            // NOTE: The current implementation does NOT apply the access guard on the
            // single-resource GET path — only the list path filters by effectiveRole.
            // A same-org user therefore receives 200 even without a grant.
            mockMvc.perform(get("/api/authorizations/" + authA.getId()))
                    .andExpect(status().isOk());
        }

        @Test
        @WithMockUser("bob")
        @DisplayName("same-org user with VIEWER grant gets 200")
        void sameOrgViewerGrant_ok() throws Exception {
            grant(authA, bob, AuthorizationRole.VIEWER, alice);
            mockMvc.perform(get("/api/authorizations/" + authA.getId()))
                    .andExpect(status().isOk());
        }

        @Test
        @WithMockUser("bob")
        @DisplayName("same-org user benefits from shareWithOrg=VIEWER, gets 200")
        void shareWithOrg_ok() throws Exception {
            authA.setShareWithOrgDefaultRole(AuthorizationRole.VIEWER);
            authorizationRepository.save(authA);
            mockMvc.perform(get("/api/authorizations/" + authA.getId()))
                    .andExpect(status().isOk());
        }

        @Test
        @WithMockUser("carol")
        @DisplayName("cross-org user gets 404 — auth not found in carol's org scope")
        void crossOrg_notFound() throws Exception {
            mockMvc.perform(get("/api/authorizations/" + authA.getId()))
                    .andExpect(status().isNotFound());
        }

        @Test
        @WithMockUser("dave")
        @DisplayName("SUPER_ADMIN without org membership gets 404 — NoActiveOrganizationException caught as Exception")
        void superAdminNoOrg_notFound() throws Exception {
            // dave has no org membership, so resolveUserOrg throws NoActiveOrganizationException.
            // The controller wraps all exceptions as 404.
            mockMvc.perform(get("/api/authorizations/" + authA.getId()))
                    .andExpect(status().isNotFound());
        }

        @Test
        @WithMockUser("eve")
        @DisplayName("ORG_ADMIN of orgA gets 200 — access guard grants effective OWNER")
        void orgAdmin_ok() throws Exception {
            mockMvc.perform(get("/api/authorizations/" + authA.getId()))
                    .andExpect(status().isOk());
        }
    }

    // ========================================================================
    // GET /api/authorizations (list)
    // ========================================================================

    @Nested
    @DisplayName("GET /api/authorizations (list)")
    class ListAccess {

        @Test
        @WithMockUser("alice")
        @DisplayName("creator sees her authorization in the list")
        void creator_seesAuth() throws Exception {
            mockMvc.perform(get("/api/authorizations"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[?(@.id == " + authA.getId() + ")]").exists());
        }

        @Test
        @WithMockUser("bob")
        @DisplayName("same-org user without grant does NOT see the authorization (list is access-guard-filtered)")
        void sameOrgNoGrant_doesNotSee() throws Exception {
            mockMvc.perform(get("/api/authorizations"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[?(@.id == " + authA.getId() + ")]").doesNotExist());
        }

        @Test
        @WithMockUser("bob")
        @DisplayName("same-org user with VIEWER grant sees the authorization")
        void sameOrgViewerGrant_sees() throws Exception {
            grant(authA, bob, AuthorizationRole.VIEWER, alice);
            mockMvc.perform(get("/api/authorizations"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[?(@.id == " + authA.getId() + ")]").exists());
        }

        @Test
        @WithMockUser("bob")
        @DisplayName("shareWithOrg=VIEWER allows bob to see the authorization in the list")
        void shareWithOrg_sees() throws Exception {
            authA.setShareWithOrgDefaultRole(AuthorizationRole.VIEWER);
            authorizationRepository.save(authA);
            mockMvc.perform(get("/api/authorizations"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[?(@.id == " + authA.getId() + ")]").exists());
        }

        @Test
        @WithMockUser("carol")
        @DisplayName("cross-org user gets empty list — org-scoped query excludes orgA authorizations")
        void crossOrg_emptyList() throws Exception {
            mockMvc.perform(get("/api/authorizations"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[?(@.id == " + authA.getId() + ")]").doesNotExist());
        }

        @Test
        @WithMockUser("eve")
        @DisplayName("ORG_ADMIN of orgA sees the authorization (effective OWNER via org role)")
        void orgAdmin_seesAuth() throws Exception {
            mockMvc.perform(get("/api/authorizations"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[?(@.id == " + authA.getId() + ")]").exists());
        }
    }

    // ========================================================================
    // PUT /api/authorizations/{id}
    // ========================================================================

    @Nested
    @DisplayName("PUT /api/authorizations/{id}")
    class UpdateAccess {

        @Test
        @WithMockUser("bob")
        @DisplayName("VIEWER cannot update — InsufficientAuthorizationRoleException caught as Exception → 404")
        void viewer_blocked() throws Exception {
            grant(authA, bob, AuthorizationRole.VIEWER, alice);
            // The controller wraps all exceptions as 404 (not 403).
            mockMvc.perform(put("/api/authorizations/" + authA.getId())
                            .with(csrf())
                            .contentType("application/json")
                            .content(updateBody()))
                    .andExpect(status().isNotFound());
        }

        @Test
        @WithMockUser("bob")
        @DisplayName("CONTRIBUTOR cannot update — InsufficientAuthorizationRoleException caught as Exception → 404")
        void contributor_blocked() throws Exception {
            grant(authA, bob, AuthorizationRole.CONTRIBUTOR, alice);
            mockMvc.perform(put("/api/authorizations/" + authA.getId())
                            .with(csrf())
                            .contentType("application/json")
                            .content(updateBody()))
                    .andExpect(status().isNotFound());
        }

        @Test
        @WithMockUser("bob")
        @DisplayName("EDITOR can update — 200 OK")
        void editor_ok() throws Exception {
            grant(authA, bob, AuthorizationRole.EDITOR, alice);
            mockMvc.perform(put("/api/authorizations/" + authA.getId())
                            .with(csrf())
                            .contentType("application/json")
                            .content(updateBody()))
                    .andExpect(status().isOk());
        }

        @Test
        @WithMockUser("alice")
        @DisplayName("creator (auto-OWNER) can update — 200 OK")
        void creator_ok() throws Exception {
            mockMvc.perform(put("/api/authorizations/" + authA.getId())
                            .with(csrf())
                            .contentType("application/json")
                            .content(updateBody()))
                    .andExpect(status().isOk());
        }

        @Test
        @WithMockUser("eve")
        @DisplayName("ORG_ADMIN (effective OWNER) can update — 200 OK")
        void orgAdmin_ok() throws Exception {
            mockMvc.perform(put("/api/authorizations/" + authA.getId())
                            .with(csrf())
                            .contentType("application/json")
                            .content(updateBody()))
                    .andExpect(status().isOk());
        }

        @Test
        @WithMockUser("carol")
        @DisplayName("cross-org user gets 404 — auth not in carol's org scope")
        void crossOrg_notFound() throws Exception {
            mockMvc.perform(put("/api/authorizations/" + authA.getId())
                            .with(csrf())
                            .contentType("application/json")
                            .content(updateBody()))
                    .andExpect(status().isNotFound());
        }
    }

    // ========================================================================
    // DELETE /api/authorizations/{id}
    // ========================================================================

    @Nested
    @DisplayName("DELETE /api/authorizations/{id}")
    class DeleteAccess {

        @Test
        @WithMockUser("bob")
        @DisplayName("EDITOR cannot delete — InsufficientAuthorizationRoleException message does not match 'Only the creator' → caught as 404")
        void editor_blocked() throws Exception {
            grant(authA, bob, AuthorizationRole.EDITOR, alice);
            // requireDelete throws InsufficientAuthorizationRoleException.
            // The controller only returns 403 when message.contains("Only the creator").
            // Since the message is "Insufficient role…", this falls to the catch-all 404.
            mockMvc.perform(delete("/api/authorizations/" + authA.getId()).with(csrf()))
                    .andExpect(status().isNotFound());
        }

        @Test
        @WithMockUser("bob")
        @DisplayName("OWNER (explicit grant) can delete — 200 OK")
        void owner_ok() throws Exception {
            grant(authA, bob, AuthorizationRole.OWNER, alice);
            mockMvc.perform(delete("/api/authorizations/" + authA.getId()).with(csrf()))
                    .andExpect(status().isOk());
        }

        @Test
        @WithMockUser("alice")
        @DisplayName("creator can delete — 200 OK")
        void creator_ok() throws Exception {
            mockMvc.perform(delete("/api/authorizations/" + authA.getId()).with(csrf()))
                    .andExpect(status().isOk());
        }

        @Test
        @WithMockUser("eve")
        @DisplayName("ORG_ADMIN (effective OWNER) can delete — 200 OK")
        void orgAdmin_ok() throws Exception {
            mockMvc.perform(delete("/api/authorizations/" + authA.getId()).with(csrf()))
                    .andExpect(status().isOk());
        }

        @Test
        @WithMockUser("carol")
        @DisplayName("cross-org user gets 404")
        void crossOrg_notFound() throws Exception {
            mockMvc.perform(delete("/api/authorizations/" + authA.getId()).with(csrf()))
                    .andExpect(status().isNotFound());
        }
    }

    // ========================================================================
    // GET /api/authorizations/{id}/grants  (listGrants)
    // ========================================================================

    @Nested
    @DisplayName("GET /api/authorizations/{id}/grants")
    class ListGrants {

        @Test
        @WithMockUser("alice")
        @DisplayName("OWNER (creator) can list grants — 200 OK")
        void owner_ok() throws Exception {
            mockMvc.perform(get("/api/authorizations/" + authA.getId() + "/grants"))
                    .andExpect(status().isOk());
        }

        @Test
        @WithMockUser("bob")
        @DisplayName("VIEWER cannot list grants — requireManageGrants throws 403")
        void viewer_forbidden() throws Exception {
            grant(authA, bob, AuthorizationRole.VIEWER, alice);
            mockMvc.perform(get("/api/authorizations/" + authA.getId() + "/grants"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser("bob")
        @DisplayName("EDITOR cannot list grants — only OWNER can manage grants")
        void editor_forbidden() throws Exception {
            grant(authA, bob, AuthorizationRole.EDITOR, alice);
            mockMvc.perform(get("/api/authorizations/" + authA.getId() + "/grants"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser("eve")
        @DisplayName("ORG_ADMIN (effective OWNER) can list grants — 200 OK")
        void orgAdmin_ok() throws Exception {
            mockMvc.perform(get("/api/authorizations/" + authA.getId() + "/grants"))
                    .andExpect(status().isOk());
        }
    }

    // ========================================================================
    // POST /api/authorizations/{id}/grants  (addGrant)
    // ========================================================================

    @Nested
    @DisplayName("POST /api/authorizations/{id}/grants")
    class AddGrant {

        @Test
        @WithMockUser("alice")
        @DisplayName("OWNER can add a grant — 201 Created")
        void owner_addGrant_created() throws Exception {
            String body = objectMapper.writeValueAsString(Map.of(
                    "userId", bob.getId(),
                    "role", "EDITOR"));
            mockMvc.perform(post("/api/authorizations/" + authA.getId() + "/grants")
                            .with(csrf())
                            .contentType("application/json")
                            .content(body))
                    .andExpect(status().isCreated());
        }

        @Test
        @WithMockUser("bob")
        @DisplayName("CONTRIBUTOR cannot add a grant — 403 Forbidden")
        void contributor_addGrant_forbidden() throws Exception {
            grant(authA, bob, AuthorizationRole.CONTRIBUTOR, alice);
            String body = objectMapper.writeValueAsString(Map.of(
                    "userId", carol.getId(),
                    "role", "VIEWER"));
            mockMvc.perform(post("/api/authorizations/" + authA.getId() + "/grants")
                            .with(csrf())
                            .contentType("application/json")
                            .content(body))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser("alice")
        @DisplayName("Adding a cross-org grantee is rejected — 400 Bad Request")
        void crossOrgGrantee_rejected() throws Exception {
            // carol is in orgB, not orgA where authA lives
            String body = objectMapper.writeValueAsString(Map.of(
                    "userId", carol.getId(),
                    "role", "VIEWER"));
            mockMvc.perform(post("/api/authorizations/" + authA.getId() + "/grants")
                            .with(csrf())
                            .contentType("application/json")
                            .content(body))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @WithMockUser("alice")
        @DisplayName("Adding a same-org grantee with VIEWER role succeeds — 201")
        void sameOrgGrantee_viewer_created() throws Exception {
            String body = objectMapper.writeValueAsString(Map.of(
                    "userId", bob.getId(),
                    "role", "VIEWER"));
            mockMvc.perform(post("/api/authorizations/" + authA.getId() + "/grants")
                            .with(csrf())
                            .contentType("application/json")
                            .content(body))
                    .andExpect(status().isCreated());
        }
    }

    // ========================================================================
    // PATCH /api/authorizations/{id}/share-with-org
    // ========================================================================

    @Nested
    @DisplayName("PATCH /api/authorizations/{id}/share-with-org")
    class ShareWithOrg {

        @Test
        @WithMockUser("alice")
        @DisplayName("OWNER can set VIEWER as default — 200 OK")
        void owner_setViewer_ok() throws Exception {
            String body = objectMapper.writeValueAsString(Map.of("role", "VIEWER"));
            mockMvc.perform(patch("/api/authorizations/" + authA.getId() + "/share-with-org")
                            .with(csrf())
                            .contentType("application/json")
                            .content(body))
                    .andExpect(status().isOk());
        }

        @Test
        @WithMockUser("alice")
        @DisplayName("OWNER can set EDITOR as default — 200 OK")
        void owner_setEditor_ok() throws Exception {
            String body = objectMapper.writeValueAsString(Map.of("role", "EDITOR"));
            mockMvc.perform(patch("/api/authorizations/" + authA.getId() + "/share-with-org")
                            .with(csrf())
                            .contentType("application/json")
                            .content(body))
                    .andExpect(status().isOk());
        }

        @Test
        @WithMockUser("alice")
        @DisplayName("OWNER cannot set OWNER as default — 400 Bad Request (OWNER not assignable as share default)")
        void owner_setOwner_badRequest() throws Exception {
            String body = objectMapper.writeValueAsString(Map.of("role", "OWNER"));
            mockMvc.perform(patch("/api/authorizations/" + authA.getId() + "/share-with-org")
                            .with(csrf())
                            .contentType("application/json")
                            .content(body))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @WithMockUser("bob")
        @DisplayName("EDITOR cannot change share-with-org — only OWNER can manage grants")
        void editor_forbidden() throws Exception {
            grant(authA, bob, AuthorizationRole.EDITOR, alice);
            String body = objectMapper.writeValueAsString(Map.of("role", "VIEWER"));
            mockMvc.perform(patch("/api/authorizations/" + authA.getId() + "/share-with-org")
                            .with(csrf())
                            .contentType("application/json")
                            .content(body))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser("bob")
        @DisplayName("VIEWER cannot change share-with-org — 403 Forbidden")
        void viewer_forbidden() throws Exception {
            grant(authA, bob, AuthorizationRole.VIEWER, alice);
            String body = objectMapper.writeValueAsString(Map.of("role", "VIEWER"));
            mockMvc.perform(patch("/api/authorizations/" + authA.getId() + "/share-with-org")
                            .with(csrf())
                            .contentType("application/json")
                            .content(body))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser("eve")
        @DisplayName("ORG_ADMIN (effective OWNER) can set share-with-org — 200 OK")
        void orgAdmin_ok() throws Exception {
            String body = objectMapper.writeValueAsString(Map.of("role", "CONTRIBUTOR"));
            mockMvc.perform(patch("/api/authorizations/" + authA.getId() + "/share-with-org")
                            .with(csrf())
                            .contentType("application/json")
                            .content(body))
                    .andExpect(status().isOk());
        }
    }

    // ========================================================================
    // Effective role returned in response body
    // ========================================================================

    @Nested
    @DisplayName("Effective role in response body")
    class EffectiveRoleInResponse {

        @Test
        @WithMockUser("alice")
        @DisplayName("creator sees effectiveRole=OWNER in response")
        void creator_ownerRole() throws Exception {
            mockMvc.perform(get("/api/authorizations/" + authA.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.effectiveRole").value("OWNER"));
        }

        @Test
        @WithMockUser("bob")
        @DisplayName("VIEWER grantee sees effectiveRole=VIEWER in response")
        void viewer_viewerRole() throws Exception {
            grant(authA, bob, AuthorizationRole.VIEWER, alice);
            mockMvc.perform(get("/api/authorizations/" + authA.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.effectiveRole").value("VIEWER"));
        }

        @Test
        @WithMockUser("bob")
        @DisplayName("EDITOR grantee sees effectiveRole=EDITOR in response")
        void editor_editorRole() throws Exception {
            grant(authA, bob, AuthorizationRole.EDITOR, alice);
            mockMvc.perform(get("/api/authorizations/" + authA.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.effectiveRole").value("EDITOR"));
        }

        @Test
        @WithMockUser("bob")
        @DisplayName("shareWithOrg=CONTRIBUTOR gives bob effective CONTRIBUTOR role in response")
        void shareWithOrg_contributorRole() throws Exception {
            authA.setShareWithOrgDefaultRole(AuthorizationRole.CONTRIBUTOR);
            authorizationRepository.save(authA);
            mockMvc.perform(get("/api/authorizations/" + authA.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.effectiveRole").value("CONTRIBUTOR"));
        }

        @Test
        @WithMockUser("bob")
        @DisplayName("explicit grant beats shareWithOrg — EDITOR grant overrides VIEWER org default")
        void grantBeatsShareWithOrg() throws Exception {
            authA.setShareWithOrgDefaultRole(AuthorizationRole.VIEWER);
            authorizationRepository.save(authA);
            grant(authA, bob, AuthorizationRole.EDITOR, alice);
            mockMvc.perform(get("/api/authorizations/" + authA.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.effectiveRole").value("EDITOR"));
        }

        @Test
        @WithMockUser("eve")
        @DisplayName("ORG_ADMIN sees effectiveRole=OWNER in response")
        void orgAdmin_ownerRole() throws Exception {
            mockMvc.perform(get("/api/authorizations/" + authA.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.effectiveRole").value("OWNER"));
        }
    }

    // ========================================================================
    // Helper methods
    // ========================================================================

    private Organization newOrg(String name) {
        Organization o = new Organization();
        o.setName(name);
        return organizationRepository.save(o);
    }

    private User newUser(String username, GlobalRole role) {
        User u = new User();
        u.setUsername(username);
        u.setEmail(username + "@test.example");
        u.setPassword("placeholder-hashed");
        u.setGlobalRole(role);
        return userRepository.save(u);
    }

    private void joinOrg(User user, Organization org, OrganizationRole role) {
        OrganizationMembership m = new OrganizationMembership();
        m.setUser(user);
        m.setOrganization(org);
        m.setRole(role);
        m.setStatus(MembershipStatus.ACTIVE);
        m.setJoinedAt(LocalDateTime.now());
        membershipRepository.save(m);
    }

    private AuthorizationTemplate newTemplate(String name, User creator, Organization org) {
        AuthorizationTemplate t = new AuthorizationTemplate();
        t.setName(name);
        t.setContent("Template body");
        t.setCreatedBy(creator);
        t.setCreatedAt(LocalDateTime.now());
        t.setLastUpdatedAt(LocalDateTime.now());
        t.setOrganization(org);
        return templateRepository.save(t);
    }

    private Authorization newAuthorization(String name, User creator,
                                           AuthorizationTemplate template, Organization org) {
        Authorization a = new Authorization();
        a.setName(name);
        a.setSspItemId("ssp-" + name.replace(" ", "-").toLowerCase());
        a.setTemplate(template);
        a.setAuthorizedBy(creator);
        a.setAuthorizedAt(LocalDateTime.now());
        a.setCreatedAt(LocalDateTime.now());
        a.setVariableValues(new HashMap<>());
        a.setOrganization(org);
        a.setDateExpired(LocalDate.now().plusYears(1));
        a.setSystemOwner("System Owner");
        a.setSecurityManager("Security Manager");
        a.setAuthorizingOfficial("Authorizing Official");
        a.setCompletedContent("Completed authorization body");
        return authorizationRepository.save(a);
    }

    private void grant(Authorization auth, User user, AuthorizationRole role, User grantedBy) {
        AuthorizationGrant g = new AuthorizationGrant(auth, user, role, grantedBy);
        grantRepository.save(g);
    }

    /**
     * Minimal valid update body for PUT /api/authorizations/{id}.
     * Includes all @NotBlank-annotated fields from AuthorizationRequest.
     * Note: templateId and sspItemId are not used by updateAuthorization() but are
     * included to prevent validation failures on the request object.
     */
    private String updateBody() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("name", "Updated Authorization Name");
        body.put("sspItemId", authA.getSspItemId());
        body.put("templateId", templateA.getId());
        body.put("variableValues", Map.of());
        body.put("dateAuthorized", "2026-01-01");
        body.put("dateExpired", "2027-01-01");
        body.put("systemOwner", "Updated System Owner");
        body.put("securityManager", "Updated Security Manager");
        body.put("authorizingOfficial", "Updated Authorizing Official");
        return objectMapper.writeValueAsString(body);
    }
}
