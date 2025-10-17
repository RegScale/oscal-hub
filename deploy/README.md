# OSCAL CLI - Azure Deployment Guide

This directory contains Terraform configurations to deploy the OSCAL CLI application to Azure using Container Apps.

## Table of Contents

- [Overview](#overview)
- [Prerequisites](#prerequisites)
- [Architecture](#architecture)
- [Quick Start](#quick-start)
- [Deployment Modes](#deployment-modes)
- [Step-by-Step Deployment](#step-by-step-deployment)
- [Configuration](#configuration)
- [CI/CD with GitHub Actions](#cicd-with-github-actions)
- [Monitoring and Observability](#monitoring-and-observability)
- [Scaling](#scaling)
- [Cost Estimation](#cost-estimation)
- [Troubleshooting](#troubleshooting)

## Overview

This deployment uses Azure Container Apps, a fully managed serverless container platform that provides:

- Auto-scaling from 0 to N instances
- Built-in load balancing and HTTPS
- No infrastructure management
- Pay-per-use pricing
- Easy integration with Azure services

## Prerequisites

### Required Tools

1. **Azure CLI** (v2.50+)
   ```bash
   # Install Azure CLI
   curl -sL https://aka.ms/InstallAzureCLIDeb | sudo bash

   # Login to Azure
   az login

   # Set your subscription
   az account set --subscription "Your Subscription Name"
   ```

2. **Terraform** (v1.5+)
   ```bash
   # Install Terraform
   wget https://releases.hashicorp.com/terraform/1.6.0/terraform_1.6.0_linux_amd64.zip
   unzip terraform_1.6.0_linux_amd64.zip
   sudo mv terraform /usr/local/bin/
   ```

3. **Docker** (for building and pushing images)
   ```bash
   # Verify Docker installation
   docker --version
   ```

### Azure Permissions

You need the following permissions in your Azure subscription:
- Contributor role (to create resources)
- Or specific permissions: `Microsoft.ContainerRegistry/*`, `Microsoft.App/*`, `Microsoft.OperationalInsights/*`

## Architecture

### Deployment Mode: Combined (Default)

```
┌─────────────────────────────────────────┐
│   Azure Container Apps Environment      │
│  ┌───────────────────────────────────┐  │
│  │   Container App (Combined)        │  │
│  │  ┌──────────────────────────┐    │  │
│  │  │  Spring Boot Backend     │    │  │
│  │  │  (Port 8080)             │    │  │
│  │  └──────────────────────────┘    │  │
│  │  ┌──────────────────────────┐    │  │
│  │  │  Next.js Frontend        │    │  │
│  │  │  (Port 3000)             │    │  │
│  │  └──────────────────────────┘    │  │
│  └───────────────────────────────────┘  │
└─────────────────────────────────────────┘
         │                    │
         ▼                    ▼
   Azure Container      Log Analytics
      Registry          + App Insights
```

### Deployment Mode: Separate (Production)

```
┌─────────────────────────────────────────┐
│   Azure Container Apps Environment      │
│  ┌────────────────┐  ┌───────────────┐  │
│  │  Frontend App  │  │  Backend App  │  │
│  │  (Next.js)     │──▶ (Spring Boot) │  │
│  │  Port 3000     │  │  Port 8080    │  │
│  └────────────────┘  └───────────────┘  │
└─────────────────────────────────────────┘
         │                    │
         ▼                    ▼
   Azure Container      Log Analytics
      Registry          + App Insights
```

## Quick Start

### 1. Configure Your Deployment

```bash
# Navigate to deploy directory
cd deploy

# Copy example configuration
cp terraform.tfvars.example terraform.tfvars

# Edit terraform.tfvars with your settings
# At minimum, update: project_name, environment, location
nano terraform.tfvars
```

### 2. Initialize Terraform

```bash
# Initialize Terraform
terraform init

# Validate configuration
terraform validate

# Preview changes
terraform plan
```

### 3. Deploy Infrastructure

```bash
# Deploy to Azure
terraform apply

# Review the plan and type 'yes' to confirm
```

### 4. Build and Push Docker Image

```bash
# Navigate back to project root
cd ..

# Build the Docker image
docker build -t oscal-ux:latest .

# Get ACR login server from Terraform output
ACR_NAME=$(cd deploy && terraform output -raw container_registry_name)

# Login to Azure Container Registry
az acr login --name $ACR_NAME

# Tag and push image
ACR_SERVER=$(cd deploy && terraform output -raw container_registry_login_server)
docker tag oscal-ux:latest $ACR_SERVER/oscal-ux:latest
docker push $ACR_SERVER/oscal-ux:latest
```

### 5. Access Your Application

```bash
# Get application URL
cd deploy
terraform output application_url

# Open in browser
# https://ca-oscal-cli-dev.xxx.azurecontainerapps.io
```

## Deployment Modes

### Combined Mode (Recommended for Getting Started)

**Pros:**
- Simplest deployment (single container)
- Easier to manage
- Lower cost for small/medium workloads
- Perfect for dev/staging environments

**Cons:**
- Frontend and backend scale together
- Less flexible resource allocation

**Configuration:**
```hcl
deployment_mode = "combined"
```

### Separate Mode (Recommended for Production)

**Pros:**
- Independent scaling of frontend/backend
- Better resource utilization
- Easier to update individual components
- Better for high-traffic scenarios

**Cons:**
- Slightly more complex setup
- Higher minimum cost (2 containers always running)

**Configuration:**
```hcl
deployment_mode = "separate"
```

## Step-by-Step Deployment

### Option 1: Deploy with Default Configuration (Dev)

```bash
cd deploy
terraform init
terraform apply -var-file="environments/dev.tfvars"
```

### Option 2: Deploy to Production

```bash
cd deploy
terraform init
terraform apply -var-file="environments/prod.tfvars"
```

### Option 3: Custom Configuration

```bash
cd deploy
terraform init

# Override specific variables
terraform apply \
  -var="environment=staging" \
  -var="location=westus2" \
  -var="combined_min_replicas=1" \
  -var="combined_max_replicas=5"
```

## Configuration

### Key Variables

| Variable | Description | Default | Example |
|----------|-------------|---------|---------|
| `project_name` | Project identifier | `oscal-cli` | `my-oscal` |
| `environment` | Environment name | `dev` | `prod`, `staging` |
| `location` | Azure region | `eastus` | `westus2`, `centralus` |
| `deployment_mode` | Architecture type | `combined` | `separate` |
| `combined_min_replicas` | Minimum instances | `0` | `2` (for HA) |
| `combined_max_replicas` | Maximum instances | `10` | `50` |
| `enable_application_insights` | Enable monitoring | `true` | `false` |
| `enable_blob_storage` | Enable file storage | `false` | `true` |

### Environment-Specific Configurations

Development:
```bash
terraform apply -var-file="environments/dev.tfvars"
```

Production:
```bash
terraform apply -var-file="environments/prod.tfvars"
```

## CI/CD with GitHub Actions

### Setup GitHub Actions

1. **Create Azure Service Principal**

```bash
# Create service principal
az ad sp create-for-rbac \
  --name "oscal-cli-github-actions" \
  --role contributor \
  --scopes /subscriptions/YOUR_SUBSCRIPTION_ID \
  --sdk-auth

# Save the JSON output as GitHub secret: AZURE_CREDENTIALS
```

2. **Add GitHub Secrets**

Go to your repository → Settings → Secrets and add:

- `AZURE_CREDENTIALS` - Output from above command
- `ACR_USERNAME` - From `terraform output container_registry_admin_username`
- `ACR_PASSWORD` - From `terraform output container_registry_admin_password`

3. **Workflow Files**

The workflow files are located in `.github/workflows/`:
- `azure-deploy.yml` - Main deployment workflow
- Triggered on push to `main` branch or manually

### Manual Deployment Trigger

```bash
# Trigger deployment from GitHub UI
# Actions → Deploy to Azure → Run workflow
```

## Monitoring and Observability

### Application Insights

When `enable_application_insights = true`, you get:

- Request tracking
- Dependency tracking
- Exception logging
- Performance metrics
- Custom metrics

**Access Application Insights:**
```bash
# Get App Insights portal URL
APP_INSIGHTS_ID=$(cd deploy && terraform output -raw application_insights_app_id)
echo "https://portal.azure.com/#@/resource/subscriptions/.../providers/microsoft.insights/components/$APP_INSIGHTS_ID"
```

### Log Analytics

View container logs:

```bash
# Get Log Analytics workspace
WORKSPACE_NAME=$(cd deploy && terraform output -raw log_analytics_workspace_name)

# Query logs using Azure CLI
az monitor log-analytics query \
  --workspace $WORKSPACE_NAME \
  --analytics-query "ContainerAppConsoleLogs_CL | take 100"
```

### Health Checks

Container Apps provides built-in health monitoring. Check status:

```bash
# Get container app name
APP_NAME="ca-oscal-cli-dev"

# Check revision status
az containerapp revision list \
  --name $APP_NAME \
  --resource-group rg-oscal-cli-dev \
  --output table
```

## Scaling

### Auto-Scaling Configuration

Container Apps automatically scales based on:

1. **HTTP Traffic** (enabled by default)
   - Scales based on concurrent requests
   - Default: 10 concurrent requests per replica

2. **CPU Usage** (configurable)
   ```hcl
   enable_cpu_scaling = true
   cpu_threshold = 70  # percentage
   ```

3. **Memory Usage** (configurable)
   ```hcl
   enable_memory_scaling = true
   memory_threshold = 80  # percentage
   ```

### Manual Scaling

Temporarily override auto-scaling:

```bash
# Scale to specific replica count
az containerapp update \
  --name ca-oscal-cli-dev \
  --resource-group rg-oscal-cli-dev \
  --min-replicas 3 \
  --max-replicas 10
```

## Cost Estimation

### Development Environment (Combined Mode, Scale to Zero)

| Resource | SKU/Size | Estimated Cost |
|----------|----------|----------------|
| Container App | 0.5 vCPU, 1Gi RAM, 8 hours/day | ~$15/month |
| Container Registry | Basic | ~$5/month |
| Log Analytics | Pay-as-you-go | ~$5/month |
| **Total** | | **~$25/month** |

### Production Environment (Separate Mode, HA)

| Resource | SKU/Size | Estimated Cost |
|----------|----------|----------------|
| Container App (Frontend) | 0.5 vCPU, 1Gi RAM, 2 replicas | ~$40/month |
| Container App (Backend) | 1 vCPU, 2Gi RAM, 2 replicas | ~$100/month |
| Container Registry | Standard | ~$20/month |
| Log Analytics + App Insights | Pay-as-you-go | ~$30/month |
| Blob Storage | LRS, minimal usage | ~$5/month |
| **Total** | | **~$195/month** |

**Note:** Costs vary based on actual usage and region. Enable scale-to-zero in dev to save costs.

### Cost Optimization Tips

1. **Scale to zero in dev/staging**
   ```hcl
   combined_min_replicas = 0  # Dev only!
   ```

2. **Use Basic SKU for ACR in dev**
   ```hcl
   acr_sku = "Basic"
   ```

3. **Reduce log retention**
   ```hcl
   log_retention_days = 7  # vs 30-90 in prod
   ```

4. **Disable Application Insights in dev**
   ```hcl
   enable_application_insights = false
   ```

## Troubleshooting

### Issue: Container fails to start

**Check logs:**
```bash
az containerapp logs show \
  --name ca-oscal-cli-dev \
  --resource-group rg-oscal-cli-dev \
  --tail 100
```

**Common causes:**
- Image not found in ACR
- Incorrect environment variables
- Port mismatch
- Resource limits too low

### Issue: Cannot access application

**Verify ingress:**
```bash
az containerapp show \
  --name ca-oscal-cli-dev \
  --resource-group rg-oscal-cli-dev \
  --query properties.configuration.ingress
```

**Check if ingress is enabled:**
```hcl
enable_ingress = true
```

### Issue: Terraform apply fails

**Check Azure quota:**
```bash
az vm list-usage --location eastus --output table
```

**Verify service principal permissions:**
```bash
az role assignment list --assignee YOUR_SP_ID
```

### Issue: High costs

**Review actual usage:**
```bash
# Check current replica count
az containerapp revision list \
  --name ca-oscal-cli-dev \
  --resource-group rg-oscal-cli-dev \
  --query "[].properties.replicas"
```

**Adjust min/max replicas:**
```hcl
combined_min_replicas = 0
combined_max_replicas = 5
```

### Getting Help

**View all Terraform outputs:**
```bash
cd deploy
terraform output
```

**Destroy and recreate (dev only!):**
```bash
cd deploy
terraform destroy
terraform apply
```

**Check Azure resource status:**
```bash
az group show --name rg-oscal-cli-dev
az resource list --resource-group rg-oscal-cli-dev --output table
```

## Updating the Application

### Option 1: Push New Image (Recommended)

```bash
# Build new version
docker build -t oscal-ux:v2.0 .

# Push to ACR
az acr login --name YOUR_ACR_NAME
docker tag oscal-ux:v2.0 YOUR_ACR.azurecr.io/oscal-ux:v2.0
docker push YOUR_ACR.azurecr.io/oscal-ux:v2.0

# Update Container App
az containerapp update \
  --name ca-oscal-cli-dev \
  --resource-group rg-oscal-cli-dev \
  --image YOUR_ACR.azurecr.io/oscal-ux:v2.0
```

### Option 2: Update via Terraform

```bash
# Update docker_image_tag in terraform.tfvars
docker_image_tag = "v2.0"

# Apply changes
cd deploy
terraform apply
```

## Clean Up

### Destroy All Resources

```bash
cd deploy
terraform destroy

# Confirm by typing 'yes'
```

### Destroy Specific Environment

```bash
cd deploy
terraform destroy -var-file="environments/dev.tfvars"
```

## Next Steps

1. **Custom Domain**: Add custom domain to Container Apps
2. **CDN**: Add Azure CDN for static assets
3. **Database**: Add Azure Database for PostgreSQL/MySQL if needed
4. **Authentication**: Integrate Azure AD or Auth0
5. **WAF**: Add Web Application Firewall for security

## Additional Resources

- [Azure Container Apps Documentation](https://learn.microsoft.com/en-us/azure/container-apps/)
- [Terraform Azure Provider](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs)
- [Azure Pricing Calculator](https://azure.microsoft.com/en-us/pricing/calculator/)
