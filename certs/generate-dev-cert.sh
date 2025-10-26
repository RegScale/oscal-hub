#!/bin/bash

# =============================================================================
# Self-Signed Certificate Generator for Development
# =============================================================================
# This script generates a self-signed SSL/TLS certificate for local development
#
# WARNING: Self-signed certificates should ONLY be used for development
# For production, use certificates from a trusted CA (Let's Encrypt, etc.)
# =============================================================================

set -e

# Color codes for output
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

echo -e "${BLUE}"
echo "======================================================================="
echo "  OSCAL Tools - Development SSL Certificate Generator"
echo "======================================================================="
echo -e "${NC}"

# Configuration
CERT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
KEYSTORE_FILE="$CERT_DIR/keystore.p12"
KEYSTORE_PASSWORD="changeit"  # Default password for development
ALIAS="oscal-dev"
VALIDITY_DAYS=365
KEY_SIZE=2048
COUNTRY="US"
STATE="Maryland"
CITY="Gaithersburg"
ORG="NIST"
OU="OSCAL Tools Development"
CN="localhost"

echo -e "${YELLOW}Configuration:${NC}"
echo "  Certificate Directory: $CERT_DIR"
echo "  Keystore File: $KEYSTORE_FILE"
echo "  Keystore Password: $KEYSTORE_PASSWORD"
echo "  Alias: $ALIAS"
echo "  Validity: $VALIDITY_DAYS days"
echo "  Common Name (CN): $CN"
echo ""

# Check if keystore already exists
if [ -f "$KEYSTORE_FILE" ]; then
    echo -e "${YELLOW}Keystore already exists: $KEYSTORE_FILE${NC}"
    read -p "Do you want to overwrite it? (y/N): " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        echo -e "${RED}Certificate generation cancelled${NC}"
        exit 1
    fi
    rm "$KEYSTORE_FILE"
fi

# Generate keystore with self-signed certificate
echo -e "${BLUE}Generating self-signed certificate...${NC}"

keytool -genkeypair \
    -alias "$ALIAS" \
    -keyalg RSA \
    -keysize $KEY_SIZE \
    -storetype PKCS12 \
    -keystore "$KEYSTORE_FILE" \
    -storepass "$KEYSTORE_PASSWORD" \
    -validity $VALIDITY_DAYS \
    -dname "CN=$CN, OU=$OU, O=$ORG, L=$CITY, ST=$STATE, C=$COUNTRY" \
    -ext "SAN=dns:localhost,dns:*.localhost,ip:127.0.0.1,ip:0.0.0.0" \
    -v

if [ $? -eq 0 ]; then
    echo ""
    echo -e "${GREEN}✓ Certificate generated successfully!${NC}"
    echo ""

    # Display certificate info
    echo -e "${BLUE}Certificate Information:${NC}"
    keytool -list -v -keystore "$KEYSTORE_FILE" -storepass "$KEYSTORE_PASSWORD" -alias "$ALIAS" | grep -A 5 "Owner:"

    echo ""
    echo -e "${GREEN}=======================================================================${NC}"
    echo -e "${GREEN}  Certificate Generation Complete${NC}"
    echo -e "${GREEN}=======================================================================${NC}"
    echo ""
    echo -e "${YELLOW}Next Steps:${NC}"
    echo ""
    echo "1. Update your application.properties or .env file:"
    echo ""
    echo "   server.ssl.enabled=true"
    echo "   server.ssl.key-store=file:certs/keystore.p12"
    echo "   server.ssl.key-store-password=$KEYSTORE_PASSWORD"
    echo "   server.ssl.key-store-type=PKCS12"
    echo "   server.ssl.key-alias=$ALIAS"
    echo "   server.port=8443"
    echo ""
    echo "2. Start your application:"
    echo "   ./dev.sh"
    echo ""
    echo "3. Access your application at:"
    echo "   https://localhost:8443"
    echo ""
    echo -e "${YELLOW}Browser Security Warnings:${NC}"
    echo "  Your browser will show a security warning because this is a"
    echo "  self-signed certificate. This is EXPECTED for development."
    echo ""
    echo "  To bypass the warning:"
    echo "    - Chrome: Type 'thisisunsafe' on the warning page"
    echo "    - Firefox: Click 'Advanced' -> 'Accept the Risk and Continue'"
    echo "    - Safari: Click 'Show Details' -> 'Visit this website'"
    echo ""
    echo -e "${RED}IMPORTANT: NEVER use self-signed certificates in production!${NC}"
    echo "  For production, obtain certificates from:"
    echo "    - Let's Encrypt (free, automated)"
    echo "    - Commercial Certificate Authority (DigiCert, Sectigo, etc.)"
    echo ""

else
    echo -e "${RED}✗ Certificate generation failed${NC}"
    exit 1
fi
