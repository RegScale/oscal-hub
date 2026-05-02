# Monitoring

OSCAL Hub runs on Google Cloud with end-to-end OpenTelemetry instrumentation.
This page is the entry point for finding the right tool for the job.

---

## Dashboards

### Operations — Cloud Monitoring

Real-time health of Cloud Run services, Cloud SQL, Pub/Sub, dimsync job runs.
Use this when something feels slow or you're investigating an alert.

**[OSCAL Hub — Operations](https://console.cloud.google.com/monitoring/dashboards/builder/be31b22d-dea7-49d8-a568-079b87445a6a;duration=PT1H?project=oscal-hub)**

Tiles:
- Request rate (RPS) per service
- p95 latency per service
- Cloud Run instance count, CPU and memory utilization
- Cloud SQL connection count + CPU

### CS Pipeline Health — Cloud Monitoring

Health of the analytics data plane: Pub/Sub publish rate, BigQuery insert
errors, dimsync job execution counts. Watch this if events stop flowing into
BigQuery.

**[OSCAL Hub — CS Pipeline Health](https://console.cloud.google.com/monitoring/dashboards/builder/54f9967c-d38c-43e1-a909-f59e41081ce4;duration=PT1H?project=oscal-hub)**

### Customer Success — Looker Studio (to be built)

The CS dashboard isn't built yet — the data pipeline is live and queryable
(see [`vw_events`](#bigquery--analytics_prod-dataset) below) but Looker Studio
configuration is manual UI work.

**Runbook:** [Phase 2 plan, Task 13](docs/superpowers/plans/2026-05-02-opentelemetry-phase-2-events-bigquery.md#task-13-looker-studio-cs-dashboard-manual)
walks through the setup step-by-step. Roughly:

1. New Looker Studio report → data source: BigQuery → `oscal-hub.analytics_prod.vw_events`
2. Add `users` and `orgs` as additional data sources (for `user_email` / `org_name` enrichment via blends on `user_id` / `org_id`)
3. Build four pages: Org overview, User overview, Feature popularity, Drill-down
4. Share with the CS team via GCP IAM group

The plan file has the full per-page tile spec.

---

## Tracing

### Cloud Trace — every API request

Every request hits the OSCAL backend gets traced end-to-end. Spans carry
`user.id`, `org.id`, `user.role.global`, `user.role.org` from the JWT (set in
`JwtAuthenticationFilter` and propagated as OTel baggage).

**[Cloud Trace explorer](https://console.cloud.google.com/traces/list?project=oscal-hub)**

Tip: filter by service name `oscal-api` and use attribute filters to find a
specific user's traces, e.g. `user.id = "456"`.

---

## Logs

### Cloud Logging — all application logs

Structured JSON logs with `trace_id` / `span_id` / `user.id` / `org.id` MDC
fields. Click any log line to jump to its associated trace.

**[Logs explorer (oscal-tools-prod)](https://console.cloud.google.com/logs/query;query=resource.labels.service_name%3D%22oscal-tools-prod%22?project=oscal-hub)**

### Business events stream

The `oscal-hub.events` SLF4J logger is where `TelemetryService.emit(...)`
calls land before the BigQuery sink picks them up. Useful for live tailing
during debugging.

**[Live events stream](https://console.cloud.google.com/logs/query;query=labels.%22instrumentation_source%22%3D%22oscal-hub.events%22?project=oscal-hub)**

---

## BigQuery — `analytics_prod` dataset

Long-term analytics store. Sink ingest is near-real-time (~1–2 min latency).

**[BigQuery console — analytics_prod](https://console.cloud.google.com/bigquery?project=oscal-hub&ws=!1m4!1m3!3m2!1soscal-hub!2sanalytics_prod)**

| Object | What it is |
|---|---|
| `vw_events` | Friendly view: flattened events from the sink table — `event_time`, `event_name`, `trace_id`, `user_attempted_sha256`, `reason`, `attributes`, etc. **Use this in Looker Studio.** |
| `oscal_hub_collector` | Raw sink table — wide schema with nested `labels.*`. Useful for ad-hoc SQL when `vw_events` doesn't expose the field you need. |
| `users` | Dimension table. Synced hourly from Postgres by `dimsync-prod` Cloud Run Job. |
| `orgs` | Dimension table. Same hourly sync. |
| `vw_events_enriched` | Join of events × users × orgs (gives you `user_email`, `org_name` directly). |
| `vw_daily_active_orgs` | Daily DAU/active-org rollup. |
| `vw_feature_popularity` | Per-event aggregates: counts, distinct users/orgs, p50/p95 duration, success rate. |
| `events` | (Reserved) Original Phase 2 schema for the direct Pub/Sub→BQ path. **Not currently populated** — events go through the sink instead. |

Quick sanity query:

```sql
SELECT event_name, COUNT(*) AS n
FROM `oscal-hub.analytics_prod.vw_events`
WHERE event_time > TIMESTAMP_SUB(CURRENT_TIMESTAMP(), INTERVAL 1 HOUR)
GROUP BY event_name
ORDER BY n DESC
```

---

## Alerts

Four Cloud Monitoring alert policies are armed. Edit notification channel via
`var.alert_email` in `terraform/gcp/terraform.tfvars`.

**[Alert policies list](https://console.cloud.google.com/monitoring/alerting/policies?project=oscal-hub)**

| Policy | Threshold |
|---|---|
| Cloud Run 5xx rate | > 1% over 5min |
| p99 latency on validate/convert/resolve | > 5s over 10min |
| Cloud SQL connection saturation | > 80% over 5min |
| Cloud Run CPU utilization (p99) | > 80% over 5min |

---

## Pub/Sub & sink (for ops debugging)

The events ingestion path:

```
TelemetryService.emit() (Java)
    → SLF4J logger "oscal-hub.events"
    → OTel agent's Logback bridge
    → OTLP/gRPC to otel-collector-prod
    → googlecloud exporter
    → Cloud Logging (logName=oscal-hub-collector)
    → Cloud Logging sink "oscal-events-prod"
    → BigQuery analytics_prod.oscal_hub_collector
    → vw_events friendly view
```

Each hop has a console URL:

- **OTel Collector service:** [`otel-collector-prod`](https://console.cloud.google.com/run/detail/us-central1/otel-collector-prod/metrics?project=oscal-hub)
- **Cloud Logging sink:** [`oscal-events-prod`](https://console.cloud.google.com/logs/router?project=oscal-hub)
- **Pub/Sub topic** (kept for future direct-to-BQ when reshaping is added): [`otel-events-prod`](https://console.cloud.google.com/cloudpubsub/topic/detail/otel-events-prod?project=oscal-hub)

---

## Dimsync — Postgres → BigQuery dimension refresh

Cloud Run Job that mirrors `users` and `organizations` tables to BigQuery
hourly. Triggered by Cloud Scheduler at minute 0 of every hour.

**[Job overview](https://console.cloud.google.com/run/jobs/details/us-central1/dimsync-prod/executions?project=oscal-hub)** ·
**[Scheduler](https://console.cloud.google.com/cloudscheduler?project=oscal-hub)**

Manually trigger:
```bash
gcloud run jobs execute dimsync-prod --region=us-central1 --wait
```

---

## Design + plan documents

| Document | Use when |
|---|---|
| [Design spec](docs/superpowers/specs/2026-05-01-opentelemetry-design.md) | Why we built it this way; PII boundary, sampling, data model decisions |
| [Phase 1 plan](docs/superpowers/plans/2026-05-01-opentelemetry-phase-1-backend.md) | Backend instrumentation, collector, ops dashboard, alerts |
| [Phase 2 plan](docs/superpowers/plans/2026-05-02-opentelemetry-phase-2-events-bigquery.md) | TelemetryService, BQ analytics, dimsync, **Looker Studio CS dashboard runbook (Task 13)** |
| [Phase 2 follow-ups](docs/superpowers/specs/2026-05-02-otel-phase-2-followups.md) | What was outstanding mid-rollout (most resolved) |
| [Collector auth hardening](docs/superpowers/specs/2026-05-02-otel-collector-auth-hardening.md) | Replace `allUsers` IAM on collector with bearer token or VPC ingress (Phase 4 work) |
