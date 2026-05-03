'use client';

import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Textarea } from '@/components/ui/textarea';
import { RepeatableSection } from './RepeatableSection';
import type { Prop } from '@/types/oscal-models';

interface PropsEditorProps {
  value: Prop[] | undefined;
  onChange: (next: Prop[] | undefined) => void;
  label?: string;
}

export function PropsEditor({ value, onChange, label = 'Properties' }: PropsEditorProps) {
  const items = value ?? [];

  return (
    <RepeatableSection<Prop>
      label={label}
      itemLabel="Property"
      description="Custom name/value pairs (OSCAL prop)"
      items={items}
      newItem={() => ({ name: '', value: '' })}
      itemTitle={(p) => p.name ? `${p.name}: ${p.value || '(empty)'}` : 'New property'}
      onChange={(next) => onChange(next.length === 0 ? undefined : next)}
      renderItem={(prop, _index, update) => (
        <div className="space-y-2">
          <div className="grid grid-cols-1 md:grid-cols-2 gap-2">
            <div className="space-y-1">
              <Label className="text-xs">Name *</Label>
              <Input
                value={prop.name}
                onChange={(e) => update({ ...prop, name: e.target.value })}
                placeholder="e.g. label"
              />
            </div>
            <div className="space-y-1">
              <Label className="text-xs">Value *</Label>
              <Input
                value={prop.value}
                onChange={(e) => update({ ...prop, value: e.target.value })}
                placeholder="e.g. AC-1"
              />
            </div>
          </div>
          <div className="grid grid-cols-1 md:grid-cols-3 gap-2">
            <div className="space-y-1">
              <Label className="text-xs">Namespace (ns)</Label>
              <Input
                value={prop.ns ?? ''}
                onChange={(e) => update({ ...prop, ns: e.target.value || undefined })}
                placeholder="http://csrc.nist.gov/ns/oscal"
              />
            </div>
            <div className="space-y-1">
              <Label className="text-xs">Class</Label>
              <Input
                value={prop.class ?? ''}
                onChange={(e) => update({ ...prop, class: e.target.value || undefined })}
                placeholder="optional"
              />
            </div>
            <div className="space-y-1">
              <Label className="text-xs">Group</Label>
              <Input
                value={prop.group ?? ''}
                onChange={(e) => update({ ...prop, group: e.target.value || undefined })}
                placeholder="optional"
              />
            </div>
          </div>
          <div className="space-y-1">
            <Label className="text-xs">Remarks</Label>
            <Textarea
              value={prop.remarks ?? ''}
              onChange={(e) => update({ ...prop, remarks: e.target.value || undefined })}
              rows={2}
              placeholder="Optional remarks"
            />
          </div>
        </div>
      )}
    />
  );
}
