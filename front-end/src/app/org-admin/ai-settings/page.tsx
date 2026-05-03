'use client';

import { useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Checkbox } from '@/components/ui/checkbox';
import { toast } from 'sonner';
import { aiClient, type AiSettingsResponse } from '@/lib/ai-client';

export default function AiSettingsPage() {
  const router = useRouter();
  const [orgId, setOrgId] = useState<number | null>(null);
  const [settings, setSettings] = useState<AiSettingsResponse | null>(null);
  const [apiKey, setApiKey] = useState('');
  const [model, setModel] = useState('claude-opus-4-7');
  const [acknowledged, setAcknowledged] = useState(false);
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
    if (!orgId || !apiKey || !acknowledged) return;
    try {
      const next = await aiClient.putSettings(orgId, { apiKey, defaultModel: model });
      setSettings(next);
      setApiKey('');
      setAcknowledged(false);
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
            Configure your organization&apos;s Anthropic API key to enable AI-assisted OSCAL authoring.
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
            <details className="text-sm text-muted-foreground">
              <summary className="cursor-pointer select-none hover:text-foreground">
                Don&apos;t have an API key? Click for setup instructions
              </summary>
              <div className="mt-3 space-y-2 rounded-md border bg-muted/40 p-4">
                <ol className="list-decimal pl-5 space-y-1.5">
                  <li>
                    Go to{' '}
                    <a
                      href="https://console.anthropic.com"
                      target="_blank"
                      rel="noopener noreferrer"
                      className="underline text-primary hover:no-underline"
                    >
                      console.anthropic.com
                    </a>{' '}
                    and sign in (or create an account).
                  </li>
                  <li>
                    If you haven&apos;t already, add a payment method under{' '}
                    <strong>Settings → Billing</strong>. Anthropic bills usage directly to this account; OSCAL Hub never sees the bill.
                  </li>
                  <li>
                    Open <strong>Settings → API Keys</strong> and click <strong>Create Key</strong>.
                  </li>
                  <li>
                    Give the key a recognizable name (e.g. <code className="rounded bg-background px-1 py-0.5 text-xs">oscal-hub-prod</code>) and create it. Copy the key shown — it starts with <code className="rounded bg-background px-1 py-0.5 text-xs">sk-ant-</code> and is only displayed once.
                  </li>
                  <li>
                    Paste the key in the field above. OSCAL Hub encrypts it at rest and never displays it back; you only see a fingerprint after saving.
                  </li>
                </ol>
                <p className="pt-2">
                  <strong>Tip:</strong> on the same Anthropic page you can set per-key spend limits — recommended for first-time setup so a runaway prompt can&apos;t spike your bill.
                </p>
              </div>
            </details>
          </div>

          <div className="space-y-2">
            <Label htmlFor="model">Default Model</Label>
            <Input id="model" value={model} onChange={(e) => setModel(e.target.value)} />
          </div>

          <div className="rounded-md border border-amber-200 bg-amber-50 p-4 text-sm dark:border-amber-900 dark:bg-amber-950/40">
            <p className="font-semibold text-amber-900 dark:text-amber-200 mb-2">
              Acknowledgement required
            </p>
            <p className="text-amber-900/90 dark:text-amber-200/90 mb-3">
              By enabling AI features, you acknowledge and agree that:
            </p>
            <ul className="list-disc pl-5 space-y-1 text-amber-900/90 dark:text-amber-200/90 mb-3">
              <li>
                Source documents you submit (PDFs, URLs, narratives, control content) will be sent
                to Anthropic for processing under your organization&apos;s own API key. Do not submit
                data your organization is not authorized to share with a third-party AI vendor.
              </li>
              <li>
                AI-generated output may be inaccurate, incomplete, biased, or include fabricated
                information. You are responsible for reviewing and validating every AI-generated
                artifact before using it for compliance, assessment, or authorization purposes.
              </li>
              <li>
                OSCAL Hub provides AI integration as a convenience and makes no warranties as to
                the accuracy, completeness, suitability, or fitness-for-purpose of any AI output,
                and disclaims all liability for decisions made on the basis of AI-generated content.
              </li>
              <li>
                Token costs and rate limits are billed and enforced by Anthropic against the API
                key you provide; OSCAL Hub does not meter or pay for these calls.
              </li>
            </ul>
            <div className="flex items-start gap-2">
              <Checkbox
                id="ack"
                checked={acknowledged}
                onCheckedChange={(v) => setAcknowledged(v === true)}
              />
              <Label htmlFor="ack" className="text-sm leading-snug font-normal cursor-pointer">
                I acknowledge and accept the responsibilities and disclaimers above on behalf of
                my organization.
              </Label>
            </div>
          </div>

          <div className="flex gap-2">
            <Button onClick={onSave} disabled={!apiKey || !acknowledged}>
              Save
            </Button>
            {settings?.enabled && (
              <Button variant="destructive" onClick={onDisable}>
                Disable
              </Button>
            )}
          </div>
        </CardContent>
      </Card>
    </div>
  );
}
