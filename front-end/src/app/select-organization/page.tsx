'use client';

import { useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import { apiClient } from '@/lib/api-client';
import { useAuth } from '@/contexts/AuthContext';

interface Organization {
  organizationId: number;
  name: string;
  description: string | null;
  logoUrl: string | null;
  role?: string;
  joinedAt?: string;
}

type TabType = 'my-orgs' | 'available';

export default function SelectOrganizationPage() {
  const router = useRouter();
  const { updateUser } = useAuth();
  const [organizations, setOrganizations] = useState<Organization[]>([]);
  const [loading, setLoading] = useState(true);
  const [selecting, setSelecting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [activeTab, setActiveTab] = useState<TabType>('my-orgs');

  useEffect(() => {
    loadOrganizations();
  }, []);

  const loadOrganizations = async () => {
    try {
      setLoading(true);
      setError(null);

      // Check if user is authenticated
      const token = localStorage.getItem('token');
      if (!token) {
        // Not authenticated, redirect to login
        router.push('/login');
        return;
      }

      // Get ALL organizations and user's memberships
      const [allOrgs, myOrgs] = await Promise.all([
        apiClient.getOrganizations(),
        apiClient.getMyOrganizations()
      ]);

      // Create a map of user's organization memberships
      const myOrgMap = new Map(myOrgs.map(org => [org.organizationId, org]));

      // Merge the data: add role and joinedAt to organizations the user is a member of
      const mergedOrgs = allOrgs.map(org => {
        const membership = myOrgMap.get(org.organizationId);
        return {
          ...org,
          role: membership?.role,
          joinedAt: membership?.joinedAt
        };
      });

      setOrganizations(mergedOrgs);

      // Set default tab based on whether user has organizations
      const hasOrganizations = mergedOrgs.some(org => org.role);
      setActiveTab(hasOrganizations ? 'my-orgs' : 'available');
    } catch (err) {
      console.error('Failed to load organizations:', err);
      setError('Failed to load organizations. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  const handleSelectOrganization = async (orgId: number) => {
    try {
      setSelecting(true);
      setError(null);

      // Select organization and get full JWT
      const result = await apiClient.selectOrganization(orgId);

      // Sync the AuthContext with the new user data from localStorage
      updateUser();

      // Check if password change is required
      if (result.mustChangePassword) {
        router.push('/change-password');
        return;
      }

      // Redirect to home page
      router.push('/');
    } catch (err: any) {
      console.error('Failed to select organization:', err);
      setError(err.message || 'Failed to select organization. Please try again.');
    } finally {
      setSelecting(false);
    }
  };

  const handleRequestAccess = (orgId?: number) => {
    if (orgId) {
      router.push(`/request-access?org=${orgId}`);
    } else {
      router.push('/request-access');
    }
  };

  const myOrganizations = organizations.filter(org => org.role);
  const availableOrganizations = organizations.filter(org => !org.role);

  if (loading) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-gradient-to-br from-blue-50 to-indigo-100 dark:from-gray-900 dark:to-gray-800">
        <div className="text-center">
          <div className="animate-spin rounded-full h-16 w-16 border-b-2 border-blue-600 mx-auto"></div>
          <p className="mt-4 text-gray-600 dark:text-gray-400">Loading organizations...</p>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gradient-to-br from-blue-50 to-indigo-100 dark:from-gray-900 dark:to-gray-800 py-12 px-4 sm:px-6 lg:px-8">
      <div className="max-w-7xl mx-auto">
        <div className="text-center mb-12">
          <h1 className="text-4xl font-bold text-gray-900 dark:text-white mb-2">
            Select Your Organization
          </h1>
          <p className="text-lg text-gray-600 dark:text-gray-400">
            Choose an organization to continue
          </p>
        </div>

        {error && (
          <div className="mb-8 bg-red-50 dark:bg-red-900/20 border border-red-200 dark:border-red-800 rounded-lg p-4">
            <p className="text-red-800 dark:text-red-200">{error}</p>
          </div>
        )}

        {organizations.length === 0 ? (
          <div className="max-w-2xl mx-auto bg-white dark:bg-gray-800 rounded-lg shadow-lg p-8 text-center">
            <svg
              className="mx-auto h-16 w-16 text-gray-400 mb-4"
              fill="none"
              stroke="currentColor"
              viewBox="0 0 24 24"
            >
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth={2}
                d="M19 21V5a2 2 0 00-2-2H7a2 2 0 00-2 2v16m14 0h2m-2 0h-5m-9 0H3m2 0h5M9 7h1m-1 4h1m4-4h1m-1 4h1m-5 10v-5a1 1 0 011-1h2a1 1 0 011 1v5m-4 0h4"
              />
            </svg>
            <h3 className="text-xl font-semibold text-gray-900 dark:text-white mb-2">
              No Organizations Available
            </h3>
            <p className="text-gray-600 dark:text-gray-400 mb-6">
              There are no organizations available at this time.
            </p>
          </div>
        ) : (
          <>
            {/* Tab Navigation */}
            <div className="mb-8">
              <div className="border-b border-gray-200 dark:border-gray-700">
                <nav className="-mb-px flex space-x-8">
                  <button
                    onClick={() => setActiveTab('my-orgs')}
                    className={`${
                      activeTab === 'my-orgs'
                        ? 'border-blue-500 text-blue-600 dark:text-blue-400'
                        : 'border-transparent text-gray-500 dark:text-gray-400 hover:text-gray-700 dark:hover:text-gray-300 hover:border-gray-300 dark:hover:border-gray-600'
                    } whitespace-nowrap py-4 px-1 border-b-2 font-medium text-sm transition-colors`}
                  >
                    My Organizations
                    {myOrganizations.length > 0 && (
                      <span className={`ml-2 ${
                        activeTab === 'my-orgs'
                          ? 'bg-blue-100 text-blue-600 dark:bg-blue-900 dark:text-blue-400'
                          : 'bg-gray-100 text-gray-600 dark:bg-gray-800 dark:text-gray-400'
                      } py-0.5 px-2.5 rounded-full text-xs font-medium`}>
                        {myOrganizations.length}
                      </span>
                    )}
                  </button>
                  <button
                    onClick={() => setActiveTab('available')}
                    className={`${
                      activeTab === 'available'
                        ? 'border-blue-500 text-blue-600 dark:text-blue-400'
                        : 'border-transparent text-gray-500 dark:text-gray-400 hover:text-gray-700 dark:hover:text-gray-300 hover:border-gray-300 dark:hover:border-gray-600'
                    } whitespace-nowrap py-4 px-1 border-b-2 font-medium text-sm transition-colors`}
                  >
                    Available Organizations
                    {availableOrganizations.length > 0 && (
                      <span className={`ml-2 ${
                        activeTab === 'available'
                          ? 'bg-blue-100 text-blue-600 dark:bg-blue-900 dark:text-blue-400'
                          : 'bg-gray-100 text-gray-600 dark:bg-gray-800 dark:text-gray-400'
                      } py-0.5 px-2.5 rounded-full text-xs font-medium`}>
                        {availableOrganizations.length}
                      </span>
                    )}
                  </button>
                </nav>
              </div>
            </div>

            {/* Tab Content */}
            {activeTab === 'my-orgs' && (
              <div>
                {myOrganizations.length === 0 ? (
                  <div className="text-center py-12">
                    <svg
                      className="mx-auto h-12 w-12 text-gray-400 mb-4"
                      fill="none"
                      stroke="currentColor"
                      viewBox="0 0 24 24"
                    >
                      <path
                        strokeLinecap="round"
                        strokeLinejoin="round"
                        strokeWidth={2}
                        d="M19 21V5a2 2 0 00-2-2H7a2 2 0 00-2 2v16m14 0h2m-2 0h-5m-9 0H3m2 0h5M9 7h1m-1 4h1m4-4h1m-1 4h1m-5 10v-5a1 1 0 011-1h2a1 1 0 011 1v5m-4 0h4"
                      />
                    </svg>
                    <h3 className="text-lg font-medium text-gray-900 dark:text-white mb-2">
                      No Organizations Yet
                    </h3>
                    <p className="text-gray-500 dark:text-gray-400 mb-4">
                      You don't have access to any organizations yet.
                    </p>
                    <button
                      onClick={() => setActiveTab('available')}
                      className="text-blue-600 dark:text-blue-400 hover:text-blue-700 dark:hover:text-blue-300 font-medium"
                    >
                      Browse available organizations →
                    </button>
                  </div>
                ) : (
                  <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-6">
                    {myOrganizations.map((org) => (
                      <button
                        key={org.organizationId}
                        onClick={() => handleSelectOrganization(org.organizationId)}
                        disabled={selecting}
                        className="group relative bg-white dark:bg-gray-800 rounded-lg shadow-lg hover:shadow-xl transition-all duration-200 overflow-hidden border-2 border-transparent hover:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:ring-offset-2 disabled:opacity-50 disabled:cursor-not-allowed"
                      >
                        <div className="aspect-square w-full flex items-center justify-center p-8 bg-gradient-to-br from-gray-50 to-gray-100 dark:from-gray-700 dark:to-gray-800">
                          {org.logoUrl ? (
                            <img
                              src={`${org.logoUrl}`}
                              alt={`${org.name} logo`}
                              className="max-w-full max-h-full object-contain group-hover:scale-105 transition-transform duration-200"
                            />
                          ) : (
                            <div className="flex items-center justify-center w-full h-full bg-gradient-to-br from-blue-500 to-indigo-600 rounded-lg">
                              <span className="text-white text-5xl font-bold">
                                {org.name.charAt(0).toUpperCase()}
                              </span>
                            </div>
                          )}
                        </div>
                        <div className="p-4 bg-white dark:bg-gray-800 border-t border-gray-200 dark:border-gray-700">
                          <h3 className="text-lg font-semibold text-gray-900 dark:text-white text-center truncate">
                            {org.name}
                          </h3>
                          {org.description && (
                            <p className="mt-1 text-sm text-gray-500 dark:text-gray-400 text-center line-clamp-2">
                              {org.description}
                            </p>
                          )}
                          <div className="mt-2 flex justify-center">
                            <span className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ${
                              org.role === 'ORG_ADMIN'
                                ? 'bg-purple-100 text-purple-800 dark:bg-purple-900 dark:text-purple-200'
                                : 'bg-blue-100 text-blue-800 dark:bg-blue-900 dark:text-blue-200'
                            }`}>
                              {org.role === 'ORG_ADMIN' ? 'Admin' : 'User'}
                            </span>
                          </div>
                        </div>
                        {selecting && (
                          <div className="absolute inset-0 bg-white/75 dark:bg-gray-900/75 flex items-center justify-center">
                            <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600"></div>
                          </div>
                        )}
                      </button>
                    ))}
                  </div>
                )}
              </div>
            )}

            {activeTab === 'available' && (
              <div>
                {availableOrganizations.length === 0 ? (
                  <div className="text-center py-12">
                    <svg
                      className="mx-auto h-12 w-12 text-gray-400 mb-4"
                      fill="none"
                      stroke="currentColor"
                      viewBox="0 0 24 24"
                    >
                      <path
                        strokeLinecap="round"
                        strokeLinejoin="round"
                        strokeWidth={2}
                        d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z"
                      />
                    </svg>
                    <h3 className="text-lg font-medium text-gray-900 dark:text-white mb-2">
                      All Caught Up!
                    </h3>
                    <p className="text-gray-500 dark:text-gray-400">
                      You already have access to all available organizations.
                    </p>
                  </div>
                ) : (
                  <>
                    <p className="text-gray-600 dark:text-gray-400 mb-6">
                      Request access to join these organizations
                    </p>
                    <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-6">
                      {availableOrganizations.map((org) => (
                        <div
                          key={org.organizationId}
                          className="group relative bg-white dark:bg-gray-800 rounded-lg shadow-lg transition-all duration-200 overflow-hidden border-2 border-gray-200 dark:border-gray-700"
                        >
                          <div className="aspect-square w-full flex items-center justify-center p-8 bg-gradient-to-br from-gray-50 to-gray-100 dark:from-gray-700 dark:to-gray-800">
                            {org.logoUrl ? (
                              <img
                                src={`${org.logoUrl}`}
                                alt={`${org.name} logo`}
                                className="max-w-full max-h-full object-contain"
                              />
                            ) : (
                              <div className="flex items-center justify-center w-full h-full bg-gradient-to-br from-gray-400 to-gray-500 rounded-lg">
                                <span className="text-white text-5xl font-bold">
                                  {org.name.charAt(0).toUpperCase()}
                                </span>
                              </div>
                            )}
                          </div>
                          <div className="p-4 bg-white dark:bg-gray-800 border-t border-gray-200 dark:border-gray-700">
                            <h3 className="text-lg font-semibold text-gray-900 dark:text-white text-center truncate">
                              {org.name}
                            </h3>
                            {org.description && (
                              <p className="mt-1 text-sm text-gray-500 dark:text-gray-400 text-center line-clamp-2">
                                {org.description}
                              </p>
                            )}
                            <div className="mt-3 flex justify-center">
                              <button
                                onClick={() => handleRequestAccess(org.organizationId)}
                                className="px-4 py-2 text-sm font-medium text-blue-600 dark:text-blue-400 hover:text-blue-700 dark:hover:text-blue-300 border border-blue-600 dark:border-blue-400 hover:border-blue-700 dark:hover:border-blue-300 rounded-lg transition-colors"
                              >
                                Request Access
                              </button>
                            </div>
                          </div>
                        </div>
                      ))}
                    </div>
                  </>
                )}
              </div>
            )}
          </>
        )}
      </div>
    </div>
  );
}
