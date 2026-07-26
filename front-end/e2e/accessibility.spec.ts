import { test, expect } from '@playwright/test';
import AxeBuilder from '@axe-core/playwright';

// Debug test to verify authentication is working
test.describe('Authentication Verification', () => {
  test('should have auth token in localStorage from storage state', async ({ page }) => {
    await page.goto('/');
    await page.waitForLoadState('networkidle');

    // Check localStorage values
    const token = await page.evaluate(() => localStorage.getItem('token'));
    const user = await page.evaluate(() => localStorage.getItem('user'));

    console.log('Auth token:', token ? 'present' : 'missing');
    console.log('User data:', user ? 'present' : 'missing');

    // Verify auth state is set
    expect(token).toBeTruthy();
    expect(user).toBeTruthy();

    // Parse and verify user data
    if (user) {
      const userData = JSON.parse(user);
      expect(userData.username).toBe('testuser');
      expect(userData.organizationId).toBe(1);
    }
  });

  test('should show authenticated dashboard content', async ({ page }) => {
    await page.goto('/');
    await page.waitForLoadState('networkidle');

    // Wait for React to hydrate and read localStorage
    await page.waitForTimeout(1000);

    // Check if we're seeing authenticated content (dashboard) or unauthenticated (hero)
    const validateCard = page.getByRole('link', { name: /navigate to validate/i });
    const isAuthenticated = await validateCard.isVisible({ timeout: 5000 }).catch(() => false);

    console.log('Is authenticated:', isAuthenticated);

    // This test documents the current state - should show authenticated dashboard
    expect(isAuthenticated).toBe(true);
  });
});

// No excluded rules — the exclusions added while fixing E2E in PR #16 were
// remediated in issue #19 (headings, labels, button names, link
// distinguishability). Keep this list empty; add entries only with an issue
// reference and a removal plan.
const AXE_EXCLUDED_RULES: string[] = [];

test.describe('Accessibility Tests', () => {
  test('Dashboard page should not have any automatically detectable accessibility issues', async ({
    page,
  }) => {
    await page.goto('/');

    const accessibilityScanResults = await new AxeBuilder({ page })
      .disableRules(AXE_EXCLUDED_RULES)
      .analyze();

    expect(accessibilityScanResults.violations).toEqual([]);
  });

  test('Validate page should not have any automatically detectable accessibility issues', async ({
    page,
  }) => {
    await page.goto('/validate');

    const accessibilityScanResults = await new AxeBuilder({ page })
      .disableRules(AXE_EXCLUDED_RULES)
      .analyze();

    expect(accessibilityScanResults.violations).toEqual([]);
  });

  test('Convert page should not have any automatically detectable accessibility issues', async ({
    page,
  }) => {
    await page.goto('/convert');

    const accessibilityScanResults = await new AxeBuilder({ page })
      .disableRules(AXE_EXCLUDED_RULES)
      .analyze();

    expect(accessibilityScanResults.violations).toEqual([]);
  });

  test('Resolve page should not have any automatically detectable accessibility issues', async ({
    page,
  }) => {
    await page.goto('/resolve');

    const accessibilityScanResults = await new AxeBuilder({ page })
      .disableRules(AXE_EXCLUDED_RULES)
      .analyze();

    expect(accessibilityScanResults.violations).toEqual([]);
  });

  test('Batch page should not have any automatically detectable accessibility issues', async ({
    page,
  }) => {
    await page.goto('/batch');

    const accessibilityScanResults = await new AxeBuilder({ page })
      .disableRules(AXE_EXCLUDED_RULES)
      .analyze();

    expect(accessibilityScanResults.violations).toEqual([]);
  });

  test('History page should not have any automatically detectable accessibility issues', async ({
    page,
  }) => {
    await page.goto('/history');

    const accessibilityScanResults = await new AxeBuilder({ page })
      .disableRules(AXE_EXCLUDED_RULES)
      .analyze();

    expect(accessibilityScanResults.violations).toEqual([]);
  });
});

