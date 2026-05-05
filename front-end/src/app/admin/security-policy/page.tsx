'use client';

import { useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import { ArrowLeft, KeyRound, Shield, Lock, Calendar, Trash2, Save, Loader2, CheckCircle2, AlertCircle } from 'lucide-react';
import { apiClient } from '@/lib/api-client';
import type { SecurityPolicy, SecurityPolicyUpdateRequest } from '@/types/oscal';
import { HelpButton } from '@/components/HelpButton';

export default function SecurityPolicyPage() {
  const router = useRouter();
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [cleaningUp, setCleaningUp] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);
  const [policy, setPolicy] = useState<SecurityPolicy | null>(null);

  // Form state
  const [mfaRequired, setMfaRequired] = useState(false);
  const [passwordMinLength, setPasswordMinLength] = useState(10);
  const [passwordMaxLength, setPasswordMaxLength] = useState(128);
  const [passwordRotationDays, setPasswordRotationDays] = useState(0);
  const [auditLogRetentionDays, setAuditLogRetentionDays] = useState(90);

  useEffect(() => {
    // Verify user is super admin
    const storedUser = localStorage.getItem('user');
    if (storedUser) {
      const userData = JSON.parse(storedUser);
      if (userData.globalRole !== 'SUPER_ADMIN') {
        router.push('/');
        return;
      }
    } else {
      router.push('/login');
      return;
    }

    // Fetch security policy
    const fetchPolicy = async () => {
      try {
        const policyData = await apiClient.getSecurityPolicy();
        setPolicy(policyData);
        setMfaRequired(policyData.mfaRequired);
        setPasswordMinLength(policyData.passwordMinLength);
        setPasswordMaxLength(policyData.passwordMaxLength);
        setPasswordRotationDays(policyData.passwordRotationDays);
        setAuditLogRetentionDays(policyData.auditLogRetentionDays);
      } catch (err) {
        setError(err instanceof Error ? err.message : 'Failed to load security policy');
      } finally {
        setLoading(false);
      }
    };

    fetchPolicy();
  }, [router]);

  const handleSave = async () => {
    setError(null);
    setSuccess(null);
    setSaving(true);

    try {
      // Validate
      if (passwordMinLength > passwordMaxLength) {
        throw new Error('Minimum password length cannot be greater than maximum');
      }

      const request: SecurityPolicyUpdateRequest = {
        mfaRequired,
        passwordMinLength,
        passwordMaxLength,
        passwordRotationDays,
        auditLogRetentionDays,
      };

      const updatedPolicy = await apiClient.updateSecurityPolicy(request);
      setPolicy(updatedPolicy);
      setSuccess('Security policy updated successfully');

      // Clear success message after 3 seconds
      setTimeout(() => setSuccess(null), 3000);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to update security policy');
    } finally {
      setSaving(false);
    }
  };

  const handleCleanup = async () => {
    setError(null);
    setSuccess(null);
    setCleaningUp(true);

    try {
      const result = await apiClient.triggerAuditLogCleanup();
      setSuccess(`Cleanup completed: ${result.deletedCount} events deleted (retention: ${result.retentionDays} days)`);

      // Clear success message after 5 seconds
      setTimeout(() => setSuccess(null), 5000);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to trigger audit log cleanup');
    } finally {
      setCleaningUp(false);
    }
  };

  if (loading) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-gray-50 dark:bg-gray-900">
        <div className="text-center">
          <div className="animate-spin rounded-full h-16 w-16 border-b-2 border-rose-600 mx-auto"></div>
          <p className="mt-4 text-gray-600 dark:text-gray-400">Loading security policy...</p>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gray-50 dark:bg-gray-900 py-8 px-4 sm:px-6 lg:px-8">
      <div className="max-w-3xl mx-auto">
        {/* Header */}
        <div className="mb-8">
          <button
            onClick={() => router.push('/admin')}
            className="flex items-center text-gray-600 dark:text-gray-400 hover:text-gray-900 dark:hover:text-white mb-4"
          >
            <ArrowLeft className="w-4 h-4 mr-2" />
            Back to Admin Dashboard
          </button>
          <div className="flex items-center">
            <div className="flex items-center justify-center w-12 h-12 bg-rose-100 dark:bg-rose-900/30 rounded-lg mr-4">
              <KeyRound className="w-6 h-6 text-rose-600 dark:text-rose-400" />
            </div>
            <div>
              <div className="flex items-center gap-2">
                <h1 className="text-2xl font-bold text-gray-900 dark:text-white">
                  Security Policy Settings
                </h1>
                <HelpButton slug="admin-security-policy" />
              </div>
              <p className="text-gray-600 dark:text-gray-400">
                Configure MFA, password policies, and audit log retention
              </p>
            </div>
          </div>
        </div>

        {/* Alerts */}
        {error && (
          <div className="mb-6 bg-red-50 dark:bg-red-900/30 border border-red-200 dark:border-red-800 rounded-lg p-4 flex items-center">
            <AlertCircle className="w-5 h-5 text-red-600 dark:text-red-400 mr-3" />
            <span className="text-red-800 dark:text-red-200">{error}</span>
          </div>
        )}

        {success && (
          <div className="mb-6 bg-green-50 dark:bg-green-900/30 border border-green-200 dark:border-green-800 rounded-lg p-4 flex items-center">
            <CheckCircle2 className="w-5 h-5 text-green-600 dark:text-green-400 mr-3" />
            <span className="text-green-800 dark:text-green-200">{success}</span>
          </div>
        )}

        {/* MFA Policy Card */}
        <div className="bg-white dark:bg-gray-800 rounded-lg shadow-md border border-gray-200 dark:border-gray-700 p-6 mb-6">
          <div className="flex items-center mb-4">
            <Shield className="w-5 h-5 text-rose-600 dark:text-rose-400 mr-2" />
            <h2 className="text-lg font-semibold text-gray-900 dark:text-white">
              MFA Policy
            </h2>
          </div>
          <div className="flex items-center justify-between">
            <div>
              <p className="text-gray-700 dark:text-gray-300 font-medium">
                Require MFA for all users
              </p>
              <p className="text-sm text-gray-500 dark:text-gray-400">
                When enabled, users must configure an authenticator app to access the platform
              </p>
            </div>
            <button
              onClick={() => setMfaRequired(!mfaRequired)}
              className={`relative inline-flex h-6 w-11 items-center rounded-full transition-colors ${
                mfaRequired ? 'bg-rose-600' : 'bg-gray-300 dark:bg-gray-600'
              }`}
            >
              <span
                className={`inline-block h-4 w-4 transform rounded-full bg-white transition-transform ${
                  mfaRequired ? 'translate-x-6' : 'translate-x-1'
                }`}
              />
            </button>
          </div>
        </div>

        {/* Password Policy Card */}
        <div className="bg-white dark:bg-gray-800 rounded-lg shadow-md border border-gray-200 dark:border-gray-700 p-6 mb-6">
          <div className="flex items-center mb-4">
            <Lock className="w-5 h-5 text-rose-600 dark:text-rose-400 mr-2" />
            <h2 className="text-lg font-semibold text-gray-900 dark:text-white">
              Password Policy
            </h2>
          </div>

          <div className="space-y-6">
            {/* Minimum Length */}
            <div>
              <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
                Minimum Password Length
              </label>
              <div className="flex items-center space-x-4">
                <input
                  type="range"
                  min="8"
                  max="32"
                  value={passwordMinLength}
                  onChange={(e) => setPasswordMinLength(parseInt(e.target.value))}
                  className="flex-1 h-2 bg-gray-200 dark:bg-gray-700 rounded-lg appearance-none cursor-pointer accent-rose-600"
                />
                <div className="w-20 text-center">
                  <span className="inline-block px-3 py-1 bg-rose-100 dark:bg-rose-900/30 text-rose-700 dark:text-rose-300 rounded-md font-medium">
                    {passwordMinLength}
                  </span>
                </div>
              </div>
              <p className="mt-1 text-xs text-gray-500 dark:text-gray-400">
                Minimum: 8 characters
              </p>
            </div>

            {/* Maximum Length */}
            <div>
              <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
                Maximum Password Length
              </label>
              <div className="flex items-center space-x-4">
                <input
                  type="range"
                  min="32"
                  max="128"
                  value={passwordMaxLength}
                  onChange={(e) => setPasswordMaxLength(parseInt(e.target.value))}
                  className="flex-1 h-2 bg-gray-200 dark:bg-gray-700 rounded-lg appearance-none cursor-pointer accent-rose-600"
                />
                <div className="w-20 text-center">
                  <span className="inline-block px-3 py-1 bg-rose-100 dark:bg-rose-900/30 text-rose-700 dark:text-rose-300 rounded-md font-medium">
                    {passwordMaxLength}
                  </span>
                </div>
              </div>
              <p className="mt-1 text-xs text-gray-500 dark:text-gray-400">
                Maximum: 128 characters
              </p>
            </div>

            {/* Password Rotation */}
            <div>
              <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
                Password Rotation (days)
              </label>
              <div className="flex items-center space-x-4">
                <input
                  type="number"
                  min="0"
                  max="365"
                  value={passwordRotationDays}
                  onChange={(e) => setPasswordRotationDays(parseInt(e.target.value) || 0)}
                  className="w-24 px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-md bg-white dark:bg-gray-700 text-gray-900 dark:text-white focus:ring-2 focus:ring-rose-500 focus:border-rose-500"
                />
                <span className="text-gray-600 dark:text-gray-400">days</span>
              </div>
              <p className="mt-1 text-xs text-gray-500 dark:text-gray-400">
                Set to 0 to disable password expiration. Maximum: 365 days.
              </p>
              {passwordRotationDays === 0 && (
                <p className="mt-1 text-xs text-amber-600 dark:text-amber-400">
                  Password rotation is currently disabled
                </p>
              )}
            </div>
          </div>
        </div>

        {/* Audit Log Retention Card */}
        <div className="bg-white dark:bg-gray-800 rounded-lg shadow-md border border-gray-200 dark:border-gray-700 p-6 mb-6">
          <div className="flex items-center mb-4">
            <Calendar className="w-5 h-5 text-rose-600 dark:text-rose-400 mr-2" />
            <h2 className="text-lg font-semibold text-gray-900 dark:text-white">
              Audit Log Retention
            </h2>
          </div>

          <div className="space-y-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
                Retention Period (days)
              </label>
              <div className="flex items-center space-x-4">
                <input
                  type="number"
                  min="1"
                  max="3650"
                  value={auditLogRetentionDays}
                  onChange={(e) => setAuditLogRetentionDays(parseInt(e.target.value) || 1)}
                  className="w-24 px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-md bg-white dark:bg-gray-700 text-gray-900 dark:text-white focus:ring-2 focus:ring-rose-500 focus:border-rose-500"
                />
                <span className="text-gray-600 dark:text-gray-400">days</span>
              </div>
              <p className="mt-1 text-xs text-gray-500 dark:text-gray-400">
                Audit logs older than this period will be automatically deleted. Minimum: 1 day, Maximum: 10 years (3650 days).
              </p>
            </div>

            {/* Manual Cleanup Button */}
            <div className="pt-4 border-t border-gray-200 dark:border-gray-700">
              <button
                onClick={handleCleanup}
                disabled={cleaningUp}
                className="inline-flex items-center px-4 py-2 border border-gray-300 dark:border-gray-600 rounded-md text-sm font-medium text-gray-700 dark:text-gray-300 bg-white dark:bg-gray-700 hover:bg-gray-50 dark:hover:bg-gray-600 disabled:opacity-50 disabled:cursor-not-allowed"
              >
                {cleaningUp ? (
                  <>
                    <Loader2 className="w-4 h-4 mr-2 animate-spin" />
                    Running cleanup...
                  </>
                ) : (
                  <>
                    <Trash2 className="w-4 h-4 mr-2" />
                    Run Manual Cleanup
                  </>
                )}
              </button>
              <p className="mt-2 text-xs text-gray-500 dark:text-gray-400">
                Immediately delete audit logs older than the retention period
              </p>
            </div>
          </div>
        </div>

        {/* Save Button */}
        <div className="flex justify-end space-x-4">
          <button
            onClick={() => router.push('/admin')}
            className="px-6 py-2 border border-gray-300 dark:border-gray-600 rounded-md text-gray-700 dark:text-gray-300 hover:bg-gray-50 dark:hover:bg-gray-700"
          >
            Cancel
          </button>
          <button
            onClick={handleSave}
            disabled={saving}
            className="inline-flex items-center px-6 py-2 bg-rose-600 hover:bg-rose-700 text-white font-medium rounded-md disabled:opacity-50 disabled:cursor-not-allowed"
          >
            {saving ? (
              <>
                <Loader2 className="w-4 h-4 mr-2 animate-spin" />
                Saving...
              </>
            ) : (
              <>
                <Save className="w-4 h-4 mr-2" />
                Save Changes
              </>
            )}
          </button>
        </div>

        {/* Last Updated Info */}
        {policy && policy.updatedAt && (
          <div className="mt-6 text-center text-sm text-gray-500 dark:text-gray-400">
            Last updated: {new Date(policy.updatedAt).toLocaleString()}
            {policy.updatedBy && ` by ${policy.updatedBy}`}
          </div>
        )}
      </div>
    </div>
  );
}
