'use client';

import { useState, useRef } from 'react';
import Link from 'next/link';
import { ArrowLeft, Download, RefreshCw } from 'lucide-react';
import { Card, CardHeader, CardTitle, CardContent } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Alert, AlertDescription } from '@/components/ui/alert';
import { Progress } from '@/components/ui/progress';
import { FileUploader } from '@/components/file-uploader';
import { SavedFileSelector, SavedFileSelectorRef } from '@/components/saved-file-selector';
import { Tabs, TabsList, TabsTrigger, TabsContent } from '@/components/ui/tabs';
import { FormatSelector } from '@/components/format-selector';
import { ModelTypeSelector } from '@/components/model-type-selector';
import { CodeEditor } from '@/components/code-editor';
import { apiClient } from '@/lib/api-client';
import { downloadFile, generateConvertedFilename } from '@/lib/download';
import ProtectedRoute from '@/components/ProtectedRoute';
import { toast } from 'sonner';
import type { OscalModelType, OscalFormat, ConversionResult, SavedFile } from '@/types/oscal';

export default function ConvertPage() {
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [inputContent, setInputContent] = useState<string>('');
  const [outputContent, setOutputContent] = useState<string>('');
  const [modelType, setModelType] = useState<OscalModelType | ''>('');
  const [fromFormat, setFromFormat] = useState<OscalFormat>('json');
  const [toFormat, setToFormat] = useState<OscalFormat>('xml');
  const [isConverting, setIsConverting] = useState(false);
  const [conversionResult, setConversionResult] = useState<ConversionResult | null>(null);
  const savedFilesRef = useRef<SavedFileSelectorRef>(null);

  const handleFileSelect = (file: File, content: string) => {
    setSelectedFile(file);
    setInputContent(content);
    setOutputContent('');
    setConversionResult(null);

    // Auto-detect source format from file extension
    const extension = file.name.split('.').pop()?.toLowerCase();
    if (extension === 'xml') {
      setFromFormat('xml');
      // Set a different target format
      if (toFormat === 'xml') setToFormat('json');
    } else if (extension === 'json') {
      setFromFormat('json');
      if (toFormat === 'json') setToFormat('xml');
    } else if (extension === 'yaml' || extension === 'yml') {
      setFromFormat('yaml');
      if (toFormat === 'yaml') setToFormat('xml');
    }

    toast.success('File loaded successfully');
  };

  const handleSavedFileSelect = (file: SavedFile, content: string) => {
    // Create a virtual File object for consistency
    const blob = new Blob([content], { type: 'text/plain' });
    const virtualFile = new File([blob], file.fileName, { type: 'text/plain' });

    setSelectedFile(virtualFile);
    setInputContent(content);
    setOutputContent('');
    setConversionResult(null);
    setFromFormat(file.format);

    // Set a different target format
    if (file.format === 'xml') {
      if (toFormat === 'xml') setToFormat('json');
    } else if (file.format === 'json') {
      if (toFormat === 'json') setToFormat('xml');
    } else if (file.format === 'yaml') {
      if (toFormat === 'yaml') setToFormat('xml');
    }

    if (file.modelType) {
      setModelType(file.modelType);
    }
  };

  const handleClear = () => {
    setSelectedFile(null);
    setInputContent('');
    setOutputContent('');
    setConversionResult(null);
    setModelType('');
  };

  const handleConvert = async () => {
    if (!inputContent || !modelType) return;

    setIsConverting(true);
    setOutputContent('');
    setConversionResult(null);
    toast.info('Converting document...');

    try {
      const result = await apiClient.convert({
        content: inputContent,
        fromFormat,
        toFormat,
        modelType,
        fileName: selectedFile?.name,
      });

      setConversionResult(result);
      if (result.success && result.content) {
        setOutputContent(result.content);
        toast.success('Document converted successfully!');
      } else {
        toast.error('Conversion failed');
      }
      // Refresh saved files list after conversion
      savedFilesRef.current?.refresh();
    } catch (error) {
      console.error('Conversion error:', error);
      toast.error('Conversion failed');
      setConversionResult({
        success: false,
        error: 'Failed to convert document. Please try again.',
        fromFormat,
        toFormat,
      });
    } finally {
      setIsConverting(false);
    }
  };

  const handleDownload = () => {
    if (!outputContent || !selectedFile) return;

    const filename = generateConvertedFilename(selectedFile.name, toFormat);
    downloadFile(outputContent, filename, toFormat);
    toast.success('Converted file downloaded');
  };

  const handleSwapFormats = () => {
    // Swap the formats
    const temp = fromFormat;
    setFromFormat(toFormat);
    setToFormat(temp);

    // If we have output, move it to input and clear output
    if (outputContent) {
      setInputContent(outputContent);
      setOutputContent('');
      setConversionResult(null);
    }
  };

  const canConvert = selectedFile && modelType && inputContent && !isConverting;
  const hasOutput = conversionResult?.success && outputContent;

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
          <h1 className="text-4xl font-bold mb-2">Convert OSCAL Document</h1>
          <p className="text-muted-foreground">
            Change format between XML, JSON, and YAML with live preview
          </p>
        </header>

        {/* Two-column layout */}
        <div className="grid grid-cols-1 lg:grid-cols-4 gap-6">
          {/* Left column - Settings and controls */}
          <div className="lg:col-span-1 space-y-6">
            <Tabs defaultValue="upload" className="w-full">
              <TabsList className="grid w-full grid-cols-2">
                <TabsTrigger value="upload">Upload Document</TabsTrigger>
                <TabsTrigger value="saved">Saved Documents</TabsTrigger>
              </TabsList>

              <TabsContent value="upload" className="mt-4">
                <Card>
                  <CardHeader>
                    <CardTitle>Source Document</CardTitle>
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

                    <FormatSelector
                      fromFormat={fromFormat}
                      toFormat={toFormat}
                      onFromFormatChange={setFromFormat}
                      onToFormatChange={setToFormat}
                      disabled={!selectedFile}
                    />

                    <div className="flex gap-2">
                      <Button
                        onClick={handleConvert}
                        disabled={!canConvert}
                        className="flex-1"
                        size="lg"
                        aria-label={isConverting ? 'Converting document, please wait' : `Convert document from ${fromFormat.toUpperCase()} to ${toFormat.toUpperCase()}`}
                      >
                        {isConverting ? 'Converting...' : 'Convert'}
                      </Button>

                      <Button
                        onClick={handleSwapFormats}
                        disabled={!selectedFile}
                        variant="outline"
                        size="lg"
                        className="px-3"
                        title="Swap formats"
                        aria-label={`Swap formats between ${fromFormat.toUpperCase()} and ${toFormat.toUpperCase()}`}
                      >
                        <RefreshCw className="h-4 w-4" aria-hidden="true" />
                      </Button>
                    </div>

                    {isConverting && (
                      <div className="space-y-2" role="status" aria-live="polite">
                        <Progress value={undefined} aria-label="Conversion progress" />
                        <p className="text-xs text-center text-muted-foreground">
                          Converting {fromFormat.toUpperCase()} to {toFormat.toUpperCase()}...
                        </p>
                      </div>
                    )}

                    {hasOutput && (
                      <Button
                        onClick={handleDownload}
                        variant="secondary"
                        className="w-full"
                        size="lg"
                        aria-label={`Download converted document as ${toFormat.toUpperCase()} file`}
                      >
                        <Download className="h-4 w-4 mr-2" aria-hidden="true" />
                        Download {toFormat.toUpperCase()}
                      </Button>
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

            {/* Conversion Status */}
            {conversionResult && (
              <Card aria-label="Conversion status" role="region">
                <CardHeader>
                  <CardTitle>Conversion Status</CardTitle>
                </CardHeader>
                <CardContent>
                  {conversionResult.success ? (
                    <Alert className="border-green-500/50 bg-green-500/10" role="status">
                      <AlertDescription>
                        Successfully converted from {conversionResult.fromFormat.toUpperCase()}{' '}
                        to {conversionResult.toFormat.toUpperCase()}
                      </AlertDescription>
                    </Alert>
                  ) : (
                    <Alert variant="destructive" role="alert">
                      <AlertDescription>
                        {conversionResult.error || 'Conversion failed'}
                      </AlertDescription>
                    </Alert>
                  )}
                </CardContent>
              </Card>
            )}
          </div>

          {/* Right column - Side-by-side editors */}
          <div className="lg:col-span-3 space-y-6">
            {inputContent ? (
              <div className="grid grid-cols-1 xl:grid-cols-2 gap-6">
                {/* Input Editor */}
                <Card>
                  <CardHeader>
                    <CardTitle className="text-base">
                      Source ({fromFormat.toUpperCase()})
                    </CardTitle>
                  </CardHeader>
                  <CardContent className="p-0">
                    <CodeEditor
                      content={inputContent}
                      format={fromFormat}
                      readOnly={true}
                      height="600px"
                    />
                  </CardContent>
                </Card>

                {/* Output Editor */}
                <Card>
                  <CardHeader>
                    <CardTitle className="text-base">
                      {outputContent ? `Result (${toFormat.toUpperCase()})` : 'Result'}
                    </CardTitle>
                  </CardHeader>
                  <CardContent className="p-0">
                    {outputContent ? (
                      <CodeEditor
                        content={outputContent}
                        format={toFormat}
                        readOnly={true}
                        height="600px"
                      />
                    ) : (
                      <div className="h-[600px] flex items-center justify-center border-t">
                        <div className="text-center text-muted-foreground p-8">
                          <p className="text-lg mb-2">
                            {isConverting
                              ? 'Converting...'
                              : 'Click "Convert" to see results'}
                          </p>
                          <p className="text-sm">
                            {isConverting
                              ? 'Please wait while we process your document'
                              : `Will convert from ${fromFormat.toUpperCase()} to ${toFormat.toUpperCase()}`}
                          </p>
                        </div>
                      </div>
                    )}
                  </CardContent>
                </Card>
              </div>
            ) : (
              <Card className="h-[700px] flex items-center justify-center">
                <CardContent>
                  <div className="text-center text-muted-foreground">
                    <p className="text-lg mb-2">No document selected</p>
                    <p className="text-sm">Upload a file to begin conversion</p>
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
