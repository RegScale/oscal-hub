'use client';

import { Card } from '@/components/ui/card';
import { Check, X } from 'lucide-react';
import type { TestResult } from '@/types/rule-gen';

interface Props {
  results: TestResult[] | null;
}

export function TestMatrix({ results }: Props) {
  if (!results) {
    return (
      <Card className="p-4 text-sm text-muted-foreground">
        Synthetic test results will appear here.
      </Card>
    );
  }
  return (
    <Card className="p-0 overflow-hidden">
      <table className="w-full text-sm">
        <thead className="bg-muted">
          <tr>
            <th className="text-left px-3 py-2">#</th>
            <th className="text-left px-3 py-2">Case</th>
            <th className="text-left px-3 py-2">Expected</th>
            <th className="text-left px-3 py-2">Actual</th>
            <th className="text-center px-3 py-2 w-12">Status</th>
          </tr>
        </thead>
        <tbody>
          {results.map((r) => (
            <tr key={r.index} className="border-t">
              <td className="px-3 py-2 text-muted-foreground">{r.index + 1}</td>
              <td className="px-3 py-2">
                <div>{r.description}</div>
                {!r.passed && r.violationMessage && (
                  <div className="text-xs text-muted-foreground mt-1">
                    {r.violationMessage}
                  </div>
                )}
              </td>
              <td className="px-3 py-2">{r.expected}</td>
              <td className="px-3 py-2">{r.actual}</td>
              <td className="px-3 py-2 text-center">
                {r.passed
                  ? <Check className="inline h-4 w-4 text-emerald-600" />
                  : <X className="inline h-4 w-4 text-red-600" />}
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </Card>
  );
}
