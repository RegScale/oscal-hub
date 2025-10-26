# SSL/TLS Certificates Directory

This directory contains SSL/TLS certificates for HTTPS configuration.

## Development Certificates

### Generate Self-Signed Certificate

For local development, generate a self-signed certificate:

```bash
cd certs
./generate-dev-cert.sh
```

This creates:
- `keystore.p12` - PKCS12 keystore with self-signed certificate
- Valid for 365 days
- Common Name (CN): localhost
- Subject Alternative Names (SAN): localhost, *.localhost, 127.0.0.1

**Default Password:** `changeit`

### Using the Development Certificate

Update `.env` or `application-dev.properties`:

```properties
server.ssl.enabled=true
server.ssl.key-store=file:certs/keystore.p12
server.ssl.key-store-password=changeit
server.ssl.key-store-type=PKCS12
server.ssl.key-alias=oscal-dev
server.port=8443
server.http.port=8080
server.ssl.redirect-http=true
```

Access application at: `https://localhost:8443`

**Note:** Browsers will show security warnings for self-signed certificates. This is expected.

## Production Certificates

### Option 1: Let's Encrypt (Recommended)

Free, automated SSL certificates:

```bash
# Install certbot
sudo apt-get install certbot

# Generate certificate for your domain
sudo certbot certonly --standalone -d oscal-tools.example.com

# Certificates will be in:
# /etc/letsencrypt/live/oscal-tools.example.com/
#   - fullchain.pem (certificate chain)
#   - privkey.pem (private key)
```

Convert to PKCS12 for Spring Boot:

```bash
sudo openssl pkcs12 -export \
  -in /etc/letsencrypt/live/oscal-tools.example.com/fullchain.pem \
  -inkey /etc/letsencrypt/live/oscal-tools.example.com/privkey.pem \
  -out keystore.p12 \
  -name oscal-prod \
  -passout pass:your-secure-password
```

### Option 2: Commercial Certificate

Purchase from Certificate Authority (DigiCert, Sectigo, etc.):

1. Generate Certificate Signing Request (CSR):
   ```bash
   keytool -certreq -alias oscal-prod -keystore keystore.p12 \
     -storepass your-password -file oscal.csr
   ```

2. Submit CSR to Certificate Authority

3. Receive signed certificate

4. Import certificate:
   ```bash
   keytool -import -alias oscal-prod -keystore keystore.p12 \
     -storepass your-password -file oscal-tools.crt
   ```

### Option 3: Use Nginx for HTTPS Termination (Recommended for Production)

Instead of configuring SSL in Spring Boot, use Nginx as reverse proxy:

- Nginx handles HTTPS/TLS
- Spring Boot runs HTTP only (internal)
- Simpler certificate management
- Better performance
- Easier to configure advanced features (HTTP/2, OCSP stapling, etc.)

See `nginx/` directory for configuration examples.

## Certificate Renewal

### Let's Encrypt Auto-Renewal

```bash
# Test renewal
sudo certbot renew --dry-run

# Set up automatic renewal (runs twice daily)
sudo systemctl enable certbot.timer
sudo systemctl start certbot.timer
```

### Manual Renewal

Repeat the certificate generation process before expiration.

Monitor expiration with:
```bash
openssl x509 -in certificate.crt -noout -enddate
```

## Security Best Practices

- ✅ Use certificates from trusted CA in production
- ✅ Never commit private keys to version control
- ✅ Use strong passwords for keystores
- ✅ Rotate certificates before expiration
- ✅ Enable OCSP stapling
- ✅ Use TLS 1.2 and TLS 1.3 only
- ✅ Disable weak cipher suites
- ❌ Never use self-signed certificates in production
- ❌ Never share private keys
- ❌ Don't use expired certificates

## Files in This Directory

```
certs/
├── README.md                    # This file
├── generate-dev-cert.sh         # Script to generate dev certificates
├── keystore.p12                 # Development keystore (gitignored)
└── .gitignore                   # Prevents committing sensitive files
```

**Important:** `keystore.p12` and any production certificates are in `.gitignore` and will not be committed to version control.

## Troubleshooting

### "Certificate not trusted" error

**Cause:** Using self-signed certificate

**Solutions:**
- Development: Accept browser warning
- Production: Use certificate from trusted CA

### "Certificate expired" error

**Cause:** Certificate validity period has ended

**Solution:** Generate new certificate

### "SSL handshake failed" error

**Cause:** TLS protocol or cipher mismatch

**Solutions:**
- Check TLS version (use TLS 1.2+)
- Verify cipher suite compatibility
- Check certificate chain is complete

## References

- [Let's Encrypt](https://letsencrypt.org/)
- [Spring Boot SSL Configuration](https://docs.spring.io/spring-boot/docs/current/reference/html/application-properties.html#application-properties.server.server.ssl)
- [Mozilla SSL Configuration Generator](https://ssl-config.mozilla.org/)
