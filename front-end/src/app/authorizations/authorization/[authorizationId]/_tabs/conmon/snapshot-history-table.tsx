'use client';

import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table';
import { Download, Eye, Trash2 } from 'lucide-react';
import type { ConMonSnapshotSummary } from '@/types/oscal';

interface Props {
  snapshots: ConMonSnapshotSummary[];
  canDelete: (s: ConMonSnapshotSummary) => boolean;
  onView: (s: ConMonSnapshotSummary) => void;
  onDownload: (s: ConMonSnapshotSummary) => void;
  onDelete: (s: ConMonSnapshotSummary) => void;
}

export function SnapshotHistoryTable({ snapshots, canDelete, onView, onDownload, onDelete }: Props) {
  if (snapshots.length === 0) {
    return <p className="py-8 text-center text-sm text-muted-foreground">No snapshots yet.</p>;
  }
  return (
    <Table>
      <TableHeader>
        <TableRow>
          <TableHead>Uploaded</TableHead>
          <TableHead>By</TableHead>
          <TableHead>Format</TableHead>
          <TableHead className="text-right">Open</TableHead>
          <TableHead className="text-right">Closed</TableHead>
          <TableHead className="text-right">Unknown</TableHead>
          <TableHead>Reconciliation</TableHead>
          <TableHead className="text-right">Actions</TableHead>
        </TableRow>
      </TableHeader>
      <TableBody>
        {snapshots.map((s) => (
          <TableRow key={s.id}>
            <TableCell className="text-sm">{new Date(s.uploadedAt).toLocaleString()}</TableCell>
            <TableCell className="text-sm text-muted-foreground">{s.uploadedByUsername ?? '—'}</TableCell>
            <TableCell><Badge variant="secondary">{s.sourceFormat.replace('_', ' ')}</Badge></TableCell>
            <TableCell className="text-right">{s.openCount}</TableCell>
            <TableCell className="text-right">{s.closedCount}</TableCell>
            <TableCell className="text-right">{s.unknownCount}</TableCell>
            <TableCell className="text-xs text-muted-foreground">
              {s.reconciliation
                ? `+${s.reconciliation.newCount} new, -${s.reconciliation.closedCount} closed, ${s.reconciliation.reopenedCount} reopened`
                : '—'}
            </TableCell>
            <TableCell className="text-right">
              <div className="flex justify-end gap-1">
                <Button variant="ghost" size="icon" onClick={() => onView(s)} aria-label="View items">
                  <Eye className="h-4 w-4" />
                </Button>
                <Button variant="ghost" size="icon" onClick={() => onDownload(s)} aria-label="Download original">
                  <Download className="h-4 w-4" />
                </Button>
                {canDelete(s) && (
                  <Button variant="ghost" size="icon" onClick={() => onDelete(s)} aria-label="Delete snapshot">
                    <Trash2 className="h-4 w-4" />
                  </Button>
                )}
              </div>
            </TableCell>
          </TableRow>
        ))}
      </TableBody>
    </Table>
  );
}
