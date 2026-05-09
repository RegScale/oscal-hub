'use client';

import { useEffect, useMemo, useState } from 'react';
import { useRouter, useSearchParams } from 'next/navigation';

type AdminUsersTab = 'users-by-org' | 'all-users' | 'archived' | 'pending-requests' | 'analytics';
const VALID_TABS: AdminUsersTab[] = ['users-by-org', 'all-users', 'archived', 'pending-requests', 'analytics'];

interface UserAnalytics {
  newUsersByMonth: Array<{ month: string; count: number }>;
  loginsByMonth: Array<{ month: string; count: number }>;
  topActiveOrganizations: Array<{ id: number; name: string; eventCount: number }>;
  staleUsers: { totalUsers: number; staleUsers: number; percentage: number; windowDays: number };
}

function tabFromParam(value: string | null): AdminUsersTab {
  return (VALID_TABS as string[]).includes(value ?? '') ? (value as AdminUsersTab) : 'users-by-org';
}
import { apiClient } from '@/lib/api-client';
import { HelpButton } from '@/components/HelpButton';

interface OrganizationSummary {
  id: number;
  name: string;
  memberCount: number;
  pendingRequestCount: number;
}

interface AccessRequest {
  id: number;
  userId: number | null;
  username: string;
  email: string;
  firstName: string;
  lastName: string;
  organizationId: number;
  organizationName: string;
  status: string;
  message: string | null;
  requestDate: string;
  reviewedBy: number | null;
  reviewedByUsername: string | null;
  reviewedDate: string | null;
  notes: string | null;
}

interface AllUser {
  id: number;
  username: string;
  email: string;
  firstName: string | null;
  lastName: string | null;
  globalRole: string;
  enabled: boolean;
  organizations: Array<{
    id: number;
    name: string;
    role: string;
  }>;
}

