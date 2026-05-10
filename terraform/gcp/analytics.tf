# ============================================================================#
# Phase 2 — analytics pipeline (events → Pub/Sub → BigQuery, plus hourly dim sync).
# ============================================================================#

# BigQuery dataset, events fact table, users/orgs dimension tables, and views.
module "analytics_bigquery" {
  source = "./modules/analytics-bigquery"

  project_id  = var.project_id
  region      = var.region
  environment = var.environment
}

# Pub/Sub topic + DLQ + BigQuery subscription.
# Use the same condition that gates otel_collector (avoids a module-count cycle).
# publisher_sa is omitted — the otel-collector module manages its own IAM
# binding via events_topic_name, so no circular dependency arises here.
module "analytics_pubsub" {
  count  = var.otel_collector_image != "" ? 1 : 0
  source = "./modules/analytics-pubsub"

  project_id     = var.project_id
  environment    = var.environment
  bigquery_table = module.analytics_bigquery.events_table
}

# Cloud Run Job + Cloud Scheduler for hourly dimension sync (Postgres → BigQuery).
module "dimsync" {
  source = "./modules/dimsync-job"

  project_id           = var.project_id
  region               = var.region
  environment          = var.environment
  image                = "${var.region}-docker.pkg.dev/${var.project_id}/${var.artifact_registry_repository}/oscal-tools:${var.image_tag}"
  db_url               = "jdbc:postgresql:///${var.db_name}?cloudSqlInstance=${module.database.instance_connection_name}&socketFactory=com.google.cloud.sql.postgres.SocketFactory"
  bigquery_dataset_id  = module.analytics_bigquery.dataset_id
  cloud_sql_connection = module.database.instance_connection_name
  db_username          = var.db_username
  db_password          = random_password.db_password.result

  cpu             = var.dimsync_cpu
  memory          = var.dimsync_memory
  timeout_seconds = var.dimsync_timeout_seconds
  max_retries     = var.dimsync_max_retries
}
