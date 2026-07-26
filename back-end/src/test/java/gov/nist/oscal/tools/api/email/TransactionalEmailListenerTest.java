/*
 * SPDX-FileCopyrightText: none
 * SPDX-License-Identifier: CC0-1.0
 */

package gov.nist.oscal.tools.api.email;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import gov.nist.oscal.tools.api.entity.User;
import gov.nist.oscal.tools.api.repository.OrganizationMembershipRepository;
import gov.nist.oscal.tools.api.repository.UserAccessRequestRepository;
import gov.nist.oscal.tools.api.repository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TransactionalEmailListenerTest {

    @Mock private EmailService emailService;
    @Mock private UserRepository userRepository;
    @Mock private UserAccessRequestRepository accessRequestRepository;
    @Mock private OrganizationMembershipRepository membershipRepository;

    @InjectMocks
    private TransactionalEmailListener listener;

    private User user() {
        User u = new User();
        u.setId(9L);
        u.setUsername("someone");
        u.setEmail("someone@example.com");
        return u;
    }

    @Test
    void welcomeEmailReloadsUserAndSends() {
        when(userRepository.findById(9L)).thenReturn(Optional.of(user()));

        listener.onWelcomeEmail(new EmailEvents.WelcomeEmail(9L));

        verify(emailService).sendWelcome(any(User.class));
    }

    @Test
    void missingUserIsANoOp() {
        when(userRepository.findById(9L)).thenReturn(Optional.empty());

        listener.onWelcomeEmail(new EmailEvents.WelcomeEmail(9L));

        verify(emailService, never()).sendWelcome(any());
    }

    @Test
    void passwordResetLinkSendsWithEventValues() {
        when(userRepository.findById(9L)).thenReturn(Optional.of(user()));

        listener.onPasswordResetLink(
                new EmailEvents.PasswordResetLinkEmail(9L, "https://x/reset-password?token=abc", 60));

        verify(emailService).sendPasswordResetLink(any(User.class),
                eq("https://x/reset-password?token=abc"), eq(60));
    }

    @Test
    void transientFailureIsRetriedOnce() {
        when(userRepository.findById(9L)).thenReturn(Optional.of(user()));
        doThrow(new RuntimeException("sendgrid blip"))
                .doNothing()
                .when(emailService).sendWelcome(any(User.class));

        listener.onWelcomeEmail(new EmailEvents.WelcomeEmail(9L));

        verify(emailService, times(2)).sendWelcome(any(User.class));
    }

    @Test
    void persistentFailureIsSwallowedAfterRetries() {
        when(userRepository.findById(9L)).thenReturn(Optional.of(user()));
        doThrow(new RuntimeException("sendgrid down"))
                .when(emailService).sendWelcome(any(User.class));

        // Must not throw — the user-facing operation already committed.
        listener.onWelcomeEmail(new EmailEvents.WelcomeEmail(9L));

        verify(emailService, times(2)).sendWelcome(any(User.class));
    }
}
