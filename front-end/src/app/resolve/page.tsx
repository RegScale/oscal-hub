'use client';

import { useState } from 'react';
import Link from 'next/link';
import { ArrowLeft, Download, GitMerge, CheckCircle2, AlertCircle } from 'lucide-react';
import { Card, CardHeader, CardTitle, CardContent } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Alert, AlertDescription } from '@/components/ui/alert';
import { Badge } from '@/components/ui/badge';
import { Progress } from '@/components/ui/progress';
import { FileUploader } from '@/components/file-uploader';
import { CodeEditor } from '@/components/code-editor';
import { apiClient } from '@/lib/api-client';
import { downloadFile } from '@/lib/download';
import ProtectedRoute from '@/components/ProtectedRoute';
import type { OscalFormat, ProfileResolutionResult } from '@/types/oscal';

export default function ResolvePage() {
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [profileContent, setProfileContent] = useState<string>('');
  const [resolvedCatalog, setResolvedCatalog] = useState<string>('');
  const [format, setFormat] = useState<OscalFormat>('json');
  const [isResolving, setIsResolving] = useState(false);
  const [resolutionResult, setResolutionResult] = useState<ProfileResolutionResult | null>(
    null
  );

  const handleFileSelect = (file: File, content: string) => {
    setSelectedFile(file);
    setProfileContent(content);
    setResolvedCatalog('');
    setResolutionResult(null);

    // Auto-detect format from file extension
    const extension = file.name.split('.').pop()?.toLowerCase();
    if (extension === 'xml') setFormat('xml');
    else if (extension === 'json') setFormat('json');
    else if (extension === 'yaml' || extension === 'yml') setFormat('yaml');
  };

  const handleClear = () => {
    setSelectedFile(null);
    setProfileContent('');
    setResolvedCatalog('');
    setResolutionResult(null);
  };

  const handleResolve = async () => {
    if (!profileContent) return;

    setIsResolving(true);
    setResolvedCatalog('');
    setResolutionResult(null);

    try {
      const result = await apiClient.resolveProfile({
        profileContent,
        format,
      });

      setResolutionResult(result);
      if (result.success && result.resolvedCatalog) {
        setResolvedCatalog(result.resolvedCatalog);
      }
    } catch (error) {
      console.error('Resolution error:', error);
      setResolutionResult({
        success: false,
        error: 'Failed to resolve profile. Please try again.',
      });
    } finally {
      setIsResolving(false);
    }
  };

  const handleDownload = () => {
    if (!resolvedCatalog || !selectedFile) return;

    const originalName = selectedFile.name.replace(/\.(xml|json|ya?ml)$/i, '');
    const extensions: Record<OscalFormat, string> = {
      xml: 'xml',
      json: 'json',
      yaml: 'yaml',
    };
    const filename = `${originalName}-resolved.${extensions[format]}`;

    downloadFile(resolvedCatalog, filename, format);
  };

  const canResolve = selectedFile && profileContent && !isResolving;
  const hasResolved = resolutionResult?.success && resolvedCatalog;

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
          <h1 className="text-4xl font-bold mb-2">Resolve OSCAL Profile</h1>
          <p className="text-muted-foreground">
            Resolve profiles into catalogs with complete control selection
          </p>
        </header>

        {/* Two-column layout */}
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
          {/* Left column - Upload and controls */}
          <div className="lg:col-span-1 space-y-6">
            <Card>
              <CardHeader>
                <CardTitle>Profile Document</CardTitle>
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

                <Button
                  onClick={handleResolve}
                  disabled={!canResolve}
                  className="w-full"
                  size="lg"
                  aria-label={isResolving ? 'Resolving profile, please wait' : 'Resolve OSCAL profile to catalog'}
                >
                  <GitMerge className="h-4 w-4 mr-2" aria-hidden="true" />
                  {isResolving ? 'Resolving...' : 'Resolve Profile'}
                </Button>

                {isResolving && (
                  <div className="space-y-2" role="status" aria-live="polite">
                    <Progress value={undefined} aria-label="Profile resolution progress" />
                    <p className="text-xs text-center text-muted-foreground">
                      Resolving profile and importing controls...
                    </p>
                  </div>
                )}

                {hasResolved && (
                  <Button
                    onClick={handleDownload}
                    variant="secondary"
                    className="w-full"
                    size="lg"
                    aria-label="Download resolved catalog file"
                  >
                    <Download className="h-4 w-4 mr-2" aria-hidden="true" />
                    Download Resolved Catalog
                  </Button>
                )}
              </CardContent>
            </Card>

            {/* Resolution Results Summary */}
            {resolutionResult && (
              <Card aria-label="Resolution results" role="region">
                <CardHeader>
                  <CardTitle className="flex items-center gap-2">
                    {resolutionResult.success ? (
                      <>
                        <CheckCircle2 className="h-5 w-5 text-green-500" aria-hidden="true" />
                        Resolution Complete
                      </>
                    ) : (
                      <>
                        <AlertCircle className="h-5 w-5 text-destructive" aria-hidden="true" />
                        Resolution Failed
                      </>
                    )}
                  </CardTitle>
                </CardHeader>
                <CardContent className="space-y-3">
                  {resolutionResult.success ? (
                    <>
                      <Alert className="border-green-500/50 bg-green-500/10" role="status">
                        <AlertDescription>
                          Successfully resolved profile to catalog
                        </AlertDescription>
                      </Alert>

                      {resolutionResult.controlCount !== undefined && (
                        <div className="text-center p-4 rounded-lg bg-primary/10">
                          <div className="text-3xl font-bold text-primary">
                            {resolutionResult.controlCount}
                          </div>
                          <div className="text-sm text-muted-foreground mt-1">
                            Controls Imported
                          </div>
                        </div>
                      )}
                    </>
                  ) : (
                    <Alert variant="destructive" role="alert">
                      <AlertDescription>
                        {resolutionResult.error || 'Failed to resolve profile'}
                      </AlertDescription>
                    </Alert>
                  )}
                </CardContent>
              </Card>
            )}

            {/* Info Card */}
            <Card>
              <CardHeader>
                <CardTitle className="text-base">About Profile Resolution</CardTitle>
              </CardHeader>
              <CardContent className="text-sm text-muted-foreground space-y-2">
                <p>
                  Profile resolution imports controls from referenced catalogs and applies
                  any modifications specified in the profile.
                </p>
                <p>
                  The resulting catalog contains the complete, tailored set of controls
                  ready for implementation.
                </p>
              </CardContent>
            </Card>
          </div>

          {/* Right column - Editors */}
          <div className="lg:col-span-2 space-y-6">
            {profileContent ? (
              <>
                {/* Profile Preview */}
                <Card>
                  <CardHeader>
                    <CardTitle>Profile Preview</CardTitle>
                  </CardHeader>
                  <CardContent className="p-0">
                    <CodeEditor
                      content={profileContent}
                      format={format}
                      readOnly={true}
                      height="400px"
                    />
                  </CardContent>
                </Card>

                {/* Resolved Catalog */}
                <Card>
                  <CardHeader>
                    <CardTitle>Resolved Catalog</CardTitle>
                  </CardHeader>
                  <CardContent className="p-0">
                    {resolvedCatalog ? (
                      <CodeEditor
                        content={resolvedCatalog}
                        format={format}
                        readOnly={true}
                        height="500px"
                      />
                    ) : (
                      <div className="h-[500px] flex items-center justify-center border-t">
                        <div className="text-center text-muted-foreground p-8">
                          <GitMerge className="h-12 w-12 mx-auto mb-4 opacity-50" aria-hidden="true" />
                          <p className="text-lg mb-2">
                            {isResolving
                              ? 'Resolving profile...'
                              : 'Click "Resolve Profile" to see results'}
                          </p>
                          <p className="text-sm">
                            {isResolving
                              ? 'Importing and tailoring controls from referenced catalogs'
                              : 'The resolved catalog will appear here'}
                          </p>
                        </div>
                      </div>
                    )}
                  </CardContent>
                </Card>
              </>
            ) : (
              <Card className="h-[700px] flex items-center justify-center">
                <CardContent>
                  <div className="text-center text-muted-foreground">
                    <GitMerge className="h-16 w-16 mx-auto mb-4 opacity-50" aria-hidden="true" />
                    <p className="text-lg mb-2">No profile selected</p>
                    <p className="text-sm">Upload an OSCAL profile to begin resolution</p>
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
