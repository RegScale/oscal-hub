'use client';

import { useState, useEffect } from 'react';
import { useParams, useRouter } from 'next/navigation';
import { Card, CardContent } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Alert, AlertDescription } from '@/components/ui/alert';
import { ArrowLeft, Hammer, Loader2 } from 'lucide-react';
import { apiClient } from '@/lib/api-client';
import type { ComponentDefinitionResponse } from '@/types/oscal';
import { useAuth } from '@/contexts/AuthContext';
import { Footer } from '@/components/Footer';
import { ComponentBuilderWizard } from '@/components/build/ComponentBuilderWizard';

export default function ComponentDetailPage() {
  const params = useParams();
  const router = useRouter();
  const { isAuthenticated, isLoading: authLoading } = useAuth();

  const componentId = params.componentId as string;

  const [component, setComponent] = useState<ComponentDefinitionResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (isAuthenticated && componentId) {
      loadComponent();
    }
  }, [isAuthenticated, componentId]);

  const loadComponent = async () => {
    try {
      setLoading(true);
      setError(null);

      // Fetch all components and find the one we need
      const components = await apiClient.getUserComponentDefinitions();
      const foundComponent = components.find(c => c.id === parseInt(componentId));

      if (foundComponent) {
        setComponent(foundComponent);
      } else {
        setError(`Component with ID ${componentId} not found`);
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load component');
      console.error('Error loading component:', err);
    } finally {
      setLoading(false);
    }
  };

  const handleSaveComplete = () => {
    // Navigate back to build page after successful save
    router.push('/build');
  };

  if (authLoading || loading) {
    return (
      <div className="min-h-screen bg-background flex items-center justify-center">
        <div className="text-center">
          <Loader2 className="h-12 w-12 animate-spin text-primary mx-auto mb-4" />
          <p className="text-muted-foreground">Loading component...</p>
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
              Please log in to access component details.
            </AlertDescription>
          </Alert>
        </div>
        <Footer />
      </div>
    );
  }

  if (error || !component) {
    return (
      <div className="min-h-screen bg-background">
        <div className="container mx-auto py-12 px-4">
          <Button
            variant="ghost"
            onClick={() => router.push('/build')}
            className="inline-flex items-center gap-2 text-sm text-muted-foreground hover:text-foreground mb-4 transition-colors px-0"
          >
            <ArrowLeft className="h-4 w-4" />
            Back to Build
          </Button>

          <Alert variant="destructive">
            <AlertDescription>
              {error || 'Component not found'}
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
            onClick={() => router.push('/build')}
            className="inline-flex items-center gap-2 text-sm text-muted-foreground hover:text-foreground mb-4 transition-colors px-0"
          >
            <ArrowLeft className="h-4 w-4" />
            Back to Build
          </Button>
          <div className="flex items-center">
            <Hammer className="h-10 w-10 text-primary mr-4" />
            <div>
              <h1 className="text-4xl font-bold">Edit Component: {component.title}</h1>
              <p className="text-muted-foreground mt-2">
                Update your OSCAL component definition
              </p>
            </div>
          </div>
        </div>

        <ComponentBuilderWizard
          editingComponent={component}
          onSaveComplete={handleSaveComplete}
        />
      </div>

      <Footer />
    </div>
  );
}
