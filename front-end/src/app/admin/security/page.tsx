'use client';

import { useEffect, useState, useCallback } from 'react';
import { useRouter } from 'next/navigation';
import {
  ShieldCheck,
  ChevronLeft,
  RefreshCw,
  CheckCircle2,
  XCircle,
  AlertTriangle,
  ChevronDown,
  ChevronRight,
  Home,
  Lock,
  Server,
  Settings,
  AlertCircle,
  FileText,
  Eye,
} from 'lucide-react';
import { apiClient } from '@/lib/api-client';
import type {
  ComplianceSummary,
  Soc2Control,
  GapAnalysis,
  ControlStatus,
  GapSeverity,
} from '@/types/oscal';
import { HelpButton } from '@/components/HelpButton';

const CATEGORY_INFO: Record<string, { name: string; icon: React.ReactNode; description: string }> = {
  CC6: {
    name: 'Logical & Physical Access',
    icon: <Lock className="w-5 h-5" />,
    description: 'Controls for authentication, authorization, and access management',
  },
  CC7: {
    name: 'System Operations',
    icon: <Server className="w-5 h-5" />,
    description: 'Controls for monitoring, detection, and incident response',
  },
  CC8: {
    name: 'Change Management',
    icon: <Settings className="w-5 h-5" />,
    description: 'Controls for authorizing and managing system changes',
  },
  CC9: {
    name: 'Risk Mitigation',
    icon: <AlertCircle className="w-5 h-5" />,
    description: 'Controls for identifying and mitigating risks',
  },
  DATA: {
    name: 'Data Protection',
    icon: <FileText className="w-5 h-5" />,
    description: 'Controls for encryption, validation, and data security',
  },
  AUDIT: {
    name: 'Audit & Monitoring',
    icon: <Eye className="w-5 h-5" />,
    description: 'Controls for logging, retention, and security monitoring',
  },
};

