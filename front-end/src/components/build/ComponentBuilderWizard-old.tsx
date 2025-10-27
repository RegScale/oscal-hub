'use client';

import { useState } from 'react';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Textarea } from '@/components/ui/textarea';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { Badge } from '@/components/ui/badge';
import { Dialog, DialogContent } from '@/components/ui/dialog';
import {
  ChevronRight,
  ChevronLeft,
  Save,
  Plus,
  Eye,
  CheckCircle2,
  Circle,
  ArrowRight,
  Library,
  Trash2
} from 'lucide-react';
import { apiClient } from '@/lib/api-client';
import { ControlSelector } from '@/components/build/ControlSelector';
import type { ComponentDefinitionRequest } from '@/types/oscal';

interface WizardStep {
  id: number;
  title: string;
  description: string;
}

interface ComponentOrCapability {
  uuid: string;
  type: 'component' | 'capability';
  // For components
  componentType?: string;
  title: string;
  description: string;
  // For capabilities
  name?: string;
}

interface ControlAssignment {
  componentUuid: string;
  controlIds: string[];
}

interface ImplementationDetail {
  componentUuid: string;
  controlId: string;
  description: string;
}

const WIZARD_STEPS: WizardStep[] = [
  { id: 1, title: 'Metadata', description: 'Basic information' },
  { id: 2, title: 'Select Controls', description: 'Choose catalog and controls' },
  { id: 3, title: 'Components/Capabilities', description: 'Define components or capabilities' },
  { id: 4, title: 'Assign Controls', description: 'Map controls to components' },
  { id: 5, title: 'Implementation Details', description: 'Add implementation guidance' },
  { id: 6, title: 'Review & Save', description: 'Preview and save your work' },
];

