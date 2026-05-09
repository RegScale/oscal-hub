package gov.nist.oscal.tools.api.service;

import org.junit.jupiter.api.Test;

import java.security.SecureRandom;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for EncryptionService — AES-256-GCM authenticated encryption
 * for at-rest TOTP secrets.
 *
 * Coverage focus:
 *  - Both key-encoding formats accepted (64-char hex, base64)
 *  - Invalid key shapes rejected at construction (fail-fast)
 *  - No-key fallback generates a working ephemeral key (with a logged warning)
 *  - Encrypt/decrypt round-trip preserves the original
 *  - Each encrypt() call produces a unique ciphertext (random IV)
 *  - Tampered ciphertext is rejected (GCM auth tag enforces integrity)
 *  - Null/empty inputs are pass-through (services rely on this for optional fields)
 */
class EncryptionServiceTest {

    // 32-byte test key (do not use in production — committed test key only)
    private static final byte[] TEST_KEY_BYTES = new byte[32];
    static {
        new SecureRandom(new byte[]{1, 2, 3}).nextBytes(TEST_KEY_BYTES);
    }
    private static final String TEST_KEY_HEX = bytesToHex(TEST_KEY_BYTES);
    private static final String TEST_KEY_B64 = Base64.getEncoder().encodeToString(TEST_KEY_BYTES);

    @Test
    void roundTrip_hexKey_preservesPlaintext() {
        EncryptionService svc = new EncryptionService(TEST_KEY_HEX);
        String original = "totp-secret-ABC123";

        String enc = svc.encrypt(original);
        String dec = svc.decrypt(enc);

        assertThat(dec).isEqualTo(original);
        assertThat(enc).isNotEqualTo(original); // sanity: actually encrypted
    }

    @Test
    void roundTrip_base64Key_preservesPlaintext() {
        EncryptionService svc = new EncryptionService(TEST_KEY_B64);
        String dec = svc.decrypt(svc.encrypt("hello"));
        assertThat(dec).isEqualTo("hello");
    }

    @Test
    void encrypt_eachCall_producesDifferentCiphertext() {
        // Random IV per call ensures equal plaintexts don't yield equal ciphertexts
        // — important for not leaking that two users share the same TOTP seed.
        EncryptionService svc = new EncryptionService(TEST_KEY_HEX);
        String c1 = svc.encrypt("same");
        String c2 = svc.encrypt("same");

        assertThat(c1).isNotEqualTo(c2);
        // …but both decrypt back to the same plaintext.
        assertThat(svc.decrypt(c1)).isEqualTo("same");
        assertThat(svc.decrypt(c2)).isEqualTo("same");
    }

    @Test
    void encrypt_nullInput_returnsNull() {
        // Pass-through is intentional — callers store optional encrypted columns
        // and don't want to write a null check at every site.
        EncryptionService svc = new EncryptionService(TEST_KEY_HEX);
        assertThat(svc.encrypt(null)).isNull();
        assertThat(svc.encrypt("")).isNull();
    }

    @Test
    void decrypt_nullInput_returnsNull() {
        EncryptionService svc = new EncryptionService(TEST_KEY_HEX);
        assertThat(svc.decrypt(null)).isNull();
        assertThat(svc.decrypt("")).isNull();
    }

    @Test
    void decrypt_tamperedCiphertext_throwsEncryptionException() {
        // GCM authenticates the ciphertext: any byte flip in the encrypted data
        // OR the auth tag must be detected and surfaced as a clear error.
        EncryptionService svc = new EncryptionService(TEST_KEY_HEX);
        String legit = svc.encrypt("payment-secret");

        // Flip a single byte in the middle of the ciphertext
        byte[] bytes = Base64.getDecoder().decode(legit);
        bytes[bytes.length / 2] ^= 0x01;
        String tampered = Base64.getEncoder().encodeToString(bytes);

        assertThatThrownBy(() -> svc.decrypt(tampered))
                .isInstanceOf(EncryptionService.EncryptionException.class)
                .hasMessageContaining("corrupted or tampered");
    }

    @Test
    void decrypt_garbageInput_throwsEncryptionException() {
        EncryptionService svc = new EncryptionService(TEST_KEY_HEX);
        assertThatThrownBy(() -> svc.decrypt("definitely-not-base64-or-aes-data!"))
                .isInstanceOf(EncryptionService.EncryptionException.class);
    }

    @Test
    void decrypt_withDifferentKey_fails() {
        // Two services with different keys must not be able to decrypt each
        // other's output — otherwise rotating the encryption key wouldn't
        // actually invalidate old data.
        EncryptionService a = new EncryptionService(TEST_KEY_HEX);

        byte[] otherKey = new byte[32];
        new SecureRandom(new byte[]{99}).nextBytes(otherKey);
        EncryptionService b = new EncryptionService(bytesToHex(otherKey));

        String enc = a.encrypt("alpha");
        assertThatThrownBy(() -> b.decrypt(enc))
                .isInstanceOf(EncryptionService.EncryptionException.class);
    }

    @Test
    void noKeyConfigured_fallsBackToRandomKey_andStillWorks() {
        // Dev-mode fallback: no ENCRYPTION_KEY env var. The service must not
        // crash on boot — it generates an ephemeral key (warn-logged) so the
        // application can come up. The ephemeral key only has to be
        // self-consistent within the JVM lifetime.
        EncryptionService svc = new EncryptionService(null);
        assertThat(svc.decrypt(svc.encrypt("data"))).isEqualTo("data");

        EncryptionService svc2 = new EncryptionService("");
        assertThat(svc2.decrypt(svc2.encrypt("data"))).isEqualTo("data");
    }

    @Test
    void invalidHexKey_isRejected_atConstruction() {
        // 64-char string but contains non-hex → falls through to base64 attempt,
        // which also fails OR yields a wrong-length key. Either way we must
        // see a fail-fast IllegalArgumentException so misconfigured deployments
        // don't silently start with random keys.
        String badHex = "ZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZ";
        assertThatThrownBy(() -> new EncryptionService(badHex))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void wrongSizeKey_isRejected_evenIfBase64() {
        // A 16-byte (128-bit) base64 key would weaken the cipher. Must be 32.
        byte[] tooShort = new byte[16];
        String b64 = Base64.getEncoder().encodeToString(tooShort);

        assertThatThrownBy(() -> new EncryptionService(b64))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("32 bytes");
    }

    @Test
    void unparseableKey_isRejected() {
        // Not valid base64 either.
        assertThatThrownBy(() -> new EncryptionService("!@#$%^&*()_+-not-base64-!@#$%^&*()"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid encryption key format");
    }

    @Test
    void nonAsciiPlaintext_roundTripsCorrectly() {
        // UTF-8 handling — TOTP secrets are ASCII but app might encrypt other fields.
        EncryptionService svc = new EncryptionService(TEST_KEY_HEX);
        String unicode = "🔐密码-Ω-passwôrd";
        assertThat(svc.decrypt(svc.encrypt(unicode))).isEqualTo(unicode);
    }

    // ---- helpers ----

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) sb.append(String.format("%02x", b & 0xff));
        return sb.toString();
    }
}
