#!/bin/bash
set -e

###############################################################################
# OSCAL Tools - Frontend Deployment to AWS S3 + CloudFront
#
# This script builds the Next.js frontend and deploys it to AWS S3,
# then invalidates the CloudFront cache
#
# Prerequisites:
#   - AWS CLI configured with appropriate credentials
#   - Node.js 18+ installed
#   - S3 bucket created
#   - CloudFront distribution created (optional)
#
# Usage:
#   ./deploy-frontend-aws.sh [environment]
#
# Arguments:
#   environment - dev, staging, or prod (default: staging)
#
###############################################################################

# Configuration
ENVIRONMENT=${1:-staging}
REGION="us-east-1"

# Environment-specific configuration
case $ENVIRONMENT in
    dev)
        BUCKET_NAME="oscal-tools-frontend-dev"
        API_URL="http://oscal-api-dev.us-east-1.elasticbeanstalk.com"
        CLOUDFRONT_DIST_ID=""  # No CloudFront for dev
        ;;
    staging)
        BUCKET_NAME="oscal-tools-frontend-staging"
        API_URL="https://api-staging.oscal-tools.com"
        CLOUDFRONT_DIST_ID="E1234567890ABC"  # Replace with your CloudFront ID
        ;;
    prod)
        BUCKET_NAME="oscal-tools-frontend-prod"
        API_URL="https://api.oscal-tools.com"
        CLOUDFRONT_DIST_ID="EXAMPLEABCDEF"  # Replace with your CloudFront ID
        ;;
    *)
        echo "Invalid environment: $ENVIRONMENT"
        echo "Usage: $0 [dev|staging|prod]"
        exit 1
        ;;
esac

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

log_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

log_info "Deploying frontend to environment: $ENVIRONMENT"

# Navigate to frontend directory
cd "$(dirname "$0")/front-end"

###############################################################################
# Step 1: Verify Prerequisites
###############################################################################

log_info "Verifying prerequisites..."

# Check AWS CLI
if ! command -v aws &> /dev/null; then
    log_error "AWS CLI is not installed"
    exit 1
fi

# Check Node.js
if ! command -v node &> /dev/null; then
    log_error "Node.js is not installed"
    exit 1
fi

# Check npm
if ! command -v npm &> /dev/null; then
    log_error "npm is not installed"
    exit 1
fi

# Verify AWS credentials
if ! aws sts get-caller-identity &> /dev/null; then
    log_error "AWS credentials not configured"
    exit 1
fi

log_info "Prerequisites verified ✓"

###############################################################################
# Step 2: Build Frontend
###############################################################################

log_info "Building Next.js application for $ENVIRONMENT..."

# Create production environment file
cat > .env.production << EOF
NEXT_PUBLIC_API_URL=$API_URL
NODE_ENV=production
EOF

log_info "API URL: $API_URL"

# Install dependencies
log_info "Installing dependencies..."
npm ci

if [ $? -ne 0 ]; then
    log_error "npm install failed"
    exit 1
fi

# Build Next.js
log_info "Building Next.js app..."
npm run build

if [ $? -ne 0 ]; then
    log_error "Next.js build failed"
    exit 1
fi

log_info "Build successful ✓"

###############################################################################
# Step 3: Check S3 Bucket
###############################################################################

log_info "Checking S3 bucket: $BUCKET_NAME"

# Check if bucket exists
if ! aws s3 ls "s3://$BUCKET_NAME" &> /dev/null; then
    log_warn "Bucket does not exist: $BUCKET_NAME"
    log_info "Creating bucket..."

    # Create bucket
    aws s3 mb "s3://$BUCKET_NAME" --region "$REGION"

    # Enable versioning
    aws s3api put-bucket-versioning \
        --bucket "$BUCKET_NAME" \
        --versioning-configuration Status=Enabled

    # Enable static website hosting
    aws s3 website "s3://$BUCKET_NAME" \
        --index-document index.html \
        --error-document 404.html

    # Configure public access (if using S3 static hosting without CloudFront)
    if [ -z "$CLOUDFRONT_DIST_ID" ]; then
        log_info "Configuring bucket for public website hosting..."

        # Bucket policy for public read access
        cat > /tmp/bucket-policy.json << EOF
{
    "Version": "2012-10-17",
    "Statement": [{
        "Sid": "PublicReadGetObject",
        "Effect": "Allow",
        "Principal": "*",
        "Action": "s3:GetObject",
        "Resource": "arn:aws:s3:::$BUCKET_NAME/*"
    }]
}
EOF

        aws s3api put-bucket-policy \
            --bucket "$BUCKET_NAME" \
            --policy file:///tmp/bucket-policy.json

        rm /tmp/bucket-policy.json
    fi

    log_info "Bucket created and configured ✓"
