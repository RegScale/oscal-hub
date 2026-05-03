const API_BASE_URL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8090/api';

function authHeaders(): Record<string, string> {
  const token = typeof window !== 'undefined' ? localStorage.getItem('token') : null;
  const h: Record<string, string> = { 'Content-Type': 'application/json' };
  if (token) h.Authorization = `Bearer ${token}`;
  return h;
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

export const aiClient = {
  async getSettingsStatus(organizationId: number): Promise<{ enabled: boolean }> {
    const res = await fetch(`${API_BASE_URL}/ai/settings/status?organizationId=${organizationId}`, {
      method: 'GET',
      headers: authHeaders(),
    });
    if (!res.ok) throw new Error(`status ${res.status}`);
    return res.json();
  },

  async getSettings(organizationId: number): Promise<AiSettingsResponse> {
    const res = await fetch(`${API_BASE_URL}/ai/settings?organizationId=${organizationId}`, {
      method: 'GET',
      headers: authHeaders(),
    });
    if (!res.ok) throw new Error(`status ${res.status}`);
    return res.json();
  },

  async putSettings(organizationId: number, req: UpdateAiSettingsRequest): Promise<AiSettingsResponse> {
    const res = await fetch(`${API_BASE_URL}/ai/settings?organizationId=${organizationId}`, {
      method: 'PUT',
      headers: authHeaders(),
      body: JSON.stringify(req),
    });
    if (!res.ok) throw new Error(`status ${res.status}`);
    return res.json();
  },

  async disable(organizationId: number): Promise<void> {
    const res = await fetch(`${API_BASE_URL}/ai/settings?organizationId=${organizationId}`, {
      method: 'DELETE',
      headers: authHeaders(),
    });
    if (!res.ok) throw new Error(`status ${res.status}`);
  },

  async startSession(req: StartSessionRequest): Promise<StartSessionResponse> {
    const res = await fetch(`${API_BASE_URL}/ai/sessions`, {
      method: 'POST',
      headers: authHeaders(),
      body: JSON.stringify(req),
    });
    if (!res.ok) throw new Error(`status ${res.status}`);
    return res.json();
  },

  async startSessionWithUpload(
    organizationId: number,
    wizardKind: WizardKind,
    file: File,
    prompt?: string,
    mode: SessionMode = 'STREAMING',
  ): Promise<StartSessionResponse> {
    const fd = new FormData();
    fd.append('file', file);
    const url = new URL(`${API_BASE_URL}/ai/sessions/upload`);
    url.searchParams.set('organizationId', String(organizationId));
    url.searchParams.set('wizardKind', wizardKind);
    url.searchParams.set('mode', mode);
    if (prompt) url.searchParams.set('prompt', prompt);

    const token = typeof window !== 'undefined' ? localStorage.getItem('token') : null;
    const headers: Record<string, string> = {};
    if (token) headers.Authorization = `Bearer ${token}`;
    // Don't set Content-Type — browser sets multipart boundary

    const res = await fetch(url.toString(), { method: 'POST', headers, body: fd });
    if (!res.ok) throw new Error(`status ${res.status}`);
    return res.json();
  },

  async cancelSession(sessionId: string): Promise<void> {
    const res = await fetch(`${API_BASE_URL}/ai/sessions/${sessionId}`, {
      method: 'DELETE',
      headers: authHeaders(),
    });
    if (!res.ok) throw new Error(`status ${res.status}`);
  },
};
