'use client';

import { useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import { Users, ClipboardCheck, LogIn, Activity, ChevronRight, Loader2 } from 'lucide-react';
import { apiClient } from '@/lib/api-client';

interface DashboardSummary {
  totalMembers: number;
  pendingRequests: number;
  loginsThisMonth: number;
  operationsThisMonth: number;
}

export default function OrgAdminDashboardPage() {
  const router = useRouter();
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [summary, setSummary] = useState<DashboardSummary | null>(null);
  const [orgName, setOrgName] = useState('');

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

      if (!isOrgAdmin && !isSuperAdmin) {
        router.push('/');
        return;
      }

      const currentOrg = localStorage.getItem('currentOrganization');
      if (currentOrg) {
        const orgData = JSON.parse(currentOrg);
        setOrgName(orgData.name || '');
        fetchSummary(orgData.id);
      } else if (userData.organizationId) {
        setOrgName(userData.organizationName || '');
        fetchSummary(userData.organizationId);
      } else {
        setError('No organization selected');
        setLoading(false);
      }
    } catch {
      router.push('/login');
    }
  }, [router]);

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
  };

  if (loading) {
    return (
      <div className="min-h-screen bg-gray-50 dark:bg-gray-900 flex items-center justify-center">
        <Loader2 className="h-8 w-8 animate-spin text-blue-500" />
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
  };

  const quickLinks = [
    { label: 'Manage Users', description: 'View and manage organization members', href: '/org-admin/users', icon: Users, color: 'blue' },
    { label: 'Access Requests', description: 'Review pending access requests', href: '/org-admin/requests', icon: ClipboardCheck, color: 'green' },
    { label: 'Usage Analytics', description: 'View organization usage metrics', href: '/org-admin/analytics', icon: Activity, color: 'purple' },
  ];

  return (
    <div className="min-h-screen bg-gray-50 dark:bg-gray-900 py-8 px-4 sm:px-6 lg:px-8">
      <div className="max-w-6xl mx-auto">
        {/* Header */}
        <div className="text-center mb-12">
          <h1 className="text-3xl font-bold text-gray-900 dark:text-white">
            Organization Admin
          </h1>
          <p className="mt-2 text-gray-600 dark:text-gray-400">
            {orgName ? `Managing ${orgName}` : 'Manage your organization'}
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
                <div className={`flex items-center justify-center w-16 h-16 ${colors.iconBg} rounded-lg mb-6 group-hover:bg-blue-200 dark:group-hover:bg-blue-900/50 transition-colors`}>
                  <Icon className={`h-8 w-8 ${colors.text}`} />
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
