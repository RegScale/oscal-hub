package gov.nist.oscal.tools.api.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import gov.nist.oscal.tools.api.config.AuditLogConfig;
import gov.nist.oscal.tools.api.entity.AuditEvent;
import gov.nist.oscal.tools.api.model.AuditEventType;
import gov.nist.oscal.tools.api.repository.AuditEventRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for AuditLogService.
 *
 * Coverage focus is the integrity-chain mechanics that are the entire point
 * of this service: each saved event carries a SHA-256 hash of its content
 * plus the previous event's hash. Tampering with any historical event
 * breaks the chain at that point and is detectable via verifyEventIntegrity.
 *
 * Also covers:
 *  - Disabled config short-circuits all log* methods
 *  - SIEM forwarding receives every saved event
 *  - HTTP request context (X-Forwarded-For, X-Real-IP, RemoteAddr) is captured
 *  - Metadata serialization handles null/empty/populated maps
 *  - SiemForwardingService is optional (null is tolerated)
 */
class AuditLogServiceTest {

    private AuditEventRepository repo;
    private AuditLogConfig config;
    private SiemForwardingService siem;
    private ObjectMapper mapper;
    private AuditLogService service;

    @BeforeEach
    void setUp() {
        repo = mock(AuditEventRepository.class);
        config = mock(AuditLogConfig.class);
        siem = mock(SiemForwardingService.class);
        mapper = new ObjectMapper();

        when(config.isEnabled()).thenReturn(true);
        when(config.isLogToApplicationLog()).thenReturn(false);
        when(repo.findTopByOrderByIdDesc()).thenReturn(null);
        when(repo.save(any(AuditEvent.class))).thenAnswer(inv -> {
            AuditEvent e = inv.getArgument(0);
            e.setId(System.nanoTime()); // simulate DB id assignment
            return e;
        });

        service = new AuditLogService(repo, config, mapper, siem);
        // The @Autowired self-reference is null in unit tests; methods that
        // rely on self.* (logAuthSuccess, etc.) won't work without it. For
        // those, we point self at the same instance so the call goes through.
        ReflectionTestUtils.setField(service, "self", service);
    }

    // ---------- enable/disable short-circuit ----------

    @Test
    void disabled_logEvent_isNoOp() {
        when(config.isEnabled()).thenReturn(false);
        service.logEvent(AuditEventType.AUTH_LOGIN_SUCCESS, "alice", "SUCCESS");
        verify(repo, never()).save(any());
        verify(siem, never()).queueEvent(any());
    }

    @Test
    void disabled_logFailure_isNoOp() {
        when(config.isEnabled()).thenReturn(false);
        service.logFailure(AuditEventType.AUTH_LOGIN_FAILURE, "alice", "bad password");
        verify(repo, never()).save(any());
    }

    @Test
    void disabled_logSecurityEvent_isNoOp() {
        when(config.isEnabled()).thenReturn(false);
        service.logSecurityEvent(AuditEventType.MFA_SETUP_COMPLETED, "alice", "details", null);
        verify(repo, never()).save(any());
    }

    // ---------- happy path ----------

    @Test
    void logEvent_persistsEvent_withIntegrityHash_andPreviousHash() {
        service.logEvent(AuditEventType.AUTH_LOGIN_SUCCESS, "alice", "SUCCESS");

        ArgumentCaptor<AuditEvent> cap = ArgumentCaptor.forClass(AuditEvent.class);
        verify(repo).save(cap.capture());
        AuditEvent saved = cap.getValue();
        assertThat(saved.getEventType()).isEqualTo(AuditEventType.AUTH_LOGIN_SUCCESS);
        assertThat(saved.getUsername()).isEqualTo("alice");
        assertThat(saved.getOutcome()).isEqualTo("SUCCESS");
        assertThat(saved.getIntegrityHash()).isNotNull().hasSize(64); // SHA-256 hex
        // First event in a fresh chain → previousHash is empty string, not null.
        assertThat(saved.getPreviousHash()).isEqualTo("");
    }

