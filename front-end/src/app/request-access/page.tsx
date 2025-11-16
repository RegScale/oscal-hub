'use client';

import { useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import { apiClient } from '@/lib/api-client';
import { ArrowLeft, Building2, CheckCircle } from 'lucide-react';
import { Button } from '@/components/ui/button';

interface Organization {
  organizationId: number;
  name: string;
  description: string | null;
  logoUrl: string | null;
}

export default function RequestAccessPage() {
  const router = useRouter();
  const [organizations, setOrganizations] = useState<Organization[]>([]);
  const [selectedOrgId, setSelectedOrgId] = useState<number | null>(null);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [submitted, setSubmitted] = useState(false);
  const [error, setError] = useState<string | null>(null);

  // Form fields
  const [firstName, setFirstName] = useState('');
  const [lastName, setLastName] = useState('');
  const [email, setEmail] = useState('');
  const [username, setUsername] = useState('');
  const [message, setMessage] = useState('');

  useEffect(() => {
    loadOrganizations();
    // Pre-fill user info if authenticated
    const storedUser = localStorage.getItem('user');
    if (storedUser) {
      try {
        const user = JSON.parse(storedUser);
        setEmail(user.email || '');
        setUsername(user.username || '');
      } catch (err) {
        console.error('Failed to parse user data:', err);
      }
    }
  }, []);

  const loadOrganizations = async () => {
    try {
      setLoading(true);
      setError(null);

      // Get all organizations
      const allOrgs = await apiClient.getOrganizations();

      // Get user's current organizations to filter them out
      const token = localStorage.getItem('token');
      let myOrgIds: number[] = [];

      if (token) {
        try {
          const myOrgs = await apiClient.getMyOrganizations();
          myOrgIds = myOrgs.map(org => org.organizationId);
        } catch (err) {
          // User might not be authenticated, continue anyway
          console.log('Could not load user organizations:', err);
        }
      }

      // Filter out organizations the user is already a member of
      const availableOrgs = allOrgs.filter(org => !myOrgIds.includes(org.organizationId));
      setOrganizations(availableOrgs);
    } catch (err) {
      console.error('Failed to load organizations:', err);
      setError('Failed to load organizations. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);

    if (!selectedOrgId) {
      setError('Please select an organization');
      return;
    }

    if (!firstName.trim() || !lastName.trim() || !email.trim()) {
      setError('Please fill in all required fields');
      return;
    }

    // Basic email validation
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (!emailRegex.test(email)) {
      setError('Please enter a valid email address');
      return;
    }

    try {
      setSubmitting(true);
      await apiClient.requestAccess({
        organizationId: selectedOrgId,
        firstName: firstName.trim(),
        lastName: lastName.trim(),
        email: email.trim(),
        username: username.trim() || undefined,
        message: message.trim() || undefined,
      });

      setSubmitted(true);
    } catch (err: any) {
      console.error('Failed to submit access request:', err);
      setError(err.message || 'Failed to submit access request. Please try again.');
    } finally {
      setSubmitting(false);
    }
  };

  const handleBack = () => {
    const token = localStorage.getItem('token');
    if (token) {
      router.push('/select-organization');
    } else {
      router.push('/login');
    }
  };

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

  if (submitted) {
    return (
      <div className="min-h-screen bg-gradient-to-br from-blue-50 to-indigo-100 dark:from-gray-900 dark:to-gray-800 py-12 px-4 sm:px-6 lg:px-8">
        <div className="max-w-2xl mx-auto">
          <div className="bg-white dark:bg-gray-800 rounded-lg shadow-lg p-8 text-center">
            <CheckCircle className="h-16 w-16 text-green-500 mx-auto mb-4" />
            <h2 className="text-2xl font-bold text-gray-900 dark:text-white mb-2">
              Request Submitted Successfully
            </h2>
            <p className="text-gray-600 dark:text-gray-400 mb-6">
              Your access request has been submitted to the organization administrators.
              You will be notified via email once your request has been reviewed.
            </p>
            <Button onClick={handleBack}>
              Back to Login
            </Button>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gradient-to-br from-blue-50 to-indigo-100 dark:from-gray-900 dark:to-gray-800 py-12 px-4 sm:px-6 lg:px-8">
      <div className="max-w-4xl mx-auto">
        <div className="mb-6">
          <button
            onClick={handleBack}
            className="flex items-center space-x-2 text-gray-600 dark:text-gray-400 hover:text-gray-900 dark:hover:text-white transition-colors"
          >
            <ArrowLeft className="h-4 w-4" />
            <span>Back</span>
          </button>
        </div>

        <div className="text-center mb-8">
          <h1 className="text-4xl font-bold text-gray-900 dark:text-white mb-2">
            Request Organization Access
          </h1>
          <p className="text-lg text-gray-600 dark:text-gray-400">
            Select an organization and submit your access request
          </p>
        </div>

        {error && (
          <div className="mb-6 bg-red-50 dark:bg-red-900/20 border border-red-200 dark:border-red-800 rounded-lg p-4">
            <p className="text-red-800 dark:text-red-200">{error}</p>
          </div>
        )}

        <div className="bg-white dark:bg-gray-800 rounded-lg shadow-lg p-8">
          <form onSubmit={handleSubmit}>
            {/* Organization Selection */}
            <div className="mb-8">
              <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-4">
                Select Organization *
              </label>
              <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
                {organizations.map((org) => (
                  <button
                    key={org.organizationId}
                    type="button"
                    onClick={() => setSelectedOrgId(org.organizationId)}
                    className={`p-4 rounded-lg border-2 transition-all ${
                      selectedOrgId === org.organizationId
                        ? 'border-blue-500 bg-blue-50 dark:bg-blue-900/20'
                        : 'border-gray-200 dark:border-gray-700 hover:border-blue-300 dark:hover:border-blue-700'
                    }`}
                  >
                    <div className="flex flex-col items-center text-center">
                      {org.logoUrl ? (
                        <img
                          src={`http://localhost:8080${org.logoUrl}`}
                          alt={org.name}
                          className="h-12 w-12 object-contain mb-2"
                        />
                      ) : (
                        <div className="h-12 w-12 bg-gradient-to-br from-blue-500 to-indigo-600 rounded-lg flex items-center justify-center mb-2">
                          <span className="text-white text-xl font-bold">
                            {org.name.charAt(0).toUpperCase()}
                          </span>
                        </div>
                      )}
                      <span className="text-sm font-medium text-gray-900 dark:text-white">
                        {org.name}
                      </span>
                      {org.description && (
                        <span className="text-xs text-gray-500 dark:text-gray-400 mt-1 line-clamp-2">
                          {org.description}
                        </span>
                      )}
                    </div>
                  </button>
                ))}
              </div>
            </div>

            {/* Personal Information */}
            <div className="grid grid-cols-1 md:grid-cols-2 gap-6 mb-6">
              <div>
                <label
                  htmlFor="firstName"
                  className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2"
                >
                  First Name *
                </label>
                <input
                  type="text"
                  id="firstName"
                  value={firstName}
                  onChange={(e) => setFirstName(e.target.value)}
                  required
                  className="w-full px-4 py-2 border border-gray-300 dark:border-gray-700 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent bg-white dark:bg-gray-900 text-gray-900 dark:text-white"
                  placeholder="John"
                />
              </div>

              <div>
                <label
                  htmlFor="lastName"
                  className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2"
                >
                  Last Name *
                </label>
                <input
                  type="text"
                  id="lastName"
                  value={lastName}
                  onChange={(e) => setLastName(e.target.value)}
                  required
                  className="w-full px-4 py-2 border border-gray-300 dark:border-gray-700 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent bg-white dark:bg-gray-900 text-gray-900 dark:text-white"
                  placeholder="Doe"
                />
              </div>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-2 gap-6 mb-6">
              <div>
                <label
                  htmlFor="email"
                  className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2"
                >
                  Email Address *
                </label>
                <input
                  type="email"
                  id="email"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  required
                  className="w-full px-4 py-2 border border-gray-300 dark:border-gray-700 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent bg-white dark:bg-gray-900 text-gray-900 dark:text-white"
                  placeholder="john.doe@example.com"
                />
              </div>

              <div>
                <label
                  htmlFor="username"
                  className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2"
                >
                  Username (Optional)
                </label>
                <input
                  type="text"
                  id="username"
                  value={username}
                  onChange={(e) => setUsername(e.target.value)}
                  className="w-full px-4 py-2 border border-gray-300 dark:border-gray-700 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent bg-white dark:bg-gray-900 text-gray-900 dark:text-white"
                  placeholder="johndoe"
                />
              </div>
            </div>

            <div className="mb-6">
              <label
                htmlFor="message"
                className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2"
              >
                Message (Optional)
              </label>
              <textarea
                id="message"
                value={message}
                onChange={(e) => setMessage(e.target.value)}
                rows={4}
                className="w-full px-4 py-2 border border-gray-300 dark:border-gray-700 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent bg-white dark:bg-gray-900 text-gray-900 dark:text-white"
                placeholder="Tell us why you need access to this organization..."
              />
            </div>

            <div className="flex justify-end">
              <Button
                type="submit"
                disabled={submitting}
                className="px-8 py-3"
              >
                {submitting ? 'Submitting...' : 'Submit Request'}
              </Button>
            </div>
          </form>
        </div>
      </div>
    </div>
  );
}
