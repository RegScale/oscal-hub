'use client';

import { useEffect, useMemo, useState } from 'react';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Alert, AlertDescription } from '@/components/ui/alert';
import { Badge } from '@/components/ui/badge';
import {
  ChevronLeft,
  ChevronRight,
  Save,
  CheckCircle2,
  Circle,
  Eye,
  Plus,
  AlertCircle,
  Upload,
  FileEdit,
} from 'lucide-react';
import { catalogBuilderApi } from '@/lib/api-client';
import { MetadataEditor } from '@/components/build/oscal/MetadataEditor';
import { ParamEditor } from '@/components/build/oscal/ParamEditor';
import { GroupEditor } from '@/components/build/oscal/GroupEditor';
import { ControlEditor } from '@/components/build/oscal/ControlEditor';
import { ResourcesEditor } from '@/components/build/oscal/ResourcesEditor';
import { JsonPreview } from '@/components/build/oscal/JsonPreview';
import { ImportJsonDialog } from '@/components/build/oscal/ImportJsonDialog';
import { SchemaValidationPanel } from '@/components/build/oscal/SchemaValidationPanel';
import {
  emptyCatalog,
  countCatalogControls,
  countCatalogGroups,
  countCatalogParams,
  nowIsoUtc,
  parseCatalog,
  CURRENT_OSCAL_VERSION,
} from '@/types/oscal-models';
import type { Catalog, CatalogResponse, Control } from '@/types/oscal-models';

interface CatalogBuilderWizardProps {
  editingCatalog?: CatalogResponse | null;
  initialCatalog?: { catalog: unknown } | null;
  onSaveComplete?: () => void;
  onCancel?: () => void;
}

const STEPS = [
  { id: 1, title: 'Metadata', description: 'Title, version, parties, roles' },
  { id: 2, title: 'Parameters', description: 'Top-level catalog params (optional)' },
  { id: 3, title: 'Controls', description: 'Groups and controls' },
  { id: 4, title: 'Back-matter', description: 'Resources and references' },
  { id: 5, title: 'Review & Save', description: 'Preview JSON and save' },
];

