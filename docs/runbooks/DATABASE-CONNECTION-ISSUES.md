# Runbook: Database Connection Issues

**Alert**: DatabaseDown
**Severity**: CRITICAL
**Threshold**: Database health check failing for > 1 minute

## Symptoms

- Application logs show "Unable to acquire JDBC Connection"
- Health check shows database component DOWN
- Users see 500 errors when accessing application
- Connection pool exhausted messages in logs

## Impact

- **User Impact**: HIGH - Cannot perform any database operations
- **Business Impact**: CRITICAL - No data read/write operations possible
- **Data Impact**: Potential data loss if writes were in progress

## Diagnosis Steps

### 1. Check Database Status

```bash
# Check if PostgreSQL container is running
docker ps | grep postgres

# Check PostgreSQL health
docker-compose exec postgres pg_isready -U oscal_user

# Check database logs
docker-compose logs --tail=50 postgres
```

### 2. Test Connectivity

```bash
# Test connection from host
psql -h localhost -p 5432 -U oscal_user -d oscal_dev -c "SELECT 1"

# Test connection from application container
docker-compose exec oscal-ux sh -c \
  'psql $DB_URL -c "SELECT 1"'
```

### 3. Check Connection Pool

```bash
# Check active connections (via actuator metrics)
curl -H "Authorization: Bearer YOUR_TOKEN" \
     http://localhost:8080/actuator/metrics/jdbc.connections.active | jq

# Check max connections
curl -H "Authorization: Bearer YOUR_TOKEN" \
     http://localhost:8080/actuator/metrics/jdbc.connections.max | jq
```

### 4. Check Database Resources

```bash
# Check PostgreSQL resource usage
docker stats postgres --no-stream

# Check active connections in database
docker-compose exec postgres psql -U oscal_user -d oscal_dev -c \
  "SELECT count(*) FROM pg_stat_activity WHERE state = 'active';"
```

## Common Causes & Solutions

### Cause 1: Database Container Not Running

**Symptoms**: `docker ps` doesn't show postgres container

**Solution**:
```bash
# Start PostgreSQL
docker-compose up -d postgres

# Wait for it to be healthy
sleep 10

# Verify it's running
docker-compose ps postgres

# Check logs for errors
docker-compose logs postgres
```

### Cause 2: Wrong Database Credentials

**Symptoms**: "password authentication failed" in logs

**Solution**:
```bash
# 1. Check environment variables
docker-compose exec oscal-ux env | grep DB_

# 2. Verify credentials match docker-compose.yml
cat docker-compose.yml | grep -A 5 "POSTGRES_"

# 3. Update credentials if needed (in .env file)
cat > .env <<EOF
DB_USERNAME=oscal_user
DB_PASSWORD=oscal_dev_password
DB_NAME=oscal_dev
EOF

# 4. Restart services
docker-compose restart oscal-ux
```

### Cause 3: Connection Pool Exhausted

**Symptoms**: "Could not obtain connection from pool" in logs

**Solution**:
```bash
# 1. Check current pool configuration
curl -H "Authorization: Bearer YOUR_TOKEN" \
     http://localhost:8080/actuator/configprops | \
     jq '.["spring.datasource.hikari"]'

# 2. Increase pool size (temporary)
docker-compose stop oscal-ux
docker-compose run -e DB_POOL_MAX_SIZE=50 oscal-ux

# 3. Permanent fix: Update application.properties
# spring.datasource.hikari.maximum-pool-size=50
```

### Cause 4: Network Issues

**Symptoms**: "Connection refused" or timeouts

**Solution**:
```bash
# 1. Check if database port is accessible
nc -zv localhost 5432

# 2. Check Docker network
docker network inspect oscal-network

# 3. Recreate network if needed
docker-compose down
docker network prune
docker-compose up -d
```

### Cause 5: Database Disk Full

**Symptoms**: "No space left on device" in database logs

