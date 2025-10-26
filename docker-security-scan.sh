#!/bin/bash
# ==============================================================================
# Docker Security Scanning Script
# ==============================================================================
# This script performs comprehensive security scanning of Docker images
# Supports multiple scanning tools: Trivy, Grype, Docker Bench
# ==============================================================================

set -euo pipefail

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
IMAGE_NAME="${1:-oscal-ux:dev}"
REPORT_DIR="./security-reports"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
SEVERITY_THRESHOLD="${SEVERITY_THRESHOLD:-MEDIUM}"

# ==============================================================================
# Helper Functions
# ==============================================================================

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

check_command() {
    if ! command -v "$1" &> /dev/null; then
        return 1
    fi
    return 0
}

# ==============================================================================
# Tool Installation Checks
# ==============================================================================

check_tools() {
    print_header "Checking Security Tools"

    local missing_tools=()

    # Check Trivy
    if check_command trivy; then
        print_success "Trivy is installed ($(trivy --version | head -n1))"
    else
        print_warning "Trivy is not installed"
        missing_tools+=("trivy")
    fi

    # Check Grype
    if check_command grype; then
        print_success "Grype is installed ($(grype version | grep Version | awk '{print $2}'))"
    else
        print_warning "Grype is not installed"
        missing_tools+=("grype")
    fi

    # Check Docker Bench
    if [ -f "./docker-bench-security/docker-bench-security.sh" ]; then
        print_success "Docker Bench Security is available"
    else
        print_warning "Docker Bench Security is not available"
        missing_tools+=("docker-bench")
    fi

    # Check Docker
    if check_command docker; then
        print_success "Docker is installed ($(docker --version))"
    else
        print_error "Docker is not installed - REQUIRED"
        exit 1
    fi

    if [ ${#missing_tools[@]} -gt 0 ]; then
        echo ""
        print_warning "Some tools are missing. Install them for complete scanning:"
        for tool in "${missing_tools[@]}"; do
            case $tool in
                trivy)
                    echo "  • Trivy: https://aquasecurity.github.io/trivy/latest/getting-started/installation/"
                    echo "    brew install trivy  (macOS)"
                    ;;
                grype)
                    echo "  • Grype: https://github.com/anchore/grype#installation"
                    echo "    brew install grype  (macOS)"
                    ;;
                docker-bench)
                    echo "  • Docker Bench: git clone https://github.com/docker/docker-bench-security.git"
                    ;;
            esac
        done
        echo ""
    fi
}

# ==============================================================================
# Image Scanning Functions
# ==============================================================================

scan_with_trivy() {
    if ! check_command trivy; then
        print_warning "Skipping Trivy scan (not installed)"
        return 0
    fi

    print_header "Scanning with Trivy"

    local json_report="${REPORT_DIR}/trivy_${TIMESTAMP}.json"
    local html_report="${REPORT_DIR}/trivy_${TIMESTAMP}.html"
    local table_report="${REPORT_DIR}/trivy_${TIMESTAMP}.txt"

    echo "Scanning image: ${IMAGE_NAME}"
    echo "Severity threshold: ${SEVERITY_THRESHOLD}"
    echo ""

    # Update vulnerability database
    print_success "Updating Trivy vulnerability database..."
    trivy image --download-db-only

    # Scan for vulnerabilities (JSON)
    print_success "Scanning for vulnerabilities (JSON report)..."
    trivy image \
        --severity "${SEVERITY_THRESHOLD},HIGH,CRITICAL" \
        --format json \
        --output "${json_report}" \
        "${IMAGE_NAME}"

    # Scan for vulnerabilities (HTML)
    print_success "Generating HTML report..."
    trivy image \
        --severity "${SEVERITY_THRESHOLD},HIGH,CRITICAL" \
        --format template \
        --template '@contrib/html.tpl' \
        --output "${html_report}" \
        "${IMAGE_NAME}"

    # Scan for vulnerabilities (Table)
    print_success "Generating table report..."
    trivy image \
        --severity "${SEVERITY_THRESHOLD},HIGH,CRITICAL" \
        --format table \
        --output "${table_report}" \
        "${IMAGE_NAME}"

    # Display summary
    echo ""
    print_success "Trivy scan complete!"
    echo "  • JSON report: ${json_report}"
    echo "  • HTML report: ${html_report}"
    echo "  • Table report: ${table_report}"

    # Show critical/high vulnerabilities
    local critical=$(grep -c "\"Severity\": \"CRITICAL\"" "${json_report}" || echo "0")
    local high=$(grep -c "\"Severity\": \"HIGH\"" "${json_report}" || echo "0")

    echo ""
    if [ "$critical" -gt 0 ] || [ "$high" -gt 0 ]; then
        print_warning "Found vulnerabilities:"
        echo "  • CRITICAL: ${critical}"
        echo "  • HIGH: ${high}"
    else
        print_success "No CRITICAL or HIGH vulnerabilities found!"
    fi
}

scan_with_grype() {
    if ! check_command grype; then
        print_warning "Skipping Grype scan (not installed)"
        return 0
    fi

    print_header "Scanning with Grype"

    local json_report="${REPORT_DIR}/grype_${TIMESTAMP}.json"
    local table_report="${REPORT_DIR}/grype_${TIMESTAMP}.txt"

    echo "Scanning image: ${IMAGE_NAME}"
    echo ""

    # Update vulnerability database
    print_success "Updating Grype vulnerability database..."
    grype db update

    # Scan for vulnerabilities (JSON)
    print_success "Scanning for vulnerabilities (JSON report)..."
    grype "${IMAGE_NAME}" \
        --output json \
        --file "${json_report}"

    # Scan for vulnerabilities (Table)
    print_success "Generating table report..."
    grype "${IMAGE_NAME}" \
        --output table \
        --file "${table_report}"

    # Display summary
    echo ""
    print_success "Grype scan complete!"
    echo "  • JSON report: ${json_report}"
    echo "  • Table report: ${table_report}"
}

