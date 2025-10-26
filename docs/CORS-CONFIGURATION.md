# CORS Configuration Guide

**Status**: Active
**Date**: 2025-10-26
**Section**: 12 of Security Hardening Plan

## Overview

This document describes the Cross-Origin Resource Sharing (CORS) configuration for the OSCAL Tools API. CORS is a security feature that controls which web applications can access the API from different origins (domains).

## What is CORS?

**Cross-Origin Resource Sharing (CORS)** is a security mechanism that allows web browsers to make requests to a different origin (domain, protocol, or port) than the one that served the web page.

Without CORS configuration, browsers block cross-origin requests by default as a security measure. The OSCAL Tools API needs CORS to allow the front-end (running on `http://localhost:3000`) to communicate with the back-end API (running on `http://localhost:8080`).

### Example

| Scenario | Same Origin? | Requires CORS? |
|----------|--------------|----------------|
| Frontend: `http://localhost:3000`<br/>Backend: `http://localhost:8080` | ❌ No (different port) | ✅ Yes |
| Frontend: `https://example.com`<br/>Backend: `http://example.com` | ❌ No (different protocol) | ✅ Yes |
| Frontend: `https://example.com`<br/>Backend: `https://api.example.com` | ❌ No (different subdomain) | ✅ Yes |
| Frontend: `https://example.com/app`<br/>Backend: `https://example.com/api` | ✅ Yes (same origin) | ❌ No |

## Current Configuration

### Architecture

The CORS configuration is managed through:

1. **Properties Files** (`application*.properties`):
   - Define allowed origins, methods, headers, and credentials settings
   - Environment-specific (dev, staging, prod)

2. **SecurityConfig.java**:
   - Reads properties and creates Spring Security CORS configuration
   - Applies CORS globally to all API endpoints

### Configuration Properties

Located in `back-end/src/main/resources/application*.properties`:

#### Default Configuration (`application.properties`)

```properties
# CORS Configuration (Override in environment-specific files)
cors.allowed-origins=${CORS_ALLOWED_ORIGINS:http://localhost:3000,http://localhost:3001}
cors.allowed-methods=${CORS_ALLOWED_METHODS:GET,POST,PUT,DELETE,OPTIONS}
cors.allowed-headers=${CORS_ALLOWED_HEADERS:*}
cors.allow-credentials=${CORS_ALLOW_CREDENTIALS:true}
```

#### Development Configuration (`application-dev.properties`)

```properties
# CORS Configuration (Permissive for development)
cors.allowed-origins=http://localhost:3000,http://localhost:3001
cors.allowed-methods=GET,POST,PUT,DELETE,OPTIONS
cors.allowed-headers=*
cors.allow-credentials=true
```

#### Production Configuration (`application-prod.properties`)

```properties
# CORS Configuration (Strict - production domains only)
cors.allowed-origins=${CORS_ALLOWED_ORIGINS}
cors.allowed-methods=GET,POST,PUT,DELETE,OPTIONS
cors.allowed-headers=Authorization,Content-Type
cors.allow-credentials=true
```

#### Staging Configuration (`application-staging.properties`)

```properties
# CORS Configuration (Restrictive for staging)
cors.allowed-origins=${CORS_ALLOWED_ORIGINS:https://staging.oscal-tools.example.com}
cors.allowed-methods=GET,POST,PUT,DELETE,OPTIONS
cors.allowed-headers=*
cors.allow-credentials=true
```

### Key Settings Explained

| Property | Description | Dev Value | Prod Value |
|----------|-------------|-----------|------------|
| `cors.allowed-origins` | Which domains can access the API | `localhost:3000,3001` | **Must be set via env var** |
| `cors.allowed-methods` | Which HTTP methods are allowed | All CRUD methods | All CRUD methods |
| `cors.allowed-headers` | Which headers can be sent | All (`*`) | Only essential headers |
| `cors.allow-credentials` | Allow cookies/auth headers | `true` | `true` |

### Exposed Headers

The following headers are exposed to the client (configured in `SecurityConfig.java`):

- `Authorization` - JWT token for authentication
- `X-RateLimit-Limit` - Maximum requests allowed per time window
- `X-RateLimit-Remaining` - Remaining requests in current window
- `X-RateLimit-Reset` - Timestamp when rate limit resets
- `Retry-After` - When to retry after rate limit exceeded

### Max Age

**Preflight cache duration**: 3600 seconds (1 hour)

Preflight requests (OPTIONS requests) check if cross-origin requests are allowed. Caching these responses for 1 hour reduces overhead.

## Environment-Specific Configuration

### Local Development

**Scenario**: Running frontend and backend locally

**Configuration**:
```properties
# application-dev.properties
cors.allowed-origins=http://localhost:3000,http://localhost:3001
cors.allowed-methods=GET,POST,PUT,DELETE,OPTIONS
cors.allowed-headers=*
cors.allow-credentials=true
```

