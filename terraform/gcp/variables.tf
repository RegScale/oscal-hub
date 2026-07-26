# ============================================================================#
# OSCAL Tools - Terraform Variables
# ============================================================================#

# ----------------------------------------------------------------------------
# Project Configuration
# ----------------------------------------------------------------------------

variable "project_id" {
  description = "The GCP project ID"
  type        = string
}

variable "region" {
  description = "The GCP region for resources"
  type        = string
  default     = "us-central1"
}

variable "environment" {
  description = "Environment name (dev, staging, prod)"
  type        = string
  default     = "prod"
}

# ----------------------------------------------------------------------------
# Artifact Registry
# ----------------------------------------------------------------------------

variable "artifact_registry_repository" {
  description = "Artifact Registry repository name for container images"
  type        = string
  default     = "oscal-tools"
}

variable "image_tag" {
  description = "Container image tag (use timestamp for unique deployments)"
  type        = string
  default     = "latest"
}

# ----------------------------------------------------------------------------
# Database Configuration
# ----------------------------------------------------------------------------

variable "db_name" {
  description = "Cloud SQL database name"
  type        = string
  default     = "oscal_production"
}

variable "db_username" {
  description = "Cloud SQL database username"
  type        = string
  default     = "oscal_user"
}

variable "db_tier" {
  description = "Cloud SQL instance tier"
  type        = string
  # db-custom-1-3840 (1 vCPU, 3.75 GB RAM) is the smallest non-shared-core tier.
  # The previous default (db-f1-micro, ~614 MB RAM) ran out of memory under
  # normal Spring Boot + Hibernate validate load and OOM-restarted under the
  # V1.12 catch-up migration on 2026-05-10. f1-micro is documented by Google
  # as not for production use.
  default = "db-custom-1-3840"
}

variable "db_availability_type" {
  description = "Cloud SQL availability type — REGIONAL doubles cost (full standby replica)."
  type        = string
  default     = "ZONAL"
  validation {
    condition     = contains(["ZONAL", "REGIONAL"], var.db_availability_type)
    error_message = "db_availability_type must be ZONAL or REGIONAL."
  }
}

variable "db_shared_buffers_8kb_pages" {
  description = "Postgres shared_buffers in 8 KB pages. ~25% of tier RAM is the rule of thumb. Default 115000 = ~900 MB tuned for db-custom-1-3840 (3.75 GB RAM)."
  type        = string
  default     = "115000"
}

# ----------------------------------------------------------------------------
# Cloud Storage Configuration
# ----------------------------------------------------------------------------

variable "bucket_prefix" {
  description = "Prefix for Cloud Storage bucket names"
  type        = string
  default     = "oscal-tools"
}

# ----------------------------------------------------------------------------
# Application Cloud Run Configuration (Single Container: Backend + Frontend)
# ----------------------------------------------------------------------------

variable "app_cpu" {
  description = "CPU allocation for OSCAL Tools Cloud Run service"
  type        = string
  default     = "2000m" # 2 vCPU (needs to run both backend and frontend)
}

variable "app_memory" {
  description = "Memory allocation for OSCAL Tools Cloud Run service"
  type        = string
  default     = "4Gi" # 4GB (needs to run both Spring Boot and Node.js)
}

variable "hubspot_service_key" {
  type        = string
  default     = ""
  sensitive   = true
  description = "HubSpot Service Key (Settings > Integrations > Service Keys) for marketing CRM sync of new users/orgs; empty disables the sync (no-op). Supply via TF_VAR_hubspot_service_key."
}

variable "app_min_instances" {
  description = "Minimum warm instances. 1 prevents cold-start login timeouts during idle hours; the main app's Spring Boot startup is ~30–60s."
  type        = number
  default     = 1
}

variable "app_max_instances" {
  description = "Max instances Cloud Run can scale to. Default autoscaling target is 60% CPU per instance. Capped at 3 for cost predictability — raise after observing real traffic patterns."
  type        = number
  default     = 3
}

variable "app_concurrency" {
  description = "Max concurrent requests per app instance before Cloud Run spawns another. 80 is the v2 default; raise on memory-bound apps with low per-request CPU."
  type        = number
  default     = 80
}

# ----------------------------------------------------------------------------
# Networking Configuration
# ----------------------------------------------------------------------------

variable "vpc_connector_machine_type" {
  description = "Machine type for VPC Access connector"
  type        = string
  default     = "e2-micro" # Cheapest option, upgrade for production
}

