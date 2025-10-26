# Production Security Hardening Plan

**Date:** 2025-10-26
**Status:** In Progress
**Author:** Security Hardening Initiative

## Executive Summary

This document outlines a comprehensive security hardening plan for the OSCAL Tools system (CLI, Back-end API, and Front-end) to prepare it for production deployment. The plan addresses critical security vulnerabilities, implements defense-in-depth strategies, and establishes secure operational practices.

## Current Security Posture

### Strengths
- ✅ BCrypt password hashing implemented (`SecurityConfig.java:39`)
- ✅ JWT-based authentication with token validation
- ✅ Stateless session management
- ✅ Input validation using `@Valid` annotations
- ✅ Environment files (`.env`) properly excluded from Git

### Critical Vulnerabilities Identified

| ID | Severity | Issue | Impact |
|----|----------|-------|--------|
| SEC-01 | **CRITICAL** | Hardcoded JWT secret in `application.properties` | Token compromise, authentication bypass |
| SEC-02 | **HIGH** | H2 console exposed without authentication | Database access, data breach |
| SEC-03 | **HIGH** | Swagger UI publicly accessible | API enumeration, information disclosure |
| SEC-04 | **CRITICAL** | No HTTPS/TLS configuration | Man-in-the-middle attacks, credential theft |
| SEC-05 | **HIGH** | Empty database password | Unauthorized database access |
| SEC-06 | **HIGH** | No rate limiting on auth endpoints | Brute force attacks, credential stuffing |
| SEC-07 | **MEDIUM** | Missing security headers | XSS, clickjacking, MIME sniffing attacks |
| SEC-08 | **MEDIUM** | CORS limited to localhost only | Cannot deploy to production domains |
| SEC-09 | **HIGH** | Docker containers run as root | Privilege escalation risk |
| SEC-10 | **MEDIUM** | No audit logging for security events | Cannot detect or investigate breaches |
| SEC-11 | **MEDIUM** | Limited file upload validation | Malicious file uploads, DoS |
| SEC-12 | **LOW** | CSRF protection disabled | Cross-site request forgery |
| SEC-13 | **MEDIUM** | No account lockout mechanism | Brute force attacks |
| SEC-14 | **LOW** | No password complexity requirements | Weak password attacks |

## Hardening Implementation Plan

### Priority 1: Critical Security Issues (Weeks 1-2)

#### 1. Secrets Management ⚡ **CRITICAL**
**Objective:** Externalize all secrets from configuration files and implement secure secret management.

**Tasks:**
- [ ] Generate cryptographically secure JWT secret (256+ bits)
- [ ] Move JWT secret to environment variables
- [ ] Implement database password
- [ ] Create `.env.example` template
- [ ] Add secret rotation documentation
- [ ] Create separate configurations for dev/staging/prod environments
- [ ] Document secret generation process

**Files to Modify:**
- `back-end/src/main/resources/application.properties`
- `back-end/src/main/resources/application-dev.properties` (new)
- `back-end/src/main/resources/application-prod.properties` (new)
- `.env.example` (update)
- `docs/SECRETS-MANAGEMENT.md` (new)

**Success Criteria:**
- No hardcoded secrets in any committed files
- Secrets loaded from environment variables
- Different secrets for dev/prod environments
- Documentation for secret rotation process

---

#### 2. Rate Limiting ⚡ **HIGH**
**Objective:** Prevent brute force attacks and API abuse through intelligent rate limiting.

**Tasks:**
- [ ] Add Bucket4j dependency to `pom.xml`
- [ ] Implement rate limiting filter for authentication endpoints
- [ ] Configure rate limits: 5 login attempts per minute per IP
- [ ] Configure rate limits: 10 registration attempts per hour per IP
- [ ] Add global API rate limiting (100 requests/minute per user)
- [ ] Implement rate limit response headers (X-RateLimit-*)
- [ ] Add rate limit exceeded error responses
- [ ] Create rate limiting configuration properties

