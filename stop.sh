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
# Try both patterns: JAR execution and mvn spring-boot:run
pkill -f 'oscal-cli-api.*\.jar' 2>/dev/null || pkill -f 'spring-boot:run' 2>/dev/null
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

# Force kill processes by port (cross-platform)
echo -e "${YELLOW}Cleaning up ports...${NC}"

kill_port() {
    local port=$1
    # Try lsof first (macOS/Linux)
    if command -v lsof &> /dev/null; then
        lsof -ti:$port 2>/dev/null | xargs kill -9 2>/dev/null && echo -e "${GREEN}✓ Port $port cleared${NC}" && return
    fi
    # Try netstat + taskkill for Windows (Git Bash)
    local pid=$(netstat -ano 2>/dev/null | grep ":$port " | grep "LISTENING" | awk '{print $5}' | head -1)
    if [ -n "$pid" ] && [ "$pid" != "0" ]; then
        taskkill //F //PID $pid 2>/dev/null && echo -e "${GREEN}✓ Port $port cleared (PID $pid)${NC}" && return
    fi
    echo -e "${BLUE}ℹ Port $port was not in use${NC}"
}

kill_port 8080
kill_port 3000
kill_port 3001

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

    # Verify port 5432 is freed (cross-platform)
    echo ""
    echo -e "${YELLOW}Verifying PostgreSQL port (5432) is freed...${NC}"
    port_in_use() {
        local port=$1
        if command -v lsof &> /dev/null; then
            lsof -ti:$port 2>/dev/null | grep -q .
        else
            netstat -ano 2>/dev/null | grep ":$port " | grep -q "LISTENING"
        fi
    }
    if port_in_use 5432; then
        echo -e "${RED}⚠ Port 5432 is still in use. Force killing...${NC}"
        kill_port 5432
        sleep 1
        if ! port_in_use 5432; then
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