export default function SecurityCompliancePage() {
  const router = useRouter();
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [summary, setSummary] = useState<ComplianceSummary | null>(null);
  const [controls, setControls] = useState<Soc2Control[]>([]);
  const [gaps, setGaps] = useState<GapAnalysis[]>([]);
  const [expandedCategories, setExpandedCategories] = useState<Set<string>>(new Set());
  const [lastUpdated, setLastUpdated] = useState<Date | null>(null);

  const fetchData = useCallback(async (showRefreshIndicator = false) => {
    try {
      if (showRefreshIndicator) setRefreshing(true);
      setError(null);

      const [summaryData, controlsData, gapsData] = await Promise.all([
        apiClient.getComplianceSummary(),
        apiClient.getAllControls(),
        apiClient.getGapAnalysis(),
      ]);

      setSummary(summaryData);
      setControls(controlsData);
      setGaps(gapsData);
      setLastUpdated(new Date());
    } catch (err) {
      console.error('Failed to fetch compliance data:', err);
      setError('Failed to load compliance data. Please try again.');
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  }, []);

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
    fetchData();
  }, [router, fetchData]);

  const toggleCategory = (category: string) => {
    setExpandedCategories((prev) => {
      const next = new Set(prev);
      if (next.has(category)) {
        next.delete(category);
      } else {
        next.add(category);
      }
      return next;
    });
  };

  const getStatusIcon = (status: ControlStatus) => {
    switch (status) {
      case 'IMPLEMENTED':
        return <CheckCircle2 className="h-5 w-5 text-green-500" />;
      case 'PARTIAL':
        return <AlertTriangle className="h-5 w-5 text-yellow-500" />;
      case 'GAP':
        return <XCircle className="h-5 w-5 text-red-500" />;
      default:
        return <AlertCircle className="h-5 w-5 text-gray-500" />;
    }
  };

  const getStatusBadge = (status: ControlStatus) => {
    const base = 'px-2 py-0.5 rounded-full text-xs font-medium';
    switch (status) {
      case 'IMPLEMENTED':
        return <span className={`${base} bg-green-100 text-green-700 dark:bg-green-900/30 dark:text-green-400`}>Implemented</span>;
      case 'PARTIAL':
        return <span className={`${base} bg-yellow-100 text-yellow-700 dark:bg-yellow-900/30 dark:text-yellow-400`}>Partial</span>;
      case 'GAP':
        return <span className={`${base} bg-red-100 text-red-700 dark:bg-red-900/30 dark:text-red-400`}>Gap</span>;
      default:
        return <span className={`${base} bg-gray-100 text-gray-700 dark:bg-gray-900/30 dark:text-gray-400`}>Unknown</span>;
    }
  };

  const getSeverityBadge = (severity: GapSeverity) => {
    const base = 'px-2 py-0.5 rounded-full text-xs font-medium';
    switch (severity) {
      case 'HIGH':
        return <span className={`${base} bg-red-100 text-red-700 dark:bg-red-900/30 dark:text-red-400`}>High</span>;
      case 'MEDIUM':
        return <span className={`${base} bg-yellow-100 text-yellow-700 dark:bg-yellow-900/30 dark:text-yellow-400`}>Medium</span>;
      case 'LOW':
        return <span className={`${base} bg-blue-100 text-blue-700 dark:bg-blue-900/30 dark:text-blue-400`}>Low</span>;
      default:
        return <span className={`${base} bg-gray-100 text-gray-700 dark:bg-gray-900/30 dark:text-gray-400`}>Unknown</span>;
    }
  };

  const controlsByCategory = controls.reduce((acc, control) => {
    const category = control.category;
    if (!acc[category]) {
      acc[category] = [];
    }
    acc[category].push(control);
    return acc;
  }, {} as Record<string, Soc2Control[]>);

  if (loading) {
    return (
      <div className="min-h-screen flex items-center justify-center">
        <div className="text-center">
          <div className="animate-spin rounded-full h-16 w-16 border-b-2 border-indigo-600 mx-auto"></div>
          <p className="mt-4 text-gray-600 dark:text-gray-400">Loading compliance data...</p>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gray-50 dark:bg-gray-900 py-8 px-4 sm:px-6 lg:px-8">
      <div className="max-w-7xl mx-auto">
        {/* Header with Breadcrumb */}
        <div className="mb-8">
          <nav className="flex items-center space-x-2 text-sm text-gray-500 dark:text-gray-400 mb-4">
            <button
              onClick={() => router.push('/admin')}
              className="hover:text-gray-700 dark:hover:text-gray-300 flex items-center"
            >
              <Home className="w-4 h-4 mr-1" />
              Admin
            </button>
            <ChevronRight className="w-4 h-4" />
            <span className="text-gray-900 dark:text-white">Security Compliance</span>
          </nav>

          <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between">
            <div className="flex items-center">
              <button
                onClick={() => router.push('/admin')}
                className="mr-4 p-2 hover:bg-gray-200 dark:hover:bg-gray-700 rounded-lg transition-colors"
              >
                <ChevronLeft className="w-6 h-6 text-gray-600 dark:text-gray-400" />
              </button>
              <div>
                <h1 className="text-3xl font-bold text-gray-900 dark:text-white flex items-center">
                  <ShieldCheck className="w-8 h-8 mr-3 text-indigo-600" />
                  SOC 2 Compliance
                  <HelpButton slug="admin-security" />
                </h1>
                <p className="mt-1 text-gray-600 dark:text-gray-400">
                  Security control implementation status and gap analysis
                </p>
              </div>
            </div>

            <div className="mt-4 sm:mt-0 flex items-center space-x-4">
              {lastUpdated && (
                <span className="text-sm text-gray-500 dark:text-gray-400">
                  Updated {lastUpdated.toLocaleTimeString()}
                </span>
              )}
              <button
                onClick={() => fetchData(true)}
                disabled={refreshing}
                className="flex items-center px-4 py-2 bg-indigo-600 text-white rounded-lg hover:bg-indigo-700 disabled:opacity-50 transition-colors"
              >
                <RefreshCw className={`w-4 h-4 mr-2 ${refreshing ? 'animate-spin' : ''}`} />
                Refresh
              </button>
            </div>
          </div>
        </div>

        {error && (
          <div className="mb-6 p-4 bg-red-50 dark:bg-red-900/20 border border-red-200 dark:border-red-800 rounded-lg">
            <p className="text-red-700 dark:text-red-400">{error}</p>
          </div>
        )}

        {summary && (
          <>
            {/* Compliance Summary Cards */}
            <div className="grid grid-cols-1 md:grid-cols-4 gap-4 mb-8">
              {/* Overall Compliance */}
              <div className="bg-white dark:bg-gray-800 rounded-lg shadow p-6">
                <div className="flex items-center justify-between">
                  <div>
                    <p className="text-sm text-gray-500 dark:text-gray-400">Overall Compliance</p>
                    <p className="text-3xl font-bold text-gray-900 dark:text-white">
                      {summary.compliancePercentage}%
                    </p>
                  </div>
                  <div className="w-16 h-16 relative">
                    <svg className="w-16 h-16 transform -rotate-90">
                      <circle
                        cx="32"
                        cy="32"
                        r="28"
                        stroke="currentColor"
                        strokeWidth="8"
                        fill="none"
                        className="text-gray-200 dark:text-gray-700"
                      />
                      <circle
                        cx="32"
                        cy="32"
                        r="28"
                        stroke="currentColor"
                        strokeWidth="8"
                        fill="none"
                        strokeDasharray={`${summary.compliancePercentage * 1.76} 176`}
                        className={
                          summary.compliancePercentage >= 80
                            ? 'text-green-500'
                            : summary.compliancePercentage >= 60
                            ? 'text-yellow-500'
                            : 'text-red-500'
                        }
                      />
                    </svg>
                  </div>
                </div>
              </div>

              {/* Implemented */}
              <div className="bg-white dark:bg-gray-800 rounded-lg shadow p-6">
                <div className="flex items-center">
                  <div className="p-3 bg-green-100 dark:bg-green-900/30 rounded-lg mr-4">
                    <CheckCircle2 className="w-6 h-6 text-green-600 dark:text-green-400" />
                  </div>
                  <div>
                    <p className="text-sm text-gray-500 dark:text-gray-400">Implemented</p>
                    <p className="text-2xl font-bold text-gray-900 dark:text-white">
                      {summary.implementedControls}
                    </p>
                  </div>
                </div>
              </div>

              {/* Partial */}
              <div className="bg-white dark:bg-gray-800 rounded-lg shadow p-6">
                <div className="flex items-center">
                  <div className="p-3 bg-yellow-100 dark:bg-yellow-900/30 rounded-lg mr-4">
                    <AlertTriangle className="w-6 h-6 text-yellow-600 dark:text-yellow-400" />
                  </div>
                  <div>
                    <p className="text-sm text-gray-500 dark:text-gray-400">Partial</p>
                    <p className="text-2xl font-bold text-gray-900 dark:text-white">
                      {summary.partialControls}
                    </p>
                  </div>
                </div>
              </div>

              {/* Gaps */}
              <div className="bg-white dark:bg-gray-800 rounded-lg shadow p-6">
                <div className="flex items-center">
                  <div className="p-3 bg-red-100 dark:bg-red-900/30 rounded-lg mr-4">
                    <XCircle className="w-6 h-6 text-red-600 dark:text-red-400" />
                  </div>
                  <div>
                    <p className="text-sm text-gray-500 dark:text-gray-400">Gaps</p>
                    <p className="text-2xl font-bold text-gray-900 dark:text-white">
                      {summary.gapControls}
                    </p>
                  </div>
                </div>
              </div>
            </div>

            {/* Control Categories Accordion */}
            <div className="bg-white dark:bg-gray-800 rounded-lg shadow mb-8">
              <div className="p-6 border-b border-gray-200 dark:border-gray-700">
                <h2 className="text-xl font-semibold text-gray-900 dark:text-white">
                  Control Categories
                </h2>
                <p className="mt-1 text-sm text-gray-500 dark:text-gray-400">
                  Click a category to view its controls
                </p>
              </div>

              <div className="divide-y divide-gray-200 dark:divide-gray-700">
                {Object.keys(CATEGORY_INFO).map((category) => {
                  const categoryInfo = CATEGORY_INFO[category];
                  const categoryControls = controlsByCategory[category] || [];
                  const categorySummary = summary.byCategory[category];
                  const isExpanded = expandedCategories.has(category);

                  if (categoryControls.length === 0) return null;

                  return (
                    <div key={category}>
                      <button
                        onClick={() => toggleCategory(category)}
                        className="w-full px-6 py-4 flex items-center justify-between hover:bg-gray-50 dark:hover:bg-gray-700/50 transition-colors"
                      >
                        <div className="flex items-center">
                          <div className="p-2 bg-indigo-100 dark:bg-indigo-900/30 rounded-lg mr-4 text-indigo-600 dark:text-indigo-400">
                            {categoryInfo.icon}
                          </div>
                          <div className="text-left">
                            <div className="flex items-center">
                              <span className="font-medium text-gray-900 dark:text-white">
                                {category}: {categoryInfo.name}
                              </span>
                              {categorySummary && (
                                <span className="ml-2 text-sm text-gray-500 dark:text-gray-400">
                                  ({categorySummary.total} controls)
                                </span>
                              )}
                            </div>
                            <p className="text-sm text-gray-500 dark:text-gray-400">
                              {categoryInfo.description}
                            </p>
                          </div>
                        </div>

                        <div className="flex items-center space-x-4">
                          {categorySummary && (
                            <div className="flex items-center space-x-2 text-sm">
                              <span className="text-green-600 dark:text-green-400">
                                {categorySummary.implemented}
                              </span>
                              <span className="text-gray-300 dark:text-gray-600">/</span>
                              <span className="text-yellow-600 dark:text-yellow-400">
                                {categorySummary.partial}
                              </span>
                              <span className="text-gray-300 dark:text-gray-600">/</span>
                              <span className="text-red-600 dark:text-red-400">
                                {categorySummary.gaps}
                              </span>
                            </div>
                          )}
                          {isExpanded ? (
                            <ChevronDown className="w-5 h-5 text-gray-400" />
                          ) : (
                            <ChevronRight className="w-5 h-5 text-gray-400" />
                          )}
                        </div>
                      </button>

                      {isExpanded && (
                        <div className="px-6 pb-4">
                          <div className="bg-gray-50 dark:bg-gray-900/50 rounded-lg overflow-hidden">
                            <table className="min-w-full">
                              <thead>
                                <tr className="text-left text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider">
                                  <th className="px-4 py-3">Control</th>
                                  <th className="px-4 py-3">Status</th>
                                  <th className="px-4 py-3">Implementation</th>
                                </tr>
                              </thead>
                              <tbody className="divide-y divide-gray-200 dark:divide-gray-700">
                                {categoryControls.map((control) => (
                                  <tr key={control.controlId} className="hover:bg-gray-100 dark:hover:bg-gray-800/50">
                                    <td className="px-4 py-3">
                                      <div className="flex items-center">
                                        {getStatusIcon(control.status)}
                                        <div className="ml-3">
                                          <div className="font-medium text-gray-900 dark:text-white">
                                            {control.controlId}
                                          </div>
                                          <div className="text-sm text-gray-500 dark:text-gray-400">
                                            {control.name}
                                          </div>
                                        </div>
                                      </div>
                                    </td>
                                    <td className="px-4 py-3">
                                      {getStatusBadge(control.status)}
                                    </td>
                                    <td className="px-4 py-3 text-sm text-gray-600 dark:text-gray-400 max-w-md">
                                      {control.implementation}
                                    </td>
                                  </tr>
                                ))}
                              </tbody>
                            </table>
                          </div>
                        </div>
                      )}
                    </div>
                  );
                })}
              </div>
            </div>

            {/* Gap Analysis Section */}
            {gaps.length > 0 && (
              <div className="bg-white dark:bg-gray-800 rounded-lg shadow">
                <div className="p-6 border-b border-gray-200 dark:border-gray-700">
                  <h2 className="text-xl font-semibold text-gray-900 dark:text-white flex items-center">
                    <AlertTriangle className="w-6 h-6 mr-2 text-amber-500" />
                    Gap Analysis & Recommendations
                  </h2>
                  <p className="mt-1 text-sm text-gray-500 dark:text-gray-400">
                    Identified gaps with remediation recommendations
                  </p>
                </div>

                <div className="overflow-x-auto">
                  <table className="min-w-full">
                    <thead className="bg-gray-50 dark:bg-gray-900/50">
                      <tr className="text-left text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider">
                        <th className="px-6 py-3">Priority</th>
                        <th className="px-6 py-3">Gap</th>
                        <th className="px-6 py-3">Severity</th>
                        <th className="px-6 py-3">Recommendation</th>
                        <th className="px-6 py-3">Effort</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-gray-200 dark:divide-gray-700">
                      {gaps.map((gap) => (
                        <tr key={gap.gapId} className="hover:bg-gray-50 dark:hover:bg-gray-700/50">
                          <td className="px-6 py-4">
                            <span className="inline-flex items-center justify-center w-8 h-8 rounded-full bg-indigo-100 dark:bg-indigo-900/30 text-indigo-700 dark:text-indigo-300 font-medium">
                              {gap.priority}
                            </span>
                          </td>
                          <td className="px-6 py-4">
                            <div className="font-medium text-gray-900 dark:text-white">
                              {gap.title}
                            </div>
                            <div className="text-sm text-gray-500 dark:text-gray-400">
                              {gap.controlId}
                            </div>
                          </td>
                          <td className="px-6 py-4">
                            {getSeverityBadge(gap.severity)}
                          </td>
                          <td className="px-6 py-4 text-sm text-gray-600 dark:text-gray-400 max-w-md">
                            {gap.recommendation}
                          </td>
                          <td className="px-6 py-4">
                            <span className="text-sm text-gray-600 dark:text-gray-400">
                              {gap.effort}
                            </span>
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
      </div>
    </div>
  );
}
