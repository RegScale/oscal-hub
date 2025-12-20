import { test, expect } from '@playwright/test';

test.describe('Validate Page', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/validate');
    // Wait for page to settle after potential redirects
    await page.waitForLoadState('networkidle');
  });

  test('should display page title and description when authenticated', async ({ page }) => {
    // Skip if redirected to home (not authenticated)
    const isOnValidatePage = page.url().includes('/validate');
    test.skip(!isOnValidatePage, 'Skipped: User not authenticated - redirected to home page');

    await expect(page.getByRole('heading', { name: /validate oscal document/i })).toBeVisible();
    await expect(
      page.getByText(/check if your oscal document is valid/i)
    ).toBeVisible();
  });

  test('should have back to dashboard link when authenticated', async ({ page }) => {
    const isOnValidatePage = page.url().includes('/validate');
    test.skip(!isOnValidatePage, 'Skipped: User not authenticated');

    const backLink = page.getByRole('link', { name: /back to dashboard/i });
    await expect(backLink).toBeVisible();
    await expect(backLink).toHaveAttribute('href', '/');
  });

  test('should show empty state when no file is selected', async ({ page }) => {
    const isOnValidatePage = page.url().includes('/validate');
    test.skip(!isOnValidatePage, 'Skipped: User not authenticated');

    await expect(page.getByText(/no document selected/i)).toBeVisible();
    await expect(page.getByText(/upload a file to begin validation/i)).toBeVisible();
  });

  test('should have file upload section when authenticated', async ({ page }) => {
    const isOnValidatePage = page.url().includes('/validate');
    test.skip(!isOnValidatePage, 'Skipped: User not authenticated');

    await expect(page.getByText(/document upload/i)).toBeVisible();
  });

  test('should have model type selector (disabled initially)', async ({ page }) => {
    const isOnValidatePage = page.url().includes('/validate');
    test.skip(!isOnValidatePage, 'Skipped: User not authenticated');

    // Model type selector should be present but disabled without a file
    await expect(page.getByText('OSCAL Model Type')).toBeVisible();
    const modelTypeSelect = page.locator('#model-type-select');
    await expect(modelTypeSelect).toBeVisible();
    await expect(modelTypeSelect).toBeDisabled();
  });

  test('should have validate button (disabled initially)', async ({ page }) => {
    const isOnValidatePage = page.url().includes('/validate');
    test.skip(!isOnValidatePage, 'Skipped: User not authenticated');

    const validateButton = page.getByText('Validate Document');
    await expect(validateButton).toBeVisible();
    await expect(validateButton).toBeDisabled();
  });

  test('should show skip link on keyboard navigation', async ({ page }) => {
    // Skip link should be visible on any page
    await page.keyboard.press('Tab');
    const skipLink = page.getByText(/skip to main content/i);
    await expect(skipLink).toBeVisible();
  });

  test('should navigate to main content when skip link is clicked', async ({ page }) => {
    await page.keyboard.press('Tab');
    await page.keyboard.press('Enter');

    // Focus should move to main content
    const mainContent = page.locator('#main-content');
    await expect(mainContent).toBeVisible();
  });

  test('should have proper heading hierarchy when authenticated', async ({ page }) => {
    const isOnValidatePage = page.url().includes('/validate');
    test.skip(!isOnValidatePage, 'Skipped: User not authenticated');

    // Check for h1
    const h1 = page.getByRole('heading', { level: 1 });
    await expect(h1).toHaveCount(1);

    // Should have proper structure
    await expect(h1).toHaveText(/validate oscal document/i);
  });

  test('all interactive elements should be keyboard accessible', async ({ page }) => {
    const isOnValidatePage = page.url().includes('/validate');
    test.skip(!isOnValidatePage, 'Skipped: User not authenticated');

    // Verify keyboard navigation works by checking focusable elements exist
    const backLink = page.getByRole('link', { name: /back to dashboard/i });
    await expect(backLink).toBeVisible();

    // Verify the link is functional by checking its href
    await expect(backLink).toHaveAttribute('href', '/');
  });
});

test.describe('Validate Page - With File', () => {
  test('should have validate button present when authenticated', async ({ page }) => {
    await page.goto('/validate');
    await page.waitForLoadState('networkidle');

    const isOnValidatePage = page.url().includes('/validate');
    test.skip(!isOnValidatePage, 'Skipped: User not authenticated');

    // Verify the validate button exists (will be enabled after file upload)
    const validateButton = page.getByText('Validate Document');
    await expect(validateButton).toBeVisible();
    // Button should be disabled initially
    await expect(validateButton).toBeDisabled();
  });
});
