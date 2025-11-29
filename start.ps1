# OSCAL CLI Web Interface - Startup Script for Windows
# This script starts both the backend (Spring Boot) and frontend (Next.js) servers

# Color output functions
function Write-ColorOutput($ForegroundColor) {
    $fc = $host.UI.RawUI.ForegroundColor
    $host.UI.RawUI.ForegroundColor = $ForegroundColor
    if ($args) {
        Write-Output $args
    }
    $host.UI.RawUI.ForegroundColor = $fc
}

function Write-Green { Write-ColorOutput Green $args }
function Write-Yellow { Write-ColorOutput Yellow $args }
function Write-Blue { Write-ColorOutput Blue $args }
function Write-Red { Write-ColorOutput Red $args }

Write-Blue @"
   ____   _____ _____          _
  / __ \ / ____/ ____|   /\   | |
 | |  | | (___| |       /  \  | |
 | |  | |\___ \ |      / /\ \ | |
 | |__| |____) | |____|/ ____ \| |____
  \____/|_____/ \_____/_/    \_\______|

         H  U  B
"@
Write-Blue "========================================"
Write-Output ""

$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path

# Check if Java is installed
try {
    $javaVersion = java -version 2>&1 | Select-Object -First 1
    Write-Green "✓ Java is installed: $javaVersion"
} catch {
    Write-Red "Error: Java is not installed or not in PATH"
    Write-Yellow "Please install Java 11+ from https://adoptium.net/"
    exit 1
}

# Check if Maven is installed
try {
    $mavenVersion = mvn -version 2>&1 | Select-Object -First 1
    Write-Green "✓ Maven is installed: $mavenVersion"
} catch {
    Write-Red "Error: Maven is not installed or not in PATH"
    Write-Yellow "Please install Maven from https://maven.apache.org/download.cgi"
    exit 1
}

# Check if Node.js is installed
try {
    $nodeVersion = node --version
    Write-Green "✓ Node.js is installed: $nodeVersion"
} catch {
    Write-Red "Error: Node.js is not installed"
    Write-Yellow "Please install Node.js from https://nodejs.org/"
    exit 1
}

Write-Output ""

# Load environment variables from .env file if it exists
$envFile = Join-Path $ScriptDir ".env"
if (Test-Path $envFile) {
    Write-Blue "Loading environment variables from .env..."
    Get-Content $envFile | ForEach-Object {
        if ($_ -match '^\s*([^#][^=]*?)\s*=\s*(.+?)\s*$') {
            $name = $matches[1]
            $value = $matches[2]
            [Environment]::SetEnvironmentVariable($name, $value, "Process")
        }
    }

    # Log which environment variables are configured
    if ($env:SPRING_PROFILES_ACTIVE) {
        Write-Green "✓ Spring Profile: $env:SPRING_PROFILES_ACTIVE"
    } else {
        Write-Yellow "⚠ SPRING_PROFILES_ACTIVE not set, using default: dev"
    }

    if ($env:JWT_SECRET) {
        $jwtLength = $env:JWT_SECRET.Length
        Write-Green "✓ JWT_SECRET is configured (length: $jwtLength characters)"
        if ($jwtLength -lt 32) {
            Write-Red "  ⚠ WARNING: JWT secret is too short. Minimum 32 characters required."
        }
    } else {
        Write-Yellow "⚠ JWT_SECRET is not set (will use dev default)"
    }

    if ($env:AZURE_STORAGE_CONNECTION_STRING) {
        Write-Green "✓ AZURE_STORAGE_CONNECTION_STRING is set"
    } else {
        Write-Yellow "⚠ AZURE_STORAGE_CONNECTION_STRING is not set"
    }

    if ($env:AZURE_STORAGE_CONTAINER_NAME) {
        Write-Green "✓ AZURE_STORAGE_CONTAINER_NAME: $env:AZURE_STORAGE_CONTAINER_NAME"
    }

    Write-Output ""
} else {
    Write-Yellow "Note: .env file not found. Using default configuration."
    Write-Yellow "To customize: cp .env.example .env and configure your settings"
    Write-Output ""
}

# Check if Docker is running
Write-Blue "Checking Docker..."
try {
    docker info 2>&1 | Out-Null
    if ($LASTEXITCODE -eq 0) {
        Write-Green "✓ Docker is running"
    } else {
        throw "Docker not running"
    }
} catch {
    Write-Red "✗ Docker is not running!"
    Write-Yellow "Please start Docker Desktop and try again."
    exit 1
}

# Check if PostgreSQL is running
Write-Blue "Checking PostgreSQL database..."
$postgresRunning = docker ps --format "{{.Names}}" | Select-String -Pattern "^oscal-postgres-dev$"
if ($postgresRunning) {
    Write-Green "✓ PostgreSQL is already running"
} else {
    Write-Yellow "PostgreSQL not running. Starting it now..."
    docker-compose -f (Join-Path $ScriptDir "docker-compose-postgres.yml") up -d

    # Wait for PostgreSQL to be healthy
    Write-Blue "Waiting for PostgreSQL to be ready..."
    $postgresReady = $false
    for ($i = 1; $i -le 30; $i++) {
        $health = docker inspect oscal-postgres-dev --format='{{.State.Health.Status}}' 2>$null
        if ($health -eq "healthy") {
            $postgresReady = $true
            break
        }
        Write-Host "." -NoNewline
        Start-Sleep -Seconds 1
    }
    Write-Output ""

    if ($postgresReady) {
        Write-Green "✓ PostgreSQL is ready!"
    } else {
        Write-Red "✗ PostgreSQL failed to start or is not healthy"
        Write-Yellow "Check logs with: docker logs oscal-postgres-dev"
        exit 1
    }
}
Write-Output ""

