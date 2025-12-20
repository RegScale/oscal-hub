import Link from 'next/link';
import { ArrowLeft, Cloud, Lock, Database, Workflow, AlertCircle, CheckCircle2 } from 'lucide-react';
import { Card, CardHeader, CardTitle, CardContent } from '@/components/ui/card';

export default function AzureDeploymentGuidePage() {
  return (
    <div className="min-h-screen bg-background">
      <div className="container mx-auto py-8 px-4 max-w-5xl">
        {/* Back Button */}
        <Link
          href="/guide"
          className="inline-flex items-center text-sm text-muted-foreground hover:text-foreground mb-6 transition-colors focus:outline-none focus:ring-2 focus:ring-ring focus:ring-offset-2 rounded"
          aria-label="Navigate back to user guide"
        >
          <ArrowLeft className="h-4 w-4 mr-2" aria-hidden="true" />
          Back to User Guide
        </Link>

        {/* Header */}
        <header className="mb-8">
          <div className="flex items-center gap-3 mb-3">
            <Cloud className="h-8 w-8 text-primary" />
            <h1 className="text-4xl font-bold">Azure Deployment Guide</h1>
          </div>
          <p className="text-lg text-muted-foreground">
            Complete guide for deploying OSCAL Tools to Azure using CI/CD, Terraform, and Key Vault
          </p>
          <div className="mt-4 flex items-center gap-4 text-sm text-muted-foreground">
            <span>Version: 1.0.0</span>
            <span>•</span>
            <span>Updated: October 26, 2025</span>
          </div>
        </header>

        {/* Table of Contents */}
        <Card className="mb-8">
          <CardHeader>
            <CardTitle>Table of Contents</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="grid grid-cols-1 md:grid-cols-2 gap-2">
              <a href="#overview" className="text-primary hover:underline">Overview</a>
              <a href="#architecture" className="text-primary hover:underline">Architecture</a>
              <a href="#prerequisites" className="text-primary hover:underline">Prerequisites</a>
              <a href="#azure-setup" className="text-primary hover:underline">Part 1: Azure Setup</a>
              <a href="#github-setup" className="text-primary hover:underline">Part 2: GitHub Setup</a>
              <a href="#terraform" className="text-primary hover:underline">Part 3: Terraform Infrastructure</a>
              <a href="#cicd" className="text-primary hover:underline">Part 4: CI/CD Pipeline</a>
              <a href="#deployment" className="text-primary hover:underline">Part 5: Deployment Process</a>
              <a href="#troubleshooting" className="text-primary hover:underline">Troubleshooting</a>
              <a href="#cost" className="text-primary hover:underline">Cost Estimation</a>
            </div>
          </CardContent>
        </Card>

        {/* Content */}
        <div className="space-y-8">
          {/* Overview */}
          <Card id="overview">
            <CardHeader>
              <CardTitle>Overview</CardTitle>
            </CardHeader>
            <CardContent className="space-y-4">
              <p className="text-muted-foreground">
                This guide walks you through deploying the OSCAL Tools full-stack application to <strong>Azure</strong> with
                production-ready infrastructure and automated CI/CD.
              </p>

              <div>
                <h3 className="text-xl font-semibold mb-3">Key Features</h3>
                <div className="space-y-2">
                  <div className="flex items-start gap-3">
                    <CheckCircle2 className="h-5 w-5 text-green-500 mt-0.5" />
                    <div>
                      <span className="font-medium">Infrastructure as Code</span>
                      <span className="text-muted-foreground"> - Terraform manages all Azure resources</span>
                    </div>
                  </div>
                  <div className="flex items-start gap-3">
                    <CheckCircle2 className="h-5 w-5 text-green-500 mt-0.5" />
                    <div>
                      <span className="font-medium">CI/CD Automation</span>
                      <span className="text-muted-foreground"> - GitHub Actions for build, test, and deploy</span>
                    </div>
                  </div>
                  <div className="flex items-start gap-3">
                    <CheckCircle2 className="h-5 w-5 text-green-500 mt-0.5" />
                    <div>
                      <span className="font-medium">Secure Configuration</span>
                      <span className="text-muted-foreground"> - Azure Key Vault for secrets management</span>
                    </div>
                  </div>
                  <div className="flex items-start gap-3">
                    <CheckCircle2 className="h-5 w-5 text-green-500 mt-0.5" />
                    <div>
                      <span className="font-medium">Database Management</span>
                      <span className="text-muted-foreground"> - Automatic Flyway migrations on deployment</span>
                    </div>
                  </div>
                  <div className="flex items-start gap-3">
                    <CheckCircle2 className="h-5 w-5 text-green-500 mt-0.5" />
                    <div>
                      <span className="font-medium">Container Registry</span>
                      <span className="text-muted-foreground"> - Azure Container Registry (ACR) for Docker images</span>
                    </div>
                  </div>
                </div>
              </div>

              <div>
                <h3 className="text-xl font-semibold mb-3">Deployment Flow</h3>
                <div className="bg-muted p-4 rounded text-sm font-mono overflow-x-auto">
                  <pre className="text-muted-foreground whitespace-pre">
{`Developer → PR to main → Approval → Merge
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
            Production Ready ✓`}
                  </pre>
                </div>
              </div>
            </CardContent>
          </Card>

          {/* Architecture */}
          <Card id="architecture">
            <CardHeader>
              <CardTitle>Architecture</CardTitle>
            </CardHeader>
            <CardContent className="space-y-4">
              <div>
                <h3 className="text-xl font-semibold mb-3">Azure Resources</h3>
                <p className="text-muted-foreground mb-3">The deployment creates the following Azure resources:</p>
                <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
                  <div className="flex items-start gap-2">
                    <CheckCircle2 className="h-5 w-5 text-green-500 mt-0.5" />
                    <div>
                      <div className="font-medium">Resource Group</div>
                      <div className="text-sm text-muted-foreground">Container for all resources</div>
                    </div>
                  </div>
                  <div className="flex items-start gap-2">
                    <CheckCircle2 className="h-5 w-5 text-green-500 mt-0.5" />
                    <div>
                      <div className="font-medium">Container Registry (ACR)</div>
                      <div className="text-sm text-muted-foreground">Private Docker image registry</div>
                    </div>
                  </div>
                  <div className="flex items-start gap-2">
                    <CheckCircle2 className="h-5 w-5 text-green-500 mt-0.5" />
                    <div>
                      <div className="font-medium">PostgreSQL Database</div>
                      <div className="text-sm text-muted-foreground">Managed database service</div>
                    </div>
                  </div>
                  <div className="flex items-start gap-2">
                    <CheckCircle2 className="h-5 w-5 text-green-500 mt-0.5" />
                    <div>
                      <div className="font-medium">Azure Key Vault</div>
                      <div className="text-sm text-muted-foreground">Secure secrets storage</div>
                    </div>
                  </div>
                  <div className="flex items-start gap-2">
                    <CheckCircle2 className="h-5 w-5 text-green-500 mt-0.5" />
                    <div>
                      <div className="font-medium">Container Instances (ACI)</div>
                      <div className="text-sm text-muted-foreground">Container hosting</div>
                    </div>
                  </div>
                  <div className="flex items-start gap-2">
                    <CheckCircle2 className="h-5 w-5 text-green-500 mt-0.5" />
                    <div>
                      <div className="font-medium">Application Insights</div>
                      <div className="text-sm text-muted-foreground">Monitoring and diagnostics</div>
                    </div>
                  </div>
                </div>
              </div>
            </CardContent>
          </Card>

          {/* Prerequisites */}
          <Card id="prerequisites">
            <CardHeader>
              <CardTitle>Prerequisites</CardTitle>
            </CardHeader>
            <CardContent className="space-y-6">
              <div>
                <h3 className="text-lg font-semibold mb-3">Required Tools</h3>
                <div className="space-y-4">
                  <div>
                    <h4 className="font-medium mb-2">1. Azure CLI (version 2.50+)</h4>
                    <code className="block bg-muted p-3 rounded text-sm font-mono whitespace-pre overflow-x-auto">
{`# macOS
brew install azure-cli

# Windows
winget install Microsoft.AzureCLI

# Linux
curl -sL https://aka.ms/InstallAzureCLIDeb | sudo bash

# Verify
az --version`}
                    </code>
                  </div>

                  <div>
                    <h4 className="font-medium mb-2">2. Terraform (version 1.5+)</h4>
                    <code className="block bg-muted p-3 rounded text-sm font-mono whitespace-pre overflow-x-auto">
{`# macOS
brew install terraform

# Windows
winget install Hashicorp.Terraform

# Verify
terraform --version`}
                    </code>
                  </div>

                  <div>
                    <h4 className="font-medium mb-2">3. GitHub CLI (optional but recommended)</h4>
                    <code className="block bg-muted p-3 rounded text-sm font-mono whitespace-pre overflow-x-auto">
{`# macOS
brew install gh

# Windows
winget install GitHub.cli

# Verify
gh --version`}
                    </code>
                  </div>
                </div>
              </div>

              <div>
                <h3 className="text-lg font-semibold mb-3">Required Accounts</h3>
                <ul className="list-disc list-inside space-y-2 text-muted-foreground ml-4">
                  <li><strong>Azure Account</strong> with permissions to create resources (Resource Groups, Container Registries, PostgreSQL, Key Vaults, etc.)</li>
                  <li><strong>GitHub Account</strong> with admin access to the repository</li>
                </ul>
              </div>

              <div className="bg-blue-500/10 border border-blue-500/20 p-4 rounded">
                <p className="text-sm font-semibold mb-2 text-blue-600 dark:text-blue-400">Azure Free Account:</p>
                <p className="text-sm text-muted-foreground">
                  Sign up for a free account at <a href="https://azure.microsoft.com/free/" target="_blank" rel="noopener noreferrer" className="text-primary hover:underline">https://azure.microsoft.com/free/</a>
                </p>
                <ul className="list-disc list-inside space-y-1 text-sm text-muted-foreground ml-4 mt-2">
                  <li>$200 credit for 30 days</li>
                  <li>12 months of free services</li>
                  <li>25+ always-free services</li>
                </ul>
              </div>
            </CardContent>
          </Card>

          {/* Azure Setup */}
          <Card id="azure-setup">
            <CardHeader>
              <CardTitle className="flex items-center gap-2">
                <Cloud className="h-5 w-5" />
                Part 1: Azure Setup
              </CardTitle>
            </CardHeader>
            <CardContent className="space-y-6">
              <div>
                <h3 className="text-lg font-semibold mb-3">Step 1: Login to Azure</h3>
                <code className="block bg-muted p-4 rounded text-sm font-mono whitespace-pre overflow-x-auto">
{`# Login to Azure
az login

# If you have multiple subscriptions, list them
az account list --output table

# Set the subscription you want to use
az account set --subscription "Your Subscription Name"

# Verify the correct subscription is active
az account show --output table`}
                </code>
              </div>

              <div>
                <h3 className="text-lg font-semibold mb-3">Step 2: Create a Service Principal</h3>
                <p className="text-sm text-muted-foreground mb-3">
                  The service principal is used by GitHub Actions to authenticate with Azure.
                </p>
                <code className="block bg-muted p-4 rounded text-sm font-mono whitespace-pre overflow-x-auto">
{`# Set variables (customize these)
export AZURE_SUBSCRIPTION_ID=$(az account show --query id -o tsv)
export SP_NAME="oscal-tools-github-actions"
export RESOURCE_GROUP_NAME="oscal-tools-prod"

# Create service principal with Contributor role
az ad sp create-for-rbac \\
  --name "$SP_NAME" \\
  --role Contributor \\
  --scopes /subscriptions/$AZURE_SUBSCRIPTION_ID/resourceGroups/$RESOURCE_GROUP_NAME \\
  --sdk-auth \\
  --output json > azure-credentials.json

# IMPORTANT: Save this file securely! You'll need it for GitHub Secrets`}
                </code>
                <div className="mt-3 bg-red-500/10 border border-red-500/20 p-4 rounded">
                  <p className="text-sm font-semibold mb-2 text-red-600 dark:text-red-400">⚠️ CRITICAL:</p>
                  <ul className="list-disc list-inside space-y-1 text-sm text-muted-foreground ml-4">
                    <li>The <code className="bg-muted px-1.5 py-0.5 rounded">azure-credentials.json</code> file contains sensitive credentials</li>
                    <li>Store it in a password manager or secure vault</li>
                    <li><strong>NEVER commit this file to version control</strong></li>
                    <li>Add it to <code className="bg-muted px-1.5 py-0.5 rounded">.gitignore</code> immediately</li>
                  </ul>
                </div>
              </div>

              <div>
                <h3 className="text-lg font-semibold mb-3">Step 3: Create Resource Group</h3>
                <code className="block bg-muted p-4 rounded text-sm font-mono whitespace-pre overflow-x-auto">
{`# Choose a location (region)
# Common regions: eastus, westus2, centralus, westeurope, eastasia

# Create resource group
az group create \\
  --name "$RESOURCE_GROUP_NAME" \\
  --location "eastus"`}
                </code>
              </div>
            </CardContent>
          </Card>

          {/* GitHub Setup */}
          <Card id="github-setup">
            <CardHeader>
              <CardTitle className="flex items-center gap-2">
                <Lock className="h-5 w-5" />
                Part 2: GitHub Repository Setup
              </CardTitle>
            </CardHeader>
            <CardContent className="space-y-6">
              <div>
                <h3 className="text-lg font-semibold mb-3">Step 1: Protect Main Branch</h3>
                <p className="text-muted-foreground mb-3">
                  Configure branch protection to require pull requests for all changes.
                </p>
                <ol className="list-decimal list-inside space-y-2 text-sm text-muted-foreground ml-4">
                  <li>Navigate to your repository on GitHub</li>
                  <li>Go to <strong>Settings</strong> → <strong>Branches</strong></li>
                  <li>Click <strong>Add branch protection rule</strong></li>
                  <li>Set branch name pattern to <code className="bg-muted px-1.5 py-0.5 rounded">main</code></li>
                  <li>Enable:
                    <ul className="list-disc list-inside ml-6 mt-1 space-y-1">
                      <li>Require a pull request before merging</li>
                      <li>Require approvals: 1 (or more)</li>
                      <li>Require status checks to pass before merging</li>
                      <li>Require conversation resolution before merging</li>
                    </ul>
                  </li>
                  <li>Click <strong>Create</strong> to save</li>
                </ol>
              </div>

              <div>
                <h3 className="text-lg font-semibold mb-3">Step 2: Add GitHub Secrets</h3>
                <p className="text-muted-foreground mb-3">
                  Navigate to <strong>Settings</strong> → <strong>Secrets and variables</strong> → <strong>Actions</strong> → <strong>New repository secret</strong>
                </p>
                <div className="space-y-3">
                  <div className="bg-muted p-3 rounded">
                    <h4 className="font-medium mb-1">1. AZURE_CREDENTIALS</h4>
                    <p className="text-sm text-muted-foreground">
                      Contents of <code className="bg-background px-1.5 py-0.5 rounded">azure-credentials.json</code> file (the entire JSON object)
                    </p>
                  </div>
                  <div className="bg-muted p-3 rounded">
                    <h4 className="font-medium mb-1">2. AZURE_SUBSCRIPTION_ID</h4>
                    <p className="text-sm text-muted-foreground">Your Azure subscription ID</p>
                  </div>
                  <div className="bg-muted p-3 rounded">
                    <h4 className="font-medium mb-1">3. JWT_SECRET</h4>
                    <p className="text-sm text-muted-foreground mb-2">Generate a secure random string (64+ characters):</p>
                    <code className="block bg-background p-2 rounded text-xs font-mono">
                      openssl rand -base64 64 | tr -d &apos;\n&apos;
                    </code>
                  </div>
                  <div className="bg-muted p-3 rounded">
                    <h4 className="font-medium mb-1">4. DB_PASSWORD</h4>
                    <p className="text-sm text-muted-foreground mb-2">Generate a strong database password (32+ characters):</p>
                    <code className="block bg-background p-2 rounded text-xs font-mono">
                      openssl rand -base64 32 | tr -d &apos;\n&apos;
                    </code>
                  </div>
                  <div className="bg-muted p-3 rounded">
                    <h4 className="font-medium mb-1">5. CORS_ALLOWED_ORIGINS</h4>
                    <p className="text-sm text-muted-foreground">Your production domain(s)</p>
                    <p className="text-xs text-muted-foreground mt-1">
                      Example: <code className="bg-background px-1.5 py-0.5 rounded">https://oscal-tools.example.com</code>
                    </p>
                  </div>
                </div>
              </div>

              <div>
                <h3 className="text-lg font-semibold mb-3">Using GitHub CLI to Add Secrets</h3>
                <code className="block bg-muted p-4 rounded text-sm font-mono whitespace-pre overflow-x-auto">
{`# Set repository
export GITHUB_REPO="your-username/oscal-cli"

# Add AZURE_CREDENTIALS (from file)
gh secret set AZURE_CREDENTIALS < azure-credentials.json --repo $GITHUB_REPO

# Add AZURE_SUBSCRIPTION_ID
gh secret set AZURE_SUBSCRIPTION_ID --body "$AZURE_SUBSCRIPTION_ID" --repo $GITHUB_REPO

# Add JWT_SECRET
gh secret set JWT_SECRET --body "$(openssl rand -base64 64 | tr -d '\\n')" --repo $GITHUB_REPO

# Add DB_PASSWORD
gh secret set DB_PASSWORD --body "$(openssl rand -base64 32 | tr -d '\\n')" --repo $GITHUB_REPO

# Add CORS_ALLOWED_ORIGINS
gh secret set CORS_ALLOWED_ORIGINS --body "https://your-domain.com" --repo $GITHUB_REPO

# Verify secrets were added
gh secret list --repo $GITHUB_REPO`}
                </code>
              </div>
            </CardContent>
          </Card>

          {/* Terraform */}
          <Card id="terraform">
            <CardHeader>
              <CardTitle className="flex items-center gap-2">
                <Database className="h-5 w-5" />
                Part 3: Terraform Infrastructure
              </CardTitle>
            </CardHeader>
            <CardContent className="space-y-6">
              <div>
                <h3 className="text-lg font-semibold mb-3">Directory Structure</h3>
                <p className="text-muted-foreground mb-3">
                  Create a <code className="bg-muted px-1.5 py-0.5 rounded">terraform/</code> directory in your repository:
                </p>
                <code className="block bg-muted p-4 rounded text-sm font-mono whitespace-pre overflow-x-auto">
{`oscal-cli/
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
│   └── terraform.tfvars        # Variable values (DO NOT COMMIT!)`}
                </code>
              </div>

              <div>
                <h3 className="text-lg font-semibold mb-3">What Terraform Creates</h3>
                <div className="space-y-2">
                  <div className="flex items-start gap-2">
                    <CheckCircle2 className="h-5 w-5 text-green-500 mt-0.5" />
                    <span className="text-muted-foreground">Resource Group - Container for all resources</span>
                  </div>
                  <div className="flex items-start gap-2">
                    <CheckCircle2 className="h-5 w-5 text-green-500 mt-0.5" />
                    <span className="text-muted-foreground">Azure Container Registry - For Docker images</span>
                  </div>
                  <div className="flex items-start gap-2">
                    <CheckCircle2 className="h-5 w-5 text-green-500 mt-0.5" />
                    <span className="text-muted-foreground">Azure PostgreSQL Flexible Server - Managed database</span>
                  </div>
                  <div className="flex items-start gap-2">
                    <CheckCircle2 className="h-5 w-5 text-green-500 mt-0.5" />
                    <span className="text-muted-foreground">Azure Key Vault - Secure secrets storage</span>
                  </div>
                  <div className="flex items-start gap-2">
                    <CheckCircle2 className="h-5 w-5 text-green-500 mt-0.5" />
                    <span className="text-muted-foreground">Azure Container Instance - Run the application</span>
                  </div>
                  <div className="flex items-start gap-2">
                    <CheckCircle2 className="h-5 w-5 text-green-500 mt-0.5" />
                    <span className="text-muted-foreground">Application Insights - Monitoring and logging</span>
                  </div>
                </div>
              </div>

              <div>
                <h3 className="text-lg font-semibold mb-3">Initial Deployment</h3>
                <code className="block bg-muted p-4 rounded text-sm font-mono whitespace-pre overflow-x-auto">
{`# 1. Initialize Terraform
cd terraform
terraform init

# 2. Plan infrastructure changes
terraform plan -out=tfplan

# 3. Apply infrastructure (create Azure resources)
terraform apply tfplan

# 4. Note the outputs (ACR name, Key Vault name, etc.)
terraform output`}
                </code>
              </div>
            </CardContent>
          </Card>

          {/* CI/CD */}
          <Card id="cicd">
            <CardHeader>
              <CardTitle className="flex items-center gap-2">
                <Workflow className="h-5 w-5" />
                Part 4: CI/CD Pipeline
              </CardTitle>
            </CardHeader>
            <CardContent className="space-y-6">
              <div>
                <h3 className="text-lg font-semibold mb-3">GitHub Actions Workflows</h3>
                <p className="text-muted-foreground mb-3">
                  The CI/CD pipeline consists of several GitHub Actions workflows:
                </p>
                <div className="space-y-2">
                  <div className="bg-muted p-3 rounded">
                    <h4 className="font-medium mb-1">.github/workflows/ci.yml</h4>
                    <p className="text-sm text-muted-foreground">Build, test, and security scan on every PR</p>
                  </div>
                  <div className="bg-muted p-3 rounded">
                    <h4 className="font-medium mb-1">.github/workflows/deploy.yml</h4>
                    <p className="text-sm text-muted-foreground">Deploy to Azure on merge to main</p>
                  </div>
                  <div className="bg-muted p-3 rounded">
                    <h4 className="font-medium mb-1">.github/workflows/terraform.yml</h4>
                    <p className="text-sm text-muted-foreground">Terraform infrastructure management</p>
                  </div>
                </div>
              </div>

              <div>
                <h3 className="text-lg font-semibold mb-3">Deployment Pipeline Steps</h3>
                <ol className="list-decimal list-inside space-y-2 text-sm text-muted-foreground ml-4">
                  <li>Build Docker Image - Multi-stage build for frontend and backend</li>
                  <li>Push to Azure Container Registry - Tag and push image to ACR</li>
                  <li>Update Azure Key Vault - Store secrets securely</li>
                  <li>Deploy to Azure Container Instance - Create/update container with latest image</li>
                  <li>Run Database Migrations - Flyway applies pending migrations automatically</li>
                  <li>Health Checks - Verify backend and frontend are responding</li>
                  <li>Notify - Post deployment status</li>
                </ol>
              </div>
            </CardContent>
          </Card>

          {/* Deployment Process */}
          <Card id="deployment">
            <CardHeader>
              <CardTitle>Part 5: Deployment Process</CardTitle>
            </CardHeader>
            <CardContent className="space-y-6">
              <div>
                <h3 className="text-lg font-semibold mb-3">Developer Workflow</h3>
                <ol className="list-decimal list-inside space-y-2 text-sm text-muted-foreground ml-4">
                  <li>Create feature branch from main: <code className="bg-muted px-1.5 py-0.5 rounded">git checkout -b feature/new-feature</code></li>
                  <li>Make changes and commit: <code className="bg-muted px-1.5 py-0.5 rounded">git add . && git commit -m &quot;Add new feature&quot;</code></li>
                  <li>Push to GitHub: <code className="bg-muted px-1.5 py-0.5 rounded">git push origin feature/new-feature</code></li>
                  <li>Create Pull Request on GitHub</li>
                  <li>CI runs automatically (build, test, security scan)</li>
                  <li>Request review from team member</li>
                  <li>Merge PR to main after approval</li>
                  <li>CD pipeline triggers automatically and deploys to Azure</li>
                </ol>
              </div>

              <div>
                <h3 className="text-lg font-semibold mb-3">Database Migrations</h3>
                <p className="text-muted-foreground mb-3">
                  Database migrations are handled by <strong>Flyway</strong> and run automatically on container startup.
                </p>
                <div className="bg-blue-500/10 border border-blue-500/20 p-4 rounded">
                  <p className="text-sm font-semibold mb-2 text-blue-600 dark:text-blue-400">Migration Best Practices:</p>
                  <ul className="list-disc list-inside space-y-1 text-sm text-muted-foreground ml-4">
                    <li>Always use versioned migrations (V1.x, V2.x, etc.)</li>
                    <li>Never modify existing migrations - Create a new one instead</li>
                    <li>Test migrations locally before pushing</li>
                    <li>Write reversible migrations</li>
                    <li>Keep migrations small - One logical change per file</li>
                  </ul>
                </div>
              </div>
            </CardContent>
          </Card>

          {/* Troubleshooting */}
          <Card id="troubleshooting">
            <CardHeader>
              <CardTitle>Troubleshooting</CardTitle>
            </CardHeader>
            <CardContent className="space-y-6">
              <div>
                <div className="flex items-start gap-3 mb-3">
                  <AlertCircle className="h-5 w-5 text-amber-500 mt-0.5" />
                  <h3 className="text-lg font-semibold">GitHub Actions Fails - Authentication Error</h3>
                </div>
                <p className="text-sm text-muted-foreground mb-2">
                  <strong>Symptom:</strong> Unable to authenticate to Azure
                </p>
                <p className="text-sm text-muted-foreground mb-2"><strong>Solution:</strong></p>
                <ul className="list-disc list-inside space-y-1 text-sm text-muted-foreground ml-4">
                  <li>Verify <code className="bg-muted px-1.5 py-0.5 rounded">AZURE_CREDENTIALS</code> secret is correct</li>
                  <li>Check service principal has not expired</li>
                  <li>Ensure service principal has Contributor role</li>
                </ul>
              </div>

              <div>
                <div className="flex items-start gap-3 mb-3">
                  <AlertCircle className="h-5 w-5 text-amber-500 mt-0.5" />
                  <h3 className="text-lg font-semibold">Container Won&apos;t Start - Database Connection Failed</h3>
                </div>
                <p className="text-sm text-muted-foreground mb-2">
                  <strong>Symptom:</strong> Container logs show connection refused
                </p>
                <p className="text-sm text-muted-foreground mb-2"><strong>Solution:</strong></p>
                <ul className="list-disc list-inside space-y-1 text-sm text-muted-foreground ml-4">
                  <li>Check PostgreSQL firewall rules allow Azure services</li>
                  <li>Verify database connection string in Key Vault</li>
                  <li>Check VNet integration (if using private endpoints)</li>
                </ul>
              </div>

              <div>
                <div className="flex items-start gap-3 mb-3">
                  <AlertCircle className="h-5 w-5 text-amber-500 mt-0.5" />
                  <h3 className="text-lg font-semibold">Database Migration Failed</h3>
                </div>
                <p className="text-sm text-muted-foreground mb-2">
                  <strong>Symptom:</strong> Flyway migration failed
                </p>
                <p className="text-sm text-muted-foreground mb-2"><strong>Solution:</strong></p>
                <ul className="list-disc list-inside space-y-1 text-sm text-muted-foreground ml-4">
                  <li>Check migration SQL syntax</li>
                  <li>Verify user has necessary permissions</li>
                  <li>Check migration hasn&apos;t already been partially applied</li>
                  <li>Review Flyway schema history table</li>
                </ul>
              </div>
            </CardContent>
          </Card>

          {/* Cost Estimation */}
          <Card id="cost">
            <CardHeader>
              <CardTitle>Cost Estimation</CardTitle>
            </CardHeader>
            <CardContent className="space-y-4">
              <p className="text-muted-foreground">
                Estimated monthly Azure costs for production deployment:
              </p>
              <div className="overflow-x-auto">
                <table className="w-full text-sm">
                  <thead className="border-b">
                    <tr>
                      <th className="text-left py-2 pr-4">Service</th>
                      <th className="text-left py-2 pr-4">Tier</th>
                      <th className="text-left py-2">Estimated Cost</th>
                    </tr>
                  </thead>
                  <tbody className="text-muted-foreground">
                    <tr className="border-b">
                      <td className="py-2 pr-4">Azure Container Instance</td>
                      <td className="py-2 pr-4">1 vCPU, 2GB RAM</td>
                      <td className="py-2">~$40/month</td>
                    </tr>
                    <tr className="border-b">
                      <td className="py-2 pr-4">Azure Database for PostgreSQL</td>
                      <td className="py-2 pr-4">Burstable B1ms</td>
                      <td className="py-2">~$20/month</td>
                    </tr>
                    <tr className="border-b">
                      <td className="py-2 pr-4">Azure Container Registry</td>
                      <td className="py-2 pr-4">Basic</td>
                      <td className="py-2">~$5/month</td>
                    </tr>
                    <tr className="border-b">
                      <td className="py-2 pr-4">Azure Key Vault</td>
                      <td className="py-2 pr-4">Standard</td>
                      <td className="py-2">~$0.03/month</td>
                    </tr>
                    <tr className="border-b">
                      <td className="py-2 pr-4">Application Insights</td>
                      <td className="py-2 pr-4">Basic</td>
                      <td className="py-2">~$2.88/GB</td>
                    </tr>
                    <tr className="border-b font-medium">
                      <td className="py-2 pr-4">Total</td>
                      <td className="py-2 pr-4"></td>
                      <td className="py-2">~$75-100/month</td>
                    </tr>
                  </tbody>
                </table>
              </div>

              <div className="bg-green-500/10 border border-green-500/20 p-4 rounded">
                <p className="text-sm font-semibold mb-2 text-green-600 dark:text-green-400">Cost Optimization Tips:</p>
                <ul className="list-disc list-inside space-y-1 text-sm text-muted-foreground ml-4">
                  <li>Use Azure Reserved Instances for 1-3 year commitments (save up to 72%)</li>
                  <li>Enable autoscaling to scale down during off-hours</li>
                  <li>Set up budget alerts to monitor spending</li>
                  <li>Use Azure Hybrid Benefit if you have Windows Server licenses</li>
                </ul>
              </div>
            </CardContent>
          </Card>

          {/* Next Steps */}
          <Card>
            <CardHeader>
              <CardTitle>Next Steps</CardTitle>
            </CardHeader>
            <CardContent className="space-y-3">
              <p className="text-muted-foreground">After completing Azure setup:</p>
              <ul className="list-disc list-inside space-y-2 text-muted-foreground ml-4">
                <li>Create Terraform configuration files</li>
                <li>Create GitHub Actions workflows</li>
                <li>Test the deployment pipeline</li>
                <li>Set up monitoring and alerts in Application Insights</li>
                <li>Configure custom domain and SSL certificate (optional)</li>
              </ul>
            </CardContent>
          </Card>

          {/* Additional Resources */}
          <Card>
            <CardHeader>
              <CardTitle>Additional Resources</CardTitle>
            </CardHeader>
            <CardContent className="space-y-3">
              <a
                href="https://docs.microsoft.com/en-us/azure/container-instances/"
                target="_blank"
                rel="noopener noreferrer"
                className="flex items-center text-primary hover:underline"
              >
                Azure Container Instances Documentation
              </a>

              <a
                href="https://docs.microsoft.com/en-us/azure/postgresql/"
                target="_blank"
                rel="noopener noreferrer"
                className="flex items-center text-primary hover:underline"
              >
                Azure PostgreSQL Documentation
              </a>

              <a
                href="https://docs.microsoft.com/en-us/azure/key-vault/"
                target="_blank"
                rel="noopener noreferrer"
                className="flex items-center text-primary hover:underline"
              >
                Azure Key Vault Documentation
              </a>

              <a
                href="https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs"
                target="_blank"
                rel="noopener noreferrer"
                className="flex items-center text-primary hover:underline"
              >
                Terraform Azure Provider
              </a>

              <a
                href="https://docs.github.com/en/actions"
                target="_blank"
                rel="noopener noreferrer"
                className="flex items-center text-primary hover:underline"
              >
                GitHub Actions Documentation
              </a>

              <Link
                href="/guide/deployment/local"
                className="flex items-center text-primary hover:underline"
              >
                Local Deployment Guide
              </Link>
            </CardContent>
          </Card>
        </div>
      </div>
    </div>
  );
}
