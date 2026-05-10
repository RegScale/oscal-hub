'use client';

import { useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import { ChevronLeft, Loader2, Users, LogIn, Activity, ShieldAlert, RefreshCw } from 'lucide-react';
import {
  BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer,
  PieChart, Pie, Cell
} from 'recharts';
import { apiClient } from '@/lib/api-client';
import { HelpButton } from '@/components/HelpButton';

interface OrgAnalytics {
  activeUsersLast7Days: number;
  totalLoginsLast30Days: number;
  totalOperationsLast30Days: number;
  failedLoginsLast30Days: number;
  loginsPerDay: Array<{ date: string; count: number }>;
  operationsByType: Array<{ name: string; count: number }>;
  topUsers: Array<{ username: string; operationCount: number }>;
}

const COLORS = ['#3B82F6', '#10B981', '#F59E0B', '#EF4444', '#8B5CF6', '#06B6D4', '#EC4899', '#F97316'];

export default function OrgAdminAnalyticsPage() {
  const router = useRouter();
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [analytics, setAnalytics] = useState<OrgAnalytics | null>(null);
  const [organizationId, setOrganizationId] = useState<number | null>(null);

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
        fetchAnalytics(orgId, false);
      } else {
        setError('No organization selected');
        setLoading(false);
      }
    } catch {
      setError('Failed to load user data. Please try logging in again.');
      setLoading(false);
    }
  }, [router]);

  const fetchAnalytics = async (orgId: number, showRefreshIndicator: boolean) => {
    try {
      if (showRefreshIndicator) {
        setRefreshing(true);
      }
      setError(null);
      const data = await apiClient.getOrgAnalytics(orgId);
      setAnalytics(data);
    } catch (err) {
      console.error('Failed to fetch analytics:', err);
      setError('Failed to load analytics data. Please try again.');
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  };

  const formatDate = (dateStr: unknown) => {
    if (typeof dateStr !== 'string' && typeof dateStr !== 'number') return '';
    const date = new Date(dateStr);
    return `${date.getMonth() + 1}/${date.getDate()}`;
  };

  if (loading) {
    return (
      <div className="min-h-screen bg-gray-50 dark:bg-gray-900 flex items-center justify-center">
        <Loader2 className="h-8 w-8 animate-spin text-blue-500" />
      </div>
    );
  }

  const statCards = [
    { label: 'Active Users (7d)', value: analytics?.activeUsersLast7Days ?? 0, icon: Users, color: 'blue' },
    { label: 'Total Logins (30d)', value: analytics?.totalLoginsLast30Days ?? 0, icon: LogIn, color: 'green' },
    { label: 'Operations (30d)', value: analytics?.totalOperationsLast30Days ?? 0, icon: Activity, color: 'purple' },
    { label: 'Failed Logins (30d)', value: analytics?.failedLoginsLast30Days ?? 0, icon: ShieldAlert, color: 'red' },
  ];

  const colorMap: Record<string, string> = {
    blue: 'bg-blue-100 dark:bg-blue-900/30 text-blue-600 dark:text-blue-400',
    green: 'bg-green-100 dark:bg-green-900/30 text-green-600 dark:text-green-400',
    purple: 'bg-purple-100 dark:bg-purple-900/30 text-purple-600 dark:text-purple-400',
    red: 'bg-red-100 dark:bg-red-900/30 text-red-600 dark:text-red-400',
  };

  return (
    <div className="min-h-screen bg-gray-50 dark:bg-gray-900 py-8 px-4 sm:px-6 lg:px-8">
      <div className="max-w-6xl mx-auto">
        {/* Header */}
        <div className="flex items-center justify-between mb-8">
          <div>
            <button onClick={() => router.push('/org-admin')} className="flex items-center text-gray-600 dark:text-gray-400 hover:text-gray-900 dark:hover:text-white mb-2">
              <ChevronLeft className="h-4 w-4 mr-1" />
              Back to Dashboard
            </button>
            <div className="flex items-center gap-2">
              <h1 className="text-3xl font-bold text-gray-900 dark:text-white">Usage Analytics</h1>
              <HelpButton slug="org-admin-analytics" />
            </div>
            <p className="text-gray-600 dark:text-gray-400">Organization usage metrics for the last 30 days</p>
          </div>
          <button
            onClick={() => organizationId && fetchAnalytics(organizationId, true)}
            disabled={refreshing}
            className="flex items-center gap-2 px-4 py-2 bg-white dark:bg-gray-800 border border-gray-300 dark:border-gray-700 rounded-md text-gray-700 dark:text-gray-300 hover:bg-gray-50 dark:hover:bg-gray-700"
          >
            <RefreshCw className={`h-4 w-4 ${refreshing ? 'animate-spin' : ''}`} />
            Refresh
          </button>
        </div>

        {error && (
          <div className="bg-red-50 dark:bg-red-900/20 border border-red-200 dark:border-red-800 rounded-lg p-4 mb-6 text-red-700 dark:text-red-300">
            {error}
          </div>
        )}

        {/* Stat Cards */}
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6 mb-8">
          {statCards.map((card) => {
            const Icon = card.icon;
            return (
              <div key={card.label} className="bg-white dark:bg-gray-800 rounded-lg shadow p-6 border border-gray-200 dark:border-gray-700">
                <div className="flex items-center justify-between">
                  <div>
                    <p className="text-sm text-gray-500 dark:text-gray-400">{card.label}</p>
                    <p className="text-2xl font-bold text-gray-900 dark:text-white">{card.value}</p>
                  </div>
                  <div className={`p-3 rounded-lg ${colorMap[card.color]}`}>
                    <Icon className="h-6 w-6" />
                  </div>
                </div>
              </div>
            );
          })}
        </div>

        {/* Charts Row */}
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-6 mb-8">
          {/* Login Activity */}
          <div className="bg-white dark:bg-gray-800 rounded-lg shadow p-6 border border-gray-200 dark:border-gray-700">
            <h2 className="text-lg font-semibold text-gray-900 dark:text-white mb-4">Login Activity</h2>
            <div className="h-64">
              {analytics?.loginsPerDay && analytics.loginsPerDay.length > 0 ? (
                <ResponsiveContainer width="100%" height={250}>
                  <BarChart data={analytics.loginsPerDay}>
                    <CartesianGrid strokeDasharray="3 3" stroke="#374151" opacity={0.3} />
                    <XAxis dataKey="date" tickFormatter={formatDate} stroke="#9CA3AF" fontSize={12} />
                    <YAxis stroke="#9CA3AF" fontSize={12} />
                    <Tooltip
                      contentStyle={{ backgroundColor: '#1F2937', border: 'none', borderRadius: '8px', color: '#F9FAFB' }}
                      labelFormatter={(label) => new Date(label).toLocaleDateString()}
                    />
                    <Bar dataKey="count" fill="#3B82F6" radius={[4, 4, 0, 0]} />
                  </BarChart>
                </ResponsiveContainer>
              ) : (
                <div className="h-full flex items-center justify-center text-gray-400">No login data available</div>
              )}
            </div>
          </div>

          {/* Operations by Type */}
          <div className="bg-white dark:bg-gray-800 rounded-lg shadow p-6 border border-gray-200 dark:border-gray-700">
            <h2 className="text-lg font-semibold text-gray-900 dark:text-white mb-4">Operations by Type</h2>
            <div className="h-64">
              {analytics?.operationsByType && analytics.operationsByType.length > 0 ? (
                <ResponsiveContainer width="100%" height={250}>
                  <PieChart>
                    <Pie
                      data={analytics.operationsByType}
                      cx="50%"
                      cy="50%"
                      outerRadius={90}
                      dataKey="count"
                      nameKey="name"
                      label={(props: any) => `${props.name} ${(Number(props.percent) * 100).toFixed(0)}%`}
                      labelLine={true}
                    >
                      {analytics.operationsByType.map((_, index) => (
                        <Cell key={`cell-${index}`} fill={COLORS[index % COLORS.length]} />
                      ))}
                    </Pie>
                    <Tooltip
                      contentStyle={{ backgroundColor: '#1F2937', border: 'none', borderRadius: '8px', color: '#F9FAFB' }}
                    />
                  </PieChart>
                </ResponsiveContainer>
              ) : (
                <div className="h-full flex items-center justify-center text-gray-400">No operation data available</div>
              )}
            </div>
          </div>
        </div>

        {/* Top Users Table */}
        <div className="bg-white dark:bg-gray-800 rounded-lg shadow border border-gray-200 dark:border-gray-700">
          <div className="px-6 py-4 border-b border-gray-200 dark:border-gray-700">
            <h2 className="text-lg font-semibold text-gray-900 dark:text-white">Most Active Users</h2>
          </div>
          {analytics?.topUsers && analytics.topUsers.length > 0 ? (
            <div className="overflow-x-auto">
              <table className="min-w-full divide-y divide-gray-200 dark:divide-gray-700">
                <thead className="bg-gray-50 dark:bg-gray-700">
                  <tr>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-300 uppercase tracking-wider">Rank</th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-300 uppercase tracking-wider">Username</th>
                    <th className="px-6 py-3 text-right text-xs font-medium text-gray-500 dark:text-gray-300 uppercase tracking-wider">Operations</th>
                  </tr>
                </thead>
                <tbody className="bg-white dark:bg-gray-800 divide-y divide-gray-200 dark:divide-gray-700">
                  {analytics.topUsers.map((user, index) => (
                    <tr key={user.username} className="hover:bg-gray-50 dark:hover:bg-gray-700">
                      <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500 dark:text-gray-400">#{index + 1}</td>
                      <td className="px-6 py-4 whitespace-nowrap text-sm font-medium text-gray-900 dark:text-white">{user.username}</td>
                      <td className="px-6 py-4 whitespace-nowrap text-sm text-right text-gray-900 dark:text-white">{user.operationCount}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          ) : (
            <div className="p-12 text-center text-gray-400">No activity data available</div>
          )}
        </div>
      </div>
    </div>
  );
}
