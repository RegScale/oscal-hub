variable "project_id" { type = string }
variable "region"      { type = string }
variable "environment" { type = string }
variable "image" {
  type        = string
  description = "Same Cloud Run image as the API service."
}
variable "db_url"  { type = string }
variable "db_username" {
  type    = string
  default = ""
}
variable "db_password" {
  type        = string
  default     = ""
  sensitive   = true
  description = "DB password passed as a direct env var (matches Phase 1 API service pattern; Secret Manager not used)."
}
variable "bigquery_dataset_id" { type = string }
variable "cloud_sql_connection" {
  type        = string
  description = "Cloud SQL instance connection name (project:region:instance) for VPC-less attach."
}
