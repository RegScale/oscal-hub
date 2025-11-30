# Google Cloud Storage Setup Guide

**Date:** 2025-11-29
**Status:** Complete

## Overview

This guide explains how to configure OSCAL Tools to use Google Cloud Storage (GCS) for object storage in both development (localhost) and production (Cloud Run) environments.

## Architecture

The application uses **GcsStorageService** (back-end/src/main/java/gov/nist/oscal/tools/api/service/GcsStorageService.java:32) which provides:

- ✅ Full GCS integration with Google Cloud Storage SDK
- ✅ Automatic bucket creation if missing
- ✅ Application Default Credentials (ADC) authentication
- ✅ Automatic fallback to local filesystem if GCS unavailable
- ✅ Support for versioning and lifecycle policies

## Authentication

The application uses **Application Default Credentials (ADC)** which automatically selects credentials based on environment:

| Environment | Authentication Method |
|-------------|----------------------|
| **Local Development** | `gcloud auth application-default login` |
| **Cloud Run** | Service account (automatically configured) |
| **Compute Engine** | Service account (automatically configured) |
| **Manual** | `GOOGLE_APPLICATION_CREDENTIALS` environment variable |

## Development Environment Setup

### 1. Prerequisites

- Google Cloud SDK (`gcloud`) installed
- Active GCP project (current: `oscal-hub`)
- Appropriate IAM permissions for GCS

### 2. Authenticate with Application Default Credentials

```bash
# Login with your Google account
gcloud auth application-default login
```

This creates credentials at: `~/.config/gcloud/application_default_credentials.json`

### 3. GCS Buckets

**DEV Bucket (already created):**
- **Name:** `oscal-tools-build-dev-us-central1`
- **Location:** `us-central1`
- **Purpose:** Store OSCAL component definitions during local development

The bucket was created with:
```bash
gcloud storage buckets create gs://oscal-tools-build-dev-us-central1 \
  --project=oscal-hub \
  --location=us-central1 \
  --uniform-bucket-level-access \
  --public-access-prevention
```

### 4. Configuration

The application is configured in `back-end/src/main/resources/application-dev.properties`:

```properties
# Storage provider (gcs is now the default)
storage.provider=gcs

# GCP Configuration
gcp.project-id=oscal-hub
gcp.storage.bucket-build=oscal-tools-build-dev-us-central1
gcp.storage.build-folder=build
```

### 5. Running Locally

```bash
# Start the application (from project root)
./dev.sh

# Or manually
cd back-end
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

The application will:
1. Use Application Default Credentials from `~/.config/gcloud/`
2. Connect to GCS bucket `oscal-tools-build-dev-us-central1`
3. Log connection status on startup

### 6. Verify GCS Connection

Check the logs on startup for:
```
Storage Provider: Google Cloud Storage (storage.provider=gcs)
Initializing GCS client for project: oscal-hub
GCS bucket 'oscal-tools-build-dev-us-central1' is accessible
Google Cloud Storage initialized successfully
```

### 7. Optional: Override Settings

You can override settings via environment variables:

```bash
# Use a different bucket
export GCS_BUCKET_BUILD=my-custom-bucket

# Use a different project
export GCP_PROJECT_ID=my-project-id

# Run the application
./dev.sh
```

## Production Environment Setup (Terraform)

### 1. Terraform Configuration

The Terraform configuration is located in `terraform/gcp/` and includes:

**Bucket Module:** `terraform/gcp/modules/cloud-storage/main.tf`
- Creates build bucket: `oscal-tools-build-prod-us-central1`
- Creates library bucket: `oscal-tools-library-prod-us-central1`
- Enables versioning for data protection
- Configures lifecycle rules to manage costs
- Enables uniform bucket-level access

**Main Configuration:** `terraform/gcp/main.tf`
- Deploys Cloud Run service with GCS access
- Sets environment variable: `GCS_BUCKET_BUILD`
- Configures service account with storage permissions

### 2. Deploy Production Infrastructure

```bash
cd terraform/gcp

# Initialize Terraform
terraform init

# Review planned changes
terraform plan

# Apply configuration
terraform apply
```

This will create:
- ✅ GCS bucket: `oscal-tools-build-prod-us-central1`
- ✅ GCS bucket: `oscal-tools-library-prod-us-central1`
- ✅ Cloud Run service with GCS environment variables
- ✅ Service account with appropriate permissions

### 3. Bucket Naming Convention

Terraform creates buckets with the pattern:
```
{bucket_prefix}-{purpose}-{environment}-{region}
```

Examples:
- **DEV (manual):** `oscal-tools-build-dev-us-central1`
- **PROD (Terraform):** `oscal-tools-build-prod-us-central1`

### 4. Environment Variables (Production)

Terraform automatically configures these environment variables in Cloud Run:

| Variable | Value | Source |
|----------|-------|--------|
| `SPRING_PROFILES_ACTIVE` | `gcp` | Terraform |
| `GCP_PROJECT_ID` | `oscal-hub` | Terraform variable |
| `GCS_BUCKET_BUILD` | `oscal-tools-build-prod-us-central1` | Terraform module output |

### 5. Service Account Permissions

The Cloud Run service account needs these IAM roles:

- **Storage Object Admin** - `roles/storage.objectAdmin` (on bucket)
  - Read, write, and delete objects in GCS buckets
- **Cloud SQL Client** - `roles/cloudsql.client` (on project)
  - Connect to Cloud SQL database

**Grant permissions via gcloud:**

```bash
# Get service account email from Terraform output
SERVICE_ACCOUNT=$(terraform output -raw service_account_email)

