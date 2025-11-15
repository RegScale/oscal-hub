'use client';

import { useState, useEffect } from 'react';
import { useParams, useRouter } from 'next/navigation';
import { Card, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Alert, AlertDescription } from '@/components/ui/alert';
import {
  ArrowLeft,
  FileText,
  Loader2
} from 'lucide-react';
import { apiClient } from '@/lib/api-client';
import type { AuthorizationTemplateResponse } from '@/types/oscal';
import { useAuth } from '@/contexts/AuthContext';
import { Footer } from '@/components/Footer';
import { MarkdownPreview } from '@/components/markdown-preview';
import { TemplateEditor } from '@/components/template-editor';
import { toast } from 'sonner';

export default function TemplateDetailPage() {
  const params = useParams();
  const router = useRouter();
  const { isAuthenticated, isLoading: authLoading, user } = useAuth();

  const templateId = params.templateId as string;

  const [template, setTemplate] = useState<AuthorizationTemplateResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [isEditing, setIsEditing] = useState(false);
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    if (isAuthenticated && templateId) {
      loadTemplate();
    }
  }, [isAuthenticated, templateId]);

  const loadTemplate = async () => {
    try {
      setLoading(true);
      setError(null);

      const data = await apiClient.getAuthorizationTemplate(parseInt(templateId));
      setTemplate(data);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load template');
      console.error('Error loading template:', err);
    } finally {
      setLoading(false);
    }
  };

  const handleSave = async (name: string, content: string) => {
    if (!template) return;

    try {
      setSaving(true);
      await apiClient.updateAuthorizationTemplate(template.id, { name, content });
      toast.success('Template updated successfully');
      await loadTemplate();
      setIsEditing(false);
    } catch (err) {
      console.error('Failed to update template:', err);
      toast.error('Failed to update template');
    } finally {
      setSaving(false);
    }
  };

  if (authLoading || loading) {
    return (
      <div className="min-h-screen bg-background flex items-center justify-center">
        <div className="text-center">
          <Loader2 className="h-12 w-12 animate-spin text-primary mx-auto mb-4" />
          <p className="text-muted-foreground">Loading template...</p>
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
              Please log in to access template details.
            </AlertDescription>
          </Alert>
        </div>
        <Footer />
      </div>
    );
  }

  if (error || !template) {
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
              {error || 'Template not found'}
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
            onClick={() => router.push('/authorizations?tab=templates')}
            className="inline-flex items-center gap-2 text-sm text-muted-foreground hover:text-foreground mb-4 transition-colors px-0"
          >
            <ArrowLeft className="h-4 w-4" />
            Back to Templates
          </Button>
          <div className="flex items-center">
            <FileText className="h-10 w-10 text-primary mr-4" />
            <div>
              <h1 className="text-4xl font-bold">{template.name}</h1>
              <p className="text-muted-foreground mt-2">
                Created by {template.createdBy} on{' '}
                {new Date(template.createdAt).toLocaleDateString()}
              </p>
            </div>
          </div>
        </div>

        <Card className="p-6">
          <div className="flex items-center justify-between mb-6">
            <CardTitle>
              {isEditing ? 'Edit Template' : 'Template Preview'}
            </CardTitle>
            <div className="flex gap-2">
              {!isEditing && (
                <Button
                  variant="outline"
                  onClick={() => setIsEditing(true)}
                  disabled={template.createdBy !== user?.username}
                >
                  Edit
                </Button>
              )}
            </div>
          </div>

          {isEditing ? (
            <TemplateEditor
              initialName={template.name}
              initialContent={template.content}
              onSave={handleSave}
              onCancel={() => setIsEditing(false)}
              isSaving={saving}
            />
          ) : (
            <MarkdownPreview
              content={template.content}
              height="600px"
            />
          )}
        </Card>
      </div>

      <Footer />
    </div>
  );
}
