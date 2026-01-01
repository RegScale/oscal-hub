#!/bin/bash
# ============================================================================#
# OSCAL Tools - Google Cloud Platform Deployment Script
# ============================================================================#
# This script automates the deployment of OSCAL Tools to Google Cloud Platform
#
# Prerequisites:
#   - gcloud CLI installed and authenticated
#   - Terraform installed
#   - Docker installed (for local builds)
#
# Usage:
#   ./deploy-gcp.sh [OPTIONS]
#
# Options:
#   --project-id PROJECT_ID    GCP project ID (required)
#   --region REGION            GCP region (default: us-central1)
#   --environment ENV          Environment (dev|staging|prod, default: prod)
#   --skip-terraform           Skip Terraform infrastructure deployment
#   --skip-build               Skip container builds
#   --skip-deploy              Skip Cloud Run deployment
#   --help                     Show this help message
# ============================================================================#

set -e  # Exit on error
set -u  # Exit on undefined variable

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Default values
PROJECT_ID=""
REGION="us-central1"
ENVIRONMENT="prod"
SKIP_TERRAFORM=false
SKIP_BUILD=false
SKIP_DEPLOY=false

# Parse arguments
while [[ $# -gt 0 ]]; do
  case $1 in
    --project-id)
      PROJECT_ID="$2"
      shift 2
      ;;
    --region)
      REGION="$2"
      shift 2
      ;;
    --environment)
      ENVIRONMENT="$2"
      shift 2
      ;;
    --skip-terraform)
      SKIP_TERRAFORM=true
      shift
      ;;
    --skip-build)
      SKIP_BUILD=true
      shift
      ;;
    --skip-deploy)
      SKIP_DEPLOY=true
      shift
      ;;
    --help)
      sed -n '2,23p' "$0" | sed 's/^# \?//'
      exit 0
      ;;
    *)
      echo -e "${RED}Error: Unknown option: $1${NC}"
      echo "Run with --help for usage information"
      exit 1
      ;;
  esac
done

# Validate required arguments
if [ -z "$PROJECT_ID" ]; then
  echo -e "${RED}Error: --project-id is required${NC}"
  exit 1
fi

# ============================================================================
# Helper Functions
# ============================================================================

print_header() {
  echo -e "\n${BLUE}============================================================${NC}"
  echo -e "${BLUE}$1${NC}"
  echo -e "${BLUE}============================================================${NC}\n"
}

print_success() {
  echo -e "${GREEN}✓ $1${NC}"
}

print_warning() {
  echo -e "${YELLOW}⚠ $1${NC}"
}

print_error() {
  echo -e "${RED}✗ $1${NC}"
}

# ============================================================================
# Pre-flight Checks
# ============================================================================

print_header "Pre-flight Checks"

# Check if gcloud is installed
if ! command -v gcloud &> /dev/null; then
  print_error "gcloud CLI is not installed"
  echo "Install from: https://cloud.google.com/sdk/docs/install"
  exit 1
fi
print_success "gcloud CLI installed"

# Check if terraform is installed
if ! command -v terraform &> /dev/null; then
  print_warning "Terraform is not installed (required for infrastructure deployment)"
  if [ "$SKIP_TERRAFORM" = false ]; then
    echo "Install from: https://www.terraform.io/downloads"
    exit 1
  fi
else
  print_success "Terraform installed"
fi

# Check if docker is installed (for local builds)
if ! command -v docker &> /dev/null; then
  print_warning "Docker is not installed (Cloud Build will be used instead)"
else
  print_success "Docker installed"
fi

# Set GCP project
print_header "Setting GCP Project"
gcloud config set project "$PROJECT_ID"
print_success "Active project: $PROJECT_ID"

# ============================================================================
# Step 1: Create Artifact Registry Repository
# ============================================================================

print_header "Step 1: Creating Artifact Registry Repository"

# Create Artifact Registry repository if it doesn't exist
# This must exist before building images
echo "Ensuring Artifact Registry repository exists..."
gcloud artifacts repositories create oscal-tools \
  --repository-format=docker \
  --location="$REGION" \
  --description="OSCAL Tools container images" \
  2>/dev/null || print_warning "Repository already exists"

print_success "Artifact Registry repository ready"

