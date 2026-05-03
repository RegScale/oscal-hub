'use client';

import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { ParamEditor } from './ParamEditor';
import { PartEditor } from './PartEditor';
import { PropsEditor } from './PropsEditor';
import { LinksEditor } from './LinksEditor';
import { Plus, Trash2 } from 'lucide-react';
import type { Control } from '@/types/oscal-models';

interface ControlEditorProps {
  control: Control;
  onChange: (next: Control) => void;
  /** When true, renders inline (no card wrapper). */
  inline?: boolean;
  depth?: number;
}

export function ControlEditor({ control, onChange, inline, depth = 0 }: ControlEditorProps) {
  const body = (
    <div className="space-y-3">
      <div className="grid grid-cols-1 md:grid-cols-2 gap-2">
        <div className="space-y-1">
          <Label className="text-xs">Control ID *</Label>
          <Input
            value={control.id}
            onChange={(e) => onChange({ ...control, id: e.target.value })}
            placeholder="ac-1"
          />
        </div>
        <div className="space-y-1">
          <Label className="text-xs">Class</Label>
          <Input
            value={control.class ?? ''}
            onChange={(e) => onChange({ ...control, class: e.target.value || undefined })}
            placeholder="SP800-53"
          />
        </div>
      </div>
      <div className="space-y-1">
        <Label className="text-xs">Title *</Label>
        <Input
          value={control.title}
          onChange={(e) => onChange({ ...control, title: e.target.value })}
          placeholder="Policy and Procedures"
        />
      </div>

      <ParamEditor value={control.params} onChange={(p) => onChange({ ...control, params: p })} />
      <PropsEditor value={control.props} onChange={(p) => onChange({ ...control, props: p })} />
      <LinksEditor value={control.links} onChange={(l) => onChange({ ...control, links: l })} />
      <PartEditor value={control.parts} onChange={(p) => onChange({ ...control, parts: p })} />

      {depth < 4 && (
        <NestedControlsEditor
          controls={control.controls}
          onChange={(c) => onChange({ ...control, controls: c })}
          depth={depth + 1}
        />
      )}
    </div>
  );

  if (inline) return body;

  return (
    <Card className="border-primary/30">
      <CardHeader className="py-3">
        <CardTitle className="text-base flex items-center gap-2">
          <Badge variant="outline" className="font-mono">{control.id || 'new-control'}</Badge>
          <span className="font-normal">{control.title || 'Untitled control'}</span>
        </CardTitle>
      </CardHeader>
      <CardContent>{body}</CardContent>
    </Card>
  );
}

function NestedControlsEditor({
  controls,
  onChange,
  depth,
}: {
  controls: Control[] | undefined;
  onChange: (next: Control[] | undefined) => void;
  depth: number;
}) {
  const items = controls ?? [];
  return (
    <div className="space-y-2">
      <div className="flex items-center justify-between">
        <Label className="text-xs">Control enhancements (nested controls)</Label>
        <Button
          type="button"
          size="sm"
          variant="outline"
          onClick={() => onChange([...items, { id: '', title: '' }])}
        >
          <Plus className="h-3.5 w-3.5 mr-1" />
          Add enhancement
        </Button>
      </div>
      {items.map((c, i) => (
        <div key={i} className="rounded-md border-l-2 border-primary/40 pl-3 py-2 space-y-2 bg-background/30">
          <div className="flex items-center justify-between">
            <span className="text-xs font-medium text-muted-foreground">
              Enhancement {i + 1}: <span className="font-mono">{c.id || '(no id)'}</span>
            </span>
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
          <ControlEditor
            control={c}
            onChange={(next) => {
              const arr = [...items];
              arr[i] = next;
              onChange(arr);
            }}
            inline
            depth={depth}
          />
        </div>
      ))}
    </div>
  );
}
