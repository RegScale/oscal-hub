'use client';

import { useState, useRef } from 'react';
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogDescription, DialogFooter } from '@/components/ui/dialog';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Alert, AlertDescription } from '@/components/ui/alert';
import { apiClient } from '@/lib/api-client';
import type { ComponentDefinitionRequest } from '@/types/oscal';
import { Upload, Loader2, FileJson, CheckCircle2, AlertCircle } from 'lucide-react';

interface ImportComponentDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  onSuccess?: () => void;
}

export function ImportComponentDialog({ open, onOpenChange, onSuccess }: ImportComponentDialogProps) {
  const fileInputRef = useRef<HTMLInputElement>(null);
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [jsonContent, setJsonContent] = useState<Record<string, unknown> | null>(null);
  const [parseError, setParseError] = useState<string | null>(null);
  const [isImporting, setIsImporting] = useState(false);
  const [importError, setImportError] = useState<string | null>(null);
  const [importSuccess, setImportSuccess] = useState(false);

  // Extracted metadata from JSON
  const [metadata, setMetadata] = useState<{
    title: string;
    version: string;
    oscalVersion: string;
    description: string;
    componentCount: number;
    controlCount: number;
  } | null>(null);

  const handleFileSelect = async (event: React.ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0];
    if (!file) return;

    setSelectedFile(file);
    setParseError(null);
    setJsonContent(null);
    setMetadata(null);
    setImportError(null);
    setImportSuccess(false);

    try {
      const text = await file.text();
      const json = JSON.parse(text);

      // Validate it's a component-definition
      if (!json['component-definition']) {
        setParseError('File does not appear to be an OSCAL Component Definition. Missing "component-definition" root element.');
        return;
      }

      const compDef = json['component-definition'];

      // Extract metadata
      const title = compDef.metadata?.title || 'Untitled Component';
      const version = compDef.metadata?.version || '1.0.0';
      const oscalVersion = compDef.metadata?.['oscal-version'] || '1.1.3';
      const description = compDef.metadata?.description || '';
      const componentCount = compDef.components?.length || 0;

      // Count control implementations
      let controlCount = 0;
      if (compDef['control-implementations']) {
        compDef['control-implementations'].forEach((ci: Record<string, unknown>) => {
          const implReqs = ci['implemented-requirements'];
          if (Array.isArray(implReqs)) {
            controlCount += implReqs.length;
          }
        });
      }

      setJsonContent(json);
      setMetadata({
        title,
        version,
        oscalVersion,
        description,
        componentCount,
        controlCount,
      });
    } catch (error) {
      setParseError(error instanceof Error ? error.message : 'Failed to parse JSON file');
      console.error('Error parsing JSON:', error);
    }
  };

  const handleImport = async () => {
    if (!selectedFile || !jsonContent || !metadata) return;

    setIsImporting(true);
    setImportError(null);
    setImportSuccess(false);

    try {
      const request: ComponentDefinitionRequest = {
        title: metadata.title,
        description: metadata.description,
        version: metadata.version,
        oscalVersion: metadata.oscalVersion,
        filename: selectedFile.name,
        jsonContent: JSON.stringify(jsonContent, null, 2),
        componentCount: metadata.componentCount,
        controlCount: metadata.controlCount,
      };

      await apiClient.createComponentDefinition(request);
      setImportSuccess(true);

      // Wait a moment to show success, then close
      setTimeout(() => {
        handleClose();
        if (onSuccess) {
          onSuccess();
        }
      }, 1500);
    } catch (error) {
      setImportError(error instanceof Error ? error.message : 'Failed to import component');
      console.error('Error importing component:', error);
    } finally {
      setIsImporting(false);
    }
  };

  const handleClose = () => {
    setSelectedFile(null);
    setJsonContent(null);
    setParseError(null);
    setMetadata(null);
    setImportError(null);
    setImportSuccess(false);
    if (fileInputRef.current) {
      fileInputRef.current.value = '';
    }
    onOpenChange(false);
  };

  const handleDrop = (event: React.DragEvent<HTMLDivElement>) => {
    event.preventDefault();
    const file = event.dataTransfer.files[0];
    if (file && file.type === 'application/json') {
      // Simulate file input change
      const dataTransfer = new DataTransfer();
      dataTransfer.items.add(file);
      if (fileInputRef.current) {
        fileInputRef.current.files = dataTransfer.files;
        handleFileSelect({ target: { files: dataTransfer.files } } as React.ChangeEvent<HTMLInputElement>);
      }
    }
  };

  const handleDragOver = (event: React.DragEvent<HTMLDivElement>) => {
    event.preventDefault();
  };

  return (
    <Dialog open={open} onOpenChange={handleClose}>
      <DialogContent className="max-w-2xl">
        <DialogHeader>
          <DialogTitle>Import OSCAL Component Definition</DialogTitle>
          <DialogDescription>
            Upload an existing OSCAL Component Definition JSON file
          </DialogDescription>
        </DialogHeader>

        <div className="space-y-6">
          {/* File Upload */}
          <div className="space-y-2">
            <Label htmlFor="file-upload">Select JSON File</Label>
            <div
              className="border-2 border-dashed rounded-lg p-8 text-center hover:border-primary/50 transition-colors cursor-pointer"
              onDrop={handleDrop}
              onDragOver={handleDragOver}
              onClick={() => fileInputRef.current?.click()}
            >
              <input
                ref={fileInputRef}
                id="file-upload"
                type="file"
                accept=".json,application/json"
                onChange={handleFileSelect}
                className="hidden"
              />
              <div className="flex flex-col items-center gap-2">
                <div className="p-3 rounded-full bg-muted">
                  {selectedFile ? (
                    <FileJson className="h-8 w-8 text-primary" />
                  ) : (
                    <Upload className="h-8 w-8 text-muted-foreground" />
                  )}
                </div>
                {selectedFile ? (
                  <>
                    <p className="font-medium">{selectedFile.name}</p>
                    <p className="text-sm text-muted-foreground">
                      {(selectedFile.size / 1024).toFixed(2)} KB
                    </p>
                  </>
                ) : (
                  <>
                    <p className="font-medium">Click to upload or drag and drop</p>
                    <p className="text-sm text-muted-foreground">
                      OSCAL Component Definition JSON file
                    </p>
                  </>
                )}
              </div>
            </div>
          </div>

          {/* Parse Error */}
          {parseError && (
            <Alert variant="destructive">
              <AlertCircle className="h-4 w-4" />
              <AlertDescription>{parseError}</AlertDescription>
            </Alert>
          )}

          {/* Metadata Preview */}
          {metadata && !parseError && (
            <div className="space-y-4">
              <div className="border rounded-lg p-4 space-y-3">
                <h4 className="font-medium">Component Definition Details</h4>
                <div className="grid grid-cols-2 gap-3 text-sm">
                  <div>
                    <span className="text-muted-foreground">Title:</span>
                    <p className="font-medium">{metadata.title}</p>
                  </div>
                  <div>
                    <span className="text-muted-foreground">Version:</span>
                    <p className="font-medium">{metadata.version}</p>
                  </div>
                  <div>
                    <span className="text-muted-foreground">OSCAL Version:</span>
                    <p className="font-medium">{metadata.oscalVersion}</p>
                  </div>
                  <div>
                    <span className="text-muted-foreground">Components:</span>
                    <p className="font-medium">{metadata.componentCount}</p>
                  </div>
                  <div className="col-span-2">
                    <span className="text-muted-foreground">Controls:</span>
                    <p className="font-medium">{metadata.controlCount}</p>
                  </div>
                  {metadata.description && (
                    <div className="col-span-2">
                      <span className="text-muted-foreground">Description:</span>
                      <p className="text-sm mt-1">{metadata.description}</p>
                    </div>
                  )}
                </div>
              </div>

              {/* Validation Status */}
              <Alert>
                <CheckCircle2 className="h-4 w-4 text-green-600" />
                <AlertDescription>
                  File is a valid OSCAL Component Definition and ready to import
                </AlertDescription>
              </Alert>
            </div>
          )}

          {/* Import Error */}
          {importError && (
            <Alert variant="destructive">
              <AlertCircle className="h-4 w-4" />
              <AlertDescription>{importError}</AlertDescription>
            </Alert>
          )}

          {/* Success */}
          {importSuccess && (
            <Alert className="border-green-200 bg-green-50 dark:bg-green-950">
              <CheckCircle2 className="h-4 w-4 text-green-600" />
              <AlertDescription className="text-green-900 dark:text-green-100">
                Component definition imported successfully!
              </AlertDescription>
            </Alert>
          )}
        </div>

        <DialogFooter>
          <Button variant="outline" onClick={handleClose} disabled={isImporting}>
            Cancel
          </Button>
          <Button
            onClick={handleImport}
            disabled={!metadata || isImporting || importSuccess}
          >
            {isImporting ? (
              <>
                <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                Importing...
              </>
            ) : importSuccess ? (
              <>
                <CheckCircle2 className="mr-2 h-4 w-4" />
                Imported
              </>
            ) : (
              <>
                <Upload className="mr-2 h-4 w-4" />
                Import Component
              </>
            )}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
