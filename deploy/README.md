# OSCAL Hub - Self-hosted Deployments

This directory holds the two supported on-prem / customer-cloud deployment
options. Pick one:

| Option | When to use | Path |
|---|---|---|
| **Docker Compose** | Single VM, Docker host, fastest path to running. Pairs the app with bundled Postgres + Nginx (TLS termination). | [`compose/`](compose/) |
| **Helm chart** | Kubernetes — bare-metal, on-prem, or any cloud Kubernetes service. Bundles Postgres as a subchart by default. | [`helm/oscal-hub/`](helm/oscal-hub/) |

For our managed GCP deployment (Cloud Run + Cloud SQL + Workload Identity)
see [`../terraform/gcp/`](../terraform/gcp/) — that path is for the
maintainer's hosted environment, not customer self-hosting.

## Choosing between Compose and Helm

- **Got Kubernetes already? Use Helm.** Plays nicely with your existing
  ingress controller, cert-manager, secrets management, monitoring.
- **One Linux box, no Kubernetes? Use Compose.** No cluster to operate, just
  Docker. The bundled Nginx terminates TLS.
- **Cloud-native shop?** Helm + an external managed Postgres
  (`postgresql.enabled=false`) + shared object storage
  (`config.storageProvider=s3|gcs|azure`) is the most operationally clean
  shape.
- **Air-gapped?** Both work. Mirror the OSCAL Hub image (and, for Helm, the
  Bitnami PostgreSQL subchart) into your internal registry and set
  `image.repository` accordingly.

## What you need either way

- The OSCAL Hub container image — either built from this repo's root
  `Dockerfile`, or pulled from a registry you control.
- A way to terminate TLS in front of the app. Compose ships an Nginx that
  handles this; Helm assumes you have an ingress controller +
  cert-manager (or pre-issued cert).
- Persistent storage for Postgres and (with the default local-filesystem
  storage mode) for uploaded files.
- Two secrets: a database password and a JWT signing secret. Both must
  survive restarts — generate them once and store securely.

Specifics — quickstart commands, configuration knobs, backup, upgrade,
troubleshooting — live in each option's own README.
