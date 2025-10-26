#!/usr/bin/env bash
#
# Restart All OSCAL Tools Services
#
# This is a convenience script that stops and then starts all services
#
# Usage:
#   ./restart-all.sh
#

set -e

# Colors
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${BLUE}=====================================${NC}"
echo -e "${BLUE}  OSCAL Tools - Restart All Services ${NC}"
echo -e "${BLUE}=====================================${NC}"
echo

echo -e "${YELLOW}⏸️  Stopping all services...${NC}"
./stop-all.sh

echo
echo -e "${YELLOW}⏳ Waiting 3 seconds...${NC}"
sleep 3

echo
echo -e "${YELLOW}▶️  Starting all services...${NC}"
echo
./start-all.sh
