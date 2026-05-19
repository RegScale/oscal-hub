'use client';

import { useEffect, useRef, useState } from 'react';
import { useRouter } from 'next/navigation';
import { Users, ClipboardCheck, LogIn, Activity, ChevronRight, Loader2, Building2, Mail, Sparkles, BarChart3, Pencil, Check, X, Upload, Trash2 } from 'lucide-react';
import { apiClient } from '@/lib/api-client';
import { aiClient } from '@/lib/ai-client';
import { HelpButton } from '@/components/HelpButton';
import { useAuth } from '@/contexts/AuthContext';
import { toast } from 'sonner';

interface DashboardSummary {
  totalMembers: number;
  pendingRequests: number;
  loginsThisMonth: number;
  operationsThisMonth: number;
}

interface OrgOption {
  id: number;
  name: string;
}

export default function OrgAdminDashboardPage() {
  const router = useRouter();
  const { updateUser } = useAuth();
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [summary, setSummary] = useState<DashboardSummary | null>(null);
  const [orgName, setOrgName] = useState('');
  const [selectedOrgId, setSelectedOrgId] = useState<number | null>(null);
  const [availableOrgs, setAvailableOrgs] = useState<OrgOption[]>([]);
  const [needsOrgSelection, setNeedsOrgSelection] = useState(false);
  const [aiConfigured, setAiConfigured] = useState<boolean | null>(null);
  // Org name inline editor state.
  const [editingName, setEditingName] = useState(false);
  const [nameDraft, setNameDraft] = useState('');
  const [savingName, setSavingName] = useState(false);
  // Org logo upload state.
  const [logoUrl, setLogoUrl] = useState<string | null>(null);
  const [logoBusy, setLogoBusy] = useState(false);
  const logoFileRef = useRef<HTMLInputElement>(null);

  useEffect(() => {
    const storedUser = localStorage.getItem('user');
    if (!storedUser) {
      router.push('/login');
      return;
    }

    let userData: Record<string, unknown>;
    try {
      userData = JSON.parse(storedUser);
    } catch {
      router.push('/login');
      return;
    }

    const isOrgAdmin = userData.orgRole === 'ORG_ADMIN';
    const isSuperAdmin = userData.globalRole === 'SUPER_ADMIN';
    const hasOrgContext = !!userData.organizationId;

    // Allow access if: ORG_ADMIN, SUPER_ADMIN, or user has an org selected
    // (backend enforces real authorization via @PreAuthorize)
    if (!isOrgAdmin && !isSuperAdmin && !hasOrgContext) {
      router.push('/');
      return;
    }

    // Try to find an org: currentOrganization > user's org > prompt to pick
    const currentOrg = localStorage.getItem('currentOrganization');
    if (currentOrg) {
      try {
        const orgData = JSON.parse(currentOrg);
        setOrgName(orgData.name || '');
        setSelectedOrgId(orgData.id);
        fetchSummary(orgData.id);
        return;
      } catch {
        // fall through
      }
    }

    if (userData.organizationId) {
      setOrgName((userData.organizationName as string) || '');
      setSelectedOrgId(userData.organizationId as number);
      fetchSummary(userData.organizationId as number);
      return;
    }

    // No org found — SUPER_ADMIN needs to pick one
    if (isSuperAdmin) {
      loadAvailableOrgs();
    } else {
      setError('No organization selected');
      setLoading(false);
    }
  }, [router]);

  const loadAvailableOrgs = async () => {
    try {
      const orgs = await apiClient.getOrganizationsSummary();
      const orgOptions = orgs.map((o) => ({ id: o.id, name: o.name }));
      setAvailableOrgs(orgOptions);
      if (orgOptions.length === 1) {
        // Auto-select if only one org
        selectOrg(orgOptions[0]);
      } else {
        setNeedsOrgSelection(true);
        setLoading(false);
      }
    } catch (err) {
      console.error('Failed to load organizations:', err);
      setError('Failed to load organizations. Please try again.');
      setLoading(false);
    }
  };

  const selectOrg = (org: OrgOption) => {
    setSelectedOrgId(org.id);
    setOrgName(org.name);
    setNeedsOrgSelection(false);
    // Persist selection so sub-pages also work
    localStorage.setItem('currentOrganization', JSON.stringify({ id: org.id, name: org.name }));
    fetchSummary(org.id);
  };

  const beginEditName = () => {
    setNameDraft(orgName);
    setEditingName(true);
  };

  const cancelEditName = () => {
    setEditingName(false);
    setNameDraft('');
  };

  const saveName = async () => {
    if (!selectedOrgId) return;
    const trimmed = nameDraft.trim();
    if (!trimmed) {
      toast.error('Organization name cannot be empty');
      return;
    }
    if (trimmed === orgName) {
      cancelEditName();
      return;
    }
    setSavingName(true);
    try {
      const updated = await apiClient.updateOrgAdminOrganization(selectedOrgId, { name: trimmed });
      setOrgName(updated.name);
      setEditingName(false);
      // Refresh persisted org selection so other pages (sidebar, dashboard,
      // OSCAL builder, etc.) see the new name without a hard reload.
      localStorage.setItem(
        'currentOrganization',
        JSON.stringify({ id: updated.id, name: updated.name }),
      );
      // The cached `user` blob also carries organizationName — keep it in sync
      // so the dashboard greeting and the AI wizard org picker stay current.
      try {
        const stored = localStorage.getItem('user');
        if (stored) {
          const userData = JSON.parse(stored);
          if (userData.organizationId === updated.id) {
            userData.organizationName = updated.name;
            localStorage.setItem('user', JSON.stringify(userData));
          }
        }
      } catch {
        /* ignore — best-effort cache refresh */
      }
      broadcastOrgUpdate(updated.id, updated.name);
      toast.success('Organization renamed');
    } catch (err) {
      const msg = err instanceof Error ? err.message : 'Unknown error';
      toast.error('Failed to rename organization: ' + msg);
    } finally {
      setSavingName(false);
    }
  };

  const refreshLogo = async (organizationId: number) => {
    try {
      const profile = await apiClient.getOrgAdminOrganization(organizationId);
      setLogoUrl(profile.logoUrl ?? null);
    } catch (err) {
      // Non-fatal — we just don't show the logo. The main dashboard already
      // surfaces auth/load failures via the analytics path.
      console.warn('Failed to load org logo:', err);
      setLogoUrl(null);
    }
  };

  const broadcastOrgUpdate = (id: number, name: string) => {
    // Re-hydrate AuthContext from localStorage and tell the top-nav switcher to
    // refetch — same channel used by the rename flow so the new logo shows up
    // in the dropdown and any other consumer of useAuth().user.
    updateUser();
    window.dispatchEvent(new CustomEvent('organization-updated', {
      detail: { id, name },
    }));
  };

  const handleLogoFilePicked = async (file: File | null) => {
    if (!file || !selectedOrgId) return;
    setLogoBusy(true);
    try {
      const result = await apiClient.uploadOrgAdminLogo(selectedOrgId, file);
      setLogoUrl(result.logoUrl);
      broadcastOrgUpdate(selectedOrgId, orgName);
      toast.success('Logo updated');
    } catch (err) {
      const msg = err instanceof Error ? err.message : 'Unknown error';
      toast.error('Failed to upload logo: ' + msg);
    } finally {
      setLogoBusy(false);
      // Allow re-picking the same filename.
      if (logoFileRef.current) logoFileRef.current.value = '';
    }
  };

  const handleDeleteLogo = async () => {
    if (!selectedOrgId || !logoUrl) return;
    setLogoBusy(true);
    try {
      await apiClient.deleteOrgAdminLogo(selectedOrgId);
      setLogoUrl(null);
      broadcastOrgUpdate(selectedOrgId, orgName);
      toast.success('Logo removed');
    } catch (err) {
      const msg = err instanceof Error ? err.message : 'Unknown error';
      toast.error('Failed to remove logo: ' + msg);
    } finally {
      setLogoBusy(false);
    }
  };

  const fetchSummary = async (organizationId: number) => {
    try {
      setLoading(true);
      setError(null);
      const data = await apiClient.getOrgAnalyticsSummary(organizationId);
      setSummary(data);
    } catch (err) {
      console.error('Failed to fetch summary:', err);
      setError('Failed to load dashboard data. Please try again.');
    } finally {
      setLoading(false);
    }
    aiClient
      .getSettingsStatus(organizationId)
      .then((s) => setAiConfigured(s.enabled))
      .catch(() => setAiConfigured(false));
    refreshLogo(organizationId);
  };

  if (loading) {
    return (
      <div className="min-h-screen bg-gray-50 dark:bg-gray-900 flex items-center justify-center">
        <Loader2 className="h-8 w-8 animate-spin text-blue-500" />
      </div>
    );
  }

  // Org selection screen for SUPER_ADMIN
  if (needsOrgSelection) {
    return (
      <div className="min-h-screen bg-gray-50 dark:bg-gray-900 py-8 px-4 sm:px-6 lg:px-8">
        <div className="max-w-2xl mx-auto">
          <div className="text-center mb-8">
            <h1 className="text-3xl font-bold text-gray-900 dark:text-white">Select Organization</h1>
            <p className="mt-2 text-gray-600 dark:text-gray-400">Choose which organization to manage</p>
          </div>
          {availableOrgs.length === 0 ? (
            <div className="bg-white dark:bg-gray-800 rounded-lg shadow p-8 text-center">
              <p className="text-gray-500 dark:text-gray-400">No organizations found.</p>
            </div>
          ) : (
            <div className="space-y-3">
              {availableOrgs.map((org) => (
                <button
                  key={org.id}
                  onClick={() => selectOrg(org)}
                  className="w-full flex items-center gap-4 bg-white dark:bg-gray-800 rounded-lg shadow-md hover:shadow-lg border border-gray-200 dark:border-gray-700 hover:border-blue-500 dark:hover:border-blue-400 p-5 text-left transition-all"
                >
                  <div className="flex items-center justify-center w-12 h-12 bg-blue-100 dark:bg-blue-900/30 rounded-lg">
                    <Building2 className="h-6 w-6 text-blue-600 dark:text-blue-400" />
                  </div>
                  <div>
                    <h3 className="text-lg font-medium text-gray-900 dark:text-white">{org.name}</h3>
                  </div>
                  <ChevronRight className="h-5 w-5 text-gray-400 ml-auto" />
                </button>
              ))}
            </div>
          )}
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="min-h-screen bg-gray-50 dark:bg-gray-900 py-8 px-4 sm:px-6 lg:px-8">
        <div className="max-w-6xl mx-auto">
          <div className="bg-red-50 dark:bg-red-900/20 border border-red-200 dark:border-red-800 rounded-lg p-4 text-red-700 dark:text-red-300">
            {error}
          </div>
        </div>
      </div>
    );
  }

  const statCards = [
    { label: 'Total Members', value: summary?.totalMembers ?? 0, icon: Users, color: 'blue' },
    { label: 'Pending Requests', value: summary?.pendingRequests ?? 0, icon: ClipboardCheck, color: 'yellow', badge: true },
    { label: 'Logins This Month', value: summary?.loginsThisMonth ?? 0, icon: LogIn, color: 'green' },
    { label: 'Operations This Month', value: summary?.operationsThisMonth ?? 0, icon: Activity, color: 'purple' },
  ];

  const colorMap: Record<string, { bg: string; iconBg: string; text: string }> = {
    blue: { bg: 'bg-blue-50 dark:bg-blue-900/20', iconBg: 'bg-blue-100 dark:bg-blue-900/30', text: 'text-blue-600 dark:text-blue-400' },
    yellow: { bg: 'bg-yellow-50 dark:bg-yellow-900/20', iconBg: 'bg-yellow-100 dark:bg-yellow-900/30', text: 'text-yellow-600 dark:text-yellow-400' },
    green: { bg: 'bg-green-50 dark:bg-green-900/20', iconBg: 'bg-green-100 dark:bg-green-900/30', text: 'text-green-600 dark:text-green-400' },
    purple: { bg: 'bg-purple-50 dark:bg-purple-900/20', iconBg: 'bg-purple-100 dark:bg-purple-900/30', text: 'text-purple-600 dark:text-purple-400' },
    indigo: { bg: 'bg-indigo-50 dark:bg-indigo-900/20', iconBg: 'bg-indigo-100 dark:bg-indigo-900/30', text: 'text-indigo-600 dark:text-indigo-400' },
  };

  type QuickLink = {
    label: string;
    description: string;
    href: string;
    icon: typeof Users;
    color: string;
    badge?: { text: string; tone: 'success' | 'warning' };
  };

  const baseQuickLinks: QuickLink[] = [
    { label: 'Manage Users', description: 'View and manage organization members', href: '/org-admin/users', icon: Users, color: 'blue' },
    { label: 'Access Requests', description: 'Review pending access requests', href: '/org-admin/requests', icon: ClipboardCheck, color: 'green' },
    { label: 'Invitations', description: 'Invite teammates and manage pending invitations', href: '/org-admin/invitations', icon: Mail, color: 'yellow' },
    { label: 'Usage Analytics', description: 'View organization usage metrics', href: '/org-admin/analytics', icon: Activity, color: 'purple' },
  ];

  const aiQuickLinks: QuickLink[] = aiConfigured === true
    ? [
        {
          label: 'AI Usage Analytics',
          description: 'Sessions, tokens, and estimated cost for this organization',
          href: '/org-admin/ai-analytics',
          icon: BarChart3,
          color: 'indigo',
        },
      ]
    : [];

  const quickLinks: QuickLink[] = [
    ...baseQuickLinks,
    ...aiQuickLinks,
    {
      label: 'AI Settings',
      description: 'Configure your Anthropic API key to enable AI-assisted authoring',
      href: '/org-admin/ai-settings',
      icon: Sparkles,
      color: 'indigo',
      badge: aiConfigured === null
        ? undefined
        : aiConfigured
          ? { text: 'Configured', tone: 'success' }
          : { text: 'Requires Setup', tone: 'warning' },
    },
  ];

  return (
    <div className="min-h-screen bg-gray-50 dark:bg-gray-900 py-8 px-4 sm:px-6 lg:px-8">
      <div className="max-w-6xl mx-auto">
        {/* Header */}
        <div className="mb-12">
          <div className="flex items-center gap-3 flex-wrap">
            <div className="relative group shrink-0">
              {logoUrl ? (
                <img
                  src={logoUrl}
                  alt={orgName || 'Organization logo'}
                  className="h-12 w-12 rounded-lg object-contain bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-700"
                />
              ) : (
                <div className="flex items-center justify-center w-12 h-12 bg-blue-100 dark:bg-blue-900/30 rounded-lg">
                  <Building2 className="h-6 w-6 text-blue-600 dark:text-blue-400" />
                </div>
              )}
              {/* Hover overlay: upload (always) + delete (only when logo present). */}
              {selectedOrgId !== null && (
                <div
                  className="absolute inset-0 flex items-center justify-center gap-1 rounded-lg bg-black/50 opacity-0 group-hover:opacity-100 transition-opacity"
                  aria-hidden={logoBusy ? 'false' : undefined}
                >
                  {logoBusy ? (
                    <Loader2 className="h-5 w-5 animate-spin text-white" />
                  ) : (
                    <>
                      <button
                        type="button"
                        onClick={() => logoFileRef.current?.click()}
                        aria-label={logoUrl ? 'Replace logo' : 'Upload logo'}
                        title={logoUrl ? 'Replace logo' : 'Upload logo'}
                        className="p-1 rounded text-white hover:bg-white/20"
                      >
                        <Upload className="h-4 w-4" />
                      </button>
                      {logoUrl && (
                        <button
                          type="button"
                          onClick={handleDeleteLogo}
                          aria-label="Remove logo"
                          title="Remove logo"
                          className="p-1 rounded text-white hover:bg-red-500/40"
                        >
                          <Trash2 className="h-4 w-4" />
                        </button>
                      )}
                    </>
                  )}
                </div>
              )}
              <input
                ref={logoFileRef}
                type="file"
                accept="image/png,image/jpeg,image/jpg,image/svg+xml"
                className="hidden"
                onChange={(e) => handleLogoFilePicked(e.target.files?.[0] ?? null)}
              />
            </div>
            {editingName ? (
              <div className="flex items-center gap-2 flex-wrap">
                <input
                  aria-label="Organization name"
                  autoFocus
                  type="text"
                  value={nameDraft}
                  disabled={savingName}
                  onChange={(e) => setNameDraft(e.target.value)}
                  onKeyDown={(e) => {
                    if (e.key === 'Enter') saveName();
                    if (e.key === 'Escape') cancelEditName();
                  }}
                  className="text-3xl font-bold bg-transparent border-b-2 border-blue-500 focus:outline-none px-1 text-gray-900 dark:text-white min-w-[16ch]"
                />
                <button
                  type="button"
                  onClick={saveName}
                  disabled={savingName}
                  aria-label="Save organization name"
                  title="Save"
                  className="p-2 rounded-md text-green-600 hover:bg-green-50 dark:hover:bg-green-900/30 disabled:opacity-50"
                >
                  {savingName ? <Loader2 className="h-5 w-5 animate-spin" /> : <Check className="h-5 w-5" />}
                </button>
                <button
                  type="button"
                  onClick={cancelEditName}
                  disabled={savingName}
                  aria-label="Cancel rename"
                  title="Cancel"
                  className="p-2 rounded-md text-gray-500 hover:bg-gray-100 dark:hover:bg-gray-800 disabled:opacity-50"
                >
                  <X className="h-5 w-5" />
                </button>
              </div>
            ) : (
              <>
                <h1 className="text-3xl font-bold text-gray-900 dark:text-white">
                  {orgName || 'Organization'}
                </h1>
                {selectedOrgId !== null && orgName && (
                  <button
                    type="button"
                    onClick={beginEditName}
                    aria-label="Edit organization name"
                    title="Rename organization"
                    className="p-2 rounded-md text-gray-500 hover:text-blue-600 hover:bg-blue-50 dark:hover:bg-blue-900/30 transition-colors"
                  >
                    <Pencil className="h-4 w-4" />
                  </button>
                )}
                <HelpButton slug="org-admin" />
              </>
            )}
          </div>
          <p className="mt-2 text-gray-600 dark:text-gray-400">
            Admin Panel — manage members, invitations, AI settings, and analytics.
          </p>
        </div>

        {/* Stat Cards */}
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6 mb-8">
          {statCards.map((card) => {
            const colors = colorMap[card.color];
            const Icon = card.icon;
            return (
              <div
                key={card.label}
                className="bg-white dark:bg-gray-800 rounded-lg shadow-md border border-gray-200 dark:border-gray-700 p-6"
              >
                <div className="flex items-center justify-between">
                  <div>
                    <p className="text-sm text-gray-500 dark:text-gray-400">{card.label}</p>
                    <div className="flex items-center gap-2">
                      <p className="text-2xl font-bold text-gray-900 dark:text-white">{card.value}</p>
                      {card.badge && card.value > 0 && (
                        <span className="bg-red-100 dark:bg-red-900/50 text-red-800 dark:text-red-200 text-xs font-medium px-2 py-0.5 rounded-full">
                          New
                        </span>
                      )}
                    </div>
                  </div>
                  <div className={`p-3 rounded-lg ${colors.iconBg}`}>
                    <Icon className={`h-6 w-6 ${colors.text}`} />
                  </div>
                </div>
              </div>
            );
          })}
        </div>

        {/* Quick Links */}
        <h2 className="text-xl font-semibold text-gray-900 dark:text-white mb-4">Quick Actions</h2>
        <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
          {quickLinks.map((link) => {
            const colors = colorMap[link.color];
            const Icon = link.icon;
            return (
              <button
                key={link.label}
                onClick={() => router.push(link.href)}
                className="group bg-white dark:bg-gray-800 rounded-lg shadow-md hover:shadow-lg transition-all duration-200 p-8 text-left border border-gray-200 dark:border-gray-700 hover:border-blue-500 dark:hover:border-blue-400"
              >
                <div className="flex items-start justify-between mb-6">
                  <div className={`flex items-center justify-center w-16 h-16 ${colors.iconBg} rounded-lg group-hover:bg-blue-200 dark:group-hover:bg-blue-900/50 transition-colors`}>
                    <Icon className={`h-8 w-8 ${colors.text}`} />
                  </div>
                  {link.badge && (
                    <span
                      className={
                        link.badge.tone === 'success'
                          ? 'inline-flex items-center gap-1 rounded-full bg-green-100 dark:bg-green-900/40 px-2.5 py-0.5 text-xs font-medium text-green-800 dark:text-green-200'
                          : 'inline-flex items-center gap-1 rounded-full bg-amber-100 dark:bg-amber-900/40 px-2.5 py-0.5 text-xs font-medium text-amber-800 dark:text-amber-200'
                      }
                    >
                      <span
                        className={
                          link.badge.tone === 'success'
                            ? 'h-1.5 w-1.5 rounded-full bg-green-500'
                            : 'h-1.5 w-1.5 rounded-full bg-amber-500'
                        }
                      />
                      {link.badge.text}
                    </span>
                  )}
                </div>
                <h3 className="text-lg font-semibold text-gray-900 dark:text-white mb-2">{link.label}</h3>
                <p className="text-sm text-gray-600 dark:text-gray-400 mb-4">{link.description}</p>
                <div className="flex items-center text-blue-500 text-sm font-medium">
                  Go to {link.label.toLowerCase()}
                  <ChevronRight className="h-4 w-4 ml-1" />
                </div>
              </button>
            );
          })}
        </div>
      </div>
    </div>
  );
}
