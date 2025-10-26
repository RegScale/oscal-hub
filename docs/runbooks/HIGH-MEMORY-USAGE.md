# Runbook: High Memory Usage

**Alert**: HighMemoryUsage
**Severity**: WARNING → CRITICAL
**Threshold**: JVM heap usage >85% for 5+ minutes

## Symptoms

- Grafana alert showing high memory usage
- Application becoming sluggish or unresponsive
- Frequent garbage collection pauses
- "OutOfMemoryError" in application logs
- Requests timing out
- Application container restarting frequently

## Impact

- **User Impact**: MEDIUM-HIGH - Slow responses, timeouts, failed requests
- **Business Impact**: HIGH - Reduced throughput, potential service outage
- **Data Impact**: MEDIUM - In-flight operations may fail
- **System Impact**: HIGH - May cause application crash and restart

## Diagnosis Steps

### 1. Check Current Memory Usage

```bash
# Check JVM heap memory via actuator
curl -H "Authorization: Bearer YOUR_TOKEN" \
     http://localhost:8080/actuator/metrics/jvm.memory.used | jq

curl -H "Authorization: Bearer YOUR_TOKEN" \
     http://localhost:8080/actuator/metrics/jvm.memory.max | jq

# Check memory usage percentage
curl -s -H "Authorization: Bearer YOUR_TOKEN" \
     http://localhost:8080/actuator/metrics/jvm.memory.used | jq -r '.measurements[] | select(.statistic=="VALUE") | .value' > /tmp/used.txt
curl -s -H "Authorization: Bearer YOUR_TOKEN" \
     http://localhost:8080/actuator/metrics/jvm.memory.max | jq -r '.measurements[] | select(.statistic=="VALUE") | .value' > /tmp/max.txt
echo "Memory usage: $(awk '{printf "%.2f%%", ($1/$2)*100}' /tmp/used.txt /tmp/max.txt)"

# Or check via Grafana dashboard
# Navigate to: http://localhost:3001/d/oscal-tools-overview
# Look at "JVM Memory Usage" panel
```

### 2. Check Garbage Collection Activity

```bash
# Check GC metrics
curl -H "Authorization: Bearer YOUR_TOKEN" \
     http://localhost:8080/actuator/metrics/jvm.gc.pause | jq

curl -H "Authorization: Bearer YOUR_TOKEN" \
     http://localhost:8080/actuator/metrics/jvm.gc.memory.allocated | jq

# Check GC count and time
curl -H "Authorization: Bearer YOUR_TOKEN" \
     http://localhost:8080/actuator/metrics/jvm.gc.live.data.size | jq
```

### 3. Check for Memory Leaks

```bash
# Look for increasing memory trend over time in Grafana
# If memory steadily increases without dropping, likely a leak

# Check heap histogram (requires JDK tools)
docker-compose exec oscal-ux jcmd 1 GC.heap_info

# Get heap dump (WARNING: may pause application briefly)
docker-compose exec oscal-ux jcmd 1 GC.heap_dump /tmp/heapdump.hprof

# Copy heap dump out for analysis
docker cp oscal-ux:/tmp/heapdump.hprof /tmp/heapdump.hprof
```

### 4. Check Application Logs

```bash
# Look for OOM errors
tail -500 back-end/logs/spring.log | grep -i "OutOfMemory\|memory\|heap"

# Check for GC overhead warnings
tail -500 back-end/logs/spring.log | grep -i "gc overhead"

# Look for large object allocations
tail -500 back-end/logs/spring.log | grep -i "large\|allocation"
```

### 5. Check Container Resources

```bash
# Check Docker container memory stats
docker stats oscal-ux --no-stream

# Check if container is hitting memory limits
docker inspect oscal-ux | jq '.[0].HostConfig.Memory'
```

## Common Causes & Solutions

### Cause 1: Memory Leak in Application Code

**Symptoms**: Memory usage steadily increases over time, never decreases significantly

**Diagnosis**:
```bash
# 1. Capture heap dump
docker-compose exec oscal-ux jcmd 1 GC.heap_dump /tmp/heapdump.hprof

# 2. Analyze with Eclipse MAT or VisualVM
# Download: https://www.eclipse.org/mat/
# Look for objects with large retained size

# 3. Check for common leak patterns:
# - Static collections that grow unbounded
# - Unclosed resources (files, connections, streams)
# - Thread-local variables
# - Event listeners not removed
# - Caches without size limits
```

