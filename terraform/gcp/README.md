# OSCAL Tools - GCP Terraform Infrastructure

This directory contains Terraform configuration for deploying OSCAL Tools to Google Cloud Platform.

## Prerequisites

1. **GCP Project**: Create a GCP project at https://console.cloud.google.com
2. **gcloud CLI**: Install from https://cloud.google.com/sdk/docs/install
3. **Terraform**: Install from https://www.terraform.io/downloads (v1.5.0+)
4. **Container Images**: Images must be built and pushed to Artifact Registry before deploying

## Quick Start

### 1. Authenticate with GCP

```bash
# Use Application Default Credentials (recommended for local dev)
unset GOOGLE_APPLICATION_CREDENTIALS  # If you have this set
gcloud auth application-default login

# Set your project
gcloud config set project YOUR_PROJECT_ID
```

**Note**: If you have `GOOGLE_APPLICATION_CREDENTIALS` environment variable set to a service account key file, you can either:
- **Unset it** and use ADC (simpler for local dev): `unset GOOGLE_APPLICATION_CREDENTIALS`
- **Keep it** if you prefer using a service account key

### 2. Create terraform.tfvars

Create a `terraform.tfvars` file in this directory (it's git-ignored):

```hcl
# Required
project_id  = "your-project-id"
region      = "us-central1"
environment = "prod"

# Optional overrides
# db_name     = "oscal"
# db_username = "oscal-admin"
# db_tier     = "db-f1-micro"  # Use "db-g1-small" for next tier up
# backend_max_instances  = 10
# backend_min_instances  = 0
# frontend_max_instances = 10
# frontend_min_instances = 0
```

### 3. Build Container Images

Container images must exist in Artifact Registry before deploying infrastructure.

**Option A: From project root (recommended)**
```bash
cd ../..
./deploy-gcp.sh --project-id YOUR_PROJECT_ID --skip-terraform
```

**Option B: Manually**
```bash
cd ../..

# Create Artifact Registry repository
gcloud artifacts repositories create oscal-tools \
  --repository-format=docker \
  --location=us-central1

# Build and push images
gcloud builds submit --config=cloudbuild-images.yaml
```

### 4. Deploy Infrastructure

From this directory (`terraform/gcp`):

```bash
# Plan (dry run)
./deploy.sh plan

# Apply (deploy)
./deploy.sh apply

# Destroy (tear down)
./deploy.sh destroy
```

Or use Terraform directly:

```bash
terraform init
terraform plan
terraform apply
```

## Directory Structure

```
terraform/gcp/
├── README.md              # This file
├── deploy.sh              # Helper script for deployment
├── main.tf                # Main infrastructure orchestration
├── variables.tf           # Input variables
├── outputs.tf             # Output values
├── terraform.tfvars       # Your configuration (git-ignored)
├── .terraform/            # Terraform cache (git-ignored)
└── modules/
    ├── cloud-run/         # Cloud Run service module
    ├── cloud-sql/         # Cloud SQL database module
    ├── cloud-storage/     # Cloud Storage buckets module
    ├── networking/        # VPC Connector module
    └── secrets/           # Secret Manager module
```

## Two Deployment Approaches

### Approach 1: Use Root deploy-gcp.sh (All-in-One)

From project root:
```bash
./deploy-gcp.sh --project-id YOUR_PROJECT_ID
```

This script:
- Creates Artifact Registry
- Builds container images
- Deploys Terraform infrastructure
- One command does everything

### Approach 2: Use terraform/gcp/deploy.sh (Terraform Only)

From this directory:
```bash
./deploy.sh apply
```

This script:
- Only runs Terraform
- Assumes images are already built
- Good for infrastructure-only updates

**Which to use?**
- **First deployment**: Use root `deploy-gcp.sh` (all-in-one)
- **Infrastructure updates**: Use `terraform/gcp/deploy.sh` (faster)
- **Code updates**: Rebuild images and run `terraform apply`

## After Deployment

### Get Database Password

```bash
# View password
terraform output -raw database_password

# Or from Secret Manager
gcloud secrets versions access latest --secret=db-password
```

### Connect to Database Locally

```bash
# Install Cloud SQL Proxy
brew install cloud-sql-proxy

# Get connection name
CONNECTION_NAME=$(terraform output -raw database_connection_name)

# Start proxy
cloud-sql-proxy $CONNECTION_NAME

# In another terminal, connect
PGPASSWORD=$(terraform output -raw database_password) \
  psql "host=127.0.0.1 user=$(terraform output -raw database_username) dbname=$(terraform output -raw database_name)"
```

### View Application

```bash
# Get URLs
terraform output frontend_url
terraform output backend_url

# Open in browser
open $(terraform output -raw frontend_url)
```

## Updating the Deployment

### Update Application Code

After changing application code:

```bash
# From project root
cd ../..
gcloud builds submit --config=cloudbuild-images.yaml

# Back to terraform directory
cd terraform/gcp

# Redeploy services (Terraform will pick up new images)
terraform apply
```

**Note**: There are two Cloud Build configs:
- `cloudbuild-images.yaml` - Builds images only (use with Terraform)
- `cloudbuild.yaml` - Full CI/CD pipeline with deployment (alternative to Terraform)

### Update Infrastructure

After changing Terraform configuration:

```bash
# From this directory
./deploy.sh apply
```

## Troubleshooting

### Authentication Issues

```bash
# Check current auth
gcloud auth list

# Re-authenticate
gcloud auth application-default login

# Verify credentials work
gcloud projects list
```

### Images Not Found

Error: `Image not found in Artifact Registry`

```bash
# Verify images exist
gcloud artifacts docker images list \
  us-central1-docker.pkg.dev/YOUR_PROJECT_ID/oscal-tools

# If missing, build them
cd ../..
gcloud builds submit --config=cloudbuild-images.yaml
```

### Terraform State Locked

```bash
# If Terraform is stuck with a lock
terraform force-unlock LOCK_ID
```

### Database Connection Issues

```bash
# Check Cloud SQL instance status
gcloud sql instances describe $(terraform output -raw database_instance_name)

# View Cloud SQL logs
gcloud sql operations list --instance=$(terraform output -raw database_instance_name)
```

## Cost Management

### Development Environment

Use minimal resources in `terraform.tfvars`:

```hcl
environment = "dev"
db_tier     = "db-f1-micro"
backend_min_instances  = 0  # Scale to zero
frontend_min_instances = 0  # Scale to zero
```

### Tear Down When Not Needed

```bash
# Destroy all infrastructure
./deploy.sh destroy

# Rebuild later with
./deploy.sh apply
```

## Security Notes

1. **Service Account Keys**: If using `GOOGLE_APPLICATION_CREDENTIALS`, keep key files secure and git-ignored
2. **Terraform State**: Contains sensitive data (passwords, keys). Keep secure. Consider using GCS backend for team collaboration.
3. **Database**: Uses private IP only. Requires Cloud SQL Proxy or VPC access to connect.
4. **Secrets**: Stored in Secret Manager with encryption at rest.

## State Management (Team Collaboration)

For team environments, store Terraform state in GCS:

```bash
# Create state bucket
gsutil mb -p YOUR_PROJECT_ID -l us-central1 gs://YOUR_PROJECT_ID-terraform-state

# Enable versioning
gsutil versioning set on gs://YOUR_PROJECT_ID-terraform-state

# Update main.tf backend configuration (uncomment and configure)
```

Then in `main.tf`, uncomment:

```hcl
backend "gcs" {
  bucket = "YOUR_PROJECT_ID-terraform-state"
  prefix = "terraform/state"
}
```

## Additional Resources

- [GCP Setup Guide](../../docs/GCP-SETUP-GUIDE.md)
- [GCP Deployment Guide](../../docs/GCP-DEPLOYMENT-GUIDE.md)
- [GCP Cost & Monitoring](../../docs/GCP-COST-AND-MONITORING.md)
