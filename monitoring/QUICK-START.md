# Monitoring Stack Quick Start Guide

## Overview

This guide will help you set up the complete monitoring stack for OSCAL Tools:
- **Prometheus**: Metrics collection and storage
- **Grafana**: Metrics visualization and dashboards
- **Actuator**: Spring Boot health checks and metrics
- **Alerting**: Prometheus alert rules

**Time to setup**: 10-15 minutes

## Prerequisites

- Docker and Docker Compose installed
- OSCAL Tools application running or ready to start
- Port 9090 (Prometheus) and 3001 (Grafana) available

## Step-by-Step Setup

### Step 1: Temporarily Make Prometheus Endpoint Public (Dev Only)

Since Prometheus needs to scrape metrics, we need to temporarily make the endpoint accessible without authentication in development.

**Option A**: Update SecurityConfig (Temporary - Dev Only)

Edit `back-end/src/main/java/gov/nist/oscal/tools/api/config/SecurityConfig.java`:

```java
.requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
.requestMatchers("/actuator/info").permitAll()
.requestMatchers("/actuator/prometheus").permitAll()  // ADD THIS LINE (dev only!)
.requestMatchers("/actuator/**").authenticated()
```

**Option B**: Use Prometheus with Bearer Token

Later, configure Prometheus with a long-lived JWT token (see Advanced Setup below).

### Step 2: Start the Monitoring Stack

```bash
# From project root
cd /Users/travishowerton/Documents/GitHub/oscal-cli

# Start Prometheus and Grafana
docker-compose -f docker-compose-monitoring.yml up -d

# Verify they're running
docker-compose -f docker-compose-monitoring.yml ps
```

**Expected output**:
```
NAME                STATUS    PORTS
oscal-prometheus    running   0.0.0.0:9090->9090/tcp
oscal-grafana       running   0.0.0.0:3001->3000/tcp
```

### Step 3: Start the OSCAL Tools Application

```bash
# If not already running
cd back-end
mvn spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=dev"
```

Wait for the application to start (~60 seconds). You should see:
```
Started OscalCliApiApplication in X seconds
```

### Step 4: Verify Metrics are Being Collected

**Test actuator endpoint**:
```bash
curl http://localhost:8080/actuator/prometheus | head -20
```

You should see Prometheus-formatted metrics like:
```
# HELP jvm_memory_used_bytes The amount of used memory
# TYPE jvm_memory_used_bytes gauge
jvm_memory_used_bytes{area="heap",id="G1 Eden Space",} 2.1233664E7
...
```

**Check Prometheus is scraping**:
1. Open http://localhost:9090
2. Go to **Status → Targets**
3. Look for `oscal-tools-api`
4. Status should be **UP** (green)

If it shows **DOWN**:
- Verify the application is running
- Check the error message (usually connection refused or timeout)
- Verify Step 1 was completed

### Step 5: Access Grafana

1. Open http://localhost:3001
2. Login with:
   - **Username**: `admin`
   - **Password**: `admin`
3. You'll be prompted to change the password (optional in dev)

### Step 6: Import a Dashboard

**Import JVM Micrometer Dashboard**:
1. Click `+` → `Import`
2. Enter Dashboard ID: `4701`
3. Click **Load**
4. Select **Prometheus** as datasource
5. Click **Import**

You should now see metrics! If the dashboard shows "No Data":
- Wait 1-2 minutes for metrics to accumulate
- Adjust time range (top right) to "Last 15 minutes"
- Verify Prometheus has data: http://localhost:9090/graph

**Import Spring Boot Statistics Dashboard**:
1. Repeat above with Dashboard ID: `6756`

### Step 7: Explore Your Metrics