**Files to Create/Modify:**
- `back-end/pom.xml` (add Bucket4j dependency)
- `back-end/src/main/java/gov/nist/oscal/tools/api/config/RateLimitConfig.java` (new)
- `back-end/src/main/java/gov/nist/oscal/tools/api/filter/RateLimitFilter.java` (new)
- `back-end/src/main/resources/application.properties` (add rate limit configs)
- `docs/RATE-LIMITING.md` (new)

**Success Criteria:**
- Login attempts limited to prevent brute force
- Rate limit headers included in responses
- Users receive clear error messages when rate limited
- Rate limits configurable per environment

---

#### 3. Security Headers ⚡ **MEDIUM**
**Objective:** Implement comprehensive HTTP security headers to prevent common web attacks.

**Tasks:**
- [ ] Add Strict-Transport-Security (HSTS) header
- [ ] Implement Content-Security-Policy (CSP)
- [ ] Add X-Content-Type-Options: nosniff
- [ ] Add X-Frame-Options: DENY (with exceptions)
- [ ] Add X-XSS-Protection: 1; mode=block
- [ ] Add Referrer-Policy: strict-origin-when-cross-origin
- [ ] Add Permissions-Policy header
- [ ] Create security headers filter
- [ ] Configure environment-specific header values

**Files to Create/Modify:**
- `back-end/src/main/java/gov/nist/oscal/tools/api/config/SecurityHeadersConfig.java` (new)
- `back-end/src/main/java/gov/nist/oscal/tools/api/filter/SecurityHeadersFilter.java` (new)
- `back-end/src/main/java/gov/nist/oscal/tools/api/config/SecurityConfig.java` (update)

**Success Criteria:**
- All security headers present in HTTP responses
- CSP prevents inline scripts and unauthorized resources
- Headers verified using security scanners (securityheaders.com)
- No security header warnings in browser console

---

#### 4. Environment-Based Security ⚡ **HIGH**
**Objective:** Disable development-only features in production environments.

**Tasks:**
- [ ] Create Spring profiles: dev, staging, prod
- [ ] Disable H2 console in production (`@Profile("!prod")`)
- [ ] Require authentication for Swagger UI in production
- [ ] Create profile-specific application properties
- [ ] Add profile detection and logging at startup
- [ ] Update Docker configuration for production profile
- [ ] Document profile usage in deployment guide

**Files to Create/Modify:**
- `back-end/src/main/resources/application-dev.properties` (new)
- `back-end/src/main/resources/application-staging.properties` (new)
- `back-end/src/main/resources/application-prod.properties` (new)
- `back-end/src/main/java/gov/nist/oscal/tools/api/config/H2ConsoleConfig.java` (new)
- `back-end/src/main/java/gov/nist/oscal/tools/api/config/SwaggerConfig.java` (update)
- `back-end/src/main/java/gov/nist/oscal/tools/api/config/SecurityConfig.java` (update)
- `Dockerfile` (update environment)
- `docker-compose.yml` (update environment)

**Success Criteria:**
- H2 console not accessible in production
- Swagger UI requires authentication in production
- Active profile logged at application startup
- Cannot accidentally deploy dev configuration to production

---

### Priority 2: Enhanced Security Features (Weeks 3-4)

#### 5. HTTPS/TLS Configuration 🔒
**Objective:** Encrypt all data in transit using TLS 1.2+.

**Tasks:**
- [ ] Generate self-signed certificates for development
- [ ] Configure Spring Boot SSL/TLS support
- [ ] Add redirect from HTTP to HTTPS
- [ ] Update CORS configuration for HTTPS origins
- [ ] Configure TLS protocols (disable TLS 1.0, 1.1)
- [ ] Configure strong cipher suites
- [ ] Document certificate installation for production
- [ ] Update frontend API URLs to use HTTPS

