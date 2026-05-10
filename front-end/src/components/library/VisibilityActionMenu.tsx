'use client';

import { useState } from 'react';
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
import { Textarea } from '@/components/ui/textarea';
import { toast } from 'sonner';
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

interface PendingChange {
  next: Visibility;
  /** True when triggered by an admin acting on someone else's PUBLIC item. */
  isForceUnpublish: boolean;
}

const visibilityCopy: Record<Visibility, { label: string; effect: string }> = {
  PRIVATE: {
    label: 'Make Private',
    effect:
      'The item will only be visible to you. Anyone who could see it as Organization or Public will lose access.',
  },
  ORGANIZATION: {
    label: 'Share with Org',
    effect:
      'Members of your organization will be able to view and download this item. It will not appear in OSCAL Data Products.',
  },
  PUBLIC: {
    label: 'Publish',
    effect:
      'The item will appear in OSCAL Data Products at /catalog and be downloadable by anyone with an account. Existing ratings and comments will become visible.',
  },
};

/**
 * Inline button cluster that lets a creator (or super-admin) change a
 * library item's visibility. Renders nothing if the caller has no rights
 * to act on the item.
 *
 * Each button opens a confirmation dialog explaining what the change does
 * before any API call. Force-unpublish additionally requires the admin to
 * type a reason that's recorded in the audit trail.
 */
export function VisibilityActionMenu({
  itemId,
  currentVisibility,
  isCreator,
  isSuperAdmin,
  onChanged,
}: VisibilityActionMenuProps) {
  const [pending, setPending] = useState<PendingChange | null>(null);
  const [reason, setReason] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  if (!isCreator && !isSuperAdmin) return null;

  const open = (change: PendingChange) => {
    setPending(change);
    setReason('');
    setError(null);
  };

  const close = () => {
    if (submitting) return;
    setPending(null);
    setReason('');
    setError(null);
  };

  const confirm = async () => {
    if (!pending) return;
    if (pending.isForceUnpublish && reason.trim().length < 3) {
      setError('Please provide a reason (at least 3 characters).');
      return;
    }
    setSubmitting(true);
    setError(null);
    try {
      await libraryPublishApi.changeVisibility(itemId, {
        visibility: pending.next,
        reason: pending.isForceUnpublish ? reason.trim() : undefined,
      });
      showSuccessToast(pending.next, pending.isForceUnpublish);
      onChanged();
      setPending(null);
      setReason('');
    } catch (e) {
      const message = e instanceof Error ? e.message : 'Failed to change visibility';
      setError(message);
      toast.error('Visibility change failed', { description: message });
    } finally {
      setSubmitting(false);
    }
  };

  const showSuccessToast = (next: Visibility, isForceUnpublish: boolean) => {
    if (isForceUnpublish) {
      toast.success('Item force-unpublished', {
        description: 'The item is no longer visible in OSCAL Data Products. Reason recorded in the audit log.',
      });
      return;
    }
    if (next === 'PUBLIC') {
      toast.success('Item published', {
        description: 'Now visible to anyone in OSCAL Data Products.',
      });
    } else if (next === 'ORGANIZATION') {
      toast.success('Item shared with organization', {
        description: 'Members of your organization can now view and download it.',
      });
    } else {
      toast.success('Item is now private', {
        description: 'Only you can see this item.',
      });
    }
  };

  const dialog = pending && (
    <AlertDialog open onOpenChange={(o) => !o && close()}>
      <AlertDialogContent>
        <AlertDialogHeader>
          <AlertDialogTitle>
            {pending.isForceUnpublish
              ? 'Force-unpublish this item?'
              : `Change visibility to ${labelFor(pending.next)}?`}
          </AlertDialogTitle>
          <AlertDialogDescription>
            <span className="block mb-2">
              <strong>{labelFor(currentVisibility)}</strong> →{' '}
              <strong>{labelFor(pending.next)}</strong>
            </span>
            <span className="block">{visibilityCopy[pending.next].effect}</span>
          </AlertDialogDescription>
        </AlertDialogHeader>

        {pending.isForceUnpublish && (
          <div className="space-y-1">
            <label htmlFor="force-unpublish-reason" className="text-sm font-medium">
              Reason (recorded in the audit log)
            </label>
            <Textarea
              id="force-unpublish-reason"
              value={reason}
              onChange={(e) => setReason(e.target.value)}
              rows={3}
              placeholder="e.g. Contains incorrect control mappings"
              disabled={submitting}
            />
          </div>
        )}

        {error && <p className="text-sm text-destructive">{error}</p>}

        <AlertDialogFooter>
          <AlertDialogCancel disabled={submitting} onClick={close}>
            Cancel
          </AlertDialogCancel>
          <AlertDialogAction
            disabled={submitting}
            onClick={(e) => {
              e.preventDefault();
              void confirm();
            }}
            className={
              pending.next === 'PUBLIC'
                ? 'bg-green-600 hover:bg-green-700'
                : pending.isForceUnpublish || pending.next === 'PRIVATE'
                  ? 'bg-red-600 hover:bg-red-700'
                  : undefined
            }
          >
            {submitting
              ? 'Saving…'
              : pending.isForceUnpublish
                ? 'Force-unpublish'
                : `Yes, ${visibilityCopy[pending.next].label.toLowerCase()}`}
          </AlertDialogAction>
        </AlertDialogFooter>
      </AlertDialogContent>
    </AlertDialog>
  );

  return (
    <>
      <div className="inline-flex gap-1">
        {currentVisibility !== 'PRIVATE' && (
          <button
            type="button"
            onClick={() => open({ next: 'PRIVATE', isForceUnpublish: false })}
            className="text-xs px-2 py-0.5 border rounded"
          >
            Make Private
          </button>
        )}
        {currentVisibility !== 'ORGANIZATION' && isCreator && (
          <button
            type="button"
            onClick={() => open({ next: 'ORGANIZATION', isForceUnpublish: false })}
            className="text-xs px-2 py-0.5 border rounded"
          >
            Share with Org
          </button>
        )}
        {currentVisibility !== 'PUBLIC' && (
          <button
            type="button"
            onClick={() => open({ next: 'PUBLIC', isForceUnpublish: false })}
            className="text-xs px-2 py-0.5 rounded bg-green-600 text-white hover:bg-green-700"
          >
            Publish
          </button>
        )}
        {currentVisibility === 'PUBLIC' && isSuperAdmin && !isCreator && (
          <button
            type="button"
            onClick={() => open({ next: 'PRIVATE', isForceUnpublish: true })}
            className="text-xs px-2 py-0.5 rounded bg-red-600 text-white hover:bg-red-700"
          >
            Force unpublish
          </button>
        )}
      </div>
      {dialog}
    </>
  );
}

function labelFor(v: Visibility): string {
  return v === 'PRIVATE' ? 'Private' : v === 'ORGANIZATION' ? 'Organization' : 'Public';
}
