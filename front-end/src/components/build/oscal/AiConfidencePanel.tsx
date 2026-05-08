'use client';
import { useMemo, useState } from 'react';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { Sparkles } from 'lucide-react';

interface ImplementedRequirement {
  uuid?: string;
  'control-id'?: string;
  description?: string;
  props?: Array<{ name?: string; ns?: string; value?: string }>;
}

interface AiConfidenceCounts {
  high: number;
  medium: number;
  low: number;
  total: number;
}

export function readConfidenceCounts(body: unknown): AiConfidenceCounts {
  const empty: AiConfidenceCounts = { high: 0, medium: 0, low: 0, total: 0 };
  if (!body || typeof body !== 'object') return empty;
  const ctrlImpl = (body as Record<string, unknown>)['control-implementation'];
  if (!ctrlImpl || typeof ctrlImpl !== 'object') return empty;
  const reqs = (ctrlImpl as Record<string, unknown>)['implemented-requirements'];
  if (!Array.isArray(reqs)) return empty;
  const counts = { ...empty };
  for (const r of reqs as ImplementedRequirement[]) {
    const prop = (r.props ?? []).find((p) => p?.name === 'ai-confidence');
    if (!prop) continue;
    counts.total += 1;
    if (prop.value === 'high') counts.high += 1;
    else if (prop.value === 'medium') counts.medium += 1;
    else if (prop.value === 'low') counts.low += 1;
  }
  return counts;
}

interface Props {
  body: unknown;
  /**
   * Optional callback invoked with a control-id when the user wants to
   * locate that control's narrative in the JSON editor. Provided by
   * OscalDocumentWizard which knows about the Monaco instance.
   */
  onLocate?: (controlId: string) => void;
}

export function AiConfidencePanel({ body, onLocate }: Props) {
  const counts = useMemo(() => readConfidenceCounts(body), [body]);
  const lowEntries: ImplementedRequirement[] = useMemo(() => {
    if (!body || typeof body !== 'object') return [];
    const ctrlImpl = (body as Record<string, unknown>)['control-implementation'];
    if (!ctrlImpl || typeof ctrlImpl !== 'object') return [];
    const reqs = (ctrlImpl as Record<string, unknown>)['implemented-requirements'];
    if (!Array.isArray(reqs)) return [];
    return (reqs as ImplementedRequirement[]).filter((r) => {
      const prop = (r.props ?? []).find((p) => p?.name === 'ai-confidence');
      return prop?.value === 'low';
    });
  }, [body]);
  const [showLow, setShowLow] = useState(false);

  if (counts.total === 0) return null;

  return (
    <div className="rounded-md border bg-indigo-50/40 dark:bg-indigo-950/20 p-3 space-y-2">
      <div className="flex items-center gap-2">
        <Sparkles className="h-4 w-4 text-indigo-600 dark:text-indigo-400" />
        <span className="text-sm font-medium">AI confidence</span>
        <Badge variant="outline">{counts.high} high</Badge>
        <Badge variant="outline">{counts.medium} medium</Badge>
        <Badge variant="outline" className="border-amber-500 text-amber-700 dark:text-amber-300">
          {counts.low} low
        </Badge>
        <span className="text-sm text-muted-foreground ml-1">/ {counts.total} controls drafted by AI</span>
        {counts.low > 0 && (
          <Button
            size="sm"
            variant="outline"
            className="ml-auto"
            onClick={() => setShowLow((v) => !v)}
          >
            {showLow ? 'Hide low confidence' : 'Review low confidence'}
          </Button>
        )}
      </div>

      {showLow && (
        <div className="space-y-2 max-h-72 overflow-auto">
          {lowEntries.map((r, i) => (
            <div key={r.uuid ?? i} className="rounded border bg-background p-2 text-sm">
              <div className="flex items-center gap-2 mb-1">
                <code className="text-xs font-mono">{r['control-id']}</code>
                {onLocate && (
                  <Button
                    size="sm"
                    variant="ghost"
                    onClick={() => onLocate(r['control-id'] ?? '')}
                  >
                    Find in editor
                  </Button>
                )}
              </div>
              <div className="text-xs text-muted-foreground line-clamp-3">{r.description}</div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
