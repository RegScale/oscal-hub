'use client';

import { useEffect, useState } from 'react';
import { Card } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table';
import { ChevronLeft, ChevronRight, Loader2, AlertTriangle } from 'lucide-react';
import { apiClient } from '@/lib/api-client';
import type { ConMonItemStatus, ConMonPoamItem } from '@/types/oscal';

interface Props {
  authorizationId: number;
  snapshotId: number | null;
  status?: ConMonItemStatus;
  overdue?: boolean;
  emptyMessage?: string;
  pageSize?: number;
}

export function PoamItemsTable({
  authorizationId,
  snapshotId,
  status,
  overdue,
  emptyMessage,
  pageSize = 25,
}: Props) {
  const [items, setItems] = useState<ConMonPoamItem[]>([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [loading, setLoading] = useState(true);
  const [searchQuery, setSearchQuery] = useState('');
  const [searchInput, setSearchInput] = useState('');

  useEffect(() => {
    if (!snapshotId) {
      setItems([]);
      setTotalElements(0);
      setTotalPages(0);
      setLoading(false);
      return;
    }
    setLoading(true);
    apiClient.listConMonItems(authorizationId, snapshotId, {
      status,
      overdue,
      q: searchQuery || undefined,
      page,
      size: pageSize,
    })
      .then((r) => {
        setItems(r.items);
        setTotalPages(r.totalPages);
        setTotalElements(r.totalElements);
      })
      .finally(() => setLoading(false));
  }, [authorizationId, snapshotId, status, overdue, searchQuery, page, pageSize]);

  // Reset to first page when filters change
  useEffect(() => { setPage(0); }, [snapshotId, status, overdue, searchQuery]);

  const handleSearch = (e: React.FormEvent) => {
    e.preventDefault();
    setSearchQuery(searchInput.trim());
  };

  const today = new Date();
  today.setHours(0, 0, 0, 0);

  if (!snapshotId) {
    return <Card className="p-6 text-center text-sm text-muted-foreground">Upload a snapshot to see POAM items.</Card>;
  }

  return (
    <Card className="p-4">
      <form onSubmit={handleSearch} className="mb-3 flex items-center gap-2">
        <Input
          className="max-w-xs"
          placeholder="Search title or external ID…"
          value={searchInput}
          onChange={(e) => setSearchInput(e.target.value)}
        />
        <Button type="submit" variant="outline" size="sm">Search</Button>
        {searchQuery && (
          <Button
            type="button"
            variant="ghost"
            size="sm"
            onClick={() => { setSearchInput(''); setSearchQuery(''); }}
          >
            Clear
          </Button>
        )}
        <span className="ml-auto text-xs text-muted-foreground">
          {totalElements} {totalElements === 1 ? 'item' : 'items'}
        </span>
      </form>

      {loading ? (
        <div className="flex items-center justify-center py-12 text-sm text-muted-foreground">
          <Loader2 className="mr-2 h-4 w-4 animate-spin" />
          Loading…
        </div>
      ) : items.length === 0 ? (
        <p className="py-12 text-center text-sm text-muted-foreground">
          {emptyMessage ?? 'No items match.'}
        </p>
      ) : (
        <>
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>ID</TableHead>
                <TableHead>Title</TableHead>
                <TableHead>Status</TableHead>
                <TableHead>Severity</TableHead>
                <TableHead>Scheduled</TableHead>
                <TableHead>POC</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {items.map((it) => {
                const sched = it.scheduledCompletionDate ? new Date(it.scheduledCompletionDate) : null;
                const isOverdue = sched != null && it.status === 'OPEN' && sched < today;
                return (
                  <TableRow key={it.id}>
                    <TableCell className="font-mono text-xs text-muted-foreground">{it.externalId}</TableCell>
                    <TableCell className="max-w-md">
                      <div className="font-medium text-sm">{it.title}</div>
                      {it.description && (
                        <div className="mt-0.5 text-xs text-muted-foreground line-clamp-2">{it.description}</div>
                      )}
                    </TableCell>
                    <TableCell>
                      <Badge variant={it.status === 'OPEN' ? 'destructive' : it.status === 'CLOSED' ? 'secondary' : 'outline'}>
                        {it.status}
                      </Badge>
                    </TableCell>
                    <TableCell>
                      {it.severity ? <Badge variant="outline">{it.severity}</Badge> : <span className="text-muted-foreground">—</span>}
                    </TableCell>
                    <TableCell className="text-sm">
                      {it.scheduledCompletionDate
                        ? <span className={isOverdue ? 'flex items-center gap-1 text-destructive' : undefined}>
                            {isOverdue && <AlertTriangle className="h-3 w-3" />}
                            {it.scheduledCompletionDate}
                          </span>
                        : <span className="text-muted-foreground">—</span>}
                    </TableCell>
                    <TableCell className="text-sm text-muted-foreground">{it.pointOfContact ?? '—'}</TableCell>
                  </TableRow>
                );
              })}
            </TableBody>
          </Table>

          {totalPages > 1 && (
            <div className="mt-3 flex items-center justify-between">
              <span className="text-xs text-muted-foreground">
                Page {page + 1} of {totalPages}
              </span>
              <div className="flex gap-1">
                <Button
                  variant="outline"
                  size="sm"
                  onClick={() => setPage(Math.max(0, page - 1))}
                  disabled={page === 0}
                >
                  <ChevronLeft className="h-4 w-4" />
                  Prev
                </Button>
                <Button
                  variant="outline"
                  size="sm"
                  onClick={() => setPage(Math.min(totalPages - 1, page + 1))}
                  disabled={page >= totalPages - 1}
                >
                  Next
                  <ChevronRight className="h-4 w-4" />
                </Button>
              </div>
            </div>
          )}
        </>
      )}
    </Card>
  );
}
