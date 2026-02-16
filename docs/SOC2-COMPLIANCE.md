# SOC 2 Compliance Documentation

**Date**: February 2025
**Status**: Active

## Overview

OSCAL Hub implements security controls aligned with SOC 2 Type II Trust Service Criteria (TSC). This document provides a comprehensive mapping of implemented controls, identified gaps, and recommendations for achieving full compliance.

## Trust Service Criteria Categories

### CC6 - Logical and Physical Access Controls

Controls for authentication, authorization, and access management.

| Control | Status | Implementation |
|---------|--------|----------------|
| **CC6.1** - Logical Access Security Software | Implemented | JWT-based authentication with secure token generation. `JwtAuthenticationFilter` validates tokens on every request. |
| **CC6.2** - Access Removal | Implemented | User disable/delete functionality. Organization membership can be revoked. Access requests require admin approval. |
| **CC6.3** - Role-Based Access Control | Implemented | Three-tier role system: SUPER_ADMIN, ORG_ADMIN, USER. Spring Security `@PreAuthorize` enforces role-based access. |
| **CC6.6** - Logical Access Restrictions | Implemented | Multi-tenant architecture with organization-scoped data isolation. |
| **CC6.7** - Information Transmission | Implemented | HTTPS/TLS enforced. CORS configuration restricts allowed origins. |
| **CC6.8** - Multi-Factor Authentication | **Gap** | Not currently implemented. Users authenticate with username/password only. |

### CC7 - System Operations

Controls for monitoring, detection, and incident response.

| Control | Status | Implementation |
|---------|--------|----------------|
| **CC7.1** - Vulnerability Detection | Partial | Rate limiting (100 req/min), input validation, file type/size restrictions, OSCAL schema validation. |
| **CC7.2** - Anomaly Detection | Implemented | 27 audit event types with SIEM integration. SHA-256 integrity hashing. |
| **CC7.3** - Security Evaluation | Implemented | Health check system monitors database, storage, memory, CPU, disk space, and configuration. |
| **CC7.4** - Incident Response | Partial | Audit logging, SIEM webhooks, account lockout (5 attempts = 15 min lockout). |

### CC8 - Change Management

Controls for authorizing and managing system changes.

| Control | Status | Implementation |
|---------|--------|----------------|
| **CC8.1** - Change Authorization | Implemented | Administrative functions restricted to SUPER_ADMIN. Configuration changes are audit logged. |

### CC9 - Risk Mitigation

Controls for identifying and mitigating risks.

| Control | Status | Implementation |
|---------|--------|----------------|
| **CC9.1** - Risk Identification | **Gap** | File uploads validated for type and size, but no malware scanning. |
| **CC9.2** - Business Continuity | Partial | Health monitoring exists. Cloud deployment provides infrastructure resilience. DR procedures not documented. |

### Data Protection

| Control | Status | Implementation |
|---------|--------|----------------|
| **DP.1** - Encryption at Rest | Implemented | Cloud storage (Azure/GCS) provides default encryption. |
| **DP.2** - Encryption in Transit | Implemented | HTTPS/TLS for all communications. Database SSL connections. |
| **DP.3** - Input Validation | Implemented | File type validation, size limits, OSCAL schema validation, Spring validation annotations. |
| **DP.4** - Password Policy | Implemented | 8+ chars, uppercase, lowercase, number, special char. BCrypt hashing. |
| **DP.5** - Account Lockout | Implemented | 5 failed attempts = 15 minute lockout. SIEM notification on lockout. |

### Audit and Monitoring

| Control | Status | Implementation |
|---------|--------|----------------|
| **AM.1** - Audit Logging | Implemented | 27 event types. Includes timestamp, user, IP, details. SHA-256 integrity hash. |
| **AM.2** - Log Retention | Implemented | Configurable retention (90 days default). Automatic cleanup. Export functionality. |
| **AM.3** - SIEM Integration | Implemented | Webhook-based integration. Async delivery. Configurable retry logic. |

---

## Gap Analysis

### High Priority

#### 1. CC6.8 - Multi-Factor Authentication (MFA)

**Current State**: Users authenticate with username and password only.

**Risk**: If credentials are compromised, attackers can gain full access to user accounts.

