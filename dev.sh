#!/bin/bash

# Quick development startup script
# This script assumes you've already run the setup once

# Get the directory where this script is located
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

# Source SDKMAN to ensure Java and Maven are available
if [ -f "$HOME/.sdkman/bin/sdkman-init.sh" ]; then
    source "$HOME/.sdkman/bin/sdkman-init.sh"
fi

echo "Starting OSCAL CLI Web Interface..."
echo ""
echo "Backend will be available at: http://localhost:8080/api"
echo "Frontend will be available at: http://localhost:3000"
echo ""
echo "Building backend..."
(cd "$SCRIPT_DIR/back-end" && mvn clean compile)
if [ $? -ne 0 ]; then
    echo "Backend build failed. Exiting."
    exit 1
fi
echo ""
echo "Starting backend..."
(cd "$SCRIPT_DIR/back-end" && mvn spring-boot:run) &
BACKEND_PID=$!

echo "Starting frontend..."
(cd "$SCRIPT_DIR/front-end" && npm run dev) &
FRONTEND_PID=$!

echo ""
echo "Servers starting in background..."
echo "Backend PID: $BACKEND_PID"
echo "Frontend PID: $FRONTEND_PID"
echo ""
echo "To stop:"
echo "  kill $BACKEND_PID $FRONTEND_PID"
echo ""
echo "Or use: pkill -f 'spring-boot:run' && pkill -f 'next-server'"
