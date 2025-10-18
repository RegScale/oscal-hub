'use client';

import { Card, CardHeader, CardTitle, CardContent } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, Cell, PieChart, Pie, Legend } from 'recharts';
import { Shield, Users, Server, AlertTriangle, Info } from 'lucide-react';

export interface SspVisualizationData {
  success: boolean;
  message: string;
  systemInfo: {
    uuid: string;
    name: string;
    shortName: string;
    description: string;
    status: string;
    systemIds: Array<{
      identifierType: string;
      id: string;
    }>;
  };
  categorization: {
    confidentiality: string;
    integrity: string;
    availability: string;
    overall: string;
  };
  informationTypes: Array<{
    uuid: string;
    title: string;
    description: string;
    categorizations: string[];
    confidentiality: {
      base: string;
      selected: string;
    };
    integrity: {
      base: string;
      selected: string;
    };
    availability: {
      base: string;
      selected: string;
    };
  }>;
  personnel: Array<{
    roleId: string;
    roleTitle: string;
    roleShortName: string;
    assignedPersonnel: Array<{
      uuid: string;
      name: string;
      jobTitle: string;
      type: string;
    }>;
  }>;
  controlsByFamily: Record<string, {
    familyId: string;
    familyName: string;
    totalControls: number;
    statusCounts: Record<string, number>;
    controls: Array<{
      controlId: string;
      implementationStatus: string;
      controlOrigination: string;
    }>;
  }>;
  assets: Array<{
    uuid: string;
    description: string;
    assetType: string;
    function: string;
    fqdn: string;
    ipv4Address: string;
    ipv6Address: string;
    macAddress: string;
    virtual: boolean;
    publicAccess: boolean;
    softwareName: string;
    softwareVersion: string;
    vendorName: string;
    scanned: boolean;
  }>;
}

