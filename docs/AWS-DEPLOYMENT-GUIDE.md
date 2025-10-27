# AWS Deployment Guide - S3, Elastic Beanstalk & RDS

**Date**: 2025-10-26
**Status**: Planning Document
**Target**: Production deployment on AWS infrastructure

---

## Table of Contents

1. [Architecture Overview](#architecture-overview)
2. [Prerequisites](#prerequisites)
3. [AWS Services Breakdown](#aws-services-breakdown)
4. [Migration Changes Required](#migration-changes-required)
5. [Step-by-Step Deployment](#step-by-step-deployment)
6. [Configuration Files](#configuration-files)
7. [Cost Analysis](#cost-analysis)
8. [Monitoring & Operations](#monitoring--operations)
9. [Troubleshooting](#troubleshooting)

---

## Architecture Overview

```
┌────────────────────────────────────────────────────────────────────┐
│                          AWS Cloud Infrastructure                   │
├────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  User Traffic                                                        │
│       │                                                              │
│       ↓                                                              │
│  ┌─────────────────────────────────────────────────────────┐       │
│  │  CloudFront CDN (Global)                                 │       │
│  │  - SSL/TLS termination                                   │       │
│  │  - Static asset caching                                  │       │
│  │  - DDoS protection (AWS Shield)                          │       │
│  └─────────────────────┬───────────────────────────────────┘       │
│                        │                                             │
│         ┌──────────────┴──────────────┐                             │
│         │                              │                             │
│         ↓                              ↓                             │
│  ┌──────────────┐              ┌─────────────────────────┐          │
│  │  S3 Bucket   │              │  Application Load        │          │
│  │  (Frontend)  │              │  Balancer (ALB)          │          │
│  │              │              │  - Health checks         │          │
│  │  Next.js SSR │              │  - SSL termination       │          │
│  │  Static files│              │  - Auto-scaling target   │          │
│  └──────────────┘              └───────────┬─────────────┘          │
│                                            │                         │
│                                            ↓                         │
│                          ┌─────────────────────────────────┐        │
│                          │  Elastic Beanstalk Environment  │        │
│                          │  - Java 21 (Corretto)           │        │
│                          │  - Auto Scaling Group            │        │
│                          │    • Min: 2 instances            │        │
│                          │    • Max: 6 instances            │        │
│                          │  - EC2 Instance: t3.medium       │        │
│                          │                                  │        │
│                          │  Spring Boot API                 │        │
│                          │  - Port 5000 (internal)          │        │
│                          │  - Health: /actuator/health      │        │
│                          └────────┬────────────────────────┘        │
│                                   │                                  │
│                      ┌────────────┼────────────┐                    │
│                      │                          │                    │
│                      ↓                          ↓                    │
│          ┌────────────────────┐    ┌───────────────────────┐        │
│          │  Amazon RDS         │    │  Amazon S3            │        │
│          │  PostgreSQL 15      │    │  (File Storage)       │        │
│          │                     │    │                       │        │
│          │  - Multi-AZ         │    │  - OSCAL files        │        │
│          │  - Automated backup │    │  - Component defs     │        │
│          │  - Encryption       │    │  - Build artifacts    │        │
│          │  - db.t3.small      │    │  - Lifecycle policies │        │
│          └────────────────────┘    └───────────────────────┘        │
│                                                                       │
│  ┌──────────────────────────────────────────────────────────┐       │
│  │  Supporting Services                                      │       │
│  │  - AWS Secrets Manager (JWT secret, DB credentials)      │       │
│  │  - CloudWatch Logs (Application logs, metrics)           │       │
│  │  - CloudWatch Alarms (Health monitoring, auto-scaling)   │       │
│  │  - Route 53 (DNS management)                             │       │
│  │  - AWS Certificate Manager (SSL/TLS certificates)        │       │
│  │  - VPC (Subnets, Security Groups, NAT Gateway)           │       │
│  └──────────────────────────────────────────────────────────┘       │
└────────────────────────────────────────────────────────────────────┘
```

---

## Prerequisites

### Required Tools

- **AWS CLI v2**: `brew install awscli` (Mac) or [download installer](https://aws.amazon.com/cli/)
- **EB CLI**: `pip install awsebcli` (Elastic Beanstalk CLI)
- **Java 21**: Amazon Corretto or OpenJDK
- **Maven 3.8+**: For building the backend
- **Node.js 18+**: For building the frontend
- **Git**: Version control

### AWS Account Requirements

- AWS account with admin access (or permissions for EB, RDS, S3, CloudFront, Secrets Manager)
- AWS CLI configured with credentials:
  ```bash
  aws configure
  # AWS Access Key ID: YOUR_ACCESS_KEY
  # AWS Secret Access Key: YOUR_SECRET_KEY
  # Default region name: us-east-1
  # Default output format: json
  ```

### Verify Setup

```bash
# Test AWS CLI
aws sts get-caller-identity

# Test EB CLI
eb --version

# Verify Java version
java -version  # Should show 21.x

# Verify Maven
mvn -version

# Verify Node.js
node --version  # Should show v18.x or higher
```

---

## AWS Services Breakdown

### 1. Amazon S3 (Simple Storage Service)

**Purpose**:
- Static frontend hosting (Next.js build output)
- OSCAL file storage (replacing Azure Blob Storage)
- Build artifacts and component definitions

**Buckets Required**:
```
oscal-tools-frontend-prod     # Frontend static files (Next.js)
oscal-tools-files-prod        # User-uploaded OSCAL files
oscal-tools-build-prod        # Component definitions and build artifacts
```

**Key Features**:
- Versioning enabled (for rollback capability)
- Server-side encryption (AES-256)
- Lifecycle policies (auto-delete temp files after 7 days)
- CORS configuration for API access

**Estimated Cost**: $0.023/GB/month storage + $0.09/GB data transfer

---

### 2. AWS Elastic Beanstalk

**Purpose**: Managed platform for Spring Boot application deployment

**Platform**:
- **Java 21 with Corretto** on Amazon Linux 2023
- Managed Tomcat environment (but Spring Boot runs embedded Tomcat)

**Features**:
- Automatic capacity provisioning and load balancing
- Health monitoring with CloudWatch integration
- Managed platform updates
- Easy blue/green deployments
- Integrated with RDS for database connectivity

**Instance Configuration**:
- **Development**: Single instance (t3.micro)
- **Production**: Auto-scaling group (2-6 instances, t3.medium)

**Deployment Methods**:
1. **EB CLI**: `eb deploy` (recommended for dev/staging)
2. **CodePipeline**: CI/CD with GitHub integration (recommended for production)
3. **Console**: Manual upload via AWS Console (not recommended)

**Estimated Cost**:
- Dev: ~$10/month (1 t3.micro)
- Prod: ~$60-180/month (2-6 t3.medium instances)

---

### 3. Amazon RDS (Relational Database Service)

**Purpose**: Managed PostgreSQL database

**Configuration**:

| Environment | Instance Class | Storage | Multi-AZ | Backups |
|-------------|---------------|---------|----------|---------|
| Development | db.t3.micro   | 20 GB   | No       | 7 days  |
| Staging     | db.t3.small   | 50 GB   | No       | 7 days  |
| Production  | db.t3.small   | 100 GB  | Yes      | 30 days |

**Features**:
- PostgreSQL 15.x
- Automated backups with point-in-time recovery
- Encryption at rest (AES-256)
- Encryption in transit (SSL/TLS)
- Automatic minor version upgrades
- Performance Insights enabled

**Security**:
- Database in private subnet (no public access)
- Security group allows access only from Elastic Beanstalk instances
- Master credentials stored in AWS Secrets Manager

**Estimated Cost**:
- Dev: ~$15/month (db.t3.micro, single AZ)
- Prod: ~$60/month (db.t3.small, multi-AZ)

---

### 4. CloudFront (CDN)

**Purpose**: Global content delivery network for frontend

**Features**:
- HTTPS only (HTTP redirects to HTTPS)
- Custom SSL certificate via ACM
- Caching static assets (JS, CSS, images)
- Origin: S3 bucket for frontend
- Custom error pages (404, 500)
- Geo-restriction (optional)

**Cache Behavior**:
```
Path Pattern         | TTL      | Origin
---------------------|----------|------------------
/_next/static/*      | 1 year   | S3 (immutable)
/images/*            | 1 day    | S3
/api/*               | 0        | ALB (no cache)
/*                   | 5 min    | S3 (SSR pages)
```

**Estimated Cost**: $0.085/GB data transfer + $0.01/10,000 requests = ~$10-50/month

---

### 5. AWS Secrets Manager

**Purpose**: Secure storage for sensitive configuration

**Secrets**:
```
oscal-tools/prod/jwt-secret        # JWT signing key (256-bit)
oscal-tools/prod/db-credentials    # RDS master username/password
oscal-tools/prod/s3-access         # S3 access keys (if using IAM user)
```

**Rotation**:
- JWT secret: Manual rotation (update EB environment variables)
- DB credentials: Automatic rotation every 90 days

**Estimated Cost**: $0.40/secret/month + $0.05 per 10,000 API calls = ~$2/month

---

### 6. CloudWatch (Monitoring & Logging)

**Log Groups**:
```
/aws/elasticbeanstalk/oscal-api-prod/var/log/tomcat/catalina.out
/aws/elasticbeanstalk/oscal-api-prod/var/log/eb-engine.log
/aws/rds/instance/oscal-db-prod/postgresql
```

**Metrics**:
- Application: CPU, memory, request count, latency
- Database: Connections, IOPS, CPU, freeable memory
- Auto-scaling: Instance count, health status

**Alarms**:
- High CPU (>80% for 5 min) → SNS notification
- Database connections (>80% max) → SNS notification
- HTTP 5xx errors (>10 in 5 min) → SNS notification

**Estimated Cost**: ~$10-20/month (depends on log volume)

---

### 7. VPC (Virtual Private Cloud)

**Architecture**:

```
VPC: 10.0.0.0/16

Public Subnets (Internet-facing):
  - 10.0.1.0/24 (us-east-1a) - ALB
  - 10.0.2.0/24 (us-east-1b) - ALB

Private Subnets (Application tier):
  - 10.0.10.0/24 (us-east-1a) - EB instances
  - 10.0.11.0/24 (us-east-1b) - EB instances

Private Subnets (Database tier):
  - 10.0.20.0/24 (us-east-1a) - RDS primary
  - 10.0.21.0/24 (us-east-1b) - RDS standby

NAT Gateway: 10.0.1.0/24 (for outbound internet from private subnets)
```

**Security Groups**:

| Name | Inbound Rules | Outbound Rules |
|------|---------------|----------------|
| ALB-SG | 443 (HTTPS) from 0.0.0.0/0 | All to EB-SG |
| EB-SG | 5000 from ALB-SG | All to 0.0.0.0/0 |
| RDS-SG | 5432 from EB-SG | None |

**Estimated Cost**: ~$35/month (NAT Gateway) + $0.045/GB data processed

---

## Migration Changes Required

### 1. Replace Azure Blob Storage with Amazon S3

**Current**: `AzureBlobService.java` uses Azure SDK
**Change**: Create `S3StorageService.java` using AWS SDK

**Dependencies to Add** (`back-end/pom.xml`):

```xml
<!-- Remove Azure Blob Storage -->
<!-- DELETE THIS:
<dependency>
    <groupId>com.azure</groupId>
    <artifactId>azure-storage-blob</artifactId>
    <version>12.25.1</version>
</dependency>
-->

<!-- Add AWS SDK for S3 -->
<dependency>
    <groupId>software.amazon.awssdk</groupId>
    <artifactId>s3</artifactId>
    <version>2.20.140</version>
</dependency>

<!-- Add AWS Secrets Manager (optional, for enhanced security) -->
<dependency>
    <groupId>software.amazon.awssdk</groupId>
    <artifactId>secretsmanager</artifactId>
    <version>2.20.140</version>
</dependency>
```

**Files to Modify**:
1. ✅ Create: `back-end/src/main/java/gov/nist/oscal/tools/api/service/S3StorageService.java`
2. ✅ Update: `back-end/src/main/java/gov/nist/oscal/tools/api/service/FileStorageService.java` (inject S3StorageService instead of AzureBlobService)
3. ✅ Update: `back-end/src/main/java/gov/nist/oscal/tools/api/service/LibraryStorageService.java` (same)
4. ✅ Update: `back-end/src/main/java/gov/nist/oscal/tools/api/service/ComponentDefinitionService.java` (same)
5. ✅ Delete: `back-end/src/main/java/gov/nist/oscal/tools/api/health/AzureBlobStorageHealthIndicator.java`
6. ✅ Create: `back-end/src/main/java/gov/nist/oscal/tools/api/health/S3StorageHealthIndicator.java`

**Configuration Changes** (`application-prod.properties`):

```properties
# REMOVE Azure configuration
# azure.storage.connection-string=...
# azure.storage.container-name=...
# azure.storage.library-container-name=...
# azure.storage.build-container-name=...

# ADD AWS S3 configuration
aws.s3.region=${AWS_REGION:us-east-1}
aws.s3.bucket-files=${AWS_S3_BUCKET_FILES:oscal-tools-files-prod}
aws.s3.bucket-library=${AWS_S3_BUCKET_LIBRARY:oscal-tools-files-prod}
aws.s3.bucket-build=${AWS_S3_BUCKET_BUILD:oscal-tools-build-prod}
aws.s3.build-folder=${AWS_S3_BUILD_FOLDER:build}

# S3 uses IAM roles when running on EC2/Elastic Beanstalk
# No access keys needed in configuration!
```

---

### 2. Database Migration (H2 → PostgreSQL)

**Good News**: Already configured! ✅

The application already has:
- PostgreSQL driver in `pom.xml` (line 71-75)
- Flyway migrations configured (line 84-94)
- Production config using PostgreSQL dialect

**Verification**:
```bash
# Check current schema
cd back-end/src/main/resources/db/migration/
ls -la  # Should show V1__*.sql migration files
```

**Testing Locally** (before AWS deployment):

```bash
# Start PostgreSQL in Docker
docker run --name oscal-postgres \
  -e POSTGRES_DB=oscal_production \
  -e POSTGRES_USER=oscal_user \
  -e POSTGRES_PASSWORD=your-secure-password \
  -p 5432:5432 \
  -d postgres:15

# Update back-end/.env (create if doesn't exist)
cat > back-end/.env << EOF
DB_URL=jdbc:postgresql://localhost:5432/oscal_production
DB_USERNAME=oscal_user
DB_PASSWORD=your-secure-password
JWT_SECRET=dev-secret-key-at-least-32-characters-long-for-jwt-signing
EOF

# Build and run
cd back-end
mvn clean package -DskipTests
mvn spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=prod"

# Check logs for successful Flyway migration
# Should see: "Successfully applied X migrations to schema `public`"
```

---

### 3. Elastic Beanstalk Configuration

**Create Configuration Directory**:

```bash
mkdir -p .ebextensions
mkdir -p .platform/nginx/conf.d
```

**File**: `.ebextensions/01-environment.config`

```yaml
option_settings:
  # Java Environment
  aws:elasticbeanstalk:application:environment:
    SERVER_PORT: 5000
    SPRING_PROFILES_ACTIVE: prod
    AWS_REGION: us-east-1

  # Java Runtime Options
  aws:elasticbeanstalk:container:java:
    Xmx: 512m
    Xms: 256m
    JVM Options: '-XX:+UseG1GC -XX:MaxGCPauseMillis=200'

  # Auto Scaling
  aws:autoscaling:asg:
    MinSize: 2
    MaxSize: 6
    Cooldown: 360

  # Auto Scaling Triggers
  aws:autoscaling:trigger:
    MeasureName: CPUUtilization
    Statistic: Average
    Unit: Percent
    UpperThreshold: 75
    UpperBreachScaleIncrement: 1
    LowerThreshold: 25
    LowerBreachScaleIncrement: -1
    Period: 5
    BreachDuration: 5

  # Load Balancer Health Check
  aws:elasticbeanstalk:application:
    Application Healthcheck URL: /actuator/health

  # Load Balancer
  aws:elbv2:loadbalancer:
    IdleTimeout: 120

  # Instance Type
  aws:autoscaling:launchconfiguration:
    InstanceType: t3.medium
    IamInstanceProfile: aws-elasticbeanstalk-ec2-role
    EC2KeyName: YOUR_KEY_PAIR_NAME  # For SSH access (optional)

  # VPC Configuration (replace with your VPC IDs)
  aws:ec2:vpc:
    VPCId: vpc-xxxxxxxxx
    Subnets: subnet-xxxxxxxx,subnet-yyyyyyyy
    ELBSubnets: subnet-aaaaaaaa,subnet-bbbbbbbb
    AssociatePublicIpAddress: false

  # Rolling Updates
  aws:elasticbeanstalk:command:
    DeploymentPolicy: Rolling
    BatchSizeType: Percentage
    BatchSize: 50

  # Managed Updates
  aws:elasticbeanstalk:managedactions:
    ManagedActionsEnabled: true
    PreferredStartTime: "Sun:03:00"

  aws:elasticbeanstalk:managedactions:platformupdate:
    UpdateLevel: minor
    InstanceRefreshEnabled: true
```

**File**: `.ebextensions/02-logs.config`

```yaml
# CloudWatch Logs Configuration
files:
  "/opt/elasticbeanstalk/tasks/bundlelogs.d/oscal-app.conf":
    mode: "000644"
    owner: root
    group: root
    content: |
      /var/log/tomcat/catalina.out
      /var/log/web.stdout.log

  "/opt/elasticbeanstalk/tasks/taillogs.d/oscal-app.conf":
    mode: "000644"
    owner: root
    group: root
    content: |
      /var/log/tomcat/catalina.out
      /var/log/web.stdout.log

option_settings:
  aws:elasticbeanstalk:cloudwatch:logs:
    StreamLogs: true
    DeleteOnTerminate: false
    RetentionInDays: 30

  aws:elasticbeanstalk:cloudwatch:logs:health:
    HealthStreamingEnabled: true
    DeleteOnTerminate: false
    RetentionInDays: 7
```

**File**: `.ebextensions/03-https-redirect.config`

```yaml
# HTTPS redirect via Application Load Balancer
option_settings:
  aws:elbv2:listener:80:
    ListenerEnabled: true
    Protocol: HTTP
    DefaultProcess: default

  aws:elbv2:listener:443:
    ListenerEnabled: true
    Protocol: HTTPS
    SSLCertificateArns: arn:aws:acm:us-east-1:ACCOUNT_ID:certificate/CERT_ID
    DefaultProcess: default
```

---

### 4. Frontend Deployment to S3

**Build Next.js for Static Export** (if not using SSR):

Update `front-end/next.config.ts`:

```typescript
import type { NextConfig } from 'next'

const nextConfig: NextConfig = {
  output: 'export',  // Static HTML export
  images: {
    unoptimized: true,  // Required for static export
  },
  env: {
    NEXT_PUBLIC_API_URL: process.env.NEXT_PUBLIC_API_URL || 'https://api.oscal-tools.com',
  },
  trailingSlash: true,  // Required for S3 routing
}

export default nextConfig
```

**Build Script** (`deploy-frontend.sh`):

```bash
#!/bin/bash
set -e

# Configuration
BUCKET_NAME="oscal-tools-frontend-prod"
CLOUDFRONT_DIST_ID="E1234567890ABC"  # Your CloudFront distribution ID

echo "Building Next.js frontend..."
cd front-end
npm ci
npm run build

echo "Deploying to S3..."
aws s3 sync out/ s3://${BUCKET_NAME}/ --delete

echo "Invalidating CloudFront cache..."
aws cloudfront create-invalidation \
  --distribution-id ${CLOUDFRONT_DIST_ID} \
  --paths "/*"

echo "Frontend deployment complete!"
echo "URL: https://${BUCKET_NAME}.s3-website-us-east-1.amazonaws.com"
```

**Alternative: Server-Side Rendering (SSR)**

For SSR, you'll need to:
1. Deploy Next.js to **EC2** or **ECS Fargate** (not S3)
2. Use **Application Load Balancer** for routing
3. Configure Next.js to connect to backend API

---

## Step-by-Step Deployment

### Phase 1: AWS Infrastructure Setup (Day 1)

#### 1.1 Create VPC and Subnets

```bash
# Create VPC
VPC_ID=$(aws ec2 create-vpc \
  --cidr-block 10.0.0.0/16 \
  --tag-specifications 'ResourceType=vpc,Tags=[{Key=Name,Value=oscal-tools-vpc}]' \
  --query 'Vpc.VpcId' \
  --output text)

echo "VPC Created: $VPC_ID"

# Enable DNS hostnames
aws ec2 modify-vpc-attribute --vpc-id $VPC_ID --enable-dns-hostnames

# Create Internet Gateway
IGW_ID=$(aws ec2 create-internet-gateway \
  --tag-specifications 'ResourceType=internet-gateway,Tags=[{Key=Name,Value=oscal-tools-igw}]' \
  --query 'InternetGateway.InternetGatewayId' \
  --output text)

# Attach IGW to VPC
aws ec2 attach-internet-gateway --vpc-id $VPC_ID --internet-gateway-id $IGW_ID

# Create Public Subnets
PUBLIC_SUBNET_1=$(aws ec2 create-subnet \
  --vpc-id $VPC_ID \
  --cidr-block 10.0.1.0/24 \
  --availability-zone us-east-1a \
  --tag-specifications 'ResourceType=subnet,Tags=[{Key=Name,Value=oscal-public-1a}]' \
  --query 'Subnet.SubnetId' \
  --output text)

PUBLIC_SUBNET_2=$(aws ec2 create-subnet \
  --vpc-id $VPC_ID \
  --cidr-block 10.0.2.0/24 \
  --availability-zone us-east-1b \
  --tag-specifications 'ResourceType=subnet,Tags=[{Key=Name,Value=oscal-public-1b}]' \
  --query 'Subnet.SubnetId' \
  --output text)

# Create Private Subnets (Application)
PRIVATE_SUBNET_1=$(aws ec2 create-subnet \
  --vpc-id $VPC_ID \
  --cidr-block 10.0.10.0/24 \
  --availability-zone us-east-1a \
  --tag-specifications 'ResourceType=subnet,Tags=[{Key=Name,Value=oscal-private-app-1a}]' \
  --query 'Subnet.SubnetId' \
  --output text)

PRIVATE_SUBNET_2=$(aws ec2 create-subnet \
  --vpc-id $VPC_ID \
  --cidr-block 10.0.11.0/24 \
  --availability-zone us-east-1b \
  --tag-specifications 'ResourceType=subnet,Tags=[{Key=Name,Value=oscal-private-app-1b}]' \
  --query 'Subnet.SubnetId' \
  --output text)

# Create Private Subnets (Database)
DB_SUBNET_1=$(aws ec2 create-subnet \
  --vpc-id $VPC_ID \
  --cidr-block 10.0.20.0/24 \
  --availability-zone us-east-1a \
  --tag-specifications 'ResourceType=subnet,Tags=[{Key=Name,Value=oscal-private-db-1a}]' \
  --query 'Subnet.SubnetId' \
  --output text)

DB_SUBNET_2=$(aws ec2 create-subnet \
  --vpc-id $VPC_ID \
  --cidr-block 10.0.21.0/24 \
  --availability-zone us-east-1b \
  --tag-specifications 'ResourceType=subnet,Tags=[{Key=Name,Value=oscal-private-db-1b}]' \
  --query 'Subnet.SubnetId' \
  --output text)

# Create NAT Gateway
EIP_ALLOC_ID=$(aws ec2 allocate-address \
  --domain vpc \
  --tag-specifications 'ResourceType=elastic-ip,Tags=[{Key=Name,Value=oscal-nat-eip}]' \
  --query 'AllocationId' \
  --output text)

NAT_GW_ID=$(aws ec2 create-nat-gateway \
  --subnet-id $PUBLIC_SUBNET_1 \
  --allocation-id $EIP_ALLOC_ID \
  --tag-specifications 'ResourceType=natgateway,Tags=[{Key=Name,Value=oscal-nat-gw}]' \
  --query 'NatGateway.NatGatewayId' \
  --output text)

# Wait for NAT Gateway
echo "Waiting for NAT Gateway to become available..."
aws ec2 wait nat-gateway-available --nat-gateway-ids $NAT_GW_ID

# Create Route Tables
PUBLIC_RT_ID=$(aws ec2 create-route-table \
  --vpc-id $VPC_ID \
  --tag-specifications 'ResourceType=route-table,Tags=[{Key=Name,Value=oscal-public-rt}]' \
  --query 'RouteTable.RouteTableId' \
  --output text)

PRIVATE_RT_ID=$(aws ec2 create-route-table \
  --vpc-id $VPC_ID \
  --tag-specifications 'ResourceType=route-table,Tags=[{Key=Name,Value=oscal-private-rt}]' \
  --query 'RouteTable.RouteTableId' \
  --output text)

# Add routes
aws ec2 create-route --route-table-id $PUBLIC_RT_ID --destination-cidr-block 0.0.0.0/0 --gateway-id $IGW_ID
aws ec2 create-route --route-table-id $PRIVATE_RT_ID --destination-cidr-block 0.0.0.0/0 --nat-gateway-id $NAT_GW_ID

# Associate subnets with route tables
aws ec2 associate-route-table --subnet-id $PUBLIC_SUBNET_1 --route-table-id $PUBLIC_RT_ID
aws ec2 associate-route-table --subnet-id $PUBLIC_SUBNET_2 --route-table-id $PUBLIC_RT_ID
aws ec2 associate-route-table --subnet-id $PRIVATE_SUBNET_1 --route-table-id $PRIVATE_RT_ID
aws ec2 associate-route-table --subnet-id $PRIVATE_SUBNET_2 --route-table-id $PRIVATE_RT_ID
aws ec2 associate-route-table --subnet-id $DB_SUBNET_1 --route-table-id $PRIVATE_RT_ID
aws ec2 associate-route-table --subnet-id $DB_SUBNET_2 --route-table-id $PRIVATE_RT_ID

echo "VPC Infrastructure Created Successfully!"
echo "VPC ID: $VPC_ID"
echo "Public Subnets: $PUBLIC_SUBNET_1, $PUBLIC_SUBNET_2"
echo "Private App Subnets: $PRIVATE_SUBNET_1, $PRIVATE_SUBNET_2"
echo "Private DB Subnets: $DB_SUBNET_1, $DB_SUBNET_2"
```

#### 1.2 Create S3 Buckets

```bash
# Create S3 buckets
BUCKETS=(
  "oscal-tools-files-prod"
  "oscal-tools-build-prod"
  "oscal-tools-frontend-prod"
)

for BUCKET in "${BUCKETS[@]}"; do
  echo "Creating bucket: $BUCKET"

  # Create bucket
  aws s3api create-bucket \
    --bucket $BUCKET \
    --region us-east-1 \
    --acl private

  # Enable versioning
  aws s3api put-bucket-versioning \
    --bucket $BUCKET \
    --versioning-configuration Status=Enabled

  # Enable encryption
  aws s3api put-bucket-encryption \
    --bucket $BUCKET \
    --server-side-encryption-configuration '{
      "Rules": [{
        "ApplyServerSideEncryptionByDefault": {
          "SSEAlgorithm": "AES256"
        },
        "BucketKeyEnabled": true
      }]
    }'

  # Block public access
  aws s3api put-public-access-block \
    --bucket $BUCKET \
    --public-access-block-configuration \
      "BlockPublicAcls=true,IgnorePublicAcls=true,BlockPublicPolicy=true,RestrictPublicBuckets=true"

  echo "Bucket $BUCKET created and configured"
done

# Configure CORS for file buckets
aws s3api put-bucket-cors \
  --bucket oscal-tools-files-prod \
  --cors-configuration '{
    "CORSRules": [{
      "AllowedOrigins": ["https://oscal-tools.com"],
      "AllowedMethods": ["GET", "PUT", "POST", "DELETE"],
      "AllowedHeaders": ["*"],
      "MaxAgeSeconds": 3000
    }]
  }'

# Configure lifecycle policy for temp files
aws s3api put-bucket-lifecycle-configuration \
  --bucket oscal-tools-files-prod \
  --lifecycle-configuration '{
    "Rules": [{
      "Id": "DeleteTempFilesAfter7Days",
      "Status": "Enabled",
      "Prefix": "temp/",
      "Expiration": { "Days": 7 }
    }]
  }'
```

#### 1.3 Create RDS PostgreSQL Instance

```bash
# Create DB subnet group
aws rds create-db-subnet-group \
  --db-subnet-group-name oscal-db-subnet-group \
  --db-subnet-group-description "Subnet group for OSCAL Tools database" \
  --subnet-ids $DB_SUBNET_1 $DB_SUBNET_2 \
  --tags "Key=Name,Value=oscal-db-subnet-group"

# Create security group for RDS
RDS_SG_ID=$(aws ec2 create-security-group \
  --group-name oscal-rds-sg \
  --description "Security group for OSCAL Tools RDS" \
  --vpc-id $VPC_ID \
  --query 'GroupId' \
  --output text)

# Note: We'll add ingress rule after creating EB environment

# Generate secure password
DB_PASSWORD=$(openssl rand -base64 32)

# Create RDS instance
aws rds create-db-instance \
  --db-instance-identifier oscal-db-prod \
  --db-instance-class db.t3.small \
  --engine postgres \
  --engine-version 15.4 \
  --master-username oscal_admin \
  --master-user-password "$DB_PASSWORD" \
  --allocated-storage 100 \
  --storage-type gp3 \
  --storage-encrypted \
  --db-subnet-group-name oscal-db-subnet-group \
  --vpc-security-group-ids $RDS_SG_ID \
  --backup-retention-period 30 \
  --preferred-backup-window "03:00-04:00" \
  --preferred-maintenance-window "sun:04:00-sun:05:00" \
  --multi-az \
  --publicly-accessible false \
  --enable-iam-database-authentication \
  --enable-performance-insights \
  --performance-insights-retention-period 7 \
  --tags "Key=Name,Value=oscal-db-prod" "Key=Environment,Value=production"

echo "RDS instance creation initiated. This will take 10-15 minutes..."
echo "Database password (save this securely): $DB_PASSWORD"

# Wait for database to be available
aws rds wait db-instance-available --db-instance-identifier oscal-db-prod

# Get database endpoint
DB_ENDPOINT=$(aws rds describe-db-instances \
  --db-instance-identifier oscal-db-prod \
  --query 'DBInstances[0].Endpoint.Address' \
  --output text)

echo "Database endpoint: $DB_ENDPOINT"
```

#### 1.4 Store Secrets in AWS Secrets Manager

```bash
# Generate JWT secret (256-bit)
JWT_SECRET=$(openssl rand -base64 32)

# Store database credentials
aws secretsmanager create-secret \
  --name oscal-tools/prod/db-credentials \
  --description "OSCAL Tools production database credentials" \
  --secret-string "{
    \"username\": \"oscal_admin\",
    \"password\": \"$DB_PASSWORD\",
    \"engine\": \"postgres\",
    \"host\": \"$DB_ENDPOINT\",
    \"port\": 5432,
    \"dbname\": \"postgres\"
  }" \
  --tags "Key=Application,Value=oscal-tools" "Key=Environment,Value=production"

# Store JWT secret
aws secretsmanager create-secret \
  --name oscal-tools/prod/jwt-secret \
  --description "OSCAL Tools JWT signing secret" \
  --secret-string "$JWT_SECRET" \
  --tags "Key=Application,Value=oscal-tools" "Key=Environment,Value=production"

echo "Secrets stored in AWS Secrets Manager"
```

---

### Phase 2: Application Deployment (Day 2)

#### 2.1 Update Backend Code for S3

See implementation files in next section.

#### 2.2 Build Backend

```bash
cd back-end

# Update pom.xml (remove Azure, add AWS SDK)
# See "Migration Changes Required" section above

# Build JAR
mvn clean package -DskipTests

# Verify JAR was created
ls -lh target/*.jar
# Should show: oscal-cli-api-1.0.0-SNAPSHOT.jar (~100MB)
```

#### 2.3 Initialize Elastic Beanstalk

```bash
# From project root
cd back-end

# Initialize EB application
eb init oscal-api \
  --platform "Corretto 21 running on 64bit Amazon Linux 2023" \
  --region us-east-1

# Create production environment
eb create oscal-api-prod \
  --instance-type t3.medium \
  --envvars \
    SPRING_PROFILES_ACTIVE=prod,\
    SERVER_PORT=5000,\
    AWS_REGION=us-east-1,\
    AWS_S3_BUCKET_FILES=oscal-tools-files-prod,\
    AWS_S3_BUCKET_BUILD=oscal-tools-build-prod,\
    DB_URL=jdbc:postgresql://$DB_ENDPOINT:5432/postgres,\
    DB_USERNAME=oscal_admin,\
    DB_PASSWORD=$DB_PASSWORD,\
    JWT_SECRET=$JWT_SECRET,\
    CORS_ALLOWED_ORIGINS=https://oscal-tools.com

# This will take 10-15 minutes to provision resources

# Check status
eb status

# View logs
eb logs
```

#### 2.4 Configure Security Group Access

```bash
# Get EB instance security group
EB_SG_ID=$(aws ec2 describe-security-groups \
  --filters "Name=tag:Name,Values=oscal-api-prod" \
  --query 'SecurityGroups[0].GroupId' \
  --output text)

# Allow EB instances to access RDS
aws ec2 authorize-security-group-ingress \
  --group-id $RDS_SG_ID \
  --protocol tcp \
  --port 5432 \
  --source-group $EB_SG_ID

echo "Security group configured: EB can now access RDS"
```

#### 2.5 Test Backend Deployment

```bash
# Get EB environment URL
EB_URL=$(eb status | grep "CNAME" | awk '{print $2}')

# Test health endpoint
curl https://$EB_URL/actuator/health

# Expected response:
# {"status":"UP"}

# Test API endpoints (requires authentication)
curl https://$EB_URL/api/health
```

---

### Phase 3: Frontend Deployment (Day 2-3)

#### 3.1 Build and Deploy Frontend

```bash
cd front-end

# Update environment variables
cat > .env.production << EOF
NEXT_PUBLIC_API_URL=https://api.oscal-tools.com
EOF

# Build Next.js
npm ci
npm run build

# Deploy to S3
aws s3 sync out/ s3://oscal-tools-frontend-prod/ --delete

# Enable static website hosting
aws s3 website s3://oscal-tools-frontend-prod/ \
  --index-document index.html \
  --error-document 404.html
```

#### 3.2 Create CloudFront Distribution

```bash
# Create CloudFront distribution
aws cloudfront create-distribution \
  --distribution-config file://cloudfront-config.json

# cloudfront-config.json:
{
  "CallerReference": "oscal-tools-$(date +%s)",
  "Comment": "OSCAL Tools Frontend CDN",
  "Enabled": true,
  "Origins": {
    "Quantity": 1,
    "Items": [{
      "Id": "S3-oscal-frontend",
      "DomainName": "oscal-tools-frontend-prod.s3.us-east-1.amazonaws.com",
      "S3OriginConfig": {
        "OriginAccessIdentity": ""
      }
    }]
  },
  "DefaultCacheBehavior": {
    "TargetOriginId": "S3-oscal-frontend",
    "ViewerProtocolPolicy": "redirect-to-https",
    "AllowedMethods": {
      "Quantity": 2,
      "Items": ["GET", "HEAD"]
    },
    "MinTTL": 0,
    "DefaultTTL": 300,
    "MaxTTL": 86400
  }
}
```

---

## Configuration Files

### S3StorageService.java Implementation

Create: `back-end/src/main/java/gov/nist/oscal/tools/api/service/S3StorageService.java`

```java
package gov.nist.oscal.tools.api.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service for handling Amazon S3 storage operations for component definitions
 * Replaces AzureBlobService with AWS S3
 */
@Service
public class S3StorageService {

    private static final Logger logger = LoggerFactory.getLogger(S3StorageService.class);

    @Value("${aws.s3.region:us-east-1}")
    private String awsRegion;

    @Value("${aws.s3.bucket-build:oscal-tools-build-prod}")
    private String buildBucketName;

    @Value("${aws.s3.build-folder:build}")
    private String buildFolder;

    @Value("${file.upload-dir:./uploads}")
    private String uploadDir;

    private S3Client s3Client;
    private boolean useLocalStorage = false;
    private Path localBuildPath;

    @PostConstruct
    public void init() {
        try {
            logger.info("Initializing S3 client for region: {}", awsRegion);

            // Create S3 client (uses IAM role credentials when running on EC2/EB)
            s3Client = S3Client.builder()
                    .region(Region.of(awsRegion))
                    .credentialsProvider(DefaultCredentialsProvider.create())
                    .build();

            // Test S3 connection by checking if bucket exists
            HeadBucketRequest headBucketRequest = HeadBucketRequest.builder()
                    .bucket(buildBucketName)
                    .build();

            s3Client.headBucket(headBucketRequest);
            logger.info("S3 bucket '{}' is accessible", buildBucketName);

        } catch (S3Exception e) {
            if (e.statusCode() == 404) {
                logger.warn("S3 bucket '{}' does not exist. Creating bucket...", buildBucketName);
                createBucket();
            } else if (e.statusCode() == 403) {
                logger.error("Access denied to S3 bucket '{}'. Check IAM permissions.", buildBucketName);
                fallbackToLocalStorage();
            } else {
                logger.error("Failed to connect to S3: {}", e.getMessage());
                fallbackToLocalStorage();
            }
        } catch (Exception e) {
            logger.error("Failed to initialize S3 client: {}", e.getMessage(), e);
            fallbackToLocalStorage();
        }
    }

    private void createBucket() {
        try {
            CreateBucketRequest createBucketRequest = CreateBucketRequest.builder()
                    .bucket(buildBucketName)
                    .build();

            s3Client.createBucket(createBucketRequest);
            logger.info("Created S3 bucket: {}", buildBucketName);

            // Enable versioning
            PutBucketVersioningRequest versioningRequest = PutBucketVersioningRequest.builder()
                    .bucket(buildBucketName)
                    .versioningConfiguration(VersioningConfiguration.builder()
                            .status(BucketVersioningStatus.ENABLED)
                            .build())
                    .build();

            s3Client.putBucketVersioning(versioningRequest);
            logger.info("Enabled versioning for bucket: {}", buildBucketName);

        } catch (S3Exception e) {
            logger.error("Failed to create S3 bucket: {}", e.getMessage(), e);
            fallbackToLocalStorage();
        }
    }

    private void fallbackToLocalStorage() {
        logger.warn("Falling back to local file storage");
        useLocalStorage = true;
        localBuildPath = Paths.get(uploadDir, buildFolder);
        try {
            Files.createDirectories(localBuildPath);
            logger.info("Local storage initialized at: {}", localBuildPath.toAbsolutePath());
        } catch (IOException e) {
            logger.error("Failed to create local directory: {}", e.getMessage(), e);
            throw new RuntimeException("Could not initialize storage", e);
        }
    }

    @PreDestroy
    public void cleanup() {
        if (s3Client != null) {
            s3Client.close();
            logger.info("S3 client closed");
        }
    }

    /**
     * Upload a component definition to S3
     */
    public String uploadComponent(String username, String filename, String jsonContent, Map<String, String> metadata) {
        String key = buildKey(username, filename);

        if (useLocalStorage) {
            saveToLocalStorage(jsonContent, key);
            return key;
        }

        try {
            byte[] contentBytes = jsonContent.getBytes(StandardCharsets.UTF_8);

            PutObjectRequest.Builder requestBuilder = PutObjectRequest.builder()
                    .bucket(buildBucketName)
                    .key(key)
                    .contentType("application/json")
                    .contentLength((long) contentBytes.length);

            if (metadata != null && !metadata.isEmpty()) {
                requestBuilder.metadata(metadata);
            }

            PutObjectRequest request = requestBuilder.build();

            s3Client.putObject(request, RequestBody.fromBytes(contentBytes));

            logger.info("Uploaded component to S3: {}/{}", buildBucketName, key);
            return key;

        } catch (S3Exception e) {
            logger.error("Failed to upload to S3: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to upload component", e);
        }
    }

    /**
     * Download a component definition from S3
     */
    public String downloadComponent(String key) {
        if (useLocalStorage) {
            return getFromLocalStorage(key);
        }

        try {
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(buildBucketName)
                    .key(key)
                    .build();

            byte[] objectBytes = s3Client.getObjectAsBytes(getObjectRequest).asByteArray();

            logger.info("Downloaded component from S3: {}/{}", buildBucketName, key);
            return new String(objectBytes, StandardCharsets.UTF_8);

        } catch (NoSuchKeyException e) {
            logger.error("Component not found in S3: {}", key);
            throw new RuntimeException("Component not found: " + key);
        } catch (S3Exception e) {
            logger.error("Failed to download from S3: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to download component", e);
        }
    }

    /**
     * List all components for a specific user
     */
    public List<String> listUserComponents(String username) {
        String prefix = buildFolder + "/" + username + "/";

        if (useLocalStorage) {
            return listFromLocalStorage(username);
        }

        List<String> componentPaths = new ArrayList<>();

        try {
            ListObjectsV2Request listRequest = ListObjectsV2Request.builder()
                    .bucket(buildBucketName)
                    .prefix(prefix)
                    .build();

            ListObjectsV2Response listResponse = s3Client.listObjectsV2(listRequest);

            componentPaths = listResponse.contents().stream()
                    .map(S3Object::key)
                    .filter(key -> !key.endsWith("/"))  // Exclude "directory" markers
                    .collect(Collectors.toList());

            logger.info("Listed {} components for user: {}", componentPaths.size(), username);
            return componentPaths;

        } catch (S3Exception e) {
            logger.error("Failed to list components from S3: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to list user components", e);
        }
    }

    /**
     * Delete a component definition from S3
     */
    public boolean deleteComponent(String key) {
        if (useLocalStorage) {
            return deleteFromLocalStorage(key);
        }

        try {
            DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                    .bucket(buildBucketName)
                    .key(key)
                    .build();

            s3Client.deleteObject(deleteRequest);

            logger.info("Deleted component from S3: {}/{}", buildBucketName, key);
            return true;

        } catch (S3Exception e) {
            logger.error("Failed to delete from S3: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Check if a component exists in S3
     */
    public boolean componentExists(String key) {
        if (useLocalStorage) {
            Path filePath = localBuildPath.resolve(key.replace(buildFolder + "/", ""));
            return Files.exists(filePath);
        }

        try {
            HeadObjectRequest headRequest = HeadObjectRequest.builder()
                    .bucket(buildBucketName)
                    .key(key)
                    .build();

            s3Client.headObject(headRequest);
            return true;

        } catch (NoSuchKeyException e) {
            return false;
        } catch (S3Exception e) {
            logger.error("Failed to check if component exists: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Get file size in bytes
     */
    public long getFileSize(String key) {
        if (useLocalStorage) {
            try {
                Path filePath = localBuildPath.resolve(key.replace(buildFolder + "/", ""));
                return Files.size(filePath);
            } catch (IOException e) {
                logger.error("Failed to get file size from local storage: {}", e.getMessage(), e);
                return 0;
            }
        }

        try {
            HeadObjectRequest headRequest = HeadObjectRequest.builder()
                    .bucket(buildBucketName)
                    .key(key)
                    .build();

            HeadObjectResponse response = s3Client.headObject(headRequest);
            return response.contentLength();

        } catch (S3Exception e) {
            logger.error("Failed to get file size from S3: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to get file size", e);
        }
    }

    /**
     * Build S3 key for component definition
     * Format: build/{username}/{filename}
     */
    public String buildKey(String username, String filename) {
        String sanitizedFileName = sanitizeFileName(filename);
        return String.format("%s/%s/%s", buildFolder, username, sanitizedFileName);
    }

    /**
     * Sanitize filename to remove problematic characters
     */
    private String sanitizeFileName(String fileName) {
        return fileName.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    /**
     * Check if storage is configured
     */
    public boolean isConfigured() {
        return s3Client != null || useLocalStorage;
    }

    // Local storage fallback methods (same as AzureBlobService)

    private void saveToLocalStorage(String content, String key) {
        try {
            String relativePath = key.replace(buildFolder + "/", "");
            Path filePath = localBuildPath.resolve(relativePath);
            Files.createDirectories(filePath.getParent());
            Files.writeString(filePath, content, StandardCharsets.UTF_8);
            logger.info("Saved component to local storage: {}", filePath.toAbsolutePath());
        } catch (IOException e) {
            logger.error("Failed to save component to local storage: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to save component", e);
        }
    }

    private String getFromLocalStorage(String key) {
        try {
            String relativePath = key.replace(buildFolder + "/", "");
            Path filePath = localBuildPath.resolve(relativePath);
            if (!Files.exists(filePath)) {
                throw new RuntimeException("Component not found: " + key);
            }
            return Files.readString(filePath, StandardCharsets.UTF_8);
        } catch (IOException e) {
            logger.error("Failed to read component from local storage: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to read component", e);
        }
    }

    private List<String> listFromLocalStorage(String username) {
        List<String> componentPaths = new ArrayList<>();
        try {
            Path userPath = localBuildPath.resolve(username);
            if (Files.exists(userPath)) {
                Files.walk(userPath)
                        .filter(Files::isRegularFile)
                        .forEach(path -> {
                            String relativePath = localBuildPath.relativize(path).toString();
                            componentPaths.add(buildFolder + "/" + relativePath);
                        });
            }
            logger.info("Listed {} components for user from local storage: {}", componentPaths.size(), username);
            return componentPaths;
        } catch (IOException e) {
            logger.error("Failed to list user components from local storage: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to list user components", e);
        }
    }

    private boolean deleteFromLocalStorage(String key) {
        try {
            String relativePath = key.replace(buildFolder + "/", "");
            Path filePath = localBuildPath.resolve(relativePath);
            if (Files.exists(filePath)) {
                Files.delete(filePath);
                logger.info("Deleted component from local storage: {}", filePath.toAbsolutePath());
                return true;
            }
            return false;
        } catch (IOException e) {
            logger.error("Failed to delete component from local storage: {}", e.getMessage(), e);
            return false;
        }
    }
}
```

---

## Cost Analysis

### Monthly Cost Breakdown (Production Environment)

| Service | Configuration | Monthly Cost (USD) |
|---------|--------------|-------------------|
| **Elastic Beanstalk** | 2 x t3.medium (minimum) | $60 |
| **Auto Scaling** | Up to 6 x t3.medium (peak) | $120 (average) |
| **Application Load Balancer** | 1 ALB + traffic | $25 |
| **RDS PostgreSQL** | db.t3.small, Multi-AZ, 100GB | $75 |
| **S3 Storage** | 100 GB + requests | $5 |
| **CloudFront** | 1 TB data transfer | $85 |
| **NAT Gateway** | 1 NAT + data processed | $40 |
| **CloudWatch Logs** | 10 GB logs/month | $8 |
| **Secrets Manager** | 3 secrets | $1.20 |
| **Route 53** | 1 hosted zone + queries | $1 |
| **Data Transfer** | Outbound data | $15 |
| **TOTAL** | | **$435/month** |

### Cost Optimization Strategies

1. **Use Reserved Instances** for RDS and EC2 (save 40-60%)
2. **Enable S3 Intelligent-Tiering** (save 20-30% on storage)
3. **Implement CloudFront caching** (reduce origin requests)
4. **Use Spot Instances for non-critical workloads** (save 70%)
5. **Set up auto-scaling schedules** (scale down during off-hours)

### Development Environment (~$50/month)

- 1 x t3.micro EB instance: $8
- db.t3.micro RDS (single-AZ): $15
- S3 storage (minimal): $2
- No CloudFront: $0
- Reduced data transfer: $5

---

## Monitoring & Operations

### CloudWatch Dashboards

Create custom dashboard: `oscal-tools-production`

**Metrics to Track**:
- **Application**: Request count, latency (p50, p95, p99), error rate
- **EC2 Instances**: CPU utilization, network in/out, disk I/O
- **Database**: Connections, read/write IOPS, CPU, free storage
- **Load Balancer**: Target response time, healthy/unhealthy hosts
- **S3**: Bucket size, request count, 4xx/5xx errors

### CloudWatch Alarms

```bash
# High CPU alarm
aws cloudwatch put-metric-alarm \
  --alarm-name oscal-api-high-cpu \
  --alarm-description "Alert when CPU > 80% for 5 minutes" \
  --metric-name CPUUtilization \
  --namespace AWS/EC2 \
  --statistic Average \
  --period 300 \
  --threshold 80 \
  --comparison-operator GreaterThanThreshold \
  --evaluation-periods 1 \
  --alarm-actions arn:aws:sns:us-east-1:123456789012:oscal-alerts

# Database connection alarm
aws cloudwatch put-metric-alarm \
  --alarm-name oscal-db-connections \
  --alarm-description "Alert when DB connections > 80%" \
  --metric-name DatabaseConnections \
  --namespace AWS/RDS \
  --statistic Average \
  --period 300 \
  --threshold 16 \
  --comparison-operator GreaterThanThreshold \
  --evaluation-periods 1 \
  --dimensions Name=DBInstanceIdentifier,Value=oscal-db-prod \
  --alarm-actions arn:aws:sns:us-east-1:123456789012:oscal-alerts
```

### Log Analysis

```bash
# Query CloudWatch Logs Insights
aws logs start-query \
  --log-group-name /aws/elasticbeanstalk/oscal-api-prod/var/log/tomcat/catalina.out \
  --start-time $(date -u -d '1 hour ago' +%s) \
  --end-time $(date -u +%s) \
  --query-string '
    fields @timestamp, @message
    | filter @message like /ERROR/
    | sort @timestamp desc
    | limit 100
  '
```

---

## Troubleshooting

### Issue: EB deployment fails with "502 Bad Gateway"

**Cause**: Application not starting on port 5000 or health check failing

**Solution**:
```bash
# Check EB logs
eb logs --all

# Verify SERVER_PORT environment variable
eb printenv

# Check if app is listening on correct port
eb ssh
sudo netstat -tlnp | grep 5000
```

### Issue: Database connection timeout

**Cause**: Security group not configured or wrong DB endpoint

**Solution**:
```bash
# Verify RDS security group
aws ec2 describe-security-groups --group-ids $RDS_SG_ID

# Test DB connection from EB instance
eb ssh
psql -h $DB_ENDPOINT -U oscal_admin -d postgres
```

### Issue: S3 access denied

**Cause**: IAM role missing S3 permissions

**Solution**:
```bash
# Attach S3 policy to EB instance role
aws iam attach-role-policy \
  --role-name aws-elasticbeanstalk-ec2-role \
  --policy-arn arn:aws:iam::aws:policy/AmazonS3FullAccess
```

### Issue: High latency from backend API

**Cause**: Database connection pool exhausted or slow queries

**Solution**:
```bash
# Check database performance insights
aws rds describe-db-instances \
  --db-instance-identifier oscal-db-prod \
  --query 'DBInstances[0].PerformanceInsightsEnabled'

# Review slow query log
# Enable in RDS parameter group: log_statement = 'all', log_min_duration_statement = 1000
```

---

## Next Steps

1. **Set up CI/CD Pipeline** with AWS CodePipeline
2. **Implement Blue/Green Deployments** for zero-downtime updates
3. **Add Web Application Firewall (WAF)** for enhanced security
4. **Enable AWS X-Ray** for distributed tracing
5. **Implement Disaster Recovery** with cross-region replication

---

**Document Version**: 1.0
**Last Updated**: 2025-10-26
**Maintained By**: OSCAL Tools Team
