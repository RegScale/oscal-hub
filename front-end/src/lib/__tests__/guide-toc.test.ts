import { describe, it, expect } from 'vitest';
import { TOC } from '../guide-toc';

describe('guide-toc', () => {
  it('has at least 10 groups', () => {
    expect(TOC.length).toBeGreaterThanOrEqual(10);
  });

  it('every entry has a non-empty label and slug', () => {
    for (const group of TOC) {
      expect(group.label).toBeTruthy();
      expect(group.entries.length).toBeGreaterThan(0);
      for (const entry of group.entries) {
        expect(entry.label).toBeTruthy();
        expect(entry.slug).toBeTruthy();
        expect(entry.slug).not.toMatch(/^\//); // no leading slash
      }
    }
  });

  it('every slug uses lowercase, hyphens, and forward slashes only', () => {
    const valid = /^[a-z0-9-]+(\/[a-z0-9-]+)*$/;
    for (const group of TOC) {
      for (const entry of group.entries) {
        expect(entry.slug, `slug "${entry.slug}" is invalid`).toMatch(valid);
      }
    }
  });
});
