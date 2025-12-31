#!/bin/bash
# Build Docker image locally and push to GCP Artifact Registry
set -e

# Colors
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

print_step() {
    echo -e "${BLUE}▶ $1${NC}"
}

print_success() {
    echo -e "${GREEN}✓ $1${NC}"
}

print_error() {
    echo -e "${RED}✗ $1${NC}"
}

# Configuration
PROJECT_ID="${1:-oscal-hub}"
REGION="${2:-us-central1}"
REPOSITORY="oscal-tools"
IMAGE_NAME="oscal-tools"
TAG="${3:-latest}"

echo "=========================================="
echo "  Build and Push to Artifact Registry"
echo "=========================================="
echo ""
echo "Project:    $PROJECT_ID"
echo "Region:     $REGION"
echo "Repository: $REPOSITORY"
echo "Image:      $IMAGE_NAME:$TAG"
echo ""

# Full image path
IMAGE_PATH="${REGION}-docker.pkg.dev/${PROJECT_ID}/${REPOSITORY}/${IMAGE_NAME}:${TAG}"

# Step 1: Authenticate Docker with Artifact Registry
print_step "Authenticating Docker with Artifact Registry..."
gcloud auth configure-docker ${REGION}-docker.pkg.dev --quiet
print_success "Docker authenticated"
echo ""

# Step 2: Build image locally
print_step "Building Docker image locally..."
echo "This will take 10-20 minutes on first build..."
echo ""

# Generate timestamp to bust cache for source code changes
CACHEBUST=$(date +%s)
echo "Cache bust timestamp: $CACHEBUST"
echo ""

docker build \
    --build-arg CACHEBUST=${CACHEBUST} \
    --platform linux/amd64 \
    --tag ${IMAGE_PATH} \
    --tag ${IMAGE_NAME}:${TAG} \
    --file Dockerfile \
    .

BUILD_STATUS=$?

if [ $BUILD_STATUS -ne 0 ]; then
    print_error "Docker build failed!"
    exit 1
fi

print_success "Image built successfully"
echo ""

# Step 3: Show image info
print_step "Image information:"
docker images ${IMAGE_NAME}:${TAG} --format "table {{.Repository}}\t{{.Tag}}\t{{.Size}}\t{{.CreatedAt}}"
echo ""

# Step 4: Push to Artifact Registry
print_step "Pushing image to Artifact Registry..."
echo "Pushing to: ${IMAGE_PATH}"
echo ""

docker push ${IMAGE_PATH}

PUSH_STATUS=$?

if [ $PUSH_STATUS -ne 0 ]; then
    print_error "Docker push failed!"
    echo ""
    echo "Common issues:"
    echo "  - Artifact Registry repository doesn't exist"
    echo "    Run: gcloud artifacts repositories create ${REPOSITORY} --repository-format=docker --location=${REGION}"
    echo "  - Authentication expired"
    echo "    Run: gcloud auth login"
    echo "  - Insufficient permissions"
    echo "    Ensure you have 'Artifact Registry Writer' role"
    exit 1
fi

print_success "Image pushed successfully!"
echo ""

# Step 5: Verify image in registry
print_step "Verifying image in Artifact Registry..."
gcloud artifacts docker images list \
    ${REGION}-docker.pkg.dev/${PROJECT_ID}/${REPOSITORY} \
    --include-tags \
    --filter="${IMAGE_NAME}" \
    --format="table(package,version,CREATE_TIME)" \
    --limit=5

echo ""
print_success "Build and push complete! ✓"
echo ""
echo "Image: ${IMAGE_PATH}"
echo ""
echo "Next steps:"
echo "  1. Deploy with Terraform:"
echo "     cd terraform/gcp"
echo "     terraform apply"
echo ""
echo "  2. Or update existing deployment:"
echo "     gcloud run deploy oscal-tools-prod \\"
echo "       --image ${IMAGE_PATH} \\"
echo "       --region ${REGION}"
echo ""
