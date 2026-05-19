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

export function CatalogWizardForm({ organizationId, onSessionStarted }: Props) {
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
        res = await aiClient.startSessionWithUpload(organizationId, 'CATALOG', file);
      } else if (tab === 'url') {
        res = await aiClient.startSessionWithUrl(organizationId, 'CATALOG', url.trim());
      } else {
        res = await aiClient.startSession({
          organizationId,
          wizardKind: 'CATALOG',
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
        <CardTitle>Build Catalog from Source</CardTitle>
      </CardHeader>
      <CardContent className="space-y-6">
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
            <Label htmlFor="file-upload">Source document</Label>
            <Input
              key="catalog-file-input"
              id="file-upload"
              type="file"
              accept=".pdf,.docx,.html,.htm,.txt,.md,.odt,.rtf"
              onChange={(e) => setFile(e.target.files?.[0] ?? null)}
            />
            <p className="text-sm text-muted-foreground">
              Accepts PDF, Word, HTML, plain text, Markdown, OpenDocument, and RTF.
            </p>
          </div>
        ) : tab === 'paste' ? (
          <div key="paste-tab" className="space-y-2">
            <Label htmlFor="paste-text">Paste source content</Label>
            <Textarea
              id="paste-text"
              rows={12}
              value={pasted}
              onChange={(e) => setPasted(e.target.value)}
              placeholder="Paste the controls publication text here…"
            />
          </div>
        ) : (
          <div key="url-tab" className="space-y-2">
            <Label htmlFor="source-url">Source URL</Label>
            <Input
              key="catalog-url-input"
              id="source-url"
              type="url"
              value={url}
              onChange={(e) => setUrl(e.target.value)}
              placeholder="https://csrc.nist.gov/pubs/sp/800/53/r5/final"
            />
            <p className="text-sm text-muted-foreground">
              We&apos;ll fetch the page (or PDF) server-side and run the wizard against
              its contents. Only http/https URLs to public hosts are allowed.
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
