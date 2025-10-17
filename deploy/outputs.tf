# ==============================================================================
# Resource Group Outputs
# ==============================================================================

output "resource_group_name" {
  description = "Name of the resource group"
  value       = azurerm_resource_group.main.name
}

output "resource_group_location" {
  description = "Location of the resource group"
  value       = azurerm_resource_group.main.location
}

# ==============================================================================
# Container Registry Outputs
# ==============================================================================

output "container_registry_name" {
  description = "Name of the Azure Container Registry"
  value       = azurerm_container_registry.main.name
}

output "container_registry_login_server" {
  description = "Login server URL for the Container Registry"
  value       = azurerm_container_registry.main.login_server
}

output "container_registry_admin_username" {
  description = "Admin username for the Container Registry"
  value       = azurerm_container_registry.main.admin_username
  sensitive   = true
}

output "container_registry_admin_password" {
  description = "Admin password for the Container Registry"
  value       = azurerm_container_registry.main.admin_password
  sensitive   = true
}

# ==============================================================================
# Container Apps Environment Outputs
# ==============================================================================

output "container_app_environment_id" {
  description = "ID of the Container Apps Environment"
  value       = azurerm_container_app_environment.main.id
}

output "container_app_environment_name" {
  description = "Name of the Container Apps Environment"
  value       = azurerm_container_app_environment.main.name
}

# ==============================================================================
# Container App Outputs - Combined Mode
# ==============================================================================

output "combined_app_url" {
  description = "URL of the combined application (combined mode only)"
  value       = var.deployment_mode == "combined" ? "https://${azurerm_container_app.combined[0].ingress[0].fqdn}" : null
}

output "combined_app_fqdn" {
  description = "FQDN of the combined application (combined mode only)"
  value       = var.deployment_mode == "combined" ? azurerm_container_app.combined[0].ingress[0].fqdn : null
}

# ==============================================================================
# Container App Outputs - Separate Mode
# ==============================================================================

output "backend_app_url" {
  description = "URL of the backend API (separate mode only)"
  value       = var.deployment_mode == "separate" ? "https://${azurerm_container_app.backend[0].ingress[0].fqdn}" : null
}

output "backend_app_fqdn" {
  description = "FQDN of the backend API (separate mode only)"
  value       = var.deployment_mode == "separate" ? azurerm_container_app.backend[0].ingress[0].fqdn : null
}

output "frontend_app_url" {
  description = "URL of the frontend application (separate mode only)"
  value       = var.deployment_mode == "separate" ? "https://${azurerm_container_app.frontend[0].ingress[0].fqdn}" : null
}

output "frontend_app_fqdn" {
  description = "FQDN of the frontend application (separate mode only)"
  value       = var.deployment_mode == "separate" ? azurerm_container_app.frontend[0].ingress[0].fqdn : null
}

# ==============================================================================
# Application Insights Outputs
# ==============================================================================

output "application_insights_instrumentation_key" {
  description = "Application Insights instrumentation key"
  value       = var.enable_application_insights ? azurerm_application_insights.main[0].instrumentation_key : null
  sensitive   = true
}

output "application_insights_connection_string" {
  description = "Application Insights connection string"
  value       = var.enable_application_insights ? azurerm_application_insights.main[0].connection_string : null
  sensitive   = true
}

output "application_insights_app_id" {
  description = "Application Insights application ID"
  value       = var.enable_application_insights ? azurerm_application_insights.main[0].app_id : null
}

# ==============================================================================
# Log Analytics Outputs
# ==============================================================================

output "log_analytics_workspace_id" {
  description = "ID of the Log Analytics workspace"
  value       = azurerm_log_analytics_workspace.main.id
}

output "log_analytics_workspace_name" {
  description = "Name of the Log Analytics workspace"
  value       = azurerm_log_analytics_workspace.main.name
}

# ==============================================================================
# Storage Account Outputs
# ==============================================================================

output "storage_account_name" {
  description = "Name of the storage account (if enabled)"
  value       = var.enable_blob_storage ? azurerm_storage_account.main[0].name : null
}

output "storage_account_primary_connection_string" {
  description = "Primary connection string for the storage account (if enabled)"
  value       = var.enable_blob_storage ? azurerm_storage_account.main[0].primary_connection_string : null
  sensitive   = true
}

output "storage_container_name" {
  description = "Name of the blob storage container (if enabled)"
  value       = var.enable_blob_storage ? azurerm_storage_container.uploads[0].name : null
}

# ==============================================================================
# Deployment Information
# ==============================================================================

output "deployment_mode" {
  description = "Current deployment mode (combined or separate)"
  value       = var.deployment_mode
}

output "environment" {
  description = "Deployment environment"
  value       = var.environment
}

# ==============================================================================
# Quick Access URLs
# ==============================================================================

output "application_url" {
  description = "Main application URL to access"
  value = var.deployment_mode == "combined" ? (
    var.enable_ingress ? "https://${azurerm_container_app.combined[0].ingress[0].fqdn}" : "Ingress not enabled"
    ) : (
    var.enable_ingress ? "https://${azurerm_container_app.frontend[0].ingress[0].fqdn}" : "Ingress not enabled"
  )
}

output "api_url" {
  description = "API base URL"
  value = var.deployment_mode == "combined" ? (
    var.enable_ingress ? "https://${azurerm_container_app.combined[0].ingress[0].fqdn}/api" : "Ingress not enabled"
    ) : (
    var.enable_ingress ? "https://${azurerm_container_app.backend[0].ingress[0].fqdn}/api" : "Ingress not enabled"
  )
}

# ==============================================================================
# Docker Commands for Deployment
# ==============================================================================

output "docker_login_command" {
  description = "Command to login to Azure Container Registry"
  value       = "az acr login --name ${azurerm_container_registry.main.name}"
}

output "docker_push_commands" {
  description = "Commands to tag and push Docker images"
  value = var.deployment_mode == "combined" ? {
    tag  = "docker tag oscal-ux:latest ${azurerm_container_registry.main.login_server}/oscal-ux:${var.docker_image_tag}"
    push = "docker push ${azurerm_container_registry.main.login_server}/oscal-ux:${var.docker_image_tag}"
    } : {
    backend_tag      = "docker tag oscal-backend:latest ${azurerm_container_registry.main.login_server}/oscal-backend:${var.docker_image_tag}"
    backend_push     = "docker push ${azurerm_container_registry.main.login_server}/oscal-backend:${var.docker_image_tag}"
    frontend_tag     = "docker tag oscal-frontend:latest ${azurerm_container_registry.main.login_server}/oscal-frontend:${var.docker_image_tag}"
    frontend_push    = "docker push ${azurerm_container_registry.main.login_server}/oscal-frontend:${var.docker_image_tag}"
  }
}
