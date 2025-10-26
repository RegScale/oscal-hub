# Secrets Management Guide

**Date:** 2025-10-26
**Status:** Implemented
**Component:** Back-end API Security

## Overview

This document describes the secrets management implementation for the OSCAL Tools project. All sensitive configuration values (passwords, API keys, tokens) are externalized from the codebase and loaded from environment variables.

## Security Principles

1. **Never commit secrets to version control**
2. **Use different secrets for each environment** (dev, staging, production)
3. **Rotate secrets regularly** (at least every 90 days for production)
4. **Use cryptographically secure random generation** for all secrets
5. **Validate secrets on application startup** (fail fast if misconfigured)

## Required Secrets

### Critical (Application will not start without these)

#### JWT Secret (`JWT_SECRET`)
- **Purpose:** Signs and validates JWT authentication tokens
- **Minimum Length:** 32 characters (256 bits) for HS256 algorithm
- **Generation:**
  ```bash
  openssl rand -base64 64 | tr -d '\n' | head -c 64
  ```
- **Example Output:**
  ```
  uXweCSxBUo+E/m2FJldJpG0keKdkryW7aqEq9cMCXu37uD3w2SLWSPJs2HLWK+7M
  ```
- **Security Notes:**
  - Application validates length on startup
  - Development secrets detected and blocked in production
  - Token invalidation requires secret rotation

### Optional (Can run with defaults)

#### Database Password (`DB_PASSWORD`)
- **Purpose:** Authenticates to the database
- **Generation:**
  ```bash
  openssl rand -base64 32 | tr -d '\n'
  ```
- **Example Output:**
  ```
  urVxzp55aiH9GNi25vMiufHxpfV2wQYN37tZ8cZvfvM=
  ```
- **Security Notes:**
  - Leave empty for development H2 database
  - **REQUIRED** for production PostgreSQL
  - Store in environment variables, never in code

#### Azure Storage Connection String (`AZURE_STORAGE_CONNECTION_STRING`)
- **Purpose:** Connects to Azure Blob Storage (if used)
- **Format:** `DefaultEndpointsProtocol=https;AccountName=...;AccountKey=...;EndpointSuffix=core.windows.net`
- **Security Notes:**
  - Optional - application works without Azure storage
  - Obtain from Azure Portal
  - Never commit to version control

## Configuration Files

### Environment Variable Files

```
.env                          # Local environment (gitignored, not committed)
.env.example                  # Template for .env (committed to Git)
```

### Application Properties Files

```
application.properties                # Base configuration (all environments)
application-dev.properties           # Development overrides
application-staging.properties       # Staging overrides
application-prod.properties          # Production overrides
```

## Setup Instructions

### 1. Development Environment

#### Step 1: Create Local .env File
```bash
cd /path/to/oscal-cli
cp .env.example .env
```

#### Step 2: Generate Development Secrets
```bash
# Generate JWT secret (or use the dev default)
echo "JWT_SECRET=dev-secret-key-only-for-development-do-not-use-in-production-minimum-256-bits" >> .env

# Set development profile
echo "SPRING_PROFILES_ACTIVE=dev" >> .env
```

#### Step 3: Verify Configuration
```bash
# The application will log configuration on startup
./dev.sh
```

Look for these log messages:
```
================================================================================
OSCAL CLI API - Environment Configuration
================================================================================
Active Profile: dev
JWT secret configured successfully (length: XX characters)
```

### 2. Staging Environment

#### Step 1: Generate Staging Secrets
```bash
# Generate unique JWT secret for staging
JWT_SECRET=$(openssl rand -base64 64 | tr -d '\n' | head -c 64)
echo "Staging JWT Secret: $JWT_SECRET"

# Generate database password
DB_PASSWORD=$(openssl rand -base64 32 | tr -d '\n')
echo "Staging DB Password: $DB_PASSWORD"
```

#### Step 2: Configure Environment Variables
```bash
# Option A: Using .env file
cat > .env << EOF
SPRING_PROFILES_ACTIVE=staging
JWT_SECRET=$JWT_SECRET
DB_URL=jdbc:postgresql://staging-db.example.com:5432/oscal_staging
DB_USERNAME=oscal_staging_user
DB_PASSWORD=$DB_PASSWORD
DB_DRIVER=org.postgresql.Driver
CORS_ALLOWED_ORIGINS=https://staging.oscal-tools.example.com
SECURITY_HEADERS_ENABLED=true
RATE_LIMIT_ENABLED=true
EOF

# Option B: Export environment variables
export SPRING_PROFILES_ACTIVE=staging
export JWT_SECRET="$JWT_SECRET"
export DB_PASSWORD="$DB_PASSWORD"
# ... (other variables)
```

