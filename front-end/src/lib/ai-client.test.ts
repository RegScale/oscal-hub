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
