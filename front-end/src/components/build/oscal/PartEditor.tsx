'use client';

import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { RepeatableSection } from './RepeatableSection';
import { PropsEditor } from './PropsEditor';
import { LinksEditor } from './LinksEditor';
import { MarkdownField } from './MarkdownField';
import type { Part } from '@/types/oscal-models';

const COMMON_PART_NAMES = [
  'statement',
  'guidance',
  'assessment-objective',
  'assessment-method',
  'objective',
  'item',
  'example',
];

interface PartEditorProps {
  value: Part[] | undefined;
  onChange: (next: Part[] | undefined) => void;
  label?: string;
  /** Recursion depth for indenting nested parts. */
  depth?: number;
}

export function PartEditor({ value, onChange, label = 'Parts', depth = 0 }: PartEditorProps) {
  const items = value ?? [];

  return (
    <RepeatableSection<Part>
      label={label}
      itemLabel="Part"
      description={depth === 0 ? 'Structured prose blocks (statement, guidance, etc.)' : undefined}
      items={items}
      newItem={() => ({ name: 'statement' })}
      itemTitle={(p) => p.name + (p.id ? ` (${p.id})` : '')}
      onChange={(next) => onChange(next.length === 0 ? undefined : next)}
      renderItem={(part, _index, update) => (
        <SinglePartEditor part={part} onChange={update} depth={depth} />
      )}
    />
  );
}

function SinglePartEditor({ part, onChange, depth }: { part: Part; onChange: (n: Part) => void; depth: number }) {
  return (
    <div className="space-y-2">
      <div className="grid grid-cols-1 md:grid-cols-3 gap-2">
        <div className="space-y-1">
          <Label className="text-xs">Name *</Label>
          <Select
            value={COMMON_PART_NAMES.includes(part.name) ? part.name : '__custom__'}
            onValueChange={(v) => {
              if (v === '__custom__') {
                onChange({ ...part, name: part.name });
              } else {
                onChange({ ...part, name: v });
              }
            }}
          >
            <SelectTrigger>
              <SelectValue />
            </SelectTrigger>
            <SelectContent>
              {COMMON_PART_NAMES.map((n) => (
                <SelectItem key={n} value={n}>
                  {n}
                </SelectItem>
              ))}
              <SelectItem value="__custom__">Custom…</SelectItem>
            </SelectContent>
          </Select>
          {!COMMON_PART_NAMES.includes(part.name) && (
            <Input
              className="mt-1"
              value={part.name}
              onChange={(e) => onChange({ ...part, name: e.target.value })}
              placeholder="Custom part name"
            />
          )}
        </div>
        <div className="space-y-1">
          <Label className="text-xs">Part ID</Label>
          <Input
            value={part.id ?? ''}
            onChange={(e) => onChange({ ...part, id: e.target.value || undefined })}
            placeholder="ac-1_smt.a"
          />
        </div>
        <div className="space-y-1">
          <Label className="text-xs">Class</Label>
          <Input
            value={part.class ?? ''}
            onChange={(e) => onChange({ ...part, class: e.target.value || undefined })}
          />
        </div>
      </div>

      <div className="space-y-1">
        <Label className="text-xs">Title</Label>
        <Input
          value={part.title ?? ''}
          onChange={(e) => onChange({ ...part, title: e.target.value || undefined })}
        />
      </div>

      <MarkdownField
        label="Prose (Markdown)"
        value={part.prose ?? ''}
        onChange={(v) => onChange({ ...part, prose: v || undefined })}
        rows={4}
        placeholder="The organization develops, documents, and disseminates..."
      />
      <p className="text-xs text-muted-foreground -mt-1">
        Use <strong>**bold**</strong>, <em>*italic*</em>, lists, links, tables (GFM).
      </p>

      <div className="space-y-1">
        <Label className="text-xs">Namespace</Label>
        <Input
          value={part.ns ?? ''}
          onChange={(e) => onChange({ ...part, ns: e.target.value || undefined })}
          placeholder="http://csrc.nist.gov/ns/oscal"
        />
      </div>

      <PropsEditor value={part.props} onChange={(p) => onChange({ ...part, props: p })} />
      <LinksEditor value={part.links} onChange={(l) => onChange({ ...part, links: l })} />

      {depth < 3 && (
        <div className="pl-3 border-l-2 border-muted">
          <PartEditor
            label="Nested parts"
            value={part.parts}
            onChange={(p) => onChange({ ...part, parts: p })}
            depth={depth + 1}
          />
        </div>
      )}
    </div>
  );
}
