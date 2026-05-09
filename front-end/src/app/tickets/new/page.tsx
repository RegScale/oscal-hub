'use client';
import { useState } from 'react';
import { useRouter } from 'next/navigation';
import { createTicket } from '@/lib/api/tickets';
import type { TicketType, TicketPriority, BugMetadata, FeatureMetadata } from '@/types/ticket';

export default function NewTicketPage() {
  const router = useRouter();
  const [type, setType] = useState<TicketType>('BUG');
  const [title, setTitle] = useState('');
  const [description, setDescription] = useState('');
  const [priority, setPriority] = useState<TicketPriority>('MEDIUM');
  const [bug, setBug] = useState<BugMetadata>({ severity: 'MAJOR' });
  const [feature, setFeature] = useState<FeatureMetadata>({});
  const [files, setFiles] = useState<File[]>([]);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  function captureEnv(): BugMetadata {
    if (typeof window === 'undefined') return {};
    return {
      browser: navigator.userAgent,
      viewport: `${window.innerWidth}x${window.innerHeight}`,
      url: window.location.pathname,
    };
  }

  function onFilesChange(e: React.ChangeEvent<HTMLInputElement>) {
    const list = Array.from(e.target.files ?? []);
    if (list.length > 5) { setError('Max 5 files'); return; }
    if (list.some(f => f.size > 10 * 1024 * 1024)) { setError('Max 10 MB per file'); return; }
    setError(null);
    setFiles(list);
  }

  async function onSubmit(e: React.FormEvent) {
    e.preventDefault();
    setSubmitting(true); setError(null);
    try {
      const metadata: Record<string, unknown> = type === 'BUG'
        ? { ...bug, ...captureEnv() }
        : { ...feature };
      const created = await createTicket({ type, title, description, priority, metadata, files });
      router.push(`/tickets/${created.id}`);
    } catch (err) {
      setError(err instanceof Error ? err.message : String(err));
      setSubmitting(false);
    }
  }

  return (
    <div className="mx-auto max-w-2xl p-6">
      <h1 className="text-2xl font-semibold mb-6">Open a Ticket</h1>
      <form onSubmit={onSubmit} className="space-y-5">
        <fieldset className="flex gap-6" aria-label="Ticket type">
          <label className="flex items-center gap-2">
            <input type="radio" checked={type === 'BUG'} onChange={() => setType('BUG')} /> Bug Report
          </label>
          <label className="flex items-center gap-2">
            <input type="radio" checked={type === 'FEATURE'} onChange={() => setType('FEATURE')} /> Feature Request
          </label>
        </fieldset>

        <label className="block">
          <span className="text-sm font-medium">Title</span>
          <input className="mt-1 w-full rounded border px-3 py-2"
                 maxLength={200} required value={title}
                 onChange={e => setTitle(e.target.value)} />
        </label>

        <label className="block">
          <span className="text-sm font-medium">Description</span>
          <textarea className="mt-1 w-full rounded border px-3 py-2 min-h-[120px]"
                    required value={description}
                    placeholder={type === 'BUG'
                      ? 'What you saw, briefly. Use the structured fields below for steps and expected behavior.'
                      : 'What problem this solves and who it helps.'}
                    onChange={e => setDescription(e.target.value)} />
        </label>

        <label className="block">
          <span className="text-sm font-medium">Priority</span>
          <select className="mt-1 w-full rounded border px-3 py-2"
                  value={priority} onChange={e => setPriority(e.target.value as TicketPriority)}>
            {(['LOW','MEDIUM','HIGH','CRITICAL'] as TicketPriority[]).map(p =>
              <option key={p} value={p}>{p}</option>)}
          </select>
        </label>

        {type === 'BUG' && (
          <div className="space-y-3">
            <label className="block">
              <span className="text-sm font-medium">Steps to Reproduce</span>
              <textarea className="mt-1 w-full rounded border px-3 py-2"
                        value={bug.stepsToReproduce ?? ''}
                        onChange={e => setBug({ ...bug, stepsToReproduce: e.target.value })} />
            </label>
            <label className="block">
              <span className="text-sm font-medium">Expected Behavior</span>
              <textarea className="mt-1 w-full rounded border px-3 py-2"
                        value={bug.expectedBehavior ?? ''}
                        onChange={e => setBug({ ...bug, expectedBehavior: e.target.value })} />
            </label>
            <label className="block">
              <span className="text-sm font-medium">Actual Behavior</span>
              <textarea className="mt-1 w-full rounded border px-3 py-2"
                        value={bug.actualBehavior ?? ''}
                        onChange={e => setBug({ ...bug, actualBehavior: e.target.value })} />
            </label>
            <label className="block">
              <span className="text-sm font-medium">Severity</span>
              <select className="mt-1 w-full rounded border px-3 py-2"
                      value={bug.severity ?? 'MAJOR'}
                      onChange={e => setBug({ ...bug, severity: e.target.value as BugMetadata['severity'] })}>
                {(['MINOR','MAJOR','CRITICAL'] as const).map(s => <option key={s} value={s}>{s}</option>)}
              </select>
            </label>
          </div>
        )}

        {type === 'FEATURE' && (
          <label className="block">
            <span className="text-sm font-medium">Use Case</span>
            <textarea className="mt-1 w-full rounded border px-3 py-2 min-h-[100px]"
                      placeholder="Why does this matter? What problem does it solve?"
                      value={feature.useCase ?? ''}
                      onChange={e => setFeature({ ...feature, useCase: e.target.value })} />
          </label>
        )}

        <label className="block">
          <span className="text-sm font-medium">Attachments (max 5 files, 10 MB each)</span>
          <input type="file" multiple onChange={onFilesChange}
                 accept=".png,.jpg,.jpeg,.gif,.pdf,.txt,.log,.json,.xml,.yaml,.yml" />
        </label>

        {error && <p className="text-sm text-rose-600">{error}</p>}

        <button type="submit" disabled={submitting || !title || !description}
                className="rounded bg-blue-600 px-4 py-2 text-white disabled:opacity-50">
          {submitting ? 'Submitting…' : 'Submit Ticket'}
        </button>
      </form>
    </div>
  );
}
