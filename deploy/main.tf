# ==============================================================================
# Local Variables
# ==============================================================================

locals {
  resource_suffix = "${var.project_name}-${var.environment}"
  common_tags = merge(
    var.tags,
    {
      Environment = var.environment
      Terraform   = "true"
    }
  )
}

# ==============================================================================
# Resource Group
# ==============================================================================

resource "azurerm_resource_group" "main" {
  name     = "rg-${local.resource_suffix}"
  location = var.location
  tags     = local.common_tags
}

# ==============================================================================
# Container Registry
# ==============================================================================

resource "azurerm_container_registry" "main" {
  name                = replace("acr${local.resource_suffix}", "-", "")
  resource_group_name = azurerm_resource_group.main.name
  location            = azurerm_resource_group.main.location
  sku                 = var.acr_sku
  admin_enabled       = true

  tags = local.common_tags
}

# ==============================================================================
# Log Analytics Workspace
# ==============================================================================

resource "azurerm_log_analytics_workspace" "main" {
  name                = "log-${local.resource_suffix}"
  location            = azurerm_resource_group.main.location
  resource_group_name = azurerm_resource_group.main.name
  sku                 = "PerGB2018"
  retention_in_days   = var.log_retention_days

  tags = local.common_tags
}

# ==============================================================================
# Application Insights
# ==============================================================================

resource "azurerm_application_insights" "main" {
  count = var.enable_application_insights ? 1 : 0

  name                = "appi-${local.resource_suffix}"
  location            = azurerm_resource_group.main.location
  resource_group_name = azurerm_resource_group.main.name
  workspace_id        = azurerm_log_analytics_workspace.main.id
  application_type    = "web"

  tags = local.common_tags
}

# ==============================================================================
# Container Apps Environment
# ==============================================================================

resource "azurerm_container_app_environment" "main" {
  name                       = "cae-${local.resource_suffix}"
  location                   = azurerm_resource_group.main.location
  resource_group_name        = azurerm_resource_group.main.name
  log_analytics_workspace_id = azurerm_log_analytics_workspace.main.id

  tags = local.common_tags
}

# ==============================================================================
# Container App - Combined Mode (Single Container)
# ==============================================================================

resource "azurerm_container_app" "combined" {
  count = var.deployment_mode == "combined" ? 1 : 0

  name                         = "ca-${local.resource_suffix}"
  container_app_environment_id = azurerm_container_app_environment.main.id
  resource_group_name          = azurerm_resource_group.main.name
  revision_mode                = "Single"

  template {
    min_replicas = var.combined_min_replicas
    max_replicas = var.combined_max_replicas

    container {
      name   = "oscal-app"
      image  = "${azurerm_container_registry.main.login_server}/oscal-ux:${var.docker_image_tag}"
      cpu    = var.combined_container_cpu
      memory = var.combined_container_memory

      env {
        name  = "JAVA_OPTS"
        value = var.java_opts
      }

      env {
        name  = "NODE_ENV"
        value = var.node_env
      }

      env {
        name  = "PORT"
        value = "3000"
      }

      env {
        name  = "NEXT_PUBLIC_API_URL"
        value = "http://localhost:8080/api"
      }

      env {
        name  = "NEXT_PUBLIC_USE_MOCK"
        value = "false"
      }

      env {
        name  = "NEXT_TELEMETRY_DISABLED"
        value = var.enable_telemetry ? "0" : "1"
      }

      dynamic "env" {
        for_each = var.enable_application_insights ? [1] : []
        content {
          name  = "APPLICATIONINSIGHTS_CONNECTION_STRING"
          value = azurerm_application_insights.main[0].connection_string
        }
      }
    }
  }

  registry {
    server               = azurerm_container_registry.main.login_server
    username             = azurerm_container_registry.main.admin_username
    password_secret_name = "registry-password"
  }

  secret {
    name  = "registry-password"
    value = azurerm_container_registry.main.admin_password
  }

  ingress {
    external_enabled           = var.enable_ingress
    target_port                = 3000
    allow_insecure_connections = var.allow_insecure_connections

    traffic_weight {
      latest_revision = true
      percentage      = 100
    }
  }

  tags = local.common_tags
}

# ==============================================================================
# Container App - Backend (Separate Mode)
# ==============================================================================

