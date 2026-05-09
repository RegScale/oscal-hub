'use client';
import Link from 'next/link';
import { useEffect, useState } from 'react';
import { useRouter, useSearchParams } from 'next/navigation';
import { useAuth } from '@/contexts/AuthContext';
import { listAdminTickets } from '@/lib/api/tickets';
import type { TicketSummaryResponse, TicketStatus, TicketType, TicketPriority } from '@/types/ticket';
import { TicketStatusBadge } from '@/components/tickets/TicketStatusBadge';
import { TicketTypeBadge } from '@/components/tickets/TicketTypeBadge';
import { TicketPriorityBadge } from '@/components/tickets/TicketPriorityBadge';
import { AnalyticsPanel } from '@/components/tickets/AnalyticsPanel';

const ALL_STATUSES: TicketStatus[] = ['OPEN','IN_PROGRESS','RESOLVED','CLOSED','WONT_FIX','DUPLICATE'];
const ALL_PRIORITIES: TicketPriority[] = ['LOW','MEDIUM','HIGH','CRITICAL'];

export default function AdminTicketsPage() {
  const router = useRouter();
  const params = useSearchParams();
  const { user } = useAuth();

  useEffect(() => {
    if (user && user.globalRole !== 'SUPER_ADMIN') router.replace('/');
  }, [user, router]);

  const [items, setItems] = useState<TicketSummaryResponse[]>([]);
  const [total, setTotal] = useState(0);
  const [loading, setLoading] = useState(true);
  const [q, setQ] = useState(params.get('q') ?? '');
  const [type, setType] = useState<TicketType | ''>((params.get('type') as TicketType) || '');
  const [statuses, setStatuses] = useState<TicketStatus[]>(params.getAll('status') as TicketStatus[]);
  const [priorities, setPriorities] = useState<TicketPriority[]>(params.getAll('priority') as TicketPriority[]);
  const [page, setPage] = useState(Number(params.get('page') ?? 0));

  useEffect(() => {
    setLoading(true);
    listAdminTickets({
      page, size: 25, q: q || undefined,
      type: type || undefined,
      status: statuses.length ? statuses : undefined,
      priority: priorities.length ? priorities : undefined,
    }).then(r => { setItems(r.content); setTotal(r.totalElements); setLoading(false); });
  }, [q, type, statuses, priorities, page]);

  function toggle<T>(arr: T[], v: T): T[] {
    return arr.includes(v) ? arr.filter(x => x !== v) : [...arr, v];
  }

  return (
    <div className="mx-auto max-w-6xl p-6">
      <h1 className="text-2xl font-semibold mb-6">Admin Tickets</h1>

      <AnalyticsPanel />

      <div className="mb-4 flex flex-wrap gap-3 items-center">
        <input
          className="rounded border px-3 py-2 text-sm flex-1 min-w-[240px]"
          placeholder="Search title or description…"
          value={q} onChange={e => { setPage(0); setQ(e.target.value); }} />
        <select className="rounded border px-2 py-2 text-sm"
                value={type} onChange={e => { setPage(0); setType(e.target.value as TicketType | ''); }}>
          <option value="">All types</option>
          <option value="BUG">Bug</option>
          <option value="FEATURE">Feature</option>
        </select>
        <details className="relative">
          <summary className="rounded border px-3 py-2 text-sm cursor-pointer">
            Status ({statuses.length || 'any'})
          </summary>
          <div className="absolute z-10 mt-1 rounded border bg-popover p-2 shadow">
            {ALL_STATUSES.map(s => (
              <label key={s} className="flex items-center gap-2 py-1 text-sm">
                <input type="checkbox" checked={statuses.includes(s)}
                       onChange={() => { setPage(0); setStatuses(toggle(statuses, s)); }} />
                {s}
              </label>
            ))}
          </div>
        </details>
        <details className="relative">
          <summary className="rounded border px-3 py-2 text-sm cursor-pointer">
            Priority ({priorities.length || 'any'})
          </summary>
          <div className="absolute z-10 mt-1 rounded border bg-popover p-2 shadow">
            {ALL_PRIORITIES.map(p => (
              <label key={p} className="flex items-center gap-2 py-1 text-sm">
                <input type="checkbox" checked={priorities.includes(p)}
                       onChange={() => { setPage(0); setPriorities(toggle(priorities, p)); }} />
                {p}
              </label>
            ))}
          </div>
        </details>
      </div>

      {loading ? <div>Loading…</div> : (
        <>
          <table className="w-full text-sm border">
            <thead className="bg-muted">
              <tr>
                <th className="text-left px-3 py-2">ID</th>
                <th className="text-left px-3 py-2">Type</th>
                <th className="text-left px-3 py-2">Title</th>
                <th className="text-left px-3 py-2">Status</th>
                <th className="text-left px-3 py-2">Priority</th>
                <th className="text-left px-3 py-2">Reporter</th>
                <th className="text-left px-3 py-2">Updated</th>
              </tr>
            </thead>
            <tbody>
              {items.map(t => (
                <tr key={t.id} className="border-t hover:bg-accent">
                  <td className="px-3 py-2 tabular-nums text-muted-foreground">TKT-{t.id}</td>
                  <td className="px-3 py-2"><TicketTypeBadge type={t.type} /></td>
                  <td className="px-3 py-2">
                    <Link href={`/tickets/${t.id}`} className="text-blue-600 hover:underline">{t.title}</Link>
                  </td>
                  <td className="px-3 py-2"><TicketStatusBadge status={t.status} /></td>
                  <td className="px-3 py-2"><TicketPriorityBadge priority={t.priority} /></td>
                  <td className="px-3 py-2">{t.reporterUsername}</td>
                  <td className="px-3 py-2 text-muted-foreground">{new Date(t.updatedAt).toLocaleDateString()}</td>
                </tr>
              ))}
            </tbody>
          </table>

          <div className="mt-3 flex items-center justify-between text-sm">
            <span>{total} total</span>
            <div className="flex gap-2">
              <button disabled={page === 0} onClick={() => setPage(page - 1)}
                      className="rounded border px-3 py-1 disabled:opacity-50">Prev</button>
              <span className="px-2 py-1">Page {page + 1}</span>
              <button disabled={(page + 1) * 25 >= total} onClick={() => setPage(page + 1)}
                      className="rounded border px-3 py-1 disabled:opacity-50">Next</button>
            </div>
          </div>
        </>
      )}
    </div>
  );
}
