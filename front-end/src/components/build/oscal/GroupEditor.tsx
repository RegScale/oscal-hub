'use client';

import { useState } from 'react';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { Dialog, DialogContent, DialogHeader, DialogTitle } from '@/components/ui/dialog';
import {
  ChevronDown,
  ChevronRight,
  Plus,
  Trash2,
  FolderOpen,
  FileText,
  Pencil,
} from 'lucide-react';
import { ParamEditor } from './ParamEditor';
import { PartEditor } from './PartEditor';
import { PropsEditor } from './PropsEditor';
import { LinksEditor } from './LinksEditor';
import { ControlEditor } from './ControlEditor';
import type { Group, Control } from '@/types/oscal-models';

interface GroupEditorProps {
  group: Group;
  onChange: (next: Group) => void;
  onRemove?: () => void;
  depth?: number;
}

export function GroupEditor({ group, onChange, onRemove, depth = 0 }: GroupEditorProps) {
  const [expanded, setExpanded] = useState(depth === 0);
  const [editingControlIndex, setEditingControlIndex] = useState<number | null>(null);
  const [showProps, setShowProps] = useState(false);

  const childGroups = group.groups ?? [];
  const childControls = group.controls ?? [];

  return (
    <div className="rounded-md border bg-background/40">
      <div className="flex items-center gap-2 p-2 border-b bg-muted/30">
        <Button
          type="button"
          size="sm"
          variant="ghost"
          onClick={() => setExpanded((v) => !v)}
          className="h-7 w-7 p-0"
        >
          {expanded ? <ChevronDown className="h-4 w-4" /> : <ChevronRight className="h-4 w-4" />}
        </Button>
        <FolderOpen className="h-4 w-4 text-amber-600" />
        <Input
          value={group.title}
          onChange={(e) => onChange({ ...group, title: e.target.value })}
          placeholder="Group title"
          className="flex-1 h-7"
        />
        <Input
          value={group.id ?? ''}
          onChange={(e) => onChange({ ...group, id: e.target.value || undefined })}
          placeholder="group-id"
          className="w-32 h-7 font-mono text-xs"
        />
        <Badge variant="secondary" className="text-xs">
          {childControls.length}c · {childGroups.length}g
        </Badge>
        {onRemove && (
          <Button
            type="button"
            size="sm"
            variant="ghost"
            onClick={onRemove}
            className="h-7 w-7 p-0 text-destructive"
          >
            <Trash2 className="h-3.5 w-3.5" />
          </Button>
        )}
      </div>

      {expanded && (
        <div className="p-3 space-y-3">
          <div className="grid grid-cols-1 md:grid-cols-2 gap-2">
            <div className="space-y-1">
              <Label className="text-xs">Class</Label>
              <Input
                value={group.class ?? ''}
                onChange={(e) => onChange({ ...group, class: e.target.value || undefined })}
                placeholder="family"
              />
            </div>
            <div className="flex items-end justify-end">
              <Button
                type="button"
                size="sm"
                variant="outline"
                onClick={() => setShowProps((v) => !v)}
              >
                {showProps ? 'Hide' : 'Show'} group metadata
              </Button>
            </div>
          </div>

          {showProps && (
            <div className="space-y-3 rounded-md border p-3 bg-muted/20">
              <PropsEditor value={group.props} onChange={(p) => onChange({ ...group, props: p })} />
              <LinksEditor value={group.links} onChange={(l) => onChange({ ...group, links: l })} />
              <ParamEditor value={group.params} onChange={(p) => onChange({ ...group, params: p })} />
              <PartEditor value={group.parts} onChange={(p) => onChange({ ...group, parts: p })} />
            </div>
          )}

          {/* Controls */}
          <div className="space-y-2">
            <div className="flex items-center justify-between">
              <h5 className="text-sm font-semibold flex items-center gap-1">
                <FileText className="h-3.5 w-3.5" /> Controls ({childControls.length})
              </h5>
              <Button
                type="button"
                size="sm"
                variant="outline"
                onClick={() =>
                  onChange({
                    ...group,
                    controls: [...childControls, { id: `ctl-${childControls.length + 1}`, title: 'New control' }],
                  })
                }
              >
                <Plus className="h-3.5 w-3.5 mr-1" />
                Add control
              </Button>
            </div>
            {childControls.length === 0 ? (
              <p className="text-xs italic text-muted-foreground border border-dashed rounded p-3 text-center">
                No controls yet.
              </p>
            ) : (
              <div className="space-y-1">
                {childControls.map((c, i) => (
                  <ControlRow
                    key={i}
                    control={c}
                    onEdit={() => setEditingControlIndex(i)}
                    onRemove={() =>
                      onChange({
                        ...group,
                        controls: childControls.filter((_, j) => j !== i),
                      })
                    }
                  />
                ))}
              </div>
            )}
          </div>

          {/* Nested groups */}
          {depth < 4 && (
            <div className="space-y-2 pl-3 border-l-2 border-amber-500/40">
              <div className="flex items-center justify-between">
                <h5 className="text-sm font-semibold flex items-center gap-1">
                  <FolderOpen className="h-3.5 w-3.5" /> Nested groups ({childGroups.length})
                </h5>
                <Button
                  type="button"
                  size="sm"
                  variant="outline"
                  onClick={() =>
                    onChange({
                      ...group,
                      groups: [...childGroups, { title: 'New subgroup' }],
                    })
                  }
                >
                  <Plus className="h-3.5 w-3.5 mr-1" />
                  Add subgroup
                </Button>
              </div>
              {childGroups.map((g, i) => (
                <GroupEditor
                  key={i}
                  group={g}
                  onChange={(next) => {
                    const arr = [...childGroups];
                    arr[i] = next;
                    onChange({ ...group, groups: arr });
                  }}
                  onRemove={() =>
                    onChange({ ...group, groups: childGroups.filter((_, j) => j !== i) })
                  }
                  depth={depth + 1}
                />
              ))}
            </div>
          )}
        </div>
      )}

      <Dialog
        open={editingControlIndex !== null}
        onOpenChange={(open) => {
          if (!open) setEditingControlIndex(null);
        }}
      >
        <DialogContent className="max-w-3xl max-h-[90vh] overflow-y-auto">
          <DialogHeader>
            <DialogTitle>
              Edit control{editingControlIndex !== null ? `: ${childControls[editingControlIndex]?.id || ''}` : ''}
            </DialogTitle>
          </DialogHeader>
          {editingControlIndex !== null && childControls[editingControlIndex] && (
            <ControlEditor
              inline
              control={childControls[editingControlIndex]}
              onChange={(next) => {
                const arr = [...childControls];
                arr[editingControlIndex] = next;
                onChange({ ...group, controls: arr });
              }}
            />
          )}
        </DialogContent>
      </Dialog>
    </div>
  );
}

function ControlRow({
  control,
  onEdit,
  onRemove,
}: {
  control: Control;
  onEdit: () => void;
  onRemove: () => void;
}) {
  return (
    <div className="flex items-center gap-2 px-2 py-1.5 rounded border bg-background hover:bg-accent/30">
      <FileText className="h-3.5 w-3.5 text-muted-foreground flex-shrink-0" />
      <Badge variant="outline" className="font-mono text-xs flex-shrink-0">{control.id || '(no id)'}</Badge>
      <span className="text-sm flex-1 truncate">{control.title || '(untitled)'}</span>
      {control.controls && control.controls.length > 0 && (
        <Badge variant="secondary" className="text-xs">+{control.controls.length}</Badge>
      )}
      {control.parts && control.parts.length > 0 && (
        <Badge variant="secondary" className="text-xs">{control.parts.length} parts</Badge>
      )}
      <Button type="button" size="sm" variant="ghost" onClick={onEdit} className="h-7 w-7 p-0">
        <Pencil className="h-3.5 w-3.5" />
      </Button>
      <Button type="button" size="sm" variant="ghost" onClick={onRemove} className="h-7 w-7 p-0 text-destructive">
        <Trash2 className="h-3.5 w-3.5" />
      </Button>
    </div>
  );
}
