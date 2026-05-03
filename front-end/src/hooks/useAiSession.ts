'use client';
import { useCallback, useEffect, useRef, useState } from 'react';

const API_BASE_URL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8090/api';

export type AiEventType =
  | 'session_started'
  | 'progress'
  | 'tool_call'
  | 'tool_result'
  | 'awaiting_input'
  | 'chunk'
  | 'partial_document'
  | 'complete'
  | 'error';

export interface AiEvent {
  type: AiEventType;
  data: Record<string, unknown>;
}

export interface UseAiSessionState {
  events: AiEvent[];
  isComplete: boolean;
  error: string | null;
  finalDocument: unknown | null;
  cancel: () => void;
}

export function useAiSession(sessionId: string | null): UseAiSessionState {
  const [events, setEvents] = useState<AiEvent[]>([]);
  const [isComplete, setIsComplete] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [finalDocument, setFinalDocument] = useState<unknown | null>(null);
  const sourceRef = useRef<EventSource | null>(null);

  useEffect(() => {
    if (!sessionId) return;
    const url = `${API_BASE_URL}/ai/sessions/${sessionId}/stream`;
    const source = new EventSource(url, { withCredentials: false });
    sourceRef.current = source;

    const handle = (type: AiEventType) => (e: MessageEvent) => {
      try {
        const data = JSON.parse(e.data);
        setEvents((prev) => [...prev, { type, data }]);
        if (type === 'complete') {
          setFinalDocument((data as { document?: unknown }).document ?? null);
          setIsComplete(true);
          source.close();
        }
        if (type === 'error') {
          setError((data as { message?: string }).message ?? 'Unknown error');
          setIsComplete(true);
          source.close();
        }
      } catch (err) {
        console.error('Failed to parse SSE event', err);
      }
    };

    const types: AiEventType[] = [
      'session_started', 'progress', 'tool_call', 'tool_result',
      'awaiting_input', 'chunk', 'partial_document', 'complete', 'error',
    ];
    types.forEach((t) => source.addEventListener(t, handle(t)));

    source.onerror = () => {
      setError('Stream interrupted');
      source.close();
    };

    return () => {
      source.close();
    };
  }, [sessionId]);

  const cancel = useCallback(() => {
    sourceRef.current?.close();
    if (sessionId) {
      fetch(`${API_BASE_URL}/ai/sessions/${sessionId}`, {
        method: 'DELETE',
        headers: { Authorization: `Bearer ${localStorage.getItem('token')}` },
      }).catch(() => {});
    }
  }, [sessionId]);

  return { events, isComplete, error, finalDocument, cancel };
}
