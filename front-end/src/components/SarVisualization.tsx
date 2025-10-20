'use client';

import { useState } from 'react';
import { Card, CardHeader, CardTitle, CardContent } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Dialog, DialogContent, DialogDescription, DialogHeader, DialogTitle } from '@/components/ui/dialog';
import { BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, Cell, PieChart, Pie, Legend } from 'recharts';
import { FileCheck, AlertTriangle, Eye, AlertCircle, Info, Target, Filter } from 'lucide-react';
import type { SarVisualizationData, Finding, Observation } from '@/types/oscal';

interface SarVisualizationProps {
  data: SarVisualizationData;
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

export function SarVisualization({ data }: SarVisualizationProps) {
  const {
    assessmentInfo,
    assessmentSummary,
    controlsByFamily = {},
    findings = [],
    observations = [],
    risks = []
  } = data || {};

  // State for findings score filter (default: 0-100)
  const [minScore, setMinScore] = useState(0);
  const [maxScore, setMaxScore] = useState(100);

  // State for pagination
  const [currentPage, setCurrentPage] = useState(1);
  const itemsPerPage = 10;

  // State for observation details modal
  const [selectedFinding, setSelectedFinding] = useState<Finding | null>(null);
  const [isDetailsModalOpen, setIsDetailsModalOpen] = useState(false);

  // Safety check for required data
  if (!data || !assessmentInfo) {
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
            The SAR document is missing critical assessment information. This may indicate an incomplete or improperly formatted document.
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
    controlsAssessed: family.totalControlsAssessed,
    findings: family.totalFindings,
    observations: family.totalObservations,
  })).sort((a, b) => b.controlsAssessed - a.controlsAssessed);

  // Prepare score distribution data in proper order
  const scoreRanges = ['0-10', '10-20', '20-30', '30-40', '40-50', '50-60', '60-70', '70-80', '80-90', '90-100'];
  const scoreData = scoreRanges.map(range => ({
    name: range + '%',
    value: assessmentSummary?.scoreDistribution?.[range] || 0,
  }));

  // Filter and sort findings by score range (low to high)
  const filteredFindings = findings
    .filter(finding => {
      if (finding.score === undefined) return true; // Show findings without scores
      return finding.score >= minScore && finding.score <= maxScore;
    })
    .sort((a, b) => {
      // Sort by score (low to high)
      const scoreA = a.score ?? Infinity; // Put findings without scores at the end
      const scoreB = b.score ?? Infinity;
      return scoreA - scoreB;
    });

  // Pagination calculation
  const totalPages = Math.ceil(filteredFindings.length / itemsPerPage);
  const startIndex = (currentPage - 1) * itemsPerPage;
  const endIndex = startIndex + itemsPerPage;
  const paginatedFindings = filteredFindings.slice(startIndex, endIndex);

  // Reset to page 1 when filters change
  const resetPagination = () => setCurrentPage(1);

  // Get score color based on value
  const getScoreColor = (score: number) => {
    if (score >= 90) return 'bg-green-500';
    if (score >= 70) return 'bg-blue-500';
    if (score >= 50) return 'bg-yellow-500';
    if (score >= 30) return 'bg-orange-500';
    return 'bg-red-500';
  };

  // Get observations related to a finding
  const getRelatedObservations = (finding: Finding) => {
    if (!finding.relatedObservations || finding.relatedObservations.length === 0) {
      return [];
    }
    return observations.filter(obs =>
      finding.relatedObservations.includes(obs.uuid)
    );
  };

  // Handle opening the details modal
  const handleViewDetails = (finding: Finding) => {
    setSelectedFinding(finding);
    setIsDetailsModalOpen(true);
  };

  return (
    <div className="space-y-6">
      {/* Assessment Information Card */}
      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <FileCheck className="h-5 w-5" />
            Assessment Information
          </CardTitle>
        </CardHeader>
        <CardContent>
          <div className="space-y-4">
            <div>
              <h3 className="text-2xl font-semibold mb-2">{assessmentInfo.title}</h3>
              <div className="flex flex-wrap gap-2 mb-3">
                {assessmentInfo.version && (
                  <Badge variant="secondary">Version {assessmentInfo.version}</Badge>
                )}
                {assessmentInfo.oscalVersion && (
                  <Badge variant="outline">OSCAL {assessmentInfo.oscalVersion}</Badge>
                )}
              </div>
              {assessmentInfo.description && (
                <p className="text-sm text-muted-foreground">{assessmentInfo.description}</p>
              )}
            </div>

            <div className="pt-4 border-t grid grid-cols-1 md:grid-cols-2 gap-4">
              {assessmentInfo.published && (
                <div>
                  <p className="text-sm font-medium text-muted-foreground">Published</p>
                  <p className="text-sm">{new Date(assessmentInfo.published).toLocaleDateString()}</p>
                </div>
              )}
              {assessmentInfo.lastModified && (
                <div>
                  <p className="text-sm font-medium text-muted-foreground">Last Modified</p>
                  <p className="text-sm">{new Date(assessmentInfo.lastModified).toLocaleDateString()}</p>
                </div>
              )}
              {assessmentInfo.sspImportHref && (
                <div className="md:col-span-2">
                  <p className="text-sm font-medium text-muted-foreground mb-1">SSP Reference</p>
                  <Badge variant="outline" className="font-mono text-xs">{assessmentInfo.sspImportHref}</Badge>
                </div>
              )}
            </div>
          </div>
        </CardContent>
      </Card>

      {/* Assessment Summary */}
      {assessmentSummary && (
        <Card>
          <CardHeader>
            <CardTitle>Assessment Summary</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="grid grid-cols-2 md:grid-cols-3 gap-4">
              <div className="text-center p-4 bg-muted rounded-lg">
                <div className="text-4xl font-bold text-primary mb-1">
                  {assessmentSummary.totalControlsAssessed}
                </div>
                <p className="text-sm text-muted-foreground">Controls Assessed</p>
              </div>
              <div className="text-center p-4 bg-muted rounded-lg">
                <div className="text-4xl font-bold text-amber-600 mb-1">
                  {assessmentSummary.totalFindings}
                </div>
                <p className="text-sm text-muted-foreground">Findings</p>
              </div>
              <div className="text-center p-4 bg-muted rounded-lg">
                <div className="text-4xl font-bold text-blue-600 mb-1">
                  {assessmentSummary.totalObservations}
                </div>
                <p className="text-sm text-muted-foreground">Observations</p>
              </div>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-2 gap-4 mt-4">
              <div className="text-center p-4 bg-muted rounded-lg">
                <div className="text-3xl font-bold text-primary mb-1">
                  {assessmentSummary.uniqueFamiliesAssessed}
                </div>
                <p className="text-sm text-muted-foreground">Control Families Assessed</p>
              </div>
              <div className="text-center p-4 bg-muted rounded-lg">
                <div className="text-3xl font-bold text-primary mb-1">
                  {assessmentSummary.totalControlsAssessed > 0
                    ? Math.round((assessmentSummary.totalFindings / assessmentSummary.totalControlsAssessed) * 100) / 100
                    : 0}
                </div>
                <p className="text-sm text-muted-foreground">Avg Findings per Control</p>
              </div>
            </div>
          </CardContent>
        </Card>
      )}

      {/* Controls Assessed by Family */}
      {familyData.length > 0 && (
        <Card>
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              <Target className="h-5 w-5" />
              Controls Assessed by Family
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
                            <div className="space-y-1 text-xs">
                              <p>
                                Controls Assessed: <span className="font-medium">{data.controlsAssessed}</span>
                              </p>
                              <p className="text-amber-600">
                                Findings: <span className="font-medium">{data.findings}</span>
                              </p>
                              <p className="text-blue-600">
                                Observations: <span className="font-medium">{data.observations}</span>
                              </p>
                            </div>
                          </div>
                        );
                      }
                      return null;
                    }}
                  />
                  <Bar dataKey="controlsAssessed" radius={[8, 8, 0, 0]}>
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

      {/* Control Score Distribution - Full Width */}
      {scoreData.some(d => d.value > 0) && (
        <Card>
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              <Target className="h-5 w-5" />
              Control Score Distribution
            </CardTitle>
          </CardHeader>
          <CardContent>
            <div className="w-full h-[400px]">
              <ResponsiveContainer width="100%" height="100%">
                <BarChart
                  data={scoreData}
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
                            <p className="font-semibold text-sm mb-1">Score Range: {data.name}</p>
                            <p className="text-xs">
                              Controls: <span className="font-medium">{data.value}</span>
                            </p>
                          </div>
                        );
                      }
                      return null;
                    }}
                  />
                  <Bar dataKey="value" radius={[8, 8, 0, 0]} fill="#3b82f6" />
                </BarChart>
              </ResponsiveContainer>
            </div>
          </CardContent>
        </Card>
      )}


      {/* Findings List */}
      {findings.length > 0 && (
        <Card>
          <CardHeader>
            <div className="flex flex-col gap-3">
              <div className="flex items-center justify-between">
                <CardTitle className="flex items-center gap-2">
                  <AlertTriangle className="h-5 w-5" />
                  Findings ({filteredFindings.length} of {findings.length}) - Sorted Low to High
                </CardTitle>
                <div className="flex items-center gap-2">
                  <Filter className="h-4 w-4 text-muted-foreground" />
                  <span className="text-sm text-muted-foreground">Score:</span>
                  <input
                    type="number"
                    min="0"
                    max="100"
                    value={minScore}
                    onChange={(e) => {
                      setMinScore(Number(e.target.value));
                      resetPagination();
                    }}
                    className="w-16 px-2 py-1 text-sm border rounded"
                    placeholder="Min"
                  />
                  <span className="text-sm text-muted-foreground">-</span>
                  <input
                    type="number"
                    min="0"
                    max="100"
                    value={maxScore}
                    onChange={(e) => {
                      setMaxScore(Number(e.target.value));
                      resetPagination();
                    }}
                    className="w-16 px-2 py-1 text-sm border rounded"
                    placeholder="Max"
                  />
                </div>
              </div>
              {/* Pagination Controls */}
              {totalPages > 1 && (
                <div className="flex items-center justify-center gap-2">
                  <button
                    onClick={() => setCurrentPage(p => Math.max(1, p - 1))}
                    disabled={currentPage === 1}
                    className="px-3 py-1 text-sm border rounded disabled:opacity-50 disabled:cursor-not-allowed hover:bg-muted"
                  >
                    Previous
                  </button>
                  <span className="text-sm text-muted-foreground">
                    Page {currentPage} of {totalPages}
                  </span>
                  <button
                    onClick={() => setCurrentPage(p => Math.min(totalPages, p + 1))}
                    disabled={currentPage === totalPages}
                    className="px-3 py-1 text-sm border rounded disabled:opacity-50 disabled:cursor-not-allowed hover:bg-muted"
                  >
                    Next
                  </button>
                </div>
              )}
            </div>
          </CardHeader>
          <CardContent>
            <div className="space-y-3">
              {paginatedFindings.map((finding) => (
                <div key={finding.uuid} className="p-4 border rounded-lg hover:bg-muted/50 transition-colors">
                  <div className="flex items-start justify-between mb-2">
                    <h4 className="font-semibold flex-1">{finding.title}</h4>
                    {finding.score !== undefined && (
                      <Badge className={`${getScoreColor(finding.score)} text-white`}>
                        Score: {finding.score.toFixed(1)}
                      </Badge>
                    )}
                  </div>
                  {finding.description && (
                    <p className="text-sm text-muted-foreground mb-2">{finding.description}</p>
                  )}
                  <div className="flex flex-wrap gap-3 items-center justify-between">
                    <div className="flex flex-wrap gap-3 items-center">
                      {finding.relatedControls.length > 0 && (
                        <div className="flex items-center gap-1">
                          <span className="text-xs text-muted-foreground">Controls:</span>
                          {finding.relatedControls.map((controlId, idx) => (
                            <Badge key={idx} variant="outline" className="text-xs">
                              {controlId}
                            </Badge>
                          ))}
                        </div>
                      )}
                      {finding.qualityScore !== undefined && (
                        <span className="text-xs text-muted-foreground">
                          Quality: <span className="font-medium">{finding.qualityScore.toFixed(1)}</span>
                        </span>
                      )}
                      {finding.completenessScore !== undefined && (
                        <span className="text-xs text-muted-foreground">
                          Completeness: <span className="font-medium">{finding.completenessScore.toFixed(1)}</span>
                        </span>
                      )}
                    </div>
                    {finding.relatedObservations && finding.relatedObservations.length > 0 && (
                      <Button
                        variant="outline"
                        size="sm"
                        onClick={() => handleViewDetails(finding)}
                        className="text-xs"
                      >
                        <Eye className="h-3 w-3 mr-1" />
                        View Details
                      </Button>
                    )}
                  </div>
                </div>
              ))}
              {filteredFindings.length === 0 && (
                <p className="text-sm text-muted-foreground text-center py-4">
                  No findings match the selected score range.
                </p>
              )}
            </div>
          </CardContent>
        </Card>
      )}

      {/* Observation Details Modal */}
      <Dialog open={isDetailsModalOpen} onOpenChange={setIsDetailsModalOpen}>
        <DialogContent className="max-w-4xl max-h-[80vh] overflow-y-auto">
          <DialogHeader>
            <DialogTitle className="flex items-center gap-2">
              <Eye className="h-5 w-5" />
              Related Observations
            </DialogTitle>
            <DialogDescription>
              {selectedFinding && `Detailed observations for: ${selectedFinding.title}`}
            </DialogDescription>
          </DialogHeader>

          {selectedFinding && (
            <div className="space-y-4 mt-4">
              {/* Finding Summary */}
              <div className="p-4 bg-muted rounded-lg">
                <h4 className="font-semibold mb-2">{selectedFinding.title}</h4>
                {selectedFinding.description && (
                  <p className="text-sm text-muted-foreground mb-3">{selectedFinding.description}</p>
                )}
                <div className="flex flex-wrap gap-3 items-center text-sm">
                  {selectedFinding.score !== undefined && (
                    <Badge className={`${getScoreColor(selectedFinding.score)} text-white`}>
                      Score: {selectedFinding.score.toFixed(1)}
                    </Badge>
                  )}
                  {selectedFinding.qualityScore !== undefined && (
                    <span className="text-muted-foreground">
                      Quality: <span className="font-medium">{selectedFinding.qualityScore.toFixed(1)}</span>
                    </span>
                  )}
                  {selectedFinding.completenessScore !== undefined && (
                    <span className="text-muted-foreground">
                      Completeness: <span className="font-medium">{selectedFinding.completenessScore.toFixed(1)}</span>
                    </span>
                  )}
                </div>
              </div>

              {/* Related Observations List */}
              <div>
                <h5 className="font-semibold mb-3 flex items-center gap-2">
                  <AlertCircle className="h-4 w-4" />
                  Observations ({getRelatedObservations(selectedFinding).length})
                </h5>
                <div className="space-y-3">
                  {getRelatedObservations(selectedFinding).map((observation) => (
                    <div key={observation.uuid} className="p-4 border rounded-lg">
                      <div className="flex items-start justify-between mb-2">
                        <h6 className="font-semibold text-sm">{observation.title}</h6>
                        {observation.observationType && (
                          <Badge variant="secondary" className="text-xs">
                            {observation.observationType}
                          </Badge>
                        )}
                      </div>
                      {observation.description && (
                        <p className="text-sm text-muted-foreground mb-3">{observation.description}</p>
                      )}
                      <div className="flex flex-wrap gap-3 items-center text-xs">
                        {observation.relatedControls.length > 0 && (
                          <div className="flex items-center gap-1">
                            <span className="text-muted-foreground">Controls:</span>
                            <div className="flex flex-wrap gap-1">
                              {observation.relatedControls.map((controlId, idx) => (
                                <Badge key={idx} variant="outline" className="text-xs">
                                  {controlId}
                                </Badge>
                              ))}
                            </div>
                          </div>
                        )}
                        {observation.overallScore !== undefined && (
                          <span className="text-muted-foreground">
                            Overall: <span className="font-medium">{observation.overallScore.toFixed(1)}</span>
                          </span>
                        )}
                        {observation.qualityScore !== undefined && (
                          <span className="text-muted-foreground">
                            Quality: <span className="font-medium">{observation.qualityScore.toFixed(1)}</span>
                          </span>
                        )}
                        {observation.completenessScore !== undefined && (
                          <span className="text-muted-foreground">
                            Completeness: <span className="font-medium">{observation.completenessScore.toFixed(1)}</span>
                          </span>
                        )}
                      </div>
                    </div>
                  ))}
                  {getRelatedObservations(selectedFinding).length === 0 && (
                    <p className="text-sm text-muted-foreground text-center py-4">
                      No observations found for this finding.
                    </p>
                  )}
                </div>
              </div>
            </div>
          )}
        </DialogContent>
      </Dialog>

    </div>
  );
}
