package gov.nist.oscal.tools.api.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import gov.nist.oscal.tools.api.config.SiemConfig;
import gov.nist.oscal.tools.api.entity.AuditEvent;
import gov.nist.oscal.tools.api.model.AuditEventType;
import gov.nist.oscal.tools.api.repository.AuditEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Unit tests for SiemForwardingService.
 *
 * The HTTP send path is hard to mock without WireMock, but the formatting
 * and queueing logic — which is what would silently corrupt audit data
 * if it broke — is pure and reachable through the public API plus a few
 * reflection calls.
 *
 * Coverage focus:
 *  - queueEvent honors enabled/category/risk-level/failure-only filters
 *  - flushBatch drains up to batchSize events and is a no-op on empty queue
 *  - JSON / CEF / Syslog formatters produce correctly shaped output
 *  - Escaping rules for CEF (pipes, equals, newlines) and Syslog (quotes, brackets)
 *  - getStatus exposes counters that match what's been queued
 *  - testConnection short-circuits when disabled or webhook URL missing
 */
class SiemForwardingServiceTest {

    private SiemConfig config;
    private SiemForwardingService service;
    private final ObjectMapper om = new ObjectMapper();

    @BeforeEach
    void setUp() {
        config = new SiemConfig();
        config.setEnabled(true);
        config.setWebhookUrl("https://siem.example.com/ingest");
        config.setFormat("json");
        config.setBatchSize(10);
        config.setMaxRetries(0);
        config.setRetryDelayMs(1);
        config.setRetryBackoffMultiplier(2.0);
        config.setConnectionTimeoutMs(1000);
        config.setReadTimeoutMs(1000);
        config.setMinRiskLevel("LOW");
        config.setIncludeCategories(""); // empty = all
        config.setFailedEventsOnly(false);
        config.setCefVendor("NIST");
        config.setCefProduct("OSCAL-Hub");
        config.setCefVersion("1.0");
        config.setSyslogFacility("LOCAL0");
        config.setSyslogAppName("oscal");

        AuditEventRepository repo = mock(AuditEventRepository.class);
        service = new SiemForwardingService(config, repo, om);
    }

    // ---------- queueEvent + filtering ----------

    @Test
    void queueEvent_disabled_isDropped() {
        config.setEnabled(false);
        service.queueEvent(event(AuditEventType.AUTH_LOGIN_SUCCESS));
        assertThat(service.getStatus().getQueuedEvents()).isZero();
    }

    @Test
    void queueEvent_passingFilters_isQueued() {
        service.queueEvent(event(AuditEventType.AUTH_LOGIN_SUCCESS));
        assertThat(service.getStatus().getQueuedEvents()).isEqualTo(1);
    }

    @Test
    void queueEvent_failedEventsOnly_dropsSuccessOutcome() {
        // Production gotcha: when the SIEM only wants failures, we must not
        // ship successful logins. SIEM ingest costs scale with volume.
        config.setFailedEventsOnly(true);
        AuditEvent success = event(AuditEventType.AUTH_LOGIN_SUCCESS);
        success.setOutcome("SUCCESS");
        service.queueEvent(success);
        assertThat(service.getStatus().getQueuedEvents()).isZero();
    }

    @Test
    void queueEvent_failedEventsOnly_keepsFailureOutcome() {
        config.setFailedEventsOnly(true);
        AuditEvent fail = event(AuditEventType.AUTH_LOGIN_FAILURE);
        fail.setOutcome("FAILURE");
        service.queueEvent(fail);
        assertThat(service.getStatus().getQueuedEvents()).isEqualTo(1);
    }

    @Test
    void queueEvent_categoryFilter_dropsExcludedCategories() {
        // includeCategoriesSet is parsed in @PostConstruct, so re-validate after
        // changing the property in the test (mirrors what Spring would do).
        config.setIncludeCategories("Authentication");
        config.validateConfiguration();

        AuditEvent ev = event(AuditEventType.AUTH_LOGIN_SUCCESS);
        ev.setCategory("Authorization"); // not in include list
        service.queueEvent(ev);
        assertThat(service.getStatus().getQueuedEvents()).isZero();
    }

