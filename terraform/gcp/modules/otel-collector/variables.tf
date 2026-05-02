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
  type    = number
  default = 1
}

variable "max_instances" {
  type    = number
  default = 10
}

variable "api_service_account" {
  type        = string
  description = "Service account email of the API service that will publish OTLP to the collector."
}
