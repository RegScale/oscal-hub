/*
 * SPDX-FileCopyrightText: none
 * SPDX-License-Identifier: CC0-1.0
 */

package gov.nist.oscal.tools.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import gov.nist.oscal.tools.api.entity.User;
import gov.nist.oscal.tools.api.model.AuthRequest;
import gov.nist.oscal.tools.api.model.AuthResponse;
import gov.nist.oscal.tools.api.model.RegisterRequest;
import gov.nist.oscal.tools.api.model.ServiceAccountTokenRequest;
import gov.nist.oscal.tools.api.security.JwtUtil;
import gov.nist.oscal.tools.api.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @MockBean
    private JwtUtil jwtUtil;

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

    @Test
    void testRegister_duplicateUsername() throws Exception {
        // Given
        RegisterRequest request = new RegisterRequest();
        request.setUsername("existing");
        request.setPassword("password123");
        request.setEmail("new@example.com");

        when(authService.register(any(RegisterRequest.class)))
                .thenThrow(new RuntimeException("Username already exists"));

        // When & Then
        mockMvc.perform(post("/api/auth/register")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Username already exists"));

        verify(authService, times(1)).register(any(RegisterRequest.class));
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

        when(authService.updateLogo(eq("testuser"), anyString())).thenReturn(updatedUser);

        // When & Then
        mockMvc.perform(post("/api/auth/logo")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(logoData)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Logo uploaded successfully"))
                .andExpect(jsonPath("$.logo").value("data:image/png;base64,iVBORw0KGgoAAAANS"));

        verify(authService, times(1)).updateLogo(eq("testuser"), anyString());
    }

    @Test
    @WithMockUser(username = "testuser")
    void testUploadLogo_invalidDataUrl() throws Exception {
        // Given
        Map<String, String> logoData = new HashMap<>();
        logoData.put("logo", "not-a-data-url");

        // When & Then
        mockMvc.perform(post("/api/auth/logo")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(logoData)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Logo must be a valid data URL (data:image/...)"));

        verify(authService, never()).updateLogo(anyString(), anyString());
    }

    @Test
    @WithMockUser(username = "testuser")
    void testUploadLogo_emptyLogo() throws Exception {
        // Given
        Map<String, String> logoData = new HashMap<>();
        logoData.put("logo", "");

        // When & Then
        mockMvc.perform(post("/api/auth/logo")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(logoData)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Logo data is required"));

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
        // Given
        Map<String, String> updates = new HashMap<>();
        updates.put("email", "invalid-email");

        when(authService.updateProfile(eq("testuser"), any()))
                .thenThrow(new RuntimeException("Invalid email format"));

        // When & Then
        mockMvc.perform(put("/api/auth/profile")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updates)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid email format"));

        verify(authService, times(1)).updateProfile(eq("testuser"), any());
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

        when(authService.updateLogo(eq("testuser"), anyString()))
                .thenThrow(new RuntimeException("Failed to save logo"));

        // When & Then
        mockMvc.perform(post("/api/auth/logo")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(logoData)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Failed to save logo"));

        verify(authService, times(1)).updateLogo(eq("testuser"), anyString());
    }

    @Test
    @WithMockUser(username = "testuser")
    void testUploadLogo_nullLogo() throws Exception {
        // Given
        Map<String, String> logoData = new HashMap<>();
        // logo key is missing

        // When & Then
        mockMvc.perform(post("/api/auth/logo")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(logoData)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Logo data is required"));

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
