import { HelpCircle } from 'lucide-react';
import { HELP_TARGETS, type HelpSlug } from '@/lib/help-targets';

export function HelpButton({ slug, className }: { slug: HelpSlug; className?: string }) {
  return (
    <a
      href={`/guide/${HELP_TARGETS[slug]}`}
      target="_blank"
      rel="noopener noreferrer"
      aria-label="Open help for this page in a new tab"
      title="Open help for this page"
      className={`inline-flex h-8 w-8 items-center justify-center rounded-full text-muted-foreground hover:bg-muted hover:text-foreground transition-colors focus:outline-none focus:ring-2 focus:ring-ring ${className ?? ''}`}
    >
      <HelpCircle className="h-5 w-5" aria-hidden="true" />
    </a>
  );
}
