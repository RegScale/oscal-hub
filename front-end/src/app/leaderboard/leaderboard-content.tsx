'use client';

import { useCallback, useEffect, useState } from 'react';
import { Award, Medal, RefreshCw, Trophy, Users } from 'lucide-react';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Tabs, TabsList, TabsTrigger } from '@/components/ui/tabs';
import { apiClient } from '@/lib/api-client';
import { useAuth } from '@/contexts/AuthContext';
import type { LeaderboardEntry, LeaderboardResponse, LeaderboardWindow } from '@/types/oscal';

const BREAKDOWN_LABELS: Record<string, [singular: string, plural: string]> = {
  operations: ['operation', 'operations'],
  libraryPublishes: ['library publish', 'library publishes'],
  artifacts: ['artifact', 'artifacts'],
  documents: ['document', 'documents'],
  authorizations: ['authorization', 'authorizations'],
};

function breakdownText(breakdown?: Record<string, number> | null): string | null {
  if (!breakdown) return null;
  const parts = Object.entries(breakdown)
    .filter(([, count]) => count > 0)
    .map(([source, count]) => {
      const labels = BREAKDOWN_LABELS[source];
      const label = labels ? labels[count === 1 ? 0 : 1] : source;
      return `${count} ${label}`;
    });
  return parts.length > 0 ? parts.join(' · ') : null;
}

function RankBadge({ rank }: { rank: number }) {
  if (rank === 1) {
    return <Trophy data-testid="medal-1" aria-label="1st place" className="h-5 w-5 text-yellow-500" />;
  }
  if (rank === 2) {
    return <Medal data-testid="medal-2" aria-label="2nd place" className="h-5 w-5 text-slate-400" />;
  }
  if (rank === 3) {
    return <Award data-testid="medal-3" aria-label="3rd place" className="h-5 w-5 text-amber-700" />;
  }
  return (
    <span data-testid="rank-number" className="text-sm font-medium text-muted-foreground">
      {rank}
    </span>
  );
}

function BoardRows({
  entries,
  currentUsername,
  emptyMessage,
  showBreakdown,
}: {
  entries: LeaderboardEntry[];
  currentUsername?: string;
  emptyMessage: string;
  showBreakdown: boolean;
}) {
  if (entries.length === 0) {
    return <p className="py-8 text-center text-sm text-muted-foreground">{emptyMessage}</p>;
  }

  return (
    <ol className="divide-y">
      {entries.map((entry) => {
        const isSelf = currentUsername != null && entry.username === currentUsername;
        const breakdown = showBreakdown ? breakdownText(entry.breakdown) : null;
        return (
          <li
            key={entry.username}
            className={`flex items-center gap-3 px-2 py-2.5 ${isSelf ? 'rounded-md bg-primary/10' : ''}`}
          >
            <span className="flex w-8 shrink-0 items-center justify-center">
              <RankBadge rank={entry.rank} />
            </span>
            <span className="min-w-0 flex-1">
              <span className="flex items-center gap-2">
                <span className="truncate text-sm font-medium">{entry.displayName}</span>
                {isSelf && <Badge variant="secondary">You</Badge>}
              </span>
              {breakdown && (
                <span className="block truncate text-xs text-muted-foreground">{breakdown}</span>
              )}
            </span>
            <span className="shrink-0 text-base font-semibold tabular-nums">{entry.score}</span>
          </li>
        );
      })}
    </ol>
  );
}

function BoardSkeleton() {
  return (
    <div className="space-y-3 py-2" aria-hidden="true">
      {Array.from({ length: 5 }).map((_, i) => (
        <div key={i} className="flex items-center gap-3">
          <div className="h-5 w-8 animate-pulse rounded bg-muted" />
          <div className="h-5 flex-1 animate-pulse rounded bg-muted" />
          <div className="h-5 w-10 animate-pulse rounded bg-muted" />
        </div>
      ))}
    </div>
  );
}

