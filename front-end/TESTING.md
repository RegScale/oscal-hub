# Testing Guide for OSCAL UX

This guide covers all testing practices, tools, and commands for the OSCAL UX application.

## Table of Contents
- [Testing Stack](#testing-stack)
- [Quick Start](#quick-start)
- [Unit & Component Testing](#unit--component-testing)
- [End-to-End Testing](#end-to-end-testing)
- [Accessibility Testing](#accessibility-testing)
- [Writing Tests](#writing-tests)
- [CI/CD Integration](#cicd-integration)
- [Troubleshooting](#troubleshooting)

---

## Testing Stack

### Unit & Component Tests
- **Framework**: [Vitest](https://vitest.dev/) - Fast, Vite-powered test runner
- **Testing Library**: [@testing-library/react](https://testing-library.com/docs/react-testing-library/intro/) - User-centric component testing
- **DOM**: [happy-dom](https://github.com/capricorn86/happy-dom) - Lightweight DOM implementation

### End-to-End Tests
- **Framework**: [Playwright](https://playwright.dev/) - Cross-browser testing
- **Accessibility**: [@axe-core/playwright](https://github.com/dequelabs/axe-core-npm/tree/develop/packages/playwright) - Automated accessibility testing

---

## Quick Start

### Running All Tests
```bash
# Run all unit tests
npm test

# Run all E2E tests
npm run test:e2e

# Run accessibility tests only
npm run test:a11y
```

### Watch Mode (for development)
```bash
# Watch unit tests
npm run test:watch

# Interactive UI for unit tests
npm run test:ui
```

### Coverage Reports
```bash
# Generate coverage report
npm run test:coverage

# Coverage report will be in ./coverage/index.html
```

---

## Unit & Component Testing

### Configuration
Unit tests are configured in `vitest.config.ts`:
- Environment: `happy-dom`
- Setup file: `src/test/setup.ts`
- Coverage provider: `v8`

### Test File Location
Place test files next to the code they test:
```
src/
├── lib/
│   ├── utils.ts
│   └── utils.test.ts          ← Test file
├── components/
│   ├── file-uploader.tsx
│   └── file-uploader.test.tsx ← Test file
```

### Example Unit Test
```typescript
import { describe, it, expect } from 'vitest';
import { generateConvertedFilename } from './download';

describe('generateConvertedFilename', () => {
  it('should replace .xml extension with target format', () => {
    const result = generateConvertedFilename('catalog.xml', 'json');
    expect(result).toBe('catalog.json');
  });
});
```

### Example Component Test
```typescript
import { render, screen } from '@testing-library/react';
import { describe, it, expect } from 'vitest';
import { Button } from './button';

describe('Button', () => {
  it('should render children', () => {
    render(<Button>Click me</Button>);
    expect(screen.getByText('Click me')).toBeInTheDocument();
  });

  it('should be disabled when disabled prop is true', () => {
    render(<Button disabled>Click me</Button>);
    expect(screen.getByText('Click me')).toBeDisabled();
  });
});
```

### Running Specific Tests
```bash
# Run tests in a specific file
npm test utils.test.ts

# Run tests matching a pattern
npm test -- -t "should replace"

# Run tests for a specific folder
npm test src/lib
```

---

## End-to-End Testing

### Configuration
E2E tests are configured in `playwright.config.ts`:
- Browsers: Chromium, Firefox, WebKit
- Base URL: `http://localhost:3000`
- Auto-starts dev server before tests

### Test File Location
Place E2E tests in the `e2e/` directory:
```
e2e/
├── validate.spec.ts       ← Feature tests
├── accessibility.spec.ts  ← Accessibility tests
└── batch.spec.ts          ← More feature tests
```

### Example E2E Test
```typescript
import { test, expect } from '@playwright/test';

test('should navigate to validate page', async ({ page }) => {
  await page.goto('/');

  await page.getByRole('link', { name: /validate/i }).click();

  await expect(page).toHaveURL('/validate');
  await expect(page.getByRole('heading', { name: /validate/i })).toBeVisible();
});
```

### Running E2E Tests
```bash
# Run all E2E tests
npm run test:e2e

# Run with UI mode (recommended for development)
npm run test:e2e:ui

# Debug mode
npm run test:e2e:debug

# Run specific test file
npx playwright test e2e/validate.spec.ts

# Run in headed mode (see the browser)
npx playwright test --headed
```

### Playwright Codegen
Generate tests by recording interactions:
```bash
npx playwright codegen http://localhost:3000
```

---

## Accessibility Testing

### Automated Accessibility Checks
We use axe-core for automated accessibility testing:

```typescript
import { test, expect } from '@playwright/test';
import AxeBuilder from '@axe-core/playwright';

test('should not have accessibility violations', async ({ page }) => {
  await page.goto('/');

  const accessibilityScanResults = await new AxeBuilder({ page }).analyze();

  expect(accessibilityScanResults.violations).toEqual([]);
});
```

### Running Accessibility Tests
```bash
# Run all accessibility tests
npm run test:a11y

# View detailed results
npx playwright test e2e/accessibility.spec.ts --reporter=html
```

### Manual Accessibility Testing
In addition to automated tests, perform manual testing:
1. **Keyboard Navigation**: Navigate using Tab, Enter, Escape
2. **Screen Reader**: Test with NVDA (Windows) or VoiceOver (Mac)
3. **Zoom**: Test at 200% zoom level
4. **Color Contrast**: Use browser dev tools

See [ACCESSIBILITY_CHECKLIST.md](../../ACCESSIBILITY_CHECKLIST.md) for comprehensive manual testing procedures.

---

## Writing Tests

### Best Practices

#### 1. Test User Behavior, Not Implementation
```typescript
// ❌ Bad - testing implementation details
const button = container.querySelector('.btn-primary');

// ✅ Good - testing user interaction
const button = screen.getByRole('button', { name: /submit/i });
```

#### 2. Use Semantic Queries
Priority order:
1. `getByRole` - Best for accessibility
2. `getByLabelText` - For form fields
3. `getByPlaceholderText` - For inputs without labels
4. `getByText` - For non-interactive elements
5. `getByTestId` - Last resort

```typescript
// ✅ Best
screen.getByRole('button', { name: /submit/i })

// ✅ Good for forms
screen.getByLabelText(/email address/i)

// ❌ Avoid
screen.getByTestId('submit-button')
```

#### 3. Test Accessibility
```typescript
test('button should be keyboard accessible', async ({ page }) => {
  await page.goto('/');

  await page.keyboard.press('Tab'); // Skip link
  await page.keyboard.press('Tab'); // First button
  await page.keyboard.press('Enter');

  // Verify action occurred
});
```

#### 4. Use Descriptive Test Names
```typescript
// ❌ Bad
it('works', () => { ... });

// ✅ Good
it('should display error message when file upload fails', () => { ... });
```

### Testing Patterns

#### Testing Async Operations
```typescript
import { waitFor } from '@testing-library/react';

it('should load data', async () => {
  render(<DataComponent />);

  await waitFor(() => {
    expect(screen.getByText('Data loaded')).toBeInTheDocument();
  });
});
```

#### Testing User Interactions
```typescript
import { userEvent } from '@testing-library/user-event';

it('should update input value', async () => {
  const user = userEvent.setup();
  render(<Form />);

  const input = screen.getByLabelText(/name/i);
  await user.type(input, 'John Doe');

  expect(input).toHaveValue('John Doe');
});
```

#### Mocking API Calls
```typescript
import { vi } from 'vitest';

vi.mock('@/lib/api-client', () => ({
  apiClient: {
    validate: vi.fn().mockResolvedValue({ valid: true }),
  },
}));
```

---

## CI/CD Integration

### GitHub Actions Example
```yaml
name: Tests

on: [push, pull_request]

jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - uses: actions/setup-node@v3
        with:
          node-version: '18'

      - name: Install dependencies
        run: npm ci

      - name: Run unit tests
        run: npm test -- --coverage

      - name: Install Playwright Browsers
        run: npx playwright install --with-deps

      - name: Run E2E tests
        run: npm run test:e2e

      - name: Upload coverage
        uses: codecov/codecov-action@v3
        with:
          files: ./coverage/coverage-final.json
```

### Pre-commit Hook
Add to `.husky/pre-commit`:
```bash
#!/bin/sh
npm test -- --run
```

---

## Test Coverage Goals

### Current Coverage
Run `npm run test:coverage` to see current coverage.

### Coverage Goals
- **Overall**: 80% minimum
- **Critical paths**: 90%+ (validation, conversion, API client)
- **UI components**: 70%+
- **Utility functions**: 95%+

### Viewing Coverage
```bash
npm run test:coverage

# Open HTML report
open coverage/index.html
```

---

## Troubleshooting

### Common Issues

#### 1. Tests timeout
```bash
# Increase timeout in vitest.config.ts
test: {
  testTimeout: 10000, // 10 seconds
}

# Or in Playwright
test.setTimeout(30000); // 30 seconds
```

#### 2. Module not found
```bash
# Check path aliases in vitest.config.ts
resolve: {
  alias: {
    '@': path.resolve(__dirname, './src'),
  },
}
```

#### 3. Playwright browser not found
```bash
# Reinstall browsers
npx playwright install
```

#### 4. Tests pass locally but fail in CI
- Check Node.js version matches
- Ensure all dependencies are in package.json (not just devDependencies for test tools)
- Check environment variables

#### 5. Flaky tests
- Add explicit waits: `await page.waitForSelector()`
- Use `waitFor` from Testing Library
- Avoid `sleep()` or fixed timeouts

---

## Resources

### Documentation
- [Vitest Docs](https://vitest.dev/)
- [Testing Library Docs](https://testing-library.com/)
- [Playwright Docs](https://playwright.dev/)
- [Axe-core Docs](https://github.com/dequelabs/axe-core)

### Guides
- [Testing Library Best Practices](https://kentcdodds.com/blog/common-mistakes-with-react-testing-library)
- [Playwright Best Practices](https://playwright.dev/docs/best-practices)
- [Web Accessibility Testing](https://www.w3.org/WAI/test-evaluate/)

---

## Contributing

When adding new features:
1. Write tests first (TDD) or alongside the feature
2. Aim for meaningful coverage, not just high percentages
3. Include accessibility tests for UI components
4. Update this guide if you add new testing patterns

---

**Questions?** Check the project's GitHub issues or create a new one.

**Last Updated**: 2025-10-15
