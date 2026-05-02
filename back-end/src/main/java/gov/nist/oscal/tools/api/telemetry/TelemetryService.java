package gov.nist.oscal.tools.api.telemetry;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.logs.Logger;
import io.opentelemetry.api.logs.LogRecordBuilder;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Emits OSCAL Hub business events. Each emit is sent via TWO paths:
 *   1. OTel Logs Bridge API (for environments where the SDK's logs API is fully active).
 *   2. SLF4J with MDC, picked up by the agent's Logback instrumentation and
 *      converted to OTel log records with event.name + payload attributes.
 *
 * The dual-path approach is defensive: in production we observed that the Logs
 * Bridge API path was silent even though the SDK was loaded; the SLF4J path is
 * reliably captured by the agent. Future cleanup (Phase 4): drop one path once
 * verified.
 */
@Service
public class TelemetryService {

    private static final org.slf4j.Logger slf4j = org.slf4j.LoggerFactory.getLogger("oscal-hub.events");
    private static final AttributeKey<String> EVENT_NAME_KEY = AttributeKey.stringKey("event.name");

    private final Logger otelLogger;

    @Autowired
    public TelemetryService(OpenTelemetry openTelemetry) {
        this.otelLogger = openTelemetry.getLogsBridge().get("oscal-hub.events");
    }

    public void emit(String eventName, Map<String, Object> attributes) {
        // Path 1: OTel Logs Bridge API
        try {
            LogRecordBuilder b = otelLogger.logRecordBuilder().setAttribute(EVENT_NAME_KEY, eventName);
            if (attributes != null) {
                for (Map.Entry<String, Object> e : attributes.entrySet()) {
                    if (e.getValue() == null) continue;
                    Object v = e.getValue();
                    if (v instanceof String s)          b.setAttribute(AttributeKey.stringKey(e.getKey()), s);
                    else if (v instanceof Long l)        b.setAttribute(AttributeKey.longKey(e.getKey()), l);
                    else if (v instanceof Integer i)     b.setAttribute(AttributeKey.longKey(e.getKey()), i.longValue());
                    else if (v instanceof Boolean bool)  b.setAttribute(AttributeKey.booleanKey(e.getKey()), bool);
                    else if (v instanceof Double d)      b.setAttribute(AttributeKey.doubleKey(e.getKey()), d);
                    else                                 b.setAttribute(AttributeKey.stringKey(e.getKey()), v.toString());
                }
            }
            b.emit();
        } catch (Exception ignored) {
            // Bridge path is best-effort; SLF4J path below is the reliable one.
        }

        // Path 2: SLF4J with MDC. The OTel agent's Logback instrumentation
        // auto-bridges this to an OTel log record. event.name + attributes
        // ride along as MDC entries.
        try {
            MDC.put("event.name", eventName);
            if (attributes != null) {
                for (Map.Entry<String, Object> e : attributes.entrySet()) {
                    if (e.getValue() == null) continue;
                    MDC.put(e.getKey(), String.valueOf(e.getValue()));
                }
            }
            slf4j.info("event {}", eventName);
        } finally {
            MDC.remove("event.name");
            if (attributes != null) {
                for (String k : attributes.keySet()) {
                    MDC.remove(k);
                }
            }
        }
    }
}
