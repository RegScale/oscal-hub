package gov.nist.oscal.tools.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import gov.nist.oscal.tools.api.config.RateLimitConfig;
import gov.nist.oscal.tools.api.config.SecurityHeadersConfig;
import gov.nist.oscal.tools.api.model.ai.AiSettingsResponse;
import gov.nist.oscal.tools.api.model.ai.UpdateAiSettingsRequest;
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
    @MockitoBean private JwtUtil jwtUtil;
    @MockitoBean private UserDetailsService userDetailsService;
    @MockitoBean private RateLimitService rateLimitService;
    @MockitoBean private RateLimitConfig rateLimitConfig;
    @MockitoBean private SecurityHeadersConfig securityHeadersConfig;
    @MockitoBean private TelemetryService telemetryService;

    @Test
    @WithMockUser(roles = "ORG_ADMIN")
    void putSettingsCallsSetApiKey() throws Exception {
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
    @WithMockUser(roles = "ORG_ADMIN")
    void getSettingsReturnsCurrent() throws Exception {
        when(service.getSettings(1L)).thenReturn(new AiSettingsResponse(false, null, "claude-opus-4-7"));
        mockMvc.perform(get("/api/ai/settings").param("organizationId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(false));
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
