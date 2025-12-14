# OSCAL Tools - Production Deployment Checklist

**Version**: 1.0.0
**Date**: 2025-10-26
**Purpose**: Comprehensive checklist for deploying OSCAL Tools to production environments

---

## Pre-Deployment Checklist

### Environment Preparation

#### Infrastructure

- [ ] **Server/VM provisioned** with minimum specs:
  - CPU: 2 cores (4 cores recommended)
  - RAM: 4GB minimum (8GB recommended)
  - Disk: 50GB minimum (SSD recommended)
  - OS: Ubuntu 22.04 LTS or RHEL 8+

- [ ] **Docker installed** (version 24.0+)
  ```bash
  docker --version
  # Expected: Docker version 24.0.0 or higher
  ```

- [ ] **Docker Compose installed** (version 2.20+)
  ```bash
  docker-compose --version
  # Expected: Docker Compose version 2.20.0 or higher
  ```

- [ ] **Firewall configured**:
  - [ ] Port 443 (HTTPS) open for incoming
  - [ ] Port 80 (HTTP) open for incoming (redirect to 443)
  - [ ] Port 22 (SSH) open for admin access only
  - [ ] All other ports blocked

#### Domain & SSL

- [ ] **Domain name configured** and DNS pointing to server:
  ```bash
  nslookup your-domain.com
  # Should return your server's IP address
  ```

- [ ] **SSL/TLS certificate obtained**:
  - Option 1: Let's Encrypt (free, automated)
  - Option 2: Commercial CA (DigiCert, Sectigo, etc.)
  - Option 3: Internal CA (for private deployments)

- [ ] **SSL certificate files accessible**:
  - [ ] `fullchain.pem` (certificate + intermediate chain)
  - [ ] `privkey.pem` (private key)
  - [ ] Stored in `/etc/nginx/ssl/` or similar secure location
  - [ ] Permissions: `chmod 600 /etc/nginx/ssl/privkey.pem`

#### Database

- [ ] **PostgreSQL 18 installed** (via Docker or native):
  ```bash
  docker exec oscal-postgres-prod psql --version
  # Expected: psql (PostgreSQL) 18.0
  ```

- [ ] **Database created**:
  - [ ] Database name: `oscal_production`
  - [ ] User: `oscal_user` (NOT root/postgres)
  - [ ] Strong password generated and stored securely

- [ ] **Database connection tested**:
  ```bash
  docker exec -e PGPASSWORD='your-password' oscal-postgres-prod \
    psql -U oscal_user -d oscal_production -c "SELECT version();"
  ```

- [ ] **Backup strategy configured**:
  - [ ] Automated daily backups scheduled (cron job)
  - [ ] Backup retention policy defined (30+ days)
  - [ ] Backup restoration tested successfully
  - [ ] Off-site backup storage configured

---

## Security Configuration

### Secret Generation

- [ ] **JWT Secret generated** (minimum 64 characters):
  ```bash
  openssl rand -base64 64 | tr -d '\n' | head -c 64
  ```
  - [ ] Stored in environment variable: `JWT_SECRET`
  - [ ] NOT committed to git
  - [ ] Documented in secure password manager

- [ ] **Database password generated** (minimum 32 characters):
  ```bash
  openssl rand -base64 32 | tr -d '\n'
  ```
  - [ ] Stored in environment variable: `DB_PASSWORD`
  - [ ] NOT committed to git
  - [ ] PostgreSQL user password updated

- [ ] **Admin account created**:
  - [ ] Username: (not "admin" - use unique name)
  - [ ] Strong password meeting policy:
    - Minimum 12 characters
    - Upper + lower + digit + special
  - [ ] Email verified
  - [ ] MFA enabled (if implemented)

### Environment Variables

Create `/etc/oscal-tools/.env.production`:

