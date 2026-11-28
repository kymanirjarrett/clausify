import { Page, expect } from '@playwright/test';
import path from 'node:path';

export const FIXTURES = path.resolve(__dirname, '../../backend/src/test/resources/contracts') + path.sep;

/** Signs up a fresh account through the real form, so every test starts with its own empty data. */
export async function signUp(page: Page): Promise<string> {
  const email = `e2e.${Date.now()}.${Math.random().toString(36).slice(2, 8)}@example.com`;
  await page.goto('/signup');
  await page.getByLabel('Full name').fill('Maria Lopez');
  await page.getByLabel('Email').fill(email);
  await page.getByLabel('Password', { exact: true }).fill('E2e-test-password-1');
  await page.getByRole('button', { name: 'Create account' }).click();
  await expect(page).toHaveURL(/\/app$/);
  return email;
}

/** Uploads a fixture from the dashboard and opens its contract page once the fake analyzer has finished. */
export async function uploadAndOpen(page: Page, fixture: string): Promise<void> {
  await page.goto('/app');
  await page.locator('input[type="file"]').setInputFiles(FIXTURES + fixture);
  const row = page.getByRole('link', { name: new RegExp(fixture.replace('.pdf', '')) }).first();
  await expect(row).toBeVisible();
  await row.click();
  await expect(page.getByRole('heading', { name: 'Analysis' })).toBeVisible();
  await expect(page.getByText('/ 100').first()).toBeVisible();
}
