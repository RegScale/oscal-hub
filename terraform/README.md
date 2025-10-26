# Terraform Infrastructure - OSCAL Tools

This directory contains Terraform configuration for deploying OSCAL Tools to Azure.

## Files

- `providers.tf` - Azure provider configuration
- `variables.tf` - Input variable definitions
- `main.tf` - Resource group and common resources
- `acr.tf` - Azure Container Registry
- `database.tf` - PostgreSQL Flexible Server
- `keyvault.tf` - Azure Key Vault for secrets
- `container-instance.tf` - Azure Container Instance
- `monitoring.tf` - Application Insights (optional)
- `outputs.tf` - Output values after deployment
- `terraform.tfvars.example` - Example variable values

## Quick Start

### 1. Prerequisites

```bash
# Install Terraform
brew install terraform  # macOS
# or download from https://www.terraform.io/downloads

# Install Azure CLI
brew install azure-cli  # macOS
# or download from https://docs.microsoft.com/en-us/cli/azure/install-azure-cli

# Login to Azure
az login
az account set --subscription "Your Subscription Name"
```

### 2. Configure Variables

```bash
# Copy example file
cp terraform.tfvars.example terraform.tfvars

# Edit terraform.tfvars with your values
# IMPORTANT: Generate strong passwords!
# JWT Secret: openssl rand -base64 64 | tr -d '\n'
# DB Password: openssl rand -base64 32 | tr -d '\n'

vi terraform.tfvars
```

### 3. Initialize Terraform

```bash
# Initialize Terraform (downloads providers)
terraform init
```

### 4. Plan Infrastructure

```bash
# See what will be created
terraform plan

# Save plan to file
terraform plan -out=tfplan
```

### 5. Apply Infrastructure

```bash
# Create all Azure resources
terraform apply tfplan

# Or apply directly (will prompt for confirmation)
terraform apply
```

### 6. View Outputs

```bash
# Show all outputs
terraform output

# Show specific output
terraform output application_url

# Show deployment summary
terraform output deployment_summary
```

## Important Variables

| Variable | Description | Example |
|----------|-------------|---------|
| `db_password` | PostgreSQL admin password | Generate with `openssl rand -base64 32` |
| `jwt_secret` | JWT signing secret | Generate with `openssl rand -base64 64` |
| `cors_allowed_origins` | Allowed CORS origins | `https://your-domain.com` |
| `allowed_ip_addresses` | IPs allowed to access DB | `["203.0.113.0"]` |

## Resource Names

Resources are created with auto-generated suffixes for uniqueness:

- Resource Group: `oscal-tools-prod` (as specified)
- ACR: `oscalcr<random>` (e.g., `oscalcra1b2c3`)
- PostgreSQL: `oscal-db-<random>` (e.g., `oscal-db-a1b2c3`)
- Key Vault: `oscal-kv-<random>` (e.g., `oscal-kv-a1b2c3`)
- Container: `oscal-ux`

## Updating Infrastructure

```bash
# Modify .tf files or terraform.tfvars
vi variables.tf

# Plan changes
terraform plan

# Apply changes
terraform apply
```

## Destroying Infrastructure

```bash
# DANGER: This will delete everything!
terraform destroy

# Destroy specific resource
terraform destroy -target=azurerm_container_group.main
```

## Common Issues

### Issue: "Name already exists"

**Cause**: Resource name must be globally unique (ACR, Key Vault)

**Solution**: Terraform auto-generates unique suffixes. If you see this, someone else might have used that name.

### Issue: "Unauthorized to Key Vault"

**Cause**: Your user doesn't have permission to create secrets

**Solution**: Grant yourself Key Vault Administrator role:
```bash
az role assignment create \
  --role "Key Vault Administrator" \
  --assignee "your-email@example.com" \
  --scope "/subscriptions/<subscription-id>/resourceGroups/oscal-tools-prod"
```

### Issue: "PostgreSQL firewall blocking connection"

**Cause**: Your IP is not allowed

**Solution**: Add your IP to `allowed_ip_addresses` in `terraform.tfvars`:
```hcl
allowed_ip_addresses = ["YOUR.IP.ADDRESS.HERE"]
```

## Terraform State

Terraform state contains sensitive information (passwords, connection strings).

**For Production**:
- Store state remotely in Azure Blob Storage
- Enable state locking
- Encrypt state at rest

Uncomment the backend configuration in `providers.tf`:
```hcl
backend "azurerm" {
  resource_group_name  = "terraform-state-rg"
  storage_account_name = "tfstateoscaltools"
  container_name       = "tfstate"
  key                  = "prod.terraform.tfstate"
}
```

## Cost Estimation

Run `terraform plan` to see estimated costs, or use Azure Cost Management.

Approximate monthly costs:
- Container Instance (2 vCPU, 4GB): ~$60/month
- PostgreSQL (B1ms): ~$20/month
- ACR (Basic): ~$5/month
- Key Vault: ~$0.50/month
- **Total: ~$85-90/month**

## Security Best Practices

1. **Never commit `terraform.tfvars`** - Contains secrets
2. **Use strong passwords** - Generate with `openssl rand`
3. **Enable HTTPS** - Set `SECURITY_REQUIRE_HTTPS=true`
4. **Restrict database access** - Use `allowed_ip_addresses`
5. **Enable monitoring** - Set `enable_monitoring=true`
6. **Regular backups** - PostgreSQL has automatic backups (7 days)

## Next Steps

After Terraform creates the infrastructure:

1. **Build Docker image**:
   ```bash
   cd ..
   docker build -t oscal-ux:latest .
   ```

2. **Push to ACR**:
   ```bash
   ACR_NAME=$(terraform output -raw acr_name)
   az acr login --name $ACR_NAME
   docker tag oscal-ux:latest $ACR_NAME.azurecr.io/oscal-ux:latest
   docker push $ACR_NAME.azurecr.io/oscal-ux:latest
   ```

3. **Restart container**:
   ```bash
   az container restart \
     --resource-group oscal-tools-prod \
     --name oscal-ux
   ```

4. **Verify deployment**:
   ```bash
   APP_URL=$(terraform output -raw application_url)
   curl $APP_URL/api/health
   ```

## Support

For issues with Terraform configuration:
- Check Terraform documentation: https://www.terraform.io/docs
- Check Azure provider docs: https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs

For issues with the application:
- See main repository README
- Check deployment guide: `../docs/AZURE-DEPLOYMENT-GUIDE.md`
