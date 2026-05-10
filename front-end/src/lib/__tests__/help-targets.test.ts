import { describe, it, expect } from 'vitest';
import { HELP_TARGETS } from '../help-targets';
import { TOC } from '../guide-toc';

describe('help-targets', () => {
  it('every help target points to a slug that exists in the TOC', () => {
    const tocSlugs = new Set(TOC.flatMap((g) => g.entries.map((e) => e.slug)));
    for (const [key, target] of Object.entries(HELP_TARGETS)) {
      expect(tocSlugs.has(target), `slug "${target}" (key="${key}") is not in the TOC`).toBe(true);
    }
  });

  it('has the expected core slugs', () => {
    expect(HELP_TARGETS.library).toBe('library/overview');
    expect(HELP_TARGETS.validate).toBe('tools/validate');
    expect(HELP_TARGETS.admin).toBe('admin/overview');
    expect(HELP_TARGETS['org-admin']).toBe('org-admin/overview');
  });
});
