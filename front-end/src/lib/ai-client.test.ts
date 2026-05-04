import { describe, it, expect, vi, beforeEach } from 'vitest';
import { aiClient } from './ai-client';

const fetchMock = vi.fn();
beforeEach(() => {
  fetchMock.mockReset();
  vi.stubGlobal('fetch', fetchMock);
  vi.stubGlobal('localStorage', {
    getItem: () => 'fake-token',
    setItem: vi.fn(),
    removeItem: vi.fn(),
  });
});

describe('aiClient.getSettingsStatus', () => {
  it('GETs /api/ai/settings/status with org id', async () => {
    fetchMock.mockResolvedValueOnce({
      ok: true,
      status: 200,
      json: async () => ({ enabled: true }),
    });
    const result = await aiClient.getSettingsStatus(7);
    expect(result.enabled).toBe(true);
    expect(fetchMock).toHaveBeenCalledWith(
      expect.stringContaining('/api/ai/settings/status?organizationId=7'),
      expect.objectContaining({ method: 'GET' }),
    );
  });
});

describe('aiClient.startSession', () => {
  it('POSTs to /api/ai/sessions', async () => {
    fetchMock.mockResolvedValueOnce({
      ok: true,
      status: 200,
      json: async () => ({ sessionId: 'abc-123' }),
    });
    const result = await aiClient.startSession({
      organizationId: 1,
      wizardKind: 'SMOKE',
      mode: 'STREAMING',
      input: 'ping',
    });
    expect(result.sessionId).toBe('abc-123');
  });
});

describe('aiClient.startSessionWithUpload', () => {
  it('POSTs multipart with file + query params', async () => {
    fetchMock.mockResolvedValueOnce({
      ok: true,
      status: 200,
      json: async () => ({ sessionId: 'xyz' }),
    });
    const file = new File(['hello'], 'test.pdf', { type: 'application/pdf' });
    const result = await aiClient.startSessionWithUpload(1, 'CATALOG', file);
    expect(result.sessionId).toBe('xyz');
    const call = fetchMock.mock.calls[0];
    expect(call[0]).toContain('/ai/sessions/upload?');
    expect(call[0]).toContain('organizationId=1');
    expect(call[0]).toContain('wizardKind=CATALOG');
    expect(call[1]?.method).toBe('POST');
  });
});

describe('aiClient.listSessions', () => {
  it('GETs /api/ai/analytics/sessions with page/size params and authHeaders', async () => {
    const sessions = [{ id: 'sess-1', status: 'COMPLETED' }];
    fetchMock.mockResolvedValueOnce({
      ok: true,
      status: 200,
      json: async () => sessions,
    });
    const result = await aiClient.listSessions(5, 20, 40);
    expect(result).toEqual(sessions);
    const [url, opts] = fetchMock.mock.calls[0];
    expect(url).toContain('/api/ai/analytics/sessions');
    expect(url).toContain('organizationId=5');
    expect(url).toContain('page=2');
    expect(url).toContain('size=20');
    expect(opts.method).toBe('GET');
    expect(opts.headers).toMatchObject({ Authorization: 'Bearer fake-token' });
  });
});

describe('aiClient.getSession', () => {
  it('GETs /api/ai/analytics/sessions/{id} with orgId and authHeaders', async () => {
    const detail = { summary: { id: 'sess-42' }, events: [], errorMessage: null };
    fetchMock.mockResolvedValueOnce({
      ok: true,
      status: 200,
      json: async () => detail,
    });
    const result = await aiClient.getSession(3, 'sess-42');
    expect(result).toEqual(detail);
    const [url, opts] = fetchMock.mock.calls[0];
    expect(url).toContain('/api/ai/analytics/sessions/sess-42');
    expect(url).toContain('organizationId=3');
    expect(opts.method).toBe('GET');
    expect(opts.headers).toMatchObject({ Authorization: 'Bearer fake-token' });
  });
});

describe('aiClient.getUsageTotals', () => {
  it('GETs /api/ai/analytics/totals with orgId and authHeaders', async () => {
    const totals = { totalSessions: 10, totalTokensIn: 5000, totalTokensOut: 2000,
      totalCostUsdMicros: 50000, sessionsThisMonth: 3, costThisMonthUsdMicros: 15000 };
    fetchMock.mockResolvedValueOnce({
      ok: true,
      status: 200,
      json: async () => totals,
    });
    const result = await aiClient.getUsageTotals(9);
    expect(result).toEqual(totals);
    const [url, opts] = fetchMock.mock.calls[0];
    expect(url).toContain('/api/ai/analytics/totals');
    expect(url).toContain('organizationId=9');
    expect(opts.method).toBe('GET');
    expect(opts.headers).toMatchObject({ Authorization: 'Bearer fake-token' });
  });
});
