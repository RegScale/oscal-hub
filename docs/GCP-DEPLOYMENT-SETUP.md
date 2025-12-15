# GCP Deployment Setup Guide

**Date:** December 14, 2025
**Status:** Active
**Last Updated:** December 14, 2025

## Overview

This guide explains how to set up automated deployment to Google Cloud Platform (GCP) using GitHub Actions. The workflow automatically deploys the OSCAL Tools application to Cloud Run whenever a PR is merged to the `main` branch.

> **Quick Start**: If you already have your GCP service account set up and just need to configure GitHub secrets, see the **[GitHub Secrets Setup Guide](GITHUB-SECRETS-SETUP.md)** for a streamlined walkthrough.

## Architecture

The deployment consists of:

- **Backend**: Spring Boot API running on Cloud Run
- **Frontend**: Next.js application running on Cloud Run
- **Container Registry**: Google Artifact Registry for Docker images
- **CI/CD**: GitHub Actions workflow triggered on PR merge to main

## Prerequisites

1. **GCP Project**: You need an active GCP project with billing enabled
2. **GitHub Repository**: Admin access to configure secrets
3. **gcloud CLI**: Installed locally for initial setup (optional but recommended)

## Initial GCP Setup

### Step 1: Create a GCP Service Account

The GitHub Actions workflow needs a service account with appropriate permissions to deploy to GCP.

```bash
# Set your project ID
export GCP_PROJECT_ID="your-project-id"

# Authenticate with GCP
gcloud auth login

# Set the active project
gcloud config set project $GCP_PROJECT_ID

# Create a service account for GitHub Actions
gcloud iam service-accounts create github-actions \
  --display-name="GitHub Actions Deployment" \
  --description="Service account for GitHub Actions to deploy to Cloud Run"

# Grant necessary roles to the service account
gcloud projects add-iam-policy-binding $GCP_PROJECT_ID \
  --member="serviceAccount:github-actions@${GCP_PROJECT_ID}.iam.gserviceaccount.com" \
  --role="roles/run.admin"

gcloud projects add-iam-policy-binding $GCP_PROJECT_ID \
  --member="serviceAccount:github-actions@${GCP_PROJECT_ID}.iam.gserviceaccount.com" \
  --role="roles/iam.serviceAccountUser"

gcloud projects add-iam-policy-binding $GCP_PROJECT_ID \
  --member="serviceAccount:github-actions@${GCP_PROJECT_ID}.iam.gserviceaccount.com" \
  --role="roles/artifactregistry.admin"

gcloud projects add-iam-policy-binding $GCP_PROJECT_ID \
  --member="serviceAccount:github-actions@${GCP_PROJECT_ID}.iam.gserviceaccount.com" \
  --role="roles/storage.admin"

# Create and download a key for the service account
gcloud iam service-accounts keys create github-actions-key.json \
  --iam-account=github-actions@${GCP_PROJECT_ID}.iam.gserviceaccount.com

echo "✅ Service account created and key saved to github-actions-key.json"
echo "⚠️  Keep this file secure - you'll need it for GitHub secrets"
```

### Step 2: Enable Required GCP APIs

```bash
# Enable Cloud Run API
gcloud services enable run.googleapis.com

# Enable Artifact Registry API
gcloud services enable artifactregistry.googleapis.com

# Enable Cloud Build API (optional, for advanced builds)
gcloud services enable cloudbuild.googleapis.com

# Enable Container Registry API (if migrating from GCR)
gcloud services enable containerregistry.googleapis.com

echo "✅ Required APIs enabled"
```

### Step 3: Create Artifact Registry Repository (Optional)

The GitHub Actions workflow will create this automatically if it doesn't exist, but you can create it manually:

```bash
# Set your preferred region
export GCP_REGION="us-central1"

# Create Artifact Registry repository
gcloud artifacts repositories create oscal-tools \
  --repository-format=docker \
  --location=$GCP_REGION \
  --description="OSCAL Tools container images"

echo "✅ Artifact Registry repository created"
```

## GitHub Repository Configuration

### Step 1: Add GitHub Secrets

Navigate to your GitHub repository:
1. Go to **Settings** → **Secrets and variables** → **Actions**
2. Click **New repository secret**
3. Add the following secrets:

#### Required Secrets

| Secret Name | Description | How to Get |
|------------|-------------|------------|
| `GCP_SA_KEY` | Service account key JSON | Content of `github-actions-key.json` file created above |
| `GCP_PROJECT_ID` | Your GCP project ID | From `gcloud config get-value project` |

