# OSCAL Hub Documentation

This directory contains comprehensive documentation for the OSCAL Hub project features, implementation details, and technical guides.

## Table of Contents

### Deployment

OSCAL Hub ships three first-class deploy options. Choose the one that
matches your environment:

- **[../deploy/compose/](../deploy/compose/)** — Docker Compose. Single VM,
  bundled Postgres + Nginx (TLS termination). Fastest path to running.
- **[../deploy/helm/oscal-hub/](../deploy/helm/oscal-hub/)** — Helm chart for
  Kubernetes. Bundles Postgres as a subchart by default; supports external
  managed databases and shared object storage.
- **[GCP-DEPLOYMENT-GUIDE.md](GCP-DEPLOYMENT-GUIDE.md)** — Cloud Run +
  Cloud SQL on GCP (the maintainer's hosted environment).

Supporting docs:

- **[CLI-DEPLOYMENT-GUIDE.md](CLI-DEPLOYMENT-GUIDE.md)** — running the
  OSCAL CLI tool standalone for batch / CI use.
- **[LOCAL-DEPLOYMENT-GUIDE.md](LOCAL-DEPLOYMENT-GUIDE.md)** — local dev
  stack via `./dev.sh`.
- **[CICD-BOOTSTRAP.md](CICD-BOOTSTRAP.md)** — one-time bootstrap for the
  GCP-hosted deployment's GitHub Actions pipeline.
- **[DEPLOYMENT-CHECKLIST.md](DEPLOYMENT-CHECKLIST.md)** — production
  go-live checklist (TLS, secrets, backups, monitoring).

### Feature Documentation

#### Authorization System
- **[AUTHORIZATION-FEATURE-SUMMARY.md](AUTHORIZATION-FEATURE-SUMMARY.md)** - Complete implementation guide for the authorization feature
  - Overview of templates and authorizations
  - API endpoints documentation
  - User interface components
  - Database schema
  - Testing results
  - Usage examples

- **[TEMPLATE-EDITOR-FIX.md](TEMPLATE-EDITOR-FIX.md)** - Technical details on template editor variable detection fixes
  - Problem identification
  - Solution implementation
  - Pattern matching details
  - UI enhancements
  - Testing and verification

- **[VARIABLE-DETECTION-SUMMARY.md](VARIABLE-DETECTION-SUMMARY.md)** - User-friendly guide for variable detection in templates
  - How to use the template editor
  - Valid variable naming
  - Visual feedback system
  - Quick testing guide

- **[VARIABLE-PATTERN-UPDATE.md](VARIABLE-PATTERN-UPDATE.md)** - Pattern matching updates to allow any content in variables
  - Problem with restrictive pattern
  - Solution to allow spaces, commas, special characters
  - FedRAMP template example
  - Test results showing 18 variables detected
  - Backward compatibility notes

- **[DOCUMENTATION-UPDATES-SUMMARY.md](DOCUMENTATION-UPDATES-SUMMARY.md)** - Summary of documentation updates for authorizations
  - Updates to README.md
  - Updates to USER_GUIDE.md
  - Updates to Hero.tsx splash page
  - Content overview and verification steps

### Security & Compliance Documentation

- **[SOC2-COMPLIANCE.md](SOC2-COMPLIANCE.md)** - SOC 2 Type II compliance documentation
  - Trust Service Criteria mapping
  - Control implementation status
  - Gap analysis and recommendations
  - Evidence collection guide
  - Attestation preparation checklist

- **[INCIDENT-RESPONSE.md](INCIDENT-RESPONSE.md)** - Incident response plan for GCP deployment
  - Incident classification (P1-P4 severity levels)
  - Response team roles and responsibilities
  - Detection and alerting configuration
  - Step-by-step response procedures
  - Communication plan templates
  - Runbooks for common incidents

- **[DISASTER-RECOVERY.md](DISASTER-RECOVERY.md)** - Disaster recovery procedures for GCP
  - RTO/RPO objectives by scenario
  - Backup strategies (Cloud SQL, Cloud Storage, container images)
  - Recovery procedures for various failure scenarios
  - DR testing schedule and procedures
  - Business continuity planning

### Technical Documentation

- **[JAVA_SPRING_UPGRADE_PLAN.md](JAVA_SPRING_UPGRADE_PLAN.md)** - Java and Spring Boot upgrade planning
  - Version upgrade strategy
  - Dependency updates
  - Migration considerations

## Documentation Standards

All documentation in this directory follows these standards:

1. **Markdown Format**: All files use Markdown (.md) format
2. **Clear Structure**: Each document includes:
   - Date and status at the top
   - Problem/Solution sections
   - Code examples
   - Testing results
   - Usage guides
3. **UPPERCASE Naming**: Files use descriptive UPPERCASE names with hyphens
4. **Comprehensive**: Documents are detailed enough for future developers to understand implementation decisions

## Contributing Documentation

When adding new features or making significant changes:

1. Create a new .md file in this `docs/` directory
2. Use a descriptive UPPERCASE name with hyphens
3. Follow the standard structure with date, status, problem, solution, examples, and testing
4. Update this README.md to include your new documentation in the appropriate section

## Quick Links

### For New Users
- **Start here**: [CLI-DEPLOYMENT-GUIDE.md](CLI-DEPLOYMENT-GUIDE.md) - Quick 2-minute setup for command-line usage
- **Local testing**: [LOCAL-DEPLOYMENT-GUIDE.md](LOCAL-DEPLOYMENT-GUIDE.md) - Run the full web interface locally
- **Production**: [SELF-HOSTED-DEPLOYMENT-GUIDE.md](SELF-HOSTED-DEPLOYMENT-GUIDE.md) - Self-hosted via Helm or Docker Compose

### For Developers
- Start with [AUTHORIZATION-FEATURE-SUMMARY.md](AUTHORIZATION-FEATURE-SUMMARY.md) to understand the full authorization system
- See [VARIABLE-PATTERN-UPDATE.md](VARIABLE-PATTERN-UPDATE.md) for details on variable naming flexibility

### For Users
- Read [VARIABLE-DETECTION-SUMMARY.md](VARIABLE-DETECTION-SUMMARY.md) for how to use the template editor
- Check [AUTHORIZATION-FEATURE-SUMMARY.md](AUTHORIZATION-FEATURE-SUMMARY.md) for complete usage examples

### For Security & Compliance
- Start with [SOC2-COMPLIANCE.md](SOC2-COMPLIANCE.md) for SOC 2 Type II control mapping
- See [INCIDENT-RESPONSE.md](INCIDENT-RESPONSE.md) for incident response procedures
- Review [DISASTER-RECOVERY.md](DISASTER-RECOVERY.md) for DR testing and recovery procedures

### For Maintenance
- Refer to [JAVA_SPRING_UPGRADE_PLAN.md](JAVA_SPRING_UPGRADE_PLAN.md) for upgrade planning
- Review [TEMPLATE-EDITOR-FIX.md](TEMPLATE-EDITOR-FIX.md) for technical implementation details

## Project Documentation Structure

```
docs/
├── README.md (this file)
│
├── Deployment Guides/
│   ├── CLI-DEPLOYMENT-GUIDE.md           # Command-line deployment
│   ├── LOCAL-DEPLOYMENT-GUIDE.md         # Local Docker deployment
│   └── SELF-HOSTED-DEPLOYMENT-GUIDE.md   # On-prem / customer-cloud (Helm + Compose)
│
├── Feature Documentation/
│   ├── AUTHORIZATION-FEATURE-SUMMARY.md
│   ├── TEMPLATE-EDITOR-FIX.md
│   ├── VARIABLE-DETECTION-SUMMARY.md
│   ├── VARIABLE-PATTERN-UPDATE.md
│   ├── COMPONENT-BUILDER-GUIDE.md
│   ├── DIGITAL-SIGNATURE-USER-GUIDE.md
│   └── USER-LOGO-FEATURE.md
│
├── Security & Compliance/
│   ├── SOC2-COMPLIANCE.md                  # SOC 2 Type II controls
│   ├── INCIDENT-RESPONSE.md                # Incident response plan
│   ├── DISASTER-RECOVERY.md                # DR procedures
│   ├── PRODUCTION-SECURITY-HARDENING-PLAN.md
│   ├── SECURITY-HARDENING-SUMMARY.md
│   ├── SECURITY-AUDIT-REPORT.md
│   ├── SECRETS-MANAGEMENT.md
│   ├── RATE-LIMITING.md
│   ├── SECURITY-HEADERS.md
│   ├── TLS-CONFIGURATION.md
│   ├── INPUT-VALIDATION-FILE-SECURITY.md
│   ├── ACCOUNT-SECURITY.md
│   ├── DOCKER-SECURITY.md
│   └── CORS-CONFIGURATION.md
│
├── Infrastructure & Operations/
│   ├── POSTGRESQL-MIGRATION.md
│   ├── MONITORING-GUIDE.md
│   ├── MONITORING-DASHBOARD-SUMMARY.md
│   ├── DEPLOYMENT-SCRIPTS-GUIDE.md
│   ├── DEPLOYMENT-CHECKLIST.md
│   └── PRODUCTION-READINESS-PLAN.md
│
└── Technical Documentation/
    ├── JAVA_SPRING_UPGRADE_PLAN.md
    ├── DEPENDENCY-SECURITY.md
    ├── DEPENDENCY-SCAN-STATUS.md
    ├── TRIVY-SECURITY-SCANNING.md
    ├── TRIVY-IMPLEMENTATION-SUMMARY.md
    ├── TEST-COVERAGE-IMPROVEMENTS.md
    └── DOCUMENTATION-ORGANIZATION.md
```

## Additional Resources

- Main project README: [../README.md](../README.md)
- User Guide: [../USER_GUIDE.md](../USER_GUIDE.md)
- Frontend Documentation: [../front-end/](../front-end/)
- Backend API: http://localhost:8080/swagger-ui/index.html (when running)

---

*Last Updated: February 2025*