export function CatalogBuilderWizard({ editingCatalog, initialCatalog, onSaveComplete, onCancel }: CatalogBuilderWizardProps) {
  const [step, setStep] = useState(1);
  const [catalog, setCatalog] = useState<Catalog>(() => emptyCatalog());
  const [isSaving, setIsSaving] = useState(false);
  const [savingMode, setSavingMode] = useState<'draft' | 'final' | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState(false);
  const [isLoading, setIsLoading] = useState(false);
  const [showPreview, setShowPreview] = useState(false);
  const [showImport, setShowImport] = useState(false);
  const [isDraft, setIsDraft] = useState<boolean>(editingCatalog?.draft ?? true);

  // Hydrate from AI draft when provided and not in edit mode
  useEffect(() => {
    if (editingCatalog) return; // edit mode takes precedence
    if (!initialCatalog) return;
    try {
      const next = parseCatalog(initialCatalog);
      setCatalog(next);
      setStep(1);
      setError(null);
    } catch {
      // ignore malformed AI draft — form stays empty
    }
  }, [initialCatalog, editingCatalog]);

  useEffect(() => {
    let cancelled = false;
    async function load() {
      if (!editingCatalog) {
        setCatalog(emptyCatalog());
        setStep(1);
        setSuccess(false);
        setIsDraft(true);
        return;
      }
      setIsLoading(true);
      setError(null);
      setIsDraft(editingCatalog.draft);
      try {
        const raw = await catalogBuilderApi.getContent(editingCatalog.id);
        const parsed = JSON.parse(raw);
        const cat: Catalog = parsed.catalog ?? parsed;
        if (!cancelled) {
          setCatalog(cat);
          setStep(1);
        }
      } catch (e) {
        if (!cancelled) setError(`Failed to load catalog: ${e instanceof Error ? e.message : 'unknown'}`);
      } finally {
        if (!cancelled) setIsLoading(false);
      }
    }
    void load();
    return () => {
      cancelled = true;
    };
  }, [editingCatalog]);

  const stats = useMemo(() => ({
    controls: countCatalogControls(catalog),
    groups: countCatalogGroups(catalog),
    params: countCatalogParams(catalog),
    resources: catalog['back-matter']?.resources?.length ?? 0,
  }), [catalog]);

  const validation = useMemo(() => {
    const issues: string[] = [];
    if (!catalog.metadata.title.trim()) issues.push('Metadata title is required.');
    if (!catalog.metadata.version.trim()) issues.push('Metadata version is required.');
    if (!catalog.metadata['oscal-version'].trim()) issues.push('OSCAL version is required.');
    if (!catalog.uuid) issues.push('Catalog UUID is required.');
    if (stats.controls === 0 && stats.groups === 0) {
      issues.push('Add at least one group or control.');
    }
    return issues;
  }, [catalog, stats]);

  const stepReachable = (target: number) => {
    if (target <= step) return true;
    if (!catalog.metadata.title.trim()) return false;
    return true;
  };

  const handleSave = async (mode: 'draft' | 'final' = 'final') => {
    // Drafts only require a title; final saves run full validation.
    if (mode === 'draft') {
      if (!catalog.metadata.title.trim()) {
        setError('Title is required (even for drafts).');
        return;
      }
    } else if (validation.length > 0) {
      setError(validation.join(' '));
      return;
    }
    setIsSaving(true);
    setSavingMode(mode);
    setError(null);
    setSuccess(false);
    try {
      const finalCatalog: Catalog = {
        ...catalog,
        metadata: { ...catalog.metadata, 'last-modified': nowIsoUtc() },
      };
      const wrapped = { catalog: finalCatalog };
      const json = JSON.stringify(wrapped, null, 2);
      const filename = `catalog-${finalCatalog.uuid}.json`;
      const draftFlag = mode === 'draft';

      if (editingCatalog) {
        await catalogBuilderApi.update(editingCatalog.id, {
          title: finalCatalog.metadata.title,
          description: catalog.metadata.remarks,
          version: finalCatalog.metadata.version,
          jsonContent: json,
          groupCount: stats.groups,
          controlCount: stats.controls,
          paramCount: stats.params,
          draft: draftFlag,
        });
      } else {
        await catalogBuilderApi.create({
          title: finalCatalog.metadata.title,
          description: catalog.metadata.remarks,
          version: finalCatalog.metadata.version,
          oscalVersion: finalCatalog.metadata['oscal-version'] || CURRENT_OSCAL_VERSION,
          filename,
          jsonContent: json,
          oscalUuid: finalCatalog.uuid,
          groupCount: stats.groups,
          controlCount: stats.controls,
          paramCount: stats.params,
          draft: draftFlag,
        });
      }

      setIsDraft(draftFlag);
      setSuccess(true);
      setTimeout(() => {
        onSaveComplete?.();
      }, 600);
    } catch (e) {
      setError(`Save failed: ${e instanceof Error ? e.message : 'unknown error'}`);
    } finally {
      setIsSaving(false);
      setSavingMode(null);
    }
  };

  if (isLoading) {
    return (
      <Card>
        <CardContent className="py-12 text-center">
          <div className="animate-spin rounded-full h-10 w-10 border-b-2 border-primary mx-auto mb-3" />
          <p className="text-muted-foreground">Loading catalog…</p>
        </CardContent>
      </Card>
    );
  }

  return (
    <div className="space-y-4">
      <Card>
        <CardHeader>
          <div className="flex items-center justify-between">
            <div>
              <CardTitle>
                {editingCatalog ? 'Edit Catalog' : 'New Catalog'}
              </CardTitle>
              <CardDescription>
                Build an OSCAL catalog with groups, controls, parameters, and back-matter.
              </CardDescription>
            </div>
            <div className="flex items-center gap-2">
              {isDraft && <Badge variant="secondary">Draft</Badge>}
              <Badge variant="outline" className="font-mono">{stats.groups}g</Badge>
              <Badge variant="outline" className="font-mono">{stats.controls}c</Badge>
              <Badge variant="outline" className="font-mono">{stats.params}p</Badge>
              <Button
                type="button"
                size="sm"
                variant="outline"
                onClick={() => setShowImport(true)}
              >
                <Upload className="h-3.5 w-3.5 mr-1" />
                Import JSON
              </Button>
              <Button
                type="button"
                size="sm"
                variant="outline"
                onClick={() => setShowPreview((v) => !v)}
              >
                <Eye className="h-3.5 w-3.5 mr-1" />
                {showPreview ? 'Hide' : 'Show'} JSON
              </Button>
            </div>
          </div>
        </CardHeader>
      </Card>

      {showPreview && (
        <JsonPreview
          value={{ catalog }}
          filename={`catalog-${catalog.uuid}.json`}
          maxHeight="320px"
        />
      )}

      {/* Step nav */}
      <div className="flex flex-wrap items-center gap-2 px-1">
        {STEPS.map((s) => (
          <button
            key={s.id}
            type="button"
            onClick={() => stepReachable(s.id) && setStep(s.id)}
            className={`flex items-center gap-2 rounded-md px-3 py-2 text-xs font-medium transition-colors ${
              step === s.id
                ? 'bg-primary text-primary-foreground'
                : stepReachable(s.id)
                  ? 'bg-muted hover:bg-muted/80 text-muted-foreground'
                  : 'bg-muted/40 text-muted-foreground/50 cursor-not-allowed'
            }`}
          >
            {step > s.id ? (
              <CheckCircle2 className="h-3.5 w-3.5" />
            ) : (
              <Circle className="h-3.5 w-3.5" />
            )}
            <span>{s.id}. {s.title}</span>
          </button>
        ))}
      </div>

      {error && (
        <Alert variant="destructive">
          <AlertCircle className="h-4 w-4" />
          <AlertDescription>{error}</AlertDescription>
        </Alert>
      )}
      {success && (
        <Alert>
          <CheckCircle2 className="h-4 w-4" />
          <AlertDescription>
            {isDraft ? 'Draft saved. You can come back to finish it later.' : 'Catalog saved successfully.'}
          </AlertDescription>
        </Alert>
      )}

      <Card>
        <CardContent className="py-6">
          {step === 1 && (
            <MetadataEditor
              value={catalog.metadata}
              onChange={(m) => setCatalog({ ...catalog, metadata: m })}
            />
          )}

          {step === 2 && (
            <div className="space-y-4">
              <p className="text-sm text-muted-foreground">
                Top-level parameters apply to the catalog as a whole. Most catalogs leave this empty
                and define parameters per control instead.
              </p>
              <ParamEditor
                value={catalog.params}
                onChange={(p) => setCatalog({ ...catalog, params: p })}
              />
            </div>
          )}

          {step === 3 && (
            <CatalogControlsStep catalog={catalog} setCatalog={setCatalog} />
          )}

          {step === 4 && (
            <ResourcesEditor
              value={catalog['back-matter']?.resources}
              onChange={(r) => {
                if (!r || r.length === 0) {
                  const next = { ...catalog };
                  delete next['back-matter'];
                  setCatalog(next);
                } else {
                  setCatalog({ ...catalog, 'back-matter': { resources: r } });
                }
              }}
            />
          )}

          {step === 5 && (
            <ReviewStep
              catalog={catalog}
              stats={stats}
              validation={validation}
              isSaving={isSaving}
              onSave={handleSave}
            />
          )}
        </CardContent>
      </Card>

      <ImportJsonDialog
        open={showImport}
        onOpenChange={setShowImport}
        target="catalog"
        onImport={(parsed) => {
          try {
            const next = parseCatalog(parsed);
            setCatalog(next);
            setStep(1);
            setError(null);
            return null;
          } catch (e) {
            return e instanceof Error ? e.message : 'Could not parse catalog.';
          }
        }}
      />

      <div className="flex items-center justify-between">
        <Button
          type="button"
          variant="outline"
          onClick={() => (step === 1 ? onCancel?.() : setStep((s) => s - 1))}
          disabled={isSaving}
        >
          <ChevronLeft className="h-4 w-4 mr-1" />
          {step === 1 ? 'Cancel' : 'Previous'}
        </Button>
        <div className="flex items-center gap-2">
          <Button
            type="button"
            variant="outline"
            onClick={() => handleSave('draft')}
            disabled={isSaving || !catalog.metadata.title.trim()}
            title="Save current progress without strict validation"
          >
            <FileEdit className="h-4 w-4 mr-1" />
            {savingMode === 'draft' ? 'Saving…' : 'Save draft'}
          </Button>
          {step < STEPS.length ? (
            <Button
              type="button"
              onClick={() => setStep((s) => s + 1)}
              disabled={!stepReachable(step + 1)}
            >
              Next
              <ChevronRight className="h-4 w-4 ml-1" />
            </Button>
          ) : (
            <Button
              type="button"
              onClick={() => handleSave('final')}
              disabled={isSaving || validation.length > 0}
            >
              <Save className="h-4 w-4 mr-1" />
              {savingMode === 'final' ? 'Saving…' : editingCatalog ? 'Save changes' : 'Save catalog'}
            </Button>
          )}
        </div>
      </div>
    </div>
  );
}

