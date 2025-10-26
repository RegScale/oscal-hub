#!/usr/bin/env bash
#
# Stop All OSCAL Tools Services
#
# This script stops:
# - OSCAL Tools application
# - PostgreSQL database
# - pgAdmin
# - Prometheus
# - Grafana
#
# Usage:
#   ./stop-all.sh           # Stop all services
#   ./stop-all.sh --remove  # Stop and remove containers and networks
#

set -e

# Colors
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

REMOVE_CONTAINERS=false

# Parse arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        --remove|-r)
            REMOVE_CONTAINERS=true
            shift
            ;;
        *)
            echo -e "${RED}Unknown option: $1${NC}"
            echo "Usage: $0 [--remove]"
            exit 1
            ;;
    esac
done

echo -e "${BLUE}=====================================${NC}"
echo -e "${BLUE}  OSCAL Tools - Stop All Services   ${NC}"
echo -e "${BLUE}=====================================${NC}"
echo

if [ "$REMOVE_CONTAINERS" = true ]; then
    echo -e "${YELLOW}⚠️  Stopping and removing all containers...${NC}"
    echo

    # Stop and remove monitoring stack
    echo -e "${GREEN}[1/2]${NC} Stopping monitoring stack..."
    docker-compose -f docker-compose-monitoring.yml down

    # Stop and remove application stack
    echo
    echo -e "${GREEN}[2/2]${NC} Stopping application stack..."
    docker-compose down

    echo
    echo -e "${GREEN}✓ All services stopped and removed!${NC}"
    echo -e "${YELLOW}💡 Note: Data volumes are preserved (database, Grafana dashboards)${NC}"
    echo -e "${YELLOW}💡 To remove volumes too, run: docker-compose down -v${NC}"
else
    echo -e "${YELLOW}⏸️  Stopping all containers...${NC}"
    echo

    # Stop monitoring stack
    echo -e "${GREEN}[1/2]${NC} Stopping monitoring stack..."
    docker-compose -f docker-compose-monitoring.yml stop

    # Stop application stack
    echo
    echo -e "${GREEN}[2/2]${NC} Stopping application stack..."
    docker-compose stop

    echo
    echo -e "${GREEN}✓ All services stopped!${NC}"
    echo -e "${YELLOW}💡 Tip: Run './start-all.sh' to start services again${NC}"
    echo -e "${YELLOW}💡 Tip: Run './stop-all.sh --remove' to remove containers${NC}"
fi

echo