**Files to Create/Modify:**
- `back-end/src/main/resources/application-prod.properties` (SSL config)
- `back-end/src/main/java/gov/nist/oscal/tools/api/config/HttpsConfig.java` (new)
- `back-end/src/main/java/gov/nist/oscal/tools/api/config/SecurityConfig.java` (update CORS)
- `docs/TLS-CONFIGURATION.md` (new)
- `certs/README.md` (new - certificate management)

---

#### 6. Input Validation & File Security 🛡️
**Objective:** Prevent malicious file uploads and injection attacks.

**Tasks:**
- [ ] Implement file type validation (whitelist: XML, JSON, YAML only)
- [ ] Add file content validation (magic number verification)
- [ ] Implement file size limits per file type
- [ ] Sanitize filenames (prevent directory traversal)
- [ ] Validate base64 logo sizes (prevent memory exhaustion)
- [ ] Add content-type validation
- [ ] Implement virus scanning integration (optional)
- [ ] Add XSS prevention for user-generated content
- [ ] Validate OSCAL document structure before processing

**Files to Create/Modify:**
- `back-end/src/main/java/gov/nist/oscal/tools/api/validation/FileValidator.java` (new)
- `back-end/src/main/java/gov/nist/oscal/tools/api/validation/FileTypeValidator.java` (new)
- `back-end/src/main/java/gov/nist/oscal/tools/api/exception/InvalidFileException.java` (new)
- `back-end/src/main/java/gov/nist/oscal/tools/api/controller/*Controller.java` (update all file upload endpoints)

---

#### 7. Account Security 🔐
**Objective:** Strengthen account protection against unauthorized access.

**Tasks:**
- [ ] Implement password complexity requirements (min 12 chars, mixed case, numbers, symbols)
- [ ] Add account lockout after 5 failed login attempts
- [ ] Implement login attempt tracking (store in database)
- [ ] Add account unlock mechanism (time-based or admin)
- [ ] Create password strength validator
- [ ] Add "forgot password" recovery flow
- [ ] Implement email verification for registration
- [ ] Add password history (prevent reuse of last 5 passwords)
- [ ] Implement session timeout and token refresh

**Files to Create/Modify:**
- `back-end/src/main/java/gov/nist/oscal/tools/api/validation/PasswordValidator.java` (new)
- `back-end/src/main/java/gov/nist/oscal/tools/api/entity/LoginAttempt.java` (new)
- `back-end/src/main/java/gov/nist/oscal/tools/api/repository/LoginAttemptRepository.java` (new)
- `back-end/src/main/java/gov/nist/oscal/tools/api/service/AccountLockoutService.java` (new)
- `back-end/src/main/java/gov/nist/oscal/tools/api/service/AuthService.java` (update)
- `back-end/src/main/java/gov/nist/oscal/tools/api/controller/AuthController.java` (update)

---

#### 8. Audit Logging 📋
**Objective:** Comprehensive logging of security events for monitoring and forensics.

**Tasks:**
- [ ] Implement security audit logger
- [ ] Log all authentication events (success/failure with username, IP, timestamp)
- [ ] Log authorization failures (403 responses)
- [ ] Log sensitive operations (file uploads, conversions, profile changes)
- [ ] Include request context (IP, user agent, session ID)
- [ ] Implement structured logging (JSON format)
- [ ] Add log rotation and retention policies
- [ ] Create audit log analysis documentation
- [ ] Integrate with centralized logging (optional: ELK stack)

**Files to Create/Modify:**
- `back-end/src/main/java/gov/nist/oscal/tools/api/audit/AuditLogger.java` (new)
- `back-end/src/main/java/gov/nist/oscal/tools/api/audit/AuditEvent.java` (new)
- `back-end/src/main/java/gov/nist/oscal/tools/api/filter/AuditLoggingFilter.java` (new)
- `back-end/src/main/resources/logback-spring.xml` (new - logging config)
- `docs/AUDIT-LOGGING.md` (new)

---

### Priority 3: Infrastructure & Operations (Weeks 5-6)

#### 9. Docker Security 🐳
**Objective:** Harden Docker containers to minimize attack surface.