# Function to kill processes using a specific port
function Kill-Port {
    param(
        [int]$Port,
        [string]$Description
    )

    Write-Yellow "🔍 Checking for processes on port $Port ($Description)..."

    # Find processes using the port
    $processes = Get-NetTCPConnection -LocalPort $Port -ErrorAction SilentlyContinue | Select-Object -ExpandProperty OwningProcess -Unique

    if ($processes) {
        Write-Red "⚠️  Found processes using port ${Port}: $($processes -join ', ')"
        Write-Yellow "🔨 Killing processes..."
        foreach ($pid in $processes) {
            try {
                Stop-Process -Id $pid -Force -ErrorAction Stop
            } catch {
                Write-Yellow "Could not kill process $pid"
            }
        }
        Start-Sleep -Seconds 1
        Write-Green "✓ Port $Port cleared"
    } else {
        Write-Green "✓ Port $Port is available"
    }
}

# Clean up ports before starting
Write-Yellow "🧹 Cleaning up ports..."
Kill-Port -Port 8080 -Description "Backend"
Kill-Port -Port 3000 -Description "Frontend"
Write-Output ""

# Build backend
Write-Green "Building backend..."
Push-Location (Join-Path $ScriptDir "back-end")
mvn clean compile > (Join-Path $ScriptDir "backend-build.log") 2>&1
if ($LASTEXITCODE -ne 0) {
    Write-Red "✗ Backend build failed. Check backend-build.log for details."
    Pop-Location
    exit 1
}
Write-Green "✓ Backend build successful"
Pop-Location

# Start backend server
Write-Green "Starting backend server..."
$backendJob = Start-Job -ScriptBlock {
    param($dir, $logFile)
    Set-Location $dir
    mvn spring-boot:run 2>&1 | Tee-Object -FilePath $logFile
} -ArgumentList (Join-Path $ScriptDir "back-end"), (Join-Path $ScriptDir "backend.log")

# Wait for backend to start
Write-Yellow "Waiting for backend to start..."
$backendReady = $false
for ($i = 1; $i -le 30; $i++) {
    try {
        $response = Invoke-WebRequest -Uri "http://localhost:8080/api/health" -TimeoutSec 1 -ErrorAction SilentlyContinue
        if ($response.StatusCode -eq 200) {
            $backendReady = $true
            Write-Green "✓ Backend is ready!"
            break
        }
    } catch {
        # Continue waiting
    }
    Start-Sleep -Seconds 2
}

if (-not $backendReady) {
    Write-Red "✗ Backend failed to start. Check backend.log for details."
    Stop-Job -Job $backendJob
    Remove-Job -Job $backendJob
    exit 1
}

# Install frontend dependencies if needed
Write-Green "Checking frontend dependencies..."
$frontendDir = Join-Path $ScriptDir "front-end"
if (-not (Test-Path (Join-Path $frontendDir "node_modules"))) {
    Write-Yellow "Installing frontend dependencies..."
    Push-Location $frontendDir
    npm ci > (Join-Path $ScriptDir "frontend-install.log") 2>&1
    if ($LASTEXITCODE -ne 0) {
        Write-Red "✗ Frontend dependency installation failed. Check frontend-install.log for details."
        Pop-Location
        Stop-Job -Job $backendJob
        Remove-Job -Job $backendJob
        exit 1
    }
    Write-Green "✓ Frontend dependencies installed"
    Pop-Location
} else {
    Write-Green "✓ Frontend dependencies already installed"
}

# Start frontend server
Write-Green "Starting frontend server..."
$frontendJob = Start-Job -ScriptBlock {
    param($dir, $logFile)
    Set-Location $dir
    npm run dev 2>&1 | Tee-Object -FilePath $logFile
} -ArgumentList $frontendDir, (Join-Path $ScriptDir "frontend.log")

# Wait for frontend to start
Write-Yellow "Waiting for frontend to start..."
Start-Sleep -Seconds 5

Write-Output ""
Write-Green "========================================"
Write-Green "  Servers are running!"
Write-Green "========================================"
Write-Output ""
Write-Blue "Frontend: http://localhost:3000"
Write-Blue "Backend:  http://localhost:8080/api"
Write-Output ""
Write-Output "Logs:"
Write-Output "  Frontend:      $(Join-Path $ScriptDir 'frontend.log')"
Write-Output "  Backend:       $(Join-Path $ScriptDir 'backend.log')"
Write-Output "  Backend Build: $(Join-Path $ScriptDir 'backend-build.log')"
Write-Output ""
Write-Output "Job IDs:"
Write-Output "  Backend:  $($backendJob.Id)"
Write-Output "  Frontend: $($frontendJob.Id)"
Write-Output ""
Write-Yellow "To view logs in real-time:"
Write-Output "  Get-Content $(Join-Path $ScriptDir 'frontend.log') -Wait"
Write-Output "  Get-Content $(Join-Path $ScriptDir 'backend.log') -Wait"
Write-Output ""
Write-Yellow "To stop servers, run: .\stop.ps1"
Write-Output ""