**Try these queries in Prometheus** (http://localhost:9090/graph):

```promql
# Request rate
sum(rate(http_server_requests_seconds_count[5m]))

# Memory usage
jvm_memory_used_bytes{area="heap"}

# Active database connections
jdbc_connections_active
```

**In Grafana**, create a simple panel:
1. Click `+` → `Dashboard` → `Add new panel`
2. Enter query: `sum(rate(http_server_requests_seconds_count[5m]))`
3. Set panel title: "Request Rate"
4. Click **Apply**

## Verification Checklist

- [ ] Prometheus UI accessible at http://localhost:9090
- [ ] Grafana UI accessible at http://localhost:3001
- [ ] oscal-tools-api target is UP in Prometheus
- [ ] Dashboards show data (may take 1-2 minutes)
- [ ] Alerts tab shows configured rules

## Next Steps

### 1. Set Up Alerting (Optional but Recommended)

Alerts are already configured but need a notification channel.

**Add Email Notifications** (example):
1. In Grafana, go to **Alerting → Contact points**
2. Click **New contact point**
3. Name: "Email"
4. Type: Email
5. Add email addresses
6. Test and Save

**Configure Alert Rules**:
1. Go to **Alerting → Alert rules**
2. Click **New alert rule**
3. Name: "High Error Rate"
4. Query:
   ```promql
   sum(rate(http_server_requests_seconds_count{status=~"5.."}[5m])) /
   sum(rate(http_server_requests_seconds_count[5m])) > 0.10
   ```
5. Set threshold and contact point
6. Save

### 2. Create Custom Dashboards

See `monitoring/grafana/dashboards/README.md` for custom queries and panel ideas.

### 3. Set Up Automated Backups

```bash
# Test backup
./scripts/backup/backup-database.sh

# Schedule daily backups (add to crontab)
crontab -e

# Add this line (runs daily at 2 AM):
0 2 * * * /path/to/oscal-cli/scripts/backup/backup-database.sh
```

### 4. Review Runbooks

Familiarize yourself with operational procedures:
- `docs/runbooks/SERVICE-DOWN.md`
- `docs/runbooks/DATABASE-CONNECTION-ISSUES.md`
- `docs/runbooks/README.md`

## Troubleshooting

### Prometheus Can't Scrape Metrics

**Problem**: oscal-tools-api shows DOWN in Prometheus

**Solutions**:
1. Verify application is running: `curl http://localhost:8080/actuator/health`
2. Check metrics endpoint: `curl http://localhost:8080/actuator/prometheus`
3. If using Docker Desktop on Mac/Windows:
   - Target should be `host.docker.internal:8080`
   - This is already configured in `prometheus.yml`
4. If on Linux, update `prometheus/prometheus.yml`:
   ```yaml
   targets: ['172.17.0.1:8080']  # Docker bridge IP
   ```

### Grafana Shows "No Data"

**Problem**: Dashboards are empty

**Solutions**:
1. Wait 2-3 minutes for metrics to accumulate
2. Check Prometheus has data: http://localhost:9090/graph
3. Run a test query: `up`
4. Verify datasource is configured: **Configuration → Data Sources → Prometheus**
5. Test datasource connection (should be green)

### Can't Access Grafana

**Problem**: Connection refused to http://localhost:3001

**Solutions**:
```bash
# Check if Grafana is running
docker-compose -f docker-compose-monitoring.yml ps grafana

# Check logs
docker-compose -f docker-compose-monitoring.yml logs grafana

# Restart if needed
docker-compose -f docker-compose-monitoring.yml restart grafana
```

### Permission Denied on Backup Script

```bash
chmod +x scripts/backup/backup-database.sh
chmod +x scripts/backup/restore-database.sh
```

## Advanced Setup

### 1. Configure Prometheus Authentication

Create a long-lived service account token:

```bash
# In a separate terminal, get a token
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"prometheus","password":"your-password"}' | jq -r '.token')

echo "Bearer token: $TOKEN"
```

Update `monitoring/prometheus/prometheus.yml`:
```yaml
scrape_configs:
  - job_name: 'oscal-tools-api'
    bearer_token: 'YOUR_TOKEN_HERE'
    # ... rest of config
```

Restart Prometheus:
```bash
docker-compose -f docker-compose-monitoring.yml restart prometheus
```

### 2. Enable Alertmanager

Uncomment the alertmanager section in `docker-compose-monitoring.yml`:

```bash
# Create alertmanager config
mkdir -p monitoring/alertmanager
cat > monitoring/alertmanager/alertmanager.yml <<EOF
global:
  resolve_timeout: 5m

route:
  group_by: ['alertname']
  receiver: 'email'

receivers:
  - name: 'email'
    email_configs:
      - to: 'your-email@example.com'
        from: 'alerts@oscal-tools.com'
        smarthost: 'smtp.gmail.com:587'
        auth_username: 'your-email@gmail.com'
        auth_password: 'your-app-password'
EOF

# Start alertmanager
docker-compose -f docker-compose-monitoring.yml up -d alertmanager
```

### 3. Production Deployment

For production:
1. Use proper authentication (remove permitAll for /actuator/prometheus)
2. Configure HTTPS/TLS for Grafana
3. Use strong passwords (not admin/admin)
4. Enable Grafana anonymous access = false
5. Set up external Prometheus storage for long-term retention
6. Configure backup automation with cloud storage

## Monitoring Stack Management

### Start/Stop

```bash
# Start
docker-compose -f docker-compose-monitoring.yml up -d

# Stop
docker-compose -f docker-compose-monitoring.yml down

# View logs
docker-compose -f docker-compose-monitoring.yml logs -f

# Restart specific service
docker-compose -f docker-compose-monitoring.yml restart prometheus
```

### Resource Usage

Check resource consumption:
```bash
docker stats oscal-prometheus oscal-grafana
```

### Data Persistence

Metrics and dashboards are persisted in Docker volumes:
```bash
# List volumes
docker volume ls | grep -E '(prometheus|grafana)'

# Backup volumes (important!)
docker run --rm -v prometheus_data:/source \
  -v $(pwd)/backups:/backup \
  alpine tar czf /backup/prometheus-backup.tar.gz -C /source .
```

## Resources

- **Prometheus Documentation**: https://prometheus.io/docs/
- **Grafana Documentation**: https://grafana.com/docs/
- **Spring Boot Actuator**: https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html
- **Monitoring Guide**: `docs/MONITORING-GUIDE.md`
- **Runbooks**: `docs/runbooks/`

## Support

For issues:
1. Check logs: `docker-compose -f docker-compose-monitoring.yml logs`
2. Review runbooks: `docs/runbooks/README.md`
3. Check application health: `curl http://localhost:8080/actuator/health`

---

**Success!** You now have a fully functional monitoring stack for OSCAL Tools! 🎉

Next recommended steps:
1. Import dashboards (IDs: 4701, 6756)
2. Set up email alerting
3. Schedule daily database backups
4. Review operational runbooks