function CatalogControlsStep({
  catalog,
  setCatalog,
}: {
  catalog: Catalog;
  setCatalog: (c: Catalog) => void;
}) {
  const groups = catalog.groups ?? [];
  const controls = catalog.controls ?? [];

  return (
    <div className="space-y-6">
      <div className="space-y-3">
        <div className="flex items-center justify-between">
          <div>
            <h3 className="text-sm font-semibold">Groups</h3>
            <p className="text-xs text-muted-foreground">
              Group controls into families. Groups can nest.
            </p>
          </div>
          <Button
            type="button"
            size="sm"
            variant="outline"
            onClick={() =>
              setCatalog({
                ...catalog,
                groups: [...groups, { id: `group-${groups.length + 1}`, title: 'New group' }],
              })
            }
          >
            <Plus className="h-3.5 w-3.5 mr-1" />
            Add group
          </Button>
        </div>
        {groups.length === 0 ? (
          <p className="text-xs italic text-muted-foreground border border-dashed rounded p-4 text-center">
            No groups yet — add one to organize your controls.
          </p>
        ) : (
          <div className="space-y-2">
            {groups.map((g, i) => (
              <GroupEditor
                key={i}
                group={g}
                onChange={(next) => {
                  const arr = [...groups];
                  arr[i] = next;
                  setCatalog({ ...catalog, groups: arr });
                }}
                onRemove={() => setCatalog({ ...catalog, groups: groups.filter((_, j) => j !== i) })}
              />
            ))}
          </div>
        )}
      </div>

      <div className="space-y-3">
        <div className="flex items-center justify-between">
          <div>
            <h3 className="text-sm font-semibold">Top-level controls (uncategorized)</h3>
            <p className="text-xs text-muted-foreground">
              Controls that aren&apos;t in any group.
            </p>
          </div>
          <Button
            type="button"
            size="sm"
            variant="outline"
            onClick={() =>
              setCatalog({
                ...catalog,
                controls: [
                  ...controls,
                  { id: `ctl-${controls.length + 1}`, title: 'New control' },
                ],
              })
            }
          >
            <Plus className="h-3.5 w-3.5 mr-1" />
            Add control
          </Button>
        </div>
        {controls.length === 0 ? (
          <p className="text-xs italic text-muted-foreground border border-dashed rounded p-4 text-center">
            None.
          </p>
        ) : (
          <div className="space-y-3">
            {controls.map((c, i) => (
              <TopLevelControl
                key={i}
                control={c}
                onChange={(next) => {
                  const arr = [...controls];
                  arr[i] = next;
                  setCatalog({ ...catalog, controls: arr });
                }}
                onRemove={() => setCatalog({ ...catalog, controls: controls.filter((_, j) => j !== i) })}
              />
            ))}
          </div>
        )}
      </div>
    </div>
  );
}

