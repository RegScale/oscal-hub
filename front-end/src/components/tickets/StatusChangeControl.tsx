'use client';
import { useState } from 'react';
import type { TicketStatus } from '@/types/ticket';
import { changeStatus } from '@/lib/api/tickets';

const ALL_STATUSES: TicketStatus[] = [
  'OPEN','IN_PROGRESS','RESOLVED','CLOSED','WONT_FIX','DUPLICATE',
];
const TERMINAL: TicketStatus[] = ['CLOSED','WONT_FIX','DUPLICATE'];

export function StatusChangeControl({
  ticketId, current, onChange,
}: { ticketId: number; current: TicketStatus; onChange: () => void }) {
  const [next, setNext] = useState<TicketStatus>(current);
  const [note, setNote] = useState('');
  const [busy, setBusy] = useState(false);

  async function submit() {
    if (next === current) return;
    if (TERMINAL.includes(next) &&
        !confirm(`Set status to ${next}? This is terminal and the user will not be able to reopen.`)) {
      return;
    }
    setBusy(true);
    try { await changeStatus(ticketId, next, note || undefined); setNote(''); onChange(); }
    finally { setBusy(false); }
  }

  return (
    <div className="flex flex-wrap items-center gap-2 rounded border p-3 bg-muted/30">
      <label className="text-sm font-medium">Set status:</label>
      <select className="rounded border px-2 py-1 text-sm"
              value={next} onChange={e => setNext(e.target.value as TicketStatus)}>
        {ALL_STATUSES.map(s => <option key={s} value={s}>{s}</option>)}
      </select>
      <input className="flex-1 rounded border px-2 py-1 text-sm"
             placeholder="Optional note (becomes a comment)…"
             value={note} onChange={e => setNote(e.target.value)} />
      <button onClick={submit} disabled={busy || next === current}
              className="rounded bg-blue-600 px-3 py-1 text-sm text-white disabled:opacity-50">
        Apply
      </button>
    </div>
  );
}
