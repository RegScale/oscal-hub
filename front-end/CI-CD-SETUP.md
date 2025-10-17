# CI/CD Pipeline Documentation

This document describes the comprehensive CI/CD pipeline for the OSCAL UX frontend application.

## Overview

The CI/CD pipeline automatically:
1. Runs unit and E2E tests
2. Builds a Docker container
3. Posts test results as PR comments
4. Performs AI-powered code review with Claude
5. Deploys to Docker Hub (on main branch)

## Pipeline Workflow

### Trigger Events

The pipeline runs on:
- **Pull Requests** to `main` or `develop` branches
- **Pushes** to `main` or `develop` branches
- Only when files in `front-end/ui/` are modified

### Pipeline Jobs

#### 1. Test Job
**Purpose**: Run all automated tests

**Steps**:
- Install Node.js dependencies
- Run Vitest unit tests
- Install Playwright browsers
- Run Playwright E2E tests
- Upload test results as artifacts

**Outputs**:
- Test status (success/failure)
- Test result artifacts (retained for 30 days)
- Playwright HTML reports

#### 2. Build Job
**Purpose**: Build and validate Docker container

**Steps**:
- Set up Docker Buildx for multi-platform builds
- Build Docker image using Dockerfile
- Test the container by:
  - Running the container
  - Testing the health endpoint at `/api/health`
  - Verifying the application starts correctly
- Save Docker image as artifact (for deployment)

**Features**:
- Multi-stage build for optimization
- Docker layer caching for faster builds
- Container health checks

#### 3. PR Comment Job
**Purpose**: Post test results summary on pull requests

**Features**:
- Parses test output to count passes/failures
- Creates formatted markdown comment with:
  - Overall test status
  - Unit test results
  - E2E test results
  - Docker build status
  - Expandable details sections
- Updates existing comment if found (no spam)

**Example Comment**:
```markdown
## 🧪 Test Results

**Overall Status:** ✅ All tests passed

### Unit Tests
- ✅ Passed: 20
- ❌ Failed: 0

### E2E Tests
- ✅ Passed: 11
- ❌ Failed: 0

### Docker Build
- Status: ✅ Success
```

#### 4. Claude AI Code Review Job
**Purpose**: Automated code review for performance, security, and best practices

**Review Categories**:
1. **Performance**: Inefficient algorithms, unnecessary re-renders, optimization opportunities
2. **Security**: XSS vulnerabilities, data handling issues, exposed secrets
3. **Best Practices**: React/Next.js patterns, TypeScript usage, code quality
4. **Accessibility**: WCAG 2.1 AA compliance, ARIA labels, keyboard navigation
5. **Testing**: Suggestions for better test coverage

**Process**:
1. Detects changed TypeScript/JavaScript files
2. Generates git diff of changes
3. Sends diff to Claude API with review prompt
4. Parses Claude's response
5. Posts formatted review as PR comment

**Model**: Claude 3.5 Sonnet (claude-3-5-sonnet-20241022)

#### 5. Deploy Job
**Purpose**: Deploy to Docker Hub

**Conditions**:
- Only runs on push to `main` branch
- Requires all tests to pass
- Requires successful Docker build

**Steps**:
- Log in to Docker Hub using credentials
- Extract metadata for tags:
  - Branch name (e.g., `main`)
  - Git SHA (e.g., `main-abc123`)
  - `latest` tag (only for main branch)
- Build and push multi-platform image (amd64, arm64)
- Generate deployment summary

**Image Tags**:
```
yourusername/oscal-ux:main
yourusername/oscal-ux:main-<git-sha>
yourusername/oscal-ux:latest
```

#### 6. Summary Job
**Purpose**: Generate overall pipeline summary

Creates a table showing the status of all jobs in the GitHub Actions summary page.

## Required Secrets

You must configure these secrets in your GitHub repository settings:

### 1. ANTHROPIC_API_KEY
**Required for**: Claude AI code review

**How to get**:
1. Sign up at https://console.anthropic.com/
2. Create an API key
3. Add to GitHub: Settings → Secrets and variables → Actions → New repository secret

### 2. DOCKERHUB_USERNAME
**Required for**: Docker Hub deployment

**Value**: Your Docker Hub username

### 3. DOCKERHUB_TOKEN
**Required for**: Docker Hub deployment

**How to get**:
1. Log in to Docker Hub
2. Account Settings → Security → New Access Token
3. Name: "GitHub Actions"
4. Permissions: Read, Write, Delete
5. Copy the token (shown only once)
6. Add to GitHub secrets

## Docker Configuration

### Dockerfile

Location: `front-end/ui/Dockerfile`

**Features**:
- Multi-stage build (deps → builder → runner)
- Runs as non-root user (`nextjs:nodejs`)
- Standalone Next.js build for minimal image size
- Health check endpoint
- Optimized layer caching

**Image Size**: ~150MB (compressed)

### Health Check Endpoint

Location: `src/app/api/health/route.ts`

**Response**:
```json
{
  "status": "healthy",
  "timestamp": "2025-01-16T10:00:00.000Z",
  "uptime": 12345.67
}
```

**Endpoint**: `GET /api/health`

## Running the Pipeline

### On Pull Requests

When you create or update a PR:
1. Tests run automatically
2. Docker build is executed
3. Test results are posted as a comment
4. Claude reviews your code changes
5. Pipeline shows pass/fail status

### On Push to Main

When code is merged to main:
1. All above steps run
2. If tests pass, Docker image is built and pushed to Docker Hub
3. Image is tagged with `latest`, `main`, and git SHA

## Local Testing

### Test Docker Build Locally

