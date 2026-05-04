# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

The OSCAL Tools project consists of three main components:

1. **CLI** (`cli/`) - Java command-line tool for performing operations on [OSCAL](https://pages.nist.gov/OSCAL/) (Open Security Controls Assessment Language) and Metaschema content
2. **Back-end** (`back-end/`) - Spring Boot REST API that exposes OSCAL operations via HTTP endpoints
3. **Front-end** (`front-end/`) - Next.js web application providing a user-friendly interface for OSCAL operations

All components are built on top of [Metaschema Java Tools](https://github.com/metaschema-framework/metaschema-java) and [OSCAL Java Library](https://github.com/metaschema-framework/liboscal-java/).

## Project Structure

```
oscal-cli/
├── cli/                 # Original OSCAL CLI command-line tool
│   ├── src/            # Java source code for CLI
│   ├── pom.xml         # Maven POM for CLI module
│   └── spotbugs-exclude.xml
├── back-end/           # Spring Boot REST API
│   ├── src/            # Java source code for API
│   └── pom.xml         # Maven POM for back-end module
├── front-end/          # Next.js web application
│   ├── src/            # TypeScript/React source code
│   ├── public/         # Static assets
│   ├── package.json    # Node.js dependencies
│   └── next.config.ts  # Next.js configuration
├── docs/               # Documentation directory
├── pom.xml             # Parent Maven POM (aggregator)
├── Dockerfile          # Multi-stage Docker build
├── docker-compose.yml  # Docker Compose configuration
├── dev.sh              # Local startup (PostgreSQL via Docker, backend, frontend)
└── stop.sh             # Stop all servers
```

## Build and Deployment Policy

**CRITICAL: DO NOT BUILD THE APPLICATION**

The user handles all builds, compilations, and deployments. Your role is to:

✅ **DO**:
- Make code changes to source files
- Fix compilation errors by editing code
- Update test files to match new signatures
- Suggest what needs to be built (e.g., "The backend needs to be rebuilt")
- Inform the user when changes require a rebuild

❌ **DO NOT**:
- Run `mvn clean install` or any Maven build commands
- Run `npm run build` or any frontend build commands
- Run `./dev.sh` or any startup scripts
- Execute any build-related Bash commands
- Attempt to compile or package the application

**Your workflow**:
1. Make necessary code changes
2. Inform the user: "Changes complete. Please rebuild the backend/frontend."
3. Let the user handle the build process

## Documentation Guidelines

**IMPORTANT**: All documentation files created during development should be placed in the `docs/` directory.

### Documentation Best Practices

1. **Location**: Always create documentation files in `docs/` directory, not in project root
2. **Naming**: Use descriptive UPPERCASE names with hyphens (e.g., `FEATURE-NAME-GUIDE.md`)
3. **Format**: Use Markdown (.md) format for all documentation
4. **Content**: Include:
   - Date and status at the top
   - Clear problem/solution sections
   - Code examples where relevant
   - Testing results
   - Step-by-step guides for complex features

### Current Documentation

The `docs/` directory contains:
- **AUTHORIZATION-FEATURE-SUMMARY.md** - Complete guide to the authorization feature
- **GCP-DEPLOYMENT-SETUP.md** - Complete guide for setting up GCP deployment with GitHub Actions
- **GITHUB-SECRETS-SETUP.md** - Quick reference for configuring GitHub secrets for GCP deployment
- **HEALTH-CHECK-API.md** - Health check API documentation for monitoring integration
- **TEMPLATE-EDITOR-FIX.md** - Technical details on template editor fixes
- **VARIABLE-DETECTION-SUMMARY.md** - User guide for variable detection
- **VARIABLE-PATTERN-UPDATE.md** - Pattern matching updates for variables
- **JAVA_SPRING_UPGRADE_PLAN.md** - Java and Spring upgrade planning

When creating new features or fixing issues, document your work in the `docs/` folder so future developers can understand the implementation and decisions made.

## Build Commands

### Building All Components

```bash
# Build all Maven modules (CLI + back-end) from root
mvn clean install

# Build without running tests
mvn clean install -DskipTests

# Build only the CLI module
cd cli && mvn clean install

# Build only the back-end module
cd back-end && mvn clean install

# Build the front-end
cd front-end && npm ci && npm run build
```

### Running Tests

```bash
# Run all Maven tests (CLI + back-end)
mvn test

# Run CLI tests only
cd cli && mvn test -Dtest=CLITest

# Run back-end tests only
cd back-end && mvn test

# Run front-end tests
cd front-end && npm test
```

## Running Locally

### Option 1: Development Mode (Recommended for Development)

```bash
# From project root - starts PostgreSQL (via Docker), back-end, and front-end
./dev.sh

# Stop all servers
./stop.sh
```

This will start:
- **Back-end API**: http://localhost:8090/api
- **Front-end UI**: http://localhost:3010

> **Why 8090 / 3010?** The dev defaults differ from production (8080 / 3000)
> so OSCAL Hub can run alongside the trust-center app, which reserves
> 80/443/4200/5433/6379/8000 and aggressively kills processes occupying
> those ports. Production still uses 8080 / 3000. Override locally with
> `SERVER_PORT=` and `FRONTEND_PORT=` env vars.

### Option 2: Run CLI Only

After building with `mvn install` in the `cli/` directory:

```bash
# Run the CLI from the build output
cli/target/appassembler/bin/oscal-cli --help

# On Windows
cli\target\appassembler\bin\oscal-cli.bat --help
```

### Option 3: Docker

```bash
# Build and run with Docker Compose
docker-compose up --build

# Run in detached mode
docker-compose up -d

# Stop containers
docker-compose down
```

## Authentication and Authorization

### Overview

The application uses **JWT (JSON Web Token)** authentication to secure all API endpoints:

- **Public endpoints** (no auth required): `/api/auth/login`, `/api/auth/register`, `/api/health`, `/api/health/ping`, Swagger UI
- **Protected endpoints** (auth required): All other `/api/*` endpoints including validation, conversion, visualization, health/detailed, etc.

### How JWT Authentication Works

1. **Login**: User provides username/password to `/api/auth/login`
2. **Token Generation**: Backend validates credentials and returns a JWT token
3. **Token Storage**: Frontend stores the token in `localStorage`
4. **Authenticated Requests**: Frontend includes token in `Authorization: Bearer <token>` header
5. **Token Validation**: Backend validates token on each request using `JwtAuthenticationFilter`

### Common Authentication Issues

#### 403 Forbidden Errors

**Symptom**: API requests fail with `403 Forbidden` error

**Common Causes**:
1. **No JWT token** - User not logged in
2. **Expired token** - Token has expired (default: 24 hours)
3. **Invalid token** - Token was generated by a different server instance
4. **Backend restart** - Server restart invalidates all existing tokens

**Solutions**:
1. **Refresh the browser** - Clear cache and reload (Cmd/Ctrl + Shift + R)
2. **Log out and log back in** - Get a fresh JWT token
3. **Check localStorage** - Verify token exists: `localStorage.getItem('token')`
4. **Verify backend is running** - Check `http://localhost:8090/api/health`

#### After Adding New Features

**IMPORTANT**: When you add a new backend endpoint and restart the server:

1. **All existing JWT tokens are invalidated** - The server generates a new JWT secret on startup
2. **Users must log in again** - Even if they were logged in before
3. **Browser cache may have old frontend code** - Hard refresh required

**Standard workflow after backend changes**:

```bash
# 1. Stop all servers
./stop.sh

# 2. Restart servers with latest code
./dev.sh

# 3. In browser:
#    - Hard refresh (Cmd/Ctrl + Shift + R)
#    - Navigate to http://localhost:3010
#    - Log in with your credentials
#    - Try the new feature
```

### Testing Authentication

#### Verify Backend is Running

```bash
# Check if backend is responding
curl http://localhost:8090/api/health

# Test an authenticated endpoint (should return 403)
curl -I http://localhost:8090/api/visualization/ssp

# Test with authentication (replace TOKEN with actual JWT)
curl -H "Authorization: Bearer TOKEN" \
     -H "Content-Type: application/json" \
     -d '{"content":"...","format":"JSON"}' \
     http://localhost:8090/api/visualization/ssp
```

#### Check Frontend Authentication State

Open browser console and run:

```javascript
// Check if token exists
console.log('Token:', localStorage.getItem('token'));

// Check if user is stored
console.log('User:', localStorage.getItem('user'));

// Check API client auth headers
const token = localStorage.getItem('token');
console.log('Auth header:', token ? `Bearer ${token}` : 'No token');
```

### Security Configuration

**Location**: `back-end/src/main/java/gov/nist/oscal/tools/api/config/SecurityConfig.java`

**Key settings**:
- **JWT Secret**: Generated on server startup (stored in application properties)
- **Token Expiration**: 24 hours (configurable in `application.properties`)
- **CORS** (dev): Allows `http://localhost:3010`, `http://localhost:3001`, `http://localhost:3000`
- **Session Management**: Stateless (no server-side sessions)

### Health Check Endpoints

The application provides health check endpoints for monitoring and load balancers:

| Endpoint | Auth Required | Purpose |
|----------|---------------|---------|
| `GET /api/health` | No | Simple health status (JSON) |
| `GET /api/health/ping` | No | Simple ping for monitoring tools (returns "OK" or 503) |
| `GET /api/health/detailed` | SUPER_ADMIN | Comprehensive health with components, system info |
| `GET /api/health/component/{name}` | SUPER_ADMIN | Individual component health check |

**Components Monitored**: Database, Storage, Memory, Disk Space, OSCAL Library

**Admin Dashboard**: Access the health dashboard at `/admin/health` (SUPER_ADMIN only)

**External Monitoring**: Use `/api/health/ping` for UptimeRobot, Kubernetes probes, etc.

For full documentation, see `docs/HEALTH-CHECK-API.md`.

### Debugging Authentication Issues

If you're getting 403 errors on a valid endpoint:

1. **Check the backend logs**:
   ```bash
   # Look for authentication failures
   tail -f back-end/logs/spring.log | grep -i "auth\|403\|forbidden"
   ```

2. **Verify endpoint exists**:
   ```bash
   # Should return 403, not 404
   curl -I http://localhost:8090/api/your-new-endpoint
   ```

3. **Test with Swagger UI**:
   - Navigate to `http://localhost:8090/swagger-ui.html`
   - Click "Authorize" button
   - Enter token in format: `Bearer YOUR_TOKEN_HERE`
   - Try the endpoint

4. **Check JWT token validity**:
   - Decode token at https://jwt.io
   - Verify `exp` (expiration) claim is in the future
   - Verify `sub` (subject) matches your username

### Database Lock Issues

**Symptom**: Backend fails to start with error:
```
Database may be already in use: ".../oscal-history.mv.db"
```

**Solution**:
```bash
# Kill all Java processes running Spring Boot
pkill -f 'spring-boot:run'

# Wait a moment for locks to release
sleep 2

# Restart servers
./dev.sh
```

## Database Schema Management

**Policy: Flyway is the schema authority. Hibernate validates only — it never modifies schema.**

This was changed from the previous `ddl-auto=update` model in commit `80dd0e5` after a silent-drift incident: Hibernate failed to add a `NOT NULL` column to a populated table (no `DEFAULT` declared), kept running anyway, and a later query against the missing column crashed the request and surfaced as a misleading 401. Under the new policy that class of bug becomes a fail-fast boot error.

### What this means for you

**When you add or change a JPA entity field**, you MUST also add a corresponding migration file. Editing the entity alone will cause Hibernate to refuse to start (`SchemaManagementException: Schema-validation: missing column ...`).

Workflow:
1. Edit the `@Entity` class.
2. Create a new migration file under `back-end/src/main/resources/db/migration/` named `V<n>__<short_description>.sql` where `<n>` is the next available version number (look at `ls db/migration/` for the highest existing one — for new work, increment from there).
3. Inside the SQL, write idempotent DDL — use `ADD COLUMN IF NOT EXISTS`, `CREATE TABLE IF NOT EXISTS`, `DROP CONSTRAINT IF EXISTS` — so the migration is safe to re-run.
4. For `NOT NULL` columns added to existing tables, always include `DEFAULT` so PostgreSQL can backfill existing rows. Mirror this on the entity with `@org.hibernate.annotations.ColumnDefault("...")` so the schema validator sees a match.
5. Restart the backend (`./stop.sh && ./dev.sh`). Flyway runs the migration, then Hibernate validates the resulting schema against the entity model.

### Example migration

```sql
-- V1.24__add_widget_color.sql
ALTER TABLE widgets
    ADD COLUMN IF NOT EXISTS color VARCHAR(32) NOT NULL DEFAULT 'gray';
```

### Configuration locations

- `application.properties` — base defaults (`spring.flyway.enabled=true`, `ddl-auto=validate`, `baseline-version=1.23`).
- `application-dev.properties`, `application-prod.properties`, `application-gcp.properties` — all set `ddl-auto=validate`.
- Migration files — `back-end/src/main/resources/db/migration/V*.sql`.

### Override knobs (escape hatches)

```bash
# Bootstrap a brand-new empty DB (no schema yet, no V1.0 baseline file):
DB_DDL_AUTO=update ./dev.sh   # one-time only, then unset

# Skip Flyway during local debugging:
SPRING_FLYWAY_ENABLED=false ./dev.sh
```

These should rarely be needed. Existing dev/prod databases have full schema and validate cleanly out of the box.

### Manual schema fixup (one-off, when you've already broken things)

If validate fails because a previous Hibernate `ddl-auto=update` left the schema in a half-state, fix the DB directly with `psql` and write the corresponding migration in the same commit:

```bash
docker exec oscal-postgres-dev psql -U oscal_user -d oscal_dev -c \
  "ALTER TABLE my_table ADD COLUMN IF NOT EXISTS my_col TEXT;"
```

Then add `V<n>__<desc>.sql` so the next person bringing up a fresh DB gets the same change automatically.

## Installation Scripts for End Users

The repository includes simplified installation scripts for end users in the `installer/` directory:

- **installer/install.sh** - Mac/Linux installation script
  - Auto-detects and verifies Java 11+
  - Downloads latest release from Maven Central
  - Installs to `~/.oscal-cli` (customizable via `OSCAL_CLI_HOME`)
  - Supports version selection via `OSCAL_CLI_VERSION` environment variable
  - Provides PATH configuration instructions

- **installer/install.ps1** - Windows PowerShell installation script
  - Auto-detects and verifies Java 11+
  - Downloads latest release from Maven Central
  - Installs to `%USERPROFILE%\.oscal-cli` (customizable via `-InstallDir` parameter)
  - Supports version selection via `-Version` parameter
  - Optionally adds to user PATH automatically

- **installer/README.md** - Complete installation guide
  - Quick installation instructions for Mac, Linux, and Windows
  - Manual installation steps
  - Prerequisites and requirements
  - Troubleshooting installation issues
  - GPG signature verification instructions

- **USER_GUIDE.md** - Comprehensive usage documentation (post-installation)
  - Command reference and common operations
  - Examples for each OSCAL model type (catalog, profile, ssp, etc.)
  - Advanced usage (batch processing, CI/CD integration)
  - Usage troubleshooting and best practices

## CI/CD and Deployment

GitHub Actions on GCP. Two workflows drive the pipeline:

| Workflow | Trigger | Purpose |
|---|---|---|
| `.github/workflows/ci.yml` | PR + push to `main` | Backend + frontend tests, Trivy/Snyk security scans |
| `.github/workflows/gcp-deploy.yml` | PR + push to `main` | PR: `terraform plan` (commented on PR). Push: build image → `terraform apply` → health check |

Branch protection on `main` requires PR review + green CI before merge, so a
push to `main` only happens after approval. That approval is the deploy gate.

### Architecture

- **Single combined Cloud Run service** `oscal-tools-prod` running both Spring
  Boot backend and Next.js frontend out of the top-level `Dockerfile`.
- **Infra as code** in `terraform/gcp/` — Cloud Run service, Cloud SQL
  PostgreSQL, Cloud Storage buckets. State lives in a GCS bucket.
- **Auth** via Workload Identity Federation — no long-lived service-account
  keys in GitHub secrets.

### GitHub variables (set during bootstrap)

| Name | Example |
|---|---|
| `GCP_PROJECT_ID` | `oscal-hub` |
| `GCP_REGION` | `us-central1` |
| `GCP_WIF_PROVIDER` | `projects/123/locations/global/workloadIdentityPools/github/providers/github-provider` |
| `GCP_DEPLOY_SA` | `gh-deploy@oscal-hub.iam.gserviceaccount.com` |
| `TF_STATE_BUCKET` | `oscal-hub-tfstate` |

No GitHub secrets required for deploy. (The old `GCP_SA_KEY` secret is
unused and can be deleted.)

### One-time setup

See **`docs/CICD-BOOTSTRAP.md`** for the bootstrap procedure: state bucket,
WIF pool/provider, deploy service account + IAM, GitHub variables, branch
protection, state migration, and troubleshooting.

### Manual deployment (escape hatch)

`deploy-gcp.sh` still works for emergency manual deploys (image swap only,
preserves env vars). Prefer the GitHub Actions flow for any change you want
tracked in git history.

```bash
./deploy-gcp.sh --project-id oscal-hub --region us-central1 --environment prod
```

### Monitoring deployments

```bash
# Service URL
gcloud run services describe oscal-tools-prod --region us-central1 --format='value(status.url)'

# Recent logs
gcloud logging read 'resource.type=cloud_run_revision AND resource.labels.service_name=oscal-tools-prod' --limit 50

# Deployed image
gcloud run services describe oscal-tools-prod --region us-central1 --format='value(spec.template.spec.containers[0].image)'
```

## Code Architecture

### CLI Module (`cli/`)

#### Command Structure

The CLI uses a hierarchical command pattern built on the Metaschema CLI framework:

- **Main Entry Point**: `cli/src/main/java/.../CLI.java` (line 52) - registers all top-level command handlers
- **Command Hierarchy**: Each OSCAL model type has a dedicated command class:
  - `CatalogCommand` - operations on catalogs
  - `ProfileCommand` - operations on profiles (including resolve)
  - `ComponentDefinitionCommand` - operations on component definitions
  - `SystemSecurityPlanCommand` - operations on SSPs
  - `AssessmentPlanCommand` - operations on assessment plans
  - `AssessmentResultsCommand` - operations on assessment results
  - `PlanOfActionsAndMilestonesCommand` - operations on POA&Ms
  - `MetaschemaCommand` - operations on Metaschema definitions

### Back-end Module (`back-end/`)

The Spring Boot REST API provides HTTP endpoints for OSCAL operations:

- **Main Entry Point**: `back-end/src/main/java/.../OscalCliApiApplication.java`
- **Controllers** (`controller/`):
  - `ValidationController` - Endpoints for validating OSCAL documents
  - `ConversionController` - Endpoints for format conversion
  - `ProfileController` - Endpoints for profile resolution
  - `HistoryController` - Operation history tracking
- **Services** (`service/`):
  - Business logic for OSCAL operations
  - Wraps liboscal-java functionality
- **Models** (`model/`):
  - DTOs for API requests/responses
  - `ValidationRequest`, `ValidationResult`, `OscalFormat`, etc.
- **Configuration** (`config/`):
  - OpenAPI/Swagger configuration
  - CORS configuration
  - Database configuration (H2)

### Front-end Module (`front-end/`)

The Next.js web application provides a user interface for OSCAL operations:

- **Main Entry Point**: `front-end/src/app/`
- **App Structure**:
  - `app/` - Next.js 13+ App Router pages
  - `components/` - Reusable React components
  - `lib/` - Utilities and API client
  - `hooks/` - Custom React hooks
  - `types/` - TypeScript type definitions
- **Key Features**:
  - File upload with drag-and-drop
  - Real-time validation feedback
  - Format conversion UI
  - Profile resolution interface
  - Operation history

### Command Pattern

Commands follow a consistent pattern:

1. **Parent Command** (e.g., `ProfileCommand`) extends `AbstractParentCommand`
   - Registers subcommands in the constructor
   - Defines the command name and description

2. **Subcommands** (e.g., `ValidateSubcommand`, `ConvertSubcommand`) extend abstract base classes:
   - `AbstractOscalValidationSubcommand` for validation operations
   - `AbstractOscalConvertSubcommand` for conversion operations
   - `AbstractTerminalCommand` for custom operations (e.g., `ResolveSubcommand`)

3. **Command Execution**:
   - Each subcommand implements `newExecutor()` to create a command executor
   - The executor's `execute()` method contains the actual operation logic
   - Returns an `ExitStatus` with appropriate `ExitCode`

### Base Classes for OSCAL Operations

- **AbstractOscalConvertSubcommand** (line 39): Base class for format conversion operations
  - Subclasses must implement `getOscalClass()` to specify the OSCAL model class
  - Uses `OscalBindingContext` for serialization/deserialization

- **AbstractOscalValidationSubcommand**: Base class for validation operations
  - Validates OSCAL content against schemas and constraints

### Profile Resolution

Profile resolution is a special operation in `profile/ResolveSubcommand.java`:
- Loads a Profile document
- Uses `ProfileResolver` with a `DynamicContext` for URI resolution
- Resolves profile imports and merges to produce a resolved Catalog
- Supports input format detection and output format conversion

### Binding Context

All OSCAL operations use `OscalBindingContext.instance()` for:
- Loading OSCAL documents from XML/JSON/YAML
- Serializing OSCAL objects to different formats
- Accessing the Metaschema binding layer

### Key Dependencies

- **liboscal-java** (v6.0.0): OSCAL model classes, profile resolution, and metaschema binding (groupId: `dev.metaschema.oscal`)
- **Spring Boot** (v3.5.9): Web framework and dependency management
- **Apache Commons CLI**: Command-line option parsing
- **Log4j2**: Logging framework
- **JUnit 5**: Testing framework

## Testing

### CLI Tests

Tests are located in `cli/src/test/java/gov/nist/secauto/oscal/tools/cli/core/`:

- **CLITest.java**: Parameterized tests for all commands and format conversions
  - Tests validate, convert, and resolve operations
  - Uses test resources in `cli/src/test/resources/cli/`
  - Validates exit codes and exception handling

Test resources follow naming conventions:
- `example_{model}_valid.{xml|json|yaml}` - valid OSCAL documents
- `example_{model}_invalid.{xml|json|yaml}` - invalid OSCAL documents for negative tests

### Back-end Tests

Tests are located in `back-end/src/test/java/`:

- Spring Boot integration tests for API endpoints
- Service layer unit tests
- Test resources in `back-end/src/test/resources/`

### Front-end Tests

Tests are located in `front-end/`:

- `__tests__/` - Component unit tests
- `e2e/` - Playwright end-to-end tests
- Run with `npm test` for unit tests
- Run with `npm run test:e2e` for E2E tests

## Maven Configuration

Key Maven plugins:
- **appassembler-maven-plugin**: Creates shell/batch scripts for running the CLI
- **maven-assembly-plugin**: Packages distribution archives
- **git-commit-id-maven-plugin**: Embeds git information in builds
- **license-maven-plugin**: Generates third-party license notices

## Java Requirements

- Java 11 or higher (source/target compatibility set to Java 11)
- Uses `@NonNull` annotations from SpotBugs for null safety

## Adding a New Command

To add a new OSCAL model type command:

1. Create a command package under `commands/` (e.g., `commands/newmodel/`)
2. Create a parent command class extending `AbstractParentCommand`
3. Create subcommand classes:
   - `ValidateSubcommand` extending `AbstractOscalValidationSubcommand`
   - `ConvertSubcommand` extending `AbstractOscalConvertSubcommand`
4. Implement `getOscalClass()` to return the OSCAL model class
5. Register the parent command in `CLI.java` (line 77)
6. Add `@AutoService(ICommand.class)` annotation to the parent command
7. Create test resources and add test cases to `CLITest.java`

## Contributing Notes

- This is a NIST public domain project
- Development uses GitHub Issues with User Story, Defect Report, and Question templates
- PRs should target the `develop` branch or specific release branches, not `main`
- All contributions are released into the public domain (CC0)
- Follow the project's Agile approach focusing on core capabilities

## Common Issues

- **403 Forbidden Errors**: See the [Authentication and Authorization](#authentication-and-authorization) section above for troubleshooting JWT authentication issues
- **Database Lock Issues**: If backend won't start due to "database already in use", see the [Database Lock Issues](#database-lock-issues) section
- **Stale Frontend After Backend Changes**: Always hard refresh (Cmd/Ctrl + Shift + R) and re-login after restarting the backend
- **ClassLoader Issues**: If you encounter classloader problems, see `Issue96ClassLoaderTest.java` for context
- **YAML Null Values**: Special handling for null values in YAML - see `NullYamlValuesTest.java`
- **Format Detection**: CLI can auto-detect format from file extension; use `--as` option if detection fails
