import { ReactNode } from 'react';
import { Info, AlertTriangle, AlertOctagon } from 'lucide-react';
import { cn } from '@/lib/utils';

type CalloutType = 'info' | 'warn' | 'danger';

const STYLES: Record<CalloutType, { wrap: string; icon: React.ComponentType<{ className?: string }> }> = {
  info:   { wrap: 'border-blue-500/40 bg-blue-500/10 text-blue-100 callout-info',   icon: Info },
  warn:   { wrap: 'border-amber-500/40 bg-amber-500/10 text-amber-100 callout-warn', icon: AlertTriangle },
  danger: { wrap: 'border-red-500/40 bg-red-500/10 text-red-100 callout-danger',     icon: AlertOctagon },
};

export function Callout({ type = 'info', children }: { type?: CalloutType; children: ReactNode }) {
  const { wrap, icon: Icon } = STYLES[type];
  return (
    <div role="note" className={cn('my-6 flex gap-3 rounded-lg border p-4', wrap)}>
      <Icon className="h-5 w-5 mt-0.5 shrink-0" aria-hidden="true" />
      <div className="prose-callout">{children}</div>
    </div>
  );
}
