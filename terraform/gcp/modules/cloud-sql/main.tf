# ============================================================================#
# Cloud SQL PostgreSQL Module
# ============================================================================#

resource "google_sql_database_instance" "postgres" {
  name             = "oscal-db-${var.environment}-${var.db_name_suffix}"
  database_version = "POSTGRES_15"
  region           = var.region
  project          = var.project_id

  # Prevent accidental deletion in production
  deletion_protection = var.environment == "prod" ? true : false

  settings {
    tier              = var.db_tier
    availability_type = var.availability_type
    disk_type         = "PD_SSD"
    disk_size         = var.disk_size_gb
    disk_autoresize   = true

    # Backup configuration
    backup_configuration {
      enabled                        = var.enable_backups
      start_time                     = var.backup_start_time
      point_in_time_recovery_enabled = var.enable_backups
      transaction_log_retention_days = 7
      backup_retention_settings {
        retained_backups = var.backup_retention_days
        retention_unit   = "COUNT"
      }
    }

    # IP configuration (public IP with Cloud Run authorized)
    ip_configuration {
      ipv4_enabled = true
      ssl_mode     = "ENCRYPTED_ONLY"

      # Allow Cloud Run to connect
      authorized_networks {
        name  = "allow-all-cloud-run"
        value = "0.0.0.0/0"
      }
    }

    # Maintenance window
    maintenance_window {
      day          = 7 # Sunday
      hour         = 3 # 3 AM
      update_track = "stable"
    }

    # Database flags for performance
    database_flags {
      name  = "max_connections"
      value = var.max_connections
    }

    database_flags {
      name  = "shared_buffers"
      # 8 KB pages. Default 115000 ≈ 900 MB, tuned for db-custom-1-3840.
      # Bump proportionally if you upgrade the tier; never set above ~25% of
      # instance RAM or the OS/connection backends start hitting OOM.
      value = var.shared_buffers_8kb_pages
    }

    # Insights configuration for monitoring
    insights_config {
      query_insights_enabled  = true
      query_plans_per_minute  = 5
      query_string_length     = 1024
      record_application_tags = true
    }
  }

  # Deletion policy
  lifecycle {
    prevent_destroy = false # Set to true for production
    ignore_changes  = [
      settings[0].disk_size # Ignore disk size changes (autoresize handles this)
    ]
  }
}

# Create database
resource "google_sql_database" "database" {
  name     = var.db_name
  instance = google_sql_database_instance.postgres.name
  project  = var.project_id
}

# Create database user
resource "google_sql_user" "user" {
  name     = var.db_username
  instance = google_sql_database_instance.postgres.name
  password = var.db_password
  project  = var.project_id
}
