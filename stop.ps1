# OSCAL CLI Web Interface - Stop Script for Windows
# This script stops frontend, backend servers, and Docker containers

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

$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path

Write-Blue "Stopping OSCAL HUB..."
Write-Output ""

# Stop Spring Boot backend
Write-Yellow "Stopping backend..."
$backendProcesses = Get-Process -Name java -ErrorAction SilentlyContinue | Where-Object {
    $_.CommandLine -like "*spring-boot:run*"
}
if ($backendProcesses) {
    $backendProcesses | Stop-Process -Force
    Write-Green "✓ Backend stopped"
} else {
    Write-Blue "ℹ Backend was not running"
}

# Stop Next.js frontend
Write-Yellow "Stopping frontend..."
$frontendProcesses = Get-Process -Name node -ErrorAction SilentlyContinue | Where-Object {
    $_.CommandLine -like "*next*dev*" -or $_.CommandLine -like "*next-server*"
}
if ($frontendProcesses) {
    $frontendProcesses | Stop-Process -Force
    Write-Green "✓ Frontend stopped"
} else {
    Write-Blue "ℹ Frontend was not running"
}

# Stop any PowerShell jobs from start.ps1 or dev.ps1
$jobs = Get-Job -ErrorAction SilentlyContinue
if ($jobs) {
    Write-Yellow "Stopping background jobs..."
    $jobs | Stop-Job
    $jobs | Remove-Job
    Write-Green "✓ Background jobs stopped"
}

# Function to kill processes using a specific port
function Kill-Port {
    param([int]$Port)

    $processes = Get-NetTCPConnection -LocalPort $Port -ErrorAction SilentlyContinue |
                 Select-Object -ExpandProperty OwningProcess -Unique

    if ($processes) {
        foreach ($pid in $processes) {
            try {
                Stop-Process -Id $pid -Force -ErrorAction Stop
            } catch {
                # Process might have already terminated
            }
        }
        return $true
    }
    return $false
}

# Force kill processes by port
Write-Yellow "Cleaning up ports..."
if (Kill-Port -Port 8080) {
    Write-Green "✓ Port 8080 cleared"
}
if (Kill-Port -Port 3000) {
    Write-Green "✓ Port 3000 cleared"
}
if (Kill-Port -Port 3001) {
    Write-Green "✓ Port 3001 cleared"
}

# Stop Docker containers
Write-Output ""
Write-Yellow "Stopping Docker containers..."

# Check if Docker is running
try {
    docker info 2>&1 | Out-Null
    if ($LASTEXITCODE -ne 0) {
        throw "Docker not running"
    }
} catch {
    Write-Yellow "ℹ Docker is not running, skipping container cleanup"
    exit 0
}

# Stop containers from docker-compose-postgres.yml
$postgresContainer = docker ps -q --filter "name=oscal-postgres-dev" 2>$null
if ($postgresContainer) {
    Write-Yellow "Stopping PostgreSQL container..."
    docker-compose -f (Join-Path $ScriptDir "docker-compose-postgres.yml") down
    Write-Green "✓ PostgreSQL container stopped"
} else {
    Write-Blue "ℹ PostgreSQL container was not running"
}

# Check for pgAdmin
$pgadminContainer = docker ps -q --filter "name=oscal-pgadmin" 2>$null
if ($pgadminContainer) {
    Write-Yellow "Stopping pgAdmin container..."
    docker stop oscal-pgadmin 2>&1 | Out-Null
    docker rm oscal-pgadmin 2>&1 | Out-Null
    Write-Green "✓ pgAdmin container stopped"
} else {
    Write-Blue "ℹ pgAdmin container was not running"
}

# Stop any containers from the main docker-compose.yml
$oscalContainer = docker ps -q --filter "name=oscal-ux-dev" 2>$null
if ($oscalContainer) {
    Write-Yellow "Stopping OSCAL UX container..."
    docker-compose -f (Join-Path $ScriptDir "docker-compose.yml") down
    Write-Green "✓ OSCAL UX containers stopped"
}

# Verify port 5432 is freed
Write-Output ""
Write-Yellow "Verifying PostgreSQL port (5432) is freed..."
if (Kill-Port -Port 5432) {
    Start-Sleep -Seconds 1
    if (-not (Get-NetTCPConnection -LocalPort 5432 -ErrorAction SilentlyContinue)) {
        Write-Green "✓ Port 5432 is now free"
    } else {
        Write-Red "✗ Port 5432 is still in use. Manual intervention may be required."
    }
} else {
    Write-Green "✓ Port 5432 is free"
}

Write-Output ""
Write-Green "========================================"
Write-Green "All servers and containers stopped!"
Write-Green "========================================"
Write-Output ""
Write-Blue "To restart:"
Write-Output "  .\dev.ps1     # Development mode"
Write-Output "  .\start.ps1   # Production mode"
Write-Output ""
