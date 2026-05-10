'use client';
import { useEffect, useState, useCallback } from 'react';
import { useParams } from 'next/navigation';
import {
  getTicket, addComment, attachmentDownloadUrl,
} from '@/lib/api/tickets';
import type { TicketDetailResponse } from '@/types/ticket';
import { TicketStatusBadge } from '@/components/tickets/TicketStatusBadge';
import { TicketTypeBadge } from '@/components/tickets/TicketTypeBadge';
import { TicketPriorityBadge } from '@/components/tickets/TicketPriorityBadge';
import { useAuth } from '@/contexts/AuthContext';
import { StatusChangeControl } from '@/components/tickets/StatusChangeControl';

export default function TicketDetailPage() {
  const { id } = useParams<{ id: string }>();
  const ticketId = Number(id);
  const { user } = useAuth();
  const [t, setT] = useState<TicketDetailResponse | null>(null);
  const [body, setBody] = useState('');
  const [files, setFiles] = useState<File[]>([]);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const reload = useCallback(() => getTicket(ticketId).then(setT), [ticketId]);

  useEffect(() => { reload(); }, [reload]);

  if (!t) return <div className="p-6">Loading…</div>;

  const isReporter = user?.username === t.reporterUsername;
  const reopenHint = isReporter && t.status === 'RESOLVED';

  async function submitReply(e: React.FormEvent) {
    e.preventDefault(); setBusy(true); setError(null);
    try { await addComment(ticketId, body, files); setBody(''); setFiles([]); await reload(); }
    catch (err) { setError(err instanceof Error ? err.message : String(err)); }
    finally { setBusy(false); }
  }

  return (
    <div className="mx-auto max-w-3xl p-6 space-y-6">
      <header className="space-y-2">
        <div className="flex items-center gap-2 text-sm text-muted-foreground">
          <span>TKT-{t.id}</span>
          <TicketTypeBadge type={t.type} />
          <TicketPriorityBadge priority={t.priority} />
          <TicketStatusBadge status={t.status} />
        </div>
        <h1 className="text-2xl font-semibold">{t.title}</h1>
        <div className="text-xs text-muted-foreground">
          Opened {new Date(t.createdAt).toLocaleString()} · last updated {new Date(t.updatedAt).toLocaleString()}
        </div>
      </header>

      <section className="rounded border p-4 whitespace-pre-wrap">
        {t.description}
        {Object.keys(t.metadata).length > 0 && (
          <dl className="mt-4 text-sm">
            {Object.entries(t.metadata).map(([k, v]) => (
              <div key={k} className="flex gap-2"><dt className="font-medium">{k}:</dt><dd>{String(v)}</dd></div>
            ))}
          </dl>
        )}
        {t.originalAttachments.length > 0 && (
          <ul className="mt-3 text-sm">
            {t.originalAttachments.map(a => (
              <li key={a.id}>
                <a href={attachmentDownloadUrl(a.id)} className="text-blue-600 underline">{a.filename}</a>
                <span className="text-muted-foreground"> ({Math.round(a.sizeBytes/1024)} KB)</span>
              </li>
            ))}
          </ul>
        )}
      </section>

      {user?.globalRole === 'SUPER_ADMIN' && (
        <StatusChangeControl ticketId={t.id} current={t.status} onChange={reload} />
      )}

      <section className="space-y-3">
        {t.comments.map(c => c.statusChange ? (
          <div key={c.id} className="text-xs italic text-muted-foreground">
            {c.body} · {new Date(c.createdAt).toLocaleString()}
          </div>
        ) : (
          <article key={c.id} className="rounded border p-3">
            <header className="flex items-center justify-between text-xs text-muted-foreground mb-2">
              <span className="font-medium text-foreground">{c.authorUsername}</span>
              <span>{new Date(c.createdAt).toLocaleString()}</span>
            </header>
            <p className="whitespace-pre-wrap">{c.body}</p>
            {c.attachments.length > 0 && (
              <ul className="mt-2 text-sm">
                {c.attachments.map(a => (
                  <li key={a.id}>
                    <a href={attachmentDownloadUrl(a.id)} className="text-blue-600 underline">{a.filename}</a>
                  </li>
                ))}
              </ul>
            )}
          </article>
        ))}
      </section>

      <form onSubmit={submitReply} className="space-y-3">
        {reopenHint && (
          <p className="text-xs text-amber-700">Posting a reply will reopen this ticket.</p>
        )}
        <textarea className="w-full rounded border px-3 py-2 min-h-[100px]"
                  placeholder="Write a reply…" required
                  value={body} onChange={e => setBody(e.target.value)} />
        <input type="file" multiple
               accept=".png,.jpg,.jpeg,.gif,.pdf,.txt,.log,.json,.xml,.yaml,.yml"
               onChange={e => setFiles(Array.from(e.target.files ?? []))} />
        {error && <p className="text-sm text-rose-600">{error}</p>}
        <button type="submit" disabled={busy || !body}
                className="rounded bg-blue-600 px-4 py-2 text-white disabled:opacity-50">
          {busy ? 'Sending…' : 'Reply'}
        </button>
      </form>
    </div>
  );
}
