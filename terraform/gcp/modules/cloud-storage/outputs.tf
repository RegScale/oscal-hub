# ============================================================================#
# Cloud Storage Module Outputs
# ============================================================================#

output "build_bucket_name" {
  description = "Name of the build bucket"
  value       = google_storage_bucket.build_bucket.name
}

output "build_bucket_url" {
  description = "URL of the build bucket"
  value       = google_storage_bucket.build_bucket.url
}

output "library_bucket_name" {
  description = "Name of the library bucket"
  value       = google_storage_bucket.library_bucket.name
}

output "library_bucket_url" {
  description = "URL of the library bucket"
  value       = google_storage_bucket.library_bucket.url
}
