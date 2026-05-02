# OpenTelemetry — Phase 2 (Business Events + BigQuery + CS Dashboard) — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Land Phase 2 of the OpenTelemetry design — emit a curated set of business events from the backend, route them to BigQuery via Pub/Sub, sync user/org dimension tables hourly from Postgres, and stand up a Customer Success dashboard in Looker Studio. After this phase, CS reps can answer "who is using what feature, how often, and is anyone churning" without ad-hoc SQL.

**Architecture:** Add a `TelemetryService` and `@Telemetry` AOP annotation in the backend that emit OTel log records carrying an `event.name` attribute. The Phase 1 collector's existing routing processor (already configured) sends these to Cloud Logging. We add a second routing target: a `googlecloudpubsub` exporter that publishes the same events to a `otel-events` Pub/Sub topic. Pub/Sub's built-in BigQuery subscription writes them to `analytics.events`. A new Cloud Run Job mirrors the `users` and `organizations` tables from Postgres into BigQuery hourly so Looker Studio can join on opaque IDs. Looker Studio queries enriched views to power the four-page CS report.

**Tech Stack:**
- Java 21 / Spring Boot 3.4.2, Maven, JUnit 5, Mockito, Spring AOP
- OpenTelemetry Java agent 2.27.0 (already attached, Phase 1)
- Google Cloud Pub/Sub Java SDK (via existing `google-cloud-bom`)
- Google Cloud BigQuery Java SDK (same BOM)
- Terraform 1.5+ with `hashicorp/google` 5.x

---

## Pre-flight check

Phase 1 must be merged and live before starting Phase 2. Verify:

- `oscal-tools-prod` is on a revision with the OTel agent attached (`gcloud run services describe oscal-tools-prod --region=us-central1 --format="value(spec.template.spec.containers[0].env[?name='JAVA_TOOL_OPTIONS'].value)"` returns the `-javaagent:...` line).
- `otel-collector-prod` is healthy and exporting to Cloud Trace.
- The `feature/otel-phase-1` PR is merged to `main`.

If any of these are not true, finish Phase 1 first.

---

## File map

### New files

| Path | Responsibility |
|---|---|
| `back-end/src/main/java/.../telemetry/TelemetryService.java` | Service that emits OTel `Log` records with an `event.name` attribute and the current baggage. |
| `back-end/src/main/java/.../telemetry/Telemetry.java` | `@Telemetry("event.name")` annotation. |
| `back-end/src/main/java/.../telemetry/TelemetryAspect.java` | Spring AOP aspect that wraps `@Telemetry`-annotated methods with `<name>_started` / `<name>_completed` emissions. |
| `back-end/src/main/java/.../telemetry/EventNames.java` | Centralized constants for every emitted event name (single registry). |
| `back-end/src/main/java/.../telemetry/DimensionSyncJob.java` | Hourly job that reads `users`/`organizations` from Postgres and MERGEs them into BigQuery dimension tables. |
| `back-end/src/main/java/.../telemetry/DimensionSyncRunner.java` | Spring `@Component` `CommandLineRunner` that executes `DimensionSyncJob` and exits — used as the Cloud Run Job entrypoint. |
| `back-end/src/main/resources/application-dimsync.properties` | Properties file selecting `dimsync` profile when running the dimension sync job. |
| `back-end/src/test/java/.../telemetry/TelemetryServiceTest.java` | Unit test for event emission + baggage merging. |
| `back-end/src/test/java/.../telemetry/TelemetryAspectTest.java` | Unit test for the started/completed pair on a sample method. |
| `back-end/src/test/java/.../telemetry/DimensionSyncJobTest.java` | Unit test using a mocked BigQuery client and an in-memory Postgres dataset. |
| `terraform/gcp/modules/analytics-pubsub/main.tf` | Pub/Sub topic + DLQ + BigQuery subscription module. |
| `terraform/gcp/modules/analytics-pubsub/variables.tf` | Module inputs. |
| `terraform/gcp/modules/analytics-pubsub/outputs.tf` | Topic name. |
| `terraform/gcp/modules/analytics-bigquery/main.tf` | BigQuery dataset, `events` table, `users`/`orgs` dimension tables, four views. |
| `terraform/gcp/modules/analytics-bigquery/variables.tf` | Module inputs. |
| `terraform/gcp/modules/analytics-bigquery/outputs.tf` | Dataset id, table refs. |
| `terraform/gcp/modules/dimsync-job/main.tf` | Cloud Run Job + dedicated GSA + Cloud Scheduler hourly trigger. |
| `terraform/gcp/modules/dimsync-job/variables.tf` | Module inputs. |
| `terraform/gcp/modules/dimsync-job/outputs.tf` | Job name, GSA email. |
| `terraform/gcp/analytics.tf` | Wires the three new modules into the root config. |
| `terraform/gcp/dashboards/cs-dashboard.json` | (Optional Cloud Monitoring placeholder; the real CS dashboard lives in Looker Studio — but a small "ops view of CS pipeline" Cloud Monitoring dashboard tracking Pub/Sub backlog age and BQ insert errors is useful here.) |
| `terraform/gcp/dashboards/cs-pipeline-dashboard.json` | Pipeline-health dashboard for CS data freshness (Pub/Sub backlog, BQ insert errors). |
| `docs/runbooks/cs-dashboard-setup.md` | Looker Studio setup runbook (hand-rolled, but documented step-by-step). |

### Modified files

