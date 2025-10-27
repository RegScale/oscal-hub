'use client';

import { useState, useEffect, useRef } from 'react';
import Editor, { OnMount } from '@monaco-editor/react';
import { Card } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Select } from '@/components/ui/select';
import { Badge } from '@/components/ui/badge';
import { CheckCircle, ChevronRight, RefreshCw } from 'lucide-react';
import { MarkdownPreview } from './markdown-preview';
import { SspVisualization } from './SspVisualization';
import { SarVisualization } from './SarVisualization';
import { ConditionsManager, type Condition } from './conditions-manager';
import { DigitalSignatureStep } from './digital-signature-step';
import type { AuthorizationTemplateResponse, LibraryItem, SspVisualizationData, SarVisualizationData } from '@/types/oscal';
import type { editor } from 'monaco-editor';
import { apiClient } from '@/lib/api-client';
import { DatePicker } from '@/components/ui/date-picker';
import { toast } from 'sonner';

interface AuthorizationWizardProps {
  templates: AuthorizationTemplateResponse[];
  sspItems: LibraryItem[];
  sarItems: LibraryItem[];
  onSave: (data: {
    name: string;
    sspItemId: string;
    sarItemId?: string;
    templateId: number;
    variableValues: Record<string, string>;
    dateAuthorized: string;
    dateExpired: string;
    systemOwner: string;
    securityManager: string;
    authorizingOfficial: string;
    editedContent: string; // The user's edited template content
    conditions?: Condition[]; // Conditions of approval
  }) => void;
  onCancel: () => void;
  isSaving?: boolean;
}

type Step = 'select-ssp' | 'select-sar' | 'stakeholder-info' | 'visualize' | 'select-template' | 'fill-variables' | 'conditions' | 'review' | 'sign';