```bash
# Spring Profile
SPRING_PROFILES_ACTIVE=prod

# Database Configuration
DB_URL=jdbc:postgresql://postgres:5432/oscal_production
DB_DRIVER=org.postgresql.Driver
DB_USERNAME=oscal_user
DB_PASSWORD=<GENERATED_PASSWORD>
DB_DIALECT=org.hibernate.dialect.PostgreSQLDialect

# JWT Configuration
JWT_SECRET=<GENERATED_SECRET>
JWT_EXPIRATION=86400000

# CORS Configuration (REQUIRED)
CORS_ALLOWED_ORIGINS=https://your-domain.com,https://www.your-domain.com
CORS_ALLOWED_METHODS=GET,POST,PUT,DELETE,OPTIONS
CORS_ALLOWED_HEADERS=Authorization,Content-Type
CORS_ALLOW_CREDENTIALS=true

# Security Headers (ENABLED)
SECURITY_HEADERS_ENABLED=true
SECURITY_REQUIRE_HTTPS=true
HSTS_MAX_AGE=31536000
CSP_DEFAULT_SRC='self'
FRAME_OPTIONS_POLICY=DENY

# Rate Limiting (ENABLED)
RATE_LIMIT_ENABLED=true
RATE_LIMIT_LOGIN_ATTEMPTS=5
RATE_LIMIT_LOGIN_DURATION=60
RATE_LIMIT_API_REQUESTS=100
RATE_LIMIT_API_DURATION=60

# Logging
LOG_LEVEL_ROOT=WARN
LOG_LEVEL_OSCAL=INFO
LOG_FILE=/var/log/oscal-tools/application.log

# Optional: Azure Storage
AZURE_STORAGE_CONNECTION_STRING=<IF_USING_AZURE>
AZURE_STORAGE_CONTAINER_NAME=oscal-files

# Optional: Email (for notifications)
MAIL_HOST=smtp.example.com
MAIL_PORT=587
MAIL_USERNAME=noreply@your-domain.com
MAIL_PASSWORD=<MAIL_PASSWORD>
```

**Verify**:
- [ ] All required variables set
- [ ] No default/example values used
- [ ] File permissions: `chmod 600 /etc/oscal-tools/.env.production`
- [ ] File owner: `chown root:root /etc/oscal-tools/.env.production`

### Application Configuration

- [ ] **application-prod.properties verified**:
  ```bash
  cat back-end/src/main/resources/application-prod.properties
  ```
  - [ ] `spring.profiles.active=prod`
  - [ ] Database connection uses environment variables
  - [ ] H2 console disabled: `spring.h2.console.enabled=false`
  - [ ] Swagger disabled: `springdoc.swagger-ui.enabled=false`

- [ ] **Security headers enabled**:
  - [ ] CSP configured
  - [ ] HSTS configured (HTTPS only)
  - [ ] X-Frame-Options set to DENY
  - [ ] X-Content-Type-Options set to nosniff

- [ ] **Rate limiting configured**:
  - [ ] Login: 5 attempts per 60 seconds
  - [ ] Registration: 3 attempts per hour
  - [ ] API: 100 requests per minute

### Docker Security

- [ ] **Docker images built**:
  ```bash
  docker build -t oscal-ux:production .
  ```

- [ ] **Image scanned for vulnerabilities**:
  ```bash
  trivy image oscal-ux:production
  ```
  - [ ] No HIGH or CRITICAL vulnerabilities
  - [ ] All vulnerabilities documented and accepted

- [ ] **Non-root user verified**:
  ```bash
  docker run --rm oscal-ux:production id
  # Expected: uid=10001(appuser) gid=10001(appgroup)
  ```

- [ ] **Security options in docker-compose.prod.yml**:
  - [ ] `no-new-privileges:true`
  - [ ] Capabilities dropped: `ALL`
  - [ ] Resource limits configured

---

## Deployment Steps

### 1. Code Deployment

- [ ] **Clone repository** to production server:
  ```bash
  cd /opt
  git clone https://github.com/RegScale/oscal-hub.git
  cd oscal-cli
  git checkout main  # or specific release tag
  ```

