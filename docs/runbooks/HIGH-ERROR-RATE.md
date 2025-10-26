# Runbook: High Error Rate

**Alert**: HighErrorRate
**Severity**: WARNING → CRITICAL (if sustained)
**Threshold**: >10% of requests returning 5xx errors for 5+ minutes

## Symptoms

- Grafana alert showing error rate >10%
- Users reporting frequent "500 Internal Server Error" responses
- Error spikes visible in monitoring dashboards
- Application logs showing repeated exceptions
- Increased support tickets about failed operations

## Impact

- **User Impact**: MEDIUM-HIGH - Users unable to complete operations
- **Business Impact**: HIGH - Reduced productivity, potential data loss
- **Data Impact**: MEDIUM - Some operations may fail mid-transaction
- **Reputation Impact**: HIGH - Poor user experience, loss of trust

## Diagnosis Steps

### 1. Check Current Error Rate

```bash
# Check error rate in Grafana
# Navigate to: http://localhost:3001/d/oscal-tools-overview
# Look at "Error Rate (%)" panel

# Or query Prometheus directly
curl -s 'http://localhost:9090/api/v1/query?query=sum(rate(http_server_requests_seconds_count{status=~"5..",job="oscal-tools-api"}[5m]))/sum(rate(http_server_requests_seconds_count{job="oscal-tools-api"}[5m]))*100' | jq '.data.result[0].value[1]'
```

### 2. Identify Which Endpoints Are Failing

```bash
# Check errors by endpoint
curl -s 'http://localhost:9090/api/v1/query?query=sum(increase(http_server_requests_seconds_count{status=~"5..",job="oscal-tools-api"}[10m]))by(uri)' | jq '.data.result[] | {endpoint: .metric.uri, errors: .value[1]}'
```

### 3. Check Application Logs

```bash
# View recent errors
tail -200 back-end/logs/spring.log | grep -i "error\|exception"

# Or with Docker
docker-compose logs --tail=200 oscal-ux | grep -i "error\|exception"

# Filter for specific exceptions
tail -500 back-end/logs/spring.log | grep -A 10 "Exception"
```

### 4. Check Error Breakdown by Status Code

```bash
# See which status codes are most common
curl -s 'http://localhost:9090/api/v1/query?query=sum(increase(http_server_requests_seconds_count{status=~"5..",job="oscal-tools-api"}[10m]))by(status)' | jq
```

### 5. Check System Resources

```bash
# Check if resource constraints are causing errors
curl -H "Authorization: Bearer YOUR_TOKEN" \
     http://localhost:8080/actuator/metrics/jvm.memory.used | jq

# Check database connections
curl -H "Authorization: Bearer YOUR_TOKEN" \
     http://localhost:8080/actuator/metrics/hikaricp.connections.active | jq
```

## Common Causes & Solutions

### Cause 1: Unhandled Application Exceptions

**Symptoms**: Specific error types repeated in logs (NullPointerException, IllegalArgumentException, etc.)

**Diagnosis**:
```bash
# Identify most common exceptions
tail -1000 back-end/logs/spring.log | grep "Exception" | cut -d: -f1 | sort | uniq -c | sort -rn | head -10
```

**Solution**:
```bash
# 1. Analyze stack traces
tail -500 back-end/logs/spring.log | grep -A 20 "NullPointerException"

# 2. If it's a code bug, apply hotfix and redeploy
# 3. If it's data validation, add better input validation
# 4. Add defensive null checks

# 4. Monitor after fix
watch -n 5 'curl -s http://localhost:9090/api/v1/query?query=sum(rate(http_server_requests_seconds_count{status=~\"5..\",job=\"oscal-tools-api\"}[5m]))/sum(rate(http_server_requests_seconds_count{job=\"oscal-tools-api\"}[5m]))*100 | jq -r ".data.result[0].value[1]"'
```

### Cause 2: Database Connection Issues

**Symptoms**: Logs show "Could not get JDBC Connection" or "Connection pool exhausted"

**Diagnosis**:
```bash
# Check database connectivity
docker-compose exec postgres pg_isready -U oscal_user

# Check active connections
curl -H "Authorization: Bearer YOUR_TOKEN" \
     http://localhost:8080/actuator/metrics/hikaricp.connections.active | jq
```

**Solution**:
```bash
# Follow database connection runbook
# See: docs/runbooks/DATABASE-CONNECTION-ISSUES.md

# Quick fix: Restart database and application
docker-compose restart postgres
sleep 10
docker-compose restart oscal-ux
```

### Cause 3: Out of Memory (OOM)

**Symptoms**: Logs show "OutOfMemoryError" or "GC overhead limit exceeded"

