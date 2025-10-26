package gov.nist.oscal.tools.api.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SignRequestTest {

    @Test
    void testNoArgsConstructor() {
        SignRequest request = new SignRequest();
        assertNotNull(request);
        assertNull(request.getAuthorizationId());
    }

    @Test
    void testAllArgsConstructor() {
        Long authId = 12345L;
        SignRequest request = new SignRequest(authId);

        assertNotNull(request);
        assertEquals(authId, request.getAuthorizationId());
    }

    @Test
    void testGetAuthorizationId() {
        Long authId = 67890L;
        SignRequest request = new SignRequest(authId);

        assertEquals(authId, request.getAuthorizationId());
    }

    @Test
    void testSetAuthorizationId() {
        SignRequest request = new SignRequest();
        Long authId = 11111L;

        request.setAuthorizationId(authId);

        assertEquals(authId, request.getAuthorizationId());
    }

    @Test
    void testSetAuthorizationIdToNull() {
        SignRequest request = new SignRequest(12345L);

        request.setAuthorizationId(null);

        assertNull(request.getAuthorizationId());
    }

    @Test
    void testSetAuthorizationIdMultipleTimes() {
        SignRequest request = new SignRequest();

        Long firstId = 111L;
        request.setAuthorizationId(firstId);
        assertEquals(firstId, request.getAuthorizationId());

        Long secondId = 222L;
        request.setAuthorizationId(secondId);
        assertEquals(secondId, request.getAuthorizationId());
    }
}