resource "azurerm_container_app" "backend" {
  count = var.deployment_mode == "separate" ? 1 : 0

  name                         = "ca-backend-${local.resource_suffix}"
  container_app_environment_id = azurerm_container_app_environment.main.id
  resource_group_name          = azurerm_resource_group.main.name
  revision_mode                = "Single"

  template {
    min_replicas = var.backend_min_replicas
    max_replicas = var.backend_max_replicas

    container {
      name   = "backend"
      image  = "${azurerm_container_registry.main.login_server}/oscal-backend:${var.docker_image_tag}"
      cpu    = var.backend_container_cpu
      memory = var.backend_container_memory

      env {
        name  = "JAVA_OPTS"
        value = var.java_opts
      }

      env {
        name  = "SERVER_PORT"
        value = tostring(var.backend_port)
      }

      dynamic "env" {
        for_each = var.enable_application_insights ? [1] : []
        content {
          name  = "APPLICATIONINSIGHTS_CONNECTION_STRING"
          value = azurerm_application_insights.main[0].connection_string
        }
      }
    }
  }

  registry {
    server               = azurerm_container_registry.main.login_server
    username             = azurerm_container_registry.main.admin_username
    password_secret_name = "registry-password"
  }

  secret {
    name  = "registry-password"
    value = azurerm_container_registry.main.admin_password
  }

  ingress {
    external_enabled           = true
    target_port                = var.backend_port
    allow_insecure_connections = var.allow_insecure_connections

    traffic_weight {
      latest_revision = true
      percentage      = 100
    }
  }

  tags = local.common_tags
}

# ==============================================================================
# Container App - Frontend (Separate Mode)
# ==============================================================================

resource "azurerm_container_app" "frontend" {
  count = var.deployment_mode == "separate" ? 1 : 0

  name                         = "ca-frontend-${local.resource_suffix}"
  container_app_environment_id = azurerm_container_app_environment.main.id
  resource_group_name          = azurerm_resource_group.main.name
  revision_mode                = "Single"

  template {
    min_replicas = var.frontend_min_replicas
    max_replicas = var.frontend_max_replicas

    container {
      name   = "frontend"
      image  = "${azurerm_container_registry.main.login_server}/oscal-frontend:${var.docker_image_tag}"
      cpu    = var.frontend_container_cpu
      memory = var.frontend_container_memory

      env {
        name  = "NODE_ENV"
        value = var.node_env
      }

      env {
        name  = "PORT"
        value = tostring(var.frontend_port)
      }

      env {
        name  = "NEXT_PUBLIC_API_URL"
        value = var.deployment_mode == "separate" ? "https://${azurerm_container_app.backend[0].ingress[0].fqdn}/api" : "http://localhost:8080/api"
      }

      env {
        name  = "NEXT_PUBLIC_USE_MOCK"
        value = "false"
      }

      env {
        name  = "NEXT_TELEMETRY_DISABLED"
        value = var.enable_telemetry ? "0" : "1"
      }

      dynamic "env" {
        for_each = var.enable_application_insights ? [1] : []
        content {
          name  = "APPLICATIONINSIGHTS_CONNECTION_STRING"
          value = azurerm_application_insights.main[0].connection_string
        }
      }
    }
  }

  registry {
    server               = azurerm_container_registry.main.login_server
    username             = azurerm_container_registry.main.admin_username
    password_secret_name = "registry-password"
  }

  secret {
    name  = "registry-password"
    value = azurerm_container_registry.main.admin_password
  }

  ingress {
    external_enabled           = var.enable_ingress
    target_port                = var.frontend_port
    allow_insecure_connections = var.allow_insecure_connections

    traffic_weight {
      latest_revision = true
      percentage      = 100
    }
  }

  tags = local.common_tags

  depends_on = [azurerm_container_app.backend]
}

# ==============================================================================
# Storage Account (Optional - for file uploads)
# ==============================================================================

resource "azurerm_storage_account" "main" {
  count = var.enable_blob_storage ? 1 : 0

  name                     = replace("st${local.resource_suffix}", "-", "")
  resource_group_name      = azurerm_resource_group.main.name
  location                 = azurerm_resource_group.main.location
  account_tier             = "Standard"
  account_replication_type = var.storage_account_replication

  tags = local.common_tags
}

resource "azurerm_storage_container" "uploads" {
  count = var.enable_blob_storage ? 1 : 0

  name                  = "oscal-uploads"
  storage_account_name  = azurerm_storage_account.main[0].name
  container_access_type = "private"
}
