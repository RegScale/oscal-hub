'use client';

import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Button } from '@/components/ui/button';
import { Plus, Trash2 } from 'lucide-react';
import { RepeatableSection } from './RepeatableSection';
import { MarkdownField } from './MarkdownField';
import { generateUuid } from '@/types/oscal-models';
import type { Resource, Rlink } from '@/types/oscal-models';

interface ResourcesEditorProps {
  value: Resource[] | undefined;
  onChange: (next: Resource[] | undefined) => void;
}

export function ResourcesEditor({ value, onChange }: ResourcesEditorProps) {
  const items = value ?? [];
  return (
    <RepeatableSection<Resource>
      label="Back-matter resources"
      itemLabel="Resource"
      description="External documents, references, citations"
      items={items}
      newItem={() => ({ uuid: generateUuid() })}
      itemTitle={(r) => r.title || `Resource ${r.uuid.slice(0, 8)}…`}
      onChange={(next) => onChange(next.length === 0 ? undefined : next)}
      renderItem={(res, _index, update) => (
        <div className="space-y-2">
          <div className="grid grid-cols-1 md:grid-cols-2 gap-2">
            <div className="space-y-1">
              <Label className="text-xs">UUID *</Label>
              <Input
                value={res.uuid}
                onChange={(e) => update({ ...res, uuid: e.target.value })}
                className="font-mono text-xs"
              />
            </div>
            <div className="space-y-1">
              <Label className="text-xs">Title</Label>
              <Input
                value={res.title ?? ''}
                onChange={(e) => update({ ...res, title: e.target.value || undefined })}
              />
            </div>
          </div>
          <MarkdownField
            label="Description"
            value={res.description ?? ''}
            onChange={(v) => update({ ...res, description: v || undefined })}
            rows={2}
          />
          <MarkdownField
            label="Citation text"
            value={res.citation?.text ?? ''}
            onChange={(v) =>
              update({
                ...res,
                citation: v ? { ...(res.citation ?? { text: '' }), text: v } : undefined,
              })
            }
            rows={2}
            singleLine
          />
          <RlinksEditor
            value={res.rlinks}
            onChange={(rl) => update({ ...res, rlinks: rl })}
          />
        </div>
      )}
    />
  );
}

function RlinksEditor({ value, onChange }: { value: Rlink[] | undefined; onChange: (next: Rlink[] | undefined) => void }) {
  const items = value ?? [];
  return (
    <div className="space-y-2">
      <div className="flex items-center justify-between">
        <Label className="text-xs">Rlinks (resource locations)</Label>
        <Button
          type="button"
          size="sm"
          variant="outline"
          onClick={() => onChange([...items, { href: '' }])}
        >
          <Plus className="h-3.5 w-3.5 mr-1" />
          Add rlink
        </Button>
      </div>
      {items.map((rl, i) => (
        <div key={i} className="rounded-md border p-2 space-y-2 bg-background/40">
          <div className="flex items-center justify-between">
            <span className="text-xs font-medium text-muted-foreground">Rlink {i + 1}</span>
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
          <Input
            value={rl.href}
            onChange={(e) => {
              const next = [...items];
              next[i] = { ...rl, href: e.target.value };
              onChange(next);
            }}
            placeholder="https://..."
          />
          <Input
            value={rl['media-type'] ?? ''}
            onChange={(e) => {
              const next = [...items];
              next[i] = { ...rl, 'media-type': e.target.value || undefined };
              onChange(next);
            }}
            placeholder="media-type (e.g. application/pdf)"
          />
        </div>
      ))}
    </div>
  );
}
