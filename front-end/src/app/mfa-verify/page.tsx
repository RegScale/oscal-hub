'use client';

import { useState, useEffect, Suspense } from 'react';
import { useRouter, useSearchParams } from 'next/navigation';
import { Shield, Loader2, AlertCircle, KeyRound } from 'lucide-react';
import { apiClient } from '@/lib/api-client';
import { useAuth } from '@/contexts/AuthContext';

function MfaVerifyContent() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const mfaToken = searchParams.get('token');
  const { updateUser, user } = useAuth();

  const [verifying, setVerifying] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [totpCode, setTotpCode] = useState<string[]>(['', '', '', '', '', '']);
  const [useBackupCode, setUseBackupCode] = useState(false);
  const [backupCode, setBackupCode] = useState('');
  const [backupCodesWarning, setBackupCodesWarning] = useState<string | null>(null);

  useEffect(() => {
    // Redirect if no MFA token
    if (!mfaToken) {
      router.push('/login');
    }
  }, [mfaToken, router]);

  const handleCodeInput = (index: number, value: string) => {
    // Only allow digits
    if (value && !/^\d$/.test(value)) return;

    const newCode = [...totpCode];
    newCode[index] = value;
    setTotpCode(newCode);

    // Auto-advance to next input
    if (value && index < 5) {
      const nextInput = document.getElementById(`code-${index + 1}`);
      nextInput?.focus();
    }
  };

  const handleKeyDown = (index: number, e: React.KeyboardEvent) => {
    if (e.key === 'Backspace' && !totpCode[index] && index > 0) {
      const prevInput = document.getElementById(`code-${index - 1}`);
      prevInput?.focus();
    }
  };

  const handlePaste = (e: React.ClipboardEvent) => {
    e.preventDefault();
    const pasted = e.clipboardData.getData('text').replace(/\D/g, '').slice(0, 6);
    const newCode = [...totpCode];
    for (let i = 0; i < pasted.length; i++) {
      newCode[i] = pasted[i];
    }
    setTotpCode(newCode);
    // Focus last filled input
    if (pasted.length > 0) {
      const lastInput = document.getElementById(`code-${Math.min(pasted.length - 1, 5)}`);
      lastInput?.focus();
    }
  };

  const handleVerifyTotp = async () => {
    const code = totpCode.join('');
    if (code.length !== 6) {
      setError('Please enter a 6-digit code');
      return;
    }

    setError(null);
    setBackupCodesWarning(null);
    setVerifying(true);

    try {
      const response = await apiClient.verifyMfaCode({
        mfaToken: mfaToken!,
        totpCode: code,
      });

      // Sync AuthContext with the new token/user data from localStorage
      updateUser();

      // Redirect based on user role
      const storedUser = localStorage.getItem('user');
      if (storedUser) {
        const userData = JSON.parse(storedUser);
        if (userData.globalRole === 'SUPER_ADMIN') {
          router.push('/admin');
          return;
        }
      }
      router.push('/select-organization');
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Invalid code. Please try again.');
      setTotpCode(['', '', '', '', '', '']);
      document.getElementById('code-0')?.focus();
    } finally {
      setVerifying(false);
    }
  };

  const handleVerifyBackupCode = async () => {
    if (!backupCode.trim()) {
      setError('Please enter a backup code');
      return;
    }

    setError(null);
    setBackupCodesWarning(null);
    setVerifying(true);

    try {
      const response = await apiClient.verifyBackupCode({
        mfaToken: mfaToken!,
        backupCode: backupCode.trim(),
      });

      // Show warning if backup codes are low
      if (response.warning) {
        setBackupCodesWarning(response.warning);
        // Store warning to show after redirect
        sessionStorage.setItem('mfaBackupWarning', response.warning);
      }

      // Sync AuthContext with the new token/user data from localStorage
      updateUser();

      // Redirect based on user role
      const storedUser = localStorage.getItem('user');
      if (storedUser) {
        const userData = JSON.parse(storedUser);
        if (userData.globalRole === 'SUPER_ADMIN') {
          router.push('/admin');
          return;
        }
      }
      router.push('/select-organization');
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Invalid backup code. Please try again.');
      setBackupCode('');
    } finally {
      setVerifying(false);
    }
  };

  if (!mfaToken) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-gray-50 dark:bg-gray-900">
        <div className="text-center">
          <div className="animate-spin rounded-full h-16 w-16 border-b-2 border-blue-600 mx-auto"></div>
          <p className="mt-4 text-gray-600 dark:text-gray-400">Redirecting...</p>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen flex items-center justify-center bg-gray-50 dark:bg-gray-900 p-4">
      <div className="max-w-md w-full bg-white dark:bg-gray-800 rounded-lg shadow-lg p-8">
        {/* Header */}
        <div className="text-center mb-8">
          <div className="inline-flex items-center justify-center w-16 h-16 bg-blue-100 dark:bg-blue-900/30 rounded-full mb-4">
            <Shield className="w-8 h-8 text-blue-600 dark:text-blue-400" />
          </div>
          <h1 className="text-2xl font-bold text-gray-900 dark:text-white">
            Two-Factor Authentication
          </h1>
          <p className="mt-2 text-gray-600 dark:text-gray-400">
            Enter the code from your authenticator app
          </p>
        </div>

        {error && (
          <div className="mb-6 bg-red-50 dark:bg-red-900/30 border border-red-200 dark:border-red-800 rounded-lg p-4 flex items-center">
            <AlertCircle className="w-5 h-5 text-red-600 dark:text-red-400 mr-3 flex-shrink-0" />
            <span className="text-red-800 dark:text-red-200">{error}</span>
          </div>
        )}

        {backupCodesWarning && (
          <div className="mb-6 bg-amber-50 dark:bg-amber-900/30 border border-amber-200 dark:border-amber-800 rounded-lg p-4 flex items-center">
            <AlertCircle className="w-5 h-5 text-amber-600 dark:text-amber-400 mr-3 flex-shrink-0" />
            <span className="text-amber-800 dark:text-amber-200">{backupCodesWarning}</span>
          </div>
        )}

        {!useBackupCode ? (
          <>
            {/* TOTP Code Entry */}
            <div className="mb-6">
              <div className="flex justify-center space-x-2" onPaste={handlePaste}>
                {totpCode.map((digit, index) => (
                  <input
                    key={index}
                    id={`code-${index}`}
                    type="text"
                    inputMode="numeric"
                    maxLength={1}
                    value={digit}
                    onChange={(e) => handleCodeInput(index, e.target.value)}
                    onKeyDown={(e) => handleKeyDown(index, e)}
                    autoFocus={index === 0}
                    className="w-12 h-14 text-center text-xl font-semibold border border-gray-300 dark:border-gray-600 rounded-md bg-white dark:bg-gray-700 text-gray-900 dark:text-white focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
                  />
                ))}
              </div>
            </div>

            {/* Verify Button */}
            <button
              onClick={handleVerifyTotp}
              disabled={verifying || totpCode.join('').length !== 6}
              className="w-full px-4 py-3 bg-blue-600 hover:bg-blue-700 text-white font-medium rounded-md disabled:opacity-50 disabled:cursor-not-allowed flex items-center justify-center mb-4"
            >
              {verifying ? (
                <>
                  <Loader2 className="w-5 h-5 mr-2 animate-spin" />
                  Verifying...
                </>
              ) : (
                'Verify'
              )}
            </button>

            {/* Switch to backup code */}
            <div className="text-center">
              <button
                onClick={() => setUseBackupCode(true)}
                className="text-sm text-blue-600 dark:text-blue-400 hover:underline"
              >
                Lost your phone? Use a backup code
              </button>
            </div>
          </>
        ) : (
          <>
            {/* Backup Code Entry */}
            <div className="mb-6">
              <div className="flex items-center mb-3">
                <KeyRound className="w-5 h-5 text-gray-500 dark:text-gray-400 mr-2" />
                <label className="text-sm font-medium text-gray-700 dark:text-gray-300">
                  Backup Code
                </label>
              </div>
              <input
                type="text"
                value={backupCode}
                onChange={(e) => setBackupCode(e.target.value.toUpperCase())}
                placeholder="XXXX-XXXX"
                autoFocus
                className="w-full px-4 py-3 text-center text-lg font-mono border border-gray-300 dark:border-gray-600 rounded-md bg-white dark:bg-gray-700 text-gray-900 dark:text-white focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
              />
              <p className="mt-2 text-xs text-gray-500 dark:text-gray-400 text-center">
                Enter one of your backup codes (each code can only be used once)
              </p>
            </div>

            {/* Verify Backup Code Button */}
            <button
              onClick={handleVerifyBackupCode}
              disabled={verifying || !backupCode.trim()}
              className="w-full px-4 py-3 bg-blue-600 hover:bg-blue-700 text-white font-medium rounded-md disabled:opacity-50 disabled:cursor-not-allowed flex items-center justify-center mb-4"
            >
              {verifying ? (
                <>
                  <Loader2 className="w-5 h-5 mr-2 animate-spin" />
                  Verifying...
                </>
              ) : (
                'Verify Backup Code'
              )}
            </button>

            {/* Switch back to TOTP */}
            <div className="text-center">
              <button
                onClick={() => {
                  setUseBackupCode(false);
                  setBackupCode('');
                  setError(null);
                }}
                className="text-sm text-blue-600 dark:text-blue-400 hover:underline"
              >
                Use authenticator app instead
              </button>
            </div>
          </>
        )}

        {/* Back to login */}
        <div className="mt-6 pt-6 border-t border-gray-200 dark:border-gray-700 text-center">
          <button
            onClick={() => router.push('/login')}
            className="text-sm text-gray-600 dark:text-gray-400 hover:text-gray-900 dark:hover:text-white"
          >
            Back to login
          </button>
        </div>
      </div>
    </div>
  );
}

export default function MfaVerifyPage() {
  return (
    <Suspense fallback={
      <div className="min-h-screen flex items-center justify-center bg-gray-50 dark:bg-gray-900">
        <div className="text-center">
          <div className="animate-spin rounded-full h-16 w-16 border-b-2 border-blue-600 mx-auto"></div>
          <p className="mt-4 text-gray-600 dark:text-gray-400">Loading...</p>
        </div>
      </div>
    }>
      <MfaVerifyContent />
    </Suspense>
  );
}
