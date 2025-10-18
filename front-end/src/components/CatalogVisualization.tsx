'use client';

import { Card, CardHeader, CardTitle, CardContent } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, Cell } from 'recharts';
import type { CatalogAnalysis } from '@/lib/oscal-parser';

interface CatalogVisualizationProps {
  analysis: CatalogAnalysis;
}

const COLORS = [
  '#3b82f6', // blue-500
  '#8b5cf6', // violet-500
  '#ec4899', // pink-500
  '#f59e0b', // amber-500
  '#10b981', // emerald-500
  '#6366f1', // indigo-500
  '#f97316', // orange-500
  '#14b8a6', // teal-500
];

export function CatalogVisualization({ analysis }: CatalogVisualizationProps) {
  const { metadata, totalControls, families } = analysis;

  // Sort families by control count in descending order
  const sortedFamilies = [...families].sort((a, b) => b.controlCount - a.controlCount);

  // Prepare data for the bar chart
  const chartData = sortedFamilies.map(family => ({
    name: family.title.length > 20 ? `${family.title.substring(0, 20)}...` : family.title,
    fullName: family.title,
    count: family.controlCount,
  }));

  return (
    <div className="space-y-6">
      {/* Basic Information Card */}
      <Card>
        <CardHeader>
          <CardTitle>Catalog Information</CardTitle>
        </CardHeader>
        <CardContent>
          <div className="space-y-4">
            <div>
              <h3 className="text-2xl font-semibold mb-2">{metadata.title}</h3>
              <div className="flex flex-wrap gap-2">
                {metadata.version && (
                  <Badge variant="secondary">Version: {metadata.version}</Badge>
                )}
                {metadata.oscalVersion && (
                  <Badge variant="secondary">OSCAL: {metadata.oscalVersion}</Badge>
                )}
              </div>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-2 gap-4 pt-4 border-t">
              {metadata.lastModified && (
                <div>
                  <p className="text-sm text-muted-foreground">Last Modified</p>
                  <p className="font-medium">
                    {new Date(metadata.lastModified).toLocaleDateString(undefined, {
                      year: 'numeric',
                      month: 'long',
                      day: 'numeric',
                    })}
                  </p>
                </div>
              )}
              {metadata.published && (
                <div>
                  <p className="text-sm text-muted-foreground">Published</p>
                  <p className="font-medium">
                    {new Date(metadata.published).toLocaleDateString(undefined, {
                      year: 'numeric',
                      month: 'long',
                      day: 'numeric',
                    })}
                  </p>
                </div>
              )}
            </div>
          </div>
        </CardContent>
      </Card>

      {/* Controls Summary Card */}
      <Card>
        <CardHeader>
          <CardTitle>Controls Overview</CardTitle>
        </CardHeader>
        <CardContent>
          <div className="text-center py-8">
            <div className="text-6xl font-bold text-primary mb-2">{totalControls}</div>
            <p className="text-lg text-muted-foreground">Total Controls</p>
          </div>

          <div className="grid grid-cols-2 md:grid-cols-4 gap-4 pt-6 border-t">
            <div className="text-center">
              <div className="text-3xl font-semibold text-primary">{families.length}</div>
              <p className="text-sm text-muted-foreground">Control Families</p>
            </div>
            <div className="text-center">
              <div className="text-3xl font-semibold text-primary">
                {families.length > 0 ? Math.round(totalControls / families.length) : 0}
              </div>
              <p className="text-sm text-muted-foreground">Avg per Family</p>
            </div>
            <div className="text-center">
              <div className="text-3xl font-semibold text-primary">
                {sortedFamilies[0]?.controlCount || 0}
              </div>
              <p className="text-sm text-muted-foreground">Largest Family</p>
            </div>
            <div className="text-center">
              <div className="text-3xl font-semibold text-primary">
                {sortedFamilies[sortedFamilies.length - 1]?.controlCount || 0}
              </div>
              <p className="text-sm text-muted-foreground">Smallest Family</p>
            </div>
          </div>
        </CardContent>
      </Card>

      {/* Control Families Chart */}
      <Card>
        <CardHeader>
          <CardTitle>Controls by Family</CardTitle>
        </CardHeader>
        <CardContent>
          {chartData.length > 0 ? (
            <div className="w-full h-[400px]">
              <ResponsiveContainer width="100%" height={400}>
                <BarChart
                  data={chartData}
                  margin={{ top: 20, right: 30, left: 20, bottom: 80 }}
                >
                  <CartesianGrid strokeDasharray="3 3" />
                  <XAxis
                    dataKey="name"
                    angle={-45}
                    textAnchor="end"
                    height={100}
                    interval={0}
                    tick={{ fontSize: 12 }}
                  />
                  <YAxis
                    label={{ value: 'Number of Controls', angle: -90, position: 'insideLeft' }}
                  />
                  <Tooltip
                    content={({ active, payload }) => {
                      if (active && payload && payload.length) {
                        const data = payload[0].payload;
                        return (
                          <div className="bg-background border border-border rounded-lg p-3 shadow-lg">
                            <p className="font-semibold text-sm mb-1">{data.fullName}</p>
                            <p className="text-sm text-muted-foreground">
                              Controls: <span className="font-medium text-foreground">{data.count}</span>
                            </p>
                          </div>
                        );
                      }
                      return null;
                    }}
                  />
                  <Bar dataKey="count" radius={[8, 8, 0, 0]}>
                    {chartData.map((entry, index) => (
                      <Cell key={`cell-${index}`} fill={COLORS[index % COLORS.length]} />
                    ))}
                  </Bar>
                </BarChart>
              </ResponsiveContainer>
            </div>
          ) : (
            <div className="text-center py-12 text-muted-foreground">
              No control families found in this catalog
            </div>
          )}
        </CardContent>
      </Card>

      {/* Family Details Table */}
      <Card>
        <CardHeader>
          <CardTitle>Family Details</CardTitle>
        </CardHeader>
        <CardContent>
          <div className="overflow-x-auto">
            <table className="w-full">
              <thead>
                <tr className="border-b">
                  <th className="text-left py-3 px-4 font-semibold">Family</th>
                  <th className="text-left py-3 px-4 font-semibold">ID</th>
                  <th className="text-right py-3 px-4 font-semibold">Controls</th>
                  <th className="text-right py-3 px-4 font-semibold">Percentage</th>
                </tr>
              </thead>
              <tbody>
                {sortedFamilies.map((family, index) => (
                  <tr key={family.id} className="border-b hover:bg-muted/50 transition-colors">
                    <td className="py-3 px-4">
                      <div className="flex items-center gap-2">
                        <div
                          className="w-3 h-3 rounded-full"
                          style={{ backgroundColor: COLORS[index % COLORS.length] }}
                        />
                        {family.title}
                      </div>
                    </td>
                    <td className="py-3 px-4 font-mono text-sm text-muted-foreground">
                      {family.id}
                    </td>
                    <td className="py-3 px-4 text-right font-medium">{family.controlCount}</td>
                    <td className="py-3 px-4 text-right">
                      <Badge variant="secondary">
                        {((family.controlCount / totalControls) * 100).toFixed(1)}%
                      </Badge>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </CardContent>
      </Card>
    </div>
  );
}
