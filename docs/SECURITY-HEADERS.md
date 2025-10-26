# Security Headers Implementation Guide

**Date:** 2025-10-26
**Status:** Implemented
**Component:** Back-end API Security

## Overview

This document describes the HTTP security headers implementation for the OSCAL Tools API. Security headers protect the application from common web vulnerabilities and attacks.

## Purpose

Security headers provide defense-in-depth protection against:
- **Cross-Site Scripting (XSS)** attacks
- **Clickjacking** attacks
- **MIME type sniffing** attacks
- **Man-in-the-middle** attacks
- **Referrer information leakage**
- **Unauthorized browser feature** access

## Implemented Security Headers

### 1. Strict-Transport-Security (HSTS)

**Purpose:** Forces browsers to use HTTPS connections only

**Header Example:**
```
Strict-Transport-Security: max-age=31536000; includeSubDomains
```

**Configuration:**
```properties
security.headers.hsts.enabled=true
security.headers.hsts.max-age=31536000          # 1 year
security.headers.hsts.include-sub-domains=true
security.headers.hsts.preload=false
```

**Protection:**
- Prevents downgrade attacks (forcing HTTPS to HTTP)
- Protects against SSL stripping attacks
- Mitigates man-in-the-middle attacks

**Notes:**
- Only sent over HTTPS connections
- Max-age of 1 year (31536000 seconds) is recommended
- Include subdomains if all subdomains support HTTPS
- Preload list requires domain registration at hstspreload.org

---

### 2. Content-Security-Policy (CSP)

**Purpose:** Controls which resources browsers can load, preventing XSS and injection attacks

**Header Example:**
```
Content-Security-Policy: default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline'; img-src 'self' data: https:; font-src 'self' data:; connect-src 'self'; frame-src 'none'; object-src 'none'; base-uri 'self'; form-action 'self'
```

**Configuration:**
```properties
security.headers.csp.enabled=true
security.headers.csp.report-only=false
security.headers.csp.default-src='self'
security.headers.csp.script-src='self'
security.headers.csp.style-src='self' 'unsafe-inline'
security.headers.csp.img-src='self' data: https:
security.headers.csp.font-src='self' data:
security.headers.csp.connect-src='self'
security.headers.csp.frame-src='none'
security.headers.csp.object-src='none'
security.headers.csp.base-uri='self'
security.headers.csp.form-action='self'
```

**Directive Meanings:**
- `default-src 'self'` - Default policy: only load resources from same origin
- `script-src 'self'` - Only load JavaScript from same origin
- `style-src 'self' 'unsafe-inline'` - Allow inline styles (needed for some frameworks)
- `img-src 'self' data: https:` - Allow images from same origin, data URLs, and HTTPS
- `connect-src 'self'` - Only allow AJAX/WebSocket to same origin
- `frame-src 'none'` - Don't allow embedding in frames
- `object-src 'none'` - Block plugins (Flash, Java, etc.)
- `base-uri 'self'` - Prevent base tag injection
- `form-action 'self'` - Forms can only submit to same origin

**Report-Only Mode:**
Set `report-only=true` to test CSP without enforcing (logs violations only):
```
Content-Security-Policy-Report-Only: ...
```

**Protection:**
- Prevents XSS attacks by blocking inline scripts
- Prevents injection attacks
- Limits resource loading to trusted sources
- Blocks malicious iframes and objects

---

### 3. X-Frame-Options

**Purpose:** Prevents clickjacking by controlling frame embedding

**Header Example:**
```
X-Frame-Options: DENY
```

**Configuration:**
```properties
security.headers.frame-options.policy=DENY
```

**Policy Options:**
- `DENY` - Cannot be embedded in frames at all (most secure)
- `SAMEORIGIN` - Can only be embedded by same origin
- `ALLOW-FROM uri` - Deprecated, use CSP frame-ancestors instead

**Protection:**
- Prevents clickjacking attacks
- Stops malicious sites from embedding your app in invisible frames
- Protects users from tricking into clicking on hidden elements

**Recommendation:** Use `DENY` for production unless you need frame embedding

---

### 4. X-Content-Type-Options

**Purpose:** Prevents MIME type sniffing

**Header Example:**
```
X-Content-Type-Options: nosniff
```

**Configuration:**
```properties
security.headers.enable-content-type-options=true
```

**Protection:**
- Forces browsers to respect declared Content-Type
- Prevents browsers from interpreting files as different MIME type
- Blocks MIME confusion attacks
- Prevents executing JavaScript disguised as images

**Why It Matters:**
Without this header, browsers might:
- Execute `image/png` file containing JavaScript
- Render `text/plain` file as HTML
- Execute CSS as JavaScript

---

### 5. X-XSS-Protection

**Purpose:** Enables browser's built-in XSS filter

**Header Example:**
```
X-XSS-Protection: 1; mode=block
```

**Configuration:**
```properties
security.headers.enable-xss-protection=true
```

**Mode Options:**
- `0` - Disable XSS filter
- `1` - Enable XSS filter
- `1; mode=block` - Enable and block page if XSS detected (recommended)

