# OpenTelemetry Phase 2 — Outstanding Follow-Ups

**Status:** Phase 2 infrastructure deployed; two app-side bugs remain.
**Date:** 2026-05-02
**Branch:** `feature/otel-phase-1` at `fb079c5`

---

## What landed and works

| Component | State |
|---|---|
| Pub/Sub topic `otel-events-prod` + DLQ + BigQuery subscription | ✅ Deployed, ACTIVE |
| BigQuery dataset `analytics_prod` with `events` / `users` / `orgs` tables + 4 views | ✅ Deployed |
| OTel Collector with `routing/events` connector → Pub/Sub | ✅ Deployed (revision `otel-collector-prod-00004-5jw`+) |
| Cloud Run Job `dimsync-prod` + Cloud Scheduler `dimsync-prod-hourly` | ✅ Deployed, scheduled |
| CS pipeline-health Cloud Monitoring dashboard | ✅ Deployed |
| `oscal-tools-prod` running phase2c image with TelemetryService + AOP | ✅ Healthy, 100% traffic |

Verified end-to-end:
- API health 200
- Auto-instrumented OTel logs (audit, JDBC) flow through the collector to Cloud Logging successfully
- Trace pipeline (Phase 1) still working — baggage→span attributes verified

---

## Bug 1 — DimensionSyncJob: `LazyInitializationException`

**Symptom:** `gcloud run jobs execute dimsync-prod` fails. Logs show:
```
org.hibernate.LazyInitializationException: failed to lazily initialize a collection of role:
  gov.nist.oscal.tools.api.entity.User.organizationMemberships:
  could not initialize proxy - no Session
```

**Root cause:** Task 8's implementer added `org_id_primary` derivation that walks `User.organizationMemberships` (a JPA `@OneToMany` lazy collection). The dimsync runner iterates `userRepo.findAll()` *outside* a transaction, so the JPA session is closed when the lazy collection is touched.

**Fix options (pick one):**

1. **`@Transactional(readOnly = true)` on `DimensionSyncJob.run()`** — opens a long-lived session for the entire sync. Simplest. ~1-line change.
2. **Eager fetch via custom repository query** — add `userRepo.findAllWithMemberships()` using `@EntityGraph` or `JOIN FETCH`. Cleaner separation but more code.
3. **Drop `org_id_primary` for v1** — the column was added by the Task 8 implementer; the BQ schema declares it but it's not strictly required for the CS dashboard. Set to NULL and revisit later.

**Recommendation:** Option 1. Add `@Transactional(readOnly = true)` to `DimensionSyncJob.run()` (and import). Rebuild + redeploy. ~15 min cycle.

**Files:** `back-end/src/main/java/gov/nist/oscal/tools/api/telemetry/DimensionSyncJob.java`

---

## Bug 2 — TelemetryService: events not reaching BigQuery

**Symptom:** `analytics_prod.events` table is empty after deliberately triggering events (failed logins from the smoke test). The catch-block in `AuthController.login()` IS firing (we see `ERROR g.n.o.t.a.controller.AuthController - Login failed for user phase2b-smoke-3` in Cloud Logging), but no log records from logger name `oscal-hub.events` appear anywhere.

**What was tried:**
- Path 1 (OTel Logs Bridge API via `OpenTelemetry.getLogsBridge()`) — captured-but-silent. Possibly no-op because the SDK's logs API isn't fully active despite `OTEL_LOGS_EXPORTER=otlp`.
- Path 2 (SLF4J fallback via `slf4j.info("event {}", eventName)`) — added in `6c45681`, deployed in phase2b/c. Still no visible `oscal-hub.events` log lines.

**Verified:** the deployed image's bytecode contains the SLF4J fallback (extracted jar, ran `strings`, found `oscal-hub.events`, `otelLogger`, `org/slf4j/MDC`, `event {}`).

