# Runbook: High Response Time

**Alert**: HighResponseTime
**Severity**: WARNING → CRITICAL
**Threshold**: p95 response time >2 seconds for 5+ minutes

## Symptoms

- Grafana alert showing high response times
- Users reporting slow application performance
- Requests taking noticeably longer than usual
- Timeouts in client applications
- Increased number of concurrent requests
- Database queries running slow

## Impact

- **User Impact**: MEDIUM-HIGH - Poor user experience, frustration
- **Business Impact**: MEDIUM - Reduced productivity, potential user churn
- **System Impact**: MEDIUM - May cascade to other issues (memory, connections)
- **SLA Impact**: HIGH - Violates response time SLAs

## Diagnosis Steps

### 1. Check Current Response Times

```bash
# Check p95 response time
curl -s 'http://localhost:9090/api/v1/query?query=histogram_quantile(0.95,sum(rate(http_server_requests_seconds_bucket{job="oscal-tools-api"}[5m]))by(le))*1000' | jq -r '.data.result[0].value[1]'
# Expected: < 2000 ms

# Check response time by endpoint
curl -s 'http://localhost:9090/api/v1/query?query=histogram_quantile(0.95,sum(rate(http_server_requests_seconds_bucket{uri=~"/api/.*",job="oscal-tools-api"}[5m]))by(uri,le))*1000' | jq -r '.data.result[] | "\(.metric.uri): \(.value[1])ms"'

# Or check in Grafana
# Navigate to: http://localhost:3001/d/oscal-tools-overview
# Look at "Response Time p95 (ms)" and "Performance - Response Time by Endpoint" panels
```

### 2. Identify Slow Endpoints

```bash
# Get slowest endpoints
curl -s 'http://localhost:9090/api/v1/query?query=topk(10,sum(rate(http_server_requests_seconds_sum{uri=~"/api/.*",job="oscal-tools-api"}[5m]))by(uri)/sum(rate(http_server_requests_seconds_count{uri=~"/api/.*",job="oscal-tools-api"}[5m]))by(uri)*1000)' | jq -r '.data.result[] | "\(.metric.uri): \(.value[1])ms"'

# Check in Grafana table
# Look at "Slowest Endpoints" panel
```

### 3. Check Database Performance

```bash
# Check database connection pool
curl -H "Authorization: Bearer YOUR_TOKEN" \
     http://localhost:8080/actuator/metrics/hikaricp.connections.active | jq

curl -H "Authorization: Bearer YOUR_TOKEN" \
     http://localhost:8080/actuator/metrics/hikaricp.connections.pending | jq

# Check database query times (if available)
# Look for slow query logs in PostgreSQL
docker-compose exec postgres psql -U oscal_user -d oscal_dev -c \
  "SELECT query, mean_exec_time, calls FROM pg_stat_statements ORDER BY mean_exec_time DESC LIMIT 10;"
```

### 4. Check System Resources

```bash
# Check CPU usage
docker stats oscal-ux --no-stream

# Check memory usage
curl -H "Authorization: Bearer YOUR_TOKEN" \
     http://localhost:8080/actuator/metrics/jvm.memory.used | jq

# Check GC activity (can cause pauses)
curl -H "Authorization: Bearer YOUR_TOKEN" \
     http://localhost:8080/actuator/metrics/jvm.gc.pause | jq
```

### 5. Check Concurrent Load

```bash
# Check current request rate
curl -s 'http://localhost:9090/api/v1/query?query=sum(rate(http_server_requests_seconds_count{job="oscal-tools-api"}[5m]))*60' | jq -r '.data.result[0].value[1]'

# Check active threads
curl -H "Authorization: Bearer YOUR_TOKEN" \
     http://localhost:8080/actuator/metrics/jvm.threads.live | jq

# Check if thread pool is saturated
curl -H "Authorization: Bearer YOUR_TOKEN" \
     http://localhost:8080/actuator/metrics/executor.active | jq
```

### 6. Check Application Logs

```bash
# Look for slow operations
tail -500 back-end/logs/spring.log | grep -i "slow\|timeout\|took.*ms"

# Check for database timeouts
tail -500 back-end/logs/spring.log | grep -i "timeout\|connection.*timeout"

# Look for external service delays
tail -500 back-end/logs/spring.log | grep -i "azure\|blob\|external"
```

## Common Causes & Solutions

### Cause 1: Database Query Performance

**Symptoms**: Slow endpoints are those with heavy database operations, high connection pool usage

