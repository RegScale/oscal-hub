variable "project_id" {
  type = string
}

variable "region" {
  type = string
}

variable "environment" {
  type = string
}

variable "image" {
  type        = string
  description = "Fully-qualified image of the collector (e.g. us-central1-docker.pkg.dev/PROJECT/REPO/otel-collector:SHA)."
}

variable "min_instances" {
  type        = number
  default     = 1
  description = "Minimum warm instances. Must be ≥1 — the OTel agent on the API service exports OTLP/gRPC here and a cold start drops spans."
}

variable "max_instances" {
  type    = number
  default = 3
}

variable "cpu" {
  type        = string
  default     = "1000m"
  description = "CPU allocation."
}

variable "memory" {
  type        = string
  default     = "512Mi"
  description = "Memory allocation."
}

variable "api_service_account" {
  type        = string
  description = "Service account email of the API service that will publish OTLP to the collector."
}

variable "events_topic_name" {
  type        = string
  description = "Pub/Sub topic name for OTel events. When empty, the publisher IAM binding is skipped."
  default     = ""
}
