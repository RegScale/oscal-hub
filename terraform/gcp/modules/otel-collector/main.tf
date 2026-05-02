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

# Allow only the API service account to invoke (publish telemetry to) the
# collector. The browser will be allowed in Phase 3 via a different binding.
resource "google_cloud_run_service_iam_member" "api_invoker" {
  project  = var.project_id
  location = var.region
  service  = google_cloud_run_v2_service.collector.name
  role     = "roles/run.invoker"
  member   = "serviceAccount:${var.api_service_account}"
}
