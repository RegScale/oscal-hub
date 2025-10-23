'use client';

import { useState, useEffect } from 'react';
import { useRouter } from 'next/navigation';
import { Card } from '@/components/ui/card';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';
import { Alert, AlertDescription } from '@/components/ui/alert';
import { ShieldCheck, FileText, CheckCircle, ArrowLeft, Download, Eye, Calendar, AlertTriangle, Clock, CheckCircle2, Pencil, Save, X, Plus, Trash2 } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Label } from '@/components/ui/label';
import { Input } from '@/components/ui/input';
import { Badge } from '@/components/ui/badge';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { Textarea } from '@/components/ui/textarea';
import { TemplateEditor } from '@/components/template-editor';
import { TemplateList } from '@/components/template-list';
import { AuthorizationWizard } from '@/components/authorization-wizard';
import { AuthorizationList } from '@/components/authorization-list';
import { MarkdownPreview } from '@/components/markdown-preview';
import { apiClient } from '@/lib/api-client';
import type {
  AuthorizationTemplateResponse,
  AuthorizationResponse,
  LibraryItem,
} from '@/types/oscal';
import { useAuth } from '@/contexts/AuthContext';
import { Footer } from '@/components/Footer';
import { toast } from 'sonner';

type View = 'list-templates' | 'create-template' | 'edit-template' | 'view-template' | 'list-authorizations' | 'create-authorization' | 'view-authorization';

