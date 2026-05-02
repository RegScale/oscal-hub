variable "alert_email" {
  type        = string
  default     = ""
  description = "Email address to receive Cloud Monitoring alerts; empty disables the email channel."
}

resource "google_monitoring_notification_channel" "email" {
  count        = var.alert_email != "" ? 1 : 0
  project      = var.project_id
  display_name = "OSCAL Hub Alerts"
  type         = "email"
  labels = {
    email_address = var.alert_email
  }
}

locals {
  notification_channels = google_monitoring_notification_channel.email[*].id
}

resource "google_monitoring_alert_policy" "high_5xx_rate" {
  project      = var.project_id
  display_name = "Cloud Run 5xx rate > 1% (5min)"
  combiner     = "OR"
  enabled      = true

  conditions {
    display_name = "5xx rate"
    condition_threshold {
      filter          = "metric.type=\"run.googleapis.com/request_count\" resource.type=\"cloud_run_revision\" metric.label.\"response_code_class\"=\"5xx\""
      duration        = "300s"
      comparison      = "COMPARISON_GT"
      threshold_value = 0.01
      aggregations {
        alignment_period   = "60s"
        per_series_aligner = "ALIGN_RATE"
      }
    }
  }

  notification_channels = local.notification_channels
}

# threshold_value 5000 = 5000 milliseconds = 5 seconds.
# Cloud Run request_latencies distribution metric reports in milliseconds.
# Verify at smoke test that values shown in Cloud Monitoring match expectation.
resource "google_monitoring_alert_policy" "p99_latency" {
  project      = var.project_id
  display_name = "p99 latency > 5s on validate/convert/resolve"
  combiner     = "OR"
  enabled      = true

  conditions {
    display_name = "validate/convert/resolve p99"
    condition_threshold {
      filter          = "metric.type=\"run.googleapis.com/request_latencies\" resource.type=\"cloud_run_revision\""
      duration        = "600s"
      comparison      = "COMPARISON_GT"
      threshold_value = 5000
      aggregations {
        alignment_period     = "60s"
        per_series_aligner   = "ALIGN_DELTA"
        cross_series_reducer = "REDUCE_PERCENTILE_99"
        group_by_fields      = ["resource.label.service_name"]
      }
    }
  }

  notification_channels = local.notification_channels
}

resource "google_monitoring_alert_policy" "sql_connection_saturation" {
  project      = var.project_id
  display_name = "Cloud SQL connection saturation > 80%"
  combiner     = "OR"
  enabled      = true

  conditions {
    display_name = "connections / max"
    condition_threshold {
      filter          = "metric.type=\"cloudsql.googleapis.com/database/postgresql/num_backends\" resource.type=\"cloudsql_database\""
      duration        = "300s"
      comparison      = "COMPARISON_GT"
      threshold_value = 80
      aggregations {
        alignment_period   = "60s"
        per_series_aligner = "ALIGN_MEAN"
      }
    }
  }

  notification_channels = local.notification_channels
}

resource "google_monitoring_alert_policy" "run_cpu_high" {
  project      = var.project_id
  display_name = "Cloud Run CPU utilization > 80% (5min)"
  combiner     = "OR"
  enabled      = true

  conditions {
    display_name = "container CPU utilization"
    condition_threshold {
      filter          = "metric.type=\"run.googleapis.com/container/cpu/utilizations\" resource.type=\"cloud_run_revision\""
      duration        = "300s"
      comparison      = "COMPARISON_GT"
      threshold_value = 0.8
      # Cloud Run CPU utilization is a DELTA+DISTRIBUTION metric, so the
      # aligner must collapse it to a scalar. ALIGN_PERCENTILE_99 picks the
      # 99th-percentile value within each alignment period, which catches
      # spikes that ALIGN_MEAN would smooth over.
      aggregations {
        alignment_period     = "60s"
        per_series_aligner   = "ALIGN_PERCENTILE_99"
        cross_series_reducer = "REDUCE_MEAN"
        group_by_fields      = ["resource.label.service_name"]
      }
    }
  }

  notification_channels = local.notification_channels
}

