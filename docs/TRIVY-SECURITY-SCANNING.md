# Trivy Security Scanning Guide

**Date**: 2025-10-26
**Status**: ✅ **Active** - Trivy v0.48.0 installed and configured
**Replaces**: OWASP Dependency-Check (due to NVD timeout issues)

---

## Table of Contents

1. [Overview](#overview)
2. [Why Trivy?](#why-trivy)
3. [Installation](#installation)
4. [Quick Start](#quick-start)
5. [Using the Trivy Scan Script](#using-the-trivy-scan-script)
6. [Manual Trivy Commands](#manual-trivy-commands)
7. [Understanding Scan Results](#understanding-scan-results)
8. [SBOM Generation](#sbom-generation)
9. [Vulnerability Remediation](#vulnerability-remediation)
10. [CI/CD Integration](#cicd-integration)
11. [Troubleshooting](#troubleshooting)
12. [Comparison with OWASP Dependency-Check](#comparison-with-owasp-dependency-check)

---

## Overview

**Trivy** (pronounced "tree-vee") is a comprehensive security scanner by Aqua Security that:

- 🔍 **Scans for vulnerabilities** in dependencies, OS packages, and container images
- 📋 **Generates SBOMs** (Software Bill of Materials) in CycloneDX and SPDX formats
- 🚀 **Runs fast** - typically completes in seconds vs minutes/hours for other tools
- 🌐 **No API keys required** - works out of the box
- 🐳 **Container-aware** - can scan Docker images and Kubernetes manifests
- 💻 **Multi-language** - supports Java, Go, Python, Node.js, Ruby, and more

### What We Use Trivy For

In the OSCAL Tools project, we use Trivy for:

1. **Dependency vulnerability scanning** - Find known CVEs in Maven dependencies
2. **SBOM generation** - Create comprehensive software component inventories
3. **Container scanning** - Scan Docker images for vulnerabilities
4. **Security compliance** - Meet security requirements for NIST software

---

## Why Trivy?

We switched from OWASP Dependency-Check to Trivy because:

| Feature | OWASP Dependency-Check | Trivy |
|---------|------------------------|-------|
| **Speed** | 30-60 min (with API key)<br>2-4 hours (without) | 10-30 seconds |
| **API Key** | Required for reasonable performance | Not required |
| **NVD Database** | Must download 315k+ CVEs locally | Uses cloud database |
| **Container Scanning** | No | Yes |
| **SBOM Generation** | Limited | Full support (CycloneDX, SPDX) |
| **Secret Detection** | No | Yes |
| **Kubernetes Scanning** | No | Yes |
| **Maintenance** | High (database updates) | Low (automatic updates) |

**Bottom line**: Trivy is faster, easier to use, and more comprehensive.

---

## Installation

### Option 1: Automatic Installation (Recommended)

The `trivy-scan.sh` script will automatically install Trivy if it's not found:

```bash
./trivy-scan.sh
```

This downloads Trivy v0.48.0 to `./tools/trivy`.

### Option 2: Manual Installation

#### macOS (Homebrew)

```bash
brew install aquasecurity/trivy/trivy
```

#### macOS/Linux (Direct Download)

```bash
# Create tools directory
mkdir -p tools

# Download and install Trivy v0.48.0
curl -sfL https://raw.githubusercontent.com/aquasecurity/trivy/main/contrib/install.sh | sh -s -- -b ./tools v0.48.0

# Verify installation
./tools/trivy --version
```

#### Linux (apt - Debian/Ubuntu)

```bash
sudo apt-get install wget apt-transport-https gnupg lsb-release
wget -qO - https://aquasecurity.github.io/trivy-repo/deb/public.key | sudo apt-key add -
echo "deb https://aquasecurity.github.io/trivy-repo/deb $(lsb_release -sc) main" | sudo tee -a /etc/apt/sources.list.d/trivy.list
sudo apt-get update
sudo apt-get install trivy
```

#### Windows (PowerShell)

```powershell
# Using Chocolatey
choco install trivy

# Or using Scoop
scoop install trivy
```

---

## Quick Start

### 1. Scan for Vulnerabilities

```bash
# Scan back-end for HIGH and CRITICAL vulnerabilities
./trivy-scan.sh
```

**Expected output**:
```
============================================
  Trivy Security Scanner
============================================

[INFO] Using Version: 0.48.0
[INFO] Starting filesystem vulnerability scan...
[INFO] Target: ./back-end
[INFO] Severity: HIGH,CRITICAL

[SUCCESS] Filesystem scan complete
[INFO] Reports saved to: ./security-reports/
```

### 2. Generate SBOM

```bash
# Scan + generate SBOM
./trivy-scan.sh --sbom
```

**Output files**:
- `security-reports/sbom-cyclonedx.json` - CycloneDX format (161 components)
- `security-reports/sbom-spdx.json` - SPDX format (162 packages)
- `security-reports/vulnerability-report-fs.json` - Vulnerability details

### 3. Scan Docker Image

```bash
# Build your Docker image first
docker-compose build

# Scan the image
./trivy-scan.sh --type image --image oscal-tools:latest --sbom
```

---

## Using the Trivy Scan Script

### Script Location

```
oscal-cli/
└── trivy-scan.sh          # Main security scanning script
```

### Available Options

```bash
./trivy-scan.sh [options]

Options:
  --type [fs|image|all]     Scan type (default: fs)
                            fs    = Scan filesystem
                            image = Scan Docker image
                            all   = Scan both

  --severity [LEVEL]        Severity levels to report (default: HIGH,CRITICAL)
                            Options: UNKNOWN,LOW,MEDIUM,HIGH,CRITICAL

  --output-dir [DIR]        Output directory for reports (default: ./security-reports)

  --sbom                    Generate SBOM in CycloneDX and SPDX formats

  --image [NAME]            Docker image name to scan (required for image scans)

  --help                    Show help message
```

### Common Usage Examples

#### Example 1: Basic Vulnerability Scan

```bash
./trivy-scan.sh
```

**What it does**:
- Scans `./back-end` directory
- Reports HIGH and CRITICAL vulnerabilities only
- Outputs to `./security-reports/`

#### Example 2: Include MEDIUM Severity

```bash
./trivy-scan.sh --severity MEDIUM,HIGH,CRITICAL
```

**Use case**: Weekly security review to catch upcoming issues early.

#### Example 3: Full Security Report with SBOM

```bash
./trivy-scan.sh --sbom --output-dir ./reports/$(date +%Y-%m-%d)
```

**What it does**:
- Scans for vulnerabilities
- Generates SBOM files
- Saves to dated directory (e.g., `./reports/2025-10-26/`)

#### Example 4: Scan Docker Image

```bash
# Build image first
docker-compose build

# Scan it
./trivy-scan.sh --type image --image oscal-tools:latest
```

**What it does**:
- Scans the Docker image for OS and application vulnerabilities
- Checks base image (Eclipse Temurin) and application layers
- Reports on outdated packages

#### Example 5: Complete Security Audit

```bash
./trivy-scan.sh --type all --sbom --image oscal-tools:latest
```

**What it does**:
- Scans filesystem (back-end dependencies)
- Scans Docker image
- Generates SBOMs for both
- Creates comprehensive security report

#### Example 6: CI/CD Integration

```bash
# In your CI/CD pipeline
./trivy-scan.sh \
  --severity HIGH,CRITICAL \
  --output-dir ./security-reports \
  --sbom

# Check exit code (0 = no vulnerabilities, 1 = vulnerabilities found)
if [ $? -ne 0 ]; then
  echo "Security vulnerabilities found! Check reports."
  exit 1
fi
```

---

## Manual Trivy Commands

If you prefer to run Trivy directly (without the script):

### Filesystem Scan

```bash
# Basic scan
./tools/trivy fs ./back-end

# HIGH and CRITICAL only
./tools/trivy fs --severity HIGH,CRITICAL ./back-end

# JSON output
./tools/trivy fs --format json --output report.json ./back-end

# Table format (human-readable)
./tools/trivy fs --format table ./back-end
```

### Docker Image Scan

```bash
# Scan an image
./tools/trivy image oscal-tools:latest

# Include specific severities
./tools/trivy image --severity HIGH,CRITICAL oscal-tools:latest

# JSON report
./tools/trivy image --format json --output image-report.json oscal-tools:latest
```

### SBOM Generation

```bash
# CycloneDX format
./tools/trivy fs --format cyclonedx --output sbom.json ./back-end

# SPDX format
./tools/trivy fs --format spdx-json --output sbom-spdx.json ./back-end

# Include vulnerabilities in SBOM
./tools/trivy fs --format cyclonedx --scanners vuln --output sbom-with-vulns.json ./back-end
```

### Scan Specific File

```bash
# Scan a specific pom.xml
./tools/trivy fs ./back-end/pom.xml

# Scan package-lock.json
./tools/trivy fs ./front-end/package-lock.json
```

---

## Understanding Scan Results

### Severity Levels

Trivy uses the same CVSS scoring as NVD:

| Severity | CVSS Score | Description | Action |
|----------|------------|-------------|--------|
| **CRITICAL** | 9.0-10.0 | Actively exploited, RCE, data breach | **Fix immediately** |
| **HIGH** | 7.0-8.9 | Privilege escalation, DoS | **Fix within 7 days** |
| **MEDIUM** | 4.0-6.9 | Information disclosure | **Fix within 30 days** |
| **LOW** | 0.1-3.9 | Minor issues | **Fix during maintenance** |
| **UNKNOWN** | N/A | No CVSS score available | **Review case-by-case** |

### Sample Output

When vulnerabilities are found, Trivy displays a table:

```
pom.xml (pom)
Total: 1 (HIGH: 1, CRITICAL: 0)

┌───────────────┬───────────────┬──────────┬────────┬───────────────────┬───────────────┬──────────────────────────┐
│    Library    │ Vulnerability │ Severity │ Status │ Installed Version │ Fixed Version │          Title           │
├───────────────┼───────────────┼──────────┼────────┼───────────────────┼───────────────┼──────────────────────────┤
│ org.json:json │ CVE-2023-5072 │ HIGH     │ fixed  │ 20230227          │ 20231013      │ JSON-java: parser OOM    │
└───────────────┴───────────────┴──────────┴────────┴───────────────────┴───────────────┴──────────────────────────┘
```

**Columns explained**:
- **Library**: Maven artifact ID
- **Vulnerability**: CVE identifier
- **Severity**: Risk level (CRITICAL, HIGH, MEDIUM, LOW)
- **Status**: `fixed` = patch available, `affected` = no fix yet
- **Installed Version**: Current version in your pom.xml
- **Fixed Version**: Version that fixes the vulnerability
- **Title**: Brief description of the issue

### No Vulnerabilities Found

If no vulnerabilities are found (our current state), you'll see:

```
2025-10-26T15:52:10.959-0400 [INFO] Number of language-specific files: 1
2025-10-26T15:52:10.959-0400 [INFO] Detecting pom vulnerabilities...
```

**No table = no vulnerabilities!** ✅

---

## SBOM Generation

### What is an SBOM?

A **Software Bill of Materials (SBOM)** is a complete inventory of all components in your software, similar to an ingredients list on food packaging.

**Why SBOMs matter**:
- 📋 **Transparency** - Know exactly what's in your software
- 🔒 **Security** - Track vulnerable components across projects
- ✅ **Compliance** - Meet federal requirements (Executive Order 14028)
- 🚨 **Incident Response** - Quickly identify if you're affected by new CVEs

### Supported Formats

Trivy generates SBOMs in industry-standard formats:

#### CycloneDX (Recommended)

```bash
./tools/trivy fs --format cyclonedx --output sbom-cyclonedx.json ./back-end
```

**Advantages**:
- Designed specifically for security use cases
- Includes vulnerability data
- Supports vulnerability exploitability exchange (VEX)
- Widely supported by security tools

**Sample structure**:
```json
{
  "bomFormat": "CycloneDX",
  "specVersion": "1.4",
  "version": 1,
  "metadata": {
    "component": {
      "type": "application",
      "name": "back-end"
    }
  },
  "components": [
    {
      "type": "library",
      "group": "org.springframework.boot",
      "name": "spring-boot-starter-web",
      "version": "3.4.10",
      "licenses": [{"license": {"id": "Apache-2.0"}}]
    }
    // ... 160 more components
  ]
}
```

#### SPDX (Software Package Data Exchange)

```bash
./tools/trivy fs --format spdx-json --output sbom-spdx.json ./back-end
```

**Advantages**:
- ISO/IEC 5962:2021 international standard
- Strong licensing focus
- Broad industry adoption
- Linux Foundation supported

**Sample structure**:
```json
{
  "SPDXID": "SPDXRef-DOCUMENT",
  "spdxVersion": "SPDX-2.3",
  "name": "back-end",
  "packages": [
    {
      "SPDXID": "SPDXRef-Package-spring-boot-starter-web",
      "name": "spring-boot-starter-web",
      "versionInfo": "3.4.10",
      "licenseConcluded": "Apache-2.0"
    }
    // ... 161 more packages
  ]
}
```

### SBOM Contents

Our generated SBOMs include:

**For the back-end** (161-162 components):

1. **Direct dependencies** (from pom.xml):
   - Spring Boot (3.4.10)
   - PostgreSQL driver
   - JWT libraries (jjwt)
   - OSCAL libraries (liboscal-java)
   - Jackson (JSON/XML/YAML)
   - Bucket4j (rate limiting)
   - Lombok
   - Security libraries

2. **Transitive dependencies**:
   - Spring Framework modules
   - Hibernate
   - Tomcat embedded
   - HikariCP
   - And 100+ more...

3. **Metadata for each component**:
   - Group ID, Artifact ID, Version
   - License information
   - Package URL (PURL)
   - Hash/checksum (for verification)

### Using SBOMs

#### 1. Share with Security Teams

```bash
# Generate SBOM
./trivy-scan.sh --sbom

# Share the files
# - security-reports/sbom-cyclonedx.json
# - security-reports/sbom-spdx.json
```

Send to your security or compliance team for review.

#### 2. Track Vulnerabilities

When a new CVE is announced (e.g., Log4Shell), you can quickly check if you're affected:

```bash
# Check if component exists in SBOM
jq '.components[] | select(.name == "log4j-core")' security-reports/sbom-cyclonedx.json
```

#### 3. License Compliance

```bash
# List all licenses
jq -r '.components[].licenses[]?.license.id' security-reports/sbom-cyclonedx.json | sort -u

# Find components with specific license
jq '.components[] | select(.licenses[]?.license.id == "GPL-3.0")' security-reports/sbom-cyclonedx.json
```

#### 4. Dependency Analysis

```bash
# Count components
jq '.components | length' security-reports/sbom-cyclonedx.json

# List all component names and versions
jq -r '.components[] | "\(.name):\(.version)"' security-reports/sbom-cyclonedx.json

# Find Spring components
jq '.components[] | select(.group == "org.springframework")' security-reports/sbom-cyclonedx.json
```

#### 5. Continuous Monitoring

```bash
# Re-scan SBOM for new vulnerabilities (without rebuilding)
./tools/trivy sbom security-reports/sbom-cyclonedx.json
```

This scans the SBOM file itself for newly discovered vulnerabilities!

---

## Vulnerability Remediation

### Step-by-Step Fix Process

When Trivy finds a vulnerability:

#### 1. Identify the Dependency

Example from our scan:
```
org.json:json | CVE-2023-5072 | HIGH | 20230227 → 20231013
```

#### 2. Determine if It's Direct or Transitive

```bash
# Check pom.xml
grep -r "org.json" back-end/pom.xml

# If not found, check dependency tree
cd back-end && mvn dependency:tree | grep org.json
```

**Result**:
```
[INFO] |  +- com.github.erosb:everit-json-schema:jar:1.14.2:compile
[INFO] |  |  +- org.json:json:jar:20230227:compile
```

It's a **transitive dependency** from `everit-json-schema`.

#### 3. Override the Version

For transitive dependencies, add an explicit declaration in `back-end/pom.xml`:

```xml
<dependencies>
    <!-- Existing dependencies... -->

    <!-- org.json - Override transitive dependency to fix CVE-2023-5072 -->
    <dependency>
        <groupId>org.json</groupId>
        <artifactId>json</artifactId>
        <version>20231013</version>
    </dependency>
</dependencies>
```

For direct dependencies, update the version directly:

```xml
<dependency>
    <groupId>com.example</groupId>
    <artifactId>vulnerable-library</artifactId>
    <version>2.0.0</version>  <!-- Updated from 1.5.0 -->
</dependency>
```

#### 4. Verify the Fix

```bash
# Re-run Trivy scan
./trivy-scan.sh

# Or manual check
./tools/trivy fs --severity HIGH,CRITICAL ./back-end
```

**Expected**: No vulnerabilities found! ✅

#### 5. Test the Application

```bash
# Rebuild
cd back-end && mvn clean install

# Run tests
mvn test

# Start application
./dev.sh
```

Ensure everything still works after the update.

#### 6. Document the Change

In your commit message:
```
fix: Upgrade org.json to 20231013 to fix CVE-2023-5072

- CVE-2023-5072: JSON-java parser confusion leads to OOM
- Severity: HIGH
- Fixed by overriding transitive dependency from everit-json-schema
- Verified with Trivy scan: 0 HIGH/CRITICAL vulnerabilities

🤖 Generated with Claude Code
Co-Authored-By: Claude <noreply@anthropic.com>
```

### Special Cases

#### Case 1: No Fix Available

If Trivy shows `Status: affected` (no fix available):

1. **Check upstream**: Visit the library's GitHub/security page
2. **Evaluate risk**: Does the vulnerability affect your usage?
3. **Mitigate**: Add firewall rules, input validation, etc.
4. **Suppress** (if acceptable risk):

Create `trivy-suppressions.yaml`:
```yaml
vulnerabilities:
  - id: CVE-2024-XXXXX
    paths:
      - "pom.xml"
    statement: "Vulnerability does not affect our usage - we don't use the vulnerable method"
    expiry: "2025-12-31"
```

Run with suppressions:
```bash
./tools/trivy fs --severity HIGH,CRITICAL --ignore-unfixed --ignorefile trivy-suppressions.yaml ./back-end
```

#### Case 2: Breaking Changes in Fix

If upgrading causes compile/test failures:

1. **Review release notes** for the fixed version
2. **Check for migration guides**
3. **Update calling code** to match new API
4. **Run full test suite**
5. **Consider alternatives** if upgrade is too complex

#### Case 3: Multiple Fixes Needed

When Trivy finds 10+ vulnerabilities:

```bash
# Get a summary
./tools/trivy fs --severity HIGH,CRITICAL --format json ./back-end | jq -r '.Results[].Vulnerabilities[] | "\(.PkgName):\(.InstalledVersion) → \(.FixedVersion)"'

# Fix one at a time, starting with CRITICAL
# Test after each fix
# Commit individually for easier rollback
```

---

## CI/CD Integration

### GitHub Actions

Create `.github/workflows/security-scan.yml`:

```yaml
name: Security Scan

on:
  push:
    branches: [ main, develop ]
  pull_request:
    branches: [ main ]
  schedule:
    # Run weekly on Mondays at 9 AM UTC
    - cron: '0 9 * * 1'

jobs:
  trivy-scan:
    runs-on: ubuntu-latest

    steps:
      - name: Checkout code
        uses: actions/checkout@v4

      - name: Run Trivy vulnerability scanner
        uses: aquasecurity/trivy-action@master
        with:
          scan-type: 'fs'
          scan-ref: './back-end'
          severity: 'HIGH,CRITICAL'
          format: 'sarif'
          output: 'trivy-results.sarif'

      - name: Upload Trivy results to GitHub Security
        uses: github/codeql-action/upload-sarif@v3
        with:
          sarif_file: 'trivy-results.sarif'

      - name: Generate SBOM
        uses: aquasecurity/trivy-action@master
        with:
          scan-type: 'fs'
          scan-ref: './back-end'
          format: 'cyclonedx'
          output: 'sbom.json'

      - name: Upload SBOM as artifact
        uses: actions/upload-artifact@v4
        with:
          name: sbom
          path: sbom.json

      - name: Fail on HIGH/CRITICAL vulnerabilities
        uses: aquasecurity/trivy-action@master
        with:
          scan-type: 'fs'
          scan-ref: './back-end'
          severity: 'HIGH,CRITICAL'
          exit-code: '1'  # Fail build if vulnerabilities found
```

### GitLab CI/CD

Create `.gitlab-ci.yml`:

```yaml
stages:
  - security

trivy-scan:
  stage: security
  image:
    name: aquasec/trivy:latest
    entrypoint: [""]
  script:
    # Filesystem scan
    - trivy fs --severity HIGH,CRITICAL --format table ./back-end

    # Generate SBOM
    - trivy fs --format cyclonedx --output sbom.json ./back-end

    # Generate JSON report for GitLab
    - trivy fs --severity HIGH,CRITICAL --format json --output gl-dependency-scanning-report.json ./back-end

  artifacts:
    reports:
      dependency_scanning: gl-dependency-scanning-report.json
    paths:
      - sbom.json
    expire_in: 1 week

  rules:
    - if: '$CI_COMMIT_BRANCH == "main"'
    - if: '$CI_PIPELINE_SOURCE == "merge_request_event"'
    - if: '$CI_PIPELINE_SOURCE == "schedule"'  # Weekly scan
```

### Jenkins

Create `Jenkinsfile`:

```groovy
pipeline {
    agent any

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Install Trivy') {
            steps {
                sh '''
                    curl -sfL https://raw.githubusercontent.com/aquasecurity/trivy/main/contrib/install.sh | sh -s -- -b ./tools v0.48.0
                '''
            }
        }

        stage('Security Scan') {
            steps {
                sh '''
                    ./trivy-scan.sh --severity HIGH,CRITICAL --output-dir ./security-reports
                '''
            }
        }

        stage('Generate SBOM') {
            steps {
                sh '''
                    ./trivy-scan.sh --sbom --output-dir ./security-reports
                '''
            }
        }

        stage('Archive Reports') {
            steps {
                archiveArtifacts artifacts: 'security-reports/*', fingerprint: true
            }
        }

        stage('Check Results') {
            steps {
                script {
                    def vulnCount = sh(
                        script: './tools/trivy fs --severity HIGH,CRITICAL --format json ./back-end | jq \'.Results[].Vulnerabilities | length\'',
                        returnStdout: true
                    ).trim().toInteger()

                    if (vulnCount > 0) {
                        error("Found ${vulnCount} HIGH/CRITICAL vulnerabilities!")
                    }
                }
            }
        }
    }

    post {
        always {
            publishHTML([
                reportDir: 'security-reports',
                reportFiles: 'vulnerability-report-fs.html',
                reportName: 'Trivy Security Report'
            ])
        }
    }
}
```

### Azure Pipelines

Create `azure-pipelines.yml`:

```yaml
trigger:
  - main
  - develop

schedules:
- cron: "0 9 * * 1"  # Weekly on Mondays at 9 AM
  displayName: Weekly security scan
  branches:
    include:
    - main

pool:
  vmImage: 'ubuntu-latest'

steps:
- task: Bash@3
  displayName: 'Install Trivy'
  inputs:
    targetType: 'inline'
    script: |
      curl -sfL https://raw.githubusercontent.com/aquasecurity/trivy/main/contrib/install.sh | sh -s -- -b $(Agent.ToolsDirectory)/trivy v0.48.0
      echo "##vso[task.prependpath]$(Agent.ToolsDirectory)/trivy"

- task: Bash@3
  displayName: 'Run Trivy Scan'
  inputs:
    targetType: 'inline'
    script: |
      trivy fs --severity HIGH,CRITICAL --format table ./back-end

- task: Bash@3
  displayName: 'Generate SBOM'
  inputs:
    targetType: 'inline'
    script: |
      mkdir -p $(Build.ArtifactStagingDirectory)/security
      trivy fs --format cyclonedx --output $(Build.ArtifactStagingDirectory)/security/sbom.json ./back-end

- task: PublishBuildArtifacts@1
  displayName: 'Publish Security Reports'
  inputs:
    pathToPublish: '$(Build.ArtifactStagingDirectory)/security'
    artifactName: 'security-reports'
```

---

## Troubleshooting

### Issue 1: "trivy: command not found"

**Cause**: Trivy not installed or not in PATH

**Fix**:

```bash
# Option 1: Let the script install it
./trivy-scan.sh

# Option 2: Manual install
curl -sfL https://raw.githubusercontent.com/aquasecurity/trivy/main/contrib/install.sh | sh -s -- -b ./tools v0.48.0

# Option 3: Use full path
./tools/trivy fs ./back-end
```

### Issue 2: "Unable to marshal SPDX licenses" Warning

**Cause**: Some dependency licenses use non-standard identifiers (e.g., "The MIT License (MIT)" instead of "MIT")

**Impact**: Low - SBOM is still generated, just with warnings

**Fix**: These are warnings only, can be safely ignored. The SBOM file is complete.

```bash
# Suppress warnings (if desired)
./tools/trivy fs --format spdx-json --output sbom.json ./back-end 2>/dev/null
```

### Issue 3: "Error: No such image"

**Cause**: Trying to scan a Docker image that doesn't exist locally

**Fix**:

```bash
# Build the image first
docker-compose build

# Verify image exists
docker images | grep oscal-tools

# Then scan
./trivy-scan.sh --type image --image oscal-tools:latest
```

### Issue 4: Scan is Very Slow

**Cause**: Trivy is downloading vulnerability database on first run

**Fix**:

```bash
# Wait for initial database download (one-time, ~100 MB)
# Subsequent scans will be fast

# Check download progress
./tools/trivy fs --debug ./back-end 2>&1 | grep -i download

# Pre-download database
./tools/trivy image --download-db-only
```

### Issue 5: False Positives

**Cause**: Vulnerability doesn't actually affect your code usage

**Fix**: Create a suppression file

```yaml
# Create trivy-suppressions.yaml
vulnerabilities:
  - id: CVE-2024-XXXXX
    paths:
      - "pom.xml"
    statement: "This vulnerability affects the HTTP client, but we only use the JSON parsing functionality"
    expiry: "2025-12-31"
```

```bash
# Scan with suppressions
./tools/trivy fs --severity HIGH,CRITICAL --ignorefile trivy-suppressions.yaml ./back-end
```

### Issue 6: Database Update Errors

**Cause**: Network issues or firewall blocking Trivy database updates

**Fix**:

```bash
# Check network connectivity
curl -I https://ghcr.io

# Use offline mode (if you have a cached database)
./tools/trivy fs --offline-scan ./back-end

# Clear cache and re-download
./tools/trivy image --clear-cache
./tools/trivy image --download-db-only
```

### Issue 7: Scan Exits with Code 1

**Cause**: Vulnerabilities were found (this is expected behavior)

**Fix**:

```bash
# If you want script to continue despite vulnerabilities
./tools/trivy fs --severity HIGH,CRITICAL --exit-code 0 ./back-end

# Or check and handle
./tools/trivy fs --severity HIGH,CRITICAL ./back-end
if [ $? -eq 1 ]; then
  echo "Vulnerabilities found - review reports"
  # Don't exit, just warn
fi
```

---

## Comparison with OWASP Dependency-Check

We previously used OWASP Dependency-Check but switched to Trivy. Here's why:

| Feature | OWASP Dependency-Check | Trivy |
|---------|------------------------|-------|
| **Initial scan time** | 40+ minutes (timed out) | ~13 seconds |
| **Subsequent scans** | 5-10 min (with API key) | ~10 seconds |
| **NVD API key** | Required for reasonable performance | Not needed |
| **Database size** | ~1-2 GB local storage | Cloud-based (minimal local storage) |
| **Database updates** | Manual (slow, can timeout) | Automatic (fast) |
| **Maven support** | Excellent | Excellent |
| **Container scanning** | No | Yes |
| **Kubernetes scanning** | No | Yes |
| **Secret detection** | No | Yes (built-in) |
| **SBOM generation** | Limited | Full (CycloneDX, SPDX) |
| **License scanning** | No | Yes |
| **Misconfiguration detection** | No | Yes (IaC scanning) |
| **False positive rate** | Moderate | Low |
| **Maintenance effort** | High | Low |
| **Memory usage** | High (1-2 GB) | Low (~200 MB) |
| **Output formats** | HTML, JSON, XML, CSV | HTML, JSON, SARIF, CycloneDX, SPDX, Table |
| **CI/CD integration** | Manual setup | Native GitHub Action |
| **Offline mode** | Difficult | Built-in |

### When to Use OWASP Dependency-Check

Despite our switch to Trivy, OWASP Dependency-Check may still be preferred if:

1. **You already have it set up** with an NVD API key
2. **Compliance requires it** (some organizations mandate specific tools)
3. **You need .NET support** (better .NET ecosystem support)
4. **You prefer Java ecosystem** tools

### Migration from OWASP to Trivy

If you're migrating:

```bash
# 1. Keep both tools temporarily
# OWASP config remains in pom.xml
# Trivy scan runs via script

# 2. Compare results
mvn org.owasp:dependency-check-maven:check  # If you have NVD API key
./trivy-scan.sh

# 3. Verify Trivy catches everything
# Review both reports side-by-side

# 4. Once confident, remove OWASP plugin from pom.xml
```

Our experience:
- ✅ **Trivy found the same CVE-2023-5072** that OWASP would have found (if it had completed)
- ✅ **Trivy completed in 13 seconds** vs 40+ minutes for OWASP
- ✅ **Trivy generated better SBOMs** in standard formats
- ✅ **No NVD API key needed** - works out of the box

---

## Summary

### Current Security Status

**✅ 0 HIGH/CRITICAL vulnerabilities**

- Fixed CVE-2023-5072 by upgrading org.json from 20230227 to 20231013
- All dependencies scanned and clean
- SBOMs generated with 161-162 components

### Quick Reference Commands

```bash
# Daily: Quick vulnerability scan
./trivy-scan.sh

# Weekly: Full scan with SBOM
./trivy-scan.sh --sbom --severity MEDIUM,HIGH,CRITICAL

# Before deployment: Complete audit
./trivy-scan.sh --type all --sbom --image oscal-tools:latest

# CI/CD: Fail on vulnerabilities
./tools/trivy fs --severity HIGH,CRITICAL --exit-code 1 ./back-end
```

### Files Generated

```
oscal-cli/
├── trivy-scan.sh                          # Main scan script
├── tools/
│   └── trivy                              # Trivy v0.48.0 binary
└── security-reports/                      # Scan output
    ├── sbom-cyclonedx.json               # CycloneDX SBOM (161 components)
    ├── sbom-spdx.json                     # SPDX SBOM (162 packages)
    ├── vulnerability-report-fs.json       # Detailed vulnerability report
    └── vulnerability-report-fs.html       # HTML report (if supported)
```

### Next Steps

1. **Integrate into CI/CD**
   - Add GitHub Action (see [CI/CD Integration](#cicd-integration))
   - Run on every PR and weekly schedule

2. **Regular Scanning**
   - Run daily: `./trivy-scan.sh`
   - Run weekly with SBOM: `./trivy-scan.sh --sbom`
   - Review and fix any new vulnerabilities within 7 days

3. **SBOM Distribution**
   - Share SBOMs with security team
   - Include in release artifacts
   - Publish to dependency tracking systems

4. **Container Scanning**
   - Build Docker image: `docker-compose build`
   - Scan image: `./trivy-scan.sh --type image --image oscal-tools:latest`
   - Fix base image vulnerabilities

---

## Additional Resources

- **Trivy Documentation**: https://aquasecurity.github.io/trivy/
- **Trivy GitHub**: https://github.com/aquasecurity/trivy
- **CycloneDX Specification**: https://cyclonedx.org/
- **SPDX Specification**: https://spdx.dev/
- **NVD Database**: https://nvd.nist.gov/
- **CVE Details**: https://cve.mitre.org/
- **CVSS Calculator**: https://www.first.org/cvss/calculator/3.1

---

**Last Updated**: 2025-10-26
**Tool Version**: Trivy v0.48.0
**Scan Status**: ✅ Clean (0 HIGH/CRITICAL vulnerabilities)
**SBOM Status**: ✅ Generated (161-162 components)
