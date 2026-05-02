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

    @Override
    public FilterReply decide(Marker marker, Logger logger, Level level,
                              String format, Object[] params, Throwable t) {
        Baggage current = Baggage.current();
        current.forEach((key, entry) -> MDC.put(key, entry.getValue()));
        return FilterReply.NEUTRAL;
    }
}
