# Deployment Scripts Guide

**Date**: October 26, 2025
**Status**: ✅ Complete

## Overview

This guide explains all the deployment scripts available for running OSCAL Tools locally and in production.

## Quick Start Scripts (Application + Monitoring)

### start-all.sh - Start Everything

Starts **all services** in one command:
- Application (frontend + backend)
- PostgreSQL database
- pgAdmin (database UI)
- Prometheus (metrics)
- Grafana (dashboards)

**Usage**:
```bash
./start-all.sh
```

**Output**:
```
=====================================
  OSCAL Tools - Start All Services
=====================================

[1/2] Starting application stack...
[2/2] Starting monitoring stack...
⏳ Waiting for services to be ready...

✓ All services started!

=====================================
  Service URLs
=====================================

Application:
  • Frontend:        http://localhost:3000
  • Backend API:     http://localhost:8080/api
  • API Health:      http://localhost:8080/actuator/health
  • API Docs:        http://localhost:8080/swagger-ui.html

Database:
  • PostgreSQL:      localhost:5432
  • pgAdmin:         http://localhost:5050
    - Email:         admin@oscal.local
    - Password:      admin

Monitoring:
  • Prometheus:      http://localhost:9090
  • Grafana:         http://localhost:3001
    - Username:      admin
    - Password:      admin
    - Dashboard:     OSCAL Tools → OSCAL Tools - Overview
```

### stop-all.sh - Stop Everything

Stops all running services.

**Usage**:
```bash
# Stop containers (preserves data)
./stop-all.sh

# Stop and remove containers (preserves data volumes)
./stop-all.sh --remove
```

**Options**:
- No flags - Stops containers, can restart later
- `--remove` or `-r` - Stops and removes containers (data volumes preserved)

### restart-all.sh - Restart Everything

Convenience script to stop and start all services.

**Usage**:
```bash
./restart-all.sh
```

This is equivalent to:
```bash
./stop-all.sh && ./start-all.sh
```

## Individual Component Scripts

### dev.sh - Development Mode

Starts **only the application** (no monitoring) with hot-reload for development:
- Backend: Maven with auto-restart
- Frontend: Next.js dev server with fast refresh

**Usage**:
```bash
./dev.sh
```

**When to use**:
- Active development with code changes
- Don't need monitoring/observability
- Want faster startup times

### start.sh - Production-like Mode

Starts **only the application** with Docker Compose:
- Frontend + backend in production mode
- PostgreSQL database
- pgAdmin

**Usage**:
```bash
./start.sh
```

**When to use**:
- Testing production build locally
- Don't need monitoring stack
- Want to match production environment

### stop.sh - Stop Application Only

Stops application services (not monitoring).

**Usage**:
```bash
./stop.sh
```

## Deployment Scripts

### local-deploy.sh - Full Local Deployment

Complete local production-like deployment with security checks.

**Usage**:
```bash
./local-deploy.sh
```

**Features**:
- Security scanning
- Production build
- Health checks
- Post-deployment verification
- See: `docs/LOCAL-DEPLOYMENT-GUIDE.md`

## Utility Scripts

### docker-security-scan.sh - Security Scanning

Scans Docker images for security vulnerabilities.

**Usage**:
```bash
./docker-security-scan.sh
```

**See**: `docs/DOCKER-SECURITY.md`

### trivy-scan.sh - Trivy Security Scanner

Comprehensive security scanning with Trivy.

**Usage**:
```bash
./trivy-scan.sh
```

**See**: `docs/TRIVY-SECURITY-SCANNING.md`

### coverage.sh - Test Coverage Report

Generates test coverage reports for all components.

**Usage**:
```bash
./coverage.sh
```

## Docker Compose Files

The scripts use these Docker Compose files:

### docker-compose.yml (Main Application)

**Services**:
- `postgres` - PostgreSQL database
- `oscal-ux` - Full-stack application (frontend + backend)
- `pgadmin` - Database management UI

