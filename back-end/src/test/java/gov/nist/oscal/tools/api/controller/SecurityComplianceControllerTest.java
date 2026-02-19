/*
 * SPDX-FileCopyrightText: none
 * SPDX-License-Identifier: CC0-1.0
 */

package gov.nist.oscal.tools.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import gov.nist.oscal.tools.api.config.RateLimitConfig;
import gov.nist.oscal.tools.api.config.SecurityHeadersConfig;
import gov.nist.oscal.tools.api.model.security.*;
import gov.nist.oscal.tools.api.security.JwtUtil;
import gov.nist.oscal.tools.api.service.RateLimitService;
import gov.nist.oscal.tools.api.service.SecurityComplianceService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.*;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SecurityComplianceController.class)
class SecurityComplianceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private SecurityComplianceService securityComplianceService;

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

    // ========== GET /api/admin/security/compliance-summary Tests ==========

    @Test
    @WithMockUser(username = "admin", roles = {"SUPER_ADMIN"})
    void testGetComplianceSummary_returnsValidSummary() throws Exception {
        // Given
        ComplianceSummary summary = createMockComplianceSummary();
        when(securityComplianceService.getComplianceSummary()).thenReturn(summary);

        // When & Then
        mockMvc.perform(get("/api/admin/security/compliance-summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalControls").value(20))
                .andExpect(jsonPath("$.implementedControls").value(15))
                .andExpect(jsonPath("$.partialControls").value(3))
                .andExpect(jsonPath("$.gapControls").value(2))
                .andExpect(jsonPath("$.compliancePercentage").value(82.5))
                .andExpect(jsonPath("$.assessmentDate").exists())
                .andExpect(jsonPath("$.byCategory").exists());

        verify(securityComplianceService).getComplianceSummary();
    }

    @Test
    @WithMockUser(username = "user", roles = {"USER"})
    void testGetComplianceSummary_nonAdminUser_returnsForbidden() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/admin/security/compliance-summary"))
                .andExpect(status().isForbidden());

        verify(securityComplianceService, never()).getComplianceSummary();
    }

    @Test
    void testGetComplianceSummary_unauthenticated_returnsUnauthorized() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/admin/security/compliance-summary"))
                .andExpect(status().isUnauthorized());

        verify(securityComplianceService, never()).getComplianceSummary();
    }

    // ========== GET /api/admin/security/controls Tests ==========

    @Test
    @WithMockUser(username = "admin", roles = {"SUPER_ADMIN"})
    void testGetAllControls_returnsControlsList() throws Exception {
        // Given
        List<Soc2Control> controls = createMockControls();
        when(securityComplianceService.getAllControls()).thenReturn(controls);

        // When & Then
        mockMvc.perform(get("/api/admin/security/controls"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].controlId").value("CC6.1"))
                .andExpect(jsonPath("$[0].name").value("Logical Access Security"))
                .andExpect(jsonPath("$[0].status").value("IMPLEMENTED"));

        verify(securityComplianceService).getAllControls();
    }

    @Test
    @WithMockUser(username = "user", roles = {"USER"})
    void testGetAllControls_nonAdminUser_returnsForbidden() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/admin/security/controls"))
                .andExpect(status().isForbidden());

        verify(securityComplianceService, never()).getAllControls();
    }

    // ========== GET /api/admin/security/controls/{category} Tests ==========

    @Test
    @WithMockUser(username = "admin", roles = {"SUPER_ADMIN"})
    void testGetControlsByCategory_CC6_returnsControls() throws Exception {
        // Given
        List<Soc2Control> controls = createMockControls();
        when(securityComplianceService.getControlsByCategory("CC6")).thenReturn(controls);

        // When & Then
        mockMvc.perform(get("/api/admin/security/controls/CC6"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].category").value("CC6"));

        verify(securityComplianceService).getControlsByCategory("CC6");
    }

    @Test
    @WithMockUser(username = "admin", roles = {"SUPER_ADMIN"})
    void testGetControlsByCategory_invalidCategory_returnsBadRequest() throws Exception {
        // Given
        when(securityComplianceService.getControlsByCategory("INVALID")).thenReturn(Collections.emptyList());

        // When & Then
        mockMvc.perform(get("/api/admin/security/controls/INVALID"))
                .andExpect(status().isBadRequest());

        verify(securityComplianceService).getControlsByCategory("INVALID");
    }

    @Test
    @WithMockUser(username = "user", roles = {"USER"})
    void testGetControlsByCategory_nonAdminUser_returnsForbidden() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/admin/security/controls/CC6"))
                .andExpect(status().isForbidden());

        verify(securityComplianceService, never()).getControlsByCategory(anyString());
    }

    // ========== GET /api/admin/security/gaps Tests ==========

    @Test
    @WithMockUser(username = "admin", roles = {"SUPER_ADMIN"})
    void testGetGapAnalysis_returnsGapsList() throws Exception {
        // Given
        List<GapAnalysis> gaps = createMockGaps();
        when(securityComplianceService.getGapAnalysis()).thenReturn(gaps);

        // When & Then
        mockMvc.perform(get("/api/admin/security/gaps"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].gapId").value("GAP-001"))
                .andExpect(jsonPath("$[0].controlId").value("CC6.8"))
                .andExpect(jsonPath("$[0].severity").value("HIGH"));

        verify(securityComplianceService).getGapAnalysis();
    }

    @Test
    @WithMockUser(username = "user", roles = {"USER"})
    void testGetGapAnalysis_nonAdminUser_returnsForbidden() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/admin/security/gaps"))
                .andExpect(status().isForbidden());

        verify(securityComplianceService, never()).getGapAnalysis();
    }

    @Test
    void testGetGapAnalysis_unauthenticated_returnsUnauthorized() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/admin/security/gaps"))
                .andExpect(status().isUnauthorized());

        verify(securityComplianceService, never()).getGapAnalysis();
    }

    // ========== Helper Methods ==========

    private ComplianceSummary createMockComplianceSummary() {
        Map<String, ComplianceSummary.CategorySummary> byCategory = new LinkedHashMap<>();
        byCategory.put("CC6", new ComplianceSummary.CategorySummary("Logical Access", 6, 5, 0, 1));
        byCategory.put("CC7", new ComplianceSummary.CategorySummary("System Operations", 4, 3, 1, 0));

        return new ComplianceSummary(
                20,      // totalControls
                15,      // implementedControls
                3,       // partialControls
                2,       // gapControls
                82.5,    // compliancePercentage
                Instant.now(),
                byCategory
        );
    }

    private List<Soc2Control> createMockControls() {
        List<Soc2Control> controls = new ArrayList<>();
        controls.add(Soc2Control.builder()
                .controlId("CC6.1")
                .name("Logical Access Security")
                .description("Control for logical access")
                .category(ControlCategory.CC6)
                .status(ControlStatus.IMPLEMENTED)
                .implementation("JWT authentication implemented")
                .evidence(Arrays.asList("JwtService.java", "SecurityConfig.java"))
                .build());
        return controls;
    }

    private List<GapAnalysis> createMockGaps() {
        List<GapAnalysis> gaps = new ArrayList<>();
        gaps.add(GapAnalysis.builder()
                .gapId("GAP-001")
                .controlId("CC6.8")
                .title("MFA Not Implemented")
                .description("Multi-factor authentication is not implemented")
                .severity(GapSeverity.HIGH)
                .recommendation("Implement TOTP-based MFA")
                .effort("Medium-High")
                .priority(1)
                .build());
        return gaps;
    }
}
