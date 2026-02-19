'use client';

import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { apiClient } from '@/lib/api-client';
import { queryKeys, STALE_TIMES, CACHE_TIMES } from '@/lib/query-client';

/**
 * Custom hooks for data fetching with React Query.
 * These hooks provide automatic caching, background refetching,
 * and request deduplication.
 */

// ==================== Library Hooks ====================

/**
 * Fetch paginated library items with caching
 */
export function useLibraryItems(page = 0, size = 20, sortBy = 'updatedAt', sortDir = 'desc') {
  return useQuery({
    queryKey: queryKeys.library.list({ page, size, sortBy, sortDir }),
    queryFn: async () => {
      const response = await fetch(
        `${process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080'}/api/library?page=${page}&size=${size}&sortBy=${sortBy}&sortDir=${sortDir}`,
        {
          headers: {
            'Authorization': `Bearer ${localStorage.getItem('token')}`,
          },
        }
      );
      if (!response.ok) throw new Error('Failed to fetch library items');
      return response.json();
    },
    staleTime: STALE_TIMES.lists,
    gcTime: CACHE_TIMES.lists,
  });
}

/**
 * Search library items
 */
export function useLibrarySearch(query: string, oscalType?: string, tag?: string) {
  return useQuery({
    queryKey: queryKeys.library.search(query, oscalType, tag),
    queryFn: async () => {
      const params = new URLSearchParams();
      if (query) params.set('q', query);
      if (oscalType) params.set('oscalType', oscalType);
      if (tag) params.set('tag', tag);

      const response = await fetch(
        `${process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080'}/api/library/search?${params}`,
        {
          headers: {
            'Authorization': `Bearer ${localStorage.getItem('token')}`,
          },
        }
      );
      if (!response.ok) throw new Error('Failed to search library');
      return response.json();
    },
    enabled: Boolean(query || oscalType || tag),
    staleTime: STALE_TIMES.lists,
  });
}

/**
 * Fetch library analytics with caching
 */
export function useLibraryAnalytics() {
  return useQuery({
    queryKey: queryKeys.library.analytics(),
    queryFn: async () => {
      const response = await fetch(
        `${process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080'}/api/library/analytics`,
        {
          headers: {
            'Authorization': `Bearer ${localStorage.getItem('token')}`,
          },
        }
      );
      if (!response.ok) throw new Error('Failed to fetch library analytics');
      return response.json();
    },
    staleTime: STALE_TIMES.analytics,
    gcTime: CACHE_TIMES.analytics,
  });
}

/**
 * Fetch library tags with long cache
 */
export function useLibraryTags() {
  return useQuery({
    queryKey: queryKeys.library.tags(),
    queryFn: async () => {
      const response = await fetch(
        `${process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080'}/api/library/tags`,
        {
          headers: {
            'Authorization': `Bearer ${localStorage.getItem('token')}`,
          },
        }
      );
      if (!response.ok) throw new Error('Failed to fetch tags');
      return response.json();
    },
    staleTime: STALE_TIMES.tags,
    gcTime: CACHE_TIMES.tags,
  });
}

// ==================== Artifact Hooks ====================

/**
 * Fetch paginated artifacts with caching
 */
export function useArtifacts(page = 0, size = 20, sortBy = 'updatedAt', sortDir = 'desc') {
  return useQuery({
    queryKey: queryKeys.artifacts.list({ page, size, sortBy, sortDir }),
    queryFn: async () => {
      const response = await fetch(
        `${process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080'}/api/artifacts?page=${page}&size=${size}&sortBy=${sortBy}&sortDir=${sortDir}`,
        {
          headers: {
            'Authorization': `Bearer ${localStorage.getItem('token')}`,
          },
        }
      );
      if (!response.ok) throw new Error('Failed to fetch artifacts');
      return response.json();
    },
    staleTime: STALE_TIMES.lists,
    gcTime: CACHE_TIMES.lists,
  });
}

/**
 * Fetch single artifact with caching
 */
export function useArtifact(artifactId: string) {
  return useQuery({
    queryKey: queryKeys.artifacts.detail(artifactId),
    queryFn: async () => {
      const response = await fetch(
        `${process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080'}/api/artifacts/${artifactId}`,
        {
          headers: {
            'Authorization': `Bearer ${localStorage.getItem('token')}`,
          },
        }
      );
      if (!response.ok) throw new Error('Failed to fetch artifact');
      return response.json();
    },
    enabled: Boolean(artifactId),
  });
}

