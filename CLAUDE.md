# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

The OSCAL Tools project consists of three main components:

1. **CLI** (`cli/`) - Java command-line tool for performing operations on [OSCAL](https://pages.nist.gov/OSCAL/) (Open Security Controls Assessment Language) and Metaschema content
2. **Back-end** (`back-end/`) - Spring Boot REST API that exposes OSCAL operations via HTTP endpoints
3. **Front-end** (`front-end/`) - Next.js web application providing a user-friendly interface for OSCAL operations

All components are built on top of [Metaschema Java Tools](https://github.com/usnistgov/metaschema-java) and [OSCAL Java Library](https://github.com/usnistgov/liboscal-java/).

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
├── pom.xml             # Parent Maven POM (aggregator)
├── Dockerfile          # Multi-stage Docker build
├── docker-compose.yml  # Docker Compose configuration
├── dev.sh              # Quick development startup
├── start.sh            # Production-like startup
└── stop.sh             # Stop all servers
```

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
# From project root - starts both back-end and front-end in dev mode
./dev.sh

# Or use the production-like startup script
./start.sh

# Stop all servers
./stop.sh
```

This will start:
- **Back-end API**: http://localhost:8080/api
- **Front-end UI**: http://localhost:3000

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

- **Metaschema Framework** (v0.12.2): Provides CLI framework, binding, and validation
- **liboscal-java** (v3.0.3): OSCAL model classes and profile resolution
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

- **ClassLoader Issues**: If you encounter classloader problems, see `Issue96ClassLoaderTest.java` for context
- **YAML Null Values**: Special handling for null values in YAML - see `NullYamlValuesTest.java`
- **Format Detection**: CLI can auto-detect format from file extension; use `--as` option if detection fails
