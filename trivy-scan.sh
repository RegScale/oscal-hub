#!/bin/bash

###############################################################################
# Trivy Security Scanner Script
#
# This script performs vulnerability scanning and SBOM generation using Trivy.
# It can scan the filesystem, Docker images, or both.
#
# Usage:
#   ./trivy-scan.sh [options]
#
# Options:
#   --type [fs|image|all]     Scan type (default: fs)
#   --severity [LEVEL]        Severity levels (default: HIGH,CRITICAL)
#   --output-dir [DIR]        Output directory (default: ./security-reports)
#   --sbom                    Generate SBOM (CycloneDX and SPDX)
#   --image [NAME]            Docker image name to scan
#   --help                    Show this help message
#
# Examples:
#   ./trivy-scan.sh                              # Scan filesystem for HIGH/CRITICAL
#   ./trivy-scan.sh --severity MEDIUM,HIGH,CRITICAL
#   ./trivy-scan.sh --sbom                       # Scan + generate SBOM
#   ./trivy-scan.sh --type image --image oscal-tools:latest
#   ./trivy-scan.sh --type all --sbom --image oscal-tools:latest
###############################################################################

set -e  # Exit on error

# Color codes for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Default configuration
SCAN_TYPE="fs"
SEVERITY="HIGH,CRITICAL"
OUTPUT_DIR="./security-reports"
GENERATE_SBOM=false
DOCKER_IMAGE=""
TRIVY_PATH="./tools/trivy"
SCAN_TARGET="./back-end"

# Script directory
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
cd "$SCRIPT_DIR"

###############################################################################
# Functions
###############################################################################

show_help() {
    cat << EOF
Trivy Security Scanner Script

Usage:
    ./trivy-scan.sh [options]

Options:
    --type [fs|image|all]     Scan type (default: fs)
                              fs    = Scan filesystem
                              image = Scan Docker image
                              all   = Scan both

    --severity [LEVEL]        Severity levels to report (default: HIGH,CRITICAL)
                              Options: UNKNOWN,LOW,MEDIUM,HIGH,CRITICAL

    --output-dir [DIR]        Output directory for reports (default: ./security-reports)

    --sbom                    Generate SBOM in CycloneDX and SPDX formats

    --image [NAME]            Docker image name to scan (required for image scans)

    --help                    Show this help message

Examples:
    # Scan filesystem for HIGH and CRITICAL vulnerabilities
    ./trivy-scan.sh

    # Include MEDIUM severity
    ./trivy-scan.sh --severity MEDIUM,HIGH,CRITICAL

    # Generate SBOM along with vulnerability scan
    ./trivy-scan.sh --sbom

    # Scan Docker image
    ./trivy-scan.sh --type image --image oscal-tools:latest

    # Full scan: filesystem + image + SBOM
    ./trivy-scan.sh --type all --sbom --image oscal-tools:latest

    # Custom output directory
    ./trivy-scan.sh --output-dir ./my-reports --sbom

EOF
}

log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

log_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

check_trivy() {
    if [ ! -f "$TRIVY_PATH" ]; then
        log_error "Trivy not found at $TRIVY_PATH"
        log_info "Installing Trivy..."

        # Create tools directory if it doesn't exist
        mkdir -p tools

        # Download and install Trivy
        curl -sfL https://raw.githubusercontent.com/aquasecurity/trivy/main/contrib/install.sh | sh -s -- -b ./tools v0.48.0

        if [ -f "$TRIVY_PATH" ]; then
            log_success "Trivy installed successfully"
        else
            log_error "Failed to install Trivy"
            exit 1
        fi
    fi

    # Show Trivy version
    TRIVY_VERSION=$($TRIVY_PATH --version | head -n1)
    log_info "Using $TRIVY_VERSION"
}

