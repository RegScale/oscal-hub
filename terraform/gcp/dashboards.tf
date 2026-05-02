resource "google_monitoring_dashboard" "ops" {
  project        = var.project_id
  dashboard_json = file("${path.module}/dashboards/ops-dashboard.json")
}