**Diagnosis**:
```bash
# Enable slow query logging in PostgreSQL
docker-compose exec postgres psql -U oscal_user -d oscal_dev -c \
  "ALTER DATABASE oscal_dev SET log_min_duration_statement = 1000;"  # Log queries >1s

# Check slow queries
docker-compose logs postgres | grep "duration:" | grep -v "duration: 0\." | tail -20

# Check for missing indexes
docker-compose exec postgres psql -U oscal_user -d oscal_dev -c \
  "SELECT schemaname, tablename, indexname FROM pg_indexes WHERE schemaname = 'public';"

# Check table sizes
docker-compose exec postgres psql -U oscal_user -d oscal_dev -c \
  "SELECT tablename, pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename)) AS size FROM pg_tables WHERE schemaname = 'public' ORDER BY pg_total_relation_size(schemaname||'.'||tablename) DESC;"
```

**Solution**:
```bash
# 1. Add missing indexes
# Identify columns used in WHERE, JOIN, ORDER BY clauses
docker-compose exec postgres psql -U oscal_user -d oscal_dev <<'EOF'
-- Example: Add index on frequently queried column
CREATE INDEX idx_operations_user_id ON operations(user_id);
CREATE INDEX idx_operations_created_at ON operations(created_at);
EOF

# 2. Optimize slow queries
# Use EXPLAIN ANALYZE to understand query plans
docker-compose exec postgres psql -U oscal_user -d oscal_dev -c \
  "EXPLAIN ANALYZE SELECT * FROM operations WHERE user_id = 1 ORDER BY created_at DESC LIMIT 10;"

# 3. Add database query caching
# Update service classes to cache frequent queries

# 4. Use database connection pooling efficiently
# Tune HikariCP settings in application.properties

# 5. Implement pagination
# Avoid loading large result sets
```

### Cause 2: Garbage Collection Pauses

**Symptoms**: Response time spikes correlate with GC activity, high memory usage

**Diagnosis**:
```bash
# Check GC pause times
curl -H "Authorization: Bearer YOUR_TOKEN" \
     http://localhost:8080/actuator/metrics/jvm.gc.pause | jq

# Check GC frequency
curl -H "Authorization: Bearer YOUR_TOKEN" \
     http://localhost:8080/actuator/metrics/jvm.gc.pause | jq '.measurements[] | select(.statistic=="COUNT")'

# Check memory pressure
curl -H "Authorization: Bearer YOUR_TOKEN" \
     http://localhost:8080/actuator/metrics/jvm.memory.used | jq
```

**Solution**:
```bash
# See HIGH-MEMORY-USAGE.md runbook for detailed steps

# Quick fixes:
# 1. Increase heap size
docker-compose stop oscal-ux
docker-compose run -e JAVA_OPTS="-Xmx4g -Xms2g" oscal-ux

# 2. Tune GC settings
# Add to JAVA_OPTS: -XX:+UseG1GC -XX:MaxGCPauseMillis=200

# 3. Fix memory leaks (long-term)
```

### Cause 3: Expensive Operations (Validation, Conversion)

**Symptoms**: Specific endpoints like /api/validation or /api/conversion are slow

**Diagnosis**:
```bash
# Check response times for specific endpoints
curl -s 'http://localhost:9090/api/v1/query?query=histogram_quantile(0.95,sum(rate(http_server_requests_seconds_bucket{uri="/api/validation/ssp",job="oscal-tools-api"}[5m]))by(le))*1000' | jq -r '.data.result[0].value[1]'

# Check if related to file size
tail -200 back-end/logs/spring.log | grep -i "processing.*size\|validation.*size"
```

**Solution**:
```bash
# 1. Implement async processing
# Move expensive operations to background jobs

# Example: Update ValidationController.java
@PostMapping("/validate/{type}")
@Async
public CompletableFuture<ResponseEntity<ValidationResult>> validateAsync(@PathVariable String type, @RequestBody ValidationRequest request) {
    return CompletableFuture.supplyAsync(() -> {
        ValidationResult result = validationService.validate(request);
        return ResponseEntity.ok(result);
    });
}

# 2. Add request/response caching
# Cache validation results for identical content

# 3. Optimize validation logic
# Profile the code to find bottlenecks

# 4. Implement timeout limits
# Prevent runaway operations
@Transactional(timeout = 30)  // 30 second timeout

# 5. Add result caching
# Cache validation results by content hash

mvn clean install
docker-compose restart oscal-ux
```

### Cause 4: External Service Latency

**Symptoms**: Slow operations correlate with external API calls (Azure Blob Storage, etc.)

**Diagnosis**:
```bash
# Check Azure Blob health
curl -H "Authorization: Bearer YOUR_TOKEN" \
     http://localhost:8080/actuator/health | jq '.components.azureBlobStorage'

# Check for timeout errors
tail -200 back-end/logs/spring.log | grep -i "azure\|timeout\|connection.*reset"
```

