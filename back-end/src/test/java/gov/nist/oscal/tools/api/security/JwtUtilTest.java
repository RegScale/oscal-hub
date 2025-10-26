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
}
