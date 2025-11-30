'use client';

import { useParams, useRouter } from 'next/navigation';
import { useEffect, useState } from 'react';

interface AccessRequest {
  id: number;
  userId: number | null;
  organizationId: number;
  email: string;
  firstName: string;
  lastName: string;
  username: string | null;
  status: string;
  requestDate: string;
  reviewedBy: number | null;
  reviewedDate: string | null;
  notes: string | null;
  message: string | null;
}

export default function OrganizationAccessRequestsPage() {
  const params = useParams();
  const router = useRouter();
  const organizationId = params.id as string;

  const [requests, setRequests] = useState<AccessRequest[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);
  const [organizationName, setOrganizationName] = useState<string>('');

  // Review modal state
  const [reviewingRequest, setReviewingRequest] = useState<AccessRequest | null>(null);
  const [reviewAction, setReviewAction] = useState<'approve' | 'reject' | null>(null);
  const [reviewNotes, setReviewNotes] = useState('');
  const [processing, setProcessing] = useState(false);

  // Filter state
  const [filterStatus, setFilterStatus] = useState<'all' | 'pending' | 'approved' | 'rejected'>('pending');

  useEffect(() => {
    loadOrganization();
    loadAccessRequests();
  }, [organizationId, filterStatus]);

  const loadOrganization = async () => {
    try {
      const response = await fetch(`/api/admin/organizations/${organizationId}`, {
        headers: {
          'Authorization': `Bearer ${localStorage.getItem('token')}`,
        },
      });

      if (response.ok) {
        const org = await response.json();
        setOrganizationName(org.name);
      }
    } catch (err) {
      console.error('Failed to load organization:', err);
    }
  };

  const loadAccessRequests = async () => {
    try {
      setLoading(true);
      setError(null);

      const endpoint = filterStatus === 'pending'
        ? `/api/org-admin/access-requests?organizationId=${organizationId}`
        : `/api/org-admin/access-requests/all?organizationId=${organizationId}`;

      const response = await fetch(`${endpoint}`, {
        headers: {
          'Authorization': `Bearer ${localStorage.getItem('token')}`,
        },
      });

      if (!response.ok) {
        throw new Error('Failed to load access requests');
      }

      let data = await response.json();

      // Filter by status if not showing all pending
      if (filterStatus !== 'pending' && filterStatus !== 'all') {
        data = data.filter((req: AccessRequest) => req.status === filterStatus.toUpperCase());
      }

      setRequests(data);
    } catch (err: any) {
      console.error('Failed to load access requests:', err);
      setError(err.message || 'Failed to load access requests');
    } finally {
      setLoading(false);
    }
  };

  const handleReviewRequest = (request: AccessRequest, action: 'approve' | 'reject') => {
    setReviewingRequest(request);
    setReviewAction(action);
    setReviewNotes('');
  };

  const handleConfirmReview = async () => {
    if (!reviewingRequest || !reviewAction) return;

    try {
      setProcessing(true);
      setError(null);
      setSuccess(null);

      const endpoint = reviewAction === 'approve'
        ? `/api/org-admin/access-requests/${reviewingRequest.id}/approve`
        : `/api/org-admin/access-requests/${reviewingRequest.id}/reject`;

      const response = await fetch(`${endpoint}`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${localStorage.getItem('token')}`,
        },
        body: JSON.stringify({
          notes: reviewNotes.trim() || null,
        }),
      });

      if (!response.ok) {
        const error = await response.json();
        throw new Error(error.error || `Failed to ${reviewAction} request`);
      }

      setSuccess(`Access request ${reviewAction === 'approve' ? 'approved' : 'rejected'} successfully`);
      setReviewingRequest(null);
      setReviewAction(null);
      setReviewNotes('');
      await loadAccessRequests();
      setTimeout(() => setSuccess(null), 3000);
    } catch (err: any) {
      console.error(`Failed to ${reviewAction} request:`, err);
      setError(err.message || `Failed to ${reviewAction} request`);
    } finally {
      setProcessing(false);
    }
  };

  const handleCancelReview = () => {
    setReviewingRequest(null);
    setReviewAction(null);
    setReviewNotes('');
  };

  if (loading) {
    return (
      <div className="min-h-screen flex items-center justify-center">
        <div className="text-center">
          <div className="animate-spin rounded-full h-16 w-16 border-b-2 border-blue-600 mx-auto"></div>
          <p className="mt-4 text-gray-600 dark:text-gray-400">Loading access requests...</p>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gray-50 dark:bg-gray-900 py-8 px-4 sm:px-6 lg:px-8">
      <div className="max-w-7xl mx-auto">
        {/* Header */}
        <div className="mb-6">
          <button
            onClick={() => router.push('/admin/organizations')}
            className="text-blue-600 hover:text-blue-700 dark:text-blue-400 dark:hover:text-blue-300 flex items-center"
          >
            <svg className="h-5 w-5 mr-1" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 19l-7-7 7-7" />
            </svg>
            Back to Organizations
          </button>
        </div>

        <div className="flex justify-between items-center mb-8">
          <div>
            <h1 className="text-3xl font-bold text-gray-900 dark:text-white">
              Access Requests
            </h1>
            <p className="mt-2 text-gray-600 dark:text-gray-400">
              {organizationName ? `Manage access requests for ${organizationName}` : `Organization #${organizationId}`}
            </p>
          </div>
          <div>
            <select
              value={filterStatus}
              onChange={(e) => setFilterStatus(e.target.value as any)}
              className="px-4 py-2 border border-gray-300 dark:border-gray-700 rounded-md bg-white dark:bg-gray-900 text-gray-900 dark:text-white"
            >
              <option value="pending">Pending</option>
              <option value="all">All Requests</option>
              <option value="approved">Approved</option>
              <option value="rejected">Rejected</option>
            </select>
          </div>
        </div>

        {/* Success Message */}
        {success && (
          <div className="mb-6 bg-green-50 dark:bg-green-900/20 border border-green-200 dark:border-green-800 rounded-lg p-4">
            <p className="text-green-800 dark:text-green-200">{success}</p>
          </div>
        )}

        {/* Error Message */}
        {error && (
          <div className="mb-6 bg-red-50 dark:bg-red-900/20 border border-red-200 dark:border-red-800 rounded-lg p-4">
            <p className="text-red-800 dark:text-red-200">{error}</p>
          </div>
        )}

        {/* Requests Table */}
        <div className="bg-white dark:bg-gray-800 rounded-lg shadow overflow-hidden">
          <table className="min-w-full divide-y divide-gray-200 dark:divide-gray-700">
            <thead className="bg-gray-50 dark:bg-gray-700">
              <tr>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-300 uppercase tracking-wider">
                  Requester
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-300 uppercase tracking-wider">
                  Email
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-300 uppercase tracking-wider">
                  Status
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-300 uppercase tracking-wider">
                  Request Date
                </th>
                <th className="px-6 py-3 text-right text-xs font-medium text-gray-500 dark:text-gray-300 uppercase tracking-wider">
                  Actions
                </th>
              </tr>
            </thead>
            <tbody className="bg-white dark:bg-gray-800 divide-y divide-gray-200 dark:divide-gray-700">
              {requests.length === 0 ? (
                <tr>
                  <td colSpan={5} className="px-6 py-12 text-center">
                    <div className="text-gray-500 dark:text-gray-400">
                      <svg className="mx-auto h-12 w-12 mb-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M20 13V6a2 2 0 00-2-2H6a2 2 0 00-2 2v7m16 0v5a2 2 0 01-2 2H6a2 2 0 01-2-2v-5m16 0h-2.586a1 1 0 00-.707.293l-2.414 2.414a1 1 0 01-.707.293h-3.172a1 1 0 01-.707-.293l-2.414-2.414A1 1 0 006.586 13H4" />
                      </svg>
                      <p className="text-lg font-medium">No access requests</p>
                      <p className="mt-1 text-sm">
                        {filterStatus === 'pending'
                          ? 'There are currently no pending access requests for this organization.'
                          : `No ${filterStatus} access requests found.`}
                      </p>
                    </div>
                  </td>
                </tr>
              ) : (
                requests.map((request) => (
                  <tr key={request.id} className="hover:bg-gray-50 dark:hover:bg-gray-700">
                    <td className="px-6 py-4 whitespace-nowrap">
                      <div className="text-sm font-medium text-gray-900 dark:text-white">
                        {request.firstName} {request.lastName}
                      </div>
                      {request.username && (
                        <div className="text-sm text-gray-500 dark:text-gray-400">@{request.username}</div>
                      )}
                      {request.message && (
                        <div className="text-sm text-gray-500 dark:text-gray-400 italic mt-1">
                          "{request.message.length > 50 ? request.message.substring(0, 50) + '...' : request.message}"
                        </div>
                      )}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500 dark:text-gray-400">
                      {request.email}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap">
                      <span className={`px-2 inline-flex text-xs leading-5 font-semibold rounded-full ${
                        request.status === 'PENDING'
                          ? 'bg-yellow-100 text-yellow-800 dark:bg-yellow-900 dark:text-yellow-200'
                          : request.status === 'APPROVED'
                          ? 'bg-green-100 text-green-800 dark:bg-green-900 dark:text-green-200'
                          : 'bg-red-100 text-red-800 dark:bg-red-900 dark:text-red-200'
                      }`}>
                        {request.status}
                      </span>
                      {request.notes && (
                        <div className="text-xs text-gray-500 dark:text-gray-400 mt-1">
                          Notes: {request.notes.length > 30 ? request.notes.substring(0, 30) + '...' : request.notes}
                        </div>
                      )}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap">
                      <div className="text-sm text-gray-900 dark:text-white">
                        {new Date(request.requestDate).toLocaleDateString()}
                      </div>
                      {request.reviewedDate && (
                        <div className="text-xs text-gray-500 dark:text-gray-400">
                          Reviewed: {new Date(request.reviewedDate).toLocaleDateString()}
                        </div>
                      )}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-right text-sm font-medium">
                      {request.status === 'PENDING' && (
                        <>
                          <button
                            onClick={() => handleReviewRequest(request, 'approve')}
                            className="text-green-600 hover:text-green-900 dark:text-green-400 dark:hover:text-green-300 mr-4"
                          >
                            Approve
                          </button>
                          <button
                            onClick={() => handleReviewRequest(request, 'reject')}
                            className="text-red-600 hover:text-red-900 dark:text-red-400 dark:hover:text-red-300"
                          >
                            Reject
                          </button>
                        </>
                      )}
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>

      {/* Review Modal */}
      {reviewingRequest && reviewAction && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
          <div className="bg-white dark:bg-gray-800 rounded-lg shadow-xl max-w-lg w-full mx-4">
            <div className="px-6 py-4 border-b border-gray-200 dark:border-gray-700">
              <h2 className="text-xl font-semibold text-gray-900 dark:text-white">
                {reviewAction === 'approve' ? 'Approve' : 'Reject'} Access Request
              </h2>
            </div>
            <div className="px-6 py-4">
              <div className="mb-4">
                <p className="text-sm text-gray-700 dark:text-gray-300">
                  <strong>Requester:</strong> {reviewingRequest.firstName} {reviewingRequest.lastName}
                </p>
                <p className="text-sm text-gray-700 dark:text-gray-300">
                  <strong>Email:</strong> {reviewingRequest.email}
                </p>
                {reviewingRequest.message && (
                  <p className="text-sm text-gray-700 dark:text-gray-300 mt-2">
                    <strong>Message:</strong> {reviewingRequest.message}
                  </p>
                )}
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
                  Notes (optional)
                </label>
                <textarea
                  value={reviewNotes}
                  onChange={(e) => setReviewNotes(e.target.value)}
                  rows={3}
                  className="w-full px-4 py-2 border border-gray-300 dark:border-gray-700 rounded-md focus:ring-2 focus:ring-blue-500 focus:border-transparent bg-white dark:bg-gray-900 text-gray-900 dark:text-white"
                  placeholder={reviewAction === 'approve' ? 'Optional notes for the user' : 'Reason for rejection'}
                />
              </div>
            </div>
            <div className="px-6 py-4 border-t border-gray-200 dark:border-gray-700 flex justify-end space-x-3">
              <button
                onClick={handleCancelReview}
                disabled={processing}
                className="px-4 py-2 border border-gray-300 dark:border-gray-700 text-gray-700 dark:text-gray-300 rounded-md hover:bg-gray-50 dark:hover:bg-gray-800 disabled:opacity-50"
              >
                Cancel
              </button>
              <button
                onClick={handleConfirmReview}
                disabled={processing}
                className={`px-4 py-2 rounded-md text-white disabled:opacity-50 ${
                  reviewAction === 'approve'
                    ? 'bg-green-600 hover:bg-green-700'
                    : 'bg-red-600 hover:bg-red-700'
                }`}
              >
                {processing
                  ? 'Processing...'
                  : reviewAction === 'approve'
                  ? 'Approve Request'
                  : 'Reject Request'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
