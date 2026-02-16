# SIEM Integration Guide

**Date**: February 2026
**Status**: Complete

This guide explains how to configure OSCAL Tools to forward audit events to your Security Information and Event Management (SIEM) system.

---

## Overview

The OSCAL Tools API includes built-in SIEM forwarding capabilities that automatically send audit events to external security monitoring systems. This enables:

- **Real-time security monitoring** - Forward authentication failures, authorization violations, and high-risk events
- **Compliance reporting** - Maintain centralized audit logs for regulatory requirements
- **Incident response** - Quickly investigate security events across all systems
- **Threat detection** - Correlate OSCAL activity with other security data

## Architecture

```
┌─────────────────────┐     ┌──────────────────────┐     ┌─────────────────┐
│   OSCAL Tools API   │────▶│  SIEM Forwarding     │────▶│   Your SIEM     │
│   (Audit Events)    │     │  Service (Batched)   │     │   (Webhook)     │
└─────────────────────┘     └──────────────────────┘     └─────────────────┘
```

**How it works:**

1. The API generates audit events for all significant actions (logins, file access, errors, etc.)
2. Events are queued in memory and batched for efficient transmission
3. Batches are sent to your SIEM webhook at configurable intervals
4. Failed sends are automatically retried with exponential backoff

---

## Supported Output Formats

### 1. JSON (Default)

Standard JSON array format, compatible with most modern SIEMs.

```json
[
  {
    "id": 12345,
    "timestamp": "2026-02-15T10:30:00.000Z",
    "eventType": "AUTH_LOGIN_SUCCESS",
    "category": "Authentication",
    "description": "User login successful",
    "username": "admin",
    "userId": 1,
    "ipAddress": "192.168.1.100",
    "userAgent": "Mozilla/5.0...",
    "outcome": "SUCCESS",
    "riskLevel": "LOW",
    "httpMethod": "POST",
    "requestUrl": "/api/auth/login",
    "processingTimeMs": 150,
    "metadata": { "organizationId": 5 }
  }
]
```

### 2. CEF (Common Event Format)

ArcSight-compatible format, widely supported by enterprise SIEMs.

```
CEF:0|NIST|OSCAL-Tools|1.0|AUTH_LOGIN_FAILURE|User login failed|6|rt=2026-02-15T10:30:00.000Z suser=unknown src=192.168.1.100 request=/api/auth/login requestMethod=POST outcome=FAILURE msg=Invalid credentials
```

**CEF Severity Mapping:**
| Risk Level | CEF Severity |
|------------|--------------|
| LOW        | 3            |
| MEDIUM     | 6            |
| HIGH       | 9            |

### 3. Syslog (RFC 5424)

Standard syslog format for traditional log aggregators.

```
<134>1 2026-02-15T10:30:00.000Z oscal-api 12345 AUTH_LOGIN_SUCCESS [oscal@1 eventType="AUTH_LOGIN_SUCCESS" category="Authentication" outcome="SUCCESS" riskLevel="LOW" user="admin" ip="192.168.1.100"] User login successful
```

---

## Configuration

### Environment Variables

All SIEM settings can be configured via environment variables:

| Variable | Description | Default |
|----------|-------------|---------|
| `SIEM_ENABLED` | Enable/disable SIEM forwarding | `false` |
| `SIEM_WEBHOOK_URL` | Your SIEM webhook endpoint URL | (none) |
| `SIEM_AUTH_HEADER` | Authorization header value (e.g., `Bearer token123`) | (none) |
| `SIEM_AUTH_HEADER_NAME` | Custom auth header name | `Authorization` |
| `SIEM_FORMAT` | Output format: `json`, `cef`, or `syslog` | `json` |
| `SIEM_BATCH_SIZE` | Number of events per batch | `100` |
| `SIEM_BATCH_INTERVAL` | Seconds between batch sends | `60` |
| `SIEM_INCLUDE_CATEGORIES` | Comma-separated categories to include (empty = all) | (all) |
| `SIEM_MIN_RISK_LEVEL` | Minimum risk level: `LOW`, `MEDIUM`, `HIGH` | `LOW` |
| `SIEM_FAILED_EVENTS_ONLY` | Only forward failure/error events | `false` |
| `SIEM_MAX_RETRIES` | Retry attempts for failed sends | `3` |
| `SIEM_RETRY_DELAY_MS` | Initial retry delay in milliseconds | `1000` |
| `SIEM_RETRY_BACKOFF` | Retry backoff multiplier | `2.0` |
| `SIEM_CONNECTION_TIMEOUT` | Connection timeout in milliseconds | `5000` |
| `SIEM_READ_TIMEOUT` | Read timeout in milliseconds | `30000` |

