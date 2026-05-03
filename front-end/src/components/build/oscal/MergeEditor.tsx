'use client';

import { Label } from '@/components/ui/label';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { Switch } from '@/components/ui/switch';
import type { ProfileMerge } from '@/types/oscal-models';

interface MergeEditorProps {
  value: ProfileMerge | undefined;
  onChange: (next: ProfileMerge | undefined) => void;
}

type MergeMode = 'unset' | 'flat' | 'as-is' | 'custom';

export function MergeEditor({ value, onChange }: MergeEditorProps) {
  const merge = value ?? {};
  const mode: MergeMode = merge.flat
    ? 'flat'
    : merge['as-is']
      ? 'as-is'
      : merge.custom
        ? 'custom'
        : 'unset';

  const setMode = (next: MergeMode) => {
    const base: ProfileMerge = { combine: merge.combine };
    switch (next) {
      case 'flat':
        base.flat = {};
        break;
      case 'as-is':
        base['as-is'] = true;
        break;
      case 'custom':
        base.custom = {};
        break;
      default:
        break;
    }
    if (Object.keys(base).length === 0 || (next === 'unset' && !merge.combine)) {
      onChange(undefined);
    } else {
      onChange(base);
    }
  };

  return (
    <div className="space-y-3">
      <p className="text-sm text-muted-foreground">
        Controls how imported controls are combined and structured in the resolved profile.
      </p>

      <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
        <div className="space-y-1">
          <Label className="text-xs">Merge mode</Label>
          <Select value={mode} onValueChange={(v) => setMode(v as MergeMode)}>
            <SelectTrigger>
              <SelectValue />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="unset">Unset (default)</SelectItem>
              <SelectItem value="flat">Flat — all controls at top level</SelectItem>
              <SelectItem value="as-is">As-is — preserve source structure</SelectItem>
              <SelectItem value="custom">Custom — define your own structure</SelectItem>
            </SelectContent>
          </Select>
        </div>

        <div className="space-y-1">
          <Label className="text-xs">Combine method</Label>
          <Select
            value={merge.combine?.method ?? '__unset__'}
            onValueChange={(v) => {
              const next: ProfileMerge = { ...merge };
              if (v === '__unset__') delete next.combine;
              else next.combine = { method: v as 'use-first' | 'merge' | 'keep' };
              onChange(Object.keys(next).length === 0 ? undefined : next);
            }}
          >
            <SelectTrigger>
              <SelectValue />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="__unset__">Unset</SelectItem>
              <SelectItem value="use-first">use-first</SelectItem>
              <SelectItem value="merge">merge</SelectItem>
              <SelectItem value="keep">keep</SelectItem>
            </SelectContent>
          </Select>
        </div>
      </div>

      {mode === 'as-is' && (
        <div className="flex items-center justify-between rounded-md border bg-muted/20 px-3 py-2">
          <Label className="text-xs">As-is enabled</Label>
          <Switch
            checked={Boolean(merge['as-is'])}
            onCheckedChange={(checked) => {
              const next = { ...merge };
              if (checked) {
                next['as-is'] = true;
                delete next.flat;
                delete next.custom;
              } else {
                delete next['as-is'];
              }
              onChange(Object.keys(next).length === 0 ? undefined : next);
            }}
          />
        </div>
      )}
    </div>
  );
}