**Recommendation**:
- Implement TOTP-based MFA using Google Authenticator or similar
- Require MFA for all admin accounts (SUPER_ADMIN, ORG_ADMIN)
- Consider optional MFA for regular users

**Effort**: Medium-High

#### 2. CC9.1 - Malware Scanning

**Current State**: File uploads are validated for type and size only.

**Risk**: Malicious files could be uploaded and stored, potentially compromising the system.

**Recommendation**:
- Integrate ClamAV for on-premise scanning
- Or use cloud-based scanning (VirusTotal API, Google Safe Browsing)
- Quarantine suspicious files before processing

**Effort**: Medium

### Medium Priority

#### 3. CC7.4 - Incident Response Procedures

**Current State**: Audit logging and SIEM integration exist, but documented procedures are not established.

**Recommendation**:
- Create incident response runbook
- Define escalation procedures
- Document common attack scenarios
- Conduct periodic incident response drills

**Effort**: Low

#### 4. CC9.2 - Disaster Recovery Documentation

**Current State**: Health monitoring exists but formal DR procedures are not documented.

**Recommendation**:
- Document disaster recovery procedures
- Define RTO/RPO targets
- Implement automated database backups
- Test recovery procedures periodically

**Effort**: Medium

#### 5. CC7.1 - Enhanced Vulnerability Management

**Current State**: Input validation exists but no automated vulnerability scanning.

**Recommendation**:
- Implement dependency scanning in CI/CD (Snyk, Dependabot)
- Add SAST tools for code security analysis
- Conduct periodic penetration testing

**Effort**: Medium

---

## Evidence Collection Guide

### For Auditors

#### Authentication & Access Control

1. **JWT Implementation**
   - `JwtAuthenticationFilter.java`
   - `JwtService.java`
   - `SecurityConfig.java`

2. **Role-Based Access**
   - `GlobalRole.java` enum
   - `OrganizationRole.java` enum
   - `@PreAuthorize` annotations on controllers

3. **Multi-Tenant Isolation**
   - Service layer organization-scoped queries
   - `getOrganizationId()` checks in services

#### Audit Logging

1. **Audit Event Types**
   - `AuditEventType.java` - 27 event types
   - `AuditService.java`
   - `AuditLog` entity

2. **SIEM Integration**
   - `SiemService.java`
   - `application.properties` - `siem.webhook.url`

3. **Log Integrity**
   - SHA-256 hash in `AuditLog.integrityHash`
   - Hash chain verification

#### Password & Account Security

1. **Password Policy**
   - `AuthService.java` - password validation
   - BCrypt configuration

2. **Account Lockout**
   - `User` entity - `failedAttempts`, `lockoutTime`
   - `AuthService.java` - lockout logic

---

## Security Dashboard

The Security Compliance Dashboard is available at `/admin/security` for SUPER_ADMIN users.

### Features

- **Compliance Overview**: Overall percentage with breakdown by category
- **Control Categories**: Expandable sections showing individual controls
- **Status Indicators**: Visual badges for Implemented/Partial/Gap
- **Gap Analysis**: Prioritized list with recommendations
- **Evidence Links**: References to implementation files

### API Endpoints

| Endpoint | Description |
|----------|-------------|
| `GET /api/admin/security/compliance-summary` | Overall compliance statistics |
| `GET /api/admin/security/controls` | All controls with status |
| `GET /api/admin/security/controls/{category}` | Controls by TSC category |
| `GET /api/admin/security/gaps` | Gap analysis with recommendations |

---

## Attestation Checklist

Before SOC 2 Type II attestation:

- [ ] Review all implemented controls against TSC criteria
- [ ] Address high-priority gaps (MFA, malware scanning)
- [ ] Document incident response procedures
- [ ] Establish disaster recovery procedures
- [ ] Conduct internal security review
- [ ] Export audit logs for review period
- [ ] Verify SIEM integration is operational
- [ ] Test account lockout functionality
- [ ] Review access control configurations
- [ ] Validate encryption settings

---

## Maintenance

This compliance documentation should be reviewed and updated:

1. **Quarterly**: Review control implementations for changes
2. **After Major Releases**: Update implementation details
3. **After Security Incidents**: Add learnings and new controls
4. **Before Audits**: Verify accuracy of all information

---

## Contact

For security compliance questions, contact the Security Team or SUPER_ADMIN users.