**CEF-specific settings:**

| Variable | Description | Default |
|----------|-------------|---------|
| `SIEM_CEF_VENDOR` | Vendor name in CEF header | `NIST` |
| `SIEM_CEF_PRODUCT` | Product name in CEF header | `OSCAL-Tools` |
| `SIEM_CEF_VERSION` | Version in CEF header | `1.0` |

**Syslog-specific settings:**

| Variable | Description | Default |
|----------|-------------|---------|
| `SIEM_SYSLOG_FACILITY` | Syslog facility (LOCAL0-7, AUTH, AUTHPRIV) | `LOCAL0` |
| `SIEM_SYSLOG_HOSTNAME` | Hostname in syslog messages | `oscal-api` |
| `SIEM_SYSLOG_APP_NAME` | Application name in syslog messages | `oscal-api` |

### Example Configurations

#### Basic Setup (JSON to Splunk HEC)

```bash
export SIEM_ENABLED=true
export SIEM_WEBHOOK_URL=https://splunk.example.com:8088/services/collector/event
export SIEM_AUTH_HEADER="Splunk YOUR-HEC-TOKEN"
export SIEM_FORMAT=json
```

#### High-Security Events Only (CEF to ArcSight)

```bash
export SIEM_ENABLED=true
export SIEM_WEBHOOK_URL=https://arcsight.example.com/receiver
export SIEM_AUTH_HEADER="Bearer your-api-key"
export SIEM_FORMAT=cef
export SIEM_MIN_RISK_LEVEL=MEDIUM
export SIEM_INCLUDE_CATEGORIES=Security,Authorization,Authentication
```

#### Failure Monitoring (Syslog to Elastic)

```bash
export SIEM_ENABLED=true
export SIEM_WEBHOOK_URL=https://elastic.example.com:9200/_bulk
export SIEM_AUTH_HEADER="ApiKey your-elastic-api-key"
export SIEM_FORMAT=syslog
export SIEM_FAILED_EVENTS_ONLY=true
export SIEM_BATCH_INTERVAL=30
```

---

## Event Categories

Events are organized into the following categories:

| Category | Description | Example Events |
|----------|-------------|----------------|
| **Authentication** | Login/logout events | `AUTH_LOGIN_SUCCESS`, `AUTH_LOGIN_FAILURE`, `AUTH_LOGOUT` |
| **Authorization** | Permission checks | `AUTHZ_ACCESS_DENIED`, `AUTHZ_ROLE_CHANGED` |
| **Data Access** | File and data operations | `DATA_FILE_ACCESS`, `DATA_FILE_UPLOAD`, `DATA_FILE_DELETE` |
| **Configuration** | System settings changes | `CONFIG_SETTING_CHANGED`, `CONFIG_USER_CREATED` |
| **Security** | Security-related events | `SEC_RATE_LIMIT_EXCEEDED`, `SEC_INVALID_TOKEN` |
| **System** | System operations | `SYS_STARTUP`, `SYS_SHUTDOWN`, `SYS_ERROR` |

### Filtering by Category

To forward only specific categories:

```bash
# Only security and authentication events
export SIEM_INCLUDE_CATEGORIES=Security,Authentication,Authorization
```

Leave empty to forward all categories.

