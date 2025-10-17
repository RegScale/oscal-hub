# OSCAL CLI - Azure Deployment Quick Start

Get your OSCAL CLI application running on Azure in 10 minutes.

## Prerequisites

Install the following tools:

```bash
# Install Azure CLI
curl -sL https://aka.ms/InstallAzureCLIDeb | sudo bash

# Install Terraform
wget https://releases.hashicorp.com/terraform/1.6.0/terraform_1.6.0_linux_amd64.zip
unzip terraform_1.6.0_linux_amd64.zip
sudo mv terraform /usr/local/bin/

# Verify Docker is installed
docker --version
```

## Step 1: Login to Azure

```bash
# Login to Azure
az login

# Select your subscription (if you have multiple)
az account list --output table
az account set --subscription "YOUR_SUBSCRIPTION_NAME"
```

## Step 2: Configure Deployment

```bash
# Navigate to deploy directory
cd deploy

# Create your configuration file
cp environments/dev.tfvars terraform.tfvars

# (Optional) Edit configuration
nano terraform.tfvars
```

## Step 3: Deploy Using Automated Script

```bash
# Make the deployment script executable
chmod +x deploy.sh

# Deploy to dev environment
./deploy.sh dev apply
```

That's it! The script will:
1. Check prerequisites
2. Initialize Terraform
3. Build your Docker image
4. Deploy infrastructure to Azure
5. Push image to Azure Container Registry
6. Update the Container App
7. Display your application URL

## Step 4: Access Your Application

After deployment completes, you'll see output like:

```
Application URL: https://ca-oscal-cli-dev.xxx.azurecontainerapps.io
```

Open that URL in your browser!

## Manual Deployment (Alternative)

If you prefer manual steps:

### Initialize Terraform

```bash
cd deploy
terraform init
```

### Deploy Infrastructure

```bash
# Plan deployment
terraform plan -var-file="environments/dev.tfvars"

# Apply deployment
terraform apply -var-file="environments/dev.tfvars"
```

### Build and Push Docker Image

```bash
# Get ACR details
ACR_NAME=$(terraform output -raw container_registry_name)
ACR_SERVER=$(terraform output -raw container_registry_login_server)

# Build image
cd ..
docker build -t oscal-ux:latest .

# Login and push to ACR
az acr login --name $ACR_NAME
docker tag oscal-ux:latest $ACR_SERVER/oscal-ux:dev
docker push $ACR_SERVER/oscal-ux:dev
```

### Update Container App

```bash
cd deploy

# Update container app with new image
az containerapp update \
  --name ca-oscal-cli-dev \
  --resource-group rg-oscal-cli-dev \
  --image $ACR_SERVER/oscal-ux:dev
```

### Get Application URL

```bash
terraform output application_url
```

## Common Commands

### View Deployment Status

```bash
cd deploy

# Show all outputs
terraform output

# Get specific output
terraform output application_url
terraform output api_url
```

### Update Application

```bash
# Rebuild and redeploy
./deploy.sh dev apply
```

### View Logs

```bash
# View container logs
az containerapp logs show \
  --name ca-oscal-cli-dev \
  --resource-group rg-oscal-cli-dev \
  --tail 100 \
  --follow
```

### Check Resource Status

```bash
# List all resources in the resource group
az resource list \
  --resource-group rg-oscal-cli-dev \
  --output table
```

### Scale Application

```bash
# Scale to specific replica count
az containerapp update \
  --name ca-oscal-cli-dev \
  --resource-group rg-oscal-cli-dev \
  --min-replicas 2 \
  --max-replicas 10
```

## Deploy to Different Environments

### Development
```bash
./deploy.sh dev apply
```

### Production
```bash
./deploy.sh prod apply
```

## Troubleshooting

### Issue: Terraform init fails

**Solution:**
```bash
cd deploy
rm -rf .terraform
terraform init
```

### Issue: Docker image not found

**Solution:**
```bash
# Ensure image is built
docker images | grep oscal-ux

# Rebuild if needed
cd ..
docker build -t oscal-ux:latest .
```

### Issue: Container app not starting

**Solution:**
```bash
# Check container logs
az containerapp logs show \
  --name ca-oscal-cli-dev \
  --resource-group rg-oscal-cli-dev \
  --tail 50

# Check revision status
az containerapp revision list \
  --name ca-oscal-cli-dev \
  --resource-group rg-oscal-cli-dev \
  --output table
```

### Issue: Can't access application

**Solution:**
```bash
# Verify ingress is enabled
az containerapp show \
  --name ca-oscal-cli-dev \
  --resource-group rg-oscal-cli-dev \
  --query properties.configuration.ingress

# Get FQDN
az containerapp show \
  --name ca-oscal-cli-dev \
  --resource-group rg-oscal-cli-dev \
  --query properties.configuration.ingress.fqdn
```

## Clean Up

To delete all resources:

```bash
# Using automated script
./deploy.sh dev destroy

# Or using Terraform directly
cd deploy
terraform destroy -var-file="environments/dev.tfvars"
```

## Cost Optimization Tips

For development environments:

1. **Enable scale to zero**
   ```hcl
   combined_min_replicas = 0
   ```

2. **Use Basic ACR tier**
   ```hcl
   acr_sku = "Basic"
   ```

3. **Reduce log retention**
   ```hcl
   log_retention_days = 7
   ```

4. **Disable Application Insights**
   ```hcl
   enable_application_insights = false
   ```

## Next Steps

- [Read full deployment guide](README.md)
- [Configure CI/CD with GitHub Actions](README.md#cicd-with-github-actions)
- [Set up custom domain](README.md#custom-domain)
- [Configure monitoring](README.md#monitoring-and-observability)
- [Scale for production](README.md#scaling)

## Getting Help

- Check the [main README](README.md) for detailed documentation
- View [Terraform outputs](README.md#terraform-outputs) for debugging
- Review [Azure Container Apps documentation](https://learn.microsoft.com/en-us/azure/container-apps/)
