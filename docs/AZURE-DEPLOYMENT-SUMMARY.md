# Azure Deployment Setup - Quick Start Summary

**Date**: 2025-10-26
**Status**: ✅ Complete - Ready for Deployment

---

## Overview

This document provides a quick summary of the Azure deployment setup for OSCAL Tools. All necessary infrastructure-as-code, CI/CD pipelines, and configuration have been created and are ready to use.

---

## What Was Created

### 1. Documentation

✅ **`docs/AZURE-DEPLOYMENT-GUIDE.md`**
   - Comprehensive 900+ line deployment guide
   - Step-by-step Azure setup instructions
   - GitHub repository configuration guide
   - Troubleshooting and maintenance procedures

### 2. Terraform Infrastructure (`terraform/`)

✅ **Complete Terraform configuration for Azure resources:**

| File | Purpose |
|------|---------|
| `providers.tf` | Azure provider configuration |
| `variables.tf` | Input variables (customizable) |
| `main.tf` | Resource group and naming |
| `acr.tf` | Azure Container Registry for Docker images |
| `database.tf` | PostgreSQL Flexible Server (production database) |
| `keyvault.tf` | Azure Key Vault for secure secrets storage |
| `container-instance.tf` | Azure Container Instance (application hosting) |
| `monitoring.tf` | Application Insights (monitoring & logging) |
| `outputs.tf` | Output values after deployment |
| `terraform.tfvars.example` | Example configuration (copy and customize) |
| `.gitignore` | Prevents committing sensitive files |
| `README.md` | Terraform usage guide |

**Resources Created by Terraform:**
- Resource Group
- Azure Container Registry (ACR)
- PostgreSQL Flexible Server (v15)
- Azure Key Vault (with secrets)
- Azure Container Instance (2 vCPU, 4GB RAM)
- Application Insights (optional monitoring)

### 3. GitHub Actions Workflows (`.github/workflows/`)

✅ **Three automated CI/CD workflows:**

#### `ci.yml` - Continuous Integration
- **Triggers**: On pull request to `main`
- **Actions**:
  - Build backend (Maven + Java 21)
  - Build frontend (npm + Node 18)
  - Run tests (backend + frontend)
  - Security scanning (Trivy + OWASP Dependency Check)
  - Upload security reports to GitHub Security tab

#### `deploy.yml` - Continuous Deployment
- **Triggers**: On merge to `main` (automatic) or manual trigger
- **Actions**:
  1. Build Docker image
  2. Push to Azure Container Registry
  3. Scan image with Trivy
  4. Deploy to Azure Container Instance
  5. Run database migrations (Flyway)
  6. Health checks (backend + frontend)
  7. Cleanup old images (keep last 5)

#### `terraform.yml` - Infrastructure Management
- **Triggers**: Manual only (workflow_dispatch)
- **Actions**:
  - `plan` - Show infrastructure changes
  - `apply` - Create/update Azure resources
  - `destroy` - Delete all resources (DANGER!)

### 4. Database Migration Support

✅ **Flyway Configuration:**
- Added `flyway-core` and `flyway-database-postgresql` dependencies to `back-end/pom.xml`
- Configured Flyway in `application.properties`
- Migrations located in `back-end/src/main/resources/db/migration/`
- Existing migration: `V1.5__add_digital_signature_fields.sql`

✅ **Updated `docker-entrypoint.sh`:**
- Checks database connectivity before starting
- Waits for database to be ready (60s timeout)
- Extended backend startup timeout for migration time
- Better logging and error handling
- Displays migration status on startup

---

## Deployment Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                      GitHub Repository                          │
│                                                                 │
│  Developer → PR → Code Review → Merge to main                  │
│                                      ↓                          │
│                            GitHub Actions Triggers              │
└────────────────────────────┬────────────────────────────────────┘
                             │
                             ↓
