# OSCAL Hub Documentation

This directory contains comprehensive documentation for the OSCAL Hub project features, implementation details, and technical guides.

## Table of Contents

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
├── AUTHORIZATION-FEATURE-SUMMARY.md
├── TEMPLATE-EDITOR-FIX.md
├── VARIABLE-DETECTION-SUMMARY.md
├── VARIABLE-PATTERN-UPDATE.md
├── DOCUMENTATION-UPDATES-SUMMARY.md
├── DOCUMENTATION-ORGANIZATION.md
└── JAVA_SPRING_UPGRADE_PLAN.md
```

## Additional Resources

- Main project README: [../README.md](../README.md)
- User Guide: [../USER_GUIDE.md](../USER_GUIDE.md)
- Frontend Documentation: [../front-end/](../front-end/)
- Backend API: http://localhost:8080/swagger-ui/index.html (when running)

---

*Last Updated: October 19, 2025*
