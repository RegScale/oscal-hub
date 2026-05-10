import type { Visibility } from '@/lib/api/library';

// Backgrounds chosen to read clearly on the dark theme as well as light:
// solid mid-tone fills (no -50 / -100 light tints that wash out to near-white).
const styles: Record<Visibility, string> = {
  PRIVATE: 'bg-red-600 text-white',
  ORGANIZATION: 'bg-blue-100 text-blue-800',
  PUBLIC: 'bg-green-600 text-white',
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
 *   PRIVATE      — red   (only the owner)
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