variable "vpc_connector_min_instances" {
  description = "Minimum instances for VPC Access connector"
  type        = number
  default     = 2
}

variable "vpc_connector_max_instances" {
  description = "Maximum instances for VPC Access connector"
  type        = number
  default     = 3
}

# ----------------------------------------------------------------------------
# Custom Domain (Optional)
# ----------------------------------------------------------------------------

variable "custom_domain" {
  description = "Custom domain for the application (optional)"
  type        = string
  default     = ""
}

# CDN not needed for single-container architecture
# (frontend is served directly from Cloud Run)

# ----------------------------------------------------------------------------
# Security Configuration
# ----------------------------------------------------------------------------

variable "allowed_ingress_cidrs" {
  description = "CIDR ranges allowed to access Cloud Run services (empty = public)"
  type        = list(string)
  default     = [] # Public access
}

# ----------------------------------------------------------------------------
# Backup Configuration
# ----------------------------------------------------------------------------

variable "enable_db_backups" {
  description = "Enable automated backups for Cloud SQL"
  type        = bool
  default     = true
}

variable "db_backup_start_time" {
  description = "Start time for automated backups (HH:MM format)"
  type        = string
  default     = "03:00"
}

variable "db_backup_retention_days" {
  description = "Number of days to retain backups"
  type        = number
  default     = 7
}

# ----------------------------------------------------------------------------
# Monitoring Configuration
# ----------------------------------------------------------------------------

variable "enable_monitoring_alerts" {
  description = "Enable Cloud Monitoring alerts"
  type        = bool
  default     = true
}

variable "alert_notification_channels" {
  description = "Notification channel IDs for alerts"
  type        = list(string)
  default     = []
}

# ----------------------------------------------------------------------------
# OpenTelemetry Configuration
# ----------------------------------------------------------------------------

variable "otel_enabled" {
  type        = bool
  default     = false
  description = "When true, the API service exports telemetry via JAVA_TOOL_OPTIONS attaching the OTel agent."
}

variable "otel_collector_image" {
  type        = string
  default     = ""
  description = "Fully-qualified image of the otel-collector image (set by CI; empty disables module)."
}

variable "otel_collector_cpu" {
  description = "CPU allocation for the otel-collector Cloud Run service."
  type        = string
  default     = "1000m"
}

variable "otel_collector_memory" {
  description = "Memory allocation for the otel-collector Cloud Run service. 512Mi is enough for the current routing connector + GCP exporters; bump if span volume grows."
  type        = string
  default     = "512Mi"
}

variable "otel_collector_min_instances" {
  description = "Minimum warm instances of the otel-collector. Must be ≥1 — the OTel Java agent on the main app exports OTLP/gRPC to this service and a cold start drops spans."
  type        = number
  default     = 1
}

variable "otel_collector_max_instances" {
  description = "Max instances the otel-collector can scale to. Capped at 3 for cost predictability — bump if you see span drop counters in Cloud Monitoring."
  type        = number
  default     = 3
}

# ----------------------------------------------------------------------------
# Dimsync (background worker — Cloud Run Job, hourly)
#
# This is the closest thing to a "Celery worker" in this codebase: a
# scheduled batch job that reads recent telemetry from Cloud SQL and
# fans out dimension data into BigQuery analytics tables. Capacity-bounded
# rather than auto-scaled — bump CPU/memory if it starts hitting timeout.
# ----------------------------------------------------------------------------

variable "dimsync_cpu" {
  description = "CPU for the dimsync Cloud Run Job. Defaults to match the main app — sharing the same JVM image, the job's cold-start JIT and Hibernate metamodel load are roughly the same cost as the API service's, even though the per-tick workload is smaller."
  type        = string
  default     = "2000m"
}

variable "dimsync_memory" {
  description = "Memory for the dimsync Cloud Run Job. Defaults to match the main app for the same reason — Spring Boot + Hibernate + OTel agent need the same headroom regardless of the workload."
  type        = string
  default     = "4Gi"
}

variable "dimsync_timeout_seconds" {
  description = "Per-execution timeout in seconds. The job is hourly, so a long timeout is safe — better to let a slow run finish than retry."
  type        = number
  default     = 600
}

variable "dimsync_max_retries" {
  description = "Cloud Run Job retry count on failure. Low because the hourly trigger gives us a natural retry interval."
  type        = number
  default     = 1
}
