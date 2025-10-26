# Azure Database for PostgreSQL Flexible Server
# Production-grade managed database

resource "azurerm_postgresql_flexible_server" "postgres" {
  name                = local.db_server
  resource_group_name = azurerm_resource_group.main.name
  location            = azurerm_resource_group.main.location

  # Administrator credentials
  administrator_login    = var.db_username
  administrator_password = var.db_password

  # Version and SKU
  version = "15"
  sku_name = var.db_sku

  # Storage configuration
  storage_mb = var.db_storage_mb

  # Backup configuration
  backup_retention_days        = 7
  geo_redundant_backup_enabled = false # Enable for production

  # High availability (disabled to reduce cost, enable for production)
  # high_availability {
  #   mode = "ZoneRedundant"
  # }

  # Security
  public_network_access_enabled = true # Set to false for VNet only

  tags = local.common_tags
}

# Firewall rule to allow Azure services
resource "azurerm_postgresql_flexible_server_firewall_rule" "allow_azure_services" {
  name             = "AllowAzureServices"
  server_id        = azurerm_postgresql_flexible_server.postgres.id
  start_ip_address = "0.0.0.0"
  end_ip_address   = "0.0.0.0"
}

# Firewall rules for specific IP addresses (GitHub Actions, admin access)
resource "azurerm_postgresql_flexible_server_firewall_rule" "allowed_ips" {
  count            = length(var.allowed_ip_addresses)
  name             = "AllowedIP-${count.index}"
  server_id        = azurerm_postgresql_flexible_server.postgres.id
  start_ip_address = var.allowed_ip_addresses[count.index]
  end_ip_address   = var.allowed_ip_addresses[count.index]
}

# Create production database
resource "azurerm_postgresql_flexible_server_database" "main" {
  name      = var.db_name
  server_id = azurerm_postgresql_flexible_server.postgres.id
  charset   = "UTF8"
  collation = "en_US.utf8"
}

# PostgreSQL configuration (performance tuning)
resource "azurerm_postgresql_flexible_server_configuration" "max_connections" {
  name      = "max_connections"
  server_id = azurerm_postgresql_flexible_server.postgres.id
  value     = "100"
}

resource "azurerm_postgresql_flexible_server_configuration" "shared_buffers" {
  name      = "shared_buffers"
  server_id = azurerm_postgresql_flexible_server.postgres.id
  value     = "256000" # 256 MB (in 8KB blocks)
}

# Output database connection info
output "database_fqdn" {
  description = "PostgreSQL server FQDN"
  value       = azurerm_postgresql_flexible_server.postgres.fqdn
}

output "database_name" {
  description = "PostgreSQL database name"
  value       = azurerm_postgresql_flexible_server_database.main.name
}

output "database_connection_string" {
  description = "PostgreSQL connection string (for Spring Boot)"
  value       = "jdbc:postgresql://${azurerm_postgresql_flexible_server.postgres.fqdn}:5432/${var.db_name}?sslmode=require"
  sensitive   = true
}