**Diagnosis**:
```bash
# Check heap usage
curl -H "Authorization: Bearer YOUR_TOKEN" \
     http://localhost:8080/actuator/metrics/jvm.memory.used | jq '.measurements[] | select(.statistic=="VALUE") | .value'

curl -H "Authorization: Bearer YOUR_TOKEN" \
     http://localhost:8080/actuator/metrics/jvm.memory.max | jq '.measurements[] | select(.statistic=="VALUE") | .value'
```

**Solution**:
```bash
# Follow high memory usage runbook
# See: docs/runbooks/HIGH-MEMORY-USAGE.md

# Quick fix: Restart application with more memory
docker-compose stop oscal-ux
docker-compose run -e JAVA_OPTS="-Xmx2g -Xms1g" oscal-ux
```

### Cause 4: External Service Failure

**Symptoms**: Errors correlate with calls to external APIs or Azure Blob Storage

**Diagnosis**:
```bash
# Check Azure Blob Storage health
curl -H "Authorization: Bearer YOUR_TOKEN" \
     http://localhost:8080/actuator/health | jq '.components.azureBlobStorage'

# Check logs for external service errors
tail -200 back-end/logs/spring.log | grep -i "azure\|blob\|timeout\|refused"
```

**Solution**:
```bash
# 1. Verify external service is accessible
# 2. Check network connectivity
# 3. Verify credentials/tokens are valid
# 4. Implement circuit breaker if not present
# 5. Add retry logic with exponential backoff

# Temporary: Disable external service if fallback available
# Update application.properties to use local storage
```

### Cause 5: Bad Deployment / Code Regression

**Symptoms**: Error rate spiked immediately after deployment

**Diagnosis**:
```bash
# Check when deployment occurred
docker-compose ps oscal-ux

# Compare error rate before/after deployment time
# Use Grafana time range selector

# Check git commit for recent changes
cd back-end
git log -10 --oneline
git diff HEAD~1
```

**Solution**:
```bash
# Rollback to previous version
# See: docs/runbooks/ROLLBACK-PROCEDURE.md

# Quick rollback with Docker
docker-compose down
git checkout <previous-commit>
mvn clean install -DskipTests
docker-compose up -d --build
```

### Cause 6: Invalid Input Data

**Symptoms**: Errors on specific endpoints that process user uploads (validation, conversion)

**Diagnosis**:
```bash
# Check which validation endpoints are failing
curl -s 'http://localhost:9090/api/v1/query?query=sum(increase(http_server_requests_seconds_count{uri=~"/api/validation.*",status=~"5..",job="oscal-tools-api"}[10m]))by(uri)' | jq

# Look for validation errors in logs
tail -200 back-end/logs/spring.log | grep -i "validation\|parse\|invalid"
```

**Solution**:
```bash
# 1. Improve input validation
# 2. Add better error handling and user-friendly messages
# 3. Return 4xx instead of 5xx for invalid input
# 4. Add request size limits
# 5. Implement file type validation

# Quick fix: Add input sanitization
# Update ValidationService.java to catch and handle parse exceptions
```

## Resolution Steps

### Quick Recovery (for temporary issues)

```bash
# 1. Restart the application
docker-compose restart oscal-ux

# 2. Monitor error rate
watch -n 10 'curl -s http://localhost:8080/actuator/health | jq ".status"'

# 3. Check if error rate is decreasing
# Open: http://localhost:3001/d/oscal-tools-overview
# Watch "Error Rate (%)" panel
```

### Full Investigation (for persistent issues)

```bash
# 1. Collect diagnostic data
mkdir -p /tmp/oscal-diagnostics
tail -1000 back-end/logs/spring.log > /tmp/oscal-diagnostics/app.log
curl http://localhost:8080/actuator/health > /tmp/oscal-diagnostics/health.json
curl http://localhost:8080/actuator/metrics > /tmp/oscal-diagnostics/metrics.json
docker-compose logs --tail=1000 > /tmp/oscal-diagnostics/docker.log

# 2. Analyze exceptions
grep -i "exception" /tmp/oscal-diagnostics/app.log | cut -d: -f1 | sort | uniq -c | sort -rn

# 3. Identify patterns
# - Same endpoint failing repeatedly?
# - Same error type?
# - Correlated with specific user actions?
# - Time-based pattern (business hours, scheduled jobs)?

# 4. Apply targeted fix based on analysis

# 5. Deploy fix and monitor
```

## Verification

