# OSCAL UX - Docker Deployment Guide

**Version**: 1.0
**Date**: October 2025
**Status**: Production Ready

## Overview

This guide explains how to build, deploy, and manage the OSCAL UX application using Docker. The application is packaged into a single, self-contained container that includes both the frontend (Next.js) and backend (Spring Boot) services.

## Prerequisites

- **Docker**: Version 20.10 or higher
- **Docker Compose**: Version 2.0 or higher (optional but recommended)
- **Minimum System Requirements**:
  - 2 CPU cores
  - 2GB RAM
  - 5GB disk space

## Quick Start

### Using Docker Compose (Recommended)

```bash
# Build and start the container
docker-compose up -d

# View logs
docker-compose logs -f

# Stop the container
docker-compose down
```

Access the application:
- Frontend: http://localhost:3000
- Backend API: http://localhost:8080/api
- Health Check: http://localhost:8080/api/health

### Using Docker CLI

```bash
# Build the image
docker build -t oscal-ux:latest .

# Run the container
docker run -d \
  --name oscal-ux \
  -p 3000:3000 \
  -p 8080:8080 \
  oscal-ux:latest

# View logs
docker logs -f oscal-ux

# Stop the container
docker stop oscal-ux
docker rm oscal-ux
```

## Building the Image

### Standard Build

```bash
cd front-end
docker build -t oscal-ux:latest .
```

Build time: ~5-10 minutes (depends on network speed and CPU)

### Build with Custom Tag

```bash
docker build -t oscal-ux:1.0.0 -t oscal-ux:latest .
```

### Build for Different Platforms

```bash
# For ARM64 (Apple Silicon, AWS Graviton)
docker buildx build --platform linux/arm64 -t oscal-ux:latest-arm64 .

# For AMD64 (Intel/AMD servers)
docker buildx build --platform linux/amd64 -t oscal-ux:latest-amd64 .

# Multi-platform build
docker buildx build \
  --platform linux/amd64,linux/arm64 \
  -t oscal-ux:latest \
  --push .
```

## Running the Container

### Basic Run

```bash
docker run -d \
  --name oscal-ux \
  -p 3000:3000 \
  -p 8080:8080 \
  oscal-ux:latest
```

### With Environment Variables

```bash
docker run -d \
  --name oscal-ux \
  -p 3000:3000 \
  -p 8080:8080 \
  -e JAVA_OPTS="-Xmx1g -Xms512m" \
  -e NODE_ENV="production" \
  oscal-ux:latest
```

### With Resource Limits

```bash
docker run -d \
  --name oscal-ux \
  -p 3000:3000 \
  -p 8080:8080 \
  --memory="2g" \
  --cpus="2" \
  oscal-ux:latest
```

### With Restart Policy

```bash
docker run -d \
  --name oscal-ux \
  -p 3000:3000 \
  -p 8080:8080 \
  --restart unless-stopped \
  oscal-ux:latest
```

## Configuration

### Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `JAVA_OPTS` | `-Xmx512m -Xms256m` | JVM options for backend |
| `NODE_ENV` | `production` | Node.js environment |
| `PORT` | `3000` | Frontend port |
| `NEXT_PUBLIC_API_URL` | `http://localhost:8080/api` | Backend API URL |
| `NEXT_PUBLIC_USE_MOCK` | `false` | Use mock data instead of real API |

### Port Mapping

| Container Port | Purpose | Default Host Port |
|----------------|---------|-------------------|
| 3000 | Frontend (Next.js) | 3000 |
| 8080 | Backend API (Spring Boot) | 8080 |

Change host ports if needed:
```bash
docker run -p 3001:3000 -p 8081:8080 oscal-ux:latest
```

## Docker Compose Configuration

### Basic docker-compose.yml

```yaml
version: '3.8'

services:
  oscal-ux:
    image: oscal-ux:latest
    container_name: oscal-ux
    ports:
      - "3000:3000"
      - "8080:8080"
    restart: unless-stopped
    environment:
      - JAVA_OPTS=-Xmx1g -Xms512m
```

