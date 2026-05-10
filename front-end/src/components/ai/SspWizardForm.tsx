'use client';
import { useEffect, useState } from 'react';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Textarea } from '@/components/ui/textarea';
import { aiClient } from '@/lib/ai-client';
import { libraryListApi, type LibraryItemSummary } from '@/lib/api/library';
import { toast } from 'sonner';

interface Props {
  organizationId: number;
  onSessionStarted: (sessionId: string) => void;
}

type SourceTab = 'file' | 'paste';
type ProfileMode = 'library' | 'url' | 'skip';

export function SspWizardForm({ organizationId, onSessionStarted }: Props) {
  const [profileMode, setProfileMode] = useState<ProfileMode>('library');
  const [profileLibraryId, setProfileLibraryId] = useState<string>('');
  const [profileUrl, setProfileUrl] = useState('');
  const [libraryProfiles, setLibraryProfiles] = useState<LibraryItemSummary[]>([]);
  const [loadingProfiles, setLoadingProfiles] = useState(false);

  const [tab, setTab] = useState<SourceTab>('file');
  const [file, setFile] = useState<File | null>(null);
  const [pasted, setPasted] = useState('');
  const [running, setRunning] = useState(false);

  useEffect(() => {
    let cancelled = false;
    setLoadingProfiles(true);
    libraryListApi
      .listByOscalType('profile')
      .then((items) => {
        if (!cancelled) setLibraryProfiles(items);
      })
      .catch(() => {
        if (!cancelled) setLibraryProfiles([]);
      })
      .finally(() => {
        if (!cancelled) setLoadingProfiles(false);
      });
    return () => {
      cancelled = true;
    };
  }, []);

  const profileHref =
    profileMode === 'library' && profileLibraryId
      ? `library:${profileLibraryId}`
      : profileMode === 'url' && profileUrl.trim().length > 0
      ? profileUrl.trim()
      : null;

  const profileValid =
    profileMode === 'skip' ||
    (profileMode === 'library' && profileLibraryId !== '') ||
    (profileMode === 'url' && profileUrl.trim().length > 0);

  const sourceValid =
    (tab === 'file' && file !== null) || (tab === 'paste' && pasted.trim().length > 0);

  const canRun = profileValid && sourceValid;

  const onRun = async () => {
    if (!canRun) return;
    setRunning(true);
    try {
      const res =
        tab === 'file' && file
          ? await aiClient.startSessionWithUpload(organizationId, 'SSP', file, { profileHref })
          : await aiClient.startSession({
              organizationId,
              wizardKind: 'SSP',
              mode: 'STREAMING',
              input: pasted,
              profileHref,
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
        <CardTitle>Build SSP from Source</CardTitle>
      </CardHeader>
      <CardContent className="space-y-6">
        <p className="text-sm text-muted-foreground">
          Upload an architecture document, system description, or existing draft SSP. AI will
          extract system characteristics and draft an OSCAL System Security Plan you can review
          and save.
        </p>

        {/* Profile picker */}
        <div className="space-y-3 border rounded-md p-4">
          <Label className="font-medium">Control baseline</Label>
          <div className="space-y-2">
            <label className="flex items-center gap-2 text-sm">
              <input
                type="radio"
                name="profile-mode"
                value="library"
                checked={profileMode === 'library'}
                onChange={() => setProfileMode('library')}
              />
              Pick a profile from your library
            </label>
            {profileMode === 'library' && (
              <select
                aria-label="Profile from library"
                value={profileLibraryId}
                onChange={(e) => setProfileLibraryId(e.target.value)}
                className="w-full rounded border px-3 py-2 text-sm"
                disabled={loadingProfiles}
              >
                <option value="">{loadingProfiles ? 'Loading…' : 'Select a profile'}</option>
                {libraryProfiles.map((p) => (
                  <option key={p.itemId} value={p.itemId}>
                    {p.title}
                    {p.version ? ` (v${p.version})` : ''}
                  </option>
                ))}
              </select>
            )}

            <label className="flex items-center gap-2 text-sm">
              <input
                type="radio"
                name="profile-mode"
                value="url"
                checked={profileMode === 'url'}
                onChange={() => setProfileMode('url')}
              />
              Paste a profile URL
            </label>
            {profileMode === 'url' && (
              <Input
                aria-label="Profile URL"
                value={profileUrl}
                onChange={(e) => setProfileUrl(e.target.value)}
                placeholder="https://example.com/fedramp-moderate-profile.json"
              />
            )}

            <label className="flex items-center gap-2 text-sm">
              <input
                type="radio"
                name="profile-mode"
                value="skip"
                checked={profileMode === 'skip'}
                onChange={() => setProfileMode('skip')}
              />
              Skip — let AI infer controls from the source document
            </label>
          </div>
        </div>

        {/* Source doc */}
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
            <Label htmlFor="ssp-file-upload">Source document</Label>
            <Input
              id="ssp-file-upload"
              type="file"
              accept=".pdf,.docx,.html,.htm,.txt,.md,.odt,.rtf"
              onChange={(e) => setFile(e.target.files?.[0] ?? null)}
            />
            <p className="text-sm text-muted-foreground">
              Architecture write-up, system description, security questionnaire, or existing
              draft SSP. PDF, Word, HTML, plain text, Markdown, OpenDocument, or RTF.
            </p>
          </div>
        ) : (
          <div className="space-y-2">
            <Label htmlFor="ssp-paste-text">Paste source content</Label>
            <Textarea
              id="ssp-paste-text"
              rows={12}
              value={pasted}
              onChange={(e) => setPasted(e.target.value)}
              placeholder="Paste the system description or draft SSP text here…"
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
