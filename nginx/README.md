# Nginx Configuration for OSCAL Tools

This directory contains Nginx configuration files for production HTTPS deployment.

## Overview

Nginx acts as a reverse proxy providing:
- **HTTPS termination** - Handles SSL/TLS encryption
- **Load balancing** - Distributes traffic (if multiple backends)
- **Rate limiting** - Protects against abuse
- **Security headers** - Adds additional security headers
- **Static file serving** - Optimized delivery of static assets
- **HTTP/2** - Improved performance

## Architecture

```
Internet
    ↓
Nginx (HTTPS:443, HTTP:80)
    ├─→ Spring Boot Backend (HTTP:8080) - /api/*
    └─→ Next.js Frontend (HTTP:3000)    - /*
```

## Files

```
nginx/
├── nginx.conf                    # Main Nginx configuration
├── conf.d/
│   └── oscal-tools.conf         # OSCAL Tools site configuration
├── ssl/                         # SSL certificates (not in git)
│   ├── fullchain.pem
│   ├── privkey.pem
│   └── chain.pem
└── README.md                    # This file
```

## Setup Instructions

### 1. Install Nginx

**Ubuntu/Debian:**
```bash
sudo apt-get update
sudo apt-get install nginx
```

**RHEL/CentOS:**
```bash
sudo yum install nginx
```

**Docker:**
```bash
# Use official Nginx image (see docker-compose.prod.yml)
docker pull nginx:alpine
```

### 2. Obtain SSL Certificate

#### Option A: Let's Encrypt (Recommended)

```bash
# Install certbot
sudo apt-get install certbot python3-certbot-nginx

# Obtain certificate
sudo certbot --nginx -d oscal-tools.example.com -d www.oscal-tools.example.com

# Certificates will be in /etc/letsencrypt/live/oscal-tools.example.com/
```

#### Option B: Manual Certificate

Place your certificates in `nginx/ssl/`:
- `fullchain.pem` - Certificate chain
- `privkey.pem` - Private key
- `chain.pem` - CA chain (for OCSP stapling)

### 3. Configure Domain

Edit `nginx/conf.d/oscal-tools.conf` and replace:
```nginx
server_name oscal-tools.example.com www.oscal-tools.example.com;
```

With your actual domain name.

### 4. Copy Configuration Files

```bash
# Copy main config
sudo cp nginx/nginx.conf /etc/nginx/nginx.conf

# Copy site config
sudo cp nginx/conf.d/oscal-tools.conf /etc/nginx/conf.d/

# Copy SSL certificates
sudo mkdir -p /etc/nginx/ssl
sudo cp nginx/ssl/* /etc/nginx/ssl/
sudo chmod 600 /etc/nginx/ssl/privkey.pem
```

### 5. Test Configuration

```bash
# Test Nginx configuration
sudo nginx -t

# Expected output:
# nginx: the configuration file /etc/nginx/nginx.conf syntax is ok
# nginx: configuration file /etc/nginx/nginx.conf test is successful
```

### 6. Start Nginx

```bash
# Start Nginx
sudo systemctl start nginx

# Enable on boot
sudo systemctl enable nginx

# Check status
sudo systemctl status nginx
```

### 7. Verify HTTPS

```bash
# Test HTTPS
curl -I https://oscal-tools.example.com

# Check security headers
curl -I https://oscal-tools.example.com | grep -E "Strict-Transport|Content-Security|X-Frame"

# Test SSL configuration
openssl s_client -connect oscal-tools.example.com:443 -tls1_2
```

## Testing

### Test SSL/TLS Configuration

