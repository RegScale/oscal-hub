# ============================================================================#
# Networking Module Outputs
# ============================================================================#

output "vpc_connector_id" {
  description = "ID of the VPC Access connector"
  value       = google_vpc_access_connector.connector.id
}

output "vpc_connector_name" {
  description = "Name of the VPC Access connector"
  value       = google_vpc_access_connector.connector.name
}

output "private_vpc_connection" {
  description = "Private VPC connection for service networking"
  value       = google_service_networking_connection.private_vpc_connection
}
