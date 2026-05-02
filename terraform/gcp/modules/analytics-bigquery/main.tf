resource "google_bigquery_dataset" "analytics" {
  project       = var.project_id
  dataset_id    = "analytics_${var.environment}"
  location      = var.region
  friendly_name = "OSCAL Hub analytics (${var.environment})"
  description   = "Phase 2: events fact + users/orgs dimensions for CS dashboards."
  default_partition_expiration_ms = var.events_partition_expiration_days * 24 * 3600 * 1000
}

resource "google_bigquery_table" "events" {
  project              = var.project_id
  dataset_id           = google_bigquery_dataset.analytics.dataset_id
  table_id             = "events"
  deletion_protection  = true

  time_partitioning {
    type          = "DAY"
    field         = "event_time"
    expiration_ms = var.events_partition_expiration_days * 24 * 3600 * 1000
  }

  clustering = ["org_id", "event_name"]

  schema = jsonencode([
    { name = "event_time",      type = "TIMESTAMP", mode = "REQUIRED" },
    { name = "event_name",      type = "STRING",    mode = "REQUIRED" },
    { name = "event_id",        type = "STRING",    mode = "REQUIRED" },
    { name = "trace_id",        type = "STRING",    mode = "NULLABLE" },
    { name = "span_id",         type = "STRING",    mode = "NULLABLE" },
    { name = "session_id",      type = "STRING",    mode = "NULLABLE" },
    { name = "user_id",         type = "STRING",    mode = "NULLABLE" },
    { name = "org_id",          type = "STRING",    mode = "NULLABLE" },
    { name = "service_name",    type = "STRING",    mode = "NULLABLE" },
    { name = "service_version", type = "STRING",    mode = "NULLABLE" },
    { name = "environment",     type = "STRING",    mode = "NULLABLE" },
    { name = "attributes",      type = "JSON",      mode = "NULLABLE" },
    { name = "ingested_at",     type = "TIMESTAMP", mode = "REQUIRED" }
  ])
}

resource "google_bigquery_table" "users" {
  project    = var.project_id
  dataset_id = google_bigquery_dataset.analytics.dataset_id
  table_id   = "users"
  deletion_protection = true

  schema = jsonencode([
    { name = "user_id",        type = "STRING",    mode = "REQUIRED" },
    { name = "username",       type = "STRING" },
    { name = "email",          type = "STRING" },
    { name = "first_name",     type = "STRING" },
    { name = "last_name",      type = "STRING" },
    { name = "org_id_primary", type = "STRING" },
    { name = "roles_global",   type = "STRING",    mode = "REPEATED" },
    { name = "created_at",     type = "TIMESTAMP" },
    { name = "last_login",     type = "TIMESTAMP" },
    { name = "active",         type = "BOOL" },
    { name = "synced_at",      type = "TIMESTAMP", mode = "REQUIRED" }
  ])
}

resource "google_bigquery_table" "orgs" {
  project    = var.project_id
  dataset_id = google_bigquery_dataset.analytics.dataset_id
  table_id   = "orgs"
  deletion_protection = true

  schema = jsonencode([
    { name = "org_id",       type = "STRING",    mode = "REQUIRED" },
    { name = "name",         type = "STRING" },
    { name = "description",  type = "STRING" },
    { name = "active",       type = "BOOL" },
    { name = "member_count", type = "INT64" },
    { name = "created_at",   type = "TIMESTAMP" },
    { name = "synced_at",    type = "TIMESTAMP", mode = "REQUIRED" }
  ])
}

resource "google_bigquery_table" "vw_events_enriched" {
  project    = var.project_id
  dataset_id = google_bigquery_dataset.analytics.dataset_id
  table_id   = "vw_events_enriched"
  deletion_protection = true

  view {
    query = <<EOT
SELECT
  e.*,
  u.email      AS user_email,
  u.first_name AS user_first_name,
  u.last_name  AS user_last_name,
  o.name       AS org_name
FROM `${var.project_id}.${google_bigquery_dataset.analytics.dataset_id}.events` e
LEFT JOIN `${var.project_id}.${google_bigquery_dataset.analytics.dataset_id}.users` u
  USING (user_id)
LEFT JOIN `${var.project_id}.${google_bigquery_dataset.analytics.dataset_id}.orgs` o
  USING (org_id)
EOT
    use_legacy_sql = false
  }
}

resource "google_bigquery_table" "vw_daily_active_orgs" {
  project    = var.project_id
  dataset_id = google_bigquery_dataset.analytics.dataset_id
  table_id   = "vw_daily_active_orgs"
  deletion_protection = true

  view {
    query = <<EOT
SELECT
  DATE(event_time) AS day,
  COUNT(DISTINCT org_id) AS active_orgs,
  COUNT(DISTINCT user_id) AS active_users,
  COUNT(*) AS event_count
FROM `${var.project_id}.${google_bigquery_dataset.analytics.dataset_id}.events`
WHERE org_id IS NOT NULL
GROUP BY day
EOT
    use_legacy_sql = false
  }
}

resource "google_bigquery_table" "vw_feature_popularity" {
  project    = var.project_id
  dataset_id = google_bigquery_dataset.analytics.dataset_id
  table_id   = "vw_feature_popularity"
  deletion_protection = true

  view {
    query = <<EOT
SELECT
  event_name,
  COUNT(*)                                   AS total_events,
  COUNT(DISTINCT user_id)                    AS distinct_users,
  COUNT(DISTINCT org_id)                     AS distinct_orgs,
  APPROX_QUANTILES(SAFE_CAST(JSON_VALUE(attributes, '$.duration_ms') AS INT64), 100)[OFFSET(50)] AS p50_duration_ms,
  APPROX_QUANTILES(SAFE_CAST(JSON_VALUE(attributes, '$.duration_ms') AS INT64), 100)[OFFSET(95)] AS p95_duration_ms,
  COUNTIF(JSON_VALUE(attributes, '$.outcome') = 'success') / NULLIF(COUNT(*), 0) AS success_rate
FROM `${var.project_id}.${google_bigquery_dataset.analytics.dataset_id}.events`
WHERE event_name LIKE '%_completed'
GROUP BY event_name
EOT
    use_legacy_sql = false
  }
}
