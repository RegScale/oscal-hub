# Rate Limiting Implementation Guide

**Date:** 2025-10-26
**Status:** Implemented
**Component:** Back-end API Security

## Overview

This document describes the rate limiting implementation for the OSCAL Tools API. Rate limiting protects the application from:
- **Brute force attacks** on authentication endpoints
- **API abuse** and denial-of-service attempts
- **Resource exhaustion** from excessive requests
- **Spam/bot registration** attempts

## Implementation Details

### Architecture

The rate limiting system uses the **Token Bucket Algorithm** implemented via:
- **Bucket4j 8.10.1** - Token bucket rate limiting library
- **Caffeine Cache** - In-memory cache for bucket storage
- **Per-IP tracking** for unauthenticated endpoints
- **Per-user tracking** for authenticated API endpoints

### Rate Limiting Strategy

| Endpoint Type | Identifier | Default Limit | Duration | Purpose |
|--------------|-----------|---------------|----------|---------|
| `/api/auth/login` | IP Address | 5 attempts | 60 seconds | Prevent brute force password attacks |
| `/api/auth/register` | IP Address | 3 attempts | 1 hour | Prevent spam registrations |
| `/api/*` (authenticated) | Username | 100 requests | 60 seconds | Prevent API abuse |

### Key Components

#### 1. RateLimitConfig.java
**Location:** `back-end/src/main/java/.../config/RateLimitConfig.java`

Configuration class for rate limiting settings:
```java
@ConfigurationProperties(prefix = "rate.limit")
public class RateLimitConfig {
    private boolean enabled = false;
    private LoginRateLimit login;
    private RegistrationRateLimit registration;
    private ApiRateLimit api;
}
```

#### 2. RateLimitService.java
**Location:** `back-end/src/main/java/.../service/RateLimitService.java`

Service managing rate limit buckets and enforcement:
- Maintains separate caches for login, registration, and API limits
- Creates token buckets with configurable capacity and refill rates
- Tracks remaining attempts and reset times
- Provides cache management functions

#### 3. RateLimitFilter.java
**Location:** `back-end/src/main/java/.../filter/RateLimitFilter.java`

Servlet filter intercepting HTTP requests:
- Applied before authentication filter
- Checks rate limits based on endpoint and identifier
- Returns HTTP 429 when limit exceeded
- Adds standard rate limit headers to responses

## Configuration

### Application Properties

#### Development (`application-dev.properties`)
```properties
# Rate limiting disabled by default in development
rate.limit.enabled=${RATE_LIMIT_ENABLED:false}
rate.limit.login.attempts=5
rate.limit.login.duration=60
rate.limit.registration.attempts=3
rate.limit.registration.duration=3600
rate.limit.api.requests=100
rate.limit.api.duration=60
```

#### Staging (`application-staging.properties`)
```properties
# Rate limiting enabled in staging
rate.limit.enabled=true
rate.limit.login.attempts=${RATE_LIMIT_LOGIN_ATTEMPTS:5}
rate.limit.login.duration=${RATE_LIMIT_LOGIN_DURATION:60}
rate.limit.registration.attempts=${RATE_LIMIT_REGISTRATION_ATTEMPTS:3}
rate.limit.registration.duration=${RATE_LIMIT_REGISTRATION_DURATION:3600}
rate.limit.api.requests=${RATE_LIMIT_API_REQUESTS:100}
rate.limit.api.duration=${RATE_LIMIT_API_DURATION:60}
```

#### Production (`application-prod.properties`)
```properties
# Rate limiting REQUIRED in production
rate.limit.enabled=true
rate.limit.login.attempts=${RATE_LIMIT_LOGIN_ATTEMPTS:5}
rate.limit.login.duration=${RATE_LIMIT_LOGIN_DURATION:60}
rate.limit.registration.attempts=${RATE_LIMIT_REGISTRATION_ATTEMPTS:3}
rate.limit.registration.duration=${RATE_LIMIT_REGISTRATION_DURATION:3600}
rate.limit.api.requests=${RATE_LIMIT_API_REQUESTS:100}
rate.limit.api.duration=${RATE_LIMIT_API_DURATION:60}
```