# ============================================================================
# Step 2: Build and Push Container Images
# ============================================================================

if [ "$SKIP_BUILD" = false ]; then
  print_header "Step 2: Building and Pushing Container Images"

  # Build locally and push to Artifact Registry (more reliable than Cloud Build)
  echo "Building image locally and pushing to Artifact Registry..."
  ./build-and-push.sh "$PROJECT_ID" "$REGION"

  print_success "Container images built and pushed"
else
  print_warning "Skipping container builds (--skip-build)"
  print_warning "Make sure container images already exist in Artifact Registry!"
fi

# ============================================================================
# Step 3: Deploy Infrastructure with Terraform
# ============================================================================

if [ "$SKIP_TERRAFORM" = false ]; then
  print_header "Step 3: Deploying Infrastructure with Terraform"

  cd terraform/gcp

  # Initialize Terraform
  echo "Initializing Terraform..."
  terraform init

  # Create terraform.tfvars if it doesn't exist
  if [ ! -f "terraform.tfvars" ]; then
    echo "Creating terraform.tfvars..."
    cat > terraform.tfvars <<EOF
project_id  = "$PROJECT_ID"
region      = "$REGION"
environment = "$ENVIRONMENT"
EOF
  fi

  # Plan and apply
  echo "Planning infrastructure changes..."
  terraform plan -out=tfplan

  echo "Applying Terraform plan automatically..."
  if terraform apply -auto-approve tfplan; then
    print_success "Infrastructure deployed successfully"
  else
    # Terraform may timeout waiting for domain SSL certificates (10-15 min)
    # Check if the actual deployment succeeded despite timeout
    CLOUD_RUN_STATUS=$(gcloud run services describe oscal-tools-${ENVIRONMENT} --region ${REGION} --format="value(status.conditions[0].status)" 2>/dev/null || echo "Unknown")

    if [ "$CLOUD_RUN_STATUS" = "True" ]; then
      print_warning "Terraform timed out (likely waiting for domain SSL certificates)"
      print_success "Cloud Run service deployed successfully"
      print_warning "Domain SSL certificate provisioning continues in background (10-15 minutes)"
      echo "Check status: gcloud run domain-mappings list --region ${REGION}"
    else
      print_error "Terraform deployment failed"
      cd ../..
      exit 1
    fi
  fi

  cd ../..
else
  print_warning "Skipping Terraform deployment (--skip-terraform)"
fi

# ============================================================================
# Step 4: Get Deployment URLs
# ============================================================================

print_header "Step 4: Getting Deployment URLs"

# Get backend and frontend URLs from Terraform output
if [ -f "terraform/gcp/terraform.tfstate" ]; then
  cd terraform/gcp
  BACKEND_URL=$(terraform output -raw backend_url 2>/dev/null || echo "")
  FRONTEND_URL=$(terraform output -raw frontend_url 2>/dev/null || echo "")
  cd ../..

  if [ -n "$BACKEND_URL" ] && [ -n "$FRONTEND_URL" ]; then
    print_success "Backend URL:  $BACKEND_URL"
    print_success "Frontend URL: $FRONTEND_URL"
  fi
fi

print_success "Deployment complete"

# ============================================================================
# Final Summary
# ============================================================================

print_header "Deployment Summary"

echo -e "Project ID:   ${BLUE}$PROJECT_ID${NC}"
echo -e "Region:       ${BLUE}$REGION${NC}"
echo -e "Environment:  ${BLUE}$ENVIRONMENT${NC}"

if [ -n "${BACKEND_URL:-}" ]; then
  echo -e "\n${GREEN}Your OSCAL Tools application is now deployed!${NC}"
  echo -e "\nFrontend:  ${BLUE}$FRONTEND_URL${NC}"
  echo -e "Backend:   ${BLUE}$BACKEND_URL${NC}"
  echo -e "\nNext steps:"
  echo -e "1. Visit the frontend URL to access the application"
  echo -e "2. View logs: ${BLUE}gcloud logging read \"resource.type=cloud_run_revision\"${NC}"
  echo -e "3. Monitor: ${BLUE}https://console.cloud.google.com/run?project=$PROJECT_ID${NC}"
fi

print_header "Deployment Complete!"
