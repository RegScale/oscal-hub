#!/bin/bash
# OSCAL Tools - Local Deployment Script
# This script builds and starts the entire application stack locally using Docker Compose
#
# Usage:
#   ./local-deploy.sh           # Start the application
#   ./local-deploy.sh stop      # Stop the application
#   ./local-deploy.sh restart   # Restart the application
#   ./local-deploy.sh clean     # Stop and remove all data (clean start)
#   ./local-deploy.sh logs      # View application logs

set -e  # Exit on error

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to print colored output
print_header() {
    echo ""
    echo -e "${BLUE}========================================${NC}"
    echo -e "${BLUE}  $1${NC}"
    echo -e "${BLUE}========================================${NC}"
    echo ""
}

print_success() {
    echo -e "${GREEN}✓ $1${NC}"
}

print_warning() {
    echo -e "${YELLOW}⚠ $1${NC}"
}

print_error() {
    echo -e "${RED}✗ $1${NC}"
}

print_info() {
    echo -e "${BLUE}ℹ $1${NC}"
}

# Function to check if a command exists
command_exists() {
    command -v "$1" >/dev/null 2>&1
}

# Function to check prerequisites
check_prerequisites() {
    print_header "Checking Prerequisites"

    local all_good=true

    # Check Docker
    if command_exists docker; then
        print_success "Docker is installed: $(docker --version)"
    else
        print_error "Docker is not installed"
        print_info "Please install Docker Desktop: https://www.docker.com/products/docker-desktop"
        all_good=false
    fi

    # Check Docker Compose
    if command_exists docker-compose || docker compose version >/dev/null 2>&1; then
        if command_exists docker-compose; then
            print_success "Docker Compose is installed: $(docker-compose --version)"
        else
            print_success "Docker Compose is installed: $(docker compose version)"
        fi
    else
        print_error "Docker Compose is not installed"
        print_info "Please install Docker Compose: https://docs.docker.com/compose/install/"
        all_good=false
    fi

    # Check if Docker is running
    if docker info >/dev/null 2>&1; then
        print_success "Docker daemon is running"
    else
        print_error "Docker daemon is not running"
        print_info "Please start Docker Desktop"
        all_good=false
    fi

    # Check available disk space (need at least 5GB)
    if command_exists df; then
        available_gb=$(df -BG . | tail -1 | awk '{print $4}' | sed 's/G//')
        if [ "$available_gb" -gt 5 ]; then
            print_success "Sufficient disk space available: ${available_gb}GB free"
        else
            print_warning "Low disk space: ${available_gb}GB free (recommend at least 5GB)"
        fi
    fi

    if [ "$all_good" = false ]; then
        echo ""
        print_error "Prerequisites not met. Please install missing components."
        exit 1
    fi

    echo ""
}

# Function to stop the application
stop_application() {
    print_header "Stopping OSCAL Tools"

    if docker-compose ps | grep -q "Up"; then
        print_info "Stopping containers..."
        docker-compose down
        print_success "Application stopped"
    else
        print_info "Application is not running"
    fi

    echo ""
}

# Function to clean everything (including data)
clean_application() {
    print_header "Cleaning OSCAL Tools (Removing All Data)"

    print_warning "This will remove all containers, volumes, and data!"
    read -p "Are you sure? (yes/no): " confirm

    if [ "$confirm" != "yes" ]; then
        print_info "Clean cancelled"
        exit 0
    fi

    print_info "Stopping and removing containers, networks, and volumes..."
    docker-compose down -v

    print_info "Removing built images..."
    docker-compose down --rmi local 2>/dev/null || true

    # Clean Maven and npm caches (optional)
    read -p "Also clean build caches (Maven/npm)? (yes/no): " clean_cache
    if [ "$clean_cache" = "yes" ]; then
        print_info "Cleaning Maven cache..."
        rm -rf back-end/target

        print_info "Cleaning npm cache..."
        rm -rf front-end/.next
        rm -rf front-end/node_modules
    fi

    print_success "Clean complete!"
    echo ""
}

# Function to view logs
view_logs() {
    print_header "OSCAL Tools - Application Logs"
    print_info "Press Ctrl+C to exit log view"
    echo ""

    docker-compose logs -f
}

