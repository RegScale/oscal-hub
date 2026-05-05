'use client';

import Link from 'next/link';
import { useState, ReactNode } from 'react';
import { Menu, X, BookOpen } from 'lucide-react';
import { DocSidebar } from '@/components/guide/DocSidebar';
import { cn } from '@/lib/utils';

export default function GuideLayout({ children }: { children: ReactNode }) {
  const [mobileOpen, setMobileOpen] = useState(false);
  return (
    <div className="container mx-auto px-4 py-6 max-w-7xl">
      <div className="lg:grid lg:grid-cols-[260px_minmax(0,1fr)] lg:gap-10">
        {/* Mobile toggle */}
        <button
          type="button"
          onClick={() => setMobileOpen(true)}
          className="lg:hidden mb-4 inline-flex items-center gap-2 rounded-md border border-border px-3 py-2 text-sm hover:bg-muted focus:outline-none focus:ring-2 focus:ring-ring"
          aria-label="Open table of contents"
        >
          <Menu className="h-4 w-4" aria-hidden="true" />
          Contents
        </button>

        {/* Desktop sidebar */}
        <aside className="hidden lg:block sticky top-4 self-start max-h-[calc(100vh-2rem)] overflow-y-auto pr-2">
          <Link href="/guide" className="mb-4 flex items-center gap-2 text-base font-semibold text-foreground hover:text-primary">
            <BookOpen className="h-4 w-4" aria-hidden="true" />
            User Guide
          </Link>
          <DocSidebar />
        </aside>

        {/* Mobile drawer */}
        {mobileOpen && (
          <div className="fixed inset-0 z-50 lg:hidden" role="dialog" aria-modal="true" aria-label="Table of contents">
            <div className="absolute inset-0 bg-black/60" onClick={() => setMobileOpen(false)} />
            <div className="absolute left-0 top-0 h-full w-72 max-w-[85vw] bg-background border-r border-border p-4 overflow-y-auto">
              <div className="mb-4 flex items-center justify-between">
                <span className="font-semibold">Contents</span>
                <button type="button" onClick={() => setMobileOpen(false)} aria-label="Close" className="rounded-md p-1 hover:bg-muted">
                  <X className="h-4 w-4" aria-hidden="true" />
                </button>
              </div>
              <DocSidebar onNavigate={() => setMobileOpen(false)} />
            </div>
          </div>
        )}

        <article className={cn('min-w-0 prose-guide')}>{children}</article>
      </div>
    </div>
  );
}
