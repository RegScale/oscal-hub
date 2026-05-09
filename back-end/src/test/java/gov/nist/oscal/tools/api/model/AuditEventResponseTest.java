package gov.nist.oscal.tools.api.model;

import gov.nist.oscal.tools.api.entity.AuditEvent;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for AuditEventResponse — focuses on the IP-masking branch
 * coverage that's the only real logic in this DTO. The dashboard relies
 * on this masking to limit re-identification when admin screenshots or
 * proxied responses leak. Errors here are silent privacy failures.
 */
class AuditEventResponseTest {

    @Test
    void from_nullEntity_returnsNull() {
        // Defensive: callers shouldn't crash when streaming a paginated result
        // that contains a null row (e.g. soft-deleted rows in some queries).
        assertThat(AuditEventResponse.from(null)).isNull();
    }

    @Test
    void from_copiesAllScalarFields() {
        AuditEvent e = new AuditEvent();
        e.setId(99L);
        e.setUsername("alice");
        e.setUserId(7L);
        e.setIpAddress("203.0.113.42");
        e.setUserAgent("Mozilla/5.0");
        e.setSessionId("sess-abc");
        e.setResource("catalog");
        e.setAction("export");
        e.setOutcome("SUCCESS");
        e.setErrorMessage(null);
        e.setMetadata("{}");
        e.setRiskLevel("LOW");
        LocalDateTime ts = LocalDateTime.of(2026, 5, 8, 12, 0);
        e.setTimestamp(ts);
        e.setProcessingTimeMs(123L);
        e.setReviewed(false);
        e.setRequestUrl("/api/foo");
        e.setHttpMethod("GET");

        AuditEventResponse r = AuditEventResponse.from(e);

        assertThat(r.getId()).isEqualTo(99L);
        assertThat(r.getUsername()).isEqualTo("alice");
        assertThat(r.getUserId()).isEqualTo(7L);
        // IP is masked here, not equal to the original
        assertThat(r.getIpAddress()).isEqualTo("203.0.x.x");
        assertThat(r.getUserAgent()).isEqualTo("Mozilla/5.0");
        assertThat(r.getResource()).isEqualTo("catalog");
        assertThat(r.getAction()).isEqualTo("export");
        assertThat(r.getOutcome()).isEqualTo("SUCCESS");
        assertThat(r.getRiskLevel()).isEqualTo("LOW");
        assertThat(r.getTimestamp()).isEqualTo(ts);
        assertThat(r.getProcessingTimeMs()).isEqualTo(123L);
        assertThat(r.getRequestUrl()).isEqualTo("/api/foo");
        assertThat(r.getHttpMethod()).isEqualTo("GET");
    }

    @Test
    void from_nullEventType_yieldsNullEventTypeString() {
        // The eventType field comes from an enum; null entity field must not NPE
        // on the enum-name conversion.
        AuditEvent e = new AuditEvent();
        e.setEventType(null);
        AuditEventResponse r = AuditEventResponse.from(e);
        assertThat(r.getEventType()).isNull();
    }

    // ---------- IP masking ----------

    @Test
    void maskIp_ipv4_masksLastTwoOctets() {
        // Standard public IPv4 — keep network portion, mask host.
        assertThat(AuditEventResponse.maskIp("203.0.113.42")).isEqualTo("203.0.x.x");
        assertThat(AuditEventResponse.maskIp("10.20.30.40")).isEqualTo("10.20.x.x");
    }

    @Test
    void maskIp_loopback_isNotMasked() {
        // 127.0.0.1 / ::1 / localhost are operational debugging cases that need
        // to remain identifiable so admins can spot health-check noise.
        assertThat(AuditEventResponse.maskIp("127.0.0.1")).isEqualTo("127.0.0.1");
        assertThat(AuditEventResponse.maskIp("::1")).isEqualTo("::1");
        assertThat(AuditEventResponse.maskIp("localhost")).isEqualTo("localhost");
        assertThat(AuditEventResponse.maskIp("LOCALHOST")).isEqualTo("LOCALHOST");
    }

    @Test
    void maskIp_null_returnsNull() {
        assertThat(AuditEventResponse.maskIp(null)).isNull();
    }

    @Test
    void maskIp_blank_returnsBlank() {
        // Whitespace passes through unchanged — masking blanks adds noise.
        assertThat(AuditEventResponse.maskIp("")).isEqualTo("");
        assertThat(AuditEventResponse.maskIp("   ")).isEqualTo("   ");
    }

    @Test
    void maskIp_ipv6_masksTrailingHalf() {
        // IPv6 has 8 groups; we mask the host half.
        String full = "2001:0db8:85a3:0000:0000:8a2e:0370:7334";
        String masked = AuditEventResponse.maskIp(full);
        // First 4 groups remain, last 4 become x
        assertThat(masked).startsWith("2001:0db8:85a3:0000:");
        assertThat(masked).endsWith(":x:x:x:x");
    }

    @Test
    void maskIp_malformedIpv4_passesThroughUnchanged() {
        // 3-octet "addresses" are nonsensical; rather than masking incorrectly,
        // fall back to the original string. This shows up as an obvious oddity
        // in the dashboard rather than silent corruption.
        assertThat(AuditEventResponse.maskIp("10.20.30")).isEqualTo("10.20.30");
        assertThat(AuditEventResponse.maskIp("not-an-ip")).isEqualTo("not-an-ip");
    }

    @Test
    void maskIp_ipv6_tooShort_passesThrough() {
        // Fewer than 4 groups isn't a real IPv6 address; mask logic skips it.
        assertThat(AuditEventResponse.maskIp("ab:cd")).isEqualTo("ab:cd");
    }
}