scan_dockerfile() {
    print_header "Scanning Dockerfile Best Practices"

    local report="${REPORT_DIR}/dockerfile_analysis_${TIMESTAMP}.txt"

    echo "Analyzing Dockerfiles for security best practices..." > "${report}"
    echo "" >> "${report}"

    # Find all Dockerfiles
    local dockerfiles=$(find . -name "Dockerfile*" -not -path "*/node_modules/*" -not -path "*/.git/*")

    for dockerfile in ${dockerfiles}; do
        echo "=== Analyzing: ${dockerfile} ===" >> "${report}"
        echo "" >> "${report}"

        # Check for non-root user
        if grep -q "USER" "${dockerfile}"; then
            echo "✓ Non-root user specified" >> "${report}"
        else
            echo "✗ WARNING: No USER instruction found (running as root)" >> "${report}"
        fi

        # Check for specific version tags
        if grep -q ":latest" "${dockerfile}"; then
            echo "✗ WARNING: Using 'latest' tag (use specific versions)" >> "${report}"
        else
            echo "✓ Using specific version tags" >> "${report}"
        fi

        # Check for HEALTHCHECK
        if grep -q "HEALTHCHECK" "${dockerfile}"; then
            echo "✓ HEALTHCHECK instruction present" >> "${report}"
        else
            echo "✗ WARNING: No HEALTHCHECK instruction" >> "${report}"
        fi

        # Check for security options
        if grep -q "no-new-privileges" "${dockerfile}"; then
            echo "✓ Security options configured" >> "${report}"
        fi

        # Check for secrets in ENV
        if grep -E "ENV.*(PASSWORD|SECRET|KEY|TOKEN)" "${dockerfile}" | grep -v "REQUIRED\|your-.*-here\|\$\{"; then
            echo "✗ CRITICAL: Potential hardcoded secrets in ENV" >> "${report}"
        fi

        echo "" >> "${report}"
    done

    print_success "Dockerfile analysis complete: ${report}"
    cat "${report}"
}

run_docker_bench() {
    if [ ! -f "./docker-bench-security/docker-bench-security.sh" ]; then
        print_warning "Skipping Docker Bench (not installed)"
        echo "Install with: git clone https://github.com/docker/docker-bench-security.git"
        return 0
    fi

    print_header "Running Docker Bench Security"

    local report="${REPORT_DIR}/docker_bench_${TIMESTAMP}.log"

    print_success "Running Docker Bench Security audit..."
    sudo ./docker-bench-security/docker-bench-security.sh | tee "${report}"

    print_success "Docker Bench report: ${report}"
}

check_image_configuration() {
    print_header "Checking Image Configuration"

    local report="${REPORT_DIR}/image_config_${TIMESTAMP}.txt"

    echo "=== Image Configuration Analysis ===" > "${report}"
    echo "" >> "${report}"

    # Check if image exists
    if ! docker image inspect "${IMAGE_NAME}" &> /dev/null; then
        print_error "Image ${IMAGE_NAME} not found. Build it first:"
        echo "  docker build -t ${IMAGE_NAME} ."
        return 1
    fi

    # Get image details
    echo "Image: ${IMAGE_NAME}" >> "${report}"
    echo "" >> "${report}"

    # Check user
    local user=$(docker image inspect "${IMAGE_NAME}" --format '{{.Config.User}}')
    if [ -z "$user" ] || [ "$user" = "root" ] || [ "$user" = "0" ]; then
        echo "✗ CRITICAL: Image runs as root" >> "${report}"
    else
        echo "✓ Image runs as non-root user: ${user}" >> "${report}"
    fi

    # Check exposed ports
    local ports=$(docker image inspect "${IMAGE_NAME}" --format '{{.Config.ExposedPorts}}')
    echo "" >> "${report}"
    echo "Exposed ports: ${ports}" >> "${report}"

    # Check environment variables (don't show values)
    echo "" >> "${report}"
    echo "Environment variables defined: $(docker image inspect "${IMAGE_NAME}" --format '{{len .Config.Env}}')" >> "${report}"

    # Check image size
    local size=$(docker image inspect "${IMAGE_NAME}" --format '{{.Size}}' | awk '{print $1/1024/1024 " MB"}')
    echo "Image size: ${size}" >> "${report}"

    # Check layers
    local layers=$(docker image inspect "${IMAGE_NAME}" --format '{{len .RootFS.Layers}}')
    echo "Number of layers: ${layers}" >> "${report}"

    print_success "Image configuration analysis complete: ${report}"
    cat "${report}"
}

# ==============================================================================
# Main Execution
# ==============================================================================

main() {
    print_header "Docker Security Scanner"
    echo "Image: ${IMAGE_NAME}"
    echo "Report directory: ${REPORT_DIR}"
    echo "Timestamp: ${TIMESTAMP}"

    # Create report directory
    mkdir -p "${REPORT_DIR}"

    # Check tools
    check_tools

    # Run scans
    scan_with_trivy
    scan_with_grype
    scan_dockerfile
    check_image_configuration
    run_docker_bench

    # Summary
    print_header "Security Scan Complete"
    echo "All reports saved to: ${REPORT_DIR}"
    echo ""
    echo "Next steps:"
    echo "  1. Review reports for vulnerabilities"
    echo "  2. Update base images and dependencies"
    echo "  3. Fix Dockerfile security issues"
    echo "  4. Re-run scan to verify fixes"
    echo ""
    print_success "Done!"
}

# Run main function
main
