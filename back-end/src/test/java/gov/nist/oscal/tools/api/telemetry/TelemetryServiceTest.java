package gov.nist.oscal.tools.api.telemetry;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.logs.Logger;
import io.opentelemetry.api.logs.LoggerProvider;
import io.opentelemetry.api.logs.LogRecordBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class TelemetryServiceTest {

    private OpenTelemetry openTelemetry;
    private Logger logger;
    private LogRecordBuilder builder;
    private TelemetryService service;

    @BeforeEach
    void setUp() {
        openTelemetry = mock(OpenTelemetry.class);
        LoggerProvider loggerProvider = mock(LoggerProvider.class);
        logger = mock(Logger.class);
        builder = mock(LogRecordBuilder.class);

        when(openTelemetry.getLogsBridge()).thenReturn(loggerProvider);
        when(loggerProvider.get("oscal-hub.events")).thenReturn(logger);
        when(logger.logRecordBuilder()).thenReturn(builder);
        when(builder.setAttribute(any(AttributeKey.class), any())).thenReturn(builder);

        service = new TelemetryService(openTelemetry);
    }

    @Test
    void emitSetsEventNameAndPayloadAttributes() {
        service.emit("oscal.validate_completed", Map.of(
                "model", "catalog",
                "format", "xml",
                "duration_ms", 123L,
                "outcome", "success"
        ));

        verify(builder).setAttribute(eq(AttributeKey.stringKey("event.name")), eq("oscal.validate_completed"));
        verify(builder).setAttribute(eq(AttributeKey.stringKey("model")), eq("catalog"));
        verify(builder).setAttribute(eq(AttributeKey.stringKey("format")), eq("xml"));
        verify(builder).setAttribute(eq(AttributeKey.longKey("duration_ms")), eq(123L));
        verify(builder).setAttribute(eq(AttributeKey.stringKey("outcome")), eq("success"));
        verify(builder).emit();
    }

    @Test
    void emitTolerantesNullAttributesPayload() {
        service.emit("auth.session_ended", null);

        verify(builder).setAttribute(eq(AttributeKey.stringKey("event.name")), eq("auth.session_ended"));
        verify(builder).emit();
    }

    @Test
    void emitWithNullAttributeValueIsSkipped() {
        Map<String, Object> attrs = new HashMap<>();
        attrs.put("present", "yes");
        attrs.put("absent", null);

        service.emit("test.event", attrs);

        verify(builder).setAttribute(eq(AttributeKey.stringKey("event.name")), eq("test.event"));
        verify(builder).setAttribute(eq(AttributeKey.stringKey("present")), eq("yes"));
        verify(builder, never()).setAttribute(eq(AttributeKey.stringKey("absent")), any());
        verify(builder).emit();
    }
}
