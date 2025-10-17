# Azure Container Apps Deployment - Complete Setup

This document summarizes the complete deployment infrastructure created for OSCAL CLI on Azure.

## Deployment Approach

**Recommended Platform:** Azure Container Apps (selected for optimal balance of simplicity and scalability)

**Key Benefits:**
- Auto-scaling from 0 to 300+ instances
- Serverless architecture (pay only for what you use)
- Built-in HTTPS, load balancing, and health monitoring
- No infrastructure management required
- Can scale to zero during off-hours (cost savings)

## Files Created

### Terraform Configuration

```
deploy/
├── providers.tf              # Azure provider configuration
├── variables.tf              # All configurable variables (70+ options)
├── main.tf                   # Core infrastructure resources
├── outputs.tf                # Deployment outputs and URLs
├── terraform.tfvars.example  # Configuration template
├── .gitignore               # Exclude sensitive files
└── environments/
    ├── dev.tfvars           # Development configuration
    └── prod.tfvars          # Production configuration
```

### Documentation

```
deploy/
├── README.md                 # Comprehensive deployment guide (400+ lines)
├── QUICKSTART.md            # 10-minute quick start guide
└── DEPLOYMENT_SUMMARY.md    # This file
```

### Automation

```
deploy/
├── deploy.sh                # Automated deployment script
└── .github/workflows/
    └── azure-deploy.yml     # GitHub Actions CI/CD pipeline
```

## Infrastructure Resources

The Terraform configuration creates:

1. **Resource Group** - Container for all Azure resources
2. **Container Registry (ACR)** - Stores Docker images
3. **Log Analytics Workspace** - Centralized logging
4. **Application Insights** - Monitoring and telemetry
5. **Container Apps Environment** - Hosting environment
6. **Container App(s)** - Your application (combined or separate mode)
7. **Storage Account** (optional) - For file uploads

## Deployment Modes

### Combined Mode (Default for Dev)
- Single container running both frontend + backend
- Simplest deployment
- Best for: dev, staging, small workloads
- Cost: ~$25-50/month

### Separate Mode (Recommended for Prod)
- Two containers: frontend and backend
- Independent scaling
- Best for: production, high traffic
- Cost: ~$150-250/month

## Configuration Highlights

### Auto-Scaling
- HTTP request-based scaling (default: 10 concurrent requests)
- CPU-based scaling (default: 70% threshold)
- Memory-based scaling (optional)
- Scale to zero in dev environments

### Resource Allocation
- Dev: 0.5 vCPU, 1Gi RAM
- Prod Frontend: 0.5 vCPU, 1Gi RAM
- Prod Backend: 1 vCPU, 2Gi RAM

### High Availability (Prod)
- Minimum 2 replicas for frontend
- Minimum 2 replicas for backend
- Geo-redundant storage
- Application Insights enabled

## Quick Deployment Commands

### Option 1: Automated Script
```bash
cd deploy
chmod +x deploy.sh
./deploy.sh dev apply        # Deploy to dev
./deploy.sh prod apply       # Deploy to prod
```

### Option 2: Terraform Direct
```bash
cd deploy
terraform init
terraform apply -var-file="environments/dev.tfvars"
```

### Option 3: GitHub Actions
- Push to `main` branch → deploys to prod
- Push to `develop` branch → deploys to dev
- Manual trigger via GitHub UI for any environment

## Post-Deployment Steps

After running the deployment, you'll get:

```bash
Outputs:

application_url = "https://ca-oscal-cli-dev.xxx.azurecontainerapps.io"
api_url = "https://ca-oscal-cli-dev.xxx.azurecontainerapps.io/api"
container_registry_name = "acroscalclidev"
```

## Deployment Workflow

1. **Infrastructure** - Terraform creates Azure resources (~3-5 min)
2. **Docker Image** - Build and push to ACR (~5-10 min)
3. **Container App** - Deploy image to Container Apps (~2-3 min)
4. **Total Time** - ~10-20 minutes for first deployment

Subsequent deployments (image updates only): ~3-5 minutes

## Cost Estimation