**Solution**:
```bash
# Immediate (temporary):
# Restart application to reclaim memory
docker-compose restart oscal-ux

# Long-term:
# 1. Fix the leak in code
# 2. Add proper resource cleanup (try-with-resources)
# 3. Implement cache size limits
# 4. Use weak references where appropriate
# 5. Add memory leak detection tools (e.g., LeakCanary for Java)

# Deploy fix:
cd back-end
mvn clean install
docker-compose up -d --build oscal-ux
```

### Cause 2: Insufficient Heap Size

**Symptoms**: Memory reaches max quickly under load, frequent GC pauses

**Diagnosis**:
```bash
# Check current heap settings
docker-compose exec oscal-ux java -XX:+PrintFlagsFinal -version | grep -i heapsize

# Check current max heap
curl -H "Authorization: Bearer YOUR_TOKEN" \
     http://localhost:8080/actuator/metrics/jvm.memory.max | jq
```

**Solution**:
```bash
# Option 1: Update docker-compose.yml
# Add JAVA_OPTS environment variable
cat >> docker-compose.yml <<'EOF'
services:
  oscal-ux:
    environment:
      - JAVA_OPTS=-Xmx2g -Xms1g -XX:MaxMetaspaceSize=512m
EOF

# Option 2: Update Dockerfile
# Add JVM options to ENTRYPOINT
# ENTRYPOINT ["java", "-Xmx2g", "-Xms1g", "-jar", "app.jar"]

# Restart with new settings
docker-compose down
docker-compose up -d --build

# Verify new settings
docker-compose exec oscal-ux java -XX:+PrintFlagsFinal -version | grep -E "MaxHeapSize|InitialHeapSize"
```

### Cause 3: Large File Processing

**Symptoms**: Memory spikes when processing large OSCAL documents

**Diagnosis**:
```bash
# Check request sizes
tail -200 back-end/logs/spring.log | grep -i "content-length\|size"

# Monitor memory during file upload
watch -n 2 'curl -s -H "Authorization: Bearer YOUR_TOKEN" http://localhost:8080/actuator/metrics/jvm.memory.used | jq -r ".measurements[] | select(.statistic==\"VALUE\") | .value"'
```

**Solution**:
```bash
# 1. Implement streaming for large files
# Update validation/conversion services to use streaming APIs

# 2. Add file size limits
# Update application.properties:
cat >> back-end/src/main/resources/application.properties <<'EOF'
# File upload limits
spring.servlet.multipart.max-file-size=50MB
spring.servlet.multipart.max-request-size=50MB
EOF

# 3. Process files in chunks
# Update ValidationService.java to process incrementally

# 4. Add file size validation before processing
# Return 413 Payload Too Large for oversized files

mvn clean install
docker-compose restart oscal-ux
```

### Cause 4: Excessive Caching

**Symptoms**: Memory usage correlates with cache size, high cache hit rate but high memory

**Diagnosis**:
```bash
# Check cache statistics (if using Spring Cache)
curl -H "Authorization: Bearer YOUR_TOKEN" \
     http://localhost:8080/actuator/caches | jq

# Look for cache-related objects in heap dump
# Check for ConcurrentHashMap, Cache, etc.
```

**Solution**:
```bash
# 1. Implement cache size limits
# Update cache configuration to use LRU eviction

# Example: Update CacheConfig.java
cat > back-end/src/main/java/gov/nist/oscal/tools/api/config/CacheConfig.java <<'EOF'
@Configuration
@EnableCaching
public class CacheConfig {
    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        cacheManager.setCaffeine(Caffeine.newBuilder()
            .maximumSize(1000)  // Limit to 1000 entries
            .expireAfterWrite(10, TimeUnit.MINUTES)
            .recordStats());
        return cacheManager;
    }
}
EOF

# 2. Add cache eviction policies
# 3. Monitor cache hit rates vs memory usage
# 4. Consider external cache (Redis) for large datasets

mvn clean install
docker-compose restart oscal-ux
```

### Cause 5: Thread/Connection Pool Leaks

**Symptoms**: Growing number of threads or database connections

