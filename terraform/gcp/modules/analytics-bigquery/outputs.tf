output "dataset_id"   { value = google_bigquery_dataset.analytics.dataset_id }
output "events_table" { value = "${var.project_id}:${google_bigquery_dataset.analytics.dataset_id}.events" }
