# OSCAL CLI Web Interface - Quick Start Guide

**Status**: ✅ **Phase 1 Complete** - Validation feature is fully functional!

## What's Working Now

- ✅ Backend API (Spring Boot) with OSCAL validation
- ✅ Frontend UI (Next.js) with dark mode
- ✅ Full integration between frontend and backend
- ✅ OSCAL document validation for all 7 model types
- ✅ Support for JSON, XML, and YAML formats

## Prerequisites

Make sure you have these installed:

- **Java 11+** (we'll help you install this)
- **Maven 3.9+** (we'll help you install this)
- **Node.js 18+** and npm

## First Time Setup

### 1. Install Java and Maven (using SDKMAN)

```bash
# Install SDKMAN
curl -s "https://get.sdkman.io" | bash
source "$HOME/.sdkman/bin/sdkman-init.sh"

# Install Java 11 and Maven
sdk install java 11.0.25-tem
sdk install maven

# Verify installations
java -version
mvn -version
```

### 2. Add SDKMAN to Your Shell

Add this to your `~/.zshrc` or `~/.bashrc` so Java and Maven are always available:

```bash
export SDKMAN_DIR="$HOME/.sdkman"
[[ -s "$HOME/.sdkman/bin/sdkman-init.sh" ]] && source "$HOME/.sdkman/bin/sdkman-init.sh"
```

Then reload your shell:
```bash
source ~/.zshrc  # or source ~/.bashrc if using bash
```

### 3. Install Frontend Dependencies

```bash
cd ui
npm install
```

## Running the Application

### Option 1: Quick Start (Recommended)

From the `front-end` directory:

```bash
./dev.sh
```

This starts both servers in the background.

### Option 2: Manual Start (Two Terminals)

**Terminal 1 - Backend:**
```bash
cd front-end/api
source ~/.sdkman/bin/sdkman-init.sh  # Only if not in your shell config
mvn spring-boot:run
```

Wait until you see: `Started OscalCliApiApplication`

**Terminal 2 - Frontend:**
```bash
cd front-end/ui
npm run dev
```

Wait until you see: `✓ Ready`

### Option 3: Interactive Script

From the `front-end` directory:

```bash
./start.sh
```

This script will:
- Check that Java and Maven are installed
- Start both servers
- Show status messages
- Create log files (frontend.log and backend.log)
- Handle graceful shutdown when you press Ctrl+C

## Access the Application

Once both servers are running:

- **Frontend UI**: http://localhost:3000 (or 3001 if 3000 is in use)
- **Backend API**: http://localhost:8080/api
- **Health Check**: http://localhost:8080/api/health

## Using the Validation Feature

1. Open http://localhost:3000 in your browser
2. Click "Validate" in the navigation
3. Choose your validation method:
   - **Upload File**: Drag and drop an OSCAL file
   - **Paste Text**: Paste OSCAL content directly
4. Select the model type (catalog, profile, etc.)
5. Select the format (JSON, XML, or YAML)
6. Click "Validate Document"
7. View the results:
   - ✅ Green = Valid
   - ❌ Red = Invalid (with error details)

## Stopping the Application

### If using dev.sh

Find and kill the processes:
```bash
pkill -f 'spring-boot:run'
pkill -f 'next-server'
```

Or find the specific processes:
```bash
lsof -i :8080  # Backend
lsof -i :3000  # Frontend
kill <PID>
```

### If using start.sh

Just press `Ctrl+C` in the terminal

### If started manually

Press `Ctrl+C` in each terminal window

## Testing the API Directly

### Health Check
```bash
curl http://localhost:8080/api/health
```

Expected: `OSCAL CLI API is running`

### Validate a Document
```bash
curl -X POST http://localhost:8080/api/validate \
  -H "Content-Type: application/json" \
  -d '{
    "content": "{\"catalog\": {\"uuid\": \"12345678-1234-1234-1234-123456789abc\", \"metadata\": {\"title\": \"Test Catalog\", \"last-modified\": \"2025-10-15T00:00:00Z\", \"version\": \"1.0\", \"oscal-version\": \"1.0.0\"}}}",
    "modelType": "catalog",
    "format": "JSON"
  }'
```

## Configuration

### Backend Configuration

File: `api/src/main/resources/application.properties`

```properties
server.port=8080
cors.allowed-origins=http://localhost:3000
spring.servlet.multipart.max-file-size=10MB
```

### Frontend Configuration

File: `ui/.env.local` (already configured)

```env
NEXT_PUBLIC_API_URL=http://localhost:8080/api
NEXT_PUBLIC_USE_MOCK=false
```

Set `NEXT_PUBLIC_USE_MOCK=true` to use mock data instead of the real backend.

## Troubleshooting

### "Java not found" or "Maven not found"

```bash
# Source SDKMAN in your current terminal
source ~/.sdkman/bin/sdkman-init.sh

# Or add to your shell config (see step 2 above)
```

### Port 8080 already in use

Find and kill the process using port 8080:
```bash
lsof -i :8080
kill -9 <PID>
```

### Port 3000 already in use

The frontend will automatically use port 3001 if 3000 is busy. Check the console output for the actual port.

### Frontend can't connect to backend

1. Make sure backend is running:
   ```bash
   curl http://localhost:8080/api/health
   ```

2. Check that `ui/.env.local` has `NEXT_PUBLIC_USE_MOCK=false`

3. Restart the frontend to pick up environment changes

### "Dependencies not found" in backend

```bash
cd api
mvn clean install -U
```

This will download all required dependencies.

## What's Next?

### Coming Soon (Not Yet Implemented)

- 🚧 Format Conversion (XML ↔ JSON ↔ YAML)
- 🚧 Profile Resolution
- 🚧 Batch Processing
- 🚧 Export Results

### Current Limitations

- Only validation is implemented
- Other features show placeholder UI
- File upload size limited to 10MB
- Single file processing only

## Project Structure

```
front-end/
├── api/                    # Spring Boot backend
│   ├── src/
│   └── pom.xml
├── ui/                     # Next.js frontend
│   ├── src/
│   ├── .env.local         # Configuration (don't commit!)
│   └── package.json
├── start.sh               # Interactive startup script
├── dev.sh                 # Quick startup script
├── QUICKSTART.md          # This file
└── README.md              # Design documentation
```

## Getting Help

- **Backend logs**: `front-end/backend.log` (if using start.sh)
- **Frontend logs**: `front-end/frontend.log` (if using start.sh)
- **Console output**: Watch the terminal for error messages
- **Browser console**: Press F12 to see frontend errors

## Example OSCAL Documents

For testing, you can use sample OSCAL documents from:
- https://github.com/usnistgov/oscal-content

Or create a minimal catalog:

```json
{
  "catalog": {
    "uuid": "12345678-1234-1234-1234-123456789abc",
    "metadata": {
      "title": "My Test Catalog",
      "last-modified": "2025-10-15T00:00:00Z",
      "version": "1.0",
      "oscal-version": "1.0.0"
    }
  }
}
```

## Success Indicators

You'll know everything is working when:

1. Backend shows: `Started OscalCliApiApplication in X seconds`
2. Frontend shows: `✓ Ready in Xms`
3. Health check returns: `OSCAL CLI API is running`
4. Browser loads the UI without errors
5. Validation returns results (valid or invalid)

---

**Questions or Issues?**

Check the main [README.md](README.md) for detailed architecture and design documentation.
