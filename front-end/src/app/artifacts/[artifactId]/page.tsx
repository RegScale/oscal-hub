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
  Clock,
  MessageSquare,
  Star,
  Globe,
  Building,
  Lock,
  Trash2,
  Eye
} from 'lucide-react';
import { apiClient } from '@/lib/api-client';
import type { Artifact, ArtifactVersion, ArtifactVisibility, RatingStats, ArtifactComment } from '@/types/oscal';
import { StarRating } from '@/components/ui/star-rating';
import { CommentThread } from '@/components/library/comment-thread';
import { useAuth } from '@/contexts/AuthContext';
import { toast } from 'sonner';
import { HelpButton } from '@/components/HelpButton';
import { MarkdownPreview } from '@/components/markdown-preview';

export default function ArtifactDetailPage() {
  const params = useParams();
  const router = useRouter();
  const { isAuthenticated, isLoading: authLoading, user } = useAuth();

  const artifactId = params.artifactId as string;

  const [artifact, setArtifact] = useState<Artifact | null>(null);
  const [versions, setVersions] = useState<ArtifactVersion[]>([]);
  const [content, setContent] = useState<string>('');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [activeTab, setActiveTab] = useState('details');

  // Edit mode state
  const [editing, setEditing] = useState(false);
  const [editTitle, setEditTitle] = useState('');
  const [editDescription, setEditDescription] = useState('');
  const [editVisibility, setEditVisibility] = useState<ArtifactVisibility>('PRIVATE');
  const [editTags, setEditTags] = useState('');
  const [editContent, setEditContent] = useState('');
  const [saving, setSaving] = useState(false);

  // Upload new version state
  const [newVersionContent, setNewVersionContent] = useState('');
  const [newVersionDescription, setNewVersionDescription] = useState('');
  const [uploading, setUploading] = useState(false);
  const [uploadSuccess, setUploadSuccess] = useState(false);

  // Rating state
  const [ratingStats, setRatingStats] = useState<RatingStats | null>(null);
  const [isRating, setIsRating] = useState(false);

  // Comments state
  const [comments, setComments] = useState<ArtifactComment[]>([]);
  const [commentsLoading, setCommentsLoading] = useState(false);

  // Delete confirmation
  const [showDeleteConfirm, setShowDeleteConfirm] = useState(false);
  const [deleting, setDeleting] = useState(false);

  // Track which artifactId we've already incremented view count for
  const viewIncrementedRef = useRef<string | null>(null);

  // Check if current user is the owner
  const isOwner = artifact?.createdBy === user?.username;

  useEffect(() => {
    if (isAuthenticated && viewIncrementedRef.current !== artifactId) {
      viewIncrementedRef.current = artifactId;
      loadArtifact();
      loadVersions();
      loadContent();
      loadRatings();
      loadComments();
    }
  }, [isAuthenticated, artifactId]);

  const loadArtifact = async () => {
    try {
      setLoading(true);
      const data = await apiClient.getArtifact(artifactId);
      setArtifact(data);
      setEditTitle(data.title);
      setEditDescription(data.description || '');
      setEditVisibility(data.visibility);
      setEditTags(data.tags.join(', '));
      setError(null);
    } catch (err) {
      setError('Failed to load artifact');
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  const loadVersions = async () => {
    try {
      const data = await apiClient.getArtifactVersionHistory(artifactId);
      setVersions(data);
    } catch (err) {
      console.error('Failed to load version history:', err);
    }
  };

  const loadContent = async () => {
    try {
      const data = await apiClient.getArtifactContent(artifactId);
      setContent(data);
      setEditContent(data);
    } catch (err) {
      console.error('Failed to load content:', err);
    }
  };

  const loadRatings = async () => {
    try {
      const data = await apiClient.getArtifactRatings(artifactId);
      setRatingStats(data);
    } catch (err) {
      console.error('Failed to load ratings:', err);
    }
  };

  const loadComments = async () => {
    try {
      setCommentsLoading(true);
      const data = await apiClient.getArtifactComments(artifactId);
      setComments(data);
    } catch (err) {
      console.error('Failed to load comments:', err);
    } finally {
      setCommentsLoading(false);
    }
  };

  const handleRatingChange = async (rating: number) => {
    try {
      setIsRating(true);
      const newStats = await apiClient.rateArtifact(artifactId, rating);
      setRatingStats(newStats);
      toast.success('Rating submitted successfully');
    } catch (err) {
      toast.error('Failed to submit rating');
      console.error(err);
    } finally {
      setIsRating(false);
    }
  };

  const handleCreateComment = async (content: string, parentCommentId?: string) => {
    await apiClient.createArtifactComment(artifactId, content, parentCommentId);
  };

  const handleEditComment = async (commentId: string, content: string) => {
    await apiClient.updateArtifactComment(artifactId, commentId, content);
  };

  const handleDeleteComment = async (commentId: string) => {
    await apiClient.deleteArtifactComment(artifactId, commentId);
  };

  const handleSaveMetadata = async () => {
    if (!artifact) return;

    try {
      setSaving(true);
      setError(null);

      const tags = editTags.split(',').map(t => t.trim()).filter(t => t);

      await apiClient.updateArtifact(artifactId, {
        title: editTitle,
        description: editDescription,
        visibility: editVisibility,
        tags,
      });

      setEditing(false);
      toast.success('Artifact updated');
      await loadArtifact();
    } catch (err) {
      setError('Failed to save changes: ' + (err as Error).message);
      toast.error('Failed to save changes: ' + (err as Error).message);
      console.error(err);
    } finally {
      setSaving(false);
    }
  };

  const handleAddVersion = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!newVersionContent.trim()) {
      setError('Please enter content');
      toast.error('Please enter content');
      return;
    }

    try {
      setUploading(true);
      setError(null);

      await apiClient.addArtifactVersion(artifactId, {
        content: newVersionContent,
        changeDescription: newVersionDescription,
      });

      setUploadSuccess(true);
      toast.success('New version added');
      setNewVersionContent('');
      setNewVersionDescription('');

      // Reload data
      await loadArtifact();
      await loadVersions();
      await loadContent();

      // Switch to version history tab
      setActiveTab('versions');

      setTimeout(() => setUploadSuccess(false), 5000);
    } catch (err) {
      setError('Add version failed: ' + (err as Error).message);
      toast.error('Add version failed: ' + (err as Error).message);
      console.error(err);
    } finally {
      setUploading(false);
    }
  };

  const handleDownloadVersion = async (version: ArtifactVersion) => {
    try {
      const content = await apiClient.getArtifactVersionContent(version.versionId);
      const blob = new Blob([content], { type: 'text/markdown' });
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `${artifact?.title.replace(/[^a-z0-9]/gi, '_').toLowerCase()}_v${version.versionNumber}.md`;
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
    if (!artifact || !content) return;
    const blob = new Blob([content], { type: 'text/markdown' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `${artifact.title.replace(/[^a-z0-9]/gi, '_').toLowerCase()}.md`;
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    URL.revokeObjectURL(url);
    toast.success('File downloaded');
  };

  const handleDelete = async () => {
    try {
      setDeleting(true);
      await apiClient.deleteArtifact(artifactId);
      toast.success('Artifact deleted');
      router.push('/artifacts');
    } catch (err) {
      toast.error('Failed to delete artifact');
      console.error(err);
    } finally {
      setDeleting(false);
      setShowDeleteConfirm(false);
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
              Please log in to view this artifact.
            </AlertDescription>
          </Alert>
        </div>
      </div>
    );
  }

  if (!artifact) {
    return (
      <div className="min-h-screen bg-background">
        <div className="container mx-auto py-12 px-4">
          <Alert variant="destructive">
            <AlertDescription>
              Artifact not found.
            </AlertDescription>
          </Alert>
          <Button
            variant="outline"
            onClick={() => router.push('/artifacts')}
            className="mt-4"
          >
            <ArrowLeft className="h-4 w-4 mr-2" />
            Back to Artifacts
          </Button>
        </div>
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
              onClick={() => router.push('/artifacts')}
            >
              <ArrowLeft className="h-4 w-4 mr-2" />
              Back
            </Button>
            <div>
              <div className="flex items-center gap-3">
                <h1 className="text-3xl font-bold">{artifact.title}</h1>
                <Badge className={`${getVisibilityColor(artifact.visibility)} flex items-center gap-1`}>
                  {getVisibilityIcon(artifact.visibility)}
                  {artifact.visibility}
                </Badge>
                <HelpButton slug="artifacts" />
              </div>
              <p className="text-sm text-muted-foreground mt-1">
                Version {artifact.currentVersion?.versionNumber || 1} • Updated {new Date(artifact.updatedAt).toLocaleDateString()}
              </p>
            </div>
          </div>
          <div className="flex gap-2">
            {isOwner && !editing && (
              <>
                <Button variant="outline" onClick={() => setEditing(true)}>
                  <Edit className="h-4 w-4 mr-2" />
                  Edit
                </Button>
                <Button variant="destructive" onClick={() => setShowDeleteConfirm(true)}>
                  <Trash2 className="h-4 w-4 mr-2" />
                  Delete
                </Button>
              </>
            )}
            <Button onClick={handleDownloadCurrent}>
              <Download className="h-4 w-4 mr-2" />
              Download
            </Button>
          </div>
        </div>

        {/* Delete Confirmation */}
        {showDeleteConfirm && (
          <Alert variant="destructive" className="mb-6">
            <AlertDescription className="flex items-center justify-between">
              <span>Are you sure you want to delete this artifact? This action cannot be undone.</span>
              <div className="flex gap-2">
                <Button size="sm" variant="outline" onClick={() => setShowDeleteConfirm(false)}>
                  Cancel
                </Button>
                <Button size="sm" variant="destructive" onClick={handleDelete} disabled={deleting}>
                  {deleting ? 'Deleting...' : 'Delete'}
                </Button>
              </div>
            </AlertDescription>
          </Alert>
        )}

        {error && (
          <Alert variant="destructive" className="mb-6">
            <AlertDescription>{error}</AlertDescription>
          </Alert>
        )}

        {uploadSuccess && (
          <Alert className="mb-6 bg-green-50 border-green-200">
            <AlertDescription className="text-green-800">
              New version added successfully!
            </AlertDescription>
          </Alert>
        )}

        <Tabs value={activeTab} onValueChange={setActiveTab} className="space-y-6">
          <TabsList className="grid w-full grid-cols-4">
            <TabsTrigger value="details">
              <FileText className="h-4 w-4 mr-2" />
              Details
            </TabsTrigger>
            <TabsTrigger value="comments">
              <MessageSquare className="h-4 w-4 mr-2" />
              Comments ({comments.length})
            </TabsTrigger>
            <TabsTrigger value="versions">
              <History className="h-4 w-4 mr-2" />
              Versions ({versions.length})
            </TabsTrigger>
            {isOwner && (
              <TabsTrigger value="add-version">
                <Upload className="h-4 w-4 mr-2" />
                Add Version
              </TabsTrigger>
            )}
          </TabsList>

          {/* Details Tab */}
          <TabsContent value="details" className="space-y-6">
            <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
              {/* Left Column - Info */}
              <Card>
                <CardHeader>
                  <CardTitle>Artifact Information</CardTitle>
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
                          rows={3}
                          value={editDescription}
                          onChange={(e) => setEditDescription(e.target.value)}
                        />
                      </div>
                      <div className="space-y-2">
                        <Label htmlFor="edit-visibility">Visibility</Label>
                        <select
                          id="edit-visibility"
                          className="w-full rounded-md border border-input bg-background px-3 py-2"
                          value={editVisibility}
                          onChange={(e) => setEditVisibility(e.target.value as ArtifactVisibility)}
                        >
                          <option value="PRIVATE">Private</option>
                          <option value="ORGANIZATION">Organization</option>
                          <option value="PUBLIC">Public</option>
                        </select>
                      </div>
                      <div className="space-y-2">
                        <Label htmlFor="edit-tags">Tags (comma-separated)</Label>
                        <Input
                          id="edit-tags"
                          value={editTags}
                          onChange={(e) => setEditTags(e.target.value)}
                          placeholder="e.g., compliance, security, template"
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
                          setEditTitle(artifact.title);
                          setEditDescription(artifact.description || '');
                          setEditVisibility(artifact.visibility);
                          setEditTags(artifact.tags.join(', '));
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
                        <p>{artifact.description || 'No description provided'}</p>
                      </div>

                      {/* Variables */}
                      {artifact.extractedVariables.length > 0 && (
                        <div>
                          <h3 className="text-sm font-medium text-muted-foreground mb-2">Template Variables</h3>
                          <div className="flex flex-wrap gap-2">
                            {artifact.extractedVariables.map((variable) => (
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

                      <div>
                        <h3 className="text-sm font-medium text-muted-foreground mb-2">Tags</h3>
                        <div className="flex flex-wrap gap-2">
                          {artifact.tags.length > 0 ? (
                            artifact.tags.map((tag) => (
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
                            <span>{artifact.createdBy}</span>
                          </div>
                        </div>
                        <div>
                          <h3 className="text-sm font-medium text-muted-foreground mb-1">Created Date</h3>
                          <div className="flex items-center">
                            <Calendar className="h-4 w-4 mr-2" />
                            <span>{new Date(artifact.createdAt).toLocaleString()}</span>
                          </div>
                        </div>
                        <div>
                          <h3 className="text-sm font-medium text-muted-foreground mb-1">Views</h3>
                          <div className="flex items-center">
                            <Eye className="h-4 w-4 mr-2" />
                            <span>{artifact.viewCount}</span>
                          </div>
                        </div>
                        <div>
                          <h3 className="text-sm font-medium text-muted-foreground mb-1">Downloads</h3>
                          <div className="flex items-center">
                            <Download className="h-4 w-4 mr-2" />
                            <span>{artifact.downloadCount}</span>
                          </div>
                        </div>
                        <div>
                          <h3 className="text-sm font-medium text-muted-foreground mb-1">Versions</h3>
                          <div className="flex items-center">
                            <History className="h-4 w-4 mr-2" />
                            <span>{artifact.versionCount || versions.length}</span>
                          </div>
                        </div>
                      </div>

                      {/* Rating Section */}
                      <div className="pt-4 border-t">
                        <h3 className="text-sm font-medium text-muted-foreground mb-2">Your Rating</h3>
                        <div className="flex items-center gap-4">
                          <StarRating
                            rating={ratingStats?.userRating || 0}
                            onRatingChange={handleRatingChange}
                            size="lg"
                          />
                          {isRating && (
                            <span className="text-sm text-muted-foreground">Submitting...</span>
                          )}
                        </div>
                        {ratingStats && ratingStats.totalRatings > 0 && (
                          <div className="mt-2 flex items-center gap-2 text-sm text-muted-foreground">
                            <Star className="h-4 w-4 fill-yellow-400 text-yellow-400" />
                            <span>
                              {ratingStats.averageRating.toFixed(1)} average ({ratingStats.totalRatings}{' '}
                              {ratingStats.totalRatings === 1 ? 'rating' : 'ratings'})
                            </span>
                          </div>
                        )}
                      </div>
                    </div>
                  )}
                </CardContent>
              </Card>

              {/* Right Column - Content Preview */}
              <Card>
                <CardHeader>
                  <CardTitle>Content Preview</CardTitle>
                  <CardDescription>Markdown rendered preview with variable highlighting</CardDescription>
                </CardHeader>
                <CardContent>
                  <MarkdownPreview content={content} height="500px" />
                </CardContent>
              </Card>
            </div>

            {/* Raw Content */}
            <Card>
              <CardHeader>
                <CardTitle>Raw Markdown Content</CardTitle>
              </CardHeader>
              <CardContent>
                <pre className="bg-muted p-4 rounded-md overflow-auto max-h-[400px] text-sm">
                  <code>{content}</code>
                </pre>
              </CardContent>
            </Card>
          </TabsContent>

          {/* Comments Tab */}
          <TabsContent value="comments" className="space-y-6">
            <Card>
              <CardHeader>
                <CardTitle className="flex items-center gap-2">
                  <MessageSquare className="h-5 w-5" />
                  Comments
                </CardTitle>
              </CardHeader>
              <CardContent>
                {commentsLoading ? (
                  <div className="text-center py-8">
                    <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary mx-auto mb-4"></div>
                    <p className="text-muted-foreground">Loading comments...</p>
                  </div>
                ) : (
                  <CommentThread
                    comments={comments as any}
                    itemId={artifactId}
                    currentUsername={user?.username}
                    onCommentAdded={loadComments}
                    onCreateComment={handleCreateComment}
                    onEditComment={handleEditComment}
                    onDeleteComment={handleDeleteComment}
                  />
                )}
              </CardContent>
            </Card>
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
                            {index === 0 && <Badge>Current</Badge>}
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
                              {(version.contentSize / 1024).toFixed(2)} KB
                            </div>
                          </div>
                          {version.changeDescription && (
                            <div className="pt-2 border-t">
                              <p className="text-sm">{version.changeDescription}</p>
                            </div>
                          )}
                          {version.extractedVariables.length > 0 && (
                            <div className="flex flex-wrap gap-1 pt-2">
                              {version.extractedVariables.map((variable) => (
                                <Badge
                                  key={variable}
                                  variant="outline"
                                  className="text-xs bg-purple-50 text-purple-700 border-purple-200"
                                >
                                  {`{{ ${variable} }}`}
                                </Badge>
                              ))}
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

          {/* Add New Version Tab */}
          {isOwner && (
            <TabsContent value="add-version" className="space-y-6">
              <Card>
                <CardHeader>
                  <CardTitle>Add New Version</CardTitle>
                  <CardDescription>
                    Create a new version of this artifact
                  </CardDescription>
                </CardHeader>
                <CardContent>
                  <form onSubmit={handleAddVersion} className="space-y-4">
                    <div className="space-y-2">
                      <div className="flex items-center justify-between">
                        <Label htmlFor="new-content">Content (Markdown) *</Label>
                        <Button
                          type="button"
                          variant="outline"
                          size="sm"
                          onClick={() => {
                            setNewVersionContent(content);
                            toast.success('Current version copied');
                          }}
                          disabled={!content}
                        >
                          <FileText className="h-4 w-4 mr-2" />
                          Copy Current Version
                        </Button>
                      </div>
                      <textarea
                        id="new-content"
                        className="w-full rounded-md border border-input bg-background px-3 py-2 font-mono text-sm"
                        rows={15}
                        value={newVersionContent}
                        onChange={(e) => setNewVersionContent(e.target.value)}
                        required
                        placeholder="Enter your Markdown content here..."
                      />
                    </div>
                    <div className="space-y-2">
                      <Label htmlFor="change-description">Change Description</Label>
                      <textarea
                        id="change-description"
                        className="w-full rounded-md border border-input bg-background px-3 py-2"
                        rows={3}
                        value={newVersionDescription}
                        onChange={(e) => setNewVersionDescription(e.target.value)}
                        placeholder="Describe what changed in this version..."
                      />
                    </div>
                    <Button type="submit" disabled={uploading} className="w-full">
                      {uploading ? (
                        <>
                          <div className="animate-spin rounded-full h-4 w-4 border-b-2 border-white mr-2" />
                          Adding...
                        </>
                      ) : (
                        <>
                          <Upload className="h-4 w-4 mr-2" />
                          Add New Version
                        </>
                      )}
                    </Button>
                  </form>
                </CardContent>
              </Card>
            </TabsContent>
          )}
        </Tabs>
      </div>
    </div>
  );
}
