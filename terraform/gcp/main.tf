# ============================================================================#
# OSCAL Tools - Google Cloud Platform Infrastructure
# ============================================================================#
# This Terraform configuration deploys OSCAL Tools to GCP with:
# - Cloud Run services (frontend + backend)
# - Cloud SQL PostgreSQL database
# - Cloud Storage buckets
# - Secret Manager for sensitive data
# - VPC Connector for private networking
# - Load Balancer for custom domain (optional)
# ============================================================================#

terraform {
  required_version = ">= 1.5.0"

  required_providers {
    google = {
      source  = "hashicorp/google"
      version = "~> 5.0"
    }
    google-beta = {
      source  = "hashicorp/google-beta"
      version = "~> 5.0"
    }
    random = {
      source  = "hashicorp/random"
      version = "~> 3.5"
    }
  }

  # State lives in GCS. Bucket is supplied at `terraform init` time via
  # `-backend-config="bucket=..."` so the same config works for any project /
  # environment. See docs/CICD-BOOTSTRAP.md.
  backend "gcs" {
    prefix = "terraform/state"
  }
}

# ============================================================================#
# Provider Configuration
# ============================================================================#

provider "google" {
  project = var.project_id
  region  = var.region
}

provider "google-beta" {
  project = var.project_id
  region  = var.region
}

# ============================================================================#
# Enable Required APIs
# ============================================================================#

resource "google_project_service" "apis" {
  for_each = toset([
    "run.googleapis.com",           # Cloud Run
    "sql-component.googleapis.com", # Cloud SQL
    "sqladmin.googleapis.com",      # Cloud SQL Admin
    "storage.googleapis.com",       # Cloud Storage
    "secretmanager.googleapis.com", # Secret Manager
    "vpcaccess.googleapis.com",     # VPC Access (for Cloud SQL)
    "cloudresourcemanager.googleapis.com",
    "servicenetworking.googleapis.com",
    "compute.googleapis.com",
    "cloudbuild.googleapis.com",       # Cloud Build
    "artifactregistry.googleapis.com", # Artifact Registry
    "cloudkms.googleapis.com",         # Cloud KMS (for encryption)
    "cloudtrace.googleapis.com",       # Cloud Trace
    "monitoring.googleapis.com",       # Cloud Monitoring + Managed Prometheus
    "pubsub.googleapis.com",           # Pub/Sub (used by Phase 2)
    "cloudscheduler.googleapis.com",   # Cloud Scheduler (Phase 2 dimsync)
    "bigquery.googleapis.com",         # BigQuery (Phase 2 analytics)
  ])

  project = var.project_id
  service = each.key

  disable_on_destroy = false
}

# ============================================================================#
# Random Suffix for Unique Resource Names
# ============================================================================#

resource "random_id" "db_name_suffix" {
  byte_length = 4
}

resource "random_password" "db_password" {
  length  = 32
  special = true
}

resource "random_password" "jwt_secret" {
  length  = 64
  special = false
}

# ============================================================================#
# Secret Manager - Store Sensitive Configuration
# ============================================================================#
# Disabled - passing secrets directly as environment variables to avoid permission issues

# module "secrets" {
#   source = "./modules/secrets"
#
#   project_id  = var.project_id
#   db_password = random_password.db_password.result
#   jwt_secret  = random_password.jwt_secret.result
#
#   depends_on = [google_project_service.apis]
# }

# ============================================================================#
# Networking - VPC Connector for Cloud SQL Private Access
# ============================================================================#
# Commented out - using Cloud SQL public IP with authorized networks instead
# This avoids VPC peering permission issues

# module "networking" {
#   source = "./modules/networking"
#
#   project_id = var.project_id
#   region     = var.region
#
#   depends_on = [google_project_service.apis]
# }

# ============================================================================#
# Cloud Storage - OSCAL File Storage
# ============================================================================#

module "storage" {
  source = "./modules/cloud-storage"

  project_id    = var.project_id
  region        = var.region
  environment   = var.environment
  bucket_prefix = var.bucket_prefix

  depends_on = [google_project_service.apis]
}

# ============================================================================#
# Cloud SQL - PostgreSQL Database
# ============================================================================#

module "database" {
  source = "./modules/cloud-sql"

  project_id     = var.project_id
  region         = var.region
  environment    = var.environment
  db_name_suffix = random_id.db_name_suffix.hex
  db_name        = var.db_name
  db_username    = var.db_username
  db_password    = random_password.db_password.result
  db_tier        = var.db_tier

  depends_on = [
    google_project_service.apis
  ]
}

# ============================================================================#
# Cloud Run - OSCAL Tools Service (Backend + Frontend)
# ============================================================================#

module "oscal_app" {
  source = "./modules/cloud-run"

  project_id   = var.project_id
  region       = var.region
  environment  = var.environment
  service_name = "oscal-tools"

  image = "${var.region}-docker.pkg.dev/${var.project_id}/${var.artifact_registry_repository}/oscal-tools:${var.image_tag}"

  # Container port (Cloud Run sets PORT=8080, Next.js frontend listens on it)
  # Backend Spring Boot runs on port 8081
  container_port = 8080

