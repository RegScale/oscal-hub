'use client';

import Link from 'next/link';
import { usePathname } from 'next/navigation';
import { useEffect, useState } from 'react';
import { ChevronRight } from 'lucide-react';
import { TOC } from '@/lib/guide-toc';
import { cn } from '@/lib/utils';

const STORAGE_KEY = 'guide.toc.expanded';

function readExpanded(): Record<string, boolean> {
  if (typeof window === 'undefined') return {};
  try { return JSON.parse(localStorage.getItem(STORAGE_KEY) ?? '{}'); }
  catch { return {}; }
}

export function DocSidebar({ onNavigate }: { onNavigate?: () => void }) {
  const pathname = usePathname();
  const activeSlug = pathname.replace(/^\/guide\//, '');

  const activeGroupLabels = TOC
    .filter((g) => g.entries.some((e) => e.slug === activeSlug))
    .map((g) => g.label);

  const [expanded, setExpanded] = useState<Record<string, boolean>>({});

  useEffect(() => {
    const stored = readExpanded();
    const initial: Record<string, boolean> = { ...stored };
    for (const label of activeGroupLabels) initial[label] = true;
    setExpanded(initial);
  }, [pathname]); // eslint-disable-line react-hooks/exhaustive-deps

  const toggle = (label: string) => {
    setExpanded((prev) => {
      const next = { ...prev, [label]: !prev[label] };
      try { localStorage.setItem(STORAGE_KEY, JSON.stringify(next)); } catch { /* ignore */ }
      return next;
    });
  };

  return (
    <nav aria-label="User guide table of contents" className="text-sm">
      <ul className="space-y-1">
        {TOC.map((group) => {
          const isOpen = expanded[group.label] ?? activeGroupLabels.includes(group.label);
          return (
            <li key={group.label}>
              <button
                type="button"
                onClick={() => toggle(group.label)}
                aria-expanded={isOpen}
                className="flex w-full items-center justify-between rounded-md px-2 py-1.5 font-semibold text-foreground hover:bg-muted focus:outline-none focus:ring-2 focus:ring-ring"
              >
                <span>{group.label}</span>
                <ChevronRight className={cn('h-4 w-4 transition-transform', isOpen && 'rotate-90')} aria-hidden="true" />
              </button>
              {isOpen && (
                <ul className="mt-1 ml-3 border-l border-border pl-2 space-y-0.5">
                  {group.entries.map((entry) => {
                    const isActive = entry.slug === activeSlug;
                    return (
                      <li key={`${group.label}-${entry.slug}`}>
                        <Link
                          href={`/guide/${entry.slug}`}
                          aria-current={isActive ? 'page' : undefined}
                          onClick={onNavigate}
                          className={cn(
                            'block rounded-md px-2 py-1 text-muted-foreground hover:bg-muted hover:text-foreground focus:outline-none focus:ring-2 focus:ring-ring',
                            isActive && 'bg-muted text-foreground font-medium',
                          )}
                        >
                          {entry.label}
                        </Link>
                      </li>
                    );
                  })}
                </ul>
              )}
            </li>
          );
        })}
      </ul>
    </nav>
  );
}
