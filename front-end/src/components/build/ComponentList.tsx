'use client';

import { useState, useEffect } from 'react';
import { useRouter } from 'next/navigation';
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
import { libraryPublishApi } from '@/lib/api/library';
import type { ComponentDefinitionResponse } from '@/types/oscal';
import { ImportComponentDialog } from './ImportComponentDialog';
import { toast } from 'sonner';
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
  Upload,
  BookPlus,
} from 'lucide-react';

interface ComponentListProps {
  onCreateNew: () => void;
  onEdit?: (component: ComponentDefinitionResponse) => void;
}

export function ComponentList({ onCreateNew, onEdit }: ComponentListProps) {
  const router = useRouter();
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

  // Add-to-library state
  const [addingToLibraryId, setAddingToLibraryId] = useState<number | null>(null);

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
          (comp.version && comp.version.toLowerCase().includes(query))
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

      // If we got a string, it might already be formatted JSON
      if (typeof json === 'string') {
        setComponentJson(json);
      } else {
        // If it's an object, stringify it with formatting
        setComponentJson(JSON.stringify(json, null, 2));
      }
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

      // If we got a string, parse it first to handle any escaped characters
      let jsonObject: unknown;
      if (typeof json === 'string') {
        try {
          jsonObject = JSON.parse(json);
        } catch (parseError) {
          // If parsing fails, the string might already be plain text, use as-is
          console.warn('Could not parse JSON string, using as-is:', parseError);
          jsonObject = json;
        }
      } else {
        jsonObject = json;
      }

      // Now format it properly with indentation
      const jsonContent = typeof jsonObject === 'string'
        ? jsonObject
        : JSON.stringify(jsonObject, null, 2);

      const blob = new Blob([jsonContent], { type: 'application/json' });
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
    // Navigate to the component detail page
    router.push(`/build/component/${component.id}`);
  };

  const handleDeleteClick = (component: ComponentDefinitionResponse) => {
    setComponentToDelete(component);
    setDeleteDialogOpen(true);
  };

  const handleAddToLibrary = async (component: ComponentDefinitionResponse) => {
    setAddingToLibraryId(component.id);
    try {
      await libraryPublishApi.saveComponentToLibrary(component.id, {
        title: component.title,
        description: component.description ?? undefined,
        visibility: 'PRIVATE',
      });
      toast.success(`${component.title} added to your Library`, {
        description: 'Visibility is Private. Open Library to publish or share.',
      });
    } catch (e) {
      toast.error(`Failed to add to Library: ${e instanceof Error ? e.message : 'unknown'}`);
    } finally {
      setAddingToLibraryId(null);
    }
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
    <div className="space-y-4">
      {/* Header card — matches BuiltDocList so Catalog/Profile/SSP/AP/AR/POA&M
          and Components all share one layout. */}
      <Card>
        <CardHeader>
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-3">
              <Blocks className="h-6 w-6 text-primary" />
              <div>
                <CardTitle>My Component Definitions</CardTitle>
                <CardDescription>
                  {components.length} component definition{components.length === 1 ? '' : 's'} created
                </CardDescription>
              </div>
            </div>
            <div className="flex items-center gap-2">
              <Button variant="outline" onClick={() => setImportDialogOpen(true)}>
                <Upload className="h-4 w-4 mr-1" />
                Import
              </Button>
              <Button onClick={onCreateNew}>
                <Plus className="h-4 w-4 mr-1" />
                New Component Definition
              </Button>
            </div>
          </div>
        </CardHeader>
        <CardContent>
          <div className="relative">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
            <Input
              placeholder="Search component definitions…"
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="pl-9"
            />
          </div>
        </CardContent>
      </Card>

      {error && (
        <Card className="border-destructive">
          <CardContent className="py-4">
            <div className="flex items-center gap-2 text-destructive">
              <AlertCircle className="h-4 w-4" />
              <span className="text-sm flex-1">{error}</span>
              <Button variant="outline" size="sm" onClick={loadComponents}>
                Retry
              </Button>
            </div>
          </CardContent>
        </Card>
      )}

      {isLoading ? (
        <Card>
          <CardContent className="py-12 text-center">
            <Loader2 className="h-8 w-8 animate-spin mx-auto text-primary mb-2" />
            <p className="text-sm text-muted-foreground">Loading component definitions…</p>
          </CardContent>
        </Card>
      ) : filteredComponents.length === 0 ? (
        <Card>
          <CardContent className="py-12 text-center">
            <Blocks className="h-12 w-12 text-muted-foreground/40 mx-auto mb-3" />
            <p className="text-sm text-muted-foreground mb-4">
              {searchQuery
                ? 'No component definitions match your search.'
                : "You haven't created any component definitions yet."}
            </p>
            {!searchQuery && (
              <Button onClick={onCreateNew}>
                <Plus className="h-4 w-4 mr-1" />
                Create your first component definition
              </Button>
            )}
          </CardContent>
        </Card>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-3">
          {filteredComponents.map((component) => (
            <Card key={component.id} className="flex flex-col">
              <CardHeader className="pb-3">
                <CardTitle className="text-base line-clamp-2 break-words overflow-hidden" title={component.title}>
                  {component.title}
                </CardTitle>
                <div className="flex items-center gap-1 mt-1">
                  <Badge variant="outline" className="text-xs">
                    v{component.version || '—'}
                  </Badge>
                </div>
                <CardDescription className="text-xs line-clamp-2">
                  {component.description || <span className="italic">No description</span>}
                </CardDescription>
              </CardHeader>
              <CardContent className="flex-1 pb-3 space-y-2">
                <div className="flex flex-wrap items-center gap-1">
                  <Badge variant="secondary" className="text-xs font-mono">
                    {component.componentCount || 0} components
                  </Badge>
                  <Badge variant="secondary" className="text-xs font-mono">
                    {component.capabilityCount || 0} capabilities
                  </Badge>
                  <Badge variant="secondary" className="text-xs font-mono">
                    {component.controlCount} controls
                  </Badge>
                  <Badge variant="outline" className="text-xs">
                    OSCAL {component.oscalVersion}
                  </Badge>
                </div>
                <div className="flex items-center gap-1.5 text-xs text-muted-foreground">
                  <Calendar className="h-3 w-3" />
                  <span>Created {formatDate(component.createdAt)}</span>
                </div>
              </CardContent>
              <div className="flex items-center justify-end gap-1 px-3 py-2 border-t bg-muted/20">
                <Button size="sm" variant="ghost" onClick={() => handleViewJson(component)} title="View JSON">
                  <Eye className="h-3.5 w-3.5" />
                </Button>
                <Button size="sm" variant="ghost" onClick={() => handleDownload(component)} title="Download">
                  <Download className="h-3.5 w-3.5" />
                </Button>
                <Button size="sm" variant="ghost" onClick={() => handleEdit(component)} title="Edit">
                  <Edit2 className="h-3.5 w-3.5" />
                </Button>
                <Button
                  size="sm"
                  variant="ghost"
                  onClick={() => handleAddToLibrary(component)}
                  disabled={addingToLibraryId === component.id}
                  title="Add to Library"
                >
                  {addingToLibraryId === component.id
                    ? <Loader2 className="h-3.5 w-3.5 animate-spin" />
                    : <BookPlus className="h-3.5 w-3.5" />}
                </Button>
                <Button
                  size="sm"
                  variant="ghost"
                  onClick={() => handleDeleteClick(component)}
                  title="Delete"
                  className="text-destructive hover:text-destructive"
                >
                  <Trash2 className="h-3.5 w-3.5" />
                </Button>
              </div>
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
                <li>{componentToDelete?.componentCount || 0} component{componentToDelete?.componentCount !== 1 ? 's' : ''}</li>
                <li>{componentToDelete?.capabilityCount || 0} {componentToDelete?.capabilityCount === 1 ? 'capability' : 'capabilities'}</li>
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
