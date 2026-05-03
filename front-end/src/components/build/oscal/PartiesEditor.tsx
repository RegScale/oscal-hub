'use client';

import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { RepeatableSection } from './RepeatableSection';
import { generateUuid } from '@/types/oscal-models';
import type { Party } from '@/types/oscal-models';

interface PartiesEditorProps {
  value: Party[] | undefined;
  onChange: (next: Party[] | undefined) => void;
  label?: string;
}

export function PartiesEditor({ value, onChange, label = 'Parties' }: PartiesEditorProps) {
  const items = value ?? [];
  return (
    <RepeatableSection<Party>
      label={label}
      itemLabel="Party"
      description="People and organizations referenced in the document"
      items={items}
      newItem={() => ({ uuid: generateUuid(), type: 'organization', name: '' })}
      itemTitle={(p) => `${p.type}: ${p.name || '(unnamed)'}`}
      onChange={(next) => onChange(next.length === 0 ? undefined : next)}
      renderItem={(party, _index, update) => (
        <div className="space-y-2">
          <div className="grid grid-cols-1 md:grid-cols-2 gap-2">
            <div className="space-y-1">
              <Label className="text-xs">UUID *</Label>
              <Input
                value={party.uuid}
                onChange={(e) => update({ ...party, uuid: e.target.value })}
                className="font-mono text-xs"
              />
            </div>
            <div className="space-y-1">
              <Label className="text-xs">Type *</Label>
              <Select
                value={party.type}
                onValueChange={(v) => update({ ...party, type: v as 'person' | 'organization' })}
              >
                <SelectTrigger>
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="organization">Organization</SelectItem>
                  <SelectItem value="person">Person</SelectItem>
                </SelectContent>
              </Select>
            </div>
          </div>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-2">
            <div className="space-y-1">
              <Label className="text-xs">Name</Label>
              <Input
                value={party.name ?? ''}
                onChange={(e) => update({ ...party, name: e.target.value || undefined })}
                placeholder={party.type === 'person' ? 'Jane Doe' : 'Acme Corp'}
              />
            </div>
            <div className="space-y-1">
              <Label className="text-xs">Short name</Label>
              <Input
                value={party['short-name'] ?? ''}
                onChange={(e) => update({ ...party, 'short-name': e.target.value || undefined })}
                placeholder="ACME"
              />
            </div>
          </div>
          <div className="space-y-1">
            <Label className="text-xs">Email addresses (comma separated)</Label>
            <Input
              value={(party['email-addresses'] ?? []).join(', ')}
              onChange={(e) => {
                const arr = e.target.value
                  .split(',')
                  .map((s) => s.trim())
                  .filter(Boolean);
                update({ ...party, 'email-addresses': arr.length > 0 ? arr : undefined });
              }}
              placeholder="contact@example.com"
            />
          </div>
        </div>
      )}
    />
  );
}
