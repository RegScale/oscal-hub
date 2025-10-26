# Azure Container Instance
# Hosts the OSCAL Tools application container

# Create a managed identity for the container
resource "azurerm_user_assigned_identity" "container" {
  name                = "${local.container_name}-identity"
  resource_group_name = azurerm_resource_group.main.name
  location            = azurerm_resource_group.main.location

  tags = local.common_tags
}

# Grant the container identity access to Key Vault
resource "azurerm_key_vault_access_policy" "container" {
  key_vault_id = azurerm_key_vault.main.id
  tenant_id    = data.azurerm_client_config.current.tenant_id
  object_id    = azurerm_user_assigned_identity.container.principal_id

  secret_permissions = [
    "Get", "List"
  ]

  depends_on = [azurerm_key_vault_access_policy.terraform]
}

# Grant the container identity permission to pull from ACR
resource "azurerm_role_assignment" "acr_pull" {
  principal_id         = azurerm_user_assigned_identity.container.principal_id
  role_definition_name = "AcrPull"
  scope                = azurerm_container_registry.acr.id
}

# Azure Container Instance
resource "azurerm_container_group" "main" {
  name                = local.container_name
  resource_group_name = azurerm_resource_group.main.name
  location            = azurerm_resource_group.main.location
  os_type             = "Linux"
  ip_address_type     = var.enable_public_access ? "Public" : "Private"
  dns_name_label      = "${var.project_name}-${var.environment}"

  # Managed identity for Key Vault access
  identity {
    type = "UserAssigned"
    identity_ids = [
      azurerm_user_assigned_identity.container.id
    ]
  }

  # Image registry credentials (using ACR admin)
  image_registry_credential {
    server   = azurerm_container_registry.acr.login_server
    username = azurerm_container_registry.acr.admin_username
    password = azurerm_container_registry.acr.admin_password
  }

  # Main application container
  container {
    name   = "oscal-ux"
    image  = "${azurerm_container_registry.acr.login_server}/oscal-ux:latest"
    cpu    = var.container_cpu
    memory = var.container_memory

    # Port exposures
    ports {
      port     = 3000
      protocol = "TCP"
    }

    ports {
      port     = 8080
      protocol = "TCP"
    }

    # Environment variables (loaded from Key Vault references)
    environment_variables = {
      SPRING_PROFILES_ACTIVE        = "prod"
      SERVER_PORT                   = "8080"
      NODE_ENV                      = "production"
      PORT                          = "3000"
      NEXT_PUBLIC_API_URL           = "http://localhost:8080/api"
      NEXT_PUBLIC_USE_MOCK          = "false"

      # Database configuration
      DB_DRIVER                     = "org.postgresql.Driver"
      DB_DIALECT                    = "org.hibernate.dialect.PostgreSQLDialect"
      DB_USERNAME                   = var.db_username
      DB_NAME                       = var.db_name

      # Security configuration
      SECURITY_HEADERS_ENABLED      = "true"
      SECURITY_REQUIRE_HTTPS        = "true"
      RATE_LIMIT_ENABLED            = "true"
      ACCOUNT_LOCKOUT_ENABLED       = "true"
      AUDIT_LOGGING_ENABLED         = "true"

      # Flyway configuration (enable migrations)
      SPRING_FLYWAY_ENABLED         = "true"
      SPRING_FLYWAY_BASELINE_ON_MIGRATE = "true"
    }

    # Secure environment variables (from Key Vault)
    secure_environment_variables = {
      JWT_SECRET            = azurerm_key_vault_secret.jwt_secret.value
      DB_PASSWORD           = azurerm_key_vault_secret.db_password.value
      DB_URL                = azurerm_key_vault_secret.db_connection_string.value
      CORS_ALLOWED_ORIGINS  = azurerm_key_vault_secret.cors_allowed_origins.value
    }

    # Liveness probe (check if backend is responding)
    liveness_probe {
      http_get {
        path   = "/api/health"
        port   = 8080
        scheme = "Http"
      }
      initial_delay_seconds = 60
      period_seconds        = 30
      failure_threshold     = 3
      timeout_seconds       = 10
    }

    # Readiness probe (check if frontend is responding)
    readiness_probe {
      http_get {
        path   = "/"
        port   = 3000
        scheme = "Http"
      }
      initial_delay_seconds = 30
      period_seconds        = 10
      failure_threshold     = 3
      timeout_seconds       = 5
    }
  }

  # Restart policy
  restart_policy = "Always"

  tags = local.common_tags

  depends_on = [
    azurerm_key_vault_access_policy.container,
    azurerm_role_assignment.acr_pull,
    azurerm_postgresql_flexible_server_database.main
  ]
}

# Outputs
output "container_ip_address" {
  description = "Public IP address of the container"
  value       = azurerm_container_group.main.ip_address
}

output "container_fqdn" {
  description = "Fully qualified domain name of the container"
  value       = azurerm_container_group.main.fqdn
}

output "application_url" {
  description = "Application URL (frontend)"
  value       = "http://${azurerm_container_group.main.fqdn}:3000"
}

output "api_url" {
  description = "API URL (backend)"
  value       = "http://${azurerm_container_group.main.fqdn}:8080"
}
