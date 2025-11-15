#!/bin/bash
set -e

echo "=========================================="
echo "  OSCAL UX Container Starting"
echo "=========================================="
echo ""
echo "Environment: ${SPRING_PROFILES_ACTIVE:-dev}"
echo "Database URL: ${DB_URL:-not set}"
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

# Function to check database connectivity (PostgreSQL)
check_database() {
    if [ -z "$DB_URL" ]; then
        echo "⚠️  DB_URL not set, skipping database check"
        return 0
    fi

    # Extract database host and port from JDBC URL
    # Format: jdbc:postgresql://host:port/database?params
    DB_HOST=$(echo "$DB_URL" | sed -n 's/.*:\/\/\([^:\/]*\).*/\1/p')
    DB_PORT=$(echo "$DB_URL" | sed -n 's/.*:\([0-9]*\)\/.*/\1/p')
    DB_NAME=$(echo "$DB_URL" | sed -n 's/.*\/\([^?]*\).*/\1/p')

    if [ -z "$DB_HOST" ] || [ -z "$DB_PORT" ]; then
        echo "⚠️  Could not parse database connection info, proceeding anyway"
        return 0
    fi

    echo "Checking database connectivity..."
    echo "  Host: $DB_HOST"
    echo "  Port: $DB_PORT"
    echo "  Database: $DB_NAME"
    echo ""

    # Wait for database to be ready (max 60 seconds)
    for i in {1..12}; do
        if timeout 2 bash -c "cat < /dev/null > /dev/tcp/$DB_HOST/$DB_PORT" 2>/dev/null; then
            echo "✓ Database is reachable on $DB_HOST:$DB_PORT"
            return 0
        fi
        echo "Attempt $i/12: Database not ready yet, waiting..."
        sleep 5
    done

    echo "⚠️  Database may not be ready, but proceeding with startup"
    echo "   (Spring Boot will retry connections automatically)"
    return 0
}

# Check database connectivity before starting backend
check_database

# Ensure file storage directory exists with proper permissions
echo "Ensuring file storage directory exists..."
mkdir -p /home/oscaluser/.oscal-hub/files
chmod 755 /home/oscaluser/.oscal-hub/files
echo "✓ File storage directory ready"
echo ""

echo "=========================================="
echo "  Starting Backend (Spring Boot)"
echo "=========================================="
echo ""
echo "Database migrations (Flyway) will run automatically on startup"
echo "if SPRING_FLYWAY_ENABLED=true (default in production)"
echo ""

# Start backend
java $JAVA_OPTS -jar /app/backend.jar &
BACKEND_PID=$!
echo "Backend PID: $BACKEND_PID"
echo ""

# Wait for backend to be ready (this includes migration time)
echo "Waiting for backend to be ready..."
echo "(This may take longer on first startup due to database migrations)"
echo ""

BACKEND_READY=false
for i in {1..60}; do
    if curl -s http://localhost:8080/api/health > /dev/null 2>&1; then
        echo "✓ Backend is ready!"
        BACKEND_READY=true
        break
    fi

    # Check if backend process is still running
    if ! kill -0 $BACKEND_PID 2>/dev/null; then
        echo "✗ Backend process died unexpectedly"
        exit 1
    fi

    # Progress indicator
    if [ $((i % 5)) -eq 0 ]; then
        echo "Still waiting for backend... (${i}s elapsed)"
    fi

    sleep 2
done

if [ "$BACKEND_READY" = false ]; then
    echo "✗ Backend failed to start within timeout"
    echo "Check logs for errors. Common issues:"
    echo "  - Database connection failed"
    echo "  - Database migration errors"
    echo "  - Configuration errors (JWT_SECRET, DB_PASSWORD, etc.)"
    exit 1
fi

echo ""
echo "=========================================="
echo "  Starting Frontend (Next.js)"
echo "=========================================="
echo ""

# Start frontend
cd /app/frontend
node server.js &
FRONTEND_PID=$!
echo "Frontend PID: $FRONTEND_PID"
echo ""

# Wait for frontend to be ready
echo "Waiting for frontend to be ready..."
FRONTEND_READY=false
for i in {1..20}; do
    if curl -s http://localhost:3000 > /dev/null 2>&1; then
        echo "✓ Frontend is ready!"
        FRONTEND_READY=true
        break
    fi

    # Check if frontend process is still running
    if ! kill -0 $FRONTEND_PID 2>/dev/null; then
        echo "✗ Frontend process died unexpectedly"
        exit 1
    fi

    sleep 2
done

if [ "$FRONTEND_READY" = false ]; then
    echo "⚠️  Frontend may not be fully ready, but continuing..."
fi

echo ""
echo "=========================================="
echo "  OSCAL UX is Ready! ✓"
echo "=========================================="
echo ""
echo "Frontend: http://localhost:3000"
echo "Backend:  http://localhost:8080/api"
echo "Health:   http://localhost:8080/api/health"
echo ""
echo "Environment: ${SPRING_PROFILES_ACTIVE:-dev}"
if [ "${SPRING_FLYWAY_ENABLED:-true}" = "true" ]; then
    echo "Database migrations: ✓ Enabled (Flyway)"
fi
echo ""
echo "Press Ctrl+C to stop"
echo ""

# Wait for processes
wait "$BACKEND_PID" "$FRONTEND_PID"
