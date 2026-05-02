# OpenTelemetry Instrumentation & Analytics — Design

**Status:** Draft for review
**Date:** 2026-05-01
**Owner:** Travis Howerton
**Audience:** Internal engineering, ops, and customer success teams

---

## Summary

End-to-end OpenTelemetry instrumentation for OSCAL Hub (Spring Boot backend +
Next.js frontend), exporting traces, metrics, logs, and business events through
a dedicated OTel Collector service into Google Cloud's native observability
backends and BigQuery. Three internal-facing dashboards consume the data:
Customer Success (Looker Studio on BigQuery), Operations (Cloud Monitoring),
and Developer (Cloud Monitoring + Cloud Trace deep links).

## Goals

- See **who** is using the app at user and organization level.
- See **what features** are most popular and how they trend.
- See **how long** every API call and user-visible operation takes, end-to-end.
- See **errors** with full trace context so devs can root-cause quickly.
- Dashboards usable by non-engineers (CS) without ad-hoc SQL.
- All data stays inside Google Cloud (already the deploy target).

## Non-goals

- Customer-facing or per-org self-service dashboards (deferred — see "Future").
- Session replay or full network waterfall capture.
- Self-hosted Grafana/Prometheus/Tempo/Loki (defeats serverless posture).
- Per-customer telemetry isolation beyond opaque IDs.

## Architecture overview

```
┌────────────────┐        ┌────────────────┐         ┌─────────────────┐
│  Next.js app   │        │  Spring Boot   │         │ Cloud Run job   │
│  (browser)     │        │  back-end      │         │ DimensionSync   │
│                │        │                │         │                 │
│ OTel Web SDK   │OTLP/   │ OTel Java agent│ OTLP/   │ Postgres → BQ   │
│ Full RUM       │HTTP    │ + custom spans │ gRPC    │ users, orgs     │
│ + custom track │───┐    │ + Micrometer   │──┐      │ hourly cron     │
└────────────────┘   │    └────────────────┘  │      └────────┬────────┘
                     │                        │               │
                     ▼                        ▼               │
              ┌──────────────────────────────────────┐        │
              │  OTel Collector (Cloud Run service)  │        │
              │  - sampling, batching, redaction     │        │
              │  - PII scrubbing                     │        │
              │  - exporter fan-out                  │        │
              └──┬──────────┬──────────┬──────────┬──┘        │
                 │          │          │          │           │
            traces      metrics      logs    business events  │
                 ▼          ▼          ▼          ▼           ▼
          ┌──────────┐┌──────────┐┌──────────┐┌─────────────────┐
          │  Cloud   ││  Cloud   ││  Cloud   ││  Pub/Sub →       │
          │  Trace   ││Monitoring││ Logging  ││  BigQuery        │
          └────┬─────┘└────┬─────┘└────┬─────┘└────────┬────────┘
               │           │           │               │
               └───────────┴───────────┴───────────────┘
                                   │
                          ┌────────┴─────────┐
                          │ Cloud Monitoring │ ← Ops + Dev dashboards
                          │ Looker Studio    │ ← CS dashboard
                          └──────────────────┘
```

## Data model: three signals, clear boundaries

Telemetry is split into three signals with explicit rules so nothing is
double-counted or miscategorized.

### Spans (Cloud Trace)

End-to-end traces of every request. Auto-instrumentation provides:

- HTTP server spans for every controller endpoint
- HTTP client spans for outbound calls
- JDBC spans for every DB query (sanitized SQL)
- Spring scheduling and `@Async` spans
- Frontend `fetch` spans, document load, route change
- Frontend user-interaction spans (Full RUM)

Every span carries baggage: `user.id`, `org.id`, `user.role.global`,
`user.role.org`, `session.id`, `deployment.environment`. Baggage is set once at
`back-end/src/main/java/.../security/JwtAuthenticationFilter.java:94`
(immediately after `setAuthentication`) and propagates via W3C Trace Context
headers across the frontend↔backend boundary.

### Metrics (Cloud Monitoring via Managed Service for Prometheus)

Aggregated counters and histograms — low cardinality, cheap to retain.

Existing Micrometer metrics (JVM, HTTP server, JDBC pool) flow through unchanged
via the Micrometer→OTel bridge. New domain metrics:

