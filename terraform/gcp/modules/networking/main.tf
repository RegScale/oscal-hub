# ============================================================================#
# Networking Module - VPC Connector for Cloud Run to Cloud SQL
# ============================================================================#

# Allocate IP address range for Google service networking (Cloud SQL)
resource "google_compute_global_address" "private_ip_address" {
  name          = "oscal-private-ip-address"
  purpose       = "VPC_PEERING"
  address_type  = "INTERNAL"
  prefix_length = 16
  network       = "default"
  project       = var.project_id
}

# Create private VPC connection to Google services
resource "google_service_networking_connection" "private_vpc_connection" {
  network                 = "default"
  service                 = "servicenetworking.googleapis.com"
  reserved_peering_ranges = [google_compute_global_address.private_ip_address.name]
}

# VPC Access Connector for Cloud Run to connect to Cloud SQL via private IP
resource "google_vpc_access_connector" "connector" {
  name    = "oscal-vpc-connector"
  project = var.project_id
  region  = var.region

  subnet {
    name       = google_compute_subnetwork.vpc_connector_subnet.name
    project_id = var.project_id
  }

  machine_type  = var.connector_machine_type
  min_instances = var.connector_min_instances
  max_instances = var.connector_max_instances
}

# Dedicated subnet for VPC Connector
resource "google_compute_subnetwork" "vpc_connector_subnet" {
  name          = "oscal-vpc-connector-subnet"
  project       = var.project_id
  region        = var.region
  network       = "default" # Using default VPC
  ip_cidr_range = var.connector_ip_cidr_range

  # Secondary IP ranges for GKE (if needed in future)
  # secondary_ip_range {
  #   range_name    = "services-range"
  #   ip_cidr_range = "192.168.1.0/24"
  # }
}

# Firewall rule to allow Cloud Run to connect through VPC Connector
resource "google_compute_firewall" "allow_vpc_connector" {
  name    = "oscal-allow-vpc-connector"
  project = var.project_id
  network = "default"

  allow {
    protocol = "tcp"
    ports    = ["5432"] # PostgreSQL
  }

  allow {
    protocol = "icmp"
  }

  source_ranges = [var.connector_ip_cidr_range]

  target_tags = ["cloud-sql"]
}
