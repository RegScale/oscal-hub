package gov.nist.oscal.tools.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import gov.nist.oscal.tools.api.email.EmailService;
import gov.nist.oscal.tools.api.entity.Invitation;
import gov.nist.oscal.tools.api.entity.Organization;
import gov.nist.oscal.tools.api.entity.OrganizationMembership;
import gov.nist.oscal.tools.api.entity.User;
import gov.nist.oscal.tools.api.model.AcceptInvitationRequest;
import gov.nist.oscal.tools.api.model.CreateInvitationRequest;
import gov.nist.oscal.tools.api.repository.InvitationRepository;
import gov.nist.oscal.tools.api.repository.OrganizationMembershipRepository;
import gov.nist.oscal.tools.api.repository.OrganizationRepository;
import gov.nist.oscal.tools.api.repository.UserRepository;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class InvitationControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired UserRepository userRepo;
    @Autowired OrganizationRepository orgRepo;
    @Autowired OrganizationMembershipRepository memRepo;
    @Autowired InvitationRepository invRepo;
    @Autowired PasswordEncoder passwordEncoder;

    @MockBean EmailService emailService;

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

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

    private User makeUserWithRole(String prefix, String role) {
        User u = makeUser(prefix);
        // Spring Security roles are stored as ROLE_<ROLE> in authorities;
        // @WithMockUser handles this independently, so the User entity role
        // is not used for @PreAuthorize in WebMvc tests.
        return u;
    }

    private Invitation makePendingInvitation(Organization org, User admin, String email) {
        Invitation inv = new Invitation();
        inv.setEmail(email);
        inv.setOrganization(org);
        inv.setInvitedBy(admin);
        inv.setRole(Invitation.Role.USER);
        return invRepo.save(inv);
    }

    // -------------------------------------------------------------------------
    // ORG-ADMIN: POST /api/org-admin/invitations
    // -------------------------------------------------------------------------

    @Test
    @WithMockUser(username = "org-admin-user", roles = {"ORG_ADMIN"})
    void createInvitation_orgAdmin_returns200() throws Exception {
        Organization org = makeOrg();
        User admin = makeUser("admin");
        // Username for @WithMockUser must match a user we can look up
        admin.setUsername("org-admin-user");
        userRepo.save(admin);
        // Give admin the ORG_ADMIN membership so per-org check passes
        memRepo.save(new OrganizationMembership(admin, org, OrganizationMembership.OrganizationRole.ORG_ADMIN));

        CreateInvitationRequest req = new CreateInvitationRequest();
        req.setEmail("invited-" + System.nanoTime() + "@example.com");
        req.setOrganizationId(org.getId());
        req.setRole(Invitation.Role.USER);

        mockMvc.perform(post("/api/org-admin/invitations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.email").value(req.getEmail()))
            .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    @WithMockUser(username = "org-admin-wrong-org", roles = {"ORG_ADMIN"})
    void createInvitation_orgAdmin_wrongOrg_returns403() throws Exception {
        Organization org = makeOrg();
        Organization otherOrg = makeOrg();
        User admin = makeUser("admin");
        admin.setUsername("org-admin-wrong-org");
        userRepo.save(admin);
        // Admin is only a member of otherOrg, not org
        memRepo.save(new OrganizationMembership(admin, otherOrg, OrganizationMembership.OrganizationRole.ORG_ADMIN));

        CreateInvitationRequest req = new CreateInvitationRequest();
        req.setEmail("invited-" + System.nanoTime() + "@example.com");
        req.setOrganizationId(org.getId());
        req.setRole(Invitation.Role.USER);

        mockMvc.perform(post("/api/org-admin/invitations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.error").value("FORBIDDEN"));
    }

    @Test
    @WithMockUser(username = "plain-user", roles = {"USER"})
    void createInvitation_nonAdmin_returns403() throws Exception {
        Organization org = makeOrg();

        CreateInvitationRequest req = new CreateInvitationRequest();
        req.setEmail("invited-" + System.nanoTime() + "@example.com");
        req.setOrganizationId(org.getId());

        mockMvc.perform(post("/api/org-admin/invitations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "org-admin-conflict", roles = {"ORG_ADMIN"})
    void createInvitation_alreadyMember_returns409() throws Exception {
        Organization org = makeOrg();
        User admin = makeUser("admin");
        admin.setUsername("org-admin-conflict");
        userRepo.save(admin);
        // Give admin the ORG_ADMIN membership so per-org check passes
        memRepo.save(new OrganizationMembership(admin, org, OrganizationMembership.OrganizationRole.ORG_ADMIN));

        // Create an existing active member with a distinct email
        User existingMember = makeUser("existing");
        memRepo.save(new OrganizationMembership(
            existingMember, org, OrganizationMembership.OrganizationRole.USER));

        CreateInvitationRequest req = new CreateInvitationRequest();
        req.setEmail(existingMember.getEmail());
        req.setOrganizationId(org.getId());

        mockMvc.perform(post("/api/org-admin/invitations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.error").value("ALREADY_MEMBER"));
    }

    // -------------------------------------------------------------------------
    // PUBLIC: GET /api/invitations/{token}
    // -------------------------------------------------------------------------

    @Test
    void viewInvitation_validToken_returns200() throws Exception {
        Organization org = makeOrg();
        User admin = makeUser("admin");
        Invitation inv = makePendingInvitation(org, admin,
            "view-" + System.nanoTime() + "@example.com");

        mockMvc.perform(get("/api/invitations/" + inv.getToken()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("PENDING"))
            .andExpect(jsonPath("$.organizationName").value(org.getName()))
            .andExpect(jsonPath("$.inviterName").value(admin.getUsername()));
    }

    @Test
    void viewInvitation_expiredToken_returns410() throws Exception {
        Organization org = makeOrg();
        User admin = makeUser("admin");
        Invitation inv = makePendingInvitation(org, admin,
            "expired-" + System.nanoTime() + "@example.com");
        inv.setExpiresAt(LocalDateTime.now().minusDays(1));
        invRepo.save(inv);

        mockMvc.perform(get("/api/invitations/" + inv.getToken()))
            .andExpect(status().isGone())
            .andExpect(jsonPath("$.error").value("INVITATION_EXPIRED"));
    }

    @Test
    void viewInvitation_unknownToken_returns404() throws Exception {
        mockMvc.perform(get("/api/invitations/nonexistent-token-" + System.nanoTime()))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.error").value("INVITATION_NOT_FOUND"));
    }

    // -------------------------------------------------------------------------
    // PUBLIC: POST /api/invitations/{token}/accept
    // -------------------------------------------------------------------------

    @Test
    void acceptInvitation_newUser_returns200AndCreatesMembership() throws Exception {
        Organization org = makeOrg();
        User admin = makeUser("admin");
        Invitation inv = makePendingInvitation(org, admin,
            "newuser-" + System.nanoTime() + "@example.com");

        AcceptInvitationRequest req = new AcceptInvitationRequest();
        req.setUsername("newacceptuser-" + System.nanoTime());
        req.setPassword("CorrectH0rse!Batt");

        mockMvc.perform(post("/api/invitations/" + inv.getToken() + "/accept")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.userId").isNumber())
            .andExpect(jsonPath("$.username").value(req.getUsername()))
            .andExpect(jsonPath("$.token").isString());
    }

    @Test
    void acceptInvitation_missingPassword_returns400() throws Exception {
        Organization org = makeOrg();
        User admin = makeUser("admin");
        Invitation inv = makePendingInvitation(org, admin,
            "nopw-" + System.nanoTime() + "@example.com");

        AcceptInvitationRequest req = new AcceptInvitationRequest();
        req.setUsername("someuser-" + System.nanoTime());
        // password intentionally omitted

        mockMvc.perform(post("/api/invitations/" + inv.getToken() + "/accept")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("VALIDATION"));
    }

    @Test
    void acceptInvitation_expiredToken_returns410() throws Exception {
        Organization org = makeOrg();
        User admin = makeUser("admin");
        Invitation inv = makePendingInvitation(org, admin,
            "lateaccept-" + System.nanoTime() + "@example.com");
        inv.setExpiresAt(LocalDateTime.now().minusDays(1));
        invRepo.save(inv);

        AcceptInvitationRequest req = new AcceptInvitationRequest();
        req.setUsername("latecomer-" + System.nanoTime());
        req.setPassword("CorrectH0rse!Batt");

        mockMvc.perform(post("/api/invitations/" + inv.getToken() + "/accept")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isGone())
            .andExpect(jsonPath("$.error").value("INVITATION_EXPIRED"));
    }
}
