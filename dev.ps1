# Quick development startup script for Windows
# This script assumes you've already run the setup once

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
Write-Output ""

$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path

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

Write-Blue "Starting OSCAL HUB..."
Write-Output ""

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
Write-Output "Backend will be available at: http://localhost:8080/api"
Write-Output "Frontend will be available at: http://localhost:3000"
Write-Output "pgAdmin will be available at: http://localhost:5050 (if started)"
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

Write-Green "Building backend..."
Push-Location (Join-Path $ScriptDir "back-end")
$buildOutput = mvn clean compile 2>&1
if ($LASTEXITCODE -ne 0) {
    Write-Red "✗ Backend build failed. Exiting."
    Pop-Location
    exit 1
}
Pop-Location

Write-Output ""
Write-Green "Starting backend..."
$backendJob = Start-Job -ScriptBlock {
    param($dir)
    Set-Location $dir
    mvn spring-boot:run
} -ArgumentList (Join-Path $ScriptDir "back-end")

Write-Green "Starting frontend..."
$frontendJob = Start-Job -ScriptBlock {
    param($dir)
    Set-Location $dir
    npm run dev
} -ArgumentList (Join-Path $ScriptDir "front-end")

Write-Output ""
Write-Output "Servers starting in background..."
Write-Output "Backend Job ID: $($backendJob.Id)"
Write-Output "Frontend Job ID: $($frontendJob.Id)"
Write-Output ""
Write-Output "To view logs:"
Write-Output "  Receive-Job -Id $($backendJob.Id) -Keep"
Write-Output "  Receive-Job -Id $($frontendJob.Id) -Keep"
Write-Output ""
Write-Output "To stop:"
Write-Output "  Stop-Job -Id $($backendJob.Id),$($frontendJob.Id)"
Write-Output "  Remove-Job -Id $($backendJob.Id),$($frontendJob.Id)"
Write-Output ""
Write-Output "Or run: .\stop.ps1"
Write-Output ""
