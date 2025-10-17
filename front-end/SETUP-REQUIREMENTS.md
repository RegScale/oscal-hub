# Setup Requirements for OSCAL CLI Web Interface

## Current Status

✅ **Frontend Ready**: Node.js and npm are installed
❌ **Backend Pending**: Java and Maven need to be installed

## Installed Dependencies

- **Node.js**: v22.19.0 ✅
- **npm**: 11.5.2 ✅

## Missing Dependencies (for Backend)

- **Java**: Not installed (Required: Java 11+)
- **Maven**: Not checked (requires Java first)

## Installation Instructions

### Option 1: Install with Homebrew (Recommended)

1. **Install Homebrew** (if not already installed):
   ```bash
   /bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"
   ```

2. **Install Java 11+ (Adoptium/Temurin)**:
   ```bash
   brew install openjdk@11

   # Add to PATH (add to ~/.zshrc or ~/.bash_profile)
   echo 'export PATH="/opt/homebrew/opt/openjdk@11/bin:$PATH"' >> ~/.zshrc
   source ~/.zshrc
   ```

3. **Install Maven**:
   ```bash
   brew install maven
   ```

4. **Verify installations**:
   ```bash
   java -version  # Should show 11.x.x
   mvn -version   # Should show Maven 3.x.x
   ```

### Option 2: Manual Installation

1. **Install Java**:
   - Download from [Adoptium](https://adoptium.net/)
   - Or [Oracle JDK](https://www.oracle.com/java/technologies/downloads/)
   - Install and add to PATH

2. **Install Maven**:
   - Download from [Maven website](https://maven.apache.org/download.cgi)
   - Extract and add bin directory to PATH

## What Can Be Done Now

### Without Java/Maven (Frontend Only)

You can start developing the **Next.js frontend** immediately:

```bash
cd front-end
npx create-next-app@latest ui --typescript --tailwind --app
cd ui
npx shadcn-ui@latest init
npm run dev
```

The frontend will run on http://localhost:3000 but won't be able to communicate with the backend API until Spring Boot is set up.

### With Java/Maven (Full Stack)

Once Java and Maven are installed, you can:

1. **Create Spring Boot backend**
2. **Run the full application stack**
3. **Develop and test API integration**

## Current Implementation Plan

Since Java is not installed, I'll proceed with:

1. ✅ Initialize Next.js frontend
2. ✅ Set up ShadCN UI with dark mode
3. ✅ Create frontend component structure
4. ✅ Build UI pages and components
5. ⏳ Backend implementation (waiting for Java/Maven)

The frontend can be developed in parallel and will use mock API responses until the backend is ready.

## Quick Setup Commands

Once Java and Maven are installed:

```bash
# Verify everything is ready
java -version   # Should be 11+
mvn -version    # Should be 3.8+
node --version  # Should be 18+ (we have 22.19.0 ✅)
npm --version   # Should be 8+ (we have 11.5.2 ✅)

# Then proceed with full implementation
cd front-end
# Backend setup commands will be provided
# Frontend setup in progress...
```

## Need Help?

If you encounter issues:
1. Check [IMPLEMENTATION-ROADMAP.md](docs/IMPLEMENTATION-ROADMAP.md) for detailed steps
2. See [ARCHITECTURE.md](docs/ARCHITECTURE.md) for system requirements
3. Refer to installer scripts in `/installer` for Java verification logic
