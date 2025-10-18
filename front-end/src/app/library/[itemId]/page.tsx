'use client';

import { useState, useEffect, useRef } from 'react';
import { useParams, useRouter } from 'next/navigation';
import { Card, CardHeader, CardTitle, CardDescription, CardContent } from '@/components/ui/card';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Badge } from '@/components/ui/badge';
import { Alert, AlertDescription } from '@/components/ui/alert';
import {
  ArrowLeft,
  FileText,
  Calendar,
  User,
  Download,
  Upload,
  Edit,
  History,
  Tag,
  Save,
  X,
  Clock
} from 'lucide-react';
import { apiClient } from '@/lib/api-client';
import type { LibraryItem, LibraryVersion, OscalModelType } from '@/types/oscal';
import { useAuth } from '@/contexts/AuthContext';
import { Footer } from '@/components/Footer';
import { toast } from 'sonner';

export default function LibraryItemDetailPage() {
  const params = useParams();
  const router = useRouter();
  const { isAuthenticated, isLoading: authLoading } = useAuth();

  const itemId = params.itemId as string;

  const [item, setItem] = useState<LibraryItem | null>(null);
  const [versions, setVersions] = useState<LibraryVersion[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [activeTab, setActiveTab] = useState('details');

  // Edit mode state
  const [editing, setEditing] = useState(false);
  const [editTitle, setEditTitle] = useState('');
  const [editDescription, setEditDescription] = useState('');
  const [editTags, setEditTags] = useState('');
  const [saving, setSaving] = useState(false);

  // Upload new version state
  const [uploadFile, setUploadFile] = useState<File | null>(null);
  const [uploadDescription, setUploadDescription] = useState('');
  const [uploading, setUploading] = useState(false);
  const [uploadSuccess, setUploadSuccess] = useState(false);

  // Track which itemId we've already incremented view count for
  const viewIncrementedRef = useRef<string | null>(null);

  useEffect(() => {
    if (isAuthenticated && viewIncrementedRef.current !== itemId) {
      viewIncrementedRef.current = itemId;
      loadItem();
      loadVersions();
    }
  }, [isAuthenticated, itemId]);

  const loadItem = async () => {
    try {
      setLoading(true);
      const data = await apiClient.getLibraryItem(itemId);
      setItem(data);
      setEditTitle(data.title);
      setEditDescription(data.description || '');
      setEditTags(data.tags.join(', '));
      setError(null);
    } catch (err) {
      setError('Failed to load library item');
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  const loadVersions = async () => {
    try {
      const data = await apiClient.getLibraryVersionHistory(itemId);
      setVersions(data);
    } catch (err) {
      console.error('Failed to load version history:', err);
    }
  };

  const handleSaveMetadata = async () => {
    if (!item) return;

    try {
      setSaving(true);
      setError(null);

      const tags = editTags.split(',').map(t => t.trim()).filter(t => t);

      await apiClient.updateLibraryItem(itemId, {
        title: editTitle,
        description: editDescription,
        tags,
      });

      setEditing(false);
      toast.success('Library item updated');
      await loadItem();
    } catch (err) {
      setError('Failed to save changes: ' + (err as Error).message);
      toast.error('Failed to save changes: ' + (err as Error).message);
      console.error(err);
    } finally {
      setSaving(false);
    }
  };

  const handleUploadVersion = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!uploadFile || !item) {
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

      await apiClient.addLibraryVersion(itemId, {
        fileName: uploadFile.name,
        format,
        fileContent: content,
        changeDescription: uploadDescription,
      });

      setUploadSuccess(true);
      toast.success('New version added');
      setUploadFile(null);
      setUploadDescription('');

      // Reload data
      await loadItem();
      await loadVersions();

      // Switch to version history tab
      setActiveTab('versions');

      setTimeout(() => setUploadSuccess(false), 5000);
    } catch (err) {
      setError('Upload failed: ' + (err as Error).message);
      toast.error('Upload failed: ' + (err as Error).message);
      console.error(err);
    } finally {
      setUploading(false);
    }
  };

  const handleDownloadVersion = async (version: LibraryVersion) => {
    try {
      const content = await apiClient.getLibraryVersionContent(version.versionId);
      const blob = new Blob([content], { type: 'text/plain' });
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = version.fileName;
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

  const handleDownloadCurrent = async () => {
    if (!item || !item.currentVersion) return;
    await handleDownloadVersion(item.currentVersion);
  };

  if (authLoading || loading) {
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
              Please log in to view this library item.
            </AlertDescription>
          </Alert>
        </div>
        <Footer />
      </div>
    );
  }

  if (!item) {
    return (
      <div className="min-h-screen bg-background">
        <div className="container mx-auto py-12 px-4">
          <Alert variant="destructive">
            <AlertDescription>
              Library item not found.
            </AlertDescription>
          </Alert>
          <Button
            variant="outline"
            onClick={() => router.push('/library')}
            className="mt-4"
          >
            <ArrowLeft className="h-4 w-4 mr-2" />
            Back to Library
          </Button>
        </div>
        <Footer />
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-background">
      <div className="container mx-auto py-12 px-4">
        {/* Header */}
        <div className="flex items-center justify-between mb-8">
          <div className="flex items-center gap-4">
            <Button
              variant="outline"
              onClick={() => router.push('/library')}
            >
              <ArrowLeft className="h-4 w-4 mr-2" />
              Back
            </Button>
            <div>
              <div className="flex items-center gap-3">
                <h1 className="text-3xl font-bold">{item.title}</h1>
                <Badge variant="outline">{item.oscalType}</Badge>
              </div>
              <p className="text-sm text-muted-foreground mt-1">
                Version {item.currentVersion?.versionNumber || 1} • Updated {new Date(item.updatedAt).toLocaleDateString()}
              </p>
            </div>
          </div>
          <div className="flex gap-2">
            {!editing && (
              <Button variant="outline" onClick={() => setEditing(true)}>
                <Edit className="h-4 w-4 mr-2" />
                Edit
              </Button>
            )}
            <Button onClick={handleDownloadCurrent}>
              <Download className="h-4 w-4 mr-2" />
              Download Current
            </Button>
          </div>
        </div>

        {error && (
          <Alert variant="destructive" className="mb-6">
            <AlertDescription>{error}</AlertDescription>
          </Alert>
        )}

        {uploadSuccess && (
          <Alert className="mb-6 bg-green-50 border-green-200">
            <AlertDescription className="text-green-800">
              New version uploaded successfully!
            </AlertDescription>
          </Alert>
        )}

        <Tabs value={activeTab} onValueChange={setActiveTab} className="space-y-6">
          <TabsList className="grid w-full grid-cols-3">
            <TabsTrigger value="details">
              <FileText className="h-4 w-4 mr-2" />
              Details
            </TabsTrigger>
            <TabsTrigger value="versions">
              <History className="h-4 w-4 mr-2" />
              Version History ({versions.length})
            </TabsTrigger>
            <TabsTrigger value="upload">
              <Upload className="h-4 w-4 mr-2" />
              Upload New Version
            </TabsTrigger>
          </TabsList>

          {/* Details Tab */}
          <TabsContent value="details" className="space-y-6">
            <Card>
              <CardHeader>
                <CardTitle>Item Information</CardTitle>
              </CardHeader>
              <CardContent className="space-y-4">
                {editing ? (
                  <div className="space-y-4">
                    <div className="space-y-2">
                      <Label htmlFor="edit-title">Title</Label>
                      <Input
                        id="edit-title"
                        value={editTitle}
                        onChange={(e) => setEditTitle(e.target.value)}
                      />
                    </div>
                    <div className="space-y-2">
                      <Label htmlFor="edit-description">Description</Label>
                      <textarea
                        id="edit-description"
                        className="w-full rounded-md border border-input bg-background px-3 py-2"
                        rows={4}
                        value={editDescription}
                        onChange={(e) => setEditDescription(e.target.value)}
                      />
                    </div>
                    <div className="space-y-2">
                      <Label htmlFor="edit-tags">Tags (comma-separated)</Label>
                      <Input
                        id="edit-tags"
                        value={editTags}
                        onChange={(e) => setEditTags(e.target.value)}
                        placeholder="e.g., NIST, FedRAMP, security"
                      />
                    </div>
                    <div className="flex gap-2">
                      <Button onClick={handleSaveMetadata} disabled={saving}>
                        {saving ? (
                          <>
                            <div className="animate-spin rounded-full h-4 w-4 border-b-2 border-white mr-2" />
                            Saving...
                          </>
                        ) : (
                          <>
                            <Save className="h-4 w-4 mr-2" />
                            Save Changes
                          </>
                        )}
                      </Button>
                      <Button variant="outline" onClick={() => {
                        setEditing(false);
                        setEditTitle(item.title);
                        setEditDescription(item.description || '');
                        setEditTags(item.tags.join(', '));
                      }}>
                        <X className="h-4 w-4 mr-2" />
                        Cancel
                      </Button>
                    </div>
                  </div>
                ) : (
                  <div className="space-y-4">
                    <div>
                      <h3 className="text-sm font-medium text-muted-foreground mb-1">Description</h3>
                      <p>{item.description || 'No description provided'}</p>
                    </div>
                    <div>
                      <h3 className="text-sm font-medium text-muted-foreground mb-2">Tags</h3>
                      <div className="flex flex-wrap gap-2">
                        {item.tags.length > 0 ? (
                          item.tags.map((tag) => (
                            <Badge key={tag} variant="secondary">
                              <Tag className="h-3 w-3 mr-1" />
                              {tag}
                            </Badge>
                          ))
                        ) : (
                          <p className="text-sm text-muted-foreground">No tags</p>
                        )}
                      </div>
                    </div>
                    <div className="grid grid-cols-2 gap-4 pt-4 border-t">
                      <div>
                        <h3 className="text-sm font-medium text-muted-foreground mb-1">Created By</h3>
                        <div className="flex items-center">
                          <User className="h-4 w-4 mr-2" />
                          <span>{item.createdBy}</span>
                        </div>
                      </div>
                      <div>
                        <h3 className="text-sm font-medium text-muted-foreground mb-1">Created Date</h3>
                        <div className="flex items-center">
                          <Calendar className="h-4 w-4 mr-2" />
                          <span>{new Date(item.createdAt).toLocaleString()}</span>
                        </div>
                      </div>
                      <div>
                        <h3 className="text-sm font-medium text-muted-foreground mb-1">Total Downloads</h3>
                        <div className="flex items-center">
                          <Download className="h-4 w-4 mr-2" />
                          <span>{item.downloadCount}</span>
                        </div>
                      </div>
                      <div>
                        <h3 className="text-sm font-medium text-muted-foreground mb-1">Total Versions</h3>
                        <div className="flex items-center">
                          <History className="h-4 w-4 mr-2" />
                          <span>{item.versionCount}</span>
                        </div>
                      </div>
                    </div>
                  </div>
                )}
              </CardContent>
            </Card>

            {/* Current Version Info */}
            {item.currentVersion && (
              <Card>
                <CardHeader>
                  <CardTitle>Current Version</CardTitle>
                </CardHeader>
                <CardContent>
                  <div className="space-y-3">
                    <div className="flex items-center justify-between">
                      <span className="text-sm font-medium">Version {item.currentVersion.versionNumber}</span>
                      <Badge>{item.currentVersion.format}</Badge>
                    </div>
                    <div className="flex items-center text-sm text-muted-foreground">
                      <User className="h-4 w-4 mr-2" />
                      Uploaded by {item.currentVersion.uploadedBy}
                    </div>
                    <div className="flex items-center text-sm text-muted-foreground">
                      <Calendar className="h-4 w-4 mr-2" />
                      {new Date(item.currentVersion.uploadedAt).toLocaleString()}
                    </div>
                    <div className="flex items-center text-sm text-muted-foreground">
                      <FileText className="h-4 w-4 mr-2" />
                      {item.currentVersion.fileName} ({(item.currentVersion.fileSize / 1024).toFixed(2)} KB)
                    </div>
                    {item.currentVersion.changeDescription && (
                      <div className="pt-2 border-t">
                        <h4 className="text-sm font-medium mb-1">Change Description</h4>
                        <p className="text-sm text-muted-foreground">{item.currentVersion.changeDescription}</p>
                      </div>
                    )}
                  </div>
                </CardContent>
              </Card>
            )}
          </TabsContent>

          {/* Version History Tab */}
          <TabsContent value="versions" className="space-y-4">
            {versions.length === 0 ? (
              <Card>
                <CardContent className="py-12 text-center">
                  <History className="h-16 w-16 text-muted-foreground mx-auto mb-4" />
                  <p className="text-lg text-muted-foreground">
                    No version history available
                  </p>
                </CardContent>
              </Card>
            ) : (
              <div className="space-y-4">
                {versions.map((version, index) => (
                  <Card key={version.versionId} className={index === 0 ? 'border-primary' : ''}>
                    <CardContent className="pt-6">
                      <div className="flex items-start justify-between">
                        <div className="space-y-3 flex-1">
                          <div className="flex items-center gap-3">
                            <h3 className="text-lg font-semibold">Version {version.versionNumber}</h3>
                            <Badge variant={index === 0 ? 'default' : 'secondary'}>
                              {index === 0 ? 'Current' : version.format}
                            </Badge>
                          </div>
                          <div className="space-y-2 text-sm text-muted-foreground">
                            <div className="flex items-center">
                              <User className="h-4 w-4 mr-2" />
                              Uploaded by {version.uploadedBy}
                            </div>
                            <div className="flex items-center">
                              <Clock className="h-4 w-4 mr-2" />
                              {new Date(version.uploadedAt).toLocaleString()}
                            </div>
                            <div className="flex items-center">
                              <FileText className="h-4 w-4 mr-2" />
                              {version.fileName} ({(version.fileSize / 1024).toFixed(2)} KB)
                            </div>
                          </div>
                          {version.changeDescription && (
                            <div className="pt-2 border-t">
                              <p className="text-sm">{version.changeDescription}</p>
                            </div>
                          )}
                        </div>
                        <Button
                          onClick={() => handleDownloadVersion(version)}
                          variant="outline"
                          size="sm"
                        >
                          <Download className="h-4 w-4 mr-2" />
                          Download
                        </Button>
                      </div>
                    </CardContent>
                  </Card>
                ))}
              </div>
            )}
          </TabsContent>

          {/* Upload New Version Tab */}
          <TabsContent value="upload" className="space-y-6">
            <Card>
              <CardHeader>
                <CardTitle>Upload New Version</CardTitle>
                <CardDescription>
                  Add a new version of this OSCAL document to the library
                </CardDescription>
              </CardHeader>
              <CardContent>
                <form onSubmit={handleUploadVersion} className="space-y-4">
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
                    <Label htmlFor="change-description">Change Description</Label>
                    <textarea
                      id="change-description"
                      className="w-full rounded-md border border-input bg-background px-3 py-2"
                      rows={3}
                      value={uploadDescription}
                      onChange={(e) => setUploadDescription(e.target.value)}
                      placeholder="Describe what changed in this version..."
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
                        Upload New Version
                      </>
                    )}
                  </Button>
                </form>
              </CardContent>
            </Card>
          </TabsContent>
        </Tabs>
      </div>
      <Footer />
    </div>
  );
}
