'use client';

import { useEffect, useState } from 'react';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Badge } from '@/components/ui/badge';
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from '@/components/ui/alert-dialog';
import { Dialog, DialogContent, DialogHeader, DialogTitle } from '@/components/ui/dialog';
import {
  Plus,
  Search,
  Eye,
  Download,
  Edit2,
  Trash2,
  FileJson,
  Loader2,
  AlertCircle,
  Calendar,
  Library,
  Layers,
  Server,
  ClipboardList,
  ClipboardCheck,
  Target,
} from 'lucide-react';
import { catalogBuilderApi, profileBuilderApi, oscalDocumentApi } from '@/lib/api-client';
import {
  modelLabel,
  summarizeOscalDocument,
} from '@/types/oscal-models';
import type {
  CatalogResponse,
  ProfileBuildResponse,
  OscalDocumentResponse,
  GenericOscalModelSlug,
} from '@/types/oscal-models';

type DocType = 'catalog' | 'profile' | GenericOscalModelSlug;

interface BuiltDoc {
  id: number;
  oscalUuid: string;
  title: string;
  description?: string;
  version?: string;
  oscalVersion: string;
  filename: string;
  fileSize: number;
  draft: boolean;
  createdBy: string;
  createdAt: string;
  updatedAt: string;
  /** doc-type specific count summaries */
  primary?: { label: string; value: number }[];
}

function adaptCatalog(c: CatalogResponse): BuiltDoc {
  return {
    id: c.id,
    oscalUuid: c.oscalUuid,
    title: c.title,
    description: c.description,
    version: c.version,
    oscalVersion: c.oscalVersion,
    filename: c.filename,
    fileSize: c.fileSize,
    draft: c.draft ?? false,
    createdBy: c.createdBy,
    createdAt: c.createdAt,
    updatedAt: c.updatedAt,
    primary: [
      { label: 'controls', value: c.controlCount ?? 0 },
      { label: 'groups', value: c.groupCount ?? 0 },
    ],
  };
}

function adaptProfile(p: ProfileBuildResponse): BuiltDoc {
  return {
    id: p.id,
    oscalUuid: p.oscalUuid,
    title: p.title,
    description: p.description,
    version: p.version,
    oscalVersion: p.oscalVersion,
    filename: p.filename,
    fileSize: p.fileSize,
    draft: p.draft ?? false,
    createdBy: p.createdBy,
    createdAt: p.createdAt,
    updatedAt: p.updatedAt,
    primary: [
      { label: 'imports', value: p.importCount ?? 0 },
      { label: 'alters', value: p.alterCount ?? 0 },
    ],
  };
}

function adaptOscalDocument(d: OscalDocumentResponse): BuiltDoc {
  let primary: { label: string; value: number }[] = [];
  if (d.statsJson) {
    try {
      const parsed = JSON.parse(d.statsJson);
      if (Array.isArray(parsed)) primary = parsed;
    } catch {
      // Fall through to empty
    }
  }
  if (primary.length === 0) {
    primary = summarizeOscalDocument(d.modelType, {});
  }
  return {
    id: d.id,
    oscalUuid: d.oscalUuid,
    title: d.title,
    description: d.description,
    version: d.version,
    oscalVersion: d.oscalVersion,
    filename: d.filename,
    fileSize: d.fileSize,
    draft: d.draft ?? false,
    createdBy: d.createdBy,
    createdAt: d.createdAt,
    updatedAt: d.updatedAt,
    primary,
  };
}

function iconFor(t: DocType): typeof Library {
  switch (t) {
    case 'catalog': return Library;
    case 'profile': return Layers;
    case 'system-security-plan': return Server;
    case 'assessment-plan': return ClipboardList;
    case 'assessment-results': return ClipboardCheck;
    case 'plan-of-action-and-milestones': return Target;
  }
}

function labelFor(t: DocType): string {
  if (t === 'catalog') return 'Catalog';
  if (t === 'profile') return 'Profile';
  return modelLabel(t);
}

interface BuiltDocListProps {
  docType: DocType;
  onCreateNew: () => void;
  onEdit?: (doc: CatalogResponse | ProfileBuildResponse | OscalDocumentResponse) => void;
  /** External signal to force a reload (incremented when something changes). */
  reloadKey?: number;
}

