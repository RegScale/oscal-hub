'use client';

import { useState, useEffect } from 'react';
import Link from 'next/link';
import { ArrowLeft, Play, CheckCircle2, AlertCircle, Loader2, Clock, FileCheck } from 'lucide-react';
import { Card, CardHeader, CardTitle, CardContent } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Alert, AlertDescription } from '@/components/ui/alert';
import { Badge } from '@/components/ui/badge';
import { Progress } from '@/components/ui/progress';
import { MultiFileUploader, type FileWithContent } from '@/components/multi-file-uploader';
import { ModelTypeSelector } from '@/components/model-type-selector';
import { FormatSelector } from '@/components/format-selector';
import { apiClient } from '@/lib/api-client';
import ProtectedRoute from '@/components/ProtectedRoute';
import { toast } from 'sonner';
import type {
  OscalModelType,
  OscalFormat,
  BatchOperationType,
  BatchOperationResult,
  BatchFileResult,
} from '@/types/oscal';

type OperationStatus = 'idle' | 'submitting' | 'processing' | 'completed' | 'error';

export default function BatchOperationsPage() {
  // File management
  const [selectedFiles, setSelectedFiles] = useState<FileWithContent[]>([]);

  // Operation settings
  const [operationType, setOperationType] = useState<BatchOperationType>('VALIDATE');
  const [modelType, setModelType] = useState<OscalModelType | ''>('');
  const [fromFormat, setFromFormat] = useState<OscalFormat>('json');
  const [toFormat, setToFormat] = useState<OscalFormat>('xml');

  // Operation state
  const [operationStatus, setOperationStatus] = useState<OperationStatus>('idle');
  const [operationId, setOperationId] = useState<string | null>(null);
  const [batchResult, setBatchResult] = useState<BatchOperationResult | null>(null);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  // Auto-detect format from first file
  useEffect(() => {
    if (selectedFiles.length > 0) {
      const firstFile = selectedFiles[0].file;
      const extension = firstFile.name.split('.').pop()?.toLowerCase();
      if (extension === 'xml') setFromFormat('xml');
      else if (extension === 'json') setFromFormat('json');
      else if (extension === 'yaml' || extension === 'yml') setFromFormat('yaml');
    }
  }, [selectedFiles]);

  // Poll for batch operation results
  useEffect(() => {
    if (!operationId || operationStatus !== 'processing') return;

    const pollInterval = setInterval(async () => {
      try {
        const result = await apiClient.getBatchOperationResult(operationId);

        // Check if all files have been processed
        if (result.results && result.results.length === result.totalFiles) {
          setBatchResult(result);
          setOperationStatus('completed');
          clearInterval(pollInterval);
          toast.success('Batch operation completed!');
        }
      } catch (error) {
        console.error('Error polling batch operation:', error);
        setErrorMessage('Failed to check operation status');
        setOperationStatus('error');
        clearInterval(pollInterval);
        toast.error('Batch operation failed');
      }
    }, 2000); // Poll every 2 seconds

    return () => clearInterval(pollInterval);
  }, [operationId, operationStatus]);

  const handleFilesSelect = (files: FileWithContent[]) => {
    setSelectedFiles(files);
    setBatchResult(null);
    setErrorMessage(null);
    toast.success(`${files.length} file${files.length !== 1 ? 's' : ''} loaded`);
  };

  const handleRemoveFile = (index: number) => {
    setSelectedFiles((prev) => prev.filter((_, i) => i !== index));
  };

  const handleClearAll = () => {
    setSelectedFiles([]);
    setBatchResult(null);
    setOperationId(null);
    setOperationStatus('idle');
    setErrorMessage(null);
  };

  const handleStartBatch = async () => {
    if (!modelType || selectedFiles.length === 0) return;

    setOperationStatus('submitting');
    setErrorMessage(null);
    setBatchResult(null);
    toast.info(`Processing ${selectedFiles.length} file${selectedFiles.length !== 1 ? 's' : ''}...`);

    try {
      const request = {
        operationType,
        modelType,
        files: selectedFiles.map((f) => ({
          filename: f.file.name,
          content: f.content,
          format: fromFormat.toUpperCase() as OscalFormat,
        })),
        ...(operationType === 'CONVERT' && {
          fromFormat: fromFormat.toUpperCase() as OscalFormat,
          toFormat: toFormat.toUpperCase() as OscalFormat,
        }),
      };

      const result = await apiClient.submitBatchOperation(request);
      setOperationId(result.operationId);
      setOperationStatus('processing');
    } catch (error) {
      console.error('Failed to start batch operation:', error);
      setErrorMessage('Failed to start batch operation. Please try again.');
      setOperationStatus('error');
      toast.error('Batch operation failed');
    }
  };

  const canStartBatch = selectedFiles.length > 0 && modelType && operationStatus === 'idle';
  const progressPercentage = batchResult
    ? Math.round(((batchResult.successCount + batchResult.failureCount) / batchResult.totalFiles) * 100)
    : 0;

  return (
    <ProtectedRoute>
      <div className="min-h-screen bg-background">
      <div className="container mx-auto py-8 px-4">
        {/* Header */}
        <header className="mb-8">
          <Link
            href="/"
            className="inline-flex items-center gap-2 text-sm text-muted-foreground hover:text-foreground mb-4 transition-colors focus:outline-none focus:ring-2 focus:ring-ring focus:ring-offset-2 rounded"
            aria-label="Navigate back to dashboard"
          >
            <ArrowLeft className="h-4 w-4" aria-hidden="true" />
            Back to Dashboard
          </Link>
          <h1 className="text-4xl font-bold mb-2">Batch Operations</h1>
          <p className="text-muted-foreground">
            Process multiple OSCAL files at once (up to 10 files)
          </p>
        </header>

        {/* Two-column layout */}
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
          {/* Left column - Settings */}
          <div className="lg:col-span-1 space-y-6">
            <Card>
              <CardHeader>
                <CardTitle>Operation Settings</CardTitle>
              </CardHeader>
              <CardContent className="space-y-4">
                {/* Operation Type */}
                <div className="space-y-2">
                  <label className="text-sm font-medium">Operation Type</label>
                  <div className="grid grid-cols-2 gap-2" role="group" aria-label="Select batch operation type">
                    <Button
                      variant={operationType === 'VALIDATE' ? 'default' : 'outline'}
                      onClick={() => setOperationType('VALIDATE')}
                      disabled={operationStatus !== 'idle'}
                      className="w-full"
                      aria-label="Select validate operation type"
                      aria-pressed={operationType === 'VALIDATE'}
                    >
                      Validate
                    </Button>
                    <Button
                      variant={operationType === 'CONVERT' ? 'default' : 'outline'}
                      onClick={() => setOperationType('CONVERT')}
                      disabled={operationStatus !== 'idle'}
                      className="w-full"
                      aria-label="Select convert operation type"
                      aria-pressed={operationType === 'CONVERT'}
                    >
                      Convert
                    </Button>
                  </div>
                </div>

                {/* Model Type */}
                <ModelTypeSelector
                  value={modelType}
                  onChange={setModelType}
                  disabled={selectedFiles.length === 0 || operationStatus !== 'idle'}
                />

                {/* Format Settings */}
                {operationType === 'CONVERT' ? (
                  <FormatSelector
                    fromFormat={fromFormat}
                    toFormat={toFormat}
                    onFromFormatChange={setFromFormat}
                    onToFormatChange={setToFormat}
                    disabled={operationStatus !== 'idle'}
                  />
                ) : (
                  <div className="space-y-2">
                    <label className="text-sm font-medium">Detected Format</label>
                    <Badge variant="secondary" className="w-full justify-center py-2">
                      {fromFormat.toUpperCase()}
                    </Badge>
                  </div>
                )}

                {/* Start Button */}
                <Button
                  onClick={handleStartBatch}
                  disabled={!canStartBatch}
                  className="w-full"
                  size="lg"
                  aria-label={operationStatus === 'submitting' ? 'Submitting batch operation, please wait' : `Start batch ${operationType === 'VALIDATE' ? 'validation' : 'conversion'} for ${selectedFiles.length} files`}
                >
                  {operationStatus === 'submitting' ? (
                    <>
                      <Loader2 className="h-4 w-4 mr-2 animate-spin" aria-hidden="true" />
                      Submitting...
                    </>
                  ) : (
                    <>
                      <Play className="h-4 w-4 mr-2" aria-hidden="true" />
                      Start Batch {operationType === 'VALIDATE' ? 'Validation' : 'Conversion'}
                    </>
                  )}
                </Button>

                {canStartBatch && (
                  <p className="text-xs text-center text-muted-foreground" role="status">
                    Ready to process {selectedFiles.length} file{selectedFiles.length !== 1 ? 's' : ''}
                  </p>
                )}
              </CardContent>
            </Card>

            {/* Status Card */}
            {(operationStatus === 'processing' || operationStatus === 'completed') && batchResult && (
              <Card aria-label="Batch operation status" role="region">
                <CardHeader>
                  <CardTitle className="flex items-center gap-2">
                    {operationStatus === 'completed' ? (
                      <>
                        <CheckCircle2 className="h-5 w-5 text-green-500" aria-hidden="true" />
                        Batch Complete
                      </>
                    ) : (
                      <>
                        <Loader2 className="h-5 w-5 text-primary animate-spin" aria-hidden="true" />
                        Processing...
                      </>
                    )}
                  </CardTitle>
                </CardHeader>
                <CardContent className="space-y-4">
                  <div className="space-y-2" role="status" aria-live="polite">
                    <div className="flex justify-between text-sm">
                      <span className="text-muted-foreground">Progress</span>
                      <span className="font-medium">{progressPercentage}%</span>
                    </div>
                    <Progress value={progressPercentage} aria-label={`Batch operation progress: ${progressPercentage}% complete`} />
                  </div>

                  <div className="grid grid-cols-2 gap-3">
                    <div className="text-center p-3 rounded-lg bg-green-500/10">
                      <div className="text-2xl font-bold text-green-500">
                        {batchResult.successCount}
                      </div>
                      <div className="text-xs text-muted-foreground">Success</div>
                    </div>
                    <div className="text-center p-3 rounded-lg bg-destructive/10">
                      <div className="text-2xl font-bold text-destructive">
                        {batchResult.failureCount}
                      </div>
                      <div className="text-xs text-muted-foreground">Failed</div>
                    </div>
                  </div>

                  {operationStatus === 'completed' && (
                    <div className="flex items-center justify-between text-xs text-muted-foreground pt-2 border-t">
                      <div className="flex items-center gap-1">
                        <Clock className="h-3 w-3" aria-hidden="true" />
                        {batchResult.totalDurationMs}ms
                      </div>
                      <div>ID: {operationId?.slice(0, 8)}...</div>
                    </div>
                  )}
                </CardContent>
              </Card>
            )}

            {errorMessage && (
              <Alert variant="destructive" role="alert">
                <AlertCircle className="h-4 w-4" aria-hidden="true" />
                <AlertDescription>{errorMessage}</AlertDescription>
              </Alert>
            )}
          </div>

          {/* Right column - Files and Results */}
          <div className="lg:col-span-2 space-y-6">
            {/* File Uploader */}
            <Card>
              <CardHeader>
                <CardTitle>Files to Process</CardTitle>
              </CardHeader>
              <CardContent>
                <MultiFileUploader
                  onFilesSelect={handleFilesSelect}
                  selectedFiles={selectedFiles}
                  onRemoveFile={handleRemoveFile}
                  onClearAll={handleClearAll}
                  maxFiles={10}
                />
              </CardContent>
            </Card>

            {/* Results */}
            {batchResult?.results && batchResult.results.length > 0 && (
              <Card>
                <CardHeader>
                  <CardTitle className="flex items-center gap-2">
                    <FileCheck className="h-5 w-5" aria-hidden="true" />
                    Results
                  </CardTitle>
                </CardHeader>
                <CardContent className="space-y-3" role="region" aria-label="Batch operation results">
                  {batchResult.results.map((fileResult: BatchFileResult, index: number) => (
                    <div
                      key={index}
                      className={`p-4 rounded-lg border ${
                        fileResult.success
                          ? 'bg-green-500/5 border-green-500/20'
                          : 'bg-destructive/5 border-destructive/20'
                      }`}
                      role="article"
                      aria-label={`Result for ${fileResult.filename}: ${fileResult.success ? 'Success' : 'Failed'}`}
                    >
                      <div className="flex items-start justify-between mb-2">
                        <div className="flex items-center gap-2 flex-1 min-w-0">
                          {fileResult.success ? (
                            <CheckCircle2 className="h-4 w-4 text-green-500 flex-shrink-0" aria-hidden="true" />
                          ) : (
                            <AlertCircle className="h-4 w-4 text-destructive flex-shrink-0" aria-hidden="true" />
                          )}
                          <span className="font-medium text-sm truncate">
                            {fileResult.filename}
                          </span>
                        </div>
                        <Badge variant="outline" className="text-xs flex-shrink-0 ml-2">
                          {fileResult.durationMs}ms
                        </Badge>
                      </div>

                      {fileResult.error && (
                        <Alert variant="destructive" className="mt-2" role="alert">
                          <AlertDescription className="text-xs">
                            {fileResult.error}
                          </AlertDescription>
                        </Alert>
                      )}

                      {fileResult.result && 'valid' in fileResult.result && (
                        <div className="mt-2 text-xs text-muted-foreground">
                          {fileResult.result.valid ? (
                            <span className="text-green-600">Document is valid</span>
                          ) : (
                            <span className="text-destructive">
                              {fileResult.result.errors.length} error(s) found
                            </span>
                          )}
                        </div>
                      )}

                      {fileResult.result && 'success' in fileResult.result && (
                        <div className="mt-2 text-xs text-muted-foreground">
                          {fileResult.result.success ? (
                            <span className="text-green-600">Conversion successful</span>
                          ) : (
                            <span className="text-destructive">Conversion failed</span>
                          )}
                        </div>
                      )}
                    </div>
                  ))}
                </CardContent>
              </Card>
            )}

            {selectedFiles.length === 0 && !batchResult && (
              <Card className="h-[400px] flex items-center justify-center">
                <CardContent>
                  <div className="text-center text-muted-foreground">
                    <p className="text-lg mb-2">No files selected</p>
                    <p className="text-sm">Upload files to begin batch processing</p>
                  </div>
                </CardContent>
              </Card>
            )}
          </div>
        </div>
      </div>
    </div>
    </ProtectedRoute>
  );
}