# Grant Storage permissions
gcloud storage buckets add-iam-policy-binding \
  gs://oscal-tools-build-prod-us-central1 \
  --member="serviceAccount:${SERVICE_ACCOUNT}" \
  --role="roles/storage.objectAdmin"
```

**Or grant permissions in GCP Console:**
1. Navigate to Cloud Storage > Buckets
2. Select `oscal-tools-build-prod-us-central1`
3. Click "Permissions" tab
4. Add principal: `{service-account-email}`
5. Assign role: "Storage Object Admin"

## Configuration Files

### Development (application-dev.properties)

```properties
storage.provider=${STORAGE_PROVIDER:gcs}
gcp.project-id=${GCP_PROJECT_ID:oscal-hub}
gcp.storage.bucket-build=${GCS_BUCKET_BUILD:oscal-tools-build-dev-us-central1}
gcp.storage.build-folder=${GCS_BUILD_FOLDER:build}
```

### Production (application-gcp.properties)

```properties
storage.provider=gcs
gcp.project-id=${GCP_PROJECT_ID}
gcp.storage.bucket-build=${GCS_BUCKET_BUILD:oscal-tools-build-prod}
gcp.storage.build-folder=${GCS_BUILD_FOLDER:build}
file.upload-dir=${FILE_UPLOAD_DIR:/tmp/oscal-uploads}
```

## Troubleshooting

### Issue: "Access denied to GCS bucket"

**Solution:**
1. Check Application Default Credentials:
   ```bash
   gcloud auth application-default login
   ```

2. Verify IAM permissions:
   ```bash
   gcloud projects get-iam-policy oscal-hub \
     --flatten="bindings[].members" \
     --filter="bindings.members:user:YOUR_EMAIL"
   ```

3. Ensure you have the `Storage Object Admin` role

### Issue: "GCS bucket does not exist"

**For DEV:**
```bash
gcloud storage buckets create gs://oscal-tools-build-dev-us-central1 \
  --project=oscal-hub \
  --location=us-central1 \
  --uniform-bucket-level-access
```

**For PROD:**
```bash
cd terraform/gcp
terraform apply
```

### Issue: "Failed to initialize GCS client"

**Check credentials:**
```bash
# Verify ADC file exists
ls -la ~/.config/gcloud/application_default_credentials.json

# Re-authenticate
gcloud auth application-default login
```

**Check project configuration:**
```bash
gcloud config get-value project
# Should output: oscal-hub
```

### Issue: Application falls back to local storage

**Check logs for specific error:**
```bash
# Look for GCS error messages
tail -f back-end/logs/spring.log | grep -i "gcs\|storage"
```

The application will automatically fall back to local filesystem (`./uploads/build/`) if:
- GCS credentials are missing
- Bucket doesn't exist and can't be created
- Service account lacks permissions
- Network connectivity issues

## Switching Storage Providers

### Use Local Filesystem (Development)

```bash
# Unset the storage provider (falls back to local)
unset STORAGE_PROVIDER

# Or explicitly set to empty
export STORAGE_PROVIDER=""

./dev.sh
```

### Use Azure Blob Storage

```bash
export STORAGE_PROVIDER=azure
export AZURE_STORAGE_CONNECTION_STRING="DefaultEndpointsProtocol=https;..."

./dev.sh
```

### Use AWS S3

```bash
export STORAGE_PROVIDER=s3
export AWS_REGION=us-east-1
export AWS_S3_BUCKET_BUILD=my-s3-bucket

./dev.sh
```

## Cost Management

### Development
- **Storage:** Minimal cost (< $1/month for typical dev usage)
- **Operations:** Free tier covers most dev operations

### Production Best Practices

1. **Lifecycle Rules** (already configured in Terraform):
   - Delete archived versions after 5 newer versions
   - Delete temp files after 90 days

2. **Monitor Usage:**
   ```bash
   gcloud storage buckets describe gs://oscal-tools-build-prod-us-central1
   ```

3. **Set Budget Alerts:**
   - GCP Console > Billing > Budgets & Alerts
   - Set alerts at 50%, 90%, 100% of monthly budget

## Security Best Practices

### Development
✅ Use Application Default Credentials (never hardcode keys)
✅ Use personal Google account (not service account keys)
✅ Bucket is private by default (public-access-prevention enabled)

### Production
✅ Use Cloud Run service account (no key files)
✅ Enable uniform bucket-level access (Terraform default)
✅ Enable versioning (Terraform default)
✅ Use private buckets (public-access-prevention)
✅ Rotate service account keys regularly (if using key-based auth)
✅ Enable audit logging for compliance

## Next Steps

1. ✅ **DEV bucket created:** `oscal-tools-build-dev-us-central1`
2. ✅ **Application configured** to use GCS by default
3. ✅ **Application Default Credentials** configured
4. **For Production:**
   - Run `terraform apply` in `terraform/gcp/`
   - Grant service account permissions
   - Deploy application to Cloud Run

## Resources

- [GCS Documentation](https://cloud.google.com/storage/docs)
- [Application Default Credentials](https://cloud.google.com/docs/authentication/application-default-credentials)
- [Cloud Run Service Accounts](https://cloud.google.com/run/docs/securing/service-identity)
- [GCS Pricing](https://cloud.google.com/storage/pricing)

## Summary

| Environment | Bucket Name | Authentication | Status |
|-------------|-------------|----------------|--------|
| **Development** | `oscal-tools-build-dev-us-central1` | ADC (gcloud) | ✅ Ready |
| **Production** | `oscal-tools-build-prod-us-central1` | Service Account | ⏳ Deploy via Terraform |
