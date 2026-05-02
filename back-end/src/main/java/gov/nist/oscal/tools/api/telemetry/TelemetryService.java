package gov.nist.oscal.tools.api.telemetry;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.logs.Logger;
import io.opentelemetry.api.logs.LogRecordBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Emits OSCAL Hub business events as OTel log records. The OTel collector
 * routes records carrying the {@code event.name} attribute to Pub/Sub → BigQuery.
 *
 * <p>Baggage entries (user.id, org.id, etc.) are attached automatically by
 * the agent's logback instrumentation; this service does not attach them.
 */
@Service
public class TelemetryService {

    private static final AttributeKey<String> EVENT_NAME_KEY = AttributeKey.stringKey("event.name");
    private final Logger logger;

    @Autowired
    public TelemetryService(OpenTelemetry openTelemetry) {
        this.logger = openTelemetry.getLogsBridge().get("oscal-hub.events");
    }

    public void emit(String eventName, Map<String, Object> attributes) {
        LogRecordBuilder b = logger.logRecordBuilder()
                .setAttribute(EVENT_NAME_KEY, eventName);

        if (attributes != null) {
            for (Map.Entry<String, Object> e : attributes.entrySet()) {
                if (e.getValue() == null) continue;
                Object v = e.getValue();
                if (v instanceof String s) {
                    b.setAttribute(AttributeKey.stringKey(e.getKey()), s);
                } else if (v instanceof Long l) {
                    b.setAttribute(AttributeKey.longKey(e.getKey()), l);
                } else if (v instanceof Integer i) {
                    b.setAttribute(AttributeKey.longKey(e.getKey()), i.longValue());
                } else if (v instanceof Boolean bool) {
                    b.setAttribute(AttributeKey.booleanKey(e.getKey()), bool);
                } else if (v instanceof Double d) {
                    b.setAttribute(AttributeKey.doubleKey(e.getKey()), d);
                } else {
                    b.setAttribute(AttributeKey.stringKey(e.getKey()), v.toString());
                }
            }
        }

        b.emit();
    }
}
