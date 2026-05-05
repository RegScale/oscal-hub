import { ReactNode } from 'react';

export function Steps({ children }: { children: ReactNode }) {
  return <ol className="my-6 space-y-4 list-decimal list-inside marker:font-semibold marker:text-primary">{children}</ol>;
}
