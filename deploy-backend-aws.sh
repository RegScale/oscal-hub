#!/bin/bash
set -e

###############################################################################
# OSCAL Tools - AWS Elastic Beanstalk Deployment Script
#
# This script builds and deploys the Spring Boot backend to AWS Elastic Beanstalk
#
# Prerequisites:
#   - AWS CLI configured with appropriate credentials
#   - EB CLI installed: pip install awsebcli
#   - Java 21 and Maven installed
#
# Usage:
#   ./deploy-backend-aws.sh [environment]
#
# Arguments:
#   environment - dev, staging, or prod (default: staging)
#
###############################################################################

# Configuration
ENVIRONMENT=${1:-staging}
APP_NAME="oscal-api"
REGION="us-east-1"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Helper functions
log_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Validate environment
if [[ ! "$ENVIRONMENT" =~ ^(dev|staging|prod)$ ]]; then
    log_error "Invalid environment: $ENVIRONMENT"
    echo "Usage: $0 [dev|staging|prod]"
    exit 1
fi

log_info "Deploying to environment: $ENVIRONMENT"

# Navigate to backend directory
cd "$(dirname "$0")/back-end"

###############################################################################
# Step 1: Verify Prerequisites
###############################################################################

log_info "Verifying prerequisites..."

# Check AWS CLI
if ! command -v aws &> /dev/null; then
    log_error "AWS CLI is not installed. Install it first: https://aws.amazon.com/cli/"
    exit 1
fi

# Check EB CLI
if ! command -v eb &> /dev/null; then
    log_error "EB CLI is not installed. Install it with: pip install awsebcli"
    exit 1
fi

# Check Maven
if ! command -v mvn &> /dev/null; then
    log_error "Maven is not installed"
    exit 1
fi

# Verify AWS credentials
if ! aws sts get-caller-identity &> /dev/null; then
    log_error "AWS credentials not configured. Run 'aws configure'"
    exit 1
fi

log_info "Prerequisites verified ✓"

###############################################################################
# Step 2: Build Application
###############################################################################

log_info "Building Spring Boot application..."

# Clean and build
mvn clean package -DskipTests

if [ $? -ne 0 ]; then
    log_error "Maven build failed"
    exit 1
fi

# Verify JAR was created
JAR_FILE=$(ls target/*.jar 2>/dev/null | head -n 1)
if [ -z "$JAR_FILE" ]; then
    log_error "JAR file not found in target/"
    exit 1
fi

JAR_SIZE=$(du -h "$JAR_FILE" | cut -f1)
log_info "Build successful ✓ (JAR size: $JAR_SIZE)"

###############################################################################
# Step 3: Check Elastic Beanstalk Configuration
###############################################################################

log_info "Checking Elastic Beanstalk configuration..."

# Check if .elasticbeanstalk/config.yml exists
if [ ! -f ".elasticbeanstalk/config.yml" ]; then
    log_warn "Elastic Beanstalk not initialized"
    log_info "Initializing EB application..."

    eb init "$APP_NAME" \
        --platform "Corretto 21 running on 64bit Amazon Linux 2023" \
        --region "$REGION"

    log_info "EB application initialized ✓"
fi

###############################################################################
# Step 4: Deploy to Elastic Beanstalk
###############################################################################

ENV_NAME="$APP_NAME-$ENVIRONMENT"

log_info "Deploying to Elastic Beanstalk environment: $ENV_NAME"

# Check if environment exists
if eb list | grep -q "$ENV_NAME"; then
    log_info "Environment exists. Deploying update..."
    eb deploy "$ENV_NAME" --timeout 20
else
    log_warn "Environment does not exist: $ENV_NAME"
    log_info "Creating new environment..."

    # Prompt for environment variables
    read -p "Enter RDS database endpoint: " DB_ENDPOINT
    read -p "Enter database username: " DB_USERNAME
    read -sp "Enter database password: " DB_PASSWORD
    echo
    read -sp "Enter JWT secret (min 32 chars): " JWT_SECRET
    echo
    read -p "Enter CORS allowed origins (e.g., https://example.com): " CORS_ORIGINS

    # Create environment
    eb create "$ENV_NAME" \
        --instance-type t3.medium \
        --envvars \
            SPRING_PROFILES_ACTIVE=prod,\
            SERVER_PORT=5000,\
            AWS_REGION=$REGION,\
            AWS_S3_BUCKET_FILES=oscal-tools-files-$ENVIRONMENT,\
            AWS_S3_BUCKET_BUILD=oscal-tools-build-$ENVIRONMENT,\
            DB_URL=jdbc:postgresql://$DB_ENDPOINT:5432/postgres,\
            DB_USERNAME=$DB_USERNAME,\
            DB_PASSWORD=$DB_PASSWORD,\
            JWT_SECRET=$JWT_SECRET,\
            CORS_ALLOWED_ORIGINS=$CORS_ORIGINS \
        --timeout 20
fi

if [ $? -ne 0 ]; then
    log_error "Deployment failed"
    log_info "Check logs with: eb logs $ENV_NAME"
    exit 1
fi

###############################################################################
# Step 5: Verify Deployment
###############################################################################

log_info "Verifying deployment..."

# Get environment URL
EB_URL=$(eb status "$ENV_NAME" | grep "CNAME" | awk '{print $2}')

if [ -z "$EB_URL" ]; then
    log_error "Could not determine environment URL"
    exit 1
fi

log_info "Environment URL: https://$EB_URL"

# Wait for health check
log_info "Waiting for application to become healthy (this may take 2-3 minutes)..."
sleep 30

# Test health endpoint
HEALTH_URL="https://$EB_URL/actuator/health"
HTTP_STATUS=$(curl -s -o /dev/null -w "%{http_code}" "$HEALTH_URL" || echo "000")

if [ "$HTTP_STATUS" = "200" ]; then
    log_info "Health check passed ✓"
    log_info "Application is running at: https://$EB_URL"

    # Display additional info
    echo
    echo "═══════════════════════════════════════════════════════════"
    echo "  Deployment Summary"
    echo "═══════════════════════════════════════════════════════════"
    echo "  Environment:     $ENVIRONMENT"
    echo "  EB Environment:  $ENV_NAME"
    echo "  URL:             https://$EB_URL"
    echo "  Health:          $HEALTH_URL"
    echo "  API Docs:        https://$EB_URL/swagger-ui.html"
    echo "═══════════════════════════════════════════════════════════"
    echo
    echo "Next steps:"
    echo "  1. Configure custom domain in Route 53"
    echo "  2. Update SSL certificate in .ebextensions/03-https-redirect.config"
    echo "  3. Update frontend NEXT_PUBLIC_API_URL to https://$EB_URL"
    echo "  4. Monitor logs: eb logs $ENV_NAME --stream"
    echo
else
    log_warn "Health check returned HTTP $HTTP_STATUS"
    log_info "Check application logs with: eb logs $ENV_NAME"
    log_info "The application may still be starting up. Check again in a few minutes."
fi

###############################################################################
# Step 6: Post-Deployment Tasks
###############################################################################

log_info "Deployment complete!"
log_info "Monitor your environment:"
echo "  - View logs:   eb logs $ENV_NAME --stream"
echo "  - Check status: eb status $ENV_NAME"
echo "  - Open console: eb console $ENV_NAME"
echo "  - SSH to instance: eb ssh $ENV_NAME"

exit 0
