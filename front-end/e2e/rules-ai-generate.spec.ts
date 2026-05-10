import { test, expect } from '@playwright/test';

/**
 * Happy-path test for the AI rule-gen wizard. The Anthropic surface is
 * mocked at the network layer so this runs deterministically without a
 * real API key. Auth is provided by the shared storageState.
 */
test.describe('AI rule-gen wizard', () => {
  test('drafts a rule, shows green test matrix, enables save', async ({ page }) => {
    await page.route('**/api/rules/ai-generate/sessions', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ sessionId: '00000000-0000-0000-0000-000000000001' }),
      });
    });

    await page.route('**/api/rules/ai-generate/sessions/*/turn', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          phase: 'proposal',
          clarifyingQuestion: null,
          proposal: {
            name: 'Mock rule',
            description: 'Always passes the synthetic check',
            severity: 'error',
            fieldPath: 'metadata',
            constraintXml:
              '<assembly target="metadata"><expect id="x" level="ERROR" test="true()"><message>m</message></expect></assembly>',
            testCases: [
              { description: 'good fragment', fragmentJson: '{}', expected: 'pass' },
            ],
          },
          testResults: [
            {
              index: 0,
              description: 'good fragment',
              expected: 'pass',
              actual: 'pass',
              passed: true,
              violationMessage: null,
            },
          ],
          lastProposal: null,
          message: null,
          iterations: 1,
          totalTokensIn: 100,
          totalTokensOut: 200,
        }),
      });
    });

    await page.goto('/rules/custom');
    await page.waitForLoadState('networkidle');

    await page.getByRole('link', { name: /Generate with AI/i }).click();
    await page.waitForURL('**/rules/custom/ai-generate');

    // Pre-flight: pick a model.
    await page.getByRole('combobox').click();
    await page.getByRole('option', { name: 'Catalog' }).click();
    await page.getByRole('button', { name: 'Start' }).click();

    // Send a description.
    await page.getByPlaceholder(/Describe what the rule should enforce/i)
      .fill('Catalog must have a non-empty title');
    await page.getByRole('button', { name: 'Send' }).click();

    // Proposal renders.
    await expect(page.getByRole('heading', { name: 'Mock rule' })).toBeVisible();

    // Test matrix shows all-green and Save is enabled (after typing a rule id).
    await expect(page.getByText('good fragment')).toBeVisible();
    await page.getByPlaceholder('custom-r-001').fill('custom-mock-001');
    await expect(page.getByRole('button', { name: 'Save rule' })).toBeEnabled();
  });
});
