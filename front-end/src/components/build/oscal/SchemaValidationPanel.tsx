'use client';

import { useState } from 'react';
import { Button } from '@/components/ui/button';
import { Alert, AlertDescription } from '@/components/ui/alert';
import { Badge } from '@/components/ui/badge';
import { CheckCircle2, AlertCircle, Loader2, ShieldCheck } from 'lucide-react';
import { apiClient } from '@/lib/api-client';
import type { OscalModelType, ValidationResult, ValidationError } from '@/types/oscal';

interface SchemaValidationPanelProps {
  /** Stringified OSCAL JSON (wrapped, e.g. `{ "catalog": { ... } }`). */
  jsonContent: string;
  modelType: OscalModelType;
}

export function SchemaValidationPanel({ jsonContent, modelType }: SchemaValidationPanelProps) {
  const [running, setRunning] = useState(false);
  const [result, setResult] = useState<ValidationResult | null>(null);
  const [error, setError] = useState<string | null>(null);

  const run = async () => {
    setRunning(true);
    setError(null);
    setResult(null);
    try {
      const r = await apiClient.validate(jsonContent, modelType, 'json');
      setResult(r);
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Validation failed');
    } finally {
      setRunning(false);
    }
  };

  return (
    <div className="rounded-md border p-3 space-y-3 bg-card">
      <div className="flex items-center justify-between gap-2">
        <div className="flex items-center gap-2">
          <ShieldCheck className="h-4 w-4 text-primary" />
          <div>
            <p className="text-sm font-semibold">OSCAL schema validation</p>
            <p className="text-xs text-muted-foreground">
              Run the official OSCAL constraints (server-side via liboscal-java).
            </p>
          </div>
        </div>
        <Button type="button" size="sm" onClick={run} disabled={running}>
          {running ? <Loader2 className="h-3.5 w-3.5 mr-1 animate-spin" /> : <ShieldCheck className="h-3.5 w-3.5 mr-1" />}
          {running ? 'Validating…' : 'Validate'}
        </Button>
      </div>

      {error && (
        <Alert variant="destructive">
          <AlertCircle className="h-4 w-4" />
          <AlertDescription>{error}</AlertDescription>
        </Alert>
      )}

      {result && <ValidationDisplay result={result} />}
    </div>
  );
}

function ValidationDisplay({ result }: { result: ValidationResult }) {
  const errorCount = result.errors?.length ?? 0;
  const warningCount = result.warnings?.length ?? 0;

  return (
    <div className="space-y-2">
      <div className="flex items-center gap-2">
        {result.valid ? (
          <Alert className="flex-1">
            <CheckCircle2 className="h-4 w-4" />
            <AlertDescription>Document is valid against the OSCAL schema.</AlertDescription>
          </Alert>
        ) : (
          <Alert variant="destructive" className="flex-1">
            <AlertCircle className="h-4 w-4" />
            <AlertDescription>
              {errorCount} error{errorCount === 1 ? '' : 's'}
              {warningCount > 0 ? `, ${warningCount} warning${warningCount === 1 ? '' : 's'}` : ''}.
            </AlertDescription>
          </Alert>
        )}
      </div>
      {(errorCount > 0 || warningCount > 0) && (
        <div className="rounded-md border bg-muted/20 p-2 max-h-64 overflow-auto space-y-1">
          {result.errors?.map((e, i) => (
            <ValidationLine key={`e${i}`} item={e} />
          ))}
          {result.warnings?.map((w, i) => (
            <ValidationLine key={`w${i}`} item={w} />
          ))}
        </div>
      )}
    </div>
  );
}

function ValidationLine({ item }: { item: ValidationError }) {
  const variant =
    item.severity === 'error' ? 'destructive' : item.severity === 'warning' ? 'secondary' : 'outline';
  return (
    <div className="flex items-start gap-2 text-xs">
      <Badge variant={variant} className="text-xs flex-shrink-0">
        {item.severity}
      </Badge>
      <div className="flex-1">
        <p>{item.message}</p>
        {(item.path || item.line != null) && (
          <p className="font-mono text-[10px] text-muted-foreground">
            {item.path ?? ''}
            {item.line != null ? ` (line ${item.line}${item.column != null ? `:${item.column}` : ''})` : ''}
          </p>
        )}
      </div>
    </div>
  );
}
