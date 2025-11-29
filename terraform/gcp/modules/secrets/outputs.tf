# ============================================================================#
# Secrets Module Outputs
# ============================================================================#

output "jwt_secret_id" {
  description = "JWT secret ID"
  value       = google_secret_manager_secret.jwt_secret.secret_id
}

output "jwt_secret_secret_id" {
  description = "JWT secret full resource ID"
  value       = google_secret_manager_secret.jwt_secret.id
}

output "db_password_id" {
  description = "Database password secret ID"
  value       = google_secret_manager_secret.db_password.secret_id
}

output "db_password_secret_id" {
  description = "Database password full resource ID"
  value       = google_secret_manager_secret.db_password.id
}
