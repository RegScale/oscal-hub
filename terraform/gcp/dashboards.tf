resource "google_monitoring_dashboard" "ops" {
  project        = var.project_id
  dashboard_json = file("${path.module}/dashboards/ops-dashboard.json")
}

resource "google_monitoring_dashboard" "cs_pipeline" {
  project        = var.project_id
  dashboard_json = file("${path.module}/dashboards/cs-pipeline-dashboard.json")
}