**To add `GCP_SA_KEY`:**

```bash
# Display the service account key (copy this entire output)
cat github-actions-key.json

# Alternatively, copy to clipboard (macOS)
cat github-actions-key.json | pbcopy

# Or on Linux
cat github-actions-key.json | xclip -selection clipboard
```

Paste the **entire JSON content** into the `GCP_SA_KEY` secret field.

### Step 2: Add GitHub Variables (Optional)

You can also set these as **repository variables** instead of secrets for easier management:

1. Go to **Settings** → **Secrets and variables** → **Actions** → **Variables** tab
2. Click **New repository variable**
3. Add the following variables:

| Variable Name | Description | Default | Recommended Value |
|--------------|-------------|---------|-------------------|
| `GCP_PROJECT_ID` | Your GCP project ID | (none) | Your project ID |
| `GCP_REGION` | GCP region for deployment | `us-central1` | `us-central1` or your preferred region |

**Note:** If you set `GCP_PROJECT_ID` as both a secret and a variable, the variable will take precedence.

## Workflow Configuration

The GitHub Actions workflow is located at:
```
.github/workflows/gcp-deploy.yml
```

### Workflow Triggers

The workflow runs automatically on:

1. **Push to main branch** (i.e., when a PR is merged)
2. **Manual trigger** via `workflow_dispatch` (allows choosing environment)

### Workflow Jobs

The workflow consists of 5 jobs:

1. **build-and-test**: Builds and tests backend and frontend
2. **build-and-push**: Builds Docker images and pushes to Artifact Registry
3. **deploy-to-gcp**: Deploys containers to Cloud Run
4. **health-checks**: Verifies deployment health
5. **cleanup-old-images**: Removes old container images (keeps last 5)

### Environment Support

The workflow supports multiple environments:

- **prod** (default for main branch)
- **staging**
- **dev**

Cloud Run services are named: `oscal-backend-{environment}` and `oscal-frontend-{environment}`

## Deployment Process

### Automatic Deployment (PR Merge)

1. Create a pull request with your changes
2. Wait for CI checks to pass
3. Merge the PR to `main` branch
4. GitHub Actions automatically:
   - Builds and tests the code
   - Creates Docker images
   - Pushes images to Artifact Registry
   - Deploys to Cloud Run (prod environment)
   - Runs health checks
   - Cleans up old images

### Manual Deployment

You can also trigger deployment manually:

1. Go to **Actions** tab in GitHub
2. Select **Deploy to Google Cloud Run** workflow
3. Click **Run workflow**
4. Choose the environment (dev, staging, or prod)
5. Click **Run workflow**

## Verifying Deployment

### Check Deployment Status

1. Go to the **Actions** tab in your GitHub repository
2. Click on the latest workflow run
3. View the step-by-step progress

### View Deployed Services

After successful deployment, the workflow summary will show:

- **Frontend URL**: `https://oscal-frontend-prod-{region}.a.run.app`
- **Backend URL**: `https://oscal-backend-prod-{region}.a.run.app`

### Access Cloud Run Console

```bash
# Open Cloud Run console in browser
gcloud run services list --platform managed

# Or visit directly:
# https://console.cloud.google.com/run?project=YOUR_PROJECT_ID
```

### View Logs

```bash
# View backend logs
gcloud logging read "resource.type=cloud_run_revision AND resource.labels.service_name=oscal-backend-prod" \
  --limit 50 \
  --format json

# View frontend logs
gcloud logging read "resource.type=cloud_run_revision AND resource.labels.service_name=oscal-frontend-prod" \
  --limit 50 \
  --format json
```

## Troubleshooting

### Issue: Workflow fails with "permission denied" error

**Solution:**
- Verify that the service account has all required roles (see Step 1)
- Check that `GCP_SA_KEY` secret is correctly set with the full JSON content

### Issue: Artifact Registry repository not found

**Solution:**
The workflow creates the repository automatically. If it fails:

```bash
gcloud artifacts repositories create oscal-tools \
  --repository-format=docker \
  --location=$GCP_REGION \
  --description="OSCAL Tools container images"
```

### Issue: Cloud Run deployment fails

**Solution:**
1. Check the workflow logs for specific error messages
2. Verify that Cloud Run API is enabled:
   ```bash
   gcloud services enable run.googleapis.com
   ```
