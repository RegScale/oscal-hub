'use client';
import Link from 'next/link';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { AiFeatureGate } from '@/components/ai/AiFeatureGate';
import { useEffect, useState } from 'react';
import { HelpButton } from '@/components/HelpButton';

interface WizardOption {
  kind: string;
  title: string;
  description: string;
  available: boolean;
}

const OPTIONS: WizardOption[] = [
  { kind: 'CATALOG', title: 'Build Catalog from Source', description: 'Drop a PDF, Word doc, HTML, or paste text — AI drafts an OSCAL catalog you can review and save.', available: true },
  { kind: 'COMPONENT_DEF', title: 'Build Component-definition from STIG / CIS / Config Guide', description: 'Drop a STIG, CIS Benchmark, or vendor configuration guide — AI maps the recommended settings to NIST 800-53 controls and drafts an OSCAL component-definition.', available: true },
  { kind: 'SSP', title: 'Draft SSP from Source', description: 'Drop an architecture doc, system description, or existing draft SSP — AI extracts system characteristics and drafts an OSCAL System Security Plan you can review and save.', available: true },
  { kind: 'POAM', title: 'Draft POA&M', description: 'Coming soon.', available: false },
];

export default function WizardPickerPage() {
  const [orgId, setOrgId] = useState<number | null>(null);
  useEffect(() => {
    const stored = typeof window !== 'undefined' ? localStorage.getItem('user') : null;
    if (stored) setOrgId((JSON.parse(stored) as { organizationId?: number }).organizationId ?? null);
  }, []);

  return (
    <AiFeatureGate
      organizationId={orgId}
      fallback={<div className="p-8 text-muted-foreground">AI features are disabled. Ask your org admin to add an Anthropic API key.</div>}
    >
      <div className="container mx-auto py-8">
        <div className="flex items-center gap-2 mb-6">
          <h1 className="text-2xl font-semibold">AI Wizard</h1>
          <HelpButton slug="ai-wizard" />
        </div>
        <div className="grid gap-4 md:grid-cols-2">
          {OPTIONS.map((opt) => (
            <Card key={opt.kind} className={!opt.available ? 'opacity-60' : ''}>
              <CardHeader>
                <CardTitle>{opt.title}</CardTitle>
                <CardDescription>{opt.description}</CardDescription>
              </CardHeader>
              <CardContent>
                {opt.available ? (
                  <Link href={`/ai/wizard/${opt.kind.toLowerCase()}`} className="text-primary hover:underline">
                    Start →
                  </Link>
                ) : (
                  <span className="text-sm text-muted-foreground">Not yet available</span>
                )}
              </CardContent>
            </Card>
          ))}
        </div>
      </div>
    </AiFeatureGate>
  );
}
