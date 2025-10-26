# Terraform Outputs
# Display important values after deployment

output "resource_group_name" {
  description = "Name of the resource group"
  value       = azurerm_resource_group.main.name
}

output "location" {
  description = "Azure region"
  value       = azurerm_resource_group.main.location
}

output "acr_name" {
  description = "Azure Container Registry name"
  value       = azurerm_container_registry.acr.name
}

output "postgresql_server_name" {
  description = "PostgreSQL server name"
  value       = azurerm_postgresql_flexible_server.postgres.name
}

output "keyvault_name_output" {
  description = "Key Vault name"
  value       = azurerm_key_vault.main.name
}

output "container_name" {
  description = "Container instance name"
  value       = azurerm_container_group.main.name
}

# Summary output for easy copy-paste
output "deployment_summary" {
  description = "Deployment summary with all important URLs and names"
  value = <<-EOT

  ========================================
  OSCAL Tools - Deployment Summary
  ========================================

  Resource Group: ${azurerm_resource_group.main.name}
  Location: ${azurerm_resource_group.main.location}

  === Container Registry ===
  ACR Name: ${azurerm_container_registry.acr.name}
  Login Server: ${azurerm_container_registry.acr.login_server}

  === Database ===
  PostgreSQL Server: ${azurerm_postgresql_flexible_server.postgres.name}
  Database Name: ${var.db_name}
  FQDN: ${azurerm_postgresql_flexible_server.postgres.fqdn}

  === Key Vault ===
  Key Vault Name: ${azurerm_key_vault.main.name}
  Vault URI: ${azurerm_key_vault.main.vault_uri}

  === Application ===
  Container Name: ${azurerm_container_group.main.name}
  Frontend URL: http://${azurerm_container_group.main.fqdn}:3000
  Backend URL: http://${azurerm_container_group.main.fqdn}:8080
  API Health: http://${azurerm_container_group.main.fqdn}:8080/api/health

  === Next Steps ===
  1. Build and push Docker image to ACR
  2. Restart container to pull latest image
  3. Verify application is running
  4. Configure custom domain (optional)

  ========================================
  EOT
}
