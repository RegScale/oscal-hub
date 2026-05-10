package gov.nist.oscal.tools.api.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import gov.nist.oscal.tools.api.entity.Authorization;
import gov.nist.oscal.tools.api.entity.AuthorizationGrant;
import gov.nist.oscal.tools.api.entity.AuthorizationRole;
import gov.nist.oscal.tools.api.entity.AuthorizationTemplate;
import gov.nist.oscal.tools.api.entity.DocumentType;
import gov.nist.oscal.tools.api.entity.Organization;
import gov.nist.oscal.tools.api.entity.OrganizationMembership;
import gov.nist.oscal.tools.api.entity.OrganizationMembership.MembershipStatus;
import gov.nist.oscal.tools.api.entity.OrganizationMembership.OrganizationRole;
import gov.nist.oscal.tools.api.entity.User;
import gov.nist.oscal.tools.api.entity.User.GlobalRole;
import gov.nist.oscal.tools.api.repository.AuthorizationDocumentRepository;
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
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("Authorization Documents end-to-end")
class AuthorizationDocumentsIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private EntityManager entityManager;
    @Autowired private UserRepository userRepository;
    @Autowired private OrganizationRepository organizationRepository;
    @Autowired private OrganizationMembershipRepository membershipRepository;
    @Autowired private AuthorizationTemplateRepository templateRepository;
    @Autowired private AuthorizationRepository authorizationRepository;
    @Autowired private AuthorizationGrantRepository grantRepository;
    @Autowired private AuthorizationDocumentRepository documentRepository;

    private Organization orgA;
    private User alice;
    private User bob;
    private User carol;
    private Authorization authA;

    @BeforeEach
    void setUp() {
        // @Transactional rolls back after each test; no manual deletes needed.
        orgA = newOrg("Org A");
        Organization orgB = newOrg("Org B");

        alice = newUser("alice");
        bob   = newUser("bob");
        carol = newUser("carol");

        joinOrg(alice, orgA, OrganizationRole.USER);
        joinOrg(bob,   orgA, OrganizationRole.USER);
        joinOrg(carol, orgB, OrganizationRole.USER);

        AuthorizationTemplate t = newTemplate("T", alice, orgA);
        authA = newAuthorization("A", alice, t, orgA);

        // Flush all pending writes and clear the first-level cache so subsequent
        // entity loads (e.g., lazy collections in isInSameOrg()) re-fetch from DB.
        entityManager.flush();
        entityManager.clear();

        alice = userRepository.findByUsername("alice").orElseThrow();
        bob   = userRepository.findByUsername("bob").orElseThrow();
        carol = userRepository.findByUsername("carol").orElseThrow();
        authA = authorizationRepository.findById(authA.getId()).orElseThrow();
    }

    // ========================================================================
    // POST /api/authorizations/{id}/documents
    // ========================================================================

    @Nested
    @DisplayName("POST /api/authorizations/{id}/documents")
    class Upload {

        @Test
        @WithMockUser("alice")
        @DisplayName("OWNER uploads a PDF — 201 with metadata")
        void owner_uploadsPdf_created() throws Exception {
            MockMultipartFile file = new MockMultipartFile(
                    "file", "pen-test.pdf", "application/pdf", "PDF body".getBytes());

            mockMvc.perform(multipart("/api/authorizations/" + authA.getId() + "/documents")
                            .file(file)
                            .param("documentType", "PENETRATION_TEST")
                            .param("description", "Q3 pen test")
                            .with(csrf()))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.originalFilename").value("pen-test.pdf"))
                    .andExpect(jsonPath("$.documentType").value("PENETRATION_TEST"));
        }

        @Test
        @WithMockUser("bob")
        @DisplayName("VIEWER cannot upload — 403")
        void viewer_blocked() throws Exception {
            grant(authA, bob, AuthorizationRole.VIEWER, alice);
            MockMultipartFile file = new MockMultipartFile(
                    "file", "x.pdf", "application/pdf", "x".getBytes());

            mockMvc.perform(multipart("/api/authorizations/" + authA.getId() + "/documents")
                            .file(file)
                            .param("documentType", "OTHER")
                            .with(csrf()))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser("bob")
        @DisplayName("CONTRIBUTOR can upload — 201")
        void contributor_canUpload() throws Exception {
            grant(authA, bob, AuthorizationRole.CONTRIBUTOR, alice);
            MockMultipartFile file = new MockMultipartFile(
                    "file", "scan.pdf", "application/pdf", "scan".getBytes());

            mockMvc.perform(multipart("/api/authorizations/" + authA.getId() + "/documents")
                            .file(file)
                            .param("documentType", "VULNERABILITY_SCAN")
                            .with(csrf()))
                    .andExpect(status().isCreated());
        }

        @Test
        @WithMockUser("alice")
        @DisplayName("Executable rejected — 400")
        void executable_rejected() throws Exception {
            MockMultipartFile file = new MockMultipartFile(
                    "file", "evil.exe", "application/x-msdownload", "MZ".getBytes());

            mockMvc.perform(multipart("/api/authorizations/" + authA.getId() + "/documents")
                            .file(file)
                            .param("documentType", "OTHER")
                            .with(csrf()))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @WithMockUser("carol")
        @DisplayName("cross-org user gets 404")
        void crossOrg_notFound() throws Exception {
            MockMultipartFile file = new MockMultipartFile(
                    "file", "x.pdf", "application/pdf", "x".getBytes());

            mockMvc.perform(multipart("/api/authorizations/" + authA.getId() + "/documents")
                            .file(file)
                            .param("documentType", "OTHER")
                            .with(csrf()))
                    .andExpect(status().isNotFound());
        }
    }

    // ========================================================================
    // GET /api/authorizations/{id}/documents
    // ========================================================================

    @Nested
    @DisplayName("GET /api/authorizations/{id}/documents")
    class Listing {

        @Test
        @WithMockUser("alice")
        @DisplayName("OWNER lists their uploads")
        void owner_listsOwn() throws Exception {
            uploadAs("alice", DocumentType.SSP);

            mockMvc.perform(get("/api/authorizations/" + authA.getId() + "/documents"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].documentType").value("SSP"));
        }

        @Test
        @WithMockUser("bob")
        @DisplayName("Same-org user without grant gets 404 (private by default)")
        void sameOrgNoGrant_notFound() throws Exception {
            mockMvc.perform(get("/api/authorizations/" + authA.getId() + "/documents"))
                    .andExpect(status().isNotFound());
        }

        @Test
        @WithMockUser("bob")
        @DisplayName("VIEWER grant: can list documents")
        void viewerGrant_canList() throws Exception {
            grant(authA, bob, AuthorizationRole.VIEWER, alice);
            uploadAs("alice", DocumentType.SSP);

            mockMvc.perform(get("/api/authorizations/" + authA.getId() + "/documents"))
                    .andExpect(status().isOk());
        }

        @Test
        @WithMockUser("alice")
        @DisplayName("Filter by type")
        void filter_byType() throws Exception {
            uploadAs("alice", DocumentType.SSP);
            uploadAs("alice", DocumentType.AUDIT_REPORT);

            mockMvc.perform(get("/api/authorizations/" + authA.getId() + "/documents")
                            .param("type", "SSP"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].documentType").value("SSP"));
        }
    }

    // ========================================================================
    // DELETE /api/authorizations/{id}/documents/{docId}
    // ========================================================================

    @Nested
    @DisplayName("DELETE /api/authorizations/{id}/documents/{docId}")
    class Delete {

        @Test
        @WithMockUser("alice")
        @DisplayName("OWNER deletes any document")
        void owner_deletesAny() throws Exception {
            Long docId = uploadAs("alice", DocumentType.SSP);

            mockMvc.perform(delete("/api/authorizations/" + authA.getId() + "/documents/" + docId)
                            .with(csrf()))
                    .andExpect(status().isNoContent());
        }

        @Test
        @WithMockUser("bob")
        @DisplayName("CONTRIBUTOR can delete their own upload")
        void contributor_deletesOwn() throws Exception {
            grant(authA, bob, AuthorizationRole.CONTRIBUTOR, alice);
            Long docId = uploadAs("bob", DocumentType.OTHER);

            mockMvc.perform(delete("/api/authorizations/" + authA.getId() + "/documents/" + docId)
                            .with(csrf()))
                    .andExpect(status().isNoContent());
        }

        @Test
        @WithMockUser("bob")
        @DisplayName("CONTRIBUTOR cannot delete someone else's upload — 403")
        void contributor_cantDeleteOthers() throws Exception {
            grant(authA, bob, AuthorizationRole.CONTRIBUTOR, alice);
            Long aliceDocId = uploadAs("alice", DocumentType.SSP);

            mockMvc.perform(delete("/api/authorizations/" + authA.getId() + "/documents/" + aliceDocId)
                            .with(csrf()))
                    .andExpect(status().isForbidden());
        }
    }

    // ========================================================================
    // GET /api/authorizations/{id}/documents/completeness
    // ========================================================================

    @Nested
    @DisplayName("GET /api/authorizations/{id}/documents/completeness")
    class Completeness {

        @Test
        @WithMockUser("alice")
        @DisplayName("Counts present and missing core types")
        void counts_present_and_missing() throws Exception {
            uploadAs("alice", DocumentType.SSP);
            uploadAs("alice", DocumentType.SAR);

            mockMvc.perform(get("/api/authorizations/" + authA.getId() + "/documents/completeness"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.coreDocuments[?(@.documentType == 'SSP')].satisfied").value(true))
                    .andExpect(jsonPath("$.coreDocuments[?(@.documentType == 'SAR')].satisfied").value(true))
                    .andExpect(jsonPath("$.coreDocuments[?(@.documentType == 'PENETRATION_TEST')].satisfied").value(false));
        }

        @Test
        @WithMockUser("alice")
        @DisplayName("Expired documents are NOT counted")
        void expired_notCounted() throws Exception {
            Long docId = uploadAs("alice", DocumentType.PENETRATION_TEST);
            documentRepository.findById(docId).ifPresent(d -> {
                d.setExpiresAt(LocalDate.now().minusDays(1));
                documentRepository.save(d);
            });
            entityManager.flush();
            entityManager.clear();

            mockMvc.perform(get("/api/authorizations/" + authA.getId() + "/documents/completeness"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.coreDocuments[?(@.documentType == 'PENETRATION_TEST')].satisfied").value(false));
        }
    }

    // ========================================================================
    // Helper methods
    // ========================================================================

    /**
     * Performs an upload via MockMvc as the given user and returns the created document's id.
     * Uses SecurityMockMvcRequestPostProcessors.user() so the upload runs as that user
     * regardless of the @WithMockUser annotation on the enclosing test.
     */
    private Long uploadAs(String username, DocumentType type) throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "doc.pdf", "application/pdf", "body".getBytes());

        MvcResult result = mockMvc.perform(
                multipart("/api/authorizations/" + authA.getId() + "/documents")
                        .file(file)
                        .param("documentType", type.name())
                        .with(csrf())
                        .with(user(username))
        ).andReturn();

        JsonNode node = objectMapper.readTree(result.getResponse().getContentAsString());
        return node.get("id").asLong();
    }

    private Organization newOrg(String name) {
        Organization o = new Organization();
        o.setName(name);
        return organizationRepository.save(o);
    }

    private User newUser(String username) {
        User u = new User();
        u.setUsername(username);
        u.setEmail(username + "@test.example");
        u.setPassword("placeholder-hashed");
        u.setGlobalRole(GlobalRole.USER);
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

    private Authorization newAuthorization(String name, User creator, AuthorizationTemplate template, Organization org) {
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
}
