resource "google_pubsub_topic" "events" {
  project = var.project_id
  name    = "otel-events-${var.environment}"
}

resource "google_pubsub_topic" "events_dlq" {
  project = var.project_id
  name    = "otel-events-dlq-${var.environment}"
}

resource "google_pubsub_subscription" "events_to_bq" {
  project = var.project_id
  name    = "otel-events-bq-${var.environment}"
  topic   = google_pubsub_topic.events.id

  bigquery_config {
    table            = var.bigquery_table
    use_table_schema = true
    write_metadata   = false  # our events table has its own event_id/event_time/etc; Pub/Sub metadata not needed
  }

  ack_deadline_seconds = 60

  dead_letter_policy {
    dead_letter_topic     = google_pubsub_topic.events_dlq.id
    max_delivery_attempts = 5
  }
}

# Optional — only created when publisher_sa is explicitly provided.
# When the otel-collector module is used, it handles the IAM binding itself
# via its events_topic_name variable, avoiding a circular dependency.
resource "google_pubsub_topic_iam_member" "publisher" {
  count   = var.publisher_sa != "" ? 1 : 0
  project = var.project_id
  topic   = google_pubsub_topic.events.name
  role    = "roles/pubsub.publisher"
  member  = "serviceAccount:${var.publisher_sa}"
}

# Pub/Sub uses an automatically-managed service-side identity to write to BQ;
# grant it bigquery.dataEditor on the project (BigQuery subscription docs).
data "google_project" "current" {
  project_id = var.project_id
}

resource "google_project_iam_member" "pubsub_to_bq" {
  project = var.project_id
  role    = "roles/bigquery.dataEditor"
  member  = "serviceAccount:service-${data.google_project.current.number}@gcp-sa-pubsub.iam.gserviceaccount.com"
}

# And metadataViewer for the BigQuery subscription health.
resource "google_project_iam_member" "pubsub_to_bq_metadata" {
  project = var.project_id
  role    = "roles/bigquery.metadataViewer"
  member  = "serviceAccount:service-${data.google_project.current.number}@gcp-sa-pubsub.iam.gserviceaccount.com"
}
