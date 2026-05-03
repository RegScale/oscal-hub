'use client';

import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { RepeatableSection } from './RepeatableSection';
import type { Link } from '@/types/oscal-models';

interface LinksEditorProps {
  value: Link[] | undefined;
  onChange: (next: Link[] | undefined) => void;
  label?: string;
}

export function LinksEditor({ value, onChange, label = 'Links' }: LinksEditorProps) {
  const items = value ?? [];
  return (
    <RepeatableSection<Link>
      label={label}
      itemLabel="Link"
      description="External references or pointers to back-matter resources"
      items={items}
      newItem={() => ({ href: '' })}
      itemTitle={(l) => l.text || l.href || 'New link'}
      onChange={(next) => onChange(next.length === 0 ? undefined : next)}
      renderItem={(link, _index, update) => (
        <div className="space-y-2">
          <div className="space-y-1">
            <Label className="text-xs">href *</Label>
            <Input
              value={link.href}
              onChange={(e) => update({ ...link, href: e.target.value })}
              placeholder="https://example.com or #resource-uuid"
            />
          </div>
          <div className="grid grid-cols-1 md:grid-cols-3 gap-2">
            <div className="space-y-1">
              <Label className="text-xs">Rel</Label>
              <Input
                value={link.rel ?? ''}
                onChange={(e) => update({ ...link, rel: e.target.value || undefined })}
                placeholder="e.g. reference"
              />
            </div>
            <div className="space-y-1">
              <Label className="text-xs">Media type</Label>
              <Input
                value={link['media-type'] ?? ''}
                onChange={(e) => update({ ...link, 'media-type': e.target.value || undefined })}
                placeholder="application/pdf"
              />
            </div>
            <div className="space-y-1">
              <Label className="text-xs">Resource fragment</Label>
              <Input
                value={link['resource-fragment'] ?? ''}
                onChange={(e) => update({ ...link, 'resource-fragment': e.target.value || undefined })}
                placeholder="optional"
              />
            </div>
          </div>
          <div className="space-y-1">
            <Label className="text-xs">Display text</Label>
            <Input
              value={link.text ?? ''}
              onChange={(e) => update({ ...link, text: e.target.value || undefined })}
              placeholder="Human-readable label"
            />
          </div>
        </div>
      )}
    />
  );
}
