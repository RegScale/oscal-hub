'use client';

import { useState, useEffect, useImperativeHandle, forwardRef } from 'react';
import { Library, Search, Download, Tag, Upload, RefreshCw, Eye } from 'lucide-react';
import { Card, CardHeader, CardTitle, CardContent } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { Alert, AlertDescription } from '@/components/ui/alert';
import { Input } from '@/components/ui/input';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select';
import { apiClient } from '@/lib/api-client';
import type { LibraryItem } from '@/types/oscal';

interface LibrarySelectorProps {
  onItemSelect: (item: LibraryItem, content: string) => void;
  onUploadNew: () => void;
  showUploadButton?: boolean;
}

export interface LibrarySelectorRef {
  refresh: () => void;
}

export const LibrarySelector = forwardRef<LibrarySelectorRef, LibrarySelectorProps>(
  function LibrarySelector({ onItemSelect, onUploadNew, showUploadButton = true }, ref) {
  const [libraryItems, setLibraryItems] = useState<LibraryItem[]>([]);
  const [filteredItems, setFilteredItems] = useState<LibraryItem[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [selectedItemId, setSelectedItemId] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [searchQuery, setSearchQuery] = useState<string>('');
  const [oscalTypeFilter, setOscalTypeFilter] = useState<string>('all');
  const [tagFilter, setTagFilter] = useState<string>('all');
  const [availableTags, setAvailableTags] = useState<string[]>([]);

  const loadLibraryItems = async () => {
    setIsLoading(true);
    setError(null);
    try {
      const items = await apiClient.getAllLibraryItems();
      setLibraryItems(items);

      // Extract unique tags
      const tags = new Set<string>();
      items.forEach(item => {
        item.tags.forEach(tag => tags.add(tag));
      });
      setAvailableTags(Array.from(tags).sort());
    } catch (err) {
      setError('Failed to load library items');
      console.error(err);
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    loadLibraryItems();
  }, []);

  // Expose refresh method to parent
  useImperativeHandle(ref, () => ({
    refresh: loadLibraryItems,
  }));

  // Apply filters and search
  useEffect(() => {
    let filtered = [...libraryItems];

    // Filter by OSCAL type
    if (oscalTypeFilter !== 'all') {
      filtered = filtered.filter(item => item.oscalType.toLowerCase() === oscalTypeFilter);
    }

    // Filter by tag
    if (tagFilter !== 'all') {
      filtered = filtered.filter(item => item.tags.includes(tagFilter));
    }

    // Filter by search query
    if (searchQuery.trim()) {
      const query = searchQuery.toLowerCase();
      filtered = filtered.filter(item => {
        const title = item.title.toLowerCase();
        const description = item.description?.toLowerCase() || '';
        const tags = item.tags.join(' ').toLowerCase();
        const oscalType = item.oscalType.toLowerCase();

        return title.includes(query) ||
               description.includes(query) ||
               tags.includes(query) ||
               oscalType.includes(query);
      });
    }

    setFilteredItems(filtered);
  }, [libraryItems, searchQuery, oscalTypeFilter, tagFilter]);

  const handleItemClick = async (item: LibraryItem) => {
    try {
      setSelectedItemId(item.itemId);
      const content = await apiClient.getLibraryItemContent(item.itemId);
      if (content) {
        onItemSelect(item, content);
      } else {
        setError('Failed to load library item content');
      }
    } catch (err) {
      setError('Failed to load library item');
      console.error(err);
    }
  };

  const formatDate = (dateString: string): string => {
    const date = new Date(dateString);
    return date.toLocaleDateString();
  };

  const getOscalTypeColor = (oscalType: string): string => {
    const colors: Record<string, string> = {
      'catalog': 'bg-blue-500/10 text-blue-500',
      'profile': 'bg-purple-500/10 text-purple-500',
      'component-definition': 'bg-green-500/10 text-green-500',
      'system-security-plan': 'bg-orange-500/10 text-orange-500',
      'assessment-plan': 'bg-yellow-500/10 text-yellow-500',
      'assessment-results': 'bg-red-500/10 text-red-500',
      'plan-of-action-and-milestones': 'bg-pink-500/10 text-pink-500',
    };
    return colors[oscalType.toLowerCase()] || 'bg-gray-500/10 text-gray-500';
  };

  const uniqueOscalTypes = Array.from(new Set(libraryItems.map(item => item.oscalType))).sort();

  return (
    <Card>
      <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-4">
        <div className="flex items-center gap-2">
          <Library className="h-5 w-5 text-primary" />
          <CardTitle>OSCAL Library</CardTitle>
        </div>
        <div className="flex gap-2">
          <Button
            variant="ghost"
            size="sm"
            onClick={loadLibraryItems}
            disabled={isLoading}
            aria-label="Refresh library"
          >
            <RefreshCw className={`h-4 w-4 ${isLoading ? 'animate-spin' : ''}`} />
          </Button>
          {showUploadButton && (
            <Button
              variant="outline"
              size="sm"
              onClick={onUploadNew}
              aria-label="Upload to library"
            >
              <Upload className="h-4 w-4 mr-2" />
              Add to Library
            </Button>
          )}
        </div>
      </CardHeader>
      <CardContent className="space-y-3">
        {error && (
          <Alert variant="destructive">
            <AlertDescription>{error}</AlertDescription>
          </Alert>
        )}

        {/* Search and filters */}
        {!isLoading && libraryItems.length > 0 && (
          <div className="space-y-2">
            {/* Search input */}
            <div className="relative">
              <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 h-4 w-4 text-muted-foreground" />
              <Input
                type="text"
                placeholder="Search by title, description, or tags..."
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                className="pl-9"
              />
            </div>

            {/* Filters */}
            <div className="grid grid-cols-2 gap-2">
              <Select value={oscalTypeFilter} onValueChange={setOscalTypeFilter}>
                <SelectTrigger>
                  <SelectValue placeholder="Filter by type" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="all">All Types</SelectItem>
                  {uniqueOscalTypes.map(type => (
                    <SelectItem key={type} value={type.toLowerCase()}>
                      {type.replace(/-/g, ' ')}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>

              <Select value={tagFilter} onValueChange={setTagFilter}>
                <SelectTrigger>
                  <SelectValue placeholder="Filter by tag" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="all">All Tags</SelectItem>
                  {availableTags.map(tag => (
                    <SelectItem key={tag} value={tag}>
                      {tag}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
          </div>
        )}

        {isLoading ? (
          <div className="text-center py-8 text-muted-foreground">
            <RefreshCw className="h-8 w-8 animate-spin mx-auto mb-2" />
            Loading library...
          </div>
        ) : libraryItems.length === 0 ? (
          <div className="text-center py-8 text-muted-foreground">
            <Library className="h-12 w-12 mx-auto mb-2 opacity-30" />
            <p className="text-sm">Library is empty</p>
            {showUploadButton && (
              <p className="text-xs mt-1">Add documents to get started</p>
            )}
          </div>
        ) : filteredItems.length === 0 ? (
          <div className="text-center py-8 text-muted-foreground">
            <Search className="h-12 w-12 mx-auto mb-2 opacity-30" />
            <p className="text-sm">No items match your filters</p>
            <p className="text-xs mt-1">Try adjusting your search or filters</p>
          </div>
        ) : (
          <div className="space-y-3 max-h-96 overflow-y-auto">
            {filteredItems.map((item) => (
              <div
                key={item.itemId}
                onClick={() => handleItemClick(item)}
                className={`p-4 rounded-lg border cursor-pointer transition-colors hover:bg-accent ${
                  selectedItemId === item.itemId ? 'bg-accent border-primary' : ''
                }`}
                role="button"
                tabIndex={0}
                aria-label={`Select library item ${item.title}`}
              >
                <div className="space-y-2">
                  {/* Title and Type */}
                  <div className="flex items-start justify-between gap-2">
                    <div className="flex-1 min-w-0">
                      <h4 className="font-semibold text-sm truncate mb-1">{item.title}</h4>
                      {item.description && (
                        <p className="text-xs text-muted-foreground line-clamp-2">
                          {item.description}
                        </p>
                      )}
                    </div>
                    <Badge className={`text-xs flex-shrink-0 ${getOscalTypeColor(item.oscalType)}`}>
                      {item.oscalType.replace(/-/g, ' ')}
                    </Badge>
                  </div>

                  {/* Tags */}
                  {item.tags.length > 0 && (
                    <div className="flex flex-wrap gap-1">
                      {item.tags.map((tag) => (
                        <Badge key={tag} variant="outline" className="text-xs">
                          <Tag className="h-3 w-3 mr-1" />
                          {tag}
                        </Badge>
                      ))}
                    </div>
                  )}

                  {/* Metadata */}
                  <div className="flex items-center gap-3 text-xs text-muted-foreground">
                    <span className="flex items-center gap-1">
                      <Download className="h-3 w-3" />
                      {item.downloadCount}
                    </span>
                    <span className="flex items-center gap-1">
                      <Eye className="h-3 w-3" />
                      {item.viewCount}
                    </span>
                    {item.currentVersion && (
                      <Badge variant="secondary" className="text-xs font-mono">
                        v{item.currentVersion.versionNumber}
                      </Badge>
                    )}
                    <span className="ml-auto">
                      Updated {formatDate(item.updatedAt)}
                    </span>
                  </div>
                </div>
              </div>
            ))}
          </div>
        )}

        {/* Results count */}
        {!isLoading && libraryItems.length > 0 && (
          <div className="text-xs text-muted-foreground text-center pt-2 border-t">
            Showing {filteredItems.length} of {libraryItems.length} items
          </div>
        )}
      </CardContent>
    </Card>
  );
});

LibrarySelector.displayName = 'LibrarySelector';
