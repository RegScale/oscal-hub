'use client';

import { useEffect, useState } from 'react';
import { X, Check, AlertCircle, Loader2 } from 'lucide-react';
import { aiClient, AiSessionDetail, AiSessionStatus } from '@/lib/ai-client';
import { describeEvent } from '@/lib/ai-events';
import type { AiEvent } from '@/hooks/useAiSession';

// ─── helpers ──────────────────────────────────────────────────────────────────

function microsToUsd(micros: number): string {
  return `$${(micros / 1_000_000).toFixed(4)}`;
}

function formatDuration(startedAt: string, endedAt: string | null): string {
  if (!endedAt) return '—';
  const ms = new Date(endedAt).getTime() - new Date(startedAt).getTime();
  if (ms < 0) return '—';
  const totalSeconds = Math.floor(ms / 1000);
  const minutes = Math.floor(totalSeconds / 60);
  const seconds = totalSeconds % 60;
  if (minutes === 0) return `${seconds}s`;
  return `${minutes}m ${seconds}s`;
}

function StatusPill({ status }: { status: AiSessionStatus }) {
  const base = 'inline-flex items-center gap-1 px-2 py-0.5 text-xs font-medium rounded-full';
  switch (status) {
    case 'COMPLETED':
      return (
        <span className={`${base} bg-green-100 text-green-800 dark:bg-green-900 dark:text-green-200`}>
          COMPLETED
        </span>
      );
    case 'FAILED':
      return (
        <span className={`${base} bg-red-100 text-red-800 dark:bg-red-900 dark:text-red-200`}>
          FAILED
        </span>
      );
    case 'RUNNING':
      return (
        <span className={`${base} bg-indigo-100 text-indigo-800 dark:bg-indigo-900 dark:text-indigo-200`}>
          <span className="h-1.5 w-1.5 rounded-full bg-indigo-500 animate-pulse" />
          RUNNING
        </span>
      );
    case 'CANCELLED':
      return (
        <span className={`${base} bg-gray-100 text-gray-700 dark:bg-gray-700 dark:text-gray-300`}>
          CANCELLED
        </span>
      );
    case 'AWAITING_INPUT':
      return (
        <span className={`${base} bg-amber-100 text-amber-800 dark:bg-amber-900 dark:text-amber-200`}>
          AWAITING INPUT
        </span>
      );
    default:
      return (
        <span className={`${base} bg-gray-100 text-gray-700 dark:bg-gray-700 dark:text-gray-300`}>
          {status}
        </span>
      );
  }
}

// ─── props ────────────────────────────────────────────────────────────────────

interface SessionDetailDrawerProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  organizationId: number | null;
  sessionId: string | null;
}

// ─── component ────────────────────────────────────────────────────────────────

/**
 * Side-panel drawer that shows the full detail for one AI session:
 * header, token/cost grid, event log, and an error callout when FAILED.
 *
 * Uses a custom right-side overlay built on Dialog primitives because
 * shadcn/ui Sheet is not installed in this project.
 */
