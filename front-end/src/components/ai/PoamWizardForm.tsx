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

type Tab = 'file' | 'paste' | 'url';

export function PoamWizardForm({ organizationId, onSessionStarted }: Props) {
  const [tab, setTab] = useState<Tab>('file');
  const [file, setFile] = useState<File | null>(null);
  const [pasted, setPasted] = useState('');
  const [url, setUrl] = useState('');
  const [running, setRunning] = useState(false);

  const canRun =
    (tab === 'file' && !!file) ||
    (tab === 'paste' && pasted.trim().length > 0) ||
    (tab === 'url' && url.trim().length > 0);

  const onRun = async () => {
    if (!canRun) return;
    setRunning(true);
    try {
      let res;
      if (tab === 'file' && file) {
        res = await aiClient.startSessionWithUpload(organizationId, 'POAM', file);
      } else if (tab === 'url') {
        res = await aiClient.startSessionWithUrl(organizationId, 'POAM', url.trim());
      } else {
        res = await aiClient.startSession({
          organizationId,
          wizardKind: 'POAM',
          mode: 'STREAMING',
          input: pasted,
        });
      }
      onSessionStarted(res.sessionId);
    } catch (err) {
      toast.error('Failed to start: ' + (err instanceof Error ? err.message : 'unknown'));
      setRunning(false);
    }
  };

  return (
    <Card>
      <CardHeader>
        <CardTitle>Build POA&M from Source</CardTitle>
      </CardHeader>
      <CardContent className="space-y-6">
        <p className="text-sm text-muted-foreground">
          Drop a FedRAMP POA&M spreadsheet (.xlsx), a CSV export, a penetration-test report
          (PDF / Word), a vulnerability scan summary, paste plain text, or link a public URL.
          AI extracts each weakness as an OSCAL POA&M item with severity, status, and a draft
          remediation narrative — review and edit before saving.
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
          <button
            onClick={() => setTab('url')}
            className={`px-4 py-2 text-sm font-medium ${tab === 'url' ? 'border-b-2 border-primary text-foreground' : 'text-muted-foreground'}`}
          >
            From URL
          </button>
        </div>

        {tab === 'file' ? (
          <div key="file-tab" className="space-y-2">
            <Label htmlFor="poam-file-upload">Source document</Label>
            <Input
              key="poam-file-input"
              id="poam-file-upload"
              type="file"
              accept=".xlsx,.xls,.csv,.pdf,.docx,.html,.htm,.txt,.md,.odt,.rtf"
              onChange={(e) => setFile(e.target.files?.[0] ?? null)}
            />
            <p className="text-sm text-muted-foreground">
              Excel, CSV, PDF, Word, HTML, plain text, Markdown, OpenDocument, or RTF.
              FedRAMP POA&M templates and pen-test reports both work.
            </p>
          </div>
        ) : tab === 'paste' ? (
          <div key="paste-tab" className="space-y-2">
            <Label htmlFor="poam-paste-text">Paste source content</Label>
            <Textarea
              id="poam-paste-text"
              rows={12}
              value={pasted}
              onChange={(e) => setPasted(e.target.value)}
              placeholder="Paste pen-test findings, vulnerability list, or POA&M tracker text…"
            />
          </div>
        ) : (
          <div key="url-tab" className="space-y-2">
            <Label htmlFor="poam-source-url">Source URL</Label>
            <Input
              key="poam-url-input"
              id="poam-source-url"
              type="url"
              value={url}
              onChange={(e) => setUrl(e.target.value)}
              placeholder="https://example.com/pentest-report.pdf"
            />
            <p className="text-sm text-muted-foreground">
              We&apos;ll fetch the document server-side. Only http/https URLs to
              public hosts are allowed.
            </p>
          </div>
        )}

        <Button onClick={onRun} disabled={!canRun || running}>
          {running ? 'Starting…' : 'Run AI Wizard'}
        </Button>
      </CardContent>
    </Card>
  );
}
