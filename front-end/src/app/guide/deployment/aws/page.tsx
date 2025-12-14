import Link from 'next/link';
import { ArrowLeft, Cloud, Lock, Database, Workflow, AlertCircle, CheckCircle2, DollarSign, BarChart3, Server } from 'lucide-react';
import { Card, CardHeader, CardTitle, CardContent } from '@/components/ui/card';

export default function AWSDeploymentGuidePage() {
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
            <Cloud className="h-8 w-8 text-orange-500" />
            <h1 className="text-4xl font-bold">AWS Deployment Guide</h1>
          </div>
          <p className="text-lg text-muted-foreground">
            Deploy OSCAL Tools to AWS using Elastic Beanstalk, S3, and RDS for a production-ready, auto-scaling environment
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
              <a href="#architecture" className="text-primary hover:underline">Architecture</a>
              <a href="#services" className="text-primary hover:underline">AWS Services</a>
              <a href="#prerequisites" className="text-primary hover:underline">Prerequisites</a>
              <a href="#quick-start" className="text-primary hover:underline">Quick Start</a>
              <a href="#deployment-steps" className="text-primary hover:underline">Deployment Steps</a>
              <a href="#monitoring" className="text-primary hover:underline">Monitoring & Operations</a>
              <a href="#cost" className="text-primary hover:underline">Cost Estimation</a>
              <a href="#troubleshooting" className="text-primary hover:underline">Troubleshooting</a>
              <a href="#detailed-docs" className="text-primary hover:underline">Detailed Documentation</a>
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
                This guide provides a comprehensive deployment strategy for running OSCAL Tools on <strong>Amazon Web Services (AWS)</strong>.
                The deployment uses traditional, battle-tested AWS services for reliability and cost-effectiveness.
              </p>

              <div>
                <h3 className="text-xl font-semibold mb-3">Key Features</h3>
                <div className="space-y-2">
                  <div className="flex items-start gap-3">
                    <CheckCircle2 className="h-5 w-5 text-green-500 mt-0.5" />
                    <div>
                      <span className="font-medium">Auto-Scaling with Elastic Beanstalk</span>
                      <span className="text-muted-foreground"> - Automatically scale from 2-6 instances based on load</span>
                    </div>
                  </div>
                  <div className="flex items-start gap-3">
                    <CheckCircle2 className="h-5 w-5 text-green-500 mt-0.5" />
                    <div>
                      <span className="font-medium">S3 Storage</span>
                      <span className="text-muted-foreground"> - Reliable, scalable file storage replacing Azure Blob</span>
                    </div>
                  </div>
                  <div className="flex items-start gap-3">
                    <CheckCircle2 className="h-5 w-5 text-green-500 mt-0.5" />
                    <div>
                      <span className="font-medium">Multi-AZ RDS PostgreSQL</span>
                      <span className="text-muted-foreground"> - High availability database with automated backups</span>
                    </div>
                  </div>
                  <div className="flex items-start gap-3">
                    <CheckCircle2 className="h-5 w-5 text-green-500 mt-0.5" />
                    <div>
                      <span className="font-medium">CloudWatch Monitoring</span>
                      <span className="text-muted-foreground"> - Comprehensive logs, metrics, and alarms</span>
                    </div>
                  </div>
                  <div className="flex items-start gap-3">
                    <CheckCircle2 className="h-5 w-5 text-green-500 mt-0.5" />
                    <div>
                      <span className="font-medium">No Code Rewrite Required</span>
                      <span className="text-muted-foreground"> - Keeps your existing Java Spring Boot application</span>
                    </div>
                  </div>
                </div>
              </div>

              <div className="bg-blue-500/10 border border-blue-500/20 p-4 rounded">
                <p className="text-sm font-semibold mb-2 text-blue-600 dark:text-blue-400">Why AWS?</p>
                <ul className="list-disc list-inside space-y-1 text-sm text-muted-foreground ml-4">
                  <li>Most widely adopted cloud platform</li>
                  <li>Extensive documentation and community support</li>
                  <li>Cost-effective for steady-state workloads</li>
                  <li>Excellent integration with CI/CD tools</li>
                </ul>
              </div>
            </CardContent>
          </Card>

          {/* Architecture */}
          <Card id="architecture">
            <CardHeader>
              <CardTitle>Architecture Overview</CardTitle>
            </CardHeader>
            <CardContent className="space-y-4">
              <div className="bg-muted p-6 rounded overflow-x-auto">
                <pre className="text-xs font-mono whitespace-pre text-muted-foreground">
{`
┌────────────────────────────────────────────────────────────────┐
│                      AWS Cloud Infrastructure                   │
├────────────────────────────────────────────────────────────────┤
│                                                                  │
│  User Traffic → CloudFront CDN → S3 (Frontend)                  │
│                        ↓                                         │
│               Application Load Balancer                         │
│                        ↓                                         │
│           ┌────────────────────────────┐                       │
│           │  Elastic Beanstalk         │                       │
│           │  Java 21 (Corretto)        │                       │
│           │  Auto Scaling Group        │                       │
│           │  • Min: 2 instances        │                       │
│           │  • Max: 6 instances        │                       │
│           │  • Instance: t3.medium     │                       │
│           └────────┬───────────────────┘                       │
│                    │                                            │
│         ┌──────────┼──────────┐                               │
│         ↓                     ↓                                │
│    ┌─────────┐        ┌──────────────┐                       │
│    │   RDS   │        │  Amazon S3   │                       │
│    │  Multi-AZ│        │  File Storage│                       │
│    │PostgreSQL│        │  • oscal-files                       │
│    │  15.x   │        │  • oscal-build│                       │
│    └─────────┘        └──────────────┘                       │
│                                                                 │
│  Supporting Services:                                          │
│  • AWS Secrets Manager (JWT, DB credentials)                  │
│  • CloudWatch (Logs, Metrics, Alarms)                         │
│  • Route 53 (DNS)                                             │
│  • Certificate Manager (SSL/TLS)                              │
│  • VPC (Networking, Security Groups)                          │
└────────────────────────────────────────────────────────────────┘
`}
                </pre>
              </div>
            </CardContent>
          </Card>

          {/* AWS Services */}
          <Card id="services">
            <CardHeader>
              <CardTitle>AWS Services Breakdown</CardTitle>
            </CardHeader>
            <CardContent className="space-y-4">
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                <div className="border rounded-lg p-4">
                  <div className="flex items-center gap-2 mb-2">
                    <Server className="h-5 w-5 text-orange-500" />
                    <h4 className="font-semibold">Elastic Beanstalk</h4>
                  </div>
                  <p className="text-sm text-muted-foreground mb-2">
                    Managed platform for Java applications with automatic capacity provisioning, load balancing, and auto-scaling.
                  </p>
                  <ul className="text-xs text-muted-foreground space-y-1">
                    <li>• Java 21 with Corretto</li>
                    <li>• Rolling deployments</li>
                    <li>• Health monitoring</li>
                  </ul>
                </div>

                <div className="border rounded-lg p-4">
                  <div className="flex items-center gap-2 mb-2">
                    <Database className="h-5 w-5 text-orange-500" />
                    <h4 className="font-semibold">RDS PostgreSQL</h4>
                  </div>
                  <p className="text-sm text-muted-foreground mb-2">
                    Managed relational database with automated backups, encryption, and multi-AZ support.
                  </p>
                  <ul className="text-xs text-muted-foreground space-y-1">
                    <li>• PostgreSQL 15.x</li>
                    <li>• Multi-AZ deployment</li>
                    <li>• 30-day backups</li>
                  </ul>
                </div>

                <div className="border rounded-lg p-4">
                  <div className="flex items-center gap-2 mb-2">
                    <Cloud className="h-5 w-5 text-orange-500" />
                    <h4 className="font-semibold">Amazon S3</h4>
                  </div>
                  <p className="text-sm text-muted-foreground mb-2">
                    Object storage for OSCAL files, component definitions, and frontend static assets.
                  </p>
                  <ul className="text-xs text-muted-foreground space-y-1">
                    <li>• 99.999999999% durability</li>
                    <li>• Versioning enabled</li>
                    <li>• Lifecycle policies</li>
                  </ul>
                </div>

                <div className="border rounded-lg p-4">
                  <div className="flex items-center gap-2 mb-2">
                    <BarChart3 className="h-5 w-5 text-orange-500" />
                    <h4 className="font-semibold">CloudWatch</h4>
                  </div>
                  <p className="text-sm text-muted-foreground mb-2">
                    Monitoring, logging, and alerting for application health and performance metrics.
                  </p>
                  <ul className="text-xs text-muted-foreground space-y-1">
                    <li>• Log aggregation</li>
                    <li>• Custom dashboards</li>
                    <li>• Automated alarms</li>
                  </ul>
                </div>

                <div className="border rounded-lg p-4">
                  <div className="flex items-center gap-2 mb-2">
                    <Lock className="h-5 w-5 text-orange-500" />
                    <h4 className="font-semibold">Secrets Manager</h4>
                  </div>
                  <p className="text-sm text-muted-foreground mb-2">
                    Secure storage for JWT secrets, database credentials, and API keys.
                  </p>
                  <ul className="text-xs text-muted-foreground space-y-1">
                    <li>• Automatic rotation</li>
                    <li>• Encryption at rest</li>
                    <li>• Fine-grained access</li>
                  </ul>
                </div>

                <div className="border rounded-lg p-4">
                  <div className="flex items-center gap-2 mb-2">
                    <Cloud className="h-5 w-5 text-orange-500" />
                    <h4 className="font-semibold">CloudFront CDN</h4>
                  </div>
                  <p className="text-sm text-muted-foreground mb-2">
                    Global content delivery network for frontend assets with edge caching.
                  </p>
                  <ul className="text-xs text-muted-foreground space-y-1">
                    <li>• HTTPS only</li>
                    <li>• Custom SSL certificate</li>
                    <li>• DDoS protection</li>
                  </ul>
                </div>
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
                <h3 className="text-lg font-semibold mb-3">Required Tools</h3>
                <div className="space-y-4">
                  <div>
                    <h4 className="font-medium mb-2">1. AWS CLI (version 2.x)</h4>
                    <code className="block bg-muted p-3 rounded text-sm font-mono whitespace-pre overflow-x-auto">
{`# macOS
brew install awscli

# Windows
msiexec.exe /i https://awscli.amazonaws.com/AWSCLIV2.msi

# Linux
curl "https://awscli.amazonaws.com/awscli-exe-linux-x86_64.zip" -o "awscliv2.zip"
unzip awscliv2.zip
sudo ./aws/install

# Verify
aws --version`}
                    </code>
                  </div>

                  <div>
                    <h4 className="font-medium mb-2">2. EB CLI (Elastic Beanstalk CLI)</h4>
                    <code className="block bg-muted p-3 rounded text-sm font-mono whitespace-pre overflow-x-auto">
{`# Install via pip
pip install awsebcli

# Verify
eb --version`}
                    </code>
                  </div>

                  <div>
                    <h4 className="font-medium mb-2">3. Java 21 & Maven</h4>
                    <code className="block bg-muted p-3 rounded text-sm font-mono whitespace-pre overflow-x-auto">
{`# macOS
brew install openjdk@21 maven

# Verify
java -version  # Should show 21.x
mvn -version`}
                    </code>
                  </div>
                </div>
              </div>

              <div>
                <h3 className="text-lg font-semibold mb-3">Required Accounts</h3>
                <ul className="list-disc list-inside space-y-2 text-muted-foreground ml-4">
                  <li><strong>AWS Account</strong> with permissions for Elastic Beanstalk, RDS, S3, CloudWatch, etc.</li>
                  <li><strong>IAM User</strong> with programmatic access (access key + secret key)</li>
                </ul>
              </div>

              <div className="bg-green-500/10 border border-green-500/20 p-4 rounded">
                <p className="text-sm font-semibold mb-2 text-green-600 dark:text-green-400">AWS Free Tier:</p>
                <p className="text-sm text-muted-foreground mb-2">
                  New AWS accounts include 12 months of free tier benefits:
                </p>
                <ul className="list-disc list-inside space-y-1 text-sm text-muted-foreground ml-4">
                  <li>750 hours/month of t2.micro EC2 instances</li>
                  <li>750 hours/month of db.t2.micro RDS</li>
                  <li>5 GB of S3 standard storage</li>
                  <li>More: <a href="https://aws.amazon.com/free/" target="_blank" rel="noopener noreferrer" className="text-primary hover:underline">aws.amazon.com/free</a></li>
                </ul>
              </div>
            </CardContent>
          </Card>

          {/* Quick Start */}
          <Card id="quick-start">
            <CardHeader>
              <CardTitle>Quick Start (5 Commands)</CardTitle>
            </CardHeader>
            <CardContent className="space-y-4">
              <p className="text-muted-foreground">
                Get started quickly with these automated deployment scripts:
              </p>
              <code className="block bg-muted p-4 rounded text-sm font-mono whitespace-pre overflow-x-auto">
{`# 1. Configure AWS credentials
aws configure
# Enter your Access Key ID, Secret Access Key, and region (us-east-1)

# 2. Clone repository (if not already)
git clone https://github.com/RegScale/oscal-hub.git
cd oscal-cli

# 3. Deploy backend to Elastic Beanstalk
./deploy-backend-aws.sh prod

# 4. Deploy frontend to S3 + CloudFront
./deploy-frontend-aws.sh prod

# 5. Open your application!
# Backend: https://oscal-api-prod.us-east-1.elasticbeanstalk.com
# Frontend: https://your-cloudfront-domain.cloudfront.net`}
              </code>

              <div className="bg-amber-500/10 border border-amber-500/20 p-4 rounded">
                <p className="text-sm font-semibold mb-2 text-amber-600 dark:text-amber-400">Note:</p>
                <p className="text-sm text-muted-foreground">
                  The deployment scripts will prompt you for required information (database password, JWT secret, etc.).
                  For a fully automated deployment with infrastructure-as-code, see the detailed documentation.
                </p>
              </div>
            </CardContent>
          </Card>

          {/* Deployment Steps */}
          <Card id="deployment-steps">
            <CardHeader>
              <CardTitle>Detailed Deployment Steps</CardTitle>
            </CardHeader>
            <CardContent className="space-y-6">
              <div>
                <h3 className="text-lg font-semibold mb-3 flex items-center gap-2">
                  <span className="flex items-center justify-center w-6 h-6 rounded-full bg-primary text-primary-foreground text-sm">1</span>
                  Infrastructure Setup
                </h3>
                <ul className="list-disc list-inside space-y-1 text-sm text-muted-foreground ml-10">
                  <li>Create VPC with public/private subnets</li>
                  <li>Set up NAT Gateway for private subnet internet access</li>
                  <li>Create security groups for ALB, EB instances, and RDS</li>
                  <li>Configure Route 53 for custom domain (optional)</li>
                </ul>
              </div>

              <div>
                <h3 className="text-lg font-semibold mb-3 flex items-center gap-2">
                  <span className="flex items-center justify-center w-6 h-6 rounded-full bg-primary text-primary-foreground text-sm">2</span>
                  Create S3 Buckets
                </h3>
                <code className="block bg-muted p-3 rounded text-xs font-mono overflow-x-auto">
{`aws s3 mb s3://oscal-tools-files-prod --region us-east-1
aws s3 mb s3://oscal-tools-build-prod --region us-east-1
aws s3 mb s3://oscal-tools-frontend-prod --region us-east-1`}
                </code>
              </div>

              <div>
                <h3 className="text-lg font-semibold mb-3 flex items-center gap-2">
                  <span className="flex items-center justify-center w-6 h-6 rounded-full bg-primary text-primary-foreground text-sm">3</span>
                  Create RDS PostgreSQL Database
                </h3>
                <code className="block bg-muted p-3 rounded text-xs font-mono overflow-x-auto">
{`aws rds create-db-instance \\
  --db-instance-identifier oscal-db-prod \\
  --db-instance-class db.t3.small \\
  --engine postgres \\
  --engine-version 15.4 \\
  --master-username oscal_admin \\
  --master-user-password <SECURE_PASSWORD> \\
  --allocated-storage 100 \\
  --multi-az`}
                </code>
              </div>

              <div>
                <h3 className="text-lg font-semibold mb-3 flex items-center gap-2">
                  <span className="flex items-center justify-center w-6 h-6 rounded-full bg-primary text-primary-foreground text-sm">4</span>
                  Store Secrets in Secrets Manager
                </h3>
                <code className="block bg-muted p-3 rounded text-xs font-mono overflow-x-auto">
{`# Generate and store JWT secret
aws secretsmanager create-secret \\
  --name oscal-tools/prod/jwt-secret \\
  --secret-string "$(openssl rand -base64 32)"

# Store database credentials
aws secretsmanager create-secret \\
  --name oscal-tools/prod/db-credentials \\
  --secret-string '{"username":"oscal_admin","password":"..."}'`}
                </code>
              </div>

              <div>
                <h3 className="text-lg font-semibold mb-3 flex items-center gap-2">
                  <span className="flex items-center justify-center w-6 h-6 rounded-full bg-primary text-primary-foreground text-sm">5</span>
                  Deploy Backend to Elastic Beanstalk
                </h3>
                <code className="block bg-muted p-3 rounded text-xs font-mono overflow-x-auto">
{`cd back-end
mvn clean package -DskipTests
eb init oscal-api --platform "Corretto 21" --region us-east-1
eb create oscal-api-prod --instance-type t3.medium
eb deploy`}
                </code>
              </div>

              <div>
                <h3 className="text-lg font-semibold mb-3 flex items-center gap-2">
                  <span className="flex items-center justify-center w-6 h-6 rounded-full bg-primary text-primary-foreground text-sm">6</span>
                  Deploy Frontend to S3 + CloudFront
                </h3>
                <code className="block bg-muted p-3 rounded text-xs font-mono overflow-x-auto">
{`cd front-end
npm ci
npm run build
aws s3 sync out/ s3://oscal-tools-frontend-prod/ --delete
aws cloudfront create-invalidation --distribution-id E1234 --paths "/*"`}
                </code>
              </div>
            </CardContent>
          </Card>

          {/* Monitoring */}
          <Card id="monitoring">
            <CardHeader>
              <CardTitle>Monitoring & Operations</CardTitle>
            </CardHeader>
            <CardContent className="space-y-4">
              <div>
                <h3 className="text-lg font-semibold mb-3">CloudWatch Dashboards</h3>
                <p className="text-sm text-muted-foreground mb-3">
                  Monitor your application health and performance:
                </p>
                <ul className="list-disc list-inside space-y-1 text-sm text-muted-foreground ml-4">
                  <li><strong>Application Metrics:</strong> Request count, latency (p50, p95, p99), error rate</li>
                  <li><strong>Instance Metrics:</strong> CPU utilization, memory usage, disk I/O</li>
                  <li><strong>Database Metrics:</strong> Connections, IOPS, CPU, free storage</li>
                  <li><strong>Load Balancer:</strong> Target response time, healthy/unhealthy hosts</li>
                </ul>
              </div>

              <div>
                <h3 className="text-lg font-semibold mb-3">CloudWatch Alarms</h3>
                <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
                  <div className="bg-muted p-3 rounded">
                    <h4 className="font-medium mb-1 text-sm">High CPU</h4>
                    <p className="text-xs text-muted-foreground">Alert when CPU &gt; 80% for 5 minutes</p>
                  </div>
                  <div className="bg-muted p-3 rounded">
                    <h4 className="font-medium mb-1 text-sm">Database Connections</h4>
                    <p className="text-xs text-muted-foreground">Alert when connections &gt; 80% of max</p>
                  </div>
                  <div className="bg-muted p-3 rounded">
                    <h4 className="font-medium mb-1 text-sm">HTTP 5xx Errors</h4>
                    <p className="text-xs text-muted-foreground">Alert when &gt; 10 errors in 5 minutes</p>
                  </div>
                  <div className="bg-muted p-3 rounded">
                    <h4 className="font-medium mb-1 text-sm">Health Check Failures</h4>
                    <p className="text-xs text-muted-foreground">Alert when health check fails 3 times</p>
                  </div>
                </div>
              </div>

              <div>
                <h3 className="text-lg font-semibold mb-3">Useful Commands</h3>
                <code className="block bg-muted p-4 rounded text-sm font-mono whitespace-pre overflow-x-auto">
{`# View application logs
eb logs oscal-api-prod --stream

# Check environment status
eb status oscal-api-prod

# SSH to instance (debugging)
eb ssh oscal-api-prod

# Scale instances
eb scale 4 oscal-api-prod

# View CloudWatch logs
aws logs tail /aws/elasticbeanstalk/oscal-api-prod/var/log/tomcat/catalina.out --follow`}
                </code>
              </div>
            </CardContent>
          </Card>

          {/* Cost Estimation */}
          <Card id="cost">
            <CardHeader>
              <CardTitle className="flex items-center gap-2">
                <DollarSign className="h-5 w-5" />
                Cost Estimation
              </CardTitle>
            </CardHeader>
            <CardContent className="space-y-4">
              <p className="text-muted-foreground">
                Estimated monthly AWS costs for different deployment tiers:
              </p>

              <div className="overflow-x-auto">
                <table className="w-full text-sm">
                  <thead className="border-b">
                    <tr>
                      <th className="text-left py-2 pr-4">Service</th>
                      <th className="text-left py-2 pr-4">Development</th>
                      <th className="text-left py-2">Production</th>
                    </tr>
                  </thead>
                  <tbody className="text-muted-foreground">
                    <tr className="border-b">
                      <td className="py-2 pr-4">Elastic Beanstalk</td>
                      <td className="py-2 pr-4">1 x t3.micro: $8</td>
                      <td className="py-2">2-6 x t3.medium: $60-180</td>
                    </tr>
                    <tr className="border-b">
                      <td className="py-2 pr-4">RDS PostgreSQL</td>
                      <td className="py-2 pr-4">db.t3.micro (single-AZ): $15</td>
                      <td className="py-2">db.t3.small (multi-AZ): $60</td>
                    </tr>
                    <tr className="border-b">
                      <td className="py-2 pr-4">S3 Storage</td>
                      <td className="py-2 pr-4">10 GB: $0.23</td>
                      <td className="py-2">100 GB: $2.30</td>
                    </tr>
                    <tr className="border-b">
                      <td className="py-2 pr-4">CloudFront</td>
                      <td className="py-2 pr-4">$0 (minimal traffic)</td>
                      <td className="py-2">1 TB transfer: $85</td>
                    </tr>
                    <tr className="border-b">
                      <td className="py-2 pr-4">ALB</td>
                      <td className="py-2 pr-4">$0 (use EB default)</td>
                      <td className="py-2">$25</td>
                    </tr>
                    <tr className="border-b">
                      <td className="py-2 pr-4">NAT Gateway</td>
                      <td className="py-2 pr-4">$0 (public subnet)</td>
                      <td className="py-2">$35</td>
                    </tr>
                    <tr className="border-b">
                      <td className="py-2 pr-4">CloudWatch</td>
                      <td className="py-2 pr-4">$2</td>
                      <td className="py-2">$10</td>
                    </tr>
                    <tr className="border-b">
                      <td className="py-2 pr-4">Secrets Manager</td>
                      <td className="py-2 pr-4">$1.20</td>
                      <td className="py-2">$1.20</td>
                    </tr>
                    <tr className="border-b font-medium">
                      <td className="py-2 pr-4">Total (monthly)</td>
                      <td className="py-2 pr-4">~$26-30</td>
                      <td className="py-2">~$280-400</td>
                    </tr>
                  </tbody>
                </table>
              </div>

              <div className="bg-green-500/10 border border-green-500/20 p-4 rounded">
                <p className="text-sm font-semibold mb-2 text-green-600 dark:text-green-400">Cost Savings Tips:</p>
                <ul className="list-disc list-inside space-y-1 text-sm text-muted-foreground ml-4">
                  <li>Use Reserved Instances (1-3 year commitment) - Save 40-60%</li>
                  <li>Enable S3 Intelligent-Tiering - Save 20-30% on storage</li>
                  <li>Use Spot Instances for dev/test - Save up to 70%</li>
                  <li>Set up auto-scaling schedules - Scale down during off-hours</li>
                  <li>Monitor with AWS Cost Explorer and set budget alerts</li>
                </ul>
              </div>
            </CardContent>
          </Card>

          {/* Troubleshooting */}
          <Card id="troubleshooting">
            <CardHeader>
              <CardTitle>Common Issues & Solutions</CardTitle>
            </CardHeader>
            <CardContent className="space-y-6">
              <div>
                <div className="flex items-start gap-3 mb-3">
                  <AlertCircle className="h-5 w-5 text-amber-500 mt-0.5" />
                  <h3 className="text-lg font-semibold">EB deployment fails with "502 Bad Gateway"</h3>
                </div>
                <p className="text-sm text-muted-foreground mb-2">
                  <strong>Cause:</strong> Application not starting on port 5000 or health check failing
                </p>
                <p className="text-sm text-muted-foreground mb-2"><strong>Solution:</strong></p>
                <code className="block bg-muted p-3 rounded text-xs font-mono overflow-x-auto mb-2">
{`# Check logs
eb logs oscal-api-prod --all

# Verify SERVER_PORT environment variable
eb printenv oscal-api-prod

# SSH to instance and check if app is running
eb ssh oscal-api-prod
sudo netstat -tlnp | grep 5000`}
                </code>
              </div>

              <div>
                <div className="flex items-start gap-3 mb-3">
                  <AlertCircle className="h-5 w-5 text-amber-500 mt-0.5" />
                  <h3 className="text-lg font-semibold">Database connection timeout</h3>
                </div>
                <p className="text-sm text-muted-foreground mb-2">
                  <strong>Cause:</strong> Security group not configured or wrong DB endpoint
                </p>
                <p className="text-sm text-muted-foreground mb-2"><strong>Solution:</strong></p>
                <ul className="list-disc list-inside space-y-1 text-sm text-muted-foreground ml-4">
                  <li>Verify RDS security group allows inbound from EB security group on port 5432</li>
                  <li>Check database endpoint is correct in environment variables</li>
                  <li>Ensure EB instances and RDS are in same VPC</li>
                  <li>Test connection: <code className="bg-muted px-1.5 py-0.5 rounded text-xs">psql -h DB_ENDPOINT -U oscal_admin -d postgres</code></li>
                </ul>
              </div>

              <div>
                <div className="flex items-start gap-3 mb-3">
                  <AlertCircle className="h-5 w-5 text-amber-500 mt-0.5" />
                  <h3 className="text-lg font-semibold">S3 access denied errors</h3>
                </div>
                <p className="text-sm text-muted-foreground mb-2">
                  <strong>Cause:</strong> IAM role missing S3 permissions
                </p>
                <p className="text-sm text-muted-foreground mb-2"><strong>Solution:</strong></p>
                <code className="block bg-muted p-3 rounded text-xs font-mono overflow-x-auto">
{`# Attach S3 policy to EB instance role
aws iam attach-role-policy \\
  --role-name aws-elasticbeanstalk-ec2-role \\
  --policy-arn arn:aws:iam::aws:policy/AmazonS3FullAccess`}
                </code>
              </div>
            </CardContent>
          </Card>

          {/* Detailed Documentation */}
          <Card id="detailed-docs">
            <CardHeader>
              <CardTitle>Complete Technical Documentation</CardTitle>
            </CardHeader>
            <CardContent className="space-y-4">
              <p className="text-muted-foreground">
                For comprehensive step-by-step instructions, infrastructure scripts, and advanced configuration:
              </p>

              <div className="bg-blue-500/10 border border-blue-500/20 p-6 rounded">
                <h3 className="text-lg font-semibold mb-3">📘 Full AWS Deployment Guide</h3>
                <p className="text-sm text-muted-foreground mb-4">
                  The complete technical guide includes:
                </p>
                <ul className="list-disc list-inside space-y-2 text-sm text-muted-foreground ml-4 mb-4">
                  <li>Complete VPC and subnet setup scripts</li>
                  <li>Detailed Elastic Beanstalk configuration files</li>
                  <li>S3StorageService.java implementation code</li>
                  <li>Database migration procedures</li>
                  <li>Monitoring dashboard setup</li>
                  <li>CI/CD pipeline configuration</li>
                  <li>Security best practices</li>
                </ul>
                <a
                  href="https://github.com/RegScale/oscal-hub/blob/main/docs/AWS-DEPLOYMENT-GUIDE.md"
                  target="_blank"
                  rel="noopener noreferrer"
                  className="inline-flex items-center gap-2 text-primary hover:underline font-medium"
                >
                  View Complete AWS Deployment Guide on GitHub →
                </a>
              </div>

              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                <div className="border rounded-lg p-4">
                  <h4 className="font-semibold mb-2">Deployment Scripts</h4>
                  <p className="text-sm text-muted-foreground mb-3">
                    Automated scripts in the repository:
                  </p>
                  <ul className="text-xs text-muted-foreground space-y-1">
                    <li>• <code className="bg-muted px-1 py-0.5 rounded">deploy-backend-aws.sh</code></li>
                    <li>• <code className="bg-muted px-1 py-0.5 rounded">deploy-frontend-aws.sh</code></li>
                    <li>• <code className="bg-muted px-1 py-0.5 rounded">back-end/.ebextensions/</code></li>
                  </ul>
                </div>

                <div className="border rounded-lg p-4">
                  <h4 className="font-semibold mb-2">Configuration Files</h4>
                  <p className="text-sm text-muted-foreground mb-3">
                    Reference configuration examples:
                  </p>
                  <ul className="text-xs text-muted-foreground space-y-1">
                    <li>• Elastic Beanstalk environment config</li>
                    <li>• CloudWatch Logs setup</li>
                    <li>• HTTPS redirect configuration</li>
                    <li>• IAM permissions for S3 access</li>
                  </ul>
                </div>
              </div>
            </CardContent>
          </Card>

          {/* Additional Resources */}
          <Card>
            <CardHeader>
              <CardTitle>Additional Resources</CardTitle>
            </CardHeader>
            <CardContent className="space-y-3">
              <a
                href="https://docs.aws.amazon.com/elasticbeanstalk/"
                target="_blank"
                rel="noopener noreferrer"
                className="flex items-center text-primary hover:underline"
              >
                AWS Elastic Beanstalk Documentation
              </a>

              <a
                href="https://docs.aws.amazon.com/AmazonRDS/latest/UserGuide/"
                target="_blank"
                rel="noopener noreferrer"
                className="flex items-center text-primary hover:underline"
              >
                Amazon RDS PostgreSQL Documentation
              </a>

              <a
                href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/"
                target="_blank"
                rel="noopener noreferrer"
                className="flex items-center text-primary hover:underline"
              >
                Amazon S3 User Guide
              </a>

              <a
                href="https://docs.aws.amazon.com/cloudwatch/"
                target="_blank"
                rel="noopener noreferrer"
                className="flex items-center text-primary hover:underline"
              >
                Amazon CloudWatch Documentation
              </a>

              <Link
                href="/guide/deployment/local"
                className="flex items-center text-primary hover:underline"
              >
                Local Deployment Guide
              </Link>

              <Link
                href="/guide/deployment/azure"
                className="flex items-center text-primary hover:underline"
              >
                Azure Deployment Guide
              </Link>
            </CardContent>
          </Card>
        </div>
      </div>
    </div>
  );
}
