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
    set -a # Automatically export all variables
    source "$SCRIPT_DIR/.env"
    set +a

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

# Source SDKMAN to ensure Java and Maven are available
if [ -f "$HOME/.sdkman/bin/sdkman-init.sh" ]; then
    source "$HOME/.sdkman/bin/sdkman-init.sh"
fi

# Ensure Java 21 is used (project requirement)
REQUIRED_JAVA_VERSION=21

get_java_version() {
    local java_cmd="${1:-java}"
    if [ -x "$java_cmd" ] || command -v "$java_cmd" &> /dev/null; then
        "$java_cmd" -version 2>&1 | head -1 | cut -d'"' -f2 | cut -d'.' -f1
    fi
}

# If JAVA_HOME is set, use it (allows override)
if [ -n "$JAVA_HOME" ] && [ -x "$JAVA_HOME/bin/java" ]; then
    export PATH="$JAVA_HOME/bin:$PATH"
    JAVA_VER=$(get_java_version "$JAVA_HOME/bin/java")
    if [ "$JAVA_VER" = "$REQUIRED_JAVA_VERSION" ]; then
        echo -e "${GREEN}✓ Using JAVA_HOME: $JAVA_HOME (Java $JAVA_VER)${NC}"
    else
        echo -e "${YELLOW}⚠ JAVA_HOME points to Java $JAVA_VER (project requires $REQUIRED_JAVA_VERSION)${NC}"
    fi
else
    # Auto-detect Java 21
    JAVA_VER=$(get_java_version)
    if [ "$JAVA_VER" != "$REQUIRED_JAVA_VERSION" ]; then
        echo -e "${YELLOW}Looking for Java $REQUIRED_JAVA_VERSION...${NC}"
        # Windows: Check Eclipse Adoptium installation
        if [ -d "/c/Program Files/Eclipse Adoptium" ]; then
            JAVA21_PATH=$(ls -d "/c/Program Files/Eclipse Adoptium/jdk-21"* 2>/dev/null | head -1)
            if [ -n "$JAVA21_PATH" ]; then
                export JAVA_HOME="$JAVA21_PATH"
                export PATH="$JAVA_HOME/bin:$PATH"
                echo -e "${GREEN}✓ Found Java 21: $JAVA_HOME${NC}"
            fi
        fi
        # macOS: Check common locations
        if [ -d "/Library/Java/JavaVirtualMachines" ]; then
            JAVA21_PATH=$(ls -d /Library/Java/JavaVirtualMachines/temurin-21* 2>/dev/null | head -1)
            if [ -n "$JAVA21_PATH" ]; then
                export JAVA_HOME="$JAVA21_PATH/Contents/Home"
                export PATH="$JAVA_HOME/bin:$PATH"
                echo -e "${GREEN}✓ Found Java 21: $JAVA_HOME${NC}"
            fi
        fi
    fi
fi

# Verify Java is available
JAVA_VER=$(get_java_version)
if [ -z "$JAVA_VER" ]; then
    echo -e "${RED}✗ Java not found${NC}"
    echo ""
    echo -e "${YELLOW}Please either:${NC}"
    echo -e "  1. Install Java $REQUIRED_JAVA_VERSION+ (Eclipse Temurin recommended)"
    echo -e "     https://adoptium.net/"
    echo ""
    echo -e "  2. Set JAVA_HOME to an existing Java $REQUIRED_JAVA_VERSION+ installation:"
    echo -e "     export JAVA_HOME=\"/path/to/jdk-$REQUIRED_JAVA_VERSION\""
    echo -e "     ./dev.sh"
    exit 1
elif [ "$JAVA_VER" -lt "$REQUIRED_JAVA_VERSION" ] 2>/dev/null; then
    echo -e "${RED}✗ Java $JAVA_VER found, but Java $REQUIRED_JAVA_VERSION+ is required${NC}"
    echo ""
    echo -e "${YELLOW}Please either:${NC}"
    echo -e "  1. Install Java $REQUIRED_JAVA_VERSION+ (Eclipse Temurin recommended)"
    echo -e "     https://adoptium.net/"
    echo ""
    echo -e "  2. Set JAVA_HOME to an existing Java $REQUIRED_JAVA_VERSION+ installation:"
    echo -e "     export JAVA_HOME=\"/path/to/jdk-$REQUIRED_JAVA_VERSION\""
    echo -e "     ./dev.sh"
    exit 1
else
    echo -e "${GREEN}✓ Using Java $JAVA_VER${NC}"
fi

# Check if git submodules are initialized
echo ""
echo -e "${BLUE}Checking git submodules...${NC}"
if [ -d "$SCRIPT_DIR/cli" ] && [ -n "$(ls -A "$SCRIPT_DIR/cli" 2>/dev/null)" ]; then
    echo -e "${GREEN}✓ Git submodules are initialized${NC}"
