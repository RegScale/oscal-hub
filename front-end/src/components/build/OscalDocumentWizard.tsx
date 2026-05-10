'use client';

import { useEffect, useMemo, useState } from 'react';
import { toast } from 'sonner';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Alert, AlertDescription } from '@/components/ui/alert';
import { Badge } from '@/components/ui/badge';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import {
  ChevronLeft,
  ChevronRight,
  Save,
  CheckCircle2,
  Circle,
  Eye,
  AlertCircle,
  Upload,
  FileEdit,
  RotateCcw,
  Library as LibraryIcon,
} from 'lucide-react';
import { oscalDocumentApi } from '@/lib/api-client';
import { libraryPublishApi } from '@/lib/api/library';
import { MetadataEditor } from '@/components/build/oscal/MetadataEditor';
import { ResourcesEditor } from '@/components/build/oscal/ResourcesEditor';
import { JsonPreview } from '@/components/build/oscal/JsonPreview';
import { ImportJsonDialog } from '@/components/build/oscal/ImportJsonDialog';
import { SchemaValidationPanel } from '@/components/build/oscal/SchemaValidationPanel';
import { SaveToLibraryModal } from '@/components/library/SaveToLibraryModal';
import { LazyMonacoEditor } from '@/components/lazy/LazyMonacoEditor';
import { AiConfidencePanel } from '@/components/build/oscal/AiConfidencePanel';
import { ControlImplementationEditor } from '@/components/build/oscal/ControlImplementationEditor';
import { PoamItemsEditor } from '@/components/build/oscal/PoamItemsEditor';
import {
  emptyMetadata,
  emptyOscalDocument,
  modelLabel,
  modelRootKey,
  modelImportKey,
  nowIsoUtc,
  generateUuid,
  summarizeOscalDocument,
  CURRENT_OSCAL_VERSION,
} from '@/types/oscal-models';
import type {
  GenericOscalModelSlug,
  Metadata,
  OscalDocumentResponse,
  Resource,
} from '@/types/oscal-models';
import type { OscalModelType } from '@/types/oscal';

interface OscalDocumentWizardProps {
  modelType: GenericOscalModelSlug;
  editingDocument?: OscalDocumentResponse | null;
  /**
   * AI-generated draft to seed a fresh wizard. Ignored when editingDocument
   * is set. Shape: the wrapped JSON body, e.g. `{ "system-security-plan": {...} }`.
   */
  initialDocument?: unknown;
  onSaveComplete?: () => void;
  onCancel?: () => void;
  /**
   * Caller's organization id, forwarded to the Save-to-Library modal so
   * the user can publish at ORGANIZATION visibility. `null` when the user
   * has no org membership.
   */
  userOrganizationId?: number | null;
}

const STEPS = [
  { id: 1, title: 'Metadata', description: 'Title, version, parties' },
  { id: 2, title: 'Import', description: 'Source document reference' },
  { id: 3, title: 'Body', description: 'Model-specific content' },
  { id: 4, title: 'Back-matter', description: 'Resources and references' },
  { id: 5, title: 'Review & Save', description: 'Validate, preview, save' },
];

interface ParsedDocument {
  uuid: string;
  metadata: Metadata;
  importHref: string;
  importSystemId?: string;
  body: Record<string, unknown>;
  backMatterResources: Resource[];
}

function emptyParsedDoc(modelType: GenericOscalModelSlug): ParsedDocument {
  const skeleton = emptyOscalDocument(modelType, 'New ' + modelLabel(modelType));
  const root = skeleton[modelRootKey(modelType)] as Record<string, unknown>;
  const importKey = modelImportKey(modelType);
  const importBlock = importKey ? (root[importKey] as { href?: string } | undefined) : undefined;

  // Strip out fields we manage separately so the Monaco body doesn't duplicate them
  const body: Record<string, unknown> = { ...root };
  delete body.uuid;
  delete body.metadata;
  delete body['back-matter'];
  if (importKey) delete body[importKey];

  return {
    uuid: typeof root.uuid === 'string' ? root.uuid : generateUuid(),
    metadata: (root.metadata as Metadata) ?? emptyMetadata('New document'),
    importHref: importBlock?.href ?? '',
    body,
    backMatterResources: [],
  };
}

