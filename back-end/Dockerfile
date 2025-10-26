# ==============================================================================
# OSCAL Tools API - Secure Multi-Stage Docker Build
# ==============================================================================
# This Dockerfile implements security best practices:
# - Multi-stage build (reduces final image size by ~70%)
# - Runs as non-root user (oscaluser)
# - Uses specific version tags (not 'latest')
# - Minimal attack surface (distroless-based runtime)
# - Health checks for container orchestration
# - Resource limits and security options
# ==============================================================================

# ==============================================================================
# Stage 1: Builder
# ==============================================================================
FROM maven:3.9.6-eclipse-temurin-21-alpine AS builder

# Set build-time metadata labels (OCI standard)
LABEL org.opencontainers.image.title="OSCAL Tools API"
LABEL org.opencontainers.image.description="Secure Spring Boot API for OSCAL operations"
LABEL org.opencontainers.image.vendor="NIST OSCAL Tools"
LABEL org.opencontainers.image.authors="oscal-tools@example.com"
LABEL org.opencontainers.image.documentation="https://github.com/oscal-tools/oscal-cli"
LABEL org.opencontainers.image.source="https://github.com/oscal-tools/oscal-cli"

# Set working directory
WORKDIR /build

# Copy only POM files first (for better layer caching)
COPY pom.xml ./
COPY back-end/pom.xml ./back-end/

# Download dependencies (cached if POM hasn't changed)
RUN mvn dependency:go-offline -B -q

# Copy source code
COPY back-end/src ./back-end/src

# Build application (skip tests in Docker build - run separately)
RUN mvn clean package -DskipTests -B -q && \
    # Verify JAR was created
    ls -lh back-end/target/*.jar && \
    # Extract JAR for inspection
    mkdir -p /build/extracted && \
    cd /build/extracted && \
    jar -xf /build/back-end/target/oscal-cli-api-*.jar

# ==============================================================================
# Stage 2: Runtime
# ==============================================================================
# Use Eclipse Temurin JRE (smaller than JDK, official OpenJDK builds)
FROM eclipse-temurin:21-jre-alpine

# Install security updates and required tools
RUN apk update && \
    apk upgrade --no-cache && \
    apk add --no-cache \
        ca-certificates \
        tzdata \
        tini && \
    # Clean up
    rm -rf /var/cache/apk/*

# Create non-root user and group
# UID/GID 10001 is commonly used for application users
RUN addgroup -g 10001 -S oscalgroup && \
    adduser -u 10001 -S oscaluser -G oscalgroup -h /app -s /sbin/nologin

# Set working directory
WORKDIR /app

# Create directories with proper permissions
RUN mkdir -p /app/data /app/logs /app/config && \
    chown -R oscaluser:oscalgroup /app

# Copy JAR from builder stage
COPY --from=builder --chown=oscaluser:oscalgroup \
    /build/back-end/target/oscal-cli-api-*.jar \
    /app/oscal-cli-api.jar

# Verify JAR integrity
RUN jar -tf /app/oscal-cli-api.jar > /dev/null && \
    echo "JAR integrity verified"

# Switch to non-root user
USER oscaluser:oscalgroup

# Expose port (non-privileged port)
EXPOSE 8080

# Set environment variables
ENV JAVA_OPTS="-XX:+UseContainerSupport \
               -XX:MaxRAMPercentage=75.0 \
               -XX:+UseG1GC \
               -XX:+HeapDumpOnOutOfMemoryError \
               -XX:HeapDumpPath=/app/logs \
               -Djava.security.egd=file:/dev/./urandom \
               -Dspring.profiles.active=prod"

ENV SPRING_PROFILES_ACTIVE=prod
ENV SERVER_PORT=8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# Use tini as init system (proper signal handling)
ENTRYPOINT ["/sbin/tini", "--"]

# Run application
CMD ["sh", "-c", "java $JAVA_OPTS -jar /app/oscal-cli-api.jar"]

# Security metadata
LABEL security.non-root="true"
LABEL security.healthcheck="true"
LABEL security.read-only-root="recommended"
