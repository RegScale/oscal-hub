# ==============================================================================
# General Configuration
# ==============================================================================

variable "project_name" {
  description = "Project name used for resource naming"
  type        = string
  default     = "oscal-cli"
}

variable "environment" {
  description = "Environment name (dev, staging, prod)"
  type        = string
  default     = "dev"

  validation {
    condition     = contains(["dev", "staging", "prod"], var.environment)
    error_message = "Environment must be dev, staging, or prod."
  }
}

variable "location" {
  description = "Azure region for resources"
  type        = string
  default     = "eastus"
}

variable "tags" {
  description = "Tags to apply to all resources"
  type        = map(string)
  default = {
    ManagedBy = "Terraform"
    Project   = "OSCAL-CLI"
  }
}

# ==============================================================================
# Deployment Architecture
# ==============================================================================

variable "deployment_mode" {
  description = "Deployment architecture: 'combined' (single container) or 'separate' (frontend + backend containers)"
  type        = string
  default     = "combined"

  validation {
    condition     = contains(["combined", "separate"], var.deployment_mode)
    error_message = "Deployment mode must be 'combined' or 'separate'."
  }
}

# ==============================================================================
# Container Registry Configuration
# ==============================================================================

variable "acr_sku" {
  description = "Azure Container Registry SKU (Basic, Standard, Premium)"
  type        = string
  default     = "Basic"

  validation {
    condition     = contains(["Basic", "Standard", "Premium"], var.acr_sku)
    error_message = "ACR SKU must be Basic, Standard, or Premium."
  }
}

variable "docker_image_tag" {
  description = "Docker image tag to deploy"
  type        = string
  default     = "latest"
}

# ==============================================================================
# Container Apps Configuration - Combined Mode
# ==============================================================================

variable "combined_container_cpu" {
  description = "CPU allocation for combined container (0.25, 0.5, 0.75, 1, 1.25, 1.5, 1.75, 2)"
  type        = number
  default     = 1
}

variable "combined_container_memory" {
  description = "Memory allocation for combined container in Gi (0.5Gi to 4Gi)"
  type        = string
  default     = "2Gi"
}

variable "combined_min_replicas" {
  description = "Minimum number of replicas for combined container"
  type        = number
  default     = 0
}

variable "combined_max_replicas" {
  description = "Maximum number of replicas for combined container"
  type        = number
  default     = 10
}

# ==============================================================================
# Container Apps Configuration - Separate Mode (Backend)
# ==============================================================================

variable "backend_container_cpu" {
  description = "CPU allocation for backend container"
  type        = number
  default     = 0.5
}

variable "backend_container_memory" {
  description = "Memory allocation for backend container"
  type        = string
  default     = "1Gi"
}

variable "backend_min_replicas" {
  description = "Minimum number of backend replicas"
  type        = number
  default     = 1
}

variable "backend_max_replicas" {
  description = "Maximum number of backend replicas"
  type        = number
  default     = 20
}

variable "backend_port" {
  description = "Backend API port"
  type        = number
  default     = 8080
}

# ==============================================================================
# Container Apps Configuration - Separate Mode (Frontend)
# ==============================================================================

variable "frontend_container_cpu" {
  description = "CPU allocation for frontend container"
  type        = number
  default     = 0.5
}

variable "frontend_container_memory" {
  description = "Memory allocation for frontend container"
  type        = string
  default     = "1Gi"
}

variable "frontend_min_replicas" {
  description = "Minimum number of frontend replicas"
  type        = number
  default     = 1
}

variable "frontend_max_replicas" {
  description = "Maximum number of frontend replicas"
  type        = number
  default     = 10
}

variable "frontend_port" {
  description = "Frontend port"
  type        = number
  default     = 3000
}

# ==============================================================================
# Auto-scaling Configuration
# ==============================================================================

variable "enable_http_scaling" {
  description = "Enable HTTP-based auto-scaling"
  type        = bool
  default     = true
}

variable "http_concurrent_requests" {
  description = "Concurrent HTTP requests threshold for scaling"
  type        = number
  default     = 10
}

variable "enable_cpu_scaling" {
  description = "Enable CPU-based auto-scaling"
  type        = bool
  default     = true
}

variable "cpu_threshold" {
  description = "CPU utilization threshold for scaling (percentage)"
  type        = number
  default     = 70
}

variable "enable_memory_scaling" {
  description = "Enable memory-based auto-scaling"
  type        = bool
  default     = false
}

variable "memory_threshold" {
  description = "Memory utilization threshold for scaling (percentage)"
  type        = number
  default     = 80
}

# ==============================================================================
# Application Configuration
# ==============================================================================

variable "java_opts" {
  description = "Java options for backend application"
  type        = string
  default     = "-Xmx1g -Xms512m"
}

variable "node_env" {
  description = "Node environment (production, development)"
  type        = string
  default     = "production"
}

variable "enable_telemetry" {
  description = "Enable Next.js telemetry"
  type        = bool
  default     = false
}

# ==============================================================================
# Monitoring and Observability
# ==============================================================================

variable "enable_application_insights" {
  description = "Enable Azure Application Insights"
  type        = bool
  default     = true
}

variable "log_retention_days" {
  description = "Log Analytics workspace retention in days"
  type        = number
  default     = 30
}

# ==============================================================================
# Networking and Security
# ==============================================================================

variable "enable_ingress" {
  description = "Enable external ingress"
  type        = bool
  default     = true
}

variable "allow_insecure_connections" {
  description = "Allow insecure HTTP connections"
  type        = bool
  default     = false
}

variable "custom_domain" {
  description = "Custom domain name (optional)"
  type        = string
  default     = null
}

variable "enable_cors" {
  description = "Enable CORS for backend API"
  type        = bool
  default     = true
}

variable "cors_allowed_origins" {
  description = "Allowed CORS origins"
  type        = list(string)
  default     = ["*"]
}

# ==============================================================================
# Storage Configuration (Optional)
# ==============================================================================

variable "enable_blob_storage" {
  description = "Enable Azure Blob Storage for file uploads"
  type        = bool
  default     = false
}

variable "storage_account_replication" {
  description = "Storage account replication type (LRS, GRS, RAGRS, ZRS)"
  type        = string
  default     = "LRS"
}

# ==============================================================================
# Cost Management
# ==============================================================================

variable "enable_consumption_workload_profile" {
  description = "Use consumption-based workload profile (more cost-effective)"
  type        = bool
  default     = true
}