3. Check service account permissions

### Issue: Health checks fail

**Solution:**
1. Check Cloud Run logs for application errors:
   ```bash
   gcloud logging read "resource.type=cloud_run_revision" --limit 20
   ```
2. Verify environment variables are correctly set
3. Check that the application is binding to the correct port (8080 for backend, 3000 for frontend)

### Issue: Frontend can't connect to backend

**Solution:**
1. Verify that `NEXT_PUBLIC_API_URL` is correctly set in the frontend deployment
2. Check CORS configuration in the backend
3. Ensure both services are in the same region

## Cost Optimization

### Cloud Run Pricing

Cloud Run charges based on:
- CPU and memory allocation
- Number of requests
- Execution time

**Default configuration** (per service):
- **Backend**: 1 CPU, 2Gi memory, 0-10 instances
- **Frontend**: 1 CPU, 512Mi memory, 0-10 instances
- **Min instances**: 0 (scales to zero when idle)

### Reduce Costs

To reduce costs for development:

1. **Use minimum instances = 0** (already configured)
2. **Set lower max instances** for dev/staging:
   ```yaml
   --max-instances 3  # Instead of 10
   ```
3. **Use smaller CPU/memory** for dev:
   ```yaml
   --cpu 0.5
   --memory 1Gi
   ```

### Free Tier

Cloud Run includes a generous free tier:
- 2 million requests per month
- 360,000 GB-seconds of memory
- 180,000 vCPU-seconds

Most development workloads stay within the free tier.

## Security Best Practices

1. **Never commit service account keys** to Git
2. **Use Secret Manager** for sensitive configuration (JWT secrets, database passwords)
3. **Enable authentication** for production deployments if needed:
   ```bash
   gcloud run services update oscal-backend-prod \
     --region $GCP_REGION \
     --no-allow-unauthenticated
   ```
4. **Rotate service account keys** regularly
5. **Use least-privilege IAM roles**

## Advanced Configuration

### Custom Domain

To use a custom domain:

```bash
# Map a custom domain to your service
gcloud run services update oscal-frontend-prod \
  --region $GCP_REGION \
  --add-custom-domain your-domain.com
```

### Environment Variables

To add environment variables (e.g., database connection):

Edit the workflow file `.github/workflows/gcp-deploy.yml`:

```yaml
gcloud run deploy oscal-backend-prod \
  --set-env-vars "DB_HOST=your-db-host,DB_NAME=your-db-name" \
  --set-secrets "DB_PASSWORD=db-password:latest"
```

### Database Setup

For production, use Cloud SQL:

```bash
# Create Cloud SQL instance
gcloud sql instances create oscal-db \
  --database-version=POSTGRES_15 \
  --tier=db-f1-micro \
  --region=$GCP_REGION

# Connect Cloud Run to Cloud SQL
gcloud run services update oscal-backend-prod \
  --region $GCP_REGION \
  --add-cloudsql-instances $GCP_PROJECT_ID:$GCP_REGION:oscal-db
```

## Migration from Azure

If you're migrating from Azure:

1. **Keep Azure workflow temporarily** for rollback safety
2. **Test GCP deployment** in dev environment first
3. **Update DNS** to point to GCP Cloud Run URLs
4. **Disable Azure workflow** after successful migration:
   - Rename `.github/workflows/azure-deploy.yml` to `.github/workflows/azure-deploy.yml.disabled`

## Support and Resources

- **GCP Documentation**: https://cloud.google.com/run/docs
- **GitHub Actions Documentation**: https://docs.github.com/en/actions
- **OSCAL Tools Issues**: https://github.com/your-org/oscal-cli/issues

## Summary Checklist

- [ ] GCP project created with billing enabled
- [ ] Service account created with required permissions
- [ ] Service account key downloaded
- [ ] Required GCP APIs enabled
- [ ] GitHub secrets configured (`GCP_SA_KEY`, `GCP_PROJECT_ID`)
- [ ] Workflow file in place (`.github/workflows/gcp-deploy.yml`)
- [ ] Test deployment triggered manually
- [ ] Health checks pass
- [ ] Frontend and backend URLs accessible
- [ ] Azure workflow disabled (if migrating)

---

**Next Steps:**
1. Test the deployment by merging a small PR to main
2. Verify health checks pass
3. Access the deployed application
4. Monitor logs for any errors
5. Set up custom domain (optional)
