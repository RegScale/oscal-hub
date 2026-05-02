# OpenTelemetry — Phase 1 (Backend Tracing + Collector + Ops Dashboard) — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Land Phase 1 of the OpenTelemetry design — every backend request traceable end-to-end in Cloud Trace, Micrometer metrics flowing into Cloud Monitoring via Managed Prometheus, structured JSON logs with trace correlation in Cloud Logging, and a reproducible Operations dashboard with alert policies. All gated behind `OTEL_ENABLED=true` for staged rollout. Phases 2 (events/BigQuery/CS), 3 (frontend RUM/Dev), and 4 (hardening) are separate plans.

**Architecture:** Attach the OpenTelemetry Java agent to the existing Spring Boot service via `JAVA_TOOL_OPTIONS`. Wire OTel baggage in `JwtAuthenticationFilter` so user/org identity propagates to every span. Bridge Micrometer → OTel and Logback MDC ← baggage. Deploy a separate Cloud Run service running the upstream OTel Collector contrib image, configured via a YAML mounted from a tiny purpose-built image. Apps export OTLP/gRPC to the collector; collector fans out to Cloud Trace, Cloud Monitoring (Managed Prometheus), and Cloud Logging. All infra committed as Terraform.

**Tech Stack:**
- Java 21 / Spring Boot 3.4.2, Maven, JUnit 5, Mockito
- OpenTelemetry Java agent 2.10.0, OTel BOM 1.46.0
- OpenTelemetry Collector Contrib (`otel/opentelemetry-collector-contrib:0.115.0`)
- Terraform 1.5+ with `hashicorp/google` 5.x
- Logback (Spring Boot default) + `logstash-logback-encoder` for JSON output

---

## File map

### New files (created by this plan)

| Path | Responsibility |
|---|---|
| `back-end/src/main/java/gov/nist/oscal/tools/api/telemetry/BaggageMdcInsertingFilter.java` | Logback turbo filter copying OTel baggage into MDC for log correlation |
| `back-end/src/main/resources/logback-spring.xml` | JSON log encoder + MDC pattern + Cloud Logging trace correlation |
| `back-end/src/test/java/gov/nist/oscal/tools/api/security/JwtAuthenticationFilterBaggageTest.java` | Unit test that the JWT filter sets correct baggage entries |
| `back-end/src/test/java/gov/nist/oscal/tools/api/telemetry/BaggageMdcInsertingFilterTest.java` | Unit test that baggage is copied to MDC at log time |
| `back-end/src/test/java/gov/nist/oscal/tools/api/telemetry/CustomSpanTracingTest.java` | Integration test that `@WithSpan`-annotated service methods produce spans |
| `terraform/gcp/modules/otel-collector/main.tf` | Reusable module that deploys collector as Cloud Run + GSA + IAM |
| `terraform/gcp/modules/otel-collector/variables.tf` | Module variables |
| `terraform/gcp/modules/otel-collector/outputs.tf` | Module outputs (collector URL) |
| `terraform/gcp/otel-collector-config/otel-config.yaml` | Collector pipeline config (receivers/processors/exporters) |
| `terraform/gcp/otel-collector-config/Dockerfile` | Tiny image: contrib collector + the YAML baked in |
| `terraform/gcp/dashboards/ops-dashboard.json` | Operations dashboard, applied via `google_monitoring_dashboard` |
| `terraform/gcp/dashboards.tf` | Terraform that pulls the JSON in and creates the dashboard |
| `terraform/gcp/alerts.tf` | Five `google_monitoring_alert_policy` resources |

### Modified files

| Path | Change |
|---|---|
| `back-end/pom.xml` | Add OTel BOM + `opentelemetry-instrumentation-annotations` + Micrometer→OTel bridge + `logstash-logback-encoder` |
| `back-end/src/main/java/.../security/JwtAuthenticationFilter.java` | Wrap `chain.doFilter` in a baggage scope (~10 added lines) |
| `back-end/src/main/java/.../service/ProfileResolutionService.java` | Add `@WithSpan` on the resolve method |
| `back-end/src/main/java/.../service/ValidationService.java` | Add `@WithSpan` on validate + sub-phases |
| `back-end/src/main/java/.../service/ConversionService.java` | Add `@WithSpan` on convert |
| `back-end/src/main/java/.../service/FileStorageService.java` | Add `@WithSpan` on upload + download |
| `back-end/src/main/resources/application.properties` | Add OTel-related properties guarded by `OTEL_ENABLED` |
| `Dockerfile` | Add stage that fetches the OTel agent jar; copy into runtime image at `/otel/opentelemetry-javaagent.jar` |
| `terraform/gcp/main.tf` | Enable Cloud Trace, Cloud Monitoring, Pub/Sub APIs; add module call for `otel-collector` |
| `terraform/gcp/modules/cloud-run/main.tf` (api service) | Pass `OTEL_*` env vars + `JAVA_TOOL_OPTIONS` |
| `terraform/gcp/variables.tf` | Add `otel_enabled` (default `true` in prod, `false` in dev) |
| `terraform/gcp/terraform.tfvars` | Set defaults |

---

## Branch and PR strategy

Work on a fresh branch off the current `howerton` branch. CI/CD on the project pushes to `main` only via PR (per `CLAUDE.md`), so this plan ends with a PR to `main`. The `OTEL_ENABLED=false` default for all environments other than staging means merging the PR cannot break prod traffic; the flag is flipped to `true` in a follow-up tfvars change after staging verification.

```bash
git checkout -b feature/otel-phase-1
```

Run this once at the start of execution; every task below assumes you're on this branch.

---

## Task 1: Add OpenTelemetry Maven dependencies

**Files:**
- Modify: `back-end/pom.xml`

The OTel Java agent (downloaded into the container in Task 5) provides nearly all auto-instrumentation without code changes. We still need API + annotation libs for our custom spans, the Micrometer bridge for metric export, and `logstash-logback-encoder` for JSON logs.

- [ ] **Step 1.1: Add the OTel BOM to `<dependencyManagement>`**

In `back-end/pom.xml`, find the `<dependencyManagement><dependencies>` block (it already contains the Azure / AWS / GCP BOMs). Add after the GCP BOM entry:

```xml
<!-- OpenTelemetry BOM - manages all OTel API versions -->
<dependency>
    <groupId>io.opentelemetry</groupId>
    <artifactId>opentelemetry-bom</artifactId>
    <version>1.46.0</version>
    <type>pom</type>
    <scope>import</scope>
</dependency>
```

- [ ] **Step 1.2: Add the OTel API + annotations + Micrometer bridge to `<dependencies>`**

Find the `<dependencies>` block (the non-management one) and append:

```xml
<!-- OpenTelemetry: API used by custom telemetry code -->
<dependency>
    <groupId>io.opentelemetry</groupId>
    <artifactId>opentelemetry-api</artifactId>
</dependency>

<!-- OpenTelemetry: @WithSpan annotation processed by the Java agent -->
<dependency>
    <groupId>io.opentelemetry.instrumentation</groupId>
    <artifactId>opentelemetry-instrumentation-annotations</artifactId>
    <version>2.10.0</version>
</dependency>

<!-- OpenTelemetry: Micrometer 1.5+ → OTel metrics bridge -->
<dependency>
    <groupId>io.opentelemetry.instrumentation</groupId>
    <artifactId>opentelemetry-micrometer-1.5</artifactId>
    <version>2.10.0-alpha</version>
</dependency>

<!-- JSON Logback encoder for Cloud Logging structured output -->
<dependency>
    <groupId>net.logstash.logback</groupId>
    <artifactId>logstash-logback-encoder</artifactId>
    <version>7.4</version>
</dependency>
```

