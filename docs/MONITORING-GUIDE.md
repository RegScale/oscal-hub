# Monitoring and Observability Guide

**Date:** October 26, 2025
**Status:** Implemented - Phase 1 Complete
**Version:** 1.0

## Overview

This guide covers the monitoring and observability features implemented in the OSCAL Tools application using Spring Boot Actuator and Micrometer Prometheus.

## What Was Implemented

### Phase 1: Foundation (Complete)
- ✅ Spring Boot Actuator for health checks and metrics
- ✅ Micrometer Prometheus for metrics export
- ✅ Custom health indicators for Azure Blob Storage
- ✅ Environment-specific actuator configuration
- ✅ Security configuration for actuator endpoints
- ✅ Comprehensive health checks (database, disk space, custom components)

## Available Endpoints

### Public Endpoints (No Authentication Required)

#### Health Check
```bash
curl http://localhost:8080/actuator/health
```

**Response**:
```json
{
  "status": "UP",
  "components": {
    "azureBlobStorage": {
      "status": "UP",
      "details": {
        "status": "not_configured",
        "storage": "local_fallback",
        "message": "Azure Blob Storage is not configured, using local storage"
      }
    },
    "db": {
      "status": "UP",
      "details": {
        "database": "PostgreSQL",
        "validationQuery": "isValid()"
      }
    },
    "diskSpace": {
      "status": "UP",
      "details": {
        "total": 500000000000,
        "free": 200000000000,
        "threshold": 10485760,
        "path": "/Users/travishowerton/Documents/GitHub/oscal-cli/.",
        "exists": true
      }
    },
    "ping": {
      "status": "UP"
    }
  }
}
```

**Use Cases**:
- Load balancer health checks
- Kubernetes readiness/liveness probes
- Uptime monitoring services
- Basic availability monitoring

####  Info
```bash
curl http://localhost:8080/actuator/info
```

**Response**:
```json
{
  "app": {
    "name": "oscal-cli-api",
    "version": "1.0.0-SNAPSHOT",
    "description": "REST API for OSCAL CLI operations"
  },
  "build": {
    "artifact": "oscal-cli-api",
    "name": "OSCAL CLI Web API",
    "time": "2025-10-26T16:00:00.000Z",
    "version": "1.0.0-SNAPSHOT",
    "group": "gov.nist.oscal.tools"
  },
  "java": {
    "version": "21.0.8",
    "vendor": {
      "name": "Eclipse Adoptium"
    },
    "runtime": {
      "name": "OpenJDK Runtime Environment",
      "version": "21.0.8+7"
    },
    "jvm": {
      "name": "OpenJDK 64-Bit Server VM",
      "version": "21.0.8+7"
    }
  },
  "os": {
    "name": "Mac OS X",
    "version": "14.6.0",
    "arch": "aarch64"
  },
  "git": {
    "branch": "howerton",
    "commit": {
      "id": "bb069da",
      "time": "2025-10-26T20:00:00Z"
    }
  }
}
```

**Use Cases**:
- Version verification
- Deployment validation
- Build tracking
- Environment diagnostics

### Protected Endpoints (Authentication Required)

All other actuator endpoints require JWT authentication. Include the JWT token in the Authorization header:

```bash
curl -H "Authorization: Bearer YOUR_JWT_TOKEN" \
     http://localhost:8080/actuator/metrics
```

#### Metrics Endpoint
```bash
curl -H "Authorization: Bearer YOUR_JWT_TOKEN" \
     http://localhost:8080/actuator/metrics
```

**Available Metrics** (partial list):
- `jvm.memory.used` - JVM memory usage
- `jvm.memory.max` - Maximum JVM memory
- `jvm.threads.live` - Number of live threads
- `jvm.gc.pause` - Garbage collection pause times
- `system.cpu.usage` - System CPU usage
- `process.cpu.usage` - Process CPU usage
- `http.server.requests` - HTTP request metrics (count, duration, percentiles)
- `jdbc.connections.active` - Active database connections
- `jdbc.connections.max` - Maximum database connections
- `logback.events` - Log event counts by level

**Query Specific Metric**:
```bash
curl -H "Authorization: Bearer YOUR_JWT_TOKEN" \
     http://localhost:8080/actuator/metrics/http.server.requests
```

**Response**:
```json
{
  "name": "http.server.requests",
  "description": "Duration of HTTP server requests",
  "baseUnit": "seconds",
  "measurements": [
    {
      "statistic": "COUNT",
      "value": 1524.0
    },
    {
      "statistic": "TOTAL_TIME",
      "value": 45.629
    },
    {
      "statistic": "MAX",
      "value": 0.523
    }
  ],
  "availableTags": [
    {
      "tag": "exception",
      "values": ["None", "IOException"]
    },
    {
      "tag": "method",
      "values": ["GET", "POST", "PUT", "DELETE"]
    },
    {
      "tag": "uri",
      "values": ["/api/validation/validate", "/api/conversion/convert", "/api/auth/login"]
    },
    {
      "tag": "status",
      "values": ["200", "400", "401", "403", "500"]
    }
  ]
}
```

