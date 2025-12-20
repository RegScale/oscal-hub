'use client';

import Link from 'next/link';
import { Card, CardHeader, CardTitle, CardDescription } from '@/components/ui/card';
import { FileCheck, ArrowRightLeft, GitMerge, Folders, Clock, BookOpen, ExternalLink, ShieldCheck, Library, BarChart3, Terminal, Hammer, Zap, Users, RefreshCw, Shield } from 'lucide-react';
import { Hero } from '@/components/Hero';
import { useAuth } from '@/contexts/AuthContext';

export default function Dashboard() {
  const { isAuthenticated, isLoading, user } = useAuth();

  // Show loading state
  if (isLoading) {
    return (
      <div className="min-h-screen bg-background flex items-center justify-center">
        <div className="text-center">
          <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-primary mx-auto mb-4"></div>
          <p className="text-muted-foreground">Loading...</p>
        </div>
      </div>
    );
  }

  // Show hero for unauthenticated users
  if (!isAuthenticated) {
    return (
      <div className="min-h-screen bg-background">
        <div>
          <Hero />
        </div>
      </div>
    );
  }

  // Check if user has organization access
  const hasOrganizationAccess = user?.organizationId != null;

  // Show pending message for authenticated users without organization access
  if (!hasOrganizationAccess) {
    return (
      <div className="min-h-screen bg-background">
        <div className="container mx-auto py-12 px-4">
          <div className="max-w-2xl mx-auto">
            <Card className="text-center p-8">
              <div className="mb-6">
                <svg
                  className="mx-auto h-16 w-16 text-yellow-500"
                  fill="none"
                  stroke="currentColor"
                  viewBox="0 0 24 24"
                >
                  <path
                    strokeLinecap="round"
                    strokeLinejoin="round"
                    strokeWidth={2}
                    d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z"
                  />
                </svg>
              </div>
              <CardTitle className="text-2xl mb-4">Access Request Pending</CardTitle>
              <CardDescription className="text-base mb-6">
                Your access request is pending approval from the organization administrator.
                You will be notified via email once your request has been reviewed.
              </CardDescription>
              <div className="text-sm text-muted-foreground">
                Please check back later or contact your organization administrator for more information.
              </div>
            </Card>
          </div>
        </div>
      </div>
    );
  }

  // Show dashboard for authenticated users
  return (
    <div className="min-h-screen bg-background">
      <div className="container mx-auto py-12 px-4">

        {/* Quick Actions Grid */}
        <nav aria-label="Main operations">
          <h2 className="sr-only">Available Operations</h2>
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6 mb-16">
            <Link
              href="/library"
              className="block group focus:outline-none focus:ring-2 focus:ring-ring focus:ring-offset-2 rounded-lg"
              aria-label="Navigate to Library page to browse and share OSCAL files"
            >
            <Card className="h-full transition-all duration-200 hover:shadow-lg hover:shadow-primary/20 hover:border-primary/50 cursor-pointer">
              <CardHeader className="space-y-4">
                <div className="p-3 rounded-lg bg-primary/10 w-fit group-hover:bg-primary/20 transition-colors">
                  <Library className="h-8 w-8 text-primary" />
                </div>
                <div>
                  <CardTitle className="text-2xl mb-2">Library</CardTitle>
                  <CardDescription className="text-base">
                    Browse, share, and download example OSCAL documents from the community
                  </CardDescription>
                </div>
              </CardHeader>
            </Card>
          </Link>

            <Link
              href="/build"
              className="block group focus:outline-none focus:ring-2 focus:ring-ring focus:ring-offset-2 rounded-lg"
              aria-label="Navigate to Build page to visually create OSCAL component definitions"
            >
            <Card className="h-full transition-all duration-200 hover:shadow-lg hover:shadow-primary/20 hover:border-primary/50 cursor-pointer">
              <CardHeader className="space-y-4">
                <div className="p-3 rounded-lg bg-primary/10 w-fit group-hover:bg-primary/20 transition-colors">
                  <Hammer className="h-8 w-8 text-primary" />
                </div>
                <div>
                  <CardTitle className="text-2xl mb-2">Build</CardTitle>
                  <CardDescription className="text-base">
                    Visually create and manage OSCAL component definitions with reusable elements
                  </CardDescription>
                </div>
              </CardHeader>
            </Card>
          </Link>

            <Link
              href="/authorizations"
              className="block group focus:outline-none focus:ring-2 focus:ring-ring focus:ring-offset-2 rounded-lg"
              aria-label="Navigate to Authorizations page to create and manage system authorizations"
            >
            <Card className="h-full transition-all duration-200 hover:shadow-lg hover:shadow-primary/20 hover:border-primary/50 cursor-pointer">
              <CardHeader className="space-y-4">
                <div className="p-3 rounded-lg bg-primary/10 w-fit group-hover:bg-primary/20 transition-colors">
                  <ShieldCheck className="h-8 w-8 text-primary" />
                </div>
                <div>
                  <CardTitle className="text-2xl mb-2">Authorizations</CardTitle>
                  <CardDescription className="text-base">
                    Create and manage system authorization documents with customizable templates
                  </CardDescription>
                </div>
              </CardHeader>
            </Card>
          </Link>

            <Link
              href="/visualize"
              className="block group focus:outline-none focus:ring-2 focus:ring-ring focus:ring-offset-2 rounded-lg"
              aria-label="Navigate to Visualize page to explore OSCAL documents with data visualizations"
            >
            <Card className="h-full transition-all duration-200 hover:shadow-lg hover:shadow-primary/20 hover:border-primary/50 cursor-pointer">
              <CardHeader className="space-y-4">
                <div className="p-3 rounded-lg bg-primary/10 w-fit group-hover:bg-primary/20 transition-colors">
                  <BarChart3 className="h-8 w-8 text-primary" />
                </div>
                <div>
                  <CardTitle className="text-2xl mb-2">Visualize</CardTitle>
                  <CardDescription className="text-base">
                    Explore and understand OSCAL documents through interactive visualizations
                  </CardDescription>
                </div>
              </CardHeader>
            </Card>
          </Link>

            <Link
              href="/validate"
              className="block group focus:outline-none focus:ring-2 focus:ring-ring focus:ring-offset-2 rounded-lg"
              aria-label="Navigate to Validate page to check if your OSCAL document is valid"
            >
              <Card className="h-full transition-all duration-200 hover:shadow-lg hover:shadow-primary/20 hover:border-primary/50 cursor-pointer">
                <CardHeader className="space-y-4">
                  <div className="p-3 rounded-lg bg-primary/10 w-fit group-hover:bg-primary/20 transition-colors">
                    <FileCheck className="h-8 w-8 text-primary" aria-hidden="true" />
                  </div>
                  <div>
                    <CardTitle className="text-2xl mb-2">Validate</CardTitle>
                    <CardDescription className="text-base">
                      Check if your OSCAL document is valid and complies with schema constraints
                    </CardDescription>
                  </div>
                </CardHeader>
              </Card>
            </Link>

            <Link
              href="/convert"
              className="block group focus:outline-none focus:ring-2 focus:ring-ring focus:ring-offset-2 rounded-lg"
              aria-label="Navigate to Convert page to change format between XML, JSON, and YAML"
            >
              <Card className="h-full transition-all duration-200 hover:shadow-lg hover:shadow-primary/20 hover:border-primary/50 cursor-pointer">
                <CardHeader className="space-y-4">
                  <div className="p-3 rounded-lg bg-primary/10 w-fit group-hover:bg-primary/20 transition-colors">
                    <ArrowRightLeft className="h-8 w-8 text-primary" aria-hidden="true" />
                  </div>
                <div>
                  <CardTitle className="text-2xl mb-2">Convert</CardTitle>
                  <CardDescription className="text-base">
                    Change format between XML, JSON, and YAML with side-by-side preview
                  </CardDescription>
                </div>
              </CardHeader>
            </Card>
          </Link>

            <Link
              href="/rules"
              className="block group focus:outline-none focus:ring-2 focus:ring-ring focus:ring-offset-2 rounded-lg"
              aria-label="Navigate to Validation Rules page to view and understand validation rules"
            >
            <Card className="h-full transition-all duration-200 hover:shadow-lg hover:shadow-primary/20 hover:border-primary/50 cursor-pointer">
              <CardHeader className="space-y-4">
                <div className="p-3 rounded-lg bg-primary/10 w-fit group-hover:bg-primary/20 transition-colors">
                  <FileCheck className="h-8 w-8 text-primary" />
                </div>
                <div>
                  <CardTitle className="text-2xl mb-2">Validation Rules</CardTitle>
                  <CardDescription className="text-base">
                    View and understand the validation rules checked for OSCAL documents
                  </CardDescription>
                </div>
              </CardHeader>
            </Card>
          </Link>

            <Link
              href="/batch"
              className="block group focus:outline-none focus:ring-2 focus:ring-ring focus:ring-offset-2 rounded-lg"
              aria-label="Navigate to Batch page to process multiple files simultaneously"
            >
            <Card className="h-full transition-all duration-200 hover:shadow-lg hover:shadow-primary/20 hover:border-primary/50 cursor-pointer">
              <CardHeader className="space-y-4">
                <div className="p-3 rounded-lg bg-primary/10 w-fit group-hover:bg-primary/20 transition-colors">
                  <Folders className="h-8 w-8 text-primary" />
                </div>
                <div>
                  <CardTitle className="text-2xl mb-2">Batch</CardTitle>
                  <CardDescription className="text-base">
                    Process multiple files simultaneously with progress tracking
                  </CardDescription>
                </div>
              </CardHeader>
            </Card>
          </Link>

            <Link
              href="/history"
              className="block group focus:outline-none focus:ring-2 focus:ring-ring focus:ring-offset-2 rounded-lg"
              aria-label="Navigate to History page to view past operations and results"
            >
            <Card className="h-full transition-all duration-200 hover:shadow-lg hover:shadow-primary/20 hover:border-primary/50 cursor-pointer">
              <CardHeader className="space-y-4">
                <div className="p-3 rounded-lg bg-primary/10 w-fit group-hover:bg-primary/20 transition-colors">
                  <Clock className="h-8 w-8 text-primary" />
                </div>
                <div>
                  <CardTitle className="text-2xl mb-2">History</CardTitle>
                  <CardDescription className="text-base">
                    View past operations, results, and re-run previous tasks
                  </CardDescription>
                </div>
              </CardHeader>
            </Card>
          </Link>

            <Link
              href="/resolve"
              className="block group focus:outline-none focus:ring-2 focus:ring-ring focus:ring-offset-2 rounded-lg"
              aria-label="Navigate to Resolve page to resolve OSCAL profiles into catalogs"
            >
            <Card className="h-full transition-all duration-200 hover:shadow-lg hover:shadow-primary/20 hover:border-primary/50 cursor-pointer">
              <CardHeader className="space-y-4">
                <div className="p-3 rounded-lg bg-primary/10 w-fit group-hover:bg-primary/20 transition-colors">
                  <GitMerge className="h-8 w-8 text-primary" />
                </div>
                <div>
                  <CardTitle className="text-2xl mb-2">Resolve</CardTitle>
                  <CardDescription className="text-base">
                    Resolve OSCAL profiles into catalogs with control selection
                  </CardDescription>
                </div>
              </CardHeader>
            </Card>
          </Link>
          </div>
        </nav>

        {/* Resources and Info Grid */}
        <section className="grid grid-cols-1 lg:grid-cols-3 gap-6 mb-16" aria-label="Resources and information">
          {/* Getting Started */}
          <Card>
            <CardHeader>
              <CardTitle className="flex items-center">
                <BookOpen className="h-5 w-5 mr-2 text-primary" />
                Getting Started
              </CardTitle>
            </CardHeader>
            <div className="px-6 pb-6 space-y-3 text-muted-foreground">
              <p>
                Welcome to OSCAL Hub! This tool provides a modern, visual interface for working with OSCAL documents.
              </p>
              <div className="pt-2 space-y-2">
                <div>
                  <Link
                    href="/guide"
                    className="text-primary hover:underline font-medium inline-flex items-center"
                  >
                    View User Guide
                    <ArrowRightLeft className="h-4 w-4 ml-2 rotate-45" />
                  </Link>
                </div>
                <div>
                  <Link
                    href="/guide/automation"
                    className="text-primary hover:underline font-medium inline-flex items-center"
                  >
                    API Automation Guide
                    <Terminal className="h-4 w-4 ml-2" />
                  </Link>
                </div>
                <div>
                  <Link
                    href="/guide/rules"
                    className="text-primary hover:underline font-medium inline-flex items-center"
                  >
                    Validation Rules Guide
                    <ShieldCheck className="h-4 w-4 ml-2" />
                  </Link>
                </div>
              </div>
              <p className="text-sm pt-2">
                <span className="text-primary font-medium">System Health:</span>{' '}
                <span className="text-green-500">✓</span> All systems operational
              </p>
            </div>
          </Card>

          {/* Benefits Section */}
          <Card>
            <CardHeader>
              <CardTitle className="flex items-center">
                <Zap className="h-5 w-5 mr-2 text-primary" />
                Why Use OSCAL?
              </CardTitle>
            </CardHeader>
            <div className="px-6 pb-6 space-y-3">
              <p className="text-muted-foreground text-sm mb-3">
                OSCAL transforms security compliance from manual documentation to machine-readable automation
              </p>
              <ul className="space-y-3 text-sm">
                <li className="flex items-start">
                  <Shield className="h-4 w-4 mr-2 text-primary mt-0.5 flex-shrink-0" />
                  <div>
                    <span className="font-medium text-foreground">Standardized Compliance</span>
                    <p className="text-xs text-muted-foreground">Consistent format across all security frameworks and controls</p>
                  </div>
                </li>
                <li className="flex items-start">
                  <RefreshCw className="h-4 w-4 mr-2 text-primary mt-0.5 flex-shrink-0" />
                  <div>
                    <span className="font-medium text-foreground">Automation Ready</span>
                    <p className="text-xs text-muted-foreground">Machine-readable format enables automated validation and reporting</p>
                  </div>
                </li>
                <li className="flex items-start">
                  <Users className="h-4 w-4 mr-2 text-primary mt-0.5 flex-shrink-0" />
                  <div>
                    <span className="font-medium text-foreground">Collaboration</span>
                    <p className="text-xs text-muted-foreground">Share and reuse compliance data across teams and organizations</p>
                  </div>
                </li>
                <li className="flex items-start">
                  <Zap className="h-4 w-4 mr-2 text-primary mt-0.5 flex-shrink-0" />
                  <div>
                    <span className="font-medium text-foreground">Faster ATO Process</span>
                    <p className="text-xs text-muted-foreground">Reduce time to Authority to Operate with streamlined documentation</p>
                  </div>
                </li>
              </ul>
            </div>
          </Card>

          {/* OSCAL Resources */}
          <Card>
            <CardHeader>
              <CardTitle className="flex items-center">
                <ExternalLink className="h-5 w-5 mr-2 text-primary" />
                OSCAL Resources
              </CardTitle>
            </CardHeader>
            <div className="px-6 pb-6 space-y-3">
              <p className="text-muted-foreground mb-4">
                Learn more about OSCAL and access official documentation
              </p>
              <ul className="space-y-2 text-sm">
                <li>
                  <a
                    href="http://localhost:8080/swagger-ui/index.html"
                    target="_blank"
                    rel="noopener noreferrer"
                    className="text-primary hover:underline inline-flex items-center"
                  >
                    API Documentation
                    <ExternalLink className="h-3 w-3 ml-2" />
                  </a>
                  <p className="text-xs text-muted-foreground ml-5">Interactive API documentation and testing interface</p>
                </li>
                <li>
                  <a
                    href="https://pages.nist.gov/OSCAL/"
                    target="_blank"
                    rel="noopener noreferrer"
                    className="text-primary hover:underline inline-flex items-center"
                  >
                    NIST OSCAL Website
                    <ExternalLink className="h-3 w-3 ml-2" />
                  </a>
                  <p className="text-xs text-muted-foreground ml-5">Official OSCAL documentation and specifications</p>
                </li>
                <li>
                  <a
                    href="https://oscalfoundation.org/"
                    target="_blank"
                    rel="noopener noreferrer"
                    className="text-primary hover:underline inline-flex items-center"
                  >
                    OSCAL Foundation
                    <ExternalLink className="h-3 w-3 ml-2" />
                  </a>
                  <p className="text-xs text-muted-foreground ml-5">Community resources and ecosystem</p>
                </li>
                <li>
                  <a
                    href="https://github.com/usnistgov/OSCAL"
                    target="_blank"
                    rel="noopener noreferrer"
                    className="text-primary hover:underline inline-flex items-center"
                  >
                    OSCAL on GitHub
                    <ExternalLink className="h-3 w-3 ml-2" />
                  </a>
                  <p className="text-xs text-muted-foreground ml-5">Source code, schemas, and sample content</p>
                </li>
              </ul>
            </div>
          </Card>
        </section>
      </div>
    </div>
  );
}
