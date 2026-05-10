'use client';

import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Switch } from '@/components/ui/switch';
import { RepeatableSection } from './RepeatableSection';
import { SelectControlEditor } from './SelectControlEditor';
import type { ProfileImport } from '@/types/oscal-models';

interface ImportEditorProps {
  value: ProfileImport[];
  onChange: (next: ProfileImport[]) => void;
}

export function ImportEditor({ value, onChange }: ImportEditorProps) {
  return (
    <RepeatableSection<ProfileImport>
      label="Imports"
      itemLabel="Import"
      description="Each import references a catalog (or another profile) and selects which controls to include"
      items={value}
      newItem={() => ({ href: '', 'include-all': {} })}
      itemTitle={(imp) => imp.href || 'New import'}
      onChange={onChange}
      renderItem={(imp, _index, update) => {
        const includeAll = imp['include-all'] !== undefined;
        return (
          <div className="space-y-3">
            <div className="space-y-1">
              <Label className="text-xs">href *</Label>
              <Input
                value={imp.href}
                onChange={(e) => update({ ...imp, href: e.target.value })}
                placeholder="#nist-800-53-rev5  or  https://..."
                className="font-mono text-xs"
              />
              <p className="text-xs text-muted-foreground">
                Use <code className="font-mono">#&lt;resource-uuid&gt;</code> to reference a back-matter resource,
                or a full URI for an external catalog.
              </p>
            </div>

            <div className="flex items-center justify-between rounded-md border bg-muted/20 px-3 py-2">
              <div>
                <Label className="text-xs font-semibold">Include all controls</Label>
                <p className="text-xs text-muted-foreground">
                  Bring in every control from the source.
                </p>
              </div>
              <Switch
                checked={includeAll}
                onCheckedChange={(checked) => {
                  const next = { ...imp };
                  if (checked) {
                    next['include-all'] = {};
                    delete next['include-controls'];
                  } else {
                    delete next['include-all'];
                    next['include-controls'] = [];
                  }
                  update(next);
                }}
              />
            </div>

            {!includeAll && (
              <SelectControlEditor
                value={imp['include-controls']}
                onChange={(next) =>
                  update({
                    ...imp,
                    'include-controls': next,
                  })
                }
                label="Include controls"
                description="Pick specific controls or patterns"
              />
            )}

            <SelectControlEditor
              value={imp['exclude-controls']}
              onChange={(next) => update({ ...imp, 'exclude-controls': next })}
              label="Exclude controls"
              description="Optionally remove controls from the included set"
            />
          </div>
        );
      }}
    />
  );
}
