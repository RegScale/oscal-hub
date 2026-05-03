'use client';

import { useState } from 'react';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { ChevronDown, ChevronRight } from 'lucide-react';
import { PropsEditor } from './PropsEditor';
import { LinksEditor } from './LinksEditor';
import { RolesEditor } from './RolesEditor';
import { PartiesEditor } from './PartiesEditor';
import { ResponsiblePartiesEditor } from './ResponsiblePartiesEditor';
import { MarkdownField } from './MarkdownField';
import { DateTimeField } from './DateTimeField';
import { CURRENT_OSCAL_VERSION } from '@/types/oscal-models';
import type { Metadata } from '@/types/oscal-models';

interface MetadataEditorProps {
  value: Metadata;
  onChange: (next: Metadata) => void;
}

export function MetadataEditor({ value, onChange }: MetadataEditorProps) {
  const [openSection, setOpenSection] = useState<string | null>('basic');

  const toggle = (key: string) => setOpenSection((prev) => (prev === key ? null : key));

  return (
    <div className="space-y-3">
      <Section title="Basic" open={openSection === 'basic'} onToggle={() => toggle('basic')}>
        <div className="space-y-3">
          <div className="space-y-1">
            <Label>Title *</Label>
            <Input
              value={value.title}
              onChange={(e) => onChange({ ...value, title: e.target.value })}
              placeholder="My Catalog"
            />
          </div>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-2">
            <div className="space-y-1">
              <Label className="text-xs">Version *</Label>
              <Input
                value={value.version}
                onChange={(e) => onChange({ ...value, version: e.target.value })}
                placeholder="1.0.0"
              />
            </div>
            <div className="space-y-1">
              <Label className="text-xs">OSCAL version *</Label>
              <Input
                value={value['oscal-version']}
                onChange={(e) => onChange({ ...value, 'oscal-version': e.target.value })}
                placeholder={CURRENT_OSCAL_VERSION}
              />
            </div>
          </div>
          <DateTimeField
            label="Last modified *"
            value={value['last-modified']}
            allowClear={false}
            onChange={(v) => onChange({ ...value, 'last-modified': v || new Date().toISOString() })}
          />
          <DateTimeField
            label="Published"
            value={value.published ?? ''}
            onChange={(v) => onChange({ ...value, published: v || undefined })}
          />
          <MarkdownField
            label="Remarks"
            value={value.remarks ?? ''}
            onChange={(v) => onChange({ ...value, remarks: v || undefined })}
            rows={2}
          />
        </div>
      </Section>

      <Section
        title={`Roles ${value.roles?.length ? `(${value.roles.length})` : ''}`}
        open={openSection === 'roles'}
        onToggle={() => toggle('roles')}
      >
        <RolesEditor value={value.roles} onChange={(r) => onChange({ ...value, roles: r })} />
      </Section>

      <Section
        title={`Parties ${value.parties?.length ? `(${value.parties.length})` : ''}`}
        open={openSection === 'parties'}
        onToggle={() => toggle('parties')}
      >
        <PartiesEditor value={value.parties} onChange={(p) => onChange({ ...value, parties: p })} />
      </Section>

      <Section
        title={`Responsible parties ${value['responsible-parties']?.length ? `(${value['responsible-parties'].length})` : ''}`}
        open={openSection === 'rps'}
        onToggle={() => toggle('rps')}
      >
        <ResponsiblePartiesEditor
          value={value['responsible-parties']}
          onChange={(rp) => onChange({ ...value, 'responsible-parties': rp })}
          availableRoles={value.roles ?? []}
          availableParties={value.parties ?? []}
        />
      </Section>

      <Section
        title={`Properties ${value.props?.length ? `(${value.props.length})` : ''}`}
        open={openSection === 'props'}
        onToggle={() => toggle('props')}
      >
        <PropsEditor value={value.props} onChange={(p) => onChange({ ...value, props: p })} />
      </Section>

      <Section
        title={`Links ${value.links?.length ? `(${value.links.length})` : ''}`}
        open={openSection === 'links'}
        onToggle={() => toggle('links')}
      >
        <LinksEditor value={value.links} onChange={(l) => onChange({ ...value, links: l })} />
      </Section>
    </div>
  );
}

function Section({
  title,
  open,
  onToggle,
  children,
}: {
  title: string;
  open: boolean;
  onToggle: () => void;
  children: React.ReactNode;
}) {
  return (
    <Card>
      <CardHeader
        className="cursor-pointer py-3"
        onClick={onToggle}
        role="button"
        tabIndex={0}
        onKeyDown={(e) => {
          if (e.key === 'Enter' || e.key === ' ') {
            e.preventDefault();
            onToggle();
          }
        }}
      >
        <CardTitle className="text-sm flex items-center gap-2">
          {open ? <ChevronDown className="h-4 w-4" /> : <ChevronRight className="h-4 w-4" />}
          {title}
        </CardTitle>
      </CardHeader>
      {open && <CardContent>{children}</CardContent>}
    </Card>
  );
}
