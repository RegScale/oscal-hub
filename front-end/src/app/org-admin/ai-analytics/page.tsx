'use client';

import { useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import { ChevronLeft, Loader2 } from 'lucide-react';
import { aiClient, AiSessionSummary, AiUsageTotals, AiSessionStatus } from '@/lib/ai-client';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';

// ─── helpers ───────────────────────────────────────────────────────────────

function microsToUsd(micros: number): string {
  return `$${(micros / 1_000_000).toFixed(4)}`;
}

function statusPill(status: AiSessionStatus) {
  const base = 'inline-flex items-center gap-1 px-2 py-0.5 text-xs font-medium rounded-full';
  switch (status) {
    case 'COMPLETED':
      return (
        <span className={`${base} bg-green-100 text-green-800 dark:bg-green-900 dark:text-green-200`}>
          COMPLETED
        </span>
      );
    case 'FAILED':
      return (
        <span className={`${base} bg-red-100 text-red-800 dark:bg-red-900 dark:text-red-200`}>
          FAILED
        </span>
      );
    case 'RUNNING':
      return (
        <span className={`${base} bg-indigo-100 text-indigo-800 dark:bg-indigo-900 dark:text-indigo-200`}>
          <span className="h-1.5 w-1.5 rounded-full bg-indigo-500 animate-pulse" />
          RUNNING
        </span>
      );
    case 'CANCELLED':
      return (
        <span className={`${base} bg-gray-100 text-gray-700 dark:bg-gray-700 dark:text-gray-300`}>
          CANCELLED
        </span>
      );
    case 'AWAITING_INPUT':
      return (
        <span className={`${base} bg-amber-100 text-amber-800 dark:bg-amber-900 dark:text-amber-200`}>
          AWAITING INPUT
        </span>
      );
    default:
      return (
        <span className={`${base} bg-gray-100 text-gray-700 dark:bg-gray-700 dark:text-gray-300`}>
          {status}
        </span>
      );
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

      // Resolve org id + name
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

  // ── loading state ──────────────────────────────────────────────────────────
  if (loading) {
    return (
      <div className="min-h-screen bg-gray-50 dark:bg-gray-900 flex items-center justify-center">
        <Loader2 className="h-8 w-8 animate-spin text-blue-500" />
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gray-50 dark:bg-gray-900 py-8 px-4 sm:px-6 lg:px-8">
      <div className="max-w-7xl mx-auto">

        {/* Back link */}
        <button
          onClick={() => router.push('/org-admin')}
          className="flex items-center text-gray-600 dark:text-gray-400 hover:text-gray-900 dark:hover:text-white mb-6"
        >
          <ChevronLeft className="h-4 w-4 mr-1" />
          Back to Dashboard
        </button>

        {/* Header */}
        <div className="mb-8">
          <h1 className="text-3xl font-bold text-gray-900 dark:text-white">AI Usage Analytics</h1>
          {orgName && (
            <p className="mt-1 text-gray-600 dark:text-gray-400">{orgName}</p>
          )}
        </div>

        {/* Error state */}
        {error && (
          <div className="bg-red-50 dark:bg-red-900/20 border border-red-200 dark:border-red-800 rounded-lg p-4 mb-6 text-red-700 dark:text-red-300">
            {error}
          </div>
        )}

        {/* Summary cards */}
        {totals && (
          <div className="grid grid-cols-1 sm:grid-cols-3 gap-4 mb-8">
            {/* Total Sessions */}
            <Card>
              <CardHeader className="pb-2">
                <CardTitle className="text-sm font-medium text-gray-500 dark:text-gray-400">
                  Total Sessions
                </CardTitle>
              </CardHeader>
              <CardContent>
                <p className="text-3xl font-bold text-gray-900 dark:text-white">
                  {totals.totalSessions.toLocaleString()}
                </p>
                <p className="mt-1 text-xs text-gray-500 dark:text-gray-400">
                  This month: {totals.sessionsThisMonth.toLocaleString()}
                </p>
              </CardContent>
            </Card>

            {/* Total Tokens */}
            <Card>
              <CardHeader className="pb-2">
                <CardTitle className="text-sm font-medium text-gray-500 dark:text-gray-400">
                  Total Tokens
                </CardTitle>
              </CardHeader>
              <CardContent>
                <p className="text-3xl font-bold text-gray-900 dark:text-white">
                  {(totals.totalTokensIn + totals.totalTokensOut).toLocaleString()}
                </p>
                <p className="mt-1 text-xs text-gray-500 dark:text-gray-400">
                  In: {totals.totalTokensIn.toLocaleString()} / Out: {totals.totalTokensOut.toLocaleString()}
                </p>
              </CardContent>
            </Card>

            {/* Total Cost */}
            <Card>
              <CardHeader className="pb-2">
                <CardTitle className="text-sm font-medium text-gray-500 dark:text-gray-400">
                  Total Cost
                </CardTitle>
              </CardHeader>
              <CardContent>
                <p className="text-3xl font-bold text-gray-900 dark:text-white">
                  {microsToUsd(totals.totalCostUsdMicros)}
                </p>
                <p className="mt-1 text-xs text-gray-500 dark:text-gray-400">
                  This month: {microsToUsd(totals.costThisMonthUsdMicros)}
                </p>
              </CardContent>
            </Card>
          </div>
        )}

        {/* Sessions table */}
        <div className="bg-white dark:bg-gray-800 rounded-lg shadow overflow-hidden">
          <div className="px-6 py-4 border-b border-gray-200 dark:border-gray-700">
            <h2 className="text-lg font-semibold text-gray-900 dark:text-white">Sessions</h2>
          </div>

          {sessions.length === 0 && !error ? (
            <div className="p-12 text-center text-gray-500 dark:text-gray-400">
              No AI sessions found for this organization.
            </div>
          ) : (
            <div className="overflow-x-auto">
              <table className="min-w-full divide-y divide-gray-200 dark:divide-gray-700">
                <thead className="bg-gray-50 dark:bg-gray-700">
                  <tr>
                    <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-300 uppercase tracking-wider">
                      Started
                    </th>
                    <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-300 uppercase tracking-wider">
                      User
                    </th>
                    <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-300 uppercase tracking-wider">
                      Wizard
                    </th>
                    <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-300 uppercase tracking-wider">
                      Status
                    </th>
                    <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-300 uppercase tracking-wider">
                      Model
                    </th>
                    <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-300 uppercase tracking-wider">
                      Tokens (in / out)
                    </th>
                    <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-300 uppercase tracking-wider">
                      Cost
                    </th>
                    <th className="px-4 py-3 text-right text-xs font-medium text-gray-500 dark:text-gray-300 uppercase tracking-wider">
                      Action
                    </th>
                  </tr>
                </thead>
                <tbody className="bg-white dark:bg-gray-800 divide-y divide-gray-200 dark:divide-gray-700">
                  {sessions.map((session) => (
                    <tr key={session.id} className="hover:bg-gray-50 dark:hover:bg-gray-750">
                      <td className="px-4 py-3 whitespace-nowrap text-sm text-gray-700 dark:text-gray-300">
                        {new Date(session.startedAt).toLocaleString()}
                      </td>
                      <td className="px-4 py-3 whitespace-nowrap text-sm text-gray-700 dark:text-gray-300">
                        {session.username ?? `#${session.userId}`}
                      </td>
                      <td className="px-4 py-3 whitespace-nowrap text-sm text-gray-700 dark:text-gray-300">
                        {session.wizardKind}
                      </td>
                      <td className="px-4 py-3 whitespace-nowrap">
                        {statusPill(session.status)}
                      </td>
                      <td className="px-4 py-3 whitespace-nowrap text-sm text-gray-500 dark:text-gray-400 font-mono">
                        {session.model}
                      </td>
                      <td className="px-4 py-3 whitespace-nowrap text-sm text-gray-700 dark:text-gray-300 tabular-nums">
                        {session.tokensIn.toLocaleString()} / {session.tokensOut.toLocaleString()}
                      </td>
                      <td className="px-4 py-3 whitespace-nowrap text-sm text-gray-700 dark:text-gray-300 tabular-nums">
                        {microsToUsd(session.costUsdMicros)}
                      </td>
                      <td className="px-4 py-3 whitespace-nowrap text-right">
                        <Button
                          variant="outline"
                          size="sm"
                          onClick={() => console.log(session.id)}
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

          {/* Load more */}
          {sessions.length > 0 && sessions.length % PAGE_SIZE === 0 && (
            <div className="px-6 py-4 border-t border-gray-200 dark:border-gray-700 flex justify-center">
              <Button
                variant="outline"
                onClick={handleLoadMore}
                disabled={loadingMore}
              >
                {loadingMore ? (
                  <>
                    <Loader2 className="h-4 w-4 animate-spin" />
                    Loading...
                  </>
                ) : (
                  'Load more'
                )}
              </Button>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