**Diagnosis**:
```bash
# Check thread count
curl -H "Authorization: Bearer YOUR_TOKEN" \
     http://localhost:8080/actuator/metrics/jvm.threads.live | jq

# Check database connection pool
curl -H "Authorization: Bearer YOUR_TOKEN" \
     http://localhost:8080/actuator/metrics/hikaricp.connections.active | jq

curl -H "Authorization: Bearer YOUR_TOKEN" \
     http://localhost:8080/actuator/metrics/hikaricp.connections.idle | jq
```

**Solution**:
```bash
# 1. Fix connection leaks
# Ensure all database operations use try-with-resources

# 2. Configure connection pool timeouts
# Update application.properties:
cat >> back-end/src/main/resources/application.properties <<'EOF'
# HikariCP Connection Pool Settings
spring.datasource.hikari.maximum-pool-size=20
spring.datasource.hikari.minimum-idle=5
spring.datasource.hikari.connection-timeout=30000
spring.datasource.hikari.idle-timeout=600000
spring.datasource.hikari.max-lifetime=1800000
spring.datasource.hikari.leak-detection-threshold=60000
EOF

# 3. Enable leak detection
# HikariCP will log warnings about leaked connections

mvn clean install
docker-compose restart oscal-ux
```

### Cause 6: Too Many Concurrent Requests

**Symptoms**: Memory spikes during high traffic, correlates with request rate

**Diagnosis**:
```bash
# Check current request rate
curl -s 'http://localhost:9090/api/v1/query?query=sum(rate(http_server_requests_seconds_count{job="oscal-tools-api"}[5m]))*60' | jq -r '.data.result[0].value[1]'

# Check thread pool utilization
curl -H "Authorization: Bearer YOUR_TOKEN" \
     http://localhost:8080/actuator/metrics/executor.active | jq
```

**Solution**:
```bash
# 1. Implement rate limiting
# Add rate limiting per user/IP

# 2. Configure thread pool limits
# Update application.properties:
cat >> back-end/src/main/resources/application.properties <<'EOF'
# Tomcat Thread Pool
server.tomcat.threads.max=200
server.tomcat.threads.min-spare=10
server.tomcat.max-connections=10000
server.tomcat.accept-count=100
EOF

# 3. Add request queuing/backpressure
# Reject requests when system is overloaded

# 4. Scale horizontally
# Add more application instances behind load balancer

mvn clean install
docker-compose restart oscal-ux
```

## Resolution Steps

### Immediate Actions (0-5 minutes)

```bash
# 1. Check if memory is still climbing
# If >95%, take immediate action

# 2. Quick fix: Force garbage collection
docker-compose exec oscal-ux jcmd 1 GC.run

# 3. If still high, restart application
docker-compose restart oscal-ux

# 4. Monitor memory after restart
watch -n 5 'curl -s -H "Authorization: Bearer YOUR_TOKEN" http://localhost:8080/actuator/metrics/jvm.memory.used | jq -r ".measurements[] | select(.statistic==\"VALUE\") | .value"'
```

### Short-term Actions (5-30 minutes)

```bash
# 1. Capture heap dump before restart (if not already done)
docker-compose exec oscal-ux jcmd 1 GC.heap_dump /tmp/heapdump.hprof
docker cp oscal-ux:/tmp/heapdump.hprof /tmp/heapdump-$(date +%Y%m%d-%H%M%S).hprof

# 2. Increase heap size temporarily
docker-compose stop oscal-ux
docker-compose run -e JAVA_OPTS="-Xmx4g -Xms2g" oscal-ux

# 3. Monitor for memory leak pattern
# Watch memory usage over 30 minutes
# If steadily increasing: likely a leak
# If stable after restart: load-related issue
```

### Long-term Actions (1-7 days)

```bash
# 1. Analyze heap dump offline
# Use Eclipse MAT or VisualVM
# Identify leak suspects

# 2. Review code for common leak patterns
# Search for static collections
# Check resource cleanup
# Review cache implementations

# 3. Add memory monitoring alerts
# Alert at 70%, 85%, 95% thresholds

# 4. Implement automatic heap dumps on OOM
# Add JVM flag: -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/tmp

# 5. Add memory profiling to CI/CD
# Run load tests with memory profiling
# Detect leaks before production
```

## Verification

