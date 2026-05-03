package gov.nist.oscal.tools.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import gov.nist.oscal.tools.api.config.RateLimitConfig;
import gov.nist.oscal.tools.api.config.SecurityHeadersConfig;
import gov.nist.oscal.tools.api.entity.OrganizationMembership;
import gov.nist.oscal.tools.api.entity.User;
import gov.nist.oscal.tools.api.model.ai.AiSettingsResponse;
import gov.nist.oscal.tools.api.model.ai.UpdateAiSettingsRequest;
import gov.nist.oscal.tools.api.repository.OrganizationMembershipRepository;
import gov.nist.oscal.tools.api.repository.UserRepository;
import gov.nist.oscal.tools.api.security.JwtUtil;
import gov.nist.oscal.tools.api.service.RateLimitService;
import gov.nist.oscal.tools.api.service.ai.AiSettingsService;
import gov.nist.oscal.tools.api.telemetry.TelemetryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AiSettingsController.class)
class AiSettingsControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockitoBean private AiSettingsService service;
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
    void putSettingsCallsSetApiKey() throws Exception {
        stubOrgAdmin("admin", 1L, 1L);
        when(service.setApiKey(eq(1L), eq("sk-ant-key12345678901234"), eq("claude-opus-4-7")))
                .thenReturn(new AiSettingsResponse(true, "abcd...1234", "claude-opus-4-7"));

        UpdateAiSettingsRequest req = new UpdateAiSettingsRequest();
        req.setApiKey("sk-ant-key12345678901234");
        req.setDefaultModel("claude-opus-4-7");

        mockMvc.perform(put("/api/ai/settings").param("organizationId", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(true))
                .andExpect(jsonPath("$.fingerprint").value("abcd...1234"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void putSettingsForbiddenForNonAdmin() throws Exception {
        UpdateAiSettingsRequest req = new UpdateAiSettingsRequest();
        req.setApiKey("sk-ant-key12345678901234");

        mockMvc.perform(put("/api/ai/settings").param("organizationId", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ORG_ADMIN")
    void getSettingsReturnsCurrent() throws Exception {
        stubOrgAdmin("admin", 1L, 1L);
        when(service.getSettings(1L)).thenReturn(new AiSettingsResponse(false, null, "claude-opus-4-7"));
        mockMvc.perform(get("/api/ai/settings").param("organizationId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(false));
    }

    @Test
    @WithMockUser(username = "wrongadmin", roles = "ORG_ADMIN")
    void getSettingsForbiddenWhenNotMemberOfOrg() throws Exception {
        User user = new User();
        user.setId(55L);
        user.setUsername("wrongadmin");
        user.setGlobalRole(User.GlobalRole.USER);
        when(users.findByUsername(eq("wrongadmin"))).thenReturn(Optional.of(user));
        // No membership in org 1
        when(memberships.findByUserIdAndOrganizationId(eq(55L), eq(1L)))
                .thenReturn(Optional.empty());

        mockMvc.perform(get("/api/ai/settings").param("organizationId", "1"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser
    void getStatusReturnsEnabledFlag() throws Exception {
        // The /status endpoint is whitelisted in SecurityConfig (permitAll). At runtime an
        // unauthenticated caller can reach it; @WebMvcTest doesn't load SecurityConfig, so this
        // unit test asserts only that the controller method returns the correct shape. The
        // public-route behavior is exercised by an integration test in Phase C.
        when(service.isEnabledFor(1L)).thenReturn(true);
        mockMvc.perform(get("/api/ai/settings/status").param("organizationId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(true));
    }
}
