# PostgreSQL Migration Guide

**Status**: ✅ Complete
**Date**: 2025-10-26
**Section**: 10 - Production Database (Security Hardening Plan)

## Table of Contents

1. [Overview](#overview)
2. [What Changed](#what-changed)
3. [Getting Started](#getting-started)
4. [Local Development Setup](#local-development-setup)
5. [Docker Deployment](#docker-deployment)
6. [Database Management](#database-management)
7. [Common Operations](#common-operations)
8. [pgAdmin Web UI](#pgadmin-web-ui)
9. [Troubleshooting](#troubleshooting)
10. [Migration from H2](#migration-from-h2)

---

## Overview

The OSCAL Tools application has been migrated from **H2 (in-memory database)** to **PostgreSQL (production-grade database)**.

### Why PostgreSQL?

| Feature | H2 (Before) | PostgreSQL (Now) |
|---------|-------------|------------------|
| **Production Ready** | ❌ Development only | ✅ Enterprise-grade |
| **Data Persistence** | ⚠️ File-based, fragile | ✅ Robust, ACID compliant |
| **Concurrency** | ⚠️ Limited | ✅ Excellent |
| **Performance** | ⚠️ Good for dev | ✅ Optimized for production |
| **Backup/Recovery** | ❌ Manual | ✅ Built-in tools |
| **Security** | ⚠️ Basic | ✅ Advanced (SCRAM-SHA-256, SSL, etc.) |
| **Scalability** | ❌ Limited | ✅ Horizontal & vertical |

---

## What Changed

### Files Modified

#### 1. **Maven Dependencies** (`back-end/pom.xml`)

**Before:**
```xml
<dependency>
    <groupId>com.h2database</groupId>
    <artifactId>h2</artifactId>
    <scope>runtime</scope>
</dependency>
```

**After:**
```xml
<!-- PostgreSQL Driver (Production) -->
<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
    <scope>runtime</scope>
</dependency>

<!-- H2 Database (Testing Only) -->
<dependency>
    <groupId>com.h2database</groupId>
    <artifactId>h2</artifactId>
    <scope>test</scope>
</dependency>
```

**Changes**:
- Added PostgreSQL driver
- Moved H2 to test scope only (for unit tests)

#### 2. **Application Configuration** (`application.properties`)

**Before:**
```properties
spring.datasource.url=jdbc:h2:file:./data/oscal-history
spring.datasource.driverClassName=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=
spring.jpa.database-platform=org.hibernate.dialect.H2Dialect
spring.h2.console.enabled=true
```

**After:**
```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/oscal_production
spring.datasource.driverClassName=org.postgresql.Driver
spring.datasource.username=oscal_user
spring.datasource.password=${DB_PASSWORD:}
spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect
spring.h2.console.enabled=false

# Connection Pool Settings (HikariCP)
spring.datasource.hikari.maximum-pool-size=20
spring.datasource.hikari.minimum-idle=5
spring.datasource.hikari.connection-timeout=30000
```

**Changes**:
- PostgreSQL JDBC URL
- PostgreSQL dialect
- Removed H2 console
- Added connection pool settings

#### 3. **Development Configuration** (`application-dev.properties`)

**Before:**
```properties
spring.datasource.url=jdbc:h2:file:./data/oscal-history-dev
spring.h2.console.enabled=true
```

**After:**
```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/oscal_dev
spring.datasource.driverClassName=org.postgresql.Driver
spring.datasource.username=oscal_user
spring.datasource.password=oscal_dev_password
spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect
spring.h2.console.enabled=false

# Connection Pool Settings
spring.datasource.hikari.maximum-pool-size=10
spring.datasource.hikari.minimum-idle=5
```

#### 4. **Security Configuration** (`SecurityConfig.java`)

**Removed**:
- `/h2-console/**` endpoint
- H2 console comments

**Result**: Cleaner security configuration without development-only endpoints

#### 5. **Docker Compose Files**

**New file**: `docker-compose-postgres.yml` - Standalone PostgreSQL for development

**Updated**: `docker-compose.yml` - Added PostgreSQL service

**Updated**: `docker-compose.prod.yml` - Already had PostgreSQL (now consistent)

---

## Getting Started

### Prerequisites

- **Docker Desktop** (for containerized PostgreSQL)
- OR **PostgreSQL 15** installed locally
- **Java 21**
- **Maven 3.9+**

### Quick Start (Recommended for Development)

**Option 1: Using Docker (Easiest)**

```bash
# 1. Start PostgreSQL in Docker
docker-compose -f docker-compose-postgres.yml up -d

# 2. Verify PostgreSQL is running
docker ps | grep postgres

# 3. Start your application
cd back-end
mvn spring-boot:run -Dspring.profiles.active=dev
```

**Option 2: Using full Docker Compose**

```bash
# Starts both PostgreSQL and the application
docker-compose up --build
```

---

## Local Development Setup

### Step 1: Start PostgreSQL

#### Using Docker (Recommended)

```bash
# Start PostgreSQL container
docker-compose -f docker-compose-postgres.yml up -d

# Check status
docker ps | grep postgres

# View logs
docker logs -f oscal-postgres-dev

# Stop PostgreSQL
docker-compose -f docker-compose-postgres.yml down

# Stop and remove data (clean start)
docker-compose -f docker-compose-postgres.yml down -v
```

#### Using Homebrew (macOS)

```bash
# Install PostgreSQL
brew install postgresql@15

# Start PostgreSQL service
brew services start postgresql@15

# Create database and user
createdb oscal_dev
createuser oscal_user
psql -c "ALTER USER oscal_user WITH PASSWORD 'oscal_dev_password';"
psql -c "GRANT ALL PRIVILEGES ON DATABASE oscal_dev TO oscal_user;"
```

### Step 2: Configure Your Application

The application is already configured! The `application-dev.properties` file has PostgreSQL settings:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/oscal_dev
spring.datasource.username=oscal_user
spring.datasource.password=oscal_dev_password
```

### Step 3: Run Your Application

```bash
cd back-end
mvn spring-boot:run -Dspring.profiles.active=dev
```

**Expected output:**
```
HikariPool-1 - Added connection org.postgresql.jdbc.PgConnection@...
Database URL: jdbc:postgresql://localhost:5432/oscal_dev
Database version: 15.14
Started OscalCliApiApplication in 3.487 seconds
```

### Step 4: Verify Database Tables

```bash
# Access PostgreSQL CLI
docker exec -it oscal-postgres-dev psql -U oscal_user -d oscal_dev

# Or with password
docker exec -e PGPASSWORD=oscal_dev_password oscal-postgres-dev \
  psql -U oscal_user -d oscal_dev

# List all tables
oscal_dev=# \dt

# Expected output: 16 tables
#  - users
#  - audit_events
#  - operation_history
#  - library_items
#  - saved_files
#  - ... and 11 more
```

---

## Docker Deployment

### Development Environment

The `docker-compose.yml` file includes PostgreSQL:

```yaml
services:
  postgres:
    image: postgres:15-alpine
    container_name: oscal-postgres-dev
    ports:
      - "5432:5432"
    environment:
      - POSTGRES_DB=oscal_dev
      - POSTGRES_USER=oscal_user
      - POSTGRES_PASSWORD=oscal_dev_password
    volumes:
      - postgres_dev_data:/var/lib/postgresql/data

  oscal-ux:
    environment:
      - DB_URL=jdbc:postgresql://postgres:5432/oscal_dev
      - DB_USERNAME=oscal_user
      - DB_PASSWORD=oscal_dev_password
    depends_on:
      postgres:
        condition: service_healthy
```

**Start everything:**

```bash
docker-compose up --build
```

### Production Environment

The `docker-compose.prod.yml` file includes production-grade PostgreSQL with:

- **SCRAM-SHA-256 authentication** (more secure than MD5)
- **Resource limits** (CPU, memory, PIDs)
- **Health checks**
- **Security hardening** (no-new-privileges, capability dropping)
- **Persistent volumes**

**Deploy to production:**

```bash
# Create .env file with secrets
cat > .env <<EOF
DB_NAME=oscal_production
DB_USER=oscal_prod_user
DB_PASSWORD=$(openssl rand -base64 32 | tr -d '\n')
JWT_SECRET=$(openssl rand -base64 64 | tr -d '\n' | head -c 64)
CORS_ALLOWED_ORIGINS=https://your-domain.com
EOF

# Deploy
docker-compose -f docker-compose.yml -f docker-compose.prod.yml up -d
```

---

## Database Management

### Connection Information

**Development (Local):**
- **Host**: `localhost`
- **Port**: `5432`
- **Database**: `oscal_dev`
- **Username**: `oscal_user`
- **Password**: `oscal_dev_password`
- **JDBC URL**: `jdbc:postgresql://localhost:5432/oscal_dev`

**Development (Docker Compose - from app container):**
- **Host**: `postgres` (container name)
- **Port**: `5432`
- **Database**: `oscal_dev`
- **Username**: `oscal_user`
- **Password**: `oscal_dev_password`
- **JDBC URL**: `jdbc:postgresql://postgres:5432/oscal_dev`

**Production:**
- Use environment variables from `.env` file
- Never hardcode production passwords

### Accessing PostgreSQL CLI

```bash
# From Docker container
docker exec -it oscal-postgres-dev psql -U oscal_user -d oscal_dev

# With password
docker exec -e PGPASSWORD=oscal_dev_password oscal-postgres-dev \
  psql -U oscal_user -d oscal_dev

# From local PostgreSQL installation
psql -h localhost -p 5432 -U oscal_user -d oscal_dev
```

### Useful psql Commands

```sql
-- List all databases
\l

-- List all tables
\dt

-- Describe a table
\d users
\d audit_events

-- Show table contents
SELECT * FROM users;
SELECT * FROM audit_events LIMIT 10;

-- Count rows
SELECT COUNT(*) FROM users;
SELECT COUNT(*) FROM audit_events;

-- Show database size
SELECT pg_size_pretty(pg_database_size('oscal_dev'));

-- Show table sizes
SELECT
  table_name,
  pg_size_pretty(pg_total_relation_size(table_name::regclass)) AS size
FROM information_schema.tables
WHERE table_schema = 'public'
ORDER BY pg_total_relation_size(table_name::regclass) DESC;

-- Exit psql
\q
```

---

## Common Operations

### Backup Database

```bash
# Backup from Docker container
docker exec oscal-postgres-dev pg_dump -U oscal_user oscal_dev > backup.sql

# Or with gzip compression
docker exec oscal-postgres-dev pg_dump -U oscal_user oscal_dev | gzip > backup_$(date +%Y%m%d).sql.gz

# From local PostgreSQL
pg_dump -h localhost -U oscal_user oscal_dev > backup.sql
```

### Restore Database

```bash
# Restore from Docker
cat backup.sql | docker exec -i oscal-postgres-dev psql -U oscal_user -d oscal_dev

# Or from gzip
gunzip -c backup.sql.gz | docker exec -i oscal-postgres-dev psql -U oscal_user -d oscal_dev

# From local PostgreSQL
psql -h localhost -U oscal_user -d oscal_dev < backup.sql
```

### Reset Database (Clean Start)

```bash
# Stop containers
docker-compose -f docker-compose-postgres.yml down

# Remove data volume (WARNING: deletes all data!)
docker volume rm oscal-cli_postgres_dev_data

# Start fresh
docker-compose -f docker-compose-postgres.yml up -d

# Application will recreate tables on next startup
mvn spring-boot:run -Dspring.profiles.active=dev
```

### View Logs

```bash
# PostgreSQL logs
docker logs -f oscal-postgres-dev

# Application logs (shows database queries)
docker logs -f oscal-ux-dev

# Or if running locally
tail -f back-end/logs/spring.log
```

---

## pgAdmin Web UI

pgAdmin provides a graphical interface for managing PostgreSQL databases.

### Starting pgAdmin

pgAdmin is included in `docker-compose-postgres.yml`:

```bash
# Start PostgreSQL and pgAdmin
docker-compose -f docker-compose-postgres.yml up -d

# Access pgAdmin
open http://localhost:5050
```

### pgAdmin Login

- **URL**: http://localhost:5050
- **Email**: `admin@oscal.local`
- **Password**: `admin`

### Adding Server in pgAdmin

1. Open http://localhost:5050
2. Login with credentials above
3. Right-click **Servers** → **Register** → **Server**
4. **General tab**:
   - Name: `OSCAL Dev`
5. **Connection tab**:
   - Host: `postgres` (use container name, NOT `localhost`!)
   - Port: `5432`
   - Database: `oscal_dev`
   - Username: `oscal_user`
   - Password: `oscal_dev_password`
   - Save password: ✓
6. Click **Save**

### Using pgAdmin

Once connected, you can:

- **Browse tables**: Servers → OSCAL Dev → Databases → oscal_dev → Schemas → public → Tables
- **Query data**: Right-click table → View/Edit Data → All Rows
- **Run SQL**: Tools → Query Tool
- **Export data**: Right-click table → Import/Export
- **View table structure**: Right-click table → Properties

---

## Troubleshooting

### Issue: "Connection refused" when starting application

**Symptoms:**
```
org.postgresql.util.PSQLException: Connection to localhost:5432 refused
```

**Cause**: PostgreSQL is not running

**Solution:**
```bash
# Check if PostgreSQL is running
docker ps | grep postgres

# If not running, start it
docker-compose -f docker-compose-postgres.yml up -d

# Wait for health check to pass (10-15 seconds)
docker ps | grep "(healthy)"

# Then restart your application
mvn spring-boot:run -Dspring.profiles.active=dev
```

### Issue: "Password authentication failed"

**Symptoms:**
```
org.postgresql.util.PSQLException: FATAL: password authentication failed for user "oscal_user"
```

**Cause**: Wrong password or user doesn't exist

**Solution:**

```bash
# Check environment variables
docker exec oscal-postgres-dev env | grep POSTGRES

# Reset user password
docker exec -it oscal-postgres-dev psql -U postgres -c \
  "ALTER USER oscal_user WITH PASSWORD 'oscal_dev_password';"

# Or recreate container
docker-compose -f docker-compose-postgres.yml down -v
docker-compose -f docker-compose-postgres.yml up -d
```

### Issue: "Database 'oscal_dev' does not exist"

**Symptoms:**
```
org.postgresql.util.PSQLException: FATAL: database "oscal_dev" does not exist
```

**Cause**: Database not created

**Solution:**

```bash
# Create database
docker exec -it oscal-postgres-dev psql -U postgres -c "CREATE DATABASE oscal_dev OWNER oscal_user;"

# Or recreate container (database is created automatically on first run)
docker-compose -f docker-compose-postgres.yml down -v
docker-compose -f docker-compose-postgres.yml up -d
```

### Issue: Tables not created

**Symptoms:**
Application starts but tables are empty when querying

**Cause**: `ddl-auto` setting or Hibernate not running

**Solution:**

Check configuration:

```properties
# In application-dev.properties
spring.jpa.hibernate.ddl-auto=update  # Should be 'update' for dev
```

**Force recreation:**

```bash
# Stop application
# Change ddl-auto to 'create' temporarily
spring.jpa.hibernate.ddl-auto=create

# Restart application (will drop and recreate all tables)
mvn spring-boot:run -Dspring.profiles.active=dev

# Change back to 'update' after tables are created
spring.jpa.hibernate.ddl-auto=update
```

### Issue: Port 5432 already in use

**Symptoms:**
```
Error starting userland proxy: listen tcp4 0.0.0.0:5432: bind: address already in use
```

**Cause**: Another PostgreSQL instance running on port 5432

**Solution:**

```bash
# Find what's using port 5432
lsof -i :5432

# Stop local PostgreSQL
brew services stop postgresql@15

# Or use a different port in docker-compose
ports:
  - "5433:5432"  # Change host port to 5433

# Update JDBC URL in application
spring.datasource.url=jdbc:postgresql://localhost:5433/oscal_dev
```

### Issue: Slow queries / Performance

**Symptoms:**
Application is slow, queries take a long time

**Solutions:**

1. **Check connection pool**:
```properties
spring.datasource.hikari.maximum-pool-size=20  # Increase if needed
spring.datasource.hikari.minimum-idle=5
```

2. **Enable query logging** (temporarily):
```properties
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true
logging.level.org.hibernate.SQL=DEBUG
logging.level.org.hibernate.type.descriptor.sql.BasicBinder=TRACE
```

3. **Analyze slow queries in psql**:
```sql
-- Enable query stats
ALTER DATABASE oscal_dev SET log_statement = 'all';
ALTER DATABASE oscal_dev SET log_min_duration_statement = 1000; -- Log queries > 1s

-- View table indexes
\d+ users

-- Create index if missing
CREATE INDEX idx_users_username ON users(username);
```

4. **Increase PostgreSQL resources** (in docker-compose):
```yaml
deploy:
  resources:
    limits:
      memory: 1G  # Increase from 512M
```

### Issue: Out of disk space

**Symptoms:**
```
ERROR: could not extend file ... No space left on device
```

**Solutions:**

1. **Check Docker disk usage**:
```bash
docker system df -v
```

2. **Clean up old data**:
```bash
# Remove old audit logs
docker exec -e PGPASSWORD=oscal_dev_password oscal-postgres-dev \
  psql -U oscal_user -d oscal_dev -c \
  "DELETE FROM audit_events WHERE timestamp < NOW() - INTERVAL '90 days';"

# Vacuum database
docker exec -e PGPASSWORD=oscal_dev_password oscal-postgres-dev \
  psql -U oscal_user -d oscal_dev -c "VACUUM FULL;"
```

3. **Prune Docker**:
```bash
docker system prune -a --volumes
```

---

## Migration from H2

### For Existing Users

If you have existing data in H2 that you want to migrate to PostgreSQL:

#### Step 1: Export data from H2

**Option 1: SQL export (recommended)**

```bash
# Start application with H2 (temporarily)
mvn spring-boot:run -Dspring.profiles.active=dev \
  -Dspring.datasource.url=jdbc:h2:file:./data/oscal-history-dev

# Access H2 console at http://localhost:8080/h2-console
# Run: SCRIPT TO './h2-export.sql'
```

**Option 2: Manual JSON export (for critical data)**

```bash
# Export users via API
curl http://localhost:8080/api/users > users_backup.json

# Export other critical data
```

#### Step 2: Start with PostgreSQL

```bash
# Start PostgreSQL
docker-compose -f docker-compose-postgres.yml up -d

# Start application with PostgreSQL
mvn spring-boot:run -Dspring.profiles.active=dev
```

#### Step 3: Import data (if applicable)

For most users, **starting fresh is recommended** since development databases typically don't have critical data.

If you need to import:

```sql
-- Manual SQL insert
INSERT INTO users (username, password, email, ...)
VALUES ('user1', 'hashed_password', 'user@example.com', ...);
```

Or use the API to recreate users/data.

### Data Schema

The database schema is identical between H2 and PostgreSQL. All JPA entities work the same way. The only differences are:

- **Auto-increment**: PostgreSQL uses `SERIAL` instead of H2's `AUTO_INCREMENT`
- **Data types**: Some minor differences (handled automatically by Hibernate)
- **Case sensitivity**: PostgreSQL is case-sensitive for identifiers (Hibernate handles this)

---

## Summary

### What You Should Know

✅ **H2 is gone** - Only used for unit tests now
✅ **PostgreSQL is the default** - For both dev and prod
✅ **Docker makes it easy** - No manual PostgreSQL installation needed
✅ **All data persists** - Even after container restarts
✅ **Production-ready** - Same database in dev and prod

### Quick Reference

```bash
# Start PostgreSQL
docker-compose -f docker-compose-postgres.yml up -d

# Start application
cd back-end && mvn spring-boot:run -Dspring.profiles.active=dev

# Access database
docker exec -e PGPASSWORD=oscal_dev_password oscal-postgres-dev \
  psql -U oscal_user -d oscal_dev

# Stop everything
docker-compose -f docker-compose-postgres.yml down

# Reset database
docker-compose -f docker-compose-postgres.yml down -v
```

### Connection Strings

**Local Development (Docker):**
```
jdbc:postgresql://localhost:5432/oscal_dev
User: oscal_user
Pass: oscal_dev_password
```

**Docker Compose (app → postgres):**
```
jdbc:postgresql://postgres:5432/oscal_dev
User: oscal_user
Pass: oscal_dev_password
```

**Production:**
```
jdbc:postgresql://postgres:5432/${DB_NAME}
User: ${DB_USER}
Pass: ${DB_PASSWORD}  # From .env file
```

---

## Next Steps

1. **Try it out**: Start PostgreSQL and your application
2. **Explore pgAdmin**: Visual database management
3. **Create a backup**: Practice backup/restore procedures
4. **Read about PostgreSQL**: [Official documentation](https://www.postgresql.org/docs/15/)
5. **Consider production**: Review `docker-compose.prod.yml` for deployment

---

## Additional Resources

- [PostgreSQL Documentation](https://www.postgresql.org/docs/15/)
- [Spring Data JPA Reference](https://docs.spring.io/spring-data/jpa/docs/current/reference/html/)
- [HikariCP Configuration](https://github.com/brettwooldridge/HikariCP#configuration-knobs-baby)
- [Docker PostgreSQL Image](https://hub.docker.com/_/postgres)
- [pgAdmin Documentation](https://www.pgadmin.org/docs/)

---

**Questions or Issues?**

File an issue in the project repository or contact the development team.
