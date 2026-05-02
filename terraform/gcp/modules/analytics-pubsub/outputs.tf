output "topic_id" {
  value       = google_pubsub_topic.events.id
  description = "Fully-qualified Pub/Sub topic id."
}

output "topic_name" {
  value       = google_pubsub_topic.events.name
  description = "Short topic name."
}

output "subscription_id" {
  value = google_pubsub_subscription.events_to_bq.id
}

output "dlq_topic_name" {
  value = google_pubsub_topic.events_dlq.name
}