#### Step 3: Verify Staging Configuration
```bash
java -jar back-end/target/oscal-cli-api-*.jar
```

### 3. Production Environment

#### Step 1: Generate Production Secrets
```bash
# Generate UNIQUE production secrets (NEVER reuse dev/staging secrets)
PROD_JWT_SECRET=$(openssl rand -base64 64 | tr -d '\n' | head -c 64)
PROD_DB_PASSWORD=$(openssl rand -base64 32 | tr -d '\n')

# SECURELY STORE these values (password manager, secrets vault, etc.)
echo "Production JWT Secret: $PROD_JWT_SECRET"
echo "Production DB Password: $PROD_DB_PASSWORD"
```

#### Step 2: Configure via Secrets Manager

**Using AWS Secrets Manager:**
```bash
aws secretsmanager create-secret \
    --name oscal-tools/prod/jwt-secret \
    --secret-string "$PROD_JWT_SECRET"

aws secretsmanager create-secret \
    --name oscal-tools/prod/db-password \
    --secret-string "$PROD_DB_PASSWORD"
```

**Using Azure Key Vault:**
```bash
az keyvault secret set \
    --vault-name oscal-tools-vault \
    --name jwt-secret \
    --value "$PROD_JWT_SECRET"

az keyvault secret set \
    --vault-name oscal-tools-vault \
    --name db-password \
    --value "$PROD_DB_PASSWORD"
```

**Using Docker Secrets:**
```bash
echo "$PROD_JWT_SECRET" | docker secret create jwt_secret -
echo "$PROD_DB_PASSWORD" | docker secret create db_password -
```

#### Step 3: Configure Production Environment
```bash
# Production docker-compose.yml
version: '3.8'
services:
  oscal-api:
    image: oscal-tools:latest
    environment:
      - SPRING_PROFILES_ACTIVE=prod
      - JWT_SECRET=${PROD_JWT_SECRET}
      - DB_PASSWORD=${PROD_DB_PASSWORD}
      - DB_URL=jdbc:postgresql://db:5432/oscal_production
      - CORS_ALLOWED_ORIGINS=https://oscal-tools.example.com
      - SECURITY_HEADERS_ENABLED=true
      - SECURITY_REQUIRE_HTTPS=true
      - RATE_LIMIT_ENABLED=true
      - ACCOUNT_LOCKOUT_ENABLED=true
      - AUDIT_LOGGING_ENABLED=true
      - SWAGGER_ENABLED=false
```

## Secret Rotation

### When to Rotate

- **Immediately** if secret is compromised
- **Every 90 days** for production secrets (recommended)
- **When employee with access leaves** the organization
- **After security incident** or breach

### How to Rotate JWT Secret

#### Impact Analysis
- **All existing JWT tokens will be invalidated**
- **All users must log in again**
- **Plan maintenance window** if possible

#### Rotation Steps

1. **Generate new secret:**
   ```bash
   NEW_JWT_SECRET=$(openssl rand -base64 64 | tr -d '\n' | head -c 64)
   ```

2. **Update secret in secrets manager:**
   ```bash
   # AWS
   aws secretsmanager update-secret \
       --secret-id oscal-tools/prod/jwt-secret \
       --secret-string "$NEW_JWT_SECRET"

   # Azure
   az keyvault secret set \
       --vault-name oscal-tools-vault \
       --name jwt-secret \
       --value "$NEW_JWT_SECRET"
   ```

3. **Restart application** (this will invalidate all existing tokens):
   ```bash
   # Docker
   docker-compose restart oscal-api

   # Kubernetes
   kubectl rollout restart deployment/oscal-api
   ```

4. **Notify users** to log in again

5. **Document rotation** in change log:
   ```
   Date: 2025-10-26
   Action: JWT secret rotated
   Reason: Scheduled 90-day rotation
   Impact: All users required to re-authenticate
   ```

### How to Rotate Database Password

1. **Create new database user** (if supported):
   ```sql
   CREATE USER oscal_api_new WITH PASSWORD 'new-password';
   GRANT ALL PRIVILEGES ON DATABASE oscal_production TO oscal_api_new;
   ```

