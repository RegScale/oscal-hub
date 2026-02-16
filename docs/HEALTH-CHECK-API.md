# Health Check API Documentation

**Date**: February 2025
**Status**: Complete

## Overview

The OSCAL CLI API provides comprehensive health check endpoints for monitoring system health, component status, and operational metrics. These endpoints support both simple monitoring (load balancers, uptime robots) and detailed health dashboards.

## Endpoints

### Public Endpoints (No Authentication Required)

These endpoints are accessible without authentication and are designed for external monitoring tools, load balancers, and health probes.

#### GET /api/health

Returns simple health status suitable for basic monitoring.

**Request:**
```bash
curl http://localhost:8080/api/health
```

**Response (200 OK):**
```json
{
  "status": "UP",
  "timestamp": "2025-02-16T10:30:00Z",
  "version": "1.0.0"
}
```

**Response Fields:**
| Field | Type | Description |
|-------|------|-------------|
| status | string | Health status: `UP` or `DOWN` |
| timestamp | string | ISO 8601 timestamp of the health check |
| version | string | Application version |

#### GET /api/health/ping

Simple ping endpoint for external monitoring tools (UptimeRobot, Pingdom, etc.).

**Request:**
```bash
curl -I http://localhost:8080/api/health/ping
```

**Response:**
- **200 OK** with body `OK` - System is healthy
- **503 Service Unavailable** with body `UNHEALTHY` - System is unhealthy

---

### Protected Endpoints (SUPER_ADMIN Authentication Required)

These endpoints require JWT authentication with SUPER_ADMIN role and provide detailed health information for admin dashboards.

#### GET /api/health/detailed

Returns comprehensive health information including all components, system metrics, and environment details.

**Request:**
```bash
curl -H "Authorization: Bearer YOUR_JWT_TOKEN" \
     http://localhost:8080/api/health/detailed
```

**Response (200 OK):**
```json
{
  "status": "UP",
  "timestamp": "2025-02-16T10:30:00Z",
  "application": {
    "name": "oscal-cli-api",
    "version": "1.0.0",
    "profile": "dev",
    "uptime": "2d 5h 30m 15s",
    "startTime": "2025-02-14T05:00:00Z"
  },
  "components": {
    "database": {
      "status": "UP",
      "message": "Database connection is healthy",
      "details": {
        "database": "PostgreSQL",
        "databaseVersion": "15.0",
        "driverName": "PostgreSQL JDBC Driver",
        "url": "jdbc:postgresql://localhost:5432/oscal_dev"
      },
      "responseTimeMs": 15
    },
    "storage": {
      "status": "UP",
      "message": "Local filesystem storage is available",
      "details": {
        "provider": "local_filesystem",
        "path": "/home/user/.oscal-hub/files",
        "exists": true,
        "writable": true
      },
      "responseTimeMs": 2
    },
    "memory": {
      "status": "UP",
      "message": "Memory usage is healthy: 45.0%",
      "details": {
        "heapUsedMb": 450,
        "heapMaxMb": 1024,
        "heapCommittedMb": 512,
        "usagePercent": 45
      },
      "responseTimeMs": 1
    },
    "cpu": {
      "status": "UP",
      "message": "CPU usage is healthy: 25.0%",
      "details": {
        "availableProcessors": 8,
        "systemLoadAverage": 2.0,
        "cpuUsagePercent": 25,
        "loadPerProcessor": 0.25
      },
      "responseTimeMs": 1
    },
    "diskSpace": {
      "status": "UP",
      "message": "Disk usage is healthy: 35.0%",
      "details": {
        "totalSpaceGb": 500,
        "freeSpaceGb": 325,
        "usableSpaceGb": 320,
        "usagePercent": 35
      },
      "responseTimeMs": 3
    },
    "oscalLibrary": {
      "status": "UP",
      "message": "OSCAL library is available and functional",
      "details": {
        "bindingContextAvailable": true,
        "library": "liboscal-java"
      },
      "responseTimeMs": 50
    },
    "secrets": {
      "status": "UP",
      "message": "All 5 required secrets/configurations are properly set",
      "details": {
        "profile": "dev",
        "configuredCount": 5,
        "missingRequiredCount": 0,
        "missingOptionalCount": 0,
        "warningCount": 0,
        "configured": ["JWT_SECRET", "DB_URL", "DB_USERNAME", "DB_PASSWORD", "CORS_ALLOWED_ORIGINS"]
      },
      "responseTimeMs": 1
    }
  },
  "system": {
    "totalMemoryMb": 1024,
    "usedMemoryMb": 450,
    "freeMemoryMb": 574,
    "memoryUsagePercent": 45,
    "availableProcessors": 8,
    "systemLoadAverage": 1.5,
    "totalDiskSpaceGb": 500,
    "freeDiskSpaceGb": 325,
    "diskUsagePercent": 35
  },
  "environment": {
    "javaVersion": "17.0.2",
    "javaVendor": "Eclipse Adoptium",
    "osName": "Linux",
    "osVersion": "5.15.0",
    "osArch": "amd64",
    "timezone": "America/New_York"
  }
}
```

