package gov.nist.oscal.tools.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import gov.nist.oscal.tools.api.config.RateLimitConfig;
import gov.nist.oscal.tools.api.config.SecurityHeadersConfig;
import gov.nist.oscal.tools.api.entity.Authorization;
import gov.nist.oscal.tools.api.entity.AuthorizationTemplate;
import gov.nist.oscal.tools.api.entity.User;
import gov.nist.oscal.tools.api.model.*;
import gov.nist.oscal.tools.api.security.JwtUtil;
import gov.nist.oscal.tools.api.service.AuthorizationService;
import gov.nist.oscal.tools.api.service.DigitalSignatureService;
import gov.nist.oscal.tools.api.service.RateLimitService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.security.cert.X509Certificate;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthorizationController.class)
class AuthorizationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthorizationService authorizationService;

    @MockBean
    private DigitalSignatureService digitalSignatureService;

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

    private User createMockUser(String username) {
        User user = new User();
        user.setUsername(username);
        user.setId(1L);
        return user;
    }

    private Authorization createMockAuthorization(Long id, String name, User user) {
        // Create mock template
        AuthorizationTemplate template = new AuthorizationTemplate();
        template.setId(1L);
        template.setName("Test Template");
        template.setContent("Template content");
        template.setCreatedBy(user);
        template.setLastUpdatedBy(user);
        template.setCreatedAt(LocalDateTime.now());
        template.setLastUpdatedAt(LocalDateTime.now());

        Authorization authorization = new Authorization();
        authorization.setId(id);
        authorization.setName(name);
        authorization.setSspItemId("ssp-123");
        authorization.setSarItemId("sar-123");
        authorization.setTemplate(template);
        authorization.setDateAuthorized(LocalDate.now());
        authorization.setDateExpired(LocalDate.now().plusYears(3));
        authorization.setSystemOwner("John Doe");
        authorization.setSecurityManager("Jane Smith");
        authorization.setAuthorizingOfficial("Bob Johnson");
        authorization.setAuthorizedBy(user);
        authorization.setCompletedContent("Completed authorization content");
        return authorization;
    }

    private AuthorizationRequest createValidRequest() {
        AuthorizationRequest request = new AuthorizationRequest();
        request.setName("Test Authorization");
        request.setSspItemId("ssp-123");
        request.setSarItemId("sar-123");
        request.setTemplateId(1L);

        Map<String, String> variableValues = new HashMap<>();
        variableValues.put("system_name", "Test System");
        variableValues.put("system_id", "SYS-001");
        request.setVariableValues(variableValues);

        request.setDateAuthorized("2024-01-01");
        request.setDateExpired("2027-01-01");
        request.setSystemOwner("John Doe");
        request.setSecurityManager("Jane Smith");
        request.setAuthorizingOfficial("Bob Johnson");
        request.setEditedContent(null); // Optional field
        request.setConditions(java.util.Collections.emptyList()); // Empty list by default

        return request;
    }

    @Test
    @WithMockUser(username = "testuser")
    void testCreateAuthorization_success_returnsCreatedAuthorization() throws Exception {
        // Arrange
        AuthorizationRequest request = createValidRequest();
        User mockUser = createMockUser("testuser");
        Authorization authorization = createMockAuthorization(1L, "Test Authorization", mockUser);

        when(authorizationService.createAuthorization(
                eq("Test Authorization"), eq("ssp-123"), eq("sar-123"), eq(1L),
                anyMap(), eq("testuser"), anyString(), anyString(), anyString(),
                anyString(), anyString(), any(), anyList()))
                .thenReturn(authorization);

        // Act & Assert
        mockMvc.perform(post("/api/authorizations")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Test Authorization"));

        verify(authorizationService, times(1)).createAuthorization(
                eq("Test Authorization"), eq("ssp-123"), eq("sar-123"), eq(1L),
                anyMap(), eq("testuser"), anyString(), anyString(), anyString(),
                anyString(), anyString(), any(), anyList());
    }

    @Test
    @WithMockUser(username = "testuser")
    void testCreateAuthorization_serviceException_returns400() throws Exception {
        // Arrange
        AuthorizationRequest request = createValidRequest();

        when(authorizationService.createAuthorization(
                anyString(), anyString(), anyString(), anyLong(), anyMap(),
                anyString(), anyString(), anyString(), anyString(), anyString(),
                anyString(), any(), anyList()))
                .thenThrow(new RuntimeException("Service error"));

        // Act & Assert
        mockMvc.perform(post("/api/authorizations")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "testuser")
    void testUpdateAuthorization_success_returnsUpdatedAuthorization() throws Exception {
        // Arrange
        AuthorizationRequest request = createValidRequest();
        request.setName("Updated Authorization");

        User mockUser = createMockUser("testuser");
        Authorization authorization = createMockAuthorization(1L, "Updated Authorization", mockUser);

        when(authorizationService.updateAuthorization(
                eq(1L), eq("Updated Authorization"), anyMap(), eq("testuser"),
                anyString(), anyString(), anyString(), anyString(), anyString(),
                any(), anyList()))
                .thenReturn(authorization);

        // Act & Assert
        mockMvc.perform(put("/api/authorizations/1")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Authorization"));

        verify(authorizationService, times(1)).updateAuthorization(
                eq(1L), eq("Updated Authorization"), anyMap(), eq("testuser"),
                anyString(), anyString(), anyString(), anyString(), anyString(),
                any(), anyList());
    }

    @Test
    @WithMockUser(username = "testuser")
    void testUpdateAuthorization_notFound_returns404() throws Exception {
        // Arrange
        AuthorizationRequest request = createValidRequest();

        when(authorizationService.updateAuthorization(
                anyLong(), anyString(), anyMap(), anyString(), anyString(),
                anyString(), anyString(), anyString(), anyString(), any(), anyList()))
                .thenThrow(new RuntimeException("Authorization not found"));

        // Act & Assert
        mockMvc.perform(put("/api/authorizations/999")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "testuser")
    void testGetAuthorization_success_returnsAuthorization() throws Exception {
        // Arrange
        User mockUser = createMockUser("testuser");
        Authorization authorization = createMockAuthorization(1L, "Test Authorization", mockUser);

        when(authorizationService.getAuthorization(1L)).thenReturn(authorization);

        // Act & Assert
        mockMvc.perform(get("/api/authorizations/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Test Authorization"));

        verify(authorizationService, times(1)).getAuthorization(1L);
    }

    @Test
    @WithMockUser(username = "testuser")
    void testGetAuthorization_notFound_returns404() throws Exception {
        // Arrange
        when(authorizationService.getAuthorization(999L))
                .thenThrow(new RuntimeException("Authorization not found"));

        // Act & Assert
        mockMvc.perform(get("/api/authorizations/999"))
                .andExpect(status().isNotFound());

        verify(authorizationService, times(1)).getAuthorization(999L);
    }

    @Test
    @WithMockUser(username = "testuser")
    void testGetAllAuthorizations_success_returnsAuthorizations() throws Exception {
        // Arrange
        User mockUser = createMockUser("testuser");
        Authorization auth1 = createMockAuthorization(1L, "Authorization 1", mockUser);
        Authorization auth2 = createMockAuthorization(2L, "Authorization 2", mockUser);
        List<Authorization> authorizations = Arrays.asList(auth1, auth2);

        when(authorizationService.getAllAuthorizations()).thenReturn(authorizations);

        // Act & Assert
        mockMvc.perform(get("/api/authorizations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("Authorization 1"))
                .andExpect(jsonPath("$[1].name").value("Authorization 2"));

        verify(authorizationService, times(1)).getAllAuthorizations();
    }

    @Test
    @WithMockUser(username = "testuser")
    void testGetAllAuthorizations_serviceException_returns500() throws Exception {
        // Arrange
        when(authorizationService.getAllAuthorizations())
                .thenThrow(new RuntimeException("Database error"));

        // Act & Assert
        mockMvc.perform(get("/api/authorizations"))
                .andExpect(status().isInternalServerError());
    }

    @Test
    @WithMockUser(username = "testuser")
    void testGetRecentlyAuthorized_success_returnsAuthorizations() throws Exception {
        // Arrange
        User mockUser = createMockUser("testuser");
        Authorization auth1 = createMockAuthorization(1L, "Recent Authorization", mockUser);
        List<Authorization> authorizations = Arrays.asList(auth1);

        when(authorizationService.getRecentlyAuthorized(10)).thenReturn(authorizations);

        // Act & Assert
        mockMvc.perform(get("/api/authorizations/recent")
                .param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Recent Authorization"));

        verify(authorizationService, times(1)).getRecentlyAuthorized(10);
    }

    @Test
    @WithMockUser(username = "testuser")
    void testGetRecentlyAuthorized_defaultLimit_uses10() throws Exception {
        // Arrange
        when(authorizationService.getRecentlyAuthorized(10)).thenReturn(Arrays.asList());

        // Act & Assert
        mockMvc.perform(get("/api/authorizations/recent"))
                .andExpect(status().isOk());

        verify(authorizationService, times(1)).getRecentlyAuthorized(10);
    }

    @Test
    @WithMockUser(username = "testuser")
    void testGetRecentlyAuthorized_serviceException_returns500() throws Exception {
        // Arrange
        when(authorizationService.getRecentlyAuthorized(anyInt()))
                .thenThrow(new RuntimeException("Database error"));

        // Act & Assert
        mockMvc.perform(get("/api/authorizations/recent"))
                .andExpect(status().isInternalServerError());
    }

    @Test
    @WithMockUser(username = "testuser")
    void testGetAuthorizationsBySsp_success_returnsAuthorizations() throws Exception {
        // Arrange
        User mockUser = createMockUser("testuser");
        Authorization auth1 = createMockAuthorization(1L, "SSP Authorization", mockUser);
        List<Authorization> authorizations = Arrays.asList(auth1);

        when(authorizationService.getAuthorizationsBySsp("ssp-123")).thenReturn(authorizations);

        // Act & Assert
        mockMvc.perform(get("/api/authorizations/ssp/ssp-123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].sspItemId").value("ssp-123"));

        verify(authorizationService, times(1)).getAuthorizationsBySsp("ssp-123");
    }

    @Test
    @WithMockUser(username = "testuser")
    void testGetAuthorizationsBySsp_serviceException_returns500() throws Exception {
        // Arrange
        when(authorizationService.getAuthorizationsBySsp(anyString()))
                .thenThrow(new RuntimeException("Database error"));

        // Act & Assert
        mockMvc.perform(get("/api/authorizations/ssp/ssp-123"))
                .andExpect(status().isInternalServerError());
    }

    @Test
    @WithMockUser(username = "testuser")
    void testSearchAuthorizations_success_returnsMatchingAuthorizations() throws Exception {
        // Arrange
        User mockUser = createMockUser("testuser");
        Authorization auth1 = createMockAuthorization(1L, "Matching Authorization", mockUser);
        List<Authorization> authorizations = Arrays.asList(auth1);

        when(authorizationService.searchAuthorizations("matching")).thenReturn(authorizations);

        // Act & Assert
        mockMvc.perform(get("/api/authorizations/search")
                .param("q", "matching"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Matching Authorization"));

        verify(authorizationService, times(1)).searchAuthorizations("matching");
    }

    @Test
    @WithMockUser(username = "testuser")
    void testSearchAuthorizations_noQuery_searchesAll() throws Exception {
        // Arrange
        when(authorizationService.searchAuthorizations(null)).thenReturn(Arrays.asList());

        // Act & Assert
        mockMvc.perform(get("/api/authorizations/search"))
                .andExpect(status().isOk());

        verify(authorizationService, times(1)).searchAuthorizations(null);
    }

    @Test
    @WithMockUser(username = "testuser")
    void testSearchAuthorizations_serviceException_returns500() throws Exception {
        // Arrange
        when(authorizationService.searchAuthorizations(anyString()))
                .thenThrow(new RuntimeException("Database error"));

        // Act & Assert
        mockMvc.perform(get("/api/authorizations/search")
                .param("q", "test"))
                .andExpect(status().isInternalServerError());
    }

    @Test
    @WithMockUser(username = "testuser")
    void testDeleteAuthorization_success_returns200() throws Exception {
        // Arrange
        doNothing().when(authorizationService).deleteAuthorization(1L, "testuser");

        // Act & Assert
        mockMvc.perform(delete("/api/authorizations/1")
                .with(csrf()))
                .andExpect(status().isOk());

        verify(authorizationService, times(1)).deleteAuthorization(1L, "testuser");
    }

    @Test
    @WithMockUser(username = "testuser")
    void testDeleteAuthorization_notCreator_returns403() throws Exception {
        // Arrange
        doThrow(new RuntimeException("Only the creator can delete this authorization"))
                .when(authorizationService).deleteAuthorization(1L, "testuser");

        // Act & Assert
        mockMvc.perform(delete("/api/authorizations/1")
                .with(csrf()))
                .andExpect(status().isForbidden());

        verify(authorizationService, times(1)).deleteAuthorization(1L, "testuser");
    }

    @Test
    @WithMockUser(username = "testuser")
    void testDeleteAuthorization_notFound_returns404() throws Exception {
        // Arrange
        doThrow(new RuntimeException("Authorization not found"))
                .when(authorizationService).deleteAuthorization(999L, "testuser");

        // Act & Assert
        mockMvc.perform(delete("/api/authorizations/999")
                .with(csrf()))
                .andExpect(status().isNotFound());

        verify(authorizationService, times(1)).deleteAuthorization(999L, "testuser");
    }

    @Test
    @WithMockUser(username = "testuser")
    void testGetSignatureDetails_success_returnsSignatureDetails() throws Exception {
        // Arrange
        User mockUser = createMockUser("testuser");
        Authorization authorization = createMockAuthorization(1L, "Test Authorization", mockUser);
        authorization.setSignerCertificate("CERTIFICATE_DATA");
        authorization.setSignerCommonName("John Doe");
        authorization.setSignerEmail("john.doe@example.com");
        authorization.setSignerEdipi("1234567890");
        authorization.setSignatureTimestamp(LocalDateTime.now());
        authorization.setCertificateVerified(true);

        when(authorizationService.getAuthorization(1L)).thenReturn(authorization);

        // Act & Assert
        mockMvc.perform(get("/api/authorizations/1/signature"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.signed").value(true))
                .andExpect(jsonPath("$.signerName").value("John Doe"))
                .andExpect(jsonPath("$.signerEmail").value("john.doe@example.com"));

        verify(authorizationService, times(1)).getAuthorization(1L);
    }

    @Test
    @WithMockUser(username = "testuser")
    void testGetSignatureDetails_noSignature_returns404() throws Exception {
        // Arrange
        User mockUser = createMockUser("testuser");
        Authorization authorization = createMockAuthorization(1L, "Test Authorization", mockUser);
        authorization.setSignerCertificate(null); // No signature

        when(authorizationService.getAuthorization(1L)).thenReturn(authorization);

        // Act & Assert
        mockMvc.perform(get("/api/authorizations/1/signature"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("No signature found"));

        verify(authorizationService, times(1)).getAuthorization(1L);
    }

    @Test
    @WithMockUser(username = "testuser")
    void testGetSignatureDetails_authorizationNotFound_returns404() throws Exception {
        // Arrange
        when(authorizationService.getAuthorization(999L))
                .thenThrow(new RuntimeException("Authorization not found"));

        // Act & Assert
        mockMvc.perform(get("/api/authorizations/999/signature"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Authorization not found"));
    }

    @Test
    @WithMockUser(username = "testuser")
    void testVerifySignature_success_returnsVerificationResult() throws Exception {
        // Arrange
        User mockUser = createMockUser("testuser");
        Authorization authorization = createMockAuthorization(1L, "Test Authorization", mockUser);
        authorization.setSignerCertificate("CERTIFICATE_DATA");

        CertificateValidationResult validationResult = new CertificateValidationResult();
        validationResult.setValid(true);
        validationResult.setNotes("Certificate is valid");

        when(authorizationService.getAuthorization(1L)).thenReturn(authorization);
        when(digitalSignatureService.verifyCertificate("CERTIFICATE_DATA")).thenReturn(validationResult);
        when(authorizationService.save(any(Authorization.class))).thenReturn(authorization);

        // Act & Assert
        mockMvc.perform(post("/api/authorizations/1/verify-signature")
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true))
                .andExpect(jsonPath("$.notes").value("Certificate is valid"));

        verify(authorizationService, times(1)).getAuthorization(1L);
        verify(digitalSignatureService, times(1)).verifyCertificate("CERTIFICATE_DATA");
        verify(authorizationService, times(1)).save(any(Authorization.class));
    }

    @Test
    @WithMockUser(username = "testuser")
    void testVerifySignature_noSignature_returns404() throws Exception {
        // Arrange
        User mockUser = createMockUser("testuser");
        Authorization authorization = createMockAuthorization(1L, "Test Authorization", mockUser);
        authorization.setSignerCertificate(null); // No signature

        when(authorizationService.getAuthorization(1L)).thenReturn(authorization);

        // Act & Assert
        mockMvc.perform(post("/api/authorizations/1/verify-signature")
                .with(csrf()))
                .andExpect(status().isNotFound());

        verify(authorizationService, times(1)).getAuthorization(1L);
        verify(digitalSignatureService, never()).verifyCertificate(anyString());
    }

    @Test
    @WithMockUser(username = "testuser")
    void testVerifySignature_verificationFails_returns500() throws Exception {
        // Arrange
        User mockUser = createMockUser("testuser");
        Authorization authorization = createMockAuthorization(1L, "Test Authorization", mockUser);
        authorization.setSignerCertificate("CERTIFICATE_DATA");

        when(authorizationService.getAuthorization(1L)).thenReturn(authorization);
        when(digitalSignatureService.verifyCertificate("CERTIFICATE_DATA"))
                .thenThrow(new RuntimeException("Verification error"));

        // Act & Assert
        mockMvc.perform(post("/api/authorizations/1/verify-signature")
                .with(csrf()))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void testCreateAuthorization_unauthenticated_returns401() throws Exception {
        // Arrange
        AuthorizationRequest request = createValidRequest();

        // Act & Assert
        mockMvc.perform(post("/api/authorizations")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());

        verify(authorizationService, never()).createAuthorization(
                anyString(), anyString(), anyString(), anyLong(), anyMap(),
                anyString(), anyString(), anyString(), anyString(), anyString(),
                anyString(), any(), anyList());
    }

    @Test
    void testDeleteAuthorization_unauthenticated_returns401() throws Exception {
        // Act & Assert
        mockMvc.perform(delete("/api/authorizations/1")
                .with(csrf()))
                .andExpect(status().isUnauthorized());

        verify(authorizationService, never()).deleteAuthorization(anyLong(), anyString());
    }
}
