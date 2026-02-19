# OSCAL Hub Incident Response Plan

**Date**: February 2025
**Status**: Active
**Classification**: Internal Use
**Review Frequency**: Quarterly

## Overview

This document defines the incident response procedures for OSCAL Hub deployed on Google Cloud Platform. It provides a structured approach to detecting, responding to, containing, and recovering from security incidents.

## Table of Contents

- [Incident Classification](#incident-classification)
- [Incident Response Team](#incident-response-team)
- [Detection and Alerting](#detection-and-alerting)
- [Response Procedures](#response-procedures)
- [Communication Plan](#communication-plan)
- [Post-Incident Activities](#post-incident-activities)
- [Runbooks](#runbooks)

---

## Incident Classification

### Severity Levels

| Severity | Description | Response Time | Examples |
|----------|-------------|---------------|----------|
| **P1 - Critical** | Service unavailable or major security breach | 15 minutes | Data breach, complete outage, ransomware |
| **P2 - High** | Significant degradation or potential security issue | 1 hour | Partial outage, suspicious access patterns |
| **P3 - Medium** | Limited impact, workaround available | 4 hours | Single component failure, failed login spikes |
| **P4 - Low** | Minimal impact, informational | 24 hours | Minor errors, configuration warnings |

### Incident Categories

1. **Security Incidents**
   - Unauthorized access attempts
   - Data breach or exfiltration
   - Malware or suspicious files
   - Account compromise
   - DDoS attacks

2. **Availability Incidents**
   - Service outage (Cloud Run)
   - Database unavailability (Cloud SQL)
   - Storage failures (Cloud Storage)
   - Network connectivity issues

3. **Performance Incidents**
   - High latency (>2 second response times)
   - Resource exhaustion (CPU, memory)
   - Connection pool exhaustion
   - Rate limit threshold breaches

4. **Data Incidents**
   - Data corruption
   - Accidental deletion
   - Backup failures
   - Replication issues

---

## Incident Response Team

### Roles and Responsibilities

| Role | Responsibilities | Escalation Path |
|------|------------------|-----------------|
| **Incident Commander** | Overall incident management, decision authority | Platform Owner |
| **Technical Lead** | Technical investigation and remediation | Incident Commander |
| **Communications Lead** | Internal/external communications | Incident Commander |
| **Security Analyst** | Security-specific investigation | Technical Lead |
| **Operations Engineer** | Infrastructure and system remediation | Technical Lead |

### Contact Information

Maintain an up-to-date contact list in your organization's secure documentation system. Include:
- Primary and secondary contacts for each role
- Phone numbers and email addresses
- Escalation procedures for after-hours incidents

---

## Detection and Alerting

### Monitoring Sources

#### 1. Application Audit Logs

OSCAL Hub captures 27 audit event types with automatic SIEM integration:

```
Location: /admin/logs (Web UI) or SIEM webhook
Event Types: LOGIN_SUCCESS, LOGIN_FAILURE, ACCOUNT_LOCKOUT,
             DOCUMENT_UPLOAD, DOCUMENT_DELETE, AUTHORIZATION_CREATED,
             ADMIN_USER_CREATED, ADMIN_USER_DISABLED, etc.
```

**Key Security Events to Monitor:**
- `LOGIN_FAILURE` - Multiple failures may indicate brute force
- `ACCOUNT_LOCKOUT` - Triggered after 5 failed attempts
- `ADMIN_USER_DISABLED` - Privileged account changes
- `DOCUMENT_DELETE` - Data destruction events
- `CONFIGURATION_CHANGE` - System configuration modifications

#### 2. Cloud Run Metrics

```bash
# View Cloud Run metrics
gcloud monitoring dashboards list --project=$PROJECT_ID

# Key metrics:
# - request_count (by response code)
# - request_latencies (p50, p95, p99)
# - container/cpu/utilization
# - container/memory/utilization
# - instance_count
```

#### 3. Cloud SQL Monitoring

```bash
# Database health
gcloud sql instances describe oscal-db-prod --project=$PROJECT_ID

# Key metrics:
# - cpu/utilization
# - memory/utilization
# - disk/utilization
# - network/connections
# - database/postgresql/num_backends
```

#### 4. Health Check Endpoints

```bash
# Application health
curl https://oscal-backend-prod-xxx.run.app/api/health

# Detailed health (requires auth)
curl -H "Authorization: Bearer $TOKEN" \
  https://oscal-backend-prod-xxx.run.app/api/health/detailed

# Component health
curl -H "Authorization: Bearer $TOKEN" \
  https://oscal-backend-prod-xxx.run.app/api/health/component/database
```

### Alert Configuration

#### Recommended Cloud Monitoring Alerts

1. **High Error Rate**
   ```yaml
   condition: response_code >= 500
   threshold: > 5% of requests in 5 minutes
   severity: P2
   ```

2. **Service Unavailable**
   ```yaml
   condition: instance_count == 0 AND request_count > 0
   threshold: Duration > 2 minutes
   severity: P1
   ```

3. **High Latency**
   ```yaml
   condition: request_latencies p99
   threshold: > 5000ms for 10 minutes
   severity: P3
   ```

4. **Database Connection Failures**
   ```yaml
   condition: cloudsql.googleapis.com/database/network/connections
   threshold: sudden drop > 50%
   severity: P2
   ```

5. **Failed Login Spike**
   ```yaml
   condition: audit_event_type == "LOGIN_FAILURE"
   threshold: > 50 events in 5 minutes
   severity: P2
   ```

6. **Account Lockout**
   ```yaml
   condition: audit_event_type == "ACCOUNT_LOCKOUT"
   threshold: any occurrence
   severity: P3
   ```

---

## Response Procedures

### Phase 1: Detection and Triage (0-15 minutes)

1. **Acknowledge the Incident**
   - Acknowledge alert in monitoring system
   - Create incident ticket with timestamp
   - Assign initial severity level

2. **Initial Assessment**
   ```bash
   # Check service status
   gcloud run services describe oscal-backend-prod --region=us-central1

   # Check recent logs
   gcloud logging read "resource.type=cloud_run_revision AND \
     resource.labels.service_name=oscal-backend-prod" \
     --limit=100 --format=json

   # Check database status
   gcloud sql instances describe oscal-db-prod
   ```

3. **Determine Scope**
   - How many users are affected?
   - Which components are impacted?
   - Is data at risk?
   - Is this a security incident?

### Phase 2: Containment (15-60 minutes)

#### For Security Incidents

1. **Isolate Compromised Accounts**
   ```sql
   -- Disable compromised user account
   UPDATE users SET enabled = false WHERE username = 'compromised_user';

   -- Revoke all active sessions (if session table exists)
   DELETE FROM user_sessions WHERE user_id = (SELECT id FROM users WHERE username = 'compromised_user');
   ```

2. **Block Suspicious IPs**
   ```bash
   # Add to Cloud Armor policy (if configured)
   gcloud compute security-policies rules create 1000 \
     --security-policy=oscal-policy \
     --src-ip-ranges="SUSPICIOUS_IP/32" \
     --action=deny-403
   ```

3. **Preserve Evidence**
   ```bash
   # Export audit logs
   gcloud logging read "resource.type=cloud_run_revision AND \
     timestamp>=\"2025-02-16T00:00:00Z\"" \
     --format=json > incident_logs_$(date +%Y%m%d).json

   # Export database audit trail
   pg_dump -h $DB_HOST -U $DB_USER -t audit_logs oscal_production > audit_backup.sql
   ```

#### For Availability Incidents

1. **Restart Service**
   ```bash
   # Force new revision deployment
   gcloud run services update oscal-backend-prod \
     --region=us-central1 \
     --update-env-vars="RESTART_TIMESTAMP=$(date +%s)"
   ```

2. **Scale Up Resources**
   ```bash
   # Increase min instances
   gcloud run services update oscal-backend-prod \
     --region=us-central1 \
     --min-instances=2 \
     --max-instances=20
   ```

3. **Database Recovery**
   ```bash
   # Check Cloud SQL status
   gcloud sql instances describe oscal-db-prod

   # Restart if needed
   gcloud sql instances restart oscal-db-prod

   # Point-in-time recovery (if data corruption)
   gcloud sql instances clone oscal-db-prod oscal-db-recovered \
     --point-in-time="2025-02-16T10:00:00Z"
   ```

### Phase 3: Eradication (1-4 hours)

1. **Remove Threat**
   - Delete malicious files from Cloud Storage
   - Remove unauthorized user accounts
   - Patch vulnerabilities

2. **Reset Credentials**
   ```bash
   # Rotate database password
   gcloud sql users set-password oscal_user \
     --instance=oscal-db-prod \
     --password="NEW_SECURE_PASSWORD"

   # Update Cloud Run with new credentials
   gcloud run services update oscal-backend-prod \
     --update-secrets=DB_PASSWORD=db-password:latest
   ```

3. **Update Security Controls**
   - Apply security patches
   - Update rate limiting rules
   - Strengthen authentication

### Phase 4: Recovery (4-24 hours)

1. **Restore Services**
   ```bash
   # Deploy known-good version
   gcloud run services update oscal-backend-prod \
     --image=us-central1-docker.pkg.dev/PROJECT/oscal-tools/oscal-backend:KNOWN_GOOD_SHA
   ```

2. **Verify Functionality**
   ```bash
   # Run health checks
   curl https://oscal-backend-prod-xxx.run.app/api/health

   # Test critical functions
   curl -X POST https://oscal-backend-prod-xxx.run.app/api/validate \
     -H "Content-Type: application/json" \
     -d '{"content":"...", "format":"JSON"}'
   ```

3. **Monitor for Recurrence**
   - Increase monitoring sensitivity
   - Watch for similar attack patterns
   - Verify containment effectiveness

---

## Communication Plan

### Internal Communications

| Severity | Notify | Method | Timing |
|----------|--------|--------|--------|
| P1 | All stakeholders | Phone + Email | Immediately |
| P2 | Technical team + Management | Slack + Email | Within 30 minutes |
| P3 | Technical team | Slack | Within 2 hours |
| P4 | On-call engineer | Ticket | Next business day |

### External Communications (if required)

1. **Customers**: Use approved templates, coordinate with legal
2. **Regulators**: Follow compliance reporting requirements
3. **Public**: Coordinate with communications team

### Status Update Template

```
INCIDENT STATUS UPDATE
======================
Incident ID: INC-2025-XXX
Severity: P2
Status: Investigating | Mitigating | Resolved

Summary:
[Brief description of the issue]

Impact:
[Who/what is affected]

Current Actions:
[What is being done]

Next Update:
[Time of next update]

Contact:
[Incident Commander contact]
```

---

## Post-Incident Activities

### Immediate (Within 24 hours)

1. **Document Timeline**
   - Detection time
   - Response actions and timestamps
   - Resolution time

2. **Collect Evidence**
   - Log exports
   - Screenshots
   - Configuration snapshots

### Within 1 Week

1. **Conduct Post-Mortem**
   - What happened?
   - Why did it happen?
   - What was the impact?
   - What went well in the response?
   - What could be improved?

2. **Identify Action Items**
   - Preventive measures
   - Detection improvements
   - Response procedure updates

### Post-Mortem Template

```markdown
# Incident Post-Mortem: INC-2025-XXX

## Summary
Brief description of the incident

## Timeline
| Time (UTC) | Event |
|------------|-------|
| HH:MM | Alert triggered |
| HH:MM | Incident acknowledged |
| HH:MM | Root cause identified |
| HH:MM | Mitigation applied |
| HH:MM | Service restored |

## Root Cause
Detailed explanation of what caused the incident

## Impact
- Users affected: X
- Duration: X hours
- Data impact: None / Limited / Significant

## Resolution
How the incident was resolved

## Lessons Learned
- What went well
- What could be improved

## Action Items
| Action | Owner | Due Date | Status |
|--------|-------|----------|--------|
| Improve monitoring | Team A | Date | Open |
```

---

## Runbooks

### Runbook 1: Service Unavailable

```bash
#!/bin/bash
# Runbook: Service Unavailable

echo "=== Service Unavailable Response ==="

# 1. Check service status
echo "Checking Cloud Run service..."
gcloud run services describe oscal-backend-prod --region=us-central1

# 2. Check recent logs
echo "Fetching recent logs..."
gcloud logging read "resource.type=cloud_run_revision AND \
  resource.labels.service_name=oscal-backend-prod AND \
  severity>=ERROR" --limit=50

# 3. Check database connectivity
echo "Checking database..."
gcloud sql instances describe oscal-db-prod

# 4. Check instance count
echo "Checking instances..."
gcloud run services describe oscal-backend-prod \
  --region=us-central1 --format="value(status.traffic)"

# 5. Force restart if needed
echo "To restart service, run:"
echo "gcloud run services update oscal-backend-prod --region=us-central1 --update-env-vars=RESTART=\$(date +%s)"
```

### Runbook 2: Suspected Account Compromise

```bash
#!/bin/bash
# Runbook: Account Compromise Response

USERNAME=$1
if [ -z "$USERNAME" ]; then
  echo "Usage: $0 <username>"
  exit 1
fi

echo "=== Account Compromise Response for $USERNAME ==="

# 1. Disable account immediately
echo "Disabling account..."
# Via admin API or direct database access
curl -X POST "https://oscal-backend-prod-xxx.run.app/api/admin/users/$USERNAME/disable" \
  -H "Authorization: Bearer $ADMIN_TOKEN"

# 2. Export user's audit logs
echo "Exporting audit logs..."
gcloud logging read "jsonPayload.username=\"$USERNAME\"" \
  --format=json > "user_audit_${USERNAME}_$(date +%Y%m%d).json"

# 3. Review recent activity
echo "Recent activity:"
gcloud logging read "jsonPayload.username=\"$USERNAME\" AND \
  timestamp>=\"$(date -d '24 hours ago' -Iseconds)\"" --limit=100

# 4. Check for unusual patterns
echo "Checking for unusual patterns..."
# Look for multiple IP addresses, unusual times, etc.
```

### Runbook 3: Database Performance Issue

```bash
#!/bin/bash
# Runbook: Database Performance Response

echo "=== Database Performance Response ==="

# 1. Check current connections
echo "Current database metrics..."
gcloud sql instances describe oscal-db-prod \
  --format="value(settings.databaseFlags)"

# 2. Check slow queries (requires Query Insights)
echo "Check Query Insights in Cloud Console:"
echo "https://console.cloud.google.com/sql/instances/oscal-db-prod/query-insights"

# 3. Check connection count
echo "Active connections..."
# Run via Cloud SQL proxy or direct connection
psql -h $DB_HOST -U $DB_USER -d oscal_production -c \
  "SELECT count(*) FROM pg_stat_activity WHERE state = 'active';"

# 4. Kill long-running queries if needed
echo "Long-running queries:"
psql -h $DB_HOST -U $DB_USER -d oscal_production -c \
  "SELECT pid, now() - pg_stat_activity.query_start AS duration, query
   FROM pg_stat_activity
   WHERE state = 'active' AND now() - pg_stat_activity.query_start > interval '5 minutes';"
```

---

## Appendix

### GCP Quick Reference Commands

```bash
# View all Cloud Run services
gcloud run services list --platform=managed

# View service logs
gcloud logging read "resource.type=cloud_run_revision" --limit=100

# View Cloud SQL instances
gcloud sql instances list

# Export logs to Cloud Storage
gcloud logging read "timestamp>=\"2025-02-16T00:00:00Z\"" \
  --format=json | gsutil cp - gs://BUCKET/incident-logs.json

# Create database backup
gcloud sql backups create --instance=oscal-db-prod
```

### Useful Cloud Console Links

- **Cloud Run**: https://console.cloud.google.com/run
- **Cloud SQL**: https://console.cloud.google.com/sql
- **Cloud Logging**: https://console.cloud.google.com/logs
- **Cloud Monitoring**: https://console.cloud.google.com/monitoring
- **Error Reporting**: https://console.cloud.google.com/errors

---

## Document Control

| Version | Date | Author | Changes |
|---------|------|--------|---------|
| 1.0 | Feb 2025 | Security Team | Initial release |

**Next Review Date**: May 2025

---

*This document should be reviewed quarterly and updated after any significant incident or infrastructure change.*
