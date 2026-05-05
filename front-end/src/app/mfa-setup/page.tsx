'use client';

import { useEffect, useState, Suspense } from 'react';
import { useRouter, useSearchParams } from 'next/navigation';
import { Shield, Copy, Download, Check, Loader2, AlertCircle, QrCode } from 'lucide-react';
import { apiClient } from '@/lib/api-client';
import { useAuth } from '@/contexts/AuthContext';
import type { MfaSetupResponse } from '@/types/oscal';
import { HelpButton } from '@/components/HelpButton';

function MfaSetupContent() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const mfaToken = searchParams.get('token');
  const { updateUser } = useAuth();

  const [loading, setLoading] = useState(true);
  const [verifying, setVerifying] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [setupData, setSetupData] = useState<MfaSetupResponse | null>(null);
  const [totpCode, setTotpCode] = useState<string[]>(['', '', '', '', '', '']);
  const [backupCodes, setBackupCodes] = useState<string[] | null>(null);
  const [copied, setCopied] = useState(false);
  const [showManualEntry, setShowManualEntry] = useState(false);

  useEffect(() => {
    // Check if user has a setup token
    if (!mfaToken) {
      // Try to initiate MFA setup for authenticated user
      initiateMfaSetup();
    } else {
      // User came from login with MFA setup required
      initiateMfaSetup();
    }
  }, [mfaToken]);

  const initiateMfaSetup = async () => {
    try {
      // Pass the mfaToken if available (from login flow requiring MFA setup)
      const data = await apiClient.initiateMfaSetup(mfaToken || undefined);
      setSetupData(data);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to initiate MFA setup');
    } finally {
      setLoading(false);
    }
  };

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

  const handleVerify = async () => {
    const code = totpCode.join('');
    if (code.length !== 6) {
      setError('Please enter a 6-digit code');
      return;
    }

    setError(null);
    setVerifying(true);

    try {
      const response = await apiClient.completeMfaSetup({
        setupToken: setupData?.setupToken || mfaToken || '',
        totpCode: code,
      });

      // Store the new token
      if (response.token) {
        localStorage.setItem('token', response.token);
      }

      // Show backup codes
      setBackupCodes(response.backupCodes);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Invalid code. Please try again.');
      setTotpCode(['', '', '', '', '', '']);
      document.getElementById('code-0')?.focus();
    } finally {
      setVerifying(false);
    }
  };

  const handleCopyBackupCodes = () => {
    if (backupCodes) {
      navigator.clipboard.writeText(backupCodes.join('\n'));
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    }
  };

  const handleDownloadBackupCodes = () => {
    if (backupCodes) {
      const content = `OSCAL Tools - MFA Backup Codes
Generated: ${new Date().toLocaleString()}

IMPORTANT: Store these codes in a safe place. Each code can only be used once.

${backupCodes.map((code, i) => `${i + 1}. ${code}`).join('\n')}

If you lose access to your authenticator app, use one of these codes to log in.
`;
      const blob = new Blob([content], { type: 'text/plain' });
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = 'oscal-mfa-backup-codes.txt';
      a.click();
      URL.revokeObjectURL(url);
    }
  };

  const handleContinue = () => {
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
  };

  if (loading) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-gray-50 dark:bg-gray-900">
        <div className="text-center">
          <div className="animate-spin rounded-full h-16 w-16 border-b-2 border-blue-600 mx-auto"></div>
          <p className="mt-4 text-gray-600 dark:text-gray-400">Setting up MFA...</p>
        </div>
      </div>
    );
  }

  // Show backup codes after successful setup
  if (backupCodes) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-gray-50 dark:bg-gray-900 p-4">
        <div className="max-w-md w-full bg-white dark:bg-gray-800 rounded-lg shadow-lg p-8">
          <div className="text-center mb-6">
            <div className="inline-flex items-center justify-center w-16 h-16 bg-green-100 dark:bg-green-900/30 rounded-full mb-4">
              <Check className="w-8 h-8 text-green-600 dark:text-green-400" />
            </div>
            <h1 className="text-2xl font-bold text-gray-900 dark:text-white">
              MFA Successfully Enabled!
            </h1>
            <p className="mt-2 text-gray-600 dark:text-gray-400">
              Save these backup codes in a safe place
            </p>
          </div>

          <div className="bg-amber-50 dark:bg-amber-900/20 border border-amber-200 dark:border-amber-800 rounded-lg p-4 mb-6">
            <p className="text-sm text-amber-800 dark:text-amber-200">
              <strong>Important:</strong> Each code can only be used once. If you lose access to your authenticator app, use a backup code to log in.
            </p>
          </div>

          <div className="bg-gray-50 dark:bg-gray-700 rounded-lg p-4 mb-6">
            <div className="grid grid-cols-2 gap-2 font-mono text-sm">
              {backupCodes.map((code, index) => (
                <div
                  key={index}
                  className="bg-white dark:bg-gray-600 px-3 py-2 rounded text-center text-gray-900 dark:text-white"
                >
                  {code}
                </div>
              ))}
            </div>
          </div>

          <div className="flex space-x-3 mb-6">
            <button
              onClick={handleCopyBackupCodes}
              className="flex-1 inline-flex items-center justify-center px-4 py-2 border border-gray-300 dark:border-gray-600 rounded-md text-gray-700 dark:text-gray-300 hover:bg-gray-50 dark:hover:bg-gray-700"
            >
              {copied ? (
                <>
                  <Check className="w-4 h-4 mr-2" />
                  Copied!
                </>
              ) : (
                <>
                  <Copy className="w-4 h-4 mr-2" />
                  Copy Codes
                </>
              )}
            </button>
            <button
              onClick={handleDownloadBackupCodes}
              className="flex-1 inline-flex items-center justify-center px-4 py-2 border border-gray-300 dark:border-gray-600 rounded-md text-gray-700 dark:text-gray-300 hover:bg-gray-50 dark:hover:bg-gray-700"
            >
              <Download className="w-4 h-4 mr-2" />
              Download
            </button>
          </div>

          <button
            onClick={handleContinue}
            className="w-full px-4 py-2 bg-blue-600 hover:bg-blue-700 text-white font-medium rounded-md"
          >
            Continue to Dashboard
          </button>
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
          <div className="flex items-center justify-center gap-2">
            <h1 className="text-2xl font-bold text-gray-900 dark:text-white">
              Setup Two-Factor Authentication
            </h1>
            <HelpButton slug="mfa-setup" />
          </div>
          <p className="mt-2 text-gray-600 dark:text-gray-400">
            Secure your account with an authenticator app
          </p>
        </div>

        {error && (
          <div className="mb-6 bg-red-50 dark:bg-red-900/30 border border-red-200 dark:border-red-800 rounded-lg p-4 flex items-center">
            <AlertCircle className="w-5 h-5 text-red-600 dark:text-red-400 mr-3" />
            <span className="text-red-800 dark:text-red-200">{error}</span>
          </div>
        )}

        {/* Step 1: QR Code */}
        <div className="mb-6">
          <h2 className="text-lg font-medium text-gray-900 dark:text-white mb-3">
            Step 1: Scan QR Code
          </h2>
          <p className="text-sm text-gray-600 dark:text-gray-400 mb-4">
            Use Google Authenticator, Microsoft Authenticator, or any TOTP app
          </p>

          {setupData?.qrCodeDataUri && (
            <div className="flex justify-center mb-4">
              <div className="bg-white p-4 rounded-lg inline-block">
                <img
                  src={setupData.qrCodeDataUri}
                  alt="QR Code"
                  className="w-48 h-48"
                />
              </div>
            </div>
          )}

          <button
            onClick={() => setShowManualEntry(!showManualEntry)}
            className="text-sm text-blue-600 dark:text-blue-400 hover:underline"
          >
            {showManualEntry ? 'Hide manual entry' : "Can't scan? Enter code manually"}
          </button>

          {showManualEntry && setupData?.formattedSecret && (
            <div className="mt-3 bg-gray-50 dark:bg-gray-700 rounded-lg p-3">
              <p className="text-xs text-gray-500 dark:text-gray-400 mb-1">
                Manual entry code:
              </p>
              <p className="font-mono text-sm text-gray-900 dark:text-white break-all">
                {setupData.formattedSecret}
              </p>
            </div>
          )}
        </div>

        {/* Step 2: Verification Code */}
        <div className="mb-6">
          <h2 className="text-lg font-medium text-gray-900 dark:text-white mb-3">
            Step 2: Enter Verification Code
          </h2>
          <p className="text-sm text-gray-600 dark:text-gray-400 mb-4">
            Enter the 6-digit code from your authenticator app
          </p>

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
                className="w-12 h-14 text-center text-xl font-semibold border border-gray-300 dark:border-gray-600 rounded-md bg-white dark:bg-gray-700 text-gray-900 dark:text-white focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
              />
            ))}
          </div>
        </div>

        {/* Verify Button */}
        <button
          onClick={handleVerify}
          disabled={verifying || totpCode.join('').length !== 6}
          className="w-full px-4 py-3 bg-blue-600 hover:bg-blue-700 text-white font-medium rounded-md disabled:opacity-50 disabled:cursor-not-allowed flex items-center justify-center"
        >
          {verifying ? (
            <>
              <Loader2 className="w-5 h-5 mr-2 animate-spin" />
              Verifying...
            </>
          ) : (
            'Verify & Enable MFA'
          )}
        </button>
      </div>
    </div>
  );
}

export default function MfaSetupPage() {
  return (
    <Suspense fallback={
      <div className="min-h-screen flex items-center justify-center bg-gray-50 dark:bg-gray-900">
        <div className="text-center">
          <div className="animate-spin rounded-full h-16 w-16 border-b-2 border-blue-600 mx-auto"></div>
          <p className="mt-4 text-gray-600 dark:text-gray-400">Loading...</p>
        </div>
      </div>
    }>
      <MfaSetupContent />
    </Suspense>
  );
}
