import { describe, it, expect } from 'vitest';
import {
  CURRENT_OSCAL_VERSION,
  generateUuid,
  emptyMetadata,
  emptyCatalog,
  emptyProfile,
  countCatalogControls,
  countCatalogGroups,
  countCatalogParams,
  countProfileControls,
  parseCatalog,
  parseProfile,
} from './oscal-models';

describe('generateUuid', () => {
  it('produces an RFC 4122-shaped UUID', () => {
    const uuid = generateUuid();
    expect(uuid).toMatch(/^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i);
  });

  it('produces unique values across calls', () => {
    const a = generateUuid();
    const b = generateUuid();
    expect(a).not.toBe(b);
  });
});

describe('emptyMetadata / emptyCatalog / emptyProfile', () => {
  it('emptyMetadata fills required fields with current OSCAL version', () => {
    const m = emptyMetadata('Hello');
    expect(m.title).toBe('Hello');
    expect(m['oscal-version']).toBe(CURRENT_OSCAL_VERSION);
    expect(m.version).toBe('1.0.0');
    expect(m['last-modified']).toMatch(/T/);
  });

  it('emptyCatalog returns a valid skeleton', () => {
    const c = emptyCatalog('My Catalog');
    expect(c.uuid).toBeTruthy();
    expect(c.metadata.title).toBe('My Catalog');
    expect(c.controls).toBeUndefined();
    expect(c.groups).toBeUndefined();
  });

  it('emptyProfile starts with empty imports array', () => {
    const p = emptyProfile();
    expect(p.imports).toEqual([]);
    expect(p.uuid).toBeTruthy();
  });
});

describe('count helpers', () => {
  const sampleCatalog = {
    uuid: 'u',
    metadata: emptyMetadata('t'),
    params: [{ id: 'top1' }, { id: 'top2' }],
    controls: [
      { id: 'tc-1', title: 'top control', params: [{ id: 'p1' }] },
    ],
    groups: [
      {
        title: 'g1',
        params: [{ id: 'g1p' }],
        controls: [
          { id: 'g1-1', title: 'c1', params: [{ id: 'g1-1-p' }] },
          {
            id: 'g1-2',
            title: 'c2',
            controls: [{ id: 'g1-2.1', title: 'enh' }],
          },
        ],
        groups: [
          { title: 'g1.1', controls: [{ id: 'g1.1-1', title: 'c' }] },
        ],
      },
    ],
  };

  it('counts catalog controls including enhancements and nested groups', () => {
    expect(countCatalogControls(sampleCatalog)).toBe(5);
  });

  it('counts catalog groups including nested', () => {
    expect(countCatalogGroups(sampleCatalog)).toBe(2);
  });

  it('counts catalog params across catalog/group/control', () => {
    expect(countCatalogParams(sampleCatalog)).toBe(5);
  });

  it('counts profile controls from with-ids', () => {
    const p = {
      uuid: 'u',
      metadata: emptyMetadata('t'),
      imports: [
        {
          href: '#x',
          'include-controls': [
            { 'with-ids': ['ac-1', 'ac-2'] },
            { 'with-ids': ['ac-3'] },
          ],
        },
        { href: '#y', 'include-all': {} },
      ],
    };
    expect(countProfileControls(p)).toBe(3);
  });
});

describe('parseCatalog', () => {
  it('accepts wrapped { catalog: {...} } documents', () => {
    const result = parseCatalog({
      catalog: {
        uuid: 'abc',
        metadata: { title: 'T', 'oscal-version': '1.1.3', version: '1.0.0', 'last-modified': '2026-01-01T00:00:00Z' },
      },
    });
    expect(result.uuid).toBe('abc');
    expect(result.metadata.title).toBe('T');
  });

  it('accepts bare catalog objects', () => {
    const result = parseCatalog({
      uuid: 'abc',
      metadata: { title: 'T', 'oscal-version': '1.1.3', version: '1.0.0', 'last-modified': '2026-01-01T00:00:00Z' },
    });
    expect(result.uuid).toBe('abc');
  });

  it('throws when uuid is missing', () => {
    expect(() =>
      parseCatalog({ catalog: { metadata: { title: 't', 'oscal-version': '1.1.3' } } }),
    ).toThrow(/uuid/);
  });

  it('throws when metadata.title is missing', () => {
    expect(() => parseCatalog({ catalog: { uuid: 'x', metadata: { 'oscal-version': '1.1.3' } } })).toThrow(/title/);
  });

  it('throws when metadata.oscal-version is missing', () => {
    expect(() => parseCatalog({ catalog: { uuid: 'x', metadata: { title: 't' } } })).toThrow(/oscal-version/);
  });

  it('throws when input is not an object', () => {
    expect(() => parseCatalog(null)).toThrow();
    expect(() => parseCatalog([])).toThrow();
    expect(() => parseCatalog(7)).toThrow();
  });

  it('preserves controls and groups arrays', () => {
    const result = parseCatalog({
      catalog: {
        uuid: 'u',
        metadata: { title: 't', 'oscal-version': '1.1.3' },
        controls: [{ id: 'c1', title: 'c' }],
        groups: [{ title: 'g' }],
      },
    });
    expect(result.controls).toHaveLength(1);
    expect(result.groups).toHaveLength(1);
  });
});

describe('parseProfile', () => {
  const validRoot = {
    uuid: 'p',
    metadata: { title: 'T', 'oscal-version': '1.1.3', version: '1.0.0', 'last-modified': '2026-01-01T00:00:00Z' },
    imports: [{ href: '#a' }],
  };

  it('accepts wrapped profiles', () => {
    const result = parseProfile({ profile: validRoot });
    expect(result.uuid).toBe('p');
    expect(result.imports).toHaveLength(1);
  });

  it('accepts bare profile objects', () => {
    expect(parseProfile(validRoot).uuid).toBe('p');
  });

  it('throws when imports array is empty', () => {
    expect(() => parseProfile({ profile: { ...validRoot, imports: [] } })).toThrow(/import/);
  });

  it('throws when imports field is missing', () => {
    const { imports: _imports, ...withoutImports } = validRoot;
    expect(() => parseProfile({ profile: withoutImports })).toThrow(/import/);
  });

  it('throws when metadata.title is missing', () => {
    expect(() =>
      parseProfile({ profile: { ...validRoot, metadata: { 'oscal-version': '1.1.3' } } }),
    ).toThrow(/title/);
  });
});
