# ============================================================================#
# Networking Module Variables
# ============================================================================#

variable "project_id" {
  description = "GCP project ID"
  type        = string
}

variable "region" {
  description = "GCP region"
  type        = string
}

variable "connector_ip_cidr_range" {
  description = "IP CIDR range for VPC connector subnet"
  type        = string
  default     = "10.8.0.0/28" # /28 gives 16 IPs (11 usable)
}

variable "connector_machine_type" {
  description = "Machine type for VPC connector"
  type        = string
  default     = "e2-micro"
}

variable "connector_min_instances" {
  description = "Minimum instances for VPC connector"
  type        = number
  default     = 2
}

variable "connector_max_instances" {
  description = "Maximum instances for VPC connector"
  type        = number
  default     = 3
}
