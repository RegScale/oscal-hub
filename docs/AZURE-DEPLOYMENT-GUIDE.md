# Azure Deployment Guide - OSCAL Tools

**Version**: 1.0.0
**Date**: 2025-10-26
**Purpose**: Complete guide for deploying OSCAL Tools to Azure using CI/CD, Terraform, and Key Vault

---

## Table of Contents

1. [Overview](#overview)
2. [Architecture](#architecture)
3. [Prerequisites](#prerequisites)
4. [Part 1: Azure Setup](#part-1-azure-setup)
5. [Part 2: GitHub Repository Setup](#part-2-github-repository-setup)
6. [Part 3: Terraform Infrastructure](#part-3-terraform-infrastructure)
7. [Part 4: CI/CD Pipeline](#part-4-cicd-pipeline)
8. [Part 5: Database Migrations](#part-5-database-migrations)
9. [Part 6: Deployment Process](#part-6-deployment-process)
10. [Troubleshooting](#troubleshooting)
11. [Maintenance](#maintenance)

---

## Overview

This guide walks you through deploying the OSCAL Tools full-stack application to **Azure** with:

- **Infrastructure as Code**: Terraform manages all Azure resources
- **CI/CD Automation**: GitHub Actions for build, test, and deploy
- **Secure Configuration**: Azure Key Vault for secrets management
- **Database Management**: Automatic Flyway migrations on deployment
- **Container Registry**: Azure Container Registry (ACR) for Docker images
- **Web Hosting**: Azure Container Instances or App Service
- **Branch Protection**: Protected `main` branch with PR workflow

### Deployment Flow

```
Developer → PR to main → Approval → Merge
                                     ↓
                         CI/CD Pipeline Triggers
                                     ↓
                    ┌────────────────┴────────────────┐
                    ↓                                 ↓
              Build & Test                    Terraform Apply
                    ↓                                 ↓
            Build Docker Image              Create/Update Azure
                    ↓                         Resources (if needed)
            Push to ACR                              ↓
                    ↓                                 ↓
            Deploy to Azure ←────────────────────────┘
                    ↓
            Run Database Migrations
                    ↓
            Health Check & Smoke Tests
                    ↓
            Production Ready ✓
```

---

## Architecture

### Azure Resources

The deployment creates the following Azure resources:

1. **Resource Group** - Container for all resources
2. **Azure Container Registry (ACR)** - Private Docker image registry
3. **Azure Database for PostgreSQL** - Managed database service
4. **Azure Key Vault** - Secure secrets storage
5. **Azure Container Instances (ACI)** - Container hosting (or App Service as alternative)
6. **Virtual Network (VNet)** - Secure network isolation
7. **Application Insights** - Monitoring and diagnostics

### Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│                        GitHub Repository                         │
│  ┌──────────────┐                           ┌─────────────────┐ │
│  │    Code      │ ──PR Merge to main──→     │ GitHub Actions  │ │
│  │  (main)      │                           │   Workflows     │ │
│  └──────────────┘                           └────────┬────────┘ │
└───────────────────────────────────────────────────────┼──────────┘
                                                        │
                                                        ↓
┌─────────────────────────────────────────────────────────────────┐
│                         Azure Cloud                             │
│                                                                 │
│  ┌─────────────────────────────────────────────────────────┐  │
│  │                  Resource Group                          │  │
│  │                                                          │  │
│  │  ┌──────────────────┐       ┌───────────────────────┐  │  │
│  │  │ Azure Container  │       │   Azure Key Vault     │  │  │
│  │  │   Registry       │       │                       │  │  │
│  │  │  (ACR)           │       │ • JWT_SECRET          │  │  │
│  │  │                  │       │ • DB_PASSWORD         │  │  │
│  │  │ oscalcr.azurecr  │       │ • CORS origins        │  │  │
│  │  └──────────────────┘       │ • Other secrets       │  │  │
│  │           │                 └───────────┬───────────┘  │  │
│  │           │                             │              │  │
│  │           ↓                             ↓              │  │
│  │  ┌──────────────────────────────────────────────────┐ │  │
│  │  │    Azure Container Instance / App Service        │ │  │
│  │  │                                                   │ │  │
│  │  │  ┌──────────────┐     ┌────────────────────┐    │ │  │
│  │  │  │   Frontend   │     │     Backend        │    │ │  │
│  │  │  │   Next.js    │     │   Spring Boot      │    │ │  │
│  │  │  │   (Port 3000)│ ←──→│   (Port 8080)      │    │ │  │
│  │  │  └──────────────┘     └─────────┬──────────┘    │ │  │
│  │  │                                  │               │ │  │
│  │  └──────────────────────────────────┼───────────────┘ │  │
│  │                                     │                 │  │
│  │                                     ↓                 │  │
│  │  ┌─────────────────────────────────────────────────┐ │  │
│  │  │   Azure Database for PostgreSQL (Flexible)      │ │  │
│  │  │                                                  │ │  │
│  │  │   Database: oscal_production                    │ │  │
│  │  │   User: oscal_user                              │ │  │
│  │  │   SSL: Required                                 │ │  │
│  │  │   Automatic Backups: Enabled                    │ │  │
│  │  └─────────────────────────────────────────────────┘ │  │
│  │                                                       │  │
│  │  ┌─────────────────────────────────────────────────┐ │  │
│  │  │        Application Insights                      │ │  │
│  │  │        (Monitoring & Logging)                    │ │  │
│  │  └─────────────────────────────────────────────────┘ │  │
│  │                                                       │  │
│  └───────────────────────────────────────────────────────┘  │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

---

## Prerequisites

### Required Tools

Install these tools on your local machine:

1. **Azure CLI** (version 2.50+)
   ```bash
   # macOS
   brew install azure-cli

   # Windows
   winget install Microsoft.AzureCLI

   # Linux
   curl -sL https://aka.ms/InstallAzureCLIDeb | sudo bash

   # Verify
   az --version
   ```

2. **Terraform** (version 1.5+)
   ```bash
   # macOS
   brew install terraform

   # Windows
   winget install Hashicorp.Terraform

   # Linux
   wget -O- https://apt.releases.hashicorp.com/gpg | sudo gpg --dearmor -o /usr/share/keyrings/hashicorp-archive-keyring.gpg
   echo "deb [signed-by=/usr/share/keyrings/hashicorp-archive-keyring.gpg] https://apt.releases.hashicorp.com $(lsb_release -cs) main" | sudo tee /etc/apt/sources.list.d/hashicorp.list
   sudo apt update && sudo apt install terraform

   # Verify
   terraform --version
   ```

3. **GitHub CLI** (optional but recommended)
   ```bash
   # macOS
   brew install gh

   # Windows
   winget install GitHub.cli

   # Linux
   sudo apt install gh

   # Verify
   gh --version
   ```

### Required Accounts

- **Azure Account** with sufficient permissions to create:
  - Resource Groups
  - Container Registries
  - PostgreSQL Databases
  - Key Vaults
  - Container Instances / App Service
  - Service Principals

- **GitHub Account** with admin access to the repository

### Azure Subscription

You need an active Azure subscription. If you don't have one:

1. Sign up for a free account: https://azure.microsoft.com/free/
2. Free tier includes:
   - $200 credit for 30 days
   - 12 months of free services
   - 25+ always-free services

---

## Part 1: Azure Setup

This section covers setting up all required Azure resources before deployment.

### Step 1: Login to Azure

```bash
# Login to Azure
az login

# If you have multiple subscriptions, list them
az account list --output table

# Set the subscription you want to use
az account set --subscription "Your Subscription Name"

# Verify the correct subscription is active
az account show --output table
```

### Step 2: Create a Service Principal

The service principal is used by GitHub Actions to authenticate with Azure.

```bash
# Set variables (customize these)
export AZURE_SUBSCRIPTION_ID=$(az account show --query id -o tsv)
export SP_NAME="oscal-tools-github-actions"
export RESOURCE_GROUP_NAME="oscal-tools-prod"

# Create service principal with Contributor role
az ad sp create-for-rbac \
  --name "$SP_NAME" \
  --role Contributor \
  --scopes /subscriptions/$AZURE_SUBSCRIPTION_ID/resourceGroups/$RESOURCE_GROUP_NAME \
  --sdk-auth \
  --output json > azure-credentials.json

# IMPORTANT: Save this file securely! You'll need it for GitHub Secrets
# The output will look like:
# {
#   "clientId": "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx",
#   "clientSecret": "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx",
#   "subscriptionId": "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx",
#   "tenantId": "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx",
#   ...
# }
```

**⚠️ CRITICAL**:
- The `azure-credentials.json` file contains sensitive credentials
- Store it in a password manager or secure vault
- **NEVER commit this file to version control**
- Add it to `.gitignore` immediately

### Step 3: Grant Additional Permissions

The service principal needs permissions to create Key Vault secrets and read from ACR.

```bash
# Get the service principal Object ID
SP_OBJECT_ID=$(az ad sp list --display-name "$SP_NAME" --query [0].id -o tsv)

# Note: This step will be handled by Terraform, but document it here
# Key Vault permissions are set in Terraform (access policies)
```

### Step 4: Create Resource Group (Optional - Terraform will do this)

You can create the resource group manually or let Terraform create it.

```bash
# Choose a location (region)
# List available regions:
az account list-locations --output table

# Common regions:
# - eastus
# - westus2
# - centralus
# - westeurope
# - eastasia

# Create resource group
az group create \
  --name "$RESOURCE_GROUP_NAME" \
  --location "eastus"
```

### Step 5: Save Azure Configuration

Create a file to store your Azure configuration values (for reference):

```bash
# Create azure-config.txt (don't commit this!)
cat > azure-config.txt <<EOF
# Azure Configuration
# DO NOT COMMIT THIS FILE

AZURE_SUBSCRIPTION_ID=$AZURE_SUBSCRIPTION_ID
AZURE_TENANT_ID=$(az account show --query tenantId -o tsv)
SERVICE_PRINCIPAL_NAME=$SP_NAME
RESOURCE_GROUP_NAME=$RESOURCE_GROUP_NAME
LOCATION=eastus
PROJECT_NAME=oscal-tools
ENVIRONMENT=prod

# These will be created by Terraform:
# - ACR Name: oscalcr<unique-suffix>
# - PostgreSQL Server: oscal-db-<unique-suffix>
# - Key Vault: oscal-kv-<unique-suffix>
# - Container Instance: oscal-ux
EOF

# Add to .gitignore
echo "azure-config.txt" >> .gitignore
echo "azure-credentials.json" >> .gitignore
```

---

## Part 2: GitHub Repository Setup

This section covers configuring your GitHub repository for protected branches and CI/CD.

### Step 1: Create Protected Branch (main)

You want to protect the `main` branch and require PRs for all changes.

#### Option A: Using GitHub Web UI

1. Navigate to your repository on GitHub
2. Go to **Settings** → **Branches**
3. Click **Add branch protection rule**
4. Configure the rule:

   **Branch name pattern**: `main`

   **Protect matching branches**:
   - ✅ **Require a pull request before merging**
     - ✅ Require approvals: **1** (or more)
     - ✅ Dismiss stale pull request approvals when new commits are pushed
     - ✅ Require review from Code Owners (optional)
   - ✅ **Require status checks to pass before merging**
     - ✅ Require branches to be up to date before merging
     - Search for and add status checks:
       - `build-and-test`
       - `security-scan`
   - ✅ **Require conversation resolution before merging**
   - ✅ **Do not allow bypassing the above settings** (recommended)
   - ✅ **Restrict who can push to matching branches** (optional - only allow admins)

5. Click **Create** to save the rule

#### Option B: Using GitHub CLI

```bash
# Set repository (format: owner/repo)
export GITHUB_REPO="your-username/oscal-cli"

# Create branch protection rule
gh api repos/$GITHUB_REPO/branches/main/protection \
  --method PUT \
  --field required_pull_request_reviews[required_approving_review_count]=1 \
  --field required_pull_request_reviews[dismiss_stale_reviews]=true \
  --field required_status_checks[strict]=true \
  --field required_status_checks[][contexts][]=build-and-test \
  --field required_status_checks[][contexts][]=security-scan \
  --field enforce_admins=true \
  --field required_conversation_resolution=true
```

### Step 2: Add GitHub Secrets

GitHub Actions needs access to Azure credentials and other secrets.

#### Required Secrets

Navigate to **Settings** → **Secrets and variables** → **Actions** → **New repository secret**

Add the following secrets:

1. **`AZURE_CREDENTIALS`**
   - Value: Contents of `azure-credentials.json` file (the entire JSON object)
   ```json
   {
     "clientId": "...",
     "clientSecret": "...",
     "subscriptionId": "...",
     "tenantId": "...",
     "activeDirectoryEndpointUrl": "...",
     "resourceManagerEndpointUrl": "...",
     "activeDirectoryGraphResourceId": "...",
     "sqlManagementEndpointUrl": "...",
     "galleryEndpointUrl": "...",
     "managementEndpointUrl": "..."
   }
   ```

2. **`AZURE_SUBSCRIPTION_ID`**
   - Value: Your Azure subscription ID
   - Example: `12345678-1234-1234-1234-123456789abc`

3. **`AZURE_TENANT_ID`**
   - Value: Your Azure AD tenant ID
   - Example: `87654321-4321-4321-4321-cba987654321`

4. **`JWT_SECRET`**
   - Value: Generate a secure random string (64+ characters)
   ```bash
   # Generate JWT secret
   openssl rand -base64 64 | tr -d '\n'
   ```

5. **`DB_PASSWORD`**
   - Value: Generate a strong database password (32+ characters)
   ```bash
   # Generate DB password
   openssl rand -base64 32 | tr -d '\n'
   ```

6. **`CORS_ALLOWED_ORIGINS`**
   - Value: Your production domain(s)
   - Example: `https://oscal-tools.example.com,https://www.oscal-tools.example.com`

#### Using GitHub CLI to Add Secrets

```bash
# Set repository
export GITHUB_REPO="your-username/oscal-cli"

# Add AZURE_CREDENTIALS (from file)
gh secret set AZURE_CREDENTIALS < azure-credentials.json --repo $GITHUB_REPO

# Add AZURE_SUBSCRIPTION_ID
gh secret set AZURE_SUBSCRIPTION_ID --body "$AZURE_SUBSCRIPTION_ID" --repo $GITHUB_REPO

# Add AZURE_TENANT_ID
gh secret set AZURE_TENANT_ID --body "$(az account show --query tenantId -o tsv)" --repo $GITHUB_REPO

# Add JWT_SECRET
gh secret set JWT_SECRET --body "$(openssl rand -base64 64 | tr -d '\n')" --repo $GITHUB_REPO

# Add DB_PASSWORD
gh secret set DB_PASSWORD --body "$(openssl rand -base64 32 | tr -d '\n')" --repo $GITHUB_REPO

# Add CORS_ALLOWED_ORIGINS
gh secret set CORS_ALLOWED_ORIGINS --body "https://your-domain.com" --repo $GITHUB_REPO

# Verify secrets were added
gh secret list --repo $GITHUB_REPO
```

### Step 3: Add GitHub Variables (Optional)

Variables are like secrets but not encrypted (for non-sensitive config).

Navigate to **Settings** → **Secrets and variables** → **Actions** → **Variables** → **New repository variable**

Add these variables:

1. **`AZURE_RESOURCE_GROUP`**
   - Value: `oscal-tools-prod`

2. **`AZURE_LOCATION`**
   - Value: `eastus`

3. **`PROJECT_NAME`**
   - Value: `oscal-tools`

4. **`ENVIRONMENT`**
   - Value: `prod`

Or using CLI:

```bash
gh variable set AZURE_RESOURCE_GROUP --body "oscal-tools-prod" --repo $GITHUB_REPO
gh variable set AZURE_LOCATION --body "eastus" --repo $GITHUB_REPO
gh variable set PROJECT_NAME --body "oscal-tools" --repo $GITHUB_REPO
gh variable set ENVIRONMENT --body "prod" --repo $GITHUB_REPO
```

### Step 4: Configure Branch Settings

Make sure your default branch is set to `main`:

1. Go to **Settings** → **General**
2. Under **Default branch**, ensure `main` is selected
3. If you're currently on a different branch (like `howerton`), you'll want to:
   - Merge all your changes to `main`
   - Set `main` as the default branch

---

## Part 3: Terraform Infrastructure

Now we'll create Terraform configuration to manage all Azure resources.

### Directory Structure

Create a `terraform/` directory in your repository:

```
oscal-cli/
├── terraform/
│   ├── main.tf                 # Main Terraform configuration
│   ├── variables.tf            # Input variables
│   ├── outputs.tf              # Output values
│   ├── providers.tf            # Azure provider configuration
│   ├── acr.tf                  # Container Registry
│   ├── database.tf             # PostgreSQL Database
│   ├── keyvault.tf             # Key Vault
│   ├── container-instance.tf  # Container hosting
│   ├── monitoring.tf           # Application Insights
│   └── terraform.tfvars        # Variable values (DO NOT COMMIT!)
```

### Terraform Configuration Files

**These files will be created in the next step of the todo list.**

The Terraform configuration will create:

1. **Resource Group** - Container for all resources
2. **Azure Container Registry** - For Docker images
3. **Azure PostgreSQL Flexible Server** - Managed database
4. **Azure Key Vault** - Secure secrets storage
5. **Azure Container Instance** - Run the application
6. **Application Insights** - Monitoring and logging

---

## Part 4: CI/CD Pipeline

The GitHub Actions workflows will handle:

1. **Build and Test** - On every PR
2. **Security Scanning** - Trivy + OWASP Dependency Check
3. **Build and Push Docker Image** - On merge to main
4. **Deploy Infrastructure** - Terraform apply
5. **Deploy Application** - Update container instance
6. **Run Database Migrations** - Flyway migrations

### Workflow Files

**These files will be created in the next step of the todo list.**

Workflows to be created:

- `.github/workflows/ci.yml` - Build, test, and security scan
- `.github/workflows/deploy.yml` - Deploy to Azure on merge to main
- `.github/workflows/terraform.yml` - Terraform infrastructure management

---

## Part 5: Database Migrations

Database migrations are handled by **Flyway** (already configured in your project).

### How Migrations Work

1. Migration files are in `back-end/src/main/resources/db/migration/`
2. Naming convention: `V{version}__{description}.sql`
   - Example: `V1.5__add_digital_signature_fields.sql`
3. On container startup, Flyway checks for pending migrations
4. New migrations are applied automatically
5. Migration history is tracked in `flyway_schema_history` table

### Adding New Migrations

When you need to add a new database migration:

1. Create a new SQL file in `back-end/src/main/resources/db/migration/`
2. Name it with the next version number:
   ```
   V1.6__add_new_feature.sql
   V1.7__update_user_table.sql
   ```
3. Write your SQL migration:
   ```sql
   -- V1.6: Add new feature
   -- Date: 2025-10-26
   -- Description: Adds support for new feature

   ALTER TABLE users ADD COLUMN new_field VARCHAR(255);
   CREATE INDEX idx_users_new_field ON users(new_field);
   ```
4. Commit and push to your branch
5. When merged to main, the migration will run automatically on deployment

### Migration Best Practices

1. **Always use versioned migrations** (V1.x, V2.x, etc.)
2. **Never modify existing migrations** - Create a new one instead
3. **Test migrations locally** before pushing
4. **Write reversible migrations** (document rollback steps in comments)
5. **Keep migrations small** - One logical change per file
6. **Add indexes for performance** - After adding new columns

---

## Part 6: Deployment Process

Once everything is set up, here's how deployments work:

### Initial Deployment

```bash
# 1. Initialize Terraform
cd terraform
terraform init

# 2. Plan infrastructure changes
terraform plan -out=tfplan

# 3. Apply infrastructure (create Azure resources)
terraform apply tfplan

# 4. Note the outputs (ACR name, Key Vault name, etc.)
terraform output
```

### Continuous Deployment (CD)

After initial setup, every merge to `main` triggers automatic deployment:

```
┌─────────────────────────────────────────────────────────────┐
│  Developer Workflow                                         │
└─────────────────────────────────────────────────────────────┘

1. Create feature branch from main
   git checkout -b feature/new-feature

2. Make changes and commit
   git add .
   git commit -m "Add new feature"
   git push origin feature/new-feature

3. Create Pull Request on GitHub
   gh pr create --title "Add new feature" --body "Description"

4. CI runs automatically on PR:
   ✓ Build backend (Maven)
   ✓ Build frontend (npm)
   ✓ Run tests
   ✓ Security scan (Trivy + OWASP)
   ✓ Code quality checks

5. Request review from team member
   - Reviewer approves PR
   - All status checks pass (green)

6. Merge PR to main
   - Squash and merge (recommended)
   - Delete feature branch

7. CD pipeline triggers automatically:
   ┌─────────────────────────────────────────┐
   │  GitHub Actions Deployment Workflow      │
   └─────────────────────────────────────────┘

   Step 1: Build Docker Image
   ├─ Build multi-stage Docker image
   └─ Tag: oscalcr.azurecr.io/oscal-ux:latest

   Step 2: Push to Azure Container Registry
   ├─ Login to ACR
   ├─ Push image
   └─ Verify push succeeded

   Step 3: Update Azure Key Vault (if needed)
   ├─ Update JWT_SECRET
   ├─ Update DB_PASSWORD
   └─ Update other secrets

   Step 4: Deploy to Azure Container Instance
   ├─ Stop old container
   ├─ Create new container with latest image
   ├─ Set environment variables from Key Vault
   └─ Wait for container to be ready

   Step 5: Run Database Migrations
   ├─ Container starts
   ├─ Flyway checks for pending migrations
   ├─ Apply new migrations (if any)
   └─ Log migration results

   Step 6: Health Checks
   ├─ Wait for backend health: /api/health
   ├─ Wait for frontend: http://frontend:3000
   ├─ Run smoke tests
   └─ Verify deployment

   Step 7: Notify
   ├─ Post deployment status to PR
   └─ Send notification (optional)

8. Verify in Production
   - Visit your Azure URL
   - Login and test functionality
   - Check logs in Application Insights

9. Monitor
   - Azure Portal → Application Insights
   - View real-time metrics
   - Set up alerts (optional)
```

### Manual Deployment (Emergency)

If you need to deploy manually (bypassing CI/CD):

```bash
# 1. Build Docker image locally
docker build -t oscal-ux:manual-deploy .

# 2. Tag for ACR
docker tag oscal-ux:manual-deploy oscalcr.azurecr.io/oscal-ux:manual

# 3. Login to ACR
az acr login --name oscalcr

# 4. Push image
docker push oscalcr.azurecr.io/oscal-ux:manual

# 5. Update container instance
az container restart --resource-group oscal-tools-prod --name oscal-ux
```

---

## Troubleshooting

### Issue: GitHub Actions Fails - Authentication Error

**Symptom**:
```
Error: Unable to authenticate to Azure
```

**Solution**:
1. Verify `AZURE_CREDENTIALS` secret is correct
2. Check service principal has not expired
3. Ensure service principal has Contributor role:
   ```bash
   az role assignment list --assignee <service-principal-id> --output table
   ```

### Issue: Terraform Fails - Resource Already Exists

**Symptom**:
```
Error: A resource with the ID "/subscriptions/.../resourceGroups/oscal-tools-prod" already exists
```

**Solution**:
```bash
# Import existing resource into Terraform state
terraform import azurerm_resource_group.main /subscriptions/{subscription-id}/resourceGroups/oscal-tools-prod
```

### Issue: Container Won't Start - Database Connection Failed

**Symptom**:
Container logs show:
```
org.postgresql.util.PSQLException: Connection refused
```

**Solution**:
1. Check PostgreSQL firewall rules allow Azure services
2. Verify database connection string in Key Vault
3. Check VNet integration (if using private endpoints)

### Issue: Database Migration Failed

**Symptom**:
```
Flyway migration failed: V1.6__add_new_feature.sql
```

**Solution**:
1. Check migration SQL syntax
2. Verify user has permissions:
   ```sql
   GRANT ALL PRIVILEGES ON DATABASE oscal_production TO oscal_user;
   ```
3. Check migration hasn't already been partially applied:
   ```sql
   SELECT * FROM flyway_schema_history ORDER BY installed_rank DESC LIMIT 5;
   ```
4. If stuck, mark migration as resolved:
   ```sql
   -- ONLY IN EMERGENCY - Fix manually then:
   UPDATE flyway_schema_history
   SET success = true
   WHERE version = '1.6';
   ```

### Issue: Secrets Not Loading from Key Vault

**Symptom**:
Application starts but JWT authentication fails, or CORS errors.

**Solution**:
1. Verify container has system-assigned managed identity enabled
2. Check Key Vault access policies allow the identity
3. Test secret retrieval:
   ```bash
   az keyvault secret show --name JWT-SECRET --vault-name oscal-kv-xxxxx
   ```

### Issue: Image Pull Failed from ACR

**Symptom**:
```
Failed to pull image: unauthorized
```

**Solution**:
1. Enable Admin user on ACR (or use managed identity)
2. Grant container instance `AcrPull` role:
   ```bash
   az role assignment create \
     --assignee <container-identity-principal-id> \
     --role AcrPull \
     --scope /subscriptions/{sub-id}/resourceGroups/{rg}/providers/Microsoft.ContainerRegistry/registries/{acr-name}
   ```

---

## Maintenance

### Monthly Tasks

1. **Update Dependencies**
   ```bash
   # Backend
   cd back-end
   mvn versions:display-dependency-updates

   # Frontend
   cd front-end
   npm outdated
   ```

2. **Review Database Size**
   ```bash
   az postgres flexible-server db show \
     --resource-group oscal-tools-prod \
     --server-name oscal-db-xxxxx \
     --database-name oscal_production
   ```

3. **Review Cost**
   - Azure Portal → Cost Management → Cost Analysis
   - Set budget alerts if spending increases

4. **Check Backup Status**
   ```bash
   az postgres flexible-server backup list \
     --resource-group oscal-tools-prod \
     --name oscal-db-xxxxx \
     --output table
   ```

### Quarterly Tasks

1. **Security Audit**
   - Run OWASP Dependency Check
   - Review Azure Security Center recommendations
   - Update SSL/TLS certificates (if using custom domain)

2. **Performance Review**
   - Application Insights → Performance
   - Optimize slow queries
   - Consider scaling up/out if needed

3. **Disaster Recovery Test**
   - Test backup restoration
   - Verify DR procedures

### Annual Tasks

1. **Compliance Review**
   - Review audit logs (Application Insights)
   - Update security policies
   - Renew certificates

2. **Architecture Review**
   - Consider moving to Kubernetes (AKS) if scale increases
   - Evaluate new Azure services

---

## Cost Estimation

Estimated monthly Azure costs for production deployment:

| Service | Tier | Estimated Cost |
|---------|------|----------------|
| Azure Container Instance | 1 vCPU, 2GB RAM | ~$40/month |
| Azure Database for PostgreSQL | Burstable B1ms | ~$20/month |
| Azure Container Registry | Basic | ~$5/month |
| Azure Key Vault | Standard | ~$0.03/month (per secret) |
| Application Insights | Basic | ~$2.88/GB ingested |
| Bandwidth | Outbound | ~$5/month (first 5GB free) |
| **Total** | | **~$75-100/month** |

**Cost Optimization Tips**:
- Use Azure Reserved Instances for 1-3 year commitments (save up to 72%)
- Enable autoscaling to scale down during off-hours
- Use Azure Hybrid Benefit if you have Windows Server licenses
- Set up budget alerts to monitor spending

---

## Next Steps

✅ **Completed**: Azure setup and GitHub configuration

🔄 **Next**:
1. Create Terraform configuration files (see Part 3)
2. Create GitHub Actions workflows (see Part 4)
3. Update docker-entrypoint.sh for database migrations (see Part 5)
4. Test the deployment pipeline

See the specific implementation files:
- `terraform/` - Infrastructure as Code
- `.github/workflows/` - CI/CD pipelines
- `docker-entrypoint.sh` - Startup script with migrations

---

## Additional Resources

- [Azure Container Instances Documentation](https://docs.microsoft.com/en-us/azure/container-instances/)
- [Azure PostgreSQL Documentation](https://docs.microsoft.com/en-us/azure/postgresql/)
- [Azure Key Vault Documentation](https://docs.microsoft.com/en-us/azure/key-vault/)
- [Terraform Azure Provider](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs)
- [GitHub Actions Documentation](https://docs.github.com/en/actions)
- [Flyway Documentation](https://flywaydb.org/documentation/)

---

**Document Version**: 1.0.0
**Last Updated**: 2025-10-26
**Maintained By**: OSCAL Tools DevOps Team

---

**End of Azure Deployment Guide**
