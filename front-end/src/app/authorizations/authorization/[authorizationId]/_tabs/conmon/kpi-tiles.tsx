'use client';

import { Card } from '@/components/ui/card';
import type { ConMonSnapshotSummary, ConMonAnalytics } from '@/types/oscal';

interface Props {
  latest: ConMonSnapshotSummary | null;
  analytics: ConMonAnalytics | null;
}

export function KpiTiles({ latest, analytics }: Props) {
  if (!latest) {
    return (
      <Card className="p-4 text-center text-sm text-muted-foreground">
        No snapshots yet. Upload one to see open/closed counts.
      </Card>
    );
  }

  const slaPct = analytics?.slaStats?.slaPercent;
  const overdue = analytics?.slaStats?.overdue ?? 0;
  const noDeadline = analytics?.slaStats?.withoutDeadline ?? 0;

  const tiles = [
    { label: 'Open', value: latest.openCount, color: 'text-amber-600' },
    { label: 'Closed', value: latest.closedCount, color: 'text-green-600' },
    { label: 'Unknown', value: latest.unknownCount, color: 'text-muted-foreground' },
    {
      label: 'Within SLA',
      value: slaPct == null ? '—' : `${slaPct.toFixed(0)}%`,
      color: slaPct == null ? '' : (slaPct >= 80 ? 'text-green-600' : slaPct >= 50 ? 'text-amber-600' : 'text-destructive'),
    },
    {
      label: 'Overdue',
      value: overdue,
      color: overdue > 0 ? 'text-destructive' : '',
    },
    {
      label: 'No deadline',
      value: noDeadline,
      color: noDeadline > 0 ? 'text-muted-foreground' : '',
    },
  ];

  return (
    <div className="grid grid-cols-2 gap-3 md:grid-cols-3 xl:grid-cols-6">
      {tiles.map((t) => (
        <Card key={t.label} className="p-4">
          <div className="text-xs uppercase text-muted-foreground">{t.label}</div>
          <div className={`mt-1 text-2xl font-semibold ${t.color}`}>{t.value}</div>
        </Card>
      ))}
    </div>
  );
}