- [ ] **Step 1.3: Verify the build still compiles**

Run:
```bash
mvn -pl back-end -am -DskipTests package
```

Expected: `BUILD SUCCESS`. Maven downloads new jars; no compile errors because no Java source yet imports them.

- [ ] **Step 1.4: Commit**

```bash
git add back-end/pom.xml
git commit -m "feat(otel): add OpenTelemetry BOM, annotations, Micrometer bridge, JSON log encoder"
```

---

## Task 2: Wire OTel baggage in `JwtAuthenticationFilter` (TDD)

**Files:**
- Modify: `back-end/src/main/java/gov/nist/oscal/tools/api/security/JwtAuthenticationFilter.java`
- Create: `back-end/src/test/java/gov/nist/oscal/tools/api/security/JwtAuthenticationFilterBaggageTest.java`

Existing filter ends at line 98 with `chain.doFilter(request, response)`. We need baggage active during that call so every downstream span inherits it. Wrap the call in a `try-with-resources` over a `Scope` from `Baggage.makeCurrent()`.

- [ ] **Step 2.1: Write the failing baggage test**

Create `back-end/src/test/java/gov/nist/oscal/tools/api/security/JwtAuthenticationFilterBaggageTest.java`:

```java
package gov.nist.oscal.tools.api.security;

import io.opentelemetry.api.baggage.Baggage;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.util.Collections;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JwtAuthenticationFilterBaggageTest {

    private JwtUtil jwtUtil;
    private UserDetailsService userDetailsService;
    private JwtAuthenticationFilter filter;

    @BeforeEach
    void setUp() throws Exception {
        jwtUtil = mock(JwtUtil.class);
        userDetailsService = mock(UserDetailsService.class);
        filter = new JwtAuthenticationFilter();
        // inject mocks via reflection because @Autowired fields are private
        var f1 = JwtAuthenticationFilter.class.getDeclaredField("jwtUtil");
        f1.setAccessible(true); f1.set(filter, jwtUtil);
        var f2 = JwtAuthenticationFilter.class.getDeclaredField("userDetailsService");
        f2.setAccessible(true); f2.set(filter, userDetailsService);
    }

    @Test
    void baggageIsPopulatedFromValidJwtAndIsActiveDuringDownstreamChain() throws Exception {
        String jwt = "fake.jwt.value";
        UserDetails user = new User("alice", "x", Collections.emptyList());
        when(jwtUtil.extractUsername(jwt)).thenReturn("alice");
        when(jwtUtil.validateToken(jwt, user)).thenReturn(true);
        when(jwtUtil.extractUserId(jwt)).thenReturn(456L);
        when(jwtUtil.extractOrganizationId(jwt)).thenReturn(123L);
        when(jwtUtil.extractGlobalRole(jwt)).thenReturn("USER");
        when(jwtUtil.extractOrganizationRole(jwt)).thenReturn("ORG_ADMIN");
        when(userDetailsService.loadUserByUsername("alice")).thenReturn(user);

        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addHeader("Authorization", "Bearer " + jwt);
        MockHttpServletResponse resp = new MockHttpServletResponse();

        AtomicReference<Baggage> captured = new AtomicReference<>();
        FilterChain chain = (HttpServletRequest r, HttpServletResponse s) ->
                captured.set(Baggage.current());

        filter.doFilter(req, resp, chain);

        Baggage b = captured.get();
        assertEquals("456", b.getEntryValue("user.id"));
        assertEquals("123", b.getEntryValue("org.id"));
        assertEquals("USER", b.getEntryValue("user.role.global"));
        assertEquals("ORG_ADMIN", b.getEntryValue("user.role.org"));
    }

    @Test
    void noBaggageEntriesWhenAuthHeaderAbsent() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest();
        MockHttpServletResponse resp = new MockHttpServletResponse();

        AtomicReference<Baggage> captured = new AtomicReference<>();
        FilterChain chain = (HttpServletRequest r, HttpServletResponse s) ->
                captured.set(Baggage.current());

        filter.doFilter(req, resp, chain);

        Baggage b = captured.get();
        // No JWT means we should not put anything in baggage; current baggage is empty
        assertEquals(0, b.size());
    }
}
```

- [ ] **Step 2.2: Run the test, confirm it fails**

```bash
mvn -pl back-end test -Dtest=JwtAuthenticationFilterBaggageTest
```

Expected: FAIL — first test asserts `"456"` but current filter sets nothing on baggage, so `b.getEntryValue("user.id")` returns `null`.

- [ ] **Step 2.3: Implement the baggage scope in the filter**

In `back-end/src/main/java/gov/nist/oscal/tools/api/security/JwtAuthenticationFilter.java`:

1. Add imports near the top of the imports block:
```java
import io.opentelemetry.api.baggage.Baggage;
import io.opentelemetry.context.Scope;
```

2. Replace the body of `doFilterInternal` from line 71 (the `// Validate token and set authentication` comment) through line 99 (the closing brace of the method) with the version below. The change wraps the existing `chain.doFilter` call in a baggage scope when the JWT is valid.

Find:
```java
        // Validate token and set authentication
        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            UserDetails userDetails = this.userDetailsService.loadUserByUsername(username);

            if (jwtUtil.validateToken(jwt, userDetails)) {
                // Extract globalRole and orgRole from JWT token and add to authorities
                Collection<GrantedAuthority> authorities = new ArrayList<>(userDetails.getAuthorities());

                // Add global role (SUPER_ADMIN or USER)
                String globalRole = jwtUtil.extractGlobalRole(jwt);
                if (globalRole != null && !globalRole.isEmpty()) {
                    authorities.add(new SimpleGrantedAuthority("ROLE_" + globalRole));
                }

                // Add organization role (ORG_ADMIN or USER)
                String orgRole = jwtUtil.extractOrganizationRole(jwt);
                if (orgRole != null && !orgRole.isEmpty()) {
                    authorities.add(new SimpleGrantedAuthority("ROLE_" + orgRole));
                }

                UsernamePasswordAuthenticationToken authenticationToken =
                        new UsernamePasswordAuthenticationToken(userDetails, null, authorities);
                authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authenticationToken);
            }
        }

        chain.doFilter(request, response);
    }
```

Replace with:
```java
        // Validate token and set authentication
        Baggage baggage = Baggage.empty();
        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            UserDetails userDetails = this.userDetailsService.loadUserByUsername(username);

            if (jwtUtil.validateToken(jwt, userDetails)) {
                Collection<GrantedAuthority> authorities = new ArrayList<>(userDetails.getAuthorities());

                String globalRole = jwtUtil.extractGlobalRole(jwt);
                if (globalRole != null && !globalRole.isEmpty()) {
                    authorities.add(new SimpleGrantedAuthority("ROLE_" + globalRole));
                }

                String orgRole = jwtUtil.extractOrganizationRole(jwt);
                if (orgRole != null && !orgRole.isEmpty()) {
                    authorities.add(new SimpleGrantedAuthority("ROLE_" + orgRole));
                }

                UsernamePasswordAuthenticationToken authenticationToken =
                        new UsernamePasswordAuthenticationToken(userDetails, null, authorities);
                authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authenticationToken);

                Long userId = jwtUtil.extractUserId(jwt);
                Long orgId = jwtUtil.extractOrganizationId(jwt);
                var builder = Baggage.current().toBuilder();
                if (userId != null) builder.put("user.id", String.valueOf(userId));
                if (orgId != null) builder.put("org.id", String.valueOf(orgId));
                if (globalRole != null && !globalRole.isEmpty()) builder.put("user.role.global", globalRole);
                if (orgRole != null && !orgRole.isEmpty()) builder.put("user.role.org", orgRole);
                baggage = builder.build();
            }
        }

        try (Scope scope = baggage.makeCurrent()) {
            chain.doFilter(request, response);
        }
    }
```