**Tasks:**
- [ ] Create non-root user in Dockerfile
- [ ] Run application as non-root user
- [ ] Use specific image versions (not `latest`)
- [ ] Implement multi-stage builds with minimal runtime images
- [ ] Remove unnecessary packages from runtime image
- [ ] Add Docker security scanning to CI/CD
- [ ] Scan base images for CVEs
- [ ] Implement read-only filesystem where possible
- [ ] Add security options to docker-compose.yml
- [ ] Document Docker security best practices

**Files to Modify:**
- `Dockerfile` (complete rewrite for security)
- `docker-compose.yml` (add security options)
- `.github/workflows/docker-security-scan.yml` (new - if using GitHub Actions)
- `docs/DOCKER-SECURITY.md` (new)

---

#### 10. Production Database 🗄️
**Objective:** Replace H2 with production-grade PostgreSQL database.

**Tasks:**
- [ ] Add PostgreSQL dependency to pom.xml
- [ ] Create PostgreSQL configuration profile
- [ ] Implement database connection pooling (HikariCP)
- [ ] Enable SSL for database connections
- [ ] Implement database migration scripts (Flyway/Liquibase)
- [ ] Configure database backup strategy
- [ ] Implement connection encryption
- [ ] Add database health checks
- [ ] Document database setup and migration
- [ ] Update Docker Compose to include PostgreSQL service

**Files to Create/Modify:**
- `back-end/pom.xml` (add PostgreSQL, Flyway dependencies)
- `back-end/src/main/resources/application-prod.properties` (PostgreSQL config)
- `back-end/src/main/resources/db/migration/V1__initial_schema.sql` (new)
- `docker-compose.yml` (add PostgreSQL service)
- `docker-compose-prod.yml` (new - production configuration)
- `docs/DATABASE-MIGRATION.md` (new)

---

#### 11. Dependency Security 🔍
**Objective:** Continuously monitor and patch vulnerable dependencies.

**Tasks:**
- [ ] Add OWASP Dependency-Check Maven plugin
- [ ] Configure dependency check to fail on high/critical CVEs
- [ ] Set up automated vulnerability scanning in CI/CD
- [ ] Enable GitHub Dependabot alerts
- [ ] Create dependency update policy
- [ ] Document vulnerability remediation process
- [ ] Schedule regular dependency updates (monthly)
- [ ] Add SBOM (Software Bill of Materials) generation

**Files to Create/Modify:**
- `pom.xml` (add OWASP Dependency-Check plugin)
- `back-end/pom.xml` (add dependency check)
- `.github/dependabot.yml` (new - if using GitHub)
- `docs/DEPENDENCY-SECURITY.md` (new)

---

#### 12. CORS Enhancement 🌐
**Objective:** Configure CORS for production deployments while maintaining security.

**Tasks:**
- [ ] Make CORS origins configurable via environment variables
- [ ] Create separate CORS profiles for dev/staging/prod
- [ ] Remove all wildcard (`*`) configurations
- [ ] Restrict allowed origins to specific production domains
- [ ] Configure CORS preflight cache
- [ ] Document CORS configuration for different environments
- [ ] Add CORS testing suite

**Files to Modify:**
- `back-end/src/main/java/gov/nist/oscal/tools/api/config/SecurityConfig.java`
- `back-end/src/main/java/gov/nist/oscal/tools/api/config/CorsConfig.java` (new)
- `back-end/src/main/resources/application.properties`
- `back-end/src/main/resources/application-prod.properties`

---

## Additional Security Recommendations

### 13. JWT Token Revocation
- Implement token blacklist/whitelist mechanism
- Add token revocation on password change
- Implement refresh token rotation
- Add "logout all sessions" functionality

### 14. API Documentation Security
- Add authentication examples to Swagger
- Document rate limits in API documentation
- Add security best practices section
- Include error response examples

### 15. Security Testing
- Set up automated security testing (OWASP ZAP)
- Conduct regular penetration testing
- Implement security regression tests
- Add security test cases to CI/CD