scan_filesystem() {
    log_info "Starting filesystem vulnerability scan..."
    log_info "Target: $SCAN_TARGET"
    log_info "Severity: $SEVERITY"

    # Create output directory
    mkdir -p "$OUTPUT_DIR"

    # Run vulnerability scan with table format
    echo ""
    $TRIVY_PATH fs \
        --severity "$SEVERITY" \
        --format table \
        "$SCAN_TARGET"

    # Generate detailed JSON report
    log_info "Generating detailed JSON report..."
    $TRIVY_PATH fs \
        --severity "$SEVERITY" \
        --format json \
        --output "$OUTPUT_DIR/vulnerability-report-fs.json" \
        "$SCAN_TARGET"

    # Generate HTML report (if available in this version)
    log_info "Generating HTML report..."
    $TRIVY_PATH fs \
        --severity "$SEVERITY" \
        --format template \
        --template "@contrib/html.tpl" \
        --output "$OUTPUT_DIR/vulnerability-report-fs.html" \
        "$SCAN_TARGET" 2>/dev/null || log_warning "HTML report generation not supported"

    log_success "Filesystem scan complete"
    log_info "Reports saved to: $OUTPUT_DIR/"
}

scan_docker_image() {
    if [ -z "$DOCKER_IMAGE" ]; then
        log_error "Docker image name not specified. Use --image option."
        exit 1
    fi

    log_info "Starting Docker image vulnerability scan..."
    log_info "Image: $DOCKER_IMAGE"
    log_info "Severity: $SEVERITY"

    # Create output directory
    mkdir -p "$OUTPUT_DIR"

    # Check if image exists locally
    if ! docker image inspect "$DOCKER_IMAGE" &>/dev/null; then
        log_warning "Image $DOCKER_IMAGE not found locally. Attempting to pull..."
        docker pull "$DOCKER_IMAGE" || {
            log_error "Failed to pull image $DOCKER_IMAGE"
            exit 1
        }
    fi

    # Run vulnerability scan with table format
    echo ""
    $TRIVY_PATH image \
        --severity "$SEVERITY" \
        --format table \
        "$DOCKER_IMAGE"

    # Generate detailed JSON report
    log_info "Generating detailed JSON report..."
    $TRIVY_PATH image \
        --severity "$SEVERITY" \
        --format json \
        --output "$OUTPUT_DIR/vulnerability-report-image.json" \
        "$DOCKER_IMAGE"

    # Generate HTML report
    log_info "Generating HTML report..."
    $TRIVY_PATH image \
        --severity "$SEVERITY" \
        --format template \
        --template "@contrib/html.tpl" \
        --output "$OUTPUT_DIR/vulnerability-report-image.html" \
        "$DOCKER_IMAGE" 2>/dev/null || log_warning "HTML report generation not supported"

    log_success "Docker image scan complete"
    log_info "Reports saved to: $OUTPUT_DIR/"
}

generate_sbom_filesystem() {
    log_info "Generating SBOM for filesystem..."

    # Create output directory
    mkdir -p "$OUTPUT_DIR"

    # Generate CycloneDX SBOM
    log_info "Generating CycloneDX SBOM..."
    $TRIVY_PATH fs \
        --format cyclonedx \
        --output "$OUTPUT_DIR/sbom-cyclonedx.json" \
        "$SCAN_TARGET"

    # Generate SPDX SBOM
    log_info "Generating SPDX SBOM..."
    $TRIVY_PATH fs \
        --format spdx-json \
        --output "$OUTPUT_DIR/sbom-spdx.json" \
        "$SCAN_TARGET"

    # Count components
    if command -v jq &> /dev/null; then
        CYCLONEDX_COUNT=$(jq -r '.components | length' "$OUTPUT_DIR/sbom-cyclonedx.json" 2>/dev/null || echo "N/A")
        SPDX_COUNT=$(jq -r '.packages | length' "$OUTPUT_DIR/sbom-spdx.json" 2>/dev/null || echo "N/A")
        log_success "SBOM generated: CycloneDX ($CYCLONEDX_COUNT components), SPDX ($SPDX_COUNT packages)"
    else
        log_success "SBOM files generated (install 'jq' to see component counts)"
    fi

    log_info "SBOM files saved to: $OUTPUT_DIR/"
}