    @Test
    void queueEvent_categoryFilter_keepsIncludedCategory() {
        // The mirror case — make sure the include list actually lets matching
        // events through, not just that an exclusion drops the wrong ones.
        config.setIncludeCategories("Authentication,Authorization");
        config.validateConfiguration();

        AuditEvent ev = event(AuditEventType.AUTH_LOGIN_SUCCESS);
        ev.setCategory("Authentication");
        service.queueEvent(ev);
        assertThat(service.getStatus().getQueuedEvents()).isEqualTo(1);
    }

    @Test
    void queueEvent_riskLevelFilter_dropsBelowThreshold() {
        config.setMinRiskLevel("HIGH");
        AuditEvent low = event(AuditEventType.AUTH_LOGIN_SUCCESS);
        low.setRiskLevel("LOW");
        service.queueEvent(low);
        assertThat(service.getStatus().getQueuedEvents()).isZero();

        AuditEvent high = event(AuditEventType.AUTH_LOGIN_FAILURE);
        high.setRiskLevel("HIGH");
        service.queueEvent(high);
        assertThat(service.getStatus().getQueuedEvents()).isEqualTo(1);
    }

    // ---------- flushBatch / manualFlush ----------

    @Test
    void flushBatch_emptyQueue_isNoOp() {
        service.flushBatch();
        assertThat(service.getStatus().getEventsForwarded()).isZero();
        assertThat(service.getStatus().getEventsFailed()).isZero();
    }

    @Test
    void manualFlush_returnsQueuedCount_evenIfSendFails() {
        // The webhook URL is unreachable; we don't care about send success here,
        // only that manualFlush returns the count of events that WERE queued.
        service.queueEvent(event(AuditEventType.AUTH_LOGIN_SUCCESS));
        service.queueEvent(event(AuditEventType.AUTH_LOGIN_SUCCESS));

        int count = service.manualFlush();

        assertThat(count).isEqualTo(2);
    }

    // ---------- JSON formatting ----------

    @Test
    void formatAsJson_producesValidJsonArray_withRequiredFields() throws Exception {
        AuditEvent ev = event(AuditEventType.AUTH_LOGIN_SUCCESS);
        ev.setUsername("alice");
        ev.setIpAddress("10.0.0.1");
        String json = (String) invokePrivate("formatAsJson", List.class, List.of(ev));

        JsonNode array = om.readTree(json);
        assertThat(array.isArray()).isTrue();
        assertThat(array).hasSize(1);
        JsonNode first = array.get(0);
        assertThat(first.get("eventType").asText()).isEqualTo("AUTH_LOGIN_SUCCESS");
        assertThat(first.get("username").asText()).isEqualTo("alice");
        assertThat(first.get("ipAddress").asText()).isEqualTo("10.0.0.1");
        assertThat(first.has("timestamp")).isTrue();
    }

    @Test
    void formatAsJson_metadataString_isParsedAsNestedObject() throws Exception {
        // The metadata column is stored as JSON-string. We want SIEM consumers
        // to receive it as a real object so they can grep on nested fields.
        AuditEvent ev = event(AuditEventType.AUTH_LOGIN_SUCCESS);
        ev.setMetadata("{\"sessionId\":\"abc-123\",\"clientVersion\":\"1.2.3\"}");
        String json = (String) invokePrivate("formatAsJson", List.class, List.of(ev));

        JsonNode metadata = om.readTree(json).get(0).get("metadata");
        assertThat(metadata.isObject()).isTrue();
        assertThat(metadata.get("sessionId").asText()).isEqualTo("abc-123");
    }

    @Test
    void formatAsJson_invalidMetadata_isStoredAsString_notDropped() throws Exception {
        // If metadata is malformed (legacy rows), don't blow up — just include
        // it as a plain string so the audit record is still forwarded.
        AuditEvent ev = event(AuditEventType.AUTH_LOGIN_SUCCESS);
        ev.setMetadata("not valid json");
        String json = (String) invokePrivate("formatAsJson", List.class, List.of(ev));

        JsonNode metadata = om.readTree(json).get(0).get("metadata");
        assertThat(metadata.asText()).isEqualTo("not valid json");
    }

    // ---------- CEF formatting ----------

