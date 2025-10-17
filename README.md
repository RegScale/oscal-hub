# OSCAL Hub

A comprehensive web-based platform for working with [OSCAL](https://pages.nist.gov/OSCAL/) (Open Security Controls Assessment Language) documents, providing a modern user interface, REST API, and command-line tools for validation, conversion, and profile resolution.

## Overview

OSCAL Hub consists of three integrated components:

- **Web Interface** - Modern Next.js application with user authentication and file management
- **REST API** - Spring Boot backend exposing OSCAL operations via HTTP endpoints
- **CLI Tool** - Java command-line interface for batch processing and automation

Built on top of [Metaschema Java Tools](https://github.com/usnistgov/metaschema-java) and [OSCAL Java Library](https://github.com/usnistgov/liboscal-java/), OSCAL Hub makes it easier to work with OSCAL content across all seven model types.

![OSCAL Hub Dashboard](oscal-hub.png)

## Features

### Core OSCAL Operations

- **Validation** - Validate OSCAL documents against schemas and constraints
- **Format Conversion** - Convert between XML, JSON, and YAML formats
- **Profile Resolution** - Resolve OSCAL Profiles into Catalogs
- **Batch Processing** - Process multiple files simultaneously

### Supported OSCAL Models

- Catalog
- Profile
- Component Definition
- System Security Plan (SSP)
- Assessment Plan (AP)
- Assessment Results (AR)
- Plan of Action and Milestones (POA&M)

### Web Interface Features

- **User Authentication** - Secure login with JWT tokens and password complexity requirements
- **File Management** - Upload, store, and manage OSCAL documents per user
- **Interactive Validation** - Real-time validation feedback with detailed error messages
- **Operation History** - Track and review past operations
- **Drag-and-Drop Upload** - Easy file upload with format auto-detection

## Quick Start

### Prerequisites

- **Java 11+** - Required for backend and CLI
- **Maven 3.8.4+** - For building Java components
- **Node.js 18+** - For frontend development

### Installation

#### Option 1: Automated Setup (Recommended)

Run the development startup script:

```bash
./dev.sh
```

This will:
- Build the backend
- Start the Spring Boot API server
- Install frontend dependencies
- Start the Next.js development server

#### Option 2: Production-like Setup

For a production-like environment with build verification:

```bash
./start.sh
```

This provides:
- Clean build with error checking
- Separate log files for troubleshooting
- Automatic dependency installation

### Access the Application

Once started, access:

- **Web Interface**: http://localhost:3000
- **API Documentation**: http://localhost:8080/swagger-ui.html
- **Backend API**: http://localhost:8080/api
- **Health Check**: http://localhost:8080/api/health

### Stopping the Servers

```bash
./stop.sh
```

## Architecture

```
oscal-hub/
├── front-end/          # Next.js web application
│   ├── src/
│   │   ├── app/        # App Router pages
│   │   ├── components/ # React components
│   │   ├── lib/        # API client and utilities
│   │   └── types/      # TypeScript definitions
│   └── package.json
│
├── back-end/           # Spring Boot REST API
│   ├── src/main/java/
│   │   ├── controller/ # API endpoints
│   │   ├── service/    # Business logic
│   │   ├── model/      # DTOs and entities
│   │   ├── security/   # JWT authentication
│   │   └── config/     # Spring configuration
│   └── pom.xml
│
└── cli/                # Command-line tool
    ├── src/main/java/  # CLI implementation
    └── README.md       # CLI-specific documentation
```

## Building from Source

### Build All Components

```bash
# From project root
mvn clean install
```

### Build Individual Components

```bash
# Backend only
cd back-end && mvn clean install

# Frontend only
cd front-end && npm ci && npm run build

# CLI only
cd cli && mvn clean install
```

## Running Tests

```bash
# All Java tests
mvn test

# Backend tests only
cd back-end && mvn test

# Frontend tests
cd front-end && npm test

# CLI tests
cd cli && mvn test
```

## Docker Deployment

Build and run with Docker Compose:

```bash
docker-compose up --build
```

This starts both backend and frontend services in containers.

## Azure Deployment

OSCAL Hub is designed for deployment on Azure with integrated Azure Blob Storage for persistent file storage.

### Prerequisites

1. **Azure Storage Account** - For storing OSCAL files
2. **Azure App Service** or **Azure Container Instances** - For hosting the application
3. **Azure Database for PostgreSQL** (optional) - For production database (default uses H2)

### Setting Up Azure Blob Storage

1. **Create a Storage Account**:
   - Navigate to Azure Portal → Storage Accounts → Create
   - Choose a unique storage account name
   - Select appropriate region and performance tier
   - Create the storage account

2. **Create a Blob Container**:
   - In your Storage Account, go to "Containers"
   - Click "+ Container"
   - Name it `oscal-files` (or your preferred name)
   - Set access level to "Private"

3. **Get Connection String**:
   - In Storage Account, go to "Access keys" under Security + networking
   - Copy the "Connection string" from key1 or key2
   - Format: `DefaultEndpointsProtocol=https;AccountName=ACCOUNT_NAME;AccountKey=ACCOUNT_KEY;EndpointSuffix=core.windows.net`

### Required Environment Variables

Configure these environment variables in your Azure App Service Configuration:

```bash
# Azure Blob Storage (REQUIRED)
AZURE_STORAGE_CONNECTION_STRING=DefaultEndpointsProtocol=https;AccountName=YOUR_ACCOUNT;AccountKey=YOUR_KEY;EndpointSuffix=core.windows.net
AZURE_STORAGE_CONTAINER_NAME=oscal-files

# JWT Configuration (REQUIRED - change in production!)
JWT_SECRET=your-secure-secret-key-minimum-256-bits

# Database (optional - defaults to H2 file-based)
SPRING_DATASOURCE_URL=jdbc:postgresql://your-db.postgres.database.azure.com:5432/oscaldb
SPRING_DATASOURCE_USERNAME=your-db-user
SPRING_DATASOURCE_PASSWORD=your-db-password

# Server Configuration (optional)
SERVER_PORT=8080
CORS_ALLOWED_ORIGINS=https://your-frontend-url.azurewebsites.net
```

### Deploying to Azure App Service

#### Option 1: Deploy via Azure CLI

```bash
# Login to Azure
az login

# Create resource group
az group create --name oscal-hub-rg --location eastus

# Create App Service plan
az appservice plan create --name oscal-hub-plan --resource-group oscal-hub-rg --sku B1 --is-linux

# Create backend web app (Java 11)
az webapp create --resource-group oscal-hub-rg --plan oscal-hub-plan --name oscal-hub-backend --runtime "JAVA:11-java11"

# Configure environment variables
az webapp config appsettings set --resource-group oscal-hub-rg --name oscal-hub-backend --settings \
  AZURE_STORAGE_CONNECTION_STRING="your-connection-string" \
  AZURE_STORAGE_CONTAINER_NAME="oscal-files" \
  JWT_SECRET="your-secure-secret-key"

# Deploy backend JAR
cd back-end
mvn clean package -DskipTests
az webapp deploy --resource-group oscal-hub-rg --name oscal-hub-backend --src-path target/oscal-cli-api-1.0.0-SNAPSHOT.jar --type jar

# Create frontend web app (Node 18)
az webapp create --resource-group oscal-hub-rg --plan oscal-hub-plan --name oscal-hub-frontend --runtime "NODE:18-lts"

# Configure frontend environment variables
az webapp config appsettings set --resource-group oscal-hub-rg --name oscal-hub-frontend --settings \
  NEXT_PUBLIC_API_URL="https://oscal-hub-backend.azurewebsites.net/api"

# Deploy frontend
cd ../front-end
npm ci
npm run build
az webapp deploy --resource-group oscal-hub-rg --name oscal-hub-frontend --src-path .next --type zip
```

#### Option 2: Deploy via GitHub Actions

Create `.github/workflows/azure-deploy.yml`:

```yaml
name: Deploy to Azure

on:
  push:
    branches: [ main ]

jobs:
  deploy-backend:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - name: Set up Java 11
        uses: actions/setup-java@v3
        with:
          java-version: '11'
          distribution: 'temurin'
      - name: Build with Maven
        run: cd back-end && mvn clean package -DskipTests
      - name: Deploy to Azure Web App
        uses: azure/webapps-deploy@v2
        with:
          app-name: 'oscal-hub-backend'
          publish-profile: ${{ secrets.AZURE_WEBAPP_PUBLISH_PROFILE }}
          package: back-end/target/*.jar

  deploy-frontend:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - name: Set up Node.js
        uses: actions/setup-node@v3
        with:
          node-version: '18'
      - name: Install and build
        run: cd front-end && npm ci && npm run build
      - name: Deploy to Azure Web App
        uses: azure/webapps-deploy@v2
        with:
          app-name: 'oscal-hub-frontend'
          publish-profile: ${{ secrets.AZURE_FRONTEND_PUBLISH_PROFILE }}
          package: front-end
```

### Local Development with Azure Storage

For local development with Azure Blob Storage:

1. Copy the environment template file:
   ```bash
   cp .env.example .env
   ```

2. Edit `.env` and add your Azure connection string from Azure Portal:
   ```bash
   export AZURE_STORAGE_CONNECTION_STRING="DefaultEndpointsProtocol=https;AccountName=YOUR_ACCOUNT;AccountKey=YOUR_KEY;EndpointSuffix=core.windows.net"
   export AZURE_STORAGE_CONTAINER_NAME="oscal-files"
   ```

3. Start the application:
   ```bash
   ./start.sh  # or ./dev.sh
   ```

The `.env` file is gitignored and won't be committed. The startup scripts automatically load environment variables from this file before starting the backend.

### Monitoring and Logs

- **Application Logs**: Available in Azure Portal → App Service → Log stream
- **Storage Metrics**: Available in Azure Portal → Storage Account → Monitoring
- **Health Check**: Access `/api/health` endpoint to verify backend status

## API Documentation

Interactive API documentation is available via Swagger UI when the backend is running:

- Swagger UI: http://localhost:8080/swagger-ui.html
- OpenAPI JSON: http://localhost:8080/v3/api-docs

### Key API Endpoints

- `POST /api/auth/login` - User authentication
- `POST /api/auth/register` - User registration
- `POST /api/validate` - Validate OSCAL document
- `POST /api/convert` - Convert between formats
- `POST /api/profile/resolve` - Resolve OSCAL Profile
- `GET /api/files` - List saved files
- `GET /api/history` - Operation history

## CLI Usage

The CLI tool can be used independently for automation and batch processing.

See [cli/README.md](cli/README.md) for detailed CLI documentation.

Quick example:

```bash
# After building with 'mvn install'
cli/target/appassembler/bin/oscal-cli catalog validate --file=catalog.json
```

## Configuration

### Backend Configuration

#### Azure Blob Storage (Required for Production)

For production deployments, configure Azure Blob Storage for persistent file storage.

**For Local Development: Use .env File**

Create a `.env` file in the project root:

```bash
# Copy the template
cp .env.example .env

# Edit .env and add your Azure credentials from Azure Portal
export AZURE_STORAGE_CONNECTION_STRING="DefaultEndpointsProtocol=https;AccountName=YOUR_ACCOUNT;AccountKey=YOUR_KEY;EndpointSuffix=core.windows.net"
export AZURE_STORAGE_CONTAINER_NAME="oscal-files"
```

The `.env` file is gitignored and won't be committed. The startup scripts (`./start.sh` and `./dev.sh`) automatically load variables from this file.

**For Production/Azure Deployments: Use Environment Variables**

Set these environment variables in your Azure App Service Configuration:

```bash
AZURE_STORAGE_CONNECTION_STRING=DefaultEndpointsProtocol=https;AccountName=YOUR_ACCOUNT;AccountKey=YOUR_KEY;EndpointSuffix=core.windows.net
AZURE_STORAGE_CONTAINER_NAME=oscal-files
```

#### Other Backend Settings

Edit `back-end/src/main/resources/application.properties`:

```properties
# Server port
server.port=8080

# CORS Configuration
cors.allowed-origins=http://localhost:3000
cors.allowed-methods=GET,POST,PUT,DELETE,OPTIONS

# Database (H2 file-based by default)
spring.datasource.url=jdbc:h2:file:./data/oscal-history

# JWT Configuration (CHANGE IN PRODUCTION!)
jwt.secret=oscal-hub-secret-key-change-this-in-production-minimum-256-bits-required-for-security
jwt.expiration=3600000

# File Upload Limits
spring.servlet.multipart.max-file-size=10MB
spring.servlet.multipart.max-request-size=10MB
```

### Frontend Configuration

Create `front-end/.env.local`:

```bash
NEXT_PUBLIC_API_URL=http://localhost:8080/api
```

For production, set the API URL to your deployed backend:
```bash
NEXT_PUBLIC_API_URL=https://your-backend.azurewebsites.net/api
```

## Security Features

- **JWT Authentication** - Token-based user authentication
- **Password Requirements** - 8+ characters, uppercase, lowercase, number, special character
- **User Isolation** - Files are stored per-user with access controls
- **CORS Protection** - Configured for allowed origins
- **Password Hashing** - BCrypt encryption for stored passwords

## Development

### Frontend Development

```bash
cd front-end
npm run dev        # Development server with hot-reload
npm run build      # Production build
npm run lint       # Run ESLint
```

### Backend Development

```bash
cd back-end
mvn spring-boot:run           # Run with Spring Boot Maven plugin
mvn clean compile             # Compile only
mvn test                      # Run tests
```

## Troubleshooting

### Backend won't start

Check logs:
```bash
tail -f backend.log
```

Common issues:
- Port 8080 already in use: Change in `application.properties`
- Java version: Requires Java 11+
- Maven not found: Install via SDKMAN or system package manager

### Frontend errors

Check logs:
```bash
tail -f frontend.log
```

Common issues:
- Port 3000 in use: Next.js will prompt for alternative port
- Node version: Requires Node 18+
- Missing dependencies: Run `npm ci` in front-end directory

### Authentication issues

- Clear browser localStorage and re-login
- Check JWT token hasn't expired
- Verify backend is running and accessible

### Azure Storage issues

**Connection errors on startup:**
```
Failed to initialize Azure Blob Storage: The specified connection string is invalid
```
- Verify `AZURE_STORAGE_CONNECTION_STRING` is set correctly
- Check the connection string format matches Azure Portal output
- Ensure no extra quotes or spaces in the environment variable

**Container not found errors:**
```
The specified container does not exist
```
- Verify the container name matches in both Azure Portal and configuration
- Check that `AZURE_STORAGE_CONTAINER_NAME` is set (defaults to "oscal-files")
- Ensure your Azure credentials have permission to create containers

**File operations failing:**
- Check Azure Storage Account firewall rules allow your IP/service
- Verify the Storage Account access keys haven't been rotated
- Check Azure Storage Account is in the same region for optimal performance
- Review Azure Portal → Storage Account → Monitoring for detailed error logs

**Local development without Azure:**
- Set an empty connection string to skip Azure initialization: `AZURE_STORAGE_CONNECTION_STRING=""`
- Note: File operations will fail without Azure Storage configured

## Contributing

This project welcomes contributions! Please see [CONTRIBUTING.md](CONTRIBUTING.md) for guidelines.

## License

This project builds upon the NIST OSCAL CLI tool. See [LICENSE.md](LICENSE.md) for details.

## Related Projects

- [OSCAL](https://pages.nist.gov/OSCAL/) - Official OSCAL documentation
- [liboscal-java](https://github.com/usnistgov/liboscal-java/) - OSCAL Java library
- [Metaschema Java Tools](https://github.com/usnistgov/metaschema-java) - Metaschema framework

## Support

- **Issues**: https://github.com/RegScale/oscal-hub/issues
- **Discussions**: https://github.com/RegScale/oscal-hub/discussions

## Acknowledgments

Based on the [OSCAL CLI](https://github.com/usnistgov/oscal-cli) project by NIST.
