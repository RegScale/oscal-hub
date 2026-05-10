'use client';
import type { TicketStatus } from '@/types/ticket';

const STATUS_LABEL: Record<TicketStatus, string> = {
  OPEN: 'Open', IN_PROGRESS: 'In Progress', RESOLVED: 'Resolved',
  CLOSED: 'Closed', WONT_FIX: "Won't Fix", DUPLICATE: 'Duplicate',
};
const STATUS_COLOR: Record<TicketStatus, string> = {
  OPEN: 'bg-blue-100 text-blue-800',
  IN_PROGRESS: 'bg-amber-100 text-amber-800',
  RESOLVED: 'bg-green-100 text-green-800',
  CLOSED: 'bg-gray-200 text-gray-800',
  WONT_FIX: 'bg-rose-100 text-rose-800',
  DUPLICATE: 'bg-purple-100 text-purple-800',
};

export function TicketStatusBadge({ status }: { status: TicketStatus }) {
  return (
    <span className={`inline-flex items-center rounded-full px-2 py-0.5 text-xs font-medium ${STATUS_COLOR[status]}`}>
      {STATUS_LABEL[status]}
    </span>
  );
}
