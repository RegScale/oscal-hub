import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest';
import { apiClient } from '../api-client';
import type { LeaderboardResponse } from '@/types/oscal';

const mockFetch = vi.fn();
global.fetch = mockFetch;

const localStorageMock = (() => {
  let store: Record<string, string> = {};
  return {
    getItem: (key: string) => store[key] || null,
    setItem: (key: string, value: string) => {
      store[key] = value;
    },
    removeItem: (key: string) => {
      delete store[key];
    },
    clear: () => {
      store = {};
    },
  };
})();

Object.defineProperty(window, 'localStorage', {
  value: localStorageMock,
  writable: true,
});

const sampleResponse: LeaderboardResponse = {
  window: '30d',
  generatedAt: '2026-07-31T12:00:00Z',
  mostActive: [
    {
      rank: 1,
      username: 'alice',
      displayName: 'Alice Ames',
      score: 42,
      breakdown: { operations: 40, libraryPublishes: 2 },
    },
  ],
  topContributors: [{ rank: 1, username: 'bob', displayName: 'Bob Brown', score: 7 }],
};

describe('apiClient.getLeaderboard', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    localStorageMock.clear();
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('requests the leaderboard for the given window with auth headers', async () => {
    localStorageMock.setItem('token', 'jwt-123');
    mockFetch.mockResolvedValueOnce({
      ok: true,
      status: 200,
      json: async () => sampleResponse,
    });

    const result = await apiClient.getLeaderboard('30d');

    expect(mockFetch).toHaveBeenCalledTimes(1);
    const [url, options] = mockFetch.mock.calls[0];
    expect(url).toContain('/leaderboard?window=30d');
    expect(options.method).toBe('GET');
    expect((options.headers as Record<string, string>).Authorization).toBe('Bearer jwt-123');
    expect(result).toEqual(sampleResponse);
  });

  it('defaults to the all-time window', async () => {
    mockFetch.mockResolvedValueOnce({
      ok: true,
      status: 200,
      json: async () => ({ ...sampleResponse, window: 'all' }),
    });

    const result = await apiClient.getLeaderboard();

    const [url] = mockFetch.mock.calls[0];
    expect(url).toContain('/leaderboard?window=all');
    expect(result.window).toBe('all');
  });

  it('throws when the server responds with an error', async () => {
    mockFetch.mockResolvedValueOnce({
      ok: false,
      status: 500,
      statusText: 'Internal Server Error',
      json: async () => ({ message: 'boom' }),
    });

    await expect(apiClient.getLeaderboard('all')).rejects.toThrow(/leaderboard/i);
  });
});