**Why this works**:
- Allows both `localhost:3000` (default frontend port) and `localhost:3001` (alternative)
- Permits all headers for development flexibility
- Enables credentials (cookies, authorization headers)

**Start servers**:
```bash
# Terminal 1: Start backend
cd back-end
mvn spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=dev"

# Terminal 2: Start frontend
cd front-end
npm run dev
```

### Staging Environment

**Scenario**: Testing in staging environment before production

**Configuration**:
```properties
# application-staging.properties
cors.allowed-origins=${CORS_ALLOWED_ORIGINS:https://staging.oscal-tools.example.com}
cors.allowed-methods=GET,POST,PUT,DELETE,OPTIONS
cors.allowed-headers=*
cors.allow-credentials=true
```

**Environment Variable**:
```bash
export CORS_ALLOWED_ORIGINS=https://staging.oscal-tools.example.com
```

**Why this works**:
- Restricts access to only the staging frontend domain
- Still allows all headers for testing
- Falls back to default staging URL if not specified

### Production Environment

**Scenario**: Production deployment

**Configuration**:
```properties
# application-prod.properties (STRICT)
cors.allowed-origins=${CORS_ALLOWED_ORIGINS}
cors.allowed-methods=GET,POST,PUT,DELETE,OPTIONS
cors.allowed-headers=Authorization,Content-Type
cors.allow-credentials=true
```

**Environment Variable** (REQUIRED):
```bash
export CORS_ALLOWED_ORIGINS=https://oscal-tools.example.com
```

**Why this is strict**:
- ⚠️ **No default value** - must be explicitly set
- Only allows essential headers (Authorization, Content-Type)
- Only allows specified production domain(s)

**Multiple production domains**:
```bash
export CORS_ALLOWED_ORIGINS=https://oscal-tools.example.com,https://www.oscal-tools.example.com
```

## Security Best Practices

### ✅ DO

1. **Always use specific origins in production**:
   ```properties
   # Good
   cors.allowed-origins=https://example.com

   # Bad (security risk)
   cors.allowed-origins=*
   ```

2. **Use HTTPS in production**:
   ```properties
   # Good
   cors.allowed-origins=https://example.com

   # Bad (security risk)
   cors.allowed-origins=http://example.com
   ```

3. **Limit allowed headers in production**:
   ```properties
   # Good
   cors.allowed-headers=Authorization,Content-Type

   # Less secure
   cors.allowed-headers=*
   ```

4. **Use environment variables for production origins**:
   ```bash
   export CORS_ALLOWED_ORIGINS=https://example.com
   ```

5. **Test CORS configuration before deploying**:
   ```bash
   curl -H "Origin: https://example.com" \
        -H "Access-Control-Request-Method: POST" \
        -H "Access-Control-Request-Headers: Authorization,Content-Type" \
        -X OPTIONS \
        http://localhost:8080/api/health -v
   ```

### ❌ DON'T

1. **Never use wildcard (`*`) with credentials**:
   ```properties
   # INVALID - will cause errors
   cors.allowed-origins=*
   cors.allow-credentials=true
   ```
   Browsers reject this configuration as insecure.

2. **Never hardcode production origins**:
   ```properties
   # Bad - hardcoded
   cors.allowed-origins=https://prod.example.com

   # Good - configurable
   cors.allowed-origins=${CORS_ALLOWED_ORIGINS}
   ```

3. **Never allow all origins in production**:
   ```properties
   # Bad - security vulnerability
   cors.allowed-origins=*
   ```

4. **Never disable credentials if you use JWT**:
   ```properties
   # Bad - breaks authentication
   cors.allow-credentials=false
   ```

5. **Never commit production CORS origins to git**:
   ```properties
   # Bad - exposes infrastructure
   cors.allowed-origins=https://internal-prod-server.example.com
   ```

## Common CORS Errors and Solutions

### Error 1: "CORS policy: No 'Access-Control-Allow-Origin' header"

**Symptom**:
```
Access to fetch at 'http://localhost:8080/api/health' from origin 'http://localhost:3000'
has been blocked by CORS policy: No 'Access-Control-Allow-Origin' header is present
on the requested resource.
```

**Cause**: The frontend origin is not in `cors.allowed-origins`

**Solution**:
1. Check `application-dev.properties` includes your frontend URL:
   ```properties
   cors.allowed-origins=http://localhost:3000
   ```
2. Restart the backend
3. Clear browser cache (Cmd/Ctrl + Shift + R)

### Error 2: "CORS policy: The value of the 'Access-Control-Allow-Credentials' header is ''"

