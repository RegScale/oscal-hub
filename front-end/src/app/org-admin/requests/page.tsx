'use client';

import { useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import { ChevronLeft, Loader2, CheckCircle, XCircle, Clock } from 'lucide-react';
import { apiClient } from '@/lib/api-client';
import { HelpButton } from '@/components/HelpButton';

interface AccessRequest {
  id: number;
  userId: number | null;
  username: string;
  email: string;
  firstName: string;
  lastName: string;
  organizationId: number;
  organizationName: string;
  status: string;
  message: string | null;
  requestDate: string;
  reviewedBy: number | null;
  reviewedByUsername: string | null;
  reviewedDate: string | null;
  notes: string | null;
}

export default function OrgAdminRequestsPage() {
  const router = useRouter();
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);
  const [requests, setRequests] = useState<AccessRequest[]>([]);
  const [activeTab, setActiveTab] = useState<'pending' | 'all'>('pending');
  const [organizationId, setOrganizationId] = useState<number | null>(null);
  const [reviewingRequest, setReviewingRequest] = useState<AccessRequest | null>(null);
  const [reviewAction, setReviewAction] = useState<'approve' | 'reject' | null>(null);
  const [reviewNotes, setReviewNotes] = useState('');
  const [processing, setProcessing] = useState(false);

  useEffect(() => {
    const storedUser = localStorage.getItem('user');
    if (!storedUser) {
      router.push('/login');
      return;
    }

    try {
      const userData = JSON.parse(storedUser);
      const isOrgAdmin = userData.orgRole === 'ORG_ADMIN';
      const isSuperAdmin = userData.globalRole === 'SUPER_ADMIN';
      const hasOrgContext = !!userData.organizationId;

      if (!isOrgAdmin && !isSuperAdmin && !hasOrgContext) {
        router.push('/');
        return;
      }

      const currentOrg = localStorage.getItem('currentOrganization');
      let orgId: number | null = null;
      if (currentOrg) {
        orgId = JSON.parse(currentOrg).id;
      } else if (userData.organizationId) {
        orgId = userData.organizationId;
      }

      if (orgId) {
        setOrganizationId(orgId);
        loadRequests(orgId, 'pending');
      } else {
        setError('No organization selected');
        setLoading(false);
      }
    } catch {
      setError('Failed to load user data. Please try logging in again.');
      setLoading(false);
    }
  }, [router]);

  const loadRequests = async (orgId: number, tab: 'pending' | 'all') => {
    try {
      setLoading(true);
      setError(null);
      const data = tab === 'pending'
        ? await apiClient.getOrgPendingRequests(orgId)
        : await apiClient.getOrgAllRequests(orgId);
      setRequests(data);
    } catch (err) {
      console.error('Failed to load requests:', err);
      setError(err instanceof Error ? err.message : 'Failed to load requests');
    } finally {
      setLoading(false);
    }
  };

  const handleTabChange = (tab: 'pending' | 'all') => {
    setActiveTab(tab);
    if (organizationId) {
      loadRequests(organizationId, tab);
    }
  };

  const handleReview = (request: AccessRequest, action: 'approve' | 'reject') => {
    setReviewingRequest(request);
    setReviewAction(action);
    setReviewNotes('');
  };

  const handleConfirmReview = async () => {
    if (!reviewingRequest || !reviewAction || !organizationId) return;

    try {
      setProcessing(true);
      setError(null);
      setSuccess(null);

      if (reviewAction === 'approve') {
        await apiClient.approveOrgRequest(reviewingRequest.id, reviewNotes || undefined);
        setSuccess(`Access request from ${reviewingRequest.firstName} ${reviewingRequest.lastName} approved`);
      } else {
        await apiClient.rejectOrgRequest(reviewingRequest.id, reviewNotes || undefined);
        setSuccess(`Access request from ${reviewingRequest.firstName} ${reviewingRequest.lastName} rejected`);
      }

      setReviewingRequest(null);
      setReviewAction(null);
      setReviewNotes('');
      await loadRequests(organizationId, activeTab);
    } catch (err) {
      console.error('Failed to process request:', err);
      setError(err instanceof Error ? err.message : 'Failed to process request');
    } finally {
      setProcessing(false);
    }
  };

  const pendingCount = requests.filter((r) => r.status === 'PENDING').length;

  const statusBadge = (status: string) => {
    const styles: Record<string, { className: string; icon: typeof Clock }> = {
      PENDING: { className: 'bg-yellow-100 text-yellow-800 dark:bg-yellow-900 dark:text-yellow-200', icon: Clock },
      APPROVED: { className: 'bg-green-100 text-green-800 dark:bg-green-900 dark:text-green-200', icon: CheckCircle },
      REJECTED: { className: 'bg-red-100 text-red-800 dark:bg-red-900 dark:text-red-200', icon: XCircle },
    };
    const style = styles[status] || styles.PENDING;
    const Icon = style.icon;
    return (
      <span className={`inline-flex items-center gap-1 px-2 py-0.5 text-xs font-medium rounded-full ${style.className}`}>
        <Icon className="h-3 w-3" />
        {status}
      </span>
    );
  };

  if (loading && requests.length === 0) {
    return (
      <div className="min-h-screen bg-gray-50 dark:bg-gray-900 flex items-center justify-center">
        <Loader2 className="h-8 w-8 animate-spin text-blue-500" />
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gray-50 dark:bg-gray-900 py-8 px-4 sm:px-6 lg:px-8">
      <div className="max-w-6xl mx-auto">
        {/* Back link */}
        <button onClick={() => router.push('/org-admin')} className="flex items-center text-gray-600 dark:text-gray-400 hover:text-gray-900 dark:hover:text-white mb-6">
          <ChevronLeft className="h-4 w-4 mr-1" />
          Back to Dashboard
        </button>

        <div className="flex items-center gap-2 mb-2">
          <h1 className="text-3xl font-bold text-gray-900 dark:text-white">Access Requests</h1>
          <HelpButton slug="org-admin-requests" />
        </div>
        <p className="text-gray-600 dark:text-gray-400 mb-8">Review and manage access requests for your organization</p>

        {/* Alerts */}
        {error && (
          <div className="bg-red-50 dark:bg-red-900/20 border border-red-200 dark:border-red-800 rounded-lg p-4 mb-4 text-red-700 dark:text-red-300">
            {error}
          </div>
        )}
        {success && (
          <div className="bg-green-50 dark:bg-green-900/20 border border-green-200 dark:border-green-800 rounded-lg p-4 mb-4 text-green-700 dark:text-green-300">
            {success}
          </div>
        )}

        {/* Tabs */}
        <div className="border-b border-gray-200 dark:border-gray-700 mb-6">
          <nav className="flex space-x-8">
            <button
              onClick={() => handleTabChange('pending')}
              className={`py-4 px-1 border-b-2 font-medium text-sm ${
                activeTab === 'pending'
                  ? 'border-blue-500 text-blue-600 dark:text-blue-400'
                  : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300 dark:text-gray-400 dark:hover:text-gray-300'
              }`}
            >
              Pending
              {activeTab === 'pending' && pendingCount > 0 && (
                <span className="ml-2 bg-red-100 dark:bg-red-900/50 text-red-800 dark:text-red-200 text-xs font-medium px-2 py-0.5 rounded-full">
                  {pendingCount}
                </span>
              )}
            </button>
            <button
              onClick={() => handleTabChange('all')}
              className={`py-4 px-1 border-b-2 font-medium text-sm ${
                activeTab === 'all'
                  ? 'border-blue-500 text-blue-600 dark:text-blue-400'
                  : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300 dark:text-gray-400 dark:hover:text-gray-300'
              }`}
            >
              All Requests
            </button>
          </nav>
        </div>

        {/* Request Cards */}
        {requests.length === 0 ? (
          <div className="bg-white dark:bg-gray-800 rounded-lg shadow p-12 text-center">
            <p className="text-gray-500 dark:text-gray-400">
              {activeTab === 'pending' ? 'No pending access requests.' : 'No access requests found.'}
            </p>
          </div>
        ) : (
          <div className="space-y-4">
            {requests.map((request) => (
              <div
                key={request.id}
                className="bg-white dark:bg-gray-800 rounded-lg shadow-md border border-gray-200 dark:border-gray-700 p-6"
              >
                <div className="flex items-start justify-between">
                  <div className="flex-1">
                    <div className="flex items-center gap-3 mb-2">
                      <h3 className="text-lg font-medium text-gray-900 dark:text-white">
                        {request.firstName} {request.lastName}
                      </h3>
                      {statusBadge(request.status)}
                    </div>
                    <div className="space-y-1 text-sm text-gray-500 dark:text-gray-400">
                      <p>Email: {request.email}</p>
                      {request.username && <p>Username: {request.username}</p>}
                      <p>Requested: {new Date(request.requestDate).toLocaleString()}</p>
                    </div>
                    {request.message && (
                      <div className="mt-3 p-3 bg-gray-50 dark:bg-gray-700 rounded-md">
                        <p className="text-sm text-gray-600 dark:text-gray-300">
                          <span className="font-medium">Message:</span> {request.message}
                        </p>
                      </div>
                    )}
                    {request.notes && (
                      <div className="mt-3 p-3 bg-blue-50 dark:bg-blue-900/20 rounded-md">
                        <p className="text-sm text-blue-700 dark:text-blue-300">
                          <span className="font-medium">Admin Notes:</span> {request.notes}
                        </p>
                        {request.reviewedByUsername && (
                          <p className="text-xs text-blue-500 dark:text-blue-400 mt-1">
                            Reviewed by {request.reviewedByUsername} on {request.reviewedDate ? new Date(request.reviewedDate).toLocaleString() : '-'}
                          </p>
                        )}
                      </div>
                    )}
                  </div>

                  {/* Actions for pending requests */}
                  {request.status === 'PENDING' && (
                    <div className="flex gap-2 ml-4">
                      <button
                        onClick={() => handleReview(request, 'approve')}
                        className="px-4 py-2 bg-green-600 text-white text-sm font-medium rounded-md hover:bg-green-700"
                      >
                        Approve
                      </button>
                      <button
                        onClick={() => handleReview(request, 'reject')}
                        className="px-4 py-2 bg-red-600 text-white text-sm font-medium rounded-md hover:bg-red-700"
                      >
                        Reject
                      </button>
                    </div>
                  )}
                </div>
              </div>
            ))}
          </div>
        )}

        {/* Review Modal */}
        {reviewingRequest && reviewAction && (
          <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
            <div className="bg-white dark:bg-gray-800 rounded-lg shadow-xl max-w-md w-full mx-4">
              <div className="px-6 py-4 border-b border-gray-200 dark:border-gray-700">
                <h3 className="text-lg font-semibold text-gray-900 dark:text-white">
                  {reviewAction === 'approve' ? 'Approve' : 'Reject'} Access Request
                </h3>
              </div>
              <div className="px-6 py-4">
                <div className="space-y-2 text-sm mb-4">
                  <p><span className="font-medium text-gray-700 dark:text-gray-300">Name:</span> <span className="text-gray-600 dark:text-gray-400">{reviewingRequest.firstName} {reviewingRequest.lastName}</span></p>
                  <p><span className="font-medium text-gray-700 dark:text-gray-300">Email:</span> <span className="text-gray-600 dark:text-gray-400">{reviewingRequest.email}</span></p>
                  {reviewingRequest.message && (
                    <p><span className="font-medium text-gray-700 dark:text-gray-300">Message:</span> <span className="text-gray-600 dark:text-gray-400">{reviewingRequest.message}</span></p>
                  )}
                </div>
                <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                  Notes {reviewAction === 'reject' ? '(recommended)' : '(optional)'}
                </label>
                <textarea
                  value={reviewNotes}
                  onChange={(e) => setReviewNotes(e.target.value)}
                  rows={3}
                  placeholder={reviewAction === 'reject' ? 'Reason for rejection...' : 'Optional notes...'}
                  className="w-full px-4 py-2 border border-gray-300 dark:border-gray-700 rounded-md bg-white dark:bg-gray-900 text-gray-900 dark:text-white focus:ring-2 focus:ring-blue-500"
                />
              </div>
              <div className="px-6 py-4 border-t border-gray-200 dark:border-gray-700 flex justify-end gap-3">
                <button
                  onClick={() => { setReviewingRequest(null); setReviewAction(null); setReviewNotes(''); }}
                  disabled={processing}
                  className="px-4 py-2 text-gray-700 dark:text-gray-300 bg-gray-100 dark:bg-gray-700 rounded-md hover:bg-gray-200 dark:hover:bg-gray-600"
                >
                  Cancel
                </button>
                <button
                  onClick={handleConfirmReview}
                  disabled={processing}
                  className={`px-4 py-2 text-white rounded-md flex items-center gap-2 ${
                    reviewAction === 'approve'
                      ? 'bg-green-600 hover:bg-green-700'
                      : 'bg-red-600 hover:bg-red-700'
                  }`}
                >
                  {processing && <Loader2 className="h-4 w-4 animate-spin" />}
                  {reviewAction === 'approve' ? 'Approve' : 'Reject'}
                </button>
              </div>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
