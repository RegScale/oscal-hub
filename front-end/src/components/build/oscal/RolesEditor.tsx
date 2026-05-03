'use client';

import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Textarea } from '@/components/ui/textarea';
import { RepeatableSection } from './RepeatableSection';
import type { Role } from '@/types/oscal-models';

interface RolesEditorProps {
  value: Role[] | undefined;
  onChange: (next: Role[] | undefined) => void;
  label?: string;
}

export function RolesEditor({ value, onChange, label = 'Roles' }: RolesEditorProps) {
  const items = value ?? [];
  return (
    <RepeatableSection<Role>
      label={label}
      itemLabel="Role"
      description="Functions performed by parties (creator, content-approver, etc.)"
      items={items}
      newItem={() => ({ id: '', title: '' })}
      itemTitle={(r) => (r.id ? `${r.id} — ${r.title || '(no title)'}` : 'New role')}
      onChange={(next) => onChange(next.length === 0 ? undefined : next)}
      renderItem={(role, _index, update) => (
        <div className="space-y-2">
          <div className="grid grid-cols-1 md:grid-cols-2 gap-2">
            <div className="space-y-1">
              <Label className="text-xs">Role ID *</Label>
              <Input
                value={role.id}
                onChange={(e) => update({ ...role, id: e.target.value })}
                placeholder="creator"
              />
            </div>
            <div className="space-y-1">
              <Label className="text-xs">Title *</Label>
              <Input
                value={role.title}
                onChange={(e) => update({ ...role, title: e.target.value })}
                placeholder="Document Creator"
              />
            </div>
          </div>
          <div className="space-y-1">
            <Label className="text-xs">Short name</Label>
            <Input
              value={role['short-name'] ?? ''}
              onChange={(e) => update({ ...role, 'short-name': e.target.value || undefined })}
            />
          </div>
          <div className="space-y-1">
            <Label className="text-xs">Description</Label>
            <Textarea
              value={role.description ?? ''}
              onChange={(e) => update({ ...role, description: e.target.value || undefined })}
              rows={2}
            />
          </div>
        </div>
      )}
    />
  );
}
