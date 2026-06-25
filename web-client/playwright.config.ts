import { defineConfig, devices } from '@playwright/test'

export default defineConfig({
  testDir: './e2e',
  testMatch: '**/*.e2e.ts',
  testIgnore: ['**/*.test.ts', '**/*.test.tsx', '**/__tests__/**', '**/*.js'],
  fullyParallel: false,
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 2 : 0,
  workers: 1,
  reporter: 'html',
  use: {
    baseURL: 'http://localhost:8080',
    trace: 'on-first-retry',
    screenshot: 'only-on-failure',
  },
  globalSetup: './e2e/global-setup',
  projects: [
    {
      name: 'chromium',
      use: { ...devices['Desktop Chrome'] },
    },
  ],
  globalTimeout: 15 * 60 * 1000,
  timeout: 5 * 60 * 1000,
})
