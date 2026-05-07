'use client';

import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { TableCell, TableRow } from '@/components/ui/table';
import { Download, Pencil, Trash2 } from 'lucide-react';
import { DOCUMENT_TYPE_LABELS, formatFileSize } from './document-type-labels';
import type { AuthorizationDocumentResponse } from '@/types/oscal';

interface Props {
  doc: AuthorizationDocumentResponse;
  canEdit: boolean;
  canDelete: boolean;
  onDownload: () => void;
  onEdit: () => void;
  onDelete: () => void;
}

export function DocumentRow({ doc, canEdit, canDelete, onDownload, onEdit, onDelete }: Props) {
  const expired = doc.expiresAt ? new Date(doc.expiresAt) < new Date() : false;

  return (
    <TableRow>
      <TableCell>
        <Badge variant="secondary">{DOCUMENT_TYPE_LABELS[doc.documentType]}</Badge>
      </TableCell>
      <TableCell className="font-medium">
        <button
          type="button"
          className="underline-offset-2 hover:underline"
          onClick={onDownload}
        >
          {doc.originalFilename}
        </button>
      </TableCell>
      <TableCell className="max-w-xs truncate text-sm text-muted-foreground">
        {doc.description}
      </TableCell>
      <TableCell className="text-sm">{doc.version ?? '—'}</TableCell>
      <TableCell className="text-sm text-muted-foreground">{doc.uploadedByUsername ?? '—'}</TableCell>
      <TableCell className="text-sm">{new Date(doc.uploadedAt).toLocaleDateString()}</TableCell>
      <TableCell className="text-sm">
        {doc.expiresAt
          ? <span className={expired ? 'text-destructive' : undefined}>{doc.expiresAt}{expired ? ' (Expired)' : ''}</span>
          : '—'}
      </TableCell>
      <TableCell className="text-right text-xs text-muted-foreground">{formatFileSize(doc.fileSize)}</TableCell>
      <TableCell className="text-right">
        <div className="flex justify-end gap-1">
          <Button variant="ghost" size="icon" onClick={onDownload} aria-label="Download">
            <Download className="h-4 w-4" />
          </Button>
          {canEdit && (
            <Button variant="ghost" size="icon" onClick={onEdit} aria-label="Edit metadata">
              <Pencil className="h-4 w-4" />
            </Button>
          )}
          {canDelete && (
            <Button variant="ghost" size="icon" onClick={onDelete} aria-label={`Delete ${doc.originalFilename}`}>
              <Trash2 className="h-4 w-4" />
            </Button>
          )}
        </div>
      </TableCell>
    </TableRow>
  );
}
