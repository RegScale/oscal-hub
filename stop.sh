#!/bin/bash

# OSCAL HUB - Stop Script
# Usage: ./stop.sh [--all]

cd "$(dirname "$0")"

echo "=== Stopping OSCAL HUB ==="

# Stop backend
echo "Stopping backend..."
pkill -f 'oscal-cli-api' 2>/dev/null || true
lsof -ti:8080 | xargs kill -9 2>/dev/null || true
echo "✓ Backend stopped"

# Stop frontend
echo "Stopping frontend..."
pkill -f 'next' 2>/dev/null || true
lsof -ti:3000 | xargs kill -9 2>/dev/null || true
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
