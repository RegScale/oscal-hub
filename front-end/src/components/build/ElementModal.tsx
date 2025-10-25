'use client';

import { useState, useEffect } from 'react';
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter, DialogDescription } from '@/components/ui/dialog';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Textarea } from '@/components/ui/textarea';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { Checkbox } from '@/components/ui/checkbox';
import { apiClient } from '@/lib/api-client';
import type { ReusableElementRequest, ReusableElementResponse, ReusableElementType } from '@/types/oscal';
import { Loader2 } from 'lucide-react';

interface ElementModalProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  element?: ReusableElementResponse;
  onSuccess?: () => void;
}

const ELEMENT_TYPES: ReusableElementType[] = [
  'ROLE',
  'PARTY',
  'LINK',
  'BACK_MATTER',
  'RESPONSIBLE_PARTY',
];

const ELEMENT_TYPE_LABELS: Record<ReusableElementType, string> = {
  ROLE: 'Role',
  PARTY: 'Party',
  LINK: 'Link',
  BACK_MATTER: 'Back Matter',
  RESPONSIBLE_PARTY: 'Responsible Party',
};

const ELEMENT_TYPE_DESCRIPTIONS: Record<ReusableElementType, string> = {
  ROLE: 'A defined function or position in an organization',
  PARTY: 'An organization or person',
  LINK: 'A reference to a resource',
  BACK_MATTER: 'Supporting material such as citations or resources',
  RESPONSIBLE_PARTY: 'A party responsible for a specific role',
};

export function ElementModal({ open, onOpenChange, element, onSuccess }: ElementModalProps) {
  const isEdit = !!element;

  const [formData, setFormData] = useState<ReusableElementRequest>({
    type: 'ROLE',
    name: '',
    jsonContent: '',
    description: '',
    isShared: false,
  });

  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [jsonError, setJsonError] = useState<string | null>(null);

  // Initialize form data when element changes
  useEffect(() => {
    if (element) {
      setFormData({
        type: element.type,
        name: element.name,
        jsonContent: element.jsonContent,
        description: element.description || '',
        isShared: element.isShared || false,
      });
    } else {
      setFormData({
        type: 'ROLE',
        name: '',
        jsonContent: '',
        description: '',
        isShared: false,
      });
    }
    setError(null);
    setJsonError(null);
  }, [element, open]);

  const validateJson = (json: string): boolean => {
    if (!json.trim()) {
      setJsonError('JSON content is required');
      return false;
    }

    try {
      JSON.parse(json);
      setJsonError(null);
      return true;
    } catch (e) {
      setJsonError(`Invalid JSON: ${e instanceof Error ? e.message : 'Unknown error'}`);
      return false;
    }
  };

  const handleJsonChange = (value: string) => {
    setFormData({ ...formData, jsonContent: value });
    if (value.trim()) {
      validateJson(value);
    } else {
      setJsonError(null);
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);

    // Validate JSON before submission
    if (!validateJson(formData.jsonContent)) {
      return;
    }

    // Validate required fields
    if (!formData.name.trim()) {
      setError('Name is required');
      return;
    }

    setIsSubmitting(true);

    try {
      if (isEdit && element) {
        await apiClient.updateReusableElement(element.id, formData);
      } else {
        await apiClient.createReusableElement(formData);
      }

      onOpenChange(false);

      if (onSuccess) {
        onSuccess();
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to save element');
      console.error('Error saving element:', err);
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleCancel = () => {
    onOpenChange(false);
  };

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-w-3xl max-h-[90vh] overflow-y-auto">
        <DialogHeader>
          <DialogTitle>
            {isEdit ? 'Edit Reusable Element' : 'Create Reusable Element'}
          </DialogTitle>
          <DialogDescription>
            {isEdit
              ? 'Update the reusable OSCAL element details below.'
              : 'Create a new reusable OSCAL element that can be used across multiple component definitions.'}
          </DialogDescription>
        </DialogHeader>

        <form onSubmit={handleSubmit} className="space-y-6">
          {/* Element Type */}
          <div className="space-y-2">
            <Label htmlFor="type">Element Type *</Label>
            <Select
              value={formData.type}
              onValueChange={(value) => setFormData({ ...formData, type: value as ReusableElementType })}
              disabled={isEdit} // Don't allow changing type on edit
            >
              <SelectTrigger id="type">
                <SelectValue placeholder="Select element type" />
              </SelectTrigger>
              <SelectContent>
                {ELEMENT_TYPES.map((type) => (
                  <SelectItem key={type} value={type}>
                    <div className="flex flex-col items-start">
                      <span className="font-medium">{ELEMENT_TYPE_LABELS[type]}</span>
                      <span className="text-xs text-muted-foreground">
                        {ELEMENT_TYPE_DESCRIPTIONS[type]}
                      </span>
                    </div>
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
            {isEdit && (
              <p className="text-xs text-muted-foreground">
                Element type cannot be changed after creation
              </p>
            )}
          </div>

          {/* Name */}
          <div className="space-y-2">
            <Label htmlFor="name">Name *</Label>
            <Input
              id="name"
              value={formData.name}
              onChange={(e) => setFormData({ ...formData, name: e.target.value })}
              placeholder="e.g., System Administrator, ACME Corporation"
              required
            />
            <p className="text-xs text-muted-foreground">
              A descriptive name for this element
            </p>
          </div>

          {/* Description */}
          <div className="space-y-2">
            <Label htmlFor="description">Description</Label>
            <Textarea
              id="description"
              value={formData.description}
              onChange={(e) => setFormData({ ...formData, description: e.target.value })}
              placeholder="Optional description of this element's purpose..."
              rows={3}
            />
          </div>

          {/* JSON Content */}
          <div className="space-y-2">
            <Label htmlFor="jsonContent">JSON Content *</Label>
            <Textarea
              id="jsonContent"
              value={formData.jsonContent}
              onChange={(e) => handleJsonChange(e.target.value)}
              placeholder='{"id": "role-1", "title": "System Administrator", ...}'
              rows={12}
              className={`font-mono text-sm ${jsonError ? 'border-red-500' : ''}`}
              required
            />
            {jsonError && (
              <p className="text-sm text-red-600">{jsonError}</p>
            )}
            <p className="text-xs text-muted-foreground">
              Valid OSCAL JSON for this element type
            </p>
          </div>

          {/* Is Shared */}
          <div className="flex items-center space-x-2">
            <Checkbox
              id="isShared"
              checked={formData.isShared}
              onCheckedChange={(checked) =>
                setFormData({ ...formData, isShared: checked === true })
              }
            />
            <div className="space-y-1">
              <Label htmlFor="isShared" className="cursor-pointer">
                Share with other users
              </Label>
              <p className="text-xs text-muted-foreground">
                Allow other users to view and use this element
              </p>
            </div>
          </div>

          {/* Error Display */}
          {error && (
            <div className="rounded-md bg-red-50 p-4 border border-red-200">
              <p className="text-sm text-red-800">{error}</p>
            </div>
          )}

          <DialogFooter>
            <Button
              type="button"
              variant="outline"
              onClick={handleCancel}
              disabled={isSubmitting}
            >
              Cancel
            </Button>
            <Button type="submit" disabled={isSubmitting || !!jsonError}>
              {isSubmitting ? (
                <>
                  <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                  {isEdit ? 'Updating...' : 'Creating...'}
                </>
              ) : (
                <>{isEdit ? 'Update Element' : 'Create Element'}</>
              )}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}
