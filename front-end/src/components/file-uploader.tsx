'use client';

import { useCallback } from 'react';
import { useDropzone } from 'react-dropzone';
import { Upload, FileText, X } from 'lucide-react';
import { Card } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';

interface FileUploaderProps {
  onFileSelect: (file: File, content: string) => void;
  selectedFile: File | null;
  onClear: () => void;
  acceptedFormats?: string[];
  maxSize?: number;
}

export function FileUploader({
  onFileSelect,
  selectedFile,
  onClear,
  acceptedFormats = ['.xml', '.json', '.yaml', '.yml'],
  maxSize = 10485760, // 10MB
}: FileUploaderProps) {
  const onDrop = useCallback(
    (acceptedFiles: File[]) => {
      if (acceptedFiles.length > 0) {
        const file = acceptedFiles[0];
        const reader = new FileReader();

        reader.onload = (e) => {
          const content = e.target?.result as string;
          onFileSelect(file, content);
        };

        reader.readAsText(file);
      }
    },
    [onFileSelect]
  );

  const { getRootProps, getInputProps, isDragActive } = useDropzone({
    onDrop,
    accept: {
      'application/xml': ['.xml'],
      'application/json': ['.json'],
      'text/yaml': ['.yaml', '.yml'],
    },
    maxFiles: 1,
    maxSize,
  });

  if (selectedFile) {
    return (
      <Card className="p-4">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-3">
            <div className="p-2 rounded-lg bg-primary/10">
              <FileText className="h-5 w-5 text-primary" />
            </div>
            <div>
              <p className="font-medium text-sm">{selectedFile.name}</p>
              <p className="text-xs text-muted-foreground">
                {(selectedFile.size / 1024).toFixed(2)} KB
              </p>
            </div>
          </div>
          <Button
            variant="ghost"
            size="sm"
            onClick={onClear}
            className="h-8 w-8 p-0"
            aria-label="Clear selected file"
          >
            <X className="h-4 w-4" aria-hidden="true" />
          </Button>
        </div>
      </Card>
    );
  }

  return (
    <Card
      {...getRootProps()}
      className={`p-8 border-2 border-dashed cursor-pointer transition-colors ${
        isDragActive
          ? 'border-primary bg-primary/5'
          : 'border-border hover:border-primary/50 hover:bg-primary/5'
      }`}
    >
      <input {...getInputProps()} />
      <div className="flex flex-col items-center justify-center gap-4 text-center">
        <div className="p-4 rounded-full bg-primary/10">
          <Upload className="h-8 w-8 text-primary" />
        </div>
        <div>
          <p className="text-sm font-medium mb-1">
            {isDragActive ? 'Drop your file here' : 'Drag & drop your OSCAL file'}
          </p>
          <p className="text-xs text-muted-foreground mb-3">
            or click to browse
          </p>
          <div className="flex flex-wrap gap-2 justify-center">
            {acceptedFormats.map((format) => (
              <Badge key={format} variant="secondary" className="text-xs">
                {format}
              </Badge>
            ))}
          </div>
        </div>
        <p className="text-xs text-muted-foreground">
          Maximum file size: {(maxSize / 1024 / 1024).toFixed(0)}MB
        </p>
      </div>
    </Card>
  );
}
