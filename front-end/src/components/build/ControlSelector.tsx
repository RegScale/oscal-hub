'use client';

import { useState, useMemo, useEffect } from 'react';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Checkbox } from '@/components/ui/checkbox';
import { Badge } from '@/components/ui/badge';
import { apiClient } from '@/lib/api-client';
import type { LibraryItem } from '@/types/oscal';
import {
  Search,
  ChevronDown,
  ChevronRight,
  CheckCircle2,
  ArrowLeft,
  Book,
} from 'lucide-react';

// Mock NIST 800-53 controls data (in a real app, this would come from API or library)
const NIST_800_53_CONTROLS = [
  {
    family: 'AC',
    name: 'Access Control',
    controls: [
      { id: 'AC-1', title: 'Policy and Procedures' },
      { id: 'AC-2', title: 'Account Management' },
      { id: 'AC-3', title: 'Access Enforcement' },
      { id: 'AC-4', title: 'Information Flow Enforcement' },
      { id: 'AC-5', title: 'Separation of Duties' },
      { id: 'AC-6', title: 'Least Privilege' },
      { id: 'AC-7', title: 'Unsuccessful Logon Attempts' },
      { id: 'AC-8', title: 'System Use Notification' },
    ],
  },
  {
    family: 'AU',
    name: 'Audit and Accountability',
    controls: [
      { id: 'AU-1', title: 'Policy and Procedures' },
      { id: 'AU-2', title: 'Event Logging' },
      { id: 'AU-3', title: 'Content of Audit Records' },
      { id: 'AU-4', title: 'Audit Log Storage Capacity' },
      { id: 'AU-5', title: 'Response to Audit Logging Process Failures' },
      { id: 'AU-6', title: 'Audit Record Review, Analysis, and Reporting' },
    ],
  },
  {
    family: 'AT',
    name: 'Awareness and Training',
    controls: [
      { id: 'AT-1', title: 'Policy and Procedures' },
      { id: 'AT-2', title: 'Literacy Training and Awareness' },
      { id: 'AT-3', title: 'Role-Based Training' },
      { id: 'AT-4', title: 'Training Records' },
    ],
  },
  {
    family: 'CM',
    name: 'Configuration Management',
    controls: [
      { id: 'CM-1', title: 'Policy and Procedures' },
      { id: 'CM-2', title: 'Baseline Configuration' },
      { id: 'CM-3', title: 'Configuration Change Control' },
      { id: 'CM-4', title: 'Impact Analyses' },
      { id: 'CM-5', title: 'Access Restrictions for Change' },
      { id: 'CM-6', title: 'Configuration Settings' },
      { id: 'CM-7', title: 'Least Functionality' },
      { id: 'CM-8', title: 'System Component Inventory' },
    ],
  },
  {
    family: 'CP',
    name: 'Contingency Planning',
    controls: [
      { id: 'CP-1', title: 'Policy and Procedures' },
      { id: 'CP-2', title: 'Contingency Plan' },
      { id: 'CP-3', title: 'Contingency Training' },
      { id: 'CP-4', title: 'Contingency Plan Testing' },
      { id: 'CP-9', title: 'System Backup' },
      { id: 'CP-10', title: 'System Recovery and Reconstitution' },
    ],
  },
  {
    family: 'IA',
    name: 'Identification and Authentication',
    controls: [
      { id: 'IA-1', title: 'Policy and Procedures' },
      { id: 'IA-2', title: 'Identification and Authentication (Organizational Users)' },
      { id: 'IA-3', title: 'Device Identification and Authentication' },
      { id: 'IA-4', title: 'Identifier Management' },
      { id: 'IA-5', title: 'Authenticator Management' },
      { id: 'IA-6', title: 'Authentication Feedback' },
      { id: 'IA-8', title: 'Identification and Authentication (Non-Organizational Users)' },
    ],
  },
  {
    family: 'IR',
    name: 'Incident Response',
    controls: [
      { id: 'IR-1', title: 'Policy and Procedures' },
      { id: 'IR-2', title: 'Incident Response Training' },
      { id: 'IR-3', title: 'Incident Response Testing' },
      { id: 'IR-4', title: 'Incident Handling' },
      { id: 'IR-5', title: 'Incident Monitoring' },
      { id: 'IR-6', title: 'Incident Reporting' },
      { id: 'IR-8', title: 'Incident Response Plan' },
    ],
  },
  {
    family: 'MA',
    name: 'Maintenance',
    controls: [
      { id: 'MA-1', title: 'Policy and Procedures' },
      { id: 'MA-2', title: 'Controlled Maintenance' },
      { id: 'MA-3', title: 'Maintenance Tools' },
      { id: 'MA-4', title: 'Nonlocal Maintenance' },
      { id: 'MA-5', title: 'Maintenance Personnel' },
    ],
  },
  {
    family: 'MP',
    name: 'Media Protection',
    controls: [
      { id: 'MP-1', title: 'Policy and Procedures' },
      { id: 'MP-2', title: 'Media Access' },
      { id: 'MP-3', title: 'Media Marking' },
      { id: 'MP-4', title: 'Media Storage' },
      { id: 'MP-5', title: 'Media Transport' },
      { id: 'MP-6', title: 'Media Sanitization' },
      { id: 'MP-7', title: 'Media Use' },
    ],
  },
  {
    family: 'PE',
    name: 'Physical and Environmental Protection',
    controls: [
      { id: 'PE-1', title: 'Policy and Procedures' },
      { id: 'PE-2', title: 'Physical Access Authorizations' },
      { id: 'PE-3', title: 'Physical Access Control' },
      { id: 'PE-6', title: 'Monitoring Physical Access' },
      { id: 'PE-8', title: 'Visitor Access Records' },
    ],
  },
  {
    family: 'PL',
    name: 'Planning',
    controls: [
      { id: 'PL-1', title: 'Policy and Procedures' },
      { id: 'PL-2', title: 'System Security and Privacy Plans' },
      { id: 'PL-4', title: 'Rules of Behavior' },
      { id: 'PL-8', title: 'Security and Privacy Architectures' },
    ],
  },
  {
    family: 'PS',
    name: 'Personnel Security',
    controls: [
      { id: 'PS-1', title: 'Policy and Procedures' },
      { id: 'PS-2', title: 'Position Risk Designation' },
      { id: 'PS-3', title: 'Personnel Screening' },
      { id: 'PS-4', title: 'Personnel Termination' },
      { id: 'PS-5', title: 'Personnel Transfer' },
      { id: 'PS-6', title: 'Access Agreements' },
      { id: 'PS-7', title: 'External Personnel Security' },
    ],
  },
  {
    family: 'RA',
    name: 'Risk Assessment',
    controls: [
      { id: 'RA-1', title: 'Policy and Procedures' },
      { id: 'RA-2', title: 'Security Categorization' },
      { id: 'RA-3', title: 'Risk Assessment' },
      { id: 'RA-5', title: 'Vulnerability Monitoring and Scanning' },
      { id: 'RA-7', title: 'Risk Response' },
    ],
  },
  {
    family: 'SA',
    name: 'System and Services Acquisition',
    controls: [
      { id: 'SA-1', title: 'Policy and Procedures' },
      { id: 'SA-2', title: 'Allocation of Resources' },
      { id: 'SA-3', title: 'System Development Life Cycle' },
      { id: 'SA-4', title: 'Acquisition Process' },
      { id: 'SA-5', title: 'System Documentation' },
      { id: 'SA-8', title: 'Security and Privacy Engineering Principles' },
      { id: 'SA-9', title: 'External System Services' },
    ],
  },
  {
    family: 'SC',
    name: 'System and Communications Protection',
    controls: [
      { id: 'SC-1', title: 'Policy and Procedures' },
      { id: 'SC-7', title: 'Boundary Protection' },
      { id: 'SC-8', title: 'Transmission Confidentiality and Integrity' },
      { id: 'SC-12', title: 'Cryptographic Key Establishment and Management' },
      { id: 'SC-13', title: 'Cryptographic Protection' },
      { id: 'SC-20', title: 'Secure Name/Address Resolution Service (Authoritative Source)' },
      { id: 'SC-21', title: 'Secure Name/Address Resolution Service (Recursive or Caching Resolver)' },
      { id: 'SC-28', title: 'Protection of Information at Rest' },
    ],
  },
  {
    family: 'SI',
    name: 'System and Information Integrity',
    controls: [
      { id: 'SI-1', title: 'Policy and Procedures' },
      { id: 'SI-2', title: 'Flaw Remediation' },
      { id: 'SI-3', title: 'Malicious Code Protection' },
      { id: 'SI-4', title: 'System Monitoring' },
      { id: 'SI-5', title: 'Security Alerts, Advisories, and Directives' },
      { id: 'SI-12', title: 'Information Management and Retention' },
    ],
  },
];