else
    log_info "Bucket exists ✓"
fi

###############################################################################
# Step 4: Deploy to S3
###############################################################################

log_info "Deploying to S3..."

# Determine output directory based on Next.js configuration
if [ -d "out" ]; then
    OUTPUT_DIR="out"
elif [ -d ".next" ]; then
    OUTPUT_DIR=".next"
else
    log_error "Build output directory not found"
    exit 1
fi

# Upload to S3 with optimized cache headers
log_info "Uploading files from $OUTPUT_DIR..."

# Upload immutable files with long cache (1 year)
aws s3 sync "$OUTPUT_DIR/_next/static" "s3://$BUCKET_NAME/_next/static" \
    --delete \
    --cache-control "public, max-age=31536000, immutable"

# Upload other static assets with shorter cache (1 day)
aws s3 sync "$OUTPUT_DIR" "s3://$BUCKET_NAME/" \
    --delete \
    --exclude "_next/static/*" \
    --cache-control "public, max-age=86400"

# Upload HTML files with short cache (5 minutes) for SSR
aws s3 sync "$OUTPUT_DIR" "s3://$BUCKET_NAME/" \
    --delete \
    --exclude "*" \
    --include "*.html" \
    --cache-control "public, max-age=300"

if [ $? -ne 0 ]; then
    log_error "S3 sync failed"
    exit 1
fi

log_info "Files uploaded to S3 ✓"

###############################################################################
# Step 5: Invalidate CloudFront Cache (if configured)
###############################################################################

if [ -n "$CLOUDFRONT_DIST_ID" ]; then
    log_info "Invalidating CloudFront cache..."

    INVALIDATION_ID=$(aws cloudfront create-invalidation \
        --distribution-id "$CLOUDFRONT_DIST_ID" \
        --paths "/*" \
        --query 'Invalidation.Id' \
        --output text)

    if [ $? -eq 0 ]; then
        log_info "CloudFront invalidation created: $INVALIDATION_ID ✓"
        log_info "Invalidation typically completes in 1-3 minutes"
    else
        log_warn "CloudFront invalidation failed (distribution may not exist yet)"
    fi
else
    log_info "No CloudFront distribution configured (skipping cache invalidation)"
fi

###############################################################################
# Step 6: Display Results
###############################################################################

log_info "Frontend deployment complete!"

echo
echo "═══════════════════════════════════════════════════════════"
echo "  Deployment Summary"
echo "═══════════════════════════════════════════════════════════"
echo "  Environment:     $ENVIRONMENT"
echo "  S3 Bucket:       $BUCKET_NAME"
echo "  API URL:         $API_URL"

if [ -n "$CLOUDFRONT_DIST_ID" ]; then
    # Get CloudFront domain name
    CF_DOMAIN=$(aws cloudfront get-distribution \
        --id "$CLOUDFRONT_DIST_ID" \
        --query 'Distribution.DomainName' \
        --output text 2>/dev/null || echo "unknown")

    echo "  CloudFront:      https://$CF_DOMAIN"
    echo "  Distribution ID: $CLOUDFRONT_DIST_ID"
else
    # S3 website endpoint
    S3_WEBSITE="http://$BUCKET_NAME.s3-website-$REGION.amazonaws.com"
    echo "  S3 Website:      $S3_WEBSITE"
fi

echo "═══════════════════════════════════════════════════════════"
echo
echo "Next steps:"
echo "  1. Test the deployment at the URL above"
echo "  2. Configure custom domain in Route 53 (if not done)"
echo "  3. Update DNS CNAME to point to CloudFront/S3"
echo "  4. Verify CORS settings allow your domain"
echo

exit 0