#### Prometheus Metrics Export
```bash
curl -H "Authorization: Bearer YOUR_JWT_TOKEN" \
     http://localhost:8080/actuator/prometheus
```

**Response** (Prometheus text format):
```
# HELP jvm_memory_used_bytes The amount of used memory
# TYPE jvm_memory_used_bytes gauge
jvm_memory_used_bytes{area="heap",id="G1 Eden Space",} 2.1233664E7
jvm_memory_used_bytes{area="heap",id="G1 Old Gen",} 1.1534336E8
jvm_memory_used_bytes{area="nonheap",id="Metaspace",} 7.9267648E7

# HELP http_server_requests_seconds Duration of HTTP server requests
# TYPE http_server_requests_seconds histogram
http_server_requests_seconds_bucket{exception="None",method="GET",status="200",uri="/api/validation/validate",le="0.001",} 45.0
http_server_requests_seconds_bucket{exception="None",method="GET",status="200",uri="/api/validation/validate",le="0.002",} 78.0
http_server_requests_seconds_bucket{exception="None",method="GET",status="200",uri="/api/validation/validate",le="0.005",} 123.0
http_server_requests_seconds_count{exception="None",method="GET",status="200",uri="/api/validation/validate",} 150.0
http_server_requests_seconds_sum{exception="None",method="GET",status="200",uri="/api/validation/validate",} 12.456

# HELP system_cpu_usage The "recent cpu usage" for the whole system
# TYPE system_cpu_usage gauge
system_cpu_usage 0.35
```

**Use Cases**:
- Scraping metrics into Prometheus
- Grafana dashboard creation
- Alert rule configuration
- Performance monitoring

## Environment-Specific Configuration

### Development (Profile: `dev`)

**Configuration** (`application-dev.properties`):
- All actuator endpoints exposed (for debugging)
- Health details always shown
- Additional debug endpoints enabled

**Available Endpoints**:
- `/actuator/*` - All endpoints available
- `/actuator/env` - Environment properties
- `/actuator/configprops` - Configuration properties
- `/actuator/beans` - Spring beans
- `/actuator/mappings` - Request mappings

**Example**:
```bash
# No authentication required in dev
curl http://localhost:8080/actuator/beans
curl http://localhost:8080/actuator/env
curl http://localhost:8080/actuator/mappings
```

### Production (Profile: `prod`)

**Configuration** (`application-prod.properties`):
- Minimal endpoints exposed (security)
- Health details only when authenticated
- Sensitive endpoints disabled

**Available Endpoints**:
- `/actuator/health` - Public health check
- `/actuator/info` - Public application info
- `/actuator/metrics` - Requires authentication
- `/actuator/prometheus` - Requires authentication

**Disabled in Production**:
- `/actuator/env` ❌
- `/actuator/configprops` ❌
- `/actuator/beans` ❌
- `/actuator/mappings` ❌
- `/actuator/shutdown` ❌

## Custom Health Indicators

### Azure Blob Storage Health Indicator

**Location**: `back-end/src/main/java/gov/nist/oscal/tools/api/health/AzureBlobStorageHealthIndicator.java`

**Purpose**: Monitors Azure Blob Storage connectivity and configuration status.

**Responses**:

1. **Not Configured** (using local storage):
```json
{
  "status": "UP",
  "details": {
    "status": "not_configured",
    "storage": "local_fallback",
    "message": "Azure Blob Storage is not configured, using local storage"
  }
}
```

2. **Configured and Connected**:
```json
{
  "status": "UP",
  "details": {
    "status": "configured",
    "storage": "azure_blob_storage",
    "message": "Azure Blob Storage is configured and ready"
  }
}
```

3. **Connection Failed**:
```json
{
  "status": "DOWN",
  "details": {
    "status": "error",
    "error": "BlobStorageException",
    "message": "Connection refused"
  }
}
```

### Database Health Indicator

**Built-in**: Spring Boot automatic configuration

**Checks**:
- Database connectivity
- Connection pool status
- Query validation

## Integration with Monitoring Tools

### Prometheus Integration

1. **Add Prometheus scrape configuration** (`prometheus.yml`):

```yaml
scrape_configs:
  - job_name: 'oscal-tools'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['localhost:8080']
    basic_auth:
      username: 'your-username'
      password: 'your-password'
    scrape_interval: 15s
    scrape_timeout: 10s
```

