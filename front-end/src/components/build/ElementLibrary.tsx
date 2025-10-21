'use client';

import { useState, useEffect } from 'react';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { Badge } from '@/components/ui/badge';
import { apiClient } from '@/lib/api-client';
import type { ReusableElementResponse, ReusableElementType } from '@/types/oscal';
import { ElementModal } from './ElementModal';
import {
  Plus,
  Search,
  Filter,
  Edit2,
  Trash2,
  FileText,
  Users,
  Link2,
  BookOpen,
  UserCheck,
  Loader2,
  AlertCircle,
} from 'lucide-react';
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

const ELEMENT_TYPE_ICONS: Record<ReusableElementType, typeof FileText> = {
  ROLE: UserCheck,
  PARTY: Users,
  LINK: Link2,
  BACK_MATTER: BookOpen,
  RESPONSIBLE_PARTY: UserCheck,
};

const ELEMENT_TYPE_LABELS: Record<ReusableElementType, string> = {
  ROLE: 'Role',
  PARTY: 'Party',
  LINK: 'Link',
  BACK_MATTER: 'Back Matter',
  RESPONSIBLE_PARTY: 'Responsible Party',
};

const ELEMENT_TYPE_COLORS: Record<ReusableElementType, string> = {
  ROLE: 'bg-blue-100 text-blue-800 dark:bg-blue-900 dark:text-blue-300',
  PARTY: 'bg-green-100 text-green-800 dark:bg-green-900 dark:text-green-300',
  LINK: 'bg-purple-100 text-purple-800 dark:bg-purple-900 dark:text-purple-300',
  BACK_MATTER: 'bg-orange-100 text-orange-800 dark:bg-orange-900 dark:text-orange-300',
  RESPONSIBLE_PARTY: 'bg-pink-100 text-pink-800 dark:bg-pink-900 dark:text-pink-300',
};

