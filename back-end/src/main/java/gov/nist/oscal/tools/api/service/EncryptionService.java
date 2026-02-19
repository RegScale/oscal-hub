package gov.nist.oscal.tools.api.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Service for encrypting and decrypting sensitive data.
 * <p>
 * Uses AES-256-GCM (Galois/Counter Mode) for authenticated encryption.
 * This provides both confidentiality and integrity protection.
 * </p>
 *
 * <h2>Usage</h2>
 * <ul>
 *   <li>Encrypt TOTP secrets before storing in database</li>
 *   <li>Decrypt TOTP secrets when verifying codes</li>
 * </ul>
 *
 * <h2>Security</h2>
 * <ul>
 *   <li>Key must be 32 bytes (256 bits) hex-encoded or base64-encoded</li>
 *   <li>Random 12-byte IV generated for each encryption</li>
 *   <li>IV prepended to ciphertext for storage</li>
 *   <li>GCM provides authentication tag to detect tampering</li>
 * </ul>
 */
@Service
public class EncryptionService {

    private static final Logger logger = LoggerFactory.getLogger(EncryptionService.class);

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;  // 96 bits recommended for GCM
    private static final int GCM_TAG_LENGTH = 128; // bits

    private final SecretKey secretKey;
    private final SecureRandom secureRandom;

    public EncryptionService(
            @Value("${encryption.key:#{null}}") String encryptionKey) {

        this.secureRandom = new SecureRandom();

        if (encryptionKey == null || encryptionKey.isEmpty()) {
            // Generate a random key for development/testing
            // In production, ENCRYPTION_KEY must be set
            logger.warn("ENCRYPTION_KEY not set - generating random key. " +
                    "MFA secrets will be lost on restart. Set ENCRYPTION_KEY in production!");
            byte[] keyBytes = new byte[32];
            secureRandom.nextBytes(keyBytes);
            this.secretKey = new SecretKeySpec(keyBytes, "AES");
        } else {
            this.secretKey = parseKey(encryptionKey);
            logger.info("Encryption service initialized with configured key");
        }
    }

    /**
     * Encrypt plaintext using AES-256-GCM.
     *
     * @param plaintext the data to encrypt
     * @return Base64-encoded ciphertext (IV + encrypted data + auth tag)
     * @throws EncryptionException if encryption fails
     */
    public String encrypt(String plaintext) {
        if (plaintext == null || plaintext.isEmpty()) {
            return null;
        }

        try {
            // Generate random IV
            byte[] iv = new byte[GCM_IV_LENGTH];
            secureRandom.nextBytes(iv);

            // Initialize cipher
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, parameterSpec);

            // Encrypt
            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

            // Combine IV + ciphertext
            byte[] combined = new byte[iv.length + ciphertext.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(ciphertext, 0, combined, iv.length, ciphertext.length);

            return Base64.getEncoder().encodeToString(combined);

        } catch (Exception e) {
            logger.error("Encryption failed: {}", e.getMessage());
            throw new EncryptionException("Failed to encrypt data", e);
        }
    }

    /**
     * Decrypt ciphertext using AES-256-GCM.
     *
     * @param ciphertext Base64-encoded ciphertext (IV + encrypted data + auth tag)
     * @return the decrypted plaintext
     * @throws EncryptionException if decryption fails or data has been tampered with
     */
    public String decrypt(String ciphertext) {
        if (ciphertext == null || ciphertext.isEmpty()) {
            return null;
        }

        try {
            // Decode from Base64
            byte[] combined = Base64.getDecoder().decode(ciphertext);

            // Extract IV and ciphertext
            byte[] iv = new byte[GCM_IV_LENGTH];
            byte[] encrypted = new byte[combined.length - GCM_IV_LENGTH];
            System.arraycopy(combined, 0, iv, 0, iv.length);
            System.arraycopy(combined, iv.length, encrypted, 0, encrypted.length);

            // Initialize cipher
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, parameterSpec);

            // Decrypt
            byte[] plaintext = cipher.doFinal(encrypted);

            return new String(plaintext, StandardCharsets.UTF_8);

        } catch (Exception e) {
            logger.error("Decryption failed: {}", e.getMessage());
            throw new EncryptionException("Failed to decrypt data - data may be corrupted or tampered", e);
        }
    }

    /**
     * Parse encryption key from hex or base64 encoded string.
     *
     * @param keyString hex-encoded (64 chars) or base64-encoded key
     * @return SecretKey for AES encryption
     */
    private SecretKey parseKey(String keyString) {
        byte[] keyBytes;

        // Try hex decoding first (64 hex chars = 32 bytes)
        if (keyString.length() == 64 && keyString.matches("[0-9a-fA-F]+")) {
            keyBytes = hexStringToByteArray(keyString);
        } else {
            // Try base64 decoding
            try {
                keyBytes = Base64.getDecoder().decode(keyString);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException(
                        "Invalid encryption key format. Must be 64 hex characters or base64-encoded 32 bytes.");
            }
        }

        if (keyBytes.length != 32) {
            throw new IllegalArgumentException(
                    "Encryption key must be 32 bytes (256 bits). Got " + keyBytes.length + " bytes.");
        }

        return new SecretKeySpec(keyBytes, "AES");
    }

    /**
     * Convert hex string to byte array.
     */
    private byte[] hexStringToByteArray(String hex) {
        int len = hex.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                    + Character.digit(hex.charAt(i + 1), 16));
        }
        return data;
    }

    /**
     * Exception thrown when encryption/decryption fails.
     */
    public static class EncryptionException extends RuntimeException {
        public EncryptionException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
