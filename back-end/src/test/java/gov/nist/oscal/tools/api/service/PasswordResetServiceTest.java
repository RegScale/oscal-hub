/*
 * SPDX-FileCopyrightText: none
 * SPDX-License-Identifier: CC0-1.0
 */

package gov.nist.oscal.tools.api.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import gov.nist.oscal.tools.api.email.EmailEvents;
import gov.nist.oscal.tools.api.email.EmailService;
import gov.nist.oscal.tools.api.entity.User;
import gov.nist.oscal.tools.api.repository.PasswordResetTokenRepository;
import gov.nist.oscal.tools.api.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.transaction.annotation.Transactional;

/**
 * Reset emails are published as after-commit events (TransactionalEmailListener),
 * so these tests capture the published events rather than mock invocations —
 * inside a rolled-back test transaction AFTER_COMMIT listeners never run.
 */
@SpringBootTest
@Transactional
@RecordApplicationEvents
class PasswordResetServiceTest {

    @Autowired PasswordResetService service;
    @Autowired PasswordResetTokenRepository tokenRepo;
    @Autowired UserRepository userRepo;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired ApplicationEvents applicationEvents;
    @MockitoBean EmailService email;

    private static final String NEW_PASSWORD = "BrandNew!Passw0rd";

    private User makeUser(String prefix) {
        User u = new User();
        u.setUsername(prefix + "-" + System.nanoTime());
        u.setEmail(prefix + "-" + System.nanoTime() + "@example.com");
        u.setPassword(passwordEncoder.encode("CorrectH0rse!Batt"));
        u.setEnabled(true);
        return userRepo.save(u);
    }

    private List<EmailEvents.PasswordResetLinkEmail> resetEvents() {
        return applicationEvents.stream(EmailEvents.PasswordResetLinkEmail.class)
                .collect(Collectors.toList());
    }

    /** Requests a reset for the identifier and returns the raw token from the event's URL. */
    private String requestAndCaptureToken(String identifier) {
        service.requestReset(identifier);
        List<EmailEvents.PasswordResetLinkEmail> events = resetEvents();
        String url = events.get(events.size() - 1).resetUrl();
        return url.substring(url.indexOf("token=") + "token=".length());
    }

    @Test
    void requestByUsernameThenResetChangesPasswordAndConsumesToken() {
        User user = makeUser("reset");
        String rawToken = requestAndCaptureToken(user.getUsername());

        service.resetPassword(rawToken, NEW_PASSWORD);

        User reloaded = userRepo.findById(user.getId()).orElseThrow();
        assertTrue(passwordEncoder.matches(NEW_PASSWORD, reloaded.getPassword()));

        // Single use: replaying the same token must fail
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
            () -> service.resetPassword(rawToken, "Another!Passw0rd9"));
        assertTrue(ex.getMessage().contains("invalid or has expired"));
    }

    @Test
    void requestByEmailWithDuplicateAccountsEmailsEachAccount() {
        String shared = "shared-" + System.nanoTime() + "@example.com";
        User a = makeUser("pr-a");
        a.setEmail(shared);
        userRepo.save(a);
        User b = makeUser("pr-b");
        b.setEmail(shared);
        userRepo.save(b);

        service.requestReset(shared);

        // one reset event per account
        assertEquals(2, resetEvents().size());
    }

    @Test
    void requestForUnknownIdentifierIsSilent() {
        service.requestReset("nobody-" + System.nanoTime() + "@example.com");
        assertEquals(0, resetEvents().size());
    }

    @Test
    void requestForDisabledAccountSendsNothing() {
        User user = makeUser("disabled");
        user.setEnabled(false);
        userRepo.save(user);

        service.requestReset(user.getUsername());
        assertEquals(0, resetEvents().size());
    }

    @Test
    void expiredTokenIsRejected() {
        User user = makeUser("expired");
        String rawToken = requestAndCaptureToken(user.getUsername());

        var token = tokenRepo.findByTokenHash(PasswordResetService.sha256Hex(rawToken)).orElseThrow();
        token.setExpiresAt(LocalDateTime.now().minusMinutes(1));
        tokenRepo.save(token);

        assertThrows(IllegalArgumentException.class,
            () -> service.resetPassword(rawToken, NEW_PASSWORD));
    }

    @Test
    void weakNewPasswordIsRejectedAndTokenStaysUsable() {
        User user = makeUser("weakpw");
        String rawToken = requestAndCaptureToken(user.getUsername());

        assertThrows(IllegalArgumentException.class,
            () -> service.resetPassword(rawToken, "lowercase0nly!pw"));

        // The failed attempt must not consume the token
        service.resetPassword(rawToken, NEW_PASSWORD);
        assertTrue(passwordEncoder.matches(NEW_PASSWORD,
            userRepo.findById(user.getId()).orElseThrow().getPassword()));
    }

    @Test
    void resetClearsAccountLockout() {
        User user = makeUser("lockedout");
        user.setFailedLoginAttempts(7);
        user.setAccountLockedUntil(LocalDateTime.now().plusHours(1));
        userRepo.save(user);
        String rawToken = requestAndCaptureToken(user.getUsername());

        service.resetPassword(rawToken, NEW_PASSWORD);

        User reloaded = userRepo.findById(user.getId()).orElseThrow();
        assertEquals(0, reloaded.getFailedLoginAttempts());
        assertNull(reloaded.getAccountLockedUntil());
    }

    @Test
    void successfulResetInvalidatesSiblingTokens() {
        User user = makeUser("siblings");
        String first = requestAndCaptureToken(user.getUsername());
        String second = requestAndCaptureToken(user.getUsername());

        service.resetPassword(first, NEW_PASSWORD);

        assertThrows(IllegalArgumentException.class,
            () -> service.resetPassword(second, "Another!Passw0rd9"));
    }
}
