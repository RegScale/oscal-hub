# Environment-Based Security Configuration Guide

**Date:** 2025-10-26
**Status:** Implemented
**Component:** Application Security

## Overview

This document describes the environment-based security configuration system for OSCAL Tools. The application uses Spring Boot profiles to automatically enable or disable security features based on the deployment environment.

## Purpose

Different environments have different security requirements:
- **Development:** Security restrictions relaxed for ease of development
- **Staging:** Security enabled with debugging features for testing
- **Production:** Maximum security, all debugging features disabled

Environment-based configuration ensures:
- Developers can work efficiently without security friction
- Staging mirrors production security for realistic testing
- Production is hardened by default, preventing accidental insecure deployments

## Supported Environments

### Development Profile (`dev`)

**Purpose:** Local development and testing

**Security Posture:** Relaxed

**Features:**
- ✅ H2 Console enabled at `/h2-console`
- ✅ Swagger UI enabled at `/swagger-ui.html`
- ❌ Security headers disabled (or minimal)
- ❌ HTTPS not required
- ❌ Rate limiting disabled (optional to enable for testing)
- ❌ Audit logging disabled

**Database:** H2 file-based (`./data/oscal-history-dev`)

**Activation:**
```bash
export SPRING_PROFILES_ACTIVE=dev
./dev.sh
```

---

### Staging Profile (`staging`)

**Purpose:** Pre-production testing and QA

**Security Posture:** Moderate

**Features:**
- ⚠️  H2 Console disabled (or warn if enabled)
- ✅ Swagger UI enabled (for API testing)
- ✅ Security headers enabled
- ✅ HTTPS recommended
- ✅ Rate limiting enabled
- ✅ CSP in report-only mode (logs violations without blocking)
- ⚠️  Audit logging enabled

**Database:** H2 or PostgreSQL (`jdbc:h2:file:./data/oscal-history-staging`)

**Activation:**
```bash
export SPRING_PROFILES_ACTIVE=staging
export JWT_SECRET="staging-secret-here"
export DB_PASSWORD="staging-password"
./start.sh
```

---

### Production Profile (`prod`)

**Purpose:** Production deployment

**Security Posture:** Maximum

**Features:**
- ❌ H2 Console **DISABLED** (fails startup if enabled)
- ❌ Swagger UI **DISABLED** (or requires authentication)
- ✅ Security headers **REQUIRED**
- ✅ HTTPS **REQUIRED**
- ✅ Rate limiting **REQUIRED**
- ✅ CSP enforced (not report-only)
- ✅ Audit logging **REQUIRED**
- ✅ Production database **REQUIRED** (PostgreSQL recommended)

**Database:** PostgreSQL (production-grade)

**Activation:**
```bash
export SPRING_PROFILES_ACTIVE=prod
export JWT_SECRET="production-secret-minimum-256-bits"
export DB_URL="jdbc:postgresql://localhost:5432/oscal_production"
export DB_USERNAME="oscal_prod_user"
export DB_PASSWORD="production-secure-password"
export CORS_ALLOWED_ORIGINS="https://oscal-tools.example.com"
```

**Production Validation:**
Application will **FAIL TO START** if:
- H2 Console is enabled
- H2 database is configured
- JWT secret is missing or too short
- Development secrets detected

---

## Implementation Details

### Configuration Files

```
back-end/src/main/resources/
├── application.properties              # Base configuration (all profiles)
├── application-dev.properties         # Development overrides
├── application-staging.properties     # Staging overrides
└── application-prod.properties        # Production overrides
```

### Profile Selection

#### Method 1: Environment Variable (Recommended)
```bash
export SPRING_PROFILES_ACTIVE=prod
java -jar oscal-cli-api.jar
```

#### Method 2: Command Line Argument
```bash
java -jar oscal-cli-api.jar --spring.profiles.active=prod
```

#### Method 3: application.properties
```properties
spring.profiles.active=prod
```

#### Method 4: Docker
```dockerfile
ENV SPRING_PROFILES_ACTIVE=prod
```

---

## Security Components

### 1. H2 Console Control

