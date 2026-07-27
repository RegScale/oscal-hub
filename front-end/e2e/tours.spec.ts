import { test, expect, type Page } from '@playwright/test';
import AxeBuilder from '@axe-core/playwright';

const TOUR_STORAGE_KEY = 'oscal-hub.tours.v1.1';

/** Remove the seeded "prompt seen" state so the welcome dialog can fire. */
async function resetTourState(page: Page) {
  await page.goto('/');
  await page.evaluate((key) => {
    localStorage.removeItem(key);
    sessionStorage.clear();
  }, TOUR_STORAGE_KEY);
  await page.reload();
  await page.waitForLoadState('networkidle');
}

async function readTourState(page: Page) {
  return page.evaluate((key) => {
    const raw = localStorage.getItem(key);
    return raw ? JSON.parse(raw) : null;
  }, TOUR_STORAGE_KEY);
}

test.describe('Guided tours', () => {
  test('welcome prompt offers the tour; completing it persists and never re-prompts', async ({ page }) => {
    await resetTourState(page);

    const welcome = page.getByRole('dialog', { name: /take a two-minute tour/i });
    await expect(welcome).toBeVisible();
    await welcome.getByRole('button', { name: 'Start tour' }).click();

    // Step 1 is the welcome modal step.
    const tourDialog = page.getByRole('dialog', { name: 'Welcome to OSCAL Hub' });
    await expect(tourDialog).toBeVisible();
    await expect(tourDialog).toContainText('Step 1 of 8');

    // Walk the 6 middle steps, then Finish on the last one (8 steps total).
    for (let i = 0; i < 7; i++) {
      await page.getByRole('dialog').getByRole('button', { name: /next|finish/i }).click();
    }
    await expect(page.getByRole('dialog')).toContainText('Step 8 of 8');
    await page.getByRole('dialog').getByRole('button', { name: 'Finish' }).click();
    await expect(page.getByRole('dialog')).toHaveCount(0);

    const state = await readTourState(page);
    expect(state.tours['get-started'].completedVersion).toBe(1);

    // Reload: no welcome prompt again.
    await page.reload();
    await page.waitForLoadState('networkidle');
    await expect(page.getByRole('dialog', { name: /take a two-minute tour/i })).toHaveCount(0);
  });

  test('Escape dismisses the tour and records the step', async ({ page }) => {
    await resetTourState(page);
    await page.getByRole('button', { name: 'Start tour' }).click();
    await expect(page.getByRole('dialog', { name: 'Welcome to OSCAL Hub' })).toBeVisible();
    await page.keyboard.press('Escape');
    await expect(page.getByRole('dialog')).toHaveCount(0);

    const state = await readTourState(page);
    expect(state.tours['get-started'].dismissedAtStep).toBe(0);

    await page.reload();
    await page.waitForLoadState('networkidle');
    await expect(page.getByRole('dialog', { name: /take a two-minute tour/i })).toHaveCount(0);
  });

  test('"Maybe later" defers the prompt and increments the deferral count', async ({ page }) => {
    await resetTourState(page);
    await page.getByRole('button', { name: 'Maybe later' }).click();
    await expect(page.getByRole('dialog')).toHaveCount(0);
    const state = await readTourState(page);
    expect(state.welcomePrompt.deferrals).toBe(1);
  });

  test('tour can be replayed from the avatar menu', async ({ page }) => {
    // Seeded state: prompt seen, tour never run — the launcher must still work.
    await page.goto('/');
    await page.waitForLoadState('networkidle');
    await page.getByRole('button', { name: 'User menu' }).click();
    await page.getByRole('button', { name: 'Guided Tours' }).click();
    const menu = page.getByRole('dialog', { name: 'Guided tours' });
    await expect(menu).toBeVisible();
    await menu.getByRole('button', { name: /start|replay/i }).click();
    await expect(page.getByRole('dialog', { name: 'Welcome to OSCAL Hub' })).toBeVisible();
    await page.keyboard.press('Escape');
  });

  test('tour is fully keyboard operable', async ({ page }) => {
    await resetTourState(page);
    const welcome = page.getByRole('dialog', { name: /take a two-minute tour/i });
    await expect(welcome).toBeVisible();
    await welcome.getByRole('button', { name: 'Start tour' }).focus();
    await page.keyboard.press('Enter');
    await expect(page.getByRole('dialog', { name: 'Welcome to OSCAL Hub' })).toBeVisible();
    // ArrowRight advances (dialog container has focus after each step change).
    await page.keyboard.press('ArrowRight');
    await expect(page.getByRole('dialog')).toContainText('Step 2 of 8');
    await page.keyboard.press('ArrowLeft');
    await expect(page.getByRole('dialog')).toContainText('Step 1 of 8');
    await page.keyboard.press('Escape');
    await expect(page.getByRole('dialog')).toHaveCount(0);
  });

  test('open tour has no automatically detectable accessibility issues', async ({ page }) => {
    // Same zero-exclusion policy as e2e/accessibility.spec.ts.
    await resetTourState(page);
    await page.getByRole('button', { name: 'Start tour' }).click();
    await expect(page.getByRole('dialog', { name: 'Welcome to OSCAL Hub' })).toBeVisible();
    const modalStepScan = await new AxeBuilder({ page }).analyze();
    expect(modalStepScan.violations).toEqual([]);

    // Also scan an anchored (spotlight) step.
    await page.getByRole('dialog').getByRole('button', { name: 'Next' }).click();
    await expect(page.getByRole('dialog')).toContainText('Step 2 of 8');
    const anchoredStepScan = await new AxeBuilder({ page }).analyze();
    expect(anchoredStepScan.violations).toEqual([]);
  });
});