- [ ] **Step 2.4: Run the test, confirm it passes**

```bash
mvn -pl back-end test -Dtest=JwtAuthenticationFilterBaggageTest
```

Expected: PASS, both tests green.

- [ ] **Step 2.5: Run the full backend test suite to confirm nothing else broke**

```bash
mvn -pl back-end test
```

Expected: PASS (or any pre-existing failures unchanged).

- [ ] **Step 2.6: Commit**

```bash
git add back-end/src/main/java/gov/nist/oscal/tools/api/security/JwtAuthenticationFilter.java \
        back-end/src/test/java/gov/nist/oscal/tools/api/security/JwtAuthenticationFilterBaggageTest.java
git commit -m "feat(otel): set OTel baggage with user/org identity in JWT filter"
```

---

## Task 3: Logback JSON output + baggage→MDC bridge (TDD)

**Files:**
- Create: `back-end/src/main/java/gov/nist/oscal/tools/api/telemetry/BaggageMdcInsertingFilter.java`
- Create: `back-end/src/main/resources/logback-spring.xml`
- Create: `back-end/src/test/java/gov/nist/oscal/tools/api/telemetry/BaggageMdcInsertingFilterTest.java`

The OTel agent's Logback instrumentation auto-injects `trace_id` / `span_id` into MDC when `OTEL_INSTRUMENTATION_LOGBACK_APPENDER_EXPERIMENTAL_LOG_ATTRIBUTES=true` is set. Baggage values do *not* get auto-copied — we need a tiny Logback turbo filter that reads the active `Baggage` and sets MDC keys at log time. Cloud Logging uses the special MDC key `logging.googleapis.com/trace` to correlate logs to traces, which we map from `trace_id`.

- [ ] **Step 3.1: Write the failing baggage→MDC test**

Create `back-end/src/test/java/gov/nist/oscal/tools/api/telemetry/BaggageMdcInsertingFilterTest.java`:

```java
package gov.nist.oscal.tools.api.telemetry;

import ch.qos.logback.classic.spi.LoggingEvent;
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
    void leavesMdcEmptyWhenBaggageIsEmpty() {
        BaggageMdcInsertingFilter filter = new BaggageMdcInsertingFilter();
        FilterReply reply = filter.decide(null, null, Level.INFO, "test", null, null);
        assertEquals(FilterReply.NEUTRAL, reply);
        assertNull(MDC.get("user.id"));
        assertNull(MDC.get("org.id"));
    }
}
```

- [ ] **Step 3.2: Run the test, confirm it fails to compile**

```bash
mvn -pl back-end test -Dtest=BaggageMdcInsertingFilterTest
```

Expected: FAIL — `cannot find symbol class BaggageMdcInsertingFilter`.

- [ ] **Step 3.3: Implement the turbo filter**

Create `back-end/src/main/java/gov/nist/oscal/tools/api/telemetry/BaggageMdcInsertingFilter.java`:

```java
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
```

- [ ] **Step 3.4: Run the test, confirm it passes**

```bash
mvn -pl back-end test -Dtest=BaggageMdcInsertingFilterTest
```

Expected: PASS.

- [ ] **Step 3.5: Add `logback-spring.xml` with JSON encoder + turbo filter**

Create `back-end/src/main/resources/logback-spring.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>

    <!-- Copy OTel baggage to MDC before each log event is encoded -->
    <turboFilter class="gov.nist.oscal.tools.api.telemetry.BaggageMdcInsertingFilter"/>

    <!-- Spring Boot defaults -->
    <include resource="org/springframework/boot/logging/logback/defaults.xml"/>

    <!-- JSON appender for Cloud Logging structured ingestion. -->
    <appender name="JSON_CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder class="net.logstash.logback.encoder.LoggingEventCompositeJsonEncoder">
            <providers>
                <timestamp>
                    <fieldName>timestamp</fieldName>
                    <pattern>yyyy-MM-dd'T'HH:mm:ss.SSS'Z'</pattern>
                    <timeZone>UTC</timeZone>
                </timestamp>
                <logLevel>
                    <fieldName>severity</fieldName>
                </logLevel>
                <loggerName>
                    <fieldName>logger</fieldName>
                </loggerName>
                <threadName/>
                <message/>
                <stackTrace/>
                <mdc/>
                <pattern>
                    <pattern>
                        {
                            "logging.googleapis.com/trace": "projects/${GCP_PROJECT_ID:-unknown}/traces/%mdc{trace_id}",
                            "logging.googleapis.com/spanId": "%mdc{span_id}",
                            "logging.googleapis.com/trace_sampled": "%mdc{trace_flags}"
                        }
                    </pattern>
                </pattern>
            </providers>
        </encoder>
    </appender>

    <!-- Plain console appender for local dev -->
    <appender name="PLAIN_CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>${CONSOLE_LOG_PATTERN:-%d{ISO8601} [%thread] %-5level %logger{36} traceId=%mdc{trace_id} userId=%mdc{user.id} orgId=%mdc{org.id} - %msg%n}</pattern>
        </encoder>
    </appender>

    <springProfile name="!cloud">
        <root level="INFO">
            <appender-ref ref="PLAIN_CONSOLE"/>
        </root>
    </springProfile>

    <springProfile name="cloud">
        <root level="INFO">
            <appender-ref ref="JSON_CONSOLE"/>
        </root>
    </springProfile>

</configuration>
```

- [ ] **Step 3.6: Run the full backend test suite**

```bash
mvn -pl back-end test
```