generate_sbom_image() {
    if [ -z "$DOCKER_IMAGE" ]; then
        log_error "Docker image name not specified. Use --image option."
        exit 1
    fi

    log_info "Generating SBOM for Docker image..."

    # Create output directory
    mkdir -p "$OUTPUT_DIR"

    # Generate CycloneDX SBOM
    log_info "Generating CycloneDX SBOM..."
    $TRIVY_PATH image \
        --format cyclonedx \
        --output "$OUTPUT_DIR/sbom-image-cyclonedx.json" \
        "$DOCKER_IMAGE"

    # Generate SPDX SBOM
    log_info "Generating SPDX SBOM..."
    $TRIVY_PATH image \
        --format spdx-json \
        --output "$OUTPUT_DIR/sbom-image-spdx.json" \
        "$DOCKER_IMAGE"

    # Count components
    if command -v jq &> /dev/null; then
        CYCLONEDX_COUNT=$(jq -r '.components | length' "$OUTPUT_DIR/sbom-image-cyclonedx.json" 2>/dev/null || echo "N/A")
        SPDX_COUNT=$(jq -r '.packages | length' "$OUTPUT_DIR/sbom-image-spdx.json" 2>/dev/null || echo "N/A")
        log_success "SBOM generated: CycloneDX ($CYCLONEDX_COUNT components), SPDX ($SPDX_COUNT packages)"
    else
        log_success "SBOM files generated (install 'jq' to see component counts)"
    fi

    log_info "SBOM files saved to: $OUTPUT_DIR/"
}

print_summary() {
    echo ""
    echo "============================================"
    log_success "Trivy Scan Complete"
    echo "============================================"
    log_info "Output directory: $OUTPUT_DIR/"
    echo ""

    if [ -d "$OUTPUT_DIR" ]; then
        log_info "Generated files:"
        ls -lh "$OUTPUT_DIR/" | tail -n +2 | awk '{printf "  - %-40s %s\n", $9, $5}'
    fi

    echo ""
    log_info "Next steps:"
    echo "  1. Review vulnerability reports in $OUTPUT_DIR/"
    echo "  2. Fix HIGH/CRITICAL vulnerabilities in pom.xml"
    echo "  3. Share SBOM files with security/compliance teams"
    echo "  4. Integrate this script into CI/CD pipeline"
    echo ""
}

###############################################################################
# Parse command line arguments
###############################################################################

while [[ $# -gt 0 ]]; do
    case $1 in
        --type)
            SCAN_TYPE="$2"
            shift 2
            ;;
        --severity)
            SEVERITY="$2"
            shift 2
            ;;
        --output-dir)
            OUTPUT_DIR="$2"
            shift 2
            ;;
        --sbom)
            GENERATE_SBOM=true
            shift
            ;;
        --image)
            DOCKER_IMAGE="$2"
            shift 2
            ;;
        --help)
            show_help
            exit 0
            ;;
        *)
            log_error "Unknown option: $1"
            show_help
            exit 1
            ;;
    esac
done

###############################################################################
# Main execution
###############################################################################

echo ""
echo "============================================"
echo "  Trivy Security Scanner"
echo "============================================"
echo ""

# Verify Trivy is installed
check_trivy

# Execute scans based on type
case $SCAN_TYPE in
    fs)
        scan_filesystem
        if [ "$GENERATE_SBOM" = true ]; then
            generate_sbom_filesystem
        fi
        ;;
    image)
        scan_docker_image
        if [ "$GENERATE_SBOM" = true ]; then
            generate_sbom_image
        fi
        ;;
    all)
        scan_filesystem
        scan_docker_image
        if [ "$GENERATE_SBOM" = true ]; then
            generate_sbom_filesystem
            generate_sbom_image
        fi
        ;;
    *)
        log_error "Invalid scan type: $SCAN_TYPE"
        log_info "Valid types: fs, image, all"
        exit 1
        ;;
esac

# Print summary
print_summary

exit 0