- [ ] **Copy environment file**:
  ```bash
  cp /etc/oscal-tools/.env.production .env
  ```

- [ ] **Build application** (if building on server):
  ```bash
  # Backend
  cd back-end
  mvn clean package -DskipTests

  # Frontend
  cd ../front-end
  npm ci
  npm run build
  ```

### 2. Database Setup

- [ ] **Start PostgreSQL** (if using Docker):
  ```bash
  docker-compose -f docker-compose-postgres.yml up -d
  ```

- [ ] **Wait for PostgreSQL to be healthy**:
  ```bash
  docker inspect oscal-postgres-prod --format='{{.State.Health.Status}}'
  # Expected: healthy
  ```

- [ ] **Run database migrations** (if applicable):
  ```bash
  # Hibernate auto-creates schema on first run
  # Or use Flyway/Liquibase for controlled migrations
  ```

- [ ] **Verify database schema**:
  ```bash
  docker exec -e PGPASSWORD='password' oscal-postgres-prod \
    psql -U oscal_user -d oscal_production -c "\dt"
  # Should show: users, roles, validation_history, etc.
  ```

### 3. Application Start

- [ ] **Start application containers**:
  ```bash
  docker-compose -f docker-compose.prod.yml up -d
  ```

- [ ] **Verify containers running**:
  ```bash
  docker-compose -f docker-compose.prod.yml ps
  # All services should be "Up" and "healthy"
  ```

- [ ] **Check application logs**:
  ```bash
  docker-compose -f docker-compose.prod.yml logs -f oscal-ux
  # Look for: "Started OscalCliApiApplication"
  ```

- [ ] **Verify health endpoint**:
  ```bash
  curl http://localhost:8080/api/health
  # Expected: {"status":"UP"}
  ```

### 4. Reverse Proxy Setup (Nginx)

- [ ] **Nginx installed**:
  ```bash
  nginx -v
  # Expected: nginx/1.24.0 or higher
  ```

- [ ] **SSL certificate configured** in `/etc/nginx/sites-available/oscal-tools`:
  ```nginx
  server {
      listen 443 ssl http2;
      server_name your-domain.com;

      ssl_certificate /etc/nginx/ssl/fullchain.pem;
      ssl_certificate_key /etc/nginx/ssl/privkey.pem;
      ssl_protocols TLSv1.2 TLSv1.3;
      ssl_ciphers HIGH:!aNULL:!MD5;
      ssl_prefer_server_ciphers on;

      # Security headers
      add_header Strict-Transport-Security "max-age=31536000; includeSubDomains" always;
      add_header X-Frame-Options "DENY" always;
      add_header X-Content-Type-Options "nosniff" always;

      # Proxy to backend
      location /api {
          proxy_pass http://localhost:8080;
          proxy_set_header Host $host;
          proxy_set_header X-Real-IP $remote_addr;
          proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
          proxy_set_header X-Forwarded-Proto $scheme;
      }

      # Proxy to frontend
      location / {
          proxy_pass http://localhost:3000;
          proxy_set_header Host $host;
          proxy_set_header X-Real-IP $remote_addr;
          proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
      }
  }

  # HTTP to HTTPS redirect
  server {
      listen 80;
      server_name your-domain.com;
      return 301 https://$host$request_uri;
  }
  ```

- [ ] **Nginx configuration tested**:
  ```bash
  nginx -t
  # Expected: configuration file test is successful
  ```

- [ ] **Nginx reloaded**:
  ```bash
  systemctl reload nginx
  ```

- [ ] **HTTPS access verified**:
  ```bash
  curl -I https://your-domain.com
  # Expected: HTTP/2 200
  ```

---

## Post-Deployment Verification

### Functional Testing

- [ ] **Homepage loads** at `https://your-domain.com`
- [ ] **User registration works**
  - [ ] Navigate to `/register`
  - [ ] Create new account
  - [ ] Verify strong password requirement
  - [ ] Confirm email verification (if enabled)

