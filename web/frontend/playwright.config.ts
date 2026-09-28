import { defineConfig, devices } from '@playwright/test';

export default defineConfig({
  testDir: './e2e',
  fullyParallel: false,
  forbidOnly: Boolean(process.env.CI),
  retries: process.env.CI ? 1 : 0,
  workers: 1,
  timeout: 60_000,
  expect: {
    timeout: 10_000
  },
  reporter: process.env.CI
    ? [['line'], ['html', { open: 'never' }]]
    : [['list'], ['html', { open: 'never' }]],
  use: {
    baseURL: process.env.PLAYWRIGHT_BASE_URL ?? 'http://127.0.0.1:18081',
    trace: 'retain-on-failure',
    screenshot: 'only-on-failure',
    video: 'retain-on-failure'
  },
  projects: [
    {
      name: 'chromium',
      use: { ...devices['Desktop Chrome'], viewport: { width: 1440, height: 900 } }
    },
    {
      name: 'chromium-mobile',
      testMatch: '**/accessibility.spec.ts',
      use: { ...devices['Desktop Chrome'], viewport: { width: 390, height: 844 } }
    },
    {
      name: 'chromium-tablet',
      testMatch: '**/accessibility.spec.ts',
      use: { ...devices['Desktop Chrome'], viewport: { width: 768, height: 1024 } }
    },
    {
      name: 'firefox',
      testMatch: '**/accessibility.spec.ts',
      use: { ...devices['Desktop Firefox'], viewport: { width: 1440, height: 900 } }
    },
    {
      name: 'webkit',
      testMatch: '**/accessibility.spec.ts',
      use: { ...devices['Desktop Safari'], viewport: { width: 1440, height: 900 } }
    }
  ],
  outputDir: 'test-results/playwright'
});
