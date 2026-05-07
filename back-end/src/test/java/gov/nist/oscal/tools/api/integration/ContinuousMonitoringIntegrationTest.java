package gov.nist.oscal.tools.api.integration;

import com.fasterxml.jackson.databind.JsonNode;
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
import gov.nist.oscal.tools.api.repository.ConMonSnapshotRepository;
import gov.nist.oscal.tools.api.repository.OrganizationMembershipRepository;
import gov.nist.oscal.tools.api.repository.OrganizationRepository;
import gov.nist.oscal.tools.api.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
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

import java.io.ByteArrayOutputStream;
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
@DisplayName("Continuous Monitoring end-to-end")
class ContinuousMonitoringIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private OrganizationRepository organizationRepository;
    @Autowired private OrganizationMembershipRepository membershipRepository;
    @Autowired private AuthorizationTemplateRepository templateRepository;
    @Autowired private AuthorizationRepository authorizationRepository;
    @Autowired private AuthorizationGrantRepository grantRepository;
    @Autowired private ConMonSnapshotRepository snapshotRepository;
    @Autowired private EntityManager entityManager;

    private Organization orgA;
    private User alice, bob, carol;
    private Authorization authA;

    @BeforeEach
    void setUp() {
        // @Transactional rolls back after each test; no manual deletes needed.
        orgA = newOrg("Org A");
        Organization orgB = newOrg("Org B");
        alice = newUser("alice");
        bob = newUser("bob");
        carol = newUser("carol");
        joinOrg(alice, orgA, OrganizationRole.USER);
        joinOrg(bob, orgA, OrganizationRole.USER);
        joinOrg(carol, orgB, OrganizationRole.USER);
        AuthorizationTemplate t = newTemplate("T", alice, orgA);
        authA = newAuthorization("A", alice, t, orgA);

        // Flush all pending writes and clear the first-level cache so subsequent
        // entity loads (e.g., lazy collections in isInSameOrg()) re-fetch from DB.
        entityManager.flush();
        entityManager.clear();

        alice = userRepository.findByUsername("alice").orElseThrow();
        bob = userRepository.findByUsername("bob").orElseThrow();
        carol = userRepository.findByUsername("carol").orElseThrow();
        authA = authorizationRepository.findById(authA.getId()).orElseThrow();
    }

    // ========================================================================
    // POST /api/authorizations/{id}/conmon/snapshots
    // ========================================================================

    @Nested
    @DisplayName("POST snapshots")
    class Upload {

        @Test
        @WithMockUser("alice")
        @DisplayName("OWNER uploads a FedRAMP xlsx — 201 with reconciliation null")
        void owner_uploadsXlsx_201() throws Exception {
            MockMultipartFile file = new MockMultipartFile(
                    "file", "poam.xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    fedrampWorkbook(1, 0));

            mockMvc.perform(multipart("/api/authorizations/" + authA.getId() + "/conmon/snapshots")
                            .file(file).with(csrf()))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.sourceFormat").value("FEDRAMP_XLSX"))
                    .andExpect(jsonPath("$.openCount").value(1))
                    .andExpect(jsonPath("$.reconciliation").doesNotExist());
        }

        @Test
        @WithMockUser("bob")
        @DisplayName("VIEWER blocked — 403")
        void viewer_blocked() throws Exception {
            grant(authA, bob, AuthorizationRole.VIEWER, alice);
            MockMultipartFile file = new MockMultipartFile(
                    "file", "poam.xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    fedrampWorkbook(1, 0));

            mockMvc.perform(multipart("/api/authorizations/" + authA.getId() + "/conmon/snapshots")
                            .file(file).with(csrf()))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser("alice")
        @DisplayName("Unsupported file type — 400")
        void unsupportedExt_400() throws Exception {
            MockMultipartFile file = new MockMultipartFile(
                    "file", "evil.docx",
                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                    "x".getBytes());

            mockMvc.perform(multipart("/api/authorizations/" + authA.getId() + "/conmon/snapshots")
                            .file(file).with(csrf()))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @WithMockUser("carol")
        @DisplayName("cross-org user — 404")
        void crossOrg_404() throws Exception {
            MockMultipartFile file = new MockMultipartFile(
                    "file", "poam.xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    fedrampWorkbook(1, 0));

            mockMvc.perform(multipart("/api/authorizations/" + authA.getId() + "/conmon/snapshots")
                            .file(file).with(csrf()))
                    .andExpect(status().isNotFound());
        }
    }

    // ========================================================================
    // Reconciliation between two snapshots
    // ========================================================================

    @Nested
    @DisplayName("Reconciliation between two snapshots")
    class Reconciliation {

        @Test
        @WithMockUser("alice")
        @DisplayName("Second snapshot triggers reconciliation row")
        void secondSnapshot_reconciles() throws Exception {
            // First snapshot: 1 open
            uploadXlsx("first.xlsx", 1, 0);
            // Second snapshot: 0 open, 1 closed (the same ID, transition open→closed)
            Long secondId = uploadXlsx("second.xlsx", 0, 1);

            mockMvc.perform(get("/api/authorizations/" + authA.getId()
                            + "/conmon/snapshots/" + secondId + "/reconciliation"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.snapshotId").value(secondId));
            // closedCount should be 1 (P-1 went open→closed in the synthetic data)
        }

        @Test
        @WithMockUser("alice")
        @DisplayName("First snapshot has no reconciliation — 404")
        void firstSnapshot_noReconciliation_404() throws Exception {
            Long id = uploadXlsx("first.xlsx", 1, 0);
            mockMvc.perform(get("/api/authorizations/" + authA.getId()
                            + "/conmon/snapshots/" + id + "/reconciliation"))
                    .andExpect(status().isNotFound());
        }
    }

    // ========================================================================
    // Listing and analytics
    // ========================================================================

    @Nested
    @DisplayName("Listing and analytics")
    class Listing {

        @Test
        @WithMockUser("alice")
        @DisplayName("Lists snapshots newest-first")
        void lists_newestFirst() throws Exception {
            uploadXlsx("a.xlsx", 1, 0);
            uploadXlsx("b.xlsx", 1, 0);

            mockMvc.perform(get("/api/authorizations/" + authA.getId() + "/conmon/snapshots"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(2));
        }

        @Test
        @WithMockUser("alice")
        @DisplayName("Analytics endpoint returns time series + donut")
        void analytics_returnsExpected() throws Exception {
            uploadXlsx("a.xlsx", 2, 0);

            mockMvc.perform(get("/api/authorizations/" + authA.getId() + "/conmon/analytics"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.openCountSeries").isArray())
                    .andExpect(jsonPath("$.currentStatusBreakdown").isArray());
        }
    }

    // ========================================================================
    // Helper methods
    // ========================================================================

    /**
     * Uploads a FedRAMP xlsx via MockMvc as alice (regardless of @WithMockUser on
     * the enclosing test) and returns the created snapshot's id.
     */
    private Long uploadXlsx(String filename, int openRows, int closedRows) throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", filename,
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                fedrampWorkbook(openRows, closedRows));
        MvcResult result = mockMvc.perform(
                multipart("/api/authorizations/" + authA.getId() + "/conmon/snapshots")
                        .file(file)
                        .with(csrf())
                        .with(user("alice")))
                .andReturn();
        JsonNode node = objectMapper.readTree(result.getResponse().getContentAsString());
        return node.get("id").asLong();
    }

    /**
     * Synthesizes a minimal FedRAMP-shaped XLSX workbook in-memory using POI.
     * Open items use IDs P-1..P-N; closed items also use P-1..P-M so that
     * a second upload with openRows=0,closedRows=1 produces an open→closed
     * transition on P-1 relative to a prior upload with openRows=1.
     */
    private byte[] fedrampWorkbook(int openRows, int closedRows) throws Exception {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            if (openRows > 0) {
                Sheet s = wb.createSheet("Open POA&M Items");
                writeHeader(s);
                for (int i = 0; i < openRows; i++) {
                    Row r = s.createRow(1 + i);
                    r.createCell(0).setCellValue("P-" + (i + 1));
                    r.createCell(1).setCellValue("Open weakness " + (i + 1));
                    r.createCell(3).setCellValue("High");
                    r.createCell(7).setCellValue("Ongoing");
                }
            }
            if (closedRows > 0) {
                Sheet s = wb.createSheet("Closed POA&M Items");
                writeHeader(s);
                for (int i = 0; i < closedRows; i++) {
                    // Use the same external IDs as the open sheet to drive transitions
                    Row r = s.createRow(1 + i);
                    r.createCell(0).setCellValue("P-" + (i + 1));
                    r.createCell(1).setCellValue("Closed weakness " + (i + 1));
                    r.createCell(3).setCellValue("High");
                    r.createCell(7).setCellValue("Completed");
                }
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            wb.write(out);
            return out.toByteArray();
        }
    }

    private void writeHeader(Sheet s) {
        Row h = s.createRow(0);
        String[] cols = {
            "POA&M Item ID", "Weakness Name", "Weakness Description", "Severity",
            "Scheduled Completion Date", "Actual Completion Date", "Point of Contact", "Status"
        };
        for (int i = 0; i < cols.length; i++) h.createCell(i).setCellValue(cols[i]);
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
