'use client';

import { useEffect, useRef, useState } from 'react';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { FileCheck, ArrowRightLeft, GitMerge, Folders, Clock, BookOpen, ExternalLink, ShieldCheck, Library, BarChart3, Terminal, Hammer, Zap, Users, RefreshCw, Shield, FileText, Building2, Search } from 'lucide-react';
import { Hero } from '@/components/Hero';
import { EmptyState } from '@/components/empty-state';
import { useAuth } from '@/contexts/AuthContext';
import { apiClient } from '@/lib/api-client';

// ---------------------------------------------------------------------------
// CreateOrgModal — small inline modal for self-serve org creation
// ---------------------------------------------------------------------------
function CreateOrgModal({ onClose, onSuccess }: { onClose: () => void; onSuccess: () => void }) {
  const [name, setName] = useState('');
  const [nameError, setNameError] = useState('');
  const [submitting, setSubmitting] = useState(false);

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (!name.trim()) { setNameError('Organization name is required.'); return; }
    setNameError('');
    setSubmitting(true);
    try {
      await apiClient.createMyOrganization({ name: name.trim() });
      onSuccess();
    } catch (err: any) {
      if (err?.field === 'name' || err?.code === 'ORGANIZATION_NAME_IN_USE') {
        setNameError(err.message || 'That organization name is already taken.');
      } else {
        setNameError('Something went wrong. Please try again.');
      }
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-black/50"
      role="dialog"
      aria-modal="true"
      aria-labelledby="create-org-modal-title"
    >
      <div className="bg-background rounded-xl shadow-xl p-8 w-full max-w-md mx-4">
        <h2 id="create-org-modal-title" className="text-xl font-semibold mb-4">Create your organization</h2>
        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label htmlFor="org-name" className="block text-sm font-medium mb-1">
              Organization name
            </label>
            <Input
              id="org-name"
              value={name}
              onChange={(e) => setName(e.target.value)}
              placeholder="e.g. Acme Corp"
              disabled={submitting}
              aria-describedby={nameError ? 'org-name-error' : undefined}
            />
            {nameError && (
              <p id="org-name-error" className="text-sm text-destructive mt-1">{nameError}</p>
            )}
          </div>
          <div className="flex gap-3 justify-end pt-2">
            <Button type="button" variant="outline" onClick={onClose} disabled={submitting}>
              Cancel
            </Button>
            <Button type="submit" disabled={submitting}>
              {submitting ? 'Creating…' : 'Create organization'}
            </Button>
          </div>
        </form>
      </div>
    </div>
  );
}

