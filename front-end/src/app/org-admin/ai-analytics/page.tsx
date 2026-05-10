'use client';

import { useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import { ChevronLeft, Loader2 } from 'lucide-react';
import { aiClient, AiSessionSummary, AiUsageTotals, AiSessionStatus } from '@/lib/ai-client';
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { SessionDetailDrawer } from '@/components/ai/SessionDetailDrawer';
import { HelpButton } from '@/components/HelpButton';

// ─── helpers ───────────────────────────────────────────────────────────────

function microsToUsd(micros: number): string {
  return `$${(micros / 1_000_000).toFixed(4)}`;
}

function StatusBadge({ status }: { status: AiSessionStatus }) {
  switch (status) {
    case 'COMPLETED':
      return <Badge variant="success">COMPLETED</Badge>;
    case 'FAILED':
      return <Badge variant="destructive">FAILED</Badge>;
    case 'RUNNING':
      return (
        <Badge variant="default">
          <span className="h-1.5 w-1.5 rounded-full bg-primary-foreground animate-pulse" />
          RUNNING
        </Badge>
      );
    case 'CANCELLED':
      return <Badge variant="secondary">CANCELLED</Badge>;
    case 'AWAITING_INPUT':
      return <Badge variant="warning">AWAITING INPUT</Badge>;
    default:
      return <Badge variant="outline">{status}</Badge>;
  }
}

// ─── page ──────────────────────────────────────────────────────────────────

export default function AiAnalyticsPage() {
  const router = useRouter();
  const [loading, setLoading] = useState(true);
  const [loadingMore, setLoadingMore] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [organizationId, setOrganizationId] = useState<number | null>(null);
  const [orgName, setOrgName] = useState<string>('');
  const [totals, setTotals] = useState<AiUsageTotals | null>(null);
  const [sessions, setSessions] = useState<AiSessionSummary[]>([]);
  const [offset, setOffset] = useState(0);
  const PAGE_SIZE = 20;
  const [drawerOpen, setDrawerOpen] = useState(false);
  const [selectedSessionId, setSelectedSessionId] = useState<string | null>(null);

  useEffect(() => {
    const storedUser = localStorage.getItem('user');
    if (!storedUser) {
      router.push('/login');
      return;
    }

    try {
      const userData = JSON.parse(storedUser);
      const isOrgAdmin = userData.orgRole === 'ORG_ADMIN';
      const isSuperAdmin = userData.globalRole === 'SUPER_ADMIN';

      if (!isOrgAdmin && !isSuperAdmin) {
        router.push('/');
        return;
      }

      const currentOrg = localStorage.getItem('currentOrganization');
      let orgId: number | null = null;
      if (currentOrg) {
        const parsed = JSON.parse(currentOrg);
        orgId = parsed.id;
        setOrgName(parsed.name ?? '');
      } else if (userData.organizationId) {
        orgId = userData.organizationId;
        setOrgName(userData.organizationName ?? '');
      }

      if (!orgId) {
        setError('No organization selected');
        setLoading(false);
        return;
      }

      setOrganizationId(orgId);
      loadData(orgId, 0);
    } catch {
      setError('Failed to load user data. Please try logging in again.');
      setLoading(false);
    }
  }, [router]);

  const loadData = async (orgId: number, newOffset: number) => {
    try {
      if (newOffset === 0) setLoading(true);
      else setLoadingMore(true);
      setError(null);

      const [totalsData, sessionsData] = await Promise.all([
        newOffset === 0 ? aiClient.getUsageTotals(orgId) : Promise.resolve(totals!),
        aiClient.listSessions(orgId, PAGE_SIZE, newOffset),
      ]);

      if (newOffset === 0) {
        setTotals(totalsData);
        setSessions(sessionsData);
      } else {
        setSessions((prev) => [...prev, ...sessionsData]);
      }
      setOffset(newOffset);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load AI analytics');
    } finally {
      setLoading(false);
      setLoadingMore(false);
    }
  };

  const handleLoadMore = () => {
    if (!organizationId) return;
    loadData(organizationId, offset + PAGE_SIZE);
  };

  if (loading) {
    return (
      <div className="min-h-screen bg-background flex items-center justify-center">
        <Loader2 className="h-8 w-8 animate-spin text-primary" />
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-background">
      <div className="container mx-auto py-12 px-4">
        <Button
          variant="ghost"
          onClick={() => router.push('/org-admin')}
          className="inline-flex items-center gap-2 text-sm text-muted-foreground hover:text-foreground mb-4 transition-colors px-0"
        >
          <ChevronLeft className="h-4 w-4" />
          Back to Dashboard
        </Button>

        <div className="mb-8">
          <div className="flex items-center gap-2">
            <h1 className="text-4xl font-bold">AI Usage Analytics</h1>
            <HelpButton slug="org-admin-ai-analytics" />
          </div>
          {orgName && (
            <p className="text-muted-foreground mt-2">{orgName}</p>
          )}
        </div>

        {error && (
          <Card className="mb-6 border-destructive/40 bg-destructive/10">
            <CardContent className="text-sm text-destructive">{error}</CardContent>
          </Card>
        )}

        {totals && (
          <div className="grid grid-cols-1 sm:grid-cols-3 gap-4 mb-8">
            <Card>
              <CardHeader>
                <CardDescription>Total Sessions</CardDescription>
                <CardTitle className="text-3xl tabular-nums">
                  {totals.totalSessions.toLocaleString()}
                </CardTitle>
              </CardHeader>
              <CardContent className="text-xs text-muted-foreground">
                This month: {totals.sessionsThisMonth.toLocaleString()}
              </CardContent>
            </Card>

            <Card>
              <CardHeader>
                <CardDescription>Total Tokens</CardDescription>
                <CardTitle className="text-3xl tabular-nums">
                  {(totals.totalTokensIn + totals.totalTokensOut).toLocaleString()}
                </CardTitle>
              </CardHeader>
              <CardContent className="text-xs text-muted-foreground">
                In: {totals.totalTokensIn.toLocaleString()} / Out: {totals.totalTokensOut.toLocaleString()}
              </CardContent>
            </Card>

            <Card>
              <CardHeader>
                <CardDescription>Total Cost</CardDescription>
                <CardTitle className="text-3xl tabular-nums">
                  {microsToUsd(totals.totalCostUsdMicros)}
                </CardTitle>
              </CardHeader>
              <CardContent className="text-xs text-muted-foreground">
                This month: {microsToUsd(totals.costThisMonthUsdMicros)}
              </CardContent>
            </Card>
          </div>
        )}

        <Card>
          <CardHeader>
            <CardTitle>Sessions</CardTitle>
          </CardHeader>
          <CardContent className="px-0">
            {sessions.length === 0 && !error ? (
              <div className="px-6 py-12 text-center text-sm text-muted-foreground">
                No AI sessions found for this organization.
              </div>
            ) : (
              <div className="overflow-x-auto">
                <table className="w-full text-sm">
                  <thead>
                    <tr className="border-y border-border bg-muted/40">
                      <th className="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-muted-foreground">Started</th>
                      <th className="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-muted-foreground">User</th>
                      <th className="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-muted-foreground">Wizard</th>
                      <th className="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-muted-foreground">Status</th>
                      <th className="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-muted-foreground">Model</th>
                      <th className="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-muted-foreground">Tokens (in / out)</th>
                      <th className="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-muted-foreground">Cost</th>
                      <th className="px-6 py-3 text-right text-xs font-medium uppercase tracking-wider text-muted-foreground">Action</th>
                    </tr>
                  </thead>
                  <tbody>
                    {sessions.map((session, i) => (
                      <tr
                        key={session.id}
                        className={
                          'transition-colors hover:bg-muted/40 ' +
                          (i < sessions.length - 1 ? 'border-b border-border' : '')
                        }
                      >
                        <td className="px-6 py-3 whitespace-nowrap tabular-nums text-foreground">
                          {new Date(session.startedAt).toLocaleString()}
                        </td>
                        <td className="px-6 py-3 whitespace-nowrap text-foreground">
                          {session.username ?? `#${session.userId}`}
                        </td>
                        <td className="px-6 py-3 whitespace-nowrap text-foreground">
                          {session.wizardKind}
                        </td>
                        <td className="px-6 py-3 whitespace-nowrap">
                          <StatusBadge status={session.status} />
                        </td>
                        <td className="px-6 py-3 whitespace-nowrap font-mono text-xs text-muted-foreground">
                          {session.model}
                        </td>
                        <td className="px-6 py-3 whitespace-nowrap tabular-nums text-foreground">
                          {session.tokensIn.toLocaleString()} / {session.tokensOut.toLocaleString()}
                        </td>
                        <td className="px-6 py-3 whitespace-nowrap tabular-nums text-foreground">
                          {microsToUsd(session.costUsdMicros)}
                        </td>
                        <td className="px-6 py-3 whitespace-nowrap text-right">
                          <Button
                            variant="outline"
                            size="sm"
                            onClick={() => {
                              setSelectedSessionId(session.id);
                              setDrawerOpen(true);
                            }}
                          >
                            View
                          </Button>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}

            {sessions.length > 0 && sessions.length % PAGE_SIZE === 0 && (
              <div className="px-6 py-4 border-t border-border flex justify-center">
                <Button variant="outline" onClick={handleLoadMore} disabled={loadingMore}>
                  {loadingMore ? (
                    <>
                      <Loader2 className="h-4 w-4 animate-spin" />
                      Loading…
                    </>
                  ) : (
                    'Load more'
                  )}
                </Button>
              </div>
            )}
          </CardContent>
        </Card>
      </div>

      <SessionDetailDrawer
        open={drawerOpen}
        onOpenChange={setDrawerOpen}
        organizationId={organizationId}
        sessionId={selectedSessionId}
      />
    </div>
  );
}