┌─────────────────────────────────────────────────────────────────┐
│                         Azure Cloud                             │
│                                                                 │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │ Azure Container Registry (ACR)                           │  │
│  │   → Stores Docker images                                 │  │
│  └────────────────┬─────────────────────────────────────────┘  │
│                   │                                             │
│                   ↓                                             │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │ Azure Container Instance                                 │  │
│  │   ┌────────────┐     ┌──────────────────────┐           │  │
│  │   │ Frontend   │ ←─→ │ Backend              │           │  │
│  │   │ Next.js    │     │ Spring Boot          │           │  │
│  │   │ Port 3000  │     │ Port 8080            │           │  │
│  │   └────────────┘     └──────────┬───────────┘           │  │
│  └──────────────────────────────────┼────────────────────────┘  │
│                                     │                           │
│                                     ↓                           │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │ PostgreSQL Flexible Server                               │  │
│  │   Database: oscal_production                             │  │
│  │   Flyway: Auto-applies migrations on startup             │  │
│  └──────────────────────────────────────────────────────────┘  │
│                                                                 │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │ Azure Key Vault                                          │  │
│  │   Secrets: JWT_SECRET, DB_PASSWORD, CORS, etc.          │  │
│  └──────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
```

---

## Quick Start Guide

### Step 1: Azure Setup (One-Time)

```bash
# 1. Login to Azure
az login
az account set --subscription "Your Subscription Name"

# 2. Create service principal for GitHub Actions
export AZURE_SUBSCRIPTION_ID=$(az account show --query id -o tsv)
export SP_NAME="oscal-tools-github-actions"

az ad sp create-for-rbac \
  --name "$SP_NAME" \
  --role Contributor \
  --scopes /subscriptions/$AZURE_SUBSCRIPTION_ID \
  --sdk-auth \
  --output json > azure-credentials.json

# ⚠️ SAVE THIS FILE SECURELY! You'll need it for GitHub Secrets
# ⚠️ DO NOT COMMIT THIS FILE TO VERSION CONTROL!
```

### Step 2: GitHub Secrets (One-Time)

Navigate to your GitHub repository → **Settings** → **Secrets and variables** → **Actions**

Add these secrets:

1. **`AZURE_CREDENTIALS`** - Contents of `azure-credentials.json`
2. **`AZURE_SUBSCRIPTION_ID`** - Your Azure subscription ID
3. **`AZURE_TENANT_ID`** - Your Azure tenant ID
4. **`JWT_SECRET`** - Generate: `openssl rand -base64 64 | tr -d '\n'`
5. **`DB_PASSWORD`** - Generate: `openssl rand -base64 32 | tr -d '\n'`
6. **`CORS_ALLOWED_ORIGINS`** - Your domain(s): `https://your-domain.com`

### Step 3: Protected Branch Setup (One-Time)

1. Navigate to **Settings** → **Branches** → **Add branch protection rule**
2. Branch name pattern: `main`
3. Enable:
   - ✅ Require a pull request before merging (1 approval)
   - ✅ Require status checks to pass (select `build-and-test`, `security-scan`)
   - ✅ Require conversation resolution before merging

### Step 4: Deploy Infrastructure (One-Time)

```bash
# 1. Configure Terraform variables
cd terraform
cp terraform.tfvars.example terraform.tfvars

# 2. Edit terraform.tfvars with your values
# IMPORTANT: Generate strong passwords!
vi terraform.tfvars

# 3. Initialize Terraform
terraform init

# 4. Preview infrastructure
terraform plan -out=tfplan

# 5. Create Azure resources
terraform apply tfplan

# 6. Note the outputs (ACR name, database connection, etc.)
terraform output deployment_summary
```

### Step 5: Build and Deploy Application

#### Option A: Automatic (Recommended)

1. Create a feature branch:
   ```bash
   git checkout -b feature/your-feature
   ```

2. Make your changes and commit:
   ```bash
   git add .
   git commit -m "Your commit message"
   git push origin feature/your-feature
   ```

3. Create a Pull Request on GitHub
4. Wait for CI checks to pass ✅
5. Get approval and merge to `main`
6. 🎉 **Automatic deployment happens!**

#### Option B: Manual

```bash
# 1. Get ACR name from Terraform
ACR_NAME=$(cd terraform && terraform output -raw acr_name)

# 2. Login to ACR
az acr login --name $ACR_NAME

# 3. Build Docker image
docker build -t oscal-ux:latest .

# 4. Tag for ACR
docker tag oscal-ux:latest $ACR_NAME.azurecr.io/oscal-ux:latest

# 5. Push to ACR
docker push $ACR_NAME.azurecr.io/oscal-ux:latest

# 6. Restart container instance
az container restart \
  --resource-group oscal-tools-prod \
  --name oscal-ux
```

---

## Deployment Workflow

### When you merge a PR to `main`:

1. ✅ **Build** - Docker image is built
2. ✅ **Push** - Image pushed to Azure Container Registry
3. ✅ **Scan** - Security scan with Trivy
4. ✅ **Deploy** - Container instance updated with new image
5. ✅ **Migrate** - Flyway runs database migrations automatically
6. ✅ **Health Check** - Backend and frontend health checks
7. ✅ **Cleanup** - Old images deleted (keep last 5)

