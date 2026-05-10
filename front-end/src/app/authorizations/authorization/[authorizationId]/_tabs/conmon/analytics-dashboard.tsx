'use client';

import { Card } from '@/components/ui/card';
import {
  LazyResponsiveContainer,
  LazyLineChart,
  LazyBarChart,
  LazyPieChart,
} from '@/components/lazy/LazyCharts';
import { Line, Bar, Pie, Cell, CartesianGrid, XAxis, YAxis, Tooltip, Legend } from 'recharts';
import type { ConMonAnalytics } from '@/types/oscal';

interface Props {
  analytics: ConMonAnalytics | null;
  loading: boolean;
}

const STATUS_COLORS: Record<string, string> = {
  Open: '#F59E0B',
  Closed: '#10B981',
  Unknown: '#9CA3AF',
};

const SEVERITY_COLORS: Record<string, string> = {
  Critical: '#7C2D12',
  High: '#EF4444',
  Moderate: '#F59E0B',
  Low: '#3B82F6',
  Unspecified: '#9CA3AF',
};

export function AnalyticsDashboard({ analytics, loading }: Props) {
  if (loading) {
    return <Card className="p-6 text-sm text-muted-foreground">Loading analytics…</Card>;
  }
  if (!analytics || analytics.openCountSeries.length === 0) {
    return (
      <Card className="p-6 text-sm text-muted-foreground">
        Upload a few snapshots to see trends and aging analytics here.
      </Card>
    );
  }

  return (
    <div className="grid grid-cols-1 gap-4 lg:grid-cols-2">
      <Card className="p-4">
        <h3 className="mb-2 text-sm font-semibold">Open POAM count over time</h3>
        <LazyResponsiveContainer width="100%" height={240}>
          <LazyLineChart data={analytics.openCountSeries}>
            <CartesianGrid strokeDasharray="3 3" stroke="#374151" opacity={0.3} />
            <XAxis dataKey="date" stroke="#9CA3AF" tick={{ fontSize: 11 }} />
            <YAxis stroke="#9CA3AF" tick={{ fontSize: 11 }} />
            <Tooltip contentStyle={tooltipStyle} />
            <Line type="monotone" dataKey="open" stroke="#F59E0B" strokeWidth={2} dot={false} />
            <Line type="monotone" dataKey="closed" stroke="#10B981" strokeWidth={2} dot={false} />
          </LazyLineChart>
        </LazyResponsiveContainer>
      </Card>

      <Card className="p-4">
        <h3 className="mb-2 text-sm font-semibold">Open POAMs by severity (current)</h3>
        {analytics.currentSeverityBreakdown.length === 0 ? (
          <p className="py-12 text-center text-sm text-muted-foreground">No open items in the latest snapshot.</p>
        ) : (
          <LazyResponsiveContainer width="100%" height={240}>
            <LazyPieChart>
              <Pie
                data={analytics.currentSeverityBreakdown}
                cx="50%" cy="50%"
                innerRadius={50} outerRadius={80}
                paddingAngle={2}
                dataKey="count"
                nameKey="label"
                label={(p: any) => `${p.label || ''} (${((Number(p.percent) || 0) * 100).toFixed(0)}%)`}
                labelLine={false}
              >
                {analytics.currentSeverityBreakdown.map((seg, i) => (
                  <Cell key={i} fill={SEVERITY_COLORS[seg.label] || '#6B7280'} />
                ))}
              </Pie>
              <Tooltip contentStyle={tooltipStyle} />
              <Legend />
            </LazyPieChart>
          </LazyResponsiveContainer>
        )}
      </Card>

      <Card className="p-4">
        <h3 className="mb-2 text-sm font-semibold">Current status breakdown</h3>
        <LazyResponsiveContainer width="100%" height={240}>
          <LazyPieChart>
            <Pie
              data={analytics.currentStatusBreakdown}
              cx="50%" cy="50%"
              innerRadius={50} outerRadius={80}
              paddingAngle={2}
              dataKey="count"
              nameKey="label"
              label={(p: any) => `${p.label || ''} (${((Number(p.percent) || 0) * 100).toFixed(0)}%)`}
              labelLine={false}
            >
              {analytics.currentStatusBreakdown.map((seg, i) => (
                <Cell key={i} fill={STATUS_COLORS[seg.label] || '#6B7280'} />
              ))}
            </Pie>
            <Tooltip contentStyle={tooltipStyle} />
          </LazyPieChart>
        </LazyResponsiveContainer>
      </Card>

      <Card className="p-4">
        <h3 className="mb-2 text-sm font-semibold">Aging — open items</h3>
        <LazyResponsiveContainer width="100%" height={240}>
          <LazyBarChart data={analytics.agingBuckets}>
            <CartesianGrid strokeDasharray="3 3" stroke="#374151" opacity={0.3} />
            <XAxis dataKey="bucket" stroke="#9CA3AF" tick={{ fontSize: 11 }} />
            <YAxis stroke="#9CA3AF" tick={{ fontSize: 11 }} />
            <Tooltip contentStyle={tooltipStyle} />
            <Bar dataKey="count" fill="#3B82F6" radius={[4, 4, 0, 0]} />
          </LazyBarChart>
        </LazyResponsiveContainer>
        {analytics.meanTimeToCloseDays != null && (
          <p className="mt-3 text-xs text-muted-foreground">
            Mean time to close: <span className="font-medium text-foreground">
              {analytics.meanTimeToCloseDays.toFixed(1)} days
            </span>
          </p>
        )}
      </Card>
    </div>
  );
}

const tooltipStyle = {
  backgroundColor: '#1F2937',
  border: '1px solid #374151',
  borderRadius: '8px',
  color: '#F9FAFB',
} as const;