export function ComponentBuilderWizard() {
  const [currentStep, setCurrentStep] = useState(1);
  const [isSaving, setIsSaving] = useState(false);
  const [saveError, setSaveError] = useState<string | null>(null);
  const [saveSuccess, setSaveSuccess] = useState(false);
  const [showControlSelector, setShowControlSelector] = useState(false);

  // Step 1: Metadata
  const [metadata, setMetadata] = useState({
    title: '',
    version: '1.0.0',
    oscalVersion: '1.1.3',
    description: '',
  });

  // Component type
  const [componentType, setComponentType] = useState<string>('software');

  // Step 2: Catalog and Control Selection
  const [selectedCatalog, setSelectedCatalog] = useState<{
    title: string;
    source: string;
  } | null>(null);
  const [selectedControls, setSelectedControls] = useState<Array<{
    controlId: string;
    title: string;
    description: string;
  }>>([]);

  // Step 3: Components/Capabilities
  const [componentsAndCapabilities, setComponentsAndCapabilities] = useState<ComponentOrCapability[]>([]);

  // Step 4: Control Assignments
  const [controlAssignments, setControlAssignments] = useState<ControlAssignment[]>([]);

  // Step 5: Implementation Details
  const [implementationDetails, setImplementationDetails] = useState<ImplementationDetail[]>([]);

  const handleCatalogAndControlSelection = (
    catalog: { title: string; source: string },
    controls: Array<{ controlId: string; title: string; description: string; }>
  ) => {
    setSelectedCatalog(catalog);
    setSelectedControls(controls);
    setShowControlSelector(false);
    // Auto-advance to next step after selecting controls
    if (currentStep === 2) {
      setCurrentStep(3);
    }
  };

  const generateOSCALJson = () => {
    // Create a single component from the metadata
    const componentUuid = crypto.randomUUID();

    const oscalDoc: Record<string, unknown> = {
      'component-definition': {
        uuid: crypto.randomUUID(),
        metadata: {
          title: metadata.title,
          'last-modified': new Date().toISOString(),
          version: metadata.version,
          'oscal-version': metadata.oscalVersion,
          ...(metadata.description && { description: metadata.description }),
        },
        components: [
          {
            uuid: componentUuid,
            type: componentType,
            title: metadata.title,
            description: metadata.description || '',
            // Include control-implementations within the component if controls are selected
            ...(selectedControls.length > 0 && selectedCatalog && {
              'control-implementations': [
                {
                  uuid: crypto.randomUUID(),
                  source: selectedCatalog.source,
                  description: `Implementation of controls from ${selectedCatalog.title}`,
                  'implemented-requirements': selectedControls.map(control => ({
                    uuid: crypto.randomUUID(),
                    'control-id': control.controlId,
                    description: control.description,
                  })),
                },
              ],
            }),
          }
        ],
      }
    };

    return oscalDoc;
  };

  const handleSave = async () => {
    setSaveError(null);
    setSaveSuccess(false);
    setIsSaving(true);

    try {
      const oscalJson = generateOSCALJson();
      const jsonContent = JSON.stringify(oscalJson, null, 2);
      const filename = `${metadata.title.toLowerCase().replace(/\s+/g, '-')}.json`;

      const request: ComponentDefinitionRequest = {
        title: metadata.title,
        description: metadata.description || '',
        version: metadata.version,
        oscalVersion: metadata.oscalVersion,
        filename,
        jsonContent,
        componentCount: 1, // Always 1 component
        controlCount: selectedControls.length,
      };

      await apiClient.createComponentDefinition(request);
      setSaveSuccess(true);
    } catch (error) {
      setSaveError(error instanceof Error ? error.message : 'Failed to save component definition');
      console.error('Error saving component definition:', error);
    } finally {
      setIsSaving(false);
    }
  };

  const canProceedToNext = () => {
    switch (currentStep) {
      case 1:
        // Step 1: Metadata is required (title and version)
        return metadata.title.trim() !== '' && metadata.version.trim() !== '';
      case 2:
        // Step 2: Catalog and controls selection is optional but recommended
        return true;
      case 3:
        // Step 3: Component type is required
        return componentType.trim() !== '';
      case 4:
        // Step 4: Control implementations are optional
        return true;
      case 5:
        // Step 5: Review step, always can proceed
        return true;
      default:
        return false;
    }
  };

  const nextStep = () => {
    if (currentStep < WIZARD_STEPS.length) {
      setCurrentStep(currentStep + 1);
    }
  };

  const previousStep = () => {
    if (currentStep > 1) {
      setCurrentStep(currentStep - 1);
    }
  };

  const renderStepIndicator = () => (
    <div className="flex items-center justify-between mb-8">
      {WIZARD_STEPS.map((step, index) => (
        <div key={step.id} className="flex items-center flex-1">
          <div className="flex flex-col items-center flex-1">
            <div className={`flex items-center justify-center w-10 h-10 rounded-full border-2 transition-colors ${
              currentStep > step.id
                ? 'bg-primary border-primary text-primary-foreground'
                : currentStep === step.id
                ? 'border-primary text-primary'
                : 'border-muted-foreground/30 text-muted-foreground'
            }`}>
              {currentStep > step.id ? (
                <CheckCircle2 className="h-5 w-5" />
              ) : (
                <Circle className={`h-5 w-5 ${currentStep === step.id ? 'fill-current' : ''}`} />
              )}
            </div>
            <div className="mt-2 text-center">
              <div className={`text-sm font-medium ${
                currentStep === step.id ? 'text-foreground' : 'text-muted-foreground'
              }`}>
                {step.title}
              </div>
              <div className="text-xs text-muted-foreground hidden sm:block">
                {step.description}
              </div>
            </div>
          </div>
          {index < WIZARD_STEPS.length - 1 && (
            <ArrowRight className="h-5 w-5 text-muted-foreground mx-2 flex-shrink-0" />
          )}
        </div>
      ))}
    </div>
  );

  const renderStep1 = () => (
    <div className="space-y-6">
      <div>
        <h3 className="text-lg font-medium mb-2">Component Definition Metadata</h3>
        <p className="text-sm text-muted-foreground">
          Provide basic information about this component definition
        </p>
      </div>

      <div className="space-y-2">
        <Label htmlFor="title">Title *</Label>
        <Input
          id="title"
          value={metadata.title}
          onChange={(e) => setMetadata({ ...metadata, title: e.target.value })}
          placeholder="e.g., Django Web Framework Component Definition"
          required
        />
        <p className="text-xs text-muted-foreground">
          A descriptive title for this component definition
        </p>
      </div>

      <div className="space-y-2">
        <Label htmlFor="description">Description</Label>
        <Textarea
          id="description"
          value={metadata.description}
          onChange={(e) => setMetadata({ ...metadata, description: e.target.value })}
          placeholder="Describe the purpose and scope of this component definition..."
          rows={4}
        />
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        <div className="space-y-2">
          <Label htmlFor="version">Version *</Label>
          <Input
            id="version"
            value={metadata.version}
            onChange={(e) => setMetadata({ ...metadata, version: e.target.value })}
            placeholder="1.0.0"
            required
          />
        </div>

        <div className="space-y-2">
          <Label htmlFor="oscalVersion">OSCAL Version *</Label>
          <Select
            value={metadata.oscalVersion}
            onValueChange={(value) => setMetadata({ ...metadata, oscalVersion: value })}
          >
            <SelectTrigger id="oscalVersion">
              <SelectValue />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="1.1.3">1.1.3 (Latest)</SelectItem>
              <SelectItem value="1.1.2">1.1.2</SelectItem>
              <SelectItem value="1.1.1">1.1.1</SelectItem>
              <SelectItem value="1.1.0">1.1.0</SelectItem>
            </SelectContent>
          </Select>
        </div>
      </div>
    </div>
  );

  const renderStep2 = () => (
    <div className="space-y-6">
      <div>
        <h3 className="text-lg font-medium mb-2">Select Catalog and Controls</h3>
        <p className="text-sm text-muted-foreground">
          Choose a catalog or profile and select the controls you want to implement
        </p>
      </div>

      {selectedCatalog && selectedControls.length > 0 ? (
        <Card className="border-green-200 bg-green-50 dark:bg-green-950">
          <CardContent className="pt-6">
            <div className="flex items-start justify-between mb-4">
              <div className="flex items-start gap-3">
                <CheckCircle2 className="h-5 w-5 text-green-600 mt-0.5" />
                <div>
                  <p className="font-medium text-green-900 dark:text-green-100">
                    Catalog Selected: {selectedCatalog.title}
                  </p>
                  <p className="text-sm text-green-700 dark:text-green-300">
                    {selectedControls.length} control{selectedControls.length !== 1 ? 's' : ''} selected
                  </p>
                </div>
              </div>
              <Button
                variant="outline"
                size="sm"
                onClick={() => setShowControlSelector(true)}
              >
                Change Selection
              </Button>
            </div>
            <div className="mt-4">
              <Label className="text-sm font-medium">Selected Controls:</Label>
              <div className="mt-2 flex flex-wrap gap-2">
                {selectedControls.map((control) => (
                  <Badge key={control.controlId} variant="secondary">
                    {control.controlId}
                  </Badge>
                ))}
              </div>
            </div>
          </CardContent>
        </Card>
      ) : (
        <Card>
          <CardContent className="flex flex-col items-center justify-center py-12">
            <Library className="h-12 w-12 text-muted-foreground mb-4" />
            <p className="text-muted-foreground mb-2">No catalog or controls selected yet</p>
            <p className="text-sm text-muted-foreground mb-4 text-center">
              Select a catalog and choose which controls you want to implement (optional but recommended)
            </p>
            <Button onClick={() => setShowControlSelector(true)} variant="outline">
              <Library className="mr-2 h-4 w-4" />
              Browse Catalogs
            </Button>
          </CardContent>
        </Card>
      )}
    </div>
  );

  const renderStep3 = () => (
    <div className="space-y-6">
      <div>
        <h3 className="text-lg font-medium mb-2">Component Details</h3>
        <p className="text-sm text-muted-foreground">
          Define the component type and details
        </p>
      </div>

      <div className="space-y-4">
        <div className="space-y-2">
          <Label htmlFor="componentType">Component Type *</Label>
          <Select
            value={componentType}
            onValueChange={setComponentType}
          >
            <SelectTrigger id="componentType">
              <SelectValue />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="software">Software</SelectItem>
              <SelectItem value="hardware">Hardware</SelectItem>
              <SelectItem value="service">Service</SelectItem>
              <SelectItem value="policy">Policy</SelectItem>
              <SelectItem value="process">Process</SelectItem>
              <SelectItem value="procedure">Procedure</SelectItem>
              <SelectItem value="plan">Plan</SelectItem>
              <SelectItem value="guidance">Guidance</SelectItem>
              <SelectItem value="standard">Standard</SelectItem>
              <SelectItem value="validation">Validation</SelectItem>
            </SelectContent>
          </Select>
          <p className="text-xs text-muted-foreground">
            Select the type of component you are defining
          </p>
        </div>

        <Card>
          <CardHeader className="pb-3">
            <CardTitle className="text-base">Component Summary</CardTitle>
          </CardHeader>
          <CardContent className="space-y-2">
            <div className="grid grid-cols-2 gap-2 text-sm">
              <div className="text-muted-foreground">Title:</div>
              <div className="font-medium">{metadata.title || 'Not set'}</div>

              <div className="text-muted-foreground">Type:</div>
              <div className="font-medium capitalize">{componentType}</div>

              <div className="text-muted-foreground">Description:</div>
              <div className="font-medium">{metadata.description || 'None'}</div>
            </div>
          </CardContent>
        </Card>
      </div>
    </div>
  );

  const renderStep4 = () => (
    <div className="space-y-6">
      <div>
        <h3 className="text-lg font-medium mb-2">Control Implementations</h3>
        <p className="text-sm text-muted-foreground">
          Configure how your component implements the selected controls (optional)
        </p>
      </div>

      {selectedControls.length === 0 ? (
        <Card>
          <CardContent className="flex flex-col items-center justify-center py-12">
            <CheckCircle2 className="h-12 w-12 text-muted-foreground mb-4" />
            <p className="text-muted-foreground mb-2">No controls selected</p>
            <p className="text-sm text-muted-foreground mb-4 text-center">
              You haven&apos;t selected any controls in Step 2. Control implementations are optional.
            </p>
            <Button onClick={() => setCurrentStep(2)} variant="outline">
              Go Back to Select Controls
            </Button>
          </CardContent>
        </Card>
      ) : (
        <Card>
          <CardHeader>
            <CardTitle className="text-base">Selected Controls from {selectedCatalog?.title}</CardTitle>
            <CardDescription>
              The following controls will be included in your component definition
            </CardDescription>
          </CardHeader>
          <CardContent>
            <div className="space-y-2">
              {selectedControls.map((control) => (
                <div key={control.controlId} className="flex items-start gap-3 p-3 border rounded-lg">
                  <CheckCircle2 className="h-5 w-5 text-green-600 mt-0.5 flex-shrink-0" />
                  <div className="flex-1">
                    <div className="font-medium">{control.controlId}</div>
                    <div className="text-sm text-muted-foreground">{control.title}</div>
                  </div>
                </div>
              ))}
            </div>
            <p className="text-xs text-muted-foreground mt-4">
              Note: You can configure implementation details for each control in future updates. For now, they will be included with their basic information.
            </p>
          </CardContent>
        </Card>
      )}
    </div>
  );

  const renderStep5 = () => {
    const oscalJson = generateOSCALJson();
    const jsonString = JSON.stringify(oscalJson, null, 2);

    return (
      <div className="space-y-6">
        <div>
          <h3 className="text-lg font-medium mb-2">Review Your Component Definition</h3>
          <p className="text-sm text-muted-foreground">
            Review the details below and preview the generated OSCAL JSON
          </p>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
          <Card>
            <CardHeader className="pb-3">
              <CardDescription>Component Title</CardDescription>
            </CardHeader>
            <CardContent>
              <p className="font-medium">{metadata.title}</p>
            </CardContent>
          </Card>

          <Card>
            <CardHeader className="pb-3">
              <CardDescription>Component Type</CardDescription>
            </CardHeader>
            <CardContent>
              <p className="font-medium capitalize">{componentType}</p>
            </CardContent>
          </Card>

          <Card>
            <CardHeader className="pb-3">
              <CardDescription>Controls</CardDescription>
            </CardHeader>
            <CardContent>
              <p className="font-medium">
                {selectedControls.length}
              </p>
            </CardContent>
          </Card>
        </div>

        <Card>
          <CardHeader>
            <CardTitle className="text-base flex items-center gap-2">
              <Eye className="h-4 w-4" />
              OSCAL JSON Preview
            </CardTitle>
            <CardDescription>
              This is the OSCAL-compliant JSON that will be saved
            </CardDescription>
          </CardHeader>
          <CardContent>
            <pre className="bg-muted p-4 rounded-lg overflow-x-auto text-xs max-h-96 overflow-y-auto">
              <code>{jsonString}</code>
            </pre>
          </CardContent>
        </Card>

        {saveError && (
          <Card className="border-red-200 bg-red-50 dark:bg-red-950">
            <CardContent className="pt-6">
              <p className="text-sm text-red-800 dark:text-red-200">{saveError}</p>
            </CardContent>
          </Card>
        )}

        {saveSuccess && (
          <Card className="border-green-200 bg-green-50 dark:bg-green-950">
            <CardContent className="pt-6 flex items-center gap-3">
              <CheckCircle2 className="h-5 w-5 text-green-600" />
              <div>
                <p className="font-medium text-green-900 dark:text-green-100">
                  Component definition saved successfully!
                </p>
                <p className="text-sm text-green-700 dark:text-green-300">
                  Your component definition has been saved to Azure Blob Storage
                </p>
              </div>
            </CardContent>
          </Card>
        )}
      </div>
    );
  };

  return (
    <>
      <div className="space-y-8">
        {renderStepIndicator()}

        <Card>
          <CardContent className="pt-6">
            {currentStep === 1 && renderStep1()}
            {currentStep === 2 && renderStep2()}
            {currentStep === 3 && renderStep3()}
            {currentStep === 4 && renderStep4()}
            {currentStep === 5 && renderStep5()}
          </CardContent>
        </Card>

      <div className="flex items-center justify-between">
        <Button
          variant="outline"
          onClick={previousStep}
          disabled={currentStep === 1}
        >
          <ChevronLeft className="mr-2 h-4 w-4" />
          Previous
        </Button>

        <div className="flex gap-2">
          {currentStep < WIZARD_STEPS.length ? (
            <Button
              onClick={nextStep}
              disabled={!canProceedToNext()}
            >
              Next
              <ChevronRight className="ml-2 h-4 w-4" />
            </Button>
          ) : (
            <Button
              onClick={handleSave}
              disabled={isSaving || saveSuccess}
            >
              {isSaving ? (
                <>Saving...</>
              ) : saveSuccess ? (
                <>
                  <CheckCircle2 className="mr-2 h-4 w-4" />
                  Saved
                </>
              ) : (
                <>
                  <Save className="mr-2 h-4 w-4" />
                  Save Component Definition
                </>
              )}
            </Button>
          )}
        </div>
      </div>
    </div>

      {/* Control Selector Dialog */}
      <Dialog open={showControlSelector} onOpenChange={setShowControlSelector}>
        <DialogContent className="max-w-4xl">
          <ControlSelector
            onAddControls={handleCatalogAndControlSelection}
            onClose={() => setShowControlSelector(false)}
          />
        </DialogContent>
      </Dialog>
    </>
  );
}
