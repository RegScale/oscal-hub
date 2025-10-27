import Link from 'next/link';
import { ArrowLeft, Terminal, CheckCircle2, AlertCircle, Code, Zap } from 'lucide-react';
import { Card, CardHeader, CardTitle, CardContent } from '@/components/ui/card';

export default function CLIDeploymentGuidePage() {
  return (
    <div className="min-h-screen bg-background">
      <div className="container mx-auto py-8 px-4 max-w-5xl" id="main-content">
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
            <Terminal className="h-8 w-8 text-primary" />
            <h1 className="text-4xl font-bold">CLI Mode Deployment Guide</h1>
          </div>
          <p className="text-lg text-muted-foreground">
            Install and use OSCAL CLI as a standalone command-line tool for automation, scripting, and CI/CD pipelines
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
              <a href="#quick-start" className="text-primary hover:underline">Quick Start (2 Minutes)</a>
              <a href="#prerequisites" className="text-primary hover:underline">Prerequisites</a>
              <a href="#installation" className="text-primary hover:underline">Installation Methods</a>
              <a href="#using-cli" className="text-primary hover:underline">Using the CLI</a>
              <a href="#common-workflows" className="text-primary hover:underline">Common Workflows</a>
              <a href="#ci-cd" className="text-primary hover:underline">CI/CD Integration</a>
              <a href="#troubleshooting" className="text-primary hover:underline">Troubleshooting</a>
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
                The OSCAL CLI is a standalone command-line tool for working with OSCAL documents without requiring a web interface or database.
                Perfect for automation, batch processing, CI/CD pipelines, and offline use.
              </p>

              <div>
                <h3 className="text-xl font-semibold mb-3">What You Get</h3>
                <div className="space-y-2">
                  <div className="flex items-start gap-3">
                    <CheckCircle2 className="h-5 w-5 text-green-500 mt-0.5" />
                    <div>
                      <span className="font-medium">Command-Line Tool</span>
                      <span className="text-muted-foreground"> - </span>
                      <code className="text-xs bg-muted px-1.5 py-0.5 rounded">oscal-cli</code>
                      <span className="text-muted-foreground"> executable for all OSCAL operations</span>
                    </div>
                  </div>
                  <div className="flex items-start gap-3">
                    <CheckCircle2 className="h-5 w-5 text-green-500 mt-0.5" />
                    <div>
                      <span className="font-medium">All OSCAL Models</span>
                      <span className="text-muted-foreground"> - Catalog, Profile, SSP, Component Definition, AP, AR, POA&amp;M</span>
                    </div>
                  </div>
                  <div className="flex items-start gap-3">
                    <CheckCircle2 className="h-5 w-5 text-green-500 mt-0.5" />
                    <div>
                      <span className="font-medium">Format Conversion</span>
                      <span className="text-muted-foreground"> - Convert between XML, JSON, and YAML</span>
                    </div>
                  </div>
                  <div className="flex items-start gap-3">
                    <CheckCircle2 className="h-5 w-5 text-green-500 mt-0.5" />
                    <div>
                      <span className="font-medium">Profile Resolution</span>
                      <span className="text-muted-foreground"> - Resolve profiles to catalogs</span>
                    </div>
                  </div>
                  <div className="flex items-start gap-3">
                    <CheckCircle2 className="h-5 w-5 text-green-500 mt-0.5" />
                    <div>
                      <span className="font-medium">Schema Validation</span>
                      <span className="text-muted-foreground"> - Validate against OSCAL schemas and constraints</span>
                    </div>
                  </div>
                  <div className="flex items-start gap-3">
                    <CheckCircle2 className="h-5 w-5 text-green-500 mt-0.5" />
                    <div>
                      <span className="font-medium">Batch Operations</span>
                      <span className="text-muted-foreground"> - Process multiple files with shell scripts</span>
                    </div>
                  </div>
                </div>
                <p className="text-sm text-muted-foreground mt-3">
                  No web interface, database, or user authentication required. Works 100% offline after installation.
                </p>
              </div>

              <div>
                <h3 className="text-xl font-semibold mb-3">NOT Included in CLI Mode</h3>
                <div className="space-y-2">
                  <div className="flex items-start gap-3">
                    <AlertCircle className="h-5 w-5 text-amber-500 mt-0.5" />
                    <span className="text-muted-foreground">Web interface (no browser UI)</span>
                  </div>
                  <div className="flex items-start gap-3">
                    <AlertCircle className="h-5 w-5 text-amber-500 mt-0.5" />
                    <span className="text-muted-foreground">User authentication or multi-user support</span>
                  </div>
                  <div className="flex items-start gap-3">
                    <AlertCircle className="h-5 w-5 text-amber-500 mt-0.5" />
                    <span className="text-muted-foreground">Database for storing files or history</span>
                  </div>
                  <div className="flex items-start gap-3">
                    <AlertCircle className="h-5 w-5 text-amber-500 mt-0.5" />
                    <span className="text-muted-foreground">REST API endpoints</span>
                  </div>
                </div>
                <p className="text-sm text-muted-foreground mt-3">
                  For web interface and API features, see <Link href="/guide/deployment/local" className="text-primary hover:underline">Local Deployment</Link> or <Link href="/guide/deployment/azure" className="text-primary hover:underline">Azure Deployment</Link>.
                </p>
              </div>
            </CardContent>
          </Card>

          {/* Quick Start */}
          <Card id="quick-start">
            <CardHeader>
              <CardTitle>Quick Start (2 Minutes)</CardTitle>
            </CardHeader>
            <CardContent className="space-y-6">
              <div>
                <h3 className="text-lg font-semibold mb-3">Mac/Linux</h3>
                <code className="block bg-muted p-4 rounded text-sm font-mono whitespace-pre overflow-x-auto">
{`# 1. Download and run the installer
curl -fsSL https://raw.githubusercontent.com/usnistgov/oscal-cli/main/installer/install.sh | bash

# 2. Add to PATH (if not already done)
export PATH="$PATH:$HOME/.oscal-cli/bin"

# 3. Verify installation
oscal-cli --version

# 4. Validate an OSCAL file
oscal-cli catalog validate examples/catalog.json`}
                </code>
              </div>

              <div>
                <h3 className="text-lg font-semibold mb-3">Windows (PowerShell)</h3>
                <code className="block bg-muted p-4 rounded text-sm font-mono whitespace-pre overflow-x-auto">
{`# 1. Allow script execution (if needed)
Set-ExecutionPolicy -ExecutionPolicy RemoteSigned -Scope CurrentUser

# 2. Download and run installer
Invoke-WebRequest -Uri https://raw.githubusercontent.com/usnistgov/oscal-cli/main/installer/install.ps1 -OutFile install.ps1
.\\install.ps1

# 3. Add to PATH or use full path
%USERPROFILE%\\.oscal-cli\\bin\\oscal-cli.bat --version

# 4. Validate an OSCAL file
oscal-cli catalog validate examples\\catalog.json`}
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
                <div>
                  <h4 className="font-medium mb-2">Java 11 or higher (Java 21 LTS recommended)</h4>
                  <code className="block bg-muted p-4 rounded text-sm font-mono whitespace-pre overflow-x-auto mb-3">
{`# Check your Java version
java -version
# Expected: openjdk version "11.0.0" or higher`}
                  </code>
                  <ul className="list-disc list-inside space-y-1 text-sm text-muted-foreground ml-4">
                    <li><strong>Mac:</strong> <code className="bg-muted px-1.5 py-0.5 rounded">brew install openjdk@21</code> or download from <a href="https://adoptium.net/" target="_blank" rel="noopener noreferrer" className="text-primary hover:underline">Adoptium</a></li>
                    <li><strong>Linux:</strong> <code className="bg-muted px-1.5 py-0.5 rounded">sudo apt install openjdk-21-jdk</code></li>
                    <li><strong>Windows:</strong> <code className="bg-muted px-1.5 py-0.5 rounded">winget install EclipseAdoptium.Temurin.21.JDK</code></li>
                  </ul>
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
                        <td className="py-2 pr-4">512 MB</td>
                        <td className="py-2">2 GB</td>
                      </tr>
                      <tr className="border-b">
                        <td className="py-2 pr-4">Disk Space</td>
                        <td className="py-2 pr-4">100 MB</td>
                        <td className="py-2">500 MB</td>
                      </tr>
                      <tr className="border-b">
                        <td className="py-2 pr-4">CPU</td>
                        <td className="py-2 pr-4">1 core</td>
                        <td className="py-2">2+ cores</td>
                      </tr>
                      <tr>
                        <td className="py-2 pr-4">Network</td>
                        <td className="py-2 pr-4" colSpan={2}>Only required for initial installation</td>
                      </tr>
                    </tbody>
                  </table>
                </div>
              </div>
            </CardContent>
          </Card>

          {/* Installation Methods */}
          <Card id="installation">
            <CardHeader>
              <CardTitle>Installation Methods</CardTitle>
            </CardHeader>
            <CardContent className="space-y-6">
              <div>
                <h3 className="text-lg font-semibold mb-3">Method 1: Automated Installer (Recommended)</h3>
                <p className="text-muted-foreground mb-3">
                  The automated installer is the easiest way to get started. It checks for Java, downloads the latest OSCAL CLI, and sets everything up.
                </p>
                <code className="block bg-muted p-4 rounded text-sm font-mono whitespace-pre overflow-x-auto">
{`# Mac/Linux
curl -fsSL https://raw.githubusercontent.com/usnistgov/oscal-cli/main/installer/install.sh | bash

# Or download first, then run
curl -O https://raw.githubusercontent.com/usnistgov/oscal-cli/main/installer/install.sh
chmod +x install.sh
./install.sh

# Custom installation directory
OSCAL_CLI_HOME=/opt/oscal-cli ./install.sh

# Install specific version
OSCAL_CLI_VERSION=1.0.3 ./install.sh`}
                </code>
              </div>

              <div>
                <h3 className="text-lg font-semibold mb-3">Method 2: Manual Installation</h3>
                <p className="text-muted-foreground mb-3">
                  Download from Maven Central and extract manually:
                </p>
                <code className="block bg-muted p-4 rounded text-sm font-mono whitespace-pre overflow-x-auto">
{`# Download latest version (example: 1.0.3)
wget https://repo1.maven.org/maven2/gov/nist/secauto/oscal/tools/oscal-cli/cli-core/1.0.3/cli-core-1.0.3-oscal-cli.zip

# Extract to installation directory
mkdir -p ~/.oscal-cli
unzip cli-core-1.0.3-oscal-cli.zip -d ~/.oscal-cli

# Add to PATH
echo 'export PATH="$PATH:$HOME/.oscal-cli/bin"' >> ~/.bashrc
source ~/.bashrc`}
                </code>
              </div>

              <div>
                <h3 className="text-lg font-semibold mb-3">Method 3: Build from Source</h3>
                <p className="text-muted-foreground mb-3">
                  For developers or those who want the latest unreleased version:
                </p>
                <code className="block bg-muted p-4 rounded text-sm font-mono whitespace-pre overflow-x-auto">
{`# Clone repository
git clone https://github.com/usnistgov/oscal-cli.git
cd oscal-cli

# Build with Maven
cd cli
mvn clean install

# Run from build output
./target/appassembler/bin/oscal-cli --version`}
                </code>
              </div>
            </CardContent>
          </Card>

          {/* Using the CLI */}
          <Card id="using-cli">
            <CardHeader>
              <CardTitle>Using the CLI</CardTitle>
            </CardHeader>
            <CardContent className="space-y-6">
              <div>
                <h3 className="text-lg font-semibold mb-3">Command Structure</h3>
                <code className="block bg-muted p-4 rounded text-sm font-mono whitespace-pre overflow-x-auto mb-3">
{`oscal-cli <model> <operation> [options] <file>`}
                </code>
                <ul className="list-disc list-inside space-y-1 text-sm text-muted-foreground ml-4">
                  <li><code className="bg-muted px-1.5 py-0.5 rounded">&lt;model&gt;</code> - OSCAL model type (catalog, profile, ssp, etc.)</li>
                  <li><code className="bg-muted px-1.5 py-0.5 rounded">&lt;operation&gt;</code> - What to do (validate, convert, resolve)</li>
                  <li><code className="bg-muted px-1.5 py-0.5 rounded">[options]</code> - Flags like --to, --as, --overwrite</li>
                  <li><code className="bg-muted px-1.5 py-0.5 rounded">&lt;file&gt;</code> - Input file path</li>
                </ul>
              </div>

              <div>
                <h3 className="text-lg font-semibold mb-3">Common Operations</h3>
                <div className="space-y-4">
                  <div>
                    <h4 className="font-medium mb-2">1. Validation</h4>
                    <code className="block bg-muted p-4 rounded text-sm font-mono whitespace-pre overflow-x-auto">
{`# Validate a catalog
oscal-cli catalog validate my-catalog.xml

# Validate a profile
oscal-cli profile validate my-profile.json

# Validate an SSP
oscal-cli ssp validate my-ssp.yaml`}
                    </code>
                  </div>

                  <div>
                    <h4 className="font-medium mb-2">2. Format Conversion</h4>
                    <code className="block bg-muted p-4 rounded text-sm font-mono whitespace-pre overflow-x-auto">
{`# XML to JSON
oscal-cli catalog convert --to json catalog.xml catalog.json

# JSON to YAML
oscal-cli profile convert --to yaml profile.json profile.yaml

# YAML to XML
oscal-cli ssp convert --to xml ssp.yaml ssp.xml`}
                    </code>
                  </div>

                  <div>
                    <h4 className="font-medium mb-2">3. Profile Resolution</h4>
                    <code className="block bg-muted p-4 rounded text-sm font-mono whitespace-pre overflow-x-auto">
{`# Resolve profile to JSON catalog
oscal-cli profile resolve --to json my-baseline.xml resolved-catalog.json

# Resolve profile to XML catalog
oscal-cli profile resolve --to xml my-baseline.json resolved-catalog.xml`}
                    </code>
                  </div>
                </div>
              </div>

              <div>
                <h3 className="text-lg font-semibold mb-3">Getting Help</h3>
                <code className="block bg-muted p-4 rounded text-sm font-mono whitespace-pre overflow-x-auto">
{`# General help
oscal-cli --help

# Model-specific help
oscal-cli catalog --help
oscal-cli profile --help

# Operation-specific help
oscal-cli catalog validate --help
oscal-cli profile resolve --help`}
                </code>
              </div>
            </CardContent>
          </Card>

          {/* Common Workflows */}
          <Card id="common-workflows">
            <CardHeader>
              <CardTitle>Common Workflows</CardTitle>
            </CardHeader>
            <CardContent className="space-y-6">
              <div>
                <h3 className="text-lg font-semibold mb-3">Validate All Files in a Directory</h3>
                <code className="block bg-muted p-4 rounded text-sm font-mono whitespace-pre overflow-x-auto">
{`# Bash (Mac/Linux)
for file in catalogs/*.xml; do
    echo "Validating $file..."
    oscal-cli catalog validate "$file"
done

# PowerShell (Windows)
Get-ChildItem *.json | ForEach-Object {
    Write-Host "Validating $_..."
    oscal-cli profile validate $_.Name
}`}
                </code>
              </div>

              <div>
                <h3 className="text-lg font-semibold mb-3">Batch Format Conversion</h3>
                <code className="block bg-muted p-4 rounded text-sm font-mono whitespace-pre overflow-x-auto">
{`# Convert all XML catalogs to JSON
for file in *.xml; do
    output="\${file%.xml}.json"
    oscal-cli catalog convert --to json "$file" "$output" --overwrite
done`}
                </code>
              </div>

              <div>
                <h3 className="text-lg font-semibold mb-3">Git Pre-Commit Hook</h3>
                <p className="text-muted-foreground mb-3">
                  Create <code className="bg-muted px-1.5 py-0.5 rounded">.git/hooks/pre-commit</code>:
                </p>
                <code className="block bg-muted p-4 rounded text-sm font-mono whitespace-pre overflow-x-auto">
{`#!/bin/bash
echo "Validating OSCAL files..."

OSCAL_FILES=$(git diff --cached --name-only | grep -E '\\.(xml|json)$')

for file in $OSCAL_FILES; do
    if ! oscal-cli catalog validate "$file"; then
        echo "✗ Validation failed for $file"
        exit 1
    fi
done

echo "✓ All OSCAL files validated successfully"`}
                </code>
              </div>
            </CardContent>
          </Card>

          {/* CI/CD Integration */}
          <Card id="ci-cd">
            <CardHeader>
              <CardTitle>CI/CD Integration</CardTitle>
            </CardHeader>
            <CardContent className="space-y-6">
              <div>
                <h3 className="text-lg font-semibold mb-3">GitHub Actions</h3>
                <p className="text-muted-foreground mb-3">
                  Create <code className="bg-muted px-1.5 py-0.5 rounded">.github/workflows/validate-oscal.yml</code>:
                </p>
                <code className="block bg-muted p-4 rounded text-sm font-mono whitespace-pre overflow-x-auto">
{`name: Validate OSCAL Files

on: [push, pull_request]

jobs:
  validate:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - uses: actions/setup-java@v3
        with:
          java-version: '21'
          distribution: 'temurin'

      - name: Install OSCAL CLI
        run: |
          curl -fsSL https://raw.githubusercontent.com/usnistgov/oscal-cli/main/installer/install.sh | bash
          echo "$HOME/.oscal-cli/bin" >> $GITHUB_PATH

      - name: Validate OSCAL files
        run: |
          for file in oscal/**/*.{xml,json}; do
            oscal-cli catalog validate "$file"
          done`}
                </code>
              </div>

              <div>
                <h3 className="text-lg font-semibold mb-3">GitLab CI</h3>
                <p className="text-muted-foreground mb-3">
                  Create <code className="bg-muted px-1.5 py-0.5 rounded">.gitlab-ci.yml</code>:
                </p>
                <code className="block bg-muted p-4 rounded text-sm font-mono whitespace-pre overflow-x-auto">
{`image: eclipse-temurin:21-jdk

validate_oscal:
  script:
    - curl -fsSL https://raw.githubusercontent.com/usnistgov/oscal-cli/main/installer/install.sh | bash
    - export PATH="$PATH:$HOME/.oscal-cli/bin"
    - for file in oscal/**/*.xml; do oscal-cli catalog validate "$file"; done`}
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
                  <h3 className="text-lg font-semibold">Command Not Found</h3>
                </div>
                <p className="text-sm text-muted-foreground mb-2">
                  <strong>Symptom:</strong> <code className="bg-muted px-1.5 py-0.5 rounded">oscal-cli: command not found</code>
                </p>
                <p className="text-sm text-muted-foreground mb-2"><strong>Solution:</strong></p>
                <code className="block bg-muted p-4 rounded text-sm font-mono whitespace-pre overflow-x-auto">
{`# Add to PATH
export PATH="$PATH:$HOME/.oscal-cli/bin"
echo 'export PATH="$PATH:$HOME/.oscal-cli/bin"' >> ~/.bashrc
source ~/.bashrc

# Or use full path
~/.oscal-cli/bin/oscal-cli --version`}
                </code>
              </div>

              <div>
                <div className="flex items-start gap-3 mb-3">
                  <AlertCircle className="h-5 w-5 text-amber-500 mt-0.5" />
                  <h3 className="text-lg font-semibold">Java Not Found or Wrong Version</h3>
                </div>
                <p className="text-sm text-muted-foreground mb-2">
                  <strong>Symptom:</strong> Error about Java not installed or Java 11+ required
                </p>
                <p className="text-sm text-muted-foreground mb-2"><strong>Solution:</strong></p>
                <ol className="list-decimal list-inside space-y-1 text-sm text-muted-foreground ml-4">
                  <li>Install Java 21 from <a href="https://adoptium.net/" target="_blank" rel="noopener noreferrer" className="text-primary hover:underline">Adoptium</a></li>
                  <li>Verify: <code className="bg-muted px-1.5 py-0.5 rounded">java -version</code></li>
                  <li>Set JAVA_HOME if needed</li>
                </ol>
              </div>

              <div>
                <div className="flex items-start gap-3 mb-3">
                  <AlertCircle className="h-5 w-5 text-amber-500 mt-0.5" />
                  <h3 className="text-lg font-semibold">Out of Memory</h3>
                </div>
                <p className="text-sm text-muted-foreground mb-2">
                  <strong>Symptom:</strong> <code className="bg-muted px-1.5 py-0.5 rounded">java.lang.OutOfMemoryError</code>
                </p>
                <p className="text-sm text-muted-foreground mb-2"><strong>Solution:</strong></p>
                <code className="block bg-muted p-4 rounded text-sm font-mono whitespace-pre overflow-x-auto">
{`# Increase Java heap size
export JAVA_OPTS="-Xmx4g"
oscal-cli catalog validate large-catalog.xml

# For very large files
export JAVA_OPTS="-Xmx8g"`}
                </code>
              </div>

              <div>
                <div className="flex items-start gap-3 mb-3">
                  <AlertCircle className="h-5 w-5 text-amber-500 mt-0.5" />
                  <h3 className="text-lg font-semibold">Validation Fails</h3>
                </div>
                <p className="text-sm text-muted-foreground mb-2">
                  <strong>Symptom:</strong> Document fails validation with schema errors
                </p>
                <p className="text-sm text-muted-foreground mb-2"><strong>Solution:</strong></p>
                <ul className="list-disc list-inside space-y-1 text-sm text-muted-foreground ml-4">
                  <li>Review error messages - they indicate specific OSCAL schema violations</li>
                  <li>Common issues: missing required fields, invalid UUIDs, incorrect nesting</li>
                  <li>Refer to <a href="https://pages.nist.gov/OSCAL/" target="_blank" rel="noopener noreferrer" className="text-primary hover:underline">OSCAL documentation</a> for model requirements</li>
                </ul>
              </div>
            </CardContent>
          </Card>

          {/* Next Steps */}
          <Card>
            <CardHeader>
              <CardTitle>Next Steps</CardTitle>
            </CardHeader>
            <CardContent className="space-y-3">
              <p className="text-muted-foreground">After successful CLI installation:</p>
              <ul className="list-disc list-inside space-y-2 text-muted-foreground ml-4">
                <li>Read the <Link href="/guide" className="text-primary hover:underline">User Guide</Link></li>
                <li>Try validating OSCAL documents with the CLI</li>
                <li>Set up batch processing scripts for your workflow</li>
                <li>Integrate into your CI/CD pipeline</li>
                <li>Consider <Link href="/guide/deployment/local" className="text-primary hover:underline">Local Deployment</Link> for web interface</li>
                <li>Explore <Link href="/guide/deployment/azure" className="text-primary hover:underline">Azure Deployment</Link> for production</li>
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
                href="https://pages.nist.gov/OSCAL/"
                target="_blank"
                rel="noopener noreferrer"
                className="flex items-center text-primary hover:underline"
              >
                NIST OSCAL Documentation
              </a>

              <a
                href="https://github.com/usnistgov/oscal-content"
                target="_blank"
                rel="noopener noreferrer"
                className="flex items-center text-primary hover:underline"
              >
                OSCAL Content Repository (Examples)
              </a>

              <a
                href="https://github.com/usnistgov/oscal-cli"
                target="_blank"
                rel="noopener noreferrer"
                className="flex items-center text-primary hover:underline"
              >
                OSCAL CLI GitHub Repository
              </a>

              <Link
                href="/guide/automation"
                className="flex items-center text-primary hover:underline"
              >
                API Automation Guide
              </Link>
            </CardContent>
          </Card>
        </div>
      </div>
    </div>
  );
}
