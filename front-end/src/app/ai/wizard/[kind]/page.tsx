'use client';
import { useEffect, useState } from 'react';
import { useParams } from 'next/navigation';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { aiClient, type WizardKind } from '@/lib/ai-client';
import { useAiSession } from '@/hooks/useAiSession';
import { toast } from 'sonner';

export default function WizardRunPage() {
  const params = useParams<{ kind: string }>();
  const wizardKind = (params.kind?.toUpperCase() ?? 'SMOKE') as WizardKind;

  const [orgId, setOrgId] = useState<number | null>(null);
  const [input, setInput] = useState('Reply with the single word OK.');
  const [sessionId, setSessionId] = useState<string | null>(null);
  const session = useAiSession(sessionId);

  useEffect(() => {
    const stored = typeof window !== 'undefined' ? localStorage.getItem('user') : null;
    if (stored) setOrgId((JSON.parse(stored) as { organizationId?: number }).organizationId ?? null);
  }, []);

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

  return (
    <div className="container mx-auto py-8 max-w-3xl space-y-4">
      <h1 className="text-2xl font-semibold">{wizardKind} Wizard</h1>

      {!sessionId && (
        <Card>
          <CardHeader>
            <CardTitle>Input</CardTitle>
          </CardHeader>
          <CardContent className="space-y-3">
            <Input value={input} onChange={(e) => setInput(e.target.value)} />
            <Button onClick={start} disabled={!orgId}>Run</Button>
          </CardContent>
        </Card>
      )}

      {sessionId && (
        <Card>
          <CardHeader>
            <CardTitle>Session {sessionId}</CardTitle>
          </CardHeader>
          <CardContent className="space-y-3">
            <ul className="text-sm font-mono space-y-1 max-h-96 overflow-auto">
              {session.events.map((e, i) => (
                <li key={i}>
                  <strong>[{e.type}]</strong> {JSON.stringify(e.data)}
                </li>
              ))}
            </ul>
            {session.error && <div className="text-destructive">Error: {session.error}</div>}
            {session.isComplete && session.finalDocument != null && (
              <pre className="text-xs bg-muted p-3 rounded max-h-64 overflow-auto">
                {JSON.stringify(session.finalDocument, null, 2)}
              </pre>
            )}
            {!session.isComplete && (
              <Button variant="outline" onClick={session.cancel}>Cancel</Button>
            )}
          </CardContent>
        </Card>
      )}
    </div>
  );
}