    @Test
    void formatAsCef_producesCefHeader_withVendorProductVersion() throws Exception {
        AuditEvent ev = event(AuditEventType.AUTH_LOGIN_FAILURE);
        ev.setUsername("alice");
        ev.setIpAddress("10.0.0.1");
        ev.setRiskLevel("HIGH");
        String cef = (String) invokePrivate("formatAsCef", List.class, List.of(ev));

        assertThat(cef).startsWith("CEF:0|NIST|OSCAL-Hub|1.0|AUTH_LOGIN_FAILURE|");
        // CEF severity for HIGH is mapped to 9
        assertThat(cef).contains("|9|");
        assertThat(cef).contains("suser=alice");
        assertThat(cef).contains("src=10.0.0.1");
    }

    @Test
    void formatAsCef_unknownRiskLevel_defaultsToSeverity3() throws Exception {
        // CEF requires a severity 0-10. If our riskLevel doesn't map, we fall
        // back to a low severity (3) rather than 0 (which means "Unknown").
        AuditEvent ev = event(AuditEventType.AUTH_LOGIN_SUCCESS);
        ev.setRiskLevel("PURPLE"); // not mapped
        String cef = (String) invokePrivate("formatAsCef", List.class, List.of(ev));
        assertThat(cef).contains("|3|");
    }

    @Test
    void escapeCef_pipesAndBackslashes_areEscaped() throws Exception {
        // Header fields use | as delimiter — unescaped pipes break CEF parsing
        // at the SIEM and can be exploited to inject fake fields.
        String escaped = (String) invokePrivate("escapeCef", String.class, "vendor|with|pipes\\and\\backslash");
        assertThat(escaped).isEqualTo("vendor\\|with\\|pipes\\\\and\\\\backslash");
    }

    @Test
    void escapeCef_null_returnsEmptyString() throws Exception {
        assertThat(invokePrivate("escapeCef", String.class, (String) null)).isEqualTo("");
    }

    @Test
    void escapeCefValue_equalsAndNewlines_areEscaped() throws Exception {
        // Extension values use key=value pairs — unescaped = breaks parsing,
        // and newlines would split a single event into multiple log lines.
        String escaped = (String) invokePrivate("escapeCefValue", String.class,
                "alice=admin\nrole=root\rdone");
        assertThat(escaped).isEqualTo("alice\\=admin\\nrole\\=root\\rdone");
    }

    // ---------- Syslog formatting ----------

    @Test
    void formatAsSyslog_producesRfc5424Header_withPriorityVersionTimestamp() throws Exception {
        AuditEvent ev = event(AuditEventType.AUTH_LOGIN_FAILURE);
        ev.setRiskLevel("HIGH");
        ev.setOutcome("FAILURE");
        ev.setUsername("alice");
        String syslog = (String) invokePrivate("formatAsSyslog", List.class, List.of(ev));

        // Format: <PRI>VERSION TIMESTAMP HOSTNAME APP-NAME PROCID MSGID STRUCTURED-DATA MSG
        assertThat(syslog).startsWith("<");
        assertThat(syslog).contains(">1 "); // version 1
        assertThat(syslog).contains("oscal "); // app name
        assertThat(syslog).contains("[oscal@1");
        assertThat(syslog).contains("eventType=\"AUTH_LOGIN_FAILURE\"");
        assertThat(syslog).contains("user=\"alice\"");
    }

    @Test
    void escapeSyslogValue_quotesAndBrackets_areEscaped() throws Exception {
        // RFC 5424 SD-PARAM values: backslash, quote, and ] must be escaped.
        String escaped = (String) invokePrivate("escapeSyslogValue", String.class,
                "value with \"quote\" and ] bracket and \\ backslash");
        assertThat(escaped).isEqualTo("value with \\\"quote\\\" and \\] bracket and \\\\ backslash");
    }

    @Test
    void getSyslogFacility_mapsKnownFacilityNames() throws Exception {
        config.setSyslogFacility("AUTH");
        assertThat(invokePrivate("getSyslogFacility")).isEqualTo(4);
        config.setSyslogFacility("AUTHPRIV");
        assertThat(invokePrivate("getSyslogFacility")).isEqualTo(10);
        config.setSyslogFacility("LOCAL3");
        assertThat(invokePrivate("getSyslogFacility")).isEqualTo(19);
    }

