package gov.nist.oscal.tools.api.entity;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ServiceAccountTokenTest {

    private ServiceAccountToken token(LocalDateTime expiresAt, LocalDateTime revokedAt) {
        ServiceAccountToken t = new ServiceAccountToken();
        t.setExpiresAt(expiresAt);
        t.setRevokedAt(revokedAt);
        return t;
    }

    @Test
    void unexpiredUnrevokedTokenIsActive() {
        ServiceAccountToken t = token(LocalDateTime.now().plusDays(1), null);

        assertEquals(ServiceAccountToken.Status.ACTIVE, t.getStatus());
    }

    @Test
    void pastExpiryReportsExpired() {
        ServiceAccountToken t = token(LocalDateTime.now().minusMinutes(1), null);

        assertEquals(ServiceAccountToken.Status.EXPIRED, t.getStatus());
    }

    @Test
    void revokedTakesPrecedenceOverExpired() {
        ServiceAccountToken t = token(LocalDateTime.now().minusMinutes(1), LocalDateTime.now().minusHours(2));

        assertEquals(ServiceAccountToken.Status.REVOKED, t.getStatus());
    }
}
