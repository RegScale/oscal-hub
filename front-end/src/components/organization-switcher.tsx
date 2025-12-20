'use client';

import { useEffect, useState, useRef } from 'react';
import { useRouter } from 'next/navigation';
import { apiClient } from '@/lib/api-client';
import { useAuth } from '@/contexts/AuthContext';

interface Organization {
  organizationId: number;
  name: string;
  description: string | null;
  logoUrl: string | null;
  role: string;
  joinedAt: string;
}

export function OrganizationSwitcher() {
  const router = useRouter();
  const { updateUser } = useAuth();
  const [currentOrg, setCurrentOrg] = useState<string | null>(null);
  const [currentOrgId, setCurrentOrgId] = useState<number | null>(null);
  const [organizations, setOrganizations] = useState<Organization[]>([]);
  const [loading, setLoading] = useState(false);
  const [switching, setSwitching] = useState(false);
  const [isOpen, setIsOpen] = useState(false);
  const dropdownRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    loadCurrentOrganization();
    loadOrganizations();
  }, []);

  // Close dropdown when clicking outside
  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (dropdownRef.current && !dropdownRef.current.contains(event.target as Node)) {
        setIsOpen(false);
      }
    };

    if (isOpen) {
      document.addEventListener('mousedown', handleClickOutside);
      return () => document.removeEventListener('mousedown', handleClickOutside);
    }
  }, [isOpen]);

  const loadCurrentOrganization = () => {
    const user = localStorage.getItem('user');
    if (user) {
      try {
        const userData = JSON.parse(user);
        setCurrentOrg(userData.organizationName || userData.organization);
        setCurrentOrgId(userData.organizationId);
      } catch (error) {
        console.error('Failed to parse user data:', error);
      }
    }
  };

  const loadOrganizations = async () => {
    try {
      setLoading(true);
      const orgs = await apiClient.getMyOrganizations();
      setOrganizations(orgs);
    } catch (error) {
      console.error('Failed to load organizations:', error);
    } finally {
      setLoading(false);
    }
  };

  const handleSwitchOrganization = async (orgId: number, orgName: string) => {
    if (orgId === currentOrgId) {
      setIsOpen(false);
      return; // Already on this organization
    }

    try {
      setSwitching(true);
      setIsOpen(false);
      const result = await apiClient.switchOrganization(orgId);

      // Update local state
      setCurrentOrg(result.organizationName);
      setCurrentOrgId(result.organizationId);

      // Sync the AuthContext with the new user data from localStorage
      updateUser();

      // Check if password change is required
      if (result.mustChangePassword) {
        router.push('/change-password');
        return;
      }

      // Reload the current page to refresh data with new organization context
      router.refresh();
    } catch (error: any) {
      console.error('Failed to switch organization:', error);
      alert(error.message || 'Failed to switch organization');
    } finally {
      setSwitching(false);
    }
  };

  const handleSelectOrganization = async (orgId: number) => {
    try {
      setSwitching(true);
      setIsOpen(false);
      const result = await apiClient.selectOrganization(orgId);

      // Update local state
      setCurrentOrg(result.organizationName);
      setCurrentOrgId(result.organizationId);

      // Sync the AuthContext with the new user data from localStorage
      updateUser();

      // Check if password change is required
      if (result.mustChangePassword) {
        router.push('/change-password');
        return;
      }

      // Reload the current page to refresh data with new organization context
      router.refresh();
    } catch (error: any) {
      console.error('Failed to select organization:', error);
      alert(error.message || 'Failed to select organization');
    } finally {
      setSwitching(false);
    }
  };

  const handleRequestAccess = () => {
    setIsOpen(false);
    router.push('/request-access');
  };

  return (
    <div className="relative" ref={dropdownRef}>
      <button
        onClick={() => setIsOpen(!isOpen)}
        disabled={switching}
        className={`flex items-center space-x-2 px-3 py-2 text-sm border rounded-md disabled:opacity-50 disabled:cursor-not-allowed min-w-[200px] justify-between ${
          !currentOrg
            ? 'border-yellow-300 dark:border-yellow-700 bg-yellow-50 dark:bg-yellow-900/20 hover:bg-yellow-100 dark:hover:bg-yellow-900/30'
            : 'border-gray-300 dark:border-gray-700 hover:bg-gray-50 dark:hover:bg-gray-800'
        }`}
      >
        <div className="flex items-center space-x-2">
          <svg className={`h-4 w-4 ${!currentOrg ? 'text-yellow-600 dark:text-yellow-400' : ''}`} fill="none" stroke="currentColor" viewBox="0 0 24 24" aria-hidden="true">
            {!currentOrg ? (
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
            ) : (
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 21V5a2 2 0 00-2-2H7a2 2 0 00-2 2v16m14 0h2m-2 0h-5m-9 0H3m2 0h5M9 7h1m-1 4h1m4-4h1m-1 4h1m-5 10v-5a1 1 0 011-1h2a1 1 0 011 1v5m-4 0h4" />
            )}
          </svg>
          <span className={`font-medium truncate max-w-[150px] ${!currentOrg ? 'text-yellow-700 dark:text-yellow-300' : ''}`}>
            {currentOrg || 'Select Organization'}
          </span>
        </div>
        <svg className={`h-4 w-4 transition-transform ${isOpen ? 'rotate-180' : ''} ${!currentOrg ? 'text-yellow-600 dark:text-yellow-400' : ''}`} fill="none" stroke="currentColor" viewBox="0 0 24 24" aria-hidden="true">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 9l-7 7-7-7" />
        </svg>
      </button>

      {isOpen && (
        <div className="absolute right-0 mt-2 w-[250px] bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-700 rounded-md shadow-lg z-50">
          <div className="px-3 py-2 text-sm font-semibold border-b border-gray-200 dark:border-gray-700">
            {currentOrg ? 'Switch Organization' : 'Select Organization'}
          </div>

          <div className="py-1 max-h-[300px] overflow-y-auto">
            {loading ? (
              <div className="px-3 py-2 text-sm text-gray-500 dark:text-gray-400">
                Loading...
              </div>
            ) : organizations.length === 0 ? (
              <div className="px-3 py-2 text-sm text-gray-500 dark:text-gray-400">
                No organizations
              </div>
            ) : (
              organizations.map((org) => (
                <button
                  key={org.organizationId}
                  onClick={() => {
                    if (currentOrg) {
                      handleSwitchOrganization(org.organizationId, org.name);
                    } else {
                      handleSelectOrganization(org.organizationId);
                    }
                  }}
                  className="w-full px-3 py-2 text-left hover:bg-gray-100 dark:hover:bg-gray-700 cursor-pointer"
                >
                  <div className="flex items-center justify-between w-full">
                    <div className="flex items-center space-x-2 flex-1 min-w-0">
                      {org.logoUrl ? (
                        <img
                          src={`${org.logoUrl}`}
                          alt={org.name}
                          className="h-5 w-5 object-contain rounded"
                        />
                      ) : (
                        <div className="h-5 w-5 bg-gradient-to-br from-blue-500 to-indigo-600 rounded flex items-center justify-center">
                          <span className="text-white text-xs font-bold">
                            {org.name.charAt(0).toUpperCase()}
                          </span>
                        </div>
                      )}
                      <div className="flex flex-col min-w-0">
                        <span className="text-sm font-medium truncate">{org.name}</span>
                        <span className="text-xs text-gray-500 dark:text-gray-400">
                          {org.role === 'ORG_ADMIN' ? 'Admin' : 'User'}
                        </span>
                      </div>
                    </div>
                    {org.organizationId === currentOrgId && (
                      <svg className="h-4 w-4 text-blue-600 ml-2 flex-shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24" aria-hidden="true">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 13l4 4L19 7" />
                      </svg>
                    )}
                  </div>
                </button>
              ))
            )}
          </div>

          <div className="border-t border-gray-200 dark:border-gray-700">
            <button
              onClick={handleRequestAccess}
              className="w-full px-3 py-2 text-left hover:bg-gray-100 dark:hover:bg-gray-700 cursor-pointer"
            >
              <div className="flex items-center space-x-2 text-gray-600 dark:text-gray-400">
                <svg className="h-4 w-4" fill="none" stroke="currentColor" viewBox="0 0 24 24" aria-hidden="true">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M18 9v3m0 0v3m0-3h3m-3 0h-3m-2-5a4 4 0 11-8 0 4 4 0 018 0zM3 20a6 6 0 0112 0v1H3v-1z" />
                </svg>
                <span className="text-sm">Request Access</span>
              </div>
            </button>
          </div>
        </div>
      )}
    </div>
  );
}
