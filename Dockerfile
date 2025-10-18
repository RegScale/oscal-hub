# syntax=docker/dockerfile:1
# OSCAL Tools - Multi-stage Dockerfile
# Builds both backend (Spring Boot) and frontend (Next.js) into a single container

# =============================================================================
# Stage 1: Build Backend (Spring Boot with Maven)
# =============================================================================
FROM maven:3.9-eclipse-temurin-21 AS backend-builder

WORKDIR /build

# Copy backend source
COPY back-end/pom.xml ./back-end/
COPY back-end/src ./back-end/src/

# Build backend (skip tests for faster builds)
WORKDIR /build/back-end
RUN mvn clean package -DskipTests

# =============================================================================
# Stage 2: Build Frontend Dependencies
# =============================================================================
FROM node:18-alpine AS frontend-deps
RUN apk add --no-cache libc6-compat
WORKDIR /app

# Copy package files
COPY front-end/package.json front-end/package-lock.json* ./

# Install dependencies
RUN npm ci --legacy-peer-deps

# =============================================================================
# Stage 3: Build Frontend (Next.js)
# =============================================================================
FROM node:18-alpine AS frontend-builder
WORKDIR /app

# Copy dependencies from deps stage
COPY --from=frontend-deps /app/node_modules ./node_modules
COPY front-end/ .

# Disable telemetry during build
ENV NEXT_TELEMETRY_DISABLED=1

# Build the application
RUN npm run build

# =============================================================================
# Stage 4: Runtime - Combine both applications
# =============================================================================
FROM eclipse-temurin:21-jre-jammy

# Install Node.js for Next.js standalone server
RUN apt-get update && \
    apt-get install -y curl && \
    curl -fsSL https://deb.nodesource.com/setup_18.x | bash - && \
    apt-get install -y nodejs && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

# Create app directory
WORKDIR /app

# Copy backend JAR
COPY --from=backend-builder /build/back-end/target/*.jar /app/backend.jar

# Copy frontend build
COPY --from=frontend-builder /app/.next/standalone /app/frontend/
COPY --from=frontend-builder /app/.next/static /app/frontend/.next/static
COPY --from=frontend-builder /app/public /app/frontend/public

# Copy startup script
COPY docker-entrypoint.sh /app/
RUN chmod +x /app/docker-entrypoint.sh

# Environment variables
ENV JAVA_OPTS="-Xmx512m -Xms256m"
ENV NODE_ENV=production
ENV PORT=3000
ENV NEXT_PUBLIC_API_URL=http://localhost:8080/api
ENV NEXT_PUBLIC_USE_MOCK=false

# Expose ports
EXPOSE 3000 8080

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8080/api/health && curl -f http://localhost:3000 || exit 1

# Start both applications
ENTRYPOINT ["/app/docker-entrypoint.sh"]
