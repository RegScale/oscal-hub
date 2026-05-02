# OTel Collector Auth Hardening — Design

**Status:** Deferred from Phase 1; not started
**Date:** 2026-05-02
**Owner:** Travis Howerton
**Audience:** Internal engineering

---

## Problem

Phase 1 deployed the OTel Collector as a Cloud Run service that the API service
publishes telemetry to via OTLP/gRPC. The OTel Java agent's gRPC exporter does
not natively send Cloud Run identity tokens, so service-to-service IAM
(`roles/run.invoker` granted only to the API service account) returned
`PERMISSION_DENIED` on every export attempt.

To unblock Phase 1 we added an `allUsers` `roles/run.invoker` binding on the
collector, allowing anyone on the public internet who guesses the collector
URL to publish OTLP. This was an explicit, documented trade-off.

## Why this is not a security crisis

The collector's dedicated service account has only write-only roles:
`cloudtrace.agent`, `monitoring.metricWriter`, `logging.logWriter`. An attacker
who finds the collector URL and submits OTLP traffic can only:

- Spam Cloud Trace with bogus spans
- Spam Cloud Monitoring with bogus metrics
- Spam Cloud Logging with bogus log entries

They cannot exfiltrate any of our existing telemetry, application data, user
data, or credentials. The blast radius is bounded to **noise + cost inflation**.

## Why we still want to fix it

- **Cost:** A motivated attacker could deliberately drive up our Cloud Trace /
  Monitoring / Logging bills.
- **Signal-to-noise:** Bogus traces in Cloud Trace and bogus metrics in
  dashboards would make production debugging harder.
- **Compliance:** OSCAL Hub is a security/compliance product — public ingress
  on any production endpoint is hard to justify on principle, even when
  technically bounded.
- **Phase 3 plan:** Frontend RUM will need to ingest browser-originated OTLP
  on a different path (likely OTLP/HTTP with Cloud-Run-issued auth tokens or
  a dedicated CORS-aware ingestion proxy). Solving the backend auth story
  first lays the groundwork.

## Options considered

### Option A — Cloud Run INTERNAL_ONLY ingress + VPC connector on API service

Set the collector's `ingress` to `INGRESS_TRAFFIC_INTERNAL_ONLY`. Cloud Run
allows internal traffic from same-project Cloud Run services and VPC, but
only when the *caller* originates from a VPC. The API service currently
calls Cloud SQL via `socketFactory` (no VPC connector). Adding a VPC
connector is a real change with real risk of breaking DB connectivity.

**Pros:** Cleanest model. No tokens. Auth handled by GCP at the network layer.
**Cons:** Requires VPC connector on API service. Risk of DB regression.
**Effort:** ~3–4 hours including VPC setup, test, rollout.

### Option B — Shared bearer token via OTel `bearertokenauth` extension

1. Generate a 32-byte random secret, store in Secret Manager.
2. Configure collector OTLP receiver with `auth: bearertokenauth/server`
   that validates `Authorization: Bearer <secret>`.
3. On the API service, set `OTEL_EXPORTER_OTLP_HEADERS=authorization=Bearer <secret>`
   sourced from the Secret Manager binding.
4. Remove the `allUsers` IAM binding from the collector.

**Pros:** No VPC change. Self-contained. Works for Phase 3 frontend if the
secret is kept server-side and the browser uses a proxy endpoint.
**Cons:** Static secret; rotation requires coordinated config updates.
**Effort:** ~2–3 hours.

### Option C — `gcp-auth-extension` on the OTel Java agent

There is no maintained "GCP ID token" extension for the Java agent's OTLP
exporter as of v2.27.0. Building one would require custom Java code that
wraps the gRPC channel with a `MetadataApplier` that calls the GCE metadata
server for an ID token at each request. This is the best long-term fix but
also the most complex.

**Pros:** No tokens to manage. Aligns with GCP's IAM model.
**Cons:** Custom Java code; more moving parts.
**Effort:** ~5–8 hours including writing, testing, packaging the extension jar.

## Recommendation

**Option B (bearer token) for the next iteration.** It removes the public
ingress with the least architectural change and the least risk to production
DB connectivity. Option A is the long-term right answer once we add a VPC
connector for other reasons (e.g., private Cloud SQL); we should revisit
when that change is needed for an unrelated reason. Option C is too much
custom Java code to maintain for this purpose.

## Out of scope for this design

- Token rotation strategy (use Secret Manager secret versions, plan rotation
  every 90 days; full automation is a follow-up)
- Frontend RUM ingestion path (Phase 3 design)
- Mutual TLS between API and collector (overkill given the closed
  network and rotated bearer)
