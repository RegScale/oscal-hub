'use client';

import { useState } from 'react';
import { useRouter } from 'next/navigation';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';
import { Button } from '@/components/ui/button';
import { Alert, AlertDescription } from '@/components/ui/alert';
import {
  Tooltip,
  TooltipContent,
  TooltipProvider,
  TooltipTrigger,
} from '@/components/ui/tooltip';
import {
  Hammer,
  Library as LibraryIcon,
  ArrowLeft,
  Blocks,
  Layers,
  Library,
  Boxes,
  Server,
  ClipboardList,
  ClipboardCheck,
  Target,
  Sparkles,
  ChevronRight,
} from 'lucide-react';
import { useAuth } from '@/contexts/AuthContext';
import { AiFeatureGate } from '@/components/ai/AiFeatureGate';
import { Footer } from '@/components/Footer';
import { ElementLibrary } from '@/components/build/ElementLibrary';
import { ComponentBuilderWizard } from '@/components/build/ComponentBuilderWizard';
import { ComponentList } from '@/components/build/ComponentList';
import { CatalogBuilderWizard } from '@/components/build/CatalogBuilderWizard';
import { ProfileBuilderWizard } from '@/components/build/ProfileBuilderWizard';
import { OscalDocumentWizard } from '@/components/build/OscalDocumentWizard';
import { BuiltDocList } from '@/components/build/BuiltDocList';
import type { ComponentDefinitionResponse } from '@/types/oscal';
import type {
  CatalogResponse,
  ProfileBuildResponse,
  OscalDocumentResponse,
  GenericOscalModelSlug,
} from '@/types/oscal-models';

type Section =
  | 'catalogs'
  | 'profiles'
  | 'components'
  | 'ssp'
  | 'assessment-plan'
  | 'assessment-results'
  | 'poam'
  | 'library';
type Mode = 'list' | 'create';

const GENERIC_SECTIONS: Record<
  'ssp' | 'assessment-plan' | 'assessment-results' | 'poam',
  { slug: GenericOscalModelSlug; label: string; icon: typeof Library }
> = {
  ssp: { slug: 'system-security-plan', label: 'SSPs', icon: Server },
  'assessment-plan': { slug: 'assessment-plan', label: 'Assessment Plans', icon: ClipboardList },
  'assessment-results': { slug: 'assessment-results', label: 'Assessment Results', icon: ClipboardCheck },
  poam: { slug: 'plan-of-action-and-milestones', label: 'POA&Ms', icon: Target },
};