else
    echo -e "${YELLOW}Git submodules not initialized. Initializing now...${NC}"
    (cd "$SCRIPT_DIR" && git submodule update --init --recursive)
    if [ $? -eq 0 ]; then
        echo -e "${GREEN}✓ Git submodules initialized successfully${NC}"
    else
        echo -e "${RED}✗ Failed to initialize git submodules${NC}"
        echo -e "${YELLOW}Try running manually: git submodule update --init --recursive${NC}"
        exit 1
    fi
fi

echo -e "${BLUE}Starting OSCAL HUB...${NC}"
echo ""

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

# Check if Docker is running - use multiple methods for reliability
docker_is_running() {
    # Try docker info first
    if docker info > /dev/null 2>&1; then
        return 0
    fi
    # Fallback: check if docker ps works
    if docker ps > /dev/null 2>&1; then
        return 0
    fi
    return 1
}

if ! docker_is_running; then
    echo -e "${YELLOW}Docker not responding, checking if it needs to start...${NC}"

    # Check if Docker Desktop is already running (macOS)
    if [[ "$(uname -s)" == "Darwin" ]] && pgrep -x "Docker" > /dev/null 2>&1; then
        echo -e "${BLUE}Docker Desktop is running but not ready yet. Waiting...${NC}"
    else
        echo -e "${YELLOW}Starting Docker...${NC}"
        # Try to start Docker
        if ! start_docker; then
            exit 1
        fi
    fi

    # Wait for Docker to be ready
    echo -e "${BLUE}Waiting for Docker to be ready...${NC}"
    DOCKER_READY=0
    for i in {1..60}; do
        if docker_is_running; then
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
    echo -e "${GREEN}✓ Docker is running${NC}"
fi

# Check if PostgreSQL is running and accepting connections
echo -e "${BLUE}Checking PostgreSQL database...${NC}"

# Function to check if PostgreSQL is accepting connections
check_postgres_connection() {
    docker exec oscal-postgres-dev pg_isready -U oscal_user -d oscal_dev > /dev/null 2>&1
}

# Check if container exists and is running
if docker ps --format '{{.Names}}' | grep -q '^oscal-postgres-dev$'; then
    echo -e "${BLUE}PostgreSQL container found, verifying connection...${NC}"
    if check_postgres_connection; then
        echo -e "${GREEN}✓ PostgreSQL is running and accepting connections${NC}"
    else
        echo -e "${YELLOW}PostgreSQL container exists but not responding. Restarting...${NC}"
        docker-compose -f "$SCRIPT_DIR/docker-compose-postgres.yml" down
        docker-compose -f "$SCRIPT_DIR/docker-compose-postgres.yml" up -d

        # Wait for PostgreSQL to be ready
        echo -e "${BLUE}Waiting for PostgreSQL to be ready...${NC}"
        POSTGRES_READY=0
        for i in {1..30}; do
            if check_postgres_connection; then
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
            echo -e "${RED}✗ PostgreSQL failed to start${NC}"
            echo -e "${YELLOW}Check logs with: docker logs oscal-postgres-dev${NC}"
            exit 1
        fi
    fi
else
    echo -e "${YELLOW}PostgreSQL not running. Starting it now...${NC}"
    docker-compose -f "$SCRIPT_DIR/docker-compose-postgres.yml" up -d

    # Wait for PostgreSQL to be healthy
    echo -e "${BLUE}Waiting for PostgreSQL to be ready...${NC}"
    POSTGRES_READY=0
    for i in {1..30}; do
        if check_postgres_connection; then
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
echo "Backend will be available at: http://localhost:8080/api"
echo "Frontend will be available at: http://localhost:3000"
echo "pgAdmin will be available at: http://localhost:5050 (if started)"
echo ""

# Function to kill processes using a specific port (cross-platform)
kill_port() {
  local port=$1
  local description=$2
  echo -e "${YELLOW}Checking for processes on port ${port} (${description})...${NC}"

  local pids=""
  # Try lsof first (macOS/Linux)
  if command -v lsof &> /dev/null; then
    pids=$(lsof -ti:$port 2>/dev/null || true)
  else
    # Windows (Git Bash) - use netstat
    pids=$(netstat -ano 2>/dev/null | grep ":$port " | grep "LISTENING" | awk '{print $5}' | head -1)
  fi

  if [ -n "$pids" ] && [ "$pids" != "0" ]; then
    echo -e "${RED}Found processes using port ${port}: $pids${NC}"
    echo -e "${YELLOW}Killing processes...${NC}"
    if command -v lsof &> /dev/null; then
      echo "$pids" | xargs kill -9 2>/dev/null || true
    else
      # Windows - use taskkill
      for pid in $pids; do
        taskkill //F //PID $pid 2>/dev/null || true
      done
    fi
    sleep 1
    echo -e "${GREEN}✓ Port ${port} cleared${NC}"
  else
    echo -e "${GREEN}✓ Port ${port} is available${NC}"
  fi
}

