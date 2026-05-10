package gov.nist.oscal.tools.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import gov.nist.oscal.tools.api.config.RateLimitConfig;
import gov.nist.oscal.tools.api.config.SecurityHeadersConfig;
import gov.nist.oscal.tools.api.entity.AiSession;
import gov.nist.oscal.tools.api.entity.AiSessionMode;
import gov.nist.oscal.tools.api.entity.AiSessionStatus;
import gov.nist.oscal.tools.api.entity.OrganizationMembership;
import gov.nist.oscal.tools.api.entity.User;
import gov.nist.oscal.tools.api.entity.WizardKind;
import gov.nist.oscal.tools.api.repository.AiSessionRepository;
import gov.nist.oscal.tools.api.repository.OrganizationMembershipRepository;
import gov.nist.oscal.tools.api.repository.UserRepository;
import gov.nist.oscal.tools.api.security.JwtUtil;
import gov.nist.oscal.tools.api.service.RateLimitService;
import gov.nist.oscal.tools.api.telemetry.TelemetryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AiAnalyticsController.class)
class AiAnalyticsControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockitoBean private AiSessionRepository sessions;
    @MockitoBean private UserRepository users;
    @MockitoBean private OrganizationMembershipRepository memberships;
    @MockitoBean private JwtUtil jwtUtil;
    @MockitoBean private UserDetailsService userDetailsService;
    @MockitoBean private RateLimitService rateLimitService;
    @MockitoBean private RateLimitConfig rateLimitConfig;
    @MockitoBean private SecurityHeadersConfig securityHeadersConfig;
    @MockitoBean private TelemetryService telemetryService;

    private void stubOrgAdmin(String username, long userId, long orgId) {
        User user = new User();
        user.setId(userId);
        user.setUsername(username);
        user.setGlobalRole(User.GlobalRole.USER);
        when(users.findByUsername(eq(username))).thenReturn(Optional.of(user));

        OrganizationMembership membership = new OrganizationMembership();
        membership.setStatus(OrganizationMembership.MembershipStatus.ACTIVE);
        membership.setRole(OrganizationMembership.OrganizationRole.ORG_ADMIN);
        when(memberships.findByUserIdAndOrganizationId(eq(userId), eq(orgId)))
                .thenReturn(Optional.of(membership));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ORG_ADMIN")
    void listSessionsReturnsSummariesForOrgAdmin() throws Exception {
        stubOrgAdmin("admin", 1L, 10L);

        UUID sessionId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();

        AiSession session = new AiSession();
        session.setId(sessionId);
        session.setOrganizationId(10L);
        session.setUserId(1L);
        session.setWizardKind(WizardKind.CATALOG);
        session.setMode(AiSessionMode.STREAMING);
        session.setModel("claude-sonnet-4-6");
        session.setStatus(AiSessionStatus.COMPLETED);
        session.setTokensIn(500);
        session.setTokensOut(200);
        session.setCostUsdMicros(4500L);
        session.setStartedAt(now.minusMinutes(5));
        session.setEndedAt(now);

        when(sessions.findByOrganizationIdOrderByStartedAtDesc(eq(10L), any(Pageable.class)))
                .thenReturn(List.of(session));

        User adminUser = new User();
        adminUser.setId(1L);
        adminUser.setUsername("admin");
        when(users.findAllById(List.of(1L))).thenReturn(List.of(adminUser));

        mockMvc.perform(get("/api/ai/analytics/sessions")
                        .param("organizationId", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(sessionId.toString()))
                .andExpect(jsonPath("$[0].username").value("admin"))
                .andExpect(jsonPath("$[0].status").value("COMPLETED"))
                .andExpect(jsonPath("$[0].tokensIn").value(500))
                .andExpect(jsonPath("$[0].costUsdMicros").value(4500));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ORG_ADMIN")
    void getTotalsReturnsAggregates() throws Exception {
        stubOrgAdmin("admin", 1L, 10L);

        when(sessions.sumForOrg(eq(10L))).thenReturn(
                Map.of("count", 42L, "ti", 100_000L, "to_", 50_000L, "cost", 9_000_000L));
        when(sessions.sumForOrgSince(eq(10L), any(LocalDateTime.class))).thenReturn(
                Map.of("count", 5L, "cost", 1_500_000L));

        mockMvc.perform(get("/api/ai/analytics/totals")
                        .param("organizationId", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalSessions").value(42))
                .andExpect(jsonPath("$.totalTokensIn").value(100_000))
                .andExpect(jsonPath("$.totalTokensOut").value(50_000))
                .andExpect(jsonPath("$.totalCostUsdMicros").value(9_000_000))
                .andExpect(jsonPath("$.sessionsThisMonth").value(5))
                .andExpect(jsonPath("$.costThisMonthUsdMicros").value(1_500_000));
    }

    @Test
    @WithMockUser(roles = "USER")
    void nonAdminGets403OnList() throws Exception {
        mockMvc.perform(get("/api/ai/analytics/sessions")
                        .param("organizationId", "10"))
                .andExpect(status().isForbidden());
    }
}