### Development Environment
| Resource | Configuration | Monthly Cost |
|----------|--------------|--------------|
| Container App | 0.5 vCPU, 1Gi, scale to zero, 8 hrs/day | ~$15 |
| Container Registry | Basic tier | ~$5 |
| Log Analytics | Minimal logs | ~$5 |
| **Total** | | **~$25/month** |

### Production Environment
| Resource | Configuration | Monthly Cost |
|----------|--------------|--------------|
| Frontend App | 0.5 vCPU, 1Gi, 2-10 replicas | ~$40 |
| Backend App | 1 vCPU, 2Gi, 2-20 replicas | ~$100 |
| Container Registry | Standard tier | ~$20 |
| Monitoring | App Insights + Logs | ~$30 |
| Storage | Geo-redundant | ~$5 |
| **Total** | | **~$195/month** |

*Actual costs vary based on traffic and scaling*

## Monitoring and Observability

- **Application Insights** - Request tracking, dependency analysis, exceptions
- **Log Analytics** - Centralized logging, custom queries
- **Container Logs** - Real-time application logs
- **Health Checks** - Built-in health monitoring
- **Metrics** - CPU, memory, request count, response time

## Security Features

- HTTPS enforced by default
- Azure AD authentication ready
- Secrets stored in Azure Key Vault (configurable)
- Private ACR with managed identity
- Network isolation options available
- CORS configuration

## Scaling Capabilities

### Automatic Scaling
- Scales based on HTTP traffic load
- Scales based on CPU/memory usage
- Can handle traffic spikes automatically
- Zero-downtime deployments

### Manual Scaling
```bash
az containerapp update \
  --name ca-oscal-cli-dev \
  --resource-group rg-oscal-cli-dev \
  --min-replicas 3 \
  --max-replicas 20
```

## CI/CD Pipeline

GitHub Actions workflow includes:
1. **Build & Test** - Compile backend, build frontend, run tests
2. **Docker Build** - Multi-stage build, push to ACR
3. **Infrastructure** - Terraform plan and apply
4. **Deploy App** - Update container apps with new image
5. **Smoke Tests** - Basic health checks

## Environment Variables

Automatically configured:
- `JAVA_OPTS` - JVM settings
- `NODE_ENV` - Node environment
- `NEXT_PUBLIC_API_URL` - API endpoint
- `APPLICATIONINSIGHTS_CONNECTION_STRING` - Monitoring

## Troubleshooting Commands

```bash
# View logs
az containerapp logs show --name ca-oscal-cli-dev --resource-group rg-oscal-cli-dev --tail 100

# Check status
az containerapp show --name ca-oscal-cli-dev --resource-group rg-oscal-cli-dev

# List revisions
az containerapp revision list --name ca-oscal-cli-dev --resource-group rg-oscal-cli-dev

# Get application URL
cd deploy && terraform output application_url
```

## Next Steps

1. **Review Configuration** - Check `deploy/terraform.tfvars.example`
2. **Deploy to Dev** - Run `./deploy.sh dev apply`
3. **Test Application** - Access the provided URL
4. **Set Up CI/CD** - Configure GitHub secrets for automated deployments
5. **Configure Monitoring** - Set up alerts in Application Insights
6. **Plan Production** - Review `environments/prod.tfvars` and adjust as needed

## Support Resources

- **Detailed Guide**: [deploy/README.md](README.md)
- **Quick Start**: [deploy/QUICKSTART.md](QUICKSTART.md)
- **Azure Docs**: [Azure Container Apps Documentation](https://learn.microsoft.com/en-us/azure/container-apps/)
- **Terraform Docs**: [Azure Provider Documentation](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs)

## Clean Up

To delete all resources:

```bash
cd deploy
./deploy.sh dev destroy
```

Or manually:

```bash
cd deploy
terraform destroy -var-file="environments/dev.tfvars"
```

---

**Created:** 2025-10-17
**Platform:** Azure Container Apps
**IaC Tool:** Terraform v1.5+
**Container Runtime:** Docker
**Target Framework:** Spring Boot + Next.js
