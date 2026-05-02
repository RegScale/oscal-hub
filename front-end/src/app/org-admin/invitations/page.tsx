'use client';

import { useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import { ChevronLeft, Loader2, Mail, Clock } from 'lucide-react';
import { apiClient } from '@/lib/api-client';

interface InvitationRow {
  id: number;
  email: string;
  role: string;
  status: string;
  createdAt: string;
  expiresAt: string;
}

export default function InvitationsPage() {
  const router = useRouter();
  const [organizationId, setOrganizationId] = useState<number | null>(null);
  const [invites, setInvites] = useState<InvitationRow[]>([]);
  const [email, setEmail] = useState('');
  const [role, setRole] = useState<'USER' | 'ORG_ADMIN'>('USER');
  const [error, setError] = useState('');
  const [info, setInfo] = useState('');
  const [busy, setBusy] = useState(false);
  const [loading, setLoading] = useState(true);
  const [noOrg, setNoOrg] = useState(false);

  useEffect(() => {
    const storedUser = localStorage.getItem('user');
    if (!storedUser) {
      router.push('/login');
      return;
    }

    try {
      const userData = JSON.parse(storedUser);
      const isOrgAdmin = userData.orgRole === 'ORG_ADMIN';
      const isSuperAdmin = userData.globalRole === 'SUPER_ADMIN';
      const hasOrgContext = !!userData.organizationId;

      if (!isOrgAdmin && !isSuperAdmin && !hasOrgContext) {
        router.push('/');
        return;
      }

      const currentOrg = localStorage.getItem('currentOrganization');
      let orgId: number | null = null;
      if (currentOrg) {
        orgId = JSON.parse(currentOrg).id;
      } else if (userData.organizationId) {
        orgId = userData.organizationId;
      }

      if (orgId) {
        setOrganizationId(orgId);
        loadInvitations(orgId);
      } else {
        setNoOrg(true);
        setLoading(false);
      }
    } catch {
      setError('Failed to load user data. Please try logging in again.');
      setLoading(false);
    }
  }, [router]);

  const loadInvitations = async (orgId: number) => {
    try {
      setLoading(true);
      setError('');
      const data = await apiClient.listInvitations(orgId, 'PENDING');
      setInvites(data as InvitationRow[]);
    } catch (e: any) {
      setError(e?.message || 'Failed to load invitations');
    } finally {
      setLoading(false);
    }
  };

  const handleSend = async () => {
    if (!organizationId) return;
    setBusy(true);
    setError('');
    setInfo('');
    try {
      await apiClient.createInvitation({
        organizationId,
        email,
        role,
      });
      setInfo(`Invitation sent to ${email}`);
      setEmail('');
      await loadInvitations(organizationId);
    } catch (e: any) {
      setError(e?.message || 'Failed to send invitation');
    } finally {
      setBusy(false);
    }
  };

  const handleRevoke = async (id: number) => {
    if (!organizationId) return;
    setError('');
    try {
      await apiClient.revokeInvitation(id);
      await loadInvitations(organizationId);
    } catch (e: any) {
      setError(e?.message || 'Failed to revoke invitation');
    }
  };

  if (loading && invites.length === 0 && !noOrg) {
    return (
      <div className="min-h-screen bg-gray-50 dark:bg-gray-900 flex items-center justify-center">
        <Loader2 className="h-8 w-8 animate-spin text-blue-500" />
      </div>
    );
  }

  if (noOrg) {
    return (
      <div className="min-h-screen bg-gray-50 dark:bg-gray-900 py-8 px-4 sm:px-6 lg:px-8">
        <div className="max-w-4xl mx-auto">
          <div className="bg-white dark:bg-gray-800 rounded-lg shadow p-12 text-center">
            <p className="text-gray-500 dark:text-gray-400">
              Select an organization to manage invitations.
            </p>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gray-50 dark:bg-gray-900 py-8 px-4 sm:px-6 lg:px-8">
      <div className="max-w-4xl mx-auto">
        {/* Back link */}
        <button
          onClick={() => router.push('/org-admin')}
          className="flex items-center text-gray-600 dark:text-gray-400 hover:text-gray-900 dark:hover:text-white mb-6"
        >
          <ChevronLeft className="h-4 w-4 mr-1" />
          Back to Dashboard
        </button>

        <h1 className="text-3xl font-bold text-gray-900 dark:text-white mb-2">Invitations</h1>
        <p className="text-gray-600 dark:text-gray-400 mb-8">
          Invite teammates to your organization and manage pending invitations
        </p>

        {/* Alerts */}
        {error && (
          <div className="bg-red-50 dark:bg-red-900/20 border border-red-200 dark:border-red-800 rounded-lg p-4 mb-4 text-red-700 dark:text-red-300">
            {error}
          </div>
        )}
        {info && (
          <div className="bg-green-50 dark:bg-green-900/20 border border-green-200 dark:border-green-800 rounded-lg p-4 mb-4 text-green-700 dark:text-green-300">
            {info}
          </div>
        )}

        {/* Invite form */}
        <div className="bg-white dark:bg-gray-800 rounded-lg shadow-md border border-gray-200 dark:border-gray-700 p-6 mb-6">
          <h2 className="text-lg font-semibold text-gray-900 dark:text-white mb-4 flex items-center gap-2">
            <Mail className="h-5 w-5 text-blue-500" />
            Invite teammate
          </h2>
          <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
            <div>
              <label htmlFor="email" className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                Email
              </label>
              <input
                id="email"
                type="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                placeholder="teammate@example.com"
                className="w-full px-4 py-2 border border-gray-300 dark:border-gray-700 rounded-md bg-white dark:bg-gray-900 text-gray-900 dark:text-white focus:ring-2 focus:ring-blue-500 text-sm"
              />
            </div>
            <div>
              <label htmlFor="role" className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                Role
              </label>
              <select
                id="role"
                value={role}
                onChange={(e) => setRole(e.target.value as 'USER' | 'ORG_ADMIN')}
                className="w-full px-4 py-2 border border-gray-300 dark:border-gray-700 rounded-md bg-white dark:bg-gray-900 text-gray-900 dark:text-white focus:ring-2 focus:ring-blue-500 text-sm h-10"
              >
                <option value="USER">User</option>
                <option value="ORG_ADMIN">Org admin</option>
              </select>
            </div>
            <div className="flex items-end">
              <button
                onClick={handleSend}
                disabled={busy || !email}
                className="w-full px-4 py-2 bg-blue-600 text-white text-sm font-medium rounded-md hover:bg-blue-700 disabled:opacity-50 disabled:cursor-not-allowed flex items-center justify-center gap-2"
              >
                {busy && <Loader2 className="h-4 w-4 animate-spin" />}
                {busy ? 'Sending…' : 'Send invitation'}
              </button>
            </div>
          </div>
        </div>

        {/* Pending invitations table */}
        <div className="bg-white dark:bg-gray-800 rounded-lg shadow-md border border-gray-200 dark:border-gray-700">
          <div className="px-6 py-4 border-b border-gray-200 dark:border-gray-700 flex items-center gap-2">
            <Clock className="h-5 w-5 text-yellow-500" />
            <h2 className="text-lg font-semibold text-gray-900 dark:text-white">Pending invitations</h2>
          </div>
          <div className="p-6">
            {invites.length === 0 ? (
              <p className="text-center text-gray-500 dark:text-gray-400 py-8">
                No pending invitations
              </p>
            ) : (
              <div className="overflow-x-auto">
                <table className="w-full text-sm text-left">
                  <thead>
                    <tr className="border-b border-gray-200 dark:border-gray-700">
                      <th className="pb-3 font-medium text-gray-700 dark:text-gray-300">Email</th>
                      <th className="pb-3 font-medium text-gray-700 dark:text-gray-300">Role</th>
                      <th className="pb-3 font-medium text-gray-700 dark:text-gray-300">Sent</th>
                      <th className="pb-3 font-medium text-gray-700 dark:text-gray-300">Expires</th>
                      <th className="pb-3"></th>
                    </tr>
                  </thead>
                  <tbody>
                    {invites.map((inv) => (
                      <tr
                        key={inv.id}
                        className="border-b border-gray-100 dark:border-gray-700 last:border-0"
                      >
                        <td className="py-3 text-gray-900 dark:text-white">{inv.email}</td>
                        <td className="py-3 text-gray-600 dark:text-gray-400">{inv.role}</td>
                        <td className="py-3 text-gray-600 dark:text-gray-400">
                          {new Date(inv.createdAt).toLocaleString()}
                        </td>
                        <td className="py-3 text-gray-600 dark:text-gray-400">
                          {new Date(inv.expiresAt).toLocaleString()}
                        </td>
                        <td className="py-3 text-right">
                          <button
                            onClick={() => handleRevoke(inv.id)}
                            className="px-3 py-1.5 text-sm text-red-600 dark:text-red-400 border border-red-300 dark:border-red-700 rounded-md hover:bg-red-50 dark:hover:bg-red-900/20"
                          >
                            Revoke
                          </button>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
