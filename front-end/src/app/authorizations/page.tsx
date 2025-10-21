'use client';

import { useState, useEffect } from 'react';
import { useRouter } from 'next/navigation';
import { Card } from '@/components/ui/card';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';
import { Alert, AlertDescription } from '@/components/ui/alert';
import { ShieldCheck, FileText, CheckCircle, ArrowLeft, Download, Eye } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Label } from '@/components/ui/label';
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

  // SSP and SAR items for authorization creation
  const [sspItems, setSspItems] = useState<LibraryItem[]>([]);
  const [sarItems, setSarItems] = useState<LibraryItem[]>([]);

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
          lastUpdatedAt: file.uploadedAt,
          lastUpdatedBy: '',
          downloadCount: 0,
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
          lastUpdatedAt: file.uploadedAt,
          lastUpdatedBy: '',
          downloadCount: 0,
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
          } else {
            setView('list-authorizations');
          }
        }} className="space-y-6">
          <TabsList className="grid w-full grid-cols-2">
            <TabsTrigger value="authorizations">
              <CheckCircle className="h-4 w-4 mr-2" />
              Authorizations
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
                  setSelectedAuthorization(authorization);
                  setView('view-authorization');
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
                  <Button
                    variant="outline"
                    onClick={() => {
                      setView('list-authorizations');
                      setSelectedAuthorization(null);
                    }}
                  >
                    Back
                  </Button>
                </div>

                <div className="space-y-6">
                  {/* Authorization Metadata */}
                  <div className="space-y-4">
                    <h3 className="text-lg font-semibold border-b pb-2">Authorization Details</h3>

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

          {/* Templates Tab */}
          <TabsContent value="templates" className="space-y-6">
            {view === 'list-templates' && (
              <TemplateList
                templates={templates}
                onSelectTemplate={(template) => {
                  setSelectedTemplate(template);
                  setView('view-template');
                }}
                onEditTemplate={(template) => {
                  setSelectedTemplate(template);
                  setView('edit-template');
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
