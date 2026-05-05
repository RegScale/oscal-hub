'use client';

import { useEffect, useMemo, useState } from 'react';
import { useRouter } from 'next/navigation';
import { toast } from 'sonner';
import ProtectedRoute from '@/components/ProtectedRoute';
import { AiFeatureGate } from '@/components/ai/AiFeatureGate';
import { HelpButton } from '@/components/HelpButton';
import { Card } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select';
import { useRuleGenSession } from './useRuleGenSession';
import { RuleGenChat } from './RuleGenChat';
import { RuleProposalView } from './RuleProposalView';
import { TestMatrix } from './TestMatrix';
import type { OscalModelType } from '@/types/rule-gen';

const MODEL_OPTIONS: { value: OscalModelType; label: string }[] = [
  { value: 'catalog', label: 'Catalog' },
  { value: 'profile', label: 'Profile' },
  { value: 'system-security-plan', label: 'System Security Plan' },
  { value: 'component-definition', label: 'Component Definition' },
  { value: 'assessment-plan', label: 'Assessment Plan' },
  { value: 'assessment-results', label: 'Assessment Results' },
  { value: 'plan-of-action-and-milestones', label: 'POA&M' },
];

const STARTER_PROMPTS = [
  'Every control in a catalog must have a non-empty title.',
  'All implemented requirements in an SSP must reference a control id.',
  'Profile imports must reference a catalog by UUID.',
];

const FLAG_DISABLED = process.env.NEXT_PUBLIC_ENABLE_AI_RULE_GEN === 'false';

export default function AiGenerateRulePage() {
  return (
    <ProtectedRoute>
      <Inner />
    </ProtectedRoute>
  );
}