**Solution**:
```bash
# 1. Implement timeouts
# Add connection and read timeouts to external clients

# Example: Update AzureBlobService.java
BlobServiceClient client = new BlobServiceClientBuilder()
    .connectionString(connectionString)
    .httpClient(new NettyAsyncHttpClientBuilder()
        .connectTimeout(Duration.ofSeconds(5))
        .readTimeout(Duration.ofSeconds(30))
        .build())
    .buildClient();

# 2. Add retry logic with exponential backoff
@Retryable(
    value = {IOException.class},
    maxAttempts = 3,
    backoff = @Backoff(delay = 1000, multiplier = 2)
)

# 3. Implement circuit breaker
# Use Resilience4j to prevent cascading failures

# 4. Add caching layer
# Cache frequently accessed blob metadata

# 5. Make external calls asynchronous
# Don't block request threads waiting for external services

mvn clean install
docker-compose restart oscal-ux
```

### Cause 5: Thread Pool Saturation

**Symptoms**: High thread count, requests queueing, thread pool at max capacity

**Diagnosis**:
```bash
# Check active threads
curl -H "Authorization: Bearer YOUR_TOKEN" \
     http://localhost:8080/actuator/metrics/jvm.threads.live | jq

# Check thread pool metrics (if using @Async)
curl -H "Authorization: Bearer YOUR_TOKEN" \
     http://localhost:8080/actuator/metrics/executor.active | jq

curl -H "Authorization: Bearer YOUR_TOKEN" \
     http://localhost:8080/actuator/metrics/executor.queue.remaining | jq
```

**Solution**:
```bash
# 1. Increase thread pool size
# Update application.properties:
cat >> back-end/src/main/resources/application.properties <<'EOF'
# Tomcat thread pool
server.tomcat.threads.max=200
server.tomcat.threads.min-spare=20

# Async executor pool (if using @Async)
spring.task.execution.pool.core-size=10
spring.task.execution.pool.max-size=50
spring.task.execution.pool.queue-capacity=1000
EOF

# 2. Optimize thread usage
# Use async/reactive programming where appropriate
# Don't block threads unnecessarily

# 3. Implement request throttling
# Limit concurrent requests per user/IP

# 4. Scale horizontally
# Add more application instances

mvn clean install
docker-compose restart oscal-ux
```

### Cause 6: Network/Infrastructure Issues

**Symptoms**: All endpoints slow, not just specific ones, Docker network issues

**Diagnosis**:
```bash
# Check container network performance
docker exec oscal-ux ping -c 5 postgres
docker exec oscal-ux ping -c 5 host.docker.internal

# Check Docker network
docker network inspect oscal-network

# Check host resources
top
df -h
iostat
```

**Solution**:
```bash
# 1. Restart Docker networking
docker network prune
docker-compose down
docker-compose up -d

# 2. Check host resources
# Ensure sufficient CPU, RAM, disk I/O

# 3. Optimize Docker configuration
# Update docker-compose.yml with resource limits

# 4. Check for DNS issues
# Verify service name resolution

# 5. Consider host networking for production
# network_mode: "host" (Linux only)
```

## Resolution Steps

### Immediate Actions (0-5 minutes)

```bash
# 1. Identify which endpoints are slow
curl -s 'http://localhost:9090/api/v1/query?query=topk(5,sum(rate(http_server_requests_seconds_sum{uri=~"/api/.*",job="oscal-tools-api"}[5m]))by(uri)/sum(rate(http_server_requests_seconds_count{uri=~"/api/.*",job="oscal-tools-api"}[5m]))by(uri)*1000)' | jq

# 2. Check if it's a resource issue
docker stats oscal-ux --no-stream

# 3. Check database connections
curl -H "Authorization: Bearer YOUR_TOKEN" \
     http://localhost:8080/actuator/metrics/hikaricp.connections.active | jq

# 4. If critical, consider restart
# (Only if response times are unacceptable >10s)
docker-compose restart oscal-ux
```

### Short-term Actions (5-30 minutes)

```bash
# 1. Enable detailed logging
# Add logging to identify slow operations

# 2. Profile slow endpoints
# Add timing logs to service methods

# 3. Check database query performance
# Enable slow query logging

# 4. Review recent changes
# Check if deployment introduced regression
git log -10 --oneline

# 5. Implement quick optimizations
# Add indexes, increase timeouts, etc.
```

### Long-term Actions (1-7 days)

```bash
# 1. Implement comprehensive performance monitoring
# Add APM tool (New Relic, DataDog, etc.)

# 2. Conduct performance profiling
# Use profilers to identify bottlenecks

# 3. Implement caching strategy
# Cache expensive operations

# 4. Optimize database queries
# Add indexes, optimize query structure

# 5. Implement async processing
# Move long-running operations to background

# 6. Load testing
# Test with production-like load
# Identify breaking points

# 7. Code optimization
# Profile and optimize hot paths
```

