'use client';

import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { RepeatableSection } from './RepeatableSection';
import type { ResponsibleParty, Role, Party } from '@/types/oscal-models';

interface ResponsiblePartiesEditorProps {
  value: ResponsibleParty[] | undefined;
  onChange: (next: ResponsibleParty[] | undefined) => void;
  availableRoles: Role[];
  availableParties: Party[];
}

export function ResponsiblePartiesEditor({ value, onChange, availableRoles, availableParties }: ResponsiblePartiesEditorProps) {
  const items = value ?? [];
  return (
    <RepeatableSection<ResponsibleParty>
      label="Responsible parties"
      itemLabel="Responsible party"
      description="Map roles to parties"
      items={items}
      newItem={() => ({ 'role-id': '', 'party-uuids': [] })}
      itemTitle={(rp) => `${rp['role-id'] || '(no role)'} → ${rp['party-uuids'].length} party/parties`}
      onChange={(next) => onChange(next.length === 0 ? undefined : next)}
      renderItem={(rp, _index, update) => (
        <div className="space-y-2">
          <div className="space-y-1">
            <Label className="text-xs">Role *</Label>
            {availableRoles.length === 0 ? (
              <Input
                value={rp['role-id']}
                onChange={(e) => update({ ...rp, 'role-id': e.target.value })}
                placeholder="Define a role first or type a role-id"
              />
            ) : (
              <Select
                value={rp['role-id']}
                onValueChange={(v) => update({ ...rp, 'role-id': v })}
              >
                <SelectTrigger>
                  <SelectValue placeholder="Select role" />
                </SelectTrigger>
                <SelectContent>
                  {availableRoles.map((r) => (
                    <SelectItem key={r.id} value={r.id}>
                      {r.id} — {r.title}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            )}
          </div>
          <div className="space-y-1">
            <Label className="text-xs">Party UUIDs (comma separated)</Label>
            <Input
              value={rp['party-uuids'].join(', ')}
              onChange={(e) => {
                const arr = e.target.value
                  .split(',')
                  .map((s) => s.trim())
                  .filter(Boolean);
                update({ ...rp, 'party-uuids': arr });
              }}
              className="font-mono text-xs"
            />
            {availableParties.length > 0 && (
              <p className="text-xs text-muted-foreground">
                Available: {availableParties.slice(0, 3).map((p) => `${p.name || p.uuid.slice(0, 8)}`).join(', ')}
                {availableParties.length > 3 ? '…' : ''}
              </p>
            )}
          </div>
        </div>
      )}
    />
  );
}
