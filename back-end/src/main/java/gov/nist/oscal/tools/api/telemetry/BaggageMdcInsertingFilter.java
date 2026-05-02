package gov.nist.oscal.tools.api.telemetry;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.turbo.TurboFilter;
import ch.qos.logback.core.spi.FilterReply;
import io.opentelemetry.api.baggage.Baggage;
import org.slf4j.MDC;
import org.slf4j.Marker;

/**
 * Copies OTel baggage entries to SLF4J MDC at log emission time so encoders
 * (e.g. JSON encoder for Cloud Logging) can pick them up. Always returns
 * NEUTRAL — never alters whether the event is logged.
 */
public class BaggageMdcInsertingFilter extends TurboFilter {

    private static final java.util.List<String> MANAGED_KEYS =
        java.util.List.of("user.id", "org.id", "user.role.global", "user.role.org");

    @Override
    public FilterReply decide(Marker marker, Logger logger, Level level,
                              String format, Object[] params, Throwable t) {
        // Clear managed keys first so a thread recycled from an authenticated
        // request never leaks identity into an unauthenticated one.
        MANAGED_KEYS.forEach(MDC::remove);
        Baggage.current().forEach((key, entry) -> MDC.put(key, entry.getValue()));
        return FilterReply.NEUTRAL;
    }
}
