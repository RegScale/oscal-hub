'use client';

import { useState } from 'react';
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogDescription, DialogFooter } from '@/components/ui/dialog';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Textarea } from '@/components/ui/textarea';
import { Loader2 } from 'lucide-react';
import { toast } from 'sonner';
import { apiClient } from '@/lib/api-client';

interface Props {
  authorizationId: number;
  open: boolean;
  onOpenChange: (open: boolean) => void;
  onUploaded: () => void;
}

export function UploadSnapshotDialog({ authorizationId, open, onOpenChange, onUploaded }: Props) {
  const [file, setFile] = useState<File | null>(null);
  const [notes, setNotes] = useState('');
  const [uploading, setUploading] = useState(false);

  const handleSubmit = async () => {
    if (!file) { toast.error('Pick a file first'); return; }
    setUploading(true);
    try {
      await apiClient.uploadConMonSnapshot(authorizationId, file, notes || undefined);
      toast.success(`Uploaded ${file.name}`);
      setFile(null); setNotes('');
      onUploaded();
      onOpenChange(false);
    } catch {
      toast.error('Upload failed');
    } finally {
      setUploading(false);
    }
  };

  return (
    <Dialog open={open} onOpenChange={(v) => { if (!uploading) onOpenChange(v); }}>
      <DialogContent className="max-w-xl">
        <DialogHeader>
          <DialogTitle>Upload POAM snapshot</DialogTitle>
          <DialogDescription>
            OSCAL JSON/XML/YAML or FedRAMP POA&amp;M Excel template (.xlsx). For other artifacts use the Documents tab.
          </DialogDescription>
        </DialogHeader>

        <div className="space-y-4">
          <div>
            <Label htmlFor="cm-file">File</Label>
            <Input id="cm-file" type="file"
                   accept=".json,.xml,.yaml,.yml,.xlsx"
                   onChange={(e) => setFile(e.target.files?.[0] ?? null)} disabled={uploading} />
          </div>
          <div>
            <Label htmlFor="cm-notes">Notes (optional)</Label>
            <Textarea id="cm-notes" rows={2} value={notes}
                      onChange={(e) => setNotes(e.target.value)} disabled={uploading}
                      placeholder="What's notable about this upload?" />
          </div>
        </div>

        <DialogFooter>
          <Button variant="ghost" onClick={() => onOpenChange(false)} disabled={uploading}>Cancel</Button>
          <Button onClick={handleSubmit} disabled={!file || uploading}>
            {uploading && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
            Upload
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
