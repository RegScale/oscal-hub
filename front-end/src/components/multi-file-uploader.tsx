'use client';

import { useCallback } from 'react';
import { useDropzone } from 'react-dropzone';
import { Upload, FileText, X } from 'lucide-react';
import { Card } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';

export interface FileWithContent {
  file: File;
  content: string;
}

interface MultiFileUploaderProps {
  onFilesSelect: (files: FileWithContent[]) => void;
  selectedFiles: FileWithContent[];
  onRemoveFile: (index: number) => void;
  onClearAll: () => void;
  acceptedFormats?: string[];
  maxSize?: number;
  maxFiles?: number;
}

export function MultiFileUploader({
  onFilesSelect,
  selectedFiles,
  onRemoveFile,
  onClearAll,
  acceptedFormats = ['.xml', '.json', '.yaml', '.yml'],
  maxSize = 10485760, // 10MB per file
  maxFiles = 10,
}: MultiFileUploaderProps) {
  const onDrop = useCallback(
    (acceptedFiles: File[]) => {
      if (acceptedFiles.length > 0) {
        // Read all files and add to existing files
        const filePromises = acceptedFiles.map((file) => {
          return new Promise<FileWithContent>((resolve) => {
            const reader = new FileReader();
            reader.onload = (e) => {
              const content = e.target?.result as string;
              resolve({ file, content });
            };
            reader.readAsText(file);
          });
        });

        Promise.all(filePromises).then((newFiles) => {
          // Combine with existing files, but respect maxFiles limit
          const combined = [...selectedFiles, ...newFiles].slice(0, maxFiles);
          onFilesSelect(combined);
        });
      }
    },
    [selectedFiles, onFilesSelect, maxFiles]
  );

  const { getRootProps, getInputProps, isDragActive } = useDropzone({
    onDrop,
    accept: {
      'application/xml': ['.xml'],
      'application/json': ['.json'],
      'text/yaml': ['.yaml', '.yml'],
    },
    maxFiles: maxFiles - selectedFiles.length,
    maxSize,
    disabled: selectedFiles.length >= maxFiles,
  });

  const remainingSlots = maxFiles - selectedFiles.length;

  return (
    <div className="space-y-4">
      {/* Selected Files List */}
      {selectedFiles.length > 0 && (
        <Card className="p-4">
          <div className="space-y-3">
            <div className="flex items-center justify-between mb-3">
              <h3 className="text-sm font-medium">
                Selected Files ({selectedFiles.length}/{maxFiles})
              </h3>
              <Button
                variant="ghost"
                size="sm"
                onClick={onClearAll}
                className="h-8 text-xs"
              >
                Clear All
              </Button>
            </div>
            <div className="space-y-2">
              {selectedFiles.map((fileWithContent, index) => (
                <div
                  key={index}
                  className="flex items-center justify-between p-2 rounded-lg bg-muted/50"
                >
                  <div className="flex items-center gap-3 flex-1 min-w-0">
                    <div className="p-1.5 rounded bg-primary/10 flex-shrink-0">
                      <FileText className="h-4 w-4 text-primary" />
                    </div>
                    <div className="min-w-0 flex-1">
                      <p className="font-medium text-sm truncate">
                        {fileWithContent.file.name}
                      </p>
                      <p className="text-xs text-muted-foreground">
                        {(fileWithContent.file.size / 1024).toFixed(2)} KB
                      </p>
                    </div>
                  </div>
                  <Button
                    variant="ghost"
                    size="sm"
                    onClick={() => onRemoveFile(index)}
                    className="h-7 w-7 p-0 flex-shrink-0"
                  >
                    <X className="h-3.5 w-3.5" />
                  </Button>
                </div>
              ))}
            </div>
          </div>
        </Card>
      )}

      {/* Upload Area */}
      {selectedFiles.length < maxFiles && (
        <Card
          {...getRootProps()}
          className={`p-6 border-2 border-dashed cursor-pointer transition-colors ${
            isDragActive
              ? 'border-primary bg-primary/5'
              : 'border-border hover:border-primary/50 hover:bg-primary/5'
          }`}
        >
          <input {...getInputProps()} />
          <div className="flex flex-col items-center justify-center gap-3 text-center">
            <div className="p-3 rounded-full bg-primary/10">
              <Upload className="h-6 w-6 text-primary" />
            </div>
            <div>
              <p className="text-sm font-medium mb-1">
                {isDragActive
                  ? 'Drop your files here'
                  : 'Drag & drop OSCAL files or click to browse'}
              </p>
              <p className="text-xs text-muted-foreground mb-2">
                Add up to {remainingSlots} more file{remainingSlots !== 1 ? 's' : ''}
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
              Maximum {(maxSize / 1024 / 1024).toFixed(0)}MB per file
            </p>
          </div>
        </Card>
      )}

      {/* Max Files Reached Message */}
      {selectedFiles.length >= maxFiles && (
        <Card className="p-4 bg-muted/30">
          <p className="text-sm text-center text-muted-foreground">
            Maximum number of files reached ({maxFiles}). Remove files to add more.
          </p>
        </Card>
      )}
    </div>
  );
}
