'use client';

import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { Button } from '@/components/ui/button';
import { Plus, Trash2 } from 'lucide-react';
import { RepeatableSection } from './RepeatableSection';
import { PartEditor } from './PartEditor';
import { PropsEditor } from './PropsEditor';
import { LinksEditor } from './LinksEditor';
import { ParamEditor } from './ParamEditor';
import type { Alter, AlterAdd, AlterRemove } from '@/types/oscal-models';

interface AlterEditorProps {
  value: Alter[] | undefined;
  onChange: (next: Alter[] | undefined) => void;
}

export function AlterEditor({ value, onChange }: AlterEditorProps) {
  const items = value ?? [];
  return (
    <RepeatableSection<Alter>
      label="Alters"
      itemLabel="Alter"
      description="Modify a control from the imported catalog (add or remove parts/props/params)"
      items={items}
      newItem={() => ({ 'control-id': '' })}
      itemTitle={(a) =>
        a['control-id']
          ? `${a['control-id']} (${(a.adds?.length ?? 0)} adds, ${(a.removes?.length ?? 0)} removes)`
          : 'New alter'
      }
      onChange={(next) => onChange(next.length === 0 ? undefined : next)}
      renderItem={(alter, _index, update) => (
        <div className="space-y-3">
          <div className="space-y-1">
            <Label className="text-xs">Control ID *</Label>
            <Input
              value={alter['control-id']}
              onChange={(e) => update({ ...alter, 'control-id': e.target.value })}
              placeholder="ac-1"
              className="font-mono"
            />
          </div>

          <RemovesEditor
            value={alter.removes}
            onChange={(r) => update({ ...alter, removes: r })}
          />

          <AddsEditor value={alter.adds} onChange={(a) => update({ ...alter, adds: a })} />
        </div>
      )}
    />
  );
}

function RemovesEditor({
  value,
  onChange,
}: {
  value: AlterRemove[] | undefined;
  onChange: (next: AlterRemove[] | undefined) => void;
}) {
  const items = value ?? [];
  return (
    <div className="space-y-2 rounded-md border bg-background/40 p-2">
      <div className="flex items-center justify-between">
        <Label className="text-xs font-semibold">Removes</Label>
        <Button
          type="button"
          size="sm"
          variant="outline"
          onClick={() => onChange([...items, {}])}
        >
          <Plus className="h-3.5 w-3.5 mr-1" />
          Add remove
        </Button>
      </div>
      {items.map((r, i) => (
        <div key={i} className="rounded-md border p-2 space-y-1">
          <div className="flex items-center justify-between">
            <span className="text-xs text-muted-foreground">Remove {i + 1}</span>
            <Button
              type="button"
              size="sm"
              variant="ghost"
              onClick={() => onChange(items.length > 1 ? items.filter((_, j) => j !== i) : undefined)}
              className="h-7 w-7 p-0 text-destructive"
            >
              <Trash2 className="h-3.5 w-3.5" />
            </Button>
          </div>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-2">
            <Input
              value={r['by-name'] ?? ''}
              onChange={(e) => {
                const arr = [...items];
                arr[i] = { ...r, 'by-name': e.target.value || undefined };
                onChange(arr);
              }}
              placeholder="by-name"
            />
            <Input
              value={r['by-class'] ?? ''}
              onChange={(e) => {
                const arr = [...items];
                arr[i] = { ...r, 'by-class': e.target.value || undefined };
                onChange(arr);
              }}
              placeholder="by-class"
            />
            <Input
              value={r['by-id'] ?? ''}
              onChange={(e) => {
                const arr = [...items];
                arr[i] = { ...r, 'by-id': e.target.value || undefined };
                onChange(arr);
              }}
              placeholder="by-id"
            />
            <Input
              value={r['by-item-name'] ?? ''}
              onChange={(e) => {
                const arr = [...items];
                arr[i] = { ...r, 'by-item-name': e.target.value || undefined };
                onChange(arr);
              }}
              placeholder="by-item-name"
            />
            <Input
              value={r['by-ns'] ?? ''}
              onChange={(e) => {
                const arr = [...items];
                arr[i] = { ...r, 'by-ns': e.target.value || undefined };
                onChange(arr);
              }}
              placeholder="by-ns"
            />
          </div>
        </div>
      ))}
    </div>
  );
}

function AddsEditor({
  value,
  onChange,
}: {
  value: AlterAdd[] | undefined;
  onChange: (next: AlterAdd[] | undefined) => void;
}) {
  const items = value ?? [];
  return (
    <div className="space-y-2 rounded-md border bg-background/40 p-2">
      <div className="flex items-center justify-between">
        <Label className="text-xs font-semibold">Adds</Label>
        <Button
          type="button"
          size="sm"
          variant="outline"
          onClick={() => onChange([...items, {}])}
        >
          <Plus className="h-3.5 w-3.5 mr-1" />
          Add add
        </Button>
      </div>
      {items.map((a, i) => (
        <div key={i} className="rounded-md border p-2 space-y-2">
          <div className="flex items-center justify-between">
            <span className="text-xs text-muted-foreground">Add {i + 1}</span>
            <Button
              type="button"
              size="sm"
              variant="ghost"
              onClick={() => onChange(items.length > 1 ? items.filter((_, j) => j !== i) : undefined)}
              className="h-7 w-7 p-0 text-destructive"
            >
              <Trash2 className="h-3.5 w-3.5" />
            </Button>
          </div>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-2">
            <div className="space-y-1">
              <Label className="text-xs">Position</Label>
              <Select
                value={a.position ?? '__unset__'}
                onValueChange={(v) => {
                  const arr = [...items];
                  const next = { ...a };
                  if (v === '__unset__') delete next.position;
                  else next.position = v as AlterAdd['position'];
                  arr[i] = next;
                  onChange(arr);
                }}
              >
                <SelectTrigger className="h-8 text-xs">
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="__unset__">Unset</SelectItem>
                  <SelectItem value="before">before</SelectItem>
                  <SelectItem value="after">after</SelectItem>
                  <SelectItem value="starting">starting</SelectItem>
                  <SelectItem value="ending">ending</SelectItem>
                </SelectContent>
              </Select>
            </div>
            <div className="space-y-1">
              <Label className="text-xs">By ID</Label>
              <Input
                value={a['by-id'] ?? ''}
                onChange={(e) => {
                  const arr = [...items];
                  arr[i] = { ...a, 'by-id': e.target.value || undefined };
                  onChange(arr);
                }}
                placeholder="ac-1_smt"
                className="font-mono text-xs"
              />
            </div>
          </div>
          <div className="space-y-1">
            <Label className="text-xs">Title</Label>
            <Input
              value={a.title ?? ''}
              onChange={(e) => {
                const arr = [...items];
                arr[i] = { ...a, title: e.target.value || undefined };
                onChange(arr);
              }}
            />
          </div>
          <ParamEditor
            value={a.params}
            onChange={(p) => {
              const arr = [...items];
              arr[i] = { ...a, params: p };
              onChange(arr);
            }}
          />
          <PropsEditor
            value={a.props}
            onChange={(p) => {
              const arr = [...items];
              arr[i] = { ...a, props: p };
              onChange(arr);
            }}
          />
          <LinksEditor
            value={a.links}
            onChange={(l) => {
              const arr = [...items];
              arr[i] = { ...a, links: l };
              onChange(arr);
            }}
          />
          <PartEditor
            value={a.parts}
            onChange={(p) => {
              const arr = [...items];
              arr[i] = { ...a, parts: p };
              onChange(arr);
            }}
          />
        </div>
      ))}
    </div>
  );
}