### Environment Variables

Set these in `.env` file or environment:

```bash
# Enable/disable rate limiting
RATE_LIMIT_ENABLED=true

# Login rate limiting
RATE_LIMIT_LOGIN_ATTEMPTS=5
RATE_LIMIT_LOGIN_DURATION=60

# Registration rate limiting
RATE_LIMIT_REGISTRATION_ATTEMPTS=3
RATE_LIMIT_REGISTRATION_DURATION=3600

# API rate limiting
RATE_LIMIT_API_REQUESTS=100
RATE_LIMIT_API_DURATION=60
```

## HTTP Response Headers

The rate limiting filter adds standard headers to all API responses:

### Standard Headers

```
X-RateLimit-Limit: <total requests allowed>
X-RateLimit-Remaining: <requests remaining>
X-RateLimit-Reset: <unix timestamp when limit resets>
```

### When Rate Limited (HTTP 429)

```
HTTP/1.1 429 Too Many Requests
X-RateLimit-Limit: 5
X-RateLimit-Remaining: 0
X-RateLimit-Reset: 1698012345
Retry-After: 60
Content-Type: application/json

{
  "error": "Too Many Requests",
  "message": "Too many login attempts. Please try again in 60 seconds.",
  "retryAfter": 60,
  "timestamp": 1698012285000
}
```

## Client Integration

### JavaScript/TypeScript Example

```typescript
async function loginWithRateLimit(username: string, password: string) {
  try {
    const response = await fetch('http://localhost:8080/api/auth/login', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({ username, password })
    });

    // Check rate limit headers
    const remaining = response.headers.get('X-RateLimit-Remaining');
    const reset = response.headers.get('X-RateLimit-Reset');

    console.log(`Login attempts remaining: ${remaining}`);

    if (response.status === 429) {
      const error = await response.json();
      const retryAfter = error.retryAfter;
      throw new Error(`Rate limited. Retry after ${retryAfter} seconds.`);
    }

    if (!response.ok) {
      throw new Error('Login failed');
    }

    return await response.json();
  } catch (error) {
    console.error('Login error:', error);
    throw error;
  }
}
```

### cURL Example

```bash
# First login attempt
curl -i -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"test","password":"wrong"}'

# Response headers show rate limit status:
# X-RateLimit-Limit: 5
# X-RateLimit-Remaining: 4
# X-RateLimit-Reset: 1698012345

# After 5 failed attempts, you get:
# HTTP/1.1 429 Too Many Requests
# Retry-After: 60
# {
#   "error": "Too Many Requests",
#   "message": "Too many login attempts. Please try again in 60 seconds."
# }
```

## Testing Rate Limiting

### Enable Rate Limiting in Development

```bash
# In .env file
export RATE_LIMIT_ENABLED=true

# Or inline
RATE_LIMIT_ENABLED=true ./dev.sh
```

### Test Login Rate Limiting

```bash
# Script to test login rate limiting
for i in {1..10}; do
  echo "Attempt $i"
  curl -s -o /dev/null -w "HTTP Status: %{http_code}\n" \
    -X POST http://localhost:8080/api/auth/login \
    -H "Content-Type: application/json" \
    -d '{"username":"test","password":"wrong"}'
  sleep 1
done

# Expected output:
# Attempt 1 - HTTP Status: 401 (Unauthorized)
# Attempt 2 - HTTP Status: 401
# Attempt 3 - HTTP Status: 401
# Attempt 4 - HTTP Status: 401
# Attempt 5 - HTTP Status: 401
# Attempt 6 - HTTP Status: 429 (Too Many Requests)
# Attempt 7 - HTTP Status: 429
# ...
```

