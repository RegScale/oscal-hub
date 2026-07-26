/**
 * Client-side password policy evaluation.
 *
 * The backend is the authority: `GET /api/auth/password-policy` serves the
 * live policy (admin-editable lengths + configured character rules), and the
 * `usePasswordPolicy` hook (components/password-requirements.tsx) hydrates it.
 * The DEFAULT_POLICY constants below are the offline/error fallback and match
 * the backend defaults in PasswordValidationService / application.properties.
 */

export interface ServerPasswordPolicy {
  minLength: number;
  maxLength: number;
  requireUppercase: boolean;
  requireLowercase: boolean;
  requireDigit: boolean;
  requireSpecial: boolean;
  specialCharacters: string;
  preventUsernameInPassword: boolean;
  noSequentialCharacters: boolean;
  noRepeatedCharacters: boolean;
}

export const DEFAULT_POLICY: ServerPasswordPolicy = {
  minLength: 10,
  maxLength: 128,
  requireUppercase: true,
  requireLowercase: true,
  requireDigit: true,
  requireSpecial: true,
  specialCharacters: '!@#$%^&*()_+-=[]{}|;:,.<>?',
  preventUsernameInPassword: true,
  noSequentialCharacters: true,
  noRepeatedCharacters: true,
};

export const PASSWORD_MIN_LENGTH = DEFAULT_POLICY.minLength;
export const PASSWORD_MAX_LENGTH = DEFAULT_POLICY.maxLength;

// Same set as the backend's SPECIAL_PATTERN
const SPECIAL_RE = /[!@#$%^&*()_+\-=\[\]{}|;:,.<>?]/;

export interface PasswordCheck {
  /** Short human-readable requirement, shown in the checklist */
  label: string;
  /** Whether the current password satisfies this requirement */
  passed: boolean;
}

function hasSequentialCharacters(password: string): boolean {
  for (let i = 0; i < password.length - 2; i++) {
    const c1 = password.charCodeAt(i);
    const c2 = password.charCodeAt(i + 1);
    const c3 = password.charCodeAt(i + 2);
    if ((c2 === c1 + 1 && c3 === c2 + 1) || (c2 === c1 - 1 && c3 === c2 - 1)) {
      return true;
    }
  }
  return false;
}

function hasRepeatedCharacters(password: string): boolean {
  for (let i = 0; i < password.length - 2; i++) {
    if (password[i + 1] === password[i] && password[i + 2] === password[i]) {
      return true;
    }
  }
  return false;
}

function containsUsername(password: string, username: string): boolean {
  if (!username) return false;
  return password.toLowerCase().includes(username.toLowerCase());
}

/**
 * Evaluate a password against every deterministic backend rule.
 * Returns one entry per rule so the UI can render a live checklist.
 */
export function checkPassword(
  password: string,
  username: string,
  policy: ServerPasswordPolicy = DEFAULT_POLICY
): PasswordCheck[] {
  const checks: PasswordCheck[] = [
    {
      label: `At least ${policy.minLength} characters`,
      passed: password.length >= policy.minLength,
    },
  ];

  if (policy.requireUppercase) {
    checks.push({ label: 'At least one uppercase letter', passed: /[A-Z]/.test(password) });
  }
  if (policy.requireLowercase) {
    checks.push({ label: 'At least one lowercase letter', passed: /[a-z]/.test(password) });
  }
  if (policy.requireDigit) {
    checks.push({ label: 'At least one number', passed: /[0-9]/.test(password) });
  }
  if (policy.requireSpecial) {
    checks.push({
      label: `At least one special character (${policy.specialCharacters})`,
      passed: SPECIAL_RE.test(password),
    });
  }
  if (policy.noSequentialCharacters) {
    checks.push({
      label: 'No sequential characters (e.g. abc, 123)',
      passed: password.length > 0 && !hasSequentialCharacters(password),
    });
  }
  if (policy.noRepeatedCharacters) {
    checks.push({
      label: 'No repeated characters (e.g. aaa, 111)',
      passed: password.length > 0 && !hasRepeatedCharacters(password),
    });
  }

  if (policy.preventUsernameInPassword && username) {
    checks.push({
      label: 'Does not contain your username',
      passed: password.length > 0 && !containsUsername(password, username),
    });
  }

  if (password.length > policy.maxLength) {
    checks.push({
      label: `At most ${policy.maxLength} characters`,
      passed: false,
    });
  }

  return checks;
}

/** True when every requirement passes. */
export function isPasswordValid(
  password: string,
  username: string,
  policy: ServerPasswordPolicy = DEFAULT_POLICY
): boolean {
  return checkPassword(password, username, policy).every((c) => c.passed);
}
