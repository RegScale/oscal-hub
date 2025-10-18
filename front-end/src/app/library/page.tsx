'use client';

import { useState, useEffect } from 'react';
import { useRouter } from 'next/navigation';
import { Card, CardHeader, CardTitle, CardDescription, CardContent } from '@/components/ui/card';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Badge } from '@/components/ui/badge';
import { Alert, AlertDescription } from '@/components/ui/alert';
import {
  Library,
  Upload,
  Search,
  BarChart3,
  Download,
  Eye,
  Tag,
  FileText,
  Calendar,
  User,
  TrendingUp,
  Filter,
  ArrowLeft
} from 'lucide-react';
import { apiClient } from '@/lib/api-client';
import type { LibraryItem, LibraryAnalytics, OscalModelType } from '@/types/oscal';
import { useAuth } from '@/contexts/AuthContext';
import { Footer } from '@/components/Footer';
import { toast } from 'sonner';

export default function LibraryPage() {
  const router = useRouter();
  const { isAuthenticated, isLoading } = useAuth();
  const [items, setItems] = useState<LibraryItem[]>([]);
  const [analytics, setAnalytics] = useState<LibraryAnalytics | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedType, setSelectedType] = useState<string>('');
  const [selectedTag, setSelectedTag] = useState<string>('');
  const [activeTab, setActiveTab] = useState('browse');

  // Upload form state
  const [uploadTitle, setUploadTitle] = useState('');
  const [uploadDescription, setUploadDescription] = useState('');
  const [uploadType, setUploadType] = useState<OscalModelType>('catalog');
  const [uploadFile, setUploadFile] = useState<File | null>(null);
  const [uploadTags, setUploadTags] = useState('');
  const [uploading, setUploading] = useState(false);
  const [uploadSuccess, setUploadSuccess] = useState(false);

  const oscalTypes: OscalModelType[] = [
    'catalog',
    'profile',
    'component-definition',
    'system-security-plan',
    'assessment-plan',
    'assessment-results',
    'plan-of-action-and-milestones'
  ];

  useEffect(() => {
    if (isAuthenticated) {
      loadLibrary();
      loadAnalytics();
    }
  }, [isAuthenticated]);

  const loadLibrary = async () => {
    try {
      setLoading(true);
      const data = await apiClient.getAllLibraryItems();
      setItems(data);
      setError(null);
    } catch (err) {
      setError('Failed to load library items');
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  const loadAnalytics = async () => {
    try {
      const data = await apiClient.getLibraryAnalytics();
      setAnalytics(data);
    } catch (err) {
      console.error('Failed to load analytics:', err);
    }
  };

  const handleSearch = async () => {
    try {
      setLoading(true);
      const results = await apiClient.searchLibrary({
        q: searchQuery || undefined,
        oscalType: selectedType || undefined,
        tag: selectedTag || undefined,
      });
      setItems(results);
      setError(null);
    } catch (err) {
      setError('Search failed');
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  const handleUpload = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!uploadFile) {
      setError('Please select a file');
      toast.error('Please select a file');
      return;
    }

    try {
      setUploading(true);
      setError(null);

      const content = await uploadFile.text();
      const format = uploadFile.name.endsWith('.xml') ? 'XML' :
                     uploadFile.name.endsWith('.yaml') || uploadFile.name.endsWith('.yml') ? 'YAML' : 'JSON';

      const tags = uploadTags.split(',').map(t => t.trim()).filter(t => t);

      await apiClient.createLibraryItem({
        title: uploadTitle,
        description: uploadDescription,
        oscalType: uploadType,
        fileName: uploadFile.name,
        format,
        fileContent: content,
        tags,
      });

      setUploadSuccess(true);
      toast.success('File uploaded to library');
      // Reset form
      setUploadTitle('');
      setUploadDescription('');
      setUploadType('catalog');
      setUploadFile(null);
      setUploadTags('');

      // Reload library
      loadLibrary();
      loadAnalytics();

      // Switch to browse tab to show the uploaded item
      setActiveTab('browse');

      setTimeout(() => setUploadSuccess(false), 5000);
    } catch (err) {
      setError('Upload failed: ' + (err as Error).message);
      toast.error('Upload failed: ' + (err as Error).message);
      console.error(err);
    } finally {
      setUploading(false);
    }
  };

  const handleDownload = async (item: LibraryItem) => {
    try {
      const content = await apiClient.getLibraryItemContent(item.itemId);
      const blob = new Blob([content], { type: 'text/plain' });
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = item.currentVersion?.fileName || 'download.json';
      document.body.appendChild(a);
      a.click();
      document.body.removeChild(a);
      URL.revokeObjectURL(url);
      toast.success('File downloaded');
    } catch (err) {
      setError('Download failed');
      toast.error('Download failed');
      console.error(err);
    }
  };

  if (isLoading) {
    return (
      <div className="min-h-screen bg-background flex items-center justify-center">
        <div className="text-center">
          <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-primary mx-auto mb-4"></div>
          <p className="text-muted-foreground">Loading...</p>
        </div>
      </div>
    );
  }

  if (!isAuthenticated) {
    return (
      <div className="min-h-screen bg-background">
        <div className="container mx-auto py-12 px-4">
          <Alert>
            <AlertDescription>
              Please log in to access the library.
            </AlertDescription>
          </Alert>
        </div>
        <Footer />
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-background">
      <div className="container mx-auto py-12 px-4">
        <div className="flex items-center justify-between mb-8">
          <div className="flex items-center">
            <Library className="h-10 w-10 text-primary mr-4" />
            <div>
              <h1 className="text-4xl font-bold">OSCAL Library</h1>
              <p className="text-muted-foreground mt-2">
                Browse, share, and download example OSCAL documents
              </p>
            </div>
          </div>
          <Button
            variant="outline"
            onClick={() => router.push('/')}
            className="flex items-center gap-2"
          >
            <ArrowLeft className="h-4 w-4" />
            Back to Dashboard
          </Button>
        </div>

        {error && (
          <Alert variant="destructive" className="mb-6">
            <AlertDescription>{error}</AlertDescription>
          </Alert>
        )}

        {uploadSuccess && (
          <Alert className="mb-6 bg-green-50 border-green-200">
            <AlertDescription className="text-green-800">
              Successfully uploaded to library!
            </AlertDescription>
          </Alert>
        )}

        <Tabs value={activeTab} onValueChange={setActiveTab} className="space-y-6">
          <TabsList className="grid w-full grid-cols-4">
            <TabsTrigger value="browse">
              <Library className="h-4 w-4 mr-2" />
              Browse
            </TabsTrigger>
            <TabsTrigger value="search">
              <Search className="h-4 w-4 mr-2" />
              Search
            </TabsTrigger>
            <TabsTrigger value="upload">
              <Upload className="h-4 w-4 mr-2" />
              Upload
            </TabsTrigger>
            <TabsTrigger value="analytics">
              <BarChart3 className="h-4 w-4 mr-2" />
              Analytics
            </TabsTrigger>
          </TabsList>

          {/* Browse Tab */}
          <TabsContent value="browse" className="space-y-6">
            {loading ? (
              <div className="text-center py-12">
                <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-primary mx-auto mb-4"></div>
                <p className="text-muted-foreground">Loading library items...</p>
              </div>
            ) : items.length === 0 ? (
              <Card>
                <CardContent className="py-12 text-center">
                  <Library className="h-16 w-16 text-muted-foreground mx-auto mb-4" />
                  <p className="text-lg text-muted-foreground">
                    No items in the library yet. Be the first to upload!
                  </p>
                </CardContent>
              </Card>
            ) : (
              <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
                {items.map((item) => (
                  <Card
                    key={item.itemId}
                    className="hover:shadow-lg transition-shadow cursor-pointer"
                    onClick={() => router.push(`/library/${item.itemId}`)}
                  >
                    <CardHeader>
                      <div className="flex items-start justify-between">
                        <FileText className="h-8 w-8 text-primary" />
                        <Badge variant="outline">{item.oscalType}</Badge>
                      </div>
                      <CardTitle className="mt-4">{item.title}</CardTitle>
                      <CardDescription className="line-clamp-2">
                        {item.description || 'No description provided'}
                      </CardDescription>
                    </CardHeader>
                    <CardContent className="space-y-4">
                      <div className="flex flex-wrap gap-2">
                        {item.tags.map((tag) => (
                          <Badge key={tag} variant="secondary" className="text-xs">
                            <Tag className="h-3 w-3 mr-1" />
                            {tag}
                          </Badge>
                        ))}
                      </div>
                      <div className="space-y-2 text-sm text-muted-foreground">
                        <div className="flex items-center">
                          <User className="h-4 w-4 mr-2" />
                          {item.createdBy}
                        </div>
                        <div className="flex items-center">
                          <Calendar className="h-4 w-4 mr-2" />
                          {new Date(item.updatedAt).toLocaleDateString()}
                        </div>
                        <div className="flex items-center justify-between">
                          <div className="flex items-center">
                            <Download className="h-4 w-4 mr-2" />
                            {item.downloadCount}
                          </div>
                          <div className="flex items-center">
                            <Eye className="h-4 w-4 mr-2" />
                            {item.viewCount}
                          </div>
                        </div>
                      </div>
                      <Button
                        onClick={(e) => {
                          e.stopPropagation();
                          handleDownload(item);
                        }}
                        className="w-full"
                      >
                        <Download className="h-4 w-4 mr-2" />
                        Download
                      </Button>
                    </CardContent>
                  </Card>
                ))}
              </div>
            )}
          </TabsContent>

          {/* Search Tab */}
          <TabsContent value="search" className="space-y-6">
            <Card>
              <CardHeader>
                <CardTitle>Search Library</CardTitle>
                <CardDescription>
                  Find OSCAL documents by keyword, type, or tag
                </CardDescription>
              </CardHeader>
              <CardContent className="space-y-4">
                <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                  <div className="space-y-2">
                    <Label htmlFor="search-query">Keyword</Label>
                    <Input
                      id="search-query"
                      placeholder="Search title or description..."
                      value={searchQuery}
                      onChange={(e) => setSearchQuery(e.target.value)}
                    />
                  </div>
                  <div className="space-y-2">
                    <Label htmlFor="search-type">OSCAL Type</Label>
                    <select
                      id="search-type"
                      className="w-full rounded-md border border-input bg-background px-3 py-2"
                      value={selectedType}
                      onChange={(e) => setSelectedType(e.target.value)}
                    >
                      <option value="">All Types</option>
                      {oscalTypes.map((type) => (
                        <option key={type} value={type}>
                          {type}
                        </option>
                      ))}
                    </select>
                  </div>
                  <div className="space-y-2">
                    <Label htmlFor="search-tag">Tag</Label>
                    <Input
                      id="search-tag"
                      placeholder="Filter by tag..."
                      value={selectedTag}
                      onChange={(e) => setSelectedTag(e.target.value)}
                    />
                  </div>
                </div>
                <Button onClick={handleSearch} className="w-full">
                  <Search className="h-4 w-4 mr-2" />
                  Search
                </Button>
              </CardContent>
            </Card>

            {/* Search Results */}
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
              {items.map((item) => (
                <Card
                  key={item.itemId}
                  className="hover:shadow-lg transition-shadow cursor-pointer"
                  onClick={() => router.push(`/library/${item.itemId}`)}
                >
                  <CardHeader>
                    <div className="flex items-start justify-between">
                      <FileText className="h-8 w-8 text-primary" />
                      <Badge variant="outline">{item.oscalType}</Badge>
                    </div>
                    <CardTitle className="mt-4">{item.title}</CardTitle>
                    <CardDescription className="line-clamp-2">
                      {item.description || 'No description provided'}
                    </CardDescription>
                  </CardHeader>
                  <CardContent className="space-y-4">
                    <div className="flex flex-wrap gap-2">
                      {item.tags.map((tag) => (
                        <Badge key={tag} variant="secondary" className="text-xs">
                          <Tag className="h-3 w-3 mr-1" />
                          {tag}
                        </Badge>
                      ))}
                    </div>
                    <Button
                      onClick={(e) => {
                        e.stopPropagation();
                        handleDownload(item);
                      }}
                      className="w-full"
                    >
                      <Download className="h-4 w-4 mr-2" />
                      Download
                    </Button>
                  </CardContent>
                </Card>
              ))}
            </div>
          </TabsContent>

          {/* Upload Tab */}
          <TabsContent value="upload" className="space-y-6">
            <Card>
              <CardHeader>
                <CardTitle>Upload to Library</CardTitle>
                <CardDescription>
                  Share your OSCAL document with the community
                </CardDescription>
              </CardHeader>
              <CardContent>
                <form onSubmit={handleUpload} className="space-y-4">
                  <div className="space-y-2">
                    <Label htmlFor="title">Title *</Label>
                    <Input
                      id="title"
                      value={uploadTitle}
                      onChange={(e) => setUploadTitle(e.target.value)}
                      required
                      placeholder="e.g., NIST 800-53 Rev 5 High Baseline"
                    />
                  </div>
                  <div className="space-y-2">
                    <Label htmlFor="description">Description</Label>
                    <textarea
                      id="description"
                      className="w-full rounded-md border border-input bg-background px-3 py-2"
                      rows={3}
                      value={uploadDescription}
                      onChange={(e) => setUploadDescription(e.target.value)}
                      placeholder="Describe your OSCAL document..."
                    />
                  </div>
                  <div className="space-y-2">
                    <Label htmlFor="oscal-type">OSCAL Type *</Label>
                    <select
                      id="oscal-type"
                      className="w-full rounded-md border border-input bg-background px-3 py-2"
                      value={uploadType}
                      onChange={(e) => setUploadType(e.target.value as OscalModelType)}
                      required
                    >
                      {oscalTypes.map((type) => (
                        <option key={type} value={type}>
                          {type}
                        </option>
                      ))}
                    </select>
                  </div>
                  <div className="space-y-2">
                    <Label htmlFor="file">File *</Label>
                    <Input
                      id="file"
                      type="file"
                      accept=".json,.xml,.yaml,.yml"
                      onChange={(e) => setUploadFile(e.target.files?.[0] || null)}
                      required
                    />
                    <p className="text-sm text-muted-foreground">
                      Supported formats: JSON, XML, YAML
                    </p>
                  </div>
                  <div className="space-y-2">
                    <Label htmlFor="tags">Tags</Label>
                    <Input
                      id="tags"
                      value={uploadTags}
                      onChange={(e) => setUploadTags(e.target.value)}
                      placeholder="e.g., NIST, FedRAMP, security (comma-separated)"
                    />
                  </div>
                  <Button type="submit" disabled={uploading} className="w-full">
                    {uploading ? (
                      <>
                        <div className="animate-spin rounded-full h-4 w-4 border-b-2 border-white mr-2" />
                        Uploading...
                      </>
                    ) : (
                      <>
                        <Upload className="h-4 w-4 mr-2" />
                        Upload to Library
                      </>
                    )}
                  </Button>
                </form>
              </CardContent>
            </Card>
          </TabsContent>

          {/* Analytics Tab */}
          <TabsContent value="analytics" className="space-y-6">
            {analytics && (
              <>
                <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
                  <Card>
                    <CardHeader>
                      <CardTitle className="text-sm font-medium">Total Items</CardTitle>
                    </CardHeader>
                    <CardContent>
                      <div className="text-3xl font-bold">{analytics.totalItems}</div>
                    </CardContent>
                  </Card>
                  <Card>
                    <CardHeader>
                      <CardTitle className="text-sm font-medium">Total Versions</CardTitle>
                    </CardHeader>
                    <CardContent>
                      <div className="text-3xl font-bold">{analytics.totalVersions}</div>
                    </CardContent>
                  </Card>
                  <Card>
                    <CardHeader>
                      <CardTitle className="text-sm font-medium">Total Tags</CardTitle>
                    </CardHeader>
                    <CardContent>
                      <div className="text-3xl font-bold">{analytics.totalTags}</div>
                    </CardContent>
                  </Card>
                </div>

                <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
                  <Card>
                    <CardHeader>
                      <CardTitle>Items by Type</CardTitle>
                    </CardHeader>
                    <CardContent>
                      <div className="space-y-2">
                        {Object.entries(analytics.itemsByType).map(([type, count]) => (
                          <div key={type} className="flex items-center justify-between">
                            <span className="text-sm">{type}</span>
                            <Badge variant="secondary">{count}</Badge>
                          </div>
                        ))}
                      </div>
                    </CardContent>
                  </Card>

                  <Card>
                    <CardHeader>
                      <CardTitle>Popular Tags</CardTitle>
                    </CardHeader>
                    <CardContent>
                      <div className="space-y-2">
                        {analytics.popularTags.map((tag) => (
                          <div key={tag.name} className="flex items-center justify-between">
                            <span className="text-sm">{tag.name}</span>
                            <Badge variant="secondary">{tag.count}</Badge>
                          </div>
                        ))}
                      </div>
                    </CardContent>
                  </Card>
                </div>

                <Card>
                  <CardHeader>
                    <CardTitle>Most Downloaded</CardTitle>
                  </CardHeader>
                  <CardContent>
                    <div className="space-y-4">
                      {analytics.mostDownloaded.map((item, index) => (
                        <div key={item.itemId} className="flex items-center space-x-4">
                          <div className="flex items-center justify-center w-8 h-8 rounded-full bg-primary/10 text-primary font-bold">
                            {index + 1}
                          </div>
                          <div className="flex-1">
                            <p className="font-medium">{item.title}</p>
                          </div>
                          <div className="flex items-center text-muted-foreground">
                            <Download className="h-4 w-4 mr-2" />
                            {item.downloadCount}
                          </div>
                        </div>
                      ))}
                    </div>
                  </CardContent>
                </Card>
              </>
            )}
          </TabsContent>
        </Tabs>
      </div>
      <Footer />
    </div>
  );
}
