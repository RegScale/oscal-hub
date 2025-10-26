# Grafana Dashboards for OSCAL Tools

## Quick Start - Import Pre-built Dashboards

Grafana has excellent pre-built dashboards for Spring Boot applications. Import these:

### 1. JVM (Micrometer) Dashboard
- **Dashboard ID**: 4701
- **Name**: JVM (Micrometer)
- **URL**: https://grafana.com/grafana/dashboards/4701

**To Import**:
1. Go to Grafana UI (http://localhost:3001)
2. Click "+" → "Import"
3. Enter ID: `4701`
4. Click "Load"
5. Select "Prometheus" as datasource
6. Click "Import"

### 2. Spring Boot Statistics Dashboard
- **Dashboard ID**: 6756
- **Name**: Spring Boot Statistics
- **URL**: https://grafana.com/grafana/dashboards/6756

**To Import**:
1. Same process as above
2. Enter ID: `6756`

### 3. Prometheus Stats Dashboard
- **Dashboard ID**: 2
- **Name**: Prometheus 2.0 Stats
- **URL**: https://grafana.com/grafana/dashboards/2

## Custom Queries for OSCAL Tools

### Application Health
```promql
# Service UP/DOWN
up{job="oscal-tools-api"}

# Health check status
health_status{job="oscal-tools-api"}
```

### Request Metrics
```promql
# Request rate (requests per second)
sum(rate(http_server_requests_seconds_count{job="oscal-tools-api"}[5m]))

# Request rate by endpoint
sum(rate(http_server_requests_seconds_count{job="oscal-tools-api"}[5m])) by (uri, method)

# Response time (p95)
histogram_quantile(0.95, sum(rate(http_server_requests_seconds_bucket{job="oscal-tools-api"}[5m])) by (le))

# Response time by endpoint
histogram_quantile(0.95, sum(rate(http_server_requests_seconds_bucket{job="oscal-tools-api"}[5m])) by (le, uri))

# Error rate
sum(rate(http_server_requests_seconds_count{status=~"5..",job="oscal-tools-api"}[5m])) /
sum(rate(http_server_requests_seconds_count{job="oscal-tools-api"}[5m]))

# Errors by status code
sum(rate(http_server_requests_seconds_count{job="oscal-tools-api"}[5m])) by (status)
```

### JVM Metrics
```promql
# Heap memory usage (percentage)
jvm_memory_used_bytes{area="heap",job="oscal-tools-api"} /
jvm_memory_max_bytes{area="heap",job="oscal-tools-api"} * 100

# GC pause time
rate(jvm_gc_pause_seconds_sum{job="oscal-tools-api"}[5m])

# Thread count
jvm_threads_live{job="oscal-tools-api"}
```

### Database Metrics
```promql
# Active connections
jdbc_connections_active{job="oscal-tools-api"}

# Connection pool utilization
jdbc_connections_active{job="oscal-tools-api"} /
jdbc_connections_max{job="oscal-tools-api"} * 100
```

### System Metrics
```promql
# CPU usage
system_cpu_usage{job="oscal-tools-api"}

# Disk space free
health_component_diskSpace_details_free{job="oscal-tools-api"}
```

## Creating Custom Dashboards

### Panel 1: Service Status
- **Type**: Stat
- **Query**: `up{job="oscal-tools-api"}`
- **Thresholds**: 0 = Red, 1 = Green
- **Value Mappings**: 0 = DOWN, 1 = UP

### Panel 2: Request Rate (Time Series)
- **Type**: Time series
- **Query**: `sum(rate(http_server_requests_seconds_count{job="oscal-tools-api"}[5m]))`
- **Unit**: req/sec
- **Legend**: Request Rate

### Panel 3: Response Time (Time Series)
- **Type**: Time series
- **Queries**:
  - p50: `histogram_quantile(0.50, sum(rate(http_server_requests_seconds_bucket{job="oscal-tools-api"}[5m])) by (le))`
  - p95: `histogram_quantile(0.95, sum(rate(http_server_requests_seconds_bucket{job="oscal-tools-api"}[5m])) by (le))`
  - p99: `histogram_quantile(0.99, sum(rate(http_server_requests_seconds_bucket{job="oscal-tools-api"}[5m])) by (le))`
- **Unit**: seconds (s)
- **Legend**: {{quantile}}

### Panel 4: Error Rate (Gauge)
- **Type**: Gauge
- **Query**:
  ```promql
  sum(rate(http_server_requests_seconds_count{status=~"5..",job="oscal-tools-api"}[5m])) /
  sum(rate(http_server_requests_seconds_count{job="oscal-tools-api"}[5m])) * 100
  ```
- **Unit**: percent (0-100)
- **Thresholds**: 0-1% = Green, 1-5% = Yellow, >5% = Red

### Panel 5: Memory Usage (Time Series)
- **Type**: Time series
- **Query**:
  ```promql
  jvm_memory_used_bytes{area="heap",job="oscal-tools-api"} /
  jvm_memory_max_bytes{area="heap",job="oscal-tools-api"} * 100
  ```
- **Unit**: percent (0-100)
- **Thresholds**: <70% = Green, 70-85% = Yellow, >85% = Red

### Panel 6: Top Endpoints by Traffic (Table)
- **Type**: Table
- **Query**:
  ```promql
  topk(10, sum(rate(http_server_requests_seconds_count{job="oscal-tools-api"}[5m])) by (uri, method))
  ```
- **Columns**: Method, URI, Request Rate

## Variables (Optional)

Add these dashboard variables for dynamic filtering:

### $instance
- **Type**: Query
- **Query**: `label_values(up{job="oscal-tools-api"}, instance)`

### $uri
- **Type**: Query
- **Query**: `label_values(http_server_requests_seconds_count{job="oscal-tools-api"}, uri)`

### $status
- **Type**: Query
- **Query**: `label_values(http_server_requests_seconds_count{job="oscal-tools-api"}, status)`

Then use in queries like:
```promql
sum(rate(http_server_requests_seconds_count{job="oscal-tools-api",uri="$uri"}[5m]))
```

## Dashboard Organization

Recommended layout:

**Row 1 - Overview**:
- Service Status (Stat)
- Request Rate (Stat)
- Error Rate (Gauge)
- Response Time p95 (Stat)

**Row 2 - Traffic**:
- Request Rate Over Time (Time Series)
- Requests by Endpoint (Time Series)
- Requests by Status Code (Time Series)

**Row 3 - Performance**:
- Response Time (p50, p95, p99) (Time Series)
- Response Time by Endpoint (Heatmap)

**Row 4 - JVM**:
- Heap Memory Usage (Time Series)
- GC Pause Time (Time Series)
- Thread Count (Time Series)

**Row 5 - Database**:
- Active Connections (Time Series)
- Connection Pool Utilization (Gauge)

**Row 6 - System**:
- CPU Usage (Time Series)
- Disk Space (Gauge)

## Tips

1. **Set appropriate time ranges**: Last 15m, 1h, 6h, 24h
2. **Use refresh intervals**: Auto-refresh every 30s or 1m
3. **Add annotations**: Mark deployments, incidents
4. **Create alerts**: Use Grafana alerting or Prometheus Alertmanager
5. **Share dashboards**: Export JSON and commit to git

## Troubleshooting

**No data showing**:
1. Check Prometheus is scraping: http://localhost:9090/targets
2. Verify metrics endpoint: http://localhost:8080/actuator/prometheus
3. Check Grafana datasource: Configuration → Data Sources → Prometheus → Test

**Queries returning no results**:
1. Verify the job label matches: `job="oscal-tools-api"`
2. Check metric names in Prometheus: http://localhost:9090/graph
3. Adjust time range (data might be outside selected range)
