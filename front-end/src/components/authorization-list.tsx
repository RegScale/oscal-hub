'use client';

import { useState } from 'react';
import { Card } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Badge } from '@/components/ui/badge';
import { Label } from '@/components/ui/label';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { Download, Calendar } from 'lucide-react';
import type { AuthorizationResponse } from '@/types/oscal';

interface AuthorizationListProps {
  authorizations: AuthorizationResponse[];
  onView: (authorization: AuthorizationResponse) => void;
  onDelete: (authorizationId: number) => void;
  onCreateNew: () => void;
  isLoading?: boolean;
  currentUsername?: string;
}

type DateFilterType = 'all' | 'authorized' | 'expired';

export function AuthorizationList({
  authorizations,
  onView,
  onDelete,
  onCreateNew,
  isLoading = false,
  currentUsername,
}: AuthorizationListProps) {
  const [searchTerm, setSearchTerm] = useState('');
  const [dateFilter, setDateFilter] = useState<DateFilterType>('all');
  const [startDate, setStartDate] = useState('');
  const [endDate, setEndDate] = useState('');

  const filteredAuthorizations = authorizations.filter((auth) => {
    // Search filter
    const matchesSearch = auth.name.toLowerCase().includes(searchTerm.toLowerCase());

    // Date filter
    let matchesDate = true;
    if (dateFilter !== 'all' && startDate && endDate) {
      const start = new Date(startDate);
      const end = new Date(endDate);

      if (dateFilter === 'authorized') {
        const authorizedDate = new Date(auth.authorizedAt);
        matchesDate = authorizedDate >= start && authorizedDate <= end;
      } else if (dateFilter === 'expired' && auth.dateExpired) {
        const expiredDate = new Date(auth.dateExpired);
        matchesDate = expiredDate >= start && expiredDate <= end;
      }
    }

    return matchesSearch && matchesDate;
  });

  const formatDate = (dateString: string) => {
    return new Date(dateString).toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
    });
  };

  const formatDateTime = (dateString: string) => {
    return new Date(dateString).toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    });
  };

  const handleDownload = (authorization: AuthorizationResponse, e: React.MouseEvent) => {
    e.stopPropagation();

    // Create a blob with the completed content
    const blob = new Blob([authorization.completedContent], { type: 'text/markdown' });
    const url = URL.createObjectURL(blob);

    // Create a temporary link and trigger download
    const link = document.createElement('a');
    link.href = url;
    link.download = `${authorization.name.replace(/\s+/g, '-')}-authorization.md`;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    URL.revokeObjectURL(url);
  };

  return (
    <div className="space-y-4">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h2 className="text-2xl font-bold">System Authorizations</h2>
          <p className="text-gray-600">View and manage authorized systems</p>
        </div>
        <Button onClick={onCreateNew}>Create New Authorization</Button>
      </div>

      {/* Filters */}
      <Card className="p-4">
        <div className="grid gap-4 md:grid-cols-4">
          {/* Search */}
          <div className="md:col-span-2">
            <Label htmlFor="search">Search</Label>
            <Input
              id="search"
              type="search"
              placeholder="Search authorizations..."
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
            />
          </div>

          {/* Date Filter Type */}
          <div>
            <Label htmlFor="date-filter">Date Filter</Label>
            <Select value={dateFilter} onValueChange={(value) => setDateFilter(value as DateFilterType)}>
              <SelectTrigger id="date-filter">
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="all">All Dates</SelectItem>
                <SelectItem value="authorized">By Authorized Date</SelectItem>
                <SelectItem value="expired">By Expiration Date</SelectItem>
              </SelectContent>
            </Select>
          </div>
        </div>

        {/* Date Range (shown when filter is not 'all') */}
        {dateFilter !== 'all' && (
          <div className="grid gap-4 md:grid-cols-2 mt-4">
            <div>
              <Label htmlFor="start-date">Start Date</Label>
              <Input
                id="start-date"
                type="date"
                value={startDate}
                onChange={(e) => setStartDate(e.target.value)}
              />
            </div>
            <div>
              <Label htmlFor="end-date">End Date</Label>
              <Input
                id="end-date"
                type="date"
                value={endDate}
                onChange={(e) => setEndDate(e.target.value)}
              />
            </div>
          </div>
        )}

        {/* Filter Summary */}
        {(searchTerm || dateFilter !== 'all') && (
          <div className="mt-4 pt-4 border-t border-gray-200">
            <p className="text-sm text-gray-600">
              Showing {filteredAuthorizations.length} of {authorizations.length} authorizations
              {dateFilter !== 'all' && startDate && endDate && (
                <span className="ml-2">
                  ({dateFilter === 'authorized' ? 'Authorized' : 'Expiring'} between{' '}
                  {formatDate(startDate)} - {formatDate(endDate)})
                </span>
              )}
            </p>
          </div>
        )}
      </Card>

      {/* Authorizations List */}
      {isLoading ? (
        <div className="text-center py-12">
          <p className="text-gray-500">Loading authorizations...</p>
        </div>
      ) : filteredAuthorizations.length === 0 ? (
        <Card className="p-12 text-center">
          <p className="text-gray-500 mb-4">
            {searchTerm ? 'No authorizations match your search.' : 'No authorizations yet.'}
          </p>
          {!searchTerm && (
            <Button onClick={onCreateNew}>Create Your First Authorization</Button>
          )}
        </Card>
      ) : (
        <div className="space-y-3">
          {filteredAuthorizations.map((authorization) => (
            <Card
              key={authorization.id}
              className="p-6 hover:shadow-lg transition-shadow cursor-pointer"
              onClick={() => onView(authorization)}
            >
              <div className="flex items-start justify-between">
                <div className="flex-1 space-y-3">
                  <div className="flex items-start gap-4">
                    <div className="flex-1">
                      <h3 className="font-semibold text-lg">{authorization.name}</h3>
                      <p className="text-sm text-gray-600 mt-1">
                        Template: {authorization.templateName}
                      </p>
                    </div>
                    <Badge className="bg-green-600 text-white border-green-700">
                      Authorized
                    </Badge>
                  </div>

                  <div className="grid grid-cols-2 md:grid-cols-3 gap-4 text-sm">
                    <div>
                      <p className="text-gray-500 flex items-center gap-1">
                        <Calendar className="h-3 w-3" />
                        Date Authorized
                      </p>
                      <p className="font-medium">{formatDate(authorization.authorizedAt)}</p>
                    </div>
                    <div>
                      <p className="text-gray-500 flex items-center gap-1">
                        <Calendar className="h-3 w-3" />
                        Date Expires
                      </p>
                      <p className="font-medium">
                        {authorization.dateExpired ? formatDate(authorization.dateExpired) : 'N/A'}
                      </p>
                    </div>
                    <div>
                      <p className="text-gray-500">Authorized By</p>
                      <p className="font-medium">{authorization.authorizedBy}</p>
                    </div>
                  </div>
                </div>
              </div>

              <div className="flex items-center gap-2 mt-4 pt-4 border-t border-gray-100">
                <Button
                  size="sm"
                  variant="outline"
                  onClick={(e) => {
                    e.stopPropagation();
                    onView(authorization);
                  }}
                >
                  View Details
                </Button>
                <Button
                  size="sm"
                  variant="outline"
                  onClick={(e) => handleDownload(authorization, e)}
                >
                  <Download className="h-4 w-4 mr-2" />
                  Download
                </Button>
                {currentUsername === authorization.authorizedBy ? (
                  <Button
                    size="sm"
                    variant="outline"
                    className="text-red-600 hover:text-red-700 hover:bg-red-50"
                    onClick={(e) => {
                      e.stopPropagation();
                      if (confirm('Are you sure you want to delete this authorization?')) {
                        onDelete(authorization.id);
                      }
                    }}
                  >
                    Delete
                  </Button>
                ) : (
                  <p className="text-xs text-gray-500">
                    Only the creator can delete this authorization
                  </p>
                )}
              </div>
            </Card>
          ))}
        </div>
      )}
    </div>
  );
}
