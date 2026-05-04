import type { Visibility } from '@/lib/api/library';

const styles: Record<Visibility, string> = {
  PRIVATE: 'bg-slate-200 text-slate-700',
  ORGANIZATION: 'bg-blue-100 text-blue-800',
  PUBLIC: 'bg-green-100 text-green-800',
};

const labels: Record<Visibility, string> = {
  PRIVATE: 'Private',
  ORGANIZATION: 'Organization',
  PUBLIC: 'Public',
};

interface VisibilityBadgeProps {
  visibility: Visibility;
}

/**
 * Compact pill that shows a library item's visibility scope.
 *
 * Color encodes the access level:
 *   PRIVATE      — slate (only the owner)
 *   ORGANIZATION — blue  (owner's org members)
 *   PUBLIC       — green (everyone, listed at /catalog)
 */
export function VisibilityBadge({ visibility }: VisibilityBadgeProps) {
  return (
    <span
      className={`inline-flex items-center px-2 py-0.5 rounded text-xs font-medium ${styles[visibility]}`}
    >
      {labels[visibility]}
    </span>
  );
}