---

## Risk Levels

Each event has a risk level that indicates its security significance:

| Risk Level | Description | Example Events |
|------------|-------------|----------------|
| **LOW** | Normal operations | Successful logins, file reads |
| **MEDIUM** | Notable events | Failed logins, permission changes |
| **HIGH** | Security concerns | Multiple failed logins, unauthorized access attempts |

### Filtering by Risk Level

```bash
# Only forward MEDIUM and HIGH risk events
export SIEM_MIN_RISK_LEVEL=MEDIUM

# Only forward HIGH risk events
export SIEM_MIN_RISK_LEVEL=HIGH
```

---

## API Endpoints

The following admin endpoints are available for managing SIEM integration:

### Get SIEM Status

```
GET /api/admin/logs/siem/status
```

Returns current forwarding status and metrics:

```json
{
  "enabled": true,
  "healthy": true,
  "eventsForwarded": 15420,
  "eventsFailed": 12,
  "batchesSent": 155,
  "batchesFailed": 1,
  "queuedEvents": 23,
  "lastSuccessfulSend": "2026-02-15T10:30:00",
  "lastFailedSend": "2026-02-14T03:22:00",
  "lastError": null,
  "webhookUrl": "***configured***",
  "format": "json"
}
```

### Test SIEM Connection

```
POST /api/admin/logs/siem/test
```

Sends a test event to verify connectivity:

```json
{
  "success": true,
  "message": "Connection successful"
}
```

Or on failure:

```json
{
  "success": false,
  "message": "Connection failed: HTTP 401: Unauthorized"
}
```

### Manual Flush

```
POST /api/admin/logs/siem/flush
```

Immediately sends all queued events:

```json
{
  "flushedEvents": 45,
  "message": "Events flushed successfully"
}
```

---

## Batching and Performance

### How Batching Works

1. Events are queued in memory as they occur
2. Batches are sent when:
   - Queue reaches `SIEM_BATCH_SIZE` (default: 100 events)
   - Batch interval timer fires (default: every 60 seconds)
   - Application shuts down (graceful flush)
   - Manual flush is triggered via API

### Tuning Recommendations

| Scenario | Batch Size | Interval | Notes |
|----------|------------|----------|-------|
| Low volume (<100 events/min) | 50 | 60s | Reduces latency |
| Medium volume (100-1000/min) | 100 | 30s | Balanced |
| High volume (>1000/min) | 500 | 15s | Reduces API calls |
| Real-time monitoring | 10 | 5s | Minimal latency, more overhead |

---

## Retry Logic

Failed sends are automatically retried with exponential backoff:

1. **Attempt 1**: Immediate
2. **Attempt 2**: Wait 1 second (configurable via `SIEM_RETRY_DELAY_MS`)
3. **Attempt 3**: Wait 2 seconds (delay × backoff multiplier)
4. **Give up**: Events marked as failed in metrics

### Configuring Retries

```bash
# More aggressive retries for critical events
export SIEM_MAX_RETRIES=5
export SIEM_RETRY_DELAY_MS=500
export SIEM_RETRY_BACKOFF=1.5
```

---

## SIEM-Specific Setup Guides

### Splunk

1. Create an HTTP Event Collector (HEC) token in Splunk
2. Configure OSCAL Tools:

```bash
export SIEM_ENABLED=true
export SIEM_WEBHOOK_URL=https://your-splunk:8088/services/collector/event
export SIEM_AUTH_HEADER="Splunk YOUR-HEC-TOKEN"
export SIEM_FORMAT=json
```

### Elastic Security

1. Create an API key with write permissions to your index
2. Configure OSCAL Tools:

```bash
export SIEM_ENABLED=true
export SIEM_WEBHOOK_URL=https://your-elastic:9200/oscal-audit/_bulk
export SIEM_AUTH_HEADER="ApiKey YOUR-API-KEY"
export SIEM_FORMAT=json
```

### Microsoft Sentinel