- [ ] **Login works**
  - [ ] Navigate to `/login`
  - [ ] Enter valid credentials
  - [ ] JWT token received and stored
  - [ ] Redirected to dashboard

- [ ] **OSCAL validation works**
  - [ ] Upload valid OSCAL JSON file
  - [ ] Validation succeeds
  - [ ] Results displayed correctly

- [ ] **OSCAL conversion works**
  - [ ] Upload OSCAL JSON file
  - [ ] Convert to XML
  - [ ] Download converted file
  - [ ] Verify conversion accuracy

- [ ] **History page works**
  - [ ] Navigate to `/history`
  - [ ] Previous operations displayed
  - [ ] Pagination works (if applicable)

- [ ] **Logout works**
  - [ ] Click logout button
  - [ ] JWT token cleared
  - [ ] Redirected to login page
  - [ ] Cannot access protected pages

### Security Testing

- [ ] **SSL/TLS verified**:
  ```bash
  openssl s_client -connect your-domain.com:443 -tls1_2
  # Verify TLS 1.2 or 1.3 connection
  ```

- [ ] **SSL Labs test** (A+ rating expected):
  - Visit: https://www.ssllabs.com/ssltest/
  - Enter: `your-domain.com`
  - [ ] Grade: A or A+
  - [ ] Certificate valid and trusted
  - [ ] Protocol support: TLS 1.2, TLS 1.3 only
  - [ ] No weak ciphers

- [ ] **Security headers present**:
  ```bash
  curl -I https://your-domain.com/api/health
  ```
  Expected headers:
  - [ ] `Strict-Transport-Security`
  - [ ] `X-Frame-Options: DENY`
  - [ ] `X-Content-Type-Options: nosniff`
  - [ ] `Content-Security-Policy`

- [ ] **CORS configuration verified**:
  ```bash
  curl -H "Origin: https://your-domain.com" \
       -H "Access-Control-Request-Method: POST" \
       -X OPTIONS \
       https://your-domain.com/api/validation/validate -v
  ```
  Expected:
  - [ ] `Access-Control-Allow-Origin: https://your-domain.com`
  - [ ] `Access-Control-Allow-Methods: GET,POST,PUT,DELETE,OPTIONS`
  - [ ] `Access-Control-Allow-Credentials: true`

- [ ] **Unauthorized CORS blocked**:
  ```bash
  curl -H "Origin: https://malicious-site.com" \
       https://your-domain.com/api/health -v
  ```
  Expected:
  - [ ] No `Access-Control-Allow-Origin` header (blocked)

- [ ] **Rate limiting works**:
  ```bash
  # Test login rate limit (5 attempts)
  for i in {1..6}; do
    curl -X POST https://your-domain.com/api/auth/login \
         -H "Content-Type: application/json" \
         -d '{"username":"test","password":"wrong"}' -i
  done
  ```
  Expected on 6th attempt:
  - [ ] `HTTP/1.1 429 Too Many Requests`
  - [ ] `Retry-After` header present
  - [ ] `X-RateLimit-Remaining: 0`

- [ ] **Authentication required**:
  ```bash
  curl -I https://your-domain.com/api/validation/validate
  ```
  Expected:
  - [ ] `HTTP/1.1 401 Unauthorized` (if not authenticated)

- [ ] **Invalid JWT rejected**:
  ```bash
  curl -H "Authorization: Bearer invalid-token" \
       https://your-domain.com/api/validation/validate -I
  ```
  Expected:
  - [ ] `HTTP/1.1 401 Unauthorized`

### Performance Testing

- [ ] **Response time acceptable**:
  ```bash
  curl -w "\nTime: %{time_total}s\n" https://your-domain.com/api/health
  ```
  Expected:
  - [ ] < 500ms for health check
  - [ ] < 2s for API validation requests

- [ ] **Concurrent users tested**:
  - Tool: Apache Bench, JMeter, or Locust
  - Test: 100 concurrent users
  - [ ] No errors under load
  - [ ] Response time degradation < 50%