# Clean up ports before starting
echo -e "${YELLOW}🧹 Cleaning up ports...${NC}"
kill_port 8080 "Backend"
kill_port 3000 "Frontend"
echo ""

echo -e "${GREEN}Building backend...${NC}"
(cd "$SCRIPT_DIR/back-end" && mvn package -DskipTests -q)
if [ $? -ne 0 ]; then
    echo -e "${RED}✗ Backend build failed. Exiting.${NC}"
    exit 1
fi
echo ""
echo -e "${GREEN}Starting backend...${NC}"
# Run JAR directly instead of mvn spring-boot:run to avoid Maven wrapper noise on shutdown
BACKEND_JAR=$(ls "$SCRIPT_DIR/back-end/target"/*.jar 2>/dev/null | grep -v sources | head -1)
if [ -z "$BACKEND_JAR" ]; then
    echo -e "${RED}✗ Backend JAR not found. Build may have failed.${NC}"
    exit 1
fi
(cd "$SCRIPT_DIR/back-end" && java -jar "$BACKEND_JAR") &
BACKEND_PID=$!

echo -e "${GREEN}Starting frontend...${NC}"
# Check if node_modules exists, install dependencies if not
if [ ! -d "$SCRIPT_DIR/front-end/node_modules" ]; then
    echo -e "${YELLOW}Installing frontend dependencies...${NC}"
    (cd "$SCRIPT_DIR/front-end" && npm install)
    if [ $? -ne 0 ]; then
        echo -e "${RED}✗ Frontend dependency installation failed. Exiting.${NC}"
        exit 1
    fi
fi
(cd "$SCRIPT_DIR/front-end" && npm run dev) &
FRONTEND_PID=$!

echo ""
echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}OSCAL HUB is starting...${NC}"
echo -e "${GREEN}========================================${NC}"
echo ""
echo "Backend PID: $BACKEND_PID"
echo "Frontend PID: $FRONTEND_PID"
echo ""
echo -e "${BLUE}Waiting for services to be ready...${NC}"

# Wait for backend to be ready
BACKEND_READY=0
for i in {1..60}; do
    if curl -s http://localhost:8080/api/health > /dev/null 2>&1; then
        BACKEND_READY=1
        break
    fi
    # Check if backend process is still running
    if ! kill -0 $BACKEND_PID 2>/dev/null; then
        echo -e "${RED}✗ Backend process died unexpectedly${NC}"
        kill $FRONTEND_PID 2>/dev/null
        exit 1
    fi
    echo -n "."
    sleep 2
done
echo ""

if [ $BACKEND_READY -eq 1 ]; then
    echo -e "${GREEN}✓ Backend is ready at http://localhost:8080/api${NC}"
else
    echo -e "${RED}✗ Backend failed to start within 2 minutes${NC}"
    echo -e "${YELLOW}Check logs for errors${NC}"
fi

# Wait for frontend to be ready
FRONTEND_READY=0
for i in {1..30}; do
    if curl -s http://localhost:3000 > /dev/null 2>&1; then
        FRONTEND_READY=1
        break
    fi
    # Check if frontend process is still running
    if ! kill -0 $FRONTEND_PID 2>/dev/null; then
        echo -e "${RED}✗ Frontend process died unexpectedly${NC}"
        kill $BACKEND_PID 2>/dev/null
        exit 1
    fi
    echo -n "."
    sleep 1
done
echo ""

if [ $FRONTEND_READY -eq 1 ]; then
    echo -e "${GREEN}✓ Frontend is ready at http://localhost:3000${NC}"
else
    echo -e "${YELLOW}⚠ Frontend may still be compiling...${NC}"
fi

echo ""
echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}OSCAL HUB is running!${NC}"
echo -e "${GREEN}========================================${NC}"
echo ""
echo -e "${BLUE}To stop: ./stop.sh${NC}"
echo -e "${BLUE}To stop everything (including DB): ./stop.sh --all${NC}"
echo ""

# Trap to handle Ctrl+C gracefully
cleanup() {
    echo ""
    echo -e "${YELLOW}Shutting down...${NC}"
    kill $BACKEND_PID 2>/dev/null
    kill $FRONTEND_PID 2>/dev/null
    echo -e "${GREEN}✓ Servers stopped${NC}"
    echo -e "${BLUE}Note: PostgreSQL is still running. Use ./stop.sh --all to stop it.${NC}"
    exit 0
}
trap cleanup SIGINT SIGTERM

# Wait for both processes (keeps script running)
wait $BACKEND_PID $FRONTEND_PID
