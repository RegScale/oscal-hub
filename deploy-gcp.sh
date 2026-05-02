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
PROJECT_ID="oscal-hub"
REGION="us-central1"
ENVIRONMENT="prod"
SKIP_TERRAFORM=false
SKIP_BUILD=false
SKIP_DEPLOY=false
IMAGE_TAG=$(date +%Y%m%d-%H%M%S)

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

# Project ID now defaults to oscal-hub (can be overridden with --project-id)
echo -e "${GREEN}Using project: $PROJECT_ID${NC}"

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
  echo "Image tag: $IMAGE_TAG"
  ./build-and-push.sh "$PROJECT_ID" "$REGION" "$IMAGE_TAG"

  print_success "Container images built and pushed with tag: $IMAGE_TAG"
else
  print_warning "Skipping container builds (--skip-build)"
  print_warning "Make sure container images already exist in Artifact Registry!"
fi

# ============================================================================
# Step 3: Deploy to Cloud Run (Update image only - preserves env vars)
# ============================================================================

print_header "Step 3: Deploying to Cloud Run"

# Full image path that was just built
FULL_IMAGE="${REGION}-docker.pkg.dev/${PROJECT_ID}/oscal-tools/oscal-tools:${IMAGE_TAG}"

echo "Deploying image: $FULL_IMAGE"
echo ""

# Update the image and ensure DB_DDL_AUTO is set
gcloud run services update oscal-tools-${ENVIRONMENT} \
  --image="$FULL_IMAGE" \
  --region=${REGION} \
  --update-env-vars="DB_DDL_AUTO=update" \
  --quiet

DEPLOY_STATUS=$?

if [ $DEPLOY_STATUS -ne 0 ]; then
  print_error "Cloud Run update failed! Trying full deploy..."

  # Fallback: full deploy (for first-time deployments)
  gcloud run deploy oscal-tools-${ENVIRONMENT} \
    --image="$FULL_IMAGE" \
    --region=${REGION} \
    --platform=managed \
    --allow-unauthenticated \
    --quiet

  DEPLOY_STATUS=$?
  if [ $DEPLOY_STATUS -ne 0 ]; then
    print_error "Deployment failed!"
    exit 1
  fi
fi

print_success "Cloud Run deployment completed"

# Verify deployment
print_header "Verifying Deployment"

# Wait a moment for the new revision to be ready
sleep 5

DEPLOYED_IMAGE=$(gcloud run services describe oscal-tools-${ENVIRONMENT} --region ${REGION} --format='value(spec.template.spec.containers[0].image)' 2>/dev/null)

echo ""
echo "Expected: $FULL_IMAGE"
echo "Deployed: $DEPLOYED_IMAGE"
echo ""

if [[ "$DEPLOYED_IMAGE" == *"$IMAGE_TAG"* ]]; then
  print_success "✓ Correct image is now running!"
else
  print_error "Image mismatch! Something went wrong."
  echo "The deployed image doesn't match what we built."
  exit 1
fi

# ============================================================================
# Step 4: Get Deployment URL
# ============================================================================

print_header "Step 4: Deployment Summary"

# Get the service URL directly from Cloud Run
SERVICE_URL=$(gcloud run services describe oscal-tools-${ENVIRONMENT} --region ${REGION} --format='value(status.url)' 2>/dev/null || echo "")

echo -e "Project ID:   ${BLUE}$PROJECT_ID${NC}"
echo -e "Region:       ${BLUE}$REGION${NC}"
echo -e "Environment:  ${BLUE}$ENVIRONMENT${NC}"
echo -e "Image Tag:    ${BLUE}$IMAGE_TAG${NC}"

if [ -n "$SERVICE_URL" ]; then
  echo -e "\n${GREEN}Your OSCAL Tools application is now deployed!${NC}"
  echo -e "\nApplication URL: ${BLUE}$SERVICE_URL${NC}"
  echo -e "API Endpoint:    ${BLUE}$SERVICE_URL/api${NC}"
  echo -e "\nNext steps:"
  echo -e "1. Visit the URL above to access the application"
  echo -e "2. View logs: ${BLUE}gcloud logging read 'resource.type=cloud_run_revision' --limit=50${NC}"
  echo -e "3. Monitor: ${BLUE}https://console.cloud.google.com/run?project=$PROJECT_ID${NC}"
fi

print_header "Deployment Complete!"
