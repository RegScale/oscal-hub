package gov.nist.oscal.tools.api.telemetry;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.lang.reflect.Method;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class TelemetryAspectTest {

    private TelemetryService telemetryService;
    private TelemetryAspect aspect;
    private ProceedingJoinPoint joinPoint;
    private MethodSignature signature;

    @BeforeEach
    void setUp() throws Exception {
        telemetryService = mock(TelemetryService.class);
        aspect = new TelemetryAspect(telemetryService);
        joinPoint = mock(ProceedingJoinPoint.class);
        signature = mock(MethodSignature.class);
        Method m = SampleAnnotated.class.getDeclaredMethod("doStuff");
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getMethod()).thenReturn(m);
    }

    @Test
    void emitsStartedAndCompletedOnSuccess() throws Throwable {
        when(joinPoint.proceed()).thenReturn("ok");

        aspect.around(joinPoint, m().getAnnotation(Telemetry.class));

        verify(telemetryService).emit(eq("oscal.validate_started"), any());
        ArgumentCaptor<Map<String, Object>> attrs = ArgumentCaptor.forClass(Map.class);
        verify(telemetryService).emit(eq("oscal.validate_completed"), attrs.capture());
        Map<String, Object> a = attrs.getValue();
        assertEquals("success", a.get("outcome"));
        assertNotNull(a.get("duration_ms"));
    }

    @Test
    void emitsFailureWithExceptionDetails() throws Throwable {
        RuntimeException boom = new RuntimeException("kaboom");
        when(joinPoint.proceed()).thenThrow(boom);

        assertThrows(RuntimeException.class, () ->
                aspect.around(joinPoint, m().getAnnotation(Telemetry.class)));

        verify(telemetryService).emit(eq("oscal.validate_started"), any());
        ArgumentCaptor<Map<String, Object>> attrs = ArgumentCaptor.forClass(Map.class);
        verify(telemetryService).emit(eq("oscal.validate_completed"), attrs.capture());
        Map<String, Object> a = attrs.getValue();
        assertEquals("failure", a.get("outcome"));
        assertEquals("RuntimeException", a.get("error_class"));
        assertEquals("kaboom", a.get("error_message"));
    }

    private Method m() throws NoSuchMethodException {
        return SampleAnnotated.class.getDeclaredMethod("doStuff");
    }

    static class SampleAnnotated {
        @Telemetry("oscal.validate")
        String doStuff() { return "ok"; }
    }
}
