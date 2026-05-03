import { describe, it, expect, vi, beforeEach } from 'vitest';
import { renderHook, act } from '@testing-library/react';
import { useAiSession } from './useAiSession';

class MockEventSource {
  static instances: MockEventSource[] = [];
  url: string;
  listeners: Record<string, ((e: MessageEvent) => void)[]> = {};
  closed = false;
  onerror: ((e: Event) => void) | null = null;
  constructor(url: string) {
    this.url = url;
    MockEventSource.instances.push(this);
  }
  addEventListener(type: string, fn: (e: MessageEvent) => void) {
    (this.listeners[type] ||= []).push(fn);
  }
  emit(type: string, data: unknown) {
    (this.listeners[type] || []).forEach((fn) =>
      fn(new MessageEvent(type, { data: JSON.stringify(data) })),
    );
  }
  close() { this.closed = true; }
}

beforeEach(() => {
  MockEventSource.instances = [];
  vi.stubGlobal('EventSource', MockEventSource as unknown as typeof EventSource);
});

describe('useAiSession', () => {
  it('starts empty and accumulates events', () => {
    const { result } = renderHook(() => useAiSession('abc'));
    expect(result.current.events).toEqual([]);
    act(() => MockEventSource.instances[0].emit('progress', { message: 'hi' }));
    expect(result.current.events).toHaveLength(1);
  });

  it('marks complete on complete event', () => {
    const { result } = renderHook(() => useAiSession('abc'));
    act(() => MockEventSource.instances[0].emit('complete', { document: { ok: true } }));
    expect(result.current.isComplete).toBe(true);
    expect(result.current.finalDocument).toEqual({ ok: true });
  });
});