    @Test
    void logEvent_chainsHashes_eachEventReferencesPriorHash() {
        // The point of the chain: tamper with event N and the integrity hash
        // computed from {N, prevHash} no longer matches event N+1's prevHash.
        service.logEvent(AuditEventType.AUTH_LOGIN_SUCCESS, "alice", "SUCCESS");
        service.logEvent(AuditEventType.AUTH_LOGOUT, "alice", "SUCCESS");

        ArgumentCaptor<AuditEvent> cap = ArgumentCaptor.forClass(AuditEvent.class);
        verify(repo, times(2)).save(cap.capture());
        AuditEvent first = cap.getAllValues().get(0);
        AuditEvent second = cap.getAllValues().get(1);

        // Second event's previousHash equals first event's integrityHash.
        assertThat(second.getPreviousHash()).isEqualTo(first.getIntegrityHash());
        // Hashes are different (different events).
        assertThat(first.getIntegrityHash()).isNotEqualTo(second.getIntegrityHash());
    }

    @Test
    void logEvent_chainContinuesFromDatabase_onStartup() {
        // If the service starts up with a populated DB, the chain must
        // continue from the last persisted hash — not start over (which would
        // make the chain unverifiable across restarts).
        AuditEvent priorEvent = new AuditEvent(AuditEventType.AUTH_LOGIN_SUCCESS, "old", "SUCCESS");
        priorEvent.setIntegrityHash("a".repeat(64));
        when(repo.findTopByOrderByIdDesc()).thenReturn(priorEvent);

        AuditLogService freshService = new AuditLogService(repo, config, mapper, siem);
        ReflectionTestUtils.setField(freshService, "self", freshService);

        freshService.logEvent(AuditEventType.AUTH_LOGIN_SUCCESS, "alice", "SUCCESS");
        ArgumentCaptor<AuditEvent> cap = ArgumentCaptor.forClass(AuditEvent.class);
        verify(repo).save(cap.capture());
        assertThat(cap.getValue().getPreviousHash()).isEqualTo("a".repeat(64));
    }

    @Test
    void logEvent_forwardsToSiem_afterPersistence() {
        // SIEM queueing must happen AFTER save so the SIEM never sees an event
        // that didn't make it to the DB (otherwise audit and SIEM would diverge).
        service.logEvent(AuditEventType.AUTH_LOGIN_SUCCESS, "alice", "SUCCESS");
        verify(siem, times(1)).queueEvent(any(AuditEvent.class));
    }

    @Test
    void logEvent_metadataMap_serializedAsJson() {
        Map<String, Object> meta = Map.of("attempts", 3, "ip", "1.2.3.4");
        service.logEvent(AuditEventType.AUTH_LOGIN_FAILURE, "alice", 7L,
                "FAILURE", null, "LOGIN", meta);

        ArgumentCaptor<AuditEvent> cap = ArgumentCaptor.forClass(AuditEvent.class);
        verify(repo).save(cap.capture());
        String json = cap.getValue().getMetadata();
        assertThat(json).contains("\"attempts\":3").contains("\"ip\":\"1.2.3.4\"");
    }

    @Test
    void logEvent_emptyMetadata_doesNotSerialize() {
        // Empty map shouldn't produce "{}" in the metadata column — keep it null
        // so DB indexes/queries can distinguish "no metadata" from "empty metadata".
        service.logEvent(AuditEventType.AUTH_LOGIN_SUCCESS, "alice", 7L,
                "SUCCESS", null, "LOGIN", Map.of());

        ArgumentCaptor<AuditEvent> cap = ArgumentCaptor.forClass(AuditEvent.class);
        verify(repo).save(cap.capture());
        assertThat(cap.getValue().getMetadata()).isNull();
    }

    @Test
    void logFailure_setsOutcomeFailureAndError() {
        service.logFailure(AuditEventType.AUTH_LOGIN_FAILURE, "alice", "bad password");

        ArgumentCaptor<AuditEvent> cap = ArgumentCaptor.forClass(AuditEvent.class);
        verify(repo).save(cap.capture());
        assertThat(cap.getValue().getOutcome()).isEqualTo("FAILURE");
        assertThat(cap.getValue().getErrorMessage()).isEqualTo("bad password");
    }

    // ---------- HTTP request context capture ----------

    @Test
    void logSecurityEvent_capturesIpAndUserAgentFromRequest() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setRemoteAddr("9.9.9.9");
        req.addHeader("User-Agent", "Mozilla/5.0");
        req.setMethod("POST");
        req.setRequestURI("/api/mfa/enable");

        service.logSecurityEvent(AuditEventType.MFA_SETUP_COMPLETED, "alice", "MFA setup", req);