Total deployment time: **3-5 minutes**

---

## Database Migrations

### How Migrations Work

1. Migration files are in `back-end/src/main/resources/db/migration/`
2. Name format: `V{version}__{description}.sql`
   - Example: `V1.5__add_digital_signature_fields.sql`
3. Flyway runs automatically when container starts
4. Only new migrations are applied
5. Migration history tracked in `flyway_schema_history` table

### Adding a New Migration

```bash
# 1. Create new migration file
cd back-end/src/main/resources/db/migration
vi V1.6__your_migration_description.sql

# 2. Write your SQL
-- V1.6: Add new feature
-- Date: 2025-10-26

ALTER TABLE users ADD COLUMN new_field VARCHAR(255);
CREATE INDEX idx_users_new_field ON users(new_field);

# 3. Commit and push
git add .
git commit -m "Add database migration for new feature"
git push

# 4. Create PR, merge to main
# Migration runs automatically on deployment ✅
```

---

## Monitoring and Logs

### View Container Logs

```bash
# Via Azure CLI
az container logs \
  --resource-group oscal-tools-prod \
  --name oscal-ux \
  --follow

# Via Azure Portal
# Navigate to: Container Instances → oscal-ux → Logs
```

### Application Insights

If enabled (`enable_monitoring = true`):
- Navigate to Azure Portal → Application Insights → `oscal-insights-xxxxx`
- View:
  - Live Metrics
  - Performance
  - Failures
  - Logs (Kusto queries)

### Health Checks

```bash
# Get container URL
FQDN=$(az container show \
  --resource-group oscal-tools-prod \
  --name oscal-ux \
  --query "ipAddress.fqdn" -o tsv)

# Check backend health
curl http://$FQDN:8080/api/health

# Check frontend
curl -I http://$FQDN:3000
```

---

## Cost Estimation

Approximate monthly Azure costs:

| Service | Configuration | Cost/Month |
|---------|---------------|------------|
| Azure Container Instance | 2 vCPU, 4GB RAM | $60 |
| PostgreSQL Flexible Server | B1ms (1 vCore, 2GB) | $20 |
| Azure Container Registry | Basic | $5 |
| Azure Key Vault | Standard, ~10 secrets | $1 |
| Application Insights | ~5GB ingestion | $15 |
| Bandwidth | ~50GB egress | $5 |
| **Total** | | **~$106/month** |

**Cost Optimization Tips:**
- Scale down container resources if needed (1 vCPU, 2GB = ~$30/month)
- Use reserved instances for 1-3 year commitments (save up to 72%)
- Disable Application Insights if not needed (save $15/month)
- Set auto-shutdown schedules for non-production environments

---

## Troubleshooting

### Issue: GitHub Actions fails with authentication error

**Solution**: Verify `AZURE_CREDENTIALS` secret is correct
```bash
# Re-create service principal
az ad sp create-for-rbac --name oscal-tools-github-actions --role Contributor --scopes /subscriptions/$AZURE_SUBSCRIPTION_ID --sdk-auth
```

### Issue: Container fails to start

**Check logs**:
```bash
az container logs --resource-group oscal-tools-prod --name oscal-ux
```

**Common causes**:
- Database connection failed → Check database firewall rules
- Secrets missing → Verify Key Vault secrets exist
- Migration failed → Check migration SQL syntax

### Issue: Database migration error

**View migration history**:
```sql
-- Connect to database
psql -h your-db-server.postgres.database.azure.com -U oscaladmin -d oscal_production

-- Check migration history
SELECT * FROM flyway_schema_history ORDER BY installed_rank DESC LIMIT 10;

-- If migration failed, fix and re-deploy
```

### Issue: 403 Forbidden errors after deployment

**Solution**: Users need to log in again
- Backend restart invalidates JWT tokens
- Hard refresh browser (Cmd/Ctrl + Shift + R)
- Log out and log back in

---

## Security Checklist

✅ **Secrets Management**
- All secrets stored in Azure Key Vault
- No hardcoded passwords in code
- GitHub Secrets for CI/CD authentication

✅ **Network Security**
- PostgreSQL firewall rules restrict access
- HTTPS required in production
- CORS configured for specific domains

✅ **Container Security**
- Non-root user (oscaluser)
- Security scanning with Trivy
- OWASP dependency checks
- No privileged containers

✅ **Database Security**
- SSL/TLS required for connections
- Strong passwords (32+ characters)
- Regular automated backups (7 days)
- SCRAM-SHA-256 authentication