### With Nginx Reverse Proxy

```yaml
version: '3.8'

services:
  oscal-ux:
    image: oscal-ux:latest
    container_name: oscal-ux
    expose:
      - "3000"
      - "8080"
    restart: unless-stopped

  nginx:
    image: nginx:alpine
    container_name: oscal-ux-nginx
    ports:
      - "80:80"
    volumes:
      - ./nginx.conf:/etc/nginx/nginx.conf:ro
    depends_on:
      - oscal-ux
    restart: unless-stopped
```

## Management Commands

### View Logs

```bash
# All logs
docker logs oscal-ux

# Follow logs
docker logs -f oscal-ux

# Last 100 lines
docker logs --tail 100 oscal-ux

# Logs since 10 minutes ago
docker logs --since 10m oscal-ux
```

### Container Status

```bash
# Check if container is running
docker ps | grep oscal-ux

# View resource usage
docker stats oscal-ux

# Inspect container
docker inspect oscal-ux
```

### Execute Commands Inside Container

```bash
# Get a shell
docker exec -it oscal-ux /bin/bash

# Check backend health
docker exec oscal-ux curl http://localhost:8080/api/health

# Check frontend
docker exec oscal-ux curl http://localhost:3000
```

### Restart Container

```bash
docker restart oscal-ux
```

### Update Container

```bash
# Pull new image
docker pull oscal-ux:latest

# Stop and remove old container
docker stop oscal-ux
docker rm oscal-ux

# Start new container
docker run -d --name oscal-ux -p 3000:3000 -p 8080:8080 oscal-ux:latest
```

## Deployment Scenarios

### Local Development

```bash
docker-compose up -d
```

Access at: http://localhost:3000

### Production Server

```bash
# Build optimized image
docker build --no-cache -t oscal-ux:production .

# Run with production settings
docker run -d \
  --name oscal-ux \
  -p 3000:3000 \
  -p 8080:8080 \
  --restart always \
  --memory="2g" \
  --cpus="2" \
  -e JAVA_OPTS="-Xmx1536m -Xms768m -XX:+UseG1GC" \
  oscal-ux:production
```

### Behind Nginx

1. Create nginx.conf:
```nginx
upstream frontend {
    server oscal-ux:3000;
}

upstream backend {
    server oscal-ux:8080;
}

server {
    listen 80;
    server_name your-domain.com;

    location / {
        proxy_pass http://frontend;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }

    location /api/ {
        proxy_pass http://backend/api/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }
}
```

2. Run with docker-compose:
```bash
docker-compose -f docker-compose-nginx.yml up -d
```

### Cloud Deployment

#### AWS ECS/Fargate

```bash
# Tag for ECR
docker tag oscal-ux:latest 123456789.dkr.ecr.us-east-1.amazonaws.com/oscal-ux:latest

# Push to ECR
aws ecr get-login-password --region us-east-1 | docker login --username AWS --password-stdin 123456789.dkr.ecr.us-east-1.amazonaws.com
docker push 123456789.dkr.ecr.us-east-1.amazonaws.com/oscal-ux:latest
```

#### Google Cloud Run

```bash
# Tag for GCR
docker tag oscal-ux:latest gcr.io/your-project/oscal-ux:latest

# Push to GCR
docker push gcr.io/your-project/oscal-ux:latest

# Deploy
gcloud run deploy oscal-ux \
  --image gcr.io/your-project/oscal-ux:latest \
  --platform managed \
  --region us-central1 \
  --allow-unauthenticated
```

#### Azure Container Instances

```bash
# Tag for ACR
docker tag oscal-ux:latest yourregistry.azurecr.io/oscal-ux:latest

# Push to ACR
docker push yourregistry.azurecr.io/oscal-ux:latest

# Deploy
az container create \
  --resource-group oscal-ux-rg \
  --name oscal-ux \
  --image yourregistry.azurecr.io/oscal-ux:latest \
  --ports 3000 8080 \
  --cpu 2 \
  --memory 2
```

