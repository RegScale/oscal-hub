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

  it('surfaces an error when the stream closes without a terminal event', async () => {
    // Simulates Cloud Run cutting the SSE connection at 5 minutes — chunks
    // streamed in, no `complete` or `error` event ever arrives, then the
    // server closes the response. The UI must not be left spinning forever.
    const fetchMock = vi.fn().mockResolvedValue(
      sseResponse([
        'event: progress\ndata: {"message":"Drafting GP family"}\n\n',
        'event: progress\ndata: {"message":"Drafting SP family"}\n\n',
        // No complete/error — stream just ends.
      ]),
    );
    vi.stubGlobal('fetch', fetchMock);

    const { result } = renderHook(() => useAiSession('abc'));

    await waitFor(() => {
      expect(result.current.isComplete).toBe(true);
    });
    expect(result.current.error).toMatch(/connection.*closed.*before.*finished/i);
    expect(result.current.events).toHaveLength(2);
  });

  it('does NOT surface a closed-without-terminal error when complete arrived normally', async () => {
    const fetchMock = vi.fn().mockResolvedValue(
      sseResponse([
        'event: progress\ndata: {"message":"working"}\n\n',
        'event: complete\ndata: {"document":{"ok":true}}\n\n',
      ]),
    );
    vi.stubGlobal('fetch', fetchMock);

    const { result } = renderHook(() => useAiSession('abc'));

    await waitFor(() => {
      expect(result.current.isComplete).toBe(true);
    });
    expect(result.current.error).toBeNull();
    expect(result.current.finalDocument).toEqual({ ok: true });
  });

  it('does NOT surface a closed-without-terminal error when error event arrived', async () => {
    const fetchMock = vi.fn().mockResolvedValue(
      sseResponse([
        'event: error\ndata: {"message":"model error"}\n\n',
      ]),
    );
    vi.stubGlobal('fetch', fetchMock);

    const { result } = renderHook(() => useAiSession('abc'));

    await waitFor(() => {
      expect(result.current.isComplete).toBe(true);
    });
    // The explicit error event wins, not the close-without-terminal fallback.
    expect(result.current.error).toBe('model error');
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
