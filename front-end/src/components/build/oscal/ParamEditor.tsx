'use client';

import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Textarea } from '@/components/ui/textarea';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { RepeatableSection } from './RepeatableSection';
import { PropsEditor } from './PropsEditor';
import { LinksEditor } from './LinksEditor';
import { MarkdownField } from './MarkdownField';
import { Plus, Trash2 } from 'lucide-react';
import type { Param, ParamConstraint, ParamGuideline } from '@/types/oscal-models';

interface ParamEditorProps {
  value: Param[] | undefined;
  onChange: (next: Param[] | undefined) => void;
  label?: string;
}

export function ParamEditor({ value, onChange, label = 'Parameters' }: ParamEditorProps) {
  const items = value ?? [];
  return (
    <RepeatableSection<Param>
      label={label}
      itemLabel="Parameter"
      description="Variables substituted into control text"
      items={items}
      newItem={() => ({ id: '' })}
      itemTitle={(p) => p.id ? `${p.id}${p.label ? ` — ${p.label}` : ''}` : 'New parameter'}
      onChange={(next) => onChange(next.length === 0 ? undefined : next)}
      renderItem={(param, _index, update) => (
        <SingleParamEditor param={param} onChange={update} />
      )}
    />
  );
}

interface SingleParamProps {
  param: Param;
  onChange: (next: Param) => void;
}

function SingleParamEditor({ param, onChange }: SingleParamProps) {
  const valueMode: 'values' | 'select' | 'none' = param.select
    ? 'select'
    : param.values && param.values.length > 0
      ? 'values'
      : 'none';

  const setMode = (mode: 'values' | 'select' | 'none') => {
    const next = { ...param };
    delete next.select;
    delete next.values;
    if (mode === 'select') {
      next.select = { 'how-many': 'one', choice: [] };
    } else if (mode === 'values') {
      next.values = [];
    }
    onChange(next);
  };

  return (
    <div className="space-y-3">
      <div className="grid grid-cols-1 md:grid-cols-3 gap-2">
        <div className="space-y-1">
          <Label className="text-xs">Param ID *</Label>
          <Input
            value={param.id}
            onChange={(e) => onChange({ ...param, id: e.target.value })}
            placeholder="ac-1_prm_1"
          />
        </div>
        <div className="space-y-1">
          <Label className="text-xs">Class</Label>
          <Input
            value={param.class ?? ''}
            onChange={(e) => onChange({ ...param, class: e.target.value || undefined })}
          />
        </div>
        <div className="space-y-1">
          <Label className="text-xs">Label</Label>
          <Input
            value={param.label ?? ''}
            onChange={(e) => onChange({ ...param, label: e.target.value || undefined })}
            placeholder="Reviewer"
          />
        </div>
      </div>

      <MarkdownField
        label="Usage"
        value={param.usage ?? ''}
        onChange={(v) => onChange({ ...param, usage: v || undefined })}
        rows={2}
      />

      <div className="space-y-2">
        <div className="flex items-center justify-between">
          <Label className="text-xs">Allowed values</Label>
          <Select value={valueMode} onValueChange={(v) => setMode(v as 'values' | 'select' | 'none')}>
            <SelectTrigger className="h-7 w-44 text-xs">
              <SelectValue />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="none">None</SelectItem>
              <SelectItem value="values">Free-form values</SelectItem>
              <SelectItem value="select">Selection (choices)</SelectItem>
            </SelectContent>
          </Select>
        </div>
        {valueMode === 'values' && param.values && (
          <ValuesList
            values={param.values}
            onChange={(next) => onChange({ ...param, values: next })}
          />
        )}
        {valueMode === 'select' && param.select && (
          <SelectionEditor
            selection={param.select}
            onChange={(next) => onChange({ ...param, select: next })}
          />
        )}
      </div>

      <ConstraintsEditor
        constraints={param.constraints}
        onChange={(next) => onChange({ ...param, constraints: next })}
      />

      <GuidelinesEditor
        guidelines={param.guidelines}
        onChange={(next) => onChange({ ...param, guidelines: next })}
      />

      <PropsEditor value={param.props} onChange={(p) => onChange({ ...param, props: p })} />
      <LinksEditor value={param.links} onChange={(l) => onChange({ ...param, links: l })} />

      <MarkdownField
        label="Remarks"
        value={param.remarks ?? ''}
        onChange={(v) => onChange({ ...param, remarks: v || undefined })}
        rows={2}
      />
    </div>
  );
}

