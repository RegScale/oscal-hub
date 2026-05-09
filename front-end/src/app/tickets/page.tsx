'use client';
import Link from 'next/link';
import { useEffect, useState } from 'react';
import { listMyTickets } from '@/lib/api/tickets';
import type { TicketSummaryResponse, TicketStatus, TicketType } from '@/types/ticket';
import { TicketStatusBadge } from '@/components/tickets/TicketStatusBadge';
import { TicketTypeBadge } from '@/components/tickets/TicketTypeBadge';
import { TicketPriorityBadge } from '@/components/tickets/TicketPriorityBadge';

const ALL_STATUSES: TicketStatus[] = ['OPEN', 'IN_PROGRESS', 'RESOLVED', 'CLOSED', 'WONT_FIX', 'DUPLICATE'];

type AgeWindow = 'ANY' | 'LAST_7' | 'LAST_30' | 'LAST_90' | 'OLDER_THAN_90';

const AGE_OPTIONS: { value: AgeWindow; label: string }[] = [
  { value: 'ANY', label: 'Any age' },
  { value: 'LAST_7', label: 'Last 7 days' },
  { value: 'LAST_30', label: 'Last 30 days' },
  { value: 'LAST_90', label: 'Last 90 days' },
  { value: 'OLDER_THAN_90', label: 'Older than 90 days' },
];

function ageWindowToRange(w: AgeWindow): { from?: string; to?: string } {
  const now = new Date();
  const daysAgo = (n: number) => new Date(now.getTime() - n * 86400000).toISOString();
  switch (w) {
    case 'ANY': return {};
    case 'LAST_7': return { from: daysAgo(7) };
    case 'LAST_30': return { from: daysAgo(30) };
    case 'LAST_90': return { from: daysAgo(90) };
    case 'OLDER_THAN_90': return { to: daysAgo(90) };
  }
}

export default function MyTicketsPage() {
  const [items, setItems] = useState<TicketSummaryResponse[]>([]);
  const [total, setTotal] = useState(0);
  const [loading, setLoading] = useState(true);
  const [q, setQ] = useState('');
  const [type, setType] = useState<TicketType | ''>('');
  const [statuses, setStatuses] = useState<TicketStatus[]>([]);
  const [age, setAge] = useState<AgeWindow>('ANY');

  function toggle<T>(arr: T[], v: T): T[] {
    return arr.includes(v) ? arr.filter(x => x !== v) : [...arr, v];
  }

  useEffect(() => {
    setLoading(true);
    const range = ageWindowToRange(age);
    listMyTickets({
      page: 0, size: 50,
      q: q || undefined,
      type: type || undefined,
      status: statuses.length ? statuses : undefined,
      from: range.from,
      to: range.to,
    }).then(p => { setItems(p.content); setTotal(p.totalElements); setLoading(false); });
  }, [q, type, statuses, age]);

  const hasNoFilters = !q && !type && statuses.length === 0 && age === 'ANY';

  return (
    <div className="mx-auto max-w-5xl p-6">
      <div className="flex items-center justify-between mb-6">
        <h1 className="text-2xl font-semibold">My Tickets</h1>
        <Link href="/tickets/new" className="rounded bg-blue-600 px-4 py-2 text-white text-sm">
          Open New Ticket
        </Link>
      </div>

      <div className="mb-4 flex flex-wrap gap-3 items-center">
        <input
          className="rounded border px-3 py-2 text-sm flex-1 min-w-[240px]"
          placeholder="Search title or description…"
          value={q} onChange={e => setQ(e.target.value)} />
        <select className="rounded border px-2 py-2 text-sm"
                value={type} onChange={e => setType(e.target.value as TicketType | '')}>
          <option value="">All types</option>
          <option value="BUG">Bug</option>
          <option value="FEATURE">Feature</option>
        </select>
        <details className="relative">
          <summary className="rounded border px-3 py-2 text-sm cursor-pointer list-none">
            Status ({statuses.length || 'any'})
          </summary>
          <div className="absolute z-10 mt-1 rounded border bg-popover p-2 shadow">
            {ALL_STATUSES.map(s => (
              <label key={s} className="flex items-center gap-2 py-1 text-sm whitespace-nowrap">
                <input type="checkbox" checked={statuses.includes(s)}
                       onChange={() => setStatuses(toggle(statuses, s))} />
                {s}
              </label>
            ))}
          </div>
        </details>
        <select className="rounded border px-2 py-2 text-sm"
                value={age} onChange={e => setAge(e.target.value as AgeWindow)}>
          {AGE_OPTIONS.map(o => <option key={o.value} value={o.value}>{o.label}</option>)}
        </select>
      </div>

      {loading ? (
        <div>Loading…</div>
      ) : items.length === 0 ? (
        hasNoFilters ? (
          <div className="text-center py-12">
            <p className="text-muted-foreground mb-6">You haven&apos;t opened any tickets yet.</p>
            <Link href="/tickets/new" className="rounded bg-blue-600 px-4 py-2 text-white">
              Open Your First Ticket
            </Link>
          </div>
        ) : (
          <p className="text-sm text-muted-foreground py-8 text-center">
            No tickets match these filters.
          </p>
        )
      ) : (
        <>
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
          <p className="mt-3 text-xs text-muted-foreground">{total} total</p>
        </>
      )}
    </div>
  );
}
