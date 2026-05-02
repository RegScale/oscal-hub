# ============================================================================#
# Cloud Logging → BigQuery sink for OSCAL Hub business events.
# ============================================================================#
# Events from TelemetryService are emitted as Logback log records under the
# "oscal-hub.events" logger. The OTel agent's Logback bridge sends them to
# the collector as OTel log records, the collector forwards them to Cloud
# Logging via the googlecloud exporter (default route in the routing
# connector). This sink picks them up from Cloud Logging and writes them to
# BigQuery as the durable analytics path.
#
# This replaces the Pub/Sub→BigQuery direct subscription path: the OTel log
# record's OTLP-shaped JSON doesn't match a flat BQ schema. Cloud Logging
# auto-creates a schema that matches the log entry shape, which is fine for
# the CS dashboard's purposes.
# ============================================================================#

# Sink writes matching log entries to a BigQuery dataset. The dataset must
# already exist; we reuse analytics_${env}. Cloud Logging will auto-create
# tables named after the log id (e.g. oscal_hub_collector_*).
resource "google_logging_project_sink" "oscal_events" {
  project     = var.project_id
  name        = "oscal-events-${var.environment}"
  destination = "bigquery.googleapis.com/projects/${var.project_id}/datasets/analytics_${var.environment}"

  # Match all log records from the oscal-hub.events SLF4J logger. The
  # collector's googlecloud exporter flattens OTel log record attributes into
  # labels.* and exposes the SLF4J logger name as labels.instrumentation_source.
  filter = <<-EOT
    logName="projects/${var.project_id}/logs/oscal-hub-collector"
    AND labels."instrumentation_source"="oscal-hub.events"
  EOT

  # Use a unique writer identity so we can grant only the bytes the sink
  # needs and it doesn't run as the Cloud Logging Service Account.
  unique_writer_identity = true

  # Partition by day (cheaper queries, parallel ingest).
  bigquery_options {
    use_partitioned_tables = true
  }
}

# The sink's writer identity needs roles/bigquery.dataEditor on the dataset.
resource "google_bigquery_dataset_iam_member" "events_sink_writer" {
  project    = var.project_id
  dataset_id = module.analytics_bigquery.dataset_id
  role       = "roles/bigquery.dataEditor"
  member     = google_logging_project_sink.oscal_events.writer_identity
}
