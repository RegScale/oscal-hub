'use client';

import { useEffect, useState } from 'react';
import { Card } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Textarea } from '@/components/ui/textarea';
import type { RuleProposal } from '@/types/rule-gen';

interface Props {
  proposal: RuleProposal | null;
  onEdit: (constraintXml: string) => void;
  onSave: () => void;
  saveDisabled: boolean;
  loading: boolean;
}

export function RuleProposalView({ proposal, onEdit, onSave, saveDisabled, loading }: Props) {
  const [xml, setXml] = useState(proposal?.constraintXml ?? '');
  useEffect(() => {
    setXml(proposal?.constraintXml ?? '');
  }, [proposal?.constraintXml]);

  if (!proposal) {
    return (
      <Card className="p-4 text-sm text-muted-foreground">
        Once a rule is drafted it will appear here.
      </Card>
    );
  }
  const dirty = xml !== proposal.constraintXml;
  return (
    <Card className="p-4 space-y-3">
      <div className="flex items-start justify-between gap-3">
        <div>
          <h3 className="font-semibold">{proposal.name}</h3>
          <p className="text-sm text-muted-foreground">{proposal.description}</p>
        </div>
        <Badge
          variant={proposal.severity === 'error' ? 'destructive' : 'secondary'}
          title="The severity this rule emits when a document violates it"
        >
          severity: {proposal.severity}
        </Badge>
      </div>
      <div>
        <label className="text-xs uppercase tracking-wide text-muted-foreground">
          Metaschema constraint (Metapath)
        </label>
        <Textarea
          value={xml}
          onChange={(e) => setXml(e.target.value)}
          rows={8}
          className="font-mono text-xs"
        />
      </div>
      <div className="flex gap-2 justify-end">
        <Button
          variant="outline"
          disabled={!dirty || loading}
          onClick={() => onEdit(xml)}
        >
          Re-test edited constraint
        </Button>
        <Button disabled={saveDisabled || loading || dirty} onClick={onSave}>
          Save rule
        </Button>
      </div>
    </Card>
  );
}