**Usage**:
```bash
# Start application only
docker-compose up -d

# Stop application
docker-compose down

# View logs
docker-compose logs -f
```

### docker-compose-monitoring.yml (Monitoring Stack)

**Services**:
- `prometheus` - Metrics collection and storage
- `grafana` - Metrics visualization and dashboards

**Usage**:
```bash
# Start monitoring only
docker-compose -f docker-compose-monitoring.yml up -d

# Stop monitoring
docker-compose -f docker-compose-monitoring.yml down

# View logs
docker-compose -f docker-compose-monitoring.yml logs -f
```

## Common Workflows

### Development Workflow

**Starting development**:
```bash
# Option 1: App + monitoring (full observability)
./start-all.sh

# Option 2: App only (faster startup)
./dev.sh
```

**Making changes**:
- Backend changes: Auto-reload with `./dev.sh`
- Frontend changes: Hot reload with `./dev.sh`
- Database changes: Use pgAdmin at http://localhost:5050

**Stopping for the day**:
```bash
./stop-all.sh  # Or ./stop.sh if using dev.sh
```

### Testing Workflow

**Test production build locally**:
```bash
# 1. Build production image
./start.sh

# 2. Test the application
curl http://localhost:8080/api/health

# 3. Run tests
./coverage.sh

# 4. Clean up
./stop.sh
```

### Debugging Workflow

**When something goes wrong**:
```bash
# 1. Check all services
docker-compose ps
docker-compose -f docker-compose-monitoring.yml ps

# 2. View logs
docker-compose logs -f oscal-ux
docker-compose logs -f postgres

# 3. Check monitoring
# Open: http://localhost:3001 (Grafana)
# Look at errors, performance metrics

# 4. Restart everything
./restart-all.sh
```

### Monitoring Workflow

**Set up monitoring**:
```bash
# 1. Start everything
./start-all.sh

# 2. Open Grafana
# http://localhost:3001
# Login: admin/admin

# 3. Navigate to dashboard
# Dashboards → OSCAL Tools → OSCAL Tools - Overview

# 4. Check Prometheus
# http://localhost:9090
# Status → Targets (should show oscal-tools-api UP)
```

## Comparison Matrix

| Script | App | DB | Monitoring | Mode | Hot Reload |
|--------|-----|----|-----------| ------|-----------|
| `start-all.sh` | ✅ | ✅ | ✅ | Production | ❌ |
| `dev.sh` | ✅ | ✅ | ❌ | Development | ✅ |
| `start.sh` | ✅ | ✅ | ❌ | Production | ❌ |
| `local-deploy.sh` | ✅ | ✅ | ❌ | Production | ❌ |

## Environment Variables

All scripts respect these environment variables:

### Database
```bash
DB_NAME=oscal_dev              # Database name
DB_USERNAME=oscal_user         # Database user
DB_PASSWORD=oscal_dev_password # Database password
```

### Grafana
```bash
GRAFANA_ADMIN_USER=admin       # Grafana username
GRAFANA_ADMIN_PASSWORD=admin   # Grafana password
```

### pgAdmin
```bash
PGADMIN_EMAIL=admin@oscal.local  # pgAdmin email
PGADMIN_PASSWORD=admin           # pgAdmin password
```

**Set in** `.env` file:
```bash
# Create .env file in project root
cat > .env <<EOF
DB_NAME=my_database
DB_USERNAME=my_user
DB_PASSWORD=my_secure_password
GRAFANA_ADMIN_PASSWORD=my_grafana_password
EOF
```

## Port Reference

| Service | Port | URL |
|---------|------|-----|
| Frontend | 3000 | http://localhost:3000 |
| Backend API | 8080 | http://localhost:8080 |
| PostgreSQL | 5432 | localhost:5432 |
| pgAdmin | 5050 | http://localhost:5050 |
| Prometheus | 9090 | http://localhost:9090 |
| Grafana | 3001 | http://localhost:3001 |

