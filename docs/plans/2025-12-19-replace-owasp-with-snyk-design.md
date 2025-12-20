# Replace OWASP Dependency Check with Snyk

**Date:** 2025-12-19
**Status:** Approved

## Summary

Replace OWASP Dependency Check Maven plugin with Snyk free tier for security vulnerability scanning. Expands coverage to include Java dependencies, npm dependencies, and Docker images.

## Current State

OWASP Dependency Check is configured in three locations:
- `pom.xml` (root) - plugin management and properties
- `back-end/pom.xml` - plugin configuration
- `.github/workflows/ci.yml` - CI execution step

Current behavior: Fails build on CVSS score >= 8 (high severity).

## Design

### Removals

**Root `pom.xml`:**
- Remove `dependency-check-maven.version` property
- Remove `dependency-check.failBuildOnCVSS` property
- Remove `dependency-check-maven` plugin from `<pluginManagement>`

**`back-end/pom.xml`:**
- Remove `dependency-check-maven` plugin block

**`.github/workflows/ci.yml`:**
- Remove "Run OWASP Dependency Check" step
- Remove "Upload dependency check report" step

**Files to delete:**
- `back-end/dependency-check-output.log`
- `back-end/dependency-check-suppressions.xml` (if exists)

### Additions

**Update existing `security-scan` job in `.github/workflows/ci.yml`:**

Replace OWASP steps with Snyk, keeping Trivy:

```yaml
security-scan:
  name: Security Scan
  runs-on: ubuntu-latest
  needs: build-and-test
  steps:
    - uses: actions/checkout@v4

    # Trivy (existing - keep)
    - name: Run Trivy vulnerability scanner
      uses: aquasecurity/trivy-action@master
      # ... existing config ...

    - name: Upload Trivy results to GitHub Security
      uses: github/codeql-action/upload-sarif@v3
      # ... existing config ...

    # Snyk (new - replaces OWASP)
    - name: Run Snyk for Maven
      uses: snyk/actions/maven@master
      env:
        SNYK_TOKEN: ${{ secrets.SNYK_TOKEN }}
      with:
        args: --severity-threshold=high --fail-on=all

    - name: Run Snyk for Node.js
      uses: snyk/actions/node@master
      env:
        SNYK_TOKEN: ${{ secrets.SNYK_TOKEN }}
      with:
        args: --severity-threshold=high --fail-on=all
      working-directory: front-end

    - name: Run Snyk for Docker
      uses: snyk/actions/docker@master
      env:
        SNYK_TOKEN: ${{ secrets.SNYK_TOKEN }}
      with:
        image: oscal-hub:latest
        args: --severity-threshold=high --file=Dockerfile
      continue-on-error: true
```

### CI Workflow Structure

```
jobs:
  build-and-test:     # Existing job (unchanged)
    - Build Maven
    - Run tests
    - Build frontend
    - Run frontend tests

  security-scan:      # Existing job (updated)
    - Trivy filesystem scan (existing)
    - Upload SARIF to GitHub Security (existing)
    - Snyk Maven scan (new)
    - Snyk Node.js scan (new)
    - Snyk Docker scan (new)
```

Security job runs after build-and-test completes.

### Failure Behavior

| Scan | High/Critical Found | Result |
|------|---------------------|--------|
| Maven | Yes | Workflow fails |
| Node.js | Yes | Workflow fails |
| Docker | Yes | Warning only |

### Required Secrets

Add to GitHub repo settings (Settings → Secrets → Actions):
- `SNYK_TOKEN` - API token from Snyk account settings

## Migration Notes

- No code changes required in application
- Existing OWASP suppressions will not carry over (add `.snyk` file if needed)
- Snyk free tier: 200 tests/month (sufficient for single repo)
