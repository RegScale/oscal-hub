'use client';

import { useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import { ChevronLeft, Search, Loader2, Lock, Unlock, UserX, UserCheck, KeyRound, UserPlus } from 'lucide-react';
import { apiClient } from '@/lib/api-client';

interface OrgUser {
  userId: number;
  username: string;
  email: string;
  firstName: string;
  lastName: string;
  role: string;
  status: string;
  joinedAt: string;
  updatedAt: string;
}

interface AllUser {
  id: number;
  username: string;
  email: string;
  firstName: string;
  lastName: string;
  globalRole: string;
  enabled: boolean;
}

interface OrgOption {
  id: number;
  name: string;
}

export default function OrgAdminUsersPage() {
  const router = useRouter();
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);
  const [users, setUsers] = useState<OrgUser[]>([]);
  const [searchQuery, setSearchQuery] = useState('');
  const [statusFilter, setStatusFilter] = useState<string>('ALL');
  const [roleFilter, setRoleFilter] = useState<string>('ALL');
  const [processingUserId, setProcessingUserId] = useState<number | null>(null);
  const [organizationId, setOrganizationId] = useState<number | null>(null);
  const [showPasswordModal, setShowPasswordModal] = useState(false);
  const [tempPasswordData, setTempPasswordData] = useState<{ tempPassword: string; username: string; email: string } | null>(null);
  const [isSuperAdmin, setIsSuperAdmin] = useState(false);
  const [activeTab, setActiveTab] = useState<'org-users' | 'assign-users'>('org-users');

  // Super admin assign state
  const [allUsers, setAllUsers] = useState<AllUser[]>([]);
  const [allOrgs, setAllOrgs] = useState<OrgOption[]>([]);
  const [assignUserId, setAssignUserId] = useState<number | null>(null);
  const [assignOrgId, setAssignOrgId] = useState<number | null>(null);
  const [assignRole, setAssignRole] = useState<string>('USER');
  const [assignLoading, setAssignLoading] = useState(false);
  const [assignSearchQuery, setAssignSearchQuery] = useState('');

  useEffect(() => {
    const storedUser = localStorage.getItem('user');
    if (!storedUser) {
      router.push('/login');
      return;
    }

    try {
      const userData = JSON.parse(storedUser);
      const isOrgAdmin = userData.orgRole === 'ORG_ADMIN';
      const superAdmin = userData.globalRole === 'SUPER_ADMIN';
      const hasOrgContext = !!userData.organizationId;

      setIsSuperAdmin(superAdmin);

      if (!isOrgAdmin && !superAdmin && !hasOrgContext) {
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
        loadUsers(orgId);
      } else {
        setError('No organization selected');
        setLoading(false);
      }
    } catch {
      setError('Failed to load user data. Please try logging in again.');
      setLoading(false);
    }
  }, [router]);

  const loadUsers = async (orgId: number) => {
    try {
      setLoading(true);
      setError(null);
      const data = await apiClient.getOrgUsers(orgId);
      setUsers(data);
    } catch (err) {
      console.error('Failed to load users:', err);
      setError(err instanceof Error ? err.message : 'Failed to load users');
    } finally {
      setLoading(false);
    }
  };

  const loadSuperAdminData = async () => {
    try {
      setAssignLoading(true);
      setError(null);
      const [usersData, orgsData] = await Promise.all([
        apiClient.getAllUsers(),
        apiClient.getOrganizationsSummary(),
      ]);
      setAllUsers(usersData);
      setAllOrgs(orgsData.map((o) => ({ id: o.id, name: o.name })));
    } catch (err) {
      console.error('Failed to load super admin data:', err);
      setError(err instanceof Error ? err.message : 'Failed to load data');
    } finally {
      setAssignLoading(false);
    }
  };

  const handleTabChange = (tab: 'org-users' | 'assign-users') => {
    setActiveTab(tab);
    if (tab === 'assign-users' && allUsers.length === 0) {
      loadSuperAdminData();
    }
  };

  const handleAssignUser = async () => {
    if (!assignUserId || !assignOrgId) return;
    try {
      setAssignLoading(true);
      setError(null);
      setSuccess(null);
      await apiClient.addMemberToOrg(assignOrgId, assignUserId, assignRole);
      const assignedUser = allUsers.find((u) => u.id === assignUserId);
      const assignedOrg = allOrgs.find((o) => o.id === assignOrgId);
      setSuccess(`${assignedUser?.username || 'User'} assigned to ${assignedOrg?.name || 'organization'} as ${assignRole}`);
      setAssignUserId(null);
      setAssignOrgId(null);
      setAssignRole('USER');
      // Reload org users if assigned to current org
      if (assignOrgId === organizationId) {
        loadUsers(organizationId!);
      }
    } catch (err) {
      console.error('Failed to assign user:', err);
      setError(err instanceof Error ? err.message : 'Failed to assign user');
    } finally {
      setAssignLoading(false);
    }
  };

  const handleAction = async (userId: number, action: 'lock' | 'unlock' | 'deactivate' | 'reactivate') => {
    if (!organizationId) return;
    try {
      setProcessingUserId(userId);
      setError(null);
      setSuccess(null);

      switch (action) {
        case 'lock':
          await apiClient.lockOrgUser(organizationId, userId);
          setSuccess('User locked successfully');
          break;
        case 'unlock':
          await apiClient.unlockOrgUser(organizationId, userId);
          setSuccess('User unlocked successfully');
          break;
        case 'deactivate':
          await apiClient.deactivateOrgUser(organizationId, userId);
          setSuccess('User deactivated successfully');
          break;
        case 'reactivate':
          await apiClient.reactivateOrgUser(organizationId, userId);
          setSuccess('User reactivated successfully');
          break;
      }

      await loadUsers(organizationId);
    } catch (err) {
      console.error(`Failed to ${action} user:`, err);
      setError(err instanceof Error ? err.message : `Failed to ${action} user`);
    } finally {
      setProcessingUserId(null);
    }
  };

  const handleResetPassword = async (userId: number) => {
    if (!organizationId) return;
    try {
      setProcessingUserId(userId);
      setError(null);
      setSuccess(null);
      const result = await apiClient.resetOrgUserPassword(organizationId, userId);
      setTempPasswordData(result);
      setShowPasswordModal(true);
    } catch (err) {
      console.error('Failed to reset password:', err);
      setError(err instanceof Error ? err.message : 'Failed to reset password');
    } finally {
      setProcessingUserId(null);
    }
  };

  const filteredUsers = users.filter((user) => {
    const matchesSearch =
      searchQuery === '' ||
      user.username.toLowerCase().includes(searchQuery.toLowerCase()) ||
      user.email.toLowerCase().includes(searchQuery.toLowerCase()) ||
      `${user.firstName} ${user.lastName}`.toLowerCase().includes(searchQuery.toLowerCase());
    const matchesStatus = statusFilter === 'ALL' || user.status === statusFilter;
    const matchesRole = roleFilter === 'ALL' || user.role === roleFilter;
    return matchesSearch && matchesStatus && matchesRole;
  });

  const filteredAllUsers = allUsers.filter((user) =>
    assignSearchQuery === '' ||
    user.username.toLowerCase().includes(assignSearchQuery.toLowerCase()) ||
    user.email.toLowerCase().includes(assignSearchQuery.toLowerCase()) ||
    `${user.firstName} ${user.lastName}`.toLowerCase().includes(assignSearchQuery.toLowerCase())
  );

  const statusBadge = (status: string) => {
    const styles: Record<string, string> = {
      ACTIVE: 'bg-green-100 text-green-800 dark:bg-green-900 dark:text-green-200',
      LOCKED: 'bg-yellow-100 text-yellow-800 dark:bg-yellow-900 dark:text-yellow-200',
      DEACTIVATED: 'bg-red-100 text-red-800 dark:bg-red-900 dark:text-red-200',
    };
    return (
      <span className={`px-2 py-0.5 text-xs font-medium rounded-full ${styles[status] || 'bg-gray-100 text-gray-800'}`}>
        {status}
      </span>
    );
  };

  const Tooltip = ({ children, text }: { children: React.ReactNode; text: string }) => (
    <div className="relative group/tooltip">
      {children}
      <div className="absolute bottom-full left-1/2 -translate-x-1/2 mb-2 px-2 py-1 text-xs font-medium text-white bg-gray-900 dark:bg-gray-700 rounded whitespace-nowrap opacity-0 group-hover/tooltip:opacity-100 transition-opacity pointer-events-none z-10">
        {text}
        <div className="absolute top-full left-1/2 -translate-x-1/2 -mt-1 border-4 border-transparent border-t-gray-900 dark:border-t-gray-700" />
      </div>
    </div>
  );

  if (loading) {
    return (
      <div className="min-h-screen bg-gray-50 dark:bg-gray-900 flex items-center justify-center">
        <Loader2 className="h-8 w-8 animate-spin text-blue-500" />
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gray-50 dark:bg-gray-900 py-8 px-4 sm:px-6 lg:px-8">
      <div className="max-w-6xl mx-auto">
        {/* Back link */}
        <button onClick={() => router.push('/org-admin')} className="flex items-center text-gray-600 dark:text-gray-400 hover:text-gray-900 dark:hover:text-white mb-6">
          <ChevronLeft className="h-4 w-4 mr-1" />
          Back to Dashboard
        </button>

        <h1 className="text-3xl font-bold text-gray-900 dark:text-white mb-2">Manage Users</h1>
        <p className="text-gray-600 dark:text-gray-400 mb-8">View and manage organization members</p>

        {/* Alerts */}
        {error && (
          <div className="bg-red-50 dark:bg-red-900/20 border border-red-200 dark:border-red-800 rounded-lg p-4 mb-4 text-red-700 dark:text-red-300">
            {error}
          </div>
        )}
        {success && (
          <div className="bg-green-50 dark:bg-green-900/20 border border-green-200 dark:border-green-800 rounded-lg p-4 mb-4 text-green-700 dark:text-green-300">
            {success}
          </div>
        )}

        {/* Tabs */}
        <div className="border-b border-gray-200 dark:border-gray-700 mb-6">
          <nav className="flex space-x-8">
            <button
              onClick={() => handleTabChange('org-users')}
              className={`py-4 px-1 border-b-2 font-medium text-sm ${
                activeTab === 'org-users'
                  ? 'border-blue-500 text-blue-600 dark:text-blue-400'
                  : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300 dark:text-gray-400 dark:hover:text-gray-300'
              }`}
            >
              Organization Users
            </button>
            {isSuperAdmin && (
              <button
                onClick={() => handleTabChange('assign-users')}
                className={`py-4 px-1 border-b-2 font-medium text-sm ${
                  activeTab === 'assign-users'
                    ? 'border-blue-500 text-blue-600 dark:text-blue-400'
                    : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300 dark:text-gray-400 dark:hover:text-gray-300'
                }`}
              >
                Assign Users to Org
              </button>
            )}
          </nav>
        </div>

        {/* Tab: Org Users */}
        {activeTab === 'org-users' && (
          <>
            {/* Filters */}
            <div className="flex flex-col sm:flex-row gap-4 mb-6">
              <div className="relative flex-1">
                <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 h-4 w-4 text-gray-400" />
                <input
                  type="text"
                  placeholder="Search by name, email, or username..."
                  value={searchQuery}
                  onChange={(e) => setSearchQuery(e.target.value)}
                  className="w-full pl-10 pr-4 py-2 border border-gray-300 dark:border-gray-700 rounded-md bg-white dark:bg-gray-800 text-gray-900 dark:text-white focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
                />
              </div>
              <select
                value={statusFilter}
                onChange={(e) => setStatusFilter(e.target.value)}
                className="px-4 py-2 border border-gray-300 dark:border-gray-700 rounded-md bg-white dark:bg-gray-800 text-gray-900 dark:text-white"
              >
                <option value="ALL">All Statuses</option>
                <option value="ACTIVE">Active</option>
                <option value="LOCKED">Locked</option>
                <option value="DEACTIVATED">Deactivated</option>
              </select>
              <select
                value={roleFilter}
                onChange={(e) => setRoleFilter(e.target.value)}
                className="px-4 py-2 border border-gray-300 dark:border-gray-700 rounded-md bg-white dark:bg-gray-800 text-gray-900 dark:text-white"
              >
                <option value="ALL">All Roles</option>
                <option value="ORG_ADMIN">Admin</option>
                <option value="USER">User</option>
              </select>
            </div>

            {/* Users Table */}
            {filteredUsers.length === 0 ? (
              <div className="bg-white dark:bg-gray-800 rounded-lg shadow p-12 text-center">
                <p className="text-gray-500 dark:text-gray-400">No users found matching your filters.</p>
              </div>
            ) : (
              <div className="bg-white dark:bg-gray-800 rounded-lg shadow overflow-hidden">
                <div className="overflow-x-auto">
                  <table className="min-w-full divide-y divide-gray-200 dark:divide-gray-700">
                    <thead className="bg-gray-50 dark:bg-gray-700">
                      <tr>
                        <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-300 uppercase tracking-wider">Name</th>
                        <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-300 uppercase tracking-wider">Email</th>
                        <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-300 uppercase tracking-wider">Role</th>
                        <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-300 uppercase tracking-wider">Status</th>
                        <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-300 uppercase tracking-wider">Joined</th>
                        <th className="px-6 py-3 text-right text-xs font-medium text-gray-500 dark:text-gray-300 uppercase tracking-wider">Actions</th>
                      </tr>
                    </thead>
                    <tbody className="bg-white dark:bg-gray-800 divide-y divide-gray-200 dark:divide-gray-700">
                      {filteredUsers.map((user) => (
                        <tr key={user.userId} className="hover:bg-gray-50 dark:hover:bg-gray-700">
                          <td className="px-6 py-4 whitespace-nowrap">
                            <div className="text-sm font-medium text-gray-900 dark:text-white">
                              {user.firstName && user.lastName ? `${user.firstName} ${user.lastName}` : user.username}
                            </div>
                            <div className="text-sm text-gray-500 dark:text-gray-400">{user.username}</div>
                          </td>
                          <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500 dark:text-gray-400">{user.email}</td>
                          <td className="px-6 py-4 whitespace-nowrap text-sm">
                            <span className={user.role === 'ORG_ADMIN' ? 'text-blue-600 dark:text-blue-400 font-medium' : 'text-gray-500 dark:text-gray-400'}>
                              {user.role === 'ORG_ADMIN' ? 'Admin' : 'User'}
                            </span>
                          </td>
                          <td className="px-6 py-4 whitespace-nowrap">{statusBadge(user.status)}</td>
                          <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500 dark:text-gray-400">
                            {user.joinedAt ? new Date(user.joinedAt).toLocaleDateString() : '-'}
                          </td>
                          <td className="px-6 py-4 whitespace-nowrap text-right">
                            <div className="flex items-center justify-end gap-1">
                              {user.status === 'ACTIVE' && (
                                <Tooltip text="Lock user account">
                                  <button
                                    onClick={() => handleAction(user.userId, 'lock')}
                                    disabled={processingUserId === user.userId}
                                    className="p-1.5 text-yellow-600 hover:bg-yellow-50 dark:hover:bg-yellow-900/20 rounded-md"
                                  >
                                    <Lock className="h-4 w-4" />
                                  </button>
                                </Tooltip>
                              )}
                              {user.status === 'LOCKED' && (
                                <Tooltip text="Unlock user account">
                                  <button
                                    onClick={() => handleAction(user.userId, 'unlock')}
                                    disabled={processingUserId === user.userId}
                                    className="p-1.5 text-green-600 hover:bg-green-50 dark:hover:bg-green-900/20 rounded-md"
                                  >
                                    <Unlock className="h-4 w-4" />
                                  </button>
                                </Tooltip>
                              )}
                              {(user.status === 'ACTIVE' || user.status === 'LOCKED') && (
                                <Tooltip text="Deactivate user">
                                  <button
                                    onClick={() => handleAction(user.userId, 'deactivate')}
                                    disabled={processingUserId === user.userId}
                                    className="p-1.5 text-red-600 hover:bg-red-50 dark:hover:bg-red-900/20 rounded-md"
                                  >
                                    <UserX className="h-4 w-4" />
                                  </button>
                                </Tooltip>
                              )}
                              {user.status === 'DEACTIVATED' && (
                                <Tooltip text="Reactivate user">
                                  <button
                                    onClick={() => handleAction(user.userId, 'reactivate')}
                                    disabled={processingUserId === user.userId}
                                    className="p-1.5 text-green-600 hover:bg-green-50 dark:hover:bg-green-900/20 rounded-md"
                                  >
                                    <UserCheck className="h-4 w-4" />
                                  </button>
                                </Tooltip>
                              )}
                              <Tooltip text="Reset password">
                                <button
                                  onClick={() => handleResetPassword(user.userId)}
                                  disabled={processingUserId === user.userId}
                                  className="p-1.5 text-blue-600 hover:bg-blue-50 dark:hover:bg-blue-900/20 rounded-md"
                                >
                                  <KeyRound className="h-4 w-4" />
                                </button>
                              </Tooltip>
                              {processingUserId === user.userId && (
                                <Loader2 className="h-4 w-4 animate-spin text-gray-400" />
                              )}
                            </div>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              </div>
            )}
          </>
        )}

        {/* Tab: Assign Users (Super Admin only) */}
        {activeTab === 'assign-users' && isSuperAdmin && (
          <div className="space-y-6">
            <div className="bg-white dark:bg-gray-800 rounded-lg shadow-md border border-gray-200 dark:border-gray-700 p-6">
              <h2 className="text-lg font-semibold text-gray-900 dark:text-white mb-1">Assign User to Organization</h2>
              <p className="text-sm text-gray-500 dark:text-gray-400 mb-6">Manually add an existing user to an organization without requiring an access request.</p>

              {assignLoading && allUsers.length === 0 ? (
                <div className="flex items-center justify-center py-8">
                  <Loader2 className="h-6 w-6 animate-spin text-blue-500" />
                </div>
              ) : (
                <div className="space-y-4">
                  {/* User selection */}
                  <div>
                    <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">Select User</label>
                    <div className="relative mb-2">
                      <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 h-4 w-4 text-gray-400" />
                      <input
                        type="text"
                        placeholder="Search users..."
                        value={assignSearchQuery}
                        onChange={(e) => setAssignSearchQuery(e.target.value)}
                        className="w-full pl-10 pr-4 py-2 border border-gray-300 dark:border-gray-700 rounded-md bg-white dark:bg-gray-800 text-gray-900 dark:text-white focus:ring-2 focus:ring-blue-500 text-sm"
                      />
                    </div>
                    <select
                      value={assignUserId ?? ''}
                      onChange={(e) => setAssignUserId(e.target.value ? Number(e.target.value) : null)}
                      className="w-full px-4 py-2 border border-gray-300 dark:border-gray-700 rounded-md bg-white dark:bg-gray-800 text-gray-900 dark:text-white"
                      size={5}
                    >
                      {filteredAllUsers.map((u) => (
                        <option key={u.id} value={u.id}>
                          {u.username} — {u.email} {u.firstName ? `(${u.firstName} ${u.lastName})` : ''}
                        </option>
                      ))}
                    </select>
                  </div>

                  {/* Organization selection */}
                  <div>
                    <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">Select Organization</label>
                    <select
                      value={assignOrgId ?? ''}
                      onChange={(e) => setAssignOrgId(e.target.value ? Number(e.target.value) : null)}
                      className="w-full px-4 py-2 border border-gray-300 dark:border-gray-700 rounded-md bg-white dark:bg-gray-800 text-gray-900 dark:text-white"
                    >
                      <option value="">Choose an organization...</option>
                      {allOrgs.map((o) => (
                        <option key={o.id} value={o.id}>{o.name}</option>
                      ))}
                    </select>
                  </div>

                  {/* Role selection */}
                  <div>
                    <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">Role</label>
                    <select
                      value={assignRole}
                      onChange={(e) => setAssignRole(e.target.value)}
                      className="w-full px-4 py-2 border border-gray-300 dark:border-gray-700 rounded-md bg-white dark:bg-gray-800 text-gray-900 dark:text-white"
                    >
                      <option value="USER">User</option>
                      <option value="ORG_ADMIN">Organization Admin</option>
                    </select>
                  </div>

                  {/* Assign button */}
                  <button
                    onClick={handleAssignUser}
                    disabled={!assignUserId || !assignOrgId || assignLoading}
                    className="flex items-center gap-2 px-6 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700 disabled:opacity-50 disabled:cursor-not-allowed"
                  >
                    {assignLoading ? (
                      <Loader2 className="h-4 w-4 animate-spin" />
                    ) : (
                      <UserPlus className="h-4 w-4" />
                    )}
                    Assign User
                  </button>
                </div>
              )}
            </div>
          </div>
        )}

        {/* Temp Password Modal */}
        {showPasswordModal && tempPasswordData && (
          <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
            <div className="bg-white dark:bg-gray-800 rounded-lg shadow-xl max-w-md w-full mx-4 p-6">
              <h3 className="text-lg font-semibold text-gray-900 dark:text-white mb-4">Password Reset</h3>
              <p className="text-sm text-gray-600 dark:text-gray-400 mb-4">
                A temporary password has been generated for <strong>{tempPasswordData.username}</strong> ({tempPasswordData.email}).
              </p>
              <div className="bg-gray-100 dark:bg-gray-700 rounded-md p-3 mb-4">
                <p className="text-sm text-gray-500 dark:text-gray-400 mb-1">Temporary Password:</p>
                <p className="text-lg font-mono font-bold text-gray-900 dark:text-white select-all">{tempPasswordData.tempPassword}</p>
              </div>
              <p className="text-xs text-gray-500 dark:text-gray-400 mb-4">
                The user will be required to change this password on their next login.
              </p>
              <button
                onClick={() => { setShowPasswordModal(false); setTempPasswordData(null); }}
                className="w-full px-4 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700"
              >
                Done
              </button>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
