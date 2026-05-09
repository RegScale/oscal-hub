# OSCAL Hub Helm Chart

Helm chart for deploying [OSCAL Hub](https://github.com/oscal-tools/oscal-cli)
on Kubernetes — for customers running on-prem or in their own cloud account.

For single-VM deployments, the [Docker Compose option](../../compose/) is
simpler. For our managed GCP deployment, see
[`terraform/gcp/`](../../../terraform/gcp/).

## Prerequisites

- Kubernetes 1.25+
- Helm 3.12+
- An ingress controller (nginx-ingress, Traefik, etc.) reachable on a public
  hostname
- (Optional) cert-manager for automatic TLS, or a pre-issued TLS secret
- A container registry your cluster can pull from, holding the OSCAL Hub
  image

## Install

```bash
# 1. Update dependencies (downloads the bundled Bitnami PostgreSQL chart)
helm dependency update deploy/helm/oscal-hub

# 2. Install
helm upgrade --install oscal-hub deploy/helm/oscal-hub \
  --namespace oscal --create-namespace \
  --set image.repository=your-registry.example.com/oscal-hub \
  --set image.tag=0.1.0 \
  --set ingress.host=oscal.example.com \
  --set ingress.className=nginx \
  --set secrets.jwtSecret="$(openssl rand -base64 64 | tr -d '\n')" \
  --set secrets.dbPassword="$DB_PW" \
  --set postgresql.auth.password="$DB_PW"
```

(`DB_PW` set once and passed twice — see "Database password" below.)

For anything beyond a single-line install, drop the configuration into
a values file:

```bash
helm upgrade --install oscal-hub deploy/helm/oscal-hub \
  --namespace oscal --create-namespace \
  -f my-values.yaml
```

A starter `my-values.yaml`:

```yaml
image:
  repository: ghcr.io/your-org/oscal-hub
  tag: "0.1.0"

ingress:
  host: oscal.example.com
  className: nginx
  annotations:
    cert-manager.io/cluster-issuer: letsencrypt-prod
    nginx.ingress.kubernetes.io/proxy-body-size: 10m

secrets:
  # In production, prefer secrets.existingSecret to avoid putting these
  # in plain values. See "Managing secrets" below.
  jwtSecret: REPLACE_ME      # openssl rand -base64 64
  dbPassword: REPLACE_ME_DB  # openssl rand -base64 32 — must match postgresql.auth.password

postgresql:
  enabled: true              # bundled Postgres; set false to use external DB
  auth:
    password: REPLACE_ME_DB  # SAME value as secrets.dbPassword (see "Database password")
  primary:
    persistence:
      size: 50Gi
```

### Database password

The bundled PostgreSQL subchart can't read the parent chart's
`secrets.dbPassword` directly (Helm subchart values are isolated). Choose
one of:

- **Set the password in both places.** `secrets.dbPassword` configures the
  app; `postgresql.auth.password` configures Postgres. Same value, two
  places.
- **Use one external Secret.** Create a Secret with keys `jwt-secret` and
  `db-password`, then set:

  ```yaml
  secrets:
    existingSecret: oscal-hub-secrets
  postgresql:
    auth:
      existingSecret: oscal-hub-secrets
      secretKeys:
        userPasswordKey: db-password
  ```

- **Use a managed Postgres.** Set `postgresql.enabled=false` and configure
  `externalDatabase.*` — only `secrets.dbPassword` is needed.

## Configuration reference

The full set of values is in [`values.yaml`](values.yaml). The most common
overrides:

| Value | Default | Description |
|-------|---------|-------------|
| `image.repository`, `image.tag` | `oscal-hub`, chart `appVersion` | Container image |
| `ingress.host` | `oscal.example.com` | Public hostname (required) |
| `ingress.className` | `""` | Ingress controller class |
| `ingress.tls.enabled` | `true` | Terminate TLS at the ingress |
| `secrets.jwtSecret` | _(required)_ | JWT signing secret (or use `existingSecret`) |
| `secrets.dbPassword` | _(required when `postgresql.enabled=true`)_ | Postgres password |
| `replicaCount` | `1` | Pods. Keep at 1 unless `config.storageProvider` is shared object storage |
| `resources` | requests 0.5/1Gi, limits 2/4Gi | Per-pod resource shape |
| `persistence.size` | `20Gi` | PVC size for local-filesystem file storage |
| `config.storageProvider` | `azure` | `azure` (no connection string → local PVC), `s3`, or `gcs` |
| `config.publicOrigin` | _(derived)_ | CORS allowed origin; defaults to `https://<ingress.host>` |
| `postgresql.enabled` | `true` | Use bundled Bitnami Postgres |
| `externalDatabase.host` | `""` | Set when `postgresql.enabled=false` |
| `autoscaling.enabled` | `false` | Enable HPA — only safe with shared object storage |
| `networkPolicy.enabled` | `false` | Restrict ingress/egress |

## Managing secrets

Three options, in increasing order of "you should do this in real
deployments":

1. **Inline values** — quick start; `secrets.jwtSecret` etc. land in the
   chart-rendered Secret. Don't commit your `values.yaml`.
2. **Pre-created secret** — create the secret out of band (sops, sealed-secrets,
   external-secrets, vault) and set `secrets.existingSecret: my-secret`. The
   secret must contain keys: `jwt-secret`, `db-password`, and optionally
   `azure-storage-connection-string` / `aws-access-key-id` /
   `aws-secret-access-key`.
3. **External Secrets Operator / Vault** — point `secrets.existingSecret` at
   the synthesized secret produced by your ESO / Vault integration.

## External database

Skip the bundled Postgres and use a managed instance (Cloud SQL, RDS, etc.):

```yaml
postgresql:
  enabled: false

externalDatabase:
  host: pg.internal
  port: 5432
  database: oscal
  username: oscal
  # password lives in secrets.dbPassword or your existingSecret

secrets:
  existingSecret: oscal-hub-secrets  # must contain db-password and jwt-secret
```

## Object storage

The default (`storageProvider: azure` with no connection string) falls back
to local-filesystem storage on the PVC mounted at
`/home/oscaluser/.oscal-hub/files`. That works fine for one replica.

For multiple replicas, point at shared object storage:

```yaml
replicaCount: 3
persistence:
  enabled: false                 # no PVC needed when storage is external

config:
  storageProvider: s3            # or gcs / azure

objectStorage:
  s3:
    region: us-east-1
    bucket: my-org-oscal-files

secrets:
  awsAccessKeyId: AKIA...
  awsSecretAccessKey: ...
```

For S3-compatible stores (MinIO, Wasabi, Cloudflare R2), use
`storageProvider: s3` and set additional Spring properties via `extraEnv`
(currently this requires post-processing — open an issue if you need it
first-class).

## Operations

```bash
# Watch rollout
kubectl -n oscal rollout status deploy/oscal-hub

# Tail backend logs
kubectl -n oscal logs -f deploy/oscal-hub

# Run the chart's smoke test
helm test oscal-hub -n oscal

# Upgrade
helm upgrade oscal-hub deploy/helm/oscal-hub -n oscal -f my-values.yaml

# Rollback
helm rollback oscal-hub <revision> -n oscal

# Uninstall (DESTRUCTIVE: also deletes the bundled Postgres PVC unless you
# scale or retain it manually first)
helm uninstall oscal-hub -n oscal
```

## Backups

The bundled Bitnami Postgres ships with no backup automation. For
production, either:

- **Use an external Postgres** (`postgresql.enabled=false`) with managed
  backups (Cloud SQL automated backups, RDS snapshots, etc.).
- **Schedule logical backups** with a CronJob:

  ```yaml
  apiVersion: batch/v1
  kind: CronJob
  metadata: { name: oscal-pg-backup, namespace: oscal }
  spec:
    schedule: "0 3 * * *"
    jobTemplate:
      spec:
        template:
          spec:
            restartPolicy: OnFailure
            containers:
              - name: dump
                image: postgres:16-alpine
                command: [/bin/sh, -c]
                args:
                  - pg_dump -h oscal-hub-postgresql -U oscal -Fc -d oscal
                    > /backup/oscal-$(date +%F).dump
                env:
                  - name: PGPASSWORD
                    valueFrom:
                      secretKeyRef:
                        name: oscal-hub-secrets
                        key: db-password
                volumeMounts:
                  - { name: backup, mountPath: /backup }
            volumes:
              - name: backup
                persistentVolumeClaim:
                  claimName: oscal-pg-backup
  ```

When using local-filesystem file storage, also back up the
`oscal-hub-files` PVC. With external object storage, the bucket is the
system of record.

## Troubleshooting

**Pod CrashLoopBackOff with `Schema-validation: missing column ...`** — the
running image expects a column the database doesn't have. Either the wrong
image tag is in use, or a Flyway migration was skipped. Check
`flyway_schema_history` in Postgres.

**Pod stuck in startup probe failures** — first boot can take several
minutes for migrations on a populated database. The default startup probe
allows 5 minutes. Bump `probes.startup.failureThreshold` if your DB is
larger.

**Ingress 502** — the deployment is up but ingress can't reach it. Check
that the ingress controller resolves the service name and that
`service.ports.frontend` / `service.ports.backend` match the deployment's
container ports.

**`ingress.host is required` error during `helm install`** — set
`ingress.host` (or disable ingress with `ingress.enabled=false` and reach
the service via port-forward / cluster-internal DNS).
