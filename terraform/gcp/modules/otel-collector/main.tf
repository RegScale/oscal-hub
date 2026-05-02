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
        container_port = 4317 # OTLP/gRPC primary
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

# Phase 1: allow unauthenticated invocation of the collector.
# The OTel Java agent's gRPC exporter doesn't send Cloud Run ID tokens
# natively; service-to-service auth requires either a custom auth
# extension or a sidecar token-injecting proxy. For Phase 1 we accept
# the trade-off: the collector's blast radius is low — its dedicated GSA
# only has write-only roles (cloudtrace.agent, monitoring.metricWriter,
# logging.logWriter), no read scope. An attacker who guesses the URL
# can only spam telemetry data, not exfiltrate anything. Phase 4
# hardening will add a GCP auth extension or VPC-only ingress.
resource "google_cloud_run_service_iam_member" "all_users_invoker" {
  project  = var.project_id
  location = var.region
  service  = google_cloud_run_v2_service.collector.name
  role     = "roles/run.invoker"
  member   = "allUsers"
}

# Keep the API service binding too (defense in depth — if Phase 4 strips
# allUsers, the API service still works).
resource "google_cloud_run_service_iam_member" "api_invoker" {
  project  = var.project_id
  location = var.region
  service  = google_cloud_run_v2_service.collector.name
  role     = "roles/run.invoker"
  member   = "serviceAccount:${var.api_service_account}"
}

# Allow the collector to publish to the analytics Pub/Sub topic.
# Optional — only created when events_topic_name is provided.
resource "google_pubsub_topic_iam_member" "publisher" {
  count   = var.events_topic_name != "" ? 1 : 0
  project = var.project_id
  topic   = var.events_topic_name
  role    = "roles/pubsub.publisher"
  member  = "serviceAccount:${google_service_account.collector.email}"
}