| Path | Change |
|---|---|
| `back-end/pom.xml` | Add `google-cloud-bigquery` dependency (already governed by `google-cloud-bom`). |
| `back-end/src/main/java/.../config/SecurityConfig.java` | Permit the `dimsync` Spring profile to skip JPA/web setup if not needed. |
| `back-end/src/main/java/.../controller/AuthController.java` | Add `TelemetryService.emit(EventNames.AUTH_LOGIN_SUCCEEDED, …)` and `…_FAILED` paths. |
| `back-end/src/main/java/.../controller/ValidationController.java` | Add `@Telemetry(EventNames.OSCAL_VALIDATE)` on the validate endpoint. |
| `back-end/src/main/java/.../controller/ConversionController.java` | Add `@Telemetry(EventNames.OSCAL_CONVERT)`. |
| `back-end/src/main/java/.../controller/ProfileController.java` | Add `@Telemetry(EventNames.OSCAL_RESOLVE)`. |
| `back-end/src/main/java/.../controller/LibraryController.java` | Emit `library.item_uploaded` / `_downloaded` / `_deleted` events at the appropriate handlers. |
| `back-end/src/main/java/.../controller/AuthorizationController.java` | Emit `authorization.template_*` events. |
| `back-end/src/main/java/.../controller/ArtifactController.java` | Emit `artifact.uploaded` / `_downloaded`. |
| `terraform/gcp/otel-collector-config/otel-config.yaml` | Add `googlecloudpubsub` exporter and route `event.name`-attributed log records to it instead of Cloud Logging. |
| `terraform/gcp/modules/otel-collector/main.tf` | Add `roles/pubsub.publisher` IAM binding to the collector GSA. |
| `terraform/gcp/main.tf` | Add module calls for `analytics-pubsub`, `analytics-bigquery`, `dimsync-job`. |
| `Dockerfile` | (No change. The dimsync job uses the same image — switched into the right Spring profile by `SPRING_PROFILES_ACTIVE=dimsync` env var on the Cloud Run Job spec.) |

---

## Branch and PR strategy

Work on a fresh branch off `main` (assuming Phase 1 is merged):

```bash
git checkout main && git pull && git checkout -b feature/otel-phase-2
```

Commit per task. Final PR is a single squash-merge to `main` with the
`OTEL_ENABLED=true` flag already set (Phase 1 enabled it). Phase 2 doesn't
introduce a new feature flag — it adds new infra (Pub/Sub, BigQuery, dimsync
job) and new event emissions. The events are written regardless of any flag;
if the Pub/Sub topic doesn't yet exist, the collector buffers and drops with
a warning (acceptable during initial deploy).

---

## Task 1: Event-name registry

**Files:**
- Create: `back-end/src/main/java/gov/nist/oscal/tools/api/telemetry/EventNames.java`

A single class of `public static final String` constants. Centralizing them
prevents typo drift (e.g., `oscal.validate_complete` vs `oscal.validate_completed`)
and makes the catalog auditable in one place.

- [ ] **Step 1.1: Create the constants file**

```java
package gov.nist.oscal.tools.api.telemetry;

public final class EventNames {
    private EventNames() {}

    public static final String AUTH_LOGIN_SUCCEEDED       = "auth.login_succeeded";
    public static final String AUTH_LOGIN_FAILED          = "auth.login_failed";
    public static final String AUTH_SESSION_STARTED       = "auth.session_started";
    public static final String AUTH_SESSION_ENDED         = "auth.session_ended";

    public static final String FEATURE_VIEWED             = "feature.viewed";

    public static final String OSCAL_VALIDATE             = "oscal.validate";
    public static final String OSCAL_CONVERT              = "oscal.convert";
    public static final String OSCAL_RESOLVE              = "oscal.resolve";
    public static final String OSCAL_BATCH_SUBMITTED      = "oscal.batch_submitted";
    public static final String OSCAL_BATCH_COMPLETED      = "oscal.batch_completed";

    public static final String LIBRARY_ITEM_UPLOADED      = "library.item_uploaded";
    public static final String LIBRARY_ITEM_DOWNLOADED    = "library.item_downloaded";
    public static final String LIBRARY_ITEM_DELETED       = "library.item_deleted";

    public static final String AUTHORIZATION_CREATED      = "authorization.template_created";
    public static final String AUTHORIZATION_APPROVED     = "authorization.template_approved";
    public static final String AUTHORIZATION_REJECTED     = "authorization.template_rejected";

    public static final String ARTIFACT_UPLOADED          = "artifact.uploaded";
    public static final String ARTIFACT_DOWNLOADED        = "artifact.downloaded";

    public static final String ADMIN_USER_INVITED         = "admin.user_invited";
    public static final String ADMIN_USER_ROLE_CHANGED    = "admin.user_role_changed";
    public static final String ADMIN_USER_DEACTIVATED     = "admin.user_deactivated";

    public static final String ERROR_UNHANDLED            = "error.unhandled";
    public static final String ERROR_FRONTEND_JS          = "error.frontend_js";
}
```

- [ ] **Step 1.2: Compile**

```bash
mvn -pl back-end -am -DskipTests compile
```

Expected: BUILD SUCCESS.

- [ ] **Step 1.3: Commit**

```bash
git add back-end/src/main/java/gov/nist/oscal/tools/api/telemetry/EventNames.java
git commit -m "feat(otel): add EventNames registry for Phase 2 business events"
```

---

## Task 2: TelemetryService (TDD)

**Files:**
- Create: `back-end/src/main/java/.../telemetry/TelemetryService.java`
- Create: `back-end/src/test/java/.../telemetry/TelemetryServiceTest.java`

Emits an OTel log record with `event.name` set, a serializable attribute payload,
and the current baggage (`user.id`, `org.id`, etc., automatically attached
because logs flow through OTLP and the collector promotes baggage already).

- [ ] **Step 2.1: Write failing tests**

Create `TelemetryServiceTest.java`:

```java
package gov.nist.oscal.tools.api.telemetry;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.logs.Logger;
import io.opentelemetry.api.logs.LoggerProvider;
import io.opentelemetry.api.logs.LogRecordBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
        when(builder.setAllAttributes(any())).thenReturn(builder);

        service = new TelemetryService(openTelemetry);
    }

    @Test
    void emitSetsEventNameAndPayloadAttributes() {
        service.emit(EventNames.OSCAL_VALIDATE + "_completed", Map.of(
                "model", "catalog",
                "format", "xml",
                "duration_ms", 123L,
                "outcome", "success"
        ));

        verify(builder).setAttribute(eq(AttributeKey.stringKey("event.name")),
                eq("oscal.validate_completed"));
        verify(builder).setAttribute(eq(AttributeKey.stringKey("model")), eq("catalog"));
        verify(builder).setAttribute(eq(AttributeKey.stringKey("format")), eq("xml"));
        verify(builder).setAttribute(eq(AttributeKey.longKey("duration_ms")), eq(123L));
        verify(builder).setAttribute(eq(AttributeKey.stringKey("outcome")), eq("success"));
        verify(builder).emit();
    }

    @Test
    void emitTolerantesNullAttributesPayload() {
        service.emit("auth.session_ended", null);

        verify(builder).setAttribute(eq(AttributeKey.stringKey("event.name")),
                eq("auth.session_ended"));
        verify(builder).emit();
    }

    @Test
    void emitWithNullAttributeValueIsSkipped() {
        // Don't crash when a value happens to be null; just skip that key.
        java.util.HashMap<String, Object> attrs = new java.util.HashMap<>();
        attrs.put("present", "yes");
        attrs.put("absent", null);

        service.emit("test.event", attrs);

        verify(builder).setAttribute(eq(AttributeKey.stringKey("event.name")), eq("test.event"));
        verify(builder).setAttribute(eq(AttributeKey.stringKey("present")), eq("yes"));
        verify(builder, never()).setAttribute(eq(AttributeKey.stringKey("absent")), any());
        verify(builder).emit();
    }
}
```