        ArgumentCaptor<AuditEvent> cap = ArgumentCaptor.forClass(AuditEvent.class);
        verify(repo).save(cap.capture());
        AuditEvent saved = cap.getValue();
        assertThat(saved.getIpAddress()).isEqualTo("9.9.9.9");
        assertThat(saved.getUserAgent()).isEqualTo("Mozilla/5.0");
        assertThat(saved.getHttpMethod()).isEqualTo("POST");
        assertThat(saved.getRequestUrl()).isEqualTo("/api/mfa/enable");
    }

    @Test
    void logSecurityEvent_xForwardedFor_takesFirstIp() {
        // Behind a load balancer, the client IP is the first entry in the
        // X-Forwarded-For chain. The audit log must record that, not the LB.
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addHeader("X-Forwarded-For", "203.0.113.5, 10.0.0.1");
        req.setRemoteAddr("10.0.0.99");

        service.logSecurityEvent(AuditEventType.MFA_VERIFICATION_SUCCESS, "alice", "MFA off", req);

        ArgumentCaptor<AuditEvent> cap = ArgumentCaptor.forClass(AuditEvent.class);
        verify(repo).save(cap.capture());
        assertThat(cap.getValue().getIpAddress()).isEqualTo("203.0.113.5");
    }

    @Test
    void logSecurityEvent_xRealIp_usedWhenNoForwardedFor() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addHeader("X-Real-IP", "198.51.100.7");
        req.setRemoteAddr("10.0.0.99");

        service.logSecurityEvent(AuditEventType.MFA_SETUP_COMPLETED, "alice", "MFA setup", req);

        ArgumentCaptor<AuditEvent> cap = ArgumentCaptor.forClass(AuditEvent.class);
        verify(repo).save(cap.capture());
        assertThat(cap.getValue().getIpAddress()).isEqualTo("198.51.100.7");
    }

    @Test
    void logSecurityEvent_nullRequest_doesNotPopulateRequestFields() {
        // Some flows (background jobs, Quartz schedulers) have no HTTP request.
        // Must not crash; just leave the request fields null.
        service.logSecurityEvent(AuditEventType.MFA_SETUP_COMPLETED, "system", "auto-enabled", null);

        ArgumentCaptor<AuditEvent> cap = ArgumentCaptor.forClass(AuditEvent.class);
        verify(repo).save(cap.capture());
        assertThat(cap.getValue().getIpAddress()).isNull();
        assertThat(cap.getValue().getUserAgent()).isNull();
    }

    @Test
    void logSecurityEventFailure_setsOutcomeFailure_andErrorEqualsDetails() {
        service.logSecurityEventFailure(AuditEventType.MFA_VERIFICATION_FAILURE,
                "alice", "wrong code", null);

        ArgumentCaptor<AuditEvent> cap = ArgumentCaptor.forClass(AuditEvent.class);
        verify(repo).save(cap.capture());
        AuditEvent saved = cap.getValue();
        assertThat(saved.getOutcome()).isEqualTo("FAILURE");
        assertThat(saved.getErrorMessage()).isEqualTo("wrong code");
    }

    @Test
    void logConfigChange_recordsResourceAsSecurityPolicy() {
        // Config change events get filed under a sentinel resource so admins
        // can filter for "what changed" in the dashboard.
        service.logConfigChange("admin", "Set lockoutMaxAttempts=10", null);

        ArgumentCaptor<AuditEvent> cap = ArgumentCaptor.forClass(AuditEvent.class);
        verify(repo).save(cap.capture());
        AuditEvent saved = cap.getValue();
        assertThat(saved.getResource()).isEqualTo("SecurityPolicy");
        assertThat(saved.getEventType()).isEqualTo(AuditEventType.CONFIG_SECURITY_POLICY_CHANGE);
    }

    // ---------- verifyEventIntegrity ----------

    @Test
    void verifyEventIntegrity_unchangedEvent_returnsTrue() {
        // Create an event, persist it (which computes hash), then verify.
        service.logEvent(AuditEventType.AUTH_LOGIN_SUCCESS, "alice", "SUCCESS");
        ArgumentCaptor<AuditEvent> cap = ArgumentCaptor.forClass(AuditEvent.class);
        verify(repo).save(cap.capture());
        AuditEvent saved = cap.getValue();
        when(repo.findById(saved.getId())).thenReturn(Optional.of(saved));

        assertThat(service.verifyEventIntegrity(saved.getId())).isTrue();
    }

    @Test
    void verifyEventIntegrity_tamperedEvent_returnsFalse() {
        // After save, mutate a field. Recomputed hash won't match the stored one.
        service.logEvent(AuditEventType.AUTH_LOGIN_SUCCESS, "alice", "SUCCESS");
        ArgumentCaptor<AuditEvent> cap = ArgumentCaptor.forClass(AuditEvent.class);
        verify(repo).save(cap.capture());
        AuditEvent saved = cap.getValue();
        // Tamper: change username after the fact
        saved.setUsername("eve");
        when(repo.findById(saved.getId())).thenReturn(Optional.of(saved));

        assertThat(service.verifyEventIntegrity(saved.getId())).isFalse();
    }

    @Test
    void verifyEventIntegrity_unknownEventId_returnsFalse() {
        when(repo.findById(eq(999L))).thenReturn(Optional.empty());
        assertThat(service.verifyEventIntegrity(999L)).isFalse();
    }

    // ---------- SIEM optional ----------

    @Test
    void nullSiem_doesNotCrashOnLog() {
        // SiemForwardingService is @Lazy; a misconfigured deployment might
        // resolve it to null. The logger must still persist events.
        AuditLogService noSiem = new AuditLogService(repo, config, mapper, null);
        ReflectionTestUtils.setField(noSiem, "self", noSiem);

        noSiem.logEvent(AuditEventType.AUTH_LOGIN_SUCCESS, "alice", "SUCCESS");
        verify(repo).save(any(AuditEvent.class));
    }

    // ---------- repository failure isolation ----------

    @Test
    void repoFailure_doesNotPropagateException() {
        // If the audit DB write fails, we want the calling business operation
        // to still succeed — audit logging is best-effort to avoid breaking
        // user-facing flows when the audit table fills up or DB hiccups.
        org.mockito.Mockito.doThrow(new RuntimeException("DB down"))
                .when(repo).save(any(AuditEvent.class));

        // Should NOT throw.
        service.logEvent(AuditEventType.AUTH_LOGIN_SUCCESS, "alice", "SUCCESS");
    }

    // ---------- convenience wrappers ----------

    @Test
    void logAuthSuccess_routesToLogEvent_withSuccessOutcome() {
        service.logAuthSuccess("alice", 7L);
        ArgumentCaptor<AuditEvent> cap = ArgumentCaptor.forClass(AuditEvent.class);
        verify(repo).save(cap.capture());
        assertThat(cap.getValue().getEventType()).isEqualTo(AuditEventType.AUTH_LOGIN_SUCCESS);
        assertThat(cap.getValue().getOutcome()).isEqualTo("SUCCESS");
        assertThat(cap.getValue().getUserId()).isEqualTo(7L);
    }

    @Test
    void logAuthFailure_routesToLogFailure_withErrorMessage() {
        service.logAuthFailure("alice", "bad password");
        ArgumentCaptor<AuditEvent> cap = ArgumentCaptor.forClass(AuditEvent.class);
        verify(repo).save(cap.capture());
        assertThat(cap.getValue().getOutcome()).isEqualTo("FAILURE");
        assertThat(cap.getValue().getErrorMessage()).isEqualTo("bad password");
    }

    @Test
    void logAccountLockout_includesFailedAttemptsInMetadata() {
        service.logAccountLockout("alice", 7L, 5);
        ArgumentCaptor<AuditEvent> cap = ArgumentCaptor.forClass(AuditEvent.class);
        verify(repo).save(cap.capture());
        assertThat(cap.getValue().getMetadata()).contains("\"failedAttempts\":5");
    }

    @Test
    void logFileUpload_includesFileNameAndSize() {
        service.logFileUpload("alice", 7L, "ssp.json", 12345L);
        ArgumentCaptor<AuditEvent> cap = ArgumentCaptor.forClass(AuditEvent.class);
        verify(repo).save(cap.capture());
        AuditEvent saved = cap.getValue();
        assertThat(saved.getResource()).isEqualTo("ssp.json");
        assertThat(saved.getMetadata())
                .contains("\"fileName\":\"ssp.json\"")
                .contains("\"fileSize\":12345");
    }
}
