/*
 * SPDX-FileCopyrightText: none
 * SPDX-License-Identifier: CC0-1.0
 */

package gov.nist.oscal.tools.api.service;

import gov.nist.oscal.tools.api.email.EmailService;
import gov.nist.oscal.tools.api.entity.Organization;
import gov.nist.oscal.tools.api.entity.OrganizationMembership;
import gov.nist.oscal.tools.api.entity.User;
import gov.nist.oscal.tools.api.exception.UsernameAlreadyExistsException;
import gov.nist.oscal.tools.api.model.AuthRequest;
import gov.nist.oscal.tools.api.model.AuthResponse;
import gov.nist.oscal.tools.api.model.RegisterRequest;
import gov.nist.oscal.tools.api.repository.OrganizationMembershipRepository;
import gov.nist.oscal.tools.api.repository.OrganizationRepository;
import gov.nist.oscal.tools.api.repository.UserRepository;
import gov.nist.oscal.tools.api.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
// Use lenient stubbing because some tests have stubs for email validation
// that aren't implemented yet (existsByEmail, findByEmail). These stubs
// prepare for future validation but shouldn't fail the tests.
@MockitoSettings(strictness = Strictness.LENIENT)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private gov.nist.oscal.tools.api.repository.ServiceAccountTokenRepository serviceAccountTokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private UserDetailsService userDetailsService;

    @Mock
    private PasswordValidationService passwordValidationService;

    @Mock
    private LoginAttemptService loginAttemptService;

    @Mock
    private AuditLogService auditLogService;

    @Mock
    private EmailService emailService;

    @Mock
    private OrganizationRepository organizationRepository;

    @Mock
    private OrganizationMembershipRepository membershipRepository;

    @Mock
    private SecurityPolicyService securityPolicyService;

    @Mock
    private gov.nist.oscal.tools.api.util.ClientIpResolver clientIpResolver;

    @Mock
    private gov.nist.oscal.tools.api.config.AccountSecurityConfig accountSecurityConfig;

    @Mock
    private org.springframework.context.ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private AuthService authService;

    private User testUser;
    private UserDetails mockUserDetails;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setPassword("encodedPassword");
        testUser.setEnabled(true);

        mockUserDetails = org.springframework.security.core.userdetails.User.builder()
                .username("testuser")
                .password("encodedPassword")
                .authorities("USER")
                .build();

        // Realistic lockout policy defaults for the DB-backed lockout logic
        when(accountSecurityConfig.isLockoutEnabled()).thenReturn(true);
        when(accountSecurityConfig.getLockoutMaxAttempts()).thenReturn(5);
        when(accountSecurityConfig.getLockoutDurationSeconds()).thenReturn(900L);
        when(accountSecurityConfig.getLockoutWindowSeconds()).thenReturn(600L);
        when(clientIpResolver.resolveCurrent()).thenReturn("203.0.113.10");
    }

    // ========== REGISTER TESTS ==========

    @Test
    void testRegister_validRequest_createsUserAndReturnsToken() {
        // Given
        RegisterRequest request = new RegisterRequest();
        request.setUsername("newuser");
        request.setEmail("new@example.com");
        request.setPassword("ValidPassword123!");

        when(userRepository.existsByUsernameIgnoreCase("newuser")).thenReturn(false);
        // Note: existsByEmail is NOT called in AuthService.register() - only username is checked
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(userRepository.saveAndFlush(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(1L);
            return user;
        });
        when(userDetailsService.loadUserByUsername("newuser")).thenReturn(mockUserDetails);
        when(jwtUtil.generateToken(any(UserDetails.class))).thenReturn("jwt-token-123");

        // Mock password validation and audit log
        doNothing().when(passwordValidationService).validatePassword(anyString(), anyString());
        doNothing().when(auditLogService).logEvent(any(), anyString(), anyLong(), anyString(), any(), anyString(), any());

        // When
        AuthResponse response = authService.register(request);

        // Then
        assertNotNull(response);
        assertEquals("newuser", response.getUsername());
        assertEquals("new@example.com", response.getEmail());
        assertEquals("jwt-token-123", response.getToken());
        assertEquals(1L, response.getUserId());

        verify(userRepository).saveAndFlush(any(User.class));
        verify(passwordEncoder).encode("ValidPassword123!");
    }

    @Test
    void testRegister_existingUsername_throwsException() {
        // Given
        RegisterRequest request = new RegisterRequest();
        request.setUsername("existinguser");
        request.setEmail("new@example.com");
        request.setPassword("ValidPassword123!");

        when(userRepository.existsByUsernameIgnoreCase("existinguser")).thenReturn(true);

        // When & Then
        UsernameAlreadyExistsException exception = assertThrows(UsernameAlreadyExistsException.class, () -> {
            authService.register(request);
        });

        assertEquals("Username already exists", exception.getMessage());
        verify(userRepository, never()).saveAndFlush(any(User.class));
    }

    @Test
    void testRegister_existingEmail_succeeds() {
        // The project intentionally allows multiple users to share an email
        // (see migrations V1.11/V1.21 — duplicate emails arise legitimately when
        // a user submits an access request and later self-registers, or holds
        // accounts across multiple organizations). Registration with an
        // already-known email must succeed without a uniqueness check.
        RegisterRequest request = new RegisterRequest();
        request.setUsername("newuser");
        request.setEmail("existing@example.com");
        request.setPassword("ValidPassword123!");

        when(userRepository.existsByUsernameIgnoreCase("newuser")).thenReturn(false);
        // Even if existsByEmail were stubbed to return true, register() must not consult it.
        when(userRepository.saveAndFlush(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            u.setId(99L);
            return u;
        });
        when(userDetailsService.loadUserByUsername("newuser")).thenReturn(mockUserDetails);
        when(jwtUtil.generateToken(any(UserDetails.class))).thenReturn("token");

        AuthResponse response = authService.register(request);

        assertNotNull(response);
        verify(userRepository).saveAndFlush(any(User.class));
        verify(userRepository, never()).existsByEmail(anyString());
    }

    @Test
    void testRegister_trimsUsernameAndEmail() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("  newuser  ");
        request.setEmail("  new@example.com  ");
        request.setPassword("ValidPassword123!");

        when(userRepository.existsByUsernameIgnoreCase("newuser")).thenReturn(false);
        when(userRepository.saveAndFlush(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            u.setId(7L);
            return u;
        });
        when(userDetailsService.loadUserByUsername("newuser")).thenReturn(mockUserDetails);
        when(jwtUtil.generateToken(any(UserDetails.class))).thenReturn("token");

        AuthResponse response = authService.register(request);

        assertEquals("newuser", response.getUsername());
        assertEquals("new@example.com", response.getEmail());
    }

    @Test
    void testRegister_mfaGloballyRequired_returnsMfaSetupInsteadOfSession() {
        // The first session must not bypass a globally required MFA setup.
        RegisterRequest request = new RegisterRequest();
        request.setUsername("newuser");
        request.setEmail("new@example.com");
        request.setPassword("ValidPassword123!");

        when(userRepository.existsByUsernameIgnoreCase("newuser")).thenReturn(false);
        when(userRepository.saveAndFlush(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            u.setId(5L);
            return u;
        });
        when(securityPolicyService.isMfaRequired()).thenReturn(true);
        when(jwtUtil.generateMfaSetupToken("newuser", 5L)).thenReturn("mfa-setup-token");

        AuthResponse response = authService.register(request);

        assertEquals(Boolean.TRUE, response.getMfaSetupRequired());
        assertEquals("mfa-setup-token", response.getMfaToken());
        assertNull(response.getToken(), "no session token until MFA setup completes");
        verify(jwtUtil, never()).generateToken(any(UserDetails.class));
    }

    @Test
    void testRegister_usernameRaceAtFlush_mapsToUsernameAlreadyExists() {
        // Two concurrent registrations can both pass the existsByUsername
        // pre-check; the loser hits the DB unique constraint at flush. That
        // must surface as the same 409-mapped UsernameAlreadyExistsException,
        // not a generic conflict.
        RegisterRequest request = new RegisterRequest();
        request.setUsername("raceduser");
        request.setEmail("race@example.com");
        request.setPassword("ValidPassword123!");

        when(userRepository.existsByUsernameIgnoreCase("raceduser")).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(userRepository.saveAndFlush(any(User.class)))
                .thenThrow(new org.springframework.dao.DataIntegrityViolationException("duplicate key"));
        doNothing().when(passwordValidationService).validatePassword(anyString(), anyString());

        UsernameAlreadyExistsException exception = assertThrows(UsernameAlreadyExistsException.class,
                () -> authService.register(request));
        assertEquals("Username already exists", exception.getMessage());
    }

    @Test
    void testRegister_weakPassword_throwsException() {
        // Given
        RegisterRequest request = new RegisterRequest();
        request.setUsername("newuser");
        request.setEmail("new@example.com");
        request.setPassword("weak"); // Too short, no special chars

        // Mock password validation to throw exception
        doThrow(new IllegalArgumentException("Password is too weak"))
                .when(passwordValidationService).validatePassword(anyString(), anyString());

        // When & Then
        assertThrows(RuntimeException.class, () -> {
            authService.register(request);
        });

        verify(userRepository, never()).save(any(User.class));
    }

    // ========== LOGIN TESTS ==========

    @Test
    void testLogin_validCredentials_returnsToken() {
        // Given
        AuthRequest request = new AuthRequest();
        request.setUsername("testuser");
        request.setPassword("password123");

        Authentication mockAuth = mock(Authentication.class);
        when(mockAuth.getPrincipal()).thenReturn(mockUserDetails);

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(mockAuth);
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(jwtUtil.generateToken(any(UserDetails.class))).thenReturn("jwt-token-456");

        // Mock login attempt service
        when(loginAttemptService.isAccountLocked(anyString())).thenReturn(false);
        when(loginAttemptService.isIpLocked(anyString())).thenReturn(false);
        doNothing().when(loginAttemptService).recordSuccessfulLogin(anyString(), anyString());
        doNothing().when(auditLogService).logAuthSuccess(anyString(), anyLong());

        // When
        AuthResponse response = authService.login(request);

        // Then
        assertNotNull(response);
        assertEquals("testuser", response.getUsername());
        assertEquals("test@example.com", response.getEmail());
        assertEquals("jwt-token-456", response.getToken());

        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(userRepository).save(argThat(user -> user.getLastLogin() != null));
    }

    @Test
    void testLogin_userNotFound_throwsException() {
        // Given
        AuthRequest request = new AuthRequest();
        request.setUsername("nonexistent");
        request.setPassword("password123");

        Authentication mockAuth = mock(Authentication.class);
        when(mockAuth.getPrincipal()).thenReturn(mockUserDetails);

        // Mock login attempt service
        when(loginAttemptService.isAccountLocked(anyString())).thenReturn(false);
        when(loginAttemptService.isIpLocked(anyString())).thenReturn(false);

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(mockAuth);
        when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        // When & Then
        assertThrows(RuntimeException.class, () -> {
            authService.login(request);
        });
    }

    @Test
    void testLogin_dbLockedAccount_rejectedEvenWithFreshInMemoryState() {
        // The DB lockout must hold on an instance that never saw the failures
        // (Cloud Run scale-out / restart wipes the in-memory caches).
        AuthRequest request = new AuthRequest();
        request.setUsername("testuser");
        request.setPassword("password123");

        testUser.setAccountLockedUntil(LocalDateTime.now().plusMinutes(10));
        when(loginAttemptService.isAccountLocked("testuser")).thenReturn(false); // fresh instance
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> authService.login(request));
        assertTrue(ex.getMessage().contains("temporarily locked"));
        verify(authenticationManager, never()).authenticate(any());
    }

    @Test
    void testLogin_reachingMaxFailuresSetsDbLock() {
        AuthRequest request = new AuthRequest();
        request.setUsername("testuser");
        request.setPassword("wrongpassword");

        testUser.setFailedLoginAttempts(4); // one away from the max of 5
        testUser.setLastFailedLogin(LocalDateTime.now().minusSeconds(30));
        when(loginAttemptService.isAccountLocked("testuser")).thenReturn(false);
        when(loginAttemptService.isIpLocked(anyString())).thenReturn(false);
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("bad"));

        assertThrows(RuntimeException.class, () -> authService.login(request));

        assertEquals(5, testUser.getFailedLoginAttempts());
        assertNotNull(testUser.getAccountLockedUntil(), "5th failure must set the DB lock");
        verify(auditLogService).logAccountLockout("testuser", 1L, 5);
    }

    @Test
    void testLogin_staleFailureWindowResetsCounter() {
        // Failures older than the sliding window must not accumulate forever —
        // the DB counter has no TTL, so a mistake months later shouldn't lock.
        AuthRequest request = new AuthRequest();
        request.setUsername("testuser");
        request.setPassword("wrongpassword");

        testUser.setFailedLoginAttempts(4);
        testUser.setLastFailedLogin(LocalDateTime.now().minusDays(30)); // far outside window
        when(loginAttemptService.isAccountLocked("testuser")).thenReturn(false);
        when(loginAttemptService.isIpLocked(anyString())).thenReturn(false);
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("bad"));

        assertThrows(RuntimeException.class, () -> authService.login(request));

        assertEquals(1, testUser.getFailedLoginAttempts(), "stale window restarts the count");
        assertNull(testUser.getAccountLockedUntil());
    }

    // ========== GET CURRENT USER TESTS ==========

    @Test
    void testGetCurrentUser_existingUser_returnsUser() {
        // Given
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        // When
        User result = authService.getCurrentUser("testuser");

        // Then
        assertNotNull(result);
        assertEquals("testuser", result.getUsername());
        assertEquals("test@example.com", result.getEmail());
    }

    @Test
    void testGetCurrentUser_nonExistentUser_throwsException() {
        // Given
        when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        // When & Then
        assertThrows(RuntimeException.class, () -> {
            authService.getCurrentUser("nonexistent");
        });
    }

    // ========== GENERATE TOKEN TESTS ==========

    @Test
    void testGenerateToken_validUserDetails_returnsToken() {
        // Given
        when(jwtUtil.generateToken(mockUserDetails)).thenReturn("generated-token");

        // When
        String token = authService.generateToken(mockUserDetails);

        // Then
        assertEquals("generated-token", token);
        verify(jwtUtil).generateToken(mockUserDetails);
    }

    // ========== UPDATE PROFILE TESTS ==========

    @Test
    void testUpdateProfile_emailUpdate_updatesSuccessfully() {
        // Given
        Map<String, String> updates = new HashMap<>();
        updates.put("email", "newemail@example.com");

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        // Note: findByEmail is NOT called in AuthService.updateProfile() - no email uniqueness check
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        User updatedUser = authService.updateProfile("testuser", updates);

        // Then
        assertEquals("newemail@example.com", updatedUser.getEmail());
        verify(userRepository).save(testUser);
    }

    @Test
    void testUpdateProfile_emailAlreadyInUse_succeeds() {
        // Email is not unique across users; updating to an email another user already
        // holds must be allowed (project intent — see V1.21 migration).
        Map<String, String> updates = new HashMap<>();
        updates.put("email", "existing@example.com");

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User updatedUser = authService.updateProfile("testuser", updates);

        assertEquals("existing@example.com", updatedUser.getEmail());
        verify(userRepository).save(testUser);
        verify(userRepository, never()).findByEmail(anyString());
    }

    @Test
    void testUpdateProfile_passwordUpdate_validatesAndEncodes() {
        // Given
        Map<String, String> updates = new HashMap<>();
        updates.put("password", "NewValidPassword123!");

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.encode("NewValidPassword123!")).thenReturn("newEncodedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Mock password validation
        doNothing().when(passwordValidationService).validatePassword(anyString(), anyString());

        // When
        User updatedUser = authService.updateProfile("testuser", updates);

        // Then
        assertEquals("newEncodedPassword", updatedUser.getPassword());
        verify(passwordEncoder).encode("NewValidPassword123!");
    }

    @Test
    void testUpdateProfile_weakPassword_throwsException() {
        // Given
        Map<String, String> updates = new HashMap<>();
        updates.put("password", "weak");

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        // Mock password validation to throw exception
        doThrow(new IllegalArgumentException("Password is too weak"))
                .when(passwordValidationService).validatePassword(anyString(), anyString());

        // When & Then
        assertThrows(RuntimeException.class, () -> {
            authService.updateProfile("testuser", updates);
        });

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void testUpdateProfile_metadataFields_updatesAll() {
        // Given
        Map<String, String> updates = new HashMap<>();
        updates.put("street", "123 Main St");
        updates.put("city", "TestCity");
        updates.put("state", "TS");
        updates.put("zip", "12345");
        updates.put("title", "Engineer");
        updates.put("organization", "TestOrg");
        updates.put("phoneNumber", "555-1234");

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        User updatedUser = authService.updateProfile("testuser", updates);

        // Then
        assertEquals("123 Main St", updatedUser.getStreet());
        assertEquals("TestCity", updatedUser.getCity());
        assertEquals("TS", updatedUser.getState());
        assertEquals("12345", updatedUser.getZip());
        assertEquals("Engineer", updatedUser.getTitle());
        assertEquals("TestOrg", updatedUser.getOrganization());
        assertEquals("555-1234", updatedUser.getPhoneNumber());
    }

    @Test
    void testUpdateProfile_userNotFound_throwsException() {
        // Given
        Map<String, String> updates = new HashMap<>();
        updates.put("email", "new@example.com");

        when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        // When & Then
        assertThrows(RuntimeException.class, () -> {
            authService.updateProfile("nonexistent", updates);
        });
    }

    // ========== UPDATE LOGO TESTS ==========

    @Test
    void testUpdateLogo_validLogo_updatesSuccessfully() {
        // Given
        String logoData = "data:image/png;base64,iVBORw0KGgo=";

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        User updatedUser = authService.updateLogo("testuser", logoData);

        // Then
        assertEquals(logoData, updatedUser.getLogo());
        verify(userRepository).save(testUser);
    }

    @Test
    void testUpdateLogo_userNotFound_throwsException() {
        // Given
        String logoData = "data:image/png;base64,iVBORw0KGgo=";

        when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        // When & Then
        assertThrows(RuntimeException.class, () -> {
            authService.updateLogo("nonexistent", logoData);
        });
    }

    // ========== SERVICE ACCOUNT TOKEN TESTS ==========

    @Test
    void testCreateServiceAccountToken_validUser_persistsRecordWithExpiry() {
        // Given
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(serviceAccountTokenRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        // When
        gov.nist.oscal.tools.api.entity.ServiceAccountToken record =
                authService.createServiceAccountToken("testuser", "CI/CD Token", 90,
                        "SUPER_ADMIN", "ORG_ADMIN", 42L);

        // Then
        assertNotNull(record);
        assertEquals(testUser, record.getUser());
        assertEquals("CI/CD Token", record.getTokenName());
        assertNotNull(record.getJti());
        assertTrue(record.getExpiresAt().isAfter(java.time.LocalDateTime.now()));
        verify(serviceAccountTokenRepository).save(record);
    }

    /** The permission snapshot is the whole point — it must survive onto the row. */
    @Test
    void testCreateServiceAccountToken_snapshotsIssuerPermissions() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(serviceAccountTokenRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        gov.nist.oscal.tools.api.entity.ServiceAccountToken record =
                authService.createServiceAccountToken("testuser", "Token", 30,
                        "SUPER_ADMIN", "ORG_ADMIN", 42L);

        assertEquals("SUPER_ADMIN", record.getGlobalRole());
        assertEquals("ORG_ADMIN", record.getOrgRole());
        assertEquals(42L, record.getOrganizationId());
    }

    /** Each mint gets a distinct jti, or revocation would kill unrelated tokens. */
    @Test
    void testCreateServiceAccountToken_generatesDistinctJtiPerToken() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(serviceAccountTokenRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        String first = authService.createServiceAccountToken("testuser", "A", 30, null, null, null).getJti();
        String second = authService.createServiceAccountToken("testuser", "B", 30, null, null, null).getJti();

        assertNotEquals(first, second);
    }

    @Test
    void testCreateServiceAccountToken_userNotFound_throwsException() {
        // Given
        when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        // When & Then
        assertThrows(RuntimeException.class, () -> {
            authService.createServiceAccountToken("nonexistent", "Token", 30, null, null, null);
        });
    }

    @Test
    void testCreateServiceAccountToken_differentExpirationDays_calculatesCorrectly() {
        // Given
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(serviceAccountTokenRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        // When
        var expiration30 = authService.createServiceAccountToken("testuser", "Token", 30,
                null, null, null).getExpiresAt();
        var expiration365 = authService.createServiceAccountToken("testuser", "Token", 365,
                null, null, null).getExpiresAt();

        // Then
        assertTrue(expiration365.isAfter(expiration30));
        assertEquals(335, java.time.temporal.ChronoUnit.DAYS.between(expiration30, expiration365));
    }
}
