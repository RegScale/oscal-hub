import type { AiEvent } from '@/hooks/useAiSession';

/**
 * Returns a human-readable description of an AI session event for display in
 * step logs and event lists.
 */
export function describeEvent(e: AiEvent): string {
  if (e.type === 'progress') return (e.data as { message?: string }).message ?? 'Working…';
  if (e.type === 'tool_call') {
    const t = (e.data as { tool?: string }).tool;
    return `Calling ${t ?? 'tool'}…`;
  }
  if (e.type === 'tool_result') {
    const t = (e.data as { tool?: string; ok?: boolean }).tool;
    const ok = (e.data as { ok?: boolean }).ok;
    return `${t ?? 'tool'} returned ${ok ? 'ok' : 'errors'}`;
  }
  if (e.type === 'chunk') return ((e.data as { text?: string }).text ?? '').slice(0, 200);
  if (e.type === 'awaiting_input') return 'Waiting for your input';
  if (e.type === 'partial_document') return 'Document update';
  if (e.type === 'complete') return 'Complete';
  if (e.type === 'error') return (e.data as { message?: string }).message ?? 'Error';
  return e.type;
}

/**
 * Event types that are rendered as discrete steps in the wizard run view and
 * the session detail drawer's event log.
 */
export const STEP_EVENT_TYPES: AiEvent['type'][] = [
  'progress',
  'tool_call',
  'tool_result',
  'awaiting_input',
];
