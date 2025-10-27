# OSCAL Hub Documentation

This directory contains comprehensive documentation for the OSCAL Hub project features, implementation details, and technical guides.

## Table of Contents

### Deployment Guides

- **[CLI-DEPLOYMENT-GUIDE.md](CLI-DEPLOYMENT-GUIDE.md)** - Complete guide for running OSCAL CLI in command-line mode
  - Quick installation (2 minutes)
  - Batch processing and automation
  - CI/CD pipeline integration
  - Workflow examples and scripts
  - Troubleshooting CLI-specific issues
  - Comparison with web and Azure deployments

- **[LOCAL-DEPLOYMENT-GUIDE.md](LOCAL-DEPLOYMENT-GUIDE.md)** - Deploy the full stack locally with Docker
  - Web interface + REST API + CLI
  - Quick start (5 minutes)
  - Database management
  - Local development setup
  - Docker Compose configuration

- **[AZURE-DEPLOYMENT-GUIDE.md](AZURE-DEPLOYMENT-GUIDE.md)** - Production deployment to Azure cloud
  - Infrastructure as Code (Terraform)
  - CI/CD with GitHub Actions
  - Azure Blob Storage integration
  - PostgreSQL database
  - Security and monitoring

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
- **Production**: [AZURE-DEPLOYMENT-GUIDE.md](AZURE-DEPLOYMENT-GUIDE.md) - Deploy to Azure cloud

### For Developers
- Start with [AUTHORIZATION-FEATURE-SUMMARY.md](AUTHORIZATION-FEATURE-SUMMARY.md) to understand the full authorization system
- See [VARIABLE-PATTERN-UPDATE.md](VARIABLE-PATTERN-UPDATE.md) for details on variable naming flexibility

### For Users
- Read [VARIABLE-DETECTION-SUMMARY.md](VARIABLE-DETECTION-SUMMARY.md) for how to use the template editor
- Check [AUTHORIZATION-FEATURE-SUMMARY.md](AUTHORIZATION-FEATURE-SUMMARY.md) for complete usage examples

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
│   └── AZURE-DEPLOYMENT-GUIDE.md         # Azure cloud deployment
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
├── Security & Production/
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

*Last Updated: October 19, 2025*
