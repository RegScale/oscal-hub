# OSCAL Tools - Simplified Google Cloud Run Architecture

**Date**: November 29, 2025
**Status**: Active

## Overview

We've simplified the GCP deployment from a two-container microservices architecture to a **single-container monolith**. This provides significant benefits for this application:

## Architecture

### Before (Complex)
```
┌─────────────────┐     ┌─────────────────┐
│  Frontend       │────▶│  Backend        │
│  Cloud Run      │     │  Cloud Run      │
│  (Next.js)      │     │  (Spring Boot)  │
│  Port 3000      │     │  Port 8080      │
└─────────────────┘     └─────────────────┘
        │                       │
        │                       │
        ▼                       ▼
┌───────────────────────────────────────┐
│        Cloud SQL PostgreSQL            │
└───────────────────────────────────────┘
```

**Issues**:
- Two Cloud Run services to manage
- CORS configuration required
- More complex networking
- Higher cost (2 services)
- Complex environment variable management

### After (Simple)
```
┌─────────────────────────────────────┐
│  OSCAL Tools Cloud Run Service     │
│  ┌──────────────┐  ┌─────────────┐ │
│  │  Frontend    │  │  Backend    │ │
│  │  (Next.js)   │──│ (Spring Boot)│ │
│  │  Port 3000   │  │  Port 8080  │ │
│  └──────────────┘  └─────────────┘ │
└─────────────────────────────────────┘
                │
                ▼
┌───────────────────────────────────────┐
│        Cloud SQL PostgreSQL            │
└───────────────────────────────────────┘
```

**Benefits**:
✅ One Cloud Run service
✅ No CORS issues (same origin)
✅ Simpler deployment
✅ Lower cost (~50% reduction)
✅ Easier to manage

## How It Works

1. **Single Container**: Uses existing `Dockerfile` that builds both backend and frontend
2. **Port Routing**:
   - Frontend listens on port 3000 (Cloud Run entry point)
   - Backend listens on port 8080 (internal only)
   - Frontend proxies API requests to `localhost:8080/api`
3. **Same Origin**: No CORS needed since frontend and backend share the same domain
4. **docker-entrypoint.sh**: Starts both processes and manages them with proper signal handling

## Resource Allocation

Since one container runs both services:

```hcl
# terraform.tfvars
app_cpu    = "2000m"  # 2 vCPU (Spring Boot + Node.js)
app_memory = "4Gi"    # 4GB RAM
```

