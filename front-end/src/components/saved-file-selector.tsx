'use client';

import { useState, useEffect, useImperativeHandle, forwardRef } from 'react';
import { FileText, Clock, Trash2, RefreshCw, Upload } from 'lucide-react';
import { Card, CardHeader, CardTitle, CardContent } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { Alert, AlertDescription } from '@/components/ui/alert';
import { apiClient } from '@/lib/api-client';
import type { SavedFile, OscalFormat, OscalModelType } from '@/types/oscal';

interface SavedFileSelectorProps {
  onFileSelect: (file: SavedFile, content: string) => void;
  onUploadNew: () => void;
  showUploadButton?: boolean;
}

export interface SavedFileSelectorRef {
  refresh: () => void;
}

export const SavedFileSelector = forwardRef<SavedFileSelectorRef, SavedFileSelectorProps>(
  function SavedFileSelector({ onFileSelect, onUploadNew, showUploadButton = true }, ref) {
  const [savedFiles, setSavedFiles] = useState<SavedFile[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [selectedFileId, setSelectedFileId] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  const loadFiles = async () => {
    setIsLoading(true);
    setError(null);
    try {
      const files = await apiClient.getSavedFiles();
      setSavedFiles(files);
    } catch (err) {
      setError('Failed to load saved files');
      console.error(err);
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    loadFiles();
  }, []);

  // Expose refresh method to parent
  useImperativeHandle(ref, () => ({
    refresh: loadFiles,
  }));

  const handleFileClick = async (file: SavedFile) => {
    try {
      setSelectedFileId(file.id);
      const content = await apiClient.getFileContent(file.id);
      if (content) {
        onFileSelect(file, content);
      } else {
        setError('Failed to load file content');
      }
    } catch (err) {
      setError('Failed to load file');
      console.error(err);
    }
  };

  const handleDelete = async (fileId: string, event: React.MouseEvent) => {
    event.stopPropagation();
    if (!confirm('Are you sure you want to delete this file?')) {
      return;
    }

    try {
      const success = await apiClient.deleteSavedFile(fileId);
      if (success) {
        setSavedFiles(savedFiles.filter(f => f.id !== fileId));
        if (selectedFileId === fileId) {
          setSelectedFileId(null);
        }
      } else {
        setError('Failed to delete file');
      }
    } catch (err) {
      setError('Failed to delete file');
      console.error(err);
    }
  };

  const formatFileSize = (bytes: number): string => {
    if (bytes < 1024) return bytes + ' B';
    if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(2) + ' KB';
    return (bytes / (1024 * 1024)).toFixed(2) + ' MB';
  };

  const formatDate = (dateString: string): string => {
    const date = new Date(dateString);
    const now = new Date();
    const diff = now.getTime() - date.getTime();
    const minutes = Math.floor(diff / 60000);
    const hours = Math.floor(diff / 3600000);
    const days = Math.floor(diff / 86400000);

    if (minutes < 1) return 'Just now';
    if (minutes < 60) return `${minutes} minute${minutes !== 1 ? 's' : ''} ago`;
    if (hours < 24) return `${hours} hour${hours !== 1 ? 's' : ''} ago`;
    if (days < 7) return `${days} day${days !== 1 ? 's' : ''} ago`;
    return date.toLocaleDateString();
  };

  const getModelTypeBadgeColor = (modelType?: OscalModelType): string => {
    if (!modelType) return 'bg-gray-500/10 text-gray-500';

    const colors: Record<string, string> = {
      'catalog': 'bg-blue-500/10 text-blue-500',
      'profile': 'bg-purple-500/10 text-purple-500',
      'component-definition': 'bg-green-500/10 text-green-500',
      'system-security-plan': 'bg-orange-500/10 text-orange-500',
      'assessment-plan': 'bg-yellow-500/10 text-yellow-500',
      'assessment-results': 'bg-red-500/10 text-red-500',
      'plan-of-action-and-milestones': 'bg-pink-500/10 text-pink-500',
    };
    return colors[modelType] || 'bg-gray-500/10 text-gray-500';
  };

  return (
    <Card>
      <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-4">
        <CardTitle>Saved Documents</CardTitle>
        <div className="flex gap-2">
          <Button
            variant="ghost"
            size="sm"
            onClick={loadFiles}
            disabled={isLoading}
            aria-label="Refresh saved files list"
          >
            <RefreshCw className={`h-4 w-4 ${isLoading ? 'animate-spin' : ''}`} />
          </Button>
          {showUploadButton && (
            <Button
              variant="outline"
              size="sm"
              onClick={onUploadNew}
              aria-label="Upload new document"
            >
              <Upload className="h-4 w-4 mr-2" />
              Upload New
            </Button>
          )}
        </div>
      </CardHeader>
      <CardContent className="space-y-3">
        {error && (
          <Alert variant="destructive">
            <AlertDescription>{error}</AlertDescription>
          </Alert>
        )}

        {isLoading ? (
          <div className="text-center py-8 text-muted-foreground">
            <RefreshCw className="h-8 w-8 animate-spin mx-auto mb-2" />
            Loading saved files...
          </div>
        ) : savedFiles.length === 0 ? (
          <div className="text-center py-8 text-muted-foreground">
            <FileText className="h-12 w-12 mx-auto mb-2 opacity-30" />
            <p className="text-sm">No saved files yet</p>
            {showUploadButton && (
              <p className="text-xs mt-1">Upload a document to get started</p>
            )}
          </div>
        ) : (
          <div className="space-y-2 max-h-96 overflow-y-auto">
            {savedFiles.map((file) => (
              <div
                key={file.id}
                onClick={() => handleFileClick(file)}
                className={`p-3 rounded-lg border cursor-pointer transition-colors hover:bg-accent ${
                  selectedFileId === file.id ? 'bg-accent border-primary' : ''
                }`}
                role="button"
                tabIndex={0}
                aria-label={`Select file ${file.fileName}`}
              >
                <div className="flex items-start justify-between gap-2">
                  <div className="flex-1 min-w-0">
                    <div className="flex items-center gap-2 mb-1">
                      <FileText className="h-4 w-4 text-primary flex-shrink-0" />
                      <p className="font-medium text-sm truncate">{file.fileName}</p>
                    </div>
                    <div className="flex flex-wrap items-center gap-2 text-xs text-muted-foreground">
                      <Badge
                        variant="secondary"
                        className="text-xs uppercase font-mono"
                      >
                        {file.format}
                      </Badge>
                      {file.modelType && (
                        <Badge
                          className={`text-xs ${getModelTypeBadgeColor(file.modelType)}`}
                        >
                          {file.modelType.replace(/-/g, ' ')}
                        </Badge>
                      )}
                      <span className="flex items-center gap-1">
                        <Clock className="h-3 w-3" />
                        {formatDate(file.uploadedAt)}
                      </span>
                      <span>{formatFileSize(file.fileSize)}</span>
                    </div>
                  </div>
                  <Button
                    variant="ghost"
                    size="sm"
                    onClick={(e) => handleDelete(file.id, e)}
                    className="h-8 w-8 p-0 flex-shrink-0"
                    aria-label={`Delete file ${file.fileName}`}
                  >
                    <Trash2 className="h-4 w-4 text-destructive" />
                  </Button>
                </div>
              </div>
            ))}
          </div>
        )}
      </CardContent>
    </Card>
  );
});

SavedFileSelector.displayName = 'SavedFileSelector';
