#!/bin/bash

# OSCAL CLI Web Interface - Startup Script
# This script starts both the backend (Spring Boot) and frontend (Next.js) servers

# Color codes for output
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

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
echo -e "${BLUE}========================================${NC}"
echo ""

# Initialize SDKMAN if available
if [ -f "$HOME/.sdkman/bin/sdkman-init.sh" ]; then
    echo -e "${BLUE}Initializing SDKMAN...${NC}"
    source "$HOME/.sdkman/bin/sdkman-init.sh"
fi

# Check if Java is installed
if ! command -v java &> /dev/null; then
    echo -e "${RED}Error: Java is not installed or not in PATH${NC}"
    echo -e "${YELLOW}Please install Java 11+ using SDKMAN:${NC}"
    echo -e "${YELLOW}  curl -s \"https://get.sdkman.io\" | bash${NC}"
    echo -e "${YELLOW}  source ~/.sdkman/bin/sdkman-init.sh${NC}"
    echo -e "${YELLOW}  sdk install java 11.0.25-tem${NC}"
    exit 1
fi

# Check if Maven is installed
if ! command -v mvn &> /dev/null; then
    echo -e "${RED}Error: Maven is not installed or not in PATH${NC}"
    echo -e "${YELLOW}Please install Maven using SDKMAN:${NC}"
    echo -e "${YELLOW}  source ~/.sdkman/bin/sdkman-init.sh${NC}"
    echo -e "${YELLOW}  sdk install maven${NC}"
    exit 1
fi

# Check if Node.js is installed
if ! command -v node &> /dev/null; then
    echo -e "${RED}Error: Node.js is not installed${NC}"
    exit 1
fi

# Get the directory where this script is located
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

# Load environment variables from .env file if it exists
if [ -f "$SCRIPT_DIR/.env" ]; then
    echo -e "${BLUE}Loading environment variables from .env...${NC}"
    source "$SCRIPT_DIR/.env"

    # Log which environment variables are configured (without showing values)
    if [ ! -z "$SPRING_PROFILES_ACTIVE" ]; then
        echo -e "${GREEN}✓ Spring Profile: ${SPRING_PROFILES_ACTIVE}${NC}"
    else
        echo -e "${YELLOW}⚠ SPRING_PROFILES_ACTIVE not set, using default: dev${NC}"
    fi

    if [ ! -z "$JWT_SECRET" ]; then
        JWT_LENGTH=${#JWT_SECRET}
        echo -e "${GREEN}✓ JWT_SECRET is configured (length: ${JWT_LENGTH} characters)${NC}"
        if [ $JWT_LENGTH -lt 32 ]; then
            echo -e "${RED}  ⚠ WARNING: JWT secret is too short. Minimum 32 characters required.${NC}"
        fi
    else
        echo -e "${YELLOW}⚠ JWT_SECRET is not set (will use dev default)${NC}"
    fi

    if [ ! -z "$AZURE_STORAGE_CONNECTION_STRING" ]; then
        echo -e "${GREEN}✓ AZURE_STORAGE_CONNECTION_STRING is set${NC}"
    else
        echo -e "${YELLOW}⚠ AZURE_STORAGE_CONNECTION_STRING is not set${NC}"
    fi

    if [ ! -z "$AZURE_STORAGE_CONTAINER_NAME" ]; then
        echo -e "${GREEN}✓ AZURE_STORAGE_CONTAINER_NAME: ${AZURE_STORAGE_CONTAINER_NAME}${NC}"
    fi

    echo ""
else
    echo -e "${YELLOW}Note: .env file not found. Using default configuration.${NC}"
    echo -e "${YELLOW}To customize: cp .env.example .env and configure your settings${NC}"
    echo ""
fi

# Function to cleanup on exit
cleanup() {
    echo ""
    echo -e "${YELLOW}Shutting down servers...${NC}"
    if [ ! -z "$BACKEND_PID" ]; then
        kill $BACKEND_PID 2>/dev/null
    fi
    if [ ! -z "$FRONTEND_PID" ]; then
        kill $FRONTEND_PID 2>/dev/null
    fi
    exit 0
}

# Trap SIGINT (Ctrl+C) and SIGTERM
trap cleanup SIGINT SIGTERM

# Function to start Docker daemon
start_docker() {
    local os_type=$(uname -s)

    case "$os_type" in
        Darwin)
            # macOS - start Docker Desktop
            echo -e "${YELLOW}Starting Docker Desktop...${NC}"
            open -a Docker
            ;;
        Linux)
            # Linux - try to start Docker daemon with systemd or service
            echo -e "${YELLOW}Starting Docker daemon...${NC}"
            if command -v systemctl &> /dev/null; then
                sudo systemctl start docker
            elif command -v service &> /dev/null; then
                sudo service docker start
            else
                echo -e "${RED}✗ Unable to start Docker automatically${NC}"
                echo -e "${YELLOW}Please start Docker manually and try again.${NC}"
                return 1
            fi
            ;;
        *)
            echo -e "${RED}✗ Unsupported operating system: $os_type${NC}"
            echo -e "${YELLOW}Please start Docker manually and try again.${NC}"
            return 1
            ;;
    esac
    return 0
}