```bash
# 1. Error rate is back to normal (<1%)
curl -s 'http://localhost:9090/api/v1/query?query=sum(rate(http_server_requests_seconds_count{status=~"5..",job="oscal-tools-api"}[5m]))/sum(rate(http_server_requests_seconds_count{job="oscal-tools-api"}[5m]))*100' | jq -r '.data.result[0].value[1]'
# Expected: < 1.0

# 2. All endpoints returning success
curl -s 'http://localhost:9090/api/v1/query?query=sum(increase(http_server_requests_seconds_count{status=~"5..",job="oscal-tools-api"}[10m]))by(uri)' | jq
# Expected: No results or very low numbers

# 3. Application health is UP
curl http://localhost:8080/actuator/health | jq '.status'
# Expected: "UP"

# 4. No recent exceptions in logs
tail -100 back-end/logs/spring.log | grep -i "exception"
# Expected: No new exceptions

# 5. Test critical endpoints
curl -X POST http://localhost:8080/api/auth/login \
     -H "Content-Type: application/json" \
     -d '{"username":"test","password":"test"}'
# Should work (or return expected auth error)
```

## Prevention

### Code Quality

- **Comprehensive error handling** - Catch and handle all exceptions
- **Input validation** - Validate all user inputs at API boundary
- **Defensive programming** - Null checks, bounds checking
- **Return appropriate status codes** - 4xx for client errors, 5xx only for server errors
- **Code reviews** - Peer review all changes
- **Static analysis** - Use SpotBugs, SonarQube

### Testing

- **Unit tests** - Cover edge cases and error conditions
- **Integration tests** - Test full request/response cycles
- **Load testing** - Identify breaking points before production
- **Chaos engineering** - Test failure scenarios

### Monitoring

- **Alert on error rate >5%** - Earlier warning
- **Track errors by endpoint** - Identify problematic endpoints
- **Monitor error trends** - Detect gradual increases
- **Log aggregation** - Centralize logs for analysis
- **Error tracking service** - Sentry, Rollbar, etc.

### Deployment

- **Canary deployments** - Test new code with small % of traffic
- **Feature flags** - Quick rollback without deployment
- **Automated rollback** - Auto-rollback on high error rate
- **Health checks** - Verify application health before routing traffic

## Escalation

**Immediate actions** (0-5 minutes):
1. Acknowledge alert
2. Check if error rate is increasing or stable
3. Identify affected endpoints

**If error rate >25%** (5-10 minutes):
1. Consider taking service offline (maintenance mode)
2. Notify users via status page
3. Escalate to senior engineer

**If error rate >50%** (10-15 minutes):
1. Roll back to last known good version
2. Escalate to platform lead
3. Start incident response process

**If unable to resolve within 30 minutes**:
1. Engage additional team members
2. Consider data recovery procedures
3. Communicate ETA to stakeholders

## Post-Incident

### Immediate (within 24 hours)

1. **Document timeline** - When did it start? When was it detected? When was it fixed?
2. **Document root cause** - What caused the error rate spike?
3. **Document fix** - What was done to resolve it?
4. **Verify metrics** - Confirm error rate is back to normal

### Follow-up (within 1 week)

1. **Write post-mortem** - Full incident analysis
2. **Identify prevention measures** - How to prevent recurrence
3. **Update monitoring** - Add alerts to detect earlier
4. **Code improvements** - Fix underlying issues
5. **Test failure scenario** - Ensure fix works

### Long-term (ongoing)

1. **Track error rates over time** - Trend analysis
2. **Review error logs weekly** - Proactive issue detection
3. **Update runbook** - Add new learnings
4. **Share learnings** - Team knowledge sharing
5. **Automate remediation** - Auto-restart, auto-scale, etc.

## Related Runbooks

- [SERVICE-DOWN.md](SERVICE-DOWN.md) - If error rate reaches 100%
- [DATABASE-CONNECTION-ISSUES.md](DATABASE-CONNECTION-ISSUES.md) - For database-related errors
- [HIGH-MEMORY-USAGE.md](HIGH-MEMORY-USAGE.md) - For OOM errors
- [HIGH-RESPONSE-TIME.md](HIGH-RESPONSE-TIME.md) - Often correlates with errors
- [ROLLBACK-PROCEDURE.md](ROLLBACK-PROCEDURE.md) - For deployment-related errors

## Additional Resources

- **Application Logs**: `back-end/logs/spring.log`
- **Grafana Dashboard**: http://localhost:3001/d/oscal-tools-overview
- **Prometheus Metrics**: http://localhost:9090
- **Error Tracking**: (Add Sentry/Rollbar URL when configured)

---

**Last Updated**: October 26, 2025
**Tested**: No (requires live incident)
**Review Cycle**: After each high error rate incident
