/*
 * SPDX-FileCopyrightText: none
 * SPDX-License-Identifier: CC0-1.0
 */

package gov.nist.oscal.tools.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import gov.nist.oscal.tools.api.config.RateLimitConfig;
import gov.nist.oscal.tools.api.config.SecurityHeadersConfig;
import gov.nist.oscal.tools.api.entity.User;
import gov.nist.oscal.tools.api.config.GlobalErrorAdvice;
import gov.nist.oscal.tools.api.exception.UsernameAlreadyExistsException;
import gov.nist.oscal.tools.api.model.AuthRequest;
import gov.nist.oscal.tools.api.model.AuthResponse;
import gov.nist.oscal.tools.api.model.RegisterRequest;
import gov.nist.oscal.tools.api.model.ServiceAccountTokenRequest;
import gov.nist.oscal.tools.api.security.JwtUtil;
import gov.nist.oscal.tools.api.service.AuthService;
import gov.nist.oscal.tools.api.service.FileValidationService;
import gov.nist.oscal.tools.api.service.RateLimitService;
import gov.nist.oscal.tools.api.telemetry.TelemetryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@Import(GlobalErrorAdvice.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private gov.nist.oscal.tools.api.service.PasswordResetService passwordResetService;

    @MockitoBean
    private gov.nist.oscal.tools.api.service.PasswordValidationService passwordValidationService;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private FileValidationService fileValidationService;

    @MockitoBean
    private RateLimitService rateLimitService;

    @MockitoBean
    private RateLimitConfig rateLimitConfig;

    @MockitoBean
    private SecurityHeadersConfig securityHeadersConfig;

    @MockitoBean
    private TelemetryService telemetryService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setPassword("hashedPassword");
        testUser.setEnabled(true);
        testUser.setStreet("123 Main St");
        testUser.setCity("TestCity");
        testUser.setState("TS");
        testUser.setZip("12345");
        testUser.setTitle("Test Engineer");
        testUser.setOrganization("Test Org");
        testUser.setPhoneNumber("555-1234");
    }

    @Test
    void testRegister_success() throws Exception {
        // Given
        RegisterRequest request = new RegisterRequest();
        request.setUsername("newuser");
        request.setPassword("password123");
        request.setEmail("newuser@example.com");

        AuthResponse response = new AuthResponse();
        response.setUsername("newuser");
        response.setEmail("newuser@example.com");
        response.setToken("jwt-token-123");

        when(authService.register(any(RegisterRequest.class))).thenReturn(response);

        // When & Then
        mockMvc.perform(post("/api/auth/register")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("newuser"))
                .andExpect(jsonPath("$.email").value("newuser@example.com"))
                .andExpect(jsonPath("$.token").value("jwt-token-123"));

        verify(authService, times(1)).register(any(RegisterRequest.class));
    }

    /**
     * Password-policy rejections surface as IllegalArgumentException and must
     * reach the client as 400 with the human-readable reason in `message`.
     * The frontend shows `message` to the user; when this contract broke,
     * users saw a bare "Bad Request" with no explanation of what to fix
     * (observed in production logs: registration attempts for 'iorga' failed
     * 3x on 2026-07-23 and the user gave up).
     */
    @Test
    void testRegister_weakPassword_returnsReasonInMessage() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("newuser");
        request.setPassword("weakpassword1!");
        request.setEmail("newuser@example.com");

        when(authService.register(any(RegisterRequest.class)))
                .thenThrow(new IllegalArgumentException(
                        "Password must contain at least one uppercase letter"));

        mockMvc.perform(post("/api/auth/register")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value(containsString("uppercase letter")));
    }

    @Test
    void testPasswordPolicy_isPublicAndReturnsRules() throws Exception {
        when(passwordValidationService.getPolicyDescriptor()).thenReturn(Map.of(
                "minLength", 10,
                "maxLength", 128,
                "requireUppercase", true));

        mockMvc.perform(get("/api/auth/password-policy"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.minLength").value(10))
                .andExpect(jsonPath("$.requireUppercase").value(true));
    }

    /**
     * Bean-validation failures must carry a human-readable `message` — Spring's
     * default rendering omits it, leaving the frontend with only "Bad Request"
     * (the unreadable-error failure mode). The prod canary asserts this contract.
     */
    @Test
    void testRegister_beanValidationFailure_includesMessageField() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\": \"u\", \"email\": \"not-an-email\", \"password\": \"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    void testForgotPassword_alwaysReturns200_evenWhenServiceFails() throws Exception {
        // Anti-enumeration contract: identical response whether or not the
        // identifier matched — even if the service blows up internally.
        doThrow(new RuntimeException("db down")).when(passwordResetService).requestReset(anyString());

        mockMvc.perform(post("/api/auth/forgot-password")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"usernameOrEmail\": \"whoever\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", containsString("If an account matches")));
    }

    @Test
    void testResetPassword_invalidToken_returns400WithMessage() throws Exception {
        doThrow(new IllegalArgumentException(
                "This password reset link is invalid or has expired. Please request a new one."))
                .when(passwordResetService).resetPassword(anyString(), anyString());

        mockMvc.perform(post("/api/auth/reset-password")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\": \"bogus\", \"newPassword\": \"BrandNew!Passw0rd\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("invalid or has expired")));
    }

    @Test
    void testResetPassword_success_returns200() throws Exception {
        mockMvc.perform(post("/api/auth/reset-password")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\": \"good-token\", \"newPassword\": \"BrandNew!Passw0rd\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", containsString("has been reset")));

        verify(passwordResetService).resetPassword("good-token", "BrandNew!Passw0rd");
    }

    @Test
    void testRegister_duplicateUsername() throws Exception {
        // Given
        RegisterRequest request = new RegisterRequest();
        request.setUsername("existing");
        request.setPassword("password123");
        request.setEmail("new@example.com");

        when(authService.register(any(RegisterRequest.class)))
                .thenThrow(new UsernameAlreadyExistsException("Username already exists"));

        // When & Then
        mockMvc.perform(post("/api/auth/register")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Username already exists"));

        verify(authService, times(1)).register(any(RegisterRequest.class));
    }

    @Test
    void testRegister_dataIntegrityViolation_returns409WithoutLeakingDetails() throws Exception {
        // Reproduces the scenario in the bug screenshot: a database-level constraint
        // violation escapes the service. The response must not leak SQL fragments,
        // table/column names, constraint identifiers, or another user's email.
        String leakyMessage = "could not execute statement [ERROR: duplicate key value violates "
                + "unique constraint \"uk6dotkott2kjsp8vw4d0m25fb7\" Detail: Key (email)="
                + "(victim@example.com) already exists.] [insert into users "
                + "(account_locked_until,city,created_at,email) values (?,?,?,?)]";

        RegisterRequest request = new RegisterRequest();
        request.setUsername("newuser");
        request.setPassword("password123");
        request.setEmail("victim@example.com");

        when(authService.register(any(RegisterRequest.class)))
                .thenThrow(new DataIntegrityViolationException(leakyMessage));

        // When & Then
        mockMvc.perform(post("/api/auth/register")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error", not(containsString("constraint"))))
                .andExpect(jsonPath("$.error", not(containsString("SQL"))))
                .andExpect(jsonPath("$.error", not(containsString("insert into"))))
                .andExpect(jsonPath("$.error", not(containsString("duplicate key"))))
                .andExpect(jsonPath("$.error", not(containsString("uk6dotkott"))))
                .andExpect(jsonPath("$.error", not(containsString("victim@example.com"))));
    }

    @Test
    void testRegister_unhandledException_returns500WithGenericMessage() throws Exception {
        // Any unexpected RuntimeException should map to 500 with a generic message,
        // never echoing the original message back to the client.
        RegisterRequest request = new RegisterRequest();
        request.setUsername("newuser");
        request.setPassword("password123");
        request.setEmail("new@example.com");

        when(authService.register(any(RegisterRequest.class)))
                .thenThrow(new RuntimeException("internal: connection refused at jdbc:postgresql://internal-host:5432/proddb"));

        // When & Then
        mockMvc.perform(post("/api/auth/register")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error", not(containsString("jdbc:"))))
                .andExpect(jsonPath("$.error", not(containsString("internal-host"))))
                .andExpect(jsonPath("$.error", not(containsString("proddb"))));
    }

    @Test
    void testLogin_success() throws Exception {
        // Given
        AuthRequest request = new AuthRequest();
        request.setUsername("testuser");
        request.setPassword("password123");

        AuthResponse response = new AuthResponse();
        response.setUsername("testuser");
        response.setEmail("test@example.com");
        response.setToken("jwt-token-123");
        response.setUserId(1L);

        when(authService.login(any(AuthRequest.class))).thenReturn(response);

        // When & Then
        mockMvc.perform(post("/api/auth/login")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(jsonPath("$.email").value("test@example.com"))
                .andExpect(jsonPath("$.token").value("jwt-token-123"));

        verify(authService, times(1)).login(any(AuthRequest.class));
    }

    @Test
    void testLogin_invalidCredentials() throws Exception {
        // Given
        AuthRequest request = new AuthRequest();
        request.setUsername("testuser");
        request.setPassword("wrongpassword");

        when(authService.login(any(AuthRequest.class)))
                .thenThrow(new RuntimeException("Invalid credentials"));

        // When & Then
        mockMvc.perform(post("/api/auth/login")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Invalid username or password"));

        verify(authService, times(1)).login(any(AuthRequest.class));
    }

    @Test
    @WithMockUser(username = "testuser")
    void testGetCurrentUser_authenticated() throws Exception {
        // Given
        when(authService.getCurrentUser("testuser")).thenReturn(testUser);

        // When & Then
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(jsonPath("$.email").value("test@example.com"))
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.street").value("123 Main St"))
                .andExpect(jsonPath("$.city").value("TestCity"))
                .andExpect(jsonPath("$.state").value("TS"))
                .andExpect(jsonPath("$.zip").value("12345"))
                .andExpect(jsonPath("$.title").value("Test Engineer"))
                .andExpect(jsonPath("$.organization").value("Test Org"))
                .andExpect(jsonPath("$.phoneNumber").value("555-1234"));

        verify(authService, times(1)).getCurrentUser("testuser");
    }

    @Test
    void testGetCurrentUser_notAuthenticated() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Not authenticated"));

        verify(authService, never()).getCurrentUser(anyString());
    }

    @Test
    void testLogout_returnsSuccess() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/auth/logout")
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Logout successful"));
    }

    @Test
    @WithMockUser(username = "testuser")
    void testRefreshToken_authenticated() throws Exception {
        // Given
        when(authService.getCurrentUser("testuser")).thenReturn(testUser);
        when(authService.generateToken(any(UserDetails.class))).thenReturn("new-jwt-token-456");

        // When & Then
        mockMvc.perform(post("/api/auth/refresh")
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("new-jwt-token-456"))
                .andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(jsonPath("$.email").value("test@example.com"));

        verify(authService, times(1)).getCurrentUser("testuser");
        verify(authService, times(1)).generateToken(any(UserDetails.class));
    }

    @Test
    void testRefreshToken_notAuthenticated() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/auth/refresh")
                .with(csrf()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Not authenticated"));

        verify(authService, never()).getCurrentUser(anyString());
        verify(authService, never()).generateToken(any(UserDetails.class));
    }

    @Test
    @WithMockUser(username = "testuser")
    void testUpdateProfile_success() throws Exception {
        // Given
        Map<String, String> updates = new HashMap<>();
        updates.put("email", "newemail@example.com");
        updates.put("city", "NewCity");

        User updatedUser = new User();
        updatedUser.setUsername("testuser");
        updatedUser.setEmail("newemail@example.com");
        updatedUser.setCity("NewCity");

        when(authService.updateProfile(eq("testuser"), any())).thenReturn(updatedUser);

        // When & Then
        mockMvc.perform(put("/api/auth/profile")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updates)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Profile updated successfully"))
                .andExpect(jsonPath("$.email").value("newemail@example.com"))
                .andExpect(jsonPath("$.city").value("NewCity"));

        verify(authService, times(1)).updateProfile(eq("testuser"), any());
    }

    @Test
    void testUpdateProfile_notAuthenticated() throws Exception {
        // Given
        Map<String, String> updates = new HashMap<>();
        updates.put("email", "newemail@example.com");

        // When & Then
        mockMvc.perform(put("/api/auth/profile")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updates)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Not authenticated"));

        verify(authService, never()).updateProfile(anyString(), any());
    }

    @Test
    @WithMockUser(username = "testuser")
    void testUploadLogo_success() throws Exception {
        // Given
        Map<String, String> logoData = new HashMap<>();
        logoData.put("logo", "data:image/png;base64,iVBORw0KGgoAAAANS");

        User updatedUser = new User();
        updatedUser.setUsername("testuser");
        updatedUser.setLogo("data:image/png;base64,iVBORw0KGgoAAAANS");

        // Mock validation to not throw any exception (successful validation)
        doNothing().when(fileValidationService).validateBase64Logo(anyString());
        when(authService.updateLogo(eq("testuser"), anyString())).thenReturn(updatedUser);

        // When & Then
        mockMvc.perform(post("/api/auth/logo")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(logoData)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Logo uploaded successfully"))
                .andExpect(jsonPath("$.logo").value("data:image/png;base64,iVBORw0KGgoAAAANS"));

        verify(fileValidationService, times(1)).validateBase64Logo(anyString());
        verify(authService, times(1)).updateLogo(eq("testuser"), anyString());
    }

    @Test
    @WithMockUser(username = "testuser")
    void testUploadLogo_invalidDataUrl() throws Exception {
        // Given
        Map<String, String> logoData = new HashMap<>();
        logoData.put("logo", "not-a-data-url");

        // Mock validation to throw exception for invalid data URL
        doThrow(new IllegalArgumentException("Logo must be a valid data URL (data:image/...)"))
                .when(fileValidationService).validateBase64Logo("not-a-data-url");

        // When & Then
        mockMvc.perform(post("/api/auth/logo")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(logoData)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Logo must be a valid data URL (data:image/...)"));

        verify(fileValidationService, times(1)).validateBase64Logo("not-a-data-url");
        verify(authService, never()).updateLogo(anyString(), anyString());
    }

    @Test
    @WithMockUser(username = "testuser")
    void testUploadLogo_emptyLogo() throws Exception {
        // Given
        Map<String, String> logoData = new HashMap<>();
        logoData.put("logo", "");

        // Mock validation to throw exception for empty logo
        doThrow(new IllegalArgumentException("Logo data is required"))
                .when(fileValidationService).validateBase64Logo("");

        // When & Then
        mockMvc.perform(post("/api/auth/logo")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(logoData)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Logo data is required"));

        verify(fileValidationService, times(1)).validateBase64Logo("");
        verify(authService, never()).updateLogo(anyString(), anyString());
    }

    @Test
    @WithMockUser(username = "testuser")
    void testGenerateServiceAccountToken_success() throws Exception {
        // Given
        ServiceAccountTokenRequest request = new ServiceAccountTokenRequest();
        request.setTokenName("CI/CD Token");
        request.setExpirationDays(90);

        Date expirationDate = new Date(System.currentTimeMillis() + 90L * 24 * 60 * 60 * 1000);

        when(authService.generateServiceAccountToken(eq("testuser"), eq("CI/CD Token"), eq(90)))
                .thenReturn(expirationDate);
        when(jwtUtil.generateServiceAccountToken(eq("testuser"), eq("CI/CD Token"), eq(90)))
                .thenReturn("service-account-token-789");

        // When & Then
        mockMvc.perform(post("/api/auth/service-account-token")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("service-account-token-789"))
                .andExpect(jsonPath("$.tokenName").value("CI/CD Token"))
                .andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(jsonPath("$.expirationDays").value(90));

        verify(authService, times(1)).generateServiceAccountToken(eq("testuser"), eq("CI/CD Token"), eq(90));
        verify(jwtUtil, times(1)).generateServiceAccountToken(eq("testuser"), eq("CI/CD Token"), eq(90));
    }

    @Test
    void testGenerateServiceAccountToken_notAuthenticated() throws Exception {
        // Given
        ServiceAccountTokenRequest request = new ServiceAccountTokenRequest();
        request.setTokenName("CI/CD Token");
        request.setExpirationDays(90);

        // When & Then
        mockMvc.perform(post("/api/auth/service-account-token")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Not authenticated"));

        verify(authService, never()).generateServiceAccountToken(anyString(), anyString(), anyInt());
        verify(jwtUtil, never()).generateServiceAccountToken(anyString(), anyString(), anyInt());
    }

    @Test
    @WithMockUser(username = "testuser")
    void testUpdateProfile_serviceException() throws Exception {
        // Given — an unexpected RuntimeException from the service should now map to 500
        // generic, not echo the message back. This is the leak-prevention contract.
        Map<String, String> updates = new HashMap<>();
        updates.put("email", "invalid-email");

        when(authService.updateProfile(eq("testuser"), any()))
                .thenThrow(new RuntimeException("internal: jdbc connection failure"));

        // When & Then
        mockMvc.perform(put("/api/auth/profile")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updates)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error", not(containsString("jdbc"))));

        verify(authService, times(1)).updateProfile(eq("testuser"), any());
    }

    @Test
    @WithMockUser(username = "testuser")
    void testUpdateProfile_dataIntegrityViolation_doesNotLeakDetails() throws Exception {
        // Same leak-prevention contract for the profile-update path.
        Map<String, String> updates = new HashMap<>();
        updates.put("email", "victim@example.com");

        String leakyMessage = "could not execute statement [ERROR: duplicate key value "
                + "violates unique constraint \"uk6dotkott2kjsp8vw4d0m25fb7\" Detail: "
                + "Key (email)=(victim@example.com) already exists.]";

        when(authService.updateProfile(eq("testuser"), any()))
                .thenThrow(new DataIntegrityViolationException(leakyMessage));

        mockMvc.perform(put("/api/auth/profile")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updates)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error", not(containsString("constraint"))))
                .andExpect(jsonPath("$.error", not(containsString("uk6dotkott"))))
                .andExpect(jsonPath("$.error", not(containsString("victim@example.com"))));
    }

    @Test
    void testUploadLogo_notAuthenticated() throws Exception {
        // Given
        Map<String, String> logoData = new HashMap<>();
        logoData.put("logo", "data:image/png;base64,iVBORw0KGgoAAAANS");

        // When & Then
        mockMvc.perform(post("/api/auth/logo")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(logoData)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Not authenticated"));

        verify(authService, never()).updateLogo(anyString(), anyString());
    }

    @Test
    @WithMockUser(username = "testuser")
    void testUploadLogo_serviceException() throws Exception {
        // Given
        Map<String, String> logoData = new HashMap<>();
        logoData.put("logo", "data:image/png;base64,iVBORw0KGgoAAAANS");

        // Mock validation to pass
        doNothing().when(fileValidationService).validateBase64Logo(anyString());
        when(authService.updateLogo(eq("testuser"), anyString()))
                .thenThrow(new RuntimeException("Failed to save logo"));

        // When & Then
        mockMvc.perform(post("/api/auth/logo")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(logoData)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("Failed to upload logo: Failed to save logo"));

        verify(fileValidationService, times(1)).validateBase64Logo(anyString());
        verify(authService, times(1)).updateLogo(eq("testuser"), anyString());
    }

    @Test
    @WithMockUser(username = "testuser")
    void testUploadLogo_nullLogo() throws Exception {
        // Given
        Map<String, String> logoData = new HashMap<>();
        // logo key is missing

        // Mock validation to throw exception for null logo
        doThrow(new IllegalArgumentException("Logo data is required"))
                .when(fileValidationService).validateBase64Logo(null);

        // When & Then
        mockMvc.perform(post("/api/auth/logo")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(logoData)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Logo data is required"));

        verify(fileValidationService, times(1)).validateBase64Logo(null);
        verify(authService, never()).updateLogo(anyString(), anyString());
    }

    @Test
    @WithMockUser(username = "testuser")
    void testGenerateServiceAccountToken_serviceException() throws Exception {
        // Given
        ServiceAccountTokenRequest request = new ServiceAccountTokenRequest();
        request.setTokenName("CI/CD Token");
        request.setExpirationDays(90);

        when(authService.generateServiceAccountToken(eq("testuser"), eq("CI/CD Token"), eq(90)))
                .thenThrow(new RuntimeException("Invalid expiration days"));

        // When & Then
        mockMvc.perform(post("/api/auth/service-account-token")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid expiration days"));

        verify(authService, times(1)).generateServiceAccountToken(eq("testuser"), eq("CI/CD Token"), eq(90));
        verify(jwtUtil, never()).generateServiceAccountToken(anyString(), anyString(), anyInt());
    }
}
