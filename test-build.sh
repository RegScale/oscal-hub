#!/bin/bash
# Test script to build Docker image locally before deploying to GCP
set -e

echo "=========================================="
echo "  OSCAL Tools - Local Docker Build Test"
echo "=========================================="
echo ""

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

print_warning() {
    echo -e "${YELLOW}⚠ $1${NC}"
}

print_error() {
    echo -e "${RED}✗ $1${NC}"
}

# Check if Docker is running
print_step "Checking Docker..."
if ! docker info > /dev/null 2>&1; then
    print_error "Docker is not running. Please start Docker Desktop."
    exit 1
fi
print_success "Docker is running"
echo ""

# Show disk space
print_step "Checking available disk space..."
df -h . | tail -1
echo ""

# Enable BuildKit for better performance
export DOCKER_BUILDKIT=1
export BUILDKIT_PROGRESS=plain

print_step "Building Docker image (this may take 10-20 minutes on first build)..."
echo ""
echo "Build stages:"
echo "  1. Backend Maven build (~5-10 min)"
echo "  2. Frontend npm install (~3-5 min)"
echo "  3. Frontend Next.js build (~2-3 min)"
echo "  4. Runtime image assembly (~1-2 min)"
echo ""

# Build with progress output
docker build \
    --tag oscal-tools:test \
    --file Dockerfile \
    --progress=plain \
    .

BUILD_STATUS=$?

echo ""
if [ $BUILD_STATUS -eq 0 ]; then
    print_success "Docker build completed successfully!"
    echo ""

    # Show image info
    print_step "Image information:"
    docker images oscal-tools:test --format "table {{.Repository}}\t{{.Tag}}\t{{.Size}}\t{{.CreatedAt}}"
    echo ""

    # Show image layers
    print_step "Image layers:"
    docker history oscal-tools:test --no-trunc --human | head -20
    echo ""

    print_success "Build test passed! ✓"
    echo ""
    echo "Next steps:"
    echo "  1. Test locally:     docker run -p 3000:3000 -p 8080:8080 oscal-tools:test"
    echo "  2. Deploy to GCP:    gcloud builds submit --config=cloudbuild-images.yaml"
    echo ""
else
    print_error "Docker build failed!"
    echo ""
    echo "Common issues:"
    echo "  - Maven dependency download failures (check internet connection)"
    echo "  - npm install failures (try: rm -rf front-end/node_modules)"
    echo "  - Out of disk space (run: docker system prune -a)"
    echo "  - Memory limits (increase Docker Desktop memory to 8GB+)"
    echo ""
    exit 1
fi
