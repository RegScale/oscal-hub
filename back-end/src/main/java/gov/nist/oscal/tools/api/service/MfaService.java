package gov.nist.oscal.tools.api.service;

import dev.samstevens.totp.code.*;
import dev.samstevens.totp.exceptions.QrGenerationException;
import dev.samstevens.totp.qr.QrData;
import dev.samstevens.totp.qr.QrGenerator;
import dev.samstevens.totp.qr.ZxingPngQrGenerator;
import dev.samstevens.totp.secret.DefaultSecretGenerator;
import dev.samstevens.totp.secret.SecretGenerator;
import dev.samstevens.totp.time.SystemTimeProvider;
import dev.samstevens.totp.time.TimeProvider;
import gov.nist.oscal.tools.api.entity.MfaBackupCode;
import gov.nist.oscal.tools.api.entity.User;
import gov.nist.oscal.tools.api.repository.MfaBackupCodeRepository;
import gov.nist.oscal.tools.api.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

import static dev.samstevens.totp.util.Utils.getDataUriForImage;

/**
 * Service for Multi-Factor Authentication (MFA) operations.
 * <p>
 * Implements TOTP (Time-based One-Time Password) authentication compatible with:
 * - Google Authenticator
 * - Microsoft Authenticator
 * - Authy
 * - Any RFC 6238 compliant authenticator app
 * </p>
 *
 * <h2>Features</h2>
 * <ul>
 *   <li>Generate TOTP secrets and QR codes</li>
 *   <li>Verify TOTP codes</li>
 *   <li>Generate and verify backup codes</li>
 *   <li>Encrypt secrets at rest</li>
 * </ul>
 */
@Service
public class MfaService {

    private static final Logger logger = LoggerFactory.getLogger(MfaService.class);

    private static final int BACKUP_CODE_COUNT = 10;
    private static final int BACKUP_CODE_LENGTH = 8;  // 8 digits like 1234-5678

    private final UserRepository userRepository;
    private final MfaBackupCodeRepository backupCodeRepository;
    private final EncryptionService encryptionService;
    private final SecretGenerator secretGenerator;
    private final QrGenerator qrGenerator;
    private final CodeVerifier codeVerifier;
    private final SecureRandom secureRandom;

    @Value("${app.display-name:OSCAL Hub}")
    private String applicationName;

    public MfaService(
            UserRepository userRepository,
            MfaBackupCodeRepository backupCodeRepository,
            EncryptionService encryptionService) {
        this.userRepository = userRepository;
        this.backupCodeRepository = backupCodeRepository;
        this.encryptionService = encryptionService;

        // Initialize TOTP components
        this.secretGenerator = new DefaultSecretGenerator();
        this.qrGenerator = new ZxingPngQrGenerator();

        // Create code verifier with some tolerance for clock skew
        TimeProvider timeProvider = new SystemTimeProvider();
        CodeGenerator codeGenerator = new DefaultCodeGenerator();
        this.codeVerifier = new DefaultCodeVerifier(codeGenerator, timeProvider);
        // Allow 1 period before/after current (30 second window each direction)
        ((DefaultCodeVerifier) this.codeVerifier).setAllowedTimePeriodDiscrepancy(1);

        this.secureRandom = new SecureRandom();
    }

    /**
     * Response object for MFA setup initiation.
     */
    public record MfaSetupData(
            String secret,      // Raw secret for manual entry
            String qrCodeDataUri, // QR code as data URI
            String formattedSecret // Secret formatted for display (groups of 4)
    ) {}

