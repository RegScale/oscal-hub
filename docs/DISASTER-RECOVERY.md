# OSCAL Hub Disaster Recovery Plan

**Date**: February 2025
**Status**: Active
**Classification**: Internal Use
**Review Frequency**: Quarterly
**Last DR Test**: [Update after each test]

## Overview

This document defines the disaster recovery (DR) procedures for OSCAL Hub deployed on Google Cloud Platform. It provides recovery objectives, backup strategies, and step-by-step recovery procedures for various disaster scenarios.

## Table of Contents

- [Recovery Objectives](#recovery-objectives)
- [Architecture Overview](#architecture-overview)
- [Backup Strategy](#backup-strategy)
- [Disaster Scenarios](#disaster-scenarios)
- [Recovery Procedures](#recovery-procedures)
- [DR Testing](#dr-testing)
- [Roles and Responsibilities](#roles-and-responsibilities)

---

## Recovery Objectives

### Recovery Time Objective (RTO)

| Component | RTO | Description |
|-----------|-----|-------------|
| **Web Application** | 15 minutes | Cloud Run auto-recovery |
| **API Backend** | 15 minutes | Cloud Run auto-recovery |
| **Database** | 1 hour | Point-in-time recovery |
| **File Storage** | 30 minutes | Cloud Storage versioning |
| **Full Platform** | 2 hours | Complete restoration |

### Recovery Point Objective (RPO)

| Component | RPO | Backup Frequency |
|-----------|-----|------------------|
| **Database** | 5 minutes | Continuous (point-in-time) |
| **User Files** | 0 minutes | Real-time versioning |
| **Configuration** | 24 hours | Daily + on change |
| **Audit Logs** | 0 minutes | Real-time replication |

---

## Architecture Overview

### Current GCP Infrastructure

```
┌─────────────────────────────────────────────────────────────────┐
│                        Google Cloud Platform                      │
├─────────────────────────────────────────────────────────────────┤
│                                                                   │
│   ┌─────────────┐    ┌─────────────┐    ┌─────────────┐        │
│   │  Cloud Run  │    │  Cloud Run  │    │   Cloud     │        │
│   │  Frontend   │◄──►│   Backend   │◄──►│    SQL      │        │
│   │  (Next.js)  │    │ (Spring Boot)│    │ (PostgreSQL)│        │
│   └─────────────┘    └─────────────┘    └─────────────┘        │
│         │                   │                   │                │
│         │                   ▼                   │                │
│         │           ┌─────────────┐             │                │
│         │           │   Cloud     │             │                │
│         │           │  Storage    │             │                │
│         │           │  (Files)    │             │                │
│         │           └─────────────┘             │                │
│         │                                       │                │
│         ▼                                       ▼                │
│   ┌─────────────────────────────────────────────────┐          │
│   │              Artifact Registry                   │          │
│   │           (Container Images)                     │          │
│   └─────────────────────────────────────────────────┘          │
│                                                                   │
└─────────────────────────────────────────────────────────────────┘
```

### Critical Components

| Component | Service | Region | Backup Strategy |
|-----------|---------|--------|-----------------|
| Frontend | Cloud Run | us-central1 | Container image versioning |
| Backend | Cloud Run | us-central1 | Container image versioning |
| Database | Cloud SQL | us-central1 | Automated + PITR |
| Storage | Cloud Storage | us-central1 | Object versioning |
| Images | Artifact Registry | us-central1 | Keep last 5 versions |
| Secrets | Secret Manager | us-central1 | Version history |

---

## Backup Strategy

### Database Backups (Cloud SQL)

#### Automated Backups

```hcl
# Terraform configuration (current)
backup_configuration {
  enabled                        = true
  start_time                     = "03:00"  # UTC
  point_in_time_recovery_enabled = true
  transaction_log_retention_days = 7
  backup_retention_settings {
    retained_backups = 7
  }
}
```

**Backup Details:**
- **Frequency**: Daily at 3:00 AM UTC
- **Retention**: 7 days
- **Type**: Full backup + transaction logs
- **Point-in-Time Recovery**: Enabled (5-minute granularity)
- **Location**: Same region as primary instance

#### Manual Backup Commands

```bash
# Create on-demand backup
gcloud sql backups create --instance=oscal-db-prod \
  --description="Pre-deployment backup $(date +%Y-%m-%d)"

# List all backups
gcloud sql backups list --instance=oscal-db-prod

# View backup details
gcloud sql backups describe BACKUP_ID --instance=oscal-db-prod
```

### Cloud Storage Backups

#### Object Versioning

```bash
# Verify versioning is enabled
gsutil versioning get gs://oscal-tools-build-prod-us-central1

# List object versions
gsutil ls -la gs://oscal-tools-build-prod-us-central1/

# Lifecycle policy (keeps 5 versions)
gsutil lifecycle get gs://oscal-tools-build-prod-us-central1
```

### Container Image Retention

```bash
# List images with versions
gcloud artifacts docker images list \
  us-central1-docker.pkg.dev/PROJECT_ID/oscal-tools/oscal-backend

# Current retention: 5 versions per tag
# Cleanup runs after each successful deployment
```

### Configuration Backups

**Backup these items regularly:**

1. **Terraform State**
   ```bash
   # Export current state
   terraform state pull > terraform_state_backup_$(date +%Y%m%d).json
   ```

2. **Application Properties**
   ```bash
   # Already in version control
   git archive --format=tar HEAD back-end/src/main/resources/ > config_backup.tar
   ```

3. **Secrets (metadata only)**
   ```bash
   gcloud secrets list --format="table(name,createTime,replication)"
   ```

---

## Disaster Scenarios

### Scenario 1: Cloud Run Service Failure

**Impact**: Application unavailable
**RTO**: 15 minutes
**Automatic Recovery**: Yes

Cloud Run automatically:
- Restarts failed containers
- Scales based on traffic
- Routes around unhealthy instances

**Manual Recovery** (if automatic fails):
```bash
# Redeploy current version
gcloud run services update oscal-backend-prod \
  --region=us-central1 \
  --image=us-central1-docker.pkg.dev/PROJECT/oscal-tools/oscal-backend:latest

# Or rollback to previous version
gcloud run services update-traffic oscal-backend-prod \
  --region=us-central1 \
  --to-revisions=oscal-backend-prod-00xxx-xxx=100
```

### Scenario 2: Database Corruption or Deletion

**Impact**: Data loss, application errors
**RTO**: 1 hour
**RPO**: 5 minutes (with PITR)

**Recovery Steps**:
```bash
# Option 1: Point-in-time recovery
gcloud sql instances clone oscal-db-prod oscal-db-recovered \
  --point-in-time="2025-02-16T10:00:00.000Z"

# Option 2: Restore from backup
gcloud sql backups restore BACKUP_ID \
  --restore-instance=oscal-db-prod

# Update application to use recovered database
gcloud run services update oscal-backend-prod \
  --update-env-vars="DB_HOST=oscal-db-recovered-ip"
```

### Scenario 3: Accidental Data Deletion

**Impact**: User data loss
**RTO**: 30 minutes
**RPO**: 0 (with versioning)

**For Cloud Storage**:
```bash
# List deleted versions
gsutil ls -la gs://BUCKET/path/to/deleted/file

# Restore specific version
gsutil cp gs://BUCKET/path/to/file#VERSION gs://BUCKET/path/to/file
```

**For Database**:
```bash
# Point-in-time recovery to just before deletion
gcloud sql instances clone oscal-db-prod oscal-db-recovered \
  --point-in-time="TIMESTAMP_BEFORE_DELETION"

# Export specific tables from recovered instance
pg_dump -h RECOVERED_IP -U oscal_user -t affected_table oscal_production > recovery.sql

# Import to production
psql -h PROD_IP -U oscal_user -d oscal_production < recovery.sql
```

### Scenario 4: Region-Wide Outage

**Impact**: Complete service unavailability
**RTO**: 2-4 hours
**Current Mitigation**: Single region (us-central1)

**Recovery Steps**:

1. **Assess Outage Duration**
   - Check GCP Status: https://status.cloud.google.com/
   - If > 2 hours expected, proceed with DR

2. **Deploy to Alternate Region**
   ```bash
   # Update Terraform variables
   export TF_VAR_region=us-east1

   # Deploy infrastructure
   cd terraform/gcp
   terraform apply -var="region=us-east1"

   # Restore database from backup
   gcloud sql instances clone oscal-db-prod oscal-db-dr \
     --region=us-east1

   # Deploy applications
   gcloud run deploy oscal-backend-prod \
     --region=us-east1 \
     --image=us-central1-docker.pkg.dev/PROJECT/oscal-tools/oscal-backend:latest
   ```

3. **Update DNS** (if using custom domain)
   ```bash
   # Point to new region endpoints
   gcloud dns record-sets update oscal.example.com \
     --zone=example-zone \
     --type=A \
     --rrdatas=NEW_REGION_IP
   ```

### Scenario 5: Security Breach / Ransomware

**Impact**: Data compromise, potential data loss
**RTO**: 4-24 hours (depends on scope)

**Immediate Actions**:
1. **Isolate Affected Systems**
   ```bash
   # Stop all Cloud Run services
   gcloud run services update oscal-backend-prod --no-traffic
   gcloud run services update oscal-frontend-prod --no-traffic
   ```

2. **Preserve Evidence**
   ```bash
   # Export all logs
   gcloud logging read "timestamp>=\"$(date -d '7 days ago' -Iseconds)\"" \
     --format=json > incident_logs.json

   # Create database snapshot
   gcloud sql backups create --instance=oscal-db-prod \
     --description="Incident preservation $(date)"
   ```

3. **Restore from Clean Backup**
   ```bash
   # Identify last known good backup
   gcloud sql backups list --instance=oscal-db-prod

   # Restore to new instance
   gcloud sql backups restore BACKUP_ID \
     --restore-instance=oscal-db-clean \
     --backup-instance=oscal-db-prod
   ```

4. **Redeploy from Known Good Images**
   ```bash
   # Deploy verified clean image
   gcloud run deploy oscal-backend-prod \
     --image=us-central1-docker.pkg.dev/PROJECT/oscal-tools/oscal-backend:VERIFIED_SHA
   ```

---

## Recovery Procedures

### Complete Platform Recovery Procedure

Follow this procedure for full platform restoration:

#### Phase 1: Infrastructure (30 minutes)

```bash
# 1. Verify GCP project access
gcloud config set project $PROJECT_ID

# 2. Check or create service account
gcloud iam service-accounts list

# 3. Enable required APIs
gcloud services enable \
  run.googleapis.com \
  sql-component.googleapis.com \
  storage.googleapis.com \
  artifactregistry.googleapis.com

# 4. Deploy infrastructure with Terraform
cd terraform/gcp
terraform init
terraform plan -var-file="prod.tfvars"
terraform apply -var-file="prod.tfvars"
```

#### Phase 2: Database Recovery (30-60 minutes)

```bash
# 1. List available backups
gcloud sql backups list --instance=oscal-db-prod

# 2. Restore database
# Option A: Restore to existing instance
gcloud sql backups restore BACKUP_ID \
  --restore-instance=oscal-db-prod

# Option B: Create new instance from backup
gcloud sql instances clone oscal-db-prod oscal-db-recovered \
  --point-in-time="TIMESTAMP"

# 3. Verify database connectivity
gcloud sql connect oscal-db-prod --user=oscal_user

# 4. Verify data integrity
psql -c "SELECT COUNT(*) FROM users;"
psql -c "SELECT COUNT(*) FROM library_items;"
psql -c "SELECT COUNT(*) FROM audit_logs;"
```

#### Phase 3: Application Deployment (15-30 minutes)

```bash
# 1. Verify container images exist
gcloud artifacts docker images list \
  us-central1-docker.pkg.dev/$PROJECT_ID/oscal-tools

# 2. Deploy backend
gcloud run deploy oscal-backend-prod \
  --image=us-central1-docker.pkg.dev/$PROJECT_ID/oscal-tools/oscal-backend:latest \
  --region=us-central1 \
  --platform=managed \
  --allow-unauthenticated \
  --set-env-vars="SPRING_PROFILES_ACTIVE=gcp"

# 3. Deploy frontend
gcloud run deploy oscal-frontend-prod \
  --image=us-central1-docker.pkg.dev/$PROJECT_ID/oscal-tools/oscal-frontend:latest \
  --region=us-central1 \
  --platform=managed \
  --allow-unauthenticated

# 4. Configure secrets
gcloud run services update oscal-backend-prod \
  --update-secrets=JWT_SECRET=jwt-secret:latest,DB_PASSWORD=db-password:latest
```

#### Phase 4: Verification (15 minutes)

```bash
# 1. Health check
BACKEND_URL=$(gcloud run services describe oscal-backend-prod \
  --region=us-central1 --format='value(status.url)')

curl $BACKEND_URL/api/health

# 2. Detailed health check
curl $BACKEND_URL/api/health/detailed

# 3. Test authentication
curl -X POST $BACKEND_URL/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"test","password":"test"}'

# 4. Test core functionality
curl -X POST $BACKEND_URL/api/validate \
  -H "Content-Type: application/json" \
  -d '{"content":"<catalog>...</catalog>","format":"XML"}'

# 5. Verify audit logging
curl $BACKEND_URL/api/admin/logs -H "Authorization: Bearer $TOKEN"
```

### Database Point-in-Time Recovery

```bash
#!/bin/bash
# Script: database_pitr.sh

# Configuration
INSTANCE_NAME="oscal-db-prod"
RECOVERY_INSTANCE="oscal-db-recovered"
PROJECT_ID="your-project-id"

# Get the point in time (ISO 8601 format)
read -p "Enter recovery point (YYYY-MM-DDTHH:MM:SS.000Z): " RECOVERY_TIME

echo "Creating clone from $INSTANCE_NAME at $RECOVERY_TIME..."

# Clone with point-in-time recovery
gcloud sql instances clone $INSTANCE_NAME $RECOVERY_INSTANCE \
  --point-in-time="$RECOVERY_TIME" \
  --project=$PROJECT_ID

echo "Waiting for clone to complete..."
gcloud sql operations list --instance=$RECOVERY_INSTANCE --limit=1

# Get new instance IP
NEW_IP=$(gcloud sql instances describe $RECOVERY_INSTANCE \
  --format='value(ipAddresses[0].ipAddress)')

echo "Recovery instance created at IP: $NEW_IP"
echo "To switch production, update Cloud Run environment variables."
```

---

## DR Testing

### Test Schedule

| Test Type | Frequency | Duration | Last Performed |
|-----------|-----------|----------|----------------|
| Backup Verification | Weekly | 30 min | [Date] |
| Database Recovery | Monthly | 2 hours | [Date] |
| Full DR Simulation | Quarterly | 4 hours | [Date] |
| Region Failover | Annually | 8 hours | [Date] |

### Test Procedures

#### Weekly: Backup Verification

```bash
#!/bin/bash
# Test: Verify backups are completing successfully

echo "=== Weekly Backup Verification ==="

# Check database backups
echo "Database backups:"
gcloud sql backups list --instance=oscal-db-prod --limit=7

# Verify latest backup
LATEST_BACKUP=$(gcloud sql backups list --instance=oscal-db-prod \
  --format='value(id)' --limit=1)
gcloud sql backups describe $LATEST_BACKUP --instance=oscal-db-prod

# Check Cloud Storage versioning
echo "Storage versioning status:"
gsutil versioning get gs://oscal-tools-build-prod-us-central1

# Check container images
echo "Container images:"
gcloud artifacts docker images list \
  us-central1-docker.pkg.dev/$PROJECT_ID/oscal-tools --limit=5

echo "=== Backup Verification Complete ==="
```

#### Monthly: Database Recovery Test

```bash
#!/bin/bash
# Test: Database recovery to test instance

echo "=== Monthly Database Recovery Test ==="

TEST_INSTANCE="oscal-db-test-recovery"
BACKUP_ID=$(gcloud sql backups list --instance=oscal-db-prod \
  --format='value(id)' --limit=1)

# Restore to test instance
echo "Restoring backup $BACKUP_ID to $TEST_INSTANCE..."
gcloud sql instances clone oscal-db-prod $TEST_INSTANCE

# Wait for restoration
sleep 120

# Verify data
echo "Verifying restored data..."
gcloud sql connect $TEST_INSTANCE --user=oscal_user << EOF
SELECT 'Users:', COUNT(*) FROM users;
SELECT 'Library Items:', COUNT(*) FROM library_items;
SELECT 'Authorizations:', COUNT(*) FROM authorizations;
EOF

# Cleanup test instance
echo "Cleaning up test instance..."
gcloud sql instances delete $TEST_INSTANCE --quiet

echo "=== Database Recovery Test Complete ==="
```

#### Quarterly: Full DR Simulation

**Checklist:**

- [ ] Notify stakeholders of planned test
- [ ] Document current state (instance counts, data counts)
- [ ] Simulate primary region failure
- [ ] Execute recovery procedures
- [ ] Verify all functionality
- [ ] Document recovery time
- [ ] Identify improvements
- [ ] Update documentation
- [ ] Report results to management

### Test Documentation Template

```markdown
# DR Test Report

**Test Date**: YYYY-MM-DD
**Test Type**: [Backup Verification | Recovery Test | Full Simulation]
**Participants**: [Names]

## Objectives
- [List test objectives]

## Pre-Test State
- Database records: X
- Storage objects: X
- Active users: X

## Test Execution
| Step | Expected | Actual | Status |
|------|----------|--------|--------|
| 1 | ... | ... | Pass/Fail |

## Metrics
- Recovery Time: X minutes
- Data Loss: X records / None
- Downtime: X minutes

## Issues Identified
1. [Issue description and resolution]

## Recommendations
1. [Improvement suggestions]

## Sign-off
- Tested by: [Name]
- Reviewed by: [Name]
```

---

## Roles and Responsibilities

### DR Team

| Role | Primary | Backup | Responsibilities |
|------|---------|--------|------------------|
| DR Coordinator | [Name] | [Name] | Overall DR management |
| Database Admin | [Name] | [Name] | Database recovery |
| Cloud Engineer | [Name] | [Name] | Infrastructure recovery |
| Application Lead | [Name] | [Name] | Application deployment |
| QA Lead | [Name] | [Name] | Recovery verification |

### Contact Information

Maintain emergency contact list separately in secure storage.

### Escalation Path

```
Level 1: On-call Engineer (15 min response)
    ↓
Level 2: Team Lead (30 min response)
    ↓
Level 3: Platform Owner (1 hour response)
    ↓
Level 4: Executive Sponsor (2 hour response)
```

---

## Appendix

### Quick Reference Commands

```bash
# Database backup
gcloud sql backups create --instance=oscal-db-prod

# Database restore
gcloud sql backups restore BACKUP_ID --restore-instance=oscal-db-prod

# Point-in-time recovery
gcloud sql instances clone SOURCE DEST --point-in-time="TIMESTAMP"

# Service rollback
gcloud run services update-traffic SERVICE --to-revisions=REVISION=100

# Export logs
gcloud logging read "FILTER" --format=json > logs.json
```

### Important URLs

- GCP Console: https://console.cloud.google.com/
- Cloud Run: https://console.cloud.google.com/run
- Cloud SQL: https://console.cloud.google.com/sql
- Cloud Storage: https://console.cloud.google.com/storage
- Monitoring: https://console.cloud.google.com/monitoring
- GCP Status: https://status.cloud.google.com/

### Recovery Checklist

```
□ Assess situation and determine scope
□ Notify DR team and stakeholders
□ Preserve evidence (if security incident)
□ Execute appropriate recovery procedure
□ Verify infrastructure restored
□ Verify database restored
□ Verify applications deployed
□ Run health checks
□ Test core functionality
□ Notify users of restoration
□ Document incident and recovery
□ Conduct post-mortem
```

---

## Document Control

| Version | Date | Author | Changes |
|---------|------|--------|---------|
| 1.0 | Feb 2025 | Platform Team | Initial release |

**Next Review Date**: May 2025

---

*This document should be reviewed quarterly and tested according to the schedule defined above. Update immediately after any infrastructure changes.*
