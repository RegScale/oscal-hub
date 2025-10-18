'use client';

import Link from 'next/link';
import { Card, CardHeader, CardTitle, CardDescription } from '@/components/ui/card';
import { FileCheck, ArrowRightLeft, GitMerge, Folders, Clock, BookOpen, ExternalLink, ShieldCheck, Library, BarChart3 } from 'lucide-react';
import { Footer } from '@/components/Footer';
import { Hero } from '@/components/Hero';
import { useAuth } from '@/contexts/AuthContext';

export default function Dashboard() {
  const { isAuthenticated, isLoading } = useAuth();

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
        <div id="main-content">
          <Hero />
        </div>
        <Footer />
      </div>
    );
  }

  // Show dashboard for authenticated users
  return (
    <div className="min-h-screen bg-background">
      <div className="container mx-auto py-12 px-4" id="main-content">

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
                  <ShieldCheck className="h-8 w-8 text-primary" />
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
        <section className="grid grid-cols-1 lg:grid-cols-2 gap-6 mb-16" aria-label="Resources and information">
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
                    href="/guide/rules"
                    className="text-primary hover:underline font-medium inline-flex items-center"
                  >
                    Validation Rules Guide
                    <ShieldCheck className="h-4 w-4 ml-2" />
                  </Link>
                </div>
              </div>
              <p className="text-sm pt-2">
                <span className="text-primary font-medium">Status:</span>{' '}
                <span className="text-green-500">✓</span> Backend connected and ready
              </p>
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

      {/* Footer */}
      <Footer />
    </div>
  );
}