- [ ] **Database connection pool working**:
  ```bash
  docker exec oscal-ux-prod jcmd <PID> GC.heap_info
  ```
  - [ ] HikariCP pool active
  - [ ] Max 20 connections
  - [ ] Min 5 idle connections

### Monitoring & Logging

- [ ] **Application logs accessible**:
  ```bash
  docker logs oscal-ux-prod
  tail -f /var/log/oscal-tools/application.log
  ```

- [ ] **Log rotation configured** (logrotate):
  ```bash
  cat /etc/logrotate.d/oscal-tools
  ```

- [ ] **Disk space monitoring**:
  ```bash
  df -h
  ```
  - [ ] At least 20% free space

- [ ] **Memory usage acceptable**:
  ```bash
  docker stats oscal-ux-prod
  ```
  - [ ] Memory < 80% of limit

- [ ] **CPU usage acceptable**:
  - [ ] CPU < 70% sustained

- [ ] **Health checks passing**:
  ```bash
  docker inspect oscal-ux-prod --format='{{.State.Health.Status}}'
  ```
  Expected:
  - [ ] `healthy`

---

## Monitoring & Alerting Setup

### Health Monitoring

- [ ] **Uptime monitoring configured** (UptimeRobot, Pingdom, etc.):
  - Endpoint: `https://your-domain.com/api/health`
  - Interval: 5 minutes
  - [ ] Email alerts on downtime
  - [ ] SMS alerts on critical failures

- [ ] **Database monitoring**:
  - [ ] Connection pool metrics
  - [ ] Query performance
  - [ ] Disk usage
  - [ ] Replication lag (if using replication)

- [ ] **Application metrics** (Prometheus + Grafana recommended):
  - [ ] Request rate
  - [ ] Error rate
  - [ ] Response time (p50, p95, p99)
  - [ ] JVM metrics (heap, GC)

### Log Aggregation

- [ ] **Centralized logging** (ELK Stack, Splunk, CloudWatch):
  - [ ] Application logs forwarded
  - [ ] Nginx access/error logs forwarded
  - [ ] PostgreSQL logs forwarded
  - [ ] Retention: 90 days minimum

- [ ] **Log dashboards created**:
  - [ ] Error rate by endpoint
  - [ ] Failed login attempts
  - [ ] Rate limit violations
  - [ ] Response time trends

### Alerting Rules

- [ ] **Critical alerts configured**:
  - [ ] Application down (5xx errors > 1% for 5 minutes)
  - [ ] Database connection failures
  - [ ] Disk space < 10%
  - [ ] Memory usage > 90%
  - [ ] SSL certificate expiring (< 30 days)

- [ ] **Warning alerts configured**:
  - [ ] High error rate (4xx > 5% for 10 minutes)
  - [ ] High rate limit violations (> 100/hour)
  - [ ] Slow response times (p95 > 2s)
  - [ ] Failed login attempts (> 10/minute)

---

## Backup & Disaster Recovery

### Backup Strategy

- [ ] **Automated daily backups**:
  ```bash
  # Cron job: /etc/cron.daily/oscal-backup
  #!/bin/bash
  BACKUP_DIR=/backup/oscal-tools
  DATE=$(date +%Y%m%d)

  # Database backup
  docker exec oscal-postgres-prod pg_dump \
    -U oscal_user oscal_production \
    > $BACKUP_DIR/db_backup_$DATE.sql

  # Application config backup
  tar -czf $BACKUP_DIR/config_backup_$DATE.tar.gz \
    /etc/oscal-tools/

  # Delete backups older than 30 days
  find $BACKUP_DIR -type f -mtime +30 -delete
  ```

- [ ] **Off-site backup configured**:
  - [ ] AWS S3, Azure Blob, or similar
  - [ ] Automated daily sync
  - [ ] Encryption at rest
  - [ ] Versioning enabled

- [ ] **Backup restoration tested**:
  ```bash
  # Test restore to staging environment
  psql -U oscal_user -h staging-db -d oscal_staging < db_backup_20251026.sql
  ```

