package gov.nist.oscal.tools.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import gov.nist.oscal.tools.api.config.RateLimitConfig;
import gov.nist.oscal.tools.api.config.SecurityHeadersConfig;
import gov.nist.oscal.tools.api.entity.ServiceAccountToken;
import gov.nist.oscal.tools.api.entity.User;
import gov.nist.oscal.tools.api.model.ServiceAccountTokenRequest;
import gov.nist.oscal.tools.api.repository.ServiceAccountTokenRepository;
import gov.nist.oscal.tools.api.repository.UserRepository;
import gov.nist.oscal.tools.api.security.JwtUtil;
import gov.nist.oscal.tools.api.service.AuthService;
import gov.nist.oscal.tools.api.service.FileValidationService;
import gov.nist.oscal.tools.api.service.PasswordResetService;
import gov.nist.oscal.tools.api.service.PasswordValidationService;
import gov.nist.oscal.tools.api.service.RateLimitService;
import gov.nist.oscal.tools.api.telemetry.TelemetryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
class ServiceAccountTokenControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockitoBean private AuthService authService;
    @MockitoBean private PasswordResetService passwordResetService;
    @MockitoBean private PasswordValidationService passwordValidationService;
    @MockitoBean private FileValidationService fileValidationService;
    @MockitoBean private ServiceAccountTokenRepository serviceAccountTokenRepository;
    @MockitoBean private UserRepository userRepository;
    @MockitoBean private JwtUtil jwtUtil;
    @MockitoBean private UserDetailsService userDetailsService;
    @MockitoBean private RateLimitService rateLimitService;
    @MockitoBean private RateLimitConfig rateLimitConfig;
    @MockitoBean private SecurityHeadersConfig securityHeadersConfig;
    @MockitoBean private TelemetryService telemetryService;

    private ServiceAccountToken record(long id, String name) {
        User owner = new User();
        owner.setId(1L);
        owner.setUsername("alice");

        ServiceAccountToken t = new ServiceAccountToken();
        t.setId(id);
        t.setUser(owner);
        t.setTokenName(name);
        t.setJti("jti-" + id);
        t.setGlobalRole("SUPER_ADMIN");
        t.setExpiresAt(LocalDateTime.now().plusDays(30));
        t.setCreatedAt(LocalDateTime.now());
        return t;
    }

    private User alice() {
        User alice = new User();
        alice.setId(1L);
        alice.setUsername("alice");
        return alice;
    }

    @Test
    @WithMockUser(username = "alice")
    void generatingATokenPersistsItAndReturnsTheJwt() throws Exception {
        when(authService.createServiceAccountToken(eq("alice"), eq("CI"), eq(30), any(), any(), any()))
                .thenReturn(record(5L, "CI"));
        when(jwtUtil.generateServiceAccountToken(any(ServiceAccountToken.class)))
                .thenReturn("minted.jwt.value");

        mockMvc.perform(post("/api/auth/service-account-token").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ServiceAccountTokenRequest("CI", 30))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("minted.jwt.value"))
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.tokenName").value("CI"))
                .andExpect(jsonPath("$.globalRole").value("SUPER_ADMIN"));

        verify(authService).createServiceAccountToken(eq("alice"), eq("CI"), eq(30), any(), any(), any());
    }

    /**
     * The UI caps expiration at 3650 but a direct API call bypasses the browser
     * entirely, so the ceiling has to be enforced server-side.
     */
    @Test
    @WithMockUser(username = "alice")
    void expirationBeyondTheConfiguredMaximumIsRejected() throws Exception {
        mockMvc.perform(post("/api/auth/service-account-token").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ServiceAccountTokenRequest("Forever", 40000))))
                .andExpect(status().isBadRequest());

        verify(authService, never()).createServiceAccountToken(any(), any(), anyInt(), any(), any(), any());
    }

    @Test
    @WithMockUser(username = "alice")
    void listReturnsTheCallersTokensWithoutTokenValues() throws Exception {
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(alice()));
        when(serviceAccountTokenRepository.findByUserIdOrderByCreatedAtDesc(1L))
                .thenReturn(List.of(record(5L, "CI"), record(6L, "Staging")));

        mockMvc.perform(get("/api/auth/service-account-tokens"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].tokenName").value("CI"))
                .andExpect(jsonPath("$[0].status").value("ACTIVE"))
                .andExpect(jsonPath("$[0].globalRole").value("SUPER_ADMIN"))
                .andExpect(jsonPath("$[0].token").doesNotExist())
                .andExpect(jsonPath("$[1].tokenName").value("Staging"));
    }

    @Test
    @WithMockUser(username = "alice")
    void revokingOwnTokenSetsRevokedAt() throws Exception {
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(alice()));
        when(serviceAccountTokenRepository.findByIdAndUserId(5L, 1L))
                .thenReturn(Optional.of(record(5L, "CI")));

        mockMvc.perform(delete("/api/auth/service-account-tokens/5").with(csrf()))
                .andExpect(status().isNoContent());

        verify(serviceAccountTokenRepository).save(argThat(t -> t.getRevokedAt() != null
                && "alice".equals(t.getRevokedBy())));
    }

    /**
     * 404 rather than 403 — a 403 would confirm the id exists and turn this
     * endpoint into a probe for other users' token ids.
     */
    @Test
    @WithMockUser(username = "mallory")
    void revokingSomeoneElsesTokenReturnsNotFound() throws Exception {
        User mallory = new User();
        mallory.setId(2L);
        mallory.setUsername("mallory");
        when(userRepository.findByUsername("mallory")).thenReturn(Optional.of(mallory));
        when(serviceAccountTokenRepository.findByIdAndUserId(5L, 2L)).thenReturn(Optional.empty());

        mockMvc.perform(delete("/api/auth/service-account-tokens/5").with(csrf()))
                .andExpect(status().isNotFound());

        verify(serviceAccountTokenRepository, never()).save(any());
    }

    @Test
    @WithMockUser(username = "alice")
    void revokingAnAlreadyRevokedTokenIsANoOp() throws Exception {
        ServiceAccountToken alreadyRevoked = record(5L, "CI");
        alreadyRevoked.setRevokedAt(LocalDateTime.now().minusDays(1));

        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(alice()));
        when(serviceAccountTokenRepository.findByIdAndUserId(5L, 1L))
                .thenReturn(Optional.of(alreadyRevoked));

        mockMvc.perform(delete("/api/auth/service-account-tokens/5").with(csrf()))
                .andExpect(status().isNoContent());

        verify(serviceAccountTokenRepository, never()).save(any());
    }
}
