'use client';

import { Card, CardHeader, CardTitle, CardContent } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, Cell } from 'recharts';
import { Shield, FileText, GitMerge, Settings, Info, AlertTriangle } from 'lucide-react';
import type { ProfileVisualizationData } from '@/types/oscal';

interface ProfileVisualizationProps {
  data: ProfileVisualizationData;
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
  '#ef4444', // red-500
  '#84cc16', // lime-500
];

export function ProfileVisualization({ data }: ProfileVisualizationProps) {
  const { profileInfo, imports = [], controlSummary, controlsByFamily = {}, modificationSummary } = data || {};

  // Safety check for required data
  if (!data || !profileInfo) {
    return (
      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <AlertTriangle className="h-5 w-5 text-amber-500" />
            Incomplete Data
          </CardTitle>
        </CardHeader>
        <CardContent>
          <p className="text-muted-foreground">
            The Profile document is missing critical information. This may indicate an incomplete or improperly formatted document.
          </p>
          {data && !data.success && data.message && (
            <div className="mt-4 p-3 bg-destructive/10 border border-destructive/20 rounded-lg">
              <p className="text-sm text-destructive">{data.message}</p>
            </div>
          )}
        </CardContent>
      </Card>
    );
  }

  // Prepare control family data for charts
  const familyData = Object.values(controlsByFamily).map(family => ({
    name: family.familyId.toUpperCase(),
    fullName: family.familyName,
    included: family.includedCount,
    excluded: family.excludedCount,
  })).sort((a, b) => b.included - a.included);

  return (
    <div className="space-y-6">
      {/* Profile Information Card */}
      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <Info className="h-5 w-5" />
            Profile Information
          </CardTitle>
        </CardHeader>
        <CardContent>
          <div className="space-y-4">
            <div>
              <h3 className="text-2xl font-semibold mb-2">{profileInfo.title}</h3>
              <div className="flex flex-wrap gap-2 mb-3">
                {profileInfo.version && (
                  <Badge variant="secondary">Version {profileInfo.version}</Badge>
                )}
                {profileInfo.oscalVersion && (
                  <Badge variant="outline">OSCAL {profileInfo.oscalVersion}</Badge>
                )}
              </div>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-2 gap-4 pt-4 border-t">
              <div>
                <p className="text-sm text-muted-foreground mb-1">UUID</p>
                <p className="font-mono text-xs">{profileInfo.uuid}</p>
              </div>
              {profileInfo.lastModified && (
                <div>
                  <p className="text-sm text-muted-foreground mb-1">Last Modified</p>
                  <p className="text-sm">{new Date(profileInfo.lastModified).toLocaleString()}</p>
                </div>
              )}
              {profileInfo.published && (
                <div>
                  <p className="text-sm text-muted-foreground mb-1">Published</p>
                  <p className="text-sm">{new Date(profileInfo.published).toLocaleString()}</p>
                </div>
              )}
            </div>
          </div>
        </CardContent>
      </Card>

      {/* Control Summary Card */}
      {controlSummary && (
        <Card>
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              <Shield className="h-5 w-5" />
              Control Summary
            </CardTitle>
          </CardHeader>
          <CardContent>
            <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
              <div className="text-center p-4 bg-muted rounded-lg">
                <div className="text-4xl font-bold text-primary mb-1">
                  {controlSummary.totalIncludedControls}
                </div>
                <p className="text-sm text-muted-foreground">Included Controls</p>
              </div>
              <div className="text-center p-4 bg-muted rounded-lg">
                <div className="text-4xl font-bold text-amber-500 mb-1">
                  {controlSummary.totalExcludedControls}
                </div>
                <p className="text-sm text-muted-foreground">Excluded Controls</p>
              </div>
              <div className="text-center p-4 bg-muted rounded-lg">
                <div className="text-4xl font-bold text-violet-500 mb-1">
                  {controlSummary.totalModifications}
                </div>
                <p className="text-sm text-muted-foreground">Modifications</p>
              </div>
              <div className="text-center p-4 bg-muted rounded-lg">
                <div className="text-4xl font-bold text-emerald-500 mb-1">
                  {controlSummary.uniqueFamilies}
                </div>
                <p className="text-sm text-muted-foreground">Control Families</p>
              </div>
            </div>
          </CardContent>
        </Card>
      )}

      {/* Imports Card */}
      {imports && imports.length > 0 && (
        <Card>
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              <GitMerge className="h-5 w-5" />
              Profile Imports
            </CardTitle>
          </CardHeader>
          <CardContent>
            <div className="space-y-4">
              {imports.map((importInfo, idx) => (
                <div key={idx} className="p-4 border rounded-lg">
                  <div className="flex items-start justify-between mb-3">
                    <div className="flex-1">
                      <p className="text-sm font-medium mb-1">Import Source</p>
                      <p className="text-xs font-mono text-muted-foreground break-all">
                        {importInfo.href}
                      </p>
                    </div>
                    <Badge variant="secondary" className="ml-3">
                      ~{importInfo.estimatedControlCount} controls
                    </Badge>
                  </div>

                  <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
                    {importInfo.includeAllIds && importInfo.includeAllIds.length > 0 && (
                      <div>
                        <p className="text-xs text-muted-foreground mb-2">Included Controls</p>
                        <div className="flex flex-wrap gap-1">
                          {importInfo.includeAllIds.slice(0, 5).map((id, i) => (
                            <Badge key={i} variant="outline" className="text-xs">
                              {id}
                            </Badge>
                          ))}
                          {importInfo.includeAllIds.length > 5 && (
                            <Badge variant="outline" className="text-xs">
                              +{importInfo.includeAllIds.length - 5} more
                            </Badge>
                          )}
                        </div>
                      </div>
                    )}

                    {importInfo.excludeIds && importInfo.excludeIds.length > 0 && (
                      <div>
                        <p className="text-xs text-muted-foreground mb-2">Excluded Controls</p>
                        <div className="flex flex-wrap gap-1">
                          {importInfo.excludeIds.slice(0, 5).map((id, i) => (
                            <Badge key={i} variant="destructive" className="text-xs">
                              {id}
                            </Badge>
                          ))}
                          {importInfo.excludeIds.length > 5 && (
                            <Badge variant="destructive" className="text-xs">
                              +{importInfo.excludeIds.length - 5} more
                            </Badge>
                          )}
                        </div>
                      </div>
                    )}
                  </div>
                </div>
              ))}
            </div>
          </CardContent>
        </Card>
      )}

      {/* Controls by Family Chart */}
      {familyData.length > 0 && (
        <Card>
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              <FileText className="h-5 w-5" />
              Controls by Family
            </CardTitle>
          </CardHeader>
          <CardContent>
            <div className="w-full h-[400px]">
              <ResponsiveContainer width="100%" height="100%">
                <BarChart
                  data={familyData}
                  margin={{ top: 20, right: 30, left: 20, bottom: 60 }}
                >
                  <CartesianGrid strokeDasharray="3 3" />
                  <XAxis
                    dataKey="name"
                    angle={-45}
                    textAnchor="end"
                    height={80}
                    interval={0}
                    tick={{ fontSize: 12 }}
                  />
                  <YAxis label={{ value: 'Number of Controls', angle: -90, position: 'insideLeft' }} />
                  <Tooltip
                    content={({ active, payload }) => {
                      if (active && payload && payload.length) {
                        const data = payload[0].payload;
                        return (
                          <div className="bg-background border border-border rounded-lg p-3 shadow-lg">
                            <p className="font-semibold text-sm mb-2">{data.fullName}</p>
                            <div className="space-y-1">
                              <p className="text-sm text-muted-foreground">
                                Included: <span className="font-medium text-emerald-500">{data.included}</span>
                              </p>
                              {data.excluded > 0 && (
                                <p className="text-sm text-muted-foreground">
                                  Excluded: <span className="font-medium text-amber-500">{data.excluded}</span>
                                </p>
                              )}
                            </div>
                          </div>
                        );
                      }
                      return null;
                    }}
                  />
                  <Bar dataKey="included" radius={[8, 8, 0, 0]}>
                    {familyData.map((entry, index) => (
                      <Cell key={`cell-${index}`} fill={COLORS[index % COLORS.length]} />
                    ))}
                  </Bar>
                </BarChart>
              </ResponsiveContainer>
            </div>
          </CardContent>
        </Card>
      )}

      {/* Modification Summary Card */}
      {modificationSummary && (
        <Card>
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              <Settings className="h-5 w-5" />
              Profile Modifications
            </CardTitle>
          </CardHeader>
          <CardContent>
            <div className="grid grid-cols-1 md:grid-cols-3 gap-4 mb-6">
              <div className="text-center p-4 bg-muted rounded-lg">
                <div className="text-4xl font-bold text-primary mb-1">
                  {modificationSummary.totalSetsParameters}
                </div>
                <p className="text-sm text-muted-foreground">Parameter Settings</p>
              </div>
              <div className="text-center p-4 bg-muted rounded-lg">
                <div className="text-4xl font-bold text-primary mb-1">
                  {modificationSummary.totalAlters}
                </div>
                <p className="text-sm text-muted-foreground">Control Alterations</p>
              </div>
              <div className="text-center p-4 bg-muted rounded-lg">
                <div className="text-4xl font-bold text-primary mb-1">
                  {modificationSummary.modifiedControlIds.length}
                </div>
                <p className="text-sm text-muted-foreground">Modified Controls</p>
              </div>
            </div>

            {modificationSummary.modifiedControlIds && modificationSummary.modifiedControlIds.length > 0 && (
              <div>
                <p className="text-sm font-medium mb-3">Modified Control IDs</p>
                <div className="flex flex-wrap gap-2">
                  {modificationSummary.modifiedControlIds.map((controlId, idx) => (
                    <Badge key={idx} variant="secondary">
                      {controlId}
                    </Badge>
                  ))}
                </div>
              </div>
            )}
          </CardContent>
        </Card>
      )}

      {/* Control Family Details */}
      {Object.keys(controlsByFamily).length > 0 && (
        <Card>
          <CardHeader>
            <CardTitle>Control Family Details</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="space-y-3">
              {Object.values(controlsByFamily).map((family) => (
                <div key={family.familyId} className="p-4 border rounded-lg">
                  <div className="flex items-start justify-between mb-3">
                    <div>
                      <h4 className="font-semibold">{family.familyName}</h4>
                      <Badge variant="outline" className="mt-1">
                        {family.familyId.toUpperCase()}
                      </Badge>
                    </div>
                    <div className="text-right">
                      <Badge variant="secondary" className="mb-1">
                        {family.includedCount} included
                      </Badge>
                      {family.excludedCount > 0 && (
                        <Badge variant="destructive" className="ml-2">
                          {family.excludedCount} excluded
                        </Badge>
                      )}
                    </div>
                  </div>

                  {family.includedControls && family.includedControls.length > 0 && (
                    <div className="mb-3">
                      <p className="text-xs text-muted-foreground mb-2">Included Controls</p>
                      <div className="flex flex-wrap gap-1">
                        {family.includedControls.slice(0, 10).map((id, idx) => (
                          <Badge key={idx} variant="outline" className="text-xs">
                            {id}
                          </Badge>
                        ))}
                        {family.includedControls.length > 10 && (
                          <Badge variant="outline" className="text-xs">
                            +{family.includedControls.length - 10} more
                          </Badge>
                        )}
                      </div>
                    </div>
                  )}

                  {family.excludedControls && family.excludedControls.length > 0 && (
                    <div>
                      <p className="text-xs text-muted-foreground mb-2">Excluded Controls</p>
                      <div className="flex flex-wrap gap-1">
                        {family.excludedControls.slice(0, 10).map((id, idx) => (
                          <Badge key={idx} variant="destructive" className="text-xs opacity-70">
                            {id}
                          </Badge>
                        ))}
                        {family.excludedControls.length > 10 && (
                          <Badge variant="destructive" className="text-xs opacity-70">
                            +{family.excludedControls.length - 10} more
                          </Badge>
                        )}
                      </div>
                    </div>
                  )}
                </div>
              ))}
            </div>
          </CardContent>
        </Card>
      )}
    </div>
  );
}