- [ ] **Step 2.2: Run test, confirm fails to compile**

```bash
mvn -pl back-end test -Dtest=TelemetryServiceTest
```
Expected: compile error — `cannot find symbol class TelemetryService`.

- [ ] **Step 2.3: Implement `TelemetryService`**

```java
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
 * routes records carrying the `event.name` attribute to Pub/Sub → BigQuery
 * (configured in Phase 2's collector pipeline).
 *
 * Baggage entries (`user.id`, `org.id`, etc.) are attached automatically by
 * the agent's logback instrumentation; we don't need to copy them here.
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
```

You will also need to register the `OpenTelemetry` bean. The OTel Java agent
auto-registers a global `OpenTelemetry`; expose it as a Spring bean by adding
this to `TelemetryConfig.java` (new file) in the same package:

```java
package gov.nist.oscal.tools.api.telemetry;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.OpenTelemetry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TelemetryConfig {
    @Bean
    public OpenTelemetry openTelemetry() {
        return GlobalOpenTelemetry.get();
    }
}
```

- [ ] **Step 2.4: Run tests, confirm pass**

```bash
mvn -pl back-end test -Dtest=TelemetryServiceTest
```
Expected: 3 tests pass.

- [ ] **Step 2.5: Commit**

```bash
git add back-end/src/main/java/gov/nist/oscal/tools/api/telemetry/TelemetryService.java \
        back-end/src/main/java/gov/nist/oscal/tools/api/telemetry/TelemetryConfig.java \
        back-end/src/test/java/gov/nist/oscal/tools/api/telemetry/TelemetryServiceTest.java
git commit -m "feat(otel): TelemetryService for emitting OTel log-event records"
```

---

## Task 3: `@Telemetry` annotation and AOP aspect (TDD)

**Files:**
- Create: `back-end/src/main/java/.../telemetry/Telemetry.java`
- Create: `back-end/src/main/java/.../telemetry/TelemetryAspect.java`
- Create: `back-end/src/test/java/.../telemetry/TelemetryAspectTest.java`

The aspect wraps `@Telemetry("oscal.validate")` methods and emits
`<name>_started` and `<name>_completed` events with `duration_ms` and
`outcome=success|failure`. Failure path also emits `error_class` and
`error_message`. The annotation gives controllers a one-line way to
auto-instrument an endpoint.

- [ ] **Step 3.1: Write failing tests**

Create `TelemetryAspectTest.java`:

```java
package gov.nist.oscal.tools.api.telemetry;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.lang.reflect.Method;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
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

        Object result = aspect.around(joinPoint, m().getAnnotation(Telemetry.class));

        // started
        verify(telemetryService).emit(eq("oscal.validate_started"), any());
        // completed with success outcome
        ArgumentCaptor<Map<String, Object>> attrs = ArgumentCaptor.forClass(Map.class);
        verify(telemetryService).emit(eq("oscal.validate_completed"), attrs.capture());
        Map<String, Object> a = attrs.getValue();
        org.junit.jupiter.api.Assertions.assertEquals("success", a.get("outcome"));
        org.junit.jupiter.api.Assertions.assertNotNull(a.get("duration_ms"));
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
        org.junit.jupiter.api.Assertions.assertEquals("failure", a.get("outcome"));
        org.junit.jupiter.api.Assertions.assertEquals("RuntimeException", a.get("error_class"));
        org.junit.jupiter.api.Assertions.assertEquals("kaboom", a.get("error_message"));
    }

    private Method m() throws NoSuchMethodException {
        return SampleAnnotated.class.getDeclaredMethod("doStuff");
    }

    static class SampleAnnotated {
        @Telemetry("oscal.validate")
        String doStuff() { return "ok"; }
    }
}
```

- [ ] **Step 3.2: Confirm tests fail to compile**

```bash
mvn -pl back-end test -Dtest=TelemetryAspectTest
```
Expected: compile errors for `Telemetry` and `TelemetryAspect`.

- [ ] **Step 3.3: Create the annotation**

`back-end/src/main/java/gov/nist/oscal/tools/api/telemetry/Telemetry.java`:

```java
package gov.nist.oscal.tools.api.telemetry;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Telemetry {
    /** Event name root, e.g. "oscal.validate". The aspect appends "_started" / "_completed". */
    String value();
}
```

- [ ] **Step 3.4: Implement the aspect**

`back-end/src/main/java/gov/nist/oscal/tools/api/telemetry/TelemetryAspect.java`:

```java
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

    /** Package-private for the unit test, which does not run AspectJ weaving. */
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
```

Spring AOP needs a starter:

```xml
<!-- in back-end/pom.xml dependencies (if not already present) -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-aop</artifactId>
</dependency>
```

(Spring Boot Web already pulls AOP transitively in most builds — verify with
`mvn -pl back-end dependency:tree | grep aop` and only add if missing.)

- [ ] **Step 3.5: Run tests, confirm pass**

```bash
mvn -pl back-end test -Dtest=TelemetryAspectTest
```

Expected: both tests pass.

- [ ] **Step 3.6: Commit**

```bash
git add back-end/src/main/java/gov/nist/oscal/tools/api/telemetry/Telemetry.java \
        back-end/src/main/java/gov/nist/oscal/tools/api/telemetry/TelemetryAspect.java \
        back-end/src/test/java/gov/nist/oscal/tools/api/telemetry/TelemetryAspectTest.java \
        back-end/pom.xml
git commit -m "feat(otel): @Telemetry annotation + AOP aspect for auto-emitting started/completed events"
```

---

## Task 4: Annotate primary controllers

