# Operational Runbooks for OSCAL Tools

## Overview

This directory contains operational runbooks for managing and troubleshooting the OSCAL Tools application. Each runbook provides step-by-step guidance for specific scenarios.

## When to Use These Runbooks

- **During Incidents**: Follow runbooks when alerts fire
- **For Maintenance**: Use deployment and backup procedures
- **For Training**: New team members should read all runbooks
- **For Improvement**: Update after incidents to capture learnings

## Available Runbooks

### Critical (Immediate Action Required)

| Runbook | Alert | When to Use |
|---------|-------|-------------|
| [SERVICE-DOWN.md](SERVICE-DOWN.md) | ServiceDown | Application is unreachable |
| [DATABASE-CONNECTION-ISSUES.md](DATABASE-CONNECTION-ISSUES.md) | DatabaseDown | Cannot connect to database |
| [HIGH-ERROR-RATE.md](HIGH-ERROR-RATE.md) | HighErrorRate | >10% of requests failing |

### Warning (Investigate Soon)

| Runbook | Alert | When to Use |
|---------|-------|-------------|
| [HIGH-MEMORY-USAGE.md](HIGH-MEMORY-USAGE.md) | HighMemoryUsage | JVM heap >85% |
| [HIGH-RESPONSE-TIME.md](HIGH-RESPONSE-TIME.md) | HighResponseTime | p95 latency >2s |
| [HIGH-CPU-USAGE.md](HIGH-CPU-USAGE.md) | HighCPUUsage | CPU >80% |
| [DATABASE-CONNECTION-POOL.md](DATABASE-CONNECTION-POOL.md) | DatabaseConnectionPoolNearlyExhausted | >90% connections in use |

### Operational Procedures

| Runbook | When to Use |
|---------|-------------|
| [DEPLOYMENT-PROCEDURE.md](DEPLOYMENT-PROCEDURE.md) | Deploying new versions |
| [BACKUP-RESTORE.md](BACKUP-RESTORE.md) | Backing up or restoring data |
| [STARTUP-SHUTDOWN.md](STARTUP-SHUTDOWN.md) | Starting or stopping services |
| [ROLLBACK-PROCEDURE.md](ROLLBACK-PROCEDURE.md) | Rolling back failed deployments |

## Runbook Template

All runbooks follow this structure:

1. **Alert Details**: Alert name, severity, threshold
2. **Symptoms**: What you'll observe
3. **Impact**: Effect on users/business/data
4. **Diagnosis Steps**: How to identify the problem
5. **Common Causes & Solutions**: Known issues and fixes
6. **Resolution Steps**: Step-by-step recovery procedures
7. **Verification**: How to confirm it's fixed
8. **Prevention**: How to avoid in the future
9. **Escalation**: When and who to contact
10. **Post-Incident**: Follow-up actions

## Quick Reference

### Emergency Contacts

| Role | Contact | Escalation Time |
|------|---------|-----------------|
| On-Call Engineer | [Add contact] | Immediate |
| Platform Lead | [Add contact] | 15 minutes |
| Database Administrator | [Add contact] | 30 minutes |
| Security Team | [Add contact] | For security incidents |

### Key URLs

| Service | URL | Auth Required |
|---------|-----|---------------|
| Application | http://localhost:8080 | Yes |
| Health Check | http://localhost:8080/actuator/health | No |
| Metrics | http://localhost:8080/actuator/prometheus | Yes |
| Prometheus | http://localhost:9090 | No |
| Grafana | http://localhost:3001 | Yes (admin/admin) |
| PostgreSQL | localhost:5432 | Yes |

### Common Commands

#### Check Service Status
```bash
# Health check
curl http://localhost:8080/actuator/health | jq

# Process status
ps aux | grep java | grep oscal

# Docker status
docker-compose ps
```

#### View Logs
```bash
# Application logs (last 100 lines)
tail -100 back-end/logs/spring.log

# Docker logs
docker-compose logs --tail=100 oscal-ux

# Follow logs in real-time
docker-compose logs -f oscal-ux
```

#### Restart Services
```bash
# Quick restart
docker-compose restart oscal-ux

# Full restart
docker-compose down && docker-compose up -d

# Restart specific service
docker-compose restart postgres
```

#### Database Operations
```bash
# Connect to database
psql -h localhost -U oscal_user -d oscal_dev

# Check database status
docker-compose ps postgres

# View database logs
docker-compose logs postgres
```

### Quick Diagnostics

**Is the service running?**
```bash
curl http://localhost:8080/actuator/health
# Expected: {"status":"UP"}
```

**Are metrics being collected?**
```bash
curl http://localhost:9090/api/v1/targets
# Check oscal-tools-api is UP
```

**Is the database accessible?**
```bash
docker-compose exec postgres pg_isready
# Expected: accepting connections
```

**How much memory is being used?**
```bash
curl -s http://localhost:8080/actuator/metrics/jvm.memory.used | jq
```

**What's the current error rate?**
- Open Grafana: http://localhost:3001
- Navigate to OSCAL Tools dashboard
- Check "Error Rate" panel

## Incident Response Process

### 1. Detect (0-2 minutes)
- Alert fires in Prometheus/Grafana
- Identify severity (Critical/Warning/Info)
- Locate relevant runbook

### 2. Acknowledge (2-5 minutes)
- Confirm alert is valid
- Notify team via Slack/PagerDuty
- Open incident ticket

### 3. Diagnose (5-15 minutes)
- Follow diagnosis steps in runbook
- Collect logs and metrics
- Identify root cause

### 4. Resolve (15-60 minutes)
- Execute resolution steps
- Verify fix is working
- Monitor for recurrence

### 5. Document (After resolution)
- Complete incident report
- Update runbook if needed
- Share lessons learned

## Best Practices

### Before Using a Runbook
1. **Read completely** before executing commands
2. **Understand impact** of each action
3. **Have rollback plan** ready
4. **Communicate** with team

### During Execution
1. **Document actions** taken
2. **Take screenshots** of errors
3. **Save logs** before clearing
4. **Test in staging** if possible

### After Incident
1. **Write post-mortem** within 48 hours
2. **Update runbook** with new findings
3. **Automate** repetitive fixes
4. **Train team** on new procedures

## Contributing

### Adding a New Runbook

1. Copy template from [RUNBOOK-TEMPLATE.md](RUNBOOK-TEMPLATE.md)
2. Fill in all sections
3. Test procedures in non-production
4. Get peer review
5. Submit PR with updates

### Updating Existing Runbook

1. Note what changed during incident
2. Update relevant sections
3. Add to "Last Updated" and changelog
4. Get review from incident responders
5. Submit PR

## Runbook Maintenance

- **Review**: Quarterly review of all runbooks
- **Test**: Annual testing of all procedures
- **Update**: After each incident
- **Archive**: Remove outdated runbooks

---

**Last Updated**: October 26, 2025
**Maintained By**: Platform/SRE Team
**Review Cycle**: Quarterly