function TopLevelControl({
  control,
  onChange,
  onRemove,
}: {
  control: Control;
  onChange: (next: Control) => void;
  onRemove: () => void;
}) {
  const [expanded, setExpanded] = useState(false);
  return (
    <div className="rounded-md border bg-background/40">
      <div className="flex items-center gap-2 px-3 py-2 border-b bg-muted/20">
        <Badge variant="outline" className="font-mono">{control.id || '(no id)'}</Badge>
        <span className="text-sm flex-1 truncate">{control.title || '(untitled)'}</span>
        <Button type="button" size="sm" variant="outline" onClick={() => setExpanded((v) => !v)}>
          {expanded ? 'Collapse' : 'Edit'}
        </Button>
        <Button type="button" size="sm" variant="ghost" onClick={onRemove} className="h-7 w-7 p-0 text-destructive">
          <ChevronRight className="h-3.5 w-3.5" />
        </Button>
      </div>
      {expanded && (
        <div className="p-3">
          <ControlEditor inline control={control} onChange={onChange} />
        </div>
      )}
    </div>
  );
}

function ReviewStep({
  catalog,
  stats,
  validation,
  isSaving,
  onSave,
}: {
  catalog: Catalog;
  stats: { controls: number; groups: number; params: number; resources: number };
  validation: string[];
  isSaving: boolean;
  onSave: () => void;
}) {
  const json = JSON.stringify({ catalog }, null, 2);
  return (
    <div className="space-y-4">
      <div className="grid grid-cols-2 md:grid-cols-4 gap-3">
        <StatCard label="Groups" value={stats.groups} />
        <StatCard label="Controls" value={stats.controls} />
        <StatCard label="Parameters" value={stats.params} />
        <StatCard label="Resources" value={stats.resources} />
      </div>

      {validation.length > 0 ? (
        <Alert variant="destructive">
          <AlertCircle className="h-4 w-4" />
          <AlertDescription>
            <ul className="list-disc list-inside">
              {validation.map((v, i) => (
                <li key={i}>{v}</li>
              ))}
            </ul>
          </AlertDescription>
        </Alert>
      ) : (
        <Alert>
          <CheckCircle2 className="h-4 w-4" />
          <AlertDescription>Catalog passes basic validation. Ready to save.</AlertDescription>
        </Alert>
      )}

      <SchemaValidationPanel jsonContent={json} modelType="catalog" />

      <JsonPreview value={{ catalog }} filename={`catalog-${catalog.uuid}.json`} />

      <div className="flex justify-end">
        <Button onClick={onSave} disabled={isSaving || validation.length > 0}>
          <Save className="h-4 w-4 mr-1" />
          {isSaving ? 'Saving…' : 'Save catalog'}
        </Button>
      </div>
    </div>
  );
}

function StatCard({ label, value }: { label: string; value: number }) {
  return (
    <div className="rounded-md border p-3 text-center bg-card">
      <p className="text-2xl font-semibold">{value}</p>
      <p className="text-xs text-muted-foreground">{label}</p>
    </div>
  );
}
