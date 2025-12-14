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
  default     = "db-f1-micro" # Change to db-custom-2-7680 for production
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

variable "app_min_instances" {
  description = "Minimum number of app instances"
  type        = number
  default     = 0 # Scale to zero for cost savings
}

variable "app_max_instances" {
  description = "Maximum number of app instances"
  type        = number
  default     = 10
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
