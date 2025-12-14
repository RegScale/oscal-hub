# ============================================================================#
# Cloud Run Module Outputs
# ============================================================================#

output "service_name" {
  description = "Name of the Cloud Run service"
  value       = google_cloud_run_v2_service.service.name
}

output "service_url" {
  description = "URL of the Cloud Run service"
  value       = google_cloud_run_v2_service.service.uri
}

output "service_account_email" {
  description = "Email of the service account"
  value       = google_service_account.service_account.email
}

output "verified_domain_name" {
  description = "Domain name that was mapped"
  value       = var.custom_domain != "" ? google_cloud_run_domain_mapping.custom_domain[0].name : null
}

output "dns_records" {
  description = "DNS records to be added to the DNS provider"
  value       = var.custom_domain != "" ? google_cloud_run_domain_mapping.custom_domain[0].status[0].resource_records : null
}

output "www_dns_records" {
  description = "DNS records for the www subdomain"
  value       = var.custom_domain != "" && !startswith(var.custom_domain, "www.") ? google_cloud_run_domain_mapping.www_custom_domain[0].status[0].resource_records : null
}
