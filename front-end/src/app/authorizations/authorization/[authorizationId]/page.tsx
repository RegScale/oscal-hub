'use client';

import { useState, useEffect } from 'react';
import { useParams, useRouter } from 'next/navigation';
import { Card } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Alert, AlertDescription } from '@/components/ui/alert';
import { Label } from '@/components/ui/label';
import { Input } from '@/components/ui/input';
import { Textarea } from '@/components/ui/textarea';
import { Badge } from '@/components/ui/badge';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import {
  ArrowLeft,
  ShieldCheck,
  Pencil,
  Save,
  X,
  Download,
  Eye,
  Plus,
  Trash2,
  Loader2,
  CheckCircle2,
  ChevronDown,
  ChevronUp,
  RefreshCcw
} from 'lucide-react';
import { apiClient } from '@/lib/api-client';
import type { AuthorizationResponse, LibraryItem } from '@/types/oscal';
import { useAuth } from '@/contexts/AuthContext';
import { Footer } from '@/components/Footer';
import { MarkdownPreview } from '@/components/markdown-preview';
import { toast } from 'sonner';

export default function AuthorizationDetailPage() {
  const params = useParams();
  const router = useRouter();
  const { isAuthenticated, isLoading: authLoading, user } = useAuth();

  const authorizationId = params.authorizationId as string;

  const [authorization, setAuthorization] = useState<AuthorizationResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [isEditing, setIsEditing] = useState(false);
  const [saving, setSaving] = useState(false);

  // SSP and SAR items for links
  const [sspItems, setSspItems] = useState<LibraryItem[]>([]);
  const [sarItems, setSarItems] = useState<LibraryItem[]>([]);

  // Edit form state
  const [editName, setEditName] = useState('');
  const [editDateAuthorized, setEditDateAuthorized] = useState('');
  const [editDateExpired, setEditDateExpired] = useState('');
  const [editSystemOwner, setEditSystemOwner] = useState('');
  const [editSecurityManager, setEditSecurityManager] = useState('');
  const [editAuthorizingOfficial, setEditAuthorizingOfficial] = useState('');
  const [editConditions, setEditConditions] = useState<Array<{
    id?: number;
    condition: string;
    conditionType: 'MANDATORY' | 'RECOMMENDED';
    dueDate?: string;
  }>>([]);

  // Digital signature state
  const [showCertDetails, setShowCertDetails] = useState(false);
  const [verifying, setVerifying] = useState(false);

  useEffect(() => {
    if (isAuthenticated && authorizationId) {
      loadAuthorization();
      loadSspItems();
      loadSarItems();
    }
  }, [isAuthenticated, authorizationId]);

  const loadAuthorization = async () => {
    try {
      setLoading(true);
      setError(null);

      const data = await apiClient.getAuthorization(parseInt(authorizationId));
      setAuthorization(data);

      // Initialize edit form
      setEditName(data.name);
      setEditDateAuthorized(data.dateAuthorized || '');
      setEditDateExpired(data.dateExpired || '');
      setEditSystemOwner(data.systemOwner || '');
      setEditSecurityManager(data.securityManager || '');
      setEditAuthorizingOfficial(data.authorizingOfficial || '');
      setEditConditions(data.conditions || []);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load authorization');
      console.error('Error loading authorization:', err);
    } finally {
      setLoading(false);
    }
  };

  const loadSspItems = async () => {
    try {
      const savedFiles = await apiClient.getSavedFiles();
      const sspSavedFiles: LibraryItem[] = savedFiles
        .filter(file => file.modelType === 'system-security-plan')
        .map(file => ({
          itemId: file.id,
          title: file.fileName || 'Untitled SSP',
          description: `Uploaded SSP (${file.format.toUpperCase()})`,
          oscalType: 'system-security-plan',
          format: file.format,
          fileSize: file.fileSize,
          tags: [],
          createdBy: '',
          createdAt: file.uploadedAt,
          updatedAt: file.uploadedAt,
          lastUpdatedAt: file.uploadedAt,
          lastUpdatedBy: '',
          downloadCount: 0,
          viewCount: 0,
          versionCount: 1,
          versions: [],
        }));
      setSspItems(sspSavedFiles);
    } catch (err) {
      console.error('Failed to load SSP items:', err);
    }
  };

  const loadSarItems = async () => {
    try {
      const savedFiles = await apiClient.getSavedFiles();
      const sarSavedFiles: LibraryItem[] = savedFiles
        .filter(file => file.modelType === 'assessment-results')
        .map(file => ({
          itemId: file.id,
          title: file.fileName || 'Untitled SAR',
          description: `Uploaded SAR (${file.format.toUpperCase()})`,
          oscalType: 'assessment-results',
          format: file.format,
          fileSize: file.fileSize,
          tags: [],
          createdBy: '',
          createdAt: file.uploadedAt,
          updatedAt: file.uploadedAt,
          lastUpdatedAt: file.uploadedAt,
          lastUpdatedBy: '',
          downloadCount: 0,
          viewCount: 0,
          versionCount: 1,
          versions: [],
        }));
      setSarItems(sarSavedFiles);
    } catch (err) {
      console.error('Failed to load SAR items:', err);
    }
  };

  const handleSaveEdit = async () => {
    if (!authorization) return;

    try {
      setSaving(true);

      const validConditions = editConditions
        .filter(c => c.condition.trim() !== '')
        .map(c => ({
          condition: c.condition,
          conditionType: c.conditionType,
          ...(c.dueDate && c.dueDate.trim() !== '' ? { dueDate: c.dueDate } : {})
        }));

      await apiClient.updateAuthorization(authorization.id, {
        name: editName,
        dateAuthorized: editDateAuthorized,
        dateExpired: editDateExpired,
        systemOwner: editSystemOwner,
        securityManager: editSecurityManager,
        authorizingOfficial: editAuthorizingOfficial,
        variableValues: authorization.variableValues,
        conditions: validConditions
      });

      toast.success('Authorization updated successfully');
      await loadAuthorization();
      setIsEditing(false);
    } catch (err) {
      console.error('Failed to update authorization:', err);
      toast.error('Failed to update authorization');
    } finally {
      setSaving(false);
    }
  };

  const handleCancelEdit = () => {
    if (authorization) {
      setEditName(authorization.name);
      setEditDateAuthorized(authorization.dateAuthorized || '');
      setEditDateExpired(authorization.dateExpired || '');
      setEditSystemOwner(authorization.systemOwner || '');
      setEditSecurityManager(authorization.securityManager || '');
      setEditAuthorizingOfficial(authorization.authorizingOfficial || '');
      setEditConditions(authorization.conditions || []);
    }
    setIsEditing(false);
  };

  const handleVerifySignature = async () => {
    if (!authorization) return;

    try {
      setVerifying(true);
      await apiClient.verifySignature(authorization.id);
      toast.success('Signature verified successfully');
      await loadAuthorization(); // Reload to get updated verification status
    } catch (err) {
      console.error('Failed to verify signature:', err);
      toast.error('Signature verification failed');
    } finally {
      setVerifying(false);
    }
  };

  if (authLoading || loading) {
    return (
      <div className="min-h-screen bg-background flex items-center justify-center">
        <div className="text-center">
          <Loader2 className="h-12 w-12 animate-spin text-primary mx-auto mb-4" />
          <p className="text-muted-foreground">Loading authorization...</p>
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
              Please log in to access authorization details.
            </AlertDescription>
          </Alert>
        </div>
        <Footer />
      </div>
    );
  }

  if (error || !authorization) {
    return (
      <div className="min-h-screen bg-background">
        <div className="container mx-auto py-12 px-4">
          <Button
            variant="ghost"
            onClick={() => router.push('/authorizations')}
            className="inline-flex items-center gap-2 text-sm text-muted-foreground hover:text-foreground mb-4 transition-colors px-0"
          >
            <ArrowLeft className="h-4 w-4" />
            Back to Authorizations
          </Button>

          <Alert variant="destructive">
            <AlertDescription>
              {error || 'Authorization not found'}
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
        <div className="mb-8">
          <Button
            variant="ghost"
            onClick={() => router.push('/authorizations')}
            className="inline-flex items-center gap-2 text-sm text-muted-foreground hover:text-foreground mb-4 transition-colors px-0"
          >
            <ArrowLeft className="h-4 w-4" />
            Back to Authorizations
          </Button>
          <div className="flex items-center">
            <ShieldCheck className="h-10 w-10 text-primary mr-4" />
            <div>
              <h1 className="text-4xl font-bold">{authorization.name}</h1>
              <p className="text-muted-foreground mt-2">
                Authorized by {authorization.authorizedBy} on{' '}
                {new Date(authorization.authorizedAt).toLocaleDateString()}
              </p>
            </div>
          </div>
        </div>

        <Card className="p-6">
          <div className="flex items-center justify-between mb-6">
            <h2 className="text-2xl font-bold">Authorization Details</h2>
            <div className="flex gap-2">
              {!isEditing && (
                <Button
                  variant="outline"
                  onClick={() => setIsEditing(true)}
                  disabled={authorization.authorizedBy !== user?.username}
                >
                  <Pencil className="h-4 w-4 mr-2" />
                  Edit
                </Button>
              )}
              {isEditing && (
                <>
                  <Button
                    variant="outline"
                    onClick={handleCancelEdit}
                    disabled={saving}
                  >
                    <X className="h-4 w-4 mr-2" />
                    Cancel
                  </Button>
                  <Button
                    onClick={handleSaveEdit}
                    disabled={saving}
                  >
                    <Save className="h-4 w-4 mr-2" />
                    {saving ? 'Saving...' : 'Save'}
                  </Button>
                </>
              )}
            </div>
          </div>

          <div className="space-y-6">
            {/* Authorization Metadata */}
            <div className="space-y-4">
              <h3 className="text-lg font-semibold border-b pb-2">Authorization Details</h3>

              {isEditing ? (
                <div className="space-y-4">
                  <div>
                    <Label htmlFor="edit-name">Authorization Name</Label>
                    <Input
                      id="edit-name"
                      value={editName}
                      onChange={(e) => setEditName(e.target.value)}
                      placeholder="Enter authorization name"
                    />
                  </div>

                  <div className="grid gap-4 md:grid-cols-2">
                    <div>
                      <Label htmlFor="edit-date-authorized">Date Authorized</Label>
                      <Input
                        id="edit-date-authorized"
                        type="date"
                        value={editDateAuthorized}
                        onChange={(e) => setEditDateAuthorized(e.target.value)}
                      />
                    </div>
                    <div>
                      <Label htmlFor="edit-date-expired">Date Expired</Label>
                      <Input
                        id="edit-date-expired"
                        type="date"
                        value={editDateExpired}
                        onChange={(e) => setEditDateExpired(e.target.value)}
                      />
                    </div>
                  </div>

                  <div className="grid gap-4 md:grid-cols-2">
                    <div>
                      <Label htmlFor="edit-system-owner">System Owner</Label>
                      <Input
                        id="edit-system-owner"
                        value={editSystemOwner}
                        onChange={(e) => setEditSystemOwner(e.target.value)}
                        placeholder="Enter system owner"
                      />
                    </div>
                    <div>
                      <Label htmlFor="edit-security-manager">Security Manager</Label>
                      <Input
                        id="edit-security-manager"
                        value={editSecurityManager}
                        onChange={(e) => setEditSecurityManager(e.target.value)}
                        placeholder="Enter security manager"
                      />
                    </div>
                  </div>

                  <div>
                    <Label htmlFor="edit-authorizing-official">Authorizing Official</Label>
                    <Input
                      id="edit-authorizing-official"
                      value={editAuthorizingOfficial}
                      onChange={(e) => setEditAuthorizingOfficial(e.target.value)}
                      placeholder="Enter authorizing official"
                    />
                  </div>
                </div>
              ) : (
                <div className="grid gap-4 md:grid-cols-2">
                  {(authorization.dateAuthorized || authorization.dateExpired) && (
                    <Card className="p-4 bg-slate-800 border-slate-700">
                      <h4 className="font-semibold mb-3 text-sm text-slate-300">Authorization Dates</h4>
                      <div className="space-y-2">
                        {authorization.dateAuthorized && (
                          <div>
                            <Label className="text-xs text-slate-400">Date Authorized</Label>
                            <p className="font-medium">{new Date(authorization.dateAuthorized).toLocaleDateString('en-US', {
                              year: 'numeric',
                              month: 'long',
                              day: 'numeric'
                            })}</p>
                          </div>
                        )}
                        {authorization.dateExpired && (
                          <div>
                            <Label className="text-xs text-slate-400">Date Expired</Label>
                            <p className="font-medium">{new Date(authorization.dateExpired).toLocaleDateString('en-US', {
                              year: 'numeric',
                              month: 'long',
                              day: 'numeric'
                            })}</p>
                          </div>
                        )}
                      </div>
                    </Card>
                  )}

                  {(authorization.systemOwner || authorization.securityManager || authorization.authorizingOfficial) && (
                    <Card className="p-4 bg-slate-800 border-slate-700">
                      <h4 className="font-semibold mb-3 text-sm text-slate-300">Stakeholders</h4>
                      <div className="space-y-2">
                        {authorization.systemOwner && (
                          <div>
                            <Label className="text-xs text-slate-400">System Owner</Label>
                            <p className="font-medium">{authorization.systemOwner}</p>
                          </div>
                        )}
                        {authorization.securityManager && (
                          <div>
                            <Label className="text-xs text-slate-400">Security Manager</Label>
                            <p className="font-medium">{authorization.securityManager}</p>
                          </div>
                        )}
                        {authorization.authorizingOfficial && (
                          <div>
                            <Label className="text-xs text-slate-400">Authorizing Official</Label>
                            <p className="font-medium">{authorization.authorizingOfficial}</p>
                          </div>
                        )}
                      </div>
                    </Card>
                  )}
                </div>
              )}
            </div>

            {/* Digital Signature Section */}
            {authorization.signerCertificate && (
              <div className="space-y-4">
                <h3 className="text-lg font-semibold border-b pb-2">Digital Signature</h3>

                <Card className="p-4 bg-blue-900/10 border-blue-800">
                  <div className="flex items-start gap-4">
                    <div className="flex-shrink-0">
                      <div className="h-12 w-12 rounded-full bg-blue-600 flex items-center justify-center">
                        <CheckCircle2 className="h-6 w-6 text-white" />
                      </div>
                    </div>

                    <div className="flex-1">
                      <div className="flex items-center gap-2 mb-2">
                        <h4 className="font-semibold text-lg">Digitally Signed</h4>
                        {authorization.certificateVerified && (
                          <Badge className="bg-green-600 text-white">Verified</Badge>
                        )}
                      </div>

                      <div className="grid gap-3 md:grid-cols-2 text-sm">
                        <div>
                          <Label className="text-xs text-slate-400">Signer</Label>
                          <p className="font-medium">{authorization.signerCommonName || 'Unknown'}</p>
                        </div>

                        {authorization.signerEmail && (
                          <div>
                            <Label className="text-xs text-slate-400">Email</Label>
                            <p className="font-medium">{authorization.signerEmail}</p>
                          </div>
                        )}

                        {authorization.signerEdipi && (
                          <div>
                            <Label className="text-xs text-slate-400">EDIPI</Label>
                            <p className="font-medium font-mono">{authorization.signerEdipi}</p>
                          </div>
                        )}

                        {authorization.signatureTimestamp && (
                          <div>
                            <Label className="text-xs text-slate-400">Signed On</Label>
                            <p className="font-medium">
                              {new Date(authorization.signatureTimestamp).toLocaleDateString('en-US', {
                                year: 'numeric',
                                month: 'long',
                                day: 'numeric',
                                hour: '2-digit',
                                minute: '2-digit'
                              })}
                            </p>
                          </div>
                        )}
                      </div>

                      <div className="mt-4 flex gap-2">
                        <Button
                          variant="outline"
                          size="sm"
                          onClick={() => setShowCertDetails(!showCertDetails)}
                        >
                          {showCertDetails ? (
                            <>
                              <ChevronUp className="h-4 w-4 mr-2" />
                              Hide Certificate Details
                            </>
                          ) : (
                            <>
                              <ChevronDown className="h-4 w-4 mr-2" />
                              Show Certificate Details
                            </>
                          )}
                        </Button>

                        <Button
                          variant="outline"
                          size="sm"
                          onClick={handleVerifySignature}
                          disabled={verifying}
                        >
                          {verifying ? (
                            <>
                              <Loader2 className="h-4 w-4 mr-2 animate-spin" />
                              Verifying...
                            </>
                          ) : (
                            <>
                              <RefreshCcw className="h-4 w-4 mr-2" />
                              Re-verify
                            </>
                          )}
                        </Button>
                      </div>

                      {showCertDetails && (
                        <div className="mt-4 p-4 bg-slate-900/50 rounded border border-slate-700">
                          <h5 className="font-semibold mb-3 text-sm">Certificate Information</h5>
                          <div className="grid gap-3 text-xs">
                            {authorization.certificateIssuer && (
                              <div>
                                <Label className="text-xs text-slate-400">Issuer</Label>
                                <p className="font-mono text-xs break-all">{authorization.certificateIssuer}</p>
                              </div>
                            )}

                            {authorization.certificateSerial && (
                              <div>
                                <Label className="text-xs text-slate-400">Serial Number</Label>
                                <p className="font-mono text-xs">{authorization.certificateSerial}</p>
                              </div>
                            )}

                            <div className="grid gap-3 md:grid-cols-2">
                              {authorization.certificateNotBefore && (
                                <div>
                                  <Label className="text-xs text-slate-400">Valid From</Label>
                                  <p className="text-xs">
                                    {new Date(authorization.certificateNotBefore).toLocaleDateString('en-US', {
                                      year: 'numeric',
                                      month: 'short',
                                      day: 'numeric'
                                    })}
                                  </p>
                                </div>
                              )}

                              {authorization.certificateNotAfter && (
                                <div>
                                  <Label className="text-xs text-slate-400">Valid Until</Label>
                                  <p className="text-xs">
                                    {new Date(authorization.certificateNotAfter).toLocaleDateString('en-US', {
                                      year: 'numeric',
                                      month: 'short',
                                      day: 'numeric'
                                    })}
                                  </p>
                                </div>
                              )}
                            </div>

                            {authorization.certificateVerified !== null && (
                              <div>
                                <Label className="text-xs text-slate-400">Verification Status</Label>
                                <div className="flex items-center gap-2 mt-1">
                                  {authorization.certificateVerified ? (
                                    <>
                                      <CheckCircle2 className="h-4 w-4 text-green-500" />
                                      <span className="text-xs text-green-500">Verified</span>
                                    </>
                                  ) : (
                                    <>
                                      <X className="h-4 w-4 text-red-500" />
                                      <span className="text-xs text-red-500">Not Verified</span>
                                    </>
                                  )}
                                  {authorization.certificateVerificationDate && (
                                    <span className="text-xs text-slate-400 ml-2">
                                      on {new Date(authorization.certificateVerificationDate).toLocaleDateString()}
                                    </span>
                                  )}
                                </div>
                                {authorization.certificateVerificationNotes && (
                                  <p className="text-xs text-slate-400 mt-1">{authorization.certificateVerificationNotes}</p>
                                )}
                              </div>
                            )}

                            {authorization.documentHash && (
                              <div>
                                <Label className="text-xs text-slate-400">Document Hash (SHA-256)</Label>
                                <p className="font-mono text-xs break-all">{authorization.documentHash}</p>
                              </div>
                            )}
                          </div>
                        </div>
                      )}
                    </div>
                  </div>
                </Card>
              </div>
            )}

            {/* Conditions of Approval */}
            <div className="space-y-4">
              <div className="flex items-center justify-between border-b pb-2">
                <h3 className="text-lg font-semibold">Conditions of Approval</h3>
                {isEditing && (
                  <Button
                    onClick={() => setEditConditions([...editConditions, {
                      condition: '',
                      conditionType: 'MANDATORY',
                      dueDate: ''
                    }])}
                    className="bg-primary hover:bg-primary/90"
                  >
                    <Plus className="h-4 w-4 mr-2" />
                    Add Condition
                  </Button>
                )}
              </div>

              {isEditing ? (
                <div className="space-y-3">
                  {editConditions.length === 0 ? (
                    <p className="text-sm text-slate-400">No conditions. Click &quot;Add Condition&quot; to add one.</p>
                  ) : (
                    editConditions.map((condition, index) => (
                      <Card
                        key={index}
                        className={`p-4 ${
                          condition.conditionType === 'MANDATORY'
                            ? 'bg-red-900/10 border-red-800'
                            : 'bg-yellow-900/10 border-yellow-800'
                        }`}
                      >
                        <div className="space-y-3">
                          <div className="flex gap-2">
                            <div className={condition.conditionType === 'MANDATORY' ? 'flex-1' : 'flex-[2]'}>
                              <Label htmlFor={`condition-type-${index}`}>Type</Label>
                              <Select
                                value={condition.conditionType}
                                onValueChange={(value: 'MANDATORY' | 'RECOMMENDED') => {
                                  const updated = [...editConditions];
                                  updated[index].conditionType = value;
                                  if (value === 'RECOMMENDED') {
                                    updated[index].dueDate = '';
                                  }
                                  setEditConditions(updated);
                                }}
                              >
                                <SelectTrigger id={`condition-type-${index}`}>
                                  <SelectValue />
                                </SelectTrigger>
                                <SelectContent>
                                  <SelectItem value="MANDATORY">MANDATORY</SelectItem>
                                  <SelectItem value="RECOMMENDED">RECOMMENDED</SelectItem>
                                </SelectContent>
                              </Select>
                            </div>
                            {condition.conditionType === 'MANDATORY' && (
                              <div className="flex-1">
                                <Label htmlFor={`condition-due-${index}`}>Due Date</Label>
                                <Input
                                  id={`condition-due-${index}`}
                                  type="date"
                                  value={condition.dueDate || ''}
                                  onChange={(e) => {
                                    const updated = [...editConditions];
                                    updated[index].dueDate = e.target.value;
                                    setEditConditions(updated);
                                  }}
                                />
                              </div>
                            )}
                            <div className="flex items-end">
                              <Button
                                variant="destructive"
                                size="icon"
                                onClick={() => {
                                  const updated = editConditions.filter((_, i) => i !== index);
                                  setEditConditions(updated);
                                }}
                              >
                                <Trash2 className="h-4 w-4" />
                              </Button>
                            </div>
                          </div>
                          <div>
                            <Label htmlFor={`condition-text-${index}`}>Condition</Label>
                            <Textarea
                              id={`condition-text-${index}`}
                              value={condition.condition}
                              onChange={(e) => {
                                const updated = [...editConditions];
                                updated[index].condition = e.target.value;
                                setEditConditions(updated);
                              }}
                              placeholder="Enter condition details"
                              rows={3}
                            />
                          </div>
                        </div>
                      </Card>
                    ))
                  )}
                </div>
              ) : (
                authorization.conditions && authorization.conditions.length > 0 ? (
                  <div className="space-y-2">
                    {authorization.conditions.map((condition, index) => (
                      <Card
                        key={condition.id}
                        className={`p-4 ${
                          condition.conditionType === 'MANDATORY'
                            ? 'bg-red-900/10 border-red-800'
                            : 'bg-yellow-900/10 border-yellow-800'
                        }`}
                      >
                        <div className="flex items-start gap-3">
                          <Badge
                            variant={condition.conditionType === 'MANDATORY' ? 'destructive' : 'default'}
                            className={
                              condition.conditionType === 'MANDATORY'
                                ? 'bg-red-600 text-white mt-0.5'
                                : 'bg-yellow-600 text-white mt-0.5'
                            }
                          >
                            {condition.conditionType}
                          </Badge>
                          <div className="flex-1">
                            <p className="text-sm">{condition.condition}</p>
                            {condition.dueDate && (
                              <p className="text-xs text-slate-400 mt-1">
                                Due: {new Date(condition.dueDate).toLocaleDateString()}
                              </p>
                            )}
                          </div>
                        </div>
                      </Card>
                    ))}
                  </div>
                ) : (
                  <p className="text-sm text-slate-400">No conditions of approval</p>
                )
              )}
            </div>

            {/* Authorization Document */}
            <div className="space-y-2">
              <h3 className="text-lg font-semibold border-b pb-2">Authorization Document</h3>
              <p className="text-xs text-slate-400 mb-2">Scroll to view the complete document</p>
              <MarkdownPreview
                content={authorization.completedContent}
                height="600px"
              />
            </div>
          </div>
        </Card>
      </div>

      <Footer />
    </div>
  );
}
