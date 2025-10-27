import Link from 'next/link';
import { Button } from '@/components/ui/button';
import { Card, CardHeader, CardTitle, CardDescription } from '@/components/ui/card';
import { FileCheck, ArrowRightLeft, Library, BarChart3, ShieldCheck, Download, Cloud, BookOpen, Zap, Users, RefreshCw, Shield } from 'lucide-react';
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

      {/* Why OSCAL & System Health Section */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6 mb-16">
        {/* Benefits Section */}
        <Card className="border-primary/20">
          <CardHeader>
            <CardTitle className="flex items-center text-2xl">
              <Zap className="h-6 w-6 mr-2 text-primary" />
              Why Use OSCAL?
            </CardTitle>
          </CardHeader>
          <div className="px-6 pb-6 space-y-4">
            <p className="text-muted-foreground text-sm mb-4">
              OSCAL transforms security compliance from manual documentation to machine-readable automation,
              enabling faster and more reliable compliance processes.
            </p>
            <ul className="space-y-4">
              <li className="flex items-start">
                <Shield className="h-5 w-5 mr-3 text-primary mt-0.5 flex-shrink-0" />
                <div>
                  <span className="font-semibold text-foreground block mb-1">Standardized Compliance</span>
                  <p className="text-sm text-muted-foreground">
                    Consistent format across all security frameworks and controls, eliminating inconsistencies
                  </p>
                </div>
              </li>
              <li className="flex items-start">
                <RefreshCw className="h-5 w-5 mr-3 text-primary mt-0.5 flex-shrink-0" />
                <div>
                  <span className="font-semibold text-foreground block mb-1">Automation Ready</span>
                  <p className="text-sm text-muted-foreground">
                    Machine-readable format enables automated validation, reporting, and continuous compliance
                  </p>
                </div>
              </li>
              <li className="flex items-start">
                <Users className="h-5 w-5 mr-3 text-primary mt-0.5 flex-shrink-0" />
                <div>
                  <span className="font-semibold text-foreground block mb-1">Team Collaboration</span>
                  <p className="text-sm text-muted-foreground">
                    Share and reuse compliance data across teams and organizations with ease
                  </p>
                </div>
              </li>
              <li className="flex items-start">
                <Zap className="h-5 w-5 mr-3 text-primary mt-0.5 flex-shrink-0" />
                <div>
                  <span className="font-semibold text-foreground block mb-1">Faster ATO Process</span>
                  <p className="text-sm text-muted-foreground">
                    Reduce time to Authority to Operate with streamlined, automated documentation
                  </p>
                </div>
              </li>
            </ul>
          </div>
        </Card>

        {/* System Health Section */}
        <div className="flex flex-col">
          <SystemHealth />
        </div>
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
                <Link href="/guide/deployment/local" className="inline-block">
                  <Button className="w-full" variant="outline">
                    <BookOpen className="h-4 w-4 mr-2" />
                    View Local Deployment Guide
                  </Button>
                </Link>
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
                <Link href="/guide/deployment/azure" className="inline-block">
                  <Button className="w-full" variant="outline">
                    <BookOpen className="h-4 w-4 mr-2" />
                    View Azure Deployment Guide
                  </Button>
                </Link>
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
