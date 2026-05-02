package gov.nist.oscal.tools.api.telemetry;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

@Aspect
@Component
public class TelemetryAspect {

    private final TelemetryService telemetryService;

    public TelemetryAspect(TelemetryService telemetryService) {
        this.telemetryService = telemetryService;
    }

    @Around("@annotation(gov.nist.oscal.tools.api.telemetry.Telemetry)")
    public Object aroundAnnotated(ProceedingJoinPoint pjp) throws Throwable {
        Method m = ((MethodSignature) pjp.getSignature()).getMethod();
        Telemetry ann = m.getAnnotation(Telemetry.class);
        return around(pjp, ann);
    }

    /** Package-private for the unit test. */
    Object around(ProceedingJoinPoint pjp, Telemetry ann) throws Throwable {
        String base = ann.value();
        telemetryService.emit(base + "_started", Map.of());
        long start = System.currentTimeMillis();
        try {
            Object result = pjp.proceed();
            Map<String, Object> attrs = new HashMap<>();
            attrs.put("duration_ms", System.currentTimeMillis() - start);
            attrs.put("outcome", "success");
            telemetryService.emit(base + "_completed", attrs);
            return result;
        } catch (Throwable t) {
            Map<String, Object> attrs = new HashMap<>();
            attrs.put("duration_ms", System.currentTimeMillis() - start);
            attrs.put("outcome", "failure");
            attrs.put("error_class", t.getClass().getSimpleName());
            attrs.put("error_message", t.getMessage());
            telemetryService.emit(base + "_completed", attrs);
            throw t;
        }
    }
}
