import Link from 'next/link';
import { Button } from '@/components/ui/button';
import { Card, CardHeader, CardTitle, CardDescription } from '@/components/ui/card';
import { FileCheck, ArrowRightLeft, Library, BarChart3, ShieldCheck } from 'lucide-react';

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
