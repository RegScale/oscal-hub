import { test as setup } from '@playwright/test';
import path from 'path';

const authFile = path.join(__dirname, '.auth/user.json');

/**
 * Global setup to establish authenticated state for E2E tests.
 * This creates a storage state file with mock authentication data
 * that can be reused across all tests requiring authentication.
 */
setup('authenticate', async ({ page }) => {
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
});
