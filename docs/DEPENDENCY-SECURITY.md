# Dependency Security - OWASP Dependency-Check

**Status**: Active
**Date**: 2025-10-26
**Section**: 11 of Security Hardening Plan

## Overview

This document describes the dependency security scanning process for the OSCAL Tools project using **OWASP Dependency-Check**. This tool identifies known vulnerabilities (CVEs) in project dependencies by checking them against the National Vulnerability Database (NVD).

## What is OWASP Dependency-Check?

OWASP Dependency-Check is a Software Composition Analysis (SCA) tool that:
- Scans project dependencies (JAR files, libraries, etc.)
- Identifies dependencies with known vulnerabilities
- Checks vulnerabilities against the NVD (National Vulnerability Database)
- Generates comprehensive reports (HTML, JSON, XML, CSV)
- Can fail builds if high-severity vulnerabilities are found
- Supports suppression files for false positives

## Quick Start

### Run Dependency Scan

```bash
# Scan back-end dependencies
cd back-end
mvn org.owasp:dependency-check-maven:check

# View HTML report
open target/dependency-check-report/dependency-check-report.html
```

### Generate Report Only (Don't Fail Build)

```bash
cd back-end
mvn org.owasp:dependency-check-maven:aggregate -DfailBuildOnCVSS=11
```

## Configuration

### Maven Plugin Configuration

The OWASP Dependency-Check plugin is configured in:
- **Parent POM**: `pom.xml` (pluginManagement section)
- **Back-end POM**: `back-end/pom.xml` (plugins section)

**Key Settings**:

| Setting | Value | Description |
|---------|-------|-------------|
| `failBuildOnCVSS` | `8` | Fail build if CVSS score ≥ 8 (High severity) |
| `format` | `ALL` | Generate all report formats (HTML, JSON, XML, CSV) |
| `skipTestScope` | `false` | Include test dependencies in scan |
| `assemblyAnalyzerEnabled` | `true` | Analyze JAR/ZIP files |
| `jarAnalyzerEnabled` | `true` | Analyze JAR manifests |
| `archiveAnalyzerEnabled` | `true` | Analyze archive files |

### CVSS Severity Levels

CVSS (Common Vulnerability Scoring System) scores range from 0-10:

| Score | Severity | Action |
|-------|----------|--------|
| 0.0 | None | No action needed |
| 0.1 - 3.9 | Low | Monitor, plan to fix |
| 4.0 - 6.9 | Medium | Schedule fix in next release |
| 7.0 - 8.9 | High | **Build fails** - Fix immediately |
| 9.0 - 10.0 | Critical | **Build fails** - Fix urgently |

## NVD API Key (Highly Recommended)

### Why You Need an API Key

Without an NVD API key:
- **First scan takes 30-60 minutes** (downloads 300,000+ vulnerability records)
- Rate-limited to very slow download speeds
- Can timeout or fail

With an NVD API key:
- **First scan takes 5-10 minutes**
- Much faster download speeds
- Reliable updates

### How to Get an NVD API Key

1. **Request API Key**:
   - Go to: https://nvd.nist.gov/developers/request-an-api-key
   - Fill out the form with your email address
   - Check your email for the API key (usually arrives in minutes)

2. **Set Environment Variable**:

   **Mac/Linux**:
   ```bash
   # Add to ~/.zshrc or ~/.bashrc
   export NVD_API_KEY="your-api-key-here"

   # Reload shell configuration
   source ~/.zshrc
   ```

   **Windows PowerShell**:
   ```powershell
   # Set for current session
   $env:NVD_API_KEY = "your-api-key-here"

   # Set permanently
   [System.Environment]::SetEnvironmentVariable('NVD_API_KEY', 'your-api-key-here', 'User')
   ```

3. **Verify Configuration**:
   ```bash
   echo $NVD_API_KEY  # Should print your API key
   ```

The Maven plugin is already configured to use the `NVD_API_KEY` environment variable automatically.

## Understanding Results

### Report Locations

After running the scan, reports are generated in:
```
back-end/target/dependency-check-report/
├── dependency-check-report.html    # Main HTML report (open in browser)
├── dependency-check-report.json    # JSON format for tools
├── dependency-check-report.xml     # XML format
└── dependency-check-report.csv     # CSV format for spreadsheets
```

### Reading the HTML Report

The HTML report includes:

1. **Summary Section**:
   - Total dependencies scanned
   - Dependencies with vulnerabilities
   - Breakdown by severity (Critical, High, Medium, Low)

2. **Dependency Details**:
   - Each vulnerable dependency listed
   - Associated CVE identifiers
   - CVSS scores and severity
   - Description of vulnerability
   - References and links

3. **Evidence Used**:
   - How the tool identified each dependency
   - File paths and locations

### Example Vulnerabilities

**Critical Example** (CVSS 9.8):
```
CVE-2024-12345 in com.example:library:1.0.0
Severity: Critical (CVSS 9.8)
Description: Remote code execution via deserialization
Recommendation: Upgrade to version 1.0.1 or later
```