export default function AuthorizationsPage() {
  const router = useRouter();
  const { isAuthenticated, isLoading, user } = useAuth();
  const [activeTab, setActiveTab] = useState('authorizations');
  const [view, setView] = useState<View>('list-authorizations');

  // Templates state
  const [templates, setTemplates] = useState<AuthorizationTemplateResponse[]>([]);
  const [selectedTemplate, setSelectedTemplate] = useState<AuthorizationTemplateResponse | null>(null);
  const [loadingTemplates, setLoadingTemplates] = useState(false);
  const [savingTemplate, setSavingTemplate] = useState(false);

  // Authorizations state
  const [authorizations, setAuthorizations] = useState<AuthorizationResponse[]>([]);
  const [selectedAuthorization, setSelectedAuthorization] = useState<AuthorizationResponse | null>(null);
  const [loadingAuthorizations, setLoadingAuthorizations] = useState(false);
  const [savingAuthorization, setSavingAuthorization] = useState(false);
  const [isEditingAuthorization, setIsEditingAuthorization] = useState(false);

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

  // SSP and SAR items for authorization creation
  const [sspItems, setSspItems] = useState<LibraryItem[]>([]);
  const [sarItems, setSarItems] = useState<LibraryItem[]>([]);

  // Calendar view state
  const [calendarSearchTerm, setCalendarSearchTerm] = useState('');
  const [calendarStartDate, setCalendarStartDate] = useState('');
  const [calendarEndDate, setCalendarEndDate] = useState('');

  useEffect(() => {
    if (isAuthenticated) {
      loadTemplates();
      loadAuthorizations();
      loadSspItems();
      loadSarItems();
    }
  }, [isAuthenticated]);

  const loadTemplates = async () => {
    try {
      setLoadingTemplates(true);
      const data = await apiClient.getAllAuthorizationTemplates();
      setTemplates(data);
    } catch (err) {
      console.error('Failed to load templates:', err);
      toast.error('Failed to load templates');
    } finally {
      setLoadingTemplates(false);
    }
  };

  const loadAuthorizations = async () => {
    try {
      setLoadingAuthorizations(true);
      const data = await apiClient.getAllAuthorizations();
      setAuthorizations(data);
    } catch (err) {
      console.error('Failed to load authorizations:', err);
      toast.error('Failed to load authorizations');
    } finally {
      setLoadingAuthorizations(false);
    }
  };

  const loadSspItems = async () => {
    try {
      // Load saved files that are SSPs (from validation/upload)
      const savedFiles = await apiClient.getSavedFiles();

      // Convert saved SSP files to LibraryItem format
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
      toast.error('Failed to load SSP items');
    }
  };

  const loadSarItems = async () => {
    try {
      // Load saved files that are SARs (Security Assessment Results)
      const savedFiles = await apiClient.getSavedFiles();

      // Convert saved SAR files to LibraryItem format
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
      toast.error('Failed to load SAR items');
    }
  };

  const handleCreateTemplate = async (name: string, content: string) => {
    try {
      setSavingTemplate(true);
      await apiClient.createAuthorizationTemplate({ name, content });
      toast.success('Template created successfully');
      await loadTemplates();
      setView('list-templates');
    } catch (err) {
      console.error('Failed to create template:', err);
      toast.error('Failed to create template');
    } finally {
      setSavingTemplate(false);
    }
  };

  const handleUpdateTemplate = async (name: string, content: string) => {
    if (!selectedTemplate) return;

    try {
      setSavingTemplate(true);
      await apiClient.updateAuthorizationTemplate(selectedTemplate.id, { name, content });
      toast.success('Template updated successfully');
      await loadTemplates();
      setView('list-templates');
      setSelectedTemplate(null);
    } catch (err) {
      console.error('Failed to update template:', err);
      toast.error('Failed to update template');
    } finally {
      setSavingTemplate(false);
    }
  };

  const handleDeleteTemplate = async (templateId: number) => {
    try {
      const success = await apiClient.deleteAuthorizationTemplate(templateId);
      if (success) {
        toast.success('Template deleted successfully');
        await loadTemplates();
      } else {
        toast.error('You do not have permission to delete this template. Only the creator can delete it.');
      }
    } catch (err) {
      console.error('Failed to delete template:', err);
      toast.error('You do not have permission to delete this template. Only the creator can delete it.');
    }
  };

  const handleCreateAuthorization = async (data: {
    name: string;
    sspItemId: string;
    sarItemId?: string;
    templateId: number;
    variableValues: Record<string, string>;
    dateAuthorized: string;
    dateExpired: string;
    systemOwner: string;
    securityManager: string;
    authorizingOfficial: string;
    editedContent: string;
    conditions?: Array<{
      condition: string;
      conditionType: 'MANDATORY' | 'RECOMMENDED';
      dueDate?: string;
    }>;
  }) => {
    try {
      setSavingAuthorization(true);
      await apiClient.createAuthorization(data);
      toast.success('Authorization created successfully');
      await loadAuthorizations();
      setView('list-authorizations');
      setActiveTab('authorizations');
    } catch (err) {
      console.error('Failed to create authorization:', err);
      toast.error('Failed to create authorization');
    } finally {
      setSavingAuthorization(false);
    }
  };

  const handleDeleteAuthorization = async (authorizationId: number) => {
    try {
      const success = await apiClient.deleteAuthorization(authorizationId);
      if (success) {
        toast.success('Authorization deleted successfully');
        await loadAuthorizations();
      } else {
        toast.error('You do not have permission to delete this authorization. Only the creator can delete it.');
      }
    } catch (err) {
      console.error('Failed to delete authorization:', err);
      toast.error('You do not have permission to delete this authorization. Only the creator can delete it.');
    }
  };

  const handleStartEdit = () => {
    if (!selectedAuthorization) return;

    setEditName(selectedAuthorization.name);
    setEditDateAuthorized(selectedAuthorization.dateAuthorized || '');
    setEditDateExpired(selectedAuthorization.dateExpired || '');
    setEditSystemOwner(selectedAuthorization.systemOwner || '');
    setEditSecurityManager(selectedAuthorization.securityManager || '');
    setEditAuthorizingOfficial(selectedAuthorization.authorizingOfficial || '');
    setEditConditions(selectedAuthorization.conditions || []);
    setIsEditingAuthorization(true);
  };

  const handleCancelEdit = () => {
    setIsEditingAuthorization(false);
    setEditName('');
    setEditDateAuthorized('');
    setEditDateExpired('');
    setEditSystemOwner('');
    setEditSecurityManager('');
    setEditAuthorizingOfficial('');
    setEditConditions([]);
  };

  const handleSaveEdit = async () => {
    if (!selectedAuthorization) return;

    try {
      setSavingAuthorization(true);

      // Filter out empty conditions and clean up data
      const validConditions = editConditions
        .filter(c => c.condition.trim() !== '') // Only include conditions with text
        .map(c => ({
          condition: c.condition,
          conditionType: c.conditionType,
          // Only include dueDate if it's not empty
          ...(c.dueDate && c.dueDate.trim() !== '' ? { dueDate: c.dueDate } : {})
        }));

      await apiClient.updateAuthorization(selectedAuthorization.id, {
        name: editName,
        dateAuthorized: editDateAuthorized,
        dateExpired: editDateExpired,
        systemOwner: editSystemOwner,
        securityManager: editSecurityManager,
        authorizingOfficial: editAuthorizingOfficial,
        variableValues: selectedAuthorization.variableValues,
        conditions: validConditions
      });

      toast.success('Authorization updated successfully');
      await loadAuthorizations();

      // Reload the selected authorization to show updated data
      const updated = await apiClient.getAuthorization(selectedAuthorization.id);
      setSelectedAuthorization(updated);
      setIsEditingAuthorization(false);
    } catch (err) {
      console.error('Failed to update authorization:', err);
      toast.error('Failed to update authorization');
    } finally {
      setSavingAuthorization(false);
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
              Please log in to access authorizations.
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
            onClick={() => router.push('/')}
            className="inline-flex items-center gap-2 text-sm text-muted-foreground hover:text-foreground mb-4 transition-colors px-0"
          >
            <ArrowLeft className="h-4 w-4" />
            Back to Dashboard
          </Button>
          <div className="flex items-center">
            <ShieldCheck className="h-10 w-10 text-primary mr-4" />
            <div>
              <h1 className="text-4xl font-bold">System Authorizations</h1>
              <p className="text-muted-foreground mt-2">
                Create and manage system authorization documents
              </p>
            </div>
          </div>
        </div>

        <Tabs value={activeTab} onValueChange={(value) => {
          setActiveTab(value);
          if (value === 'templates') {
            setView('list-templates');
          } else if (value === 'authorizations') {
            setView('list-authorizations');
          }
        }} className="space-y-6">
          <TabsList className="grid w-full grid-cols-3">
            <TabsTrigger value="authorizations">
              <CheckCircle className="h-4 w-4 mr-2" />
              Authorizations
            </TabsTrigger>
            <TabsTrigger value="calendar">
              <Calendar className="h-4 w-4 mr-2" />
              Calendar
            </TabsTrigger>
            <TabsTrigger value="templates">
              <FileText className="h-4 w-4 mr-2" />
              Templates
            </TabsTrigger>
          </TabsList>

          {/* Authorizations Tab */}
          <TabsContent value="authorizations" className="space-y-6">
            {view === 'list-authorizations' && (
              <AuthorizationList
                authorizations={authorizations}
                onView={(authorization) => {
                  router.push(`/authorizations/authorization/${authorization.id}`);
                }}
                onDelete={handleDeleteAuthorization}
                onCreateNew={() => setView('create-authorization')}
                isLoading={loadingAuthorizations}
                currentUsername={user?.username}
              />
            )}

            {view === 'create-authorization' && (
              <Card className="p-6">
                <h2 className="text-2xl font-bold mb-6">Create New Authorization</h2>
                <AuthorizationWizard
                  templates={templates}
                  sspItems={sspItems}
                  sarItems={sarItems}
                  onSave={handleCreateAuthorization}
                  onCancel={() => setView('list-authorizations')}
                  isSaving={savingAuthorization}
                />
              </Card>
            )}

            {view === 'view-authorization' && selectedAuthorization && (
              <Card className="p-6">
                <div className="flex items-center justify-between mb-6">
                  <div>
                    <h2 className="text-2xl font-bold">{selectedAuthorization.name}</h2>
                    <p className="text-gray-600 mt-1">
                      Authorized by {selectedAuthorization.authorizedBy} on{' '}
                      {new Date(selectedAuthorization.authorizedAt).toLocaleDateString()}
                    </p>
                  </div>
                  <div className="flex gap-2">
                    {!isEditingAuthorization && (
                      <Button
                        variant="outline"
                        onClick={handleStartEdit}
                        disabled={selectedAuthorization.authorizedBy !== user?.username}
                      >
                        <Pencil className="h-4 w-4 mr-2" />
                        Edit
                      </Button>
                    )}
                    {isEditingAuthorization ? (
                      <>
                        <Button
                          variant="outline"
                          onClick={handleCancelEdit}
                          disabled={savingAuthorization}
                        >
                          <X className="h-4 w-4 mr-2" />
                          Cancel
                        </Button>
                        <Button
                          onClick={handleSaveEdit}
                          disabled={savingAuthorization}
                        >
                          <Save className="h-4 w-4 mr-2" />
                          {savingAuthorization ? 'Saving...' : 'Save'}
                        </Button>
                      </>
                    ) : (
                      <Button
                        variant="outline"
                        onClick={() => {
                          setView('list-authorizations');
                          setSelectedAuthorization(null);
                          setIsEditingAuthorization(false);
                        }}
                      >
                        Back
                      </Button>
                    )}
                  </div>
                </div>

                <div className="space-y-6">
                  {/* Authorization Metadata */}
                  <div className="space-y-4">
                    <h3 className="text-lg font-semibold border-b pb-2">Authorization Details</h3>

                    {isEditingAuthorization ? (
                      /* Edit Mode */
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
                      /* View Mode */
                      <div className="grid gap-4 md:grid-cols-2">
                        {/* Authorization Dates */}
                        {(selectedAuthorization.dateAuthorized || selectedAuthorization.dateExpired) && (
                          <Card className="p-4 bg-slate-800 border-slate-700">
                            <h4 className="font-semibold mb-3 text-sm text-slate-300">Authorization Dates</h4>
                            <div className="space-y-2">
                              {selectedAuthorization.dateAuthorized && (
                                <div>
                                  <Label className="text-xs text-slate-400">Date Authorized</Label>
                                  <p className="font-medium">{new Date(selectedAuthorization.dateAuthorized).toLocaleDateString('en-US', {
                                    year: 'numeric',
                                    month: 'long',
                                    day: 'numeric'
                                  })}</p>
                                </div>
                              )}
                              {selectedAuthorization.dateExpired && (
                                <div>
                                  <Label className="text-xs text-slate-400">Date Expired</Label>
                                  <p className="font-medium">{new Date(selectedAuthorization.dateExpired).toLocaleDateString('en-US', {
                                    year: 'numeric',
                                    month: 'long',
                                    day: 'numeric'
                                  })}</p>
                                </div>
                              )}
                            </div>
                          </Card>
                        )}

                        {/* Stakeholders */}
                        {(selectedAuthorization.systemOwner || selectedAuthorization.securityManager || selectedAuthorization.authorizingOfficial) && (
                          <Card className="p-4 bg-slate-800 border-slate-700">
                            <h4 className="font-semibold mb-3 text-sm text-slate-300">Stakeholders</h4>
                            <div className="space-y-2">
                              {selectedAuthorization.systemOwner && (
                                <div>
                                  <Label className="text-xs text-slate-400">System Owner</Label>
                                  <p className="font-medium">{selectedAuthorization.systemOwner}</p>
                                </div>
                              )}
                              {selectedAuthorization.securityManager && (
                                <div>
                                  <Label className="text-xs text-slate-400">Security Manager</Label>
                                  <p className="font-medium">{selectedAuthorization.securityManager}</p>
                                </div>
                              )}
                              {selectedAuthorization.authorizingOfficial && (
                                <div>
                                  <Label className="text-xs text-slate-400">Authorizing Official</Label>
                                  <p className="font-medium">{selectedAuthorization.authorizingOfficial}</p>
                                </div>
                              )}
                            </div>
                          </Card>
                        )}
                      </div>
                    )}
                  </div>

                  {/* Conditions of Approval */}
                  <div className="space-y-4">
                    <div className="flex items-center justify-between border-b pb-2">
                      <h3 className="text-lg font-semibold">Conditions of Approval</h3>
                      {isEditingAuthorization && (
                        <Button
                          onClick={() => setEditConditions([...editConditions, {
                            condition: '',
                            conditionType: 'MANDATORY',
                            dueDate: ''
                          }])}
                          className="bg-primary hover:bg-primary/90"
                        >
                          <Plus className="h-4 w-4 mr-2" />
                          Add New Condition
                        </Button>
                      )}
                    </div>

                    {isEditingAuthorization ? (
                      /* Edit Mode - Editable Conditions */
                      <div className="space-y-3">
                        {editConditions.length === 0 ? (
                          <p className="text-sm text-slate-400">No conditions. Click "Add Condition" to add one.</p>
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
                                        // Clear due date if changing to RECOMMENDED
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
                      /* View Mode - Display Only */
                      selectedAuthorization.conditions && selectedAuthorization.conditions.length > 0 ? (
                        <div className="space-y-2">
                          {selectedAuthorization.conditions.map((condition, index) => (
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

                  {/* System Documents */}
                  <div className="space-y-4">
                    <h3 className="text-lg font-semibold border-b pb-2">System Documents</h3>
                    <div className="grid gap-4 md:grid-cols-2">
                      {/* SSP */}
                      <Card className="p-4">
                        <div className="flex items-start justify-between">
                          <div className="flex-1">
                            <h4 className="font-semibold mb-2">System Security Plan</h4>
                            <p className="text-sm text-gray-600 mb-1">
                              {sspItems.find(item => item.itemId === selectedAuthorization.sspItemId)?.title || 'Unknown SSP'}
                            </p>
                            <p className="text-xs text-slate-400">ID: {selectedAuthorization.sspItemId}</p>
                          </div>
                        </div>
                        <div className="flex gap-2 mt-3">
                          <Button
                            variant="outline"
                            size="sm"
                            onClick={() => {
                              const sspItem = sspItems.find(item => item.itemId === selectedAuthorization.sspItemId);
                              if (sspItem) {
                                router.push(`/visualize?fileId=${sspItem.itemId}`);
                              }
                            }}
                            className="flex-1"
                          >
                            <Eye className="h-4 w-4 mr-1" />
                            Visualize
                          </Button>
                          <Button
                            variant="outline"
                            size="sm"
                            onClick={async () => {
                              const sspItem = sspItems.find(item => item.itemId === selectedAuthorization.sspItemId);
                              if (sspItem) {
                                const content = await apiClient.getFileContent(sspItem.itemId);
                                if (content) {
                                  const blob = new Blob([content], { type: 'text/plain' });
                                  const url = URL.createObjectURL(blob);
                                  const a = document.createElement('a');
                                  a.href = url;
                                  a.download = sspItem.title;
                                  a.click();
                                  URL.revokeObjectURL(url);
                                  toast.success('SSP downloaded');
                                }
                              }
                            }}
                            className="flex-1"
                          >
                            <Download className="h-4 w-4 mr-1" />
                            Download
                          </Button>
                        </div>
                      </Card>

                      {/* SAR */}
                      <Card className="p-4">
                        <div className="flex items-start justify-between">
                          <div className="flex-1">
                            <h4 className="font-semibold mb-2">Security Assessment Report</h4>
                            {selectedAuthorization.sarItemId ? (
                              <>
                                <p className="text-sm text-gray-600 mb-1">
                                  {sarItems.find(item => item.itemId === selectedAuthorization.sarItemId)?.title || 'Unknown SAR'}
                                </p>
                                <p className="text-xs text-slate-400">ID: {selectedAuthorization.sarItemId}</p>
                              </>
                            ) : (
                              <p className="text-sm text-gray-400 italic">Not selected</p>
                            )}
                          </div>
                        </div>
                        {selectedAuthorization.sarItemId && (
                          <div className="flex gap-2 mt-3">
                            <Button
                              variant="outline"
                              size="sm"
                              onClick={() => {
                                const sarItem = sarItems.find(item => item.itemId === selectedAuthorization.sarItemId);
                                if (sarItem) {
                                  router.push(`/visualize?fileId=${sarItem.itemId}`);
                                }
                              }}
                              className="flex-1"
                            >
                              <Eye className="h-4 w-4 mr-1" />
                              Visualize
                            </Button>
                            <Button
                              variant="outline"
                              size="sm"
                              onClick={async () => {
                                const sarItem = sarItems.find(item => item.itemId === selectedAuthorization.sarItemId);
                                if (sarItem) {
                                  const content = await apiClient.getFileContent(sarItem.itemId);
                                  if (content) {
                                    const blob = new Blob([content], { type: 'text/plain' });
                                    const url = URL.createObjectURL(blob);
                                    const a = document.createElement('a');
                                    a.href = url;
                                    a.download = sarItem.title;
                                    a.click();
                                    URL.revokeObjectURL(url);
                                    toast.success('SAR downloaded');
                                  }
                                }
                              }}
                              className="flex-1"
                            >
                              <Download className="h-4 w-4 mr-1" />
                              Download
                            </Button>
                          </div>
                        )}
                      </Card>

                      {/* Template */}
                      <Card className="p-4">
                        <h4 className="font-semibold mb-2">Template</h4>
                        <p className="text-sm text-gray-600">{selectedAuthorization.templateName}</p>
                      </Card>
                    </div>
                  </div>

                  {/* Authorization Document */}
                  <div className="space-y-2">
                    <h3 className="text-lg font-semibold border-b pb-2">Authorization Document</h3>
                    <p className="text-xs text-slate-400 mb-2">Scroll to view the complete document</p>
                    <MarkdownPreview
                      content={selectedAuthorization.completedContent}
                      height="600px"
                    />
                  </div>
                </div>
              </Card>
            )}
          </TabsContent>

          {/* Calendar Tab */}
          <TabsContent value="calendar" className="space-y-6">
            {/* Recently Authorized (Last 90 days) */}
            <Card className="p-6">
              <div className="flex items-center gap-2 mb-4">
                <CheckCircle2 className="h-5 w-5 text-green-500" />
                <h3 className="text-lg font-semibold">Recently Authorized (Last 90 days)</h3>
                <Badge variant="secondary">
                  {(() => {
                    const now = new Date();
                    const ninetyDaysAgo = new Date(now.getTime() - 90 * 24 * 60 * 60 * 1000);
                    return authorizations.filter(auth => {
                      if (!auth.dateAuthorized) return false;
                      const authDate = new Date(auth.dateAuthorized);
                      return authDate >= ninetyDaysAgo && authDate <= now;
                    }).length;
                  })()}
                </Badge>
              </div>
              <div className="space-y-2">
                {(() => {
                  const now = new Date();
                  const ninetyDaysAgo = new Date(now.getTime() - 90 * 24 * 60 * 60 * 1000);
                  const recent = authorizations.filter(auth => {
                    if (!auth.dateAuthorized) return false;
                    const authDate = new Date(auth.dateAuthorized);
                    return authDate >= ninetyDaysAgo && authDate <= now;
                  });

                  if (recent.length === 0) {
                    return <p className="text-sm text-slate-400">No authorizations in the last 90 days</p>;
                  }

                  return recent.map(auth => (
                    <Card key={auth.id} className="p-4 hover:bg-slate-800/50 cursor-pointer transition-colors" onClick={() => {
                      router.push(`/authorizations/authorization/${auth.id}`);
                    }}>
                      <div className="flex items-start justify-between">
                        <div className="flex-1">
                          <h4 className="font-semibold">{auth.name}</h4>
                          <p className="text-sm text-slate-400 mt-1">
                            Authorized: {auth.dateAuthorized ? new Date(auth.dateAuthorized).toLocaleDateString() : 'N/A'}
                          </p>
                          {auth.dateExpired && (
                            <p className="text-sm text-slate-400">
                              Expires: {new Date(auth.dateExpired).toLocaleDateString()}
                            </p>
                          )}
                        </div>
                        <Badge variant="outline" className="bg-green-500/10 border-green-500/30 text-green-400">
                          Recent
                        </Badge>
                      </div>
                    </Card>
                  ));
                })()}
              </div>
            </Card>

            {/* Overdue (Expired) */}
            <Card className="p-6">
              <div className="flex items-center gap-2 mb-4">
                <AlertTriangle className="h-5 w-5 text-red-500" />
                <h3 className="text-lg font-semibold">Overdue (Expired)</h3>
                <Badge variant="destructive">
                  {(() => {
                    const now = new Date();
                    return authorizations.filter(auth => {
                      if (!auth.dateExpired) return false;
                      const expDate = new Date(auth.dateExpired);
                      return expDate < now;
                    }).length;
                  })()}
                </Badge>
              </div>
              <div className="space-y-2">
                {(() => {
                  const now = new Date();
                  const overdue = authorizations.filter(auth => {
                    if (!auth.dateExpired) return false;
                    const expDate = new Date(auth.dateExpired);
                    return expDate < now;
                  });

                  if (overdue.length === 0) {
                    return <p className="text-sm text-slate-400">No overdue authorizations</p>;
                  }

                  return overdue.map(auth => (
                    <Card key={auth.id} className="p-4 hover:bg-slate-800/50 cursor-pointer transition-colors border-red-500/20" onClick={() => {
                      router.push(`/authorizations/authorization/${auth.id}`);
                    }}>
                      <div className="flex items-start justify-between">
                        <div className="flex-1">
                          <h4 className="font-semibold">{auth.name}</h4>
                          <p className="text-sm text-slate-400 mt-1">
                            Authorized: {auth.dateAuthorized ? new Date(auth.dateAuthorized).toLocaleDateString() : 'N/A'}
                          </p>
                          <p className="text-sm text-red-400 font-medium">
                            Expired: {new Date(auth.dateExpired!).toLocaleDateString()}
                          </p>
                        </div>
                        <Badge variant="outline" className="bg-red-500/10 border-red-500/30 text-red-400">
                          Expired
                        </Badge>
                      </div>
                    </Card>
                  ));
                })()}
              </div>
            </Card>

            {/* Expiring Soon (Next 90 days) */}
            <Card className="p-6">
              <div className="flex items-center gap-2 mb-4">
                <Clock className="h-5 w-5 text-yellow-500" />
                <h3 className="text-lg font-semibold">Expiring Soon (Next 90 days)</h3>
                <Badge variant="outline" className="bg-yellow-500/10 border-yellow-500/30 text-yellow-400">
                  {(() => {
                    const now = new Date();
                    const ninetyDaysFromNow = new Date(now.getTime() + 90 * 24 * 60 * 60 * 1000);
                    return authorizations.filter(auth => {
                      if (!auth.dateExpired) return false;
                      const expDate = new Date(auth.dateExpired);
                      return expDate >= now && expDate <= ninetyDaysFromNow;
                    }).length;
                  })()}
                </Badge>
              </div>
              <div className="space-y-2">
                {(() => {
                  const now = new Date();
                  const ninetyDaysFromNow = new Date(now.getTime() + 90 * 24 * 60 * 60 * 1000);
                  const expiringSoon = authorizations.filter(auth => {
                    if (!auth.dateExpired) return false;
                    const expDate = new Date(auth.dateExpired);
                    return expDate >= now && expDate <= ninetyDaysFromNow;
                  });

                  if (expiringSoon.length === 0) {
                    return <p className="text-sm text-slate-400">No authorizations expiring in the next 90 days</p>;
                  }

                  return expiringSoon.map(auth => (
                    <Card key={auth.id} className="p-4 hover:bg-slate-800/50 cursor-pointer transition-colors border-yellow-500/20" onClick={() => {
                      router.push(`/authorizations/authorization/${auth.id}`);
                    }}>
                      <div className="flex items-start justify-between">
                        <div className="flex-1">
                          <h4 className="font-semibold">{auth.name}</h4>
                          <p className="text-sm text-slate-400 mt-1">
                            Authorized: {auth.dateAuthorized ? new Date(auth.dateAuthorized).toLocaleDateString() : 'N/A'}
                          </p>
                          <p className="text-sm text-yellow-400 font-medium">
                            Expires: {new Date(auth.dateExpired!).toLocaleDateString()}
                          </p>
                        </div>
                        <Badge variant="outline" className="bg-yellow-500/10 border-yellow-500/30 text-yellow-400">
                          Expiring Soon
                        </Badge>
                      </div>
                    </Card>
                  ));
                })()}
              </div>
            </Card>

            {/* All Authorizations (with search and filter) */}
            <Card className="p-6">
              <div className="flex items-center gap-2 mb-4">
                <ShieldCheck className="h-5 w-5 text-primary" />
                <h3 className="text-lg font-semibold">All Authorizations</h3>
                <Badge variant="secondary">{authorizations.length}</Badge>
              </div>

              {/* Search and Filter */}
              <div className="mb-4 p-4 bg-slate-800/50 rounded-lg border border-slate-700">
                <div className="flex flex-col md:flex-row gap-4">
                  <div className="flex-1">
                    <Label htmlFor="calendar-search">Search by Title</Label>
                    <Input
                      id="calendar-search"
                      type="text"
                      placeholder="Search authorizations..."
                      value={calendarSearchTerm}
                      onChange={(e) => setCalendarSearchTerm(e.target.value)}
                    />
                  </div>
                  <div className="flex gap-4">
                    <div>
                      <Label htmlFor="start-date">Start Date</Label>
                      <Input
                        id="start-date"
                        type="date"
                        value={calendarStartDate}
                        onChange={(e) => setCalendarStartDate(e.target.value)}
                      />
                    </div>
                    <div>
                      <Label htmlFor="end-date">End Date</Label>
                      <Input
                        id="end-date"
                        type="date"
                        value={calendarEndDate}
                        onChange={(e) => setCalendarEndDate(e.target.value)}
                      />
                    </div>
                  </div>
                </div>
              </div>

              <div className="space-y-2">
                {(() => {
                  let filtered = authorizations;

                  // Apply search filter
                  if (calendarSearchTerm) {
                    const searchLower = calendarSearchTerm.toLowerCase();
                    filtered = filtered.filter(auth =>
                      auth.name.toLowerCase().includes(searchLower)
                    );
                  }

                  // Apply date range filter
                  if (calendarStartDate || calendarEndDate) {
                    filtered = filtered.filter(auth => {
                      if (!auth.dateAuthorized) return false;
                      const authDate = new Date(auth.dateAuthorized);

                      if (calendarStartDate) {
                        const startDate = new Date(calendarStartDate);
                        if (authDate < startDate) return false;
                      }

                      if (calendarEndDate) {
                        const endDate = new Date(calendarEndDate);
                        if (authDate > endDate) return false;
                      }

                      return true;
                    });
                  }

                  if (filtered.length === 0) {
                    return <p className="text-sm text-slate-400">No authorizations match your search criteria</p>;
                  }

                  return filtered.map(auth => {
                    const now = new Date();
                    const isExpired = auth.dateExpired && new Date(auth.dateExpired) < now;
                    const ninetyDaysFromNow = new Date(now.getTime() + 90 * 24 * 60 * 60 * 1000);
                    const isExpiringSoon = auth.dateExpired && new Date(auth.dateExpired) >= now && new Date(auth.dateExpired) <= ninetyDaysFromNow;

                    return (
                      <Card key={auth.id} className={`p-4 hover:bg-slate-800/50 cursor-pointer transition-colors ${
                        isExpired ? 'border-red-500/20' : isExpiringSoon ? 'border-yellow-500/20' : ''
                      }`} onClick={() => {
                        router.push(`/authorizations/authorization/${auth.id}`);
                      }}>
                        <div className="flex items-start justify-between">
                          <div className="flex-1">
                            <h4 className="font-semibold">{auth.name}</h4>
                            <p className="text-sm text-slate-400 mt-1">
                              Authorized: {auth.dateAuthorized ? new Date(auth.dateAuthorized).toLocaleDateString() : 'N/A'}
                            </p>
                            {auth.dateExpired && (
                              <p className={`text-sm font-medium ${
                                isExpired ? 'text-red-400' : isExpiringSoon ? 'text-yellow-400' : 'text-slate-400'
                              }`}>
                                Expires: {new Date(auth.dateExpired).toLocaleDateString()}
                              </p>
                            )}
                          </div>
                          {isExpired && (
                            <Badge variant="outline" className="bg-red-500/10 border-red-500/30 text-red-400">
                              Expired
                            </Badge>
                          )}
                          {!isExpired && isExpiringSoon && (
                            <Badge variant="outline" className="bg-yellow-500/10 border-yellow-500/30 text-yellow-400">
                              Expiring Soon
                            </Badge>
                          )}
                        </div>
                      </Card>
                    );
                  });
                })()}
              </div>
            </Card>
          </TabsContent>

          {/* Templates Tab */}
          <TabsContent value="templates" className="space-y-6">
            {view === 'list-templates' && (
              <TemplateList
                templates={templates}
                onSelectTemplate={(template) => {
                  router.push(`/authorizations/template/${template.id}`);
                }}
                onEditTemplate={(template) => {
                  router.push(`/authorizations/template/${template.id}`);
                }}
                onDeleteTemplate={handleDeleteTemplate}
                onCreateNew={() => setView('create-template')}
                isLoading={loadingTemplates}
                currentUsername={user?.username}
              />
            )}

            {view === 'create-template' && (
              <Card className="p-6">
                <h2 className="text-2xl font-bold mb-6">Create Authorization Template</h2>
                <TemplateEditor
                  onSave={handleCreateTemplate}
                  onCancel={() => setView('list-templates')}
                  isSaving={savingTemplate}
                />
              </Card>
            )}

            {view === 'edit-template' && selectedTemplate && (
              <Card className="p-6">
                <h2 className="text-2xl font-bold mb-6">Edit Template</h2>
                <TemplateEditor
                  initialName={selectedTemplate.name}
                  initialContent={selectedTemplate.content}
                  onSave={handleUpdateTemplate}
                  onCancel={() => {
                    setView('list-templates');
                    setSelectedTemplate(null);
                  }}
                  isSaving={savingTemplate}
                />
              </Card>
            )}

            {view === 'view-template' && selectedTemplate && (
              <Card className="p-6">
                <div className="flex items-center justify-between mb-6">
                  <h2 className="text-2xl font-bold">{selectedTemplate.name}</h2>
                  <div className="flex items-center gap-2">
                    <Button
                      variant="outline"
                      onClick={() => setView('edit-template')}
                    >
                      Edit
                    </Button>
                    <Button
                      variant="outline"
                      onClick={() => setView('list-templates')}
                    >
                      Back
                    </Button>
                  </div>
                </div>
                <MarkdownPreview
                  content={selectedTemplate.content}
                  variables={selectedTemplate.variables}
                  height="600px"
                />
              </Card>
            )}
          </TabsContent>
        </Tabs>
      </div>
      <Footer />
    </div>
  );
}
