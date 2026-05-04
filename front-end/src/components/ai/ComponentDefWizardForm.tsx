'use client';
import { useState } from 'react';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Textarea } from '@/components/ui/textarea';
import { aiClient } from '@/lib/ai-client';
import { toast } from 'sonner';

interface Props {
  organizationId: number;
  onSessionStarted: (sessionId: string) => void;
}

type Tab = 'file' | 'paste';

export function ComponentDefWizardForm({ organizationId, onSessionStarted }: Props) {
  const [tab, setTab] = useState<Tab>('file');
  const [file, setFile] = useState<File | null>(null);
  const [pasted, setPasted] = useState('');
  const [running, setRunning] = useState(false);

  const canRun = (tab === 'file' && file) || (tab === 'paste' && pasted.trim().length > 0);

  const onRun = async () => {
    if (!canRun) return;
    setRunning(true);
    try {
      const res = tab === 'file' && file
        ? await aiClient.startSessionWithUpload(organizationId, 'COMPONENT_DEF', file)
        : await aiClient.startSession({
            organizationId,
            wizardKind: 'COMPONENT_DEF',
            mode: 'STREAMING',
            input: pasted,
          });
      onSessionStarted(res.sessionId);
    } catch (err) {
      toast.error('Failed to start: ' + (err instanceof Error ? err.message : 'unknown'));
      setRunning(false);
    }
  };

  return (
    <Card>
      <CardHeader>
        <CardTitle>Build Component-definition from Source</CardTitle>
      </CardHeader>
      <CardContent className="space-y-6">
        <p className="text-sm text-muted-foreground">
          Upload or paste a STIG, CIS Benchmark, configuration guide, or similar source document.
          The AI will extract components, capabilities, and control implementations to produce an
          OSCAL component-definition.
        </p>

        <div className="flex gap-2 border-b">
          <button
            onClick={() => setTab('file')}
            className={`px-4 py-2 text-sm font-medium ${tab === 'file' ? 'border-b-2 border-primary text-foreground' : 'text-muted-foreground'}`}
          >
            Upload file
          </button>
          <button
            onClick={() => setTab('paste')}
            className={`px-4 py-2 text-sm font-medium ${tab === 'paste' ? 'border-b-2 border-primary text-foreground' : 'text-muted-foreground'}`}
          >
            Paste text
          </button>
        </div>

        {tab === 'file' ? (
          <div className="space-y-2">
            <Label htmlFor="file-upload">Source document</Label>
            <Input
              id="file-upload"
              type="file"
              accept=".pdf,.docx,.html,.htm,.txt,.md,.odt,.rtf"
              onChange={(e) => setFile(e.target.files?.[0] ?? null)}
            />
            <p className="text-sm text-muted-foreground">
              Accepts PDF, Word, HTML, plain text, Markdown, OpenDocument, and RTF.
            </p>
          </div>
        ) : (
          <div className="space-y-2">
            <Label htmlFor="paste-text">Paste source content</Label>
            <Textarea
              id="paste-text"
              rows={12}
              value={pasted}
              onChange={(e) => setPasted(e.target.value)}
              placeholder="Paste the STIG, CIS Benchmark, or configuration guide text here…"
            />
          </div>
        )}

        <Button onClick={onRun} disabled={!canRun || running}>
          {running ? 'Starting…' : 'Run AI Wizard'}
        </Button>
      </CardContent>
    </Card>
  );
}