2. **Update application configuration** with new credentials

3. **Restart application** with rolling restart (zero downtime)

4. **Remove old database user** after verification:
   ```sql
   DROP USER oscal_api_old;
   ```

## Validation and Verification

### Startup Validation

The application automatically validates secrets on startup:

```java
// JwtUtil.java validates JWT secret
@PostConstruct
public void validateSecretConfiguration() {
    // Checks:
    // 1. Secret is not empty
    // 2. Secret is at least 32 characters
    // 3. Development secrets not used in production
    // 4. Logs configuration (without exposing secrets)
}
```

### Expected Log Output

**Successful Configuration:**
```
INFO  JwtUtil - JWT secret configured successfully (length: 64 characters)
INFO  JwtUtil - JWT token expiration set to 3600000 ms (1 hours)
INFO  EnvironmentConfig - Active Profile: prod
INFO  EnvironmentConfig - Security Headers: true
INFO  EnvironmentConfig - HTTPS Required: true
```

**Failed Configuration:**
```
ERROR JwtUtil - CRITICAL SECURITY ERROR: JWT secret is not configured.
ERROR JwtUtil - Set the JWT_SECRET environment variable before starting.
```

### Manual Verification

Check that secrets are loaded correctly:

```bash
# DO NOT RUN IN PRODUCTION (exposes secrets)
# Only for development troubleshooting

# Check environment variables
env | grep JWT_SECRET
env | grep DB_PASSWORD

# Check Spring Boot actuator (if enabled)
curl http://localhost:8080/actuator/env | grep jwt
```

## Troubleshooting

### Problem: Application fails to start with "JWT secret is not configured"

**Solution:**
```bash
# Verify environment variable is set
echo $JWT_SECRET

# If empty, set it:
export JWT_SECRET="your-secret-here"

# Or create .env file:
echo "JWT_SECRET=your-secret-here" > .env
```

### Problem: "JWT secret is too short" error

**Solution:**
```bash
# Generate a new secret with correct length
openssl rand -base64 64 | tr -d '\n' | head -c 64
```

### Problem: "Development JWT secret detected in PRODUCTION"

**Solution:**
```bash
# NEVER use development secrets in production
# Generate a unique production secret
PROD_JWT_SECRET=$(openssl rand -base64 64 | tr -d '\n' | head -c 64)
export JWT_SECRET="$PROD_JWT_SECRET"
```

### Problem: Secrets not loading from .env file

**Solution:**
```bash
# Ensure .env is in the project root
ls -la .env

# Verify Spring Boot dotenv support (if using)
# Or use environment variables directly:
source .env
```

## Security Best Practices

### DO
- ✅ Use different secrets for each environment
- ✅ Generate secrets with `openssl rand` or equivalent
- ✅ Store production secrets in a secrets manager (AWS, Azure, HashiCorp Vault)
- ✅ Rotate secrets regularly (90 days recommended)
- ✅ Use at least 256-bit secrets for JWT
- ✅ Document secret rotation procedures
- ✅ Audit access to secrets
- ✅ Use principle of least privilege

### DON'T
- ❌ Commit secrets to version control
- ❌ Share secrets via email or chat
- ❌ Reuse secrets across environments
- ❌ Use weak or predictable secrets
- ❌ Log secrets in application logs
- ❌ Store secrets in plain text files
- ❌ Hard-code secrets in application code
- ❌ Share secrets with unauthorized personnel

## Compliance

This secrets management implementation supports compliance with:

- **NIST SP 800-53:** SC-28 (Protection of Information at Rest), SC-12 (Cryptographic Key Establishment)
- **OWASP Top 10:** A02:2021 - Cryptographic Failures
- **CIS Controls:** Control 3 (Data Protection)
- **PCI DSS:** Requirement 8 (Identify and Authenticate Access)

## References

- [Spring Boot Externalized Configuration](https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.external-config)
- [OWASP Secrets Management Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Secrets_Management_Cheat_Sheet.html)
- [NIST SP 800-57: Key Management](https://csrc.nist.gov/publications/detail/sp/800-57-part-1/rev-5/final)

## Change Log

| Date | Version | Changes | Author |
|------|---------|---------|--------|
| 2025-10-26 | 1.0 | Initial secrets management implementation | Security Team |

---

**Next Steps:** Implement Rate Limiting (Task 2)
