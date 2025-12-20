import { test as setup, expect } from '@playwright/test';
import path from 'path';
import fs from 'fs';

const authDir = path.join(__dirname, '.auth');
const authFile = path.join(authDir, 'user.json');

/**
 * Global setup to establish authenticated state for E2E tests.
 * This creates a storage state file with mock authentication data
 * that can be reused across all tests requiring authentication.
 */
setup('authenticate', async ({ page }) => {
  // Ensure auth directory exists
  if (!fs.existsSync(authDir)) {
    fs.mkdirSync(authDir, { recursive: true });
  }

  // Navigate to the app to establish the browser context
  await page.goto('/');

  // Wait for the page to load
  await page.waitForLoadState('networkidle');

  // Set up mock authentication in localStorage
  // This simulates a logged-in user with full organization access
  const mockUser = {
    userId: 1,
    username: 'testuser',
    email: 'test@example.com',
    organizationId: 1,
    organizationName: 'Test Organization',
  };

  await page.evaluate((user) => {
    // Set a mock JWT token (doesn't need to be valid for frontend-only tests)
    localStorage.setItem('token', 'mock-e2e-test-token-12345');
    localStorage.setItem('user', JSON.stringify(user));
  }, mockUser);

  // Save the authenticated state
  await page.context().storageState({ path: authFile });

  // Verify localStorage was set correctly
  const storedToken = await page.evaluate(() => localStorage.getItem('token'));
  const storedUser = await page.evaluate(() => localStorage.getItem('user'));
  expect(storedToken).toBe('mock-e2e-test-token-12345');
  expect(storedUser).toContain('testuser');

  // Log success for debugging
  console.log('Auth setup complete - localStorage set and storage state saved');
});
