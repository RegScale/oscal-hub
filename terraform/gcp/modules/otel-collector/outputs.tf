output "collector_url" {
  value       = google_cloud_run_v2_service.collector.uri
  description = "Base URL of the collector service."
}

output "collector_service_account" {
  value       = google_service_account.collector.email
  description = "Service account email used by the collector."
}
