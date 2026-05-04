import { describe, it, expect, vi, beforeEach } from 'vitest';
import { renderHook, waitFor } from '@testing-library/react';
import { useAiSession } from './useAiSession';

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

  it('clears stale credentials on 401 from the stream endpoint', async () => {
    const removeItem = vi.fn();
    vi.stubGlobal('localStorage', {
      getItem: () => 'stale-jwt',
      setItem: vi.fn(),
      removeItem,
    });
    const fetchMock = vi.fn().mockResolvedValue(new Response(null, { status: 401 }));
    vi.stubGlobal('fetch', fetchMock);

    renderHook(() => useAiSession('abc'));

    await waitFor(() => {
      expect(removeItem).toHaveBeenCalledWith('token');
      expect(removeItem).toHaveBeenCalledWith('user');
    });
  });
});
