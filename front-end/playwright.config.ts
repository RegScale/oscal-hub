import { defineConfig, devices } from '@playwright/test';
import path from 'path';

// Use BROWSER_SCOPE env var to control which browsers to test
// 'chromium' = Chromium only (fast, for PRs)
// 'all' = All browsers (comprehensive, for develop branch)
const browserScope = process.env.BROWSER_SCOPE || 'all';

// Path to store authenticated state
const authFile = path.join(__dirname, 'e2e/.auth/user.json');

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

  /* Configure projects for authentication setup and browser testing */
  projects: [
    // Setup project - runs first to establish authenticated state
    {
      name: 'setup',
      testMatch: /.*\.setup\.ts/,
    },
    // Chromium tests with authentication
    {
      name: 'chromium',
      use: {
        ...devices['Desktop Chrome'],
        storageState: authFile,
      },
      dependencies: ['setup'],
    },
    // Additional browsers when BROWSER_SCOPE is 'all'
    ...(browserScope === 'all'
      ? [
          {
            name: 'firefox',
            use: {
              ...devices['Desktop Firefox'],
              storageState: authFile,
            },
            dependencies: ['setup'],
          },
          {
            name: 'webkit',
            use: {
              ...devices['Desktop Safari'],
              storageState: authFile,
            },
            dependencies: ['setup'],
          },
        ]
      : []),
  ],

  /* Run your local dev server before starting the tests */
  webServer: {
    command: 'npm run dev',
    url: 'http://localhost:3000',
    reuseExistingServer: !process.env.CI,
    timeout: 120 * 1000,
  },
});
