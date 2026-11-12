import { defineConfig } from '@playwright/test';

const previewURL = process.env.JONPC_TEST_BASE_URL ?? 'http://127.0.0.1:5174';

export default defineConfig({
  testDir: './tests',
  testMatch: ['storefront-navigation.spec.ts', 'storefront-purchase.spec.ts'],
  workers: 1,
  timeout: 60_000,
  expect: { timeout: 10_000 },
  outputDir: '../output/seo-regression-fix/playwright',
  reporter: [['list'], ['json', { outputFile: '../output/seo-regression-fix/ui-tests.json' }]],
  webServer: process.env.CI ? {
    command: `npm run preview -- --host 127.0.0.1 --port ${new URL(previewURL).port} --strictPort`,
    url: previewURL,
    timeout: 30_000,
  } : undefined,
  use: {
    browserName: 'chromium',
    channel: process.env.PLAYWRIGHT_BROWSER_CHANNEL,
    viewport: { width: 1440, height: 1000 },
    trace: 'retain-on-failure',
  },
  projects: process.env.JONPC_TEST_BASE_URL
    ? [{ name: 'selected-environment', use: { baseURL: process.env.JONPC_TEST_BASE_URL } }]
    : [
      { name: 'dev-ipv4', use: { baseURL: 'http://127.0.0.1:5174' } },
      { name: 'dev-localhost', use: { baseURL: 'http://localhost:5174' } },
      { name: 'generated-preview', use: { baseURL: 'http://127.0.0.1:5175' } },
    ],
});
