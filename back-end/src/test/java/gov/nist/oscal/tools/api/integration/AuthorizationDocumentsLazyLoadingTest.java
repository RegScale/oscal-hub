package gov.nist.oscal.tools.api.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import gov.nist.oscal.tools.api.entity.Authorization;
import gov.nist.oscal.tools.api.entity.AuthorizationTemplate;
import gov.nist.oscal.tools.api.entity.DocumentType;
import gov.nist.oscal.tools.api.entity.Organization;
import gov.nist.oscal.tools.api.entity.OrganizationMembership;
import gov.nist.oscal.tools.api.entity.OrganizationMembership.MembershipStatus;
import gov.nist.oscal.tools.api.entity.OrganizationMembership.OrganizationRole;
import gov.nist.oscal.tools.api.entity.User;
import gov.nist.oscal.tools.api.entity.User.GlobalRole;
import gov.nist.oscal.tools.api.repository.AuthorizationDocumentRepository;
import gov.nist.oscal.tools.api.repository.AuthorizationRepository;
import gov.nist.oscal.tools.api.repository.AuthorizationTemplateRepository;
import gov.nist.oscal.tools.api.repository.OrganizationMembershipRepository;
import gov.nist.oscal.tools.api.repository.OrganizationRepository;
import gov.nist.oscal.tools.api.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Regression test for the LazyInitializationException bug in
 * GET /api/authorizations/{id}/documents.
 *
 * <p>The root cause: {@link gov.nist.oscal.tools.api.service.AuthorizationDocumentService#list}
 * is {@code @Transactional(readOnly=true)}, but the controller mapped its result to
 * {@code AuthorizationDocumentResponse} <em>after</em> the transaction boundary closed, causing
 * {@code doc.getAuthorization().getId()} and {@code doc.getUploadedBy().getUsername()} to throw
 * {@code LazyInitializationException} on the LAZY-fetched {@code @ManyToOne} associations.
 * The same pattern existed on the single-document GET / PATCH / DELETE paths.
 *
 * <p><strong>This test class intentionally does NOT carry {@code @Transactional} at the class
 * level.</strong> Unlike the sibling {@code AuthorizationDocumentsIntegrationTest} (which is
 * {@code @Transactional} and therefore keeps one outer transaction open across both the upload
 * and the list call — masking the bug), each MockMvc call here runs in its own request-scoped
 * transaction, matching production behaviour. Data is cleaned up manually in {@link #tearDown}.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Authorization Documents — LazyInitializationException regression")
class AuthorizationDocumentsLazyLoadingTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private OrganizationRepository organizationRepository;
    @Autowired private OrganizationMembershipRepository membershipRepository;
    @Autowired private AuthorizationTemplateRepository templateRepository;
    @Autowired private AuthorizationRepository authorizationRepository;
    @Autowired private AuthorizationDocumentRepository documentRepository;

    private Organization org;
    private User alice;
    private Authorization auth;

    @BeforeEach
    void setUp() {
        org = newOrg("LazyOrg-" + System.nanoTime());
        alice = newUser("lazy-alice-" + System.nanoTime());
        joinOrg(alice, org, OrganizationRole.USER);
        AuthorizationTemplate tmpl = newTemplate("T", alice, org);
        auth = newAuthorization("A", alice, tmpl, org);
    }

    @AfterEach
    void tearDown() {
        // Clean up in reverse dependency order.
        documentRepository.deleteAll(documentRepository.findByAuthorizationOrderByUploadedAtDesc(auth));
        authorizationRepository.delete(auth);
        templateRepository.findAll().stream()
                .filter(t -> t.getOrganization() != null && t.getOrganization().getId().equals(org.getId()))
                .forEach(templateRepository::delete);
        membershipRepository.deleteAll(membershipRepository.findByUser(alice));
        userRepository.delete(alice);
        organizationRepository.delete(org);
    }

    // -------------------------------------------------------------------------
    // The regression test: upload then list in separate request transactions.
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("GET /documents returns 200 with authorizationId and uploadedByUsername populated " +
                 "(no LazyInitializationException) — each call is its own transaction")
    void listDocuments_afterUpload_noLazyInitException() throws Exception {
        // STEP 1 — upload a document.  This runs in its own request transaction.
        MockMultipartFile file = new MockMultipartFile(
                "file", "ssp.pdf", "application/pdf", "PDF content".getBytes());

        MvcResult uploadResult = mockMvc.perform(
                multipart("/api/authorizations/" + auth.getId() + "/documents")
                        .file(file)
                        .param("documentType", DocumentType.SSP.name())
                        .param("description", "System Security Plan")
                        .with(csrf())
                        .with(user(alice.getUsername())))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode created = objectMapper.readTree(uploadResult.getResponse().getContentAsString());
        long docId = created.get("id").asLong();

        // STEP 2 — list documents.  Separate request transaction.
        // Before the fix this threw LazyInitializationException → 500.
        // After the fix the service force-loads the lazy associations inside its own
        // @Transactional(readOnly=true) boundary before returning.
        mockMvc.perform(
                get("/api/authorizations/" + auth.getId() + "/documents")
                        .with(user(alice.getUsername())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(docId))
                .andExpect(jsonPath("$[0].documentType").value("SSP"))
                .andExpect(jsonPath("$[0].authorizationId").value(auth.getId()))
                .andExpect(jsonPath("$[0].uploadedByUsername").value(alice.getUsername()));

        // STEP 3 — GET single document by id.  Same lazy-loading issue on the
        // single-doc path (via documentRepository.findByIdAndAuthorization).
        mockMvc.perform(
                get("/api/authorizations/" + auth.getId() + "/documents/" + docId)
                        .with(user(alice.getUsername())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(docId))
                .andExpect(jsonPath("$.authorizationId").value(auth.getId()))
                .andExpect(jsonPath("$.uploadedByUsername").value(alice.getUsername()));
    }

    @Test
    @DisplayName("GET /documents with type filter returns correct document " +
                 "(no LazyInitializationException) after a separate upload transaction")
    void listDocuments_withTypeFilter_noLazyInitException() throws Exception {
        // Upload two docs of different types in separate calls.
        uploadDoc(DocumentType.SSP);
        uploadDoc(DocumentType.SAR);

        // Filter by SSP — previously crashed with LIAE because the lazy authorizationId
        // and uploadedBy were accessed after the service transaction closed.
        mockMvc.perform(
                get("/api/authorizations/" + auth.getId() + "/documents")
                        .param("type", "SSP")
                        .with(user(alice.getUsername())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].documentType").value("SSP"))
                .andExpect(jsonPath("$[0].uploadedByUsername").value(alice.getUsername()));
    }

    // -------------------------------------------------------------------------
    // Helper methods
    // -------------------------------------------------------------------------

    private void uploadDoc(DocumentType type) throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "doc.pdf", "application/pdf", "body".getBytes());
        MvcResult result = mockMvc.perform(
                multipart("/api/authorizations/" + auth.getId() + "/documents")
                        .file(file)
                        .param("documentType", type.name())
                        .with(csrf())
                        .with(user(alice.getUsername())))
                .andReturn();
        assertThat(result.getResponse().getStatus())
                .as("Upload of " + type + " should be 201")
                .isEqualTo(201);
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

    private void joinOrg(User u, Organization o, OrganizationRole role) {
        OrganizationMembership m = new OrganizationMembership();
        m.setUser(u);
        m.setOrganization(o);
        m.setRole(role);
        m.setStatus(MembershipStatus.ACTIVE);
        m.setJoinedAt(LocalDateTime.now());
        membershipRepository.save(m);
    }

    private AuthorizationTemplate newTemplate(String name, User creator, Organization o) {
        AuthorizationTemplate t = new AuthorizationTemplate();
        t.setName(name);
        t.setContent("Template body");
        t.setCreatedBy(creator);
        t.setCreatedAt(LocalDateTime.now());
        t.setLastUpdatedAt(LocalDateTime.now());
        t.setOrganization(o);
        return templateRepository.save(t);
    }

    private Authorization newAuthorization(String name, User creator, AuthorizationTemplate tmpl, Organization o) {
        Authorization a = new Authorization();
        a.setName(name);
        a.setSspItemId("ssp-" + name.replace(" ", "-").toLowerCase() + "-" + System.nanoTime());
        a.setTemplate(tmpl);
        a.setAuthorizedBy(creator);
        a.setAuthorizedAt(LocalDateTime.now());
        a.setCreatedAt(LocalDateTime.now());
        a.setVariableValues(new HashMap<>());
        a.setOrganization(o);
        a.setDateExpired(LocalDate.now().plusYears(1));
        a.setSystemOwner("System Owner");
        a.setSecurityManager("Security Manager");
        a.setAuthorizingOfficial("Authorizing Official");
        a.setCompletedContent("Completed authorization body");
        return authorizationRepository.save(a);
    }
}
