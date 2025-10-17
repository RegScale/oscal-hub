'use client';

import { useState, useEffect } from 'react';
import Link from 'next/link';
import { ArrowLeft, Clock, CheckCircle2, AlertCircle, TrendingUp, Trash2, RefreshCw } from 'lucide-react';
import { Card, CardHeader, CardTitle, CardContent } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { apiClient } from '@/lib/api-client';
import ProtectedRoute from '@/components/ProtectedRoute';
import type { OperationHistory, OperationStats } from '@/types/oscal';

export default function HistoryPage() {
  const [operations, setOperations] = useState<OperationHistory[]>([]);
  const [stats, setStats] = useState<OperationStats | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);

  const loadData = async () => {
    setIsLoading(true);
    try {
      const [historyData, statsData] = await Promise.all([
        apiClient.getOperationHistory(page, 20),
        apiClient.getOperationStats(),
      ]);

      setOperations(historyData.content);
      setTotalPages(historyData.totalPages);
      setStats(statsData);
    } catch (error) {
      console.error('Failed to load history data:', error);
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    loadData();
  }, [page]);

  const handleDelete = async (id: number) => {
    if (!confirm('Are you sure you want to delete this operation from history?')) {
      return;
    }

    try {
      await apiClient.deleteOperation(id);
      loadData(); // Reload data after deletion
    } catch (error) {
      console.error('Failed to delete operation:', error);
      alert('Failed to delete operation. Please try again.');
    }
  };

  const getOperationTypeColor = (type: string) => {
    if (type.includes('VALIDATE')) return 'bg-blue-500/10 text-blue-500 border-blue-500/20';
    if (type.includes('CONVERT')) return 'bg-purple-500/10 text-purple-500 border-purple-500/20';
    if (type.includes('RESOLVE')) return 'bg-orange-500/10 text-orange-500 border-orange-500/20';
    if (type.includes('BATCH')) return 'bg-indigo-500/10 text-indigo-500 border-indigo-500/20';
    return 'bg-gray-500/10 text-gray-500 border-gray-500/20';
  };

  const formatDuration = (ms?: number) => {
    if (!ms) return 'N/A';
    if (ms < 1000) return `${ms}ms`;
    return `${(ms / 1000).toFixed(2)}s`;
  };

  const formatTimestamp = (timestamp: string) => {
    try {
      const date = new Date(timestamp);
      return date.toLocaleString();
    } catch {
      return timestamp;
    }
  };

  return (
    <ProtectedRoute>
      <div className="min-h-screen bg-background">
      <div className="container mx-auto py-8 px-4" id="main-content">
        {/* Header */}
        <header className="mb-8">
          <Link
            href="/"
            className="inline-flex items-center gap-2 text-sm text-muted-foreground hover:text-foreground mb-4 transition-colors focus:outline-none focus:ring-2 focus:ring-ring focus:ring-offset-2 rounded"
            aria-label="Navigate back to dashboard"
          >
            <ArrowLeft className="h-4 w-4" aria-hidden="true" />
            Back to Dashboard
          </Link>
          <div className="flex items-center justify-between">
            <div>
              <h1 className="text-4xl font-bold mb-2">Operation History</h1>
              <p className="text-muted-foreground">
                View and manage all your OSCAL operations
              </p>
            </div>
            <Button
              onClick={loadData}
              variant="outline"
              size="sm"
              aria-label="Refresh operation history"
            >
              <RefreshCw className="h-4 w-4 mr-2" aria-hidden="true" />
              Refresh
            </Button>
          </div>
        </header>

        {/* Statistics Cards */}
        {stats && (
          <section
            className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6 mb-8"
            aria-label="Operation statistics"
          >
            <Card>
              <CardContent className="pt-6">
                <div className="flex items-center gap-4">
                  <div className="p-3 rounded-lg bg-blue-500/10">
                    <Clock className="h-6 w-6 text-blue-500" />
                  </div>
                  <div>
                    <p className="text-sm text-muted-foreground">Total Operations</p>
                    <p className="text-2xl font-bold">{stats.totalOperations}</p>
                  </div>
                </div>
              </CardContent>
            </Card>

            <Card>
              <CardContent className="pt-6">
                <div className="flex items-center gap-4">
                  <div className="p-3 rounded-lg bg-green-500/10">
                    <CheckCircle2 className="h-6 w-6 text-green-500" />
                  </div>
                  <div>
                    <p className="text-sm text-muted-foreground">Successful</p>
                    <p className="text-2xl font-bold">{stats.successfulOperations}</p>
                  </div>
                </div>
              </CardContent>
            </Card>

            <Card>
              <CardContent className="pt-6">
                <div className="flex items-center gap-4">
                  <div className="p-3 rounded-lg bg-red-500/10">
                    <AlertCircle className="h-6 w-6 text-red-500" />
                  </div>
                  <div>
                    <p className="text-sm text-muted-foreground">Failed</p>
                    <p className="text-2xl font-bold">{stats.failedOperations}</p>
                  </div>
                </div>
              </CardContent>
            </Card>

            <Card>
              <CardContent className="pt-6">
                <div className="flex items-center gap-4">
                  <div className="p-3 rounded-lg bg-purple-500/10">
                    <TrendingUp className="h-6 w-6 text-purple-500" />
                  </div>
                  <div>
                    <p className="text-sm text-muted-foreground">Success Rate</p>
                    <p className="text-2xl font-bold">{stats.successRate.toFixed(1)}%</p>
                  </div>
                </div>
              </CardContent>
            </Card>
          </section>
        )}

        {/* Operations Table */}
        <Card>
          <CardHeader>
            <CardTitle>Recent Operations</CardTitle>
          </CardHeader>
          <CardContent>
            {isLoading ? (
              <div
                className="text-center py-12 text-muted-foreground"
                role="status"
                aria-live="polite"
              >
                Loading operations...
              </div>
            ) : operations.length === 0 ? (
              <div
                className="text-center py-12 text-muted-foreground"
                role="status"
              >
                <p className="text-lg mb-2">No operations yet</p>
                <p className="text-sm">Operations will appear here after you validate, convert, or resolve OSCAL documents</p>
              </div>
            ) : (
              <>
                <div className="overflow-x-auto">
                  <table
                    className="w-full"
                    aria-label="Operation history table"
                  >
                    <thead className="border-b">
                      <tr className="text-left text-sm text-muted-foreground">
                        <th scope="col" className="pb-3 font-medium">Type</th>
                        <th scope="col" className="pb-3 font-medium">File</th>
                        <th scope="col" className="pb-3 font-medium">Status</th>
                        <th scope="col" className="pb-3 font-medium">Model</th>
                        <th scope="col" className="pb-3 font-medium">Duration</th>
                        <th scope="col" className="pb-3 font-medium">Timestamp</th>
                        <th scope="col" className="pb-3 font-medium">
                          <span className="sr-only">Actions</span>
                        </th>
                      </tr>
                    </thead>
                    <tbody>
                      {operations.map((op) => (
                        <tr key={op.id} className="border-b last:border-0 hover:bg-muted/50">
                          <td className="py-4">
                            <Badge variant="outline" className={getOperationTypeColor(op.operationType)}>
                              {op.operationType}
                            </Badge>
                          </td>
                          <td className="py-4">
                            <div className="font-medium">{op.fileName}</div>
                            {op.details && (
                              <div className="text-xs text-muted-foreground mt-1">{op.details}</div>
                            )}
                          </td>
                          <td className="py-4">
                            {op.success ? (
                              <div className="flex items-center gap-2 text-green-500">
                                <CheckCircle2 className="h-4 w-4" />
                                <span className="text-sm">Success</span>
                              </div>
                            ) : (
                              <div className="flex items-center gap-2 text-red-500">
                                <AlertCircle className="h-4 w-4" />
                                <span className="text-sm">Failed</span>
                              </div>
                            )}
                          </td>
                          <td className="py-4">
                            <div className="text-sm">{op.modelType || 'N/A'}</div>
                            <div className="text-xs text-muted-foreground">{op.format || ''}</div>
                          </td>
                          <td className="py-4 text-sm">{formatDuration(op.durationMs)}</td>
                          <td className="py-4 text-sm text-muted-foreground">
                            {formatTimestamp(op.timestamp)}
                          </td>
                          <td className="py-4">
                            <Button
                              variant="ghost"
                              size="sm"
                              onClick={() => handleDelete(op.id)}
                              className="text-red-500 hover:text-red-600 hover:bg-red-500/10"
                              aria-label={`Delete operation ${op.fileName} from ${formatTimestamp(op.timestamp)}`}
                            >
                              <Trash2 className="h-4 w-4" aria-hidden="true" />
                              <span className="sr-only">Delete</span>
                            </Button>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>

                {/* Pagination */}
                {totalPages > 1 && (
                  <nav
                    className="flex items-center justify-between mt-6 pt-6 border-t"
                    aria-label="Pagination navigation"
                  >
                    <div className="text-sm text-muted-foreground" aria-live="polite">
                      Page {page + 1} of {totalPages}
                    </div>
                    <div className="flex gap-2">
                      <Button
                        variant="outline"
                        size="sm"
                        onClick={() => setPage(p => Math.max(0, p - 1))}
                        disabled={page === 0}
                        aria-label="Go to previous page"
                      >
                        Previous
                      </Button>
                      <Button
                        variant="outline"
                        size="sm"
                        onClick={() => setPage(p => Math.min(totalPages - 1, p + 1))}
                        disabled={page >= totalPages - 1}
                        aria-label="Go to next page"
                      >
                        Next
                      </Button>
                    </div>
                  </nav>
                )}
              </>
            )}
          </CardContent>
        </Card>
      </div>
    </div>
    </ProtectedRoute>
  );
}
