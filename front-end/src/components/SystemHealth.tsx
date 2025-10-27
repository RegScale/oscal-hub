'use client';

import { useEffect, useState } from 'react';
import { Card, CardHeader, CardTitle, CardContent } from '@/components/ui/card';
import { Activity, Database, HardDrive, Cloud, Server, CheckCircle2, XCircle, AlertCircle, Loader2 } from 'lucide-react';

interface HealthComponent {
  status: 'UP' | 'DOWN' | 'UNKNOWN';
  details?: Record<string, unknown>;
}

interface HealthResponse {
  status: 'UP' | 'DOWN';
  components?: {
    db?: HealthComponent;
    diskSpace?: HealthComponent;
    azureBlobStorage?: HealthComponent;
    ping?: HealthComponent;
  };
}

export function SystemHealth() {
  const [health, setHealth] = useState<HealthResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [lastUpdate, setLastUpdate] = useState<Date>(new Date());

  const fetchHealth = async () => {
    try {
      const response = await fetch(`${process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080/api'}/actuator/health`.replace('/api/actuator', '/actuator'));

      if (!response.ok) {
        throw new Error(`HTTP ${response.status}`);
      }

      const data: HealthResponse = await response.json();
      setHealth(data);
      setError(null);
      setLastUpdate(new Date());
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to fetch health status');
      setHealth({ status: 'DOWN' });
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchHealth();

    // Auto-refresh every 30 seconds
    const interval = setInterval(fetchHealth, 30000);

    return () => clearInterval(interval);
  }, []);

  const getStatusIcon = (status: 'UP' | 'DOWN' | 'UNKNOWN') => {
    switch (status) {
      case 'UP':
        return <CheckCircle2 className="h-5 w-5 text-green-500" />;
      case 'DOWN':
        return <XCircle className="h-5 w-5 text-red-500" />;
      default:
        return <AlertCircle className="h-5 w-5 text-yellow-500" />;
    }
  };

  const getStatusColor = (status: 'UP' | 'DOWN' | 'UNKNOWN') => {
    switch (status) {
      case 'UP':
        return 'text-green-500';
      case 'DOWN':
        return 'text-red-500';
      default:
        return 'text-yellow-500';
    }
  };

  const getStatusBadge = (status: 'UP' | 'DOWN' | 'UNKNOWN') => {
    const baseClasses = "inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-sm font-medium";

    switch (status) {
      case 'UP':
        return `${baseClasses} bg-green-100 text-green-700 dark:bg-green-900/20 dark:text-green-400`;
      case 'DOWN':
        return `${baseClasses} bg-red-100 text-red-700 dark:bg-red-900/20 dark:text-red-400`;
      default:
        return `${baseClasses} bg-yellow-100 text-yellow-700 dark:bg-yellow-900/20 dark:text-yellow-400`;
    }
  };

  if (loading) {
    return (
      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <Activity className="h-5 w-5 text-primary" />
            System Health
          </CardTitle>
        </CardHeader>
        <CardContent>
          <div className="flex items-center justify-center py-8">
            <Loader2 className="h-8 w-8 animate-spin text-muted-foreground" />
          </div>
        </CardContent>
      </Card>
    );
  }

  return (
    <Card>
      <CardHeader>
        <CardTitle className="flex items-center justify-between">
          <span className="flex items-center gap-2">
            <Activity className="h-5 w-5 text-primary" />
            System Health
          </span>
          <span className={getStatusBadge(health?.status || 'UNKNOWN')}>
            {getStatusIcon(health?.status || 'UNKNOWN')}
            {health?.status || 'UNKNOWN'}
          </span>
        </CardTitle>
      </CardHeader>
      <CardContent>
        {error && (
          <div className="mb-4 p-3 bg-red-50 dark:bg-red-900/20 border border-red-200 dark:border-red-800 rounded-lg">
            <p className="text-sm text-red-600 dark:text-red-400">
              Unable to connect to backend: {error}
            </p>
          </div>
        )}

        <div className="space-y-3">
          {/* Overall Status */}
          <div className="flex items-center justify-between p-3 rounded-lg bg-muted/50">
            <div className="flex items-center gap-3">
              <Server className="h-5 w-5 text-muted-foreground" />
              <span className="font-medium">Application</span>
            </div>
            <div className="flex items-center gap-2">
              {getStatusIcon(health?.status || 'UNKNOWN')}
              <span className={`font-semibold ${getStatusColor(health?.status || 'UNKNOWN')}`}>
                {health?.status || 'UNKNOWN'}
              </span>
            </div>
          </div>

          {/* Database Status */}
          {health?.components?.db && (
            <div className="flex items-center justify-between p-3 rounded-lg bg-muted/50">
              <div className="flex items-center gap-3">
                <Database className="h-5 w-5 text-muted-foreground" />
                <span className="font-medium">Database</span>
              </div>
              <div className="flex items-center gap-2">
                {getStatusIcon(health.components.db.status)}
                <span className={`font-semibold ${getStatusColor(health.components.db.status)}`}>
                  {health.components.db.status}
                </span>
              </div>
            </div>
          )}

          {/* Disk Space Status */}
          {health?.components?.diskSpace && (
            <div className="flex items-center justify-between p-3 rounded-lg bg-muted/50">
              <div className="flex items-center gap-3">
                <HardDrive className="h-5 w-5 text-muted-foreground" />
                <span className="font-medium">Disk Space</span>
              </div>
              <div className="flex items-center gap-2">
                {getStatusIcon(health.components.diskSpace.status)}
                <span className={`font-semibold ${getStatusColor(health.components.diskSpace.status)}`}>
                  {health.components.diskSpace.status}
                </span>
                {(typeof health.components.diskSpace.details?.free === 'number') && (
                  <span className="text-xs text-muted-foreground ml-2">
                    {Math.round((health.components.diskSpace.details.free as number) / 1024 / 1024 / 1024)}GB free
                  </span>
                )}
              </div>
            </div>
          )}

          {/* Azure Blob Storage Status */}
          {health?.components?.azureBlobStorage && (
            <div className="flex items-center justify-between p-3 rounded-lg bg-muted/50">
              <div className="flex items-center gap-3">
                <Cloud className="h-5 w-5 text-muted-foreground" />
                <span className="font-medium">Cloud Storage</span>
              </div>
              <div className="flex items-center gap-2">
                {getStatusIcon(health.components.azureBlobStorage.status)}
                <span className={`font-semibold ${getStatusColor(health.components.azureBlobStorage.status)}`}>
                  {health.components.azureBlobStorage.status}
                </span>
                {(typeof health.components.azureBlobStorage.details?.storage === 'string') && (
                  <span className="text-xs text-muted-foreground ml-2">
                    ({health.components.azureBlobStorage.details.storage as string})
                  </span>
                )}
              </div>
            </div>
          )}
        </div>

        {/* Last Updated */}
        <div className="mt-4 pt-3 border-t border-border">
          <p className="text-xs text-muted-foreground text-center">
            Last updated: {lastUpdate.toLocaleTimeString()} · Auto-refreshes every 30s
          </p>
        </div>
      </CardContent>
    </Card>
  );
}
