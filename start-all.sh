#!/usr/bin/env bash
#
# Start All OSCAL Tools Services (Application + Monitoring)
#
# This script starts:
# - PostgreSQL database
# - OSCAL Tools application (frontend + backend)
# - pgAdmin (database management UI)
# - Prometheus (metrics collection)
# - Grafana (metrics visualization)
#
# Usage:
#   ./start-all.sh
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
echo -e "${BLUE}  OSCAL Tools - Start All Services  ${NC}"
echo -e "${BLUE}=====================================${NC}"
echo

# Step 0: Clean up ports
echo -e "${YELLOW}🧹 Cleaning up ports...${NC}"
kill_port 3000
kill_port 8080
kill_port 5432
kill_port 5050
kill_port 9090
kill_port 3001
echo

# Step 1: Start main application stack
echo -e "${GREEN}[1/2]${NC} Starting application stack..."
docker-compose up -d

# Wait a moment for services to initialize
sleep 2

# Step 2: Start monitoring stack
echo
echo -e "${GREEN}[2/2]${NC} Starting monitoring stack..."
docker-compose -f docker-compose-monitoring.yml up -d

# Wait for services to be ready
echo
echo -e "${YELLOW}⏳ Waiting for services to be ready...${NC}"
sleep 5

# Check status
echo
echo -e "${GREEN}✓ All services started!${NC}"
echo
echo -e "${BLUE}=====================================${NC}"
echo -e "${BLUE}  Service URLs                       ${NC}"
echo -e "${BLUE}=====================================${NC}"
echo
echo -e "${GREEN}Application:${NC}"
echo "  • Frontend:        http://localhost:3000"
echo "  • Backend API:     http://localhost:8080/api"
echo "  • API Health:      http://localhost:8080/actuator/health"
echo "  • API Docs:        http://localhost:8080/swagger-ui.html"
echo
echo -e "${GREEN}Database:${NC}"
echo "  • PostgreSQL:      localhost:5432"
echo "  • pgAdmin:         http://localhost:5050"
echo "    - Email:         admin@oscal.local"
echo "    - Password:      admin"
echo
echo -e "${GREEN}Monitoring:${NC}"
echo "  • Prometheus:      http://localhost:9090"
echo "  • Grafana:         http://localhost:3001"
echo "    - Username:      admin"
echo "    - Password:      admin"
echo "    - Dashboard:     OSCAL Tools → OSCAL Tools - Overview"
echo
echo -e "${BLUE}=====================================${NC}"
echo
echo -e "${YELLOW}💡 Tip: Run './stop-all.sh' to stop all services${NC}"
echo -e "${YELLOW}💡 Tip: Run 'docker-compose logs -f' to view application logs${NC}"
echo -e "${YELLOW}💡 Tip: Run 'docker-compose -f docker-compose-monitoring.yml logs -f' to view monitoring logs${NC}"
echo

# Show running containers
echo -e "${GREEN}Running containers:${NC}"
docker-compose ps
echo
docker-compose -f docker-compose-monitoring.yml ps
