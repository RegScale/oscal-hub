'use client';

import { useEffect, useState } from 'react';
import { Card } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';
import { Plus, Loader2 } from 'lucide-react';
import { toast } from 'sonner';
import { apiClient } from '@/lib/api-client';
import { KpiTiles } from './conmon/kpi-tiles';
import { ReconciliationBanner } from './conmon/reconciliation-banner';
import { AnalyticsDashboard } from './conmon/analytics-dashboard';
import { SnapshotHistoryTable } from './conmon/snapshot-history-table';
import { UploadSnapshotDialog } from './conmon/upload-snapshot-dialog';
import { PoamItemsTable } from './conmon/poam-items-table';
import type { AuthorizationResponse, ConMonAnalytics, ConMonSnapshotSummary } from '@/types/oscal';

interface Props {
  authorization: AuthorizationResponse;
}

export function ContinuousMonitoringTab({ authorization }: Props) {
  const [snapshots, setSnapshots] = useState<ConMonSnapshotSummary[]>([]);
  const [analytics, setAnalytics] = useState<ConMonAnalytics | null>(null);
  const [loading, setLoading] = useState(true);
  const [analyticsLoading, setAnalyticsLoading] = useState(true);
  const [uploadOpen, setUploadOpen] = useState(false);
  const [activeSubtab, setActiveSubtab] = useState<'analytics' | 'open' | 'overdue' | 'closed'>('analytics');

  const role = authorization.effectiveRole;
  const canUpload = role === 'OWNER' || role === 'EDITOR' || role === 'CONTRIBUTOR';

  const refresh = async () => {
    setLoading(true);
    try {
      setSnapshots(await apiClient.listConMonSnapshots(authorization.id));
    } catch {
      toast.error('Failed to load snapshots');
    } finally {
      setLoading(false);
    }
  };

  const refreshAnalytics = async () => {
    setAnalyticsLoading(true);
    try {
      setAnalytics(await apiClient.getConMonAnalytics(authorization.id));
    } catch {
      // non-fatal
    } finally {
      setAnalyticsLoading(false);
    }
  };

  useEffect(() => { void refresh(); /* eslint-disable-next-line react-hooks/exhaustive-deps */ }, [authorization.id]);
  useEffect(() => { void refreshAnalytics(); /* eslint-disable-next-line react-hooks/exhaustive-deps */ }, [authorization.id, snapshots.length]);

  const latest = snapshots[0] ?? null;
  const previousDate = (() => {
    if (!latest?.reconciliation) return null;
    const prev = snapshots.find((s) => s.id === latest.reconciliation?.previousSnapshotId);
    return prev?.uploadedAt ?? null;
  })();
  const latestSnapshotId = latest?.id ?? null;

  const handleDownload = async (s: ConMonSnapshotSummary) => {
    try {
      const blob = await apiClient.downloadConMonSnapshot(authorization.id, s.id);
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url; a.download = s.originalFilename; a.click();
      URL.revokeObjectURL(url);
    } catch { toast.error('Download failed'); }
  };

  const handleDelete = async (s: ConMonSnapshotSummary) => {
    if (!confirm(`Delete snapshot uploaded ${new Date(s.uploadedAt).toLocaleString()}? This cannot be undone.`)) return;
    try {
      await apiClient.deleteConMonSnapshot(authorization.id, s.id);
      toast.success('Snapshot deleted');
      await refresh();
    } catch { toast.error('Delete failed'); }
  };

  const canDeleteSnapshot = (s: ConMonSnapshotSummary) => {
    if (role === 'OWNER' || role === 'EDITOR') return true;
    if (role === 'CONTRIBUTOR') return s.uploadedByUsername === currentUsername();
    return false;
  };

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between gap-2">
        <h2 className="text-lg font-semibold">Continuous Monitoring</h2>
        {canUpload && (
          <Button onClick={() => setUploadOpen(true)}>
            <Plus className="mr-1 h-4 w-4" />
            Upload snapshot
          </Button>
        )}
      </div>

      <KpiTiles latest={latest} analytics={analytics} />

      {latest?.reconciliation && (
        <ReconciliationBanner
          counts={latest.reconciliation}
          previousSnapshotDate={previousDate}
          onLoadDetail={() => apiClient.getConMonReconciliation(authorization.id, latest.id)}
        />
      )}

      <Tabs value={activeSubtab} onValueChange={(v) => setActiveSubtab(v as 'analytics' | 'open' | 'overdue' | 'closed')}>
        <TabsList>
          <TabsTrigger value="analytics">Analytics</TabsTrigger>
          <TabsTrigger value="open">Open</TabsTrigger>
          <TabsTrigger value="overdue">Overdue</TabsTrigger>
          <TabsTrigger value="closed">Closed</TabsTrigger>
        </TabsList>

        <TabsContent value="analytics" className="mt-4 space-y-4">
          <AnalyticsDashboard analytics={analytics} loading={analyticsLoading} />
          <Card className="p-4">
            <h3 className="mb-2 text-sm font-semibold">Snapshot history</h3>
            {loading ? (
              <div className="flex items-center justify-center py-6 text-sm text-muted-foreground">
                <Loader2 className="mr-2 h-4 w-4 animate-spin" /> Loading…
              </div>
            ) : (
              <SnapshotHistoryTable
                snapshots={snapshots}
                canDelete={canDeleteSnapshot}
                onDownload={(s) => void handleDownload(s)}
                onDelete={(s) => void handleDelete(s)}
              />
            )}
          </Card>
        </TabsContent>

        <TabsContent value="open" className="mt-4">
          <PoamItemsTable
            authorizationId={authorization.id}
            snapshotId={latestSnapshotId}
            status="OPEN"
            emptyMessage="No open items in the latest snapshot."
          />
        </TabsContent>

        <TabsContent value="overdue" className="mt-4">
          <PoamItemsTable
            authorizationId={authorization.id}
            snapshotId={latestSnapshotId}
            overdue={true}
            emptyMessage="No overdue items — all open POAMs are within their scheduled completion date."
          />
        </TabsContent>

        <TabsContent value="closed" className="mt-4">
          <PoamItemsTable
            authorizationId={authorization.id}
            snapshotId={latestSnapshotId}
            status="CLOSED"
            emptyMessage="No closed items in the latest snapshot."
          />
        </TabsContent>
      </Tabs>

      <UploadSnapshotDialog
        authorizationId={authorization.id}
        open={uploadOpen}
        onOpenChange={setUploadOpen}
        onUploaded={refresh}
      />
    </div>
  );
}

function currentUsername(): string | null {
  if (typeof localStorage === 'undefined') return null;
  try {
    const raw = localStorage.getItem('user');
    if (!raw) return null;
    return JSON.parse(raw).username ?? null;
  } catch { return null; }
}