**High Example** (CVSS 8.1):
```
CVE-2024-67890 in org.springframework:spring-web:5.3.0
Severity: High (CVSS 8.1)
Description: Authentication bypass in Spring Security
Recommendation: Upgrade to version 5.3.30 or later
```

## Fixing Vulnerabilities

### 1. Upgrade Dependencies

**Find Vulnerable Dependency** in `pom.xml`:
```xml
<dependency>
    <groupId>com.example</groupId>
    <artifactId>vulnerable-library</artifactId>
    <version>1.0.0</version>  <!-- Vulnerable version -->
</dependency>
```

**Upgrade to Fixed Version**:
```xml
<dependency>
    <groupId>com.example</groupId>
    <artifactId>vulnerable-library</artifactId>
    <version>1.0.1</version>  <!-- Fixed version -->
</dependency>
```

**Verify Fix**:
```bash
cd back-end
mvn clean install
mvn org.owasp:dependency-check-maven:check
```

### 2. Check for Transitive Dependencies

If the vulnerability is in a transitive dependency (not directly declared):

**Find Which Dependency Brings It In**:
```bash
cd back-end
mvn dependency:tree | grep vulnerable-library
```

**Override Transitive Dependency Version**:
```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>com.example</groupId>
            <artifactId>vulnerable-library</artifactId>
            <version>1.0.1</version>  <!-- Force newer version -->
        </dependency>
    </dependencies>
</dependencyManagement>
```

### 3. Exclude Vulnerable Dependency (If Not Needed)

```xml
<dependency>
    <groupId>com.other</groupId>
    <artifactId>main-library</artifactId>
    <version>2.0.0</version>
    <exclusions>
        <exclusion>
            <groupId>com.example</groupId>
            <artifactId>vulnerable-library</artifactId>
        </exclusion>
    </exclusions>
</dependency>
```

## Suppressing False Positives

Sometimes Dependency-Check reports vulnerabilities that don't apply to your project (false positives). You can suppress these using a suppression file.

### Creating a Suppression File

1. **Create `dependency-check-suppressions.xml`** in `back-end/`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<suppressions xmlns="https://jeremylong.github.io/DependencyCheck/dependency-suppression.1.3.xsd">

    <!-- Example: Suppress specific CVE for a dependency -->
    <suppress>
        <notes>
            This CVE affects server-side usage only. We only use this library
            for client-side operations, so this vulnerability does not apply.
        </notes>
        <packageUrl regex="true">^pkg:maven/com\.example/library@.*$</packageUrl>
        <cve>CVE-2024-12345</cve>
    </suppress>

    <!-- Example: Suppress all CVEs for a specific file -->
    <suppress>
        <notes>
            This is a test-only dependency and not included in production builds.
        </notes>
        <filePath regex="true">.*test-library-1\.0\.0\.jar</filePath>
    </suppress>

    <!-- Example: Suppress based on CPE (Common Platform Enumeration) -->
    <suppress>
        <notes>
            False positive - this CPE doesn't match our actual dependency.
        </notes>
        <cpe>cpe:/a:example:wrong-product</cpe>
    </suppress>

</suppressions>
```

2. **Verify Suppression Works**:
```bash
cd back-end
mvn org.owasp:dependency-check-maven:check
```

The suppressed vulnerabilities will still appear in the report but marked as "suppressed".

### Suppression Best Practices

✅ **Do**:
- Document why each suppression is valid
- Review suppressions regularly (quarterly)
- Remove suppressions when dependencies are upgraded
- Use specific CVE suppressions when possible

❌ **Don't**:
- Suppress all vulnerabilities for convenience
- Suppress without investigation
- Use regex suppressions too broadly
- Forget about suppressed vulnerabilities

## Integration with Build Process

### Maven Lifecycle Integration

The plugin can be bound to Maven lifecycle phases:

**Run on Every Build**:
```xml
<executions>
    <execution>
        <phase>verify</phase>
        <goals>
            <goal>check</goal>
        </goals>
    </execution>
</executions>
```

**Run Manually Only** (current configuration):
```bash
mvn org.owasp:dependency-check-maven:check
```

### CI/CD Integration

#### GitHub Actions

```yaml
name: Security Scan

on:
  push:
    branches: [ main, develop ]
  pull_request:
    branches: [ main ]
  schedule:
    - cron: '0 0 * * 0'  # Weekly on Sunday

jobs:
  dependency-check:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3

      - name: Set up JDK 21
        uses: actions/setup-java@v3
        with:
          java-version: '21'
          distribution: 'temurin'

      - name: Cache NVD database
        uses: actions/cache@v3
        with:
          path: ~/.m2/repository/org/owasp/dependency-check-data
          key: ${{ runner.os }}-nvd-${{ hashFiles('**/pom.xml') }}

      - name: Run Dependency Check
        env:
          NVD_API_KEY: ${{ secrets.NVD_API_KEY }}
        run: |
          cd back-end
          mvn org.owasp:dependency-check-maven:check

      - name: Upload Results
        if: always()
        uses: actions/upload-artifact@v3
        with:
          name: dependency-check-report
          path: back-end/target/dependency-check-report/