### Disaster Recovery Plan

- [ ] **DR Plan documented**:
  - [ ] RTO (Recovery Time Objective): Target uptime restoration
  - [ ] RPO (Recovery Point Objective): Acceptable data loss window
  - [ ] Contact list for incident response
  - [ ] Step-by-step recovery procedures

- [ ] **DR Testing completed**:
  - [ ] Restore from backup tested successfully
  - [ ] Failover procedure tested
  - [ ] Data integrity verified after restore

---

## Compliance & Documentation

### Compliance

- [ ] **OWASP Top 10 (2021) addressed**:
  - See `docs/SECURITY-AUDIT-REPORT.md` for details

- [ ] **Security audit completed**:
  - [ ] Audit report reviewed: `docs/SECURITY-AUDIT-REPORT.md`
  - [ ] All critical findings addressed
  - [ ] Residual risks accepted and documented

- [ ] **Dependency vulnerabilities checked**:
  ```bash
  cd back-end
  mvn org.owasp:dependency-check-maven:check
  ```
  - [ ] No HIGH or CRITICAL vulnerabilities
  - [ ] All vulnerabilities reviewed and addressed

- [ ] **Data protection compliance**:
  - [ ] GDPR (if applicable): Privacy policy, consent, data deletion
  - [ ] CCPA (if applicable): Data disclosure, opt-out
  - [ ] HIPAA (if applicable): N/A for OSCAL tools

### Documentation

- [ ] **Production documentation complete**:
  - [ ] **Architecture diagram** updated
  - [ ] **API documentation** (Swagger/OpenAPI)
  - [ ] **Deployment guide** (this document)
  - [ ] **Security audit report** (`SECURITY-AUDIT-REPORT.md`)
  - [ ] **Incident response plan**
  - [ ] **Runbook** for common operations

- [ ] **User documentation**:
  - [ ] User guide
  - [ ] API usage examples
  - [ ] Troubleshooting guide
  - [ ] FAQ

- [ ] **Admin documentation**:
  - [ ] Server maintenance procedures
  - [ ] Backup and restore procedures
  - [ ] Monitoring and alerting setup
  - [ ] Security best practices

---

## Go-Live Checklist

### Final Verification

- [ ] **All stakeholders notified**:
  - [ ] Users informed of go-live date
  - [ ] Support team briefed
  - [ ] Management approval obtained

- [ ] **Change window scheduled**:
  - [ ] Maintenance window communicated
  - [ ] Rollback plan prepared
  - [ ] On-call team assigned

- [ ] **Final smoke tests passed**:
  - [ ] Registration
  - [ ] Login/Logout
  - [ ] OSCAL validation
  - [ ] OSCAL conversion
  - [ ] File upload
  - [ ] History retrieval

- [ ] **Performance baseline established**:
  - [ ] Response time metrics
  - [ ] Error rate metrics
  - [ ] Resource usage metrics

### Go-Live

- [ ] **Cutover to production**:
  - [ ] DNS switched to production server
  - [ ] SSL certificate active
  - [ ] Load balancer configured (if applicable)

- [ ] **Post-deployment monitoring** (first 24 hours):
  - [ ] No critical errors
  - [ ] Response times acceptable
  - [ ] User feedback positive
  - [ ] No security incidents

- [ ] **Rollback plan ready** (just in case):
  - [ ] Previous version backed up
  - [ ] Quick rollback procedure documented
  - [ ] Rollback tested in staging

---

## Post-Deployment Tasks

### Week 1

- [ ] **Monitor for issues**:
  - [ ] Check logs daily
  - [ ] Review error rates
  - [ ] User feedback

- [ ] **Tune performance**:
  - [ ] Database query optimization
  - [ ] Cache configuration
  - [ ] Resource allocation

- [ ] **Security hardening**:
  - [ ] Review security logs
  - [ ] Check for failed login attempts
  - [ ] Verify rate limiting effectiveness

### Month 1

