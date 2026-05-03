#!/bin/bash

# OSCAL HUB - Simple Dev Script
# Prereq: Docker Desktop must be running
#
# Local dev ports:
#   Backend API:  8090   (override via SERVER_PORT)
#   Frontend UI:  3010   (override via FRONTEND_PORT)
#   PostgreSQL:   5432   (Docker container; trust-center uses 5433)
#
# These differ from production (8080 / 3000) so OSCAL Hub can run alongside
# the trust-center app, which reserves 80/443/4200/5433/6379/8000.

set -e

cd "$(dirname "$0")"

echo "=== OSCAL HUB ==="

# Check Docker
if ! docker info >/dev/null 2>&1; then
    echo "ERROR: Docker is not running. Start Docker Desktop first."
    exit 1
fi
echo "✓ Docker OK"

# Load .env
[ -f .env ] && source .env

# Pass through SendGrid config if set in the user's shell
[ -n "$SENDGRID_API_KEY" ] && export SENDGRID_API_KEY
[ -n "$SENDGRID_FROM_EMAIL" ] && export SENDGRID_FROM_EMAIL
[ -n "$SENDGRID_FROM_NAME" ] && export SENDGRID_FROM_NAME
[ -n "$APP_BASE_URL" ] && export APP_BASE_URL
[ -n "$EMAIL_ENABLED" ] && export EMAIL_ENABLED

# Local dev ports (overridable)
export SERVER_PORT="${SERVER_PORT:-8090}"
export HTTP_PORT="${HTTP_PORT:-$SERVER_PORT}"
FRONTEND_PORT="${FRONTEND_PORT:-3010}"

# Tell the frontend where the backend lives and what port to bind to
export PORT="$FRONTEND_PORT"
export NEXT_PUBLIC_API_URL="${NEXT_PUBLIC_API_URL:-http://localhost:${SERVER_PORT}/api}"
# The Next.js API proxy at src/app/api/[...path]/route.ts uses this to forward
# server-side requests (image URLs, etc.). Must point at the dev backend port.
export BACKEND_INTERNAL_URL="${BACKEND_INTERNAL_URL:-http://localhost:${SERVER_PORT}}"
export APP_BASE_URL="${APP_BASE_URL:-http://localhost:${FRONTEND_PORT}}"
export CORS_ALLOWED_ORIGINS="${CORS_ALLOWED_ORIGINS:-http://localhost:${FRONTEND_PORT},http://localhost:3001,http://localhost:3000}"

# Activate the dev Spring profile so application-dev.properties applies
export SPRING_PROFILES_ACTIVE="${SPRING_PROFILES_ACTIVE:-dev}"

# Start PostgreSQL
echo "Starting PostgreSQL..."
docker-compose -f docker-compose-postgres.yml up -d --remove-orphans

# Wait for healthy
echo "Waiting for PostgreSQL..."
until docker exec oscal-postgres-dev pg_isready -U oscal_user >/dev/null 2>&1; do
    sleep 1
done
echo "✓ PostgreSQL ready"

# Build backend
echo "Building backend..."
cd back-end
mvn package -DskipTests -q
echo "✓ Backend built"

# Clear ports
lsof -ti:"$SERVER_PORT" | xargs kill -9 2>/dev/null || true
lsof -ti:"$FRONTEND_PORT" | xargs kill -9 2>/dev/null || true

# Start backend
echo "Starting backend on port $SERVER_PORT..."
java -jar target/oscal-cli-api-*.jar &

# Start frontend (uses PORT env var, set above)
echo "Starting frontend on port $FRONTEND_PORT..."
cd ../front-end
[ ! -d node_modules ] && npm install
npm run dev &

# Wait for ready
echo "Waiting for services..."
until curl -sf "http://localhost:${SERVER_PORT}/api/health" >/dev/null 2>&1; do sleep 2; done
echo "✓ Backend ready: http://localhost:${SERVER_PORT}/api"

until curl -sf "http://localhost:${FRONTEND_PORT}" >/dev/null 2>&1; do sleep 1; done
echo "✓ Frontend ready: http://localhost:${FRONTEND_PORT}"

echo ""
echo "=== RUNNING ==="
echo "Press Ctrl+C to stop"
wait
