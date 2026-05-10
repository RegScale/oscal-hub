'use client';

import { useState } from 'react';
import { Card } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { ChevronDown, ChevronUp } from 'lucide-react';
import type { ConMonReconciliationCounts, ConMonReconciliationDetail } from '@/types/oscal';

interface Props {
  counts: ConMonReconciliationCounts;
  previousSnapshotDate: string | null;
  onLoadDetail: () => Promise<ConMonReconciliationDetail>;
}

export function ReconciliationBanner({ counts, previousSnapshotDate, onLoadDetail }: Props) {
  const [expanded, setExpanded] = useState(false);
  const [detail, setDetail] = useState<ConMonReconciliationDetail | null>(null);

  const handleToggle = async () => {
    if (!expanded && !detail) {
      try {
        setDetail(await onLoadDetail());
      } catch {
        // surfaced as toast upstream
      }
    }
    setExpanded(!expanded);
  };

  return (
    <Card className="p-4">
      <div className="flex items-center justify-between gap-3">
        <div>
          <h3 className="text-sm font-semibold">Since last snapshot</h3>
          <p className="text-xs text-muted-foreground">
            {previousSnapshotDate ? `Previous upload: ${new Date(previousSnapshotDate).toLocaleDateString()}` : ''}
          </p>
          <p className="mt-1 text-sm">
            <span className="font-medium">{counts.newCount}</span> new
            {' · '}
            <span className="font-medium text-green-600">{counts.closedCount}</span> closed
            {' · '}
            <span className="font-medium text-amber-600">{counts.reopenedCount}</span> reopened
            {' · '}
            <span className="font-medium">{counts.stillOpenCount}</span> still open
            {counts.removedCount > 0 && (
              <> {' · '} <span className="font-medium text-destructive">{counts.removedCount}</span> removed</>
            )}
            {counts.changedCount > 0 && (
              <> {' · '} <span className="font-medium">{counts.changedCount}</span> changed</>
            )}
          </p>
        </div>
        <Button variant="ghost" size="sm" onClick={handleToggle}>
          {expanded ? <ChevronUp className="h-4 w-4" /> : <ChevronDown className="h-4 w-4" />}
          {expanded ? 'Hide details' : 'Show details'}
        </Button>
      </div>

      {expanded && detail && (
        <div className="mt-4 grid grid-cols-1 gap-3 md:grid-cols-2">
          <DetailList title="New" items={detail.newItems.map((i) => i.title)} />
          <DetailList title="Newly closed" items={detail.newlyClosedItems.map((i) => i.title)} />
          <DetailList title="Reopened" items={detail.reopenedItems.map((i) => i.title)} />
          <DetailList title="Removed" items={detail.removedItems.map((i) => i.title)} />
          <DetailList
            title="Changed"
            items={detail.changedItems.map((c) => `${c.current.title} (${c.fieldsChanged.join(', ')})`)}
          />
        </div>
      )}
    </Card>
  );
}

function DetailList({ title, items }: { title: string; items: string[] }) {
  if (items.length === 0) return null;
  return (
    <div className="rounded-md border p-2">
      <div className="mb-1 text-xs font-semibold uppercase text-muted-foreground">{title}</div>
      <ul className="space-y-0.5 text-xs">
        {items.slice(0, 10).map((s, i) => <li key={i} className="truncate">{s}</li>)}
        {items.length > 10 && <li className="text-muted-foreground">…and {items.length - 10} more</li>}
      </ul>
    </div>
  );
}