#### GET /api/health/component/{component}

Returns health status for a specific component.

**Path Parameters:**
| Parameter | Values | Description |
|-----------|--------|-------------|
| component | `database`, `db`, `storage`, `memory`, `cpu`, `processor`, `diskspace`, `disk`, `oscal`, `oscallibrary`, `secrets`, `config`, `configuration` | Component to check |

**Request:**
```bash
curl -H "Authorization: Bearer YOUR_JWT_TOKEN" \
     http://localhost:8080/api/health/component/database
```

**Response (200 OK):**
```json
{
  "status": "UP",
  "message": "Database connection is healthy",
  "details": {
    "database": "PostgreSQL",
    "databaseVersion": "15.0"
  },
  "responseTimeMs": 15
}
```

---

## Components Monitored

### 1. Database
- **What it checks**: PostgreSQL connection pool health via `connection.isValid(5)`
- **Status**: UP when connection is valid
- **Details**: Database name, version, driver, connection URL

### 2. Storage
- **What it checks**: Azure Blob Storage, GCS, or local filesystem availability
- **Status**: UP when storage is accessible and writable
- **Providers**: `azure_blob_storage`, `google_cloud_storage`, `local_filesystem`

### 3. Memory
- **What it checks**: JVM heap memory usage
- **Thresholds**:
  - UP: < 90% usage
  - DEGRADED: >= 90% usage
- **Details**: Heap used/max/committed in MB, usage percentage

### 4. CPU
- **What it checks**: CPU usage and system load
- **Thresholds**:
  - UP: < 80% CPU usage
  - DEGRADED: >= 80% CPU usage
  - DOWN: >= 95% CPU usage (critical)
- **Details**: Available processors, system load average, CPU usage percentage, process CPU load
- **Aliases**: `cpu`, `processor`

### 5. Disk Space
- **What it checks**: Available disk space on the root filesystem
- **Thresholds**:
  - UP: < 90% usage and > 100MB free
  - DEGRADED: >= 90% usage
  - DOWN: < 100MB free
- **Details**: Total/free/usable space in GB, usage percentage

### 6. OSCAL Library
- **What it checks**: OSCAL binding context instantiation
- **Status**: UP when `OscalBindingContext.instance()` returns successfully
- **Details**: Library name (`liboscal-java`)

### 7. Secrets/Configuration
- **What it checks**: Required environment variables and configuration secrets
- **Thresholds**:
  - UP: All required secrets are configured
  - DEGRADED: Warnings present (e.g., using dev defaults in non-prod)
  - DOWN: Missing required secrets
