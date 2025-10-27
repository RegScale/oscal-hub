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
RED='\033[0;31m'
NC='\033[0m' # No Color

# Function to kill processes using a specific port
kill_port() {
  local port=$1
  echo -e "${YELLOW}🔍 Checking for processes on port ${port}...${NC}"

  # Find process IDs using the port (macOS and Linux compatible)
  local pids=$(lsof -ti:$port 2>/dev/null || true)

  if [ -n "$pids" ]; then
    echo -e "${RED}⚠️  Found processes using port ${port}: $pids${NC}"
    echo -e "${YELLOW}🔨 Killing processes...${NC}"
    echo "$pids" | xargs kill -9 2>/dev/null || true
    sleep 1
    echo -e "${GREEN}✓ Port ${port} cleared${NC}"
  else
    echo -e "${GREEN}✓ Port ${port} is available${NC}"
  fi
}

echo -e "${BLUE}=====================================${NC}"
echo -e "${BLUE}  OSCAL Tools - Restart All Services ${NC}"
echo -e "${BLUE}=====================================${NC}"
echo

echo -e "${YELLOW}⏸️  Stopping all services...${NC}"
./stop-all.sh

echo
echo -e "${YELLOW}🧹 Cleaning up ports...${NC}"
kill_port 3000
kill_port 8080

echo
echo -e "${YELLOW}⏳ Waiting 3 seconds...${NC}"
sleep 3

echo
echo -e "${YELLOW}▶️  Starting all services...${NC}"
echo
./start-all.sh
