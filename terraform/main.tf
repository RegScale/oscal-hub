# Main Terraform Configuration
# Creates the resource group and random suffix

# Random suffix for unique resource names
resource "random_string" "suffix" {
  length  = 6
  special = false
  upper   = false
  numeric = true
}

# Resource Group
resource "azurerm_resource_group" "main" {
  name     = var.resource_group_name
  location = var.location
  tags     = var.tags
}

# Local values for computed names
locals {
  # Unique suffix for resource names
  suffix = random_string.suffix.result

  # Resource naming
  acr_name      = "oscalcr${local.suffix}"
  db_server     = "oscal-db-${local.suffix}"
  keyvault_name = "oscal-kv-${local.suffix}"
  container_name = "oscal-ux"
  app_insights_name = "oscal-insights-${local.suffix}"

  # Common tags
  common_tags = merge(
    var.tags,
    {
      ResourceGroup = azurerm_resource_group.main.name
      Location      = azurerm_resource_group.main.location
    }
  )
}
