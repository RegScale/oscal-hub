'use client';

import Link from 'next/link';
import { Button } from '@/components/ui/button';
import { Card, CardHeader, CardTitle, CardDescription, CardContent, CardFooter } from '@/components/ui/card';
import {
  FileCheck, ArrowRightLeft, Library, BarChart3, ShieldCheck, BookOpen,
  Zap, Users, RefreshCw, Shield, AlertCircle, Clock, FileX, Mail, CheckCircle2, XCircle,
  Building2, Briefcase, Code, UserCheck, HelpCircle, ChevronLeft, ChevronRight,
  Sparkles, Wand2, FileSearch, KeyRound, Brain, Workflow, Upload, GitBranch, ClipboardCheck, Plug,
  Hammer, FileText, Folders, GitMerge, Share2, Recycle, ArrowRight,
} from 'lucide-react';
import { useRef } from 'react';

const FEATURES = [
  { icon: Library, title: 'Library', description: 'Browse, share, and download example OSCAL documents from the community.' },
  { icon: FileText, title: 'Artifacts', description: 'Markdown templates with variables for compliance docs — generate consistent narratives at scale.' },
  { icon: Hammer, title: 'Build', description: 'Visually create catalogs, profiles, components, SSPs, AP/AR/POA&M without writing OSCAL by hand.' },
  { icon: ShieldCheck, title: 'Authorizations', description: 'Create, track, and manage system authorizations with conditions and expirations.' },
  { icon: BarChart3, title: 'Visualize', description: 'Explore OSCAL documents through interactive control coverage and dependency graphs.' },
  { icon: FileCheck, title: 'Validate', description: 'Check OSCAL against schema, constraints, and your org&rsquo;s custom rules in one click.' },
  { icon: ArrowRightLeft, title: 'Convert', description: 'Round-trip between XML, JSON, and YAML with side-by-side preview.' },
  { icon: FileCheck, title: 'Validation Rules', description: 'Browse the rules checked during validation — and write your own with AI assistance.' },
  { icon: Folders, title: 'Batch', description: 'Process many files at once with live progress tracking and a results summary.' },
  { icon: Clock, title: 'History', description: 'Every operation logged and re-runnable. Audit trail and quick redo in one view.' },
  { icon: GitMerge, title: 'Resolve', description: 'Resolve OSCAL profiles into the underlying baseline catalog with full traceability.' },
];

