'use client';

import { Card } from '@/components/ui/card';
import { CheckCircle2, XCircle } from 'lucide-react';
import { DOCUMENT_TYPE_LABELS } from './document-type-labels';
import type { PackageCompletenessResponse } from '@/types/oscal';

interface Props {
  completeness: PackageCompletenessResponse | null;
  loading: boolean;
}

export function PackageCompletenessCard({ completeness, loading }: Props) {
  return (
    <Card className="p-4">
      <h3 className="mb-1 text-sm font-semibold">Package completeness</h3>
      <p className="mb-3 text-xs text-muted-foreground">
        Core documents typically required in an authorization package.
      </p>
      {loading ? (
        <p className="text-sm text-muted-foreground">Loading…</p>
      ) : !completeness ? (
        <p className="text-sm text-muted-foreground">Unavailable.</p>
      ) : (
        <ul className="space-y-1.5 text-sm">
          {completeness.coreDocuments.map((item) => (
            <li key={item.documentType} className="flex items-center gap-2">
              {item.satisfied
                ? <CheckCircle2 className="h-4 w-4 text-green-600" />
                : <XCircle className="h-4 w-4 text-muted-foreground" />}
              <span className={item.satisfied ? '' : 'text-muted-foreground'}>
                {DOCUMENT_TYPE_LABELS[item.documentType]}
              </span>
              {item.presentCount > 1 && (
                <span className="text-xs text-muted-foreground">×{item.presentCount}</span>
              )}
            </li>
          ))}
        </ul>
      )}
    </Card>
  );
}