- **Checks performed**:
  - `JWT_SECRET` - JWT signing secret (validates not using default in prod/staging)
  - `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` - Database credentials
  - `CORS_ALLOWED_ORIGINS` - CORS configuration (required in prod/staging)
  - Cloud storage credentials (Azure or AWS, optional warning)
  - `SIEM_WEBHOOK_URL` - Required if SIEM is enabled
- **Details**: Profile, configured count, missing required, missing optional, warnings
- **Aliases**: `secrets`, `config`, `configuration`

---

## Health Status Values

| Status | Description |
|--------|-------------|
| UP | Component is fully operational |
| DOWN | Component is not working - critical issue |
| DEGRADED | Component is operational but experiencing issues (e.g., high memory) |
| UNKNOWN | Unable to determine component status |

---

## External Monitoring Integration

### UptimeRobot / Pingdom Configuration

**URL**: `https://your-domain.com/api/health/ping`
**Expected Response**: `OK`
**HTTP Status**: 200 (healthy) or 503 (unhealthy)

### Kubernetes Health Probes

```yaml
apiVersion: v1
kind: Pod
spec:
  containers:
  - name: oscal-api
    livenessProbe:
      httpGet:
        path: /api/health/ping
        port: 8080
      initialDelaySeconds: 30
      periodSeconds: 10
      failureThreshold: 3
    readinessProbe:
      httpGet:
        path: /api/health
        port: 8080
      initialDelaySeconds: 5
      periodSeconds: 5
      failureThreshold: 3
```

### Docker Compose Health Check

```yaml
services:
  backend:
    image: oscal-backend
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/api/health/ping"]
      interval: 30s
      timeout: 10s
      retries: 3
      start_period: 40s
```

### Cloud Run Health Checks

Cloud Run automatically uses the `/` endpoint for health checks. Configure your service to use:

```yaml
# In Cloud Run service configuration
healthCheck:
  httpGet:
    path: /api/health/ping
    port: 8080
```

### Prometheus Integration

The application also exposes Prometheus metrics at `/actuator/prometheus` (requires authentication). Key metrics:

- `jvm_memory_used_bytes`
- `jvm_memory_max_bytes`
- `hikaricp_connections_active`
- `http_server_requests_seconds`

---

## Accessing the Health Dashboard

The health dashboard is available at `/admin/health` in the web application.

**Features:**
- Overall system status banner
- Component health cards with status indicators
- Memory and disk usage progress bars
- Application information (version, uptime, start time)
- Environment details (Java version, OS, timezone)
- Auto-refresh every 30 seconds (toggleable)
- Manual refresh button

**Access Requirements:**
- Must be logged in as SUPER_ADMIN

---

## Troubleshooting

### Common Issues

#### 1. Health endpoint returns DOWN

**Symptoms**: `/api/health` returns `{"status": "DOWN"}`

**Possible Causes**:
- Database connection failure
- Database pool exhausted

**Solutions**:
```bash
# Check database is running
docker-compose ps
pg_isready -h localhost -p 5432

# Check backend logs
tail -f back-end/logs/spring.log | grep -i "database\|connection"
```

#### 2. 403 Forbidden on /api/health/detailed

**Symptoms**: Detailed health endpoint returns 403

**Possible Causes**:
- Missing JWT token
- Token expired
- User is not SUPER_ADMIN

**Solutions**:
- Log out and log back in to get fresh token
- Verify user has SUPER_ADMIN role

#### 3. Component shows DEGRADED status

**Memory DEGRADED**:
- JVM heap usage >= 90%
- Solution: Increase JVM heap size or restart to clear memory

**Disk DEGRADED**:
- Disk usage >= 90%
- Solution: Free up disk space or expand storage

---

## API Response Codes

| Code | Description |
|------|-------------|
| 200 | Health check successful |
| 400 | Invalid component name (for `/component/{name}`) |
| 403 | Authentication required or insufficient permissions |
| 503 | System unhealthy (for `/ping` endpoint) |
