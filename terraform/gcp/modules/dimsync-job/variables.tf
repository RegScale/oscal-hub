variable "project_id" { type = string }
variable "region"      { type = string }
variable "environment" { type = string }
variable "image" {
  type        = string
  description = "Same Cloud Run image as the API service."
}
variable "db_url"  { type = string }
variable "db_user" {
  type    = string
  default = ""
}
variable "db_password_secret" {
  type        = string
  default     = ""
  description = "Secret Manager secret name (not full path) holding the DB password. Empty disables."
}
variable "bigquery_dataset_id" { type = string }
variable "cloud_sql_connection" {
  type        = string
  description = "Cloud SQL instance connection name (project:region:instance) for VPC-less attach."
}
