# ============================================================================#
# Cloud Run Module Variables
# ============================================================================#

variable "project_id" {
  description = "GCP project ID"
  type        = string
}

variable "region" {
  description = "GCP region"
  type        = string
}

variable "environment" {
  description = "Environment name"
  type        = string
}

variable "service_name" {
  description = "Name of the Cloud Run service"
  type        = string
}

variable "image" {
  description = "Container image URL"
  type        = string
}

variable "cpu_limit" {
  description = "CPU limit"
  type        = string
  default     = "1000m"
}

variable "memory_limit" {
  description = "Memory limit"
  type        = string
  default     = "512Mi"
}

variable "min_instances" {
  description = "Minimum warm instances. Set ≥1 to avoid cold-start latency."
  type        = number
  default     = 1
}

variable "max_instances" {
  description = "Maximum instances Cloud Run can scale to."
  type        = number
  default     = 3
}

variable "concurrency" {
  description = "Max concurrent requests per instance before autoscaling. 80 is the v2 default."
  type        = number
  default     = 80
}

variable "environment_variables" {
  description = "Environment variables as key-value pairs"
  type        = map(string)
  default     = {}
}

variable "secret_environment_variables" {
  description = "Environment variables from Secret Manager (secret IDs)"
  type        = map(string)
  default     = {}
}

variable "db_url" {
  description = "Database connection URL"
  type        = string
  default     = ""
}

# Emitted in its own env block after DB_URL so TF order matches Cloud Run's
# insertion order (DB_DDL_AUTO was originally added via gcloud --update-env-vars
# after the service existed, and Cloud Run preserves insertion order on update).
variable "db_ddl_auto" {
  description = "Hibernate hbm2ddl.auto value (e.g. update, validate, none); empty disables the env var"
  type        = string
  default     = ""
}

variable "cors_allowed_origins" {
  description = "CORS allowed origins"
  type        = string
  default     = ""
}

variable "cloud_sql_connections" {
  description = "List of Cloud SQL instance connection names"
  type        = list(string)
  default     = []
}

variable "vpc_connector_id" {
  description = "VPC Access Connector ID"
  type        = string
  default     = ""
}

variable "container_port" {
  description = "Container port"
  type        = number
  default     = 8080
}

variable "health_check_path" {
  description = "Health check path"
  type        = string
  default     = "/actuator/health"
}

variable "request_timeout_seconds" {
  description = "Request timeout in seconds"
  type        = number
  default     = 300
}


variable "allowed_ingress_cidrs" {
  description = "Allowed ingress CIDR ranges (empty = public)"
  type        = list(string)
  default     = []
}

variable "custom_domain" {
  description = "Custom domain to map to the Cloud Run service (e.g. example.com)"
  type        = string
  default     = ""
}