✅ **Application Security**
- JWT authentication required
- Rate limiting enabled
- Account lockout after failed attempts
- Audit logging enabled
- Security headers configured

---

## Maintenance Tasks

### Monthly
- Review Azure costs
- Check for dependency updates
- Review security scan results
- Verify backup status

### Quarterly
- Run security audit
- Review access permissions
- Update SSL/TLS certificates (if custom domain)
- Disaster recovery test

### Annual
- Review architecture for optimization
- Update documentation
- Compliance audit

---

## Next Steps

Now that everything is set up:

1. **Test the deployment locally**:
   ```bash
   # Build and run locally to verify
   docker build -t oscal-ux:test .
   docker run -p 3000:3000 -p 8080:8080 \
     -e SPRING_PROFILES_ACTIVE=dev \
     -e JWT_SECRET=test-secret \
     -e DB_URL=jdbc:postgresql://localhost:5432/oscal_dev \
     oscal-ux:test
   ```

2. **Deploy to Azure** (follow Quick Start Guide above)

3. **Verify deployment**:
   - Visit application URL
   - Register a test account
   - Test OSCAL validation/conversion
   - Check database migrations applied

4. **Set up monitoring alerts** (optional):
   - High error rate
   - High memory usage
   - SSL certificate expiration

5. **Configure custom domain** (optional):
   - Set up Azure Front Door or Application Gateway
   - Configure SSL certificate
   - Update CORS settings

---

## Files Summary

### Created Files

```
oscal-cli/
├── docs/
│   ├── AZURE-DEPLOYMENT-GUIDE.md (920 lines) ✅
│   └── AZURE-DEPLOYMENT-SUMMARY.md (this file) ✅
├── terraform/
│   ├── providers.tf ✅
│   ├── variables.tf ✅
│   ├── main.tf ✅
│   ├── acr.tf ✅
│   ├── database.tf ✅
│   ├── keyvault.tf ✅
│   ├── container-instance.tf ✅
│   ├── monitoring.tf ✅
│   ├── outputs.tf ✅
│   ├── terraform.tfvars.example ✅
│   ├── .gitignore ✅
│   └── README.md ✅
├── .github/
│   └── workflows/
│       ├── ci.yml (CI pipeline) ✅
│       ├── deploy.yml (CD pipeline) ✅
│       └── terraform.yml (Infrastructure management) ✅
└── docker-entrypoint.sh (Updated for migrations) ✅
```

### Modified Files

```
oscal-cli/
├── back-end/
│   ├── pom.xml (Added Flyway dependencies) ✅
│   └── src/main/resources/
│       └── application.properties (Added Flyway config) ✅
└── docker-entrypoint.sh (Enhanced for DB migrations) ✅
```

---

## Support and Resources

### Documentation
- **Main Guide**: `docs/AZURE-DEPLOYMENT-GUIDE.md` (detailed step-by-step)
- **This Summary**: `docs/AZURE-DEPLOYMENT-SUMMARY.md` (quick reference)
- **Terraform README**: `terraform/README.md` (Terraform usage)
- **Project README**: `README.md` (application overview)

### External Resources
- [Azure Container Instances](https://docs.microsoft.com/en-us/azure/container-instances/)
- [Terraform Azure Provider](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs)
- [Flyway Documentation](https://flywaydb.org/documentation/)
- [GitHub Actions](https://docs.github.com/en/actions)

### Getting Help
- GitHub Issues: https://github.com/usnistgov/oscal-cli/issues
- Azure Support: https://azure.microsoft.com/support/

---

## Checklist for First Deployment

Before your first deployment, complete these steps:

- [ ] Azure subscription active
- [ ] Azure CLI installed and logged in
- [ ] Service principal created (Step 1)
- [ ] GitHub secrets configured (Step 2)
- [ ] Protected branch rules set (Step 3)
- [ ] `terraform.tfvars` configured
- [ ] Infrastructure deployed with Terraform (Step 4)
- [ ] Test PR created and merged
- [ ] Application accessible at Azure URL
- [ ] Database migrations applied successfully
- [ ] Health checks passing

---

**🎉 Congratulations!** Your OSCAL Tools application is now production-ready with automated CI/CD deployment to Azure!

For detailed instructions, troubleshooting, and advanced topics, see:
**`docs/AZURE-DEPLOYMENT-GUIDE.md`**

---

**Document Version**: 1.0.0
**Last Updated**: 2025-10-26
**Status**: ✅ Ready for Production

---

**End of Summary**
