#!/bin/bash

# OSCAL CLI Web Interface - Stop Script
# This script stops frontend, backend servers, and Docker containers

# Color codes for output
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# Get the directory where this script is located
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

echo -e "${BLUE}Stopping OSCAL HUB...${NC}"
echo ""

# Stop Spring Boot backend
echo -e "${YELLOW}Stopping backend...${NC}"
pkill -f 'spring-boot:run' 2>/dev/null
if [ $? -eq 0 ]; then
    echo -e "${GREEN}✓ Backend stopped${NC}"
else
    echo -e "${BLUE}ℹ Backend was not running${NC}"
fi

# Stop Next.js frontend
echo -e "${YELLOW}Stopping frontend...${NC}"
pkill -f 'next-server' 2>/dev/null
pkill -f 'next dev' 2>/dev/null
if [ $? -eq 0 ]; then
    echo -e "${GREEN}✓ Frontend stopped${NC}"
else
    echo -e "${BLUE}ℹ Frontend was not running${NC}"
fi

# Force kill processes by port
echo -e "${YELLOW}Cleaning up ports...${NC}"
lsof -ti:8080 2>/dev/null | xargs kill -9 2>/dev/null && echo -e "${GREEN}✓ Port 8080 cleared${NC}"
lsof -ti:3000 2>/dev/null | xargs kill -9 2>/dev/null && echo -e "${GREEN}✓ Port 3000 cleared${NC}"
lsof -ti:3001 2>/dev/null | xargs kill -9 2>/dev/null && echo -e "${GREEN}✓ Port 3001 cleared${NC}"

# Stop Docker containers
echo ""
echo -e "${YELLOW}Stopping Docker containers...${NC}"

# Check if Docker is running
if ! docker info > /dev/null 2>&1; then
    echo -e "${YELLOW}ℹ Docker is not running, skipping container cleanup${NC}"
else
    # Stop containers from docker-compose-postgres.yml
    if docker ps -q --filter "name=oscal-postgres-dev" 2>/dev/null | grep -q .; then
        echo -e "${YELLOW}Stopping PostgreSQL container...${NC}"
        docker-compose -f "$SCRIPT_DIR/docker-compose-postgres.yml" down
        echo -e "${GREEN}✓ PostgreSQL container stopped${NC}"
    else
        echo -e "${BLUE}ℹ PostgreSQL container was not running${NC}"
    fi

    # Also check for pgAdmin
    if docker ps -q --filter "name=oscal-pgadmin" 2>/dev/null | grep -q .; then
        echo -e "${YELLOW}Stopping pgAdmin container...${NC}"
        docker stop oscal-pgadmin 2>/dev/null
        docker rm oscal-pgadmin 2>/dev/null
        echo -e "${GREEN}✓ pgAdmin container stopped${NC}"
    else
        echo -e "${BLUE}ℹ pgAdmin container was not running${NC}"
    fi

    # Also stop any containers from the main docker-compose.yml
    if docker ps -q --filter "name=oscal-ux-dev" 2>/dev/null | grep -q .; then
        echo -e "${YELLOW}Stopping OSCAL UX container...${NC}"
        docker-compose -f "$SCRIPT_DIR/docker-compose.yml" down
        echo -e "${GREEN}✓ OSCAL UX containers stopped${NC}"
    fi

    # Verify port 5432 is freed
    echo ""
    echo -e "${YELLOW}Verifying PostgreSQL port (5432) is freed...${NC}"
    if lsof -ti:5432 2>/dev/null | grep -q .; then
        echo -e "${RED}⚠ Port 5432 is still in use. Force killing...${NC}"
        lsof -ti:5432 | xargs kill -9 2>/dev/null
        sleep 1
        if ! lsof -ti:5432 2>/dev/null | grep -q .; then
            echo -e "${GREEN}✓ Port 5432 is now free${NC}"
        else
            echo -e "${RED}✗ Port 5432 is still in use. Manual intervention may be required.${NC}"
        fi
    else
        echo -e "${GREEN}✓ Port 5432 is free${NC}"
    fi
fi

echo ""
echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}All servers and containers stopped!${NC}"
echo -e "${GREEN}========================================${NC}"
echo ""
echo -e "${BLUE}To restart:${NC}"
echo -e "  ./dev.sh     ${YELLOW}# Development mode${NC}"
echo -e "  ./start.sh   ${YELLOW}# Production mode${NC}"
echo ""
