# Google Cloud Platform Deployment Guide

**Status:** Production Ready
**Date:** 2025-01-29
**Target Platform:** Google Cloud Platform (Cloud Run + Cloud SQL + Cloud Storage)

## Table of Contents

1. [Overview](#overview)
2. [Architecture](#architecture)
3. [Prerequisites](#prerequisites)
4. [Quick Start](#quick-start)
5. [Detailed Deployment Steps](#detailed-deployment-steps)
6. [Configuration](#configuration)
7. [Monitoring and Logging](#monitoring-and-logging)
8. [Troubleshooting](#troubleshooting)
9. [Cost Optimization](#cost-optimization)
10. [Security Best Practices](#security-best-practices)

---

## Overview

This guide walks you through deploying the OSCAL Tools application to Google Cloud Platform using:

- **Cloud Run** - Serverless containers for frontend and backend
- **Cloud SQL** - Managed PostgreSQL database
- **Cloud Storage** - Object storage for OSCAL files
- **Secret Manager** - Secure secret storage
- **Cloud Build** - CI/CD automation
- **Terraform** - Infrastructure as Code

### Why Google Cloud Platform?

- ✅ **Serverless** - Pay only for what you use, scale to zero
- ✅ **Fully Managed** - No server management required
- ✅ **Auto-scaling** - Handles traffic spikes automatically
- ✅ **Global** - Deploy to 40+ regions worldwide
- ✅ **Cost-effective** - Free tier + generous quotas

---

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                     Google Cloud Platform                    │
├─────────────────────────────────────────────────────────────┤
│                                                               │
│  ┌──────────────────┐          ┌──────────────────┐         │
│  │  Cloud Run       │          │  Cloud Run       │         │
│  │  (Frontend)      │◄────────►│  (Backend API)   │         │
│  │  Next.js         │          │  Spring Boot     │         │
│  └──────────────────┘          └────────┬─────────┘         │
│           │                              │                   │
│           │                              │                   │
│           ▼                              ▼                   │
│  ┌──────────────────┐          ┌──────────────────┐         │
│  │  Cloud CDN       │          │  Cloud SQL       │         │
│  │  (Static Assets) │          │  (PostgreSQL 15) │         │
│  └──────────────────┘          └──────────────────┘         │
│                                          │                   │
│                                          ▼                   │
│                              ┌──────────────────┐           │
│                              │ Cloud Storage    │           │
│                              │ (OSCAL Files)    │           │
│                              └──────────────────┘           │
│                                                              │
│  ├─────────────────── Supporting Services ─────────────────┤│
│  │ • Secret Manager    • Cloud Build    • VPC Connector   ││
│  │ • Artifact Registry • Cloud Monitoring • Load Balancer ││
│  └────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────┘
```

---

## Prerequisites

### Required Tools

1. **Google Cloud SDK (gcloud)**
   ```bash
   # Install gcloud CLI
   # macOS
   brew install --cask google-cloud-sdk

   # Linux
   curl https://sdk.cloud.google.com | bash

   # Windows
   # Download from: https://cloud.google.com/sdk/docs/install
   ```

2. **Terraform** (v1.5+)
   ```bash
   # macOS
   brew tap hashicorp/tap
   brew install hashicorp/tap/terraform

   # Linux
   wget https://releases.hashicorp.com/terraform/1.6.0/terraform_1.6.0_linux_amd64.zip
   unzip terraform_1.6.0_linux_amd64.zip
   sudo mv terraform /usr/local/bin/

   # Windows
   choco install terraform
   ```

3. **Docker** (for local builds, optional)
   ```bash
   # macOS
   brew install --cask docker

   # Linux
   sudo apt-get install docker.io

   # Windows
   # Download from: https://www.docker.com/products/docker-desktop
   ```

### GCP Account Setup

1. **Create GCP Project**
   ```bash
   # Authenticate with Google Cloud
   gcloud auth login

   # Create a new project
   export PROJECT_ID="oscal-tools-prod"
   gcloud projects create $PROJECT_ID --name="OSCAL Tools Production"

   # Set as default project
   gcloud config set project $PROJECT_ID

   # Enable billing (required for Cloud Run, Cloud SQL, etc.)
   # Visit: https://console.cloud.google.com/billing
   ```

2. **Enable Required APIs**
   ```bash
   gcloud services enable \
     run.googleapis.com \
     sql-component.googleapis.com \
     sqladmin.googleapis.com \
     storage.googleapis.com \
     secretmanager.googleapis.com \
     vpcaccess.googleapis.com \
     cloudbuild.googleapis.com \
     artifactregistry.googleapis.com \
     compute.googleapis.com
   ```

3. **Set up Authentication**
   ```bash
   # Create service account for Terraform (optional but recommended)
   gcloud iam service-accounts create terraform-sa \
     --display-name="Terraform Service Account"

   # Grant necessary permissions
   gcloud projects add-iam-policy-binding $PROJECT_ID \
     --member="serviceAccount:terraform-sa@${PROJECT_ID}.iam.gserviceaccount.com" \
     --role="roles/editor"

   # Create and download key
   gcloud iam service-accounts keys create ~/terraform-key.json \
     --iam-account=terraform-sa@${PROJECT_ID}.iam.gserviceaccount.com

   # Set environment variable
   export GOOGLE_APPLICATION_CREDENTIALS=~/terraform-key.json
   ```

---

## Quick Start

### Option 1: Automated Deployment (Recommended)

```bash
# Clone the repository
git clone https://github.com/your-org/oscal-cli.git
cd oscal-cli

# Run the deployment script
./deploy-gcp.sh --project-id=$PROJECT_ID --region=us-central1 --environment=prod

# Follow the prompts to deploy infrastructure and application
```

### Option 2: Manual Deployment

See [Detailed Deployment Steps](#detailed-deployment-steps) below.

---

## Detailed Deployment Steps

### Step 1: Infrastructure Deployment with Terraform

1. **Navigate to Terraform directory**
   ```bash
   cd terraform/gcp
   ```

2. **Create `terraform.tfvars` file**
   ```bash
   cat > terraform.tfvars <<EOF
   project_id  = "$PROJECT_ID"
   region      = "us-central1"
   environment = "prod"

   # Database configuration
   db_tier     = "db-f1-micro"  # Start small, upgrade later
   db_name     = "oscal_production"
   db_username = "oscal_user"

   # Cloud Run configuration
   backend_cpu           = "1000m"
   backend_memory        = "2Gi"
   backend_min_instances = 0  # Scale to zero for cost savings
   backend_max_instances = 10

   frontend_cpu           = "1000m"
   frontend_memory        = "512Mi"
   frontend_min_instances = 0
   frontend_max_instances = 10
   EOF
   ```

3. **Initialize Terraform**
   ```bash
   terraform init
   ```

4. **Review the deployment plan**
   ```bash
   terraform plan
   ```

5. **Deploy infrastructure**
   ```bash
   terraform apply

   # Type "yes" when prompted
   ```

6. **Note the outputs**
   ```bash
   terraform output

   # Save these values - you'll need them later:
   # - frontend_url
   # - backend_url
   # - database_connection_name
   ```

### Step 2: Build and Push Container Images

1. **Create Artifact Registry repository**
   ```bash
   gcloud artifacts repositories create oscal-tools \
     --repository-format=docker \
     --location=us-central1 \
     --description="OSCAL Tools container images"
   ```

2. **Build and push images using Cloud Build**
   ```bash
   cd ../..  # Back to project root

   gcloud builds submit \
     --config=cloudbuild.yaml \
     --substitutions=_REGION=us-central1,_ENVIRONMENT=prod
   ```

   This will:
   - Build backend Docker image
   - Build frontend Docker image
   - Push both to Artifact Registry
   - Deploy to Cloud Run

### Step 3: Verify Deployment

1. **Check Cloud Run services**
   ```bash
   gcloud run services list --region=us-central1
   ```

2. **Test the backend**
   ```bash
   BACKEND_URL=$(gcloud run services describe oscal-backend-prod \
     --region=us-central1 \
     --format='value(status.url)')

   curl $BACKEND_URL/actuator/health
   ```

3. **Test the frontend**
   ```bash
   FRONTEND_URL=$(gcloud run services describe oscal-frontend-prod \
     --region=us-central1 \
     --format='value(status.url)')

   open $FRONTEND_URL  # macOS
   # or
   xdg-open $FRONTEND_URL  # Linux
   ```

### Step 4: Configure Secrets

Secrets are automatically created by Terraform, but you can update them:

```bash
# Update JWT secret
echo -n "your-new-jwt-secret" | \
  gcloud secrets versions add jwt-secret --data-file=-

# Update database password
echo -n "your-new-db-password" | \
  gcloud secrets versions add db-password --data-file=-
```

### Step 5: Set up Custom Domain (Optional)

1. **Add domain mapping**
   ```bash
   gcloud run domain-mappings create \
     --service=oscal-frontend-prod \
     --domain=oscal.your-domain.com \
     --region=us-central1
   ```

2. **Update DNS records**
   Follow the instructions from the command output to add the required DNS records.

---

## Configuration

### Environment Variables

#### Backend (Cloud Run)

| Variable | Description | Required | Default |
|----------|-------------|----------|---------|
| `SPRING_PROFILES_ACTIVE` | Spring Boot profile | Yes | `gcp` |
| `GCP_PROJECT_ID` | GCP project ID | Yes | - |
| `DB_URL` | Cloud SQL connection string | Yes | - |
| `DB_USERNAME` | Database username | Yes | `oscal_user` |
| `DB_PASSWORD` | Database password (from Secret Manager) | Yes | - |
| `JWT_SECRET` | JWT signing secret (from Secret Manager) | Yes | - |
| `GCS_BUCKET_BUILD` | Cloud Storage bucket name | Yes | - |
| `CORS_ALLOWED_ORIGINS` | Frontend URL for CORS | Yes | - |

#### Frontend (Cloud Run)

| Variable | Description | Required | Default |
|----------|-------------|----------|---------|
| `NODE_ENV` | Node environment | Yes | `production` |
| `NEXT_PUBLIC_API_URL` | Backend API URL | Yes | - |
| `PORT` | Server port | No | `3000` |

### Spring Boot Profiles

- **gcp** - Production profile for Google Cloud Platform
- **dev** - Development profile (local testing)
- **staging** - Staging environment

Profile configuration: `back-end/src/main/resources/application-gcp.properties`

---

## Monitoring and Logging

### Cloud Logging

**View logs:**
```bash
# Backend logs
gcloud logging read "resource.type=cloud_run_revision AND resource.labels.service_name=oscal-backend-prod" \
  --limit=50 \
  --format=json

# Frontend logs
gcloud logging read "resource.type=cloud_run_revision AND resource.labels.service_name=oscal-frontend-prod" \
  --limit=50 \
  --format=json

# Filter by severity
gcloud logging read "resource.type=cloud_run_revision AND severity>=ERROR" \
  --limit=50
```

**Log-based metrics:**
```bash
# Create metric for 5xx errors
gcloud logging metrics create http_5xx_errors \
  --description="Count of HTTP 5xx errors" \
  --log-filter='resource.type="cloud_run_revision" AND httpRequest.status>=500'
```

### Cloud Monitoring

**View dashboards:**
- Navigate to https://console.cloud.google.com/monitoring
- Select "Dashboards" → "Cloud Run"

**Create alert policies:**
```bash
# Alert on high error rate
gcloud alpha monitoring policies create \
  --notification-channels=CHANNEL_ID \
  --display-name="OSCAL High Error Rate" \
  --condition-display-name="Error rate > 5%" \
  --condition-filter='resource.type="cloud_run_revision" AND metric.type="run.googleapis.com/request_count" AND metric.label.response_code_class="5xx"' \
  --condition-threshold-value=5 \
  --condition-threshold-duration=300s
```

### Application Performance

**Cloud Run metrics:**
- Request count
- Request latency (p50, p95, p99)
- Container CPU utilization
- Container memory utilization
- Container instance count

**Cloud SQL metrics:**
- CPU utilization
- Memory utilization
- Connections (active/total)
- Query execution time
- Disk I/O

**Cloud Storage metrics:**
- Request count
- Data transfer (egress/ingress)
- Object count
- Storage size

---

## Troubleshooting

### Common Issues

#### 1. Cloud Run Service Won't Start

**Symptom:** Service stuck in "DEPLOYING" state

**Diagnosis:**
```bash
gcloud run services describe oscal-backend-prod \
  --region=us-central1 \
  --format='value(status.conditions)'
```

**Solutions:**
- Check container logs for startup errors
- Verify all environment variables are set
- Ensure Secret Manager secrets are accessible
- Check VPC Connector status

#### 2. Database Connection Failures

**Symptom:** Backend logs show "Connection refused" or "Cannot connect to database"

**Diagnosis:**
```bash
# Check Cloud SQL instance status
gcloud sql instances describe oscal-db-prod-XXXX

# Test connection from Cloud Shell
gcloud sql connect oscal-db-prod-XXXX --user=oscal_user
```

**Solutions:**
- Verify Cloud SQL instance is running
- Check VPC Connector is properly configured
- Ensure Cloud SQL connection string is correct
- Verify database credentials in Secret Manager

#### 3. 403 Forbidden Errors from Frontend

**Symptom:** API requests fail with 403 status

**Diagnosis:**
- Check browser console for error messages
- Verify JWT token in localStorage
- Test backend health endpoint directly

**Solutions:**
- Clear browser cache and re-login
- Check CORS configuration in backend
- Verify frontend is using correct backend URL
- Ensure backend service is publicly accessible

#### 4. Cloud Storage Access Denied

**Symptom:** File uploads fail with "Access Denied"

**Diagnosis:**
```bash
# Check service account permissions
gcloud iam service-accounts get-iam-policy \
  oscal-backend-sa-prod@$PROJECT_ID.iam.gserviceaccount.com
```

**Solutions:**
- Grant `storage.objectAdmin` role to service account
- Verify bucket exists and is accessible
- Check bucket IAM policy

### Debug Commands

```bash
# Get service details
gcloud run services describe SERVICE_NAME --region=REGION

# View recent logs (last 1 hour)
gcloud logging read "resource.type=cloud_run_revision" \
  --limit=100 \
  --freshness=1h

# Check Cloud SQL connection
gcloud sql connect INSTANCE_NAME --user=USERNAME

# Test backend health
curl https://BACKEND_URL/actuator/health

# List Cloud Storage buckets
gsutil ls

# View bucket contents
gsutil ls gs://BUCKET_NAME/

# Download secret value
gcloud secrets versions access latest --secret=SECRET_NAME
```

---

## Cost Optimization

### Monthly Cost Estimate

**Minimal Usage (Dev/Test):**
- Cloud Run (frontend): $0 - $5/month
- Cloud Run (backend): $0 - $10/month
- Cloud SQL (db-f1-micro): $7.67/month
- Cloud Storage: $0.026/GB/month
- **Total: ~$15-20/month**

**Moderate Usage (Small Production):**
- Cloud Run (frontend): $10 - $30/month
- Cloud Run (backend): $20 - $60/month
- Cloud SQL (db-custom-2-7680): $60/month
- Cloud Storage: $1 - $5/month
- Secret Manager: $0.06/secret/month
- **Total: ~$90-160/month**

**High Usage (Large Production):**
- Cloud Run (frontend + backend): $200 - $500/month
- Cloud SQL (high-availability): $300/month
- Cloud Storage: $20/month
- Load Balancer: $18/month
- Cloud CDN: $20/month
- **Total: ~$560-860/month**

### Cost Reduction Tips

1. **Scale to Zero**
   - Set `min_instances = 0` for Cloud Run services
   - Services will scale down when not in use

2. **Right-size Resources**
   - Start with `db-f1-micro` for database
   - Use `1000m CPU / 512Mi memory` for frontend
   - Monitor and adjust based on actual usage

3. **Enable Cloud CDN**
   - Reduces Cloud Run requests for static assets
   - Improves performance globally

4. **Set up Lifecycle Policies**
   - Archive old files in Cloud Storage
   - Delete temporary files after 30 days

5. **Use Committed Use Discounts**
   - 1-year or 3-year commitments save 25-40%
   - Apply to Cloud SQL and Compute Engine

6. **Monitor with Budget Alerts**
   ```bash
   # Create budget alert
   gcloud billing budgets create \
     --billing-account=BILLING_ACCOUNT_ID \
     --display-name="OSCAL Tools Budget" \
     --budget-amount=100 \
     --threshold-rule=percent=50 \
     --threshold-rule=percent=90 \
     --threshold-rule=percent=100
   ```

---

## Security Best Practices

### 1. Secret Management

✅ **DO:**
- Store all secrets in Secret Manager
- Use IAM to control secret access
- Rotate secrets regularly
- Use separate secrets for each environment

❌ **DON'T:**
- Hard-code secrets in code or config files
- Commit secrets to version control
- Share secrets via email or chat
- Use the same secret across environments

### 2. IAM and Access Control

✅ **DO:**
- Use service accounts for Cloud Run services
- Follow principle of least privilege
- Enable Cloud Armor for DDoS protection
- Require authentication for sensitive endpoints

❌ **DON'T:**
- Use user credentials for services
- Grant overly broad permissions
- Allow unauthenticated access to backend
- Share service account keys

### 3. Network Security

✅ **DO:**
- Use VPC Connector for Cloud SQL access
- Enable Cloud SQL private IP
- Use Cloud Run ingress controls
- Enable HTTPS-only

❌ **DON'T:**
- Expose Cloud SQL publicly
- Allow HTTP traffic
- Use default VPC for production
- Disable SSL/TLS verification

### 4. Data Protection

✅ **DO:**
- Enable encryption at rest (default)
- Enable encryption in transit
- Use Cloud Storage versioning
- Implement backup retention policies
- Enable Cloud SQL automated backups

❌ **DON'T:**
- Store sensitive data unencrypted
- Disable backup automation
- Ignore data retention requirements

### 5. Monitoring and Auditing

✅ **DO:**
- Enable Cloud Audit Logs
- Set up alerting for security events
- Monitor for unusual access patterns
- Review IAM permissions regularly

❌ **DON'T:**
- Ignore security alerts
- Disable audit logging
- Skip security reviews

---

## Next Steps

1. **Set up CI/CD**
   - Create GitHub Actions or Cloud Build triggers
   - Automate deployments on git push

2. **Add Custom Domain**
   - Configure Cloud DNS
   - Set up SSL certificates

3. **Enable Cloud CDN**
   - Improve global performance
   - Reduce costs

4. **Set up Monitoring**
   - Create custom dashboards
   - Configure alert policies

5. **Implement Backup Strategy**
   - Schedule Cloud SQL backups
   - Test restore procedures

6. **Scale for Production**
   - Upgrade database tier
   - Enable high availability
   - Add load balancing

---

## Resources

- [Cloud Run Documentation](https://cloud.google.com/run/docs)
- [Cloud SQL Documentation](https://cloud.google.com/sql/docs)
- [Cloud Storage Documentation](https://cloud.google.com/storage/docs)
- [Terraform GCP Provider](https://registry.terraform.io/providers/hashicorp/google/latest/docs)
- [OSCAL Tools GitHub Repository](https://github.com/RegScale/oscal-hub)

---

## Support

For issues or questions:
- GitHub Issues: https://github.com/RegScale/oscal-hub/issues
- OSCAL Gitter: https://gitter.im/usnistgov-OSCAL/Lobby
- Google Cloud Support: https://cloud.google.com/support

---

**Last Updated:** 2025-01-29
**Version:** 1.0.0
