'use client';
import { useEffect, useState } from 'react';
import { useParams, useRouter } from 'next/navigation';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { aiClient, type WizardKind } from '@/lib/ai-client';
import { useAiSession, type AiEvent } from '@/hooks/useAiSession';
import { toast } from 'sonner';
import { CatalogWizardForm } from '@/components/ai/CatalogWizardForm';
import { ChevronLeft, Check, Loader2, AlertCircle, Sparkles } from 'lucide-react';

const WIZARD_TITLES: Record<WizardKind, string> = {
  SMOKE: 'Smoke Test',
  CATALOG: 'Build Catalog from Source',
  PROFILE: 'Build Profile',
  COMPONENT_DEF: 'Build Component-definition',
  SSP: 'Draft SSP',
  POAM: 'Draft POA&M',
  BUILDER_ASSIST: 'Builder Author Assist',
};

// Friendly label for an event payload — what the user actually sees in the step list.
function describeEvent(e: AiEvent): string {
  if (e.type === 'progress') return (e.data as { message?: string }).message ?? 'Working…';
  if (e.type === 'tool_call') {
    const t = (e.data as { tool?: string }).tool;
    return `Calling ${t ?? 'tool'}…`;
  }
  if (e.type === 'tool_result') {
    const t = (e.data as { tool?: string; ok?: boolean }).tool;
    const ok = (e.data as { ok?: boolean }).ok;
    return `${t ?? 'tool'} returned ${ok ? 'ok' : 'errors'}`;
  }
  if (e.type === 'chunk') return ((e.data as { text?: string }).text ?? '').slice(0, 200);
  if (e.type === 'awaiting_input') return 'Waiting for your input';
  if (e.type === 'partial_document') return 'Document update';
  if (e.type === 'complete') return 'Complete';
  if (e.type === 'error') return (e.data as { message?: string }).message ?? 'Error';
  return e.type;
}

// We only render progress / tool_call / tool_result / awaiting_input / chunk events
// as steps. complete/error are status-level and drive the panel state.
const STEP_EVENT_TYPES: AiEvent['type'][] = [
  'progress',
  'tool_call',
  'tool_result',
  'awaiting_input',
];

