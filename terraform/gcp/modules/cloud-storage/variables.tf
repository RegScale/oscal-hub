# ============================================================================#
# Cloud Storage Module Variables
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

variable "bucket_prefix" {
  description = "Prefix for bucket names"
  type        = string
}

variable "kms_key_name" {
  description = "KMS key name for encryption (optional)"
  type        = string
  default     = ""
}
