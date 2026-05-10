'use client';

import { useCallback, useState } from 'react';
import { apiClient, RuleGenSessionExpiredError } from '@/lib/api-client';
import type {
  ChatEntry,
  OscalModelType,
  RuleGenTurnResponse,
} from '@/types/rule-gen';

interface RuleGenState {
  sessionId: string | null;
  organizationId: number | null;
  modelType: OscalModelType | null;
  chat: ChatEntry[];
  latest: RuleGenTurnResponse | null;
  loading: boolean;
  error: string | null;
  /**
   * Set briefly when the hook silently restarts a session that the backend
   * forgot (typically a dev hot-reload). The wizard surfaces this to the user
   * as a toast so they understand why their conversation is shorter than they
   * remember.
   */
  recovered: boolean;
}

const INITIAL: RuleGenState = {
  sessionId: null,
  organizationId: null,
  modelType: null,
  chat: [],
  latest: null,
  loading: false,
  error: null,
  recovered: false,
};

export function useRuleGenSession() {
  const [state, setState] = useState<RuleGenState>(INITIAL);

  const start = useCallback(async (organizationId: number, modelType: OscalModelType) => {
    setState((s) => ({ ...s, loading: true, error: null, organizationId, modelType }));
    try {
      const { sessionId } = await apiClient.startRuleGen(organizationId, modelType);
      setState((s) => ({ ...s, sessionId, loading: false }));
    } catch (e) {
      setState((s) => ({ ...s, error: errorMessage(e), loading: false }));
    }
  }, []);

  const send = useCallback(async (userMessage: string) => {
    let currentId: string | null = null;
    let currentOrgId: number | null = null;
    let currentModel: OscalModelType | null = null;
    setState((s) => {
      currentId = s.sessionId;
      currentOrgId = s.organizationId;
      currentModel = s.modelType;
      return {
        ...s,
        loading: true,
        error: null,
        recovered: false,
        chat: [...s.chat, { role: 'user', text: userMessage }],
      };
    });
    if (!currentId) return;

    try {
      const res = await apiClient.sendRuleGenTurn(currentId, userMessage);
      setState((s) => ({
        ...s,
        loading: false,
        latest: res,
        chat: [...s.chat, { role: 'assistant', text: renderAssistantBlurb(res) }],
      }));
    } catch (e) {
      if (e instanceof RuleGenSessionExpiredError && currentOrgId !== null && currentModel) {
        // Backend restarted (or TTL expired). Silently start a new session
        // and replay the message. We lose prior history, which is fine for
        // a "first turn after restart" but worth surfacing as a banner.
        try {
          const { sessionId: newId } = await apiClient.startRuleGen(currentOrgId, currentModel);
          const res = await apiClient.sendRuleGenTurn(newId, userMessage);
          setState((s) => ({
            ...s,
            sessionId: newId,
            loading: false,
            latest: res,
            recovered: true,
            // The lost chat history was tied to the expired session; keep the
            // user message we just resent and the assistant's fresh reply.
            chat: [
              { role: 'user', text: userMessage },
              { role: 'assistant', text: renderAssistantBlurb(res) },
            ],
          }));
          return;
        } catch (retryErr) {
          setState((s) => ({ ...s, error: errorMessage(retryErr), loading: false }));
          return;
        }
      }
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
      // No auto-recover for edit: the edit refers to a specific in-flight
      // proposal that doesn't survive a session restart.
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
