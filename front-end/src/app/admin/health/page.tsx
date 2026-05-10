'use client';

import { useEffect, useState, useCallback } from 'react';
import { useRouter } from 'next/navigation';
import {
  Activity,
  Database,
  HardDrive,
  Cloud,
  Server,
  Cpu,
  ChevronLeft,
  RefreshCw,
  CheckCircle2,
  XCircle,
  AlertCircle,
  Clock,
  Home,
  Info,
  Settings,
  BookOpen,
  Shield,
  Monitor,
} from 'lucide-react';
import { apiClient } from '@/lib/api-client';
import type { DetailedHealthResponse, ComponentHealth, HealthStatus } from '@/types/oscal';
import { HelpButton } from '@/components/HelpButton';

export default function HealthDashboardPage() {
  const router = useRouter();
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [health, setHealth] = useState<DetailedHealthResponse | null>(null);
  const [autoRefresh, setAutoRefresh] = useState(true);
  const [lastUpdated, setLastUpdated] = useState<Date | null>(null);

  const fetchHealth = useCallback(async (showRefreshIndicator = false) => {
    try {
      if (showRefreshIndicator) setRefreshing(true);
      setError(null);
      const data = await apiClient.getDetailedHealth();
      setHealth(data);
      setLastUpdated(new Date());
    } catch (err) {
      console.error('Failed to fetch health:', err);
      setError('Failed to load health status. Please try again.');
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
    fetchHealth();
  }, [router, fetchHealth]);

  useEffect(() => {
    if (!autoRefresh) return;
    const interval = setInterval(() => fetchHealth(), 30000);
    return () => clearInterval(interval);
  }, [autoRefresh, fetchHealth]);

  const getStatusIcon = (status: HealthStatus) => {
    switch (status) {
      case 'UP':
        return <CheckCircle2 className="h-5 w-5 text-green-500" />;
      case 'DOWN':
        return <XCircle className="h-5 w-5 text-red-500" />;
      case 'DEGRADED':
        return <AlertCircle className="h-5 w-5 text-yellow-500" />;
      default:
        return <AlertCircle className="h-5 w-5 text-gray-500" />;
    }
  };

  const getStatusBadgeClass = (status: HealthStatus) => {
    const base = 'px-3 py-1 rounded-full text-sm font-medium';
    switch (status) {
      case 'UP':
        return `${base} bg-green-100 text-green-700 dark:bg-green-900/30 dark:text-green-400`;
      case 'DOWN':
        return `${base} bg-red-100 text-red-700 dark:bg-red-900/30 dark:text-red-400`;
      case 'DEGRADED':
        return `${base} bg-yellow-100 text-yellow-700 dark:bg-yellow-900/30 dark:text-yellow-400`;
      default:
        return `${base} bg-gray-100 text-gray-700 dark:bg-gray-900/30 dark:text-gray-400`;
    }
  };

  const getComponentIcon = (componentName: string) => {
    switch (componentName.toLowerCase()) {
      case 'database':
        return <Database className="w-6 h-6" />;
      case 'storage':
        return <Cloud className="w-6 h-6" />;
      case 'memory':
        return <Cpu className="w-6 h-6" />;
      case 'cpu':
        return <Monitor className="w-6 h-6" />;
      case 'diskspace':
        return <HardDrive className="w-6 h-6" />;
      case 'oscallibrary':
        return <BookOpen className="w-6 h-6" />;
      case 'secrets':
        return <Shield className="w-6 h-6" />;
      default:
        return <Settings className="w-6 h-6" />;
    }
  };

  const formatTimestamp = (timestamp: string) => {
    return new Date(timestamp).toLocaleString();
  };

  if (loading) {
    return (
      <div className="min-h-screen flex items-center justify-center">
        <div className="text-center">
          <div className="animate-spin rounded-full h-16 w-16 border-b-2 border-cyan-600 mx-auto"></div>
          <p className="mt-4 text-gray-600 dark:text-gray-400">Loading health status...</p>
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
              className="flex items-center hover:text-gray-700 dark:hover:text-gray-200"
            >
              <Home className="w-4 h-4 mr-1" />
              Admin
            </button>
            <ChevronLeft className="w-4 h-4 rotate-180" />
            <span className="text-gray-900 dark:text-white font-medium">System Health</span>
          </nav>

          <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between">
            <div>
              <h1 className="text-3xl font-bold text-gray-900 dark:text-white flex items-center">
                <Activity className="w-8 h-8 mr-3 text-cyan-600" />
                System Health Dashboard
                <HelpButton slug="admin-health" />
              </h1>
              <p className="mt-2 text-gray-600 dark:text-gray-400">
                Monitor system components, resources, and service availability
              </p>
            </div>

            <div className="mt-4 sm:mt-0 flex items-center space-x-4">
              {/* Auto-refresh toggle */}
              <label className="flex items-center cursor-pointer">
                <input
                  type="checkbox"
                  checked={autoRefresh}
                  onChange={(e) => setAutoRefresh(e.target.checked)}
                  className="sr-only"
                />
                <div
                  className={`relative w-11 h-6 rounded-full transition-colors ${
                    autoRefresh ? 'bg-cyan-600' : 'bg-gray-300 dark:bg-gray-600'
                  }`}
                >
                  <div
                    className={`absolute top-0.5 left-0.5 w-5 h-5 bg-white rounded-full transition-transform ${
                      autoRefresh ? 'translate-x-5' : 'translate-x-0'
                    }`}
                  />
                </div>
                <span className="ml-2 text-sm text-gray-600 dark:text-gray-400">Auto-refresh</span>
              </label>

              {/* Refresh button */}
              <button
                onClick={() => fetchHealth(true)}
                disabled={refreshing}
                className="flex items-center px-4 py-2 bg-cyan-600 text-white rounded-lg hover:bg-cyan-700 disabled:opacity-50 transition-colors"
              >
                <RefreshCw className={`w-4 h-4 mr-2 ${refreshing ? 'animate-spin' : ''}`} />
                Refresh
              </button>
            </div>
          </div>

          {/* Last Updated */}
          {lastUpdated && (
            <div className="mt-2 flex items-center text-sm text-gray-500 dark:text-gray-400">
              <Clock className="w-4 h-4 mr-1" />
              Last updated: {lastUpdated.toLocaleTimeString()}
            </div>
          )}
        </div>

        {/* Error Message */}
        {error && (
          <div className="mb-6 bg-red-50 dark:bg-red-900/20 border border-red-200 dark:border-red-800 rounded-lg p-4">
            <div className="flex items-center">
              <XCircle className="w-5 h-5 text-red-500 mr-2" />
              <p className="text-red-700 dark:text-red-400">{error}</p>
            </div>
          </div>
        )}

        {health && (
          <>
            {/* Overall Status Banner */}
            <div
              className={`mb-8 rounded-lg p-6 ${
                health.status === 'UP'
                  ? 'bg-green-50 dark:bg-green-900/20 border border-green-200 dark:border-green-800'
                  : 'bg-red-50 dark:bg-red-900/20 border border-red-200 dark:border-red-800'
              }`}
            >
              <div className="flex items-center justify-between">
                <div className="flex items-center">
                  {health.status === 'UP' ? (
                    <CheckCircle2 className="w-12 h-12 text-green-500 mr-4" />
                  ) : (
                    <XCircle className="w-12 h-12 text-red-500 mr-4" />
                  )}
                  <div>
                    <h2 className="text-2xl font-bold text-gray-900 dark:text-white">
                      System Status: {health.status}
                    </h2>
                    <p className="text-gray-600 dark:text-gray-400">
                      {health.status === 'UP'
                        ? 'All systems operational'
                        : 'Some components may be experiencing issues'}
                    </p>
                  </div>
                </div>
                <span className={getStatusBadgeClass(health.status as HealthStatus)}>
                  {health.status}
                </span>
              </div>
            </div>

            {/* Application Info Card */}
            <div className="mb-8 bg-white dark:bg-gray-800 rounded-lg shadow-md p-6 border border-gray-200 dark:border-gray-700">
              <h3 className="text-lg font-semibold text-gray-900 dark:text-white mb-4 flex items-center">
                <Info className="w-5 h-5 mr-2 text-cyan-600" />
                Application Information
              </h3>
              <div className="grid grid-cols-2 md:grid-cols-3 gap-4">
                <div>
                  <p className="text-sm text-gray-500 dark:text-gray-400">Name</p>
                  <p className="font-medium text-gray-900 dark:text-white">{health.application.name}</p>
                </div>
                <div>
                  <p className="text-sm text-gray-500 dark:text-gray-400">Version</p>
                  <p className="font-medium text-gray-900 dark:text-white">{health.application.version}</p>
                </div>
                <div>
                  <p className="text-sm text-gray-500 dark:text-gray-400">Started</p>
                  <p className="font-medium text-gray-900 dark:text-white">{formatTimestamp(health.application.startTime)}</p>
                </div>
              </div>
            </div>

            {/* Component Health Cards */}
            <div className="mb-8">
              <h3 className="text-lg font-semibold text-gray-900 dark:text-white mb-4">
                Component Health
              </h3>
              <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
                {Object.entries(health.components).map(([name, component]) => (
                  <ComponentHealthCard
                    key={name}
                    name={name}
                    component={component}
                    icon={getComponentIcon(name)}
                    statusIcon={getStatusIcon(component.status as HealthStatus)}
                    badgeClass={getStatusBadgeClass(component.status as HealthStatus)}
                  />
                ))}
              </div>
            </div>

            {/* System Resources */}
            <div className="grid grid-cols-1 lg:grid-cols-2 gap-8 mb-8">
              {/* Memory Usage */}
              <div className="bg-white dark:bg-gray-800 rounded-lg shadow-md p-6 border border-gray-200 dark:border-gray-700">
                <h3 className="text-lg font-semibold text-gray-900 dark:text-white mb-4 flex items-center">
                  <Cpu className="w-5 h-5 mr-2 text-cyan-600" />
                  Memory Usage
                </h3>
                <div className="space-y-4">
                  <div>
                    <div className="flex justify-between text-sm mb-1">
                      <span className="text-gray-600 dark:text-gray-400">Heap Memory</span>
                      <span className="text-gray-900 dark:text-white font-medium">
                        {health.system.usedMemoryMb} MB / {health.system.totalMemoryMb} MB
                      </span>
                    </div>
                    <div className="w-full bg-gray-200 dark:bg-gray-700 rounded-full h-3">
                      <div
                        className={`h-3 rounded-full transition-all ${
                          health.system.memoryUsagePercent >= 90
                            ? 'bg-red-500'
                            : health.system.memoryUsagePercent >= 70
                            ? 'bg-yellow-500'
                            : 'bg-green-500'
                        }`}
                        style={{ width: `${health.system.memoryUsagePercent}%` }}
                      />
                    </div>
                    <p className="text-sm text-gray-500 dark:text-gray-400 mt-1">
                      {health.system.memoryUsagePercent}% used ({health.system.freeMemoryMb} MB free)
                    </p>
                  </div>
                </div>
              </div>

              {/* Disk Usage */}
              <div className="bg-white dark:bg-gray-800 rounded-lg shadow-md p-6 border border-gray-200 dark:border-gray-700">
                <h3 className="text-lg font-semibold text-gray-900 dark:text-white mb-4 flex items-center">
                  <HardDrive className="w-5 h-5 mr-2 text-cyan-600" />
                  Disk Usage
                </h3>
                <div className="space-y-4">
                  <div>
                    <div className="flex justify-between text-sm mb-1">
                      <span className="text-gray-600 dark:text-gray-400">Disk Space</span>
                      <span className="text-gray-900 dark:text-white font-medium">
                        {health.system.totalDiskSpaceGb - health.system.freeDiskSpaceGb} GB / {health.system.totalDiskSpaceGb} GB
                      </span>
                    </div>
                    <div className="w-full bg-gray-200 dark:bg-gray-700 rounded-full h-3">
                      <div
                        className={`h-3 rounded-full transition-all ${
                          health.system.diskUsagePercent >= 90
                            ? 'bg-red-500'
                            : health.system.diskUsagePercent >= 70
                            ? 'bg-yellow-500'
                            : 'bg-green-500'
                        }`}
                        style={{ width: `${health.system.diskUsagePercent}%` }}
                      />
                    </div>
                    <p className="text-sm text-gray-500 dark:text-gray-400 mt-1">
                      {health.system.diskUsagePercent}% used ({health.system.freeDiskSpaceGb} GB free)
                    </p>
                  </div>
                </div>
              </div>
            </div>

            {/* System Info */}
            <div className="grid grid-cols-1 lg:grid-cols-2 gap-8">
              {/* CPU/System Info */}
              <div className="bg-white dark:bg-gray-800 rounded-lg shadow-md p-6 border border-gray-200 dark:border-gray-700">
                <h3 className="text-lg font-semibold text-gray-900 dark:text-white mb-4 flex items-center">
                  <Server className="w-5 h-5 mr-2 text-cyan-600" />
                  System Resources
                </h3>
                <div className="space-y-3">
                  <div className="flex justify-between">
                    <span className="text-gray-600 dark:text-gray-400">Available Processors</span>
                    <span className="font-medium text-gray-900 dark:text-white">{health.system.availableProcessors}</span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-gray-600 dark:text-gray-400">System Load Average</span>
                    <span className="font-medium text-gray-900 dark:text-white">
                      {health.system.systemLoadAverage >= 0 ? health.system.systemLoadAverage.toFixed(2) : 'N/A'}
                    </span>
                  </div>
                </div>
              </div>

              {/* Environment Info */}
              <div className="bg-white dark:bg-gray-800 rounded-lg shadow-md p-6 border border-gray-200 dark:border-gray-700">
                <h3 className="text-lg font-semibold text-gray-900 dark:text-white mb-4 flex items-center">
                  <Settings className="w-5 h-5 mr-2 text-cyan-600" />
                  Environment
                </h3>
                <div className="space-y-3">
                  <div className="flex justify-between">
                    <span className="text-gray-600 dark:text-gray-400">Java Version</span>
                    <span className="font-medium text-gray-900 dark:text-white">{health.environment.javaVersion}</span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-gray-600 dark:text-gray-400">OS</span>
                    <span className="font-medium text-gray-900 dark:text-white">{health.environment.osName}</span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-gray-600 dark:text-gray-400">Timezone</span>
                    <span className="font-medium text-gray-900 dark:text-white">{health.environment.timezone}</span>
                  </div>
                </div>
              </div>
            </div>
          </>
        )}
      </div>
    </div>
  );
}

// Component Health Card Component
function ComponentHealthCard({
  name,
  component,
  icon,
  statusIcon,
  badgeClass,
}: {
  name: string;
  component: ComponentHealth;
  icon: React.ReactNode;
  statusIcon: React.ReactNode;
  badgeClass: string;
}) {
  const formatName = (name: string) => {
    // Convert camelCase to Title Case
    return name
      .replace(/([A-Z])/g, ' $1')
      .replace(/^./, (str) => str.toUpperCase())
      .trim();
  };

  return (
    <div className="bg-white dark:bg-gray-800 rounded-lg shadow-md p-5 border border-gray-200 dark:border-gray-700">
      <div className="flex items-start justify-between mb-3">
        <div className="flex items-center">
          <div className="p-2 bg-cyan-100 dark:bg-cyan-900/30 rounded-lg mr-3 text-cyan-600 dark:text-cyan-400">
            {icon}
          </div>
          <div>
            <h4 className="font-semibold text-gray-900 dark:text-white">{formatName(name)}</h4>
            {component.responseTimeMs !== undefined && (
              <p className="text-xs text-gray-500 dark:text-gray-400">{component.responseTimeMs}ms</p>
            )}
          </div>
        </div>
        {statusIcon}
      </div>

      <div className="mb-3">
        <span className={badgeClass}>{component.status}</span>
      </div>

      {component.message && (
        <p className="text-sm text-gray-600 dark:text-gray-400 mb-2">{component.message}</p>
      )}

      {component.details && Object.keys(component.details).length > 0 && (
        <div className="mt-3 pt-3 border-t border-gray-200 dark:border-gray-700">
          <p className="text-xs text-gray-500 dark:text-gray-400 mb-2">Details:</p>
          <div className="space-y-1">
            {Object.entries(component.details).slice(0, 3).map(([key, value]) => (
              <div key={key} className="flex justify-between text-xs">
                <span className="text-gray-500 dark:text-gray-400">{key}</span>
                <span className="text-gray-900 dark:text-white font-medium truncate ml-2">
                  {String(value)}
                </span>
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  );
}