**Hypothesis (most likely):** The Logback config (`logback-spring.xml`) uses `<springProfile name="cloud">` and `<springProfile name="!cloud">` for appender wiring. The actual active Spring profile in production is `gcp`, NOT `cloud` — set explicitly in the API service env vars. Both `springProfile` blocks are *false-y* under the `gcp` profile, so the root logger has *no appender attached*, and console output we DO see comes from Spring Boot's auto-config fallback only because the springProfile blocks are silent.

If this is right, the fix is: change the springProfile blocks to match the actual profile name, OR remove the springProfile guards entirely and use a single root with the JSON appender.

**Other possibilities to rule out:**
- Maybe `slf4j.info(...)` for a logger named `oscal-hub.events` is being filtered (Logback). Check `org.springframework.boot.logging.logback.defaults.xml` for any `<logger>` rules.
- Maybe `LoggerFactory.getLogger("oscal-hub.events")` returns a no-op logger (not impossible if Logback context isn't fully initialized). Verify by adding `System.err.println` next to the slf4j call as a definitive sanity check on a one-off build.

**Recommendation:** First, fix the `springProfile` mismatch — change `<springProfile name="cloud">` to `<springProfile name="gcp">` (and `!cloud` to `!gcp`). Rebuild + redeploy. This alone may fix Bug 2.

**Files:** `back-end/src/main/resources/logback-spring.xml`

---

## What I'd do next, in order

1. **Fix Bug 1** — `@Transactional(readOnly = true)` on `DimensionSyncJob.run()`. Build + push + apply. Verify dimsync populates users/orgs.
2. **Fix Bug 2** — change Logback `springProfile` from `cloud` to `gcp`. Build + push + apply. Verify events appear in `analytics_prod.events`.
3. If Bug 2 isn't fixed by step 2, add `System.err.println("EMIT " + eventName)` to TelemetryService and one more rebuild — that'll prove definitively whether emit is being called at all.
4. **Build the Looker Studio CS dashboard** following `docs/superpowers/plans/2026-05-02-opentelemetry-phase-2-events-bigquery.md` Task 13. Manual UI work, ~30 min.

---

## Build / Docker note

The local `docker buildx` build process repeatedly hung in this session — buildkit went idle for 30+ minutes with no progress. Cause was likely a combination of (a) Docker Desktop's image cache filling to 348 GB reclaimable on a 15.6 GB-RAM host, (b) transient Docker Hub pulls of `docker/dockerfile:1` failing.

`docker buildx prune -af && docker image prune -af` reclaimed 128 GB and the next build worked. Worth running before any future build cycle.

Alternative: trigger a Cloud Build via `gcloud builds submit` to avoid local Docker entirely.

---

## Total commits on `feature/otel-phase-1` for Phase 2 work

```
fb079c5 fix(otel): dimsync writes roles_global ARRAY (matches BQ schema)
6c45681 fix(otel): TelemetryService dual-emits via SLF4J as reliable path
2e82039 fix(otel): dimsync uses STRING for user_id/org_id (BQ schema is STRING, not INT64)
3c6429b fix(otel): Phase 2 deploy fixes (caught at terraform apply)
311ee31 fix(otel): wire DB credentials into dimsync job (was missing)
6cc6888 feat(otel): CS pipeline-health Cloud Monitoring dashboard
39783ee feat(otel): wire analytics modules (Pub/Sub, BigQuery, dimsync) into root
79c4046 feat(otel): Cloud Run Job + Cloud Scheduler for hourly dimension sync
bf21041 feat(otel): DimensionSyncJob mirrors users/orgs from Postgres to BigQuery
9de79e7 feat(otel): collector routes event.name-tagged log records to Pub/Sub
c2618d5 feat(otel): BigQuery dataset, fact + dim tables, four enriched views
1ebf65f feat(otel): Terraform module for analytics Pub/Sub topic + BQ subscription
f32d666 feat(otel): emit business events from primary controllers
2572a31 feat(otel): @Telemetry annotation + AOP aspect for auto-emitting started/completed events
de90fdf feat(otel): TelemetryService for emitting OTel log-event records
16081e4 feat(otel): add EventNames registry for Phase 2 business events
```

15 Phase 2 commits, plus the Phase 1 commits earlier in the same branch. Ready for PR review.
