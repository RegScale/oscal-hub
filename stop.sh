#!/bin/bash

# OSCAL CLI Web Interface - Stop Script
# This script stops both frontend and backend servers

echo "Stopping OSCAL CLI Web Interface..."

# Stop Spring Boot backend
echo "Stopping backend..."
pkill -f 'spring-boot:run' 2>/dev/null
if [ $? -eq 0 ]; then
    echo "✓ Backend stopped"
else
    echo "ℹ Backend was not running"
fi

# Stop Next.js frontend
echo "Stopping frontend..."
pkill -f 'next-server' 2>/dev/null
pkill -f 'next dev' 2>/dev/null
if [ $? -eq 0 ]; then
    echo "✓ Frontend stopped"
else
    echo "ℹ Frontend was not running"
fi

# Also check and stop by port
lsof -ti:8080 | xargs kill -9 2>/dev/null
lsof -ti:3000 | xargs kill -9 2>/dev/null
lsof -ti:3001 | xargs kill -9 2>/dev/null

echo ""
echo "All servers stopped"