**Location:** `H2ConsoleConfig.java`

**Behavior:**
- **Dev:** Enabled by default, logs connection info
- **Staging:** Disabled by default, warns if enabled
- **Prod:** **FAILS STARTUP** if enabled

**Configuration:**
```properties
# Development
spring.h2.console.enabled=true

# Staging
spring.h2.console.enabled=false

# Production
spring.h2.console.enabled=false  # REQUIRED
```

**Startup Logs:**
```
# Development
INFO  H2ConsoleConfig - H2 Console enabled for development at: /h2-console
INFO  H2ConsoleConfig -   JDBC URL: jdbc:h2:file:./data/oscal-history
INFO  H2ConsoleConfig -   Username: sa

# Production (if accidentally enabled)
ERROR H2ConsoleConfig - CRITICAL SECURITY ERROR: H2 Console is ENABLED in PRODUCTION
ERROR H2ConsoleConfig - This exposes your database to potential attacks
java.lang.IllegalStateException: H2 Console enabled in production
```

---

### 2. Swagger UI Control

**Location:** `SwaggerConfig.java`

**Behavior:**
- **Dev:** Enabled, no authentication required
- **Staging:** Enabled for API testing
- **Prod:** Disabled by default, warns if enabled

**Configuration:**
```properties
# Development
springdoc.swagger-ui.enabled=true
springdoc.api-docs.enabled=true

# Staging
springdoc.swagger-ui.enabled=true

# Production
springdoc.swagger-ui.enabled=false  # RECOMMENDED
# OR require authentication
```

**Startup Logs:**
```
# Development
INFO  SwaggerConfig - Swagger UI enabled for development at: /swagger-ui.html

# Production (if enabled)
WARN  SwaggerConfig - WARNING: Swagger UI is ENABLED in PRODUCTION environment
WARN  SwaggerConfig - This exposes your API structure and endpoints
WARN  SwaggerConfig - Recommendation: Set springdoc.swagger-ui.enabled=false
```

---

### 3. Environment Validation

**Location:** `EnvironmentConfig.java`

**Behavior:**
- Validates configuration on startup
- Logs security status for each feature
- **BLOCKS PRODUCTION DEPLOYMENT** if critical issues found

**Validation Checks:**
1. H2 Console disabled in production (**CRITICAL**)
2. H2 database not used in production (**CRITICAL**)
3. Swagger UI status (WARNING)
4. Security headers enabled (WARNING)
5. HTTPS required (WARNING)
6. Rate limiting enabled (WARNING)
7. Audit logging enabled (WARNING)

**Startup Output (Production):**
```
================================================================================
PRODUCTION ENVIRONMENT - Security Configuration Validation
================================================================================
✓ H2 Console disabled
✓ Swagger UI disabled
✓ Security headers enabled
✓ HTTPS required
✓ Rate limiting enabled
✓ Audit logging enabled
✓ Production-grade database configured
================================================================================
Security Validation Summary:
  Critical Issues: 0
  Warnings: 0
✓ Production security configuration validated successfully
================================================================================
```

**Blocked Deployment Example:**
```
================================================================================
❌ CRITICAL: H2 Console is ENABLED in PRODUCTION
   Risk: Exposes database structure and allows direct SQL execution
   Fix: Set spring.h2.console.enabled=false
❌ CRITICAL: H2 database detected in PRODUCTION
   Risk: H2 is not production-grade, lacks enterprise features
   Fix: Use PostgreSQL, MySQL, or another production database
================================================================================
Security Validation Summary:
  Critical Issues: 2
  Warnings: 3
❌ PRODUCTION DEPLOYMENT BLOCKED: 2 critical security issues found
Fix critical issues before deploying to production
================================================================================

java.lang.IllegalStateException: Production deployment blocked: 2 critical security issues
```

---

## Deployment Guide

### Development Deployment