// ==================== Analytics Hooks ====================

/**
 * Fetch dashboard analytics with caching
 */
export function useDashboardAnalytics() {
  return useQuery({
    queryKey: queryKeys.analytics.dashboard(),
    queryFn: async () => {
      const response = await fetch(
        `${process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080'}/api/admin/analytics`,
        {
          headers: {
            'Authorization': `Bearer ${localStorage.getItem('token')}`,
          },
        }
      );
      if (!response.ok) throw new Error('Failed to fetch analytics');
      return response.json();
    },
    staleTime: STALE_TIMES.analytics,
    gcTime: CACHE_TIMES.analytics,
  });
}

/**
 * Fetch summary stats with caching
 */
export function useSummaryStats() {
  return useQuery({
    queryKey: queryKeys.analytics.summary(),
    queryFn: async () => {
      const response = await fetch(
        `${process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080'}/api/admin/analytics/summary`,
        {
          headers: {
            'Authorization': `Bearer ${localStorage.getItem('token')}`,
          },
        }
      );
      if (!response.ok) throw new Error('Failed to fetch summary');
      return response.json();
    },
    staleTime: STALE_TIMES.analytics,
    gcTime: CACHE_TIMES.analytics,
  });
}

// ==================== History Hooks ====================

/**
 * Fetch operation history with pagination
 */
export function useOperationHistory(page = 0, size = 20) {
  return useQuery({
    queryKey: queryKeys.history.list(page, size),
    queryFn: () => apiClient.getOperationHistory(page, size),
    staleTime: STALE_TIMES.lists,
  });
}

/**
 * Fetch operation stats
 */
export function useOperationStats() {
  return useQuery({
    queryKey: queryKeys.history.stats(),
    queryFn: () => apiClient.getOperationStats(),
    staleTime: STALE_TIMES.analytics,
    gcTime: CACHE_TIMES.analytics,
  });
}

// ==================== Health Hooks ====================

/**
 * Health ping check with short cache
 */
export function useHealthPing() {
  return useQuery({
    queryKey: queryKeys.health.ping(),
    queryFn: async () => {
      const response = await fetch(
        `${process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080'}/api/health/ping`
      );
      return response.ok;
    },
    staleTime: STALE_TIMES.health,
    gcTime: CACHE_TIMES.health,
    refetchInterval: 30000, // Refetch every 30 seconds
  });
}

/**
 * Detailed health check
 */
export function useDetailedHealth() {
  return useQuery({
    queryKey: queryKeys.health.detailed(),
    queryFn: async () => {
      const response = await fetch(
        `${process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080'}/api/health/detailed`,
        {
          headers: {
            'Authorization': `Bearer ${localStorage.getItem('token')}`,
          },
        }
      );
      if (!response.ok) throw new Error('Failed to fetch health details');
      return response.json();
    },
    staleTime: STALE_TIMES.health,
    gcTime: CACHE_TIMES.health,
  });
}

// ==================== Organization Hooks ====================

/**
 * Fetch organizations summary (Super Admin)
 */
export function useOrganizationsSummary() {
  return useQuery({
    queryKey: queryKeys.organizations.summary(),
    queryFn: async () => {
      const response = await fetch(
        `${process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080'}/api/admin/organizations/summary`,
        {
          headers: {
            'Authorization': `Bearer ${localStorage.getItem('token')}`,
          },
        }
      );
      if (!response.ok) throw new Error('Failed to fetch organizations summary');
      return response.json();
    },
    staleTime: STALE_TIMES.lists,
  });
}

// ==================== Mutation Hooks ====================

/**
 * Invalidate queries after mutations
 */
export function useInvalidateQueries() {
  const queryClient = useQueryClient();

  return {
    invalidateLibrary: () => queryClient.invalidateQueries({ queryKey: queryKeys.library.all }),
    invalidateArtifacts: () => queryClient.invalidateQueries({ queryKey: queryKeys.artifacts.all }),
    invalidateAnalytics: () => queryClient.invalidateQueries({ queryKey: queryKeys.analytics.all }),
    invalidateOrganizations: () => queryClient.invalidateQueries({ queryKey: queryKeys.organizations.all }),
    invalidateHistory: () => queryClient.invalidateQueries({ queryKey: queryKeys.history.all }),
    invalidateAll: () => queryClient.invalidateQueries(),
  };
}
