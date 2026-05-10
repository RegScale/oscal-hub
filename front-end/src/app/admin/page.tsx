'use client';

import { useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import { Building2, Users, BarChart3, FileText, Activity, CheckCircle2, XCircle, Loader2, ShieldCheck, KeyRound, SlidersHorizontal } from 'lucide-react';
import { apiClient } from '@/lib/api-client';
import type { HealthStatus } from '@/types/oscal';
import { HelpButton } from '@/components/HelpButton';

export default function AdminDashboardPage() {
  const router = useRouter();
  const [loading, setLoading] = useState(true);
  const [healthStatus, setHealthStatus] = useState<HealthStatus | 'LOADING'>('LOADING');

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
    setLoading(false);

    // Fetch health status
    const fetchHealth = async () => {
      try {
        const health = await apiClient.getSimpleHealth();
        setHealthStatus(health.status);
      } catch (error) {
        console.error('Failed to fetch health status:', error);
        setHealthStatus('DOWN');
      }
    };

    fetchHealth();
    // Refresh health status every 30 seconds
    const interval = setInterval(fetchHealth, 30000);
    return () => clearInterval(interval);
  }, [router]);

  if (loading) {
    return (
      <div className="min-h-screen flex items-center justify-center">
        <div className="text-center">
          <div className="animate-spin rounded-full h-16 w-16 border-b-2 border-blue-600 mx-auto"></div>
          <p className="mt-4 text-gray-600 dark:text-gray-400">Loading...</p>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gray-50 dark:bg-gray-900 py-8 px-4 sm:px-6 lg:px-8">
      <div className="max-w-6xl mx-auto">
        {/* Header */}
        <div className="text-center mb-12">
          <div className="flex items-center justify-center gap-2">
            <h1 className="text-3xl font-bold text-gray-900 dark:text-white">
              Super Admin Dashboard
            </h1>
            <HelpButton slug="admin" />
          </div>
          <p className="mt-2 text-gray-600 dark:text-gray-400">
            Manage organizations and users across the platform
          </p>
        </div>

        {/* Tiles Grid */}
        <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
          {/* Manage Organizations Tile */}
          <button
            onClick={() => router.push('/admin/organizations')}
            className="group bg-white dark:bg-gray-800 rounded-lg shadow-md hover:shadow-lg transition-all duration-200 p-8 text-left border border-gray-200 dark:border-gray-700 hover:border-blue-500 dark:hover:border-blue-400"
          >
            <div className="flex items-center justify-center w-16 h-16 bg-blue-100 dark:bg-blue-900/30 rounded-lg mb-6 group-hover:bg-blue-200 dark:group-hover:bg-blue-900/50 transition-colors">
              <Building2 className="w-8 h-8 text-blue-600 dark:text-blue-400" />
            </div>
            <h2 className="text-xl font-semibold text-gray-900 dark:text-white mb-2">
              Manage Organizations
            </h2>
            <p className="text-gray-600 dark:text-gray-400">
              Create, edit, and manage organizations. View organization members and assign administrators.
            </p>
            <div className="mt-4 flex items-center text-blue-600 dark:text-blue-400 font-medium">
              <span>Go to Organizations</span>
              <svg className="w-4 h-4 ml-2 group-hover:translate-x-1 transition-transform" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5l7 7-7 7" />
              </svg>
            </div>
          </button>

          {/* Manage Users Tile */}
          <button
            onClick={() => router.push('/admin/users')}
            className="group bg-white dark:bg-gray-800 rounded-lg shadow-md hover:shadow-lg transition-all duration-200 p-8 text-left border border-gray-200 dark:border-gray-700 hover:border-purple-500 dark:hover:border-purple-400"
          >
            <div className="flex items-center justify-center w-16 h-16 bg-purple-100 dark:bg-purple-900/30 rounded-lg mb-6 group-hover:bg-purple-200 dark:group-hover:bg-purple-900/50 transition-colors">
              <Users className="w-8 h-8 text-purple-600 dark:text-purple-400" />
            </div>
            <h2 className="text-xl font-semibold text-gray-900 dark:text-white mb-2">
              Manage Users
            </h2>
            <p className="text-gray-600 dark:text-gray-400">
              View users by organization, manage pending access requests, and approve or reject new users.
            </p>
            <div className="mt-4 flex items-center text-purple-600 dark:text-purple-400 font-medium">
              <span>Go to Users</span>
              <svg className="w-4 h-4 ml-2 group-hover:translate-x-1 transition-transform" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5l7 7-7 7" />
              </svg>
            </div>
          </button>

          {/* Analytics Tile */}
          <button
            onClick={() => router.push('/admin/analytics')}
            className="group bg-white dark:bg-gray-800 rounded-lg shadow-md hover:shadow-lg transition-all duration-200 p-8 text-left border border-gray-200 dark:border-gray-700 hover:border-emerald-500 dark:hover:border-emerald-400"
          >
            <div className="flex items-center justify-center w-16 h-16 bg-emerald-100 dark:bg-emerald-900/30 rounded-lg mb-6 group-hover:bg-emerald-200 dark:group-hover:bg-emerald-900/50 transition-colors">
              <BarChart3 className="w-8 h-8 text-emerald-600 dark:text-emerald-400" />
            </div>
            <h2 className="text-xl font-semibold text-gray-900 dark:text-white mb-2">
              Analytics
            </h2>
            <p className="text-gray-600 dark:text-gray-400">
              View platform analytics, user activity trends, and usage statistics across all organizations.
            </p>
            <div className="mt-4 flex items-center text-emerald-600 dark:text-emerald-400 font-medium">
              <span>View Analytics</span>
              <svg className="w-4 h-4 ml-2 group-hover:translate-x-1 transition-transform" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5l7 7-7 7" />
              </svg>
            </div>
          </button>

          {/* Logs Tile */}
          <button
            onClick={() => router.push('/admin/logs')}
            className="group bg-white dark:bg-gray-800 rounded-lg shadow-md hover:shadow-lg transition-all duration-200 p-8 text-left border border-gray-200 dark:border-gray-700 hover:border-amber-500 dark:hover:border-amber-400"
          >
            <div className="flex items-center justify-center w-16 h-16 bg-amber-100 dark:bg-amber-900/30 rounded-lg mb-6 group-hover:bg-amber-200 dark:group-hover:bg-amber-900/50 transition-colors">
              <FileText className="w-8 h-8 text-amber-600 dark:text-amber-400" />
            </div>
            <h2 className="text-xl font-semibold text-gray-900 dark:text-white mb-2">
              Logs
            </h2>
            <p className="text-gray-600 dark:text-gray-400">
              View audit logs, security events, and system errors. Search, filter, and export for compliance.
            </p>
            <div className="mt-4 flex items-center text-amber-600 dark:text-amber-400 font-medium">
              <span>View Logs</span>
              <svg className="w-4 h-4 ml-2 group-hover:translate-x-1 transition-transform" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5l7 7-7 7" />
              </svg>
            </div>
          </button>

          {/* System Health Tile */}
          <button
            onClick={() => router.push('/admin/health')}
            className="group bg-white dark:bg-gray-800 rounded-lg shadow-md hover:shadow-lg transition-all duration-200 p-8 text-left border border-gray-200 dark:border-gray-700 hover:border-cyan-500 dark:hover:border-cyan-400"
          >
            <div className="flex items-center justify-center w-16 h-16 bg-cyan-100 dark:bg-cyan-900/30 rounded-lg mb-6 group-hover:bg-cyan-200 dark:group-hover:bg-cyan-900/50 transition-colors relative">
              <Activity className="w-8 h-8 text-cyan-600 dark:text-cyan-400" />
              {/* Live Status Indicator */}
              <div className="absolute -top-1 -right-1">
                {healthStatus === 'LOADING' && (
                  <Loader2 className="w-5 h-5 text-gray-400 animate-spin" />
                )}
                {healthStatus === 'UP' && (
                  <CheckCircle2 className="w-5 h-5 text-green-500" />
                )}
                {(healthStatus === 'DOWN' || healthStatus === 'DEGRADED' || healthStatus === 'UNKNOWN') && (
                  <XCircle className="w-5 h-5 text-red-500" />
                )}
              </div>
            </div>
            <div className="flex items-center mb-2">
              <h2 className="text-xl font-semibold text-gray-900 dark:text-white">
                System Health
              </h2>
              {/* Status Badge */}
              {healthStatus !== 'LOADING' && (
                <span className={`ml-2 px-2 py-0.5 text-xs font-medium rounded-full ${
                  healthStatus === 'UP'
                    ? 'bg-green-100 text-green-700 dark:bg-green-900/30 dark:text-green-400'
                    : healthStatus === 'DEGRADED'
                    ? 'bg-yellow-100 text-yellow-700 dark:bg-yellow-900/30 dark:text-yellow-400'
                    : 'bg-red-100 text-red-700 dark:bg-red-900/30 dark:text-red-400'
                }`}>
                  {healthStatus}
                </span>
              )}
            </div>
            <p className="text-gray-600 dark:text-gray-400">
              Monitor system health, component status, memory usage, and service availability.
            </p>
            <div className="mt-4 flex items-center text-cyan-600 dark:text-cyan-400 font-medium">
              <span>View Health Status</span>
              <svg className="w-4 h-4 ml-2 group-hover:translate-x-1 transition-transform" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5l7 7-7 7" />
              </svg>
            </div>
          </button>

          {/* Security Compliance Tile */}
          <button
            onClick={() => router.push('/admin/security')}
            className="group bg-white dark:bg-gray-800 rounded-lg shadow-md hover:shadow-lg transition-all duration-200 p-8 text-left border border-gray-200 dark:border-gray-700 hover:border-indigo-500 dark:hover:border-indigo-400"
          >
            <div className="flex items-center justify-center w-16 h-16 bg-indigo-100 dark:bg-indigo-900/30 rounded-lg mb-6 group-hover:bg-indigo-200 dark:group-hover:bg-indigo-900/50 transition-colors">
              <ShieldCheck className="w-8 h-8 text-indigo-600 dark:text-indigo-400" />
            </div>
            <h2 className="text-xl font-semibold text-gray-900 dark:text-white mb-2">
              Security Compliance
            </h2>
            <p className="text-gray-600 dark:text-gray-400">
              View SOC 2 compliance status, control implementation, and gap analysis for attestation.
            </p>
            <div className="mt-4 flex items-center text-indigo-600 dark:text-indigo-400 font-medium">
              <span>View Compliance</span>
              <svg className="w-4 h-4 ml-2 group-hover:translate-x-1 transition-transform" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5l7 7-7 7" />
              </svg>
            </div>
          </button>

          {/* Org Admin Panel Tile */}
          <button
            onClick={() => router.push('/org-admin')}
            title="Manage organization members, review access requests, and view usage analytics"
            className="group bg-white dark:bg-gray-800 rounded-lg shadow-md hover:shadow-lg transition-all duration-200 p-8 text-left border border-gray-200 dark:border-gray-700 hover:border-teal-500 dark:hover:border-teal-400"
          >
            <div className="flex items-center justify-center w-16 h-16 bg-teal-100 dark:bg-teal-900/30 rounded-lg mb-6 group-hover:bg-teal-200 dark:group-hover:bg-teal-900/50 transition-colors">
              <SlidersHorizontal className="w-8 h-8 text-teal-600 dark:text-teal-400" />
            </div>
            <h2 className="text-xl font-semibold text-gray-900 dark:text-white mb-2">
              Setup - Admin Panel
            </h2>
            <p className="text-gray-600 dark:text-gray-400">
              Manage org members, review access requests, and view org-scoped usage analytics.
            </p>
            <div className="mt-4 flex items-center text-teal-600 dark:text-teal-400 font-medium">
              <span>Go to Org Admin</span>
              <svg className="w-4 h-4 ml-2 group-hover:translate-x-1 transition-transform" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5l7 7-7 7" />
              </svg>
            </div>
          </button>

          {/* Security Policy Tile */}
          <button
            onClick={() => router.push('/admin/security-policy')}
            className="group bg-white dark:bg-gray-800 rounded-lg shadow-md hover:shadow-lg transition-all duration-200 p-8 text-left border border-gray-200 dark:border-gray-700 hover:border-rose-500 dark:hover:border-rose-400"
          >
            <div className="flex items-center justify-center w-16 h-16 bg-rose-100 dark:bg-rose-900/30 rounded-lg mb-6 group-hover:bg-rose-200 dark:group-hover:bg-rose-900/50 transition-colors">
              <KeyRound className="w-8 h-8 text-rose-600 dark:text-rose-400" />
            </div>
            <h2 className="text-xl font-semibold text-gray-900 dark:text-white mb-2">
              Security Policy
            </h2>
            <p className="text-gray-600 dark:text-gray-400">
              Configure MFA requirements, password policies, and audit log retention settings.
            </p>
            <div className="mt-4 flex items-center text-rose-600 dark:text-rose-400 font-medium">
              <span>Manage Policy</span>
              <svg className="w-4 h-4 ml-2 group-hover:translate-x-1 transition-transform" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5l7 7-7 7" />
              </svg>
            </div>
          </button>
        </div>
      </div>
    </div>
  );
}
