# OSCAL CLI - Command-Line Deployment Guide

**Version**: 1.0.0
**Date**: 2025-10-26
**Purpose**: Deploy and use OSCAL CLI as a standalone command-line tool for validation, conversion, and automation

---

## Table of Contents

1. [Overview](#overview)
2. [Quick Start (2 Minutes)](#quick-start-2-minutes)
3. [Prerequisites](#prerequisites)
4. [Installation Methods](#installation-methods)
5. [Using the CLI](#using-the-cli)
6. [Common Workflows](#common-workflows)
7. [Batch Processing & Automation](#batch-processing--automation)
8. [CI/CD Integration](#cicd-integration)
9. [Troubleshooting](#troubleshooting)
10. [Advanced Usage](#advanced-usage)
11. [Comparison: CLI vs Web vs Azure](#comparison-cli-vs-web-vs-azure)

---

## Overview

The OSCAL CLI is a **standalone command-line tool** for working with OSCAL documents without requiring a web interface or database. Perfect for:

- **Automation** - Integrate OSCAL validation into scripts and pipelines
- **Batch Processing** - Process hundreds of OSCAL files in bulk
- **CI/CD Pipelines** - Validate OSCAL in GitHub Actions, GitLab CI, Jenkins, etc.
- **Offline Use** - No internet connection required after installation
- **Lightweight** - No database, no web server, just a Java application
- **Scripting** - Chain OSCAL operations together with shell scripts

### What You Get with CLI Mode

The CLI deployment provides:

- ✅ **Command-Line Tool** - `oscal-cli` executable for all OSCAL operations
- ✅ **All OSCAL Models** - Catalog, Profile, SSP, Component Definition, AP, AR, POA&M
- ✅ **Format Conversion** - Convert between XML, JSON, and YAML
- ✅ **Profile Resolution** - Resolve profiles to catalogs
- ✅ **Schema Validation** - Validate against OSCAL schemas and constraints
- ✅ **Batch Operations** - Process multiple files with shell loops
- ✅ **Cross-Platform** - Mac, Linux, and Windows support

### What's NOT Included in CLI Mode

CLI mode does **not** include:

- ❌ Web interface (no browser UI)
- ❌ User authentication or multi-user support
- ❌ Database for storing files or history
- ❌ REST API endpoints
- ❌ Real-time collaboration features
- ❌ Authorization template management (web-only feature)

For web interface and REST API, see [LOCAL-DEPLOYMENT-GUIDE.md](LOCAL-DEPLOYMENT-GUIDE.md) or [AZURE-DEPLOYMENT-GUIDE.md](AZURE-DEPLOYMENT-GUIDE.md).

---

## Quick Start (2 Minutes)

### Mac/Linux

```bash
# 1. Download and run the installer
curl -fsSL https://raw.githubusercontent.com/RegScale/oscal-hub/main/installer/install.sh | bash

# 2. Add to PATH (if not already done)
export PATH="$PATH:$HOME/.oscal-cli/bin"

# 3. Verify installation
oscal-cli --version

# 4. Validate an OSCAL file
oscal-cli catalog validate examples/catalog.json
```

### Windows (PowerShell)

```powershell
# 1. Allow script execution (if needed)
Set-ExecutionPolicy -ExecutionPolicy RemoteSigned -Scope CurrentUser

# 2. Download and run installer
Invoke-WebRequest -Uri https://raw.githubusercontent.com/RegScale/oscal-hub/main/installer/install.ps1 -OutFile install.ps1
.\install.ps1

# 3. Add to PATH or use full path
%USERPROFILE%\.oscal-cli\bin\oscal-cli.bat --version

# 4. Validate an OSCAL file
oscal-cli catalog validate examples\catalog.json
```

**Total time**: 2-5 minutes (depending on download speed)

---

## Prerequisites

### Required Software

1. **Java 11 or higher** (Java 21 LTS recommended)

   **Check your Java version:**
   ```bash
   java -version
   # Expected: openjdk version "11.0.0" or higher
   ```

   **Install Java if needed:**

   **Mac:**
   ```bash
   # Using Homebrew
   brew install openjdk@21

   # Or download from:
   # https://adoptium.net/
   ```

   **Linux (Ubuntu/Debian):**
   ```bash
   sudo apt update
   sudo apt install openjdk-21-jdk
   ```

   **Windows:**
   ```powershell
   # Using winget
   winget install EclipseAdoptium.Temurin.21.JDK

   # Or download from:
   # https://adoptium.net/
   ```

### Optional Software

2. **Git** (for cloning repository and examples)
   ```bash
   # Mac
   brew install git

   # Linux
   sudo apt install git

   # Windows
   winget install Git.Git
   ```

3. **curl or wget** (for downloading installer)
   - Usually pre-installed on Mac/Linux
   - Windows users can use PowerShell's `Invoke-WebRequest`

### System Requirements

| Requirement | Minimum | Recommended |
|-------------|---------|-------------|
| **RAM** | 512 MB | 2 GB |
| **Disk Space** | 100 MB | 500 MB |
| **CPU** | 1 core | 2+ cores |
| **OS** | Java-compatible OS | macOS 11+, Ubuntu 20.04+, Windows 10+ |

### Network Requirements

- Internet connection **only** required for:
  - Initial installation (downloading from Maven Central)
  - Profile resolution with remote catalog imports

- After installation, CLI works **100% offline**

### Verify Prerequisites

```bash
# Check Java
java -version
# Expected output: version "11.0.0" or higher

# Check PATH utilities (Mac/Linux)
which curl
which unzip

# Check PowerShell version (Windows)
$PSVersionTable.PSVersion
# Expected: 5.0 or higher
```

---

## Installation Methods

### Method 1: Automated Installer (Recommended)

The automated installer is the easiest way to get started.

#### Mac/Linux

```bash
# Download and run the installer
curl -fsSL https://raw.githubusercontent.com/RegScale/oscal-hub/main/installer/install.sh | bash

# Or download first, then run
curl -O https://raw.githubusercontent.com/RegScale/oscal-hub/main/installer/install.sh
chmod +x install.sh
./install.sh
```

**What the installer does:**
1. ✅ Checks for Java 11+ installation
2. ✅ Downloads latest OSCAL CLI from Maven Central
3. ✅ Extracts to `~/.oscal-cli`
4. ✅ Sets executable permissions
5. ✅ Provides PATH configuration instructions

**Custom installation directory:**
```bash
OSCAL_CLI_HOME=/opt/oscal-cli ./install.sh
```

**Install specific version:**
```bash
OSCAL_CLI_VERSION=1.0.3 ./install.sh
```

**Add to PATH:**
```bash
# Add to ~/.bashrc, ~/.zshrc, or ~/.bash_profile
echo 'export PATH="$PATH:$HOME/.oscal-cli/bin"' >> ~/.bashrc
source ~/.bashrc
```

#### Windows (PowerShell)

```powershell
# 1. Allow script execution (if needed)
Set-ExecutionPolicy -ExecutionPolicy RemoteSigned -Scope CurrentUser

# 2. Download installer
Invoke-WebRequest -Uri https://raw.githubusercontent.com/RegScale/oscal-hub/main/installer/install.ps1 -OutFile install.ps1

# 3. Run installer
.\install.ps1

# 4. (Optional) Add to PATH automatically
.\install.ps1 -AddToPath
```

**Custom installation directory:**
```powershell
.\install.ps1 -InstallDir "C:\Tools\oscal-cli"
```

**Install specific version:**
```powershell
.\install.ps1 -Version "1.0.3"
```

### Method 2: Manual Installation

If you prefer manual control or the automated installer doesn't work:

#### Step 1: Download OSCAL CLI

Go to [Maven Central](https://repo1.maven.org/maven2/gov/nist/secauto/oscal/tools/oscal-cli/cli-core/) and download the latest version:

```bash
# Example: Download version 1.0.3
wget https://repo1.maven.org/maven2/gov/nist/secauto/oscal/tools/oscal-cli/cli-core/1.0.3/cli-core-1.0.3-oscal-cli.zip

# Or using curl
curl -LO https://repo1.maven.org/maven2/gov/nist/secauto/oscal/tools/oscal-cli/cli-core/1.0.3/cli-core-1.0.3-oscal-cli.zip
```

#### Step 2: Extract the Archive

**Mac/Linux:**
```bash
# Create installation directory
mkdir -p ~/.oscal-cli

# Extract archive
unzip cli-core-1.0.3-oscal-cli.zip -d ~/.oscal-cli

# Verify extraction
ls -la ~/.oscal-cli/bin/oscal-cli
```

**Windows:**
```powershell
# Create installation directory
New-Item -ItemType Directory -Path $env:USERPROFILE\.oscal-cli -Force

# Extract archive
Expand-Archive -Path cli-core-1.0.3-oscal-cli.zip -DestinationPath $env:USERPROFILE\.oscal-cli

# Verify extraction
Get-ChildItem $env:USERPROFILE\.oscal-cli\bin\oscal-cli.bat
```

#### Step 3: Configure PATH

**Mac/Linux:**
```bash
# Add to ~/.bashrc (or ~/.zshrc for Zsh)
echo 'export PATH="$PATH:$HOME/.oscal-cli/bin"' >> ~/.bashrc
source ~/.bashrc

# Verify
which oscal-cli
oscal-cli --version
```

**Windows:**
```powershell
# Option 1: Add to user PATH via Environment Variables UI
# 1. Open: System Properties → Environment Variables
# 2. Under User variables, edit "Path"
# 3. Add: %USERPROFILE%\.oscal-cli\bin
# 4. Click OK

# Option 2: Add to PATH via PowerShell (requires admin)
[Environment]::SetEnvironmentVariable(
    "Path",
    [Environment]::GetEnvironmentVariable("Path", "User") + ";$env:USERPROFILE\.oscal-cli\bin",
    "User"
)

# Restart PowerShell, then verify
oscal-cli --version
```

### Method 3: Build from Source

For developers or those who want the latest unreleased version:

```bash
# 1. Clone repository
git clone https://github.com/RegScale/oscal-hub.git
cd oscal-cli

# 2. Build with Maven
cd cli
mvn clean install

# 3. Run from build output
./target/appassembler/bin/oscal-cli --version

# 4. (Optional) Copy to standard location
mkdir -p ~/.oscal-cli
cp -r target/appassembler/* ~/.oscal-cli/
echo 'export PATH="$PATH:$HOME/.oscal-cli/bin"' >> ~/.bashrc
```

### Verify Installation

After installation with any method:

```bash
# Check version
oscal-cli --version

# Display help
oscal-cli --help

# Test with a simple command
oscal-cli catalog --help
```

**Expected output:**
```
OSCAL CLI version 1.0.3
Built with liboscal-java 3.0.3
Metaschema Framework 0.12.2
```

---

## Using the CLI

### Command Structure

OSCAL CLI uses a hierarchical command structure:

```
oscal-cli <model> <operation> [options] <file>
```

**Components:**
- `<model>` - The OSCAL model type (catalog, profile, ssp, etc.)
- `<operation>` - What to do (validate, convert, resolve)
- `[options]` - Flags like `--to`, `--as`, `--overwrite`
- `<file>` - Input file path

### Available Models

| Model | Command | Description |
|-------|---------|-------------|
| **Catalog** | `catalog` | Security control catalogs (e.g., NIST SP 800-53) |
| **Profile** | `profile` | Baselines that tailor catalogs |
| **Component Definition** | `component-definition` | Component implementation details |
| **System Security Plan** | `ssp` | System security documentation |
| **Assessment Plan** | `assessment-plan` or `ap` | Security assessment plans |
| **Assessment Results** | `assessment-results` or `ar` | Assessment findings |
| **POA&M** | `poam` | Plan of Actions and Milestones |
| **Metaschema** | `metaschema` | Metaschema definitions |

### Common Operations

#### 1. Validation

Validate that an OSCAL document is well-formed and complies with schemas.

```bash
# Validate a catalog
oscal-cli catalog validate my-catalog.xml

# Validate a profile
oscal-cli profile validate my-profile.json

# Validate an SSP
oscal-cli ssp validate my-ssp.yaml

# Validate with explicit format specification
oscal-cli catalog validate --as xml catalog-file-without-extension
```

**Success output:**
```
The file 'my-catalog.xml' is valid.
```

**Failure output:**
```
Validation errors found in 'my-catalog.xml':
  - Line 45: Missing required element 'title'
  - Line 120: Invalid UUID format
```

#### 2. Format Conversion

Convert between XML, JSON, and YAML formats.

```bash
# XML to JSON
oscal-cli catalog convert --to json catalog.xml catalog.json

# JSON to YAML
oscal-cli profile convert --to yaml profile.json profile.yaml

# YAML to XML
oscal-cli ssp convert --to xml ssp.yaml ssp.xml

# Convert with overwrite
oscal-cli catalog convert --to json catalog.xml catalog.json --overwrite

# Convert and output to stdout (no output file)
oscal-cli profile convert --to json profile.xml
```

**Options:**
- `--to <format>` - Target format: `xml`, `json`, or `yaml`
- `--as <format>` - Source format (if not auto-detected)
- `--overwrite` - Overwrite existing output file

#### 3. Profile Resolution

Resolve a profile into a catalog (profiles only).

```bash
# Resolve profile to JSON catalog
oscal-cli profile resolve --to json my-baseline.xml resolved-catalog.json

# Resolve profile to XML catalog
oscal-cli profile resolve --to xml my-baseline.json resolved-catalog.xml

# Resolve and output to stdout
oscal-cli profile resolve --to json my-baseline.xml
```

**What resolution does:**
- Imports referenced catalogs (local or remote)
- Selects controls based on include/exclude rules
- Applies parameter modifications
- Applies control modifications
- Produces a stand-alone resolved catalog

### Getting Help

```bash
# General help
oscal-cli --help

# Model-specific help
oscal-cli catalog --help
oscal-cli profile --help
oscal-cli ssp --help

# Operation-specific help
oscal-cli catalog validate --help
oscal-cli profile resolve --help
```

---

## Common Workflows

### Workflow 1: Validate All Files in a Directory

**Bash (Mac/Linux):**
```bash
# Validate all XML catalogs
for file in catalogs/*.xml; do
    echo "Validating $file..."
    oscal-cli catalog validate "$file"
done

# Validate with error tracking
EXIT_CODE=0
for file in *.json; do
    if ! oscal-cli ssp validate "$file"; then
        echo "ERROR: $file failed validation"
        EXIT_CODE=1
    fi
done
exit $EXIT_CODE
```

**PowerShell (Windows):**
```powershell
# Validate all JSON profiles
Get-ChildItem *.json | ForEach-Object {
    Write-Host "Validating $_..."
    oscal-cli profile validate $_.Name
}

# Validate with error tracking
$exitCode = 0
Get-ChildItem *.xml | ForEach-Object {
    if (-not (oscal-cli catalog validate $_.Name)) {
        Write-Host "ERROR: $_ failed validation"
        $exitCode = 1
    }
}
exit $exitCode
```

### Workflow 2: Batch Format Conversion

Convert all OSCAL files from one format to another.

**Bash:**
```bash
# Convert all XML catalogs to JSON
for file in *.xml; do
    output="${file%.xml}.json"
    echo "Converting $file to $output..."
    oscal-cli catalog convert --to json "$file" "$output" --overwrite
done

# Convert all JSON to YAML with error handling
for file in *.json; do
    output="${file%.json}.yaml"
    if oscal-cli ssp convert --to yaml "$file" "$output"; then
        echo "✓ Converted $file"
    else
        echo "✗ Failed to convert $file"
    fi
done
```

**PowerShell:**
```powershell
# Convert all XML to JSON
Get-ChildItem *.xml | ForEach-Object {
    $output = $_.BaseName + ".json"
    Write-Host "Converting $_ to $output..."
    oscal-cli catalog convert --to json $_.Name $output --overwrite
}
```

### Workflow 3: Profile Resolution Pipeline

Resolve multiple profiles and validate the output.

```bash
#!/bin/bash
# resolve-profiles.sh

PROFILE_DIR="profiles"
OUTPUT_DIR="resolved-catalogs"

mkdir -p "$OUTPUT_DIR"

for profile in "$PROFILE_DIR"/*.json; do
    filename=$(basename "$profile" .json)
    output="$OUTPUT_DIR/${filename}-resolved.json"

    echo "Resolving $profile..."

    # Resolve profile
    if oscal-cli profile resolve --to json "$profile" "$output"; then
        echo "✓ Resolved to $output"

        # Validate resolved catalog
        if oscal-cli catalog validate "$output"; then
            echo "✓ Validation passed"
        else
            echo "✗ Validation failed for $output"
            exit 1
        fi
    else
        echo "✗ Failed to resolve $profile"
        exit 1
    fi
done

echo "All profiles resolved and validated successfully!"
```

### Workflow 4: Validate Before Commit (Git Hook)

Create a pre-commit hook to validate OSCAL files automatically.

```bash
#!/bin/bash
# .git/hooks/pre-commit

echo "Validating OSCAL files..."

# Get staged OSCAL files
OSCAL_FILES=$(git diff --cached --name-only --diff-filter=ACM | grep -E '\.(xml|json|yaml)$')

if [ -z "$OSCAL_FILES" ]; then
    echo "No OSCAL files to validate"
    exit 0
fi

EXIT_CODE=0

for file in $OSCAL_FILES; do
    # Determine model type from directory structure
    if [[ "$file" == *"catalog"* ]]; then
        MODEL="catalog"
    elif [[ "$file" == *"profile"* ]]; then
        MODEL="profile"
    elif [[ "$file" == *"ssp"* ]]; then
        MODEL="ssp"
    else
        echo "Skipping $file (unknown model type)"
        continue
    fi

    echo "Validating $file as $MODEL..."

    if ! oscal-cli "$MODEL" validate "$file"; then
        echo "✗ Validation failed for $file"
        EXIT_CODE=1
    fi
done

if [ $EXIT_CODE -ne 0 ]; then
    echo ""
    echo "COMMIT BLOCKED: OSCAL validation failed"
    echo "Fix validation errors and try again"
fi

exit $EXIT_CODE
```

Make it executable:
```bash
chmod +x .git/hooks/pre-commit
```

---

## Batch Processing & Automation

### Processing Large Numbers of Files

When you have hundreds or thousands of OSCAL files to process:

```bash
#!/bin/bash
# batch-validate.sh - Validate OSCAL files in parallel

THREADS=4  # Number of parallel processes
LOG_FILE="validation-results.log"

find oscal-files/ -name "*.json" -print0 | \
  xargs -0 -P "$THREADS" -I {} bash -c '
    if oscal-cli catalog validate "{}"; then
      echo "✓ {}" >> '"$LOG_FILE"'
    else
      echo "✗ {}" >> '"$LOG_FILE"'
    fi
  '

echo "Validation complete. Results in $LOG_FILE"
```

### Logging and Reporting

Generate detailed validation reports:

```bash
#!/bin/bash
# validate-and-report.sh

REPORT_FILE="validation-report-$(date +%Y%m%d-%H%M%S).txt"

echo "OSCAL Validation Report" > "$REPORT_FILE"
echo "Date: $(date)" >> "$REPORT_FILE"
echo "----------------------------------------" >> "$REPORT_FILE"

TOTAL=0
PASSED=0
FAILED=0

for file in oscal/**/*.{xml,json,yaml}; do
    [ -f "$file" ] || continue

    TOTAL=$((TOTAL + 1))

    if oscal-cli catalog validate "$file" 2>&1 | tee -a "$REPORT_FILE"; then
        PASSED=$((PASSED + 1))
        echo "✓ PASSED: $file" >> "$REPORT_FILE"
    else
        FAILED=$((FAILED + 1))
        echo "✗ FAILED: $file" >> "$REPORT_FILE"
    fi

    echo "----------------------------------------" >> "$REPORT_FILE"
done

echo "" >> "$REPORT_FILE"
echo "Summary:" >> "$REPORT_FILE"
echo "  Total files: $TOTAL" >> "$REPORT_FILE"
echo "  Passed: $PASSED" >> "$REPORT_FILE"
echo "  Failed: $FAILED" >> "$REPORT_FILE"

cat "$REPORT_FILE"
```

### Scheduled Validation

Use cron (Linux/Mac) to validate OSCAL files on a schedule:

```bash
# Edit crontab
crontab -e

# Add entry to run validation daily at 2 AM
0 2 * * * /home/user/scripts/validate-oscal.sh >> /home/user/logs/oscal-validation.log 2>&1
```

Use Windows Task Scheduler for scheduled tasks on Windows.

---

## CI/CD Integration

### GitHub Actions

Create `.github/workflows/validate-oscal.yml`:

```yaml
name: Validate OSCAL Files

on:
  push:
    branches: [ main, develop ]
  pull_request:
    branches: [ main ]

jobs:
  validate:
    runs-on: ubuntu-latest

    steps:
      - name: Checkout code
        uses: actions/checkout@v3

      - name: Set up Java 21
        uses: actions/setup-java@v3
        with:
          java-version: '21'
          distribution: 'temurin'

      - name: Install OSCAL CLI
        run: |
          curl -fsSL https://raw.githubusercontent.com/RegScale/oscal-hub/main/installer/install.sh | bash
          echo "$HOME/.oscal-cli/bin" >> $GITHUB_PATH

      - name: Verify installation
        run: oscal-cli --version

      - name: Validate catalogs
        run: |
          for file in catalogs/*.xml; do
            echo "Validating $file..."
            oscal-cli catalog validate "$file"
          done

      - name: Validate profiles
        run: |
          for file in profiles/*.json; do
            echo "Validating $file..."
            oscal-cli profile validate "$file"
          done

      - name: Validate SSPs
        run: |
          for file in ssps/*.json; do
            echo "Validating $file..."
            oscal-cli ssp validate "$file"
          done
```

### GitLab CI

Create `.gitlab-ci.yml`:

```yaml
image: eclipse-temurin:21-jdk

stages:
  - validate
  - convert

before_script:
  - apt-get update && apt-get install -y curl unzip
  - curl -fsSL https://raw.githubusercontent.com/RegScale/oscal-hub/main/installer/install.sh | bash
  - export PATH="$PATH:$HOME/.oscal-cli/bin"
  - oscal-cli --version

validate_catalogs:
  stage: validate
  script:
    - |
      for file in catalogs/*.xml; do
        echo "Validating $file..."
        oscal-cli catalog validate "$file"
      done

validate_profiles:
  stage: validate
  script:
    - |
      for file in profiles/*.json; do
        echo "Validating $file..."
        oscal-cli profile validate "$file"
      done

convert_to_json:
  stage: convert
  script:
    - mkdir -p output
    - |
      for file in catalogs/*.xml; do
        output="output/$(basename "$file" .xml).json"
        oscal-cli catalog convert --to json "$file" "$output"
      done
  artifacts:
    paths:
      - output/
    expire_in: 1 week
```

### Jenkins Pipeline

Create `Jenkinsfile`:

```groovy
pipeline {
    agent any

    tools {
        jdk 'JDK21'
    }

    stages {
        stage('Install OSCAL CLI') {
            steps {
                sh '''
                    curl -fsSL https://raw.githubusercontent.com/RegScale/oscal-hub/main/installer/install.sh | bash
                    export PATH="$PATH:$HOME/.oscal-cli/bin"
                '''
            }
        }

        stage('Validate OSCAL Files') {
            steps {
                sh '''
                    export PATH="$PATH:$HOME/.oscal-cli/bin"

                    for file in oscal/**/*.{xml,json}; do
                        [ -f "$file" ] || continue
                        echo "Validating $file..."
                        oscal-cli catalog validate "$file"
                    done
                '''
            }
        }

        stage('Generate Report') {
            steps {
                sh '''
                    export PATH="$PATH:$HOME/.oscal-cli/bin"
                    echo "Validation completed successfully"
                '''
            }
        }
    }

    post {
        failure {
            emailext (
                subject: "OSCAL Validation Failed: ${env.JOB_NAME} - Build #${env.BUILD_NUMBER}",
                body: "OSCAL validation failed. Check Jenkins for details.",
                to: "team@example.com"
            )
        }
    }
}
```

### Azure DevOps Pipeline

Create `azure-pipelines.yml`:

```yaml
trigger:
  branches:
    include:
      - main
      - develop

pool:
  vmImage: 'ubuntu-latest'

steps:
  - task: JavaToolInstaller@0
    inputs:
      versionSpec: '21'
      jdkArchitectureOption: 'x64'
      jdkSourceOption: 'PreInstalled'

  - script: |
      curl -fsSL https://raw.githubusercontent.com/RegScale/oscal-hub/main/installer/install.sh | bash
      echo "##vso[task.prependpath]$HOME/.oscal-cli/bin"
    displayName: 'Install OSCAL CLI'

  - script: |
      oscal-cli --version
    displayName: 'Verify OSCAL CLI'

  - script: |
      for file in catalogs/*.xml; do
        echo "Validating $file..."
        oscal-cli catalog validate "$file"
      done
    displayName: 'Validate Catalogs'

  - script: |
      for file in profiles/*.json; do
        echo "Validating $file..."
        oscal-cli profile validate "$file"
      done
    displayName: 'Validate Profiles'
```

---

## Troubleshooting

### Issue: Command Not Found

**Symptom:**
```bash
oscal-cli: command not found
```

**Solution 1: Add to PATH**
```bash
# Mac/Linux
export PATH="$PATH:$HOME/.oscal-cli/bin"
echo 'export PATH="$PATH:$HOME/.oscal-cli/bin"' >> ~/.bashrc
source ~/.bashrc

# Windows PowerShell (requires admin)
[Environment]::SetEnvironmentVariable("Path",
    $env:Path + ";$env:USERPROFILE\.oscal-cli\bin",
    "User")
```

**Solution 2: Use full path**
```bash
# Mac/Linux
~/.oscal-cli/bin/oscal-cli --version

# Windows
%USERPROFILE%\.oscal-cli\bin\oscal-cli.bat --version
```

### Issue: Java Not Found or Wrong Version

**Symptom:**
```
Error: Java is not installed or not in PATH
Error: Java 11 or higher is required (found Java 8)
```

**Solution:**
1. Install Java 21 (LTS) from [Adoptium](https://adoptium.net/)
2. Verify installation:
   ```bash
   java -version
   ```
3. If multiple Java versions installed, set `JAVA_HOME`:
   ```bash
   # Mac/Linux
   export JAVA_HOME=/path/to/java21
   export PATH="$JAVA_HOME/bin:$PATH"

   # Windows
   setx JAVA_HOME "C:\Program Files\Eclipse Adoptium\jdk-21.0.0.0-hotspot"
   ```

### Issue: Validation Fails - Missing Required Elements

**Symptom:**
```
Validation failed: Missing required element 'title'
```

**Solution:**
Review the OSCAL schema requirements for your model type:
- Check [OSCAL Reference Documentation](https://pages.nist.gov/OSCAL/reference/)
- Common issues:
  - Missing `title` in metadata
  - Invalid UUID format (must be RFC 4122 compliant)
  - Incorrect element nesting
  - Missing required attributes

**Debug approach:**
```bash
# Validate a known-good example first
oscal-cli catalog validate examples/nist-800-53-catalog.json

# Compare your file structure to the working example
diff your-catalog.json examples/nist-800-53-catalog.json
```

### Issue: Profile Resolution Fails

**Symptom:**
```
Unable to resolve profile: Failed to load imported catalog
```

**Common causes and solutions:**

1. **Network issue** (remote catalog unreachable)
   ```bash
   # Test URL accessibility
   curl -I https://url-to-catalog.xml

   # Use local catalog instead
   # Edit profile's import href to point to local file
   ```

2. **Invalid catalog reference**
   ```bash
   # Verify the catalog file exists
   ls -la path/to/catalog.xml

   # Validate the catalog separately
   oscal-cli catalog validate path/to/catalog.xml
   ```

3. **Malformed catalog**
   ```bash
   # Validate imported catalog
   oscal-cli catalog validate imported-catalog.xml

   # Fix validation errors before resolving profile
   ```

### Issue: Out of Memory with Large Files

**Symptom:**
```
java.lang.OutOfMemoryError: Java heap space
```

**Solution:**
Increase Java heap size:

```bash
# Mac/Linux
export JAVA_OPTS="-Xmx4g"
oscal-cli catalog validate large-catalog.xml

# Or set permanently in shell profile
echo 'export JAVA_OPTS="-Xmx4g"' >> ~/.bashrc
```

**Windows:**
```powershell
# Set environment variable
$env:JAVA_OPTS="-Xmx4g"
oscal-cli catalog validate large-catalog.xml

# Or set permanently
[Environment]::SetEnvironmentVariable("JAVA_OPTS", "-Xmx4g", "User")
```

**Heap size recommendations:**
- Small files (<1MB): Default (usually 512MB-1GB)
- Medium files (1-10MB): `-Xmx2g`
- Large files (10-100MB): `-Xmx4g`
- Very large files (>100MB): `-Xmx8g` or higher

### Issue: Permission Denied

**Symptom:**
```bash
Permission denied: cannot write to output file
```

**Solution:**
```bash
# Check file permissions
ls -la output-file.json

# Make directory writable
chmod u+w output-directory/

# Use --overwrite flag
oscal-cli catalog convert --to json catalog.xml catalog.json --overwrite

# Or specify different output location
oscal-cli catalog convert --to json catalog.xml ~/Documents/catalog.json
```

### Issue: Special Characters in Filenames

**Symptom:**
Files with spaces or special characters cause errors.

**Solution:**
Always quote filenames:

```bash
# Correct
oscal-cli catalog validate "my catalog file.xml"
oscal-cli profile convert --to json "baseline (draft).xml" "baseline.json"

# Incorrect (will fail)
oscal-cli catalog validate my catalog file.xml
```

---

## Advanced Usage

### Format Auto-Detection

OSCAL CLI automatically detects input format from file extension:

```bash
# Auto-detected as XML
oscal-cli catalog validate catalog.xml

# Auto-detected as JSON
oscal-cli profile validate profile.json

# Auto-detected as YAML
oscal-cli ssp validate ssp.yaml
```

Override auto-detection with `--as`:

```bash
# File has .txt extension but is actually XML
oscal-cli catalog validate --as xml catalog.txt

# File has no extension
oscal-cli profile validate --as json profile-file
```

### Working with URLs

OSCAL CLI can resolve profiles that import catalogs from HTTP/HTTPS URLs:

```bash
# Profile references catalog at https://...
oscal-cli profile resolve --to json profile-with-remote-import.xml resolved.json
```

**Requirements:**
- Internet connection
- Accessible URL (not behind authentication)
- Valid OSCAL catalog at the URL

### Piping and Redirection

Use stdin/stdout for piping operations:

```bash
# Convert and pipe to another tool
oscal-cli catalog convert --to json catalog.xml | jq '.catalog.metadata.title'

# Validate multiple files and filter results
find . -name "*.xml" -exec oscal-cli catalog validate {} \; | grep -i "error"

# Convert and save with redirection
oscal-cli profile convert --to json profile.xml > profile.json

# Chain operations
cat catalog.xml | oscal-cli catalog convert --to json --as xml > catalog.json
```

### Custom Java Settings

Configure Java runtime parameters:

```bash
# Increase heap size
export JAVA_OPTS="-Xmx4g"

# Enable debug logging
export JAVA_OPTS="-Xmx2g -Dlog.level=DEBUG"

# Set specific Java version (if multiple installed)
export JAVA_HOME=/path/to/jdk-21
export PATH="$JAVA_HOME/bin:$PATH"

# Run with custom settings
oscal-cli catalog validate large-catalog.xml
```

### Integration with Other Tools

#### jq (JSON processing)

```bash
# Extract catalog title
oscal-cli catalog convert --to json catalog.xml | jq -r '.catalog.metadata.title'

# Count controls in resolved profile
oscal-cli profile resolve --to json profile.xml | jq '.catalog.groups[].controls | length'

# Extract all control IDs
oscal-cli catalog convert --to json catalog.xml | jq -r '.catalog.groups[].controls[].id'
```

#### xmllint (XML processing)

```bash
# Pretty-print XML output
oscal-cli catalog convert --to xml catalog.json | xmllint --format -

# Validate XML syntax
oscal-cli profile convert --to xml profile.json | xmllint --noout -
```

#### yq (YAML processing)

```bash
# Extract metadata from YAML
oscal-cli ssp convert --to yaml ssp.json | yq '.system-security-plan.metadata'

# Modify and re-validate
yq '.system-security-plan.metadata.title = "Updated Title"' ssp.yaml | \
  oscal-cli ssp validate --as yaml
```

### Docker Container Usage

Run OSCAL CLI in a Docker container:

```dockerfile
# Dockerfile
FROM eclipse-temurin:21-jdk-alpine

RUN apk add --no-cache curl bash

RUN curl -fsSL https://raw.githubusercontent.com/RegScale/oscal-hub/main/installer/install.sh | bash

ENV PATH="/root/.oscal-cli/bin:${PATH}"

WORKDIR /oscal

ENTRYPOINT ["oscal-cli"]
```

Build and run:

```bash
# Build image
docker build -t oscal-cli .

# Run validation
docker run --rm -v $(pwd):/oscal oscal-cli catalog validate /oscal/catalog.xml

# Run conversion
docker run --rm -v $(pwd):/oscal oscal-cli profile convert --to json /oscal/profile.xml /oscal/profile.json
```

---

## Comparison: CLI vs Web vs Azure

| Feature | CLI Mode | Local Web | Azure Deployment |
|---------|----------|-----------|------------------|
| **Setup Time** | 2 minutes | 5-10 minutes | 30-60 minutes (one-time) |
| **Installation** | Single download | Docker + build | Cloud resources |
| **Cost** | Free | Free | ~$75-100/month |
| **Interface** | Command-line only | Browser UI + API + CLI | Browser UI + API + CLI |
| **Authentication** | None (single-user) | JWT multi-user | JWT multi-user |
| **Database** | None | H2 file-based | PostgreSQL (production) |
| **File Storage** | Local filesystem | Local filesystem | Azure Blob Storage |
| **History Tracking** | Manual (scripts) | Automatic | Automatic |
| **Internet Required** | No (after install) | No | Yes |
| **Batch Processing** | ✅ Excellent | ⚠️ Via API | ⚠️ Via API |
| **Automation** | ✅ Excellent | ✅ Good (API) | ✅ Good (API) |
| **CI/CD Integration** | ✅ Perfect | ⚠️ Possible | ✅ Good |
| **Offline Use** | ✅ Yes | ✅ Yes | ❌ No |
| **Scalability** | Limited to single machine | Limited to single machine | Auto-scaling |
| **Collaboration** | Manual (Git, shared files) | Multi-user (local) | Multi-user (cloud) |
| **Backup** | Manual | Manual | Automatic (Azure) |
| **Updates** | Manual download | `git pull` | CI/CD pipeline |
| **Best For** | Automation, CI/CD, Scripts | Local development, Testing | Production, Teams |

### When to Use CLI Mode

**✅ Use CLI when you need:**
- Automation and scripting
- CI/CD pipeline integration
- Batch processing of many files
- Offline usage
- Lightweight tool without database
- Command-line workflow
- Version control integration (Git hooks)
- Cron jobs / scheduled tasks

**❌ Don't use CLI when you need:**
- Web browser interface
- Multi-user collaboration
- Visual file management
- Operation history tracking
- Authorization template management
- Database storage
- Real-time collaboration

### When to Use Local Web Mode

**✅ Use Local Web when you need:**
- User-friendly browser interface
- Testing before Azure deployment
- Local development environment
- Visual file management
- Operation history
- Authorization templates
- Multi-user support (local)

See [LOCAL-DEPLOYMENT-GUIDE.md](LOCAL-DEPLOYMENT-GUIDE.md) for details.

### When to Use Azure Deployment

**✅ Use Azure when you need:**
- Production environment
- Team collaboration
- Auto-scaling
- Automatic backups
- High availability
- Cloud storage (Blob)
- Enterprise security

See [AZURE-DEPLOYMENT-GUIDE.md](AZURE-DEPLOYMENT-GUIDE.md) for details.

---

## Performance Tips

### Speed Up Batch Processing

1. **Use parallel processing:**
   ```bash
   # Process 4 files simultaneously
   find oscal/ -name "*.xml" -print0 | \
     xargs -0 -P 4 -I {} oscal-cli catalog validate "{}"
   ```

2. **Pre-allocate heap for large files:**
   ```bash
   export JAVA_OPTS="-Xmx4g -Xms4g"  # Set min=max for faster startup
   ```

3. **Use local catalogs instead of remote URLs:**
   ```bash
   # Download catalog once
   curl -o nist-800-53.json https://url-to-catalog.json

   # Update profile to use local file
   # Then resolve profile
   oscal-cli profile resolve --to json profile.xml resolved.json
   ```

### Optimize for CI/CD

1. **Cache installation:**
   ```yaml
   # GitHub Actions
   - name: Cache OSCAL CLI
     uses: actions/cache@v3
     with:
       path: ~/.oscal-cli
       key: oscal-cli-${{ runner.os }}-1.0.3
   ```

2. **Fail fast:**
   ```bash
   # Exit on first error
   set -e
   for file in *.xml; do
       oscal-cli catalog validate "$file"
   done
   ```

3. **Validate only changed files:**
   ```bash
   # In Git hook or CI
   git diff --name-only HEAD | grep '\.xml$' | while read file; do
       oscal-cli catalog validate "$file"
   done
   ```

---

## Next Steps

After successful CLI installation:

1. **Read the User Guide**: [USER_GUIDE.md](../USER_GUIDE.md)
2. **Try example files**: Download from [OSCAL Content Repository](https://github.com/usnistgov/oscal-content)
3. **Integrate into CI/CD**: See [CI/CD Integration](#cicd-integration) section
4. **Set up automation**: See [Batch Processing](#batch-processing--automation) section
5. **Consider web interface**: See [LOCAL-DEPLOYMENT-GUIDE.md](LOCAL-DEPLOYMENT-GUIDE.md)

---

## Support

### Documentation

- **This Guide**: CLI deployment and automation
- **User Guide**: [USER_GUIDE.md](../USER_GUIDE.md)
- **Installer Guide**: [installer/README.md](../installer/README.md)
- **Local Web Guide**: [LOCAL-DEPLOYMENT-GUIDE.md](LOCAL-DEPLOYMENT-GUIDE.md)
- **Azure Guide**: [AZURE-DEPLOYMENT-GUIDE.md](AZURE-DEPLOYMENT-GUIDE.md)
- **Main README**: [README.md](../README.md)

### Getting Help

- **GitHub Issues**: https://github.com/RegScale/oscal-hub/issues
- **Discussions**: https://github.com/RegScale/oscal-hub/discussions
- **OSCAL Documentation**: https://pages.nist.gov/OSCAL/
- **Gitter Chat**: https://gitter.im/usnistgov-OSCAL/Lobby
- **Email**: oscal@nist.gov

### Common Resources

- **OSCAL Website**: https://pages.nist.gov/OSCAL/
- **OSCAL GitHub**: https://github.com/usnistgov/OSCAL
- **OSCAL Content Examples**: https://github.com/usnistgov/oscal-content
- **Metaschema**: https://github.com/usnistgov/metaschema
- **Java Adoptium**: https://adoptium.net/

---

## Checklist

Before considering your CLI deployment complete:

- [ ] Java 21 (or 11+) installed and verified
- [ ] OSCAL CLI downloaded and extracted
- [ ] PATH configured (or know how to use full path)
- [ ] `oscal-cli --version` displays version info
- [ ] Can validate a sample OSCAL file
- [ ] Can convert between formats
- [ ] Can resolve a profile (if needed)
- [ ] Integration script/CI pipeline created (if automating)
- [ ] Team members trained on CLI usage
- [ ] Documentation bookmarked for reference

---

**🎉 Congratulations!** You now have OSCAL CLI running for automation and batch processing!

For web interface deployment, see: **[LOCAL-DEPLOYMENT-GUIDE.md](LOCAL-DEPLOYMENT-GUIDE.md)**

For production cloud deployment, see: **[AZURE-DEPLOYMENT-GUIDE.md](AZURE-DEPLOYMENT-GUIDE.md)**

---

**Document Version**: 1.0.0
**Last Updated**: 2025-10-26
**Status**: ✅ Complete

---

**End of CLI Deployment Guide**
