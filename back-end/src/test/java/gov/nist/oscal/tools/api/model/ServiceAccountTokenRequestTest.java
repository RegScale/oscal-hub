package gov.nist.oscal.tools.api.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ServiceAccountTokenRequestTest {

    @Test
    void testNoArgsConstructor() {
        ServiceAccountTokenRequest request = new ServiceAccountTokenRequest();

        assertNotNull(request);
        assertNull(request.getTokenName());
        assertNull(request.getExpirationDays());
    }

    @Test
    void testAllArgsConstructor() {
        ServiceAccountTokenRequest request = new ServiceAccountTokenRequest(
                "api-token-prod",
                90
        );

        assertEquals("api-token-prod", request.getTokenName());
        assertEquals(90, request.getExpirationDays());
    }

    @Test
    void testSetTokenName() {
        ServiceAccountTokenRequest request = new ServiceAccountTokenRequest();
        request.setTokenName("deployment-token");
        assertEquals("deployment-token", request.getTokenName());
    }

    @Test
    void testSetExpirationDays() {
        ServiceAccountTokenRequest request = new ServiceAccountTokenRequest();
        request.setExpirationDays(30);
        assertEquals(30, request.getExpirationDays());
    }

    @Test
    void testSetAllFields() {
        ServiceAccountTokenRequest request = new ServiceAccountTokenRequest();
        request.setTokenName("ci-cd-token");
        request.setExpirationDays(365);

        assertEquals("ci-cd-token", request.getTokenName());
        assertEquals(365, request.getExpirationDays());
    }

    @Test
    void testSetFieldsToNull() {
        ServiceAccountTokenRequest request = new ServiceAccountTokenRequest(
                "test-token",
                7
        );

        request.setTokenName(null);
        request.setExpirationDays(null);

        assertNull(request.getTokenName());
        assertNull(request.getExpirationDays());
    }

    @Test
    void testModifyAllFields() {
        ServiceAccountTokenRequest request = new ServiceAccountTokenRequest();

        request.setTokenName("old-token");
        request.setExpirationDays(10);

        request.setTokenName("new-token");
        request.setExpirationDays(20);

        assertEquals("new-token", request.getTokenName());
        assertEquals(20, request.getExpirationDays());
    }

    @Test
    void testWithEmptyTokenName() {
        ServiceAccountTokenRequest request = new ServiceAccountTokenRequest("", 30);
        assertEquals("", request.getTokenName());
        assertEquals(30, request.getExpirationDays());
    }

    @Test
    void testWithMinimumExpirationDays() {
        ServiceAccountTokenRequest request = new ServiceAccountTokenRequest("min-token", 1);
        assertEquals("min-token", request.getTokenName());
        assertEquals(1, request.getExpirationDays());
    }

    @Test
    void testWithLargeExpirationDays() {
        ServiceAccountTokenRequest request = new ServiceAccountTokenRequest("long-term-token", 3650);
        assertEquals("long-term-token", request.getTokenName());
        assertEquals(3650, request.getExpirationDays());
    }

    @Test
    void testWithZeroExpirationDays() {
        ServiceAccountTokenRequest request = new ServiceAccountTokenRequest();
        request.setExpirationDays(0);
        assertEquals(0, request.getExpirationDays());
    }

    @Test
    void testWithNegativeExpirationDays() {
        ServiceAccountTokenRequest request = new ServiceAccountTokenRequest();
        request.setExpirationDays(-1);
        assertEquals(-1, request.getExpirationDays());
    }

    @Test
    void testWithSpecialCharactersInTokenName() {
        ServiceAccountTokenRequest request = new ServiceAccountTokenRequest();
        String tokenWithSpecialChars = "api-token_v2.0-prod[2025]";
        request.setTokenName(tokenWithSpecialChars);
        assertEquals(tokenWithSpecialChars, request.getTokenName());
        assertTrue(request.getTokenName().contains("_"));
        assertTrue(request.getTokenName().contains("-"));
    }

    @Test
    void testWithLongTokenName() {
        ServiceAccountTokenRequest request = new ServiceAccountTokenRequest();
        StringBuilder longTokenName = new StringBuilder();
        for (int i = 0; i < 50; i++) {
            longTokenName.append("token");
        }
        request.setTokenName(longTokenName.toString());
        assertEquals(longTokenName.toString(), request.getTokenName());
        assertTrue(request.getTokenName().length() > 100);
    }

    @Test
    void testCommonExpirationScenarios() {
        // 7 days - short term
        ServiceAccountTokenRequest shortTerm = new ServiceAccountTokenRequest("short-term", 7);
        assertEquals(7, shortTerm.getExpirationDays());

        // 30 days - monthly
        ServiceAccountTokenRequest monthly = new ServiceAccountTokenRequest("monthly", 30);
        assertEquals(30, monthly.getExpirationDays());

        // 90 days - quarterly
        ServiceAccountTokenRequest quarterly = new ServiceAccountTokenRequest("quarterly", 90);
        assertEquals(90, quarterly.getExpirationDays());

        // 365 days - annual
        ServiceAccountTokenRequest annual = new ServiceAccountTokenRequest("annual", 365);
        assertEquals(365, annual.getExpirationDays());
    }

    @Test
    void testProductionTokenScenario() {
        ServiceAccountTokenRequest request = new ServiceAccountTokenRequest(
                "production-api-token-2025",
                365
        );

        assertNotNull(request.getTokenName());
        assertNotNull(request.getExpirationDays());
        assertTrue(request.getTokenName().contains("production"));
        assertTrue(request.getExpirationDays() >= 365);
    }

    @Test
    void testDevelopmentTokenScenario() {
        ServiceAccountTokenRequest request = new ServiceAccountTokenRequest(
                "dev-temp-token",
                7
        );

        assertNotNull(request.getTokenName());
        assertTrue(request.getTokenName().contains("dev"));
        assertTrue(request.getExpirationDays() <= 7);
    }

    @Test
    void testWithWhitespaceInTokenName() {
        ServiceAccountTokenRequest request = new ServiceAccountTokenRequest(
                "  token-with-spaces  ",
                30
        );
        assertEquals("  token-with-spaces  ", request.getTokenName());
    }

    @Test
    void testConstructorAndSettersCombined() {
        ServiceAccountTokenRequest request = new ServiceAccountTokenRequest(
                "initial-token",
                10
        );

        assertEquals("initial-token", request.getTokenName());
        assertEquals(10, request.getExpirationDays());

        request.setTokenName("updated-token");
        request.setExpirationDays(20);

        assertEquals("updated-token", request.getTokenName());
        assertEquals(20, request.getExpirationDays());
    }

    @Test
    void testWithNumericTokenName() {
        ServiceAccountTokenRequest request = new ServiceAccountTokenRequest("12345", 30);
        assertEquals("12345", request.getTokenName());
    }

    @Test
    void testWithUUIDTokenName() {
        ServiceAccountTokenRequest request = new ServiceAccountTokenRequest();
        String uuidToken = "550e8400-e29b-41d4-a716-446655440000";
        request.setTokenName(uuidToken);
        assertEquals(uuidToken, request.getTokenName());
    }

    @Test
    void testWithUnicodeCharacters() {
        ServiceAccountTokenRequest request = new ServiceAccountTokenRequest(
                "令牌名称",
                30
        );
        assertEquals("令牌名称", request.getTokenName());
        assertEquals(30, request.getExpirationDays());
    }

    @Test
    void testCIcdTokenScenario() {
        ServiceAccountTokenRequest request = new ServiceAccountTokenRequest(
                "github-actions-token",
                90
        );

        assertTrue(request.getTokenName().contains("github"));
        assertEquals(90, request.getExpirationDays());
    }
}
