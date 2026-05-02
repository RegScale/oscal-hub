variable "project_id" {
  type        = string
  description = "GCP project ID."
}

variable "environment" {
  type        = string
  description = "Environment suffix (dev / staging / prod)."
}

variable "bigquery_table" {
  type        = string
  description = "Fully-qualified BigQuery table for the BQ subscription, e.g. PROJECT:DATASET.TABLE."
}

variable "publisher_sa" {
  type        = string
  description = "Service account email allowed to publish to the topic."
}
