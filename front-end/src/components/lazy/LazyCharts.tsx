'use client';

import dynamic from 'next/dynamic';
import { Loader2 } from 'lucide-react';

/**
 * Loading skeleton for charts.
 */
function ChartSkeleton({ height = 300 }: { height?: number }) {
  return (
    <div
      className="flex items-center justify-center bg-muted/30 border border-border rounded-md animate-pulse"
      style={{ height }}
    >
      <div className="flex flex-col items-center gap-2 text-muted-foreground">
        <Loader2 className="h-6 w-6 animate-spin" />
        <span className="text-sm">Loading chart...</span>
      </div>
    </div>
  );
}

/**
 * Lazy-loaded Recharts components.
 * These are code-split to reduce initial bundle size.
 */

export const LazyBarChart = dynamic(
  () => import('recharts').then((mod) => mod.BarChart),
  {
    loading: () => <ChartSkeleton />,
    ssr: false,
  }
);

export const LazyLineChart = dynamic(
  () => import('recharts').then((mod) => mod.LineChart),
  {
    loading: () => <ChartSkeleton />,
    ssr: false,
  }
);

export const LazyPieChart = dynamic(
  () => import('recharts').then((mod) => mod.PieChart),
  {
    loading: () => <ChartSkeleton />,
    ssr: false,
  }
);

export const LazyAreaChart = dynamic(
  () => import('recharts').then((mod) => mod.AreaChart),
  {
    loading: () => <ChartSkeleton />,
    ssr: false,
  }
);

export const LazyResponsiveContainer = dynamic(
  () => import('recharts').then((mod) => mod.ResponsiveContainer),
  {
    loading: () => <ChartSkeleton />,
    ssr: false,
  }
);

/**
 * Preload Recharts library in the background.
 */
export function preloadCharts() {
  import('recharts');
}

export default {
  LazyBarChart,
  LazyLineChart,
  LazyPieChart,
  LazyAreaChart,
  LazyResponsiveContainer,
};
