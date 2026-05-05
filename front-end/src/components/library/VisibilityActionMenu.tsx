'use client';

import { useState } from 'react';
import { libraryPublishApi, type Visibility } from '@/lib/api/library';

interface VisibilityActionMenuProps {
  itemId: string;
  currentVisibility: Visibility;
  /** True when the caller created the item (only the creator can move from PRIVATE → ORGANIZATION). */
  isCreator: boolean;
  /** True when the caller has SUPER_ADMIN role (can force-unpublish public items). */
  isSuperAdmin: boolean;
  /** Callback fired after a successful visibility change so the parent can refresh. */
  onChanged: () => void;
}

/**
 * Inline button cluster that lets a creator (or super-admin) change a
 * library item's visibility. Renders nothing if the caller has no rights
 * to act on the item.
 *
 * Buttons surface only the *valid* transitions from the current state:
 *   - "Make Private"     — visible when not already PRIVATE
 *   - "Share with Org"   — visible when not already ORGANIZATION (creator only)
 *   - "Publish"          — visible when not already PUBLIC
 *   - "Force unpublish"  — only for super-admins acting on someone else's PUBLIC item;
 *                         requires a non-empty reason.
 */
export function VisibilityActionMenu({
  itemId,
  currentVisibility,
  isCreator,
  isSuperAdmin,
  onChanged,
}: VisibilityActionMenuProps) {
  const [pending, setPending] = useState(false);

  if (!isCreator && !isSuperAdmin) return null;

  const change = async (next: Visibility, reason?: string) => {
    setPending(true);
    try {
      await libraryPublishApi.changeVisibility(itemId, { visibility: next, reason });
      onChanged();
    } finally {
      setPending(false);
    }
  };

  return (
    <div className="inline-flex gap-1">
      {currentVisibility !== 'PRIVATE' && (
        <button
          type="button"
          disabled={pending}
          onClick={() => change('PRIVATE')}
          className="text-xs px-2 py-0.5 border rounded disabled:opacity-50"
        >
          Make Private
        </button>
      )}
      {currentVisibility !== 'ORGANIZATION' && isCreator && (
        <button
          type="button"
          disabled={pending}
          onClick={() => change('ORGANIZATION')}
          className="text-xs px-2 py-0.5 border rounded disabled:opacity-50"
        >
          Share with Org
        </button>
      )}
      {currentVisibility !== 'PUBLIC' && (
        <button
          type="button"
          disabled={pending}
          onClick={() => change('PUBLIC')}
          className="text-xs px-2 py-0.5 rounded bg-green-600 text-white hover:bg-green-700 disabled:opacity-50"
        >
          Publish
        </button>
      )}
      {currentVisibility === 'PUBLIC' && isSuperAdmin && !isCreator && (
        <button
          type="button"
          disabled={pending}
          onClick={() => {
            const reason = window.prompt('Reason for force-unpublish (required):');
            if (reason && reason.trim()) {
              void change('PRIVATE', reason.trim());
            }
          }}
          className="text-xs px-2 py-0.5 rounded bg-red-600 text-white hover:bg-red-700 disabled:opacity-50"
        >
          Force unpublish
        </button>
      )}
    </div>
  );
}
