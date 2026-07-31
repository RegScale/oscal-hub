import { test, expect, type Page } from '@playwright/test';
import AxeBuilder from '@axe-core/playwright';

/**
 * Leaderboard e2e. The backend is mocked via page.route so the spec runs
 * against the frontend dev server alone (same pattern as
 * rules-ai-generate.spec.ts).
 */

const leaderboardFixture = (window: string) => ({
  window,
  generatedAt: '2026-07-31T12:00:00Z',
  mostActive: [
    {
      rank: 1,
      username: 'alice',
      displayName: 'Alice Ames',
      score: 42,
      breakdown: { operations: 40, libraryPublishes: 2 },
    },
    {
      rank: 2,
      username: 'testuser',
      displayName: 'Test User',
      score: 17,
      breakdown: { operations: 17 },
    },
    { rank: 3, username: 'bob', displayName: 'Bob Brown', score: 9, breakdown: { operations: 9 } },
    { rank: 4, username: 'dave', displayName: 'Dave Diaz', score: 3, breakdown: { operations: 3 } },
  ],
  topContributors: [
    { rank: 1, username: 'bob', displayName: 'Bob Brown', score: 7 },
    { rank: 2, username: 'alice', displayName: 'Alice Ames', score: 4 },
  ],
});

async function mockLeaderboardApi(page: Page) {
  await page.route('**/api/leaderboard*', async (route) => {
    const url = new URL(route.request().url());
    const window = url.searchParams.get('window') ?? 'all';
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify(leaderboardFixture(window)),
    });
  });
}

test.describe('Leaderboard', () => {
  test.beforeEach(async ({ page }) => {
    await mockLeaderboardApi(page);
  });

  test('nav link is visible and navigates to the leaderboard page', async ({ page }) => {
    await page.goto('/');
    await page.waitForLoadState('networkidle');

    const navLink = page.getByRole('link', { name: 'Leaderboard' });
    await expect(navLink).toBeVisible();
    await navLink.click();

    await expect(page).toHaveURL(/\/leaderboard$/);
    await expect(page.getByRole('heading', { name: 'Leaderboard' })).toBeVisible();
  });

  test('renders both boards with ranked rows and self-highlight', async ({ page }) => {
    await page.goto('/leaderboard');

    await expect(page.getByText('Most Active Users')).toBeVisible();
    await expect(page.getByText('Top Contributors')).toBeVisible();
    await expect(page.getByText('Alice Ames').first()).toBeVisible();

    // Signed-in mock user (testuser) is rank 2 and gets the You badge.
    await expect(page.getByText('You')).toBeVisible();

    // Medals for the top three in the most-active board.
    const activeBoard = page.getByTestId('board-most-active');
    await expect(activeBoard.getByTestId('medal-1')).toBeVisible();
    await expect(activeBoard.getByTestId('medal-2')).toBeVisible();
    await expect(activeBoard.getByTestId('medal-3')).toBeVisible();
  });

  test('switching tabs requests the 30-day window', async ({ page }) => {
    await page.goto('/leaderboard');
    await expect(page.getByText('Alice Ames').first()).toBeVisible();

    const request = page.waitForRequest((req) =>
      req.url().includes('/api/leaderboard') && req.url().includes('window=30d')
    );
    await page.getByRole('tab', { name: 'Last 30 days' }).click();
    await request;

    await expect(page.getByRole('tab', { name: 'Last 30 days' })).toHaveAttribute(
      'aria-selected',
      'true'
    );
  });

  test('tabs are keyboard operable', async ({ page }) => {
    await page.goto('/leaderboard');
    await expect(page.getByText('Alice Ames').first()).toBeVisible();

    const request = page.waitForRequest((req) =>
      req.url().includes('/api/leaderboard') && req.url().includes('window=30d')
    );
    await page.getByRole('tab', { name: 'Last 30 days' }).focus();
    await page.keyboard.press('Enter');
    await request;

    await expect(page.getByRole('tab', { name: 'Last 30 days' })).toHaveAttribute(
      'aria-selected',
      'true'
    );
  });

  test('has no automatically detectable accessibility issues', async ({ page }) => {
    await page.goto('/leaderboard');
    await expect(page.getByText('Alice Ames').first()).toBeVisible();

    const results = await new AxeBuilder({ page })
      .withTags(['wcag2a', 'wcag2aa', 'wcag21a', 'wcag21aa'])
      .analyze();

    expect(results.violations).toEqual([]);
  });
});