### 16. Monitoring & Alerting
- Integrate with SIEM or monitoring tools
- Set up alerts for suspicious activities
- Implement real-time threat detection
- Create security dashboard

### 17. Backup & Recovery
- Implement automated encrypted backups
- Test disaster recovery procedures
- Document backup retention policy
- Implement point-in-time recovery

### 18. Incident Response
- Create incident response plan
- Document security contact information
- Establish escalation procedures
- Conduct incident response drills

---

## Implementation Timeline

| Week | Focus | Tasks |
|------|-------|-------|
| 1 | Critical Security | Tasks 1-2: Secrets Management, Rate Limiting |
| 2 | Critical Security | Tasks 3-4: Security Headers, Environment-Based Security |
| 3 | Enhanced Security | Tasks 5-6: HTTPS/TLS, Input Validation |
| 4 | Enhanced Security | Tasks 7-8: Account Security, Audit Logging |
| 5 | Infrastructure | Tasks 9-10: Docker Security, PostgreSQL Migration |
| 6 | Infrastructure | Tasks 11-12: Dependency Security, CORS Enhancement |
| 7-8 | Additional Items | Tasks 13-18: Token Revocation, Testing, Monitoring |

---

## Testing Strategy

### Security Testing Checklist
- [ ] Automated security scans (OWASP ZAP, Burp Suite)
- [ ] Manual penetration testing
- [ ] Dependency vulnerability scanning
- [ ] Docker image security scanning
- [ ] TLS/SSL configuration testing
- [ ] Authentication bypass testing
- [ ] Authorization testing
- [ ] Input validation testing
- [ ] Rate limiting testing
- [ ] CORS policy testing

### Compliance Verification
- [ ] OWASP Top 10 compliance
- [ ] CIS Security Benchmarks
- [ ] NIST Security Controls (relevant to OSCAL project)
- [ ] Security headers validation (securityheaders.com)

---

## Success Metrics

1. **Zero high/critical vulnerabilities** in security scans
2. **100% of secrets externalized** from codebase
3. **All production features** require authentication
4. **Rate limiting active** on all auth endpoints
5. **TLS 1.2+ enforced** for all connections
6. **Security headers** scoring A+ on securityheaders.com
7. **Comprehensive audit logs** for all security events
8. **Docker containers** running as non-root users
9. **Database encryption** at rest and in transit
10. **Automated security testing** in CI/CD pipeline

---

## Risk Assessment

| Risk | Likelihood | Impact | Mitigation |
|------|------------|--------|------------|
| JWT secret compromise | Medium | Critical | Task 1: Externalize and rotate secrets |
| Brute force attack | High | High | Task 2: Rate limiting |
| Man-in-the-middle | High | Critical | Task 5: HTTPS/TLS |
| Malicious file upload | Medium | High | Task 6: Input validation |
| Account compromise | Medium | High | Task 7: Account security |
| Dependency vulnerability | Medium | Medium | Task 11: Dependency scanning |
| Container escape | Low | Critical | Task 9: Docker security |
| Data breach | Low | Critical | Task 10: Database encryption |

---

## Rollback Plan

For each implementation:
1. Create feature branch for security changes
2. Test thoroughly in development environment
3. Deploy to staging for integration testing
4. Maintain rollback capability for 48 hours post-deployment
5. Document rollback procedures in deployment guide

---

## References

- [OWASP Top 10](https://owasp.org/www-project-top-ten/)
- [NIST Cybersecurity Framework](https://www.nist.gov/cyberframework)
- [Spring Security Documentation](https://spring.io/projects/spring-security)
- [Docker Security Best Practices](https://docs.docker.com/engine/security/)
- [CWE Top 25](https://cwe.mitre.org/top25/)

---

## Change Log

| Date | Version | Changes | Author |
|------|---------|---------|--------|
| 2025-10-26 | 1.0 | Initial security hardening plan created | Security Team |

---

**Next Steps:** Begin implementation with Task 1 - Secrets Management
