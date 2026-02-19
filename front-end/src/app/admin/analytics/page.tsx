'use client';

import { useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import {
  BarChart3,
  Users,
  Building2,
  Activity,
  TrendingUp,
  FileText,
  ChevronLeft,
  RefreshCw,
  CheckCircle,
  XCircle
} from 'lucide-react';
import {
  LineChart,
  Line,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
  PieChart,
  Pie,
  Cell,
  BarChart,
  Bar,
  Legend
} from 'recharts';
import { apiClient } from '@/lib/api-client';

interface AnalyticsData {
  totalUsers: number;
  totalOrganizations: number;
  totalOperationsThisMonth: number;
  activeUsersLast7Days: number;
  newUsersThisMonth: number;
  pendingAccessRequests: number;
  activityOverTime: Array<{ date: string; count: number }>;
  operationsByType: Array<{ name: string; count: number }>;
  contentByType: Array<{ name: string; count: number }>;
  formatPreferences: Array<{ name: string; count: number }>;
  mostActiveUsers: Array<{
    username: string;
    organizationName: string;
    operationsCount: number;
    lastActive: string;
  }>;
  mostActiveOrganizations: Array<{
    id: number;
    name: string;
    memberCount: number;
    totalOperations: number;
    lastActive: string;
  }>;
  newestOrganizations: Array<{
    id: number;
    name: string;
    createdAt: string;
    memberCount: number;
    createdBy: string;
  }>;
  userGrowthTrend: Array<{ date: string; count: number }>;
  recentActivity: Array<{
    timestamp: string;
    username: string;
    operationType: string;
    documentType: string;
    status: string;
    resource: string;
  }>;
}

const COLORS = ['#3B82F6', '#10B981', '#F59E0B', '#EF4444', '#8B5CF6', '#EC4899', '#06B6D4', '#84CC16'];

export default function AnalyticsDashboardPage() {
  const router = useRouter();
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [analytics, setAnalytics] = useState<AnalyticsData | null>(null);

  const fetchAnalytics = async (showRefreshIndicator = false) => {
    try {
      if (showRefreshIndicator) {
        setRefreshing(true);
      }
      setError(null);
      const data = await apiClient.getAnalytics();
      setAnalytics(data);
    } catch (err) {
      console.error('Failed to fetch analytics:', err);
      setError('Failed to load analytics data. Please try again.');
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  };

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
    fetchAnalytics();
  }, [router]);

  const formatDate = (dateString: string) => {
    const date = new Date(dateString);
    return date.toLocaleDateString('en-US', { month: 'short', day: 'numeric' });
  };

  const formatDateTime = (dateString: string) => {
    const date = new Date(dateString);
    return date.toLocaleString('en-US', {
      month: 'short',
      day: 'numeric',
      hour: 'numeric',
      minute: '2-digit',
      hour12: true
    });
  };

  if (loading) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-gray-50 dark:bg-gray-900">
        <div className="text-center">
          <div className="animate-spin rounded-full h-16 w-16 border-b-2 border-emerald-600 mx-auto"></div>
          <p className="mt-4 text-gray-600 dark:text-gray-400">Loading analytics...</p>
        </div>
      </div>
    );
  }

  if (error && !analytics) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-gray-50 dark:bg-gray-900">
        <div className="text-center">
          <p className="text-red-600 dark:text-red-400 mb-4">{error}</p>
          <button
            onClick={() => fetchAnalytics()}
            className="px-4 py-2 bg-emerald-600 text-white rounded-lg hover:bg-emerald-700 transition-colors"
          >
            Retry
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gray-50 dark:bg-gray-900 py-8 px-4 sm:px-6 lg:px-8">
      <div className="max-w-7xl mx-auto">
        {/* Header */}
        <div className="mb-8">
          <button
            onClick={() => router.push('/admin')}
            className="flex items-center text-gray-600 dark:text-gray-400 hover:text-gray-900 dark:hover:text-white mb-4 transition-colors"
          >
            <ChevronLeft className="w-5 h-5 mr-1" />
            Back to Admin Dashboard
          </button>
          <div className="flex items-center justify-between">
            <div>
              <h1 className="text-3xl font-bold text-gray-900 dark:text-white flex items-center gap-3">
                <BarChart3 className="w-8 h-8 text-emerald-600" />
                Platform Analytics
              </h1>
              <p className="mt-2 text-gray-600 dark:text-gray-400">
                Monitor platform usage, user activity, and trends across all organizations
              </p>
            </div>
            <button
              onClick={() => fetchAnalytics(true)}
              disabled={refreshing}
              className="flex items-center gap-2 px-4 py-2 bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-700 rounded-lg hover:bg-gray-50 dark:hover:bg-gray-700 transition-colors disabled:opacity-50"
            >
              <RefreshCw className={`w-4 h-4 ${refreshing ? 'animate-spin' : ''}`} />
              Refresh
            </button>
          </div>
        </div>

        {analytics && (
          <>
            {/* Summary Stats Cards */}
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6 mb-8">
              <div className="bg-white dark:bg-gray-800 rounded-lg shadow p-6 border border-gray-200 dark:border-gray-700">
                <div className="flex items-center justify-between">
                  <div>
                    <p className="text-sm font-medium text-gray-500 dark:text-gray-400">Total Users</p>
                    <p className="text-3xl font-bold text-gray-900 dark:text-white">{analytics.totalUsers ?? 0}</p>
                    <p className="text-sm text-emerald-600 dark:text-emerald-400 mt-1">
                      +{analytics.newUsersThisMonth ?? 0} this month
                    </p>
                  </div>
                  <div className="p-3 bg-blue-100 dark:bg-blue-900/30 rounded-lg">
                    <Users className="w-6 h-6 text-blue-600 dark:text-blue-400" />
                  </div>
                </div>
              </div>

              <div className="bg-white dark:bg-gray-800 rounded-lg shadow p-6 border border-gray-200 dark:border-gray-700">
                <div className="flex items-center justify-between">
                  <div>
                    <p className="text-sm font-medium text-gray-500 dark:text-gray-400">Organizations</p>
                    <p className="text-3xl font-bold text-gray-900 dark:text-white">{analytics.totalOrganizations ?? 0}</p>
                  </div>
                  <div className="p-3 bg-purple-100 dark:bg-purple-900/30 rounded-lg">
                    <Building2 className="w-6 h-6 text-purple-600 dark:text-purple-400" />
                  </div>
                </div>
              </div>

              <div className="bg-white dark:bg-gray-800 rounded-lg shadow p-6 border border-gray-200 dark:border-gray-700">
                <div className="flex items-center justify-between">
                  <div>
                    <p className="text-sm font-medium text-gray-500 dark:text-gray-400">Operations (This Month)</p>
                    <p className="text-3xl font-bold text-gray-900 dark:text-white">{(analytics.totalOperationsThisMonth ?? 0).toLocaleString()}</p>
                  </div>
                  <div className="p-3 bg-emerald-100 dark:bg-emerald-900/30 rounded-lg">
                    <FileText className="w-6 h-6 text-emerald-600 dark:text-emerald-400" />
                  </div>
                </div>
              </div>

              <div className="bg-white dark:bg-gray-800 rounded-lg shadow p-6 border border-gray-200 dark:border-gray-700">
                <div className="flex items-center justify-between">
                  <div>
                    <p className="text-sm font-medium text-gray-500 dark:text-gray-400">Active Users (7d)</p>
                    <p className="text-3xl font-bold text-gray-900 dark:text-white">{analytics.activeUsersLast7Days ?? 0}</p>
                  </div>
                  <div className="p-3 bg-orange-100 dark:bg-orange-900/30 rounded-lg">
                    <Activity className="w-6 h-6 text-orange-600 dark:text-orange-400" />
                  </div>
                </div>
              </div>
            </div>

            {/* Charts Row 1 */}
            <div className="grid grid-cols-1 lg:grid-cols-2 gap-6 mb-8">
              {/* Activity Over Time */}
              <div className="bg-white dark:bg-gray-800 rounded-lg shadow p-6 border border-gray-200 dark:border-gray-700">
                <h3 className="text-lg font-semibold text-gray-900 dark:text-white mb-4 flex items-center gap-2">
                  <TrendingUp className="w-5 h-5 text-emerald-600" />
                  Activity Over Time (Last 30 Days)
                </h3>
                <div className="h-64">
                  {(analytics.activityOverTime?.length ?? 0) > 0 ? (
                    <ResponsiveContainer width="100%" height={250}>
                      <LineChart data={analytics.activityOverTime}>
                        <CartesianGrid strokeDasharray="3 3" stroke="#374151" opacity={0.3} />
                        <XAxis
                          dataKey="date"
                          tickFormatter={formatDate}
                          stroke="#9CA3AF"
                          tick={{ fontSize: 12 }}
                        />
                        <YAxis stroke="#9CA3AF" tick={{ fontSize: 12 }} />
                        <Tooltip
                          labelFormatter={formatDate}
                          contentStyle={{
                            backgroundColor: '#1F2937',
                            border: '1px solid #374151',
                            borderRadius: '8px',
                            color: '#F9FAFB'
                          }}
                        />
                        <Line
                          type="monotone"
                          dataKey="count"
                          stroke="#10B981"
                          strokeWidth={2}
                          dot={false}
                          activeDot={{ r: 6 }}
                        />
                      </LineChart>
                    </ResponsiveContainer>
                  ) : (
                    <div className="h-full flex items-center justify-center text-gray-500 dark:text-gray-400">
                      No activity data available
                    </div>
                  )}
                </div>
              </div>

              {/* User Growth Trend */}
              <div className="bg-white dark:bg-gray-800 rounded-lg shadow p-6 border border-gray-200 dark:border-gray-700">
                <h3 className="text-lg font-semibold text-gray-900 dark:text-white mb-4 flex items-center gap-2">
                  <Users className="w-5 h-5 text-blue-600" />
                  User Growth (Last 30 Days)
                </h3>
                <div className="h-64">
                  {(analytics.userGrowthTrend?.length ?? 0) > 0 ? (
                    <ResponsiveContainer width="100%" height={250}>
                      <BarChart data={analytics.userGrowthTrend}>
                        <CartesianGrid strokeDasharray="3 3" stroke="#374151" opacity={0.3} />
                        <XAxis
                          dataKey="date"
                          tickFormatter={formatDate}
                          stroke="#9CA3AF"
                          tick={{ fontSize: 12 }}
                        />
                        <YAxis stroke="#9CA3AF" tick={{ fontSize: 12 }} />
                        <Tooltip
                          labelFormatter={formatDate}
                          contentStyle={{
                            backgroundColor: '#1F2937',
                            border: '1px solid #374151',
                            borderRadius: '8px',
                            color: '#F9FAFB'
                          }}
                        />
                        <Bar dataKey="count" fill="#3B82F6" radius={[4, 4, 0, 0]} />
                      </BarChart>
                    </ResponsiveContainer>
                  ) : (
                    <div className="h-full flex items-center justify-center text-gray-500 dark:text-gray-400">
                      No user growth data available
                    </div>
                  )}
                </div>
              </div>
            </div>

            {/* Charts Row 2 - Pie Charts */}
            <div className="grid grid-cols-1 lg:grid-cols-2 gap-6 mb-8">
              {/* Operations by Model Type */}
              <div className="bg-white dark:bg-gray-800 rounded-lg shadow p-6 border border-gray-200 dark:border-gray-700">
                <h3 className="text-lg font-semibold text-gray-900 dark:text-white mb-4">
                  Operations by Model Type
                </h3>
                <div className="h-64">
                  {(analytics.operationsByType?.length ?? 0) > 0 ? (
                    <ResponsiveContainer width="100%" height={250}>
                      <PieChart>
                        <Pie
                          data={analytics.operationsByType}
                          cx="50%"
                          cy="50%"
                          innerRadius={50}
                          outerRadius={80}
                          paddingAngle={2}
                          dataKey="count"
                          nameKey="name"
                          label={(props: Record<string, unknown>) => `${props.name || ''} (${((Number(props.percent) || 0) * 100).toFixed(0)}%)`}
                          labelLine={false}
                        >
                          {(analytics.operationsByType ?? []).map((_, index) => (
                            <Cell key={`cell-${index}`} fill={COLORS[index % COLORS.length]} />
                          ))}
                        </Pie>
                        <Tooltip
                          contentStyle={{
                            backgroundColor: '#1F2937',
                            border: '1px solid #374151',
                            borderRadius: '8px',
                            color: '#F9FAFB'
                          }}
                        />
                      </PieChart>
                    </ResponsiveContainer>
                  ) : (
                    <div className="h-full flex items-center justify-center text-gray-500 dark:text-gray-400">
                      No model type data available
                    </div>
                  )}
                </div>
              </div>

              {/* Operations by Format */}
              <div className="bg-white dark:bg-gray-800 rounded-lg shadow p-6 border border-gray-200 dark:border-gray-700">
                <h3 className="text-lg font-semibold text-gray-900 dark:text-white mb-4">
                  Operations by Format
                </h3>
                <div className="h-64">
                  {(analytics.formatPreferences?.length ?? 0) > 0 ? (
                    <ResponsiveContainer width="100%" height={250}>
                      <PieChart>
                        <Pie
                          data={analytics.formatPreferences}
                          cx="50%"
                          cy="50%"
                          innerRadius={50}
                          outerRadius={80}
                          paddingAngle={2}
                          dataKey="count"
                          nameKey="name"
                          label={(props: Record<string, unknown>) => `${props.name || ''} (${((Number(props.percent) || 0) * 100).toFixed(0)}%)`}
                          labelLine={false}
                        >
                          {(analytics.formatPreferences ?? []).map((_, index) => (
                            <Cell key={`cell-${index}`} fill={COLORS[index % COLORS.length]} />
                          ))}
                        </Pie>
                        <Tooltip
                          contentStyle={{
                            backgroundColor: '#1F2937',
                            border: '1px solid #374151',
                            borderRadius: '8px',
                            color: '#F9FAFB'
                          }}
                        />
                      </PieChart>
                    </ResponsiveContainer>
                  ) : (
                    <div className="h-full flex items-center justify-center text-gray-500 dark:text-gray-400">
                      No format data available
                    </div>
                  )}
                </div>
              </div>
            </div>

            {/* Tables Row */}
            <div className="grid grid-cols-1 lg:grid-cols-2 gap-6 mb-8">
              {/* Most Active Users */}
              <div className="bg-white dark:bg-gray-800 rounded-lg shadow border border-gray-200 dark:border-gray-700">
                <div className="p-6 border-b border-gray-200 dark:border-gray-700">
                  <h3 className="text-lg font-semibold text-gray-900 dark:text-white flex items-center gap-2">
                    <Users className="w-5 h-5 text-blue-600" />
                    Most Active Users
                  </h3>
                </div>
                <div className="overflow-x-auto">
                  <table className="w-full">
                    <thead className="bg-gray-50 dark:bg-gray-900/50">
                      <tr>
                        <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider">User</th>
                        <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider">Organization</th>
                        <th className="px-6 py-3 text-right text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider">Operations</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-gray-200 dark:divide-gray-700">
                      {(analytics.mostActiveUsers?.length ?? 0) > 0 ? (
                        analytics.mostActiveUsers.map((user, index) => (
                          <tr key={user.username} className="hover:bg-gray-50 dark:hover:bg-gray-900/30">
                            <td className="px-6 py-4 whitespace-nowrap">
                              <div className="flex items-center">
                                <div className="flex-shrink-0 w-8 h-8 rounded-full bg-blue-100 dark:bg-blue-900/30 flex items-center justify-center text-blue-600 dark:text-blue-400 font-semibold text-sm">
                                  {index + 1}
                                </div>
                                <div className="ml-3">
                                  <p className="text-sm font-medium text-gray-900 dark:text-white">{user.username}</p>
                                </div>
                              </div>
                            </td>
                            <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500 dark:text-gray-400">
                              {user.organizationName || '-'}
                            </td>
                            <td className="px-6 py-4 whitespace-nowrap text-sm text-right font-medium text-gray-900 dark:text-white">
                              {(user.operationsCount ?? 0).toLocaleString()}
                            </td>
                          </tr>
                        ))
                      ) : (
                        <tr>
                          <td colSpan={3} className="px-6 py-8 text-center text-gray-500 dark:text-gray-400">
                            No user activity data available
                          </td>
                        </tr>
                      )}
                    </tbody>
                  </table>
                </div>
              </div>

              {/* Organization Activity */}
              <div className="bg-white dark:bg-gray-800 rounded-lg shadow border border-gray-200 dark:border-gray-700">
                <div className="p-6 border-b border-gray-200 dark:border-gray-700">
                  <h3 className="text-lg font-semibold text-gray-900 dark:text-white flex items-center gap-2">
                    <Building2 className="w-5 h-5 text-purple-600" />
                    Most Active Organizations
                  </h3>
                </div>
                <div className="overflow-x-auto">
                  <table className="w-full">
                    <thead className="bg-gray-50 dark:bg-gray-900/50">
                      <tr>
                        <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider">Organization</th>
                        <th className="px-6 py-3 text-right text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider">Members</th>
                        <th className="px-6 py-3 text-right text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider">Operations</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-gray-200 dark:divide-gray-700">
                      {(analytics.mostActiveOrganizations?.length ?? 0) > 0 ? (
                        analytics.mostActiveOrganizations.map((org, index) => (
                          <tr
                            key={org.id}
                            className="hover:bg-gray-50 dark:hover:bg-gray-900/30 cursor-pointer"
                            onClick={() => router.push(`/admin/organizations/${org.id}`)}
                          >
                            <td className="px-6 py-4 whitespace-nowrap">
                              <div className="flex items-center">
                                <div className="flex-shrink-0 w-8 h-8 rounded-full bg-purple-100 dark:bg-purple-900/30 flex items-center justify-center text-purple-600 dark:text-purple-400 font-semibold text-sm">
                                  {index + 1}
                                </div>
                                <span className="ml-3 text-sm font-medium text-gray-900 dark:text-white">{org.name}</span>
                              </div>
                            </td>
                            <td className="px-6 py-4 whitespace-nowrap text-sm text-right text-gray-500 dark:text-gray-400">
                              {org.memberCount}
                            </td>
                            <td className="px-6 py-4 whitespace-nowrap text-sm text-right font-medium text-gray-900 dark:text-white">
                              {(org.totalOperations ?? 0).toLocaleString()}
                            </td>
                          </tr>
                        ))
                      ) : (
                        <tr>
                          <td colSpan={3} className="px-6 py-8 text-center text-gray-500 dark:text-gray-400">
                            No organization activity data available
                          </td>
                        </tr>
                      )}
                    </tbody>
                  </table>
                </div>
              </div>
            </div>

            {/* Bottom Row - Newest Orgs and Recent Activity */}
            <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
              {/* Newest Organizations */}
              <div className="bg-white dark:bg-gray-800 rounded-lg shadow border border-gray-200 dark:border-gray-700">
                <div className="p-6 border-b border-gray-200 dark:border-gray-700">
                  <h3 className="text-lg font-semibold text-gray-900 dark:text-white flex items-center gap-2">
                    <Building2 className="w-5 h-5 text-emerald-600" />
                    Newest Organizations
                  </h3>
                </div>
                <div className="overflow-x-auto">
                  <table className="w-full">
                    <thead className="bg-gray-50 dark:bg-gray-900/50">
                      <tr>
                        <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider">Organization</th>
                        <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider">Created</th>
                        <th className="px-6 py-3 text-right text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider">Members</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-gray-200 dark:divide-gray-700">
                      {(analytics.newestOrganizations?.length ?? 0) > 0 ? (
                        analytics.newestOrganizations.map((org) => (
                          <tr
                            key={org.id}
                            className="hover:bg-gray-50 dark:hover:bg-gray-900/30 cursor-pointer"
                            onClick={() => router.push(`/admin/organizations/${org.id}`)}
                          >
                            <td className="px-6 py-4 whitespace-nowrap">
                              <span className="text-sm font-medium text-gray-900 dark:text-white">{org.name}</span>
                            </td>
                            <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500 dark:text-gray-400">
                              {formatDate(org.createdAt)}
                            </td>
                            <td className="px-6 py-4 whitespace-nowrap text-sm text-right text-gray-500 dark:text-gray-400">
                              {org.memberCount}
                            </td>
                          </tr>
                        ))
                      ) : (
                        <tr>
                          <td colSpan={3} className="px-6 py-8 text-center text-gray-500 dark:text-gray-400">
                            No organizations yet
                          </td>
                        </tr>
                      )}
                    </tbody>
                  </table>
                </div>
              </div>

              {/* Recent Activity */}
              <div className="bg-white dark:bg-gray-800 rounded-lg shadow border border-gray-200 dark:border-gray-700">
                <div className="p-6 border-b border-gray-200 dark:border-gray-700">
                  <h3 className="text-lg font-semibold text-gray-900 dark:text-white flex items-center gap-2">
                    <Activity className="w-5 h-5 text-orange-600" />
                    Recent Activity
                  </h3>
                </div>
                <div className="overflow-x-auto">
                  <table className="w-full">
                    <thead className="bg-gray-50 dark:bg-gray-900/50">
                      <tr>
                        <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider">User</th>
                        <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider">Operation</th>
                        <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider">Time</th>
                        <th className="px-6 py-3 text-center text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider">Status</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-gray-200 dark:divide-gray-700">
                      {(analytics.recentActivity?.length ?? 0) > 0 ? (
                        analytics.recentActivity.map((activity, index) => (
                          <tr key={index} className="hover:bg-gray-50 dark:hover:bg-gray-900/30">
                            <td className="px-6 py-4 whitespace-nowrap text-sm font-medium text-gray-900 dark:text-white">
                              {activity.username || 'Anonymous'}
                            </td>
                            <td className="px-6 py-4 whitespace-nowrap">
                              <div>
                                <p className="text-sm text-gray-900 dark:text-white">{activity.operationType}</p>
                                <p className="text-xs text-gray-500 dark:text-gray-400 truncate max-w-[150px]" title={activity.resource}>
                                  {activity.documentType || activity.resource || '-'}
                                </p>
                              </div>
                            </td>
                            <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500 dark:text-gray-400">
                              {activity.timestamp ? formatDateTime(activity.timestamp) : '-'}
                            </td>
                            <td className="px-6 py-4 whitespace-nowrap text-center">
                              {activity.status === 'SUCCESS' ? (
                                <CheckCircle className="w-5 h-5 text-emerald-500 inline" />
                              ) : (
                                <XCircle className="w-5 h-5 text-red-500 inline" />
                              )}
                            </td>
                          </tr>
                        ))
                      ) : (
                        <tr>
                          <td colSpan={4} className="px-6 py-8 text-center text-gray-500 dark:text-gray-400">
                            No recent activity
                          </td>
                        </tr>
                      )}
                    </tbody>
                  </table>
                </div>
              </div>
            </div>
          </>
        )}
      </div>
    </div>
  );
}
