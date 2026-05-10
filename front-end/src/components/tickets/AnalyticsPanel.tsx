'use client';
import { useEffect, useState } from 'react';
import Link from 'next/link';
import { LineChart, Line, XAxis, YAxis, Tooltip, Legend, ResponsiveContainer } from 'recharts';
import { getTicketAnalytics, type TicketAnalytics } from '@/lib/api/tickets';
import type { TicketStatus } from '@/types/ticket';

const STATUS_LABEL: Record<TicketStatus, string> = {
  OPEN: 'Open',
  IN_PROGRESS: 'In Progress',
  RESOLVED: 'Resolved',
  CLOSED: 'Closed',
  WONT_FIX: "Won't Fix",
  DUPLICATE: 'Duplicate',
};

export function AnalyticsPanel() {
  const [data, setData] = useState<TicketAnalytics | null>(null);

  useEffect(() => {
    getTicketAnalytics().then(setData).catch(console.error);
  }, []);

  if (!data) return <div className="text-sm text-muted-foreground mb-6">Loading analytics…</div>;

  // Build merged-by-week dataset for the chart
  const weeks = new Map<string, { week: string; opened: number; resolved: number }>();
  data.openedPerWeek.forEach(p => weeks.set(p.week, { week: p.week, opened: p.count, resolved: 0 }));
  data.resolvedPerWeek.forEach(p => {
    const w = weeks.get(p.week) ?? { week: p.week, opened: 0, resolved: 0 };
    w.resolved = p.count;
    weeks.set(p.week, w);
  });
  const chartData = Array.from(weeks.values()).sort((a, b) => a.week.localeCompare(b.week));

  return (
    <div className="space-y-6 mb-6">
      {/* Status counts */}
      <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-6 gap-3">
        {(Object.keys(data.statusCounts) as TicketStatus[]).map(s => (
          <div key={s} className="rounded border p-3">
            <div className="text-xs text-muted-foreground">{STATUS_LABEL[s]}</div>
            <div className="text-2xl font-semibold tabular-nums">{data.statusCounts[s]}</div>
          </div>
        ))}
      </div>

      {/* Type split */}
      <div className="grid grid-cols-2 gap-3">
        <div className="rounded border p-3">
          <div className="text-xs text-muted-foreground">Bug Reports</div>
          <div className="text-2xl font-semibold tabular-nums">{data.typeSplit.BUG ?? 0}</div>
        </div>
        <div className="rounded border p-3">
          <div className="text-xs text-muted-foreground">Feature Requests</div>
          <div className="text-2xl font-semibold tabular-nums">{data.typeSplit.FEATURE ?? 0}</div>
        </div>
      </div>

      {/* Opened vs Resolved per week */}
      <div className="rounded border p-4">
        <h2 className="text-sm font-medium mb-3">Activity (last 12 weeks)</h2>
        {chartData.length === 0 ? (
          <p className="text-sm text-muted-foreground">No activity yet.</p>
        ) : (
          <ResponsiveContainer width="100%" height={200}>
            <LineChart data={chartData}>
              <XAxis dataKey="week" tick={{ fontSize: 11 }} />
              <YAxis allowDecimals={false} tick={{ fontSize: 11 }} />
              <Tooltip />
              <Legend />
              <Line type="monotone" dataKey="opened" stroke="#3b82f6" name="Opened" dot={false} />
              <Line type="monotone" dataKey="resolved" stroke="#10b981" name="Resolved" dot={false} />
            </LineChart>
          </ResponsiveContainer>
        )}
      </div>

      {/* Stale tickets */}
      {data.staleTickets.length > 0 && (
        <div className="rounded border">
          <div className="px-4 py-2 border-b bg-muted text-sm font-medium">
            Stale tickets (open &gt;30 days)
          </div>
          <ul className="divide-y">
            {data.staleTickets.map(s => (
              <li key={s.id} className="flex items-center gap-3 p-3 text-sm">
                <span className="text-xs text-muted-foreground tabular-nums w-16">TKT-{s.id}</span>
                <Link href={`/tickets/${s.id}`} className="flex-1 truncate text-blue-600 hover:underline">
                  {s.title}
                </Link>
                <span className="text-xs text-muted-foreground">{s.ageDays}d old</span>
              </li>
            ))}
          </ul>
        </div>
      )}
    </div>
  );
}
