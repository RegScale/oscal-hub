#!/bin/bash

# OSCAL HUB - Simple Dev Script
# Prereq: Docker Desktop must be running

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
lsof -ti:8080 | xargs kill -9 2>/dev/null || true
lsof -ti:3000 | xargs kill -9 2>/dev/null || true

# Start backend
echo "Starting backend..."
java -jar target/oscal-cli-api-*.jar &

# Start frontend
echo "Starting frontend..."
cd ../front-end
[ ! -d node_modules ] && npm install
npm run dev &

# Wait for ready
echo "Waiting for services..."
until curl -sf http://localhost:8080/api/health >/dev/null 2>&1; do sleep 2; done
echo "✓ Backend ready: http://localhost:8080/api"

until curl -sf http://localhost:3000 >/dev/null 2>&1; do sleep 1; done
echo "✓ Frontend ready: http://localhost:3000"

echo ""
echo "=== RUNNING ==="
echo "Press Ctrl+C to stop"
wait
