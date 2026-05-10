'use client';

import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Textarea } from '@/components/ui/textarea';
import { RepeatableSection } from './RepeatableSection';
import type { SetParameter } from '@/types/oscal-models';

interface SetParameterEditorProps {
  value: SetParameter[] | undefined;
  onChange: (next: SetParameter[] | undefined) => void;
}

export function SetParameterEditor({ value, onChange }: SetParameterEditorProps) {
  const items = value ?? [];
  return (
    <RepeatableSection<SetParameter>
      label="Set parameters"
      itemLabel="Set parameter"
      description="Override parameters defined in the imported catalog"
      items={items}
      newItem={() => ({ 'param-id': '' })}
      itemTitle={(sp) =>
        sp['param-id']
          ? `${sp['param-id']} → ${(sp.values?.length ?? 0) > 0 ? sp.values?.join(', ') : (sp.label || '(no value)')}`
          : 'New override'
      }
      onChange={(next) => onChange(next.length === 0 ? undefined : next)}
      renderItem={(sp, _index, update) => (
        <div className="space-y-2">
          <div className="grid grid-cols-1 md:grid-cols-2 gap-2">
            <div className="space-y-1">
              <Label className="text-xs">Parameter ID *</Label>
              <Input
                value={sp['param-id']}
                onChange={(e) => update({ ...sp, 'param-id': e.target.value })}
                placeholder="ac-1_prm_1"
              />
            </div>
            <div className="space-y-1">
              <Label className="text-xs">Class</Label>
              <Input
                value={sp.class ?? ''}
                onChange={(e) => update({ ...sp, class: e.target.value || undefined })}
              />
            </div>
          </div>
          <div className="space-y-1">
            <Label className="text-xs">Label</Label>
            <Input
              value={sp.label ?? ''}
              onChange={(e) => update({ ...sp, label: e.target.value || undefined })}
            />
          </div>
          <div className="space-y-1">
            <Label className="text-xs">Values (comma separated)</Label>
            <Input
              value={(sp.values ?? []).join(', ')}
              onChange={(e) => {
                const arr = e.target.value.split(',').map((s) => s.trim()).filter(Boolean);
                update({ ...sp, values: arr.length > 0 ? arr : undefined });
              }}
              placeholder="quarterly, semi-annually"
            />
          </div>
          <div className="space-y-1">
            <Label className="text-xs">Usage</Label>
            <Textarea
              value={sp.usage ?? ''}
              onChange={(e) => update({ ...sp, usage: e.target.value || undefined })}
              rows={2}
            />
          </div>
        </div>
      )}
    />
  );
}
