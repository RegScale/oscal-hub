# ============================================================================#
# OSCAL Tools - Terraform Outputs
# ============================================================================#

# ----------------------------------------------------------------------------
# Application Outputs
# ----------------------------------------------------------------------------

output "app_url" {
  description = "URL for the OSCAL Tools application (frontend + backend)"
  value       = module.oscal_app.service_url
}

output "app_service_name" {
  description = "Name of the Cloud Run service"
  value       = module.oscal_app.service_name
}

# Legacy outputs for compatibility
output "frontend_url" {
  description = "URL for accessing the frontend (same as app_url)"
  value       = module.oscal_app.service_url
}

output "backend_url" {
  description = "URL for accessing the API (same as app_url with /api path)"
  value       = "${module.oscal_app.service_url}/api"
}

# ----------------------------------------------------------------------------
# Database Outputs
# ----------------------------------------------------------------------------

output "database_instance_name" {
  description = "Cloud SQL instance name"
  value       = module.database.instance_name
}

output "database_connection_name" {
  description = "Cloud SQL instance connection name"
  value       = module.database.instance_connection_name
}

output "database_name" {
  description = "Database name"
  value       = var.db_name
}

output "database_username" {
  description = "Database username"
  value       = var.db_username
}

output "database_password" {
  description = "Database password (randomly generated)"
  value       = random_password.db_password.result
  sensitive   = true
}

output "database_private_ip" {
  description = "Cloud SQL private IP address"
  value       = module.database.private_ip_address
  sensitive   = true
}

# ----------------------------------------------------------------------------
# Storage Outputs
# ----------------------------------------------------------------------------

output "build_bucket_name" {
  description = "Name of the build storage bucket"
  value       = module.storage.build_bucket_name
}

output "build_bucket_url" {
  description = "URL of the build storage bucket"
  value       = module.storage.build_bucket_url
}

# ----------------------------------------------------------------------------
# Secrets Outputs
# ----------------------------------------------------------------------------
# Disabled - not using Secret Manager

# output "jwt_secret_name" {
#   description = "Name of the JWT secret in Secret Manager"
#   value       = module.secrets.jwt_secret_id
# }
#
# output "db_password_secret_name" {
#   description = "Name of the database password secret in Secret Manager"
#   value       = module.secrets.db_password_id
# }

# ----------------------------------------------------------------------------
# Quick Setup Instructions
# ----------------------------------------------------------------------------

output "next_steps" {
  description = "Next steps after infrastructure deployment"
  value       = <<-EOT

    ============================================================================
    OSCAL Tools Infrastructure Deployed Successfully!
    ============================================================================

    Application URL:  ${module.oscal_app.service_url}
    Database:         ${module.database.instance_name}

    NEXT STEPS:

    1. Build and push container image:
       cd ../..
       gcloud builds submit --config=cloudbuild-images.yaml

    2. Access your application:
       Web Interface: ${module.oscal_app.service_url}
       API Docs:      ${module.oscal_app.service_url}/swagger-ui
       Health Check:  ${module.oscal_app.service_url}/actuator/health

    3. Get database connection details:
       Database Name:     ${var.db_name}
       Database Username: ${var.db_username}
       Database Password: terraform output -raw database_password

       Or retrieve from Secret Manager:
       gcloud secrets versions access latest --secret=db-password

       To connect locally via Cloud SQL Proxy:
       cloud-sql-proxy ${module.database.instance_connection_name}
       psql "host=127.0.0.1 user=${var.db_username} dbname=${var.db_name}"

    4. Monitor your application:
       Cloud Run:  https://console.cloud.google.com/run
       Cloud SQL:  https://console.cloud.google.com/sql
       Logs:       https://console.cloud.google.com/logs

    5. Set up custom domain (optional):
       gcloud run domain-mappings create --service=oscal-tools-${var.environment} --domain=your-domain.com --region=${var.region}

    ============================================================================
    Architecture: Single container with Spring Boot backend + Next.js frontend
    ============================================================================

  EOT
}