    @Test
    void getSyslogFacility_unknownName_fallsBackToLocal0() throws Exception {
        // Misconfigured facility shouldn't crash logging — default to LOCAL0
        // which is the conventional choice for app logs.
        config.setSyslogFacility("BOGUS");
        assertThat(invokePrivate("getSyslogFacility")).isEqualTo(16);
    }

    @Test
    void getSyslogSeverity_errorOutcome_mapsToError3() throws Exception {
        AuditEvent ev = event(AuditEventType.AUTH_LOGIN_FAILURE);
        ev.setOutcome("ERROR");
        assertThat(invokePrivate("getSyslogSeverity", AuditEvent.class, ev)).isEqualTo(3);
    }

    @Test
    void getSyslogSeverity_failureOutcome_mapsToWarning4() throws Exception {
        AuditEvent ev = event(AuditEventType.AUTH_LOGIN_FAILURE);
        ev.setOutcome("FAILURE");
        assertThat(invokePrivate("getSyslogSeverity", AuditEvent.class, ev)).isEqualTo(4);
    }

    @Test
    void getSyslogSeverity_highRisk_mapsToWarning4() throws Exception {
        AuditEvent ev = event(AuditEventType.AUTH_LOGIN_SUCCESS);
        ev.setOutcome("SUCCESS");
        ev.setRiskLevel("HIGH");
        assertThat(invokePrivate("getSyslogSeverity", AuditEvent.class, ev)).isEqualTo(4);
    }

    @Test
    void getSyslogSeverity_lowRisk_mapsToInformational6() throws Exception {
        AuditEvent ev = event(AuditEventType.AUTH_LOGIN_SUCCESS);
        ev.setOutcome("SUCCESS");
        ev.setRiskLevel("LOW");
        assertThat(invokePrivate("getSyslogSeverity", AuditEvent.class, ev)).isEqualTo(6);
    }

    // ---------- Status / testConnection ----------

    @Test
    void getStatus_reflectsConfigAndCounters() {
        SiemForwardingService.SiemStatus status = service.getStatus();
        assertThat(status.isEnabled()).isTrue();
        assertThat(status.getFormat()).isEqualTo("json");
        assertThat(status.getQueuedEvents()).isZero();
        // URL is masked, never returned verbatim — prevents credentials in logs.
        assertThat(status.getWebhookUrl()).doesNotContain("siem.example.com");
        assertThat(status.getWebhookUrl()).contains("configured");
    }

    @Test
    void getStatus_nullUrl_isReportedAsNull_notMaskedString() {
        config.setWebhookUrl(null);
        assertThat(service.getStatus().getWebhookUrl()).isNull();
    }

    @Test
    void testConnection_disabled_returnsClearMessage() {
        config.setEnabled(false);
        SiemForwardingService.TestResult r = service.testConnection();
        assertThat(r.isSuccess()).isFalse();
        assertThat(r.getMessage()).contains("disabled");
    }

    @Test
    void testConnection_emptyUrl_returnsConfigError() {
        config.setWebhookUrl("");
        SiemForwardingService.TestResult r = service.testConnection();
        assertThat(r.isSuccess()).isFalse();
        assertThat(r.getMessage()).contains("URL");
    }

    @Test
    void testConnection_nullUrl_returnsConfigError() {
        config.setWebhookUrl(null);
        SiemForwardingService.TestResult r = service.testConnection();
        assertThat(r.isSuccess()).isFalse();
        assertThat(r.getMessage()).contains("URL");
    }

    // ---------- helpers ----------

    private static AuditEvent event(AuditEventType type) {
        AuditEvent e = new AuditEvent();
        e.setEventType(type);
        e.setCategory(type.getCategory());
        e.setRiskLevel(type.getRiskLevel());
        e.setOutcome("SUCCESS");
        e.setTimestamp(LocalDateTime.of(2026, 1, 15, 12, 0, 0));
        return e;
    }

    private Object invokePrivate(String methodName, Class<?> argType, Object arg) throws Exception {
        Method m = SiemForwardingService.class.getDeclaredMethod(methodName, argType);
        m.setAccessible(true);
        return m.invoke(service, arg);
    }

    private Object invokePrivate(String methodName) throws Exception {
        Method m = SiemForwardingService.class.getDeclaredMethod(methodName);
        m.setAccessible(true);
        return m.invoke(service);
    }
}
