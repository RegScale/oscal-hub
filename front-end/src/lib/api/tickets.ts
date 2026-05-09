import type {
  TicketDetailResponse, TicketSummaryResponse,
  TicketType, TicketPriority, TicketStatus,
} from '@/types/ticket';

const BASE = '/api/tickets';
const ADMIN_BASE = '/api/admin/tickets';

function authHeaders(): HeadersInit {
  const token = typeof window !== 'undefined' ? localStorage.getItem('token') : null;
  return token ? { Authorization: `Bearer ${token}` } : {};
}

export async function createTicket(form: {
  type: TicketType; title: string; description: string;
  priority: TicketPriority; metadata: Record<string, unknown>;
  files: File[];
}): Promise<TicketSummaryResponse> {
  const fd = new FormData();
  fd.set('type', form.type);
  fd.set('title', form.title);
  fd.set('description', form.description);
  fd.set('priority', form.priority);
  fd.set('metadata', JSON.stringify(form.metadata));
  form.files.forEach(f => fd.append('files', f));
  const res = await fetch(BASE, { method: 'POST', headers: authHeaders(), body: fd });
  if (!res.ok) throw new Error(`Create failed: ${res.status}`);
  return res.json();
}

export async function listMyTickets(
  page = 0, size = 25,
): Promise<{ content: TicketSummaryResponse[]; totalElements: number }> {
  const res = await fetch(`${BASE}/mine?page=${page}&size=${size}`, { headers: authHeaders() });
  if (!res.ok) throw new Error(`List failed: ${res.status}`);
  return res.json();
}

export async function getTicket(id: number): Promise<TicketDetailResponse> {
  const res = await fetch(`${BASE}/${id}`, { headers: authHeaders() });
  if (!res.ok) throw new Error(`Get failed: ${res.status}`);
  return res.json();
}

export async function addComment(
  ticketId: number, body: string, files: File[]
): Promise<void> {
  const fd = new FormData();
  fd.set('body', body);
  files.forEach(f => fd.append('files', f));
  const res = await fetch(`${BASE}/${ticketId}/comments`, {
    method: 'POST', headers: authHeaders(), body: fd,
  });
  if (!res.ok) throw new Error(`Comment failed: ${res.status}`);
}

export async function changeStatus(
  ticketId: number, status: TicketStatus, note?: string
): Promise<TicketSummaryResponse> {
  const res = await fetch(`${ADMIN_BASE}/${ticketId}/status`, {
    method: 'PATCH',
    headers: { ...authHeaders(), 'Content-Type': 'application/json' },
    body: JSON.stringify({ status, note: note ?? null }),
  });
  if (!res.ok) throw new Error(`Status change failed: ${res.status}`);
  return res.json();
}

export function attachmentDownloadUrl(attachmentId: number): string {
  return `${BASE}/attachments/${attachmentId}`;
}

export interface TicketAnalytics {
  statusCounts: Record<TicketStatus, number>;
  typeSplit: Record<TicketType, number>;
  openedPerWeek: { week: string; count: number }[];
  resolvedPerWeek: { week: string; count: number }[];
  staleTickets: {
    id: number; type: TicketType; title: string;
    priority: TicketPriority; createdAt: string; ageDays: number;
  }[];
}

export async function getTicketAnalytics(): Promise<TicketAnalytics> {
  const res = await fetch(`${ADMIN_BASE}/analytics`, { headers: authHeaders() });
  if (!res.ok) throw new Error(`Analytics failed: ${res.status}`);
  return res.json();
}

export interface AdminListParams {
  page?: number; size?: number;
  q?: string;
  status?: TicketStatus[];
  type?: TicketType;
  priority?: TicketPriority[];
  from?: string; to?: string;
}

export async function listAdminTickets(p: AdminListParams = {})
  : Promise<{ content: TicketSummaryResponse[]; totalElements: number; totalPages: number }> {
  const qs = new URLSearchParams();
  qs.set('page', String(p.page ?? 0));
  qs.set('size', String(p.size ?? 25));
  if (p.q) qs.set('q', p.q);
  if (p.type) qs.set('type', p.type);
  (p.status ?? []).forEach(s => qs.append('status', s));
  (p.priority ?? []).forEach(pr => qs.append('priority', pr));
  if (p.from) qs.set('from', p.from);
  if (p.to) qs.set('to', p.to);
  const res = await fetch(`${ADMIN_BASE}?${qs}`, { headers: authHeaders() });
  if (!res.ok) throw new Error(`Admin list failed: ${res.status}`);
  return res.json();
}
