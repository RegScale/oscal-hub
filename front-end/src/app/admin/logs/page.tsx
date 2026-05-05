'use client';

import { useEffect, useState, useCallback } from 'react';
import { useRouter } from 'next/navigation';
import {
  ArrowLeft,
  RefreshCw,
  Search,
  X,
  ChevronLeft,
  ChevronRight,
  Download,
  Filter,
  AlertTriangle,
  Shield,
  FileText,
  Activity,
  Clock,
  User,
  Globe,
  CheckCircle,
  XCircle,
  AlertCircle
} from 'lucide-react';
import { apiClient } from '@/lib/api-client';
import type { AuditLog, AuditLogStats } from '@/types/oscal';
import { HelpButton } from '@/components/HelpButton';

type TabType = 'raw' | 'security' | 'errors';

interface Filters {
  search: string;
  username: string;
  riskLevel: string;
  startDate: string;
  endDate: string;
}

export default function LogsPage() {
  const router = useRouter();
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [activeTab, setActiveTab] = useState<TabType>('raw');
  const [logs, setLogs] = useState<AuditLog[]>([]);
  const [stats, setStats] = useState<AuditLogStats | null>(null);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [currentPage, setCurrentPage] = useState(0);
  const [pageSize] = useState(50);
  const [selectedLog, setSelectedLog] = useState<AuditLog | null>(null);
  const [showFilters, setShowFilters] = useState(false);
  const [filters, setFilters] = useState<Filters>({
    search: '',
    username: '',
    riskLevel: '',
    startDate: '',
    endDate: '',
  });

  // Check admin access
  useEffect(() => {
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
  }, [router]);

  // Load stats
  const loadStats = useCallback(async () => {
    try {
      const statsData = await apiClient.getAuditLogStats();
      setStats(statsData);
    } catch (error) {
      console.error('Failed to load stats:', error);
    }
  }, []);

  // Load logs based on active tab and filters
  const loadLogs = useCallback(async (page = 0) => {
    try {
      setRefreshing(true);
      let result;

      // Build filter object for tab-specific endpoints
      const tabFilters = {
        username: filters.username || undefined,
        riskLevel: filters.riskLevel || undefined,
      };

      if (filters.search) {
        result = await apiClient.searchAuditLogs(filters.search, page, pageSize);
      } else if (activeTab === 'raw') {
        result = await apiClient.getRawLogs(page, pageSize, tabFilters);
      } else if (activeTab === 'security') {
        result = await apiClient.getSecurityLogs(page, pageSize, tabFilters);
      } else if (activeTab === 'errors') {
        result = await apiClient.getErrorLogs(page, pageSize, tabFilters);
      } else {
        result = await apiClient.getAuditLogs({
          page,
          size: pageSize,
          username: filters.username || undefined,
          riskLevel: filters.riskLevel || undefined,
          startDate: filters.startDate || undefined,
          endDate: filters.endDate || undefined,
        });
      }

      setLogs(result.content);
      setTotalPages(result.totalPages);
      setTotalElements(result.totalElements);
      setCurrentPage(result.number);
    } catch (error) {
      console.error('Failed to load logs:', error);
    } finally {
      setRefreshing(false);
      setLoading(false);
    }
  }, [activeTab, filters, pageSize]);

  // Initial load
  useEffect(() => {
    loadStats();
    loadLogs(0);
  }, [loadStats, loadLogs]);

  // Reload when tab changes
  useEffect(() => {
    setCurrentPage(0);
    loadLogs(0);
  }, [activeTab]);

  const handleRefresh = () => {
    loadStats();
    loadLogs(currentPage);
  };

  const handleSearch = (e: React.FormEvent) => {
    e.preventDefault();
    setCurrentPage(0);
    loadLogs(0);
  };

  const clearFilters = () => {
    setFilters({
      search: '',
      username: '',
      riskLevel: '',
      startDate: '',
      endDate: '',
    });
    setCurrentPage(0);
  };

  const [exporting, setExporting] = useState(false);

  const handleExportCsv = async () => {
    try {
      setExporting(true);
      await apiClient.exportLogsCsv({
        username: filters.username || undefined,
        riskLevel: filters.riskLevel || undefined,
        startDate: filters.startDate || undefined,
        endDate: filters.endDate || undefined,
      });
    } catch (error) {
      console.error('Failed to export CSV:', error);
      alert('Failed to export CSV. Please try again.');
    } finally {
      setExporting(false);
    }
  };

  const handleExportJson = async () => {
    try {
      setExporting(true);
      await apiClient.exportLogsJson({
        username: filters.username || undefined,
        riskLevel: filters.riskLevel || undefined,
        startDate: filters.startDate || undefined,
        endDate: filters.endDate || undefined,
      });
    } catch (error) {
      console.error('Failed to export JSON:', error);
      alert('Failed to export JSON. Please try again.');
    } finally {
      setExporting(false);
    }
  };

  const formatTimestamp = (timestamp: string) => {
    return new Date(timestamp).toLocaleString();
  };

  const getOutcomeIcon = (outcome: string) => {
    switch (outcome) {
      case 'SUCCESS':
        return <CheckCircle className="w-4 h-4 text-green-500" />;
      case 'FAILURE':
        return <XCircle className="w-4 h-4 text-red-500" />;
      case 'ERROR':
        return <AlertCircle className="w-4 h-4 text-orange-500" />;
      default:
        return <Activity className="w-4 h-4 text-gray-500" />;
    }
  };

  const getRiskLevelBadge = (level: string) => {
    const colors = {
      LOW: 'bg-green-100 text-green-800 dark:bg-green-900/30 dark:text-green-400',
      MEDIUM: 'bg-yellow-100 text-yellow-800 dark:bg-yellow-900/30 dark:text-yellow-400',
      HIGH: 'bg-red-100 text-red-800 dark:bg-red-900/30 dark:text-red-400',
    };
    return colors[level as keyof typeof colors] || colors.LOW;
  };

  const getCategoryIcon = (category: string) => {
    switch (category) {
      case 'Authentication':
        return <User className="w-4 h-4" />;
      case 'Authorization':
        return <Shield className="w-4 h-4" />;
      case 'Security':
        return <AlertTriangle className="w-4 h-4" />;
      case 'Data Access':
        return <FileText className="w-4 h-4" />;
      case 'System':
        return <Activity className="w-4 h-4" />;
      default:
        return <Globe className="w-4 h-4" />;
    }
  };

  if (loading) {
    return (
      <div className="min-h-screen flex items-center justify-center">
        <div className="text-center">
          <div className="animate-spin rounded-full h-16 w-16 border-b-2 border-amber-600 mx-auto"></div>
          <p className="mt-4 text-gray-600 dark:text-gray-400">Loading logs...</p>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gray-50 dark:bg-gray-900">
      {/* Header */}
      <div className="bg-white dark:bg-gray-800 border-b border-gray-200 dark:border-gray-700">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-4">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-4">
              <button
                onClick={() => router.push('/admin')}
                className="p-2 text-gray-500 hover:text-gray-700 dark:text-gray-400 dark:hover:text-gray-200"
              >
                <ArrowLeft className="w-5 h-5" />
              </button>
              <div>
                <div className="flex items-center gap-2">
                  <h1 className="text-2xl font-bold text-gray-900 dark:text-white">Audit Logs</h1>
                  <HelpButton slug="admin-logs" />
                </div>
                <p className="text-sm text-gray-500 dark:text-gray-400">
                  View and analyze system audit logs, security events, and errors
                </p>
              </div>
            </div>
            <div className="flex items-center gap-2">
              <button
                onClick={handleRefresh}
                disabled={refreshing}
                className="flex items-center gap-2 px-4 py-2 text-gray-700 dark:text-gray-200 bg-gray-100 dark:bg-gray-700 rounded-lg hover:bg-gray-200 dark:hover:bg-gray-600 disabled:opacity-50"
              >
                <RefreshCw className={`w-4 h-4 ${refreshing ? 'animate-spin' : ''}`} />
                Refresh
              </button>
            </div>
          </div>
        </div>
      </div>

      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-6">
        {/* Stats Cards */}
        {stats && (
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-5 gap-4 mb-6">
            <div className="bg-white dark:bg-gray-800 rounded-lg p-4 border border-gray-200 dark:border-gray-700">
              <div className="flex items-center gap-3">
                <div className="p-2 bg-blue-100 dark:bg-blue-900/30 rounded-lg">
                  <FileText className="w-5 h-5 text-blue-600 dark:text-blue-400" />
                </div>
                <div>
                  <p className="text-sm text-gray-500 dark:text-gray-400">Total Logs</p>
                  <p className="text-xl font-bold text-gray-900 dark:text-white">{stats.totalLogs.toLocaleString()}</p>
                </div>
              </div>
            </div>
            <div className="bg-white dark:bg-gray-800 rounded-lg p-4 border border-gray-200 dark:border-gray-700">
              <div className="flex items-center gap-3">
                <div className="p-2 bg-green-100 dark:bg-green-900/30 rounded-lg">
                  <Activity className="w-5 h-5 text-green-600 dark:text-green-400" />
                </div>
                <div>
                  <p className="text-sm text-gray-500 dark:text-gray-400">Today</p>
                  <p className="text-xl font-bold text-gray-900 dark:text-white">{stats.logsToday.toLocaleString()}</p>
                </div>
              </div>
            </div>
            <div className="bg-white dark:bg-gray-800 rounded-lg p-4 border border-gray-200 dark:border-gray-700">
              <div className="flex items-center gap-3">
                <div className="p-2 bg-amber-100 dark:bg-amber-900/30 rounded-lg">
                  <Shield className="w-5 h-5 text-amber-600 dark:text-amber-400" />
                </div>
                <div>
                  <p className="text-sm text-gray-500 dark:text-gray-400">Security Today</p>
                  <p className="text-xl font-bold text-gray-900 dark:text-white">{stats.securityEventsToday.toLocaleString()}</p>
                </div>
              </div>
            </div>
            <div className="bg-white dark:bg-gray-800 rounded-lg p-4 border border-gray-200 dark:border-gray-700">
              <div className="flex items-center gap-3">
                <div className="p-2 bg-red-100 dark:bg-red-900/30 rounded-lg">
                  <AlertTriangle className="w-5 h-5 text-red-600 dark:text-red-400" />
                </div>
                <div>
                  <p className="text-sm text-gray-500 dark:text-gray-400">Errors Today</p>
                  <p className="text-xl font-bold text-gray-900 dark:text-white">{stats.errorsToday.toLocaleString()}</p>
                </div>
              </div>
            </div>
            <div className="bg-white dark:bg-gray-800 rounded-lg p-4 border border-gray-200 dark:border-gray-700">
              <div className="flex items-center gap-3">
                <div className="p-2 bg-purple-100 dark:bg-purple-900/30 rounded-lg">
                  <Clock className="w-5 h-5 text-purple-600 dark:text-purple-400" />
                </div>
                <div>
                  <p className="text-sm text-gray-500 dark:text-gray-400">High Risk Unreviewed</p>
                  <p className="text-xl font-bold text-gray-900 dark:text-white">{stats.highRiskUnreviewed.toLocaleString()}</p>
                </div>
              </div>
            </div>
          </div>
        )}

        {/* Tabs and Filters */}
        <div className="bg-white dark:bg-gray-800 rounded-lg border border-gray-200 dark:border-gray-700 mb-6">
          <div className="border-b border-gray-200 dark:border-gray-700">
            <div className="flex items-center justify-between px-4">
              <nav className="flex -mb-px">
                <button
                  onClick={() => setActiveTab('raw')}
                  className={`px-4 py-4 text-sm font-medium border-b-2 ${
                    activeTab === 'raw'
                      ? 'border-amber-500 text-amber-600 dark:text-amber-400'
                      : 'border-transparent text-gray-500 dark:text-gray-400 hover:text-gray-700 dark:hover:text-gray-200'
                  }`}
                >
                  <div className="flex items-center gap-2">
                    <FileText className="w-4 h-4" />
                    Raw Logs
                  </div>
                </button>
                <button
                  onClick={() => setActiveTab('security')}
                  className={`px-4 py-4 text-sm font-medium border-b-2 ${
                    activeTab === 'security'
                      ? 'border-amber-500 text-amber-600 dark:text-amber-400'
                      : 'border-transparent text-gray-500 dark:text-gray-400 hover:text-gray-700 dark:hover:text-gray-200'
                  }`}
                >
                  <div className="flex items-center gap-2">
                    <Shield className="w-4 h-4" />
                    Security Logs
                  </div>
                </button>
                <button
                  onClick={() => setActiveTab('errors')}
                  className={`px-4 py-4 text-sm font-medium border-b-2 ${
                    activeTab === 'errors'
                      ? 'border-amber-500 text-amber-600 dark:text-amber-400'
                      : 'border-transparent text-gray-500 dark:text-gray-400 hover:text-gray-700 dark:hover:text-gray-200'
                  }`}
                >
                  <div className="flex items-center gap-2">
                    <AlertTriangle className="w-4 h-4" />
                    Error Logs
                  </div>
                </button>
              </nav>
              <div className="flex items-center gap-2 py-2">
                <button
                  onClick={() => setShowFilters(!showFilters)}
                  className={`flex items-center gap-2 px-3 py-2 text-sm rounded-lg ${
                    showFilters
                      ? 'bg-amber-100 text-amber-700 dark:bg-amber-900/30 dark:text-amber-400'
                      : 'text-gray-600 dark:text-gray-400 hover:bg-gray-100 dark:hover:bg-gray-700'
                  }`}
                >
                  <Filter className="w-4 h-4" />
                  Filters
                </button>
                <button
                  onClick={handleExportCsv}
                  disabled={exporting}
                  className="flex items-center gap-2 px-3 py-2 text-sm text-gray-600 dark:text-gray-400 hover:bg-gray-100 dark:hover:bg-gray-700 rounded-lg disabled:opacity-50"
                >
                  <Download className={`w-4 h-4 ${exporting ? 'animate-pulse' : ''}`} />
                  CSV
                </button>
                <button
                  onClick={handleExportJson}
                  disabled={exporting}
                  className="flex items-center gap-2 px-3 py-2 text-sm text-gray-600 dark:text-gray-400 hover:bg-gray-100 dark:hover:bg-gray-700 rounded-lg disabled:opacity-50"
                >
                  <Download className={`w-4 h-4 ${exporting ? 'animate-pulse' : ''}`} />
                  JSON
                </button>
              </div>
            </div>
          </div>

          {/* Filters Panel */}
          {showFilters && (
            <div className="p-4 border-b border-gray-200 dark:border-gray-700 bg-gray-50 dark:bg-gray-800/50">
              <form onSubmit={handleSearch} className="grid grid-cols-1 md:grid-cols-5 gap-4">
                <div className="md:col-span-2">
                  <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                    Search
                  </label>
                  <div className="relative">
                    <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" />
                    <input
                      type="text"
                      value={filters.search}
                      onChange={(e) => setFilters({ ...filters, search: e.target.value })}
                      placeholder="Search logs..."
                      className="w-full pl-10 pr-4 py-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-white focus:ring-2 focus:ring-amber-500"
                    />
                  </div>
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                    Username
                  </label>
                  <input
                    type="text"
                    value={filters.username}
                    onChange={(e) => setFilters({ ...filters, username: e.target.value })}
                    placeholder="Filter by user"
                    className="w-full px-4 py-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-white focus:ring-2 focus:ring-amber-500"
                  />
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                    Risk Level
                  </label>
                  <select
                    value={filters.riskLevel}
                    onChange={(e) => setFilters({ ...filters, riskLevel: e.target.value })}
                    className="w-full px-4 py-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-white focus:ring-2 focus:ring-amber-500"
                  >
                    <option value="">All Levels</option>
                    <option value="LOW">Low</option>
                    <option value="MEDIUM">Medium</option>
                    <option value="HIGH">High</option>
                  </select>
                </div>
                <div className="flex items-end gap-2">
                  <button
                    type="submit"
                    className="flex-1 px-4 py-2 bg-amber-600 text-white rounded-lg hover:bg-amber-700"
                  >
                    Apply
                  </button>
                  <button
                    type="button"
                    onClick={clearFilters}
                    className="px-4 py-2 text-gray-600 dark:text-gray-400 hover:bg-gray-200 dark:hover:bg-gray-700 rounded-lg"
                  >
                    Clear
                  </button>
                </div>
              </form>
            </div>
          )}

          {/* Logs Table */}
          <div className="overflow-x-auto">
            <table className="w-full">
              <thead className="bg-gray-50 dark:bg-gray-700/50">
                <tr>
                  <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider">
                    Timestamp
                  </th>
                  <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider">
                    Event
                  </th>
                  <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider">
                    User
                  </th>
                  <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider">
                    IP Address
                  </th>
                  <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider">
                    Resource
                  </th>
                  <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider">
                    Outcome
                  </th>
                  <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider">
                    Risk
                  </th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-200 dark:divide-gray-700">
                {logs.length === 0 ? (
                  <tr>
                    <td colSpan={7} className="px-4 py-12 text-center text-gray-500 dark:text-gray-400">
                      <FileText className="w-12 h-12 mx-auto mb-4 opacity-50" />
                      <p>No logs found</p>
                    </td>
                  </tr>
                ) : (
                  logs.map((log) => (
                    <tr
                      key={log.id}
                      onClick={() => setSelectedLog(log)}
                      className="hover:bg-gray-50 dark:hover:bg-gray-700/50 cursor-pointer"
                    >
                      <td className="px-4 py-3 text-sm text-gray-500 dark:text-gray-400 whitespace-nowrap">
                        {formatTimestamp(log.timestamp)}
                      </td>
                      <td className="px-4 py-3">
                        <div className="flex items-center gap-2">
                          <span className="text-gray-400">{getCategoryIcon(log.category)}</span>
                          <span className="text-sm font-medium text-gray-900 dark:text-white">
                            {log.eventType.replace(/_/g, ' ')}
                          </span>
                        </div>
                      </td>
                      <td className="px-4 py-3 text-sm text-gray-900 dark:text-white">
                        {log.username || '-'}
                      </td>
                      <td className="px-4 py-3 text-sm text-gray-500 dark:text-gray-400 font-mono">
                        {log.ipAddress || '-'}
                      </td>
                      <td className="px-4 py-3 text-sm text-gray-500 dark:text-gray-400 max-w-xs truncate">
                        {log.resource || log.requestUrl || '-'}
                      </td>
                      <td className="px-4 py-3">
                        <div className="flex items-center gap-1">
                          {getOutcomeIcon(log.outcome)}
                          <span className="text-sm text-gray-900 dark:text-white">
                            {log.outcome}
                          </span>
                        </div>
                      </td>
                      <td className="px-4 py-3">
                        <span className={`px-2 py-1 text-xs font-medium rounded-full ${getRiskLevelBadge(log.riskLevel)}`}>
                          {log.riskLevel}
                        </span>
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>

          {/* Pagination */}
          {totalPages > 1 && (
            <div className="flex items-center justify-between px-4 py-3 border-t border-gray-200 dark:border-gray-700">
              <div className="text-sm text-gray-500 dark:text-gray-400">
                Showing {currentPage * pageSize + 1} to {Math.min((currentPage + 1) * pageSize, totalElements)} of {totalElements} logs
              </div>
              <div className="flex items-center gap-2">
                <button
                  onClick={() => loadLogs(currentPage - 1)}
                  disabled={currentPage === 0}
                  className="p-2 text-gray-500 dark:text-gray-400 hover:bg-gray-100 dark:hover:bg-gray-700 rounded-lg disabled:opacity-50 disabled:cursor-not-allowed"
                >
                  <ChevronLeft className="w-5 h-5" />
                </button>
                <span className="text-sm text-gray-700 dark:text-gray-300">
                  Page {currentPage + 1} of {totalPages}
                </span>
                <button
                  onClick={() => loadLogs(currentPage + 1)}
                  disabled={currentPage >= totalPages - 1}
                  className="p-2 text-gray-500 dark:text-gray-400 hover:bg-gray-100 dark:hover:bg-gray-700 rounded-lg disabled:opacity-50 disabled:cursor-not-allowed"
                >
                  <ChevronRight className="w-5 h-5" />
                </button>
              </div>
            </div>
          )}
        </div>
      </div>

      {/* Slide-out Detail Panel */}
      {selectedLog && (
        <div className="fixed inset-y-0 right-0 w-full max-w-lg bg-white dark:bg-gray-800 shadow-xl z-50 overflow-y-auto">
          <div className="sticky top-0 bg-white dark:bg-gray-800 border-b border-gray-200 dark:border-gray-700 px-6 py-4">
            <div className="flex items-center justify-between">
              <h2 className="text-lg font-semibold text-gray-900 dark:text-white">Log Details</h2>
              <button
                onClick={() => setSelectedLog(null)}
                className="p-2 text-gray-400 hover:text-gray-600 dark:hover:text-gray-200"
              >
                <X className="w-5 h-5" />
              </button>
            </div>
          </div>

          <div className="p-6 space-y-6">
            {/* Status & Risk */}
            <div className="flex items-center gap-4">
              <div className="flex items-center gap-2">
                {getOutcomeIcon(selectedLog.outcome)}
                <span className="font-medium text-gray-900 dark:text-white">{selectedLog.outcome}</span>
              </div>
              <span className={`px-3 py-1 text-sm font-medium rounded-full ${getRiskLevelBadge(selectedLog.riskLevel)}`}>
                {selectedLog.riskLevel} Risk
              </span>
            </div>

            {/* Event Info */}
            <div>
              <h3 className="text-sm font-medium text-gray-500 dark:text-gray-400 mb-2">Event</h3>
              <div className="flex items-center gap-2">
                {getCategoryIcon(selectedLog.category)}
                <span className="text-gray-900 dark:text-white font-medium">
                  {selectedLog.eventType.replace(/_/g, ' ')}
                </span>
              </div>
              <p className="text-sm text-gray-500 dark:text-gray-400 mt-1">
                Category: {selectedLog.category}
              </p>
            </div>

            {/* Timestamp */}
            <div>
              <h3 className="text-sm font-medium text-gray-500 dark:text-gray-400 mb-2">Timestamp</h3>
              <p className="text-gray-900 dark:text-white">{formatTimestamp(selectedLog.timestamp)}</p>
            </div>

            {/* User Info */}
            <div>
              <h3 className="text-sm font-medium text-gray-500 dark:text-gray-400 mb-2">User</h3>
              <p className="text-gray-900 dark:text-white">{selectedLog.username || 'Anonymous'}</p>
              {selectedLog.userId && (
                <p className="text-sm text-gray-500 dark:text-gray-400">User ID: {selectedLog.userId}</p>
              )}
            </div>

            {/* Client Info */}
            <div>
              <h3 className="text-sm font-medium text-gray-500 dark:text-gray-400 mb-2">Client</h3>
              <p className="text-gray-900 dark:text-white font-mono text-sm">{selectedLog.ipAddress || '-'}</p>
              {selectedLog.userAgent && (
                <p className="text-sm text-gray-500 dark:text-gray-400 mt-1 break-words">{selectedLog.userAgent}</p>
              )}
            </div>

            {/* Request Info */}
            {(selectedLog.requestUrl || selectedLog.httpMethod) && (
              <div>
                <h3 className="text-sm font-medium text-gray-500 dark:text-gray-400 mb-2">Request</h3>
                <p className="text-gray-900 dark:text-white font-mono text-sm break-all">
                  {selectedLog.httpMethod} {selectedLog.requestUrl}
                </p>
              </div>
            )}

            {/* Resource */}
            {selectedLog.resource && (
              <div>
                <h3 className="text-sm font-medium text-gray-500 dark:text-gray-400 mb-2">Resource</h3>
                <p className="text-gray-900 dark:text-white break-words">{selectedLog.resource}</p>
                {selectedLog.action && (
                  <p className="text-sm text-gray-500 dark:text-gray-400">Action: {selectedLog.action}</p>
                )}
              </div>
            )}

            {/* Error Message */}
            {selectedLog.errorMessage && (
              <div>
                <h3 className="text-sm font-medium text-red-500 mb-2">Error Message</h3>
                <p className="text-red-600 dark:text-red-400 bg-red-50 dark:bg-red-900/20 p-3 rounded-lg text-sm">
                  {selectedLog.errorMessage}
                </p>
              </div>
            )}

            {/* Processing Time */}
            {selectedLog.processingTimeMs !== null && (
              <div>
                <h3 className="text-sm font-medium text-gray-500 dark:text-gray-400 mb-2">Processing Time</h3>
                <p className="text-gray-900 dark:text-white">{selectedLog.processingTimeMs}ms</p>
              </div>
            )}

            {/* Metadata */}
            {selectedLog.metadata && (
              <div>
                <h3 className="text-sm font-medium text-gray-500 dark:text-gray-400 mb-2">Metadata</h3>
                <pre className="bg-gray-100 dark:bg-gray-900 p-3 rounded-lg text-sm overflow-x-auto">
                  {JSON.stringify(JSON.parse(selectedLog.metadata), null, 2)}
                </pre>
              </div>
            )}

            {/* Review Status */}
            <div>
              <h3 className="text-sm font-medium text-gray-500 dark:text-gray-400 mb-2">Review Status</h3>
              <p className="text-gray-900 dark:text-white">
                {selectedLog.reviewed ? 'Reviewed' : 'Not Reviewed'}
              </p>
              {selectedLog.reviewedBy && (
                <p className="text-sm text-gray-500 dark:text-gray-400">
                  By {selectedLog.reviewedBy} at {selectedLog.reviewedAt ? formatTimestamp(selectedLog.reviewedAt) : '-'}
                </p>
              )}
              {selectedLog.reviewNotes && (
                <p className="text-sm text-gray-600 dark:text-gray-300 mt-2 p-2 bg-gray-100 dark:bg-gray-700 rounded">
                  {selectedLog.reviewNotes}
                </p>
              )}
            </div>

            {/* Integrity */}
            {selectedLog.integrityHash && (
              <div>
                <h3 className="text-sm font-medium text-gray-500 dark:text-gray-400 mb-2">Integrity Hash</h3>
                <p className="text-gray-500 dark:text-gray-400 font-mono text-xs break-all">
                  {selectedLog.integrityHash}
                </p>
              </div>
            )}
          </div>
        </div>
      )}

      {/* Backdrop for slide-out panel */}
      {selectedLog && (
        <div
          className="fixed inset-0 bg-black/50 z-40"
          onClick={() => setSelectedLog(null)}
        />
      )}
    </div>
  );
}
