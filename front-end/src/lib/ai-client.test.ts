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
