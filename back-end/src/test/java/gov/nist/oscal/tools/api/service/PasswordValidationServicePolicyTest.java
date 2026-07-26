/*
 * SPDX-FileCopyrightText: none
 * SPDX-License-Identifier: CC0-1.0
 */

package gov.nist.oscal.tools.api.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import gov.nist.oscal.tools.api.config.AccountSecurityConfig;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * The admin-editable security policy (DB) must be the authority for password
 * length bounds. Before this wiring existed, the Security Policy screen's
 * length settings were read by nothing — enforcement silently used only the
 * env config.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PasswordValidationServicePolicyTest {

    @Mock private SecurityPolicyService securityPolicyService;

    private AccountSecurityConfig config;

    @InjectMocks
    private PasswordValidationService service;

    @BeforeEach
    void setUp() {
        config = new AccountSecurityConfig(); // defaults: min 10, max 128, all rules on
        service = new PasswordValidationService(config);
        org.springframework.test.util.ReflectionTestUtils.setField(
                service, "securityPolicyService", securityPolicyService);
    }

    @Test
    void dbPolicyMinLengthIsEnforced() {
        when(securityPolicyService.getPasswordMinLength()).thenReturn(12);
        when(securityPolicyService.getPasswordMaxLength()).thenReturn(128);

        // 11 chars, satisfies every character rule — must fail only on length
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.validatePassword("Xk9!mQ2w#Zp", "someuser"));
        assertTrue(ex.getMessage().contains("at least 12"));

        // 12 chars passes
        assertDoesNotThrow(() -> service.validatePassword("Xk9!mQ2w#Zpr", "someuser"));
    }

    @Test
    void fallsBackToConfigWhenDbPolicyUnavailable() {
        when(securityPolicyService.getPasswordMinLength())
                .thenThrow(new RuntimeException("db down"));
        when(securityPolicyService.getPasswordMaxLength())
                .thenThrow(new RuntimeException("db down"));

        assertEquals(10, service.getEffectiveMinLength());
        assertEquals(128, service.getEffectiveMaxLength());
        assertDoesNotThrow(() -> service.validatePassword("Xk9!mQ2w#Z", "someuser"));
    }

    @Test
    void policyDescriptorReflectsEffectiveLengths() {
        when(securityPolicyService.getPasswordMinLength()).thenReturn(14);
        when(securityPolicyService.getPasswordMaxLength()).thenReturn(64);

        Map<String, Object> descriptor = service.getPolicyDescriptor();

        assertEquals(14, descriptor.get("minLength"));
        assertEquals(64, descriptor.get("maxLength"));
        assertEquals(true, descriptor.get("requireUppercase"));
        assertEquals("!@#$%^&*()_+-=[]{}|;:,.<>?", descriptor.get("specialCharacters"));
    }
}
