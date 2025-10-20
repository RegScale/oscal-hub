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
import { LibrarySelector, LibrarySelectorRef } from '@/components/library-selector';
import { CatalogVisualization } from '@/components/CatalogVisualization';
import { SspVisualization } from '@/components/SspVisualization';
import { ProfileVisualization } from '@/components/ProfileVisualization';
import { SarVisualization } from '@/components/SarVisualization';
import { analyzeCatalog, type CatalogAnalysis } from '@/lib/oscal-parser';
import ProtectedRoute from '@/components/ProtectedRoute';
import { toast } from 'sonner';
import type { OscalFormat, SavedFile, LibraryItem, SspVisualizationData, ProfileVisualizationData, SarVisualizationData } from '@/types/oscal';
import { apiClient } from '@/lib/api-client';

export default function VisualizePage() {
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [fileContent, setFileContent] = useState<string>('');
  const [format, setFormat] = useState<OscalFormat>('json');
  const [catalogAnalysis, setCatalogAnalysis] = useState<CatalogAnalysis | null>(null);
  const [sspVisualization, setSspVisualization] = useState<SspVisualizationData | null>(null);
  const [profileVisualization, setProfileVisualization] = useState<ProfileVisualizationData | null>(null);
  const [sarVisualization, setSarVisualization] = useState<SarVisualizationData | null>(null);
  const [isAnalyzing, setIsAnalyzing] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const savedFilesRef = useRef<SavedFileSelectorRef>(null);
  const libraryRef = useRef<LibrarySelectorRef>(null);

  const handleFileSelect = async (file: File, content: string) => {
    setSelectedFile(file);
    setFileContent(content);
    setCatalogAnalysis(null);
    setSspVisualization(null);
    setProfileVisualization(null);
    setSarVisualization(null);
    setError(null);

    // Auto-detect format from file extension
    const extension = file.name.split('.').pop()?.toLowerCase();
    let detectedFormat: OscalFormat = 'json';
    if (extension === 'xml') detectedFormat = 'xml';
    else if (extension === 'json') detectedFormat = 'json';
    else if (extension === 'yaml' || extension === 'yml') detectedFormat = 'yaml';

    setFormat(detectedFormat);

    // Save the file to backend storage
    try {
      const savedFile = await apiClient.saveFile(content, file.name, detectedFormat);
      if (savedFile) {
        // Refresh the saved files list
        savedFilesRef.current?.refresh();
        toast.success('File loaded and saved successfully');
      } else {
        toast.success('File loaded successfully');
      }
    } catch (err) {
      console.error('Failed to save file:', err);
      toast.success('File loaded successfully');
    }
  };

  const handleSavedFileSelect = async (file: SavedFile, content: string) => {
    // Create a virtual File object for consistency
    const blob = new Blob([content], { type: 'text/plain' });
    const virtualFile = new File([blob], file.fileName, { type: 'text/plain' });

    // Convert format to lowercase for consistency
    const normalizedFormat = file.format.toLowerCase() as OscalFormat;

    setSelectedFile(virtualFile);
    setFileContent(content);
    setCatalogAnalysis(null);
    setSspVisualization(null);
    setProfileVisualization(null);
    setSarVisualization(null);
    setError(null);
    setFormat(normalizedFormat);

    // Auto-trigger visualization
    setIsAnalyzing(true);
    toast.info('Analyzing document...');

    try {
      // Detect document type and visualize appropriately
      const docType = detectDocumentType(content, normalizedFormat);

      if (docType === 'catalog') {
        const analysis = analyzeCatalog(content, normalizedFormat);
        setCatalogAnalysis(analysis);
        setSspVisualization(null);
        setProfileVisualization(null);
        setSarVisualization(null);
        toast.success('Catalog analyzed successfully!');
      } else if (docType === 'ssp') {
        const sspData = await apiClient.visualizeSSP(content, normalizedFormat, file.fileName);
        setSspVisualization(sspData);
        setCatalogAnalysis(null);
        setProfileVisualization(null);
        setSarVisualization(null);
        toast.success('SSP analyzed successfully!');
      } else if (docType === 'profile') {
        const profileData = await apiClient.visualizeProfile(content, normalizedFormat, file.fileName);
        setProfileVisualization(profileData);
        setCatalogAnalysis(null);
        setSspVisualization(null);
        setSarVisualization(null);
        toast.success('Profile analyzed successfully!');
      } else if (docType === 'sar') {
        const sarData = await apiClient.visualizeSAR(content, normalizedFormat, file.fileName);
        setSarVisualization(sarData);
        setCatalogAnalysis(null);
        setSspVisualization(null);
        setProfileVisualization(null);
        toast.success('SAR analyzed successfully!');
      } else {
        throw new Error('Unsupported document type. Please upload a Catalog, Profile, System Security Plan (SSP), or Security Assessment Results (SAR).');
      }
    } catch (err) {
      console.error('Analysis error:', err);
      const errorMessage = err instanceof Error ? err.message : 'Failed to analyze document';
      setError(errorMessage);
      toast.error(errorMessage);
    } finally {
      setIsAnalyzing(false);
    }
  };

  const handleLibraryItemSelect = async (item: LibraryItem, content: string) => {
    // Create a virtual File object for consistency
    const fileName = item.currentVersion?.fileName || `${item.title}.json`;
    const blob = new Blob([content], { type: 'text/plain' });
    const virtualFile = new File([blob], fileName, { type: 'text/plain' });

    // Convert format to lowercase for consistency
    const normalizedFormat = (item.currentVersion?.format || 'json').toLowerCase() as OscalFormat;

    setSelectedFile(virtualFile);
    setFileContent(content);
    setCatalogAnalysis(null);
    setSspVisualization(null);
    setProfileVisualization(null);
    setSarVisualization(null);
    setError(null);
    setFormat(normalizedFormat);

    // Auto-trigger visualization
    setIsAnalyzing(true);
    toast.info('Analyzing document...');

    try {
      // Detect document type and visualize appropriately
      const docType = detectDocumentType(content, normalizedFormat);

      if (docType === 'catalog') {
        const analysis = analyzeCatalog(content, normalizedFormat);
        setCatalogAnalysis(analysis);
        setSspVisualization(null);
        setProfileVisualization(null);
        setSarVisualization(null);
        toast.success('Catalog analyzed successfully!');
      } else if (docType === 'ssp') {
        const sspData = await apiClient.visualizeSSP(content, normalizedFormat, fileName);
        setSspVisualization(sspData);
        setCatalogAnalysis(null);
        setProfileVisualization(null);
        setSarVisualization(null);
        toast.success('SSP analyzed successfully!');
      } else if (docType === 'profile') {
        const profileData = await apiClient.visualizeProfile(content, normalizedFormat, fileName);
        setProfileVisualization(profileData);
        setCatalogAnalysis(null);
        setSspVisualization(null);
        setSarVisualization(null);
        toast.success('Profile analyzed successfully!');
      } else if (docType === 'sar') {
        const sarData = await apiClient.visualizeSAR(content, normalizedFormat, fileName);
        setSarVisualization(sarData);
        setCatalogAnalysis(null);
        setSspVisualization(null);
        setProfileVisualization(null);
        toast.success('SAR analyzed successfully!');
      } else {
        throw new Error('Unsupported document type. Please upload a Catalog, Profile, System Security Plan (SSP), or Security Assessment Results (SAR).');
      }
    } catch (err) {
      console.error('Analysis error:', err);
      const errorMessage = err instanceof Error ? err.message : 'Failed to analyze document';
      setError(errorMessage);
      toast.error(errorMessage);
    } finally {
      setIsAnalyzing(false);
    }
  };

  const handleClear = () => {
    setSelectedFile(null);
    setFileContent('');
    setCatalogAnalysis(null);
    setSspVisualization(null);
    setProfileVisualization(null);
    setSarVisualization(null);
    setError(null);
  };

  const detectDocumentType = (content: string, docFormat: OscalFormat): 'catalog' | 'ssp' | 'profile' | 'sar' | 'unknown' => {
    try {
      // Normalize content for searching (trim whitespace)
      const normalizedContent = content.trim().toLowerCase();

      console.log(`Detecting document type - Format: ${docFormat}, Content length: ${content.length}`);
      console.log(`First 200 chars: ${normalizedContent.substring(0, 200)}`);

      if (docFormat === 'json') {
        // JSON format: look for the property name with quotes
        // Check multiple patterns to be safe
        if (normalizedContent.includes('"assessment-results"') ||
            normalizedContent.includes("'assessment-results'") ||
            normalizedContent.includes('assessment-results')) {
          console.log('Document type detected: SAR (JSON)');
          return 'sar';
        } else if (normalizedContent.includes('"system-security-plan"') ||
            normalizedContent.includes("'system-security-plan'") ||
            normalizedContent.includes('system-security-plan')) {
          console.log('Document type detected: SSP (JSON)');
          return 'ssp';
        } else if (normalizedContent.includes('"profile"') ||
                   normalizedContent.includes("'profile'") ||
                   (normalizedContent.includes('profile') && normalizedContent.includes('"imports"'))) {
          console.log('Document type detected: Profile (JSON)');
          return 'profile';
        } else if (normalizedContent.includes('"catalog"') ||
                   normalizedContent.includes("'catalog'") ||
                   (normalizedContent.includes('catalog') && normalizedContent.includes('{'))) {
          console.log('Document type detected: Catalog (JSON)');
          return 'catalog';
        }
      } else if (docFormat === 'yaml') {
        // YAML format: look for the key with colon (no quotes typically)
        if (normalizedContent.includes('assessment-results:') ||
            normalizedContent.includes('assessment_results:')) {
          console.log('Document type detected: SAR (YAML)');
          return 'sar';
        } else if (normalizedContent.includes('system-security-plan:') ||
            normalizedContent.includes('system_security_plan:')) {
          console.log('Document type detected: SSP (YAML)');
          return 'ssp';
        } else if (normalizedContent.includes('profile:') && normalizedContent.includes('imports:')) {
          console.log('Document type detected: Profile (YAML)');
          return 'profile';
        } else if (normalizedContent.includes('catalog:')) {
          console.log('Document type detected: Catalog (YAML)');
          return 'catalog';
        }
      } else if (docFormat === 'xml') {
        // XML format: look for the element tag
        if (normalizedContent.includes('<assessment-results') ||
            normalizedContent.includes('<assessment_results')) {
          console.log('Document type detected: SAR (XML)');
          return 'sar';
        } else if (normalizedContent.includes('<system-security-plan') ||
            normalizedContent.includes('<system_security_plan')) {
          console.log('Document type detected: SSP (XML)');
          return 'ssp';
        } else if (normalizedContent.includes('<profile')) {
          console.log('Document type detected: Profile (XML)');
          return 'profile';
        } else if (normalizedContent.includes('<catalog')) {
          console.log('Document type detected: Catalog (XML)');
          return 'catalog';
        }
      }

      console.log(`Document type detected: unknown (no match found)`);
      return 'unknown';
    } catch (err) {
      console.error('Error detecting document type:', err);
      return 'unknown';
    }
  };

  const handleVisualize = async () => {
    if (!fileContent) return;

    setIsAnalyzing(true);
    setError(null);
    toast.info('Analyzing document...');

    try {
      const docType = detectDocumentType(fileContent, format);

      if (docType === 'catalog') {
        const analysis = analyzeCatalog(fileContent, format);
        setCatalogAnalysis(analysis);
        setSspVisualization(null);
        setProfileVisualization(null);
        setSarVisualization(null);
        toast.success('Catalog analyzed successfully!');
      } else if (docType === 'ssp') {
        const sspData = await apiClient.visualizeSSP(fileContent, format, selectedFile?.name);
        setSspVisualization(sspData);
        setCatalogAnalysis(null);
        setProfileVisualization(null);
        setSarVisualization(null);
        toast.success('SSP analyzed successfully!');
      } else if (docType === 'profile') {
        const profileData = await apiClient.visualizeProfile(fileContent, format, selectedFile?.name);
        setProfileVisualization(profileData);
        setCatalogAnalysis(null);
        setSspVisualization(null);
        setSarVisualization(null);
        toast.success('Profile analyzed successfully!');
      } else if (docType === 'sar') {
        const sarData = await apiClient.visualizeSAR(fileContent, format, selectedFile?.name);
        setSarVisualization(sarData);
        setCatalogAnalysis(null);
        setSspVisualization(null);
        setProfileVisualization(null);
        toast.success('SAR analyzed successfully!');
      } else {
        throw new Error('Unsupported document type. Please upload a Catalog, Profile, System Security Plan (SSP), or Security Assessment Results (SAR).');
      }
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
                <TabsList className="grid w-full grid-cols-3">
                  <TabsTrigger value="upload">Upload</TabsTrigger>
                  <TabsTrigger value="saved">Saved</TabsTrigger>
                  <TabsTrigger value="library">Library</TabsTrigger>
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
                          Currently supports: Catalogs, System Security Plans (SSPs), Profiles, Security Assessment Results (SAR)
                          <br />
                          Coming soon: Components, POA&Ms
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

                <TabsContent value="library" className="mt-4">
                  <LibrarySelector
                    ref={libraryRef}
                    onItemSelect={handleLibraryItemSelect}
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
              ) : sspVisualization ? (
                <SspVisualization data={sspVisualization} />
              ) : profileVisualization ? (
                <ProfileVisualization data={profileVisualization} />
              ) : sarVisualization ? (
                <SarVisualization data={sarVisualization} />
              ) : (
                <Card className="h-[500px] flex items-center justify-center">
                  <CardContent>
                    <div className="text-center text-muted-foreground">
                      <BarChart3 className="h-16 w-16 mx-auto mb-4 opacity-50" />
                      <p className="text-lg mb-2">No document visualized</p>
                      <p className="text-sm">Upload or select a document to begin visualization</p>
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
