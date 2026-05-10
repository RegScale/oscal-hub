'use client';

import { useEffect, useState } from 'react';
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter } from '@/components/ui/dialog';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Textarea } from '@/components/ui/textarea';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { Loader2 } from 'lucide-react';
import { toast } from 'sonner';
import { apiClient } from '@/lib/api-client';
import { ALL_DOCUMENT_TYPES, DOCUMENT_TYPE_LABELS } from './document-type-labels';
import type { AuthorizationDocumentResponse, DocumentType } from '@/types/oscal';

interface Props {
  authorizationId: number;
  document: AuthorizationDocumentResponse | null;
  open: boolean;
  onOpenChange: (open: boolean) => void;
  onUpdated: () => void;
}

export function EditDocumentMetadataDialog({ authorizationId, document, open, onOpenChange, onUpdated }: Props) {
  const [documentType, setDocumentType] = useState<DocumentType>('OTHER');
  const [description, setDescription] = useState('');
  const [tags, setTags] = useState('');
  const [version, setVersion] = useState('');
  const [effectiveDate, setEffectiveDate] = useState('');
  const [expiresAt, setExpiresAt] = useState('');
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    if (document) {
      setDocumentType(document.documentType);
      setDescription(document.description ?? '');
      setTags(document.tags ?? '');
      setVersion(document.version ?? '');
      setEffectiveDate(document.effectiveDate ?? '');
      setExpiresAt(document.expiresAt ?? '');
    }
  }, [document]);

  const handleSave = async () => {
    if (!document) return;
    setSaving(true);
    try {
      await apiClient.updateDocumentMetadata(authorizationId, document.id, {
        documentType,
        description: description || null,
        tags: tags || null,
        version: version || null,
        effectiveDate: effectiveDate || null,
        expiresAt: expiresAt || null,
      });
      toast.success('Metadata updated');
      onUpdated();
      onOpenChange(false);
    } catch (e) {
      toast.error('Failed to update metadata');
    } finally {
      setSaving(false);
    }
  };

  return (
    <Dialog open={open} onOpenChange={(v) => { if (!saving) onOpenChange(v); }}>
      <DialogContent className="max-w-xl">
        <DialogHeader>
          <DialogTitle>Edit document metadata</DialogTitle>
        </DialogHeader>

        {document && (
          <div className="space-y-4">
            <p className="text-sm text-muted-foreground">{document.originalFilename}</p>

            <div>
              <Label htmlFor="edit-type">Document type</Label>
              <Select value={documentType} onValueChange={(v) => setDocumentType(v as DocumentType)} disabled={saving}>
                <SelectTrigger id="edit-type">
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  {ALL_DOCUMENT_TYPES.map((t) => (
                    <SelectItem key={t} value={t}>{DOCUMENT_TYPE_LABELS[t]}</SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>

            <div>
              <Label htmlFor="edit-desc">Description</Label>
              <Textarea id="edit-desc" rows={2} value={description}
                onChange={(e) => setDescription(e.target.value)} disabled={saving} />
            </div>

            <div className="grid grid-cols-3 gap-3">
              <div>
                <Label htmlFor="edit-version">Version</Label>
                <Input id="edit-version" value={version} onChange={(e) => setVersion(e.target.value)} disabled={saving} />
              </div>
              <div>
                <Label htmlFor="edit-effective">Effective date</Label>
                <Input id="edit-effective" type="date" value={effectiveDate}
                  onChange={(e) => setEffectiveDate(e.target.value)} disabled={saving} />
              </div>
              <div>
                <Label htmlFor="edit-expires">Expires</Label>
                <Input id="edit-expires" type="date" value={expiresAt}
                  onChange={(e) => setExpiresAt(e.target.value)} disabled={saving} />
              </div>
            </div>

            <div>
              <Label htmlFor="edit-tags">Tags</Label>
              <Input id="edit-tags" value={tags} onChange={(e) => setTags(e.target.value)} disabled={saving} />
            </div>
          </div>
        )}

        <DialogFooter>
          <Button variant="ghost" onClick={() => onOpenChange(false)} disabled={saving}>Cancel</Button>
          <Button onClick={handleSave} disabled={saving}>
            {saving && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
            Save
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