**Files:**
- Modify: `controller/AuthController.java` (manual `emit` calls — login is non-trivial)
- Modify: `controller/ValidationController.java`
- Modify: `controller/ConversionController.java`
- Modify: `controller/ProfileController.java`
- Modify: `controller/LibraryController.java`
- Modify: `controller/AuthorizationController.java`
- Modify: `controller/ArtifactController.java`

For controllers handling validate/convert/resolve, just add `@Telemetry(EventNames.OSCAL_VALIDATE)`
etc. to the public method.

For login, the `_started`/`_completed` model doesn't fit (login outcomes are
either succeeded or failed and we want different attribute payloads); use
`telemetryService.emit(...)` directly inside the success and failure branches.

- [ ] **Step 4.1: Annotate `ValidationController.validate(...)`**

Add at the top of the controller:
```java
import gov.nist.oscal.tools.api.telemetry.Telemetry;
import gov.nist.oscal.tools.api.telemetry.EventNames;
```

On the public validate method:
```java
@Telemetry(EventNames.OSCAL_VALIDATE)
public ResponseEntity<ValidationResult> validate(...) { /* existing body */ }
```

- [ ] **Step 4.2: Annotate `ConversionController.convert(...)`**

Same pattern with `EventNames.OSCAL_CONVERT`.

- [ ] **Step 4.3: Annotate `ProfileController.resolve(...)`**

Same pattern with `EventNames.OSCAL_RESOLVE`.

- [ ] **Step 4.4: Wire `AuthController` login emission**

In the existing login method, find the success branch (after generating the
JWT but before returning) and add:

```java
telemetryService.emit(EventNames.AUTH_LOGIN_SUCCEEDED, Map.of(
        "user_id", String.valueOf(user.getId()),
        "org_id", String.valueOf(user.getOrganizationId()),
        "mfa_used", Boolean.toString(usedMfa)
));
```

In the failure branch (e.g., the catch for `BadCredentialsException`):

```java
telemetryService.emit(EventNames.AUTH_LOGIN_FAILED, Map.of(
        "attempted_username_sha256", sha256(loginRequest.getUsername()),
        "reason", "bad_credentials"
));
```

Inject `TelemetryService` via constructor; add a small helper:
```java
private static String sha256(String s) {
    try {
        java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
        byte[] h = md.digest(s.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        for (byte b : h) sb.append(String.format("%02x", b));
        return sb.toString();
    } catch (java.security.NoSuchAlgorithmException e) {
        return "sha256-error";
    }
}
```

- [ ] **Step 4.5: Wire `LibraryController`, `AuthorizationController`, `ArtifactController`**

For each controller, identify the handlers per the event catalog:
- `LibraryController.upload(...)` → `EventNames.LIBRARY_ITEM_UPLOADED`
- `LibraryController.download(...)` → `EventNames.LIBRARY_ITEM_DOWNLOADED`
- `LibraryController.delete(...)` → `EventNames.LIBRARY_ITEM_DELETED`
- `AuthorizationController.create(...)` → `EventNames.AUTHORIZATION_CREATED`
- `AuthorizationController.approve(...)` → `EventNames.AUTHORIZATION_APPROVED`
- `AuthorizationController.reject(...)` → `EventNames.AUTHORIZATION_REJECTED`
- `ArtifactController.upload(...)` → `EventNames.ARTIFACT_UPLOADED`
- `ArtifactController.download(...)` → `EventNames.ARTIFACT_DOWNLOADED`

For these, prefer `telemetryService.emit(...)` with explicit attribute payloads
(item kind, bytes, etc.) rather than the `@Telemetry` annotation, because the
attribute payloads are specific to each handler.

- [ ] **Step 4.6: Run the full backend test suite**

```bash
mvn -pl back-end test
```

Expected: same pre-existing failure count. No NEW failures.

- [ ] **Step 4.7: Commit**

```bash
git add back-end/src/main/java/gov/nist/oscal/tools/api/controller/
git commit -m "feat(otel): emit business events from primary controllers (validate/convert/resolve/auth/library/authorization/artifact)"
```

---

## Task 5: Pub/Sub topic + DLQ + BigQuery subscription

**Files:**
- Create: `terraform/gcp/modules/analytics-pubsub/main.tf`
- Create: `terraform/gcp/modules/analytics-pubsub/variables.tf`
- Create: `terraform/gcp/modules/analytics-pubsub/outputs.tf`

- [ ] **Step 5.1: Author the module**

`variables.tf`:
```hcl
variable "project_id"       { type = string }
variable "environment"      { type = string }
variable "bigquery_table"   { type = string }
variable "publisher_sa"     { type = string }
```

`main.tf`:
```hcl
resource "google_pubsub_topic" "events" {
  project = var.project_id
  name    = "otel-events-${var.environment}"
}

resource "google_pubsub_topic" "events_dlq" {
  project = var.project_id
  name    = "otel-events-dlq-${var.environment}"
}

resource "google_pubsub_subscription" "events_to_bq" {
  project = var.project_id
  name    = "otel-events-bq-${var.environment}"
  topic   = google_pubsub_topic.events.id

  bigquery_config {
    table = var.bigquery_table
    use_table_schema = true
    write_metadata = true
  }

  ack_deadline_seconds = 60

  dead_letter_policy {
    dead_letter_topic     = google_pubsub_topic.events_dlq.id
    max_delivery_attempts = 5
  }
}

resource "google_pubsub_topic_iam_member" "publisher" {
  project = var.project_id
  topic   = google_pubsub_topic.events.name
  role    = "roles/pubsub.publisher"
  member  = "serviceAccount:${var.publisher_sa}"
}

# BQ subscription needs the BigQuery Data Editor role on its own service
# account (Pub/Sub creates and uses a project-default Pub/Sub SA).
data "google_project" "current" {
  project_id = var.project_id
}

resource "google_project_iam_member" "pubsub_to_bq" {
  project = var.project_id
  role    = "roles/bigquery.dataEditor"
  member  = "serviceAccount:service-${data.google_project.current.number}@gcp-sa-pubsub.iam.gserviceaccount.com"
}
```

`outputs.tf`:
```hcl
output "topic_id"   { value = google_pubsub_topic.events.id }
output "topic_name" { value = google_pubsub_topic.events.name }
```

- [ ] **Step 5.2: Validate**

