# Google Cloud Platform - Cost Estimation & Monitoring Guide

**Status:** Production Ready
**Date:** 2025-01-29
**Last Updated:** 2025-01-29

## Table of Contents

1. [Cost Estimation](#cost-estimation)
2. [Cost Optimization Strategies](#cost-optimization-strategies)
3. [Monitoring Setup](#monitoring-setup)
4. [Alerting Configuration](#alerting-configuration)
5. [Performance Optimization](#performance-optimization)
6. [Budget Management](#budget-management)

---

## Cost Estimation

### Pricing Calculator

Use the [Google Cloud Pricing Calculator](https://cloud.google.com/products/calculator) to estimate costs based on your specific usage patterns.

### Component Breakdown

#### 1. Cloud Run (Serverless Containers)

**Pricing Model:** Pay per request + CPU/Memory usage

**Backend API:**
```
Assumptions:
- 100,000 requests/month
- Average request duration: 500ms
- CPU: 1 vCPU (1000m)
- Memory: 2GB

Monthly Cost:
- Requests: (100,000 - 2M free) × $0.40/million = $0 (within free tier)
- CPU time: 100,000 × 0.5s × $0.00002400/vCPU-second = $1.20
- Memory: 100,000 × 0.5s × 2GB × $0.0000025/GB-second = $0.25
- Total: ~$1.45/month

High Traffic (1M requests/month, 1s avg duration):
- Requests: (1M - 2M free) × $0.40/million = $0
- CPU time: 1M × 1s × $0.00002400 = $24.00
- Memory: 1M × 1s × 2GB × $0.0000025 = $5.00
- Total: ~$29/month
```

**Frontend:**
```
Assumptions:
- 200,000 requests/month
- Average request duration: 200ms
- CPU: 1 vCPU
- Memory: 512MB

Monthly Cost:
- Requests: $0 (within free tier)
- CPU time: 200,000 × 0.2s × $0.00002400 = $0.96
- Memory: 200,000 × 0.2s × 0.5GB × $0.0000025 = $0.05
- Total: ~$1/month
```

**Cloud Run Free Tier (per month):**
- 2 million requests
- 360,000 vCPU-seconds
- 180,000 GiB-seconds of memory

#### 2. Cloud SQL (Managed PostgreSQL)

**db-f1-micro (Development):**
```
- CPU: Shared, 0.6GB RAM
- Storage: 10GB SSD
- Cost: $7.67/month + $0.17/GB storage = $9.37/month
```

**db-custom-2-7680 (Production):**
```
- CPU: 2 vCPUs
- Memory: 7.5GB RAM
- Storage: 20GB SSD
- Cost: $69.37/month + $0.17/GB × 20GB = $72.77/month
```

**High Availability (Multi-Zone):**
```
- Cost: 2× instance cost + egress
- db-custom-2-7680 HA: ~$145/month
```

#### 3. Cloud Storage (Object Storage)

**Standard Storage:**
```
Price per GB/month:
- First 1 TB: $0.020/GB
- Next 49 TB: $0.010/GB
- Over 50 TB: $0.007/GB

Example Usage:
- 10GB stored: $0.20/month
- 100GB stored: $2.00/month
- 1TB stored: $20.48/month

Operations:
- Class A (writes): $0.05/10,000 operations
- Class B (reads): $0.004/10,000 operations
```

#### 4. Secret Manager

**Pricing:**
```
- Active secret versions: $0.06/version/month
- Access operations: $0.03/10,000 accesses

Example (3 secrets, 100k accesses/month):
- Secret storage: 3 × $0.06 = $0.18/month
- Access: 100,000 × $0.03/10,000 = $0.30/month
- Total: ~$0.48/month
```

#### 5. VPC Connector

**Pricing:**
```
- f1-micro (2 instances minimum): $10.95/month
- e2-micro (2 instances minimum): $12.41/month
- e2-small (2 instances minimum): $24.82/month

Egress charges apply for data transfer
```

#### 6. Artifact Registry

**Pricing:**
```
- Storage: $0.10/GB/month
- Network egress: Standard rates

Example (10GB images):
- Storage: $1.00/month
- Egress within region: Free
```

#### 7. Cloud Build

**Free Tier:**
- 120 build-minutes/day

**Pricing (beyond free tier):**
```
- n1-standard-1: $0.003/build-minute
- e2-highcpu-8: $0.016/build-minute

Example (20 builds/month, 10 min each, e2-highcpu-8):
- 200 minutes × $0.016 = $3.20/month
```

### Monthly Cost Examples

#### Scenario 1: Development/Testing

```
Service                    Cost
────────────────────────────────
Cloud Run (Frontend)       $1.00
Cloud Run (Backend)        $1.45
Cloud SQL (db-f1-micro)    $9.37
Cloud Storage (10GB)       $0.20
Secret Manager             $0.48
VPC Connector              $12.41
Artifact Registry (5GB)    $0.50
────────────────────────────────
TOTAL                      $25.41/month
```

#### Scenario 2: Small Production

```
Service                      Cost
──────────────────────────────────
Cloud Run (Frontend)         $10.00
Cloud Run (Backend)          $29.00
Cloud SQL (db-custom-2-7680) $72.77
Cloud Storage (100GB)        $2.00
Secret Manager               $0.48
VPC Connector                $12.41
Artifact Registry (10GB)     $1.00
Cloud Build                  $3.20
──────────────────────────────────
TOTAL                        $130.86/month
```

#### Scenario 3: High-Traffic Production

```
Service                       Cost
───────────────────────────────────
Cloud Run (Frontend)          $50.00
Cloud Run (Backend)           $150.00
Cloud SQL (HA, custom-4-15360) $290.00
Cloud Storage (1TB)           $20.48
Cloud CDN                     $20.00
Secret Manager                $0.48
VPC Connector (e2-small)      $24.82
Artifact Registry (20GB)      $2.00
Cloud Build                   $10.00
Load Balancer                 $18.00
───────────────────────────────────
TOTAL                         $585.78/month
```

---

## Cost Optimization Strategies

### 1. Cloud Run Optimization

#### Scale to Zero
```bash
# Set minimum instances to 0 in Terraform
resource "google_cloud_run_v2_service" "service" {
  # ...
  template {
    scaling {
      min_instance_count = 0  # Scale to zero when idle
      max_instance_count = 10
    }
  }
}
```

**Savings:** Up to 90% during low-traffic periods

#### Right-size Resources
```bash
# Start conservative, monitor, and adjust
resource "google_cloud_run_v2_service" "backend" {
  template {
    containers {
      resources {
        limits = {
          cpu    = "1000m"    # Start with 1 vCPU
          memory = "512Mi"    # Start with 512MB
        }
      }
    }
  }
}

# Monitor actual usage:
gcloud monitoring read "resource.type=cloud_run_revision AND \
  metric.type=run.googleapis.com/container/cpu/utilization" \
  --format=json
```

#### Request Optimization
- Implement caching to reduce duplicate requests
- Use compression for API responses
- Optimize frontend bundle size

### 2. Cloud SQL Optimization

#### Choose Right Tier

| Tier | Use Case | Monthly Cost |
|------|----------|--------------|
| db-f1-micro | Dev/Test, <100 connections | $9.37 |
| db-g1-small | Small prod, <250 connections | $25 |
| db-custom-2-7680 | Medium prod, <400 connections | $72.77 |
| db-custom-4-15360 | Large prod, <800 connections | $147 |

#### Enable Automatic Storage Increase
```hcl
resource "google_sql_database_instance" "postgres" {
  settings {
    disk_autoresize = true
    disk_autoresize_limit = 100  # GB limit
  }
}
```

#### Optimize Connection Pooling
```properties
# application-gcp.properties
spring.datasource.hikari.maximum-pool-size=5  # Reduce from default 20
spring.datasource.hikari.minimum-idle=1       # Reduce from default 5
```

#### Schedule Backups During Off-Peak Hours
```hcl
resource "google_sql_database_instance" "postgres" {
  settings {
    backup_configuration {
      start_time = "03:00"  # 3 AM in your timezone
    }
  }
}
```

### 3. Cloud Storage Optimization

#### Lifecycle Policies
```bash
# Delete files older than 90 days
gsutil lifecycle set lifecycle.json gs://your-bucket-name

# lifecycle.json:
{
  "lifecycle": {
    "rule": [
      {
        "action": {"type": "Delete"},
        "condition": {
          "age": 90,
          "matchesPrefix": ["temp/", "cache/"]
        }
      },
      {
        "action": {"type": "SetStorageClass", "storageClass": "NEARLINE"},
        "condition": {
          "age": 30,
          "matchesStorageClass": ["STANDARD"]
        }
      }
    ]
  }
}
```

#### Storage Classes

| Class | Use Case | Cost/GB/month |
|-------|----------|---------------|
| Standard | Frequently accessed | $0.020 |
| Nearline | <1x/month access | $0.010 |
| Coldline | <1x/quarter access | $0.004 |
| Archive | <1x/year access | $0.0012 |

### 4. Network Optimization

#### Reduce Egress Costs
- Use Cloud CDN for static content
- Keep data transfers within same region
- Use compression for large files

#### VPC Connector Right-Sizing
```hcl
resource "google_vpc_access_connector" "connector" {
  machine_type  = "e2-micro"  # Cheapest option
  min_instances = 2           # Minimum required
  max_instances = 3           # Adjust based on load
}
```

### 5. Committed Use Discounts

Save 25-57% with 1 or 3-year commitments:

```bash
# Purchase commitment for Cloud SQL
gcloud compute commitments create my-commitment \
  --region=us-central1 \
  --resources=vcpu=2,memory=7680 \
  --plan=twelve-month

# Save 25% with 1-year commitment
# Save 52% with 3-year commitment
```

---

## Monitoring Setup

### Cloud Monitoring Workspace

1. **Create Monitoring Workspace**
   ```bash
   gcloud alpha monitoring workspaces create \
     --project=$PROJECT_ID \
     --display-name="OSCAL Tools Monitoring"
   ```

2. **Install Ops Agent (for VM monitoring, if needed)**
   ```bash
   curl -sSO https://dl.google.com/cloudagents/add-google-cloud-ops-agent-repo.sh
   sudo bash add-google-cloud-ops-agent-repo.sh --also-install
   ```

### Key Metrics to Monitor

#### Cloud Run Metrics

```bash
# Request count
gcloud monitoring read "resource.type=cloud_run_revision AND \
  metric.type=run.googleapis.com/request_count"

# Request latencies
gcloud monitoring read "resource.type=cloud_run_revision AND \
  metric.type=run.googleapis.com/request_latencies"

# Container CPU utilization
gcloud monitoring read "resource.type=cloud_run_revision AND \
  metric.type=run.googleapis.com/container/cpu/utilization"

# Container memory utilization
gcloud monitoring read "resource.type=cloud_run_revision AND \
  metric.type=run.googleapis.com/container/memory/utilization"

# Instance count
gcloud monitoring read "resource.type=cloud_run_revision AND \
  metric.type=run.googleapis.com/container/instance_count"
```

#### Cloud SQL Metrics

```bash
# CPU utilization
gcloud monitoring read "resource.type=cloudsql_database AND \
  metric.type=cloudsql.googleapis.com/database/cpu/utilization"

# Memory utilization
gcloud monitoring read "resource.type=cloudsql_database AND \
  metric.type=cloudsql.googleapis.com/database/memory/utilization"

# Active connections
gcloud monitoring read "resource.type=cloudsql_database AND \
  metric.type=cloudsql.googleapis.com/database/network/connections"

# Disk I/O
gcloud monitoring read "resource.type=cloudsql_database AND \
  metric.type=cloudsql.googleapis.com/database/disk/bytes_used"
```

### Custom Dashboards

Create a custom monitoring dashboard:

```bash
# Create dashboard JSON
cat > dashboard.json <<'EOF'
{
  "displayName": "OSCAL Tools Dashboard",
  "dashboardFilters": [],
  "mosaicLayout": {
    "columns": 12,
    "tiles": [
      {
        "width": 6,
        "height": 4,
        "widget": {
          "title": "Cloud Run Request Count",
          "xyChart": {
            "dataSets": [{
              "timeSeriesQuery": {
                "timeSeriesFilter": {
                  "filter": "resource.type=\"cloud_run_revision\" metric.type=\"run.googleapis.com/request_count\""
                }
              }
            }]
          }
        }
      },
      {
        "width": 6,
        "height": 4,
        "xPos": 6,
        "widget": {
          "title": "Cloud SQL CPU Utilization",
          "xyChart": {
            "dataSets": [{
              "timeSeriesQuery": {
                "timeSeriesFilter": {
                  "filter": "resource.type=\"cloudsql_database\" metric.type=\"cloudsql.googleapis.com/database/cpu/utilization\""
                }
              }
            }]
          }
        }
      }
    ]
  }
}
EOF

# Create dashboard
gcloud monitoring dashboards create --config-from-file=dashboard.json
```

---

## Alerting Configuration

### Alert Policies

#### 1. High Error Rate Alert

```bash
gcloud alpha monitoring policies create \
  --notification-channels=CHANNEL_ID \
  --display-name="OSCAL High Error Rate" \
  --condition-display-name="5xx Error Rate > 5%" \
  --condition-filter='resource.type="cloud_run_revision" AND metric.type="run.googleapis.com/request_count" AND metric.label.response_code_class="5xx"' \
  --condition-threshold-value=5 \
  --condition-threshold-duration=300s \
  --condition-comparison=COMPARISON_GT
```

#### 2. High Database CPU Alert

```bash
gcloud alpha monitoring policies create \
  --notification-channels=CHANNEL_ID \
  --display-name="OSCAL Database High CPU" \
  --condition-display-name="CPU Usage > 80%" \
  --condition-filter='resource.type="cloudsql_database" AND metric.type="cloudsql.googleapis.com/database/cpu/utilization"' \
  --condition-threshold-value=0.8 \
  --condition-threshold-duration=300s \
  --condition-comparison=COMPARISON_GT
```

#### 3. Budget Alert

```bash
gcloud billing budgets create \
  --billing-account=BILLING_ACCOUNT_ID \
  --display-name="OSCAL Tools Budget Alert" \
  --budget-amount=100 \
  --threshold-rule=percent=50 \
  --threshold-rule=percent=90 \
  --threshold-rule=percent=100 \
  --notification-pubsub-topic=projects/$PROJECT_ID/topics/budget-alerts
```

### Notification Channels

```bash
# Create email notification channel
gcloud alpha monitoring channels create \
  --display-name="DevOps Team Email" \
  --type=email \
  --channel-labels=email_address=devops@your-company.com

# Create Slack notification channel
gcloud alpha monitoring channels create \
  --display-name="DevOps Slack" \
  --type=slack \
  --channel-labels=url=https://hooks.slack.com/services/YOUR/WEBHOOK/URL

# Create PagerDuty notification channel
gcloud alpha monitoring channels create \
  --display-name="PagerDuty" \
  --type=pagerduty \
  --channel-labels=service_key=YOUR_PAGERDUTY_SERVICE_KEY
```

---

## Performance Optimization

### 1. Reduce Cold Start Time

**Optimize Docker Image Size:**
```dockerfile
# Multi-stage build to reduce image size
FROM maven:3.9-eclipse-temurin-21 AS builder
# ... build steps ...

FROM eclipse-temurin:21-jre-jammy  # Use JRE instead of JDK
# ... runtime setup ...
```

**Keep Services Warm:**
```hcl
resource "google_cloud_run_v2_service" "backend" {
  template {
    scaling {
      min_instance_count = 1  # Keep 1 instance always running
    }
  }
}
```

**Cost vs Performance Trade-off:**
- 0 min instances: $0/month baseline, slower cold starts
- 1 min instance: ~$15-30/month, no cold starts

### 2. Optimize Database Queries

**Enable Query Insights:**
```bash
gcloud sql instances patch INSTANCE_NAME \
  --insights-config-query-insights-enabled \
  --insights-config-query-plans-per-minute=5
```

**Add Database Indexes:**
```sql
-- Analyze slow queries
SELECT * FROM pg_stat_statements
ORDER BY mean_exec_time DESC
LIMIT 10;

-- Add indexes for frequently queried columns
CREATE INDEX idx_users_username ON users(username);
CREATE INDEX idx_history_timestamp ON operation_history(timestamp DESC);
```

### 3. Enable Cloud CDN

```bash
# Create load balancer and enable CDN
gcloud compute backend-services create oscal-backend-cdn \
  --protocol=HTTPS \
  --global \
  --enable-cdn

gcloud compute backend-services add-backend oscal-backend-cdn \
  --backend-service-tier=PREMIUM \
  --balancing-mode=RATE \
  --max-rate-per-instance=100

# Update frontend to use CDN
gcloud compute url-maps create oscal-url-map \
  --default-service=oscal-backend-cdn
```

---

## Budget Management

### Set Up Budget Alerts

1. **Navigate to Billing → Budgets & Alerts**
2. **Create Budget:**
   - Name: "OSCAL Tools Monthly Budget"
   - Projects: Select your project
   - Budget Amount: $200/month
   - Alert Thresholds: 50%, 90%, 100%

3. **Configure Actions:**
   - Send email to devops team
   - Publish to Pub/Sub topic
   - Trigger Cloud Function to scale down resources (optional)

### Cost Anomaly Detection

Enable automatic anomaly detection:

```bash
# This is done in the GCP Console under Billing → Cost Management
# It will automatically alert on unusual spending patterns
```

### Resource Quotas

Set quotas to prevent runaway costs:

```bash
# Set max Cloud Run instances
gcloud run services update oscal-backend-prod \
  --max-instances=10 \
  --region=us-central1

# Set max Cloud SQL storage
gcloud sql instances patch INSTANCE_NAME \
  --storage-auto-resize-limit=100
```

---

## Cost Reporting

### Export Billing Data to BigQuery

```bash
# Enable billing export
gcloud beta billing accounts set-iam-policy BILLING_ACCOUNT_ID \
  policy.yaml

# Configure BigQuery dataset
bq mk --dataset --location=US $PROJECT_ID:billing_export
```

### Create Cost Reports

```sql
-- BigQuery query for monthly cost by service
SELECT
  service.description AS service_name,
  SUM(cost) AS total_cost,
  EXTRACT(MONTH FROM usage_start_time) AS month
FROM `project.billing_export.gcp_billing_export_*`
WHERE _TABLE_SUFFIX BETWEEN '20250101' AND '20250131'
GROUP BY service_name, month
ORDER BY total_cost DESC;
```

---

## Summary

### Quick Reference: Monthly Cost Targets

| Environment | Target Cost | Services |
|-------------|-------------|----------|
| Development | $20-40 | Min specs, scale to zero |
| Staging | $60-100 | Moderate specs, minimal redundancy |
| Production (Small) | $130-200 | Right-sized, HA database |
| Production (Large) | $500-1000 | High specs, CDN, HA, multi-region |

### Monitoring Checklist

- ✅ Cloud Run request latency < 500ms p95
- ✅ Cloud SQL CPU utilization < 70%
- ✅ Cloud SQL connections < 80% of max
- ✅ 5xx error rate < 0.1%
- ✅ Cloud Storage costs tracked monthly
- ✅ Budget alerts configured
- ✅ Custom dashboards created
- ✅ Alert policies tested

---

**Last Updated:** 2025-01-29
**Version:** 1.0.0