export default function Dashboard() {
  const router = useRouter();
  const { isAuthenticated, isLoading, user } = useAuth();
  const [checkingAuth, setCheckingAuth] = useState(true);
  const [isSuperAdminUser, setIsSuperAdminUser] = useState(false);
  const [hasOrgAccess, setHasOrgAccess] = useState(false);
  // Onboarding state
  const [pendingRequests, setPendingRequests] = useState<Array<{ organizationName: string }>>([]);
  const [pendingLoaded, setPendingLoaded] = useState(false);
  const [showCreateModal, setShowCreateModal] = useState(false);

  // Check auth status from localStorage on mount and navigation
  useEffect(() => {
    if (typeof window === 'undefined') return;

    const storedUser = localStorage.getItem('user');
    if (storedUser) {
      try {
        const userData = JSON.parse(storedUser);

        // Check super admin status
        if (userData.globalRole === 'SUPER_ADMIN') {
          setIsSuperAdminUser(true);
          router.push('/admin');
          return;
        }

        // Check organization access
        setHasOrgAccess(userData.organizationId != null);
      } catch (e) {
        setHasOrgAccess(false);
      }
    }
    setCheckingAuth(false);
  }, [router]);

  // Also update when user from context changes
  useEffect(() => {
    if (user) {
      setIsSuperAdminUser(user.globalRole === 'SUPER_ADMIN');
      setHasOrgAccess(user.organizationId != null);
    }
  }, [user]);

  // Load pending requests once we know the user has no org access
  useEffect(() => {
    if (!isAuthenticated || hasOrgAccess || checkingAuth || pendingLoaded) return;
    let cancelled = false;
    apiClient.getMyPendingRequests().then((reqs) => {
      if (!cancelled) {
        setPendingRequests(reqs);
        setPendingLoaded(true);
      }
    }).catch(() => {
      if (!cancelled) setPendingLoaded(true);
    });
    return () => { cancelled = true; };
  }, [isAuthenticated, hasOrgAccess, checkingAuth, pendingLoaded]);

  // Show loading while checking auth or AuthContext is loading
  if (isLoading || checkingAuth) {
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

  // Super admin redirect is handled by useEffect above
  // Show loading while redirect is happening
  if (isSuperAdminUser) {
    return (
      <div className="min-h-screen bg-background flex items-center justify-center">
        <div className="text-center">
          <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-primary mx-auto mb-4"></div>
          <p className="text-muted-foreground">Redirecting to admin dashboard...</p>
        </div>
      </div>
    );
  }

  // Three-branch empty state for authenticated users without organization access
  if (!hasOrgAccess) {
    // While loading pending requests, show spinner
    if (!pendingLoaded) {
      return (
        <div className="min-h-screen bg-background flex items-center justify-center">
          <div className="text-center">
            <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-primary mx-auto mb-4"></div>
            <p className="text-muted-foreground">Loading…</p>
          </div>
        </div>
      );
    }

    const hasPending = pendingRequests.length > 0;
    const firstPendingOrg = hasPending ? pendingRequests[0].organizationName : null;

    function handleOrgCreated() {
      // Refresh the page so the new org context is picked up
      window.location.reload();
    }

    // Branch A: Has pending request → show status + CTA to create own org
    if (hasPending) {
      return (
        <div className="min-h-screen bg-background">
          <div className="container mx-auto py-12 px-4">
            <EmptyState
              title="Access request pending"
              description={`Your request to join ${firstPendingOrg} is awaiting admin review. You'll be notified by email when it's approved.`}
              primary={{
                label: 'Create your own organization',
                onClick: () => setShowCreateModal(true),
              }}
              secondary={{
                label: 'Request access to another org',
                onClick: () => router.push('/request-access'),
              }}
            />
            {showCreateModal && (
              <CreateOrgModal
                onClose={() => setShowCreateModal(false)}
                onSuccess={handleOrgCreated}
              />
            )}
          </div>
        </div>
      );
    }

    // Branch B: No memberships, no pending requests → "Get started" with two cards
    return (
      <div className="min-h-screen bg-background">
        <div className="container mx-auto py-12 px-4">
          <div className="max-w-3xl mx-auto">
            <div className="text-center mb-10">
              <h1 className="text-3xl font-bold mb-3">Welcome to OSCAL Hub</h1>
              <p className="text-muted-foreground text-lg">
                Get started by creating your own organization or requesting access to an existing one.
              </p>
            </div>
            <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
              {/* Create org card */}
              <button
                className="text-left block group focus:outline-none focus:ring-2 focus:ring-ring focus:ring-offset-2 rounded-xl"
                onClick={() => setShowCreateModal(true)}
                data-testid="create-org-card"
              >
                <Card className="h-full transition-all duration-200 hover:shadow-lg hover:shadow-primary/20 hover:border-primary/50 cursor-pointer">
                  <CardHeader className="space-y-4">
                    <div className="p-3 rounded-lg bg-primary/10 w-fit group-hover:bg-primary/20 transition-colors">
                      <Building2 className="h-8 w-8 text-primary" aria-hidden="true" />
                    </div>
                    <div>
                      <CardTitle className="text-xl mb-2">Create an organization</CardTitle>
                      <CardDescription className="text-base">
                        Start fresh. Create a new organization and become its administrator — invite your team later.
                      </CardDescription>
                    </div>
                  </CardHeader>
                </Card>
              </button>

              {/* Request access card */}
              <Link
                href="/request-access"
                className="block group focus:outline-none focus:ring-2 focus:ring-ring focus:ring-offset-2 rounded-xl"
                data-testid="request-access-card"
              >
                <Card className="h-full transition-all duration-200 hover:shadow-lg hover:shadow-primary/20 hover:border-primary/50 cursor-pointer">
                  <CardHeader className="space-y-4">
                    <div className="p-3 rounded-lg bg-primary/10 w-fit group-hover:bg-primary/20 transition-colors">
                      <Search className="h-8 w-8 text-primary" aria-hidden="true" />
                    </div>
                    <div>
                      <CardTitle className="text-xl mb-2">Request access</CardTitle>
                      <CardDescription className="text-base">
                        Find an existing organization and submit an access request for an administrator to approve.
                      </CardDescription>
                    </div>
                  </CardHeader>
                </Card>
              </Link>
            </div>
          </div>
          {showCreateModal && (
            <CreateOrgModal
              onClose={() => setShowCreateModal(false)}
              onSuccess={handleOrgCreated}
            />
          )}
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
                  <Library className="h-8 w-8 text-primary" aria-hidden="true" />
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
              href="/artifacts"
              className="block group focus:outline-none focus:ring-2 focus:ring-ring focus:ring-offset-2 rounded-lg"
              aria-label="Navigate to Artifacts page to create and share Markdown templates"
            >
            <Card className="h-full transition-all duration-200 hover:shadow-lg hover:shadow-primary/20 hover:border-primary/50 cursor-pointer">
              <CardHeader className="space-y-4">
                <div className="p-3 rounded-lg bg-primary/10 w-fit group-hover:bg-primary/20 transition-colors">
                  <FileText className="h-8 w-8 text-primary" aria-hidden="true" />
                </div>
                <div>
                  <CardTitle className="text-2xl mb-2">Artifacts</CardTitle>
                  <CardDescription className="text-base">
                    Create and share Markdown templates with variables for documentation and compliance artifacts
                  </CardDescription>
                </div>
              </CardHeader>
            </Card>
          </Link>

            <Link
              href="/build"
              className="block group focus:outline-none focus:ring-2 focus:ring-ring focus:ring-offset-2 rounded-lg"
              aria-label="Navigate to Build page to visually create OSCAL catalogs, profiles, and component definitions"
            >
            <Card className="h-full transition-all duration-200 hover:shadow-lg hover:shadow-primary/20 hover:border-primary/50 cursor-pointer">
              <CardHeader className="space-y-4">
                <div className="p-3 rounded-lg bg-primary/10 w-fit group-hover:bg-primary/20 transition-colors">
                  <Hammer className="h-8 w-8 text-primary" aria-hidden="true" />
                </div>
                <div>
                  <CardTitle className="text-2xl mb-2">Build</CardTitle>
                  <CardDescription className="text-base">
                    Visually create and manage OSCAL catalogs, profiles, and component definitions
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
                  <ShieldCheck className="h-8 w-8 text-primary" aria-hidden="true" />
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
                  <BarChart3 className="h-8 w-8 text-primary" aria-hidden="true" />
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
                  <FileCheck className="h-8 w-8 text-primary" aria-hidden="true" />
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
                  <Folders className="h-8 w-8 text-primary" aria-hidden="true" />
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
                  <Clock className="h-8 w-8 text-primary" aria-hidden="true" />
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
                  <GitMerge className="h-8 w-8 text-primary" aria-hidden="true" />
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
                <BookOpen className="h-5 w-5 mr-2 text-primary" aria-hidden="true" />
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
                    <ArrowRightLeft className="h-4 w-4 ml-2 rotate-45" aria-hidden="true" />
                  </Link>
                </div>
                <div>
                  <Link
                    href="/guide/reference/api-automation"
                    className="text-primary hover:underline font-medium inline-flex items-center"
                  >
                    API Automation Guide
                    <Terminal className="h-4 w-4 ml-2" aria-hidden="true" />
                  </Link>
                </div>
                <div>
                  <Link
                    href="/guide/reference/rules"
                    className="text-primary hover:underline font-medium inline-flex items-center"
                  >
                    Validation Rules Guide
                    <ShieldCheck className="h-4 w-4 ml-2" aria-hidden="true" />
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
                <Zap className="h-5 w-5 mr-2 text-primary" aria-hidden="true" />
                Why Use OSCAL?
              </CardTitle>
            </CardHeader>
            <div className="px-6 pb-6 space-y-3">
              <p className="text-muted-foreground text-sm mb-3">
                OSCAL transforms security compliance from manual documentation to machine-readable automation
              </p>
              <ul className="space-y-3 text-sm">
                <li className="flex items-start">
                  <Shield className="h-4 w-4 mr-2 text-primary mt-0.5 flex-shrink-0" aria-hidden="true" />
                  <div>
                    <span className="font-medium text-foreground">Standardized Compliance</span>
                    <p className="text-xs text-muted-foreground">Consistent format across all security frameworks and controls</p>
                  </div>
                </li>
                <li className="flex items-start">
                  <RefreshCw className="h-4 w-4 mr-2 text-primary mt-0.5 flex-shrink-0" aria-hidden="true" />
                  <div>
                    <span className="font-medium text-foreground">Automation Ready</span>
                    <p className="text-xs text-muted-foreground">Machine-readable format enables automated validation and reporting</p>
                  </div>
                </li>
                <li className="flex items-start">
                  <Users className="h-4 w-4 mr-2 text-primary mt-0.5 flex-shrink-0" aria-hidden="true" />
                  <div>
                    <span className="font-medium text-foreground">Collaboration</span>
                    <p className="text-xs text-muted-foreground">Share and reuse compliance data across teams and organizations</p>
                  </div>
                </li>
                <li className="flex items-start">
                  <Zap className="h-4 w-4 mr-2 text-primary mt-0.5 flex-shrink-0" aria-hidden="true" />
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
                <ExternalLink className="h-5 w-5 mr-2 text-primary" aria-hidden="true" />
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
                    href="/api-docs"
                    target="_blank"
                    rel="noopener noreferrer"
                    className="text-primary hover:underline inline-flex items-center"
                  >
                    API Documentation
                    <ExternalLink className="h-3 w-3 ml-2" aria-hidden="true" />
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
                    <ExternalLink className="h-3 w-3 ml-2" aria-hidden="true" />
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
                    <ExternalLink className="h-3 w-3 ml-2" aria-hidden="true" />
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
                    <ExternalLink className="h-3 w-3 ml-2" aria-hidden="true" />
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
