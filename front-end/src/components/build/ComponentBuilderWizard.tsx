'use client';

import { useState } from 'react';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Textarea } from '@/components/ui/textarea';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { Badge } from '@/components/ui/badge';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';
import {
  ChevronRight,
  ChevronLeft,
  Save,
  Plus,
  Trash2,
  Eye,
  FileJson,
  CheckCircle2,
  Circle,
  ArrowRight
} from 'lucide-react';
import { apiClient } from '@/lib/api-client';
import type { ComponentDefinitionRequest } from '@/types/oscal';

interface WizardStep {
  id: number;
  title: string;
  description: string;
}

const WIZARD_STEPS: WizardStep[] = [
  { id: 1, title: 'Metadata', description: 'Component definition information' },
  { id: 2, title: 'Components', description: 'Add system components' },
  { id: 3, title: 'Control Implementations', description: 'Map to security controls' },
  { id: 4, title: 'Review & Save', description: 'Preview and save your work' },
];

interface ComponentItem {
  uuid: string;
  type: string;
  title: string;
  description: string;
  props?: Array<{ name: string; value: string; }>;
}

interface ControlImplementation {
  uuid: string;
  source: string;
  description: string;
  implementedRequirements: Array<{
    uuid: string;
    controlId: string;
    description: string;
  }>;
}

