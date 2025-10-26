# Azure Key Vault
# Secure storage for secrets and configuration

# Get current Azure client configuration
data "azurerm_client_config" "current" {}

resource "azurerm_key_vault" "main" {
  name                = local.keyvault_name
  location            = azurerm_resource_group.main.location
  resource_group_name = azurerm_resource_group.main.name
  tenant_id           = data.azurerm_client_config.current.tenant_id

  # SKU
  sku_name = "standard"

  # Soft delete and purge protection
  soft_delete_retention_days = 7
  purge_protection_enabled   = false # Enable for production

  # Network access
  public_network_access_enabled = true

  # Enable RBAC for access control (recommended)
  enable_rbac_authorization = false

  tags = local.common_tags
}

# Access policy for Terraform/admin user
resource "azurerm_key_vault_access_policy" "terraform" {
  key_vault_id = azurerm_key_vault.main.id
  tenant_id    = data.azurerm_client_config.current.tenant_id
  object_id    = data.azurerm_client_config.current.object_id

  secret_permissions = [
    "Get", "List", "Set", "Delete", "Backup", "Restore", "Recover", "Purge"
  ]

  key_permissions = [
    "Get", "List", "Create", "Delete", "Backup", "Restore", "Recover", "Purge"
  ]

  certificate_permissions = [
    "Get", "List", "Create", "Delete", "Backup", "Restore", "Recover", "Purge"
  ]
}

# Store secrets in Key Vault
resource "azurerm_key_vault_secret" "jwt_secret" {
  name         = "JWT-SECRET"
  value        = var.jwt_secret
  key_vault_id = azurerm_key_vault.main.id

  depends_on = [azurerm_key_vault_access_policy.terraform]

  tags = local.common_tags
}

resource "azurerm_key_vault_secret" "db_password" {
  name         = "DB-PASSWORD"
  value        = var.db_password
  key_vault_id = azurerm_key_vault.main.id

  depends_on = [azurerm_key_vault_access_policy.terraform]

  tags = local.common_tags
}

resource "azurerm_key_vault_secret" "db_username" {
  name         = "DB-USERNAME"
  value        = var.db_username
  key_vault_id = azurerm_key_vault.main.id

  depends_on = [azurerm_key_vault_access_policy.terraform]

  tags = local.common_tags
}

resource "azurerm_key_vault_secret" "db_connection_string" {
  name         = "DB-CONNECTION-STRING"
  value        = "jdbc:postgresql://${azurerm_postgresql_flexible_server.postgres.fqdn}:5432/${var.db_name}?sslmode=require"
  key_vault_id = azurerm_key_vault.main.id

  depends_on = [azurerm_key_vault_access_policy.terraform]

  tags = local.common_tags
}

resource "azurerm_key_vault_secret" "cors_allowed_origins" {
  name         = "CORS-ALLOWED-ORIGINS"
  value        = var.cors_allowed_origins
  key_vault_id = azurerm_key_vault.main.id

  depends_on = [azurerm_key_vault_access_policy.terraform]

  tags = local.common_tags
}

# Outputs
output "keyvault_uri" {
  description = "Key Vault URI"
  value       = azurerm_key_vault.main.vault_uri
}

output "keyvault_name" {
  description = "Key Vault name"
  value       = azurerm_key_vault.main.name
}
