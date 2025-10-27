'use client';

import { useState, useEffect } from 'react';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Textarea } from '@/components/ui/textarea';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { Badge } from '@/components/ui/badge';
import { Dialog, DialogContent } from '@/components/ui/dialog';
import { Checkbox } from '@/components/ui/checkbox';
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
import type { ComponentDefinitionRequest, ComponentDefinitionResponse } from '@/types/oscal';

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

interface ComponentBuilderWizardProps {
  editingComponent?: ComponentDefinitionResponse | null;
  onSaveComplete?: () => void;
}

const WIZARD_STEPS: WizardStep[] = [
  { id: 1, title: 'Metadata', description: 'Basic information' },
  { id: 2, title: 'Select Controls', description: 'Choose catalog and controls' },
  { id: 3, title: 'Components/Capabilities', description: 'Define components or capabilities' },
  { id: 4, title: 'Assign Controls', description: 'Map controls to components' },
  { id: 5, title: 'Implementation Details', description: 'Add implementation guidance' },
  { id: 6, title: 'Review & Save', description: 'Preview and save your work' },
];

export function ComponentBuilderWizard({ editingComponent, onSaveComplete }: ComponentBuilderWizardProps) {
  const [currentStep, setCurrentStep] = useState(1);
  const [isSaving, setIsSaving] = useState(false);
  const [saveError, setSaveError] = useState<string | null>(null);
  const [saveSuccess, setSaveSuccess] = useState(false);
  const [showControlSelector, setShowControlSelector] = useState(false);
  const [isLoadingEdit, setIsLoadingEdit] = useState(false);
  const [loadError, setLoadError] = useState<string | null>(null);

  // Step 1: Metadata
  const [metadata, setMetadata] = useState({
    title: '',
    version: '1.0.0',
    oscalVersion: '1.1.3',
    description: '',
  });

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

  // Load component data when editing
  useEffect(() => {
    if (editingComponent) {
      loadComponentForEditing(editingComponent.id);
    } else {
      // Reset to defaults when creating new
      resetWizard();
    }
  }, [editingComponent]);

  const resetWizard = () => {
    setCurrentStep(1);
    setMetadata({ title: '', version: '1.0.0', oscalVersion: '1.1.3', description: '' });
    setSelectedCatalog(null);
    setSelectedControls([]);
    setComponentsAndCapabilities([]);
    setControlAssignments([]);
    setImplementationDetails([]);
    setSaveError(null);
    setSaveSuccess(false);
  };

  const loadComponentForEditing = async (componentId: number) => {
    setIsLoadingEdit(true);
    setLoadError(null);

    try {
      let oscalJson: unknown = await apiClient.getComponentDefinitionContent(componentId);

      // If we got a string, parse it as JSON
      if (typeof oscalJson === 'string') {
        try {
          oscalJson = JSON.parse(oscalJson);
        } catch (parseError) {
          throw new Error('Failed to parse OSCAL JSON from server');
        }
      }

      console.log('Loaded OSCAL JSON:', oscalJson);

      // Validate the structure
      if (!oscalJson || typeof oscalJson !== 'object') {
        throw new Error('No OSCAL data returned from server');
      }

      const oscalObj = oscalJson as Record<string, unknown>;
      const compDefRaw = oscalObj['component-definition'];

      if (!compDefRaw || typeof compDefRaw !== 'object') {
        console.error('OSCAL JSON structure:', Object.keys(oscalObj));
        throw new Error(`Invalid OSCAL structure: missing component-definition. Found keys: ${Object.keys(oscalObj).join(', ')}`);
      }

      const compDef = compDefRaw as Record<string, unknown>;

      if (!compDef.metadata || typeof compDef.metadata !== 'object') {
        throw new Error('Invalid OSCAL structure: missing metadata');
      }

      const metadata = compDef.metadata as Record<string, unknown>;

      // Load metadata
      setMetadata({
        title: (metadata.title as string) || '',
        version: (metadata.version as string) || '1.0.0',
        oscalVersion: (metadata['oscal-version'] as string) || '1.1.3',
        description: (metadata.description as string) || '',
      });

      // Load components and capabilities
      const loadedItems: ComponentOrCapability[] = [];
      const loadedAssignments: ControlAssignment[] = [];
      const loadedDetails: ImplementationDetail[] = [];
      const allControls: Set<string> = new Set();
      let catalogSource: string | null = null;

      // Process components
      if (compDef.components && Array.isArray(compDef.components)) {
        for (const compRaw of compDef.components) {
          const comp = compRaw as Record<string, unknown>;
          loadedItems.push({
            uuid: comp.uuid as string,
            type: 'component',
            componentType: comp.type as string,
            title: comp.title as string,
            description: (comp.description as string) || '',
          });

          // Extract control implementations
          if (comp['control-implementations'] && Array.isArray(comp['control-implementations'])) {
            for (const controlImplRaw of comp['control-implementations']) {
              const controlImpl = controlImplRaw as Record<string, unknown>;
              if (!catalogSource) catalogSource = controlImpl.source as string;

              const controlIds: string[] = [];
              if (controlImpl['implemented-requirements'] && Array.isArray(controlImpl['implemented-requirements'])) {
                for (const reqRaw of controlImpl['implemented-requirements']) {
                  const req = reqRaw as Record<string, unknown>;
                  const controlId = req['control-id'] as string;
                  controlIds.push(controlId);
                  allControls.add(controlId);

                  // Store implementation details
                  if (req.description) {
                    loadedDetails.push({
                      componentUuid: comp.uuid as string,
                      controlId,
                      description: req.description as string,
                    });
                  }
                }
              }

              if (controlIds.length > 0) {
                loadedAssignments.push({
                  componentUuid: comp.uuid as string,
                  controlIds,
                });
              }
            }
          }
        }
      }

      // Process capabilities
      if (compDef.capabilities && Array.isArray(compDef.capabilities)) {
        for (const capRaw of compDef.capabilities) {
          const cap = capRaw as Record<string, unknown>;
          loadedItems.push({
            uuid: cap.uuid as string,
            type: 'capability',
            name: cap.name as string,
            title: (cap.name as string) || '',
            description: (cap.description as string) || '',
          });

          // Extract control implementations for capabilities
          if (cap['control-implementations'] && Array.isArray(cap['control-implementations'])) {
            for (const controlImplRaw of cap['control-implementations']) {
              const controlImpl = controlImplRaw as Record<string, unknown>;
              if (!catalogSource) catalogSource = controlImpl.source as string;

              const controlIds: string[] = [];
              if (controlImpl['implemented-requirements'] && Array.isArray(controlImpl['implemented-requirements'])) {
                for (const reqRaw of controlImpl['implemented-requirements']) {
                  const req = reqRaw as Record<string, unknown>;
                  const controlId = req['control-id'] as string;
                  controlIds.push(controlId);
                  allControls.add(controlId);

                  if (req.description) {
                    loadedDetails.push({
                      componentUuid: cap.uuid as string,
                      controlId,
                      description: req.description as string,
                    });
                  }
                }
              }

              if (controlIds.length > 0) {
                loadedAssignments.push({
                  componentUuid: cap.uuid as string,
                  controlIds,
                });
              }
            }
          }
        }
      }

      setComponentsAndCapabilities(loadedItems);
      setControlAssignments(loadedAssignments);
      setImplementationDetails(loadedDetails);

      // Set catalog and controls (simplified - just control IDs)
      if (catalogSource && allControls.size > 0) {
        setSelectedCatalog({
          title: 'Loaded Catalog',
          source: catalogSource,
        });

        setSelectedControls(
          Array.from(allControls).map(id => ({
            controlId: id,
            title: id,
            description: '',
          }))
        );
      }
    } catch (error) {
      setLoadError(error instanceof Error ? error.message : 'Failed to load component for editing');
      console.error('Error loading component for editing:', error);
    } finally {
      setIsLoadingEdit(false);
    }
  };

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

  // Component/Capability management
  const addComponent = () => {
    const newComponent: ComponentOrCapability = {
      uuid: crypto.randomUUID(),
      type: 'component',
      componentType: 'software',
      title: '',
      description: '',
    };
    setComponentsAndCapabilities([...componentsAndCapabilities, newComponent]);
  };

  const addCapability = () => {
    const newCapability: ComponentOrCapability = {
      uuid: crypto.randomUUID(),
      type: 'capability',
      name: '',
      title: '',
      description: '',
    };
    setComponentsAndCapabilities([...componentsAndCapabilities, newCapability]);
  };

  const updateComponentOrCapability = (uuid: string, field: string, value: string) => {
    setComponentsAndCapabilities(componentsAndCapabilities.map(item =>
      item.uuid === uuid ? { ...item, [field]: value } : item
    ));
  };

  const removeComponentOrCapability = (uuid: string) => {
    setComponentsAndCapabilities(componentsAndCapabilities.filter(item => item.uuid !== uuid));
    // Also remove related control assignments and implementation details
    setControlAssignments(controlAssignments.filter(ca => ca.componentUuid !== uuid));
    setImplementationDetails(implementationDetails.filter(id => id.componentUuid !== uuid));
  };

  // Control assignment management
  const toggleControlAssignment = (componentUuid: string, controlId: string) => {
    const assignment = controlAssignments.find(ca => ca.componentUuid === componentUuid);

    if (assignment) {
      if (assignment.controlIds.includes(controlId)) {
        // Remove control
        const newControlIds = assignment.controlIds.filter(id => id !== controlId);
        if (newControlIds.length === 0) {
          setControlAssignments(controlAssignments.filter(ca => ca.componentUuid !== componentUuid));
        } else {
          setControlAssignments(controlAssignments.map(ca =>
            ca.componentUuid === componentUuid
              ? { ...ca, controlIds: newControlIds }
              : ca
          ));
        }
        // Remove implementation details for this control
        setImplementationDetails(implementationDetails.filter(
          id => !(id.componentUuid === componentUuid && id.controlId === controlId)
        ));
      } else {
        // Add control
        setControlAssignments(controlAssignments.map(ca =>
          ca.componentUuid === componentUuid
            ? { ...ca, controlIds: [...ca.controlIds, controlId] }
            : ca
        ));
      }
    } else {
      // Create new assignment
      setControlAssignments([...controlAssignments, {
        componentUuid,
        controlIds: [controlId],
      }]);
    }
  };

  // Implementation details management
  const updateImplementationDetail = (componentUuid: string, controlId: string, description: string) => {
    const existing = implementationDetails.find(
      id => id.componentUuid === componentUuid && id.controlId === controlId
    );

    if (existing) {
      setImplementationDetails(implementationDetails.map(id =>
        id.componentUuid === componentUuid && id.controlId === controlId
          ? { ...id, description }
          : id
      ));
    } else {
      setImplementationDetails([...implementationDetails, {
        componentUuid,
        controlId,
        description,
      }]);
    }
  };

  const generateOSCALJson = () => {
    const compDefObj: Record<string, unknown> = {
      uuid: crypto.randomUUID(),
      metadata: {
        title: metadata.title,
        'last-modified': new Date().toISOString(),
        version: metadata.version,
        'oscal-version': metadata.oscalVersion,
        ...(metadata.description && { description: metadata.description }),
      },
    };

    // Add components
    const components = componentsAndCapabilities.filter(item => item.type === 'component');
    if (components.length > 0) {
      compDefObj.components = components.map(comp => {
        const assignment = controlAssignments.find(ca => ca.componentUuid === comp.uuid);
        const compObj: Record<string, unknown> = {
          uuid: comp.uuid,
          type: comp.componentType || 'software',
          title: comp.title,
          description: comp.description || 'No description provided',
        };

        // Add control implementations if controls are assigned
        if (assignment && assignment.controlIds.length > 0 && selectedCatalog) {
          compObj['control-implementations'] = [{
            uuid: crypto.randomUUID(),
            source: selectedCatalog.source,
            description: `Implementation of controls from ${selectedCatalog.title}`,
            'implemented-requirements': assignment.controlIds.map(controlId => {
              const implDetail = implementationDetails.find(
                id => id.componentUuid === comp.uuid && id.controlId === controlId
              );
              return {
                uuid: crypto.randomUUID(),
                'control-id': controlId,
                description: implDetail?.description && implDetail.description.trim() !== ''
                  ? implDetail.description
                  : `Implementation details for ${controlId}`,
              };
            }),
          }];
        }

        return compObj;
      });
    }

    // Add capabilities
    const capabilities = componentsAndCapabilities.filter(item => item.type === 'capability');
    if (capabilities.length > 0) {
      compDefObj.capabilities = capabilities.map(cap => {
        const assignment = controlAssignments.find(ca => ca.componentUuid === cap.uuid);
        const capObj: Record<string, unknown> = {
          uuid: cap.uuid,
          name: cap.name || cap.title,
          description: cap.description || 'No description provided',
        };

        // Add control implementations if controls are assigned
        if (assignment && assignment.controlIds.length > 0 && selectedCatalog) {
          capObj['control-implementations'] = [{
            uuid: crypto.randomUUID(),
            source: selectedCatalog.source,
            description: `Implementation of controls from ${selectedCatalog.title}`,
            'implemented-requirements': assignment.controlIds.map(controlId => {
              const implDetail = implementationDetails.find(
                id => id.componentUuid === cap.uuid && id.controlId === controlId
              );
              return {
                uuid: crypto.randomUUID(),
                'control-id': controlId,
                description: implDetail?.description && implDetail.description.trim() !== ''
                  ? implDetail.description
                  : `Implementation details for ${controlId}`,
              };
            }),
          }];
        }

        return capObj;
      });
    }

    return {
      'component-definition': compDefObj
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

      const totalControls = controlAssignments.reduce((sum, ca) => sum + ca.controlIds.length, 0);
      const componentCount = componentsAndCapabilities.filter(item => item.type === 'component').length;
      const capabilityCount = componentsAndCapabilities.filter(item => item.type === 'capability').length;

      const request: ComponentDefinitionRequest = {
        title: metadata.title,
        description: metadata.description || '',
        version: metadata.version,
        oscalVersion: metadata.oscalVersion,
        filename,
        jsonContent,
        componentCount,
        capabilityCount,
        controlCount: totalControls,
      };

      if (editingComponent) {
        // Update existing component
        await apiClient.updateComponentDefinition(editingComponent.id, request);
      } else {
        // Create new component
        await apiClient.createComponentDefinition(request);
      }

      setSaveSuccess(true);

      // Call completion callback after a brief delay
      setTimeout(() => {
        if (onSaveComplete) {
          onSaveComplete();
        }
      }, 1500);
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
        return true; // Optional
      case 3:
        return componentsAndCapabilities.length > 0 &&
               componentsAndCapabilities.every(item => item.title.trim() !== '');
      case 4:
        return true; // Optional
      case 5:
        return true; // Optional
      case 6:
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
    <div className="flex items-center justify-between mb-8 overflow-x-auto">
      {WIZARD_STEPS.map((step, index) => (
        <div key={step.id} className="flex items-center flex-1 min-w-0">
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
              <div className={`text-xs sm:text-sm font-medium ${
                currentStep === step.id ? 'text-foreground' : 'text-muted-foreground'
              }`}>
                {step.title}
              </div>
              <div className="text-xs text-muted-foreground hidden lg:block">
                {step.description}
              </div>
            </div>
          </div>
          {index < WIZARD_STEPS.length - 1 && (
            <ArrowRight className="h-4 w-4 text-muted-foreground mx-1 flex-shrink-0" />
          )}
        </div>
      ))}
    </div>
  );

  // Step render functions will continue...
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
                    Catalog: {selectedCatalog.title}
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
                Change
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
            <p className="text-muted-foreground mb-2">No controls selected</p>
            <p className="text-sm text-muted-foreground mb-4 text-center">
              Select a catalog and controls (optional but recommended)
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
      <div className="flex items-center justify-between">
        <div>
          <h3 className="text-lg font-medium mb-2">Components and Capabilities</h3>
          <p className="text-sm text-muted-foreground">
            Define the components or capabilities - you can add multiple
          </p>
        </div>
        <div className="flex gap-2">
          <Button onClick={addComponent} size="sm">
            <Plus className="mr-1 h-4 w-4" />
            Add Component
          </Button>
          <Button onClick={addCapability} size="sm" variant="outline">
            <Plus className="mr-1 h-4 w-4" />
            Add Capability
          </Button>
        </div>
      </div>

      {componentsAndCapabilities.length > 0 && (
        <div className="flex items-center gap-2 px-4 py-2 bg-blue-50 dark:bg-blue-950 border border-blue-200 dark:border-blue-800 rounded-lg">
          <CheckCircle2 className="h-4 w-4 text-blue-600" />
          <p className="text-sm text-blue-900 dark:text-blue-100">
            <strong>{componentsAndCapabilities.length}</strong> {componentsAndCapabilities.length === 1 ? 'item' : 'items'} added.
            Click the buttons above to add more, or click Next when done.
          </p>
        </div>
      )}

      {componentsAndCapabilities.length === 0 ? (
        <Card>
          <CardContent className="flex flex-col items-center justify-center py-12">
            <CheckCircle2 className="h-12 w-12 text-muted-foreground mb-4" />
            <p className="text-muted-foreground mb-2">No components or capabilities defined</p>
            <p className="text-sm text-muted-foreground mb-4 text-center">
              Click one of the buttons above to add your first component or capability
            </p>
          </CardContent>
        </Card>
      ) : (
        <div className="space-y-4">
          {componentsAndCapabilities.map((item) => (
            <Card key={item.uuid}>
              <CardHeader>
                <div className="flex items-center justify-between">
                  <div>
                    <CardTitle className="text-base">
                      {item.type === 'component' ? 'Component' : 'Capability'}
                    </CardTitle>
                    <CardDescription className="text-xs">
                      {item.type === 'component' ? item.componentType : 'Capability'}
                    </CardDescription>
                  </div>
                  <Button
                    variant="ghost"
                    size="sm"
                    onClick={() => removeComponentOrCapability(item.uuid)}
                  >
                    <Trash2 className="h-4 w-4 text-red-600" />
                  </Button>
                </div>
              </CardHeader>
              <CardContent className="space-y-4">
                {item.type === 'component' && (
                  <div className="space-y-2">
                    <Label>Component Type *</Label>
                    <Select
                      value={item.componentType || 'software'}
                      onValueChange={(value) => updateComponentOrCapability(item.uuid, 'componentType', value)}
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
                )}

                <div className="space-y-2">
                  <Label>{item.type === 'component' ? 'Title' : 'Name'} *</Label>
                  <Input
                    value={item.title}
                    onChange={(e) => updateComponentOrCapability(item.uuid, 'title', e.target.value)}
                    placeholder={item.type === 'component' ? 'Component title...' : 'Capability name...'}
                  />
                </div>

                <div className="space-y-2">
                  <Label>Description</Label>
                  <Textarea
                    value={item.description}
                    onChange={(e) => updateComponentOrCapability(item.uuid, 'description', e.target.value)}
                    placeholder="Describe this..."
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

  const renderStep4 = () => (
    <div className="space-y-6">
      <div>
        <h3 className="text-lg font-medium mb-2">Assign Controls to Components/Capabilities</h3>
        <p className="text-sm text-muted-foreground">
          Select which controls each component or capability implements
        </p>
      </div>

      {selectedControls.length === 0 ? (
        <Card>
          <CardContent className="flex flex-col items-center justify-center py-12">
            <Library className="h-12 w-12 text-muted-foreground mb-4" />
            <p className="text-muted-foreground mb-2">No controls selected</p>
            <p className="text-sm text-muted-foreground mb-4 text-center">
              Go back to Step 2 to select controls
            </p>
            <Button onClick={() => setCurrentStep(2)} variant="outline">
              Select Controls
            </Button>
          </CardContent>
        </Card>
      ) : (
        <div className="space-y-4">
          {componentsAndCapabilities.map((item) => {
            const assignment = controlAssignments.find(ca => ca.componentUuid === item.uuid);

            return (
              <Card key={item.uuid}>
                <CardHeader>
                  <CardTitle className="text-base">{item.title || 'Unnamed'}</CardTitle>
                  <CardDescription>
                    {item.type === 'component' ? `Component (${item.componentType})` : 'Capability'} -{' '}
                    {assignment ? assignment.controlIds.length : 0} controls assigned
                  </CardDescription>
                </CardHeader>
                <CardContent>
                  <div className="space-y-2">
                    {selectedControls.map((control) => (
                      <div key={control.controlId} className="flex items-center gap-3 p-2 border rounded hover:bg-muted/50">
                        <Checkbox
                          checked={assignment?.controlIds.includes(control.controlId) || false}
                          onCheckedChange={() => toggleControlAssignment(item.uuid, control.controlId)}
                        />
                        <div className="flex-1">
                          <div className="font-medium text-sm">{control.controlId}</div>
                          <div className="text-xs text-muted-foreground">{control.title}</div>
                        </div>
                      </div>
                    ))}
                  </div>
                </CardContent>
              </Card>
            );
          })}
        </div>
      )}
    </div>
  );

  const renderStep5 = () => {
    const assignedPairs: Array<{ component: ComponentOrCapability; controlId: string; }> = [];
    controlAssignments.forEach(ca => {
      const component = componentsAndCapabilities.find(c => c.uuid === ca.componentUuid);
      if (component) {
        ca.controlIds.forEach(controlId => {
          assignedPairs.push({ component, controlId });
        });
      }
    });

    return (
      <div className="space-y-6">
        <div>
          <h3 className="text-lg font-medium mb-2">Implementation Details</h3>
          <p className="text-sm text-muted-foreground">
            Add implementation guidance for each control in each component/capability
          </p>
        </div>

        {assignedPairs.length === 0 ? (
          <Card>
            <CardContent className="flex flex-col items-center justify-center py-12">
              <CheckCircle2 className="h-12 w-12 text-muted-foreground mb-4" />
              <p className="text-muted-foreground mb-2">No control assignments</p>
              <p className="text-sm text-muted-foreground mb-4 text-center">
                Go back to Step 4 to assign controls to components
              </p>
            </CardContent>
          </Card>
        ) : (
          <div className="space-y-6">
            {componentsAndCapabilities.map((component) => {
              const componentPairs = assignedPairs.filter(p => p.component.uuid === component.uuid);
              if (componentPairs.length === 0) return null;

              return (
                <Card key={component.uuid}>
                  <CardHeader>
                    <CardTitle className="text-base">{component.title}</CardTitle>
                    <CardDescription>
                      {componentPairs.length} control{componentPairs.length !== 1 ? 's' : ''} to implement
                    </CardDescription>
                  </CardHeader>
                  <CardContent className="space-y-4">
                    {componentPairs.map(({ controlId }) => {
                      const control = selectedControls.find(c => c.controlId === controlId);
                      const existingDetail = implementationDetails.find(
                        id => id.componentUuid === component.uuid && id.controlId === controlId
                      );

                      return (
                        <div key={controlId} className="space-y-2 p-3 border rounded">
                          <div className="font-medium text-sm">{controlId} - {control?.title}</div>
                          <Textarea
                            value={existingDetail?.description || ''}
                            onChange={(e) => updateImplementationDetail(component.uuid, controlId, e.target.value)}
                            placeholder="Describe how this component implements this control..."
                            rows={3}
                          />
                        </div>
                      );
                    })}
                  </CardContent>
                </Card>
              );
            })}
          </div>
        )}
      </div>
    );
  };

  const renderStep6 = () => {
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
              <CardDescription>Components/Capabilities</CardDescription>
            </CardHeader>
            <CardContent>
              <p className="font-medium">{componentsAndCapabilities.length}</p>
            </CardContent>
          </Card>

          <Card>
            <CardHeader className="pb-3">
              <CardDescription>Total Control Assignments</CardDescription>
            </CardHeader>
            <CardContent>
              <p className="font-medium">
                {controlAssignments.reduce((sum, ca) => sum + ca.controlIds.length, 0)}
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

  // Show loading indicator while loading edit data
  if (isLoadingEdit) {
    return (
      <Card>
        <CardContent className="flex items-center justify-center py-12">
          <div className="text-center">
            <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-primary mx-auto mb-4"></div>
            <p className="text-muted-foreground">Loading component for editing...</p>
          </div>
        </CardContent>
      </Card>
    );
  }

  // Show error if loading failed
  if (loadError) {
    return (
      <Card className="border-red-200 bg-red-50 dark:bg-red-950">
        <CardContent className="pt-6">
          <p className="text-sm text-red-800 dark:text-red-200 mb-4">{loadError}</p>
          <Button onClick={() => {
            if (onSaveComplete) onSaveComplete();
          }}>
            Go Back
          </Button>
        </CardContent>
      </Card>
    );
  }

  return (
    <>
      {editingComponent && (
        <div className="mb-4 p-4 bg-blue-50 dark:bg-blue-950 border border-blue-200 dark:border-blue-800 rounded-lg">
          <p className="text-sm text-blue-900 dark:text-blue-100 font-medium">
            Editing: {editingComponent.title}
          </p>
        </div>
      )}

      <div className="space-y-8">
        {renderStepIndicator()}

        <Card>
          <CardContent className="pt-6">
            {currentStep === 1 && renderStep1()}
            {currentStep === 2 && renderStep2()}
            {currentStep === 3 && renderStep3()}
            {currentStep === 4 && renderStep4()}
            {currentStep === 5 && renderStep5()}
            {currentStep === 6 && renderStep6()}
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
                  <>{editingComponent ? 'Updating...' : 'Saving...'}</>
                ) : saveSuccess ? (
                  <>
                    <CheckCircle2 className="mr-2 h-4 w-4" />
                    {editingComponent ? 'Updated' : 'Saved'}
                  </>
                ) : (
                  <>
                    <Save className="mr-2 h-4 w-4" />
                    {editingComponent ? 'Update Component Definition' : 'Save Component Definition'}
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