## Data Persistence

All scripts preserve data in Docker volumes:

**Application data**:
- `oscal-data` - Application files
- `oscal-logs` - Application logs
- `postgres_dev_data` - PostgreSQL database
- `pgadmin_data` - pgAdmin configuration

**Monitoring data**:
- `prometheus_data` - Metrics data (30 days retention)
- `grafana_data` - Dashboards, users, settings

**To remove all data** (⚠️ irreversible):
```bash
docker-compose down -v
docker-compose -f docker-compose-monitoring.yml down -v
```

## Troubleshooting

### "Port already in use"

One or more ports are occupied:
```bash
# Find what's using the port
lsof -i :3000  # or :8080, :9090, etc.

# Stop all containers
./stop-all.sh --remove

# Try again
./start-all.sh
```

### "Cannot connect to database"

Database not ready yet:
```bash
# Check database status
docker-compose ps postgres

# Check database logs
docker-compose logs postgres

# Restart database
docker-compose restart postgres
```

### "Containers not starting"

Resource issues or conflicts:
```bash
# Clean up everything
./stop-all.sh --remove

# Prune Docker system
docker system prune -f

# Start fresh
./start-all.sh
```

### "Monitoring not showing data"

Prometheus not scraping or Grafana not configured:
```bash
# Check Prometheus targets
# Open: http://localhost:9090
# Go to: Status → Targets
# Verify: oscal-tools-api is UP

# Check Grafana datasource
# Open: http://localhost:3001
# Go to: Configuration → Data Sources → Prometheus
# Click: Test (should be green)

# Restart monitoring
docker-compose -f docker-compose-monitoring.yml restart
```

## Production Deployment

For production deployment (not local):

### Azure Deployment
See `docs/AZURE-DEPLOYMENT-GUIDE.md`

**Note**: Azure uses **Application Insights** for monitoring, not Prometheus/Grafana.

### Custom Production Deployment
See `docs/LOCAL-DEPLOYMENT-GUIDE.md` for production-ready local deployment.

## Best Practices

### Development

✅ **Do**:
- Use `./dev.sh` for active development
- Use `./start-all.sh` when you need monitoring
- Commit `.env.example` (not `.env`)
- Check logs when things fail

❌ **Don't**:
- Run multiple instances on the same ports
- Commit sensitive data in `.env`
- Use production credentials in development
- Skip health checks after starting

### Monitoring

✅ **Do**:
- Start monitoring with `./start-all.sh` regularly
- Check Grafana dashboard for insights
- Set up custom alerts for your use case
- Review metrics weekly

❌ **Don't**:
- Run monitoring 24/7 in development (uses resources)
- Ignore Prometheus alerts
- Skip monitoring in staging/production
- Use default passwords in production

### Data Management

✅ **Do**:
- Backup database regularly (see `scripts/backup/backup-database.sh`)
- Test restore procedures
- Monitor disk space for Docker volumes
- Clean up old containers/images periodically

❌ **Don't**:
- Delete volumes without backup
- Run `docker system prune -a` without understanding impact
- Ignore disk space warnings
- Mix production and development data

## Summary

**Quick commands**:
```bash
# Start everything (app + monitoring)
./start-all.sh

# Stop everything
./stop-all.sh

# Restart everything
./restart-all.sh

# Development mode (app only, hot reload)
./dev.sh

# Production mode (app only)
./start.sh
```

**Access points**:
- **App**: http://localhost:3000
- **API**: http://localhost:8080/api
- **Monitoring**: http://localhost:3001 (Grafana)
- **Metrics**: http://localhost:9090 (Prometheus)
- **Database**: http://localhost:5050 (pgAdmin)

---

**Created**: October 26, 2025
**Last Updated**: October 26, 2025
**Maintained By**: Development Team
