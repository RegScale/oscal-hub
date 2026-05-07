'use client';

import { useEffect, useState } from 'react';
import { Card } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Label } from '@/components/ui/label';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { Trash2, Loader2, Plus } from 'lucide-react';
import { toast } from 'sonner';
import { apiClient } from '@/lib/api-client';
import { UserPicker } from '@/components/user-picker';
import type {
  AuthorizationRole,
  AuthorizationGrantResponse,
  OrgMemberResponse,
  AuthorizationResponse,
} from '@/types/oscal';

const ROLE_OPTIONS: AuthorizationRole[] = ['VIEWER', 'CONTRIBUTOR', 'EDITOR', 'OWNER'];
const SHARE_OPTIONS: AuthorizationRole[] = ['VIEWER', 'CONTRIBUTOR', 'EDITOR'];

interface Props {
  authorization: AuthorizationResponse;
  onAuthorizationUpdated: (a: AuthorizationResponse) => void;
}

export function SharingAccessCard({ authorization, onAuthorizationUpdated }: Props) {
  const [grants, setGrants] = useState<AuthorizationGrantResponse[]>([]);
  const [members, setMembers] = useState<OrgMemberResponse[]>([]);
  const [loadingGrants, setLoadingGrants] = useState(true);
  const [loadingMembers, setLoadingMembers] = useState(true);
  const [pickerValue, setPickerValue] = useState<number | null>(null);
  const [pickerRole, setPickerRole] = useState<AuthorizationRole>('VIEWER');
  const [adding, setAdding] = useState(false);

  useEffect(() => {
    void refresh();
    void loadMembers();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [authorization.id]);

  const refresh = async () => {
    setLoadingGrants(true);
    try {
      const data = await apiClient.listGrants(authorization.id);
      setGrants(data);
    } catch (e) {
      toast.error('Failed to load grants');
    } finally {
      setLoadingGrants(false);
    }
  };

  const loadMembers = async () => {
    setLoadingMembers(true);
    try {
      const data = await apiClient.listMyOrgMembers();
      setMembers(data);
    } catch (e) {
      toast.error('Failed to load org members');
    } finally {
      setLoadingMembers(false);
    }
  };

  const handleAdd = async () => {
    if (pickerValue == null) return;
    setAdding(true);
    try {
      await apiClient.addGrant(authorization.id, pickerValue, pickerRole);
      setPickerValue(null);
      setPickerRole('VIEWER');
      await refresh();
      toast.success('Grant added');
    } catch (e) {
      toast.error('Failed to add grant');
    } finally {
      setAdding(false);
    }
  };

  const handleRoleChange = async (grantId: number, role: AuthorizationRole) => {
    try {
      await apiClient.updateGrant(authorization.id, grantId, role);
      await refresh();
      toast.success('Role updated');
    } catch (e) {
      toast.error('Failed to update role');
    }
  };

  const handleRemove = async (grantId: number) => {
    try {
      await apiClient.removeGrant(authorization.id, grantId);
      await refresh();
      toast.success('Grant removed');
    } catch (e) {
      toast.error('Failed to remove grant');
    }
  };

  const handleShareChange = async (value: string) => {
    const role = value === 'NONE' ? null : (value as AuthorizationRole);
    try {
      const updated = await apiClient.setShareWithOrg(authorization.id, role);
      onAuthorizationUpdated(updated);
      toast.success(role ? `Shared with org as ${role}` : 'Org-wide sharing cleared');
    } catch (e) {
      toast.error('Failed to update sharing');
    }
  };

  const grantedUserIds = grants.map((g) => g.userId);

  return (
    <Card className="p-6">
      <h2 className="mb-1 text-lg font-semibold">Sharing &amp; Access</h2>
      <p className="mb-4 text-sm text-muted-foreground">
        Manage who can view, edit, or contribute to this authorization within your organization.
      </p>

      <section className="mb-6">
        <Label className="mb-2 block text-sm font-medium">Share with all org members as</Label>
        <Select
          value={authorization.shareWithOrgDefaultRole ?? 'NONE'}
          onValueChange={handleShareChange}
        >
          <SelectTrigger className="w-64">
            <SelectValue />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="NONE">Not shared</SelectItem>
            {SHARE_OPTIONS.map((r) => (
              <SelectItem key={r} value={r}>{r}</SelectItem>
            ))}
          </SelectContent>
        </Select>
        <p className="mt-1 text-xs text-muted-foreground">
          Every active member of your organization will get this role unless overridden by an explicit grant below.
        </p>
      </section>

      <section className="mb-6">
        <h3 className="mb-2 text-sm font-medium">Add a person</h3>
        <div className="flex items-end gap-2">
          <div className="flex-1">
            <UserPicker
              value={pickerValue}
              onChange={setPickerValue}
              members={members}
              loading={loadingMembers}
              excludeUserIds={grantedUserIds}
            />
          </div>
          <Select value={pickerRole} onValueChange={(v) => setPickerRole(v as AuthorizationRole)}>
            <SelectTrigger className="w-40">
              <SelectValue />
            </SelectTrigger>
            <SelectContent>
              {ROLE_OPTIONS.map((r) => (
                <SelectItem key={r} value={r}>{r}</SelectItem>
              ))}
            </SelectContent>
          </Select>
          <Button onClick={handleAdd} disabled={pickerValue == null || adding}>
            {adding ? <Loader2 className="h-4 w-4 animate-spin" /> : <Plus className="h-4 w-4" />}
            Add
          </Button>
        </div>
      </section>

      <section>
        <h3 className="mb-2 text-sm font-medium">People with access</h3>
        {loadingGrants ? (
          <div className="py-4 text-center text-sm text-muted-foreground">Loading…</div>
        ) : grants.length === 0 ? (
          <p className="text-sm text-muted-foreground">
            No explicit grants yet. {authorization.shareWithOrgDefaultRole
              ? `Org-wide ${authorization.shareWithOrgDefaultRole} sharing is active above.`
              : 'Only the creator and org admins can see this authorization.'}
          </p>
        ) : (
          <div className="divide-y">
            {grants.map((g) => (
              <div key={g.id} className="flex items-center justify-between py-2">
                <div className="flex flex-col">
                  <span className="text-sm font-medium">
                    {`${g.firstName ?? ''} ${g.lastName ?? ''}`.trim() || g.username}
                  </span>
                  <span className="text-xs text-muted-foreground">{g.email}</span>
                </div>
                <div className="flex items-center gap-2">
                  <Select value={g.role} onValueChange={(v) => handleRoleChange(g.id, v as AuthorizationRole)}>
                    <SelectTrigger className="w-36">
                      <SelectValue />
                    </SelectTrigger>
                    <SelectContent>
                      {ROLE_OPTIONS.map((r) => (
                        <SelectItem key={r} value={r}>{r}</SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                  <Button
                    variant="ghost"
                    size="icon"
                    onClick={() => handleRemove(g.id)}
                    aria-label={`Remove ${g.username}`}
                  >
                    <Trash2 className="h-4 w-4" />
                  </Button>
                </div>
              </div>
            ))}
          </div>
        )}
      </section>
    </Card>
  );
}
