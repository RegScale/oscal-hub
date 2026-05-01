'use client';

import { useEffect, useState } from 'react';
import { useSearchParams, useRouter } from 'next/navigation';
import { apiClient } from '@/lib/api-client';
import { useAuth } from '@/contexts/AuthContext';
import { Card, CardHeader, CardTitle, CardContent } from '@/components/ui/card';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Button } from '@/components/ui/button';
import { Alert } from '@/components/ui/alert';

interface Invite {
  email: string;
  organizationName: string;
  inviterName: string;
}

export default function AcceptInvitePage() {
  const search = useSearchParams();
  const router = useRouter();
  const { user, isAuthenticated } = useAuth();
  const token = search.get('token') ?? '';

  const [invite, setInvite] = useState<Invite | null>(null);
  const [loadError, setLoadError] = useState('');
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [submitError, setSubmitError] = useState('');
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    if (!token) { setLoadError('Missing invitation token.'); return; }
    apiClient.getInvitation(token)
      .then((d) => setInvite(d as Invite))
      .catch((e: any) => {
        if (e?.status === 410) setLoadError('This invitation is no longer valid.');
        else if (e?.status === 404) setLoadError('Invitation not found.');
        else setLoadError('Failed to load invitation.');
      });
  }, [token]);

  if (loadError) {
    return (
      <div className="max-w-md mx-auto mt-12">
        <Card>
          <CardHeader><CardTitle>Invitation</CardTitle></CardHeader>
          <CardContent><p className="text-sm text-muted-foreground">{loadError}</p></CardContent>
        </Card>
      </div>
    );
  }

  if (!invite) {
    return (
      <div className="max-w-md mx-auto mt-12">
        <Card><CardContent className="p-6">Loading…</CardContent></Card>
      </div>
    );
  }

  const handleAccept = async () => {
    setSubmitting(true);
    setSubmitError('');
    try {
      const body = isAuthenticated ? {} : { username, password };
      const result = await apiClient.acceptInvitation(token, body);
      // For new users the backend returns a JWT — store it so AuthContext picks it up.
      if (!isAuthenticated && result?.token) {
        localStorage.setItem('token', result.token);
        // Hard-reload so AuthContext re-reads localStorage and hydrates the session.
        window.location.href = '/';
        return;
      }
      router.push('/');
    } catch (e: any) {
      setSubmitError(e?.message || 'Failed to accept invitation.');
      setSubmitting(false);
    }
  };

  return (
    <div className="max-w-md mx-auto mt-12">
      <Card>
        <CardHeader>
          <CardTitle>You&apos;re invited to {invite.organizationName}</CardTitle>
        </CardHeader>
        <CardContent className="space-y-4">
          <p className="text-sm">{invite.inviterName} invited <strong>{invite.email}</strong>.</p>
          {submitError && <Alert variant="destructive"><p className="text-sm">{submitError}</p></Alert>}

          {isAuthenticated ? (
            <>
              <p className="text-sm text-muted-foreground">
                You&apos;re signed in as {user?.username}. Click accept to join.
              </p>
              <Button onClick={handleAccept} disabled={submitting}>
                {submitting ? 'Accepting…' : 'Accept invitation'}
              </Button>
            </>
          ) : (
            <>
              <div className="space-y-2">
                <Label htmlFor="email">Email</Label>
                <Input id="email" type="email" value={invite.email} readOnly />
              </div>
              <div className="space-y-2">
                <Label htmlFor="username">Username</Label>
                <Input id="username" value={username} onChange={(e) => setUsername(e.target.value)} />
              </div>
              <div className="space-y-2">
                <Label htmlFor="password">Password</Label>
                <Input id="password" type="password" value={password} onChange={(e) => setPassword(e.target.value)} />
              </div>
              <Button onClick={handleAccept} disabled={submitting || !username || !password}>
                {submitting ? 'Accepting…' : 'Accept'}
              </Button>
              <p className="text-xs text-muted-foreground">
                Already have an account?{' '}
                <a
                  href={`/login?next=${encodeURIComponent(`/accept-invite?token=${token}`)}`}
                  className="underline"
                >
                  Sign in
                </a>
              </p>
            </>
          )}
        </CardContent>
      </Card>
    </div>
  );
}
