'use client';

import { Card } from '@/components/ui/card';
import type { ConMonSnapshotSummary } from '@/types/oscal';

interface Props {
  latest: ConMonSnapshotSummary | null;
}

export function KpiTiles({ latest }: Props) {
  if (!latest) {
    return (
      <Card className="p-4 text-center text-sm text-muted-foreground">
        No snapshots yet. Upload one to see open/closed counts.
      </Card>
    );
  }

  const tiles = [
    { label: 'Open', value: latest.openCount, color: 'text-amber-600' },
    { label: 'Closed', value: latest.closedCount, color: 'text-green-600' },
    { label: 'Unknown', value: latest.unknownCount, color: 'text-muted-foreground' },
    { label: 'Last snapshot', value: new Date(latest.uploadedAt).toLocaleDateString(), color: '' },
  ];

  return (
    <div className="grid grid-cols-2 gap-3 md:grid-cols-4">
      {tiles.map((t) => (
        <Card key={t.label} className="p-4">
          <div className="text-xs uppercase text-muted-foreground">{t.label}</div>
          <div className={`mt-1 text-2xl font-semibold ${t.color}`}>{t.value}</div>
        </Card>
      ))}
    </div>
  );
}
