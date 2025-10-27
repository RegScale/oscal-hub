package gov.nist.oscal.tools.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import gov.nist.oscal.tools.api.model.*;
import gov.nist.oscal.tools.api.security.JwtUtil;
import gov.nist.oscal.tools.api.config.RateLimitConfig;
import gov.nist.oscal.tools.api.config.SecurityHeadersConfig;
import gov.nist.oscal.tools.api.service.RateLimitService;
import gov.nist.oscal.tools.api.service.VisualizationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(VisualizationController.class)
class VisualizationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private VisualizationService visualizationService;

    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private UserDetailsService userDetailsService;

    @MockBean
    private RateLimitService rateLimitService;

    @MockBean
    private RateLimitConfig rateLimitConfig;

    @MockBean
    private SecurityHeadersConfig securityHeadersConfig;

    @Test
    @WithMockUser(username = "testuser")
    void testVisualizeSSP_success_returnsResult() throws Exception {
        // Arrange
        SspVisualizationRequest request = new SspVisualizationRequest();
        request.setContent("<system-security-plan></system-security-plan>");
        request.setFormat(OscalFormat.XML);

        SspVisualizationResult result = new SspVisualizationResult();
        result.setSuccess(true);

        when(visualizationService.analyzeSSP(any(SspVisualizationRequest.class), eq("testuser")))
                .thenReturn(result);

        // Act & Assert
        mockMvc.perform(post("/api/visualization/ssp")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(visualizationService, times(1)).analyzeSSP(any(SspVisualizationRequest.class), eq("testuser"));
    }

    @Test
    @WithMockUser(username = "testuser")
    void testVisualizeProfile_success_returnsResult() throws Exception {
        // Arrange
        ProfileVisualizationRequest request = new ProfileVisualizationRequest();
        request.setContent("<profile></profile>");
        request.setFormat(OscalFormat.XML);

        ProfileVisualizationResult result = new ProfileVisualizationResult();
        result.setSuccess(true);

        when(visualizationService.analyzeProfile(any(ProfileVisualizationRequest.class), eq("testuser")))
                .thenReturn(result);

        // Act & Assert
        mockMvc.perform(post("/api/visualization/profile")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(visualizationService, times(1)).analyzeProfile(any(ProfileVisualizationRequest.class), eq("testuser"));
    }

    @Test
    @WithMockUser(username = "testuser")
    void testVisualizeSAR_success_returnsResult() throws Exception {
        // Arrange
        SarVisualizationRequest request = new SarVisualizationRequest();
        request.setContent("<assessment-results></assessment-results>");
        request.setFormat(OscalFormat.XML);

        SarVisualizationResult result = new SarVisualizationResult();
        result.setSuccess(true);

        when(visualizationService.analyzeSAR(any(SarVisualizationRequest.class), eq("testuser")))
                .thenReturn(result);

        // Act & Assert
        mockMvc.perform(post("/api/visualization/sar")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(visualizationService, times(1)).analyzeSAR(any(SarVisualizationRequest.class), eq("testuser"));
    }

    @Test
    void testVisualizeSSP_unauthenticated_returns401() throws Exception {
        // Arrange
        SspVisualizationRequest request = new SspVisualizationRequest();
        request.setContent("<system-security-plan></system-security-plan>");
        request.setFormat(OscalFormat.XML);

        // Act & Assert
        mockMvc.perform(post("/api/visualization/ssp")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());

        verify(visualizationService, never()).analyzeSSP(any(), anyString());
    }
}