# Check if Docker is running
if ! docker info > /dev/null 2>&1; then
    echo -e "${RED}✗ Docker is not running!${NC}"

    # Try to start Docker
    if ! start_docker; then
        exit 1
    fi

    # Wait for Docker to be ready
    echo -e "${BLUE}Waiting for Docker to be ready...${NC}"
    DOCKER_READY=0
    for i in {1..60}; do
        if docker info > /dev/null 2>&1; then
            DOCKER_READY=1
            break
        fi
        echo -n "."
        sleep 2
    done
    echo ""

    if [ $DOCKER_READY -eq 1 ]; then
        echo -e "${GREEN}✓ Docker is ready!${NC}"
    else
        echo -e "${RED}✗ Docker failed to start${NC}"
        echo -e "${YELLOW}Please start Docker manually and try again.${NC}"
        exit 1
    fi
else
    echo -e "${GREEN}✓ Docker is already running${NC}"
fi

# Check if PostgreSQL is running
echo -e "${BLUE}Checking PostgreSQL database...${NC}"
if docker ps --format '{{.Names}}' | grep -q '^oscal-postgres-dev$'; then
    echo -e "${GREEN}✓ PostgreSQL is already running${NC}"
else
    echo -e "${YELLOW}PostgreSQL not running. Starting it now...${NC}"
    docker-compose -f "$SCRIPT_DIR/docker-compose-postgres.yml" up -d

    # Wait for PostgreSQL to be healthy
    echo -e "${BLUE}Waiting for PostgreSQL to be ready...${NC}"
    POSTGRES_READY=0
    for i in {1..30}; do
        if docker inspect oscal-postgres-dev --format='{{.State.Health.Status}}' 2>/dev/null | grep -q 'healthy'; then
            POSTGRES_READY=1
            break
        fi
        echo -n "."
        sleep 1
    done
    echo ""

    if [ $POSTGRES_READY -eq 1 ]; then
        echo -e "${GREEN}✓ PostgreSQL is ready!${NC}"
    else
        echo -e "${RED}✗ PostgreSQL failed to start or is not healthy${NC}"
        echo -e "${YELLOW}Check logs with: docker logs oscal-postgres-dev${NC}"
        exit 1
    fi
fi
echo ""

# Build backend
echo -e "${GREEN}Building backend...${NC}"
cd "$SCRIPT_DIR/back-end"
mvn clean compile > "$SCRIPT_DIR/backend-build.log" 2>&1
if [ $? -ne 0 ]; then
    echo -e "${RED}✗ Backend build failed. Check backend-build.log for details.${NC}"
    exit 1
fi
echo -e "${GREEN}✓ Backend build successful${NC}"
cd "$SCRIPT_DIR"

# Start backend server
echo -e "${GREEN}Starting backend server...${NC}"
cd "$SCRIPT_DIR/back-end"
mvn spring-boot:run > "$SCRIPT_DIR/backend.log" 2>&1 &
BACKEND_PID=$!
cd "$SCRIPT_DIR"

# Wait for backend to start
echo -e "${YELLOW}Waiting for backend to start...${NC}"
for i in {1..30}; do
    if curl -s http://localhost:8080/api/health > /dev/null 2>&1; then
        echo -e "${GREEN}✓ Backend is ready!${NC}"
        break
    fi
    if [ $i -eq 30 ]; then
        echo -e "${RED}✗ Backend failed to start. Check backend.log for details.${NC}"
        cleanup
    fi
    sleep 2
done

# Install frontend dependencies if needed
echo -e "${GREEN}Checking frontend dependencies...${NC}"
cd "$SCRIPT_DIR/front-end"
if [ ! -d "node_modules" ]; then
    echo -e "${YELLOW}Installing frontend dependencies...${NC}"
    npm ci > "$SCRIPT_DIR/frontend-install.log" 2>&1
    if [ $? -ne 0 ]; then
        echo -e "${RED}✗ Frontend dependency installation failed. Check frontend-install.log for details.${NC}"
        cleanup
    fi
    echo -e "${GREEN}✓ Frontend dependencies installed${NC}"
else
    echo -e "${GREEN}✓ Frontend dependencies already installed${NC}"
fi
cd "$SCRIPT_DIR"

# Start frontend server
echo -e "${GREEN}Starting frontend server...${NC}"
cd "$SCRIPT_DIR/front-end"
npm run dev > "$SCRIPT_DIR/frontend.log" 2>&1 &
FRONTEND_PID=$!
cd "$SCRIPT_DIR"

# Wait for frontend to start
echo -e "${YELLOW}Waiting for frontend to start...${NC}"
sleep 5

echo ""
echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}  Servers are running!${NC}"
echo -e "${GREEN}========================================${NC}"
echo ""
echo -e "${BLUE}Frontend:${NC} http://localhost:3000"
echo -e "${BLUE}Backend:${NC}  http://localhost:8080/api"
echo ""
echo -e "Logs:"
echo -e "  Frontend:      ${SCRIPT_DIR}/frontend.log"
echo -e "  Backend:       ${SCRIPT_DIR}/backend.log"
echo -e "  Backend Build: ${SCRIPT_DIR}/backend-build.log"
echo ""
echo -e "${YELLOW}Press Ctrl+C to stop all servers${NC}"
echo ""

# Keep script running and wait for user to stop
wait
