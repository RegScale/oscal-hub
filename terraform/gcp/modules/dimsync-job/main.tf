resource "google_service_account" "dimsync" {
  account_id   = "dimsync-${var.environment}"
  display_name = "OSCAL Hub Dimension Sync (${var.environment})"
  project      = var.project_id
}

# BigQuery write access on the analytics dataset
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

# Cloud SQL client for the dimsync job to reach Postgres (same instance as API)
resource "google_project_iam_member" "dimsync_sql_client" {
  project = var.project_id
  role    = "roles/cloudsql.client"
  member  = "serviceAccount:${google_service_account.dimsync.email}"
}

# Optional: secret access if a DB password secret is configured
resource "google_secret_manager_secret_iam_member" "dimsync_secret_access" {
  count     = var.db_password_secret != "" ? 1 : 0
  project   = var.project_id
  secret_id = var.db_password_secret
  role      = "roles/secretmanager.secretAccessor"
  member    = "serviceAccount:${google_service_account.dimsync.email}"
}

resource "google_cloud_run_v2_job" "dimsync" {
  name     = "dimsync-${var.environment}"
  location = var.region
  project  = var.project_id

  template {
    template {
      service_account = google_service_account.dimsync.email
      max_retries     = 1
      timeout         = "600s"

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
        env {
          name  = "GCP_PROJECT_ID"
          value = var.project_id
        }
        dynamic "env" {
          for_each = var.db_user != "" ? [1] : []
          content {
            name  = "DB_USER"
            value = var.db_user
          }
        }
        dynamic "env" {
          for_each = var.db_password_secret != "" ? [1] : []
          content {
            name = "DB_PASSWORD"
            value_source {
              secret_key_ref {
                secret  = var.db_password_secret
                version = "latest"
              }
            }
          }
        }

        resources {
          limits = {
            cpu    = "1"
            memory = "512Mi"
          }
        }

        # Allow the JVM and Cloud SQL socket factory to find the instance
        # via socketFactory in DB_URL — no VPC connector needed.
      }
    }
  }
}

# Hourly trigger
resource "google_cloud_scheduler_job" "dimsync_hourly" {
  name      = "dimsync-${var.environment}-hourly"
  schedule  = "0 * * * *"
  time_zone = "UTC"
  project   = var.project_id
  region    = var.region

  http_target {
    uri         = "https://${var.region}-run.googleapis.com/apis/run.googleapis.com/v1/namespaces/${var.project_id}/jobs/${google_cloud_run_v2_job.dimsync.name}:run"
    http_method = "POST"

    oauth_token {
      service_account_email = google_service_account.dimsync.email
    }
  }
}

# Scheduler needs run.invoker on the job
resource "google_cloud_run_v2_job_iam_member" "scheduler_invoker" {
  project  = var.project_id
  location = var.region
  name     = google_cloud_run_v2_job.dimsync.name
  role     = "roles/run.invoker"
  member   = "serviceAccount:${google_service_account.dimsync.email}"
}
