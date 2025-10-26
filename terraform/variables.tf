# Terraform Variables Definition
# Define all input variables for the infrastructure

variable "project_name" {
  description = "Name of the project, used for resource naming"
  type        = string
  default     = "oscal-tools"
}

variable "environment" {
  description = "Environment name (dev, staging, prod)"
  type        = string
  default     = "prod"
}

variable "location" {
  description = "Azure region for resources"
  type        = string
  default     = "eastus"
}

variable "resource_group_name" {
  description = "Name of the resource group"
  type        = string
  default     = "oscal-tools-prod"
}

# Database Configuration
variable "db_username" {
  description = "PostgreSQL administrator username"
  type        = string
  default     = "oscaladmin"
  sensitive   = true
}

variable "db_password" {
  description = "PostgreSQL administrator password"
  type        = string
  sensitive   = true
}

variable "db_name" {
  description = "PostgreSQL database name"
  type        = string
  default     = "oscal_production"
}

variable "db_sku" {
  description = "PostgreSQL SKU (B_Standard_B1ms, GP_Standard_D2s_v3, etc.)"
  type        = string
  default     = "B_Standard_B1ms" # Burstable, 1 vCore, 2GB RAM
}

variable "db_storage_mb" {
  description = "PostgreSQL storage in MB"
  type        = number
  default     = 32768 # 32 GB
}

# Application Configuration
variable "jwt_secret" {
  description = "JWT secret key for authentication"
  type        = string
  sensitive   = true
}

variable "cors_allowed_origins" {
  description = "Comma-separated list of allowed CORS origins"
  type        = string
  default     = "https://example.com"
}

# Container Configuration
variable "container_cpu" {
  description = "Number of CPU cores for container"
  type        = number
  default     = 2
}

variable "container_memory" {
  description = "Memory in GB for container"
  type        = number
  default     = 4
}

# Tags
variable "tags" {
  description = "Tags to apply to all resources"
  type        = map(string)
  default = {
    Project     = "OSCAL Tools"
    Environment = "Production"
    ManagedBy   = "Terraform"
  }
}

# Network Configuration
variable "allowed_ip_addresses" {
  description = "List of IP addresses allowed to access PostgreSQL (GitHub Actions runner IPs)"
  type        = list(string)
  default     = []
}

# ACR Configuration
variable "acr_sku" {
  description = "Azure Container Registry SKU (Basic, Standard, Premium)"
  type        = string
  default     = "Basic"
}

# Enable/Disable Features
variable "enable_monitoring" {
  description = "Enable Application Insights monitoring"
  type        = bool
  default     = true
}

variable "enable_public_access" {
  description = "Enable public access to container (vs VNet only)"
  type        = bool
  default     = true
}
