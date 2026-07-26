package gov.nist.oscal.tools.api.service;

import gov.nist.oscal.tools.api.entity.PasswordResetToken;
import gov.nist.oscal.tools.api.entity.User;
import gov.nist.oscal.tools.api.model.AuditEventType;
import gov.nist.oscal.tools.api.repository.PasswordResetTokenRepository;
import gov.nist.oscal.tools.api.repository.UserRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Self-serve forgot-password flow.
 *
 * Design constraints:
 * <ul>
 *   <li><b>No user enumeration</b> — requestReset never reveals whether the
 *       identifier matched an account; the API response is identical either way.</li>
 *   <li><b>Non-unique emails</b> — an email may match several accounts; each
 *       matching account gets its own reset email (which names the username),
 *       capped to avoid abuse.</li>
 *   <li><b>Hashed at rest</b> — only the SHA-256 of the token is stored.</li>
 *   <li><b>Single use</b> — consuming a token invalidates it and every other
 *       outstanding token for that user.</li>
 * </ul>
 */
@Service
public class PasswordResetService {

    private static final Logger logger = LoggerFactory.getLogger(PasswordResetService.class);

    /** Reset links are valid for one hour. */
    static final int TOKEN_TTL_MINUTES = 60;

    /** Max accounts a single request will send reset emails for (abuse cap). */
    private static final int MAX_ACCOUNTS_PER_REQUEST = 5;

    private static final SecureRandom RANDOM = new SecureRandom();

    @Autowired private UserRepository userRepository;
    @Autowired private PasswordResetTokenRepository tokenRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private PasswordValidationService passwordValidationService;
    @Autowired private org.springframework.context.ApplicationEventPublisher eventPublisher;
    @Autowired private AuditLogService auditLogService;

    @Value("${app.base-url:http://localhost:3000}")
    private String baseUrl;

    /**
     * Issue reset tokens for every enabled account matching the identifier
     * (username first, then email). Deliberately returns nothing: the caller
     * must respond identically whether or not anything matched.
     */
    @Transactional
    public void requestReset(String usernameOrEmail) {
        if (usernameOrEmail == null || usernameOrEmail.isBlank()) {
            return;
        }
        String identifier = usernameOrEmail.trim();

        // LinkedHashMap dedupes by id while preserving match order
        Map<Long, User> matches = new LinkedHashMap<>();
        userRepository.findByUsername(identifier).ifPresent(u -> matches.put(u.getId(), u));
        for (User u : userRepository.findAllByEmailIgnoreCase(identifier)) {
            matches.put(u.getId(), u);
        }

        int issued = 0;
        for (User user : matches.values()) {
            if (issued >= MAX_ACCOUNTS_PER_REQUEST) {
                logger.warn("Password reset request matched more than {} accounts; capping", MAX_ACCOUNTS_PER_REQUEST);
                break;
            }
            if (!Boolean.TRUE.equals(user.getEnabled())) {
                continue;
            }

            String rawToken = generateToken();
            PasswordResetToken token = new PasswordResetToken();
            token.setUser(user);
            token.setTokenHash(sha256Hex(rawToken));
            token.setExpiresAt(LocalDateTime.now().plusMinutes(TOKEN_TTL_MINUTES));
            tokenRepository.save(token);

            String resetUrl = baseUrl + "/reset-password?token=" + rawToken;
            // Sent after commit, async with retry (TransactionalEmailListener).
            // After-commit also guarantees the token row exists before the link
            // can arrive; failures are logged there, never surfaced (enumeration).
            eventPublisher.publishEvent(new gov.nist.oscal.tools.api.email.EmailEvents
                    .PasswordResetLinkEmail(user.getId(), resetUrl, TOKEN_TTL_MINUTES));

            auditLogService.logEvent(AuditEventType.SECURITY_PASSWORD_RESET_REQUEST,
                    user.getUsername(), user.getId(), "SUCCESS", "user:" + user.getId(),
                    "PASSWORD_RESET_REQUEST", null);
            issued++;
        }

        if (issued == 0) {
            logger.info("Password reset requested for unknown or disabled identifier");
        }
    }

    /**
     * Consume a reset token and set the new password.
     *
     * @throws IllegalArgumentException (→ 400) for an invalid/expired/used token
     *         or a password that fails complexity rules
     */
    @Transactional
    public void resetPassword(String rawToken, String newPassword) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new IllegalArgumentException("This password reset link is invalid or has expired.");
        }

        PasswordResetToken token = tokenRepository.findByTokenHash(sha256Hex(rawToken.trim()))
                .filter(PasswordResetToken::isUsable)
                .orElseThrow(() -> new IllegalArgumentException(
                        "This password reset link is invalid or has expired. Please request a new one."));

        User user = token.getUser();
        passwordValidationService.validatePassword(newPassword, user.getUsername());

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setPasswordChangedAt(LocalDateTime.now());
        user.setMustChangePassword(false);
        // A successful reset proves control of the email account — clear any
        // failed-login lockout so the user isn't stuck waiting it out.
        user.setFailedLoginAttempts(0);
        user.setAccountLockedUntil(null);
        userRepository.save(user);

        // Single use: this token and every other outstanding one for the user.
        tokenRepository.invalidateAllForUser(user.getId(), LocalDateTime.now());

        auditLogService.logEvent(AuditEventType.SECURITY_PASSWORD_RESET_COMPLETE,
                user.getUsername(), user.getId(), "SUCCESS", "user:" + user.getId(),
                "PASSWORD_RESET_COMPLETE", null);
        logger.info("Password reset completed for user {}", user.getUsername());
    }

    private static String generateToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    static String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                sb.append(Character.forDigit((b >> 4) & 0xF, 16));
                sb.append(Character.forDigit(b & 0xF, 16));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
}
