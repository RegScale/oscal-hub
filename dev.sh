#!/bin/bash

# Quick development startup script
# This script assumes you've already run the setup once

# Color codes for output
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# Get the directory where this script is located
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

echo -e "${BLUE}"
cat << "EOF"
   ____   _____ _____          _
  / __ \ / ____/ ____|   /\   | |
 | |  | | (___| |       /  \  | |
 | |  | |\___ \ |      / /\ \ | |
 | |__| |____) | |____|/ ____ \| |____
  \____/|_____/ \_____/_/    \_\______|

         H  U  B
EOF
echo -e "${NC}"
echo ""

# Load environment variables from .env file if it exists
if [ -f "$SCRIPT_DIR/.env" ]; then
    echo -e "${BLUE}Loading environment variables from .env...${NC}"
    source "$SCRIPT_DIR/.env"

    # Log which environment variables are configured (without showing values)
    if [ ! -z "$AZURE_STORAGE_CONNECTION_STRING" ]; then
        echo -e "${GREEN}✓ AZURE_STORAGE_CONNECTION_STRING is set${NC}"
    else
        echo -e "${YELLOW}⚠ AZURE_STORAGE_CONNECTION_STRING is not set${NC}"
    fi

    if [ ! -z "$AZURE_STORAGE_CONTAINER_NAME" ]; then
        echo -e "${GREEN}✓ AZURE_STORAGE_CONTAINER_NAME is set to: ${AZURE_STORAGE_CONTAINER_NAME}${NC}"
    fi

    echo ""
else
    echo -e "${YELLOW}Note: .env file not found. Azure Blob Storage will not be available.${NC}"
    echo -e "${YELLOW}To enable Azure storage: cp .env.example .env and add your connection string${NC}"
    echo ""
fi

# Source SDKMAN to ensure Java and Maven are available
if [ -f "$HOME/.sdkman/bin/sdkman-init.sh" ]; then
    source "$HOME/.sdkman/bin/sdkman-init.sh"
fi

echo -e "${BLUE}Starting OSCAL HUB...${NC}"
echo ""
echo "Backend will be available at: http://localhost:8080/api"
echo "Frontend will be available at: http://localhost:3000"
echo ""
echo -e "${GREEN}Building backend...${NC}"
(cd "$SCRIPT_DIR/back-end" && mvn clean compile)
if [ $? -ne 0 ]; then
    echo -e "${RED}✗ Backend build failed. Exiting.${NC}"
    exit 1
fi
echo ""
echo -e "${GREEN}Starting backend...${NC}"
(cd "$SCRIPT_DIR/back-end" && mvn spring-boot:run) &
BACKEND_PID=$!

echo -e "${GREEN}Starting frontend...${NC}"
(cd "$SCRIPT_DIR/front-end" && npm run dev) &
FRONTEND_PID=$!

echo ""
echo "Servers starting in background..."
echo "Backend PID: $BACKEND_PID"
echo "Frontend PID: $FRONTEND_PID"
echo ""
echo "To stop:"
echo "  kill $BACKEND_PID $FRONTEND_PID"
echo ""
echo "Or use: pkill -f 'spring-boot:run' && pkill -f 'next-server'"
