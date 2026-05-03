'use client';

import { useState } from 'react';
import { Button } from '@/components/ui/button';
import { Alert, AlertDescription } from '@/components/ui/alert';
import { Badge } from '@/components/ui/badge';
import { CheckCircle2, AlertCircle, Loader2, GitMerge } from 'lucide-react';
import { JsonPreview } from './JsonPreview';
import { apiClient } from '@/lib/api-client';

interface ResolvePreviewPanelProps {
  /** Stringified profile JSON wrapped: `{ "profile": { ... } }` */
  jsonContent: string;
}

export function ResolvePreviewPanel({ jsonContent }: ResolvePreviewPanelProps) {
  const [running, setRunning] = useState(false);
  const [result, setResult] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [controlCount, setControlCount] = useState<number | null>(null);

  const run = async () => {
    setRunning(true);
    setError(null);
    setResult(null);
    setControlCount(null);
    try {
      const r = await apiClient.resolveProfile({
        profileContent: jsonContent,
        format: 'json',
      });
      if (r.success && r.resolvedCatalog) {
        setResult(r.resolvedCatalog);
        setControlCount(r.controlCount ?? null);
      } else {
        setError(r.error ?? 'Resolution returned no catalog.');
      }
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Resolution failed');
    } finally {
      setRunning(false);
    }
  };

  let parsed: unknown = null;
  if (result) {
    try {
      parsed = JSON.parse(result);
    } catch {
      parsed = { rawText: result };
    }
  }

  return (
    <div className="rounded-md border p-3 space-y-3 bg-card">
      <div className="flex items-center justify-between gap-2">
        <div className="flex items-center gap-2">
          <GitMerge className="h-4 w-4 text-primary" />
          <div>
            <p className="text-sm font-semibold">Resolve preview</p>
            <p className="text-xs text-muted-foreground">
              Run profile resolution to see the merged catalog this profile would produce.
            </p>
          </div>
        </div>
        <Button type="button" size="sm" onClick={run} disabled={running}>
          {running ? (
            <Loader2 className="h-3.5 w-3.5 mr-1 animate-spin" />
          ) : (
            <GitMerge className="h-3.5 w-3.5 mr-1" />
          )}
          {running ? 'Resolving…' : 'Resolve'}
        </Button>
      </div>

      {error && (
        <Alert variant="destructive">
          <AlertCircle className="h-4 w-4" />
          <AlertDescription>{error}</AlertDescription>
        </Alert>
      )}

      {result && (
        <div className="space-y-2">
          <Alert>
            <CheckCircle2 className="h-4 w-4" />
            <AlertDescription className="flex items-center gap-2">
              <span>Resolved successfully.</span>
              {controlCount != null && (
                <Badge variant="outline" className="font-mono text-xs">
                  {controlCount} controls
                </Badge>
              )}
            </AlertDescription>
          </Alert>
          <JsonPreview value={parsed} filename="resolved-catalog.json" maxHeight="400px" />
        </div>
      )}
    </div>
  );
}