- [ ] **Performance review**:
  - [ ] Analyze response time trends
  - [ ] Review resource usage
  - [ ] Plan capacity upgrades if needed

- [ ] **Security review**:
  - [ ] Review audit logs
  - [ ] Check for vulnerabilities
  - [ ] Update dependencies

- [ ] **Backup verification**:
  - [ ] Test backup restoration
  - [ ] Verify off-site backups
  - [ ] Review retention policy

### Ongoing

- [ ] **Monthly security updates**:
  - [ ] OS patches
  - [ ] Docker image updates
  - [ ] Dependency updates

- [ ] **Quarterly security audit**:
  - [ ] Penetration testing
  - [ ] Dependency vulnerability scan
  - [ ] Access control review

- [ ] **Annual DR test**:
  - [ ] Full disaster recovery simulation
  - [ ] Document lessons learned
  - [ ] Update DR plan

---

## Support & Contacts

### Emergency Contacts

- **Primary On-Call**: [Name/Phone/Email]
- **Secondary On-Call**: [Name/Phone/Email]
- **Database Admin**: [Name/Phone/Email]
- **Security Team**: [Name/Phone/Email]

### Escalation Procedure

1. **Level 1** (Minor issues): On-call engineer
2. **Level 2** (Major issues): Technical lead + Database admin
3. **Level 3** (Critical/Security): Management + Security team

### Support Resources

- **Documentation**: `/docs` directory
- **Runbook**: `docs/RUNBOOK.md`
- **Security Audit**: `docs/SECURITY-AUDIT-REPORT.md`
- **GitHub Issues**: https://github.com/RegScale/oscal-hub/issues

---

## Appendix A: Quick Reference Commands

### Application Management

```bash
# Start application
docker-compose -f docker-compose.prod.yml up -d

# Stop application
docker-compose -f docker-compose.prod.yml down

# Restart application
docker-compose -f docker-compose.prod.yml restart oscal-ux

# View logs
docker-compose -f docker-compose.prod.yml logs -f oscal-ux

# Check status
docker-compose -f docker-compose.prod.yml ps
```

### Database Management

```bash
# Database backup
docker exec oscal-postgres-prod pg_dump \
  -U oscal_user oscal_production > backup_$(date +%Y%m%d).sql

# Database restore
docker exec -i oscal-postgres-prod psql \
  -U oscal_user oscal_production < backup.sql

# Connect to database
docker exec -it -e PGPASSWORD='password' oscal-postgres-prod \
  psql -U oscal_user -d oscal_production

# Check database size
docker exec -e PGPASSWORD='password' oscal-postgres-prod \
  psql -U oscal_user -d oscal_production \
  -c "SELECT pg_size_pretty(pg_database_size('oscal_production'));"
```

### Health Checks

```bash
# Application health
curl https://your-domain.com/api/health

# Database health
docker inspect oscal-postgres-prod --format='{{.State.Health.Status}}'

# SSL certificate expiry
echo | openssl s_client -servername your-domain.com \
  -connect your-domain.com:443 2>/dev/null | \
  openssl x509 -noout -dates
```

---

## Appendix B: Rollback Procedure

### Quick Rollback (< 30 minutes)

1. **Stop current deployment**:
   ```bash
   docker-compose -f docker-compose.prod.yml down
   ```

2. **Checkout previous version**:
   ```bash
   git fetch --all
   git checkout <previous-tag>
   ```

3. **Restore database** (if schema changed):
   ```bash
   docker exec -i oscal-postgres-prod psql \
     -U oscal_user oscal_production < backup_before_deployment.sql
   ```

4. **Start previous version**:
   ```bash
   docker-compose -f docker-compose.prod.yml up -d
   ```

5. **Verify rollback**:
   ```bash
   curl https://your-domain.com/api/health
   ```

---

**Deployment Checklist Version**: 1.0.0
**Last Updated**: 2025-10-26
**Maintained By**: OSCAL Tools DevOps Team

---

**End of Deployment Checklist**