function parseLoadedDoc(modelType: GenericOscalModelSlug, raw: string): ParsedDocument {
  let parsed: unknown;
  try {
    parsed = JSON.parse(raw);
  } catch {
    parsed = {};
  }
  const wrapped = parsed as Record<string, unknown>;
  const root = (wrapped[modelRootKey(modelType)] ?? parsed) as Record<string, unknown>;
  const importKey = modelImportKey(modelType);

  const body: Record<string, unknown> = { ...root };
  delete body.uuid;
  delete body.metadata;
  delete body['back-matter'];
  if (importKey) delete body[importKey];

  const importBlock = importKey ? (root[importKey] as { href?: string; 'system-id'?: { id?: string } } | undefined) : undefined;
  const backMatter = root['back-matter'] as { resources?: Resource[] } | undefined;

  return {
    uuid: (root.uuid as string) ?? generateUuid(),
    metadata: (root.metadata as Metadata) ?? emptyMetadata('Imported'),
    importHref: importBlock?.href ?? '',
    importSystemId: importBlock?.['system-id']?.id,
    body,
    backMatterResources: backMatter?.resources ?? [],
  };
}

function reassembleDoc(modelType: GenericOscalModelSlug, doc: ParsedDocument): Record<string, unknown> {
  const root: Record<string, unknown> = {
    uuid: doc.uuid,
    metadata: doc.metadata,
    ...doc.body,
  };
  const importKey = modelImportKey(modelType);
  if (importKey) {
    if (doc.importHref.trim()) {
      root[importKey] = { href: doc.importHref };
    } else {
      delete root[importKey];
    }
  }
  if (doc.backMatterResources.length > 0) {
    root['back-matter'] = { resources: doc.backMatterResources };
  } else {
    delete root['back-matter'];
  }
  return { [modelRootKey(modelType)]: root };
}

