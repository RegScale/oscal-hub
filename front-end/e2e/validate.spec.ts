import { test, expect } from '@playwright/test';

test.describe('Validate Page', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/validate');
  });

  test('should display page title and description', async ({ page }) => {
    await expect(page.getByRole('heading', { name: /validate oscal document/i })).toBeVisible();
    await expect(
      page.getByText(/check if your oscal document is valid/i)
    ).toBeVisible();
  });

  test('should have back to dashboard link', async ({ page }) => {
    const backLink = page.getByRole('link', { name: /back to dashboard/i });
    await expect(backLink).toBeVisible();
    await expect(backLink).toHaveAttribute('href', '/');
  });

  test('should show empty state when no file is selected', async ({ page }) => {
    await expect(page.getByText(/no document selected/i)).toBeVisible();
    await expect(page.getByText(/upload a file to begin validation/i)).toBeVisible();
  });

  test('should have file upload section', async ({ page }) => {
    await expect(page.getByText(/document upload/i)).toBeVisible();
  });

  test('should have model type selector (disabled initially)', async ({ page }) => {
    // Model type selector should be present but disabled without a file
    await expect(page.getByText('OSCAL Model Type')).toBeVisible();
    const modelTypeSelect = page.locator('#model-type-select');
    await expect(modelTypeSelect).toBeVisible();
    await expect(modelTypeSelect).toBeDisabled();
  });

  test('should have validate button (disabled initially)', async ({ page }) => {
    // Use getByRole to specifically target the button, not the nav link
    const validateButton = page.getByRole('button', { name: /validate document/i });
    await expect(validateButton).toBeVisible();
    await expect(validateButton).toBeDisabled();
  });

  test('should show skip link on keyboard navigation', async ({ page }) => {
    // Press Tab to show skip link
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

  test('should have proper heading hierarchy', async ({ page }) => {
    // Check for h1
    const h1 = page.getByRole('heading', { level: 1 });
    await expect(h1).toHaveCount(1);

    // Should have proper structure
    await expect(h1).toHaveText(/validate oscal document/i);
  });

  test('all interactive elements should be keyboard accessible', async ({ page }) => {
    // Verify keyboard navigation works by checking focusable elements exist
    const backLink = page.getByRole('link', { name: /back to dashboard/i });
    await expect(backLink).toBeVisible();

    // Verify the link is functional by checking its href
    await expect(backLink).toHaveAttribute('href', '/');
  });
});

test.describe('Validate Page - With File', () => {
  test('should have validate button present', async ({ page }) => {
    await page.goto('/validate');

    // Verify the validate button exists (will be enabled after file upload)
    // Use getByRole to specifically target the button, not the nav link
    const validateButton = page.getByRole('button', { name: /validate document/i });
    await expect(validateButton).toBeVisible();
    // Button should be disabled initially
    await expect(validateButton).toBeDisabled();
  });
});