**Online Tools:**
- [SSL Labs SSL Test](https://www.ssllabs.com/ssltest/) - **Target: A+ rating**
- [SecurityHeaders.com](https://securityheaders.com/) - **Target: A+ rating**

**Command Line:**
```bash
# Test TLS versions
openssl s_client -connect oscal-tools.example.com:443 -tls1_2
openssl s_client -connect oscal-tools.example.com:443 -tls1_3

# Test cipher suites
nmap --script ssl-enum-ciphers -p 443 oscal-tools.example.com
```

### Test Rate Limiting

```bash
# Test API rate limiting (should block after 10 req/sec)
for i in {1..20}; do curl -s https://oscal-tools.example.com/api/health; done

# Test auth rate limiting (should block after 5 req/min)
for i in {1..10}; do
  curl -X POST https://oscal-tools.example.com/api/auth/login \
    -H "Content-Type: application/json" \
    -d '{"username":"test","password":"wrong"}'
done
```

### Test HTTP to HTTPS Redirect

```bash
# Should redirect to HTTPS
curl -I http://oscal-tools.example.com

# Expected output:
# HTTP/1.1 301 Moved Permanently
# Location: https://oscal-tools.example.com/
```

## Security Features

### TLS Configuration

- ✅ **TLS 1.2 and 1.3 only** - Disabled SSL, TLS 1.0, TLS 1.1
- ✅ **Strong cipher suites** - OWASP recommended ciphers
- ✅ **OCSP stapling** - Faster certificate validation
- ✅ **Perfect Forward Secrecy** - ECDHE and DHE key exchange

### Security Headers

All responses include:
- `Strict-Transport-Security` - Force HTTPS
- `X-Frame-Options` - Prevent clickjacking
- `X-Content-Type-Options` - Prevent MIME sniffing
- `X-XSS-Protection` - Enable browser XSS filter
- `Referrer-Policy` - Control referrer information
- `Content-Security-Policy` - Prevent XSS and injection
- `Permissions-Policy` - Control browser features

### Rate Limiting

- **API endpoints:** 10 requests/second
- **Auth endpoints:** 5 requests/minute
- Configured via `limit_req_zone` directives

## Monitoring

### Access Logs

```bash
# View access logs
sudo tail -f /var/log/nginx/access.log

# View error logs
sudo tail -f /var/log/nginx/error.log

# Count requests by endpoint
sudo cat /var/log/nginx/access.log | awk '{print $7}' | sort | uniq -c | sort -rn
```

### Performance Monitoring

```bash
# Monitor active connections
sudo watch -n 1 "echo 'Active: ' && netstat -an | grep :443 | wc -l"

# Check Nginx status (if status module enabled)
curl http://localhost/nginx_status
```

## Troubleshooting

### Problem: 502 Bad Gateway

**Cause:** Backend not running or not accessible

**Solution:**
```bash
# Check if backend is running
curl http://localhost:8080/api/health

# Check Nginx error log
sudo tail /var/log/nginx/error.log

# Verify proxy_pass addresses
sudo nginx -T | grep proxy_pass
```

### Problem: SSL certificate error

**Cause:** Certificate expired or misconfigured

**Solution:**
```bash
# Check certificate expiration
openssl x509 -in /etc/nginx/ssl/fullchain.pem -noout -dates

# Renew Let's Encrypt certificate
sudo certbot renew

# Test SSL configuration
sudo nginx -t
```

### Problem: Rate limiting too aggressive

**Cause:** Legitimate traffic being blocked

**Solution:**
Edit `nginx/conf.d/oscal-tools.conf`:
```nginx
# Increase rate limits
limit_req_zone $binary_remote_addr zone=api_limit:10m rate=20r/s;
limit_req_zone $binary_remote_addr zone=auth_limit:10m rate=10r/m;
```

Reload Nginx:
```bash
sudo nginx -s reload
```

## Certificate Renewal

### Let's Encrypt Auto-Renewal

```bash
# Test renewal
sudo certbot renew --dry-run

# Automatic renewal (runs twice daily)
sudo systemctl enable certbot.timer
sudo systemctl start certbot.timer

# Check timer status
sudo systemctl status certbot.timer
```

### Manual Renewal

```bash
# Renew certificates
sudo certbot renew

# Reload Nginx
sudo nginx -s reload
```

## Performance Tuning

### Worker Processes

```nginx
# Set to number of CPU cores
worker_processes auto;
```

### Connection Limits

```nginx
events {
    worker_connections 2048;  # Increase for high traffic
    use epoll;                # Efficient on Linux
}
```

### Caching

```nginx
# Enable proxy caching for static assets
proxy_cache_path /var/cache/nginx levels=1:2 keys_zone=my_cache:10m max_size=1g;
```

## References

- [Nginx Documentation](https://nginx.org/en/docs/)
- [Mozilla SSL Configuration Generator](https://ssl-config.mozilla.org/)
- [OWASP Nginx Security](https://cheatsheetseries.owasp.org/cheatsheets/Nginx_Security_Cheat_Sheet.html)