```bash
cd terraform/gcp/modules/analytics-pubsub && terraform init -backend=false && terraform validate
```

- [ ] **Step 5.3: Commit**

```bash
git add terraform/gcp/modules/analytics-pubsub/
git commit -m "feat(otel): Terraform module for analytics Pub/Sub topic + BQ subscription"
```

---

## Task 6: BigQuery dataset, tables, views

**Files:**
- Create: `terraform/gcp/modules/analytics-bigquery/main.tf`
- Create: `terraform/gcp/modules/analytics-bigquery/variables.tf`
- Create: `terraform/gcp/modules/analytics-bigquery/outputs.tf`

- [ ] **Step 6.1: Author the module**

`variables.tf`:
```hcl
variable "project_id"  { type = string }
variable "environment" { type = string }
variable "region"      { type = string }
variable "kms_key_id"  { type = string, default = null }
variable "events_partition_expiration_days" {
  type    = number
  default = 400
}
```

`main.tf`:
```hcl
resource "google_bigquery_dataset" "analytics" {
  project       = var.project_id
  dataset_id    = "analytics_${var.environment}"
  location      = var.region
  friendly_name = "OSCAL Hub analytics (${var.environment})"
  description   = "Phase 2: events fact + users/orgs dimensions for CS dashboards."
  default_partition_expiration_ms = var.events_partition_expiration_days * 24 * 3600 * 1000
}

resource "google_bigquery_table" "events" {
  project              = var.project_id
  dataset_id           = google_bigquery_dataset.analytics.dataset_id
  table_id             = "events"
  deletion_protection  = true

  time_partitioning {
    type          = "DAY"
    field         = "event_time"
    expiration_ms = var.events_partition_expiration_days * 24 * 3600 * 1000
  }

  clustering = ["org_id", "event_name"]

  schema = jsonencode([
    { name = "event_time",      type = "TIMESTAMP", mode = "REQUIRED" },
    { name = "event_name",      type = "STRING",    mode = "REQUIRED" },
    { name = "event_id",        type = "STRING",    mode = "REQUIRED" },
    { name = "trace_id",        type = "STRING",    mode = "NULLABLE" },
    { name = "span_id",         type = "STRING",    mode = "NULLABLE" },
    { name = "session_id",      type = "STRING",    mode = "NULLABLE" },
    { name = "user_id",         type = "STRING",    mode = "NULLABLE" },
    { name = "org_id",          type = "STRING",    mode = "NULLABLE" },
    { name = "service_name",    type = "STRING",    mode = "NULLABLE" },
    { name = "service_version", type = "STRING",    mode = "NULLABLE" },
    { name = "environment",     type = "STRING",    mode = "NULLABLE" },
    { name = "attributes",      type = "JSON",      mode = "NULLABLE" },
    { name = "ingested_at",     type = "TIMESTAMP", mode = "REQUIRED" }
  ])
}

resource "google_bigquery_table" "users" {
  project    = var.project_id
  dataset_id = google_bigquery_dataset.analytics.dataset_id
  table_id   = "users"
  deletion_protection = true

  schema = jsonencode([
    { name = "user_id",        type = "STRING",    mode = "REQUIRED" },
    { name = "username",       type = "STRING" },
    { name = "email",          type = "STRING" },
    { name = "first_name",     type = "STRING" },
    { name = "last_name",      type = "STRING" },
    { name = "org_id_primary", type = "STRING" },
    { name = "roles_global",   type = "STRING",    mode = "REPEATED" },
    { name = "created_at",     type = "TIMESTAMP" },
    { name = "last_login",     type = "TIMESTAMP" },
    { name = "active",         type = "BOOL" },
    { name = "synced_at",      type = "TIMESTAMP", mode = "REQUIRED" }
  ])
}

resource "google_bigquery_table" "orgs" {
  project    = var.project_id
  dataset_id = google_bigquery_dataset.analytics.dataset_id
  table_id   = "orgs"
  deletion_protection = true

  schema = jsonencode([
    { name = "org_id",       type = "STRING",    mode = "REQUIRED" },
    { name = "name",         type = "STRING" },
    { name = "description",  type = "STRING" },
    { name = "active",       type = "BOOL" },
    { name = "member_count", type = "INT64" },
    { name = "created_at",   type = "TIMESTAMP" },
    { name = "synced_at",    type = "TIMESTAMP", mode = "REQUIRED" }
  ])
}

resource "google_bigquery_table" "vw_events_enriched" {
  project    = var.project_id
  dataset_id = google_bigquery_dataset.analytics.dataset_id
  table_id   = "vw_events_enriched"
  deletion_protection = true

  view {
    query = <<EOT
SELECT
  e.*,
  u.email      AS user_email,
  u.first_name AS user_first_name,
  u.last_name  AS user_last_name,
  o.name       AS org_name
FROM `${var.project_id}.${google_bigquery_dataset.analytics.dataset_id}.events` e
LEFT JOIN `${var.project_id}.${google_bigquery_dataset.analytics.dataset_id}.users` u
  USING (user_id)
LEFT JOIN `${var.project_id}.${google_bigquery_dataset.analytics.dataset_id}.orgs` o
  USING (org_id)
EOT
    use_legacy_sql = false
  }
}

resource "google_bigquery_table" "vw_daily_active_orgs" {
  project    = var.project_id
  dataset_id = google_bigquery_dataset.analytics.dataset_id
  table_id   = "vw_daily_active_orgs"
  deletion_protection = true

  view {
    query = <<EOT
SELECT
  DATE(event_time) AS day,
  COUNT(DISTINCT org_id) AS active_orgs,
  COUNT(DISTINCT user_id) AS active_users,
  COUNT(*) AS event_count
FROM `${var.project_id}.${google_bigquery_dataset.analytics.dataset_id}.events`
WHERE org_id IS NOT NULL
GROUP BY day
EOT
    use_legacy_sql = false
  }
}

resource "google_bigquery_table" "vw_feature_popularity" {
  project    = var.project_id
  dataset_id = google_bigquery_dataset.analytics.dataset_id
  table_id   = "vw_feature_popularity"
  deletion_protection = true

  view {
    query = <<EOT
SELECT
  event_name,
  COUNT(*)                                   AS total_events,
  COUNT(DISTINCT user_id)                    AS distinct_users,
  COUNT(DISTINCT org_id)                     AS distinct_orgs,
  APPROX_QUANTILES(SAFE_CAST(JSON_VALUE(attributes, '$.duration_ms') AS INT64), 100)[OFFSET(50)] AS p50_duration_ms,
  APPROX_QUANTILES(SAFE_CAST(JSON_VALUE(attributes, '$.duration_ms') AS INT64), 100)[OFFSET(95)] AS p95_duration_ms,
  COUNTIF(JSON_VALUE(attributes, '$.outcome') = 'success') / NULLIF(COUNT(*), 0) AS success_rate
FROM `${var.project_id}.${google_bigquery_dataset.analytics.dataset_id}.events`
WHERE event_name LIKE '%_completed'
GROUP BY event_name
EOT
    use_legacy_sql = false
  }
}
```

