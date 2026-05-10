# Nginx Reverse Proxy Configuration

Nginx terminates HTTPS in front of OSCAL Hub when deploying via Docker
Compose. It is mounted into the `nginx` service defined in
[`../docker-compose.yml`](../docker-compose.yml).

## Files

```
nginx/
├── nginx.conf                # Main Nginx config (gzip, rate-limit zones, log format)
├── conf.d/
│   ├── oscal.conf.example    # Site config template — copy to oscal.conf and edit
│   └── oscal.conf            # (gitignored) — your customized site config
├── ssl/                      # (gitignored) — TLS material lives here
│   ├── fullchain.pem
│   ├── privkey.pem
│   └── chain.pem
└── README.md                 # This file
```

## Setup

1. Copy the site template and edit your hostname:

   ```bash
   cp conf.d/oscal.conf.example conf.d/oscal.conf
   $EDITOR conf.d/oscal.conf       # replace oscal-tools.example.com
   ```

2. Place the certificate / key / chain into `ssl/`:

   ```
   ssl/fullchain.pem    # full certificate chain (server + intermediates)
   ssl/privkey.pem      # private key (mode 0600)
   ssl/chain.pem        # intermediate chain (used for OCSP stapling)
   ```

   Let's Encrypt outputs all three under `/etc/letsencrypt/live/<domain>/`.
   For ACME HTTP-01 issuance, the site config already exposes
   `/.well-known/acme-challenge/`.

3. Bring the stack up — Nginx will load the config on first start and
   reload it whenever you `docker compose up -d nginx`.

## Without Nginx

If TLS is terminated upstream (cloud LB, ingress controller, customer
proxy), don't run this container at all:

```bash
docker compose up -d --scale nginx=0 postgres oscal-hub
```

Then route traffic at the LB:

- `/api/*` → `oscal-hub:8080`
- `/*`     → `oscal-hub:3000`

## Reference

The site template (`oscal.conf.example`) sets:

- TLS 1.2 + 1.3 only, OWASP-recommended cipher suite
- HSTS (1 year, includeSubDomains, preload), CSP, frame-options DENY,
  XSS / nosniff / referrer-policy headers
- Rate limits (`api_limit` 10 r/s, `auth_limit` 5 r/m) defined in
  `nginx.conf`
- WebSocket upgrade support for the Next.js frontend
- Static-asset caching for `/_next/static/`

Tune any of those by editing `oscal.conf` or `nginx.conf` and reloading
the container.