    /**
     * Initiate MFA setup for a user.
     * Generates a new TOTP secret and QR code.
     * The secret is stored encrypted in the user's record but MFA is not enabled yet.
     *
     * @param userId the user ID
     * @return MFA setup data containing secret and QR code
     */
    @Transactional
    public MfaSetupData initiateMfaSetup(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));

        // Generate new secret
        String secret = secretGenerator.generate();

        // Store encrypted secret (but don't enable MFA yet)
        user.setMfaSecret(encryptionService.encrypt(secret));
        user.setMfaSetupCompleted(false);
        userRepository.save(user);

        // Generate QR code
        String qrCodeDataUri = generateQrCodeDataUri(user.getUsername(), secret);

        // Format secret for manual entry (groups of 4)
        String formattedSecret = formatSecret(secret);

        logger.info("MFA setup initiated for user: {}", user.getUsername());

        return new MfaSetupData(secret, qrCodeDataUri, formattedSecret);
    }

    /**
     * Complete MFA setup by verifying the first TOTP code.
     * Generates backup codes upon successful verification.
     *
     * @param userId   the user ID
     * @param totpCode the TOTP code from authenticator app
     * @return list of backup codes
     * @throws InvalidMfaCodeException if the code is invalid
     */
    @Transactional
    public List<String> completeMfaSetup(Long userId, String totpCode) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));

        if (user.getMfaSecret() == null) {
            throw new RuntimeException("MFA setup not initiated. Call initiateMfaSetup first.");
        }

        // Decrypt and verify the code
        logger.debug("Decrypting MFA secret for user: {}", user.getUsername());
        String secret = encryptionService.decrypt(user.getMfaSecret());
        logger.debug("Decrypted secret length: {}, verifying code: {}",
                secret != null ? secret.length() : "null", totpCode);
        if (!verifyCode(secret, totpCode)) {
            logger.warn("TOTP code verification failed for user: {}", user.getUsername());
            throw new InvalidMfaCodeException("Invalid TOTP code");
        }

        // Enable MFA
        user.setMfaEnabled(true);
        user.setMfaSetupCompleted(true);
        userRepository.save(user);

        // Generate backup codes
        List<String> backupCodes = generateBackupCodes(userId);

        logger.info("MFA setup completed for user: {}", user.getUsername());

        return backupCodes;
    }

    /**
     * Verify a TOTP code during login.
     *
     * @param userId   the user ID
     * @param totpCode the TOTP code from authenticator app
     * @return true if valid
     */
    public boolean verifyTotpCode(Long userId, String totpCode) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));

        if (!user.getMfaEnabled() || user.getMfaSecret() == null) {
            throw new RuntimeException("MFA is not enabled for this user");
        }

        String secret = encryptionService.decrypt(user.getMfaSecret());
        boolean valid = verifyCode(secret, totpCode);

        if (valid) {
            logger.debug("MFA verification successful for user: {}", user.getUsername());
        } else {
            logger.warn("MFA verification failed for user: {}", user.getUsername());
        }

        return valid;
    }

    /**
     * Verify and consume a backup code.
     *
     * @param userId     the user ID
     * @param backupCode the backup code (format: XXXX-XXXX or XXXXXXXX)
     * @return true if valid and consumed
     */
    @Transactional
    public boolean verifyBackupCode(Long userId, String backupCode) {
        // Normalize code (remove dashes, uppercase)
        String normalizedCode = backupCode.replace("-", "").toUpperCase();
        String codeHash = hashBackupCode(normalizedCode);

        return backupCodeRepository.findByUserIdAndCodeHashAndUsedFalse(userId, codeHash)
                .map(code -> {
                    code.markAsUsed();
                    backupCodeRepository.save(code);
                    logger.info("Backup code used for user ID: {}", userId);
                    return true;
                })
                .orElse(false);
    }

    /**
     * Generate new backup codes (deletes existing codes).
     *
     * @param userId the user ID
     * @return list of new backup codes (plaintext, for display to user)
     */
    @Transactional
    public List<String> generateBackupCodes(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));

        // Delete existing codes
        backupCodeRepository.deleteByUserId(userId);

        // Generate new codes
        List<String> codes = new ArrayList<>();
        for (int i = 0; i < BACKUP_CODE_COUNT; i++) {
            String code = generateRandomCode();
            String formattedCode = formatBackupCode(code);
            codes.add(formattedCode);

            // Store hash
            MfaBackupCode backupCode = new MfaBackupCode(user, hashBackupCode(code));
            backupCodeRepository.save(backupCode);
        }

        logger.info("Generated {} backup codes for user: {}", BACKUP_CODE_COUNT, user.getUsername());

        return codes;
    }

    /**
     * Get count of remaining unused backup codes.
     *
     * @param userId the user ID
     * @return count of unused backup codes
     */
    public int getRemainingBackupCodesCount(Long userId) {
        return backupCodeRepository.countByUserIdAndUsedFalse(userId);
    }

    /**
     * Disable MFA for a user (removes secret and backup codes).
     *
     * @param userId the user ID
     */
    @Transactional
    public void disableMfa(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));

        user.setMfaEnabled(false);
        user.setMfaSecret(null);
        user.setMfaSetupCompleted(false);
        userRepository.save(user);

        // Delete backup codes
        backupCodeRepository.deleteByUserId(userId);

        logger.info("MFA disabled for user: {}", user.getUsername());
    }

    /**
     * Check if MFA is enabled for a user.
     *
     * @param userId the user ID
     * @return true if MFA is enabled
     */
    public boolean isMfaEnabled(Long userId) {
        return userRepository.findById(userId)
                .map(User::getMfaEnabled)
                .orElse(false);
    }

    /**
     * Check if user has completed MFA setup.
     *
     * @param userId the user ID
     * @return true if MFA setup is completed
     */
    public boolean isMfaSetupCompleted(Long userId) {
        return userRepository.findById(userId)
                .map(User::getMfaSetupCompleted)
                .orElse(false);
    }

    // ========================================
    // Private Helper Methods
    // ========================================

    private boolean verifyCode(String secret, String code) {
        logger.debug("Verifying TOTP code. Secret length: {}, Code: {}",
                secret != null ? secret.length() : "null", code);
        boolean valid = codeVerifier.isValidCode(secret, code);
        logger.debug("TOTP verification result: {}", valid);
        return valid;
    }

    private String generateQrCodeDataUri(String username, String secret) {
        QrData data = new QrData.Builder()
                .label(username)
                .secret(secret)
                .issuer(applicationName)
                .algorithm(HashingAlgorithm.SHA1)
                .digits(6)
                .period(30)
                .build();

        try {
            byte[] imageData = qrGenerator.generate(data);
            return getDataUriForImage(imageData, qrGenerator.getImageMimeType());
        } catch (QrGenerationException e) {
            logger.error("Failed to generate QR code: {}", e.getMessage());
            throw new RuntimeException("Failed to generate QR code", e);
        }
    }

    private String formatSecret(String secret) {
        // Format as groups of 4 characters: XXXX XXXX XXXX XXXX
        StringBuilder formatted = new StringBuilder();
        for (int i = 0; i < secret.length(); i += 4) {
            if (formatted.length() > 0) {
                formatted.append(" ");
            }
            formatted.append(secret.substring(i, Math.min(i + 4, secret.length())));
        }
        return formatted.toString();
    }

    private String generateRandomCode() {
        StringBuilder code = new StringBuilder();
        for (int i = 0; i < BACKUP_CODE_LENGTH; i++) {
            code.append(secureRandom.nextInt(10));
        }
        return code.toString();
    }

    private String formatBackupCode(String code) {
        // Format as XXXX-XXXX
        if (code.length() == 8) {
            return code.substring(0, 4) + "-" + code.substring(4);
        }
        return code;
    }

    private String hashBackupCode(String code) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(code.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

    /**
     * Exception thrown when an invalid MFA code is provided.
     */
    public static class InvalidMfaCodeException extends RuntimeException {
        public InvalidMfaCodeException(String message) {
            super(message);
        }
    }
}
