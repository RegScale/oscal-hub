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
  FileText,
  Upload,
  Search,
  BarChart3,
  Download,
  Eye,
  Tag,
  Calendar,
  User,
  ArrowLeft,
  MessageSquare,
  Globe,
  Building,
  Lock
} from 'lucide-react';
import { StarRating } from '@/components/ui/star-rating';
import { apiClient } from '@/lib/api-client';
import type { Artifact, ArtifactAnalytics, ArtifactVisibility } from '@/types/oscal';
import { useAuth } from '@/contexts/AuthContext';
import { toast } from 'sonner';

export default function ArtifactsPage() {
  const router = useRouter();
  const { isAuthenticated, isLoading, user } = useAuth();
  const [artifacts, setArtifacts] = useState<Artifact[]>([]);
  const [analytics, setAnalytics] = useState<ArtifactAnalytics | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedVisibility, setSelectedVisibility] = useState<string>('');
  const [selectedTag, setSelectedTag] = useState<string>('');
  const [activeTab, setActiveTab] = useState('browse');

  // Create form state
  const [createTitle, setCreateTitle] = useState('');
  const [createDescription, setCreateDescription] = useState('');
  const [createVisibility, setCreateVisibility] = useState<ArtifactVisibility>('PRIVATE');
  const [createContent, setCreateContent] = useState('');
  const [createTags, setCreateTags] = useState('');
  const [creating, setCreating] = useState(false);
  const [createSuccess, setCreateSuccess] = useState(false);
  const [extractedVariables, setExtractedVariables] = useState<string[]>([]);

  // Get user's organization ID from localStorage
  const [userOrgId, setUserOrgId] = useState<number | undefined>(undefined);

  useEffect(() => {
    if (typeof window !== 'undefined') {
      const storedUser = localStorage.getItem('user');
      if (storedUser) {
        try {
          const userData = JSON.parse(storedUser);
          setUserOrgId(userData.organizationId);
        } catch (e) {
          // ignore
        }
      }
    }
  }, []);

  useEffect(() => {
    if (isAuthenticated) {
      loadArtifacts();
      loadAnalytics();
    }
  }, [isAuthenticated]);

  // Extract variables from content in real-time
  useEffect(() => {
    const pattern = /\{\{\s*([^}]+?)\s*\}\}/g;
    const vars: string[] = [];
    let match;
    while ((match = pattern.exec(createContent)) !== null) {
      const varName = match[1].trim();
      if (!vars.includes(varName)) {
        vars.push(varName);
      }
    }
    setExtractedVariables(vars);
  }, [createContent]);

  const loadArtifacts = async () => {
    try {
      setLoading(true);
      const data = await apiClient.getAllArtifacts();
      setArtifacts(data);
      setError(null);
    } catch (err) {
      setError('Failed to load artifacts');
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  const loadAnalytics = async () => {
    try {
      const data = await apiClient.getArtifactAnalytics();
      setAnalytics(data);
    } catch (err) {
      console.error('Failed to load analytics:', err);
    }
  };

  const handleSearch = async () => {
    try {
      setLoading(true);
      const results = await apiClient.searchArtifacts({
        keyword: searchQuery || undefined,
        visibility: selectedVisibility as ArtifactVisibility || undefined,
        tag: selectedTag || undefined,
      });
      setArtifacts(results);
      setError(null);
    } catch (err) {
      setError('Search failed');
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  const handleCreate = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!createContent.trim()) {
      setError('Please enter some content');
      toast.error('Please enter some content');
      return;
    }

    try {
      setCreating(true);
      setError(null);

      const tags = createTags.split(',').map(t => t.trim()).filter(t => t);

      await apiClient.createArtifact({
        title: createTitle,
        description: createDescription,
        visibility: createVisibility,
        organizationId: createVisibility === 'ORGANIZATION' ? userOrgId : undefined,
        content: createContent,
        tags,
      });

      setCreateSuccess(true);
      toast.success('Artifact created successfully');
      // Reset form
      setCreateTitle('');
      setCreateDescription('');
      setCreateVisibility('PRIVATE');
      setCreateContent('');
      setCreateTags('');

      // Reload artifacts
      loadArtifacts();
      loadAnalytics();

      // Switch to browse tab
      setActiveTab('browse');

      setTimeout(() => setCreateSuccess(false), 5000);
    } catch (err) {
      setError('Create failed: ' + (err as Error).message);
      toast.error('Create failed: ' + (err as Error).message);
      console.error(err);
    } finally {
      setCreating(false);
    }
  };

  const handleDownload = async (artifact: Artifact) => {
    try {
      const content = await apiClient.getArtifactContent(artifact.artifactId);
      const blob = new Blob([content], { type: 'text/markdown' });
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `${artifact.title.replace(/[^a-z0-9]/gi, '_').toLowerCase()}.md`;
      document.body.appendChild(a);
      a.click();
      document.body.removeChild(a);
      URL.revokeObjectURL(url);
      toast.success('Artifact downloaded');
    } catch (err) {
      setError('Download failed');
      toast.error('Download failed');
      console.error(err);
    }
  };

  const getVisibilityIcon = (visibility: ArtifactVisibility) => {
    switch (visibility) {
      case 'PUBLIC':
        return <Globe className="h-4 w-4" />;
      case 'ORGANIZATION':
        return <Building className="h-4 w-4" />;
      case 'PRIVATE':
      default:
        return <Lock className="h-4 w-4" />;
    }
  };

  const getVisibilityColor = (visibility: ArtifactVisibility) => {
    switch (visibility) {
      case 'PUBLIC':
        return 'bg-green-100 text-green-800 border-green-200';
      case 'ORGANIZATION':
        return 'bg-blue-100 text-blue-800 border-blue-200';
      case 'PRIVATE':
      default:
        return 'bg-gray-100 text-gray-800 border-gray-200';
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
              Please log in to access the artifacts.
            </AlertDescription>
          </Alert>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-background">
      <div className="container mx-auto py-12 px-4">
        <div className="mb-8">
          <Button
            variant="ghost"
            onClick={() => router.push('/')}
            className="inline-flex items-center gap-2 text-sm text-muted-foreground hover:text-foreground mb-4 transition-colors px-0"
          >
            <ArrowLeft className="h-4 w-4" />
            Back to Dashboard
          </Button>
          <div className="flex items-center">
            <FileText className="h-10 w-10 text-primary mr-4" />
            <div>
              <h1 className="text-4xl font-bold">Artifacts</h1>
              <p className="text-muted-foreground mt-2">
                Create and share Markdown templates with variables
              </p>
            </div>
          </div>
        </div>

        {error && (
          <Alert variant="destructive" className="mb-6">
            <AlertDescription>{error}</AlertDescription>
          </Alert>
        )}

        {createSuccess && (
          <Alert className="mb-6 bg-green-50 border-green-200">
            <AlertDescription className="text-green-800">
              Successfully created artifact!
            </AlertDescription>
          </Alert>
        )}

        <Tabs value={activeTab} onValueChange={setActiveTab} className="space-y-6">
          <TabsList className="grid w-full grid-cols-4">
            <TabsTrigger value="browse">
              <FileText className="h-4 w-4 mr-2" />
              Browse
            </TabsTrigger>
            <TabsTrigger value="search">
              <Search className="h-4 w-4 mr-2" />
              Search
            </TabsTrigger>
            <TabsTrigger value="create">
              <Upload className="h-4 w-4 mr-2" />
              Create
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
                <p className="text-muted-foreground">Loading artifacts...</p>
              </div>
            ) : artifacts.length === 0 ? (
              <Card>
                <CardContent className="py-12 text-center">
                  <FileText className="h-16 w-16 text-muted-foreground mx-auto mb-4" />
                  <p className="text-lg text-muted-foreground">
                    No artifacts yet. Be the first to create one!
                  </p>
                </CardContent>
              </Card>
            ) : (
              <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
                {artifacts.map((artifact) => (
                  <Card
                    key={artifact.artifactId}
                    className="hover:shadow-lg transition-shadow cursor-pointer"
                    onClick={() => router.push(`/artifacts/${artifact.artifactId}`)}
                  >
                    <CardHeader>
                      <div className="flex items-start justify-between">
                        <FileText className="h-8 w-8 text-primary" />
                        <Badge className={`${getVisibilityColor(artifact.visibility)} flex items-center gap-1`}>
                          {getVisibilityIcon(artifact.visibility)}
                          {artifact.visibility}
                        </Badge>
                      </div>
                      <CardTitle className="mt-4">{artifact.title}</CardTitle>
                      <CardDescription className="line-clamp-2">
                        {artifact.description || 'No description provided'}
                      </CardDescription>
                    </CardHeader>
                    <CardContent className="space-y-4">
                      {artifact.extractedVariables.length > 0 && (
                        <div className="flex flex-wrap gap-1">
                          {artifact.extractedVariables.slice(0, 3).map((variable) => (
                            <Badge key={variable} variant="outline" className="text-xs bg-purple-50 text-purple-700 border-purple-200">
                              {`{{ ${variable} }}`}
                            </Badge>
                          ))}
                          {artifact.extractedVariables.length > 3 && (
                            <Badge variant="outline" className="text-xs">
                              +{artifact.extractedVariables.length - 3} more
                            </Badge>
                          )}
                        </div>
                      )}
                      <div className="flex flex-wrap gap-2">
                        {artifact.tags.map((tag) => (
                          <Badge key={tag} variant="secondary" className="text-xs">
                            <Tag className="h-3 w-3 mr-1" />
                            {tag}
                          </Badge>
                        ))}
                      </div>
                      <div className="space-y-2 text-sm text-muted-foreground">
                        <div className="flex items-center">
                          <User className="h-4 w-4 mr-2" />
                          {artifact.createdBy}
                        </div>
                        <div className="flex items-center">
                          <Calendar className="h-4 w-4 mr-2" />
                          {new Date(artifact.updatedAt).toLocaleDateString()}
                        </div>
                        <div className="flex items-center justify-between">
                          <div className="flex items-center">
                            <Download className="h-4 w-4 mr-2" />
                            {artifact.downloadCount}
                          </div>
                          <div className="flex items-center">
                            <Eye className="h-4 w-4 mr-2" />
                            {artifact.viewCount}
                          </div>
                        </div>
                      </div>
                      {/* Rating and Comments */}
                      <div className="flex items-center justify-between pt-2 border-t">
                        <StarRating
                          rating={artifact.averageRating || 0}
                          readonly
                          size="sm"
                          showCount
                          totalRatings={artifact.totalRatings || 0}
                        />
                        <div className="flex items-center text-sm text-muted-foreground">
                          <MessageSquare className="h-4 w-4 mr-1" />
                          {artifact.commentCount || 0}
                        </div>
                      </div>
                      <Button
                        onClick={(e) => {
                          e.stopPropagation();
                          handleDownload(artifact);
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
                <CardTitle>Search Artifacts</CardTitle>
                <CardDescription>
                  Find artifacts by keyword, visibility, or tag
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
                    <Label htmlFor="search-visibility">Visibility</Label>
                    <select
                      id="search-visibility"
                      className="w-full rounded-md border border-input bg-background px-3 py-2"
                      value={selectedVisibility}
                      onChange={(e) => setSelectedVisibility(e.target.value)}
                    >
                      <option value="">All Visibility</option>
                      <option value="PRIVATE">Private (My Own)</option>
                      <option value="ORGANIZATION">Organization</option>
                      <option value="PUBLIC">Public</option>
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
              {artifacts.map((artifact) => (
                <Card
                  key={artifact.artifactId}
                  className="hover:shadow-lg transition-shadow cursor-pointer"
                  onClick={() => router.push(`/artifacts/${artifact.artifactId}`)}
                >
                  <CardHeader>
                    <div className="flex items-start justify-between">
                      <FileText className="h-8 w-8 text-primary" />
                      <Badge className={`${getVisibilityColor(artifact.visibility)} flex items-center gap-1`}>
                        {getVisibilityIcon(artifact.visibility)}
                        {artifact.visibility}
                      </Badge>
                    </div>
                    <CardTitle className="mt-4">{artifact.title}</CardTitle>
                    <CardDescription className="line-clamp-2">
                      {artifact.description || 'No description provided'}
                    </CardDescription>
                  </CardHeader>
                  <CardContent className="space-y-4">
                    <div className="flex flex-wrap gap-2">
                      {artifact.tags.map((tag) => (
                        <Badge key={tag} variant="secondary" className="text-xs">
                          <Tag className="h-3 w-3 mr-1" />
                          {tag}
                        </Badge>
                      ))}
                    </div>
                    <Button
                      onClick={(e) => {
                        e.stopPropagation();
                        handleDownload(artifact);
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

          {/* Create Tab */}
          <TabsContent value="create" className="space-y-6">
            <Card>
              <CardHeader>
                <CardTitle>Create Artifact</CardTitle>
                <CardDescription>
                  Create a new Markdown artifact with template variables
                </CardDescription>
              </CardHeader>
              <CardContent>
                <form onSubmit={handleCreate} className="space-y-4">
                  <div className="space-y-2">
                    <Label htmlFor="title">Title *</Label>
                    <Input
                      id="title"
                      value={createTitle}
                      onChange={(e) => setCreateTitle(e.target.value)}
                      required
                      placeholder="e.g., Security Acknowledgment Form"
                    />
                  </div>
                  <div className="space-y-2">
                    <Label htmlFor="description">Description</Label>
                    <textarea
                      id="description"
                      className="w-full rounded-md border border-input bg-background px-3 py-2"
                      rows={2}
                      value={createDescription}
                      onChange={(e) => setCreateDescription(e.target.value)}
                      placeholder="Describe your artifact..."
                    />
                  </div>
                  <div className="space-y-2">
                    <Label htmlFor="visibility">Visibility *</Label>
                    <select
                      id="visibility"
                      className="w-full rounded-md border border-input bg-background px-3 py-2"
                      value={createVisibility}
                      onChange={(e) => setCreateVisibility(e.target.value as ArtifactVisibility)}
                      required
                    >
                      <option value="PRIVATE">Private - Only you can see this</option>
                      <option value="ORGANIZATION">Organization - Members of your organization</option>
                      <option value="PUBLIC">Public - Everyone can see this</option>
                    </select>
                  </div>
                  <div className="space-y-2">
                    <Label htmlFor="content">Content (Markdown) *</Label>
                    <textarea
                      id="content"
                      className="w-full rounded-md border border-input bg-background px-3 py-2 font-mono text-sm"
                      rows={12}
                      value={createContent}
                      onChange={(e) => setCreateContent(e.target.value)}
                      required
                      placeholder={`# Document Title

## Section 1

This document was created for {{ organization_name }} on {{ date }}.

The responsible party is {{ responsible_person }}.

## Section 2

Additional content here...

Use {{ variable_name }} syntax for template variables.`}
                    />
                    <p className="text-sm text-muted-foreground">
                      Use {'{{ variable_name }}'} syntax for template variables
                    </p>
                  </div>

                  {/* Display extracted variables */}
                  {extractedVariables.length > 0 && (
                    <div className="space-y-2">
                      <Label>Detected Variables</Label>
                      <div className="flex flex-wrap gap-2">
                        {extractedVariables.map((variable) => (
                          <Badge
                            key={variable}
                            variant="outline"
                            className="bg-purple-50 text-purple-700 border-purple-200"
                          >
                            {`{{ ${variable} }}`}
                          </Badge>
                        ))}
                      </div>
                    </div>
                  )}

                  <div className="space-y-2">
                    <Label htmlFor="tags">Tags</Label>
                    <Input
                      id="tags"
                      value={createTags}
                      onChange={(e) => setCreateTags(e.target.value)}
                      placeholder="e.g., compliance, security, template (comma-separated)"
                    />
                  </div>
                  <Button type="submit" disabled={creating} className="w-full">
                    {creating ? (
                      <>
                        <div className="animate-spin rounded-full h-4 w-4 border-b-2 border-white mr-2" />
                        Creating...
                      </>
                    ) : (
                      <>
                        <Upload className="h-4 w-4 mr-2" />
                        Create Artifact
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
                      <CardTitle className="text-sm font-medium">Total Artifacts</CardTitle>
                    </CardHeader>
                    <CardContent>
                      <div className="text-3xl font-bold">{analytics.totalArtifacts}</div>
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
                      <CardTitle>Artifacts by Visibility</CardTitle>
                    </CardHeader>
                    <CardContent>
                      <div className="space-y-2">
                        {Object.entries(analytics.artifactsByVisibility).map(([visibility, count]) => (
                          <div key={visibility} className="flex items-center justify-between">
                            <div className="flex items-center gap-2">
                              {getVisibilityIcon(visibility as ArtifactVisibility)}
                              <span className="text-sm">{visibility}</span>
                            </div>
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
                        <div key={item.artifactId} className="flex items-center space-x-4">
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
    </div>
  );
}