2. **Start Prometheus**:
```bash
prometheus --config.file=prometheus.yml
```

3. **Access Prometheus UI**: http://localhost:9090

4. **Sample Queries**:
```promql
# Request rate
rate(http_server_requests_seconds_count[5m])

# 95th percentile response time
histogram_quantile(0.95, rate(http_server_requests_seconds_bucket[5m]))

# JVM memory usage
jvm_memory_used_bytes{area="heap"}

# Error rate
rate(http_server_requests_seconds_count{status=~"5.."}[5m])
```

### Grafana Dashboards

1. **Add Prometheus Data Source**:
   - Navigate to Configuration > Data Sources
   - Add Prometheus
   - URL: http://localhost:9090

2. **Import Pre-built Dashboard**:
   - Dashboard ID: 11378 (JVM Micrometer)
   - Dashboard ID: 4701 (Spring Boot Statistics)

3. **Custom Dashboard Panels**:

**API Request Rate**:
```promql
sum(rate(http_server_requests_seconds_count[5m])) by (uri, method)
```

**Response Time (p95)**:
```promql
histogram_quantile(0.95,
  sum(rate(http_server_requests_seconds_bucket[5m])) by (le, uri)
)
```

**Error Rate**:
```promql
sum(rate(http_server_requests_seconds_count{status=~"5.."}[5m])) /
sum(rate(http_server_requests_seconds_count[5m]))
```

**Database Connections**:
```promql
jdbc_connections_active{application="oscal-cli-api"}
```

**JVM Heap Usage**:
```promql
jvm_memory_used_bytes{area="heap"} /
jvm_memory_max_bytes{area="heap"} * 100
```

### Alert Rules

**High Error Rate** (`alerts.yml`):
```yaml
groups:
  - name: oscal_tools_alerts
    rules:
      - alert: HighErrorRate
        expr: |
          sum(rate(http_server_requests_seconds_count{status=~"5.."}[5m])) /
          sum(rate(http_server_requests_seconds_count[5m])) > 0.05
        for: 5m
        labels:
          severity: critical
        annotations:
          summary: "High error rate detected"
          description: "Error rate is {{ $value | humanizePercentage }}"

      - alert: HighResponseTime
        expr: |
          histogram_quantile(0.95,
            sum(rate(http_server_requests_seconds_bucket[5m])) by (le)
          ) > 2
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "High response time detected"
          description: "95th percentile response time is {{ $value }}s"

      - alert: DatabaseConnectionPoolExhausted
        expr: jdbc_connections_active / jdbc_connections_max > 0.9
        for: 2m
        labels:
          severity: warning
        annotations:
          summary: "Database connection pool nearly exhausted"
          description: "{{ $value | humanizePercentage }} of connections in use"

      - alert: HighMemoryUsage
        expr: |
          jvm_memory_used_bytes{area="heap"} /
          jvm_memory_max_bytes{area="heap"} > 0.85
        for: 10m
        labels:
          severity: warning
        annotations:
          summary: "High JVM heap usage"
          description: "Heap usage is {{ $value | humanizePercentage }}"

      - alert: ServiceDown
        expr: up{job="oscal-tools"} == 0
        for: 1m
        labels:
          severity: critical
        annotations:
          summary: "OSCAL Tools service is down"
          description: "Service has been down for more than 1 minute"
```

## Kubernetes Integration

### Liveness Probe
```yaml
livenessProbe:
  httpGet:
    path: /actuator/health
    port: 8080
  initialDelaySeconds: 60
  periodSeconds: 10
  timeoutSeconds: 5
  failureThreshold: 3
```

### Readiness Probe
```yaml
readinessProbe:
  httpGet:
    path: /actuator/health
    port: 8080
  initialDelaySeconds: 30
  periodSeconds: 5
  timeoutSeconds: 3
  failureThreshold: 3
```

### ServiceMonitor (Prometheus Operator)
```yaml
apiVersion: monitoring.coreos.com/v1
kind: ServiceMonitor
metadata:
  name: oscal-tools
  labels:
    app: oscal-tools
spec:
  selector:
    matchLabels:
      app: oscal-tools
  endpoints:
    - port: http
      path: /actuator/prometheus
      interval: 30s
      basicAuth:
        username:
          name: oscal-tools-basic-auth
          key: username
        password:
          name: oscal-tools-basic-auth
          key: password
```

## Monitoring Best Practices

### 1. What to Monitor

**Golden Signals**:
- **Latency**: Response time (p50, p95, p99)
- **Traffic**: Requests per second
- **Errors**: Error rate and types
- **Saturation**: Resource utilization (CPU, memory, connections)