### Test API Rate Limiting

```bash
# Test API rate limiting (requires authentication)
TOKEN="your-jwt-token-here"

for i in {1..110}; do
  echo "Request $i"
  curl -s -o /dev/null -w "HTTP Status: %{http_code}\n" \
    -H "Authorization: Bearer $TOKEN" \
    http://localhost:8080/api/visualization/ssp

  # Small delay to avoid overwhelming server
  sleep 0.1
done

# Expected: First 100 requests succeed, then HTTP 429
```

### Test Registration Rate Limiting

```bash
# Test registration rate limiting
for i in {1..5}; do
  echo "Registration attempt $i"
  curl -s -o /dev/null -w "HTTP Status: %{http_code}\n" \
    -X POST http://localhost:8080/api/auth/register \
    -H "Content-Type: application/json" \
    -d "{\"username\":\"user$i\",\"password\":\"password123\",\"email\":\"user$i@example.com\"}"
  sleep 1
done

# Expected: First 3 attempts proceed normally, then HTTP 429
```

## Performance Considerations

### Cache Configuration

The rate limiting service uses three separate Caffeine caches:

```java
// Login bucket cache
Cache<String, Bucket> loginBucketCache = Caffeine.newBuilder()
    .expireAfterAccess(10, TimeUnit.MINUTES)
    .maximumSize(10_000)
    .build();

// Registration bucket cache
Cache<String, Bucket> registrationBucketCache = Caffeine.newBuilder()
    .expireAfterAccess(2, TimeUnit.HOURS)
    .maximumSize(10_000)
    .build();

// API bucket cache
Cache<String, Bucket> apiBucketCache = Caffeine.newBuilder()
    .expireAfterAccess(5, TimeUnit.MINUTES)
    .maximumSize(50_000)
    .build();
```

### Memory Impact

- **Per bucket overhead:** ~200-300 bytes
- **Maximum memory (worst case):**
  - Login cache: 10,000 buckets × 300 bytes = ~3 MB
  - Registration cache: 10,000 buckets × 300 bytes = ~3 MB
  - API cache: 50,000 buckets × 300 bytes = ~15 MB
  - **Total:** ~21 MB maximum

### Performance Overhead

- **Filter overhead:** <1ms per request
- **Cache lookup:** O(1) constant time
- **Token consumption:** O(1) constant time

## Security Considerations

### IP Address Spoofing

The filter extracts the client IP from:
1. `X-Forwarded-For` header (first IP if multiple)
2. `X-Real-IP` header
3. `request.getRemoteAddr()` (fallback)

**Production Recommendation:** Configure your load balancer/proxy to:
- Strip untrusted `X-Forwarded-For` headers from clients
- Add trusted `X-Forwarded-For` with actual client IP
- Use `X-Real-IP` for single-IP scenarios

### Distributed Deployments

Current implementation uses **in-memory caching** (Caffeine), which means:
- ✅ Fast and efficient for single-instance deployments
- ❌ Each instance maintains separate rate limit counters
- ⚠️  For multi-instance deployments, consider:
  - Redis-backed Bucket4j implementation
  - Shared distributed cache
  - Load balancer session affinity

### Bypass Prevention

Rate limiting is applied at the filter level **before authentication**, making it difficult to bypass:
- Cannot use valid credentials to bypass login rate limits
- Cannot create tokens to bypass API rate limits
- Cannot register multiple accounts to bypass registration limits

However, attackers could:
- Use multiple IP addresses (mitigated by lowering limits)
- Use proxy/VPN services (consider IP reputation services)
- Distribute attacks over time (acceptable trade-off)

## Administration

### Clearing Rate Limits

For administrative purposes or testing:

```java
@Autowired
private RateLimitService rateLimitService;

// Clear all rate limit caches
rateLimitService.clearAllCaches();

// Clear rate limit for specific IP
rateLimitService.clearRateLimitForIp("192.168.1.100");
```

