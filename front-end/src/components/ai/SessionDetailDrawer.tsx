'use client';

import { useEffect, useState } from 'react';
import { X, Check, AlertCircle, Loader2 } from 'lucide-react';
import { aiClient, AiSessionDetail, AiSessionStatus } from '@/lib/ai-client';
import { describeEvent } from '@/lib/ai-events';
import type { AiEvent } from '@/hooks/useAiSession';
import { Badge } from '@/components/ui/badge';

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

function StatusBadge({ status }: { status: AiSessionStatus }) {
  switch (status) {
    case 'COMPLETED':
      return <Badge variant="success">COMPLETED</Badge>;
    case 'FAILED':
      return <Badge variant="destructive">FAILED</Badge>;
    case 'RUNNING':
      return (
        <Badge variant="default">
          <span className="h-1.5 w-1.5 rounded-full bg-primary-foreground animate-pulse" />
          RUNNING
        </Badge>
      );
    case 'CANCELLED':
      return <Badge variant="secondary">CANCELLED</Badge>;
    case 'AWAITING_INPUT':
      return <Badge variant="warning">AWAITING INPUT</Badge>;
    default:
      return <Badge variant="outline">{status}</Badge>;
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
      <div
        className="fixed inset-0 z-40 bg-foreground/40 backdrop-blur-sm"
        onClick={() => onOpenChange(false)}
        aria-hidden="true"
      />

      <div
        role="dialog"
        aria-modal="true"
        aria-label="Session detail"
        className="fixed right-0 top-0 z-50 flex h-full w-full max-w-xl flex-col bg-background border-l border-border shadow-xl overflow-hidden"
      >
        <button
          onClick={() => onOpenChange(false)}
          className="absolute right-4 top-4 rounded-sm text-muted-foreground hover:text-foreground transition-colors focus:outline-none focus:ring-2 focus:ring-ring focus:ring-offset-2"
          aria-label="Close"
        >
          <X className="h-5 w-5" />
        </button>

        {loading && (
          <div className="flex flex-1 items-center justify-center">
            <Loader2 className="h-6 w-6 animate-spin text-primary" />
          </div>
        )}

        {!loading && error && (
          <div className="m-6 rounded-md border border-destructive/30 bg-destructive/10 p-4 text-sm text-destructive">
            <div className="font-medium mb-1">Failed to load session</div>
            <div className="font-mono text-xs">{error}</div>
          </div>
        )}

        {!loading && detail && summary && (
          <div className="flex flex-col h-full overflow-y-auto">
            <div className="px-6 pt-6 pb-4 border-b border-border pr-12">
              <div className="flex items-center gap-2 mb-2">
                <span className="text-base font-semibold text-foreground">
                  {summary.wizardKind}
                </span>
                <StatusBadge status={summary.status} />
              </div>
              <div className="text-xs text-muted-foreground space-y-0.5">
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

            <div className="px-6 py-4 border-b border-border">
              <h3 className="text-sm font-semibold text-foreground mb-3">
                Tokens &amp; Cost
              </h3>
              <dl className="grid grid-cols-2 gap-x-6 gap-y-2 text-sm">
                <div>
                  <dt className="text-xs text-muted-foreground">Tokens in</dt>
                  <dd className="font-medium tabular-nums text-foreground">{summary.tokensIn.toLocaleString()}</dd>
                </div>
                <div>
                  <dt className="text-xs text-muted-foreground">Tokens out</dt>
                  <dd className="font-medium tabular-nums text-foreground">{summary.tokensOut.toLocaleString()}</dd>
                </div>
                <div>
                  <dt className="text-xs text-muted-foreground">Model</dt>
                  <dd className="font-mono text-xs truncate text-foreground">{summary.model}</dd>
                </div>
                <div>
                  <dt className="text-xs text-muted-foreground">Cost</dt>
                  <dd className="font-medium tabular-nums text-foreground">{microsToUsd(summary.costUsdMicros)}</dd>
                </div>
                <div>
                  <dt className="text-xs text-muted-foreground">Duration</dt>
                  <dd className="font-medium text-foreground">{formatDuration(summary.startedAt, summary.endedAt)}</dd>
                </div>
                <div>
                  <dt className="text-xs text-muted-foreground">User</dt>
                  <dd className="truncate text-foreground">{summary.username ?? `#${summary.userId}`}</dd>
                </div>
              </dl>
            </div>

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

            <div className="px-6 pt-4 pb-6 flex-1">
              <h3 className="text-sm font-semibold text-foreground mb-3">
                Event log ({detail.events.length})
              </h3>
              {detail.events.length === 0 ? (
                <p className="text-sm text-muted-foreground">No events recorded.</p>
              ) : (
                <ol className="space-y-2">
                  {detail.events.map((raw, i) => {
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
                            <Check className="h-4 w-4 text-emerald-500" />
                          ) : (
                            <span className="h-4 w-4 inline-flex items-center justify-center">
                              <span className="h-1.5 w-1.5 rounded-full bg-muted-foreground" />
                            </span>
                          )}
                        </span>
                        <span className="text-foreground leading-snug">
                          <span className="text-xs font-mono text-muted-foreground mr-2">
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
