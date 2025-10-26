package gov.nist.oscal.tools.api.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ServiceAccountTokenResponseTest {

    @Test
    void testNoArgsConstructor() {
        ServiceAccountTokenResponse response = new ServiceAccountTokenResponse();

        assertNotNull(response);
        assertNull(response.getToken());
        assertNull(response.getTokenName());
        assertNull(response.getUsername());
        assertNull(response.getExpiresAt());
        assertNull(response.getExpirationDays());
    }

    @Test
    void testFiveArgsConstructor() {
        String token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9";
        String tokenName = "production-api-token";
        String username = "service-account";
        String expiresAt = "2025-12-31T23:59:59";
        Integer expirationDays = 365;

        ServiceAccountTokenResponse response = new ServiceAccountTokenResponse(
                token, tokenName, username, expiresAt, expirationDays
        );

        assertNotNull(response);
        assertEquals(token, response.getToken());
        assertEquals(tokenName, response.getTokenName());
        assertEquals(username, response.getUsername());
        assertEquals(expiresAt, response.getExpiresAt());
        assertEquals(expirationDays, response.getExpirationDays());
    }

    @Test
    void testSetToken() {
        ServiceAccountTokenResponse response = new ServiceAccountTokenResponse();
        assertNull(response.getToken());

        String token = "new-token-value";
        response.setToken(token);
        assertEquals(token, response.getToken());
    }

    @Test
    void testSetTokenName() {
        ServiceAccountTokenResponse response = new ServiceAccountTokenResponse();
        assertNull(response.getTokenName());

        String tokenName = "deployment-token";
        response.setTokenName(tokenName);
        assertEquals(tokenName, response.getTokenName());
    }

    @Test
    void testSetUsername() {
        ServiceAccountTokenResponse response = new ServiceAccountTokenResponse();
        assertNull(response.getUsername());

        String username = "ci-cd-user";
        response.setUsername(username);
        assertEquals(username, response.getUsername());
    }

    @Test
    void testSetExpiresAt() {
        ServiceAccountTokenResponse response = new ServiceAccountTokenResponse();
        assertNull(response.getExpiresAt());

        String expiresAt = "2026-01-01T00:00:00";
        response.setExpiresAt(expiresAt);
        assertEquals(expiresAt, response.getExpiresAt());
    }

    @Test
    void testSetExpirationDays() {
        ServiceAccountTokenResponse response = new ServiceAccountTokenResponse();
        assertNull(response.getExpirationDays());

        Integer days = 180;
        response.setExpirationDays(days);
        assertEquals(days, response.getExpirationDays());
    }

    @Test
    void testWithLongToken() {
        StringBuilder longToken = new StringBuilder();
        for (int i = 0; i < 500; i++) {
            longToken.append("a");
        }

        ServiceAccountTokenResponse response = new ServiceAccountTokenResponse();
        response.setToken(longToken.toString());

        assertTrue(response.getToken().length() > 400);
        assertEquals(longToken.toString(), response.getToken());
    }

    @Test
    void testWithEmptyStrings() {
        ServiceAccountTokenResponse response = new ServiceAccountTokenResponse("", "", "", "", 0);

        assertEquals("", response.getToken());
        assertEquals("", response.getTokenName());
        assertEquals("", response.getUsername());
        assertEquals("", response.getExpiresAt());
        assertEquals(0, response.getExpirationDays());
    }

    @Test
    void testWithNullConstructorValues() {
        ServiceAccountTokenResponse response = new ServiceAccountTokenResponse(null, null, null, null, null);

        assertNull(response.getToken());
        assertNull(response.getTokenName());
        assertNull(response.getUsername());
        assertNull(response.getExpiresAt());
        assertNull(response.getExpirationDays());
    }

    @Test
    void testSetAllFieldsToNull() {
        ServiceAccountTokenResponse response = new ServiceAccountTokenResponse(
                "token", "name", "user", "2025-12-31", 365
        );

        response.setToken(null);
        response.setTokenName(null);
        response.setUsername(null);
        response.setExpiresAt(null);
        response.setExpirationDays(null);

        assertNull(response.getToken());
        assertNull(response.getTokenName());
        assertNull(response.getUsername());
        assertNull(response.getExpiresAt());
        assertNull(response.getExpirationDays());
    }

    @Test
    void testModifyAllFields() {
        ServiceAccountTokenResponse response = new ServiceAccountTokenResponse(
                "old-token", "old-name", "old-user", "2025-01-01", 30
        );

        response.setToken("new-token");
        response.setTokenName("new-name");
        response.setUsername("new-user");
        response.setExpiresAt("2026-01-01");
        response.setExpirationDays(365);

        assertEquals("new-token", response.getToken());
        assertEquals("new-name", response.getTokenName());
        assertEquals("new-user", response.getUsername());
        assertEquals("2026-01-01", response.getExpiresAt());
        assertEquals(365, response.getExpirationDays());
    }

    @Test
    void testWithZeroExpirationDays() {
        ServiceAccountTokenResponse response = new ServiceAccountTokenResponse();
        response.setExpirationDays(0);
        assertEquals(0, response.getExpirationDays());
    }

    @Test
    void testWithNegativeExpirationDays() {
        ServiceAccountTokenResponse response = new ServiceAccountTokenResponse();
        response.setExpirationDays(-1);
        assertEquals(-1, response.getExpirationDays());
    }

    @Test
    void testWithLargeExpirationDays() {
        ServiceAccountTokenResponse response = new ServiceAccountTokenResponse();
        response.setExpirationDays(9999);
        assertEquals(9999, response.getExpirationDays());
    }

    @Test
    void testWithSpecialCharactersInFields() {
        String tokenWithSpecialChars = "token_with-special.chars!@#$%";
        String nameWithSpecialChars = "name (v1.2) [prod]";

        ServiceAccountTokenResponse response = new ServiceAccountTokenResponse();
        response.setToken(tokenWithSpecialChars);
        response.setTokenName(nameWithSpecialChars);

        assertEquals(tokenWithSpecialChars, response.getToken());
        assertEquals(nameWithSpecialChars, response.getTokenName());
    }

    @Test
    void testCompleteTokenCreationScenario() {
        // Simulate complete token creation for service account
        ServiceAccountTokenResponse response = new ServiceAccountTokenResponse(
                "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJzZXJ2aWNlLWFjY291bnQifQ",
                "github-actions-token",
                "service-account-github",
                "2025-12-31T23:59:59Z",
                365
        );

        assertNotNull(response);
        assertTrue(response.getToken().startsWith("eyJ"));
        assertTrue(response.getTokenName().contains("github"));
        assertTrue(response.getUsername().contains("service"));
        assertTrue(response.getExpiresAt().contains("2025"));
        assertTrue(response.getExpirationDays() > 0);
    }

    @Test
    void testTokenRenewalScenario() {
        // Simulate token renewal with updated expiration
        ServiceAccountTokenResponse response = new ServiceAccountTokenResponse(
                "original-token",
                "api-token",
                "service-user",
                "2025-06-30T00:00:00",
                180
        );

        // Renew token with new expiration
        response.setToken("renewed-token");
        response.setExpiresAt("2026-06-30T00:00:00");
        response.setExpirationDays(365);

        assertEquals("renewed-token", response.getToken());
        assertEquals("2026-06-30T00:00:00", response.getExpiresAt());
        assertEquals(365, response.getExpirationDays());
        // Token name and username remain the same
        assertEquals("api-token", response.getTokenName());
        assertEquals("service-user", response.getUsername());
    }

    @Test
    void testWithMaxIntegerExpirationDays() {
        ServiceAccountTokenResponse response = new ServiceAccountTokenResponse();
        response.setExpirationDays(Integer.MAX_VALUE);
        assertEquals(Integer.MAX_VALUE, response.getExpirationDays());
    }
}