interface SspVisualizationProps {
  data: SspVisualizationData;
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

const IMPACT_COLORS: Record<string, string> = {
  'fips-199-low': '#10b981', // green
  'fips-199-moderate': '#f59e0b', // amber
  'fips-199-high': '#ef4444', // red
};

export function SspVisualization({ data }: SspVisualizationProps) {
  const { systemInfo, categorization, informationTypes = [], personnel = [], controlsByFamily = {}, assets = [] } = data || {};

  // Safety check for required data
  if (!data || !systemInfo) {
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
            The SSP document is missing critical system information. This may indicate an incomplete or improperly formatted document.
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
    count: family.totalControls,
  })).sort((a, b) => b.count - a.count);

  // Calculate total controls
  const totalControls = familyData.reduce((sum, f) => sum + f.count, 0);

  // Prepare status distribution data
  const allStatusCounts: Record<string, number> = {};
  Object.values(controlsByFamily).forEach(family => {
    Object.entries(family.statusCounts).forEach(([status, count]) => {
      allStatusCounts[status] = (allStatusCounts[status] || 0) + count;
    });
  });

  const statusData = Object.entries(allStatusCounts).map(([name, value]) => ({
    name: name.replace(/-/g, ' ').replace(/\b\w/g, c => c.toUpperCase()),
    value,
  }));

  // Calculate asset types
  const assetTypeCount = assets.reduce((acc, asset) => {
    const type = asset.assetType || 'unknown';
    acc[type] = (acc[type] || 0) + 1;
    return acc;
  }, {} as Record<string, number>);

  const assetChartData = Object.entries(assetTypeCount).map(([name, count]) => ({
    name: name.replace(/-/g, ' ').replace(/\b\w/g, c => c.toUpperCase()),
    count,
  }));

  return (
    <div className="space-y-6">
      {/* System Information Card */}
      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <Info className="h-5 w-5" />
            System Information
          </CardTitle>
        </CardHeader>
        <CardContent>
          <div className="space-y-4">
            <div>
              <h3 className="text-2xl font-semibold mb-2">{systemInfo.name}</h3>
              <div className="flex flex-wrap gap-2 mb-3">
                {systemInfo.shortName && (
                  <Badge variant="secondary">{systemInfo.shortName}</Badge>
                )}
                {systemInfo.status && (
                  <Badge variant="outline">
                    Status: {systemInfo.status.replace(/-/g, ' ').replace(/\b\w/g, c => c.toUpperCase())}
                  </Badge>
                )}
              </div>
              {systemInfo.description && (
                <p className="text-sm text-muted-foreground">{systemInfo.description}</p>
              )}
            </div>

            {systemInfo.systemIds && systemInfo.systemIds.length > 0 && (
              <div className="pt-4 border-t">
                <p className="text-sm font-medium mb-2">System Identifiers</p>
                <div className="flex flex-wrap gap-2">
                  {systemInfo.systemIds.map((sid, idx) => (
                    <Badge key={idx} variant="outline">
                      {sid.identifierType}: {sid.id}
                    </Badge>
                  ))}
                </div>
              </div>
            )}
          </div>
        </CardContent>
      </Card>

      {/* Security Categorization Card */}
      {categorization && (
        <Card>
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              <Shield className="h-5 w-5" />
              Security Categorization
            </CardTitle>
          </CardHeader>
          <CardContent>
            <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
              <div className="text-center p-4 bg-muted rounded-lg">
                <p className="text-sm text-muted-foreground mb-2">Overall</p>
                <Badge
                  variant="outline"
                  className="text-lg px-4 py-2"
                  style={{
                    borderColor: IMPACT_COLORS[categorization.overall] || '#6b7280',
                    color: IMPACT_COLORS[categorization.overall] || '#6b7280',
                  }}
                >
                  {categorization.overall?.replace('fips-199-', '').toUpperCase()}
                </Badge>
              </div>
              <div className="text-center p-4 bg-muted rounded-lg">
                <p className="text-sm text-muted-foreground mb-2">Confidentiality</p>
                <Badge
                  variant="outline"
                  className="text-lg px-4 py-2"
                  style={{
                    borderColor: IMPACT_COLORS[categorization.confidentiality] || '#6b7280',
                    color: IMPACT_COLORS[categorization.confidentiality] || '#6b7280',
                  }}
                >
                  {categorization.confidentiality?.replace('fips-199-', '').toUpperCase()}
                </Badge>
              </div>
              <div className="text-center p-4 bg-muted rounded-lg">
                <p className="text-sm text-muted-foreground mb-2">Integrity</p>
                <Badge
                  variant="outline"
                  className="text-lg px-4 py-2"
                  style={{
                    borderColor: IMPACT_COLORS[categorization.integrity] || '#6b7280',
                    color: IMPACT_COLORS[categorization.integrity] || '#6b7280',
                  }}
                >
                  {categorization.integrity?.replace('fips-199-', '').toUpperCase()}
                </Badge>
              </div>
              <div className="text-center p-4 bg-muted rounded-lg">
                <p className="text-sm text-muted-foreground mb-2">Availability</p>
                <Badge
                  variant="outline"
                  className="text-lg px-4 py-2"
                  style={{
                    borderColor: IMPACT_COLORS[categorization.availability] || '#6b7280',
                    color: IMPACT_COLORS[categorization.availability] || '#6b7280',
                  }}
                >
                  {categorization.availability?.replace('fips-199-', '').toUpperCase()}
                </Badge>
              </div>
            </div>
          </CardContent>
        </Card>
      )}

      {/* Information Types */}
      {informationTypes && informationTypes.length > 0 && (
        <Card>
          <CardHeader>
            <CardTitle>Information Types</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="space-y-4">
              {informationTypes.map((infoType) => (
                <div key={infoType.uuid} className="p-4 border rounded-lg">
                  <h4 className="font-semibold mb-2">{infoType.title}</h4>
                  {infoType.description && (
                    <p className="text-sm text-muted-foreground mb-3">{infoType.description}</p>
                  )}
                  <div className="grid grid-cols-3 gap-2">
                    <div>
                      <p className="text-xs text-muted-foreground mb-1">Confidentiality</p>
                      <Badge variant="secondary" className="text-xs">
                        {infoType.confidentiality?.selected?.replace('fips-199-', '').toUpperCase()}
                      </Badge>
                    </div>
                    <div>
                      <p className="text-xs text-muted-foreground mb-1">Integrity</p>
                      <Badge variant="secondary" className="text-xs">
                        {infoType.integrity?.selected?.replace('fips-199-', '').toUpperCase()}
                      </Badge>
                    </div>
                    <div>
                      <p className="text-xs text-muted-foreground mb-1">Availability</p>
                      <Badge variant="secondary" className="text-xs">
                        {infoType.availability?.selected?.replace('fips-199-', '').toUpperCase()}
                      </Badge>
                    </div>
                  </div>
                </div>
              ))}
            </div>
          </CardContent>
        </Card>
      )}

      {/* Personnel and Roles */}
      {personnel && personnel.length > 0 && (
        <Card>
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              <Users className="h-5 w-5" />
              Personnel and Roles
            </CardTitle>
          </CardHeader>
          <CardContent>
            <div className="space-y-3">
              {personnel.map((role) => (
                <div key={role.roleId} className="p-3 border rounded-lg">
                  <div className="flex items-start justify-between mb-2">
                    <div>
                      <h4 className="font-semibold">{role.roleTitle}</h4>
                      {role.roleShortName && (
                        <Badge variant="outline" className="mt-1">
                          {role.roleShortName}
                        </Badge>
                      )}
                    </div>
                    <Badge>{role.assignedPersonnel.length} assigned</Badge>
                  </div>
                  {role.assignedPersonnel.length > 0 && (
                    <div className="mt-2 space-y-1">
                      {role.assignedPersonnel.map((person) => (
                        <div key={person.uuid} className="text-sm flex items-center justify-between py-1">
                          <span className="font-medium">{person.name}</span>
                          {person.jobTitle && (
                            <span className="text-muted-foreground text-xs">{person.jobTitle}</span>
                          )}
                        </div>
                      ))}
                    </div>
                  )}
                </div>
              ))}
            </div>
          </CardContent>
        </Card>
      )}

      {/* Control Implementation Status */}
      {familyData.length > 0 && (
        <>
          <Card>
            <CardHeader>
              <CardTitle>Control Implementation Overview</CardTitle>
            </CardHeader>
            <CardContent>
              <div className="grid grid-cols-2 md:grid-cols-4 gap-4 mb-6">
                <div className="text-center p-4 bg-muted rounded-lg">
                  <div className="text-4xl font-bold text-primary mb-1">{totalControls}</div>
                  <p className="text-sm text-muted-foreground">Total Controls</p>
                </div>
                <div className="text-center p-4 bg-muted rounded-lg">
                  <div className="text-4xl font-bold text-primary mb-1">{familyData.length}</div>
                  <p className="text-sm text-muted-foreground">Control Families</p>
                </div>
                <div className="text-center p-4 bg-muted rounded-lg">
                  <div className="text-4xl font-bold text-primary mb-1">
                    {allStatusCounts['implemented'] || 0}
                  </div>
                  <p className="text-sm text-muted-foreground">Implemented</p>
                </div>
                <div className="text-center p-4 bg-muted rounded-lg">
                  <div className="text-4xl font-bold text-primary mb-1">
                    {Math.round(((allStatusCounts['implemented'] || 0) / totalControls) * 100)}%
                  </div>
                  <p className="text-sm text-muted-foreground">Complete</p>
                </div>
              </div>

              {statusData.length > 0 && (
                <div className="w-full h-[300px]">
                  <ResponsiveContainer width="100%" height="100%">
                    <PieChart>
                      <Pie
                        data={statusData}
                        cx="50%"
                        cy="50%"
                        labelLine={false}
                        label={({ name, percent }) => `${name}: ${(percent * 100).toFixed(0)}%`}
                        outerRadius={80}
                        fill="#8884d8"
                        dataKey="value"
                      >
                        {statusData.map((entry, index) => (
                          <Cell key={`cell-${index}`} fill={COLORS[index % COLORS.length]} />
                        ))}
                      </Pie>
                      <Tooltip />
                      <Legend />
                    </PieChart>
                  </ResponsiveContainer>
                </div>
              )}
            </CardContent>
          </Card>

          <Card>
            <CardHeader>
              <CardTitle>Controls by Family</CardTitle>
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
                      {familyData.map((entry, index) => (
                        <Cell key={`cell-${index}`} fill={COLORS[index % COLORS.length]} />
                      ))}
                    </Bar>
                  </BarChart>
                </ResponsiveContainer>
              </div>
            </CardContent>
          </Card>
        </>
      )}

      {/* Asset Information */}
      {assets && assets.length > 0 && (
        <>
          <Card>
            <CardHeader>
              <CardTitle className="flex items-center gap-2">
                <Server className="h-5 w-5" />
                Asset Inventory
              </CardTitle>
            </CardHeader>
            <CardContent>
              <div className="grid grid-cols-2 md:grid-cols-4 gap-4 mb-6">
                <div className="text-center p-4 bg-muted rounded-lg">
                  <div className="text-4xl font-bold text-primary mb-1">{assets.length}</div>
                  <p className="text-sm text-muted-foreground">Total Assets</p>
                </div>
                <div className="text-center p-4 bg-muted rounded-lg">
                  <div className="text-4xl font-bold text-primary mb-1">
                    {assets.filter(a => a.virtual).length}
                  </div>
                  <p className="text-sm text-muted-foreground">Virtual</p>
                </div>
                <div className="text-center p-4 bg-muted rounded-lg">
                  <div className="text-4xl font-bold text-primary mb-1">
                    {assets.filter(a => a.publicAccess).length}
                  </div>
                  <p className="text-sm text-muted-foreground">Public</p>
                </div>
                <div className="text-center p-4 bg-muted rounded-lg">
                  <div className="text-4xl font-bold text-primary mb-1">
                    {assets.filter(a => a.scanned).length}
                  </div>
                  <p className="text-sm text-muted-foreground">Scanned</p>
                </div>
              </div>

              {assetChartData.length > 0 && (
                <div className="w-full h-[300px] mb-6">
                  <ResponsiveContainer width="100%" height="100%">
                    <PieChart>
                      <Pie
                        data={assetChartData}
                        cx="50%"
                        cy="50%"
                        labelLine={false}
                        label={({ name, count }) => `${name}: ${count}`}
                        outerRadius={80}
                        fill="#8884d8"
                        dataKey="count"
                      >
                        {assetChartData.map((entry, index) => (
                          <Cell key={`cell-${index}`} fill={COLORS[index % COLORS.length]} />
                        ))}
                      </Pie>
                      <Tooltip />
                      <Legend />
                    </PieChart>
                  </ResponsiveContainer>
                </div>
              )}

              <div className="overflow-x-auto">
                <table className="w-full text-sm">
                  <thead>
                    <tr className="border-b">
                      <th className="text-left py-2 px-3 font-semibold">Description</th>
                      <th className="text-left py-2 px-3 font-semibold">Type</th>
                      <th className="text-left py-2 px-3 font-semibold">Function</th>
                      <th className="text-left py-2 px-3 font-semibold">FQDN</th>
                      <th className="text-center py-2 px-3 font-semibold">Virtual</th>
                      <th className="text-center py-2 px-3 font-semibold">Public</th>
                    </tr>
                  </thead>
                  <tbody>
                    {assets.map((asset) => (
                      <tr key={asset.uuid} className="border-b hover:bg-muted/50 transition-colors">
                        <td className="py-2 px-3">{asset.description}</td>
                        <td className="py-2 px-3">
                          <Badge variant="outline">{asset.assetType}</Badge>
                        </td>
                        <td className="py-2 px-3">{asset.function}</td>
                        <td className="py-2 px-3 font-mono text-xs">{asset.fqdn}</td>
                        <td className="py-2 px-3 text-center">
                          {asset.virtual ? '✓' : '—'}
                        </td>
                        <td className="py-2 px-3 text-center">
                          {asset.publicAccess ? (
                            <AlertTriangle className="h-4 w-4 text-amber-500 inline" />
                          ) : (
                            '—'
                          )}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </CardContent>
          </Card>
        </>
      )}
    </div>
  );
}
