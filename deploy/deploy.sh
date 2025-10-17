#!/bin/bash
set -e

# ==============================================================================
# OSCAL CLI Deployment Script
# ==============================================================================
# This script automates the deployment of OSCAL CLI to Azure Container Apps
# Usage: ./deploy.sh [environment] [action]
#   environment: dev, staging, prod (default: dev)
#   action: plan, apply, destroy (default: apply)
# ==============================================================================

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
ENVIRONMENT="${1:-dev}"
ACTION="${2:-apply}"

# Validation
VALID_ENVIRONMENTS=("dev" "staging" "prod")
VALID_ACTIONS=("plan" "apply" "destroy")

if [[ ! " ${VALID_ENVIRONMENTS[@]} " =~ " ${ENVIRONMENT} " ]]; then
    echo -e "${RED}Error: Invalid environment '${ENVIRONMENT}'${NC}"
    echo "Valid environments: ${VALID_ENVIRONMENTS[@]}"
    exit 1
fi

if [[ ! " ${VALID_ACTIONS[@]} " =~ " ${ACTION} " ]]; then
    echo -e "${RED}Error: Invalid action '${ACTION}'${NC}"
    echo "Valid actions: ${VALID_ACTIONS[@]}"
    exit 1
fi

# Functions
print_header() {
    echo -e "\n${BLUE}========================================${NC}"
    echo -e "${BLUE}$1${NC}"
    echo -e "${BLUE}========================================${NC}\n"
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

check_prerequisites() {
    print_header "Checking Prerequisites"

    # Check Azure CLI
    if ! command -v az &> /dev/null; then
        print_error "Azure CLI is not installed"
        echo "Install from: https://docs.microsoft.com/en-us/cli/azure/install-azure-cli"
        exit 1
    fi
    print_success "Azure CLI installed"

    # Check Terraform
    if ! command -v terraform &> /dev/null; then
        print_error "Terraform is not installed"
        echo "Install from: https://www.terraform.io/downloads"
        exit 1
    fi
    print_success "Terraform installed"

    # Check Docker
    if ! command -v docker &> /dev/null; then
        print_error "Docker is not installed"
        echo "Install from: https://docs.docker.com/get-docker/"
        exit 1
    fi
    print_success "Docker installed"

    # Check Azure login
    if ! az account show &> /dev/null; then
        print_error "Not logged in to Azure"
        echo "Run: az login"
        exit 1
    fi
    print_success "Logged in to Azure"

    local subscription_name=$(az account show --query name -o tsv)
    print_success "Using subscription: $subscription_name"
}

init_terraform() {
    print_header "Initializing Terraform"

    cd "$SCRIPT_DIR"

    terraform init

    print_success "Terraform initialized"
}

build_docker_image() {
    print_header "Building Docker Image"

    cd "$PROJECT_ROOT"

    # Build the Docker image
    docker build -t oscal-ux:latest .

    print_success "Docker image built: oscal-ux:latest"
}

terraform_plan() {
    print_header "Planning Terraform Deployment"

    cd "$SCRIPT_DIR"

    local var_file="environments/${ENVIRONMENT}.tfvars"

    if [[ ! -f "$var_file" ]]; then
        print_error "Environment file not found: $var_file"
        exit 1
    fi

    terraform plan -var-file="$var_file" -out=tfplan

    print_success "Terraform plan created"
}

terraform_apply() {
    print_header "Applying Terraform Configuration"

    cd "$SCRIPT_DIR"

    # Run plan first
    terraform_plan

    # Confirm before applying
    if [[ "$ENVIRONMENT" == "prod" ]]; then
        print_warning "You are about to deploy to PRODUCTION"
        read -p "Are you sure you want to continue? (yes/no): " confirm
        if [[ "$confirm" != "yes" ]]; then
            print_warning "Deployment cancelled"
            exit 0
        fi
    fi

    # Apply the plan
    terraform apply tfplan

    print_success "Infrastructure deployed"
}

push_docker_image() {
    print_header "Pushing Docker Image to ACR"

    cd "$SCRIPT_DIR"

    # Get ACR details from Terraform output
    local acr_name=$(terraform output -raw container_registry_name)
    local acr_server=$(terraform output -raw container_registry_login_server)

    if [[ -z "$acr_name" ]]; then
        print_error "Could not get ACR name from Terraform output"
        exit 1
    fi

    # Login to ACR
    print_success "Logging in to ACR: $acr_name"
    az acr login --name "$acr_name"

    # Tag and push image
    local image_tag="${ENVIRONMENT}"
    local full_image_name="${acr_server}/oscal-ux:${image_tag}"

    print_success "Tagging image: $full_image_name"
    docker tag oscal-ux:latest "$full_image_name"

    print_success "Pushing image to ACR..."
    docker push "$full_image_name"

    # Also push as 'latest' for the environment
    docker tag oscal-ux:latest "${acr_server}/oscal-ux:latest"
    docker push "${acr_server}/oscal-ux:latest"

    print_success "Docker image pushed to ACR"
}

update_container_app() {
    print_header "Updating Container App"

    cd "$SCRIPT_DIR"

    # Get resource names
    local rg_name="rg-oscal-cli-${ENVIRONMENT}"
    local app_name="ca-oscal-cli-${ENVIRONMENT}"
    local acr_server=$(terraform output -raw container_registry_login_server)
    local image_name="${acr_server}/oscal-ux:${ENVIRONMENT}"

    # Update container app
    print_success "Updating Container App: $app_name"
    az containerapp update \
        --name "$app_name" \
        --resource-group "$rg_name" \
        --image "$image_name"

    print_success "Container App updated"
}

get_app_url() {
    cd "$SCRIPT_DIR"

    local app_url=$(terraform output -raw application_url 2>/dev/null || echo "")

    if [[ -n "$app_url" ]]; then
        print_success "Application URL: $app_url"
        return 0
    else
        print_warning "Could not retrieve application URL"
        return 1
    fi
}

terraform_destroy() {
    print_header "Destroying Infrastructure"

    cd "$SCRIPT_DIR"

    local var_file="environments/${ENVIRONMENT}.tfvars"

    # Strong confirmation for destroy
    print_warning "You are about to DESTROY all resources in $ENVIRONMENT environment"
    read -p "Type 'destroy-$ENVIRONMENT' to confirm: " confirm
    if [[ "$confirm" != "destroy-$ENVIRONMENT" ]]; then
        print_warning "Destroy cancelled"
        exit 0
    fi

    terraform destroy -var-file="$var_file" -auto-approve

    print_success "Infrastructure destroyed"
}

show_summary() {
    print_header "Deployment Summary"

    echo -e "${GREEN}Environment:${NC} $ENVIRONMENT"
    echo -e "${GREEN}Action:${NC} $ACTION"
    echo ""

    if [[ "$ACTION" != "destroy" ]]; then
        get_app_url
    fi
}

# Main execution
main() {
    print_header "OSCAL CLI Deployment - $ENVIRONMENT ($ACTION)"

    check_prerequisites

    case "$ACTION" in
        plan)
            init_terraform
            terraform_plan
            ;;
        apply)
            init_terraform
            build_docker_image
            terraform_apply
            push_docker_image
            update_container_app
            show_summary
            ;;
        destroy)
            init_terraform
            terraform_destroy
            ;;
    esac

    print_success "Deployment script completed successfully!"
}

# Run main function
main