export default function WizardRunPage() {
  const params = useParams<{ kind: string }>();
  const wizardKind = (params.kind?.toUpperCase() ?? 'SMOKE') as WizardKind;
  const router = useRouter();

  const [orgId, setOrgId] = useState<number | null>(null);
  const [input, setInput] = useState('Reply with the single word OK.');
  const [sessionId, setSessionId] = useState<string | null>(null);
  const session = useAiSession(sessionId);

  useEffect(() => {
    const stored = typeof window !== 'undefined' ? localStorage.getItem('user') : null;
    if (stored) setOrgId((JSON.parse(stored) as { organizationId?: number }).organizationId ?? null);
  }, []);

  useEffect(() => {
    if (
      wizardKind === 'CATALOG' &&
      session.isComplete &&
      session.finalDocument != null &&
      sessionId
    ) {
      sessionStorage.setItem(`aiDraft:${sessionId}`, JSON.stringify(session.finalDocument));
      router.push(`/build?section=catalogs&aiDraft=${sessionId}`);
    }
  }, [wizardKind, session.isComplete, session.finalDocument, sessionId, router]);

  const start = async () => {
    if (!orgId) return;
    try {
      const res = await aiClient.startSession({
        organizationId: orgId,
        wizardKind,
        mode: 'STREAMING',
        input,
      });
      setSessionId(res.sessionId);
    } catch (err) {
      toast.error('Failed to start: ' + (err instanceof Error ? err.message : 'unknown'));
    }
  };

  const steps = session.events.filter((e) => STEP_EVENT_TYPES.includes(e.type));

  return (
    <div className="container mx-auto py-8 max-w-3xl space-y-4">
      <button
        onClick={() => router.push('/ai/wizard')}
        className="flex items-center text-gray-600 dark:text-gray-400 hover:text-gray-900 dark:hover:text-white mb-2"
      >
        <ChevronLeft className="h-4 w-4 mr-1" />
        Back to AI Wizard
      </button>

      <div className="flex items-center gap-3">
        <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-indigo-100 dark:bg-indigo-900/40">
          <Sparkles className="h-5 w-5 text-indigo-600 dark:text-indigo-400" />
        </div>
        <h1 className="text-2xl font-semibold">{WIZARD_TITLES[wizardKind] ?? wizardKind}</h1>
      </div>

      {!sessionId && wizardKind === 'CATALOG' && orgId != null && (
        <CatalogWizardForm organizationId={orgId} onSessionStarted={setSessionId} />
      )}

      {!sessionId && wizardKind === 'SMOKE' && (
        <Card>
          <CardHeader>
            <CardTitle>Smoke test input</CardTitle>
          </CardHeader>
          <CardContent className="space-y-3">
            <Input value={input} onChange={(e) => setInput(e.target.value)} />
            <Button onClick={start} disabled={!orgId}>
              Run
            </Button>
          </CardContent>
        </Card>
      )}

      {sessionId && (
        <Card>
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              {!session.isComplete && (
                <Loader2 className="h-4 w-4 animate-spin text-indigo-500" />
              )}
              {session.isComplete && !session.error && (
                <Check className="h-4 w-4 text-green-600" />
              )}
              {session.error && <AlertCircle className="h-4 w-4 text-destructive" />}
              {!session.isComplete && 'Working with Claude…'}
              {session.isComplete && !session.error && 'Done'}
              {session.error && 'Failed'}
            </CardTitle>
          </CardHeader>
          <CardContent className="space-y-4">
            {steps.length === 0 && !session.error && (
              <div className="flex items-center gap-2 text-sm text-muted-foreground">
                <Loader2 className="h-4 w-4 animate-spin" />
                Connecting to Anthropic and reading your input…
              </div>
            )}

            <ol className="space-y-2">
              {steps.map((e, i) => {
                const isLast = i === steps.length - 1;
                const inProgress = isLast && !session.isComplete;
                return (
                  <li key={i} className="flex items-start gap-3">
                    <div className="flex-shrink-0 mt-0.5">
                      {inProgress ? (
                        <Loader2 className="h-4 w-4 animate-spin text-indigo-500" />
                      ) : (
                        <Check className="h-4 w-4 text-green-600" />
                      )}
                    </div>
                    <div
                      className={
                        'text-sm leading-snug ' +
                        (inProgress ? 'text-foreground font-medium' : 'text-muted-foreground')
                      }
                    >
                      {describeEvent(e)}
                    </div>
                  </li>
                );
              })}
            </ol>

            {session.error && (
              <div className="rounded-md border border-destructive/30 bg-destructive/10 p-3 text-sm text-destructive">
                <div className="font-medium mb-1">Error</div>
                <div className="font-mono text-xs whitespace-pre-wrap">{session.error}</div>
              </div>
            )}

            {session.isComplete && !session.error && session.finalDocument != null && wizardKind !== 'CATALOG' && (
              <details className="rounded-md border bg-muted/30 p-3 text-sm">
                <summary className="cursor-pointer font-medium">Final output</summary>
                <pre className="mt-2 text-xs overflow-auto max-h-80">
                  {JSON.stringify(session.finalDocument, null, 2)}
                </pre>
              </details>
            )}

            <div className="flex gap-2">
              {!session.isComplete && (
                <Button variant="outline" size="sm" onClick={session.cancel}>
                  Cancel
                </Button>
              )}
              {session.isComplete && (
                <Button variant="outline" size="sm" onClick={() => setSessionId(null)}>
                  Run another
                </Button>
              )}
            </div>
          </CardContent>
        </Card>
      )}
    </div>
  );
}
