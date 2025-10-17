#!/usr/bin/env bash

# OSCAL CLI Installer for Mac/Linux
# This script downloads and installs the latest OSCAL CLI release

set -e

# Color codes for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
VERSION="${OSCAL_CLI_VERSION:-1.0.3}"
INSTALL_DIR="${OSCAL_CLI_HOME:-$HOME/.oscal-cli}"
MAVEN_BASE_URL="https://repo1.maven.org/maven2/gov/nist/secauto/oscal/tools/oscal-cli/cli-core"

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}OSCAL CLI Installer${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""

# Check if Java is installed
echo -e "${YELLOW}Checking Java installation...${NC}"
if ! command -v java &> /dev/null; then
    echo -e "${RED}Error: Java is not installed or not in PATH${NC}"
    echo -e "${YELLOW}Please install Java 11 or higher from:${NC}"
    echo -e "  - https://adoptium.net/ (recommended)"
    echo -e "  - https://www.oracle.com/java/technologies/downloads/"
    exit 1
fi

# Check Java version
JAVA_VERSION=$(java -version 2>&1 | head -1 | cut -d'"' -f2 | sed 's/\..*//')
if [ "$JAVA_VERSION" -lt 11 ]; then
    echo -e "${RED}Error: Java 11 or higher is required (found Java $JAVA_VERSION)${NC}"
    echo -e "${YELLOW}Please upgrade Java from: https://adoptium.net/${NC}"
    exit 1
fi

echo -e "${GREEN}✓ Java $JAVA_VERSION detected${NC}"
echo ""

# Create installation directory
echo -e "${YELLOW}Creating installation directory: $INSTALL_DIR${NC}"
mkdir -p "$INSTALL_DIR"
cd "$INSTALL_DIR"

# Download OSCAL CLI
echo -e "${YELLOW}Downloading OSCAL CLI version $VERSION...${NC}"
ZIP_FILE="cli-core-${VERSION}-oscal-cli.zip"
DOWNLOAD_URL="${MAVEN_BASE_URL}/${VERSION}/${ZIP_FILE}"

if command -v wget &> /dev/null; then
    wget -q --show-progress "$DOWNLOAD_URL" -O "$ZIP_FILE"
elif command -v curl &> /dev/null; then
    curl -L --progress-bar "$DOWNLOAD_URL" -o "$ZIP_FILE"
else
    echo -e "${RED}Error: Neither wget nor curl is available${NC}"
    echo -e "${YELLOW}Please install wget or curl and try again${NC}"
    exit 1
fi

if [ ! -f "$ZIP_FILE" ]; then
    echo -e "${RED}Error: Failed to download OSCAL CLI${NC}"
    exit 1
fi

echo -e "${GREEN}✓ Download complete${NC}"
echo ""

# Extract the archive
echo -e "${YELLOW}Extracting OSCAL CLI...${NC}"
if command -v unzip &> /dev/null; then
    unzip -q -o "$ZIP_FILE"
    rm "$ZIP_FILE"
else
    echo -e "${RED}Error: unzip is not available${NC}"
    echo -e "${YELLOW}Please install unzip and try again${NC}"
    exit 1
fi

echo -e "${GREEN}✓ Extraction complete${NC}"
echo ""

# Make the script executable
chmod +x "$INSTALL_DIR/bin/oscal-cli"

# Verify installation
echo -e "${YELLOW}Verifying installation...${NC}"
if "$INSTALL_DIR/bin/oscal-cli" --version &> /dev/null; then
    echo -e "${GREEN}✓ OSCAL CLI installed successfully!${NC}"
else
    echo -e "${RED}Error: Installation verification failed${NC}"
    exit 1
fi

echo ""
echo -e "${BLUE}========================================${NC}"
echo -e "${GREEN}Installation Complete!${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""
echo -e "${YELLOW}OSCAL CLI is installed at:${NC}"
echo -e "  $INSTALL_DIR"
echo ""
echo -e "${YELLOW}To use oscal-cli, you have two options:${NC}"
echo ""
echo -e "${BLUE}Option 1: Add to PATH (recommended)${NC}"
echo -e "Add the following line to your shell profile (~/.bashrc, ~/.zshrc, or ~/.bash_profile):"
echo ""
echo -e "${GREEN}  export PATH=\"\$PATH:$INSTALL_DIR/bin\"${NC}"
echo ""
echo -e "Then run: ${GREEN}source ~/.bashrc${NC} (or your shell profile)"
echo ""
echo -e "${BLUE}Option 2: Use full path${NC}"
echo -e "Run OSCAL CLI using the full path:"
echo -e "  ${GREEN}$INSTALL_DIR/bin/oscal-cli --help${NC}"
echo ""
echo -e "${YELLOW}Quick test:${NC}"
if echo "$PATH" | grep -q "$INSTALL_DIR/bin"; then
    echo -e "  ${GREEN}oscal-cli --version${NC}"
else
    echo -e "  ${GREEN}$INSTALL_DIR/bin/oscal-cli --version${NC}"
fi
echo ""
echo -e "${YELLOW}For usage guide, see:${NC} USER_GUIDE.md"
echo ""