export default function BuildPage() {
  const router = useRouter();
  const { isAuthenticated, isLoading, user } = useAuth();
  const orgId = user?.organizationId ?? null;

  const [section, setSection] = useState<Section>('catalogs');
  const [mode, setMode] = useState<Mode>('list');
  const [reloadKey, setReloadKey] = useState(0);

  const [editingCatalog, setEditingCatalog] = useState<CatalogResponse | null>(null);
  const [editingProfile, setEditingProfile] = useState<ProfileBuildResponse | null>(null);
  const [editingComponent, setEditingComponent] = useState<ComponentDefinitionResponse | null>(null);
  const [editingDoc, setEditingDoc] = useState<OscalDocumentResponse | null>(null);

  if (isLoading) {
    return (
      <div className="min-h-screen bg-background flex items-center justify-center">
        <div className="text-center">
          <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-primary mx-auto mb-4" />
          <p className="text-muted-foreground">Loading…</p>
        </div>
      </div>
    );
  }

  if (!isAuthenticated) {
    return (
      <div className="min-h-screen bg-background">
        <div className="container mx-auto py-12 px-4">
          <Alert>
            <AlertDescription>Please log in to access the OSCAL builder.</AlertDescription>
          </Alert>
        </div>
        <Footer />
      </div>
    );
  }

  const switchSection = (next: Section) => {
    setSection(next);
    setMode('list');
    setEditingCatalog(null);
    setEditingProfile(null);
    setEditingComponent(null);
    setEditingDoc(null);
  };

  const onSaveComplete = () => {
    setMode('list');
    setEditingCatalog(null);
    setEditingProfile(null);
    setEditingComponent(null);
    setEditingDoc(null);
    setReloadKey((k) => k + 1);
  };

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
              <h1 className="text-4xl font-bold">OSCAL Builder</h1>
              <p className="text-muted-foreground mt-2">
                Visually create and manage every OSCAL model: catalogs, profiles, component
                definitions, system security plans, assessment plans, assessment results, and POA&amp;Ms.
              </p>
            </div>
          </div>
        </div>

        <AiFeatureGate organizationId={orgId}>
          <button
            onClick={() => router.push('/ai/wizard')}
            className="group w-full mb-8 rounded-lg border border-indigo-200 dark:border-indigo-900 bg-gradient-to-r from-indigo-50 to-purple-50 dark:from-indigo-950/40 dark:to-purple-950/40 p-5 text-left transition-all hover:shadow-md hover:border-indigo-400 dark:hover:border-indigo-700"
          >
            <div className="flex items-center justify-between gap-4">
              <div className="flex items-center gap-4">
                <div className="flex h-12 w-12 items-center justify-center rounded-lg bg-indigo-100 dark:bg-indigo-900/50">
                  <Sparkles className="h-6 w-6 text-indigo-600 dark:text-indigo-400" />
                </div>
                <div>
                  <h3 className="text-lg font-semibold">Generate with AI</h3>
                  <p className="text-sm text-muted-foreground">
                    Draft a catalog, profile, component-definition, SSP, or POA&amp;M from a PDF, URL, or description.
                  </p>
                </div>
              </div>
              <ChevronRight className="h-5 w-5 text-muted-foreground group-hover:text-foreground transition-colors" />
            </div>
          </button>
        </AiFeatureGate>

        <Tabs value={section} onValueChange={(v) => switchSection(v as Section)} className="space-y-6">
          <TooltipProvider delayDuration={200}>
            <TabsList className="grid w-full grid-cols-4 lg:grid-cols-8">
              <TabsTrigger value="catalogs">
                <Library className="h-4 w-4 mr-1" />
                Catalogs
              </TabsTrigger>
              <TabsTrigger value="profiles">
                <Layers className="h-4 w-4 mr-1" />
                Profiles
              </TabsTrigger>
              <TabsTrigger value="components">
                <Blocks className="h-4 w-4 mr-1" />
                Components
              </TabsTrigger>
              <Tooltip>
                <TooltipTrigger asChild>
                  <TabsTrigger value="ssp">
                    <Server className="h-4 w-4 mr-1" />
                    SSP
                  </TabsTrigger>
                </TooltipTrigger>
                <TooltipContent>System Security Plan</TooltipContent>
              </Tooltip>
              <Tooltip>
                <TooltipTrigger asChild>
                  <TabsTrigger value="assessment-plan">
                    <ClipboardList className="h-4 w-4 mr-1" />
                    AP
                  </TabsTrigger>
                </TooltipTrigger>
                <TooltipContent>Assessment Plan</TooltipContent>
              </Tooltip>
              <Tooltip>
                <TooltipTrigger asChild>
                  <TabsTrigger value="assessment-results">
                    <ClipboardCheck className="h-4 w-4 mr-1" />
                    AR
                  </TabsTrigger>
                </TooltipTrigger>
                <TooltipContent>Assessment Results</TooltipContent>
              </Tooltip>
              <Tooltip>
                <TooltipTrigger asChild>
                  <TabsTrigger value="poam">
                    <Target className="h-4 w-4 mr-1" />
                    POA&amp;M
                  </TabsTrigger>
                </TooltipTrigger>
                <TooltipContent>Plan of Action and Milestones</TooltipContent>
              </Tooltip>
              <TabsTrigger value="library">
                <LibraryIcon className="h-4 w-4 mr-1" />
                Library
              </TabsTrigger>
            </TabsList>
          </TooltipProvider>

          {/* Catalogs */}
          <TabsContent value="catalogs" className="space-y-6">
            {mode === 'list' ? (
              <BuiltDocList
                docType="catalog"
                reloadKey={reloadKey}
                onCreateNew={() => {
                  setEditingCatalog(null);
                  setMode('create');
                }}
                onEdit={(doc) => {
                  setEditingCatalog(doc as CatalogResponse);
                  setMode('create');
                }}
              />
            ) : (
              <CatalogBuilderWizard
                editingCatalog={editingCatalog}
                onSaveComplete={onSaveComplete}
                onCancel={() => {
                  setMode('list');
                  setEditingCatalog(null);
                }}
              />
            )}
          </TabsContent>

          {/* Profiles */}
          <TabsContent value="profiles" className="space-y-6">
            {mode === 'list' ? (
              <BuiltDocList
                docType="profile"
                reloadKey={reloadKey}
                onCreateNew={() => {
                  setEditingProfile(null);
                  setMode('create');
                }}
                onEdit={(doc) => {
                  setEditingProfile(doc as ProfileBuildResponse);
                  setMode('create');
                }}
              />
            ) : (
              <ProfileBuilderWizard
                editingProfile={editingProfile}
                onSaveComplete={onSaveComplete}
                onCancel={() => {
                  setMode('list');
                  setEditingProfile(null);
                }}
              />
            )}
          </TabsContent>

          {/* Components (existing flow) */}
          <TabsContent value="components" className="space-y-6">
            {mode === 'list' ? (
              <ComponentList
                onCreateNew={() => {
                  setEditingComponent(null);
                  setMode('create');
                }}
                onEdit={(comp) => {
                  setEditingComponent(comp);
                  setMode('create');
                }}
              />
            ) : (
              <div className="space-y-3">
                <Button
                  variant="ghost"
                  size="sm"
                  onClick={() => {
                    setMode('list');
                    setEditingComponent(null);
                  }}
                >
                  <ArrowLeft className="h-4 w-4 mr-1" />
                  Back to components
                </Button>
                <ComponentBuilderWizard
                  editingComponent={editingComponent}
                  onSaveComplete={onSaveComplete}
                />
              </div>
            )}
          </TabsContent>

          {/* Generic OSCAL document tabs (SSP, AP, AR, POA&M) */}
          {(['ssp', 'assessment-plan', 'assessment-results', 'poam'] as const).map((key) => {
            const cfg = GENERIC_SECTIONS[key];
            return (
              <TabsContent key={key} value={key} className="space-y-6">
                {mode === 'list' ? (
                  <BuiltDocList
                    docType={cfg.slug}
                    reloadKey={reloadKey}
                    onCreateNew={() => {
                      setEditingDoc(null);
                      setMode('create');
                    }}
                    onEdit={(doc) => {
                      setEditingDoc(doc as OscalDocumentResponse);
                      setMode('create');
                    }}
                  />
                ) : (
                  <OscalDocumentWizard
                    modelType={cfg.slug}
                    editingDocument={editingDoc}
                    onSaveComplete={onSaveComplete}
                    onCancel={() => {
                      setMode('list');
                      setEditingDoc(null);
                    }}
                  />
                )}
              </TabsContent>
            );
          })}

          {/* Element library */}
          <TabsContent value="library" className="space-y-6">
            <Card>
              <CardHeader>
                <div className="flex items-center gap-3">
                  <Boxes className="h-6 w-6 text-primary" />
                  <div>
                    <CardTitle>Reusable elements</CardTitle>
                    <CardDescription>
                      Save commonly-used roles, parties, links, and resources to reuse across all
                      OSCAL models.
                    </CardDescription>
                  </div>
                </div>
              </CardHeader>
              <CardContent>
                <ElementLibrary />
              </CardContent>
            </Card>
          </TabsContent>
        </Tabs>
      </div>

      <Footer />
    </div>
  );
}
