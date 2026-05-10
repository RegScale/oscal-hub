'use client';

import { useState } from 'react';
import { useDropzone } from 'react-dropzone';
import { Dialog, DialogContent, DialogDescription, DialogHeader, DialogTitle } from '@/components/ui/dialog';
import { Button } from '@/components/ui/button';
import { Alert, AlertDescription } from '@/components/ui/alert';
import { Textarea } from '@/components/ui/textarea';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';
import { Upload, FileJson, AlertCircle } from 'lucide-react';

export type ImportTarget = 'catalog' | 'profile';

interface ImportJsonDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  target: ImportTarget;
  onImport: (parsed: unknown) => string | null;
}

/**
 * Import dialog that accepts an OSCAL JSON file or pasted JSON.
 * Calls onImport with the parsed object; onImport returns null on success or
 * an error message string on failure.
 */
export function ImportJsonDialog({ open, onOpenChange, target, onImport }: ImportJsonDialogProps) {
  const [error, setError] = useState<string | null>(null);
  const [pasted, setPasted] = useState('');
  const [busy, setBusy] = useState(false);

  const handleParse = (raw: string) => {
    setError(null);
    setBusy(true);
    try {
      const parsed = JSON.parse(raw);
      const err = onImport(parsed);
      if (err) {
        setError(err);
      } else {
        setPasted('');
        onOpenChange(false);
      }
    } catch (e) {
      setError(`Invalid JSON: ${e instanceof Error ? e.message : 'unknown'}`);
    } finally {
      setBusy(false);
    }
  };

  const onDrop = (files: File[]) => {
    const file = files[0];
    if (!file) return;
    const reader = new FileReader();
    reader.onload = () => {
      const text = typeof reader.result === 'string' ? reader.result : '';
      handleParse(text);
    };
    reader.onerror = () => setError('Failed to read file.');
    reader.readAsText(file);
  };

  const { getRootProps, getInputProps, isDragActive } = useDropzone({
    onDrop,
    accept: { 'application/json': ['.json'] },
    multiple: false,
    maxSize: 50 * 1024 * 1024, // 50 MB
  });

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-w-xl">
        <DialogHeader>
          <DialogTitle>Import {target === 'catalog' ? 'catalog' : 'profile'} JSON</DialogTitle>
          <DialogDescription>
            Drop an OSCAL {target} JSON file or paste it below to pre-fill the wizard. Imported
            content replaces the current draft.
          </DialogDescription>
        </DialogHeader>

        <Tabs defaultValue="upload">
          <TabsList className="grid w-full grid-cols-2">
            <TabsTrigger value="upload">
              <Upload className="h-3.5 w-3.5 mr-1" /> Upload file
            </TabsTrigger>
            <TabsTrigger value="paste">
              <FileJson className="h-3.5 w-3.5 mr-1" /> Paste JSON
            </TabsTrigger>
          </TabsList>

          <TabsContent value="upload">
            <div
              {...getRootProps()}
              className={`mt-2 rounded-md border-2 border-dashed p-8 text-center cursor-pointer transition-colors ${
                isDragActive ? 'border-primary bg-primary/5' : 'border-muted-foreground/30 hover:bg-muted/30'
              }`}
            >
              <input {...getInputProps()} />
              <Upload className="h-8 w-8 mx-auto text-muted-foreground mb-2" />
              {isDragActive ? (
                <p className="text-sm">Drop the file here…</p>
              ) : (
                <>
                  <p className="text-sm font-medium">Drag & drop or click to browse</p>
                  <p className="text-xs text-muted-foreground mt-1">.json files up to 50 MB</p>
                </>
              )}
            </div>
          </TabsContent>

          <TabsContent value="paste">
            <Textarea
              value={pasted}
              onChange={(e) => setPasted(e.target.value)}
              placeholder='{ "catalog": { ... } }'
              rows={10}
              className="font-mono text-xs"
            />
            <Button
              type="button"
              className="mt-2 w-full"
              onClick={() => handleParse(pasted)}
              disabled={!pasted.trim() || busy}
            >
              Import
            </Button>
          </TabsContent>
        </Tabs>

        {error && (
          <Alert variant="destructive" className="mt-2">
            <AlertCircle className="h-4 w-4" />
            <AlertDescription>{error}</AlertDescription>
          </Alert>
        )}
      </DialogContent>
    </Dialog>
  );
}