## Verification

```bash
# 1. Response time is back to normal (<1s p95)
curl -s 'http://localhost:9090/api/v1/query?query=histogram_quantile(0.95,sum(rate(http_server_requests_seconds_bucket{job="oscal-tools-api"}[5m]))by(le))*1000' | jq -r '.data.result[0].value[1]'
# Expected: < 1000 ms

# 2. All endpoints performing well
# Check Grafana "Slowest Endpoints" table
# No endpoints should be >2s

# 3. Database queries fast
docker-compose exec postgres psql -U oscal_user -d oscal_dev -c \
  "SELECT query, mean_exec_time FROM pg_stat_statements WHERE mean_exec_time > 1000 ORDER BY mean_exec_time DESC LIMIT 5;"
# Expected: Few or no slow queries

# 4. System resources healthy
docker stats oscal-ux --no-stream
# CPU should be reasonable, memory stable

# 5. No timeouts in logs
tail -100 back-end/logs/spring.log | grep -i "timeout"
# Expected: No timeout errors

# 6. Test critical operations
time curl -X POST http://localhost:8080/api/auth/login \
     -H "Content-Type: application/json" \
     -d '{"username":"test","password":"test"}'
# Should complete in <1s
```

## Prevention

### Code Optimization

- **Profile regularly** - Use profilers to find bottlenecks
- **Optimize database queries** - Use EXPLAIN, add indexes
- **Implement caching** - Cache expensive operations
- **Async processing** - Don't block request threads
- **Pagination** - Limit result set sizes
- **Connection pooling** - Reuse connections efficiently
- **Timeouts** - Set reasonable timeouts for all operations

### Infrastructure

- **Adequate resources** - Sufficient CPU, RAM, disk I/O
- **Database tuning** - Optimize PostgreSQL configuration
- **Network optimization** - Fast, reliable networking
- **Load balancing** - Distribute load across instances
- **Auto-scaling** - Scale based on load
- **CDN** - Cache static assets

### Monitoring

- **Response time alerts** - Alert at 1s, 2s thresholds
- **Endpoint-specific monitoring** - Track each endpoint separately
- **Database query monitoring** - Track slow queries
- **Resource monitoring** - CPU, memory, disk, network
- **Distributed tracing** - Trace requests across services
- **APM tools** - Application Performance Monitoring

### Testing

- **Performance testing** - Regular load/stress tests
- **Benchmark tests** - Track performance over time
- **Profiling** - Profile code during development
- **Database query testing** - Test queries with production data volumes
- **Chaos engineering** - Test under failure conditions

## Escalation

**p95 > 1s** (Early warning):
- Monitor closely
- Check recent changes
- Review slow endpoints

**p95 > 2s** (Alert threshold):
- Investigate immediately
- Identify root cause
- Apply quick fixes
- Notify team

**p95 > 5s** (Critical):
- Escalate to senior engineer
- Consider rollback if recent deployment
- Implement emergency fixes
- Start incident process

**p95 > 10s or timeouts**:
- Emergency response
- Consider taking service offline
- Escalate to platform lead
- Immediate rollback/fix

## Post-Incident

1. **Analyze metrics** - Review response time trends
2. **Identify root cause** - What caused the slowdown?
3. **Implement fix** - Address the underlying issue
4. **Load testing** - Verify fix under load
5. **Update monitoring** - Add better alerts
6. **Code review** - Review performance implications
7. **Documentation** - Update runbook with learnings
8. **Team training** - Share performance best practices

## Related Runbooks

- [HIGH-MEMORY-USAGE.md](HIGH-MEMORY-USAGE.md) - Memory issues can cause slow GC
- [DATABASE-CONNECTION-ISSUES.md](DATABASE-CONNECTION-ISSUES.md) - Database problems affect response time
- [HIGH-ERROR-RATE.md](HIGH-ERROR-RATE.md) - Errors often correlate with slow responses
- [HIGH-CPU-USAGE.md](HIGH-CPU-USAGE.md) - CPU saturation causes slowness

## Additional Resources

- **JProfiler**: https://www.ej-technologies.com/products/jprofiler/overview.html
- **VisualVM**: https://visualvm.github.io/
- **PostgreSQL EXPLAIN**: https://www.postgresql.org/docs/current/using-explain.html
- **Spring Boot Performance**: https://spring.io/blog/2015/12/10/spring-boot-memory-performance
- **HikariCP Tuning**: https://github.com/brettwooldridge/HikariCP/wiki/About-Pool-Sizing

---

**Last Updated**: October 26, 2025
**Tested**: No (requires performance degradation scenario)
**Review Cycle**: After each performance incident