export function AuthorizationWizard({
  templates,
  sspItems,
  sarItems,
  onSave,
  onCancel,
  isSaving = false,
}: AuthorizationWizardProps) {
  const [step, setStep] = useState<Step>('select-ssp');
  const [authorizationName, setAuthorizationName] = useState('');
  const [selectedSsp, setSelectedSsp] = useState<LibraryItem | null>(null);
  const [selectedSar, setSelectedSar] = useState<LibraryItem | null>(null);
  const [sspVisualization, setSspVisualization] = useState<SspVisualizationData | null>(null);
  const [sarVisualization, setSarVisualization] = useState<SarVisualizationData | null>(null);
  const [loadingVisualizations, setLoadingVisualizations] = useState(false);
  const [selectedTemplate, setSelectedTemplate] = useState<AuthorizationTemplateResponse | null>(null);
  const [editedContent, setEditedContent] = useState(''); // The user's edited template content
  const [detectedVariables, setDetectedVariables] = useState<string[]>([]); // Variables detected from edited content
  const [variableValues, setVariableValues] = useState<Record<string, string>>({});
  const [completedContent, setCompletedContent] = useState('');

  const editorRef = useRef<editor.IStandaloneCodeEditor | null>(null);

  // Metadata fields
  const [dateAuthorized, setDateAuthorized] = useState(new Date().toISOString().split('T')[0]);
  const [dateExpired, setDateExpired] = useState('');
  const [systemOwner, setSystemOwner] = useState('');
  const [securityManager, setSecurityManager] = useState('');
  const [authorizingOfficial, setAuthorizingOfficial] = useState('');

  // Conditions of Approval
  const [conditions, setConditions] = useState<Condition[]>([]);

  // Digital Signature
  const [draftAuthorizationId, setDraftAuthorizationId] = useState<number | null>(null);
  const [signatureResult, setSignatureResult] = useState<unknown>(null);

  // Load SSP visualization when SSP is selected
  useEffect(() => {
    if (selectedSsp && step === 'select-ssp') {
      // You would fetch the SSP visualization here
      // For now, we'll just move to the next step
    }
  }, [selectedSsp, step]);

  // Extract variables from edited content
  const extractVariables = (text: string) => {
    const pattern = /\{\{\s*([^}]+?)\s*\}\}/g;
    const matches = text.matchAll(pattern);
    const extractedVars = new Set<string>();

    for (const match of matches) {
      extractedVars.add(match[1].trim());
    }

    return Array.from(extractedVars);
  };

  // Update detected variables when edited content changes
  useEffect(() => {
    if (editedContent) {
      const vars = extractVariables(editedContent);
      setDetectedVariables(vars);

      // Initialize variable values for any new variables
      setVariableValues((prev) => {
        const updated = { ...prev };
        vars.forEach((v) => {
          if (!(v in updated)) {
            updated[v] = '';
          }
        });
        return updated;
      });
    }
  }, [editedContent]);

  // Update completed content when variables change
  useEffect(() => {
    if (editedContent) {
      let content = editedContent;
      Object.entries(variableValues).forEach(([key, value]) => {
        const regex = new RegExp(`\\{\\{\\s*${key}\\s*\\}\\}`, 'g');
        content = content.replace(regex, value || `{{ ${key} }}`);
      });
      setCompletedContent(content);
    }
  }, [editedContent, variableValues]);

  const handleSspSelect = (ssp: LibraryItem) => {
    setSelectedSsp(ssp);
  };

  const handleSarSelect = (sar: LibraryItem) => {
    setSelectedSar(sar);
  };

  // Load visualizations for SSP and SAR
  const loadVisualizations = async () => {
    if (!selectedSsp) return;

    setLoadingVisualizations(true);
    try {
      // Fetch SSP file info and content
      const sspFile = await apiClient.getSavedFile(selectedSsp.itemId);
      if (!sspFile) {
        console.error('Failed to load SSP file');
        return;
      }

      const sspContent = await apiClient.getFileContent(selectedSsp.itemId);
      if (!sspContent) {
        console.error('Failed to load SSP content');
        return;
      }

      // Get SSP visualization
      const sspVizData = await apiClient.visualizeSSP(
        sspContent,
        sspFile.format,
        sspFile.fileName
      );
      setSspVisualization(sspVizData);

      // Get SAR visualization if SAR is selected
      if (selectedSar) {
        const sarFile = await apiClient.getSavedFile(selectedSar.itemId);
        if (sarFile) {
          const sarContent = await apiClient.getFileContent(selectedSar.itemId);
          if (sarContent) {
            const sarVizData = await apiClient.visualizeSAR(
              sarContent,
              sarFile.format,
              sarFile.fileName
            );
            setSarVisualization(sarVizData);
          }
        }
      } else {
        setSarVisualization(null);
      }
    } catch (error) {
      console.error('Failed to load visualizations:', error);
    } finally {
      setLoadingVisualizations(false);
    }
  };

  const handleTemplateSelect = (template: AuthorizationTemplateResponse) => {
    setSelectedTemplate(template);
    // Initialize edited content with template content
    setEditedContent(template.content);
    // Initialize variable values
    const initialValues: Record<string, string> = {};
    template.variables.forEach((variable) => {
      initialValues[variable] = '';
    });
    setVariableValues(initialValues);
  };

  const handleVariableChange = (variable: string, value: string) => {
    setVariableValues((prev) => ({ ...prev, [variable]: value }));
  };

  const canProceed = () => {
    switch (step) {
      case 'select-ssp':
        return selectedSsp !== null;
      case 'select-sar':
        return true; // SAR is optional, so always allow proceeding
      case 'stakeholder-info':
        return authorizationName.trim() !== '' && systemOwner.trim() !== '' && securityManager.trim() !== '' && authorizingOfficial.trim() !== '' && dateExpired !== '';
      case 'visualize':
        return true; // Visualizations are informational, always allow proceeding
      case 'select-template':
        return selectedTemplate !== null;
      case 'fill-variables':
        return detectedVariables.every((v) => variableValues[v]?.trim());
      case 'conditions':
        return true; // Conditions are optional, so always allow proceeding
      case 'review':
        return true;
      case 'sign':
        return true; // Digital signature is optional, always allow proceeding
      default:
        return false;
    }
  };

  const handleNext = async () => {
    if (step === 'select-ssp') setStep('select-sar');
    else if (step === 'select-sar') setStep('stakeholder-info');
    else if (step === 'stakeholder-info') {
      // Load visualizations when moving to visualize step
      await loadVisualizations();
      setStep('visualize');
    }
    else if (step === 'visualize') setStep('select-template');
    else if (step === 'select-template') setStep('fill-variables');
    else if (step === 'fill-variables') setStep('conditions');
    else if (step === 'conditions') setStep('review');
    else if (step === 'review') {
      // Create draft authorization before moving to sign step
      await createDraftAuthorization();
      setStep('sign');
    }
  };

  // Create draft authorization for signing
  const createDraftAuthorization = async () => {
    if (!selectedSsp || !selectedTemplate || !authorizationName) {
      toast.error('Missing required information');
      return;
    }

    try {
      const authData = {
        name: authorizationName,
        sspItemId: selectedSsp.itemId,
        sarItemId: selectedSar?.itemId,
        templateId: selectedTemplate.id,
        variableValues,
        dateAuthorized,
        dateExpired,
        systemOwner,
        securityManager,
        authorizingOfficial,
        editedContent,
        conditions: conditions.map(c => ({
          condition: c.condition,
          conditionType: c.conditionType,
          dueDate: c.dueDate || ''
        }))
      };

      const response = await apiClient.createAuthorization(authData);
      if (response) {
        setDraftAuthorizationId(response.id);
        toast.success('Authorization created - ready for signature');
      }
    } catch (error) {
      console.error('Failed to create draft authorization:', error);
      toast.error('Failed to create authorization');
    }
  };

  const handleBack = () => {
    if (step === 'sign') setStep('review');
    else if (step === 'review') setStep('conditions');
    else if (step === 'conditions') setStep('fill-variables');
    else if (step === 'fill-variables') setStep('select-template');
    else if (step === 'select-template') setStep('visualize');
    else if (step === 'visualize') setStep('stakeholder-info');
    else if (step === 'stakeholder-info') setStep('select-sar');
    else if (step === 'select-sar') setStep('select-ssp');
  };

  // Calculate progress for variables
  const getVariableProgress = () => {
    if (detectedVariables.length === 0) return 100;
    const filledCount = detectedVariables.filter(v => variableValues[v]?.trim()).length;
    return Math.round((filledCount / detectedVariables.length) * 100);
  };

  // Handle Monaco editor mount
  const handleEditorDidMount: OnMount = (editor, monaco) => {
    editorRef.current = editor;

    // Register custom language for variable highlighting
    monaco.languages.register({ id: 'markdown-template' });

    // Define tokenization rules for variables
    monaco.languages.setMonarchTokensProvider('markdown-template', {
      tokenizer: {
        root: [
          [/\{\{[^}]*\}\}/, 'variable'],
        ],
      },
    });

    // Configure dark theme for editor with purple variables
    monaco.editor.defineTheme('markdown-dark', {
      base: 'vs-dark',
      inherit: true,
      rules: [
        { token: 'variable', foreground: 'a78bfa', fontStyle: 'bold' },
      ],
      colors: {
        'editor.background': '#1e1e1e',
        'editor.foreground': '#d4d4d4',
        'editor.lineHighlightBackground': '#2a2a2a',
        'editorLineNumber.foreground': '#858585',
        'editor.selectionBackground': '#264f78',
        'editor.inactiveSelectionBackground': '#3a3d41',
      },
    });
    monaco.editor.setTheme('markdown-dark');
  };

  const handleRefreshVariables = () => {
    if (editedContent) {
      const vars = extractVariables(editedContent);
      setDetectedVariables(vars);
    }
  };

  const handleSubmit = () => {
    if (selectedSsp && selectedTemplate && authorizationName) {
      onSave({
        name: authorizationName,
        sspItemId: selectedSsp.itemId,
        sarItemId: selectedSar?.itemId, // Optional SAR item ID
        templateId: selectedTemplate.id,
        variableValues,
        dateAuthorized,
        dateExpired,
        systemOwner,
        securityManager,
        authorizingOfficial,
        editedContent, // Pass the user's edited template content
        conditions, // Include conditions of approval
      });
    }
  };

  return (
    <div className="space-y-6">
      {/* Progress Indicator */}
      <div className="flex items-center justify-between">
        <div className="flex items-center">
          {(['select-ssp', 'select-sar', 'stakeholder-info', 'visualize', 'select-template', 'fill-variables', 'conditions', 'review', 'sign'] as Step[]).map((s, index) => {
            const steps: Step[] = ['select-ssp', 'select-sar', 'stakeholder-info', 'visualize', 'select-template', 'fill-variables', 'conditions', 'review', 'sign'];
            const currentIndex = steps.indexOf(step);
            const thisIndex = steps.indexOf(s);

            const isActive = s === step;
            const isCompleted = thisIndex < currentIndex;

            const stepLabels = {
              'select-ssp': 'SSP',
              'select-sar': 'SAR',
              'stakeholder-info': 'Details',
              'visualize': 'Visualize',
              'select-template': 'Template',
              'fill-variables': 'Variables',
              'conditions': 'Conditions',
              'review': 'Review',
              'sign': 'Sign',
            };

            return (
              <div key={s} className="flex items-center">
                {index > 0 && (
                  <ChevronRight className={`h-5 w-5 mx-2 ${
                    isCompleted ? 'text-green-600' : 'text-gray-300'
                  }`} />
                )}
                <div className="flex flex-col items-center">
                  <div
                    className={`flex items-center justify-center w-10 h-10 rounded-full border-2 transition-all ${
                      isCompleted
                        ? 'border-green-600 bg-green-600 text-white'
                        : isActive
                        ? 'border-blue-600 bg-blue-600 text-white'
                        : 'border-gray-300 bg-white text-gray-400'
                    }`}
                  >
                    {isCompleted ? (
                      <CheckCircle className="h-5 w-5" />
                    ) : (
                      <span className="font-semibold">{index + 1}</span>
                    )}
                  </div>
                  <span className={`text-xs mt-1 font-medium ${
                    isActive ? 'text-blue-600' : isCompleted ? 'text-green-600' : 'text-gray-500'
                  }`}>
                    {stepLabels[s]}
                  </span>
                </div>
              </div>
            );
          })}
        </div>
      </div>

      {/* Top Navigation - Duplicate for scrolling pages */}
      <div className="flex items-center justify-between">
        <Button type="button" variant="outline" onClick={step === 'select-ssp' ? onCancel : handleBack}>
          {step === 'select-ssp' ? 'Cancel' : 'Back'}
        </Button>

        {step === 'sign' ? (
          <Button type="button" onClick={() => {
            toast.success('Authorization completed successfully');
            onCancel(); // Close wizard
          }}>
            Complete
          </Button>
        ) : (
          <Button type="button" onClick={handleNext} disabled={!canProceed()}>
            Next
          </Button>
        )}
      </div>

      {/* Step Content */}
      <Card className="p-6">
        {/* Step 1: Select SSP */}
        {step === 'select-ssp' && (
          <div className="space-y-4">
            <div>
              <h2 className="text-xl font-bold mb-2">Select System Security Plan</h2>
              <p className="text-gray-600">Choose the SSP you want to authorize</p>
            </div>

            <div className="grid gap-3 md:grid-cols-2 lg:grid-cols-3 max-h-96 overflow-y-auto">
              {sspItems.map((ssp) => (
                <Card
                  key={ssp.itemId}
                  className={`p-4 cursor-pointer transition-all relative ${
                    selectedSsp?.itemId === ssp.itemId
                      ? 'border-green-600 bg-slate-800'
                      : 'hover:border-slate-600'
                  }`}
                  onClick={() => handleSspSelect(ssp)}
                >
                  {selectedSsp?.itemId === ssp.itemId && (
                    <CheckCircle className="absolute top-2 right-2 h-5 w-5 text-green-500" />
                  )}
                  <h3 className="font-semibold truncate">{ssp.title}</h3>
                  <p className="text-sm text-slate-400 mt-1 line-clamp-2">{ssp.description}</p>
                  <div className="mt-2 flex items-center gap-2">
                    <Badge variant="secondary">{ssp.oscalType}</Badge>
                  </div>
                </Card>
              ))}
            </div>
          </div>
        )}

        {/* Step 2: Select SAR */}
        {step === 'select-sar' && (
          <div className="space-y-4">
            <div>
              <h2 className="text-xl font-bold mb-2">Select Security Assessment Report (Optional)</h2>
              <p className="text-gray-600">Choose the SAR associated with this authorization, or skip to continue</p>
            </div>

            {sarItems.length === 0 ? (
              <Card className="p-6 text-center">
                <p className="text-gray-600">No SAR files available. You can skip this step and continue.</p>
                <p className="text-sm text-gray-500 mt-2">Upload SAR files in the Validate section to make them available here.</p>
              </Card>
            ) : (
              <div className="grid gap-3 md:grid-cols-2 lg:grid-cols-3 max-h-96 overflow-y-auto">
                {sarItems.map((sar) => (
                  <Card
                    key={sar.itemId}
                    className={`p-4 cursor-pointer transition-all relative ${
                      selectedSar?.itemId === sar.itemId
                        ? 'border-green-600 bg-slate-800'
                        : 'hover:border-slate-600'
                    }`}
                    onClick={() => handleSarSelect(sar)}
                  >
                    {selectedSar?.itemId === sar.itemId && (
                      <CheckCircle className="absolute top-2 right-2 h-5 w-5 text-green-500" />
                    )}
                    <h3 className="font-semibold truncate">{sar.title}</h3>
                    <p className="text-sm text-slate-400 mt-1 line-clamp-2">{sar.description}</p>
                    <div className="mt-2 flex items-center gap-2">
                      <Badge variant="secondary">{sar.oscalType}</Badge>
                    </div>
                  </Card>
                ))}
              </div>
            )}

            {selectedSar && (
              <Card className="p-4 bg-green-900/20 border-green-600">
                <div className="flex items-center justify-between">
                  <div>
                    <p className="text-sm text-gray-400">Selected SAR</p>
                    <p className="font-semibold text-green-400">{selectedSar.title}</p>
                  </div>
                  <Button
                    type="button"
                    variant="outline"
                    size="sm"
                    onClick={() => setSelectedSar(null)}
                    className="text-red-400 border-red-400 hover:bg-red-900/20"
                  >
                    Clear Selection
                  </Button>
                </div>
              </Card>
            )}
          </div>
        )}

        {/* Step 3: Stakeholder Information */}
        {step === 'stakeholder-info' && (
          <div className="space-y-6">
            <div>
              <h2 className="text-xl font-bold mb-2">Authorization Details</h2>
              <p className="text-gray-600">Provide authorization title, dates, and stakeholder information</p>
            </div>

            {/* Authorization Title */}
            <div className="space-y-2">
              <Label htmlFor="auth-title">Authorization Title *</Label>
              <Input
                id="auth-title"
                type="text"
                placeholder="Enter authorization title..."
                value={authorizationName}
                onChange={(e) => setAuthorizationName(e.target.value)}
              />
              <p className="text-xs text-slate-400">Give this authorization a descriptive title</p>
            </div>

            <div className="grid gap-6 md:grid-cols-2">
              <div className="space-y-4">
                <h3 className="font-semibold text-lg">Authorization Dates</h3>
                <div className="space-y-2">
                  <Label htmlFor="date-authorized">Date Authorized</Label>
                  <DatePicker
                    id="date-authorized"
                    value={dateAuthorized}
                    onChange={(value) => setDateAuthorized(value)}
                    placeholder="Select authorization date"
                  />
                </div>
                <div className="space-y-2">
                  <Label htmlFor="date-expired">Date Expired *</Label>
                  <DatePicker
                    id="date-expired"
                    value={dateExpired}
                    onChange={(value) => setDateExpired(value)}
                    placeholder="Select expiration date"
                    minDate={dateAuthorized}
                  />
                  <p className="text-xs text-slate-400">Authorization expiration date (typically 3 years from authorization)</p>
                </div>
              </div>

              <div className="space-y-4">
                <h3 className="font-semibold text-lg">Stakeholders</h3>
                <div className="space-y-2">
                  <Label htmlFor="system-owner">Information System Owner *</Label>
                  <Input
                    id="system-owner"
                    type="text"
                    placeholder="Enter system owner name..."
                    value={systemOwner}
                    onChange={(e) => setSystemOwner(e.target.value)}
                  />
                </div>
                <div className="space-y-2">
                  <Label htmlFor="security-manager">Information System Security Manager *</Label>
                  <Input
                    id="security-manager"
                    type="text"
                    placeholder="Enter security manager name..."
                    value={securityManager}
                    onChange={(e) => setSecurityManager(e.target.value)}
                  />
                </div>
                <div className="space-y-2">
                  <Label htmlFor="authorizing-official">Authorizing Official *</Label>
                  <Input
                    id="authorizing-official"
                    type="text"
                    placeholder="Enter authorizing official name..."
                    value={authorizingOfficial}
                    onChange={(e) => setAuthorizingOfficial(e.target.value)}
                  />
                </div>
              </div>
            </div>
          </div>
        )}

        {/* Step 4: Visualize SSP and SAR */}
        {step === 'visualize' && (
          <div className="space-y-4">
            <div>
              <h2 className="text-xl font-bold mb-2">System Overview</h2>
              <p className="text-gray-600">Review the system and assessment information to inform your authorization decision</p>
            </div>

            {loadingVisualizations ? (
              <Card className="p-8">
                <div className="flex flex-col items-center justify-center">
                  <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-primary mb-4"></div>
                  <p className="text-muted-foreground">Loading visualizations...</p>
                </div>
              </Card>
            ) : (
              <div className="space-y-6">
                {/* SSP Visualization */}
                {sspVisualization && selectedSsp && (
                  <div>
                    <h3 className="text-lg font-semibold mb-3 flex items-center gap-2">
                      <Badge variant="default">System Security Plan</Badge>
                      {selectedSsp.title}
                    </h3>
                    <SspVisualization data={sspVisualization} />
                  </div>
                )}

                {/* SAR Visualization */}
                {sarVisualization && selectedSar && (
                  <div className="mt-6">
                    <h3 className="text-lg font-semibold mb-3 flex items-center gap-2">
                      <Badge variant="secondary">Security Assessment Report</Badge>
                      {selectedSar.title}
                    </h3>
                    <SarVisualization data={sarVisualization} />
                  </div>
                )}

                {/* Message if no SAR was selected */}
                {!selectedSar && (
                  <Card className="p-6 bg-slate-800/50 border-slate-700">
                    <p className="text-center text-slate-400">
                      No Security Assessment Report selected. Only SSP information is displayed.
                    </p>
                  </Card>
                )}
              </div>
            )}
          </div>
        )}

        {/* Step 5: Select Template */}
        {step === 'select-template' && (
          <div className="space-y-4">
            <div>
              <h2 className="text-xl font-bold mb-2">Select Authorization Template</h2>
              <p className="text-gray-600">Choose a template for the authorization</p>
            </div>

            <div className="grid gap-3 md:grid-cols-2 lg:grid-cols-3 max-h-96 overflow-y-auto">
              {templates.map((template) => (
                <Card
                  key={template.id}
                  className={`p-4 cursor-pointer transition-all relative ${
                    selectedTemplate?.id === template.id
                      ? 'border-green-600 bg-slate-800'
                      : 'hover:border-slate-600'
                  }`}
                  onClick={() => handleTemplateSelect(template)}
                >
                  {selectedTemplate?.id === template.id && (
                    <CheckCircle className="absolute top-2 right-2 h-5 w-5 text-green-500" />
                  )}
                  <h3 className="font-semibold truncate">{template.name}</h3>
                  <p className="text-sm text-slate-400 mt-2">
                    {template.variables.length} variable{template.variables.length !== 1 ? 's' : ''}
                  </p>
                  <div className="mt-2 flex flex-wrap gap-1">
                    {template.variables.slice(0, 3).map((v) => (
                      <span key={v} className="text-xs px-2 py-0.5 bg-purple-500/10 border border-purple-500/30 text-purple-400 rounded">
                        {v}
                      </span>
                    ))}
                    {template.variables.length > 3 && (
                      <span className="text-xs px-2 py-0.5 bg-slate-700 text-slate-300 rounded">
                        +{template.variables.length - 3}
                      </span>
                    )}
                  </div>
                </Card>
              ))}
            </div>
          </div>
        )}

        {/* Step 4: Edit Template & Fill Variables */}
        {step === 'fill-variables' && selectedTemplate && (
          <div className="space-y-4">
            <div>
              <h2 className="text-xl font-bold mb-2">Edit Template & Fill Variables</h2>
              <p className="text-gray-600">Edit the template content and provide values for variables</p>
            </div>

            {/* Variables Status */}
            <Card className="p-4 bg-slate-800 border-slate-700">
              <div className="flex items-start justify-between">
                <div className="flex-1">
                  <div className="flex items-center gap-2 mb-2">
                    <Label className="text-slate-200">Detected Variables</Label>
                    <Badge variant="secondary" className="bg-slate-700 text-slate-200 border-slate-600">
                      {detectedVariables.length} {detectedVariables.length === 1 ? 'variable' : 'variables'}
                    </Badge>
                  </div>

                  {detectedVariables.length > 0 ? (
                    <div className="flex flex-wrap gap-2">
                      {detectedVariables.map((variable) => (
                        <Badge
                          key={variable}
                          variant="outline"
                          className={`font-mono ${
                            variableValues[variable]?.trim()
                              ? 'bg-green-500/10 border-green-500/30 text-green-400'
                              : 'bg-purple-500/10 border-purple-500/30 text-purple-400'
                          }`}
                        >
                          {variable}
                        </Badge>
                      ))}
                    </div>
                  ) : (
                    <p className="text-sm text-slate-400">
                      No variables detected. Use <code className="px-1 py-0.5 bg-slate-900 rounded text-xs text-purple-400">{`{{ variable name }}`}</code> to add variables.
                    </p>
                  )}
                </div>

                <Button
                  type="button"
                  variant="outline"
                  size="sm"
                  onClick={handleRefreshVariables}
                  className="ml-4"
                >
                  <RefreshCw className="h-4 w-4 mr-2" />
                  Refresh
                </Button>
              </div>
            </Card>

            {/* Template Editor */}
            <div className="space-y-2">
              <Label>Template Content (Editable Markdown)</Label>
              <p className="text-xs text-slate-400 mb-2">
                Edit the template content below. You can add new sections or modify existing ones. Variables are highlighted in purple.
              </p>
              <Card className="overflow-hidden" style={{ height: '400px' }}>
                <Editor
                  height="400px"
                  language="markdown-template"
                  value={editedContent}
                  onChange={(value) => setEditedContent(value || '')}
                  onMount={handleEditorDidMount}
                  theme="markdown-dark"
                  options={{
                    readOnly: false,
                    minimap: { enabled: false },
                    fontSize: 14,
                    lineNumbers: 'on',
                    scrollBeyondLastLine: false,
                    automaticLayout: true,
                    wordWrap: 'on',
                    folding: true,
                    lineDecorationsWidth: 10,
                    lineNumbersMinChars: 3,
                    renderLineHighlight: 'line',
                    contextmenu: true,
                    selectOnLineNumbers: true,
                    roundedSelection: false,
                    cursorStyle: 'line',
                    formatOnPaste: true,
                    formatOnType: true,
                  }}
                />
              </Card>
            </div>

            {/* Progress Bar */}
            <div className="space-y-2">
              <div className="flex items-center justify-between">
                <Label>Variable Completion Progress</Label>
                <span className="text-sm font-semibold text-blue-600">{getVariableProgress()}%</span>
              </div>
              <div className="w-full bg-gray-200 rounded-full h-3">
                <div
                  className="bg-gradient-to-r from-blue-500 to-green-500 h-3 rounded-full transition-all duration-300"
                  style={{ width: `${getVariableProgress()}%` }}
                />
              </div>
              <p className="text-xs text-slate-400">
                {detectedVariables.filter(v => variableValues[v]?.trim()).length} of {detectedVariables.length} variables completed
              </p>
            </div>

            <div className="grid grid-cols-2 gap-6">
              {/* Variable Inputs */}
              <div className="space-y-4">
                <Card className="p-4">
                  <h3 className="font-semibold mb-4">Fill Variable Values</h3>
                  {detectedVariables.length > 0 ? (
                    <div className="space-y-3 max-h-96 overflow-y-auto">
                      {detectedVariables.map((variable) => (
                        <div key={variable} className="space-y-2">
                          <Label htmlFor={`var-${variable}`}>
                            {variable}
                            {variableValues[variable]?.trim() && (
                              <CheckCircle className="inline h-4 w-4 ml-2 text-green-500" />
                            )}
                          </Label>
                          <Input
                            id={`var-${variable}`}
                            type="text"
                            placeholder={`Enter ${variable}...`}
                            value={variableValues[variable] || ''}
                            onChange={(e) => handleVariableChange(variable, e.target.value)}
                          />
                        </div>
                      ))}
                    </div>
                  ) : (
                    <p className="text-sm text-slate-400">
                      No variables detected in the template. Add variables using the <code className="px-1 py-0.5 bg-slate-900 rounded text-xs">{`{{ name }}`}</code> syntax.
                    </p>
                  )}
                </Card>
              </div>

              {/* Live Preview */}
              <div className="space-y-2">
                <Label>Preview with Variables Filled</Label>
                <MarkdownPreview content={completedContent} height="500px" />
              </div>
            </div>
          </div>
        )}

        {/* Step: Conditions of Approval */}
        {step === 'conditions' && (
          <ConditionsManager
            conditions={conditions}
            onConditionsChange={setConditions}
          />
        )}

        {/* Step 8: Review */}
        {step === 'review' && selectedSsp && selectedTemplate && (
          <div className="space-y-6">
            <div>
              <h2 className="text-xl font-bold mb-2">Review Authorization</h2>
              <p className="text-gray-600">Review the authorization before signing</p>
            </div>

            <div className="space-y-6">
              {/* Authorization Metadata */}
              <div className="space-y-4">
                <h3 className="text-lg font-semibold border-b pb-2">Authorization Details</h3>
                <div className="space-y-2">
                  <Label>Authorization Title</Label>
                  <p className="font-medium text-lg">{authorizationName}</p>
                </div>

                <div className="grid gap-4 md:grid-cols-2">
                  <Card className="p-4 bg-slate-800 border-slate-700">
                    <h4 className="font-semibold mb-3 text-sm text-slate-300">Authorization Dates</h4>
                    <div className="space-y-2">
                      <div>
                        <Label className="text-xs text-slate-400">Date Authorized</Label>
                        <p className="font-medium">{new Date(dateAuthorized).toLocaleDateString('en-US', {
                          year: 'numeric',
                          month: 'long',
                          day: 'numeric'
                        })}</p>
                      </div>
                      <div>
                        <Label className="text-xs text-slate-400">Date Expired</Label>
                        <p className="font-medium">{new Date(dateExpired).toLocaleDateString('en-US', {
                          year: 'numeric',
                          month: 'long',
                          day: 'numeric'
                        })}</p>
                      </div>
                    </div>
                  </Card>

                  <Card className="p-4 bg-slate-800 border-slate-700">
                    <h4 className="font-semibold mb-3 text-sm text-slate-300">Stakeholders</h4>
                    <div className="space-y-2">
                      <div>
                        <Label className="text-xs text-slate-400">System Owner</Label>
                        <p className="font-medium">{systemOwner}</p>
                      </div>
                      <div>
                        <Label className="text-xs text-slate-400">Security Manager</Label>
                        <p className="font-medium">{securityManager}</p>
                      </div>
                      <div>
                        <Label className="text-xs text-slate-400">Authorizing Official</Label>
                        <p className="font-medium">{authorizingOfficial}</p>
                      </div>
                    </div>
                  </Card>
                </div>
              </div>

              {/* System Documents */}
              <div className="space-y-4">
                <h3 className="text-lg font-semibold border-b pb-2">System Documents</h3>
                <div className="grid gap-4 md:grid-cols-2">
                  <Card className="p-4">
                    <h4 className="font-semibold mb-2">System Security Plan</h4>
                    <p className="text-sm text-gray-600">{selectedSsp.title}</p>
                  </Card>

                  <Card className="p-4">
                    <h4 className="font-semibold mb-2">Security Assessment Report</h4>
                    <p className="text-sm text-gray-600">
                      {selectedSar ? selectedSar.title : <span className="text-gray-400 italic">Not selected</span>}
                    </p>
                  </Card>

                  <Card className="p-4">
                    <h4 className="font-semibold mb-2">Template</h4>
                    <p className="text-sm text-gray-600">{selectedTemplate.name}</p>
                  </Card>
                </div>
              </div>

              {/* Conditions of Approval Summary */}
              {conditions.length > 0 && (
                <div className="space-y-4">
                  <h3 className="text-lg font-semibold border-b pb-2">Conditions of Approval</h3>
                  <div className="space-y-2">
                    {conditions.map((condition, index) => (
                      <Card
                        key={index}
                        className={`p-3 ${
                          condition.conditionType === 'MANDATORY'
                            ? 'bg-red-900/10 border-red-800'
                            : 'bg-yellow-900/10 border-yellow-800'
                        }`}
                      >
                        <div className="flex items-start gap-3">
                          <Badge
                            variant={condition.conditionType === 'MANDATORY' ? 'destructive' : 'default'}
                            className={
                              condition.conditionType === 'MANDATORY'
                                ? 'bg-red-600 text-white mt-0.5'
                                : 'bg-yellow-600 text-white mt-0.5'
                            }
                          >
                            {condition.conditionType}
                          </Badge>
                          <div className="flex-1">
                            <p className="text-sm">{condition.condition}</p>
                            {condition.dueDate && (
                              <p className="text-xs text-slate-400 mt-1">
                                Due: {new Date(condition.dueDate).toLocaleDateString()}
                              </p>
                            )}
                          </div>
                        </div>
                      </Card>
                    ))}
                  </div>
                </div>
              )}

              {/* Completed Authorization */}
              <div className="space-y-2">
                <h3 className="text-lg font-semibold border-b pb-2">Completed Authorization</h3>
                <p className="text-xs text-slate-400 mb-2">Scroll to view the complete document</p>
                <MarkdownPreview content={completedContent} height="600px" />
              </div>
            </div>
          </div>
        )}

        {/* Step 9: Digital Signature */}
        {step === 'sign' && draftAuthorizationId && (
          <DigitalSignatureStep
            authorizationId={draftAuthorizationId}
            authorizationName={authorizationName}
            onSignatureComplete={(result) => {
              setSignatureResult(result);
              toast.success(`Authorization signed by ${result.signerName || 'Unknown'}`);
            }}
            onSkip={() => {
              toast.info('Digital signature skipped - authorization created without signature');
            }}
          />
        )}
      </Card>

      {/* Navigation */}
      <div className="flex items-center justify-between">
        <Button type="button" variant="outline" onClick={step === 'select-ssp' ? onCancel : handleBack}>
          {step === 'select-ssp' ? 'Cancel' : 'Back'}
        </Button>

        {step === 'sign' ? (
          <Button type="button" onClick={() => {
            toast.success('Authorization completed successfully');
            onCancel(); // Close wizard
          }}>
            Complete
          </Button>
        ) : (
          <Button type="button" onClick={handleNext} disabled={!canProceed()}>
            Next
          </Button>
        )}
      </div>
    </div>
  );
}
