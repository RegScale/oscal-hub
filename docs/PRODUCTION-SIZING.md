# Production sizing baseline

This document codifies the resource sizing for the GCP production deployment of OSCAL Hub. Every value here has a default in Terraform — change the value here when you change the default in `terraform/gcp/variables.tf`, not the other way around.

The principle: **a baseline that doesn't OOM under normal load + Cloud Run autoscaling above it for traffic spikes**. Cost is roughly $80–$110/month for the baseline; spikes are billable but capped by `max_instances`.

## Cloud SQL — PostgreSQL 15

| Setting | Value | Why |
|---|---|---|
| `db_tier` | `db-custom-1-3840` | 1 vCPU dedicated, 3.75 GB RAM. f1-micro (~614 MB) OOM-restarted under V1.12's catch-up migration on 2026-05-10. f1-micro is documented by Google as not for production. |
| `db_availability_type` | `ZONAL` | REGIONAL doubles cost via a hot standby. Flip via `db_availability_type=REGIONAL` only when HA is genuinely required — for a single-region single-writer workload like this, ZONAL is correct. |
| `db_shared_buffers_8kb_pages` | `115000` | 115000 × 8 KB = ~900 MB ≈ 25% of tier RAM. Postgres canonical rule of thumb. The previous `50000` (~400 MB) on a 614 MB instance left no room for connection backends. |
| `max_connections` | `100` | HikariPool defaults to 10 per Cloud Run instance. 10 instances × 10 = 100 with no headroom for migrations / scheduled jobs / manual psql connections. Bump to 200 if `app_max_instances` rises above 10. |
| disk autoresize | enabled | PD-SSD grows automatically. No manual intervention. |
| backups | enabled, 7-day retention, point-in-time recovery on | Default. |

**When to upgrade tier:** when `pg_stat_database.numbackends` regularly exceeds 60, or instance CPU > 70% sustained, or memory utilization > 85%. Next step up is `db-custom-2-7680` (2 vCPU, 7.5 GB RAM).

## Main app — `oscal-tools-prod`

Combined Spring Boot backend + Next.js frontend in a single Cloud Run v2 service.

| Setting | Value | Why |
|---|---|---|
| `app_cpu` | `2000m` (2 vCPU) | The container runs both the JVM (Spring Boot + OTel agent + Hibernate) and the Node.js Next.js standalone server. 1 vCPU is too tight under cold-start; 2 vCPU absorbs Flyway migration + JIT warmup without exceeding the 60s startup probe window. |
| `app_memory` | `4Gi` | Spring Boot 4 + OTel agent + Hibernate metamodel for ~50 entities + Next.js runtime ≈ 1.5–2 GB steady-state. 4 GB leaves room for Hikari pool, file uploads, and JVM heap headroom (we use `-XX:MaxRAMPercentage=50`). |
| `app_min_instances` | `1` | Always-warm baseline. Spring Boot cold starts are 30–60s; with `min=0`, every login attempt during idle hours hits a cold start and the browser AbortController fires before the server responds. The cost ($25–35/mo) is worth the absence of cold-start outages. |
| `app_max_instances` | `3` | Capped at 3 for cost predictability — observed traffic is low. Each instance idles at ~$35/mo, so the worst-case bill is bounded at ~$105/mo. Raise after observing real load patterns; Cloud SQL `max_connections=100` supports up to 10 instances at 10 Hikari connections each. |
| `app_concurrency` | `80` | Cloud Run v2 default. Each instance handles up to 80 simultaneous HTTP requests before another instance is spawned. The app is mostly I/O-bound (DB calls), so 80 is fine. Lower if you see CPU saturation per instance. |

**Health probes:** startup probe with 60s initial delay + 40 × 10s = 7+ min total budget for cold start. Liveness probe checks every 30s. Both hit `/api/health`.

## OTel collector — `otel-collector-prod`

Routes telemetry from the main app to Cloud Logging / Trace / Pub/Sub.

