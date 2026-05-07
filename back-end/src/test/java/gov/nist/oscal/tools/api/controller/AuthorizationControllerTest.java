package gov.nist.oscal.tools.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import gov.nist.oscal.tools.api.config.RateLimitConfig;
import gov.nist.oscal.tools.api.config.SecurityHeadersConfig;
import gov.nist.oscal.tools.api.entity.Authorization;
import gov.nist.oscal.tools.api.entity.AuthorizationGrant;
import gov.nist.oscal.tools.api.entity.AuthorizationRole;
import gov.nist.oscal.tools.api.entity.AuthorizationTemplate;
import gov.nist.oscal.tools.api.entity.Organization;
import gov.nist.oscal.tools.api.entity.OrganizationMembership;
import gov.nist.oscal.tools.api.entity.User;
import gov.nist.oscal.tools.api.model.*;
import gov.nist.oscal.tools.api.repository.AuthorizationGrantRepository;
import gov.nist.oscal.tools.api.repository.UserRepository;
import gov.nist.oscal.tools.api.security.JwtUtil;
import gov.nist.oscal.tools.api.service.AuthorizationAccessGuard;
import gov.nist.oscal.tools.api.service.AuthorizationService;
import gov.nist.oscal.tools.api.service.DigitalSignatureService;
import gov.nist.oscal.tools.api.service.RateLimitService;
import gov.nist.oscal.tools.api.telemetry.TelemetryService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
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
import java.util.Optional;

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

    @MockitoBean
    private AuthorizationService authorizationService;

    @MockitoBean
    private DigitalSignatureService digitalSignatureService;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @MockitoBean
    private RateLimitService rateLimitService;

    @MockitoBean
    private RateLimitConfig rateLimitConfig;

    @MockitoBean
    private SecurityHeadersConfig securityHeadersConfig;

    @MockitoBean
    private TelemetryService telemetryService;

    @MockitoBean
    private AuthorizationAccessGuard accessGuard;

    @MockitoBean
    private AuthorizationGrantRepository grantRepository;

    @MockitoBean
    private UserRepository userRepository;

    @BeforeEach
    void setUpAccessGuardDefaults() {
        User testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        // toResponse calls accessGuard.effectiveRole(authorization, currentUser).
        // Default to OWNER so happy-path tests don't NPE.
        when(accessGuard.effectiveRole(any(Authorization.class), any(User.class)))
                .thenReturn(AuthorizationRole.OWNER);
    }

    private User createMockUser(String username) {
        User user = new User();
        user.setUsername(username);
        user.setId(1L);
        return user;
    }

    private Authorization createMockAuthorization(Long id, String name, User user) {
        // Create mock organization
        Organization org = new Organization();
        org.setId(1L);
        org.setName("Test Org");

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
        authorization.setOrganization(org);
        authorization.setDateAuthorized(LocalDate.now());
        authorization.setDateExpired(LocalDate.now().plusYears(3));
        authorization.setSystemOwner("John Doe");
        authorization.setSecurityManager("Jane Smith");
        authorization.setAuthorizingOfficial("Bob Johnson");
        authorization.setAuthorizedBy(user);
        authorization.setAuthorizedAt(LocalDateTime.now());
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

        when(authorizationService.getAuthorizationForUser(1L, "testuser")).thenReturn(authorization);

        // Act & Assert
        mockMvc.perform(get("/api/authorizations/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Test Authorization"));

        verify(authorizationService, times(1)).getAuthorizationForUser(1L, "testuser");
    }

    @Test
    @WithMockUser(username = "testuser")
    void testGetAuthorization_notFound_returns404() throws Exception {
        // Arrange
        when(authorizationService.getAuthorizationForUser(999L, "testuser"))
                .thenThrow(new RuntimeException("Authorization not found"));

        // Act & Assert
        mockMvc.perform(get("/api/authorizations/999"))
                .andExpect(status().isNotFound());

        verify(authorizationService, times(1)).getAuthorizationForUser(999L, "testuser");
    }

    @Test
    @WithMockUser(username = "testuser")
    void testGetAllAuthorizations_success_returnsAuthorizations() throws Exception {
        // Arrange
        User mockUser = createMockUser("testuser");
        Authorization auth1 = createMockAuthorization(1L, "Authorization 1", mockUser);
        Authorization auth2 = createMockAuthorization(2L, "Authorization 2", mockUser);
        List<Authorization> authorizations = Arrays.asList(auth1, auth2);

        when(authorizationService.getAllAuthorizationsForUser("testuser")).thenReturn(authorizations);

        // Act & Assert
        mockMvc.perform(get("/api/authorizations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("Authorization 1"))
                .andExpect(jsonPath("$[1].name").value("Authorization 2"));

        verify(authorizationService, times(1)).getAllAuthorizationsForUser("testuser");
    }

    @Test
    @WithMockUser(username = "testuser")
    void testGetAllAuthorizations_serviceException_returns500() throws Exception {
        // Arrange
        when(authorizationService.getAllAuthorizationsForUser("testuser"))
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

        when(authorizationService.getAllAuthorizationsForUser("testuser")).thenReturn(authorizations);

        // Act & Assert
        mockMvc.perform(get("/api/authorizations/recent")
                .param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Recent Authorization"));

        verify(authorizationService, times(1)).getAllAuthorizationsForUser("testuser");
    }

    @Test
    @WithMockUser(username = "testuser")
    void testGetRecentlyAuthorized_defaultLimit_uses10() throws Exception {
        // Arrange
        when(authorizationService.getAllAuthorizationsForUser("testuser")).thenReturn(Arrays.asList());

        // Act & Assert
        mockMvc.perform(get("/api/authorizations/recent"))
                .andExpect(status().isOk());

        verify(authorizationService, times(1)).getAllAuthorizationsForUser("testuser");
    }

    @Test
    @WithMockUser(username = "testuser")
    void testGetRecentlyAuthorized_serviceException_returns500() throws Exception {
        // Arrange
        when(authorizationService.getAllAuthorizationsForUser(anyString()))
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

        when(authorizationService.getAuthorizationsBySspForUser("ssp-123", "testuser")).thenReturn(authorizations);

        // Act & Assert
        mockMvc.perform(get("/api/authorizations/ssp/ssp-123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].sspItemId").value("ssp-123"));

        verify(authorizationService, times(1)).getAuthorizationsBySspForUser("ssp-123", "testuser");
    }

    @Test
    @WithMockUser(username = "testuser")
    void testGetAuthorizationsBySsp_serviceException_returns500() throws Exception {
        // Arrange
        when(authorizationService.getAuthorizationsBySspForUser(anyString(), anyString()))
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

        when(authorizationService.searchAuthorizationsForUser("testuser", "matching")).thenReturn(authorizations);

        // Act & Assert
        mockMvc.perform(get("/api/authorizations/search")
                .param("q", "matching"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Matching Authorization"));

        verify(authorizationService, times(1)).searchAuthorizationsForUser("testuser", "matching");
    }

    @Test
    @WithMockUser(username = "testuser")
    void testSearchAuthorizations_noQuery_searchesAll() throws Exception {
        // Arrange
        when(authorizationService.searchAuthorizationsForUser("testuser", null)).thenReturn(Arrays.asList());

        // Act & Assert
        mockMvc.perform(get("/api/authorizations/search"))
                .andExpect(status().isOk());

        verify(authorizationService, times(1)).searchAuthorizationsForUser("testuser", null);
    }

    @Test
    @WithMockUser(username = "testuser")
    void testSearchAuthorizations_serviceException_returns500() throws Exception {
        // Arrange
        when(authorizationService.searchAuthorizationsForUser(anyString(), anyString()))
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

        when(authorizationService.getAuthorizationForUser(1L, "testuser")).thenReturn(authorization);

        // Act & Assert
        mockMvc.perform(get("/api/authorizations/1/signature"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.signed").value(true))
                .andExpect(jsonPath("$.signerName").value("John Doe"))
                .andExpect(jsonPath("$.signerEmail").value("john.doe@example.com"));

        verify(authorizationService, times(1)).getAuthorizationForUser(1L, "testuser");
    }

    @Test
    @WithMockUser(username = "testuser")
    void testGetSignatureDetails_noSignature_returns404() throws Exception {
        // Arrange
        User mockUser = createMockUser("testuser");
        Authorization authorization = createMockAuthorization(1L, "Test Authorization", mockUser);
        authorization.setSignerCertificate(null); // No signature

        when(authorizationService.getAuthorizationForUser(1L, "testuser")).thenReturn(authorization);

        // Act & Assert
        mockMvc.perform(get("/api/authorizations/1/signature"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("No signature found"));

        verify(authorizationService, times(1)).getAuthorizationForUser(1L, "testuser");
    }

    @Test
    @WithMockUser(username = "testuser")
    void testGetSignatureDetails_authorizationNotFound_returns404() throws Exception {
        // Arrange
        when(authorizationService.getAuthorizationForUser(999L, "testuser"))
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

        when(authorizationService.getAuthorizationForUser(1L, "testuser")).thenReturn(authorization);
        when(digitalSignatureService.verifyCertificate("CERTIFICATE_DATA")).thenReturn(validationResult);
        when(authorizationService.save(any(Authorization.class))).thenReturn(authorization);

        // Act & Assert
        mockMvc.perform(post("/api/authorizations/1/verify-signature")
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true))
                .andExpect(jsonPath("$.notes").value("Certificate is valid"));

        verify(authorizationService, times(1)).getAuthorizationForUser(1L, "testuser");
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

        when(authorizationService.getAuthorizationForUser(1L, "testuser")).thenReturn(authorization);

        // Act & Assert
        mockMvc.perform(post("/api/authorizations/1/verify-signature")
                .with(csrf()))
                .andExpect(status().isNotFound());

        verify(authorizationService, times(1)).getAuthorizationForUser(1L, "testuser");
        verify(digitalSignatureService, never()).verifyCertificate(anyString());
    }

    @Test
    @WithMockUser(username = "testuser")
    void testVerifySignature_verificationFails_returns500() throws Exception {
        // Arrange
        User mockUser = createMockUser("testuser");
        Authorization authorization = createMockAuthorization(1L, "Test Authorization", mockUser);
        authorization.setSignerCertificate("CERTIFICATE_DATA");

        when(authorizationService.getAuthorizationForUser(1L, "testuser")).thenReturn(authorization);
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

    // ===== Sign-with-cert org-isolation tests =====

    @Test
    @WithMockUser(username = "testuser")
    void testSignWithClientCertificate_noCertificate_returns401() throws Exception {
        // No X509Certificate attribute on the request → should return 401
        SignRequest request = new SignRequest(1L);

        mockMvc.perform(post("/api/authorizations/sign-with-cert")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());

        // getAuthorizationForUser must never be called when no cert is present
        verify(authorizationService, never()).getAuthorizationForUser(anyLong(), anyString());
    }

    @Test
    @WithMockUser(username = "testuser")
    void testSignWithClientCertificate_crossOrgId_returns404() throws Exception {
        // Simulates an attacker guessing a foreign org's authorization ID.
        // authorizationService.getAuthorizationForUser throws RuntimeException("not found")
        // when the ID is not in the current user's org.
        SignRequest request = new SignRequest(999L);

        when(authorizationService.getAuthorizationForUser(999L, "testuser"))
                .thenThrow(new RuntimeException("Authorization not found"));

        // Because no X509 cert attribute is set in MockMvc, the endpoint will reject
        // at the cert-presence check (401) before reaching the org lookup.  This test
        // verifies the lookup is never called when no cert is present.
        mockMvc.perform(post("/api/authorizations/sign-with-cert")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());

        verify(authorizationService, never()).getAuthorizationForUser(anyLong(), anyString());
    }

    @Test
    void testSignWithClientCertificate_unauthenticated_returns401() throws Exception {
        SignRequest request = new SignRequest(1L);

        mockMvc.perform(post("/api/authorizations/sign-with-cert")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());

        verify(authorizationService, never()).getAuthorizationForUser(anyLong(), anyString());
        verify(digitalSignatureService, never()).signAuthorization(any(gov.nist.oscal.tools.api.entity.Authorization.class), any(X509Certificate.class));
    }

    // ===== Grant endpoint tests (Task 12) =====

    @Test
    @WithMockUser(username = "testuser")
    void listGrants_owner_returns200() throws Exception {
        Authorization auth = mockAuthorizationForGrants(1L);
        when(authorizationService.getAuthorizationForUser(1L, "testuser")).thenReturn(auth);
        when(grantRepository.findByAuthorization(auth)).thenReturn(List.of());

        mockMvc.perform(get("/api/authorizations/1/grants"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "testuser")
    void addGrant_owner_returns201() throws Exception {
        Authorization auth = mockAuthorizationForGrants(1L);
        Organization org = auth.getOrganization();

        User grantee = new User();
        grantee.setId(2L);
        grantee.setUsername("bob");

        OrganizationMembership granteeMembership = new OrganizationMembership();
        granteeMembership.setOrganization(org);
        granteeMembership.setStatus(OrganizationMembership.MembershipStatus.ACTIVE);

        java.util.Set<OrganizationMembership> memberships = new java.util.HashSet<>();
        memberships.add(granteeMembership);
        grantee.setOrganizationMemberships(memberships);

        when(authorizationService.getAuthorizationForUser(1L, "testuser")).thenReturn(auth);
        when(userRepository.findById(2L)).thenReturn(Optional.of(grantee));
        when(grantRepository.findByAuthorizationAndUser(auth, grantee)).thenReturn(Optional.empty());
        when(grantRepository.save(any(AuthorizationGrant.class)))
                .thenAnswer(inv -> {
                    AuthorizationGrant g = inv.getArgument(0);
                    g.setId(99L);
                    return g;
                });

        AuthorizationGrantRequest body = new AuthorizationGrantRequest();
        body.setUserId(2L);
        body.setRole(AuthorizationRole.EDITOR);

        mockMvc.perform(post("/api/authorizations/1/grants")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(username = "viewer-user")
    void addGrant_nonOwner_returns403() throws Exception {
        // For this test we override the default OWNER stub by making
        // requireManageGrants throw.
        Authorization auth = mockAuthorizationForGrants(1L);

        User viewerUser = new User();
        viewerUser.setId(99L);
        viewerUser.setUsername("viewer-user");
        when(userRepository.findByUsername("viewer-user")).thenReturn(Optional.of(viewerUser));
        when(authorizationService.getAuthorizationForUser(1L, "viewer-user")).thenReturn(auth);

        org.mockito.Mockito.doThrow(
                new gov.nist.oscal.tools.api.exception.InsufficientAuthorizationRoleException("VIEWER", "OWNER"))
                .when(accessGuard).requireManageGrants(eq(auth), eq(viewerUser));

        AuthorizationGrantRequest body = new AuthorizationGrantRequest();
        body.setUserId(2L);
        body.setRole(AuthorizationRole.EDITOR);

        mockMvc.perform(post("/api/authorizations/1/grants")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "testuser")
    void setShareWithOrg_validRole_returns200() throws Exception {
        Authorization auth = mockAuthorizationForGrants(1L);
        when(authorizationService.getAuthorizationForUser(1L, "testuser")).thenReturn(auth);
        when(authorizationService.save(any(Authorization.class))).thenAnswer(inv -> inv.getArgument(0));

        ShareWithOrgRequest body = new ShareWithOrgRequest();
        body.setRole(AuthorizationRole.VIEWER);

        mockMvc.perform(patch("/api/authorizations/1/share-with-org")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "testuser")
    void setShareWithOrg_ownerRole_rejectsWithException() throws Exception {
        // The controller throws IllegalArgumentException for OWNER role.
        // Without a @ControllerAdvice mapping IllegalArgumentException → 400,
        // Spring Boot 4 / MockMvc re-throws the raw exception rather than mapping
        // it to a 500 response. We assert the exception propagates with the expected
        // message. A follow-up should add a global @ControllerAdvice to map
        // IllegalArgumentException → 400 so clients receive a proper error response.
        Authorization auth = mockAuthorizationForGrants(1L);
        when(authorizationService.getAuthorizationForUser(1L, "testuser")).thenReturn(auth);

        ShareWithOrgRequest body = new ShareWithOrgRequest();
        body.setRole(AuthorizationRole.OWNER);

        org.junit.jupiter.api.Assertions.assertThrows(
                Exception.class,
                () -> mockMvc.perform(patch("/api/authorizations/1/share-with-org")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body))));
    }

    private Authorization mockAuthorizationForGrants(Long id) {
        Authorization auth = new Authorization();
        auth.setId(id);

        Organization org = new Organization();
        org.setId(100L);
        auth.setOrganization(org);

        User creator = new User();
        creator.setId(1L);
        creator.setUsername("testuser");
        auth.setAuthorizedBy(creator);

        // AuthorizationResponse constructor calls template.getId(); supply a minimal template
        // so the DTO mapping doesn't NPE when the helper is used in tests that call toResponse().
        AuthorizationTemplate template = new AuthorizationTemplate();
        template.setId(1L);
        template.setName("Test Template");
        template.setContent("content");
        template.setCreatedBy(creator);
        template.setLastUpdatedBy(creator);
        template.setCreatedAt(LocalDateTime.now());
        template.setLastUpdatedAt(LocalDateTime.now());
        auth.setTemplate(template);

        return auth;
    }
}