`outputs.tf`:
```hcl
output "dataset_id"   { value = google_bigquery_dataset.analytics.dataset_id }
output "events_table" { value = "${var.project_id}:${google_bigquery_dataset.analytics.dataset_id}.events" }
```

- [ ] **Step 6.2: Validate**

```bash
cd terraform/gcp/modules/analytics-bigquery && terraform init -backend=false && terraform validate
```

- [ ] **Step 6.3: Commit**

```bash
git add terraform/gcp/modules/analytics-bigquery/
git commit -m "feat(otel): BigQuery dataset, fact + dim tables, four enriched views"
```

---

## Task 7: Add `googlecloudpubsub` exporter to collector pipeline

**Files:**
- Modify: `terraform/gcp/otel-collector-config/otel-config.yaml`
- Modify: `terraform/gcp/modules/otel-collector/main.tf` (IAM)

- [ ] **Step 7.1: Add exporter and update logs pipeline**

In `otel-config.yaml` add to `exporters`:
```yaml
  googlecloudpubsub:
    project: "${env:GCP_PROJECT_ID}"
    topic: "${env:OTEL_EVENTS_TOPIC}"
    encoding: otlp_proto
```

Add a routing processor:
```yaml
  routing/events:
    error_mode: ignore
    default_exporters: [googlecloud]
    table:
      - context: log
        statement: route() where attributes["event.name"] != nil
        exporters: [googlecloudpubsub]
```

Replace the existing logs pipeline:
```yaml
    logs:
      receivers: [otlp]
      processors: [memory_limiter, resourcedetection, transform/redact, batch]
      exporters: [googlecloud, googlecloudpubsub]
```

with the routed version (the routing processor handles fan-out by attribute):
```yaml
    logs:
      receivers: [otlp]
      processors: [memory_limiter, resourcedetection, transform/redact, batch, routing/events]
      exporters: [googlecloud, googlecloudpubsub]
```

- [ ] **Step 7.2: Add IAM binding for collector to publish**

In `terraform/gcp/modules/otel-collector/main.tf`, append:

```hcl
resource "google_pubsub_topic_iam_member" "publisher" {
  project = var.project_id
  topic   = var.events_topic_name
  role    = "roles/pubsub.publisher"
  member  = "serviceAccount:${google_service_account.collector.email}"
}
```

Add `events_topic_name` to `variables.tf`. Make it optional (default `""`)
and skip the binding when empty, so the module is back-compat for any other
callers.

- [ ] **Step 7.3: Validate config**

```bash
docker run --rm \
  -v "$(pwd)/terraform/gcp/otel-collector-config/otel-config.yaml:/cfg.yaml" \
  otel/opentelemetry-collector-contrib:0.151.0 \
  validate --config=/cfg.yaml
```

- [ ] **Step 7.4: Commit**

```bash
git add terraform/gcp/otel-collector-config/otel-config.yaml \
        terraform/gcp/modules/otel-collector/
git commit -m "feat(otel): collector routes event.name-tagged log records to Pub/Sub"
```

---

## Task 8: DimensionSyncJob — backend code (TDD)

**Files:**
- Create: `back-end/src/main/java/.../telemetry/DimensionSyncJob.java`
- Create: `back-end/src/main/java/.../telemetry/DimensionSyncRunner.java`
- Create: `back-end/src/main/resources/application-dimsync.properties`
- Create: `back-end/src/test/java/.../telemetry/DimensionSyncJobTest.java`

The job:
1. Reads `users` and `organizations` from Postgres via the existing
   `UserRepository` and `OrganizationRepository`.
2. Maps each row to a BQ row (with `synced_at = now`).
3. Issues `MERGE` statements into `analytics.users` and `analytics.orgs`.
4. Tombstones rows whose IDs are no longer in Postgres
   (`UPDATE … SET active=false WHERE user_id NOT IN (current set)`).
5. Logs success/failure metrics so the pipeline-health dashboard can alert
   on stale data.

- [ ] **Step 8.1: Write failing test**

Test with mocked `BigQuery` client and mocked repositories; verify the MERGE
and tombstone SQL is constructed correctly. (Detailed test code omitted here
for brevity — pattern: inject mocks, capture `QueryJobConfiguration`s, assert
on their query text.)

- [ ] **Step 8.2: Implement `DimensionSyncJob`**

Outline:
```java
@Service
public class DimensionSyncJob {
    private final UserRepository userRepo;
    private final OrganizationRepository orgRepo;
    private final BigQuery bq;
    private final String dataset; // e.g. "analytics_prod"

    public void run() {
        Instant now = Instant.now();
        syncUsers(now);
        syncOrgs(now);
        tombstoneUsers();
        tombstoneOrgs();
    }

    // ... MERGE INTO ... USING (UNNEST([…])) AS source ON target.user_id = source.user_id
    //     WHEN MATCHED THEN UPDATE SET … WHEN NOT MATCHED THEN INSERT …
}
```

- [ ] **Step 8.3: Implement `DimensionSyncRunner`**

```java
@Component
@Profile("dimsync")
public class DimensionSyncRunner implements CommandLineRunner {
    private final DimensionSyncJob job;
    @Override
    public void run(String... args) {
        try {
            job.run();
            System.exit(0);
        } catch (Exception e) {
            log.error("dimsync failed", e);
            System.exit(1);
        }
    }
}
```

- [ ] **Step 8.4: Add `application-dimsync.properties`**

```
# Run the sync without binding the web server, JPA scheduling etc.
spring.main.web-application-type=none
spring.task.scheduling.enabled=false
```

- [ ] **Step 8.5: Run tests**

