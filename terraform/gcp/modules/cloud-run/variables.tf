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
  description = "Minimum number of instances"
  type        = number
  default     = 0
}

variable "max_instances" {
  description = "Maximum number of instances"
  type        = number
  default     = 10
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