export function OscalDocumentWizard({
  modelType,
  editingDocument,
  initialDocument,
  onSaveComplete,
  onCancel,
  userOrganizationId,
}: OscalDocumentWizardProps) {
  const [step, setStep] = useState(1);
  const [doc, setDoc] = useState<ParsedDocument>(() => emptyParsedDoc(modelType));
  const [bodyText, setBodyText] = useState<string>(() =>
    JSON.stringify(emptyParsedDoc(modelType).body, null, 2),
  );
  const [bodyError, setBodyError] = useState<string | null>(null);
  const [isSaving, setIsSaving] = useState(false);
  const [savingMode, setSavingMode] = useState<'draft' | 'final' | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState(false);
  const [isLoading, setIsLoading] = useState(false);
  const [showPreview, setShowPreview] = useState(false);
  const [showImport, setShowImport] = useState(false);
  const [isDraft, setIsDraft] = useState<boolean>(editingDocument?.draft ?? true);
  // Track the saved-row id so "Save to Library" works after a fresh create
  // as well as in edit mode. One id covers all four model types because
  // the backend route is the same: /build/oscal-documents/{id}/save-to-library.
  const [savedDocId, setSavedDocId] = useState<number | null>(editingDocument?.id ?? null);
  const [saveToLibOpen, setSaveToLibOpen] = useState(false);

  // Parse the body Monaco text into the doc.body whenever it changes (best-effort).
  useEffect(() => {
    try {
      const parsed = JSON.parse(bodyText);
      if (parsed && typeof parsed === 'object' && !Array.isArray(parsed)) {
        setDoc((prev) => ({ ...prev, body: parsed as Record<string, unknown> }));
        setBodyError(null);
      }
    } catch (e) {
      setBodyError(e instanceof Error ? e.message : 'Invalid JSON');
    }
  }, [bodyText]);

  // Load editing doc, seed from AI draft, or reset to an empty document.
  useEffect(() => {
    let cancelled = false;
    async function load() {
      if (editingDocument) {
        setIsLoading(true);
        setError(null);
        setIsDraft(editingDocument.draft);
        setSavedDocId(editingDocument.id);
        try {
          const raw = await oscalDocumentApi.getContent(editingDocument.id);
          const next = parseLoadedDoc(modelType, raw);
          if (!cancelled) {
            setDoc(next);
            setBodyText(JSON.stringify(next.body, null, 2));
            setStep(1);
          }
        } catch (e) {
          if (!cancelled) setError(`Failed to load document: ${e instanceof Error ? e.message : 'unknown'}`);
        } finally {
          if (!cancelled) setIsLoading(false);
        }
        return;
      }

      if (initialDocument) {
        try {
          const next = parseLoadedDoc(modelType, JSON.stringify(initialDocument));
          if (!cancelled) {
            setDoc(next);
            setBodyText(JSON.stringify(next.body, null, 2));
            setStep(1);
            setSuccess(false);
            setIsDraft(true);
            setSavedDocId(null);
          }
        } catch (e) {
          if (!cancelled) setError(`Failed to seed document: ${e instanceof Error ? e.message : 'unknown'}`);
        }
        return;
      }

      const fresh = emptyParsedDoc(modelType);
      if (!cancelled) {
        setDoc(fresh);
        setBodyText(JSON.stringify(fresh.body, null, 2));
        setStep(1);
        setSuccess(false);
        setIsDraft(true);
        setSavedDocId(null);
      }
    }
    void load();
    return () => {
      cancelled = true;
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [editingDocument, initialDocument, modelType]);

  const importKey = modelImportKey(modelType);
  // SSP's import-profile is optional in this app — the AI wizard offers a
  // "skip profile" mode and users can save and refine the plan without
  // committing to a baseline yet. POA&M doesn't have an import either.
  const importRequiredForFinal =
    modelType !== 'plan-of-action-and-milestones' && modelType !== 'system-security-plan';

  const stats = useMemo(() => summarizeOscalDocument(modelType, reassembleDoc(modelType, doc)), [modelType, doc]);

  const validation = useMemo(() => {
    const issues: string[] = [];
    if (!doc.metadata.title.trim()) issues.push('Metadata title is required.');
    if (!doc.metadata.version.trim()) issues.push('Metadata version is required.');
    if (!doc.metadata['oscal-version'].trim()) issues.push('OSCAL version is required.');
    if (!doc.uuid) issues.push('Document UUID is required.');
    if (importRequiredForFinal && importKey && !doc.importHref.trim()) {
      issues.push(`${importKey} href is required.`);
    }
    if (bodyError) issues.push(`Body JSON is invalid: ${bodyError}`);
    return issues;
  }, [doc, importKey, importRequiredForFinal, bodyError]);

  const stepReachable = (target: number) => {
    if (target <= step) return true;
    if (!doc.metadata.title.trim()) return false;
    return true;
  };

  const handleSave = async (mode: 'draft' | 'final' = 'final') => {
    if (mode === 'draft') {
      if (!doc.metadata.title.trim()) {
        setError('Title is required (even for drafts).');
        return;
      }
      if (bodyError) {
        setError(`Cannot save: body JSON is invalid (${bodyError}).`);
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
      const finalDoc: ParsedDocument = {
        ...doc,
        metadata: { ...doc.metadata, 'last-modified': nowIsoUtc() },
      };
      const wrapped = reassembleDoc(modelType, finalDoc);
      const json = JSON.stringify(wrapped, null, 2);
      const filename = `${modelType}-${finalDoc.uuid}.json`;
      const draftFlag = mode === 'draft';
      const statsJson = JSON.stringify(stats);

      if (editingDocument) {
        const updated = await oscalDocumentApi.update(editingDocument.id, {
          modelType,
          title: finalDoc.metadata.title,
          description: finalDoc.metadata.remarks,
          version: finalDoc.metadata.version,
          oscalVersion: finalDoc.metadata['oscal-version'],
          filename,
          jsonContent: json,
          statsJson,
          draft: draftFlag,
        });
        setSavedDocId(updated.id);
      } else {
        const created = await oscalDocumentApi.create({
          modelType,
          title: finalDoc.metadata.title,
          description: finalDoc.metadata.remarks,
          version: finalDoc.metadata.version,
          oscalVersion: finalDoc.metadata['oscal-version'] || CURRENT_OSCAL_VERSION,
          filename,
          jsonContent: json,
          oscalUuid: finalDoc.uuid,
          statsJson,
          draft: draftFlag,
        });
        setSavedDocId(created.id);
      }

      setIsDraft(draftFlag);
      setSuccess(true);
      setTimeout(() => onSaveComplete?.(), 600);
    } catch (e) {
      setError(`Save failed: ${e instanceof Error ? e.message : 'unknown error'}`);
    } finally {
      setIsSaving(false);
      setSavingMode(null);
    }
  };

  const handleResetBody = () => {
    const fresh = emptyParsedDoc(modelType);
    setBodyText(JSON.stringify(fresh.body, null, 2));
  };

  if (isLoading) {
    return (
      <Card>
        <CardContent className="py-12 text-center">
          <div className="animate-spin rounded-full h-10 w-10 border-b-2 border-primary mx-auto mb-3" />
          <p className="text-muted-foreground">Loading document…</p>
        </CardContent>
      </Card>
    );
  }

  const label = modelLabel(modelType);

  return (
    <div className="space-y-4">
      <Card>
        <CardHeader>
          <div className="flex items-center justify-between">
            <div>
              <CardTitle>{editingDocument ? `Edit ${label}` : `New ${label}`}</CardTitle>
              <CardDescription>
                Build an OSCAL {label} with metadata, references, and a JSON body editor for
                model-specific content.
              </CardDescription>
            </div>
            <div className="flex items-center gap-2">
              {isDraft && <Badge variant="secondary">Draft</Badge>}
              {stats.map((s) => (
                <Badge key={s.label} variant="outline" className="font-mono">
                  {s.value} {s.label}
                </Badge>
              ))}
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
          value={reassembleDoc(modelType, doc)}
          filename={`${modelType}-${doc.uuid}.json`}
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
            {step > s.id ? <CheckCircle2 className="h-3.5 w-3.5" /> : <Circle className="h-3.5 w-3.5" />}
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
            {isDraft ? 'Draft saved. You can come back to finish it later.' : `${label} saved successfully.`}
          </AlertDescription>
        </Alert>
      )}

      <Card>
        <CardContent className="py-6">
          {step === 1 && (
            <MetadataEditor
              value={doc.metadata}
              onChange={(m) => setDoc({ ...doc, metadata: m })}
            />
          )}

          {step === 2 && (
            <div className="space-y-3">
              <div>
                <h3 className="text-sm font-semibold">{importKey ?? 'Import'}</h3>
                <p className="text-xs text-muted-foreground">
                  {importKey
                    ? `Reference the source document this ${label} is based on.`
                    : 'No import section for this model type.'}
                </p>
              </div>
              {importKey && (
                <div className="space-y-1">
                  <Label className="text-xs">href {importRequiredForFinal && '*'}</Label>
                  <Input
                    value={doc.importHref}
                    onChange={(e) => setDoc({ ...doc, importHref: e.target.value })}
                    placeholder="#source-uuid  or  https://..."
                    className="font-mono text-xs"
                  />
                  <p className="text-xs text-muted-foreground">
                    Use <code className="font-mono">#&lt;resource-uuid&gt;</code> to reference a back-matter resource,
                    or a full URI for an external document.
                    {modelType === 'plan-of-action-and-milestones' &&
                      ' For a POA&M, this is optional — leave blank if you supply a system-id directly in the body JSON.'}
                  </p>
                </div>
              )}
            </div>
          )}

          {step === 3 && (
            <div className="space-y-3">
              <div className="flex items-end justify-between gap-2">
                <div>
                  <h3 className="text-sm font-semibold">{label} body</h3>
                  <p className="text-xs text-muted-foreground">
                    Edit model-specific JSON. Required keys vary by model — see the{' '}
                    <a
                      href={`https://pages.nist.gov/OSCAL-Reference/models/v${CURRENT_OSCAL_VERSION}/${modelType}/json-reference/`}
                      target="_blank"
                      rel="noopener noreferrer"
                      className="text-primary underline"
                    >
                      OSCAL reference
                    </a>
                    .
                  </p>
                </div>
                <Button type="button" size="sm" variant="outline" onClick={handleResetBody}>
                  <RotateCcw className="h-3.5 w-3.5 mr-1" />
                  Reset to template
                </Button>
              </div>
              {bodyError && (
                <Alert variant="destructive">
                  <AlertCircle className="h-4 w-4" />
                  <AlertDescription>{bodyError}</AlertDescription>
                </Alert>
              )}
              {modelType === 'system-security-plan' ? (
                <>
                  <AiConfidencePanel body={doc.body} />
                  <ControlImplementationEditor
                    body={doc.body}
                    onChange={(next) => {
                      setDoc((prev) => ({ ...prev, body: next }));
                      setBodyText(JSON.stringify(next, null, 2));
                    }}
                  />
                  <details className="rounded-md border bg-muted/10">
                    <summary className="cursor-pointer select-none px-3 py-2 text-sm font-medium">
                      Advanced — edit raw JSON body
                    </summary>
                    <div className="border-t overflow-hidden">
                      <LazyMonacoEditor
                        height="500px"
                        defaultLanguage="json"
                        theme="vs-dark"
                        value={bodyText}
                        onChange={(v) => setBodyText(v ?? '')}
                        options={{
                          minimap: { enabled: false },
                          formatOnPaste: true,
                          formatOnType: true,
                          tabSize: 2,
                          wordWrap: 'on',
                          scrollBeyondLastLine: false,
                        }}
                      />
                    </div>
                  </details>
                </>
              ) : modelType === 'plan-of-action-and-milestones' ? (
                <>
                  <PoamItemsEditor
                    body={doc.body}
                    onChange={(next) => {
                      setDoc((prev) => ({ ...prev, body: next }));
                      setBodyText(JSON.stringify(next, null, 2));
                    }}
                  />
                  <details className="rounded-md border bg-muted/10">
                    <summary className="cursor-pointer select-none px-3 py-2 text-sm font-medium">
                      Advanced — edit raw JSON body
                    </summary>
                    <div className="border-t overflow-hidden">
                      <LazyMonacoEditor
                        height="500px"
                        defaultLanguage="json"
                        theme="vs-dark"
                        value={bodyText}
                        onChange={(v) => setBodyText(v ?? '')}
                        options={{
                          minimap: { enabled: false },
                          formatOnPaste: true,
                          formatOnType: true,
                          tabSize: 2,
                          wordWrap: 'on',
                          scrollBeyondLastLine: false,
                        }}
                      />
                    </div>
                  </details>
                </>
              ) : (
                <div className="rounded-md border overflow-hidden">
                  <LazyMonacoEditor
                    height="500px"
                    defaultLanguage="json"
                    theme="vs-dark"
                    value={bodyText}
                    onChange={(v) => setBodyText(v ?? '')}
                    options={{
                      minimap: { enabled: false },
                      formatOnPaste: true,
                      formatOnType: true,
                      tabSize: 2,
                      wordWrap: 'on',
                      scrollBeyondLastLine: false,
                    }}
                  />
                </div>
              )}
            </div>
          )}

          {step === 4 && (
            <ResourcesEditor
              value={doc.backMatterResources}
              onChange={(r) => setDoc({ ...doc, backMatterResources: r ?? [] })}
            />
          )}

          {step === 5 && (
            <ReviewStep
              wrapped={reassembleDoc(modelType, doc)}
              modelType={modelType}
              validation={validation}
              uuid={doc.uuid}
              isSaving={isSaving}
              onSave={() => handleSave('final')}
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
            const next = parseLoadedDoc(modelType, JSON.stringify(parsed));
            setDoc(next);
            setBodyText(JSON.stringify(next.body, null, 2));
            setStep(1);
            setError(null);
            return null;
          } catch (e) {
            return e instanceof Error ? e.message : 'Could not parse document.';
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
            disabled={isSaving || !doc.metadata.title.trim() || !!bodyError}
            title="Save current progress without strict validation"
          >
            <FileEdit className="h-4 w-4 mr-1" />
            {savingMode === 'draft' ? 'Saving…' : 'Save draft'}
          </Button>
          <Button
            type="button"
            variant="outline"
            onClick={() => setSaveToLibOpen(true)}
            disabled={isSaving || savedDocId == null}
            title={
              savedDocId == null
                ? `Save the ${label.toLowerCase()} first, then publish it to the Library`
                : `Publish this ${label.toLowerCase()} to the Library`
            }
          >
            <LibraryIcon className="h-4 w-4 mr-1" />
            Save to Library
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
              {savingMode === 'final' ? 'Saving…' : editingDocument ? 'Save changes' : `Save ${label.toLowerCase()}`}
            </Button>
          )}
        </div>
      </div>

      <SaveToLibraryModal
        open={saveToLibOpen}
        onClose={() => setSaveToLibOpen(false)}
        defaultTitle={doc.metadata.title}
        defaultDescription={doc.metadata.remarks}
        userOrganizationId={userOrganizationId ?? null}
        onSubmit={async (req) => {
          if (savedDocId == null) return;
          try {
            await libraryPublishApi.saveOscalDocumentToLibrary(savedDocId, req);
            toast.success(`${label} published to Library`);
          } catch (e) {
            toast.error(
              `Failed to publish ${label.toLowerCase()}: ${e instanceof Error ? e.message : 'unknown error'}`,
            );
            throw e;
          }
        }}
      />
    </div>
  );
}

function ReviewStep({
  wrapped,
  modelType,
  validation,
  uuid,
  isSaving,
  onSave,
}: {
  wrapped: Record<string, unknown>;
  modelType: GenericOscalModelSlug;
  validation: string[];
  uuid: string;
  isSaving: boolean;
  onSave: () => void;
}) {
  const json = JSON.stringify(wrapped, null, 2);
  const validatorModelType: OscalModelType =
    modelType === 'plan-of-action-and-milestones' ? 'plan-of-action-and-milestones' : modelType;

  return (
    <div className="space-y-4">
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
          <AlertDescription>Document passes basic validation. Ready to save.</AlertDescription>
        </Alert>
      )}

      <SchemaValidationPanel jsonContent={json} modelType={validatorModelType} />

      <JsonPreview value={wrapped} filename={`${modelType}-${uuid}.json`} />

      <div className="flex justify-end">
        <Button onClick={onSave} disabled={isSaving || validation.length > 0}>
          <Save className="h-4 w-4 mr-1" />
          {isSaving ? 'Saving…' : 'Save'}
        </Button>
      </div>
    </div>
  );
}
