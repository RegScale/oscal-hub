'use client';

import { useState } from 'react';
import { useRouter } from 'next/navigation';
import { Card, CardHeader, CardTitle, CardDescription, CardContent } from '@/components/ui/card';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';
import { Button } from '@/components/ui/button';
import { Alert, AlertDescription } from '@/components/ui/alert';
import {
  Hammer,
  Plus,
  Library as LibraryIcon,
  FileText,
  ArrowLeft,
  Blocks,
  Users,
  Link as LinkIcon,
  FileBox
} from 'lucide-react';
import { useAuth } from '@/contexts/AuthContext';
import { Footer } from '@/components/Footer';
import { ElementLibrary } from '@/components/build/ElementLibrary';
import { ComponentBuilderWizard } from '@/components/build/ComponentBuilderWizard';
import { ComponentList } from '@/components/build/ComponentList';
import type { ComponentDefinitionResponse } from '@/types/oscal';

export default function BuildPage() {
  const router = useRouter();
  const { isAuthenticated, isLoading } = useAuth();
  const [activeTab, setActiveTab] = useState('components');
  const [editingComponent, setEditingComponent] = useState<ComponentDefinitionResponse | null>(null);

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
              Please log in to access the component builder.
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
            <Hammer className="h-10 w-10 text-primary mr-4" />
            <div>
              <h1 className="text-4xl font-bold">OSCAL Component Builder</h1>
              <p className="text-muted-foreground mt-2">
                Visually create and manage OSCAL component definitions
              </p>
            </div>
          </div>
        </div>

        <Tabs value={activeTab} onValueChange={setActiveTab} className="space-y-6">
          <TabsList className="grid w-full grid-cols-3">
            <TabsTrigger value="components">
              <Blocks className="h-4 w-4 mr-2" />
              My Components
            </TabsTrigger>
            <TabsTrigger value="create">
              <Plus className="h-4 w-4 mr-2" />
              Create New
            </TabsTrigger>
            <TabsTrigger value="library">
              <LibraryIcon className="h-4 w-4 mr-2" />
              Element Library
            </TabsTrigger>
          </TabsList>

          {/* My Components Tab */}
          <TabsContent value="components" className="space-y-6">
            <ComponentList
              onCreateNew={() => {
                setEditingComponent(null);
                setActiveTab('create');
              }}
              onEdit={(component) => {
                setEditingComponent(component);
                setActiveTab('create');
              }}
            />
          </TabsContent>

          {/* Create New Tab */}
          <TabsContent value="create" className="space-y-6">
            <ComponentBuilderWizard
              editingComponent={editingComponent}
              onSaveComplete={() => {
                setEditingComponent(null);
                setActiveTab('components');
              }}
            />
          </TabsContent>

          {/* Element Library Tab */}
          <TabsContent value="library" className="space-y-6">
            <ElementLibrary />
          </TabsContent>
        </Tabs>
      </div>

      <Footer />
    </div>
  );
}
