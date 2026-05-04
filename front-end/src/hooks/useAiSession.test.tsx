import { describe, it, expect, vi, beforeEach } from 'vitest';
import { renderHook, waitFor } from '@testing-library/react';
import { useAiSession, formatAiError } from './useAiSession';

function sseResponse(chunks: string[]): Response {
  const encoder = new TextEncoder();
  let i = 0;
  const stream = new ReadableStream<Uint8Array>({
    pull(controller) {
      if (i < chunks.length) {
        controller.enqueue(encoder.encode(chunks[i]));
        i += 1;
      } else {
        controller.close();
      }
    },
  });
  return new Response(stream, { status: 200, headers: { 'Content-Type': 'text/event-stream' } });
}

beforeEach(() => {
  vi.stubGlobal('localStorage', {
    getItem: () => 'fake-jwt',
    setItem: vi.fn(),
    removeItem: vi.fn(),
  });
});

describe('useAiSession', () => {
  it('accumulates progress events from the stream', async () => {
    const fetchMock = vi.fn().mockResolvedValue(
      sseResponse(['event: progress\ndata: {"message":"hi"}\n\n']),
    );
    vi.stubGlobal('fetch', fetchMock);

    const { result } = renderHook(() => useAiSession('abc'));

    await waitFor(() => {
      expect(result.current.events).toHaveLength(1);
    });
    expect(result.current.events[0]).toEqual({ type: 'progress', data: { message: 'hi' } });
  });

  it('marks complete on complete event and captures finalDocument', async () => {
    const fetchMock = vi.fn().mockResolvedValue(
      sseResponse(['event: complete\ndata: {"document":{"ok":true}}\n\n']),
    );
    vi.stubGlobal('fetch', fetchMock);

    const { result } = renderHook(() => useAiSession('abc'));

    await waitFor(() => {
      expect(result.current.isComplete).toBe(true);
    });
    expect(result.current.finalDocument).toEqual({ ok: true });
  });

  it('clears stale credentials and surfaces error on 401 from the stream endpoint', async () => {
    const removeItem = vi.fn();
    vi.stubGlobal('localStorage', {
      getItem: () => 'stale-jwt',
      setItem: vi.fn(),
      removeItem,
    });
    const fetchMock = vi.fn().mockResolvedValue(new Response(null, { status: 401 }));
    vi.stubGlobal('fetch', fetchMock);

    const { result } = renderHook(() => useAiSession('abc'));

    await waitFor(() => {
      expect(removeItem).toHaveBeenCalledWith('token');
      expect(removeItem).toHaveBeenCalledWith('user');
    });
    expect(result.current.error).toContain('401');
    expect(result.current.error).toMatch(/session expired/i);
    expect(result.current.isComplete).toBe(true);
  });
});

describe('formatAiError', () => {
  it('translates Anthropic 529 overloaded into a friendly message', () => {
    const raw = 'Error 529: {type=error, error={type=overloaded_error, message=Overloaded}, request_id=req_X}';
    expect(formatAiError(raw)).toMatch(/overloaded.*try again/i);
  });

  it('translates 429 rate limit into a friendly message', () => {
    expect(formatAiError('429 rate_limit_error')).toMatch(/rate limit/i);
  });

  it('translates context length errors into a size hint', () => {
    expect(formatAiError('prompt is too long: 250000 tokens > max 200000'))
      .toMatch(/document too large/i);
  });

  it('translates auth errors into an API-key hint', () => {
    expect(formatAiError('authentication_error: invalid_api_key'))
      .toMatch(/api key/i);
  });

  it('passes unknown errors through verbatim', () => {
    expect(formatAiError('Some unrelated server error')).toBe('Some unrelated server error');
  });

  it('handles null and empty input', () => {
    expect(formatAiError(null)).toBe('Unknown error');
    expect(formatAiError(undefined)).toBe('Unknown error');
    expect(formatAiError('')).toBe('Unknown error');
  });
});
