import { ReactNode } from 'react';

export function Step({ title, children }: { title?: string; children: ReactNode }) {
  return (
    <li className="pl-2">
      {title && <span className="font-medium text-foreground">{title}</span>}
      <div className="mt-1 text-muted-foreground space-y-2">{children}</div>
    </li>
  );
}
