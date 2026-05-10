# Self-Hosted Deployment Guide

OSCAL Hub ships two on-prem / customer-cloud deployment options:

| Option | When | Where |
|---|---|---|
| Docker Compose | Single Linux VM, no Kubernetes | [`deploy/compose/`](../deploy/compose/) |
| Helm chart | Any Kubernetes cluster | [`deploy/helm/oscal-hub/`](../deploy/helm/oscal-hub/) |

For our managed GCP deployment (Cloud Run + Cloud SQL) see
[`GCP-DEPLOYMENT-GUIDE.md`](GCP-DEPLOYMENT-GUIDE.md). Customer self-hosting
should pick Compose or Helm.

## Choosing between them

- **Got a Linux VM and Docker?** Compose. Done in 5 minutes.
- **Got Kubernetes?** Helm. Plays nicely with your ingress controller,
  cert-manager, secrets management, monitoring.
- **Air-gapped?** Both work. Mirror the OSCAL Hub image (and, for Helm, the
  Bitnami PostgreSQL subchart) into your internal registry first.
- **Multi-replica / HA?** Helm with shared object storage (S3 / GCS / Azure
  Blob) and an external managed Postgres.

## Common requirements

- A container image — built from the repo root `Dockerfile`, or pulled
  from a registry you control.
- TLS termination — Compose ships Nginx for this; Helm assumes you have
  ingress + cert-manager (or pre-issued certs).
- Two secrets — a database password and a JWT signing secret. Generate
  once, store securely. Rotating `JWT_SECRET` invalidates all issued
  tokens.

## Storage providers

Both options default to local-filesystem storage on a Docker volume / PVC.
For multi-replica Kubernetes deployments, switch to shared object storage:

| `STORAGE_PROVIDER` | Backend |
|---|---|
| `azure` (no connection string) | Local filesystem (default) |
| `azure` | Azure Blob Storage |
| `s3`    | AWS S3 / S3-compatible (MinIO) |
| `gcs`   | Google Cloud Storage |

## Database

Compose runs Postgres as a sibling container. Helm runs it as a Bitnami
subchart by default. For production, prefer an external managed Postgres
(Cloud SQL, RDS, Crunchy, etc.) — set `postgresql.enabled=false` in Helm
and supply `externalDatabase.*`. Compose users can comment out the
`postgres` service and point `DB_URL` at their own host.

## Next steps

- [Docker Compose quickstart →](../deploy/compose/README.md)
- [Helm quickstart →](../deploy/helm/oscal-hub/README.md)
- [Production go-live checklist →](DEPLOYMENT-CHECKLIST.md)