- `oscal_operation_duration_seconds{operation, model, format, outcome}`
- `oscal_operation_total{operation, model, format, outcome}`
- `oscal_auth_events_total{event, outcome}`
- `oscal_authorization_actions_total{action, outcome}`
- `oscal_storage_bytes{kind}`
- `web_vital_lcp_ms`, `web_vital_inp_ms`, `web_vital_cls` (frontend)
- `js_error_total{component, error_type}` (frontend)

**Cardinality rule:** `user_id` and `org_id` are never metric labels. They live
only in spans (low retention) and events (BigQuery, designed for high
cardinality).

### Business events (BigQuery)

Discrete user actions with full attribute payloads. Always 100% sampled. This
is the data CS dashboards run on. Emitted as OTel Log records with an
`event.name` attribute (the OTel convention for events) and routed by the
collector to a Pub/Sub topic with a BigQuery subscription.

Initial event catalog (extensible via single registry):

| Event | When | Key attributes |
|---|---|---|
| `auth.login_succeeded` | Successful `/api/auth/login` | `user_id`, `org_id`, `mfa_used`, `ip_country` |
| `auth.login_failed` | Failed login | `attempted_username` (sha256), `reason`, `ip_country` |
| `auth.session_started` | First page load with valid token | `user_id`, `org_id`, `session_id` |
| `auth.session_ended` | Logout / timeout | `session_id`, `duration_ms` |
| `feature.viewed` | Frontend route change | `route`, `user_id`, `org_id`, `referrer` |
| `oscal.validate_started` / `_completed` | Validate API | `model`, `format`, `bytes_in`, `duration_ms`, `outcome`, `error_count` |
| `oscal.convert_started` / `_completed` | Convert API | `model`, `from_format`, `to_format`, `bytes_in`, `duration_ms`, `outcome` |
| `oscal.resolve_started` / `_completed` | Profile resolve API | `bytes_in`, `controls_resolved`, `duration_ms`, `outcome` |
| `oscal.batch_submitted` / `_completed` | Batch ops | `op_type`, `item_count`, `total_duration_ms`, `success_count`, `failure_count` |
| `library.item_uploaded` / `_downloaded` / `_deleted` | Library API | `item_kind`, `bytes`, `org_storage_used` |
| `authorization.template_created` / `_approved` / `_rejected` | Authorization workflow | `template_kind`, `approver_id`, `reviewer_count` |
| `artifact.uploaded` / `_downloaded` | Artifact API | `kind`, `bytes` |
| `admin.user_invited` / `_role_changed` / `_deactivated` | Admin actions | `actor_id`, `target_id`, `change_summary` |
| `error.unhandled` | Backend exception caught in `@ControllerAdvice` | `exception_class`, `endpoint`, `status` |
| `error.frontend_js` | window.onerror / unhandledrejection | `error_class`, `route`, `user_agent_class` |