function ValuesList({ values, onChange }: { values: string[]; onChange: (next: string[]) => void }) {
  return (
    <div className="space-y-2">
      {values.map((v, i) => (
        <div key={i} className="flex items-center gap-2">
          <Input
            value={v}
            onChange={(e) => {
              const next = [...values];
              next[i] = e.target.value;
              onChange(next);
            }}
            placeholder="Value"
          />
          <Button
            type="button"
            size="sm"
            variant="ghost"
            onClick={() => onChange(values.filter((_, j) => j !== i))}
            className="h-8 w-8 p-0 text-destructive"
          >
            <Trash2 className="h-3.5 w-3.5" />
          </Button>
        </div>
      ))}
      <Button type="button" size="sm" variant="outline" onClick={() => onChange([...values, ''])}>
        <Plus className="h-3.5 w-3.5 mr-1" />
        Add value
      </Button>
    </div>
  );
}

function SelectionEditor({
  selection,
  onChange,
}: {
  selection: NonNullable<Param['select']>;
  onChange: (next: NonNullable<Param['select']>) => void;
}) {
  const choices = selection.choice ?? [];
  return (
    <div className="space-y-2 rounded-md border bg-background/40 p-3">
      <div className="space-y-1">
        <Label className="text-xs">How many</Label>
        <Select
          value={selection['how-many'] ?? 'one'}
          onValueChange={(v) => onChange({ ...selection, 'how-many': v as 'one' | 'one-or-more' })}
        >
          <SelectTrigger className="h-8 text-xs">
            <SelectValue />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="one">One</SelectItem>
            <SelectItem value="one-or-more">One or more</SelectItem>
          </SelectContent>
        </Select>
      </div>
      <div className="space-y-2">
        <Label className="text-xs">Choices</Label>
        {choices.map((c, i) => (
          <div key={i} className="flex items-center gap-2">
            <Input
              value={c}
              onChange={(e) => {
                const next = [...choices];
                next[i] = e.target.value;
                onChange({ ...selection, choice: next });
              }}
            />
            <Button
              type="button"
              size="sm"
              variant="ghost"
              onClick={() => onChange({ ...selection, choice: choices.filter((_, j) => j !== i) })}
              className="h-8 w-8 p-0 text-destructive"
            >
              <Trash2 className="h-3.5 w-3.5" />
            </Button>
          </div>
        ))}
        <Button
          type="button"
          size="sm"
          variant="outline"
          onClick={() => onChange({ ...selection, choice: [...choices, ''] })}
        >
          <Plus className="h-3.5 w-3.5 mr-1" />
          Add choice
        </Button>
      </div>
    </div>
  );
}

function ConstraintsEditor({
  constraints,
  onChange,
}: {
  constraints: ParamConstraint[] | undefined;
  onChange: (next: ParamConstraint[] | undefined) => void;
}) {
  const items = constraints ?? [];
  return (
    <div className="space-y-2">
      <div className="flex items-center justify-between">
        <Label className="text-xs">Constraints {items.length > 0 && <Badge variant="outline" className="ml-1">{items.length}</Badge>}</Label>
        <Button
          type="button"
          size="sm"
          variant="outline"
          onClick={() => onChange([...items, {}])}
        >
          <Plus className="h-3.5 w-3.5 mr-1" />
          Add constraint
        </Button>
      </div>
      {items.map((c, i) => (
        <div key={i} className="rounded-md border p-2 space-y-2 bg-background/40">
          <div className="flex items-center justify-between">
            <span className="text-xs font-medium text-muted-foreground">Constraint {i + 1}</span>
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
          <Textarea
            value={c.description ?? ''}
            onChange={(e) => {
              const next = [...items];
              next[i] = { ...c, description: e.target.value || undefined };
              onChange(next);
            }}
            rows={2}
            placeholder="Description"
          />
        </div>
      ))}
    </div>
  );
}

function GuidelinesEditor({
  guidelines,
  onChange,
}: {
  guidelines: ParamGuideline[] | undefined;
  onChange: (next: ParamGuideline[] | undefined) => void;
}) {
  const items = guidelines ?? [];
  return (
    <div className="space-y-2">
      <div className="flex items-center justify-between">
        <Label className="text-xs">Guidelines {items.length > 0 && <Badge variant="outline" className="ml-1">{items.length}</Badge>}</Label>
        <Button
          type="button"
          size="sm"
          variant="outline"
          onClick={() => onChange([...items, { prose: '' }])}
        >
          <Plus className="h-3.5 w-3.5 mr-1" />
          Add guideline
        </Button>
      </div>
      {items.map((g, i) => (
        <div key={i} className="rounded-md border p-2 space-y-2 bg-background/40">
          <div className="flex items-center justify-between">
            <span className="text-xs font-medium text-muted-foreground">Guideline {i + 1}</span>
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
          <MarkdownField
            value={g.prose}
            onChange={(v) => {
              const next = [...items];
              next[i] = { prose: v };
              onChange(next);
            }}
            rows={2}
            placeholder="Prose guidance for value selection"
          />
        </div>
      ))}
    </div>
  );
}