1. Create a Data Connector with webhook ingestion
2. Configure OSCAL Tools:

```bash
export SIEM_ENABLED=true
export SIEM_WEBHOOK_URL=https://YOUR-WORKSPACE.ods.opinsights.azure.com/api/logs?api-version=2016-04-01
export SIEM_AUTH_HEADER="SharedKey YOUR-SHARED-KEY"
export SIEM_AUTH_HEADER_NAME=Authorization
export SIEM_FORMAT=json
```

### IBM QRadar

1. Configure a Log Source with CEF parsing
2. Configure OSCAL Tools:

```bash
export SIEM_ENABLED=true
export SIEM_WEBHOOK_URL=https://qradar.example.com/api/ariel/searches
export SIEM_AUTH_HEADER="SEC YOUR-TOKEN"
export SIEM_FORMAT=cef
export SIEM_CEF_VENDOR=NIST
export SIEM_CEF_PRODUCT=OSCAL-Tools
```

---

## Troubleshooting

### Events Not Being Forwarded

1. **Check if enabled**: Verify `SIEM_ENABLED=true`
2. **Check webhook URL**: Ensure `SIEM_WEBHOOK_URL` is set correctly
3. **Check filters**: Events may be filtered out by category or risk level
4. **Check status endpoint**: `GET /api/admin/logs/siem/status`
5. **Test connection**: `POST /api/admin/logs/siem/test`

### Connection Failures

1. **Verify URL is reachable** from the server
2. **Check authentication**: Ensure `SIEM_AUTH_HEADER` is correct
3. **Check firewall rules**: Ensure outbound HTTPS is allowed
4. **Increase timeouts** if network is slow:
   ```bash
   export SIEM_CONNECTION_TIMEOUT=10000
   export SIEM_READ_TIMEOUT=60000
   ```

### High Failure Rate

1. **Check SIEM capacity**: Your SIEM may be rate-limiting
2. **Increase batch interval**: Reduce load on SIEM
3. **Filter events**: Forward only critical events
4. **Check status endpoint**: Look at `lastError` for details

### Format Issues

1. **Verify SIEM parser**: Ensure SIEM is configured to parse the format
2. **Test with curl**: Manually send a test payload to verify format
3. **Check logs**: Backend logs show formatted payloads on debug level

---

## Security Considerations

1. **Use HTTPS**: Always use HTTPS for webhook URLs
2. **Rotate tokens**: Regularly rotate SIEM API tokens
3. **Restrict access**: SIEM endpoints require SUPER_ADMIN role
4. **Monitor failures**: Set up alerts for SIEM forwarding failures
5. **Sensitive data**: Audit events may contain usernames and IPs - ensure your SIEM access is appropriately restricted

---

## Docker/Kubernetes Deployment

### Docker Compose Example

```yaml
services:
  oscal-backend:
    image: oscal-tools-backend
    environment:
      - SIEM_ENABLED=true
      - SIEM_WEBHOOK_URL=https://splunk.example.com:8088/services/collector/event
      - SIEM_AUTH_HEADER=Splunk ${SPLUNK_HEC_TOKEN}
      - SIEM_FORMAT=json
      - SIEM_MIN_RISK_LEVEL=LOW
```

### Kubernetes Secret Example

```yaml
apiVersion: v1
kind: Secret
metadata:
  name: siem-credentials
type: Opaque
stringData:
  webhook-url: "https://splunk.example.com:8088/services/collector/event"
  auth-header: "Splunk your-hec-token"
---
apiVersion: apps/v1
kind: Deployment
metadata:
  name: oscal-backend
spec:
  template:
    spec:
      containers:
        - name: backend
          env:
            - name: SIEM_ENABLED
              value: "true"
            - name: SIEM_WEBHOOK_URL
              valueFrom:
                secretKeyRef:
                  name: siem-credentials
                  key: webhook-url
            - name: SIEM_AUTH_HEADER
              valueFrom:
                secretKeyRef:
                  name: siem-credentials
                  key: auth-header
```