export function SessionDetailDrawer({
  open,
  onOpenChange,
  organizationId,
  sessionId,
}: SessionDetailDrawerProps) {
  const [detail, setDetail] = useState<AiSessionDetail | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!open || !sessionId || !organizationId) return;

    let cancelled = false;
    setLoading(true);
    setError(null);
    setDetail(null);

    aiClient
      .getSession(organizationId, sessionId)
      .then((data) => {
        if (!cancelled) setDetail(data);
      })
      .catch((err: unknown) => {
        if (!cancelled)
          setError(err instanceof Error ? err.message : 'Failed to load session');
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });

    return () => {
      cancelled = true;
    };
  }, [open, sessionId, organizationId]);

  if (!open) return null;

  const summary = detail?.summary;

  return (
    <>
      {/* Backdrop */}
      <div
        className="fixed inset-0 z-40 bg-black/50"
        onClick={() => onOpenChange(false)}
        aria-hidden="true"
      />

      {/* Panel */}
      <div
        role="dialog"
        aria-modal="true"
        aria-label="Session detail"
        className="fixed right-0 top-0 z-50 flex h-full w-full max-w-xl flex-col bg-white dark:bg-gray-900 shadow-xl overflow-hidden"
      >
        {/* Close button */}
        <button
          onClick={() => onOpenChange(false)}
          className="absolute right-4 top-4 rounded-sm opacity-70 hover:opacity-100 focus:outline-none focus:ring-2 focus:ring-ring focus:ring-offset-2"
          aria-label="Close"
        >
          <X className="h-5 w-5" />
        </button>

        {/* Loading state */}
        {loading && (
          <div className="flex flex-1 items-center justify-center">
            <Loader2 className="h-6 w-6 animate-spin text-indigo-500" />
          </div>
        )}

        {/* Fetch error */}
        {!loading && error && (
          <div className="m-6 rounded-md border border-destructive/30 bg-destructive/10 p-4 text-sm text-destructive">
            <div className="font-medium mb-1">Failed to load session</div>
            <div className="font-mono text-xs">{error}</div>
          </div>
        )}

        {/* Detail content */}
        {!loading && detail && summary && (
          <div className="flex flex-col h-full overflow-y-auto">
            {/* ── Header ── */}
            <div className="px-6 pt-6 pb-4 border-b border-gray-200 dark:border-gray-700 pr-12">
              <div className="flex items-center gap-2 mb-2">
                <span className="text-base font-semibold text-gray-900 dark:text-white">
                  {summary.wizardKind}
                </span>
                <StatusPill status={summary.status} />
              </div>
              <div className="text-xs text-gray-500 dark:text-gray-400 space-y-0.5">
                <div>
                  <span className="font-medium">Started:</span>{' '}
                  {new Date(summary.startedAt).toLocaleString()}
                </div>
                {summary.endedAt && (
                  <div>
                    <span className="font-medium">Ended:</span>{' '}
                    {new Date(summary.endedAt).toLocaleString()}
                  </div>
                )}
                <div>
                  <span className="font-medium">Duration:</span>{' '}
                  {formatDuration(summary.startedAt, summary.endedAt)}
                </div>
              </div>
            </div>

            {/* ── Tokens & cost grid ── */}
            <div className="px-6 py-4 border-b border-gray-200 dark:border-gray-700">
              <h3 className="text-sm font-semibold text-gray-700 dark:text-gray-300 mb-3">
                Tokens &amp; Cost
              </h3>
              <dl className="grid grid-cols-2 gap-x-6 gap-y-2 text-sm">
                <div>
                  <dt className="text-xs text-gray-500 dark:text-gray-400">Tokens in</dt>
                  <dd className="font-medium tabular-nums">{summary.tokensIn.toLocaleString()}</dd>
                </div>
                <div>
                  <dt className="text-xs text-gray-500 dark:text-gray-400">Tokens out</dt>
                  <dd className="font-medium tabular-nums">{summary.tokensOut.toLocaleString()}</dd>
                </div>
                <div>
                  <dt className="text-xs text-gray-500 dark:text-gray-400">Model</dt>
                  <dd className="font-mono text-xs truncate">{summary.model}</dd>
                </div>
                <div>
                  <dt className="text-xs text-gray-500 dark:text-gray-400">Cost</dt>
                  <dd className="font-medium tabular-nums">{microsToUsd(summary.costUsdMicros)}</dd>
                </div>
                <div>
                  <dt className="text-xs text-gray-500 dark:text-gray-400">Duration</dt>
                  <dd className="font-medium">{formatDuration(summary.startedAt, summary.endedAt)}</dd>
                </div>
                <div>
                  <dt className="text-xs text-gray-500 dark:text-gray-400">User</dt>
                  <dd className="truncate">{summary.username ?? `#${summary.userId}`}</dd>
                </div>
              </dl>
            </div>

            {/* ── Error callout (FAILED only) ── */}
            {summary.status === 'FAILED' && (
              <div className="mx-6 mt-4 rounded-md border border-destructive/40 bg-destructive/10 p-3 text-sm">
                <div className="flex items-center gap-1.5 font-medium text-destructive mb-1">
                  <AlertCircle className="h-4 w-4 flex-shrink-0" />
                  Error
                </div>
                {summary.errorCode && (
                  <div className="text-xs font-mono text-destructive mb-0.5">
                    Code: {summary.errorCode}
                  </div>
                )}
                {detail.errorMessage && (
                  <div className="text-xs whitespace-pre-wrap text-destructive">
                    {detail.errorMessage}
                  </div>
                )}
              </div>
            )}

            {/* ── Event log ── */}
            <div className="px-6 pt-4 pb-6 flex-1">
              <h3 className="text-sm font-semibold text-gray-700 dark:text-gray-300 mb-3">
                Event log ({detail.events.length})
              </h3>
              {detail.events.length === 0 ? (
                <p className="text-sm text-gray-400 dark:text-gray-500">No events recorded.</p>
              ) : (
                <ol className="space-y-2">
                  {detail.events.map((raw, i) => {
                    // `data` on the persisted event is a stringified JSON blob — parse it
                    // before calling describeEvent which expects an object.
                    let parsedData: Record<string, unknown> = {};
                    try {
                      parsedData = typeof raw.data === 'string'
                        ? (JSON.parse(raw.data) as Record<string, unknown>)
                        : (raw.data as Record<string, unknown>);
                    } catch {
                      parsedData = { raw: raw.data };
                    }

                    const event: AiEvent = {
                      type: raw.type as AiEvent['type'],
                      data: parsedData,
                    };

                    const label = describeEvent(event);
                    const isComplete = raw.type === 'complete';
                    const isError = raw.type === 'error';

                    return (
                      <li key={i} className="flex items-start gap-2.5 text-sm">
                        <span className="flex-shrink-0 mt-0.5">
                          {isError ? (
                            <AlertCircle className="h-4 w-4 text-destructive" />
                          ) : isComplete ? (
                            <Check className="h-4 w-4 text-green-600" />
                          ) : (
                            <span className="h-4 w-4 inline-flex items-center justify-center">
                              <span className="h-1.5 w-1.5 rounded-full bg-gray-400 dark:bg-gray-500" />
                            </span>
                          )}
                        </span>
                        <span className="text-gray-700 dark:text-gray-300 leading-snug">
                          <span className="text-xs font-mono text-gray-400 dark:text-gray-500 mr-2">
                            {raw.type}
                          </span>
                          {label}
                        </span>
                      </li>
                    );
                  })}
                </ol>
              )}
            </div>
          </div>
        )}
      </div>
    </>
  );
}
