#!/bin/bash
set -e

echo "=========================================="
echo "  OSCAL UX Container Starting"
echo "=========================================="
echo ""

# Function to handle shutdown
shutdown() {
    echo ""
    echo "Shutting down OSCAL UX..."
    kill -TERM "$BACKEND_PID" "$FRONTEND_PID" 2>/dev/null || true
    wait "$BACKEND_PID" "$FRONTEND_PID" 2>/dev/null || true
    echo "Shutdown complete"
    exit 0
}

# Trap signals
trap shutdown SIGTERM SIGINT

# Start backend
echo "Starting backend (Spring Boot)..."
java $JAVA_OPTS -jar /app/backend.jar &
BACKEND_PID=$!
echo "Backend PID: $BACKEND_PID"

# Wait for backend to be ready
echo "Waiting for backend to be ready..."
for i in {1..30}; do
    if curl -s http://localhost:8080/api/health > /dev/null 2>&1; then
        echo "✓ Backend is ready!"
        break
    fi
    if [ $i -eq 30 ]; then
        echo "✗ Backend failed to start"
        exit 1
    fi
    sleep 2
done

# Start frontend
echo ""
echo "Starting frontend (Next.js)..."
cd /app/frontend
node server.js &
FRONTEND_PID=$!
echo "Frontend PID: $FRONTEND_PID"

# Wait for frontend to be ready
echo "Waiting for frontend to be ready..."
sleep 5

echo ""
echo "=========================================="
echo "  OSCAL UX is Ready!"
echo "=========================================="
echo ""
echo "Frontend: http://localhost:3000"
echo "Backend:  http://localhost:8080/api"
echo ""
echo "Press Ctrl+C to stop"
echo ""

# Wait for processes
wait "$BACKEND_PID" "$FRONTEND_PID"