```bash
mvn -pl back-end test -Dtest=DimensionSyncJobTest
```

- [ ] **Step 8.6: Commit**

```bash
git add back-end/src/main/java/gov/nist/oscal/tools/api/telemetry/DimensionSyncJob.java \
        back-end/src/main/java/gov/nist/oscal/tools/api/telemetry/DimensionSyncRunner.java \
        back-end/src/main/resources/application-dimsync.properties \
        back-end/src/test/java/gov/nist/oscal/tools/api/telemetry/DimensionSyncJobTest.java
git commit -m "feat(otel): DimensionSyncJob mirrors users/orgs from Postgres to BigQuery"
```

---

## Task 9: Cloud Run Job + Cloud Scheduler

**Files:**
- Create: `terraform/gcp/modules/dimsync-job/main.tf`
- Create: `terraform/gcp/modules/dimsync-job/variables.tf`
- Create: `terraform/gcp/modules/dimsync-job/outputs.tf`

The Cloud Run Job uses the same image as `oscal-tools-prod` (so we don't
need a second build pipeline) but with `SPRING_PROFILES_ACTIVE=dimsync` and
the `DB_*` env vars present, no public ingress.

- [ ] **Step 9.1: Author the module**

```hcl
resource "google_service_account" "dimsync" {
  account_id   = "dimsync-${var.environment}"
  display_name = "OSCAL Hub Dimension Sync (${var.environment})"
  project      = var.project_id
}

resource "google_bigquery_dataset_iam_member" "dimsync_editor" {
  project    = var.project_id
  dataset_id = var.bigquery_dataset_id
  role       = "roles/bigquery.dataEditor"
  member     = "serviceAccount:${google_service_account.dimsync.email}"
}

resource "google_project_iam_member" "dimsync_job_user" {
  project = var.project_id
  role    = "roles/bigquery.jobUser"
  member  = "serviceAccount:${google_service_account.dimsync.email}"
}

resource "google_cloud_run_v2_job" "dimsync" {
  name     = "dimsync-${var.environment}"
  location = var.region
  project  = var.project_id

  template {
    template {
      service_account = google_service_account.dimsync.email
      max_retries     = 1

      containers {
        image = var.image
        env {
          name  = "SPRING_PROFILES_ACTIVE"
          value = "dimsync"
        }
        env {
          name  = "DB_URL"
          value = var.db_url
        }
        env {
          name  = "ANALYTICS_DATASET_ID"
          value = var.bigquery_dataset_id
        }
        # ... DB_USER, DB_PASSWORD, etc. from Secret Manager bindings
      }
    }
  }
}

resource "google_cloud_scheduler_job" "dimsync_hourly" {
  name        = "dimsync-${var.environment}-hourly"
  schedule    = "0 * * * *"
  time_zone   = "UTC"
  project     = var.project_id
  region      = var.region

  http_target {
    uri         = "https://${var.region}-run.googleapis.com/apis/run.googleapis.com/v1/namespaces/${var.project_id}/jobs/${google_cloud_run_v2_job.dimsync.name}:run"
    http_method = "POST"

    oauth_token {
      service_account_email = google_service_account.dimsync.email
    }
  }
}
```

- [ ] **Step 9.2: Validate**

- [ ] **Step 9.3: Commit**

```bash
git add terraform/gcp/modules/dimsync-job/
git commit -m "feat(otel): Cloud Run Job + Cloud Scheduler for hourly dimension sync"
```

---

## Task 10: Wire all three modules in root Terraform

**Files:**
- Create: `terraform/gcp/analytics.tf`
- Modify: `terraform/gcp/main.tf` (collector module call: pass `events_topic_name`)
- Modify: `terraform/gcp/variables.tf` (any new vars)

- [ ] **Step 10.1: Author `analytics.tf`**

```hcl
module "analytics_bigquery" {
  source      = "./modules/analytics-bigquery"
  project_id  = var.project_id
  region      = var.region
  environment = var.environment
}

module "analytics_pubsub" {
  source         = "./modules/analytics-pubsub"
  project_id     = var.project_id
  environment    = var.environment
  bigquery_table = module.analytics_bigquery.events_table
  publisher_sa   = module.otel_collector[0].collector_service_account
}

module "dimsync" {
  source              = "./modules/dimsync-job"
  project_id          = var.project_id
  region              = var.region
  environment         = var.environment
  image               = "us-central1-docker.pkg.dev/${var.project_id}/oscal-tools/oscal-tools:${var.image_tag}"
  db_url              = "jdbc:postgresql:///${var.db_name}?cloudSqlInstance=${module.database.instance_connection_name}&socketFactory=com.google.cloud.sql.postgres.SocketFactory"
  bigquery_dataset_id = module.analytics_bigquery.dataset_id
}
```

- [ ] **Step 10.2: Pass `events_topic_name` to the collector module**

Update the existing `module "otel_collector"` block in `main.tf`:
```hcl
events_topic_name = module.analytics_pubsub.topic_name
```

- [ ] **Step 10.3: Validate + commit**

```bash
terraform validate
git add terraform/gcp/analytics.tf terraform/gcp/main.tf
git commit -m "feat(otel): wire analytics modules (Pub/Sub, BigQuery, dimsync) into root"
```

---

## Task 11: Pipeline-health Cloud Monitoring dashboard

**Files:**
- Create: `terraform/gcp/dashboards/cs-pipeline-dashboard.json`
- Modify: `terraform/gcp/dashboards.tf`

Tiles:
- Pub/Sub `otel-events-prod` backlog age (alert source)
- Pub/Sub publish rate
- BigQuery `events` table insert errors (`bigquery.googleapis.com/storage/api/streaming_insert_errors_count`)
- Cloud Run Job `dimsync-prod` success/failure count
- Last successful sync timestamp

(Full JSON elided here — pattern is identical to the Phase 1 ops dashboard.)

- [ ] **Step 11.1: Author JSON**

- [ ] **Step 11.2: Add Terraform resource**

```hcl
resource "google_monitoring_dashboard" "cs_pipeline" {
  project        = var.project_id
  dashboard_json = file("${path.module}/dashboards/cs-pipeline-dashboard.json")
}
```

- [ ] **Step 11.3: Commit**

---

## Task 12: Build, deploy, smoke-test

This task is the rollout. **Pause for explicit user approval before running
each subtask** — every step here modifies production GCP.

