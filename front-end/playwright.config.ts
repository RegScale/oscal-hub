import { defineConfig, devices } from '@playwright/test';

// Use BROWSER_SCOPE env var to control which browsers to test
// 'chromium' = Chromium only (fast, for PRs)
// 'all' = All browsers (comprehensive, for develop branch)
const browserScope = process.env.BROWSER_SCOPE || 'all';

const allProjects = [
  {
    name: 'chromium',
    use: { ...devices['Desktop Chrome'] },
  },
  {
    name: 'firefox',
    use: { ...devices['Desktop Firefox'] },
  },
  {
    name: 'webkit',
    use: { ...devices['Desktop Safari'] },
  },
];

const chromiumOnly = [
  {
    name: 'chromium',
    use: { ...devices['Desktop Chrome'] },
  },
];

/**
 * See https://playwright.dev/docs/test-configuration.
 */
export default defineConfig({
  testDir: './e2e',
  /* Run tests in files in parallel */
  fullyParallel: true,
  /* Fail the build on CI if you accidentally left test.only in the source code. */
  forbidOnly: !!process.env.CI,
  /* Retry on CI only - reduced from 2 to 1 for faster feedback */
  retries: process.env.CI ? 1 : 0,
  /* Use half available CPUs on CI for parallel execution */
  workers: process.env.CI ? '50%' : undefined,
  /* Reporter to use. See https://playwright.dev/docs/test-reporters */
  reporter: 'html',
  /* Shared settings for all the projects below. See https://playwright.dev/docs/api/class-testoptions. */
  use: {
    /* Base URL to use in actions like `await page.goto('/')`. */
    baseURL: 'http://localhost:3000',
    /* Collect trace when retrying the failed test. See https://playwright.dev/docs/trace-viewer */
    trace: 'on-first-retry',
    screenshot: 'only-on-failure',
  },

  /* Configure projects based on BROWSER_SCOPE */
  projects: browserScope === 'chromium' ? chromiumOnly : allProjects,

  /* Run your local dev server before starting the tests */
  webServer: {
    command: 'npm run dev',
    url: 'http://localhost:3000',
    reuseExistingServer: !process.env.CI,
    timeout: 120 * 1000,
  },
});