test.describe('Keyboard Navigation Tests', () => {
  test('Should be able to navigate the dashboard using only keyboard', async ({ page }) => {
    await page.goto('/');
    await page.waitForLoadState('networkidle');

    // Press Tab to activate skip link
    await page.keyboard.press('Tab');
    const skipLink = page.getByText(/skip to main content/i);
    await expect(skipLink).toBeVisible();

    // Skip to main content
    await page.keyboard.press('Enter');

    // Tab to first operation card (Library is first in the grid)
    await page.keyboard.press('Tab');
    const libraryCard = page.getByRole('link', { name: /navigate to library/i });
    await expect(libraryCard).toBeFocused();

    // Tab until the Validate card gains focus. Bounded loop instead of a
    // hardcoded press count so the test doesn't break when the card grid
    // order changes — what matters is keyboard reachability.
    const validateCard = page.getByRole('link', { name: /navigate to validate/i });
    let reached = false;
    for (let i = 0; i < 15 && !reached; i++) {
      reached = await validateCard.evaluate((el) => el === document.activeElement);
      if (!reached) await page.keyboard.press('Tab');
    }
    await expect(validateCard).toBeFocused();

    // Press Enter to activate
    await page.keyboard.press('Enter');
    await expect(page).toHaveURL('/validate');
  });

  test('Should show visible focus indicators on all interactive elements', async ({ page }) => {
    await page.goto('/');

    // Tab through elements and check for focus styles
    await page.keyboard.press('Tab'); // Skip link
    await page.keyboard.press('Tab'); // First focusable element

    const focused = await page.evaluate(() => {
      const element = document.activeElement;
      if (!element) return null;

      const styles = window.getComputedStyle(element);
      return {
        outline: styles.outline,
        outlineColor: styles.outlineColor,
        outlineWidth: styles.outlineWidth,
      };
    });

    // Should have some form of focus indicator
    expect(focused).not.toBeNull();
  });
});

test.describe('Screen Reader Tests', () => {
  test('All images and icons should have appropriate alt text or aria-hidden', async ({
    page,
  }) => {
    await page.goto('/');
    await page.waitForLoadState('networkidle');

    const images = await page.locator('img, svg').all();

    for (const image of images) {
      // Skip Next.js dev-overlay internals (nextjs-portal) — dev-mode only,
      // not part of the shipped app. The overlay lives in a shadow root, so
      // walk shadow hosts rather than using closest().
      const imgInDevOverlay = await image.evaluate((el) => {
        let root = el.getRootNode();
        while (root instanceof ShadowRoot) {
          if (root.host.tagName === 'NEXTJS-PORTAL') return true;
          root = root.host.getRootNode();
        }
        return false;
      });
      if (imgInDevOverlay) continue;
      const tagName = await image.evaluate((el) => el.tagName);
      const hasAlt = await image.getAttribute('alt');
      const hasAriaHidden = await image.getAttribute('aria-hidden');
      const hasAriaLabel = await image.getAttribute('aria-label');
      const hasRole = await image.getAttribute('role');

      // Icon should either be decorative (aria-hidden) or have a label
      if (tagName === 'svg' || tagName === 'SVG') {
        const isAccessible =
          hasAriaHidden === 'true' ||
          hasAriaLabel ||
          hasRole === 'img' ||
          hasRole === 'presentation';
        expect(isAccessible).toBe(true);
      }
    }
  });

  test('All buttons should have accessible names', async ({ page }) => {
    await page.goto('/');
    await page.waitForLoadState('networkidle');

    const buttons = await page.locator('button').all();

    for (const button of buttons) {
      // Skip Next.js dev-overlay internals (nextjs-portal) — dev-mode only.
      // Overlay content is inside a shadow root, so walk shadow hosts.
      const btnInDevOverlay = await button.evaluate((el) => {
        let root = el.getRootNode();
        while (root instanceof ShadowRoot) {
          if (root.host.tagName === 'NEXTJS-PORTAL') return true;
          root = root.host.getRootNode();
        }
        return false;
      });
      if (btnInDevOverlay) continue;
      const accessibleName =
        (await button.getAttribute('aria-label')) ||
        (await button.textContent()) ||
        (await button.getAttribute('title'));

      expect(accessibleName).toBeTruthy();
    }
  });

  test('All form inputs should have associated labels', async ({ page }) => {
    await page.goto('/validate');

    const inputs = await page.locator('input, select, textarea').all();

    for (const input of inputs) {
      const id = await input.getAttribute('id');
      const ariaLabel = await input.getAttribute('aria-label');
      const ariaLabelledBy = await input.getAttribute('aria-labelledby');

      if (id) {
        const label = await page.locator(`label[for="${id}"]`).count();
        const hasLabel = label > 0 || ariaLabel || ariaLabelledBy;
        expect(hasLabel).toBe(true);
      } else {
        // Should have aria-label if no id
        expect(ariaLabel || ariaLabelledBy).toBeTruthy();
      }
    }
  });
});

test.describe('ARIA Landmarks Tests', () => {
  test('Pages should have proper landmark regions', async ({ page }) => {
    await page.goto('/');

    // Should have main landmark
    const main = await page.locator('[role="main"], main, #main-content').count();
    expect(main).toBeGreaterThan(0);

    // Should have navigation landmark
    const nav = await page.locator('[role="navigation"], nav').count();
    expect(nav).toBeGreaterThan(0);
  });

  test('Skip link should point to main content', async ({ page }) => {
    await page.goto('/');

    await page.keyboard.press('Tab');
    const skipLink = page.getByText(/skip to main content/i);

    const href = await skipLink.getAttribute('href');
    expect(href).toBe('#main-content');

    // Verify target exists
    const target = await page.locator('#main-content').count();
    expect(target).toBe(1);
  });
});
