'use client';

import { useEffect, useState } from 'react';
import { Card } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { Table, TableBody, TableHead, TableHeader, TableRow } from '@/components/ui/table';
import { Plus, Loader2 } from 'lucide-react';
import { toast } from 'sonner';
import { apiClient } from '@/lib/api-client';
import { UploadDocumentDialog } from './documents/upload-document-dialog';
import { EditDocumentMetadataDialog } from './documents/edit-document-metadata-dialog';
import { DocumentRow } from './documents/document-row';
import { PackageCompletenessCard } from './documents/package-completeness-card';
import { ALL_DOCUMENT_TYPES, DOCUMENT_TYPE_LABELS } from './documents/document-type-labels';
import type {
  AuthorizationResponse,
  AuthorizationDocumentResponse,
  PackageCompletenessResponse,
  DocumentType,
} from '@/types/oscal';

interface Props {
  authorization: AuthorizationResponse;
}

export function DocumentsTab({ authorization }: Props) {
  const [documents, setDocuments] = useState<AuthorizationDocumentResponse[]>([]);
  const [completeness, setCompleteness] = useState<PackageCompletenessResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [completenessLoading, setCompletenessLoading] = useState(true);
  const [uploadOpen, setUploadOpen] = useState(false);
  const [editing, setEditing] = useState<AuthorizationDocumentResponse | null>(null);
  const [typeFilter, setTypeFilter] = useState<DocumentType | 'ALL'>('ALL');
  const [searchQuery, setSearchQuery] = useState('');

  const role = authorization.effectiveRole;
  const canUpload = role === 'OWNER' || role === 'EDITOR' || role === 'CONTRIBUTOR';
  const canEditAny = role === 'OWNER' || role === 'EDITOR' || role === 'CONTRIBUTOR';

  const refresh = async () => {
    setLoading(true);
    try {
      const data = await apiClient.listDocuments(authorization.id, {
        type: typeFilter === 'ALL' ? undefined : typeFilter,
        q: searchQuery || undefined,
      });
      setDocuments(data);
    } catch (e) {
      toast.error('Failed to load documents');
    } finally {
      setLoading(false);
    }
  };

  const refreshCompleteness = async () => {
    setCompletenessLoading(true);
    try {
      const data = await apiClient.getPackageCompleteness(authorization.id);
      setCompleteness(data);
    } catch (e) {
      // non-fatal
    } finally {
      setCompletenessLoading(false);
    }
  };

  useEffect(() => {
    void refresh();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [authorization.id, typeFilter, searchQuery]);

  useEffect(() => {
    void refreshCompleteness();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [authorization.id, documents.length]);

  const handleDownload = async (doc: AuthorizationDocumentResponse) => {
    try {
      const blob = await apiClient.downloadDocument(authorization.id, doc.id);
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = doc.originalFilename;
      a.click();
      URL.revokeObjectURL(url);
    } catch (e) {
      toast.error('Download failed');
    }
  };

  const handleDelete = async (doc: AuthorizationDocumentResponse) => {
    if (!confirm(`Delete "${doc.originalFilename}"? This cannot be undone.`)) return;
    try {
      await apiClient.deleteDocument(authorization.id, doc.id);
      toast.success('Document deleted');
      await refresh();
    } catch (e) {
      toast.error('Delete failed');
    }
  };

  // CONTRIBUTOR can only delete their own uploads.
  const canDelete = (doc: AuthorizationDocumentResponse) => {
    if (role === 'OWNER' || role === 'EDITOR') return true;
    if (role === 'CONTRIBUTOR') return doc.uploadedByUsername === currentUsername();
    return false;
  };

  return (
    <div className="grid grid-cols-1 gap-6 lg:grid-cols-[1fr_280px]">
      <div className="space-y-4">
        <Card className="p-4">
          <div className="mb-3 flex items-center justify-between gap-2">
            <h2 className="text-lg font-semibold">Documents</h2>
            {canUpload && (
              <Button onClick={() => setUploadOpen(true)}>
                <Plus className="mr-1 h-4 w-4" />
                Upload document
              </Button>
            )}
          </div>

          <div className="mb-3 flex flex-wrap items-center gap-2">
            <Select value={typeFilter} onValueChange={(v) => setTypeFilter(v as DocumentType | 'ALL')}>
              <SelectTrigger className="w-56">
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="ALL">All document types</SelectItem>
                {ALL_DOCUMENT_TYPES.map((t) => (
                  <SelectItem key={t} value={t}>{DOCUMENT_TYPE_LABELS[t]}</SelectItem>
                ))}
              </SelectContent>
            </Select>
            <Input
              className="max-w-xs"
              placeholder="Search filename, description, tags…"
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
            />
          </div>

          {loading ? (
            <div className="flex items-center justify-center py-12 text-sm text-muted-foreground">
              <Loader2 className="mr-2 h-4 w-4 animate-spin" />
              Loading…
            </div>
          ) : documents.length === 0 ? (
            <p className="py-8 text-center text-sm text-muted-foreground">
              No documents yet. {canUpload && 'Click "Upload document" to add one.'}
            </p>
          ) : (
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Type</TableHead>
                  <TableHead>Filename</TableHead>
                  <TableHead>Description</TableHead>
                  <TableHead>Version</TableHead>
                  <TableHead>Uploaded by</TableHead>
                  <TableHead>Uploaded</TableHead>
                  <TableHead>Expires</TableHead>
                  <TableHead className="text-right">Size</TableHead>
                  <TableHead className="text-right">Actions</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {documents.map((d) => (
                  <DocumentRow
                    key={d.id}
                    doc={d}
                    canEdit={canEditAny}
                    canDelete={canDelete(d)}
                    onDownload={() => void handleDownload(d)}
                    onEdit={() => setEditing(d)}
                    onDelete={() => void handleDelete(d)}
                  />
                ))}
              </TableBody>
            </Table>
          )}
        </Card>
      </div>

      <div>
        <PackageCompletenessCard completeness={completeness} loading={completenessLoading} />
      </div>

      <UploadDocumentDialog
        authorizationId={authorization.id}
        open={uploadOpen}
        onOpenChange={setUploadOpen}
        onUploaded={refresh}
      />

      <EditDocumentMetadataDialog
        authorizationId={authorization.id}
        document={editing}
        open={editing !== null}
        onOpenChange={(v) => { if (!v) setEditing(null); }}
        onUpdated={refresh}
      />
    </div>
  );
}

/**
 * Returns the current user's username from the user object stored in
 * localStorage by AuthContext. Used to gate "delete own uploads" for
 * CONTRIBUTORS in the absence of an effectiveRole-per-document field.
 */
function currentUsername(): string | null {
  if (typeof localStorage === 'undefined') return null;
  try {
    const raw = localStorage.getItem('user');
    if (!raw) return null;
    const parsed = JSON.parse(raw);
    return parsed.username ?? null;
  } catch {
    return null;
  }
}