export function Hero() {
  const featuresRef = useRef<HTMLDivElement | null>(null);

  const scrollFeatures = (direction: 1 | -1) => {
    const el = featuresRef.current;
    if (!el) return;
    // Scroll by roughly one card-width (the cards are 320px wide with 16px gap).
    el.scrollBy({ left: direction * 336, behavior: 'smooth' });
  };

  return (
    <div className="container mx-auto px-4 py-16">
      {/* Hero Section */}
      <div className="relative text-center mb-16">
        {/* Decorative glow — modern gradient mesh behind the headline. Sits
            behind the text via -z-10 and is fully decorative (pointer-events
            disabled, hidden from assistive tech via aria-hidden). */}
        <div aria-hidden="true" className="pointer-events-none absolute inset-x-0 -top-10 -z-10 flex justify-center">
          <div className="h-72 w-[80%] max-w-3xl rounded-full bg-gradient-to-r from-blue-500/30 via-purple-500/30 to-pink-500/20 blur-3xl opacity-70" />
        </div>
        <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-green-500/10 text-green-700 dark:text-green-400 text-xs font-semibold tracking-wide uppercase mb-6">
          <Sparkles className="h-3.5 w-3.5" />
          100% Free &amp; Open Source
        </div>
        <h1 className="text-5xl sm:text-6xl font-bold mb-6 bg-gradient-to-r from-blue-500 to-purple-500 bg-clip-text text-transparent leading-tight">
          OSCAL — Made Easy
        </h1>
        <p className="text-xl text-muted-foreground mb-8 max-w-3xl mx-auto leading-relaxed">
          The fastest way to author, validate, and ship OSCAL — the machine-readable compliance standard behind FedRAMP, NIST 800-53, and CMMC. AI turns the source documents you already have — PDFs, STIGs, CIS benchmarks — into valid OSCAL in minutes, not weeks.
        </p>
        <div className="flex flex-wrap gap-3 justify-center">
          <Link href="/login?mode=signup">
            <Button size="lg" className="text-lg px-8">
              Get Started — Free
            </Button>
          </Link>
          <Link href="/catalog">
            <Button size="lg" variant="outline" className="text-lg px-8">
              <Library className="h-5 w-5 mr-2" />
              Browse the Library
            </Button>
          </Link>
          <a
            href="https://pages.nist.gov/OSCAL/"
            target="_blank"
            rel="noopener noreferrer"
          >
            <Button size="lg" variant="outline" className="text-lg px-8">
              <BookOpen className="h-5 w-5 mr-2" />
              What is OSCAL?
            </Button>
          </a>
        </div>
      </div>

      {/* Problem Statement Section */}
      <div className="mb-16">
        <div className="text-center mb-8">
          <h2 className="text-3xl font-bold mb-4">Security Compliance is Hard. It Doesn&rsquo;t Have to Be.</h2>
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
            OSCAL takes months of manual work to minutes of automation.
          </p>
        </div>
      </div>

      {/* Lifecycle Section — the OSCAL story in four stages */}
      <div className="mb-16">
        <div className="text-center mb-10">
          <h2 className="text-3xl font-bold mb-3">OSCAL for the Full Lifecycle</h2>
          <p className="text-muted-foreground max-w-2xl mx-auto">
            Build, validate, share, reuse — every stage of the compliance authoring loop, in one platform.
          </p>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6 lg:gap-4 relative">
          {[
            {
              n: '01',
              icon: Hammer,
              title: 'Build',
              description: 'Author catalogs, profiles, components, SSPs, AP/AR/POA&M with visual builders — or let AI draft them from PDFs, STIGs, and CIS benchmarks you already have.',
              accent: 'text-blue-500',
              bg: 'bg-blue-500/10',
            },
            {
              n: '02',
              icon: FileCheck,
              title: 'Validate',
              description: 'Check every artifact against schema, NIST constraints, and your org’s custom rules in one click. Catch problems before the assessor does.',
              accent: 'text-purple-500',
              bg: 'bg-purple-500/10',
            },
            {
              n: '03',
              icon: Share2,
              title: 'Share',
              description: 'Publish to your org library, the public community library, or hand assessors a machine-readable OSCAL package — not a 200-page Word doc.',
              accent: 'text-cyan-500',
              bg: 'bg-cyan-500/10',
            },
            {
              n: '04',
              icon: Recycle,
              title: 'Reuse',
              description: 'Fork shared catalogs, profiles, and components into your next system. Inherit what’s done, customize what’s different, never start from scratch.',
              accent: 'text-green-500',
              bg: 'bg-green-500/10',
            },
          ].map(({ n, icon: Icon, title, description, accent, bg }, idx, arr) => (
            <div key={title} className="relative">
              <Card className="h-full hover:shadow-md transition-shadow border-primary/20">
                <CardHeader className="space-y-3">
                  <div className="flex items-center justify-between">
                    <div className={`p-2.5 rounded-lg ${bg}`}>
                      <Icon className={`h-6 w-6 ${accent}`} />
                    </div>
                    <span className="text-3xl font-bold text-muted-foreground/30 tabular-nums">{n}</span>
                  </div>
                  <CardTitle className="text-xl">{title}</CardTitle>
                  <CardDescription className="text-sm leading-relaxed">{description}</CardDescription>
                </CardHeader>
              </Card>
              {/* Connector arrow on lg between cards */}
              {idx < arr.length - 1 && (
                <div aria-hidden="true" className="hidden lg:flex absolute top-1/2 -right-3 -translate-y-1/2 items-center justify-center w-6 h-6 rounded-full bg-background border border-border z-10">
                  <ArrowRight className="h-3.5 w-3.5 text-muted-foreground" />
                </div>
              )}
            </div>
          ))}
        </div>
      </div>

      {/* Features Section */}
      <div className="mb-16">
        <div className="text-center mb-10">
          <h2 className="text-3xl font-bold mb-3">One Tool, the Whole OSCAL Lifecycle</h2>
          <p className="text-muted-foreground max-w-2xl mx-auto">
            From authoring to validation to assessor handoff — every step lives in one place, scripted via REST API or used through a clean UI.
          </p>
        </div>

        <div className="relative">
          {/* Floating prev/next arrows — overlay the carousel rail so the
              centered header above stays balanced. Hidden on small screens
              where touch swipe is the natural affordance. */}
          <button
            type="button"
            onClick={() => scrollFeatures(-1)}
            aria-label="Previous features"
            className="hidden sm:inline-flex absolute -left-3 top-1/2 -translate-y-1/2 z-10 h-10 w-10 items-center justify-center rounded-full border border-border bg-background shadow-md hover:bg-accent hover:text-accent-foreground transition-colors"
          >
            <ChevronLeft className="h-5 w-5" />
          </button>
          <button
            type="button"
            onClick={() => scrollFeatures(1)}
            aria-label="Next features"
            className="hidden sm:inline-flex absolute -right-3 top-1/2 -translate-y-1/2 z-10 h-10 w-10 items-center justify-center rounded-full border border-border bg-background shadow-md hover:bg-accent hover:text-accent-foreground transition-colors"
          >
            <ChevronRight className="h-5 w-5" />
          </button>

          <div
            ref={featuresRef}
            className="-mx-4 px-4 pb-2 flex gap-4 overflow-x-auto snap-x snap-mandatory scroll-smooth [scrollbar-width:none] [-ms-overflow-style:none] [&::-webkit-scrollbar]:hidden"
          >
            {FEATURES.map(({ icon: Icon, title, description }) => (
              <Card
                key={title}
                className="border-primary/20 shrink-0 w-[280px] sm:w-[320px] snap-start hover:shadow-md transition-shadow"
              >
                <CardHeader className="space-y-4">
                  <div className="p-3 rounded-lg bg-primary/10 w-fit">
                    <Icon className="h-8 w-8 text-primary" />
                  </div>
                  <div>
                    <CardTitle className="text-xl mb-2">{title}</CardTitle>
                    <CardDescription>{description}</CardDescription>
                  </div>
                </CardHeader>
              </Card>
            ))}
          </div>
        </div>
      </div>

      {/* AI Section */}
      <div className="rounded-2xl bg-gradient-to-br from-purple-500/10 via-blue-500/10 to-purple-500/10 dark:from-purple-500/15 dark:via-blue-500/15 dark:to-purple-500/15 border border-purple-500/30 p-8 sm:p-10 mb-16">
        <div className="text-center mb-10">
          <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-purple-500/10 text-purple-700 dark:text-purple-300 text-xs font-semibold tracking-wide uppercase mb-4">
            <Sparkles className="h-3.5 w-3.5" />
            New
          </div>
          <h2 className="text-3xl font-bold mb-3">AI Drafts the OSCAL. You Review It.</h2>
          <p className="text-muted-foreground max-w-3xl mx-auto">
            Drop in the source documents you already have — control catalogs as PDFs, DISA STIGs, CIS benchmarks, vendor hardening guides — and AI produces a draft OSCAL artifact you can review, edit, and ship. Hours, not weeks. Schema-validated. Always with a human in the loop.
          </p>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 gap-6 max-w-5xl mx-auto mb-8">
          <Card className="border-purple-200/60 dark:border-purple-800/60 bg-background/60 backdrop-blur">
            <CardHeader className="space-y-3">
              <div className="flex items-center gap-3">
                <div className="p-2.5 rounded-lg bg-purple-500/10">
                  <FileSearch className="h-6 w-6 text-purple-500" />
                </div>
                <CardTitle className="text-lg">Catalog from Source</CardTitle>
              </div>
              <CardDescription className="text-base">
                Drop a control catalog as PDF, Word, HTML, or pasted text. AI extracts the controls, parameters, and groups, and produces a valid OSCAL catalog you can review side-by-side with the source.
              </CardDescription>
            </CardHeader>
          </Card>

          <Card className="border-purple-200/60 dark:border-purple-800/60 bg-background/60 backdrop-blur">
            <CardHeader className="space-y-3">
              <div className="flex items-center gap-3">
                <div className="p-2.5 rounded-lg bg-purple-500/10">
                  <Workflow className="h-6 w-6 text-purple-500" />
                </div>
                <CardTitle className="text-lg">Component-Definition from STIG / CIS</CardTitle>
              </div>
              <CardDescription className="text-base">
                Upload a DISA STIG, CIS Benchmark, or vendor configuration guide. AI maps each recommendation to NIST 800-53 controls and drafts an OSCAL component-definition with implemented-requirements and statements.
              </CardDescription>
            </CardHeader>
          </Card>

          <Card className="border-purple-200/60 dark:border-purple-800/60 bg-background/60 backdrop-blur">
            <CardHeader className="space-y-3">
              <div className="flex items-center gap-3">
                <div className="p-2.5 rounded-lg bg-purple-500/10">
                  <Wand2 className="h-6 w-6 text-purple-500" />
                </div>
                <CardTitle className="text-lg">AI-Generated Validation Rules</CardTitle>
              </div>
              <CardDescription className="text-base">
                Describe the rule you want in plain English (&ldquo;flag any control without a responsible-role&rdquo;) — AI writes the Metaschema constraint, runs it against your sample doc, and shows you the matches before you save it.
              </CardDescription>
            </CardHeader>
          </Card>

          <Card className="border-purple-200/60 dark:border-purple-800/60 bg-background/60 backdrop-blur">
            <CardHeader className="space-y-3">
              <div className="flex items-center gap-3">
                <div className="p-2.5 rounded-lg bg-purple-500/10">
                  <KeyRound className="h-6 w-6 text-purple-500" />
                </div>
                <CardTitle className="text-lg">Bring Your Own Key &middot; Org-Level Control</CardTitle>
              </div>
              <CardDescription className="text-base">
                Configure your own Anthropic API key per organization. Token usage and cost are tracked per wizard run, and an admin analytics dashboard shows spend, model selection, and document throughput.
              </CardDescription>
            </CardHeader>
          </Card>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-3 gap-4 max-w-4xl mx-auto">
          <div className="flex items-start gap-3 p-4 rounded-lg bg-background/40">
            <Brain className="h-5 w-5 text-purple-500 mt-0.5 flex-shrink-0" />
            <div>
              <h3 className="font-semibold text-sm mb-1">Schema-aware</h3>
              <p className="text-xs text-muted-foreground">
                Output is validated against the OSCAL Metaschema before it&rsquo;s shown to you.
              </p>
            </div>
          </div>
          <div className="flex items-start gap-3 p-4 rounded-lg bg-background/40">
            <ClipboardCheck className="h-5 w-5 text-purple-500 mt-0.5 flex-shrink-0" />
            <div>
              <h3 className="font-semibold text-sm mb-1">Human in the loop</h3>
              <p className="text-xs text-muted-foreground">
                AI drafts, you review, edit, and sign off. Nothing ships without your approval.
              </p>
            </div>
          </div>
          <div className="flex items-start gap-3 p-4 rounded-lg bg-background/40">
            <ShieldCheck className="h-5 w-5 text-purple-500 mt-0.5 flex-shrink-0" />
            <div>
              <h3 className="font-semibold text-sm mb-1">Your data stays yours</h3>
              <p className="text-xs text-muted-foreground">
                Calls go directly from your org&rsquo;s key to Anthropic. No third-party retention or training.
              </p>
            </div>
          </div>
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

      {/* Why OSCAL Section */}
      <div className="mb-16">
        <div className="text-center mb-10">
          <h2 className="text-3xl font-bold mb-3 flex items-center justify-center gap-2">
            <Zap className="h-7 w-7 text-primary" />
            Why Use OSCAL?
          </h2>
          <p className="text-muted-foreground max-w-2xl mx-auto">
            OSCAL transforms security compliance from manual documentation into machine-readable automation — faster, more reliable, and reusable across teams.
          </p>
        </div>
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
          <Card className="border-primary/20 hover:shadow-md transition-shadow">
            <CardHeader>
              <div className="p-3 rounded-lg bg-primary/10 w-fit mb-3">
                <Shield className="h-6 w-6 text-primary" />
              </div>
              <CardTitle className="text-lg">Standardized Compliance</CardTitle>
              <CardDescription>
                One consistent format across every framework and control — no more reconciling Word, Excel, and PDF artifacts.
              </CardDescription>
            </CardHeader>
          </Card>
          <Card className="border-primary/20 hover:shadow-md transition-shadow">
            <CardHeader>
              <div className="p-3 rounded-lg bg-primary/10 w-fit mb-3">
                <RefreshCw className="h-6 w-6 text-primary" />
              </div>
              <CardTitle className="text-lg">Automation Ready</CardTitle>
              <CardDescription>
                Machine-readable means CI/CD-validatable. Wire OSCAL into your pipelines for continuous compliance.
              </CardDescription>
            </CardHeader>
          </Card>
          <Card className="border-primary/20 hover:shadow-md transition-shadow">
            <CardHeader>
              <div className="p-3 rounded-lg bg-primary/10 w-fit mb-3">
                <Users className="h-6 w-6 text-primary" />
              </div>
              <CardTitle className="text-lg">Team Collaboration</CardTitle>
              <CardDescription>
                Share and reuse compliance content across teams and organizations without copy-paste drift.
              </CardDescription>
            </CardHeader>
          </Card>
          <Card className="border-primary/20 hover:shadow-md transition-shadow">
            <CardHeader>
              <div className="p-3 rounded-lg bg-primary/10 w-fit mb-3">
                <Zap className="h-6 w-6 text-primary" />
              </div>
              <CardTitle className="text-lg">Faster ATO</CardTitle>
              <CardDescription>
                Cut review cycles from weeks to days with validated, structured packages assessors can actually parse.
              </CardDescription>
            </CardHeader>
          </Card>
        </div>
      </div>

      {/* Use Cases Section */}
      <div className="rounded-2xl bg-gradient-to-br from-cyan-500/5 via-blue-500/5 to-cyan-500/5 dark:from-cyan-500/10 dark:via-blue-500/10 dark:to-cyan-500/10 border border-cyan-500/20 p-8 sm:p-10 mb-16">
        <div className="text-center mb-10">
          <h2 className="text-3xl font-bold mb-3">How Teams Use OSCAL Hub</h2>
          <p className="text-muted-foreground max-w-2xl mx-auto">
            Concrete scenarios from real compliance work — find the one that sounds like your week.
          </p>
        </div>
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          <Card className="hover:shadow-md transition-shadow">
            <CardHeader>
              <div className="flex items-center gap-3 mb-2">
                <div className="p-2 rounded-lg bg-blue-500/10">
                  <Upload className="h-5 w-5 text-blue-500" />
                </div>
                <CardTitle className="text-lg">Onboard a control catalog you can&rsquo;t find in OSCAL</CardTitle>
              </div>
              <CardDescription>
                <span className="font-medium text-foreground">You have</span> a PDF of an internal or customer-specific catalog (FedRAMP overlays, agency-specific tailoring, ISO 27001).
                <span className="block mt-3" />
                <span className="font-medium text-foreground">With OSCAL Hub:</span> drop it into the AI Catalog wizard, review the extracted controls, save it to your library, and reference it from every SSP that depends on it.
              </CardDescription>
            </CardHeader>
          </Card>

          <Card className="hover:shadow-md transition-shadow">
            <CardHeader>
              <div className="flex items-center gap-3 mb-2">
                <div className="p-2 rounded-lg bg-purple-500/10">
                  <Workflow className="h-5 w-5 text-purple-500" />
                </div>
                <CardTitle className="text-lg">Map a STIG to NIST 800-53 controls</CardTitle>
              </div>
              <CardDescription>
                <span className="font-medium text-foreground">You have</span> a DISA STIG or CIS Benchmark you need to wire into a System Security Plan.
                <span className="block mt-3" />
                <span className="font-medium text-foreground">With OSCAL Hub:</span> upload the STIG, the AI Component-Definition wizard maps each setting to a control, and you get an OSCAL component you can drop into multiple SSPs.
              </CardDescription>
            </CardHeader>
          </Card>

          <Card className="hover:shadow-md transition-shadow">
            <CardHeader>
              <div className="flex items-center gap-3 mb-2">
                <div className="p-2 rounded-lg bg-green-500/10">
                  <ShieldCheck className="h-5 w-5 text-green-500" />
                </div>
                <CardTitle className="text-lg">Prepare a FedRAMP / ATO package</CardTitle>
              </div>
              <CardDescription>
                <span className="font-medium text-foreground">You have</span> a system going through authorization and a deadline.
                <span className="block mt-3" />
                <span className="font-medium text-foreground">With OSCAL Hub:</span> assemble the SSP from existing components, run schema and constraint validation in one click, resolve the profile into a baseline catalog, and hand the assessor an OSCAL package — not a 200-page Word doc.
              </CardDescription>
            </CardHeader>
          </Card>

          <Card className="hover:shadow-md transition-shadow">
            <CardHeader>
              <div className="flex items-center gap-3 mb-2">
                <div className="p-2 rounded-lg bg-orange-500/10">
                  <GitBranch className="h-5 w-5 text-orange-500" />
                </div>
                <CardTitle className="text-lg">Validate OSCAL in CI/CD</CardTitle>
              </div>
              <CardDescription>
                <span className="font-medium text-foreground">You have</span> compliance artifacts checked into git and want every change validated automatically.
                <span className="block mt-3" />
                <span className="font-medium text-foreground">With OSCAL Hub:</span> wire the REST API or CLI into your pipeline. Schema, constraint, and custom-rule violations fail the build before they reach an assessor.
              </CardDescription>
            </CardHeader>
          </Card>

          <Card className="hover:shadow-md transition-shadow">
            <CardHeader>
              <div className="flex items-center gap-3 mb-2">
                <div className="p-2 rounded-lg bg-teal-500/10">
                  <ArrowRightLeft className="h-5 w-5 text-teal-500" />
                </div>
                <CardTitle className="text-lg">Convert between XML, JSON, and YAML</CardTitle>
              </div>
              <CardDescription>
                <span className="font-medium text-foreground">You have</span> an SSP in JSON but the assessor wants XML — or you&rsquo;re hand-editing YAML for review.
                <span className="block mt-3" />
                <span className="font-medium text-foreground">With OSCAL Hub:</span> convert in seconds with a side-by-side preview. Round-trip safe, schema-validated.
              </CardDescription>
            </CardHeader>
          </Card>

          <Card className="hover:shadow-md transition-shadow">
            <CardHeader>
              <div className="flex items-center gap-3 mb-2">
                <div className="p-2 rounded-lg bg-pink-500/10">
                  <Plug className="h-5 w-5 text-pink-500" />
                </div>
                <CardTitle className="text-lg">Author your own validation rules</CardTitle>
              </div>
              <CardDescription>
                <span className="font-medium text-foreground">You have</span> internal review checks (&ldquo;every control needs a responsible-role&rdquo;) that aren&rsquo;t in NIST&rsquo;s baseline.
                <span className="block mt-3" />
                <span className="font-medium text-foreground">With OSCAL Hub:</span> describe the rule in plain English, let AI write the Metaschema constraint, run it against your library, and add it to your org&rsquo;s standard validation profile.
              </CardDescription>
            </CardHeader>
          </Card>

          <Card className="hover:shadow-md transition-shadow">
            <CardHeader>
              <div className="flex items-center gap-3 mb-2">
                <div className="p-2 rounded-lg bg-indigo-500/10">
                  <Library className="h-5 w-5 text-indigo-500" />
                </div>
                <CardTitle className="text-lg">Reuse community-shared content</CardTitle>
              </div>
              <CardDescription>
                <span className="font-medium text-foreground">You have</span> a new system to authorize and don&rsquo;t want to write the same baseline content for the tenth time.
                <span className="block mt-3" />
                <span className="font-medium text-foreground">With OSCAL Hub:</span> browse the public library for catalogs, profiles, and components, fork them into your org, customize what&rsquo;s different, and inherit the rest.
              </CardDescription>
            </CardHeader>
          </Card>

          <Card className="hover:shadow-md transition-shadow">
            <CardHeader>
              <div className="flex items-center gap-3 mb-2">
                <div className="p-2 rounded-lg bg-cyan-500/10">
                  <BarChart3 className="h-5 w-5 text-cyan-500" />
                </div>
                <CardTitle className="text-lg">Visualize control coverage and gaps</CardTitle>
              </div>
              <CardDescription>
                <span className="font-medium text-foreground">You have</span> an SSP and an assessor asking &ldquo;which 800-53 controls are actually addressed?&rdquo;
                <span className="block mt-3" />
                <span className="font-medium text-foreground">With OSCAL Hub:</span> open the Visualize view to see implemented vs. inherited vs. gap controls, drill into the implementation statements, and export the picture for the package.
              </CardDescription>
            </CardHeader>
          </Card>

          <Card className="hover:shadow-md transition-shadow">
            <CardHeader>
              <div className="flex items-center gap-3 mb-2">
                <div className="p-2 rounded-lg bg-amber-500/10">
                  <Folders className="h-5 w-5 text-amber-500" />
                </div>
                <CardTitle className="text-lg">Clean up a backlog of OSCAL files</CardTitle>
              </div>
              <CardDescription>
                <span className="font-medium text-foreground">You have</span> a folder full of OSCAL artifacts inherited from a prior team and no idea which ones still validate.
                <span className="block mt-3" />
                <span className="font-medium text-foreground">With OSCAL Hub:</span> drop the whole batch into the Batch view, get a per-file pass/fail summary with actionable error messages, and prioritize what to fix.
              </CardDescription>
            </CardHeader>
          </Card>
        </div>
      </div>

      {/* FAQ Section */}
      <div className="rounded-2xl bg-gradient-to-br from-indigo-500/5 via-purple-500/5 to-indigo-500/5 dark:from-indigo-500/10 dark:via-purple-500/10 dark:to-indigo-500/10 border border-indigo-500/20 p-8 sm:p-10 mb-16">
        <div className="text-center mb-10">
          <h2 className="text-3xl font-bold mb-3">Frequently Asked Questions</h2>
          <p className="text-muted-foreground max-w-2xl mx-auto">
            Quick answers to the things most teams ask before signing up.
          </p>
        </div>
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {[
            {
              q: 'Is my data secure?',
              a: 'Yes. All data is encrypted at rest and in transit. You can also self-host OSCAL Hub on your own infrastructure.',
            },
            {
              q: 'Do I need to learn OSCAL syntax?',
              a: 'No. Templates and visual tools handle the complexity. You focus on your security posture while OSCAL Hub handles formatting and validation automatically.',
            },
            {
              q: 'How do the AI features work?',
              a: 'You bring your own Anthropic API key per organization. The wizards send the source documents you choose to Anthropic, get a draft back, and validate it against the OSCAL Metaschema before showing you the result. Token usage and cost are tracked per run.',
            },
            {
              q: 'Can I import existing Word or Excel documents?',
              a: 'Word and Excel are not native OSCAL formats — but the AI Catalog wizard accepts PDF, Word, HTML, and pasted text and extracts a draft OSCAL catalog you can review and save.',
            },
            {
              q: 'Can I integrate OSCAL Hub with my existing tools?',
              a: 'Yes. OSCAL Hub exposes a REST API and a CLI for CI/CD pipelines, GRC tools, and custom integrations. Validation, conversion, profile resolution, and batch processing are all scriptable.',
            },
            {
              q: 'Do you offer training and support?',
              a: 'Comprehensive documentation and an in-app user guide are included. Community support runs through our GitHub repository. Reach out for enterprise onboarding and support packages.',
            },
          ].map((faq, index) => (
            <Card key={index} className="hover:shadow-md transition-shadow">
              <CardHeader>
                <CardTitle className="flex items-start gap-3 text-lg">
                  <HelpCircle className="h-5 w-5 text-primary mt-0.5 flex-shrink-0" />
                  <span>{faq.q}</span>
                </CardTitle>
                <CardDescription className="pt-2">{faq.a}</CardDescription>
              </CardHeader>
            </Card>
          ))}
        </div>
      </div>

      {/* CTA Section */}
      <div className="rounded-2xl bg-gradient-to-br from-blue-500/10 via-purple-500/10 to-blue-500/10 dark:from-blue-500/15 dark:via-purple-500/15 dark:to-blue-500/15 border border-primary/20 p-10 sm:p-14 text-center">
        <h2 className="text-3xl sm:text-4xl font-bold mb-4">
          Cut OSCAL authoring time from <span className="text-primary">weeks to hours</span>.
        </h2>
        <p className="text-lg text-muted-foreground mb-8 max-w-2xl mx-auto">
          Free, open source, and built for the way real compliance teams work. Create an account in 60 seconds — no credit card, no demo gate.
        </p>
        <div className="flex flex-wrap gap-3 justify-center">
          <Link href="/login?mode=signup">
            <Button size="lg" className="text-lg px-10">
              Get Started — Free
            </Button>
          </Link>
          <Link href="/catalog">
            <Button size="lg" variant="outline" className="text-lg px-8">
              <Library className="h-5 w-5 mr-2" />
              Browse the Library First
            </Button>
          </Link>
        </div>
        <div className="mt-6 flex flex-wrap items-center justify-center gap-6 text-xs text-muted-foreground">
          <span className="inline-flex items-center gap-1.5">
            <CheckCircle2 className="h-4 w-4 text-green-500" /> 100% Free &amp; open source
          </span>
          <span className="inline-flex items-center gap-1.5">
            <CheckCircle2 className="h-4 w-4 text-green-500" /> Self-host or hosted
          </span>
          <span className="inline-flex items-center gap-1.5">
            <CheckCircle2 className="h-4 w-4 text-green-500" /> REST API + CLI
          </span>
        </div>
      </div>
    </div>
  );
}