```bash
# 1. Memory usage is below 75%
curl -s -H "Authorization: Bearer YOUR_TOKEN" \
     http://localhost:8080/actuator/metrics/jvm.memory.used | jq -r '.measurements[] | select(.statistic=="VALUE") | .value' > /tmp/used.txt
curl -s -H "Authorization: Bearer YOUR_TOKEN" \
     http://localhost:8080/actuator/metrics/jvm.memory.max | jq -r '.measurements[] | select(.statistic=="VALUE") | .value' > /tmp/max.txt
awk '{if(($1/$2)*100 < 75) print "✓ Memory usage OK"; else print "✗ Memory usage still high"}' /tmp/used.txt /tmp/max.txt

# 2. GC activity is normal
curl -H "Authorization: Bearer YOUR_TOKEN" \
     http://localhost:8080/actuator/metrics/jvm.gc.pause | jq
# GC pauses should be short (<100ms typically)

# 3. Application is responsive
curl -w "\nResponse time: %{time_total}s\n" http://localhost:8080/actuator/health
# Should respond quickly (<1s)

# 4. No OOM errors in logs
tail -100 back-end/logs/spring.log | grep -i "OutOfMemory"
# Expected: No results

# 5. Memory stable over time (no leak)
# Monitor for 1 hour
# Memory should not continuously increase
```

## Prevention

### JVM Configuration

```bash
# 1. Set appropriate heap sizes
# -Xms (initial) = 50-75% of -Xmx (max)
# Example: -Xms2g -Xmx4g

# 2. Enable GC logging
# -Xlog:gc*:file=/var/log/gc.log:time,uptime:filecount=5,filesize=10m

# 3. Enable heap dump on OOM
# -XX:+HeapDumpOnOutOfMemoryError
# -XX:HeapDumpPath=/tmp/heapdump.hprof

# 4. Use modern GC (G1GC is default in Java 11+)
# -XX:+UseG1GC
# -XX:MaxGCPauseMillis=200
```

### Application Design

- **Streaming APIs** - Process large files without loading entirely into memory
- **Pagination** - Limit result set sizes
- **Connection pooling** - Reuse connections, enforce limits
- **Resource cleanup** - Always close resources (try-with-resources)
- **Cache limits** - Set maximum cache sizes with LRU eviction
- **Object pooling** - Reuse expensive objects

### Monitoring

- **Memory alerts** - Alert at 70%, 85%, 95% heap usage
- **GC monitoring** - Track GC frequency and duration
- **Heap trend analysis** - Detect gradual memory leaks
- **Thread monitoring** - Track thread count growth
- **Connection pool monitoring** - Alert on pool exhaustion

### Testing

- **Load testing** - Test with production-like load
- **Memory leak testing** - Run for extended periods (12-24 hours)
- **Heap analysis** - Regular heap dump analysis
- **Stress testing** - Push beyond normal limits
- **Profiling** - Use profilers during development

## Escalation

**70% heap usage**:
- Monitor closely
- Review recent changes
- Check for unusual activity

**85% heap usage** (Alert threshold):
- Investigate immediately
- Identify cause
- Prepare to restart if needed
- Notify team

**95% heap usage** (Critical):
- Capture heap dump
- Restart application
- Escalate to senior engineer
- Start incident process

**Application crashes due to OOM**:
- Immediate restart
- Increase heap size temporarily
- Escalate to platform lead
- Emergency code review

## Post-Incident

1. **Analyze heap dump** - Identify root cause
2. **Review code** - Fix memory leaks
3. **Update monitoring** - Add better alerts
4. **Load testing** - Verify fix under load
5. **Documentation** - Update this runbook
6. **Team training** - Share learnings
7. **Code review process** - Add memory considerations
8. **CI/CD checks** - Add memory leak detection

## Related Runbooks

- [SERVICE-DOWN.md](SERVICE-DOWN.md) - If OOM causes service crash
- [HIGH-RESPONSE-TIME.md](HIGH-RESPONSE-TIME.md) - GC pauses cause slow responses
- [HIGH-ERROR-RATE.md](HIGH-ERROR-RATE.md) - OOM causes errors

## Additional Resources

- **Eclipse MAT**: https://www.eclipse.org/mat/
- **VisualVM**: https://visualvm.github.io/
- **Java GC Tuning**: https://docs.oracle.com/en/java/javase/11/gctuning/
- **HikariCP Configuration**: https://github.com/brettwooldridge/HikariCP#configuration-knobs-baby

---

**Last Updated**: October 26, 2025
**Tested**: No (requires simulated memory pressure)
**Review Cycle**: After each memory-related incident