**Symptom**:
```
Access to fetch at 'http://localhost:8080/api/auth/login' has been blocked by CORS policy:
The value of the 'Access-Control-Allow-Credentials' header in the response is '' which
must be 'true' when the request's credentials mode is 'include'.
```

**Cause**: `cors.allow-credentials` is set to `false` but frontend is sending credentials

**Solution**:
Set `cors.allow-credentials=true` in your properties file

### Error 3: "CORS policy: Response to preflight request doesn't pass access control check"

**Symptom**:
```
Access to fetch at 'http://localhost:8080/api/validation/validate' has been blocked
by CORS policy: Response to preflight request doesn't pass access control check:
It does not have HTTP ok status.
```

**Cause**: The OPTIONS (preflight) request failed (returned non-200 status)

**Solution**:
1. Ensure OPTIONS method is allowed:
   ```properties
   cors.allowed-methods=GET,POST,PUT,DELETE,OPTIONS
   ```
2. Check SecurityConfig allows OPTIONS for the endpoint
3. Check if rate limiting is blocking OPTIONS requests

### Error 4: "CORS policy: Request header field X is not allowed"

**Symptom**:
```
Access to fetch at 'http://localhost:8080/api/ssp/create' has been blocked by CORS policy:
Request header field custom-header is not allowed by Access-Control-Allow-Headers in
preflight response.
```

**Cause**: Frontend is sending a header not in `cors.allowed-headers`

**Solution**:
1. Add the header to `cors.allowed-headers`:
   ```properties
   cors.allowed-headers=Authorization,Content-Type,Custom-Header
   ```
2. Or use `*` for development (not production):
   ```properties
   cors.allowed-headers=*
   ```

### Error 5: "Wildcard origin not allowed with credentials"

**Symptom**:
```
Access-Control-Allow-Origin: * is not allowed when credentials flag is true
```

**Cause**: Trying to use `*` for origins while `allow-credentials=true`

**Solution**:
Use specific origins instead of wildcard:
```properties
cors.allowed-origins=http://localhost:3000
cors.allow-credentials=true
```

## Testing CORS Configuration

### Test 1: Simple GET Request

```bash
curl -H "Origin: http://localhost:3000" \
     http://localhost:8080/api/health \
     -v
```

**Expected Response Headers**:
```
Access-Control-Allow-Origin: http://localhost:3000
Access-Control-Allow-Credentials: true
```

### Test 2: Preflight Request (OPTIONS)

```bash
curl -H "Origin: http://localhost:3000" \
     -H "Access-Control-Request-Method: POST" \
     -H "Access-Control-Request-Headers: Authorization,Content-Type" \
     -X OPTIONS \
     http://localhost:8080/api/validation/validate \
     -v
```

**Expected Response Headers**:
```
Access-Control-Allow-Origin: http://localhost:3000
Access-Control-Allow-Methods: GET,POST,PUT,DELETE,OPTIONS
Access-Control-Allow-Headers: Authorization,Content-Type
Access-Control-Allow-Credentials: true
Access-Control-Max-Age: 3600
```

### Test 3: POST Request with Credentials

```bash
curl -H "Origin: http://localhost:3000" \
     -H "Authorization: Bearer YOUR_TOKEN" \
     -H "Content-Type: application/json" \
     -X POST \
     -d '{"content":"test","format":"JSON"}' \
     http://localhost:8080/api/validation/validate \
     -v
```

**Expected Response Headers**:
```
Access-Control-Allow-Origin: http://localhost:3000
Access-Control-Allow-Credentials: true
Access-Control-Expose-Headers: Authorization,X-RateLimit-Limit,...
```

### Test 4: Unauthorized Origin

```bash
curl -H "Origin: https://malicious-site.com" \
     http://localhost:8080/api/health \
     -v
```

**Expected**: No `Access-Control-Allow-Origin` header (origin blocked)

### Test 5: Browser DevTools Test

1. Open browser DevTools (F12)
2. Go to Console tab
3. Run:
   ```javascript
   fetch('http://localhost:8080/api/health', {
     method: 'GET',
     headers: {
       'Content-Type': 'application/json'
     },
     credentials: 'include'
   })
   .then(response => response.json())
   .then(data => console.log('Success:', data))
   .catch(error => console.error('CORS Error:', error));
   ```

## Advanced Configuration

### Multiple Origins

**Development** (testing from multiple ports):
```properties
cors.allowed-origins=http://localhost:3000,http://localhost:3001,http://localhost:4200
```

**Production** (multiple subdomains):
```properties
cors.allowed-origins=https://app.example.com,https://www.example.com,https://admin.example.com
```

**Environment Variable**:
```bash
export CORS_ALLOWED_ORIGINS=https://app.example.com,https://www.example.com
```

### Dynamic Origin Configuration

For advanced use cases where you need to dynamically allow origins based on patterns (e.g., `*.example.com`), you would need to create a custom `CorsConfigurationSource` in `SecurityConfig.java`:

```java
@Bean
public CorsConfigurationSource corsConfigurationSource() {
    return request -> {
        String origin = request.getHeader("Origin");
        CorsConfiguration config = new CorsConfiguration();

        // Check if origin matches allowed pattern
        if (origin != null && origin.matches("https://.*\\.example\\.com")) {
            config.addAllowedOrigin(origin);
            config.setAllowedMethods(corsAllowedMethods);
            config.setAllowedHeaders(corsAllowedHeaders);
            config.setAllowCredentials(corsAllowCredentials);
            config.setMaxAge(3600L);
        }

        return config;
    };
}
```

**Note**: This is not currently implemented. Stick to explicit origins for security.

### CORS with Reverse Proxy (Nginx)

When deploying behind Nginx, you can handle CORS at the proxy level:

**Nginx Configuration**:
```nginx
location /api {
    # CORS headers
    add_header 'Access-Control-Allow-Origin' 'https://example.com' always;
    add_header 'Access-Control-Allow-Methods' 'GET, POST, PUT, DELETE, OPTIONS' always;
    add_header 'Access-Control-Allow-Headers' 'Authorization, Content-Type' always;
    add_header 'Access-Control-Allow-Credentials' 'true' always;

    # Preflight requests
    if ($request_method = 'OPTIONS') {
        add_header 'Access-Control-Max-Age' 3600;
        return 204;
    }

    proxy_pass http://localhost:8080;
}
```

**Important**: If using Nginx CORS headers, disable Spring CORS to avoid conflicts.

## Integration with Other Security Features

### CORS + JWT Authentication

CORS configuration works with JWT authentication:

1. **Preflight Request** (OPTIONS):
   - Browser sends OPTIONS request
   - No JWT token required
   - CORS headers returned

2. **Actual Request** (GET/POST/etc.):
   - Frontend sends JWT in `Authorization` header
   - Backend validates JWT
   - If valid, processes request and returns CORS headers

### CORS + Rate Limiting

Rate limiting respects CORS:

- Preflight requests (OPTIONS) bypass rate limiting
- Actual requests are rate-limited
- Rate limit headers are exposed via CORS (`X-RateLimit-*`)

### CORS + Security Headers

CORS works alongside security headers:

- CORS headers: Control cross-origin access
- Security headers (CSP, HSTS, etc.): Control content security
- Both are applied to responses independently

## Deployment Checklist

### Development Deployment

- [ ] Verify `cors.allowed-origins` includes `localhost:3000`
- [ ] Confirm `cors.allow-credentials=true`
- [ ] Test preflight requests work
- [ ] Test authenticated requests work

### Production Deployment

- [ ] Set `CORS_ALLOWED_ORIGINS` environment variable
- [ ] Use HTTPS origins only (`https://...`)
- [ ] Restrict `cors.allowed-headers` to essentials
- [ ] Remove development origins from configuration
- [ ] Test CORS from production frontend domain
- [ ] Verify unauthorized origins are blocked
- [ ] Monitor logs for CORS-related errors

## Monitoring and Logging

### Enable CORS Logging

Add to `application-dev.properties`:
```properties
logging.level.org.springframework.web.cors=DEBUG
```

**Sample Log Output**:
```
DEBUG o.s.web.cors.DefaultCorsProcessor : CORS request origin: http://localhost:3000
DEBUG o.s.web.cors.DefaultCorsProcessor : CORS configuration: CorsConfiguration[allowedOrigins=[http://localhost:3000]]
DEBUG o.s.web.cors.DefaultCorsProcessor : Allowed origin: http://localhost:3000
```

### Metrics to Monitor

- **CORS Preflight Requests**: Track OPTIONS request volume
- **CORS Errors**: Monitor blocked cross-origin requests
- **Origin Distribution**: Which origins are accessing the API
- **Credential Usage**: Track requests with credentials

## References

- **Spring Security CORS**: https://docs.spring.io/spring-security/reference/servlet/integrations/cors.html
- **MDN CORS Guide**: https://developer.mozilla.org/en-US/docs/Web/HTTP/CORS
- **OWASP CORS Security**: https://cheatsheetseries.owasp.org/cheatsheets/HTML5_Security_Cheat_Sheet.html#cross-origin-resource-sharing
- **Spring Boot CORS**: https://spring.io/guides/gs/rest-service-cors

## Troubleshooting Contacts

For CORS-related issues:
1. Check browser DevTools Console for specific error messages
2. Enable DEBUG logging for CORS
3. Test with `curl` to isolate browser vs server issues
4. Review SecurityConfig.java and application.properties
5. Create issue at: https://github.com/usnistgov/oscal-cli/issues

---

**Last Updated**: 2025-10-26
**Maintainer**: OSCAL Tools Security Team
