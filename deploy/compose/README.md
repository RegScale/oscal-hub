# Docker Compose Deployment

Deploy OSCAL Hub on a single VM (or Docker host) with PostgreSQL + Nginx
reverse proxy. Suitable for small/medium on-prem installs and customer-cloud
deployments where Kubernetes is not in play.

For Kubernetes, use the [Helm chart](../helm/oscal-hub/) instead.

## Prerequisites

- Docker Engine 20.10+ and Docker Compose v2 (`docker compose` subcommand)
- A Linux host with at least 2 vCPU and 4 GB RAM
- (Optional) A TLS certificate for your domain — Let's Encrypt is fine

## Quickstart

```bash
cd deploy/compose
cp .env.example .env

# Generate secrets
echo "DB_PASSWORD=$(openssl rand -base64 32 | tr -d '\n')" >> .env
echo "JWT_SECRET=$(openssl rand -base64 64 | tr -d '\n')"  >> .env

# Edit .env and set PUBLIC_ORIGIN to your URL
$EDITOR .env

# Configure TLS (see "TLS / HTTPS" below) — or skip for HTTP-only testing
cp nginx/conf.d/oscal.conf.example nginx/conf.d/oscal.conf
$EDITOR nginx/conf.d/oscal.conf
mkdir -p nginx/ssl && cp /path/to/{fullchain,privkey,chain}.pem nginx/ssl/

docker compose up -d
docker compose logs -f oscal-hub
```

The first start takes a few minutes — the image builds, Postgres
initializes, then Flyway runs all migrations. The app is ready when
`docker compose ps` shows `oscal-hub` as `healthy`.

Visit `${PUBLIC_ORIGIN}` (or `http://localhost` for a quick local test
before TLS). The default super-admin account is created during first-run
boot — check the backend logs for the printed credentials.

## TLS / HTTPS

The bundled Nginx terminates HTTPS. Two options:

**Option A — Bring your own certs.** Drop these three files into
`nginx/ssl/`:

```
nginx/ssl/fullchain.pem
nginx/ssl/privkey.pem
nginx/ssl/chain.pem
```

Then in `nginx/conf.d/oscal.conf` (copied from
`oscal.conf.example`), replace `oscal-tools.example.com` with your
hostname.

**Option B — Let's Encrypt with certbot.** Easiest path is to issue
the certificate on the host (DNS-01 or HTTP-01) and bind-mount the
result. The `oscal.conf.example` already includes the
`/.well-known/acme-challenge/` location for HTTP-01 issuance — point it
at a writable directory on the host.

**Option C — Terminate TLS upstream.** If a cloud load balancer or
upstream proxy already does TLS, scale Nginx to zero and expose
`oscal-hub:3000` / `oscal-hub:8080` directly:

```bash
docker compose up -d --scale nginx=0 postgres oscal-hub
# Then route your LB:  /api → oscal-hub:8080,  /* → oscal-hub:3000
```

Set `SECURITY_REQUIRE_HTTPS=false` in `.env` if the app receives plain
HTTP from the LB.

## Object storage

By default OSCAL Hub stores uploaded files inside the container, persisted
to the `oscal_files` Docker volume. To use external object storage, set
`STORAGE_PROVIDER` in `.env` to `s3`, `gcs`, or `azure` and provide the
matching credentials. See `.env.example` for the variables each provider
needs. S3-compatible stores like MinIO work via the `s3` provider.

## Operations

```bash
# Status
docker compose ps

# Tail logs
docker compose logs -f oscal-hub

# Apply a new image (pull or rebuild then restart)
docker compose pull       # if using OSCAL_HUB_IMAGE
docker compose build      # if building from source
docker compose up -d

# Stop everything (data preserved)
docker compose down

# Stop + delete volumes (DESTRUCTIVE — wipes the database)
docker compose down -v
```

### Backups

Take regular logical backups of the database volume:

```bash
docker compose exec -T postgres \
    pg_dump -U "$DB_USERNAME" -d "$DB_NAME" -Fc \
    > oscal-$(date +%F).dump
```

Restore:

```bash
docker compose exec -T postgres \
    pg_restore -U "$DB_USERNAME" -d "$DB_NAME" -c \
    < oscal-2026-05-08.dump
```

The `oscal_files` volume holds uploaded artifacts when running with the
default local-filesystem storage provider — back it up alongside the
database. When using `STORAGE_PROVIDER=s3|gcs|azure` the bucket itself is
the system of record.

### Upgrades

1. Bump the image tag in `.env` (or pull `latest`) — or rebuild from a new
   git tag.
2. `docker compose up -d` — Flyway runs new migrations automatically on
   start; Hibernate validates the resulting schema.
3. If validation fails the container exits — read the logs, do not
   force-start with `DB_DDL_AUTO=update`.

## Troubleshooting

**Container exits immediately with `Schema-validation: missing column ...`** —
the running image expects a column that the database doesn't have. Either
the wrong image tag is being used, or a migration was skipped. Check
`docker compose logs oscal-hub` for the exact column, then verify the
image and the Flyway migration history (`SELECT * FROM
flyway_schema_history;`).

**`401 Unauthorized` on every request after restart** — JWTs are signed
with `JWT_SECRET`. Restarting with a different secret invalidates every
issued token. Have users sign in again.

**Backend healthy but frontend 502** — Nginx proxies the frontend on port
3000. Check `docker compose logs nginx` and confirm the `oscal-hub`
container is healthy (`docker compose ps`).

**Where are the files / data?** — Docker named volumes:
`oscal-hub_postgres_data`, `oscal-hub_oscal_data`, `oscal-hub_oscal_logs`,
`oscal-hub_oscal_files`. List with `docker volume ls | grep oscal-hub`.