| Setting | Value | Why |
|---|---|---|
| `otel_collector_cpu` | `1000m` | One vCPU is enough for the current routing connector + GCP exporters at observed span volume. |
| `otel_collector_memory` | `512Mi` | Same — collector is mostly stateless transformation. |
| `otel_collector_min_instances` | `1` | **Required, must stay ≥1.** The OTel Java agent on the main app exports OTLP/gRPC to this service via `OTEL_EXPORTER_OTLP_ENDPOINT`. A cold start on the collector means dropped spans (the agent has only a small in-memory buffer). |
| `otel_collector_max_instances` | `3` | Matches the main app's max — span volume scales linearly with main-app traffic. Capped at 3 for cost predictability; bump if you see span drop counters in Cloud Monitoring. |

**When to bump CPU/memory:** if you see export errors in the collector logs (OOM, queue full) or span drop counters in Cloud Monitoring.

## dimsync — `dimsync-prod` (Cloud Run Job, hourly)

Closest analogue to a "Celery worker" in this codebase. A scheduled batch job that reads recent telemetry from Cloud SQL and fans out dimension data into BigQuery analytics tables. Runs every hour via Cloud Scheduler.

| Setting | Value | Why |
|---|---|---|
| `dimsync_cpu` | `2000m` (2 vCPU) | Defaults to **match the main app**. The dimsync job runs the same Spring Boot image (`oscal-tools:<sha>`), so the cold-start JIT and Hibernate metamodel load cost is the same as the API service's regardless of how small the per-tick workload is. Undersizing causes timeouts during JIT warm-up rather than during the actual sync work. |
| `dimsync_memory` | `4Gi` | Same reasoning — Spring Boot + Hibernate + OTel agent need the same memory headroom as the API service. |
| `dimsync_timeout_seconds` | `600` (10 min) | The job is hourly, so a 10-minute timeout is safe — better to let a slow run finish than retry under load. The job is idempotent. |
| `dimsync_max_retries` | `1` | Low because the hourly trigger gives a natural retry cadence. Higher retries waste compute and risk cascading partial-write states. |

**When to scale up:** if average run duration exceeds 5 min (50% of timeout), bump CPU to 2 or memory to 1Gi. If average duration exceeds 9 min, also bump `dimsync_timeout_seconds`.

## Cost summary (us-central1, mid-2026 prices, approximate)

| Service | Baseline / month | Notes |
|---|---|---|
| Cloud SQL `db-custom-1-3840` ZONAL | ~$25 | 1 vCPU + 3.75 GB RAM + 10 GB SSD |
| oscal-tools-prod (1 always-warm instance, 2 vCPU / 4 GiB) | ~$35–45 | At ZERO traffic. Each additional instance-hour is ~$0.05. |
| otel-collector-prod (1 always-warm, 1 vCPU / 512 MiB) | ~$10–15 | |
| dimsync (hourly job, ~1 min avg, 2 vCPU / 4 GiB) | ~$2–3 | Cloud Run Jobs only bill while running. |
| Networking, logs, Pub/Sub, BigQuery storage | ~$5–10 | Heavily depends on volume. |
| **Total baseline** | **~$80–115/month** | |

Bursts above baseline (extra Cloud Run instances during traffic spikes) are billed per instance-second. With `max_instances=3` on both Cloud Run services, the absolute ceiling is ~$200–250/month under sustained 100% utilization. Raise the cap (and update this doc) only after observing real traffic patterns — most production workloads of this shape never need more than 3 instances.

## How to change a value

Three layers, in increasing scope:

1. **One-off override** for a single deploy: pass `-var="app_max_instances=20"` in the apply step (or set a GitHub Variable like `APP_MAX_INSTANCES` and forward it in `.github/workflows/gcp-deploy.yml`).
2. **New default** for the project: edit the `default = ...` line in `terraform/gcp/variables.tf` *and* the table above. Commit together.
3. **Architectural change** (e.g., upgrading tier): also update this doc's reasoning, the cost summary, and the "when to upgrade" trigger.

Never change a sizing default without updating this doc.
