# ============================================================================#
# vw_events: friendly view over the sink-created oscal_hub_collector table.
# ============================================================================#
# Cloud Logging → BigQuery sink writes log entries into a wide schema with
# nested labels.* and resource.labels.*. Looker Studio (and the existing
# vw_events_enriched view) wants a flat shape matching the original events
# table schema. This view bridges the two without changing the sink.
# ============================================================================#

resource "google_bigquery_table" "vw_events" {
  project             = var.project_id
  dataset_id          = module.analytics_bigquery.dataset_id
  table_id            = "vw_events"
  deletion_protection = true

  view {
    query          = <<-EOT
      SELECT
        timestamp                                AS event_time,
        labels.event_name                        AS event_name,
        insertId                                 AS event_id,
        SPLIT(`trace`, '/')[SAFE_OFFSET(3)]      AS trace_id,
        spanId                                   AS span_id,
        CAST(NULL AS STRING)                     AS session_id,
        labels.attempted_username_sha256         AS user_attempted_sha256,
        labels.reason                            AS reason,
        labels.service_name                      AS service_name,
        labels.service_namespace                 AS service_namespace,
        labels.thread_name                       AS thread_name,
        labels.thread_id                         AS thread_id,
        labels                                   AS attributes,
        receiveTimestamp                         AS ingested_at
      FROM `${var.project_id}.${module.analytics_bigquery.dataset_id}.oscal_hub_collector`
      WHERE labels.instrumentation_source = 'oscal-hub.events'
    EOT
    use_legacy_sql = false
  }

  depends_on = [google_logging_project_sink.oscal_events]
}