  environment_variables = merge(
    {
      # Spring Boot backend settings
      SPRING_PROFILES_ACTIVE = "gcp"
      GCP_PROJECT_ID         = var.project_id
      DB_USERNAME            = var.db_username
      DB_PASSWORD            = random_password.db_password.result
      JWT_SECRET             = random_password.jwt_secret.result
      GCS_BUCKET_BUILD       = module.storage.build_bucket_name
      BACKEND_PORT           = "8081"

      # Next.js frontend settings
      NODE_ENV = "production"
      # Note: NEXT_PUBLIC_* variables are baked into the build at Docker build time
      # They cannot be changed at runtime and are set in the Dockerfile
    },
    var.otel_enabled && length(module.otel_collector) > 0 ? {
      JAVA_TOOL_OPTIONS                                                 = "-javaagent:/otel/opentelemetry-javaagent.jar"
      OTEL_SERVICE_NAME                                                 = "oscal-api"
      OTEL_RESOURCE_ATTRIBUTES                                          = "service.namespace=oscal-hub,deployment.environment=${var.environment}"
      OTEL_EXPORTER_OTLP_PROTOCOL                                       = "grpc"
      OTEL_EXPORTER_OTLP_ENDPOINT                                       = module.otel_collector[0].collector_url
      OTEL_TRACES_SAMPLER                                               = "parentbased_always_on"
      OTEL_LOGS_EXPORTER                                                = "otlp"
      OTEL_METRICS_EXPORTER                                             = "otlp"
      OTEL_INSTRUMENTATION_LOGBACK_APPENDER_EXPERIMENTAL_LOG_ATTRIBUTES = "true"
      OTEL_INSTRUMENTATION_MICROMETER_ENABLED                           = "true"
      # Copies baggage entries onto every span as attributes. Per OTel
      # Java agent v2.x: otel.java.experimental.span-attributes.copy-from-baggage.include.
      # `*` means copy all baggage keys; restrict if cardinality becomes a concern.
      OTEL_JAVA_EXPERIMENTAL_SPAN_ATTRIBUTES_COPY_FROM_BAGGAGE_INCLUDE   = "*"
      # Same for log records — flows user.id/org.id into Cloud Logging
      OTEL_JAVA_EXPERIMENTAL_LOG_ATTRIBUTES_COPY_FROM_BAGGAGE_INCLUDE    = "*"
    } : {}
  )

  secret_environment_variables = {}

  db_url      = "jdbc:postgresql:///${var.db_name}?cloudSqlInstance=${module.database.instance_connection_name}&socketFactory=com.google.cloud.sql.postgres.SocketFactory"
  db_ddl_auto = "update"

  cloud_sql_connections = [module.database.instance_connection_name]
  vpc_connector_id      = null  # Using public IP, no VPC connector needed

  # Combined resources (backend + frontend in one container)
  max_instances = var.app_max_instances
  min_instances = var.app_min_instances
  cpu_limit     = var.app_cpu
  memory_limit  = var.app_memory


  # Health check on backend API health endpoint
  health_check_path = "/api/health"

  custom_domain = var.custom_domain

  depends_on = [
    google_project_service.apis,
    module.database,
    module.storage
  ]
}

# ============================================================================#
# OpenTelemetry Collector (conditionally deployed when otel_collector_image is set)
# ============================================================================#

module "otel_collector" {
  count  = var.otel_collector_image != "" ? 1 : 0
  source = "./modules/otel-collector"

  project_id          = var.project_id
  region              = var.region
  environment         = var.environment
  image               = var.otel_collector_image
  api_service_account = module.oscal_app.service_account_email

  events_topic_name = length(module.analytics_pubsub) > 0 ? module.analytics_pubsub[0].topic_name : ""

  depends_on = [google_project_service.apis]
}

# ============================================================================#
# IAM - Service Account Permissions
# ============================================================================#
# Note: These permissions need to be granted manually via gcloud or GCP Console
# if you don't have IAM admin permissions

# Grant app service account access to Cloud SQL
# resource "google_project_iam_member" "app_sql_client" {
#   project = var.project_id
#   role    = "roles/cloudsql.client"
#   member  = "serviceAccount:${module.oscal_app.service_account_email}"
# }

# Grant app service account access to Cloud Storage
# resource "google_storage_bucket_iam_member" "app_storage_admin" {
#   bucket = module.storage.build_bucket_name
#   role   = "roles/storage.objectAdmin"
#   member  = "serviceAccount:${module.oscal_app.service_account_email}"
# }

# Grant app service account access to Secret Manager
# Disabled - not using Secret Manager, passing values directly as env vars
# resource "google_secret_manager_secret_iam_member" "app_jwt_secret_accessor" {
#   secret_id = module.secrets.jwt_secret_id
#   role      = "roles/secretmanager.secretAccessor"
#   member    = "serviceAccount:${module.oscal_app.service_account_email}"
# }
#
# resource "google_secret_manager_secret_iam_member" "app_db_password_accessor" {
#   secret_id = module.secrets.db_password_id
#   role      = "roles/secretmanager.secretAccessor"
#   member    = "serviceAccount:${module.oscal_app.service_account_email}"
# }
