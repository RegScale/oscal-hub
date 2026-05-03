'use client';

import { useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { toast } from 'sonner';
import { aiClient, type AiSettingsResponse } from '@/lib/ai-client';

export default function AiSettingsPage() {
  const router = useRouter();
  const [orgId, setOrgId] = useState<number | null>(null);
  const [settings, setSettings] = useState<AiSettingsResponse | null>(null);
  const [apiKey, setApiKey] = useState('');
  const [model, setModel] = useState('claude-opus-4-7');
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const stored = typeof window !== 'undefined' ? localStorage.getItem('user') : null;
    if (!stored) {
      router.push('/');
      return;
    }
    const userData = JSON.parse(stored) as { orgRole?: string; globalRole?: string; organizationId?: number };
    const isAdmin = userData.orgRole === 'ORG_ADMIN' || userData.globalRole === 'SUPER_ADMIN';
    if (!isAdmin || !userData.organizationId) {
      router.push('/');
      return;
    }
    setOrgId(userData.organizationId);
    aiClient.getSettings(userData.organizationId).then((s) => {
      setSettings(s);
      setModel(s.defaultModel);
      setLoading(false);
    });
  }, [router]);

  const onSave = async () => {
    if (!orgId || !apiKey) return;
    try {
      const next = await aiClient.putSettings(orgId, { apiKey, defaultModel: model });
      setSettings(next);
      setApiKey('');
      toast.success('AI features enabled. Key fingerprint saved.');
    } catch (err) {
      toast.error('Failed to save: ' + (err instanceof Error ? err.message : 'unknown'));
    }
  };

  const onDisable = async () => {
    if (!orgId) return;
    if (!confirm('Disable AI features and clear the stored key?')) return;
    try {
      await aiClient.disable(orgId);
      const refreshed = await aiClient.getSettings(orgId);
      setSettings(refreshed);
      toast.success('AI disabled and key cleared.');
    } catch (err) {
      toast.error('Failed to disable: ' + (err instanceof Error ? err.message : 'unknown'));
    }
  };

  if (loading) return <div className="p-8">Loading…</div>;

  return (
    <div className="container mx-auto py-8 max-w-2xl">
      <Card>
        <CardHeader>
          <CardTitle>AI Features</CardTitle>
          <CardDescription>
            Configure your organization's Anthropic API key to enable AI-assisted OSCAL authoring.
            Your key is encrypted at rest and never returned to the browser.
          </CardDescription>
        </CardHeader>
        <CardContent className="space-y-6">
          <div className="rounded-md bg-muted p-4 text-sm">
            <strong>Status:</strong>{' '}
            {settings?.enabled ? `Enabled — fingerprint ${settings.fingerprint}` : 'Disabled'}
          </div>

          <div className="space-y-2">
            <Label htmlFor="api-key">Anthropic API Key</Label>
            <Input
              id="api-key"
              type="password"
              autoComplete="off"
              value={apiKey}
              onChange={(e) => setApiKey(e.target.value)}
              placeholder="sk-ant-…"
            />
          </div>

          <div className="space-y-2">
            <Label htmlFor="model">Default Model</Label>
            <Input id="model" value={model} onChange={(e) => setModel(e.target.value)} />
          </div>

          <div className="flex gap-2">
            <Button onClick={onSave} disabled={!apiKey}>Save</Button>
            {settings?.enabled && (
              <Button variant="destructive" onClick={onDisable}>Disable</Button>
            )}
          </div>
        </CardContent>
      </Card>
    </div>
  );
}
