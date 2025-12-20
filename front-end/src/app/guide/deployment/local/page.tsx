import Link from 'next/link';
import { ArrowLeft, Server, Database, Globe, CheckCircle2, AlertCircle } from 'lucide-react';
import { Card, CardHeader, CardTitle, CardContent } from '@/components/ui/card';

export default function LocalDeploymentGuidePage() {
  return (
    <div className="min-h-screen bg-background">
      <div className="container mx-auto py-8 px-4 max-w-5xl">
        {/* Back Button */}
        <Link
          href="/guide"
          className="inline-flex items-center text-sm text-muted-foreground hover:text-foreground mb-6 transition-colors focus:outline-none focus:ring-2 focus:ring-ring focus:ring-offset-2 rounded"
          aria-label="Navigate back to user guide"
        >
          <ArrowLeft className="h-4 w-4 mr-2" aria-hidden="true" />
          Back to User Guide
        </Link>

        {/* Header */}
        <header className="mb-8">
          <div className="flex items-center gap-3 mb-3">
            <Server className="h-8 w-8 text-primary" />
            <h1 className="text-4xl font-bold">Local Deployment Guide</h1>
          </div>
          <p className="text-lg text-muted-foreground">
            Deploy OSCAL Tools on your local machine or VM for testing and development
          </p>
          <div className="mt-4 flex items-center gap-4 text-sm text-muted-foreground">
            <span>Version: 1.0.0</span>
            <span>•</span>
            <span>Updated: October 26, 2025</span>
          </div>
        </header>

        {/* Table of Contents */}
        <Card className="mb-8">
          <CardHeader>
            <CardTitle>Table of Contents</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="grid grid-cols-1 md:grid-cols-2 gap-2">
              <a href="#overview" className="text-primary hover:underline">Overview</a>
              <a href="#quick-start" className="text-primary hover:underline">Quick Start (5 Minutes)</a>
              <a href="#prerequisites" className="text-primary hover:underline">Prerequisites</a>
              <a href="#installation" className="text-primary hover:underline">Installation Methods</a>
              <a href="#using" className="text-primary hover:underline">Using the Application</a>
              <a href="#management" className="text-primary hover:underline">Management Commands</a>
              <a href="#troubleshooting" className="text-primary hover:underline">Troubleshooting</a>
              <a href="#advanced" className="text-primary hover:underline">Advanced Configuration</a>
            </div>
          </CardContent>
        </Card>

        {/* Content */}
        <div className="space-y-8">
          {/* Overview */}
          <Card id="overview">
            <CardHeader>
              <CardTitle>Overview</CardTitle>
            </CardHeader>
            <CardContent className="space-y-4">
              <p className="text-muted-foreground">
                This guide shows you how to run the full OSCAL Tools stack locally on your laptop or a VM.
                Perfect for testing, development, training, offline use, and demos.
              </p>

              <div>
                <h3 className="text-xl font-semibold mb-3">What Gets Installed</h3>
                <div className="space-y-2">
                  <div className="flex items-start gap-3">
                    <CheckCircle2 className="h-5 w-5 text-green-500 mt-0.5" />
                    <div>
                      <span className="font-medium">Frontend</span>
                      <span className="text-muted-foreground"> - Next.js web application (port 3000)</span>
                    </div>
                  </div>
                  <div className="flex items-start gap-3">
                    <CheckCircle2 className="h-5 w-5 text-green-500 mt-0.5" />
                    <div>
                      <span className="font-medium">Backend API</span>
                      <span className="text-muted-foreground"> - Spring Boot REST API (port 8080)</span>
                    </div>
                  </div>
                  <div className="flex items-start gap-3">
                    <CheckCircle2 className="h-5 w-5 text-green-500 mt-0.5" />
                    <div>
                      <span className="font-medium">PostgreSQL Database</span>
                      <span className="text-muted-foreground"> - Production-grade database (port 5432)</span>
                    </div>
                  </div>
                  <div className="flex items-start gap-3">
                    <CheckCircle2 className="h-5 w-5 text-green-500 mt-0.5" />
                    <div>
                      <span className="font-medium">pgAdmin</span>
                      <span className="text-muted-foreground"> - Database management UI (port 5050)</span>
                    </div>
                  </div>
                </div>
                <p className="text-sm text-muted-foreground mt-3">
                  All running in Docker containers for easy setup and isolation.
                </p>
              </div>
            </CardContent>
          </Card>

          {/* Quick Start */}
          <Card id="quick-start">
            <CardHeader>
              <CardTitle>Quick Start (5 Minutes)</CardTitle>
            </CardHeader>
            <CardContent className="space-y-6">
              <div>
                <h3 className="text-lg font-semibold mb-3">Option 1: Automated Script (Easiest)</h3>
                <code className="block bg-muted p-4 rounded text-sm font-mono whitespace-pre overflow-x-auto">
{`# 1. Clone the repository
git clone https://github.com/RegScale/oscal-hub.git
cd oscal-cli

# 2. Run the deployment script
./local-deploy.sh

# 3. Open your browser
open http://localhost:3000`}
                </code>
                <div className="mt-4 space-y-2">
                  <p className="text-sm text-muted-foreground">The script will:</p>
                  <ul className="list-none space-y-1 text-sm text-muted-foreground ml-4">
                    <li className="flex items-start gap-2">
                      <CheckCircle2 className="h-4 w-4 text-green-500 mt-0.5" />
                      Check prerequisites
                    </li>
                    <li className="flex items-start gap-2">
                      <CheckCircle2 className="h-4 w-4 text-green-500 mt-0.5" />
                      Build backend (Maven) and frontend (npm)
                    </li>
                    <li className="flex items-start gap-2">
                      <CheckCircle2 className="h-4 w-4 text-green-500 mt-0.5" />
                      Build Docker images and start all containers
                    </li>
                    <li className="flex items-start gap-2">
                      <CheckCircle2 className="h-4 w-4 text-green-500 mt-0.5" />
                      Wait for everything to be ready
                    </li>
                  </ul>
                  <p className="text-sm font-medium mt-3">Total time: 5-10 minutes (longer on first run)</p>
                </div>
              </div>

              <div>
                <h3 className="text-lg font-semibold mb-3">Option 2: Manual Docker Compose</h3>
                <code className="block bg-muted p-4 rounded text-sm font-mono whitespace-pre overflow-x-auto">
{`# 1. Clone and build
git clone https://github.com/RegScale/oscal-hub.git
cd oscal-cli

# 2. Build backend
cd back-end
mvn clean package -DskipTests
cd ..

# 3. Build frontend
cd front-end
npm ci
npm run build
cd ..

# 4. Start with Docker Compose
docker-compose up -d

# 5. Open browser
open http://localhost:3000`}
                </code>
              </div>

              <div>
                <h3 className="text-lg font-semibold mb-3">Option 3: Development Mode (No Docker)</h3>
                <code className="block bg-muted p-4 rounded text-sm font-mono whitespace-pre overflow-x-auto">
{`# 1. Clone repository
git clone https://github.com/RegScale/oscal-hub.git
cd oscal-cli

# 2. Start PostgreSQL (Docker only for database)
docker-compose -f docker-compose-postgres.yml up -d

# 3. Start backend (in one terminal)
cd back-end
mvn spring-boot:run -Dspring.profiles.active=dev

# 4. Start frontend (in another terminal)
cd front-end
npm install
npm run dev

# Access:
# - Frontend: http://localhost:3000
# - Backend: http://localhost:8080`}
                </code>
              </div>
            </CardContent>
          </Card>

          {/* Prerequisites */}
          <Card id="prerequisites">
            <CardHeader>
              <CardTitle>Prerequisites</CardTitle>
            </CardHeader>
            <CardContent className="space-y-6">
              <div>
                <h3 className="text-lg font-semibold mb-3">Required Software</h3>
                <div className="space-y-4">
                  <div>
                    <h4 className="font-medium mb-2">1. Docker Desktop (version 20.10+)</h4>
                    <ul className="list-disc list-inside space-y-1 text-sm text-muted-foreground ml-4">
                      <li><strong>macOS:</strong> <a href="https://docs.docker.com/desktop/install/mac-install/" target="_blank" rel="noopener noreferrer" className="text-primary hover:underline">Download Docker Desktop for Mac</a></li>
                      <li><strong>Windows:</strong> <a href="https://docs.docker.com/desktop/install/windows-install/" target="_blank" rel="noopener noreferrer" className="text-primary hover:underline">Download Docker Desktop for Windows</a></li>
                      <li><strong>Linux:</strong> <a href="https://docs.docker.com/desktop/install/linux-install/" target="_blank" rel="noopener noreferrer" className="text-primary hover:underline">Download Docker Desktop for Linux</a></li>
                    </ul>
                  </div>

                  <div>
                    <h4 className="font-medium mb-2">2. Git</h4>
                    <ul className="list-disc list-inside space-y-1 text-sm text-muted-foreground ml-4">
                      <li><strong>macOS:</strong> Already installed or via Homebrew: <code className="bg-muted px-1.5 py-0.5 rounded">brew install git</code></li>
                      <li><strong>Windows:</strong> <a href="https://git-scm.com/download/win" target="_blank" rel="noopener noreferrer" className="text-primary hover:underline">Download Git for Windows</a></li>
                      <li><strong>Linux:</strong> <code className="bg-muted px-1.5 py-0.5 rounded">sudo apt-get install git</code> (Ubuntu/Debian)</li>
                    </ul>
                  </div>
                </div>
              </div>

              <div>
                <h3 className="text-lg font-semibold mb-3">System Requirements</h3>
                <div className="overflow-x-auto">
                  <table className="w-full text-sm">
                    <thead className="border-b">
                      <tr>
                        <th className="text-left py-2 pr-4">Requirement</th>
                        <th className="text-left py-2 pr-4">Minimum</th>
                        <th className="text-left py-2">Recommended</th>
                      </tr>
                    </thead>
                    <tbody className="text-muted-foreground">
                      <tr className="border-b">
                        <td className="py-2 pr-4">RAM</td>
                        <td className="py-2 pr-4">4 GB</td>
                        <td className="py-2">8 GB</td>
                      </tr>
                      <tr className="border-b">
                        <td className="py-2 pr-4">Disk Space</td>
                        <td className="py-2 pr-4">10 GB free</td>
                        <td className="py-2">20 GB free</td>
                      </tr>
                      <tr className="border-b">
                        <td className="py-2 pr-4">CPU</td>
                        <td className="py-2 pr-4">2 cores</td>
                        <td className="py-2">4 cores</td>
                      </tr>
                      <tr>
                        <td className="py-2 pr-4">OS</td>
                        <td className="py-2 pr-4">macOS 11+, Windows 10+, Ubuntu 20.04+</td>
                        <td className="py-2">Latest versions</td>
                      </tr>
                    </tbody>
                  </table>
                </div>
              </div>

              <div>
                <h3 className="text-lg font-semibold mb-3">Verify Prerequisites</h3>
                <code className="block bg-muted p-4 rounded text-sm font-mono whitespace-pre overflow-x-auto">
{`# Check Docker
docker --version
# Expected: Docker version 20.10.0 or higher

# Check Docker Compose
docker-compose --version
# Expected: Docker Compose version 2.x or higher

# Check if Docker is running
docker info
# Should show Docker engine information`}
                </code>
              </div>
            </CardContent>
          </Card>

          {/* Using the Application */}
          <Card id="using">
            <CardHeader>
              <CardTitle>Using the Application</CardTitle>
            </CardHeader>
            <CardContent className="space-y-6">
              <div>
                <h3 className="text-lg font-semibold mb-3">First-Time Setup</h3>
                <ol className="list-decimal list-inside space-y-2 text-muted-foreground">
                  <li>Open your browser to <a href="http://localhost:3000" className="text-primary hover:underline">http://localhost:3000</a></li>
                  <li>Click &quot;Register&quot; or &quot;Sign Up&quot;</li>
                  <li>Fill in your details (password must be at least 10 characters with uppercase, lowercase, digit, and special character)</li>
                  <li>Log in with your new credentials</li>
                  <li>Start validating OSCAL documents!</li>
                </ol>
              </div>

              <div>
                <h3 className="text-lg font-semibold mb-3">Access Points</h3>
                <div className="space-y-2">
                  <div className="flex items-center gap-3">
                    <Globe className="h-5 w-5 text-primary" />
                    <div>
                      <div className="font-medium">Frontend</div>
                      <a href="http://localhost:3000" target="_blank" rel="noopener noreferrer" className="text-sm text-primary hover:underline">
                        http://localhost:3000
                      </a>
                    </div>
                  </div>
                  <div className="flex items-center gap-3">
                    <Server className="h-5 w-5 text-primary" />
                    <div>
                      <div className="font-medium">Backend API</div>
                      <a href="http://localhost:8080/api" target="_blank" rel="noopener noreferrer" className="text-sm text-primary hover:underline">
                        http://localhost:8080/api
                      </a>
                    </div>
                  </div>
                  <div className="flex items-center gap-3">
                    <Server className="h-5 w-5 text-primary" />
                    <div>
                      <div className="font-medium">Swagger UI (API Docs)</div>
                      <a href="http://localhost:8080/swagger-ui.html" target="_blank" rel="noopener noreferrer" className="text-sm text-primary hover:underline">
                        http://localhost:8080/swagger-ui.html
                      </a>
                    </div>
                  </div>
                  <div className="flex items-center gap-3">
                    <Database className="h-5 w-5 text-primary" />
                    <div>
                      <div className="font-medium">pgAdmin (Database UI)</div>
                      <a href="http://localhost:5050" target="_blank" rel="noopener noreferrer" className="text-sm text-primary hover:underline">
                        http://localhost:5050
                      </a>
                      <div className="text-xs text-muted-foreground">Email: admin@oscal.local, Password: admin</div>
                    </div>
                  </div>
                </div>
              </div>
            </CardContent>
          </Card>

          {/* Management Commands */}
          <Card id="management">
            <CardHeader>
              <CardTitle>Management Commands</CardTitle>
            </CardHeader>
            <CardContent className="space-y-6">
              <div>
                <h3 className="text-lg font-semibold mb-3">Using the Script</h3>
                <code className="block bg-muted p-4 rounded text-sm font-mono whitespace-pre overflow-x-auto">
{`# View status
./local-deploy.sh status

# View logs
./local-deploy.sh logs

# Stop application
./local-deploy.sh stop

# Restart application
./local-deploy.sh restart

# Clean everything (removes data!)
./local-deploy.sh clean

# Show help
./local-deploy.sh help`}
                </code>
              </div>

              <div>
                <h3 className="text-lg font-semibold mb-3">Using Docker Compose</h3>
                <code className="block bg-muted p-4 rounded text-sm font-mono whitespace-pre overflow-x-auto">
{`# View running containers
docker-compose ps

# View logs
docker-compose logs -f

# View logs for specific service
docker-compose logs -f oscal-ux
docker-compose logs -f postgres

# Stop application
docker-compose stop

# Start application
docker-compose start

# Restart specific service
docker-compose restart oscal-ux

# Stop and remove containers
docker-compose down

# Stop and remove containers + volumes (deletes data!)
docker-compose down -v`}
                </code>
              </div>
            </CardContent>
          </Card>

          {/* Troubleshooting */}
          <Card id="troubleshooting">
            <CardHeader>
              <CardTitle>Troubleshooting</CardTitle>
            </CardHeader>
            <CardContent className="space-y-6">
              <div>
                <div className="flex items-start gap-3 mb-3">
                  <AlertCircle className="h-5 w-5 text-amber-500 mt-0.5" />
                  <h3 className="text-lg font-semibold">Docker is not running</h3>
                </div>
                <p className="text-sm text-muted-foreground mb-2">
                  <strong>Symptom:</strong> Script fails with &quot;Docker daemon is not running&quot;
                </p>
                <p className="text-sm text-muted-foreground mb-2"><strong>Solution:</strong></p>
                <ol className="list-decimal list-inside space-y-1 text-sm text-muted-foreground ml-4">
                  <li>Start Docker Desktop application</li>
                  <li>Wait for Docker to fully start (icon in system tray)</li>
                  <li>Verify with: <code className="bg-muted px-1.5 py-0.5 rounded">docker info</code></li>
                  <li>Run script again: <code className="bg-muted px-1.5 py-0.5 rounded">./local-deploy.sh</code></li>
                </ol>
              </div>

              <div>
                <div className="flex items-start gap-3 mb-3">
                  <AlertCircle className="h-5 w-5 text-amber-500 mt-0.5" />
                  <h3 className="text-lg font-semibold">Port already in use</h3>
                </div>
                <p className="text-sm text-muted-foreground mb-2">
                  <strong>Symptom:</strong> Error: Port 3000 or 8080 is already in use
                </p>
                <p className="text-sm text-muted-foreground mb-2"><strong>Solution:</strong></p>
                <code className="block bg-muted p-4 rounded text-sm font-mono whitespace-pre overflow-x-auto">
{`# Find what's using port 3000
lsof -i :3000

# Kill the process
kill -9 <PID>

# Or for port 8080
lsof -i :8080
kill -9 <PID>`}
                </code>
              </div>

              <div>
                <div className="flex items-start gap-3 mb-3">
                  <AlertCircle className="h-5 w-5 text-amber-500 mt-0.5" />
                  <h3 className="text-lg font-semibold">Container keeps restarting</h3>
                </div>
                <p className="text-sm text-muted-foreground mb-2">
                  <strong>Symptom:</strong> docker-compose ps shows &quot;Restarting&quot; status
                </p>
                <p className="text-sm text-muted-foreground mb-2"><strong>Solution:</strong></p>
                <code className="block bg-muted p-4 rounded text-sm font-mono whitespace-pre overflow-x-auto">
{`# View logs to see error
docker-compose logs oscal-ux

# Common causes:
# 1. Database not ready - wait 30 seconds and check again
# 2. Environment variable missing - check docker-compose.yml
# 3. Port conflict - see "Port already in use" above

# Try restart
docker-compose restart`}
                </code>
              </div>

              <div>
                <div className="flex items-start gap-3 mb-3">
                  <AlertCircle className="h-5 w-5 text-amber-500 mt-0.5" />
                  <h3 className="text-lg font-semibold">Frontend shows &quot;Cannot connect to API&quot;</h3>
                </div>
                <p className="text-sm text-muted-foreground mb-2">
                  <strong>Symptom:</strong> Frontend loads but shows connection error
                </p>
                <p className="text-sm text-muted-foreground mb-2"><strong>Solution:</strong></p>
                <code className="block bg-muted p-4 rounded text-sm font-mono whitespace-pre overflow-x-auto">
{`# 1. Check if backend is running
curl http://localhost:8080/api/health

# 2. If not responding, check logs
docker-compose logs oscal-ux

# 3. Common fixes:
# - Wait longer (backend takes 30-60 seconds to start)
# - Check if port 8080 is accessible
# - Restart: docker-compose restart oscal-ux`}
                </code>
              </div>
            </CardContent>
          </Card>

          {/* Advanced Configuration */}
          <Card id="advanced">
            <CardHeader>
              <CardTitle>Advanced Configuration</CardTitle>
            </CardHeader>
            <CardContent className="space-y-6">
              <div>
                <h3 className="text-lg font-semibold mb-3">Custom Configuration</h3>
                <p className="text-muted-foreground mb-3">
                  Create a <code className="bg-muted px-1.5 py-0.5 rounded">.env</code> file in the project root:
                </p>
                <code className="block bg-muted p-4 rounded text-sm font-mono whitespace-pre overflow-x-auto">
{`# Database
DB_NAME=oscal_dev
DB_USER=oscal_user
DB_PASSWORD=oscal_dev_password

# Application
JWT_SECRET=dev-secret-key-at-least-32-characters-long
CORS_ALLOWED_ORIGINS=http://localhost:3000,http://localhost:3001

# Features
SPRING_PROFILES_ACTIVE=dev
SWAGGER_ENABLED=true
RATE_LIMIT_ENABLED=false`}
                </code>
              </div>

              <div>
                <h3 className="text-lg font-semibold mb-3">Comparison: Local vs Azure Deployment</h3>
                <div className="overflow-x-auto">
                  <table className="w-full text-sm">
                    <thead className="border-b">
                      <tr>
                        <th className="text-left py-2 pr-4">Feature</th>
                        <th className="text-left py-2 pr-4">Local Deployment</th>
                        <th className="text-left py-2">Azure Deployment</th>
                      </tr>
                    </thead>
                    <tbody className="text-muted-foreground">
                      <tr className="border-b">
                        <td className="py-2 pr-4 font-medium">Setup Time</td>
                        <td className="py-2 pr-4">5-10 minutes</td>
                        <td className="py-2">30-60 minutes (one-time)</td>
                      </tr>
                      <tr className="border-b">
                        <td className="py-2 pr-4 font-medium">Cost</td>
                        <td className="py-2 pr-4">Free</td>
                        <td className="py-2">~$100/month</td>
                      </tr>
                      <tr className="border-b">
                        <td className="py-2 pr-4 font-medium">Internet Required</td>
                        <td className="py-2 pr-4">Only for initial download</td>
                        <td className="py-2">Always</td>
                      </tr>
                      <tr className="border-b">
                        <td className="py-2 pr-4 font-medium">Scalability</td>
                        <td className="py-2 pr-4">Single machine</td>
                        <td className="py-2">Auto-scaling</td>
                      </tr>
                      <tr className="border-b">
                        <td className="py-2 pr-4 font-medium">Backup</td>
                        <td className="py-2 pr-4">Manual</td>
                        <td className="py-2">Automated (7 days)</td>
                      </tr>
                      <tr className="border-b">
                        <td className="py-2 pr-4 font-medium">SSL/HTTPS</td>
                        <td className="py-2 pr-4">Optional (self-signed)</td>
                        <td className="py-2">Automatic</td>
                      </tr>
                      <tr>
                        <td className="py-2 pr-4 font-medium">Best For</td>
                        <td className="py-2 pr-4">Testing, Development, Demos</td>
                        <td className="py-2">Production, Teams</td>
                      </tr>
                    </tbody>
                  </table>
                </div>
              </div>
            </CardContent>
          </Card>

          {/* Next Steps */}
          <Card>
            <CardHeader>
              <CardTitle>Next Steps</CardTitle>
            </CardHeader>
            <CardContent className="space-y-3">
              <p className="text-muted-foreground">After successful local deployment:</p>
              <ul className="list-disc list-inside space-y-2 text-muted-foreground ml-4">
                <li>Read the <Link href="/guide" className="text-primary hover:underline">User Guide</Link></li>
                <li>Try validating OSCAL documents via the web interface</li>
                <li>Explore the API at <a href="http://localhost:8080/swagger-ui.html" target="_blank" rel="noopener noreferrer" className="text-primary hover:underline">http://localhost:8080/swagger-ui.html</a></li>
                <li>Learn about <Link href="/guide/deployment/azure" className="text-primary hover:underline">Azure deployment</Link> for production</li>
              </ul>
            </CardContent>
          </Card>

          {/* Additional Resources */}
          <Card>
            <CardHeader>
              <CardTitle>Additional Resources</CardTitle>
            </CardHeader>
            <CardContent className="space-y-3">
              <a
                href="https://docs.docker.com/"
                target="_blank"
                rel="noopener noreferrer"
                className="flex items-center text-primary hover:underline"
              >
                Docker Documentation
              </a>

              <a
                href="https://docs.docker.com/compose/"
                target="_blank"
                rel="noopener noreferrer"
                className="flex items-center text-primary hover:underline"
              >
                Docker Compose Documentation
              </a>

              <Link
                href="/guide/automation"
                className="flex items-center text-primary hover:underline"
              >
                API Automation Guide
              </Link>

              <a
                href="https://pages.nist.gov/OSCAL/"
                target="_blank"
                rel="noopener noreferrer"
                className="flex items-center text-primary hover:underline"
              >
                NIST OSCAL Documentation
              </a>
            </CardContent>
          </Card>
        </div>
      </div>
    </div>
  );
}
