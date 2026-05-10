import { describe, it, expect } from 'vitest';
import {
  emptyOscalDocument,
  modelImportKey,
  modelLabel,
  modelRootKey,
  summarizeOscalDocument,
} from './oscal-models';

describe('OSCAL generic-model helpers', () => {
  it('exposes a label per slug', () => {
    expect(modelLabel('system-security-plan')).toMatch(/system security plan/i);
    expect(modelLabel('assessment-plan')).toMatch(/assessment plan/i);
    expect(modelLabel('assessment-results')).toMatch(/assessment results/i);
    expect(modelLabel('plan-of-action-and-milestones')).toMatch(/plan of action/i);
  });

  it('exposes the OSCAL root key per slug', () => {
    expect(modelRootKey('system-security-plan')).toBe('system-security-plan');
    expect(modelRootKey('plan-of-action-and-milestones')).toBe('plan-of-action-and-milestones');
  });

  it('exposes the import key per model', () => {
    expect(modelImportKey('system-security-plan')).toBe('import-profile');
    expect(modelImportKey('assessment-plan')).toBe('import-ssp');
    expect(modelImportKey('assessment-results')).toBe('import-ap');
    expect(modelImportKey('plan-of-action-and-milestones')).toBe('import-ssp');
  });

  it('emptyOscalDocument returns a wrapped skeleton with the right root key for each model', () => {
    for (const slug of [
      'system-security-plan',
      'assessment-plan',
      'assessment-results',
      'plan-of-action-and-milestones',
    ] as const) {
      const skeleton = emptyOscalDocument(slug, 'Hello');
      expect(skeleton).toHaveProperty(slug);
      const body = (skeleton as Record<string, Record<string, unknown>>)[slug];
      expect(body.uuid).toBeTruthy();
      expect(body.metadata).toBeDefined();
    }
  });

  it('SSP skeleton includes system-characteristics and control-implementation', () => {
    const s = emptyOscalDocument('system-security-plan') as Record<string, Record<string, unknown>>;
    expect(s['system-security-plan']['system-characteristics']).toBeDefined();
    expect(s['system-security-plan']['control-implementation']).toBeDefined();
    expect(s['system-security-plan']['system-implementation']).toBeDefined();
  });

  it('AR skeleton seeds an empty results array', () => {
    const s = emptyOscalDocument('assessment-results') as Record<string, Record<string, unknown>>;
    expect(Array.isArray(s['assessment-results'].results)).toBe(true);
  });

  it('POAM skeleton seeds empty observations/risks/findings/poam-items', () => {
    const s = emptyOscalDocument('plan-of-action-and-milestones') as Record<string, Record<string, unknown>>;
    const root = s['plan-of-action-and-milestones'];
    expect(Array.isArray(root.observations)).toBe(true);
    expect(Array.isArray(root.risks)).toBe(true);
    expect(Array.isArray(root.findings)).toBe(true);
    expect(Array.isArray(root['poam-items'])).toBe(true);
  });

  it('summarizeOscalDocument returns the right counts for each model', () => {
    const ssp = summarizeOscalDocument('system-security-plan', {
      'system-security-plan': {
        'system-implementation': { components: [{}, {}] },
        'control-implementation': { 'implemented-requirements': [{}, {}, {}] },
      },
    });
    expect(ssp.find((s) => s.label === 'components')?.value).toBe(2);
    expect(ssp.find((s) => s.label === 'requirements')?.value).toBe(3);

    const ap = summarizeOscalDocument('assessment-plan', { 'assessment-plan': { tasks: [{}, {}] } });
    expect(ap.find((s) => s.label === 'tasks')?.value).toBe(2);

    const ar = summarizeOscalDocument('assessment-results', {
      'assessment-results': { results: [{}] },
    });
    expect(ar.find((s) => s.label === 'results')?.value).toBe(1);

    const poam = summarizeOscalDocument('plan-of-action-and-milestones', {
      'plan-of-action-and-milestones': { 'poam-items': [{}, {}, {}], risks: [{}] },
    });
    expect(poam.find((s) => s.label === 'items')?.value).toBe(3);
    expect(poam.find((s) => s.label === 'risks')?.value).toBe(1);
  });

  it('summarizeOscalDocument handles missing/empty content gracefully', () => {
    expect(summarizeOscalDocument('system-security-plan', {})).toEqual([
      { label: 'components', value: 0 },
      { label: 'requirements', value: 0 },
    ]);
  });
});
