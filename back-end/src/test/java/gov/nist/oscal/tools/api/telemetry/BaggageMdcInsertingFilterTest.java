package gov.nist.oscal.tools.api.telemetry;

import ch.qos.logback.classic.Level;
import ch.qos.logback.core.spi.FilterReply;
import io.opentelemetry.api.baggage.Baggage;
import io.opentelemetry.context.Scope;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class BaggageMdcInsertingFilterTest {

    @Test
    void copiesBaggageEntriesToMdcDuringDecide() {
        BaggageMdcInsertingFilter filter = new BaggageMdcInsertingFilter();
        Baggage baggage = Baggage.empty().toBuilder()
                .put("user.id", "456")
                .put("org.id", "123")
                .put("user.role.global", "USER")
                .put("user.role.org", "ORG_ADMIN")
                .build();

        try (Scope scope = baggage.makeCurrent()) {
            FilterReply reply = filter.decide(null, null, Level.INFO, "test", null, null);
            assertEquals(FilterReply.NEUTRAL, reply);
            assertEquals("456", MDC.get("user.id"));
            assertEquals("123", MDC.get("org.id"));
            assertEquals("USER", MDC.get("user.role.global"));
            assertEquals("ORG_ADMIN", MDC.get("user.role.org"));
        } finally {
            MDC.clear();
        }
    }

    @Test
    void clearsManagedMdcKeysWhenBaggageIsEmpty() {
        // Simulate a dirty thread recycled from a prior authenticated request.
        MDC.put("user.id", "stale-456");
        MDC.put("org.id", "stale-123");
        MDC.put("user.role.global", "stale-USER");
        MDC.put("user.role.org", "stale-ORG_ADMIN");
        try {
            BaggageMdcInsertingFilter filter = new BaggageMdcInsertingFilter();
            FilterReply reply = filter.decide(null, null, Level.INFO, "test", null, null);
            assertEquals(FilterReply.NEUTRAL, reply);
            assertNull(MDC.get("user.id"));
            assertNull(MDC.get("org.id"));
            assertNull(MDC.get("user.role.global"));
            assertNull(MDC.get("user.role.org"));
        } finally {
            MDC.clear();
        }
    }
}
