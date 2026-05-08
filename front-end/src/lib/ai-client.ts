const API_BASE_URL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8090/api';

function authHeaders(): Record<string, string> {
  const token = typeof window !== 'undefined' ? localStorage.getItem('token') : null;
  const h: Record<string, string> = { 'Content-Type': 'application/json' };
  if (token) h.Authorization = `Bearer ${token}`;
  return h;
}

// Drop-in replacement for fetch() that, on a non-OK response, throws an Error
// whose message includes both the status code and any body text returned by
// the backend. This lets the calling page show the *server's* explanation
// (e.g. "Full authentication is required to access this resource") instead
// of a bare "status 401". We deliberately do NOT clear localStorage on 401:
// in dev the JWT secret is hardcoded, so a 401 means a real auth bug rather
// than a stale token, and clearing the token would mask the underlying cause.
async function aiFetch(input: string | URL, init?: RequestInit): Promise<Response> {
  const res = await fetch(input, init);
  if (!res.ok) {
    let body = '';
    try {
      body = await res.clone().text();
    } catch {
      // ignore — body wasn't readable
    }
    const detail = body && body.length < 500 ? ` — ${body}` : '';
    throw new Error(`HTTP ${res.status}${detail}`);
  }
  return res;
}

export type WizardKind =
  | 'SMOKE'
  | 'CATALOG'
  | 'PROFILE'
  | 'COMPONENT_DEF'
  | 'SSP'
  | 'POAM'
  | 'BUILDER_ASSIST';

export type SessionMode = 'STREAMING' | 'THOROUGH';

export interface StartSessionRequest {
  organizationId: number;
  wizardKind: WizardKind;
  mode: SessionMode;
  input?: string;
  profileHref?: string | null;
}

export interface StartSessionResponse {
  sessionId: string;
}

export interface AiSettingsResponse {
  enabled: boolean;
  fingerprint: string | null;
  defaultModel: string;
}

export interface UpdateAiSettingsRequest {
  apiKey: string;
  defaultModel?: string;
}

export type AiSessionStatus = 'RUNNING' | 'AWAITING_INPUT' | 'COMPLETED' | 'CANCELLED' | 'FAILED';

export interface AiSessionSummary {
  id: string;
  userId: number;
  username: string | null;
  wizardKind: WizardKind;
  mode: SessionMode;
  model: string;
  status: AiSessionStatus;
  tokensIn: number;
  tokensOut: number;
  costUsdMicros: number;
  startedAt: string;
  endedAt: string | null;
  errorCode: string | null;
}

export interface AiSessionDetail {
  summary: AiSessionSummary;
  events: Array<{ type: string; data: string }>;
  errorMessage: string | null;
}

export interface AiUsageTotals {
  totalSessions: number;
  totalTokensIn: number;
  totalTokensOut: number;
  totalCostUsdMicros: number;
  sessionsThisMonth: number;
  costThisMonthUsdMicros: number;
}

export const aiClient = {
  async getSettingsStatus(organizationId: number): Promise<{ enabled: boolean }> {
    const res = await aiFetch(`${API_BASE_URL}/ai/settings/status?organizationId=${organizationId}`, {
      method: 'GET',
      headers: authHeaders(),
    });
    return res.json();
  },

  async getSettings(organizationId: number): Promise<AiSettingsResponse> {
    const res = await aiFetch(`${API_BASE_URL}/ai/settings?organizationId=${organizationId}`, {
      method: 'GET',
      headers: authHeaders(),
    });
    return res.json();
  },

  async putSettings(organizationId: number, req: UpdateAiSettingsRequest): Promise<AiSettingsResponse> {
    const res = await aiFetch(`${API_BASE_URL}/ai/settings?organizationId=${organizationId}`, {
      method: 'PUT',
      headers: authHeaders(),
      body: JSON.stringify(req),
    });
    return res.json();
  },

  async disable(organizationId: number): Promise<void> {
    const res = await aiFetch(`${API_BASE_URL}/ai/settings?organizationId=${organizationId}`, {
      method: 'DELETE',
      headers: authHeaders(),
    });
  },

  async startSession(req: StartSessionRequest): Promise<StartSessionResponse> {
    const res = await aiFetch(`${API_BASE_URL}/ai/sessions`, {
      method: 'POST',
      headers: authHeaders(),
      body: JSON.stringify(req),
    });
    return res.json();
  },

  async startSessionWithUpload(
    organizationId: number,
    wizardKind: WizardKind,
    file: File,
    options?: { prompt?: string; mode?: SessionMode; profileHref?: string | null },
  ): Promise<StartSessionResponse> {
    const fd = new FormData();
    fd.append('file', file);
    const url = new URL(`${API_BASE_URL}/ai/sessions/upload`);
    url.searchParams.set('organizationId', String(organizationId));
    url.searchParams.set('wizardKind', wizardKind);
    url.searchParams.set('mode', options?.mode ?? 'STREAMING');
    if (options?.prompt) url.searchParams.set('prompt', options.prompt);
    if (options?.profileHref) url.searchParams.set('profileHref', options.profileHref);

    const token = typeof window !== 'undefined' ? localStorage.getItem('token') : null;
    const headers: Record<string, string> = {};
    if (token) headers.Authorization = `Bearer ${token}`;
    // Don't set Content-Type — browser sets multipart boundary

    const res = await aiFetch(url.toString(), { method: 'POST', headers, body: fd });
    return res.json();
  },

  async cancelSession(sessionId: string): Promise<void> {
    const res = await aiFetch(`${API_BASE_URL}/ai/sessions/${sessionId}`, {
      method: 'DELETE',
      headers: authHeaders(),
    });
  },

  async listSessions(orgId: number, limit = 20, offset = 0): Promise<AiSessionSummary[]> {
    const page = Math.floor(offset / limit);
    const res = await aiFetch(
      `${API_BASE_URL}/ai/analytics/sessions?organizationId=${orgId}&page=${page}&size=${limit}`,
      { method: 'GET', headers: authHeaders() },
    );
    return res.json();
  },

  async getSession(orgId: number, id: string): Promise<AiSessionDetail> {
    const res = await aiFetch(
      `${API_BASE_URL}/ai/analytics/sessions/${id}?organizationId=${orgId}`,
      { method: 'GET', headers: authHeaders() },
    );
    return res.json();
  },

  async getUsageTotals(orgId: number): Promise<AiUsageTotals> {
    const res = await aiFetch(
      `${API_BASE_URL}/ai/analytics/totals?organizationId=${orgId}`,
      { method: 'GET', headers: authHeaders() },
    );
    return res.json();
  },
};