The `_started`/`_completed` pair lets CS analyze drop-off (users who started a
validate but didn't complete).

## Backend instrumentation

### OTel Java agent

Attached via `JAVA_TOOL_OPTIONS=-javaagent:/otel/opentelemetry-javaagent.jar`
in the multi-stage Dockerfile. Agent jar copied in from the official
`otel-javaagent` image. Configured via env vars on the Cloud Run service:

- `OTEL_SERVICE_NAME=oscal-api`
- `OTEL_EXPORTER_OTLP_ENDPOINT=https://otel-collector-${env}-xxx.run.app`
- `OTEL_EXPORTER_OTLP_PROTOCOL=grpc`
- `OTEL_RESOURCE_ATTRIBUTES=service.namespace=oscal-hub,deployment.environment=${env},service.version=${GIT_SHA}`
- `OTEL_TRACES_SAMPLER=parentbased_always_on` (sampling decision is made at the collector via tail-sampling, not in the app)
- `OTEL_LOGS_EXPORTER=otlp`, `OTEL_METRICS_EXPORTER=otlp`

### Baggage hook

Extend `JwtAuthenticationFilter.doFilterInternal` after the existing
`setAuthentication` call:

```java
Baggage.current().toBuilder()
    .put("user.id", userId)
    .put("org.id", orgId)
    .put("user.role.global", globalRole)
    .put("user.role.org", orgRole)
    .build()
    .makeCurrent();
```

The collector's `transform` processor promotes baggage attributes to span
attributes, so every downstream span carries org/user. A Logback MDC converter
populates `trace_id`, `span_id`, `user.id`, `org.id` from the active context so
log lines correlate in Cloud Logging.

### Custom telemetry surface

Three new components in `back-end/src/main/java/.../telemetry/`:

1. **`TelemetryService.emit(EventName name, Map<String,Object> attrs)`** —
   constructs an OTel Log record with `event.name`, attaches current baggage,
   sends to the configured logger provider. Routed by the collector to the
   events pipeline.

2. **`@Telemetry("oscal.validate")` annotation + `TelemetryAspect`** —
   AOP wrapper that auto-emits `<name>_started` and `<name>_completed` events
   with `duration_ms` and `outcome=success|failure`. Failure emits
   `error_class` and `error_message`. Applied to controller methods for the
   high-volume operations.

3. **Micrometer→OTel bridge** —
   `io.opentelemetry.instrumentation:opentelemetry-micrometer-1.5`
   already on `MeterRegistry`. New domain counters/timers added to the same
   registry; existing metrics flow through unchanged.

### Custom spans for internal phases

`@WithSpan` applied to the heavy work inside OSCAL ops so devs can see exactly
where time goes:

- `OscalLoader.parse` (input parsing)
- `ProfileResolver.resolve` (the expensive step in resolve)
- `ValidationEngine.runConstraints`
- `FileStorageService.upload` / `.download`

## Frontend instrumentation

### Bootstrap

New module `front-end/src/lib/telemetry/index.ts`, initialized from
`app/layout.tsx`:

- `@opentelemetry/sdk-trace-web` with `BatchSpanProcessor`
- Exporter: `@opentelemetry/exporter-trace-otlp-http` →
  `${NEXT_PUBLIC_OTEL_COLLECTOR_URL}/v1/traces`
- Resource attributes: `service.name=oscal-web`, `deployment.environment`,
  `service.version` (build-time env)
- Propagator: W3C Trace Context (so backend stitches frontend traces)

### Auto-instrumentations (Full RUM)

- `instrumentation-fetch` — wraps the existing api-client. URL allowlist:
  same-origin `/api/*` only. Cross-origin requests do not get trace headers
  injected.
- `instrumentation-document-load` — initial paint timing.
- `instrumentation-user-interaction` — clicks/submits, allowlist of CSS
  selectors, no value capture.
- `web-vitals` library, piped to OTel metrics for LCP/INP/CLS.
- Custom error reporter wired to `window.addEventListener('error')`,
  `unhandledrejection`, and a top-level Next.js Error Boundary.

### PII safeguards (hard-wired)

- `instrumentation-user-interaction` config: `eventNames: ['click', 'submit']`,
  `shouldPreventSpanCreation: (event, el) => el.hasAttribute('data-oscal-sensitive')`.
- All `<textarea>`, file inputs, and elements with class `oscal-content` or
  `oscal-secret` get `data-oscal-sensitive` automatically via a small wrapper
  component.
- Fetch request and response bodies are never captured (default behavior; we
  assert it explicitly in config and tests).
- URL query strings are stripped of `token`, `secret`, `password`, `key`
  before becoming span attributes.

### `useTelemetry()` hook

Exposes `track(eventName, attrs)` and `withTelemetry(fn, eventName)` for
wrapping mutations. A Next.js `usePathname()` listener emits `feature.viewed`
on every route change. `AuthContext.login()` calls `telemetry.identify()`;
`AuthContext.logout()` calls `telemetry.clear()`.

## Collector configuration

Deployed as a separate Cloud Run service `otel-collector-${env}` running
`otel/opentelemetry-collector-contrib`. Public ingestion endpoint with TLS;
CORS configured for the frontend origin.

- Min instances: 1 (avoids cold-start latency on telemetry)
- Max instances: 10, concurrency 100
- Memory limit + queue backpressure so a burst can't take it down
- Identity: dedicated GSA with least-privilege per pipeline:
  `roles/cloudtrace.agent`, `roles/monitoring.metricWriter`,
  `roles/logging.logWriter`, `roles/pubsub.publisher`

### Pipelines

```yaml
receivers:
  otlp:
    protocols:
      grpc: { endpoint: 0.0.0.0:4317 }
      http:
        endpoint: 0.0.0.0:4318
        cors: { allowed_origins: [https://app.oscalhub.example] }

processors:
  batch: {}
  resourcedetection: { detectors: [gcp, env] }
  attributes/redact:
    actions:
      - key: http.url
        action: update
        pattern: "(token|secret|password|key)=[^&]+"
        replacement: "$1=REDACTED"
      - key: "*"
        action: update
        pattern: "[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}"
        replacement: "EMAIL_REDACTED"
  transform/baggage_to_attrs: { ... }   # promote user.id, org.id, etc.
  tail_sampling:
    policies:
      - { name: errors, type: status_code, status_code: { status_codes: [ERROR] } }
      - { name: baseline, type: probabilistic, probabilistic: { sampling_percentage: 25 } }
  routing/events:
    from_attribute: event.name
    table:
      - value: ".+"   # has event.name → events pipeline
        exporters: [googlecloudpubsub]

exporters:
  googlecloud: {}
  googlemanagedprometheus: {}
  googlecloudpubsub: { topic: projects/${PROJECT}/topics/otel-events }

service:
  pipelines:
    traces:  { receivers: [otlp], processors: [batch, resourcedetection, attributes/redact, transform/baggage_to_attrs, tail_sampling], exporters: [googlecloud] }
    metrics: { receivers: [otlp], processors: [batch, resourcedetection], exporters: [googlemanagedprometheus] }
    logs:    { receivers: [otlp], processors: [batch, resourcedetection, attributes/redact, routing/events], exporters: [googlecloud] }
```

## Data stores: BigQuery dataset `analytics`

Region `us-central1`, customer-managed encryption via the existing KMS key.

### Fact table: `events`

Partitioned daily on `event_time`, clustered on `org_id`, `event_name`.

| Column | Type | Notes |
|---|---|---|
| `event_time` | TIMESTAMP | required |
| `event_name` | STRING | required |
| `event_id` | STRING | required, UUID for dedup on Pub/Sub redelivery |
| `trace_id` | STRING | joins to Cloud Trace |
| `span_id` | STRING | |
| `session_id` | STRING | |
| `user_id` | STRING | opaque, joins to `users` dim |
| `org_id` | STRING | opaque, joins to `orgs` dim |
| `service_name` | STRING | `oscal-api` or `oscal-web` |
| `service_version` | STRING | git SHA |
| `environment` | STRING | dev/staging/prod |
| `attributes` | JSON | event-specific payload |
| `ingested_at` | TIMESTAMP | required |

### Dimension table: `users`

Synced hourly from Postgres by `DimensionSyncJob`.

| Column | Type |
|---|---|
| `user_id` | STRING |
| `username` | STRING |
| `email` | STRING |
| `first_name` | STRING |
| `last_name` | STRING |
| `org_id_primary` | STRING |
| `roles_global` | ARRAY<STRING> |
| `created_at` | TIMESTAMP |
| `last_login` | TIMESTAMP |
| `active` | BOOL |
| `synced_at` | TIMESTAMP |

### Dimension table: `orgs`

| Column | Type |
|---|---|
| `org_id` | STRING |
| `name` | STRING |
| `description` | STRING |
| `active` | BOOL |
| `member_count` | INT64 |
| `created_at` | TIMESTAMP |
| `synced_at` | TIMESTAMP |

### Sync job

`back-end/src/main/java/.../telemetry/DimensionSyncJob.java`. `@Scheduled(cron
= "0 0 * * * *")` runs hourly. Reads Postgres, writes to BigQuery via `MERGE`
(upsert by id). Deployed as a separate Cloud Run job triggered by Cloud
Scheduler so it does not share resources with the API. Includes a tombstone
step: rows present in BigQuery but absent in Postgres are flagged `active =
false`. Right-to-be-forgotten propagates within an hour of app-side delete.

### Views

For Looker Studio (cheaper than authorized views):

- `vw_events_enriched` — `events` LEFT JOIN `orgs` USING(`org_id`) LEFT JOIN
  `users` USING(`user_id`)
- `vw_daily_active_orgs` — `event_time → date`, distinct `org_id` count by day
- `vw_feature_popularity` — `event_name`, count, distinct users, p50/p95
  `duration_ms`
- `vw_session_durations` — joins `auth.session_started` /
  `auth.session_ended` pairs

## Pub/Sub

- Topic: `otel-events`
- Subscription: `otel-events-bq` (BigQuery subscription, writes to
  `analytics.events`)
- Dead-letter topic: `otel-events-dlq`, max delivery attempts 5
- Message ordering disabled (events are independent)
- Exactly-once delivery enabled

## Dashboards

### Customer Success (Looker Studio, primary)

One report with multiple pages, BigQuery-backed via service account, refresh
hourly. Access via GCP IAM group `cs-team@`.

**Page 1 — Org overview**

- Big-number tiles: total orgs, DAU, WAU, MAU
- Org leaderboard (top 25 by ops in last 30d): org name, total ops, distinct
  users, last active, primary feature
- New orgs in last 30d (line chart)
- Inactive orgs (>14d since last event) — risk list

**Page 2 — User overview**

- DAU / WAU / MAU lines, last 90d
- New users by org (heatmap)
- Top users by activity, with org affiliation
- Time-to-first-action funnel: register → first login → first
  validate/convert/resolve

**Page 3 — Feature popularity**

- Stacked bar of event counts by `event_name`, last 30d
- Per-feature: distinct users, distinct orgs, p50/p95 duration, success rate
- Funnel for batch ops: submitted → completed → success
- Trend lines per feature, week-over-week % change

**Page 4 — Drill-down**

- Filter by org or user → all their events, sessions, last touchpoints, error
  count
- Used by CS reps before customer calls

### Operations (Cloud Monitoring)

JSON dashboard committed to `terraform/gcp/dashboards/ops-dashboard.json`,
applied via `google_monitoring_dashboard` resource so it is reproducible.

- Cloud Run: instance count, CPU, memory, concurrency per service
  (api, web, otel-collector)
- Request rate (RPS) per service, stacked by 2xx/3xx/4xx/5xx
- p50 / p95 / p99 latency per service (heatmap)
- Cloud SQL: connection count, query rate, slow query count, replica lag,
  disk used %
- GCS: bucket size by bucket, egress GB/day
- Pub/Sub: backlog age and depth (`otel-events`), dead-letter count
- Cold-start frequency per service
- Budget burn (daily $ from billing export, projected month total)
- Error rate alert chart with policy thresholds drawn

### Developer (Cloud Monitoring + Cloud Trace)

- p95/p99 latency by endpoint (top 20)
- Error rate by endpoint (top 10), each tile linked to a Cloud Logging filter
  pre-scoped to `severity>=ERROR AND http.route=...`
- Top exceptions in last 24h (from `error.unhandled` events): class, count,
  last seen, link to representative Cloud Trace
- DB query duration breakdown (top 10 sanitized statements)
- External call latency (`OscalLoader.parse`, `ProfileResolver.resolve`, etc.)
- Frontend Core Web Vitals: LCP, INP, CLS (p75) per route
- Frontend JS error rate by route
- Sampling rate gauge (so devs know whether they're seeing 100% or 25%)

### Alert policies

Committed alongside dashboards.

- 5xx rate > 1% over 5min → Slack `#oscal-alerts`
- p99 latency > 5s on `/api/validate|convert|resolve` over 10min
- Cloud SQL connection saturation > 80%
- Cloud Run concurrency utilization > 80%
- Pub/Sub `otel-events` backlog age > 5min (telemetry pipeline stuck)
- Budget burn > 1.5× projected → email
- Failed login rate > 50/min over 5min → security pager

## Privacy, sampling, retention

### PII boundary

- Spans/metrics/Cloud Trace: opaque IDs only (`user_id`, `org_id`)
- BigQuery `events`: opaque IDs only — names/emails come from dimension join
- Collector regex-redact processor strips emails, JWTs, IPv4/IPv6 patterns
  from any attribute that leaks them
- Frontend never captures form values, file contents, or response bodies

### Sampling

- Traces: tail-sampled at the collector — 100% of error spans + 25% baseline
  in prod, 100% in dev/staging
- Metrics: never sampled
- Business events: never sampled (analytics signal must be exact)

### Retention

- Cloud Trace: 30 days (default)
- Cloud Logging: 30 days, with a sink to GCS for compliance archives if needed
- Cloud Monitoring metrics: 24 months (default for managed Prometheus)
- BigQuery `events`: 400 days (partition TTL, covers YoY analysis)
- BigQuery dimensions: indefinite, but right-to-be-forgotten propagates from
  app DB hourly

### GDPR / CCPA stance

- "Erase me" workflow: app deletes user from Postgres → next dimension sync
  drops the row → Looker Studio joins return null/anonymized
- `events` rows retain only opaque ID; without a dimension row they are
  unattributable
- Optional hard-delete job runs nightly on `events` for any `user_id` flagged
  in a `deletion_requests` table

## Cost projection

Rough; actual depends on traffic. Assumes ~1k DAU and ~50 ops/user/day.

| Component | Monthly cost |
|---|---|
| Cloud Trace | $0 (under 2.5M-span free tier) |
| Cloud Monitoring (managed Prometheus + custom metrics) | $5–15 |
| Cloud Logging | $5–10 (most logs free under 50 GiB) |
| BigQuery (storage + queries) | $1–8 |
| Pub/Sub | ~$1 |
| OTel Collector Cloud Run service | ~$10 |
| **Total** | **~$25–50/mo at current scale** |

Scales roughly linearly with traffic.

## Phasing

Single spec, phased implementation plan.

### Phase 1 — Backend tracing + Ops dashboard (foundation)

- Add OTel Java agent to backend Dockerfile
- Wire baggage in `JwtAuthenticationFilter` + Logback MDC bridge
- Deploy collector Cloud Run service with traces + metrics + logs pipelines
  (no events yet)
- Add Cloud Trace + Ops dashboard JSON to Terraform
- Verify end-to-end on staging: trace from browser fetch → controller → DB
  visible in Cloud Trace
- Ship behind `OTEL_ENABLED=true` flag for staged rollout

### Phase 2 — Business events + BigQuery + CS dashboard

- Add `TelemetryService` + `@Telemetry` annotation + event emissions on
  controllers per the catalog
- Add Pub/Sub topic + BigQuery subscription + `events` table via Terraform
- Build `DimensionSyncJob` and Cloud Scheduler entry
- Build Looker Studio CS report from views

### Phase 3 — Frontend RUM + Dev dashboard polish

- Add OTel Web SDK bootstrap + auto-instrumentations + PII safeguards
- Wire `useTelemetry` and route-change events
- Add Core Web Vitals + JS error reporter
- Build Dev dashboard JSON, alert policies

### Phase 4 — Hardening

- Tail-sampler tuning based on actual cost data after 2 weeks
- Right-to-be-forgotten workflow + tests
- Runbooks for collector outage, Pub/Sub backlog, BQ schema migration

## Testing strategy

- **Backend unit:** `TelemetryService` with mocked exporter; verify event
  attributes and baggage propagation
- **Backend integration:** Testcontainers + the OTel collector image; assert
  that a known span and a known event reach the collector with expected
  attributes
- **Frontend unit:** jest tests on `useTelemetry` and the api-client wrapper
- **Frontend e2e:** Playwright asserts that navigating to a route emits a
  `feature.viewed` event hitting a stub collector
- **Collector config:** `otelcol validate` in CI; golden-file tests for the
  redaction processor
- **Schema:** CI step runs `bq query --dry_run` against `vw_events_enriched`
  to detect schema drift

## Risks and mitigations

| Risk | Mitigation |
|---|---|
| Collector outage drops telemetry | App-side OTel SDK has bounded queue + retry; collector has min-1-instance and queued retries; alert on Pub/Sub backlog age |
| Cardinality explosion on metric labels | Code review rule + Prometheus `up` cardinality alert; `user_id`/`org_id` reserved for spans/events only |
| PII leaks into Cloud Trace | Collector redact processor + golden-file tests; review checklist for new spans |
| Cost surprise from a runaway feature | Budget burn alert at 1.5× projected; tail-sampling tuned after 2 weeks of real data |
| Frontend bundle bloat from Full RUM | Bundle-size CI budget; tree-shake unused instrumentations |
| Stale dimension data → wrong CS reports | Sync job alert if last successful run > 2 hours; CS dashboard shows "data freshness" timestamp |
| Right-to-be-forgotten misses old events | Nightly hard-delete job + audit query of orphaned `user_id`s |

## Open questions

None blocking. Things to revisit after phase 1:

- Whether to add per-org self-service usage view in-app (user said "later")
- Whether tail-sampling % needs adjustment based on real volumes
- Whether to add Cloud Profiler for the OSCAL parsing hot paths

## Future work (out of scope)

- Customer-facing per-org dashboards (SaaS-style usage page)
- SLO definitions + error budget burn alerts
- Cross-trace correlation with Cloud SQL Insights query stats
- Cloud Profiler integration for OSCAL parser hot paths
- Anomaly detection on event volumes per org (churn/security signal)