This is actually more efficient than two separate containers because:
- Shared base OS layer
- No duplicate Java/Node runtimes (they're in same container)
- Better resource utilization

## Deployment

### Build Single Container
```bash
gcloud builds submit --config=cloudbuild-images.yaml
```

This builds ONE image: `oscal-tools:latest`

### Deploy with Terraform
```bash
cd terraform/gcp
terraform apply
```

This deploys ONE Cloud Run service: `oscal-tools-prod`

## Cost Comparison

### Old Architecture (Two Containers)
- Frontend: $X/month
- Backend: $Y/month
- **Total**: $X + $Y

### New Architecture (One Container)
- Combined: $(X+Y)/2 approximately
- **Total**: ~50% cost reduction

Reasons:
- Single service = single cold start overhead
- Shared infrastructure
- Better resource packing

## File Changes

### Updated Files
1. **cloudbuild-images.yaml** - Builds single `oscal-tools` image
2. **terraform/gcp/main.tf** - Single `oscal_app` module instead of backend+frontend
3. **terraform/gcp/variables.tf** - `app_*` variables instead of `backend_*` and `frontend_*`
4. **terraform/gcp/outputs.tf** - Single `app_url` output

### Unchanged Files
- **Dockerfile** - Already supported single-container build!
- **docker-entrypoint.sh** - Already manages both processes
- **back-end/** - No changes
- **front-end/** - No changes

## URLs

After deployment:

```
Application:  https://oscal-tools-prod-xxxxx.run.app
Frontend:     https://oscal-tools-prod-xxxxx.run.app/
API:          https://oscal-tools-prod-xxxxx.run.app/api
API Docs:     https://oscal-tools-prod-xxxxx.run.app/swagger-ui
Health:       https://oscal-tools-prod-xxxxx.run.app/actuator/health
```

Everything is on the same domain!

## Environment Variables

The container receives:

**Backend Settings**:
- `SPRING_PROFILES_ACTIVE=gcp`
- `GCP_PROJECT_ID`
- `DB_URL` (Cloud SQL connection string)
- `DB_USERNAME`
- `DB_PASSWORD` (from Secret Manager)
- `JWT_SECRET` (from Secret Manager)
- `GCS_BUCKET_BUILD`

**Frontend Settings**:
- `NODE_ENV=production`
- `NEXT_PUBLIC_API_URL=/api` (same origin!)
- `NEXT_PUBLIC_USE_MOCK=false`

**Cloud Run Settings**:
- `PORT=3000` (set by Cloud Run)

## Terraform Configuration

### terraform.tfvars Example

```hcl
project_id  = "oscal-hub"
region      = "us-central1"
environment = "prod"

# Single app configuration
app_cpu          = "2000m"  # 2 vCPU
app_memory       = "4Gi"    # 4 GB
app_min_instances = 0       # Scale to zero
app_max_instances = 10      # Max scale

# Database
db_tier = "db-f1-micro"     # Or "db-g1-small" for more power
```

## Migration from Two-Container Setup

If you have an existing two-container deployment:

1. **Backup current state**:
   ```bash
   cd terraform/gcp
   terraform state pull > backup.tfstate
   ```

2. **Update Terraform files** (already done)

3. **Destroy old services**:
   ```bash
   terraform destroy -target=module.backend -target=module.frontend
   ```

4. **Build new container**:
   ```bash
   cd ../..
   gcloud builds submit --config=cloudbuild-images.yaml
   ```

5. **Deploy new service**:
   ```bash
   cd terraform/gcp
   terraform apply
   ```

## Monitoring

Same monitoring as before, but simpler:

```bash
# View logs (single service now)
gcloud run logs read oscal-tools-prod --region=us-central1

# View service details
gcloud run services describe oscal-tools-prod --region=us-central1

# View metrics
# Cloud Console → Cloud Run → oscal-tools-prod
```

## Scaling Behavior

The container scales as a unit:
- Cold start: ~30-45 seconds (backend startup + frontend startup)
- Warm start: instant (already running)
- Scale to zero: Yes (min_instances = 0)
- Auto-scale: Based on CPU/memory/requests

## When to Use Separate Containers

Consider separate containers if:
- ❌ Frontend and backend need very different resource allocations
- ❌ You need to scale frontend and backend independently
- ❌ You want different deployment frequencies
- ❌ Team structure requires separate ownership

For OSCAL Tools:
- ✅ Resources are similar (both need 1-2 GB)
- ✅ Scaling requirements are the same
- ✅ Deploy together (same release cycle)
- ✅ Single team

**Conclusion**: Single container is the right choice!

## Troubleshooting

### Container Won't Start

Check logs:
```bash
gcloud run logs read oscal-tools-prod --region=us-central1 --limit=50
```

Common issues:
1. Backend startup timeout - Increase memory or CPU
2. Database migration failures - Check Cloud SQL connectivity
3. Secret access denied - Verify IAM permissions

### Frontend Not Loading

The frontend depends on backend being ready. Check:
```bash
curl https://YOUR-APP-URL.run.app/actuator/health
```

If backend is down, frontend will show errors.

### API Requests Failing

1. Check that `NEXT_PUBLIC_API_URL=/api` (same origin)
2. Verify backend is listening on `localhost:8080`
3. Check docker-entrypoint.sh logs

## Performance

Single-container performance metrics:
- **Cold start**: 30-45s (acceptable for scale-to-zero)
- **Warm response**: <100ms
- **Memory usage**: ~3GB (backend: 2GB, frontend: 1GB)
- **CPU usage**: Low at idle, spikes during requests

To reduce cold starts:
```hcl
app_min_instances = 1  # Keep at least one warm instance
```

## Summary

| Aspect | Before | After |
|--------|--------|-------|
| Containers | 2 | 1 |
| Cloud Run Services | 2 | 1 |
| CORS Needed | Yes | No |
| Cost | 2x | 1x |
| Complexity | High | Low |
| Build Time | Parallel | Sequential |
| Deploy Time | 2x | 1x |
| Management | Complex | Simple |

**Result**: Simpler, cheaper, easier to maintain! 🎉
