'use client';

import { useState } from 'react';
import { Card } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Badge } from '@/components/ui/badge';
import type { AuthorizationTemplateResponse } from '@/types/oscal';

interface TemplateListProps {
  templates: AuthorizationTemplateResponse[];
  onSelectTemplate: (template: AuthorizationTemplateResponse) => void;
  onEditTemplate: (template: AuthorizationTemplateResponse) => void;
  onDeleteTemplate: (templateId: number) => void;
  onCreateNew: () => void;
  isLoading?: boolean;
  currentUsername?: string;
}

export function TemplateList({
  templates,
  onSelectTemplate,
  onEditTemplate,
  onDeleteTemplate,
  onCreateNew,
  isLoading = false,
  currentUsername,
}: TemplateListProps) {
  const [searchTerm, setSearchTerm] = useState('');

  const filteredTemplates = templates.filter((template) =>
    template.name.toLowerCase().includes(searchTerm.toLowerCase())
  );

  const formatDate = (dateString: string) => {
    return new Date(dateString).toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
    });
  };

  return (
    <div className="space-y-4">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h2 className="text-2xl font-bold">Authorization Templates</h2>
          <p className="text-gray-600">Create and manage authorization templates</p>
        </div>
        <Button onClick={onCreateNew}>Create New Template</Button>
      </div>

      {/* Search */}
      <div className="max-w-md">
        <Input
          type="search"
          placeholder="Search templates..."
          value={searchTerm}
          onChange={(e) => setSearchTerm(e.target.value)}
        />
      </div>

      {/* Templates List */}
      {isLoading ? (
        <div className="text-center py-12">
          <p className="text-gray-500">Loading templates...</p>
        </div>
      ) : filteredTemplates.length === 0 ? (
        <Card className="p-12 text-center">
          <p className="text-gray-500 mb-4">
            {searchTerm ? 'No templates match your search.' : 'No templates yet.'}
          </p>
          {!searchTerm && (
            <Button onClick={onCreateNew}>Create Your First Template</Button>
          )}
        </Card>
      ) : (
        <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
          {filteredTemplates.map((template) => (
            <Card
              key={template.id}
              className="p-6 hover:shadow-lg transition-shadow cursor-pointer"
              onClick={() => onSelectTemplate(template)}
            >
              <div className="space-y-3">
                <div className="flex items-start justify-between">
                  <h3 className="font-semibold text-lg truncate flex-1">
                    {template.name}
                  </h3>
                  {template.variables && template.variables.length > 0 && (
                    <Badge variant="secondary" className="ml-2">
                      {template.variables.length} vars
                    </Badge>
                  )}
                </div>

                <div className="text-sm text-gray-600 space-y-1">
                  <p>Created by: {template.createdBy}</p>
                  <p>Created: {formatDate(template.createdAt)}</p>
                  {template.lastUpdatedAt !== template.createdAt && (
                    <p className="text-amber-600">
                      Updated: {formatDate(template.lastUpdatedAt)}
                    </p>
                  )}
                </div>

                {template.variables && template.variables.length > 0 && (
                  <div className="pt-2 border-t border-gray-100">
                    <p className="text-xs text-gray-500 mb-2">Variables:</p>
                    <div className="flex flex-wrap gap-1">
                      {template.variables.slice(0, 3).map((variable) => (
                        <span
                          key={variable}
                          className="inline-flex items-center px-2 py-0.5 rounded text-xs bg-amber-50 text-amber-700"
                        >
                          {variable}
                        </span>
                      ))}
                      {template.variables.length > 3 && (
                        <span className="inline-flex items-center px-2 py-0.5 rounded text-xs bg-gray-100 text-gray-600">
                          +{template.variables.length - 3} more
                        </span>
                      )}
                    </div>
                  </div>
                )}

                <div className="flex items-center gap-2 pt-3 border-t border-gray-100">
                  {currentUsername === template.createdBy ? (
                    <>
                      <Button
                        size="sm"
                        variant="outline"
                        onClick={(e) => {
                          e.stopPropagation();
                          onEditTemplate(template);
                        }}
                      >
                        Edit
                      </Button>
                      <Button
                        size="sm"
                        variant="outline"
                        className="text-red-600 hover:text-red-700 hover:bg-red-50"
                        onClick={(e) => {
                          e.stopPropagation();
                          if (confirm('Are you sure you want to delete this template?')) {
                            onDeleteTemplate(template.id);
                          }
                        }}
                      >
                        Delete
                      </Button>
                    </>
                  ) : (
                    <p className="text-xs text-gray-500">
                      Only the creator can edit or delete this template
                    </p>
                  )}
                </div>
              </div>
            </Card>
          ))}
        </div>
      )}
    </div>
  );
}
