package gov.nist.oscal.tools.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import gov.nist.oscal.tools.api.config.RateLimitConfig;
import gov.nist.oscal.tools.api.config.SecurityHeadersConfig;
import gov.nist.oscal.tools.api.entity.AiSession;
import gov.nist.oscal.tools.api.entity.AiSessionMode;
import gov.nist.oscal.tools.api.entity.OrganizationMembership;
import gov.nist.oscal.tools.api.entity.User;
import gov.nist.oscal.tools.api.entity.WizardKind;
import gov.nist.oscal.tools.api.model.ai.StartSessionRequest;
import gov.nist.oscal.tools.api.repository.AiSessionRepository;
import gov.nist.oscal.tools.api.repository.OrganizationMembershipRepository;
import gov.nist.oscal.tools.api.repository.UserRepository;
import gov.nist.oscal.tools.api.security.JwtUtil;
import gov.nist.oscal.tools.api.service.RateLimitService;
import gov.nist.oscal.tools.api.service.ai.AiOrchestrator;
import gov.nist.oscal.tools.api.service.ai.stream.AiSessionEventStream;
import gov.nist.oscal.tools.api.telemetry.TelemetryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AiSessionController.class)
class AiSessionControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockitoBean private AiOrchestrator orchestrator;
    @MockitoBean private AiSessionEventStream stream;
    @MockitoBean private UserRepository users;
    @MockitoBean private OrganizationMembershipRepository memberships;
    @MockitoBean private AiSessionRepository sessions;
    @MockitoBean private JwtUtil jwtUtil;
    @MockitoBean private UserDetailsService userDetailsService;
    @MockitoBean private RateLimitService rateLimitService;
    @MockitoBean private RateLimitConfig rateLimitConfig;
    @MockitoBean private SecurityHeadersConfig securityHeadersConfig;
    @MockitoBean private TelemetryService telemetryService;

    @Test
    @WithMockUser(username = "alice", roles = "USER")
    void startSessionReturnsSessionId() throws Exception {
        UUID id = UUID.randomUUID();

        User alice = new User();
        alice.setId(42L);
        alice.setUsername("alice");
        alice.setGlobalRole(User.GlobalRole.USER);
        when(users.findByUsername(eq("alice"))).thenReturn(Optional.of(alice));

        OrganizationMembership membership = new OrganizationMembership();
        membership.setStatus(OrganizationMembership.MembershipStatus.ACTIVE);
        when(memberships.findByUserIdAndOrganizationId(eq(42L), eq(1L)))
                .thenReturn(Optional.of(membership));

        when(orchestrator.start(eq(1L), eq(42L), eq(WizardKind.SMOKE), eq(AiSessionMode.STREAMING), eq("ping")))
                .thenReturn(id);

        StartSessionRequest req = new StartSessionRequest();
        req.setOrganizationId(1L);
        req.setWizardKind(WizardKind.SMOKE);
        req.setMode(AiSessionMode.STREAMING);
        req.setInput("ping");

        mockMvc.perform(post("/api/ai/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessionId").value(id.toString()));
    }

    @Test
    @WithMockUser(username = "bob", roles = "USER")
    void startSessionForbiddenWhenNotOrgMember() throws Exception {
        User bob = new User();
        bob.setId(99L);
        bob.setUsername("bob");
        bob.setGlobalRole(User.GlobalRole.USER);
        when(users.findByUsername(eq("bob"))).thenReturn(Optional.of(bob));

        // Bob has no membership in org 1
        when(memberships.findByUserIdAndOrganizationId(eq(99L), eq(1L)))
                .thenReturn(Optional.empty());

        StartSessionRequest req = new StartSessionRequest();
        req.setOrganizationId(1L);
        req.setWizardKind(WizardKind.SMOKE);
        req.setMode(AiSessionMode.STREAMING);
        req.setInput("ping");

        mockMvc.perform(post("/api/ai/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "alice", roles = "USER")
    void cancelSessionUpdatesCancelledStatus() throws Exception {
        UUID sessionId = UUID.randomUUID();

        User alice = new User();
        alice.setId(42L);
        alice.setUsername("alice");
        alice.setGlobalRole(User.GlobalRole.USER);
        when(users.findByUsername(eq("alice"))).thenReturn(Optional.of(alice));

        AiSession session = new AiSession();
        session.setId(sessionId);
        session.setUserId(42L);
        when(sessions.findById(sessionId)).thenReturn(Optional.of(session));

        mockMvc.perform(delete("/api/ai/sessions/" + sessionId).with(csrf()))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(username = "charlie", roles = "USER")
    void cancelSessionForbiddenForOtherUser() throws Exception {
        UUID sessionId = UUID.randomUUID();

        User charlie = new User();
        charlie.setId(77L);
        charlie.setUsername("charlie");
        charlie.setGlobalRole(User.GlobalRole.USER);
        when(users.findByUsername(eq("charlie"))).thenReturn(Optional.of(charlie));

        AiSession session = new AiSession();
        session.setId(sessionId);
        session.setUserId(42L); // owned by alice (id=42), not charlie
        when(sessions.findById(sessionId)).thenReturn(Optional.of(session));

        mockMvc.perform(delete("/api/ai/sessions/" + sessionId).with(csrf()))
                .andExpect(status().isForbidden());
    }
}
