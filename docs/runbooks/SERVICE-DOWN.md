# Runbook: Service Down

**Alert**: ServiceDown
**Severity**: CRITICAL
**Threshold**: Service unavailable for > 1 minute

## Symptoms

- Health check endpoint returns failure or times out
- Users cannot access the application
- Prometheus shows `up{job="oscal-tools-api"} == 0`
- Load balancer marks service as unhealthy

## Impact

- **User Impact**: HIGH - Users cannot access the application
- **Business Impact**: CRITICAL - No operations can be performed
- **Data Impact**: None (unless service crashed during write operation)

## Diagnosis Steps

### 1. Verify Service Status

```bash
# Check if service is running
curl http://localhost:8080/actuator/health

# Check if port is listening
lsof -i :8080

# Check process status
ps aux | grep java | grep oscal
```

### 2. Check Recent Logs

```bash
# Check application logs
tail -100 back-end/logs/spring.log

# Check for errors
tail -200 back-end/logs/spring.log | grep -i error

# Check for Out of Memory errors
tail -200 back-end/logs/spring.log | grep -i "OutOfMemoryError"
```

### 3. Check System Resources

```bash
# Check available memory
free -h

# Check disk space
df -h

# Check CPU usage
top -n 1
```

### 4. Check Database Connectivity

```bash
# If using PostgreSQL in Docker
docker ps | grep postgres

# Test database connection
psql -h localhost -U oscal_user -d oscal_dev -c "SELECT 1"
```

## Common Causes & Solutions

### Cause 1: Application Crashed

**Symptoms**: No Java process running, heap dump files present

**Solution**:
```bash
# 1. Check for heap dumps
ls -lh *.hprof

# 2. Review crash logs
tail -100 back-end/logs/spring.log

# 3. Restart the application
cd back-end
mvn spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=prod"

# Or if using Docker
docker-compose restart oscal-ux
```

**Post-Resolution**:
- Analyze heap dump if OOM occurred
- Review logs for root cause
- Consider increasing JVM memory

### Cause 2: Port Already in Use

**Symptoms**: "Address already in use" error in logs

**Solution**:
```bash
# 1. Find process using port 8080
lsof -i :8080

# 2. Kill the process (if safe)
kill -9 <PID>

# 3. Or use a different port
SERVER_PORT=8081 mvn spring-boot:run
```

### Cause 3: Database Connection Failure

**Symptoms**: "Unable to acquire JDBC Connection" in logs

**Solution**:
```bash
# 1. Check database is running
docker ps | grep postgres

# 2. Start database if down
docker-compose up -d postgres

# 3. Wait for database to be healthy (30 seconds)
sleep 30

# 4. Restart application
docker-compose restart oscal-ux
```

**Reference**: See [DATABASE-CONNECTION-ISSUES.md](DATABASE-CONNECTION-ISSUES.md)

### Cause 4: Out of Memory (OOM)

**Symptoms**: "OutOfMemoryError" in logs, heap dump generated

**Solution**:
```bash
# 1. Increase JVM memory (temporary)
export JAVA_OPTS="-Xmx2g -Xms1g"
mvn spring-boot:run

# 2. Or in Docker
docker-compose up -d --force-recreate \
  -e JAVA_OPTS="-Xmx2g -Xms1g" oscal-ux
```

**Permanent Fix**: Update `docker-compose.yml`:
```yaml
environment:
  - JAVA_OPTS=-Xmx2g -Xms1g
```

**Reference**: See [HIGH-MEMORY-USAGE.md](HIGH-MEMORY-USAGE.md)

### Cause 5: Configuration Error

**Symptoms**: Application starts then immediately exits, configuration errors in logs

**Solution**:
```bash
# 1. Check configuration files
cat back-end/src/main/resources/application.properties
cat back-end/src/main/resources/application-prod.properties

# 2. Verify environment variables
env | grep -E '(DB_|JWT_|AZURE_)'

# 3. Check for missing required variables
# JWT_SECRET is required for production
echo $JWT_SECRET

# 4. Set missing variables and restart
export JWT_SECRET="your-secret-key-at-least-32-characters"
docker-compose restart oscal-ux
```

## Resolution Steps

### Quick Recovery (< 5 minutes)

```bash
# 1. Check if it's a simple restart issue
docker-compose restart oscal-ux

# 2. Wait for startup (60 seconds)
sleep 60

# 3. Verify health
curl http://localhost:8080/actuator/health

# 4. If still down, check logs
docker-compose logs --tail=100 oscal-ux
```

### Full Restart (If quick recovery fails)

```bash
# 1. Stop all services
docker-compose down

# 2. Check for stale processes
ps aux | grep java | grep oscal

# 3. Kill stale processes if any
pkill -f 'oscal'

# 4. Start services
docker-compose up -d

# 5. Monitor startup
docker-compose logs -f oscal-ux

# 6. Verify health (wait 60s for startup)
sleep 60
curl http://localhost:8080/actuator/health
```

## Verification

After resolving, verify:

```bash
# 1. Health check passes
curl http://localhost:8080/actuator/health | jq '.status'
# Should return: "UP"

# 2. Application responds to API requests
curl http://localhost:8080/api/health

# 3. Check Prometheus
# Go to http://localhost:9090/targets
# Verify oscal-tools-api is UP

# 4. Check metrics are being collected
curl -H "Authorization: Bearer YOUR_TOKEN" \
     http://localhost:8080/actuator/prometheus | head -20
```

## Prevention

### Monitoring
- Set up uptime monitoring (UptimeRobot, Pingdom)
- Configure alerting on service down (already done via Prometheus)
- Monitor startup time trends

### Capacity Planning
- Review memory usage trends weekly
- Set memory alerts at 85% threshold
- Plan capacity increases proactively

### Deployment
- Use health checks in deployment pipelines
- Implement blue-green deployments
- Test configuration changes in staging first

## Escalation

If service cannot be restored within **15 minutes**:

1. **Notify**: Alert on-call engineer
2. **Communicate**: Update status page
3. **Escalate**: Contact senior engineer/architect
4. **Document**: Take notes of all actions taken

## Post-Incident

After resolution:

1. **Document**: Create incident report
2. **Analyze**: Review logs and identify root cause
3. **Improve**: Implement preventive measures
4. **Test**: Verify similar issues won't recur
5. **Update**: Update this runbook if needed

## Related Runbooks

- [DATABASE-CONNECTION-ISSUES.md](DATABASE-CONNECTION-ISSUES.md)
- [HIGH-MEMORY-USAGE.md](HIGH-MEMORY-USAGE.md)
- [DEPLOYMENT-PROCEDURE.md](DEPLOYMENT-PROCEDURE.md)

## References

- Health Check Endpoint: http://localhost:8080/actuator/health
- Prometheus: http://localhost:9090
- Grafana: http://localhost:3001
- Application Logs: `back-end/logs/spring.log`
- Docker Logs: `docker-compose logs oscal-ux`

---

**Last Updated**: October 26, 2025
**Owner**: Platform Team
**Reviewers**: DevOps, SRE