export function ComponentBuilderWizard() {
  const [currentStep, setCurrentStep] = useState(1);
  const [isSaving, setIsSaving] = useState(false);
  const [saveError, setSaveError] = useState<string | null>(null);
  const [saveSuccess, setSaveSuccess] = useState(false);

  // Step 1: Metadata
  const [metadata, setMetadata] = useState({
    title: '',
    version: '1.0.0',
    oscalVersion: '1.1.3',
    description: '',
  });

  // Step 2: Components
  const [components, setComponents] = useState<ComponentItem[]>([]);

  // Step 3: Control Implementations
  const [controlImplementations, setControlImplementations] = useState<ControlImplementation[]>([]);

  const addComponent = () => {
    const newComponent: ComponentItem = {
      uuid: crypto.randomUUID(),
      type: 'software',
      title: '',
      description: '',
      props: [],
    };
    setComponents([...components, newComponent]);
  };

  const updateComponent = (uuid: string, field: string, value: string) => {
    setComponents(components.map(c =>
      c.uuid === uuid ? { ...c, [field]: value } : c
    ));
  };

  const removeComponent = (uuid: string) => {
    setComponents(components.filter(c => c.uuid !== uuid));
  };

  const addControlImplementation = () => {
    const newImplementation: ControlImplementation = {
      uuid: crypto.randomUUID(),
      source: '',
      description: '',
      implementedRequirements: [],
    };
    setControlImplementations([...controlImplementations, newImplementation]);
  };

  const updateControlImplementation = (uuid: string, field: string, value: string) => {
    setControlImplementations(controlImplementations.map(ci =>
      ci.uuid === uuid ? { ...ci, [field]: value } : ci
    ));
  };

  const removeControlImplementation = (uuid: string) => {
    setControlImplementations(controlImplementations.filter(ci => ci.uuid !== uuid));
  };

  const addImplementedRequirement = (controlImplUuid: string) => {
    setControlImplementations(controlImplementations.map(ci => {
      if (ci.uuid === controlImplUuid) {
        return {
          ...ci,
          implementedRequirements: [
            ...ci.implementedRequirements,
            {
              uuid: crypto.randomUUID(),
              controlId: '',
              description: '',
            }
          ]
        };
      }
      return ci;
    }));
  };

  const updateImplementedRequirement = (
    controlImplUuid: string,
    reqUuid: string,
    field: string,
    value: string
  ) => {
    setControlImplementations(controlImplementations.map(ci => {
      if (ci.uuid === controlImplUuid) {
        return {
          ...ci,
          implementedRequirements: ci.implementedRequirements.map(req =>
            req.uuid === reqUuid ? { ...req, [field]: value } : req
          )
        };
      }
      return ci;
    }));
  };

  const removeImplementedRequirement = (controlImplUuid: string, reqUuid: string) => {
    setControlImplementations(controlImplementations.map(ci => {
      if (ci.uuid === controlImplUuid) {
        return {
          ...ci,
          implementedRequirements: ci.implementedRequirements.filter(req => req.uuid !== reqUuid)
        };
      }
      return ci;
    }));
  };

  const generateOSCALJson = () => {
    return {
      'component-definition': {
        uuid: crypto.randomUUID(),
        metadata: {
          title: metadata.title,
          'last-modified': new Date().toISOString(),
          version: metadata.version,
          'oscal-version': metadata.oscalVersion,
          ...(metadata.description && { description: metadata.description }),
        },
        components: components.map(c => ({
          uuid: c.uuid,
          type: c.type,
          title: c.title,
          description: c.description,
          ...(c.props && c.props.length > 0 && { props: c.props }),
        })),
        ...(controlImplementations.length > 0 && {
          'control-implementations': controlImplementations.map(ci => ({
            uuid: ci.uuid,
            source: ci.source,
            description: ci.description,
            'implemented-requirements': ci.implementedRequirements.map(req => ({
              uuid: req.uuid,
              'control-id': req.controlId,
              description: req.description,
            }))
          }))
        })
      }
    };
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
        componentCount: components.length,
        controlCount: controlImplementations.reduce(
          (sum, ci) => sum + ci.implementedRequirements.length,
          0
        ),
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
        return metadata.title.trim() !== '' && metadata.version.trim() !== '';
      case 2:
        return components.length > 0 && components.every(c => c.title.trim() !== '');
      case 3:
        return true; // Control implementations are optional
      case 4:
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
      <div className="space-y-2">
        <Label htmlFor="title">Component Definition Title *</Label>
        <Input
          id="title"
          value={metadata.title}
          onChange={(e) => setMetadata({ ...metadata, title: e.target.value })}
          placeholder="e.g., Django Web Framework Security Controls"
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
          placeholder="Describe the purpose of this component definition..."
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
      <div className="flex items-center justify-between">
        <div>
          <h3 className="text-lg font-medium">System Components</h3>
          <p className="text-sm text-muted-foreground">
            Add the components that make up your system
          </p>
        </div>
        <Button onClick={addComponent}>
          <Plus className="mr-2 h-4 w-4" />
          Add Component
        </Button>
      </div>

      {components.length === 0 ? (
        <Card>
          <CardContent className="flex flex-col items-center justify-center py-12">
            <FileJson className="h-12 w-12 text-muted-foreground mb-4" />
            <p className="text-muted-foreground mb-4">No components added yet</p>
            <Button onClick={addComponent}>
              <Plus className="mr-2 h-4 w-4" />
              Add First Component
            </Button>
          </CardContent>
        </Card>
      ) : (
        <div className="space-y-4">
          {components.map((component, index) => (
            <Card key={component.uuid}>
              <CardHeader>
                <div className="flex items-center justify-between">
                  <CardTitle className="text-base">Component {index + 1}</CardTitle>
                  <Button
                    variant="ghost"
                    size="sm"
                    onClick={() => removeComponent(component.uuid)}
                  >
                    <Trash2 className="h-4 w-4 text-red-600" />
                  </Button>
                </div>
              </CardHeader>
              <CardContent className="space-y-4">
                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                  <div className="space-y-2">
                    <Label>Component Type</Label>
                    <Select
                      value={component.type}
                      onValueChange={(value) => updateComponent(component.uuid, 'type', value)}
                    >
                      <SelectTrigger>
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
                  </div>

                  <div className="space-y-2">
                    <Label>Component Title *</Label>
                    <Input
                      value={component.title}
                      onChange={(e) => updateComponent(component.uuid, 'title', e.target.value)}
                      placeholder="e.g., Django Web Framework"
                      required
                    />
                  </div>
                </div>

                <div className="space-y-2">
                  <Label>Description</Label>
                  <Textarea
                    value={component.description}
                    onChange={(e) => updateComponent(component.uuid, 'description', e.target.value)}
                    placeholder="Describe this component..."
                    rows={3}
                  />
                </div>
              </CardContent>
            </Card>
          ))}
        </div>
      )}
    </div>
  );

  const renderStep3 = () => (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h3 className="text-lg font-medium">Control Implementations</h3>
          <p className="text-sm text-muted-foreground">
            Map your components to security controls (optional)
          </p>
        </div>
        <Button onClick={addControlImplementation}>
          <Plus className="mr-2 h-4 w-4" />
          Add Control Implementation
        </Button>
      </div>

      {controlImplementations.length === 0 ? (
        <Card>
          <CardContent className="flex flex-col items-center justify-center py-12">
            <CheckCircle2 className="h-12 w-12 text-muted-foreground mb-4" />
            <p className="text-muted-foreground mb-2">No control implementations yet</p>
            <p className="text-sm text-muted-foreground mb-4">
              Control implementations are optional but recommended
            </p>
            <Button onClick={addControlImplementation} variant="outline">
              <Plus className="mr-2 h-4 w-4" />
              Add Control Implementation
            </Button>
          </CardContent>
        </Card>
      ) : (
        <div className="space-y-4">
          {controlImplementations.map((impl, index) => (
            <Card key={impl.uuid}>
              <CardHeader>
                <div className="flex items-center justify-between">
                  <CardTitle className="text-base">Control Implementation {index + 1}</CardTitle>
                  <Button
                    variant="ghost"
                    size="sm"
                    onClick={() => removeControlImplementation(impl.uuid)}
                  >
                    <Trash2 className="h-4 w-4 text-red-600" />
                  </Button>
                </div>
              </CardHeader>
              <CardContent className="space-y-4">
                <div className="grid grid-cols-1 gap-4">
                  <div className="space-y-2">
                    <Label>Source (e.g., NIST SP 800-53 Rev 5)</Label>
                    <Input
                      value={impl.source}
                      onChange={(e) => updateControlImplementation(impl.uuid, 'source', e.target.value)}
                      placeholder="https://doi.org/10.6028/NIST.SP.800-53r5"
                    />
                  </div>

                  <div className="space-y-2">
                    <Label>Description</Label>
                    <Textarea
                      value={impl.description}
                      onChange={(e) => updateControlImplementation(impl.uuid, 'description', e.target.value)}
                      placeholder="Describe this control implementation..."
                      rows={2}
                    />
                  </div>
                </div>

                <div className="border-t pt-4">
                  <div className="flex items-center justify-between mb-3">
                    <Label className="text-sm font-medium">Implemented Requirements</Label>
                    <Button
                      size="sm"
                      variant="outline"
                      onClick={() => addImplementedRequirement(impl.uuid)}
                    >
                      <Plus className="mr-1 h-3 w-3" />
                      Add Requirement
                    </Button>
                  </div>

                  <div className="space-y-3">
                    {impl.implementedRequirements.map((req) => (
                      <div key={req.uuid} className="flex gap-2 items-start p-3 border rounded-lg">
                        <div className="flex-1 grid grid-cols-1 md:grid-cols-2 gap-2">
                          <Input
                            value={req.controlId}
                            onChange={(e) => updateImplementedRequirement(
                              impl.uuid,
                              req.uuid,
                              'controlId',
                              e.target.value
                            )}
                            placeholder="Control ID (e.g., AC-1)"
                            className="text-sm"
                          />
                          <Input
                            value={req.description}
                            onChange={(e) => updateImplementedRequirement(
                              impl.uuid,
                              req.uuid,
                              'description',
                              e.target.value
                            )}
                            placeholder="Description"
                            className="text-sm"
                          />
                        </div>
                        <Button
                          variant="ghost"
                          size="sm"
                          onClick={() => removeImplementedRequirement(impl.uuid, req.uuid)}
                        >
                          <Trash2 className="h-3 w-3 text-red-600" />
                        </Button>
                      </div>
                    ))}
                  </div>
                </div>
              </CardContent>
            </Card>
          ))}
        </div>
      )}
    </div>
  );

  const renderStep4 = () => {
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
              <CardDescription>Title</CardDescription>
            </CardHeader>
            <CardContent>
              <p className="font-medium">{metadata.title}</p>
            </CardContent>
          </Card>

          <Card>
            <CardHeader className="pb-3">
              <CardDescription>Components</CardDescription>
            </CardHeader>
            <CardContent>
              <p className="font-medium">{components.length}</p>
            </CardContent>
          </Card>

          <Card>
            <CardHeader className="pb-3">
              <CardDescription>Controls</CardDescription>
            </CardHeader>
            <CardContent>
              <p className="font-medium">
                {controlImplementations.reduce((sum, ci) => sum + ci.implementedRequirements.length, 0)}
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
    <div className="space-y-8">
      {renderStepIndicator()}

      <Card>
        <CardContent className="pt-6">
          {currentStep === 1 && renderStep1()}
          {currentStep === 2 && renderStep2()}
          {currentStep === 3 && renderStep3()}
          {currentStep === 4 && renderStep4()}
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
  );
}
