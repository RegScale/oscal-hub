# Registration-funnel observability (resilience plan, Phase 5).
#
# Log-based metrics + alert policies for the user/org onboarding flows. These
# exist because the July 2026 registration failures (user "iorga") sat in the
# logs for two days until the user reported them — nothing watched the funnel.
#
# Notification routing uses the same email channel as alerts.tf
# (var.alert_email; empty disables delivery but the policies still evaluate).

# --- Log-based metrics ------------------------------------------------------

# Server-side registration failures (audit line from AuditLogService). After
# the client-side password checklist shipped, these should be RARE — a burst
# means either an outage or a user fighting the form.
resource "google_logging_metric" "registration_failures" {
  project = var.project_id
  name    = "oscal_registration_failures"
  filter  = <<-EOT
    resource.type="cloud_run_revision"
    resource.labels.service_name="oscal-tools-${var.environment}"
    jsonPayload.message:"User registration failed"
  EOT

  metric_descriptor {
    metric_kind = "DELTA"
    value_type  = "INT64"
  }
}

# Nightly/scheduled job failures (Spring TaskUtils logs this exact phrase).
# The audit-log cleanup job failed silently every night for weeks before the
# July 2026 sweep found it.
resource "google_logging_metric" "scheduled_task_failures" {
  project = var.project_id
  name    = "oscal_scheduled_task_failures"
  filter  = <<-EOT
    resource.type="cloud_run_revision"
    resource.labels.service_name="oscal-tools-${var.environment}"
    jsonPayload.message:"Unexpected error occurred in scheduled task"
  EOT

  metric_descriptor {
    metric_kind = "DELTA"
    value_type  = "INT64"
  }
}

# Email delivery failures: the after-commit listener's terminal failure line
# plus the synchronous invitation-send failures. Onboarding depends on email;
# silent SendGrid breakage must page, not hide.
resource "google_logging_metric" "email_send_failures" {
  project = var.project_id
  name    = "oscal_email_send_failures"
  filter  = <<-EOT
    resource.type="cloud_run_revision"
    resource.labels.service_name="oscal-tools-${var.environment}"
    (jsonPayload.message:"Email send FAILED" OR jsonPayload.message:"Failed to send invitation email" OR jsonPayload.message:"Failed to re-send invitation email")
  EOT

  metric_descriptor {
    metric_kind = "DELTA"
    value_type  = "INT64"
  }
}

# --- Alert policies ---------------------------------------------------------

resource "google_monitoring_alert_policy" "registration_failures" {
  project      = var.project_id
  display_name = "Registration failures > 3 in 1h"
  combiner     = "OR"
  enabled      = true

  documentation {
    content = <<-EOT
      More than 3 server-side registration failures in the last hour on
      oscal-tools-${var.environment}. Either registration is broken or a user
      is stuck fighting the form (the Michaela Iorga failure mode). Check:
      gcloud logging read 'resource.labels.service_name="oscal-tools-${var.environment}" AND jsonPayload.message:"User registration failed"' --project=${var.project_id} --freshness=2h
    EOT
  }

  conditions {
    display_name = "registration failures per hour"
    condition_threshold {
      filter          = "metric.type=\"logging.googleapis.com/user/oscal_registration_failures\" resource.type=\"cloud_run_revision\""
      duration        = "0s"
      comparison      = "COMPARISON_GT"
      threshold_value = 3
      aggregations {
        alignment_period   = "3600s"
        per_series_aligner = "ALIGN_SUM"
      }
      trigger {
        count = 1
      }
    }
  }

  notification_channels = local.notification_channels
}

resource "google_monitoring_alert_policy" "scheduled_task_failure" {
  project      = var.project_id
  display_name = "Scheduled task failed"
  combiner     = "OR"
  enabled      = true

  documentation {
    content = <<-EOT
      A Spring scheduled task threw on oscal-tools-${var.environment}. These
      failures repeat silently on every run until fixed (the nightly audit
      cleanup broke this way for weeks). Check:
      gcloud logging read 'resource.labels.service_name="oscal-tools-${var.environment}" AND jsonPayload.message:"Unexpected error occurred in scheduled task"' --project=${var.project_id} --freshness=1d
    EOT
  }

  conditions {
    display_name = "any scheduled task failure"
    condition_threshold {
      filter          = "metric.type=\"logging.googleapis.com/user/oscal_scheduled_task_failures\" resource.type=\"cloud_run_revision\""
      duration        = "0s"
      comparison      = "COMPARISON_GT"
      threshold_value = 0
      aggregations {
        alignment_period   = "300s"
        per_series_aligner = "ALIGN_SUM"
      }
      trigger {
        count = 1
      }
    }
  }

  notification_channels = local.notification_channels
}

resource "google_monitoring_alert_policy" "email_send_failures" {
  project      = var.project_id
  display_name = "Onboarding email delivery failing"
  combiner     = "OR"
  enabled      = true

  documentation {
    content = <<-EOT
      Email sends (welcome / invitation / password reset / access request) are
      failing after retries on oscal-tools-${var.environment}. Onboarding and
      password recovery depend on these. Check SendGrid status and the API key,
      then:
      gcloud logging read 'resource.labels.service_name="oscal-tools-${var.environment}" AND (jsonPayload.message:"Email send FAILED" OR jsonPayload.message:"Failed to send invitation email")' --project=${var.project_id} --freshness=2h
    EOT
  }

  conditions {
    display_name = "email failures in 15 min"
    condition_threshold {
      filter          = "metric.type=\"logging.googleapis.com/user/oscal_email_send_failures\" resource.type=\"cloud_run_revision\""
      duration        = "0s"
      comparison      = "COMPARISON_GT"
      threshold_value = 0
      aggregations {
        alignment_period   = "900s"
        per_series_aligner = "ALIGN_SUM"
      }
      trigger {
        count = 1
      }
    }
  }

  notification_channels = local.notification_channels
}
