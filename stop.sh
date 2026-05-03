#!/bin/bash

# OSCAL HUB - Stop Script
# Usage: ./stop.sh [--all]
#
# Stops dev services on the local dev ports (default 8090 backend, 3010 frontend).
# Override via SERVER_PORT / FRONTEND_PORT to match a custom dev.sh launch.

cd "$(dirname "$0")"

SERVER_PORT="${SERVER_PORT:-8090}"
FRONTEND_PORT="${FRONTEND_PORT:-3010}"

echo "=== Stopping OSCAL HUB ==="

# Stop backend
echo "Stopping backend (port $SERVER_PORT)..."
pkill -f 'oscal-cli-api' 2>/dev/null || true
lsof -ti:"$SERVER_PORT" | xargs kill -9 2>/dev/null || true
echo "✓ Backend stopped"

# Stop frontend
echo "Stopping frontend (port $FRONTEND_PORT)..."
pkill -f 'next' 2>/dev/null || true
lsof -ti:"$FRONTEND_PORT" | xargs kill -9 2>/dev/null || true
echo "✓ Frontend stopped"

# Stop database if --all
if [ "$1" = "--all" ]; then
    echo "Stopping PostgreSQL..."
    docker-compose -f docker-compose-postgres.yml down 2>/dev/null || true
    echo "✓ PostgreSQL stopped"
else
    echo "(PostgreSQL still running - use --all to stop)"
fi

echo "=== Done ==="
