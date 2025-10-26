# OSCAL Tools - Local Deployment Guide

**Version**: 1.0.0
**Date**: 2025-10-26
**Purpose**: Deploy OSCAL Tools on your local machine or VM for testing and development

---

## Table of Contents

1. [Overview](#overview)
2. [Quick Start (5 Minutes)](#quick-start-5-minutes)
3. [Prerequisites](#prerequisites)
4. [Installation Methods](#installation-methods)
5. [Using the Application](#using-the-application)
6. [Management Commands](#management-commands)
7. [Troubleshooting](#troubleshooting)
8. [Advanced Configuration](#advanced-configuration)
9. [Uninstalling](#uninstalling)

---

## Overview

This guide shows you how to run the full OSCAL Tools stack locally on your laptop or a VM. Perfect for:

- **Testing** - Try out OSCAL Tools before deploying to production
- **Development** - Make and test changes locally
- **Training** - Learn OSCAL in a safe environment
- **Offline Use** - Run OSCAL Tools without internet connectivity
- **Demos** - Show OSCAL Tools to stakeholders

### What Gets Installed

The local deployment includes everything you need:

- ✅ **Frontend** - Next.js web application (port 3000)
- ✅ **Backend API** - Spring Boot REST API (port 8080)
- ✅ **PostgreSQL Database** - Production-grade database (port 5432)
- ✅ **pgAdmin** - Database management UI (port 5050)

All running in Docker containers for easy setup and isolation.

---

## Quick Start (5 Minutes)

### Option 1: Automated Script (Easiest)

```bash
# 1. Clone the repository
git clone https://github.com/usnistgov/oscal-cli.git
cd oscal-cli

# 2. Run the deployment script
./local-deploy.sh

# 3. Open your browser
open http://localhost:3000
```

That's it! The script will:
- ✅ Check prerequisites
- ✅ Build backend (Maven)
- ✅ Build frontend (npm)
- ✅ Build Docker images
- ✅ Start all containers
- ✅ Wait for everything to be ready

**Total time**: 5-10 minutes (longer on first run)

### Option 2: Manual Docker Compose

```bash
# 1. Clone and build
git clone https://github.com/usnistgov/oscal-cli.git
cd oscal-cli

# 2. Build backend
cd back-end
mvn clean package -DskipTests
cd ..

# 3. Build frontend
cd front-end
npm ci
npm run build
cd ..

# 4. Start with Docker Compose
docker-compose up -d

# 5. Open browser
open http://localhost:3000
```

---

## Prerequisites

### Required Software

1. **Docker Desktop** (version 20.10+)
   - **macOS**: https://docs.docker.com/desktop/install/mac-install/
   - **Windows**: https://docs.docker.com/desktop/install/windows-install/
   - **Linux**: https://docs.docker.com/desktop/install/linux-install/

2. **Git**
   - **macOS**: Already installed or via Homebrew: `brew install git`
   - **Windows**: https://git-scm.com/download/win
   - **Linux**: `sudo apt-get install git` (Ubuntu/Debian)

### Optional (for manual builds)

3. **Java 21** (if building manually)
   ```bash
   # macOS
   brew install openjdk@21

   # Windows
   winget install EclipseAdoptium.Temurin.21.JDK

   # Linux
   sudo apt install openjdk-21-jdk
   ```

4. **Node.js 18** (if building manually)
   ```bash
   # macOS
   brew install node@18

   # Windows
   winget install OpenJS.NodeJS.LTS

   # Linux
   curl -fsSL https://deb.nodesource.com/setup_18.x | sudo -E bash -
   sudo apt-get install -y nodejs
   ```

### System Requirements

| Requirement | Minimum | Recommended |
|-------------|---------|-------------|
| **RAM** | 4 GB | 8 GB |
| **Disk Space** | 10 GB free | 20 GB free |
| **CPU** | 2 cores | 4 cores |
| **OS** | macOS 11+, Windows 10+, Ubuntu 20.04+ | Latest versions |

### Verify Prerequisites

```bash
# Check Docker
docker --version
# Expected: Docker version 20.10.0 or higher

# Check Docker Compose
docker-compose --version
# Expected: Docker Compose version 2.x or higher

# Check if Docker is running
docker info
# Should show Docker engine information
```

---

## Installation Methods

### Method 1: Automated Script (Recommended)

The `local-deploy.sh` script automates everything:

```bash
# Clone repository
git clone https://github.com/usnistgov/oscal-cli.git
cd oscal-cli

# Make script executable (if needed)
chmod +x local-deploy.sh

# Run deployment
./local-deploy.sh
```

**What the script does:**

1. ✅ Checks all prerequisites (Docker, Docker Compose, disk space)
2. ✅ Builds backend with Maven
3. ✅ Builds frontend with npm
4. ✅ Creates Docker images
5. ✅ Starts PostgreSQL database
6. ✅ Starts application container
7. ✅ Waits for all services to be ready
8. ✅ Displays access URLs and next steps

**Script output:**
```
========================================
  OSCAL Tools - Local Deployment
========================================

Step 1/4: Building Backend (Maven)
✓ Backend built successfully

Step 2/4: Building Frontend (npm)
✓ Frontend built successfully

Step 3/4: Building Docker Images
✓ Docker images built successfully

Step 4/4: Starting Application
✓ PostgreSQL is ready
✓ Backend API is ready
✓ Frontend is ready

========================================
  🎉 OSCAL Tools is Ready!
========================================

Application URLs:
  Frontend:  http://localhost:3000
  Backend API: http://localhost:8080/api
  Swagger UI: http://localhost:8080/swagger-ui.html

Next Steps:
  1. Open http://localhost:3000 in your browser
  2. Register a new user account
  3. Start using OSCAL Tools!
```

### Method 2: Docker Compose Only

If you already have Java and Node.js installed:

```bash
# 1. Clone repository
git clone https://github.com/usnistgov/oscal-cli.git
cd oscal-cli

# 2. Build backend
cd back-end
mvn clean package -DskipTests
cd ..

# 3. Build frontend
cd front-end
npm ci --legacy-peer-deps
npm run build
cd ..

# 4. Start containers
docker-compose up -d

# 5. Check status
docker-compose ps

# 6. View logs (optional)
docker-compose logs -f
```

### Method 3: Development Mode (No Docker)

For active development without Docker:

```bash
# 1. Clone repository
git clone https://github.com/usnistgov/oscal-cli.git
cd oscal-cli

# 2. Start PostgreSQL (Docker only for database)
docker-compose -f docker-compose-postgres.yml up -d

# 3. Start backend (in one terminal)
cd back-end
mvn spring-boot:run -Dspring.profiles.active=dev

# 4. Start frontend (in another terminal)
cd front-end
npm install
npm run dev

# Access:
# - Frontend: http://localhost:3000
# - Backend: http://localhost:8080
```

---

## Using the Application

### First-Time Setup

1. **Open your browser** to http://localhost:3000

2. **Register a new account**:
   - Click "Register" or "Sign Up"
   - Fill in your details:
     - Username: `your-username`
     - Email: `your-email@example.com`
     - Password: At least 10 characters with uppercase, lowercase, digit, and special character

3. **Log in** with your new credentials

4. **Start validating OSCAL documents**!

### Example Workflows

#### 1. Validate an OSCAL Document

```bash
# Prepare a test OSCAL file (SSP example)
cat > test-ssp.json <<EOF
{
  "system-security-plan": {
    "uuid": "12345678-1234-1234-1234-123456789012",
    "metadata": {
      "title": "Test System Security Plan",
      "last-modified": "2025-10-26T00:00:00Z",
      "version": "1.0",
      "oscal-version": "1.0.0"
    },
    "import-profile": {
      "href": "https://example.com/profile.json"
    },
    "system-characteristics": {
      "system-name": "Test System",
      "description": "A test system",
      "security-sensitivity-level": "low"
    }
  }
}
EOF
```

Then in the UI:
1. Go to **Validate** page
2. Upload `test-ssp.json`
3. Click **Validate**
4. View results

#### 2. Convert Between Formats

1. Go to **Convert** page
2. Upload OSCAL JSON file
3. Select target format (XML or YAML)
4. Click **Convert**
5. Download converted file

#### 3. Resolve a Profile

1. Go to **Profile Resolution** page
2. Upload an OSCAL profile
3. Click **Resolve**
4. Download resolved catalog

### Accessing the Database

If you need to view or manage the database directly:

1. **Open pgAdmin**: http://localhost:5050
2. **Login**:
   - Email: `admin@oscal.local`
   - Password: `admin`

3. **Add Server**:
   - Right-click **Servers** → **Register** → **Server**
   - **General tab**:
     - Name: `OSCAL Local DB`
   - **Connection tab**:
     - Host: `postgres` (or `localhost` if outside Docker)
     - Port: `5432`
     - Database: `oscal_dev`
     - Username: `oscal_user`
     - Password: `oscal_dev_password`

4. **Browse data**:
   - Expand: **Servers** → **OSCAL Local DB** → **Databases** → **oscal_dev** → **Schemas** → **public** → **Tables**

### API Documentation

Swagger UI is available at: http://localhost:8080/swagger-ui.html

This provides:
- Interactive API documentation
- Ability to test API endpoints directly
- Request/response examples
- Authentication testing

---

## Management Commands

### Using the Script

```bash
# View status
./local-deploy.sh status

# View logs
./local-deploy.sh logs

# Stop application
./local-deploy.sh stop

# Restart application
./local-deploy.sh restart

# Clean everything (removes data!)
./local-deploy.sh clean

# Show help
./local-deploy.sh help
```

### Using Docker Compose

```bash
# View running containers
docker-compose ps

# View logs
docker-compose logs -f

# View logs for specific service
docker-compose logs -f oscal-ux
docker-compose logs -f postgres

# Stop application
docker-compose stop

# Start application
docker-compose start

# Restart specific service
docker-compose restart oscal-ux

# Stop and remove containers
docker-compose down

# Stop and remove containers + volumes (deletes data!)
docker-compose down -v
```

### Database Management

```bash
# Backup database
docker-compose exec postgres pg_dump -U oscal_user oscal_dev > backup.sql

# Restore database
cat backup.sql | docker-compose exec -T postgres psql -U oscal_user oscal_dev

# Connect to database
docker-compose exec postgres psql -U oscal_user -d oscal_dev

# List tables
docker-compose exec postgres psql -U oscal_user -d oscal_dev -c "\dt"

# Reset database (WARNING: Deletes all data!)
docker-compose down -v
docker-compose up -d
```

---

## Troubleshooting

### Issue: Script fails with "Docker is not running"

**Symptom**:
```
✗ Docker daemon is not running
Please start Docker Desktop
```

**Solution**:
1. Start Docker Desktop application
2. Wait for Docker to fully start (icon in system tray)
3. Verify with: `docker info`
4. Run script again: `./local-deploy.sh`

### Issue: Port already in use

**Symptom**:
```
Error: Port 3000 is already in use
or
Error: Port 8080 is already in use
```

**Solution Option 1** - Stop the conflicting process:
```bash
# Find what's using port 3000
lsof -i :3000

# Kill the process
kill -9 <PID>

# Or for port 8080
lsof -i :8080
kill -9 <PID>
```

**Solution Option 2** - Change the port in docker-compose.yml:
```yaml
# For frontend
ports:
  - "3001:3000"  # Change 3000 to 3001

# For backend
ports:
  - "8081:8080"  # Change 8080 to 8081
```

### Issue: Build fails - "Cannot download dependencies"

**Symptom**:
```
Failed to download Maven dependencies
or
npm ERR! network timeout
```

**Solution**:
1. Check internet connection
2. Retry with: `./local-deploy.sh`
3. Or manually rebuild:
   ```bash
   cd back-end
   mvn clean package -DskipTests -U
   cd ../front-end
   npm ci --legacy-peer-deps
   ```

### Issue: Container keeps restarting

**Symptom**:
```
docker-compose ps shows "Restarting" status
```

**Solution**:
```bash
# View logs to see error
docker-compose logs oscal-ux

# Common causes:
# 1. Database not ready - wait 30 seconds and check again
# 2. Environment variable missing - check docker-compose.yml
# 3. Port conflict - see "Port already in use" above

# Try restart
docker-compose restart
```

### Issue: Cannot connect to database

**Symptom**:
Application logs show:
```
Connection refused: localhost:5432
```

**Solution**:
```bash
# 1. Check if PostgreSQL is running
docker-compose ps postgres

# 2. If not running, start it
docker-compose up -d postgres

# 3. Wait for PostgreSQL to be ready
docker-compose exec postgres pg_isready -U oscal_user

# 4. Restart application
docker-compose restart oscal-ux
```

### Issue: Frontend shows "Cannot connect to API"

**Symptom**:
Frontend loads but shows connection error

**Solution**:
```bash
# 1. Check if backend is running
curl http://localhost:8080/api/health

# 2. If not responding, check logs
docker-compose logs oscal-ux

# 3. Common fixes:
# - Wait longer (backend takes 30-60 seconds to start)
# - Check if port 8080 is accessible
# - Restart: docker-compose restart oscal-ux
```

### Issue: "Out of disk space"

**Symptom**:
```
no space left on device
```

**Solution**:
```bash
# 1. Clean up Docker
docker system prune -a --volumes

# 2. Remove old images
docker image prune -a

# 3. Check disk space
df -h

# 4. If still low, remove unused applications/files
```

### Issue: Slow performance

**Symptoms**:
- Application is slow to respond
- High CPU/memory usage
- Long startup time

**Solutions**:

1. **Increase Docker resources**:
   - Open Docker Desktop → Settings → Resources
   - Increase:
     - CPUs: 4 (from 2)
     - Memory: 8 GB (from 4 GB)
     - Swap: 2 GB
   - Apply & Restart

2. **Close other applications**:
   - Free up system resources
   - Close unnecessary browser tabs

3. **Check resource usage**:
   ```bash
   docker stats
   ```

---

## Advanced Configuration

### Changing Default Ports

Edit `docker-compose.yml`:

```yaml
services:
  oscal-ux:
    ports:
      - "3001:3000"  # Frontend: Change 3001 to desired port
      - "8081:8080"  # Backend: Change 8081 to desired port

  postgres:
    ports:
      - "5433:5432"  # PostgreSQL: Change 5433 to desired port

  pgadmin:
    ports:
      - "5051:80"    # pgAdmin: Change 5051 to desired port
```

### Enabling HTTPS Locally

For production-like HTTPS testing:

1. **Generate self-signed certificate**:
   ```bash
   openssl req -x509 -nodes -days 365 -newkey rsa:2048 \
     -keyout nginx/certs/localhost.key \
     -out nginx/certs/localhost.crt \
     -subj "/CN=localhost"
   ```

2. **Add nginx reverse proxy** to `docker-compose.yml`
3. **Update CORS settings** in backend

See `docs/TLS-CONFIGURATION.md` for detailed instructions.

### Persistent Data Location

By default, data is stored in Docker volumes. To use a specific directory:

Edit `docker-compose.yml`:

```yaml
volumes:
  postgres_data:
    driver: local
    driver_opts:
      type: none
      o: bind
      device: /path/to/your/data/directory
```

### Custom Configuration

Create `.env` file in project root:

```bash
# Database
DB_NAME=oscal_dev
DB_USER=oscal_user
DB_PASSWORD=oscal_dev_password

# Application
JWT_SECRET=dev-secret-key-at-least-32-characters-long
CORS_ALLOWED_ORIGINS=http://localhost:3000,http://localhost:3001

# Features
SPRING_PROFILES_ACTIVE=dev
SWAGGER_ENABLED=true
RATE_LIMIT_ENABLED=false
```

---

## Uninstalling

### Option 1: Using the Script

```bash
# Stop and remove everything
./local-deploy.sh clean

# Answer "yes" to confirm
```

### Option 2: Manual Cleanup

```bash
# 1. Stop and remove containers
docker-compose down -v

# 2. Remove images
docker-compose down --rmi all

# 3. Remove cloned repository
cd ..
rm -rf oscal-cli

# 4. (Optional) Clean all Docker data
docker system prune -a --volumes
```

---

## Performance Tips

### For Faster Builds

```bash
# Skip tests during Maven build
mvn clean package -DskipTests

# Use Maven offline mode (after first build)
mvn -o package

# Use npm cache
npm ci --prefer-offline
```

### For Development

```bash
# Use development mode (faster rebuilds)
cd back-end
mvn spring-boot:run -Dspring.profiles.active=dev

cd front-end
npm run dev
```

---

## Comparison: Local vs Azure Deployment

| Feature | Local Deployment | Azure Deployment |
|---------|------------------|------------------|
| **Setup Time** | 5-10 minutes | 30-60 minutes (one-time) |
| **Cost** | Free | ~$100/month |
| **Internet Required** | Only for initial download | Always |
| **Scalability** | Single machine | Auto-scaling |
| **Backup** | Manual | Automated (7 days) |
| **SSL/HTTPS** | Optional (self-signed) | Automatic |
| **Updates** | Manual (`git pull`) | Automatic CI/CD |
| **Best For** | Testing, Development, Demos | Production, Teams |

---

## Next Steps

After successful local deployment:

1. **Read the User Guide**: `docs/COMPONENT-BUILDER-GUIDE.md`
2. **Try sample OSCAL files**: Check `examples/` directory (if available)
3. **Explore the API**: http://localhost:8080/swagger-ui.html
4. **Watch application logs**: `./local-deploy.sh logs`
5. **Learn about Azure deployment**: `docs/AZURE-DEPLOYMENT-GUIDE.md`

---

## Support

### Documentation

- **This Guide**: Local deployment
- **Azure Guide**: `docs/AZURE-DEPLOYMENT-GUIDE.md`
- **PostgreSQL Guide**: `docs/POSTGRESQL-MIGRATION.md`
- **Security Guide**: `docs/SECURITY-HARDENING-SUMMARY.md`
- **Main README**: `README.md`

### Getting Help

- **Issues**: https://github.com/usnistgov/oscal-cli/issues
- **Discussions**: https://github.com/usnistgov/oscal-cli/discussions
- **OSCAL Documentation**: https://pages.nist.gov/OSCAL/

### Common Resources

- **Docker Documentation**: https://docs.docker.com/
- **Docker Compose**: https://docs.docker.com/compose/
- **PostgreSQL**: https://www.postgresql.org/docs/
- **Spring Boot**: https://spring.io/projects/spring-boot
- **Next.js**: https://nextjs.org/docs

---

## Checklist

Before considering your local deployment complete:

- [ ] Docker Desktop installed and running
- [ ] Repository cloned
- [ ] `./local-deploy.sh` completed successfully
- [ ] Frontend accessible at http://localhost:3000
- [ ] Backend health check passes: http://localhost:8080/api/health
- [ ] User account registered
- [ ] Able to log in successfully
- [ ] Validated at least one OSCAL document
- [ ] Converted a document between formats
- [ ] Reviewed Swagger UI documentation

---

**🎉 Congratulations!** You now have OSCAL Tools running locally!

For production deployment to Azure, see: **`docs/AZURE-DEPLOYMENT-GUIDE.md`**

---

**Document Version**: 1.0.0
**Last Updated**: 2025-10-26
**Status**: ✅ Complete

---

**End of Local Deployment Guide**