```bash
# 1. Ensure development profile
export SPRING_PROFILES_ACTIVE=dev

# 2. Set optional secrets (or use defaults)
export JWT_SECRET="dev-secret-key-only-for-development"

# 3. Start application
./dev.sh

# Expected output:
# ================================================================================
# DEVELOPMENT ENVIRONMENT
# ================================================================================
# Running in development mode - security restrictions relaxed
# H2 Console available at: /h2-console
# Swagger UI available at: /swagger-ui.html
# ================================================================================
```

---

### Staging Deployment

```bash
# 1. Set staging profile
export SPRING_PROFILES_ACTIVE=staging

# 2. Generate unique staging secrets
export JWT_SECRET=$(openssl rand -base64 64 | tr -d '\n' | head -c 64)
export DB_PASSWORD=$(openssl rand -base64 32 | tr -d '\n')

# 3. Configure staging settings
export CORS_ALLOWED_ORIGINS="https://staging.oscal-tools.example.com"
export SECURITY_HEADERS_ENABLED=true
export RATE_LIMIT_ENABLED=true

# 4. Start application
java -jar back-end/target/oscal-cli-api-*.jar

# Expected output:
# ================================================================================
# STAGING ENVIRONMENT
# ================================================================================
# Swagger UI enabled for API testing
# ================================================================================
```

---

### Production Deployment

#### Prerequisites

1. **Database:** PostgreSQL 12+ installed and configured
2. **Secrets:** Strong JWT secret and database password generated
3. **Domain:** HTTPS domain with valid SSL certificate
4. **Monitoring:** Logging and monitoring infrastructure ready

#### Deployment Steps

```bash
# 1. Set production profile
export SPRING_PROFILES_ACTIVE=prod

# 2. Generate production secrets (UNIQUE, never reuse staging/dev)
export JWT_SECRET=$(openssl rand -base64 64 | tr -d '\n' | head -c 64)
export DB_PASSWORD=$(openssl rand -base64 32 | tr -d '\n')

# Store these securely (password manager, secrets vault, etc.)
echo "JWT_SECRET=$JWT_SECRET" >> /secure/location/secrets.txt
echo "DB_PASSWORD=$DB_PASSWORD" >> /secure/location/secrets.txt

# 3. Configure database
export DB_URL="jdbc:postgresql://prod-db.example.com:5432/oscal_production"
export DB_USERNAME="oscal_prod_user"
export DB_DRIVER="org.postgresql.Driver"

# 4. Configure security
export CORS_ALLOWED_ORIGINS="https://oscal-tools.example.com"
export SECURITY_HEADERS_ENABLED=true
export SECURITY_REQUIRE_HTTPS=true
export RATE_LIMIT_ENABLED=true
export AUDIT_LOGGING_ENABLED=true

# 5. Disable development features
export H2_CONSOLE_ENABLED=false
export SWAGGER_ENABLED=false

# 6. Start application
java -jar back-end/target/oscal-cli-api-*.jar

# Expected output:
# ================================================================================
# PRODUCTION ENVIRONMENT - Security Configuration Validation
# ================================================================================
# ✓ H2 Console disabled
# ✓ Swagger UI disabled
# ✓ Security headers enabled
# ✓ HTTPS required
# ✓ Rate limiting enabled
# ✓ Audit logging enabled
# ✓ Production-grade database configured
# ================================================================================
# ✓ Production security configuration validated successfully
# ================================================================================
```

---

### Docker Production Deployment

```bash
# 1. Create production .env file
cat > .env << EOF
SPRING_PROFILES_ACTIVE=prod
JWT_SECRET=$(openssl rand -base64 64 | tr -d '\n' | head -c 64)
DB_NAME=oscal_production
DB_USER=oscal_user
DB_PASSWORD=$(openssl rand -base64 32 | tr -d '\n')
CORS_ALLOWED_ORIGINS=https://oscal-tools.example.com
NEXT_PUBLIC_API_URL=https://oscal-tools.example.com/api
SECURITY_HEADERS_ENABLED=true
SECURITY_REQUIRE_HTTPS=true
RATE_LIMIT_ENABLED=true
H2_CONSOLE_ENABLED=false
SWAGGER_ENABLED=false
EOF

# 2. Deploy with production compose file
docker-compose -f docker-compose.yml -f docker-compose.prod.yml up -d

# 3. Verify deployment
docker logs oscal-ux-prod | grep "PRODUCTION ENVIRONMENT"
docker logs oscal-ux-prod | grep "Production security configuration validated"

# 4. Test security
curl -I https://oscal-tools.example.com/api/health | grep "Strict-Transport-Security"
```