## Health Checks

### Manual Health Check

```bash
# Backend
curl http://localhost:8080/api/health

# Frontend
curl http://localhost:3000
```

### Docker Health Check

The Dockerfile includes automatic health checks:
```dockerfile
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8080/api/health && curl -f http://localhost:3000 || exit 1
```

View health status:
```bash
docker inspect --format='{{.State.Health.Status}}' oscal-ux
```

## Troubleshooting

### Container Won't Start

```bash
# Check logs
docker logs oscal-ux

# Common issues:
# 1. Port already in use
lsof -i :3000
lsof -i :8080

# 2. Insufficient memory
docker stats oscal-ux

# 3. Image not found
docker images | grep oscal-ux
```

### Backend Not Responding

```bash
# Check if backend process is running
docker exec oscal-ux ps aux | grep java

# Check backend logs
docker exec oscal-ux cat /app/backend.log

# Restart just the backend (from inside container)
docker exec -it oscal-ux /bin/bash
kill -9 $(pgrep java)
java -jar /app/backend.jar &
```

### Frontend Not Responding

```bash
# Check if frontend process is running
docker exec oscal-ux ps aux | grep node

# Check frontend logs
docker logs oscal-ux | grep "Frontend"
```

### High Memory Usage

```bash
# Check memory usage
docker stats oscal-ux

# Reduce Java heap size
docker stop oscal-ux
docker run -d \
  --name oscal-ux \
  -p 3000:3000 \
  -p 8080:8080 \
  -e JAVA_OPTS="-Xmx512m -Xms256m" \
  oscal-ux:latest
```

### Networking Issues

```bash
# Check network connectivity
docker exec oscal-ux ping -c 3 google.com

# Check DNS
docker exec oscal-ux nslookup github.com

# Check if services can communicate
docker exec oscal-ux curl http://localhost:8080/api/health
docker exec oscal-ux curl http://localhost:3000
```

## Best Practices

### Production Deployment

1. **Use specific image tags**: Don't use `:latest` in production
   ```bash
   docker build -t oscal-ux:1.0.0 .
   ```

2. **Set resource limits**: Prevent runaway containers
   ```bash
   docker run --memory="2g" --cpus="2" oscal-ux:1.0.0
   ```

3. **Enable restart policy**: Auto-recover from failures
   ```bash
   docker run --restart unless-stopped oscal-ux:1.0.0
   ```

4. **Use read-only root filesystem**: Enhance security
   ```bash
   docker run --read-only --tmpfs /tmp oscal-ux:1.0.0
   ```

5. **Run as non-root user**: Add to Dockerfile
   ```dockerfile
   RUN useradd -m oscaluser
   USER oscaluser
   ```

### Security

- Keep base images updated
- Scan images for vulnerabilities: `docker scan oscal-ux:latest`
- Don't expose unnecessary ports
- Use secrets management for sensitive data
- Enable Docker Content Trust: `export DOCKER_CONTENT_TRUST=1`

### Monitoring

- Collect logs: Use Docker logging drivers
- Monitor metrics: Prometheus, Datadog, etc.
- Set up alerts for container failures
- Track resource usage over time

## Image Size Optimization

Current image size: ~800MB

To reduce further:
1. Use Alpine base images
2. Multi-stage builds (already implemented)
3. Remove unnecessary dependencies
4. Use `.dockerignore` (already implemented)

## Support

For issues or questions:
- **GitHub**: https://github.com/usnistgov/oscal-cli/issues
- **RegScale**: https://www.regscale.com
- **Email**: support@regscale.com

## License

Licensed for non-commercial use only. See [LICENSE.md](LICENSE.md) for details.

Commercial licenses available from RegScale: https://www.regscale.com

---

**Built with**: Docker, Next.js 15, Spring Boot 2.7, Java 11, Node.js 18