```

#### GitLab CI/CD

```yaml
dependency-check:
  stage: test
  image: maven:3.9-eclipse-temurin-21
  variables:
    NVD_API_KEY: $NVD_API_KEY
  cache:
    paths:
      - .m2/repository/org/owasp/dependency-check-data
  script:
    - cd back-end
    - mvn org.owasp:dependency-check-maven:check
  artifacts:
    when: always
    paths:
      - back-end/target/dependency-check-report/
    reports:
      dependency_scanning: back-end/target/dependency-check-report/dependency-check-report.json
```

## Troubleshooting

### Issue: First Scan Takes Too Long

**Symptom**: Scan times out or takes 30+ minutes

**Solution**:
1. Get an NVD API key (see above)
2. Set `NVD_API_KEY` environment variable
3. Run scan overnight if needed on first run
4. Subsequent scans will be much faster (only downloads new vulnerabilities)

### Issue: Build Fails with High CVSS Score

**Symptom**:
```
[ERROR] Dependency-check failed:
One or more dependencies were identified with vulnerabilities that have a CVSS score greater than or equal to '8.0'
```

**Solution**:
1. Review the HTML report: `back-end/target/dependency-check-report/dependency-check-report.html`
2. Identify vulnerable dependencies
3. Upgrade to fixed versions
4. Re-run scan to verify fix

### Issue: False Positive CVE

**Symptom**: CVE reported but doesn't apply to your usage

**Solution**:
1. Verify it's actually a false positive (check CVE details)
2. Create suppression entry in `dependency-check-suppressions.xml`
3. Document why it's a false positive
4. Re-run scan to verify suppression works

### Issue: Database Update Fails

**Symptom**:
```
[ERROR] Unable to update NVD database
```

**Solution**:
1. Check internet connectivity
2. Check if NVD API is down: https://nvd.nist.gov/
3. Clear cache and retry:
   ```bash
   rm -rf ~/.m2/repository/org/owasp/dependency-check-data
   mvn org.owasp:dependency-check-maven:check
   ```
4. Wait a few hours and retry (NVD can have temporary outages)

### Issue: Out of Memory Error

**Symptom**:
```
[ERROR] Java heap space
```

**Solution**:
```bash
export MAVEN_OPTS="-Xmx2g"
mvn org.owasp:dependency-check-maven:check
```

## Maintenance

### Weekly Tasks
- [ ] Review new vulnerabilities in dependencies
- [ ] Check if any critical/high vulnerabilities need immediate attention

### Monthly Tasks
- [ ] Update NVD database manually (automatic, but verify)
- [ ] Review suppression file for outdated entries
- [ ] Check for dependency updates that fix vulnerabilities

### Quarterly Tasks
- [ ] Full dependency audit
- [ ] Review all suppressed vulnerabilities (still valid?)
- [ ] Update CVSS threshold if needed
- [ ] Review and update this documentation

## Commands Reference

```bash
# Run dependency check (fail on CVSS >= 8)
mvn org.owasp:dependency-check-maven:check

# Run dependency check (generate report only, don't fail)
mvn org.owasp:dependency-check-maven:aggregate -DfailBuildOnCVSS=11

# Update NVD database only (no scan)
mvn org.owasp:dependency-check-maven:update-only

# Purge NVD database and re-download
mvn org.owasp:dependency-check-maven:purge

# Generate report with custom output directory
mvn org.owasp:dependency-check-maven:check \
    -DoutputDirectory=/path/to/custom/location

# Skip test dependencies
mvn org.owasp:dependency-check-maven:check -DskipTestScope=true

# Set custom CVSS threshold
mvn org.owasp:dependency-check-maven:check -DfailBuildOnCVSS=7

# Use custom suppression file
mvn org.owasp:dependency-check-maven:check \
    -DsuppressionFile=/path/to/custom-suppressions.xml
```

## Resources

- **OWASP Dependency-Check**: https://owasp.org/www-project-dependency-check/
- **Maven Plugin Documentation**: https://jeremylong.github.io/DependencyCheck/dependency-check-maven/
- **NVD API Key Request**: https://nvd.nist.gov/developers/request-an-api-key
- **CVE Database**: https://cve.mitre.org/
- **CVSS Calculator**: https://nvd.nist.gov/vuln-metrics/cvss/v3-calculator
- **Suppression File Schema**: https://jeremylong.github.io/DependencyCheck/general/suppression.html

## Security Contacts

For security vulnerabilities in OSCAL Tools itself:
- Create a security advisory: https://github.com/RegScale/oscal-hub/security/advisories
- Email: NIST OSCAL Team (see README for contact)

For questions about dependency vulnerabilities:
- Check the CVE details at: https://nvd.nist.gov/
- Contact the upstream library maintainers
- Check library's security policy/advisories

---

**Last Updated**: 2025-10-26
**Maintainer**: OSCAL Tools Security Team