interface Control {
  id: string;
  title: string;
}

interface SelectedControl {
  controlId: string;
  title: string;
  description: string;
}

interface ControlSelectorProps {
  onAddControls: (
    catalog: { title: string; source: string },
    controls: SelectedControl[]
  ) => void;
  onClose: () => void;
}

export function ControlSelector({ onAddControls, onClose }: ControlSelectorProps) {
  const [step, setStep] = useState<'select-catalog' | 'browse-controls'>('select-catalog');
  const [catalogs, setCatalogs] = useState<LibraryItem[]>([]);
  const [selectedCatalog, setSelectedCatalog] = useState<LibraryItem | null>(null);
  const [catalogControls, setCatalogControls] = useState<typeof NIST_800_53_CONTROLS>([]);
  const [isLoadingCatalogs, setIsLoadingCatalogs] = useState(true);
  const [searchQuery, setSearchQuery] = useState('');
  const [expandedFamilies, setExpandedFamilies] = useState<Set<string>>(new Set());
  const [selectedControls, setSelectedControls] = useState<Set<string>>(new Set());

  // Load catalogs from library on mount
  useEffect(() => {
    loadCatalogs();
  }, []);

  const loadCatalogs = async () => {
    setIsLoadingCatalogs(true);
    try {
      const items = await apiClient.getAllLibraryItems();
      // Filter for catalogs and profiles
      const catalogItems = items.filter(
        item => item.oscalType === 'catalog' || item.oscalType === 'profile'
      );
      setCatalogs(catalogItems);
    } catch (error) {
      console.error('Failed to load catalogs:', error);
      // If loading fails, set to empty array so user can still use mock data
      setCatalogs([]);
    } finally {
      setIsLoadingCatalogs(false);
    }
  };

  const selectCatalog = async (catalog: LibraryItem | 'mock') => {
    if (catalog === 'mock') {
      setCatalogControls(NIST_800_53_CONTROLS);
      setSelectedCatalog(null);
      setStep('browse-controls');
      return;
    }

    try {
      // Load catalog content
      const content = await apiClient.getLibraryItemContent(catalog.itemId);
      const catalogData = JSON.parse(content);

      // Parse controls from catalog
      const controls = parseCatalogControls(catalogData);
      setCatalogControls(controls);
      setSelectedCatalog(catalog);
      setStep('browse-controls');
    } catch (error) {
      console.error('Failed to load catalog:', error);
      // Fallback to mock data
      setCatalogControls(NIST_800_53_CONTROLS);
      setStep('browse-controls');
    }
  };

  const parseCatalogControls = (catalogData: any) => {
    // Try to parse OSCAL catalog structure
    const catalog = catalogData.catalog || catalogData.profile;
    if (!catalog) return NIST_800_53_CONTROLS;

    const groups = catalog.groups || [];
    return groups.map((group: any) => ({
      family: group.id || group.title?.substring(0, 2).toUpperCase() || 'XX',
      name: group.title || 'Unknown Family',
      controls: (group.controls || []).map((control: any) => ({
        id: control.id,
        title: control.title || control.id,
      })),
    })).filter((group: any) => group.controls.length > 0);
  };

  const toggleFamily = (family: string) => {
    const newExpanded = new Set(expandedFamilies);
    if (newExpanded.has(family)) {
      newExpanded.delete(family);
    } else {
      newExpanded.add(family);
    }
    setExpandedFamilies(newExpanded);
  };

  const toggleControl = (controlId: string) => {
    const newSelected = new Set(selectedControls);
    if (newSelected.has(controlId)) {
      newSelected.delete(controlId);
    } else {
      newSelected.add(controlId);
    }
    setSelectedControls(newSelected);
  };

  const toggleAllInFamily = (family: string, controls: Control[]) => {
    const newSelected = new Set(selectedControls);
    const familyControlIds = controls.map(c => c.id);
    const allSelected = familyControlIds.every(id => newSelected.has(id));

    if (allSelected) {
      // Deselect all in family
      familyControlIds.forEach(id => newSelected.delete(id));
    } else {
      // Select all in family
      familyControlIds.forEach(id => newSelected.add(id));
    }
    setSelectedControls(newSelected);
  };

  const filteredControls = useMemo(() => {
    const controlsToFilter = catalogControls.length > 0 ? catalogControls : NIST_800_53_CONTROLS;

    if (!searchQuery.trim()) return controlsToFilter;

    const query = searchQuery.toLowerCase();
    return controlsToFilter.map(family => ({
      ...family,
      controls: family.controls.filter(
        control =>
          control.id.toLowerCase().includes(query) ||
          control.title.toLowerCase().includes(query)
      ),
    })).filter(family => family.controls.length > 0);
  }, [searchQuery, catalogControls]);

  const handleAddSelected = () => {
    const selectedControlDetails: SelectedControl[] = [];
    const controlsToUse = catalogControls.length > 0 ? catalogControls : NIST_800_53_CONTROLS;

    controlsToUse.forEach(family => {
      family.controls.forEach(control => {
        if (selectedControls.has(control.id)) {
          selectedControlDetails.push({
            controlId: control.id,
            title: control.title,
            description: `Implementation of ${control.id}: ${control.title}`,
          });
        }
      });
    });

    // Prepare catalog information
    const catalogInfo = selectedCatalog
      ? {
          title: selectedCatalog.name,
          source: selectedCatalog.blobUrl || `library:${selectedCatalog.itemId}`,
        }
      : {
          title: 'NIST SP 800-53 Rev 5 (Sample)',
          source: 'https://doi.org/10.6028/NIST.SP.800-53r5',
        };

    onAddControls(catalogInfo, selectedControlDetails);
    onClose();
  };

  // Catalog selection view
  if (step === 'select-catalog') {
    return (
      <Card className="w-full">
        <CardHeader>
          <CardTitle>Select a Catalog</CardTitle>
          <CardDescription>
            Choose a catalog or profile to browse controls
          </CardDescription>
        </CardHeader>
        <CardContent className="space-y-4">
          {isLoadingCatalogs ? (
            <div className="flex items-center justify-center py-12">
              <div className="text-center">
                <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-primary mx-auto mb-4"></div>
                <p className="text-muted-foreground">Loading catalogs...</p>
              </div>
            </div>
          ) : (
            <>
              {catalogs.length > 0 && (
                <div className="space-y-2">
                  <Label>Available Catalogs from Library</Label>
                  <div className="grid grid-cols-1 gap-2 max-h-[400px] overflow-y-auto">
                    {catalogs.map((catalog) => (
                      <Button
                        key={catalog.itemId}
                        variant="outline"
                        className="justify-start h-auto p-4 text-left"
                        onClick={() => selectCatalog(catalog)}
                      >
                        <div className="flex items-start gap-3 w-full">
                          <Book className="h-5 w-5 mt-0.5 flex-shrink-0 text-primary" />
                          <div className="flex-1 min-w-0">
                            <div className="font-medium">{catalog.name}</div>
                            {catalog.description && (
                              <div className="text-sm text-muted-foreground line-clamp-2 mt-1">
                                {catalog.description}
                              </div>
                            )}
                            <div className="flex items-center gap-2 mt-2">
                              <Badge variant="secondary" className="text-xs">
                                {catalog.oscalType}
                              </Badge>
                              {catalog.version && (
                                <span className="text-xs text-muted-foreground">
                                  v{catalog.version}
                                </span>
                              )}
                            </div>
                          </div>
                        </div>
                      </Button>
                    ))}
                  </div>
                </div>
              )}

              <div className="border-t pt-4">
                <Label className="mb-2 block">Or use built-in catalog</Label>
                <Button
                  variant="outline"
                  className="w-full justify-start h-auto p-4"
                  onClick={() => selectCatalog('mock')}
                >
                  <div className="flex items-start gap-3">
                    <Book className="h-5 w-5 mt-0.5 flex-shrink-0 text-primary" />
                    <div>
                      <div className="font-medium">NIST SP 800-53 Rev 5 (Sample)</div>
                      <div className="text-sm text-muted-foreground mt-1">
                        Built-in sample catalog for testing
                      </div>
                    </div>
                  </div>
                </Button>
              </div>
            </>
          )}

          <div className="flex items-center justify-end gap-2 pt-4 border-t">
            <Button variant="outline" onClick={onClose}>
              Cancel
            </Button>
          </div>
        </CardContent>
      </Card>
    );
  }

  // Browse controls view
  return (
    <Card className="w-full">
      <CardHeader>
        <div className="flex items-center gap-2 mb-2">
          <Button
            variant="ghost"
            size="sm"
            onClick={() => {
              setStep('select-catalog');
              setSelectedControls(new Set());
            }}
          >
            <ArrowLeft className="h-4 w-4 mr-1" />
            Back to Catalogs
          </Button>
        </div>
        <CardTitle>
          {selectedCatalog ? selectedCatalog.name : 'NIST SP 800-53 Rev 5 (Sample)'}
        </CardTitle>
        <CardDescription>
          Browse and select controls to add to your component
        </CardDescription>
      </CardHeader>
      <CardContent className="space-y-4">
        <div className="space-y-2">
          <Label htmlFor="search">Search Controls</Label>
          <div className="relative">
            <Search className="absolute left-2 top-2.5 h-4 w-4 text-muted-foreground" />
            <Input
              id="search"
              placeholder="Search by control ID or title..."
              className="pl-8"
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
            />
          </div>
        </div>

        {selectedControls.size > 0 && (
          <div className="flex items-center justify-between p-3 bg-muted rounded-lg">
            <div className="flex items-center gap-2">
              <CheckCircle2 className="h-4 w-4 text-primary" />
              <span className="text-sm font-medium">
                {selectedControls.size} control{selectedControls.size > 1 ? 's' : ''} selected
              </span>
            </div>
            <Button
              size="sm"
              onClick={() => setSelectedControls(new Set())}
              variant="ghost"
            >
              Clear All
            </Button>
          </div>
        )}

        <div className="h-[400px] overflow-y-auto pr-4">
          <div className="space-y-2">
            {filteredControls.map((family) => {
              const isExpanded = expandedFamilies.has(family.family);
              const familyControlIds = family.controls.map(c => c.id);
              const selectedInFamily = familyControlIds.filter(id => selectedControls.has(id)).length;
              const allSelected = selectedInFamily === familyControlIds.length;

              return (
                <div key={family.family} className="border rounded-lg">
                  <div
                    className="flex items-center justify-between p-3 cursor-pointer hover:bg-muted/50 transition-colors"
                    onClick={() => toggleFamily(family.family)}
                  >
                    <div className="flex items-center gap-3">
                      {isExpanded ? (
                        <ChevronDown className="h-4 w-4 text-muted-foreground" />
                      ) : (
                        <ChevronRight className="h-4 w-4 text-muted-foreground" />
                      )}
                      <Checkbox
                        checked={allSelected}
                        onCheckedChange={() => toggleAllInFamily(family.family, family.controls)}
                        onClick={(e) => e.stopPropagation()}
                      />
                      <div>
                        <div className="font-medium">
                          {family.family} - {family.name}
                        </div>
                        <div className="text-xs text-muted-foreground">
                          {family.controls.length} controls
                        </div>
                      </div>
                    </div>
                    {selectedInFamily > 0 && (
                      <Badge variant="secondary">
                        {selectedInFamily} selected
                      </Badge>
                    )}
                  </div>

                  {isExpanded && (
                    <div className="border-t bg-muted/20 p-2 space-y-1">
                      {family.controls.map((control) => (
                        <div
                          key={control.id}
                          className="flex items-start gap-2 p-2 rounded hover:bg-background transition-colors"
                        >
                          <Checkbox
                            checked={selectedControls.has(control.id)}
                            onCheckedChange={() => toggleControl(control.id)}
                          />
                          <div className="flex-1 cursor-pointer" onClick={() => toggleControl(control.id)}>
                            <div className="font-medium text-sm">{control.id}</div>
                            <div className="text-xs text-muted-foreground">{control.title}</div>
                          </div>
                        </div>
                      ))}
                    </div>
                  )}
                </div>
              );
            })}
          </div>
        </div>

        <div className="flex items-center justify-end gap-2 pt-4 border-t">
          <Button variant="outline" onClick={onClose}>
            Cancel
          </Button>
          <Button
            onClick={handleAddSelected}
            disabled={selectedControls.size === 0}
          >
            Add {selectedControls.size} Control{selectedControls.size > 1 ? 's' : ''}
          </Button>
        </div>
      </CardContent>
    </Card>
  );
}