**Protection:**
- Stops reflected XSS attacks
- Blocks page rendering if attack detected
- Defense-in-depth with CSP

**Note:** Modern browsers rely more on CSP, but this provides backward compatibility

---

### 6. Referrer-Policy

**Purpose:** Controls how much referrer information is sent with requests

**Header Example:**
```
Referrer-Policy: strict-origin-when-cross-origin
```

**Configuration:**
```properties
security.headers.referrer-policy=strict-origin-when-cross-origin
```

**Policy Options:**
- `no-referrer` - Never send referrer
- `same-origin` - Send referrer only for same-origin requests
- `strict-origin` - Send only origin (not full URL) for cross-origin HTTPS
- `strict-origin-when-cross-origin` - Full URL for same-origin, origin for cross-origin (recommended)

**Protection:**
- Prevents referrer leakage of sensitive URLs
- Protects user privacy
- Reduces information disclosure
- Maintains analytics while protecting sensitive paths

---

### 7. Permissions-Policy

**Purpose:** Controls browser features and APIs

**Header Example:**
```
Permissions-Policy: geolocation=(), microphone=(), camera=(), payment=(), usb=(), magnetometer=(), gyroscope=(), accelerometer=()
```

**Configuration:**
```properties
security.headers.permissions-policy.enabled=true
security.headers.permissions-policy.geolocation=()
security.headers.permissions-policy.microphone=()
security.headers.permissions-policy.camera=()
security.headers.permissions-policy.payment=()
```

**Policy Syntax:**
- `()` - Disabled for all origins
- `(self)` - Enabled for same origin only
- `(self "https://example.com")` - Enabled for self and specific origin
- `*` - Enabled for all origins (not recommended)

**Protection:**
- Disables unnecessary browser features
- Prevents malicious scripts from accessing sensitive APIs
- Reduces attack surface
- Protects user privacy

---

## Implementation Details

### Architecture

**Components:**
1. **SecurityHeadersConfig.java** - Configuration properties
2. **SecurityHeadersFilter.java** - Servlet filter applying headers
3. **SecurityConfig.java** - Integration with Spring Security

### Filter Order

Security headers are applied first in the filter chain:
```
1. SecurityHeadersFilter  ← Adds security headers
2. RateLimitFilter        ← Rate limiting
3. JwtAuthenticationFilter ← Authentication
```

### Environment-Specific Configuration

#### Development (application-dev.properties)
```properties
security.headers.enabled=false  # Disabled by default
security.headers.frame-options.policy=SAMEORIGIN  # Allow H2 console
```

**Rationale:** Flexibility for development, H2 console requires frames

#### Staging (application-staging.properties)
```properties
security.headers.enabled=true
security.headers.require-https=true
security.headers.csp.report-only=true  # Test CSP without enforcing
```

**Rationale:** Test headers without breaking functionality

#### Production (application-prod.properties)
```properties
security.headers.enabled=true
security.headers.require-https=true
security.headers.hsts.enabled=true
security.headers.csp.enabled=true
security.headers.csp.report-only=false  # Enforce CSP
security.headers.frame-options.policy=DENY
```

**Rationale:** Maximum security, all protections enforced

---

## Configuration Guide

### Enabling Security Headers

#### Development
```bash
# In .env file
export SECURITY_HEADERS_ENABLED=true

# Start application
./dev.sh
```

#### Staging
```bash
export SPRING_PROFILES_ACTIVE=staging
export SECURITY_HEADERS_ENABLED=true
export SECURITY_REQUIRE_HTTPS=true
```

#### Production
```bash
export SPRING_PROFILES_ACTIVE=prod
export SECURITY_HEADERS_ENABLED=true
export SECURITY_REQUIRE_HTTPS=true
export HSTS_MAX_AGE=31536000
export CSP_DEFAULT_SRC="'self'"
export FRAME_OPTIONS_POLICY=DENY
```

### Customizing CSP for Specific Use Cases

#### Allow External Scripts (CDN)
```properties
security.headers.csp.script-src='self' https://cdn.example.com
```

#### Allow Inline Styles (if needed)
```properties
security.headers.csp.style-src='self' 'unsafe-inline'
```

**Note:** Avoid `'unsafe-inline'` for scripts as it defeats XSS protection

#### Allow Frame Embedding (specific domains)
```properties
security.headers.csp.frame-ancestors='self' https://trusted-site.com
```

---

## Testing Security Headers

### Manual Testing with cURL

```bash
# Test that security headers are present
curl -I http://localhost:8080/api/health

# Expected output:
# Strict-Transport-Security: max-age=31536000; includeSubDomains
# Content-Security-Policy: default-src 'self'; ...
# X-Frame-Options: DENY
# X-Content-Type-Options: nosniff
# X-XSS-Protection: 1; mode=block
# Referrer-Policy: strict-origin-when-cross-origin
# Permissions-Policy: geolocation=(), microphone=(), ...
```

### Browser Developer Tools