---

## Testing Profile Configuration

### Test Profile Detection

```bash
# Test development profile
SPRING_PROFILES_ACTIVE=dev java -jar app.jar | grep "DEVELOPMENT ENVIRONMENT"

# Test staging profile
SPRING_PROFILES_ACTIVE=staging java -jar app.jar | grep "STAGING ENVIRONMENT"

# Test production profile
SPRING_PROFILES_ACTIVE=prod java -jar app.jar | grep "PRODUCTION ENVIRONMENT"
```

### Test H2 Console Blocking

```bash
# This should FAIL in production
export SPRING_PROFILES_ACTIVE=prod
export H2_CONSOLE_ENABLED=true
java -jar app.jar

# Expected error:
# CRITICAL SECURITY ERROR: H2 Console is ENABLED in PRODUCTION
# java.lang.IllegalStateException
```

### Test Swagger Warnings

```bash
# This should WARN in production
export SPRING_PROFILES_ACTIVE=prod
export SWAGGER_ENABLED=true
java -jar app.jar | grep "WARNING: Swagger UI is ENABLED"
```

---

## Troubleshooting

### Problem: Application uses wrong profile

**Symptom:** H2 console available in production, or security disabled

**Solution:**
```bash
# Check active profile
curl http://localhost:8080/actuator/env | grep spring.profiles.active

# Or check startup logs
grep "Active Profile" application.log

# Set correct profile
export SPRING_PROFILES_ACTIVE=prod
```

### Problem: Production deployment blocked

**Symptom:** Application fails to start with "critical security issues"

**Solution:**
```
1. Read the error messages carefully
2. Check which critical issues are flagged:
   - H2 Console enabled? Set spring.h2.console.enabled=false
   - H2 database? Configure PostgreSQL
3. Fix each issue and restart
```

### Problem: Configuration not loading

**Symptom:** Settings from application-prod.properties not applied

**Solution:**
```bash
# Verify profile is set
echo $SPRING_PROFILES_ACTIVE

# Check file exists
ls -la back-end/src/main/resources/application-prod.properties

# Ensure file is in classpath
mvn clean package
jar tf target/oscal-cli-api-*.jar | grep application-prod.properties
```

---

## Security Best Practices

### DO
- ✅ Always set SPRING_PROFILES_ACTIVE explicitly
- ✅ Use different secrets for each environment
- ✅ Test staging with production profile before deploying
- ✅ Review startup logs for security warnings
- ✅ Use PostgreSQL or similar for production
- ✅ Disable all debugging tools in production

### DON'T
- ❌ Use development profile in production
- ❌ Enable H2 console in production
- ❌ Reuse dev/staging secrets in production
- ❌ Expose Swagger UI publicly in production
- ❌ Use H2 database in production
- ❌ Ignore security warnings at startup

---

## Compliance

This environment-based security approach supports:

- **NIST SP 800-53:** CM-2 (Baseline Configuration), CM-6 (Configuration Settings)
- **OWASP Top 10:** A05:2021 - Security Misconfiguration
- **CIS Controls:** Control 5 (Secure Configuration)
- **PCI DSS:** Requirement 2 (Configuration Standards)

---

## References

- [Spring Boot Profiles Documentation](https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.profiles)
- [OWASP Configuration Management](https://cheatsheetseries.owasp.org/cheatsheets/Infrastructure_as_Code_Security_Cheat_Sheet.html)
- [12-Factor App: Config](https://12factor.net/config)

---

## Change Log

| Date | Version | Changes | Author |
|------|---------|---------|--------|
| 2025-10-26 | 1.0 | Initial environment-based security implementation | Security Team |

---

**Completion Status:** Section 4 Complete
**Next Steps:** Implement HTTPS/TLS Configuration (Task 5)
