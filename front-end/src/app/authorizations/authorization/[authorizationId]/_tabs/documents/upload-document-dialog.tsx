'use client';

import { useState } from 'react';
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogDescription, DialogFooter } from '@/components/ui/dialog';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Textarea } from '@/components/ui/textarea';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { Loader2 } from 'lucide-react';
import { toast } from 'sonner';
import { apiClient } from '@/lib/api-client';
import { ALL_DOCUMENT_TYPES, DOCUMENT_TYPE_LABELS } from './document-type-labels';
import type { DocumentType } from '@/types/oscal';

interface Props {
  authorizationId: number;
  open: boolean;
  onOpenChange: (open: boolean) => void;
  onUploaded: () => void;
}

export function UploadDocumentDialog({ authorizationId, open, onOpenChange, onUploaded }: Props) {
  const [file, setFile] = useState<File | null>(null);
  const [documentType, setDocumentType] = useState<DocumentType>('OTHER');
  const [description, setDescription] = useState('');
  const [tags, setTags] = useState('');
  const [version, setVersion] = useState('');
  const [effectiveDate, setEffectiveDate] = useState('');
  const [expiresAt, setExpiresAt] = useState('');
  const [uploading, setUploading] = useState(false);

  const reset = () => {
    setFile(null);
    setDocumentType('OTHER');
    setDescription('');
    setTags('');
    setVersion('');
    setEffectiveDate('');
    setExpiresAt('');
  };

  const handleSubmit = async () => {
    if (!file) {
      toast.error('Pick a file first');
      return;
    }
    setUploading(true);
    try {
      await apiClient.uploadDocument(authorizationId, file, {
        documentType,
        description: description || undefined,
        tags: tags || undefined,
        version: version || undefined,
        effectiveDate: effectiveDate || undefined,
        expiresAt: expiresAt || undefined,
      });
      toast.success(`Uploaded ${file.name}`);
      reset();
      onUploaded();
      onOpenChange(false);
    } catch (e) {
      const msg = e instanceof Error ? e.message : 'Upload failed';
      toast.error(msg, { duration: 8000 });
    } finally {
      setUploading(false);
    }
  };

  return (
    <Dialog open={open} onOpenChange={(v) => { if (!uploading) onOpenChange(v); }}>
      <DialogContent className="max-w-xl">
        <DialogHeader>
          <DialogTitle>Upload document</DialogTitle>
          <DialogDescription>
            Attach a supporting artifact to this authorization. Required: file + document type.
          </DialogDescription>
        </DialogHeader>

        <div className="space-y-4">
          <div>
            <Label htmlFor="doc-file">File</Label>
            <Input
              id="doc-file"
              type="file"
              onChange={(e) => setFile(e.target.files?.[0] ?? null)}
              disabled={uploading}
            />
          </div>

          <div>
            <Label htmlFor="doc-type">Document type</Label>
            <Select value={documentType} onValueChange={(v) => setDocumentType(v as DocumentType)} disabled={uploading}>
              <SelectTrigger id="doc-type">
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
            <Label htmlFor="doc-desc">Description</Label>
            <Textarea
              id="doc-desc"
              rows={2}
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              disabled={uploading}
              placeholder="Optional. What is this document?"
            />
          </div>

          <div className="grid grid-cols-3 gap-3">
            <div>
              <Label htmlFor="doc-version">Version</Label>
              <Input
                id="doc-version"
                value={version}
                onChange={(e) => setVersion(e.target.value)}
                disabled={uploading}
                placeholder="v1.0"
              />
            </div>
            <div>
              <Label htmlFor="doc-effective">Effective date</Label>
              <Input
                id="doc-effective"
                type="date"
                value={effectiveDate}
                onChange={(e) => setEffectiveDate(e.target.value)}
                disabled={uploading}
              />
            </div>
            <div>
              <Label htmlFor="doc-expires">Expires</Label>
              <Input
                id="doc-expires"
                type="date"
                value={expiresAt}
                onChange={(e) => setExpiresAt(e.target.value)}
                disabled={uploading}
              />
            </div>
          </div>

          <div>
            <Label htmlFor="doc-tags">Tags</Label>
            <Input
              id="doc-tags"
              value={tags}
              onChange={(e) => setTags(e.target.value)}
              disabled={uploading}
              placeholder="comma,separated,tags"
            />
          </div>
        </div>

        <DialogFooter>
          <Button variant="ghost" onClick={() => onOpenChange(false)} disabled={uploading}>
            Cancel
          </Button>
          <Button onClick={handleSubmit} disabled={!file || uploading}>
            {uploading && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
            Upload
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