function Inner() {
  const router = useRouter();
  const session = useRuleGenSession();
  const [model, setModel] = useState<OscalModelType | ''>('');
  const [orgId, setOrgId] = useState<number | null>(null);
  const [ruleId, setRuleId] = useState('');

  useEffect(() => {
    if (FLAG_DISABLED) {
      router.replace('/rules/custom');
      return;
    }
    const stored =
      typeof window !== 'undefined' ? localStorage.getItem('user') : null;
    if (stored) {
      setOrgId(
        (JSON.parse(stored) as { organizationId?: number }).organizationId ??
          null,
      );
    }
  }, [router]);

  // Surface silent session-restart recoveries (backend restart / TTL).
  useEffect(() => {
    if (session.recovered) {
      toast('Session was reset — your prior turns were not preserved.', {
        description: 'The backend forgot the conversation; we resent your last message on a fresh session.',
      });
    }
  }, [session.recovered]);

  const begin = async () => {
    if (!model || !orgId) return;
    await session.start(orgId, model);
  };

  const onSave = async () => {
    if (!ruleId.trim()) {
      toast.error('Please choose a rule id');
      return;
    }
    try {
      await session.save(ruleId.trim());
      toast.success('Rule saved');
      router.push('/rules/custom');
    } catch (e) {
      toast.error('Save failed: ' + (e instanceof Error ? e.message : String(e)));
    }
  };

  const matrixClean = useMemo(
    () => (session.latest?.testResults ?? []).every((r) => r.passed),
    [session.latest?.testResults],
  );

  // The save button is gated only on whether we have a parseable proposal
  // — synthetic test failures are informational, not blocking. The backend
  // only returns phase='proposal' when the constraint XML actually compiles
  // against the Metaschema spec, so this is the correct gate.
  const proposalAvailable = session.latest?.phase === 'proposal'
    && (session.latest?.proposal != null);

  if (FLAG_DISABLED) return null;

  return (
    <AiFeatureGate
      organizationId={orgId}
      fallback={
        <div className="p-8 text-muted-foreground">
          AI features are disabled. Ask your org admin to add an Anthropic API key in
          Org Admin → AI Settings.
        </div>
      }
    >
      {!session.sessionId ? (
        <div className="container mx-auto p-6 max-w-2xl">
          <div className="flex items-center gap-2 mb-4">
            <h1 className="text-2xl font-semibold">Generate a rule with AI</h1>
            <HelpButton slug="ai-rule-generator" />
          </div>
          <Card className="p-4 space-y-4">
            <div>
              <label className="text-sm font-medium block mb-1">OSCAL model</label>
              <Select
                value={model}
                onValueChange={(v) => setModel(v as OscalModelType)}
              >
                <SelectTrigger>
                  <SelectValue placeholder="Pick a model" />
                </SelectTrigger>
                <SelectContent>
                  {MODEL_OPTIONS.map((m) => (
                    <SelectItem key={m.value} value={m.value}>
                      {m.label}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
            <div className="text-sm text-muted-foreground">
              <div className="font-medium mb-1">Examples to try:</div>
              <ul className="list-disc pl-5 space-y-1">
                {STARTER_PROMPTS.map((p) => (
                  <li key={p}>{p}</li>
                ))}
              </ul>
            </div>
            <Button onClick={begin} disabled={!model || !orgId}>
              Start
            </Button>
            {session.error && (
              <p className="text-sm text-red-600">{session.error}</p>
            )}
          </Card>
        </div>
      ) : (
        <div className="container mx-auto p-4 grid grid-cols-1 lg:grid-cols-2 gap-4 h-[calc(100vh-6rem)]">
          <div className="flex flex-col h-full min-h-0">
            <h2 className="text-lg font-semibold mb-2">
              Conversation
              {session.latest && (
                <span className="ml-3 text-xs text-muted-foreground">
                  tokens in: {session.latest.totalTokensIn} / out:{' '}
                  {session.latest.totalTokensOut}
                </span>
              )}
            </h2>
            <RuleGenChat
              entries={session.chat}
              loading={session.loading}
              disabled={proposalAvailable}
              placeholder="Describe what the rule should enforce…"
              onSend={session.send}
            />
            {session.error && (
              <p className="text-sm text-red-600 mt-2">{session.error}</p>
            )}
          </div>
          <div className="flex flex-col gap-4 h-full overflow-auto">
            <RuleProposalView
              proposal={
                session.latest?.proposal ?? session.latest?.lastProposal ?? null
              }
              onEdit={session.editConstraint}
              onSave={onSave}
              saveDisabled={!proposalAvailable}
              loading={session.loading}
            />
            {proposalAvailable && (
              <Card className="p-4 space-y-2">
                <label className="text-sm font-medium">Rule id</label>
                <Input
                  value={ruleId}
                  onChange={(e) => setRuleId(e.target.value)}
                  placeholder="custom-r-001"
                />
                <p className="text-xs text-muted-foreground">
                  Must be unique. This is the id used when this rule fires
                  during validation.
                </p>
              </Card>
            )}
            <TestMatrix results={session.latest?.testResults ?? null} />
            {/*
              Informational banner when the AI couldn't reach a clean test
              matrix but the rule itself is valid Metaschema. Shown alongside
              the (still saveable) proposal so users can review the matrix
              and decide.
            */}
            {proposalAvailable && !matrixClean && session.latest?.message && (
              <Card className="p-4 text-sm border-amber-300 dark:border-amber-700 bg-amber-50 text-amber-900 dark:bg-amber-950/40 dark:text-amber-100">
                <div className="font-medium mb-1">Heads up — test matrix isn't clean</div>
                {session.latest.message}
              </Card>
            )}
            {/*
              Hard failure: constraint XML didn't compile against the
              Metaschema spec after retries. Save is blocked.
            */}
            {session.latest?.phase === 'exhausted' && (
              <Card className="p-4 text-sm border-red-300 dark:border-red-700 bg-red-50 text-red-900 dark:bg-red-950/40 dark:text-red-100">
                <div className="font-medium mb-1">Couldn't generate a valid rule</div>
                {session.latest.message}
              </Card>
            )}
            <div className="flex justify-end">
              <Button
                variant="ghost"
                onClick={async () => {
                  await session.abandon();
                  router.push('/rules/custom');
                }}
              >
                Abandon
              </Button>
            </div>
          </div>
        </div>
      )}
    </AiFeatureGate>
  );
}
