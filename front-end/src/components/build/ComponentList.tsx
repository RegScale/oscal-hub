'use client';

import { useState, useEffect } from 'react';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Badge } from '@/components/ui/badge';
import { Dialog, DialogContent, DialogHeader, DialogTitle } from '@/components/ui/dialog';
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from '@/components/ui/alert-dialog';
import { apiClient } from '@/lib/api-client';
import type { ComponentDefinitionResponse } from '@/types/oscal';
import { ImportComponentDialog } from './ImportComponentDialog';
import {
  Plus,
  Search,
  Eye,
  Download,
  Edit2,
  Trash2,
  FileJson,
  Loader2,
  AlertCircle,
  Calendar,
  Blocks,
  Shield,
  ChevronRight,
  Upload,
} from 'lucide-react';

interface ComponentListProps {
  onCreateNew: () => void;
  onEdit?: (component: ComponentDefinitionResponse) => void;
}

export function ComponentList({ onCreateNew, onEdit }: ComponentListProps) {
  const [components, setComponents] = useState<ComponentDefinitionResponse[]>([]);
  const [filteredComponents, setFilteredComponents] = useState<ComponentDefinitionResponse[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // Search
  const [searchQuery, setSearchQuery] = useState('');

  // View JSON Modal
  const [viewJsonOpen, setViewJsonOpen] = useState(false);
  const [viewingComponent, setViewingComponent] = useState<ComponentDefinitionResponse | null>(null);
  const [componentJson, setComponentJson] = useState<string>('');
  const [loadingJson, setLoadingJson] = useState(false);

  // Delete confirmation
  const [deleteDialogOpen, setDeleteDialogOpen] = useState(false);
  const [componentToDelete, setComponentToDelete] = useState<ComponentDefinitionResponse | null>(null);
  const [isDeleting, setIsDeleting] = useState(false);

  // Import dialog
  const [importDialogOpen, setImportDialogOpen] = useState(false);

  // Load components
  const loadComponents = async () => {
    setIsLoading(true);
    setError(null);

    try {
      const data = await apiClient.getUserComponentDefinitions();
      setComponents(data);
      setFilteredComponents(data);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load components');
      console.error('Error loading components:', err);
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    loadComponents();
  }, []);

  // Apply search filter
  useEffect(() => {
    let filtered = components;

    if (searchQuery.trim()) {
      const query = searchQuery.toLowerCase();
      filtered = filtered.filter(
        (comp) =>
          comp.title.toLowerCase().includes(query) ||
          (comp.description && comp.description.toLowerCase().includes(query)) ||
          comp.version.toLowerCase().includes(query)
      );
    }

    setFilteredComponents(filtered);
  }, [components, searchQuery]);

  const handleViewJson = async (component: ComponentDefinitionResponse) => {
    setViewingComponent(component);
    setViewJsonOpen(true);
    setLoadingJson(true);

    try {
      const json = await apiClient.getComponentDefinitionContent(component.id);
      setComponentJson(JSON.stringify(json, null, 2));
    } catch (err) {
      setComponentJson(`Error loading JSON: ${err instanceof Error ? err.message : 'Unknown error'}`);
      console.error('Error loading component JSON:', err);
    } finally {
      setLoadingJson(false);
    }
  };

  const handleDownload = async (component: ComponentDefinitionResponse) => {
    try {
      const json = await apiClient.getComponentDefinitionContent(component.id);
      const blob = new Blob([JSON.stringify(json, null, 2)], { type: 'application/json' });
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = component.filename || `${component.title.toLowerCase().replace(/\s+/g, '-')}.json`;
      document.body.appendChild(a);
      a.click();
      document.body.removeChild(a);
      URL.revokeObjectURL(url);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to download component');
      console.error('Error downloading component:', err);
    }
  };

  const handleEdit = (component: ComponentDefinitionResponse) => {
    if (onEdit) {
      onEdit(component);
    } else {
      console.log('Edit component:', component);
      // TODO: Implement edit functionality
    }
  };

  const handleDeleteClick = (component: ComponentDefinitionResponse) => {
    setComponentToDelete(component);
    setDeleteDialogOpen(true);
  };

  const handleDeleteConfirm = async () => {
    if (!componentToDelete) return;

    setIsDeleting(true);
    try {
      const success = await apiClient.deleteComponentDefinition(componentToDelete.id);
      if (success) {
        await loadComponents();
        setDeleteDialogOpen(false);
        setComponentToDelete(null);
      } else {
        setError('Failed to delete component');
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to delete component');
      console.error('Error deleting component:', err);
    } finally {
      setIsDeleting(false);
    }
  };

  const formatDate = (dateString: string) => {
    return new Date(dateString).toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
    });
  };

  return (
    <div className="space-y-6">
      {/* Header with Search and Create */}
      <div className="flex flex-col sm:flex-row gap-4 items-start sm:items-center justify-between">
        <div className="flex-1 w-full sm:w-auto">
          <div className="relative">
            <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 h-4 w-4 text-muted-foreground" />
            <Input
              placeholder="Search components..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="pl-10 w-full"
            />
          </div>
        </div>
        <div className="flex gap-2">
          <Button variant="outline" onClick={() => setImportDialogOpen(true)}>
            <Upload className="mr-2 h-4 w-4" />
            Import
          </Button>
          <Button onClick={onCreateNew}>
            <Plus className="mr-2 h-4 w-4" />
            Create New
          </Button>
        </div>
      </div>

      {/* Active Search Summary */}
      {searchQuery && (
        <div className="flex items-center gap-2 text-sm text-muted-foreground">
          <span>
            Showing {filteredComponents.length} of {components.length} components
          </span>
          {searchQuery && (
            <Button
              variant="ghost"
              size="sm"
              onClick={() => setSearchQuery('')}
            >
              Clear Search
            </Button>
          )}
        </div>
      )}

      {/* Error State */}
      {error && (
        <Card className="border-red-200 bg-red-50 dark:bg-red-950">
          <CardContent className="flex items-center gap-3 p-6">
            <AlertCircle className="h-5 w-5 text-red-600" />
            <div className="flex-1">
              <p className="font-medium text-red-900 dark:text-red-100">Error loading components</p>
              <p className="text-sm text-red-700 dark:text-red-300">{error}</p>
            </div>
            <Button variant="outline" size="sm" onClick={loadComponents}>
              Retry
            </Button>
          </CardContent>
        </Card>
      )}

      {/* Loading State */}
      {isLoading && (
        <div className="flex items-center justify-center p-12">
          <Loader2 className="h-8 w-8 animate-spin text-muted-foreground" />
        </div>
      )}

      {/* Empty State */}
      {!isLoading && !error && filteredComponents.length === 0 && (
        <Card>
          <CardContent className="flex flex-col items-center justify-center py-12 text-center">
            <Blocks className="h-16 w-16 text-muted-foreground mb-4" />
            <h3 className="text-lg font-semibold mb-2">
              {searchQuery ? 'No components found' : 'No component definitions yet'}
            </h3>
            <p className="text-muted-foreground mb-6 max-w-md">
              {searchQuery
                ? 'Try adjusting your search query'
                : 'Create your first OSCAL component definition using the visual builder'}
            </p>
            {!searchQuery && (
              <Button onClick={onCreateNew}>
                <Plus className="mr-2 h-4 w-4" />
                Create Your First Component
              </Button>
            )}
          </CardContent>
        </Card>
      )}

      {/* Components List */}
      {!isLoading && !error && filteredComponents.length > 0 && (
        <div className="grid grid-cols-1 gap-4">
          {filteredComponents.map((component) => (
            <Card key={component.id} className="hover:shadow-md transition-shadow">
              <CardHeader>
                <div className="flex items-start justify-between">
                  <div className="flex-1">
                    <div className="flex items-center gap-3 mb-2">
                      <div className="p-2 rounded-lg bg-primary/10">
                        <FileJson className="h-5 w-5 text-primary" />
                      </div>
                      <div className="flex-1">
                        <CardTitle className="text-xl">{component.title}</CardTitle>
                        <div className="flex items-center gap-2 mt-1">
                          <Badge variant="outline" className="text-xs">
                            v{component.version}
                          </Badge>
                          <Badge variant="secondary" className="text-xs">
                            OSCAL {component.oscalVersion}
                          </Badge>
                        </div>
                      </div>
                    </div>
                    {component.description && (
                      <CardDescription className="mt-2">
                        {component.description}
                      </CardDescription>
                    )}
                  </div>
                </div>
              </CardHeader>

              <CardContent className="space-y-4">
                {/* Stats */}
                <div className="flex items-center gap-6 text-sm">
                  <div className="flex items-center gap-2 text-muted-foreground">
                    <Blocks className="h-4 w-4" />
                    <span>
                      <span className="font-medium text-foreground">{component.componentCount}</span>{' '}
                      {component.componentCount === 1 ? 'Component' : 'Components'}
                    </span>
                  </div>
                  <div className="flex items-center gap-2 text-muted-foreground">
                    <Shield className="h-4 w-4" />
                    <span>
                      <span className="font-medium text-foreground">{component.controlCount}</span>{' '}
                      {component.controlCount === 1 ? 'Control' : 'Controls'}
                    </span>
                  </div>
                  <div className="flex items-center gap-2 text-muted-foreground">
                    <Calendar className="h-4 w-4" />
                    <span>Created {formatDate(component.createdAt)}</span>
                  </div>
                </div>

                {/* Actions */}
                <div className="flex flex-wrap gap-2 pt-2 border-t">
                  <Button
                    variant="outline"
                    size="sm"
                    onClick={() => handleViewJson(component)}
                  >
                    <Eye className="mr-2 h-3 w-3" />
                    View JSON
                  </Button>
                  <Button
                    variant="outline"
                    size="sm"
                    onClick={() => handleDownload(component)}
                  >
                    <Download className="mr-2 h-3 w-3" />
                    Download
                  </Button>
                  <Button
                    variant="outline"
                    size="sm"
                    onClick={() => handleEdit(component)}
                  >
                    <Edit2 className="mr-2 h-3 w-3" />
                    Edit
                  </Button>
                  <Button
                    variant="outline"
                    size="sm"
                    onClick={() => handleDeleteClick(component)}
                    className="text-red-600 hover:text-red-700 hover:bg-red-50"
                  >
                    <Trash2 className="mr-2 h-3 w-3" />
                    Delete
                  </Button>
                </div>
              </CardContent>
            </Card>
          ))}
        </div>
      )}

      {/* View JSON Modal */}
      <Dialog open={viewJsonOpen} onOpenChange={setViewJsonOpen}>
        <DialogContent className="max-w-4xl max-h-[80vh] overflow-hidden flex flex-col">
          <DialogHeader>
            <DialogTitle>
              {viewingComponent?.title || 'Component Definition'} - OSCAL JSON
            </DialogTitle>
          </DialogHeader>
          <div className="flex-1 overflow-auto">
            {loadingJson ? (
              <div className="flex items-center justify-center p-12">
                <Loader2 className="h-8 w-8 animate-spin text-muted-foreground" />
              </div>
            ) : (
              <pre className="bg-muted p-4 rounded-lg overflow-x-auto text-xs">
                <code>{componentJson}</code>
              </pre>
            )}
          </div>
          <div className="flex gap-2 pt-4 border-t">
            <Button
              variant="outline"
              onClick={() => {
                if (viewingComponent) {
                  handleDownload(viewingComponent);
                }
              }}
              disabled={loadingJson}
            >
              <Download className="mr-2 h-4 w-4" />
              Download JSON
            </Button>
            <Button variant="outline" onClick={() => setViewJsonOpen(false)}>
              Close
            </Button>
          </div>
        </DialogContent>
      </Dialog>

      {/* Delete Confirmation Dialog */}
      <AlertDialog open={deleteDialogOpen} onOpenChange={setDeleteDialogOpen}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>Delete Component Definition</AlertDialogTitle>
            <AlertDialogDescription>
              Are you sure you want to delete &quot;{componentToDelete?.title}&quot;? This action cannot be undone.
              <span className="block mt-2 text-foreground font-medium">
                This will delete:
              </span>
              <ul className="list-disc list-inside mt-1 text-sm">
                <li>{componentToDelete?.componentCount} component{componentToDelete?.componentCount !== 1 ? 's' : ''}</li>
                <li>{componentToDelete?.controlCount} control implementation{componentToDelete?.controlCount !== 1 ? 's' : ''}</li>
                <li>The OSCAL JSON file from storage</li>
              </ul>
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel disabled={isDeleting}>Cancel</AlertDialogCancel>
            <AlertDialogAction
              onClick={handleDeleteConfirm}
              disabled={isDeleting}
              className="bg-red-600 hover:bg-red-700"
            >
              {isDeleting ? (
                <>
                  <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                  Deleting...
                </>
              ) : (
                'Delete'
              )}
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>

      {/* Import Component Dialog */}
      <ImportComponentDialog
        open={importDialogOpen}
        onOpenChange={setImportDialogOpen}
        onSuccess={loadComponents}
      />
    </div>
  );
}