export function LeaderboardContent() {
  const { user } = useAuth();
  const [timeWindow, setTimeWindow] = useState<LeaderboardWindow>('all');
  const [data, setData] = useState<LeaderboardResponse | null>(null);
  const [error, setError] = useState<string | null>(null);
  // Bumped by the Retry button to re-run the fetch effect after a failure.
  const [retryToken, setRetryToken] = useState(0);

  // Loading is derived: no error and no data yet for the selected window.
  // Keeps the fetch effect free of synchronous setState (lint: react-hooks).
  const isLoading = !error && (data === null || data.window !== timeWindow);

  useEffect(() => {
    let cancelled = false;
    apiClient
      .getLeaderboard(timeWindow)
      .then((response) => {
        if (!cancelled) {
          setData(response);
          setError(null);
        }
      })
      .catch((e: unknown) => {
        console.error('Failed to load leaderboard:', e);
        if (!cancelled) {
          setError(e instanceof Error ? e.message : 'Failed to load leaderboard');
        }
      });
    return () => {
      cancelled = true;
    };
  }, [timeWindow, retryToken]);

  const retry = useCallback(() => {
    setError(null);
    setRetryToken((token) => token + 1);
  }, []);

  return (
    <div className="container mx-auto max-w-5xl px-4 py-8" data-tour="leaderboard-page">
      <div className="mb-6 flex items-center justify-between">
        <div>
          <h1 className="flex items-center gap-2 text-2xl font-bold">
            <Trophy className="h-6 w-6 text-yellow-500" />
            Leaderboard
          </h1>
          <p className="text-sm text-muted-foreground">
            Who&apos;s doing the most in OSCAL Hub — and who&apos;s sharing the most with everyone else.
          </p>
        </div>
        <Tabs value={timeWindow} onValueChange={(v) => setTimeWindow(v as LeaderboardWindow)}>
          <TabsList>
            <TabsTrigger value="30d">Last 30 days</TabsTrigger>
            <TabsTrigger value="all">All time</TabsTrigger>
          </TabsList>
        </Tabs>
      </div>

      {error ? (
        <Card>
          <CardContent className="flex flex-col items-center gap-3 py-10">
            <p className="text-sm text-muted-foreground">{error}</p>
            <Button variant="outline" onClick={retry}>
              <RefreshCw className="mr-2 h-4 w-4" />
              Try again
            </Button>
          </CardContent>
        </Card>
      ) : (
        <div className="grid gap-6 md:grid-cols-2">
          <Card data-testid="board-most-active" data-board="most-active">
            <CardHeader className="pb-2">
              <CardTitle className="flex items-center gap-2 text-lg">
                <Users className="h-5 w-5 text-blue-500" />
                Most Active Users
              </CardTitle>
              <p className="text-xs text-muted-foreground">
                Operations run, content built, and items published across the platform.
              </p>
            </CardHeader>
            <CardContent>
              {isLoading ? (
                <BoardSkeleton />
              ) : (
                <BoardRows
                  entries={data?.mostActive ?? []}
                  currentUsername={user?.username}
                  emptyMessage="No activity yet — go validate something!"
                  showBreakdown
                />
              )}
            </CardContent>
          </Card>

          <Card data-testid="board-top-contributors" data-board="top-contributors">
            <CardHeader className="pb-2">
              <CardTitle className="flex items-center gap-2 text-lg">
                <Award className="h-5 w-5 text-purple-500" />
                Top Contributors
              </CardTitle>
              <p className="text-xs text-muted-foreground">
                Items shared into the library for others to reuse.
              </p>
            </CardHeader>
            <CardContent>
              {isLoading ? (
                <BoardSkeleton />
              ) : (
                <BoardRows
                  entries={data?.topContributors ?? []}
                  currentUsername={user?.username}
                  emptyMessage="No activity yet — share something to the library!"
                  showBreakdown={false}
                />
              )}
            </CardContent>
          </Card>
        </div>
      )}
    </div>
  );
}