# Function to build and start the application
start_application() {
    print_header "OSCAL Tools - Local Deployment"

    # Step 1: Build backend
    print_header "Step 1/4: Building Backend (Maven)"
    print_info "This may take 2-3 minutes on first run..."

    cd back-end
    if mvn clean package -DskipTests; then
        print_success "Backend built successfully"
    else
        print_error "Backend build failed"
        print_info "Check the output above for errors"
        exit 1
    fi
    cd ..

    # Step 2: Build frontend
    print_header "Step 2/4: Building Frontend (npm)"
    print_info "Installing dependencies and building..."

    cd front-end
    if npm ci --legacy-peer-deps; then
        print_success "Frontend dependencies installed"
    else
        print_error "Frontend dependency installation failed"
        exit 1
    fi

    if npm run build; then
        print_success "Frontend built successfully"
    else
        print_error "Frontend build failed"
        exit 1
    fi
    cd ..

    # Step 3: Build Docker images
    print_header "Step 3/4: Building Docker Images"
    print_info "Building application container..."

    if docker-compose build; then
        print_success "Docker images built successfully"
    else
        print_error "Docker build failed"
        exit 1
    fi

    # Step 4: Start containers
    print_header "Step 4/4: Starting Application"
    print_info "Starting PostgreSQL and application containers..."

    if docker-compose up -d; then
        print_success "Containers started"
    else
        print_error "Failed to start containers"
        exit 1
    fi

    # Wait for services to be ready
    print_info "Waiting for services to be ready..."
    echo ""

    # Wait for PostgreSQL
    print_info "Waiting for PostgreSQL database..."
    for i in {1..30}; do
        if docker-compose exec -T postgres pg_isready -U oscal_user >/dev/null 2>&1; then
            print_success "PostgreSQL is ready"
            break
        fi
        if [ $i -eq 30 ]; then
            print_error "PostgreSQL failed to start within timeout"
            print_info "Check logs with: docker-compose logs postgres"
            exit 1
        fi
        echo -n "."
        sleep 2
    done
    echo ""

    # Wait for backend
    print_info "Waiting for backend API..."
    for i in {1..60}; do
        if curl -s http://localhost:8080/api/health >/dev/null 2>&1; then
            print_success "Backend API is ready"
            break
        fi
        if [ $i -eq 60 ]; then
            print_error "Backend failed to start within timeout"
            print_info "Check logs with: docker-compose logs oscal-ux"
            exit 1
        fi

        # Progress indicator
        if [ $((i % 5)) -eq 0 ]; then
            echo -n "."
        fi
        sleep 2
    done
    echo ""

    # Wait for frontend
    print_info "Waiting for frontend..."
    for i in {1..30}; do
        if curl -s http://localhost:3000 >/dev/null 2>&1; then
            print_success "Frontend is ready"
            break
        fi
        if [ $i -eq 30 ]; then
            print_warning "Frontend may not be fully ready yet"
            break
        fi
        echo -n "."
        sleep 2
    done
    echo ""

    # Success message
    print_header "🎉 OSCAL Tools is Ready!"
    echo ""
    echo -e "${GREEN}Application URLs:${NC}"
    echo -e "  ${BLUE}Frontend:${NC}  http://localhost:3000"
    echo -e "  ${BLUE}Backend API:${NC} http://localhost:8080/api"
    echo -e "  ${BLUE}API Health:${NC} http://localhost:8080/api/health"
    echo -e "  ${BLUE}Swagger UI:${NC} http://localhost:8080/swagger-ui.html"
    echo ""
    echo -e "${GREEN}Database Access:${NC}"
    echo -e "  ${BLUE}pgAdmin:${NC} http://localhost:5050"
    echo -e "  ${BLUE}Email:${NC} admin@oscal.local"
    echo -e "  ${BLUE}Password:${NC} admin"
    echo ""
    echo -e "${YELLOW}Next Steps:${NC}"
    echo "  1. Open http://localhost:3000 in your browser"
    echo "  2. Register a new user account"
    echo "  3. Start using OSCAL Tools!"
    echo ""
    echo -e "${BLUE}Management Commands:${NC}"
    echo "  ${GREEN}View logs:${NC}     ./local-deploy.sh logs"
    echo "  ${GREEN}Stop app:${NC}      ./local-deploy.sh stop"
    echo "  ${GREEN}Restart app:${NC}   ./local-deploy.sh restart"
    echo "  ${GREEN}Clean all:${NC}     ./local-deploy.sh clean"
    echo ""
    echo -e "${BLUE}Documentation:${NC}"
    echo "  See docs/LOCAL-DEPLOYMENT-GUIDE.md for detailed information"
    echo ""
}

# Main script logic
main() {
    case "${1:-start}" in
        start)
            check_prerequisites
            start_application
            ;;
        stop)
            stop_application
            ;;
        restart)
            stop_application
            check_prerequisites
            print_header "Restarting OSCAL Tools"
            docker-compose up -d
            print_success "Application restarted"
            print_info "Frontend: http://localhost:3000"
            print_info "Backend: http://localhost:8080/api"
            echo ""
            ;;
        clean)
            clean_application
            ;;
        logs)
            view_logs
            ;;
        status)
            print_header "OSCAL Tools - Status"
            docker-compose ps
            echo ""
            ;;
        --help|-h|help)
            echo "OSCAL Tools - Local Deployment Script"
            echo ""
            echo "Usage: ./local-deploy.sh [command]"
            echo ""
            echo "Commands:"
            echo "  start     Build and start the application (default)"
            echo "  stop      Stop the application"
            echo "  restart   Restart the application"
            echo "  clean     Stop and remove all containers and data"
            echo "  logs      View application logs"
            echo "  status    Show container status"
            echo "  help      Show this help message"
            echo ""
            echo "Examples:"
            echo "  ./local-deploy.sh              # Start the application"
            echo "  ./local-deploy.sh stop         # Stop the application"
            echo "  ./local-deploy.sh logs         # View logs"
            echo ""
            ;;
        *)
            print_error "Unknown command: $1"
            echo "Run './local-deploy.sh help' for usage information"
            exit 1
            ;;
    esac
}

# Run main function
main "$@"