```bash
# Build the image
cd front-end/ui
docker build -t oscal-ux:local .

# Run the container
docker run -d -p 3000:3000 --name oscal-ux oscal-ux:local

# Test health endpoint
curl http://localhost:3000/api/health

# View logs
docker logs oscal-ux

# Stop and remove
docker stop oscal-ux
docker rm oscal-ux
```

### Run Tests Locally

```bash
cd front-end/ui

# Unit tests
npm test

# E2E tests
npm run test:e2e

# All tests
npm run test:all
```

## Pulling from Docker Hub

After successful deployment:

```bash
# Pull the latest image
docker pull yourusername/oscal-ux:latest

# Pull a specific version
docker pull yourusername/oscal-ux:main-abc1234

# Run the container
docker run -p 3000:3000 yourusername/oscal-ux:latest

# With environment variables
docker run -p 3000:3000 \
  -e NEXT_PUBLIC_API_URL=http://localhost:8080 \
  yourusername/oscal-ux:latest
```

## Docker Compose (Optional)

Create `docker-compose.yml`:

```yaml
version: '3.8'

services:
  frontend:
    image: yourusername/oscal-ux:latest
    ports:
      - "3000:3000"
    environment:
      - NEXT_PUBLIC_API_URL=http://backend:8080
    depends_on:
      - backend
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:3000/api/health"]
      interval: 30s
      timeout: 3s
      retries: 3

  backend:
    image: yourusername/oscal-backend:latest
    ports:
      - "8080:8080"
```

Run with:
```bash
docker-compose up -d
```

## Monitoring and Debugging

### View Workflow Runs

1. Go to your GitHub repository
2. Click "Actions" tab
3. Select "Frontend CI/CD" workflow
4. Click on a specific run to see details

### Check Test Results

- Test results are available as artifacts for 30 days
- Playwright reports include screenshots and traces
- Download from workflow run page

### Debug Failed Builds

1. Check the job logs in GitHub Actions
2. Look for error messages in the specific step
3. Common issues:
   - Missing secrets (ANTHROPIC_API_KEY, DOCKERHUB_TOKEN)
   - Test failures (check test output)
   - Docker build errors (check Dockerfile syntax)

## Performance Optimization

### Caching

The pipeline uses several caching strategies:
- **npm cache**: Speeds up dependency installation
- **Docker layer cache**: Reuses unchanged layers
- **GitHub Actions cache**: Persists between runs

### Parallel Execution

Jobs run in parallel when possible:
- Tests and builds can run simultaneously
- Comments and reviews post concurrently

## Security Considerations

### Secrets Management

- Never commit secrets to the repository
- Use GitHub Secrets for sensitive data
- Rotate Docker Hub tokens regularly
- Limit API key permissions

### Container Security

- Runs as non-root user
- Minimal base image (Alpine Linux)
- No unnecessary packages
- Regular dependency updates

### Code Review

Claude reviews check for:
- SQL injection risks
- XSS vulnerabilities
- Insecure data handling
- Exposed sensitive information

## Customization

### Modify Test Thresholds

Edit `.github/workflows/frontend-ci-cd.yml`:

```yaml
- name: Check test coverage
  run: |
    if [ "$COVERAGE" -lt "80" ]; then
      echo "Coverage below 80%"
      exit 1
    fi
```

### Change Docker Registry

Replace Docker Hub with GitHub Container Registry:

```yaml
- name: Log in to GitHub Container Registry
  uses: docker/login-action@v3
  with:
    registry: ghcr.io
    username: ${{ github.actor }}
    password: ${{ secrets.GITHUB_TOKEN }}
```

### Adjust Claude Review Prompt

Edit the `review_prompt.txt` section in the workflow to customize what Claude reviews.

## Troubleshooting

### Issue: Tests pass locally but fail in CI

**Solution**:
- Check Node.js version matches (18.x)
- Verify all dependencies are in `package.json`
- Check for environment-specific code
- Review test output artifacts

### Issue: Docker build fails

**Solution**:
- Verify Dockerfile syntax
- Check that `output: 'standalone'` is in `next.config.ts`
- Ensure all dependencies are installed
- Test build locally first

### Issue: Claude review not posting

**Solution**:
- Verify `ANTHROPIC_API_KEY` secret is set
- Check API key has credits
- Review workflow logs for API errors
- Ensure PR has TypeScript/JavaScript changes

### Issue: Docker Hub push fails

**Solution**:
- Verify `DOCKERHUB_USERNAME` and `DOCKERHUB_TOKEN` secrets
- Check token permissions (Read, Write, Delete)
- Ensure repository exists on Docker Hub
- Verify you're on `main` branch

## Best Practices

1. **Always run tests locally** before pushing
2. **Review Claude's feedback** carefully - it's usually right
3. **Keep Docker images small** by excluding unnecessary files
4. **Monitor workflow execution time** and optimize slow steps
5. **Update dependencies regularly** to get security fixes
6. **Use semantic versioning** for production releases

## Future Enhancements

Potential improvements to consider:
- Automatic semantic versioning based on commit messages
- Slack/Discord notifications for build failures
- Automated deployment to Kubernetes
- Performance benchmarking in CI
- Visual regression testing with Percy or Chromatic
- Dependency vulnerability scanning with Snyk
- SAST (Static Application Security Testing) integration

## Support

For issues with the CI/CD pipeline:
1. Check this documentation
2. Review GitHub Actions logs
3. Check Docker Hub repository
4. Verify all secrets are configured
5. Open an issue in the repository

---

**Last Updated**: January 2025
**Pipeline Version**: 1.0.0
