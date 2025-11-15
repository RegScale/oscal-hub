# Startup and Shutdown Scripts Guide

**Date**: October 31, 2025
**Status**: Active

## Overview

This guide explains how to use the startup and shutdown scripts for the OSCAL HUB application, which includes a Spring Boot backend, Next.js frontend, and PostgreSQL database running in Docker.

## Quick Reference

```bash
# Start development environment
./dev.sh

# Start production-like environment
./start.sh

# Stop everything (servers + Docker containers)
./stop.sh
```

## Architecture

The application consists of three main components:

1. **PostgreSQL Database** - Runs in Docker container on port 5432
2. **Spring Boot Backend** - Runs locally on port 8080
3. **Next.js Frontend** - Runs locally on port 3000

## Scripts Explained

### `dev.sh` - Development Mode

**What it does:**
- Checks if Docker is running
- Starts PostgreSQL container (if not already running)
- Waits for PostgreSQL to be healthy
- Cleans up ports 8080 and 3000
- Builds and starts the backend (Spring Boot)
- Starts the frontend (Next.js in dev mode)

**When to use:**
- Active development with hot-reload
- Debugging backend or frontend code
- When you need to see real-time changes

**Output:**
- Backend: http://localhost:8080/api
- Frontend: http://localhost:3000
- pgAdmin: http://localhost:5050 (if started)

### `start.sh` - Production-like Mode

**What it does:**
- Similar to `dev.sh` but with production builds
- More comprehensive health checks
- Writes logs to separate files
- Waits for services to be fully ready before proceeding

**When to use:**
- Testing production builds locally
- Verifying production configurations
- Performance testing

**Log files:**
- `backend-build.log` - Maven build output
- `backend.log` - Backend runtime logs
- `frontend.log` - Frontend runtime logs

### `stop.sh` - Comprehensive Shutdown (NEW)

**What it does:**
1. Stops Spring Boot backend processes
2. Stops Next.js frontend processes
3. Clears ports 8080, 3000, 3001
4. **Stops PostgreSQL Docker container** ⭐
5. **Stops pgAdmin Docker container** ⭐
6. **Stops OSCAL UX container (if running)** ⭐
7. **Verifies port 5432 is freed** ⭐

**When to use:**
- **When switching between projects** (frees port 5432)
- End of day shutdown
- Before system maintenance
- When you need to ensure all resources are released

**Why this was needed:**
- Previous version only stopped backend/frontend processes
- PostgreSQL container kept running, locking port 5432
- Other applications couldn't use PostgreSQL port
- Manual `docker stop` was required

## Troubleshooting

### Port 5432 Still in Use

If another application is using port 5432:

```bash
# Check what's using the port
lsof -i :5432

# Force kill the process
lsof -ti:5432 | xargs kill -9

# Or stop with Docker
docker stop oscal-postgres-dev
docker rm oscal-postgres-dev
```

### Backend Won't Start

```bash
# Check if backend is still running
ps aux | grep spring-boot

# Kill any hung processes
pkill -f 'spring-boot:run'

# Check if port 8080 is in use
lsof -i :8080
```

### PostgreSQL Won't Start

```bash
# Check PostgreSQL logs
docker logs oscal-postgres-dev

# Remove and recreate the container
docker-compose -f docker-compose-postgres.yml down
docker-compose -f docker-compose-postgres.yml up -d

# If you need a fresh database (CAUTION: deletes data)
docker-compose -f docker-compose-postgres.yml down -v
```

### Docker Not Running

```bash
# Start Docker Desktop
open -a Docker

# Wait for Docker to start, then verify
docker info
```

## Docker Compose Files

The project uses multiple Docker Compose files:

- **`docker-compose-postgres.yml`** - Standalone PostgreSQL + pgAdmin (used by dev.sh/start.sh)
- **`docker-compose.yml`** - Full stack in containers (postgres + oscal-ux + pgadmin)
- **`docker-compose.prod.yml`** - Production configuration
- **`docker-compose-monitoring.yml`** - Monitoring tools

## Environment Variables

Create a `.env` file in the project root to customize settings:

```bash
# Database
DB_NAME=oscal_dev
DB_USERNAME=oscal_user
DB_PASSWORD=oscal_dev_password

# Spring Boot
SPRING_PROFILES_ACTIVE=dev
JWT_SECRET=your-secret-key-here

# Azure Storage (optional)
AZURE_STORAGE_CONNECTION_STRING=...
AZURE_STORAGE_CONTAINER_NAME=...

# pgAdmin
PGADMIN_EMAIL=admin@oscal.local
PGADMIN_PASSWORD=admin
```

## Common Workflows

### Start Working (Morning)

```bash
# Start development environment
./dev.sh

# Open browser to http://localhost:3000
# Backend API available at http://localhost:8080/api
```

### End of Day

```bash
# Stop everything and free resources
./stop.sh

# Verify everything is stopped
docker ps
```

### Switch to Another Project

```bash
# Free port 5432 for other projects
./stop.sh

# Verify PostgreSQL port is free
lsof -i :5432
# (should return nothing)
```

### Restart After Code Changes

```bash
# Stop everything
./stop.sh

# Wait a moment
sleep 2

# Restart
./dev.sh
```

### Clean Slate (Reset Everything)

```bash
# Stop all containers and remove volumes (CAUTION: deletes data)
./stop.sh
docker-compose -f docker-compose-postgres.yml down -v

# Restart from scratch
./dev.sh
```

## Best Practices

1. **Always use `./stop.sh` when done** - Don't let PostgreSQL run unnecessarily
2. **Check ports before starting** - Use `lsof -i :5432` to verify port is free
3. **Monitor logs** - Check `backend.log` and `frontend.log` when debugging
4. **Use `.env` for secrets** - Don't commit sensitive data to git
5. **Run `./stop.sh` between projects** - Prevent port conflicts

## Port Reference

| Port | Service | Purpose |
|------|---------|---------|
| 3000 | Frontend | Next.js development server |
| 5050 | pgAdmin | PostgreSQL web UI |
| 5432 | PostgreSQL | Database server |
| 8080 | Backend | Spring Boot REST API |

## Additional Commands

### View Running Containers

```bash
docker ps --filter "name=oscal"
```

### View Container Logs

```bash
# PostgreSQL logs
docker logs oscal-postgres-dev

# pgAdmin logs
docker logs oscal-pgadmin

# Follow logs in real-time
docker logs -f oscal-postgres-dev
```

### Connect to PostgreSQL

```bash
# Via docker exec
docker exec -it oscal-postgres-dev psql -U oscal_user -d oscal_dev

# Via psql (if installed locally)
psql -h localhost -p 5432 -U oscal_user -d oscal_dev
```

### Database Management

```bash
# Backup database
docker exec oscal-postgres-dev pg_dump -U oscal_user oscal_dev > backup.sql

# Restore database
docker exec -i oscal-postgres-dev psql -U oscal_user -d oscal_dev < backup.sql
```

## References

- [Docker Documentation](https://docs.docker.com/)
- [PostgreSQL Docker Image](https://hub.docker.com/_/postgres)
- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
- [Next.js Documentation](https://nextjs.org/docs)

---

**Need Help?**

If you encounter issues not covered here:
1. Check the logs: `backend.log`, `frontend.log`, or `docker logs oscal-postgres-dev`
2. Verify Docker is running: `docker info`
3. Check for port conflicts: `lsof -i :5432 -i :8080 -i :3000`
4. Try a clean restart: `./stop.sh && ./dev.sh`
