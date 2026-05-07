'use client';

import { Card } from '@/components/ui/card';

export function DocumentsTab() {
  return (
    <Card className="p-8 text-center">
      <h2 className="mb-2 text-lg font-semibold">Documents</h2>
      <p className="text-sm text-muted-foreground">
        Coming soon — upload vuln scans, pen tests, asset inventories, and other supporting artifacts.
      </p>
    </Card>
  );
}
