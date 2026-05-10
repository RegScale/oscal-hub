'use client';

import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { Button } from '@/components/ui/button';
import { Plus, Trash2 } from 'lucide-react';
import type { SelectControl } from '@/types/oscal-models';

interface SelectControlEditorProps {
  value: SelectControl[] | undefined;
  onChange: (next: SelectControl[] | undefined) => void;
  label: string;
  description?: string;
}

export function SelectControlEditor({ value, onChange, label, description }: SelectControlEditorProps) {
  const items = value ?? [];

  const update = (index: number, next: SelectControl) => {
    const arr = [...items];
    arr[index] = next;
    onChange(arr);
  };

  return (
    <div className="space-y-2">
      <div className="flex items-center justify-between">
        <div>
          <Label className="text-xs font-semibold">{label}</Label>
          {description && <p className="text-xs text-muted-foreground">{description}</p>}
        </div>
        <Button
          type="button"
          size="sm"
          variant="outline"
          onClick={() => onChange([...items, { 'with-ids': [] }])}
        >
          <Plus className="h-3.5 w-3.5 mr-1" />
          Add selector
        </Button>
      </div>
      {items.length === 0 ? (
        <p className="text-xs italic text-muted-foreground border border-dashed rounded p-3 text-center">
          None.
        </p>
      ) : (
        <div className="space-y-2">
          {items.map((sc, i) => (
            <div key={i} className="rounded-md border p-2 space-y-2 bg-background/40">
              <div className="flex items-center justify-between">
                <span className="text-xs font-medium text-muted-foreground">Selector {i + 1}</span>
                <Button
                  type="button"
                  size="sm"
                  variant="ghost"
                  onClick={() => onChange(items.filter((_, j) => j !== i))}
                  className="h-7 w-7 p-0 text-destructive"
                >
                  <Trash2 className="h-3.5 w-3.5" />
                </Button>
              </div>
              <div className="space-y-1">
                <Label className="text-xs">With child controls</Label>
                <Select
                  value={sc['with-child-controls'] ?? '__unset__'}
                  onValueChange={(v) => {
                    const next = { ...sc };
                    if (v === '__unset__') delete next['with-child-controls'];
                    else next['with-child-controls'] = v as 'yes' | 'no';
                    update(i, next);
                  }}
                >
                  <SelectTrigger className="h-8 text-xs">
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="__unset__">Unset</SelectItem>
                    <SelectItem value="yes">Yes — include enhancements</SelectItem>
                    <SelectItem value="no">No — leave enhancements behind</SelectItem>
                  </SelectContent>
                </Select>
              </div>
              <div className="space-y-1">
                <Label className="text-xs">Control IDs (comma separated)</Label>
                <Input
                  value={(sc['with-ids'] ?? []).join(', ')}
                  onChange={(e) => {
                    const arr = e.target.value.split(',').map((s) => s.trim()).filter(Boolean);
                    update(i, { ...sc, 'with-ids': arr.length > 0 ? arr : undefined });
                  }}
                  placeholder="ac-1, ac-2, ac-3"
                  className="font-mono text-xs"
                />
              </div>
              <div className="space-y-1">
                <Label className="text-xs">Match patterns (one per line, glob style)</Label>
                <textarea
                  value={(sc.matching ?? []).map((m) => m.pattern).join('\n')}
                  onChange={(e) => {
                    const lines = e.target.value.split('\n').map((s) => s.trim()).filter(Boolean);
                    update(i, {
                      ...sc,
                      matching: lines.length > 0 ? lines.map((pattern) => ({ pattern })) : undefined,
                    });
                  }}
                  placeholder="ac-*&#10;au-?"
                  rows={2}
                  className="w-full rounded-md border bg-transparent px-3 py-2 text-xs font-mono shadow-sm focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring"
                />
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
