import { describe, it, expect } from 'vitest';
import {
  checkPassword,
  isPasswordValid,
  PASSWORD_MIN_LENGTH,
  PASSWORD_MAX_LENGTH,
} from './password-policy';

// Mirrors the backend PasswordValidationService rules — if the backend policy
// changes, these tests (and password-policy.ts) must change with it.
describe('password-policy', () => {
  const failedLabels = (password: string, username = 'someuser') =>
    checkPassword(password, username)
      .filter((c) => !c.passed)
      .map((c) => c.label);

  it('accepts a strong policy-compliant password', () => {
    expect(isPasswordValid('CorrectH0rse!Batt', 'travis')).toBe(true);
  });

  it('rejects a password with no uppercase letter (the Michaela Iorga case)', () => {
    // Same failure mode as the 2026-07-23 production registration attempts
    expect(isPasswordValid('lowercase0nly!pw', 'iorga')).toBe(false);
    expect(failedLabels('lowercase0nly!pw', 'iorga')).toContain(
      'At least one uppercase letter'
    );
  });

  it('rejects passwords shorter than the backend minimum (10, not 8)', () => {
    // 'Piz2a!Ok' is 8 chars — the old frontend accepted it, the backend rejects it
    const pw = 'Piz2a!Ok';
    expect(pw.length).toBeGreaterThanOrEqual(8);
    expect(pw.length).toBeLessThan(PASSWORD_MIN_LENGTH);
    expect(isPasswordValid(pw, 'user')).toBe(false);
    expect(failedLabels(pw)).toContain(`At least ${PASSWORD_MIN_LENGTH} characters`);
  });

  it('rejects missing lowercase / digit / special character', () => {
    expect(failedLabels('UPPERCASE0NLY!PW')).toContain('At least one lowercase letter');
    expect(failedLabels('NoDigitsHere!Pw')).toContain('At least one number');
    expect(failedLabels('NoSpecial0Chars')).toContain(
      'At least one special character (!@#$%^&*()_+-=[]{}|;:,.<>?)'
    );
  });

  it('rejects sequential characters like abc and 123', () => {
    expect(isPasswordValid('Xk9!mabcQr2w', 'user')).toBe(false);
    expect(isPasswordValid('Xk9!m123Qr2w', 'user')).toBe(false);
    // descending sequences too (cba)
    expect(isPasswordValid('Xk9!mcbaQr2w', 'user')).toBe(false);
  });

  it('rejects three repeated characters', () => {
    expect(isPasswordValid('Xk9!maaaQr2w', 'user')).toBe(false);
  });

  it('rejects passwords containing the username (case-insensitive)', () => {
    expect(isPasswordValid('X!MyIorga2Pw9', 'iorga')).toBe(false);
    expect(failedLabels('X!MyIorga2Pw9', 'iorga')).toContain(
      'Does not contain your username'
    );
  });

  it('skips the username check when username is empty', () => {
    const labels = checkPassword('CorrectH0rse!Batt', '').map((c) => c.label);
    expect(labels).not.toContain('Does not contain your username');
  });

  it('rejects passwords over the maximum length', () => {
    const long = 'Aa1!'.repeat(40); // 160 chars, no seq/repeat violations
    expect(long.length).toBeGreaterThan(PASSWORD_MAX_LENGTH);
    expect(isPasswordValid(long, 'user')).toBe(false);
  });

  it('marks all checks failed for an empty password', () => {
    expect(isPasswordValid('', 'user')).toBe(false);
    expect(checkPassword('', 'user').every((c) => !c.passed)).toBe(true);
  });
});
