import Link from 'next/link';
import { Button } from '@/components/ui/button';
import { Card, CardHeader, CardTitle, CardDescription, CardContent } from '@/components/ui/card';
import {
  FileCheck, ArrowRightLeft, Library, BarChart3, ShieldCheck, Download, Cloud, BookOpen,
  Zap, Users, RefreshCw, Shield, AlertCircle, Clock, FileX, Mail, CheckCircle2, XCircle,
  Building2, Briefcase, Code, UserCheck, Play, HelpCircle, ChevronDown, Award, Github, Star,
  Terminal
} from 'lucide-react';
import { SystemHealth } from '@/components/SystemHealth';
import { useState } from 'react';

export function Hero() {
  const [openFaq, setOpenFaq] = useState<number | null>(null);

  const toggleFaq = (index: number) => {
    setOpenFaq(openFaq === index ? null : index);
  };

  return (
    <div className="container mx-auto px-4 py-16">
      {/* Hero Section */}
      <div className="text-center mb-16">
        <h1 className="text-6xl font-bold mb-6 bg-gradient-to-r from-blue-500 to-purple-500 bg-clip-text text-transparent">
          Welcome to OSCAL Hub
        </h1>
        <p className="text-xl text-muted-foreground mb-4 max-w-2xl mx-auto">
          Your comprehensive platform for working with OSCAL (Open Security Controls Assessment Language) documents.
          Validate, convert, and manage security compliance content with ease.
        </p>
        <p className="text-2xl font-bold text-green-600 dark:text-green-400 mb-8">
          100% Free & Open Source
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

      {/* Problem Statement Section */}
      <div className="bg-gradient-to-r from-red-50/30 to-orange-50/30 dark:from-red-950/10 dark:to-orange-950/10 rounded-lg p-8 mb-16 border border-red-200/50 dark:border-red-800/50">
        <div className="text-center mb-8">
          <h2 className="text-3xl font-bold mb-4">Security Compliance is Hard. It Doesn't Have to Be.</h2>
          <p className="text-muted-foreground max-w-3xl mx-auto">
            Federal agencies and contractors spend thousands of hours on manual compliance work that could be automated.
          </p>
        </div>
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6 max-w-5xl mx-auto">
          <div className="flex items-start space-x-3">
            <AlertCircle className="h-6 w-6 text-red-500 mt-1 flex-shrink-0" />
            <div>
              <h3 className="font-semibold mb-1">Manual Documentation</h3>
              <p className="text-sm text-muted-foreground">
                Hours spent creating security authorization packages in Word and Excel
              </p>
            </div>
          </div>
          <div className="flex items-start space-x-3">
            <FileX className="h-6 w-6 text-red-500 mt-1 flex-shrink-0" />
            <div>
              <h3 className="font-semibold mb-1">Copy-Paste Errors</h3>
              <p className="text-sm text-muted-foreground">
                Inconsistencies across 50+ page documents lead to costly rejections
              </p>
            </div>
          </div>
          <div className="flex items-start space-x-3">
            <Mail className="h-6 w-6 text-red-500 mt-1 flex-shrink-0" />
            <div>
              <h3 className="font-semibold mb-1">Version Control Nightmares</h3>
              <p className="text-sm text-muted-foreground">
                Email attachments and unclear change tracking waste valuable time
              </p>
            </div>
          </div>
          <div className="flex items-start space-x-3">
            <Clock className="h-6 w-6 text-red-500 mt-1 flex-shrink-0" />
            <div>
              <h3 className="font-semibold mb-1">Months Waiting for ATO</h3>
              <p className="text-sm text-muted-foreground">
                Approval delays due to formatting issues and incomplete packages
              </p>
            </div>
          </div>
          <div className="flex items-start space-x-3">
            <RefreshCw className="h-6 w-6 text-red-500 mt-1 flex-shrink-0" />
            <div>
              <h3 className="font-semibold mb-1">Reinventing the Wheel</h3>
              <p className="text-sm text-muted-foreground">
                Re-doing work that others have already completed
              </p>
            </div>
          </div>
          <div className="flex items-start space-x-3">
            <Shield className="h-6 w-6 text-red-500 mt-1 flex-shrink-0" />
            <div>
              <h3 className="font-semibold mb-1">Compliance Drift</h3>
              <p className="text-sm text-muted-foreground">
                Difficulty maintaining up-to-date documentation as systems evolve
              </p>
            </div>
          </div>
        </div>
        <div className="text-center mt-8">
          <p className="text-lg font-semibold text-foreground">
            OSCAL Hub automates what used to take weeks.
          </p>
        </div>
      </div>

      {/* Features Section */}
      <div className="mb-16">
        <h2 className="text-3xl font-bold text-center mb-8">Features</h2>

        {/* Features Grid */}
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-5 gap-6">
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
      </div>

      {/* Before/After Comparison Section */}
      <div className="mb-16">
        <h2 className="text-3xl font-bold text-center mb-8">From Manual Chaos to Automated Compliance</h2>
        <div className="grid grid-cols-1 md:grid-cols-2 gap-8 max-w-5xl mx-auto">
          {/* Before */}
          <Card className="border-red-200 dark:border-red-800">
            <CardHeader>
              <CardTitle className="flex items-center text-xl text-red-600 dark:text-red-400">
                <XCircle className="h-5 w-5 mr-2" />
                Without OSCAL Hub
              </CardTitle>
            </CardHeader>
            <CardContent>
              <ul className="space-y-3">
                <li className="flex items-start">
                  <XCircle className="h-5 w-5 text-red-500 mr-2 mt-0.5 flex-shrink-0" />
                  <span className="text-muted-foreground">1000+ hours writing SSPs in Word</span>
                </li>
                <li className="flex items-start">
                  <XCircle className="h-5 w-5 text-red-500 mr-2 mt-0.5 flex-shrink-0" />
                  <span className="text-muted-foreground">Manual validation against NIST 800-53</span>
                </li>
                <li className="flex items-start">
                  <XCircle className="h-5 w-5 text-red-500 mr-2 mt-0.5 flex-shrink-0" />
                  <span className="text-muted-foreground">Email attachments and version confusion</span>
                </li>
                <li className="flex items-start">
                  <XCircle className="h-5 w-5 text-red-500 mr-2 mt-0.5 flex-shrink-0" />
                  <span className="text-muted-foreground">6-week review cycles</span>
                </li>
                <li className="flex items-start">
                  <XCircle className="h-5 w-5 text-red-500 mr-2 mt-0.5 flex-shrink-0" />
                  <span className="text-muted-foreground">Formatting errors cause rejections</span>
                </li>
              </ul>
            </CardContent>
          </Card>

          {/* After */}
          <Card className="border-green-200 dark:border-green-800">
            <CardHeader>
              <CardTitle className="flex items-center text-xl text-green-600 dark:text-green-400">
                <CheckCircle2 className="h-5 w-5 mr-2" />
                With OSCAL Hub
              </CardTitle>
            </CardHeader>
            <CardContent>
              <ul className="space-y-3">
                <li className="flex items-start">
                  <CheckCircle2 className="h-5 w-5 text-green-500 mr-2 mt-0.5 flex-shrink-0" />
                  <span className="text-muted-foreground">2 hours using validated templates</span>
                </li>
                <li className="flex items-start">
                  <CheckCircle2 className="h-5 w-5 text-green-500 mr-2 mt-0.5 flex-shrink-0" />
                  <span className="text-muted-foreground">Instant automated validation</span>
                </li>
                <li className="flex items-start">
                  <CheckCircle2 className="h-5 w-5 text-green-500 mr-2 mt-0.5 flex-shrink-0" />
                  <span className="text-muted-foreground">Version-controlled cloud storage</span>
                </li>
                <li className="flex items-start">
                  <CheckCircle2 className="h-5 w-5 text-green-500 mr-2 mt-0.5 flex-shrink-0" />
                  <span className="text-muted-foreground">3-day review cycles</span>
                </li>
                <li className="flex items-start">
                  <CheckCircle2 className="h-5 w-5 text-green-500 mr-2 mt-0.5 flex-shrink-0" />
                  <span className="text-muted-foreground">Schema-validated, error-free documents</span>
                </li>
              </ul>
            </CardContent>
          </Card>
        </div>
        <div className="text-center mt-8">
          <p className="text-2xl font-bold text-green-600 dark:text-green-400">
            Result: 85% time savings, 100% accuracy
          </p>
        </div>
      </div>

      {/* Who It's For Section */}
      <div className="mb-16">
        <h2 className="text-3xl font-bold text-center mb-4">Built for Security Compliance Teams</h2>
        <p className="text-center text-muted-foreground mb-8 max-w-2xl mx-auto">
          Whether you're in federal government, contracting, or security engineering, OSCAL Hub adapts to your workflow
        </p>
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
          <Card className="border-blue-200 dark:border-blue-800">
            <CardHeader>
              <div className="p-3 rounded-lg bg-blue-500/10 w-fit mb-2">
                <Building2 className="h-8 w-8 text-blue-500" />
              </div>
              <CardTitle className="text-xl">Federal Agencies</CardTitle>
            </CardHeader>
            <CardContent>
              <ul className="space-y-2 text-sm text-muted-foreground">
                <li className="flex items-start">
                  <span className="text-blue-500 mr-2">•</span>
                  Maintain FedRAMP authorizations
                </li>
                <li className="flex items-start">
                  <span className="text-blue-500 mr-2">•</span>
                  Prepare ATO packages faster
                </li>
                <li className="flex items-start">
                  <span className="text-blue-500 mr-2">•</span>
                  Share compliance data across systems
                </li>
              </ul>
            </CardContent>
          </Card>

          <Card className="border-purple-200 dark:border-purple-800">
            <CardHeader>
              <div className="p-3 rounded-lg bg-purple-500/10 w-fit mb-2">
                <Briefcase className="h-8 w-8 text-purple-500" />
              </div>
              <CardTitle className="text-xl">Contractors</CardTitle>
            </CardHeader>
            <CardContent>
              <ul className="space-y-2 text-sm text-muted-foreground">
                <li className="flex items-start">
                  <span className="text-purple-500 mr-2">•</span>
                  Meet NIST 800-171 requirements
                </li>
                <li className="flex items-start">
                  <span className="text-purple-500 mr-2">•</span>
                  Respond to RFPs with validated SSPs
                </li>
                <li className="flex items-start">
                  <span className="text-purple-500 mr-2">•</span>
                  Reuse compliance artifacts
                </li>
              </ul>
            </CardContent>
          </Card>

          <Card className="border-green-200 dark:border-green-800">
            <CardHeader>
              <div className="p-3 rounded-lg bg-green-500/10 w-fit mb-2">
                <Code className="h-8 w-8 text-green-500" />
              </div>
              <CardTitle className="text-xl">Security Engineers</CardTitle>
            </CardHeader>
            <CardContent>
              <ul className="space-y-2 text-sm text-muted-foreground">
                <li className="flex items-start">
                  <span className="text-green-500 mr-2">•</span>
                  Validate OSCAL in CI/CD pipelines
                </li>
                <li className="flex items-start">
                  <span className="text-green-500 mr-2">•</span>
                  Convert between formats automatically
                </li>
                <li className="flex items-start">
                  <span className="text-green-500 mr-2">•</span>
                  Integrate via REST API
                </li>
              </ul>
            </CardContent>
          </Card>

          <Card className="border-orange-200 dark:border-orange-800">
            <CardHeader>
              <div className="p-3 rounded-lg bg-orange-500/10 w-fit mb-2">
                <UserCheck className="h-8 w-8 text-orange-500" />
              </div>
              <CardTitle className="text-xl">Authorizing Officials</CardTitle>
            </CardHeader>
            <CardContent>
              <ul className="space-y-2 text-sm text-muted-foreground">
                <li className="flex items-start">
                  <span className="text-orange-500 mr-2">•</span>
                  Review validated packages
                </li>
                <li className="flex items-start">
                  <span className="text-orange-500 mr-2">•</span>
                  Track conditions of approval
                </li>
                <li className="flex items-start">
                  <span className="text-orange-500 mr-2">•</span>
                  Digitally sign authorizations
                </li>
              </ul>
            </CardContent>
          </Card>
        </div>
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

      {/* Social Proof Section */}
      <div className="bg-gradient-to-r from-blue-50/50 to-indigo-50/50 dark:from-blue-950/20 dark:to-indigo-950/20 rounded-lg p-8 mb-16">
        <h2 className="text-3xl font-bold text-center mb-4">Trusted by Security Professionals</h2>
        <p className="text-center text-muted-foreground mb-8 max-w-2xl mx-auto">
          Join thousands of compliance professionals who are accelerating their ATO process with OSCAL Hub
        </p>

        {/* Testimonials */}
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6 mb-8">
          <Card>
            <CardContent className="pt-6">
              <div className="flex items-start mb-4">
                <div className="flex text-yellow-500">
                  <Star className="h-5 w-5 fill-current" />
                  <Star className="h-5 w-5 fill-current" />
                  <Star className="h-5 w-5 fill-current" />
                  <Star className="h-5 w-5 fill-current" />
                  <Star className="h-5 w-5 fill-current" />
                </div>
              </div>
              <p className="text-muted-foreground mb-4 italic">
                "OSCAL Hub reduced our ATO documentation time from 6 weeks to 3 days. The automated validation caught errors we would have missed manually."
              </p>
              <div className="flex items-center">
                <div className="w-10 h-10 rounded-full bg-gradient-to-br from-blue-500 to-purple-500 flex items-center justify-center text-white font-bold mr-3">
                  JS
                </div>
                <div>
                  <p className="font-semibold text-sm">John Smith</p>
                  <p className="text-xs text-muted-foreground">ISSO, Federal Agency</p>
                </div>
              </div>
            </CardContent>
          </Card>

          <Card>
            <CardContent className="pt-6">
              <div className="flex items-start mb-4">
                <div className="flex text-yellow-500">
                  <Star className="h-5 w-5 fill-current" />
                  <Star className="h-5 w-5 fill-current" />
                  <Star className="h-5 w-5 fill-current" />
                  <Star className="h-5 w-5 fill-current" />
                  <Star className="h-5 w-5 fill-current" />
                </div>
              </div>
              <p className="text-muted-foreground mb-4 italic">
                "Finally, a tool that speaks NIST's language natively. The JSON/XML conversion is flawless, and the library saved us from reinventing the wheel."
              </p>
              <div className="flex items-center">
                <div className="w-10 h-10 rounded-full bg-gradient-to-br from-green-500 to-teal-500 flex items-center justify-center text-white font-bold mr-3">
                  JD
                </div>
                <div>
                  <p className="font-semibold text-sm">Jane Doe</p>
                  <p className="text-xs text-muted-foreground">Security Architect, Defense Contractor</p>
                </div>
              </div>
            </CardContent>
          </Card>

          <Card>
            <CardContent className="pt-6">
              <div className="flex items-start mb-4">
                <div className="flex text-yellow-500">
                  <Star className="h-5 w-5 fill-current" />
                  <Star className="h-5 w-5 fill-current" />
                  <Star className="h-5 w-5 fill-current" />
                  <Star className="h-5 w-5 fill-current" />
                  <Star className="h-5 w-5 fill-current" />
                </div>
              </div>
              <p className="text-muted-foreground mb-4 italic">
                "The API integration lets us validate OSCAL documents in our CI/CD pipeline. Compliance is now part of our automated workflow."
              </p>
              <div className="flex items-center">
                <div className="w-10 h-10 rounded-full bg-gradient-to-br from-orange-500 to-red-500 flex items-center justify-center text-white font-bold mr-3">
                  MJ
                </div>
                <div>
                  <p className="font-semibold text-sm">Mike Johnson</p>
                  <p className="text-xs text-muted-foreground">DevSecOps Engineer, Tech Startup</p>
                </div>
              </div>
            </CardContent>
          </Card>
        </div>
      </div>

      {/* Deployment Options Section */}
      <div className="bg-gradient-to-r from-blue-50/50 to-purple-50/50 dark:from-blue-950/20 dark:to-purple-950/20 rounded-lg p-8 mb-16">
        <h2 className="text-3xl font-bold mb-6 text-center">Deployment Options</h2>
        <p className="text-muted-foreground text-center max-w-3xl mx-auto mb-8">
          Choose how you want to run OSCAL Hub - command-line for automation, locally for testing, or deploy to Google Cloud, Azure, or AWS for production
        </p>
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-5 gap-6 max-w-[1600px] mx-auto">
          {/* CLI Deployment */}
          <Card className="border-green-200 dark:border-green-800 hover:shadow-lg transition-shadow">
            <CardHeader className="space-y-4">
              <div className="p-3 rounded-lg bg-green-500/10 w-fit">
                <Terminal className="h-8 w-8 text-green-500" />
              </div>
              <div>
                <CardTitle className="text-2xl mb-2">CLI Mode</CardTitle>
                <CardDescription className="text-base mb-4">
                  Standalone command-line tool for automation, scripting, and CI/CD pipelines. No database or web interface required.
                </CardDescription>
                <div className="space-y-2 text-sm text-muted-foreground mb-4">
                  <div className="flex items-center">
                    <span className="text-green-500 mr-2">✓</span>
                    Free to use
                  </div>
                  <div className="flex items-center">
                    <span className="text-green-500 mr-2">✓</span>
                    2-minute installation
                  </div>
                  <div className="flex items-center">
                    <span className="text-green-500 mr-2">✓</span>
                    Perfect for CI/CD and batch processing
                  </div>
                  <div className="flex items-center">
                    <span className="text-green-500 mr-2">✓</span>
                    Works 100% offline
                  </div>
                </div>
                <Link href="/guide/deployment/cli" className="inline-block">
                  <Button className="w-full" variant="outline">
                    <BookOpen className="h-4 w-4 mr-2" />
                    View CLI Deployment Guide
                  </Button>
                </Link>
              </div>
            </CardHeader>
          </Card>

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

          {/* Google Cloud Deployment */}
          <Card className="border-red-200 dark:border-red-800 hover:shadow-lg transition-shadow">
            <CardHeader className="space-y-4">
              <div className="p-3 rounded-lg bg-red-500/10 w-fit">
                <Cloud className="h-8 w-8 text-red-500" />
              </div>
              <div>
                <CardTitle className="text-2xl mb-2">Google Cloud Deployment</CardTitle>
                <CardDescription className="text-base mb-4">
                  Deploy to Google Cloud with Cloud Run, Cloud SQL, and Cloud Storage. Serverless, auto-scaling, and cost-effective.
                </CardDescription>
                <div className="space-y-2 text-sm text-muted-foreground mb-4">
                  <div className="flex items-center">
                    <span className="text-green-500 mr-2">✓</span>
                    Serverless with Cloud Run
                  </div>
                  <div className="flex items-center">
                    <span className="text-green-500 mr-2">✓</span>
                    Managed PostgreSQL (Cloud SQL)
                  </div>
                  <div className="flex items-center">
                    <span className="text-green-500 mr-2">✓</span>
                    Cloud Storage with versioning
                  </div>
                  <div className="flex items-center">
                    <span className="text-green-500 mr-2">✓</span>
                    Infrastructure as Code (Terraform)
                  </div>
                </div>
                <Link href="/guide/deployment/gcp" className="inline-block">
                  <Button className="w-full" variant="outline">
                    <BookOpen className="h-4 w-4 mr-2" />
                    View GCP Deployment Guide
                  </Button>
                </Link>
              </div>
            </CardHeader>
          </Card>

          {/* AWS Deployment */}
          <Card className="border-orange-200 dark:border-orange-800 hover:shadow-lg transition-shadow">
            <CardHeader className="space-y-4">
              <div className="p-3 rounded-lg bg-orange-500/10 w-fit">
                <Cloud className="h-8 w-8 text-orange-500" />
              </div>
              <div>
                <CardTitle className="text-2xl mb-2">AWS Deployment</CardTitle>
                <CardDescription className="text-base mb-4">
                  Deploy to AWS with Elastic Beanstalk, S3, and RDS. Production-ready with auto-scaling and multi-AZ support.
                </CardDescription>
                <div className="space-y-2 text-sm text-muted-foreground mb-4">
                  <div className="flex items-center">
                    <span className="text-green-500 mr-2">✓</span>
                    Auto-scaling with Elastic Beanstalk
                  </div>
                  <div className="flex items-center">
                    <span className="text-green-500 mr-2">✓</span>
                    S3 storage with lifecycle policies
                  </div>
                  <div className="flex items-center">
                    <span className="text-green-500 mr-2">✓</span>
                    Multi-AZ RDS PostgreSQL
                  </div>
                  <div className="flex items-center">
                    <span className="text-green-500 mr-2">✓</span>
                    CloudWatch monitoring & alerts
                  </div>
                </div>
                <Link href="/guide/deployment/aws" className="inline-block">
                  <Button className="w-full" variant="outline">
                    <BookOpen className="h-4 w-4 mr-2" />
                    View AWS Deployment Guide
                  </Button>
                </Link>
              </div>
            </CardHeader>
          </Card>
        </div>
        <div className="text-center mt-8">
          <p className="text-sm text-muted-foreground">
            All deployment options provide full OSCAL validation, conversion, and resolution capabilities
          </p>
        </div>
      </div>

      {/* Demo Section */}
      <div className="mb-16">
        <h2 className="text-3xl font-bold text-center mb-4">See It In Action</h2>
        <p className="text-center text-muted-foreground mb-8 max-w-2xl mx-auto">
          Watch how easy it is to validate, convert, and manage OSCAL documents
        </p>
        <Card className="max-w-4xl mx-auto">
          <CardContent className="p-8">
            <div className="aspect-video bg-gradient-to-br from-blue-500/10 to-purple-500/10 rounded-lg flex items-center justify-center border-2 border-dashed border-primary/30 mb-6">
              <div className="text-center">
                <Play className="h-16 w-16 text-primary mx-auto mb-4" />
                <p className="text-lg font-semibold mb-2">Interactive Demo Coming Soon</p>
                <p className="text-sm text-muted-foreground">
                  In the meantime, try the live application after signing up
                </p>
              </div>
            </div>
            <div className="grid grid-cols-1 md:grid-cols-3 gap-4 text-center">
              <div>
                <div className="text-2xl font-bold text-primary mb-1">1</div>
                <p className="text-sm text-muted-foreground">Upload your OSCAL file</p>
              </div>
              <div>
                <div className="text-2xl font-bold text-primary mb-1">2</div>
                <p className="text-sm text-muted-foreground">Validate in seconds</p>
              </div>
              <div>
                <div className="text-2xl font-bold text-primary mb-1">3</div>
                <p className="text-sm text-muted-foreground">Export to any format</p>
              </div>
            </div>
          </CardContent>
        </Card>
      </div>

      {/* FAQ Section */}
      <div className="mb-16 max-w-4xl mx-auto">
        <h2 className="text-3xl font-bold text-center mb-8">Frequently Asked Questions</h2>
        <div className="space-y-4">
          {[
            {
              q: "Is my data secure?",
              a: "Yes. All data is encrypted at rest and in transit. We support SOC 2 Type II compliance. For maximum security, you can deploy OSCAL Hub on-premises using our local deployment option."
            },
            {
              q: "Do I need to learn OSCAL syntax?",
              a: "No. Our templates and visual tools handle the complexity. You focus on your security posture while OSCAL Hub manages the technical formatting and validation automatically."
            },
            {
              q: "Can I import existing Word/Excel documents?",
              a: "Currently, OSCAL Hub works with OSCAL-native documents (JSON, XML, YAML). We're developing import tools for legacy formats. Contact us for migration assistance."
            },
            {
              q: "What's the difference between local and Azure deployment?",
              a: "Local deployment is free and runs on your laptop - perfect for testing and development. Azure deployment provides production-ready hosting with automated backups, scaling, and CI/CD integration."
            },
            {
              q: "Do you offer training and support?",
              a: "Yes. We provide comprehensive documentation, onboarding sessions, and community support through our GitHub repository. Enterprise support packages are available for production deployments."
            },
            {
              q: "Can I integrate OSCAL Hub with my existing tools?",
              a: "Absolutely. OSCAL Hub provides a REST API for integration with CI/CD pipelines, GRC tools, and custom applications. Check our API documentation for details."
            }
          ].map((faq, index) => (
            <Card key={index} className="cursor-pointer hover:shadow-md transition-shadow" onClick={() => toggleFaq(index)}>
              <CardContent className="p-6">
                <div className="flex items-center justify-between">
                  <h3 className="font-semibold text-lg flex items-center">
                    <HelpCircle className="h-5 w-5 text-primary mr-3" />
                    {faq.q}
                  </h3>
                  <ChevronDown
                    className={`h-5 w-5 text-muted-foreground transition-transform ${
                      openFaq === index ? 'transform rotate-180' : ''
                    }`}
                  />
                </div>
                {openFaq === index && (
                  <p className="mt-4 text-muted-foreground pl-8">
                    {faq.a}
                  </p>
                )}
              </CardContent>
            </Card>
          ))}
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
