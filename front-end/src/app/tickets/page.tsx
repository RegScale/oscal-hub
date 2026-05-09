'use client';
import Link from 'next/link';
import { useEffect, useState } from 'react';
import { listMyTickets } from '@/lib/api/tickets';
import type { TicketSummaryResponse } from '@/types/ticket';
import { TicketStatusBadge } from '@/components/tickets/TicketStatusBadge';
import { TicketTypeBadge } from '@/components/tickets/TicketTypeBadge';
import { TicketPriorityBadge } from '@/components/tickets/TicketPriorityBadge';

export default function MyTicketsPage() {
  const [items, setItems] = useState<TicketSummaryResponse[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    listMyTickets(0, 50).then(p => { setItems(p.content); setLoading(false); });
  }, []);

  if (loading) return <div className="p-6">Loading…</div>;

  if (items.length === 0) {
    return (
      <div className="mx-auto max-w-3xl p-6 text-center">
        <h1 className="text-2xl font-semibold mb-3">My Tickets</h1>
        <p className="text-muted-foreground mb-6">You haven&apos;t opened any tickets yet.</p>
        <Link href="/tickets/new" className="rounded bg-blue-600 px-4 py-2 text-white">
          Open Your First Ticket
        </Link>
      </div>
    );
  }

  return (
    <div className="mx-auto max-w-5xl p-6">
      <div className="flex items-center justify-between mb-6">
        <h1 className="text-2xl font-semibold">My Tickets</h1>
        <Link href="/tickets/new" className="rounded bg-blue-600 px-4 py-2 text-white text-sm">
          Open New Ticket
        </Link>
      </div>
      <ul className="divide-y rounded border">
        {items.map(t => (
          <li key={t.id} className="hover:bg-accent">
            <Link href={`/tickets/${t.id}`} className="flex items-center gap-3 p-4">
              <span className="text-xs text-muted-foreground tabular-nums">TKT-{t.id}</span>
              <TicketTypeBadge type={t.type} />
              <span className="flex-1 truncate font-medium">{t.title}</span>
              <TicketPriorityBadge priority={t.priority} />
              <TicketStatusBadge status={t.status} />
              <span className="text-xs text-muted-foreground">
                {new Date(t.updatedAt).toLocaleDateString()}
              </span>
            </Link>
          </li>
        ))}
      </ul>
    </div>
  );
}
