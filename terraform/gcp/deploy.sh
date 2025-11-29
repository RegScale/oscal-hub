#!/bin/bash
# ============================================================================#
# OSCAL Tools - Terraform Deployment Script
# ============================================================================#
# This script deploys OSCAL Tools infrastructure using Terraform
# Run this from the terraform/gcp directory
#
# Prerequisites:
#   - gcloud CLI authenticated (gcloud auth application-default login)
#   - Container images already built and pushed to Artifact Registry
#
# Usage:
#   ./deploy.sh [apply|plan|destroy]
# ============================================================================#

set -e

# Colors
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

# Helper functions
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

# Determine action
ACTION="${1:-plan}"

# Validate action
case $ACTION in
  plan|apply|destroy)
    ;;
  *)
    print_error "Invalid action: $ACTION"
    echo "Usage: $0 [plan|apply|destroy]"
    exit 1
    ;;
esac

# ============================================================================
# Pre-flight Checks
# ============================================================================

print_header "Pre-flight Checks"

# Check if terraform is installed
if ! command -v terraform &> /dev/null; then
  print_error "Terraform is not installed"
  echo "Install from: https://www.terraform.io/downloads"
  exit 1
fi
print_success "Terraform installed: $(terraform version -json | jq -r .terraform_version)"

# Check if gcloud is authenticated
if ! gcloud auth application-default print-access-token &> /dev/null; then
  print_error "Not authenticated with gcloud"
  echo "Run: gcloud auth application-default login"
  exit 1
fi
print_success "GCP authentication verified"

# Check if terraform.tfvars exists
if [ ! -f "terraform.tfvars" ]; then
  print_warning "terraform.tfvars not found"
  echo ""
  echo "Creating terraform.tfvars from template..."
  echo "Please edit terraform.tfvars with your project details:"
  echo ""

  cat > terraform.tfvars <<'EOF'
# GCP Project Configuration
project_id  = "your-project-id"  # TODO: Set your GCP project ID
region      = "us-central1"
environment = "prod"

# Database Configuration (optional overrides)
# db_name     = "oscal"
# db_username = "oscal-admin"
# db_tier     = "db-f1-micro"

# Cloud Run Configuration (optional overrides)
# backend_max_instances  = 10
# backend_min_instances  = 0
# frontend_max_instances = 10
# frontend_min_instances = 0
EOF

  print_error "Please edit terraform.tfvars before continuing"
  exit 1
fi
print_success "terraform.tfvars found"

# Get project ID from tfvars
PROJECT_ID=$(grep "project_id" terraform.tfvars | cut -d '"' -f 2)
if [ "$PROJECT_ID" = "your-project-id" ]; then
  print_error "Please set your project_id in terraform.tfvars"
  exit 1
fi

print_success "Project ID: $PROJECT_ID"

# Set gcloud project
gcloud config set project "$PROJECT_ID" --quiet

# ============================================================================
# Terraform Actions
# ============================================================================

print_header "Terraform: $ACTION"

# Initialize if needed
if [ ! -d ".terraform" ]; then
  echo "Initializing Terraform..."
  terraform init
fi

case $ACTION in
  plan)
    terraform plan
    ;;

  apply)
    echo "Planning deployment..."
    terraform plan -out=tfplan
    echo ""
    read -p "Apply this plan? (yes/no): " confirm
    if [ "$confirm" = "yes" ]; then
      terraform apply tfplan
      rm -f tfplan
      print_success "Deployment complete!"
      echo ""
      terraform output next_steps
    else
      print_warning "Deployment cancelled"
      rm -f tfplan
    fi
    ;;

  destroy)
    print_warning "This will destroy ALL infrastructure!"
    echo ""
    read -p "Are you sure? Type 'yes' to confirm: " confirm
    if [ "$confirm" = "yes" ]; then
      terraform destroy
      print_success "Infrastructure destroyed"
    else
      print_warning "Destroy cancelled"
    fi
    ;;
esac

# ============================================================================
# Post-deployment Information
# ============================================================================

if [ "$ACTION" = "apply" ] && [ "$confirm" = "yes" ]; then
  print_header "Connection Information"

  echo "To get your database password:"
  echo -e "  ${BLUE}terraform output -raw database_password${NC}"
  echo ""

  echo "To connect to Cloud SQL locally:"
  echo -e "  ${BLUE}cloud-sql-proxy \$(terraform output -raw database_connection_name)${NC}"
  echo -e "  ${BLUE}PGPASSWORD=\$(terraform output -raw database_password) psql \"host=127.0.0.1 user=\$(terraform output -raw database_username) dbname=\$(terraform output -raw database_name)\"${NC}"
  echo ""

  echo "View in GCP Console:"
  echo -e "  Cloud Run:  ${BLUE}https://console.cloud.google.com/run?project=$PROJECT_ID${NC}"
  echo -e "  Cloud SQL:  ${BLUE}https://console.cloud.google.com/sql?project=$PROJECT_ID${NC}"
  echo ""
fi
