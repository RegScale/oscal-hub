import Link from 'next/link';
import { Button } from '@/components/ui/button';
import { Card, CardHeader, CardTitle, CardDescription } from '@/components/ui/card';
import { FileCheck, ArrowRightLeft, Library, BarChart3, ShieldCheck, Download, Cloud, BookOpen } from 'lucide-react';
import { SystemHealth } from '@/components/SystemHealth';

export function Hero() {
  return (
    <div className="container mx-auto px-4 py-16">
      {/* Hero Section */}
      <div className="text-center mb-16">
        <h1 className="text-6xl font-bold mb-6 bg-gradient-to-r from-blue-500 to-purple-500 bg-clip-text text-transparent">
          Welcome to OSCAL Hub
        </h1>
        <p className="text-xl text-muted-foreground mb-8 max-w-2xl mx-auto">
          Your comprehensive platform for working with OSCAL (Open Security Controls Assessment Language) documents.
          Validate, convert, and manage security compliance content with ease.
        </p>
        <div className="flex gap-4 justify-center">
          <Link href="/login">
            <Button size="lg" className="text-lg px-8">
              Get Started
            </Button>
          </Link>
          <a
            href="https://pages.nist.gov/OSCAL/"
            target="_blank"
            rel="noopener noreferrer"
          >
            <Button size="lg" variant="outline" className="text-lg px-8">
              Learn About OSCAL
            </Button>
          </a>
        </div>
      </div>

      {/* Features Grid */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-5 gap-6 mb-16">
        <Card className="border-primary/20">
          <CardHeader className="space-y-4">
            <div className="p-3 rounded-lg bg-primary/10 w-fit">
              <FileCheck className="h-8 w-8 text-primary" />
            </div>
            <div>
              <CardTitle className="text-xl mb-2">Validate</CardTitle>
              <CardDescription>
                Ensure your OSCAL documents comply with schema constraints and validation rules
              </CardDescription>
            </div>
          </CardHeader>
        </Card>

        <Card className="border-primary/20">
          <CardHeader className="space-y-4">
            <div className="p-3 rounded-lg bg-primary/10 w-fit">
              <ArrowRightLeft className="h-8 w-8 text-primary" />
            </div>
            <div>
              <CardTitle className="text-xl mb-2">Convert</CardTitle>
              <CardDescription>
                Seamlessly convert between XML, JSON, and YAML formats with side-by-side preview
              </CardDescription>
            </div>
          </CardHeader>
        </Card>

        <Card className="border-primary/20">
          <CardHeader className="space-y-4">
            <div className="p-3 rounded-lg bg-primary/10 w-fit">
              <Library className="h-8 w-8 text-primary" />
            </div>
            <div>
              <CardTitle className="text-xl mb-2">Library</CardTitle>
              <CardDescription>
                Browse, share, and download example OSCAL documents from the community
              </CardDescription>
            </div>
          </CardHeader>
        </Card>

        <Card className="border-primary/20">
          <CardHeader className="space-y-4">
            <div className="p-3 rounded-lg bg-primary/10 w-fit">
              <BarChart3 className="h-8 w-8 text-primary" />
            </div>
            <div>
              <CardTitle className="text-xl mb-2">Visualize</CardTitle>
              <CardDescription>
                Explore and understand OSCAL documents through interactive data visualizations
              </CardDescription>
            </div>
          </CardHeader>
        </Card>

        <Card className="border-primary/20">
          <CardHeader className="space-y-4">
            <div className="p-3 rounded-lg bg-primary/10 w-fit">
              <ShieldCheck className="h-8 w-8 text-primary" />
            </div>
            <div>
              <CardTitle className="text-xl mb-2">Authorizations</CardTitle>
              <CardDescription>
                Create and manage system authorization documents with customizable templates
              </CardDescription>
            </div>
          </CardHeader>
        </Card>
      </div>

      {/* Why OSCAL Section */}
      <div className="bg-muted/50 rounded-lg p-8 mb-16">
        <h2 className="text-3xl font-bold mb-4 text-center">Why OSCAL?</h2>
        <p className="text-muted-foreground text-center max-w-3xl mx-auto mb-6">
          OSCAL (Open Security Controls Assessment Language) is a standardized, machine-readable format
          for security controls, enabling automation of compliance and risk management processes.
        </p>
        <div className="grid grid-cols-1 md:grid-cols-3 gap-4 mt-8">
          <div className="text-center">
            <h3 className="font-semibold mb-2">Standardized</h3>
            <p className="text-sm text-muted-foreground">
              NIST-maintained standard for security controls
            </p>
          </div>
          <div className="text-center">
            <h3 className="font-semibold mb-2">Machine-Readable</h3>
            <p className="text-sm text-muted-foreground">
              Enables automation and tool integration
            </p>
          </div>
          <div className="text-center">
            <h3 className="font-semibold mb-2">Comprehensive</h3>
            <p className="text-sm text-muted-foreground">
              Covers the full compliance lifecycle
            </p>
          </div>
        </div>
      </div>

      {/* System Health Section */}
      <div className="max-w-2xl mx-auto mb-16">
        <SystemHealth />
      </div>

      {/* Deployment Options Section */}
      <div className="bg-gradient-to-r from-blue-50/50 to-purple-50/50 dark:from-blue-950/20 dark:to-purple-950/20 rounded-lg p-8 mb-16">
        <h2 className="text-3xl font-bold mb-6 text-center">Deployment Options</h2>
        <p className="text-muted-foreground text-center max-w-3xl mx-auto mb-8">
          Choose how you want to run OSCAL Hub - locally on your laptop for testing, or deploy to Azure for production use
        </p>
        <div className="grid grid-cols-1 md:grid-cols-2 gap-6 max-w-4xl mx-auto">
          {/* Local Deployment */}
          <Card className="border-blue-200 dark:border-blue-800 hover:shadow-lg transition-shadow">
            <CardHeader className="space-y-4">
              <div className="p-3 rounded-lg bg-blue-500/10 w-fit">
                <Download className="h-8 w-8 text-blue-500" />
              </div>
              <div>
                <CardTitle className="text-2xl mb-2">Local Deployment</CardTitle>
                <CardDescription className="text-base mb-4">
                  Run OSCAL Hub on your local machine or VM in minutes. Perfect for testing, development, and offline use.
                </CardDescription>
                <div className="space-y-2 text-sm text-muted-foreground mb-4">
                  <div className="flex items-center">
                    <span className="text-green-500 mr-2">✓</span>
                    Free to use
                  </div>
                  <div className="flex items-center">
                    <span className="text-green-500 mr-2">✓</span>
                    5-minute setup with automated script
                  </div>
                  <div className="flex items-center">
                    <span className="text-green-500 mr-2">✓</span>
                    Works offline after initial download
                  </div>
                  <div className="flex items-center">
                    <span className="text-green-500 mr-2">✓</span>
                    Includes PostgreSQL and pgAdmin
                  </div>
                </div>
                <a
                  href="https://raw.githubusercontent.com/usnistgov/oscal-cli/main/docs/LOCAL-DEPLOYMENT-GUIDE.md"
                  target="_blank"
                  rel="noopener noreferrer"
                  className="inline-block"
                >
                  <Button className="w-full" variant="outline">
                    <BookOpen className="h-4 w-4 mr-2" />
                    View Local Deployment Guide
                  </Button>
                </a>
              </div>
            </CardHeader>
          </Card>

          {/* Azure Deployment */}
          <Card className="border-purple-200 dark:border-purple-800 hover:shadow-lg transition-shadow">
            <CardHeader className="space-y-4">
              <div className="p-3 rounded-lg bg-purple-500/10 w-fit">
                <Cloud className="h-8 w-8 text-purple-500" />
              </div>
              <div>
                <CardTitle className="text-2xl mb-2">Azure Deployment</CardTitle>
                <CardDescription className="text-base mb-4">
                  Deploy to Azure with automated CI/CD, Terraform, and secure Key Vault integration for production environments.
                </CardDescription>
                <div className="space-y-2 text-sm text-muted-foreground mb-4">
                  <div className="flex items-center">
                    <span className="text-green-500 mr-2">✓</span>
                    Automated CI/CD with GitHub Actions
                  </div>
                  <div className="flex items-center">
                    <span className="text-green-500 mr-2">✓</span>
                    Infrastructure as Code (Terraform)
                  </div>
                  <div className="flex items-center">
                    <span className="text-green-500 mr-2">✓</span>
                    Secure secrets with Azure Key Vault
                  </div>
                  <div className="flex items-center">
                    <span className="text-green-500 mr-2">✓</span>
                    Auto database migrations on deploy
                  </div>
                </div>
                <a
                  href="https://raw.githubusercontent.com/usnistgov/oscal-cli/main/docs/AZURE-DEPLOYMENT-GUIDE.md"
                  target="_blank"
                  rel="noopener noreferrer"
                  className="inline-block"
                >
                  <Button className="w-full" variant="outline">
                    <BookOpen className="h-4 w-4 mr-2" />
                    View Azure Deployment Guide
                  </Button>
                </a>
              </div>
            </CardHeader>
          </Card>
        </div>
        <div className="text-center mt-8">
          <p className="text-sm text-muted-foreground">
            Both deployment options include the full feature set of OSCAL Hub
          </p>
        </div>
      </div>

      {/* CTA Section */}
      <div className="text-center">
        <h2 className="text-3xl font-bold mb-4">Ready to Get Started?</h2>
        <p className="text-muted-foreground mb-6">
          Create an account to start working with OSCAL documents today
        </p>
        <Link href="/login">
          <Button size="lg" className="text-lg px-12">
            Sign Up Now
          </Button>
        </Link>
      </div>
    </div>
  );
}
