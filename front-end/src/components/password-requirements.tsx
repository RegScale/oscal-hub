'use client';

import { useEffect, useState } from 'react';
import { apiClient } from '@/lib/api-client';
import { checkPassword, DEFAULT_POLICY, ServerPasswordPolicy } from '@/lib/password-policy';

// Module-level cache: the policy rarely changes; one fetch per page load is plenty.
let cachedPolicy: ServerPasswordPolicy | null = null;

/**
 * The live password policy as enforced by the server, hydrated from
 * GET /api/auth/password-policy with the static defaults as fallback.
 * Use this for both the checklist and submit gating so they always agree.
 */
export function usePasswordPolicy(): ServerPasswordPolicy {
  const [policy, setPolicy] = useState<ServerPasswordPolicy>(cachedPolicy ?? DEFAULT_POLICY);

  useEffect(() => {
    if (cachedPolicy) return;
    let cancelled = false;
    try {
      apiClient
        .fetchPasswordPolicy()
        .then((p) => {
          cachedPolicy = { ...DEFAULT_POLICY, ...p };
          if (!cancelled) setPolicy(cachedPolicy);
        })
        .catch(() => {
          // Offline or endpoint unavailable — the static defaults still apply,
          // and the server remains the final authority on submit.
        });
    } catch {
      // apiClient without fetchPasswordPolicy (e.g. partial test mocks) — use defaults.
    }
    return () => {
      cancelled = true;
    };
  }, []);

  return policy;
}

interface PasswordRequirementsProps {
  password: string;
  username: string;
  /** Pass the value from usePasswordPolicy() when the page also gates on it. */
  policy?: ServerPasswordPolicy;
}

/**
 * Live password-requirements checklist. Renders the server-enforced policy so
 * users see exactly which rules they haven't met before submitting.
 */
export function PasswordRequirements({ password, username, policy }: PasswordRequirementsProps) {
  const hydrated = usePasswordPolicy();
  const checks = checkPassword(password, username, policy ?? hydrated);

  return (
    <div className="text-xs space-y-1" aria-live="polite">
      <p className="font-medium text-muted-foreground">Password requirements:</p>
      <ul className="space-y-0.5">
        {checks.map((check) => (
          <li
            key={check.label}
            className={
              check.passed
                ? 'text-green-600 dark:text-green-500'
                : 'text-muted-foreground'
            }
          >
            <span className="inline-block w-4" aria-hidden="true">
              {check.passed ? '✓' : '○'}
            </span>
            {check.label}
            <span className="sr-only">{check.passed ? ' (met)' : ' (not met)'}</span>
          </li>
        ))}
      </ul>
    </div>
  );
}