**Application Metrics**:
- Validation operations per minute
- Conversion operations per minute
- Profile resolution operations per minute
- Authentication success/failure rates
- File upload sizes and frequencies

**Infrastructure Metrics**:
- JVM heap usage and GC pauses
- Database connection pool utilization
- Disk space usage
- Network I/O

### 2. Alert Thresholds

**Critical** (immediate action required):
- Service down (> 1 minute)
- Error rate > 10%
- Response time p95 > 5 seconds
- Memory usage > 95%

**Warning** (investigate soon):
- Error rate > 5%
- Response time p95 > 2 seconds
- Memory usage > 85%
- Database connections > 90%

**Info** (awareness):
- Response time p95 > 1 second
- Memory usage > 70%
- Disk space < 20%

### 3. Dashboard Layout

**Overview Dashboard**:
1. Service status (UP/DOWN)
2. Request rate (last hour)
3. Error rate (last hour)
4. Response time (p95, last hour)
5. Active users

**Detailed Dashboard**:
1. Request rate by endpoint
2. Response time by endpoint
3. Error breakdown by status code
4. JVM metrics (heap, GC)
5. Database metrics (connections, query time)
6. Custom business metrics

### 4. Retention Policies

**Metrics**:
- Raw data: 15 days
- 5-minute aggregates: 60 days
- 1-hour aggregates: 1 year

**Logs**:
- DEBUG/INFO: 7 days
- WARN: 30 days
- ERROR: 90 days

## Troubleshooting

### Health Check Returns DOWN

1. **Check component details**:
```bash
curl http://localhost:8080/actuator/health | jq '.components'
```

2. **Identify failing component**:
   - Database: Check PostgreSQL connection
   - Disk Space: Check available storage
   - Azure Blob Storage: Check configuration/connectivity

3. **Check logs**:
```bash
tail -f back-end/logs/spring.log | grep -i error
```

### Metrics Not Appearing in Prometheus

1. **Verify Prometheus scrape config**:
```bash
curl http://localhost:9090/api/v1/targets
```

2. **Test metrics endpoint**:
```bash
curl -H "Authorization: Bearer YOUR_JWT_TOKEN" \
     http://localhost:8080/actuator/prometheus
```

3. **Check Prometheus logs**:
```bash
docker logs prometheus
```

### High Memory Usage

1. **Check heap usage**:
```bash
curl -H "Authorization: Bearer YOUR_JWT_TOKEN" \
     http://localhost:8080/actuator/metrics/jvm.memory.used | jq
```

2. **Trigger heap dump**:
```bash
curl -H "Authorization: Bearer YOUR_JWT_TOKEN" \
     -X POST http://localhost:8080/actuator/heapdump \
     --output heapdump.hprof
```

3. **Analyze with VisualVM or Eclipse MAT**

## Next Steps

### Phase 2: Enhanced Observability

1. **Distributed Tracing**:
   - Add Spring Cloud Sleuth or OpenTelemetry
   - Configure Jaeger or Zipkin
   - Add trace IDs to logs

2. **Centralized Logging**:
   - Configure Logback JSON logging
   - Set up ELK stack (Elasticsearch, Logback, Kibana)
   - Add correlation IDs

3. **APM Integration**:
   - Integrate New Relic, Datadog, or Dynatrace
   - Configure automatic instrumentation
   - Set up error tracking

4. **Custom Metrics**:
   - Business metrics (validations/hour, conversions/hour)
   - User activity metrics
   - Performance SLIs/SLOs

5. **Alerting**:
   - Configure PagerDuty or Opsgenie
   - Set up escalation policies
   - Create runbooks for alerts

## Resources

### Documentation
- [Spring Boot Actuator Documentation](https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html)
- [Micrometer Documentation](https://micrometer.io/docs)
- [Prometheus Documentation](https://prometheus.io/docs/)
- [Grafana Documentation](https://grafana.com/docs/)

### Sample Queries
- [PromQL Examples](https://prometheus.io/docs/prometheus/latest/querying/examples/)
- [Grafana Dashboard Gallery](https://grafana.com/grafana/dashboards/)

### Tools
- [Prometheus](https://prometheus.io/)
- [Grafana](https://grafana.com/)
- [Jaeger](https://www.jaegertracing.io/)
- [ELK Stack](https://www.elastic.co/elastic-stack)

## Summary

The monitoring foundation is now in place with:
- ✅ Health checks for all critical components
- ✅ Comprehensive metrics collection
- ✅ Prometheus-compatible metrics export
- ✅ Environment-specific configuration
- ✅ Security-conscious endpoint exposure

This provides immediate visibility into application health and performance, forming the foundation for advanced observability features in future phases.
