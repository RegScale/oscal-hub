/*
 * SPDX-FileCopyrightText: none
 * SPDX-License-Identifier: CC0-1.0
 */

package gov.nist.oscal.tools.api.service;

import gov.nist.oscal.tools.api.entity.SecurityPolicy;
import gov.nist.oscal.tools.api.entity.User;
import gov.nist.oscal.tools.api.model.SecurityPolicyUpdateRequest;
import gov.nist.oscal.tools.api.repository.SecurityPolicyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SecurityPolicyServiceTest {

    @Mock
    private SecurityPolicyRepository securityPolicyRepository;

    @InjectMocks
    private SecurityPolicyService securityPolicyService;

    private SecurityPolicy testPolicy;

    @BeforeEach
    void setUp() {
        // The service injects itself via @Autowired @Lazy SecurityPolicyService self
        // to invoke @Transactional methods through the Spring proxy. @InjectMocks
        // can't satisfy that field, so wire it manually to the same instance for
        // these unit tests (transactional semantics aren't being exercised here).
        ReflectionTestUtils.setField(securityPolicyService, "self", securityPolicyService);

        testPolicy = new SecurityPolicy();
        testPolicy.setId(1L);
        testPolicy.setMfaRequired(false);
        testPolicy.setPasswordMinLength(10);
        testPolicy.setPasswordMaxLength(128);
        testPolicy.setPasswordRotationDays(0);
        testPolicy.setAuditLogRetentionDays(90);
        testPolicy.setUpdatedAt(LocalDateTime.now());
        testPolicy.setUpdatedBy("admin");
    }

    // ========== GET POLICY TESTS ==========

    @Test
    void testGetPolicy_returnsPolicy() {
        // Given
        when(securityPolicyRepository.findById(SecurityPolicy.SINGLETON_ID)).thenReturn(Optional.of(testPolicy));

        // When
        SecurityPolicy result = securityPolicyService.getPolicy();

        // Then
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertFalse(result.getMfaRequired());
        assertEquals(10, result.getPasswordMinLength());
        assertEquals(90, result.getAuditLogRetentionDays());
    }

    // ========== UPDATE POLICY TESTS ==========

    @Test
    void testUpdatePolicy_validRequest_updatesAll() {
        // Given
        SecurityPolicyUpdateRequest request = new SecurityPolicyUpdateRequest();
        request.setMfaRequired(true);
        request.setPasswordMinLength(12);
        request.setPasswordMaxLength(64);
        request.setPasswordRotationDays(90);
        request.setAuditLogRetentionDays(180);

        when(securityPolicyRepository.findById(SecurityPolicy.SINGLETON_ID)).thenReturn(Optional.of(testPolicy));
        when(securityPolicyRepository.getPolicy()).thenReturn(testPolicy);
        when(securityPolicyRepository.save(any(SecurityPolicy.class))).thenAnswer(inv -> inv.getArgument(0));

        // When
        SecurityPolicy result = securityPolicyService.updatePolicy(request, "superadmin");

        // Then
        assertTrue(result.getMfaRequired());
        assertEquals(12, result.getPasswordMinLength());
        assertEquals(64, result.getPasswordMaxLength());
        assertEquals(90, result.getPasswordRotationDays());
        assertEquals(180, result.getAuditLogRetentionDays());
        assertEquals("superadmin", result.getUpdatedBy());
        assertNotNull(result.getUpdatedAt());

        verify(securityPolicyRepository).save(any(SecurityPolicy.class));
    }

    @Test
    void testUpdatePolicy_minGreaterThanMax_throwsException() {
        // Given
        SecurityPolicyUpdateRequest request = new SecurityPolicyUpdateRequest();
        request.setMfaRequired(false);
        request.setPasswordMinLength(50);  // Min > Max
        request.setPasswordMaxLength(30);
        request.setPasswordRotationDays(0);
        request.setAuditLogRetentionDays(90);

        when(securityPolicyRepository.findById(SecurityPolicy.SINGLETON_ID)).thenReturn(Optional.of(testPolicy));
        when(securityPolicyRepository.getPolicy()).thenReturn(testPolicy);

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            securityPolicyService.updatePolicy(request, "admin");
        });

        verify(securityPolicyRepository, never()).save(any(SecurityPolicy.class));
    }

    // ========== MFA REQUIRED TESTS ==========

    @Test
    void testIsMfaRequired_whenEnabled_returnsTrue() {
        // Given
        testPolicy.setMfaRequired(true);
        when(securityPolicyRepository.findById(SecurityPolicy.SINGLETON_ID)).thenReturn(Optional.of(testPolicy));

        // When
        boolean result = securityPolicyService.isMfaRequired();

        // Then
        assertTrue(result);
    }

    @Test
    void testIsMfaRequired_whenDisabled_returnsFalse() {
        // Given
        testPolicy.setMfaRequired(false);
        when(securityPolicyRepository.findById(SecurityPolicy.SINGLETON_ID)).thenReturn(Optional.of(testPolicy));

        // When
        boolean result = securityPolicyService.isMfaRequired();

        // Then
        assertFalse(result);
    }

    // ========== PASSWORD EXPIRATION TESTS ==========

    @Test
    void testIsPasswordExpired_rotationDisabled_returnsFalse() {
        // Given
        testPolicy.setPasswordRotationDays(0); // Disabled
        when(securityPolicyRepository.findById(SecurityPolicy.SINGLETON_ID)).thenReturn(Optional.of(testPolicy));

        User user = new User();
        user.setPasswordChangedAt(LocalDateTime.now().minusDays(365)); // Old password

        // When
        boolean result = securityPolicyService.isPasswordExpired(user);

        // Then
        assertFalse(result);
    }

    @Test
    void testIsPasswordExpired_noPasswordChangeDate_returnsTrue() {
        // Given
        testPolicy.setPasswordRotationDays(90);
        when(securityPolicyRepository.findById(SecurityPolicy.SINGLETON_ID)).thenReturn(Optional.of(testPolicy));

        User user = new User();
        user.setPasswordChangedAt(null); // Never changed

        // When
        boolean result = securityPolicyService.isPasswordExpired(user);

        // Then
        assertTrue(result);
    }

    @Test
    void testIsPasswordExpired_passwordExpired_returnsTrue() {
        // Given
        testPolicy.setPasswordRotationDays(90);
        when(securityPolicyRepository.findById(SecurityPolicy.SINGLETON_ID)).thenReturn(Optional.of(testPolicy));

        User user = new User();
        user.setPasswordChangedAt(LocalDateTime.now().minusDays(100)); // 100 days ago

        // When
        boolean result = securityPolicyService.isPasswordExpired(user);

        // Then
        assertTrue(result);
    }

    @Test
    void testIsPasswordExpired_passwordNotExpired_returnsFalse() {
        // Given
        testPolicy.setPasswordRotationDays(90);
        when(securityPolicyRepository.findById(SecurityPolicy.SINGLETON_ID)).thenReturn(Optional.of(testPolicy));

        User user = new User();
        user.setPasswordChangedAt(LocalDateTime.now().minusDays(30)); // 30 days ago

        // When
        boolean result = securityPolicyService.isPasswordExpired(user);

        // Then
        assertFalse(result);
    }

    // ========== DAYS UNTIL EXPIRATION TESTS ==========

    @Test
    void testGetDaysUntilPasswordExpires_rotationDisabled_returnsMinusOne() {
        // Given
        testPolicy.setPasswordRotationDays(0); // Disabled
        when(securityPolicyRepository.findById(SecurityPolicy.SINGLETON_ID)).thenReturn(Optional.of(testPolicy));

        User user = new User();
        user.setPasswordChangedAt(LocalDateTime.now());

        // When
        long result = securityPolicyService.getDaysUntilPasswordExpires(user);

        // Then
        assertEquals(-1, result);
    }

    @Test
    void testGetDaysUntilPasswordExpires_noPasswordChangeDate_returnsZero() {
        // Given
        testPolicy.setPasswordRotationDays(90);
        when(securityPolicyRepository.findById(SecurityPolicy.SINGLETON_ID)).thenReturn(Optional.of(testPolicy));

        User user = new User();
        user.setPasswordChangedAt(null);

        // When
        long result = securityPolicyService.getDaysUntilPasswordExpires(user);

        // Then
        assertEquals(0, result);
    }

    @Test
    void testGetDaysUntilPasswordExpires_returnsCorrectDays() {
        // Given
        testPolicy.setPasswordRotationDays(90);
        when(securityPolicyRepository.findById(SecurityPolicy.SINGLETON_ID)).thenReturn(Optional.of(testPolicy));

        User user = new User();
        user.setPasswordChangedAt(LocalDateTime.now().minusDays(30)); // 30 days ago, 60 days left

        // When
        long result = securityPolicyService.getDaysUntilPasswordExpires(user);

        // Then
        // Should be approximately 60 days (allow for time during test execution)
        assertTrue(result >= 59 && result <= 60);
    }

    // ========== GETTER TESTS ==========

    @Test
    void testGetAuditLogRetentionDays_returnsConfiguredValue() {
        // Given
        testPolicy.setAuditLogRetentionDays(180);
        when(securityPolicyRepository.findById(SecurityPolicy.SINGLETON_ID)).thenReturn(Optional.of(testPolicy));

        // When
        int result = securityPolicyService.getAuditLogRetentionDays();

        // Then
        assertEquals(180, result);
    }

    @Test
    void testGetPasswordMinLength_returnsConfiguredValue() {
        // Given
        testPolicy.setPasswordMinLength(12);
        when(securityPolicyRepository.findById(SecurityPolicy.SINGLETON_ID)).thenReturn(Optional.of(testPolicy));

        // When
        int result = securityPolicyService.getPasswordMinLength();

        // Then
        assertEquals(12, result);
    }

    @Test
    void testGetPasswordMaxLength_returnsConfiguredValue() {
        // Given
        testPolicy.setPasswordMaxLength(64);
        when(securityPolicyRepository.findById(SecurityPolicy.SINGLETON_ID)).thenReturn(Optional.of(testPolicy));

        // When
        int result = securityPolicyService.getPasswordMaxLength();

        // Then
        assertEquals(64, result);
    }
}
