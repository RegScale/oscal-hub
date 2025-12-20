# syntax=docker/dockerfile:1
# OSCAL Tools - Optimized Multi-stage Dockerfile
# Builds both backend (Spring Boot) and frontend (Next.js) into a single container
# Compatible with Cloud Build

# =============================================================================
# Stage 1: Build Backend (Spring Boot with Maven) - OPTIMIZED
# =============================================================================
FROM maven:3.9-eclipse-temurin-21 AS backend-builder

WORKDIR /build

# Copy pom.xml FIRST for dependency caching
COPY back-end/pom.xml ./back-end/pom.xml

# Download dependencies (cached if pom.xml unchanged)
WORKDIR /build/back-end
RUN mvn dependency:go-offline -B

# Copy source code
COPY back-end/src ./src/

# Build application (dependencies already cached)
RUN mvn package -DskipTests -B -q

# =============================================================================
# Stage 2: Build Frontend Dependencies - OPTIMIZED
# =============================================================================
FROM node:25-alpine AS frontend-deps
RUN apk add --no-cache libc6-compat
WORKDIR /app

# Copy package files (cached if unchanged)
COPY front-end/package.json front-end/package-lock.json* ./

# Install dependencies (cached layer)
RUN npm ci --legacy-peer-deps --prefer-offline && \
    npm cache clean --force

# =============================================================================
# Stage 3: Build Frontend (Next.js)
# =============================================================================
FROM node:25-alpine AS frontend-builder
WORKDIR /app

# Copy dependencies from deps stage
COPY --from=frontend-deps /app/node_modules ./node_modules
COPY front-end/ .

# Set build-time environment variables for Next.js
# IMPORTANT: NEXT_PUBLIC_* vars are baked into the build and cannot be changed at runtime
# Use relative URL so it works in any deployment environment
ENV NEXT_TELEMETRY_DISABLED=1 \
    NEXT_PUBLIC_API_URL=/api \
    NEXT_PUBLIC_USE_MOCK=false

# Build the application
RUN npm run build

# =============================================================================
# Stage 4: Runtime - Combine both applications (SECURITY HARDENED)
# =============================================================================
FROM eclipse-temurin:21-jre-jammy

# Add security metadata labels (OCI standard)
LABEL org.opencontainers.image.title="OSCAL Tools"
LABEL org.opencontainers.image.description="Secure full-stack OSCAL tools with Spring Boot API and Next.js UI"
LABEL org.opencontainers.image.vendor="NIST OSCAL Tools"
LABEL org.opencontainers.image.authors="oscal-tools@example.com"
LABEL security.non-root="true"
LABEL security.healthcheck="true"
LABEL security.read-only-root="recommended"

# Install Node.js for Next.js standalone server + security updates (OPTIMIZED)
RUN apt-get update && \
    apt-get upgrade -y && \
    apt-get install -y --no-install-recommends \
        curl \
        ca-certificates \
        tzdata \
        tini \
        gnupg && \
    curl -fsSL https://deb.nodesource.com/setup_24.x | bash - && \
    apt-get install -y --no-install-recommends nodejs && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/* /tmp/* /var/tmp/*

# Create non-root user and setup directories (COMBINED for fewer layers)
RUN groupadd -g 10001 oscalgroup && \
    useradd -u 10001 -g oscalgroup -s /bin/false -m -d /home/oscaluser oscaluser

WORKDIR /app
RUN mkdir -p \
        /app/data \
        /app/logs \
        /app/backend \
        /app/frontend \
        /app/uploads/org-logos \
        /home/oscaluser/.oscal-hub/files && \
    chown -R oscaluser:oscalgroup /app /home/oscaluser

# Copy backend JAR with proper ownership
COPY --from=backend-builder --chown=oscaluser:oscalgroup \
    /build/back-end/target/*.jar /app/backend.jar

# Copy frontend build with proper ownership
COPY --from=frontend-builder --chown=oscaluser:oscalgroup \
    /app/.next/standalone /app/frontend/
COPY --from=frontend-builder --chown=oscaluser:oscalgroup \
    /app/.next/static /app/frontend/.next/static
COPY --from=frontend-builder --chown=oscaluser:oscalgroup \
    /app/public /app/frontend/public

# Copy startup script
COPY --chown=oscaluser:oscalgroup docker-entrypoint.sh /app/
RUN chmod +x /app/docker-entrypoint.sh

# Switch to non-root user
USER oscaluser:oscalgroup

# Environment variables (single layer for efficiency)
# Note: SPRING_PROFILES_ACTIVE will be overridden by Cloud Run env var to 'gcp'
ENV JAVA_OPTS="-XX:+UseContainerSupport \
               -XX:MaxRAMPercentage=50.0 \
               -XX:+UseG1GC \
               -XX:+HeapDumpOnOutOfMemoryError \
               -XX:HeapDumpPath=/app/logs \
               -Djava.security.egd=file:/dev/./urandom" \
    NODE_ENV=production \
    SECURITY_HEADERS_ENABLED=true \
    SECURITY_REQUIRE_HTTPS=false \
    RATE_LIMIT_ENABLED=true \
    ACCOUNT_LOCKOUT_ENABLED=true \
    AUDIT_LOGGING_ENABLED=true

# Note: CRITICAL - Set these via docker-compose or runtime (NEVER hardcode):
# - JWT_SECRET
# - DB_PASSWORD
# - CORS_ALLOWED_ORIGINS

# Expose ports (non-privileged)
# Note: PORT env var (set by Cloud Run) determines frontend port, backend uses 8081
EXPOSE 8080 8081

# Health check
# Backend on 8081, frontend on $PORT (defaults to 8080 in Cloud Run)
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8081/actuator/health && curl -f http://localhost:${PORT:-8080} || exit 1

# Use tini as init system (proper signal handling, zombie reaping)
ENTRYPOINT ["/usr/bin/tini", "--"]

# Start both applications
CMD ["/app/docker-entrypoint.sh"]
