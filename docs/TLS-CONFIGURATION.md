# TLS/HTTPS Configuration Guide

**Date**: 2025-10-26
**Status**: Production Ready
**Priority**: CRITICAL (SEC-02)

## Overview

This document describes the HTTPS/TLS configuration for the OSCAL Tools platform, providing secure encrypted communication between clients and the server. The implementation supports two deployment models:

1. **Development**: Spring Boot handles HTTPS directly with self-signed certificates
2. **Production**: Nginx reverse proxy handles HTTPS termination (recommended)

## Table of Contents

1. [Security Features](#security-features)
2. [Development Setup](#development-setup)
3. [Staging Setup](#staging-setup)
4. [Production Setup](#production-setup)
5. [Certificate Management](#certificate-management)
6. [Testing and Verification](#testing-and-verification)
7. [Troubleshooting](#troubleshooting)
8. [Security Best Practices](#security-best-practices)

## Security Features

### TLS Protocol Support

- ✅ **TLS 1.2 and TLS 1.3 only** - Disabled SSL, TLS 1.0, and TLS 1.1
- ✅ **Strong cipher suites** - OWASP and Mozilla recommended ciphers
- ✅ **Perfect Forward Secrecy** - ECDHE and DHE key exchange
- ✅ **HTTP to HTTPS redirect** - Automatic upgrade of insecure connections

### Cipher Suite Configuration

**Spring Boot** (configured in `HttpsConfig.java`):
```
TLS_AES_128_GCM_SHA256
TLS_AES_256_GCM_SHA384
TLS_CHACHA20_POLY1305_SHA256
TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256
TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384
TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256
TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384
TLS_DHE_RSA_WITH_AES_128_GCM_SHA256
TLS_DHE_RSA_WITH_AES_256_GCM_SHA384
```

**Nginx** (configured in `nginx/conf.d/oscal-tools.conf`):
```
ECDHE-ECDSA-AES128-GCM-SHA256
ECDHE-RSA-AES128-GCM-SHA256
ECDHE-ECDSA-AES256-GCM-SHA384
ECDHE-RSA-AES256-GCM-SHA384
ECDHE-ECDSA-CHACHA20-POLY1305
ECDHE-RSA-CHACHA20-POLY1305
DHE-RSA-AES128-GCM-SHA256
DHE-RSA-AES256-GCM-SHA384
```

## Development Setup

### Quick Start (Self-Signed Certificate)

**1. Generate Development Certificate**

```bash
cd certs
chmod +x generate-dev-cert.sh
./generate-dev-cert.sh
```

This creates:
- `certs/keystore.p12` - PKCS12 keystore with self-signed certificate
- Valid for 365 days
- CN=localhost, SAN=localhost,*.localhost,127.0.0.1

**2. Enable HTTPS in Development**

Create or update `.env`:
```bash
# SSL/TLS Configuration (Development)
SSL_ENABLED=true
SSL_KEYSTORE=file:certs/keystore.p12
SSL_KEYSTORE_PASSWORD=changeit
SSL_KEY_ALIAS=oscal-dev
HTTP_PORT=8080
```

**3. Start the Application**

```bash
./dev.sh
```

The application will:
- Listen on HTTPS: `https://localhost:8443`
- Listen on HTTP: `http://localhost:8080` (redirects to HTTPS)

**4. Accept Self-Signed Certificate in Browser**

When accessing `https://localhost:8443`:
1. Browser will show a security warning
2. Click "Advanced" → "Proceed to localhost (unsafe)"
3. This is expected for self-signed certificates in development

### Development Configuration Files

**File**: `back-end/src/main/resources/application-dev.properties`

```properties
# Server Configuration
server.port=${SERVER_PORT:8443}

# SSL/TLS Configuration (Optional in development)
server.ssl.enabled=${SSL_ENABLED:false}
server.ssl.key-store=${SSL_KEYSTORE:file:certs/keystore.p12}
server.ssl.key-store-password=${SSL_KEYSTORE_PASSWORD:changeit}
server.ssl.key-store-type=PKCS12
server.ssl.key-alias=${SSL_KEY_ALIAS:oscal-dev}

# HTTP port (redirects to HTTPS if SSL enabled)
server.http.port=${HTTP_PORT:8080}
server.ssl.redirect-http=${SSL_REDIRECT_HTTP:true}

# Security Headers (Relaxed in development)
security.headers.enabled=${SECURITY_HEADERS_ENABLED:false}
security.headers.require-https=${SECURITY_REQUIRE_HTTPS:false}
```

**File**: `back-end/src/main/java/.../config/HttpsConfig.java`

Key features:
- Configures HTTP to HTTPS redirect
- Sets strong TLS protocols (TLSv1.2, TLSv1.3)
- Configures strong cipher suites
- Only activates when `server.ssl.redirect-http=true`

## Staging Setup

### Configuration

**1. Generate or Obtain Certificate**

**Option A: Self-Signed (for internal staging)**
```bash
cd certs
./generate-dev-cert.sh
```

**Option B: Let's Encrypt (for external staging)**
```bash
# Install certbot
sudo apt-get install certbot

# Obtain certificate
sudo certbot certonly --standalone \
  -d staging.oscal-tools.example.com \
  --non-interactive --agree-tos \
  -m admin@example.com

# Copy certificates
sudo cp /etc/letsencrypt/live/staging.oscal-tools.example.com/fullchain.pem certs/
sudo cp /etc/letsencrypt/live/staging.oscal-tools.example.com/privkey.pem certs/

# Convert to PKCS12
openssl pkcs12 -export \
  -in certs/fullchain.pem \
  -inkey certs/privkey.pem \
  -out certs/keystore.p12 \
  -name oscal-staging \
  -passout pass:$SSL_KEYSTORE_PASSWORD
```

**2. Configure Environment Variables**

```bash
# SSL/TLS Configuration (Staging)
SSL_ENABLED=true
SSL_KEYSTORE=file:certs/keystore.p12
SSL_KEYSTORE_PASSWORD=<strong-random-password>
SSL_KEY_ALIAS=oscal-staging
HTTP_PORT=8080
```

**3. Start with Staging Profile**

```bash
SPRING_PROFILES_ACTIVE=staging ./start.sh
```

### Staging Configuration Files

**File**: `back-end/src/main/resources/application-staging.properties`

```properties
# Server Configuration
server.port=${SERVER_PORT:8443}

# SSL/TLS Configuration (Recommended in staging)
server.ssl.enabled=${SSL_ENABLED:true}
server.ssl.key-store=${SSL_KEYSTORE:file:certs/keystore.p12}
server.ssl.key-store-password=${SSL_KEYSTORE_PASSWORD}
server.ssl.key-store-type=PKCS12
server.ssl.key-alias=${SSL_KEY_ALIAS:oscal-staging}

# HTTP port (redirects to HTTPS)
server.http.port=${HTTP_PORT:8080}
server.ssl.redirect-http=true

# Security Headers (Enabled)
security.headers.enabled=true
security.headers.require-https=${SECURITY_REQUIRE_HTTPS:true}
```

## Production Setup

### Recommended Architecture: Nginx Reverse Proxy

```
Internet
    ↓
Nginx (HTTPS:443, HTTP:80)
    ├─→ Spring Boot Backend (HTTP:8080) - /api/*
    └─→ Next.js Frontend (HTTP:3000)    - /*
```

**Why Nginx for Production?**
- ✅ Better TLS performance
- ✅ OCSP stapling support
- ✅ Load balancing capabilities
- ✅ Static file caching
- ✅ Rate limiting at network layer
- ✅ Centralized certificate management
- ✅ HTTP/2 support

### Production Setup Steps

**1. Install Nginx**

```bash
# Ubuntu/Debian
sudo apt-get update
sudo apt-get install nginx

# RHEL/CentOS
sudo yum install nginx
```

**2. Obtain SSL Certificate (Let's Encrypt)**

```bash
# Install certbot
sudo apt-get install certbot python3-certbot-nginx

# Obtain certificate
sudo certbot --nginx \
  -d oscal-tools.example.com \
  -d www.oscal-tools.example.com \
  --non-interactive --agree-tos \
  -m admin@example.com

# Certificates will be in:
# /etc/letsencrypt/live/oscal-tools.example.com/
```

**3. Copy Nginx Configuration**

```bash
# Copy main config
sudo cp nginx/nginx.conf /etc/nginx/nginx.conf

# Copy site config
sudo cp nginx/conf.d/oscal-tools.conf /etc/nginx/conf.d/

# Update domain name in config
sudo sed -i 's/oscal-tools.example.com/your-actual-domain.com/g' \
  /etc/nginx/conf.d/oscal-tools.conf
```

**4. Update SSL Certificate Paths**

Edit `/etc/nginx/conf.d/oscal-tools.conf`:

```nginx
# SSL/TLS Configuration
ssl_certificate /etc/letsencrypt/live/your-domain.com/fullchain.pem;
ssl_certificate_key /etc/letsencrypt/live/your-domain.com/privkey.pem;
ssl_trusted_certificate /etc/letsencrypt/live/your-domain.com/chain.pem;
```

**5. Test Nginx Configuration**

```bash
sudo nginx -t

# Expected output:
# nginx: the configuration file /etc/nginx/nginx.conf syntax is ok
# nginx: configuration file /etc/nginx/nginx.conf test is successful
```

**6. Start Nginx**

```bash
sudo systemctl start nginx
sudo systemctl enable nginx
sudo systemctl status nginx
```

**7. Configure Spring Boot for HTTP (Behind Nginx)**

In production, Spring Boot runs on HTTP internally since Nginx handles HTTPS:

```bash
# Production .env (Spring Boot runs on HTTP)
SERVER_PORT=8080
SSL_ENABLED=false

# Security headers still enabled (defense in depth)
SECURITY_HEADERS_ENABLED=true
SECURITY_REQUIRE_HTTPS=true
```

**8. Start Application**

```bash
SPRING_PROFILES_ACTIVE=prod ./start.sh
```

### Production Configuration Files

**File**: `nginx/nginx.conf`
- Worker processes: auto
- Rate limiting zones: api_limit (10 req/s), auth_limit (5 req/min)
- Gzip compression enabled
- Logging configuration

**File**: `nginx/conf.d/oscal-tools.conf`
- HTTP to HTTPS redirect
- TLS 1.2/1.3 only
- Strong cipher suites
- Security headers
- OCSP stapling
- Proxy configuration for backend/frontend
- Rate limiting per endpoint

**File**: `back-end/src/main/resources/application-prod.properties`

```properties
# Server Configuration (HTTP behind Nginx)
server.port=${SERVER_PORT:8080}

# SSL/TLS Configuration (Disabled - Nginx handles HTTPS)
# Uncomment only if running Spring Boot HTTPS directly (not recommended)
# server.ssl.enabled=${SSL_ENABLED:false}

# Security Headers (REQUIRED in production)
security.headers.enabled=true
security.headers.require-https=true
```

## Certificate Management

### Development Certificates

**Generation Script**: `certs/generate-dev-cert.sh`

```bash
#!/bin/bash
# Generates self-signed certificate for development

KEYSTORE_FILE="keystore.p12"
KEYSTORE_PASSWORD="${SSL_KEYSTORE_PASSWORD:-changeit}"
ALIAS="oscal-dev"
VALIDITY_DAYS=365
KEY_SIZE=2048

# Generate self-signed certificate
keytool -genkeypair -alias "$ALIAS" -keyalg RSA -keysize $KEY_SIZE \
    -storetype PKCS12 -keystore "$KEYSTORE_FILE" \
    -storepass "$KEYSTORE_PASSWORD" -validity $VALIDITY_DAYS \
    -dname "CN=localhost, OU=Development, O=OSCAL Tools, L=City, ST=State, C=US" \
    -ext "SAN=dns:localhost,dns:*.localhost,ip:127.0.0.1"
```

**Certificate Properties**:
- **Algorithm**: RSA 2048-bit
- **Validity**: 365 days
- **Format**: PKCS12
- **Subject Alternative Names**: localhost, *.localhost, 127.0.0.1

**Renewal** (before expiration):
```bash
cd certs
rm keystore.p12
./generate-dev-cert.sh
```

### Staging Certificates

**Option 1: Self-Signed** (internal staging only)
```bash
cd certs
./generate-dev-cert.sh
```

**Option 2: Let's Encrypt** (external staging)
```bash
sudo certbot certonly --standalone \
  -d staging.oscal-tools.example.com \
  --non-interactive --agree-tos \
  -m admin@example.com
```

### Production Certificates (Let's Encrypt)

**Initial Setup**:
```bash
sudo certbot --nginx -d oscal-tools.example.com -d www.oscal-tools.example.com
```

**Auto-Renewal**:
```bash
# Test renewal
sudo certbot renew --dry-run

# Enable automatic renewal (runs twice daily)
sudo systemctl enable certbot.timer
sudo systemctl start certbot.timer

# Check timer status
sudo systemctl status certbot.timer
```

**Manual Renewal**:
```bash
sudo certbot renew
sudo nginx -s reload
```

### Certificate Verification

**Check Certificate Expiration**:
```bash
# PKCS12 keystore
keytool -list -v -keystore certs/keystore.p12 -storepass changeit

# PEM format
openssl x509 -in /etc/nginx/ssl/fullchain.pem -noout -dates
```

**Check Certificate Details**:
```bash
# Via OpenSSL
openssl s_client -connect localhost:8443 -servername localhost

# Extract certificate
echo | openssl s_client -connect localhost:8443 2>/dev/null | \
  openssl x509 -noout -text
```

## Testing and Verification

### Basic Connectivity Tests

**1. Test HTTP to HTTPS Redirect**

```bash
# Should return 301 or 302 redirect
curl -I http://localhost:8080/api/health

# Expected output:
# HTTP/1.1 302
# Location: https://localhost:8443/api/health
```

**2. Test HTTPS Endpoint**

```bash
# Self-signed certificate (use -k to skip verification)
curl -k https://localhost:8443/api/health

# Production (should verify certificate)
curl https://oscal-tools.example.com/api/health
```

**3. Test TLS Version**

```bash
# TLS 1.2 (should succeed)
openssl s_client -connect localhost:8443 -tls1_2 < /dev/null

# TLS 1.1 (should fail)
openssl s_client -connect localhost:8443 -tls1_1 < /dev/null
```

**4. Test Cipher Suites**

```bash
# List supported ciphers
nmap --script ssl-enum-ciphers -p 8443 localhost

# Test specific cipher
openssl s_client -connect localhost:8443 -cipher 'ECDHE-RSA-AES128-GCM-SHA256'
```

### Security Scanning

**1. SSL Labs Test** (Production only)
```
https://www.ssllabs.com/ssltest/analyze.html?d=oscal-tools.example.com
```

Target: **A+ rating**

**2. Test Security Headers**
```bash
curl -k -I https://localhost:8443/api/health | grep -E "Strict-Transport|X-Frame|Content-Security"
```

Expected headers:
- `Strict-Transport-Security: max-age=31536000; includeSubDomains`
- `X-Frame-Options: DENY`
- `X-Content-Type-Options: nosniff`
- `Content-Security-Policy: default-src 'self'...`

**3. Test HTTP/2 Support** (Nginx only)
```bash
curl -I --http2 https://oscal-tools.example.com
```

### Automated Testing

**Test Suite**: `back-end/src/test/java/.../config/HttpsConfigTest.java`

```bash
# Run HTTPS configuration tests
cd back-end
mvn test -Dtest=HttpsConfigTest
```

## Troubleshooting

### Problem: Certificate Not Found

**Symptom**:
```
java.io.FileNotFoundException: certs/keystore.p12 (No such file or directory)
```

**Solution**:
```bash
# Check if certificate exists
ls -la certs/keystore.p12

# Generate if missing
cd certs && ./generate-dev-cert.sh

# Verify path in .env
grep SSL_KEYSTORE .env
```

### Problem: Wrong Password

**Symptom**:
```
java.io.IOException: keystore password was incorrect
```

**Solution**:
```bash
# Check password in .env
grep SSL_KEYSTORE_PASSWORD .env

# If using default password
echo "SSL_KEYSTORE_PASSWORD=changeit" >> .env

# If password is correct, certificate may be corrupted - regenerate
cd certs
rm keystore.p12
./generate-dev-cert.sh
```

### Problem: Port Already in Use

**Symptom**:
```
Port 8443 was already in use
```

**Solution**:
```bash
# Find process using port
lsof -i :8443

# Kill process
kill -9 <PID>

# Or use different port
echo "SERVER_PORT=9443" >> .env
```

### Problem: Browser Shows "Not Secure"

**Symptom**: Browser shows security warning for HTTPS site

**Development**:
- Expected for self-signed certificates
- Click "Advanced" → "Proceed anyway"
- Or add certificate to browser's trusted certificates

**Production**:
```bash
# Check certificate validity
openssl x509 -in /etc/letsencrypt/live/your-domain.com/fullchain.pem -noout -dates

# Check certificate matches domain
openssl x509 -in /etc/letsencrypt/live/your-domain.com/fullchain.pem -noout -subject

# Renew if expired
sudo certbot renew
sudo nginx -s reload
```

### Problem: Mixed Content Warnings

**Symptom**: Browser console shows "Mixed Content" errors

**Cause**: HTTPS page loading HTTP resources

**Solution**:
```javascript
// Update API client to use relative URLs
const API_BASE_URL = '/api';  // Not 'http://localhost:8080/api'

// Or use protocol-relative URLs
const API_BASE_URL = window.location.protocol + '//' + window.location.host + '/api';
```

### Problem: Nginx 502 Bad Gateway

**Symptom**: Nginx returns 502 error

**Solution**:
```bash
# Check backend is running
curl http://localhost:8080/api/health

# Check Nginx error log
sudo tail -f /var/log/nginx/error.log

# Verify proxy_pass addresses in Nginx config
sudo nginx -T | grep proxy_pass

# Restart backend
./stop.sh && ./start.sh
```

### Problem: TLS Handshake Failed

**Symptom**:
```
SSL handshake failed: SSL error:1417A0C1:SSL routines:tls_post_process_client_hello
```

**Solution**:
```bash
# Check TLS version
openssl s_client -connect localhost:8443 -tls1_2

# Check cipher compatibility
openssl ciphers -v 'ECDHE-RSA-AES128-GCM-SHA256'

# Update cipher configuration in HttpsConfig.java
```

## Security Best Practices

### Certificate Management

1. **Use Let's Encrypt in production** - Free, automated, widely trusted
2. **Enable auto-renewal** - Certificates expire every 90 days
3. **Monitor expiration** - Set up alerts 30 days before expiration
4. **Use PKCS12 format** - More secure than JKS for modern systems
5. **Protect private keys** - chmod 600, never commit to git
6. **Use strong passwords** - 32+ character random passwords for keystores

### TLS Configuration

1. **TLS 1.2 minimum** - TLS 1.3 preferred when available
2. **Strong ciphers only** - Follow OWASP/Mozilla recommendations
3. **Enable PFS** - Use ECDHE or DHE key exchange
4. **OCSP stapling** - Faster certificate validation (Nginx only)
5. **No cipher suite fallbacks** - Don't downgrade to weak ciphers

### HTTP to HTTPS

1. **Always redirect HTTP** - Never serve content over HTTP
2. **Use 301 redirects** - Permanent redirects for SEO
3. **Include HSTS header** - Force HTTPS for future requests
4. **HSTS preload** - Submit domain to browser preload lists (production only)

### Defense in Depth

1. **Multiple layers** - Nginx AND Spring Boot headers
2. **Fail securely** - Block if TLS setup fails
3. **Regular testing** - Automated security scans
4. **Log security events** - TLS handshake failures, certificate errors
5. **Monitor certificates** - Alert before expiration

### Development vs Production

| Feature | Development | Staging | Production |
|---------|------------|---------|------------|
| Certificate | Self-signed | Self-signed or Let's Encrypt | Let's Encrypt |
| HTTPS Required | Optional | Recommended | Required |
| HSTS Enabled | No | Yes | Yes (with preload) |
| HSTS Max-Age | N/A | 1 year | 2 years |
| OCSP Stapling | No | Optional | Yes |
| HTTP/2 | Optional | Optional | Yes |
| TLS Termination | Spring Boot | Spring Boot or Nginx | Nginx |

## References

- **Mozilla SSL Configuration Generator**: https://ssl-config.mozilla.org/
- **OWASP TLS Cheat Sheet**: https://cheatsheetseries.owasp.org/cheatsheets/Transport_Layer_Protection_Cheat_Sheet.html
- **SSL Labs Testing**: https://www.ssllabs.com/ssltest/
- **Let's Encrypt Documentation**: https://letsencrypt.org/docs/
- **Nginx SSL Module**: https://nginx.org/en/docs/http/ngx_http_ssl_module.html
- **Spring Boot SSL Documentation**: https://docs.spring.io/spring-boot/docs/current/reference/html/howto.html#howto.webserver.configure-ssl

## Related Documentation

- `docs/SECRETS-MANAGEMENT.md` - Environment variable configuration
- `docs/SECURITY-HEADERS.md` - HTTP security headers
- `docs/PRODUCTION-SECURITY-HARDENING-PLAN.md` - Overall security roadmap
- `certs/README.md` - Certificate management guide
- `nginx/README.md` - Nginx deployment guide
