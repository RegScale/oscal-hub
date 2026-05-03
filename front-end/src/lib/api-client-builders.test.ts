import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { catalogBuilderApi, profileBuilderApi, oscalDocumentApi } from './api-client';

const fetchMock = vi.fn();

const localStorageMock = (() => {
  let store: Record<string, string> = { token: 'test-token' };
  return {
    getItem: (k: string) => store[k] ?? null,
    setItem: (k: string, v: string) => { store[k] = v; },
    removeItem: (k: string) => { delete store[k]; },
    clear: () => { store = {}; },
    setStore: (next: Record<string, string>) => { store = next; },
  };
})();
Object.defineProperty(window, 'localStorage', { value: localStorageMock, writable: true });

function jsonResponse<T>(body: T, status = 200): Response {
  return {
    ok: status >= 200 && status < 300,
    status,
    statusText: status === 200 ? 'OK' : 'Error',
    json: async () => body,
  } as unknown as Response;
}

beforeEach(() => {
  fetchMock.mockReset();
  globalThis.fetch = fetchMock as unknown as typeof fetch;
  localStorageMock.setStore({ token: 'test-token' });
});

afterEach(() => {
  vi.useRealTimers();
});

function urlOf(call: number): string {
  return String(fetchMock.mock.calls[call][0]);
}

function initOf(call: number): RequestInit {
  return fetchMock.mock.calls[call][1] as RequestInit;
}

describe('catalogBuilderApi', () => {
  it('create POSTs to /build/catalogs with the bearer token', async () => {
    fetchMock.mockResolvedValue(jsonResponse({ id: 1, title: 'C' }, 201));
    const out = await catalogBuilderApi.create({
      title: 'C',
      oscalVersion: '1.1.3',
      filename: 'c.json',
      jsonContent: '{}',
    });
    expect(out).toMatchObject({ id: 1 });
    expect(urlOf(0)).toContain('/build/catalogs');
    expect(initOf(0).method).toBe('POST');
    expect((initOf(0).headers as Record<string, string>).Authorization).toBe('Bearer test-token');
  });

  it('update PUTs to /build/catalogs/{id}', async () => {
    fetchMock.mockResolvedValue(jsonResponse({ id: 1 }));
    await catalogBuilderApi.update(42, { title: 'New' });
    expect(urlOf(0)).toMatch(/\/build\/catalogs\/42$/);
    expect(initOf(0).method).toBe('PUT');
  });

  it('get GETs the resource by id', async () => {
    fetchMock.mockResolvedValue(jsonResponse({ id: 7, title: 'X' }));
    const r = await catalogBuilderApi.get(7);
    expect(r.id).toBe(7);
    expect(urlOf(0)).toMatch(/\/build\/catalogs\/7$/);
    expect(initOf(0).method).toBe('GET');
  });

  it('getContent unwraps {content: string}', async () => {
    fetchMock.mockResolvedValue(jsonResponse({ content: '{"catalog":{}}' }));
    const r = await catalogBuilderApi.getContent(3);
    expect(r).toBe('{"catalog":{}}');
    expect(urlOf(0)).toMatch(/\/build\/catalogs\/3\/content$/);
  });

  it('list returns the array', async () => {
    fetchMock.mockResolvedValue(jsonResponse([{ id: 1 }, { id: 2 }]));
    const r = await catalogBuilderApi.list();
    expect(r).toHaveLength(2);
  });

  it('search URL-encodes the query parameter', async () => {
    fetchMock.mockResolvedValue(jsonResponse([]));
    await catalogBuilderApi.search('AC&AU');
    expect(urlOf(0)).toContain('q=AC%26AU');
  });

  it('search omits the query string when no term is supplied', async () => {
    fetchMock.mockResolvedValue(jsonResponse([]));
    await catalogBuilderApi.search();
    expect(urlOf(0)).toMatch(/\/build\/catalogs\/search$/);
  });

  it('remove DELETEs the resource', async () => {
    fetchMock.mockResolvedValue(jsonResponse(null, 204));
    await catalogBuilderApi.remove(9);
    expect(initOf(0).method).toBe('DELETE');
    expect(urlOf(0)).toMatch(/\/build\/catalogs\/9$/);
  });

  it('throws on non-2xx responses', async () => {
    fetchMock.mockResolvedValue(jsonResponse({}, 500));
    await expect(catalogBuilderApi.get(1)).rejects.toThrow(/500/);
  });

  it('clears the token from localStorage on a 401 response', async () => {
    fetchMock.mockResolvedValue(jsonResponse({}, 401));
    await expect(catalogBuilderApi.get(1)).rejects.toThrow();
    expect(localStorage.getItem('token')).toBeNull();
  });
});

describe('profileBuilderApi', () => {
  it('create POSTs to /build/profiles', async () => {
    fetchMock.mockResolvedValue(jsonResponse({ id: 1 }, 201));
    await profileBuilderApi.create({
      title: 'P',
      oscalVersion: '1.1.3',
      filename: 'p.json',
      jsonContent: '{}',
    });
    expect(urlOf(0)).toContain('/build/profiles');
    expect(initOf(0).method).toBe('POST');
  });

  it('list hits /build/profiles', async () => {
    fetchMock.mockResolvedValue(jsonResponse([]));
    await profileBuilderApi.list();
    expect(urlOf(0)).toMatch(/\/build\/profiles$/);
  });

  it('getContent hits /build/profiles/{id}/content', async () => {
    fetchMock.mockResolvedValue(jsonResponse({ content: '{}' }));
    await profileBuilderApi.getContent(5);
    expect(urlOf(0)).toMatch(/\/build\/profiles\/5\/content$/);
  });
});

describe('oscalDocumentApi', () => {
  it('create POSTs to /build/oscal-documents', async () => {
    fetchMock.mockResolvedValue(jsonResponse({ id: 1 }, 201));
    await oscalDocumentApi.create({
      modelType: 'system-security-plan',
      title: 'S',
      oscalVersion: '1.1.3',
      filename: 's.json',
      jsonContent: '{}',
    });
    expect(urlOf(0)).toContain('/build/oscal-documents');
    expect(initOf(0).method).toBe('POST');
  });

  it('list passes modelType as a query parameter', async () => {
    fetchMock.mockResolvedValue(jsonResponse([]));
    await oscalDocumentApi.list('plan-of-action-and-milestones');
    expect(urlOf(0)).toContain('modelType=plan-of-action-and-milestones');
  });

  it('search includes both modelType and q', async () => {
    fetchMock.mockResolvedValue(jsonResponse([]));
    await oscalDocumentApi.search('assessment-results', 'Q3 audit');
    expect(urlOf(0)).toContain('modelType=assessment-results');
    expect(urlOf(0)).toMatch(/q=Q3\+audit|q=Q3%20audit/);
  });

  it('search without a query still passes modelType', async () => {
    fetchMock.mockResolvedValue(jsonResponse([]));
    await oscalDocumentApi.search('assessment-plan');
    expect(urlOf(0)).toContain('modelType=assessment-plan');
    expect(urlOf(0)).not.toContain('q=');
  });

  it('getContent unwraps {content}', async () => {
    fetchMock.mockResolvedValue(jsonResponse({ content: '{"x":1}' }));
    const r = await oscalDocumentApi.getContent(11);
    expect(r).toBe('{"x":1}');
  });

  it('remove DELETEs the resource', async () => {
    fetchMock.mockResolvedValue(jsonResponse(null, 204));
    await oscalDocumentApi.remove(2);
    expect(initOf(0).method).toBe('DELETE');
    expect(urlOf(0)).toMatch(/\/build\/oscal-documents\/2$/);
  });
});