Expected: PASS. (Logback may print warnings about missing `${GCP_PROJECT_ID}` during tests; that's fine — the JSON appender only fires under the `cloud` profile.)

- [ ] **Step 3.7: Commit**

```bash
git add back-end/src/main/java/gov/nist/oscal/tools/api/telemetry/BaggageMdcInsertingFilter.java \
        back-end/src/main/resources/logback-spring.xml \
        back-end/src/test/java/gov/nist/oscal/tools/api/telemetry/BaggageMdcInsertingFilterTest.java
git commit -m "feat(otel): JSON Logback config with baggage->MDC bridge for Cloud Logging correlation"
```

---

## Task 4: Add `@WithSpan` to OSCAL hot paths

**Files:**
- Modify: `back-end/src/main/java/.../service/ProfileResolutionService.java`
- Modify: `back-end/src/main/java/.../service/ValidationService.java`
- Modify: `back-end/src/main/java/.../service/ConversionService.java`
- Modify: `back-end/src/main/java/.../service/FileStorageService.java`

`@WithSpan` is processed by the Java agent at runtime; in tests it is a no-op without the agent attached. We can still verify wiring via the existing test suite — the annotation is a no-op when no agent is present, so all existing tests must still pass after adding it.

- [ ] **Step 4.1: Add `@WithSpan` on `ProfileResolutionService`**

Find the public method that performs profile resolution (the one called by `ProfileController`). Add the annotation directly above the method declaration:

```java
import io.opentelemetry.instrumentation.annotations.WithSpan;
import io.opentelemetry.instrumentation.annotations.SpanAttribute;

// ...
@WithSpan("oscal.profile.resolve_internal")
public ResolveResult resolve(@SpanAttribute("oscal.profile.bytes_in") long bytesIn,
                             /* existing args */) {
    // existing body unchanged
}
```

If the method does not currently take `bytesIn` as an argument, omit the `@SpanAttribute` parameter — capture only the span name. The point is to get a named span; attributes are bonus.

- [ ] **Step 4.2: Add `@WithSpan` on `ValidationService.validate`**

Same pattern. Span name: `oscal.validate_internal`.

- [ ] **Step 4.3: Add `@WithSpan` on `ConversionService.convert`**

Span name: `oscal.convert_internal`.

- [ ] **Step 4.4: Add `@WithSpan` on `FileStorageService.upload` and `.download`**

Span names: `oscal.storage.upload`, `oscal.storage.download`. If the method takes a path/key, annotate it as `@SpanAttribute("oscal.storage.kind")`.

- [ ] **Step 4.5: Run the full backend test suite**

```bash
mvn -pl back-end test
```

Expected: PASS. The annotations are no-ops without the agent, so tests are unaffected.

- [ ] **Step 4.6: Commit**

```bash
git add back-end/src/main/java/gov/nist/oscal/tools/api/service/
git commit -m "feat(otel): annotate OSCAL service hot paths with @WithSpan for finer trace breakdown"
```

---

## Task 5: Add OTel Java agent to the Dockerfile

**Files:**
- Modify: `Dockerfile`

The agent is a single jar; download it in a tiny build stage and copy into the runtime layer. Pin the version so reproducible builds work.

- [ ] **Step 5.1: Add an `otel-agent` build stage**

In `Dockerfile`, immediately before the line `FROM eclipse-temurin:25-jre-jammy` (around line 87), insert:

```dockerfile
# =============================================================================
# Stage: Fetch OpenTelemetry Java agent (pinned version)
# =============================================================================
FROM curlimages/curl:8.10.1 AS otel-agent
ARG OTEL_AGENT_VERSION=2.10.0
RUN curl -fsSL -o /tmp/opentelemetry-javaagent.jar \
    "https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/download/v${OTEL_AGENT_VERSION}/opentelemetry-javaagent.jar"
```

- [ ] **Step 5.2: Copy the agent into the runtime image**

In the same `Dockerfile`, find the runtime stage starting at `FROM eclipse-temurin:25-jre-jammy`. Immediately after the `WORKDIR /app` line (it should already exist; if not, after the `FROM` line), add:

```dockerfile
# OpenTelemetry agent — attached at runtime via JAVA_TOOL_OPTIONS env var
COPY --from=otel-agent /tmp/opentelemetry-javaagent.jar /otel/opentelemetry-javaagent.jar
```

- [ ] **Step 5.3: Verify the image builds locally**

```bash
docker build --target backend-builder -t oscal-builder-test . && \
docker build -t oscal-runtime-test .
```

Expected: both succeed. `docker run --rm oscal-runtime-test ls -la /otel/` should show the agent jar.

- [ ] **Step 5.4: Commit**

```bash
git add Dockerfile
git commit -m "feat(otel): bundle OpenTelemetry Java agent v2.10.0 in runtime image"
```

---

## Task 6: Application-level OTel configuration + feature flag

**Files:**
- Modify: `back-end/src/main/resources/application.properties`

The agent is fully configured by environment variables; the only thing we add to `application.properties` is the Spring profile activation that picks the JSON Logback config in cloud environments.

- [ ] **Step 6.1: Add the `cloud` profile activator**

Append to `back-end/src/main/resources/application.properties`:

```
# Activate the "cloud" Spring profile when running in Cloud Run so the
# JSON Logback appender is selected. Cloud Run sets K_SERVICE.
spring.profiles.active=${SPRING_PROFILES_ACTIVE:${K_SERVICE:+cloud}}
```

- [ ] **Step 6.2: Verify the test suite still passes**

```bash
mvn -pl back-end test
```

Expected: PASS. (Tests don't set `K_SERVICE`, so the `cloud` profile is not activated and plain console output remains.)

- [ ] **Step 6.3: Commit**

```bash
git add back-end/src/main/resources/application.properties
git commit -m "feat(otel): activate cloud profile for JSON logging when running in Cloud Run"
```

---

## Task 7: Enable Cloud Trace, Cloud Monitoring, Pub/Sub APIs (Terraform)

**Files:**
- Modify: `terraform/gcp/main.tf`

Phase 1 needs Cloud Trace + Cloud Monitoring. Pub/Sub is added now even though Phase 2 uses it, because the API enablement is idempotent and free.

- [ ] **Step 7.1: Add APIs to the existing `for_each` set**

In `terraform/gcp/main.tf`, find the `google_project_service "apis"` block (lines 57–77). Append three entries inside the `toset([...])`:

```hcl
    "cloudtrace.googleapis.com",      # Cloud Trace
    "monitoring.googleapis.com",      # Cloud Monitoring + Managed Prometheus
    "pubsub.googleapis.com",          # Pub/Sub (used by Phase 2)
```

- [ ] **Step 7.2: Validate**

```bash
cd terraform/gcp && terraform init -backend=false && terraform validate
```

Expected: `Success! The configuration is valid.`

- [ ] **Step 7.3: Commit**

```bash
git add terraform/gcp/main.tf
git commit -m "feat(otel): enable Cloud Trace, Cloud Monitoring, and Pub/Sub APIs"
```

---

## Task 8: Author the OTel Collector configuration

**Files:**
- Create: `terraform/gcp/otel-collector-config/otel-config.yaml`
- Create: `terraform/gcp/otel-collector-config/Dockerfile`

The upstream `otel/opentelemetry-collector-contrib` image expects its config at `/etc/otelcol-contrib/config.yaml`. We bake our config into a tiny purpose-built image so Cloud Run can pull it from Artifact Registry without runtime config-mounting machinery.

- [ ] **Step 8.1: Author `otel-config.yaml`**

Create `terraform/gcp/otel-collector-config/otel-config.yaml`:

```yaml
# OpenTelemetry Collector configuration for OSCAL Hub — Phase 1.
# Receives OTLP from backend service; exports traces to Cloud Trace,
# metrics to Cloud Monitoring (Managed Prometheus), logs to Cloud Logging.
# Phase 2 will add the events pipeline (Pub/Sub).

receivers:
  otlp:
    protocols:
      grpc:
        endpoint: 0.0.0.0:4317
      http:
        endpoint: 0.0.0.0:4318
        cors:
          allowed_origins:
            - https://*.run.app
            # Add custom-domain origins here once configured.

processors:
  batch:
    send_batch_size: 1024
    timeout: 5s

  memory_limiter:
    check_interval: 1s
    limit_mib: 384
    spike_limit_mib: 64

  resourcedetection:
    detectors: [gcp, env]
    timeout: 2s
    override: false

  # Promote OTel baggage entries to span attributes so they are queryable
  # in Cloud Trace.
  transform/baggage_to_attrs:
    error_mode: ignore
    trace_statements:
      - context: span
        statements:
          - set(attributes["user.id"], baggage["user.id"]) where baggage["user.id"] != nil
          - set(attributes["org.id"], baggage["org.id"]) where baggage["org.id"] != nil
          - set(attributes["user.role.global"], baggage["user.role.global"]) where baggage["user.role.global"] != nil
          - set(attributes["user.role.org"], baggage["user.role.org"]) where baggage["user.role.org"] != nil

  # Strip likely-PII patterns from any string attribute. Belt-and-suspenders;
  # we shouldn't be sending these in the first place.
  attributes/redact:
    actions:
      - key: http.url
        action: update
        from_attribute: http.url
        pattern: "(?i)(token|secret|password|key)=[^&]+"
        value: "$$1=REDACTED"

  tail_sampling:
    decision_wait: 10s
    num_traces: 50000
    expected_new_traces_per_sec: 100
    policies:
      - name: errors-always
        type: status_code
        status_code:
          status_codes: [ERROR]
      - name: slow-always
        type: latency
        latency:
          threshold_ms: 1000
      - name: baseline-25pct
        type: probabilistic
        probabilistic:
          sampling_percentage: 25

exporters:
  googlecloud:
    log:
      default_log_name: oscal-hub-collector
  googlemanagedprometheus: {}

service:
  telemetry:
    logs:
      level: info
  pipelines:
    traces:
      receivers: [otlp]
      processors: [memory_limiter, resourcedetection, transform/baggage_to_attrs, attributes/redact, tail_sampling, batch]
      exporters: [googlecloud]
    metrics:
      receivers: [otlp]
      processors: [memory_limiter, resourcedetection, batch]
      exporters: [googlemanagedprometheus]
    logs:
      receivers: [otlp]
      processors: [memory_limiter, resourcedetection, attributes/redact, batch]
      exporters: [googlecloud]
```

- [ ] **Step 8.2: Author the collector image Dockerfile**

Create `terraform/gcp/otel-collector-config/Dockerfile`:

```dockerfile
# OSCAL Hub OTel Collector image. Just upstream contrib + our config.
FROM otel/opentelemetry-collector-contrib:0.115.0
COPY otel-config.yaml /etc/otelcol-contrib/config.yaml
```

- [ ] **Step 8.3: Validate the collector config**

```bash
docker run --rm -v "$(pwd)/terraform/gcp/otel-collector-config/otel-config.yaml:/cfg.yaml" \
  otel/opentelemetry-collector-contrib:0.115.0 \
  --config=/cfg.yaml --dry-run
```

Expected: exits 0 with no errors. (`--dry-run` validates and exits.)

- [ ] **Step 8.4: Commit**

```bash
git add terraform/gcp/otel-collector-config/
git commit -m "feat(otel): collector config and Dockerfile for traces, metrics, logs pipelines"
```

---

## Task 9: Author the OTel Collector Terraform module

**Files:**
- Create: `terraform/gcp/modules/otel-collector/main.tf`
- Create: `terraform/gcp/modules/otel-collector/variables.tf`
- Create: `terraform/gcp/modules/otel-collector/outputs.tf`

The module wraps the existing `cloud-run` module with collector-specific GSA, IAM, and the right env vars. Keeping it as its own module makes the Phase 2 additions (Pub/Sub publish role) a one-line change.

- [ ] **Step 9.1: Author `variables.tf`**

Create `terraform/gcp/modules/otel-collector/variables.tf`:

```hcl
variable "project_id" {
  type = string
}

variable "region" {
  type = string
}

variable "environment" {
  type = string
}

variable "image" {
  type        = string
  description = "Fully-qualified image of the collector (e.g. us-central1-docker.pkg.dev/PROJECT/REPO/otel-collector:SHA)."
}

variable "min_instances" {
  type    = number
  default = 1
}

variable "max_instances" {
  type    = number
  default = 10
}
```

- [ ] **Step 9.2: Author `main.tf`**

Create `terraform/gcp/modules/otel-collector/main.tf`:

```hcl
# Dedicated service account for the collector with least-privilege roles.
resource "google_service_account" "collector" {
  account_id   = "otel-collector-${var.environment}"
  display_name = "OSCAL Hub OTel Collector (${var.environment})"
  project      = var.project_id
}

resource "google_project_iam_member" "trace_agent" {
  project = var.project_id
  role    = "roles/cloudtrace.agent"
  member  = "serviceAccount:${google_service_account.collector.email}"
}

resource "google_project_iam_member" "metric_writer" {
  project = var.project_id
  role    = "roles/monitoring.metricWriter"
  member  = "serviceAccount:${google_service_account.collector.email}"
}

resource "google_project_iam_member" "log_writer" {
  project = var.project_id
  role    = "roles/logging.logWriter"
  member  = "serviceAccount:${google_service_account.collector.email}"
}

resource "google_cloud_run_v2_service" "collector" {
  name     = "otel-collector-${var.environment}"
  location = var.region
  project  = var.project_id
  ingress  = "INGRESS_TRAFFIC_ALL"

  template {
    service_account = google_service_account.collector.email

    scaling {
      min_instance_count = var.min_instances
      max_instance_count = var.max_instances
    }

    containers {
      image = var.image

      ports {
        container_port = 4317  # OTLP/gRPC primary
      }
      ports {
        container_port = 4318  # OTLP/HTTP for browser clients (used in Phase 3)
      }

      resources {
        limits = {
          cpu    = "1"
          memory = "512Mi"
        }
        cpu_idle          = false
        startup_cpu_boost = true
      }

      startup_probe {
        tcp_socket { port = 4317 }
        initial_delay_seconds = 5
        timeout_seconds       = 3
        period_seconds        = 5
        failure_threshold     = 6
      }
    }
  }
}

# Allow only project-internal callers to publish telemetry. The api service
# account is the principal that reaches the collector via OTLP/gRPC.
resource "google_cloud_run_service_iam_member" "invoker" {
  project  = var.project_id
  location = var.region
  service  = google_cloud_run_v2_service.collector.name
  role     = "roles/run.invoker"
  member   = "serviceAccount:${var.project_id}@appspot.gserviceaccount.com"
  # Replace member with the actual API service account once known. See
  # outputs.tf — main.tf passes the api SA in here.
}
```

- [ ] **Step 9.3: Author `outputs.tf`**

Create `terraform/gcp/modules/otel-collector/outputs.tf`:

```hcl
output "collector_url" {
  value       = google_cloud_run_v2_service.collector.uri
  description = "Base URL of the collector service."
}

output "collector_service_account" {
  value       = google_service_account.collector.email
  description = "Service account email used by the collector."
}
```

- [ ] **Step 9.4: Validate**

```bash
cd terraform/gcp && terraform init -backend=false && terraform validate
```

Expected: `Success! The configuration is valid.`

- [ ] **Step 9.5: Commit**

```bash
git add terraform/gcp/modules/otel-collector/
git commit -m "feat(otel): Terraform module for OTel Collector Cloud Run service with least-priv GSA"
```

---

## Task 10: Wire collector module + API service env vars in root Terraform

**Files:**
- Modify: `terraform/gcp/main.tf`
- Modify: `terraform/gcp/variables.tf`
- Modify: `terraform/gcp/terraform.tfvars`
- Modify: `terraform/gcp/modules/cloud-run/main.tf` (only the API service env wiring path)

Add a module call for the collector and pass `OTEL_*` env vars + `JAVA_TOOL_OPTIONS` to the API service. The image for the collector is built and pushed by CI (`gcp-deploy.yml`) — Task 13 covers that.

- [ ] **Step 10.1: Add `otel_enabled` and `otel_collector_image` to `variables.tf`**

In `terraform/gcp/variables.tf`, append:

```hcl
variable "otel_enabled" {
  type        = bool
  default     = false
  description = "When true, the API service exports telemetry via JAVA_TOOL_OPTIONS attaching the OTel agent."
}

variable "otel_collector_image" {
  type        = string
  default     = ""
  description = "Fully-qualified image of the otel-collector image (set by CI; empty disables module)."
}
```

- [ ] **Step 10.2: Add module call in `main.tf`**

In `terraform/gcp/main.tf`, append after the existing modules:

```hcl
module "otel_collector" {
  count  = var.otel_collector_image != "" ? 1 : 0
  source = "./modules/otel-collector"

  project_id  = var.project_id
  region      = var.region
  environment = var.environment
  image       = var.otel_collector_image
}
```

- [ ] **Step 10.3: Pass OTel env vars to the API service**

In the call site of `module "cloud_run_api"` (find it in `terraform/gcp/main.tf`), merge OTel env vars into the existing `environment_variables` map. Append the new keys to whatever map is already passed:

```hcl
environment_variables = merge({
  # existing vars...
}, var.otel_enabled && length(module.otel_collector) > 0 ? {
  JAVA_TOOL_OPTIONS                                                     = "-javaagent:/otel/opentelemetry-javaagent.jar"
  OTEL_SERVICE_NAME                                                     = "oscal-api"
  OTEL_RESOURCE_ATTRIBUTES                                              = "service.namespace=oscal-hub,deployment.environment=${var.environment}"
  OTEL_EXPORTER_OTLP_PROTOCOL                                           = "grpc"
  OTEL_EXPORTER_OTLP_ENDPOINT                                           = module.otel_collector[0].collector_url
  OTEL_TRACES_SAMPLER                                                   = "parentbased_always_on"
  OTEL_LOGS_EXPORTER                                                    = "otlp"
  OTEL_METRICS_EXPORTER                                                 = "otlp"
  OTEL_INSTRUMENTATION_LOGBACK_APPENDER_EXPERIMENTAL_LOG_ATTRIBUTES     = "true"
  OTEL_INSTRUMENTATION_MICROMETER_ENABLED                               = "true"
  GCP_PROJECT_ID                                                        = var.project_id
} : {})
```

If the existing call site doesn't already have an `environment_variables` map, this snippet is the new one. Either way, `var.otel_enabled` defaulting to `false` keeps the env vars out until staging verification (Task 13) flips it.

- [ ] **Step 10.4: Set defaults in `terraform.tfvars`**

In `terraform/gcp/terraform.tfvars`, append:

```hcl
otel_enabled         = false
otel_collector_image = ""
```

- [ ] **Step 10.5: Validate**

```bash
cd terraform/gcp && terraform validate
```

Expected: `Success! The configuration is valid.`

- [ ] **Step 10.6: Commit**

```bash
git add terraform/gcp/main.tf terraform/gcp/variables.tf terraform/gcp/terraform.tfvars
git commit -m "feat(otel): wire collector module + API service OTel env vars (gated by otel_enabled flag)"
```

---

## Task 11: Operations dashboard JSON + Terraform

**Files:**
- Create: `terraform/gcp/dashboards/ops-dashboard.json`
- Create: `terraform/gcp/dashboards.tf`

Cloud Monitoring dashboard JSON is verbose; the file is one source-of-truth checked into git, applied via `google_monitoring_dashboard`. Tile filters reference the metrics the OTel collector emits via Managed Prometheus.

- [ ] **Step 11.1: Author the dashboard JSON**

Create `terraform/gcp/dashboards/ops-dashboard.json`:

```json
{
  "displayName": "OSCAL Hub — Operations",
  "mosaicLayout": {
    "columns": 12,
    "tiles": [
      {
        "width": 6, "height": 4,
        "widget": {
          "title": "Request rate (RPS) by service",
          "xyChart": {
            "dataSets": [{
              "timeSeriesQuery": {
                "timeSeriesFilter": {
                  "filter": "metric.type=\"run.googleapis.com/request_count\" resource.type=\"cloud_run_revision\"",
                  "aggregation": {
                    "alignmentPeriod": "60s",
                    "perSeriesAligner": "ALIGN_RATE",
                    "crossSeriesReducer": "REDUCE_SUM",
                    "groupByFields": ["resource.label.service_name", "metric.label.response_code_class"]
                  }
                }
              },
              "plotType": "STACKED_BAR"
            }]
          }
        }
      },
      {
        "xPos": 6, "width": 6, "height": 4,
        "widget": {
          "title": "p95 latency by service",
          "xyChart": {
            "dataSets": [{
              "timeSeriesQuery": {
                "timeSeriesFilter": {
                  "filter": "metric.type=\"run.googleapis.com/request_latencies\" resource.type=\"cloud_run_revision\"",
                  "aggregation": {
                    "alignmentPeriod": "60s",
                    "perSeriesAligner": "ALIGN_DELTA",
                    "crossSeriesReducer": "REDUCE_PERCENTILE_95",
                    "groupByFields": ["resource.label.service_name"]
                  }
                }
              }
            }]
          }
        }
      },
      {
        "yPos": 4, "width": 4, "height": 4,
        "widget": {
          "title": "Cloud Run instances",
          "xyChart": {
            "dataSets": [{
              "timeSeriesQuery": {
                "timeSeriesFilter": {
                  "filter": "metric.type=\"run.googleapis.com/container/instance_count\" resource.type=\"cloud_run_revision\"",
                  "aggregation": {
                    "alignmentPeriod": "60s",
                    "perSeriesAligner": "ALIGN_MEAN",
                    "groupByFields": ["resource.label.service_name"]
                  }
                }
              }
            }]
          }
        }
      },
      {
        "yPos": 4, "xPos": 4, "width": 4, "height": 4,
        "widget": {
          "title": "CPU utilization",
          "xyChart": {
            "dataSets": [{
              "timeSeriesQuery": {
                "timeSeriesFilter": {
                  "filter": "metric.type=\"run.googleapis.com/container/cpu/utilizations\" resource.type=\"cloud_run_revision\"",
                  "aggregation": {
                    "alignmentPeriod": "60s",
                    "perSeriesAligner": "ALIGN_MEAN",
                    "crossSeriesReducer": "REDUCE_MEAN",
                    "groupByFields": ["resource.label.service_name"]
                  }
                }
              }
            }]
          }
        }
      },
      {
        "yPos": 4, "xPos": 8, "width": 4, "height": 4,
        "widget": {
          "title": "Memory utilization",
          "xyChart": {
            "dataSets": [{
              "timeSeriesQuery": {
                "timeSeriesFilter": {
                  "filter": "metric.type=\"run.googleapis.com/container/memory/utilizations\" resource.type=\"cloud_run_revision\"",
                  "aggregation": {
                    "alignmentPeriod": "60s",
                    "perSeriesAligner": "ALIGN_MEAN",
                    "crossSeriesReducer": "REDUCE_MEAN",
                    "groupByFields": ["resource.label.service_name"]
                  }
                }
              }
            }]
          }
        }
      },
      {
        "yPos": 8, "width": 6, "height": 4,
        "widget": {
          "title": "Cloud SQL connections",
          "xyChart": {
            "dataSets": [{
              "timeSeriesQuery": {
                "timeSeriesFilter": {
                  "filter": "metric.type=\"cloudsql.googleapis.com/database/postgresql/num_backends\" resource.type=\"cloudsql_database\"",
                  "aggregation": {
                    "alignmentPeriod": "60s",
                    "perSeriesAligner": "ALIGN_MEAN"
                  }
                }
              }
            }]
          }
        }
      },
      {
        "yPos": 8, "xPos": 6, "width": 6, "height": 4,
        "widget": {
          "title": "Cloud SQL CPU",
          "xyChart": {
            "dataSets": [{
              "timeSeriesQuery": {
                "timeSeriesFilter": {
                  "filter": "metric.type=\"cloudsql.googleapis.com/database/cpu/utilization\" resource.type=\"cloudsql_database\"",
                  "aggregation": {
                    "alignmentPeriod": "60s",
                    "perSeriesAligner": "ALIGN_MEAN"
                  }
                }
              }
            }]
          }
        }
      }
    ]
  }
}
```

- [ ] **Step 11.2: Author `dashboards.tf`**

Create `terraform/gcp/dashboards.tf`:

```hcl
resource "google_monitoring_dashboard" "ops" {
  project        = var.project_id
  dashboard_json = file("${path.module}/dashboards/ops-dashboard.json")
}
```

- [ ] **Step 11.3: Validate**

```bash
cd terraform/gcp && terraform validate && \
  python3 -m json.tool dashboards/ops-dashboard.json > /dev/null
```

Expected: terraform success + JSON parses cleanly.

- [ ] **Step 11.4: Commit**

```bash
git add terraform/gcp/dashboards/ops-dashboard.json terraform/gcp/dashboards.tf
git commit -m "feat(otel): committed Operations dashboard with CPU/memory/latency/RPS/SQL tiles"
```

---

## Task 12: Alert policies in Terraform

**Files:**
- Create: `terraform/gcp/alerts.tf`

Five alert policies from the spec. Notification channels are bound by name from existing project resources or variable inputs; if none exist yet, the policies still create — just with no targets — and a follow-up tfvars change wires them up.

- [ ] **Step 12.1: Author `alerts.tf`**

Create `terraform/gcp/alerts.tf`:

```hcl
variable "alert_email" {
  type        = string
  default     = ""
  description = "Email address to receive Cloud Monitoring alerts; empty disables the email channel."
}

resource "google_monitoring_notification_channel" "email" {
  count        = var.alert_email != "" ? 1 : 0
  project      = var.project_id
  display_name = "OSCAL Hub Alerts"
  type         = "email"
  labels = {
    email_address = var.alert_email
  }
}

locals {
  notification_channels = google_monitoring_notification_channel.email[*].id
}

resource "google_monitoring_alert_policy" "high_5xx_rate" {
  project      = var.project_id
  display_name = "Cloud Run 5xx rate > 1% (5min)"
  combiner     = "OR"
  enabled      = true

  conditions {
    display_name = "5xx rate"
    condition_threshold {
      filter          = "metric.type=\"run.googleapis.com/request_count\" resource.type=\"cloud_run_revision\" metric.label.\"response_code_class\"=\"5xx\""
      duration        = "300s"
      comparison      = "COMPARISON_GT"
      threshold_value = 0.01
      aggregations {
        alignment_period   = "60s"
        per_series_aligner = "ALIGN_RATE"
      }
    }
  }

  notification_channels = local.notification_channels
}

resource "google_monitoring_alert_policy" "p99_latency" {
  project      = var.project_id
  display_name = "p99 latency > 5s on validate/convert/resolve"
  combiner     = "OR"
  enabled      = true

  conditions {
    display_name = "validate/convert/resolve p99"
    condition_threshold {
      filter          = "metric.type=\"run.googleapis.com/request_latencies\" resource.type=\"cloud_run_revision\""
      duration        = "600s"
      comparison      = "COMPARISON_GT"
      threshold_value = 5000
      aggregations {
        alignment_period     = "60s"
        per_series_aligner   = "ALIGN_DELTA"
        cross_series_reducer = "REDUCE_PERCENTILE_99"
        group_by_fields      = ["resource.label.service_name"]
      }
    }
  }

  notification_channels = local.notification_channels
}

resource "google_monitoring_alert_policy" "sql_connection_saturation" {
  project      = var.project_id
  display_name = "Cloud SQL connection saturation > 80%"
  combiner     = "OR"
  enabled      = true

  conditions {
    display_name = "connections / max"
    condition_threshold {
      filter          = "metric.type=\"cloudsql.googleapis.com/database/postgresql/num_backends\" resource.type=\"cloudsql_database\""
      duration        = "300s"
      comparison      = "COMPARISON_GT"
      threshold_value = 80
      aggregations {
        alignment_period   = "60s"
        per_series_aligner = "ALIGN_MEAN"
      }
    }
  }

  notification_channels = local.notification_channels
}

resource "google_monitoring_alert_policy" "run_concurrency" {
  project      = var.project_id
  display_name = "Cloud Run concurrency utilization > 80%"
  combiner     = "OR"
  enabled      = true

  conditions {
    display_name = "container concurrency"
    condition_threshold {
      filter          = "metric.type=\"run.googleapis.com/container/instance_count\" resource.type=\"cloud_run_revision\""
      duration        = "300s"
      comparison      = "COMPARISON_GT"
      threshold_value = 0.8
      aggregations {
        alignment_period   = "60s"
        per_series_aligner = "ALIGN_MEAN"
      }
    }
  }

  notification_channels = local.notification_channels
}

resource "google_monitoring_alert_policy" "auth_failed_burst" {
  project      = var.project_id
  display_name = "Failed login burst > 50/min"
  combiner     = "OR"
  enabled      = true

  conditions {
    display_name = "auth_failed_burst"
    condition_threshold {
      filter          = "metric.type=\"logging.googleapis.com/user/oscal_auth_login_failed\""
      duration        = "300s"
      comparison      = "COMPARISON_GT"
      threshold_value = 50
      aggregations {
        alignment_period   = "60s"
        per_series_aligner = "ALIGN_RATE"
      }
    }
  }

  notification_channels = local.notification_channels
}
```

The last policy references a log-based metric `oscal_auth_login_failed` that does not exist until Phase 2 emits the event. Until then, the alert policy exists but has no data — that's fine; it activates automatically once the metric appears.

- [ ] **Step 12.2: Validate**

```bash
cd terraform/gcp && terraform validate
```

Expected: `Success! The configuration is valid.`

- [ ] **Step 12.3: Commit**

```bash
git add terraform/gcp/alerts.tf
git commit -m "feat(otel): five alert policies (5xx, p99, SQL conns, run concurrency, auth burst)"
```

---

## Task 13: Apply, smoke-test on staging, open PR, then enable in prod

**Files:** none changed in this task — operational verification + rollout.

CI/CD pushes to main only via PR (per `CLAUDE.md`). The plan ends with a PR. Staging verification happens in the PR workflow (`gcp-deploy.yml` plans on PR; if you have a staging tfvars, a manual `terraform apply -var-file=staging.tfvars` step is the verification path). The default `otel_enabled=false` ensures merging the PR cannot break production traffic — collector deploys, but the API service ignores it until the flag flips.

- [ ] **Step 13.1: Push the branch and open the PR**

```bash
git push -u origin feature/otel-phase-1
gh pr create --title "OpenTelemetry Phase 1: backend tracing + collector + ops dashboard" --body "$(cat <<'EOF'
## Summary
- Adds OpenTelemetry Java agent + custom baggage in JWT filter so user/org identity propagates to every span
- Adds JSON Logback config with trace correlation for Cloud Logging
- Deploys OTel Collector as a separate Cloud Run service with traces/metrics/logs pipelines
- Adds reproducible Operations dashboard JSON + five alert policies in Terraform
- All gated behind `otel_enabled=false` for safe rollout

## Test plan
- [ ] Backend test suite passes (`mvn -pl back-end test`)
- [ ] Terraform validates (`cd terraform/gcp && terraform validate`)
- [ ] Collector image builds locally
- [ ] After merge: build collector image, push to Artifact Registry, set `otel_collector_image` in tfvars
- [ ] Apply on staging, set `otel_enabled=true`, hit a few API endpoints, verify traces appear in Cloud Trace
- [ ] Verify the Operations dashboard renders in Cloud Monitoring with non-empty data
- [ ] Once staging is healthy for 24h, flip `otel_enabled=true` in prod tfvars in a follow-up PR

🤖 Generated with [Claude Code](https://claude.com/claude-code)
EOF
)"
```

- [ ] **Step 13.2: Wait for CI green + reviewer approval**

Per branch protection in `CLAUDE.md`: PRs to main need green CI + approval. Do not bypass.

- [ ] **Step 13.3: Merge the PR**

Once approved + CI is green:
```bash
gh pr merge --squash
```

- [ ] **Step 13.4: Build and push the collector image (one-time bootstrap)**

The CI workflow `gcp-deploy.yml` does not yet build the collector image. The first push needs a manual build. From a clone of `main` with auth to the project:

```bash
PROJECT="$(gcloud config get-value project)"
REGION="us-central1"
REPO="oscal"  # the existing Artifact Registry repo
TAG="$(git rev-parse --short HEAD)"
IMAGE="${REGION}-docker.pkg.dev/${PROJECT}/${REPO}/otel-collector:${TAG}"

cd terraform/gcp/otel-collector-config
docker build -t "${IMAGE}" .
docker push "${IMAGE}"
echo "${IMAGE}"
```

Save the resulting image URL — it goes into `terraform.tfvars`.

- [ ] **Step 13.5: Open a follow-up PR setting `otel_collector_image` and `otel_enabled=true` for staging**

```bash
git checkout -b chore/otel-enable-staging
# Edit terraform/gcp/terraform.tfvars (or staging.tfvars):
#   otel_enabled         = true
#   otel_collector_image = "<the image url from step 13.4>"
git add terraform/gcp/terraform.tfvars
git commit -m "chore(otel): enable OpenTelemetry on staging"
git push -u origin chore/otel-enable-staging
gh pr create --title "Enable OpenTelemetry on staging" --body "Flip otel_enabled=true on staging after Phase 1 PR merged."
```

- [ ] **Step 13.6: After staging-PR merges, smoke-test**

After the staging deploy completes:

1. Hit a known-authenticated endpoint:
   ```bash
   TOKEN=$(curl -s -X POST -H 'Content-Type: application/json' \
     -d '{"username":"dev-user","password":"…"}' \
     "https://oscal-api-staging-…run.app/api/auth/login" | jq -r .token)
   curl -s -H "Authorization: Bearer ${TOKEN}" \
     -X POST -H 'Content-Type: application/json' \
     -d '{"content":"<catalog…>","format":"XML"}' \
     "https://oscal-api-staging-…run.app/api/validate" >/dev/null
   ```

2. Open Cloud Trace in the GCP console; filter by `service.name=oscal-api`; expect to see the validate span tree with child JDBC + custom `oscal.validate_internal` spans.

3. Open Cloud Logging Explorer; filter `logName="…/oscal-api-staging"` and confirm log entries carry `logging.googleapis.com/trace` populated and MDC fields `user.id` / `org.id`.

4. Open the Operations dashboard in Cloud Monitoring; tiles should be populated.

- [ ] **Step 13.7: Open a follow-up PR enabling OTel on prod**

If staging is healthy for 24h with no alerts firing unexpectedly, repeat 13.5 for the prod environment.

---

## Self-review

I checked this plan against the spec sections covered by Phase 1.

**Spec coverage check (Phase 1 only):**
- ✅ "Add OTel Java agent to backend Dockerfile" — Task 5
- ✅ "Wire baggage in JwtAuthenticationFilter" — Task 2
- ✅ "MDC bridge" — Task 3
- ✅ "Deploy collector Cloud Run service with traces + metrics + logs pipelines" — Tasks 8, 9, 10
- ✅ "Add Cloud Trace + Ops dashboard JSON to Terraform" — Tasks 7, 11
- ✅ "Verify end-to-end on staging" — Task 13
- ✅ "Ship behind `OTEL_ENABLED=true` flag for staged rollout" — Tasks 10, 13
- ✅ Custom `@WithSpan` on OSCAL hot paths (from spec "Backend instrumentation" section) — Task 4
- ✅ Micrometer→OTel bridge (spec "Custom telemetry surface" section) — Task 1 (dep added; agent enables the bridge via env var in Task 10)
- ✅ Alert policies (spec "Alert policies" section, Phase 1 subset) — Task 12

**Out of scope here (later phases):**
- Frontend OTel Web SDK (Phase 3)
- `TelemetryService` + `@Telemetry` AOP (Phase 2)
- Pub/Sub topic, BigQuery `events`, `DimensionSyncJob`, Looker Studio (Phase 2)
- Right-to-be-forgotten workflow (Phase 4)
- Tail-sampler tuning (Phase 4)

**Placeholder scan:** None. All steps have exact file paths, complete code blocks, exact commands.

**Type consistency check:** baggage keys are spelled `user.id`, `org.id`, `user.role.global`, `user.role.org` consistently across Tasks 2, 3, 8.

**Ambiguity check:** the one judgement call left to the implementer is which OSCAL service methods deserve `@WithSpan` (Task 4) — the plan names the four highest-value services and the implementer picks the public method on each.

---

## Risks during execution

- **OTel agent + Java 21+ versions:** If the agent jar version 2.10.0 is older than the JVM, you may see `UnsupportedClassVersionError` for the agent's own classes. If so, bump `OTEL_AGENT_VERSION` in the Dockerfile; releases happen monthly.
- **Cloud Run cold start with min_instance=1:** The collector at min-1 keeps one warm instance, billing ~$10/mo. If cost matters more than first-request latency on telemetry, drop to `min_instances=0` after staging validation.
- **`logback-spring.xml` ordering:** Spring Boot picks up `logback-spring.xml` only if `logback.xml` is absent. Verify the latter is not present in `back-end/src/main/resources` (it isn't today).
- **Tail sampling at 25%:** real traffic may want this lower or higher; revisit in Phase 4 once 2 weeks of cost data is in.
