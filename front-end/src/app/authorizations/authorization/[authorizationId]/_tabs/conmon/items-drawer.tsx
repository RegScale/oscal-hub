'use client';

import { useEffect, useState } from 'react';
import { Dialog, DialogContent, DialogHeader, DialogTitle } from '@/components/ui/dialog';
import { Input } from '@/components/ui/input';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { Badge } from '@/components/ui/badge';
import { apiClient } from '@/lib/api-client';
import type { ConMonItemStatus, ConMonPoamItem } from '@/types/oscal';

interface Props {
  authorizationId: number;
  snapshotId: number | null;
  onClose: () => void;
}

export function ItemsDrawer({ authorizationId, snapshotId, onClose }: Props) {
  const [status, setStatus] = useState<ConMonItemStatus | 'ALL'>('ALL');
  const [q, setQ] = useState('');
  const [items, setItems] = useState<ConMonPoamItem[]>([]);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    if (!snapshotId) return;
    setLoading(true);
    apiClient.listConMonItems(authorizationId, snapshotId, {
      status: status === 'ALL' ? undefined : status,
      q: q || undefined,
      size: 100,
    }).then((r) => setItems(r.items)).finally(() => setLoading(false));
  }, [authorizationId, snapshotId, status, q]);

  return (
    <Dialog open={snapshotId !== null} onOpenChange={(v) => { if (!v) onClose(); }}>
      <DialogContent className="max-w-4xl">
        <DialogHeader><DialogTitle>POAM items</DialogTitle></DialogHeader>

        <div className="mb-3 flex flex-wrap items-center gap-2">
          <Select value={status} onValueChange={(v) => setStatus(v as any)}>
            <SelectTrigger className="w-40"><SelectValue /></SelectTrigger>
            <SelectContent>
              <SelectItem value="ALL">All statuses</SelectItem>
              <SelectItem value="OPEN">Open</SelectItem>
              <SelectItem value="CLOSED">Closed</SelectItem>
              <SelectItem value="UNKNOWN">Unknown</SelectItem>
            </SelectContent>
          </Select>
          <Input className="max-w-xs" placeholder="Search title or ID" value={q}
                 onChange={(e) => setQ(e.target.value)} />
        </div>

        {loading ? <p className="py-6 text-center text-sm text-muted-foreground">Loading…</p>
         : items.length === 0 ? <p className="py-6 text-center text-sm text-muted-foreground">No items match.</p>
         : (
          <div className="max-h-[60vh] overflow-y-auto divide-y">
            {items.map((it) => (
              <div key={it.id} className="py-2">
                <div className="flex items-center justify-between gap-2">
                  <div className="font-medium text-sm">{it.title}</div>
                  <div className="flex gap-1">
                    <Badge variant={it.status === 'OPEN' ? 'destructive' : 'secondary'}>{it.status}</Badge>
                    {it.severity && <Badge variant="outline">{it.severity}</Badge>}
                  </div>
                </div>
                <div className="text-xs text-muted-foreground">{it.externalId}</div>
                {it.description && <div className="mt-1 text-xs text-muted-foreground line-clamp-2">{it.description}</div>}
              </div>
            ))}
          </div>
        )}
      </DialogContent>
    </Dialog>
  );
}
