# OSCAL CLI Installer for Windows
# This script downloads and installs the latest OSCAL CLI release
# Run this script in PowerShell: .\install.ps1

param(
    [string]$Version = "1.0.3",
    [string]$InstallDir = "$env:USERPROFILE\.oscal-cli"
)

$ErrorActionPreference = "Stop"

# Configuration
$MavenBaseUrl = "https://repo1.maven.org/maven2/gov/nist/secauto/oscal/tools/oscal-cli/cli-core"

# Helper function for colored output
function Write-ColorOutput {
    param(
        [string]$Message,
        [string]$Color = "White"
    )
    Write-Host $Message -ForegroundColor $Color
}

Write-ColorOutput "========================================" "Cyan"
Write-ColorOutput "OSCAL CLI Installer for Windows" "Cyan"
Write-ColorOutput "========================================" "Cyan"
Write-Host ""

# Check if Java is installed
Write-ColorOutput "Checking Java installation..." "Yellow"
try {
    $javaVersion = & java -version 2>&1 | Select-String -Pattern 'version' | ForEach-Object { $_ -replace '.*version "([^"]+)".*', '$1' }
    $javaMajorVersion = ($javaVersion -split '\.')[0]

    if ([int]$javaMajorVersion -lt 11) {
        Write-ColorOutput "Error: Java 11 or higher is required (found Java $javaMajorVersion)" "Red"
        Write-ColorOutput "Please upgrade Java from: https://adoptium.net/" "Yellow"
        exit 1
    }

    Write-ColorOutput "✓ Java $javaMajorVersion detected" "Green"
} catch {
    Write-ColorOutput "Error: Java is not installed or not in PATH" "Red"
    Write-ColorOutput "Please install Java 11 or higher from:" "Yellow"
    Write-Host "  - https://adoptium.net/ (recommended)"
    Write-Host "  - https://www.oracle.com/java/technologies/downloads/"
    exit 1
}

Write-Host ""

# Create installation directory
Write-ColorOutput "Creating installation directory: $InstallDir" "Yellow"
if (!(Test-Path $InstallDir)) {
    New-Item -ItemType Directory -Path $InstallDir -Force | Out-Null
}

# Download OSCAL CLI
Write-ColorOutput "Downloading OSCAL CLI version $Version..." "Yellow"
$ZipFile = "cli-core-$Version-oscal-cli.zip"
$DownloadUrl = "$MavenBaseUrl/$Version/$ZipFile"
$ZipPath = Join-Path $InstallDir $ZipFile

try {
    # Use .NET WebClient for better progress display
    $webClient = New-Object System.Net.WebClient
    $webClient.DownloadFile($DownloadUrl, $ZipPath)
    Write-ColorOutput "✓ Download complete" "Green"
} catch {
    Write-ColorOutput "Error: Failed to download OSCAL CLI" "Red"
    Write-ColorOutput "URL: $DownloadUrl" "Red"
    Write-ColorOutput "Error: $_" "Red"
    exit 1
}

Write-Host ""

# Extract the archive
Write-ColorOutput "Extracting OSCAL CLI..." "Yellow"
try {
    # Remove old installation if exists
    $BinDir = Join-Path $InstallDir "bin"
    $LibDir = Join-Path $InstallDir "lib"
    if (Test-Path $BinDir) {
        Remove-Item -Recurse -Force $BinDir
    }
    if (Test-Path $LibDir) {
        Remove-Item -Recurse -Force $LibDir
    }

    # Extract using .NET
    Add-Type -AssemblyName System.IO.Compression.FileSystem
    [System.IO.Compression.ZipFile]::ExtractToDirectory($ZipPath, $InstallDir)

    # Clean up zip file
    Remove-Item $ZipPath

    Write-ColorOutput "✓ Extraction complete" "Green"
} catch {
    Write-ColorOutput "Error: Failed to extract OSCAL CLI" "Red"
    Write-ColorOutput "Error: $_" "Red"
    exit 1
}

Write-Host ""

# Verify installation
Write-ColorOutput "Verifying installation..." "Yellow"
$OscalCliPath = Join-Path $InstallDir "bin\oscal-cli.bat"
try {
    $null = & $OscalCliPath --version 2>&1
    Write-ColorOutput "✓ OSCAL CLI installed successfully!" "Green"
} catch {
    Write-ColorOutput "Warning: Installation verification had issues, but files are in place" "Yellow"
}

Write-Host ""
Write-ColorOutput "========================================" "Cyan"
Write-ColorOutput "Installation Complete!" "Green"
Write-ColorOutput "========================================" "Cyan"
Write-Host ""

Write-ColorOutput "OSCAL CLI is installed at:" "Yellow"
Write-Host "  $InstallDir"
Write-Host ""

# Check if already in PATH
$currentPath = [Environment]::GetEnvironmentVariable("Path", "User")
$binPath = Join-Path $InstallDir "bin"

if ($currentPath -like "*$binPath*") {
    Write-ColorOutput "✓ OSCAL CLI bin directory is already in your PATH" "Green"
    Write-Host ""
    Write-ColorOutput "You can now use OSCAL CLI from any command prompt:" "Yellow"
    Write-Host "  " -NoNewline
    Write-ColorOutput "oscal-cli --help" "Green"
} else {
    Write-ColorOutput "To use oscal-cli, you have two options:" "Yellow"
    Write-Host ""

    Write-ColorOutput "Option 1: Add to PATH (recommended)" "Cyan"
    Write-Host "Would you like to add OSCAL CLI to your user PATH now? (Y/N): " -NoNewline
    $response = Read-Host

    if ($response -eq 'Y' -or $response -eq 'y') {
        try {
            $newPath = "$currentPath;$binPath"
            [Environment]::SetEnvironmentVariable("Path", $newPath, "User")
            Write-ColorOutput "✓ Added to PATH successfully!" "Green"
            Write-Host ""
            Write-ColorOutput "Please restart your command prompt or PowerShell window," "Yellow"
            Write-ColorOutput "then run: " "Yellow" -NoNewline
            Write-ColorOutput "oscal-cli --version" "Green"
        } catch {
            Write-ColorOutput "Error: Failed to update PATH" "Red"
            Write-ColorOutput "You can manually add this directory to your PATH:" "Yellow"
            Write-Host "  $binPath"
        }
    } else {
        Write-Host ""
        Write-ColorOutput "To manually add to PATH:" "Cyan"
        Write-Host "1. Open System Properties > Environment Variables"
        Write-Host "2. Edit the 'Path' variable under 'User variables'"
        Write-Host "3. Add this directory:"
        Write-Host "   " -NoNewline
        Write-ColorOutput "$binPath" "Green"
        Write-Host ""
        Write-ColorOutput "Option 2: Use full path" "Cyan"
        Write-Host "Run OSCAL CLI using the full path:"
        Write-Host "  " -NoNewline
        Write-ColorOutput "$OscalCliPath --help" "Green"
    }
}

Write-Host ""
Write-ColorOutput "Quick test:" "Yellow"
if ($currentPath -like "*$binPath*") {
    Write-Host "  " -NoNewline
    Write-ColorOutput "oscal-cli --version" "Green"
} else {
    Write-Host "  " -NoNewline
    Write-ColorOutput "$OscalCliPath --version" "Green"
}

Write-Host ""
Write-ColorOutput "For usage guide, see: USER_GUIDE.md" "Yellow"
Write-Host ""