export function BuiltDocList({ docType, onCreateNew, onEdit, reloadKey }: BuiltDocListProps) {
  const [docs, setDocs] = useState<BuiltDoc[]>([]);
  const [rawDocs, setRawDocs] = useState<(CatalogResponse | ProfileBuildResponse | OscalDocumentResponse)[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [searchQuery, setSearchQuery] = useState('');

  const [viewing, setViewing] = useState<BuiltDoc | null>(null);
  const [viewJson, setViewJson] = useState<string>('');
  const [viewLoading, setViewLoading] = useState(false);

  const [deletingId, setDeletingId] = useState<number | null>(null);
  const [deleteBusy, setDeleteBusy] = useState(false);

  const label = labelFor(docType);
  const labelLower = label.toLowerCase();
  const Icon = iconFor(docType);

  const load = async () => {
    setIsLoading(true);
    setError(null);
    try {
      if (docType === 'catalog') {
        const data = await catalogBuilderApi.list();
        setRawDocs(data);
        setDocs(data.map(adaptCatalog));
      } else if (docType === 'profile') {
        const data = await profileBuilderApi.list();
        setRawDocs(data);
        setDocs(data.map(adaptProfile));
      } else {
        const data = await oscalDocumentApi.list(docType);
        setRawDocs(data);
        setDocs(data.map(adaptOscalDocument));
      }
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Failed to load');
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    void load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [docType, reloadKey]);

  const filtered = docs.filter((d) => {
    if (!searchQuery.trim()) return true;
    const q = searchQuery.toLowerCase();
    return (
      d.title.toLowerCase().includes(q) ||
      (d.description?.toLowerCase().includes(q) ?? false) ||
      (d.version?.toLowerCase().includes(q) ?? false)
    );
  });

  const fetchContent = async (id: number): Promise<string> => {
    if (docType === 'catalog') return catalogBuilderApi.getContent(id);
    if (docType === 'profile') return profileBuilderApi.getContent(id);
    return oscalDocumentApi.getContent(id);
  };

  const removeDoc = async (id: number): Promise<void> => {
    if (docType === 'catalog') return catalogBuilderApi.remove(id);
    if (docType === 'profile') return profileBuilderApi.remove(id);
    return oscalDocumentApi.remove(id);
  };

  const handleView = async (doc: BuiltDoc) => {
    setViewing(doc);
    setViewLoading(true);
    setViewJson('');
    try {
      const content = await fetchContent(doc.id);
      try {
        setViewJson(JSON.stringify(JSON.parse(content), null, 2));
      } catch {
        setViewJson(content);
      }
    } catch (e) {
      setViewJson(`/* Failed to load: ${e instanceof Error ? e.message : 'unknown'} */`);
    } finally {
      setViewLoading(false);
    }
  };

  const handleDownload = async (doc: BuiltDoc) => {
    try {
      const content = await fetchContent(doc.id);
      const blob = new Blob([content], { type: 'application/json' });
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = doc.filename;
      a.click();
      URL.revokeObjectURL(url);
    } catch (e) {
      console.error('Download failed', e);
    }
  };

  const handleDeleteConfirm = async () => {
    if (deletingId == null) return;
    setDeleteBusy(true);
    try {
      await removeDoc(deletingId);
      setDeletingId(null);
      void load();
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Delete failed');
    } finally {
      setDeleteBusy(false);
    }
  };

  return (
    <div className="space-y-4">
      <Card>
        <CardHeader>
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-3">
              <Icon className="h-6 w-6 text-primary" />
              <div>
                <CardTitle>My {label}s</CardTitle>
                <CardDescription>
                  {docs.length} {labelLower}{docs.length === 1 ? '' : 's'} created
                </CardDescription>
              </div>
            </div>
            <Button onClick={onCreateNew}>
              <Plus className="h-4 w-4 mr-1" />
              New {label}
            </Button>
          </div>
        </CardHeader>
        <CardContent>
          <div className="relative">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
            <Input
              placeholder={`Search ${labelLower}s…`}
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="pl-9"
            />
          </div>
        </CardContent>
      </Card>

      {error && (
        <Card className="border-destructive">
          <CardContent className="py-4">
            <div className="flex items-center gap-2 text-destructive">
              <AlertCircle className="h-4 w-4" />
              <span className="text-sm">{error}</span>
            </div>
          </CardContent>
        </Card>
      )}

      {isLoading ? (
        <Card>
          <CardContent className="py-12 text-center">
            <Loader2 className="h-8 w-8 animate-spin mx-auto text-primary mb-2" />
            <p className="text-sm text-muted-foreground">Loading {labelLower}s…</p>
          </CardContent>
        </Card>
      ) : filtered.length === 0 ? (
        <Card>
          <CardContent className="py-12 text-center">
            <Icon className="h-12 w-12 text-muted-foreground/40 mx-auto mb-3" />
            <p className="text-sm text-muted-foreground mb-4">
              {searchQuery
                ? `No ${labelLower}s match your search.`
                : `You haven't created any ${labelLower}s yet.`}
            </p>
            {!searchQuery && (
              <Button onClick={onCreateNew}>
                <Plus className="h-4 w-4 mr-1" />
                Create your first {labelLower}
              </Button>
            )}
          </CardContent>
        </Card>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-3">
          {filtered.map((doc, i) => (
            <Card key={doc.id} className="flex flex-col">
              <CardHeader className="pb-3">
                {/* Title gets its own row with line-clamp-2 + overflow-hidden
                    so long catalog names wrap to two lines with an ellipsis
                    instead of overflowing the card. Badges sit on a second
                    row beneath, never competing with the title for space. */}
                <CardTitle className="text-base line-clamp-2 break-words overflow-hidden" title={doc.title}>
                  {doc.title}
                </CardTitle>
                <div className="flex items-center gap-1 mt-1">
                  {doc.draft && (
                    <Badge variant="secondary" className="text-xs">
                      Draft
                    </Badge>
                  )}
                  <Badge variant="outline" className="text-xs">
                    v{doc.version || '—'}
                  </Badge>
                </div>
                <CardDescription className="text-xs line-clamp-2">
                  {doc.description || <span className="italic">No description</span>}
                </CardDescription>
              </CardHeader>
              <CardContent className="flex-1 pb-3 space-y-2">
                <div className="flex flex-wrap items-center gap-1">
                  {doc.primary?.map((p) => (
                    <Badge key={p.label} variant="secondary" className="text-xs font-mono">
                      {p.value} {p.label}
                    </Badge>
                  ))}
                  <Badge variant="outline" className="text-xs">
                    OSCAL {doc.oscalVersion}
                  </Badge>
                </div>
                <div className="flex items-center gap-1.5 text-xs text-muted-foreground">
                  <Calendar className="h-3 w-3" />
                  <span>Updated {new Date(doc.updatedAt).toLocaleDateString()}</span>
                </div>
              </CardContent>
              <div className="flex items-center justify-end gap-1 px-3 py-2 border-t bg-muted/20">
                <Button
                  size="sm"
                  variant="ghost"
                  onClick={() => handleView(doc)}
                  title="View JSON"
                >
                  <Eye className="h-3.5 w-3.5" />
                </Button>
                <Button
                  size="sm"
                  variant="ghost"
                  onClick={() => handleDownload(doc)}
                  title="Download"
                >
                  <Download className="h-3.5 w-3.5" />
                </Button>
                {onEdit && (
                  <Button
                    size="sm"
                    variant="ghost"
                    onClick={() => onEdit(rawDocs[i])}
                    title="Edit"
                  >
                    <Edit2 className="h-3.5 w-3.5" />
                  </Button>
                )}
                <Button
                  size="sm"
                  variant="ghost"
                  onClick={() => setDeletingId(doc.id)}
                  title="Delete"
                  className="text-destructive hover:text-destructive"
                >
                  <Trash2 className="h-3.5 w-3.5" />
                </Button>
              </div>
            </Card>
          ))}
        </div>
      )}

      <Dialog open={viewing != null} onOpenChange={(o) => !o && setViewing(null)}>
        <DialogContent className="max-w-4xl max-h-[80vh] overflow-hidden flex flex-col">
          <DialogHeader>
            <DialogTitle className="flex items-center gap-2">
              <FileJson className="h-4 w-4" />
              {viewing?.title}
            </DialogTitle>
          </DialogHeader>
          {viewLoading ? (
            <div className="flex-1 flex items-center justify-center py-12">
              <Loader2 className="h-6 w-6 animate-spin text-primary" />
            </div>
          ) : (
            <pre className="flex-1 overflow-auto text-xs bg-muted/50 rounded p-3 font-mono">
              <code>{viewJson}</code>
            </pre>
          )}
        </DialogContent>
      </Dialog>

      <AlertDialog open={deletingId != null} onOpenChange={(o) => !o && setDeletingId(null)}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>Delete this {labelLower}?</AlertDialogTitle>
            <AlertDialogDescription>
              This action cannot be undone. The OSCAL JSON file will be permanently removed from
              storage.
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel disabled={deleteBusy}>Cancel</AlertDialogCancel>
            <AlertDialogAction
              onClick={(e) => {
                e.preventDefault();
                void handleDeleteConfirm();
              }}
              disabled={deleteBusy}
              className="bg-destructive text-destructive-foreground hover:bg-destructive/90"
            >
              {deleteBusy ? 'Deleting…' : 'Delete'}
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </div>
  );
}