export function ElementLibrary() {
  const [elements, setElements] = useState<ReusableElementResponse[]>([]);
  const [filteredElements, setFilteredElements] = useState<ReusableElementResponse[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // Filters
  const [searchQuery, setSearchQuery] = useState('');
  const [typeFilter, setTypeFilter] = useState<string>('all');

  // Modal state
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [editingElement, setEditingElement] = useState<ReusableElementResponse | undefined>();

  // Delete confirmation
  const [deleteDialogOpen, setDeleteDialogOpen] = useState(false);
  const [elementToDelete, setElementToDelete] = useState<ReusableElementResponse | null>(null);

  // Load elements
  const loadElements = async () => {
    setIsLoading(true);
    setError(null);

    try {
      const data = await apiClient.getUserReusableElements();
      setElements(data);
      setFilteredElements(data);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load elements');
      console.error('Error loading elements:', err);
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    loadElements();
  }, []);

  // Apply filters
  useEffect(() => {
    let filtered = elements;

    // Type filter
    if (typeFilter !== 'all') {
      filtered = filtered.filter((el) => el.type === typeFilter);
    }

    // Search filter
    if (searchQuery.trim()) {
      const query = searchQuery.toLowerCase();
      filtered = filtered.filter(
        (el) =>
          el.name.toLowerCase().includes(query) ||
          (el.description && el.description.toLowerCase().includes(query))
      );
    }

    setFilteredElements(filtered);
  }, [elements, typeFilter, searchQuery]);

  const handleCreate = () => {
    setEditingElement(undefined);
    setIsModalOpen(true);
  };

  const handleEdit = (element: ReusableElementResponse) => {
    setEditingElement(element);
    setIsModalOpen(true);
  };

  const handleDeleteClick = (element: ReusableElementResponse) => {
    setElementToDelete(element);
    setDeleteDialogOpen(true);
  };

  const handleDeleteConfirm = async () => {
    if (!elementToDelete) return;

    try {
      const success = await apiClient.deleteReusableElement(elementToDelete.id);
      if (success) {
        await loadElements();
      } else {
        setError('Failed to delete element');
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to delete element');
      console.error('Error deleting element:', err);
    } finally {
      setDeleteDialogOpen(false);
      setElementToDelete(null);
    }
  };

  const handleModalSuccess = () => {
    loadElements();
  };

  // Get element count by type
  const getTypeCounts = () => {
    const counts: Record<string, number> = { all: elements.length };
    elements.forEach((el) => {
      counts[el.type] = (counts[el.type] || 0) + 1;
    });
    return counts;
  };

  const typeCounts = getTypeCounts();

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h2 className="text-2xl font-bold tracking-tight">Element Library</h2>
          <p className="text-muted-foreground">
            Manage reusable OSCAL elements for your component definitions
          </p>
        </div>
        <Button onClick={handleCreate}>
          <Plus className="mr-2 h-4 w-4" />
          Create Element
        </Button>
      </div>

      {/* Filters */}
      <Card>
        <CardHeader>
          <CardTitle className="text-lg flex items-center">
            <Filter className="mr-2 h-5 w-5" />
            Filter Elements
          </CardTitle>
        </CardHeader>
        <CardContent className="space-y-4">
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            {/* Search */}
            <div className="space-y-2">
              <label className="text-sm font-medium">Search</label>
              <div className="relative">
                <Search className="absolute left-2 top-2.5 h-4 w-4 text-muted-foreground" />
                <Input
                  placeholder="Search by name or description..."
                  value={searchQuery}
                  onChange={(e) => setSearchQuery(e.target.value)}
                  className="pl-8"
                />
              </div>
            </div>

            {/* Type Filter */}
            <div className="space-y-2">
              <label className="text-sm font-medium">Element Type</label>
              <Select value={typeFilter} onValueChange={setTypeFilter}>
                <SelectTrigger>
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="all">All Types ({typeCounts.all})</SelectItem>
                  <SelectItem value="ROLE">Roles ({typeCounts.ROLE || 0})</SelectItem>
                  <SelectItem value="PARTY">Parties ({typeCounts.PARTY || 0})</SelectItem>
                  <SelectItem value="LINK">Links ({typeCounts.LINK || 0})</SelectItem>
                  <SelectItem value="BACK_MATTER">
                    Back Matter ({typeCounts.BACK_MATTER || 0})
                  </SelectItem>
                  <SelectItem value="RESPONSIBLE_PARTY">
                    Responsible Parties ({typeCounts.RESPONSIBLE_PARTY || 0})
                  </SelectItem>
                </SelectContent>
              </Select>
            </div>
          </div>

          {/* Active Filters Summary */}
          {(searchQuery || typeFilter !== 'all') && (
            <div className="flex items-center gap-2 text-sm text-muted-foreground">
              <span>
                Showing {filteredElements.length} of {elements.length} elements
              </span>
              {(searchQuery || typeFilter !== 'all') && (
                <Button
                  variant="ghost"
                  size="sm"
                  onClick={() => {
                    setSearchQuery('');
                    setTypeFilter('all');
                  }}
                >
                  Clear Filters
                </Button>
              )}
            </div>
          )}
        </CardContent>
      </Card>

      {/* Loading State */}
      {isLoading && (
        <div className="flex items-center justify-center p-12">
          <Loader2 className="h-8 w-8 animate-spin text-muted-foreground" />
        </div>
      )}

      {/* Error State */}
      {error && (
        <Card className="border-red-200 bg-red-50 dark:bg-red-950">
          <CardContent className="flex items-center gap-3 p-6">
            <AlertCircle className="h-5 w-5 text-red-600" />
            <div>
              <p className="font-medium text-red-900 dark:text-red-100">Error loading elements</p>
              <p className="text-sm text-red-700 dark:text-red-300">{error}</p>
            </div>
            <Button variant="outline" size="sm" onClick={loadElements} className="ml-auto">
              Retry
            </Button>
          </CardContent>
        </Card>
      )}

      {/* Empty State */}
      {!isLoading && !error && filteredElements.length === 0 && (
        <Card>
          <CardContent className="flex flex-col items-center justify-center p-12 text-center">
            <FileText className="h-12 w-12 text-muted-foreground mb-4" />
            <h3 className="text-lg font-semibold mb-2">
              {searchQuery || typeFilter !== 'all' ? 'No elements found' : 'No elements yet'}
            </h3>
            <p className="text-muted-foreground mb-4">
              {searchQuery || typeFilter !== 'all'
                ? 'Try adjusting your filters or search query'
                : 'Create your first reusable element to get started'}
            </p>
            {elements.length === 0 && (
              <Button onClick={handleCreate}>
                <Plus className="mr-2 h-4 w-4" />
                Create First Element
              </Button>
            )}
          </CardContent>
        </Card>
      )}

      {/* Elements Grid */}
      {!isLoading && !error && filteredElements.length > 0 && (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          {filteredElements.map((element) => {
            const Icon = ELEMENT_TYPE_ICONS[element.type];
            return (
              <Card key={element.id} className="hover:shadow-md transition-shadow">
                <CardHeader>
                  <div className="flex items-start justify-between">
                    <div className="flex items-center gap-3">
                      <div className="p-2 rounded-lg bg-muted">
                        <Icon className="h-5 w-5" />
                      </div>
                      <div>
                        <CardTitle className="text-lg">{element.name}</CardTitle>
                        <Badge variant="secondary" className={ELEMENT_TYPE_COLORS[element.type]}>
                          {ELEMENT_TYPE_LABELS[element.type]}
                        </Badge>
                      </div>
                    </div>
                  </div>
                </CardHeader>
                <CardContent className="space-y-4">
                  {element.description && (
                    <CardDescription className="line-clamp-2">
                      {element.description}
                    </CardDescription>
                  )}

                  {/* Metadata */}
                  <div className="flex items-center gap-4 text-sm text-muted-foreground">
                    <div>
                      Used: <span className="font-medium">{element.useCount}</span> times
                    </div>
                    {element.shared && (
                      <Badge variant="outline" className="text-xs">
                        Shared
                      </Badge>
                    )}
                  </div>

                  {/* Actions */}
                  <div className="flex gap-2 pt-2">
                    <Button
                      variant="outline"
                      size="sm"
                      onClick={() => handleEdit(element)}
                      className="flex-1"
                    >
                      <Edit2 className="mr-2 h-3 w-3" />
                      Edit
                    </Button>
                    <Button
                      variant="outline"
                      size="sm"
                      onClick={() => handleDeleteClick(element)}
                      className="text-red-600 hover:text-red-700 hover:bg-red-50"
                    >
                      <Trash2 className="h-3 w-3" />
                    </Button>
                  </div>
                </CardContent>
              </Card>
            );
          })}
        </div>
      )}

      {/* Element Modal */}
      <ElementModal
        open={isModalOpen}
        onOpenChange={setIsModalOpen}
        element={editingElement}
        onSuccess={handleModalSuccess}
      />

      {/* Delete Confirmation Dialog */}
      <AlertDialog open={deleteDialogOpen} onOpenChange={setDeleteDialogOpen}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>Delete Element</AlertDialogTitle>
            <AlertDialogDescription>
              Are you sure you want to delete &quot;{elementToDelete?.name}&quot;? This action cannot be
              undone.
              {elementToDelete && elementToDelete.useCount > 0 && (
                <span className="block mt-2 text-orange-600 font-medium">
                  This element has been used {elementToDelete.useCount} time
                  {elementToDelete.useCount > 1 ? 's' : ''}.
                </span>
              )}
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel>Cancel</AlertDialogCancel>
            <AlertDialogAction
              onClick={handleDeleteConfirm}
              className="bg-red-600 hover:bg-red-700"
            >
              Delete
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </div>
  );
}