- [ ] **Step 12.1: Build a fresh app image**

The dimsync runner is in the same image; no separate build pipeline needed.

```bash
cd /Users/travishowerton/Documents/GitHub/oscal-cli
NEW_TAG="$(date -u +%Y%m%d-%H%M%S)-phase2"
IMAGE="us-central1-docker.pkg.dev/oscal-hub/oscal-tools/oscal-tools:$NEW_TAG"
docker build --platform linux/amd64 -t "$IMAGE" --build-arg CACHEBUST=$(date +%s) .
docker push "$IMAGE"
echo "$NEW_TAG" > /tmp/phase2_tag.txt
```

- [ ] **Step 12.2: Build a fresh collector image**

```bash
cd /Users/travishowerton/Documents/GitHub/oscal-cli/terraform/gcp/otel-collector-config
TAG="$(date -u +%Y%m%d-%H%M%S)-phase2"
IMAGE="us-central1-docker.pkg.dev/oscal-hub/oscal-tools/otel-collector:$TAG"
docker build --platform linux/amd64 -t "$IMAGE" .
docker push "$IMAGE"
echo "$TAG" > /tmp/phase2_collector_tag.txt
```

- [ ] **Step 12.3: Update `terraform.tfvars`**

```
image_tag            = "<new app tag from 12.1>"
otel_collector_image = "us-central1-docker.pkg.dev/oscal-hub/oscal-tools/otel-collector:<new collector tag from 12.2>"
```

- [ ] **Step 12.4: `terraform plan`**

Expected: ~10–15 adds (Pub/Sub, BQ tables, views, dimsync job, scheduler, etc.) + 2 in-place updates (oscal-tools-prod and otel-collector-prod for new images).

- [ ] **Step 12.5: User reviews plan + explicitly approves**

(Pause here. Do not auto-apply.)

- [ ] **Step 12.6: `terraform apply`**

- [ ] **Step 12.7: Smoke test**

1. Hit the API a few times with a valid JWT (login, validate, etc.).
2. Verify Pub/Sub backlog drains: `gcloud pubsub subscriptions describe otel-events-bq-prod`.
3. Query BigQuery: `bq query "SELECT COUNT(*) FROM analytics_prod.events WHERE event_time > CURRENT_TIMESTAMP() - INTERVAL 10 MINUTE"` — expect non-zero.
4. Manually trigger the dimsync job: `gcloud run jobs execute dimsync-prod --region=us-central1`. Wait ~30s, then `bq query "SELECT COUNT(*) FROM analytics_prod.users"` and confirm it matches the Postgres count.

- [ ] **Step 12.8: Open PR + merge**

```bash
gh pr create --title "OpenTelemetry Phase 2: events + BigQuery + CS pipeline" \
  --body "..."
```

---

## Task 13: Looker Studio CS dashboard (manual)

**Files:**
- Create: `docs/runbooks/cs-dashboard-setup.md`

Looker Studio is configured manually (no Terraform support for individual
dashboards). The runbook walks through setup so the next person can
reproduce it.

Outline:
1. Open Looker Studio → New Report → BigQuery data source → choose project
   `oscal-hub`, dataset `analytics_prod`, table `vw_events_enriched`.
2. Add three more sources for `vw_daily_active_orgs`, `vw_feature_popularity`,
   and `users` (for drill-down).
3. **Page 1 (Org overview):** scorecards for total orgs / DAU / WAU / MAU,
   table of top 25 orgs by activity, line chart of new orgs, table of
   inactive orgs (>14d).
4. **Page 2 (User overview):** DAU/WAU/MAU lines, heatmap of new users by
   org, top users table, time-to-first-action funnel.
5. **Page 3 (Feature popularity):** stacked bar by `event_name`, per-feature
   metrics table, batch ops funnel, week-over-week trend lines.
6. **Page 4 (Drill-down):** filterable table by org or user, showing all
   events, sessions, last touchpoints.
7. Set sharing to GCP IAM group `cs-team@`.

- [ ] **Step 13.1: Author the runbook**

- [ ] **Step 13.2: Build the dashboard following the runbook**

- [ ] **Step 13.3: Commit the runbook**

---

## Self-review

**Spec coverage check:**
- ✅ TelemetryService + @Telemetry AOP — Tasks 2, 3
- ✅ Event emissions on controllers — Task 4
- ✅ Pub/Sub topic + DLQ + BQ subscription — Task 5
- ✅ BigQuery dataset, fact + dim tables, views — Task 6
- ✅ Collector pipeline addition — Task 7
- ✅ DimensionSyncJob + Cloud Run Job + Scheduler — Tasks 8, 9
- ✅ Looker Studio CS dashboard — Task 13
- ✅ Pipeline-health Cloud Monitoring dashboard — Task 11

**Out of scope here (Phase 3, 4):**
- Frontend RUM (Phase 3)
- Dev dashboard (Phase 3)
- Right-to-be-forgotten hard-delete job (Phase 4)
- Tail-sampler tuning (Phase 4)

**Placeholder scan:** Tasks 8 and 11 have outline-level detail rather than
verbatim code. Step 8.1's test code is described as "pattern" rather than
written out — this is intentional given the test mock complexity, and the
implementer is expected to follow the established pattern from Phase 1
(see `JwtAuthenticationFilterBaggageTest`). Same for Task 11's dashboard
JSON. These are deliberately less prescriptive because the implementer has
strong precedent from Phase 1; if they get stuck, they should escalate.

---

## Risks during execution

- **Pub/Sub → BigQuery subscription with `use_table_schema = true`** requires
  the table to exist before the subscription. Terraform `depends_on` should
  handle this; if not, the apply will fail and need a re-run.
- **DimensionSyncJob VPC connectivity:** Cloud Run Jobs default to public
  egress. Postgres is reached via `socketFactory` which works without VPC.
  Confirm at smoke test.
- **BigQuery view recursion:** the `vw_feature_popularity` view uses
  `JSON_VALUE(attributes, '$.duration_ms')` which assumes events use that key
  consistently. If a controller emits a different shape, the view returns
  NULL for that row. Acceptable.
- **Cardinality on per-org metrics:** if an org spams a single feature, the
  `events` table grows fast. 400-day partition expiration plus clustering
  on `org_id` keeps queries cheap, but watch BigQuery storage costs in the
  pipeline-health dashboard.
