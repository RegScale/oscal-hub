'use client';

import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { useState, type ReactNode } from 'react';

/**
 * Default stale times for different types of data.
 * Stale data will be refetched in the background when requested.
 */
export const STALE_TIMES = {
  /** Analytics data - 5 minutes */
  analytics: 5 * 60 * 1000,
  /** Library/artifact lists - 2 minutes */
  lists: 2 * 60 * 1000,
  /** Tags - 10 minutes (rarely change) */
  tags: 10 * 60 * 1000,
  /** User profile - 5 minutes */
  profile: 5 * 60 * 1000,
  /** Health checks - 30 seconds */
  health: 30 * 1000,
  /** Static content like SOC 2 controls - 1 hour */
  static: 60 * 60 * 1000,
} as const;

/**
 * Default cache times for different types of data.
 * Cached data will be garbage collected after this time.
 */
export const CACHE_TIMES = {
  /** Analytics data - 10 minutes */
  analytics: 10 * 60 * 1000,
  /** Library/artifact lists - 5 minutes */
  lists: 5 * 60 * 1000,
  /** Tags - 30 minutes */
  tags: 30 * 60 * 1000,
  /** User profile - 10 minutes */
  profile: 10 * 60 * 1000,
  /** Health checks - 1 minute */
  health: 60 * 1000,
  /** Static content - 2 hours */
  static: 2 * 60 * 60 * 1000,
} as const;

/**
 * Create a configured QueryClient with optimized defaults
 */
function makeQueryClient() {
  return new QueryClient({
    defaultOptions: {
      queries: {
        // Default stale time - data considered fresh for 2 minutes
        staleTime: STALE_TIMES.lists,
        // Default cache time - data kept in cache for 5 minutes
        gcTime: CACHE_TIMES.lists,
        // Retry failed requests up to 3 times with exponential backoff
        retry: 3,
        retryDelay: (attemptIndex) => Math.min(1000 * 2 ** attemptIndex, 30000),
        // Refetch on window focus for real-time data feel
        refetchOnWindowFocus: true,
        // Don't refetch on mount if data is fresh
        refetchOnMount: true,
        // Refetch on reconnect after network loss
        refetchOnReconnect: true,
      },
      mutations: {
        // Retry mutations once
        retry: 1,
      },
    },
  });
}

// Browser: Store QueryClient in a variable so it's reused across re-renders
let browserQueryClient: QueryClient | undefined = undefined;

function getQueryClient() {
  if (typeof window === 'undefined') {
    // Server: always make a new query client
    return makeQueryClient();
  } else {
    // Browser: make a new query client if we don't already have one
    if (!browserQueryClient) {
      browserQueryClient = makeQueryClient();
    }
    return browserQueryClient;
  }
}

interface QueryProviderProps {
  children: ReactNode;
}

/**
 * React Query Provider for the application.
 * Provides caching, background refetching, and request deduplication.
 */
export function QueryProvider({ children }: QueryProviderProps) {
  // Using useState to ensure the client is only created once on the client
  const [queryClient] = useState(() => getQueryClient());

  return (
    <QueryClientProvider client={queryClient}>
      {children}
    </QueryClientProvider>
  );
}

/**
 * Query key factory for consistent cache key management.
 * Using factory pattern ensures consistent keys across the app.
 */
export const queryKeys = {
  // Artifacts
  artifacts: {
    all: ['artifacts'] as const,
    lists: () => [...queryKeys.artifacts.all, 'list'] as const,
    list: (filters: Record<string, unknown>) => [...queryKeys.artifacts.lists(), filters] as const,
    details: () => [...queryKeys.artifacts.all, 'detail'] as const,
    detail: (id: string) => [...queryKeys.artifacts.details(), id] as const,
    search: (query: string) => [...queryKeys.artifacts.all, 'search', query] as const,
    analytics: () => [...queryKeys.artifacts.all, 'analytics'] as const,
    tags: () => [...queryKeys.artifacts.all, 'tags'] as const,
  },

  // Library items
  library: {
    all: ['library'] as const,
    lists: () => [...queryKeys.library.all, 'list'] as const,
    list: (filters: Record<string, unknown>) => [...queryKeys.library.lists(), filters] as const,
    details: () => [...queryKeys.library.all, 'detail'] as const,
    detail: (id: string) => [...queryKeys.library.details(), id] as const,
    search: (query: string, type?: string, tag?: string) =>
      [...queryKeys.library.all, 'search', { query, type, tag }] as const,
    analytics: () => [...queryKeys.library.all, 'analytics'] as const,
    tags: () => [...queryKeys.library.all, 'tags'] as const,
    byType: (type: string) => [...queryKeys.library.all, 'type', type] as const,
  },

  // Authorizations
  authorizations: {
    all: ['authorizations'] as const,
    lists: () => [...queryKeys.authorizations.all, 'list'] as const,
    list: (filters: Record<string, unknown>) => [...queryKeys.authorizations.lists(), filters] as const,
    details: () => [...queryKeys.authorizations.all, 'detail'] as const,
    detail: (id: number) => [...queryKeys.authorizations.details(), id] as const,
    bySsp: (sspId: string) => [...queryKeys.authorizations.all, 'ssp', sspId] as const,
  },

  // Organizations
  organizations: {
    all: ['organizations'] as const,
    lists: () => [...queryKeys.organizations.all, 'list'] as const,
    summary: () => [...queryKeys.organizations.all, 'summary'] as const,
    details: () => [...queryKeys.organizations.all, 'detail'] as const,
    detail: (id: number) => [...queryKeys.organizations.details(), id] as const,
    members: (id: number) => [...queryKeys.organizations.detail(id), 'members'] as const,
  },

  // Users
  users: {
    all: ['users'] as const,
    current: () => [...queryKeys.users.all, 'current'] as const,
    profile: () => [...queryKeys.users.all, 'profile'] as const,
    lists: () => [...queryKeys.users.all, 'list'] as const,
  },

  // Analytics
  analytics: {
    all: ['analytics'] as const,
    dashboard: () => [...queryKeys.analytics.all, 'dashboard'] as const,
    summary: () => [...queryKeys.analytics.all, 'summary'] as const,
  },

  // History
  history: {
    all: ['history'] as const,
    lists: () => [...queryKeys.history.all, 'list'] as const,
    list: (page: number, size: number) => [...queryKeys.history.lists(), { page, size }] as const,
    stats: () => [...queryKeys.history.all, 'stats'] as const,
  },

  // Health
  health: {
    all: ['health'] as const,
    ping: () => [...queryKeys.health.all, 'ping'] as const,
    detailed: () => [...queryKeys.health.all, 'detailed'] as const,
    component: (name: string) => [...queryKeys.health.all, 'component', name] as const,
  },

  // Audit logs
  audit: {
    all: ['audit'] as const,
    logs: (filters: Record<string, unknown>) => [...queryKeys.audit.all, 'logs', filters] as const,
    stats: () => [...queryKeys.audit.all, 'stats'] as const,
  },
} as const;
