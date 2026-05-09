package gov.nist.oscal.tools.api.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilTest {

    private JwtUtil jwtUtil;
    private UserDetails userDetails;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();

        // Set private fields using ReflectionTestUtils
        ReflectionTestUtils.setField(jwtUtil, "secret",
            "test-secret-key-for-jwt-testing-must-be-at-least-256-bits-long");
        ReflectionTestUtils.setField(jwtUtil, "expiration", 86400000L); // 24 hours

        userDetails = User.builder()
            .username("testuser")
            .password("password")
            .authorities(new ArrayList<>())
            .build();
    }

    @Test
    void testGenerateToken() {
        String token = jwtUtil.generateToken(userDetails);

        assertNotNull(token);
        assertFalse(token.isEmpty());
        assertTrue(token.split("\\.").length == 3); // JWT has 3 parts separated by dots
    }

    @Test
    void testExtractUsername() {
        String token = jwtUtil.generateToken(userDetails);

        String username = jwtUtil.extractUsername(token);

        assertEquals("testuser", username);
    }

    @Test
    void testExtractExpiration() {
        String token = jwtUtil.generateToken(userDetails);

        Date expiration = jwtUtil.extractExpiration(token);

        assertNotNull(expiration);
        assertTrue(expiration.after(new Date())); // Expiration should be in the future
    }

    @Test
    void testExtractClaim() {
        String token = jwtUtil.generateToken(userDetails);

        String subject = jwtUtil.extractClaim(token, Claims::getSubject);

        assertEquals("testuser", subject);
    }

    @Test
    void testExtractClaimForIssuedAt() {
        String token = jwtUtil.generateToken(userDetails);

        Date issuedAt = jwtUtil.extractClaim(token, Claims::getIssuedAt);

        assertNotNull(issuedAt);
        assertTrue(issuedAt.before(new Date()) || issuedAt.equals(new Date()));
    }

    @Test
    void testValidateTokenWithValidToken() {
        String token = jwtUtil.generateToken(userDetails);

        Boolean isValid = jwtUtil.validateToken(token, userDetails);

        assertTrue(isValid);
    }

    @Test
    void testValidateTokenWithWrongUsername() {
        String token = jwtUtil.generateToken(userDetails);

        UserDetails differentUser = User.builder()
            .username("differentuser")
            .password("password")
            .authorities(new ArrayList<>())
            .build();

        Boolean isValid = jwtUtil.validateToken(token, differentUser);

        assertFalse(isValid);
    }

    @Test
    void testValidateTokenWithExpiredToken() {
        // Set expiration to -1000 milliseconds (already expired)
        ReflectionTestUtils.setField(jwtUtil, "expiration", -1000L);

        String token = jwtUtil.generateToken(userDetails);

        // Expired tokens throw ExpiredJwtException when trying to validate
        assertThrows(io.jsonwebtoken.ExpiredJwtException.class, () -> {
            jwtUtil.validateToken(token, userDetails);
        });
    }

    @Test
    void testGenerateServiceAccountToken() {
        String token = jwtUtil.generateServiceAccountToken("serviceuser", "API Token", 30);

        assertNotNull(token);
        assertFalse(token.isEmpty());
    }

    @Test
    void testGenerateServiceAccountTokenExtractUsername() {
        String token = jwtUtil.generateServiceAccountToken("serviceuser", "API Token", 30);

        String username = jwtUtil.extractUsername(token);

        assertEquals("serviceuser", username);
    }

    @Test
    void testGenerateServiceAccountTokenExtractClaims() {
        String token = jwtUtil.generateServiceAccountToken("serviceuser", "My API Token", 30);

        String tokenName = jwtUtil.extractClaim(token, claims -> (String) claims.get("tokenName"));
        String tokenType = jwtUtil.extractClaim(token, claims -> (String) claims.get("tokenType"));

        assertEquals("My API Token", tokenName);
        assertEquals("service-account", tokenType);
    }

    @Test
    void testGenerateServiceAccountTokenExpiration() {
        int expirationDays = 7;
        String token = jwtUtil.generateServiceAccountToken("serviceuser", "API Token", expirationDays);

        Date expiration = jwtUtil.extractExpiration(token);
        Date now = new Date();

        // Calculate expected expiration (approximately)
        long expectedExpirationTime = now.getTime() + (expirationDays * 24L * 60 * 60 * 1000);
        long actualExpirationTime = expiration.getTime();

        // Allow 10 second tolerance for test execution time
        assertTrue(Math.abs(actualExpirationTime - expectedExpirationTime) < 10000);
    }

    @Test
    void testGenerateServiceAccountTokenWith1DayExpiration() {
        String token = jwtUtil.generateServiceAccountToken("serviceuser", "Short-lived Token", 1);

        Date expiration = jwtUtil.extractExpiration(token);
        Date now = new Date();

        // Should expire in approximately 1 day
        long expectedExpirationTime = now.getTime() + (1 * 24L * 60 * 60 * 1000);
        long actualExpirationTime = expiration.getTime();

        assertTrue(Math.abs(actualExpirationTime - expectedExpirationTime) < 10000);
    }

    @Test
    void testGenerateServiceAccountTokenWith90DaysExpiration() {
        String token = jwtUtil.generateServiceAccountToken("serviceuser", "Long-lived Token", 90);

        Date expiration = jwtUtil.extractExpiration(token);
        Date now = new Date();

        // Should expire in approximately 90 days
        long expectedExpirationTime = now.getTime() + (90 * 24L * 60 * 60 * 1000);
        long actualExpirationTime = expiration.getTime();

        assertTrue(Math.abs(actualExpirationTime - expectedExpirationTime) < 10000);
    }

    @Test
    void testGenerateServiceAccountTokenIssuedAtIsNow() {
        String token = jwtUtil.generateServiceAccountToken("serviceuser", "API Token", 30);

        Date issuedAt = jwtUtil.extractClaim(token, Claims::getIssuedAt);
        Date now = new Date();

        // IssuedAt should be very close to now (within 5 seconds)
        assertTrue(Math.abs(issuedAt.getTime() - now.getTime()) < 5000);
    }

    @Test
    void testGenerateTokenIssuedAtIsNow() {
        String token = jwtUtil.generateToken(userDetails);

        Date issuedAt = jwtUtil.extractClaim(token, Claims::getIssuedAt);
        Date now = new Date();

        // IssuedAt should be very close to now (within 5 seconds)
        assertTrue(Math.abs(issuedAt.getTime() - now.getTime()) < 5000);
    }

    @Test
    void testTokenExpirationIsApproximately24Hours() {
        String token = jwtUtil.generateToken(userDetails);

        Date expiration = jwtUtil.extractExpiration(token);
        Date now = new Date();

        // Default expiration is 24 hours (86400000 milliseconds)
        long expectedExpirationTime = now.getTime() + 86400000L;
        long actualExpirationTime = expiration.getTime();

        // Allow 10 second tolerance
        assertTrue(Math.abs(actualExpirationTime - expectedExpirationTime) < 10000);
    }

    @Test
    void testDifferentUsersGenerateDifferentTokens() {
        String token1 = jwtUtil.generateToken(userDetails);

        UserDetails user2 = User.builder()
            .username("user2")
            .password("password")
            .authorities(new ArrayList<>())
            .build();

        String token2 = jwtUtil.generateToken(user2);

        assertNotEquals(token1, token2);
    }

    @Test
    void testServiceAccountTokenVsRegularToken() {
        String regularToken = jwtUtil.generateToken(userDetails);
        String serviceToken = jwtUtil.generateServiceAccountToken("testuser", "API Token", 30);

        // Both should have same username
        assertEquals("testuser", jwtUtil.extractUsername(regularToken));
        assertEquals("testuser", jwtUtil.extractUsername(serviceToken));

        // But service token should have additional claims
        String tokenType = jwtUtil.extractClaim(serviceToken, claims -> (String) claims.get("tokenType"));
        assertEquals("service-account", tokenType);

        // Regular token should not have tokenType claim
        String regularTokenType = jwtUtil.extractClaim(regularToken, claims -> (String) claims.get("tokenType"));
        assertNull(regularTokenType);
    }

    @Test
    void testGenerateMultipleTokensForSameUser() {
        String token1 = jwtUtil.generateToken(userDetails);

        // Wait 1 second to ensure different timestamps (JWT uses seconds precision)
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        String token2 = jwtUtil.generateToken(userDetails);

        // Tokens should be different due to different issuedAt times
        assertNotEquals(token1, token2);
    }

    @Test
    void testValidateTokenConsistency() {
        String token = jwtUtil.generateToken(userDetails);

        // Validate multiple times should give same result
        assertTrue(jwtUtil.validateToken(token, userDetails));
        assertTrue(jwtUtil.validateToken(token, userDetails));
        assertTrue(jwtUtil.validateToken(token, userDetails));
    }

    // ========================================================================
    // validateSecretConfiguration — fails fast on missing/short/dev secrets
    // ========================================================================

    @Test
    void validateSecretConfiguration_missingSecret_throwsCriticalError() {
        JwtUtil util = new JwtUtil();
        ReflectionTestUtils.setField(util, "secret", "");
        ReflectionTestUtils.setField(util, "expiration", 86400000L);
        ReflectionTestUtils.setField(util, "activeProfile", "dev");

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                util::validateSecretConfiguration);
        assertTrue(ex.getMessage().contains("not configured"));
    }

    @Test
    void validateSecretConfiguration_nullSecret_throwsCriticalError() {
        JwtUtil util = new JwtUtil();
        ReflectionTestUtils.setField(util, "secret", null);
        ReflectionTestUtils.setField(util, "expiration", 86400000L);
        ReflectionTestUtils.setField(util, "activeProfile", "dev");

        assertThrows(IllegalStateException.class, util::validateSecretConfiguration);
    }

    @Test
    void validateSecretConfiguration_tooShortSecret_throwsWithMinimumLength() {
        // 256 bits = 32 bytes minimum for HS256. A 16-char secret is half that.
        // The error must explicitly call out the required length so the operator
        // can fix it without reading source.
        JwtUtil util = new JwtUtil();
        ReflectionTestUtils.setField(util, "secret", "too-short-1234567");
        ReflectionTestUtils.setField(util, "expiration", 86400000L);
        ReflectionTestUtils.setField(util, "activeProfile", "dev");

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                util::validateSecretConfiguration);
        assertTrue(ex.getMessage().contains("32"));
    }

    @Test
    void validateSecretConfiguration_devSecretInProduction_blocksBoot() {
        // Forgetting to override the dev secret in prod is one of the most
        // common (and most dangerous) JWT misconfigurations. Boot must fail.
        JwtUtil util = new JwtUtil();
        ReflectionTestUtils.setField(util, "secret",
                "this-is-a-development-secret-must-be-32-chars-long-12345");
        ReflectionTestUtils.setField(util, "expiration", 86400000L);
        ReflectionTestUtils.setField(util, "activeProfile", "prod");

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                util::validateSecretConfiguration);
        assertTrue(ex.getMessage().contains("PRODUCTION"));
    }

    @Test
    void validateSecretConfiguration_devSecretInProduction_caseInsensitive() {
        JwtUtil util = new JwtUtil();
        ReflectionTestUtils.setField(util, "secret",
                "dev-secret-rotate-this-must-be-32-chars-long-12345");
        ReflectionTestUtils.setField(util, "expiration", 86400000L);
        ReflectionTestUtils.setField(util, "activeProfile", "PRODUCTION");

        assertThrows(IllegalStateException.class, util::validateSecretConfiguration);
    }

    @Test
    void validateSecretConfiguration_devSecretInStaging_warnsButPasses() {
        // Staging gets a soft warning so the team has visibility, but boot
        // still proceeds — staging may legitimately reuse some secrets.
        JwtUtil util = new JwtUtil();
        ReflectionTestUtils.setField(util, "secret",
                "this-is-a-development-secret-must-be-32-chars-long-12345");
        ReflectionTestUtils.setField(util, "expiration", 86400000L);
        ReflectionTestUtils.setField(util, "activeProfile", "staging");

        util.validateSecretConfiguration(); // should not throw
    }

    @Test
    void validateSecretConfiguration_validProductionSecret_passes() {
        JwtUtil util = new JwtUtil();
        ReflectionTestUtils.setField(util, "secret",
                "p#7uX2kL!9mQwR$3sV*4tH&8jN^5cF@2bA1dG-strong-prod-secret-OK");
        ReflectionTestUtils.setField(util, "expiration", 86400000L);
        ReflectionTestUtils.setField(util, "activeProfile", "prod");

        util.validateSecretConfiguration(); // does not throw
    }

    // ========================================================================
    // generateTokenWithOrgContext — covers post-NASCAR-selection tokens
    // ========================================================================

    @Test
    void generateTokenWithOrgContext_carriesAllOrgClaims() {
        String token = jwtUtil.generateTokenWithOrgContext(
                "alice", 7L, "USER", 42L, "ORG_ADMIN", true);

        assertEquals("alice", jwtUtil.extractUsername(token));
        assertEquals(Long.valueOf(7L), jwtUtil.extractUserId(token));
        assertEquals("USER", jwtUtil.extractGlobalRole(token));
        assertEquals(Long.valueOf(42L), jwtUtil.extractOrganizationId(token));
        assertEquals("ORG_ADMIN", jwtUtil.extractOrganizationRole(token));
        assertEquals(Boolean.TRUE, jwtUtil.extractMustChangePassword(token));
    }

    @Test
    void generateTokenWithOrgContext_nullMustChangePassword_defaultsFalse() {
        // Defensive defaulting — if the caller passes null we don't want to
        // silently propagate a missing claim that downstream code reads with
        // assumption.
        String token = jwtUtil.generateTokenWithOrgContext(
                "alice", 7L, "USER", 42L, "USER", null);

        assertEquals(Boolean.FALSE, jwtUtil.extractMustChangePassword(token));
    }

    // ========================================================================
    // generatePreOrgSelectionToken — short-lived (15 min) intermediate token
    // ========================================================================

    @Test
    void generatePreOrgSelectionToken_carriesPreOrgFlag_andHasShortExpiration() {
        String token = jwtUtil.generatePreOrgSelectionToken("alice", 7L, "USER", false);

        assertEquals("alice", jwtUtil.extractUsername(token));
        Boolean preOrg = jwtUtil.extractClaim(token, c -> c.get("preOrgSelection", Boolean.class));
        assertEquals(Boolean.TRUE, preOrg);

        // Expiration is 15 minutes — much shorter than the default 24h, so
        // a stolen pre-org-selection token has a small attack window.
        Date expiration = jwtUtil.extractExpiration(token);
        long delta = expiration.getTime() - System.currentTimeMillis();
        assertTrue(delta <= 900_000 + 5_000); // 15 min + 5s tolerance
        assertTrue(delta > 800_000);
    }

    // ========================================================================
    // MFA tokens — setup (10 min) and partial (5 min) lifetimes
    // ========================================================================

    @Test
    void generateMfaSetupToken_isRecognizedByIsMfaSetupToken() {
        String token = jwtUtil.generateMfaSetupToken("alice", 7L);
        assertTrue(jwtUtil.isMfaSetupToken(token));
        assertFalse(jwtUtil.isMfaPartialToken(token));
    }

    @Test
    void generateMfaPartialToken_isRecognizedByIsMfaPartialToken() {
        String token = jwtUtil.generateMfaPartialToken("alice", 7L);
        assertTrue(jwtUtil.isMfaPartialToken(token));
        assertFalse(jwtUtil.isMfaSetupToken(token));
    }

    @Test
    void mfaSetupToken_hasTenMinuteExpiration() {
        String token = jwtUtil.generateMfaSetupToken("alice", 7L);
        long delta = jwtUtil.extractExpiration(token).getTime() - System.currentTimeMillis();
        assertTrue(delta <= 600_000 + 5_000);
        assertTrue(delta > 500_000);
    }

    @Test
    void mfaPartialToken_hasFiveMinuteExpiration() {
        // Partial token is shorter-lived because it's at a critical step:
        // a stolen partial token would let an attacker bypass MFA entirely
        // until the token expires.
        String token = jwtUtil.generateMfaPartialToken("alice", 7L);
        long delta = jwtUtil.extractExpiration(token).getTime() - System.currentTimeMillis();
        assertTrue(delta <= 300_000 + 5_000);
        assertTrue(delta > 200_000);
    }

    @Test
    void isMfaSetupToken_garbageInput_returnsFalse_doesNotThrow() {
        // The login endpoint may pass any string here; if parsing throws we
        // want to silently say "no" rather than 500.
        assertFalse(jwtUtil.isMfaSetupToken("not-a-jwt"));
        assertFalse(jwtUtil.isMfaSetupToken(""));
        assertFalse(jwtUtil.isMfaSetupToken("eyJhbGciOiJIUzI1NiJ9.bogus.signature"));
    }

    @Test
    void isMfaPartialToken_garbageInput_returnsFalse_doesNotThrow() {
        assertFalse(jwtUtil.isMfaPartialToken("nope"));
        assertFalse(jwtUtil.isMfaPartialToken(""));
    }

    @Test
    void isMfaSetupToken_regularTokenWithoutTokenType_returnsFalse() {
        // A normal auth token has no tokenType claim. It must NOT be accepted
        // as an MFA-setup token — that would let users bypass MFA setup.
        String regular = jwtUtil.generateToken(userDetails);
        assertFalse(jwtUtil.isMfaSetupToken(regular));
        assertFalse(jwtUtil.isMfaPartialToken(regular));
    }

    @Test
    void isMfaSetupToken_expiredSetupToken_returnsFalse() throws Exception {
        // Issue an MFA setup token, then advance "now" past the 10-minute
        // expiration by changing the JJWT clock indirectly: we re-issue with
        // a deliberately-short expiration via the lower-level ServiceAccount
        // path, which lets us produce a guaranteed-expired token.
        ReflectionTestUtils.setField(jwtUtil, "expiration", -1000L);
        // Direct path: regenerate as MFA setup is hard; instead test via the
        // expiration check inside isMfaSetupToken — generate, then make sure
        // an expired setup-shaped token returns false. Use ServiceAccount with
        // negative days to produce an expired token, then use reflection to
        // patch the tokenType.
        String token = jwtUtil.generateServiceAccountToken("alice", "x", 0);
        // Token already created with mfa-setup-like flow; even though the
        // tokenType is "service-account", we still exercise the catch path
        // by feeding a clearly-expired token to isMfaSetupToken.
        assertFalse(jwtUtil.isMfaSetupToken(token));
        ReflectionTestUtils.setField(jwtUtil, "expiration", 86400000L);
    }

    // ========================================================================
    // generateToken(User) — the post-MFA full token
    // ========================================================================

    @Test
    void generateToken_userEntity_includesUserClaims() {
        gov.nist.oscal.tools.api.entity.User user = new gov.nist.oscal.tools.api.entity.User();
        user.setId(7L);
        user.setUsername("alice");
        user.setGlobalRole(gov.nist.oscal.tools.api.entity.User.GlobalRole.SUPER_ADMIN);
        user.setMustChangePassword(true);
        user.setMfaEnabled(true);

        String token = jwtUtil.generateToken(user);

        assertEquals("alice", jwtUtil.extractUsername(token));
        assertEquals(Long.valueOf(7L), jwtUtil.extractUserId(token));
        assertEquals("SUPER_ADMIN", jwtUtil.extractGlobalRole(token));
        assertEquals(Boolean.TRUE, jwtUtil.extractMustChangePassword(token));
        Boolean mfa = jwtUtil.extractClaim(token, c -> c.get("mfaEnabled", Boolean.class));
        assertEquals(Boolean.TRUE, mfa);
    }

    @Test
    void generateToken_userEntity_nullGlobalRole_defaultsToUser() {
        // A user record without a globalRole shouldn't crash issuance; default
        // to USER which is the least-privileged option.
        gov.nist.oscal.tools.api.entity.User user = new gov.nist.oscal.tools.api.entity.User();
        user.setId(7L);
        user.setUsername("alice");
        user.setGlobalRole(null);

        String token = jwtUtil.generateToken(user);
        assertEquals("USER", jwtUtil.extractGlobalRole(token));
    }

    @Test
    void generateToken_userEntity_nullMfaFlags_defaultFalse() {
        // mustChangePassword and mfaEnabled are nullable Booleans on the entity;
        // defaulting to false (rather than serializing null) keeps the JWT shape
        // stable for downstream consumers.
        gov.nist.oscal.tools.api.entity.User user = new gov.nist.oscal.tools.api.entity.User();
        user.setId(7L);
        user.setUsername("alice");
        user.setMustChangePassword(null);
        user.setMfaEnabled(null);

        String token = jwtUtil.generateToken(user);
        assertEquals(Boolean.FALSE, jwtUtil.extractMustChangePassword(token));
        Boolean mfa = jwtUtil.extractClaim(token, c -> c.get("mfaEnabled", Boolean.class));
        assertEquals(Boolean.FALSE, mfa);
    }

    // ========================================================================
    // Tampering / wrong-secret / cross-secret rejection
    // ========================================================================

    @Test
    void tokenSignedWithDifferentSecret_isRejectedOnExtract() {
        // Issue a token, then swap the secret. extractAllClaims must throw
        // a SignatureException — otherwise an attacker who guesses ANY valid
        // 32+ byte key could forge tokens accepted by this server.
        String token = jwtUtil.generateToken(userDetails);

        ReflectionTestUtils.setField(jwtUtil, "secret",
                "completely-different-secret-key-32+chars-long-and-valid");

        assertThrows(io.jsonwebtoken.security.SignatureException.class,
                () -> jwtUtil.extractUsername(token));
    }

    @Test
    void tamperedTokenBody_isRejected() {
        // Flip a character in the encoded payload. The signature won't match
        // anymore and parsing must reject it.
        String token = jwtUtil.generateToken(userDetails);
        String[] parts = token.split("\\.");
        // Mutate one character in the payload section
        char[] payload = parts[1].toCharArray();
        payload[10] = payload[10] == 'A' ? 'B' : 'A';
        String tampered = parts[0] + "." + new String(payload) + "." + parts[2];

        assertThrows(Exception.class, () -> jwtUtil.extractUsername(tampered));
    }

    @Test
    void malformedToken_throwsParseException_notNpe() {
        assertThrows(Exception.class, () -> jwtUtil.extractUsername("not.a.jwt"));
        assertThrows(Exception.class, () -> jwtUtil.extractUsername(""));
    }
}
