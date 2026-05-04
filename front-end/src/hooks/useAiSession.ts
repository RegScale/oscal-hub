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

// Translate raw backend/SDK error messages into something a user can act on.
// Backend logs still keep the full message; this only changes what's surfaced.
export function formatAiError(raw: string | null | undefined): string {
  if (!raw) return 'Unknown error';
  const m = raw.toLowerCase();
  if (m.includes('overloaded') || m.includes('529')) {
    return 'Anthropic AI is currently overloaded. Please wait a minute and try again.';
  }
  if (m.includes('rate_limit') || m.includes('rate limit') || m.includes('429')) {
    return 'AI rate limit reached for your API key. Please wait a moment and try again.';
  }
  if (m.includes('context_length') || m.includes('prompt is too long') || m.includes('max_tokens')) {
    return 'Document too large for AI processing. Try a smaller or trimmed source file.';
  }
  if (m.includes('invalid_api_key') || m.includes('authentication_error') || m.includes('401')) {
    return 'AI authentication failed. Check your Anthropic API key in Org Admin → AI Settings.';
  }
  if (m.includes('insufficient_quota') || m.includes('billing')) {
    return 'AI account has insufficient quota. Check your Anthropic billing.';
  }
  return raw;
}

// Parse one SSE event block. Block is the text between two consecutive `\n\n`
// separators in the stream. Returns null if the block has no `data:` line.
function parseSseBlock(block: string): { event: string; data: string } | null {
  let event = 'message';
  const dataLines: string[] = [];
  for (const line of block.split('\n')) {
    if (line.startsWith('event:')) event = line.slice(6).trim();
    else if (line.startsWith('data:')) dataLines.push(line.slice(5).trimStart());
  }
  if (dataLines.length === 0) return null;
  return { event, data: dataLines.join('\n') };
}

export function useAiSession(sessionId: string | null): UseAiSessionState {
  const [events, setEvents] = useState<AiEvent[]>([]);
  const [isComplete, setIsComplete] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [finalDocument, setFinalDocument] = useState<unknown | null>(null);
  const abortRef = useRef<AbortController | null>(null);

  useEffect(() => {
    if (!sessionId) return;

    const ac = new AbortController();
    abortRef.current = ac;

    (async () => {
      const token = typeof window !== 'undefined' ? localStorage.getItem('token') : null;
      try {
        const res = await fetch(`${API_BASE_URL}/ai/sessions/${sessionId}/stream`, {
          method: 'GET',
          headers: {
            Accept: 'text/event-stream',
            ...(token ? { Authorization: `Bearer ${token}` } : {}),
          },
          signal: ac.signal,
        });
        if (!res.ok) {
          if (res.status === 401 && typeof window !== 'undefined') {
            localStorage.removeItem('token');
            localStorage.removeItem('user');
          }
          setError(`Stream failed: HTTP ${res.status}${res.status === 401 ? ' — session expired, please log in again' : ''}`);
          setIsComplete(true);
          return;
        }
        if (!res.body) {
          setError('Stream returned no body');
          setIsComplete(true);
          return;
        }

        const reader = res.body.getReader();
        const decoder = new TextDecoder();
        let buffer = '';
        for (;;) {
          const { done, value } = await reader.read();
          if (done) break;
          buffer += decoder.decode(value, { stream: true });

          // SSE event blocks are separated by a blank line (\n\n).
          let sep: number;
          while ((sep = buffer.indexOf('\n\n')) !== -1) {
            const block = buffer.slice(0, sep);
            buffer = buffer.slice(sep + 2);
            const parsed = parseSseBlock(block);
            if (!parsed) continue;
            let data: Record<string, unknown> = {};
            try {
              data = JSON.parse(parsed.data) as Record<string, unknown>;
            } catch {
              data = { raw: parsed.data };
            }
            const type = parsed.event as AiEventType;
            setEvents((prev) => [...prev, { type, data }]);
            if (type === 'complete') {
              setFinalDocument((data as { document?: unknown }).document ?? null);
              setIsComplete(true);
              ac.abort();
            } else if (type === 'error') {
              setError(formatAiError((data as { message?: string }).message));
              setIsComplete(true);
              ac.abort();
            }
          }
        }
      } catch (err) {
        if ((err as Error).name === 'AbortError') return;
        setError('Stream interrupted: ' + ((err as Error).message ?? 'unknown'));
        setIsComplete(true);
      }
    })();

    return () => {
      ac.abort();
    };
  }, [sessionId]);

  const cancel = useCallback(() => {
    abortRef.current?.abort();
    if (sessionId) {
      const token = typeof window !== 'undefined' ? localStorage.getItem('token') : null;
      fetch(`${API_BASE_URL}/ai/sessions/${sessionId}`, {
        method: 'DELETE',
        headers: token ? { Authorization: `Bearer ${token}` } : {},
      }).catch(() => {});
    }
  }, [sessionId]);

  return { events, isComplete, error, finalDocument, cancel };
}
