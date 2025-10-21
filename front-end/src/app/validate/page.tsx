'use client';

import { useState, useRef } from 'react';
import Link from 'next/link';
import { ArrowLeft, CheckCircle2, AlertCircle, Info } from 'lucide-react';
import { Card, CardHeader, CardTitle, CardContent } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Alert, AlertDescription } from '@/components/ui/alert';
import { Badge } from '@/components/ui/badge';
import { Progress } from '@/components/ui/progress';
import { Tabs, TabsList, TabsTrigger, TabsContent } from '@/components/ui/tabs';
import { FileUploader } from '@/components/file-uploader';
import { SavedFileSelector, SavedFileSelectorRef } from '@/components/saved-file-selector';
import { ModelTypeSelector } from '@/components/model-type-selector';
import { CodeEditor } from '@/components/code-editor';
import { apiClient } from '@/lib/api-client';
import ProtectedRoute from '@/components/ProtectedRoute';
import { toast } from 'sonner';
import type { OscalModelType, OscalFormat, ValidationResult, SavedFile } from '@/types/oscal';

export default function ValidatePage() {
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [fileContent, setFileContent] = useState<string>('');
  const [modelType, setModelType] = useState<OscalModelType | ''>('');
  const [format, setFormat] = useState<OscalFormat>('json');
  const [validationResult, setValidationResult] = useState<ValidationResult | null>(null);
  const [isValidating, setIsValidating] = useState(false);
  const [highlightedLine, setHighlightedLine] = useState<number | undefined>(undefined);
  const savedFilesRef = useRef<SavedFileSelectorRef>(null);

  const handleFileSelect = (file: File, content: string) => {
    setSelectedFile(file);
    setValidationResult(null);
    setHighlightedLine(undefined);

    // Auto-detect format from file extension
    const extension = file.name.split('.').pop()?.toLowerCase();
    let detectedFormat: OscalFormat = 'json';
    if (extension === 'xml') detectedFormat = 'xml';
    else if (extension === 'json') detectedFormat = 'json';
    else if (extension === 'yaml' || extension === 'yml') detectedFormat = 'yaml';
    setFormat(detectedFormat);

    // Format JSON content properly if it's JSON format
    if (detectedFormat === 'json') {
      try {
        // Try to parse and reformat the JSON
        const parsed = JSON.parse(content);
        const formatted = JSON.stringify(parsed, null, 2);
        setFileContent(formatted);
      } catch (error) {
        // If parsing fails, use content as-is
        console.warn('Could not parse JSON for formatting:', error);
        setFileContent(content);
      }
    } else {
      setFileContent(content);
    }

    toast.success('File loaded successfully');
  };

  const handleSavedFileSelect = (file: SavedFile, content: string) => {
    // Create a virtual File object for consistency
    const blob = new Blob([content], { type: 'text/plain' });
    const virtualFile = new File([blob], file.fileName, { type: 'text/plain' });

    setSelectedFile(virtualFile);
    setValidationResult(null);
    setHighlightedLine(undefined);
    setFormat(file.format);

    if (file.modelType) {
      setModelType(file.modelType);
    }

    // Format JSON content properly if it's JSON format
    if (file.format === 'json') {
      try {
        // Try to parse and reformat the JSON
        const parsed = JSON.parse(content);
        const formatted = JSON.stringify(parsed, null, 2);
        setFileContent(formatted);
      } catch (error) {
        // If parsing fails, use content as-is
        console.warn('Could not parse JSON for formatting:', error);
        setFileContent(content);
      }
    } else {
      setFileContent(content);
    }
  };

  const handleClear = () => {
    setSelectedFile(null);
    setFileContent('');
    setValidationResult(null);
    setModelType('');
    setHighlightedLine(undefined);
  };

  const handleValidate = async () => {
    if (!fileContent || !modelType) return;

    setIsValidating(true);
    setValidationResult(null);
    setHighlightedLine(undefined);
    toast.info('Validating document...');

    try {
      const result = await apiClient.validate(
        fileContent,
        modelType,
        format,
        selectedFile?.name
      );
      setValidationResult(result);

      if (result.valid) {
        toast.success('Document is valid!');
      } else {
        toast.error('Validation failed with errors');
      }

      // Refresh saved files list after validation
      savedFilesRef.current?.refresh();
    } catch (error) {
      console.error('Validation error:', error);
      toast.error('Validation failed with errors');
      setValidationResult({
        valid: false,
        errors: [
          {
            message: 'Failed to validate document. Please try again.',
            severity: 'error',
          },
        ],
        warnings: [],
        timestamp: new Date().toISOString(),
      });
    } finally {
      setIsValidating(false);
    }
  };

  const handleErrorClick = (line?: number) => {
    if (line) {
      setHighlightedLine(line);
    }
  };

  const canValidate = selectedFile && modelType && !isValidating;

  return (
    <ProtectedRoute>
      <div className="min-h-screen bg-background">
      <div className="container mx-auto py-8 px-4" id="main-content">
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
          <h1 className="text-4xl font-bold mb-2">Validate OSCAL Document</h1>
          <p className="text-muted-foreground">
            Check if your OSCAL document is valid and complies with schema constraints
          </p>
        </header>

        {/* Two-column layout */}
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
          {/* Left column - Upload and settings */}
          <div className="lg:col-span-1 space-y-6">
            <Tabs defaultValue="upload" className="w-full">
              <TabsList className="grid w-full grid-cols-2">
                <TabsTrigger value="upload">Upload Document</TabsTrigger>
                <TabsTrigger value="saved">Saved Documents</TabsTrigger>
              </TabsList>

              <TabsContent value="upload" className="mt-4">
                <Card>
                  <CardHeader>
                    <CardTitle>Document Upload</CardTitle>
                  </CardHeader>
                  <CardContent className="space-y-4">
                    <FileUploader
                      onFileSelect={handleFileSelect}
                      selectedFile={selectedFile}
                      onClear={handleClear}
                    />

                    <ModelTypeSelector
                      value={modelType}
                      onChange={setModelType}
                      disabled={!selectedFile}
                    />

                    <div className="space-y-2">
                      <label className="text-sm font-medium">Detected Format</label>
                      <Badge variant="secondary" className="w-full justify-center py-2">
                        {format.toUpperCase()}
                      </Badge>
                    </div>

                    <Button
                      onClick={handleValidate}
                      disabled={!canValidate}
                      className="w-full"
                      size="lg"
                      aria-label={isValidating ? 'Validating document, please wait' : 'Start validation of OSCAL document'}
                    >
                      {isValidating ? 'Validating...' : 'Validate Document'}
                    </Button>

                    {isValidating && (
                      <div className="space-y-2" role="status" aria-live="polite">
                        <Progress value={undefined} aria-label="Validation progress" />
                        <p className="text-xs text-center text-muted-foreground">
                          Validating document...
                        </p>
                      </div>
                    )}
                  </CardContent>
                </Card>
              </TabsContent>

              <TabsContent value="saved" className="mt-4">
                <SavedFileSelector
                  ref={savedFilesRef}
                  onFileSelect={handleSavedFileSelect}
                  onUploadNew={handleClear}
                  showUploadButton={false}
                />
              </TabsContent>
            </Tabs>

            {/* Validation Results Summary */}
            {validationResult && (
              <Card aria-label="Validation results summary" role="region">
                <CardHeader>
                  <CardTitle className="flex items-center gap-2">
                    {validationResult.valid ? (
                      <>
                        <CheckCircle2 className="h-5 w-5 text-green-500" aria-hidden="true" />
                        Valid Document
                      </>
                    ) : (
                      <>
                        <AlertCircle className="h-5 w-5 text-destructive" aria-hidden="true" />
                        Validation Failed
                      </>
                    )}
                  </CardTitle>
                </CardHeader>
                <CardContent className="space-y-3">
                  <div className="grid grid-cols-2 gap-4">
                    <div className="text-center p-3 rounded-lg bg-destructive/10">
                      <div className="text-2xl font-bold text-destructive">
                        {validationResult.errors.length}
                      </div>
                      <div className="text-xs text-muted-foreground">Errors</div>
                    </div>
                    <div className="text-center p-3 rounded-lg bg-yellow-500/10">
                      <div className="text-2xl font-bold text-yellow-500">
                        {validationResult.warnings.length}
                      </div>
                      <div className="text-xs text-muted-foreground">Warnings</div>
                    </div>
                  </div>

                  <div className="text-xs text-muted-foreground">
                    Validated at {new Date(validationResult.timestamp).toLocaleTimeString()}
                  </div>
                </CardContent>
              </Card>
            )}
          </div>

          {/* Right column - Code editor and detailed results */}
          <div className="lg:col-span-2 space-y-6">
            {fileContent ? (
              <>
                <Card>
                  <CardHeader>
                    <CardTitle>Document Preview</CardTitle>
                  </CardHeader>
                  <CardContent className="p-0">
                    <CodeEditor
                      content={fileContent}
                      format={format}
                      readOnly={true}
                      highlightLine={highlightedLine}
                      height="500px"
                    />
                  </CardContent>
                </Card>

                {/* Detailed validation results */}
                {validationResult && (
                  <Card>
                    <CardHeader>
                      <CardTitle>Validation Details</CardTitle>
                    </CardHeader>
                    <CardContent className="space-y-3">
                      {validationResult.errors.length > 0 && (
                        <section className="space-y-2" aria-label="Validation errors">
                          <h3 className="text-sm font-semibold text-destructive flex items-center gap-2">
                            <AlertCircle className="h-4 w-4" aria-hidden="true" />
                            Errors ({validationResult.errors.length})
                          </h3>
                          {validationResult.errors.map((error, index) => (
                            <Alert
                              key={index}
                              variant="destructive"
                              className="cursor-pointer hover:bg-destructive/20 transition-colors"
                              onClick={() => handleErrorClick(error.line)}
                              role="button"
                              tabIndex={0}
                              aria-label={`Error at line ${error.line || 'unknown'}: ${error.message}`}
                            >
                              <AlertDescription className="text-sm">
                                {error.line && (
                                  <span className="font-mono font-semibold">
                                    Line {error.line}
                                    {error.column && `:${error.column}`}:{' '}
                                  </span>
                                )}
                                {error.message}
                                {error.path && (
                                  <div className="text-xs mt-1 opacity-70">{error.path}</div>
                                )}
                              </AlertDescription>
                            </Alert>
                          ))}
                        </section>
                      )}

                      {validationResult.warnings.length > 0 && (
                        <section className="space-y-2" aria-label="Validation warnings">
                          <h3 className="text-sm font-semibold text-yellow-500 flex items-center gap-2">
                            <Info className="h-4 w-4" aria-hidden="true" />
                            Warnings ({validationResult.warnings.length})
                          </h3>
                          {validationResult.warnings.map((warning, index) => (
                            <Alert
                              key={index}
                              className="border-yellow-500/50 bg-yellow-500/10 cursor-pointer hover:bg-yellow-500/20 transition-colors"
                              onClick={() => handleErrorClick(warning.line)}
                              role="button"
                              tabIndex={0}
                              aria-label={`Warning at line ${warning.line || 'unknown'}: ${warning.message}`}
                            >
                              <AlertDescription className="text-sm">
                                {warning.line && (
                                  <span className="font-mono font-semibold">
                                    Line {warning.line}
                                    {warning.column && `:${warning.column}`}:{' '}
                                  </span>
                                )}
                                {warning.message}
                                {warning.path && (
                                  <div className="text-xs mt-1 opacity-70">{warning.path}</div>
                                )}
                              </AlertDescription>
                            </Alert>
                          ))}
                        </section>
                      )}

                      {validationResult.valid &&
                        validationResult.errors.length === 0 &&
                        validationResult.warnings.length === 0 && (
                          <Alert className="border-green-500/50 bg-green-500/10">
                            <CheckCircle2 className="h-4 w-4 text-green-500" aria-hidden="true" />
                            <AlertDescription>
                              Document is valid and contains no errors or warnings. It fully
                              complies with the OSCAL {modelType} schema.
                            </AlertDescription>
                          </Alert>
                        )}
                    </CardContent>
                  </Card>
                )}
              </>
            ) : (
              <Card className="h-[500px] flex items-center justify-center">
                <CardContent>
                  <div className="text-center text-muted-foreground">
                    <p className="text-lg mb-2">No document selected</p>
                    <p className="text-sm">Upload a file to begin validation</p>
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