### Monitoring

Monitor these metrics in production:
- Number of HTTP 429 responses (indicates abuse or legitimate high traffic)
- Top IP addresses hitting rate limits (potential attackers)
- Cache sizes and eviction rates
- False positive rate (legitimate users being rate limited)

Add logging:
```bash
# Enable rate limiting debug logging
logging.level.gov.nist.oscal.tools.api.filter.RateLimitFilter=DEBUG
logging.level.gov.nist.oscal.tools.api.service.RateLimitService=DEBUG
```

## Customization Examples

### Stricter Login Limits

```properties
# Allow only 3 attempts per 5 minutes
rate.limit.login.attempts=3
rate.limit.login.duration=300
```

### More Lenient API Limits

```properties
# Allow 500 requests per 5 minutes
rate.limit.api.requests=500
rate.limit.api.duration=300
```

### Different Limits for Different Environments

```bash
# Development: Relaxed limits for testing
RATE_LIMIT_LOGIN_ATTEMPTS=100
RATE_LIMIT_API_REQUESTS=1000

# Staging: Moderate limits
RATE_LIMIT_LOGIN_ATTEMPTS=10
RATE_LIMIT_API_REQUESTS=200

# Production: Strict limits
RATE_LIMIT_LOGIN_ATTEMPTS=5
RATE_LIMIT_API_REQUESTS=100
```

## Troubleshooting

### Problem: Legitimate users being rate limited

**Symptoms:** Users report "Too Many Requests" errors during normal usage

**Solutions:**
1. **Increase limits:** Adjust `rate.limit.*.attempts` upward
2. **Increase duration:** Extend `rate.limit.*.duration` for longer windows
3. **Check for shared IPs:** Corporate NAT/proxy may cause multiple users to share IP
4. **Review logs:** Identify specific patterns causing limits

```bash
# Check rate limit logs
tail -f back-end/logs/spring.log | grep RateLimit
```

### Problem: Rate limiting not working

**Symptoms:** No HTTP 429 responses even after exceeding limits

**Checks:**
1. **Verify enabled:**
   ```bash
   # Check if rate limiting is enabled
   curl http://localhost:8080/actuator/env | grep rate.limit.enabled
   ```

2. **Check filter registration:**
   ```bash
   # Look for filter initialization in startup logs
   grep -i "RateLimitFilter" back-end/logs/spring.log
   ```

3. **Verify configuration:**
   ```bash
   # Check application logs for configuration errors
   grep -i "rate" back-end/logs/spring.log
   ```

### Problem: Rate limits reset too quickly

**Cause:** Token bucket refills tokens over time, not all at once

**Understanding:** If limit is 5 requests per 60 seconds:
- Tokens refill at rate of 1 per 12 seconds
- Not a hard cutoff at 60 seconds

**Solution:** This is expected behavior. For hard cutoffs, consider sliding window implementation.

## Compliance

This rate limiting implementation supports:
- **OWASP Top 10:** A07:2021 - Identification and Authentication Failures
- **NIST SP 800-63B:** Protection against online guessing attacks
- **CIS Controls:** Control 6.2 - Ensure Use of Automated Tools
- **PCI DSS:** Requirement 8.1.6 - Limit repeated access attempts

## References

- [Bucket4j Documentation](https://bucket4j.com/)
- [OWASP Rate Limiting Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Denial_of_Service_Cheat_Sheet.html#rate-limiting)
- [RFC 6585 - HTTP 429 Too Many Requests](https://tools.ietf.org/html/rfc6585#section-4)
- [Token Bucket Algorithm](https://en.wikipedia.org/wiki/Token_bucket)

## Change Log

| Date | Version | Changes | Author |
|------|---------|---------|--------|
| 2025-10-26 | 1.0 | Initial rate limiting implementation | Security Team |

---

**Next Steps:** Implement Security Headers (Task 3)