export default function AdminUsersPage() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const activeTab: AdminUsersTab = tabFromParam(searchParams.get('tab'));
  const setActiveTab = (tab: AdminUsersTab) => {
    const params = new URLSearchParams(searchParams.toString());
    if (tab === 'users-by-org') {
      params.delete('tab');
    } else {
      params.set('tab', tab);
    }
    const qs = params.toString();
    router.replace(qs ? `/admin/users?${qs}` : '/admin/users');
  };
  const [organizations, setOrganizations] = useState<OrganizationSummary[]>([]);
  const [pendingRequests, setPendingRequests] = useState<AccessRequest[]>([]);
  const [allUsers, setAllUsers] = useState<AllUser[]>([]);
  const [orgSearch, setOrgSearch] = useState('');
  const [requestSearch, setRequestSearch] = useState('');
  const [userSearch, setUserSearch] = useState('');
  const [userOrgFilter, setUserOrgFilter] = useState<string>('all');
  const [userRoleFilter, setUserRoleFilter] = useState<string>('all');
  const [openMenuUserId, setOpenMenuUserId] = useState<number | null>(null);

  // Reset-password modal state
  const [resetTarget, setResetTarget] = useState<AllUser | null>(null);
  const [resetMode, setResetMode] = useState<'auto' | 'manual'>('auto');
  const [resetManualPassword, setResetManualPassword] = useState('');
  const [resetNotify, setResetNotify] = useState(true);
  const [resetSubmitting, setResetSubmitting] = useState(false);
  const [resetResultPassword, setResetResultPassword] = useState<string | null>(null);
  const [resetCopied, setResetCopied] = useState(false);

  // Analytics tab data — fetched only when the tab is opened.
  const [analytics, setAnalytics] = useState<UserAnalytics | null>(null);
  const [analyticsLoading, setAnalyticsLoading] = useState(false);
  const [analyticsError, setAnalyticsError] = useState<string | null>(null);

  useEffect(() => {
    if (activeTab !== 'analytics') return;
    // Note: only depending on `activeTab`. Including `analyticsLoading` here
    // creates a self-cancelling effect — setAnalyticsLoading(true) re-runs
    // the effect, the cleanup flips `cancelled`, and the fetch result is
    // dropped, stranding the UI in the loading state forever.
    let cancelled = false;
    (async () => {
      try {
        setAnalyticsLoading(true);
        setAnalyticsError(null);
        const data = await apiClient.getUserAnalytics();
        if (!cancelled) setAnalytics(data);
      } catch (err) {
        if (!cancelled) setAnalyticsError(err instanceof Error ? err.message : 'Failed to load analytics');
      } finally {
        if (!cancelled) setAnalyticsLoading(false);
      }
    })();
    return () => { cancelled = true; };
  }, [activeTab]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);

  // Reject modal state
  const [showRejectModal, setShowRejectModal] = useState(false);
  const [rejectingRequestId, setRejectingRequestId] = useState<number | null>(null);
  const [rejectNotes, setRejectNotes] = useState('');
  const [processingAction, setProcessingAction] = useState(false);

  useEffect(() => {
    if (openMenuUserId === null) return;
    const onDocClick = (e: MouseEvent) => {
      const target = e.target as HTMLElement;
      if (!target.closest('[data-user-menu]')) {
        setOpenMenuUserId(null);
      }
    };
    const onKey = (e: KeyboardEvent) => {
      if (e.key === 'Escape') setOpenMenuUserId(null);
    };
    document.addEventListener('mousedown', onDocClick);
    document.addEventListener('keydown', onKey);
    return () => {
      document.removeEventListener('mousedown', onDocClick);
      document.removeEventListener('keydown', onKey);
    };
  }, [openMenuUserId]);

  useEffect(() => {
    // Verify user is super admin
    const storedUser = localStorage.getItem('user');
    if (storedUser) {
      const userData = JSON.parse(storedUser);
      if (userData.globalRole !== 'SUPER_ADMIN') {
        router.push('/');
        return;
      }
    } else {
      router.push('/login');
      return;
    }
    loadData();
  }, [router]);

  const loadData = async () => {
    try {
      setLoading(true);
      setError(null);

      // Load all data sets in parallel
      const [orgsResponse, requestsResponse, allUsersResponse] = await Promise.all([
        apiClient.getOrganizationsSummary(),
        apiClient.getAllPendingAccessRequests(),
        apiClient.getAllUsers(),
      ]);

      setOrganizations(orgsResponse);
      setPendingRequests(requestsResponse);
      setAllUsers(allUsersResponse);
    } catch (err: unknown) {
      console.error('Failed to load data:', err);
      setError(err instanceof Error ? err.message : 'Failed to load data');
    } finally {
      setLoading(false);
    }
  };

  const handleApprove = async (requestId: number) => {
    try {
      setProcessingAction(true);
      setError(null);
      await apiClient.approveAccessRequest(requestId);
      setSuccess('Access request approved successfully');
      await loadData();

      // Clear success message after 3 seconds
      setTimeout(() => setSuccess(null), 3000);
    } catch (err: unknown) {
      console.error('Failed to approve request:', err);
      setError(err instanceof Error ? err.message : 'Failed to approve request');
    } finally {
      setProcessingAction(false);
    }
  };

  const openRejectModal = (requestId: number) => {
    setRejectingRequestId(requestId);
    setRejectNotes('');
    setShowRejectModal(true);
  };

  const handleReject = async () => {
    if (rejectingRequestId === null) return;

    try {
      setProcessingAction(true);
      setError(null);
      await apiClient.rejectAccessRequest(rejectingRequestId, rejectNotes || undefined);
      setSuccess('Access request rejected');
      setShowRejectModal(false);
      setRejectingRequestId(null);
      setRejectNotes('');
      await loadData();

      // Clear success message after 3 seconds
      setTimeout(() => setSuccess(null), 3000);
    } catch (err: unknown) {
      console.error('Failed to reject request:', err);
      setError(err instanceof Error ? err.message : 'Failed to reject request');
    } finally {
      setProcessingAction(false);
    }
  };

  const filteredOrganizations = useMemo(() => {
    const q = orgSearch.trim().toLowerCase();
    if (!q) return organizations;
    return organizations.filter((org) => org.name.toLowerCase().includes(q));
  }, [organizations, orgSearch]);

  const baseFilteredUsers = useMemo(() => {
    const q = userSearch.trim().toLowerCase();
    return allUsers.filter((u) => {
      if (q) {
        const fullName = `${u.firstName ?? ''} ${u.lastName ?? ''}`.toLowerCase();
        const matches =
          u.username.toLowerCase().includes(q) ||
          u.email.toLowerCase().includes(q) ||
          fullName.includes(q) ||
          u.organizations.some((o) => o.name.toLowerCase().includes(q));
        if (!matches) return false;
      }
      if (userOrgFilter !== 'all') {
        if (userOrgFilter === '__none__') {
          if (u.organizations.length > 0) return false;
        } else {
          const orgId = Number(userOrgFilter);
          if (!u.organizations.some((o) => o.id === orgId)) return false;
        }
      }
      if (userRoleFilter !== 'all') {
        if (userRoleFilter === 'SUPER_ADMIN') {
          if (u.globalRole !== 'SUPER_ADMIN') return false;
        } else {
          if (!u.organizations.some((o) => o.role === userRoleFilter)) return false;
        }
      }
      return true;
    });
  }, [allUsers, userSearch, userOrgFilter, userRoleFilter]);

  const activeAllUsers = useMemo(() => allUsers.filter((u) => u.enabled), [allUsers]);
  const archivedAllUsers = useMemo(() => allUsers.filter((u) => !u.enabled), [allUsers]);
  const filteredActiveUsers = useMemo(() => baseFilteredUsers.filter((u) => u.enabled), [baseFilteredUsers]);
  const filteredArchivedUsers = useMemo(() => baseFilteredUsers.filter((u) => !u.enabled), [baseFilteredUsers]);

  const filteredRequests = useMemo(() => {
    const q = requestSearch.trim().toLowerCase();
    if (!q) return pendingRequests;
    return pendingRequests.filter((r) => {
      const fullName = `${r.firstName} ${r.lastName}`.toLowerCase();
      return (
        fullName.includes(q) ||
        r.username.toLowerCase().includes(q) ||
        r.email.toLowerCase().includes(q) ||
        r.organizationName.toLowerCase().includes(q)
      );
    });
  }, [pendingRequests, requestSearch]);

  const handleArchiveUser = async (user: AllUser) => {
    const action = user.enabled ? 'archive' : 'unarchive';
    if (!confirm(`${action === 'archive' ? 'Archive' : 'Unarchive'} ${user.username}? ${action === 'archive' ? 'They will no longer be able to log in.' : 'They will regain login access.'}`)) {
      return;
    }
    try {
      setProcessingAction(true);
      setError(null);
      if (user.enabled) {
        await apiClient.archiveUser(user.id);
        setSuccess(`${user.username} archived.`);
      } else {
        await apiClient.unarchiveUser(user.id);
        setSuccess(`${user.username} unarchived.`);
      }
      await loadData();
      setTimeout(() => setSuccess(null), 3000);
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : `Failed to ${action} user`);
    } finally {
      setProcessingAction(false);
    }
  };

  const openResetPasswordModal = (user: AllUser) => {
    setResetTarget(user);
    setResetMode('auto');
    setResetManualPassword('');
    setResetNotify(true);
    setResetResultPassword(null);
    setResetCopied(false);
    setError(null);
  };

  const closeResetPasswordModal = () => {
    if (resetSubmitting) return;
    setResetTarget(null);
    setResetManualPassword('');
    setResetResultPassword(null);
    setResetCopied(false);
  };

  const handleSubmitResetPassword = async () => {
    if (!resetTarget) return;
    if (resetMode === 'manual' && !resetManualPassword.trim()) {
      setError('Enter a password or switch to auto-generate.');
      return;
    }
    try {
      setResetSubmitting(true);
      setError(null);
      const result = await apiClient.resetUserPassword(resetTarget.id, {
        password: resetMode === 'manual' ? resetManualPassword : undefined,
        notify: resetNotify,
      });
      if (result.notified) {
        setSuccess(`Temporary password emailed to ${result.email}.`);
        setResetTarget(null);
        setTimeout(() => setSuccess(null), 5000);
      } else {
        // Keep modal open so admin can copy the plaintext password.
        setResetResultPassword(result.password ?? '');
      }
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : 'Failed to reset password');
    } finally {
      setResetSubmitting(false);
    }
  };

  const copyResetPasswordToClipboard = async () => {
    if (!resetResultPassword) return;
    try {
      await navigator.clipboard.writeText(resetResultPassword);
      setResetCopied(true);
      setTimeout(() => setResetCopied(false), 2000);
    } catch {
      // ignore — fallback prompt already visible
    }
  };

  const handleViewLogs = (user: AllUser) => {
    router.push(`/admin/logs?username=${encodeURIComponent(user.username)}`);
  };

  const formatDate = (dateString: string) => {
    return new Date(dateString).toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    });
  };

  if (loading) {
    return (
      <div className="min-h-screen flex items-center justify-center">
        <div className="text-center">
          <div className="animate-spin rounded-full h-16 w-16 border-b-2 border-blue-600 mx-auto"></div>
          <p className="mt-4 text-gray-600 dark:text-gray-400">Loading user data...</p>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gray-50 dark:bg-gray-900 py-8 px-4 sm:px-6 lg:px-8">
      <div className="max-w-7xl mx-auto">
        {/* Breadcrumb Navigation */}
        <nav className="mb-6 flex items-center text-sm text-gray-500 dark:text-gray-400">
          <button
            onClick={() => router.push('/admin')}
            className="hover:text-blue-600 dark:hover:text-blue-400 flex items-center"
          >
            <svg className="h-4 w-4 mr-1" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M3 12l2-2m0 0l7-7 7 7M5 10v10a1 1 0 001 1h3m10-11l2 2m-2-2v10a1 1 0 01-1 1h-3m-6 0a1 1 0 001-1v-4a1 1 0 011-1h2a1 1 0 011 1v4a1 1 0 001 1m-6 0h6" />
            </svg>
            Admin Dashboard
          </button>
          <svg className="h-4 w-4 mx-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5l7 7-7 7" />
          </svg>
          <span className="text-gray-900 dark:text-white font-medium">Manage Users</span>
        </nav>

        {/* Header */}
        <div className="mb-8">
          <div className="flex items-center gap-2">
            <h1 className="text-3xl font-bold text-gray-900 dark:text-white">
              Manage Users
            </h1>
            <HelpButton slug="admin-users" />
          </div>
          <p className="mt-2 text-gray-600 dark:text-gray-400">
            View users by organization and manage access requests
          </p>
        </div>

        {/* Error/Success Messages */}
        {error && (
          <div className="mb-6 bg-red-50 dark:bg-red-900/20 border border-red-200 dark:border-red-800 rounded-lg p-4">
            <p className="text-red-800 dark:text-red-200">{error}</p>
          </div>
        )}
        {success && (
          <div className="mb-6 bg-green-50 dark:bg-green-900/20 border border-green-200 dark:border-green-800 rounded-lg p-4">
            <p className="text-green-800 dark:text-green-200">{success}</p>
          </div>
        )}

        {/* Tabs */}
        <div className="border-b border-gray-200 dark:border-gray-700 mb-6">
          <nav className="-mb-px flex space-x-8">
            <button
              onClick={() => setActiveTab('users-by-org')}
              className={`py-4 px-1 border-b-2 font-medium text-sm ${
                activeTab === 'users-by-org'
                  ? 'border-blue-500 text-blue-600 dark:text-blue-400'
                  : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300 dark:text-gray-400 dark:hover:text-gray-300'
              }`}
            >
              Users by Organization
            </button>
            <button
              onClick={() => setActiveTab('all-users')}
              className={`py-4 px-1 border-b-2 font-medium text-sm ${
                activeTab === 'all-users'
                  ? 'border-blue-500 text-blue-600 dark:text-blue-400'
                  : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300 dark:text-gray-400 dark:hover:text-gray-300'
              }`}
            >
              All Users
              {activeAllUsers.length > 0 && (
                <span className="ml-2 bg-gray-100 dark:bg-gray-700 text-gray-700 dark:text-gray-300 text-xs font-medium px-2 py-0.5 rounded-full">
                  {activeAllUsers.length}
                </span>
              )}
            </button>
            <button
              onClick={() => setActiveTab('archived')}
              className={`py-4 px-1 border-b-2 font-medium text-sm ${
                activeTab === 'archived'
                  ? 'border-blue-500 text-blue-600 dark:text-blue-400'
                  : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300 dark:text-gray-400 dark:hover:text-gray-300'
              }`}
            >
              Archived
              {archivedAllUsers.length > 0 && (
                <span className="ml-2 bg-gray-100 dark:bg-gray-700 text-gray-700 dark:text-gray-300 text-xs font-medium px-2 py-0.5 rounded-full">
                  {archivedAllUsers.length}
                </span>
              )}
            </button>
            <button
              onClick={() => setActiveTab('pending-requests')}
              className={`py-4 px-1 border-b-2 font-medium text-sm flex items-center ${
                activeTab === 'pending-requests'
                  ? 'border-blue-500 text-blue-600 dark:text-blue-400'
                  : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300 dark:text-gray-400 dark:hover:text-gray-300'
              }`}
            >
              Pending Access Requests
              {pendingRequests.length > 0 && (
                <span className="ml-2 bg-red-100 dark:bg-red-900/50 text-red-800 dark:text-red-200 text-xs font-medium px-2 py-0.5 rounded-full">
                  {pendingRequests.length}
                </span>
              )}
            </button>
            <button
              onClick={() => setActiveTab('analytics')}
              className={`py-4 px-1 border-b-2 font-medium text-sm ${
                activeTab === 'analytics'
                  ? 'border-blue-500 text-blue-600 dark:text-blue-400'
                  : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300 dark:text-gray-400 dark:hover:text-gray-300'
              }`}
            >
              Analytics
            </button>
          </nav>
        </div>

        {/* Tab Content */}
        {activeTab === 'users-by-org' && (
          <>
            {organizations.length > 0 && (
              <div className="mb-4 flex items-center gap-3">
                <div className="relative flex-1 max-w-md">
                  <svg
                    className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-gray-400"
                    fill="none"
                    stroke="currentColor"
                    viewBox="0 0 24 24"
                  >
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M21 21l-4.35-4.35m0 0A7.5 7.5 0 103.5 3.5a7.5 7.5 0 0013.15 13.15z" />
                  </svg>
                  <input
                    type="text"
                    value={orgSearch}
                    onChange={(e) => setOrgSearch(e.target.value)}
                    placeholder="Search organizations..."
                    className="w-full pl-10 pr-3 py-2 border border-gray-300 dark:border-gray-700 rounded-md focus:ring-2 focus:ring-blue-500 focus:border-transparent bg-white dark:bg-gray-900 text-gray-900 dark:text-white text-sm"
                  />
                </div>
                <span className="text-sm text-gray-500 dark:text-gray-400">
                  {filteredOrganizations.length} of {organizations.length}
                </span>
              </div>
            )}
            <div className="bg-white dark:bg-gray-800 rounded-lg shadow overflow-hidden">
            {organizations.length === 0 ? (
              <div className="text-center py-12">
                <svg className="mx-auto h-12 w-12 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 21V5a2 2 0 00-2-2H7a2 2 0 00-2 2v16m14 0h2m-2 0h-5m-9 0H3m2 0h5M9 7h1m-1 4h1m4-4h1m-1 4h1m-5 10v-5a1 1 0 011-1h2a1 1 0 011 1v5m-4 0h4" />
                </svg>
                <h3 className="mt-2 text-sm font-medium text-gray-900 dark:text-white">No organizations</h3>
                <p className="mt-1 text-sm text-gray-500 dark:text-gray-400">
                  No organizations have been created yet.
                </p>
              </div>
            ) : (
              <table className="min-w-full divide-y divide-gray-200 dark:divide-gray-700">
                <thead className="bg-gray-50 dark:bg-gray-700">
                  <tr>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-300 uppercase tracking-wider">
                      Organization Name
                    </th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-300 uppercase tracking-wider">
                      Total Users
                    </th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-300 uppercase tracking-wider">
                      Pending Requests
                    </th>
                    <th className="px-6 py-3 text-right text-xs font-medium text-gray-500 dark:text-gray-300 uppercase tracking-wider">
                      Actions
                    </th>
                  </tr>
                </thead>
                <tbody className="bg-white dark:bg-gray-800 divide-y divide-gray-200 dark:divide-gray-700">
                  {filteredOrganizations.length === 0 ? (
                    <tr>
                      <td colSpan={4} className="px-6 py-12 text-center text-sm text-gray-500 dark:text-gray-400">
                        No organizations match &ldquo;{orgSearch}&rdquo;.
                      </td>
                    </tr>
                  ) : filteredOrganizations.map((org) => (
                    <tr
                      key={org.id}
                      className="hover:bg-gray-50 dark:hover:bg-gray-700 cursor-pointer"
                      onClick={() => router.push(`/admin/organizations/${org.id}`)}
                    >
                      <td className="px-6 py-4 whitespace-nowrap">
                        <div className="text-sm font-medium text-gray-900 dark:text-white">
                          {org.name}
                        </div>
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap">
                        <div className="text-sm text-gray-900 dark:text-white">
                          {org.memberCount}
                        </div>
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap">
                        {org.pendingRequestCount > 0 ? (
                          <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-yellow-100 dark:bg-yellow-900/50 text-yellow-800 dark:text-yellow-200">
                            {org.pendingRequestCount} pending
                          </span>
                        ) : (
                          <span className="text-sm text-gray-500 dark:text-gray-400">
                            None
                          </span>
                        )}
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap text-right text-sm font-medium">
                        <button
                          onClick={(e) => {
                            e.stopPropagation();
                            router.push(`/admin/organizations/${org.id}`);
                          }}
                          className="text-blue-600 dark:text-blue-400 hover:text-blue-900 dark:hover:text-blue-300"
                        >
                          View Details
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )}
            </div>
          </>
        )}

        {(activeTab === 'all-users' || activeTab === 'archived') && (() => {
          const isArchived = activeTab === 'archived';
          const tabUsers = isArchived ? filteredArchivedUsers : filteredActiveUsers;
          const tabTotal = isArchived ? archivedAllUsers.length : activeAllUsers.length;
          return (
          <>
            <div className="mb-4 flex flex-wrap items-center gap-3">
              <div className="relative flex-1 min-w-[240px] max-w-md">
                <svg
                  className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-gray-400"
                  fill="none"
                  stroke="currentColor"
                  viewBox="0 0 24 24"
                >
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M21 21l-4.35-4.35m0 0A7.5 7.5 0 103.5 3.5a7.5 7.5 0 0013.15 13.15z" />
                </svg>
                <input
                  type="text"
                  value={userSearch}
                  onChange={(e) => setUserSearch(e.target.value)}
                  placeholder={isArchived ? 'Search archived users...' : 'Search by name, username, email, or org...'}
                  className="w-full pl-10 pr-3 py-2 border border-gray-300 dark:border-gray-700 rounded-md focus:ring-2 focus:ring-blue-500 focus:border-transparent bg-white dark:bg-gray-900 text-gray-900 dark:text-white text-sm"
                />
              </div>
              <select
                value={userOrgFilter}
                onChange={(e) => setUserOrgFilter(e.target.value)}
                className="px-3 py-2 border border-gray-300 dark:border-gray-700 rounded-md focus:ring-2 focus:ring-blue-500 focus:border-transparent bg-white dark:bg-gray-900 text-gray-900 dark:text-white text-sm"
              >
                <option value="all">All organizations</option>
                <option value="__none__">No organization</option>
                {organizations.map((org) => (
                  <option key={org.id} value={String(org.id)}>{org.name}</option>
                ))}
              </select>
              <select
                value={userRoleFilter}
                onChange={(e) => setUserRoleFilter(e.target.value)}
                className="px-3 py-2 border border-gray-300 dark:border-gray-700 rounded-md focus:ring-2 focus:ring-blue-500 focus:border-transparent bg-white dark:bg-gray-900 text-gray-900 dark:text-white text-sm"
              >
                <option value="all">All roles</option>
                <option value="SUPER_ADMIN">Super Admin (global)</option>
                <option value="ORG_ADMIN">Org Admin</option>
                <option value="USER">User</option>
              </select>
              <span className="text-sm text-gray-500 dark:text-gray-400">
                {tabUsers.length} of {tabTotal}
              </span>
            </div>

            <div className="bg-white dark:bg-gray-800 rounded-lg shadow">
              {tabTotal === 0 ? (
                <div className="text-center py-12 text-sm text-gray-500 dark:text-gray-400">
                  {isArchived ? 'No archived users.' : 'No active users.'}
                </div>
              ) : (
                <table className="min-w-full divide-y divide-gray-200 dark:divide-gray-700">
                  <thead className="bg-gray-50 dark:bg-gray-700">
                    <tr>
                      <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-300 uppercase tracking-wider">
                        Name
                      </th>
                      <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-300 uppercase tracking-wider">
                        Email
                      </th>
                      <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-300 uppercase tracking-wider">
                        Global Role
                      </th>
                      <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-300 uppercase tracking-wider">
                        Organizations
                      </th>
                      <th className="px-6 py-3 text-right text-xs font-medium text-gray-500 dark:text-gray-300 uppercase tracking-wider">
                        Actions
                      </th>
                    </tr>
                  </thead>
                  <tbody className="bg-white dark:bg-gray-800 divide-y divide-gray-200 dark:divide-gray-700">
                    {tabUsers.length === 0 ? (
                      <tr>
                        <td colSpan={5} className="px-6 py-12 text-center text-sm text-gray-500 dark:text-gray-400">
                          No users match the current filters.
                        </td>
                      </tr>
                    ) : tabUsers.map((u) => {
                      const fullName = [u.firstName, u.lastName].filter(Boolean).join(' ');
                      return (
                        <tr key={u.id} className={`hover:bg-gray-50 dark:hover:bg-gray-700 ${!u.enabled ? 'opacity-60' : ''}`}>
                          <td className="px-6 py-4 whitespace-nowrap">
                            <div className="flex items-center gap-2">
                              <div className="text-sm font-medium text-gray-900 dark:text-white">
                                {fullName || '—'}
                              </div>
                              {!u.enabled && (
                                <span className="inline-flex items-center px-2 py-0.5 rounded text-xs font-medium bg-gray-200 dark:bg-gray-700 text-gray-700 dark:text-gray-300">
                                  Archived
                                </span>
                              )}
                            </div>
                            <div className="text-sm text-gray-500 dark:text-gray-400">
                              @{u.username}
                            </div>
                          </td>
                          <td className="px-6 py-4 whitespace-nowrap">
                            <div className="text-sm text-gray-900 dark:text-white">{u.email}</div>
                          </td>
                          <td className="px-6 py-4 whitespace-nowrap">
                            {u.globalRole === 'SUPER_ADMIN' ? (
                              <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-purple-100 dark:bg-purple-900/50 text-purple-800 dark:text-purple-200">
                                Super Admin
                              </span>
                            ) : (
                              <span className="text-sm text-gray-500 dark:text-gray-400">User</span>
                            )}
                          </td>
                          <td className="px-6 py-4">
                            {u.organizations.length === 0 ? (
                              <span className="text-sm text-gray-400 dark:text-gray-500 italic">None</span>
                            ) : (
                              <div className="flex flex-wrap gap-1">
                                {u.organizations.map((org) => (
                                  <button
                                    key={org.id}
                                    onClick={() => router.push(`/admin/organizations/${org.id}`)}
                                    className={`inline-flex items-center px-2 py-0.5 rounded text-xs font-medium hover:underline ${
                                      org.role === 'ORG_ADMIN'
                                        ? 'bg-blue-100 dark:bg-blue-900/40 text-blue-800 dark:text-blue-200'
                                        : 'bg-gray-100 dark:bg-gray-700 text-gray-700 dark:text-gray-300'
                                    }`}
                                    title={org.role === 'ORG_ADMIN' ? 'Org Admin' : 'User'}
                                  >
                                    {org.name}
                                    <span className="ml-1 opacity-70">
                                      {org.role === 'ORG_ADMIN' ? '· admin' : ''}
                                    </span>
                                  </button>
                                ))}
                              </div>
                            )}
                          </td>
                          <td className="px-6 py-4 whitespace-nowrap text-right text-sm font-medium">
                            <div className="relative inline-block" data-user-menu>
                              <button
                                onClick={() => setOpenMenuUserId(openMenuUserId === u.id ? null : u.id)}
                                disabled={processingAction}
                                aria-label={`Actions for ${u.username}`}
                                aria-haspopup="menu"
                                aria-expanded={openMenuUserId === u.id}
                                className="p-1.5 rounded-md text-gray-500 dark:text-gray-400 hover:bg-gray-100 dark:hover:bg-gray-700 hover:text-gray-700 dark:hover:text-gray-200 disabled:opacity-50 focus:outline-none focus:ring-2 focus:ring-blue-500"
                              >
                                <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 5v.01M12 12v.01M12 19v.01M12 6a1 1 0 110-2 1 1 0 010 2zm0 7a1 1 0 110-2 1 1 0 010 2zm0 7a1 1 0 110-2 1 1 0 010 2z" />
                                </svg>
                              </button>
                              {openMenuUserId === u.id && (
                                <div
                                  role="menu"
                                  className="absolute right-0 mt-1 w-48 origin-top-right rounded-md border border-gray-200 dark:border-gray-700 bg-white dark:bg-gray-800 shadow-lg z-20 py-1 text-left"
                                >
                                  <button
                                    role="menuitem"
                                    onClick={() => { setOpenMenuUserId(null); handleViewLogs(u); }}
                                    className="block w-full text-left px-4 py-2 text-sm text-gray-700 dark:text-gray-200 hover:bg-gray-100 dark:hover:bg-gray-700"
                                  >
                                    View logs
                                  </button>
                                  <button
                                    role="menuitem"
                                    onClick={() => { setOpenMenuUserId(null); openResetPasswordModal(u); }}
                                    disabled={!u.enabled}
                                    className="block w-full text-left px-4 py-2 text-sm text-gray-700 dark:text-gray-200 hover:bg-gray-100 dark:hover:bg-gray-700 disabled:opacity-50 disabled:cursor-not-allowed disabled:hover:bg-transparent"
                                    title={u.enabled ? '' : 'Cannot reset password for archived user'}
                                  >
                                    Reset password
                                  </button>
                                  <div className="my-1 border-t border-gray-200 dark:border-gray-700" />
                                  <button
                                    role="menuitem"
                                    onClick={() => { setOpenMenuUserId(null); handleArchiveUser(u); }}
                                    className={`block w-full text-left px-4 py-2 text-sm hover:bg-gray-100 dark:hover:bg-gray-700 ${u.enabled ? 'text-red-600 dark:text-red-400' : 'text-green-600 dark:text-green-400'}`}
                                  >
                                    {u.enabled ? 'Archive user' : 'Unarchive user'}
                                  </button>
                                </div>
                              )}
                            </div>
                          </td>
                        </tr>
                      );
                    })}
                  </tbody>
                </table>
              )}
            </div>
          </>
          );
        })()}

        {activeTab === 'pending-requests' && (
          <>
            {pendingRequests.length > 0 && (
              <div className="mb-4 flex items-center gap-3">
                <div className="relative flex-1 max-w-md">
                  <svg
                    className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-gray-400"
                    fill="none"
                    stroke="currentColor"
                    viewBox="0 0 24 24"
                  >
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M21 21l-4.35-4.35m0 0A7.5 7.5 0 103.5 3.5a7.5 7.5 0 0013.15 13.15z" />
                  </svg>
                  <input
                    type="text"
                    value={requestSearch}
                    onChange={(e) => setRequestSearch(e.target.value)}
                    placeholder="Search by name, email, username, or organization..."
                    className="w-full pl-10 pr-3 py-2 border border-gray-300 dark:border-gray-700 rounded-md focus:ring-2 focus:ring-blue-500 focus:border-transparent bg-white dark:bg-gray-900 text-gray-900 dark:text-white text-sm"
                  />
                </div>
                <span className="text-sm text-gray-500 dark:text-gray-400">
                  {filteredRequests.length} of {pendingRequests.length}
                </span>
              </div>
            )}
            <div className="bg-white dark:bg-gray-800 rounded-lg shadow overflow-hidden">
            {pendingRequests.length === 0 ? (
              <div className="text-center py-12">
                <svg className="mx-auto h-12 w-12 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
                </svg>
                <h3 className="mt-2 text-sm font-medium text-gray-900 dark:text-white">No pending requests</h3>
                <p className="mt-1 text-sm text-gray-500 dark:text-gray-400">
                  All access requests have been processed.
                </p>
              </div>
            ) : (
              <table className="min-w-full divide-y divide-gray-200 dark:divide-gray-700">
                <thead className="bg-gray-50 dark:bg-gray-700">
                  <tr>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-300 uppercase tracking-wider">
                      Name
                    </th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-300 uppercase tracking-wider">
                      Email
                    </th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-300 uppercase tracking-wider">
                      Organization
                    </th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-300 uppercase tracking-wider">
                      Request Date
                    </th>
                    <th className="px-6 py-3 text-right text-xs font-medium text-gray-500 dark:text-gray-300 uppercase tracking-wider">
                      Actions
                    </th>
                  </tr>
                </thead>
                <tbody className="bg-white dark:bg-gray-800 divide-y divide-gray-200 dark:divide-gray-700">
                  {filteredRequests.length === 0 ? (
                    <tr>
                      <td colSpan={5} className="px-6 py-12 text-center text-sm text-gray-500 dark:text-gray-400">
                        No requests match &ldquo;{requestSearch}&rdquo;.
                      </td>
                    </tr>
                  ) : filteredRequests.map((request) => (
                    <tr key={request.id} className="hover:bg-gray-50 dark:hover:bg-gray-700">
                      <td className="px-6 py-4 whitespace-nowrap">
                        <div className="text-sm font-medium text-gray-900 dark:text-white">
                          {request.firstName} {request.lastName}
                        </div>
                        <div className="text-sm text-gray-500 dark:text-gray-400">
                          @{request.username}
                        </div>
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap">
                        <div className="text-sm text-gray-900 dark:text-white">
                          {request.email}
                        </div>
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap">
                        <div className="text-sm text-gray-900 dark:text-white">
                          {request.organizationName}
                        </div>
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap">
                        <div className="text-sm text-gray-500 dark:text-gray-400">
                          {formatDate(request.requestDate)}
                        </div>
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap text-right text-sm font-medium">
                        <button
                          onClick={() => handleApprove(request.id)}
                          disabled={processingAction}
                          className="text-green-600 dark:text-green-400 hover:text-green-900 dark:hover:text-green-300 mr-4 disabled:opacity-50"
                        >
                          Approve
                        </button>
                        <button
                          onClick={() => openRejectModal(request.id)}
                          disabled={processingAction}
                          className="text-red-600 dark:text-red-400 hover:text-red-900 dark:hover:text-red-300 disabled:opacity-50"
                        >
                          Reject
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )}
            </div>
          </>
        )}

        {activeTab === 'analytics' && (() => {
          const formatMonthLabel = (ym: string) => {
            // ym is "YYYY-MM"
            const [year, month] = ym.split('-');
            const d = new Date(Number(year), Number(month) - 1, 1);
            return d.toLocaleString(undefined, { month: 'short', year: '2-digit' });
          };
          const renderBars = (data: Array<{ month: string; count: number }>, accent: string) => {
            const max = Math.max(1, ...data.map((d) => d.count));
            return (
              <div className="flex items-end gap-1.5 h-32">
                {data.map((d) => {
                  const pct = (d.count / max) * 100;
                  return (
                    <div key={d.month} className="flex-1 flex flex-col items-center gap-1 group relative">
                      <div
                        className={`w-full ${accent} rounded-t transition-all`}
                        style={{ height: `${Math.max(pct, 2)}%`, minHeight: d.count > 0 ? '4px' : '2px' }}
                        title={`${formatMonthLabel(d.month)}: ${d.count}`}
                      />
                      <span className="text-[10px] text-gray-500 dark:text-gray-400 whitespace-nowrap">
                        {formatMonthLabel(d.month)}
                      </span>
                    </div>
                  );
                })}
              </div>
            );
          };
          return (
            <div>
              {analyticsLoading && (
                <div className="text-center py-12 text-sm text-gray-500 dark:text-gray-400">Loading analytics…</div>
              )}
              {analyticsError && (
                <div className="bg-red-50 dark:bg-red-900/20 border border-red-200 dark:border-red-800 rounded-lg p-4 text-sm text-red-800 dark:text-red-200">
                  {analyticsError}
                </div>
              )}
              {analytics && !analyticsLoading && (
                <>
                  {/* KPI strip */}
                  <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4 mb-6">
                    <div className="bg-white dark:bg-gray-800 rounded-lg shadow p-5">
                      <div className="text-xs uppercase tracking-wide text-gray-500 dark:text-gray-400 mb-1">Total users</div>
                      <div className="text-3xl font-bold text-gray-900 dark:text-white">{analytics.staleUsers.totalUsers.toLocaleString()}</div>
                    </div>
                    <div className="bg-white dark:bg-gray-800 rounded-lg shadow p-5">
                      <div className="text-xs uppercase tracking-wide text-gray-500 dark:text-gray-400 mb-1">New this month</div>
                      <div className="text-3xl font-bold text-gray-900 dark:text-white">
                        {analytics.newUsersByMonth.length > 0 ? analytics.newUsersByMonth[analytics.newUsersByMonth.length - 1].count.toLocaleString() : 0}
                      </div>
                    </div>
                    <div className="bg-white dark:bg-gray-800 rounded-lg shadow p-5">
                      <div className="text-xs uppercase tracking-wide text-gray-500 dark:text-gray-400 mb-1">Logins this month</div>
                      <div className="text-3xl font-bold text-gray-900 dark:text-white">
                        {analytics.loginsByMonth.length > 0 ? analytics.loginsByMonth[analytics.loginsByMonth.length - 1].count.toLocaleString() : 0}
                      </div>
                    </div>
                    <div className="bg-white dark:bg-gray-800 rounded-lg shadow p-5">
                      <div className="text-xs uppercase tracking-wide text-gray-500 dark:text-gray-400 mb-1">Stale ({analytics.staleUsers.windowDays}-day)</div>
                      <div className="flex items-baseline gap-2">
                        <span className={`text-3xl font-bold ${analytics.staleUsers.percentage >= 50 ? 'text-amber-600 dark:text-amber-400' : 'text-gray-900 dark:text-white'}`}>
                          {analytics.staleUsers.percentage}%
                        </span>
                        <span className="text-sm text-gray-500 dark:text-gray-400">
                          {analytics.staleUsers.staleUsers.toLocaleString()} / {analytics.staleUsers.totalUsers.toLocaleString()}
                        </span>
                      </div>
                    </div>
                  </div>

                  {/* Charts row */}
                  <div className="grid grid-cols-1 lg:grid-cols-2 gap-4 mb-6">
                    <div className="bg-white dark:bg-gray-800 rounded-lg shadow p-5">
                      <h3 className="text-sm font-semibold text-gray-900 dark:text-white mb-4">New users by month</h3>
                      {renderBars(analytics.newUsersByMonth, 'bg-blue-500/80 dark:bg-blue-500/70 group-hover:bg-blue-600 dark:group-hover:bg-blue-400')}
                    </div>
                    <div className="bg-white dark:bg-gray-800 rounded-lg shadow p-5">
                      <h3 className="text-sm font-semibold text-gray-900 dark:text-white mb-4">Successful logins by month</h3>
                      {renderBars(analytics.loginsByMonth, 'bg-purple-500/80 dark:bg-purple-500/70 group-hover:bg-purple-600 dark:group-hover:bg-purple-400')}
                    </div>
                  </div>

                  {/* Top orgs */}
                  <div className="bg-white dark:bg-gray-800 rounded-lg shadow p-5 mb-6">
                    <h3 className="text-sm font-semibold text-gray-900 dark:text-white mb-1">Most active organizations</h3>
                    <p className="text-xs text-gray-500 dark:text-gray-400 mb-4">By total events from all members in the last {analytics.staleUsers.windowDays} days.</p>
                    {analytics.topActiveOrganizations.length === 0 ? (
                      <div className="text-sm text-gray-500 dark:text-gray-400 py-4 text-center">No activity recorded yet.</div>
                    ) : (
                      <div className="space-y-2">
                        {(() => {
                          const max = Math.max(1, ...analytics.topActiveOrganizations.map((o) => o.eventCount));
                          return analytics.topActiveOrganizations.map((org) => {
                            const pct = (org.eventCount / max) * 100;
                            return (
                              <button
                                key={org.id}
                                onClick={() => router.push(`/admin/organizations/${org.id}`)}
                                className="w-full text-left group"
                              >
                                <div className="flex items-center justify-between text-sm mb-1">
                                  <span className="font-medium text-gray-900 dark:text-white group-hover:underline">{org.name}</span>
                                  <span className="text-gray-500 dark:text-gray-400 tabular-nums">{org.eventCount.toLocaleString()}</span>
                                </div>
                                <div className="h-2 bg-gray-100 dark:bg-gray-700 rounded-full overflow-hidden">
                                  <div className="h-full bg-blue-500/70 dark:bg-blue-500/60" style={{ width: `${pct}%` }} />
                                </div>
                              </button>
                            );
                          });
                        })()}
                      </div>
                    )}
                  </div>
                </>
              )}
            </div>
          );
        })()}

        {/* Reset Password Modal */}
        {resetTarget && (
          <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4">
            <div className="bg-white dark:bg-gray-800 rounded-lg shadow-xl max-w-lg w-full mx-4 p-6">
              <h3 className="text-lg font-medium text-gray-900 dark:text-white mb-1">
                Reset password for @{resetTarget.username}
              </h3>
              <p className="text-sm text-gray-500 dark:text-gray-400 mb-5">
                The user will be required to change this password on their next login.
              </p>

              {!resetResultPassword ? (
                <>
                  <fieldset className="mb-4">
                    <legend className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
                      Password
                    </legend>
                    <label className="flex items-start gap-2 mb-2 cursor-pointer">
                      <input
                        type="radio"
                        name="reset-mode"
                        value="auto"
                        checked={resetMode === 'auto'}
                        onChange={() => setResetMode('auto')}
                        className="mt-1"
                      />
                      <span className="text-sm text-gray-700 dark:text-gray-200">
                        Auto-generate a secure temporary password
                      </span>
                    </label>
                    <label className="flex items-start gap-2 cursor-pointer">
                      <input
                        type="radio"
                        name="reset-mode"
                        value="manual"
                        checked={resetMode === 'manual'}
                        onChange={() => setResetMode('manual')}
                        className="mt-1"
                      />
                      <span className="text-sm text-gray-700 dark:text-gray-200">
                        Set a specific password
                      </span>
                    </label>
                    {resetMode === 'manual' && (
                      <input
                        type="text"
                        value={resetManualPassword}
                        onChange={(e) => setResetManualPassword(e.target.value)}
                        autoComplete="new-password"
                        placeholder="Enter new password"
                        className="mt-2 ml-6 w-[calc(100%-1.5rem)] px-3 py-2 text-sm border border-gray-300 dark:border-gray-700 rounded-md focus:ring-2 focus:ring-blue-500 focus:border-transparent bg-white dark:bg-gray-900 text-gray-900 dark:text-white font-mono"
                      />
                    )}
                  </fieldset>

                  <fieldset className="mb-4">
                    <legend className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
                      Delivery
                    </legend>
                    <label className="flex items-start gap-2 mb-2 cursor-pointer">
                      <input
                        type="radio"
                        name="reset-notify"
                        value="email"
                        checked={resetNotify}
                        onChange={() => setResetNotify(true)}
                        className="mt-1"
                      />
                      <span className="text-sm text-gray-700 dark:text-gray-200">
                        Email it to the user ({resetTarget.email})
                      </span>
                    </label>
                    <label className="flex items-start gap-2 cursor-pointer">
                      <input
                        type="radio"
                        name="reset-notify"
                        value="oob"
                        checked={!resetNotify}
                        onChange={() => setResetNotify(false)}
                        className="mt-1"
                      />
                      <span className="text-sm text-gray-700 dark:text-gray-200">
                        Show me the password — I&rsquo;ll deliver it out of band
                      </span>
                    </label>
                  </fieldset>

                  {error && (
                    <div className="mb-4 bg-red-50 dark:bg-red-900/20 border border-red-200 dark:border-red-800 rounded-md p-3 text-sm text-red-800 dark:text-red-200">
                      {error}
                    </div>
                  )}

                  <div className="flex justify-end gap-3">
                    <button
                      onClick={closeResetPasswordModal}
                      disabled={resetSubmitting}
                      className="px-4 py-2 border border-gray-300 dark:border-gray-700 rounded-md text-sm text-gray-700 dark:text-gray-300 hover:bg-gray-50 dark:hover:bg-gray-700 disabled:opacity-50"
                    >
                      Cancel
                    </button>
                    <button
                      onClick={handleSubmitResetPassword}
                      disabled={resetSubmitting}
                      className="px-4 py-2 bg-amber-600 text-white rounded-md text-sm hover:bg-amber-700 disabled:opacity-50"
                    >
                      {resetSubmitting ? 'Resetting…' : 'Reset password'}
                    </button>
                  </div>
                </>
              ) : (
                <>
                  <div className="mb-4 rounded-md border border-amber-200 dark:border-amber-800 bg-amber-50 dark:bg-amber-900/20 p-3 text-sm text-amber-900 dark:text-amber-100">
                    Password reset. Copy this now — it will not be shown again.
                  </div>
                  <div className="mb-4">
                    <label className="block text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wide mb-1">
                      Temporary password
                    </label>
                    <div className="flex items-stretch gap-2">
                      <input
                        type="text"
                        readOnly
                        value={resetResultPassword}
                        onFocus={(e) => e.currentTarget.select()}
                        className="flex-1 px-3 py-2 text-sm border border-gray-300 dark:border-gray-700 rounded-md bg-gray-50 dark:bg-gray-900 text-gray-900 dark:text-white font-mono"
                      />
                      <button
                        onClick={copyResetPasswordToClipboard}
                        className="px-3 py-2 bg-blue-600 text-white rounded-md text-sm hover:bg-blue-700"
                      >
                        {resetCopied ? 'Copied' : 'Copy'}
                      </button>
                    </div>
                  </div>
                  <div className="flex justify-end">
                    <button
                      onClick={closeResetPasswordModal}
                      className="px-4 py-2 bg-gray-700 text-white rounded-md text-sm hover:bg-gray-800"
                    >
                      Done
                    </button>
                  </div>
                </>
              )}
            </div>
          </div>
        )}

        {/* Reject Confirmation Modal */}
        {showRejectModal && (
          <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
            <div className="bg-white dark:bg-gray-800 rounded-lg shadow-xl max-w-md w-full mx-4 p-6">
              <h3 className="text-lg font-medium text-gray-900 dark:text-white mb-4">
                Reject Access Request
              </h3>
              <p className="text-sm text-gray-500 dark:text-gray-400 mb-4">
                Are you sure you want to reject this access request? You can optionally provide a reason below.
              </p>
              <textarea
                value={rejectNotes}
                onChange={(e) => setRejectNotes(e.target.value)}
                placeholder="Reason for rejection (optional)"
                rows={3}
                className="w-full px-4 py-2 border border-gray-300 dark:border-gray-700 rounded-md focus:ring-2 focus:ring-blue-500 focus:border-transparent bg-white dark:bg-gray-900 text-gray-900 dark:text-white mb-4"
              />
              <div className="flex justify-end space-x-3">
                <button
                  onClick={() => {
                    setShowRejectModal(false);
                    setRejectingRequestId(null);
                    setRejectNotes('');
                  }}
                  disabled={processingAction}
                  className="px-4 py-2 border border-gray-300 dark:border-gray-700 rounded-md text-gray-700 dark:text-gray-300 hover:bg-gray-50 dark:hover:bg-gray-700 disabled:opacity-50"
                >
                  Cancel
                </button>
                <button
                  onClick={handleReject}
                  disabled={processingAction}
                  className="px-4 py-2 bg-red-600 text-white rounded-md hover:bg-red-700 focus:outline-none focus:ring-2 focus:ring-red-500 disabled:opacity-50"
                >
                  {processingAction ? 'Rejecting...' : 'Reject'}
                </button>
              </div>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
