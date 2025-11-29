# ============================================================================#
# Secrets Module Variables
# ============================================================================#

variable "project_id" {
  description = "GCP project ID"
  type        = string
}

variable "db_password" {
  description = "Database password"
  type        = string
  sensitive   = true
}

variable "jwt_secret" {
  description = "JWT signing secret"
  type        = string
  sensitive   = true
}
