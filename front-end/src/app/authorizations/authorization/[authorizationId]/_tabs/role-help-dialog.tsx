'use client';

import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogDescription,
  DialogFooter,
  DialogClose,
} from '@/components/ui/dialog';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { CheckCircle2, XCircle } from 'lucide-react';

interface Props {
  open: boolean;
  onOpenChange: (open: boolean) => void;
}

const ROLE_DESCRIPTIONS: Record<'OWNER' | 'EDITOR' | 'CONTRIBUTOR' | 'VIEWER', string> = {
  OWNER:
    'Full control. Can edit, share, and delete the authorization, and manage who else has access.',
  EDITOR:
    'Can edit details, conditions, the authorization document, and the digital signature. Cannot manage grants or delete.',
  CONTRIBUTOR:
    'Can upload Continuous Monitoring snapshots and Documents. Cannot edit the core authorization or manage grants.',
  VIEWER:
    'Read-only. Can see everything attached to the authorization but cannot make changes.',
};

const PERMISSION_MATRIX: {
  label: string;
  OWNER: 'yes' | 'no' | 'own';
  EDITOR: 'yes' | 'no' | 'own';
  CONTRIBUTOR: 'yes' | 'no' | 'own';
  VIEWER: 'yes' | 'no' | 'own';
}[] = [
  {
    label: 'View authorization, ConMon, Documents',
    OWNER: 'yes',
    EDITOR: 'yes',
    CONTRIBUTOR: 'yes',
    VIEWER: 'yes',
  },
  {
    label: 'Edit details, conditions, signature',
    OWNER: 'yes',
    EDITOR: 'yes',
    CONTRIBUTOR: 'no',
    VIEWER: 'no',
  },
  {
    label: 'Upload Continuous Monitoring snapshots',
    OWNER: 'yes',
    EDITOR: 'yes',
    CONTRIBUTOR: 'yes',
    VIEWER: 'no',
  },
  {
    label: 'Upload / edit documents',
    OWNER: 'yes',
    EDITOR: 'yes',
    CONTRIBUTOR: 'yes',
    VIEWER: 'no',
  },
  {
    label: 'Delete ConMon snapshots / documents',
    OWNER: 'yes',
    EDITOR: 'yes',
    CONTRIBUTOR: 'own',
    VIEWER: 'no',
  },
  {
    label: 'Manage access (add / remove people, change roles)',
    OWNER: 'yes',
    EDITOR: 'no',
    CONTRIBUTOR: 'no',
    VIEWER: 'no',
  },
  {
    label: 'Delete the authorization',
    OWNER: 'yes',
    EDITOR: 'no',
    CONTRIBUTOR: 'no',
    VIEWER: 'no',
  },
];

function PermissionCell({ value }: { value: 'yes' | 'no' | 'own' }) {
  if (value === 'yes') {
    return <CheckCircle2 className="h-4 w-4 text-green-600" aria-label="Allowed" />;
  }
  if (value === 'no') {
    return <XCircle className="h-4 w-4 text-muted-foreground" aria-label="Not allowed" />;
  }
  return (
    <span className="text-xs text-muted-foreground" aria-label="Own items only">
      own only
    </span>
  );
}

export function RoleHelpDialog({ open, onOpenChange }: Props) {
  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-w-3xl">
        <DialogHeader>
          <DialogTitle>Access levels</DialogTitle>
          <DialogDescription>
            What each role can do on this authorization. Org admins and platform super admins always
            have full access.
          </DialogDescription>
        </DialogHeader>

        <div className="space-y-4">
          <section className="grid grid-cols-1 gap-3 sm:grid-cols-2">
            {(Object.keys(ROLE_DESCRIPTIONS) as Array<keyof typeof ROLE_DESCRIPTIONS>).map(
              (role) => (
                <div key={role} className="rounded-md border p-3">
                  <div className="mb-1 flex items-center gap-2">
                    <Badge variant={role === 'OWNER' ? 'default' : 'secondary'}>{role}</Badge>
                  </div>
                  <p className="text-sm text-muted-foreground">{ROLE_DESCRIPTIONS[role]}</p>
                </div>
              ),
            )}
          </section>

          <section>
            <h3 className="mb-2 text-sm font-semibold">Permissions at a glance</h3>
            <div className="overflow-x-auto rounded-md border">
              <table className="w-full text-sm">
                <thead className="bg-muted/50">
                  <tr>
                    <th className="px-3 py-2 text-left font-medium">Action</th>
                    <th className="px-3 py-2 text-center font-medium">OWNER</th>
                    <th className="px-3 py-2 text-center font-medium">EDITOR</th>
                    <th className="px-3 py-2 text-center font-medium">CONTRIB.</th>
                    <th className="px-3 py-2 text-center font-medium">VIEWER</th>
                  </tr>
                </thead>
                <tbody>
                  {PERMISSION_MATRIX.map((row) => (
                    <tr key={row.label} className="border-t">
                      <td className="px-3 py-2">{row.label}</td>
                      <td className="px-3 py-2 text-center">
                        <div className="flex justify-center">
                          <PermissionCell value={row.OWNER} />
                        </div>
                      </td>
                      <td className="px-3 py-2 text-center">
                        <div className="flex justify-center">
                          <PermissionCell value={row.EDITOR} />
                        </div>
                      </td>
                      <td className="px-3 py-2 text-center">
                        <div className="flex justify-center">
                          <PermissionCell value={row.CONTRIBUTOR} />
                        </div>
                      </td>
                      <td className="px-3 py-2 text-center">
                        <div className="flex justify-center">
                          <PermissionCell value={row.VIEWER} />
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
            <p className="mt-2 text-xs text-muted-foreground">
              <strong>own only</strong> = a CONTRIBUTOR can delete only the ConMon snapshots and
              documents they personally uploaded.
            </p>
          </section>

          <section className="rounded-md border bg-muted/30 p-3">
            <h3 className="mb-1 text-sm font-semibold">Sharing with the whole organization</h3>
            <p className="text-xs text-muted-foreground">
              The &quot;Share with all org members as&quot; setting gives every active member of
              your organization access at the chosen level (VIEWER, CONTRIBUTOR, or EDITOR). You can
              still grant individual users a higher role on top — the more privileged role wins.
              OWNER cannot be set as the org-wide default.
            </p>
          </section>
        </div>

        <DialogFooter>
          <DialogClose asChild>
            <Button variant="secondary">Got it</Button>
          </DialogClose>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