**Solution**:
```bash
# 1. Check disk usage
df -h

# 2. Check Docker volume size
docker system df -v | grep postgres

# 3. Clean up old data
docker-compose exec postgres psql -U oscal_user -d oscal_dev -c \
  "VACUUM FULL;"

# 4. Prune Docker volumes (CAUTION: data loss)
docker volume prune
```

### Cause 6: Too Many Connections

**Symptoms**: "FATAL: sorry, too many clients already"

**Solution**:
```bash
# 1. Check current connections
docker-compose exec postgres psql -U oscal_user -d oscal_dev -c \
  "SELECT count(*) FROM pg_stat_activity;"

# 2. Check max connections limit
docker-compose exec postgres psql -U oscal_user -d oscal_dev -c \
  "SHOW max_connections;"

# 3. Kill idle connections
docker-compose exec postgres psql -U oscal_user -d oscal_dev -c \
  "SELECT pg_terminate_backend(pid) FROM pg_stat_activity
   WHERE state = 'idle' AND state_change < NOW() - INTERVAL '10 minutes';"

# 4. Increase max connections (update docker-compose.yml)
# Add: command: -c max_connections=200
```

## Resolution Steps

### Quick Recovery

```bash
# 1. Restart database
docker-compose restart postgres

# 2. Wait for database to be ready (check logs)
docker-compose logs -f postgres
# Wait for: "database system is ready to accept connections"

# 3. Restart application
docker-compose restart oscal-ux

# 4. Verify connectivity
curl http://localhost:8080/actuator/health | jq '.components.db'
```

### Full Recovery (if quick restart fails)

```bash
# 1. Stop all services
docker-compose down

# 2. Check for corrupted data volumes
docker volume ls | grep postgres

# 3. If needed, backup and recreate volume
docker volume create postgres_backup
docker run --rm -v postgres_dev_data:/source \
           -v postgres_backup:/backup \
           alpine tar czf /backup/postgres-backup.tar.gz -C /source .

# 4. Start services
docker-compose up -d

# 5. Monitor startup
docker-compose logs -f
```

## Verification

```bash
# 1. Database is accessible
docker-compose exec postgres pg_isready -U oscal_user
# Expected: accepting connections

# 2. Application can connect
curl http://localhost:8080/actuator/health | jq '.components.db.status'
# Expected: "UP"

# 3. Connection pool is healthy
curl -H "Authorization: Bearer YOUR_TOKEN" \
     http://localhost:8080/actuator/metrics/jdbc.connections.active | jq
# Should be less than max

# 4. Test a database operation
curl -X POST http://localhost:8080/api/auth/login \
     -H "Content-Type: application/json" \
     -d '{"username":"test","password":"test"}'
# Should work (or return expected auth error)
```

## Prevention

### Monitoring
- Alert on connection pool utilization >80%
- Monitor database CPU and memory
- Track slow queries
- Monitor disk space

### Configuration
- Set appropriate connection pool size
- Configure connection timeout
- Enable connection validation
- Set idle connection timeout

### Maintenance
- Regular VACUUM operations
- Monitor table bloat
- Index maintenance
- Log rotation

## Escalation

If database cannot be restored within **10 minutes**:

1. **Immediate**: Notify database administrator
2. **15 minutes**: Escalate to platform lead
3. **30 minutes**: Consider failover to backup database (if available)
4. **Consider**: Restoring from latest backup

## Post-Incident

1. **Analyze**: Review database logs for root cause
2. **Optimize**: Tune connection pool settings
3. **Monitor**: Add metrics for early detection
4. **Document**: Update this runbook with findings
5. **Test**: Verify backup/restore procedures

## Related Runbooks

- [SERVICE-DOWN.md](SERVICE-DOWN.md)
- [BACKUP-RESTORE.md](BACKUP-RESTORE.md)
- [HIGH-RESPONSE-TIME.md](HIGH-RESPONSE-TIME.md)

---

**Last Updated**: October 26, 2025