1. Open browser DevTools (F12)
2. Navigate to Network tab
3. Make request to API
4. Click on response
5. View "Response Headers" section
6. Verify all security headers are present

### Online Security Scanners

#### SecurityHeaders.com
```bash
# For deployed applications
https://securityheaders.com/?q=https://your-domain.com
```

**Target Grade:** A+

#### Mozilla Observatory
```bash
https://observatory.mozilla.org/
```

**Target Score:** 90+

### Automated Testing Script

```bash
#!/bin/bash
# test-security-headers.sh

BASE_URL="http://localhost:8080"

echo "Testing Security Headers..."
echo "=========================="

headers=$(curl -s -I $BASE_URL/api/health)

check_header() {
    local header=$1
    if echo "$headers" | grep -i "$header" > /dev/null; then
        echo "✓ $header present"
    else
        echo "✗ $header missing"
    fi
}

check_header "Strict-Transport-Security"
check_header "Content-Security-Policy"
check_header "X-Frame-Options"
check_header "X-Content-Type-Options"
check_header "X-XSS-Protection"
check_header "Referrer-Policy"
check_header "Permissions-Policy"

echo "=========================="
```

---

## Troubleshooting

### Problem: CSP blocks legitimate resources

**Symptom:** Browser console shows CSP violations, resources fail to load

**Solution:**
1. Check browser console for specific violations
2. Add allowed sources to appropriate CSP directive
3. Test in report-only mode first

**Example:**
```
Refused to load script from 'https://cdn.example.com' because it violates CSP
```

**Fix:**
```properties
security.headers.csp.script-src='self' https://cdn.example.com
```

### Problem: Application doesn't work in iframe

**Symptom:** Application blank when embedded in iframe

**Solution:**
1. Check X-Frame-Options policy
2. Change from `DENY` to `SAMEORIGIN` if needed
3. Or use CSP frame-ancestors directive

```properties
security.headers.frame-options.policy=SAMEORIGIN
# OR
security.headers.csp.frame-ancestors='self' https://trusted-embedder.com
```

### Problem: HSTS header not appearing

**Symptom:** Strict-Transport-Security header missing

**Causes:**
1. Connection is HTTP (not HTTPS)
2. HSTS disabled in configuration
3. Behind proxy without X-Forwarded-Proto header

**Solutions:**
```properties
# Force HSTS even on HTTP (for development)
security.headers.require-https=true

# Or ensure proxy sets header
X-Forwarded-Proto: https
```

### Problem: Inline styles/scripts blocked

**Symptom:** Styles or scripts don't work, CSP violations in console

**Cause:** CSP blocking inline content

**Solutions:**
1. **Recommended:** Move inline styles/scripts to external files
2. **Temporary:** Add nonce or hash to CSP
3. **Last resort:** Allow unsafe-inline (not recommended for scripts)

```properties
# Unsafe (defeats XSS protection for scripts)
security.headers.csp.script-src='self' 'unsafe-inline'

# Better: Use external files
# <script src="/js/app.js"></script>
```

---

## Security Best Practices

### DO
- ✅ Enable all security headers in production
- ✅ Use HSTS with long max-age (1 year minimum)
- ✅ Set restrictive CSP (default-src 'self')
- ✅ Use DENY for X-Frame-Options unless necessary
- ✅ Test in staging before production
- ✅ Monitor CSP violations
- ✅ Keep CSP policies as strict as possible

### DON'T
- ❌ Use 'unsafe-inline' for script-src in CSP
- ❌ Use 'unsafe-eval' in CSP
- ❌ Allow all origins (*) in CSP
- ❌ Disable security headers in production
- ❌ Set HSTS max-age below 6 months
- ❌ Ignore CSP violation reports

---

## Performance Impact

Security headers have **negligible performance impact**:
- **Header size:** ~500-1000 bytes per response
- **Processing overhead:** <0.1ms per request
- **Client-side:** No JavaScript execution needed
- **Caching:** Headers cached by browser

**Conclusion:** Security benefits far outweigh minimal overhead

---

## Compliance

This security headers implementation supports compliance with:

- **OWASP Top 10:** A05:2021 - Security Misconfiguration
- **NIST SP 800-53:** SC-8 (Transmission Confidentiality), SC-28 (Protection of Information at Rest)
- **PCI DSS:** Requirement 6.5 (Common Coding Vulnerabilities)
- **CIS Controls:** Control 14 (Controlled Access Based on Need to Know)
- **GDPR:** Article 32 (Security of Processing)

---

## References

- [OWASP Secure Headers Project](https://owasp.org/www-project-secure-headers/)
- [MDN Web Security Headers](https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers#security)
- [Content Security Policy Reference](https://content-security-policy.com/)
- [HSTS Preload List](https://hstspreload.org/)
- [Security Headers Scanner](https://securityheaders.com/)

---

## Change Log

| Date | Version | Changes | Author |
|------|---------|---------|--------|
| 2025-10-26 | 1.0 | Initial security headers implementation | Security Team |

---

**Next Steps:** Implement Environment-Based Security Configuration (Task 4)
