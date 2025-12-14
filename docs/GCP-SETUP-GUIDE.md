# Google Cloud Platform - Complete Setup Guide

**Status:** Production Ready
**Date:** 2025-01-29
**Estimated Time:** 75 minutes
**Estimated Cost:** $25-30/month (development tier)

---

## Table of Contents

- [Prerequisites](#prerequisites)
- [Part 1: Google Cloud Platform Setup](#part-1-google-cloud-platform-setup)
- [Part 2: Local Machine Setup](#part-2-local-machine-setup)
- [Part 3: Configure Terraform Variables](#part-3-configure-terraform-variables)
- [Part 4: Deploy Infrastructure](#part-4-deploy-infrastructure)
- [Part 5: Build and Deploy Application](#part-5-build-and-deploy-application)
- [Part 6: Verify Deployment](#part-6-verify-deployment)
- [Part 7: Final Configuration](#part-7-final-configuration)
- [Quick Reference](#quick-reference)
- [Troubleshooting](#troubleshooting-common-issues)
- [What's Next](#whats-next)

---

## Prerequisites

Before starting, ensure you have:

- ✅ A Google account (Gmail, Google Workspace, etc.)
- ✅ A credit card for GCP billing (Google offers $300 free credit for new accounts)
- ✅ Terminal access (macOS Terminal, Linux shell, or Windows PowerShell)
- ✅ Basic command-line knowledge
- ✅ Internet connection

**No prior GCP experience required!** This guide walks you through everything.

---

## Part 1: Google Cloud Platform Setup

**Time Required:** ~15 minutes

### Step 1: Create GCP Account and Project

1. **Navigate to Google Cloud Console**
   ```
   https://console.cloud.google.com
   ```

2. **Sign in with your Google account**
   - Use your existing Gmail or create a new account
   - If first-time user, you may be offered $300 in free credits (valid for 90 days)

3. **Accept Terms of Service**
   - Read and accept the Google Cloud Terms of Service
   - Accept the terms for your country/region

4. **Create a New Project**

   a. Click the project dropdown at the top of the page (next to "Google Cloud")

   b. Click **"New Project"** button

   c. Fill in project details:
   - **Project Name:** `oscal-tools-prod` (or your choice)
   - **Organization:** Leave as "No organization" (unless you have a Google Workspace)
   - **Location:** Leave as "No organization"

   d. Click **"Create"**

   e. **IMPORTANT:** Note your **Project ID**
   - It will be auto-generated (e.g., `oscal-tools-prod-123456`)
   - You'll need this later
   - Write it down or copy it somewhere safe

   ```
   Example:
   Project Name: oscal-tools-prod
   Project ID:   oscal-tools-prod-384520  ← YOU NEED THIS
   ```

5. **Select your new project**
   - Click the project dropdown again
   - Select your newly created project
   - Verify it's selected (name appears in top bar)

### Step 2: Enable Billing

> **Note:** Google offers $300 in free credits for new accounts. You won't be charged during the free trial period, but a credit card is required for verification.

1. **Navigate to Billing**
   ```
   https://console.cloud.google.com/billing
   ```

2. **Create Billing Account**

   a. Click **"Add Billing Account"** or **"Create Account"**

   b. Fill in your information:
   - **Country:** Select your country
   - **Account Type:** Choose "Individual" or "Business"
   - **Name/Organization:** Your name or company name
   - **Address:** Your billing address

   c. **Enter payment information:**
   - Credit or debit card number
   - Expiration date
   - CVV/CVC
   - Billing address (if different)

   d. Accept the terms and click **"Submit and enable billing"**

3. **Link Billing to Your Project**

   a. Navigate to **"Account Management"** → **"My Projects"**

   b. Find your project (`oscal-tools-prod`)

   c. Click the **three dots menu** (⋮) next to your project

   d. Click **"Change billing"**

   e. Select your billing account

   f. Click **"Set account"**

   Verify billing is enabled:
   - Green checkmark next to your project name
   - Billing account shows in project details

### Step 3: Enable Required APIs

> **Tip:** We'll use Cloud Shell (a free browser-based terminal) for this step to avoid local setup initially.

1. **Open Cloud Shell**

   a. In the Google Cloud Console, click the **Cloud Shell icon** (>_) in the top-right corner

   b. Wait for the shell to initialize (~10 seconds)

   c. You'll see a terminal at the bottom of your screen

2. **Set Your Project**

   ```bash
   # Replace with YOUR actual project ID
   export PROJECT_ID="oscal-tools-prod-384520"

   # Set this as the default project
   gcloud config set project $PROJECT_ID

   # Verify it's set correctly
   gcloud config get-value project
   ```

3. **Enable All Required APIs**

   Copy and paste this entire block into Cloud Shell:

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
     compute.googleapis.com \
     cloudresourcemanager.googleapis.com \
     servicenetworking.googleapis.com
   ```

   Press **Enter** and wait for completion (~2-3 minutes)

   You should see:
   ```
   Operation "operations/..." finished successfully.
   ```

### Step 4: Create Service Account for Terraform

> **What is a Service Account?** It's a special account that Terraform will use to create resources on your behalf. Think of it as a robot user with specific permissions.

1. **Create the Service Account**

   ```bash
   gcloud iam service-accounts create terraform-sa \
     --display-name="Terraform Service Account" \
     --description="Service account for Terraform deployments"
   ```

2. **Grant Necessary Permissions**

   ```bash
   # Grant Editor role (allows creating/modifying most resources)
   gcloud projects add-iam-policy-binding $PROJECT_ID \
     --member="serviceAccount:terraform-sa@${PROJECT_ID}.iam.gserviceaccount.com" \
     --role="roles/editor"

   # Grant Service Account User role (allows deploying to Cloud Run)
   gcloud projects add-iam-policy-binding $PROJECT_ID \
     --member="serviceAccount:terraform-sa@${PROJECT_ID}.iam.gserviceaccount.com" \
     --role="roles/iam.serviceAccountUser"
   ```

3. **Create and Download the Service Account Key**

   ```bash
   gcloud iam service-accounts keys create ~/terraform-key.json \
     --iam-account=terraform-sa@${PROJECT_ID}.iam.gserviceaccount.com
   ```

   You should see:
   ```
   created key [...] of type [json] as [/home/yourname/terraform-key.json]
   ```

4. **Download the Key to Your Local Computer**

   In Cloud Shell:

   a. Click the **three dots menu** (⋮) in the top right of Cloud Shell

   b. Select **"Download"**

   c. Enter the file path: `terraform-key.json`

   d. Click **"Download"**

   e. Save the file to a secure location on your computer:
   - **macOS/Linux:** `~/Downloads/terraform-key.json`
   - **Windows:** `C:\Users\YourName\Downloads\terraform-key.json`

   > **IMPORTANT:** Keep this file secure! It's like a password that gives access to your GCP resources.

---

## Part 2: Local Machine Setup

**Time Required:** ~15 minutes

### Step 5: Install Required Tools

Choose your operating system:

<details>
<summary><strong>macOS Instructions</strong></summary>

1. **Install Homebrew** (if not already installed)

   ```bash
   /bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"
   ```

   Follow the on-screen instructions.

2. **Install Google Cloud SDK**

   ```bash
   brew install --cask google-cloud-sdk
   ```

3. **Install Terraform**

   ```bash
   brew tap hashicorp/tap
   brew install hashicorp/tap/terraform
   ```

4. **Install Docker** (optional, for local builds)

   ```bash
   brew install --cask docker
   ```

   After installation, open Docker Desktop from Applications.

5. **Verify Installations**

   ```bash
   gcloud version
   terraform version
   docker version  # If installed
   ```

   Expected output:
   ```
   Google Cloud SDK 460.0.0
   Terraform v1.6.0
   Docker version 24.0.7
   ```

</details>

<details>
<summary><strong>Linux (Ubuntu/Debian) Instructions</strong></summary>

1. **Update Package Manager**

   ```bash
   sudo apt-get update
   sudo apt-get install -y apt-transport-https ca-certificates gnupg curl
   ```

2. **Install Google Cloud SDK**

   ```bash
   # Add Google Cloud SDK repository
   curl https://packages.cloud.google.com/apt/doc/apt-key.gpg | \
     sudo gpg --dearmor -o /usr/share/keyrings/cloud.google.gpg

   echo "deb [signed-by=/usr/share/keyrings/cloud.google.gpg] https://packages.cloud.google.com/apt cloud-sdk main" | \
     sudo tee -a /etc/apt/sources.list.d/google-cloud-sdk.list

   # Install
   sudo apt-get update && sudo apt-get install -y google-cloud-cli
   ```

3. **Install Terraform**

   ```bash
   # Add HashiCorp repository
   wget -O- https://apt.releases.hashicorp.com/gpg | \
     sudo gpg --dearmor -o /usr/share/keyrings/hashicorp-archive-keyring.gpg

   echo "deb [signed-by=/usr/share/keyrings/hashicorp-archive-keyring.gpg] https://apt.releases.hashicorp.com $(lsb_release -cs) main" | \
     sudo tee /etc/apt/sources.list.d/hashicorp.list

   # Install
   sudo apt-get update && sudo apt-get install -y terraform
   ```

4. **Install Docker** (optional)

   ```bash
   sudo apt-get install -y docker.io
   sudo systemctl start docker
   sudo systemctl enable docker
   sudo usermod -aG docker $USER
   ```

   Log out and back in for Docker group changes to take effect.

5. **Verify Installations**

   ```bash
   gcloud version
   terraform version
   docker version  # If installed
   ```

</details>

<details>
<summary><strong>Windows Instructions</strong></summary>

1. **Install Chocolatey** (Windows package manager)

   Open **PowerShell as Administrator** and run:

   ```powershell
   Set-ExecutionPolicy Bypass -Scope Process -Force
   [System.Net.ServicePointManager]::SecurityProtocol = [System.Net.ServicePointManager]::SecurityProtocol -bor 3072
   iex ((New-Object System.Net.WebClient).DownloadString('https://community.chocolatey.org/install.ps1'))
   ```

2. **Install Google Cloud SDK**

   ```powershell
   choco install gcloudsdk -y
   ```

3. **Install Terraform**

   ```powershell
   choco install terraform -y
   ```

4. **Install Docker Desktop** (optional)

   Download and install from:
   ```
   https://www.docker.com/products/docker-desktop
   ```

5. **Restart PowerShell** and verify installations

   Close and reopen PowerShell (as regular user), then:

   ```powershell
   gcloud version
   terraform version
   docker version  # If installed
   ```

</details>

### Step 6: Authenticate with Google Cloud

1. **Move Your Service Account Key to a Secure Location**

   **macOS/Linux:**
   ```bash
   # Create a secure directory
   mkdir -p ~/.gcp

   # Move the key file
   mv ~/Downloads/terraform-key.json ~/.gcp/terraform-key.json

   # Secure the file (only you can read it)
   chmod 600 ~/.gcp/terraform-key.json
   ```

   **Windows (PowerShell):**
   ```powershell
   # Create a directory
   New-Item -ItemType Directory -Force -Path $env:USERPROFILE\.gcp

   # Move the key file
   Move-Item $env:USERPROFILE\Downloads\terraform-key.json $env:USERPROFILE\.gcp\terraform-key.json
   ```

2. **Authenticate gcloud CLI with Your Personal Account**

   ```bash
   # This will open a browser window
   gcloud auth login
   ```

   Steps:
   - Browser window will open automatically
   - Sign in with your Google account
   - Click "Allow" to grant permissions
   - Return to your terminal

3. **Set Your Default Project**

   ```bash
   # Replace with YOUR project ID
   gcloud config set project oscal-tools-prod-384520

   # Verify it's set
   gcloud config list
   ```

   You should see:
   ```
   [core]
   account = your-email@gmail.com
   project = oscal-tools-prod-384520
   ```

### Step 7: Set Up Environment Variables

1. **Navigate to Your Project Directory**

   ```bash
   # Replace with your actual path
   cd ~/Documents/GitHub/oscal-cli

   # Or wherever you cloned the repository
   ```

2. **Create Environment Variables File**

   **macOS/Linux:**
   ```bash
   cat > .env.gcp <<'EOF'
# =============================================================================
# OSCAL Tools - Google Cloud Platform Configuration
# =============================================================================
# IMPORTANT: Do not commit this file to git (it's in .gitignore)

# GCP Project Configuration
export PROJECT_ID="YOUR_PROJECT_ID_HERE"
export REGION="us-central1"
export ENVIRONMENT="prod"

# Terraform Service Account (path to your JSON key file)
export GOOGLE_APPLICATION_CREDENTIALS="$HOME/.gcp/terraform-key.json"

# Optional: Uncomment when you set up remote state storage
# export TF_STATE_BUCKET="oscal-terraform-state"
EOF
   ```

   **Windows (PowerShell):**
   ```powershell
   @"
# =============================================================================
# OSCAL Tools - Google Cloud Platform Configuration
# =============================================================================

`$env:PROJECT_ID="YOUR_PROJECT_ID_HERE"
`$env:REGION="us-central1"
`$env:ENVIRONMENT="prod"
`$env:GOOGLE_APPLICATION_CREDENTIALS="`$env:USERPROFILE\.gcp\terraform-key.json"
"@ | Out-File -FilePath .env.gcp.ps1 -Encoding UTF8
   ```

3. **Edit the File with Your Actual Project ID**

   **macOS/Linux:**
   ```bash
   # Using nano (simple text editor)
   nano .env.gcp

   # Or use your favorite editor:
   # code .env.gcp        # VS Code
   # vim .env.gcp         # Vim
   # open -a TextEdit .env.gcp  # macOS TextEdit
   ```

   **Windows:**
   ```powershell
   notepad .env.gcp.ps1
   ```

   Replace `YOUR_PROJECT_ID_HERE` with your actual project ID (e.g., `oscal-tools-prod-384520`)

   Save and close the file.

4. **Load the Environment Variables**

   **macOS/Linux:**
   ```bash
   source .env.gcp

   # Verify they're set correctly
   echo "Project ID: $PROJECT_ID"
   echo "Region: $REGION"
   echo "Credentials: $GOOGLE_APPLICATION_CREDENTIALS"
   ```

   **Windows (PowerShell):**
   ```powershell
   . .\.env.gcp.ps1

   # Verify
   Write-Host "Project ID: $env:PROJECT_ID"
   Write-Host "Region: $env:REGION"
   Write-Host "Credentials: $env:GOOGLE_APPLICATION_CREDENTIALS"
   ```

5. **Add to Shell Profile** (optional but recommended)

   This ensures variables are loaded every time you open a terminal.

   **macOS/Linux (bash):**
   ```bash
   echo "source ~/Documents/GitHub/oscal-cli/.env.gcp" >> ~/.bashrc
   ```

   **macOS (zsh - default on newer Macs):**
   ```bash
   echo "source ~/Documents/GitHub/oscal-cli/.env.gcp" >> ~/.zshrc
   ```

   **Windows (PowerShell):**
   ```powershell
   # Add to PowerShell profile
   Add-Content $PROFILE ". $PWD\.env.gcp.ps1"
   ```

---

## Part 3: Configure Terraform Variables

**Time Required:** ~5 minutes

### Step 8: Create Terraform Configuration File

1. **Navigate to Terraform Directory**

   ```bash
   cd terraform/gcp
   ```

2. **Create `terraform.tfvars` File**

   This file contains all the configuration for your deployment.

   **Method 1: Using cat (macOS/Linux)**
   ```bash
   cat > terraform.tfvars <<EOF
# =============================================================================
# OSCAL Tools - Terraform Variables
# =============================================================================
# This file configures your GCP deployment
# Adjust values based on your needs and budget

# Project Configuration
project_id  = "$PROJECT_ID"
region      = "us-central1"  # Change if you want a different region
environment = "prod"

# Artifact Registry (where Docker images are stored)
artifact_registry_repository = "oscal-tools"

# Database Configuration
# Start with smallest tier, upgrade as needed
db_name     = "oscal_production"
db_username = "oscal_user"
db_tier     = "db-f1-micro"  # Cost: ~$9/month - smallest tier

# Cloud Storage Configuration
bucket_prefix = "oscal-tools"

# Backend Cloud Run Configuration
# Adjust CPU/Memory based on your needs
backend_cpu           = "1000m"   # 1 vCPU
backend_memory        = "2Gi"     # 2GB RAM
backend_min_instances = 0         # Scale to zero when not in use (saves $$)
backend_max_instances = 10        # Maximum concurrent instances

# Frontend Cloud Run Configuration
frontend_cpu           = "1000m"  # 1 vCPU
frontend_memory        = "512Mi"  # 512MB RAM
frontend_min_instances = 0        # Scale to zero
frontend_max_instances = 10

# Networking Configuration
# VPC Connector allows Cloud Run to access Cloud SQL privately
vpc_connector_machine_type  = "e2-micro"  # Cost: ~$12/month - smallest
vpc_connector_min_instances = 2           # Minimum required
vpc_connector_max_instances = 3

# Database Backup Configuration
enable_db_backups         = true
db_backup_start_time      = "03:00"  # 3 AM UTC
db_backup_retention_days  = 7        # Keep backups for 7 days
EOF
   ```

   **Method 2: Manual Creation (Windows or if cat fails)**

   Create a file named `terraform.tfvars` with your text editor and paste:

   ```hcl
   # Project Configuration
   project_id  = "oscal-tools-prod-384520"  # ← CHANGE THIS TO YOUR PROJECT ID
   region      = "us-central1"
   environment = "prod"

   # Artifact Registry
   artifact_registry_repository = "oscal-tools"

   # Database Configuration
   db_name     = "oscal_production"
   db_username = "oscal_user"
   db_tier     = "db-f1-micro"

   # Cloud Storage
   bucket_prefix = "oscal-tools"

   # Backend Cloud Run
   backend_cpu           = "1000m"
   backend_memory        = "2Gi"
   backend_min_instances = 0
   backend_max_instances = 10

   # Frontend Cloud Run
   frontend_cpu           = "1000m"
   frontend_memory        = "512Mi"
   frontend_min_instances = 0
   frontend_max_instances = 10

   # Networking
   vpc_connector_machine_type  = "e2-micro"
   vpc_connector_min_instances = 2
   vpc_connector_max_instances = 3

   # Backups
   enable_db_backups         = true
   db_backup_start_time      = "03:00"
   db_backup_retention_days  = 7
   ```

   **IMPORTANT:** Update `project_id` with YOUR actual project ID!

3. **Review Your Configuration**

   ```bash
   cat terraform.tfvars
   ```

   Make sure:
   - `project_id` matches YOUR project ID
   - All other values look correct
   - No syntax errors (commas, quotes, etc.)

---

## Part 4: Deploy Infrastructure

**Time Required:** ~20 minutes (mostly waiting for resources to be created)

### Step 9: Initialize Terraform

Terraform needs to download the Google Cloud provider and prepare your workspace.

1. **Initialize Terraform**

   ```bash
   terraform init
   ```

   You should see:
   ```
   Initializing the backend...
   Initializing provider plugins...
   - Downloading hashicorp/google v5.0.0...
   - Downloading hashicorp/random v3.5.1...

   Terraform has been successfully initialized!
   ```

   **If you see errors:**
   - "terraform: command not found" → Terraform not installed correctly, go back to Step 5
   - "Failed to get existing workspaces" → Check your credentials are set correctly

2. **Validate Configuration**

   ```bash
   terraform validate
   ```

   Expected output:
   ```
   Success! The configuration is valid.
   ```

   **If you see errors:**
   - Check your `terraform.tfvars` file for syntax errors
   - Make sure you're in the `terraform/gcp` directory

3. **Format Configuration** (optional)

   This cleans up formatting in your Terraform files:

   ```bash
   terraform fmt
   ```

### Step 10: Preview the Deployment

Before creating anything, let's see what Terraform will do.

1. **Generate and Review the Plan**

   ```bash
   terraform plan
   ```

   This will:
   - Check your configuration
   - Show you what will be created
   - Estimate costs
   - Highlight any issues

   You should see output like:
   ```
   Plan: 25 to add, 0 to change, 0 to destroy.

   Changes to Outputs:
     + frontend_url  = (known after apply)
     + backend_url   = (known after apply)
     + database_connection_name = (known after apply)
   ```

   Review the resources that will be created:
   - Cloud Run services (frontend, backend)
   - Cloud SQL database instance
   - Cloud Storage buckets
   - VPC Connector
   - Secrets in Secret Manager
   - Service accounts

2. **Save the Plan** (optional but recommended)

   ```bash
   terraform plan -out=tfplan
   ```

   This saves the plan so you can apply exactly what you reviewed.

### Step 11: Deploy Infrastructure

Now let's actually create the resources.

1. **Apply the Configuration**

   ```bash
   terraform apply
   ```

   Or, if you saved the plan:
   ```bash
   terraform apply tfplan
   ```

2. **Review and Confirm**

   Terraform will show you the plan again and ask:
   ```
   Do you want to perform these actions?
     Terraform will perform the actions described above.
     Only 'yes' will be accepted to approve.

     Enter a value:
   ```

   Type `yes` and press **Enter**

3. **Wait for Deployment**

   This will take **10-15 minutes**. You'll see progress:

   ```
   google_project_service.apis["run.googleapis.com"]: Creating...
   google_secret_manager_secret.jwt_secret: Creating...
   google_storage_bucket.build_bucket: Creating...
   ...
   google_cloud_run_v2_service.backend: Still creating... [2m30s elapsed]
   ...
   ```

   **Don't interrupt this process!** Let it complete.

4. **Verify Success**

   When complete, you should see:
   ```
   Apply complete! Resources: 25 added, 0 changed, 0 destroyed.

   Outputs:

   backend_url = "https://oscal-backend-prod-xxxxx-uc.a.run.app"
   frontend_url = "https://oscal-frontend-prod-xxxxx-uc.a.run.app"
   database_connection_name = "oscal-tools-prod-384520:us-central1:oscal-db-prod-xxxx"
   ...
   ```

5. **Save the Outputs**

   ```bash
   # Save all outputs to a file
   terraform output > ../../terraform-outputs.txt

   # View them
   terraform output
   ```

   **Copy these URLs!** You'll need them in the next steps.

---

## Part 5: Build and Deploy Application

**Time Required:** ~25 minutes

### Step 12: Create Artifact Registry Repository

This is where Docker images will be stored.

1. **Go Back to Project Root**

   ```bash
   cd ../..  # Back to oscal-cli root directory
   ```

2. **Create the Repository**

   ```bash
   gcloud artifacts repositories create oscal-tools \
     --repository-format=docker \
     --location=$REGION \
     --description="OSCAL Tools container images"
   ```

   Expected output:
   ```
   Create request issued for: [oscal-tools]
   Waiting for operation [projects/.../locations/us-central1/operations/...] to complete...done.
   ```

3. **Configure Docker Authentication**

   This allows Docker to push images to Artifact Registry.

   ```bash
   gcloud auth configure-docker ${REGION}-docker.pkg.dev
   ```

   When prompted, type `Y` and press Enter.

### Step 13: Build and Deploy with Cloud Build

This single command will build both Docker images and deploy them to Cloud Run.

1. **Trigger the Build**

   ```bash
   gcloud builds submit \
     --config=cloudbuild.yaml \
     --substitutions=_REGION=$REGION,_ENVIRONMENT=$ENVIRONMENT
   ```

   **What happens:**
   - Stage 1: Build backend (Maven + Java) → ~5 minutes
   - Stage 2: Build frontend (npm + Next.js) → ~5 minutes
   - Stage 3: Push backend to Artifact Registry → ~2 minutes
   - Stage 4: Push frontend to Artifact Registry → ~1 minute
   - Stage 5: Deploy backend to Cloud Run → ~2 minutes
   - Stage 6: Deploy frontend to Cloud Run → ~2 minutes

   **Total: 15-20 minutes**

2. **Monitor the Build**

   You can:

   **Option A:** Watch in terminal (recommended)
   - Output will stream in real-time
   - Shows each step as it completes

   **Option B:** View in Cloud Console
   - Open: https://console.cloud.google.com/cloud-build
   - Click on the running build
   - See detailed logs and progress

3. **Build Output**

   You'll see output like:
   ```
   BUILD
   Starting Step #0 - "build-backend"
   Already have image (with digest): gcr.io/cloud-builders/docker
   Sending build context to Docker daemon...
   Step 1/10 : FROM maven:3.9-eclipse-temurin-21 AS backend-builder
   ...
   Successfully tagged us-central1-docker.pkg.dev/.../oscal-backend:latest
   DONE
   ...
   ID                                    CREATE_TIME                DURATION  STATUS
   abc123-def456-ghi789                  2025-01-29T12:34:56+00:00  17M       SUCCESS
   ```

4. **Handle Errors**

   **Common errors:**

   - **"Permission denied"**
     ```bash
     # Grant Cloud Build permissions
     PROJECT_NUMBER=$(gcloud projects describe $PROJECT_ID --format='value(projectNumber)')

     gcloud projects add-iam-policy-binding $PROJECT_ID \
       --member=serviceAccount:$PROJECT_NUMBER@cloudbuild.gserviceaccount.com \
       --role=roles/run.admin

     gcloud projects add-iam-policy-binding $PROJECT_ID \
       --member=serviceAccount:$PROJECT_NUMBER@cloudbuild.gserviceaccount.com \
       --role=roles/iam.serviceAccountUser
     ```

   - **"Quota exceeded"**
     - Wait a few minutes and try again
     - Or request quota increase in Cloud Console

   - **Build timeout**
     - This is normal if slow internet
     - Cloud Build will retry automatically

---

## Part 6: Verify Deployment

**Time Required:** ~5 minutes

### Step 14: Test the Application

1. **Get Your Service URLs**

   ```bash
   # Backend URL
   BACKEND_URL=$(gcloud run services describe oscal-backend-$ENVIRONMENT \
     --region=$REGION \
     --format='value(status.url)')

   echo "Backend URL: $BACKEND_URL"

   # Frontend URL
   FRONTEND_URL=$(gcloud run services describe oscal-frontend-$ENVIRONMENT \
     --region=$REGION \
     --format='value(status.url)')

   echo "Frontend URL: $FRONTEND_URL"
   ```

   Example output:
   ```
   Backend URL: https://oscal-backend-prod-abc123-uc.a.run.app
   Frontend URL: https://oscal-frontend-prod-def456-uc.a.run.app
   ```

2. **Test Backend Health Endpoint**

   ```bash
   curl $BACKEND_URL/actuator/health
   ```

   Expected response:
   ```json
   {"status":"UP"}
   ```

   **If you see an error:**
   - Wait 30 seconds and try again (service may be starting)
   - Check Cloud Run logs: `gcloud logging read "resource.type=cloud_run_revision" --limit=20`

3. **Test Frontend in Browser**

   **macOS:**
   ```bash
   open $FRONTEND_URL
   ```

   **Linux:**
   ```bash
   xdg-open $FRONTEND_URL
   ```

   **Windows:**
   ```powershell
   Start-Process $env:FRONTEND_URL
   ```

   Or manually copy the URL and paste it in your browser.

4. **Create a Test User**

   In the browser:

   a. You should see the OSCAL Hub splash page

   b. Click **"Get Started"** or **"Sign Up"**

   c. Fill in the registration form:
   - Username: `testuser`
   - Email: `test@example.com`
   - Password: Create a strong password (10+ characters)

   d. Click **"Sign Up"**

   e. Log in with your new credentials

5. **Test Core Functionality**

   After logging in:

   a. **Test File Upload:**
   - Navigate to "Validate" page
   - Download a sample OSCAL file from: https://github.com/usnistgov/oscal-content
   - Upload it
   - Verify validation works

   b. **Test Format Conversion:**
   - Navigate to "Convert" page
   - Upload an OSCAL JSON file
   - Convert to XML or YAML
   - Download the result

   c. **Check Operation History:**
   - Navigate to "History" page
   - Verify your operations are recorded

---

## Part 7: Final Configuration

**Time Required:** ~5 minutes

### Step 15: Update CORS Configuration

The backend needs to allow requests from the frontend URL.

1. **Update Backend with Frontend URL**

   ```bash
   gcloud run services update oscal-backend-$ENVIRONMENT \
     --region=$REGION \
     --update-env-vars="CORS_ALLOWED_ORIGINS=$FRONTEND_URL"
   ```

   This takes ~30 seconds.

2. **Verify CORS is Set**

   ```bash
   gcloud run services describe oscal-backend-$ENVIRONMENT \
     --region=$REGION \
     --format='value(spec.template.spec.containers[0].env)' | \
     grep CORS
   ```

   Should show:
   ```
   name: CORS_ALLOWED_ORIGINS
   value: https://oscal-frontend-prod-xxxxx-uc.a.run.app
   ```

3. **Test Frontend → Backend Connection**

   - Go back to the frontend in your browser
   - Hard refresh: Ctrl+Shift+R (Windows/Linux) or Cmd+Shift+R (Mac)
   - Log in again if needed
   - Try uploading a file
   - Should work without CORS errors

---

## Quick Reference

### Save Your Configuration

Create a reference file with all your important information:

```bash
cat > ~/oscal-gcp-config.txt <<EOF
# =============================================================================
# OSCAL Tools - GCP Configuration Reference
# =============================================================================
# Generated: $(date)

PROJECT_ID: $PROJECT_ID
REGION: $REGION
ENVIRONMENT: $ENVIRONMENT

# Application URLs
Frontend: $FRONTEND_URL
Backend:  $BACKEND_URL
API Docs: $BACKEND_URL/swagger-ui

# Google Cloud Console
Dashboard: https://console.cloud.google.com/home/dashboard?project=$PROJECT_ID
Cloud Run: https://console.cloud.google.com/run?project=$PROJECT_ID
Cloud SQL: https://console.cloud.google.com/sql?project=$PROJECT_ID
Cloud Storage: https://console.cloud.google.com/storage?project=$PROJECT_ID
Logs: https://console.cloud.google.com/logs?project=$PROJECT_ID
Billing: https://console.cloud.google.com/billing?project=$PROJECT_ID

# =============================================================================
# Useful Commands
# =============================================================================

# View backend logs
gcloud logging read "resource.type=cloud_run_revision AND resource.labels.service_name=oscal-backend-$ENVIRONMENT" --limit=50 --format=json

# View frontend logs
gcloud logging read "resource.type=cloud_run_revision AND resource.labels.service_name=oscal-frontend-$ENVIRONMENT" --limit=50 --format=json

# Redeploy after code changes
cd /path/to/oscal-cli
gcloud builds submit --config=cloudbuild.yaml --substitutions=_REGION=$REGION,_ENVIRONMENT=$ENVIRONMENT

# Update environment variables
gcloud run services update oscal-backend-$ENVIRONMENT --region=$REGION --update-env-vars="KEY=VALUE"

# Access Cloud SQL database
gcloud sql connect \$(terraform output -raw database_instance_name 2>/dev/null || echo "oscal-db-prod-xxxx") --user=oscal_user

# View secrets
gcloud secrets versions access latest --secret=jwt-secret
gcloud secrets versions access latest --secret=db-password

# Scale services
gcloud run services update oscal-backend-$ENVIRONMENT --region=$REGION --min-instances=1  # Keep warm
gcloud run services update oscal-backend-$ENVIRONMENT --region=$REGION --min-instances=0  # Scale to zero

# Stop all services (to save costs)
gcloud run services delete oscal-backend-$ENVIRONMENT --region=$REGION
gcloud run services delete oscal-frontend-$ENVIRONMENT --region=$REGION

# =============================================================================
# Cost Information
# =============================================================================

Estimated monthly cost (development): \$25-30
- Cloud Run: \$1-5
- Cloud SQL (db-f1-micro): \$9.37
- Cloud Storage: \$0.20
- VPC Connector: \$12.41
- Misc: \$2

View current costs: https://console.cloud.google.com/billing/reports?project=$PROJECT_ID

Set up budget alerts: https://console.cloud.google.com/billing/budgets?project=$PROJECT_ID

EOF

cat ~/oscal-gcp-config.txt
```

---

## Troubleshooting Common Issues

### Issue 1: "API not enabled" Error

**Symptom:**
```
Error: Error creating service: googleapi: Error 403:
Cloud Run API has not been used in project before or it is disabled.
```

**Solution:**
```bash
# Re-enable the specific API
gcloud services enable run.googleapis.com

# Or enable all required APIs again
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

### Issue 2: "Permission denied" Error

**Symptom:**
```
Error: Error when reading or editing Project Service: Request
`Get "https://serviceusage.googleapis.com/...` returned error 403
```

**Solution:**
```bash
# Check your service account has correct permissions
gcloud projects get-iam-policy $PROJECT_ID \
  --flatten="bindings[].members" \
  --format="table(bindings.role)" \
  --filter="bindings.members:terraform-sa@"

# Re-grant Editor role
gcloud projects add-iam-policy-binding $PROJECT_ID \
  --member="serviceAccount:terraform-sa@${PROJECT_ID}.iam.gserviceaccount.com" \
  --role="roles/editor"

# Re-grant Service Account User role
gcloud projects add-iam-policy-binding $PROJECT_ID \
  --member="serviceAccount:terraform-sa@${PROJECT_ID}.iam.gserviceaccount.com" \
  --role="roles/iam.serviceAccountUser"
```

### Issue 3: Cloud Run Service Won't Start

**Symptom:**
```
Service oscal-backend-prod is in state DEPLOYING but never becomes READY
```

**Diagnosis:**
```bash
# Check service status
gcloud run services describe oscal-backend-$ENVIRONMENT \
  --region=$REGION \
  --format='value(status.conditions)'

# View recent logs
gcloud logging read "resource.type=cloud_run_revision AND resource.labels.service_name=oscal-backend-$ENVIRONMENT" \
  --limit=50 \
  --format=json
```

**Common causes:**
1. **Missing environment variables** → Check all required vars are set
2. **Database connection failure** → Verify Cloud SQL is running and VPC Connector is working
3. **Secret Manager access denied** → Check service account permissions
4. **Application crashes on startup** → Check logs for Java errors

**Solutions:**
```bash
# Verify all environment variables are set
gcloud run services describe oscal-backend-$ENVIRONMENT \
  --region=$REGION \
  --format='value(spec.template.spec.containers[0].env)'

# Check Cloud SQL status
gcloud sql instances list

# Test VPC Connector
gcloud compute networks vpc-access connectors describe oscal-vpc-connector \
  --region=$REGION

# Grant Secret Manager access
gcloud secrets add-iam-policy-binding jwt-secret \
  --member="serviceAccount:oscal-backend-sa-$ENVIRONMENT@$PROJECT_ID.iam.gserviceaccount.com" \
  --role="roles/secretmanager.secretAccessor"

gcloud secrets add-iam-policy-binding db-password \
  --member="serviceAccount:oscal-backend-sa-$ENVIRONMENT@$PROJECT_ID.iam.gserviceaccount.com" \
  --role="roles/secretmanager.secretAccessor"
```

### Issue 4: Frontend Can't Connect to Backend

**Symptom:**
- Frontend loads but API requests fail
- Browser console shows CORS errors or 403 errors

**Diagnosis:**
```bash
# Check CORS configuration
gcloud run services describe oscal-backend-$ENVIRONMENT \
  --region=$REGION \
  --format='get(spec.template.spec.containers[0].env)' | \
  grep CORS

# Test backend directly
curl $BACKEND_URL/actuator/health
```

**Solutions:**
```bash
# Update CORS to allow frontend
gcloud run services update oscal-backend-$ENVIRONMENT \
  --region=$REGION \
  --update-env-vars="CORS_ALLOWED_ORIGINS=$FRONTEND_URL"

# Verify backend is publicly accessible
gcloud run services describe oscal-backend-$ENVIRONMENT \
  --region=$REGION \
  --format='value(status.url)'

# Check IAM policy allows public access
gcloud run services get-iam-policy oscal-backend-$ENVIRONMENT \
  --region=$REGION
```

### Issue 5: Database Connection Failures

**Symptom:**
```
org.postgresql.util.PSQLException: Connection refused
```

**Diagnosis:**
```bash
# Check Cloud SQL instance status
gcloud sql instances describe $(terraform output -raw database_instance_name | grep -oE '[^:]+$')

# Test connection from Cloud Shell
gcloud sql connect $(terraform output -raw database_instance_name | grep -oE '[^:]+$') --user=oscal_user
```

**Solutions:**
```bash
# Verify VPC Connector is working
gcloud compute networks vpc-access connectors describe oscal-vpc-connector \
  --region=$REGION \
  --format='value(state)'
# Should show: READY

# Check if database is running
gcloud sql instances list --filter="name:oscal-db"

# Restart Cloud SQL instance if needed
gcloud sql instances restart $(terraform output -raw database_instance_name | grep -oE '[^:]+$')

# Verify connection string is correct
gcloud run services describe oscal-backend-$ENVIRONMENT \
  --region=$REGION \
  --format='value(spec.template.spec.containers[0].env)' | \
  grep DB_URL
```

### Issue 6: Cloud Build Timeout

**Symptom:**
```
ERROR: build step 0 "gcr.io/cloud-builders/docker" failed:
context deadline exceeded
```

**Solutions:**
```bash
# Increase timeout in cloudbuild.yaml (already set to 20 minutes)
# If build is slow, you can retry:
gcloud builds submit \
  --config=cloudbuild.yaml \
  --timeout=30m \
  --substitutions=_REGION=$REGION,_ENVIRONMENT=$ENVIRONMENT

# Or use a larger machine type
# Edit cloudbuild.yaml and change:
# machineType: 'E2_HIGHCPU_32'  # Faster but more expensive
```

### Issue 7: Out of Quota

**Symptom:**
```
Error 429: Quota exceeded for quota metric 'Cloud Run requests' and limit
'Cloud Run requests per day' of service 'run.googleapis.com'
```

**Solution:**
```bash
# Request quota increase
# 1. Go to: https://console.cloud.google.com/iam-admin/quotas?project=$PROJECT_ID
# 2. Search for the quota name from the error message
# 3. Click the quota → Click "Edit Quotas"
# 4. Enter new limit and justification
# 5. Submit request

# Or wait 24 hours for quota to reset
```

### Issue 8: "terraform: command not found"

**Solution:**
```bash
# Verify Terraform installation
which terraform

# If not found, reinstall:
# macOS:
brew install hashicorp/tap/terraform

# Linux:
sudo apt-get update && sudo apt-get install terraform

# Windows:
choco install terraform

# Verify:
terraform version
```

### Getting More Help

1. **View Logs:**
   ```bash
   # All logs for your project
   gcloud logging read "resource.type=cloud_run_revision" \
     --limit=100 \
     --format=json

   # Error logs only
   gcloud logging read "resource.type=cloud_run_revision AND severity>=ERROR" \
     --limit=50
   ```

2. **Check Cloud Console:**
   - Cloud Run: https://console.cloud.google.com/run
   - Cloud SQL: https://console.cloud.google.com/sql
   - Logs Explorer: https://console.cloud.google.com/logs
   - Error Reporting: https://console.cloud.google.com/errors

3. **Community Support:**
   - OSCAL Gitter: https://gitter.im/usnistgov-OSCAL/Lobby
   - Stack Overflow: Tag questions with `google-cloud-run` and `oscal`
   - Google Cloud Support: https://cloud.google.com/support

---

## What's Next?

### Immediate Next Steps

1. ✅ **Your application is running!**
   - Frontend: Accessible via Cloud Run URL
   - Backend: Processing OSCAL files
   - Database: Storing user data and history

2. **Explore the Application**
   - Upload sample OSCAL files
   - Try validation, conversion, and visualization
   - Check operation history

3. **Set Up Monitoring** (Recommended)
   ```bash
   # Enable monitoring dashboard
   # Go to: https://console.cloud.google.com/monitoring?project=$PROJECT_ID

   # Set up budget alerts
   # Go to: https://console.cloud.google.com/billing/budgets?project=$PROJECT_ID
   ```

### Optional Enhancements

<details>
<summary><strong>1. Set Up Custom Domain</strong></summary>

Instead of the long Cloud Run URL, use your own domain:

```bash
# Map custom domain to Cloud Run
gcloud run domain-mappings create \
  --service=oscal-frontend-$ENVIRONMENT \
  --domain=oscal.yourdomain.com \
  --region=$REGION

# Follow instructions to update DNS records
```

</details>

<details>
<summary><strong>2. Enable Cloud CDN</strong></summary>

Improve performance and reduce costs by caching static assets:

```bash
# This requires setting up a load balancer
# See: docs/GCP-DEPLOYMENT-GUIDE.md for detailed instructions
```

</details>

<details>
<summary><strong>3. Upgrade Database for Production</strong></summary>

If you're getting real traffic, upgrade from the micro instance:

```bash
# Edit terraform.tfvars
# Change: db_tier = "db-custom-2-7680"
# Then:
cd terraform/gcp
terraform apply
```

</details>

<details>
<summary><strong>4. Set Up CI/CD with GitHub Actions</strong></summary>

Automatically deploy when you push code:

```yaml
# .github/workflows/deploy-gcp.yml
name: Deploy to GCP
on:
  push:
    branches: [main]
jobs:
  deploy:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - uses: google-github-actions/setup-gcloud@v1
        with:
          service_account_key: ${{ secrets.GCP_SA_KEY }}
      - run: gcloud builds submit --config=cloudbuild.yaml
```

</details>

<details>
<summary><strong>5. Enable High Availability</strong></summary>

For production workloads:

```bash
# Edit terraform.tfvars
# Add:
# db_availability_type = "REGIONAL"  # Multi-zone
# backend_min_instances = 1          # Always-on
# frontend_min_instances = 1

cd terraform/gcp
terraform apply
```

</details>

### Cost Management

**Current Estimated Cost:** $25-30/month

**To reduce costs:**
- ✅ Already using smallest database tier
- ✅ Already scaling to zero
- ✅ Already using smallest VPC connector

**To increase costs (for better performance):**
- Upgrade database tier: `db_tier = "db-custom-2-7680"` (+$63/month)
- Keep services warm: `min_instances = 1` (+$15-30/month)
- Enable HA database: `REGIONAL` (+$70/month)

**Track costs:**
```bash
# View billing
open "https://console.cloud.google.com/billing?project=$PROJECT_ID"

# Set up budget alerts (recommended)
gcloud billing budgets create \
  --billing-account=$(gcloud billing projects describe $PROJECT_ID --format='value(billingAccountName)') \
  --display-name="OSCAL Tools Budget" \
  --budget-amount=50 \
  --threshold-rule=percent=50 \
  --threshold-rule=percent=90
```

---

## Summary

**Congratulations!** 🎉 You've successfully deployed OSCAL Hub to Google Cloud Platform!

**What you've accomplished:**
- ✅ Set up GCP project with billing
- ✅ Enabled all required APIs
- ✅ Created service accounts and credentials
- ✅ Deployed infrastructure with Terraform
- ✅ Built and deployed Docker containers
- ✅ Configured Cloud Run services
- ✅ Set up Cloud SQL database
- ✅ Configured Cloud Storage
- ✅ Tested the application

**Your application is now:**
- 🌐 Publicly accessible via HTTPS
- 🔒 Secured with authentication and encryption
- 📊 Monitored with Cloud Logging
- 💰 Cost-optimized with scale-to-zero
- 🚀 Ready for production use

**Monthly cost:** ~$25-30 (development tier)

**Need help?** Refer to:
- [GCP Deployment Guide](./GCP-DEPLOYMENT-GUIDE.md) - Comprehensive deployment documentation
- [Cost & Monitoring Guide](./GCP-COST-AND-MONITORING.md) - Cost optimization and monitoring

---

**Last Updated:** 2025-01-29
**Version:** 1.0.0
**Author:** OSCAL Tools Team
