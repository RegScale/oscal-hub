'use client';

import { useState, useRef } from 'react';
import Link from 'next/link';
import { ArrowLeft, BarChart3, AlertCircle } from 'lucide-react';
import { Card, CardHeader, CardTitle, CardContent } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Alert, AlertDescription } from '@/components/ui/alert';
import { Badge } from '@/components/ui/badge';
import { Tabs, TabsList, TabsTrigger, TabsContent } from '@/components/ui/tabs';
import { FileUploader } from '@/components/file-uploader';
import { SavedFileSelector, SavedFileSelectorRef } from '@/components/saved-file-selector';
import { CatalogVisualization } from '@/components/CatalogVisualization';
import { analyzeCatalog, type CatalogAnalysis } from '@/lib/oscal-parser';
import ProtectedRoute from '@/components/ProtectedRoute';
import { toast } from 'sonner';
import type { OscalFormat, SavedFile } from '@/types/oscal';

export default function VisualizePage() {
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [fileContent, setFileContent] = useState<string>('');
  const [format, setFormat] = useState<OscalFormat>('json');
  const [catalogAnalysis, setCatalogAnalysis] = useState<CatalogAnalysis | null>(null);
  const [isAnalyzing, setIsAnalyzing] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const savedFilesRef = useRef<SavedFileSelectorRef>(null);

  const handleFileSelect = (file: File, content: string) => {
    setSelectedFile(file);
    setFileContent(content);
    setCatalogAnalysis(null);
    setError(null);

    // Auto-detect format from file extension
    const extension = file.name.split('.').pop()?.toLowerCase();
    if (extension === 'xml') setFormat('xml');
    else if (extension === 'json') setFormat('json');
    else if (extension === 'yaml' || extension === 'yml') setFormat('yaml');

    toast.success('File loaded successfully');
  };

  const handleSavedFileSelect = (file: SavedFile, content: string) => {
    // Create a virtual File object for consistency
    const blob = new Blob([content], { type: 'text/plain' });
    const virtualFile = new File([blob], file.fileName, { type: 'text/plain' });

    setSelectedFile(virtualFile);
    setFileContent(content);
    setCatalogAnalysis(null);
    setError(null);
    setFormat(file.format);
  };

  const handleClear = () => {
    setSelectedFile(null);
    setFileContent('');
    setCatalogAnalysis(null);
    setError(null);
  };

  const handleVisualize = async () => {
    if (!fileContent) return;

    setIsAnalyzing(true);
    setError(null);
    toast.info('Analyzing document...');

    try {
      // For now, we only support catalog visualization
      // In the future, we'll detect the model type and route to appropriate visualizer
      const analysis = analyzeCatalog(fileContent, format);
      setCatalogAnalysis(analysis);
      toast.success('Document analyzed successfully!');
    } catch (err) {
      console.error('Analysis error:', err);
      const errorMessage = err instanceof Error ? err.message : 'Failed to analyze document';
      setError(errorMessage);
      toast.error(errorMessage);
    } finally {
      setIsAnalyzing(false);
    }
  };

  const canVisualize = selectedFile && !isAnalyzing;

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
            <h1 className="text-4xl font-bold mb-2 flex items-center gap-3">
              <BarChart3 className="h-10 w-10 text-primary" />
              Visualize OSCAL Document
            </h1>
            <p className="text-muted-foreground">
              Explore and understand your OSCAL documents through interactive visualizations
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

                      <div className="space-y-2">
                        <label className="text-sm font-medium">Detected Format</label>
                        <Badge variant="secondary" className="w-full justify-center py-2">
                          {format.toUpperCase()}
                        </Badge>
                      </div>

                      <Alert>
                        <AlertDescription className="text-sm">
                          Currently supports: Catalogs
                          <br />
                          Coming soon: Profiles, SSPs, Components, Assessment Results, POA&Ms
                        </AlertDescription>
                      </Alert>

                      <Button
                        onClick={handleVisualize}
                        disabled={!canVisualize}
                        className="w-full"
                        size="lg"
                        aria-label={isAnalyzing ? 'Analyzing document, please wait' : 'Start visualization of OSCAL document'}
                      >
                        {isAnalyzing ? 'Analyzing...' : 'Visualize Document'}
                      </Button>
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

              {/* Info Card */}
              <Card>
                <CardHeader>
                  <CardTitle className="text-base">About Visualizations</CardTitle>
                </CardHeader>
                <CardContent className="text-sm text-muted-foreground space-y-2">
                  <p>
                    Data visualizations help you quickly understand the structure and content of
                    your OSCAL documents.
                  </p>
                  <ul className="list-disc list-inside space-y-1 pl-2">
                    <li>View metadata and basic information</li>
                    <li>Analyze control distributions</li>
                    <li>Identify patterns and relationships</li>
                    <li>Compare across families and groups</li>
                  </ul>
                </CardContent>
              </Card>
            </div>

            {/* Right column - Visualizations */}
            <div className="lg:col-span-2">
              {error && (
                <Alert variant="destructive" className="mb-6">
                  <AlertCircle className="h-4 w-4" />
                  <AlertDescription>{error}</AlertDescription>
                </Alert>
              )}

              {catalogAnalysis ? (
                <CatalogVisualization analysis={catalogAnalysis} />
              ) : (
                <Card className="h-[500px] flex items-center justify-center">
                  <CardContent>
                    <div className="text-center text-muted-foreground">
                      <BarChart3 className="h-16 w-16 mx-auto mb-4 opacity-50" />
                      <p className="text-lg mb-2">No document visualized</p>
                      <p className="text-sm">Upload or select a catalog to begin visualization</p>
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
