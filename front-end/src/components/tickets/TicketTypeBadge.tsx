'use client';
import type { TicketType } from '@/types/ticket';

const TYPE_LABEL: Record<TicketType, string> = { BUG: 'Bug', FEATURE: 'Feature' };
const TYPE_COLOR: Record<TicketType, string> = {
  BUG: 'bg-rose-100 text-rose-800',
  FEATURE: 'bg-sky-100 text-sky-800',
};

export function TicketTypeBadge({ type }: { type: TicketType }) {
  return (
    <span className={`inline-flex items-center rounded-full px-2 py-0.5 text-xs font-medium ${TYPE_COLOR[type]}`}>
      {TYPE_LABEL[type]}
    </span>
  );
}
