# ============================================================================#
# Cloud Run Service Module
# ============================================================================#

resource "google_cloud_run_v2_service" "service" {
  name     = "${var.service_name}-${var.environment}"
  location = var.region
  project  = var.project_id

  template {
    # Scaling configuration
    scaling {
      min_instance_count = var.min_instances
      max_instance_count = var.max_instances
    }

    # VPC Access (for Cloud SQL private connections)
    dynamic "vpc_access" {
      for_each = var.vpc_connector_id != null && var.vpc_connector_id != "" ? [1] : []
      content {
        connector = var.vpc_connector_id
        egress    = "PRIVATE_RANGES_ONLY"
      }
    }

    # Container configuration
    containers {
      image = var.image

      # Resource limits
      resources {
        limits = {
          cpu    = var.cpu_limit
          memory = var.memory_limit
        }
        cpu_idle          = true
        startup_cpu_boost = true
      }

      # Environment variables from direct values
      dynamic "env" {
        for_each = var.environment_variables
        content {
          name  = env.key
          value = env.value
        }
      }

      # DB_URL environment variable (special handling)
      dynamic "env" {
        for_each = var.db_url != "" ? [1] : []
        content {
          name  = "DB_URL"
          value = var.db_url
        }
      }

      # CORS_ALLOWED_ORIGINS (set to frontend URL)
      dynamic "env" {
        for_each = var.cors_allowed_origins != "" ? [1] : []
        content {
          name  = "CORS_ALLOWED_ORIGINS"
          value = var.cors_allowed_origins
        }
      }

      # Environment variables from Secret Manager
      dynamic "env" {
        for_each = var.secret_environment_variables
        content {
          name = env.key
          value_source {
            secret_key_ref {
              secret  = env.value
              version = "latest"
            }
          }
        }
      }

      # Health check / startup probe
      startup_probe {
        http_get {
          path = var.health_check_path
          port = var.container_port
        }
        initial_delay_seconds = 60
        timeout_seconds       = 10
        period_seconds        = 30
        failure_threshold     = 20
      }

      # Liveness probe
      liveness_probe {
        http_get {
          path = var.health_check_path
          port = var.container_port
        }
        initial_delay_seconds = 30
        timeout_seconds       = 3
        period_seconds        = 30
        failure_threshold     = 3
      }
    }

    # Service account
    service_account = google_service_account.service_account.email

    # Timeout for requests
    timeout = "${var.request_timeout_seconds}s"

    # Cloud SQL connections (annotations are a map, not a block)
    annotations = length(var.cloud_sql_connections) > 0 ? {
      "run.googleapis.com/cloudsql-instances" = join(",", var.cloud_sql_connections)
    } : {}
  }

  traffic {
    type    = "TRAFFIC_TARGET_ALLOCATION_TYPE_LATEST"
    percent = 100
  }
}

# Service account for Cloud Run service
resource "google_service_account" "service_account" {
  project      = var.project_id
  account_id   = "${var.service_name}-sa-${var.environment}"
  display_name = "Service Account for ${var.service_name}"
}

# IAM policy to allow public access (or restrict based on var.allowed_ingress_cidrs)
# Commented out due to permission issues - grant manually with gcloud:
#   gcloud run services add-iam-policy-binding oscal-tools-prod \
#     --region=us-central1 \
#     --member="allUsers" \
#     --role="roles/run.invoker"
#
# resource "google_cloud_run_v2_service_iam_member" "public_access" {
#   count = length(var.allowed_ingress_cidrs) == 0 ? 1 : 0
#
#   project  = var.project_id
#   location = var.region
#   name     = google_cloud_run_v2_service.service.name
#   role     = "roles/run.invoker"
#   member   = "allUsers"
# }

# TODO: For private access, implement Cloud Armor or load balancer with IP restrictions
