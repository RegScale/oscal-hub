'use client';

import { useCallback, useState } from 'react';
import { apiClient } from '@/lib/api-client';
import type {
  ChatEntry,
  OscalModelType,
  RuleGenTurnResponse,
} from '@/types/rule-gen';

interface RuleGenState {
  sessionId: string | null;
  modelType: OscalModelType | null;
  chat: ChatEntry[];
  latest: RuleGenTurnResponse | null;
  loading: boolean;
  error: string | null;
}

const INITIAL: RuleGenState = {
  sessionId: null,
  modelType: null,
  chat: [],
  latest: null,
  loading: false,
  error: null,
};

export function useRuleGenSession() {
  const [state, setState] = useState<RuleGenState>(INITIAL);

  const start = useCallback(async (organizationId: number, modelType: OscalModelType) => {
    setState((s) => ({ ...s, loading: true, error: null, modelType }));
    try {
      const { sessionId } = await apiClient.startRuleGen(organizationId, modelType);
      setState((s) => ({ ...s, sessionId, loading: false }));
    } catch (e) {
      setState((s) => ({ ...s, error: errorMessage(e), loading: false }));
    }
  }, []);

  const send = useCallback(async (userMessage: string) => {
    let currentId: string | null = null;
    setState((s) => {
      currentId = s.sessionId;
      return {
        ...s,
        loading: true,
        error: null,
        chat: [...s.chat, { role: 'user', text: userMessage }],
      };
    });
    if (!currentId) return;
    try {
      const res = await apiClient.sendRuleGenTurn(currentId, userMessage);
      const assistantText = renderAssistantBlurb(res);
      setState((s) => ({
        ...s,
        loading: false,
        latest: res,
        chat: [...s.chat, { role: 'assistant', text: assistantText }],
      }));
    } catch (e) {
      setState((s) => ({ ...s, error: errorMessage(e), loading: false }));
    }
  }, []);

  const editConstraint = useCallback(async (constraintXml: string) => {
    let currentId: string | null = null;
    setState((s) => {
      currentId = s.sessionId;
      return { ...s, loading: true, error: null };
    });
    if (!currentId) return;
    try {
      const res = await apiClient.editRuleGenProposal(currentId, constraintXml);
      setState((s) => ({ ...s, loading: false, latest: res }));
    } catch (e) {
      setState((s) => ({ ...s, error: errorMessage(e), loading: false }));
    }
  }, []);

  const save = useCallback(async (ruleId: string, category?: string) => {
    if (!state.sessionId) return null;
    return apiClient.saveRuleGenRule(state.sessionId, ruleId, category, true);
  }, [state.sessionId]);

  const abandon = useCallback(async () => {
    if (state.sessionId) {
      try {
        await apiClient.abandonRuleGen(state.sessionId);
      } catch {
        // best-effort cleanup; the server-side cache will TTL the session anyway.
      }
    }
    setState(INITIAL);
  }, [state.sessionId]);

  return { ...state, start, send, editConstraint, save, abandon };
}

function renderAssistantBlurb(res: RuleGenTurnResponse): string {
  if (res.phase === 'clarify') return res.clarifyingQuestion ?? '';
  if (res.phase === 'proposal') {
    return `Drafted "${res.proposal?.name}" — ${res.testResults?.length ?? 0} test cases all pass.`;
  }
  return res.message ?? "I couldn't reach a working rule.";
}

function errorMessage(e: unknown): string {
  if (e instanceof Error) return e.message;
  return String(e);
}
